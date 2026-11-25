#!/usr/bin/env bash
# Zillit Automation — one command: run every suite, then push the report
# to the team Google Sheet. Works standalone; no Claude involved.
#
#   ./run-and-report.sh --all                   # full suite (E2E asks for manual OTP)
#   ./run-and-report.sh --no-e2e           # skip the OTP/E2E test
#   ./run-and-report.sh --all-in-project   # Home+Calendar+Email+C&C+Tools+Admin Settings, one report
#   ./run-and-report.sh --admin-settings   # Settings → Admin Settings only (e.g. Department Listing Order)
#   ./run-and-report.sh --settings         # Settings tab (non-admin), e.g. Edit My Profile
#   ./run-and-report.sh --two-device       # cross-device send/receive (two users)
#
# Prerequisites (once): emulator running, Appium server running
#   (ANDROID_HOME must be exported, then: npm run appium),
#   develop-debug APK built in ../Zillit-Android.
#
# --two-device also needs a SECOND device attached and BOTH devices already
# logged into the SAME project as two different users. Set:
#   ZILLIT_SENDER_UDID=<devA>  ZILLIT_RECEIVER_UDID=<devB>  ./run-and-report.sh --two-device

set -u
cd "$(dirname "$0")"

# Git Bash / MSYS rewrites any argument that looks like a Unix path into a
# Windows one, so `adb push … /sdcard/Pictures/x.jpg` silently became
# `C:/Program Files/Git/sdcard/…` and every fixture push failed — invisibly,
# because prep_device discards output. These two vars turn that off and are
# simply ignored by bash on macOS/Linux.
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL='*'

GRADLE_ARGS=(test --continue)
if [[ "${1:-}" == "--no-e2e" ]]; then
  GRADLE_ARGS+=(--tests '*.JoinProjectTest'
                --tests '*.CreateProjectValidationTest'
                --tests '*.CreateProjectSubTypeTest'
                --tests '*.ProjectListTest')
elif [[ "${1:-}" == "--flow" ]]; then
  # Just the ordered happy-path journey: create -> join -> select project.
  GRADLE_ARGS+=(--tests '*.ProjectFlowE2ETest')
elif [[ "${1:-}" == "--sweep" ]]; then
  # Clean unit-by-unit flow: finish each unit fully (send every type + check
  # long-press on each) before the next — Bulletin → Call Sheet → … → Calendar.
  GRADLE_ARGS+=(--tests '*.AllUnitsMediaTest'
                --tests '*.CalendarTest')
elif [[ "${1:-}" == "--home" ]]; then
  # In-project suites (need a logged-in project): Home chat, media send/view/
  # reply, call-sheet, calendar.
  GRADLE_ARGS+=(--tests '*.HomeChatTest'
                --tests '*.PostingRightsTest'
                --tests '*.MediaPreviewTest'
                --tests '*.MediaSystemPickerTest'
                --tests '*.ChatOptionsTest'
                --tests '*.CallSheetTest'
                --tests '*.RegressionStressTest'
                --tests '*.AllUnitsMediaTest'
                --tests '*.HomeWebGapTest'
                --tests '*.CalendarTest'
                --tests '*.CalendarRecurringScopeTest'
                --tests '*.CalendarWebGapTest')
elif [[ "${1:-}" == "--call-sheet-upload" ]]; then
  # ZL-21154 upload-mode sheet (Continuation / New / Replace) on the Call Sheet unit.
  # Its own flag rather than part of --home: every case drives a full picker round-trip to
  # reach the sheet, so folding 17 of them into an already hour-long --home would roughly
  # double it for one feature.
  # Add -Dzillit.callSheetDestructive=true to also run the "New wipes the unit" case.
  GRADLE_ARGS+=(--tests '*.CallSheetUploadModeTest')
elif [[ "${1:-}" == "--calendar" ]]; then
  # CALENDAR ONLY — its own fresh session, nothing to do with chat. Full field
  # coverage, all pickers, and CRUD (create / view / update / delete + recurring).
  GRADLE_ARGS+=(--tests '*.CalendarTest'
                --tests '*.CalendarRecurringScopeTest'
                --tests '*.CalendarWebGapTest')
elif [[ "${1:-}" == "--chat" ]]; then
  # CHAT ONLY — every chat suite, no calendar. Sending, options, media, stress.
  GRADLE_ARGS+=(--tests '*.HomeChatTest'
                --tests '*.PostingRightsTest'
                --tests '*.MediaPreviewTest'
                --tests '*.MediaSystemPickerTest'
                --tests '*.ChatOptionsTest'
                --tests '*.CallSheetTest'
                --tests '*.RegressionStressTest'
                --tests '*.AllUnitsMediaTest'
                --tests '*.HomeWebGapTest')
elif [[ "${1:-}" == "--email" ]]; then
  # EMAIL ONLY — its own fresh session. Landing/list, folder drawer, compose
  # (To/Cc/Bcc + subject + body + rich-text + attach + send) with validation,
  # open/read, reply/forward, delete, search, groups CRUD, settings.
  GRADLE_ARGS+=(--tests '*.EmailTest'
                --tests '*.EmailDrawerTest'
                --tests '*.EmailExtrasTest'
                --tests '*.EmailActionsTest'
                --tests '*.EmailCalendarTest')
elif [[ "${1:-}" == "--cc" ]]; then
  # C&C (Chat & Calling) ONLY — landing, group create/manage, chat thread, calling.
  GRADLE_ARGS+=(--tests '*.CcGroupCreateTest'
                --tests '*.CcGroupChatTest'
                --tests '*.CcGroupManageTest'
                --tests '*.CcDirectChatTest'
                --tests '*.CcChatMediaTest'
                --tests '*.CcCallTest'
                --tests '*.CcCallTabTest'
                --tests '*.CcContactsTest')
elif [[ "${1:-}" == "--tools" ]]; then
  # TOOLS page — the two Bulletin-style chat modules (Confidential Info + Info)
  # PLUS the Tools page management (search, change group order, enable/disable).
  GRADLE_ARGS+=(--tests '*.ToolsManagementTest'
                --tests '*.ConfidentialInfoTest'
                --tests '*.InfoTest'
                --tests '*.CateringTest'
                --tests '*.ExternalUserTest'
                --tests '*.RecceTest')
elif [[ "${1:-}" == "--external-users" ]]; then
  # EXTERNAL USERS — the listing and its filters, the three user types with their
  # show/hide rules, every field's validation, the read-only detail screen, and the taps
  # that hand over to the Email module and to the dialer.
  GRADLE_ARGS+=(--tests '*.ExternalUserTest')
elif [[ "${1:-}" == "--sides" ]]; then
  # SIDES — the day's pages cut out of the script. Covers both tabs, the two gates that
  # guard Generate and Autogenerate, a script with and without a file, the scene pickers
  # read out of the uploaded PDF, the manual generate flow through review and publish,
  # the row's four actions, History, and the Autogenerate sheet.
  #
  # It needs the two fixture PDFs on the device; the prep step above pushes them to
  # /sdcard/Documents. The script one carries five numbered scene headings on purpose —
  # the backend derives the scene list from the PDF's text, so a blank stub would leave
  # every picker empty.
  GRADLE_ARGS+=(--tests '*.SidesTest')
elif [[ "${1:-}" == "--continuity" ]]; then
  # CONTINUITY — the department's photo record of each scene. Covers both tabs, adding a
  # record through the app's own gallery, the scene grid and its card menu, edit, forward
  # to All Department, distribute (stops at the confirm), download, delete on each level,
  # the All Department department list, and a final sweep of every scene marked ZLAUTO.
  #
  # Records are made from the gallery's first photos — the zl_test_*.jpg the prep step
  # pushes to /sdcard/Pictures and touches to the top. Adding is asynchronous (upload,
  # then a background create, then a socket), so the add cases wait up to 150s for a row.
  GRADLE_ARGS+=(--tests '*.ContinuityTest')
elif [[ "${1:-}" == "--call-sheet-creation" ]]; then
  # CALL SHEET CREATION — Tools → "Call Sheet Creation" (ProjectDetailActivity), NOT the
  # Home "Call Sheet" chat unit (that is --home / --call-sheet-upload). Creator side on
  # one device: tabs, template picker, the editor's native parts (header, sections,
  # approvers, overflow, name and discard dialogs), Drafts actions, Send for Comments /
  # Signature, the admin Permission tab, and a sweep of every ZL_AUTO_CS_ sheet.
  # Needs the project's approver set (Vidya Pixel on ZL-Android-Automation).
  # With ZILLIT_RECEIVER_UDID set (the approver's device, Pixel), also runs the two-device
  # approval round CallSheetApprovalTest; without it that class skips.
  GRADLE_ARGS+=(--tests '*.CallSheetCreationTest' --tests '*.CallSheetApprovalTest')
