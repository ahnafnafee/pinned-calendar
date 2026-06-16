package com.ahnafnafee.pinnedcalendar

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.ahnafnafee.pinnedcalendar.work.AgendaScheduler

class App : Application() {

    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Calendar data changed — refresh the pin now (while the process is alive);
            // the periodic worker covers changes that happen when it isn't.
            AgendaScheduler.refreshNow(this@App)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // The notification channel is created by the post path (NotificationPoster) using the
        // user's chosen priority; refreshNow() below triggers that immediately.
        AgendaScheduler.schedulePeriodic(this)
        AgendaScheduler.refreshNow(this)
        // Durable: refreshes after the process is killed, when the calendar changes (see worker).
        AgendaScheduler.observeCalendarChanges(this)
        // Immediate: refreshes within this process while it's alive, with no trigger delay.
        runCatching {
            contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, calendarObserver)
        }
    }
}
