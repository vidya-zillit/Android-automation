#!/usr/bin/env bash
# Zillit Automation — one command: run every suite, then push the report
# to the team Google Sheet. Works standalone; no Claude involved.
#
#   ./run-and-report.sh                    # full suite (E2E asks for manual OTP)
#   ./run-and-report.sh --no-e2e           # skip the OTP/E2E test
#   ./run-and-report.sh --two-device       # cross-device send/receive (two users)
#
# Prerequisites (once): emulator running, Appium server running
#   (ANDROID_HOME=$HOME/Library/Android/sdk npm run appium),
#   develop-debug APK built in ../Zillit-Android.
#
# --two-device also needs a SECOND device attached and BOTH devices already
# logged into the SAME project as two different users. Set:
#   ZILLIT_SENDER_UDID=<devA>  ZILLIT_RECEIVER_UDID=<devB>  ./run-and-report.sh --two-device

set -u
cd "$(dirname "$0")"

GRADLE_ARGS=(test --continue)
if [[ "${1:-}" == "--no-e2e" ]]; then
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.JoinProjectTest'
                --tests 'com.zillit.automation.tests.CreateProjectValidationTest'
                --tests 'com.zillit.automation.tests.CreateProjectSubTypeTest'
                --tests 'com.zillit.automation.tests.ProjectListTest')
elif [[ "${1:-}" == "--flow" ]]; then
  # Just the ordered happy-path journey: create -> join -> select project.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.ProjectFlowE2ETest')
elif [[ "${1:-}" == "--sweep" ]]; then
  # Clean unit-by-unit flow: finish each unit fully (send every type + check
  # long-press on each) before the next — Bulletin → Call Sheet → … → Calendar.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.AllUnitsMediaTest'
                --tests 'com.zillit.automation.tests.CalendarTest')
elif [[ "${1:-}" == "--home" ]]; then
  # In-project suites (need a logged-in project): Home chat, media send/view/
  # reply, call-sheet, calendar.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.HomeChatTest'
                --tests 'com.zillit.automation.tests.PostingRightsTest'
                --tests 'com.zillit.automation.tests.MediaPreviewTest'
                --tests 'com.zillit.automation.tests.MediaSystemPickerTest'
                --tests 'com.zillit.automation.tests.ChatOptionsTest'
                --tests 'com.zillit.automation.tests.CallSheetTest'
                --tests 'com.zillit.automation.tests.RegressionStressTest'
                --tests 'com.zillit.automation.tests.AllUnitsMediaTest'
                --tests 'com.zillit.automation.tests.CalendarTest')
elif [[ "${1:-}" == "--calendar" ]]; then
  # CALENDAR ONLY — its own fresh session, nothing to do with chat. Full field
  # coverage, all pickers, and CRUD (create / view / update / delete + recurring).
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.CalendarTest')
elif [[ "${1:-}" == "--chat" ]]; then
  # CHAT ONLY — every chat suite, no calendar. Sending, options, media, stress.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.HomeChatTest'
                --tests 'com.zillit.automation.tests.PostingRightsTest'
                --tests 'com.zillit.automation.tests.MediaPreviewTest'
                --tests 'com.zillit.automation.tests.MediaSystemPickerTest'
                --tests 'com.zillit.automation.tests.ChatOptionsTest'
                --tests 'com.zillit.automation.tests.CallSheetTest'
                --tests 'com.zillit.automation.tests.RegressionStressTest'
                --tests 'com.zillit.automation.tests.AllUnitsMediaTest')
elif [[ "${1:-}" == "--email" ]]; then
  # EMAIL ONLY — its own fresh session. Landing/list, folder drawer, compose
  # (To/Cc/Bcc + subject + body + rich-text + attach + send) with validation,
  # open/read, reply/forward, delete, search, groups CRUD, settings.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.EmailTest'
                --tests 'com.zillit.automation.tests.EmailDrawerTest'
                --tests 'com.zillit.automation.tests.EmailExtrasTest'
                --tests 'com.zillit.automation.tests.EmailActionsTest'
                --tests 'com.zillit.automation.tests.EmailCalendarTest')
