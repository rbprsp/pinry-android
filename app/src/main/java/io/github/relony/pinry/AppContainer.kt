package io.github.relony.pinry

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.relony.pinry.data.AuthRepository
import io.github.relony.pinry.data.BoardRepository
import io.github.relony.pinry.data.ImageUrlResolver
import io.github.relony.pinry.data.PinRepository
import io.github.relony.pinry.data.SessionStore
import io.github.relony.pinry.data.TagRepository
import io.github.relony.pinry.data.api.PinryApi
import io.github.relony.pinry.data.api.createPinryApi
import io.github.relony.pinry.data.net.AuthInterceptor
import io.github.relony.pinry.data.net.HostCookieJar
import io.github.relony.pinry.data.net.ServerContext
import io.github.relony.pinry.data.net.SlowCallTimeouts
import io.github.relony.pinry.data.net.UserAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** App-wide singletons. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val server = ServerContext()
    private val cookies = HostCookieJar(server)

    val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor(UserAgent("PinryAndroid/${BuildConfig.VERSION_NAME} (self-hosted Pinry client; +https://github.com/pinry/pinry)"))
        .addInterceptor(AuthInterceptor(server))
        .addInterceptor(SlowCallTimeouts())
        .build()

    val auth = AuthRepository(http, server, cookies, SessionStore(context.sessionDataStore), scope)

    private var api: Pair<HttpUrl, PinryApi>? = null

    /** The API of the logged-in server. */
    @Synchronized
    fun api(): PinryApi {
        val base = baseUrl()
        return api?.takeIf { it.first == base }?.second
            ?: createPinryApi(base, http).also { api = base to it }
    }

    private fun baseUrl(): HttpUrl = checkNotNull(server.baseUrl) { "Not logged in" }

    val pins = PinRepository(::api, ::baseUrl)
    val boards = BoardRepository(::api, ::baseUrl)
    val tags = TagRepository(::api)
    val imageUrls = ImageUrlResolver(http)

    init {
        scope.launch { auth.restore() }
    }
}
