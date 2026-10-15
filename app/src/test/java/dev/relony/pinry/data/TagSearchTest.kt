package dev.relony.pinry.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagSearchTest {
    private val tags = listOf("art", "Cats", "cat-memes", "concept art", "pixel-art", "Tokyo Ghoul")

    @Test
    fun matchesAnywhereIgnoringCaseWithPrefixMatchesFirst() {
        assertEquals(listOf("art", "concept art", "pixel-art"), matchingTags(tags, "ART"))
        assertEquals(listOf("Cats", "cat-memes"), matchingTags(tags, "cat"))
    }

    @Test
    fun blankQueryMatchesNothing() {
        assertEquals(emptyList<String>(), matchingTags(tags, "  "))
    }

    @Test
    fun keepsAtMostTheLimit() {
        assertEquals(2, matchingTags(tags, "a", limit = 2).size)
    }
}
