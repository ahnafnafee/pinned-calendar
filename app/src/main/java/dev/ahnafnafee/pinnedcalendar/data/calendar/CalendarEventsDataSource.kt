package dev.ahnafnafee.pinnedcalendar.data.calendar

import android.content.Context
import android.provider.CalendarContract
import dev.ahnafnafee.pinnedcalendar.R
import dev.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import dev.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Reads calendar events already synced on the device via the Calendar Provider.
 * Requires the READ_CALENDAR permission. No network, no account sign-in.
 */
class CalendarEventsDataSource(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    fun eventsInWindow(
        startMillis: Long,
        endMillis: Long,
        excludedCalendarIds: Set<String> = emptySet(),
    ): List<AgendaItem> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
        )
        val out = ArrayList<AgendaItem>()
        context.contentResolver.query(
            uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val colorIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
            val calIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            while (c.moveToNext()) {
                val calId = c.getLong(calIdx).toString()
                if (calId in excludedCalendarIds) continue
                val id = c.getLong(idIdx)
                val untitled = context.getString(R.string.calendar_event_untitled)
                val title = c.getString(titleIdx)?.ifBlank { untitled } ?: untitled
                val begin = c.getLong(beginIdx)
                val allDay = c.getInt(allDayIdx) == 1
                val color = c.getInt(colorIdx)
                val start = if (allDay) {
                    Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                        .atStartOfDay(zone).toInstant()
                } else {
                    Instant.ofEpochMilli(begin)
                }
                out.add(
                    AgendaItem(
                        id = "evt_$id",
                        kind = ItemKind.EVENT,
                        title = title,
                        start = start,
                        allDay = allDay,
                        colorHex = if (color != 0) String.format("#%06X", 0xFFFFFF and color) else null,
                        deepLink = "content://com.android.calendar/events/$id",
                    ),
                )
            }
        }
        return out
    }
}
