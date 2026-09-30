package io.github.relony.pinry.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import io.github.relony.pinry.R
import io.github.relony.pinry.data.api.Pin

private val MinCellWidth = 160.dp
private val Spacing = 8.dp

/** Masonry grid of pins that asks for the next page when the end comes within 10 items. */
@Composable
fun PinGrid(
    state: PagerState<Pin>,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (Pin) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    header: (@Composable () -> Unit)? = null,
) {
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) -> if (last >= total - 10) loadMore() }
    }

    BoxWithConstraints(modifier) {
        val direction = LocalLayoutDirection.current
        val usable = maxWidth - contentPadding.calculateStartPadding(direction) - contentPadding.calculateEndPadding(direction)
        val columns = ((usable + Spacing) / (MinCellWidth + Spacing)).toInt().coerceAtLeast(1)
        val cellWidthPx = with(LocalDensity.current) { ((usable - Spacing * (columns - 1)) / columns).roundToPx() }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = gridState,
            contentPadding = contentPadding,
            verticalItemSpacing = Spacing,
            horizontalArrangement = Arrangement.spacedBy(Spacing),
        ) {
            if (header != null) {
                item(key = "header", contentType = "header", span = StaggeredGridItemSpan.FullLine) { header() }
            }
            items(state.items, key = { it.id }, contentType = { "pin" }) { pin ->
                PinCard(pin, cellWidthPx, onClick = { onOpen(pin) }, Modifier.animateItem())
            }
            if (!state.endReached || state.error != null) {
                item(key = "footer", contentType = "footer", span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (state.error != null) {
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        } else {
                            PinryLoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinCard(pin: Pin, widthPx: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalPlatformContext.current
    val request = remember(pin.id, widthPx) {
        ImageRequest.Builder(context)
            .data(pin.image.urlFor(widthPx))
            .memoryCacheKey(gridCacheKey(pin.id))
            .build()
    }
    Column(modifier) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            AsyncImage(
                model = request,
                contentDescription = pin.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(pin.image.gridAspectRatio()),
            )
        }
        val description = pin.description?.trim()
        if (!description.isNullOrEmpty()) {
            Text(
                description,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            )
        }
    }
}
