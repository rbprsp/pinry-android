package dev.relony.pinry.data

import dev.relony.pinry.data.api.ImageSize
import dev.relony.pinry.data.api.Pin
import dev.relony.pinry.data.api.PinImage
import dev.relony.pinry.data.api.User
import dev.relony.pinry.data.net.rebaseMediaUrl
import dev.relony.pinry.data.net.withMediaFrom
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaUrlsTest {
    private val base = "https://pinry.example.com/".toHttpUrl()

    @Test
    fun mediaUrlFromBehindTheProxyIsMovedToTheServer() {
        assertEquals(
            "https://pinry.example.com/media/a/b/cat.jpg",
            rebaseMediaUrl("http://127.0.0.1:8000/media/a/b/cat.jpg", base),
        )
    }

    @Test
    fun nonMediaAndRelativeUrls() {
        assertEquals("https://cdn.example.com/x.jpg", rebaseMediaUrl("https://cdn.example.com/x.jpg", base))
        assertEquals("https://pinry.example.com/media/x.jpg", rebaseMediaUrl("/media/x.jpg", base))
    }

    @Test
    fun pinSourceUrlIsLeftAlone() {
        val internal = "http://pinry:8000/media"
        val pin = Pin(
            id = 1,
            submitter = User("me"),
            url = "http://example.com/media/original.jpg",
            image = PinImage(
                id = 1, image = "$internal/o.jpg", width = 800, height = 600,
                standard = ImageSize("$internal/s.jpg", 600, 450),
                thumbnail = ImageSize("$internal/t.jpg", 240, 180),
            ),
        )

        val rebased = pin.withMediaFrom(base)

        assertEquals("http://example.com/media/original.jpg", rebased.url)
        assertEquals("https://pinry.example.com/media/o.jpg", rebased.image.image)
        assertEquals("https://pinry.example.com/media/s.jpg", rebased.image.standard!!.image)
        assertEquals("https://pinry.example.com/media/t.jpg", rebased.image.thumbnail!!.image)
    }
}
