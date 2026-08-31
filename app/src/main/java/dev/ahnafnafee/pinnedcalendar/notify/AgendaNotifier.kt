package dev.ahnafnafee.pinnedcalendar.notify

import android.content.Context
import dev.ahnafnafee.pinnedcalendar.data.AgendaRepository
import dev.ahnafnafee.pinnedcalendar.data.SettingsRepository
import dev.ahnafnafee.pinnedcalendar.data.settingsDataStore
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import dev.ahnafnafee.pinnedcalendar.domain.DayBucketer
import dev.ahnafnafee.pinnedcalendar.domain.DayBucketLabels
import dev.ahnafnafee.pinnedcalendar.domain.NotificationContentBuilder
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.localization.LocalePatterns
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
        val locale = context.resources.configuration.locales[0]
        val bucketer = DayBucketer(
            clock,
            use24Hour = s.use24HourClock,
            locale = locale,
            labels = DayBucketLabels(
                today = context.getString(R.string.agenda_today),
                tomorrow = context.getString(R.string.agenda_tomorrow),
                allDay = context.getString(R.string.agenda_all_day),
                relativeHeaderPattern = context.getString(R.string.agenda_relative_day_header),
            ),
            timePattern = LocalePatterns.time(locale, s.use24HourClock),
            dayHeaderPattern = LocalePatterns.dayHeader(locale),
        )
        val content = NotificationContentBuilder(bucketer).build(items, settings.displaySettings(s))
        NotificationPoster(context).apply(
            pinEnabled = s.pinEnabled,
            priority = s.notificationPriority,
            content = content,
            collapsedItems = s.collapsedItems,
            showHeader = s.showNotificationHeader,
            showTodayHeader = s.showTodayHeader,
            rowPaddingDp = s.notificationRowPaddingDp,
            rowTextSizeSp = s.notificationRowTextSizeSp,
            rowHeightDp = s.notificationRowHeightDp,
            timeColumnWidthDp = s.notificationTimeColumnWidthDp,
            useContentPadding = s.notificationContentPadding,
        )

        // Every refresh re-arms (or cancels) the single due-time reminder alarm, so boots,
        // edits, and completions all keep it pointed at the next upcoming to-do.
        TodoReminderScheduler.sync(
            context,
            enabled = s.todoReminders,
            todos = TodoRepository(context.applicationContext.settingsDataStore).snapshot(),
            nowMillis = clock.millis(),
        )
    }
}
