package com.ahnafnafee.pinnedcalendar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ahnafnafee.pinnedcalendar.R
import com.ahnafnafee.pinnedcalendar.data.AppFont

/**
 * Google Sans Flex with the roundness axis driven to its maximum, giving the soft "Google Sans
 * Rounded" letterforms that define the Material 3 Expressive look. One [Font] per weight so the
 * variable axis is pinned alongside [FontWeight].
 */
private const val RoundAxis = 100f

@OptIn(ExperimentalTextApi::class)
private fun roundedGoogleSans(weight: FontWeight) = Font(
    R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("ROND", RoundAxis),
    ),
)

val GoogleSansRounded = FontFamily(
    roundedGoogleSans(FontWeight.Normal),
    roundedGoogleSans(FontWeight.Medium),
    roundedGoogleSans(FontWeight.SemiBold),
    roundedGoogleSans(FontWeight.Bold),
)

fun AppFont.toFontFamily(): FontFamily? = when (this) {
    AppFont.GOOGLE_SANS -> GoogleSansRounded
    AppFont.SYSTEM -> null
    AppFont.FIGTREE -> FontFamily(Font(R.font.figtree))
    AppFont.OUTFIT -> FontFamily(Font(R.font.outfit))
    AppFont.INTER -> FontFamily(Font(R.font.inter))
}

/**
 * Expressive type scale: rounded Google Sans drives the display / headline / title roles for the
 * big, friendly headings; body and label roles use the selected [bodyFamily] (or the rounded font
 * when none is chosen) so running text stays legible.
 */
fun appTypography(bodyFamily: FontFamily?): Typography {
    val heading = GoogleSansRounded
    val body = bodyFamily ?: GoogleSansRounded
    return Typography(
        displayLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = 0.sp),
        displayMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
        displaySmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
        headlineLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
}
