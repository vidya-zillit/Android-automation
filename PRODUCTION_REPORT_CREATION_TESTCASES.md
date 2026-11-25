# Production Report Creation — test cases

**Production Report Creation** is where the day's production report is written from a
template, sent round for comments, signed off by its approvers and published into the
Production Report chat. Tools → **Production Report Creation** (tile `production_report_tool`)
opens `bottomNav/tools/productionreport/ui/projects/ProductionReportDashboardActivity` —
**but only when remote config enables report templates for the project**
(`AppHelper.isProductionReportTemplateEnabled()`: a project-id list or `"All"`). Otherwise the
same tile opens the legacy `ProductionReportChatActivity` and none of this exists. The suite's
first job is to tell which one it got.

It is a **twin of Call Sheet Creation** ([CALL_SHEET_CREATION_TESTCASES.md](CALL_SHEET_CREATION_TESTCASES.md)):
the drafts, approvals, published and dialog layouts are the same files, so the resource ids
are the same in both modules (and some strings still say "call sheet"). Page objects must be
scoped by screen/activity, and one shared workflow page object can serve both.

**Status: test cases written 2026-11-23 from the source (`Zillit-Android` @
`feature/create-shooting-schedule`), the web plan (`zillit_web/e2e/PRODUCTION_REPORT_TEST_PLAN.md`)
and the QA module testing report. Not yet automated and not yet walked on a device.**
Two devices: **emulator = creator**, **Pixel 7 = approver**.

**How to read this sheet.** As in the Call Sheet sheet: quoted wording is the app's own,
`id` is a real resource id, **[QA PR-nn]** re-checks a row of the QA sheet's *Production
Report* tab, **[2D]** needs both devices, *italics* say when a case skips.

---

## Differences from Call Sheet that the cases depend on

| | Call Sheet Creation | Production Report Creation |
|---|---|---|
| Entry | always the dashboard | dashboard **only with the template remote-config flag**, else the chat |
| Create button | `fab_new_callsheet` (Drafts fragment) | `fabCreate`, content-desc "Create Production Report" (activity; Drafts page only) |
| New report pre-fill | shoot day, date | shoot day, date **and the last published Call Sheet** ("No Call Sheet Found" dialog if none) |
| Sending needs | approvers | approvers **and Crew Call + Unit Wrap** ("Missing Call Times") |
| Header dialog | no call times | `btn_crew_call`, `btn_unit_wrap` (only if the template has those rows) |
| Approver picker | current user excluded? (comments picker: yes) | **current user listed** |
| No-approvers dialog | "No approvers" | "No approver has been added yet." |
| Send success | server message | "Production report has been submitted for approval successfully." |
| Approve feedback | — | **none** (snackbars commented out) — assert on the lists |
| Published card Comments | read-only thread | **does nothing** [QA PR-12] |
| Publish lands in | Home "Call Sheet" unit | **Production Report chat** tool |
| Permission tab | admin | 2nd AD **and** posting on `permission_grid_tool` |

---

## 0. Entry · `PRC-0`

| # | Case | Expected |
|---|---|---|
| PRC-00 | Which screen did the tile open? | `ProductionReportDashboardActivity` (template flag on) → continue. `ProductionReportChatActivity` → **every case skips** with "Report templates are not enabled for this project (remote config)" |
| PRC-01 | Dashboard (creator) | `headerText` **"PRODUCTION REPORT CREATION"**; `tlTab` **Drafts · Approvals · Published** (+ Permission for 2nd AD with Permission Grid posting); `fabCreate` visible on Drafts, hidden on the other tabs |
| PRC-02 | Tile name follows the rights **[QA PR-05] [2D]** | Posting/admin: "Production Report Creation". Member without posting: the tile and the module title read the **same** name. Sheet: the tile keeps "…Creation" while the module says "Production Report" |
| PRC-03 | Drafts furniture | Chips "All  N" / "Drafts  N" / "For Comments  N" add up; "Drafts Template" group with "N+ Show more" when there are saved templates |
| PRC-04 | View-only approver **[2D]** | Pixel: Drafts only, read-only (no `fabCreate`, no chips, menu View/Comment); Approvals appears once a signature is owed |

