---
name: Player
description: Use when touching playback, ExoPlayer, player.kt, player_overlays.kt, ui/PlayerChrome.kt, tracks/subtitles, VOD cache, resume positions or playback errors on the TV.
---

# Player

Read the `Player` block and the `Data flow` block in `CONTEXT.md` first. The player
is the largest area of the app (~2,265 lines across three files); do not restructure
it in the middle of an unrelated change.

## Map

| Concern | File |
| --- | --- |
| `PlaybackScreen`, player construction, stats, errors | `player.kt` |
| Stats / tracks / quality / speed / controls / subtitles / next / error overlays | `player_overlays.kt` |
| Bottom player bar chrome, Canvas icons | `ui/PlayerChrome.kt` |
| Audio/subtitle choice mapping | `player/TrackChoices.kt` |
| Resume + last playback state | `data/playback/` |
| VOD cache | `data/cache/` |
| Subtitles (embedded, SRT, OpenSubtitles) | `data/subtitles/` |

## Invariants

- One `PlayerBar`, anchored at the bottom of the screen, no top overlay. Icon-only
  actions drawn as Canvas glyphs, no text labels.
- The bar is three rows shown together: title; a selectable seek bar with
  elapsed/duration (Left/Right step the seek ladder); and one transport row with the
  action icons split evenly around the centre, where play/pause lives. Centring is
  done with two equal-weight side clusters, so it holds for any `uiConfig.playerButtons`.
- Transport reads `prev · ‹ · play/pause · › · next`. `prev`/`next` jump one item
  (episode on a series queue, channel on live) and an unavailable neighbour keeps its
  42 dp slot as a dimmed, non-focusable icon, so the row is symmetric and play/pause
  is always on the exact centre. `‹`/`›` are the seek buttons, VOD-only (`canSeek`).
- Seeks share one ladder (seek bar Left/Right and `‹`/`›`): 5 s, 10 s, 30 s, then
  60 s per press; the step survives a direction change and resets to 5 s after 1.5 s
  idle. Media keys stay at 30 s.
- The bar is visible on entry and hides after 1 s while playing unless a control has
  focus. Back hides the bar when a control has focus; from the video it closes
  playback.
- On live TV the seek row is inert, so Up/Down zap (when `zapping` is on and the
  list has more than one channel) even while the bar has focus; on VOD they move
  between the seek and transport rows, and Left/Right/Center reveal/focus. Series
  queues change episode with CH+/CH− and Up/Down. Live titles read `CH5 - Peppa`.
- Media3's default `SubtitleView` renders captions at the bottom, so the bar covers
  them while visible; that is accepted, do not move captions.
- The embedded ExoPlayer controller stays disabled; the custom bar is the only chrome.
- Media keys play/pause. `MEDIA_NEXT` / `MEDIA_PREVIOUS` seek 30 s in VOD and zap in
  live. `CHANNEL_UP` / `CHANNEL_DOWN` zap live only when `liveConfig.zapping` is on.
- The player background and letterbox bars are pure black; never the app background
  or the accent. Check with a 4:3 or pillarboxed stream, not just a 16:9 one.
- Cache is VOD-only; never cache live TV.
- Resume positions, last playback and config caches are encrypted (Keystore).
- External-player handoff is VOD-only.
- Never print or log stream URLs, Xtream credentials or playlist tokens — including
  in error overlays, stats and `ErrorLogStore`.

## Verify

Open both a VOD item and a live channel on the TV:

```bash
./scripts/check.sh
./scripts/deploy.sh
./scripts/logcat.sh          # in a second shell
```

- Entry shows the bar at the bottom with play/pause centred; it hides ~1 s into
  playback; any key brings it back.
- D-pad focuses the bar first (play/pause), Left/Right reach the side icon clusters,
  and on VOD Up reaches the seek bar; Back leaves the player.
- `‹`/`›` are absent on live and step the ladder (5, 10, 30, 60 s) on VOD;
  `prev`/`next` switch episode on a series queue and channel on live.
- Play/pause and seek work from media keys; CH+/CH− zaps only on live.
- VOD resume works after leaving and reopening; live is never cached.
