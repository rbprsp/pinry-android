package io.github.relony.pinry.ui.common

import io.github.relony.pinry.data.api.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagerState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val endReached: Boolean = false,
    val error: Throwable? = null,
)

/**
 * Offset pagination over a list the server orders newest first. Pins added on the server between
 * page loads shift the offsets, so a page may repeat items; those are dropped by [key].
 */
class Pager<T>(
    private val scope: CoroutineScope,
    private val key: (T) -> Any,
    private val load: suspend (offset: Int) -> Page<T>,
) {
    private val _state = MutableStateFlow(PagerState<T>())
    val state: StateFlow<PagerState<T>> = _state

    /** How many items the server has handed out so far, duplicates included. */
    private var offset = 0
    private var job: Job? = null

    fun loadMore() {
        val s = _state.value
        if (job?.isActive == true || s.endReached || s.error != null) return
        job = scope.launch { fetch(reset = false) }
    }

    /** Reloads from the first page. */
    fun refresh() {
        job?.cancel()
        job = scope.launch { fetch(reset = true) }
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        loadMore()
    }

    /** A new item at the top of the server's list: shown now, and the next page starts one later. */
    fun prepend(item: T) {
        val itemKey = key(item)
        if (_state.value.items.any { key(it) == itemKey }) return
        _state.update { it.copy(items = listOf(item) + it.items) }
        offset++
    }

    fun replace(item: T) {
        val itemKey = key(item)
        _state.update { s -> s.copy(items = s.items.map { if (key(it) == itemKey) item else it }) }
    }

    /** Deleted on the server: everything after it moved up by one, so the next page starts one earlier. */
    fun remove(itemKey: Any) {
        if (_state.value.items.none { key(it) == itemKey }) return
        _state.update { s -> s.copy(items = s.items.filterNot { key(it) == itemKey }) }
        offset--
    }

    private suspend fun fetch(reset: Boolean) {
        val from = if (reset) 0 else offset
        _state.update { it.copy(loading = true, refreshing = reset, error = null) }
        try {
            val page = load(from)
            offset = from + page.results.size
            _state.update { s ->
                val kept = if (reset) emptyList() else s.items
                val seen = kept.mapTo(HashSet(), key)
                s.copy(
                    items = kept + page.results.filter { seen.add(key(it)) },
                    loading = false,
                    refreshing = false,
                    endReached = page.next == null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, refreshing = false, error = e) }
        }
    }
}
