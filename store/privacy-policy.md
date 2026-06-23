# Privacy Policy — Pinned Calendar

> Canonical, hosted version: <https://pinnedcalendar.ahnafnafee.dev/privacy/>
> (source: `web/src/pages/privacy.astro`). Keep the two in sync.

_Last updated: 2026-06-23_

Pinned Calendar (`dev.ahnafnafee.pinnedcalendar`) is an offline-first Android
app. This policy explains what it does — and does not — do with your data.

## The short version

**Pinned Calendar does not collect, store, transmit, or share any personal
data.** It has no `INTERNET` permission, so it is technically incapable of
sending anything off your device.

## What the app accesses, and why

- **Calendar (`READ_CALENDAR`)** — The app reads the events from the calendars
  already synced on your device so it can show them in the notification and in
  the app. This data is read on demand, displayed locally, and never leaves
  your phone.
- **Notifications (`POST_NOTIFICATIONS`)** — Used only to show the pinned
  agenda notification.
- **Run at startup (`RECEIVE_BOOT_COMPLETED`)** — Used only to re-create the
  pinned notification after a reboot.
- **Ignore battery optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`,
  optional)** — If you grant it, this only helps the app refresh reliably in
  the background. It accesses no data.

## Data you create in the app

To-dos you add are stored **only on your device** (local app storage). They are
never uploaded or shared. Uninstalling the app removes them.

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
