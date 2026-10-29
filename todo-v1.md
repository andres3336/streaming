# Streaming — V1 reconciled TODO

**Last reconciled:** 2026-10-29  
**App:** Streaming 0.10.0 (`versionCode 10`)  
**TV:** `192.168.100.16:5555` — MOTOROLA GOOGLE TV, Android 11 / API 30, armeabi-v7a  
**Specs:** 74 tasks in `tasks/`  
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

- **Live:** curated 55-channel gist playlist, group `TV`, logos/initials, auto-focus,
  favorites, recents, last channel, CH+/CH− and media-key zapping, channel search.
- **Movies:** Xtream categories, posters, playback (1920x960 verified).
- **Series:** Xtream categories, seasons/episodes, playback, next-episode queue.
- **Player:** audio/subtitle pickers with immediate apply and clear off state.
- **Config:** permanent gist, no-cache fetch, encrypted last-known-good cache,
  refresh on resume, manual update, safe apply only when not playing.
- **Ajustes:** buffer, quality cap, aspect, VOD cache size/clear, WiFi/Ethernet-only,
  max bitrate. Local overrides merge on top of the gist.
- **Playback:** ExoPlayer HLS/TS/MP4, headers, retry/backoff, live auto-reconnect,
  Spanish error overlay, VOD cache, resume/Continue, next-episode countdown.
- **Diagnostics:** in-player stream info (codec, resolution, FPS, bitrate, buffer,
  dropped frames, decoder, bandwidth) with host-only source masking.
- **Security:** config cache, playback positions and live state use Keystore AES-GCM;
  errors do not log/display URLs.
- **Tests:** 65 unit tests passing.

## Now (max 3)

- [ ] `cfg-008` · Source health check · [spec](tasks/cfg-008_source_health_check.md) — Source health check.
- [ ] `cfg-009` · Update-config dialog and retry flow · [spec](tasks/cfg-009_update_config_dialog_and_retry_flow.md) — Update-config dialog and retry flow.
- [ ] `cfg-004` · Config refresh on start, resume and failure · [spec](tasks/cfg-004_config_refresh_on_start_resume_and_failure.md) — Start/resume/manual refresh and safe apply done; forceUpdate prompt pending.

## Next (V1)

- [ ] `cfg-016` · Offline playlist cache · [spec](tasks/cfg-016_offline_playlist_cache.md) — Cache the channel list for a true offline start; added during reconciliation.
- [ ] `liv-005` · Xtream auth and lazy live categories · [spec](tasks/liv-005_xtream_auth_and_lazy_live_categories.md) — Xtream auth + VOD catalog done; live categories/lazy loading pending.
- [ ] `liv-006` · Live source mode gist, Xtream or merge · [spec](tasks/liv-006_live_source_mode_gist_xtream_or_merge.md) — Gist live works; Xtream VOD works; xtream/merge live mode pending.
- [ ] `cfg-007` · Config validate and publish script · [spec](tasks/cfg-007_config_validate_and_publish_script.md) — Config validate/publish script.
- [ ] `test-004` · Cache and storage limit test · [spec](tasks/test-004_cache_and_storage_limit_test.md) — Cache and storage limit test.
- [ ] `test-005` · Config update propagation test · [spec](tasks/test-005_config_update_propagation_test.md) — Config update propagation test.

## Polish (V1, small)

- [ ] `liv-002` · Channel list, group rail and logos · [spec](tasks/liv-002_channel_list_group_rail_and_logos.md) — Lazy rows, logos/initials, group headers and auto-focus done; group rail/channel numbers pending.
- [ ] `ply-002` · Retry, fallback and reconnect · [spec](tasks/ply-002_retry_fallback_and_reconnect.md) — Bounded retry/backoff + live auto-reconnect done; HLS→TS fallback and source-failure event pending.
- [ ] `ply-005` · Aspect ratio and zoom modes · [spec](tasks/ply-005_aspect_ratio_and_zoom_modes.md) — Fit/fill/zoom/original done; forced 16:9/4:3 pending.
- [ ] `ply-008` · Error overlay and diagnostics · [spec](tasks/ply-008_error_overlay_and_diagnostics.md) — Spanish error overlay + Reintentar/Cerrar done; bounded error log pending.
- [ ] `ply-010` · Cache controls and storage usage · [spec](tasks/ply-010_cache_controls_and_storage_usage.md) — Cache size + clear done; storage usage display pending.
- [ ] `cfg-010` · Atomic config apply and cache invalidation · [spec](tasks/cfg-010_atomic_config_apply_and_cache_invalidation.md) — Atomic cache write + state swap done; cache invalidation/publish flow partial.
- [ ] `ui-002` · Reusable browse shell · [spec](tasks/ui-002_reusable_browse_shell.md) — Tabs/screens exist; no reusable browse-shell abstraction yet.

