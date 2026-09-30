package io.github.relony.pinry.ui.pin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.relony.pinry.data.PinRepository
import io.github.relony.pinry.data.TagRepository
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.data.api.PinUpdate
import io.github.relony.pinry.ui.common.describe
import io.github.relony.pinry.ui.create.PinFormState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class EditPinViewModel(
    private val pins: PinRepository,
    private val tagRepository: TagRepository,
    private val id: Int,
) : ViewModel() {
    /** Null until the pin is loaded. */
    var form by mutableStateOf(pins.cached(id)?.let(::formFor))
        private set
    var allTags by mutableStateOf(emptyList<String>())
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf(false)
        private set

    init {
        if (form == null) {
            viewModelScope.launch {
                try {
                    form = formFor(pins.pin(id))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = describe(e)
                }
            }
        }
        viewModelScope.launch {
            allTags = try {
                tagRepository.all()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun save() {
        val form = form ?: return
        form.commitTagInput()
        saving = true
        error = null
        viewModelScope.launch {
            try {
                // Empty description is sent as "" so clearing it works; tags always go along (see PinUpdate).
                pins.update(id, PinUpdate(tags = form.tags, description = form.description.trim(), isPrivate = form.isPrivate))
                if (form.tags.any { it !in allTags }) tagRepository.forget()
                done = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describe(e)
            } finally {
                saving = false
            }
        }
    }

    private fun formFor(pin: Pin) = PinFormState(pin.description.orEmpty(), pin.tags, pin.isPrivate)
}
