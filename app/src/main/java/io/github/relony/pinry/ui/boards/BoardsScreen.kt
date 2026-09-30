package io.github.relony.pinry.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.relony.pinry.R
import io.github.relony.pinry.data.api.Board
import io.github.relony.pinry.ui.common.LocalToolbarInset
import io.github.relony.pinry.ui.common.PinryLoadingIndicator
import io.github.relony.pinry.ui.common.RefreshBox
import io.github.relony.pinry.ui.common.urlFor
import io.github.relony.pinry.ui.create.NewBoardDialog

@Composable
fun BoardsScreen(vm: BoardsViewModel, onOpen: (Board) -> Unit) {
    val state by vm.pager.state.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalToolbarInset.current
    val fullLine: (Any) -> GridItemSpan = { GridItemSpan(Int.MAX_VALUE) }

    RefreshBox(state.refreshing, vm.pager::refresh, Modifier.fillMaxSize(), indicatorTopPadding = statusBar) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = statusBar + 8.dp, bottom = bottom + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tab_boards), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = { creating = true }) { Text(stringResource(R.string.board_new_button)) }
                }
            }
            vm.error?.let { error ->
                item(key = "error", span = { GridItemSpan(maxLineSpan) }) { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            items(state.items, key = { it.id }) { board ->
                BoardCard(board, onClick = { onOpen(board) }, Modifier.animateItem())
            }
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    when {
                        state.error != null -> TextButton(onClick = vm.pager::retry) { Text(stringResource(R.string.retry)) }
                        !state.endReached -> {
                            // Composed only when scrolled into view, so this pages in the next boards.
                            LaunchedEffect(state.items.size) { vm.pager.loadMore() }
                            PinryLoadingIndicator()
                        }
                        state.items.isEmpty() -> Text(stringResource(R.string.boards_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
    if (creating) {
        NewBoardDialog(onDismiss = { creating = false }, onCreate = { vm.create(it); creating = false })
    }
}

@Composable
private fun BoardCard(board: Board, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                board.cover?.let { cover ->
                    AsyncImage(
                        model = cover.image.urlFor(600),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (board.isPrivate) {
                    Icon(
                        painterResource(R.drawable.ic_lock),
                        contentDescription = stringResource(R.string.pin_private),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
            }
            Text(
                board.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
            )
            Text(
                pluralStringResource(R.plurals.board_pins, board.totalPins, board.totalPins),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}
