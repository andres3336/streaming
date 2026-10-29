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
- Added the config schema, validator, JSON Schema and redacted sample (`config/`), with
  9 parser tests.
- Added `ConfigRepository` (OkHttp, ETag/If-None-Match, no URL or credential logging)
  and `M3uRepository`, with 5 repository tests.
- Wired `config.url` from untracked `local.properties` into `BuildConfig.CONFIG_URL`;
  an empty URL falls back to the demo playlist.
- Added loading, error/retry, auto-focused grouped channel list, and remote M3U loading.
- Verified end-to-end against a local config and playlist over `adb reverse`: config
  fetch, 3 channels in 2 groups, playback. `cfg-001` and `cfg-002` are done.
- Pointed `config.url` at the permanent gist and added `Cache-Control: no-cache` to the
  config and playlist requests so gist edits propagate.
- Verified the real 55-channel live list on the TV: group header, auto-focus and
  playback. TD+ plays at 1280x720.
- Added Coil channel logos. The current playlist uses one generic logo for all 55
  channels, so the list falls back to channel initials unless the playlist has
  distinct logos.
- Added the Xtream client: auth, VOD categories, movies, series, series info and
  stream URL construction. Credentials stay in the gist and are never logged.
- Added the Películas and Series tabs with category rails, posters, movie playback,
  seasons/episodes and episode playback.
- Verified on the TV: 38 movie categories, 36 series categories, movie at 1920x960
  and episode at 1920x872. Version 0.4.0.
- Expanded the config to v2: network, player, cache, live, EPG, diagnostics and UI
  settings, with validation and tests.
- Added local device overrides in `files/device-settings.json` and a deep merge on top
  of the gist. Sources and credentials remain central-only.
- Added the Ajustes screen: buffer, quality cap, aspect, VOD cache size, clear cache,
  WiFi/Ethernet-only and max bitrate.
- Wired buffer, quality/bitrate track selection, aspect resize, network policy and a
  VOD-only Media3 SimpleCache.
- Verified on the TV: buffer persisted across restart, the VOD cache grew to ~13 MB,
  and clearing removed the cache directory. 34 unit tests passing. Version 0.5.0.
