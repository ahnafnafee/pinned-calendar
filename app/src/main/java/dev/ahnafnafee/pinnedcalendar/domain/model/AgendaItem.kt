package dev.ahnafnafee.pinnedcalendar.domain.model

import java.time.Instant

enum class ItemKind { EVENT, TASK }

data class AgendaItem(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val start: Instant?,            // null = undated task (excluded from the week view)
    val allDay: Boolean = false,
    val colorHex: String? = null,   // calendar color for events; null for tasks
    val completed: Boolean = false,
    val deepLink: String? = null,
)
