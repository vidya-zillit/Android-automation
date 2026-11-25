# Plan — automating Call Sheet Creation and Production Report Creation

Written 2026-11-23. Test cases:
[CALL_SHEET_CREATION_TESTCASES.md](CALL_SHEET_CREATION_TESTCASES.md) (CSC-*, 69 cases) and
[PRODUCTION_REPORT_CREATION_TESTCASES.md](PRODUCTION_REPORT_CREATION_TESTCASES.md) (PRC-*, 60 cases).
Both come from the app source, the web test plan and the QA *Module Testing Report*
(Call Sheet tab: 11 bugs, 6 open on Android; Production Report tab: 16 bugs, 6 open on Android).

## The one idea that shapes the design

The two tools are **the same workflow built twice**: Drafts → Send for Comments → Send for
Signature → Approve/Reject → Finalized → Publish, on **the same layout files and resource
ids** (`fragment_draft_list`, `item_draft_row`, `fragment_approval_list`, `item_sent_row`,
`item_approval_row`, `item_finalized_row`, `dialog_reject`, `dialog_comments`,
`dialog_history`, `dialog_send_for_approval`, `dialog_save_name`, `dialog_approvers_editor`,
`bottomsheet_template_picker`, the publish-destination prompt and the upload-mode sheet).
So we write the workflow page objects **once** and give each module a thin layer for what
differs (entry, create button, editor extras, publish target).

## Devices and accounts

| Device | Role | Needs |
|---|---|---|
| **Emulator** `emulator-5554` (AVD `Medium_Phone_API_37.0`) | **Creator** | Member of `ZL-Android-Automation` with **posting rights** on `callsheet_tool` and `production_report_tool` (an admin row alone is not enough for the Call Sheet editor) |
| **Pixel 7** `34281FDH2005XC` | **Approver** | Member of the **same project** as a **different user**, with **view rights** on both tools (no posting rights, so the view-only cases run) |

**Seen on the devices 2026-11-23:** emulator = **Vidya Testing** (Production coordinator,
admin); Pixel = **Vidya Pixel** (Writer, Screen writer). They are the project's only two
members. Vidya Pixel already has **create rights** on Call Sheet Creation (switch on and
locked), so the Pixel is a poster, not view-only. The view-only cases skip unless an admin
takes that right away. No approvers are set on either tool yet.

Env, following the existing two-device suites (`DriveCollabEditTest`):
`ZILLIT_SENDER_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC ZILLIT_FLAVOR=dev`,
drivers on systemPorts 8200 / 8210.

## Blockers to clear before any device work

1. **adb keeps crashing — Android Studio's adb.** Studio starts its own adb server without
   `ADB_MDNS=0`; it dies in the mDNS bridge every few seconds (28 crash reports on 2026-11-23)
   and drops the emulator. Fix once, either way:
   - Android Studio → Settings → Build, Execution, Deployment → Debugger → untick
     **"Enable adb mDNS for wireless debugging"**, then restart Studio; or
   - quit Android Studio while suites run.
2. ~~Confirm the accounts~~ — done (see above). **The Pixel must stay unlocked** during runs
   (Developer options → Stay awake while charging).
3. ~~Production Report template flag~~ — **on** for ZL-Android-Automation (checked on the
   emulator).
4. ~~A template with Call Times rows~~ — Template 1 has Crew Call / Unit Wrap.
5. **Approvers.** Setting approvers in either editor also overwrites the project's default
   approver list. The suites set the Pixel user as the only approver **once**, in setup, and
   leave it — tell the team, since it affects anyone else testing on this project.

## Code layout

```
pages/workflow/            shared by both tools (new)
  DocWorkflowDashboard     tabs (tlTab), Drafts list, chips, row ⋮ menu by text, serial lookup
  DocEditorPage            toggle, Sections list, Header dialog, Insert-section, Approvers, name dialog,
                           overflow menu, discard dialog  (native parts only — WebViews avoided)
  ApprovalsPage            Sent / Received / Finalized sub-tabs, rows by serial, Approve / Reject /
                           Reminder dialogs
  CommentsDialog, HistoryDialog, RecipientsPicker, PublishFlow (+ reuse callsheet/UploadModePage)
  PdfViewerPage            Detail activities (loading / error / retry)
pages/callsheetcreation/   CallSheetCreationPage   (entry, fab_new_callsheet, Permission tab)
pages/productionreport/    ProductionReportPage    (entry + template-flag check, fabCreate,
                                                    call-times fields, PR chat for publish)
tests/callsheetcreation/   CallSheetCreationBaseTest, CallSheetCreationTest (1 device),
                           CallSheetApprovalTest (2 devices)
tests/productionreport/    ProductionReportBaseTest, ProductionReportTest (1 device),
                           ProductionReportApprovalTest (2 devices)
```

Rules the page objects follow (learned from the source):
- **Find approval rows by Serial No.** — names are blank on Sent/Received/Finalized.
- **PopupMenu items by text** — they have no resource ids.
- **Scope by activity** — the same ids exist in both modules.
- **Avoid the section-editor WebView** (no WebView debugging in the app): edit through the
  Header dialog, the Sections list and the native dialogs only.
- **Received may be missing** until its first load or a badge — pull to refresh and wait.
- **No toast after approve/reject in Production Report** — assert on list state.

## Suites, order, runner

