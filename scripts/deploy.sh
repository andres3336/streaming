#!/usr/bin/env bash
# Build, install and launch the debug APK on the TV.
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/env.sh

echo "==> Connecting to $ADB_DEVICE"
adb connect "$ADB_DEVICE" >/dev/null
adb -s "$ADB_DEVICE" wait-for-device

echo "==> Building debug APK"
./gradlew :app:assembleDebug

echo "==> Installing $APK_PATH"
adb -s "$ADB_DEVICE" install -r "$APK_PATH"

echo "==> Launching $APP_ACTIVITY"
adb -s "$ADB_DEVICE" shell am start -W -n "$APP_ACTIVITY"

echo "==> Done"
