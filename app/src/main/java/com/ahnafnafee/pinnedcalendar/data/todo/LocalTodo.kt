package com.ahnafnafee.pinnedcalendar.data.todo

data class LocalTodo(
    val id: String,
    val title: String,
    val dueMillis: Long?,        // null = undated (excluded from the week view)
    val completed: Boolean = false,
)
