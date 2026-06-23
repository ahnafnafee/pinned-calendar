#!/usr/bin/env python3
"""Generate Play Store image assets from the design-folder source.

Re-runnable. The icon and screenshots are authored in the design tool and live
in design/assets/; this copies them into store/ with Play-ready names and builds
the feature graphic (which the design folder doesn't include) to match the
dark/orange screenshot theme. Produces, under store/:
  - play_icon_512.png                    (copied from design/icons/play-store-icon/play-store-icon-512.png)
  - play_feature_graphic_1024x500.png    (generated, dark + orange)
  - screenshots/screen_N_<feature>_1080x1920.png  (from design/assets/screenshot-N.png)
Requires Pillow.
"""
from pathlib import Path
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter

REPO = Path(__file__).resolve().parents[1]
STORE = REPO / "store"
DESIGN = REPO / "design" / "assets"
PACK = REPO / "design" / "icons" / "play-store-icon"  # canonical icon pack (latest logo)
OUT = STORE / "screenshots"
FONTS = REPO / "app" / "src" / "main" / "res" / "font"

# Dark/orange brand, sampled from the design screenshots + icon.
INK_TOP = (26, 20, 14)
INK_BOT = (10, 9, 8)
ORANGE = (240, 162, 78)
WHITE = (248, 246, 242)
MUTED = (176, 166, 152)

# Carousel order: design screenshot file -> feature label baked into the filename.
SHOTS = [
    ("screenshot-1.png", "agenda"),      # "Never miss what's next" — the pin in the shade
    ("screenshot-2.png", "week"),        # "Your week, pinned" — to-dos + week overview
    ("screenshot-3.png", "controls"),    # "Pin it your way" — settings / priority
    ("screenshot-4.png", "appearance"),  # "Make it yours" — Material You
    ("screenshot-5.png", "privacy"),     # "Private by design"
]


def gsans(weight, size):
    f = ImageFont.truetype(str(FONTS / "google_sans_flex.ttf"), size)
    try:
        f.set_variation_by_name(weight)
    except Exception:
        pass
    return f


def inter(weight, size):
    f = ImageFont.truetype(str(FONTS / "inter.ttf"), size)
    try:
        f.set_variation_by_name(weight)
    except Exception:
        pass
    return f


def fit_font(maker, weight, text, max_w, start, floor=24):
    s = start
    while s > floor:
        f = maker(weight, s)
        if f.getbbox(text)[2] <= max_w:
            return f
        s -= 2
    return maker(weight, floor)


def wrap(draw, text, font, max_w):
    words, lines, cur = text.split(), [], ""
    for wd in words:
        test = (cur + " " + wd).strip()
        if draw.textlength(test, font=font) <= max_w:
            cur = test
        else:
            lines.append(cur)
            cur = wd
    if cur:
        lines.append(cur)
    return lines


def make_icon():
    shutil.copyfile(PACK / "play-store-icon-512.png", STORE / "play_icon_512.png")
    print("icon  -> play_icon_512.png (from design/icons/play-store-icon/play-store-icon-512.png)")


def copy_shots():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("screen_*.png"):
        old.unlink()
    for i, (fname, label) in enumerate(SHOTS, 1):
        im = Image.open(DESIGN / fname).convert("RGB")
        w, h = im.size
        lo, hi = min(w, h), max(w, h)
        if hi > 2 * lo:  # Play rule: long side <= 2x short side (design shots are 9:16, so skipped)
            need = int(hi / 1.98)
            pad = need - w
            c = Image.new("RGB", (need, h))
            c.paste(im.crop((0, 0, 1, h)).resize((pad // 2 + 1, h)), (0, 0))
            c.paste(im.crop((w - 1, 0, w, h)).resize((need - pad // 2 - w, h)), (pad // 2 + w, 0))
            c.paste(im, (pad // 2, 0))
            im = c
        W, H = im.size
        out = OUT / f"screen_{i}_{label}_{W}x{H}.png"
        im.save(out)
        print("shot  ->", out.name)


def make_feature():
    ss = 3
    W, H = 1024 * ss, 500 * ss
    col = Image.new("RGB", (1, H))
    for y in range(H):
        t = y / (H - 1)
        col.putpixel((0, y), tuple(int(a + (b - a) * t) for a, b in zip(INK_TOP, INK_BOT)))
    img = col.resize((W, H))

    # warm orange glow, upper-left
    glow = Image.new("L", (W, H), 0)
    dg = ImageDraw.Draw(glow)
    dg.ellipse([-W * 0.12, -H * 0.5, W * 0.62, H * 0.5], fill=120)
    glow = glow.filter(ImageFilter.GaussianBlur(W * 0.08))
    img = Image.composite(Image.new("RGB", (W, H), (224, 120, 40)), img, glow.point(lambda v: int(v * 0.5)))

    d = ImageDraw.Draw(img)

    # icon tile from the design's rounded icon, with a soft shadow
    T = 300 * ss
    tx0, ty0 = 76 * ss, (500 * ss - T) // 2
    icon = Image.open(PACK / "icon-rounded-512.png").convert("RGBA").resize((T, T), Image.LANCZOS)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dsh = ImageDraw.Draw(shadow)
    dsh.rounded_rectangle([tx0, ty0 + 12 * ss, tx0 + T, ty0 + T + 12 * ss], radius=int(T * 0.235), fill=(0, 0, 0, 175))
    shadow = shadow.filter(ImageFilter.GaussianBlur(16 * ss))
    img.paste(shadow, (0, 0), shadow)
    img.paste(icon, (tx0, ty0), icon)

    # text block
    x = tx0 + T + 60 * ss
    avail = W - x - 56 * ss
    title_f = fit_font(gsans, "Bold", "Pinned Calendar", avail, 96 * ss)
    tag_f = inter("Medium", 30 * ss)
    chip_f = inter("SemiBold", 23 * ss)
    tag_lines = wrap(d, "Your week's agenda, pinned to your notifications", tag_f, avail)

    th = title_f.getbbox("Pinned Calendar")
    title_h = th[3] - th[1]
    line_h = int(40 * ss)
    block_h = title_h + 22 * ss + len(tag_lines) * line_h + 30 * ss + int(33 * ss)
    y = (H - block_h) // 2

    d.text((x, y - th[1]), "Pinned Calendar", font=title_f, fill=WHITE)
    y += title_h + 22 * ss
    for ln in tag_lines:
        d.text((x, y), ln, font=tag_f, fill=MUTED)
        y += line_h
    y += 30 * ss
    d.text((x, y), "Offline  ·  No sign-in  ·  Material You", font=chip_f, fill=ORANGE)

    img = img.resize((1024, 500), Image.LANCZOS)
    img.save(STORE / "play_feature_graphic_1024x500.png")
    print("feat  -> play_feature_graphic_1024x500.png (dark + orange)")


if __name__ == "__main__":
    make_icon()
    copy_shots()
    make_feature()
    print("done")
