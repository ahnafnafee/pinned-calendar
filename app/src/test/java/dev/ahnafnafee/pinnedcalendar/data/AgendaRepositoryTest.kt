package dev.ahnafnafee.pinnedcalendar.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoPriority
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import dev.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import dev.ahnafnafee.pinnedcalendar.data.WindowMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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

    @Test fun overdue_open_todo_carries_forward_to_today() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val todos = freshTodoRepo()
        todos.add("Email prof", Instant.parse("2026-05-30T18:00:00Z").toEpochMilli()) // 3 days before "today"
        val repo = AgendaRepository(ctx, clock, zone, todos)

        val items = repo.agenda(WindowMode.SEVEN_DAYS, emptySet())
        assertEquals(1, items.size)
        assertEquals("Email prof", items[0].title)
        assertEquals(LocalDate.of(2026, 6, 2), items[0].start!!.atZone(zone).toLocalDate()) // re-anchored to today
    }

    @Test fun overdue_completed_todo_is_dropped() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val todos = freshTodoRepo()
        todos.add("Old done task", Instant.parse("2026-05-30T18:00:00Z").toEpochMilli())
        todos.toggle(todos.snapshot()[0].id)
        val repo = AgendaRepository(ctx, clock, zone, todos)

        assertEquals(0, repo.agenda(WindowMode.SEVEN_DAYS, emptySet()).size)
    }

    @Test fun prioritized_todo_carries_its_flag_color_into_the_agenda() = runTest {
        val ctx = RuntimeEnvironment.getApplication()
        val todos = freshTodoRepo()
        val due = Instant.parse("2026-06-02T21:00:00Z").toEpochMilli()
        todos.add("Ship release", due)
        todos.add("Water plants", due)
        val high = todos.snapshot().first { it.title == "Ship release" }
        todos.update(high.id, high.title, high.dueMillis, "", TodoPriority.HIGH)
        val repo = AgendaRepository(ctx, clock, zone, todos)

        val items = repo.agenda(WindowMode.SEVEN_DAYS, emptySet())
        assertEquals(TodoPriority.HIGH.colorHex, items.first { it.title == "Ship release" }.colorHex)
        assertEquals(null, items.first { it.title == "Water plants" }.colorHex)
    }
}
