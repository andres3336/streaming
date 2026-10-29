#!/usr/bin/env bash
# Follow logcat for the app, including crash output.
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/env.sh

adb connect "$ADB_DEVICE" >/dev/null
adb -s "$ADB_DEVICE" wait-for-device

PID="$(adb -s "$ADB_DEVICE" shell pidof -s "$APP_ID" | tr -d '\r' || true)"
if [ -n "$PID" ]; then
    echo "==> Following PID $PID"
    exec adb -s "$ADB_DEVICE" logcat -v time --pid="$PID" "$@"
fi

echo "==> App not running; following all logs"
exec adb -s "$ADB_DEVICE" logcat -v time "$@"
