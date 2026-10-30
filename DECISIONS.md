# Decisions

Short record of product and architecture decisions. `HISTORY.md` is the running log;
`tasks/` is a frozen archive of old specs and is not read as context.

## Identity

- App name: **Streaming**
- Package: `com.andres3336.streaming`
- Platform: Android TV / Google TV first
- Language: **Spanish only**
- UI strings live in the default `values/strings.xml`; no translation files

## Target device

- Android TV 11 / API 30
- 32-bit `armeabi-v7a`
- About 2 GB free storage
- Streaming is the priority

## Sources

- Curated live channels from a private/secret gist M3U feed **En vivo**
- Xtream feeds **Canales** (live), Películas and Series
- The two live sources are separate tabs; they load and fail independently
- `sources.liveMode` is legacy and no longer merges the sources
- Grandma profile uses `gist` only, so Xtream's 10k live list is never loaded
- No Stalker, Jellyfin, Stremio, or DRM

## Remote configuration

- No setup code in V1
- One APK; the config URL is embedded at build time
- Config is a JSON file in a secret/unlisted gist
- The app checks on cold start, on resume, on source failure, and via a hidden manual action
- **No periodic timer**
- Last-known-good config is cached and encrypted with Android Keystore
- New config is validated and applied atomically
- If Xtream or the M3U source breaks, show a Spanish dialog to fetch config again and retry
- Do not prompt for a single dead channel
- No per-device credentials or source configuration

Secret gist caveat: GitHub secret gists are unlisted, not access-controlled. Treat the
config URL as a password. A truly private source requires a backend, which is out of scope.

## Visual theme

- Flat, monochrome dark UI with one slate-blue accent (`#485584`)
- Solid fills and 1 px borders only: no transparency, gradients, glass or focus scaling
- The only non-opaque color is the modal backdrop over playing video
- Horizontal top navigation bar with icon and label items; section titles live in the
  content below it
- Keycap hint strip in the bottom-right of every section
- The app owns its text scale: the TV's system font scale is ignored so layouts stay stable
- Hero banners put the text on a solid panel next to the artwork, never on a photo
- The live list has a right-hand preview pane with the EPG below it; movies and series
  preview through a hero-on-focus above the grid, with the full synopsis
- OwnTV is a UX reference only; all code is clean-room

## UI and profiles

- One universal browse/player skeleton for live, movies and series
- `MediaItem` + `MediaCapabilities` drive type-specific slots
- Live: zapping and optional now/next
- Movie: seekbar and resume
- Series: seasons, episodes and next episode
- Grandma/abuela mode is deferred to v2; V1 has one UI and no PIN
- V2 may show live TV only and hide settings, sections and source switching
- App-level lock only; it is not an Android kiosk/device-owner mode

## Player

- Media3 ExoPlayer only
- HLS, MPEG-TS, MP4
- No mpv, no FFmpeg native extension
- Per-channel User-Agent/Referer headers
- Retry, fallback and reconnect
- Audio/subtitle pickers
- Aspect/zoom: Fit, Fill, Stretch, Original, Force 16:9, Force 4:3
- Stream info: codec, resolution, FPS, bitrate, buffer, dropped frames, decoder, bandwidth
- External player handoff for movies/series only; live TV stays in the internal player
- Quality: automatic cap plus manual HLS variant preference
- Buffer: configurable start pre-buffer and after-rebuffer pre-buffer; optional low-latency live
- VOD speed selector; live playback is always 1x
- Subtitles: embedded tracks, local `.srt` via SAF, and OpenSubtitles search/download
  with manual timing offset. Exact file-hash sync is not available for streams.

## Startup

- `Iniciar al encender` and `Reanudar al despertar` are on by default
- Android 11 blocks background activity starts after boot on the target TV; the app
  keeps the receiver and also offers an opt-in `Usar Streaming como inicio` alias
- Restoring the last channel/VOD position uses an encrypted last-playback record

## Video cache

- Cache **VOD only**, never live TV
- Live caching does not improve transmission and adds latency and disk churn
- Sizes: 100, 250 or 500 MB, LRU eviction
- Default: 250 MB
- Clear-on-start is optional and off by default
- Manual clear cache always available
- Clearing on exit is not used because process death makes it unreliable

## Non-goals

- Recording, DVR, timeshift, live rewind
- Catch-up/archive
- Downloads and offline video
- Multiview
- Full EPG grid (now/next is optional in V1.1)
- mpv/FFmpeg, Widevine/ClearKey DRM
- TMDB, Trakt, Stremio, Cast, Watch Next, TV Input
- Full profiles, local sync, per-device config
- Glass/blur/ambient/animation engines
- RTL support
- In-app updates and QR setup

## Licensing

OpenTV and OwnTV are GPL-3.0; StreamVault is source-available and non-commercial.
Use them as UX references and write clean-room code. Do not copy their source unless
the project accepts the corresponding license.
