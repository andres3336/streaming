---
id: cfg-010
title: "Atomic config apply and cache invalidation"
priority: P1
tags: [v1, config]
epic: Config
created: 2026-10-29
depends_on: [cfg-003, cfg-008]
---
# Atomic config apply and cache invalidation

## Context

Apply config changes safely and only clear the caches whose source identity changed.

## Subtasks

- [ ] Compare source identity before clearing caches
- [ ] Clear Xtream catalog only when its URL/user/pass changed
- [ ] Clear M3U cache only when its URL/headers changed
- [ ] Preserve favorites, recents and history
- [ ] Keep old config on parse or validation failure

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
