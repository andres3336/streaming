# History

Append-only. Newest entries at the bottom.

## 2026-10-29

- Checkpointed the Kandown board and the live-preview spec (`f24885f`).
- Archived Kandown to `archive/kandown/`; `TODO.md` is now the source of truth.
- Imported 73 task specs: 32 P1, 5 P2, 24 P3, 12 P4; 0 in progress.
- Removed legacy board fields from specs: `status`, `order`, `updated`, `ownerType`.
- Installed JDK 21 (`~/.local/opt/jdk-21`), Gradle 8.14.6, and SDK platform 36.
- Added the Android TV app (`com.andres3336.streaming`): leanback manifest, banner,
  launcher icon, and a D-pad test screen.
- Added `scripts/env.sh`, `deploy.sh`, `logcat.sh`, `screenshot.sh`.
- Built `app-debug.apk` (9.6 MB), installed it on `192.168.100.16:5555`, and verified
  launch plus D-pad input. `apk-001`, `apk-002`, `apk-003` are done.
- Added the M3U parser: EXTINF attributes, EXTGRP, EXTVLCOPT headers, BOM/CRLF,
  missing-URL handling, and 10 unit tests.
- Added Media3 ExoPlayer 1.11.1 with HLS, per-channel headers, lifecycle pause/resume,
  audio focus and keep-screen-on.
- Added a demo channel list (two public HLS streams) and a fullscreen player. Verified
  848x480 H.264 playback and Back navigation on the TV. `liv-001` and `ply-001` are done.
- Debug builds use a `TextureView` so `adb screencap` can capture video; release keeps
  `SurfaceView`.
