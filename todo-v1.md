# Streaming — V1 reconciled TODO

**Last reconciled:** 2026-10-29  
**App:** Streaming 0.14.1 (`versionCode 16`)  
**TV:** `192.168.100.16:5555` — MOTOROLA GOOGLE TV, Android 11 / API 30, armeabi-v7a  
**Specs:** 76 tasks in `tasks/`  
**Change log:** `HISTORY.md`  

This is the single source of truth for V1. `tasks/` holds the detailed specs; this file
holds the reconciled status. The old Kandown board is frozen in `archive/kandown/`.

## How to use this file

1. Read this file and `HISTORY.md` before starting.
2. Work top-down in **Now**; keep at most three items there.
3. Read the linked spec in `tasks/` before changing code.
4. Build and install with `./scripts/deploy.sh`.
5. After work: update this file, append to `HISTORY.md`, and commit.
6. Never commit `local.properties`, config URLs, playlist URLs or Xtream credentials.

## Current state

- **Live:** separate **En vivo** (curated gist) and **Canales** (Xtream) tabs;
  lazy categories with divider/empty filtering; group rail; channel numbers;
  logos/initials; favorites, recents, last channel; CH+/CH− and media-key zapping;
  channel search; optional XMLTV guide with now/next in rows (`ui.showNowNext`).
- **Movies:** Xtream categories, posters, metadata detail (plot/year/rating/genre/
  cast), playback (1920x960 verified).
- **Series:** Xtream categories, metadata, seasons/episodes, playback,
  next-episode queue.
- **Search:** universal fuzzy search across En vivo, Canales, Películas and Series;
  accents/ñ folded; typo tolerance; 47k+ Xtream entries on demand.
- **Player:** audio/subtitle pickers, local SRT + OpenSubtitles auto-download with
  timing offsets, aspect modes, HLS→TS fallback, quality cap/manual variant,
  buffer/pre-buffer/low-latency, VOD speed, media keys, stream info, external player
  for VOD only.
- **Config:** permanent gist, no-cache fetch, encrypted last-known-good config and
  offline playlist cache, source identities for selective invalidation, refresh on
  resume, manual update, source health check, update-config retry dialog,
  forceUpdate prompt, validate/publish script, v3 sources/subtitles/UI schema.
- **Ajustes:** buffer/pre-buffer/quality/latency, aspect, VOD cache, storage usage,
  WiFi/Ethernet-only, max bitrate, source health, category filtering, text/UI scale,
  top/rail navigation, full/compact player bar, visible player buttons, subtitle
  auto-download, boot/wake auto-start, error log.
- **Playback:** ExoPlayer HLS/TS/MP4, headers, retry/backoff, live auto-reconnect,
  Spanish error overlay, VOD cache, resume/Continue, next-episode countdown.
- **Diagnostics:** in-player stream info (including effective buffer settings),
  bounded sanitized error log, host-only source masking.
- **Security:** config cache, playback positions, last playback, live state, playlist
  cache, subtitle offsets and Xtream catalog cache use Keystore AES-GCM; errors do
  not log/display URLs or keys.
- **Tests:** 124 unit tests passing.

## Now (max 3)

- [ ] None — v1.1 is complete.

## Next (V1.2)

- [ ] No committed v1.2 items yet. Candidates: themes/font family (`ui-008`,
  `ui-009`), full EPG grid (`epg-003`), recording/catch-up (out of scope).

## Done

