# Pinned Calendar — Plan 1: Foundation + Self-Healing Pinned Notification (mock data)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A sideloadable debug APK that pins a persistent, self-healing Material 3 notification showing a mock weekly agenda (events + tasks), grouped by day, with a master on/off toggle in-app.

**Architecture:** Native Kotlin + Jetpack Compose (Material 3). Pure-logic core (day bucketing, notification-content building) is JVM-testable via TDD and injected `java.time.Clock`. The Android shell (RemoteViews notification, WorkManager refresh, self-heal `BroadcastReceiver`, boot receiver) consumes that core. **No foreground service** — a `WorkManager` periodic refresh keeps the pin fresh and a `deleteIntent` → receiver re-posts it on swipe. Mock data this plan; real Google data is Plan 2; the full config UI is Plan 3.

**Tech Stack:** Kotlin 2.1, AGP 8.7, Jetpack Compose (BOM) + Material 3 (dynamic color), WorkManager, DataStore (Preferences), Coroutines; JUnit4 + Robolectric + WorkManager-testing for tests.

**Roadmap (context only — do not build here):**
- **Plan 1 (this doc):** foundation + self-healing notification with mock data.
- **Plan 2:** Google Sign-In + Calendar/Tasks REST + repositories + cache → real data; `WindowCalculator`.
- **Plan 3:** full single-page Compose config screen (time window, calendar/task-list toggles, display options, theme) + onboarding.

**Spec:** `docs/superpowers/specs/2026-06-02-pinned-google-calendar-design.md`

---

## ⚠️ Build notes & deviations (verified on a Pixel 10 Pro / API 36 emulator)

The task code below was drafted for an AGP-8 / `kotlin-android` world. The **actual, emulator-verified** "latest everything" setup differs — use this:

