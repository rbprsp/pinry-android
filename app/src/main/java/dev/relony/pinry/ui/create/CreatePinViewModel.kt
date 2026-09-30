package dev.relony.pinry.ui.create

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.relony.pinry.AppContainer
import dev.relony.pinry.data.BoardRepository
import dev.relony.pinry.data.ImageSource
import dev.relony.pinry.data.ImageUrlResolver
import dev.relony.pinry.data.LocalImage
import dev.relony.pinry.data.LocalImages
import dev.relony.pinry.data.PinRepository
import dev.relony.pinry.data.TagRepository
import dev.relony.pinry.data.api.BoardName
import dev.relony.pinry.data.api.NewPin
import dev.relony.pinry.data.api.Pin
import dev.relony.pinry.data.firstUrl
import dev.relony.pinry.ui.common.describe
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Where the new pin's image comes from. */
@Serializable
sealed interface CreateSource {
    /** A content:// URI from the photo picker or another app's share. */
    @Serializable data class Local(val uri: String) : CreateSource

    /** An image or page URL, typed or shared; may start empty. */
    @Serializable data class Url(val url: String) : CreateSource
}

class CreatePinViewModel(
    private val context: Context,
    private val pins: PinRepository,
    private val boards: BoardRepository,
    private val tagRepository: TagRepository,
    private val imageUrls: ImageUrlResolver,
    private val me: String,
    private val source: CreateSource,
) : ViewModel() {
    val form = PinFormState()
    val fromUrl = source is CreateSource.Url
    var url by mutableStateOf((source as? CreateSource.Url)?.url.orEmpty())

    var localImage by mutableStateOf<LocalImage?>(null)
        private set
    var resolved by mutableStateOf<ImageSource?>(null)
        private set
    var resolving by mutableStateOf(false)
        private set
    var noImageFound by mutableStateOf(false)
        private set

    var board by mutableStateOf<BoardName?>(null)
    var boardList by mutableStateOf(emptyList<BoardName>())
        private set
    var allTags by mutableStateOf(emptyList<String>())
        private set

    /** Upload progress 0..1 while the image is being sent, else null. */
    var progress by mutableStateOf<Float?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf(false)
        private set

    // Kept across retries so a failure halfway through never uploads or creates twice.
    private var uploadedImageId: Int? = null
    private var createdPin: Pin? = null

    val canSubmit get() = !saving && (localImage != null || resolved != null)

    init {
        when (source) {
            is CreateSource.Local -> viewModelScope.launch {
                try {
                    localImage = withContext(Dispatchers.IO) { LocalImages.copy(context, source.uri.toUri()) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = "Couldn't read this image (${e.message ?: e.javaClass.simpleName})"
                }
            }
            is CreateSource.Url -> viewModelScope.launch { resolveWhileTyping() }
        }
        viewModelScope.launch { boardList = orEmpty { boards.names(me) } }
        viewModelScope.launch { allTags = orEmpty { tagRepository.all() } }
    }

    @OptIn(FlowPreview::class)
    private suspend fun resolveWhileTyping() {
        snapshotFlow { url }.debounce(500).collectLatest { text ->
            resolved = null
            noImageFound = false
            val target = firstUrl(text) ?: return@collectLatest
            resolving = true
            try {
                resolved = imageUrls.resolve(target)
            } catch (e: IOException) {
                resolved = null
            } finally {
                resolving = false
            }
            noImageFound = resolved == null
        }
    }

    fun createBoard(name: String) {
        viewModelScope.launch {
            try {
                val created = boards.create(name)
                val entry = BoardName(created.id, created.name)
                boardList = boardList + entry
                board = entry
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describe(e)
            }
        }
    }

    fun submit() {
        if (!canSubmit) return
        form.commitTagInput()
        saving = true
        error = null
        viewModelScope.launch {
            try {
                val pin = createdPin ?: createPin().also { createdPin = it }
                board?.let { boards.addPin(it.id, pin.id) }
                if (form.tags.any { it !in allTags }) tagRepository.forget()
                localImage?.file?.delete()
                done = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describe(e)
            } finally {
                saving = false
                progress = null
            }
        }
    }

    private suspend fun createPin(): Pin {
        val description = form.description.trim().ifEmpty { null }
        val image = localImage
        if (image != null) {
            val imageId = uploadedImageId
                ?: pins.upload(image) { progress = it }.id.also { uploadedImageId = it }
            progress = null
            return pins.create(NewPin(imageId = imageId, description = description, isPrivate = form.isPrivate, tags = form.tags))
        }
        val source = checkNotNull(resolved)
        return pins.create(
            NewPin(url = source.imageUrl, referer = source.pageUrl, description = description, isPrivate = form.isPrivate, tags = form.tags)
        )
    }

    private suspend fun <T> orEmpty(load: suspend () -> List<T>): List<T> = try {
        load()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }
}

fun AppContainer.createPinViewModel(context: Context, me: String, source: CreateSource) =
    CreatePinViewModel(context.applicationContext, pins, boards, tags, imageUrls, me, source)
