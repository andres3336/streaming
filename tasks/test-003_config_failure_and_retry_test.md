---
id: test-003
title: "Config failure and retry test"
status: Todo
priority: P2
tags: [v1, test, config]
epic: Testing
ownerType: human
created: 2026-10-29
order: 37
depends_on: [cfg-009, test-002]
---
# Config failure and retry test

## Context

Simulate a broken Xtream setup and verify the recovery flow.

## Subtasks

- [ ] Break Xtream credentials in a test config
- [ ] Confirm the Spanish update dialog appears
- [ ] Publish corrected config
- [ ] Verify update applies and playback recovers
- [ ] Confirm old config survives a failed update

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
