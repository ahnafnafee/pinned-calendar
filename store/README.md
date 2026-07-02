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
├── play_icon_512.png                        512×512 app icon (from design/assets/icon-512.png)
├── play_feature_graphic_1024x500.png        feature graphic (dark + orange, matches the screenshots)
├── _generate_assets.py                      re-runnable: copies icon + screenshots from design/, builds feature graphic
└── screenshots/
    └── screen_N_<feature>_1080x1920.png     Play-ready phone shots (from design/assets/screenshot-N.png)
```

Image assets are authored in **`design/assets/`** (the orange pin icon + 5 captioned
screenshots, dark/orange theme); `_generate_assets.py` copies them into `store/` with
Play-ready names and builds the feature graphic (the one asset not in the design folder).

---

## Pre-publish checklist

**Graphics — done ✅**
- [x] App icon — `play_icon_512.png` (512×512, the orange pin icon from `design/assets/`)
- [x] Feature graphic — `play_feature_graphic_1024x500.png` (1024×500, dark + orange)
- [x] Phone screenshots — **5** in `screenshots/` (1080×1920, exact 9:16, 24-bit). Play needs 2–8.

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
| Release → … → release notes | Auto-attached from `distribution/whatsnew/<version>/` by the Release workflow (see `release-notes.md`) |

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
GitHub release with both attached. Check **publish_to_play** at dispatch to also upload the
`.aab` to Play's internal track (with release notes) automatically; otherwise download the
`.aab` and upload it by hand.

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

## Screenshots

Five polished, captioned screenshots (1080×1920, dark + orange theme), in carousel order:

1. `screen_1_agenda` — “Never miss what’s next” — the pin in the notification shade
2. `screen_2_week` — “Your week, pinned” — To-dos tab + week overview
3. `screen_3_controls` — “Pin it your way” — notifications / priority / time window
4. `screen_4_appearance` — “Make it yours” — Material You theming
5. `screen_5_privacy` — “Private by design” — offline / no sign-in / open source

### To change or add screenshots
1. Edit and re-export in the design tool to `design/assets/screenshot-N.png`.
2. Adjust the `SHOTS` list (order + feature labels) in `_generate_assets.py` if needed.
3. Run `python store/_generate_assets.py` — it rewrites `screenshots/screen_N_*` (and pads anything not already ≤ 2:1).

---

## Regenerating all assets

```bash
python store/_generate_assets.py
```
Copies the icon (`design/assets/icon-512.png`) and the 5 screenshots
(`design/assets/screenshot-N.png`) into `store/`, and builds the dark/orange
feature graphic. Requires Pillow (`pip install pillow`).

---

## Intentionally skipped (vs. the player2 reference)

player2 is a large team app; these were left out as overkill for a solo
open-source utility. Add them later if you ever need to:
- **Localized listings** (player2 ships 8 languages) — add `play-listing.<lang>.md` when you localize.
- **Tablet / other device screenshots** — optional for phone apps; add a `screenshots/tablet/` folder if you want the tablet slots filled.
- **ASO strategy / custom store pages / EAS config** — not relevant here (this is a native Gradle app, not Expo).
- **Full metadata sync to Play** (fastlane `supply` pushing listing text, screenshots, and graphics straight to Play) — not wired up. Direct `.aab` upload with release notes **is** wired (opt-in `publish_to_play` in the Release workflow, via a Play service-account secret); only the store-listing/graphics sync stays manual. Note the *first* release must be uploaded manually anyway, to create the app on Play.
