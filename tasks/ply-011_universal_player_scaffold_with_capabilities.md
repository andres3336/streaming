---
id: ply-011
title: "Universal player scaffold with capabilities"
priority: P1
tags: [v1, video, ui, movies, series]
epic: Player
created: 2026-10-29
depends_on: [ply-003, ui-001]
---
# Universal player scaffold with capabilities

## Context

One player skeleton with conditional controls for live, movie and series.

## Subtasks

- [ ] Add MediaCapabilities: canSeek, hasEpisodes, hasEpg, isLive
- [ ] Add live slot: CH plus/minus and now/next
- [ ] Add movie slot: seekbar and resume
- [ ] Add series slot: episode selector and next episode
- [ ] Avoid separate player screens

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