- **Package:** `com.ahnafnafee.pinnedcalendar`. **compileSdk/targetSdk = 36**, minSdk 26.
- **Pinned versions:** Gradle **9.5.1**, AGP **9.2.1**, Compose BOM **2026.05.01**, core-ktx 1.18.0, activity-compose 1.13.0, lifecycle 2.10.0, work 2.11.2, datastore 1.2.1, coroutines 1.11.0, Robolectric 4.16.1.
- **AGP 9 has built-in Kotlin** — three consequences:
  1. **Do NOT apply `org.jetbrains.kotlin.android`** (it's a hard error in AGP 9).
  2. Apply `org.jetbrains.kotlin.plugin.compose`, but it **must be pinned to AGP's bundled Kotlin version**. AGP 9.2.1 bundles **Kotlin 2.2.10** (find it via the `kotlin-gradle-plugin` dep in `gradle-<v>.pom`). "Latest Kotlin" (2.3.x) is NOT what AGP 9.2.1 ships.
  3. **Omit** any explicit `jvmTarget` — there is no resolvable `android { compilerOptions {} }` nor a needed top-level `kotlin {}` block; AGP's defaults are self-consistent. (The AGP-9 release-notes snippet claiming `android.compilerOptions` did not resolve in 9.2.1.)
- **RemoteViews gotcha (this bit us):** a bare `<View>` throws `Class not allowed to be inflated android.view.View` in notification RemoteViews. Use **`<ImageView>`** (or `TextView`) for the color bars/dots — `setBackgroundColor` still works. Only `@RemoteView` classes inflate.
- **Robolectric:** add `app/src/test/resources/robolectric.properties` with `sdk=34` (4.16.1 may lack an API-36 sandbox).
- **Observation:** with `maxItems=8` plus day-group headers, the expanded notification exceeds the system's max height and clips the last row(s). Consider defaulting `maxItems` to ~6, or accept clipping.

Net "latest stable, mutually-coherent" stack: **AGP 9.2.1 + Gradle 9.5.1 + Kotlin/compose-compiler 2.2.10 + Compose BOM 2026.05.01 + compileSdk 36**.

---

## Prerequisites (verify once before Task 0)

- JDK 17 on PATH (`java -version` → 17.x).
- Android SDK with platform **android-35** and build-tools installed; `ANDROID_HOME`/`ANDROID_SDK_ROOT` set.
- `gradle` available on PATH for one-time wrapper generation (or generate the wrapper from Android Studio). Target Gradle 8.9+.
- A device or emulator (API 26+; API 31+ to see dynamic color) for manual checks.
- Package name used throughout: **`com.dynasty11.pinnedcalendar`**.

> **Version note:** the versions in `libs.versions.toml` are known-good starting pins. If `./gradlew` reports a newer stable, bump the catalog — do not downgrade.

---

## File Structure (created across this plan)

```
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
.gitignore
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/drawable/ic_calendar.xml          (notification small icon, monochrome)
app/src/main/res/layout/notif_collapsed.xml
app/src/main/res/layout/notif_expanded.xml
app/src/main/res/layout/notif_day_header.xml
app/src/main/res/layout/notif_row.xml
app/src/main/java/com/dynasty11/pinnedcalendar/App.kt
app/src/main/java/com/dynasty11/pinnedcalendar/MainActivity.kt
app/src/main/java/com/dynasty11/pinnedcalendar/ui/theme/{Color,Theme,Type}.kt
app/src/main/java/com/dynasty11/pinnedcalendar/domain/model/AgendaItem.kt
app/src/main/java/com/dynasty11/pinnedcalendar/domain/model/NotificationContent.kt
app/src/main/java/com/dynasty11/pinnedcalendar/domain/SampleAgenda.kt
app/src/main/java/com/dynasty11/pinnedcalendar/domain/DayBucketer.kt
app/src/main/java/com/dynasty11/pinnedcalendar/domain/NotificationContentBuilder.kt
app/src/main/java/com/dynasty11/pinnedcalendar/data/SettingsRepository.kt
app/src/main/java/com/dynasty11/pinnedcalendar/notify/ChannelManager.kt
app/src/main/java/com/dynasty11/pinnedcalendar/notify/AccentResolver.kt
app/src/main/java/com/dynasty11/pinnedcalendar/notify/AgendaRemoteViewsRenderer.kt
app/src/main/java/com/dynasty11/pinnedcalendar/notify/AgendaNotificationBuilder.kt
app/src/main/java/com/dynasty11/pinnedcalendar/notify/NotificationPoster.kt
app/src/main/java/com/dynasty11/pinnedcalendar/notify/SelfHealReceiver.kt
app/src/main/java/com/dynasty11/pinnedcalendar/work/RefreshAgendaWorker.kt
app/src/main/java/com/dynasty11/pinnedcalendar/work/AgendaScheduler.kt
app/src/main/java/com/dynasty11/pinnedcalendar/work/BootReceiver.kt
app/src/test/java/com/dynasty11/pinnedcalendar/...                (unit/Robolectric tests)
```

---

## Task 0: Project scaffold + first commit

**Files:** create all Gradle/manifest/app-shell files listed below.

- [ ] **Step 1: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
work = "2.10.0"
datastore = "1.1.1"
coroutines = "1.9.0"
junit = "4.13.2"
robolectric = "4.14.1"
workTesting = "2.10.0"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-ui = { module = "androidx.compose.ui:ui" }
androidx-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-material3 = { module = "androidx.compose.material3:material3" }
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
androidx-work-testing = { module = "androidx.work:work-testing", version.ref = "workTesting" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: Create root `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "PinnedCalendar"
include(":app")
```

- [ ] **Step 3: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- [ ] **Step 4: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dynasty11.pinnedcalendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dynasty11.pinnedcalendar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
}
```

- [ ] **Step 5: Create `app/proguard-rules.pro`** (empty placeholder is fine)

```proguard
# No custom rules yet.
```

- [ ] **Step 6: Create `.gitignore`**

```gitignore
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
app/build/
.superpowers/
```

- [ ] **Step 7: Create `app/src/main/res/values/strings.xml`**

```xml
<resources>
    <string name="app_name">Pinned Calendar</string>
    <string name="channel_name">Pinned agenda</string>
    <string name="channel_desc">The always-visible weekly agenda pin</string>
</resources>
```

- [ ] **Step 8: Create `app/src/main/res/values/themes.xml`**

```xml
<resources>
    <style name="Theme.PinnedCalendar" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 9: Create `app/src/main/res/drawable/ic_calendar.xml`** (monochrome small icon)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="#FFFFFFFF">
    <path android:fillColor="#FF000000"
        android:pathData="M19,4h-1V2h-2v2H8V2H6v2H5C3.9,4 3,4.9 3,6v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2zM19,20H5V9h14V20z"/>
</vector>
```

- [ ] **Step 10: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:theme="@style/Theme.PinnedCalendar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".notify.SelfHealReceiver"
            android:exported="false" />

        <receiver
            android:name=".work.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 11: Create `app/src/main/java/com/dynasty11/pinnedcalendar/App.kt`** (minimal for now; expanded in Task 11)

```kotlin
package com.dynasty11.pinnedcalendar

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

- [ ] **Step 12: Create `app/src/main/java/com/dynasty11/pinnedcalendar/MainActivity.kt`** (stub; expanded in Task 11)

```kotlin
package com.dynasty11.pinnedcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Pinned Calendar") }
    }
}
```

- [ ] **Step 13: Generate the Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.9`
Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/`.

- [ ] **Step 14: Build to verify the scaffold compiles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. An APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 15: Initialize git and commit**

```bash
git init
git add -A
git commit -m "chore: scaffold PinnedCalendar Android project"
```

---

## Task 1: Material 3 theme (light/dark/dynamic)

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/ui/theme/Color.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/ui/theme/Type.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/ui/theme/Theme.kt`

- [ ] **Step 1: Create `Color.kt`** (fallback seed = Google-Calendar blue)

```kotlin
package com.dynasty11.pinnedcalendar.ui.theme

import androidx.compose.ui.graphics.Color

val SeedBlue = Color(0xFF1A73E8)
val SeedBlueDark = Color(0xFF8AB4F8)
```

- [ ] **Step 2: Create `Type.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.ui.theme

import androidx.compose.material3.Typography

val AppTypography = Typography()
```

- [ ] **Step 3: Create `Theme.kt`** (dynamic color on API 31+, seeded fallback below)

```kotlin
package com.dynasty11.pinnedcalendar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FallbackLight = lightColorScheme(primary = SeedBlue)
private val FallbackDark = darkColorScheme(primary = SeedBlueDark)

@Composable
fun PinnedCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Material 3 theme with dynamic color + seeded fallback"
```

---

## Task 2: Domain model + sample agenda

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/domain/model/AgendaItem.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/domain/model/NotificationContent.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/domain/SampleAgenda.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/domain/SampleAgendaTest.kt`

- [ ] **Step 1: Create `AgendaItem.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain.model

import java.time.Instant

enum class ItemKind { EVENT, TASK }

data class AgendaItem(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val start: Instant?,        // null = undated task (excluded from the week view)
    val allDay: Boolean = false,
    val colorHex: String? = null, // calendar color for events; null for tasks
    val completed: Boolean = false,
    val deepLink: String? = null,
)
```

- [ ] **Step 2: Create `NotificationContent.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain.model

data class NotificationRow(
    val time: String,           // "9:00", "All day", or ""
    val title: String,
    val colorHex: String?,      // event calendar color; null => task
    val isTask: Boolean,
    val completed: Boolean,
)

data class DaySection(
    val header: String,         // "TODAY · MON 2" or "" when ungrouped
    val isToday: Boolean,
    val rows: List<NotificationRow>,
)

data class NotificationContent(
    val headerCount: Int,       // total items in window
    val collapsedLine: String,  // next item, e.g. "9:00 Team standup"
    val collapsedColorHex: String?,
    val sections: List<DaySection>,
    val moreCount: Int,         // items beyond the cap
    val isEmpty: Boolean,
)
```

- [ ] **Step 3: Create `SampleAgenda.kt`** (deterministic mock relative to a `Clock`)

```kotlin
package com.dynasty11.pinnedcalendar.domain

import com.dynasty11.pinnedcalendar.domain.model.AgendaItem
import com.dynasty11.pinnedcalendar.domain.model.ItemKind
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId

object SampleAgenda {
    fun items(clock: Clock, zone: ZoneId = ZoneId.systemDefault()): List<AgendaItem> {
        val today = java.time.LocalDate.now(clock.withZone(zone))
        fun at(daysAhead: Long, time: LocalTime) =
            today.plusDays(daysAhead).atTime(time).atZone(zone).toInstant()
        return listOf(
            AgendaItem("e1", ItemKind.EVENT, "Team standup", at(0, LocalTime.of(9, 0)), colorHex = "#039BE5"),
            AgendaItem("e2", ItemKind.EVENT, "1:1 with Sam", at(0, LocalTime.of(14, 0)), colorHex = "#D50000"),
            AgendaItem("t1", ItemKind.TASK, "Submit expense report", at(0, LocalTime.of(17, 0)), colorHex = null),
            AgendaItem("e3", ItemKind.EVENT, "Dentist appointment", at(1, LocalTime.of(10, 0)), colorHex = "#0B8043"),
            AgendaItem("e4", ItemKind.EVENT, "Design review", at(1, LocalTime.of(16, 0)), colorHex = "#8E24AA"),
            AgendaItem("e5", ItemKind.EVENT, "Lunch with Priya", at(2, LocalTime.of(12, 30)), colorHex = "#F4511E"),
            AgendaItem("t2", ItemKind.TASK, "Renew passport", at(3, LocalTime.of(9, 0)), colorHex = null),
            AgendaItem("e6", ItemKind.EVENT, "Sprint planning", at(4, LocalTime.of(11, 0)), colorHex = "#3F51B5"),
        )
    }
}
```

- [ ] **Step 4: Write the failing test `SampleAgendaTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SampleAgendaTest {
    private val zone = ZoneId.of("America/New_York")
    private val clock = Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), zone)

    @Test fun returns_eight_items_within_the_week() {
        val items = SampleAgenda.items(clock, zone)
        assertEquals(8, items.size)
        assertTrue(items.all { it.start != null })
    }

    @Test fun first_item_is_todays_standup() {
        val items = SampleAgenda.items(clock, zone).sortedBy { it.start }
        assertEquals("Team standup", items.first().title)
    }
}
```

- [ ] **Step 5: Run test to verify it fails (then passes once files exist)**

Run: `./gradlew :app:testDebugUnitTest --tests "*SampleAgendaTest"`
Expected: compiles and PASSES (model + sample created in Steps 1–3). If red, fix until green.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add agenda domain model + deterministic sample data"
```

---

## Task 3: DayBucketer (TDD)

Groups agenda items into per-day sections with relative headers (TODAY / TOMORROW / weekday), all-day first within a day.

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/domain/DayBucketer.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/domain/DayBucketerTest.kt`

- [ ] **Step 1: Write the failing test `DayBucketerTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain

import com.dynasty11.pinnedcalendar.domain.model.AgendaItem
import com.dynasty11.pinnedcalendar.domain.model.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class DayBucketerTest {
    private val zone = ZoneId.of("America/New_York")
    // 2026-06-01 is a Monday.
    private val clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), zone)
    private val bucketer = DayBucketer(clock, zone)

    private fun event(daysAhead: Long, h: Int, m: Int, title: String, allDay: Boolean = false): AgendaItem {
        val date = java.time.LocalDate.now(clock.withZone(zone)).plusDays(daysAhead)
        return AgendaItem(title, ItemKind.EVENT, title,
            date.atTime(LocalTime.of(h, m)).atZone(zone).toInstant(), allDay = allDay, colorHex = "#039BE5")
    }

    @Test fun groups_by_day_and_labels_today_tomorrow() {
        val sections = bucketer.bucket(listOf(event(0, 9, 0, "A"), event(1, 10, 0, "B"), event(2, 11, 0, "C")))
        assertEquals(3, sections.size)
        assertTrue(sections[0].header.startsWith("TODAY"))
        assertTrue(sections[0].isToday)
        assertTrue(sections[1].header.startsWith("TOMORROW"))
        // Day after tomorrow: weekday + day-of-month, no relative word.
        assertTrue(sections[2].header.startsWith("WED"))
    }

    @Test fun today_header_includes_weekday_and_day_of_month() {
        val sections = bucketer.bucket(listOf(event(0, 9, 0, "A")))
        assertEquals("TODAY · MON 1", sections[0].header)
    }

    @Test fun all_day_items_sort_before_timed_within_a_day() {
        val sections = bucketer.bucket(listOf(event(0, 9, 0, "timed"), event(0, 0, 0, "allday", allDay = true)))
        assertEquals("allday", sections[0].rows[0].title)
        assertEquals("All day", sections[0].rows[0].time)
        assertEquals("timed", sections[0].rows[1].title)
        assertEquals("9:00", sections[0].rows[1].time)
    }

    @Test fun tasks_render_as_task_rows_without_color() {
        val date = java.time.LocalDate.now(clock.withZone(zone))
        val task = AgendaItem("t", ItemKind.TASK, "Pay rent",
            date.atTime(LocalTime.of(17, 0)).atZone(zone).toInstant(), colorHex = null)
        val rows = bucketer.bucket(listOf(task))[0].rows
        assertTrue(rows[0].isTask)
        assertEquals(null, rows[0].colorHex)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*DayBucketerTest"`
Expected: FAIL — `DayBucketer` unresolved.

- [ ] **Step 3: Implement `DayBucketer.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain

import com.dynasty11.pinnedcalendar.domain.model.AgendaItem
import com.dynasty11.pinnedcalendar.domain.model.DaySection
import com.dynasty11.pinnedcalendar.domain.model.ItemKind
import com.dynasty11.pinnedcalendar.domain.model.NotificationRow
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class DayBucketer(
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val timeFmt = DateTimeFormatter.ofPattern("H:mm", Locale.getDefault())

    fun bucket(items: List<AgendaItem>): List<DaySection> {
        val today = LocalDate.now(clock.withZone(zone))
        return items.filter { it.start != null }
            .groupBy { LocalDate.ofInstant(it.start, zone) }
            .toSortedMap()
            .map { (date, dayItems) ->
                DaySection(
                    header = headerFor(date, today),
                    isToday = date == today,
                    rows = dayItems
                        .sortedWith(compareByDescending<AgendaItem> { it.allDay }.thenBy { it.start })
                        .map { toRow(it) },
                )
            }
    }

    private fun headerFor(date: LocalDate, today: LocalDate): String {
        val dow = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .uppercase(Locale.getDefault())
        val label = "$dow ${date.dayOfMonth}"
        return when (ChronoUnit.DAYS.between(today, date)) {
            0L -> "TODAY · $label"
            1L -> "TOMORROW · $label"
            else -> label
        }
    }

    private fun toRow(item: AgendaItem): NotificationRow {
        val time = when {
            item.allDay -> "All day"
            item.start != null -> timeFmt.format(item.start.atZone(zone))
            else -> ""
        }
        return NotificationRow(
            time = time,
            title = item.title,
            colorHex = item.colorHex,
            isTask = item.kind == ItemKind.TASK,
            completed = item.completed,
        )
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*DayBucketerTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add DayBucketer with relative day headers (TDD)"
```

---

## Task 4: NotificationContentBuilder (TDD — core logic)

Turns items + display settings into the renderable `NotificationContent`: hide-completed filter, day grouping (or flat), cap at `maxItems` with a "+N more" count, collapsed next-item line, empty state.

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/domain/NotificationContentBuilder.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/domain/NotificationContentBuilderTest.kt`

- [ ] **Step 1: Write the failing test `NotificationContentBuilderTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain

import com.dynasty11.pinnedcalendar.data.DisplaySettings
import com.dynasty11.pinnedcalendar.domain.model.AgendaItem
import com.dynasty11.pinnedcalendar.domain.model.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class NotificationContentBuilderTest {
    private val zone = ZoneId.of("America/New_York")
    private val clock = Clock.fixed(Instant.parse("2026-06-01T06:00:00Z"), zone)
    private val builder = NotificationContentBuilder(DayBucketer(clock, zone))

    private fun item(daysAhead: Long, h: Int, title: String, kind: ItemKind = ItemKind.EVENT, completed: Boolean = false): AgendaItem {
        val date = java.time.LocalDate.now(clock.withZone(zone)).plusDays(daysAhead)
        return AgendaItem(title, kind, title,
            date.atTime(LocalTime.of(h, 0)).atZone(zone).toInstant(),
            colorHex = if (kind == ItemKind.EVENT) "#039BE5" else null, completed = completed)
    }

    @Test fun empty_input_is_empty_state() {
        val c = builder.build(emptyList(), DisplaySettings())
        assertTrue(c.isEmpty)
        assertEquals(0, c.headerCount)
    }

    @Test fun collapsed_line_is_the_chronologically_next_item() {
        val c = builder.build(listOf(item(1, 10, "B"), item(0, 9, "A")), DisplaySettings())
        assertEquals("9:00 A", c.collapsedLine)
        assertFalse(c.isEmpty)
    }

    @Test fun caps_rows_at_maxItems_and_reports_more_count() {
        val items = (0 until 12).map { item(it.toLong() % 6, 8 + it % 6, "i$it") }
        val c = builder.build(items, DisplaySettings(maxItems = 5))
        val shown = c.sections.sumOf { it.rows.size }
        assertEquals(5, shown)
        assertEquals(7, c.moreCount)
        assertEquals(12, c.headerCount)
    }

    @Test fun hides_completed_tasks_when_enabled() {
        val items = listOf(item(0, 9, "open"), item(0, 10, "done", ItemKind.TASK, completed = true))
        val c = builder.build(items, DisplaySettings(hideCompletedTasks = true))
        assertEquals(1, c.headerCount)
        assertEquals("open", c.sections[0].rows[0].title)
    }

    @Test fun ungrouped_mode_produces_single_headerless_section() {
        val items = listOf(item(0, 9, "A"), item(1, 10, "B"))
        val c = builder.build(items, DisplaySettings(groupByDay = false))
        assertEquals(1, c.sections.size)
        assertEquals("", c.sections[0].header)
        assertEquals(2, c.sections[0].rows.size)
    }
}
```

- [ ] **Step 2: Create `DisplaySettings` (needed by the test) at `app/src/main/java/com/dynasty11/pinnedcalendar/data/DisplaySettings.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.data

data class DisplaySettings(
    val maxItems: Int = 8,
    val hideCompletedTasks: Boolean = true,
    val groupByDay: Boolean = true,
)
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NotificationContentBuilderTest"`
Expected: FAIL — `NotificationContentBuilder` unresolved.

- [ ] **Step 4: Implement `NotificationContentBuilder.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.domain

import com.dynasty11.pinnedcalendar.data.DisplaySettings
import com.dynasty11.pinnedcalendar.domain.model.AgendaItem
import com.dynasty11.pinnedcalendar.domain.model.DaySection
import com.dynasty11.pinnedcalendar.domain.model.NotificationContent
import com.dynasty11.pinnedcalendar.domain.model.NotificationRow

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
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NotificationContentBuilderTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add NotificationContentBuilder with cap/more/empty logic (TDD)"
```

---

## Task 5: SettingsRepository (master pin toggle via DataStore)

Persists the master "pin enabled" flag (and serves `DisplaySettings` defaults for now; sources/window come in later plans).

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/data/SettingsRepository.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/data/SettingsRepositoryTest.kt`

- [ ] **Step 1: Implement `SettingsRepository.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val pinEnabled: Flow<Boolean> = dataStore.data.map { it[PIN_ENABLED] ?: true }

    suspend fun isPinEnabled(): Boolean {
        var value = true
        dataStore.data.collectFirst { value = it[PIN_ENABLED] ?: true }
        return value
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { it[PIN_ENABLED] = enabled }
    }

    fun displaySettings(): DisplaySettings = DisplaySettings()

    private companion object {
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    }
}

