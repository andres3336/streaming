---
id: test-001
title: "Parser unit tests"
status: Todo
priority: P2
tags: [v1, test]
epic: Testing
ownerType: human
created: 2026-10-29
order: 35
depends_on: [liv-001, liv-005, cfg-001]
---
# Parser unit tests

## Context

Protect the parsers that everything depends on.

## Subtasks

- [ ] Test EXTINF with attrs and commas
- [ ] Test EXTGRP and EXTVLCOPT
- [ ] Test BOM and CRLF
- [ ] Test Xtream JSON and config validation
- [ ] Run with gradlew testDebugUnitTest

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
