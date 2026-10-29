---
id: cfg-006
title: "First-run loading and retry screen"
priority: P1
tags: [v1, config, ui]
epic: Config
created: 2026-10-29
depends_on: [cfg-002]
---
# First-run loading and retry screen

## Context

First launch has no setup code. It fetches config and shows a clear Spanish retry screen on failure.

## Subtasks

- [ ] Show loading state while fetching
- [ ] Show Spanish error with Reintentar
- [ ] Do not ask for a playlist code
- [ ] Proceed automatically when config is valid

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
