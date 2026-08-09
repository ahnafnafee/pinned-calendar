package dev.ahnafnafee.pinnedcalendar.notify

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.data.NotificationPriority
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationContent

class AgendaNotificationBuilder(private val context: Context) {

    private val renderer = AgendaRemoteViewsRenderer(context)

    fun build(
        content: NotificationContent,
        priority: NotificationPriority,
        collapsedItems: Int = 1,
        showHeader: Boolean = true,
        showTodayHeader: Boolean = true,
        rowPaddingDp: Int = 5,
        rowTextSizeSp: Int = 14,
        rowHeightDp: Int = 22,
        timeColumnWidthDp: Int = 64,
        useContentPadding: Boolean = true,
    ): Notification {
        require(!content.isEmpty) { "Empty notification content must be cancelled, not rendered" }
        // Tapping the pin's header or icon opens this app; rows keep their own deep links.
        val contentIntent = AppLaunch.pendingIntent(context)
        val deleteIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, SelfHealReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val compactCount = collapsedItems.coerceIn(1, 6)
        val visibleRows = content.sections.sumOf { it.rows.size }
        val needsExpandedView = content.moreCount > 0 || visibleRows > compactCount

        val builder = NotificationCompat.Builder(context, ChannelManager.channelId(priority))
            .setSmallIcon(R.drawable.ic_calendar)
            .setColor(AccentResolver.accentColor(context))
            .setColorized(false)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(
                renderer.collapsed(content, compactCount, showTodayHeader, rowPaddingDp, rowTextSizeSp, rowHeightDp, timeColumnWidthDp, useContentPadding),
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            // Never heads-up: every fresh post (first pin, self-heal, app update) must slide into
            // the shade quietly, even on the high-importance Top channel. Importance-based ranking
            // is unaffected — only the peek/sound/vibration alert path is suppressed.
            .setSilent(true)
            // The shade breaks importance ties by ranking time, and the OS refreshes that only
            // from an app-provided, non-future 'when' — future values are ignored and updates
            // inherit the previous time, so a frozen or future stamp makes the pin sink as newer
            // notifications arrive. A fresh stamp on every build re-asserts the pin as newest in
            // its tier on each refresh; setShowWhen(false) keeps the timestamp itself hidden.
            .setWhen(System.currentTimeMillis())
            .setShowWhen(false)
            .setPriority(legacyPriority(priority))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)

        // Do not attach a second layout when the compact view already contains every row. This
        // removes the otherwise redundant expand affordance on System UI implementations.
        if (needsExpandedView) {
            builder.setCustomBigContentView(
                renderer.expanded(content, showHeader, showTodayHeader, rowPaddingDp, rowTextSizeSp, rowHeightDp, timeColumnWidthDp, useContentPadding),
            )
        }
        return builder.build()
    }

    /** Pre-O priority hint, aligned with each level's channel importance (ignored on O+ in favour of it). */
    private fun legacyPriority(priority: NotificationPriority): Int = when (priority) {
        NotificationPriority.TOP -> NotificationCompat.PRIORITY_MAX
        NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.SILENT -> NotificationCompat.PRIORITY_LOW
    }
}
