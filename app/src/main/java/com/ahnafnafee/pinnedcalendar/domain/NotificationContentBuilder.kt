package com.ahnafnafee.pinnedcalendar.domain

import com.ahnafnafee.pinnedcalendar.data.DisplaySettings
import com.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import com.ahnafnafee.pinnedcalendar.domain.model.DaySection
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationContent
import com.ahnafnafee.pinnedcalendar.domain.model.NotificationRow

class NotificationContentBuilder(private val bucketer: DayBucketer) {

    fun build(items: List<AgendaItem>, settings: DisplaySettings): NotificationContent {
        val filtered = items
            .filter { it.start != null }
            .filter { !(settings.hideCompletedTasks && it.completed) }

        if (filtered.isEmpty()) {
            return NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)
        }

        val sortedAll = filtered.sortedBy { it.start }
        val next = sortedAll.first()
        val collapsedTime = bucketRowFor(next).time
        val collapsedLine = listOf(collapsedTime, next.title).filter { it.isNotBlank() }.joinToString(" ")

        val sections = if (settings.groupByDay) {
            bucketer.bucket(filtered)
        } else {
            listOf(DaySection("", isToday = false, rows = bucketer.bucket(filtered).flatMap { it.rows }))
        }

        val capped = capSections(sections, settings.maxItems)
        val shown = capped.sumOf { it.rows.size }

        return NotificationContent(
            headerCount = filtered.size,
            collapsedLine = collapsedLine,
            collapsedColorHex = next.colorHex,
            sections = capped,
            moreCount = (filtered.size - shown).coerceAtLeast(0),
            isEmpty = false,
        )
    }

    private fun bucketRowFor(item: AgendaItem): NotificationRow =
        bucketer.bucket(listOf(item)).first().rows.first()

    private fun capSections(sections: List<DaySection>, maxItems: Int): List<DaySection> {
        if (maxItems <= 0) return emptyList()
        val out = ArrayList<DaySection>()
        var remaining = maxItems
        for (section in sections) {
            if (remaining <= 0) break
            val take = section.rows.take(remaining)
            if (take.isNotEmpty()) {
                out.add(section.copy(rows = take))
                remaining -= take.size
            }
        }
        return out
    }
}
