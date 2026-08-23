package dev.ahnafnafee.pinnedcalendar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.ahnafnafee.pinnedcalendar.data.AgendaRepository
import dev.ahnafnafee.pinnedcalendar.data.AppFont
import dev.ahnafnafee.pinnedcalendar.data.AppPalette
import dev.ahnafnafee.pinnedcalendar.data.AppSettings
import dev.ahnafnafee.pinnedcalendar.data.DisplaySettings
import dev.ahnafnafee.pinnedcalendar.data.NotificationPriority
import dev.ahnafnafee.pinnedcalendar.data.SettingsRepository
import dev.ahnafnafee.pinnedcalendar.data.ThemeMode
import dev.ahnafnafee.pinnedcalendar.data.WindowMode
import dev.ahnafnafee.pinnedcalendar.data.calendar.CalendarInfo
import dev.ahnafnafee.pinnedcalendar.data.calendar.CalendarsRepository
import dev.ahnafnafee.pinnedcalendar.data.settingsDataStore
import dev.ahnafnafee.pinnedcalendar.data.todo.LocalTodo
import dev.ahnafnafee.pinnedcalendar.data.todo.RecurrenceFrequency
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoPriority
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRecurrence
import dev.ahnafnafee.pinnedcalendar.data.todo.TodoRepository
import dev.ahnafnafee.pinnedcalendar.domain.DayBucketer
import dev.ahnafnafee.pinnedcalendar.domain.NotificationContentBuilder
import dev.ahnafnafee.pinnedcalendar.domain.SampleAgenda
import dev.ahnafnafee.pinnedcalendar.domain.TodoGroups
import dev.ahnafnafee.pinnedcalendar.domain.TodoSchedule
import dev.ahnafnafee.pinnedcalendar.domain.model.AgendaItem
import dev.ahnafnafee.pinnedcalendar.domain.model.ItemKind
import dev.ahnafnafee.pinnedcalendar.notify.AgendaRemoteViewsRenderer
import dev.ahnafnafee.pinnedcalendar.notify.NotificationSettingsIntent
import dev.ahnafnafee.pinnedcalendar.system.BatteryOptimization
import dev.ahnafnafee.pinnedcalendar.ui.theme.AppShape
import dev.ahnafnafee.pinnedcalendar.ui.theme.CircleToCookieMorph
import dev.ahnafnafee.pinnedcalendar.ui.theme.MorphableShape
import dev.ahnafnafee.pinnedcalendar.ui.theme.PinnedCalendarTheme
import dev.ahnafnafee.pinnedcalendar.work.AgendaScheduler
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PRIVACY_POLICY_URL = "https://pinnedcalendar.ahnafnafee.dev/privacy/"

class MainActivity : ComponentActivity() {

    private val reloadTick = mutableIntStateOf(0)