elif [[ "${1:-}" == "--production-report" ]]; then
  # PRODUCTION REPORT CREATION — Tools → "Production Report Creation"
  # (ProductionReportDashboardActivity; the tile opens the chat instead unless remote config
  # enables report templates for the project). Creator side on one device: dashboard,
  # template picker + "No Call Sheet Found", the editor (Crew Call / Unit Wrap, approvers,
  # overflow, name/discard dialogs), Drafts actions, "Missing Call Times", Send for Signature,
  # Sent / Reminder, and a sweep of every ZL_AUTO_PR_ report.
  # With ZILLIT_RECEIVER_UDID set, also the two-device approval round ProductionReportApprovalTest.
  GRADLE_ARGS+=(--tests '*.ProductionReportTest' --tests '*.ProductionReportApprovalTest')
elif [[ "${1:-}" == "--schedule-dod" ]]; then
  # SCHEDULE D.O.D. — Tools → "Schedule D.O.D." (DodActivity): folders and files, upload
  # (Document picker, name, colour), card menus, View / Download / counts, History, search,
  # QA DOD-01…05 re-checks, and a sweep of every ZL_AUTO upload. Fixture: zl_test_doc.pdf.
  GRADLE_ARGS+=(--tests '*.ScheduleDodTest')
elif [[ "${1:-}" == "--chat-doc-editor" ]]; then
  # CHAT DOCUMENT → EDITOR — opening an Office document (docx/xlsx) from each chat opens the
  # app's own editor read-only ("View only", no app Edit button) and one Back returns to the
  # chat: Bulletin, Call Sheet, Info, Confidential Info, Production Report chat, Forms
  # discussion, C&C 1:1 and group. A PDF in Bulletin is the negative control (another app).
  GRADLE_ARGS+=(--tests '*.ChatDocumentEditorTest')
elif [[ "${1:-}" == "--e-signature" ]]; then
  # Z-E-SIGNATURE — Tools → "Z-E-Signature" (DocSignMainActivity): drafts / sent / received /
  # completed lists, upload a document (zl_test_doc.pdf), envelope composer, saved signatures,
  # templates, row menus, and a sweep of every ZL_AUTO envelope.
  GRADLE_ARGS+=(--tests '*.ESignatureTest')
elif [[ "${1:-}" == "--crew-dood" ]]; then
  # CREW DOOD — Tools → "Crew DOOD" (CrewDoodActivity): crew grid and calendar, days-off
  # editor, filters, detail, export (skips without a matching deal memo), and a sweep of
  # everything ZL_AUTO_ it made.
  GRADLE_ARGS+=(--tests '*.CrewDoodTest')
elif [[ "${1:-}" == "--schedule-dist" ]]; then
  # SCHEDULE DISTRIBUTION — the Tools tile reads "Schedule Full & One Line" (server label;
  # identifier schedule_distribution_tool). Covers the three tabs, the Full and One Line
  # upload and Replace, View (hands off to an external PDF app), Download, counts, page
  # uploads and their validation, the Pages search and folders, page delete, the publish
  # confirm (stops at Cancel unless -Dzillit.schedule.publish=true), and the History screens.
  #
  # Full / One Line rows can never be deleted from the app, so the suite keeps ONE sentinel
  # row per tab (episode 9900 on a TV project) and Replaces it on every run after the first.
  # Pages are made under scene 99ZLAUTO<run> and swept at the end. Fixture: zl_test_doc.pdf,
  # pushed to /sdcard/Documents by the prep step above.
  GRADLE_ARGS+=(--tests '*.ScheduleDistTest')
elif [[ "${1:-}" == "--recce" ]]; then
  # RECCE ONLY — the location-scout module: list/search/filters, create form +
  # publish validation, drafts, stops, personnel, location picker, detail, delete.
  GRADLE_ARGS+=(--tests '*.RecceTest')
elif [[ "${1:-}" == "--only" ]]; then
  # ONE SUITE — ./run-and-report.sh --only MapCityCrudTest
  #
  # A long Appium session degrades: the UiAutomator2 instrumentation on the device
  # gets killed somewhere past the half-hour mark and everything after it fails for
  # reasons that have nothing to do with the app. Running a single suite keeps the
  # session short enough to trust what it reports, which is what you want while
  # fixing one.
  if [[ -z "${2:-}" ]]; then
    echo "--only needs a test class, e.g. --only MapCityCrudTest" >&2
    exit 2
  fi
  GRADLE_ARGS+=(--tests "*.${2}")
elif [[ "${1:-}" == "--map" ]]; then
  # MAP ONLY — Tools → LOCATION → Map (new_map): landing state, tutorial card,
  # city panel + last-visited persistence, city-gated toolbar, search, map type,
  # filter panel, carousel and the View All list.
  GRADLE_ARGS+=(--tests '*.MapModuleTest')
elif [[ "${1:-}" == "--map-pins" ]]; then
  # MAP OVERLAY ONLY — the Location tool's Shortlisted/Final shoot locations drawn
  # over the Map: toggle gating, ON-by-default, count label, persistence across an
  # app kill, the two label registers, read-only enforcement and project-wide scope.
  GRADLE_ARGS+=(--tests '*.MapLocationPinsTest')
elif [[ "${1:-}" == "--location" ]]; then
  # LOCATION TOOL ONLY — Select / Shortlist / Final tabs, the source of the Map
  # overlay. Skips wholesale when the tool is not enabled on the project.
  GRADLE_ARGS+=(--tests '*.LocationToolTest')
elif [[ "${1:-}" == "--map-crud" ]]; then
  # MAP WRITE PATHS — cities, studio zones, pin boundary verdicts, locations.
  # These CREATE and DELETE real records on the project. Every fixture is named
  # "ZL AUTO …" and each suite deletes only what it created.
  GRADLE_ARGS+=(--tests '*.MapCityCrudTest'
                --tests '*.MapZoneCrudTest'
                --tests '*.MapPinBoundaryTest'
                --tests '*.MapTypeManagerTest'
                --tests '*.MapLocationCrudTest')
