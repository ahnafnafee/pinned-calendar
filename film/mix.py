"""The score and the sound of the Pinned Calendar launch film: out/mix.wav.

Usage: python mix.py <path of the Lemo-Opuscar library>

Reads the same cue file as the picture (cues.json) and the sound events the picture
exports (events.json), so every note and every hit sits on the frame it belongs to.
Everything is synthesized here: no samples, no downloads.
"""
import json
import sys
from pathlib import Path

import numpy as np
import soundfile as sf

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(Path(sys.argv[1]) / "core" / "audio"))
import sfx  # noqa: E402
from sfx import SR, add, bp, hp, lp, noise, norm, t_  # noqa: E402

CUES = json.loads((HERE / "cues.json").read_text(encoding="utf-8"))
EV = json.loads((HERE / "events.json").read_text(encoding="utf-8"))["ev"]
BEAT = 60 / CUES["bpm"]
BAR = 4 * BEAT
DUR = CUES["bars"] * BAR
SIL0, SIL1 = (x * BAR for x in CUES["silence"])
TURN_BAR, TURN_CHORD = CUES["turn"]
N = int(DUR * SR) + SR


def hz(m):
    return 440 * 2 ** ((m - 69) / 12)


def B(bar, beat=0.0):
    return (bar * 4 + beat) * BEAT


TURN = B(TURN_BAR)
LOGO = B([s for s in CUES["scenes"] if s["id"] == "logo"][0]["bar"])


def kick_on(t):
    return any(B(a) - 1e-6 <= t < B(b) - 1e-6 for a, b in CUES["kick"])


def chord_at(t):
    s = [s for s in CUES["scenes"] if B(s["bar"]) <= t + 1e-6][-1]
    if TURN <= t < LOGO:
        return TURN_CHORD
    return s["chord"]


drums, bass, music, fxs, bed = (np.zeros((N, 2)) for _ in range(5))


def adsr(d, a=0.005, r=0.05):
    e = np.ones(len(t_(d)))
    na, nr = max(1, int(a * SR)), max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def kick():
    d = 0.3
    tt = t_(d)
    f = 50 + 110 * np.exp(-tt / 0.03)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / 0.11)
    return body + hp(noise(d), 2500) * np.exp(-tt / 0.003) * 0.18


def clap():
    d = 0.2
    tt = t_(d)
    out = np.zeros(len(tt))
    for off in (0.0, 0.010, 0.021):
        out += bp(noise(d), 1000, 4200) * np.exp(-np.maximum(tt - off, 0) / 0.045) * (tt >= off)
    return norm(out)


def shaker(d=0.07):
    tt = t_(d)
    return norm(hp(noise(d), 6000) * np.sin(np.pi * np.minimum(tt / d, 1)) ** 2)


def marimba(m, d=0.6, bright=1.0):
    """A wooden bar: a sine with a fourth-harmonic partial that dies fast, and a soft mallet tick."""
    tt = t_(d)
    f = hz(m)
    x = np.sin(2 * np.pi * f * tt) * np.exp(-tt / (0.32 * d + 0.05))
    x += 0.35 * bright * np.sin(2 * np.pi * f * 3.93 * tt) * np.exp(-tt / 0.045)
    x += 0.12 * bright * bp(noise(d), 1500, 6000) * np.exp(-tt / 0.004)
    return x * adsr(d, 0.002, 0.03)


def woodblock(pitch=1.0):
    d = 0.12
    tt = t_(d)
    return norm(np.sin(2 * np.pi * 1180 * pitch * tt) * np.exp(-tt / 0.018)
                + 0.5 * bp(noise(d), 1500 * pitch, 5000) * np.exp(-tt / 0.006))


def saw(f, d, detune=0.0):
    tt = t_(d)
    return sum(2 * ((tt * f * (1 + k * detune)) % 1) - 1 for k in (-1, 0, 1)) / 3


def bell(m, d=1.6):
    tt = t_(d)
    f = hz(m)
    return (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt / 0.3)
            + 0.2 * np.sin(2 * np.pi * f * 5.4 * tt) * np.exp(-tt / 0.12)) * np.exp(-tt / 0.6)


def subdrop(d=1.2):
    tt = t_(d)
    f = 36 + 60 * np.exp(-tt / 0.22)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / 0.45)


def paper(d=0.32):
    tt = t_(d)
    n = noise(d)
    return norm(bp(n, 900, 7000) * np.exp(-((tt - d * 0.45) / (d * 0.28)) ** 2) * (1 + 0.6 * np.sin(2 * np.pi * 37 * tt)))


