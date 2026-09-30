@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.relony.pinry.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

class ToolbarTab<K>(val key: K, @DrawableRes val icon: Int, @StringRes val label: Int)

@Composable
fun <K> PinryToolbar(
    tabs: List<ToolbarTab<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.offset(y = -FloatingToolbarDefaults.ScreenOffset),
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
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
