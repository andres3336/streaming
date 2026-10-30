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
core.kt             StreamingApp, AppState, loaders, EPG/playback/cache orchestration
layout.kt           ReadyScreen router, shell scaffolding, common empty/error UI
home.kt             Inicio hero and rows; Continuar viendo
channels.kt         En vivo and Canales; category rail, channel rows, preview
movies.kt           Películas grid and movie detail
series.kt           Series grid and series/episode detail
search.kt           universal fuzzy search
settings.kt         Ajustes cards and choice rows
player.kt           PlaybackScreen, player construction, stream stats, playback errors
player_overlays.kt  stats/tracks/quality/speed/controls/subtitles/next/error overlays
ui/                 reusable widgets and tokens
```

Reusable UI: `ui/AppShell.kt` (top nav/hints/QuickChip), `ui/CategoryPanel.kt`,
`ui/Hero.kt`, `ui/MediaCard.kt`, `ui/PosterGrid.kt`, `ui/LivePreview.kt`,
`ui/PlayerChrome.kt`, `ui/Tokens.kt`.

## Data flow

`StreamingApp` in `core.kt` owns config, encrypted caches, EPG, device settings,
playback state, live favorites/recents and the last-playback record. It builds the
effective config, applies UI/text scale, then calls `ReadyScreen` in `layout.kt`.

`ReadyScreen` owns section navigation and passes callbacks to section composables.
Sections use `PlaybackRequest` to open `PlaybackScreen`; closing playback returns to
the same section. A pending config refresh is deferred while playback is open.

## Screens

- `Inicio` — `HomeScreen` (`home.kt`): hero (Continuar → Película → En vivo →
  empty), rows for Continuar viendo, Favoritos, En vivo ahora, Películas, Series.
- `TV` — `GistLiveBrowse` / `XtreamLiveBrowse` (`channels.kt`): rail + channel list +
  fixed preview pane. `En vivo` uses gist groups; `Canales` is a flat Xtream list.
  No section title, no inline search. Channel rows have name/group on the left and
  EPG + progress on the right; favorite stars are plain icons.
- `Películas` — `XtreamMoviesScreen` (`movies.kt`): rail, hero-on-focus, 4-column
  16:9 grid. `MovieDetail` uses the preview as the play target (`OK`), keeps only
  `Volver` in its header, and relies on long-press in the grid for favorites.
- `Series` — `XtreamSeriesScreen` (`series.kt`): same grid; `SeriesDetail` lists
  seasons and episode rows and opens a queue for next-episode playback.
- `Buscar` — `UniversalSearchScreen` (`search.kt`): fuzzy search across En vivo,
  Canales, Películas and Series; debounced, filters by type, caches the catalogue.
- `Ajustes` — `SettingsScreen` (`settings.kt`): identity card, Inicio, Reproducción,
  Interfaz, Subtítulos, Caché VOD, Red, Fuentes, Diagnóstico, Configuración.
- `Continuar` — conditional section when playback entries exist.

## UI rules

- Flat monochrome dark palette; accent `#485584`; solid fills and 1 px borders.
- No transparency except the modal scrim over video; no gradients, glass or scaling.
- The app owns text scale via `LocalDensity`; ignore the TV system font scale.
- Spanish labels. Keep focus reachable with the D-pad.
- Hero text always sits on a solid panel next to the artwork, never over a photo.
- Hints appear only where useful: TV, Continuar, Buscar and Ajustes; not on Inicio,
  Películas or Series.
- Favorite card stroke is the accent; live-row stars have no rounded container.

## Player

- One `PlayerTopBar` in `ui/PlayerChrome.kt`: title + icon-only actions. Icons are
  Canvas glyphs; no text labels.
- Actions: Info, Aspecto, Calidad, Audio/Subs; VOD adds Velocidad and Externo.
- Player chrome is visible on entry. While playing it hides after 1 s unless a bar
  button has focus. It stays visible when paused or an overlay is open, and returns
  on any key. D-pad shows/focuses the bar.
- The embedded ExoPlayer controller is disabled; media keys still play/pause and
  seek, CH+/CH− zaps live channels.
- Overlays: stream info, audio/subtitle picker, subtitle search, quality, speed,
  player controls, next episode and playback error.
- VOD: resume positions, next-episode auto-play, speed, external-player handoff.
- Cache is VOD-only; never cache live TV.

## Sources and config

- `En vivo`: curated private/secret-gist M3U feed.
- `Canales`/Películas/Series: Xtream. Live sources are independent tabs.
- Config is one JSON secret/unlisted gist URL embedded at build time. It refreshes
  on cold start, resume, source failure and manual action; no periodic timer.
- Last-known-good config and caches are encrypted with Android Keystore.
- Do not show or log URLs, credentials or playlist tokens.
- If a source breaks, offer `Actualizar configuración` and `Reintentar`; keep the
  previous config on failure.

## Startup and cache

- `Iniciar al encender` and `Reanudar al despertar` are on by default; the opt-in
  HOME alias is the fallback when Android 11 blocks boot starts.
- Cache VOD only: 100/250/500 MB, LRU, default 250 MB; optional clear-on-start.
- Last playback and resume positions are encrypted.

## Non-goals

Recording/DVR/timeshift, catch-up, downloads, multiview, full EPG grid,
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
