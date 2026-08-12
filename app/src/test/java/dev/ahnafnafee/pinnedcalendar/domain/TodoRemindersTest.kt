package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoRemindersTest {

    private val now = 1_000_000L

    private fun todo(id: String, due: Long?, done: Boolean = false) =
        LocalTodo(id = id, title = "t$id", dueMillis = due, completed = done)

    @Test fun `next reminder is the earliest open future due`() {
        val next = TodoReminders.nextDueAt(
            listOf(
                todo("late", now + 50_000),
                todo("soon", now + 10_000),
                todo("past", now - 10_000),
                todo("doneSoon", now + 5_000, done = true),
                todo("undated", null),
            ),
            now,
        )
        assertEquals(now + 10_000, next)
    }

    @Test fun `no future dues means no alarm`() {
        assertNull(TodoReminders.nextDueAt(listOf(todo("past", now - 1), todo("undated", null)), now))
        assertNull(TodoReminders.nextDueAt(emptyList(), now))
    }

    @Test fun `dueNow catches items inside the window and skips done, future, and stale ones`() {
        val due = TodoReminders.dueNow(
            listOf(
                todo("exact", now),
                todo("recent", now - TodoReminders.DUE_WINDOW_MILLIS + 1),
                todo("stale", now - TodoReminders.DUE_WINDOW_MILLIS),
                todo("future", now + 1),
                todo("doneNow", now, done = true),
            ),
            now,
        )
        assertEquals(listOf("exact", "recent"), due.map { it.id })
    }
}
