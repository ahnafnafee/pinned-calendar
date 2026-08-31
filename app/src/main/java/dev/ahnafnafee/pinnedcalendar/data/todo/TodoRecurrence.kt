package dev.ahnafnafee.pinnedcalendar.data.todo

import java.time.DayOfWeek
import java.time.LocalDate

enum class RecurrenceFrequency(val persistedValue: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly"),
    ;

    companion object {
        fun fromPersisted(value: String?): RecurrenceFrequency? =
            entries.firstOrNull { it.persistedValue == value }
    }
}

/**
 * A calendar-based recurrence rule. An empty [weekdays] set means "the anchor weekday" for a
 * simple weekly preset. At most one of [endDate] and [maxOccurrences] is retained.
 */
data class TodoRecurrence(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null,
) {
    val isSimplePreset: Boolean
        get() = interval == 1 && weekdays.isEmpty() && endDate == null && maxOccurrences == null

    fun hasSamePatternAs(other: TodoRecurrence): Boolean =
        frequency == other.frequency && interval == other.interval && weekdays == other.weekdays

    fun normalized(anchorDate: LocalDate? = null): TodoRecurrence {
        val safeEndDate = endDate?.let { end ->
            if (anchorDate != null && end.isBefore(anchorDate)) anchorDate else end
        }
        return copy(
            interval = interval.coerceIn(1, MAX_INTERVAL),
            weekdays = if (frequency == RecurrenceFrequency.WEEKLY) weekdays.toSet() else emptySet(),
            endDate = safeEndDate,
            maxOccurrences = if (safeEndDate == null) {
                maxOccurrences?.coerceIn(1, MAX_OCCURRENCES)
            } else {
                null
            },
        )
    }

    /** Compact primitive-only representation used by Compose state restoration. */
    fun toStateValue(): String = listOf(
        frequency.persistedValue,
        interval,
        weekdayMask(weekdays),
        endDate?.toEpochDay() ?: "",
        maxOccurrences ?: "",
    ).joinToString("|")

    companion object {
        const val MAX_INTERVAL = 99
        const val MAX_OCCURRENCES = 999

        fun preset(frequency: RecurrenceFrequency): TodoRecurrence = TodoRecurrence(frequency)

        fun fromStateValue(value: String?): TodoRecurrence? {
            if (value.isNullOrBlank()) return null
            val parts = value.split('|')
            return fromPersisted(
                frequencyValue = parts.getOrNull(0),
                interval = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                weekdayMask = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                endDateEpochDay = parts.getOrNull(3)?.toLongOrNull(),
                maxOccurrences = parts.getOrNull(4)?.toIntOrNull(),
            )
        }

        fun fromPersisted(
            frequencyValue: String?,
            interval: Int = 1,
            weekdayMask: Int = 0,
            endDateEpochDay: Long? = null,
            maxOccurrences: Int? = null,
        ): TodoRecurrence? {
            val frequency = RecurrenceFrequency.fromPersisted(frequencyValue) ?: return null
            val endDate = endDateEpochDay?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
            return TodoRecurrence(
                frequency = frequency,
                interval = interval,
                weekdays = weekdaysFromMask(weekdayMask),
                endDate = endDate,
                maxOccurrences = maxOccurrences,
            ).normalized()
        }

        fun weekdayMask(days: Set<DayOfWeek>): Int =
            days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

        fun weekdaysFromMask(mask: Int): Set<DayOfWeek> =
            DayOfWeek.entries.filterTo(linkedSetOf()) { day -> mask and (1 shl (day.value - 1)) != 0 }
    }
}
