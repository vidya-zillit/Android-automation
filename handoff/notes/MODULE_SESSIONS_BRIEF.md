# Brief for the five module sessions (2026-11-23)

Shared by: Schedule D.O.D. · Supporting Artistes / Extras · Costumes & Set Sync · Z-E-Signature · Crew DOOD.
Coordinator: the session that started you. Goal: each module
automated **today** — test-case doc + page objects + suite, run on a device, results reported.

## 1. Repos and baseline
- Automation repo (main checkout, has UNCOMMITTED work you need): `/Users/vidyasagar/StudioProjects/Android-automation`
- You run in your own git worktree cut from the last commit, which still has the OLD flat package
  layout. **First thing:** copy the current working tree in (no build dirs):
  ```bash
  rsync -a --delete --exclude build --exclude .gradle --exclude node_modules --exclude .git --exclude .idea --exclude '.claude/worktrees' \
    /Users/vidyasagar/StudioProjects/Android-automation/ ./
  ```
  Then `./gradlew -q compileTestJava` must pass before you add anything.
- App source (read ids, strings, gating here — never guess): `/Users/vidyasagar/StudioProjects/Zillit-Android`
  (Kotlin + `app/src/main/res`). Tools tiles route in `bottomNav/tools/Tools.kt`.
- Web app for business rules: `/Users/vidyasagar/StudioProjects/zillit_web` (`src/`, `e2e/*_TEST_PLAN.md`).

## 2. Conventions (copy an existing module)
- Best template: Schedule Distribution — `tests/scheduledist/ScheduleDistBaseTest.java`,
  `ScheduleDistTest.java`, `pages/scheduledist/*`, doc `SCHEDULE_DIST_MODULE_TESTCASES.md`.
  Newest (list/menus/dialog helpers you can reuse): `pages/workflow/DocWorkflowPage.java`.
- Packages: `com.zillit.automation.pages.<module>` and `com.zillit.automation.tests.<module>`;
  shared pages in `pages.common` (`VisiblePage`, `ToolsPage`, `BottomNavPage`, `BasePage`).
- `@Order(TestOrder.<CONST>)` — constants are ALREADY reserved, do not edit `TestOrder.java`:
  `SCHEDULE_DOD=171`, `SA_EXTRAS=172`, `COSTUMES_SET_SYNC=173`, `E_SIGNATURE=174`, `CREW_DOOD=176`.
- Everything you create is named with `ZL_AUTO_` and is swept by a final `…-99` cleanup case.
- `assumeTrue` = SKIP (precondition missing), `assertTrue` = FAIL (product wrong). Re-check QA sheet
  bugs as cases tagged `[QA <id>]`; still-open ones are expected to fail.
- Doc: `<MODULE>_TESTCASES.md` at repo root, same style as `SCHEDULE_DIST_MODULE_TESTCASES.md`
  (cases table with quoted app strings and real ids, "From the QA sheet" table, findings).
- **Do not edit** shared files: `run-and-report.sh`, `run-ui.sh`, `tools/make-report.mjs`,
  `MODULE_AUTOMATION_LIST.md`, `README.md`, `TestOrder.java`, anything in `pages/common`, `core/`,
  `pages/workflow`. If you need a change there, say so in your report. Write the runner flag you'd
  want (name + one-line help + comment block) into your report; the coordinator wires it.

## 3. Devices — SHARED, use the lock
- Devices: Pixel 7 `34281FDH2005XC` (user's phone, Zillit **dev** logged in as *Vidya Pixel*, project
  ZL-Android-Automation, admin) and emulator `emulator-5554` (*Vidya Testing*, admin; the coordinator
  is using it for Call Sheet / Production Report until it says otherwise). One Appium server is
  already up on 4723 — never start/kill Appium or adb servers.
