---
id: ply-002
title: "Retry, fallback and reconnect"
priority: P1
tags: [v1, streaming]
epic: Player
created: 2026-10-29
depends_on: [ply-001]
---
# Retry, fallback and reconnect

## Context

IPTV streams fail often. Recover gracefully and distinguish channel errors from source errors.

## Subtasks

- [ ] Retry once with backoff
- [ ] Try HLS to TS fallback where applicable
- [ ] Auto-reconnect live after drop
- [ ] Emit source-failure event for the config dialog
- [ ] Never retry a 404 forever

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
