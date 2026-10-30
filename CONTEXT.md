# Context

Technical working map for Streaming. Read this before changing code. `README.md`
has status/setup; `HISTORY.md` has the append-only change log.

## Identity

- App: **Streaming** (`com.andres3336.streaming`)
- Android TV / Google TV first; Android 11, 32-bit ARM
- Spanish-only UI; strings in `app/src/main/res/values/strings.xml`
- Media3 ExoPlayer only; no mpv, FFmpeg, DRM or Stalker/Jellyfin
- OwnTV/OpenTV/StreamVault under `ingest/` are UX references only; code is clean-room

## Commands

```bash
./scripts/check.sh               # unit tests + debug APK
./scripts/deploy.sh              # build, install and launch on the TV
./scripts/logcat.sh
./scripts/screenshot.sh out.png
```

Environment: JDK 21 at `~/.local/opt/jdk-21`, Android SDK at
`/home/andres/Android/Sdk`, Gradle wrapper 8.14.6.

## Source map

```text
MainActivity.kt     activity entry point only
core.kt             StreamingApp, AppState, loaders, playback/cache orchestration
layout.kt           ReadyScreen router, shell scaffolding, common empty/error UI
home.kt             Inicio hero and rows; Continuar viendo
channels.kt         TV: one rail over the curated list and Xtream categories, preview
movies.kt           Películas grid and movie detail
series.kt           Series grid and series/episode detail
search.kt           universal fuzzy search
settings.kt         Ajustes cards and choice rows
player.kt           PlaybackScreen, player construction, stream stats, playback errors
player_overlays.kt  stats/tracks/quality/speed/controls/subtitles/next/error overlays
ui/                 reusable widgets and tokens
```

Reusable UI: `ui/AppShell.kt` (top nav/AppMark, Back focus), `ui/Modal.kt`,
`ui/Cinematic.kt` (fixed-height focus header), `ui/CategoryPanel.kt`, `ui/Hero.kt`,
`ui/MediaCard.kt`, `ui/PosterGrid.kt`, `ui/LivePreview.kt`, `ui/PlayerChrome.kt`,
`ui/Tokens.kt`.

## Data flow

`StreamingApp` in `core.kt` owns config, encrypted caches, device settings, playback
state, live favorites/recents and the last-playback record. It builds the effective
config, applies the text scale, then calls `ReadyScreen` in `layout.kt`.

`ReadyScreen` owns section navigation and passes callbacks to section composables.
Sections use `PlaybackRequest` to open `PlaybackScreen`; closing playback returns to
the same section. A pending config refresh is deferred while playback is open.

## Screens

- `Inicio` — `HomeScreen` (`home.kt`): **Continuar viendo** row first (history),
  then the hero (Película → En vivo → empty), then rows for Favoritos, En vivo
  ahora, Películas, Series.
- `TV` — `TvBrowseScreen` (`channels.kt`): one section and one rail. `_Mis canales`
  is the curated gist list pinned on top, followed by the Xtream categories; there
  are no source sub-tabs. The curated list groups Favoritos, Recientes and its M3U
  groups; Xtream categories are flat and numbered. Rail + list + fixed preview pane
  (video, logo, name, group · source, `OK · Ver`). No EPG and no inline search.
  Channel rows are `[★] [n] [logo] name / group`; the star is a passive indicator
  and long-press OK toggles it.
- `Películas` — `XtreamMoviesScreen` (`movies.kt`): rail, a compact fixed-height
  header for the focused title (title, meta, two-line synopsis) and the 7-column
  2:3 poster grid. One card per title: language variants (`SU-`, `CA-`, `ES-`, …) are
  grouped by `TitleVariants` and chosen in `MovieDetail` with `SUB`/`CAM`/`ESP`
  chips. `MovieDetail` uses the preview as the play target (`OK`), keeps only
  `Volver` in its header, and relies on long-press in the grid for favorites.
- `Series` — `XtreamSeriesScreen` (`series.kt`): same header, variant grouping and
  poster grid. `SeriesDetail` is two columns: seasons/episodes on the left (with
  watched progress and the last played episode pre-focused) and an autoplaying 16:9
  preview of the focused episode on the right; `Visto hasta` shows its resume
  position. CH+/CH− move the list selection; the same keys and Up/Down change
  episode in the player.
- `Buscar` — `UniversalSearchScreen` (`search.kt`): one master search over TV,
  Películas, Series and their categories. Results are grouped by type with
  a header and count per group (best-scoring group first), eight rows per group.
  Selecting a category opens its section with that category already selected.
  The catalogue is lean (ids + display fields, no full VOD objects) and persists in
  an encrypted gzipped file (`SearchCatalogStore`), so a cold start does not download
  it again. The field autofocuses and exposes the IME search/microphone action.
- `Ajustes` — `SettingsScreen` (`settings.kt`): identity card, Inicio,
  Reproducción, Interfaz, Subtítulos, Caché VOD, Red, Fuentes, Diagnóstico,
  Configuración. Every setting is one content-sized row: label left, control right,
  the selected option marked with `✓`. Sí/No uses two chips; longer lists open a
  dropdown. Ancho de banda offers Sin límite / 200 / 100 / 50 Mbps. There is no
  WiFi-only setting: a TV is wired or on the home network.

## UI rules

- Flat monochrome dark palette; accent `#485584`; solid fills and 1 px borders.
- No transparency except the modal scrim over video; no gradients, glass or scaling.
- The app owns text scale via `LocalDensity.fontScale`; ignore the TV system font
  scale. Density is the device's, so dp layout is a fixed 960 x 540 canvas and the
  text scale changes type only, never paddings, gaps or card sizes.
