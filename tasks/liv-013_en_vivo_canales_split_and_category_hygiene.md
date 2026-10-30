---
id: liv-013
title: "En vivo/Canales split and Xtream category hygiene"
priority: P1
tags: [v1.1, channels, ui]
epic: Live TV
created: 2026-10-29
depends_on: [liv-002, liv-005, liv-006]
---
# En vivo/Canales split and Xtream category hygiene

## Context

`En vivo` is the curated gist playlist and `Canales` is the Xtream live catalogue.
They are separate tabs and load independently. The old `liveMode` merge behavior is
retired. Xtream providers also insert divider categories and empty streams that make
the rail confusing.

## Subtasks

- [x] Separate `En vivo` (gist) and `Canales` (Xtream) tabs
- [x] Migrate old configs by adding the `channels` section
- [x] Decouple gist and Xtream failures
- [x] Check every configured source in the health check
- [x] Accept single-quoted M3U attributes and `#EXTGRP` before `#EXTINF`
- [x] Hide punctuation-only divider categories and streams
- [x] Hide categories with fewer than `minCategoryItems` usable streams after load
- [x] Add `Mostrar ocultas` and local settings for both filters
- [x] Keep `liveMode` as a legacy parsed field

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
