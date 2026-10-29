---
id: cfg-009
title: "Update-config dialog and retry flow"
priority: P1
tags: [v1, config, ui]
epic: Config
created: 2026-10-29
depends_on: [cfg-008, cfg-004]
---
# Update-config dialog and retry flow

## Context

When Xtream or the M3U source breaks, offer to fetch the latest credentials and retry.

## Subtasks

- [ ] Show Spanish two-button dialog
- [ ] Fetch config again on Actualizar
- [ ] Refresh affected source and retry
- [ ] Keep old config if update fails
- [ ] Do not expose technical details in simple mode

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
