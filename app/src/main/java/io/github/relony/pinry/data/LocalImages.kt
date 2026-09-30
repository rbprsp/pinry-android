package io.github.relony.pinry.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.math.max

data class LocalImage(val file: File, val mimeType: String)

/**
 * Copies a picked or shared image into the app cache straight away: URI grants from other apps
 * are temporary. Two kinds of photos are re-encoded as upright JPEG:
 * - HEIC/HEIF, which the stock Pinry server can't read;
 * - JPEGs with an EXIF rotation (phone portraits): Pinry stores their sideways width/height,
 *   so every client would lay them out as landscape and crop them.
 */
object LocalImages {
    private const val MAX_CONVERTED_SIDE = 4096

    fun copy(context: Context, uri: Uri): LocalImage {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val canConvert = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val heic = canConvert && mime.startsWith("image/hei")
        // Rotated JPEGs keep their type, so the file name is the same whether or not it gets converted.
        val type = if (heic) "image/jpeg" else mime
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: "jpg"
        // Same URI → same file name, so the copy is found again after process death.
        val target = File(File(context.cacheDir, "uploads").apply { mkdirs() }, "image-${uri.toString().hashCode().toUInt()}.$extension")

        try {
            val convert = heic || (canConvert && mime == "image/jpeg" && isRotated(resolver, uri))
            if (convert) convertToJpeg(resolver, uri, target) else copyStream(resolver, uri, target)
        } catch (e: Exception) {
            // After process death the grant is gone (SecurityException); the earlier copy is still good.
            if (!target.exists()) throw e
        }
        return LocalImage(target, type)
    }

    private fun isRotated(resolver: ContentResolver, uri: Uri): Boolean {
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: return false
        return orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED
    }

    private fun copyStream(resolver: ContentResolver, uri: Uri, target: File) {
        val input = resolver.openInputStream(uri) ?: throw IOException("Can't open $uri")
        input.use { source -> writeAtomically(target) { source.copyTo(it) } }
    }

    /** ImageDecoder applies the EXIF orientation, so the JPEG written here is upright. */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun convertToJpeg(resolver: ContentResolver, uri: Uri, target: File) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_CONVERTED_SIDE) {
                val scale = MAX_CONVERTED_SIDE.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
        writeAtomically(target) { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }

    private fun writeAtomically(target: File, write: (OutputStream) -> Unit) {
        val partial = File(target.parentFile, target.name + ".part")
        partial.outputStream().use(write)
        if (!partial.renameTo(target)) throw IOException("Can't write ${target.name}")
    }
}
