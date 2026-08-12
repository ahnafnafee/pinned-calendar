package dev.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.widget.FrameLayout
import android.widget.TextView
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.data.NotificationPriority
import dev.ahnafnafee.pinnedcalendar.domain.model.DaySection
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationRow
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
    private val emptyContent = NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)
    private val content = NotificationContent(
        headerCount = 1,
        collapsedLine = "9:00 Standup",
        collapsedColorHex = "#039BE5",
        sections = listOf(
            DaySection("", false, listOf(NotificationRow("9:00", "Standup", "#039BE5", false, false))),
        ),
        moreCount = 0,
        isEmpty = false,
    )

    @Test fun posts_when_enabled() {
        val showing = poster.apply(pinEnabled = true, priority = NotificationPriority.TOP, content = content)
        assertEquals(true, showing)
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }

    // The pin persists through an empty week as a "Nothing scheduled" state; only disabling
    // the pin removes it.
    @Test fun keeps_showing_the_empty_state_when_content_is_empty() {
        poster.apply(pinEnabled = true, priority = NotificationPriority.TOP, content = content)
        val showing = poster.apply(pinEnabled = true, priority = NotificationPriority.TOP, content = emptyContent)
        assertEquals(true, showing)
        val active = shadowOf(mgr).activeNotifications
        assertEquals(1, active.size)
        // The active pin is the empty state, not a stale leftover from the earlier real post.
        val view = active[0].notification.contentView.apply(ctx, FrameLayout(ctx))
        assertEquals(
            "Nothing scheduled this week",
            view.findViewById<TextView>(R.id.collapsed_line).text.toString(),
        )
    }

    @Test fun cancels_when_disabled() {
        poster.apply(pinEnabled = true, priority = NotificationPriority.TOP, content = content)
        val showing = poster.apply(pinEnabled = false, priority = NotificationPriority.TOP, content = content)
        assertEquals(false, showing)
        assertEquals(0, shadowOf(mgr).activeNotifications.size)
    }
}