elif [[ "${1:-}" == "--box" ]]; then
  # PRODUCTION DIARY / BOX SCHEDULE — one module, two names. Suites run in an order where
  # each starts from what the one before it made: a type, then a schedule day, then the
  # events and notes that hang off it.
  BOX_SUITES=(BoxShellTest BoxTypeTest BoxScheduleCrudTest BoxConflictTest
              BoxEventNoteTest BoxProductionScheduleTest BoxScheduleEditTest
              BoxCrossViewTest BoxEventLifecycleTest)
  ACC="build/allresults"; rm -rf "$ACC"; mkdir -p "$ACC"; TEST_EXIT=0
  for C in "${BOX_SUITES[@]}"; do
    echo ""; echo "════════════════════════════════════════════════════════════"
    echo "▶ $C"; echo "════════════════════════════════════════════════════════════"
    ./gradlew cleanTest test --continue --tests "*.$C" || TEST_EXIT=1
    if ls build/test-results/test/*.xml >/dev/null 2>&1; then
      for f in build/test-results/test/*.xml; do cp "$f" "$ACC/${C}__$(basename "$f")" 2>/dev/null || true; done
    fi
  done
  if ls "$ACC"/*.xml >/dev/null 2>&1; then
    rm -f build/test-results/test/*.xml 2>/dev/null || true
    cp "$ACC"/*.xml build/test-results/test/ 2>/dev/null || true
  fi
  echo ""; echo "▶ Building shareable HTML report…"; node tools/make-report.mjs || true
  exit $TEST_EXIT
elif [[ "${1:-}" == "--map-cities" ]]; then
  # A SECOND CITY, and what only two cities can prove: switching, per-city pin scoping
  # and city search. Small on purpose — these cases are worthless unless they finish.
  GRADLE_ARGS+=(--tests '*.MapCityMultiTest')
elif [[ "${1:-}" == "--map-cleanup" ]]; then
  # HOUSEKEEPING — deletes ZL AUTO locations/zones/types left on the project when a Map
  # run lost its session before its own cleanup case. Deliberately NOT in --map-all or
  # --all: it removes data, and nothing should do that unasked.
  GRADLE_ARGS+=(--tests '*.MapCleanupTest')
elif [[ "${1:-}" == "--map-crud-full" ]]; then
  # THE DATA LIFECYCLE — creates several locations by dropping pins, renames one, sets a
  # scene number, exercises the out-of-city choices, drags a pin, deletes, and cleans up
  # after itself. This is the suite that finds data bugs; the others read screens.
  GRADLE_ARGS+=(--tests '*.MapCrudLifecycleTest')
elif [[ "${1:-}" == "--map-directions" ]]; then
  # DIRECTIONS + SHARE — both hand off to other apps, so they run last.
  GRADLE_ARGS+=(--tests '*.MapDirectionsShareTest')
elif [[ "${1:-}" == "--map-location-flow" ]]; then
  # LOCATION TOOL → MAP, end to end: a ZL_AUTO_ shoot location (pin inside a Map city,
  # City = that city) is created in Selects, moved to Shortlist and Final, and read back
  # on the Map at each stage. WRITES to both modules; deletes the location and any city
  # it had to add.
  GRADLE_ARGS+=(--tests '*.MapLocationFlowTest')
elif [[ "${1:-}" == "--map-all" ]]; then
  # EVERYTHING map-related — but ONE APPIUM SESSION PER SUITE, not one session for all
  # ten. Handing every class to a single Gradle run is what this branch used to do, and
  # it is the pattern that produced a 76-minute run reporting 38 failures with no
  # assertion behind any of them: the UiAutomator2 instrumentation dies somewhere past
  # the half-hour mark, every later lookup throws, and each remaining case burns its full
  # landing budget before failing. Per-suite sessions kept the same ten suites at
  # 30 passed / 2 failed / 77 skipped with no rebuilds in nine of them (2026-11-20).
  #
  # Each suite's XML is collected and ONE HTML report is built from the lot, so this is
  # still a single command with a single shareable result.
  MAP_SUITES=(MapModuleTest MapLocationPinsTest LocationToolTest MapCityCrudTest
              MapZoneCrudTest MapPinBoundaryTest MapTypeManagerTest MapLocationCrudTest
              MapDirectionsShareTest MapGuardsTest MapCrudLifecycleTest
              MapCityMultiTest MapLocationFlowTest MapWebGapTest MapZoneGuardTest)
  ACC="build/allresults"
  rm -rf "$ACC"; mkdir -p "$ACC"
  TEST_EXIT=0
  for C in "${MAP_SUITES[@]}"; do
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "▶ $C"
    echo "════════════════════════════════════════════════════════════"
    ./gradlew cleanTest test --continue --tests "*.$C" || TEST_EXIT=1
    if ls build/test-results/test/*.xml >/dev/null 2>&1; then
      for f in build/test-results/test/*.xml; do
        cp "$f" "$ACC/${C}__$(basename "$f")" 2>/dev/null || true
      done
    fi
  done
  if ls "$ACC"/*.xml >/dev/null 2>&1; then
    rm -f build/test-results/test/*.xml 2>/dev/null || true
    cp "$ACC"/*.xml build/test-results/test/ 2>/dev/null || true
  fi
  echo ""
  echo "▶ Building shareable HTML report…"
  node tools/make-report.mjs || true
  exit $TEST_EXIT
elif [[ "${1:-}" == "--drive" ]]; then
  # DRIVE ONLY — Tools → ADMIN → Drive: home shell (tabs/chips/select/drawer),
  # create-folder validation + access control, folder navigation and cleanup.
  GRADLE_ARGS+=(--tests '*.DriveTest')
elif [[ "${1:-}" == "--drive-e2e" ]]; then
  # SYMMETRIC TWO-DEVICE JOURNEY — A uploads/shares → B receives and verifies rights →
  # B uploads INTO A's folder → B shares back → A receives → both edit → both edits
  # survive → A revokes and it disappears from B.
  #   ZILLIT_SENDER_UDID=<A> ZILLIT_RECEIVER_UDID=<B> ./run-and-report.sh --drive-e2e
  GRADLE_ARGS+=(--tests '*.DriveTwoDeviceE2ETest')
elif [[ "${1:-}" == "--drive-two-device" ]]; then
  # FULL TWO-DEVICE DRIVE SWEEP — the owner suites on device A, then the recipient
  # suites on device B, then collaborative editing across both. Each phase is its own
  # Gradle run against its own device, so a failure on one side cannot mask the other.
  #
  #   ZILLIT_SENDER_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=<serial> \
  #     ./run-and-report.sh --drive-two-device
  #
  # Both devices must be in the SAME project as DIFFERENT users, and the shared file
  # must already be granted to device B's user (the owner phase does this).
  DRIVE_TWO_DEVICE=1
elif [[ "${1:-}" == "--drive-collab" ]]; then
  # COLLABORATIVE EDITING — the SAME document open on TWO devices as two users.
  # Proves live sync and that concurrent edits to different cells both survive.
  #   ZILLIT_SENDER_UDID=<devA> ZILLIT_RECEIVER_UDID=<devB> ./run-and-report.sh --drive-collab
  GRADLE_ARGS+=(--tests '*.DriveCollabEditTest')
elif [[ "${1:-}" == "--drive-editor" ]]; then
  # DOCUMENT EDITING — the Collabora editor: the Edit gate (editable type + canEdit +
  # posting access), read-only vs edit mode, the full toolbar, and Leave confirmation.
  # Needs the editable fixtures (zl_drive_book.xlsx) pushed by prep_device.
  GRADLE_ARGS+=(--tests '*.DriveEditorTest')
elif [[ "${1:-}" == "--drive-organise" ]]; then
  # ORGANISING — move an item into a folder, bulk select + bulk delete, and the Trash
  # lifecycle. The destructive operations, each verified by where the item ENDS UP.
  GRADLE_ARGS+=(--tests '*.DriveOrganiseTest')
elif [[ "${1:-}" == "--drive-view" ]]; then
  # VIEW CONTROLS — list/grid toggle, favourites-only, sort, tag filter, kind tabs,
  # and the Search screen with its type chips.
  GRADLE_ARGS+=(--tests '*.DriveViewTest')
elif [[ "${1:-}" == "--drive-upload" ]]; then
  # DRIVE UPLOADS — file to root, file INSIDE a folder, whole-folder upload, staging
  # options, cancel, and the offline/retry path.
  GRADLE_ARGS+=(--tests '*.DriveUploadTest')
elif [[ "${1:-}" == "--drive-share" ]]; then
  # DRIVE SHARING (OWNER side) — grant a project user View/Edit/Download, share a
  # folder, and the share-by-link flow. Set ZILLIT_SHARE_USER to the other user.
  GRADLE_ARGS+=(--tests '*.DriveShareTest')
elif [[ "${1:-}" == "--drive-shared" ]]; then
  # DRIVE SHARING (RECIPIENT side) — run on the SECOND device, signed in as the user
  # --drive-share shared with. Proves the share actually arrived and that the
  # recipient's rights are enforced.
  #   ZILLIT_UDID=<second-device> ./run-and-report.sh --drive-shared
  GRADLE_ARGS+=(--tests '*.DriveSharedWithMeTest')
elif [[ "${1:-}" == "--drive-comments" ]]; then
  # COMMENTS (area P) — the comments block on a file's detail screen and the full
  # "View all N comments" screen: post, edit, delete, empty-comment guard.
  GRADLE_ARGS+=(--tests '*.DriveCommentsTest')
elif [[ "${1:-}" == "--drive-requests" ]]; then
  # FILE REQUESTS (area S) — the folder-scoped "Request Files" sheet (form, validation,
  # recipient chips, created link, copy) and "View File Requests" (list + revoke).
  GRADLE_ARGS+=(--tests '*.DriveRequestFilesTest')
elif [[ "${1:-}" == "--drive-activity" ]]; then
  # ACTIVITY LOG (area M) — drawer → Activity: the log opens, a create is recorded,
  # swipe-refresh reloads it.
  GRADLE_ARGS+=(--tests '*.DriveActivityTest')
elif [[ "${1:-}" == "--drive-settings" ]]; then
  # SETTINGS / "More" (area N) — mostly a GUARD: the screen is built but nothing
  # navigates to it in this build, so the suite asserts it is still unreachable and
  # covers Trash/Activity via the drawer entry points that do ship.
  GRADLE_ARGS+=(--tests '*.DriveSettingsTest')
elif [[ "${1:-}" == "--doc-dist" ]]; then
  # DOCUMENT DISTRIBUTION — areas A+B (entry, shell, library listing). See
  # DOC_DISTRIBUTION_TEST_CASES.md for the full 152-case map and the suite plan.
  GRADLE_ARGS+=(--tests '*.DocDistributionTest')
elif [[ "${1:-}" == "--doc-dist-folders" ]]; then
  # DOCUMENT DISTRIBUTION — area C (folders) plus the area-B cases that need content.
  GRADLE_ARGS+=(--tests '*.DocDistFoldersTest')
elif [[ "${1:-}" == "--doc-dist-files" ]]; then
  # DOCUMENT DISTRIBUTION — area D (files & selection): upload a fixture PDF into a
  # QA folder, preview it (FilePreviewActivity), row menu, selection bar, move sheet,
  # delete. Needs /sdcard/Download/zl_dd_doc.pdf (pushed by prep_device below).
  GRADLE_ARGS+=(--tests '*.DocDistFilesTest')
elif [[ "${1:-}" == "--doc-dist-composer" ]]; then
  # DOCUMENT DISTRIBUTION — areas E+F core: the composer (chips, Cc/Bcc, subject,
  # send-gating, preview round-trip) and the attach-from-library picker. Seeds its own
  # folder + upload; NEVER sends a real email.
  GRADLE_ARGS+=(--tests '*.DocDistComposerTest')
elif [[ "${1:-}" == "--doc-dist-lists" ]]; then
  # Areas G+H — distribution lists (presets) and email templates: CRUD, badges, apply.
  GRADLE_ARGS+=(--tests '*.DocDistPresetsTest'
                --tests '*.DocDistTemplatesTest')
elif [[ "${1:-}" == "--doc-dist-contacts" ]]; then
  # Area I — address book, contact detail/editor, CSV import, export.
  GRADLE_ARGS+=(--tests '*.DocDistContactsTest')
elif [[ "${1:-}" == "--doc-dist-history" ]]; then
  # Areas J+K+L — history list, distribution detail, analytics. Read-mostly, and
  # meaningful only against a project that HAS distributions (see --doc-dist-send).
  GRADLE_ARGS+=(--tests '*.DocDistHistoryTest')
elif [[ "${1:-}" == "--doc-dist-watermark" ]]; then
  # Area M — watermark wizard, batch screen, download.
  GRADLE_ARGS+=(--tests '*.DocDistWatermarkTest')
elif [[ "${1:-}" == "--doc-dist-publish" ]]; then
  # Area N — the publish sheet. Drives every control and CANCELS: a real publish
  # writes into OTHER modules and cannot be cleaned up from here.
  GRADLE_ARGS+=(--tests '*.DocDistPublishTest')
elif [[ "${1:-}" == "--doc-dist-rights" ]]; then
  # Area O — the click-time permission gate. Two cases need a SECOND account.
  GRADLE_ARGS+=(--tests '*.DocDistRightsTest')
elif [[ "${1:-}" == "--doc-dist-regression" ]]; then
  # Area P — offline, rotation, long/special names, rapid BACK. Toggles device
  # connectivity and restores it; do not run it against a device you are using.
  GRADLE_ARGS+=(--tests '*.DocDistRegressionTest')
elif [[ "${1:-}" == "--doc-dist-send" ]]; then
  # F-25 — the ONLY suite that really sends. Recipients are @example.com (RFC 2606),
  # so nothing can reach a real mailbox. Verifies the distribution in History.
  GRADLE_ARGS+=(--tests '*.DocDistSendTest')
elif [[ "${1:-}" == "--doc-dist-cleanup" ]]; then
  # HOUSEKEEPING — deletes stray QA_* folders / lists / templates left behind when a case
  # failed before its own cleanup. Opt-in only: it removes data. Touches nothing whose
  # name does not start with QA_. Run it before the empty-state cases (H-2) if a previous
  # run died mid-way.
  GRADLE_ARGS+=(--tests '*.DocDistCleanupTest')
elif [[ "${1:-}" == "--doc-dist-grid" ]]; then
  # GRID VIEW — the second layout family. A grid FILE row carries Send/Publish/Watermark/
  # Download/Delete as inline buttons and no ⋮ at all, so the list-view row-menu cases
  # cover none of it.
  GRADLE_ARGS+=(--tests '*.DocDistGridViewTest')
elif [[ "${1:-}" == "--doc-dist-publish-types" ]]; then
  # PUBLISH TYPE RULES — most destinations are PDF-ONLY (Call Sheet, Production Report,
  # Schedule*, Script, Page); Info and Confidential Info take anything. Uploads one file
  # of every supported family and checks the per-row gate. Never publishes.
  GRADLE_ARGS+=(--tests '*.DocDistPublishTypesTest')
elif [[ "${1:-}" == "--doc-dist-nested" ]]; then
  # NESTED FOLDERS — folder inside folder inside folder, the breadcrumb at depth, and
  # uploading every supported media type at the deepest level.
  GRADLE_ARGS+=(--tests '*.DocDistNestedFoldersTest')
elif [[ "${1:-}" == "--doc-dist-verify" ]]; then
  # RE-CHECK after a fix round: whichever suites still carry a failure. Much faster than
  # --doc-dist-all when you only need to know whether the last fixes took. Keep this list
  # current — it is a scratch flag, not a fixed contract.
  #
  # Round 1 (H-4/H-5, N-5/N-6, NEST-7) all passed and dropped off. Still open: presets
  # (G-7's keyboard-tap fix, G-10 downstream of it), publish-types (the sheet was only
  # getting 3 of 5 files) and rights (O-2 now seeds its own document instead of skipping).
  GRADLE_ARGS+=(--tests '*.DocDistPresetsTest'
                --tests '*.DocDistRightsTest'
                --tests '*.DocDistPublishTypesTest')
elif [[ "${1:-}" == "--doc-dist-all" ]]; then
  # Everything EXCEPT the real send and the device-state regression suite — those two
  # are opt-in on purpose (--doc-dist-send, --doc-dist-regression).
  GRADLE_ARGS+=(--tests '*.DocDistributionTest'
                --tests '*.DocDistFoldersTest'
                --tests '*.DocDistFilesTest'
                --tests '*.DocDistComposerTest'
                --tests '*.DocDistPresetsTest'
                --tests '*.DocDistTemplatesTest'
                --tests '*.DocDistContactsTest'
                --tests '*.DocDistHistoryTest'
                --tests '*.DocDistWatermarkTest'
                --tests '*.DocDistPublishTest'
                --tests '*.DocDistGridViewTest'
                --tests '*.DocDistPublishTypesTest'
                --tests '*.DocDistNestedFoldersTest'
                --tests '*.DocDistRightsTest'
                --tests '*.DocDistWebGapTest')
elif [[ "${1:-}" == "--doc-dist-everything" ]]; then
  # Literally every Document Distribution suite, including the real send and the
  # regression suite that toggles the network.
  GRADLE_ARGS+=(--tests '*.DocDist*')
elif [[ "${1:-}" == "--drive-editor-probe" ]]; then
  # EXPERIMENT HARNESS (plan §2.2), not coverage — writes a marker to a cell with each
  # candidate strategy and reports which ones survive a write → move away → return read.
  # Needs Appium with --relaxed-security.
  GRADLE_ARGS+=(--tests '*.DriveEditorWriteProbeTest')
elif [[ "${1:-}" == "--drive-folder-share" ]]; then
  # FOLDER SHARING, isolated (plan §2.1) — splits "A shares a folder with B" into its two
  # links: did the grant SAVE (re-read on the owner's own Share screen), and did it REACH
  # the recipient. Reports what the recipient can see when it does not arrive.
  #   ZILLIT_SENDER_UDID=<A> ZILLIT_RECEIVER_UDID=<B> ./run-and-report.sh --drive-folder-share
  GRADLE_ARGS+=(--tests '*.DriveFolderShareTest')
elif [[ "${1:-}" == "--drive-all" ]]; then
  # Every OWNER-side Drive suite in order (recipient side needs the 2nd device).
  GRADLE_ARGS+=(--tests '*.DriveTest'
                --tests '*.DriveUploadTest'
                --tests '*.DriveShareTest'
                --tests '*.DriveEditorTest'
                --tests '*.DriveViewTest'
                --tests '*.DriveOrganiseTest'
                --tests '*.DriveCommentsTest'
                --tests '*.DriveRequestFilesTest'
                --tests '*.DriveActivityTest'
                --tests '*.DriveSettingsTest')
elif [[ "${1:-}" == "--recce-pagination" ]]; then
  # PAGINATION stress: seed N recces (ZILLIT_RECCE_COUNT, default 120) then verify
  # every one is reachable by scrolling. Black-box — needs only the APK.
  #   ZILLIT_RECCE_COUNT=300 ./run-and-report.sh --recce-pagination
  #   ZILLIT_RECCE_SEED=false ./run-and-report.sh --recce-pagination   # verify only
  GRADLE_ARGS+=(--tests '*.RecceePaginationTest')
elif [[ "${1:-}" == "--admin-settings" ]]; then
  # SETTINGS → ADMIN SETTINGS — every admin-settings sub-feature test class, in the
  # agreed running order (that order lives in TestOrder, 100–149).
  # Add new admin-settings classes here AND give them a TestOrder constant.
  GRADLE_ARGS+=(--tests '*.DepartmentListingOrderTest'
                --tests '*.AdminSettingsGroupsTest'
                --tests '*.CompanyDetailsTest'
                --tests '*.CreateAdditionalShootingUnitTest'
                --tests '*.CreateNewDepartmentTest'
                --tests '*.CreateNewDesignationTest'
                --tests '*.CreateRemoteShootingUnitTest'
                --tests '*.CreateUpdateHomeUnitTest'
                --tests '*.CustomizationOfToolsTest'
                --tests '*.EditProjectNameTest'
                --tests '*.ManageToolGroupsTest'
                --tests '*.PreApprovedUsersTest'
                --tests '*.ApproveNewUserRequestTest'
                --tests '*.SosReceiversTest'
                --tests '*.UserManagementTest'
                --tests '*.WatermarkLogoTest'
                --tests '*.DeleteProjectTest')
elif [[ "${1:-}" == "--settings" ]]; then
  # SETTINGS TAB (non-admin) — separate from Admin Settings. Run this after
  # --admin-settings. TestOrder range 150–169.
  GRADLE_ARGS+=(--tests '*.EditMyProfileTest'
                --tests '*.AppPreferencesTest'
                --tests '*.LinkedDeviceTest'
                --tests '*.InviteUsersTest'
                --tests '*.ZillitHelpTest'
                --tests '*.PermissionGridTest'
                --tests '*.AdminSettingsWebGapTest')
elif [[ "${1:-}" == "--manual-media" ]]; then
  # Interactive: YOU pick Video/Location/Contact on the device and reach the
  # send screen; automation sends + verifies. Set ZILLIT_PICK_WAIT for a longer
  # picking window (default 120s).
  GRADLE_ARGS+=(--tests '*.ManualPickSendTest')
elif [[ "${1:-}" == "--two-device" ]]; then
  # Cross-device, two-user: every message type is RECEIVED on the 2nd device,
  # edits update on the receiver, deletes disappear on the receiver.
  GRADLE_ARGS+=(--tests '*.TwoUserChatTest')
elif [[ "${1:-}" == "--casting" ]]; then
  # CASTING — "Casting (Main Cast)" and "Casting(Background Cast)": tabs, filters, the form's
  # validation, create/edit, moves Selects → Shortlist → Final, report, delete. ~50 min.
  GRADLE_ARGS+=(--tests '*.CastingTest')
elif [[ "${1:-}" == "--costume" ]]; then
  # COSTUME — "Costume (Main Cast)" and "Costume (Background Cast)" (the Wardrobe tools, not
  # Costumes & Set Sync). ~70 min for both; COSTUME_TOOL=MAIN|BACKGROUND runs one.
  GRADLE_ARGS+=(--tests '*.CostumeTest')
elif [[ "${1:-}" == "--location-full" ]]; then
  # LOCATION TOOL, everything on one device: the read-only smoke plus the full write suite.
  GRADLE_ARGS+=(--tests '*.LocationToolTest'
                --tests '*.LocationToolFullTest')
elif [[ "${1:-}" == "--notifications" ]]; then
  # NOTIFICATIONS — the list on first open and where its rows lead (NOTIF-01/02).
  GRADLE_ARGS+=(--tests '*.NotificationsTest')
elif [[ "${1:-}" == "--box-cleanup" ]]; then
  # Delete this project's leftover Box Schedule fixtures (opt-in: it removes data).
  GRADLE_ARGS+=(--tests '*.BoxCleanupTest')
elif [[ "${1:-}" == "--two-device-all" ]]; then
  # EVERY TWO-DEVICE SUITE, one Gradle run each, one combined report. Two devices, two
  # different users, the same project:
  #   ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=<phone> ./run-ui.sh --two-device-all
  if [[ -z "${ZILLIT_UDID:-}" || -z "${ZILLIT_RECEIVER_UDID:-}" || "$ZILLIT_UDID" == "${ZILLIT_RECEIVER_UDID:-}" ]]; then
    echo "✗ --two-device-all needs two different devices: ZILLIT_UDID (creator) and ZILLIT_RECEIVER_UDID (second user)."
    exit 2
  fi
  export ZILLIT_SENDER_UDID="${ZILLIT_SENDER_UDID:-$ZILLIT_UDID}"
  RUN_ALL=1
  TWO_DEVICE_ALL=1
elif [[ "${1:-}" == "--all" ]]; then
  # FULL BUILD SWEEP — every module, one after another (Home/Bulletin → Calendar →
  # Email → C&C → Tools), each run SEPARATELY so a degraded emulator on one module
  # can't kill the rest, then ONE combined HTML report for the developer.
  # It starts by creating a project and selecting it (you enter the email OTP); every later
  # module then runs in THAT project. If it cannot be created the sweep stops. The logged-out
  # suites (create-form validation, join) clear the app's data, so they stay in --no-e2e.
  RUN_ALL=1
  CREATE_PROJECT_FIRST=1
  rm -f build/run-project.txt
  unset ZILLIT_RUN_PROJECT
elif [[ "${1:-}" == "--all-in-project" ]]; then
  # Same sweep as --all but SKIPS "Create and select project" (needs a manual
  # OTP, so it can't run unattended) — Home/Bulletin, Calendar, Email, C&C, Tools,
  # each isolated, one combined report. Needs an already logged-in project.
  RUN_ALL=1
  SKIP_PROJECT_MODULE=1
elif [[ "${1:-}" == -* ]]; then
  # An unrecognised flag used to fall through to "no --tests filter", which runs EVERY
  # suite in the project — a single typo ('--drive-editor-probe' before it existed)
  # started a multi-hour sweep of an unrelated module. Fail loudly instead.
  echo "✗ Unknown option: $1"
  echo "  Available: --no-e2e --flow --sweep --home --call-sheet-upload --calendar --chat"
  echo "             --email --cc --tools"
  echo "             --recce --recce-pagination --drive --drive-upload --drive-share"
  echo "             --map --map-pins --location --map-crud --map-directions --map-location-flow --map-all"
  echo "             --drive-shared --drive-editor --drive-editor-probe --drive-view"
  echo "             --drive-organise --drive-comments --drive-requests --drive-activity"
  echo "             --drive-settings --drive-folder-share --drive-collab --drive-e2e
             --doc-dist --doc-dist-folders --doc-dist-files --doc-dist-composer
             --doc-dist-lists --doc-dist-contacts --doc-dist-history --doc-dist-watermark
             --doc-dist-publish --doc-dist-rights --doc-dist-regression --doc-dist-send
             --doc-dist-grid --doc-dist-nested --doc-dist-publish-types --doc-dist-verify --doc-dist-all --doc-dist-everything --doc-dist-cleanup"
  echo "             --drive-two-device --drive-all --admin-settings --manual-media"
  echo "             --sides --continuity --schedule-dist --call-sheet-creation --production-report --schedule-dod --crew-dood --e-signature --chat-doc-editor --two-device --two-device-all --all --all-in-project"
  echo "             --casting --costume --location-full --notifications --box --box-cleanup --external-users --settings --map-cities --map-cleanup --map-crud-full --only <Class>"
  echo "  (no option at all = the full suite, including the manual-OTP E2E)"
  exit 2
fi

# Push test media + pre-grant permissions to the target device(s) so media-send
# tests have content and don't stall on system permission dialogs.
# adb location — resolved, not hardcoded. The old macOS-only path meant that on
# any other machine BOTH prep_device and relaunch_app silently did nothing (the
# -x guard below just skipped them), so media tests failed for no visible reason.
# Order: ANDROID_HOME / ANDROID_SDK_ROOT (set on every dev machine) → the macOS
# default → whatever is on PATH. Windows reports ANDROID_HOME with backslashes
# and needs adb.exe, so normalise the separators and try both names.
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ANDROID_SDK="${ANDROID_SDK//\\//}"
ADB="$ANDROID_SDK/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$ANDROID_SDK/platform-tools/adb.exe"
[[ -x "$ADB" ]] || ADB="$(command -v adb 2>/dev/null || true)"
[[ -x "$ADB" ]] || echo "⚠ adb not found (looked in $ANDROID_SDK/platform-tools and on PATH)" \
                        "— device prep and app relaunch will be skipped."
FX=src/test/resources/fixtures
# DEV BUILD ONLY. This used to default to .qa while config/config.properties said .dev, so
# a run without ZILLIT_FLAVOR drove the tests on dev but granted permissions to, and opened,
# the QA app before every module. Anything but dev is refused rather than half-honoured.
DEV_PKG="com.zillit.zillitapp.dev"
case "$(printf '%s' "${ZILLIT_FLAVOR:-dev}" | tr '[:upper:]' '[:lower:]')" in
  dev|develop) ;;
  *) echo "✗ ZILLIT_FLAVOR=$ZILLIT_FLAVOR — this project automates the DEV build only ($DEV_PKG)."; exit 2 ;;
esac
PKG="${ZILLIT_APP_PACKAGE:-$DEV_PKG}"
if [[ "$PKG" != "$DEV_PKG" ]]; then
  echo "✗ ZILLIT_APP_PACKAGE=$PKG — this project automates the DEV build only ($DEV_PKG)."
  exit 2
fi
export ZILLIT_APP_PACKAGE="$PKG"

prep_device() {  # $1 = adb device serial (may be empty for the only device)
  local flag=""
  [[ -n "$1" ]] && flag="-s $1"
  echo "▶ Prepping device ${1:-<default>}…"
  # CLEAR A STALE UiAutomator2 SERVER before the first session is created.
  #
  # A run that ended badly can leave io.appium.uiautomator2.server holding UiAutomation.
  # The next run then dies at session creation with "The instrumentation process cannot
  # be initialized" — the whole class reports a single initializationError and nothing
  # runs. BaseTest already recovers from this MID-run by force-stopping both packages,
  # but nothing cleared it BEFORE the first session, so a poisoned device stayed poisoned.
  # One adb server, started once, before anything else shells out to it. Two concurrent
  # `adb` calls on a stopped server race to start one and both lose:
  #   "could not install *smartsocket* listener: Address already in use"
  # after which the device is unreachable for the rest of the run. Seen in a real --map-all.
  $ADB start-server >/dev/null 2>&1

  # SILENCE THE SYSTEM "isn't responding" DIALOGS.
  #
  # An emulator under load lets SystemUI ANR, and Android draws "System UI isn't
  # responding / Close app / Wait" OVER everything. It is a system window, so the app's
  # own views drop out of the accessibility tree entirely: every lookup returns zero and
  # the suite reports "No known landing screen appeared within 120s" while the app is
  # sitting right there, perfectly healthy, behind the dialog. Measured here — it cost a
  # cold boot and six runs before a screenshot showed what the tree could not.
  #
  # This is the standard setting for an automation device and nothing else depends on it;
  # a real user's phone is untouched because this only runs against the device under test.
  $ADB $flag shell settings put global hide_error_dialogs 1 >/dev/null 2>&1
  $ADB $flag shell am force-stop io.appium.uiautomator2.server      >/dev/null 2>&1
  $ADB $flag shell am force-stop io.appium.uiautomator2.server.test >/dev/null 2>&1
  # WAKE AND UNLOCK, for a physical device.
  #
  # An emulator sits awake forever; a phone dozes and locks itself between runs. Appium
  # then activates the app behind the lock screen and every lookup finds nothing, which
  # surfaces as "No known landing screen appeared" after the full 120s — a device problem
  # wearing a test failure's clothes.
  #
  # Waking and swiping the lock screen away is enough for a device with no PIN. A device
  # WITH one stops at the keypad, and this says so plainly rather than typing anything: a
  # PIN belongs to its owner, not to a test script or a repository.
  if [[ "$(printf '%s' "${ZILLIT_UDID:-}" | cut -c1-8)" != "emulator" ]]; then
    $ADB $flag shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
    sleep 1
    $ADB $flag shell input keyevent KEYCODE_MENU   >/dev/null 2>&1
    sleep 1
    $ADB $flag shell input swipe 540 1800 540 600 200 >/dev/null 2>&1
    sleep 1
    if $ADB $flag shell dumpsys window 2>/dev/null | grep -q 'mDreamingLockscreen=true'; then
      echo "  ⚠ $ZILLIT_UDID is still locked — unlock it on the device and run again."
      echo "    (the run will keep going, but every case will fail to find a screen)"
    else
      echo "  device is awake and unlocked"
    fi
  fi

  for i in 1 2 3; do
    $ADB $flag push "$FX/zl_test_image.jpg" /sdcard/Pictures/zl_test_$i.jpg >/dev/null 2>&1
  done
  # Sides needs a script PDF with real scene headings: the backend derives the scene list
  # from the text, so the generic 426-byte stub yields zero scenes and every picker comes
  # back empty. These two carry five numbered scenes and a call sheet naming three of them.
  for f in zl_sides_script.pdf zl_sides_callsheet.pdf zl_sides_script.fdx; do
    if [[ -f "$FX/$f" ]]; then
      $ADB $flag push "$FX/$f" /sdcard/Documents/$f >/dev/null 2>&1
      $ADB $flag shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
        -d file:///sdcard/Documents/$f >/dev/null 2>&1
    fi
  done
  [[ -f "$FX/zl_test_doc.pdf" ]] && $ADB $flag push "$FX/zl_test_doc.pdf" /sdcard/Documents/zl_test_doc.pdf >/dev/null 2>&1
  [[ -f "$FX/zl_test_audio.wav" ]] && $ADB $flag push "$FX/zl_test_audio.wav" /sdcard/Music/zl_test_audio.wav >/dev/null 2>&1
  # Drive upload fixtures: a single file, plus a TREE for the whole-folder upload.
  [[ -f "$FX/zl_test_doc.pdf" ]] && $ADB $flag push "$FX/zl_test_doc.pdf" /sdcard/Download/zl_drive_doc.pdf >/dev/null 2>&1
  # Document Distribution upload fixtures — ONE PER SUPPORTED FAMILY, because the
  # module's allow-list (FileTypeDetector) is PDF + images + Word + Excel + vCard, and
  # only PDF/JPEG/PNG/WEBP are watermarkable. zl_dd_bad.txt is deliberately NOT allowed
  # (txt is absent from ALLOWED_EXT) so the rejection path can be tested too.
  [[ -f "$FX/zl_test_doc.pdf" ]]    && $ADB $flag push "$FX/zl_test_doc.pdf"   /sdcard/Download/zl_dd_doc.pdf   >/dev/null 2>&1
  [[ -f "$FX/zl_test_image.jpg" ]]  && $ADB $flag push "$FX/zl_test_image.jpg" /sdcard/Download/zl_dd_pic.jpg   >/dev/null 2>&1
  [[ -f "$FX/zl_drive_doc.docx" ]]  && $ADB $flag push "$FX/zl_drive_doc.docx" /sdcard/Download/zl_dd_word.docx >/dev/null 2>&1
  [[ -f "$FX/zl_drive_book.xlsx" ]] && $ADB $flag push "$FX/zl_drive_book.xlsx" /sdcard/Download/zl_dd_book.xlsx >/dev/null 2>&1
  [[ -f "$FX/zl_dd_card.vcf" ]]     && $ADB $flag push "$FX/zl_dd_card.vcf"    /sdcard/Download/zl_dd_card.vcf  >/dev/null 2>&1
  [[ -f "$FX/zl_drive_note.txt" ]]  && $ADB $flag push "$FX/zl_drive_note.txt" /sdcard/Download/zl_dd_bad.txt   >/dev/null 2>&1
  # Make the DD fixtures the NEWEST files in Downloads. adb push preserves each file's
  # original mtime, and the system picker sorts by modified-date — so a fixture pushed
  # today can carry a date from weeks ago, sort to the bottom of a long list, and never
  # appear under "Recent" at all. Touching them puts them on the first screenful, which
  # is the difference between the upload cases running and silently failing to find
  # their own fixture.
  $ADB $flag shell 'touch /sdcard/Download/zl_dd_*' >/dev/null 2>&1
  # Same treatment for everything in Documents (zl_test_*, and the Sides/Schedule
  # zl_sides_* scripts) plus the zl_test_* media, for the same reason. These are what the
  # chat/media suites ask the SYSTEM picker for ("zl_test"), and zl_test_doc.pdf lives in
  # Documents, not Downloads — so the picker opens on Recent without it in sight and the
  # suite has to fall back on search. adb push keeps each file's ORIGINAL mtime, so a
  # fixture copied today can carry a date from weeks back, sort below everything, and miss
  # Recent entirely. Touching them puts them on the first screenful, which is the
  # difference between the picker finding the file directly and depending on search.
  $ADB $flag shell 'touch /sdcard/Documents/zl_* /sdcard/Pictures/zl_test_* /sdcard/Music/zl_test_*' >/dev/null 2>&1
  [[ -f "$FX/zl_test_image.jpg" ]] && $ADB $flag push "$FX/zl_test_image.jpg" /sdcard/Download/zl_drive_pic.jpg >/dev/null 2>&1
  # EDITABLE office fixtures — the Drive editor only appears for docx/xlsx/pptx/csv/txt,
  # so a PDF or JPG can never exercise it.
  for ed in zl_drive_book.xlsx zl_drive_doc.docx zl_drive_sheet.csv zl_drive_note.txt; do
    [[ -f "$FX/$ed" ]] && $ADB $flag push "$FX/$ed" "/sdcard/Download/$ed" >/dev/null 2>&1
  done
  if [[ -d "$FX/drive_tree" ]]; then
    $ADB $flag shell mkdir -p /sdcard/Download/zl_drive_tree/sub_a /sdcard/Download/zl_drive_tree/sub_b >/dev/null 2>&1
    $ADB $flag push "$FX/drive_tree/." /sdcard/Download/zl_drive_tree/ >/dev/null 2>&1
  fi
  for f in Pictures/zl_test_1.jpg Documents/zl_test_doc.pdf Music/zl_test_audio.wav; do
    $ADB $flag shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/$f" >/dev/null 2>&1
  done
  for perm in RECORD_AUDIO READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_MEDIA_AUDIO POST_NOTIFICATIONS ACCESS_FINE_LOCATION READ_CONTACTS READ_CALENDAR; do
    $ADB $flag shell pm grant "$PKG" android.permission.$perm >/dev/null 2>&1
  done
}

if [[ -x "$ADB" && -f "$FX/zl_test_image.jpg" ]]; then
  if [[ "${1:-}" == "--two-device" || "${1:-}" == "--two-device-all" ]]; then
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
  echo "    export ANDROID_HOME=<your Android SDK>   # macOS: \$HOME/Library/Android/sdk"
  echo "    npm run appium"
  exit 2
fi

# Preflight: two people add suites on separate branches, so two suites can end up
# on the same class-level @Order — git merges both without complaining and the run
# order silently changes. This check is pure source inspection (no device, ~1s), so
# it catches the clash before we spend an hour running against a device.
TESTORDER_JAVA="src/test/java/com/zillit/automation/core/TestOrder.java"
# Suites declare @Order(TestOrder.NAME), so resolve NAME -> value via TestOrder.java.
ORDER_MAP=$(grep -rH --include="*.java" "^@Order(TestOrder\." src/test/java/com/zillit/automation/tests 2>/dev/null \
  | sed -E 's|.*/([A-Za-z0-9_]+)\.java:@Order\(TestOrder\.([A-Z0-9_]+)\)|\2 \1|' \
  | while read -r name cls; do
      val=$(sed -nE "s/.*public static final int[[:space:]]+$name[[:space:]]*=[[:space:]]*([0-9]+)[[:space:]]*;.*/\1/p" "$TESTORDER_JAVA")
      [[ -n "$val" ]] && echo "$val $cls"
    done)
