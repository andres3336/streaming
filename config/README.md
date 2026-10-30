# Config

The remote config is a single **plain JSON** file published as a secret/unlisted gist.
No JSONC comments: keep explanations here and in `schema.json`.

- `sample-config.json` — redacted v3 example; copy it, fill in the real values, and publish.
- `schema.json` — JSON Schema for editors and CI validators.

The app reads the raw config URL from `local.properties` at build time:

```properties
config.url=https://gist.githubusercontent.com/USUARIO/GIST_ID/raw/config.json
```

`local.properties` is gitignored. If `config.url` is empty, the app falls back to the
bundled demo playlist so the APK is still installable.

## v3 sources

`En vivo` and `Canales` are independent tabs:

- `sources.m3uUrl` feeds **En vivo** (the curated gist playlist).
- `sources.xtream` feeds **Canales**, Películas and Series.
- `sources.liveMode` is legacy; it is accepted from v1/v2 configs but no longer
  merges the two sources. `merge` maps to both separate tabs.

Old configs with an Xtream block automatically gain the `channels` tab on upgrade.

## Xtream category hygiene

```json
"xtream": {
  "minCategoryItems": 2,
  "hideEmptyCategories": true
}
```

Categories with fewer usable streams are hidden after they load. Punctuation-only
divider categories and streams are always ignored. `Ajustes → Fuentes` can change
both values per device, and the live rail has a `Mostrar ocultas` escape hatch.

## Subtitles / OpenSubtitles

```json
"subtitles": {
  "enabled": true,
  "apiKey": "TU_API_KEY",
  "username": "",
  "password": "",
  "languages": ["es", "en"],
  "autoDownload": true
}
```

Create a free account at OpenSubtitles.com and request an API key. The key and
optional account stay in the gist; they are never logged. When a movie or episode
has no embedded subtitles, the app searches Spanish first, then English, downloads
the best match, caches it, and lets you adjust the timing in
`Audio/Subs → Sincronización` (`±0.5 s`, `±5 s`, reset). A local `.srt` file can also
be attached with `Archivo .srt`.

Without an API key, playback and embedded subtitles keep working; only the
OpenSubtitles search is disabled.

## Local overrides

Hardware-specific settings are stored on the TV in
`files/device-settings.json` and merged on top of the gist:

```text
app defaults < gist config < local device settings
```

Ajustes writes only the changed keys: buffer, pre-buffer, quality, aspect, cache size,
clear-on-start, WiFi-only, max bitrate, text/UI scale, navigation rail, player bar,
visible player buttons, subtitle auto-download, category filtering, and boot/wake
auto-start. Sources, credentials, EPG and force-update stay central-only.

Validation rules are implemented in `ConfigParser.kt` and mirrored by `schema.json`.
Never commit the real config URL, Xtream credentials, the OpenSubtitles key, or a
filled-in sample.

## Validate and publish

Validate a filled config before pasting it into the gist:

```bash
./scripts/config-validate.sh /path/to/config.json
```

The script checks JSON syntax, `config/schema.json` and the source combinations.
It never prints the config contents.

To publish: edit the secret gist, replace `config.json`, save, then use
`Ajustes → Actualizar configuración` or restart the app. The app also refreshes
on resume.

To roll back, open the gist on GitHub, choose **Revisions**, select the previous
revision and **Restore**. Locally, the encrypted last-known-good config and the
playlist/xtream caches keep the TV usable while a bad revision is fixed.