- Spanish labels. Keep focus reachable with the D-pad.
- Inicio's hero text sits on a solid panel next to the artwork. Películas/Series show
  a compact header above the grid; every line in it reserves its height so moving
  across posters never shifts the grid.
- The top bar is the only place a section is named: no second title row and no
  bottom hint strip. The bar shows the app mark, the sections, and the time plus the
  date on one line with the same size.
- Favorite card stroke is the accent; the live-row star is a small left indicator
  with no container and long-press OK toggles it.
- TV sizing: 7-column 2:3 poster grid, rating top-left, favourite star top-right,
  two-line card titles, small row heights and paddings. Aim at a 1080p screen viewed
  from the couch, not at touch targets.
- Modals (`ui/Modal.kt`) are separate windows: focus cannot escape them and Back
  closes them.

## Player

- One `PlayerBar` in `ui/PlayerChrome.kt`, anchored at the bottom of the screen
  (32 dp margins): title, then the seek bar with elapsed/duration, then one row with
  the transport in the middle and the action icons split evenly around it. Icons are
  Canvas glyphs; no text labels.
- Actions: Info, Aspecto, Calidad, Audio/Subs; VOD adds Velocidad and Externo. The
  split (first half left, rest right) keeps play/pause in the centre of the screen
  whatever a config enables.
- Transport is `prev · ‹ · play/pause · › · next`: `prev`/`next` jump a whole
  episode (series queue) or channel (live, with zapping) and a missing neighbour
  keeps its slot as a dimmed, non-focusable icon, so the row stays symmetric and
  play/pause sits on the exact screen centre. `‹`/`›` are the seek buttons and are
  VOD-only (`canSeek`).
- One seek ladder for the focused seek bar's Left/Right and for `‹`/`›`: 5 s, 10 s,
  30 s, then 60 s per press. The step survives a direction change and resets to 5 s
  after 1.5 s without a seek press. Media keys stay at a fixed 30 s.
- The chrome is visible on entry, hides after 1 s while playing unless a button has
  focus, and returns on any key. Back hides the bar when a control has focus,
  leaving playback; from the video it closes playback. On live TV the seek row is
  inert, so Up/Down zap (when `zapping` is on and the list has more than one
  channel) even while the bar has focus; on VOD they move between the seek and
  transport rows, and Left/Right/Center reveal/focus. Series queues use CH+/CH− and
  Up/Down to change episode. Live titles read `CH5 - Peppa - TV`.
- Captions render through Media3's default `SubtitleView` at the bottom, so the bar
  covers them while it is visible.
- The embedded ExoPlayer controller is disabled; media keys still play/pause and
  seek, CH+/CH− and MEDIA_NEXT/PREVIOUS zap live channels.
- The player background and the letterbox bars are always pure black
  (`#000000`), never the app background or the accent.
- Overlays: stream info, audio/subtitle picker, subtitle search, quality, speed,
  player controls, next episode and playback error.
- VOD: resume positions, next-episode auto-play, speed, external-player handoff.
- Cache is VOD-only; never cache live TV.

## Sources and config

- `_Mis canales`: curated private/secret-gist M3U feed, pinned on top of the TV rail.
- Xtream: live categories (`Cultura`, `Deportes`, …), Películas and Series.
- There is no EPG. `epg`, `epgUrl` and `ui.showNowNext` are accepted and ignored so
  published gists keep validating; do not reintroduce XMLTV.
- Config is one JSON secret/unlisted gist URL embedded at build time. It refreshes
  on cold start, resume, source failure and manual action; no periodic timer.
- Last-known-good config and caches are encrypted with Android Keystore.
- Do not show or log URLs, credentials or playlist tokens.
- Back moves focus to the top bar; only from the bar does it offer to exit.
- If a source breaks, offer `Actualizar configuración` and `Reintentar`; keep the
  previous config on failure.

## Startup and cache

- `Iniciar al encender` and `Reanudar al despertar` are on by default; the opt-in
  HOME alias is the fallback when Android 11 blocks boot starts.
- Cache VOD only: 100/250/500 MB, LRU, default 250 MB; optional clear-on-start.
- Last playback and resume positions are encrypted.

## Non-goals

Recording/DVR/timeshift, catch-up, downloads, multiview, EPG of any kind,
mpv/FFmpeg, DRM, TMDB/Trakt/Stremio/Cast/Watch Next, profiles/PIN, RTL, in-app
updates or QR setup.

## Working rules

- Read this file first; use `README.md` and `HISTORY.md` only for status/history.
- `ingest/` is local reference only, gitignored, never a build input.
- No draw.io or wireframe maintenance.
- Update `README.md`, `CONTEXT.md`, `HISTORY.md` and `AGENTS.md` only in the same
  commit as the code change. Append to `HISTORY.md`, never rewrite it.
- Keep `MainActivity.kt` thin; put new screen code in its section file.
- Shared state/protocol changes go through `core.kt`; `layout.kt` owns the router
  and common screen scaffolding.
- Subagents may work on independent section files or read-only review. Do not
  parallelize edits to `core.kt` or `layout.kt`.

## Agent tooling

- `.opencode/skills/tv-ui/` — D-pad, tokens, hero/card/grid, text scale, strings.
- `.opencode/skills/player/` — ExoPlayer, chrome, overlays, tracks, VOD cache.
- `.opencode/skills/sources-config/` — config gist, M3U/Xtream, Keystore, secrets.
- `.opencode/skills/tv-device/` — adb device at `192.168.100.16:5555`, deploy, logs,
  screenshots and key events.
- `.opencode/agents/reviewer.md` — read-only review subagent for UI rules, player
  invariants, Spanish strings and leaked secrets.

Skills hold procedures and invariants; this file stays the map. Change a rule here and
in its skill together, in the same commit.
