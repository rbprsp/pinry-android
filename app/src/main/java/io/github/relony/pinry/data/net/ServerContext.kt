package io.github.relony.pinry.data.net

import okhttp3.HttpUrl

/**
 * The one Pinry server the app talks to. The shared OkHttp client also loads images from other
 * hosts, so the auth token and cookies are only ever attached to requests for this origin.
 */
class ServerContext {
    @Volatile var baseUrl: HttpUrl? = null
    @Volatile var token: String? = null

    /** Called from an OkHttp thread when the server rejects the saved session. */
    @Volatile var onSessionExpired: () -> Unit = {}

    fun owns(url: HttpUrl): Boolean {
        val base = baseUrl ?: return false
        return url.scheme == base.scheme && url.host == base.host && url.port == base.port
    }
}
