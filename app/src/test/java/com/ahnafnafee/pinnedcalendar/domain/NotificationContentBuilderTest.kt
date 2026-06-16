package com.ahnafnafee.pinnedcalendar.domain

import com.ahnafnafee.pinnedcalendar.data.DisplaySettings
import com.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import com.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class NotificationContentBuilderTest {
    private val zone = ZoneId.of("America/New_York")
    private val clock = Clock.fixed(Instant.parse("2026-06-01T06:00:00Z"), zone)
    private val builder = NotificationContentBuilder(DayBucketer(clock, zone))

    private fun item(daysAhead: Long, h: Int, title: String, kind: ItemKind = ItemKind.EVENT, completed: Boolean = false): AgendaItem {
        val date = java.time.LocalDate.now(clock.withZone(zone)).plusDays(daysAhead)
        return AgendaItem(
            title, kind, title,
            date.atTime(LocalTime.of(h, 0)).atZone(zone).toInstant(),
            colorHex = if (kind == ItemKind.EVENT) "#039BE5" else null, completed = completed,
        )
    }

    @Test fun empty_input_is_empty_state() {
        val c = builder.build(emptyList(), DisplaySettings())
        assertTrue(c.isEmpty)
        assertEquals(0, c.headerCount)
    }

    @Test fun collapsed_line_is_the_chronologically_next_item() {
        val c = builder.build(listOf(item(1, 10, "B"), item(0, 9, "A")), DisplaySettings())
        assertEquals("9:00 AM A", c.collapsedLine)
        assertFalse(c.isEmpty)
    }

    @Test fun caps_rows_at_maxItems_and_reports_more_count() {
        val items = (0 until 12).map { item(it.toLong() % 6, 8 + it % 6, "i$it") }
        val c = builder.build(items, DisplaySettings(maxItems = 5))
        val shown = c.sections.sumOf { it.rows.size }
        assertEquals(5, shown)
        assertEquals(7, c.moreCount)
        assertEquals(12, c.headerCount)
    }

    @Test fun hides_completed_tasks_when_enabled() {
        val items = listOf(item(0, 9, "open"), item(0, 10, "done", ItemKind.TASK, completed = true))
        val c = builder.build(items, DisplaySettings(hideCompletedTasks = true))
        assertEquals(1, c.headerCount)
        assertEquals("open", c.sections[0].rows[0].title)
    }

    @Test fun ungrouped_mode_produces_single_headerless_section() {
        val items = listOf(item(0, 9, "A"), item(1, 10, "B"))
        val c = builder.build(items, DisplaySettings(groupByDay = false))
        assertEquals(1, c.sections.size)
        assertEquals("", c.sections[0].header)
        assertEquals(2, c.sections[0].rows.size)
    }
}
