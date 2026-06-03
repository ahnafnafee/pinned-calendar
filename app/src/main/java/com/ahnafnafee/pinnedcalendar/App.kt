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
        runCatching {
            contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, calendarObserver)
        }
    }
}
