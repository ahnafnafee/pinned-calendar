package com.ahnafnafee.pinnedcalendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SampleAgendaTest {
    private val zone = ZoneId.of("America/New_York")
    private val clock = Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), zone)

    @Test fun returns_eight_items_within_the_week() {
        val items = SampleAgenda.items(clock, zone)
        assertEquals(8, items.size)
        assertTrue(items.all { it.start != null })
    }

    @Test fun first_item_is_todays_standup() {
        val items = SampleAgenda.items(clock, zone).sortedBy { it.start }
        assertEquals("Team standup", items.first().title)
    }
}
