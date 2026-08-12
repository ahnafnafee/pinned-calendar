package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sections for the to-do list, in display order. Only non-empty groups are returned. Open items
 * sort by due date (undated last) then priority; completed items keep their stored order.
 */
object TodoGroups {
    fun of(todos: List<LocalTodo>, today: LocalDate, zone: ZoneId): List<Pair<String, List<LocalTodo>>> {
        val (done, open) = todos.partition { it.completed }
        fun dueDate(t: LocalTodo): LocalDate? =
            t.dueMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

        val sortedOpen = open.sortedWith(
            compareBy<LocalTodo, Long?>(nullsLast(), { it.dueMillis })
                .thenByDescending { it.priority.value },
        )
        return listOf(
            "Overdue" to sortedOpen.filter { dueDate(it)?.isBefore(today) == true },
            "Today" to sortedOpen.filter { dueDate(it) == today },
            "Upcoming" to sortedOpen.filter { dueDate(it)?.isAfter(today) == true },
            "No date" to sortedOpen.filter { it.dueMillis == null },
            "Completed" to done,
        ).filter { it.second.isNotEmpty() }
    }
}
