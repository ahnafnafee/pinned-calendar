package dev.ahnafnafee.pinnedcalendar.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Refreshes the pin when the local date changes. The posted notification carries fixed
 * "Today"/"Tomorrow" labels, so it goes stale at midnight. Periodic work cannot be trusted to
 * catch that, because Doze and OEM battery managers defer jobs overnight. An allow-while-idle
 * alarm still fires then. Clock and time-zone changes move midnight, so they refresh (and re-arm)
 * too. The refresh runs here directly: going through WorkManager would reintroduce the deferral.
 */
class DayRolloverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        val appContext = context.applicationContext
        receiverScope.launch {
            try {
                AgendaNotifier(appContext).refresh() // re-arms the next midnight via sync()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        internal const val ACTION_DAY_ROLLOVER = "dev.ahnafnafee.pinnedcalendar.action.DAY_ROLLOVER"
        private val HANDLED_ACTIONS =
            setOf(ACTION_DAY_ROLLOVER, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)

        /** Keeps one wake alarm pending at the next local midnight while the pin is on. */
        fun sync(context: Context, enabled: Boolean, now: ZonedDateTime) {
            val alarms = context.getSystemService<AlarmManager>() ?: return
            val intent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, DayRolloverReceiver::class.java).setAction(ACTION_DAY_ROLLOVER),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            if (enabled) alarms.setWakeWhileIdle(nextDayStartMillis(now), intent) else alarms.cancel(intent)
        }

        /** First instant of tomorrow; java.time moves it past a DST gap that swallows midnight. */
        fun nextDayStartMillis(now: ZonedDateTime): Long =
            now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
    }
}
