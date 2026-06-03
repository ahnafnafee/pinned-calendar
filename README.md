<div align="center">

# 📌 Pinned Calendar

### Your week, pinned to the notification shade — so it never gets swiped away.

**A privacy-first Android app that keeps this week's Google Calendar events *and* your to-dos in a persistent, self‑healing notification. 100% local — no sign-in, no internet permission, no tracking.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-3DDC84)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material You](https://img.shields.io/badge/Material%20You-Dynamic%20Color-FF6F61)](https://m3.material.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#-contributing)

[![Latest release](https://img.shields.io/github/v/release/ahnafnafee/pinned-calendar?sort=semver)](https://github.com/ahnafnafee/pinned-calendar/releases/latest)
[![GitHub stars](https://img.shields.io/github/stars/ahnafnafee/pinned-calendar?style=social)](https://github.com/ahnafnafee/pinned-calendar/stargazers)

</div>

---

## ✨ Why Pinned Calendar?

Calendar reminders are easy to swipe away by accident — and then you forget what's next.

**Pinned Calendar keeps a single, always-present notification at the top of your drawer** showing your whole week at a glance: events **and** tasks, grouped by day, colour‑coded by calendar. Swipe it away and it **instantly re-posts itself**. No more lost reminders, no widget hunting on a crowded home screen.

It reads the calendars already synced to your phone — **no Google sign-in, no OAuth, no `INTERNET` permission.** Your schedule never leaves the device.

> Think of it as a lightweight, private, always-on **agenda widget that lives in your notification shade.**

---

## 📸 Screenshots

<table>
  <tr>
    <td align="center"><b>Pinned agenda (light)</b></td>
    <td align="center"><b>Pinned agenda (dark)</b></td>
    <td align="center"><b>Material You settings</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/notification-light.png" width="240" alt="Persistent calendar notification showing the week's agenda in light mode" /></td>
    <td><img src="docs/screenshots/notification-dark.png" width="240" alt="Persistent calendar notification showing the week's agenda in dark mode" /></td>
    <td><img src="docs/screenshots/settings.png" width="240" alt="Material You settings screen with week overview card and pill chips" /></td>
  </tr>
</table>

---

## 🎯 Features

- 📌 **Persistent, self-healing notification** — an ongoing pin that re-posts itself the instant an accidental swipe dismisses it, with a **configurable priority** (Top keeps it above other notifications, silently).
- 📅 **Reads your device Google Calendar** via the system Calendar Provider — **no sign-in, no OAuth, no network.** Per-calendar colours included.
- ✅ **Local to-dos** — add quick tasks right in the app; they merge into the same pinned agenda.
- 🗂️ **Day-grouped agenda** — TODAY / TOMORROW / weekday sections, color-coded bars per calendar, tasks shown as distinct rows.
- 👆 **Tap to open** — tap any event row to jump straight to it in Google Calendar.
- 🎨 **Material 3 + Material You** — wallpaper-based dynamic colour, hand-picked seed colours, palette styles, **AMOLED black**, and selectable fonts.
- 🌗 **Light / dark / system** theme, plus a **theme- and accent-adaptive launcher icon**.
- 🪟 **Configurable window** — show the next 3 days, this week, 7 days, or 14 days; pick which calendars appear; cap the item count.
- 🔋 **Reliable in the background** — `WorkManager` refresh + a `ContentObserver` for instant updates, with an optional battery-optimization exemption for aggressive OEMs.
- ⚡ **No foreground service** — light on battery; the pin survives reboots.
- 🔒 **Private by design** — fully offline, minimal permissions, zero analytics, open source.

---

## 🔒 Privacy & Permissions

Pinned Calendar is **offline-first**. It has **no `INTERNET` permission** and sends nothing anywhere.

| Permission | Why it's needed |
|---|---|
| `READ_CALENDAR` | Read the events already synced on your device. |
| `POST_NOTIFICATIONS` | Show the pinned agenda (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Re-pin the agenda after a reboot. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | *Optional* — keep background refresh reliable on aggressive devices. |

No accounts. No ads. No trackers. No cloud.

---

## 🛠️ Tech Stack

| Area | Technology |
|---|---|
| Language | **Kotlin 2.2** |
| UI | **Jetpack Compose** · **Material 3** |
| Dynamic colour | [**MaterialKolor**](https://github.com/jordond/MaterialKolor) · Material You |
| Background work | **WorkManager** · `ContentObserver` |
| Storage | **DataStore (Preferences)** |
| Calendar | `CalendarContract` (Calendar Provider) |
| Notification | `NotificationCompat` · custom `RemoteViews` |
| Build | **AGP 9.2** · **Gradle 9.5** · `compileSdk 36` · `minSdk 26` |

---

## 📥 Download

Grab the latest signed APK from the **[Releases page](https://github.com/ahnafnafee/pinned-calendar/releases/latest)** and install it on your device (you may need to allow installs from unknown sources). No Play Store account required.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest), JDK 17+, Android SDK Platform 36.

### Build & run
```bash
git clone https://github.com/ahnafnafee/pinned-calendar.git
cd pinned-calendar
./gradlew :app:assembleDebug          # build a debug APK
./gradlew :app:installDebug           # install on a connected device/emulator
```
Or just open the project in Android Studio and hit **Run**.

On first launch, grant **Calendar** and **Notification** access, then toggle **Pin to notifications** on. (Optional: enable **Ignore battery optimizations** under *Reliability* for rock-solid background updates.)

---

## 🧩 How It Works

```
Device Calendar Provider ─┐
                          ├─► AgendaRepository ─► NotificationContentBuilder ─► ongoing notification
Local to-dos (DataStore) ─┘            ▲                                              │ deleteIntent
                                       │                                              ▼
              WorkManager (15-min refresh) + ContentObserver        SelfHealReceiver re-posts on swipe
```

A pure-Kotlin core (windowing, day-bucketing, content building) is unit-tested and decoupled from Android; the platform layer (RemoteViews, WorkManager, receivers) consumes it. No foreground service is used — the notification persists on its own and a delete-intent receiver makes it self-healing.

---

## ⚙️ Configuration

Everything is on one Material You settings screen:

- **Notifications** — pin on/off and **priority** (Top · Normal · Silent); Top keeps the pin above the everyday notification stream
- **Time window** — 3 days · this week · 7 days · 14 days
- **Calendars** — toggle each synced calendar on/off
- **Display** — group by day, hide completed to-dos, max items
- **Appearance** — theme, Material You, AMOLED, accent seed, palette style, font
- **Reliability** — battery-optimization exemption
- **To-dos** — add / complete / delete

---

## 🗺️ Roadmap

- [ ] Full colour-picker for custom seed colours
- [ ] Home-screen widget companion
- [ ] Per-to-do due-date picker
- [ ] Google Tasks integration (opt-in)
- [ ] Recurring-task support
- [ ] Wear OS tile

Have an idea? [Open an issue](https://github.com/ahnafnafee/pinned-calendar/issues)!

---

## 🤝 Contributing

Contributions are welcome! Fork the repo, create a feature branch, run `./gradlew :app:testDebugUnitTest`, and open a PR. For larger changes, please open an issue first to discuss.

---

## 📄 License

Released under the **MIT License** — see [`LICENSE`](LICENSE).

---

## 🙌 Acknowledgements

- [MaterialKolor](https://github.com/jordond/MaterialKolor) for seed-based Material You theming.
- The bundled fonts (Figtree, Outfit, Inter, Google Sans Flex) and the Material 3 design system.
- Design language inspired by modern Material 3 Expressive Android apps.

---

<div align="center">

**If Pinned Calendar keeps you on schedule, please ⭐ star the repo — it really helps!**

`android` · `kotlin` · `jetpack-compose` · `material-you` · `material3` · `google-calendar` · `notification` · `agenda` · `productivity` · `privacy` · `offline` · `foss`

</div>
