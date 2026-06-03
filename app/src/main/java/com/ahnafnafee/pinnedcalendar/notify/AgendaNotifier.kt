package com.ahnafnafee.pinnedcalendar.notify

import android.content.Context
import com.ahnafnafee.pinnedcalendar.data.AgendaRepository
import com.ahnafnafee.pinnedcalendar.data.SettingsRepository
import com.ahnafnafee.pinnedcalendar.data.settingsDataStore
import com.ahnafnafee.pinnedcalendar.domain.DayBucketer
import com.ahnafnafee.pinnedcalendar.domain.NotificationContentBuilder
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
        val content = NotificationContentBuilder(DayBucketer(clock)).build(items, settings.displaySettings(s))
        NotificationPoster(context).apply(s.pinEnabled, s.notificationPriority, content)
    }
}
