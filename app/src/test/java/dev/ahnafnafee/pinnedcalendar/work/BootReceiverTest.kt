package dev.ahnafnafee.pinnedcalendar.work

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

    @Test fun app_update_reposts_the_pin() {
        // Updating the package force-stops the app and clears its notifications;
        // MY_PACKAGE_REPLACED must restore the pin without waiting for an app open.
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
        val wm = WorkManager.getInstance(ctx)
        assertEquals(1, wm.getWorkInfosForUniqueWork("agenda_refresh_periodic").get().size)
        assertEquals(1, wm.getWorkInfosForUniqueWork("agenda_refresh_now").get().size)
    }

    @Test fun boot_arms_calendar_change_trigger() {
        // Survives process death: the system holds the content-URI trigger and wakes the app to
        // refresh when the calendar changes, even when nothing else is running.
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))
        val work = WorkManager.getInstance(ctx)
            .getWorkInfosForUniqueWork("agenda_calendar_trigger").get()
        assertEquals(1, work.size)
    }
}
