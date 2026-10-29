---
id: liv-005
title: "Xtream auth and lazy live categories"
status: Todo
priority: P1
tags: [v1, streaming, channels]
epic: Live TV
ownerType: human
created: 2026-10-29
order: 18
depends_on: [cfg-001]
---
# Xtream auth and lazy live categories

## Context

Support Xtream without ever loading the full 10k live list.

## Subtasks

- [ ] Implement player_api login/status
- [ ] Fetch live categories
- [ ] Fetch streams only for the selected category
- [ ] Cache category payloads
- [ ] Mask credentials in logs

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
