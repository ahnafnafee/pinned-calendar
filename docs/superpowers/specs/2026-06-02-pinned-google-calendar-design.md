# Pinned Google Calendar — Design Doc

- **Date:** 2026-06-02
- **Status:** APPROVED (2026-06-02) — §12 defaults accepted
- **Revision (2026-06-02): LOCAL-FIRST.** Superseding the Google-API data plan below: **events are read from the device Calendar Provider (`READ_CALENDAR`), and "tasks" are a local in-app to-do list.** No Google Sign-In, no OAuth, no Calendar/Tasks REST, no Google Cloud project, no network. §5.5 (Auth), §10 (OAuth setup), and the `calendar.readonly`/`tasks.readonly` scopes in §2 no longer apply. Permissions reduce to `READ_CALENDAR` + `POST_NOTIFICATIONS` + `RECEIVE_BOOT_COMPLETED` (drop `INTERNET`). The notification/builder/worker/self-heal layers are unchanged (they already consume the `AgendaItem` domain model).
- **Author:** brainstorming session
- **Next step:** implementation plan (writing-plans)

---

## 1. Problem & Goal

Calendar notifications get accidentally swiped away, so upcoming commitments stop being visible. **Goal:** an Android app that keeps a single, persistent, *self-healing* notification in the drawer showing this week's Google Calendar **events** and Google **Tasks**, styled to feel like the Google Calendar app, with an in-app screen to control what's shown.

**Success criteria:**
- A pinned notification is always present in the drawer; an accidental swipe re-posts it within ~1s.
- It shows a configurable window (default: this week) of events + tasks, grouped by day, color-coded per calendar.
- The user can choose which calendars/task-lists feed it, the time window, and display options — without touching the calendar source data (read-only).
- Light/dark + Material You adaptive color throughout.
- Installs as a sideloaded APK for personal use; no Google verification review required.

---

## 2. Decisions Locked (from brainstorming)

| Decision | Choice | Rationale |
|---|---|---|
| Platform | **Native Kotlin + Jetpack Compose (Material 3)** | The hard part (persistent/self-heal notification, RemoteViews custom layout, background refresh) is Android-native; Compose M3 is the reference Material You implementation. |
| Distribution | **Personal sideload APK** | OAuth stays in "testing" mode → no Google verification, no privacy policy. |
| Data sources | **Google Calendar events + Google Tasks** | User needs both; Tasks has no on-device source, so Google APIs required. |
| Access | **Read-only** (`calendar.readonly`, `tasks.readonly`) | "Modifiable" = control what's shown, not edit source entries. |
| Notification expanded style | **Rich agenda (B)** — color bars, day-group headers, tasks as check-circles | Most "inviting" / closest to Google Calendar agenda. |
| Config screen | **Single scrolling page** | Everything visible; fast tweaking. |
| Theming | **Light + dark + Material You adaptive color** | Accent tracks wallpaper (API 31+), seeded-blue fallback below. Event colors stay vivid (data, not theme). |
| Notification keep-alive | **WorkManager + self-heal receiver (NO foreground service)** | Avoids Android 14 FGS-type restrictions; lighter on battery. |
| Data access mechanism | **Calendar/Tasks REST over HTTPS** (not device CalendarProvider) | No `READ_CALENDAR` permission needed; unified model for events + tasks. |

---

## 3. Scope

**In scope (v1):**
- Google Sign-In + incremental authorization for read-only Calendar + Tasks.
- Fetch & merge events + tasks within a configurable window.
- Persistent, ongoing, self-healing notification (collapsed + expanded custom layouts).
- Single-page config: time window, calendar toggles, task-list toggles, display options, master pin on/off, theme.
- Light/dark + dynamic color. Offline render from cache.

**Out of scope (v1, see §11):**
- Editing/creating/completing events or tasks from the app or notification (read-only).
- Multiple Google accounts (single signed-in account in v1).
- iOS / cross-platform.
- Home-screen widgets (notification only).
- Recurring-event editing, attendees, RSVP.

---

## 4. UX

### 4.1 Pinned notification (the core)

Built with classic **RemoteViews** wrapped in `NotificationCompat.DecoratedCustomViewStyle` (system chrome — app icon, name, expand caret — around our custom body).

