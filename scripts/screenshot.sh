#!/usr/bin/env bash
# Capture a screenshot from the TV.
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/env.sh

OUT="${1:-/tmp/opencode/tv.png}"
mkdir -p "$(dirname "$OUT")"
adb connect "$ADB_DEVICE" >/dev/null
adb -s "$ADB_DEVICE" exec-out screencap -p > "$OUT"
echo "$OUT"
