package dev.relony.pinry.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSaverTest {
    @Test
    fun keepsTheServersFileName() {
        assertEquals("sunset.png", imageFileName("https://pinry.example.com/media/a/b/sunset.png", 7))
    }

    @Test
    fun ignoresTheQuery() {
        assertEquals("cat.webp", imageFileName("https://pinry.example.com/media/cat.webp?v=2", 7))
    }

    @Test
    fun fallsBackWithoutAnImageExtension() {
        assertEquals("pin-7.jpg", imageFileName("https://pinry.example.com/media/abc123", 7))
        assertEquals("pin-7.jpg", imageFileName("not a url", 7))
    }

    @Test
    fun mimeTypeComesFromTheExtensionWhenTheServerSendsNoImageType() {
        assertEquals("image/png", imageMimeType("application/octet-stream", "sunset.png"))
        assertEquals("image/jpeg", imageMimeType(null, "pin-7.jpg"))
        assertEquals("image/webp", imageMimeType("image/webp", "cat.webp"))
        assertEquals("image/jpeg", imageMimeType("text/html", "unknown"))
    }
}
