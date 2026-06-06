package com.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.ahnafnafee.pinnedcalendar.data.NotificationPriority
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent

class NotificationPoster(private val context: Context) {

    private val builder = AgendaNotificationBuilder(context)

    /** Posts when [pinEnabled]; cancels when disabled. Returns true if a notification is now showing. */
    fun apply(pinEnabled: Boolean, priority: NotificationPriority, content: NotificationContent): Boolean {
        val mgr = context.getSystemService<NotificationManager>() ?: return false
        return if (pinEnabled) {
            ChannelManager.ensureChannel(context, priority)
            mgr.notify(ChannelManager.NOTIFICATION_ID, builder.build(content, priority))
            true
        } else {
            mgr.cancel(ChannelManager.NOTIFICATION_ID)
            false
        }
    }
}