## 1. Create and the editor · `PRC-1`

| # | Case | Expected |
|---|---|---|
| PRC-10 | `fabCreate` | Template picker ("Choose a template", Template 1 / Template 2, "Use Template 1"). **Wait for the picker's preview to render before tapping Use** — tapped early, the editor opens **blank** (only Header + Approvers, empty header, no call times, no "No Call Sheet Found"). Seen on the device; finding 7 |
| PRC-11 | No published call sheet | From a stock template on a project with no published call sheet: "No Call Sheet Found" / "No published call sheet was found for the shoot day. You can still fill in and submit the production report." → OK → editor. *Skips when a call sheet is published* |
| PRC-12 | Editor opens | "New Production Report"; Preview/Sections toggle; Sections has a virtual **"Header"** row first and an **"Approvers"** row (badge "Approval", no delete) |
| PRC-13 | Header dialog | "Production Report info." with `edit_shoot_day` (pre-filled **2** on the automation project), `edit_total_days` (0), `btn_date_picker`, `edit_day_type` (pre-set **SWD**) and — Template 1 has Call Times — `btn_crew_call` **"SELECT CREW CALL TIME"** / `btn_unit_wrap` **"SELECT UNIT WRAP TIME"** |
| PRC-14 | Set call times | The picker is a Material time picker in **keyboard-input mode** ("Hour" / "Minute" fields, `material_timepicker_ok_button`) and stores **24-hour** text: OK on 14:03 makes `btn_crew_call` read **"14:03"**. Set Crew Call 06:00 and Unit Wrap 19:00 → DONE → Preview shows both. *If the template has no Call Times rows, PRC-14 and every send case skip: "the template cannot be sent"* |
| PRC-15 | Add / delete section | As CSC-14 / CSC-15 ("Delete Section" confirm, Cancel keeps it) |
| PRC-16 | Manage Approvers | "Manage Approvers": search, `btn_save` disabled until changed; **the current user is in the list** (web excludes them). The dialog says **"Select who needs to approve this call sheet"** — call-sheet wording in Production Report (new finding, same family as PR-15). Tick the approver → Save. *Once, in setup: it overwrites the project default* |
| PRC-17 | New report overflow | `action_more` → Save As + Save as Template (or Update Template when opened from a saved one); **no Save, no Send** |
| PRC-18 | Name dialog **[QA PR-01, PR-15]** | First save: title **"Save Production Report"**, Save As: **"Save As New Draft"**; `btn_save` disabled while blank. The hint should be production-report wording — sheet PR-15: it reads "Enter call sheet name". *Hint half expected to fail* |
| PRC-19 | Save | `ZL_AUTO_PR_<run>` → "Saved successfully"; Drafts card with name, "Draft", "Serial No.: N", shoot day "12 of 42" or "-" |
| PRC-20 | Back with no changes **[QA PR-11]** | Toolbar up-arrow on an untouched new report closes silently. Sheet: "Unsaved changes" every time. *Expected to fail until fixed* |
| PRC-21 | Back with changes on a saved draft **[QA PR-08]** | Edit → change Shooting Call → back → "Unsaved changes" (Save & Leave / Discard / Cancel). Sheet (web): no dialog, the change is lost. Android: not checked |
| PRC-22 | Edits survive recreation **[QA PR-14]** | Delete a section → `cmd uimode night yes` → the section is still deleted and back still warns. Sheet: the edit is silently restored. *Expected to fail; restores the theme in `finally`* |
| PRC-23 | Existing report overflow | Edit → Save, Save As, Send for Signature, Send for Comments |
| PRC-24 | Save again → History | Edit → change → Save → History shows "Updated" (revision 2) |
| PRC-25 | Template save / delete | As CSC-26 (`Saved as "<name>"`, "Delete template?") |

## 2. Drafts row actions · `PRC-2`

