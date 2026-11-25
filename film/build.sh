#!/bin/sh
# Build the Pinned Calendar launch film: sh build.sh [draft]
# draft = 30 fps, quick. Without it: 60 fps, the release cut.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
LIB=${LIB:-$HOME/lemo-opuscar}
export LIB
[ -f "$LIB/core/render/video.mjs" ] || { echo "set LIB to the Lemo-Opuscar library folder"; exit 1; }
PY="$LIB/.venv/bin/python"; [ -x "$PY" ] || PY="$LIB/.venv/Scripts/python.exe"
FPS=60
OUT=pinned-calendar-launch.mp4
[ "$1" = draft ] && FPS=30 && OUT=out/draft.mp4
mkdir -p "$HERE/out"
"$PY" "$HERE/tools/prep.py"   # frames, font and icon are generated, not committed
cd "$LIB"
node core/render/events.mjs "$HERE"
"$PY" "$HERE/mix.py" "$LIB"
rm -f "$HERE/out/video.mp4"
node core/render/video.mjs "$HERE" --fps $FPS --workers 6 --out "$HERE/out/video.mp4"
sh core/render/mux.sh "$HERE/out/video.mp4" "$HERE/out/mix.wav" "$HERE/$OUT" $FPS 0