// Small helper so we don't pull in `first()` ambiguity in tests.
private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.collectFirst(action: (T) -> Unit) {
    var taken = false
    collect { if (!taken) { taken = true; action(it) } }
}
```

> Note: the `collectFirst` helper exists only to read a single value without importing `kotlinx.coroutines.flow.first` (which collides in some setups). Prefer the `pinEnabled` Flow in production code; `isPinEnabled()` is for one-shot reads in receivers/workers.

- [ ] **Step 2: Write the failing test `SettingsRepositoryTest.kt`** (Robolectric for a real DataStore on a temp file)

```kotlin
package com.dynasty11.pinnedcalendar.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private fun newRepo(): SettingsRepository {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File.createTempFile("settings", ".preferences_pb", ctx.cacheDir)
        file.delete()
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
        return SettingsRepository(ds)
    }

    @Test fun defaults_to_enabled() = runTest {
        assertTrue(newRepo().isPinEnabled())
    }

    @Test fun persists_disabled_state() = runTest {
        val repo = newRepo()
        repo.setPinEnabled(false)
        assertEquals(false, repo.isPinEnabled())
    }
}
```

- [ ] **Step 3: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*SettingsRepositoryTest"`
Expected: PASS (2 tests). (Robolectric downloads its runtime on first run — allow time.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add SettingsRepository for master pin toggle (TDD)"
```

---

## Task 6: Notification channel + RemoteViews layouts + renderer + builder (Android)

Builds the actual ongoing notification from `NotificationContent` using a custom RemoteViews body wrapped in `DecoratedCustomViewStyle`.

**Files:**
- Create: `app/src/main/res/layout/notif_collapsed.xml`
- Create: `app/src/main/res/layout/notif_expanded.xml`
- Create: `app/src/main/res/layout/notif_day_header.xml`
- Create: `app/src/main/res/layout/notif_row.xml`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/notify/ChannelManager.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/notify/AccentResolver.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/notify/AgendaRemoteViewsRenderer.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/notify/AgendaNotificationBuilder.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/notify/AgendaNotificationBuilderTest.kt`

- [ ] **Step 1: `notif_collapsed.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:paddingStart="4dp" android:paddingEnd="8dp" android:paddingTop="2dp" android:paddingBottom="2dp">

    <View android:id="@+id/collapsed_dot"
        android:layout_width="4dp" android:layout_height="22dp"
        android:layout_marginEnd="10dp" android:background="#039BE5" />

    <TextView android:id="@+id/collapsed_line"
        android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
        android:maxLines="1" android:ellipsize="end" android:textSize="14sp"
        android:textColor="?android:attr/textColorPrimary" tools:ignore="HardcodedText" />

    <TextView android:id="@+id/collapsed_more"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_marginStart="8dp" android:textSize="12sp"
        android:textColor="?android:attr/textColorSecondary" />
</LinearLayout>
```
> Add `xmlns:tools="http://schemas.android.com/tools"` to the root if your linter requires it; otherwise drop the `tools:ignore`.

- [ ] **Step 2: `notif_day_header.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/day_header"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:paddingTop="8dp" android:paddingBottom="2dp"
    android:textSize="11sp" android:textStyle="bold" android:letterSpacing="0.06"
    android:textColor="?android:attr/textColorSecondary" />
```

- [ ] **Step 3: `notif_row.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:paddingTop="3dp" android:paddingBottom="3dp">

    <View android:id="@+id/row_bar"
        android:layout_width="4dp" android:layout_height="22dp"
        android:layout_marginEnd="10dp" android:background="#039BE5" />

    <TextView android:id="@+id/row_time"
        android:layout_width="52dp" android:layout_height="wrap_content"
        android:maxLines="1" android:textSize="12.5sp"
        android:textColor="?android:attr/textColorSecondary" />

    <TextView android:id="@+id/row_title"
        android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
        android:maxLines="1" android:ellipsize="end" android:textSize="14sp"
        android:textColor="?android:attr/textColorPrimary" />
</LinearLayout>
```

- [ ] **Step 4: `notif_expanded.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="4dp" android:paddingEnd="8dp" android:paddingTop="2dp" android:paddingBottom="6dp">

    <TextView android:id="@+id/expanded_title"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:textSize="13sp" android:textStyle="bold" android:paddingBottom="2dp" />

    <LinearLayout android:id="@+id/expanded_container"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" />

    <TextView android:id="@+id/expanded_more"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:paddingTop="6dp" android:textSize="13sp" android:textStyle="bold" />
</LinearLayout>
```

- [ ] **Step 5: `ChannelManager.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object ChannelManager {
    const val CHANNEL_ID = "pinned_agenda"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.dynasty11.pinnedcalendar.R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW, // silent, always in shade
            ).apply {
                description = context.getString(com.dynasty11.pinnedcalendar.R.string.channel_desc)
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
```

- [ ] **Step 6: `AccentResolver.kt`** (dynamic accent on API 31+, blue fallback)

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.ContextCompat

object AccentResolver {
    fun accentColor(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val res = if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_500
            return ContextCompat.getColor(context, res)
        }
        return 0xFF1A73E8.toInt()
    }
}
```

- [ ] **Step 7: `AgendaRemoteViewsRenderer.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.dynasty11.pinnedcalendar.R
import com.dynasty11.pinnedcalendar.domain.model.NotificationContent

