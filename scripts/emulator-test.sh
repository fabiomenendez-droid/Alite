#!/usr/bin/env bash
set -euo pipefail
mkdir -p artifacts/emulator
collect_evidence() {
    adb logcat -d > artifacts/emulator/logcat.txt || true
    for name in ship-intro status trade galaxy flight resumed-flight; do
        adb exec-out run-as de.phbouillon.android.games.alite cat "files/test-evidence/$name.png" > "artifacts/emulator/$name.png" || true
    done
}
trap collect_evidence EXIT
adb install artifacts/app/Alite-1.5.9-Android15-debug.apk
adb install artifacts/test/*.apk
adb shell wm size 2400x1080
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb logcat -c
adb shell am instrument -w -r de.phbouillon.android.games.alite.test/androidx.test.runner.AndroidJUnitRunner | tee artifacts/emulator/instrumentation.txt
grep -Eq 'OK \([0-9]+ tests?\)' artifacts/emulator/instrumentation.txt
! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' artifacts/emulator/instrumentation.txt
adb exec-out screencap -p > artifacts/emulator/final-screen.png