| Suite | Devices | TestOrder | Cases |
|---|---|---|---|
| `CallSheetCreationTest` | emulator | `CALLSHEET_CREATION = 180` | CSC-0x…4x, 5x (sender half), 90s, 95s, 99 |
| `CallSheetApprovalTest` | both | `CALLSHEET_APPROVAL = 181` | CSC-06, 43–46, 60–73, 83 |
| `ProductionReportTest` | emulator | `PRODUCTION_REPORT = 185` | PRC-0x…5x (sender half), 95s, 99 |
| `ProductionReportApprovalTest` | both | `PRODUCTION_REPORT_APPROVAL = 186` | PRC-02, 04, 42–44, 60–71, 85, 90 |

`TestOrder` 180–186 sit in the free `176–189` block ("tools overflow · VIDYA"); update the
`free:` line and the range table.

Runner (one flag and one `.run` file per module):

| Flag | Runs | `.run` |
|---|---|---|
| `--call-sheet-creation` | `CallSheetCreationTest`, then `CallSheetApprovalTest` if `ZILLIT_RECEIVER_UDID` is set (otherwise it skips with a message) | `Zillit ▸ Call Sheet Creation` |
| `--production-report` | `ProductionReportTest`, then `ProductionReportApprovalTest` likewise | `Zillit ▸ Production Report` |

Plus `make-report.mjs` names ("Call Sheet Creation", "Production Report Creation"), the
`MODULES` entries, README "Adding flows", and `MODULE_AUTOMATION_LIST.md` rows 13 and 16.

Publish cases are **opt-in** (`-Dzillit.callsheet.publish=true`,
`-Dzillit.productionreport.publish=true`): they post into real chats, notify people and leave
locked sheets that cannot be deleted. They use **Continuation** only, never **New**.

## Phases

| # | Phase | Output | Rough effort |
|---|---|---|---|
| 0 | Clear blockers 1–4; walk both tools on both devices with screenshots (no `uiautomator dump` mid-run); correct the test-case docs where the device disagrees | Updated docs, confirmed accounts | ½ day |
| 1 | Shared `pages/workflow` + Call Sheet single-device suite (shell, create/editor, drafts actions, comments picker, send, cleanup) | `CallSheetCreationTest` green on the emulator | 1–1½ days |
| 2 | Two-device base (two drivers, serial hand-off, per-device navigation, `finally` cleanup on both) + `CallSheetApprovalTest` (comment round-trip, reject → resubmit → approve, finalized) | `CallSheetApprovalTest` green on both | 1 day |
| 3 | Production Report on the same page objects: `ProductionReportTest` + `ProductionReportApprovalTest` (call times, no-call-sheet dialog, chat publish target) | both green | 1 day |
| 4 | Opt-in publish cases, offline cases (`svc wifi/data` with restore in `finally`), recreation cases (font scale / night mode with restore) | opt-in + regression cases | ½ day |
| 5 | Wiring (TestOrder, flags, `.run`, report names, README, module list) + full runs, each suite on its own, logcat checked for crashes; mark each QA row as reproduced / not reproduced | runs + report | ½ day |

Each phase ends with a run on the device; nothing is reported as an app defect until the
source confirms it (the Sides "1 pages" lesson).

## Risks

- **Backend rules we can't see:** whether every approver must approve, what one rejection does
  to the others' requests, and whether a revision save really resets to Draft. The cases
  assert what the UI shows and say so.
- **Shared project state:** default approvers, comment recipients and the Call Sheet / PR
  chats are project-wide. Everything the suites create is named `ZL_AUTO_CS_*` / `ZL_AUTO_PR_*`
  and swept; publish is opt-in.
- **Pushes are not asserted** (they land on Drafts and are timing-dependent); badges are.
- **Expected failures:** 12 cases re-check bugs that are open on Android today (CS-06…11,
  PR-11…16). They will fail until the app is fixed — that is the point; the report names the
  QA row for each.

## What the device walk (2026-11-23) changed

- **Approver set:** with the user's OK, Vidya Pixel is the approver and project default on both
  tools. Vidya Pixel is also a **project admin** (hence the locked create-right switch).
- **The full Call Sheet cycle works** on both devices: send → reject (reason required) →
  edit/save back to Draft → resend → approve → Finalized (Publish for the creator only).
- **Production Report reject is broken on dev:** `POST /approval-requests/<id>/reject` returns
  **404 `approval_request_not_found`** and the app shows nothing; approve works. The PR approval
  suite must expect this case to fail, and the reject→resubmit path can't run on Production
  Report until it is fixed.
- **Every approved item stays forever.** Final Approved and Published items are locked and
  can't be deleted from the app, so each approval run leaves one `ZL_AUTO_*` item per tool in
  Finalized (two are there now, `ZL_AUTO_CS_walk1` and `ZL_AUTO_P`). The suites will reuse the
  serial they create, never an older one, and the report will count what is left behind.
- **Page-object rules learned:** wait for the template preview before "Use Template"; pull to
  refresh on the approver after an action; badges are content-desc "1 new notification"; the
  day-type list is a popup window; the Approvers row summary never names anyone; times are
  24-hour.
- **The Pixel must stay unlocked** (Developer options → Stay awake). It also raised a location
  permission prompt on unlock; the suite should answer it the way `BaseTest` already handles
  permission dialogs.

## Decisions needed from you

1. ~~Pixel user as default approver~~ — approved and done 2026-11-23.
2. Should Vidya Pixel keep create rights on Call Sheet Creation (view-only cases skip), or
   should an admin turn them off so the view-only approver path is tested too?
3. Should publish ever run by default, or stay opt-in?
4. Is it acceptable that each approval run leaves one locked `ZL_AUTO_*` item per tool, or can
   someone with backend access clear them periodically?
