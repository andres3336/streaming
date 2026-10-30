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
- Added the encrypted last-known-good config cache: Android Keystore AES-GCM, atomic
  write, and fallback when the gist is unreachable.
- Verified the fallback by building against an unreachable config URL: the app started
  from the cache, loaded the real playlist and showed `Gist · caché`. The cache file
  contains no plaintext credentials or playlist URL. Version 0.6.0.
- Added a Spanish playback error overlay with Reintentar and Cerrar.
- Added bounded retry with backoff and auto-reconnect for live streams using
  `retryCount` and `retryDelayMs`.
- Verified with a 404 channel: two bounded retries, then the error overlay; Cerrar
  returns to the channel list. Version 0.7.0.
- Added silent config refresh on app resume and a manual `Actualizar configuración`
  action in Ajustes.
- Config updates apply only when nothing is playing; a refresh that arrives during
  playback is deferred until the player closes.
- Verified playback survives Home/resume and the manual refresh completes without
  interrupting the UI. Version 0.8.0.
- Added VOD resume positions and a `Continuar` tab. Positions save every 5 seconds,
  on stop and on exit; verified a movie resumed from 0:17.
- Added next-episode auto-play with a 10-second countdown and Reproducir ahora /
  Cancelar overlay. The queue comes from the Xtream season episodes.
- Security fix: playback positions are now Keystore AES-GCM encrypted because the
  stored URLs contain Xtream credentials. The legacy plaintext file is deleted.
- Sanitized config, playlist, Xtream and player errors so URLs/credentials are not
  logged or shown. 43 unit tests passing. Version 0.9.0.
- Reconciled the stale board into `todo-v1.md`: 74 specs, 20 done, 3 Now items,
  10 Next, 7 polish, 18 v1.1 and 15 v2. Added `cfg-016` offline playlist cache.
  Renamed `TODO.md` and updated the agent instructions.
- Added encrypted live state (`files/live-state.bin`) for favorites, recents and the
  last channel, keyed by `tvg-id` or URL so it survives playlist refreshes.
- Added Favoritos and Recientes rows, star toggles, and last-channel focus on entry.
- Added CH+/CH− and media next/previous zapping over the current filtered list with
  wraparound. Verified FUTV → Tigo → Canal 4 on the TV, plus state persistence after
  a force-stop and relaunch.
- Added accent-insensitive channel search by name and group, query persistence while
  browsing results, a clear action and a no-results state. Verified on the TV with the
  system keyboard.
- Added the audio/subtitle picker: Spanish labels (language, layout, codec), immediate
  track overrides, an explicit Desactivado subtitle state, and MENU/INFO shortcuts.
  Verified the panel and selection on live playback.
- 60 unit tests passing (17 new for live state, search and track labels).
  Version 0.10.0.
- Scope decision: finish V1 before v1.1. `liv-008`/`liv-009` moved to v2 because
  local category/channel overrides conflict with the gist as the remote source of
  truth; `liv-012` and `ply-016` moved to v2 as not required. v1.1 now starts with
  `epg-001` and `epg-002`.
- Added the in-player stream info panel (`INFO` or the `Info` button): video codec,
  resolution, FPS, bitrate, buffer, dropped frames, decoder name, Media3 bandwidth
  estimate and stream type. The source is reduced to its host so tokens and
  credentials never appear.
- Verified the panel on the TV: H.264 1280×720, OMX.realtek decoder, 2.4 Mbps
  estimate, 24 s buffer, 0 dropped frames. 65 unit tests passing.
- Added the Xtream account status probe and a mode-aware source health check. Ajustes
  now has `Comprobar fuentes`; verified `En vivo: OK · Xtream: OK` on the TV.
- Added the Spanish update-config recovery flow on start and VOD source failures:
  `Actualizar configuración` forces a fresh gist fetch without the ETag and retries;
  `Reintentar` retries the current source. The old config is kept if the update fails.
- Verified the failure path with a local test config whose M3U was unreachable: the
  error screen focused `Actualizar configuración`, issued a second forced config
  request and kept the recovery prompt without crashing. Restored the real config.
- Added the blocking `forceUpdate` prompt. 70 unit tests passing. Version 0.11.0.
- Added an encrypted offline playlist cache and non-reversible source identities.
  A changed M3U URL/headers or Xtream account invalidates only that source's cache.
  Verified a cold start with the playlist host down: the TV showed `Lista en caché`
  with the last channel list.
- Added Xtream login/status, live categories, lazy per-category streams and an
  encrypted Xtream catalog cache. Verified `xtream` mode (real category rail and lazy
  streams) and `merge` mode (106 merged channels, deduped, with `Xtream` provider
  labels) on the TV.
- Added the group rail, channel numbers, stable list keys and a reusable browse shell
  shared by Live/Movies/Series, with a shared poster row and optional detail slot.
