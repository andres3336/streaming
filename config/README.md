# Config

The remote config is a single JSON file published as a secret/unlisted gist.

- `sample-config.json` — redacted example; copy it, fill in the real values, and publish.
- `schema.json` — JSON Schema for editors and CI validators.

The app reads the raw config URL from `local.properties` at build time:

```properties
config.url=https://gist.githubusercontent.com/USUARIO/GIST_ID/raw/config.json
```

`local.properties` is gitignored. If `config.url` is empty, the app falls back to the
bundled demo playlist so the APK is still installable.

Validation rules are implemented in `ConfigParser.kt` and mirrored by `schema.json`.
Never commit the real config URL, Xtream credentials, or a filled-in sample.
