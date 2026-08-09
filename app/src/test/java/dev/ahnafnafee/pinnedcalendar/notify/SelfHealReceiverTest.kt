package dev.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.Intent
import dev.ahnafnafee.pinnedcalendar.data.SettingsRepository
import dev.ahnafnafee.pinnedcalendar.data.settingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SelfHealReceiverTest {

    private val ctx = RuntimeEnvironment.getApplication()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)
    // The settings DataStore is a process singleton shared across tests, so each test sets the
    // preferences it depends on rather than relying on defaults or run order.
    private val settings = SettingsRepository(ctx.settingsDataStore)

    // Restore app defaults so a disabled-pin test can't leak into other classes sharing the singleton.
    @After fun restoreDefaults() {
        runBlocking {
            settings.setPinEnabled(true)
            settings.setDoubleSwipeDismiss(false)
            settings.setLastDismissAt(0L)
        }
    }

    @Test fun does_not_post_when_self_healing_an_empty_agenda() = runBlocking {
        settings.setPinEnabled(true)
        settings.setDoubleSwipeDismiss(false)
        settings.setLastDismissAt(0L)

        SelfHealReceiver().onReceive(ctx, Intent())
        assertEquals(0, shadowOf(mgr).activeNotifications.size)
    }

    @Test fun double_swipe_within_window_removes_the_pin() = runBlocking {
        settings.setPinEnabled(true)
        settings.setDoubleSwipeDismiss(true)
        settings.setLastDismissAt(0L)

        val receiver = SelfHealReceiver()
        receiver.onReceive(ctx, Intent()) // first swipe: records the dismissal
        assertEquals(0, shadowOf(mgr).activeNotifications.size)
        receiver.onReceive(ctx, Intent()) // second quick swipe: removes the pin for good
        assertEquals(0, shadowOf(mgr).activeNotifications.size)
        assertFalse(settings.isPinEnabled())
    }

    @Test fun isSecondQuickSwipe_requires_enabled_and_a_recent_first_swipe() {
        assertFalse(SelfHealReceiver.isSecondQuickSwipe(enabled = false, lastDismissAt = 100L, now = 200L))
        assertFalse(SelfHealReceiver.isSecondQuickSwipe(enabled = true, lastDismissAt = 0L, now = 200L))
        assertFalse(
            SelfHealReceiver.isSecondQuickSwipe(
                enabled = true,
                lastDismissAt = 100L,
                now = 100L + SelfHealReceiver.DOUBLE_SWIPE_WINDOW_MS + 1,
            ),
        )
        assertTrue(SelfHealReceiver.isSecondQuickSwipe(enabled = true, lastDismissAt = 100L, now = 1_100L))
    }
}