class AgendaRemoteViewsRenderer(private val context: Context) {

    private val taskColor = 0xFF80868B.toInt()

    fun collapsed(content: NotificationContent): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_collapsed)
        if (content.isEmpty) {
            rv.setTextViewText(R.id.collapsed_line, "Nothing scheduled this week")
            rv.setViewVisibility(R.id.collapsed_dot, View.INVISIBLE)
            rv.setTextViewText(R.id.collapsed_more, "")
        } else {
            rv.setTextViewText(R.id.collapsed_line, content.collapsedLine)
            rv.setInt(R.id.collapsed_dot, "setBackgroundColor", parseColor(content.collapsedColorHex))
            rv.setTextViewText(R.id.collapsed_more, if (content.headerCount > 1) "+${content.headerCount - 1}" else "")
        }
        return rv
    }

    fun expanded(content: NotificationContent): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_expanded)
        rv.removeAllViews(R.id.expanded_container)

        if (content.isEmpty) {
            rv.setTextViewText(R.id.expanded_title, "This week")
            val row = RemoteViews(context.packageName, R.layout.notif_row)
            row.setViewVisibility(R.id.row_bar, View.INVISIBLE)
            row.setTextViewText(R.id.row_time, "")
            row.setTextViewText(R.id.row_title, "Nothing scheduled 🎉")
            rv.addView(R.id.expanded_container, row)
            rv.setTextViewText(R.id.expanded_more, "")
            return rv
        }

        rv.setTextViewText(R.id.expanded_title, "This week · ${content.headerCount}")
        for (section in content.sections) {
            if (section.header.isNotEmpty()) {
                val header = RemoteViews(context.packageName, R.layout.notif_day_header)
                header.setTextViewText(R.id.day_header, section.header)
                rv.addView(R.id.expanded_container, header)
            }
            for (r in section.rows) {
                val row = RemoteViews(context.packageName, R.layout.notif_row)
                if (r.isTask) {
                    row.setInt(R.id.row_bar, "setBackgroundColor", taskColor)
                } else {
                    row.setInt(R.id.row_bar, "setBackgroundColor", parseColor(r.colorHex))
                }
                row.setTextViewText(R.id.row_time, r.time)
                row.setTextViewText(R.id.row_title, r.title)
                rv.addView(R.id.expanded_container, row)
            }
        }
        rv.setTextViewText(R.id.expanded_more, if (content.moreCount > 0) "⌄ ${content.moreCount} more this week" else "")
        return rv
    }

    private fun parseColor(hex: String?): Int =
        try { if (hex != null) Color.parseColor(hex) else 0xFF1A73E8.toInt() } catch (_: Exception) { 0xFF1A73E8.toInt() }
}
```

- [ ] **Step 8: `AgendaNotificationBuilder.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dynasty11.pinnedcalendar.MainActivity
import com.dynasty11.pinnedcalendar.R
import com.dynasty11.pinnedcalendar.domain.model.NotificationContent