    // Reloads the in-app agenda live when the calendar changes while the screen is visible
    // (e.g. a sync lands, or an event is added from a split-screen calendar).
    private val calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            reloadTick.intValue++
        }
    }

    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            reloadTick.intValue++
            AgendaScheduler.refreshNow(this)
        }

    override fun onStart() {
        super.onStart()
        runCatching {
            contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, calendarObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { contentResolver.unregisterContentObserver(calendarObserver) }
    }

    override fun onResume() {
        super.onResume()
        reloadTick.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val needed = buildList {
            add(Manifest.permission.READ_CALENDAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPerms.launch(needed.toTypedArray())

        val settings = SettingsRepository(applicationContext.settingsDataStore)
        val todos = TodoRepository(applicationContext.settingsDataStore)
        val activity = this

        setContent {
            val s by settings.settings.collectAsState(initial = AppSettings())

            PinnedCalendarTheme(settings = s) {
                val scope = rememberCoroutineScope()
                val todoList by todos.todos.collectAsState(initial = emptyList())
                var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
                var ignoringBattery by remember { mutableStateOf(false) }
                var agendaItems by remember { mutableStateOf<List<AgendaItem>>(emptyList()) }
                var selectedTab by rememberSaveable { mutableStateOf(0) }

                LaunchedEffect(reloadTick.intValue) {
                    calendars = withContext(Dispatchers.IO) {
                        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CALENDAR) ==
                            PackageManager.PERMISSION_GRANTED
                        ) CalendarsRepository(activity).calendars() else emptyList()
                    }
                    ignoringBattery = BatteryOptimization.isIgnoring(activity)
                }

                LaunchedEffect(reloadTick.intValue, s.windowMode, s.excludedCalendarIds, todoList) {
                    agendaItems = withContext(Dispatchers.IO) {
                        AgendaRepository(activity).agenda(s.windowMode, s.excludedCalendarIds)
                    }
                }

                fun refresh() = AgendaScheduler.refreshNow(activity)
                fun edit(block: suspend SettingsRepository.() -> Unit) =
                    scope.launch { settings.block(); refresh() }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(painterResource(R.drawable.ic_tab_todos), contentDescription = null) },
                                label = { Text("To-dos") },
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(painterResource(R.drawable.ic_tab_settings), contentDescription = null) },
                                label = { Text("Settings") },
                            )
                        }
                    },
                ) { innerPadding ->
                    val todoScroll = rememberScrollState()
                    val settingsScroll = rememberScrollState()
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(if (selectedTab == 0) todoScroll else settingsScroll),
                    ) {
                        Text(
                            if (selectedTab == 0) "Pinned Calendar" else "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp),
                        )

                        if (selectedTab == 0) {
                            TodosTab(
                                agendaItems = agendaItems,
                                todoList = todoList,
                                onAdd = { title -> scope.launch { todos.add(title, System.currentTimeMillis()); refresh() } },
                                onToggle = { id -> scope.launch { todos.toggle(id); refresh() } },
                                onDelete = { id -> scope.launch { todos.delete(id); refresh() } },
                                onUpdate = { t ->
                                    scope.launch {
                                        todos.update(t.id, t.title, t.dueMillis, t.notes, t.priority, t.recurrence)
                                        refresh()
                                    }
                                },
                                onAddRich = { t ->
                                    scope.launch {
                                        todos.add(t.title, t.dueMillis, t.notes, t.priority, t.recurrence)
                                        refresh()
                                    }
                                },
                            )
                        } else {
                            SettingsTab(
                                s = s,
                                calendars = calendars,
                                ignoringBattery = ignoringBattery,
                                edit = { block -> edit(block) },
                                onOpenNotificationSettings = {
                                    runCatching { activity.startActivity(NotificationSettingsIntent.forApp(activity)) }
                                },
                                onRequestBatteryExemption = {
                                    runCatching { activity.startActivity(BatteryOptimization.requestIntent(activity)) }
                                },
                                onOpenPrivacyPolicy = {
                                    runCatching {
                                        activity.startActivity(
                                            Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()),
                                        )
                                    }
                                },
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TodosTab(
    agendaItems: List<AgendaItem>,
    todoList: List<LocalTodo>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (LocalTodo) -> Unit,
    onAddRich: (LocalTodo) -> Unit,
) {
    var newTitle by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var drafting by rememberSaveable { mutableStateOf(false) }

    WeekOverviewCard(agendaItems)

    SettingsCard("To-dos") {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("New to-do (due today)") },
                singleLine = true,
                shape = AppShape.field,
                trailingIcon = {
                    // Expands the quick add into the full editor: schedule, priority, notes.
                    IconButton(onClick = { drafting = true }) {
                        Text(
                            "⋯",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            // The outlined field reserves headroom for its floating label; nudge the button down
            // so it centers on the field's box rather than the composable's full height.
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val title = newTitle
                    if (title.isNotBlank()) {
                        newTitle = ""
                        onAdd(title)
                    }
                },
            ) { Text("Add") }
        }
        if (todoList.isEmpty()) {
            CardCaption("No to-dos yet.")
        }
        TodoGroups.of(todoList, LocalDate.now(), ZoneId.systemDefault()).forEach { (groupLabel, groupItems) ->
            CardCaption(groupLabel)
            groupItems.forEach { todo ->
                ListItem(
                    colors = transparentListItem(),
                    modifier = Modifier.clickable { editingId = todo.id },
                    leadingContent = {
                        Checkbox(
                            checked = todo.completed,
                            onCheckedChange = { onToggle(todo.id) },
                        )
                    },
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            todo.priority.colorHex?.let { hex ->
                                ColorDot(hex)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                todo.title,
                                textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                            )
                        }
                    },
                    supportingContent = {
                        val due = dueLabel(todo.dueMillis)
                        val repeat = todo.recurrence?.let {
                            recurrenceSummary(it, todo.dueMillis, ZoneId.systemDefault())
                        }
                        val hasNotes = todo.notes.isNotBlank()
                        if (due != null || repeat != null || hasNotes) {
                            Text(
                                listOfNotNull(due, repeat, if (hasNotes) "Notes" else null).joinToString(" · "),
                                color = if (isOverdue(todo)) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingContent = {
                        TextButton(onClick = { onDelete(todo.id) }) { Text("Delete") }
                    },
                )
            }
        }
    }

    val editing = todoList.firstOrNull { it.id == editingId }
    if (editing != null) {
        TodoEditorSheet(
            todo = editing,
            onSave = { onUpdate(it); editingId = null },
            onDelete = { onDelete(editing.id); editingId = null },
            onDismiss = { editingId = null },
        )
    }

    if (drafting) {
        TodoEditorSheet(
            todo = LocalTodo(id = "draft", title = newTitle, dueMillis = System.currentTimeMillis()),
            isNew = true,
            onSave = { onAddRich(it); newTitle = ""; drafting = false },
            onDelete = { drafting = false },
            onDismiss = { drafting = false },
        )
    }
}

private fun dueLabel(dueMillis: Long?): String? {
    val due = dueMillis ?: return null
    val date = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Due today"
        today.plusDays(1) -> "Due tomorrow"
        else -> "Due ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}"
    }
}

private fun isOverdue(todo: LocalTodo): Boolean {
    val due = todo.dueMillis ?: return false
    return !todo.completed &&
        Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate() < LocalDate.now()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TodoEditorSheet(
    todo: LocalTodo,
    onSave: (LocalTodo) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    isNew: Boolean = false,
) {
    // Saveable so in-progress edits survive activity recreation (rotation, dark-mode flip);
    // enum and nullable values ride along as their primitive forms.
    var title by rememberSaveable(todo.id) { mutableStateOf(todo.title) }
    var notes by rememberSaveable(todo.id) { mutableStateOf(todo.notes) }
    var priorityValue by rememberSaveable(todo.id) { mutableStateOf(todo.priority.value) }
    var recurrenceValue by rememberSaveable(todo.id) {
        mutableStateOf(todo.recurrence?.toStateValue().orEmpty())
    }
    var dueMillisOrZero by rememberSaveable(todo.id) { mutableStateOf(todo.dueMillis ?: 0L) }
    val priority = TodoPriority.from(priorityValue)
    val recurrence = TodoRecurrence.fromStateValue(recurrenceValue)
    val dueMillis: Long? = dueMillisOrZero.takeIf { it != 0L }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceEditor by rememberSaveable(todo.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = AppShape.field,
                modifier = Modifier.fillMaxWidth(),
            )

            CardCaption("Schedule")
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val dueDate = dueMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Today" to today,
                    "Tomorrow" to today.plusDays(1),
                    "Next week" to today.plusWeeks(1),
                ).forEach { (label, date) ->
                    PillChip(dueDate == date, { dueMillisOrZero = TodoSchedule.at(dueMillis, date, zone) }, label)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(dueLabel(dueMillis) ?: "Pick a date")
                }
                if (dueMillis != null) {
                    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
                    TextButton(onClick = { showTimePicker = true }) { Text(timeLabel(dueMillis, zone, is24Hour)) }
                    TextButton(onClick = {
                        dueMillisOrZero = 0L
                        recurrenceValue = ""
                    }) { Text("Clear") }
                }
            }

            CardCaption("Repeat")
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillChip(
                    selected = recurrence == null,
                    onClick = { recurrenceValue = "" },
                    label = "Never",
                )
                RecurrenceFrequency.entries.forEach { frequency ->
                    val preset = TodoRecurrence.preset(frequency)
                    PillChip(
                        selected = recurrence == preset,
                        onClick = {
                            if (dueMillis == null) {
                                dueMillisOrZero = TodoSchedule.at(null, today, zone)
                            }
                            recurrenceValue = preset.toStateValue()
                        },
                        label = frequency.presetLabel,
                    )
                }
                PillChip(
                    selected = recurrence != null && !recurrence.isSimplePreset,
                    onClick = {
                        if (dueMillis == null) dueMillisOrZero = TodoSchedule.at(null, today, zone)
                        showRecurrenceEditor = true
                    },
                    label = "Custom…",
                )
            }
            recurrence?.let {
                Text(
                    recurrenceSummary(it, dueMillis, zone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                )
            }

            CardCaption("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TodoPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priorityValue = p.value },
                        label = { Text(p.label) },
                        shape = AppShape.chip,
                        border = null,
                        leadingIcon = p.colorHex?.let { hex -> { ColorDot(hex) } },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                shape = AppShape.field,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isNew) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                } else {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(
                            todo.copy(
                                title = title,
                                dueMillis = dueMillis,
                                notes = notes,
                                priority = priority,
                                recurrence = recurrence,
                            ),
                        )
                    },
                ) { Text(if (isNew) "Add" else "Save") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        // The picker hands back UTC midnight for the chosen day; scheduledAt keeps the previous
        // time of day so a date change never silently moves the item within a day.
        val zone = ZoneId.systemDefault()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueMillis?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        dueMillisOrZero = TodoSchedule.at(dueMillis, date, zone)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker && dueMillis != null) {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(dueMillis).atZone(zone)
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueMillisOrZero = current.toLocalDate()
                        .atTime(LocalTime.of(timeState.hour, timeState.minute))
                        .atZone(zone).toInstant().toEpochMilli()
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) },
        )
    }

    if (showRecurrenceEditor) {
        val zone = ZoneId.systemDefault()
        val anchorDate = dueMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: LocalDate.now()
        RecurrenceEditorDialog(
            initial = recurrence ?: TodoRecurrence.preset(RecurrenceFrequency.WEEKLY),
            anchorDate = anchorDate,
            onSave = {
                recurrenceValue = it.toStateValue()
                showRecurrenceEditor = false
            },
            onDismiss = { showRecurrenceEditor = false },
        )
    }
}

