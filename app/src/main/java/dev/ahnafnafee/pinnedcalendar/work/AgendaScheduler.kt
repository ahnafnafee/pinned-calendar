package dev.ahnafnafee.pinnedcalendar.work

import android.content.Context
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AgendaScheduler {
    private const val PERIODIC = "agenda_refresh_periodic"
    private const val ONESHOT = "agenda_refresh_now"
    private const val CALENDAR_TRIGGER = "agenda_calendar_trigger"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshAgendaWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RefreshAgendaWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONESHOT, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Wakes the app to refresh the pin whenever the Calendar Provider changes, surviving process
     * death (JobScheduler holds the trigger). The job fires once and [CalendarObserverWorker]
     * re-arms it, so steady-state keeps exactly one trigger pending.
     *
     * [replaceExisting] is REPLACE when re-arming from the worker (the just-fired job still counts
     * as pending, so KEEP would no-op) and KEEP when arming from app start / boot (don't disturb an
     * already-pending trigger).
     */
    fun observeCalendarChanges(context: Context, replaceExisting: Boolean = false) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
            // A single calendar sync fires many provider writes; batch them into one refresh.
            .setTriggerContentUpdateDelay(5L, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(30L, TimeUnit.SECONDS)
            .build()
        val request = OneTimeWorkRequestBuilder<CalendarObserverWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CALENDAR_TRIGGER,
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
