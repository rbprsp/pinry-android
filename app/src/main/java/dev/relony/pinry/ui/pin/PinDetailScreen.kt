package dev.relony.pinry.ui.pin

import android.content.ActivityNotFoundException
import android.content.Intent
import dev.relony.pinry.data.imageFileName
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.relony.pinry.ui.common.Action
import dev.relony.pinry.ui.common.ActionGroup
import dev.relony.pinry.ui.common.PinCardCorner
import dev.relony.pinry.ui.common.avatarShape
import dev.relony.pinry.ui.common.sharedPinImage
import dev.relony.pinry.ui.theme.LocalAppSettings
import dev.relony.pinry.ui.theme.SeededTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import dev.relony.pinry.ui.common.PinGrid
import dev.relony.pinry.ui.common.LocalToolbarInset
import dev.relony.pinry.ui.common.Pager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
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
import dev.relony.pinry.R
import dev.relony.pinry.data.api.BoardName
import dev.relony.pinry.data.api.Pin
import dev.relony.pinry.ui.create.NewBoardDialog
import dev.relony.pinry.ui.common.PinryLoadingIndicator
import dev.relony.pinry.ui.common.aspectRatio
import dev.relony.pinry.ui.common.gravatarUrl
import dev.relony.pinry.ui.common.gridCacheKey
import dev.relony.pinry.ui.common.tagLabel

@Composable
fun PinDetailScreen(
    vm: PinViewModel,
    fromBoard: BoardName?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenImage: () -> Unit,
    onTag: (String) -> Unit,
    onUser: (String) -> Unit,
    onOpen: (Pin) -> Unit,
) {
    val pin by vm.pin.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val deleteFailed by vm.deleteFailed.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickingBoard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(deleted) { if (deleted) onBack() }
    val saveAs = rememberLauncherForActivityResult(CreateDocument("image/*")) { uri -> if (uri != null) vm.downloadTo(uri) }
    val seed = rememberImageSeed(pin?.let { it.image.thumbnail?.image ?: it.image.image })

    // The screen takes its colors from the image once they're known.
    SeededTheme(seed) {
    Surface(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize()) {
        when (val p = pin) {
            null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                if (failed) TextButton(onClick = vm::load) { Text(stringResource(R.string.retry)) } else PinryLoadingIndicator()
            }
            else -> RelatedGrid(vm.related(p), onOpen) {
                PinDetails(
                    pin = p,
                    webUrl = vm.webUrl,
                    ownActions = if (vm.isMine(p)) OwnActions(onEdit, onDelete = { confirmDelete = true }, deleteFailed) else null,
                    boardActions = BoardActions(
                        onSave = if (vm.canSave) ({ vm.loadBoards(); pickingBoard = true }) else null,
                        removeFrom = fromBoard?.takeIf { vm.removedFrom != it },
                        onRemove = { vm.removeFrom(it) },
                        message = vm.boardError
                            ?: vm.removedFrom?.let { stringResource(R.string.pin_removed_from_board, it.name) }
                            ?: vm.savedTo?.let { stringResource(R.string.pin_saved_to, it.name) },
                    ),
                    onOpenImage = onOpenImage,
                    onDownload = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) vm.download() else saveAs.launch(imageFileName(p.image.image, p.id))
                    },
                    downloading = vm.downloading,
                    downloaded = vm.downloaded,
                    downloadError = vm.downloadError,
                    onTag = onTag,
                    onUser = onUser,
                )
            }
        }
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
        }
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

private class BoardActions(
    /** Null without an account. */
    val onSave: (() -> Unit)?,
    /** The board this pin was opened from, when it can be removed from it. */
    val removeFrom: BoardName?,
    val onRemove: (BoardName) -> Unit,
    val message: String?,
)

