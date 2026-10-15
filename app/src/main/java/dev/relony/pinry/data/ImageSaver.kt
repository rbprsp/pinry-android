package dev.relony.pinry.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

/** Saves a pin's image, fetched from the user's own server, to the device. */
class ImageSaver(context: Context, private val http: OkHttpClient) {
    private val resolver = context.applicationContext.contentResolver

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveToPictures(url: String, pinId: Int) = withContext(Dispatchers.IO) {
        fetch(url) { body ->
            val name = imageFileName(url, pinId)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, imageMimeType(body.contentType()?.let { "${it.type}/${it.subtype}" }, name))
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pinry")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val item = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create the image file")
            try {
                write(body, item)
                resolver.update(item, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(item, null, null)
                throw e
            }
        }
    }

    suspend fun saveTo(url: String, target: Uri) = withContext(Dispatchers.IO) {
        fetch(url) { body -> write(body, target) }
    }

    private fun fetch(url: String, use: (ResponseBody) -> Unit) {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            use(response.body)
        }
    }

    private fun write(body: ResponseBody, target: Uri) {
        val out = resolver.openOutputStream(target) ?: throw IOException("Could not open the image file")
        out.use { body.byteStream().copyTo(it) }
    }
}

private val ImageTypes = mapOf(
    "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png", "gif" to "image/gif", "webp" to "image/webp",
    "heic" to "image/heic", "heif" to "image/heif", "avif" to "image/avif", "bmp" to "image/bmp",
)

/** The server's file name for the image, or `pin-<id>.jpg` when it has none that looks like an image. */
fun imageFileName(url: String, pinId: Int): String {
    val name = url.toHttpUrlOrNull()?.pathSegments?.lastOrNull().orEmpty()
    return if (name.substringAfterLast('.', "").lowercase() in ImageTypes) name else "pin-$pinId.jpg"
}

/** MediaStore only accepts image types; some servers send media files as `application/octet-stream`. */
fun imageMimeType(contentType: String?, fileName: String): String =
    contentType?.takeIf { it.startsWith("image/") }
        ?: ImageTypes[fileName.substringAfterLast('.', "").lowercase()]
        ?: "image/jpeg"
