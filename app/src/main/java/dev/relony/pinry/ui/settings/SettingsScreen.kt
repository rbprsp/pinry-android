package dev.relony.pinry.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import dev.relony.pinry.BuildConfig
import dev.relony.pinry.R
import dev.relony.pinry.ui.common.BackTopBar
import dev.relony.pinry.ui.theme.DefaultSeed
import dev.relony.pinry.ui.theme.ThemeMode

private val Swatches = listOf(
    DefaultSeed, Color(0xFFF08A24), Color(0xFFE0B400), Color(0xFF4CAF50), Color(0xFF009688),
    Color(0xFF00ACC1), Color(0xFF3F7CE8), Color(0xFF5C6BC0), Color(0xFF8E5BD8), Color(0xFFE05AA8),
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    account: String,
    /** Without an account the button leads to the login screen instead. */
    signedIn: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val wallpaperColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val customColor = !(wallpaperColors && settings.useSystemColor)

    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.settings), onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Section(stringResource(R.string.settings_theme)) {
                Choice(
                    options = ThemeMode.entries,
                    selected = settings.mode,
                    label = { stringResource(it.label) },
                    onSelect = { mode -> onChange { it.copy(mode = mode) } },
                )
            }
            Section(stringResource(R.string.settings_color)) {
                if (wallpaperColors) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_wallpaper_color), Modifier.weight(1f))
                        Switch(settings.useSystemColor, onCheckedChange = { on -> onChange { it.copy(useSystemColor = on) } })
                    }
                }
                if (customColor) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Swatches.forEach { color ->
                            Swatch(color, selected = settings.seed == color.toArgb()) {
                                onChange { it.copy(seed = color.toArgb(), useSystemColor = false) }
                            }
                        }
                    }
                    HueSlider(settings.seed) { argb -> onChange { it.copy(seed = argb, useSystemColor = false) } }
                }
            }
            Section(stringResource(R.string.settings_palette)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaletteStyle.entries.forEach { style ->
                        FilterChip(
                            selected = settings.style == style,
                            onClick = { onChange { it.copy(style = style) } },
                            label = { Text(style.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")) },
                        )
                    }
                }
            }
            Section(stringResource(R.string.settings_grid)) {
                Choice(
                    options = GridDensity.entries,
                    selected = settings.density,
                    label = { stringResource(it.label) },
                    onSelect = { density -> onChange { it.copy(density = density) } },
                )
            }
            Section(stringResource(R.string.settings_account)) {
                Text(account, style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = onLogout) { Text(stringResource(if (signedIn) R.string.logout else R.string.login_submit)) }
            }
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun <T> Choice(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label(option)) }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White)
    }
}

/** Any hue at a fixed, pleasant saturation; saved when the drag ends so DataStore isn't hit every frame. */
@Composable
private fun HueSlider(seed: Int, onPick: (Int) -> Unit) {
    // Re-created when the seed changes elsewhere (a swatch), so the thumb follows it.
    val state = remember(seed) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(seed, it) }
        SliderState(value = hsv[0], trackRange = 0f..359f)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.hsv(state.value, 0.65f, 0.85f)))
        Slider(
            state = state,
            onValueChange = { state.value = it },
            onValueChangeFinished = { onPick(Color.hsv(state.value, 0.65f, 0.85f).toArgb()) },
            modifier = Modifier.weight(1f),
        )
    }
}

private val ThemeMode.label
    get() = when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
        ThemeMode.Black -> R.string.theme_black
    }

private val GridDensity.label
    get() = when (this) {
        GridDensity.Small -> R.string.grid_small
        GridDensity.Medium -> R.string.grid_medium
        GridDensity.Large -> R.string.grid_large
    }
