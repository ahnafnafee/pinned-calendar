package dev.ahnafnafee.pinnedcalendar.notify

import android.app.AlarmManager
import androidx.core.content.getSystemService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Suppress("DEPRECATION") // Robolectric exposes ScheduledAlarm.operation only through its legacy field.
class TodoReminderSchedulerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    @Test fun snoozed_occurrences_use_independent_one_shot_alarms() {
        ShadowAlarmManager.reset()

        TodoReminderScheduler.scheduleSnooze(context, "1", dueMillis = 100L, triggerAtMillis = 1_900L)
        TodoReminderScheduler.scheduleSnooze(context, "2", dueMillis = 200L, triggerAtMillis = 2_000L)

        val alarms = shadowOf(alarmManager).scheduledAlarms.sortedBy { it.triggerAtTime }
        assertEquals(2, alarms.size)
        assertEquals(1_900L, alarms[0].triggerAtTime)
        assertEquals(2_000L, alarms[1].triggerAtTime)
        alarms.forEach { alarm ->
            val intent = shadowOf(alarm.operation).savedIntent
            assertEquals(TodoReminderReceiver.ACTION_SNOOZE_FIRED, intent.action)
        }
    }

    @Test fun completing_an_occurrence_cancels_its_snooze_alarm_only() {
        ShadowAlarmManager.reset()
        TodoReminderScheduler.scheduleSnooze(context, "1", dueMillis = 100L, triggerAtMillis = 1_900L)
        TodoReminderScheduler.scheduleSnooze(context, "2", dueMillis = 200L, triggerAtMillis = 2_000L)

        TodoReminderScheduler.cancelSnooze(context, "1", dueMillis = 100L)

        val remaining = shadowOf(alarmManager).scheduledAlarms.single()
        val intent = shadowOf(remaining.operation).savedIntent
        assertEquals("2", intent.getStringExtra(TodoReminderReceiver.EXTRA_TODO_ID))
        assertEquals(200L, intent.getLongExtra(TodoReminderReceiver.EXTRA_DUE_MILLIS, 0L))
    }
}
