package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import dev.ahnafnafee.pinnedcalendar.domain.model.DaySection
import dev.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationRow
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class DayBucketLabels(
    val today: String = "Today",
    val tomorrow: String = "Tomorrow",
    val allDay: String = "All day",
    val relativeHeaderPattern: String = "%1\$s · %2\$s",
)

class DayBucketer(
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
    use24Hour: Boolean = false,
    private val locale: Locale = Locale.getDefault(),
    private val labels: DayBucketLabels = DayBucketLabels(),
    timePattern: String = if (use24Hour) "H:mm" else "h:mm a",
    dayHeaderPattern: String = "EEE d",
) {
    private val timeFmt = DateTimeFormatter.ofPattern(timePattern, locale)
    private val dayHeaderFmt = DateTimeFormatter.ofPattern(dayHeaderPattern, locale)

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
        val label = dayHeaderFmt.format(date).uppercase(locale)
        return when (ChronoUnit.DAYS.between(today, date)) {
            0L -> String.format(
                locale,
                labels.relativeHeaderPattern,
                labels.today.uppercase(locale),
                label,
            )
            1L -> String.format(
                locale,
                labels.relativeHeaderPattern,
                labels.tomorrow.uppercase(locale),
                label,
            )
            else -> label
        }
    }

    private fun toRow(item: AgendaItem): NotificationRow {
        val time = when {
            item.allDay -> labels.allDay
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
