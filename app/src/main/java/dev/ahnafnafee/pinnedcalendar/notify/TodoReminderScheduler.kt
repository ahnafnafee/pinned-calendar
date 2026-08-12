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
 * Keeps exactly one alarm pending: the next open to-do due in the future. Every agenda refresh
 * re-syncs it, and the receiver re-syncs after firing, so the chain is self-healing without
 * per-item bookkeeping.
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
        // Exact when the user has allowed it; otherwise the inexact variant may land a few
        // minutes late under Doze, which a reminder survives better than not firing at all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 2,
            Intent(context, TodoReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
