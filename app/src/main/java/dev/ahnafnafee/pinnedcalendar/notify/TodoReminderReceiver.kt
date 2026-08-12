package dev.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.data.SettingsRepository
import dev.ahnafnafee.pinnedcalendar.data.settingsDataStore
import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import dev.ahnafnafee.pinnedcalendar.domain.TodoReminders
import kotlinx.coroutines.runBlocking

/**
 * Fires at a to-do's due time and posts a regular, dismissible notification for it, then arms
 * the alarm for the next one. Deliberately separate from the pinned agenda: these behave like
 * everyday notifications (sound, swipe to clear) while the pin stays ongoing and self-healing.
 */
class TodoReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val now = System.currentTimeMillis()
        val pending = goAsync()
        try {
            runBlocking {
                val app = context.applicationContext
                val settings = SettingsRepository(app.settingsDataStore).snapshot()
                if (!settings.todoReminders) return@runBlocking
                val todos = TodoRepository(app.settingsDataStore).snapshot()
                val mgr = context.getSystemService<NotificationManager>() ?: return@runBlocking
                ChannelManager.ensureReminderChannel(context)
                TodoReminders.dueNow(todos, now).forEach { t ->
                    mgr.notify(reminderId(t), build(context, t))
                }
                TodoReminderScheduler.sync(context, enabled = true, todos = todos, nowMillis = now)
            }
        } finally {
            pending?.finish()
        }
    }

    private fun build(context: Context, todo: LocalTodo) =
        NotificationCompat.Builder(context, ChannelManager.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setColor(
                todo.priority.colorHex?.let { Color.parseColor(it) }
                    ?: AccentResolver.accentColor(context),
            )
            .setContentTitle(todo.title)
            .setContentText(todo.notes.ifBlank { "Due now" })
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(AppLaunch.pendingIntent(context))
            .build()

    private companion object {
        // Keeps reminder ids clear of the pin's fixed id; a to-do edits over its own reminder.
        fun reminderId(todo: LocalTodo): Int = 2000 + (todo.id.toIntOrNull() ?: todo.id.hashCode() and 0xFFFF)
    }
}
