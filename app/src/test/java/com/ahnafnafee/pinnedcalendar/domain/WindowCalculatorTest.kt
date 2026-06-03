package com.ahnafnafee.pinnedcalendar.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WindowCalculatorTest {
    private val zone = ZoneId.of("America/New_York")
    private val clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), zone)

    @Test fun rolling_window_starts_now_and_ends_n_days_out_at_local_midnight() {
        val (start, end) = WindowCalculator(clock, zone).rolling(7)
        assertEquals(Instant.parse("2026-06-01T12:00:00Z").toEpochMilli(), start)
        // today in NY is 2026-06-01; +7 days -> 2026-06-08 local midnight.
        val expectedEnd = LocalDate.of(2026, 6, 8).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedEnd, end)
    }
}
