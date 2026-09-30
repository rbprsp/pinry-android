@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package dev.relony.pinry.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.relony.pinry.R

/** Wrappers around Material 3 Expressive (alpha) APIs, kept here so a version bump touches one package. */

@Composable
fun PinryLoadingIndicator(modifier: Modifier = Modifier) = LoadingIndicator(modifier)

@Composable
fun UploadProgress(progress: () -> Float, modifier: Modifier = Modifier) =
    LinearWavyProgressIndicator(progress = progress, modifier = modifier)

@Composable
fun RefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTopPadding: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = indicatorTopPadding),
            )
        },
        content = content,
    )
}

private val BoardShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Arch,
    MaterialShapes.Gem,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie4Sided,
    MaterialShapes.SoftBurst,
    MaterialShapes.Pill,
)

/** Each board keeps its own shape, which makes boards recognisable at a glance. */
@Composable
fun boardShape(id: Int): Shape = BoardShapes[Math.floorMod(id, BoardShapes.size)].toShape()

@Composable
fun avatarShape(): Shape = MaterialShapes.Cookie9Sided.toShape()

class Action(
    @DrawableRes val icon: Int,
    val label: String,
    val onClick: () -> Unit,
    /** Shown as a filled button with its label; the others are icon buttons. */
    val primary: Boolean = false,
)

/** A row of actions whose pressed button grows while its neighbours shrink; overflow goes to a menu. */
@Composable
fun ActionGroup(actions: List<Action>, modifier: Modifier = Modifier) {
    ButtonGroup(
        overflowIndicator = { menu ->
            IconButton(onClick = { menu.show() }) {
                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.more))
            }
        },
        modifier = modifier,
    ) {
        actions.forEach { action ->
            customItem(
                buttonGroupContent = {
                    val interaction = remember { MutableInteractionSource() }
                    val icon = @Composable {
                        Icon(painterResource(action.icon), contentDescription = if (action.primary) null else action.label)
                    }
                    if (action.primary) {
                        FilledTonalButton(
                            onClick = action.onClick,
                            shapes = ButtonDefaults.shapes(),
                            interactionSource = interaction,
                            modifier = Modifier.animateWidth(interaction),
                        ) {
                            icon()
                            Spacer(Modifier.size(8.dp))
                            Text(action.label)
                        }
                    } else {
                        OutlinedIconButton(
                            onClick = action.onClick,
                            shapes = IconButtonDefaults.shapes(),
                            interactionSource = interaction,
                            modifier = Modifier.animateWidth(interaction),
                            content = icon,
                        )
                    }
                },
                menuContent = { menu ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        leadingIcon = { Icon(painterResource(action.icon), contentDescription = null) },
                        onClick = { menu.dismiss(); action.onClick() },
                    )
                },
            )
        }
    }
}

/**
 * Tabs on the vibrant (primaryContainer) toolbar. The selected tab inverts the container/content
 * pair, which contrasts in every scheme; `primary` did not in dark vibrant palettes.
 */
@Composable
fun toolbarTabColors() = IconButtonDefaults.iconToggleButtonVibrantColors(
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    checkedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
    checkedContentColor = MaterialTheme.colorScheme.primaryContainer,
)
