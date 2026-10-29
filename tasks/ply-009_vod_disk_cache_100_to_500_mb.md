---
id: ply-009
title: "VOD disk cache 100 to 500 MB"
status: Todo
priority: P1
tags: [v1, cache, video]
epic: Player
ownerType: human
created: 2026-10-29
order: 28
depends_on: [ply-001, cfg-001]
---
# VOD disk cache 100 to 500 MB

## Context

Cache only VOD. Live TV is never cached because it does not improve transmission.

## Subtasks

- [ ] Add Media3 SimpleCache with LRU eviction
- [ ] Offer 100, 250 and 500 MB
- [ ] Use CacheDataSource for movies and series only
- [ ] Keep live playback uncached
- [ ] Respect the 5 GB device budget

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
