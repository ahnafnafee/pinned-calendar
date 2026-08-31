package dev.ahnafnafee.pinnedcalendar.domain

import dev.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import dev.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class SampleAgendaLabels(
    val teamStandup: String = "Team standup",
    val oneOnOne: String = "1:1 with Sam",
    val expenseReport: String = "Submit expense report",
    val dentist: String = "Dentist appointment",
    val designReview: String = "Design review",
    val lunch: String = "Lunch with Priya",
    val passport: String = "Renew passport",
    val sprintPlanning: String = "Sprint planning",
)

object SampleAgenda {
    fun items(
        clock: Clock,
        zone: ZoneId = ZoneId.systemDefault(),
        labels: SampleAgendaLabels = SampleAgendaLabels(),
    ): List<AgendaItem> {
        val today = LocalDate.now(clock.withZone(zone))
        fun at(daysAhead: Long, time: LocalTime) =
            today.plusDays(daysAhead).atTime(time).atZone(zone).toInstant()
        return listOf(
            AgendaItem("e1", ItemKind.EVENT, labels.teamStandup, at(0, LocalTime.of(9, 0)), colorHex = "#039BE5"),
            AgendaItem("e2", ItemKind.EVENT, labels.oneOnOne, at(0, LocalTime.of(14, 0)), colorHex = "#D50000"),
            AgendaItem("t1", ItemKind.TASK, labels.expenseReport, at(0, LocalTime.of(17, 0)), colorHex = null),
            AgendaItem("e3", ItemKind.EVENT, labels.dentist, at(1, LocalTime.of(10, 0)), colorHex = "#0B8043"),
            AgendaItem("e4", ItemKind.EVENT, labels.designReview, at(1, LocalTime.of(16, 0)), colorHex = "#8E24AA"),
            AgendaItem("e5", ItemKind.EVENT, labels.lunch, at(2, LocalTime.of(12, 30)), colorHex = "#F4511E"),
            AgendaItem("t2", ItemKind.TASK, labels.passport, at(3, LocalTime.of(9, 0)), colorHex = null),
            AgendaItem("e6", ItemKind.EVENT, labels.sprintPlanning, at(4, LocalTime.of(11, 0)), colorHex = "#3F51B5"),
        )
    }
}
