package io.github.relony.pinry.data

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareImageTest {
    private val page = "https://example.com/articles/42".toHttpUrl()

    @Test
    fun openGraphImageInEitherAttributeOrder() {
        assertEquals(
            "https://cdn.example.com/a.jpg",
            findShareImage("""<meta property="og:image" content="https://cdn.example.com/a.jpg">""", page),
        )
        assertEquals(
            "https://cdn.example.com/b.jpg",
            findShareImage("""<META content='https://cdn.example.com/b.jpg' property='og:image' />""", page),
        )
    }

    @Test
    fun relativeAndEscapedUrlsAreResolved() {
        assertEquals(
            "https://example.com/img/c.jpg?w=1200&h=630",
            findShareImage("""<meta property="og:image" content="/img/c.jpg?w=1200&amp;h=630">""", page),
        )
    }

    @Test
    fun twitterImageIsTheFallbackAndOgWins() {
        val html = """
            <meta name="twitter:image" content="https://t.example/t.jpg">
            <meta property="og:image" content="https://o.example/o.jpg">
        """
        assertEquals("https://o.example/o.jpg", findShareImage(html, page))
        assertEquals("https://t.example/t.jpg", findShareImage("""<meta name="twitter:image" content="https://t.example/t.jpg">""", page))
        assertNull(findShareImage("<html><title>No preview</title></html>", page))
    }

    @Test
    fun urlIsTakenOutOfSharedText() {
        assertEquals("https://pin.it/3xYz", firstUrl("Look at this! https://pin.it/3xYz."))
        assertEquals("http://a.example/b?c=d", firstUrl("http://a.example/b?c=d"))
        assertNull(firstUrl("no link here"))
    }

    @Test
    fun resolverTellsImagesFromPages() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val resolver = ImageUrlResolver(OkHttpClient())
            server.enqueue(MockResponse.Builder().addHeader("Content-Type", "image/png").body("png").build())
            server.enqueue(
                MockResponse.Builder().addHeader("Content-Type", "text/html; charset=utf-8")
                    .body("""<head><meta property="og:image" content="/big.jpg"></head>""").build()
            )
            server.enqueue(MockResponse.Builder().addHeader("Content-Type", "text/html").body("<p>nothing</p>").build())
            server.enqueue(MockResponse.Builder().code(404).build())

            val image = server.url("/cat.png").toString()
            assertEquals(ImageSource(image, pageUrl = null), resolver.resolve(image))
            val article = server.url("/post").toString()
            assertEquals(ImageSource(server.url("/big.jpg").toString(), article), resolver.resolve(article))
            assertNull(resolver.resolve(server.url("/plain").toString()))
            assertNull(resolver.resolve(server.url("/gone").toString()))
            assertEquals("browser user agent", true, server.takeRequest().headers["User-Agent"]!!.startsWith("Mozilla/"))
        }
    }
}
