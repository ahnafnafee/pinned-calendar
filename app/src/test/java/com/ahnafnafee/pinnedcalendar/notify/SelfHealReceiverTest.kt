package com.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SelfHealReceiverTest {

    private val ctx = RuntimeEnvironment.getApplication()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)

    @Test fun reposts_notification_on_receive_when_enabled() {
        // Default pin state is enabled, so a dismissal must immediately re-post.
        SelfHealReceiver().onReceive(ctx, Intent())
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }
}