# ---- drums: a light groove where the cue file says so ---------------------------------
K, C, SH = kick(), clap(), shaker()
beats = int(round(DUR / BEAT))
for n in range(beats):
    t = n * BEAT
    if not kick_on(t):
        continue
    add(drums, K, t, 0.68)
    if n % 4 in (1, 3):
        add(drums, C, t, 0.30, -0.1)
    for q in (0.5,) if t < B(7) else (0.25, 0.5, 0.75):                   # sixteenth shakers from the repeat scene on
        add(drums, SH, t + BEAT * q, 0.10 if q != 0.5 else 0.16, 0.25 if q == 0.5 else -0.25)

# ---- sidechain: everything musical breathes with the kick ------------------------------------
duck = np.ones(N)
for n in range(beats):
    t = n * BEAT
    if kick_on(t):
        s = int(t * SR)
        e = min(N, s + int(0.35 * SR))
        duck[s:e] = np.minimum(duck[s:e], 1 - 0.55 * np.exp(-np.arange(e - s) / SR / 0.12))

# ---- bass: an offbeat root two octaves down, with harmonics so it speaks on small speakers ------
for n in range(beats * 2):
    t = n * BEAT / 2
    if n % 2 == 0 or not kick_on(t - BEAT / 2):
        continue
    f = hz(chord_at(t)[0] - 12)
    d = BEAT * 0.4
    x = lp(saw(f, d) * 0.5 + np.sin(2 * np.pi * f * t_(d)) * 0.9, 700) * adsr(d, 0.004, 0.05)
    add(bass, x, t, 0.32)

# ---- marimba: the film's voice; eighths over the chord, sixteenths in the busy scenes --------------
PATTERN = [0, 2, 1, 3, 2, 1, 3, 2]
for n in range(beats * 4):
    t = n * BEAT / 4
    if SIL0 - 0.05 <= t < TURN or t >= DUR - 0.2:
        continue
    ch = chord_at(t)
    notes = [ch[0] + 12, ch[1] + 12, ch[2] + 12, ch[0] + 24]
    m = notes[PATTERN[n % 8]]
    if t < B(1):                                    # the opening: a few muffled notes with the clock
        if n % 8 not in (0, 3, 6):
            continue
        g, br = 0.10, 0.2
    elif B(11) <= t < TURN:                         # the evening: the notes thin out toward midnight
        if n % 4 != 0 or t >= B(12):
            continue
        g, br = 0.12 * (1 - (t - B(11)) / BAR * 0.5), 0.5
    elif t >= LOGO:
        g, br = 0.16 * max(0.0, 1 - (t - LOGO) / (BAR * 2.2)), 0.8
        if n % 2:
            continue
    else:
        busy = B(7) <= t < B(9)
        if n % 2 and not busy:
            continue
        g, br = (0.13 if busy else 0.15), 0.9
    pan = 0.35 * np.sin(n * 0.7)
    add(music, marimba(m, 0.55, br), t, g, pan)

# ---- pad: one soft chord per scene, a held one at night, F on the turn ---------------------------
for s in CUES["scenes"]:
    if s["id"] == "open":
        continue
    t0, d = B(s["bar"]), s["bars"] * BAR
    if s["id"] == "midnight":
        d = SIL0 - t0
    last = s["id"] == "logo"
    x = sum(saw(hz(m + 12), d, 0.003) for m in s["chord"]) / len(s["chord"])
    x = lp(x, 1800 if last else 1300) * adsr(d, 0.05 if s["id"] != "midnight" else 0.8, 0.3 if last else 0.06)
    if last:
        x *= np.exp(-t_(d) / 2.4)
    add(music, x, t0, 0.26 if last else 0.15)
x = sum(saw(hz(m + 12), LOGO - TURN, 0.003) for m in TURN_CHORD) / 4
add(music, lp(x, 2200) * adsr(LOGO - TURN, 0.01, 0.2), TURN, 0.2)

# ---- the room: a quiet bed under the opening and the night ---------------------------------------
for t0, t1 in ((0.0, B(1) + 0.4), (B(11), SIL0)):
    d = t1 - t0
    x = lp(sfx.brown(d), 380) * np.minimum(1, np.minimum(t_(d) / 0.3, (d - t_(d)) / 0.3))
    add(bed, norm(x), t0, 0.05)

# ---- the big moments ---------------------------------------------------------------------------
add(fxs, subdrop(), B(1), 0.42)
add(fxs, subdrop(), TURN, 0.5)
add(fxs, hp(noise(1.4), 5500) * np.exp(-t_(1.4) / 0.4), TURN, 0.12)                  # a soft crash on the turn
stab = sum(marimba(m + 12, 1.2, 1.0) for m in CUES["scenes"][-1]["chord"])
add(music, stab, LOGO, 0.22)

