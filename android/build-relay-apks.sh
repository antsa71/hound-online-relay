#!/usr/bin/env bash
set -euo pipefail

: "${HOUND_RELAY_URL:?Set HOUND_RELAY_URL to the deployed relay Worker URL}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"

./gradlew assembleServerDebug assembleClientDebug \
  -PhoundRelayUrl="$HOUND_RELAY_URL" \
  --no-daemon --no-configuration-cache

echo "Server APK: app/build/outputs/apk/server/debug/app-server-debug.apk"
echo "Client APK: app/build/outputs/apk/client/debug/app-client-debug.apk"
