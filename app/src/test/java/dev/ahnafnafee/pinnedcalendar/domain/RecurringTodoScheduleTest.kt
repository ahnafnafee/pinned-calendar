package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import dev.ahnafnafee.pinnedcalendar.data.todo.RecurrenceFrequency
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRecurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurringTodoScheduleTest {

    private val zone = ZoneId.of("America/New_York")

    private fun at(date: LocalDate, time: LocalTime = LocalTime.of(9, 0)): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun recurring(
        due: Long,
        recurrence: TodoRecurrence,
        anchor: Long = due,
        occurrenceNumber: Int = 1,
    ) = LocalTodo(
        id = "1",
        title = "Water plants",
        dueMillis = due,
        recurrence = recurrence,
        recurrenceAnchorMillis = anchor,
        recurrenceOccurrence = occurrenceNumber,
    )

    private fun preset(frequency: RecurrenceFrequency) = TodoRecurrence.preset(frequency)

    @Test fun `overdue daily series skips missed occurrences`() {
        val due = at(LocalDate.of(2026, 6, 1))
        val completedAt = at(LocalDate.of(2026, 6, 4), LocalTime.NOON)

        assertEquals(
            at(LocalDate.of(2026, 6, 5)),
            RecurringTodoSchedule.nextDueAfterCompletion(
                recurring(due, preset(RecurrenceFrequency.DAILY)),
                completedAt,
                zone,
            )?.dueMillis,
        )
    }

    @Test fun `early completion still advances beyond the current occurrence`() {
        val due = at(LocalDate.of(2026, 6, 8))

        assertEquals(
            at(LocalDate.of(2026, 6, 15)),
            RecurringTodoSchedule.nextDueAfterCompletion(
                recurring(due, preset(RecurrenceFrequency.WEEKLY)),
                at(LocalDate.of(2026, 6, 6)),
                zone,
            )?.dueMillis,
        )
    }

    @Test fun `monthly series recovers its anchor day after a short month`() {
        val anchor = at(LocalDate.of(2027, 1, 31))
        val februaryOccurrence = at(LocalDate.of(2027, 2, 28))

        assertEquals(
            at(LocalDate.of(2027, 3, 31)),
            RecurringTodoSchedule.nextDueAfterCompletion(
                recurring(februaryOccurrence, preset(RecurrenceFrequency.MONTHLY), anchor, 2),
                februaryOccurrence,
                zone,
            )?.dueMillis,
        )
    }

    @Test fun `yearly series returns to leap day when the calendar allows it`() {
        val anchor = at(LocalDate.of(2024, 2, 29))
        val occurrence2027 = at(LocalDate.of(2027, 2, 28))

        assertEquals(
            at(LocalDate.of(2028, 2, 29)),
            RecurringTodoSchedule.nextDueAfterCompletion(
                recurring(occurrence2027, preset(RecurrenceFrequency.YEARLY), anchor, 4),
                occurrence2027,
                zone,
            )?.dueMillis,
        )
    }

    @Test fun `daily series keeps its local time across daylight saving changes`() {
        val anchor = LocalDateTime.of(2026, 3, 7, 9, 30).atZone(zone).toInstant().toEpochMilli()
        val expected = LocalDateTime.of(2026, 3, 8, 9, 30).atZone(zone).toInstant().toEpochMilli()

        val next = RecurringTodoSchedule.nextDueAfterCompletion(
            recurring(anchor, preset(RecurrenceFrequency.DAILY)),
            anchor,
            zone,
        )

        assertEquals(expected, next?.dueMillis)
        assertEquals(
            LocalTime.of(9, 30),
            java.time.Instant.ofEpochMilli(next!!.dueMillis).atZone(zone).toLocalTime(),
        )
    }

    @Test fun `custom daily interval advances by the requested number of days`() {
        val due = at(LocalDate.of(2026, 6, 1))
        val rule = TodoRecurrence(RecurrenceFrequency.DAILY, interval = 3)

        val next = RecurringTodoSchedule.nextDueAfterCompletion(recurring(due, rule), due, zone)

        assertEquals(at(LocalDate.of(2026, 6, 4)), next?.dueMillis)
        assertEquals(2, next?.occurrenceNumber)
    }

    @Test fun `weekly rule visits each selected weekday`() {
        val monday = at(LocalDate.of(2026, 6, 1))
        val wednesday = at(LocalDate.of(2026, 6, 3))
        val rule = TodoRecurrence(
            RecurrenceFrequency.WEEKLY,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        val fromMonday = RecurringTodoSchedule.nextDueAfterCompletion(recurring(monday, rule), monday, zone)
        val fromWednesday = RecurringTodoSchedule.nextDueAfterCompletion(
            recurring(wednesday, rule, monday, occurrenceNumber = 2),
            wednesday,
            zone,
        )

        assertEquals(wednesday, fromMonday?.dueMillis)
        assertEquals(at(LocalDate.of(2026, 6, 5)), fromWednesday?.dueMillis)
    }

    @Test fun `multi-week rule skips inactive weeks`() {
        val monday = at(LocalDate.of(2026, 6, 1))
        val friday = at(LocalDate.of(2026, 6, 5))
        val rule = TodoRecurrence(
            RecurrenceFrequency.WEEKLY,
            interval = 2,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        )

        val next = RecurringTodoSchedule.nextDueAfterCompletion(
            recurring(friday, rule, monday, occurrenceNumber = 2),
            friday,
            zone,
        )

        assertEquals(at(LocalDate.of(2026, 6, 15)), next?.dueMillis)
    }

    @Test fun `end date includes its day then closes the series`() {
        val june1 = at(LocalDate.of(2026, 6, 1))
        val june2 = at(LocalDate.of(2026, 6, 2))
        val june3 = at(LocalDate.of(2026, 6, 3))
        val rule = TodoRecurrence(
            RecurrenceFrequency.DAILY,
            endDate = LocalDate.of(2026, 6, 3),
        )

        val last = RecurringTodoSchedule.nextDueAfterCompletion(
            recurring(june2, rule, june1, occurrenceNumber = 2),
            june2,
            zone,
        )
        val finished = RecurringTodoSchedule.nextDueAfterCompletion(
            recurring(june3, rule, june1, occurrenceNumber = 3),
            june3,
            zone,
        )

        assertEquals(june3, last?.dueMillis)
        assertNull(finished)
    }

    @Test fun `missed occurrences count toward an occurrence limit`() {
        val june1 = at(LocalDate.of(2026, 6, 1))
        val rule = TodoRecurrence(RecurrenceFrequency.DAILY, maxOccurrences = 3)

        val next = RecurringTodoSchedule.nextDueAfterCompletion(
            recurring(june1, rule),
            at(LocalDate.of(2026, 6, 10)),
            zone,
        )

        assertNull(next)
    }
}
