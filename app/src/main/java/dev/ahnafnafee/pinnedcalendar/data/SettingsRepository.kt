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
    suspend fun setCollapsedItems(value: Int) = update { it[COLLAPSED_ITEMS] = value.coerceIn(1, 6) }
    suspend fun setShowNotificationHeader(value: Boolean) = update { it[SHOW_NOTIFICATION_HEADER] = value }
    suspend fun setShowTodayNotificationHeader(value: Boolean) =
        update { it[SHOW_TODAY_NOTIFICATION_HEADER] = value }
    suspend fun setNotificationRowPadding(value: Int) =
        update { it[NOTIFICATION_ROW_PADDING] = value.coerceIn(0, 12) }
    suspend fun setNotificationRowTextSize(value: Int) =
        update { it[NOTIFICATION_ROW_TEXT_SIZE] = value.coerceIn(11, 18) }
    suspend fun setNotificationRowHeight(value: Int) =
        update { it[NOTIFICATION_ROW_HEIGHT] = value.coerceIn(12, 32) }
    suspend fun setNotificationTimeColumnWidth(value: Int) =
        update { it[NOTIFICATION_TIME_COLUMN_WIDTH] = value.coerceIn(32, 64) }
    suspend fun setNotificationContentPadding(value: Boolean) =
        update { it[NOTIFICATION_CONTENT_PADDING] = value }
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
        collapsedItems = (this[COLLAPSED_ITEMS] ?: 1).coerceIn(1, 6),
        showNotificationHeader = this[SHOW_NOTIFICATION_HEADER] ?: true,
        showTodayNotificationHeader = this[SHOW_TODAY_NOTIFICATION_HEADER] ?: true,
        notificationRowPaddingDp = (this[NOTIFICATION_ROW_PADDING] ?: 5).coerceIn(0, 12),
        notificationRowTextSizeSp = (this[NOTIFICATION_ROW_TEXT_SIZE] ?: 14).coerceIn(11, 18),
        notificationRowHeightDp = (this[NOTIFICATION_ROW_HEIGHT] ?: 22).coerceIn(12, 32),
        notificationTimeColumnWidthDp = (this[NOTIFICATION_TIME_COLUMN_WIDTH] ?: 64).coerceIn(32, 64),
        notificationContentPadding = this[NOTIFICATION_CONTENT_PADDING] ?: true,
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
        val COLLAPSED_ITEMS = intPreferencesKey("collapsed_items")
        val SHOW_NOTIFICATION_HEADER = booleanPreferencesKey("show_notification_header")
        val SHOW_TODAY_NOTIFICATION_HEADER = booleanPreferencesKey("show_today_notification_header")
        val NOTIFICATION_ROW_PADDING = intPreferencesKey("notification_row_padding")
        val NOTIFICATION_ROW_TEXT_SIZE = intPreferencesKey("notification_row_text_size")
        val NOTIFICATION_ROW_HEIGHT = intPreferencesKey("notification_row_height")
        val NOTIFICATION_TIME_COLUMN_WIDTH = intPreferencesKey("notification_time_column_width")
        val NOTIFICATION_CONTENT_PADDING = booleanPreferencesKey("notification_content_padding")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val THEME = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val AMOLED = booleanPreferencesKey("amoled")
        val SEED = intPreferencesKey("seed_color")
        val PALETTE = stringPreferencesKey("palette")
        val FONT = stringPreferencesKey("font")
    }
}
