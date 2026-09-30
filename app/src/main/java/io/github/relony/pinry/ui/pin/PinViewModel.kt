package io.github.relony.pinry.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.relony.pinry.data.PinRepository
import io.github.relony.pinry.data.api.Pin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinViewModel(private val repository: PinRepository, private val id: Int) : ViewModel() {
    /** Shown straight away when the pin was already in a feed; fetched otherwise (e.g. after process death). */
    private val _pin = MutableStateFlow(repository.cached(id))
    val pin: StateFlow<Pin?> = _pin

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed

    val webUrl: String get() = repository.webUrl(id)

    init {
        if (_pin.value == null) load()
    }

    fun load() {
        _failed.value = false
        viewModelScope.launch {
            try {
                _pin.value = repository.pin(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _failed.value = true
            }
        }
    }
}
