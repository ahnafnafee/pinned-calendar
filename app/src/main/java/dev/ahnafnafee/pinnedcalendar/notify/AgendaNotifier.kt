package dev.ahnafnafee.pinnedcalendar.notify

import android.content.Context
import dev.ahnafnafee.pinnedcalendar.data.AgendaRepository
import dev.ahnafnafee.pinnedcalendar.data.SettingsRepository
import dev.ahnafnafee.pinnedcalendar.data.settingsDataStore
import dev.ahnafnafee.pinnedcalendar.domain.DayBucketer
import dev.ahnafnafee.pinnedcalendar.domain.NotificationContentBuilder
import java.time.Clock

/** Reads current settings, builds the agenda, and posts (or cancels) the pinned notification. */
class AgendaNotifier(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun refresh() {
        val settings = SettingsRepository(context.applicationContext.settingsDataStore)
        val s = settings.snapshot()
        val items = AgendaRepository(context, clock).agenda(s.windowMode, s.excludedCalendarIds)
        val bucketer = DayBucketer(clock, use24Hour = s.use24HourClock)
        val content = NotificationContentBuilder(bucketer).build(items, settings.displaySettings(s))
        NotificationPoster(context).apply(
            pinEnabled = s.pinEnabled,
            priority = s.notificationPriority,
            content = content,
            collapsedItems = s.collapsedItems,
            showHeader = s.showNotificationHeader,
            showTodayHeader = s.showTodayNotificationHeader,
            rowPaddingDp = s.notificationRowPaddingDp,
            rowTextSizeSp = s.notificationRowTextSizeSp,
            rowHeightDp = s.notificationRowHeightDp,
            timeColumnWidthDp = s.notificationTimeColumnWidthDp,
            useContentPadding = s.notificationContentPadding,
        )
    }
}
