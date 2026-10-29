---
id: test-004
title: "Cache and storage limit test"
status: Backlog
priority: P3
tags: [v1.1, test, cache]
epic: Testing
ownerType: human
created: 2026-10-29
order: 59
depends_on: [ply-010]
---
# Cache and storage limit test

## Context

Verify the app never exceeds its cache budget on the 5 GB device.

## Subtasks

- [ ] Set 100/250/500 MB
- [ ] Verify LRU eviction
- [ ] Verify clear on start
- [ ] Check free space after playback

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
