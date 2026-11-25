---
name: zillit-automation-local-env
description: Running the Zillit Appium suite on this machine — dev build only, app source is checked out, one suite per Appium session
metadata:
  type: project
---

Operational facts for running the Zillit automation repo on this machine (checked 2026-11-20):

- **Dev build only (user's decision, 2026-11-24).** `run-ui.sh`, `run-and-report.sh` and `DriverFactory`
  refuse any package but `com.zillit.zillitapp.dev`; no `ZILLIT_FLAVOR` is needed. Before this,
  `run-and-report.sh` defaulted to `.qa` and ignored `ZILLIT_FLAVOR` (only auto-env.sh read it), so it
  opened QA between modules while the tests ran on dev. Never reintroduce a QA default. The
  `.claude/worktrees/*` copies still have the old default. The runner no longer force-stops the app on a
  physical phone (see [[zillit-two-device-roles]]: a cold start raises the app lock).
- **Run one suite per Appium session**: `./run-ui.sh --only <Class>`. Sessions degrade past
  roughly half an hour, and a single long run has lost the UiAutomator2 instrumentation
  mid-way and reported 38 false failures.
- The **app source IS available** at `/Users/vidyasagar/StudioProjects/Zillit-Android`
  (corrects an earlier note that said otherwise). Read resource ids, validation strings and
  visibility gating from the Kotlin/XML there rather than inferring them from device dumps
  — several "app defects" turned out to be test bugs that the source settled in a minute.
- The Java 17 toolchain is **provisioned by Gradle** now (commit 3e5c918), so a missing
  local JDK 17 is no longer a blocker.
- `adb` is not on `PATH`: prefix with `$HOME/Library/Android/sdk/platform-tools`.

**Why:** each of these presents as a test or product bug when hit — a run that aborts
instantly, mass failures with no assertion behind them, or a locator that looks wrong.
**How to apply:** run dev only (already the default), drive one class at a time, and check the
Android source before calling anything an app defect. See [[zillit-location-tool-deferred]].

**Updating the app on the test devices (2026-11-23):** build from a separate worktree at
`/Users/vidyasagar/StudioProjects/Zillit-Android-devbuild` (detached at `origin/development`; never
switch branches in the main app repo — it has the user's uncommitted work), `./gradlew
:app:assembleDevelopDebug`, then `adb install -r -d` under each device's lock. All installed builds
are signed with this Mac's debug key, so the update keeps every account signed in. `git fetch`
does not work here (no GitHub credentials) — ask the user to fetch if they pushed newer commits.
A third device, `emulator-5556` (AVD `ZL_Clone_2`, clone of the signed-in emulator), exists.

Emulators (2026-11-24): emulator-5554 = AVD Medium_Phone_API_37.0; emulator-5556 = AVD ZL_Clone_2; emulator-5558 = AVD ZL_Clone_3 (user asked for a third); emulator-5560 = AVD ZL_Clone_4 (1.5 GB RAM, `-memory 1536`; a fourth, user asked for more). Clones are APFS copies (`cp -c` per file, skip *.lock) of a signed-in AVD with AvdId/displayname renamed in config.ini + hardware-qemu.ini and a new <name>.ini; launch `emulator -avd <name> -port <even port> -no-snapshot-load -no-snapshot-save`. All three are signed in as Vidya Testing. The Mac (24 GB) swaps heavily with 3 emulators + several Gradle runs.
