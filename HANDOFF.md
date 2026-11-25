# HANDOFF — Zillit Android automation (2026-11-24 ~08:00 IST)

Written by the coordinating Claude session before the account limit ran out. **Everything is stopped**:
no agents, no test runs, no watchers, no Appium sessions, all device locks and the queue cleared.
Give this file to the next Claude session (any account) and say: *"Read HANDOFF.md and continue."*

---

## 0. Read first
1. This file.
2. `handoff/memory/*.md`: the previous account's memory notes (local env, device roles, run stability).
   A new account doesn't have them, so read them, or copy them into its own memory folder.
3. `handoff/notes/WEB_GAP_AGENTS_BRIEF.md`: the rules every agent followed (devices, locks, queue, code
   rules). Its "UPDATE"/"RULE" sections at the end are the hard-won ones.
4. `REMAINING_MODULES_AND_WEB_GAPS.md` and `WEB_E2E_GAP_SUMMARY.md`: what the work was about.
5. Per-module test-case docs (`*_TESTCASES.md` in the repo root): every case, its web id and its last result.

## 1. Git state: NOTHING IS COMMITTED
- Repo `/Users/vidyasagar/StudioProjects/Android-automation`, branch **`vidya_bhavik`**, last commit `36e1493`.
- About **110 modified + 73 new files** are uncommitted (all of today's and last night's work). The main tree
  **compiles** (`./gradlew compileTestJava` exit 0, checked at handoff).
- First thing to do with the user: **ask whether to commit** (suggested: one commit on `vidya_bhavik`).
  Don't commit without asking. `handoff/` is this handoff's own folder. It's fine to commit it or to leave it out.

## 2. Environment (Mac, user vidyasagar)
- Appium 2 (UiAutomator2) on port 4723. JUnit 5, Gradle. Runner: `./run-and-report.sh --only <Class or Class.method>`
  (dev build only: QA/stg/prod are refused, no ZILLIT_FLAVOR needed). **Never use `run-ui.sh`**: it posts to the team Google Sheet.
- Always `export ADB_MDNS=0` (adb crashes in mDNS otherwise). Android Studio starts its own adb without it.
- App build `Zillit_32539-dev` (package `com.zillit.zillitapp.dev`), built from `origin/development` in
  `/Users/vidyasagar/StudioProjects/Zillit-Android-devbuild` (`./gradlew :app:assembleDevelopDebug`, `adb install -r -d`).
- Devices (all project **ZL-Android-Automation**, a TV project):

| Serial | What | User |
|---|---|---|
| emulator-5554 | AVD `Medium_Phone_API_37.0` | Vidya Testing (admin) |
| emulator-5556 | AVD `ZL_Clone_2` (clone) | Vidya Testing |
| emulator-5558 | AVD `ZL_Clone_3` (clone) | Vidya Testing |
| emulator-5560 | AVD `ZL_Clone_4` (clone, 1.5 GB, `-memory 1536`) | Vidya Testing |
| 34281FDH2005XC | Pixel 7 (USB) | Vidya Pixel: admin, department **Catering**, approver for Call Sheet / Production Report |

  Start an emulator with `~/Library/Android/sdk/emulator/emulator -avd <AVD> -port <5554|5556|5558|5560> -no-snapshot-load -no-snapshot-save`.
  The Mac has 24 GB and swaps with 4 emulators. emulator-5556 crashed once (restarted fine).
- Emails: only `TestEmails.recipient()` (vidyapixel…@zillit.net) / `TestEmails.self()` (vidyatesting…@zillit.net).
- **Pixel rules:**
  - Stay awake is ON. Never enter the device PIN.
  - **Never cold-start the app on the Pixel** (`terminateApp`, `am force-stop`, `relaunchApp`, fullReset). Zillit then shows a
    fingerprint/PIN **App Lock** that only the user can clear, and every Pixel run loops open/close.
    Recover with BACK / `activateApp` only. If it happens, stop the Pixel runs and ask the user to unlock.
