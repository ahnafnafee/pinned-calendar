# Google Play listing — Pinned Calendar

Source of truth for the Play Console store listing. Edit here, then paste into
Play Console (Grow → Store presence → Main store listing). Keep this file in
sync with the live listing in the same PR.

Package: `dev.ahnafnafee.pinnedcalendar` · Default language: English (US)

---

## App name (max 30 — currently 29)

```
Pinned Calendar: To-Do Agenda
```

## Short description (max 80 — currently 79)

```
Your week's calendar & to-dos in one always-on agenda pin. No sign-in, offline.
```

## Full description (max 4000 — currently ~3.2k)

Emoji section headers are allowed in the Play description (not in the title).
Paste as plain text — Play does not render Markdown.

```
Tired of opening a calendar app just to remember what's next? Pinned Calendar keeps this week's Google Calendar events and your to-dos in a single ongoing notification at the top of your shade — so your next thing is always one glance away.

It reads the calendars already synced on your phone. There's no Google sign-in, no OAuth, and no internet permission at all. Your schedule never leaves the device.

📌 An always-on pin
The agenda lives in your notification shade and stays put. Swipe it away by accident and it quietly re-posts itself. When you really want it gone, switch it off in the app or turn on swipe-twice-to-remove. Set its priority — Top, Normal, or Silent — and Top keeps it above everyday notifications without ever popping up or making a sound.

🗓️ Your real calendars
Reads your device calendars through Android's Calendar Provider, color-coded per calendar. Pick which calendars appear and how far ahead to look: the next 3 days, this week, 7 days, or 14 days.

✅ Built-in to-dos
Add quick tasks right in the app and they merge into the same agenda. Add a due date and time, notes, or a priority flag. Repeat tasks with quick daily, weekly, monthly, and yearly presets—or customize the interval, weekdays, end date, or occurrence count. Unfinished tasks carry forward day to day, while completing a recurring task advances it to its next upcoming date. Optional alerting reminders let you know when scheduled tasks come due.

👀 At a glance
A day-grouped agenda — Today, Tomorrow, and weekday sections — with events and tasks as their own rows. Tap an event to open it in Google Calendar; tap anywhere else to jump into the app.

🎨 Material You
Wallpaper-based dynamic color, seed colors, palette styles, AMOLED black, and selectable fonts. Light, dark, and system themes, plus a theme- and accent-adaptive launcher icon.

🔋 Light on battery
No foreground service. Background refresh uses WorkManager with an instant content observer, and the pin restores itself after reboots and app updates. An optional battery-optimization exemption keeps refresh reliable on aggressive devices.

🔒 Private by design
• No INTERNET permission — nothing is ever sent off your phone
• No accounts, no ads, no trackers, no analytics
• Minimal permissions, all explained in the app

Permissions used:
• Calendar — read the events already synced on your device
• Notifications — show the pinned agenda (Android 13+)
• Run at startup — re-pin after a reboot
• Ignore battery optimizations (optional) — reliable background refresh

Open source. Code, releases, and issues: https://github.com/ahnafnafee/pinned-calendar

FAQ

Does it need internet or a Google sign-in?
No. The app has no INTERNET permission and never signs in. It reads the calendars already on your device.

Why did my pin come back after I swiped it?
That's by design — the pin is self-healing so an accidental swipe doesn't lose your agenda. Turn it off in the app, or enable swipe-twice-to-remove.

Does the pin drain my battery?
No. There's no foreground service; it refreshes in the background with WorkManager and updates instantly when your calendar changes.
```

---

## Listing settings (Play Console fields)

| Field | Value |
|---|---|
| App category | Productivity |
| Tags | **Pick up to 5 from Google's predefined list** (Store settings → App category → Tags — not free text): Calendar · To-Do Lists · Reminders · Day Planner · Productivity Tools (choose the closest matches the Console offers) |
| Store listing contact email | dynasty11studios@gmail.com *(edit if you prefer another)* |
| Website (optional) | https://github.com/ahnafnafee/pinned-calendar |
| Privacy policy URL | https://pinnedcalendar.ahnafnafee.dev/privacy/ *(once the marketing site is deployed; the GitHub copy at `store/privacy-policy.md` works as a pre-launch fallback)* |

> Title policy note: emoji are discouraged in the **app name** (policy risk) — keep it plain text. Emoji in the **description** are fine.