elif [[ "${1:-}" == "--cc" ]]; then
  # C&C (Chat & Calling) ONLY — landing, group create/manage, chat thread, calling.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.CcGroupCreateTest'
                --tests 'com.zillit.automation.tests.CcGroupChatTest'
                --tests 'com.zillit.automation.tests.CcGroupManageTest'
                --tests 'com.zillit.automation.tests.CcDirectChatTest'
                --tests 'com.zillit.automation.tests.CcChatMediaTest'
                --tests 'com.zillit.automation.tests.CcCallTest'
                --tests 'com.zillit.automation.tests.CcCallTabTest'
                --tests 'com.zillit.automation.tests.CcContactsTest')
elif [[ "${1:-}" == "--tools" ]]; then
  # TOOLS page — the two Bulletin-style chat modules (Confidential Info + Info)
  # PLUS the Tools page management (search, change group order, enable/disable).
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.ToolsManagementTest'
                --tests 'com.zillit.automation.tests.ConfidentialInfoTest'
                --tests 'com.zillit.automation.tests.InfoTest')
elif [[ "${1:-}" == "--manual-media" ]]; then
  # Interactive: YOU pick Video/Location/Contact on the device and reach the
  # send screen; automation sends + verifies. Set ZILLIT_PICK_WAIT for a longer
  # picking window (default 120s).
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.ManualPickSendTest')
elif [[ "${1:-}" == "--two-device" ]]; then
  # Cross-device, two-user: every message type is RECEIVED on the 2nd device,
  # edits update on the receiver, deletes disappear on the receiver.
  GRADLE_ARGS+=(--tests 'com.zillit.automation.tests.TwoUserChatTest')
elif [[ "${1:-}" == "--all" ]]; then
  # FULL BUILD SWEEP — every module, one after another (Home/Bulletin → Calendar →
  # Email → C&C → Tools), each run SEPARATELY so a degraded emulator on one module
  # can't kill the rest, then ONE combined HTML report for the developer.
  RUN_ALL=1
fi

# Push test media + pre-grant permissions to the target device(s) so media-send
# tests have content and don't stall on system permission dialogs.
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
FX=src/test/resources/fixtures
PKG="${ZILLIT_APP_PACKAGE:-com.zillit.zillitapp.dev}"

prep_device() {  # $1 = adb device serial (may be empty for the only device)
  local flag=""
  [[ -n "$1" ]] && flag="-s $1"
  echo "▶ Prepping device ${1:-<default>}…"
  for i in 1 2 3; do
    $ADB $flag push "$FX/zl_test_image.jpg" /sdcard/Pictures/zl_test_$i.jpg >/dev/null 2>&1
  done
  [[ -f "$FX/zl_test_doc.pdf" ]] && $ADB $flag push "$FX/zl_test_doc.pdf" /sdcard/Documents/zl_test_doc.pdf >/dev/null 2>&1
  [[ -f "$FX/zl_test_audio.wav" ]] && $ADB $flag push "$FX/zl_test_audio.wav" /sdcard/Music/zl_test_audio.wav >/dev/null 2>&1
  for f in Pictures/zl_test_1.jpg Documents/zl_test_doc.pdf Music/zl_test_audio.wav; do
    $ADB $flag shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/$f" >/dev/null 2>&1
  done
  for perm in RECORD_AUDIO READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_MEDIA_AUDIO POST_NOTIFICATIONS ACCESS_FINE_LOCATION READ_CONTACTS READ_CALENDAR; do
    $ADB $flag shell pm grant "$PKG" android.permission.$perm >/dev/null 2>&1
  done
}

if [[ -x "$ADB" && -f "$FX/zl_test_image.jpg" ]]; then
  if [[ "${1:-}" == "--two-device" ]]; then
    # Prep BOTH the sender and receiver devices.
    prep_device "${ZILLIT_SENDER_UDID:-${ZILLIT_UDID:-}}"
    [[ -n "${ZILLIT_RECEIVER_UDID:-}" ]] && prep_device "$ZILLIT_RECEIVER_UDID"
  else
    prep_device "${ZILLIT_UDID:-}"
  fi
fi

