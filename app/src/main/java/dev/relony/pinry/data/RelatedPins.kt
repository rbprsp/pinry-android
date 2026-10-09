package dev.relony.pinry.data

import dev.relony.pinry.data.api.Page
import dev.relony.pinry.data.api.Pin
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Pins sharing any of [tags], in a random order that [seed] keeps stable. Pinry only orders by id,
 * so each tag's pages are fetched in shuffled order and the tags are mixed by how many pins they have left.
 */
class RelatedPins(
    tags: List<String>,
    private val exclude: Int,
    seed: Long,
    private val page: suspend (tag: String, offset: Int) -> Page<Pin>,
) {
    private inner class Feed(val tag: String, val random: Random) {
        val buffer = ArrayDeque<Pin>()
        var offsets: ArrayDeque<Int>? = null
        var firstPage: List<Pin>? = null
        var remaining = 0

        val hasMore get() = offsets?.isNotEmpty() ?: true

        suspend fun fetch() {
            if (offsets == null) {
                val first = page(tag, 0)
                val size = first.results.size
                remaining = first.count
                firstPage = first.results
                offsets = ArrayDeque(if (size == 0) emptyList() else (0 until first.count step size).shuffled(random))
            }
            val offset = offsets?.removeFirstOrNull() ?: return
            val pins = if (offset == 0) firstPage.orEmpty().also { firstPage = null } else page(tag, offset).results
            buffer.addAll(pins.shuffled(random))
        }
    }

    private val random = Random(seed)
    private val feeds = tags.distinct().map { Feed(it, Random(seed + it.hashCode())) }
    private val seen = HashSet<Int>()

    suspend fun next(count: Int): Page<Pin> {
        val pins = mutableListOf<Pin>()
        while (pins.size < count) {
            refill()
            val ready = feeds.filter { it.buffer.isNotEmpty() }
            if (ready.isEmpty()) {
                if (feeds.any { it.hasMore }) continue else break
            }
            val feed = pick(ready)
            val pin = feed.buffer.removeFirst()
            feed.remaining--
            if (pin.id != exclude && seen.add(pin.id)) pins += pin
        }
        val more = feeds.any { it.hasMore || it.buffer.isNotEmpty() }
        return Page(count = pins.size, next = if (more) "more" else null, results = pins)
    }

    private fun pick(ready: List<Feed>): Feed {
        var roll = random.nextInt(ready.sumOf { it.remaining.coerceAtLeast(1) })
        return ready.first { roll -= it.remaining.coerceAtLeast(1); roll < 0 }
    }

    private suspend fun refill() = coroutineScope {
        feeds.filter { it.buffer.isEmpty() && it.hasMore }.map { async { it.fetch() } }.awaitAll()
    }
}