- [x] 2026-10-29 · `apk-004` · Auto-start on boot and wake · [spec](tasks/apk-004_auto_start_on_boot_and_wake.md) — Receiver + alarm attempted; Android 11 blocks background starts on this TV; launcher alias opt-in added.
- [x] 2026-10-29 · `liv-013` · En vivo/Canales split and Xtream category hygiene · [spec](tasks/liv-013_en_vivo_canales_split_and_category_hygiene.md) — Separate tabs, independent failures, parser hardening, divider/empty filtering with “Mostrar ocultas”.
- [x] 2026-10-29 · `ui-001` · Universal MediaItem and capabilities model · [spec](tasks/ui-001_universal_mediaitem_and_capabilities_model.md) — `MediaKind`, `MediaItem`, `MediaCapabilities`; `Channel` remains the source DTO.
- [x] 2026-10-29 · `ply-011` · Universal player scaffold with capabilities · [spec](tasks/ply-011_universal_player_scaffold_with_capabilities.md) — One player gated by capabilities; quality/speed/external only when supported.
- [x] 2026-10-29 · `ply-012` · Quality cap and HLS variant selector · [spec](tasks/ply-012_quality_cap_and_hls_variant_selector.md) — Auto plus manual HLS variant preference, persisted; single-quality explanation.
- [x] 2026-10-29 · `ply-013` · Buffer and latency settings · [spec](tasks/ply-013_buffer_and_latency_settings.md) — Start pre-buffer, after-rebuffer pre-buffer, optional live low latency, values in Info.
- [x] 2026-10-29 · `ply-017` · Playback speed and media keys · [spec](tasks/ply-017_playback_speed_and_media_keys.md) — VOD 0.5–2x, play/pause, ±30 s next/previous; live keeps zapping.
- [x] 2026-10-29 · `ply-014` · Local subtitle file and OpenSubtitles · [spec](tasks/ply-014_local_subtitle_file.md) — SAF SRT, OpenSubtitles auto-search es→en, encrypted offsets, encoding detection.
- [x] 2026-10-29 · `ply-015` · External player handoff · [spec](tasks/ply-015_external_player_handoff.md) — ACTION_VIEW + headers for movies/series only; live stays internal.
- [x] 2026-10-29 · `ui-005` · Font size and UI scale · [spec](tasks/ui-005_font_size_and_ui_scale.md) — Text and UI scale applied through `LocalDensity`, persisted locally.
- [x] 2026-10-29 · `ui-006` · Navigation rail and player bar layouts · [spec](tasks/ui-006_navigation_rail_and_player_bar_layouts.md) — Top/rail navigation, full/compact player bar, visible button selection.
- [x] 2026-10-29 · `vod-003` · Xtream metadata for movies and series · [spec](tasks/vod-003_xtream_metadata_for_movies_and_series.md) — Plot/year/rating/duration/genre/cast/backdrop; reusable detail panels.
- [x] 2026-10-29 · `vod-006` · Universal fuzzy search across live and VOD · [spec](tasks/vod-006_universal_search_across_live_and_vod.md) — En vivo/Canales/Películas/Series, accents/ñ, typos, on-demand 47k+ catalog.

