package io.github.relony.pinry

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.relony.pinry.data.AuthRepository
import io.github.relony.pinry.data.SessionStore
import io.github.relony.pinry.data.net.AuthInterceptor
import io.github.relony.pinry.data.net.HostCookieJar
import io.github.relony.pinry.data.net.ServerContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** App-wide singletons. */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val server = ServerContext()
    private val cookies = HostCookieJar(server)

    val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor(AuthInterceptor(server))
        .build()

    val auth = AuthRepository(http, server, cookies, SessionStore(context.sessionDataStore), scope)

    init {
        scope.launch { auth.restore() }
    }
}
