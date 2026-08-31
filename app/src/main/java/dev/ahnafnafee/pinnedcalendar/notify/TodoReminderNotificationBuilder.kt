package dev.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo

/** Builds the dismissible alert for one scheduled to-do occurrence. */
internal class TodoReminderNotificationBuilder(private val context: Context) {

    fun build(todo: LocalTodo): Notification {
        val dueMillis = requireNotNull(todo.dueMillis) { "A reminder action requires a due time" }
        return NotificationCompat.Builder(context, ChannelManager.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setColor(
                todo.priority.colorHex?.toColorInt()
                    ?: AccentResolver.accentColor(context),
            )
            .setContentTitle(todo.title)
            .setContentText(todo.notes.ifBlank { context.getString(R.string.todo_reminder_due_now) })
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(AppLaunch.pendingIntent(context))
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_done,
                    context.getString(R.string.todo_reminder_mark_done),
                    actionPendingIntent(TodoReminderReceiver.ACTION_MARK_DONE, todo.id, dueMillis),
                ).setShowsUserInterface(false).build(),
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_snooze,
                    context.getString(R.string.todo_reminder_snooze),
                    actionPendingIntent(TodoReminderReceiver.ACTION_SNOOZE, todo.id, dueMillis),
                ).setShowsUserInterface(false).build(),
            )
            .build()
    }

    private fun actionPendingIntent(action: String, todoId: String, dueMillis: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            TodoReminderReceiver.occurrenceIntent(context, action, todoId, dueMillis),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
