package dev.ahnafnafee.pinnedcalendar.localization

import android.text.format.DateFormat
import java.util.Locale

/** Locale-native field order and punctuation while preserving the user’s clock preference. */
object LocalePatterns {
    fun time(locale: Locale, use24Hour: Boolean): String =
        DateFormat.getBestDateTimePattern(locale, if (use24Hour) "Hm" else "hm")

    fun dayHeader(locale: Locale): String =
        DateFormat.getBestDateTimePattern(locale, "EEEd")
}