- Data rule: create only `ZL_AUTO…`/`QA_…` data and delete only what the run created. **Never touch the "Mountain View" Map
  city.** `MapCityMultiTest` and `MapLocationFlowTest` LF-01/LF-99 can create and delete a real-named city: don't run
  them until they're changed.

## 3. Running many things in parallel (what worked)
- Each runner uses its **own rsync copy** of the repo (Gradle allows one build per folder):
  `rsync -a --delete --exclude build --exclude .gradle --exclude .git --exclude .idea --exclude .claude <repo>/ <copy>/`
- **Device locks:** `mkdir "<notes>/locks/<serial>"` + an `owner` file. Remove the lock when done.
  The notes folder was `~/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c`.
  A new account can use any shared folder, e.g. `handoff/locks`.
- **Emulator QUEUE + dispatcher** (`handoff/scripts/dispatcher.sh`): agents append their name once to `QUEUE`. The
  dispatcher hands each freed emulator to the head of the queue by writing owner `RESERVED:<name>`. The Pixel is not
  queued: take it with mkdir, **Pixel first** for two-device runs, and never hold an emulator while waiting for the Pixel.
- Before releasing a lock: kill your test JVM and delete your Appium sessions
  (`curl -X DELETE http://127.0.0.1:4723/session/<id>`).
- `handoff/scripts/coord-2dev.sh <specs…>`: runs two-device suites (Pixel + a queued emulator). Use it for §5.

## 4. Module status

| Module | Status | Doc |
|---|---|---|
| Call Sheet Creation / Production Report (2 devices) | Done earlier; CSC 30/5/1, approval 8/8; PR 19/4, approval 5/1/2 | CALL_SHEET_CREATION_…, PRODUCTION_REPORT_CREATION_… |
| Schedule D.O.D. / Crew DOOD / Z-E-Signature | Done (copied in from linked sessions) | SCHEDULE_DOD_…, CREW_DOOD_…, E_SIGNATURE_… |
| Chat → document editor | 6 pass; C&C opens Google Docs (app bug); Location case still to write | CHAT_DOCUMENT_EDITOR_TESTCASES.md |
| Home + Call Sheet unit | **Done**: 26 pass, 9 real bugs. 7 two-device cases NOT RUN | HOME_CHAT_TESTCASES.md |
| Calendar + Email | **Done**: CalendarTest 29/29; 8 bugs; invites 10 pass + #135 bug | CALENDAR_EMAIL_WEB_GAP_TESTCASES.md |
| Info / Confidential Info | **Done**: CI 73/81; 5 bugs; voice-note case unconfirmed; InfoTwoUserTest NOT RUN | INFO_CONFIDENTIAL_INFO_TESTCASES.md |
| Document Distribution | **Done**; no new bugs on this build; a few fixed cases not re-run | DOCUMENT_DISTRIBUTION_TESTCASES.md |
| C&C | **Done**: 17/17 written; bug CNC-03; CC-15 (two-device) NOT RUN | CC_MODULE_TESTCASES.md |
| Casting Main + Background | **Done**: 31 + 33 pass; bugs CAST-19/21/34; CastingTwoUserTest NOT RUN | CASTING_TESTCASES.md |
| Costume Main + Background (Wardrobe) | **Done**: 57/70; bugs COS-BUG-1/2; CostumeTwoUserTest NOT RUN | COSTUME_TESTCASES.md |
| Recce + External Users | **Done**: RECCE-02, RECCE-05, EXT-01a bugs; plus "ADD EXTERNAL USER" title bug | RECCE_TESTCASES.md, EXTERNAL_USERS_TESTCASES.md |
| Map | **Mostly done**: bugs H-06, Z-08b, Z-08d, Z-11c; T-12 intermittent. Unfinished: test fixes for M-10, H-09, N-15b, N-19, P-14/17, H-05, H-01, H-08, N-07, N-11c; RT-02/03/06/07 two-device NOT RUN | MAP_MODULE_TESTCASES.md |
| Drive | **In progress** when stopped: ~80 new cases; bugs BUG-1 (office edits not saved), BUG-2, DRIVE-03, BUG-5, BUG-6?; reruns + two-device suites pending | DRIVE_MODULE_TESTCASES.md |
| Catering + Settings | **In progress** when stopped: the 4 earlier Pixel failures pass on an emulator; Settings cases partly run | CATERING_TESTCASES.md, SETTINGS_WEB_GAP_TESTCASES.md |
| Location tool | **In progress** when stopped: LocationToolFullTest / LocationToolTwoUserTest written; results partial | LOCATION_TOOL_TESTCASES.md |
| Small modules (Notifications, Tools page/admin, Schedule Full, Box) | **In progress** when stopped: NotificationsTest (TestOrder.NOTIFICATIONS=190), TOOLS-01/02, SCH/BOX cases; some runs pending | NOTIFICATIONS_…, TOOLS_PAGE_…, SCHEDULE_DIST_…, BOX_… |
| Supporting Artistes / Extras | Parked: needs artiste + department setup by the user (linked session `sa-extras`) | worktree, see §6 |
| Costumes & Set Sync | Parked in linked session `costumes-sync`: was waiting for the user's approval to create a production from the Sides script | worktree, see §6 |
| Not started (20 tools) | Account Hub family (11), AD Dashboard, Create Shooting Order, Transportation (164 web tests), Script & Pages, Weather, Rights Grid, Tasks | REMAINING_MODULES_AND_WEB_GAPS.md |

