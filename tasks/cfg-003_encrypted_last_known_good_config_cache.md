---
id: cfg-003
title: "Encrypted last-known-good config cache"
status: Todo
priority: P1
tags: [v1, config, security]
epic: Config
ownerType: human
created: 2026-10-29
order: 6
depends_on: [cfg-002]
---
# Encrypted last-known-good config cache

## Context

The app must start offline with the last valid config and never store credentials in plaintext.

## Subtasks

- [ ] Encrypt cached JSON with Android Keystore AES-GCM
- [ ] Write atomically and keep previous copy
- [ ] Fall back silently when the gist is unreachable
- [ ] Show retry screen only when no cache exists

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
