package com.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import android.app.NotificationManager
import androidx.core.content.getSystemService
import com.ahnafnafee.pinnedcalendar.data.NotificationPriority
import com.ahnafnafee.pinnedcalendar.domain.model.DaySection
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AgendaNotificationBuilderTest {

    private val ctx = RuntimeEnvironment.getApplication()
    private val mgr = ctx.getSystemService<NotificationManager>()!!

    private fun sampleContent() = NotificationContent(
        headerCount = 2,
        collapsedLine = "9:00 Standup",
        collapsedColorHex = "#039BE5",
        sections = listOf(
            DaySection(
                "TODAY · MON 1", true,
                listOf(
                    NotificationRow("9:00", "Standup", "#039BE5", false, false),
                    NotificationRow("17:00", "Pay rent", null, true, false),
                ),
            ),
        ),
        moreCount = 0,
        isEmpty = false,
    )

    @Test fun builds_an_ongoing_notification_on_the_priority_channel() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.TOP)
        val channelId = ChannelManager.channelId(NotificationPriority.TOP)
        val n = AgendaNotificationBuilder(ctx).build(sampleContent(), NotificationPriority.TOP)
        assertEquals(channelId, n.channelId)
        assertTrue("expected FLAG_ONGOING_EVENT", (n.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertNotNull("expected a delete intent for self-heal", n.deleteIntent)
        assertNotNull("expected a content intent that opens the app", n.contentIntent)
    }

    @Test fun empty_content_still_builds() {
        val channelId = ChannelManager.channelId(NotificationPriority.NORMAL)
        ChannelManager.ensureChannel(ctx, NotificationPriority.NORMAL)
        val empty = NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)
        val n = AgendaNotificationBuilder(ctx).build(empty, NotificationPriority.NORMAL)
        assertEquals(channelId, n.channelId)
    }

    @Suppress("DEPRECATION") // legacy Notification.priority is the field this lever populates
    @Test fun top_priority_sorts_newest_with_max_priority() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.TOP)
        val top = AgendaNotificationBuilder(ctx).build(sampleContent(), NotificationPriority.TOP)
        assertEquals(Notification.PRIORITY_MAX, top.priority)
        // Far-future post time keeps Top at the head of the High bucket as new notifications arrive.
        assertTrue("Top should post far in the future", top.`when` > System.currentTimeMillis())

        ChannelManager.ensureChannel(ctx, NotificationPriority.NORMAL)
        val normal = AgendaNotificationBuilder(ctx).build(sampleContent(), NotificationPriority.NORMAL)
        assertEquals(Notification.PRIORITY_DEFAULT, normal.priority)
    }

    @Test fun each_priority_maps_to_its_importance() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.TOP)
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            mgr.getNotificationChannel(ChannelManager.channelId(NotificationPriority.TOP)).importance,
        )

        ChannelManager.ensureChannel(ctx, NotificationPriority.SILENT)
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            mgr.getNotificationChannel(ChannelManager.channelId(NotificationPriority.SILENT)).importance,
        )
    }

    @Test fun switching_priority_retires_the_previous_channel() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.TOP)
        assertNotNull(mgr.getNotificationChannel(ChannelManager.channelId(NotificationPriority.TOP)))

        ChannelManager.ensureChannel(ctx, NotificationPriority.NORMAL)
        assertNull(
            "the unselected TOP channel should be removed",
            mgr.getNotificationChannel(ChannelManager.channelId(NotificationPriority.TOP)),
        )
        assertNotNull(mgr.getNotificationChannel(ChannelManager.channelId(NotificationPriority.NORMAL)))
    }
}
