---
id: cfg-016
title: "Offline playlist cache"
priority: P1
tags: [v1, config, channels, cache]
epic: Config
created: 2026-10-29
depends_on: [cfg-003, liv-001]
---
# Offline playlist cache

## Context

The config is cached, but the M3U is still fetched on every start. If the playlist host is
unreachable, the app cannot show channels even though it has a valid config.

## Subtasks

- [ ] Cache the parsed channel list after a successful M3U fetch
- [ ] Fall back to the cached list when the M3U fetch fails
- [ ] Invalidate the cache when `sources.m3uUrl` changes
- [ ] Mark the UI as `Lista en caché` when using it
- [ ] Encrypt the cache or strip credentials from stored stream URLs
- [ ] Test cold start with the playlist host unreachable

## Acceptance criteria

- [ ] Cold start with the gist unreachable and the playlist host unreachable still shows the last channel list.
- [ ] A changed `sources.m3uUrl` replaces the cache on the next successful fetch.
- [ ] No credentials or full stream URLs are stored in plaintext.
- [ ] The UI indicates when the list is from cache.

## Notes

See `DECISIONS.md` for project-wide scope and non-goals.
