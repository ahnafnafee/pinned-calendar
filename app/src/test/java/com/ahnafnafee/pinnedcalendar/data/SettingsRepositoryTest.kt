package com.ahnafnafee.pinnedcalendar.data

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
}
