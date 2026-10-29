# Streaming

Android TV app for watching M3U playlists and Xtream sources.

Target device: Google TV / Android TV, 32-bit ARM, Android 11, limited storage.

## Current status

Planning and task board. The Android app is not scaffolded yet.

Work is tracked in [Kandown](https://kandown.dev) task files:

```text
tasks/          one Markdown file per task
.kandown/       board config and local web UI
```

The board is the source of truth. Do not keep a second Kanban file.

## Board workflow

```bash
kandown init      # already done
kandown           # web board + terminal UI
kandown board     # terminal UI only
kandown list      # CLI
```

Columns:

```text
Backlog -> Todo -> In Progress -> Review -> Done
```

Tags:

- `v1` — required for the first deployable build
- `v1.1` — fast follow
- `v2` — later
- area tags: `apk`, `config`, `ui`, `video`, `streaming`, `channels`, `movies`, `series`, `epg`, `cache`, `storage`, `security`, `test`

Only `v1` tasks in `Todo` are in scope until the first build is installed on the TV.
New ideas go to `Backlog` with priority `P4`.

## Environment

- JDK 21, installed user-local
- Gradle wrapper 8.14.6
- Android SDK at `/home/andres/Android/Sdk`
- `compileSdk 36`, `targetSdk 36`, `minSdk 23`
- TV reachable over adb at `192.168.100.16:5555`

No Android Studio is required.

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

## Secrets

Never commit:

- the config gist URL
- Xtream credentials
- `local.properties`
- signing keys

`local.properties` and `keystore.properties` are gitignored.