- Added the remaining aspect modes (stretch, force 16:9 and 4:3) with an in-player
  control and persistence. Verified cycling to 4:3 and that the setting survives into
  Ajustes.
- Added the HLS→TS fallback, source-failure recovery event and a bounded sanitized
  error log. Verified an unreachable channel logs
  `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED` without URLs or credentials.
- Added cache usage in MB, clear-now and clear-on-start. Verified the cache dropped
  from 26 MB to 0 MB after a restart with clear-on-start enabled, then restored the
  setting to No.
- Added `scripts/config-validate.sh` with schema validation, publish instructions and
  gist rollback documentation. Verified against `config/sample-config.json`.
- Verified config propagation on the TV with a local config/playlist server: a changed
  config applied after a cold restart and after Home/resume, and a changed M3U URL
  invalidated the old playlist cache.
- 87 unit tests passing. Version 0.12.0. V1 is complete.
- Added optional XMLTV EPG (`epg-001`): SAX parser, transparent gzip, encrypted
  source-keyed cache with a rolling 6 h back / 48 h forward window and
  `epg.refreshHours` refresh policy. No URL means no EPG work.
- Added now/next to live channel rows (`epg-002`): tvg-id first, XMLTV display-name
  fallback, programme title, time left and next title; rows stay unchanged when no
  guide data exists. A 30 s ticker keeps the countdown current.
- Verified on the TV with a local gzipped XMLTV guide: TD+ showed
  `Ahora: Noticias TD · 29 min` + `Después: Documental`; after killing the guide
  server and restarting, the same data came from the encrypted cache.
- 95 unit tests passing. Version 0.13.0. v1.1 Now starts with `ply-012`, `ply-013`
  and `ply-017`.
- Wired `ui.showNowNext` to the guide rows so the config flag actually controls
  whether now/next is displayed. Version 0.13.1.
- Started v1.1. Split live into **En vivo** (curated gist only) and **Canales**
  (Xtream only); `liveMode` is now legacy and `merge` is retired. Old configs gain
  the `channels` tab automatically, and gist/Xtream failures no longer block each
  other. Source health checks every configured source.
- Hardened the M3U parser for single-quoted attributes and `#EXTGRP` before
  `#EXTINF`. Added Xtream divider/empty category filtering, a lazy `< 2` items rule,
  and `Mostrar ocultas`. Verified the Canales tab on the TV (52 channels in Cultura,
  one hidden category).
- Added the pragmatic `MediaKind`/`MediaItem`/`MediaCapabilities` model and gated the
  single player by capabilities. VOD gains quality selection, speed and an external
  player button; live keeps zapping, HUD and recovery.
- Added quality cap + manual HLS variant preference, start/after-rebuffer pre-buffer,
  optional low-latency live, VOD speed (0.5–2x), play/pause media keys and ±30 s
  seek for VOD. Effective buffer values appear in stream info.
- Added Xtream movie/series metadata (`get_vod_info`, series info) with plot, year,
  rating, duration, genre, cast, backdrop and a reusable movie detail panel.
- Added universal fuzzy search: En vivo, Canales, Películas and Series; accents and
  ñ/n are folded; typo tolerance, ranking, debounced input, on-demand full catalogue
  (47,646 entries on the test account), session cache and progressive live results.
  Verified `nino` → Ninotchka / Niño Santo / Niño Ardilla on the TV.
- Added subtitles: local SAF `.srt`, OpenSubtitles search/download (Spanish first,
  English fallback), encrypted per-media offsets (±0.5 s, ±5 s, reset), UTF-8/
  Windows-1252 detection. Without an API key the app degrades gracefully.
- Added the VOD-only external-player handoff (`ACTION_VIEW` + headers). Verified the
  no-handler path on the TV.
- Added text/UI scale via `LocalDensity`, top/rail navigation, full/compact player
  bar and visible player button selection. Settings persist locally.
- Added boot/wake auto-start (on by default) with an encrypted last-playback record
  and last-position restore. `BOOT_COMPLETED` and the alarm fallback are both blocked
  by Android 11 on this Google TV; the opt-in HOME alias is the documented fallback.
  `Reanudar al despertar` restored a movie at 03:44.
- Updated config to v3 (subtitles, player pre-buffer/latency, UI scale/layout,
  `channels` section, Xtream category settings) with schema, sample, validator and
  README updates. 124 unit tests passing. Version 0.14.0.
- Added a Back confirmation on the main screen: a single Back now shows
  `¿Salir de Streaming?` with Cancelar focused by default. Playback Back still
  returns to the list. Version 0.14.1.

## 2026-10-30

- Replaced the old top/rail shell with a rounded navigation pill, compact top bar,
  quick section chips and a 26/74 split category panel for Películas/Series
  (`ui-011`). The old wide text rail and full-width poster rows are gone.
