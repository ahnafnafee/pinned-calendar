package dev.ahnafnafee.pinnedcalendar.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object TodoSchedule {
    /** Moves a due instant to [date], keeping its time of day (9:00 when previously undated). */
    fun at(current: Long?, date: LocalDate, zone: ZoneId): Long {
        val time = current?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
            ?: LocalTime.of(9, 0)
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}
