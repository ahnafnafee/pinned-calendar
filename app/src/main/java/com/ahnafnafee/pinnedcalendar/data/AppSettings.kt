package com.ahnafnafee.pinnedcalendar.data

enum class WindowMode(val label: String, val days: Long) {
    THREE_DAYS("3 days", 3),
    THIS_WEEK("This week", 7), // end computed by WindowCalculator, not `days`
    SEVEN_DAYS("7 days", 7),
    FOURTEEN_DAYS("14 days", 14),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Font choices (bundled OFL fonts); GOOGLE_SANS = rounded Google Sans, SYSTEM = platform default. */
enum class AppFont(val label: String) {
    GOOGLE_SANS("Google Sans"), SYSTEM("System"), FIGTREE("Figtree"), OUTFIT("Outfit"), INTER("Inter")
}

/** MaterialKolor palette styles (subset). */
enum class AppPalette(val label: String) { TONAL_SPOT("Tonal"), VIBRANT("Vibrant"), EXPRESSIVE("Expressive"), NEUTRAL("Neutral") }

/**
 * How the pinned notification ranks in the shade. Each level maps to an Android channel importance
 * in [com.ahnafnafee.pinnedcalendar.notify.ChannelManager]; all levels stay silent (no sound/vibration).
 * TOP uses IMPORTANCE_HIGH so the pin sits above the everyday notification stream.
 */
enum class NotificationPriority(val label: String) {
    TOP("Top"),       // above normal notifications; may briefly peek the first time it posts
    NORMAL("Normal"), // mixes with everyday notifications
    SILENT("Silent"), // below the shade's "Silent" divider
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
    // Event times read as 12-hour (9:00 AM) by default; on = 24-hour (09:00).
    val use24HourClock: Boolean = false,
    // Appearance: Material 3 Expressive + seed-based color theming
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val materialYou: Boolean = true,           // wallpaper dynamic color (API 31+)
    val amoled: Boolean = false,               // pure-black dark theme
    val seedColorArgb: Int = 0xFF1A73E8.toInt(), // seed when Material You is off
    val palette: AppPalette = AppPalette.TONAL_SPOT,
    val font: AppFont = AppFont.GOOGLE_SANS,
)
