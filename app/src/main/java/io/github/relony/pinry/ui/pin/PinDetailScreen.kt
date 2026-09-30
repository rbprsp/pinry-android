package io.github.relony.pinry.ui.pin

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import io.github.relony.pinry.R
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.ui.common.PinryLoadingIndicator
import io.github.relony.pinry.ui.common.aspectRatio
import io.github.relony.pinry.ui.common.gravatarUrl
import io.github.relony.pinry.ui.common.gridCacheKey

@Composable
fun PinDetailScreen(
    vm: PinViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenImage: () -> Unit,
    onTag: (String) -> Unit,
    onUser: (String) -> Unit,
) {
    val pin by vm.pin.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val deleteFailed by vm.deleteFailed.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Box(Modifier.fillMaxSize()) {
        when (val p = pin) {
            null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                if (failed) TextButton(onClick = vm::load) { Text(stringResource(R.string.retry)) } else PinryLoadingIndicator()
            }
            else -> PinDetails(
                pin = p,
                webUrl = vm.webUrl,
                ownActions = if (vm.isMine(p)) OwnActions(onEdit, onDelete = { confirmDelete = true }, deleteFailed) else null,
                onOpenImage = onOpenImage,
                onTag = onTag,
                onUser = onUser,
            )
        }
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.pin_delete_confirm_title)) },
            text = { Text(stringResource(R.string.pin_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text(stringResource(R.string.pin_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private class OwnActions(val onEdit: () -> Unit, val onDelete: () -> Unit, val deleteFailed: Boolean)

@Composable
private fun PinDetails(
    pin: Pin,
    webUrl: String,
    ownActions: OwnActions?,
    onOpenImage: () -> Unit,
    onTag: (String) -> Unit,
    onUser: (String) -> Unit,
) {
    val context = LocalContext.current
    val source = pin.referer ?: pin.url

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AsyncImage(
            model = rememberFullImageRequest(pin),
            contentDescription = pin.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(pin.image.aspectRatio())
                .clickable(onClick = onOpenImage),
        )
        Column(
            Modifier.padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (source != null) {
                    FilledTonalButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, source.toUri()))
                    }) {
                        Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.pin_open_source))
                    }
                }
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, webUrl)
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.pin_share))
                }
                if (ownActions != null) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = ownActions.onEdit) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.pin_edit))
                    }
                    IconButton(onClick = ownActions.onDelete) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.pin_delete))
                    }
                }
            }
            if (ownActions?.deleteFailed == true) {
                Text(stringResource(R.string.pin_delete_failed), color = MaterialTheme.colorScheme.error)
            }
            val description = pin.description?.trim()
            if (!description.isNullOrEmpty()) {
                Text(description, style = MaterialTheme.typography.bodyLarge)
            }
            if (pin.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pin.tags.forEach { tag -> AssistChip(onClick = { onTag(tag) }, label = { Text("#$tag") }) }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onUser(pin.submitter.username) }
                    .padding(vertical = 4.dp),
            ) {
                AsyncImage(
                    model = gravatarUrl(pin.submitter.gravatar, 96),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
                Text(pin.submitter.username, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** The original image, showing the grid's cached copy until it arrives. */
@Composable
fun rememberFullImageRequest(pin: Pin): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(pin.id) {
        ImageRequest.Builder(context)
            .data(pin.image.image)
            .placeholderMemoryCacheKey(gridCacheKey(pin.id))
            .build()
    }
}