- [x] 2026-10-29 · `apk-001` · Toolchain and Android scaffold · [spec](tasks/apk-001_toolchain_and_android_scaffold.md) — JDK 21, Gradle 8.14.6 wrapper, SDK 36, first debug APK.
- [x] 2026-10-29 · `apk-002` · TV manifest, banner and launcher assets · [spec](tasks/apk-002_tv_manifest_banner_and_launcher_assets.md) — Leanback manifest, 320x180 banner, launcher icon, landscape.
- [x] 2026-10-29 · `apk-003` · Deploy and logcat scripts · [spec](tasks/apk-003_deploy_and_logcat_scripts.md) — env.sh, deploy.sh, logcat.sh, screenshot.sh.
- [x] 2026-10-29 · `cfg-001` · Config schema and validator · [spec](tasks/cfg-001_config_schema_and_validator.md) — v2 schema, validator, local overrides, 11 parser tests.
- [x] 2026-10-29 · `cfg-002` · Fetch config from secret gist · [spec](tasks/cfg-002_fetch_config_from_secret_gist.md) — Permanent gist URL, ETag, no-cache, 5 repository tests.
- [x] 2026-10-29 · `cfg-003` · Encrypted last-known-good config cache · [spec](tasks/cfg-003_encrypted_last_known_good_config_cache.md) — Keystore AES-GCM cache, atomic write, offline config fallback verified.
- [x] 2026-10-29 · `cfg-006` · First-run loading and retry screen · [spec](tasks/cfg-006_first_run_loading_and_retry_screen.md) — Loading, Spanish error and retry screens.
- [x] 2026-10-29 · `liv-001` · M3U parser · [spec](tasks/liv-001_m3u_parser.md) — EXTINF/EXTGRP/EXTVLCOPT, BOM/CRLF, 10 tests.
- [x] 2026-10-29 · `ply-001` · ExoPlayer core with headers · [spec](tasks/ply-001_exoplayer_core_with_headers.md) — Media3 HLS/TS/MP4, per-channel headers, lifecycle, screen-on.
- [x] 2026-10-29 · `ply-003` · Player HUD, auto-hide and keep screen on · [spec](tasks/ply-003_player_hud_auto_hide_and_keep_screen_on.md) — PlayerView HUD/auto-hide and keep-screen-on.
- [x] 2026-10-29 · `ply-009` · VOD disk cache 100 to 500 MB · [spec](tasks/ply-009_vod_disk_cache_100_to_500_mb.md) — VOD-only SimpleCache 100/250/500 MB; live never cached; verified ~13 MB.
- [x] 2026-10-29 · `test-001` · Parser unit tests · [spec](tasks/test-001_parser_unit_tests.md) — 10 M3U parser tests; 43 tests total across the project.
- [x] 2026-10-29 · `test-002` · On-TV smoke test and first deploy · [spec](tasks/test-002_on_tv_smoke_test_and_first_deploy.md) — On-TV smoke test and first deploy; grandma mode moved to v2.
- [x] 2026-10-29 · `test-003` · Config failure and retry test · [spec](tasks/test-003_config_failure_and_retry_test.md) — Config cache fallback and retry verified with unreachable URL.
- [x] 2026-10-29 · `ui-004` · Spanish-only strings and locale · [spec](tasks/ui-004_spanish_only_strings_and_locale.md) — Spanish-only UI; strings still live in code (extraction optional).
- [x] 2026-10-29 · `ui-007` · Settings hub behind PIN · [spec](tasks/ui-007_settings_hub_behind_pin.md) — Visible Ajustes, no PIN, per product decision.
- [x] 2026-10-29 · `vod-001` · Xtream Movies browse · [spec](tasks/vod-001_xtream_movies_browse.md) — Core categories/posters/playback; grid and detail panel deferred.
- [x] 2026-10-29 · `vod-002` · Xtream Series browse · [spec](tasks/vod-002_xtream_series_browse.md) — Core categories/seasons/episodes/playback; season tabs/detail panel deferred.
- [x] 2026-10-29 · `vod-004` · Resume and Continue watching · [spec](tasks/vod-004_resume_and_continue_watching.md) — Resume positions + Continue tab; 0:17 resume verified.
- [x] 2026-10-29 · `vod-005` · Next episode auto-play · [spec](tasks/vod-005_next_episode_auto_play.md) — 10 s countdown + Reproducir ahora/Cancelar; on/off setting pending.
- [x] 2026-10-29 · `liv-003` · Favorites, recents and last channel · [spec](tasks/liv-003_favorites_recents_and_last_channel.md) — Encrypted live state, Favoritos/Recientes rows, last-channel focus.
- [x] 2026-10-29 · `ply-004` · Channel zapping and last channel · [spec](tasks/ply-004_channel_zapping_and_last_channel.md) — CH+/CH− and media next/previous, filtered queue with wrap.
- [x] 2026-10-29 · `liv-004` · Channel search · [spec](tasks/liv-004_channel_search.md) — Accent-insensitive name/group search, query persistence, no-results state.
- [x] 2026-10-29 · `ply-006` · Audio and subtitle track pickers · [spec](tasks/ply-006_audio_and_subtitle_track_pickers.md) — Spanish labels, immediate apply, explicit Desactivado state.
- [x] 2026-10-29 · `ply-007` · Stream info and bandwidth estimate · [spec](tasks/ply-007_stream_info_and_bandwidth_estimate.md) — Codec, resolution, FPS, bitrate, buffer, dropped frames, decoder, bandwidth; host-only source.
- [x] 2026-10-29 · `cfg-008` · Source health check · [spec](tasks/cfg-008_source_health_check.md) — M3U and Xtream checks, mode-aware, manual check in Ajustes.
- [x] 2026-10-29 · `cfg-009` · Update-config dialog and retry flow · [spec](tasks/cfg-009_update_config_dialog_and_retry_flow.md) — Spanish two-button recovery, forced config fetch, old config retained on failure.
- [x] 2026-10-29 · `cfg-004` · Config refresh on start, resume and failure · [spec](tasks/cfg-004_config_refresh_on_start_resume_and_failure.md) — Start/resume/manual refresh, safe apply and forceUpdate blocking prompt.
- [x] 2026-10-29 · `cfg-016` · Offline playlist cache · [spec](tasks/cfg-016_offline_playlist_cache.md) — Encrypted channel cache with source identity; verified cold start with playlist host down.
- [x] 2026-10-29 · `liv-005` · Xtream auth and lazy live categories · [spec](tasks/liv-005_xtream_auth_and_lazy_live_categories.md) — Login/status, live categories, lazy streams, encrypted category payloads.
- [x] 2026-10-29 · `liv-006` · Live source mode gist, Xtream or merge · [spec](tasks/liv-006_live_source_mode_gist_xtream_or_merge.md) — Mode-aware sources, lazy Xtream, merge with dedupe and provider labels.
- [x] 2026-10-29 · `cfg-007` · Config validate and publish script · [spec](tasks/cfg-007_config_validate_and_publish_script.md) — Schema validation, publish instructions and gist rollback docs.
- [x] 2026-10-29 · `test-004` · Cache and storage limit test · [spec](tasks/test-004_cache_and_storage_limit_test.md) — 100/250/500 MB controls, usage display, clear-on-start verified 26 MB → 0.
- [x] 2026-10-29 · `test-005` · Config update propagation test · [spec](tasks/test-005_config_update_propagation_test.md) — Gist/config changes applied on restart and resume; source cache invalidated.
- [x] 2026-10-29 · `liv-002` · Channel list, group rail and logos · [spec](tasks/liv-002_channel_list_group_rail_and_logos.md) — Group rail, channel numbers, stable keys, logos/initials and favorite state.
- [x] 2026-10-29 · `ply-002` · Retry, fallback and reconnect · [spec](tasks/ply-002_retry_fallback_and_reconnect.md) — Bounded retry, HLS→TS fallback, auto-reconnect and source-failure recovery.
- [x] 2026-10-29 · `ply-005` · Aspect ratio and zoom modes · [spec](tasks/ply-005_aspect_ratio_and_zoom_modes.md) — Fit/fill/stretch/zoom/original/16:9/4:3 with in-player control and persistence.
- [x] 2026-10-29 · `ply-008` · Error overlay and diagnostics · [spec](tasks/ply-008_error_overlay_and_diagnostics.md) — Spanish overlay, bounded sanitized error log, no URLs.
- [x] 2026-10-29 · `ply-010` · Cache controls and storage usage · [spec](tasks/ply-010_cache_controls_and_storage_usage.md) — Usage in MB, clear now, clear on start, live-not-cached note.
- [x] 2026-10-29 · `cfg-010` · Atomic config apply and cache invalidation · [spec](tasks/cfg-010_atomic_config_apply_and_cache_invalidation.md) — Source identities invalidate only the affected M3U/Xtream cache; live state preserved.
- [x] 2026-10-29 · `ui-002` · Reusable browse shell · [spec](tasks/ui-002_reusable_browse_shell.md) — Shared shell, category rail, poster row, optional detail slot and focus handling.
- [x] 2026-10-30 · `epg-001` · Optional XMLTV source from config · [spec](tasks/epg-001_optional_xmltv_source_from_config.md) — Optional XMLTV/gzip, SAX parser, encrypted rolling two-day cache.
- [x] 2026-10-30 · `epg-002` · Now and next in channel rows · [spec](tasks/epg-002_now_and_next_in_channel_rows.md) — tvg-id then display-name matching, now/time-left/next, hidden when absent.

