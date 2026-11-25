# Brief for the "missing web cases" agents (2026-11-23)

Goal: add the Android cases that the web E2E report has and our suites lack, one module group per agent.

## Where things are
- Main repo (edit here): `/Users/vidyasagar/StudioProjects/Android-automation`. Appium 2 + JUnit 5 + Page Objects.
- Gap analysis per module, with proposed Android cases, ids and steps:
  `/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c/web-e2e/gaps/<module>.md`
  The raw web cases are in `web-e2e/<module>.md`.
- Android app source (development, matches the installed build Zillit_32539-dev): `/Users/vidyasagar/StudioProjects/Zillit-Android-devbuild`. Read it before calling a case missing or web-only.
- Summary: `WEB_E2E_GAP_SUMMARY.md` and `REMAINING_MODULES_AND_WEB_GAPS.md` in the repo.
- Memory notes worth reading: `/Users/vidyasagar/.claude/projects/-Users-vidyasagar-StudioProjects-Android-automation/memory/*.md`.

## Devices (3, shared by all agents)
- `emulator-5554` and `emulator-5556`: user Vidya Testing. `34281FDH2005XC` (Pixel 7): user Vidya Pixel, admin, the second user for two-device cases. **Since 2026-11-23 ~22:00 the Pixel user's department is Catering (a caterer)** — use it as the caterer side for Catering cases, and don't assume it is a plain crew member elsewhere.
- All run Zillit_32539-dev. Package `com.zillit.zillitapp.dev`. Never enter the Pixel PIN; if it is locked, use another device.
- **Lock before using a device:** `mkdir "<notes>/locks/<serial>"` succeeds only if free; write an `owner` file with your name, classes and time inside; `rm -rf` the dir when done (use a trap). Never touch a device whose lock you don't hold. Two-device cases take both locks.
- The Drive agent may hold one or both emulators for a while. Wait (poll every 30–60 s) or use another free device.
- adb: always `ADB_MDNS=0`, `ANDROID_HOME=$HOME/Library/Android/sdk`, platform-tools on PATH. Never run `uiautomator dump` while a suite runs on that device. If a run dies, delete orphaned Appium sessions (`curl -X DELETE http://127.0.0.1:4723/session/<id>`, ids from `build/appium.log`).

**Pixel back in use (22:35):** the user cleared the App Lock prompt; the app opens to the project list again. If a Pixel suite ever loops open→close with every case skipping "could not be returned to a bottom-nav screen", STOP it at once, release the lock and tell the coordinator — don't let it keep relaunching the app.

## How to run
- Gradle allows one build per folder, so run from your OWN copy: rsync the main repo into
  `/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad/gap-<agent>/`
  (`rsync -a --delete --exclude build --exclude .gradle --exclude .git --exclude .idea --exclude .claude`), then
  `ZILLIT_FLAVOR=dev ZILLIT_UDID=<serial> ./run-and-report.sh --only <Class>` (two devices: also `ZILLIT_RECEIVER_UDID`).
  See `scratchpad/lane.sh` for a working example. Results: `build/test-results/test/TEST-*.xml`.
- **Never** run `run-ui.sh` (it posts to the team Google Sheet). Never commit, push, stash, reset or switch branches.
- Runs over ~2 minutes go in the background.
- If the build fails in a file you don't own, another agent is mid-edit: wait 2 minutes, rsync again, retry.

## Rules for the code
- Edit only the files your brief gives you. Shared files (BaseTest, DriverFactory, TestOrder.java, run-and-report.sh, any page object owned by another agent): do NOT edit; put helpers in your own page/test files, and report anything you need changed.
- **TestOrder constants now exist** (added by the coordinator, 2026-11-23): SCHEDULE_DIST_TWO_USER 192, BOX_HISTORY_TWO_USER 193, DOCDIST_WEB_GAP 195, CALENDAR_RECURRING_SCOPE 200, CALENDAR_WEB_GAP 201, CALENDAR_INVITE 202, HOME_WEB_GAP 205, PERMISSION_GRID 215, ADMIN_SETTINGS_WEB_GAP 216, CATERING_TWO_USER 217, MAP_WEB_GAP 220, MAP_ZONE_GUARDS 221. Your new classes in the main repo were switched to them — keep that line. For another new class, ask the coordinator for a constant; never work around run-and-report's literal-@Order guard (or any other check), not even in your private copy.
- Prefer new `@Test` methods in the existing suites. If a new test class is needed, give it a `@Order(TestOrder.X)` constant — the coordinator adds constants: put the class, but tell the coordinator the constant name and your assigned number; until then you may use the literal number from your range.
- Each new case: an id from the gap file (e.g. `// [WEB DD-23]`), a `@DisplayName`, ZL_AUTO prefixed data, cleanup in `finally`/sweep.
- `assertTrue` for behaviour (a reproducing app bug must FAIL, naming the bug id); `assumeTrue` only for missing preconditions, always with a reason (ReasonLogger). No test that can never fail.
- Email: only `TestEmails.recipient()` / `TestEmails.self()`. Never invent an address. Never send anything to people outside the two test accounts. Never tap Send on "Let others know you updated this file?" (tap Skip).
- Don't change project settings, roles or rights for real users; if a case needs it, restore in `finally`, or skip with a reason.
- Go in the gap file's priority order (High / bug-reproducing first). Do as many as you reliably can; a smaller set that runs green (or fails only on real bugs) beats many flaky ones.