## 5. Pending runs (code is written and compiles)
**Two-device (Pixel + emulator):**
- TwoUserChatTest: `otherMembersMessageOffersNoEdit`, `unreadBadgeAndNewMessagesDivider`, `liveEditKeepsReplies`, `readByUpdatesLive`, `searchFindsCommentSender`, `forwardForwardedCopyToMember`, `otherUserMessageInteractions`
- `InfoTwoUserTest`, `CastingTwoUserTest`, `CostumeTwoUserTest`
- `CcGroupChatTest.groupMessageArrivesLive`
- `MapWebGapTest.rt*`
- `LocationToolTwoUserTest`
- Drive: `DriveTwoDeviceE2ETest`, `DriveSharedWithMeTest`, `DriveFolderShareTest`, `DriveCollabEditTest`
- `ScheduleDistTwoUserTest`, `BoxHistoryTwoUserTest`

Command for the Home cases and the five suites after them (the stopped `coord-2dev.sh` batch):
`handoff/scripts/coord-2dev.sh TwoUserChatTest.<each> InfoTwoUserTest CastingTwoUserTest CostumeTwoUserTest CcGroupChatTest.groupMessageArrivesLive 'MapWebGapTest.rt*'`.
The rest (Location, Drive, Schedule Full, Box) need the same Pixel + emulator setup.

**Single-device reruns:**
- Map test-side fixes (see §4).
- Info `voiceNotePostsAndMenu`.
- Drive (View, WriteProbe, Upload, Organise).
- Catering + Settings, Location full, Notifications/Tools/Schedule/Box.
- Costume COS-41/42/99 (cut off when emulator-5556 crashed).
- HomeChatTest "each media type opens" (possibly a side effect of the BUL-15 permission test).

## 6. Linked Claude sessions (account-local: another account can't open them, but their worktrees are on disk)

| Name | Session id | Worktree (branch) | State |
|---|---|---|---|
| e-signature | local_33b57462-2070-4b87-ac7e-6d948bc8eed9 | `.claude/worktrees/lucid-liskov-74e829` (`claude/lucid-liskov-74e829`) | done; files copied into main |
| schedule-dod | local_395242e7-a975-4ea2-869c-aaadfb6dff88 | `.claude/worktrees/optimistic-bouman-8f71d5` | done; copied |
| crew-dood | local_867995f0-37f4-406d-a42b-89e612294f73 | `.claude/worktrees/magical-davinci-1569db` | done; copied |
| costumes-sync | local_28039d90-3d31-47ad-ba58-cb7ebe8631c3 | `.claude/worktrees/affectionate-panini-e9a163` | parked; its files are NOT in main yet (copy them in + wire `--costumes` when finished) |
| sa-extras | local_f8267258-3612-4f49-bcdf-6bd6e1092ec7 | `.claude/worktrees/admiring-panini-5d0e2d` | parked; NOT in main yet |
| (this session) | local_43b380ae-985d-4659-a61e-6ec09577973c (child b1614600…) | main repo | coordinator; stopped |

