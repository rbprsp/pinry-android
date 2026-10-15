package dev.relony.pinry.data

import dev.relony.pinry.data.api.Page
import dev.relony.pinry.data.api.Pin
import dev.relony.pinry.data.api.PinImage
import dev.relony.pinry.data.api.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPinsTest {
    private fun pin(id: Int) = Pin(id, submitter = User("u"), image = PinImage(id, "http://x/$id.jpg", 1, 1))

    /** A newest-first feed of ids, [pageSize] per request. */
    private class FakeFeed(private val ids: List<Int>, private val pageSize: Int = 3) {
        val offsets = mutableListOf<Int>()
        fun page(offset: Int): Page<Pin> {
            offsets += offset
            val results = ids.drop(offset).take(pageSize)
            return Page(ids.size, next = if (offset + results.size < ids.size) "more" else null, results = results.map {
                Pin(it, submitter = User("u"), image = PinImage(it, "http://x/$it.jpg", 1, 1))
            })
        }
    }

    @Test
    fun startsFromWhatTheFeedShowedAndPagesOnFromThere() = runTest {
        val feed = FakeFeed((10 downTo 1).toList())
        val pins = FeedPins(listOf(10, 9, 8, 7), cached = ::pin, page = feed::page)

        assertEquals(listOf(10, 9, 8, 7), pins.pins.map { it.id })
        pins.loadMore()
        assertEquals(listOf(10, 9, 8, 7, 6, 5, 4), pins.pins.map { it.id })
        assertEquals(listOf(4), feed.offsets)
    }

    @Test
    fun dropsRepeatsAndStopsAtTheEnd() = runTest {
        // Pin 6 was added after the feed loaded, so offset 3 now starts at pin 3 again.
        val feed = FakeFeed(listOf(6, 5, 4, 3, 2, 1), pageSize = 2)
        val pins = FeedPins(listOf(5, 4, 3), cached = ::pin, page = feed::page)

        pins.loadMore()
        assertEquals(listOf(5, 4, 3, 2), pins.pins.map { it.id })
        assertFalse(pins.endReached)
        pins.loadMore()
        assertTrue(pins.endReached)
        assertEquals(listOf(5, 4, 3, 2, 1), pins.pins.map { it.id })
        pins.loadMore()
        assertEquals(listOf(3, 5), feed.offsets)
    }

    @Test
    fun skipsPinsNoLongerCached() {
        val pins = FeedPins(listOf(3, 2, 1), cached = { if (it == 2) null else pin(it) }, page = { error("unused") })

        assertEquals(listOf(3, 1), pins.pins.map { it.id })
    }
}