class AgendaNotificationBuilder(private val context: Context) {

    private val renderer = AgendaRemoteViewsRenderer(context)

    fun build(content: NotificationContent): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, SelfHealReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, ChannelManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar)
            .setColor(AccentResolver.accentColor(context))
            .setColorized(false)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(renderer.collapsed(content))
            .setCustomBigContentView(renderer.expanded(content))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .build()
    }
}
```

- [ ] **Step 9: Write Robolectric test `AgendaNotificationBuilderTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.dynasty11.pinnedcalendar.domain.model.DaySection
import com.dynasty11.pinnedcalendar.domain.model.NotificationContent
import com.dynasty11.pinnedcalendar.domain.model.NotificationRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AgendaNotificationBuilderTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun sampleContent() = NotificationContent(
        headerCount = 2,
        collapsedLine = "9:00 Standup",
        collapsedColorHex = "#039BE5",
        sections = listOf(
            DaySection("TODAY · MON 1", true, listOf(
                NotificationRow("9:00", "Standup", "#039BE5", false, false),
                NotificationRow("17:00", "Pay rent", null, true, false),
            )),
        ),
        moreCount = 0,
        isEmpty = false,
    )

    @Test fun builds_an_ongoing_notification_on_our_channel() {
        ChannelManager.ensureChannel(ctx)
        val n = AgendaNotificationBuilder(ctx).build(sampleContent())
        assertEquals(ChannelManager.CHANNEL_ID, n.channelId)
        assertTrue("expected FLAG_ONGOING_EVENT", (n.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue("expected a delete intent for self-heal", n.deleteIntent != null)
    }

    @Test fun empty_content_still_builds() {
        ChannelManager.ensureChannel(ctx)
        val empty = NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)
        val n = AgendaNotificationBuilder(ctx).build(empty)
        assertEquals(ChannelManager.CHANNEL_ID, n.channelId)
    }
}
```

- [ ] **Step 10: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "*AgendaNotificationBuilderTest"`
Expected: PASS (2 tests).

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: render agenda as ongoing RemoteViews notification (collapsed+expanded)"
```

---

## Task 7: NotificationPoster (gate + post/cancel)

Posts the notification only when the master toggle is on; cancels otherwise. Pure gate logic is unit-tested; posting is Robolectric-verified.

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/notify/NotificationPoster.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/notify/NotificationPosterTest.kt`

