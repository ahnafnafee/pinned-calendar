package com.ahnafnafee.pinnedcalendar.domain

import com.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import com.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class DayBucketerTest {
    private val zone = ZoneId.of("America/New_York")
    // 2026-06-01 is a Monday.
    private val clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), zone)
    private val bucketer = DayBucketer(clock, zone)

    private fun event(daysAhead: Long, h: Int, m: Int, title: String, allDay: Boolean = false): AgendaItem {
        val date = java.time.LocalDate.now(clock.withZone(zone)).plusDays(daysAhead)
        return AgendaItem(
            title, ItemKind.EVENT, title,
            date.atTime(LocalTime.of(h, m)).atZone(zone).toInstant(),
            allDay = allDay, colorHex = "#039BE5",
        )
    }

    @Test fun groups_by_day_and_labels_today_tomorrow() {
        val sections = bucketer.bucket(listOf(event(0, 9, 0, "A"), event(1, 10, 0, "B"), event(2, 11, 0, "C")))
        assertEquals(3, sections.size)
        assertTrue(sections[0].header.startsWith("TODAY"))
        assertTrue(sections[0].isToday)
        assertTrue(sections[1].header.startsWith("TOMORROW"))
        assertTrue(sections[2].header.startsWith("WED"))
    }

    @Test fun today_header_includes_weekday_and_day_of_month() {
        val sections = bucketer.bucket(listOf(event(0, 9, 0, "A")))
        assertEquals("TODAY · MON 1", sections[0].header)
    }

    @Test fun all_day_items_sort_before_timed_within_a_day() {
        val sections = bucketer.bucket(listOf(event(0, 9, 0, "timed"), event(0, 0, 0, "allday", allDay = true)))
        assertEquals("allday", sections[0].rows[0].title)
        assertEquals("All day", sections[0].rows[0].time)
        assertEquals("timed", sections[0].rows[1].title)
        assertEquals("9:00", sections[0].rows[1].time)
    }

    @Test fun tasks_render_as_task_rows_without_color() {
        val date = java.time.LocalDate.now(clock.withZone(zone))
        val task = AgendaItem(
            "t", ItemKind.TASK, "Pay rent",
            date.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant(), colorHex = null,
        )
        val rows = bucketer.bucket(listOf(task))[0].rows
        assertTrue(rows[0].isTask)
        assertEquals(null, rows[0].colorHex)
    }

    @Test fun propagates_event_deeplink_to_row() {
        val date = java.time.LocalDate.now(clock.withZone(zone))
        val ev = AgendaItem(
            "e", ItemKind.EVENT, "Standup",
            date.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant(),
            colorHex = "#039BE5", deepLink = "content://com.android.calendar/events/42",
        )
        val row = bucketer.bucket(listOf(ev))[0].rows[0]
        assertEquals("content://com.android.calendar/events/42", row.deepLink)
    }
}
