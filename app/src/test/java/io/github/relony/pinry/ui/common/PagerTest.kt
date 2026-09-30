package io.github.relony.pinry.ui.common

import io.github.relony.pinry.data.api.Page
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagerTest {
    /** A newest-first server list of ids; [pageSize] items per request. */
    private class FakeServer(var ids: List<Int>, private val pageSize: Int = 3) {
        val requestedOffsets = mutableListOf<Int>()
        fun page(offset: Int): Page<Int> {
            requestedOffsets += offset
            val results = ids.drop(offset).take(pageSize)
            return Page(ids.size, next = if (offset + results.size < ids.size) "more" else null, results = results)
        }
    }

    private fun TestScope.pagerOver(server: FakeServer) = Pager(this, key = { it: Int -> it }) { server.page(it) }

    @Test
    fun pagesAppendUntilTheEnd() = runTest {
        val server = FakeServer((10 downTo 4).toList())
        val pager = pagerOver(server)

        repeat(4) { pager.loadMore(); advanceUntilIdle() }

        assertEquals((10 downTo 4).toList(), pager.state.value.items)
        assertTrue(pager.state.value.endReached)
        assertEquals(listOf(0, 3, 6), server.requestedOffsets)
    }

    @Test
    fun pinAddedOnTheServerBetweenPagesIsNotShownTwice() = runTest {
        val server = FakeServer(listOf(10, 9, 8, 7, 6))
        val pager = pagerOver(server)
        pager.loadMore(); advanceUntilIdle()

        server.ids = listOf(11) + server.ids // shifts everything by one: the next page starts with 8 again
        pager.loadMore(); advanceUntilIdle()

        assertEquals(listOf(10, 9, 8, 7, 6), pager.state.value.items)
    }

    @Test
    fun concurrentLoadMoreRequestsOnePage() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val pager = Pager(this, key = { it: Int -> it }) {
            calls++
            gate.await()
            Page(1, results = listOf(1))
        }

        pager.loadMore(); pager.loadMore(); advanceUntilIdle()
        pager.loadMore()
        gate.complete(Unit); advanceUntilIdle()

        assertEquals(1, calls)
    }

    @Test
    fun errorStopsPagingUntilRetry() = runTest {
        var fail = true
        val pager = Pager(this, key = { it: Int -> it }) {
            if (fail) error("offline") else Page(1, results = listOf(1))
        }

        pager.loadMore(); advanceUntilIdle()
        assertNotNull(pager.state.value.error)
        pager.loadMore(); advanceUntilIdle()
        assertNotNull("loadMore must not retry on its own", pager.state.value.error)

        fail = false
        pager.retry(); advanceUntilIdle()
        assertNull(pager.state.value.error)
        assertEquals(listOf(1), pager.state.value.items)
    }

    @Test
    fun refreshStartsOverFromTheFirstPage() = runTest {
        val server = FakeServer((10 downTo 1).toList())
        val pager = pagerOver(server)
        pager.loadMore(); advanceUntilIdle()
        pager.loadMore(); advanceUntilIdle()

        server.ids = listOf(12, 11) + server.ids
        pager.refresh(); advanceUntilIdle()

        assertEquals(listOf(12, 11, 10), pager.state.value.items)
        assertFalse(pager.state.value.refreshing)
        assertEquals(0, server.requestedOffsets.last())
    }

    @Test
    fun deletedPinDoesNotMakeTheNextPageSkipOne() = runTest {
        val server = FakeServer((10 downTo 1).toList())
        val pager = pagerOver(server)
        pager.loadMore(); advanceUntilIdle() // 10 9 8

        server.ids = server.ids - 9
        pager.remove(9)
        pager.loadMore(); advanceUntilIdle()

        assertEquals(listOf(10, 8, 7, 6, 5), pager.state.value.items)
    }

    @Test
    fun prependedPinShiftsTheNextPage() = runTest {
        val server = FakeServer((10 downTo 1).toList())
        val pager = pagerOver(server)
        pager.loadMore(); advanceUntilIdle()

        server.ids = listOf(11) + server.ids
        pager.prepend(11)
        pager.loadMore(); advanceUntilIdle()

        assertEquals(listOf(11, 10, 9, 8, 7, 6, 5), pager.state.value.items)
        assertEquals(listOf(0, 4), server.requestedOffsets)
    }
}
