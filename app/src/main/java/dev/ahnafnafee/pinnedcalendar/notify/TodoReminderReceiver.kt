package dev.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.getSystemService
import dev.ahnafnafee.pinnedcalendar.data.SettingsRepository
import dev.ahnafnafee.pinnedcalendar.data.settingsDataStore
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import dev.ahnafnafee.pinnedcalendar.domain.TodoReminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires at a to-do's due time and posts a regular, actionable notification for it, then arms the
 * alarm for the next one. It also handles completion and snooze actions for the exact occurrence.
 * Deliberately separate from the pinned agenda: these behave like everyday notifications (sound,
 * swipe to clear) while the pin stays ongoing and self-healing.
 */
class TodoReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val now = System.currentTimeMillis()
        val pending = goAsync()
        val appContext = context.applicationContext
        receiverScope.launch {
            try {
                when (intent?.action) {
                    ACTION_MARK_DONE -> markDone(appContext, intent, now)
                    ACTION_SNOOZE -> snooze(appContext, intent, now)
                    ACTION_SNOOZE_FIRED -> showSnoozed(appContext, intent, now)
                    else -> showDue(appContext, now)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun showDue(context: Context, nowMillis: Long) {
        val app = context.applicationContext
        val settings = SettingsRepository(app.settingsDataStore).snapshot()
        if (!settings.todoReminders) return
        val todos = TodoRepository(app.settingsDataStore).snapshot()
        val manager = context.getSystemService<NotificationManager>()
        if (manager != null) {
            ChannelManager.ensureReminderChannel(context)
            val builder = TodoReminderNotificationBuilder(context)
            TodoReminders.dueNow(todos, nowMillis).forEach { todo ->
                manager.notify(reminderId(todo.id), builder.build(todo))
            }
        }
        TodoReminderScheduler.sync(context, enabled = true, todos = todos, nowMillis = nowMillis)
    }

    private suspend fun markDone(context: Context, intent: Intent, nowMillis: Long) {
        val occurrence = intent.occurrence() ?: return
        val app = context.applicationContext
        TodoRepository(app.settingsDataStore).completeOccurrence(
            id = occurrence.todoId,
            expectedDueMillis = occurrence.dueMillis,
            completedAtMillis = nowMillis,
        )
        TodoReminderScheduler.cancelSnooze(context, occurrence.todoId, occurrence.dueMillis)
        context.getSystemService<NotificationManager>()?.cancel(reminderId(occurrence.todoId))
        AgendaNotifier(context).refresh()
    }

    private suspend fun snooze(context: Context, intent: Intent, nowMillis: Long) {
        val occurrence = intent.occurrence() ?: return
        val app = context.applicationContext
        val settings = SettingsRepository(app.settingsDataStore).snapshot()
        val todos = TodoRepository(app.settingsDataStore).snapshot()
        val isCurrent = todos.any { todo ->
            todo.id == occurrence.todoId && !todo.completed && todo.dueMillis == occurrence.dueMillis
        }
        if (settings.todoReminders && isCurrent) {
            TodoReminderScheduler.scheduleSnooze(
                context,
                occurrence.todoId,
                occurrence.dueMillis,
                nowMillis + SNOOZE_DURATION_MILLIS,
            )
        }
        context.getSystemService<NotificationManager>()?.cancel(reminderId(occurrence.todoId))
        TodoReminderScheduler.sync(
            context,
            enabled = settings.todoReminders,
            todos = todos,
            nowMillis = nowMillis,
        )
    }

    private suspend fun showSnoozed(context: Context, intent: Intent, nowMillis: Long) {
        val occurrence = intent.occurrence() ?: return
        val app = context.applicationContext
        val settings = SettingsRepository(app.settingsDataStore).snapshot()
        val todos = TodoRepository(app.settingsDataStore).snapshot()
        val todo = todos.firstOrNull { candidate ->
            candidate.id == occurrence.todoId &&
                !candidate.completed &&
                candidate.dueMillis == occurrence.dueMillis
        }
        if (settings.todoReminders && todo != null) {
            context.getSystemService<NotificationManager>()?.let { manager ->
                ChannelManager.ensureReminderChannel(context)
                manager.notify(reminderId(todo.id), TodoReminderNotificationBuilder(context).build(todo))
            }
        }
        TodoReminderScheduler.sync(
            context,
            enabled = settings.todoReminders,
            todos = todos,
            nowMillis = nowMillis,
        )
    }

    private fun Intent.occurrence(): Occurrence? {
        val todoId = getStringExtra(EXTRA_TODO_ID) ?: return null
        if (!hasExtra(EXTRA_DUE_MILLIS)) return null
        return Occurrence(todoId, getLongExtra(EXTRA_DUE_MILLIS, 0L))
    }

    private data class Occurrence(val todoId: String, val dueMillis: Long)

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        internal const val ACTION_MARK_DONE =
            "dev.ahnafnafee.pinnedcalendar.action.MARK_TODO_DONE"
        internal const val ACTION_SNOOZE =
            "dev.ahnafnafee.pinnedcalendar.action.SNOOZE_TODO"
        internal const val ACTION_SNOOZE_FIRED =
            "dev.ahnafnafee.pinnedcalendar.action.SHOW_SNOOZED_TODO"
        internal const val EXTRA_TODO_ID = "todo_id"
        internal const val EXTRA_DUE_MILLIS = "due_millis"
        internal const val SNOOZE_DURATION_MILLIS = 30 * 60_000L

        internal fun occurrenceIntent(
            context: Context,
            action: String,
            todoId: String,
            dueMillis: Long,
        ): Intent = Intent(context, TodoReminderReceiver::class.java)
            .setAction(action)
            // PendingIntent identity ignores extras. A per-occurrence URI keeps stale actions from
            // being silently updated to target a later occurrence of the same recurring to-do.
            .setData(
                Uri.Builder()
                    .scheme("pinnedcalendar")
                    .authority("todo-reminder")
                    .appendPath(todoId)
                    .appendPath(dueMillis.toString())
                    .build(),
            )
            .putExtra(EXTRA_TODO_ID, todoId)
            .putExtra(EXTRA_DUE_MILLIS, dueMillis)

        // Keeps reminder ids clear of the pin's fixed id; a to-do edits over its own reminder.
        internal fun reminderId(todoId: String): Int =
            2000 + (todoId.toIntOrNull() ?: (todoId.hashCode() and 0xFFFF))
    }
}
