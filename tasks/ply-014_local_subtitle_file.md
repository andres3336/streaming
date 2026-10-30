---
id: ply-014
title: "Local subtitle file and OpenSubtitles"
priority: P2
tags: [v1.1, video]
epic: Player
created: 2026-10-29
depends_on: [ply-006, vod-003]
---
# Local subtitle file and OpenSubtitles

## Context

Most Xtream movies do not include embedded subtitles. Attach a local SRT and, when an
OpenSubtitles API key is configured, search/download Spanish first and English as a
fallback. Exact file-hash sync is not possible for streams, so timing is adjusted
manually.

## Subtasks

- [x] Pick a local `.srt` with SAF
- [x] Detect UTF-8 BOM, UTF-8 and Windows-1252
- [x] Attach the subtitle to the current media
- [x] Cache the subtitle by media/language
- [x] Search OpenSubtitles by title, year, season/episode and IMDb/TMDB id
- [x] Download and cache the best result
- [x] Auto-download when no embedded subtitle exists
- [x] Spanish first, English fallback
- [x] Manual timing offset (`±0.5 s`, `±5 s`, reset)
- [x] Persist the offset per media
- [x] Degrade gracefully without an API key or network

## Notes

The API key, optional account and languages live in the gist (`subtitles.*`) and are
never logged. The downloaded file is cached under `files/subtitles/`.

See `DECISIONS.md` for project-wide scope and non-goals.
