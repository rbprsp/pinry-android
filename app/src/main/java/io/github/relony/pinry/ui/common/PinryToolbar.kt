@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.relony.pinry.ui.common

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.relony.pinry.R

class ToolbarTab<K>(val key: K, @DrawableRes val icon: Int, @StringRes val label: Int)

class FabAction(@DrawableRes val icon: Int, @StringRes val label: Int, val onClick: () -> Unit)

/** Bottom space scrolling content should leave free for the floating toolbar (navigation bar excluded). */
val LocalToolbarInset = compositionLocalOf { 0.dp }

/**
 * Screen content with the floating tab toolbar over it, plus a "+" button opening [fabActions].
 * Both slide away while content scrolls down and come back on scroll up.
 */
@Composable
fun <K> ToolbarScaffold(
    tabs: List<ToolbarTab<K>>,
    selected: K?,
    onSelect: (K) -> Unit,
    showToolbar: Boolean,
    fabActions: List<FabAction>,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(FloatingToolbarExitDirection.Bottom)
    val toolbarState = scrollBehavior.state
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showToolbar) {
        // Scrolling on other screens (a form, say) also moves the toolbar; show it again on return.
        if (showToolbar) toolbarState.offset = 0f else menuOpen = false
    }
    LaunchedEffect(toolbarState) { snapshotFlow { toolbarState.offset }.collect { if (it != 0f) menuOpen = false } }
    BackHandler(menuOpen) { menuOpen = false }
    val inset: Dp = if (showToolbar) FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset * 2 else 0.dp

    Box(Modifier.fillMaxSize().nestedScroll(scrollBehavior)) {
        CompositionLocalProvider(LocalToolbarInset provides inset, content = content)
        if (menuOpen) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { menuOpen = false })
        }
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
                        colors = toolbarTabColors(),
                    ) {
                        Icon(painterResource(tab.icon), contentDescription = stringResource(tab.label))
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showToolbar,
            enter = slideInVertically { it * 2 },
            exit = slideOutVertically { it * 2 },
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            FloatingActionButtonMenu(
                expanded = menuOpen,
                button = {
                    ToggleFloatingActionButton(checked = menuOpen, onCheckedChange = { menuOpen = it }) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.create_title),
                            // The container goes from primaryContainer to primary as the menu opens.
                            tint = lerp(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.onPrimary, checkedProgress),
                            modifier = Modifier.graphicsLayer { rotationZ = 45f * checkedProgress },
                        )
                    }
                },
                // Follows the toolbar when it slides away on scroll.
                modifier = Modifier.navigationBarsPadding().graphicsLayer { translationY = -toolbarState.offset },
            ) {
                fabActions.forEach { action ->
                    FloatingActionButtonMenuItem(
                        onClick = { menuOpen = false; action.onClick() },
                        text = { Text(stringResource(action.label)) },
                        icon = { Icon(painterResource(action.icon), contentDescription = null) },
                    )
                }
            }
        }
    }
}