| # | Case | Expected |
|---|---|---|
| PRC-30 | Row menu on own Draft | View · Edit · Send for Comments · Send for Signature · Send to Document Distribution · Send for Chat · History · Delete |
| PRC-31 | View | `ProductionReportDetailActivity`: title = report name, "Loading PDF..." then pages in `webview_pdf`; "Failed to load PDF" + Retry on error |
| PRC-32 | View loads fast and once **[QA PR-16]** | PDF shown within a few seconds; one report + one PDF fetch. Sheet: two of each and a ~42 s spinner. *Timing threshold 15 s; expected to fail until fixed* |
| PRC-33 | Delete | "Delete Draft" / `Are you sure you want to delete "<name>"?`; Cancel keeps, Delete removes |
| PRC-34 | Updated time is per report **[QA PR-04]** | Two drafts R1, R2; send only R2; R1's Updated time is unchanged |

## 3. Send for Comments · `PRC-3`

| # | Case | Expected |
|---|---|---|
| PRC-40 | Missing call times blocks sending | Draft without Crew Call/Unit Wrap → Send for Comments → **"Missing Call Times"** / "Please set Crew Call and Unit Wrap in the Call Times section before sending for approval." Saving such a draft is allowed |
| PRC-41 | Picker | "Select Recipients", me excluded, `btn_send` disabled at 0, Select All / Deselect All, search |
| PRC-42 | Send for comments **[2D]** | Tick the approver → Send → "Production report has been sent for comments successfully."; pill "For Comments" |
| PRC-43 | Recipient side **[2D]** | Pixel: in Drafts read-only, "For Comments"; View + Comment only; no Approve/Reject |
| PRC-44 | Comments both ways **[2D]** | Each side comments; the other's `badge_comments` counts it; own comments can be edited/deleted |
| PRC-45 | Removal prompt | Re-send without the recipient → "Remove from comments?" with its three choices |

## 4. Send for Signature · `PRC-4`

| # | Case | Expected |
|---|---|---|
| PRC-50 | No approver | "No approver has been added yet." / "This document cannot be sent for approval until at least one approver is added. Please add an approver and try again." *Skips when approvers exist* |
| PRC-51 | Missing call times | Same "Missing Call Times" dialog as PRC-40 from Send for Signature |
| PRC-52 | Send for signature | ⋮ → Send for Signature → snackbar (`snackbar_text`) **"Production report has been submitted for approval successfully."** (confirmed); the card leaves Drafts; Approvals › Sent: serial, blank name, "Pending Signature", "0/1 approved" |
| PRC-53 | Sent row menu | Edit · Reminder · Comment · History · Send for Chat · Send to Document Distribution · Delete; no Send for Signature |
| PRC-54 | Reminder | "Enter a reminder message..." → blank disables Send → Send Reminder → History "Reminder Sent" |
| PRC-55 | Send from the editor | Editor → Send for Signature → toast "Production report has been submitted for approval successfully.", editor closes |

## 5. Approver side · `PRC-5` **[2D]**

