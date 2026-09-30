package io.github.relony.pinry.ui.feed

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.relony.pinry.R
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.ui.common.BackTopBar
import io.github.relony.pinry.ui.common.LocalToolbarInset
import io.github.relony.pinry.ui.common.PinGrid
import io.github.relony.pinry.ui.common.PinryLoadingIndicator
import io.github.relony.pinry.ui.common.RefreshBox

/**
 * A feed of pins. With a [title] it gets a top bar with Back; without one the grid runs under the
 * status bar (tab roots).
 */
@Composable
fun FeedScreen(
    vm: FeedViewModel,
    onOpen: (Pin) -> Unit,
    title: String? = null,
    onBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
) {
    val state by vm.pager.state.collectAsStateWithLifecycle()
    // "Fully drawn" for startup metrics: the first feed on screen has pins.
    ReportDrawnWhen { state.items.isNotEmpty() }
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalToolbarInset.current
    val top = if (title == null) statusBar + 8.dp else 8.dp

    Column(Modifier.fillMaxSize()) {
        if (title != null) BackTopBar(title, onBack, actions = actions)
        RefreshBox(
            refreshing = state.refreshing,
            onRefresh = vm.pager::refresh,
            indicatorTopPadding = if (title == null) statusBar else 0.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            val empty = state.items.isEmpty()
            when {
                empty && state.error != null -> Message(stringResource(R.string.feed_error), vm.pager::retry)
                empty && state.endReached && header == null -> Message(stringResource(R.string.feed_empty))
                empty && !state.endReached -> Box(Modifier.fillMaxSize(), Alignment.Center) { PinryLoadingIndicator() }
                else -> PinGrid(
                    state = state,
                    onLoadMore = vm.pager::loadMore,
                    onRetry = vm.pager::retry,
                    onOpen = onOpen,
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = top, bottom = bottom + 8.dp),
                    header = header,
                )
            }
        }
    }
}

@Composable
private fun Message(text: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
