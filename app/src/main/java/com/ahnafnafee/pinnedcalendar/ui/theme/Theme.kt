package com.ahnafnafee.pinnedcalendar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ahnafnafee.pinnedcalendar.data.AppPalette
import com.ahnafnafee.pinnedcalendar.data.AppSettings
import com.ahnafnafee.pinnedcalendar.data.ThemeMode
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Material 3 theme. Color comes from a seed colour + palette style (MaterialKolor); when Material
 * You is enabled (API 31+) the wallpaper scheme is used instead. AMOLED forces pure-black surfaces.
 * Typography uses the selected font.
 */
@Composable
fun PinnedCalendarTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val seedScheme = rememberDynamicColorScheme(
        seedColor = Color(settings.seedColorArgb),
        isDark = dark,
        isAmoled = settings.amoled,
        style = settings.palette.toPaletteStyle(),
    )

    val colorScheme = when {
        settings.materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val sys = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (settings.amoled && dark) sys.copy(background = Color.Black, surface = Color.Black) else sys
        }
        else -> seedScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(settings.font.toFontFamily()),
        content = content,
    )
}

private fun AppPalette.toPaletteStyle(): PaletteStyle = when (this) {
    AppPalette.TONAL_SPOT -> PaletteStyle.TonalSpot
    AppPalette.VIBRANT -> PaletteStyle.Vibrant
    AppPalette.EXPRESSIVE -> PaletteStyle.Expressive
    AppPalette.NEUTRAL -> PaletteStyle.Neutral
}
