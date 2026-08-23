# Pinned Calendar

Pinned Calendar combines device calendar events with local to-dos and keeps the resulting agenda visible in a persistent notification.

## Language

**To-do**:
A user-created local item that may have a due time, notes, priority, and recurrence.
_Avoid_: Custom reminder, task reminder

**Reminder**:
A dismissible alert posted when an open, scheduled to-do reaches its due time. A reminder is a notification for a to-do occurrence, not the to-do itself.
_Avoid_: To-do, task

**Recurring to-do**:
A to-do whose completion advances it to another scheduled occurrence while retaining the same identity and details, until its recurrence rule ends.
_Avoid_: Repeating reminder

**Recurrence rule**:
The cadence, interval, optional weekly days, and optional ending that define a recurring to-do's schedule.
_Avoid_: Repeat settings, reminder schedule

**Occurrence**:
The single current due instance of a recurring to-do. Missed occurrences are not retained as history.

**Series anchor**:
The original scheduled date and time from which a recurring to-do's occurrences are calculated, preserving calendar intent across short months, leap years, and daylight-saving changes.

**Series end**:
An optional inclusive date or total scheduled-occurrence count after which a recurring to-do has no next occurrence. Missed occurrences count toward a count-based end.
