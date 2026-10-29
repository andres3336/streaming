---
id: cfg-008
title: "Source health check"
priority: P1
tags: [v1, config, streaming]
epic: Config
created: 2026-10-29
depends_on: [cfg-002, liv-001, liv-005]
---
# Source health check

## Context

Detect source-level failures without prompting for every dead channel.

## Subtasks

- [ ] Check Xtream login/status
- [ ] Check gist M3U download
- [ ] Only check enabled sources for the current mode
- [ ] Classify channel failure versus source failure

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
