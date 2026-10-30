# Streaming

Android TV app for M3U playlists and Xtream sources.

Target: Google TV / Android TV 11, 32-bit ARM, limited storage.

## Current status

Version 0.20.3. Flat monochrome dark UI with one slate-blue accent (`#485584`),
solid fills and 1 px borders. The shell is a horizontal top navigation bar with
icon and label items, a clock and date on one line, and no duplicate screen title
or bottom hint strip. **Inicio** is the landing screen and starts with the
**Continuar viendo** row, then the hero. **TV** is one section: `_Mis canales` (the
curated M3U list) is pinned on top of the Xtream categories, with a live preview
pane. There is no EPG. Películas and Series show a compact header for the focused
title (meta and two-line synopsis) above a seven-column 2:3 poster grid; language
variants (`SU-`, `CA-`, `ES-`) are grouped into one poster and picked before
playing. Series details show the episode list and an autoplaying preview side by
side, and remember the last played episode. **Buscar** is a master search grouped
by type, cached on disk between runs. Ajustes is a list of one-row cards. Text scale
only changes type, never layout. The player bar sits at the bottom of the screen:
title, a selectable seek bar whose Left/Right steps grow 5 → 10 → 30 → 60 s, and one
row with the settings icons split around the transport (`prev`, `‹`, play/pause,
`›`, `next`), so play/pause is always centred; `prev`/`next` change episode or
channel and the seek buttons are VOD-only. Live TV zaps with D-pad Up/Down and
series episodes change with CH+/CH−.
Back returns to the top bar before it offers to exit.

## Project map

```text
CONTEXT.md     technical map for agents: files, screens, player, rules
HISTORY.md     append-only log of decisions and changes
AGENTS.md      agent working rules
.opencode/     on-demand skills and the read-only reviewer subagent
app/           Android app
config/        config schema, sample and validator docs
scripts/       build, deploy, logcat, screenshot and check scripts
ingest/        local reference dumps, gitignored and never used by the build
```

`ingest/` contains Gitingest dumps of other open-source projects used only as UX
reference. It is intentionally not tracked.

## Environment

- JDK 21 at `~/.local/opt/jdk-21` (Temurin)
- Gradle wrapper 8.14.6 (`./gradlew`)
- Android SDK at `/home/andres/Android/Sdk` (`platforms;android-36`)
- `compileSdk 36`, `targetSdk 36`, `minSdk 23`
- TV reachable over adb at `192.168.100.16:5555`

No Android Studio is required.

## Commands

```bash
./scripts/check.sh               # unit tests + debug APK
./scripts/deploy.sh              # build, install and launch on the TV
./scripts/logcat.sh              # follow the app log
./scripts/screenshot.sh out.png  # capture the TV screen
```

`scripts/env.sh` sets `JAVA_HOME`, the SDK path and the default device. Override
`ADB_DEVICE` to target another TV.

## Scope

Included:

- curated M3U live list from a private/secret gist (`_Mis canales`)
- Xtream live categories, movies and series
- remote config through a secret-gist JSON file
- cinematic stage with poster grid for movies and series, live preview pane
- universal fuzzy search, movie/series metadata detail
- subtitles: embedded, local SRT and OpenSubtitles
- ExoPlayer, stream stats, quality/buffer/speed, aspect modes, VOD cache
- Spanish-only interface, text scale, navigation layouts

Excluded:

- recording, DVR, timeshift, catch-up
- downloads and multiview
- mpv/FFmpeg, DRM, Stalker/Jellyfin
- TMDB, Trakt, Stremio, Cast, Watch Next

## Configuration

Set the raw secret-gist URL in `local.properties`:

```properties
config.url=https://gist.githubusercontent.com/USUARIO/GIST_ID/raw/config.json
```

If it is empty, the app uses the bundled demo playlist. See `config/README.md` and
`config/schema.json`.

## Secrets

Never commit:

- the config gist URL
- Xtream credentials
- `local.properties`
- signing keys

`local.properties` and `keystore.properties` are gitignored.
