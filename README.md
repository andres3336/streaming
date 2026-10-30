# Streaming

Android TV app for watching M3U playlists and Xtream sources.

Target device: Google TV / Android TV, 32-bit ARM, Android 11, limited storage.

## Current status

Version 0.16.0 is the flat visual pass. The app now uses a monochrome dark palette
with a single slate-blue accent (`#485584`), solid fills and 1 px borders: no
transparency, gradients or motion. The shell is a horizontal top navigation bar
with icon and label items, a source chip, clock and keycap hints, plus a new **Inicio** landing screen with a hero
and rows. Live TV browses with a right-hand preview pane and the EPG below it; movies
and series show a hero for the focused poster and the synopsis under it. Live sources
are merged into one **TV** destination with `En vivo` / `Canales` chips, and the app
now owns its text scale so the TV's system font scale cannot break layouts.

Everything from 0.15.0 is still there: Xtream movies/series with metadata detail,
universal fuzzy search across all sources, VOD subtitles with OpenSubtitles and local
files, quality/buffer/speed controls, text/UI scale, VOD external-player handoff and
boot/wake auto-start settings. See `HISTORY.md`.

Android 11 on the test Google TV blocks background activity starts after boot; the
app keeps the boot receiver and offers an opt-in HOME alias as the fallback.

## Task tracking

```text
HISTORY.md      append-only log of decisions and status changes
DECISIONS.md    product and architecture decisions
tasks/          frozen archive of old specs, never read as context
archive/        frozen material (old Kandown board), never used at build time
```

Rules:

- `HISTORY.md` is the only running record; append to it, never rewrite it.
- `tasks/` is kept for history only. Do not read it as context or use it for status.
- Git history plus `HISTORY.md` is the version log.

Tags:

- `v1` — required for the first deployable build
- `v1.1` — fast follow
- `v2` — later
- area tags: `apk`, `config`, `ui`, `video`, `streaming`, `channels`, `movies`, `series`, `epg`, `cache`, `storage`, `security`, `test`

Only `v1` tasks in `Todo` are in scope until the first build is installed on the TV.
New ideas go to `Backlog` with priority `P4`.

## Environment

- JDK 21 at `~/.local/opt/jdk-21` (Temurin)
- Gradle wrapper 8.14.6 (`./gradlew`)
- Android SDK at `/home/andres/Android/Sdk` (`platforms;android-36`)
- `compileSdk 36`, `targetSdk 36`, `minSdk 23`
- TV reachable over adb at `192.168.100.16:5555`

No Android Studio is required.

## Build and deploy

```bash
./scripts/deploy.sh              # build, install and launch on the TV
./scripts/logcat.sh              # follow the app log
./scripts/screenshot.sh out.png  # capture the TV screen
```

`scripts/env.sh` sets `JAVA_HOME`, the SDK path, and the default device. Override
`ADB_DEVICE` to target another TV.

## Scope

Included:

- curated M3U live list from a private/secret gist (`En vivo`)
- Xtream live (`Canales`), movies and series
- remote config through a secret-gist JSON file
- live preview pane with EPG, hero-on-focus for movies and series
- universal fuzzy search, movie/series metadata detail
- subtitles: embedded, local SRT and OpenSubtitles
- ExoPlayer, stream stats, quality/buffer/speed, aspect modes, VOD cache
- Spanish-only interface, text/UI scale, navigation layouts

Excluded:

- recording, DVR, timeshift, catch-up
- downloads and multiview
- mpv/FFmpeg, DRM, Stalker/Jellyfin
- TMDB, Trakt, Stremio, Cast, Watch Next

See `DECISIONS.md` for details.

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
