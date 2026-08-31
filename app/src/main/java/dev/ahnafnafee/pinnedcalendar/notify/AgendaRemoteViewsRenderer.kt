package dev.ahnafnafee.pinnedcalendar.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import dev.ahnafnafee.pinnedcalendar.domain.model.NotificationRow

class AgendaRemoteViewsRenderer(private val context: Context) {

    private val taskColor = 0xFF80868B.toInt()
    private val fallbackColor = 0xFFE07F2C.toInt()

    // The notification shade follows the system day/night setting. The app's own theme attrs
    // (?android:textColorPrimary) resolve to the app theme, not the shade, so set text colours here.
    private val isDark =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    private val primaryText = if (isDark) 0xFFECECEC.toInt() else 0xFF1F1F1F.toInt()
    private val secondaryText = if (isDark) 0xFFB0B5BB.toInt() else 0xFF5F6368.toInt()
    private val accent = AccentResolver.accentColor(context)

    fun collapsed(
        content: NotificationContent,
        maxRows: Int,
        showTodayHeader: Boolean,
        rowPaddingDp: Int,
        rowTextSizeSp: Int,
        rowHeightDp: Int,
        timeColumnWidthDp: Int,
        useContentPadding: Boolean,
    ): RemoteViews =
        if (maxRows <= 1) collapsedSingleLine(content, rowTextSizeSp, useContentPadding)
        else collapsedRows(
            content,
            maxRows,
            showTodayHeader,
            rowPaddingDp,
            rowTextSizeSp,
            rowHeightDp,
            timeColumnWidthDp,
            useContentPadding,
        )

    private fun collapsedSingleLine(
        content: NotificationContent,
        rowTextSizeSp: Int,
        useContentPadding: Boolean,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_collapsed)
        if (content.isEmpty) {
            rv.setTextViewText(R.id.collapsed_line, context.getString(R.string.agenda_empty))
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
        rv.setFloat(R.id.collapsed_line, "setTextSize", rowTextSizeSp.coerceIn(11, 18).toFloat())
        applyContentPadding(rv, useContentPadding, compact = true)
        return rv
    }