- [ ] **Step 1: Implement `NotificationPoster.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.dynasty11.pinnedcalendar.domain.model.NotificationContent

class NotificationPoster(private val context: Context) {

    private val builder = AgendaNotificationBuilder(context)

    /** Posts when [pinEnabled]; cancels when disabled. Returns true if a notification is now showing. */
    fun apply(pinEnabled: Boolean, content: NotificationContent): Boolean {
        val mgr = context.getSystemService<NotificationManager>() ?: return false
        return if (pinEnabled) {
            ChannelManager.ensureChannel(context)
            mgr.notify(ChannelManager.NOTIFICATION_ID, builder.build(content))
            true
        } else {
            mgr.cancel(ChannelManager.NOTIFICATION_ID)
            false
        }
    }
}
```

- [ ] **Step 2: Write Robolectric test `NotificationPosterTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.dynasty11.pinnedcalendar.domain.model.NotificationContent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationPosterTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)
    private val poster = NotificationPoster(ctx)
    private val content = NotificationContent(0, "", null, emptyList(), 0, isEmpty = true)

    @Test fun posts_when_enabled() {
        val showing = poster.apply(pinEnabled = true, content = content)
        assertEquals(true, showing)
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }

    @Test fun cancels_when_disabled() {
        poster.apply(pinEnabled = true, content = content)
        val showing = poster.apply(pinEnabled = false, content = content)
        assertEquals(false, showing)
        assertEquals(0, shadowOf(mgr).activeNotifications.size)
    }
}
```

