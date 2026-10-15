package dev.relony.pinry.ui.pin

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.relony.pinry.R
import dev.relony.pinry.ui.common.ActionIcon
import dev.relony.pinry.ui.common.PinryLoadingIndicator
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

/** Full-screen image with pinch and double-tap zoom. */
@Composable
fun ImageViewerScreen(vm: PinViewModel, onBack: () -> Unit) {
    val pin by vm.pin.collectAsStateWithLifecycle()
    val download = rememberDownload(vm)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val p = pin
        if (p == null) {
            PinryLoadingIndicator(Modifier.align(Alignment.Center))
        } else {
            ZoomableAsyncImage(
                model = rememberFullImageRequest(p),
                contentDescription = p.description,
                modifier = Modifier.fillMaxSize(),
            )
        }
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
        }
        if (p != null) {
            FilledTonalIconButton(
                onClick = { if (!vm.downloaded) download() },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            ) {
                val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.string.pin_downloaded else R.string.pin_downloaded_file
                ActionIcon(
                    icon = when {
                        vm.downloading -> null
                        vm.downloaded -> R.drawable.ic_check
                        else -> R.drawable.ic_download
                    },
                    contentDescription = stringResource(
                        when {
                            vm.downloading -> R.string.pin_downloading
                            vm.downloaded -> saved
                            else -> R.string.pin_download
                        },
                    ),
                )
            }
        }
        vm.imageError?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            )
        }
    }
}
