package dev.ahnafnafee.pinnedcalendar.data.calendar

import android.content.Context
import android.provider.CalendarContract
import dev.ahnafnafee.pinnedcalendar.R

data class CalendarInfo(val id: String, val name: String, val colorHex: String?)

/** Lists the calendars synced on the device (for the per-calendar on/off toggles). */
class CalendarsRepository(private val context: Context) {
    fun calendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val out = ArrayList<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val colorIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            while (c.moveToNext()) {
                val color = c.getInt(colorIdx)
                out.add(
                    CalendarInfo(
                        id = c.getLong(idIdx).toString(),
                        name = c.getString(nameIdx) ?: context.getString(R.string.calendar_unnamed),
                        colorHex = if (color != 0) String.format("#%06X", 0xFFFFFF and color) else null,
                    ),
                )
            }
        }
        return out
    }
}
