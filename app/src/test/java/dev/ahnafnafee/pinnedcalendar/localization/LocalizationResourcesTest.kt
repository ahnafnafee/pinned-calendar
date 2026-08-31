package dev.ahnafnafee.pinnedcalendar.localization

import android.content.Context
import android.content.res.Configuration
import android.view.View
import dev.ahnafnafee.pinnedcalendar.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LocalizationResourcesTest {

    private val application = RuntimeEnvironment.getApplication()

    @Test fun locale_config_lists_all_supported_languages() {
        val configuredTags = buildList {
            val parser = application.resources.getXml(R.xml.locales_config)
            try {
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                        add(parser.getAttributeValue(ANDROID_NAMESPACE, "name"))
                    }
                    parser.next()
                }
            } finally {
                parser.close()
            }
        }

        assertEquals(SUPPORTED_LANGUAGE_TAGS, configuredTags)
    }

    @Test fun every_supported_locale_resolves_ui_and_reminder_copy() {
        val englishContext = localizedContext("en")
        val representativeResources = listOf(
            R.string.nav_settings,
            R.string.todo_reminder_mark_done,
            R.string.todo_reminder_snooze,
            R.string.agenda_today,
            R.string.settings_amoled,
        )

        SUPPORTED_LANGUAGE_TAGS.forEach { languageTag ->
            val context = localizedContext(languageTag)

            representativeResources.forEach { resourceId ->
                val localizedValue = context.getString(resourceId)
                assertTrue("$languageTag resolved an empty string", localizedValue.isNotBlank())
                // JDK 11 canonicalizes modern Indonesian "id" to legacy "in", while Android's
                // BCP-47 resource qualifier remains "b+id". Lint verifies that catalog; this
                // Robolectric assertion can still exercise the configured locale without
                // reliably distinguishing its resource from the English fallback.
                if (languageTag != "en" && languageTag != "id") {
                    assertNotEquals(
                        "$languageTag fell back to English for resource $resourceId",
                        englishContext.getString(resourceId),
                        localizedValue,
                    )
                }
            }
        }
    }

    @Test fun arabic_resources_use_right_to_left_layout_direction() {
        assertEquals(
            View.LAYOUT_DIRECTION_RTL,
            localizedContext("ar").resources.configuration.layoutDirection,
        )
    }

    private fun localizedContext(languageTag: String): Context {
        val locale = Locale.forLanguageTag(languageTag)
        val configuration = Configuration(application.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return application.createConfigurationContext(configuration)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

        val SUPPORTED_LANGUAGE_TAGS = listOf(
            "en",
            "ar",
            "tr",
            "es",
            "zh-CN",
            "fr",
            "pt",
            "de",
            "ja",
            "hi",
            "bn",
            "id",
            "ko",
            "it",
            "vi",
        )
    }
}
