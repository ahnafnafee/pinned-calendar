package dev.ahnafnafee.pinnedcalendar.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private fun newRepo(): SettingsRepository {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File.createTempFile("settings", ".preferences_pb", ctx.cacheDir)
        file.delete()
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
        return SettingsRepository(ds)
    }

    @Test fun defaults_to_enabled() = runTest {
        assertTrue(newRepo().isPinEnabled())
    }

    @Test fun persists_disabled_state() = runTest {
        val repo = newRepo()
        repo.setPinEnabled(false)
        assertEquals(false, repo.isPinEnabled())
    }

    @Test fun double_swipe_defaults_off_and_persists() = runTest {
        val repo = newRepo()
        assertEquals(false, repo.snapshot().doubleSwipeDismiss)
        repo.setDoubleSwipeDismiss(true)
        assertTrue(repo.snapshot().doubleSwipeDismiss)
    }

    @Test fun last_dismiss_at_roundtrips() = runTest {
        val repo = newRepo()
        assertEquals(0L, repo.lastDismissAt())
        repo.setLastDismissAt(123_456L)
        assertEquals(123_456L, repo.lastDismissAt())
    }

    @Test fun clock_defaults_to_twelve_hour_and_persists() = runTest {
        val repo = newRepo()
        assertEquals(false, repo.snapshot().use24HourClock)
        repo.setUse24HourClock(true)
        assertTrue(repo.snapshot().use24HourClock)
    }

    @Test fun notification_layout_settings_default_persist_and_clamp() = runTest {
        val repo = newRepo()
        assertEquals(1, repo.snapshot().collapsedItems)
        assertTrue(repo.snapshot().showNotificationHeader)
        assertTrue(repo.snapshot().showTodayHeader)
        assertEquals(5, repo.snapshot().notificationRowPaddingDp)
        assertEquals(14, repo.snapshot().notificationRowTextSizeSp)
        assertEquals(22, repo.snapshot().notificationRowHeightDp)
        assertEquals(64, repo.snapshot().notificationTimeColumnWidthDp)
        assertTrue(repo.snapshot().notificationContentPadding)

        repo.setCollapsedItems(2)
        repo.setShowTodayHeader(false)
        repo.setNotificationRowPadding(7)
        repo.setNotificationRowTextSize(12)
        repo.setNotificationRowHeight(16)
        repo.setNotificationTimeColumnWidth(48)
        repo.setNotificationContentPadding(false)
        assertEquals(2, repo.snapshot().collapsedItems)
        assertEquals(false, repo.snapshot().showTodayHeader)
        assertEquals(7, repo.snapshot().notificationRowPaddingDp)
        assertEquals(12, repo.snapshot().notificationRowTextSizeSp)
        assertEquals(16, repo.snapshot().notificationRowHeightDp)
        assertEquals(48, repo.snapshot().notificationTimeColumnWidthDp)
        assertEquals(false, repo.snapshot().notificationContentPadding)

        repo.setCollapsedItems(99)
        repo.setShowNotificationHeader(false)
        repo.setNotificationRowPadding(-1)
        repo.setNotificationRowTextSize(99)
        repo.setNotificationRowHeight(-1)
        repo.setNotificationTimeColumnWidth(-1)
        assertEquals(6, repo.snapshot().collapsedItems)
        assertEquals(false, repo.snapshot().showNotificationHeader)
        assertEquals(0, repo.snapshot().notificationRowPaddingDp)
        assertEquals(18, repo.snapshot().notificationRowTextSizeSp)
        assertEquals(12, repo.snapshot().notificationRowHeightDp)
        assertEquals(32, repo.snapshot().notificationTimeColumnWidthDp)
    }
}
