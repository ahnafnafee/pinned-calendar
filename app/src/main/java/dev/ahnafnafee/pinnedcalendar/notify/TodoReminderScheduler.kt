package dev.ahnafnafee.pinnedcalendar.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import dev.ahnafnafee.pinnedcalendar.domain.TodoReminders

/**
 * Keeps exactly one regular due-time alarm pending: the next open to-do due in the future. Every
 * agenda refresh re-syncs it, and the receiver re-syncs after firing. Snoozed occurrences use
 * separate, identity-scoped one-shot alarms so several reminders can be deferred independently.
 */
object TodoReminderScheduler {

    fun sync(context: Context, enabled: Boolean, todos: List<LocalTodo>, nowMillis: Long = System.currentTimeMillis()) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val intent = pendingIntent(context)
        val next = if (enabled) TodoReminders.nextDueAt(todos, nowMillis) else null
        if (next == null) {
            alarms.cancel(intent)
            return
        }
        schedule(alarms, next, intent)
    }

    /** Defers one specific occurrence without changing the to-do's actual due time. */
    fun scheduleSnooze(context: Context, todoId: String, dueMillis: Long, triggerAtMillis: Long) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        schedule(alarms, triggerAtMillis, snoozePendingIntent(context, todoId, dueMillis))
    }

    fun cancelSnooze(context: Context, todoId: String, dueMillis: Long) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        alarms.cancel(snoozePendingIntent(context, todoId, dueMillis))
    }

    private fun schedule(alarms: AlarmManager, triggerAtMillis: Long, intent: PendingIntent) {
        // Exact when the user has allowed it; otherwise the inexact variant may land a few
        // minutes late under Doze, which a reminder survives better than not firing at all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 2,
            Intent(context, TodoReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun snoozePendingIntent(context: Context, todoId: String, dueMillis: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            TodoReminderReceiver.occurrenceIntent(
                context,
                TodoReminderReceiver.ACTION_SNOOZE_FIRED,
                todoId,
                dueMillis,
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
