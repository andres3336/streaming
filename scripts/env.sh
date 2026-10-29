#!/usr/bin/env bash
# Shared environment for local builds and deploys. Source this file.

export JAVA_HOME="${JAVA_HOME:-$HOME/.local/opt/jdk-21}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

export ADB_DEVICE="${ADB_DEVICE:-192.168.100.16:5555}"
export APP_ID="com.andres3336.streaming"
export APP_ACTIVITY="$APP_ID/.MainActivity"
export APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
