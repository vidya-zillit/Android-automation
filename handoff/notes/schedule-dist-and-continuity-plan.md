# Schedule Distribution + Continuity — planning hand-over (2026-11-21)

Deliverables (automation repo, uncommitted, docs only — no Java, runner or .run touched):
- `SCHEDULE_DIST_MODULE_TESTCASES.md` — prefix `SD-`, 47 cases, 10 findings, ids for 23 layouts
- `CONTINUITY_MODULE_TESTCASES.md` — prefix `CT-`, 58 cases, 14 findings, ids for 28 layouts

Proposed wiring: a NEW range `170–189 · tools overflow · VIDYA` in TestOrder
(`SCHEDULE_DIST = 170`, `CONTINUITY = 175`), flags `--schedule-dist` / `--continuity`,
`.run` files `Zillit_Schedule_Distribution.run.xml` / `Zillit_Continuity.run.xml`.
Reason for not taking 120/122: those are the gaps Bhavik's admin block keeps for new admin modules.

QA sheet folded in (read through the user's Chrome; the sheet is not link-viewable, every export URL 401s).
It is a BUG sheet (ID / Layer Web|Backend / Severity / … / Status), NOT a Web/Android/Desktop/iOS verdict grid.
Schedule Distribution tab: BUG-04, 05, 11, 12, 21. Continuity tab: BUG-06, 07, 13, 14, 15, 17, 18, 20, 22, WEB-06, WEB-08.
Each doc's "From the QA sheet" table maps every row to an Android case or says why the UI cannot reach it.
Biggest one for Android: BUG-14 (edit = whole-item replace) + the Android edit body has no attachment → CT-41.

Design consequences worth knowing before writing code:
- Schedule Full / One Line rows can NEVER be deleted from the UI (ScheduleVM.kt:199) and have no
  name field (schedule_name always null). Plan: one sentinel row per tab (schedule date 31 DEC 2099),
  uploaded once, Replaced on every later run. Pages are deletable and swept normally.
- Continuity scene numbers must start with a digit, ≤15 chars → fixtures `99ZLAUTO<run id>`.
  A record is created asynchronously (upload → POST with empty callbacks → socket), so "added" is a
  wait-for-row, never a confirmation message.
- Continuity grid shimmer (`loaderView`) never stops after an API error → UiAutomator never idles.

Source digests (long, line-referenced) in the subagent scratchpad — they may be cleared:
/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad/{schedule,continuity}_digest.md

## Update — suites written, wired and run (2026-11-21, emulator, ZL-Android-Automation)
- ContinuityTest (57 cases, TestOrder 175, --continuity, Zillit_Continuity.run.xml): 53 pass. 3 app defects
  confirmed: CT-32 (finding 6 recycled note), CT-44 (finding 11), CT-84 (finding 4 Edit→Distribute). CT-83 skips
  (one department).
- ScheduleDistTest (44 cases, TestOrder 170, --schedule-dist, Zillit_Schedule_Distribution.run.xml): 40 pass.
  2 app defects: SD-04 (new finding 11, blank Pages tab: deleted folders still returned, empty check unfiltered),
  SD-39 (finding 4, 4 of 5 runs). SD-51/52 opt-in publish.
- No FATAL/ANR in logcat across all runs.
- Environment: adb 37.0.1 mDNS panic → ADB_MDNS=0 exported in tools/auto-env.sh. See memory note
  zillit-device-run-stability for orphaned Appium sessions / uiautomator dump.
- Shared files touched (additive only): TestOrder.java, run-and-report.sh, run-ui.sh, tools/make-report.mjs,
  tools/auto-env.sh. New: pages/VisiblePage, Continuity*Page, Schedule*Page, tests/Continuity*, tests/ScheduleDist*.
- Nothing committed.
