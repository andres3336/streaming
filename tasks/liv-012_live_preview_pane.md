---
id: liv-012
title: "Live preview pane with session handoff"
status: Backlog
priority: P3
tags: [v1.1, channels, video, ui]
epic: Live TV
ownerType: human
created: 2026-10-29
order: 73
depends_on: [liv-002, ply-001, ply-011, ui-002]
---
# Live preview pane with session handoff

## Context

Review of preview playback in the three reference apps (StreamVault, OpenTV, OwnTV) on
2026-10-29. All three implement a guide/Home live preview, but their ownership models differ
and none handles the preview-to-player seam cleanly: StreamVault hands an engine across
surfaces with timeout-based ownership, OpenTV cannot adopt the preview at all, and OwnTV
promotes the same engine by URL/command equality.

This is a fast-follow feature, not required for the first deployable build. The pane is a
second consumer of the single-stream budget, so it must not compete with an active recording.

## Goal

- Selecting a channel in Live or the Guide plays a muted preview in the pane.
- Opening the previewed channel promotes the running session into the fullscreen player
  without a second prepare, decoder or provider connection.
- Back from fullscreen returns to the same pane in the originating surface.
- Backgrounding or leaving the screen stops the preview; no hidden audio or network pull.

## Design constraints

Learned from the reference review; these are the parts that must not be improvised later.

- One playback owner above navigation. Screens request sessions; they never own engines.
- Session state machine with generation tokens:
  `Idle -> Preparing -> Previewing -> HandoffPending(deadline) -> Fullscreen -> Returning(origin, deadline) -> Releasing`.
  Every transition has an owner and a cancel path.
- Handoff is accepted only on exact identity match (engine generation + channel + provider,
  plus URL or Xtream/Stalker command). Otherwise prepare a fresh session.
- Clear/cancel must release or explicitly transfer the engine. A timeout alone is not an
  ownership model.
- Mute is explicit state (volume 0 plus audio-track disable for passthrough) and is restored
  on promotion.
- A bounded open deadline (about 8–10 s) is armed for previews too; no endless auto-retry.
- Start preview after a focus/selection debounce; cancel the pending tune on blur and on
  screen exit. Show a tuning state over the old frame until the new stream's first frame.
- `ON_STOP` stops preview; `ON_START` restores only when the surface is visible. Keep the
  screen on while previewing.
- Check the provider connection budget before opening a preview: one stream at a time.
- Surface detach is identity-guarded with a generation counter.
- Clean-room implementation. OpenTV and OwnTV are GPL-3.0; StreamVault is
  source-available/non-commercial. Use them as behavior references only.

## Subtasks

- [ ] Preview pane in the Guide (and Live list if it fits the layout) with debounce/cancel
- [ ] Tuning overlay over the previous frame until the new stream renders
- [ ] Preview session state machine above navigation with deadlines and cancel paths
- [ ] Promote the preview engine into the fullscreen player without a second decoder
- [ ] Reverse handoff from player back to the originating surface
- [ ] Mute/unmute across promotion (volume plus audio track)
- [ ] Stop preview on background and screen exit; keep-screen-on while previewing
- [ ] Buffering, error and retry affordance in the pane
- [ ] Unit tests for cancel, stale session, deadline, provider mismatch and background
- [ ] On-TV check: no audio without a pane, no second stream while recording, no decoder overlap

## Acceptance criteria

- [ ] Preview starts only after the debounce; moving focus off the pane cancels a pending
      tune and stops playback.
- [ ] Selecting a previewed channel opens fullscreen using the same engine and does not issue
      a second prepare.
- [ ] Back from fullscreen resumes the preview in the originating surface (Home or Guide),
      not the other one.
- [ ] If promotion fails or navigation is cancelled, the session is released within the
      deadline and no engine is orphaned.
- [ ] Muting is reliable for AC3/DTS passthrough: no audio while the pane is muted, full
      audio after promotion.
- [ ] Backgrounding the app or leaving the screen stops preview within 1 s; no audio and no
      network pull continues.
- [ ] A stream that never delivers bytes fails within the open deadline and shows a Spanish
      retry action.
- [ ] Preview respects the single-stream budget: it does not compete with an active recording
      or fullscreen playback.

## Verification

- Unit tests for the session state machine (fake clock for deadlines, fake engine for
  identity/generation checks).
- On-device: guide focus scroll, OK promotion, Back return, Home/Guide routing, app
  background/foreground, screen off/on, dead channel, recording active.
- Record reproducible evidence in this task before moving to Review.

## Out of scope

- Home hero preview (one preview surface for now)
- Multiview tiles and their engine pool
- Picture-in-picture promotion
- Full EPG grid changes (owned by epg-003)

## Notes

Reference review was read-only; no code was taken. The board had no preview task before this
one; `ui-010` covers panel widths only. Promote the priority only if preview becomes part of
the first-install experience.
