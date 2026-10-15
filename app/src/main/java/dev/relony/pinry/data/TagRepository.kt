package dev.relony.pinry.data

import dev.relony.pinry.data.api.PinryApi

class TagRepository(private val api: () -> PinryApi) {
    @Volatile private var cached: List<String>? = null

    /** Every tag on the server, for autocomplete. The server caches this list for 5 minutes anyway. */
    suspend fun all(): List<String> =
        cached ?: api().tags().map { it.name }.distinct().sortedBy { it.lowercase() }.also { cached = it }

    fun forget() {
        cached = null
    }
}

/** Tags containing [query], ignoring case; those starting with it come first. */
fun matchingTags(tags: List<String>, query: String, limit: Int = 20): List<String> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val (prefix, inside) = tags.filter { it.contains(q, ignoreCase = true) }.partition { it.startsWith(q, ignoreCase = true) }
    return (prefix + inside).take(limit)
}
