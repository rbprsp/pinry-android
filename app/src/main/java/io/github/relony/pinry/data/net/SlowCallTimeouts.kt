package io.github.relony.pinry.data.net

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Longer timeouts for the two slow calls: image uploads can be large, and for a pin created from a
 * URL the server downloads the image before it answers.
 */
class SlowCallTimeouts : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val slow = request.method == "POST" && (path.endsWith("/api/v2/images/") || path.endsWith("/api/v2/pins/"))
        if (!slow) return chain.proceed(request)
        return chain
            .withReadTimeout(60, TimeUnit.SECONDS)
            .withWriteTimeout(60, TimeUnit.SECONDS)
            .proceed(request)
    }
}
