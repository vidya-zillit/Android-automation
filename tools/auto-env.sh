#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Brings the whole test environment up so a run needs ZERO manual steps.
#
#   1. resolve adb / emulator from the SDK (no hardcoded paths)
#   2. make sure npm dependencies (Appium + driver) are installed
#   3. make sure exactly ONE target device is attached — boot the AVD if none is
#   4. grant io.appium.settings its location permissions (Android 14+ fix)
#   5. start the Appium server if it is not already listening
#
# Sourced by run-ui.sh. Every step is a no-op when it has already been done, so
# running it twice costs a second, not a reboot.
#
# Overrides (all optional):
#   ZILLIT_UDID          pick this device instead of auto-detecting
#   ZILLIT_AVD           boot this AVD when no device is attached
#   ZILLIT_APPIUM_PORT   default 4723
#   ZILLIT_NO_EMULATOR=1 never boot an emulator; fail instead
# ---------------------------------------------------------------------------

_ae_say()  { printf '  %s\n' "$*"; }
_ae_step() { printf '\n▶ %s\n' "$*"; }
_ae_fail() { printf '\n✗ %s\n' "$*" >&2; }

# --- 1. SDK tools ----------------------------------------------------------
# Same resolution order as run-and-report.sh: env → macOS default → PATH.
# Windows reports ANDROID_HOME with backslashes and needs the .exe names.
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ANDROID_SDK="${ANDROID_SDK//\\//}"
export ANDROID_HOME="$ANDROID_SDK"

# adb's mDNS discovery (Wi-Fi debugging announcements) panics on a name it cannot parse —
# platform-tools 37.0.1 on macOS 27.2.1, 44 crash reports on 2026-11-21, every one aborting
# in adbmdns_bridge::zero_config. Something on the local network advertises that name, so
# the adb SERVER dies every few seconds, and with it the UiAutomator2 instrumentation that
# runs under it: suites then read empty screens and fail or skip for no reason of their own.
# The runner never uses Wi-Fi pairing (USB and emulators only), so turn discovery off for
# any adb server started from here. Has no effect on a server already running WITH it on —
# if the symptom returns, `adb kill-server` once and run again.
export ADB_MDNS=0

ADB="$ANDROID_SDK/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$ANDROID_SDK/platform-tools/adb.exe"
[[ -x "$ADB" ]] || ADB="$(command -v adb 2>/dev/null || true)"
if [[ ! -x "$ADB" ]]; then
  _ae_fail "adb not found. Looked in $ANDROID_SDK/platform-tools and on PATH."
  _ae_say  "Set ANDROID_HOME to your SDK, e.g. export ANDROID_HOME=\$HOME/Library/Android/sdk"
  return 1 2>/dev/null || exit 1
fi

EMU="$ANDROID_SDK/emulator/emulator"
[[ -x "$EMU" ]] || EMU="$ANDROID_SDK/emulator/emulator.exe"

# --- 1b. flavour -----------------------------------------------------------
# One applicationId per flavour (app/build.gradle.kts): base com.zillit.zillitapp,
# develop adds .dev, qa adds .qa, prod adds nothing. The launcher activity is NOT
# suffixed — android:name=".splash.SplashActivity" resolves against the manifest
# namespace, which stays com.zillit.zillitapp for every flavour — so only the package
# changes here.
#
# Resource ids ARE prefixed by the applicationId, so this one variable also decides
# what every locator in the suite resolves against. Setting it wrong does not error;
# it just matches nothing, and the whole run skips or times out.
# DEV BUILD ONLY — qa, stg and prod are refused, so no run can open another build.
case "$(printf '%s' "${ZILLIT_FLAVOR:-dev}" | tr '[:upper:]' '[:lower:]')" in
  dev|develop) ;;
  *)
    _ae_fail "ZILLIT_FLAVOR=$ZILLIT_FLAVOR — this project automates the DEV build only."
    return 1 2>/dev/null || exit 1 ;;
esac
if [[ -n "${ZILLIT_APP_PACKAGE:-}" && "$ZILLIT_APP_PACKAGE" != "com.zillit.zillitapp.dev" ]]; then
  _ae_fail "ZILLIT_APP_PACKAGE=$ZILLIT_APP_PACKAGE — this project automates the DEV build only."
  return 1 2>/dev/null || exit 1
fi
export ZILLIT_APP_PACKAGE="com.zillit.zillitapp.dev"

APPIUM_PORT="${ZILLIT_APPIUM_PORT:-4723}"
APPIUM_URL="http://127.0.0.1:$APPIUM_PORT"
mkdir -p build

