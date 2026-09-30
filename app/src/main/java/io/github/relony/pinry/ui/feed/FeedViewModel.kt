package io.github.relony.pinry.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.relony.pinry.data.BoardChange
import io.github.relony.pinry.data.BoardRepository
import io.github.relony.pinry.data.PinChange
import io.github.relony.pinry.data.PinFilter
import io.github.relony.pinry.data.PinRepository
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.ui.common.Pager
import kotlinx.coroutines.launch

class FeedViewModel(
    repository: PinRepository,
    boards: BoardRepository,
    private val filter: PinFilter,
) : ViewModel() {
    val pager = Pager(viewModelScope, key = Pin::id) { offset -> repository.page(filter, offset) }

    init {
        pager.loadMore()
        viewModelScope.launch {
            repository.changes.collect { change ->
                when (change) {
                    is PinChange.Created -> if (filter.includesNew(change.pin)) pager.prepend(change.pin)
                    is PinChange.Updated -> pager.replace(change.pin)
                    is PinChange.Deleted -> pager.remove(change.id)
                }
            }
        }
        if (filter is PinFilter.Board) {
            viewModelScope.launch {
                boards.changes.collect { change ->
                    when {
                        change is BoardChange.PinRemoved && change.boardId == filter.id -> pager.remove(change.pinId)
                        // Board feeds are ordered by pin id, not by when a pin was added; reload to place it.
                        change is BoardChange.PinAdded && change.boardId == filter.id -> pager.refresh()
                    }
                }
            }
        }
    }

    /** Board membership is set after creation, so board feeds pick new pins up on refresh. */
    private fun PinFilter.includesNew(pin: Pin) = when (this) {
        PinFilter.All -> true
        is PinFilter.User -> pin.submitter.username == username
        is PinFilter.Tag -> name in pin.tags
        is PinFilter.Board -> false
    }
}