    private fun collapsedRows(
        content: NotificationContent,
        maxRows: Int,
        showTodayHeader: Boolean,
        rowPaddingDp: Int,
        rowTextSizeSp: Int,
        rowHeightDp: Int,
        timeColumnWidthDp: Int,
        useContentPadding: Boolean,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_collapsed_multi)
        rv.removeAllViews(R.id.collapsed_container)
        if (content.isEmpty) {
            // The row shell keeps the message aligned with agenda rows; the bar and time column
            // are gone (not invisible) so the text is not indented by an empty column.
            val row = RemoteViews(context.packageName, R.layout.notif_row)
            row.setViewVisibility(R.id.row_bar, View.GONE)
            row.setViewVisibility(R.id.row_time, View.GONE)
            row.setTextViewText(R.id.row_title, context.getString(R.string.agenda_empty))
            row.setTextColor(R.id.row_title, primaryText)
            row.setFloat(R.id.row_title, "setTextSize", rowTextSizeSp.coerceIn(11, 18).toFloat())
            rv.addView(R.id.collapsed_container, row)
            rv.setTextViewText(R.id.collapsed_more, "")
            applyContentPadding(rv, useContentPadding, compact = true)
            return rv
        }
        var shownRows = 0
        var clickReq = 100
        var remainingRows = maxRows.coerceIn(1, 6)
        for (section in content.sections) {
            if (remainingRows == 0) break
            val rows = section.rows.take(remainingRows)
            if (rows.isEmpty()) continue

            if (section.header.isNotEmpty() && (!section.isToday || showTodayHeader)) {
                val header = RemoteViews(context.packageName, R.layout.notif_day_header_compact)
                header.setTextViewText(R.id.day_header, section.header)
                header.setTextColor(R.id.day_header, secondaryText)
                rv.addView(R.id.collapsed_container, header)
            }
            rows.forEach { row ->
                rv.addView(
                    R.id.collapsed_container,
                    agendaRow(row, clickReq++, rowPaddingDp, rowTextSizeSp, rowHeightDp, timeColumnWidthDp),
                )
            }
            shownRows += rows.size
            remainingRows -= rows.size
        }
        // Count rows omitted from this RemoteViews. System UI may display a different number
        // of the included rows depending on Android version and device implementation.
        val hiddenCount = (content.headerCount - shownRows).coerceAtLeast(0)
        rv.setTextViewText(R.id.collapsed_more, if (hiddenCount > 0) "+$hiddenCount" else "")
        rv.setTextColor(R.id.collapsed_more, accent)
        applyContentPadding(rv, useContentPadding, compact = true)
        return rv
    }

    fun expanded(
        content: NotificationContent,
        showHeader: Boolean,
        showTodayHeader: Boolean,
        rowPaddingDp: Int,
        rowTextSizeSp: Int,
        rowHeightDp: Int,
        timeColumnWidthDp: Int,
        useContentPadding: Boolean,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_expanded)
        applyContentPadding(rv, useContentPadding, compact = false)
        rv.removeAllViews(R.id.expanded_container)
        rv.setViewVisibility(
            R.id.expanded_title,
            if (showHeader) View.VISIBLE else View.GONE,
        )

        rv.setTextViewText(
            R.id.expanded_title,
            context.getString(R.string.agenda_week_header, content.headerCount),
        )
        rv.setTextColor(R.id.expanded_title, accent)
        var clickReq = 200
        for (section in content.sections) {
            if (section.header.isNotEmpty() && (!section.isToday || showTodayHeader)) {
                val header = RemoteViews(context.packageName, R.layout.notif_day_header)
                header.setTextViewText(R.id.day_header, section.header)
                header.setTextColor(R.id.day_header, secondaryText)
                rv.addView(R.id.expanded_container, header)
            }
            for (r in section.rows) {
                rv.addView(
                    R.id.expanded_container,
                    agendaRow(r, clickReq++, rowPaddingDp, rowTextSizeSp, rowHeightDp, timeColumnWidthDp),
                )
            }
        }
        if (content.moreCount > 0) {
            rv.setViewVisibility(R.id.expanded_more, View.VISIBLE)
            rv.setTextViewText(
                R.id.expanded_more,
                context.resources.getQuantityString(
                    R.plurals.agenda_more_this_week,
                    content.moreCount,
                    content.moreCount,
                ),
            )
            rv.setTextColor(R.id.expanded_more, accent)
            rv.setOnClickPendingIntent(R.id.expanded_more, AppLaunch.pendingIntent(context))
        } else {
            rv.setViewVisibility(R.id.expanded_more, View.GONE)
        }
        return rv
    }

    private fun agendaRow(
        row: NotificationRow,
        requestCode: Int,
        rowPaddingDp: Int,
        rowTextSizeSp: Int,
        rowHeightDp: Int,
        timeColumnWidthDp: Int,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_row)
        // An explicit color wins even for tasks (priority flags); colorless tasks stay neutral.
        val barColor = when {
            row.colorHex != null -> parseColor(row.colorHex)
            row.isTask -> taskColor
            else -> fallbackColor
        }
        rv.setInt(R.id.row_bar, "setBackgroundColor", barColor)
        rv.setTextViewText(R.id.row_time, row.time)
        rv.setTextColor(R.id.row_time, secondaryText)
        rv.setTextViewText(R.id.row_title, row.title)
        rv.setTextColor(R.id.row_title, primaryText)
        rv.setOnClickPendingIntent(R.id.row_root, itemClickIntent(row, requestCode))
        applyRowAppearance(rv, rowPaddingDp, rowTextSizeSp, rowHeightDp, timeColumnWidthDp)
        return rv
    }

    private fun applyRowAppearance(
        row: RemoteViews,
        rowPaddingDp: Int,
        rowTextSizeSp: Int,
        rowHeightDp: Int,
        timeColumnWidthDp: Int,
    ) {
        val padding = dp(rowPaddingDp.coerceIn(0, 12))
        row.setViewPadding(
            R.id.row_root,
            0,
            padding,
            0,
            padding,
        )
        val titleSize = rowTextSizeSp.coerceIn(11, 18).toFloat()
        row.setFloat(R.id.row_title, "setTextSize", titleSize)
        row.setFloat(R.id.row_time, "setTextSize", (titleSize - 1.5f).coerceAtLeast(10f))
        // The setting describes the content area; vertical padding is added around it. The color
        // bar uses match_parent, so it follows the resulting row height instead of imposing 22dp.
        row.setInt(
            R.id.row_root,
            "setMinimumHeight",
            dp(rowHeightDp.coerceIn(12, 32) + rowPaddingDp.coerceIn(0, 12) * 2),
        )
        val timeWidth = timeColumnWidthDp.coerceIn(32, 64)
        row.setInt(R.id.row_time, "setWidth", dp(timeWidth))
    }

    private fun applyContentPadding(row: RemoteViews, useContentPadding: Boolean, compact: Boolean) {
        val top = if (useContentPadding) dp(2) else 0
        val bottom = if (useContentPadding) dp(if (compact) 2 else 6) else 0
        row.setViewPadding(R.id.notification_content, dp(4), top, dp(8), bottom)
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    /** Per-row tap: open the event in the calendar app; open this app for local to-dos. */
    private fun itemClickIntent(row: NotificationRow, requestCode: Int): PendingIntent =
        if (!row.isTask && row.deepLink != null) {
            PendingIntent.getActivity(
                context, requestCode,
                Intent(Intent.ACTION_VIEW, Uri.parse(row.deepLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            AppLaunch.pendingIntent(context)
        }

    private fun parseColor(hex: String?): Int =
        try {
            if (hex != null) Color.parseColor(hex) else fallbackColor
        } catch (_: Exception) {
            fallbackColor
        }
}
