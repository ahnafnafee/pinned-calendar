package com.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ahnafnafee.pinnedcalendar.R
import com.ahnafnafee.pinnedcalendar.data.NotificationPriority
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent

class AgendaNotificationBuilder(private val context: Context) {

    private val renderer = AgendaRemoteViewsRenderer(context)

    fun build(content: NotificationContent, priority: NotificationPriority): Notification {
        // Tapping the pin's header or icon opens this app; rows keep their own deep links.
        val contentIntent = AppLaunch.pendingIntent(context)
        val deleteIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, SelfHealReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, ChannelManager.channelId(priority))
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
            .setPriority(legacyPriority(priority))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .apply {
                // The shade ranks same-importance notifications newest-first. Stamping Top with a
                // fixed far-future post time keeps the pin at the top of the High bucket instead of
                // sinking as new notifications arrive; setShowWhen(false) and the custom layout keep
                // the timestamp itself hidden. Lower levels keep their real post time so they mix in.
                if (priority == NotificationPriority.TOP) setWhen(PIN_SORT_WHEN)
            }
            .build()
    }

    /** Pre-O priority hint, aligned with each level's channel importance (ignored on O+ in favour of it). */
    private fun legacyPriority(priority: NotificationPriority): Int = when (priority) {
        NotificationPriority.TOP -> NotificationCompat.PRIORITY_MAX
        NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.SILENT -> NotificationCompat.PRIORITY_LOW
    }

    private companion object {
        // Fixed, far in the future (~year 2100) so the pin always sorts as the "newest" High
        // notification; fixed rather than recomputed so re-posts don't reshuffle the order.
        const val PIN_SORT_WHEN = 4_102_444_800_000L
    }
}
