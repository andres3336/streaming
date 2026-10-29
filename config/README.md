# Config

The remote config is a single **plain JSON** file published as a secret/unlisted gist.
No JSONC comments: keep explanations here and in `schema.json`.

- `sample-config.json` — redacted v2 example; copy it, fill in the real values, and publish.
- `schema.json` — JSON Schema for editors and CI validators.

The app reads the raw config URL from `local.properties` at build time:

```properties
config.url=https://gist.githubusercontent.com/USUARIO/GIST_ID/raw/config.json
```

`local.properties` is gitignored. If `config.url` is empty, the app falls back to the
bundled demo playlist so the APK is still installable.

## Local overrides

Hardware-specific settings are stored on the TV in
`files/device-settings.json` and merged on top of the gist:

```text
app defaults < gist config < local device settings
```

Ajustes writes only the changed keys: buffer, quality cap, aspect, cache size, WiFi-only
and max bitrate. Sources, credentials, EPG and force-update stay central-only.

Validation rules are implemented in `ConfigParser.kt` and mirrored by `schema.json`.
Never commit the real config URL, Xtream credentials, or a filled-in sample.

