package dev.relony.pinry.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.relony.pinry.R
import dev.relony.pinry.ui.common.PinryLoadingIndicator
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

/** Full-screen images with pinch and double-tap zoom; swipe sideways through the feed the pin came from. */
@Composable
fun ImageViewerScreen(vm: ViewerViewModel, pinViewModel: @Composable (Int) -> PinViewModel, onBack: () -> Unit) {
    val pins = vm.pins

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (pins.isEmpty()) {
            Box(Modifier.align(Alignment.Center)) {
                if (vm.failed) TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } else PinryLoadingIndicator()
            }
        } else {
            val pager = rememberPagerState(initialPage = vm.start) { pins.size }
            LaunchedEffect(pager.currentPage, pins.size) { if (pager.currentPage >= pins.size - 3) vm.loadMore() }
            HorizontalPager(pager, beyondViewportPageCount = 1, key = { pins[it].id }) { page ->
                val pin = pins[page]
                ZoomableAsyncImage(
                    model = rememberFullImageRequest(pin),
                    contentDescription = pin.description,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val current = pinViewModel(pins[pager.currentPage.coerceAtMost(pins.lastIndex)].id)
            val download = rememberDownload(current)
            FilledTonalIconButton(
                onClick = { if (!current.downloaded) download() },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            ) {
                DownloadIcon(current)
            }
            current.imageError?.let {
                Text(
                    it,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                )
            }
        }
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
        }
    }
}
