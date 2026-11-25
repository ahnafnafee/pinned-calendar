#!/bin/sh
# Render the link-preview images from preview.html: sh design/previews/render.sh
#   web/public/og-image.png               1200x630, the website's Open Graph image
#   design/assets/github-social-preview.png  1280x640, upload in the repository's Settings > Social preview
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$HERE/../.." && pwd)
if command -v cygpath >/dev/null 2>&1; then URL="file:///$(cygpath -m "$HERE/preview.html")"; else URL="file://$HERE/preview.html"; fi
shot() { npx -y playwright screenshot --viewport-size="$1,$2" --wait-for-timeout=1200 "$URL" "$3"; }
shot 1200 630 "$REPO/web/public/og-image.png"
shot 1280 640 "$REPO/design/assets/github-social-preview.png"
