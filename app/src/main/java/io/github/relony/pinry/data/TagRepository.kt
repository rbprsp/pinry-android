package io.github.relony.pinry.data

import io.github.relony.pinry.data.api.PinryApi

class TagRepository(private val api: () -> PinryApi) {
    @Volatile private var cached: List<String>? = null

    /** Every tag on the server, for autocomplete. The server caches this list for 5 minutes anyway. */
    suspend fun all(): List<String> =
        cached ?: api().tags().map { it.name }.distinct().sortedBy { it.lowercase() }.also { cached = it }

    fun forget() {
        cached = null
    }
}
