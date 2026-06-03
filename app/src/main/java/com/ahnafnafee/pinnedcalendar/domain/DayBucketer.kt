package com.ahnafnafee.pinnedcalendar.domain

import com.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import com.ahnafnafee.pinnedcalendar.domain.model.DaySection
import com.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationRow
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class DayBucketer(
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val timeFmt = DateTimeFormatter.ofPattern("H:mm", Locale.getDefault())

    fun bucket(items: List<AgendaItem>): List<DaySection> {
        val today = LocalDate.now(clock.withZone(zone))
        return items.filter { it.start != null }
            .groupBy { it.start!!.atZone(zone).toLocalDate() }
            .toSortedMap()
            .map { (date, dayItems) ->
                DaySection(
                    header = headerFor(date, today),
                    isToday = date == today,
                    rows = dayItems
                        .sortedWith(compareByDescending<AgendaItem> { it.allDay }.thenBy { it.start })
                        .map { toRow(it) },
                )
            }
    }

    private fun headerFor(date: LocalDate, today: LocalDate): String {
        val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .uppercase(Locale.getDefault())
        val label = "$dow ${date.dayOfMonth}"
        return when (ChronoUnit.DAYS.between(today, date)) {
            0L -> "TODAY · $label"
            1L -> "TOMORROW · $label"
            else -> label
        }
    }

    private fun toRow(item: AgendaItem): NotificationRow {
        val time = when {
            item.allDay -> "All day"
            item.start != null -> timeFmt.format(item.start.atZone(zone))
            else -> ""
        }
        return NotificationRow(
            time = time,
            title = item.title,
            colorHex = item.colorHex,
            isTask = item.kind == ItemKind.TASK,
            completed = item.completed,
            deepLink = item.deepLink,
        )
    }
}
