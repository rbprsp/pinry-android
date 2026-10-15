package dev.relony.pinry.ui.pin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.relony.pinry.data.FeedPins
import dev.relony.pinry.data.PinFilter
import dev.relony.pinry.data.PinRepository
import dev.relony.pinry.data.api.Pin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The pins the full-screen viewer can swipe through: the feed the pin was opened from, or just the pin. */
class ViewerViewModel(private val repository: PinRepository, id: Int, source: PinFilter?) : ViewModel() {
    private val feed = source?.let { filter ->
        repository.order(filter)?.let { ids -> FeedPins(ids, repository::cached) { repository.page(filter, it) } }
    }?.takeIf { feed -> feed.pins.any { it.id == id } }

    var pins by mutableStateOf(feed?.pins ?: listOfNotNull(repository.cached(id)))
        private set
    val start = pins.indexOfFirst { it.id == id }.coerceAtLeast(0)
    var failed by mutableStateOf(false)
        private set
    private var loading = false

    init {
        if (pins.isEmpty()) {
            viewModelScope.launch {
                try {
                    pins = listOf(repository.pin(id))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed = true
                }
            }
        }
    }

    fun loadMore() {
        val feed = feed ?: return
        if (loading || feed.endReached) return
        loading = true
        viewModelScope.launch {
            try {
                feed.loadMore()
                pins = feed.pins
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Swiping near the end again retries.
            } finally {
                loading = false
            }
        }
    }
}
