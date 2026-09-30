package dev.relony.pinry.data

import dev.relony.pinry.data.api.NewPin
import dev.relony.pinry.data.api.Page
import dev.relony.pinry.data.api.Pin
import dev.relony.pinry.data.api.PinImage
import dev.relony.pinry.data.api.PinUpdate
import dev.relony.pinry.data.api.PinryApi
import dev.relony.pinry.data.net.ProgressRequestBody
import dev.relony.pinry.data.net.withMediaFrom
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody

sealed interface PinChange {
    data class Created(val pin: Pin) : PinChange
    data class Updated(val pin: Pin) : PinChange
    data class Deleted(val id: Int) : PinChange
}

class PinRepository(private val api: () -> PinryApi, private val baseUrl: () -> HttpUrl) {
    /** Pins seen in any feed, so the detail screen can show one without waiting for the network. */
    private val known = ConcurrentHashMap<Int, Pin>()

    private val _changes = MutableSharedFlow<PinChange>(extraBufferCapacity = 16)

    /** Edits made in this app, so open feeds and screens can update without reloading. */
    val changes: SharedFlow<PinChange> = _changes

    suspend fun page(filter: PinFilter, offset: Int): Page<Pin> {
        val page = when (filter) {
            PinFilter.All -> api().pins(offset)
            is PinFilter.Tag -> api().pins(offset, tag = filter.name)
            is PinFilter.User -> api().pins(offset, user = filter.username)
            is PinFilter.Board -> api().pins(offset, board = filter.id)
        }
        val base = baseUrl()
        return page.copy(results = page.results.map { it.withMediaFrom(base) }).also { remember(it.results) }
    }

    fun cached(id: Int): Pin? = known[id]

    suspend fun pin(id: Int): Pin = api().pin(id).withMediaFrom(baseUrl()).also { known[id] = it }

    suspend fun upload(image: LocalImage, onProgress: (Float) -> Unit): PinImage {
        val body = ProgressRequestBody(image.file, image.mimeType.toMediaTypeOrNull(), onProgress)
        return api().uploadImage(MultipartBody.Part.createFormData("image", image.file.name, body))
    }

    suspend fun create(pin: NewPin): Pin =
        api().createPin(pin).withMediaFrom(baseUrl()).also {
            known[it.id] = it
            _changes.emit(PinChange.Created(it))
        }

    suspend fun update(id: Int, update: PinUpdate): Pin =
        api().updatePin(id, update).withMediaFrom(baseUrl()).also {
            known[id] = it
            _changes.emit(PinChange.Updated(it))
        }

    suspend fun delete(id: Int) {
        api().deletePin(id)
        known.remove(id)
        _changes.emit(PinChange.Deleted(id))
    }

    /** The pin's page in Pinry's web app. */
    fun webUrl(id: Int): String = baseUrl().resolve("pins/$id")!!.toString()

    private fun remember(pins: List<Pin>) {
        // Unbounded growth would only matter after tens of thousands of pins; start over well before that.
        if (known.size > 5_000) known.clear()
        pins.forEach { known[it.id] = it }
    }
}
