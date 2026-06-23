package dev.ahnafnafee.pinnedcalendar.data.todo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
}
