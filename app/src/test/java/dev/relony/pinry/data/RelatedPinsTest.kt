package dev.relony.pinry.data

import dev.relony.pinry.data.api.Page
import dev.relony.pinry.data.api.Pin
import dev.relony.pinry.data.api.PinImage
import dev.relony.pinry.data.api.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedPinsTest {
    private class FakeServer(private val tags: Map<String, List<Int>>, private val pageSize: Int = 3) {
        val requests = mutableListOf<Pair<String, Int>>()
        fun page(tag: String, offset: Int): Page<Pin> {
            requests += tag to offset
            val ids = tags.getValue(tag)
            val results = ids.drop(offset).take(pageSize)
            return Page(ids.size, next = if (offset + results.size < ids.size) "more" else null, results = results.map {
                Pin(it, submitter = User("u"), image = PinImage(it, "http://x/$it.jpg", 1, 1))
            })
        }
    }

    private val cats = (40 downTo 21).toList()
    private val art = (30 downTo 11).toList()

    private suspend fun RelatedPins.all(): List<Int> {
        val ids = mutableListOf<Int>()
        while (true) {
            val page = next(7)
            ids += page.results.map { it.id }
            if (page.next == null) return ids
        }
    }

    @Test
    fun everyPinOnceWithoutTheOpenPin() = runTest {
        val server = FakeServer(mapOf("cats" to cats, "art" to art))
        val ids = RelatedPins(listOf("cats", "art"), exclude = 25, seed = 1, page = server::page).all()

        assertEquals((11..40).toSet() - 25, ids.toSet())
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun sameSeedSameOrderAndNotNewestFirst() = runTest {
        fun related() = RelatedPins(listOf("cats", "art"), exclude = 0, seed = 7, page = FakeServer(mapOf("cats" to cats, "art" to art))::page)
        val first = related().all()

        assertEquals(first, related().all())
        assertNotEquals(first.sortedDescending(), first)
    }

    @Test
    fun eachPageIsFetchedOnce() = runTest {
        val server = FakeServer(mapOf("cats" to cats, "art" to art))
        RelatedPins(listOf("cats", "art", "cats"), exclude = 0, seed = 3, page = server::page).all()

        assertEquals(server.requests.size, server.requests.toSet().size)
        assertEquals(setOf(0, 3, 6, 9, 12, 15, 18), server.requests.filter { it.first == "cats" }.map { it.second }.toSet())
    }

    @Test
    fun loadsLazily() = runTest {
        val server = FakeServer(mapOf("cats" to cats, "art" to art))
        RelatedPins(listOf("cats", "art"), exclude = 0, seed = 5, page = server::page).next(1)

        assertTrue(server.requests.size <= 4)
    }

    @Test
    fun noTagsMeansNoPinsAndNoRequests() = runTest {
        val server = FakeServer(emptyMap())
        val page = RelatedPins(emptyList(), exclude = 0, seed = 1, page = server::page).next(5)

        assertEquals(emptyList<Pin>(), page.results)
        assertNull(page.next)
        assertEquals(emptyList<Pair<String, Int>>(), server.requests)
    }
}
