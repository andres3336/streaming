---
id: ui-011
title: "Pill shell and VOD poster grid"
priority: P2
tags: [v1.2, ui, tv, vod]
epic: Shared UI
created: 2026-10-30
depends_on: [ui-002, ui-006, vod-001, vod-002, vod-003]
---
# Pill shell and VOD poster grid

## Context

The wireframe calls for a rounded navigation pill, a compact top bar, a split
category panel and a four-column poster grid. The previous shell used a wide
text rail, a large title and full-width list rows, which felt dated on TV.

The wireframe colors were region markers, not a theme, so the app keeps its
existing dark palette. Distances were converted from 1920x1080 design pixels to
density-independent units and panels use relative weights.

## Subtasks

- [x] Rounded navigation pill with En vivo, Canales, Películas, Series,
      Continuar (when history exists), Buscar and Ajustes
- [x] Compact top bar: quick section chips, centered dynamic title, app mark
- [x] Split view for Películas/Series: 26% category panel, 74% grid
- [x] Four-column 16:9 poster grid with focus scale
- [x] Favorite star (upper left) and 2 dp gold stroke
- [x] Rating and language badges (upper right, rating first)
- [x] Language from `tvg-language` and Xtream `lang`/`language`; hidden when absent
- [x] Unified VOD favorites in the encrypted live state (`movie:`/`series:` keys)
- [x] Favoritos pseudo-category with on-demand full catalog fetch
- [x] Long-press OK toggles favorite; detail panel has a favorite chip
- [x] Explicit left/right focus routing between categories and grid
- [x] Pill-styled search filters and settings choices; player top bar chips

## Notes

- `ui.navigation` is kept in the schema for compatibility but the pill shell is
  now unconditional; the old top/rail choice was removed from Ajustes.
- The grid uses 16:9 cards; on the test TV at 1280x720 it shows three rows, and
  it scrolls for the rest. At a 4x2 target it shows at least two full rows.
- Ratings are shown as `★ 7.6` from Xtream's own score. No Rotten Tomatoes
  percentage is invented because the provider does not supply one.
- The language badge stays hidden when the provider sends no metadata.