private fun timeLabel(dueMillis: Long, zone: ZoneId, is24Hour: Boolean): String =
    Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalTime()
        .format(DateTimeFormatter.ofPattern(if (is24Hour) "H:mm" else "h:mm a"))

private enum class RecurrenceEndChoice { NEVER, ON_DATE, AFTER_COUNT }

private fun recurrenceSummary(rule: TodoRecurrence, dueMillis: Long?, zone: ZoneId): String {
    val dueDate = dueMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        ?: LocalDate.now()
    val normalized = rule.normalized(dueDate)
    val base = if (normalized.isSimplePreset) {
        normalized.frequency.presetLabel
    } else {
        val unit = if (normalized.interval == 1) {
            normalized.frequency.singularLabel
        } else {
            normalized.frequency.pluralLabel
        }
        buildString {
            append(if (normalized.interval == 1) "Every $unit" else "Every ${normalized.interval} $unit")
            if (normalized.frequency == RecurrenceFrequency.WEEKLY) {
                val days = normalized.weekdays.ifEmpty { setOf(dueDate.dayOfWeek) }
                    .sortedBy { it.value }
                    .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
                append(" on $days")
            }
        }
    }
    val ending = when {
        normalized.endDate != null ->
            "until ${normalized.endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))}"
        normalized.maxOccurrences != null -> {
            val noun = if (normalized.maxOccurrences == 1) "occurrence" else "occurrences"
            "for ${normalized.maxOccurrences} $noun"
        }
        else -> null
    }
    return listOfNotNull(base, ending).joinToString(" · ")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurrenceEditorDialog(
    initial: TodoRecurrence,
    anchorDate: LocalDate,
    onSave: (TodoRecurrence) -> Unit,
    onDismiss: () -> Unit,
) {
    val stateKey = "${initial.toStateValue()}|${anchorDate.toEpochDay()}"
    var frequencyValue by rememberSaveable(stateKey) { mutableStateOf(initial.frequency.persistedValue) }
    var interval by rememberSaveable(stateKey) { mutableIntStateOf(initial.interval) }
    val initialDays = initial.weekdays.ifEmpty { setOf(anchorDate.dayOfWeek) }
    var weekdayMask by rememberSaveable(stateKey) {
        mutableIntStateOf(TodoRecurrence.weekdayMask(initialDays))
    }
    val initialEndChoice = when {
        initial.endDate != null -> RecurrenceEndChoice.ON_DATE
        initial.maxOccurrences != null -> RecurrenceEndChoice.AFTER_COUNT
        else -> RecurrenceEndChoice.NEVER
    }
    var endChoiceValue by rememberSaveable(stateKey) { mutableStateOf(initialEndChoice.name) }
    var endDateEpochDay by rememberSaveable(stateKey) {
        mutableStateOf(initial.endDate?.toEpochDay())
    }
    var occurrenceCount by rememberSaveable(stateKey) {
        mutableIntStateOf(initial.maxOccurrences ?: 10)
    }
    var showEndDatePicker by rememberSaveable(stateKey) { mutableStateOf(false) }
    val frequency = RecurrenceFrequency.fromPersisted(frequencyValue) ?: RecurrenceFrequency.WEEKLY
    val endChoice = runCatching { RecurrenceEndChoice.valueOf(endChoiceValue) }
        .getOrDefault(RecurrenceEndChoice.NEVER)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom recurrence") },
        confirmButton = {
            TextButton(onClick = {
                val endDate = if (endChoice == RecurrenceEndChoice.ON_DATE) {
                    LocalDate.ofEpochDay(endDateEpochDay ?: anchorDate.plusMonths(1).toEpochDay())
                } else {
                    null
                }
                val selectedWeekdays = TodoRecurrence.weekdaysFromMask(weekdayMask)
                val weekdays = if (
                    frequency == RecurrenceFrequency.WEEKLY &&
                    initial.frequency == RecurrenceFrequency.WEEKLY &&
                    initial.weekdays.isEmpty() &&
                    selectedWeekdays == setOf(anchorDate.dayOfWeek)
                ) {
                    emptySet()
                } else {
                    selectedWeekdays
                }
                onSave(
                    TodoRecurrence(
                        frequency = frequency,
                        interval = interval,
                        weekdays = if (frequency == RecurrenceFrequency.WEEKLY) weekdays else emptySet(),
                        endDate = endDate,
                        maxOccurrences = occurrenceCount.takeIf {
                            endChoice == RecurrenceEndChoice.AFTER_COUNT
                        },
                    ).normalized(anchorDate),
                )
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DialogSectionLabel("Frequency")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceFrequency.entries.forEach { option ->
                        PillChip(
                            selected = frequency == option,
                            onClick = { frequencyValue = option.persistedValue },
                            label = option.presetLabel,
                        )
                    }
                }

                DialogSectionLabel("Repeat every")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        enabled = interval > 1,
                        onClick = { interval-- },
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    val unit = if (interval == 1) frequency.singularLabel else frequency.pluralLabel
                    Text(
                        "$interval $unit",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(
                        enabled = interval < TodoRecurrence.MAX_INTERVAL,
                        onClick = { interval++ },
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }

                if (frequency == RecurrenceFrequency.WEEKLY) {
                    DialogSectionLabel("Repeat on")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val bit = 1 shl (day.value - 1)
                            val selected = weekdayMask and bit != 0
                            PillChip(
                                selected = selected,
                                onClick = {
                                    val updated = weekdayMask xor bit
                                    if (updated != 0) weekdayMask = updated
                                },
                                label = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                            )
                        }
                    }
                }

                DialogSectionLabel("Ends")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RecurrenceEndChoice.NEVER to "Never",
                        RecurrenceEndChoice.ON_DATE to "On date",
                        RecurrenceEndChoice.AFTER_COUNT to "After",
                    ).forEach { (choice, label) ->
                        PillChip(
                            selected = endChoice == choice,
                            onClick = { endChoiceValue = choice.name },
                            label = label,
                        )
                    }
                }
                when (endChoice) {
                    RecurrenceEndChoice.NEVER -> Unit
                    RecurrenceEndChoice.ON_DATE -> {
                        val endDate = LocalDate.ofEpochDay(
                            endDateEpochDay ?: anchorDate.plusMonths(1).toEpochDay(),
                        )
                        TextButton(onClick = { showEndDatePicker = true }) {
                            Text(endDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)))
                        }
                    }
                    RecurrenceEndChoice.AFTER_COUNT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                enabled = occurrenceCount > 1,
                                onClick = { occurrenceCount-- },
                            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                            Text(
                                "$occurrenceCount occurrences",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            IconButton(
                                enabled = occurrenceCount < TodoRecurrence.MAX_OCCURRENCES,
                                onClick = { occurrenceCount++ },
                            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }

                Text(
                    "Completing the current occurrence advances this to-do to the next scheduled date. " +
                        "Missed dates are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
    )

    if (showEndDatePicker) {
        val initialDate = LocalDate.ofEpochDay(
            endDateEpochDay ?: anchorDate.plusMonths(1).toEpochDay(),
        )
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utc ->
                        val selected = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        endDateEpochDay = maxOf(selected.toEpochDay(), anchorDate.toEpochDay())
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun DialogSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsTab(
    s: AppSettings,
    calendars: List<CalendarInfo>,
    ignoringBattery: Boolean,
    edit: (suspend SettingsRepository.() -> Unit) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    SettingsCard("Notifications") {
        CardItem(
            title = "Pin to notifications",
            subtitle = "Keep this week's agenda in the drawer",
            trailing = {
                Switch(checked = s.pinEnabled, onCheckedChange = { v -> edit { setPinEnabled(v) } })
            },
        )
        CardCaption("Priority")
        ChipRow {
            NotificationPriority.entries.forEach { p ->
                PillChip(
                    s.notificationPriority == p,
                    { edit { setNotificationPriority(p) } },
                    p.label,
                )
            }
        }
        CardCaption(
            when (s.notificationPriority) {
                NotificationPriority.TOP ->
                    "Sits above other notifications — never pops up or makes a sound."
                NotificationPriority.NORMAL ->
                    "Mixes in with your everyday notifications."
                NotificationPriority.SILENT ->
                    "Stays below the shade's 'Silent' divider."
            },
        )
        CardItem(
            title = "Swipe twice to remove",
            subtitle = "Swipe the pin away twice within a few seconds to turn it off",
            trailing = {
                Switch(
                    checked = s.doubleSwipeDismiss,
                    onCheckedChange = { v -> edit { setDoubleSwipeDismiss(v) } },
                )
            },
        )
        CardItem(
            title = "To-do reminders",
            subtitle = "A normal, swipeable notification when a scheduled to-do comes due. The pin is unaffected",
            trailing = {
                Switch(
                    checked = s.todoReminders,
                    onCheckedChange = { v -> edit { setTodoReminders(v) } },
                )
            },
        )
        Row(Modifier.padding(start = 8.dp)) {
            TextButton(onClick = onOpenNotificationSettings) { Text("Fine-tune in system settings") }
        }
    }

    SettingsCard("Notification layout") {
        NotificationLayoutContent(s, edit)
    }

    SettingsCard("Time window") {
        ChipRow {
            WindowMode.entries.forEach { mode ->
                PillChip(s.windowMode == mode, { edit { setWindowMode(mode) } }, mode.label)
            }
        }
    }

    SettingsCard("Calendars") {
        if (calendars.isEmpty()) {
            CardCaption("Grant calendar access to choose which calendars appear.")
        }
        calendars.forEach { cal ->
            val enabled = !s.excludedCalendarIds.contains(cal.id)
            ListItem(
                colors = transparentListItem(),
                leadingContent = { ColorDot(cal.colorHex) },
                headlineContent = { Text(cal.name) },
                trailingContent = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on -> edit { setCalendarExcluded(cal.id, !on) } },
                    )
                },
            )
        }
    }

    SettingsCard("Display") {
        CardItem(
            title = "Group by day",
            trailing = {
                Switch(checked = s.groupByDay, onCheckedChange = { v -> edit { setGroupByDay(v) } })
            },
        )
        CardItem(
            title = "Hide completed to-dos",
            trailing = {
                Switch(checked = s.hideCompletedTasks, onCheckedChange = { v -> edit { setHideCompleted(v) } })
            },
        )
        CardItem(
            title = "24-hour time",
            subtitle = "Show event times as 14:30 instead of 2:30 PM",
            trailing = {
                Switch(checked = s.use24HourClock, onCheckedChange = { v -> edit { setUse24HourClock(v) } })
            },
        )
        Text(
            "Max items in notification: ${s.maxItems}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 20.dp, top = 6.dp),
        )
        WavySlider(
            value = s.maxItems.toFloat(),
            onValueChange = { v -> edit { setMaxItems(v.roundToInt()) } },
            valueRange = 3f..12f,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }

    SettingsCard("Appearance") {
        ChipRow {
            ThemeMode.entries.forEach { mode ->
                PillChip(
                    s.themeMode == mode,
                    { edit { setThemeMode(mode) } },
                    mode.name.lowercase().replaceFirstChar { it.uppercase() },
                )
            }
        }
        CardItem(
            title = "Material You",
            subtitle = "Use the wallpaper colour scheme",
            trailing = {
                Switch(checked = s.materialYou, onCheckedChange = { v -> edit { setMaterialYou(v) } })
            },
        )
        CardItem(
            title = "AMOLED black",
            subtitle = "Pure-black surfaces in dark mode",
            trailing = {
                Switch(checked = s.amoled, onCheckedChange = { v -> edit { setAmoled(v) } })
            },
        )
        CardCaption("Accent (used when Material You is off)")
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SEED_SWATCHES.forEach { argb ->
                val selected = s.seedColorArgb == argb
                // Expressive accent: the chosen swatch blooms from a disc into a scalloped cookie.
                val morph by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(durationMillis = 350),
                    label = "swatchMorph",
                )
                val shape = MorphableShape(CircleToCookieMorph, morph)
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(shape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = shape,
                        )
                        .clickable { edit { setSeedColor(argb) } },
                )
            }
        }
        CardCaption("Palette")
        ChipRow {
            AppPalette.entries.forEach { p ->
                PillChip(s.palette == p, { edit { setPalette(p) } }, p.label)
            }
        }
        CardCaption("Font")
        ChipRow {
            AppFont.entries.forEach { f ->
                PillChip(s.font == f, { edit { setFont(f) } }, f.label)
            }
        }
    }

    SettingsCard("Reliability") {
        ListItem(
            colors = transparentListItem(),
            headlineContent = { Text("Ignore battery optimizations") },
            supportingContent = {
                Text(
                    if (ignoringBattery) "On — the pin stays reliable in the background"
                    else "Recommended on aggressive devices so the pin keeps updating",
                )
            },
            trailingContent = {
                if (ignoringBattery) {
                    Text("On", color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = onRequestBatteryExemption) { Text("Allow") }
                }
            },
        )
    }

    SettingsCard("About") {
        ListItem(
            colors = transparentListItem(),
            modifier = Modifier.clickable(onClick = onOpenPrivacyPolicy),
            headlineContent = { Text("Privacy policy") },
            supportingContent = { Text("How Pinned Calendar handles your data — opens the website") },
        )
    }
}

