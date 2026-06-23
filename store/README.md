# Play Store assets — Pinned Calendar

Everything needed to publish **Pinned Calendar** on Google Play, version
controlled so the live listing is reviewed in git (pattern borrowed from the
`player2` repo's `store/` folder, trimmed to what a solo app actually needs).

```
store/
├── README.md                                this file — publish guide + field map
├── play-listing.md                          title / short / full description (source of truth)
├── release-notes.md                         "What's New" per version
├── privacy-policy.md                        required by Play — host it, paste the URL
├── play_icon_512.png                        512×512 app icon (24-bit, no alpha)
├── play_feature_graphic_1024x500.png        feature graphic
├── _generate_assets.py                      re-runnable: pads screenshots, builds icon + feature graphic
└── screenshots/
    ├── raw/                                 drop full-res device captures here (any size)
    └── screen_N_<feature>_<W>x<H>.png       Play-ready phone shots (generated)
```

---

## Pre-publish checklist

**Graphics — done ✅**
- [x] App icon — `play_icon_512.png` (512×512, no alpha)
- [x] Feature graphic — `play_feature_graphic_1024x500.png` (1024×500)
- [x] Phone screenshots — 4 in `screenshots/` (1442×2856, ratio 1.98 ≤ 2:1, no alpha). Play needs **2–8**; 4 is fine, 5–6 is better (see shot list below).

**Text — done ✅** → `play-listing.md`, `release-notes.md`

**You still need to do in Play Console**
- [ ] Create the app (Productivity, Free, not primarily for children)
- [ ] Host `privacy-policy.md` and paste its URL (see below)
- [ ] Fill **Data safety** (answers below) and **Content rating** (answers below)
- [ ] Upload a signed **`.aab`** to a release track (build step below)
- [ ] Set **App access** = all functionality available without special access
- [ ] Declarations: contains ads = **No**, in-app purchases = **No**

---

## Where each asset goes in Play Console

| Play Console location | Asset / file |
|---|---|
| Store presence → Main store listing → App name | `play-listing.md` → App name |
| … → Short description | `play-listing.md` → Short description |
| … → Full description | `play-listing.md` → Full description |
| … → App icon | `play_icon_512.png` |
| … → Feature graphic | `play_feature_graphic_1024x500.png` |
| … → Phone screenshots | `screenshots/screen_*.png` |
| Store settings → App category | Productivity |
| Store settings → Contact details | email / website from `play-listing.md` |
| Policy → App content → Privacy policy | `https://pinnedcalendar.ahnafnafee.dev/privacy/` |
| Release → Production (or Testing) → release notes | `release-notes.md` (latest entry) |

### Privacy policy URL (required)
Primary: the marketing site's hosted page — `https://pinnedcalendar.ahnafnafee.dev/privacy/`
(source: `web/src/pages/privacy.astro`). Deploy the `web/` site first (see `web/README.md`),
then paste that URL. Before the site is live, the GitHub copy at
`https://github.com/ahnafnafee/pinned-calendar/blob/main/store/privacy-policy.md` works as a
fallback. There's a matching Terms page at `/terms/`.

---

## Data safety form (answers)

The app has **no INTERNET permission**, so this is the easy path:

- Does your app collect or share any of the required user data types? → **No**
- Is all user data encrypted in transit? → N/A (no data leaves the device)
- Do you provide a way to request data deletion? → N/A (nothing collected; uninstall removes local to-dos)
- Result: **"No data collected, no data shared."**

If Play asks about permissions, justify them as in `privacy-policy.md`: calendar
read is for on-device display only; nothing is transmitted.

## Content rating questionnaire (answers)

- Category: **Utility / Productivity / Communication** (Reference: a utility app)
- Violence, sexual content, profanity, controlled substances, gambling, user-generated content sharing, location sharing → **No** to all
- Expected result: **Everyone**

## App content declarations

| Declaration | Answer |
|---|---|
| Privacy policy | URL above |
| Ads | No |
| In-app purchases | No |
| App access (login required?) | No — all features available to everyone |
| Content rating | Everyone (from questionnaire) |
| Target audience | 13+ (not designed for children) |
| News app | No |
| COVID-19 / health | No |
| Data safety | No data collected or shared |
| Government app | No |

---

## Build the upload artifact (signed `.aab`)

Play wants an **Android App Bundle**, not an APK.

**Automated (recommended) — the Release workflow.** GitHub → Actions → **Release**
→ Run workflow, enter a semver `version` (e.g. `1.4.1`). It validates the version
(valid semver, not an existing tag, and strictly higher than the latest release),
derives a monotonic `versionCode` (`MAJOR*10000 + MINOR*100 + PATCH`), builds the
signed **`.aab`** (Play) and **`.apk`** (sideload), and publishes a `v<version>`
GitHub release with both attached. Download the `.aab` and upload it to Play.

Requires these repo secrets (Settings → Secrets and variables → Actions):
`RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD`. See `.github/workflows/release.yml`.

**Manual fallback.** With `keystore.properties` at the repo root (gitignored —
see `app/build.gradle.kts`):

```bash
./gradlew :app:bundleRelease   # → app/build/outputs/bundle/release/app-release.aab
```

Recommended either way: enroll in **Play App Signing** (Play holds the app signing
key; you keep an upload key).

---

## Screenshots — what you have, and what to grab

**Current set (already wired up, in carousel order):**
1. `notification-light` — the pin in the shade (the hook) ✅
2. `todos` — the To-dos tab + week-overview card ✅
3. `settings` — Notifications / time window / calendars ✅
4. `notification-dark` — the pin in dark mode ✅

That's a complete, valid listing. To make it stronger, grab **1–2 more** so the
carousel tells the whole story (Play allows up to 8):

| Priority | Grab this screen | Why it sells |
|---|---|---|
| ⭐ High | **Appearance settings** — Material You: seed color, palette style, AMOLED, font picker | Customization is a top selling point and isn't shown yet |
| ⭐ High | **Lock-screen view** of the pinned notification | Shows the agenda is glanceable without unlocking |
| Medium | **A second dynamic theme** (different wallpaper/accent) of the To-dos screen | Demonstrates Material You adapting to the user |
| Medium | **Expanded notification** (if it differs from collapsed) showing more days | Reinforces the "whole week" promise |
| Low | **Priority picker in context** (Top/Normal/Silent) or swipe-twice-to-remove | Niche; only if you want a 7th/8th |

### How to capture clean shots
- Same device/emulator for all, status bar tidy (full battery, no stray icons — the existing set uses a 9:30 / 100% bar; match it).
- Use the app's demo/sample data (the existing shots already do).
- Any resolution is fine — the generator pads to Play's 2:1 rule for you.

### To add them
1. Save each new capture as a PNG into `screenshots/raw/` (descriptive name, e.g. `appearance.png`).
2. Add it to the `ORDER` list in `_generate_assets.py` at the position you want.
3. Run `python store/_generate_assets.py` — it re-pads everything and rewrites the `screen_N_*` files.

---

## Regenerating all assets

```bash
python store/_generate_assets.py
```
Rebuilds the icon, feature graphic, and padded/renamed screenshots from
`screenshots/raw/` and the app's launcher-icon paths. Requires Pillow
(`pip install pillow`).

---

## Intentionally skipped (vs. the player2 reference)

player2 is a large team app; these were left out as overkill for a solo
open-source utility. Add them later if you ever need to:
- **Localized listings** (player2 ships 8 languages) — add `play-listing.<lang>.md` when you localize.
- **Tablet / other device screenshots** — optional for phone apps; add a `screenshots/tablet/` folder if you want the tablet slots filled.
- **ASO strategy / custom store pages / EAS config** — not relevant here (this is a native Gradle app, not Expo).
- **Direct push to Play** (Gradle Play Publisher / fastlane `supply` uploading the `.aab` + this metadata straight to a Play track) — not wired up. The Release workflow stops at building and attaching the signed `.aab`, which you upload to Play by hand. Going fully hands-off needs a Play Developer API service-account secret — and note the *first* release must be uploaded manually anyway, to create the app on Play.
