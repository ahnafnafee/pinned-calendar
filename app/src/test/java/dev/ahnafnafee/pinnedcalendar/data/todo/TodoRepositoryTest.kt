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

@RunWith(RobolectricTestRunner::class)
class TodoRepositoryTest {

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

    @Test fun undated_todo_persists_null_due() = runTest {
        val r = newRepo()
        r.add("Someday", null)
        assertNull(r.snapshot()[0].dueMillis)
    }

    @Test fun update_edits_all_fields_and_roundtrips() = runTest {
        val r = newRepo()
        r.add("Draft", 100L)
        val id = r.snapshot()[0].id

        r.update(id, "Final title", 200L, "Bring the charger", TodoPriority.HIGH)
        val t = r.snapshot()[0]
        assertEquals("Final title", t.title)
        assertEquals(200L, t.dueMillis)
        assertEquals("Bring the charger", t.notes)
        assertEquals(TodoPriority.HIGH, t.priority)

        // A blank title is rejected; the existing values stay.
        r.update(id, "   ", null, "", TodoPriority.NONE)
        assertEquals("Final title", r.snapshot()[0].title)
        assertEquals(TodoPriority.HIGH, r.snapshot()[0].priority)
    }

    @Test fun new_todos_default_to_no_priority_and_empty_notes() = runTest {
        val r = newRepo()
        r.add("Plain", 1L)
        val t = r.snapshot()[0]
        assertEquals(TodoPriority.NONE, t.priority)
        assertEquals("", t.notes)
    }

    @Test fun decodes_entries_written_before_notes_and_priority_existed() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File.createTempFile("todos_legacy", ".preferences_pb", ctx.cacheDir)
        file.delete()
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        // The exact shape the previous schema persisted: no "notes", no "prio".
        store.edit { prefs ->
            prefs[stringPreferencesKey("todos_json")] =
                """[{"id":"1","title":"Old task","due":123,"done":true}]"""
        }

        val t = TodoRepository(store).snapshot()[0]
        assertEquals("Old task", t.title)
        assertEquals(123L, t.dueMillis)
        assertTrue(t.completed)
        assertEquals("", t.notes)
        assertEquals(TodoPriority.NONE, t.priority)
    }
}
