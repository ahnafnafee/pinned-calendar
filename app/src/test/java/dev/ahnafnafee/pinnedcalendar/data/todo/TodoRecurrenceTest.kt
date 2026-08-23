package dev.ahnafnafee.pinnedcalendar.data.todo

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoRecurrenceTest {

    @Test fun `state representation roundtrips a full custom rule`() {
        val rule = TodoRecurrence(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 3,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            endDate = LocalDate.of(2027, 1, 15),
        )

        assertEquals(rule, TodoRecurrence.fromStateValue(rule.toStateValue()))
    }

    @Test fun `normalization bounds numbers and keeps only one ending`() {
        val anchor = LocalDate.of(2026, 6, 1)
        val normalized = TodoRecurrence(
            frequency = RecurrenceFrequency.DAILY,
            interval = 0,
            weekdays = setOf(DayOfWeek.MONDAY),
            endDate = anchor.minusDays(1),
            maxOccurrences = 20,
        ).normalized(anchor)

        assertEquals(1, normalized.interval)
        assertEquals(emptySet<DayOfWeek>(), normalized.weekdays)
        assertEquals(anchor, normalized.endDate)
        assertNull(normalized.maxOccurrences)
    }

    @Test fun `weekday bitmask is stable and ignores unused bits`() {
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)
        val mask = TodoRecurrence.weekdayMask(days)

        assertEquals(days, TodoRecurrence.weekdaysFromMask(mask or (1 shl 12)))
    }

    @Test fun `unknown persisted frequency disables recurrence`() {
        assertNull(TodoRecurrence.fromPersisted("fortnightly", interval = 2))
    }
}
