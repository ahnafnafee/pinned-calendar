// Orchestrator: fetch the live sitemap, then notify IndexNow (Bing/Yahoo/DuckDuckGo/Yandex/Seznam) and
// Google's Indexing API. Run from web/ in CI after the Cloudflare Pages deploy is live:
//   SITEMAP_MODE=true node scripts/notify-search-engines.js
// Or pass explicit URLs as args. GOOGLE_SERVICE_ACCOUNT_JSON is optional (Google step skips without it).
import { submit } from './indexnow-submit.js';
import { publish } from './google-index-urls.js';

const HOST = process.env.INDEXNOW_HOST || 'pinnedcalendar.ahnafnafee.dev';
const SITE = `https://${HOST}`;

async function sitemapUrls() {
  try {
    const res = await fetch(`${SITE}/sitemap.xml`);
    if (!res.ok) {
      console.error(`[sitemap] ${res.status} fetching ${SITE}/sitemap.xml`);
      return [];
    }
    const xml = await res.text();
    return [...xml.matchAll(/<loc>\s*([^<\s]+)\s*<\/loc>/g)].map((m) => m[1]);
  } catch (e) {
    console.error('[sitemap] fetch failed:', e.message);
    return [];
  }
}

async function main() {
  const urls = process.env.SITEMAP_MODE ? await sitemapUrls() : process.argv.slice(2);
  console.log(`[notify] ${urls.length} URL(s) from ${process.env.SITEMAP_MODE ? 'sitemap' : 'argv'}`);
  const indexnow = await submit(urls, { host: HOST });
  await publish(urls);
  if (!indexnow.ok) process.exitCode = 1;
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
