---
name: zillit-device-run-stability
description: "Why Appium runs on this Mac lose the device mid-run — adb mDNS crash, orphaned Appium sessions, manual uiautomator dump — and the fixes"
metadata:
  node_type: memory
  type: project
  originSessionId: b1614600-558f-4f70-91b2-a2665802f9c5
  modified: 2026-11-21T14:34:02.135Z
---

Three causes of "UiAutomator2 instrumentation is gone" / empty screens on this machine, found
2026-11-21 while running the Continuity and Schedule Distribution suites:

1. **adb server crashes in its mDNS bridge.** platform-tools 37.0.1 on macOS 27.2.1 aborts in
   `adbmdns_bridge::zero_config` (crash reports `~/Library/Logs/DiagnosticReports/adb-*.ips`,
   44 on that day) because something on the LAN advertises a name it can't parse. The server
   dies every few seconds and takes the instrumentation with it. Fix: `ADB_MDNS=0` —
   `tools/auto-env.sh` now exports it. If it recurs, check `ps eww <adb server pid>` for the
   flag; Android Studio may start its own server without it. It is NOT the Pixel's USB cable
   and NOT Studio's kill requests, though both were blamed first.
2. **Orphaned Appium sessions.** Killing a run's test JVM leaves its Appium session alive; 900s
   later its newCommandTimeout fires and its shutdown force-stops `io.appium.uiautomator2.server`
   — killing whatever run is using the device by then. When stopping a run, DELETE its sessions
   (ids are in `build/appium.log`) or restart Appium.
3. **Never run `adb shell uiautomator dump` during a run** — it takes the UiAutomation
   connection and kills the session. Use `screencap` to look at the screen.

**Why:** each presents as a test or app failure. **How to apply:** before blaming a locator,
check adb crash reports and Appium's log for "Shutting down because we waited". Related:
[[zillit-automation-local-env]].
