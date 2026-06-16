package com.ahnafnafee.pinnedcalendar.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restores the pin after system events that kill the process and clear the
 * notification: device reboots and package updates. Both downstream calls are
 * idempotent, so racing the App.onCreate refresh is harmless.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                AgendaScheduler.schedulePeriodic(context)
                AgendaScheduler.refreshNow(context)
                AgendaScheduler.observeCalendarChanges(context)
            }
        }
    }
}
