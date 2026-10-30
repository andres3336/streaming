# Streaming

Android TV app for watching M3U playlists and Xtream sources.

Target device: Google TV / Android TV, 32-bit ARM, Android 11, limited storage.

## Current status

Version 0.15.0 adds the v1.2 TV shell: a rounded navigation pill, compact top bar,
split category/poster-grid browsing for movies and series, unified live/VOD
favorites, and rating/language badges. The app already has separate **En vivo**
(curated gist) and **Canales** (Xtream) live tabs, Xtream movies/series with
metadata detail, universal fuzzy search across all sources, VOD subtitles with
OpenSubtitles and local files, quality/buffer/speed controls, text/UI scale,
VOD external-player handoff, and boot/wake auto-start settings. See `todo-v1.md`
and `HISTORY.md`.

Android 11 on the test Google TV blocks background activity starts after boot; the
app keeps the boot receiver and offers an opt-in HOME alias as the fallback.

## Task tracking

```text
todo-v1.md      the only source of truth for what is next and what is done
HISTORY.md      append-only log of decisions and status changes
tasks/          one Markdown spec per task, linked from todo-v1.md
archive/        frozen material (old Kandown board), never used at build time
```

Rules:

- Keep at most three items in `Now`.
- Move items between sections; do not copy them.
- Never remove or rewrite `Done` entries.
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
