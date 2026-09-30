package dev.relony.pinry.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import dev.relony.pinry.ui.theme.DefaultSeed
import dev.relony.pinry.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class GridDensity(val minCellWidthDp: Int) { Small(120), Medium(160), Large(220) }

data class AppSettings(
    val mode: ThemeMode = ThemeMode.System,
    /** Seed from the wallpaper (API 31+) instead of [seed]. */
    val useSystemColor: Boolean = true,
    val seed: Int = DefaultSeed.toArgb(),
    val style: PaletteStyle = PaletteStyle.Expressive,
    val density: GridDensity = GridDensity.Medium,
)

class SettingsStore(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val new = transform(p.toSettings())
            p[MODE] = new.mode.name
            p[SYSTEM_COLOR] = new.useSystemColor
            p[SEED] = new.seed
            p[STYLE] = new.style.name
            p[DENSITY] = new.density.name
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            mode = this[MODE].toEnum(defaults.mode),
            useSystemColor = this[SYSTEM_COLOR] ?: defaults.useSystemColor,
            seed = this[SEED] ?: defaults.seed,
            style = this[STYLE].toEnum(defaults.style),
            density = this[DENSITY].toEnum(defaults.density),
        )
    }

    private companion object {
        val MODE = stringPreferencesKey("mode")
        val SYSTEM_COLOR = booleanPreferencesKey("system_color")
        val SEED = intPreferencesKey("seed")
        val STYLE = stringPreferencesKey("palette_style")
        val DENSITY = stringPreferencesKey("grid_density")
    }
}

/** Unknown or missing names (say, after an enum entry was renamed) fall back to [default]. */
private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
