package io.github.relony.pinry.ui.common

import io.github.relony.pinry.data.api.fieldErrors
import java.io.IOException
import retrofit2.HttpException

/** A message for the user from a failed Pinry call. */
fun describe(e: Exception): String = when (e) {
    is HttpException -> {
        val fields = e.fieldErrors()
        when {
            e.code() == 413 -> "The image is larger than the server accepts (nginx client_max_body_size)."
            "url" in fields -> "The server couldn't download an image from this address."
            fields.isNotEmpty() -> fields.values.first()
            else -> "Server error (HTTP ${e.code()})"
        }
    }
    is IOException -> "Network error: ${e.message ?: e.javaClass.simpleName}"
    else -> e.message ?: e.javaClass.simpleName
}
