#!/usr/bin/env bash
# Wird im Emulator-Schritt von .github/workflows/screenshots.yml ausgeführt.
set -x
PKG=de.marcel.zeitwaechter
adb root || true
sleep 3
adb wait-for-device
adb shell setprop persist.sys.timezone Europe/Berlin || true
adb shell cmd alarm set-timezone Europe/Berlin || true
adb install -r app/build/outputs/apk/debug/app-debug.apk
for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION ACCESS_BACKGROUND_LOCATION POST_NOTIFICATIONS; do
  adb shell pm grant $PKG android.permission.$p || true
done
adb shell dumpsys deviceidle whitelist +$PKG || true
python3 tools/screens.py