- **Only touch a device while you hold its lock** (adb input, screencap, uiautomator dump, a run):
  ```bash
  L="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c/locks"
  mkdir -p "$L"; until mkdir "$L/34281FDH2005XC" 2>/dev/null; do sleep 20; done
  echo "<your module> $(date)" > "$L/34281FDH2005XC/owner"
  # … your walk or run (keep it under ~25 min) …
  rm -rf "$L/34281FDH2005XC"
  ```
  Release it as soon as you're done, even on failure. Prefer short holds: explore with the source first.
- Env for every run: `export ANDROID_HOME=$HOME/Library/Android/sdk PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ADB_MDNS=0 ZILLIT_FLAVOR=dev ZILLIT_UDID=<locked serial>`
  then `./run-and-report.sh --only <YourTestClass>` (NOT run-ui.sh — it posts to the team sheet).
- Never `uiautomator dump` while someone else's run is on that device (the lock prevents it).
- If the Pixel shows its lock screen, you cannot unlock it (PIN): message the coordinator
  (the session that started you — `SendMessage`) and wait.

## 4. QA sheets (read with Claude in Chrome — the user is signed in)
Module testing report: `https://docs.google.com/spreadsheets/d/1Bh8KWbRAy8fU_32UYil2wiYHlXcK7ePh7-SX2EXn33U/`
Read a tab as plain text: navigate to
`…/gviz/tq?tqx=out:html&sheet=<Tab%20Name>&tq=select%20A%2CB%2CE%2CH%2CJ%2CK%2CL%2CM%2CQ%2CR`
and `get_page_text` (columns: Bug ID, Title, Android status, Severity, Screen, Steps, Expected, Actual, Test case, Notes).
Tabs: `Schedule DOD`, `Costume & Sync`, `E-Signature`, `Crew List`, `Summary`. (No Supporting Artistes / Crew DOOD tab — check `Summary` and `By platform`.)
Bug sheet: `https://docs.google.com/spreadsheets/d/1wdBSmZA-gAiMl2qzaeLqlVTv9iUp0e9GPu8uMV3S2h0/` tab `Forms & Signature`.
Close the tabs you open.

## 5. Done means
1. `<MODULE>_TESTCASES.md` written from source + sheet (+ what the device showed).
2. Page objects + suite compile; suite run on a device with the lock; failures triaged
   (test bug fixed vs app bug confirmed from source) — rerun until only real app bugs fail/skip.
3. Nothing `ZL_AUTO_` left behind that the app allows deleting.
4. Report (first 400 chars matter): pass/fail/skip counts, real app bugs found, files added,
   runner flag text you want. Do NOT commit; the coordinator copies your new files into the main tree.

## Update (coordinator)
- **Both devices are usable through the lock**: `locks/34281FDH2005XC` (Pixel) and `locks/emulator-5554`
  (emulator). The coordinator takes the same locks for its own runs. Take whichever is free; poll both.
- The Pixel goes to its PIN lock screen on timeout (`adb shell dumpsys window | grep isKeyguardShowing`).
  If it is locked, release its lock, tell the coordinator, and use the emulator.

## Update 2 — web E2E report (user's reference)
The web automation report is split per module in `web-e2e/` next to this brief (`INDEX.md` lists them;
source `/Users/vidyasagar/Downloads/Zillit E2E Report`). **Compare your test cases with your module's
file**: add any web case that applies on Android and is missing, and fix a case of ours the web shows is
wrong. Note the mapping (web test → our case id, or "web-only") in a "From the web E2E report" section
of your `_TESTCASES.md`. Files: `schedule-dod.md`, `costume-and-sync.md` (+ `costumes-and-set-sync.md`),
`e-signature.md` (15 web tests), `crew-list.md` / `crew-dood.md`; Supporting Artistes has none.

## Update 3 — a third device
`emulator-5556` (AVD `ZL_Clone_2`, a clone of emulator-5554: same user *Vidya Testing*, same project,
dev app signed in). Lock dir `locks/emulator-5556`. Single-device suites can use any of the three:
poll all three locks and take the first free one, set `ZILLIT_UDID` to it.
