---
name: TV Device
description: Use when you need the physical Android TV — install or launch the app, follow logcat, take a screenshot, inject D-pad keys, check focus/window, resolution, app version or process state, or reconnect after a dropped adb link.
---

# TV Device

The target is a real TV over network adb, not an emulator.

| Fact | Value |
| --- | --- |
| Device | `192.168.100.16:5555` |
| Model | `MOTOROLA_GOOGLE_TV` (mountbaker) |
| Android | 11 / SDK 30, `minSdk 23`, `targetSdk 36` |
| ABI | `armeabi-v7a` only (32-bit) |
| Display | 1280x720 @ 213 dpi |
| App | `com.andres3336.streaming/.MainActivity` |

Always load the environment first; it sets `JAVA_HOME`, the SDK path, `adb` and the
device:

```bash
source scripts/env.sh
```

Override the target with `ADB_DEVICE=...` when working on another TV.

## Daily commands

```bash
./scripts/deploy.sh                 # build, install -r, launch -W
./scripts/logcat.sh                 # app PID only, falls back to all logs
./scripts/screenshot.sh out.png     # default /tmp/opencode/tv.png
./scripts/check.sh                  # tests + debug APK, no device needed
```

Manual equivalents:

```bash
adb connect "$ADB_DEVICE" && adb -s "$ADB_DEVICE" wait-for-device
adb -s "$ADB_DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$ADB_DEVICE" shell am start -W -n "$APP_ACTIVITY"
adb -s "$ADB_DEVICE" shell am force-stop "$APP_ID"
adb -s "$ADB_DEVICE" shell pidof -s "$APP_ID"
```

## Inspect

```bash
adb devices -l
adb -s "$ADB_DEVICE" shell dumpsys window | grep -m1 mCurrentFocus
adb -s "$ADB_DEVICE" shell dumpsys package "$APP_ID" | grep -m2 'versionName\|versionCode'
adb -s "$ADB_DEVICE" shell wm size
adb -s "$ADB_DEVICE" shell wm density
adb -s "$ADB_DEVICE" shell getprop ro.build.version.sdk
```

`mCurrentFocus` is the fastest proof the app is actually foregrounded after a deploy.
Installed `versionCode` is the fastest proof the APK really changed.

## D-pad and media input

```bash
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_DPAD_UP        # 19
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_DPAD_DOWN      # 20
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_DPAD_LEFT      # 21
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_DPAD_RIGHT     # 22
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_DPAD_CENTER    # 23 (OK)
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_BACK           # 4
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_MEDIA_PLAY_PAUSE  # 85
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_MEDIA_NEXT     # 87 (seek +30 s / zap)
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_MEDIA_PREVIOUS # 88 (seek -30 s / zap)
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_CHANNEL_UP     # 166
adb -s "$ADB_DEVICE" shell input keyevent KEYCODE_CHANNEL_DOWN   # 167
adb -s "$ADB_DEVICE" shell input text 'texto'                    # Buscar field
```

Use key events to prove focus order: screenshot after each step, and check that Up
from content reaches the top bar.

## Rules and hazards

- Default screenshots to `/tmp/opencode`, never the repo.
- `adb shell pm clear "$APP_ID"` wipes encrypted config, caches, favorites and resume
  positions. Ask before running it, and warn that the app will need a config refresh.
- Do not change `font_scale` or TV display settings to test layout; the app owns text
  scale via `LocalDensity` and ignores the system value.
- Do not reboot the TV, change its network or uninstall the app without being asked.
- If the link drops, `adb connect` again; only `adb kill-server` as a last resort, and
  ask first because it affects every adb session on the machine.
- Never paste device output containing stream URLs or credentials into the repo,
  a commit or a report.
