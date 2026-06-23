package dev.ahnafnafee.pinnedcalendar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.ahnafnafee.pinnedcalendar.data.AppPalette
import dev.ahnafnafee.pinnedcalendar.data.AppSettings
import dev.ahnafnafee.pinnedcalendar.data.ThemeMode
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

    // Edge-to-edge: keep the system bar icons legible against the app surface behind them.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).run {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
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