# ---- foley: one sound for every visible action, from the picture's own events --------------------------
for e in EV:
    t, v, kind = e["t"], e.get("v", 1), e["type"]
    if kind == "slam":                                   # a word lands
        add(fxs, sfx.thump(0.35 * v, 120), t, 0.35, -0.3)
        add(fxs, sfx.click(1.6, 0.25 * v), t, 0.3, -0.3)
    elif kind == "tick":                                 # the clock
        add(fxs, woodblock(1.0), t, 0.16 * v, 0.2)
    elif kind == "swipe":
        add(fxs, hp(sfx.whoosh(0.45, 0.5), 900), t, 0.35, 0.1)
    elif kind == "pin":                                  # the pushpin into card: a wooden tock
        add(fxs, sfx.thump(0.8 * v, 95), t, 0.6, 0.3)
        add(fxs, woodblock(0.62), t, 0.28, 0.3)
    elif kind == "drop":
        add(fxs, hp(sfx.whoosh(0.3, 0.5), 2000)[::-1].copy(), t - 0.3, 0.25, 0.3)
    elif kind == "check":                                # a pencil tick
        add(fxs, norm(hp(noise(0.06), 3500) * np.exp(-t_(0.06) / 0.015)), t, 0.18, 0.4)
        add(fxs, sfx.pop(0.6), t + 0.02, 0.25, 0.4)
    elif kind == "swish":
        add(fxs, sfx.whoosh(0.3, 0.55), t, 0.4, 0.6)
    elif kind == "ding":                                 # the notification arrives
        add(fxs, bell(88, 1.0), t, 0.16, 0.3)
        add(fxs, bell(93, 1.0), t + 0.09, 0.12, 0.3)
    elif kind == "tap":
        add(fxs, sfx.click(1.1, 0.7), t, 0.45, 0.2)
    elif kind == "pop":
        add(fxs, sfx.pop(0.7), t, 0.4, 0.2)
    elif kind == "click":                                # chips
        add(fxs, sfx.click(1.4, 0.5), t, 0.3, -0.2)
    elif kind == "land":
        add(fxs, sfx.thump(0.5, 110), t, 0.4, -0.2)
        add(fxs, sfx.click(1.2, 0.6), t, 0.35, -0.2)
    elif kind == "dot":                                  # the chore copies across the week, rising
        add(fxs, marimba(72 + [0, 2, 4, 5, 7, 9, 12][int(v)], 0.3, 1.0), t, 0.16, -0.5 + v / 6)
    elif kind == "color":                                # each accent color gets a rising note
        add(fxs, marimba(77 + [0, 2, 4, 7, 9, 12][int(v)], 0.35, 1.0), t, 0.2, -0.4 + v * 0.16)
        add(fxs, sfx.pop(0.5), t, 0.18, -0.4 + v * 0.16)
    elif kind == "switch":
        add(fxs, sfx.click(0.8, 0.8), t, 0.4, 0.0)
        add(fxs, sfx.click(1.3, 0.5), t + 0.035, 0.3, 0.0)
    elif kind == "flip":                                 # the page turns: paper, then a soft landing
        add(fxs, paper(0.36), t, 0.45, 0.1)
        add(fxs, sfx.thump(0.6, 80), t + 0.3, 0.45, 0.0)
    elif kind == "impact":
        add(fxs, bell(77, 1.8), t, 0.1, -0.2)
    elif kind == "whoosh":
        add(fxs, sfx.whoosh(0.32, 0.5), t, 0.35, 0.0)
    elif kind == "chime":                                # the end card: two notes
        add(fxs, bell(84, 1.6), t + BEAT, 0.14, -0.15)
        add(fxs, bell(89, 1.8), t + BEAT + 0.12, 0.12, 0.15)

# ---- mix -----------------------------------------------------------------------------------------------
mix = drums * 0.85 + (bass + music) * duck[:, None] + fxs + bed
tt = np.arange(N) / SR
gate = np.clip((SIL0 - tt) / 0.012, 0, 1) + np.clip((tt - SIL1) / 0.003, 0, 1)    # digital zero before the turn
mix *= np.clip(gate, 0, 1)[:, None]
mix *= np.clip((DUR - tt) / 0.8, 0, 1)[:, None]                                    # the tail closes with the picture
mix = np.stack([sfx.limit(np.tanh(mix[:, c] * 0.9), 0.92) for c in (0, 1)], axis=1)
(HERE / "out").mkdir(exist_ok=True)
sf.write(HERE / "out" / "mix.wav", mix[: int(DUR * SR)].astype(np.float32), SR)
low = np.sqrt(np.mean(lp(mix[:, 0], 120) ** 2))
whole = np.sqrt(np.mean(mix[:, 0] ** 2))
print(f"wrote out/mix.wav, {DUR:.2f} s, peak {np.abs(mix).max():.2f}, "
      f"low band {20 * np.log10(low / whole):.1f} dB of the whole")