## Done

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

## Deferred — v1.1

- `epg-001` · Optional XMLTV source from config · [spec](tasks/epg-001_optional_xmltv_source_from_config.md)
- `epg-002` · Now and next in channel rows · [spec](tasks/epg-002_now_and_next_in_channel_rows.md)
- `liv-007` · Multiple playlists and switcher · [spec](tasks/liv-007_multiple_playlists_and_switcher.md)
- `ply-011` · Universal player scaffold with capabilities · [spec](tasks/ply-011_universal_player_scaffold_with_capabilities.md) — Architecture/refactor; not user-visible.
- `ply-012` · Quality cap and HLS variant selector · [spec](tasks/ply-012_quality_cap_and_hls_variant_selector.md)
- `ply-013` · Buffer and latency settings · [spec](tasks/ply-013_buffer_and_latency_settings.md)
- `ply-014` · Local subtitle file · [spec](tasks/ply-014_local_subtitle_file.md)
- `ply-015` · External player handoff · [spec](tasks/ply-015_external_player_handoff.md)
- `ply-017` · Playback speed and media keys · [spec](tasks/ply-017_playback_speed_and_media_keys.md)
- `ui-001` · Universal MediaItem and capabilities model · [spec](tasks/ui-001_universal_mediaitem_and_capabilities_model.md) — Architecture/refactor; not user-visible.
- `ui-005` · Font size and UI scale · [spec](tasks/ui-005_font_size_and_ui_scale.md)
- `ui-006` · Navigation rail and player bar layouts · [spec](tasks/ui-006_navigation_rail_and_player_bar_layouts.md)
- `vod-003` · Xtream metadata for movies and series · [spec](tasks/vod-003_xtream_metadata_for_movies_and_series.md)
- `vod-006` · Universal search across live and VOD · [spec](tasks/vod-006_universal_search_across_live_and_vod.md) — Spec is tagged v2; keep here unless the roadmap changes.

