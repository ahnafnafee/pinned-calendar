import { SITE } from '../config.mjs';

// lastmod = build date, so every deploy refreshes it.
const LASTMOD = new Date().toISOString().slice(0, 10);

const pages = [
  { loc: '/', changefreq: 'weekly', priority: '1.0', img: '/og-image.png' },
  { loc: '/privacy/', changefreq: 'yearly', priority: '0.3' },
  { loc: '/terms/', changefreq: 'yearly', priority: '0.3' },
];

export function GET() {
  const urls = pages
    .map(
      (p) =>
        `  <url>\n    <loc>${SITE}${p.loc}</loc>\n    <lastmod>${LASTMOD}</lastmod>\n    <changefreq>${p.changefreq}</changefreq>\n    <priority>${p.priority}</priority>${
          p.img ? `\n    <image:image><image:loc>${SITE}${p.img}</image:loc></image:image>` : ''
        }\n  </url>`
    )
    .join('\n');
  const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">\n${urls}\n</urlset>\n`;
  return new Response(xml, { headers: { 'Content-Type': 'application/xml; charset=utf-8' } });
}
