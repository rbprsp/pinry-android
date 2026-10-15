@file:OptIn(ExperimentalMaterial3Api::class)

package dev.relony.pinry.ui.pin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.relony.pinry.R
import dev.relony.pinry.ui.common.ActionIcon

/** Save, download and share a pin from a grid without opening it. */
@Composable
fun PinQuickActions(vm: PinViewModel, onDismiss: () -> Unit) {
    val download = rememberDownload(vm)
    var pickingBoard by rememberSaveable { mutableStateOf(false) }
    ShareSheetEffect(vm, onStarted = onDismiss)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 16.dp)) {
            vm.pin.collectAsStateWithLifecycle().value?.let { pin ->
                ListItem(
                    headlineContent = {
                        Text(
                            pin.description?.trim()?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.pin_by, pin.submitter.username),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = pin.image.thumbnail?.image ?: pin.image.image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                        )
                    },
                )
            }
            if (vm.canSave) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pin_save_to)) },
                    leadingContent = { ActionIcon(R.drawable.ic_bookmark, null) },
                    modifier = Modifier.clickable { vm.loadBoards(); pickingBoard = true },
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.pin_download)) },
                leadingContent = { DownloadIcon(vm) },
                modifier = Modifier.clickable { if (!vm.downloaded) download() },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.pin_share)) },
                leadingContent = { ActionIcon(if (vm.sharing) null else R.drawable.ic_share, null) },
                modifier = Modifier.clickable(onClick = vm::share),
            )
            val message = vm.boardError ?: vm.imageError
            val saved = vm.savedTo?.let { stringResource(R.string.pin_saved_to, it.name) }
            (message ?: saved)?.let {
                Text(
                    it,
                    color = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (pickingBoard) {
        SaveToBoardDialog(
            boards = vm.myBoards,
            onPick = { vm.saveTo(it); pickingBoard = false },
            onCreate = { vm.createBoardAndSave(it); pickingBoard = false },
            onDismiss = { pickingBoard = false },
        )
    }
}
