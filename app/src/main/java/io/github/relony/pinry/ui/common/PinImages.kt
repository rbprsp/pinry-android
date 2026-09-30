package io.github.relony.pinry.ui.common

import io.github.relony.pinry.data.api.PinImage

/** The smallest variant at least [widthPx] wide, else the original. `square` is cropped, so never used. */
fun PinImage.urlFor(widthPx: Int): String =
    listOfNotNull(thumbnail, standard)
        .sortedBy { it.width }
        .firstOrNull { it.width >= widthPx && it.width <= width }
        ?.image
        ?: image

/** Width / height for a grid cell; very tall images are cropped to 1:2.5 so one pin can't fill the screen. */
fun PinImage.gridAspectRatio(): Float =
    if (width <= 0 || height <= 0) 1f else (width.toFloat() / height).coerceAtLeast(0.4f)

fun PinImage.aspectRatio(): Float = if (width <= 0 || height <= 0) 1f else width.toFloat() / height

/** Memory-cache key of a pin's grid image; the detail screen shows it while the full image loads. */
fun gridCacheKey(pinId: Int) = "pin-grid-$pinId"

fun gravatarUrl(hash: String?, sizePx: Int): String? =
    hash?.let { "https://www.gravatar.com/avatar/$it?d=identicon&s=$sizePx" }
