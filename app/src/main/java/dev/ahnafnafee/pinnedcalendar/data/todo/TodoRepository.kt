package dev.ahnafnafee.pinnedcalendar.data.todo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        mutate {
            it + LocalTodo(
                id = nextId(it),
                title = clean,
                dueMillis = dueMillis,
                notes = notes.trim(),
                priority = priority,
            )
        }
    }

    suspend fun toggle(id: String) =
        mutate { list -> list.map { if (it.id == id) it.copy(completed = !it.completed) else it } }

    /** Full edit of one to-do; a blank title leaves the item unchanged. */
    suspend fun update(id: String, title: String, dueMillis: Long?, notes: String, priority: TodoPriority) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        mutate { list ->
            list.map {
                if (it.id == id) {
                    it.copy(title = clean, dueMillis = dueMillis, notes = notes.trim(), priority = priority)
                } else {
                    it
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
                    .put("prio", t.priority.value),
            )
        }
        return arr.toString()
    }

    private fun decode(s: String?): List<LocalTodo> {
        if (s.isNullOrBlank()) return emptyList()
        val arr = JSONArray(s)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LocalTodo(
                id = o.getString("id"),
                title = o.getString("title"),
                dueMillis = if (o.isNull("due")) null else o.getLong("due"),
                completed = o.optBoolean("done", false),
                // Optional fields default so entries written by earlier versions decode cleanly.
                notes = o.optString("notes", ""),
                priority = TodoPriority.from(o.optInt("prio", 0)),
            )
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("todos_json")
    }
}
