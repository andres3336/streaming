---
id: cfg-004
title: "Config refresh on start, resume and failure"
priority: P1
tags: [v1, config]
epic: Config
created: 2026-10-29
depends_on: [cfg-002, cfg-003]
---
# Config refresh on start, resume and failure

## Context

No periodic timer. Check the gist on cold start, on resume, on source failure, and via a hidden manual action.

## Subtasks

- [ ] Check config on cold start
- [ ] Check config when the app resumes
- [ ] Apply new config at a safe point, never mid-playback
- [ ] Add hidden Actualizar configuracion action
- [ ] Support forceUpdate blocking prompt

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
