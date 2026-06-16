package com.ahnafnafee.pinnedcalendar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahnafnafee.pinnedcalendar.notify.AgendaNotifier

/**
 * Runs when the Calendar Provider reports a change (added/edited/deleted event), even after the app
 * process has been killed — the system wakes it via the content-URI trigger registered in
 * [AgendaScheduler.observeCalendarChanges]. Refreshes the pin, then re-arms the trigger because a
 * content-URI job fires only once per registration.
 */
class CalendarObserverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        AgendaNotifier(applicationContext).refresh()
        AgendaScheduler.observeCalendarChanges(applicationContext, replaceExisting = true)
        return Result.success()
    }
}
