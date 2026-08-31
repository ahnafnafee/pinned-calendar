package dev.ahnafnafee.pinnedcalendar.notify

import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TodoReminderNotificationBuilderTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test fun reminder_offers_done_and_thirty_minute_snooze_actions_for_its_occurrence() {
        ChannelManager.ensureReminderChannel(context)
        val todo = LocalTodo(id = "task/one", title = "Send invoice", dueMillis = 123_456L)

        val notification = TodoReminderNotificationBuilder(context).build(todo)

        assertEquals(2, notification.actions.size)
        assertEquals("Mark done", notification.actions[0].title.toString())
        assertEquals("Snooze 30 min", notification.actions[1].title.toString())

        val doneIntent = shadowOf(notification.actions[0].actionIntent).savedIntent
        assertEquals(TodoReminderReceiver.ACTION_MARK_DONE, doneIntent.action)
        assertEquals(todo.id, doneIntent.getStringExtra(TodoReminderReceiver.EXTRA_TODO_ID))
        assertEquals(todo.dueMillis, doneIntent.getLongExtra(TodoReminderReceiver.EXTRA_DUE_MILLIS, 0L))

        val snoozeIntent = shadowOf(notification.actions[1].actionIntent).savedIntent
        assertEquals(TodoReminderReceiver.ACTION_SNOOZE, snoozeIntent.action)
        assertEquals(todo.id, snoozeIntent.getStringExtra(TodoReminderReceiver.EXTRA_TODO_ID))
        assertEquals(todo.dueMillis, snoozeIntent.getLongExtra(TodoReminderReceiver.EXTRA_DUE_MILLIS, 0L))
    }

    @Test fun recurring_occurrences_get_distinct_action_pending_intents() {
        val first = TodoReminderNotificationBuilder(context).build(
            LocalTodo(id = "1", title = "Water plants", dueMillis = 100L),
        )
        val next = TodoReminderNotificationBuilder(context).build(
            LocalTodo(id = "1", title = "Water plants", dueMillis = 200L),
        )

        assertNotEquals(first.actions[0].actionIntent, next.actions[0].actionIntent)
        assertNotEquals(first.actions[1].actionIntent, next.actions[1].actionIntent)
    }
}