## Deferred — v2

- `liv-007` · Multiple playlists and switcher · [spec](tasks/liv-007_multiple_playlists_and_switcher.md) — Not needed; En vivo is the curated gist and Canales is Xtream.

- `cfg-005` · Grandma mode lock with PIN · [spec](tasks/cfg-005_grandma_mode_lock_with_pin.md) — Abuela/grandma mode deferred to v2; PIN policy TBD.
- `cfg-011` · Config history and rollback · [spec](tasks/cfg-011_config_history_and_rollback.md)
- `cfg-012` · Hidden sync and config diagnostics · [spec](tasks/cfg-012_hidden_sync_and_config_diagnostics.md)
- `cfg-013` · Push notification for config updates · [spec](tasks/cfg-013_push_notification_for_config_updates.md)
- `cfg-014` · Local admin dashboard · [spec](tasks/cfg-014_local_admin_dashboard.md)
- `cfg-015` · Backup and restore · [spec](tasks/cfg-015_backup_and_restore.md)
- `epg-003` · Full TV guide grid · [spec](tasks/epg-003_full_tv_guide_grid.md)
- `liv-008` · Category hide, rename and reorder · [spec](tasks/liv-008_category_hide_rename_and_reorder.md) — Conflicts with the gist as the single remote source; revisit only if local overrides become canonical.
- `liv-009` · Hide individual channels · [spec](tasks/liv-009_hide_individual_channels.md) — Conflicts with the gist as the single remote source; revisit only if local overrides become canonical.
- `liv-010` · Custom groups and pinned categories · [spec](tasks/liv-010_custom_groups_and_pinned_categories.md)
- `liv-011` · Advanced filters and bulk rename · [spec](tasks/liv-011_advanced_filters_and_bulk_rename.md)
- `liv-012` · Live preview pane with session handoff · [spec](tasks/liv-012_live_preview_pane.md) — Not required for the product; keep as a reference design.
- `ply-016` · Sleep timer · [spec](tasks/ply-016_sleep_timer.md) — Not required for the product.
- `ply-018` · Picture-in-picture · [spec](tasks/ply-018_picture_in_picture.md)
- `ply-019` · Audio-only mode · [spec](tasks/ply-019_audio_only_mode.md)
- `ui-003` · Simple and Full UI modes · [spec](tasks/ui-003_simple_and_full_ui_modes.md)
- `ui-008` · Themes and accent color · [spec](tasks/ui-008_themes_and_accent_color.md)
- `ui-009` · Font family and popup size · [spec](tasks/ui-009_font_family_and_popup_size.md)
- `ui-010` · Live layout variants and panel widths · [spec](tasks/ui-010_live_layout_variants_and_panel_widths.md)

