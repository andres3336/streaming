# Streaming

Android TV app for watching M3U playlists and Xtream sources.

Target device: Google TV / Android TV, 32-bit ARM, Android 11, limited storage.

## Current status

The Android TV app runs the real 55-channel gist playlist and Xtream movies/series,
with playback, Ajustes, local hardware overrides, VOD caching and an encrypted
last-known-good config. Refresh-on-resume, retry/reconnect and EPG are next. See
`TODO.md`.

## Task tracking

```text
TODO.md         the only source of truth for what is next and what is done
HISTORY.md      append-only log of decisions and status changes
tasks/          one Markdown spec per task, linked from TODO.md
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

- curated M3U live list from a private/secret gist
- Xtream live, movies and series
- remote config through a secret-gist JSON file
- simple locked mode for a non-technical TV
- ExoPlayer, stream stats, aspect modes, VOD cache
- Spanish-only interface

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
