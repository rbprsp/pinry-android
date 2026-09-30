package io.github.relony.pinry.data.net

import okhttp3.Interceptor
import okhttp3.Response

/**
 * A descriptive user agent for requests that don't set one. Some hosts (Wikimedia, for one)
 * answer OkHttp's generic `okhttp/x.y` with 403, which broke image previews.
 */
class UserAgent(private val value: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("User-Agent") != null) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("User-Agent", value).build())
    }
}
