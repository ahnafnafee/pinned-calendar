package dev.ahnafnafee.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import dev.ahnafnafee.pinnedcalendar.data.NotificationPriority
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationContent

class NotificationPoster(private val context: Context) {

    private val builder = AgendaNotificationBuilder(context)

    /** Posts when [pinEnabled] and content exists; otherwise cancels. Returns true when showing. */
    fun apply(
        pinEnabled: Boolean,
        priority: NotificationPriority,
        content: NotificationContent,
        collapsedItems: Int = 1,
        showHeader: Boolean = true,
        showTodayHeader: Boolean = true,
        rowPaddingDp: Int = 5,
        rowTextSizeSp: Int = 14,
        rowHeightDp: Int = 22,
        timeColumnWidthDp: Int = 64,
        useContentPadding: Boolean = true,
    ): Boolean {
        val mgr = context.getSystemService<NotificationManager>() ?: return false
        return if (pinEnabled && !content.isEmpty) {
            ChannelManager.ensureChannel(context, priority)
            mgr.notify(
                ChannelManager.NOTIFICATION_ID,
                builder.build(
                    content, priority, collapsedItems, showHeader, showTodayHeader, rowPaddingDp, rowTextSizeSp, rowHeightDp, timeColumnWidthDp, useContentPadding,
                ),
            )
            true
        } else {
            mgr.cancel(ChannelManager.NOTIFICATION_ID)
            false
        }
    }
}