## Deferred — v2

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
- `ply-018` · Picture-in-picture · [spec](tasks/ply-018_picture_in_picture.md)
- `ply-019` · Audio-only mode · [spec](tasks/ply-019_audio_only_mode.md)
- `ply-016` · Sleep timer · [spec](tasks/ply-016_sleep_timer.md) — Not required for the product.
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
| [`cfg-004`](tasks/cfg-004_config_refresh_on_start_resume_and_failure.md) | Config refresh on start, resume and failure | Now | Start/resume/manual refresh and safe apply done; forceUpdate prompt pending. |
| [`cfg-008`](tasks/cfg-008_source_health_check.md) | Source health check | Now | Source health check. |
| [`cfg-009`](tasks/cfg-009_update_config_dialog_and_retry_flow.md) | Update-config dialog and retry flow | Now | Update-config dialog and retry flow. |
| [`cfg-016`](tasks/cfg-016_offline_playlist_cache.md) | Offline playlist cache | Next | Cache the channel list for a true offline start; added during reconciliation. |
| [`liv-005`](tasks/liv-005_xtream_auth_and_lazy_live_categories.md) | Xtream auth and lazy live categories | Next | Xtream auth + VOD catalog done; live categories/lazy loading pending. |
| [`liv-006`](tasks/liv-006_live_source_mode_gist_xtream_or_merge.md) | Live source mode gist, Xtream or merge | Next | Gist live works; Xtream VOD works; xtream/merge live mode pending. |
| [`cfg-007`](tasks/cfg-007_config_validate_and_publish_script.md) | Config validate and publish script | Next | Config validate/publish script. |
| [`test-004`](tasks/test-004_cache_and_storage_limit_test.md) | Cache and storage limit test | Next | Cache and storage limit test. |
| [`test-005`](tasks/test-005_config_update_propagation_test.md) | Config update propagation test | Next | Config update propagation test. |
| [`liv-002`](tasks/liv-002_channel_list_group_rail_and_logos.md) | Channel list, group rail and logos | Polish | Lazy rows, logos/initials, group headers and auto-focus done; group rail/channel numbers pending. |
| [`ply-002`](tasks/ply-002_retry_fallback_and_reconnect.md) | Retry, fallback and reconnect | Polish | Bounded retry/backoff + live auto-reconnect done; HLS→TS fallback and source-failure event pending. |
| [`ply-005`](tasks/ply-005_aspect_ratio_and_zoom_modes.md) | Aspect ratio and zoom modes | Polish | Fit/fill/zoom/original done; forced 16:9/4:3 pending. |
| [`ply-008`](tasks/ply-008_error_overlay_and_diagnostics.md) | Error overlay and diagnostics | Polish | Spanish error overlay + Reintentar/Cerrar done; bounded error log pending. |
| [`ply-010`](tasks/ply-010_cache_controls_and_storage_usage.md) | Cache controls and storage usage | Polish | Cache size + clear done; storage usage display pending. |
| [`cfg-010`](tasks/cfg-010_atomic_config_apply_and_cache_invalidation.md) | Atomic config apply and cache invalidation | Polish | Atomic cache write + state swap done; cache invalidation/publish flow partial. |
| [`ui-002`](tasks/ui-002_reusable_browse_shell.md) | Reusable browse shell | Polish | Tabs/screens exist; no reusable browse-shell abstraction yet. |
| [`epg-001`](tasks/epg-001_optional_xmltv_source_from_config.md) | Optional XMLTV source from config | v1.1 |  |
| [`epg-002`](tasks/epg-002_now_and_next_in_channel_rows.md) | Now and next in channel rows | v1.1 |  |
| [`liv-007`](tasks/liv-007_multiple_playlists_and_switcher.md) | Multiple playlists and switcher | v1.1 |  |
| [`liv-008`](tasks/liv-008_category_hide_rename_and_reorder.md) | Category hide, rename and reorder | v2 | Conflicts with the gist as the single remote source. |
| [`liv-009`](tasks/liv-009_hide_individual_channels.md) | Hide individual channels | v2 | Conflicts with the gist as the single remote source. |
| [`liv-012`](tasks/liv-012_live_preview_pane.md) | Live preview pane with session handoff | v2 | Not required for the product. |
| [`ply-011`](tasks/ply-011_universal_player_scaffold_with_capabilities.md) | Universal player scaffold with capabilities | v1.1 | Architecture/refactor; not user-visible. |
| [`ply-012`](tasks/ply-012_quality_cap_and_hls_variant_selector.md) | Quality cap and HLS variant selector | v1.1 |  |
| [`ply-013`](tasks/ply-013_buffer_and_latency_settings.md) | Buffer and latency settings | v1.1 |  |
| [`ply-014`](tasks/ply-014_local_subtitle_file.md) | Local subtitle file | v1.1 |  |
| [`ply-015`](tasks/ply-015_external_player_handoff.md) | External player handoff | v1.1 |  |
| [`ply-016`](tasks/ply-016_sleep_timer.md) | Sleep timer | v2 | Not required for the product. |
| [`ply-017`](tasks/ply-017_playback_speed_and_media_keys.md) | Playback speed and media keys | v1.1 |  |
| [`ui-001`](tasks/ui-001_universal_mediaitem_and_capabilities_model.md) | Universal MediaItem and capabilities model | v1.1 | Architecture/refactor; not user-visible. |
| [`ui-005`](tasks/ui-005_font_size_and_ui_scale.md) | Font size and UI scale | v1.1 |  |
| [`ui-006`](tasks/ui-006_navigation_rail_and_player_bar_layouts.md) | Navigation rail and player bar layouts | v1.1 |  |
| [`vod-003`](tasks/vod-003_xtream_metadata_for_movies_and_series.md) | Xtream metadata for movies and series | v1.1 |  |
| [`vod-006`](tasks/vod-006_universal_search_across_live_and_vod.md) | Universal search across live and VOD | v1.1 |  |
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

- Product decision: finish V1 before starting v1.1. v1.1 opens with `epg-001`/`epg-002`.
- `liv-008` and `liv-009` moved to v2: per-device category/channel overrides conflict
  with the gist as the single remote source of truth.
- `liv-012` and `ply-016` moved to v2: not required for the product.
- The config gist is the canonical config; sources and credentials are central-only.
- `Ajustes` is visible and has no PIN. Local overrides live in `files/device-settings.json`.
- Live state (favorites, recents, last channel) is encrypted in `files/live-state.bin`
  because channel keys can contain playlist tokens. It survives playlist refreshes by
  keying channels on `tvg-id`, falling back to the URL.
- Zapping follows the current search-filtered list, wraps at both ends, and is enabled
  by `live.zapping`. `live.startLastChannel` focuses the last channel on entry.
- The player track picker opens from `Audio · Subtítulos`, `MENU` or `INFO`.
- Config cache works when the gist is unreachable. The playlist still needs network;
  `cfg-016` fixes that.
- Next-episode auto-play is always on; the on/off setting is still pending.
- `vod-001`/`vod-002` are marked done for core browse/playback; poster grid and detail
  panels are v1.1 polish.
- Architecture refactors (`ui-001`, `ply-011`) are not user-visible; keep them in v1.1
  unless a feature needs them.

## Copy this into a new session

> Work on Streaming. Read `todo-v1.md` and `HISTORY.md` first. Start with the first
> unchecked item in **Now**, read its spec in `tasks/`, implement it and verify it on
> the TV at `192.168.100.16:5555` with `./scripts/deploy.sh`, then update `todo-v1.md`
> and `HISTORY.md` and commit. Never commit `local.properties`, config URLs, playlist
> URLs or Xtream credentials.
