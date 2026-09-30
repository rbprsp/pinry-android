@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.relony.pinry.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.LocalDynamicMaterialThemeSeed
import io.github.relony.pinry.ui.settings.AppSettings

enum class ThemeMode { System, Light, Dark, Black }

val DefaultSeed = Color(0xFFE8505B)

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/**
 * Every color scheme is generated from one seed color. "System color" uses the wallpaper accent
 * as that seed (API 31+), so palette style and pure-black mode apply to it as well.
 */
@Composable
fun PinryTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val seed = if (settings.useSystemColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_500)
    } else {
        Color(settings.seed)
    }
    CompositionLocalProvider(LocalAppSettings provides settings) {
        SchemeFrom(seed, content)
    }
}

/**
 * Re-themes [content] from [seed] (say, a pin image's dominant color) with the user's mode and
 * palette style; until the seed is known the app's own colors are used. Changes animate.
 */
@Composable
fun SeededTheme(seed: Color?, content: @Composable () -> Unit) =
    SchemeFrom(seed ?: LocalDynamicMaterialThemeSeed.current, content)

@Composable
private fun SchemeFrom(seed: Color, content: @Composable () -> Unit) {
    val settings = LocalAppSettings.current
    val dark = when (settings.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Black -> true
    }
    DynamicMaterialExpressiveTheme(
        seedColor = seed,
        motionScheme = MotionScheme.expressive(),
        isDark = dark,
        isAmoled = settings.mode == ThemeMode.Black,
        style = settings.style,
        animate = true,
        content = content,
    )
}

/** Emphasized styles (Expressive) for screen titles. */
val Typography.screenTitle: TextStyle get() = headlineMediumEmphasized
val Typography.barTitle: TextStyle get() = titleLargeEmphasized
