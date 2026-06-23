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
}