- [ ] **Step 3: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "*NotificationPosterTest"`
Expected: PASS (2 tests).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add NotificationPoster gated on master pin toggle"
```

---

## Task 8: SelfHealReceiver (re-post on swipe)

When the user dismisses the notification, its `deleteIntent` fires this receiver, which re-posts from the latest sample content if the pin is still enabled.

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/notify/SelfHealReceiver.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/notify/SelfHealReceiverTest.kt`

(Manifest entry already added in Task 0, Step 10.)

- [ ] **Step 1: Implement `SelfHealReceiver.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dynasty11.pinnedcalendar.data.SettingsRepository
import com.dynasty11.pinnedcalendar.data.settingsDataStore
import com.dynasty11.pinnedcalendar.domain.DayBucketer
import com.dynasty11.pinnedcalendar.domain.NotificationContentBuilder
import com.dynasty11.pinnedcalendar.domain.SampleAgenda
import kotlinx.coroutines.runBlocking
import java.time.Clock

class SelfHealReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        try {
            runBlocking {
                val store: DataStore<Preferences> = context.applicationContext.settingsDataStore
                val settings = SettingsRepository(store)
                if (settings.isPinEnabled()) {
                    val clock = Clock.systemDefaultZone()
                    val builder = NotificationContentBuilder(DayBucketer(clock))
                    val content = builder.build(SampleAgenda.items(clock), settings.displaySettings())
                    NotificationPoster(context).apply(pinEnabled = true, content = content)
                }
            }
        } finally {
            pending.finish()
        }
    }
}
```

- [ ] **Step 2: Add the app-wide DataStore extension `app/src/main/java/com/dynasty11/pinnedcalendar/data/DataStore.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
```

- [ ] **Step 3: Write Robolectric test `SelfHealReceiverTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.notify

import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SelfHealReceiverTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)

    @Test fun reposts_notification_on_receive_when_enabled() {
        // Default pin state is enabled.
        SelfHealReceiver().onReceive(ctx, Intent())
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "*SelfHealReceiverTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add SelfHealReceiver that re-posts the pin on dismissal"
```

---

## Task 9: RefreshAgendaWorker (mock) + scheduler

WorkManager posts/refreshes the notification. A periodic worker (15 min) keeps it fresh; an expedited one-time request refreshes immediately on demand.

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/work/RefreshAgendaWorker.kt`
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/work/AgendaScheduler.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/work/RefreshAgendaWorkerTest.kt`

- [ ] **Step 1: Implement `RefreshAgendaWorker.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dynasty11.pinnedcalendar.data.SettingsRepository
import com.dynasty11.pinnedcalendar.data.settingsDataStore
import com.dynasty11.pinnedcalendar.domain.DayBucketer
import com.dynasty11.pinnedcalendar.domain.NotificationContentBuilder
import com.dynasty11.pinnedcalendar.domain.SampleAgenda
import com.dynasty11.pinnedcalendar.notify.NotificationPoster
import java.time.Clock

class RefreshAgendaWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext.settingsDataStore)
        val pinEnabled = settings.isPinEnabled()
        val clock = Clock.systemDefaultZone()
        val content = NotificationContentBuilder(DayBucketer(clock))
            .build(SampleAgenda.items(clock), settings.displaySettings())
        NotificationPoster(applicationContext).apply(pinEnabled, content)
        return Result.success()
    }
}
```

- [ ] **Step 2: Implement `AgendaScheduler.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AgendaScheduler {
    private const val PERIODIC = "agenda_refresh_periodic"
    private const val ONESHOT = "agenda_refresh_now"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshAgendaWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RefreshAgendaWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONESHOT, ExistingWorkPolicy.REPLACE, request)
    }
}
```

- [ ] **Step 3: Write the worker test `RefreshAgendaWorkerTest.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.work

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RefreshAgendaWorkerTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val mgr = ctx.getSystemService(NotificationManager::class.java)

    @Test fun worker_posts_notification_and_succeeds() = runTest {
        val worker = TestListenableWorkerBuilder<RefreshAgendaWorker>(ctx).build()
        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, shadowOf(mgr).activeNotifications.size)
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "*RefreshAgendaWorkerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add RefreshAgendaWorker (mock) + WorkManager scheduler"
```

---

## Task 10: BootReceiver

Re-establishes the periodic refresh and re-posts the pin after a reboot.

**Files:**
- Create: `app/src/main/java/com/dynasty11/pinnedcalendar/work/BootReceiver.kt`
- Test: `app/src/test/java/com/dynasty11/pinnedcalendar/work/BootReceiverTest.kt`

(Manifest entry already added in Task 0, Step 10.)

- [ ] **Step 1: Implement `BootReceiver.kt`**

```kotlin
package com.dynasty11.pinnedcalendar.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AgendaScheduler.schedulePeriodic(context)
            AgendaScheduler.refreshNow(context)
        }
    }
}
```

- [ ] **Step 2: Write the test `BootReceiverTest.kt`** (verifies periodic work is enqueued)

```kotlin
package com.dynasty11.pinnedcalendar.work

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun setUp() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config)
    }

    @Test fun boot_enqueues_periodic_refresh() {
        BootReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))
        val work = WorkManager.getInstance(ctx)
            .getWorkInfosForUniqueWork("agenda_refresh_periodic").get()
        assertEquals(1, work.size)
    }
}
```

- [ ] **Step 3: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "*BootReceiverTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: re-arm pin + periodic refresh on boot"
```

