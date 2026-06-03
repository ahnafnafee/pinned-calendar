package com.ahnafnafee.pinnedcalendar.work

import android.content.Intent
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    private val ctx = RuntimeEnvironment.getApplication()

    @Before fun setUp() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config)
    }

    @Test fun boot_enqueues_periodic_refresh() {
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))
        val work = WorkManager.getInstance(ctx)
            .getWorkInfosForUniqueWork("agenda_refresh_periodic").get()
        assertEquals(1, work.size)
    }
}