@Composable
private fun WeekOverviewCard(items: List<AgendaItem>) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val days = (0L..6L).map { today.plusDays(it) }
    val countByDate = items.mapNotNull { it.start?.atZone(zone)?.toLocalDate() }
        .groupingBy { it }.eachCount()
    val counts = days.map { countByDate[it] ?: 0 }
    val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val events = items.count { it.kind == ItemKind.EVENT }
    val tasks = items.count { it.kind == ItemKind.TASK }
    val onColor = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = AppShape.cardLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${items.size}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = onColor,
                )
                Spacer(Modifier.width(12.dp))
                Text("items pinned\nthis week", style = MaterialTheme.typography.titleMedium, color = onColor)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEachIndexed { i, d ->
                    Column(
                        modifier = Modifier.weight(1f).height(72.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val frac = counts[i].toFloat() / maxCount
                        Box(
                            Modifier
                                .width(16.dp)
                                .height((8 + frac * 42).dp)
                                .clip(AppShape.bar)
                                .background(if (counts[i] > 0) MaterialTheme.colorScheme.primary else onColor.copy(alpha = 0.18f)),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(dayInitial(d), style = MaterialTheme.typography.labelSmall, color = onColor)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("$events events", onColor)
                StatPill("$tasks to-dos", onColor)
            }
        }
    }
}

