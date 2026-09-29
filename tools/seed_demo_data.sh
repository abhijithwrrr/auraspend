#!/usr/bin/env bash
# Seed the connected device with demo data and open the app on it.
#
# Writes into the app's REAL database via DemoDataSeeder (an instrumentation
# test), so the data survives after the test APK is removed and you can drive
# the app normally. Re-runnable: the seeder clears the tables first, so it is
# safe to run again at any time and always leaves the same state.
#
#   tools/seed_demo_data.sh          # seed + launch
#   tools/seed_demo_data.sh --wipe   # clear app data first (fresh consent state)
#
# To undo: adb shell pm clear com.awbuilds.auraspend
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PKG=com.awbuilds.auraspend
TEST_PKG=com.awbuilds.auraspend.test
WIPE=0
[[ "${1:-}" == "--wipe" ]] && WIPE=1

command -v adb >/dev/null || { echo "adb not on PATH"; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "no device attached"; exit 1; }

if [[ $WIPE -eq 1 ]]; then
  echo "==> wiping app data (consent + downloads reset)"
  adb shell pm clear "$PKG" >/dev/null
fi

echo "==> building the instrumentation APK"
./gradlew :app:assembleFreeDebug :app:assembleFreeDebugAndroidTest --console=plain -q

# Install with -r so the app's database survives. Plain `connectedAndroidTest`
# uninstalls both APKs when it finishes, which takes the seeded data with it —
# that is why this drives `am instrument` directly.
echo "==> installing"
adb install -r app/build/outputs/apk/free/debug/app-free-debug.apk >/dev/null
adb install -r app/build/outputs/apk/androidTest/free/debug/app-free-debug-androidTest.apk >/dev/null

if ! adb shell pm list packages | grep -q "^package:$TEST_PKG$"; then
  echo "test APK not installed"; exit 1
fi

echo "==> seeding"
RESULT=$(adb shell am instrument -w -r \
  -e class "$PKG.DemoDataSeeder" \
  "$TEST_PKG/androidx.test.runner.AndroidJUnitRunner" 2>&1)

if grep -q "OK (1 test)" <<<"$RESULT"; then
  echo "    seeded: $(grep -c . <<<"$RESULT") lines OK"
else
  echo "    seeding FAILED:"
  sed 's/^/    /' <<<"$RESULT"
  exit 1
fi

# The seeder runs inside the app process, so restart it to pick the data up
# cleanly rather than leaving a stale Room singleton behind.
adb shell am force-stop "$PKG"
sleep 1
adb shell am start -n "$PKG/.MainActivity" >/dev/null
echo "==> launched. Screens: Home / Activity / Plan / Insights, + the centre FAB."
echo "    Undo with: adb shell pm clear $PKG"
