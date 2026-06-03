package com.ahnafnafee.pinnedcalendar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ahnafnafee.pinnedcalendar.R
import com.ahnafnafee.pinnedcalendar.data.AppFont

/** Expressive variable display font used for display / headline / title styles. */
@OptIn(ExperimentalTextApi::class)
private val HeadingFont = FontFamily(
    Font(
        R.font.google_sans_flex, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.google_sans_flex, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(620)),
    ),
    Font(
        R.font.google_sans_flex, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(760)),
    ),
)

fun AppFont.toFontFamily(): FontFamily? = when (this) {
    AppFont.SYSTEM -> null
    AppFont.FIGTREE -> FontFamily(Font(R.font.figtree))
    AppFont.OUTFIT -> FontFamily(Font(R.font.outfit))
    AppFont.INTER -> FontFamily(Font(R.font.inter))
}

/**
 * Headings (display / headline / title) use [HeadingFont]; body and label styles use the
 * selected [bodyFamily] (or the platform default when null).
 */
fun appTypography(bodyFamily: FontFamily?): Typography {
    val b = Typography()
    return b.copy(
        displayLarge = b.displayLarge.copy(fontFamily = HeadingFont),
        displayMedium = b.displayMedium.copy(fontFamily = HeadingFont),
        displaySmall = b.displaySmall.copy(fontFamily = HeadingFont),
        headlineLarge = b.headlineLarge.copy(fontFamily = HeadingFont),
        headlineMedium = b.headlineMedium.copy(fontFamily = HeadingFont),
        headlineSmall = b.headlineSmall.copy(fontFamily = HeadingFont),
        titleLarge = b.titleLarge.copy(fontFamily = HeadingFont),
        titleMedium = b.titleMedium.copy(fontFamily = HeadingFont),
        titleSmall = b.titleSmall.copy(fontFamily = HeadingFont),
        bodyLarge = b.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = b.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = b.bodySmall.copy(fontFamily = bodyFamily),
        labelLarge = b.labelLarge.copy(fontFamily = bodyFamily),
        labelMedium = b.labelMedium.copy(fontFamily = bodyFamily),
        labelSmall = b.labelSmall.copy(fontFamily = bodyFamily),
    )
}