@Composable
private fun StatPill(text: String, onColor: Color) {
    Box(
        Modifier
            .clip(AppShape.pill)
            .background(onColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = onColor)
    }
}

private fun dayInitial(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())

@Composable
private fun PillChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = AppShape.chip,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = AppShape.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(top = 14.dp, bottom = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
            )
            content()
        }
    }
}

@Composable
private fun CardItem(title: String, subtitle: String? = null, trailing: @Composable () -> Unit) {
    ListItem(
        colors = transparentListItem(),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = trailing,
    )
}

@Composable
private fun transparentListItem() = ListItemDefaults.colors(containerColor = Color.Transparent)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun CardCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun ColorDot(hex: String?) {
    val color = hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: MaterialTheme.colorScheme.primary
    Spacer(Modifier.size(14.dp).background(color, CircleShape))
}

private val SEED_SWATCHES = listOf(
    0xFFE07F2C, 0xFF00897B, 0xFF7E57C2, 0xFFD81B60, 0xFF1A73E8, 0xFF43A047,
).map { it.toInt() }

/** A coherent padding/text/height triple for the notification rows. */
private enum class DensityPreset(
    val label: String,
    val paddingDp: Int,
    val textSp: Int,
    val heightDp: Int,
) {
    COMPACT("Compact", 2, 12, 16),
    COZY("Cozy", 5, 14, 22),
    COMFORTABLE("Comfortable", 8, 16, 28),
    ;

    fun matches(s: AppSettings): Boolean =
        s.notificationRowPaddingDp == paddingDp &&
            s.notificationRowTextSizeSp == textSp &&
            s.notificationRowHeightDp == heightDp
}

