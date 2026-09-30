package dev.relony.pinry.ui.boards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.relony.pinry.data.BoardChange
import dev.relony.pinry.data.BoardRepository
import dev.relony.pinry.data.api.Board
import dev.relony.pinry.ui.common.Pager
import dev.relony.pinry.ui.common.describe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The user's own boards, newest first. */
class BoardsViewModel(private val boards: BoardRepository, me: String) : ViewModel() {
    val pager = Pager(viewModelScope, key = Board::id) { offset -> boards.page(me, offset) }

    var error by mutableStateOf<String?>(null)
        private set

    init {
        pager.loadMore()
        viewModelScope.launch {
            boards.changes.collect { change ->
                when (change) {
                    is BoardChange.Created -> pager.prepend(change.board)
                    is BoardChange.Updated -> pager.replace(change.board)
                    is BoardChange.Deleted -> pager.remove(change.id)
                    is BoardChange.PinAdded, is BoardChange.PinRemoved -> Unit // followed by Updated
                }
            }
        }
    }

    fun create(name: String) {
        error = null
        viewModelScope.launch {
            try {
                boards.create(name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describe(e)
            }
        }
    }
}
