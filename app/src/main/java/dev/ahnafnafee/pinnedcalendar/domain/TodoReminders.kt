package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo

/**
 * Due-time reminder selection. Reminders are one-shot, dismissible notifications for open,
 * dated to-dos; the pinned agenda stays the persistent surface and is untouched by them.
 */
object TodoReminders {
    /** How far back a firing alarm still counts a to-do as "due now" (batched or deferred alarms). */
    const val DUE_WINDOW_MILLIS: Long = 5 * 60_000L

    /** The next instant a reminder should fire, or null when nothing is scheduled ahead. */
    fun nextDueAt(todos: List<LocalTodo>, nowMillis: Long): Long? =
        todos.asSequence()
            .filter { !it.completed }
            .mapNotNull { it.dueMillis }
            .filter { it > nowMillis }
            .minOrNull()

    /** Open to-dos whose due time just arrived, inclusive of a short catch-up window. */
    fun dueNow(todos: List<LocalTodo>, nowMillis: Long): List<LocalTodo> =
        todos.filter { t ->
            !t.completed && t.dueMillis != null &&
                t.dueMillis > nowMillis - DUE_WINDOW_MILLIS && t.dueMillis <= nowMillis
        }
}
