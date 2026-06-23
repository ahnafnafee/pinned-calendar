// @ts-check
import { defineConfig } from 'astro/config';

// Pure static output → drop `dist/` straight onto Cloudflare Pages (no adapter).
// Zero client JS; small stylesheets get inlined automatically.
export default defineConfig({
  site: 'https://pinnedcalendar.ahnafnafee.dev',
  compressHTML: true,
  trailingSlash: 'always',
  prefetch: { defaultStrategy: 'hover', prefetchAll: true },
  build: { format: 'directory', inlineStylesheets: 'auto' },
});
