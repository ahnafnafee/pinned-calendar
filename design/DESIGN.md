# Pinned Calendar — Design System

The brand in one line: **your week, pinned where you'll actually see it.** Warm, confident, utilitarian. A friendly orange over calm dark surfaces in the app; cream paper, ink, and the same orange for the marketing site.

There are two contexts. Keep them distinct:

- **App theme** (Android, Material 3 / Material You) → dark surfaces + orange accent. Use the tables in §2.
- **Marketing / web** (landing page, store) → cream paper + ink + orange. §3.

---

## 1. Brand color

| Token | Hex | Use |
|---|---|---|
| Orange / Primary | `#E07F2C` | The brand. Seed color, key accents. |
| Orange Light | `#F4A94E` | Accent on dark surfaces (buttons, toggles, links in the app). |
| Orange Bright | `#F8BC63` | Top of the icon gradient, highlights. |
| Orange Deep | `#A85B16` | Orange on light backgrounds (text/links on cream). |
| Ink | `#211C16` | Near-black warm text, borders, dark buttons. |

The app icon gradient runs `#F8BC63` → `#E07F2C` (diagonal, top-left to bottom-right).

---

## 2. App theme (set this as the default)

The app is Material You. When "Material You" (wallpaper colors) is **off**, default the accent to the brand orange. Recommended **seed color: `#E07F2C`**. If you hand-tune the schemes instead of generating from the seed, these roles match the screenshots:

### Dark scheme (default)
| Material role | Hex |
|---|---|
| primary | `#F4A94E` |
| onPrimary | `#2A1A02` |
| primaryContainer | `#5A3D10` |
| onPrimaryContainer | `#FFD9A6` |
| background | `#0B0D10` |
| surface | `#0E1116` |
| surfaceContainer | `#1A1F25` |
| surfaceContainerHigh | `#222831` |
| onSurface | `#E6E2DB` |
| onSurfaceVariant | `#9AA1A9` |
| outline | `#3A4048` |

AMOLED-black variant: set `background` and `surface` to `#000000`, keep containers as above.

### Light scheme
| Material role | Hex |
|---|---|
| primary | `#B45A12` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#FFDDB3` |
| onPrimaryContainer | `#2E1500` |
| background | `#FCFAF5` |
| surface | `#FFFFFF` |
| surfaceContainer | `#F2ECE1` |
| onSurface | `#211C16` |
| onSurfaceVariant | `#6B5F4D` |
| outline | `#C7BCA8` |

### Event / category accents (the colored bars in the agenda)
| Meaning | Hex |
|---|---|
| Neutral / to-do | `#6B7280` |
| Work (blue) | `#5B7CFF` |
| Personal (green) | `#4CAF6E` |

Where to change it in an Android project:
- Compose: your `darkColorScheme(...)` / `lightColorScheme(...)`, or the seed passed to your dynamic-color fallback.
- Views/XML: `res/values/colors.xml` + `themes.xml` (`colorPrimary`, `colorSurface`, etc.).

---

## 3. Marketing / web palette

| Token | Hex |
|---|---|
| Paper (bg) | `#EFE6D3` |
| Ink (text/borders) | `#211C16` |
| Body text | `#3A3328` / muted `#5C5240` |
| Orange (fills, accent block) | `#DD6E1E` |
| Orange deep (links on cream) | `#A85B16` |

Signature treatments: 1.5px solid ink borders, hard offset shadows (`box-shadow: 4–10px 0 #211C16` or orange), a subtle multiply grain overlay, and a hand-tilted orange marker highlight behind a key word.

---

## 4. Typography

| Where | Font | Notes |
|---|---|---|
| App UI | Rounded geometric sans (the app's current face, e.g. Google Sans Rounded style). Open-source match: **Nunito** or **Figtree**. | Keep it friendly and rounded. |
| Marketing display | **Bricolage Grotesque** (700–800) | Big, tight headlines, `letter-spacing: -2px`. |
| Labels / times / kickers | **Space Mono** (700) | Uppercase, `letter-spacing: 1.5–2px`. Reinforces the schedule feel. |

Weights: headings 700–800, body 500, labels 700.

---

## 5. Iconography & shape

- App icon: a white calendar card with two charcoal binder tabs, three agenda rows (top row orange = "this week"), and a charcoal push-pin with an orange center pinning the top-right corner, on the orange gradient. Files in `play-store-icon/`.
- Line icons: 1.8–2px stroke, rounded caps and joins.
- Radii: cards ~24–28dp, pills fully round, buttons ~14–16px on web.

---

## 6. Motion

Restrained and physical. One orchestrated load (elements rise + the phone "drops and settles" on its pin), then near-still: a slow sway, a soft pulsing live dot. Easing `cubic-bezier(.2,.7,.2,1)`; the pin uses a slight overshoot `cubic-bezier(.3,1.3,.5,1)`. Avoid floaty glows and gratuitous micro-animations.
