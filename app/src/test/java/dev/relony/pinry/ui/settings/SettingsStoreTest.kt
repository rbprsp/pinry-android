package dev.relony.pinry.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.materialkolor.PaletteStyle
import dev.relony.pinry.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun changesArePersistedAndDefaultsFillTheRest() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create { File(tmp.root, "settings.preferences_pb") }
        val store = SettingsStore(dataStore)
        assertEquals(AppSettings(), store.settings.first())

        store.update { it.copy(mode = ThemeMode.Black, useSystemColor = false, seed = 0xFF009688.toInt(), density = GridDensity.Small) }

        assertEquals(
            AppSettings(ThemeMode.Black, useSystemColor = false, seed = 0xFF009688.toInt(), style = PaletteStyle.Expressive, density = GridDensity.Small),
            store.settings.first(),
        )
    }

    @Test
    fun unknownStoredNamesFallBackToDefaults() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create { File(tmp.root, "settings.preferences_pb") }
        dataStore.edit {
            it[stringPreferencesKey("mode")] = "Sepia"
            it[stringPreferencesKey("palette_style")] = "Removed"
        }

        val settings = SettingsStore(dataStore).settings.first()

        assertEquals(ThemeMode.System, settings.mode)
        assertEquals(PaletteStyle.Expressive, settings.style)
    }
}
