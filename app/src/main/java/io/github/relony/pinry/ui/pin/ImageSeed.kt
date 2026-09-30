package io.github.relony.pinry.ui.pin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The most vibrant (else dominant) color of an image, from a 64 px software decode so it stays
 * cheap next to the full image. Null until known.
 */
@Composable
fun rememberImageSeed(url: String?): Color? {
    val context = LocalPlatformContext.current
    var seed by remember(url) { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(url).size(64).allowHardware(false).build()
        val bitmap = (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap() ?: return@LaunchedEffect
        seed = withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).generate()
            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let(::Color)
        }
    }
    return seed
}
