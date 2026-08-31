package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sections for the to-do list, in display order. Only non-empty groups are returned. Open items
 * sort by due date (undated last) then priority; completed items keep their stored order.
 */
enum class TodoGroup { OVERDUE, TODAY, UPCOMING, NO_DATE, COMPLETED }

object TodoGroups {
    fun of(todos: List<LocalTodo>, today: LocalDate, zone: ZoneId): List<Pair<TodoGroup, List<LocalTodo>>> {
        val (done, open) = todos.partition { it.completed }
        fun dueDate(t: LocalTodo): LocalDate? =
            t.dueMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

        val sortedOpen = open.sortedWith(
            compareBy<LocalTodo, Long?>(nullsLast(), { it.dueMillis })
                .thenByDescending { it.priority.value },
        )
        return listOf(
            TodoGroup.OVERDUE to sortedOpen.filter { dueDate(it)?.isBefore(today) == true },
            TodoGroup.TODAY to sortedOpen.filter { dueDate(it) == today },
            TodoGroup.UPCOMING to sortedOpen.filter { dueDate(it)?.isAfter(today) == true },
            TodoGroup.NO_DATE to sortedOpen.filter { it.dueMillis == null },
            TodoGroup.COMPLETED to done,
        ).filter { it.second.isNotEmpty() }
    }
}
