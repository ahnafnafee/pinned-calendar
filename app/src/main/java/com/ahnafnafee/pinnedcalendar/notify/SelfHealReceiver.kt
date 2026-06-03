package com.ahnafnafee.pinnedcalendar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

/** Fired by the notification's delete-intent when the user dismisses it; re-posts the pin. */
class SelfHealReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        try {
            runBlocking { AgendaNotifier(context).refresh() }
        } finally {
            pending?.finish()
        }
    }
}
