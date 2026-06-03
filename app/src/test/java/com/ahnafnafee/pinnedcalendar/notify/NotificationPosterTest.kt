package com.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import com.ahnafnafee.pinnedcalendar.data.NotificationPriority
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationPosterTest {

    private val ctx = RuntimeEnvironment.getApplication()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)
    private val poster = NotificationPoster(ctx)
    private val content = NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)

    @Test fun posts_when_enabled() {
        val showing = poster.apply(pinEnabled = true, priority = NotificationPriority.TOP, content = content)
        assertEquals(true, showing)
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }

    @Test fun cancels_when_disabled() {
        poster.apply(pinEnabled = true, priority = NotificationPriority.TOP, content = content)
        val showing = poster.apply(pinEnabled = false, priority = NotificationPriority.TOP, content = content)
        assertEquals(false, showing)
        assertEquals(0, shadowOf(mgr).activeNotifications.size)
    }
}