---

## Task 11: Wire App + minimal MainActivity toggle, then manual end-to-end

Initialize the channel + scheduling on app start, and give MainActivity a single M3 switch bound to the master pin toggle (full config screen is Plan 3).

**Files:**
- Modify: `app/src/main/java/com/dynasty11/pinnedcalendar/App.kt`
- Modify: `app/src/main/java/com/dynasty11/pinnedcalendar/MainActivity.kt`

- [ ] **Step 1: Replace `App.kt`**

```kotlin
package com.dynasty11.pinnedcalendar

import android.app.Application
import com.dynasty11.pinnedcalendar.notify.ChannelManager
import com.dynasty11.pinnedcalendar.work.AgendaScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ChannelManager.ensureChannel(this)
        AgendaScheduler.schedulePeriodic(this)
        AgendaScheduler.refreshNow(this)
    }
}
```

- [ ] **Step 2: Replace `MainActivity.kt`** (M3 screen + pin toggle + runtime POST_NOTIFICATIONS request)

```kotlin
package com.dynasty11.pinnedcalendar

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dynasty11.pinnedcalendar.data.SettingsRepository
import com.dynasty11.pinnedcalendar.data.settingsDataStore
import com.dynasty11.pinnedcalendar.ui.theme.PinnedCalendarTheme
import com.dynasty11.pinnedcalendar.work.AgendaScheduler
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val settings = SettingsRepository(applicationContext.settingsDataStore)

        setContent {
            PinnedCalendarTheme {
                val scope = rememberCoroutineScope()
                val pinEnabled by settings.pinEnabled.collectAsStateWithLifecycle(initialValue = true)

                Scaffold(
                    topBar = { TopAppBar(title = { Text("Pinned Calendar") }) },
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        ListItem(
                            headlineContent = { Text("Pin to notifications") },
                            supportingContent = { Text("Keep this week's agenda in the drawer") },
                            trailingContent = {
                                Switch(
                                    checked = pinEnabled,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            settings.setPinEnabled(checked)
                                            AgendaScheduler.refreshNow(this@MainActivity)
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
```

> If `collectAsStateWithLifecycle` is unresolved, add `implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")` to `app/build.gradle.kts` (catalog: reuse `lifecycle` version) and re-sync. Otherwise swap it for `settings.pinEnabled.collectAsState(initial = true)` and import `androidx.compose.runtime.collectAsState`.

- [ ] **Step 3: Build the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`; APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Run the full unit-test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: all tests PASS (SampleAgenda, DayBucketer, NotificationContentBuilder, SettingsRepository, AgendaNotificationBuilder, NotificationPoster, SelfHealReceiver, RefreshAgendaWorker, BootReceiver).

- [ ] **Step 5: Manual end-to-end on a device/emulator**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.dynasty11.pinnedcalendar/.MainActivity
```
Verify by observation:
1. A persistent notification appears: collapsed shows "9:00 Team standup · +7"; expand → day-grouped agenda with color bars + a task row + "⌄ N more".
2. **Swipe it away → it reappears within ~1s** (self-heal).
3. In-app: toggle **Pin to notifications** OFF → notification disappears; ON → it returns.
4. (API 31+) the accent (small icon tint / "more" text) follows the system/wallpaper color; event bars stay vivid.
5. Toggle dark mode (system) → notification + app switch to dark surfaces.

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "feat: wire app startup + master pin toggle; mock pinned agenda end-to-end"
```

---

## Self-Review (completed by plan author)

**Spec coverage (Plan 1 scope):**
- Persistent self-healing notification → Tasks 6–9 (builder, poster, self-heal, worker). ✔
- Rich agenda style B (color bars, day headers, tasks as distinct rows) → Tasks 3, 6 (renderer). ✔
- Collapsed = next item + count; expanded = grouped + "N more" → Task 4 + renderer. ✔
- Light/dark + dynamic color → Task 1 (theme) + Task 6 (`AccentResolver`, theme-attr colors in layouts). ✔
- Master pin on/off → Tasks 5, 7, 11. ✔
- No foreground service; WorkManager + boot re-arm → Tasks 9, 10. ✔
- Permissions limited to INTERNET/network/POST_NOTIFICATIONS/BOOT → Task 0 manifest. ✔
- **Deferred (correctly out of Plan 1):** real Google data + auth + window calc (Plan 2); full config screen with calendar/task-list/window/display controls (Plan 3). Tracked in Roadmap.

**Placeholder scan:** No "TBD/TODO/handle edge cases" — every code step has complete content. ✔

**Type consistency:** `NotificationContent`/`DaySection`/`NotificationRow` fields are defined in Task 2 and used identically in Tasks 4, 6, 7, 9 (`headerCount`, `collapsedLine`, `collapsedColorHex`, `sections`, `moreCount`, `isEmpty`; row `time/title/colorHex/isTask/completed`). `DisplaySettings(maxItems, hideCompletedTasks, groupByDay)` defined Task 4 Step 2, used in Tasks 5, 8, 9. `ChannelManager.CHANNEL_ID/NOTIFICATION_ID`, `AgendaScheduler` unique-work names consistent across Tasks 6–11. ✔

---

## Notes for Plan 2 / Plan 3 (do not build now)

- Plan 2 replaces `SampleAgenda` with `AgendaRepository` (Google Sign-In + Calendar/Tasks REST + cache) and adds `WindowCalculator`; `NotificationContentBuilder`/renderer/poster stay unchanged (they already consume domain models).
- Plan 3 replaces the single MainActivity switch with the full single-page config screen and wires each control to `SettingsRepository` + `AgendaScheduler.refreshNow`.