| # | Case | Expected |
|---|---|---|
| PRC-60 | Badges | Pixel: tile `badge` "1"; the Approvals tab's content-desc is "1 new notification"; a "Zillit Dev" push arrives. *Push lands on Drafts, so the case taps Approvals → Received itself* |
| PRC-61 | Received row | Serial from the creator; "Pending Signature"; "For Signature"; "Created By: <creator>"; ⋮ = View · View History · Chat · Comment · Approve · Reject |
| PRC-62 | Reject needs a reason | Empty reason → **"Please provide a rejection reason"** (web disables the button instead) |
| PRC-63 | Reject | Reason → Reject → row "Final Rejected", Approve/Reject gone; **no toast**. **Fails on dev (2026-11-23):** `POST /api/v2/approval-requests/<id>/reject` → **404 `approval_request_not_found`** for a request the list itself returns as PENDING/FINAL/round 1, and the app shows nothing — the report stays "Pending Signature". Approve on the same request works. Finding 8. *Expected to fail until fixed; the suite also asserts that a failed reject shows an error* |
| PRC-64 | Rejected stays in Received **[QA PR-03]** | After PRC-63 the row is **still** in Received. Sheet: product decision pending — the case records what it sees and asserts the current behaviour |
| PRC-65 | Creator after rejection | Emulator: Sent "Final Rejected" with a badge; History "Rejected" with the reason |
| PRC-66 | History shows roles **[QA PR-06]** | Every History row shows the person's role. **On Android the role is part of `text_by`** ("Vidya Testing (Production coordinator)") and the creator's Created / Sent for Signature rows **do** carry it — the sheet's PR-06 (web) does not reproduce here. Also check order: rows should be newest first; on the device "Sent for Signature" was listed **above** "Approved" (finding 9) |
| PRC-67 | Resubmit | Edit → Save → "Confirm save" (… "move it back to Draft" …) → Continue → Send for Signature → Pixel gets a new round-2 request |
| PRC-68 | Approve without signature | Approve → `text_title` "Approve <name>" → "Approve Without Signature" → row leaves Received, the sub-tab empties ("No approval requests"); **no toast**. Confirmed on the device |
| PRC-69 | Creator after approval | Sent → Finalized, "Final Approved", locked (no Edit/Delete), Finalized badge |
| PRC-70 | Approver's Finalized | View · View History · Comment; no Publish |
| PRC-71 | Approve with signature | Attach Signature → saved signature. *Skips without one* |

## 6. Publish · `PRC-6` (opt-in: `-Dzillit.productionreport.publish=true`)

Publishing posts into the real **Production Report chat** and notifies people. Continuation
only — never New.

| # | Case | Expected |
|---|---|---|
| PRC-80 | Destinations | "Where should this be published?" — App / Document Distribution / Both / Cancel; DD disabled without DD rights |
| PRC-81 | Publish in App | → `umContinuation` → row leaves Finalized; Published "Currently Published" / "Live" with the serial |
| PRC-82 | PDF in the chat | Tools → Production Report chat (`rvChat`) has the PDF |
| PRC-83 | Published card | View opens the PDF; History lists it; "History (N)" toggle |
| PRC-84 | Published Comments **[QA PR-12]** | `btn_latest_comments` opens a read-only thread. Sheet: nothing happens (no click handler). *Expected to fail until fixed* |
| PRC-85 | Approver after publish **[2D]** | Pixel: gone from Finalized; no Published tab; PDF visible in the chat tool |

## 7. Chat search · `PRC-7` **[2D]**

| # | Case | Expected |
|---|---|---|
| PRC-90 | Search count matches highlights **[QA PR-02]** | Emulator sends "ZL_AUTO PR-016 <run>" in the Production Report chat; Pixel searches "PR-016 <run>" → "1 Match Found" and exactly one highlight |

## 8. Offline · `PRC-8`

| # | Case | Expected |
|---|---|---|
| PRC-95 | Drafts offline **[QA PR-13]** | Pull to refresh offline → a friendly message and retry; no raw `UnknownHostException`. *Expected to fail; network restored in `finally`* |
| PRC-96 | Other tabs offline **[QA PR-09]** | Approvals › Sent and Published show an error/last rows, not "No approval requests" / "No published production reports"; Edit says why it did nothing |

## 9. Cleanup · `PRC-99`

| # | Case | Expected |
|---|---|---|
| PRC-99 | Sweep | Every `ZL_AUTO_PR_*` draft/Sent report and `ZL_AUTO_*` template from the run is deleted. Finalized/Published are locked — publish stays opt-in |

---

## From the QA sheet

*Module testing report → Production Report* tab, read 2026-11-23.

