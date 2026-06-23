PINNED CALENDAR — PLAY STORE & LAUNCHER ICON PACK
==================================================

play-store-icon-512.png    512×512  → Google Play Console "App icon" (hi-res). Upload as-is. Play applies the rounded mask.
icon-master-1024.png       1024×1024 master, for any larger surface or re-export.
icon-rounded-512.png       512×512 preview showing how it looks rounded on the home screen.

launcher/                  Legacy square launcher icons, drop into android res/:
  mipmap-mdpi/ic_launcher.png      48×48
  mipmap-hdpi/ic_launcher.png      72×72
  mipmap-xhdpi/ic_launcher.png     96×96
  mipmap-xxhdpi/ic_launcher.png    144×144
  mipmap-xxxhdpi/ic_launcher.png   192×192

NOTE — Adaptive icons (Android 8+): for a true adaptive icon you want a transparent
FOREGROUND layer (the calendar + pin, with ~25% safe padding) over a solid orange
BACKGROUND (#E07F2C). These square PNGs work as a legacy fallback; generate the
adaptive foreground/background from the master if you want the modern shape support.
