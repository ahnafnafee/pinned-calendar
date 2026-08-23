package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import dev.ahnafnafee.pinnedcalendar.data.todo.RecurrenceFrequency
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRecurrence
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class NextTodoOccurrence(
    val dueMillis: Long,
    val occurrenceNumber: Int,
)

data class TodoSeriesPosition(
    val anchorMillis: Long,
    val occurrenceNumber: Int,
)

/** Calculates custom recurring occurrences from a stable local-calendar series anchor. */
object RecurringTodoSchedule {

    /**
     * Chooses the series position after an edit. A due-date or rule change starts a new series;
     * changing only the time applies that time to the original anchor date without losing it.
     */
    fun positionAfterEdit(
        todo: LocalTodo,
        newDueMillis: Long?,
        newRecurrence: TodoRecurrence?,
        zone: ZoneId,
    ): TodoSeriesPosition? {
        if (newDueMillis == null || newRecurrence == null) return null
        val oldDueMillis = todo.dueMillis ?: return TodoSeriesPosition(newDueMillis, 1)
        if (todo.recurrence?.hasSamePatternAs(newRecurrence) != true) {
            return TodoSeriesPosition(newDueMillis, 1)
        }
        val oldAnchorMillis = todo.recurrenceAnchorMillis ?: oldDueMillis
        val oldOccurrence = todo.recurrenceOccurrence.coerceAtLeast(1)
        if (oldDueMillis == newDueMillis) return TodoSeriesPosition(oldAnchorMillis, oldOccurrence)

        val oldDue = instantAt(oldDueMillis, zone) ?: return TodoSeriesPosition(newDueMillis, 1)
        val newDue = instantAt(newDueMillis, zone) ?: return TodoSeriesPosition(newDueMillis, 1)
        if (oldDue.toLocalDate() != newDue.toLocalDate()) return TodoSeriesPosition(newDueMillis, 1)
        val oldAnchor = instantAt(oldAnchorMillis, zone) ?: return TodoSeriesPosition(newDueMillis, 1)
        val adjustedAnchor = runCatching {
            oldAnchor.toLocalDate()
                .atTime(newDue.toLocalTime())
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(newDueMillis)
        return TodoSeriesPosition(adjustedAnchor, oldOccurrence)
    }

    /**
     * Returns the first permitted occurrence after both the current due time and
     * [completedAtMillis]. Missed scheduled occurrences count toward an occurrence limit.
     */
    fun nextDueAfterCompletion(
        todo: LocalTodo,
        completedAtMillis: Long,
        zone: ZoneId,
    ): NextTodoOccurrence? {
        val currentDueMillis = todo.dueMillis ?: return null
        val rawRule = todo.recurrence ?: return null
        val anchorMillis = todo.recurrenceAnchorMillis ?: currentDueMillis
        val anchor = instantAt(anchorMillis, zone) ?: return null
        val rule = rawRule.normalized(anchor.toLocalDate())
        val thresholdMillis = maxOf(currentDueMillis, completedAtMillis)
        var occurrenceNumber = todo.recurrenceOccurrence.coerceAtLeast(1)
        var current = instantAt(currentDueMillis, zone) ?: return null

        while (true) {
            if (rule.maxOccurrences != null && occurrenceNumber >= rule.maxOccurrences) return null
            val nextNumber = occurrenceNumber + 1
            val candidate = when (rule.frequency) {
                RecurrenceFrequency.WEEKLY -> nextWeeklyOccurrence(current, anchor, rule, zone)
                else -> occurrenceAt(anchor, rule, nextNumber)
            } ?: return null

            if (rule.endDate != null && candidate.toLocalDate().isAfter(rule.endDate)) return null
            val candidateMillis = runCatching { candidate.toInstant().toEpochMilli() }.getOrNull() ?: return null
            if (candidateMillis > thresholdMillis) return NextTodoOccurrence(candidateMillis, nextNumber)

            current = candidate
            occurrenceNumber = nextNumber
        }
    }

    private fun occurrenceAt(
        anchor: ZonedDateTime,
        rule: TodoRecurrence,
        occurrenceNumber: Int,
    ): ZonedDateTime? = runCatching {
        val periods = Math.multiplyExact((occurrenceNumber - 1).toLong(), rule.interval.toLong())
        when (rule.frequency) {
            RecurrenceFrequency.DAILY -> anchor.plusDays(periods)
            RecurrenceFrequency.WEEKLY -> anchor.plusWeeks(periods)
            RecurrenceFrequency.MONTHLY -> anchor.plusMonths(periods)
            RecurrenceFrequency.YEARLY -> anchor.plusYears(periods)
        }
    }.getOrNull()

    private fun nextWeeklyOccurrence(
        current: ZonedDateTime,
        anchor: ZonedDateTime,
        rule: TodoRecurrence,
        zone: ZoneId,
    ): ZonedDateTime? {
        val anchorDate = anchor.toLocalDate()
        val anchorWeek = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val selectedDays = rule.weekdays.ifEmpty { setOf(anchorDate.dayOfWeek) }
        var date = current.toLocalDate().plusDays(1)
        while (rule.endDate == null || !date.isAfter(rule.endDate)) {
            val candidateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weeksFromAnchor = ChronoUnit.WEEKS.between(anchorWeek, candidateWeek)
            if (weeksFromAnchor >= 0 && weeksFromAnchor % rule.interval == 0L && date.dayOfWeek in selectedDays) {
                return runCatching { date.atTime(anchor.toLocalTime()).atZone(zone) }.getOrNull()
            }
            date = runCatching { date.plusDays(1) }.getOrNull() ?: return null
        }
        return null
    }

    private fun instantAt(millis: Long, zone: ZoneId): ZonedDateTime? =
        runCatching { Instant.ofEpochMilli(millis).atZone(zone) }.getOrNull()
}
