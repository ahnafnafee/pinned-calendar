package dev.ahnafnafee.pinnedcalendar.data

enum class WindowMode(val days: Long) {
    THREE_DAYS(3),
    THIS_WEEK(7), // end computed by WindowCalculator, not `days`
    SEVEN_DAYS(7),
    FOURTEEN_DAYS(14),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Font choices (bundled OFL fonts); GOOGLE_SANS = rounded Google Sans, SYSTEM = platform default. */
enum class AppFont { GOOGLE_SANS, SYSTEM, FIGTREE, OUTFIT, INTER }

/** MaterialKolor palette styles (subset). */
enum class AppPalette { TONAL_SPOT, VIBRANT, EXPRESSIVE, NEUTRAL }

/**
 * How the pinned notification ranks in the shade. Each level maps to an Android channel importance
 * in [dev.ahnafnafee.pinnedcalendar.notify.ChannelManager]; all levels stay silent (no sound/vibration).
 * TOP uses IMPORTANCE_HIGH so the pin sits above the everyday notification stream.
 */
enum class NotificationPriority {
    TOP,       // above normal notifications; may briefly peek the first time it posts
    NORMAL,    // mixes with everyday notifications
    SILENT,    // below the shade's "Silent" divider
}

data class AppSettings(
    val pinEnabled: Boolean = true,
    val notificationPriority: NotificationPriority = NotificationPriority.TOP,
    // When on, dismissing the pin twice in quick succession turns it off instead of self-healing.
    val doubleSwipeDismiss: Boolean = false,
    val windowMode: WindowMode = WindowMode.THIS_WEEK,
    val excludedCalendarIds: Set<String> = emptySet(),
    val groupByDay: Boolean = true,
    val hideCompletedTasks: Boolean = true,
    val maxItems: Int = 6,
    // Number of agenda rows rendered in the compact notification before it is expanded.
    val collapsedItems: Int = 1,
    val showNotificationHeader: Boolean = true,
    // Show Today's section label unless explicitly hidden.
    val showTodayHeader: Boolean = true,
    // Vertical padding on each side of an agenda row.
    val notificationRowPaddingDp: Int = 5,
    // Agenda row title size.
    val notificationRowTextSizeSp: Int = 14,
    // Minimum row content height, excluding vertical padding.
    val notificationRowHeightDp: Int = 22,
    // Time column width.
    val notificationTimeColumnWidthDp: Int = 64,
    // Add outer top and bottom padding around notification content.
    val notificationContentPadding: Boolean = true,
    // A normal, dismissible notification when a scheduled to-do comes due.
    val todoReminders: Boolean = true,
    // Event times read as 12-hour (9:00 AM) by default; on = 24-hour (09:00).
    val use24HourClock: Boolean = false,
    // Appearance: Material 3 Expressive + seed-based color theming
    val themeMode: ThemeMode = ThemeMode.DARK, // brand default is dark surfaces (DESIGN.md §2)
    val materialYou: Boolean = false,          // off by default so the brand orange shows (DESIGN.md §2)
    val amoled: Boolean = false,               // pure-black dark theme
    val seedColorArgb: Int = 0xFFE07F2C.toInt(), // brand orange seed when Material You is off (DESIGN.md §2)
    val palette: AppPalette = AppPalette.TONAL_SPOT,
    val font: AppFont = AppFont.GOOGLE_SANS,
)
