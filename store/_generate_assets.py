#!/usr/bin/env python3
"""Generate Play Store assets from source captures and the app's launcher icon.

Re-runnable. Drop raw device captures (any size) in store/screenshots/raw/ and
run `python store/_generate_assets.py`. Produces, under store/:
  - play_icon_512.png                     512x512 app icon (24-bit, no alpha)
  - play_feature_graphic_1024x500.png     feature graphic
  - screenshots/screen_N_<feature>_<W>x<H>.png   Play-ready phone shots

Play rules enforced here: long side <= 2x short side, no alpha. Requires Pillow.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

REPO = Path(__file__).resolve().parents[1]
STORE = REPO / "store"
RAW = STORE / "screenshots" / "raw"
OUT = STORE / "screenshots"
FONTS = REPO / "app" / "src" / "main" / "res" / "font"

# Brand palette (from the launcher icon + app accent)
CREAM = (243, 239, 231)   # #F3EFE7 icon background
CHARCOAL = (42, 42, 42)   # #2A2A2A icon glyph
INDIGO_TOP = (70, 88, 142)    # #46588E
INDIGO_BOT = (96, 114, 172)   # #6072AC
PERIWINKLE = (200, 210, 240)

# Carousel order + feature label for each raw file. Add new entries as you
# capture more screens; anything not listed is appended alphabetically.
ORDER = [
    ("notification-light.png", "notification-light"),
    ("todos.png", "todos"),
    ("settings.png", "settings"),
    ("notification-dark.png", "notification-dark"),
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


def draw_glyph(d, ox, oy, size, fg, bg):
    """Calendar glyph from ic_launcher_foreground.xml (group scale 0.85)."""
    u = size / 108.0

    def t(x, y):
        x = 54 + (x - 54) * 0.85
        y = 54.5 + (y - 54.5) * 0.85
        return ox + x * u, oy + y * u

    def rr(x0, y0, x1, y1, r, fill):
        (X0, Y0), (X1, Y1) = t(x0, y0), t(x1, y1)
        d.rounded_rectangle([X0, Y0, X1, Y1], radius=r * 0.85 * u, fill=fill)

    rr(43, 29, 47, 41, 2, fg)
    rr(61, 29, 65, 41, 2, fg)
    rr(32, 36, 76, 80, 7, fg)
    rr(32, 50, 76, 53.5, 0, bg)
    for cy in (58, 67):
        for cx in (41, 51, 61):
            rr(cx, cy, cx + 6, cy + 5, 0, bg)


def make_icon():
    ss = 4
    n = 512 * ss
    img = Image.new("RGB", (n, n), CREAM)
    draw_glyph(ImageDraw.Draw(img), 0, 0, n, CHARCOAL, CREAM)
    img = img.resize((512, 512), Image.LANCZOS)
    img.save(STORE / "play_icon_512.png")
    print("icon  ->", "play_icon_512.png 512x512")


def pad_screenshots():
    OUT.mkdir(parents=True, exist_ok=True)
    files = {p.name: p for p in RAW.glob("*.png")} if RAW.exists() else {}
    ordered = [n for n, _ in ORDER if n in files]
    ordered += sorted(n for n in files if n not in dict(ORDER))
    labels = dict(ORDER)
    for i, name in enumerate(ordered, 1):
        im = Image.open(files[name]).convert("RGB")
        w, h = im.size
        lo, hi = min(w, h), max(w, h)
        if hi > 2 * lo:  # widen the short side until ratio <= ~1.98 (safe margin)
            need = int(hi / 1.98)
            if w < h:
                pad = need - w
                canvas = Image.new("RGB", (need, h))
                canvas.paste(im.crop((0, 0, 1, h)).resize((pad // 2 + 1, h)), (0, 0))
                canvas.paste(im.crop((w - 1, 0, w, h)).resize((need - pad // 2 - w, h)),
                             (pad // 2 + w, 0))
                canvas.paste(im, (pad // 2, 0))
            else:
                pad = need - h
                canvas = Image.new("RGB", (w, need))
                canvas.paste(im.crop((0, 0, w, 1)).resize((w, pad // 2 + 1)), (0, 0))
                canvas.paste(im.crop((0, h - 1, w, h)).resize((w, need - pad // 2 - h)),
                             (0, pad // 2 + h))
                canvas.paste(im, (0, pad // 2))
            im = canvas
        W, H = im.size
        label = labels.get(name, Path(name).stem)
        out = OUT / f"screen_{i}_{label}_{W}x{H}.png"
        im.save(out)
        print("shot  ->", out.name, f"ratio={max(W,H)/min(W,H):.3f}")


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


def make_feature():
    ss = 3
    W, H = 1024 * ss, 500 * ss
    # vertical indigo gradient
    col = Image.new("RGB", (1, H))
    for y in range(H):
        t = y / (H - 1)
        col.putpixel((0, y), tuple(int(a + (b - a) * t) for a, b in
                                   zip(INDIGO_TOP, INDIGO_BOT)))
    img = col.resize((W, H))

    # soft top-left sheen
    sheen = Image.new("L", (W, H), 0)
    ds = ImageDraw.Draw(sheen)
    ds.ellipse([-W * 0.2, -H * 0.6, W * 0.7, H * 0.6], fill=70)
    sheen = sheen.filter(ImageFilter.GaussianBlur(W * 0.06))
    img = Image.composite(Image.new("RGB", (W, H), (255, 255, 255)), img, sheen.point(lambda v: v))

    d = ImageDraw.Draw(img)

    # icon tile (cream squircle + glyph) with soft shadow
    T = 300 * ss
    tx0, ty0 = 72 * ss, (500 * ss - T) // 2
    rad = int(T * 0.235)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dsh = ImageDraw.Draw(shadow)
    dsh.rounded_rectangle([tx0, ty0 + 10 * ss, tx0 + T, ty0 + T + 10 * ss],
                          radius=rad, fill=(20, 26, 50, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(14 * ss))
    img.paste(shadow, (0, 0), shadow)
    d.rounded_rectangle([tx0, ty0, tx0 + T, ty0 + T], radius=rad, fill=CREAM)
    draw_glyph(d, tx0, ty0, T, CHARCOAL, CREAM)

    # text block
    x = tx0 + T + 60 * ss
    avail = W - x - 56 * ss
    title_f = fit_font(gsans, "Bold", "Pinned Calendar", avail, 96 * ss)
    tag_f = inter("Medium", 30 * ss)
    chip_f = inter("SemiBold", 23 * ss)
    tag_lines = wrap(d, "Your week's agenda, pinned to your notifications", tag_f, avail)
    chips = "Offline  ·  No sign-in  ·  Material You"

    th = title_f.getbbox("Pinned Calendar")
    title_h = th[3] - th[1]
    line_h = int(40 * ss)
    block_h = title_h + 22 * ss + len(tag_lines) * line_h + 30 * ss + int(33 * ss)
    y = (H - block_h) // 2

    d.text((x, y - th[1]), "Pinned Calendar", font=title_f, fill=(255, 255, 255))
    y += title_h + 22 * ss
    for ln in tag_lines:
        d.text((x, y), ln, font=tag_f, fill=(226, 231, 246))
        y += line_h
    y += 30 * ss
    d.text((x, y), chips, font=chip_f, fill=PERIWINKLE)

    img = img.resize((1024, 500), Image.LANCZOS)
    img.save(STORE / "play_feature_graphic_1024x500.png")
    print("feat  ->", "play_feature_graphic_1024x500.png 1024x500")


if __name__ == "__main__":
    make_icon()
    pad_screenshots()
    make_feature()
    print("done")