| Sheet row | Android | Severity | What it says | Case |
|---|---|---|---|---|
| PR-01 | Not checked | Low | Name / Save As sheets had the wrong title | PRC-18 |
| PR-02 | Not checked | Medium | Chat search count ≠ highlights | PRC-90 |
| PR-03 | Not checked | Low | Rejected report stays in Received (product decision) | PRC-64 |
| PR-04 | Not checked | Low | Sending one report changes another's Updated time | PRC-34 |
| PR-05 | Not checked | Low | Tile keeps "Creation" without posting rights | PRC-02 |
| PR-06 | Not checked | Low | History rows of the creator show no role | PRC-66 |
| PR-07 | Not affected | High | Desktop tile does nothing | — (Desktop only) |
| PR-08 | Not checked | High | Edit → Back loses the change with no warning (web) | PRC-21 |
| PR-09 | Not checked | Low | Offline Approvals/Published read empty | PRC-96 |
| PR-10 | Not checked | Low | Old web URL opens nothing | — (web only) |
| PR-11 | **Open** | Medium | Toolbar back always asks "Unsaved changes" | PRC-20 |
| PR-12 | **Open** | Medium | Published card Comment does nothing | PRC-84 |
| PR-13 | **Open** | Low | No offline state in Drafts | PRC-95 |
| PR-14 | **Open** | Medium | Dark-mode/font-scale change discards edits | PRC-22 |
| PR-15 | **Open** | Low | "Enter call sheet name" hint, hard-coded English | PRC-18 |
| PR-16 | **Open** | Medium | PDF fetched twice, ~40 s spinner | PRC-32 |

## Found in the source (not on the sheet)

1. **The tile opens the chat, not the dashboard, unless remote config lists the project.**
2. **A template without Crew Call / Unit Wrap rows can never be sent** — the call-times
   fields are hidden but the send check still demands them.
