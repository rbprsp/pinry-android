package io.github.relony.pinry.ui.common

import io.github.relony.pinry.data.api.ImageSize
import io.github.relony.pinry.data.api.PinImage
import org.junit.Assert.assertEquals
import org.junit.Test

class PinImagesTest {
    private val image = PinImage(
        id = 1, image = "orig", width = 736, height = 981,
        standard = ImageSize("std", 600, 800),
        thumbnail = ImageSize("thumb", 240, 320),
        square = ImageSize("square", 125, 125),
    )

    @Test
    fun smallestVariantThatCoversTheCell() {
        assertEquals("thumb", image.urlFor(200))
        assertEquals("std", image.urlFor(300))
        assertEquals("std", image.urlFor(600))
        assertEquals("orig", image.urlFor(700))
    }

    @Test
    fun missingVariantsFallBackToOriginal() {
        assertEquals("orig", image.copy(standard = null, thumbnail = null).urlFor(100))
    }

    @Test
    fun veryTallImagesAreClampedInTheGrid() {
        assertEquals(0.4f, image.copy(width = 600, height = 6000).gridAspectRatio())
        assertEquals(736f / 981f, image.gridAspectRatio())
        assertEquals(1f, image.copy(height = 0).gridAspectRatio())
    }
}
