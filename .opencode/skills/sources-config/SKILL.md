---
name: Sources and Config
description: Use when changing remote config, the secret-gist URL, M3U or Xtream sources, source health/failure UI, encrypted storage or anything that could expose URLs and credentials.
---

# Sources and Config

Read the `Sources and config` and `Startup and cache` blocks in `CONTEXT.md` first.

## Map

| Concern | Path |
| --- | --- |
| Config shape, parse, merge, cache, health | `data/config/` |
| Xtream client, models, category filter | `data/xtream/` |
| M3U parse and fetch | `data/M3uParser.kt`, `data/M3uRepository.kt` |
| Live merge, search, zapping state | `data/live/` |
| Encrypted storage (Keystore) | `data/security/EncryptedFileStore.kt` |
| Caches (VOD/catalog only) | `data/cache/` |
| Subtitles / OpenSubtitles | `data/subtitles/` |

## Invariants

- Config is one JSON secret/unlisted gist URL, embedded at build time from
  `local.properties` (`config.url`). It is never fetched on a periodic timer.
- Refresh on cold start, resume, source failure and manual action only.
- On failure keep the last-known-good config and offer `Actualizar configuración` and
  `Reintentar`. Never drop the user into an empty app because one source broke.
- There is one TV section: the curated gist M3U is `_Mis canales`, pinned on top of
  the Xtream categories. No EPG: `epg`, `epgUrl` and `ui.showNowNext` are accepted and
  ignored; do not reintroduce XMLTV fetching or parsing.
- Xtream channels are keyed by `xtream:<stream id>`, never by `epg_channel_id`:
  providers reuse that id across SD/HD variants.
- `sources.xtream.hideCategories` hides live, movie and series categories from the
  gist by normalized substring (NFKD, no case/accents/emoji/punctuation). It is
  central-only: never move it to device settings. Hidden categories also leave
  Buscar, and `Mostrar ocultas` only reveals empty ones.
- `group-title` and `#EXTGRP` are ignored on purpose: channels carry no playlist
  group, and Xtream category names are never copied onto them. Do not reintroduce
  group parsing or group labels.
- Last-known-good config and caches are encrypted with Android Keystore.
- Do not show or log URLs, credentials or playlist tokens — not in UI, logcat,
  diagnostics, error text or test fixtures.
- Cache is VOD-only; never cache live TV.
- `ingest/` is reference only: never a build input, never copy its source.

## Config changes

```bash
./scripts/config-validate.sh /path/to/config.json
./scripts/config-validate.sh config/sample-config.json config/schema.json
```

The validator prints Spanish guidance and never echoes the file contents. Edit
`config/schema.json` and `config/README.md` together with any shape change, bump
`version` in the sample, then republish the gist.

Remote filters live under `sources.xtream.hideCategories`. Keep them as plain
substrings, not regex: symbol-only entries are ignored, and a broad word can also
hide a real category (`DEPORTES` matches both the divider and `🏈 Deportes`).

`config/config.json` is the local gitignored mirror of the published gist. Use it to
prepare and validate changes; never add it to git.

## Never commit

`local.properties`, `config.local.json`, `config/config.json`, `secrets.properties`,
`.env`, `*.jks`, `*.keystore`, `keystore.properties`, the gist URL and real
credentials.

## Verify

```bash
./scripts/check.sh
```

Relevant tests: `ConfigParserTest`, `ConfigV3Test`, `ConfigMergerTest`, `M3uParserTest`,
`EpgRepositoryTest`, `SourceHealthTest`, `XtreamClientTest`, `CacheIdentityTest`.
