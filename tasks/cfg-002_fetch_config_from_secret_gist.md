---
id: cfg-002
title: Fetch config from secret gist
status: Todo
priority: P1
tags: [v1, config, security]
epic: Config
ownerType: human
created: 2026-10-29
order: 5
depends_on: [cfg-001]
updated: 2026-10-29T20:40:26Z
---

# Fetch config from secret gist

## Context

Load the central config from a permanent secret-gist raw URL embedded at build time.

## Subtasks

- [ ] Add BuildConfig CONFIG_URL sourced from untracked local.properties
- [ ] Fetch with OkHttp and If-None-Match
- [ ] Parse and validate JSON
- [ ] Never log credentials or full URLs
- [ ] Expose a ConfigRepository API

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