- Added a four-column 16:9 poster grid with focus scale, 2 dp gold favorite stroke,
  favorite star (upper left), rating and language badges (upper right), and
  one-line ellipsized titles/subtitles. Cards auto-focus and Left/Right route
  explicitly between the category panel and the grid.
- Unified favorites: live channels keep their existing keys and movies/series use
  `movie:<id>`/`series:<id>` in the same encrypted state. Long-press OK toggles a
  card; movie/series detail panels gained a favorite chip. The Favoritos category
  loads the full VOD catalogue on demand and filters locally.
- Added `tvg-language` to the M3U parser and optional Xtream `lang`/`language`/
  `audio_language` fields. Badges normalize to ESP/ENG/DUAL and stay hidden when the
  provider sends no metadata. Ratings remain the provider's own `★ 7.6` score.
- Pill-styled search filters, settings choices, result rows and the player top bar.
  Removed the obsolete Ajustes navigation choice; `ui.navigation` is still parsed
  for config compatibility but the pill shell is unconditional.
- Verified on the TV at 1280x720: rail + split panels, 4-column grid, focus routing,
  long-press favorite, gold stroke persistence after force-stop, detail toggle,
  Favoritos, Continue grid, search filters, settings and player chips.
  129 unit tests passing. Version 0.15.0.
- Removed `todo-v1.md` and retired `tasks/` as agent context: it is now a frozen
  archive with a single README marker. `AGENTS.md` points at `README.md`,
  `DECISIONS.md` and `HISTORY.md` instead, and the README task-tracking section was
  rewritten. No spec file was moved or deleted.
- Replaced the blue/green palette with a flat monochrome theme and one slate-blue
  accent (`#485584`): solid fills, 1 px borders, no transparency, gradients, glass or
  focus scaling. The only non-opaque layer left is the modal backdrop over video.
- Rebuilt the shell as an icon-only navigation pill with the app monogram, a source
  chip, a large clock with the date and a keycap hint strip. Section titles moved out
  of the top bar and into the content.
- Merged `En vivo` and `Canales` into one **TV** destination switched by header chips.
- Added the **Inicio** landing screen: a hero with the text on a solid panel next to
  the artwork, plus Continuar viendo, Favoritos, En vivo ahora, Películas and Series
  rows. Continue cards keep the resume bar.
- Added the live preview pane: channel list on the left, autoplaying stream with audio
  on the right and the EPG now/next plus progress below it. Preview requests start only
  after focus settles and are released on leave. Verified TD+ and South Park on the TV.
- Added hero-on-focus to Películas and Series: the focused poster fills a compact hero
  with year, genre, rating and synopsis; `vodInfo` fills the plot on demand. The movie
  detail now shows an autoplaying preview with the synopsis beside it.
- Restyled Ajustes into flat bordered cards with a section rail marker, an identity
  card and the current value on each row; restyled the series detail with flat season
  headers and episode rows.
- The app now owns its text scale (`ui.fontScale`) instead of multiplying the TV's
  system font scale, which is 1.4 on the test device and was clipping every title.
- Restyled the player chrome and the Controles panel to the same flat pills; player
  behaviour is unchanged.
- Verified on the TV at 1280x720: home hero and rows, live list plus preview and EPG,
  movie/series hero-on-focus, movie detail preview, series episodes and Ajustes.
  129 unit tests passing. Version 0.16.0.
- Moved the navigation from the left pill to a horizontal top bar with icon and label
  items, so Up from the content reaches the sections directly. The inline channel
  search is now a `Buscar` button that opens the field on demand, because the always
  visible field used to trap Up and open the keyboard. Version 0.16.1.
- Added editable neutral draw.io wireframes in `design/layouts/`: one file per screen
  (Inicio, TV, Películas, Series, Buscar, Ajustes, Reproductor) with viewport,
  full-scroll and secondary-state pages. Each page has Wireframe, Anotaciones,
  Etiquetas de elemento and Medidas layers; major blocks carry stable `element` and
  `source` attributes so an edited XML can be mapped back to Compose.
- Applied the draw.io red/green markup: removed the hint strips from Inicio, Películas
  and Series; removed the TV section title and inline channel search from both TV tabs;
  moved the channel EPG into a second column and made live favorite stars plain icons.
- The movie detail preview is now the play target; its duplicate favorite chips and
  Reproducir/Favorito buttons are gone and favorites stay on long-press in the poster
  grid. The player now uses one icon-only top bar that auto-hides 1 s after playback
  starts, stays visible while paused or in an overlay, and returns on any key. The
  embedded ExoPlayer controller is disabled so the custom bar is the only chrome;
  media keys still play/pause and seek. Kept CH+/CH− zapping without the on-screen
  text. Refreshed `design/layouts/` and its README. 129 unit tests passing; debug APK
  builds and was verified on the TV.
