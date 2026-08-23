package dev.ahnafnafee.pinnedcalendar.data.todo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.ahnafnafee.pinnedcalendar.domain.RecurringTodoSchedule
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Local-only to-do list persisted as a JSON array in DataStore. No network, no account. */
class TodoRepository(private val dataStore: DataStore<Preferences>) {

    val todos: Flow<List<LocalTodo>> = dataStore.data.map { decode(it[KEY]) }

    suspend fun snapshot(): List<LocalTodo> = todos.first()

    suspend fun add(
        title: String,
        dueMillis: Long?,
        notes: String = "",
        priority: TodoPriority = TodoPriority.NONE,
        recurrence: TodoRecurrence? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val normalizedRecurrence = normalizeRecurrence(recurrence, dueMillis, zone)
        mutate {
            it + LocalTodo(
                id = nextId(it),
                title = clean,
                dueMillis = dueMillis,
                notes = notes.trim(),
                priority = priority,
                recurrence = normalizedRecurrence,
                recurrenceAnchorMillis = dueMillis.takeIf { normalizedRecurrence != null },
            )
        }
    }

    /**
     * Toggles a one-time to-do. Completing an open recurring to-do instead advances its current
     * occurrence and leaves the series open; missed occurrences are skipped.
     */
    suspend fun toggle(
        id: String,
        completedAtMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) = mutate { list ->
        list.map { todo ->
            if (todo.id != id) return@map todo
            if (!todo.completed && todo.recurrence != null) {
                RecurringTodoSchedule.nextDueAfterCompletion(todo, completedAtMillis, zone)
                    ?.let { next ->
                        todo.copy(
                            dueMillis = next.dueMillis,
                            completed = false,
                            recurrenceOccurrence = next.occurrenceNumber,
                        )
                    }
                    ?: todo.copy(
                        completed = true,
                        recurrence = null,
                        recurrenceAnchorMillis = null,
                        recurrenceOccurrence = 1,
                    )
            } else {
                todo.copy(completed = !todo.completed)
            }
        }
    }

    /** Full edit of one to-do; a blank title leaves the item unchanged. */
    suspend fun update(
        id: String,
        title: String,
        dueMillis: Long?,
        notes: String,
        priority: TodoPriority,
        recurrence: TodoRecurrence? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        mutate { list ->
            list.map { todo ->
                if (todo.id == id) {
                    val normalizedRecurrence = normalizeRecurrence(recurrence, dueMillis, zone)
                    val position = RecurringTodoSchedule.positionAfterEdit(
                        todo,
                        dueMillis,
                        normalizedRecurrence,
                        zone,
                    )
                    todo.copy(
                        title = clean,
                        dueMillis = dueMillis,
                        notes = notes.trim(),
                        priority = priority,
                        completed = if (todo.completed && normalizedRecurrence != null) false else todo.completed,
                        recurrence = normalizedRecurrence,
                        recurrenceAnchorMillis = position?.anchorMillis,
                        recurrenceOccurrence = position?.occurrenceNumber ?: 1,
                    )
                } else {
                    todo
                }
            }
        }
    }

    suspend fun delete(id: String) =
        mutate { list -> list.filterNot { it.id == id } }

    private suspend fun mutate(transform: (List<LocalTodo>) -> List<LocalTodo>) {
        dataStore.edit { prefs -> prefs[KEY] = encode(transform(decode(prefs[KEY]))) }
    }

    private fun nextId(list: List<LocalTodo>): String =
        ((list.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()

    private fun encode(list: List<LocalTodo>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("due", t.dueMillis ?: JSONObject.NULL)
                    .put("done", t.completed)
                    .put("notes", t.notes)
                    .put("prio", t.priority.value)
                    .put("repeat", t.recurrence?.frequency?.persistedValue ?: "none")
                    .put("repeatEvery", t.recurrence?.interval ?: 1)
                    .put("repeatDays", t.recurrence?.let { TodoRecurrence.weekdayMask(it.weekdays) } ?: 0)
                    .put("repeatUntil", t.recurrence?.endDate?.toEpochDay() ?: JSONObject.NULL)
                    .put("repeatCount", t.recurrence?.maxOccurrences ?: JSONObject.NULL)
                    .put("repeatFrom", t.recurrenceAnchorMillis ?: JSONObject.NULL)
                    .put("repeatOccurrence", t.recurrenceOccurrence),
            )
        }
        return arr.toString()
    }

    private fun decode(s: String?): List<LocalTodo> {
        if (s.isNullOrBlank()) return emptyList()
        val arr = JSONArray(s)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val dueMillis = if (o.isNull("due")) null else o.getLong("due")
            val recurrence = dueMillis?.let {
                TodoRecurrence.fromPersisted(
                    frequencyValue = if (o.has("repeat") && !o.isNull("repeat")) {
                        o.getString("repeat")
                    } else {
                        null
                    },
                    interval = o.optInt("repeatEvery", 1),
                    weekdayMask = o.optInt("repeatDays", 0),
                    endDateEpochDay = o.optionalLong("repeatUntil"),
                    maxOccurrences = o.optionalInt("repeatCount"),
                )
            }
            val storedAnchor = if (o.has("repeatFrom") && !o.isNull("repeatFrom")) {
                o.getLong("repeatFrom")
            } else {
                null
            }
            val recurrenceAnchorMillis = dueMillis
                ?.takeIf { recurrence != null }
                ?.let { due -> storedAnchor?.takeIf { it <= due } ?: due }
            LocalTodo(
                id = o.getString("id"),
                title = o.getString("title"),
                dueMillis = dueMillis,
                completed = o.optBoolean("done", false),
                // Optional fields default so entries written by earlier versions decode cleanly.
                notes = o.optString("notes", ""),
                priority = TodoPriority.from(o.optInt("prio", 0)),
                recurrence = recurrence,
                recurrenceAnchorMillis = recurrenceAnchorMillis,
                recurrenceOccurrence = if (recurrence == null) {
                    1
                } else {
                    o.optInt("repeatOccurrence", 1).coerceAtLeast(1)
                },
            )
        }
    }

    private fun normalizeRecurrence(
        recurrence: TodoRecurrence?,
        dueMillis: Long?,
        zone: ZoneId,
    ): TodoRecurrence? {
        val dueDate = dueMillis?.let {
            runCatching { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.getOrNull()
        } ?: return null
        return recurrence?.normalized(dueDate)
    }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private fun JSONObject.optionalInt(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private companion object {
        val KEY = stringPreferencesKey("todos_json")
    }
}
