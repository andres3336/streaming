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
