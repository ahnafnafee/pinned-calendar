# Google Play listing — Pinned Calendar

The localized main-store-listing copy is version controlled under
[`store/listings/`](listings/). The default language is English (United States), sourced
from [`listings/en-US.md`](listings/en-US.md).

All 15 app languages have matching Play listings: Arabic, Bengali, English, French,
German, Hindi, Indonesian, Italian, Japanese, Korean, Portuguese, Simplified Chinese,
Spanish, Turkish, and Vietnamese. Their Play locale codes are `ar`, `bn-BD`, `de-DE`,
`en-US`, `es-ES`, `fr-FR`, `hi-IN`, `id`, `it-IT`, `ja-JP`, `ko-KR`, `pt-BR`, `tr-TR`,
`vi`, and `zh-CN`.

Each file uses the header-driven format consumed by [`_publish_listing.py`](_publish_listing.py):

```text
# App title

## Short description
One line, at most 80 characters

## Full description
Localized description, at most 4,000 characters
```

Run `python store/_publish_listing.py --validate` for a credential-free completeness
and length check. The **Play listing** workflow can publish all files in one atomic Play
edit by selecting `sync_listings`.

## Shared listing settings

| Field | Value |
|---|---|
| App category | Productivity |
| Tags | Calendar · To-Do Lists · Reminders · Day Planner · Productivity Tools (choose the closest predefined Play tags) |
| Store listing contact email | dynasty11studios@gmail.com |
| Website | https://github.com/ahnafnafee/pinned-calendar |
| Privacy policy URL | https://pinnedcalendar.ahnafnafee.dev/privacy/ |

Keep emoji out of the app title. Emoji section headers in the full descriptions are
intentional and supported by Play.