# --- 2. npm dependencies ---------------------------------------------------
APPIUM_BIN="node_modules/.bin/appium"
[[ -x "$APPIUM_BIN" ]] || APPIUM_BIN="node_modules/.bin/appium.cmd"
if [[ ! -x "$APPIUM_BIN" ]]; then
  _ae_step "Installing Appium and the UiAutomator2 driver (first run only)…"
  if ! npm install; then
    _ae_fail "npm install failed — is Node 18+ installed?"
    return 1 2>/dev/null || exit 1
  fi
  APPIUM_BIN="node_modules/.bin/appium"
fi

# --- 3. device -------------------------------------------------------------
# A phone connected over both USB and wireless debugging shows up TWICE: once as
# its plain serial and once as adb-<serial>-XXXX._adb-tls-connect._tcp. Counting
# both would make a single phone look like two devices and block the run, so the
# wireless twin is dropped whenever a plain serial is also present.
_ae_devices() {
  "$ADB" devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}' | grep -v '_adb-tls-connect\._tcp$' || true
}
_ae_devices_any() {
  "$ADB" devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}' || true
}

_ae_boot_avd() {
  if [[ "${ZILLIT_NO_EMULATOR:-}" == "1" ]]; then
    _ae_fail "No device attached and ZILLIT_NO_EMULATOR=1 — plug one in or start it yourself."
    return 1
  fi
  if [[ ! -x "$EMU" ]]; then
    _ae_fail "No device attached and no emulator binary at $ANDROID_SDK/emulator/emulator."
    _ae_say  "Plug in a phone with USB debugging on, or install the Emulator in the SDK Manager."
    return 1
  fi
  local avds avd
  avds="$("$EMU" -list-avds 2>/dev/null)"
  if [[ -z "$avds" ]]; then
    _ae_fail "No AVDs exist. Create one in Android Studio → Device Manager."
    return 1
  fi
  avd="${ZILLIT_AVD:-$(printf '%s\n' "$avds" | head -1)}"
  if ! printf '%s\n' "$avds" | grep -qx "$avd"; then
    _ae_fail "AVD '$avd' not found. Available:"
    printf '%s\n' "$avds" | sed 's/^/      /'
    return 1
  fi
  _ae_step "No device attached — booting emulator '$avd'…"
  nohup "$EMU" -avd "$avd" -netdelay none -netspeed full >build/emulator.log 2>&1 &
  # wait-for-device returns as soon as adb sees it, which is long before the
  # system is usable; sys.boot_completed is the real signal.
  "$ADB" wait-for-device
  local i
  for i in $(seq 1 180); do
    [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] && break
    sleep 2
  done
  if [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; then
    _ae_fail "Emulator '$avd' did not finish booting in 6 minutes (see build/emulator.log)."
    return 1
  fi
  _ae_say "booted."
}

_ae_pick_device() {
  local list
  list="$(_ae_devices)"
  [[ -z "$list" ]] && list="$(_ae_devices_any)"

  if [[ -z "$list" ]]; then
    _ae_boot_avd || return 1
    list="$(_ae_devices)"
    [[ -z "$list" ]] && list="$(_ae_devices_any)"
  fi

  # An explicit choice always wins — but only if it is actually attached,
  # otherwise the run fails much later with a confusing Appium error.
  if [[ -n "${ZILLIT_UDID:-}" ]]; then
    if printf '%s\n' "$list" | grep -qx "$ZILLIT_UDID"; then
      return 0
    fi
    _ae_fail "ZILLIT_UDID=$ZILLIT_UDID is not attached. Attached now:"
    printf '%s\n' "$list" | sed 's/^/      /'
    return 1
  fi

  local count
  count="$(printf '%s\n' "$list" | grep -c . || true)"
  if [[ "$count" -eq 1 ]]; then
    export ZILLIT_UDID="$list"
    return 0
  fi

  # More than one. config/config.properties already declares the project's
  # device, so use that when it is one of the attached ones — predictable, and
  # it keeps the common "emulator plus a plugged-in phone" case working.
  local cfg
  cfg="$(sed -nE 's/^zillit\.udid=(.*)$/\1/p' config/config.properties 2>/dev/null | tr -d '\r ' | head -1)"
  if [[ -n "$cfg" ]] && printf '%s\n' "$list" | grep -qx "$cfg"; then
    export ZILLIT_UDID="$cfg"
    _ae_say "$count devices attached — using zillit.udid from config/config.properties"
    return 0
  fi

  # Nothing to go on: refuse to guess. Choosing wrongly here means writing real
  # chat messages into a project on whichever device happened to sort first.
  _ae_fail "$count devices attached — say which one to use:"
  printf '%s\n' "$list" | sed 's/^/      ZILLIT_UDID=/'
  _ae_say ""
  _ae_say "e.g.  ZILLIT_UDID=$(printf '%s\n' "$list" | head -1) ./run-ui.sh --home"
  return 1
}

_ae_step "Looking for a device…"
_ae_pick_device || { return 1 2>/dev/null || exit 1; }
_ae_say "using $ZILLIT_UDID ($("$ADB" -s "$ZILLIT_UDID" shell getprop ro.product.model 2>/dev/null | tr -d '\r'), Android $("$ADB" -s "$ZILLIT_UDID" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r'))"

# --- 3b. is the app under test actually on this device? --------------------
# Without this the run starts, every suite fails to find its locators, and an hour
# later the report says "no project" — when the real answer was that this flavour
# was never installed on this phone.
ZILLIT_PKG="${ZILLIT_APP_PACKAGE:-$(sed -nE 's/^zillit\.appPackage=(.*)$/\1/p' config/config.properties 2>/dev/null | tr -d '\r ' | head -1)}"
# One `pm list packages` is not evidence. This platform-tools build drops a shell
# command now and then — measured here: the same query that returns 265 packages by
# hand came back EMPTY inside this script twice in a row, and the run aborted saying
# the app was not installed while it was sitting right there and launching by hand.
# An empty list means adb hiccuped, never that the device has no packages, so ask
# again before believing it.
_ae_pkg_list() {
  local out=""
  for _ae_try in 1 2 3 4 5; do
    out="$("$ADB" -s "$ZILLIT_UDID" shell pm list packages 2>/dev/null | tr -d '\r' | grep '^package:')"
    [[ -n "$out" ]] && { printf '%s\n' "$out"; return 0; }
    sleep 2
  done
  return 1
}

if [[ -n "$ZILLIT_PKG" ]]; then
  _ae_packages="$(_ae_pkg_list || true)"
  if [[ -z "$_ae_packages" ]]; then
    _ae_fail "adb would not list the packages on $ZILLIT_UDID (five tries)."
    _ae_say  "That is the device connection, not the app — nothing about the build is known."
    _ae_say  "Try:  adb kill-server && adb start-server,  then run again."
    return 1 2>/dev/null || exit 1
  fi
  if printf '%s\n' "$_ae_packages" | grep -qx "package:$ZILLIT_PKG"; then
    _ae_say "app:   $ZILLIT_PKG ($("$ADB" -s "$ZILLIT_UDID" shell dumpsys package "$ZILLIT_PKG" 2>/dev/null | grep -m1 versionName | tr -d '\r ' | cut -d= -f2))"
  else
    # Never fall back to another build that happens to be installed: dev or nothing.
    _ae_fail "$ZILLIT_PKG (the dev build) is not installed on $ZILLIT_UDID."
    _ae_say  "Zillit builds that ARE on this device:"
    printf '%s\n' "$_ae_packages" | grep 'com.zillit.zillitapp' | sed 's/^package:/      /'
    _ae_say  ""
    _ae_say  "Install the dev build there, then run again."
    return 1 2>/dev/null || exit 1
  fi
fi

# --- 4. Appium Settings helper (Android 14+) -------------------------------
# io.appium.settings runs a LOCATION-TYPED foreground service. From Android 14
# that service refuses to start unless ACCESS_FINE/COARSE_LOCATION are actually
# GRANTED — declaring them is not enough. A fresh emulator image grants them by
# default, a real phone does not, so every session on a physical device dies with
# the misleading "Appium Settings app is not running after 5000ms".
if "$ADB" -s "$ZILLIT_UDID" shell pm list packages 2>/dev/null | grep -q '^package:io.appium.settings$'; then
  for p in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION; do
    "$ADB" -s "$ZILLIT_UDID" shell pm grant io.appium.settings "android.permission.$p" >/dev/null 2>&1
  done
fi

# --- 5. Appium server ------------------------------------------------------
_ae_appium_ready() { curl -s -m 4 "$APPIUM_URL/status" >/dev/null 2>&1; }

if _ae_appium_ready; then
  _ae_step "Appium already running on $APPIUM_URL"
else
  _ae_step "Starting Appium on $APPIUM_URL…"
  nohup "$APPIUM_BIN" server --use-drivers uiautomator2 --port "$APPIUM_PORT" \
      >build/appium.log 2>&1 &
  echo $! >build/appium.pid
  for i in $(seq 1 40); do
    _ae_appium_ready && break
    sleep 1
  done
  if ! _ae_appium_ready; then
    _ae_fail "Appium did not come up within 40s — see build/appium.log"
    return 1 2>/dev/null || exit 1
  fi
  _ae_say "ready (log: build/appium.log, stop with: kill \$(cat build/appium.pid))"
fi

export ZILLIT_UDID
