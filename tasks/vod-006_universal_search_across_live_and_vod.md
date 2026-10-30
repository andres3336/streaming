---
id: vod-006
title: "Universal fuzzy search across live and VOD"
priority: P2
tags: [v1.1, ui, movies, series]
epic: Shared UI
created: 2026-10-29
depends_on: [liv-004, vod-001, vod-002, vod-003]
---
# Universal fuzzy search across live and VOD

## Context

Search `En vivo`, `Canales`, Películas and Series from one screen. Accents and ñ are
folded; typos are tolerated. The Xtream catalogue is loaded on demand because the
provider can return 40k+ entries.

## Subtasks

- [x] Add a `Buscar` section and type filters
- [x] Normalize accents and ñ/n
- [x] Match exact, prefix, substring, token and bounded typos
- [x] Rank title hits above category/provider hits
- [x] Debounce input and cap results
- [x] Load all Xtream live/movies/series on demand
- [x] Show live results while movies/series are still loading
- [x] Cache the catalog for the session
- [x] Fall back to the loaded category on failure
- [x] Open movie/series detail from results

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
