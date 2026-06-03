package com.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.ahnafnafee.pinnedcalendar.R

object ChannelManager {
    // A channel's importance can't be raised from code once it exists, so lifting the pin out of
    // the shade's "Silent" section requires a fresh channel id. The legacy low-importance channel
    // is retired below.
    const val CHANNEL_ID = "pinned_agenda_v2"
    const val NOTIFICATION_ID = 1001

    private const val LEGACY_CHANNEL_ID = "pinned_agenda"

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService<NotificationManager>() ?: return

        if (mgr.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            mgr.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }

        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            // DEFAULT importance keeps the pin in the shade's main (non-silent) area so it ranks
            // near the top; a null sound and disabled vibration keep it quiet. DEFAULT — unlike
            // HIGH — never triggers a heads-up pop.
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
