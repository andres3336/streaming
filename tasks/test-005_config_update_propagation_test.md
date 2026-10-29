---
id: test-005
title: "Config update propagation test"
status: Backlog
priority: P3
tags: [v1.1, test, config]
epic: Testing
ownerType: human
created: 2026-10-29
order: 60
depends_on: [cfg-010]
---
# Config update propagation test

## Context

Verify a gist edit reaches a TV on start/resume without re-setup.

## Subtasks

- [ ] Publish new config revision
- [ ] Restart app and verify apply
- [ ] Resume app and verify apply
- [ ] Verify source cache invalidation

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
