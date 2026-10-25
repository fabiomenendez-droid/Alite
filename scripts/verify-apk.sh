#!/usr/bin/env bash
set -euo pipefail
apk="${1:-build/outputs/apk/debug/Alite-debug.apk}"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
: "${sdk:?Set ANDROID_HOME to the Android SDK directory}"
tools="$sdk/build-tools/36.0.0"
python3 scripts/verify_apk.py "$apk"
"$tools/apksigner" verify --verbose "$apk"
"$tools/zipalign" -c -v 4 "$apk" > /dev/null
badging="$("$tools/aapt" dump badging "$apk")"
printf '%s\n' "$badging" | head -n 5
[[ "$badging" == *"sdkVersion:'23'"* ]]
[[ "$badging" == *"targetSdkVersion:'35'"* ]]
[[ "$badging" == *"de.phbouillon.android.games.alite.AliteStartManager"* ]]
if [[ "$badging" == *"uses-permission: name='android.permission.INTERNET'"* ]]; then
    printf 'Unexpected network permission\n' >&2
    exit 1
fi
printf 'PASS: APK signature, alignment, SDK 23/35 and offline launcher.\n'
