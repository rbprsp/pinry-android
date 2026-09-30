package dev.relony.pinry.data

import dev.relony.pinry.data.api.LoginRequest
import dev.relony.pinry.data.api.PinryApi
import dev.relony.pinry.data.api.createPinryApi
import dev.relony.pinry.data.api.fieldErrors
import dev.relony.pinry.data.net.HostCookieJar
import dev.relony.pinry.data.net.ServerContext
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.HttpException

sealed interface SessionState {
    data object Loading : SessionState
    data class LoggedOut(val lastServer: String?) : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

/** Keys are form fields (`server`, `username`, `password`) or whatever else the server reported. */
class LoginException(val fields: Map<String, String>) : Exception(fields.values.joinToString("; "))

class AuthRepository(
    private val http: OkHttpClient,
    private val server: ServerContext,
    private val cookies: HostCookieJar,
    private val store: SessionStore,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state

    init {
        server.onSessionExpired = ::expire
        cookies.onChanged = { changed ->
            val current = _state.value
            if (current is SessionState.LoggedIn) {
                val session = current.session.copy(cookies = changed.map(Cookie::toString))
                _state.value = SessionState.LoggedIn(session)
                scope.launch { store.write(session) }
            }
        }
    }

    suspend fun restore() {
        val session = store.read()
        if (session == null) {
            _state.value = SessionState.LoggedOut(store.lastServer())
        } else {
            activate(session)
        }
    }

    /**
     * Django session login: fetch the CSRF cookie, then POST the credentials with it. The response
     * carries the user's API token; the session cookie it sets is what private instances require.
     */
    suspend fun login(serverInput: String, username: String, password: String) {
        val requested = parseServerUrl(serverInput)
            ?: throw LoginException(mapOf(FIELD_SERVER to "Enter the server address"))
        val base = probe(requested, followRedirect = true)
        val csrfToken = cookies.value("csrftoken") ?: throw notPinry()

        val user = try {
            api(base).login(csrfToken, referer = base.toString(), LoginRequest(username, password))
        } catch (e: HttpException) {
            throw LoginException(e.fieldErrors().ifEmpty { mapOf(FIELD_GENERAL to "Login failed (HTTP ${e.code()})") })
        } catch (e: IOException) {
            throw unreachable(e)
        }
        val token = user.token
            ?: throw LoginException(mapOf(FIELD_GENERAL to "The server didn't return an API token"))

        val session = Session(base.toString(), user.username, token, cookies.all().map(Cookie::toString))
        store.write(session)
        activate(session)
    }

    /**
     * Read-only browsing without an account. Only public instances allow it: with `PUBLIC = False`
     * Pinry refuses the pin list to anyone without a session.
     */
    suspend fun browse(serverInput: String) {
        val requested = parseServerUrl(serverInput)
            ?: throw LoginException(mapOf(FIELD_SERVER to "Enter the server address"))
        val base = probe(requested, followRedirect = true)
        try {
            api(base).pins(offset = 0, limit = 1)
        } catch (e: HttpException) {
            val message = if (e.code() == 401 || e.code() == 403) {
                "This server is private: log in to see its pins"
            } else {
                "Couldn't load pins (HTTP ${e.code()})"
            }
            throw LoginException(mapOf(FIELD_SERVER to message))
        } catch (e: IOException) {
            throw unreachable(e)
        }
        val session = Session(base.toString(), cookies = cookies.all().map(Cookie::toString))
        store.write(session)
        activate(session)
    }

    suspend fun logout() {
        val base = server.baseUrl
        if (base != null) {
            // Best effort: ends the Django session. DRF tokens can't be revoked through the API.
            withTimeoutOrNull(3_000) {
                try {
                    api(base).logout()
                } catch (_: IOException) {
                } catch (_: HttpException) {
                }
            }
        }
        signOutLocally()
        store.clear()
    }

    /** Checks that [base] is a Pinry server and picks up its `csrftoken` cookie. */
    private suspend fun probe(base: HttpUrl, followRedirect: Boolean): HttpUrl {
        server.baseUrl = base
        server.token = null
        cookies.replaceAll(emptyList())

        val response = try {
            api(base).currentUser()
        } catch (e: IOException) {
            throw unreachable(e)
        } catch (_: SerializationException) {
            throw notPinry()
        }
        val landed = response.raw().request.url
        if (!server.owns(landed)) {
            // Redirected (typically http → https). The cookies were set for the other origin, and a
            // login POST would be turned into a GET by the redirect, so start over on the new origin.
            if (!followRedirect) throw notPinry()
            val moved = base.newBuilder().scheme(landed.scheme).host(landed.host).port(landed.port).build()
            return probe(moved, followRedirect = false)
        }
        if (!response.isSuccessful) throw notPinry()
        return base
    }

    private fun activate(session: Session) {
        val base = session.baseUrl.toHttpUrl()
        server.baseUrl = base
        server.token = session.token
        cookies.replaceAll(session.cookies.mapNotNull { Cookie.parse(base, it) })
        _state.value = SessionState.LoggedIn(session)
    }

    /** The server rejected the session (see [dev.relony.pinry.data.net.AuthInterceptor]). */
    private fun expire() {
        if (_state.value !is SessionState.LoggedIn) return
        signOutLocally()
        scope.launch { store.clear() }
    }

    private fun signOutLocally() {
        val lastServer = server.baseUrl?.toString()
        server.token = null
        cookies.replaceAll(emptyList())
        _state.value = SessionState.LoggedOut(lastServer)
    }

    private fun api(base: HttpUrl): PinryApi = createPinryApi(base, http)

    private fun notPinry() = LoginException(mapOf(FIELD_SERVER to "This doesn't look like a Pinry server"))

    private fun unreachable(e: IOException) =
        LoginException(mapOf(FIELD_SERVER to "Can't reach the server: ${e.message ?: e.javaClass.simpleName}"))

    companion object {
        const val FIELD_SERVER = "server"
        const val FIELD_GENERAL = "general"
    }
}

/** `pinry.example.com` → `https://pinry.example.com/`; keeps an explicit scheme, port and path. */
fun parseServerUrl(input: String): HttpUrl? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val url = (if ("://" in trimmed) trimmed else "https://$trimmed").toHttpUrlOrNull() ?: return null
    val path = url.encodedPath.let { if (it.endsWith("/")) it else "$it/" }
    return url.newBuilder().encodedPath(path).query(null).fragment(null).build()
}
