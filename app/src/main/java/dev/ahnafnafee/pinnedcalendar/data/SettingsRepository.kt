package dev.ahnafnafee.pinnedcalendar.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }
    val pinEnabled: Flow<Boolean> = dataStore.data.map { it[PIN] ?: true }

    suspend fun snapshot(): AppSettings = settings.first()
    suspend fun isPinEnabled(): Boolean = pinEnabled.first()

    suspend fun setPinEnabled(value: Boolean) = update { it[PIN] = value }
    suspend fun setNotificationPriority(p: NotificationPriority) = update { it[NOTIF_PRIORITY] = p.name }
    suspend fun setDoubleSwipeDismiss(value: Boolean) = update { it[DOUBLE_SWIPE] = value }

    /** Epoch-millis of the last pin dismissal (0 = none). Drives the double-swipe-to-remove gesture. */
    suspend fun lastDismissAt(): Long = dataStore.data.map { it[LAST_DISMISS] ?: 0L }.first()
    suspend fun setLastDismissAt(value: Long) = update { it[LAST_DISMISS] = value }
    suspend fun setWindowMode(mode: WindowMode) = update { it[WINDOW] = mode.name }
    suspend fun setGroupByDay(value: Boolean) = update { it[GROUP] = value }
    suspend fun setHideCompleted(value: Boolean) = update { it[HIDE_DONE] = value }
    suspend fun setMaxItems(value: Int) = update { it[MAX] = value }
    suspend fun setUse24HourClock(value: Boolean) = update { it[CLOCK_24H] = value }
    suspend fun setThemeMode(mode: ThemeMode) = update { it[THEME] = mode.name }
    suspend fun setMaterialYou(value: Boolean) = update { it[MATERIAL_YOU] = value }
    suspend fun setAmoled(value: Boolean) = update { it[AMOLED] = value }
    suspend fun setSeedColor(argb: Int) = update { it[SEED] = argb }
    suspend fun setPalette(p: AppPalette) = update { it[PALETTE] = p.name }
    suspend fun setFont(f: AppFont) = update { it[FONT] = f.name }

    suspend fun setCalendarExcluded(id: String, excluded: Boolean) = update { p ->
        val cur = (p[EXCLUDED] ?: emptySet()).toMutableSet()
        if (excluded) cur.add(id) else cur.remove(id)
        p[EXCLUDED] = cur
    }

    fun displaySettings(s: AppSettings) =
        DisplaySettings(maxItems = s.maxItems, hideCompletedTasks = s.hideCompletedTasks, groupByDay = s.groupByDay)

    private suspend fun update(block: (MutablePreferences) -> Unit) = dataStore.edit(block)

    private fun Preferences.toAppSettings() = AppSettings(
        pinEnabled = this[PIN] ?: true,
        notificationPriority = this[NOTIF_PRIORITY].toEnum(NotificationPriority.TOP) { NotificationPriority.valueOf(it) },
        doubleSwipeDismiss = this[DOUBLE_SWIPE] ?: false,
        windowMode = this[WINDOW].toEnum(WindowMode.THIS_WEEK) { WindowMode.valueOf(it) },
        excludedCalendarIds = this[EXCLUDED] ?: emptySet(),
        groupByDay = this[GROUP] ?: true,
        hideCompletedTasks = this[HIDE_DONE] ?: true,
        maxItems = this[MAX] ?: 6,
        use24HourClock = this[CLOCK_24H] ?: false,
        themeMode = this[THEME].toEnum(ThemeMode.DARK) { ThemeMode.valueOf(it) },
        materialYou = this[MATERIAL_YOU] ?: false,
        amoled = this[AMOLED] ?: false,
        seedColorArgb = this[SEED] ?: 0xFFE07F2C.toInt(),
        palette = this[PALETTE].toEnum(AppPalette.TONAL_SPOT) { AppPalette.valueOf(it) },
        font = this[FONT].toEnum(AppFont.GOOGLE_SANS) { AppFont.valueOf(it) },
    )

    private inline fun <T> String?.toEnum(default: T, parse: (String) -> T): T =
        this?.let { runCatching { parse(it) }.getOrNull() } ?: default

    private companion object {
        val PIN = booleanPreferencesKey("pin_enabled")
        val NOTIF_PRIORITY = stringPreferencesKey("notif_priority")
        val DOUBLE_SWIPE = booleanPreferencesKey("double_swipe_dismiss")
        val LAST_DISMISS = longPreferencesKey("last_dismiss_at")
        val WINDOW = stringPreferencesKey("window_mode")
        val EXCLUDED = stringSetPreferencesKey("excluded_cal_ids")
        val GROUP = booleanPreferencesKey("group_by_day")
        val HIDE_DONE = booleanPreferencesKey("hide_completed")
        val MAX = intPreferencesKey("max_items")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val THEME = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val AMOLED = booleanPreferencesKey("amoled")
        val SEED = intPreferencesKey("seed_color")
        val PALETTE = stringPreferencesKey("palette")
        val FONT = stringPreferencesKey("font")
    }
}
