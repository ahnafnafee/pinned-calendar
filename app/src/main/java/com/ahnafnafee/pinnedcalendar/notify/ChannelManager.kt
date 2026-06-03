package com.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.ahnafnafee.pinnedcalendar.R
import com.ahnafnafee.pinnedcalendar.data.NotificationPriority

object ChannelManager {
    const val NOTIFICATION_ID = 1001

    // A channel's importance can't be raised from code once it exists, so each priority level owns
    // its own channel id with a fixed importance. Switching levels posts on a different channel and
    // removes the others, leaving a single "Pinned agenda" entry in system settings at a time.
    private val CHANNELS = mapOf(
        NotificationPriority.TOP to "pinned_agenda_high",
        NotificationPriority.NORMAL to "pinned_agenda_default",
        NotificationPriority.SILENT to "pinned_agenda_low",
    )

    // Channel ids shipped by earlier versions, superseded by the per-level channels above.
    private val LEGACY_CHANNEL_IDS = listOf("pinned_agenda", "pinned_agenda_v2")

    fun channelId(priority: NotificationPriority): String = CHANNELS.getValue(priority)

    private fun importance(priority: NotificationPriority): Int = when (priority) {
        NotificationPriority.TOP -> NotificationManager.IMPORTANCE_HIGH
        NotificationPriority.NORMAL -> NotificationManager.IMPORTANCE_DEFAULT
        NotificationPriority.SILENT -> NotificationManager.IMPORTANCE_LOW
    }

    /** Ensures the channel for [priority] exists and retires every other channel we own. */
    fun ensureChannel(context: Context, priority: NotificationPriority) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        val activeId = channelId(priority)

        // Remove legacy channels and the channels for the non-selected levels so only one shows.
        (LEGACY_CHANNEL_IDS + (CHANNELS.values - activeId)).forEach { id ->
            if (mgr.getNotificationChannel(id) != null) mgr.deleteNotificationChannel(id)
        }

        if (mgr.getNotificationChannel(activeId) == null) {
            // All levels stay silent (null sound, no vibration). TOP uses IMPORTANCE_HIGH so the pin
            // ranks above everyday notifications; HIGH — unlike a normal alerting notification — may
            // show a brief heads-up the first time it posts, which setOnlyAlertOnce limits to once.
            val channel = NotificationChannel(
                activeId,
                context.getString(R.string.channel_name),
                importance(priority),
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
