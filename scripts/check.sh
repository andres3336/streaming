#!/usr/bin/env bash
# Run the fast verification: unit tests and debug APK.
set -euo pipefail

cd "$(dirname "$0")/.."
source scripts/env.sh

./gradlew testDebugUnitTest assembleDebug --console=plain
