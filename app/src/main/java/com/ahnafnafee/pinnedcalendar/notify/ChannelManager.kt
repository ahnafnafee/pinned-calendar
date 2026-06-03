package com.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.ahnafnafee.pinnedcalendar.R

object ChannelManager {
    const val CHANNEL_ID = "pinned_agenda"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW, // silent, always in shade
            ).apply {
                description = context.getString(R.string.channel_desc)
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
