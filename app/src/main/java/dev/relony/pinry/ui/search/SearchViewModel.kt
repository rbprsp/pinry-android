package dev.relony.pinry.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.relony.pinry.data.BoardChange
import dev.relony.pinry.data.BoardRepository
import dev.relony.pinry.data.TagRepository
import dev.relony.pinry.data.api.Board
import dev.relony.pinry.data.matchingTags
import dev.relony.pinry.ui.common.describe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val tagRepository: TagRepository, private val boardRepository: BoardRepository) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var tags by mutableStateOf<List<String>>(emptyList())
        private set
    var boards by mutableStateOf<List<Board>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var allTags: List<String>? = null
    private var job: Job? = null

    init {
        viewModelScope.launch {
            boardRepository.changes.collect { change ->
                when (change) {
                    is BoardChange.Deleted -> boards = boards.filterNot { it.id == change.id }
                    is BoardChange.Updated -> boards = boards.map { if (it.id == change.board.id) change.board else it }
                    else -> Unit
                }
            }
        }
    }

    fun onQueryChange(text: String) {
        query = text
        job?.cancel()
        error = null
        if (text.isBlank()) {
            tags = emptyList()
            boards = emptyList()
            searching = false
            return
        }
        searching = true
        job = viewModelScope.launch {
            try {
                val known = allTags ?: tagRepository.all().also { allTags = it }
                tags = matchingTags(known, text)
                // Wait for a pause in typing before asking the server about boards.
                delay(300)
                boards = boardRepository.search(text.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describe(e)
            } finally {
                if (query == text) searching = false
            }
        }
    }
}