# A raw @Order(123) bypasses TestOrder entirely — that is how duplicates sneak in.
RAW=$(grep -rl --include="*.java" "^@Order([0-9]" src/test/java/com/zillit/automation/tests 2>/dev/null \
  | xargs -n1 basename 2>/dev/null | sed 's/\.java//')
if [[ -n "$RAW" ]]; then
  echo "✗ These suites hardcode @Order instead of using a TestOrder constant:"
  echo "$RAW" | sed 's/^/    /'
  echo "  A raw number bypasses TestOrder, so a duplicate merges silently."
  echo "  Add a constant to $TESTORDER_JAVA (in your module's range) and use it."
  exit 2
fi
DUPES=$(echo "$ORDER_MAP" | awk 'NF{print $1}' | sort -n | uniq -d)
if [[ -n "$DUPES" ]]; then
  echo "✗ @Order collision between test classes — their run order is undefined."
  for n in $DUPES; do
    echo "    @Order($n) → $(echo "$ORDER_MAP" | awk -v n="$n" '$1==n {printf "%s ", $2}')"
  done
  echo "  Fix: pick a free number from YOUR module's reserved range —"
  echo "    src/test/java/com/zillit/automation/core/TestOrder.java"
  echo "  (48-69 Vidya / tools · 70-89 Bhavik / admin settings · 90-99 destructive)"
  exit 2
