package com.ahnafnafee.pinnedcalendar.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.ahnafnafee.pinnedcalendar.MainActivity
import com.ahnafnafee.pinnedcalendar.R
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationRow

class AgendaRemoteViewsRenderer(private val context: Context) {

    private val taskColor = 0xFF80868B.toInt()
    private val fallbackColor = 0xFF1A73E8.toInt()

    // The notification shade follows the system day/night setting. The app's own theme attrs
    // (?android:textColorPrimary) resolve to the app theme, not the shade, so set text colours here.
    private val isDark =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    private val primaryText = if (isDark) 0xFFECECEC.toInt() else 0xFF1F1F1F.toInt()
    private val secondaryText = if (isDark) 0xFFB0B5BB.toInt() else 0xFF5F6368.toInt()
    private val accent = AccentResolver.accentColor(context)

    fun collapsed(content: NotificationContent): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_collapsed)
        if (content.isEmpty) {
            rv.setTextViewText(R.id.collapsed_line, "Nothing scheduled this week")
            rv.setTextColor(R.id.collapsed_line, primaryText)
            rv.setViewVisibility(R.id.collapsed_dot, View.INVISIBLE)
            rv.setTextViewText(R.id.collapsed_more, "")
        } else {
            rv.setViewVisibility(R.id.collapsed_dot, View.VISIBLE)
            rv.setTextViewText(R.id.collapsed_line, content.collapsedLine)
            rv.setTextColor(R.id.collapsed_line, primaryText)
            rv.setInt(R.id.collapsed_dot, "setBackgroundColor", parseColor(content.collapsedColorHex))
            rv.setTextViewText(
                R.id.collapsed_more,
                if (content.headerCount > 1) "+${content.headerCount - 1}" else "",
            )
            rv.setTextColor(R.id.collapsed_more, accent)
        }
        return rv
    }

    fun expanded(content: NotificationContent): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_expanded)
        rv.removeAllViews(R.id.expanded_container)

        if (content.isEmpty) {
            rv.setTextViewText(R.id.expanded_title, "This week")
            rv.setTextColor(R.id.expanded_title, accent)
            val row = RemoteViews(context.packageName, R.layout.notif_row)
            row.setViewVisibility(R.id.row_bar, View.INVISIBLE)
            row.setTextViewText(R.id.row_time, "")
            row.setTextViewText(R.id.row_title, "Nothing scheduled 🎉")
            row.setTextColor(R.id.row_title, primaryText)
            rv.addView(R.id.expanded_container, row)
            rv.setTextViewText(R.id.expanded_more, "")
            return rv
        }

        rv.setTextViewText(R.id.expanded_title, "This week · ${content.headerCount}")
        rv.setTextColor(R.id.expanded_title, accent)
        var clickReq = 200
        for (section in content.sections) {
            if (section.header.isNotEmpty()) {
                val header = RemoteViews(context.packageName, R.layout.notif_day_header)
                header.setTextViewText(R.id.day_header, section.header)
                header.setTextColor(R.id.day_header, secondaryText)
                rv.addView(R.id.expanded_container, header)
            }
            for (r in section.rows) {
                val row = RemoteViews(context.packageName, R.layout.notif_row)
                val barColor = if (r.isTask) taskColor else parseColor(r.colorHex)
                row.setInt(R.id.row_bar, "setBackgroundColor", barColor)
                row.setTextViewText(R.id.row_time, r.time)
                row.setTextColor(R.id.row_time, secondaryText)
                row.setTextViewText(R.id.row_title, r.title)
                row.setTextColor(R.id.row_title, primaryText)
                row.setOnClickPendingIntent(R.id.row_root, itemClickIntent(r, clickReq++))
                rv.addView(R.id.expanded_container, row)
            }
        }
        rv.setTextViewText(
            R.id.expanded_more,
            if (content.moreCount > 0) "⌄ ${content.moreCount} more this week" else "",
        )
        rv.setTextColor(R.id.expanded_more, accent)
        return rv
    }

    /** Per-row tap: open the event in the calendar app; open this app for local to-dos. */
    private fun itemClickIntent(row: NotificationRow, requestCode: Int): PendingIntent {
        val intent = if (!row.isTask && row.deepLink != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(row.deepLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun parseColor(hex: String?): Int =
        try {
            if (hex != null) Color.parseColor(hex) else fallbackColor
        } catch (_: Exception) {
            fallbackColor
        }
}
