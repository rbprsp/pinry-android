package dev.relony.pinry.data

import dev.relony.pinry.data.api.Page
import dev.relony.pinry.data.api.Pin

/** A feed's pins in order: those it has shown ([ids]), then further pages from where it stopped. */
class FeedPins(
    ids: List<Int>,
    cached: (Int) -> Pin?,
    private val page: suspend (offset: Int) -> Page<Pin>,
) {
    var pins: List<Pin> = ids.mapNotNull(cached)
        private set
    var endReached = false
        private set
    private var offset = ids.size

    suspend fun loadMore() {
        if (endReached) return
        val next = page(offset)
        offset += next.results.size
        val known = pins.mapTo(HashSet()) { it.id }
        pins = pins + next.results.filter { known.add(it.id) }
        endReached = next.next == null || next.results.isEmpty()
    }
}
