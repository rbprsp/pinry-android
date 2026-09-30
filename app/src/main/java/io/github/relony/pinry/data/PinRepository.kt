package io.github.relony.pinry.data

import io.github.relony.pinry.data.api.Page
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.data.api.PinryApi
import io.github.relony.pinry.data.net.withMediaFrom
import java.util.concurrent.ConcurrentHashMap
import okhttp3.HttpUrl

class PinRepository(private val api: () -> PinryApi, private val baseUrl: () -> HttpUrl) {
    /** Pins seen in any feed, so the detail screen can show one without waiting for the network. */
    private val known = ConcurrentHashMap<Int, Pin>()

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

    /** The pin's page in Pinry's web app. */
    fun webUrl(id: Int): String = baseUrl().resolve("pins/$id")!!.toString()

    private fun remember(pins: List<Pin>) {
        // Unbounded growth would only matter after tens of thousands of pins; start over well before that.
        if (known.size > 5_000) known.clear()
        pins.forEach { known[it.id] = it }
    }
}
