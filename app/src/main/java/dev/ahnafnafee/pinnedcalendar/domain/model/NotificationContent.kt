package dev.ahnafnafee.pinnedcalendar.domain.model

data class NotificationRow(
    val time: String,           // "9:00", "All day", or ""
    val title: String,
    val colorHex: String?,      // event calendar color; null => task
    val isTask: Boolean,
    val completed: Boolean,
    val deepLink: String? = null, // e.g. content://com.android.calendar/events/<id>
)

data class DaySection(
    val header: String,         // "TODAY · MON 2" or "" when ungrouped
    val isToday: Boolean,
    val rows: List<NotificationRow>,
)

data class NotificationContent(
    val headerCount: Int,       // total items in window
    val collapsedLine: String,  // next item, e.g. "9:00 Team standup"
    val collapsedColorHex: String?,
    val sections: List<DaySection>,
    val moreCount: Int,         // items beyond the cap
    val isEmpty: Boolean,
)
