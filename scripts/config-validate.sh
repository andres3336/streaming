#!/usr/bin/env bash
# Validate a config file against config/schema.json before pasting it into the gist.
# Never prints the config contents, the gist URL or credentials.
set -euo pipefail

cd "$(dirname "$0")/.."

CONFIG_FILE="${1:-config/sample-config.json}"
SCHEMA_FILE="${2:-config/schema.json}"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "No existe el archivo: $CONFIG_FILE" >&2
  exit 1
fi
if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "No existe el esquema: $SCHEMA_FILE" >&2
  exit 1
fi

python3 - "$CONFIG_FILE" "$SCHEMA_FILE" <<'PY'
import json
import sys

config_path, schema_path = sys.argv[1], sys.argv[2]

try:
    with open(config_path, "r", encoding="utf-8") as handle:
        config = json.load(handle)
except json.JSONDecodeError as error:
    print(f"JSON inválido: línea {error.lineno}, columna {error.colno}", file=sys.stderr)
    sys.exit(1)

with open(schema_path, "r", encoding="utf-8") as handle:
    schema = json.load(handle)

try:
    import jsonschema
except ImportError:
    jsonschema = None

if jsonschema is not None:
    try:
        jsonschema.Draft7Validator(schema).validate(config)
    except jsonschema.ValidationError as error:
        location = "/".join(str(part) for part in error.absolute_path) or "(raíz)"
        print(f"Config inválida en {location}: {error.message}", file=sys.stderr)
        sys.exit(1)

sources = config.get("sources")
if not isinstance(sources, dict):
    print("Config inválida: falta sources", file=sys.stderr)
    sys.exit(1)

mode = sources.get("liveMode")
if mode not in {"gist", "xtream", "merge"}:
    print("Config inválida: liveMode debe ser gist, xtream o merge", file=sys.stderr)
    sys.exit(1)
if mode in {"gist", "merge"} and not sources.get("m3uUrl"):
    print("Config inválida: este modo requiere sources.m3uUrl", file=sys.stderr)
    sys.exit(1)
if mode in {"xtream", "merge"} and not isinstance(sources.get("xtream"), dict):
    print("Config inválida: este modo requiere sources.xtream", file=sys.stderr)
    sys.exit(1)

print("Config válida.")
PY

cat <<'TEXT'

Publicar sin exponer secretos:
  1. Copia config/sample-config.json a un archivo temporal fuera del repositorio.
  2. Rellena credenciales y URLs solo en ese archivo.
  3. Ejecuta: ./scripts/config-validate.sh /ruta/al/config.json
  4. Abre el gist secreto y reemplaza config.json por el contenido validado.
  5. En el TV: Ajustes → Actualizar configuración, o reinicia la app.

Rollback:
  GitHub → Gist → Revisions → elige la revisión anterior → Restore.

Nunca hagas commit del config relleno, de la URL del gist ni de credenciales.
TEXT
