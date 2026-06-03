package com.ahnafnafee.pinnedcalendar.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import com.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import com.ahnafnafee.pinnedcalendar.data.WindowMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class AgendaRepositoryTest {

    private val zone = ZoneId.of("America/New_York")
    // 2026-06-02 11:00 ET (READ_CALENDAR is not granted in tests, so only to-dos appear).
    private val clock = Clock.fixed(Instant.parse("2026-06-02T15:00:00Z"), zone)

    private fun freshTodoRepo(): TodoRepository {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File.createTempFile("agtd", ".preferences_pb", ctx.cacheDir)
        file.delete()
        return TodoRepository(PreferenceDataStoreFactory.create(produceFile = { file }))
    }

    @Test fun includes_dated_todo_in_window_as_task() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val todos = freshTodoRepo()
        todos.add("Pay rent", Instant.parse("2026-06-02T21:00:00Z").toEpochMilli()) // today 17:00 ET
        val repo = AgendaRepository(ctx, clock, zone, todos)

        val items = repo.agenda(WindowMode.SEVEN_DAYS, emptySet())
        assertEquals(1, items.size)
        assertEquals("Pay rent", items[0].title)
        assertEquals(ItemKind.TASK, items[0].kind)
    }

    @Test fun excludes_undated_todo() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val todos = freshTodoRepo()
        todos.add("Someday", null)
        val repo = AgendaRepository(ctx, clock, zone, todos)

        assertEquals(0, repo.agenda(WindowMode.SEVEN_DAYS, emptySet()).size)
    }
}
