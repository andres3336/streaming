---
id: liv-001
title: "M3U parser"
priority: P1
tags: [v1, channels, streaming]
epic: Live TV
created: 2026-10-29
depends_on: [apk-001]
---
# M3U parser

## Context

Parse the curated gist playlist without loading an entire Xtream catalog.

## Subtasks

- [ ] Parse EXTINF attributes: name, logo, group, tvg-id
- [ ] Parse EXTGRP
- [ ] Parse EXTVLCOPT User-Agent and Referer
- [ ] Handle BOM, CRLF and missing URLs
- [ ] Add unit tests for edge cases

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
