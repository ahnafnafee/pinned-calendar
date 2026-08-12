<div align="center">

# Pinned Calendar

Your week's Google Calendar events and to-dos in a single persistent Android notification.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-3DDC84)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/ahnafnafee/pinned-calendar?sort=semver)](https://github.com/ahnafnafee/pinned-calendar/releases/latest)
[![Website](https://img.shields.io/badge/Website-pinnedcalendar.ahnafnafee.dev-E07F2C?logo=cloudflare&logoColor=white)](https://pinnedcalendar.ahnafnafee.dev)

[**pinnedcalendar.ahnafnafee.dev**](https://pinnedcalendar.ahnafnafee.dev) · Coming soon to Google Play

</div>

---

Pinned Calendar is an open-source Android app that keeps this week's Google Calendar events and your to-dos in one ongoing notification at the top of the shade. It reads the calendars already synced on your device, so there is no Google sign-in, no OAuth, and no internet permission — your schedule never leaves the phone.

The pin is self-healing: swipe it away by accident and it re-posts itself. When you do want it gone, switch it off in the app or turn on swipe-twice-to-remove.

## Screenshots

<table>
  <tr>
    <td align="center"><b>Pinned agenda (light)</b></td>
    <td align="center"><b>Pinned agenda (dark)</b></td>
    <td align="center"><b>To-dos tab</b></td>
    <td align="center"><b>Settings tab</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/notification-light.png" width="200" alt="Persistent calendar notification showing the week's agenda in light mode" /></td>
    <td><img src="docs/screenshots/notification-dark.png" width="200" alt="Persistent calendar notification showing the week's agenda in dark mode" /></td>
    <td><img src="docs/screenshots/todos.png" width="200" alt="To-dos tab with the week overview card and task list" /></td>
    <td><img src="docs/screenshots/settings.png" width="200" alt="Settings tab with Material You cards for notifications, time window, and calendars" /></td>
  </tr>
</table>

## Features

- Persistent, self-healing notification: an ongoing pin that re-posts itself after an accidental swipe. Set its priority to Top, Normal, or Silent — Top keeps it above your everyday notifications without ever popping up or making a sound.
- Swipe twice to remove: an optional gesture to dismiss the pin for good without opening settings.
- Reads your device calendars through Android's Calendar Provider, with per-calendar colours. No sign-in, no OAuth, no network.
- Local to-dos: add quick tasks in the app and they merge into the same agenda. Tasks carry forward day to day until you complete or delete them.
- Day-grouped agenda: Today, Tomorrow, and weekday sections, color-coded per calendar, with tasks shown as their own rows.
- Tap an event row to open it directly in Google Calendar; tap anywhere else on the pin to jump into the app.
- Material 3 and Material You: wallpaper-based dynamic colour, seed colours, palette styles, AMOLED black, and selectable fonts.
- Light, dark, and system themes, plus a theme- and accent-adaptive launcher icon.
- Configurable window: the next 3 days, this week, 7 days, or 14 days. Choose which calendars appear and cap the item count.
- Background refresh with WorkManager and a ContentObserver for instant updates, plus an optional battery-optimization exemption for aggressive OEMs.
- No foreground service, so it stays light on battery; the pin restores itself after reboots and app updates.
- Offline by design: no analytics, no accounts, and minimal permissions.

## Privacy and permissions

Pinned Calendar is offline-first. It has no `INTERNET` permission and sends nothing off the device.

| Permission | Why it's needed |
|---|---|
| `READ_CALENDAR` | Read the events already synced on your device. |
| `POST_NOTIFICATIONS` | Show the pinned agenda (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Re-pin the agenda after a reboot. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional — keep background refresh reliable on aggressive devices. |

No accounts, ads, trackers, or cloud.

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose · Material 3 |
| Dynamic colour | [MaterialKolor](https://github.com/jordond/MaterialKolor) · Material You |
| Background work | WorkManager · `ContentObserver` |
| Storage | DataStore (Preferences) |
| Calendar | `CalendarContract` (Calendar Provider) |
| Notification | `NotificationCompat` · custom `RemoteViews` |
| Build | AGP 9.2 · Gradle 9.5 · `compileSdk 36` · `minSdk 26` |

## Download

**Coming soon to Google Play.** Until then, grab the latest signed APK from the [Releases page](https://github.com/ahnafnafee/pinned-calendar/releases/latest) and install it on your device (you may need to allow installs from unknown sources).

Learn more at the website: **[pinnedcalendar.ahnafnafee.dev](https://pinnedcalendar.ahnafnafee.dev)**.

## Building from source

Prerequisites: Android Studio (latest), JDK 17+, and Android SDK Platform 36.

```bash
git clone https://github.com/ahnafnafee/pinned-calendar.git
cd pinned-calendar
./gradlew :app:assembleDebug          # build a debug APK
./gradlew :app:installDebug           # install on a connected device/emulator
```

Or open the project in Android Studio and run it. On first launch, grant Calendar and Notification access, then turn on "Pin to notifications". For reliable background updates on aggressive OEMs, also enable "Ignore battery optimizations" under Reliability.

## How it works

```
Device Calendar Provider ─┐
                          ├─► AgendaRepository ─► NotificationContentBuilder ─► ongoing notification
Local to-dos (DataStore) ─┘            ▲                                              │ deleteIntent
                                       │                                              ▼
              WorkManager (15-min refresh) + ContentObserver        SelfHealReceiver re-posts on swipe
```

A pure-Kotlin core (windowing, day-bucketing, content building) is unit-tested and decoupled from Android, and the platform layer (RemoteViews, WorkManager, receivers) consumes it. There is no foreground service: the notification persists on its own, and a delete-intent receiver makes it self-healing.

## Configuration

The app is split into two tabs.

To-dos — the week at a glance:

- Week overview card — how many items are pinned, day by day.
- Add, complete, and delete local to-dos that merge into the pinned agenda.
- Tap a to-do to edit it: rename, schedule (today · tomorrow · next week · any date and time), set a priority flag that colors its bar in the pin, and attach notes.

Settings — everything else, Material You styled:

- Notifications — pin on/off, priority (Top · Normal · Silent), optional swipe-twice-to-remove, and opt-in to-do reminders: a normal, swipeable notification when a scheduled to-do comes due, leaving the pin untouched.
- Notification layout — a live preview of the pin, density presets (Compact · Cozy · Comfortable · Custom), rows shown before expanding, and the heading toggles.
- Time window — 3 days · this week · 7 days · 14 days.
- Calendars — toggle each synced calendar on or off.
- Display — group by day, hide completed to-dos, 24-hour time, and a max-items cap.
- Appearance — theme, Material You, AMOLED, accent seed, palette style, and font.
- Reliability — battery-optimization exemption.

## Roadmap

- [ ] Full colour-picker for custom seed colours
- [ ] Home-screen widget companion
- [x] Per-to-do due-date picker
- [ ] Google Tasks integration (opt-in)
- [ ] Recurring-task support
- [ ] Wear OS tile

Have an idea? [Open an issue](https://github.com/ahnafnafee/pinned-calendar/issues).

## Contributing

Contributions are welcome. Fork the repo, create a feature branch, run `./gradlew :app:testDebugUnitTest`, and open a pull request. For larger changes, open an issue first to discuss the approach.

## License

Released under the MIT License. See [`LICENSE`](LICENSE).

## Acknowledgements

- [MaterialKolor](https://github.com/jordond/MaterialKolor) for seed-based Material You theming.
- The bundled fonts (Figtree, Outfit, Inter, Google Sans Flex) and the Material 3 design system.
