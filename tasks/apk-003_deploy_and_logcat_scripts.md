---
id: apk-003
title: "Deploy and logcat scripts"
status: Todo
priority: P2
tags: [v1, apk, test]
epic: APK
ownerType: human
created: 2026-10-29
order: 3
depends_on: [apk-001]
---
# Deploy and logcat scripts

## Context

Create repeatable CLI deployment for the TV at 192.168.100.16:5555 without Android Studio.

## Subtasks

- [ ] Add scripts/env.sh with JAVA_HOME and SDK paths
- [ ] Add scripts/deploy.sh: connect, assemble, install, launch
- [ ] Add scripts/logcat.sh filtered by package
- [ ] Document usage in README

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
