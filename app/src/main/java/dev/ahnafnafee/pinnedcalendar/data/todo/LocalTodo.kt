package dev.ahnafnafee.pinnedcalendar.data.todo

data class LocalTodo(
    val id: String,
    val title: String,
    val dueMillis: Long?,        // null = undated (excluded from the week view)
    val completed: Boolean = false,
    val notes: String = "",
    val priority: TodoPriority = TodoPriority.NONE,
    val recurrence: TodoRecurrence? = null,
    // The first occurrence, retained so Jan 31 monthly and Feb 29 yearly schedules do not drift.
    val recurrenceAnchorMillis: Long? = null,
    val recurrenceOccurrence: Int = 1,
)

/**
 * Flag colors follow the red / amber / blue language task apps have made conventional;
 * NONE renders with the neutral task color.
 */
enum class TodoPriority(val value: Int, val colorHex: String?) {
    NONE(0, null),
    LOW(1, "#4285F4"),
    MEDIUM(2, "#F9AB00"),
    HIGH(3, "#EA4335"),
    ;

    companion object {
        fun from(value: Int): TodoPriority = entries.firstOrNull { it.value == value } ?: NONE
    }
}
