package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.WindowMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/** Computes the [now, end) epoch-millis window for the agenda. */
class WindowCalculator(
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** Window for a settings [WindowMode]. */
    fun window(mode: WindowMode): Pair<Long, Long> {
        val start = clock.millis()
        val end = when (mode) {
            WindowMode.THIS_WEEK -> endOfCurrentWeekExclusive()
            else -> endOfDayPlus(mode.days)
        }
        return start to end
    }

    /** [now, start-of-day(today + days)) — `days` full calendar days starting today. */
    fun rolling(days: Long): Pair<Long, Long> = clock.millis() to endOfDayPlus(days)

    private fun endOfDayPlus(days: Long): Long =
        LocalDate.now(clock.withZone(zone)).plusDays(days).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun endOfCurrentWeekExclusive(): Long {
        val today = LocalDate.now(clock.withZone(zone))
        val lastDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek.plus(6)
        var d = today
        while (d.dayOfWeek != lastDayOfWeek) d = d.plusDays(1)
        return d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() // start of day after the week's last day
    }
}
