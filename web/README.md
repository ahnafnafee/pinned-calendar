# Pinned Calendar — marketing site

Static [Astro](https://astro.build) site for **pinnedcalendar.ahnafnafee.dev**. Zero client JS,
vanilla CSS, deployed to Cloudflare Pages.

## Develop

```bash
cd web
npm install
npm run dev       # http://localhost:4321
npm run build     # → dist/  (what Pages serves)
npm run preview   # serve the built dist/ locally
```

Requires Node 22.

## Edit content

Swappable values live in **`src/config.mjs`** (app name, tagline, links, package id, contact).
Notably: flip `PLAY_AVAILABLE` to `true` once the app is live on Google Play — the download
buttons switch from “Download the APK” to “Get it on Google Play.”

```
src/
  config.mjs            ← links, store URLs, contact, the PLAY_AVAILABLE flag
  layouts/Base.astro    ← <head> SEO + JSON-LD, header/footer
  layouts/Legal.astro   ← wrapper + prose styles for privacy/terms
  components/            ← Header, Footer, StoreButtons
  pages/                ← index, privacy, terms, 404
  styles/site.css       ← the whole design system (light cream + indigo Material You)
public/                 ← icon, og-image, screenshots, _headers, robots.txt, manifest, sitemap
```

## Deploy — Cloudflare Pages

Auto-deploys on push to `main` (when `web/**` changes) via
`.github/workflows/deploy-web.yml`.

**One-time setup:**

1. In the Cloudflare dashboard, create a **Pages** project named `pinned-calendar`
   (Workers & Pages → Create → Pages → *Direct upload* is fine; the workflow uploads to it).
2. Add two repo secrets (GitHub → Settings → Secrets and variables → Actions):
   - `CLOUDFLARE_API_TOKEN` — a token with the **Cloudflare Pages: Edit** permission.
   - `CLOUDFLARE_ACCOUNT_ID` — your account ID (right sidebar of the dashboard).
3. Push to `main` (or run the workflow manually) — it builds and deploys.
4. In the Pages project → **Custom domains → Set up domain** → `pinnedcalendar.ahnafnafee.dev`.
   Cloudflare creates the DNS record for you. `public/_headers` ships security + cache headers
   automatically.

**Alternative (no Action, no secrets): Cloudflare Git integration.** Connect the repo in the
Pages dashboard with Root directory `web`, Build command `npm run build`, Output directory `dist`.
If you use this, you can delete `deploy-web.yml`.