# Preflight: the Appium server MUST be running, or every suite fails at session
# creation with a cryptic SessionNotCreatedException. Fail fast with a clear fix.
if ! curl -s -m 4 http://127.0.0.1:4723/status >/dev/null 2>&1; then
  echo "✗ Appium server is NOT running on http://127.0.0.1:4723"
  echo "  Start it in another terminal, then re-run:"
  echo "    ANDROID_HOME=\$HOME/Library/Android/sdk npm run appium"
  exit 2
fi

UDID="${ZILLIT_UDID:-emulator-5554}"

# Relaunch the app to the foreground (a cold Appium session fails otherwise —
# the launcher activity is SplashActivity, and monkey is flaky at foregrounding).
relaunch_app() {
  $ADB -s "$UDID" shell am force-stop "$PKG" >/dev/null 2>&1
  $ADB -s "$UDID" shell am start-activity -n "$PKG/com.zillit.zillitapp.splash.SplashActivity" >/dev/null 2>&1
  sleep 7
}

if [[ "${RUN_ALL:-0}" == "1" ]]; then
  # ---- FULL SWEEP: each module as its own gradle run, results accumulated ----
  ACC="build/allresults"
  rm -rf "$ACC"; mkdir -p "$ACC"
  # Ordered module list — "Label::Class1 Class2 …" — runs from project setup all
  # the way through every module. (LoginFlowTest is NOT included: it needs a manual
  # OTP, so it can't run unattended — do the OTP login once before starting --all.)
  MODULES=(
    "Project create / join / list::CreateProjectValidationTest CreateProjectSubTypeTest JoinProjectTest ProjectListTest ProjectFlowE2ETest"
    "Home / Bulletin chat::HomeChatTest PostingRightsTest MediaPreviewTest MediaSystemPickerTest ChatOptionsTest CallSheetTest RegressionStressTest AllUnitsMediaTest"
    "Calendar::CalendarTest"
    "Email::EmailTest EmailDrawerTest EmailExtrasTest EmailActionsTest EmailCalendarTest"
    "C&C (Chat & Calling)::CcGroupCreateTest CcGroupChatTest CcGroupManageTest CcDirectChatTest CcChatMediaTest CcCallTest CcCallTabTest CcContactsTest"
    "Tools::ToolsManagementTest ConfidentialInfoTest InfoTest"
  )
  TEST_EXIT=0
  for entry in "${MODULES[@]}"; do
    label="${entry%%::*}"; classes="${entry##*::}"
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "▶ MODULE: $label"
    echo "════════════════════════════════════════════════════════════"
    relaunch_app
    args=(cleanTest test --continue)
    for c in $classes; do args+=(--tests "com.zillit.automation.tests.$c"); done
    ./gradlew "${args[@]}"
    [[ $? -ne 0 ]] && TEST_EXIT=1
    # Accumulate this module's JUnit XMLs (one file per test class, no name clashes).
    if ls build/test-results/test/*.xml >/dev/null 2>&1; then
      cp -f build/test-results/test/*.xml "$ACC"/ 2>/dev/null
    fi
    echo "▶ $label done — $(ls "$ACC"/*.xml 2>/dev/null | wc -l | tr -d ' ') result files so far."
  done
  # Point the report at the accumulated results (all modules).
  mkdir -p build/test-results/test
  rm -f build/test-results/test/*.xml 2>/dev/null
  if ls "$ACC"/*.xml >/dev/null 2>&1; then
    cp -f "$ACC"/*.xml build/test-results/test/ 2>/dev/null
  fi
else
  echo "▶ Running UI test suites…"
  ./gradlew "${GRADLE_ARGS[@]}"
  TEST_EXIT=$?
fi

echo "▶ Building shareable HTML report…"
node tools/make-report.mjs
REPORT_EXIT=$?

echo ""
echo "Shareable report:  build/report/zillit-report.html   ← send this to the developer"
echo "Raw Gradle report: build/reports/tests/test/index.html"
[[ $TEST_EXIT -ne 0 ]] && echo "⚠ Some tests failed/skipped — each is explained in the shareable report."
exit $(( TEST_EXIT || REPORT_EXIT ))
