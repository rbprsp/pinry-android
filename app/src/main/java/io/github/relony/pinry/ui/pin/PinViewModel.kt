package io.github.relony.pinry.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.relony.pinry.data.PinChange
import io.github.relony.pinry.data.PinRepository
import io.github.relony.pinry.data.api.Pin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinViewModel(
    private val repository: PinRepository,
    private val id: Int,
    private val me: String,
) : ViewModel() {
    /** Shown straight away when the pin was already in a feed; fetched otherwise (e.g. after process death). */
    private val _pin = MutableStateFlow(repository.cached(id))
    val pin: StateFlow<Pin?> = _pin

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed

    /** Set once the pin is gone, so the screen can close. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    private val _deleteFailed = MutableStateFlow(false)
    val deleteFailed: StateFlow<Boolean> = _deleteFailed

    val webUrl: String get() = repository.webUrl(id)

    fun isMine(pin: Pin) = pin.submitter.username == me

    init {
        if (_pin.value == null) load()
        viewModelScope.launch {
            repository.changes.collect { change ->
                when (change) {
                    is PinChange.Updated -> if (change.pin.id == id) _pin.value = change.pin
                    is PinChange.Deleted -> if (change.id == id) _deleted.value = true
                    is PinChange.Created -> Unit
                }
            }
        }
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

    fun delete() {
        _deleteFailed.value = false
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _deleteFailed.value = true
            }
        }
    }
}
