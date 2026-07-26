package dev.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import android.app.NotificationManager
import androidx.core.content.getSystemService
import dev.ahnafnafee.pinnedcalendar.data.NotificationPriority
import dev.ahnafnafee.pinnedcalendar.domain.model.DaySection
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationRow
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

    @Test fun omits_expanded_layout_when_all_rows_fit_in_compact_view() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.NORMAL)
        val n = AgendaNotificationBuilder(ctx).build(
            sampleContent(),
            NotificationPriority.NORMAL,
            collapsedItems = 2,
        )
        assertNull(n.bigContentView)
    }

    @Test fun includes_expanded_layout_when_compact_view_hides_rows() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.NORMAL)
        val n = AgendaNotificationBuilder(ctx).build(
            sampleContent(),
            NotificationPriority.NORMAL,
            collapsedItems = 1,
        )
        assertNotNull(n.bigContentView)
    }

    @Suppress("DEPRECATION") // legacy Notification.priority is the field this lever populates
    @Test fun every_build_stamps_a_fresh_ranking_timestamp_with_mapped_priority() {
        ChannelManager.ensureChannel(ctx, NotificationPriority.TOP)
        val before = System.currentTimeMillis()
        val top = AgendaNotificationBuilder(ctx).build(sampleContent(), NotificationPriority.TOP)
        val after = System.currentTimeMillis()
        assertEquals(Notification.PRIORITY_MAX, top.priority)
        // The shade breaks importance ties by ranking time, and the OS refreshes that only from
        // an app-provided, NON-future 'when' (future values are ignored and updates inherit the
        // old time). A fresh stamp on every build keeps the pin newest in its tier.
        assertTrue("'when' must be a fresh, non-future timestamp", top.`when` in before..after)

        ChannelManager.ensureChannel(ctx, NotificationPriority.NORMAL)
        val normal = AgendaNotificationBuilder(ctx).build(sampleContent(), NotificationPriority.NORMAL)
        assertEquals(Notification.PRIORITY_DEFAULT, normal.priority)
        assertTrue("'when' is stamped for every priority", normal.`when` >= before)
    }

    @Test fun pin_is_silent_so_it_never_heads_up() {
        // Every fresh post (first pin, self-heal, app update) must slide into the shade without
        // peeking on screen. Compat's setSilent contract: alerts are deferred to a group summary
        // that never exists.
        ChannelManager.ensureChannel(ctx, NotificationPriority.TOP)
        val n = AgendaNotificationBuilder(ctx).build(sampleContent(), NotificationPriority.TOP)
        assertEquals("silent", n.group)
        assertEquals(Notification.GROUP_ALERT_SUMMARY, n.groupAlertBehavior)
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
