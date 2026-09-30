package io.github.relony.pinry.data.net

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Token …` to requests for the current server. Token auth takes precedence in
 * DRF, so writes need no CSRF header. The session cookie is still needed: on a private instance
 * (`PUBLIC = False`) Pinry's middleware only looks at the Django session.
 */
class AuthInterceptor(private val server: ServerContext) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = server.token
        if (token == null || !server.owns(request.url)) return chain.proceed(request)

        val response = chain.proceed(
            request.newBuilder().header("Authorization", "Token $token").build()
        )
        if (isSessionRejected(response)) server.onSessionExpired()
        return response
    }

    /**
     * 401, or the private-instance middleware's empty non-JSON 403. A JSON 403 is DRF refusing one
     * action (say, editing someone else's pin) and doesn't mean the session is gone.
     */
    private fun isSessionRejected(response: Response): Boolean {
        if (!response.request.url.encodedPath.startsWith("/api/")) return false
        return when (response.code) {
            401 -> true
            403 -> response.body.contentType()?.subtype != "json"
            else -> false
        }
    }
}