@Composable
private fun ColumnScope.NotificationLayoutContent(
    s: AppSettings,
    edit: (suspend SettingsRepository.() -> Unit) -> Unit,
) {
    NotificationPreview(s)

    CardCaption("Density")
    // The active preset is derived from the stored triple, so the chips never disagree with
    // what the notification actually renders. Custom is a UI state, not a persisted value:
    // a hand-tuned triple lands on Custom once on entry, and after that only the chips move
    // it, so the sliders never unmount mid-drag when a drag passes through a preset's values.
    val activePreset = DensityPreset.entries.firstOrNull { it.matches(s) }
    var customChosen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (activePreset == null) customChosen = true }
    ChipRow {
        DensityPreset.entries.forEach { preset ->
            PillChip(
                selected = !customChosen && activePreset == preset,
                onClick = {
                    customChosen = false
                    edit { setNotificationDensity(preset.paddingDp, preset.textSp, preset.heightDp) }
                },
                label = preset.label,
            )
        }
        PillChip(customChosen, { customChosen = true }, "Custom")
    }
    if (customChosen) {
        SliderRow("Row spacing", s.notificationRowPaddingDp, 0f..12f) { edit { setNotificationRowPadding(it) } }
        SliderRow("Text size", s.notificationRowTextSizeSp, 11f..18f) { edit { setNotificationRowTextSize(it) } }
        SliderRow("Row height", s.notificationRowHeightDp, 12f..32f) { edit { setNotificationRowHeight(it) } }
        SliderRow("Time column width", s.notificationTimeColumnWidthDp, 32f..64f) {
            edit { setNotificationTimeColumnWidth(it) }
        }
    }

    SliderRow("Rows before expanding", s.collapsedItems, 1f..6f) { edit { setCollapsedItems(it) } }
    CardItem(
        title = "Show “This week” heading",
        subtitle = "Display the heading above the expanded agenda",
        trailing = {
            Switch(
                checked = s.showNotificationHeader,
                onCheckedChange = { v -> edit { setShowNotificationHeader(v) } },
            )
        },
    )
    CardItem(
        title = "Show Today label",
        subtitle = "Display Today in multi-row compact and expanded notifications",
        trailing = {
            Switch(
                checked = s.showTodayHeader,
                onCheckedChange = { v -> edit { setShowTodayHeader(v) } },
            )
        },
    )
    CardItem(
        title = "Outer notification padding",
        subtitle = "Add vertical space above and below the notification content",
        trailing = {
            Switch(
                checked = s.notificationContentPadding,
                onCheckedChange = { v -> edit { setNotificationContentPadding(v) } },
            )
        },
    )
}

