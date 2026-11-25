"""Prepare the film's generated inputs: footage frames, the app font and the app icon.

None of these are committed: `python tools/prep.py` rebuilds frames/ from
media/shade-pull-640x1428.mp4 (a 30 fps emulator screen recording of the app) and copies
the font and icon from where the app and the store assets keep them, so the film never
holds a second copy that could drift.
"""
import shutil
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
REPO = HERE.parent
SRC = HERE / "media" / "shade-pull-640x1428.mp4"
OUT = HERE / "frames"

for src, dst in ((REPO / "app/src/main/res/font/google_sans_flex.ttf", HERE / "fonts/GoogleSansFlex.ttf"),
                 (REPO / "design/assets/icon-512.png", HERE / "assets/icon.png")):
    dst.parent.mkdir(exist_ok=True)
    shutil.copyfile(src, dst)

if OUT.exists():
    shutil.rmtree(OUT)
OUT.mkdir()
subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(SRC), "-vf", "fps=30,colorlevels=rimin=0.0627:gimin=0.0627:bimin=0.0627:rimax=0.9216:gimax=0.9216:bimax=0.9216", "-q:v", "2",
                str(OUT / "%04d.jpg")], check=True)
print(f"frames: {len(list(OUT.glob('*.jpg')))}")
