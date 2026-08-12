package dev.ahnafnafee.pinnedcalendar.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoScheduleTest {

    private val zone = ZoneId.of("America/New_York")

    @Test fun `moving a due date keeps the time of day`() {
        val original = LocalDate.of(2026, 6, 2).atTime(17, 30).atZone(zone).toInstant().toEpochMilli()
        val moved = TodoSchedule.at(original, LocalDate.of(2026, 6, 9), zone)
        assertEquals(
            LocalDate.of(2026, 6, 9).atTime(17, 30).atZone(zone).toInstant().toEpochMilli(),
            moved,
        )
        assertEquals(LocalTime.of(17, 30), Instant.ofEpochMilli(moved).atZone(zone).toLocalTime())
    }

    @Test fun `a previously undated item lands at nine`() {
        val date = LocalDate.of(2026, 6, 9)
        assertEquals(
            date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            TodoSchedule.at(null, date, zone),
        )
    }
}
