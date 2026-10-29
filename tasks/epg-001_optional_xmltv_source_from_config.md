---
id: epg-001
title: "Optional XMLTV source from config"
status: Backlog
priority: P3
tags: [v1.1, epg]
epic: EPG
ownerType: human
created: 2026-10-29
order: 57
depends_on: [cfg-001]
---
# Optional XMLTV source from config

## Context

Guide data is optional and configured centrally, never on the TV.

## Subtasks

- [ ] Read epgUrl from config
- [ ] Download and parse XMLTV gzip
- [ ] Cache a rolling two-day window
- [ ] Do nothing when epgUrl is absent

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
