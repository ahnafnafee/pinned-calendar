package dev.ahnafnafee.pinnedcalendar.notify

import android.app.AlarmManager
import android.content.Intent
import androidx.core.content.getSystemService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Suppress("DEPRECATION") // Robolectric exposes ScheduledAlarm.operation only through its legacy field.
class DayRolloverReceiverTest {

    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService<AlarmManager>()!!
    private val moscow = ZoneId.of("Europe/Moscow")

    @Before fun resetAlarms() = ShadowAlarmManager.reset()

    private fun rolloverAlarms() = shadowOf(alarmManager).scheduledAlarms.filter {
        shadowOf(it.operation).savedIntent.component?.className == DayRolloverReceiver::class.java.name
    }

    @Test fun arms_a_wake_alarm_at_the_next_local_midnight() {
        DayRolloverReceiver.sync(context, enabled = true, now = ZonedDateTime.of(2026, 11, 23, 21, 30, 0, 0, moscow))

        val alarm = rolloverAlarms().single()
        assertEquals(ZonedDateTime.of(2026, 11, 24, 0, 0, 0, 0, moscow).toInstant().toEpochMilli(), alarm.triggerAtTime)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(DayRolloverReceiver.ACTION_DAY_ROLLOVER, shadowOf(alarm.operation).savedIntent.action)
    }

    @Test fun disabled_pin_cancels_the_rollover_alarm() {
        val now = ZonedDateTime.of(2026, 11, 23, 21, 30, 0, 0, moscow)
        DayRolloverReceiver.sync(context, enabled = true, now = now)
        DayRolloverReceiver.sync(context, enabled = false, now = now)

        assertTrue(rolloverAlarms().isEmpty())
    }

    @Test fun midnight_inside_a_dst_gap_uses_the_first_instant_of_the_day() {
        // Havana springs forward at 00:00, so 8 March 2026 starts at 01:00.
        val havana = ZoneId.of("America/Havana")
        val start = DayRolloverReceiver.nextDayStartMillis(ZonedDateTime.of(2026, 3, 7, 22, 0, 0, 0, havana))

        assertEquals(LocalDateTime.of(2026, 3, 8, 1, 0), Instant.ofEpochMilli(start).atZone(havana).toLocalDateTime())
    }

    @Test fun every_agenda_refresh_arms_the_rollover_alarm() = runTest {
        AgendaNotifier(context).refresh()

        assertEquals(1, rolloverAlarms().size)
    }

    @Test fun clock_and_time_zone_changes_reach_the_receiver() {
        listOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED).forEach { action ->
            val receivers = context.packageManager
                .queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
            assertTrue(action, receivers.any { it.activityInfo.name == DayRolloverReceiver::class.java.name })
        }
    }
}