## Full ledger

| ID | Task | Status | Note |
|---|---|---|---|
| [`apk-001`](tasks/apk-001_toolchain_and_android_scaffold.md) | Toolchain and Android scaffold | Done | JDK 21, Gradle 8.14.6 wrapper, SDK 36, first debug APK. |
| [`apk-002`](tasks/apk-002_tv_manifest_banner_and_launcher_assets.md) | TV manifest, banner and launcher assets | Done | Leanback manifest, 320x180 banner, launcher icon, landscape. |
| [`apk-003`](tasks/apk-003_deploy_and_logcat_scripts.md) | Deploy and logcat scripts | Done | env.sh, deploy.sh, logcat.sh, screenshot.sh. |
| [`apk-004`](tasks/apk-004_auto_start_on_boot_and_wake.md) | Auto-start on boot and wake | Done | Receiver + alarm; Android 11 blocks background starts; launcher alias opt-in. |
| [`cfg-001`](tasks/cfg-001_config_schema_and_validator.md) | Config schema and validator | Done | v2 schema, validator, local overrides, 11 parser tests. |
| [`cfg-002`](tasks/cfg-002_fetch_config_from_secret_gist.md) | Fetch config from secret gist | Done | Permanent gist URL, ETag, no-cache, 5 repository tests. |
| [`cfg-003`](tasks/cfg-003_encrypted_last_known_good_config_cache.md) | Encrypted last-known-good config cache | Done | Keystore AES-GCM cache, atomic write, offline config fallback verified. |
| [`cfg-006`](tasks/cfg-006_first_run_loading_and_retry_screen.md) | First-run loading and retry screen | Done | Loading, Spanish error and retry screens. |
| [`liv-001`](tasks/liv-001_m3u_parser.md) | M3U parser | Done | EXTINF/EXTGRP/EXTVLCOPT, BOM/CRLF, 10 tests. |
| [`ply-001`](tasks/ply-001_exoplayer_core_with_headers.md) | ExoPlayer core with headers | Done | Media3 HLS/TS/MP4, per-channel headers, lifecycle, screen-on. |
| [`ply-003`](tasks/ply-003_player_hud_auto_hide_and_keep_screen_on.md) | Player HUD, auto-hide and keep screen on | Done | PlayerView HUD/auto-hide and keep-screen-on. |
| [`ply-009`](tasks/ply-009_vod_disk_cache_100_to_500_mb.md) | VOD disk cache 100 to 500 MB | Done | VOD-only SimpleCache 100/250/500 MB; live never cached; verified ~13 MB. |
| [`test-001`](tasks/test-001_parser_unit_tests.md) | Parser unit tests | Done | 10 M3U parser tests; 43 tests total across the project. |
| [`test-002`](tasks/test-002_on_tv_smoke_test_and_first_deploy.md) | On-TV smoke test and first deploy | Done | On-TV smoke test and first deploy; grandma mode moved to v2. |
| [`test-003`](tasks/test-003_config_failure_and_retry_test.md) | Config failure and retry test | Done | Config cache fallback and retry verified with unreachable URL. |
| [`ui-004`](tasks/ui-004_spanish_only_strings_and_locale.md) | Spanish-only strings and locale | Done | Spanish-only UI; strings still live in code (extraction optional). |
| [`ui-007`](tasks/ui-007_settings_hub_behind_pin.md) | Settings hub behind PIN | Done | Visible Ajustes, no PIN, per product decision. |
| [`vod-001`](tasks/vod-001_xtream_movies_browse.md) | Xtream Movies browse | Done | Core categories/posters/playback; grid and detail panel deferred. |
| [`vod-002`](tasks/vod-002_xtream_series_browse.md) | Xtream Series browse | Done | Core categories/seasons/episodes/playback; season tabs/detail panel deferred. |
| [`vod-004`](tasks/vod-004_resume_and_continue_watching.md) | Resume and Continue watching | Done | Resume positions + Continue tab; 0:17 resume verified. |
| [`vod-005`](tasks/vod-005_next_episode_auto_play.md) | Next episode auto-play | Done | 10 s countdown + Reproducir ahora/Cancelar; on/off setting pending. |
| [`liv-003`](tasks/liv-003_favorites_recents_and_last_channel.md) | Favorites, recents and last channel | Done | Encrypted live state, Favoritos/Recientes rows, last-channel focus. |
| [`ply-004`](tasks/ply-004_channel_zapping_and_last_channel.md) | Channel zapping and last channel | Done | CH+/CH− and media next/previous, filtered queue with wrap. |
| [`liv-004`](tasks/liv-004_channel_search.md) | Channel search | Done | Accent-insensitive name/group search, query persistence, no-results state. |
| [`ply-006`](tasks/ply-006_audio_and_subtitle_track_pickers.md) | Audio and subtitle track pickers | Done | Spanish labels, immediate apply, explicit Desactivado state. |
| [`ply-007`](tasks/ply-007_stream_info_and_bandwidth_estimate.md) | Stream info and bandwidth estimate | Done | Codec, resolution, FPS, bitrate, buffer, dropped frames, decoder, bandwidth; host-only source. |
| [`cfg-004`](tasks/cfg-004_config_refresh_on_start_resume_and_failure.md) | Config refresh on start, resume and failure | Done | Start/resume/manual refresh, safe apply and forceUpdate blocking prompt. |
| [`cfg-008`](tasks/cfg-008_source_health_check.md) | Source health check | Done | M3U and Xtream checks, mode-aware, manual check in Ajustes. |
| [`cfg-009`](tasks/cfg-009_update_config_dialog_and_retry_flow.md) | Update-config dialog and retry flow | Done | Spanish two-button recovery, forced config fetch, old config retained on failure. |
| [`cfg-016`](tasks/cfg-016_offline_playlist_cache.md) | Offline playlist cache | Done | Encrypted channel cache with source identity; offline start verified. |
| [`liv-005`](tasks/liv-005_xtream_auth_and_lazy_live_categories.md) | Xtream auth and lazy live categories | Done | Login/status, live categories, lazy streams, encrypted category payloads. |
| [`liv-006`](tasks/liv-006_live_source_mode_gist_xtream_or_merge.md) | Live source mode gist, Xtream or merge | Done | Mode-aware sources, lazy Xtream, merge with dedupe and provider labels. |
| [`cfg-007`](tasks/cfg-007_config_validate_and_publish_script.md) | Config validate and publish script | Done | Schema validation, publish instructions and gist rollback docs. |
| [`test-004`](tasks/test-004_cache_and_storage_limit_test.md) | Cache and storage limit test | Done | 100/250/500 MB controls, usage display, clear-on-start verified 26 MB → 0. |
| [`test-005`](tasks/test-005_config_update_propagation_test.md) | Config update propagation test | Done | Changes applied on restart and resume; source cache invalidated. |
| [`liv-002`](tasks/liv-002_channel_list_group_rail_and_logos.md) | Channel list, group rail and logos | Done | Group rail, channel numbers, stable keys, logos/initials and favorite state. |
| [`ply-002`](tasks/ply-002_retry_fallback_and_reconnect.md) | Retry, fallback and reconnect | Done | Bounded retry, HLS→TS fallback, auto-reconnect and source-failure recovery. |
| [`ply-005`](tasks/ply-005_aspect_ratio_and_zoom_modes.md) | Aspect ratio and zoom modes | Done | Fit/fill/stretch/zoom/original/16:9/4:3 with in-player control and persistence. |
| [`ply-008`](tasks/ply-008_error_overlay_and_diagnostics.md) | Error overlay and diagnostics | Done | Spanish overlay, bounded sanitized error log, no URLs. |
| [`ply-010`](tasks/ply-010_cache_controls_and_storage_usage.md) | Cache controls and storage usage | Done | Usage in MB, clear now, clear on start, live-not-cached note. |
| [`cfg-010`](tasks/cfg-010_atomic_config_apply_and_cache_invalidation.md) | Atomic config apply and cache invalidation | Done | Source identities invalidate only the affected cache; live state preserved. |
| [`ui-002`](tasks/ui-002_reusable_browse_shell.md) | Reusable browse shell | Done | Shared shell, category rail, poster row, optional detail slot and focus handling. |
| [`epg-001`](tasks/epg-001_optional_xmltv_source_from_config.md) | Optional XMLTV source from config | Done | Optional XMLTV/gzip, SAX parser, encrypted rolling two-day cache. |
| [`epg-002`](tasks/epg-002_now_and_next_in_channel_rows.md) | Now and next in channel rows | Done | tvg-id then display-name matching, now/time-left/next, hidden when absent. |
| [`liv-007`](tasks/liv-007_multiple_playlists_and_switcher.md) | Multiple playlists and switcher | v2 | Not needed: En vivo is gist-only and Canales is Xtream-only. |
| [`liv-008`](tasks/liv-008_category_hide_rename_and_reorder.md) | Category hide, rename and reorder | v2 | Conflicts with the gist as the single remote source. |
| [`liv-009`](tasks/liv-009_hide_individual_channels.md) | Hide individual channels | v2 | Conflicts with the gist as the single remote source. |
| [`liv-012`](tasks/liv-012_live_preview_pane.md) | Live preview pane with session handoff | v2 | Not required for the product. |
| [`liv-013`](tasks/liv-013_en_vivo_canales_split_and_category_hygiene.md) | En vivo/Canales split and category hygiene | Done | Independent tabs, parser hardening, divider/empty filtering. |
| [`ply-011`](tasks/ply-011_universal_player_scaffold_with_capabilities.md) | Universal player scaffold with capabilities | Done | One player gated by `MediaCapabilities`. |
| [`ply-012`](tasks/ply-012_quality_cap_and_hls_variant_selector.md) | Quality cap and HLS variant selector | Done | Auto + manual HLS variant, persisted. |
| [`ply-013`](tasks/ply-013_buffer_and_latency_settings.md) | Buffer and latency settings | Done | Pre-buffer, after-rebuffer, low-latency live, Info values. |
| [`ply-014`](tasks/ply-014_local_subtitle_file.md) | Local subtitle file and OpenSubtitles | Done | SAF SRT, OpenSubtitles es→en, offsets. |
| [`ply-015`](tasks/ply-015_external_player_handoff.md) | External player handoff | Done | VOD only, ACTION_VIEW + headers. |
| [`ply-016`](tasks/ply-016_sleep_timer.md) | Sleep timer | v2 | Not required for the product. |
| [`ply-017`](tasks/ply-017_playback_speed_and_media_keys.md) | Playback speed and media keys | Done | VOD 0.5–2x, play/pause, ±30 s seek. |
| [`ui-001`](tasks/ui-001_universal_mediaitem_and_capabilities_model.md) | Universal MediaItem and capabilities model | Done | `MediaKind`, `MediaItem`, `MediaCapabilities`. |
| [`ui-005`](tasks/ui-005_font_size_and_ui_scale.md) | Font size and UI scale | Done | Text/UI scale via `LocalDensity`. |
| [`ui-006`](tasks/ui-006_navigation_rail_and_player_bar_layouts.md) | Navigation rail and player bar layouts | Done | Top/rail + full/compact + button selection. |
| [`vod-003`](tasks/vod-003_xtream_metadata_for_movies_and_series.md) | Xtream metadata for movies and series | Done | Plot/year/rating/genre/cast/backdrop + detail panels. |
| [`vod-006`](tasks/vod-006_universal_search_across_live_and_vod.md) | Universal fuzzy search across live and VOD | Done | En vivo/Canales/Películas/Series, accents/ñ, typos. |
| [`cfg-005`](tasks/cfg-005_grandma_mode_lock_with_pin.md) | Grandma mode lock with PIN | v2 | Abuela/grandma mode deferred to v2; PIN policy TBD. |
| [`cfg-011`](tasks/cfg-011_config_history_and_rollback.md) | Config history and rollback | v2 |  |
| [`cfg-012`](tasks/cfg-012_hidden_sync_and_config_diagnostics.md) | Hidden sync and config diagnostics | v2 |  |
| [`cfg-013`](tasks/cfg-013_push_notification_for_config_updates.md) | Push notification for config updates | v2 |  |
| [`cfg-014`](tasks/cfg-014_local_admin_dashboard.md) | Local admin dashboard | v2 |  |
| [`cfg-015`](tasks/cfg-015_backup_and_restore.md) | Backup and restore | v2 |  |
| [`epg-003`](tasks/epg-003_full_tv_guide_grid.md) | Full TV guide grid | v2 |  |
| [`liv-010`](tasks/liv-010_custom_groups_and_pinned_categories.md) | Custom groups and pinned categories | v2 |  |
| [`liv-011`](tasks/liv-011_advanced_filters_and_bulk_rename.md) | Advanced filters and bulk rename | v2 |  |
| [`ply-018`](tasks/ply-018_picture_in_picture.md) | Picture-in-picture | v2 |  |
| [`ply-019`](tasks/ply-019_audio_only_mode.md) | Audio-only mode | v2 |  |
| [`ui-003`](tasks/ui-003_simple_and_full_ui_modes.md) | Simple and Full UI modes | v2 |  |
| [`ui-008`](tasks/ui-008_themes_and_accent_color.md) | Themes and accent color | v2 |  |
| [`ui-009`](tasks/ui-009_font_family_and_popup_size.md) | Font family and popup size | v2 |  |
| [`ui-010`](tasks/ui-010_live_layout_variants_and_panel_widths.md) | Live layout variants and panel widths | v2 |  |

