package dev.ahnafnafee.pinnedcalendar.data.todo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class TodoRepositoryTest {

    private fun preset(frequency: RecurrenceFrequency) = TodoRecurrence.preset(frequency)

    private fun newRepo(): TodoRepository {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File.createTempFile("todos", ".preferences_pb", ctx.cacheDir)
        file.delete()
        return TodoRepository(PreferenceDataStoreFactory.create(produceFile = { file }))
    }

    @Test fun add_then_snapshot_roundtrips() = runTest {
        val r = newRepo()
        r.add("Buy milk", 123L)
        val list = r.snapshot()
        assertEquals(1, list.size)
        assertEquals("Buy milk", list[0].title)
        assertEquals(123L, list[0].dueMillis)
        assertFalse(list[0].completed)
    }

    @Test fun blank_titles_are_ignored() = runTest {
        val r = newRepo()
        r.add("   ", 1L)
        assertEquals(0, r.snapshot().size)
    }

    @Test fun toggle_then_delete() = runTest {
        val r = newRepo()
        r.add("Task", null)
        val id = r.snapshot()[0].id
        r.toggle(id)
        assertTrue(r.snapshot()[0].completed)
        r.delete(id)
        assertEquals(0, r.snapshot().size)
    }

    @Test fun reminder_completion_only_changes_the_matching_open_occurrence() = runTest {
        val r = newRepo()
        val due = 123L
        r.add("Task", due)
        val id = r.snapshot().single().id

        r.completeOccurrence(id, expectedDueMillis = due + 1)
        assertFalse(r.snapshot().single().completed)

        r.completeOccurrence(id, expectedDueMillis = due)
        assertTrue(r.snapshot().single().completed)

        // A duplicated broadcast cannot toggle the completed item open again.
        r.completeOccurrence(id, expectedDueMillis = due)
        assertTrue(r.snapshot().single().completed)
    }

    @Test fun undated_todo_persists_null_due() = runTest {
        val r = newRepo()
        r.add("Someday", null)
        assertNull(r.snapshot()[0].dueMillis)
    }

    @Test fun update_edits_all_fields_and_roundtrips() = runTest {
        val r = newRepo()
        r.add("Draft", 100L)
        r.add("Bystander", 555L)
        val id = r.snapshot().first { it.title == "Draft" }.id

        r.update(id, "Final title", 200L, "Bring the charger", TodoPriority.HIGH)
        val t = r.snapshot().first { it.id == id }
        assertEquals("Final title", t.title)
        assertEquals(200L, t.dueMillis)
        assertEquals("Bring the charger", t.notes)
        assertEquals(TodoPriority.HIGH, t.priority)

        // The other item is untouched in every field.
        val other = r.snapshot().first { it.id != id }
        assertEquals("Bystander", other.title)
        assertEquals(555L, other.dueMillis)
        assertEquals("", other.notes)
        assertEquals(TodoPriority.NONE, other.priority)

        // A blank title is rejected; the existing values stay.
        r.update(id, "   ", null, "", TodoPriority.NONE)
        assertEquals("Final title", r.snapshot().first { it.id == id }.title)
        assertEquals(TodoPriority.HIGH, r.snapshot().first { it.id == id }.priority)
    }

    @Test fun new_todos_default_to_no_priority_and_empty_notes() = runTest {
        val r = newRepo()
        r.add("Plain", 1L)
        val t = r.snapshot()[0]
        assertEquals(TodoPriority.NONE, t.priority)
        assertEquals("", t.notes)
    }

    @Test fun rich_add_persists_notes_and_priority_and_trims_notes() = runTest {
        val r = newRepo()
        r.add("Prep demo", 5L, notes = "  Slides in drive  ", priority = TodoPriority.MEDIUM)
        val t = r.snapshot()[0]
        assertEquals("Prep demo", t.title)
        assertEquals("Slides in drive", t.notes)
        assertEquals(TodoPriority.MEDIUM, t.priority)
    }

    @Test fun recurring_add_roundtrips_custom_rule_and_series_position() = runTest {
        val zone = ZoneId.of("America/New_York")
        val due = LocalDate.of(2026, 6, 2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val rule = TodoRecurrence(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            endDate = LocalDate.of(2026, 12, 31),
        )
        val r = newRepo()
        r.add("Water plants", due, recurrence = rule, zone = zone)

        val t = r.snapshot().single()
        assertEquals(rule, t.recurrence)
        assertEquals(due, t.recurrenceAnchorMillis)
        assertEquals(1, t.recurrenceOccurrence)
    }

    @Test fun completing_recurring_todo_advances_it_without_closing_the_series() = runTest {
        val zone = ZoneId.of("America/New_York")
        val due = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val completedAt = LocalDate.of(2026, 6, 3).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        r.add("Water plants", due, recurrence = preset(RecurrenceFrequency.DAILY), zone = zone)
        val original = r.snapshot().single()

        r.toggle(original.id, completedAt, zone)

        val advanced = r.snapshot().single()
        assertEquals(original.id, advanced.id)
        assertFalse(advanced.completed)
        assertEquals(
            LocalDate.of(2026, 6, 4).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            advanced.dueMillis,
        )
        assertEquals(due, advanced.recurrenceAnchorMillis)
        assertEquals(4, advanced.recurrenceOccurrence)
    }

    @Test fun repeated_reminder_completion_does_not_skip_a_recurring_occurrence() = runTest {
        val zone = ZoneId.of("America/New_York")
        val due = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val completedAt = LocalDate.of(2026, 6, 1).atTime(9, 1).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        r.add("Water plants", due, recurrence = preset(RecurrenceFrequency.DAILY), zone = zone)
        val id = r.snapshot().single().id

        r.completeOccurrence(id, due, completedAt, zone)
        val advancedOnce = r.snapshot().single()
        r.completeOccurrence(id, due, completedAt, zone)

        assertEquals(advancedOnce, r.snapshot().single())
        assertEquals(2, advancedOnce.recurrenceOccurrence)
    }

    @Test fun editing_details_preserves_a_recurring_series_anchor() = runTest {
        val zone = ZoneId.of("America/New_York")
        val january = LocalDate.of(2027, 1, 31).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val february = LocalDate.of(2027, 2, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        val recurrence = preset(RecurrenceFrequency.MONTHLY)
        r.add("Invoice", january, recurrence = recurrence, zone = zone)
        val id = r.snapshot().single().id
        r.toggle(id, january, zone)
        assertEquals(february, r.snapshot().single().dueMillis)

        r.update(id, "Send invoice", february, "Client A", TodoPriority.HIGH, recurrence, zone)

        assertEquals(january, r.snapshot().single().recurrenceAnchorMillis)
    }

    @Test fun editing_only_the_time_keeps_the_monthly_anchor_day() = runTest {
        val zone = ZoneId.of("America/New_York")
        val january = LocalDate.of(2027, 1, 31).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val february = LocalDate.of(2027, 2, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val februaryAtTwo = LocalDate.of(2027, 2, 28).atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val marchAtTwo = LocalDate.of(2027, 3, 31).atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        r.add("Invoice", january, recurrence = preset(RecurrenceFrequency.MONTHLY), zone = zone)
        val id = r.snapshot().single().id
        r.toggle(id, january, zone)
        assertEquals(february, r.snapshot().single().dueMillis)

        val current = r.snapshot().single()
        r.update(
            id,
            current.title,
            februaryAtTwo,
            current.notes,
            current.priority,
            current.recurrence,
            zone,
        )
        r.toggle(id, februaryAtTwo, zone)

        assertEquals(marchAtTwo, r.snapshot().single().dueMillis)
    }

    @Test fun recurrence_requires_a_due_date() = runTest {
        val r = newRepo()
        r.add("Someday", null, recurrence = preset(RecurrenceFrequency.DAILY))

        val t = r.snapshot().single()
        assertNull(t.recurrence)
        assertNull(t.recurrenceAnchorMillis)
    }

    @Test fun reaching_an_occurrence_limit_completes_and_closes_the_series() = runTest {
        val zone = ZoneId.of("America/New_York")
        val june1 = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val june2 = LocalDate.of(2026, 6, 2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        r.add(
            "Two doses",
            june1,
            recurrence = TodoRecurrence(RecurrenceFrequency.DAILY, maxOccurrences = 2),
            zone = zone,
        )
        val id = r.snapshot().single().id

        r.toggle(id, june1, zone)
        assertEquals(june2, r.snapshot().single().dueMillis)
        assertEquals(2, r.snapshot().single().recurrenceOccurrence)
        r.toggle(id, june2, zone)

        val finished = r.snapshot().single()
        assertTrue(finished.completed)
        assertNull(finished.recurrence)
        assertNull(finished.recurrenceAnchorMillis)
        assertEquals(1, finished.recurrenceOccurrence)
    }

    @Test fun changing_the_rule_starts_a_new_series_at_the_current_due_date() = runTest {
        val zone = ZoneId.of("America/New_York")
        val january = LocalDate.of(2027, 1, 31).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val february = LocalDate.of(2027, 2, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        val monthly = preset(RecurrenceFrequency.MONTHLY)
        r.add("Invoice", january, recurrence = monthly, zone = zone)
        val id = r.snapshot().single().id
        r.toggle(id, january, zone)

        val everyTwoMonths = TodoRecurrence(RecurrenceFrequency.MONTHLY, interval = 2)
        r.update(id, "Invoice", february, "", TodoPriority.NONE, everyTwoMonths, zone)

        val restarted = r.snapshot().single()
        assertEquals(february, restarted.recurrenceAnchorMillis)
        assertEquals(1, restarted.recurrenceOccurrence)
        assertEquals(everyTwoMonths, restarted.recurrence)
    }

    @Test fun changing_only_the_series_end_preserves_the_existing_position() = runTest {
        val zone = ZoneId.of("America/New_York")
        val january = LocalDate.of(2027, 1, 31).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val february = LocalDate.of(2027, 2, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val r = newRepo()
        val monthly = preset(RecurrenceFrequency.MONTHLY)
        r.add("Invoice", january, recurrence = monthly, zone = zone)
        val id = r.snapshot().single().id
        r.toggle(id, january, zone)

        val endingAfterThree = monthly.copy(maxOccurrences = 3)
        r.update(id, "Invoice", february, "", TodoPriority.NONE, endingAfterThree, zone)

        val updated = r.snapshot().single()
        assertEquals(january, updated.recurrenceAnchorMillis)
        assertEquals(2, updated.recurrenceOccurrence)
    }

    @Test fun adding_recurrence_to_a_completed_todo_reopens_it() = runTest {
        val r = newRepo()
        r.add("Review budget", 123L)
        val id = r.snapshot().single().id
        r.toggle(id)
        assertTrue(r.snapshot().single().completed)

        r.update(
            id,
            "Review budget",
            123L,
            "",
            TodoPriority.NONE,
            preset(RecurrenceFrequency.MONTHLY),
        )

        assertFalse(r.snapshot().single().completed)
    }

    @Test fun decodes_entries_written_before_notes_and_priority_existed() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File.createTempFile("todos_legacy", ".preferences_pb", ctx.cacheDir)
        file.delete()
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        // The exact shape the previous schema persisted: no "notes", no "prio".
        store.edit { prefs ->
            prefs[stringPreferencesKey("todos_json")] =
                """[{"id":"1","title":"Old task","due":123,"done":true},""" +
                """{"id":"2","title":"From the future","due":456,"done":false,"prio":99}]"""
        }

        val list = TodoRepository(store).snapshot()
        val t = list[0]
        assertEquals("Old task", t.title)
        assertEquals(123L, t.dueMillis)
        assertTrue(t.completed)
        assertEquals("", t.notes)
        assertEquals(TodoPriority.NONE, t.priority)
        assertNull(t.recurrence)
        assertNull(t.recurrenceAnchorMillis)
        // An unknown priority value (newer schema, hand-edited store) degrades to NONE.
        assertEquals(TodoPriority.NONE, list[1].priority)
    }

    @Test fun recurrence_without_a_stored_anchor_migrates_from_its_due_time() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File.createTempFile("todos_repeat_legacy", ".preferences_pb", ctx.cacheDir)
        file.delete()
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        store.edit { prefs ->
            prefs[stringPreferencesKey("todos_json")] =
                """[{"id":"1","title":"Rent","due":123,"done":false,"repeat":"monthly"},""" +
                """{"id":"2","title":"Unknown","due":456,"done":false,"repeat":"fortnightly"}]"""
        }

        val list = TodoRepository(store).snapshot()
        assertEquals(preset(RecurrenceFrequency.MONTHLY), list[0].recurrence)
        assertEquals(123L, list[0].recurrenceAnchorMillis)
        assertNull(list[1].recurrence)
        assertNull(list[1].recurrenceAnchorMillis)
    }
}
