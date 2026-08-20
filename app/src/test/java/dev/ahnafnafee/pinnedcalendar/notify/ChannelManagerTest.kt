package dev.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ChannelManagerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService<NotificationManager>()!!

    @Test fun reminder_channel_migrates_to_high_importance_for_heads_up_alerts() {
        manager.createNotificationChannel(
            NotificationChannel(
                LEGACY_REMINDER_CHANNEL_ID,
                "Task reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        ChannelManager.ensureReminderChannel(context)

        assertNotEquals(LEGACY_REMINDER_CHANNEL_ID, ChannelManager.REMINDER_CHANNEL_ID)
        val channel = manager.getNotificationChannel(ChannelManager.REMINDER_CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertNull(manager.getNotificationChannel(LEGACY_REMINDER_CHANNEL_ID))
    }

    private companion object {
        const val LEGACY_REMINDER_CHANNEL_ID = "todo_reminders"
    }
}
