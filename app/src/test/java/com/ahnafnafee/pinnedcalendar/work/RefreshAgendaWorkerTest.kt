package com.ahnafnafee.pinnedcalendar.work

import android.app.NotificationManager
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RefreshAgendaWorkerTest {

    private val ctx = RuntimeEnvironment.getApplication()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)

    @Test fun worker_posts_notification_and_succeeds() = runTest {
        val worker = TestListenableWorkerBuilder<RefreshAgendaWorker>(ctx).build()
        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }
}
