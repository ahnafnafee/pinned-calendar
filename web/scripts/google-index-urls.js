// Submit URLs to Google's Indexing API (urlNotifications.publish). Best-effort: if no service account is
// configured it skips cleanly — IndexNow + a fresh sitemap remain the load-bearing signals. Google formally
// only guarantees JobPosting/BroadcastEvent URLs here; ordinary pages are accepted in practice, not promised.
export async function publish(urls) {
  const raw = process.env.GOOGLE_SERVICE_ACCOUNT_JSON;
  if (!raw) {
    console.log('[google] no GOOGLE_SERVICE_ACCOUNT_JSON — skipping');
    return { ok: true, skipped: true };
  }
  if (!urls.length) return { ok: true, submitted: 0 };
  let creds;
  try {
    creds = JSON.parse(raw);
  } catch (e) {
    console.error('[google] GOOGLE_SERVICE_ACCOUNT_JSON is not valid JSON:', e.message);
    return { ok: true, skipped: true };
  }
  // Lazy import so IndexNow-only runs need no googleapis install.
  const { google } = await import('googleapis');
  const auth = new google.auth.GoogleAuth({
    credentials: { client_email: creds.client_email, private_key: creds.private_key },
    scopes: ['https://www.googleapis.com/auth/indexing'],
  });
  const indexing = google.indexing({ version: 'v3', auth });
  let submitted = 0;
  for (const url of urls) {
    try {
      const res = await indexing.urlNotifications.publish({ requestBody: { url, type: 'URL_UPDATED' } });
      console.log(`[google] ${res.status} ${url}`);
      submitted++;
    } catch (e) {
      // 403 = API disabled or the service account lacks permission. Don't fail the pipeline.
      console.error(`[google] FAILED ${url}: ${e.message}`);
    }
    await new Promise((r) => setTimeout(r, 500)); // gentle on the quota
  }
  return { ok: true, submitted };
}