- **Collapsed (always visible):** accent label `This week · N`, then the single next item row (color dot · time · title), e.g. `● 9:00 Team standup · +10 this week`.
- **Expanded:** day-group headers (`TODAY · MON 2`, `TOMORROW · TUE 3`, then weekday + date), each item a row: colored left bar (calendar color) **or** check-circle (task) · time · title. Cap at ~7–8 rows, then `⌄ N more this week`.
- **Channel:** one channel "Pinned agenda", `IMPORTANCE_LOW` (silent, no vibration, always in shade). `setOngoing(true)`, `setAutoCancel(false)`, category `CATEGORY_STATUS`.
- **Accent color:** `setColor()` resolved from `system_accent1` at runtime (dynamic), fallback blue. Light/dark RemoteViews layout chosen from current UI mode.
- **Tap targets:** tapping an item → open it in Google Calendar / Google Tasks (via the item's `htmlLink`/intent; falls back to browser if app absent). Tapping notification chrome → open the config app.
- **Empty state:** "Nothing scheduled this week 🎉".

### 4.2 In-app config screen (single scrolling page, M3)

Top app bar "Pinned Calendar". Sections top-to-bottom:
1. **Live preview** card (mini render of the current notification).
2. **Master toggle:** "Pin to notifications" (on/off — lets the user intentionally stop the self-healing pin).
3. **Time window:** filter chips — `3 days` · `This week` · `7 days` · `14 days` (default **This week** = through end of current locale week). 7/14 are rolling.
4. **Calendars:** one row per Google calendar — color dot · name · M3 switch.
5. **Task lists:** one row per Google Tasks list — switch.
6. **Display:** Group by day (default on) · Show all-day events (on) · Hide completed tasks (on) · Max items in notification (slider, default 8).
7. **Appearance:** Theme (System / Light / Dark) · Use dynamic color (on).

Any change → enqueue an expedited refresh worker → notification updates.

### 4.3 Onboarding / sign-in

First launch: brief explainer → "Sign in with Google" (Credential Manager) → authorize Calendar + Tasks (AuthorizationClient) → request `POST_NOTIFICATIONS` (API 33+) → optional "ignore battery optimizations" prompt for reliability → land on config with the pin enabled.

### 4.4 Theming

Compose M3: `dynamicLightColorScheme`/`dynamicDarkColorScheme` (API 31+) seeded from wallpaper; fallback to a Google-Calendar-blue seeded scheme below 31. Follows `isSystemInDarkTheme()` unless overridden by the Theme setting.

---

## 5. Architecture

### 5.1 Package layout (single module to start)

```
app/
  di/                 (manual or Hilt wiring)
  data/
    auth/             GoogleAuthManager (Credential Manager + AuthorizationClient, token provider)
    remote/           CalendarApi, TasksApi (Retrofit), DTOs
    repository/       AgendaRepository, SourcesRepository (calendars/task-lists)
    prefs/            SettingsRepository (DataStore: window, enabled ids, display opts, theme, pin on/off)
    cache/            Room (or serialized DataStore) cache of last agenda + sources
  domain/
    model/            AgendaItem, DaySection, CalendarSource, TaskListSource
    usecase/          BuildAgendaUseCase (apply window + filters + ordering + grouping)
  notification/       AgendaNotificationBuilder (RemoteViews), NotificationPoster,
                      SelfHealReceiver, ChannelManager
  work/               RefreshAgendaWorker, BootReceiver
  ui/
    main/             MainScreen (single-page config) + ViewModel
    onboarding/       SignInScreen
    components/       Chips, SourceToggleRow, NotificationPreview, sliders
    theme/            Color, Theme (dynamic + fallback), Type
  MainActivity, App (WorkManager + channel init)
```

### 5.2 Data model

```kotlin
data class AgendaItem(
  val id: String,
  val kind: Kind,            // EVENT or TASK
  val title: String,
  val start: Instant?,       // null for undated tasks (excluded by default)
  val allDay: Boolean,
  val colorHex: String?,     // calendar color (events)
  val completed: Boolean,    // tasks
  val deepLink: String?      // htmlLink / intent uri
)
```

`BuildAgendaUseCase`: filter by enabled source ids → filter to window → drop completed tasks (if set) → sort by start (all-day first within a day) → group into `DaySection`s.

### 5.3 Data flow

1. Sign-in → fetch calendar list + task lists → cache → populate config toggles.
2. User edits config → `SettingsRepository` (DataStore) → enqueue **expedited** one-time `RefreshAgendaWorker`.
3. **RefreshAgendaWorker:** get access token → `events.list` (`timeMin=now`, `timeMax=now+window`, `singleEvents=true`, `orderBy=startTime`) per enabled calendar + `tasks.list` per enabled task list → merge via `BuildAgendaUseCase` → write cache → build + post notification.
4. **Periodic** `RefreshAgendaWorker` every 15 min keeps content fresh; also a daily ~00:00 run so TODAY/TOMORROW grouping rolls over.
5. **Self-heal:** notification carries a `deleteIntent` → `SelfHealReceiver` fires on dismissal → re-post from cache (if master toggle on) + ensure worker scheduled.
6. **Boot:** `BootReceiver` → reschedule periodic work + re-post from cache.

### 5.4 Notification mechanism — why WorkManager + self-heal, not a foreground service

- Android 14+ requires a declared `foregroundServiceType`; a "show an agenda" service maps poorly to allowed types (`dataSync` is scrutinized and time-limited on 15+). A persistent informational pin doesn't justify an FGS.
- A posted notification **persists in the shade even if the process is killed**. `setOngoing(true)` keeps it out of "clear all" and (≤ API 33) un-swipeable; on 14+ it's swipeable but the `deleteIntent` → manifest receiver re-posts instantly (works even if the app process was dead).
- WorkManager (15-min floor + expedited on change) is the battery-correct refresher. 15-min staleness is acceptable for a weekly agenda.

### 5.5 Auth & Google APIs

- **Sign-in:** `androidx.credentials` (Credential Manager) + `com.google.android.libraries.identity.googleid` for the Google credential.
- **Authorization:** `Identity.getAuthorizationClient()` requesting scopes `calendar.readonly` + `tasks.readonly` → access token. On-device account → silent token refresh (no weekly re-login).
- **API calls:** **Retrofit + OkHttp + kotlinx.serialization** hitting REST directly (keeps APK lean vs. the heavy Google API Java client):
  - Calendar: `GET /calendar/v3/users/me/calendarList`, `GET /calendar/v3/calendars/{id}/events`
  - Tasks: `GET /tasks/v1/users/@me/lists`, `GET /tasks/v1/lists/{id}/tasks`
  - Bearer access token via OkHttp auth interceptor; 401 → re-authorize.

---

## 6. Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | REST calls |
| `POST_NOTIFICATIONS` (runtime, API 33+) | show the pin |
| `RECEIVE_BOOT_COMPLETED` | re-post + reschedule after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (optional, user-prompted) | reliability on aggressive OEMs |

**Notably NOT needed:** `READ_CALENDAR` / `WRITE_CALENDAR` (we read via the Google API, not the device provider), no foreground-service permission, no `GET_ACCOUNTS`.

---

## 7. Tech stack & versions

- Kotlin, Gradle (Kotlin DSL), **minSdk 26**, target latest stable (35/36).
- Jetpack Compose (BOM) + **Material 3** + Material You dynamic color.
- WorkManager, DataStore (Preferences), Room (cache), Coroutines/Flow.
- Retrofit + OkHttp + kotlinx.serialization.
- Credential Manager (`androidx.credentials`, `googleid`) + Google Identity (`play-services-auth` AuthorizationClient).
- (Optional) Hilt for DI.

---

## 8. Edge cases & states

- **Not signed in / token revoked:** notification shows "Tap to sign in"; config gates behind sign-in.
- **No network:** render cached agenda + subtle "updated Xm ago"; refresh on reconnect.
- **No items in window:** empty-state copy.
- **Undated tasks:** excluded from the week view by default (option to surface later).
- **All-day events:** sorted first within their day.
- **Many items:** expanded capped at `maxItems` (default 8) + "N more"; collapsed = next item + count.
- **Master toggle off:** cancel notification, stop self-heal, keep periodic work paused.
- **Day rollover:** scheduled refresh re-buckets TODAY/TOMORROW.

---

## 9. Known risks & mitigations

| Risk | Mitigation |
|---|---|
| Aggressive OEM battery killers (MIUI/OneUI/etc.) throttle WorkManager | Prompt battery-optimization exemption; posted notification persists regardless; self-heal receiver is manifest-registered (survives process death). |
| OAuth "testing" mode refresh-token expiry (server/offline-token cases) | On-device Google sign-in refreshes access tokens silently; if re-consent ever needed, in-app re-auth flow. Verify behavior during implementation. |
| RemoteViews styling limits (no Compose in notifications) | Build agenda rows programmatically with supported views; verify on a real device early. |
| Android 14+ ongoing notifications are user-swipeable | Self-heal re-post (accidental swipes self-correct; intentional removal via master toggle). |
| Dynamic accent in RemoteViews | Resolve `system_accent1_*` at runtime; fallback blue below API 31. |

---

## 10. Google Cloud / OAuth setup (one-time, manual)

1. Create a Google Cloud project; enable **Google Calendar API** + **Google Tasks API**.
2. OAuth consent screen: **External**, publishing status **Testing**; add the user's Google account as a **test user**; add scopes `calendar.readonly`, `tasks.readonly`.
3. Create an **OAuth client ID (Android)** with the app package name + signing-cert **SHA-1** (debug keystore for debug builds, or release keystore for signed APK).
4. (If using a Web client for token exchange) note the Web client ID for `getGoogleIdOption`.

---

## 11. Non-goals / future

- Write actions (complete task, RSVP, reschedule) — would need write scopes.
- Multiple accounts; home-screen Glance widget; per-event reminders; wear support.

---

## 12. Open questions

_Resolved 2026-06-02 — all defaults accepted by user._

1. **minSdk** — 26 (broad) vs higher (your device only)? Default 26.
2. **Tap-an-item target** — open in Google Calendar/Tasks (assumed) vs open in-app detail?
3. **Undated tasks** — keep excluded (assumed) or show in a "No date" group?
4. **Signing** — debug keystore (simplest) vs a dedicated release keystore for the sideload APK?
