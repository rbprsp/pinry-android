package io.github.relony.pinry.data.net

import io.github.relony.pinry.data.api.Board
import io.github.relony.pinry.data.api.ImageSize
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.data.api.PinImage
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Django builds media URLs from the request it received, so behind a reverse proxy they can come
 * back as `http://` or with an internal host. Media paths are moved onto the server the app uses.
 */
fun rebaseMediaUrl(url: String, base: HttpUrl): String {
    val parsed = url.toHttpUrlOrNull() ?: return base.resolve(url)?.toString() ?: url
    if (!parsed.encodedPath.startsWith("/media/")) return url
    return parsed.newBuilder().scheme(base.scheme).host(base.host).port(base.port).build().toString()
}

/** Rebases the image URLs only; the pin's source [Pin.url] points elsewhere and stays as is. */
fun Pin.withMediaFrom(base: HttpUrl): Pin = copy(image = image.withMediaFrom(base))

fun Board.withMediaFrom(base: HttpUrl): Board = copy(cover = cover?.withMediaFrom(base))

private fun PinImage.withMediaFrom(base: HttpUrl) = copy(
    image = rebaseMediaUrl(image, base),
    standard = standard?.withMediaFrom(base),
    thumbnail = thumbnail?.withMediaFrom(base),
    square = square?.withMediaFrom(base),
)

private fun ImageSize.withMediaFrom(base: HttpUrl) = copy(image = rebaseMediaUrl(image, base))
