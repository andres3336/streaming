---
id: cfg-007
title: "Config validate and publish script"
priority: P2
tags: [v1, config, docs]
epic: Config
created: 2026-10-29
depends_on: [cfg-001]
---
# Config validate and publish script

## Context

Make editing the secret gist safe and repeatable without a backend.

## Subtasks

- [ ] Validate JSON before publishing
- [ ] Print the gist edit instructions
- [ ] Never commit the config URL or credentials
- [ ] Document rollback via GitHub gist revisions

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
