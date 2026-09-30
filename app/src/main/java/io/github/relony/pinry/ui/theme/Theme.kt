@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.relony.pinry.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle

enum class ThemeMode { System, Light, Dark, Black }

val DefaultSeed = Color(0xFFE8505B)

/**
 * Every color scheme is generated from one seed color. "System color" uses the wallpaper accent
 * as that seed (API 31+), so palette style and pure-black mode apply to it as well.
 */
@Composable
fun PinryTheme(
    mode: ThemeMode = ThemeMode.System,
    useSystemColor: Boolean = true,
    seed: Color = DefaultSeed,
    style: PaletteStyle = PaletteStyle.Expressive,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Black -> true
    }
    val seedColor = if (useSystemColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        seed
    }
    DynamicMaterialExpressiveTheme(
        seedColor = seedColor,
        motionScheme = MotionScheme.expressive(),
        isDark = dark,
        isAmoled = mode == ThemeMode.Black,
        style = style,
        animate = true,
        content = content,
    )
}
