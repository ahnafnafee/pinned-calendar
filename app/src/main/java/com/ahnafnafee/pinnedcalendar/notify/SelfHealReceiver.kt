package com.ahnafnafee.pinnedcalendar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ahnafnafee.pinnedcalendar.data.SettingsRepository
import com.ahnafnafee.pinnedcalendar.data.settingsDataStore
import kotlinx.coroutines.runBlocking

/**
 * Fired by the notification's delete-intent when the user dismisses the pin.
 *
 * By default the pin self-heals: a dismissal immediately re-posts it. When the user enables
 * "swipe twice to remove", a second dismissal within [DOUBLE_SWIPE_WINDOW_MS] of the first turns
 * the pin off instead — the same state as the in-app switch, so it stays gone until re-enabled.
 */
class SelfHealReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val now = System.currentTimeMillis()
        val pending = goAsync()
        try {
            runBlocking {
                val repo = SettingsRepository(context.applicationContext.settingsDataStore)
                val settings = repo.snapshot()
                if (isSecondQuickSwipe(settings.doubleSwipeDismiss, repo.lastDismissAt(), now)) {
                    repo.setLastDismissAt(0L)
                    repo.setPinEnabled(false) // refresh() now cancels rather than re-posts; pin stays gone
                } else {
                    repo.setLastDismissAt(now)
                }
                AgendaNotifier(context).refresh()
            }
        } finally {
            pending?.finish()
        }
    }

    companion object {
        /** Two dismissals within this gap count as the deliberate "remove it" gesture. */
        const val DOUBLE_SWIPE_WINDOW_MS = 5_000L

        /** Pure decision: is this dismissal the second half of a quick double-swipe? */
        fun isSecondQuickSwipe(enabled: Boolean, lastDismissAt: Long, now: Long): Boolean =
            enabled && lastDismissAt != 0L && (now - lastDismissAt) in 0..DOUBLE_SWIPE_WINDOW_MS
    }
}
