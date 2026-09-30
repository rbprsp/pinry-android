@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.relony.pinry.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class ToolbarTab<K>(val key: K, @DrawableRes val icon: Int, @StringRes val label: Int)

/** Bottom space scrolling content should leave free for the floating toolbar (navigation bar excluded). */
val LocalToolbarInset = compositionLocalOf { 0.dp }

/**
 * Screen content with the floating tab toolbar over it. The toolbar slides away while content
 * scrolls down and comes back on scroll up.
 */
@Composable
fun <K> ToolbarScaffold(
    tabs: List<ToolbarTab<K>>,
    selected: K?,
    onSelect: (K) -> Unit,
    showToolbar: Boolean,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(FloatingToolbarExitDirection.Bottom)
    val inset: Dp = if (showToolbar) FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset * 2 else 0.dp

    Box(Modifier.fillMaxSize().nestedScroll(scrollBehavior)) {
        CompositionLocalProvider(LocalToolbarInset provides inset, content = content)
        AnimatedVisibility(
            visible = showToolbar,
            enter = slideInVertically { it * 2 },
            exit = slideOutVertically { it * 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                scrollBehavior = scrollBehavior,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = Modifier.navigationBarsPadding().offset(y = -FloatingToolbarDefaults.ScreenOffset),
            ) {
                tabs.forEach { tab ->
                    IconToggleButton(
                        checked = tab.key == selected,
                        onCheckedChange = { onSelect(tab.key) },
                        shapes = IconButtonDefaults.toggleableShapes(),
                    ) {
                        Icon(painterResource(tab.icon), contentDescription = stringResource(tab.label))
                    }
                }
            }
        }
    }
}
