---
id: cfg-005
title: "Grandma mode lock with PIN"
priority: P3
tags: [v2, config, ui, security]
epic: Config
created: 2026-10-29
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

Deferred to v2. V1 has no simple mode and no PIN. The v2 PIN/no-PIN policy is not
decided yet.
See `DECISIONS.md` for project-wide scope and non-goals.
