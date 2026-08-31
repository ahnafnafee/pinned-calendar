# Release notes (Play "What's New")

Play "What's New" text lives in **`distribution/whatsnew/<version>/whatsnew-<LOCALE>`** —
one folder per released version, with one file for each supported Play locale. The
**Release** workflow auto-attaches every localized file when `publish_to_play` is checked,
so there is no manual paste.

Rules:
- **Max 500 characters** per language; **plain text** (no Markdown).
- Keep all 15 Play locales in sync: `ar`, `bn-BD`, `de-DE`, `en-US`, `es-ES`, `fr-FR`,
  `hi-IN`, `id`, `it-IT`, `ja-JP`, `ko-KR`, `pt-BR`, `tr-TR`, `vi`, and `zh-CN`.
- Add the file for a version **before** dispatching its Play release — the workflow
  preflight fails fast if any locale is missing or exceeds the limit.

Latest: [`distribution/whatsnew/3.3.0/`](../distribution/whatsnew/3.3.0/).
