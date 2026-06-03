package com.ahnafnafee.pinnedcalendar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahnafnafee.pinnedcalendar.notify.AgendaNotifier

class RefreshAgendaWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        AgendaNotifier(applicationContext).refresh()
        return Result.success()
    }
}
