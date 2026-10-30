---
id: apk-004
title: "Auto-start on boot and wake"
priority: P2
tags: [v1.1, apk, test]
epic: Platform
created: 2026-10-29
depends_on: [apk-001, cfg-004]
---
# Auto-start on boot and wake

## Context

Start Streaming after a TV reboot and restore the last channel or VOD position after
standby, with an explicit opt-in fallback for Android builds that block background
activity starts.

## Subtasks

- [x] Add `RECEIVE_BOOT_COMPLETED` and a boot receiver
- [x] Handle `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` and quick-boot variants
- [x] Add an alarm PendingIntent fallback
- [x] Persist the last playback in an encrypted record
- [x] Restore live channel or VOD position after boot/wake
- [x] Add `Iniciar al encender` and `Reanudar al despertar` (on by default)
- [x] Add an opt-in `Usar Streaming como inicio` activity alias
- [x] Test reboot and standby on the TV

## Result

Android 11 on the target TV blocks background activity starts from both the receiver
and the alarm (`Background activity start ... isBgStartWhitelisted: false`). The
receiver is kept for TVs that allow it, and the launcher alias is the documented
fallback. The alias can be enabled from Ajustes; on this Google TV image the system
still prefers its own launcher, so the user must choose Streaming as the home app in
the TV settings if that option is available.

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
