# Release notes (Play "What's New")

Play "What's New" text lives in **`distribution/whatsnew/<version>/whatsnew-en-US`** —
one folder per released version. The **Release** workflow auto-attaches the file matching
the version being released when `publish_to_play` is checked, so there's no manual paste.

Rules:
- **Max 500 characters** per language; **plain text** (no Markdown).
- One `whatsnew-<LOCALE>` file per language, BCP 47 named (e.g. `whatsnew-en-US`).
- Add the file for a version **before** dispatching its Play release — the workflow
  preflight fails fast if it's missing.

Latest: [`distribution/whatsnew/2.0.2/whatsnew-en-US`](../distribution/whatsnew/2.0.2/whatsnew-en-US).
