#!/usr/bin/env bash
set -euo pipefail
mkdir -p artifacts/emulator
logcat_pid=''
collect_evidence() {
    if [[ -n "$logcat_pid" ]]; then kill "$logcat_pid" 2>/dev/null || true; fi
    logcat_pid=''
    timeout 10s adb shell dumpsys activity activities > artifacts/emulator/activities.txt 2>&1 || true
    timeout 10s adb shell dumpsys meminfo de.phbouillon.android.games.alite > artifacts/emulator/memory.txt 2>&1 || true
    for name in ship-intro status trade galaxy flight resumed-flight; do
        [[ -s "artifacts/emulator/$name.png" ]] && continue
        timeout 5s adb exec-out run-as de.phbouillon.android.games.alite cat "files/test-evidence/$name.png" > "artifacts/emulator/$name.png" 2>/dev/null || true
    done
}
trap collect_evidence EXIT
run_suite() {
    local classes="$1" report="$2"
    timeout --foreground 240s adb shell am instrument -w -r -e class "$classes" de.phbouillon.android.games.alite.test/androidx.test.runner.AndroidJUnitRunner | tee "$report"
    grep -Eq 'OK \([0-9]+ tests?\)' "$report"
    ! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' "$report"
}
timeout 120s adb install artifacts/app/Alite-1.5.9-Android15-debug.apk
timeout 30s adb install artifacts/test/*.apk
timeout 10s adb shell wm size 2400x1080
timeout 10s adb shell settings put system accelerometer_rotation 0
timeout 10s adb shell settings put system user_rotation 1
timeout 10s adb logcat -c
# Preserve app/system diagnostics even if the emulator disconnects later.
adb logcat -v threadtime > artifacts/emulator/logcat.txt 2>&1 &
logcat_pid=$!
run_suite de.phbouillon.android.games.alite.StorageCompatibilityTest artifacts/emulator/storage.txt
run_suite de.phbouillon.android.games.alite.Android15GameTest artifacts/emulator/instrumentation.txt
collect_evidence
# Only the fresh emulator's synthetic app data is reset between the two suites.
timeout 15s adb shell pm clear de.phbouillon.android.games.alite
adb logcat -v threadtime > artifacts/emulator/intro-logcat.txt 2>&1 &
logcat_pid=$!
run_suite de.phbouillon.android.games.alite.LauncherIntroTest artifacts/emulator/launcher-intro.txt
timeout 10s adb exec-out screencap -p > artifacts/emulator/final-screen.png
