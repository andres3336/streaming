---
id: cfg-005
title: "Grandma mode lock with PIN"
status: Todo
priority: P1
tags: [v1, config, ui, security]
epic: Config
ownerType: human
created: 2026-10-29
order: 8
depends_on: [cfg-001, ui-003]
---
# Grandma mode lock with PIN

## Context

A local, PIN-protected simple mode so one TV shows only the curated gist live list.

## Subtasks

- [ ] Persist local deviceMode simple/full
- [ ] Hash and verify the admin PIN
- [ ] Hide settings, sections and source switching in simple mode
- [ ] Add hidden gesture to unlock
- [ ] Keep credentials central, only UI state local

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