@Composable
private fun PinDetails(
    pin: Pin,
    webUrl: String,
    ownActions: OwnActions?,
    boardActions: BoardActions,
    onOpenImage: () -> Unit,
    onDownload: () -> Unit,
    downloading: Boolean,
    downloaded: Boolean,
    downloadError: String?,
    onTag: (String) -> Unit,
    onUser: (String) -> Unit,
) {
    val context = LocalContext.current
    // Pin fields are written by any user of the server: only web links are opened.
    val source = (pin.referer ?: pin.url)?.takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }

    // Square once shown; rounded like the grid card while flying in from it.
    val corner by LocalNavAnimatedContentScope.current.transition.animateDp(label = "corner") { state ->
        if (state == EnterExitState.Visible) 0.dp else PinCardCorner
    }
    Column(Modifier.bleed(GridEdge)) {
        AsyncImage(
            model = rememberFullImageRequest(pin),
            contentDescription = pin.description?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.pin_by, pin.submitter.username),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .sharedPinImage(pin.id, corner)
                .clip(RoundedCornerShape(corner))
                .fillMaxWidth()
                .aspectRatio(pin.image.aspectRatio())
                .clickable(onClick = onOpenImage),
        )
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionGroup(
                buildList {
                    boardActions.onSave?.let { add(Action(R.drawable.ic_bookmark, stringResource(R.string.pin_save), it, primary = true)) }
                    add(Action(R.drawable.ic_share, stringResource(R.string.pin_share), {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, webUrl)
                        context.startActivity(Intent.createChooser(send, null))
                    }))
                    if (downloaded) {
                        val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.string.pin_downloaded else R.string.pin_downloaded_file
                        add(Action(R.drawable.ic_check, stringResource(saved), {}))
                    } else {
                        val label = stringResource(if (downloading) R.string.pin_downloading else R.string.pin_download)
                        add(Action(R.drawable.ic_download, label, onDownload, loading = downloading))
                    }
                    if (source != null) {
                        add(Action(R.drawable.ic_open_in_new, stringResource(R.string.pin_open_source), {
                            try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, source.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // No browser installed; nothing sensible to do.
                        }
                        }))
                    }
                    if (ownActions != null) {
                        add(Action(R.drawable.ic_edit, stringResource(R.string.pin_edit), ownActions.onEdit))
                        add(Action(R.drawable.ic_delete, stringResource(R.string.pin_delete), ownActions.onDelete))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (ownActions?.deleteFailed == true) {
                Text(stringResource(R.string.pin_delete_failed), color = MaterialTheme.colorScheme.error)
            }
            boardActions.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            boardActions.removeFrom?.let { board ->
                TextButton(onClick = { boardActions.onRemove(board) }) {
                    Text(stringResource(R.string.pin_remove_from_board, board.name))
                }
            }
            val description = pin.description?.trim()
            if (!description.isNullOrEmpty()) {
                Text(description, style = MaterialTheme.typography.bodyLarge)
            }
            if (pin.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pin.tags.forEach { tag -> AssistChip(onClick = { onTag(tag) }, label = { Text(tagLabel(tag)) }) }
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
                if (LocalAppSettings.current.gravatar) {
                    AsyncImage(
                        model = gravatarUrl(pin.submitter.gravatar, 96),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(avatarShape()),
                    )
                } else {
                    Box(
                        Modifier.size(40.dp).clip(avatarShape()).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            pin.submitter.username.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(pin.submitter.username, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            if (pin.tags.isNotEmpty()) {
                Text(stringResource(R.string.pin_related), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private val GridEdge = 8.dp

@Composable
private fun RelatedGrid(related: Pager<Pin>, onOpen: (Pin) -> Unit, header: @Composable () -> Unit) {
    val state by related.state.collectAsStateWithLifecycle()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalToolbarInset.current
    PinGrid(
        state = state,
        onLoadMore = related::loadMore,
        onRetry = related::retry,
        onOpen = onOpen,
        contentPadding = PaddingValues(start = GridEdge, end = GridEdge, bottom = bottom + GridEdge),
        header = header,
    )
}

private fun Modifier.bleed(edge: Dp) = layout { measurable, constraints ->
    val extra = edge.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
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

@Composable
private fun SaveToBoardDialog(
    boards: List<BoardName>?,
    onPick: (BoardName) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    if (creating) {
        NewBoardDialog(onDismiss = { creating = false }, onCreate = onCreate)
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_save_to)) },
        text = {
            if (boards == null) {
                Box(Modifier.fillMaxWidth(), Alignment.Center) { PinryLoadingIndicator() }
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(boards, key = { it.id }) { board ->
                        ListItem(headlineContent = { Text(board.name) }, modifier = Modifier.clickable { onPick(board) })
                    }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.board_new), color = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { creating = true },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
