#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# ONE command for a whole test run — nothing to start by hand.
#
#   ./run-ui.sh --home            # Home module
#   ./run-ui.sh --admin-settings  # Admin Settings module
#   ./run-ui.sh --all-in-project  # every module, one combined report
#   ./run-ui.sh                   # full suite (includes the manual-OTP E2E)
#
# It boots a device if none is attached, starts Appium if it is not running,
# runs the suites, builds the shareable HTML report and — when a webhook is
# configured — appends every case to the team Google Sheet.
#
# Module flags are exactly the ones run-and-report.sh accepts; ./run-ui.sh --help
# lists the common ones.
# ---------------------------------------------------------------------------
cd "$(dirname "$0")" || exit 1

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  cat <<'HELP'
Usage: ./run-ui.sh [module-flag]

Common modules
  --home              Home: chat, posting rights, media send/preview, call sheet, calendar
  --chat              Chat suites only (no calendar)
  --calendar          Calendar only — full CRUD and pickers
  --email             Email module
  --cc                Communication & Collaboration
  --tools             Tools module
  --recce             Recce
  --drive             Drive (use --drive-all for the full Drive sweep)
  --doc-dist-all      Document Distribution (all content suites)
  --admin-settings    Settings -> Admin Settings
  --settings          Settings tab
  --all-in-project    EVERY module, already logged in, one combined report
  (no flag)           the complete suite, including the manual-OTP E2E

  --call-sheet-upload The ZL-21154 Continuation / New / Replace sheet
  --devices           list attached devices and the Zillit builds on each, then exit

Choosing the build (flavour)
  ZILLIT_FLAVOR=dev      com.zillit.zillitapp.dev
  ZILLIT_FLAVOR=qa       com.zillit.zillitapp.qa     (config default)
  ZILLIT_FLAVOR=stg      com.zillit.zillitapp.stg
  ZILLIT_FLAVOR=prod     com.zillit.zillitapp        (also needs ZILLIT_ALLOW_PROD=1)

Choosing the device
  ZILLIT_UDID=<serial>   run on a specific device — ./run-ui.sh --devices lists them
  ZILLIT_AVD=<avd-name>  which emulator to boot when nothing is attached

Other switches
  ZILLIT_NO_OPEN=1       do not open the HTML report at the end
  ZILLIT_SHARE=0         do not push results to the Google Sheet
  -Dzillit.callSheetDestructive=true   also run the 'New wipes the unit' case

Every other flag run-and-report.sh knows still works — run it with a bad flag to see all of them.
HELP
  exit 0
fi

if [[ "${1:-}" == "--devices" ]]; then
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"; SDK="${SDK//\\//}"
  ADB="$SDK/platform-tools/adb"; [[ -x "$ADB" ]] || ADB="$SDK/platform-tools/adb.exe"
  [[ -x "$ADB" ]] || ADB="$(command -v adb || true)"
  [[ -x "$ADB" ]] || { echo "✗ adb not found — set ANDROID_HOME"; exit 1; }
  # for-over-capture, NOT a while-read pipeline: adb reads stdin, so inside a
  # `... | while read` it swallows the device list and only the first row survives.
  DEVS=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}' | grep -v '_adb-tls-connect\._tcp$')
  [[ -n "$DEVS" ]] || { echo "No devices attached. Plug one in, or let ./run-ui.sh boot an AVD."; exit 0; }
  printf '\nAttached devices\n'
  for d in $DEVS; do
    printf '\n  %s  —  %s, Android %s\n' "$d" \
      "$("$ADB" -s "$d" shell getprop ro.product.model </dev/null 2>/dev/null | tr -d '\r')" \
      "$("$ADB" -s "$d" shell getprop ro.build.version.release </dev/null 2>/dev/null | tr -d '\r')"
    PKGS=$("$ADB" -s "$d" shell pm list packages </dev/null 2>/dev/null | tr -d '\r' | grep 'com.zillit.zillitapp' | sed 's/^package://')
    [[ -n "$PKGS" ]] || printf '      (no Zillit build installed)\n'
    for pkg in $PKGS; do
      ver=$("$ADB" -s "$d" shell dumpsys package "$pkg" </dev/null 2>/dev/null | grep -m1 versionName | tr -d '\r ' | cut -d= -f2)
      case "$pkg" in
        *.dev) fl="dev " ;;
        *.qa)  fl="qa  " ;;
        *.stg) fl="stg " ;;
        *)     fl="prod" ;;
      esac
      printf '      ZILLIT_FLAVOR=%s  %-34s %s\n' "$fl" "$pkg" "$ver"
    done
  done
  printf '\nRun on one of them:\n  ZILLIT_UDID=<serial> ZILLIT_FLAVOR=<flavour> ./run-ui.sh --home\n\n'
  exit 0
fi

# 1 + 2 + 3: device, Appium, permissions. Exits with a clear reason if it can't.
# shellcheck source=tools/auto-env.sh
source tools/auto-env.sh || exit $?

# 4: the suites themselves. run-and-report.sh already builds the HTML report.
./run-and-report.sh "$@"
STATUS=$?

# 5: sharing ---------------------------------------------------------------
REPORT="build/report/zillit-report.html"
if [[ -f "$REPORT" ]]; then
  # Keep every run instead of overwriting: a module name + timestamp makes the
  # file self-describing once it is out of the repo and in a chat thread.
  MODULE="${1:-full-suite}"; MODULE="${MODULE#--}"
  STAMP="$(date +%Y%m%d-%H%M)"
  ARCHIVE="build/report/zillit-${MODULE}-${STAMP}.html"
  cp -f "$REPORT" "$ARCHIVE" 2>/dev/null

  printf '\n▶ Share this run\n'
  printf '  %s\n' "$ARCHIVE"
  printf '  (self-contained HTML — attach it anywhere, no server needed)\n'

  # Google Sheet: one row per test case, appended to the team sheet. Only runs
  # when a webhook is configured, so it is silent on a fresh clone.
  if [[ "${ZILLIT_SHARE:-1}" != "0" ]]; then
    if [[ -n "${ZILLIT_SHEET_WEBHOOK:-}" ]] || grep -qE '^zillit\.sheetWebhook=.+' config/config.properties 2>/dev/null; then
      printf '\n▶ Appending cases to the team Google Sheet…\n'
      node tools/report-to-sheet.mjs || printf '  ⚠ Sheet update failed — the HTML report above is unaffected.\n'
    fi
  fi

  if [[ "${ZILLIT_NO_OPEN:-}" != "1" ]]; then
    if command -v open >/dev/null 2>&1; then open "$ARCHIVE" >/dev/null 2>&1
    elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$ARCHIVE" >/dev/null 2>&1
    elif command -v start >/dev/null 2>&1; then start "" "$ARCHIVE" >/dev/null 2>&1
    fi
  fi
fi

exit $STATUS