## Report back
- Update the module's `*_TESTCASES.md` (or add a "Web gap cases" section) with each case, its web id and its last result.
- Final message: files changed, cases added (by id), run results (pass / fail with real bug / skip with reason), cases left undone and why, and any shared-file change you need.

## UPDATE 23:55 — the user wants ALL remaining cases done, as soon as possible
- Scope is now every MISSING (and partial / weak / never-fails) row in your gap file(s), not just the priority ones. Each row must end as: implemented (with a run result), or "not automatable" with a concrete reason in the module's *_TESTCASES.md (offline/network fault on a shared device, changing real users' rights with no safe restore, needs a fixture we don't have, web-only).
- Speed: write in large batches, compile once per batch in your own copy, and use a device only to run (release the lock right after). Don't hold a device while writing code. Don't rerun a whole suite to check one fix — use `--only 'Class.method'` where run-and-report supports it.
- C&C (tests/cc, pages/cc) moved to a dedicated C&C agent; the small-modules agent no longer touches those files.
- A literal class-level @Order in the MAIN repo blocks every agent's runs (the preflight checks the whole tree). New class: keep it in your private copy until the coordinator has added its constant (INFO_TWO_USER 210 added 00:20); free numbers in your range are yours to request.

## UPDATE 00:55 — a THIRD emulator: `emulator-5558`
AVD ZL_Clone_3 (clone of ZL_Clone_2), user Vidya Testing, project ZL-Android-Automation, Zillit_32539-dev. Same lock protocol (`locks/emulator-5558`). Add it to any device list your lane scripts pick from (e.g. `emulator-5554 emulator-5556 emulator-5558`). Memory on the Mac is tight: don't run more than one Gradle test JVM per agent at a time.

## RULE 00:20 — no idle device holds
- At most ONE emulator per agent at a time (two only for a two-emulator case that is actually running).
- Two-device runs: take the Pixel lock first, then an emulator; never hold an emulator while waiting for the Pixel.
- Release a lock the moment your run ends; never hold a device while writing code.

## UPDATE 00:40 — a FOURTH emulator: `emulator-5560`
AVD ZL_Clone_4, 1.5 GB RAM, Vidya Testing, current build. Lock dir `locks/emulator-5560`. Devices now: emulator-5554, 5556, 5558, 5560 + Pixel 34281FDH2005XC. Add 5560 to your device lists.
- Before releasing a device lock: kill your test JVM(s) for that device and delete your Appium sessions on it — a leftover JVM collided with another agent's run on emulator-5554 at 00:59.

## RULE 01:50 — emulators are handed out by a QUEUE (fairness)
Some agents waited 1.5 h because faster pollers always won. From now on:
- To get an emulator, append ONE line with your short name to `<notes>/QUEUE` (names in use: home, casting, costume, location, cc, info, catering, calendar, docdist, drive, map, small) — once per run you want, not per poll.
- The coordinator's dispatcher hands each freed emulator to the head of the queue by writing owner `RESERVED:<name> …` into its lock. Accept a lock whose owner starts with `RESERVED:<your name>`: overwrite the owner with your line and run. Reservations not picked up in 10 min are released.
- STOP grabbing free emulator locks directly with mkdir (the dispatcher takes free ones within a second anyway). The Pixel is not queued: keep using mkdir on `locks/34281FDH2005XC`, Pixel first for two-device runs.
- DEADLOCK RULE (04:40): never hold an EMULATOR while waiting for the Pixel. Two-device runs: Pixel first (mkdir), THEN queue for an emulator. If you already hold an emulator and need the Pixel, finish your single-device work, release the emulator, and start over Pixel-first.
- PIXEL RULE (05:20): NEVER terminate/force-stop/cold-restart the Zillit app on the Pixel (terminateApp, am force-stop, relaunchApp, fullReset). A cold start shows Zillit's fingerprint/PIN app lock, which only the user can clear, and every later Pixel run then loops. Recover on the Pixel with BACK / activateApp only.