3. **Publish is gated two ways**: the menu item is creator-only, the dialog checks only posting rights.
4. **No approve/reject feedback** — the success snackbars are commented out.
5. **Manage Approvers overwrites the project default** and lists the current user (web doesn't).
6. **Approval pushes land on Drafts**; only comment pushes carry `manageTab`.
7. **"Use Template" before the preview loads gives a blank report** (seen on the device) — the
   Use button should wait for the template, or the editor should load it itself.
8. **Reject returns 404 and fails silently** (dev, 2026-11-23): `POST
   productionreportsapi-dev…/api/v2/approval-requests/6b03feee38973d5bc517aa4d/reject` →
   `{"status":0,"message":"approval_request_not_found"}` while `GET /approval-requests?assignee_id=…`
   lists that same id as PENDING. Approve on it succeeded. Call Sheet's reject works. Two bugs:
   the backend 404, and the app swallowing it (no message, list unchanged).
9. **History order**: "Sent for Signature" shown above the later "Approved".
10. **Raw label key on the Pixel's Tools**: the chat tile reads `production_report_tool_chat_label`
    (the emulator shows "Daily Production Report") — a missing translation on that account/locale.

## Device walk — 2026-11-23 (emulator, creator, read-only)

Nothing was saved or sent.

- **PRC-00: the template flag is on** for ZL-Android-Automation — the tile opens
  `ProductionReportDashboardActivity`. Tools also lists **"Daily Production Report"** (the chat
  tool) next to **"Production Report Creation"**.
- Dashboard: `headerText` "PRODUCTION REPORT CREATION"; tabs **Drafts · Approvals · Published**
  (no Permission tab for Vidya Testing); chips "All 0"; "No drafts yet"; `fabCreate`.
- Template picker: Template 1 / Template 2.
- **"No Call Sheet Found"** appears on every new report until a call sheet is published — confirmed.
- Sections (Template 1): Header, Production · Day / Date, DAYS, UNIT TIMES, SCENES, SCRIPT
  PROGRESS · Scene Counts, ROLLS, NOTES, CAST - WEEKLY & DAY PLAYERS, … Approvers last.
- Header dialog has **Crew Call / Unit Wrap** on Template 1, so sending is possible.
- Time picker stores **24-hour** values ("14:03").
- Manage Approvers lists Vidya Pixel and Vidya Testing; no approvers set; wording says
  "call sheet".
- Back with no real change closed the editor without a prompt (system back).

## Device walk — 2026-11-23 (both devices, approver path)

With the user's OK, **Vidya Pixel is now the approver** (and project default) for Production
Report Creation. One report, **`ZL_AUTO_P` (Serial No. 1, name cut short while typing)**:

1. First Create tapped "Use Template 1" before the preview rendered → blank editor (finding 7);
   discarded. Second try loaded Template 1 with the "No Call Sheet Found" dialog.
2. Crew Call / Unit Wrap set (24-hour "14:21"), approver ticked, Save As → Drafts "Draft",
   "2 of 0". Drafts ⋮ = View · Edit · Send for Comments · Send for Signature · Send to Document
   Distribution · Send for Chat · History · Delete.
3. Send for Signature → snackbar "Production report has been submitted for approval successfully."
   → Sent "Pending Signature".
4. Pixel: tile badge "1"; Received row "Pending Signature", "For Signature", "0/1 approved";
   menu View · View History · Chat · Comment · Approve · Reject.
5. **Reject twice → 404 `approval_request_not_found`, nothing shown** (finding 8).
6. Approve Without Signature → accepted; both devices: Finalized "Final Approved"; creator's
   menu has Publish. History: Sent for Signature, Approved, Created v1 (order — finding 9).

**Left behind:** `ZL_AUTO_P` is Final Approved and **locked** (not published, cannot be deleted).

## Automation status — 2026-11-23

- Suites: [`ProductionReportTest`](src/test/java/com/zillit/automation/tests/productionreport/ProductionReportTest.java)
  (one device, `TestOrder.PRODUCTION_REPORT = 185`) and
  [`ProductionReportApprovalTest`](src/test/java/com/zillit/automation/tests/productionreport/ProductionReportApprovalTest.java)
  (two devices, `= 186`), on the shared `pages/workflow` page objects and `pages/productionreport/ProductionReportPage`.
  Run: `./run-ui.sh --production-report`.
- First single-device run: **14 passed, 5 failed of 19**.
- **App bugs confirmed:** PRC-16 (Manage Approvers says "approve this call sheet"), PRC-18 (QA PR-15,
  hint "Enter call sheet name"), PRC-20 (QA PR-11, toolbar back always asks). PRC-32 (QA PR-16): the
  PDF did not show within 60 s — to be confirmed by hand. PRC-52 failed on the snackbar's timing
  (test-side, fixed); the send itself worked.
- The approval suite expects PRC-63 (reject) to fail on dev: finding 8, a 404 from the server.

## From the web E2E report (`Zillit E2E Report`, generated 2026-11-17)

| Web test | Web status | Our case |
|---|---|---|
| PR-S01 module shell renders with the manage tabs | passed | PRC-00/01 |
| PR-S02 tab switching keeps the shell mounted | passed | **PRC-02** (added) |
| PR-S03 Approvals Sent/Received sub-tabs swap the list | passed | **PRC-02** (added) |
| PR-P03/P05 Published hero card and history, or the empty message | passed | **PRC-84** (added; card or "No published production reports") |
| PR-C01/C02/C03 template picker lists layouts, switches, Cancel is clean | not run | PRC-10 + **PRC-10b** (added: "Use Template 2", Cancel opens no editor) |
| PR-D01/D02/D05 create a draft, see it listed, delete it | not run | PRC-19, PRC-33 |
| PR-D03/E01 edit a draft, save a revision without a second row | not run | **PRC-24** (added: v2 in History, row count unchanged) |
| PR-A01 Send for Signature with no approvers is refused | not run | PRC-50 (skips while the project has approvers) |
| PR-A02/A03 send for comments / signature | blocked on web | PRC-42, PRC-52 + `ProductionReportApprovalTest` |
| PR-A04/A05/A06 approve / reject by the second user | blocked on web | `ProductionReportApprovalTest` (PRC-60…70) |
| PR-P02/P06/P07 publish, attach, publish to DD | blocked on web | PRC-80…85, opt-in, not automated yet |

The web report's sheet bugs PR-01…06 are the same rows as the QA sheet, already mapped above.
