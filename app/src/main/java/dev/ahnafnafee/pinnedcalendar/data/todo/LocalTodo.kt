package dev.ahnafnafee.pinnedcalendar.data.todo

data class LocalTodo(
    val id: String,
    val title: String,
    val dueMillis: Long?,        // null = undated (excluded from the week view)
    val completed: Boolean = false,
    val notes: String = "",
    val priority: TodoPriority = TodoPriority.NONE,
)

/**
 * Flag colors follow the red / amber / blue language task apps have made conventional;
 * NONE renders with the neutral task color.
 */
enum class TodoPriority(val value: Int, val label: String, val colorHex: String?) {
    NONE(0, "None", null),
    LOW(1, "Low", "#4285F4"),
    MEDIUM(2, "Medium", "#F9AB00"),
    HIGH(3, "High", "#EA4335"),
    ;

    companion object {
        fun from(value: Int): TodoPriority = entries.firstOrNull { it.value == value } ?: NONE
    }
}
