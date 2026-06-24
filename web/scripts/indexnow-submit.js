// Submit URLs to IndexNow — one POST to api.indexnow.org fans out to Bing, Yahoo, DuckDuckGo, Yandex,
// Seznam. No account or registration; the only requirement is the public key file at https://<host>/<key>.txt.
// Docs: https://www.indexnow.org/documentation
const DEFAULT_KEY = '8c45c09dcb914d3bb75c270e09b3bf49'; // matches web/public/<key>.txt
const DEFAULT_HOST = 'pinnedcalendar.ahnafnafee.dev';
const ENDPOINT = 'https://api.indexnow.org/IndexNow';

export async function submit(urls, { key, host } = {}) {
  const k = key || process.env.INDEXNOW_KEY || DEFAULT_KEY;
  const h = host || process.env.INDEXNOW_HOST || DEFAULT_HOST;
  if (!urls.length) {
    console.log('[indexnow] no URLs to submit');
    return { ok: true, submitted: 0 };
  }
  const res = await fetch(ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ host: h, key: k, keyLocation: `https://${h}/${k}.txt`, urlList: urls }),
  });
  // 200 = success, 202 = accepted/queued; 403 = key-file mismatch, 422 = URL host mismatch, 429 = rate limited.
  if (res.status === 200 || res.status === 202) {
    console.log(`[indexnow] ok ${res.status} — submitted ${urls.length} URL(s)`);
    return { ok: true, submitted: urls.length, status: res.status };
  }
  const text = await res.text().catch(() => '');
  console.error(`[indexnow] FAILED ${res.status} ${res.statusText}: ${text}`);
  return { ok: false, status: res.status, error: text };
}
