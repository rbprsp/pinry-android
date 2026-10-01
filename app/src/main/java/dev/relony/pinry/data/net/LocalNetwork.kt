package dev.relony.pinry.data.net

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Android 17 keeps apps that target it off the local network (a server at home, say) until the
 * user allows ACCESS_LOCAL_NETWORK, listed under "Nearby devices". Android doesn't document which
 * addresses count; this takes private, link-local and unique-local addresses and `.local` names.
 */
class LocalNetwork(private val context: Context, private val scope: CoroutineScope) {
    val granted: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN ||
            context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Whether reaching [host] needs the permission, and it's missing. Blocking: looks [host] up. */
    fun isBlocked(host: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN || granted) return false
        if (host.endsWith(".local", ignoreCase = true)) return true
        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (_: UnknownHostException) {
            return false
        }
        return addresses.any { it.mayBeLocal() }
    }

    /** [isBlocked] without blocking; a lookup that takes over a second counts as not blocked. */
    suspend fun shouldAsk(host: String): Boolean {
        if (granted) return false
        val lookup = scope.async(Dispatchers.IO) { isBlocked(host) }
        return withTimeoutOrNull(1_000) { lookup.await() } ?: false
    }

    companion object {
        // Only checked and asked for on Android 17+, where it exists.
        @SuppressLint("InlinedApi")
        const val PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK
    }
}

/** Private (RFC 1918), link-local, or IPv6 unique-local (fc00::/7). */
internal fun InetAddress.mayBeLocal(): Boolean =
    isSiteLocalAddress || isLinkLocalAddress || (this is Inet6Address && (address[0].toInt() and 0xFE) == 0xFC)

/**
 * Explains a failed connection when the missing permission is the likely cause: Android drops the
 * traffic, so all the app sees otherwise is a timeout.
 */
class LocalNetworkHint(private val localNetwork: LocalNetwork) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            return chain.proceed(chain.request())
        } catch (e: IOException) {
            val network = e is SocketException || e is SocketTimeoutException
            if (network && !chain.call().isCanceled() && localNetwork.isBlocked(chain.request().url.host)) {
                throw IOException("Allow Pinry to access “Nearby devices” in its app permissions to reach a server on your local network", e)
            }
            throw e
        }
    }
}