## Notes for the next session

- **V1 and V1.1 are complete.** `Now` is empty; v1.2 has no committed items yet.
- `liv-007` is deferred: En vivo is the curated gist and Canales is Xtream; no
  multiple-playlist switcher is needed.
- `liv-008` and `liv-009` remain v2: per-device category/channel overrides conflict
  with the gist as the single remote source of truth.
- `liv-012` and `ply-016` remain v2: not required for the product.
- The config gist is the canonical config; sources and credentials are central-only.
- **En vivo** = `sources.m3uUrl` only. **Canales** = `sources.xtream` live only. They
  load and fail independently. `sources.liveMode` is legacy; `merge` no longer merges.
  Old configs automatically gain the `channels` tab in v3.
- Xtream divider categories/streams are filtered; categories with fewer than
  `minCategoryItems` usable streams are hidden after load. `Mostrar ocultas` restores
  them; `Ajustes → Fuentes` controls both filters.
- EPG is optional (`epg.url`). XMLTV may be plain or gzip; the guide is cached encrypted
  for a rolling window (6 h back, 48 h forward) and refreshed per `epg.refreshHours`.
  Rows match `tvg-id` first, then XMLTV display name, and hide when there is no data.
- `Ajustes` is visible and has no PIN. Local overrides live in `files/device-settings.json`.
- Live state (favorites, recents, last channel) is encrypted in `files/live-state.bin`
  because channel keys can contain playlist tokens. It survives playlist refreshes by
  keying channels on `tvg-id`, falling back to the URL.