fi

UDID="${ZILLIT_UDID:-emulator-5554}"

# Relaunch the app to the foreground (a cold Appium session fails otherwise —
# the launcher activity is SplashActivity, and monkey is flaky at foregrounding).
relaunch_app() {
  # Emulators only: on a physical phone a cold start raises Zillit's fingerprint/PIN app
  # lock, which a run cannot clear. There the app is just brought to the front.
  [[ "$UDID" == emulator-* ]] && $ADB -s "$UDID" shell am force-stop "$PKG" >/dev/null 2>&1
  $ADB -s "$UDID" shell am start-activity -n "$PKG/com.zillit.zillitapp.splash.SplashActivity" >/dev/null 2>&1
  sleep 7
}

if [[ "${DRIVE_TWO_DEVICE:-0}" == "1" ]]; then
  DEV_A="${ZILLIT_SENDER_UDID:-emulator-5554}"
  DEV_B="${ZILLIT_RECEIVER_UDID:-}"
  if [[ -z "$DEV_B" ]]; then
    echo "✗ ZILLIT_RECEIVER_UDID is required for --drive-two-device"
    echo "  e.g. ZILLIT_SENDER_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=<serial> $0 --drive-two-device"
    exit 2
  fi
  if [[ "$DEV_A" == "$DEV_B" ]]; then
    echo "✗ The two devices must be different (both were '$DEV_A')"
    exit 2
  fi
  for d in "$DEV_A" "$DEV_B"; do
    if ! $ADB -s "$d" shell true >/dev/null 2>&1; then
      echo "✗ Device '$d' is not reachable — check 'adb devices'"
      exit 2
    fi
  done

  ACC="build/allresults"
  rm -rf "$ACC"; mkdir -p "$ACC"
  prep_device "$DEV_A"
  prep_device "$DEV_B"

  # "Label::device::classes" — device A owns the content, device B receives it.
  DRIVE_PHASES=(
    "Drive — owner side (device A)::$DEV_A::DriveTest DriveUploadTest DriveViewTest DriveOrganiseTest DriveEditorTest DriveCommentsTest DriveRequestFilesTest DriveActivityTest DriveSettingsTest DriveShareTest"
    "Drive — recipient side (device B)::$DEV_B::DriveSharedWithMeTest"
    "Drive — collaborative editing (both)::$DEV_A::DriveCollabEditTest"
    "Drive — symmetric E2E (both)::$DEV_A::DriveTwoDeviceE2ETest"
  )
  TEST_EXIT=0
  for entry in "${DRIVE_PHASES[@]}"; do
    label="${entry%%::*}"; rest="${entry#*::}"
    dev="${rest%%::*}"; classes="${rest#*::}"
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "▶ $label   [$dev]"
    echo "════════════════════════════════════════════════════════════"
    args=(cleanTest test --continue)
    for c in $classes; do args+=(--tests "*.$c"); done
    ZILLIT_UDID="$dev" ZILLIT_SENDER_UDID="$DEV_A" ZILLIT_RECEIVER_UDID="$DEV_B" \
      ./gradlew "${args[@]}"
    [[ $? -ne 0 ]] && TEST_EXIT=1
    if ls build/test-results/test/*.xml >/dev/null 2>&1; then
      for f in build/test-results/test/*.xml; do
        cp "$f" "$ACC/$(basename "${label// /_}")__$(basename "$f")" 2>/dev/null || true
      done
    fi
  done
  if ls "$ACC"/*.xml >/dev/null 2>&1; then
    rm -f build/test-results/test/*.xml 2>/dev/null || true
    cp "$ACC"/*.xml build/test-results/test/ 2>/dev/null || true
  fi
  node tools/make-report.mjs || true
  exit $TEST_EXIT
fi

if [[ "${RUN_ALL:-0}" == "1" ]]; then
  # ---- FULL SWEEP: each module as its own gradle run, results accumulated ----
  ACC="build/allresults"
  rm -rf "$ACC"; mkdir -p "$ACC"
  # Ordered module list — "Label::Class1 Class2 …" — runs from project setup all
  # the way through every module. (LoginFlowTest is NOT included: it needs a manual
  # OTP, so it can't run unattended — do the OTP login once before starting --all.)
  MODULES=(
    "Create and select project::CreateAndSelectProjectTest"
    "Home / Bulletin chat::HomeChatTest PostingRightsTest MediaPreviewTest MediaSystemPickerTest ChatOptionsTest CallSheetTest RegressionStressTest AllUnitsMediaTest"
    "Home · web gap cases::HomeWebGapTest"
    "Call Sheet upload mode::CallSheetUploadModeTest"
    "Calendar::CalendarTest"
    "Calendar · web gap cases::CalendarRecurringScopeTest CalendarWebGapTest"
    "Email::EmailTest EmailDrawerTest EmailExtrasTest EmailActionsTest EmailCalendarTest"
    "C&C (Chat & Calling)::CcGroupCreateTest CcGroupChatTest CcGroupManageTest CcDirectChatTest CcChatMediaTest CcCallTest CcCallTabTest CcContactsTest"
    "Tools::ConfidentialInfoTest InfoTest CateringTest ExternalUserTest RecceTest DriveTest ToolsManagementTest"
    "Sides::SidesTest"
    "Continuity::ContinuityTest"
    "Call Sheet Creation::CallSheetCreationTest CallSheetApprovalTest"
    "Production Report Creation::ProductionReportTest ProductionReportApprovalTest"
    "Schedule D.O.D.::ScheduleDodTest"
    "Crew DOOD::CrewDoodTest"
    "Z-E-Signature::ESignatureTest"
    "Chat documents → editor::ChatDocumentEditorTest"
    "Casting::CastingTest"
    "Costume::CostumeTest"
    "Location tool::LocationToolFullTest"
    "Notifications::NotificationsTest"
    "Document Distribution::DocDistributionTest DocDistFoldersTest DocDistFilesTest DocDistComposerTest DocDistPresetsTest DocDistTemplatesTest DocDistContactsTest DocDistHistoryTest DocDistWatermarkTest DocDistPublishTest DocDistGridViewTest DocDistPublishTypesTest DocDistNestedFoldersTest DocDistRightsTest DocDistWebGapTest"
    "Drive::DriveUploadTest DriveShareTest DriveEditorTest DriveViewTest DriveOrganiseTest DriveCommentsTest DriveRequestFilesTest DriveActivityTest DriveSettingsTest"
    "Schedule Distribution::ScheduleDistTest"
    "Production Diary / Box Schedule::BoxShellTest BoxTypeTest BoxScheduleCrudTest BoxConflictTest BoxEventNoteTest BoxProductionScheduleTest BoxScheduleEditTest BoxCrossViewTest BoxEventLifecycleTest"
    "Map & Location::MapModuleTest MapLocationPinsTest LocationToolTest MapCityCrudTest MapZoneCrudTest MapPinBoundaryTest MapTypeManagerTest MapLocationCrudTest MapDirectionsShareTest MapGuardsTest MapCrudLifecycleTest MapCityMultiTest MapLocationFlowTest MapWebGapTest MapZoneGuardTest"
    "Settings / Admin Settings::DepartmentListingOrderTest AdminSettingsGroupsTest CompanyDetailsTest CreateAdditionalShootingUnitTest CreateNewDepartmentTest CreateNewDesignationTest CreateRemoteShootingUnitTest CreateUpdateHomeUnitTest CustomizationOfToolsTest EditProjectNameTest ManageToolGroupsTest PreApprovedUsersTest ApproveNewUserRequestTest SosReceiversTest UserManagementTest WatermarkLogoTest DeleteProjectTest"
    "Settings (tab)::EditMyProfileTest AppPreferencesTest LinkedDeviceTest InviteUsersTest ZillitHelpTest PermissionGridTest AdminSettingsWebGapTest"
  )
  if [[ "${TWO_DEVICE_ALL:-0}" == "1" ]]; then
    MODULES=(
      "Home · two devices::TwoUserChatTest"
      "Calendar invites · two devices::CalendarInviteTest"
      "C&C · two devices::CcGroupChatTest"
      "Info / Confidential Info · two devices::InfoTwoUserTest"
      "Catering · two devices::CateringTwoUserTest"
      "Call Sheet approval · two devices::CallSheetApprovalTest"
      "Production Report approval · two devices::ProductionReportApprovalTest"
      "Casting · two devices::CastingTwoUserTest"
      "Costume · two devices::CostumeTwoUserTest"
      "Location tool · two devices::LocationToolTwoUserTest"
      "Schedule Distribution · two devices::ScheduleDistTwoUserTest"
      "Box history · two devices::BoxHistoryTwoUserTest"
      "Drive · folder share::DriveFolderShareTest"
      "Drive · two-device journey::DriveTwoDeviceE2ETest"
      "Drive · collaborative edit::DriveCollabEditTest"
    )
  fi
  if [[ "${SKIP_PROJECT_MODULE:-0}" == "1" ]]; then
    MODULES=("${MODULES[@]:1}")  # drop "Create and select project"
  fi
  # Modules whose suites each need their OWN Appium session, instead of sharing one
  # session for the whole module.
  #
  # The Map suites render a live Google map. UiAutomator waits for an idle screen before
  # every lookup and a map never goes idle, so these sessions are the ones that lose the
  # on-device UiAutomator2 instrumentation — and once it is gone, every remaining case in
  # that gradle run fails with no assertion behind it. Measured 2026-11-20: ten Map suites
  # in ONE session gave 3 passed / 38 failed over 76 minutes; the same ten, one session
  # each, gave 30 passed / 2 failed with nine suites completely clean.
  #
  # Costs one extra gradle startup per suite. Add a label here only with a run to justify
  # it — this is not the default for good reason.
  PER_SUITE_LABELS=("Map & Location" "Production Diary / Box Schedule" "Document Distribution" "Drive")
  TEST_EXIT=0
  for entry in "${MODULES[@]}"; do
    label="${entry%%::*}"; classes="${entry##*::}"
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "▶ MODULE: $label"
    echo "════════════════════════════════════════════════════════════"
    _per_suite=0
    for _l in "${PER_SUITE_LABELS[@]}"; do [[ "$label" == "$_l" ]] && _per_suite=1; done
    if [[ $_per_suite -eq 1 ]]; then
      for c in $classes; do
        echo "  ── $c (own Appium session)"
        relaunch_app
        ./gradlew cleanTest test --continue --tests "*.$c" \
          || TEST_EXIT=1
        if ls build/test-results/test/*.xml >/dev/null 2>&1; then
          cp -f build/test-results/test/*.xml "$ACC"/ 2>/dev/null
        fi
      done
    else
      relaunch_app
      args=(cleanTest test --continue)
      for c in $classes; do args+=(--tests "*.$c"); done
      ./gradlew "${args[@]}"
      [[ $? -ne 0 ]] && TEST_EXIT=1
    fi
    # Accumulate this module's JUnit XMLs (one file per test class, no name clashes).
    if ls build/test-results/test/*.xml >/dev/null 2>&1; then
      cp -f build/test-results/test/*.xml "$ACC"/ 2>/dev/null
    fi
    if [[ "${CREATE_PROJECT_FIRST:-0}" == "1" && "$label" == "Create and select project" ]]; then
      if [[ -s build/run-project.txt ]]; then
        export ZILLIT_RUN_PROJECT="$(cat build/run-project.txt)"
        echo "▶ Every module from here runs in project: $ZILLIT_RUN_PROJECT"
      else
        echo "✗ The project was not created and selected — stopping, so nothing runs in another project."
        TEST_EXIT=1
        break
      fi
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
