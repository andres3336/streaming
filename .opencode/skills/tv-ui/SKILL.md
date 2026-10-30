---
name: TV UI
description: Use when changing any Android TV screen or shared Compose widget (Inicio, TV, Películas, Series, Buscar, Ajustes, ui/) — D-pad focus, hero/card/grid patterns, tokens, text scale, Spanish labels.
---

# TV UI

Read the `UI rules` block and the relevant `Screens` bullet in `CONTEXT.md` before
editing. They are the source of truth; this file only adds the working procedure.

## Where code goes

| Screen | File |
| --- | --- |
| Inicio / Continuar viendo | `home.kt` |
| TV (`_Mis canales` + Xtream categories) | `channels.kt` |
| Películas + detail | `movies.kt` |
| Series + episode detail | `series.kt` |
| Buscar | `search.kt` |
| Ajustes | `settings.kt` |
| Player UI | `player.kt`, `player_overlays.kt` (use the `player` skill) |
| Shell and nav | `layout.kt` |
| Reusable widgets | `ui/` |

`MainActivity.kt` stays thin. Shared state and protocol changes go through
`core.kt`; the router and common scaffolding stay in `layout.kt`. Never edit
`core.kt` and `layout.kt` in parallel with another agent.

## Invariants

- Flat monochrome dark palette, one accent `#485584`; solid fills and 1 px borders.
- No transparency except the modal scrim over video. No gradients, glass, blur or
  scaling transitions.
- Hero text sits on a solid panel beside the artwork, never over a photo.
- The app owns text scale through `LocalDensity`; ignore the TV system font scale.
- Spanish labels only. They live inline in the composables (the app is Spanish-only
  and has no locale switching); keep them Spanish and short.
- Every action must be reachable with the D-pad, and Up from content must reach the
  top bar.
- The top bar is the only place a section is named: no second title row, no bottom
  hint strip. Modals are separate windows (`ui/Modal.kt`) so focus cannot escape.
- There is no EPG anywhere: no XMLTV fetch, parsing, cache or programme column.
- Películas/Series use `CinematicBrowse` + `ui/Cinematic.kt`: a compact fixed-height
  header (title, meta, two-line synopsis) above a 7-column 2:3 poster grid with a
  rating badge top-left, favourite star top-right and two-line titles. No backdrop
  artwork and no header buttons; every header line reserves its height so the grid
  never jumps when the focused item changes.
- One poster per title: language variants are grouped with `TitleVariants` and
  selected with SUB/CAM/ESP chips in the detail. Series detail is two columns
  (episodes left, autoplaying 16:9 preview right).
- Back moves focus to the top bar; only from the bar does it offer to exit.
- Settings rows size to their content (min 44 dp) and mark the selected option with
  `✓`; no WiFi-only setting and no key-hint footers anywhere.
- Keep rows, paddings and gaps small — this is a 1080p TV viewed from the couch, not
  a touch UI. Text scale must never resize layout, only type.
- Favorite card stroke is the accent; the live-row star is a small left indicator
  with no container and long-press OK toggles it.
- Reuse `ui/AppShell.kt`, `ui/CategoryPanel.kt`, `ui/Hero.kt`, `ui/MediaCard.kt`,
  `ui/PosterGrid.kt`, `ui/LivePreview.kt`, `ui/Tokens.kt` instead of forking a widget.

## Verify

```bash
./scripts/check.sh                  # unit tests + debug APK
./scripts/deploy.sh                 # install and launch
./scripts/screenshot.sh out.png     # capture the TV
```

Then confirm the change on the TV with D-pad input; a screenshot alone does not
prove focus order. See the `tv-device` skill for key events.
