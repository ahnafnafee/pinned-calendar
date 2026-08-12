#!/usr/bin/env python3
"""Compose the five store screenshots from raw emulator captures.

Re-runnable. Reads full-screen captures from design/assets/captures/ and writes
design/assets/screenshot-N.png; store/_generate_assets.py then copies those into
store/screenshots/ with Play-ready names. The layout mirrors the website's light
theme (web/src/styles/site.css): paper background, ink text, orange accents.
Requires Pillow.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

REPO = Path(__file__).resolve().parents[1]
ASSETS = REPO / "design" / "assets"
CAPS = ASSETS / "captures"
FONTS = REPO / "app" / "src" / "main" / "res" / "font"

W, H = 1080, 1920

# Website palette (web/src/styles/site.css :root).
PAPER = (239, 230, 211)      # --paper #efe6d3
INK = (33, 28, 22)           # --ink #211c16
MUTED = (92, 82, 64)         # --muted #5c5240
ORANGE = (221, 110, 30)      # --orange #dd6e1e
ORANGE_MARK = (232, 132, 58) # --orange-mark #e8843a
PILL_BG = (246, 239, 224)
PHONE_BODY = (12, 14, 18)    # website .phone background #0c0e12


def gsans(weight, size):
    f = ImageFont.truetype(str(FONTS / "google_sans_flex.ttf"), size)
    try:
        f.set_variation_by_name(weight)
    except Exception:
        pass
    return f


def text_w(draw, s, font):
    left, _, right, _ = draw.textbbox((0, 0), s, font=font)
    return right - left


def wrap(draw, s, font, max_w):
    lines, line = [], ""
    for word in s.split():
        probe = f"{line} {word}".strip()
        if text_w(draw, probe, font) <= max_w:
            line = probe
        else:
            lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines


def headline(draw, y, lines, accent_words):
    font = gsans("Bold", 104)
    for line in lines:
        words = line.split()
        widths = [text_w(draw, w + " ", font) for w in words]
        total = sum(widths) - text_w(draw, " ", font)
        x = (W - total) // 2
        for word, w in zip(words, widths):
            color = ORANGE if word.strip(",") in accent_words else INK
            draw.text((x, y), word, font=font, fill=color)
            x += w
        y += 122
    return y


def subhead(draw, y, s):
    font = gsans("Regular", 44)
    for line in wrap(draw, s, font, 860):
        draw.text((W // 2 - text_w(draw, line, font) // 2, y), line, font=font, fill=MUTED)
        y += 60
    return y


def badge(draw, y, s):
    font = gsans("Bold", 34)
    tw = text_w(draw, s, font)
    pad_x, pad_y = 34, 16
    x0 = W // 2 - tw // 2 - pad_x
    x1 = W // 2 + tw // 2 + pad_x
    draw.rounded_rectangle((x0, y, x1, y + 34 + 2 * pad_y), radius=40, fill=ORANGE)
    draw.text((W // 2 - tw // 2, y + pad_y - 2), s, font=font, fill=(26, 18, 6))
    return y + 34 + 2 * pad_y


def phone(canvas, capture_name, top, screen_w=560):
    cap = Image.open(CAPS / capture_name).convert("RGB")
    screen_h = round(screen_w * cap.height / cap.width)
    cap = cap.resize((screen_w, screen_h), Image.LANCZOS)

    bezel = 22
    fw, fh = screen_w + 2 * bezel, screen_h + 2 * bezel
    fx = (W - fw) // 2

    # Soft shadow, echoing the website's phone drop shadow.
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (fx + 14, top + 24, fx + fw + 14, top + fh + 24), radius=70, fill=(33, 28, 22, 90),
    )
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(18)))

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((fx, top, fx + fw, top + fh), radius=70, fill=PHONE_BODY, outline=INK, width=3)

    mask = Image.new("L", cap.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen_w, screen_h), radius=48, fill=255)
    canvas.paste(cap, (fx + bezel, top + bezel), mask)


def base():
    return Image.new("RGBA", (W, H), PAPER + (255,))


def save(img, n):
    out = ASSETS / f"screenshot-{n}.png"
    img.convert("RGB").save(out)
    print("wrote", out)


def slide(n, capture, badge_text, head_lines, accents, sub):
    img = base()
    draw = ImageDraw.Draw(img)
    y = 96
    if badge_text:
        y = badge(draw, y, badge_text) + 26
    y = headline(draw, y, head_lines, accents) + 18
    y = subhead(draw, y, sub) + 34
    phone(img, capture, y)
    save(img, n)


def privacy_slide():
    img = base()
    draw = ImageDraw.Draw(img)

    icon = Image.open(ASSETS / "icon-rounded-512.png").convert("RGBA").resize((230, 230), Image.LANCZOS)
    img.alpha_composite(icon, ((W - 230) // 2, 300))

    y = headline(draw, 600, ["Private", "by design"], {"by", "design"}) + 18
    y = subhead(draw, y, "No account. No cloud. Your calendar and to-dos never leave your phone.") + 60

    font = gsans("Medium", 40)
    for line in [
        "100% on-device, no sign-in",
        "Reads your calendar, sends nothing",
        "Free & open source",
    ]:
        tw = text_w(draw, line, font)
        box_w = tw + 160
        x0 = (W - box_w) // 2
        draw.rounded_rectangle((x0, y, x0 + box_w, y + 104), radius=32, fill=PILL_BG, outline=INK, width=2)
        cx, cy = x0 + 52, y + 52
        draw.ellipse((cx - 22, cy - 22, cx + 22, cy + 22), fill=ORANGE_MARK)
        # The bundled fonts have no check glyph, so draw the mark directly.
        draw.line([(cx - 10, cy + 1), (cx - 3, cy + 9)], fill=(26, 18, 6), width=5)
        draw.line([(cx - 3, cy + 9), (cx + 11, cy - 8)], fill=(26, 18, 6), width=5)
        draw.text((cx + 40, y + 28), line, font=font, fill=INK)
        y += 136
    save(img, 5)


slide(
    1, "cap1_shade.png", "ALWAYS-ON AGENDA",
    ["Never miss", "what's next"], {"next"},
    "Your whole week lives in the notification shade, one swipe away, every time you wake your phone.",
)
slide(
    2, "cap2_todos.png", None,
    ["Your week,", "pinned"], {"pinned"},
    "Events and to-dos with priorities and schedules, grouped and ready the moment you unlock.",
)
slide(
    3, "cap3_layout.png", None,
    ["Pin it", "your way"], {"your", "way"},
    "Density presets, a live preview, priority, and time window, tuned from one screen.",
)
slide(
    4, "cap4_appearance.png", None,
    ["Make it", "yours"], {"yours"},
    "Material You theming, six accent colors, and light or dark, tuned to match your phone.",
)
privacy_slide()
