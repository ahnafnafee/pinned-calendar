package com.ahnafnafee.pinnedcalendar.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ahnafnafee.pinnedcalendar.data.calendar.CalendarEventsDataSource
import com.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import com.ahnafnafee.pinnedcalendar.domain.WindowCalculator
import com.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import com.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of agenda items: device calendar events (Calendar Provider, minus
 * excluded calendars) plus local in-app to-dos, within the window. Fully local.
 */
class AgendaRepository(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val todoRepo: TodoRepository = TodoRepository(context.applicationContext.settingsDataStore),
) {
    private val events = CalendarEventsDataSource(context, zone)
    private val windows = WindowCalculator(clock, zone)

    suspend fun agenda(mode: WindowMode, excludedCalendarIds: Set<String>): List<AgendaItem> {
        val (start, end) = windows.window(mode)
        val items = mutableListOf<AgendaItem>()

        if (hasCalendarPermission()) {
            items += events.eventsInWindow(start, end, excludedCalendarIds)
        }

        // Local to-dos within the window. Undated ones are excluded; a still-open task whose due
        // date has already passed carries forward to today (keeping its time of day) instead of
        // dropping out, so manual tasks persist until completed or deleted. Completed past tasks fall off.
        val today = LocalDate.now(clock.withZone(zone))
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        todoRepo.snapshot().forEach { t ->
            val due = t.dueMillis ?: return@forEach
            if (due >= end) return@forEach // due beyond the window — not yet relevant
            val start = when {
                due >= todayStart -> Instant.ofEpochMilli(due) // today or later in window — keep its slot
                t.completed -> return@forEach                  // overdue and done — drop it
                else -> Instant.ofEpochMilli(due).atZone(zone) // overdue and open — re-anchor to today
                    .toLocalTime().atDate(today).atZone(zone).toInstant()
            }
            items += AgendaItem(
                id = "todo_${t.id}",
                kind = ItemKind.TASK,
                title = t.title,
                start = start,
                allDay = false,
                colorHex = null,
                completed = t.completed,
            )
        }

        return items.sortedBy { it.start }
    }

    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
}