@Composable
private fun SliderRow(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onChange: (Int) -> Unit) {
    Text(
        "$label: $value",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 20.dp, top = 6.dp),
    )
    WavySlider(
        value = value.toFloat(),
        onValueChange = { v -> onChange(v.roundToInt()) },
        valueRange = range,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun NotificationPreview(s: AppSettings) {
    val configuration = LocalConfiguration.current
    var showExpanded by rememberSaveable { mutableStateOf(false) }

    // Sample content runs through the real pipeline (bucketer + content builder), so the preview
    // obeys the same grouping, capping, and clock settings the posted notification does.
    val content = remember(s.use24HourClock, s.groupByDay, s.hideCompletedTasks, s.maxItems) {
        val clock = Clock.systemDefaultZone()
        NotificationContentBuilder(DayBucketer(clock, use24Hour = s.use24HourClock)).build(
            SampleAgenda.items(clock),
            DisplaySettings(s.maxItems, s.hideCompletedTasks, s.groupByDay),
        )
    }

    // The renderer keys its text colors off the SYSTEM night mode (the shade's theme), not the
    // app theme, so the preview backdrop must follow the same signal or the text goes illegible.
    val systemDark =
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val shadeBackground = if (systemDark) Color(0xFF1F2124) else Color(0xFFE9EBEE)

    ChipRow {
        PillChip(!showExpanded, { showExpanded = false }, "Collapsed")
        PillChip(showExpanded, { showExpanded = true }, "Expanded")
    }
    AndroidView(
        factory = { ctx -> FrameLayout(ctx) },
        update = { host ->
            val renderer = AgendaRemoteViewsRenderer(host.context)
            val rv = if (showExpanded) {
                renderer.expanded(
                    content,
                    s.showNotificationHeader,
                    s.showTodayHeader,
                    s.notificationRowPaddingDp,
                    s.notificationRowTextSizeSp,
                    s.notificationRowHeightDp,
                    s.notificationTimeColumnWidthDp,
                    s.notificationContentPadding,
                )
            } else {
                renderer.collapsed(
                    content,
                    s.collapsedItems,
                    s.showTodayHeader,
                    s.notificationRowPaddingDp,
                    s.notificationRowTextSizeSp,
                    s.notificationRowHeightDp,
                    s.notificationTimeColumnWidthDp,
                    s.notificationContentPadding,
                )
            }
            host.removeAllViews()
            host.addView(rv.apply(host.context, host))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(AppShape.card)
            .background(shadeBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