- Zapping follows the current search-filtered list, wraps at both ends, and is enabled
  by `live.zapping`. `live.startLastChannel` focuses the last channel on entry.
- The player track picker opens from `Audio/Subs` or `MENU`; `INFO` opens stream
  diagnostics. VOD adds quality, speed and external-player controls.
- `Ajustes → Fuentes → Comprobar fuentes` checks every configured source. Source
  failures offer `Actualizar configuración` + `Reintentar`; force-updating fetches the
  gist without an ETag and keeps the old config on failure.
- Config, playlist and Xtream category caches are encrypted and keyed by
  `CacheIdentity`; changing only the playlist URL/headers or Xtream account invalidates
  only that cache. Favorites, recents and history are preserved.
- With the playlist host down, a cold start shows the last channel list as
  `Lista en caché`; with both gist and playlist down, the config cache still starts.
- Next-episode auto-play is always on; the on/off setting is still pending.
- `Ajustes → Diagnóstico` shows the bounded error log (channel, Media3 code, Spanish
  message; no URLs).
- Universal search loads the full Xtream catalogue on demand (47k+ entries on the
  test account), caches it for the session, and normalizes accents/ñ with bounded
  typo tolerance. Live results appear before movies/series finish loading.
- Subtitles: embedded tracks, local SAF `.srt`, and OpenSubtitles when
  `subtitles.apiKey` is set. Spanish first, English fallback; offsets are stored
  encrypted in `files/subtitle-offsets.bin`.
- External player is VOD-only; live TV stays in ExoPlayer for zapping/recovery.
- Boot/wake: the receiver and alarm are attempted, but Android 11 on this Google TV
  blocks background activity starts. `Usar Streaming como inicio` enables a HOME
  alias; the system may still prefer its own launcher. `Reanudar al despertar`
  restores the encrypted last playback when the app is started.
- `Ajustes → Inicio` has boot, wake and launcher toggles (first two default on).
- `Ajustes → Interfaz` has text/UI scale, top/rail navigation, full/compact player bar
  and visible player buttons.
- `vod-001`/`vod-002` are marked done for core browse/playback; movie detail is now
  implemented in v1.1. Poster grid remains a polish item.

## Copy this into a new session

> Work on Streaming. Read `todo-v1.md` and `HISTORY.md` first. Start with the first
> unchecked item in **Now**, read its spec in `tasks/`, implement it and verify it on
> the TV at `192.168.100.16:5555` with `./scripts/deploy.sh`, then update `todo-v1.md`
> and `HISTORY.md` and commit. Never commit `local.properties`, config URLs, playlist
> URLs or Xtream credentials.
