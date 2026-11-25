# Pinned Calendar launch film: running log

Read this first to resume cold.

## What this is

A 36-second launch film, 1920x1080 at 60 fps, for the Play listing (as a YouTube link) and
social. It follows the method of the Player 2 launch film
(`player2-repos-main/player2-marketing/film`). `sh build.sh` builds
`pinned-calendar-launch.mp4`. `sh build.sh draft` builds a 30 fps draft to `out/draft.mp4` in
about 25 seconds.

- `cues.json`: tempo, bars, scenes, chords, the midnight turn and the silence. The picture and
  the score both read it.
- `index.html`: the picture, a function of time (`window.render(t)`).
- `mix.py`: the score and the sound effects, all synthesized.
- `TREATMENT.md`: the premise, the shot list, the cue map and the sound design.
- `media/`: the emulator recording of the shade pull-down. `tools/prep.py` turns it into
  `frames/` (generated, not committed) and stretches its limited-range levels to full range so
  the footage matches the rebuilt card.
- Render tools: the Lemo-Opuscar library (`LIB`, default `~/lemo-opuscar`).

## Rounds

1. First stills. Fixed: words ran together (inline-block spans drop trailing spaces, now
   `white-space: pre`); the pin sat on the reminder's text (the card now steps down under the
   reminder); the repeat pill hid behind the chips (stacking order); tomorrow's rows were
   stacked on one line; the page turn showed mirrored text (the back of the page is now blank
   paper); the evening passed through a muddy gray (now cream, sunset, night).
2. Draft with sound. Fixed: "Midnight?" vanished against the sunset (headline color now follows
   the sky's luminance); tomorrow's card peeked out during the silence (it now appears on the
   turn); the turn lengthened from 0.55 s to 0.75 s; the low band trimmed from -1.8 to -2.2 dB.
3. Release cut at 60 fps.

## Checks on the current cut

- `readcheck.mjs`: 9 of 9 texts on screen long enough (the clock label is marked as passing UI).
- Loudness: -14.0 LUFS, peak -1.4 dBFS.
- `blackdetect`: no black frames.

## Open

- No person has watched it at full speed with sound yet. The checks above are from frames and
  meters.
- Ideas not built: a vertical 1080x1920 cut for social; a real recording of the Mark done tap.
