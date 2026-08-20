# Privacy Policy — Pinned Calendar

> Canonical, hosted version: <https://pinnedcalendar.ahnafnafee.dev/privacy/>
> (source: `web/src/pages/privacy.astro`). Keep the two in sync.

_Last updated: 2026-08-20_

Pinned Calendar (`dev.ahnafnafee.pinnedcalendar`) is an offline-first Android
app. This policy explains what it does — and does not — do with your data.

## The short version

**Pinned Calendar does not collect, receive, transmit, sell, or share your
personal data.** The app stores the to-dos and preferences needed to work
locally on your device. It has no `INTERNET` permission, and Android cloud
backup and device-to-device backup transfer are disabled, so the app does not
send this data off your device.

## What the app accesses, and why

- **Calendar (`READ_CALENDAR`)** — The app reads the events from the calendars
  already synced on your device so it can show them in the notification and in
  the app. Event contents are read on demand and are not copied into the app's
  to-do and settings storage. The identifiers of calendars you choose to hide
  are saved locally as a preference.
- **Notifications (`POST_NOTIFICATIONS`)** — Used only to show the pinned
  agenda notification.
- **Run at startup (`RECEIVE_BOOT_COMPLETED`)** — Used only to re-create the
  pinned notification after a reboot.
- **Ignore battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`,
  optional)** — If you grant it, this only helps the app refresh reliably in
  the background. It accesses no data.

## Data you create in the app

The app stores the following information in its private local app storage:

- To-dos, including their titles, due times, completion state, notes, and
  priority.
- App preferences, including notification, reminder, layout, appearance, time
  window, and calendar-filter choices.
- App-internal operational data needed to schedule and refresh local work.

This information is never uploaded or shared by the app.

## Local retention, backup, and deletion

On Android 10 and later, a compatible uninstall screen may offer a system
**Keep app data** option. If you select it, Android can retain the app's private
local data on that device and make it available after you reinstall the same
app. This is an optional same-device convenience, not a guaranteed backup. The
option is not available on Android 8 or 9 and may not be offered by every
device or uninstall method.

The app does not use Android cloud backup, and it excludes its data from
Android device-to-device backup transfer. Retained data is therefore not a way
to recover from a lost, reset, or damaged device.

You can permanently delete the app's locally stored data by clearing its
storage in Android settings. When uninstalling, the data is deleted unless you
explicitly choose **Keep app data** when that option is offered. To remove data
that was previously kept after uninstall, reinstall the same app, clear its
storage, and then uninstall it without keeping the data.

## No tracking

- No accounts or sign-in.
- No analytics, advertising, crash reporting, or third-party SDKs.
- No data is sold or shared with anyone.

## Children

The app collects no data from anyone, including children.

## Changes

If this policy changes, the updated version will be posted at this URL with a
new "Last updated" date.

## Contact

Questions? Open an issue at
<https://github.com/ahnafnafee/pinned-calendar/issues> or email
dynasty11studios@gmail.com.
