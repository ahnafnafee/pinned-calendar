package com.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import com.ahnafnafee.pinnedcalendar.R
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent

class AgendaNotificationBuilder(private val context: Context) {

    private val renderer = AgendaRemoteViewsRenderer(context)

    fun build(content: NotificationContent): Notification {
        // Tapping the pin opens the Google Calendar app at today's agenda.
        val calendarUri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(Intent.ACTION_VIEW, calendarUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, SelfHealReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, ChannelManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setColor(AccentResolver.accentColor(context))
            .setColorized(false)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(renderer.collapsed(content))
            .setCustomBigContentView(renderer.expanded(content))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .build()
    }
}
