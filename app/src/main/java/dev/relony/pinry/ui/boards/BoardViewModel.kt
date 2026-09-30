package dev.relony.pinry.ui.boards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.relony.pinry.data.BoardChange
import dev.relony.pinry.data.BoardRepository
import dev.relony.pinry.data.api.Board
import dev.relony.pinry.ui.common.describe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** One board's settings: rename, private/public, delete. */
class BoardViewModel(private val boards: BoardRepository, private val id: Int, private val me: String) : ViewModel() {
    var board by mutableStateOf<Board?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var deleted by mutableStateOf(false)
        private set

    val isMine get() = board?.submitter?.username == me

    init {
        launchAction { board = boards.board(id) }
        viewModelScope.launch {
            boards.changes.collect { change ->
                if (change is BoardChange.Updated && change.board.id == id) board = change.board
            }
        }
    }

    fun rename(name: String) = launchAction { boards.rename(id, name) }

    fun setPrivate(isPrivate: Boolean) = launchAction { boards.setPrivate(id, isPrivate) }

    fun delete() = launchAction {
        boards.delete(id)
        deleted = true
    }

    private fun launchAction(action: suspend () -> Unit) {
        error = null
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describe(e)
            }
        }
    }
}
