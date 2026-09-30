package io.github.relony.pinry.data.net

import java.io.File
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import okio.source

/** Streams [file] and reports the uploaded fraction (0..1). */
class ProgressRequestBody(
    private val file: File,
    private val type: MediaType?,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = type
    override fun contentLength() = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1)
        var sent = 0L
        file.source().buffer().use { source ->
            while (true) {
                val read = source.read(sink.buffer, 64 * 1024)
                if (read == -1L) break
                sink.emit()
                sent += read
                onProgress(sent.toFloat() / total)
            }
        }
    }
}
