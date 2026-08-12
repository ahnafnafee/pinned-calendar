package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoPriority
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoGroupsTest {

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 6, 2)

    private fun dueAt(date: LocalDate, time: LocalTime = LocalTime.of(9, 0)): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun todo(
        id: String,
        due: Long?,
        done: Boolean = false,
        priority: TodoPriority = TodoPriority.NONE,
    ) = LocalTodo(id = id, title = "t$id", dueMillis = due, completed = done, priority = priority)

    @Test
    fun `sections appear in display order and empty ones are dropped`() {
        val groups = TodoGroups.of(
            listOf(
                todo("done", dueAt(today), done = true),
                todo("later", dueAt(today.plusDays(3))),
                todo("past", dueAt(today.minusDays(1))),
                todo("now", dueAt(today)),
                todo("someday", null),
            ),
            today,
            zone,
        )
        assertEquals(listOf("Overdue", "Today", "Upcoming", "No date", "Completed"), groups.map { it.first })

        val onlyToday = TodoGroups.of(listOf(todo("now", dueAt(today))), today, zone)
        assertEquals(listOf("Today"), onlyToday.map { it.first })
    }

    @Test
    fun `a day boundary is judged in the given zone, not by raw millis`() {
        // 03:00 UTC on June 3 is still June 2 in New York, so it belongs to Today, not Upcoming.
        val lateEvening = dueAt(today, LocalTime.of(23, 0))
        val groups = TodoGroups.of(listOf(todo("evening", lateEvening)), today, zone)
        assertEquals(listOf("Today"), groups.map { it.first })
    }

    @Test
    fun `open items sort by due date then priority within a section`() {
        val groups = TodoGroups.of(
            listOf(
                todo("lowLate", dueAt(today, LocalTime.of(17, 0)), priority = TodoPriority.LOW),
                todo("nonePrio", dueAt(today, LocalTime.of(9, 0))),
                todo("highSameTime", dueAt(today, LocalTime.of(9, 0)), priority = TodoPriority.HIGH),
            ),
            today,
            zone,
        )
        assertEquals(listOf("highSameTime", "nonePrio", "lowLate"), groups.single().second.map { it.id })
    }
}
