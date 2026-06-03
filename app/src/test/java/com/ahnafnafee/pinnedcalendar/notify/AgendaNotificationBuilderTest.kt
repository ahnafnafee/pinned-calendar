package com.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import com.ahnafnafee.pinnedcalendar.domain.model.DaySection
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AgendaNotificationBuilderTest {

    private val ctx = RuntimeEnvironment.getApplication()

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

    @Test fun builds_an_ongoing_notification_on_our_channel() {
        ChannelManager.ensureChannel(ctx)
        val n = AgendaNotificationBuilder(ctx).build(sampleContent())
        assertEquals(ChannelManager.CHANNEL_ID, n.channelId)
        assertTrue("expected FLAG_ONGOING_EVENT", (n.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertNotNull("expected a delete intent for self-heal", n.deleteIntent)
    }

    @Test fun empty_content_still_builds() {
        ChannelManager.ensureChannel(ctx)
        val empty = NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)
        val n = AgendaNotificationBuilder(ctx).build(empty)
        assertEquals(ChannelManager.CHANNEL_ID, n.channelId)
    }
}