To continue a parked one from another account: open a session in that worktree folder and read its `*_TESTCASES.md`.

## 7. Wiring (done 2026-11-24, dry-run only, no device run yet)
- Flags + Studio entries: `--casting`, `--costume`, `--location-full`, `--notifications`, `--two-device-all`, `--box-cleanup`. The web-gap classes were added to `--home`, `--chat`, `--calendar`, `--settings`, `--map-all`, `--doc-dist-all`. Document Distribution and Drive are now in the full sweep.
- `--all` ("ALL modules (create project first)"): `CreateAndSelectProjectTest` (manual email OTP) → writes `build/run-project.txt` → every later module runs with `ZILLIT_RUN_PROJECT`; the sweep stops if that fails. `--all-in-project` = "ALL modules (current project)".
- `ProjectListPage.runProject()` is the single place that ties suites to the run project.
- Still not wired: Supporting Artistes and Costumes & Set Sync (their code is only in the worktrees, §6).
- `MODULE_AUTOMATION_LIST.md` still needs Casting, Costume and Location.
- Known shared-code issues: `BaseTest.relaunchApp` gives `terminateApp` only 500 ms; `MediaPreviewPage.setCaption` hides the keyboard with BACK and loses the caption; older Recce cases `createAndPublish`, `publishAnExistingDraft` and `multipleStopsPersistToDetail` need `RecceFormPage.addFirstCrewMember()`.
- `DeleteProjectTest` (Admin Settings) leaves a real 12-hour deletion armed on whichever project the run uses.

## 8. Leftover test data to clean
- emulator-5560 Bulletin: a link row `ZL_AUTO-m21-gal-…`, the M29 video, old `qa-*` rows.
- C&C group `ZL AUTO ren 636025` (only Vidya Pixel is in it).
- Calendar: one repeating series on the Pixel, "QA Recur", past ZL_AUTO events, old drafts, "ZL_AUTO self-unread…" emails.
- Drive Trash: folder-upload files, `zl_drive_doc.pdf`.
- Recce: possibly `QA Recce Lunch 20404`.
- Costume: temporary "Zl_auto_cos …" characters stay in the character picker (no way to delete: report to product).

## 9. App bugs found (for the dev team)
- **Home:**
  - reply quote survives a unit switch
  - double-tap Send starts a voice note
  - Call Sheet offered as a forward target
  - CSV not pickable
  - PDF with JavaScript accepted
  - file size shown without a unit
  - media permission denied → Document does nothing
  - offline delete refused
  - OOM crash typing
- **Info/CI:** History has no search; draft lost; size without a unit; JavaScript PDF accepted; CSV greyed out.
- **Calendar:** #42 duration, #31 time mismatch, #29 overnight, #50 past reminder, #20 past date, #39 Deselect All, #135 stale sheet, #58 intermittent time shift.
- **Email:** EMAIL-02 self-mail sometimes arrives already read.
- **Map:** H-06 description can't be cleared; Z-08b; Z-08d; Z-11c; T-12 intermittent.
- **Drive:** BUG-1 office edits never saved; BUG-2; DRIVE-03; BUG-5; editor takes 100–250 s to load.
- **C&C:** CNC-03 name silently cut to 40 characters; C&C documents open in Google Docs.
- **Casting:** gender never shown; Reset changes Group By; Non-Binary filter.
- **Costume:** Non-Binary filter; move form title.
- **Recce / External Users:** RECCE-02, RECCE-05, EXT-01a, "ADD EXTERNAL USER" title on update.
- **Call Sheet / Production Report:** CS-06/08/10, DAY TYPE box, PR-11/15/16, reject returns 404.

Details and source lines are in each module's doc.
