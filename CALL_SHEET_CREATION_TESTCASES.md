# Call Sheet Creation — test cases

**Call Sheet Creation** is where a call sheet is written from a template, sent round for
comments, signed off by its approvers and published. Tools → **Call Sheet Creation** (tile
`callsheet_tool`) opens `bottomNav/tools/callsheet/ui/projects/ProjectDetailActivity` — **not**
`CallSheetDetailActivity` / `CallSheetEditorActivity`, which are the viewer and the editor
behind it. It talks to its own service, `CALLSHEET_BASE_URL/api/v2` (the dev flavour uses
`STG_CALLSHEET_BASE_URL`).

> **Not the Home "Call Sheet" unit.** `CallSheetTest` and `CallSheetUploadModeTest` drive the
> Call Sheet *chat unit* on Home (doc-only picker, PDF bubble, upload-mode sheet). This tool
> only touches that unit at the very end: **Publish in App** posts the PDF there. Until
> 2026-11-23 `MODULE_AUTOMATION_LIST.md` wrongly marked this tool covered by those suites.

**Status: test cases written 2026-11-23 from the source (`Zillit-Android` @
`feature/create-shooting-schedule`) and the QA module testing report, then corrected from a
read-only walk on the emulator (creator side) the same day. Not yet automated; the approver
side has not been walked yet.** Two devices: **emulator = creator**, **Pixel 7 = approver**.

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own; `id` in backticks is a real resource id. **[QA CS-nn]** marks
a case that re-checks a row of the QA sheet's *Call Sheet* tab (see *From the QA sheet*).
**[2D]** cases need both devices. *Italic* notes say when a case skips.

---

## The shape of the workflow

```
DRAFT ──Send for Comments──▶ "For Comments" (PENDING_INTERNAL_APPROVAL)   stays in Drafts
  │                          recipients can only comment — no approve/reject on Android
  └──Send for Signature──▶ "Pending Signature" (PENDING_APPROVAL)
         creator: Approvals › Sent        approver: Approvals › Received
         ├─ an approver Rejects (reason) ─▶ "Final Rejected" (APPROVAL_REJECTED)
         │      └─ creator Edit → Save ("move it back to Draft") → Send for Signature again
         └─ every approver Approves ─▶ "Final Approved" (APPROVED_FOR_PUBLISH, locked)
                 creator + approvers: Approvals › Finalized
                 └─ creator Publish (App / DD / Both) ─▶ "Published"  → Published tab
                                                          + PDF in Home "Call Sheet" unit
```

Who does what:

| Role | Tabs | Can |
|---|---|---|
| Admin or posting rights | Drafts · Approvals · Published (+ Permission if admin) | create, edit, send, delete, publish (own sheets) |
| View rights + owes a signature | Drafts (read-only) · Approvals (Received, Finalized) | comment, approve, reject |
| View rights only | Drafts (read-only) | view, comment on sheets sent to them |

**Rows on the Approvals sub-tabs have a blank name** (`text_approval_name`, `text_sent_name`,
`text_finalized_name` are all set to `""`). The creator reads the sheet's **Serial No.**
(`text_draft_id`) on its Drafts card and both devices find the row by that.

---

## 1. Module shell · `CSC-0`

| # | Case | Expected |
|---|---|---|
| CSC-01 | Tools → "Call Sheet Creation" (creator, posting rights) | `ProjectDetailActivity`; `headerText` **"CALL SHEET CREATION"** (upper-case; the toolbar shows the project name); tabs `tlTab` in order **Drafts · Approvals · Published** (+ **Permission** for an admin); Drafts selected |
| CSC-02 | Tile name follows the rights | Posting/admin: the unit's name ("Call Sheet Creation"). View-only: "Drafts Call Sheet". *Second half on the approver device* |
| CSC-03 | Drafts tab furniture (creator) | `fab_new_callsheet` shown; filter chips `chip_filter_all` / `chip_filter_drafts` / `chip_filter_for_comments` read "All 0", "Drafts 0", "For Comments 0" (one space) and the counts add up to the rows |
| CSC-04 | Filter chips filter | "Drafts" shows only "Draft" pills; "For Comments" only "For Comments" pills; "All" both |
| CSC-05 | Empty Drafts | `text_empty` "No drafts yet". *Skips when the project has drafts* |
| CSC-06 | Approver device, view rights only **[2D]** | Tile reads "Drafts Call Sheet"; only **Drafts**, read-only: no FAB, no chips, no "Drafts Template" group, row menu = View (+ Comment once sent) |
| CSC-07 | Drafts order **[QA CS-04]** | A draft created now is the **first** row. Sheet: it is listed last (server sorts on Updated only). *Expected to fail until fixed* |

## 2. Create and the editor · `CSC-1`

| # | Case | Expected |
|---|---|---|
| CSC-10 | `fab_new_callsheet` (content-desc "New Call Sheet") | The automation project has **Template 1 and Template 2**, so the picker always shows (`tplTitle` "Choose a template", numbered options, `tplConfirm` "Use …"); with one, the editor straight away. `templateLoadingOverlay` may block up to 6 s first |
| CSC-11 | Editor opens | Toolbar "New Call Sheet"; `toggle_mode` with "Preview" / "Sections"; Sections lists the template's sections (`recycler_sections`) with "Header" first and an "Approvers" row |
| CSC-12 | Header dialog (Sections → Header) | "Call sheet info." (`btn_cancel` "CANCEL", `btn_done` "DONE") with `edit_shoot_day`, `edit_total_days`, `btn_date_picker` ("23 NOV 2026" style), `edit_day_type` ("Select day type"). New sheet on the automation project: shoot day **1**, total days **0**, today. Change total days → DONE → the **Preview** reads "CALL SHEET <day> of <total>". The Header row's summary is fixed text, "Shoot day, date, day type", so assert in Preview |
| CSC-13 | Day type | The dropdown offers **SWD, CWD, SCWD, "+ Add new"**; the chosen one fills `edit_day_type` and Preview's "Day Type: SWD". The list is a **popup window** that a plain `uiautomator dump` does not show; the page object must find it with the multi-window setting or by position under the field |
| CSC-14 | Add a section | `fab_add_section` → "Section / Table / Notes / Approval / Page Break" → pick "Notes" → a new row at the end of `recycler_sections` |
| CSC-15 | Delete a section | `btn_delete_section` → "Delete Section" / "Are you sure you want to delete this section?" → **Cancel** keeps it, **Delete** removes it |
| CSC-16 | Manage Approvers | The **Approvers** row is the **last** row (scroll) → "Manage Approvers", "Select who needs to approve this call sheet"; the list includes **me** as well as the others; `edit_search` filters members; `btn_save` "Save Approvers" is **disabled until the selection changes**; tick the approver → Save. The Approvers row summary stays "Approval section" (it never names anyone), so assert by reopening the dialog: the approver's `checkbox_select` is checked. *Writes the project default too — done once, in setup* |
| CSC-17 | New sheet overflow | `action_more` offers **Save As** and **Save as Template** only — no Save, no Send (a new sheet can't be sent) |
| CSC-18 | Name dialog | Save As on a new sheet → titled **"Save As New Draft"** (seen on the device; the source suggested "Save Call Sheet"), "Name", `input_name` hint "Enter call sheet name"; `btn_save` **disabled while blank** (spaces count as blank) |
| CSC-19 | Save a new sheet | Name `ZL_AUTO_CS_<run>` → Save → "Saved successfully", editor closes, Drafts has the card: name, pill "Draft", "Serial No.: N", "Day: …", created by me |
| CSC-20 | Back with no changes **[QA CS-08]** | Toolbar back on an untouched new sheet closes it **silently**, like system back. Sheet: the "Unsaved changes" dialog appears anyway. *Expected to fail until fixed* |
| CSC-21 | Back with changes | Change the header → back → "Unsaved changes" / "You have unsaved changes. Would you like to save before leaving?" with **Save & Leave / Discard / Cancel**; Cancel stays, Discard leaves and nothing is saved |
| CSC-22 | Edits survive recreation **[QA CS-09]** | Change Total Days → toggle font scale 1.3 → 1.0 → the value is still there and back still warns. Sheet: the edit is silently lost. *Expected to fail until fixed* |
| CSC-23 | Edit an existing draft | Card ⋮ → Edit → "Edit Call Sheet"; overflow now offers **Save, Save As, Send for Signature, Send for Comments** |
| CSC-24 | Save As a copy **[QA CS-01]** | Edit → Save As → dialog titled **"Save As New Draft"** → new name → "Saved as new draft"; Drafts has both, each with its own serial |
| CSC-25 | Save again bumps the version **[QA CS-02]** | Edit → change → Save → History lists a second revision (`badge_revision` v2). Sheet: version stays v1 on iOS; Android not checked |
| CSC-26 | Save as Template / delete template | New sheet → Save as Template → confirm → `Saved as "<name>"`; it appears under "Drafts Template"; its `btn_template_actions` → "Delete template?" → gone |

## 3. Drafts row actions · `CSC-2`

| # | Case | Expected |
|---|---|---|
| CSC-30 | Row menu on own Draft | `btn_actions` → **View · Edit · Send for Comments · Send for Signature · Send to Document Distribution · Send for Chat · History · Delete** (no Comment, no Reminder while "Draft"). Items are matched by text |
| CSC-31 | View | `CallSheetDetailActivity`: toolbar = sheet name, "Loading PDF..." then the PDF in `webview_pdf`; no share/download; back returns to Drafts |
| CSC-32 | View header **[QA CS-06]** | The View screen shows status, version, creator, time and "Day Type: …" above the PDF (iOS has it). Sheet: Android shows only the name. *Expected to fail — parity gap* |
| CSC-33 | PDF fetched once **[QA CS-11]** | One detail + one PDF request per open (`adb logcat -s CallSheetAPI`). Sheet: both are fetched twice. *Log check; skips without logcat access* |
| CSC-34 | History on a new draft | "Created" row by me; `btn_close` "Close" |
| CSC-35 | Delete | ⋮ → Delete → "Delete Draft" / `Are you sure you want to delete "<name>"?` → Cancel keeps it; Delete removes the card and the chip count drops |

## 4. Send for Comments · `CSC-3`

| # | Case | Expected |
|---|---|---|
| CSC-40 | Open the picker | ⋮ → Send for Comments → **"Select Recipients"** (step 2 directly); I am not in the list; `btn_send` disabled at 0 |
| CSC-41 | Counter and Select All **[QA CS-10]** | Tick one → "1 of M selected", "Send to 1 Recipient"; Select All → "M of M"; Deselect All → 0 and `btn_send` disabled. Sheet: the counter counts hidden pre-ticked ids (starts one high) and Select All doesn't repaint "Previously Selected". *Expected to fail until fixed* |
| CSC-42 | Search | `edit_search` "Search by name, designation, department" narrows the list; `btn_clear_search` restores it |
| CSC-43 | Send for comments **[2D]** | Tick the approver → Send → pill becomes **"For Comments"**, row stays in Drafts under the "For Comments" chip; ⋮ now offers **Comment** and no longer Send for Comments |
| CSC-44 | Recipient sees it **[2D]** | Pixel: the sheet is in Drafts (read-only) with "For Comments"; its menu is View + Comment; **no Approve/Reject** |
| CSC-45 | Comment round-trip **[2D]** | Pixel comments "ZL_AUTO c<run>" → emulator's `btn_comments` shows `badge_comments`; the thread lists the comment with the Pixel user as `text_author` |
| CSC-46 | Comment badge for the other side **[QA CS-03] [2D]** | Emulator comments → Pixel's tile / card shows an unread comment badge. Sheet: no badge for B (iOS); Android not checked |
| CSC-47 | Edit / delete own comment | `btn_comment_menu` → edit shows "edited"; delete → "Are you sure you want to delete this comment?" → gone; others' comments have no menu |
| CSC-48 | Remove a recipient | Send for Comments again, untick the recipient → "Remove from comments?" with "Remove from comments only" / "Remove comments and viewing access" / Cancel |

## 5. Send for Signature · `CSC-4`

| # | Case | Expected |
|---|---|---|
| CSC-50 | No approvers | On a project with no approvers: ⋮ → Send for Signature → "No approvers" / "No approvers have been set for this project, so this call sheet cannot be sent for signature. Add approvers first, then try again." *Skips when approvers exist (setting none would wipe the project default)* |
| CSC-51 | Send for signature | ⋮ → Send for Signature (fires immediately, no confirm) → the card **leaves Drafts**; Approvals › Sent has the serial with "Pending Signature", "Created By: <me>" and Created/Updated dates. **The Sent card shows no "0/1 approved"** (the Received card does) |
| CSC-52 | Sent row menu | Pending: ⋮ → Edit · Reminder · Comment · History · Send for Chat · Send to Document Distribution · Delete. After a rejection (seen on device): **View · Edit · Comment · Send to Document Distribution · Send for Chat · History · Delete** — no Reminder and **no Send for Signature** |
| CSC-53 | Reminder | Sent ⋮ → Reminder → `input_message` pre-filled "Please review and approve this call sheet."; clearing it disables `btn_send`; Send → success; History shows "Reminder Sent" |
| CSC-54 | Send from the editor | Edit a draft → overflow Send for Signature → it saves a revision first, then sends; editor closes; row in Sent |

## 6. Approver side — Received · `CSC-5` **[2D]**

| # | Case | Expected |
|---|---|---|
| CSC-60 | Notification and badge | After CSC-51, the Pixel's Call Sheet tile `badge` reads "1"; inside, the **Approvals** tab and the **Received** sub-tab have content-desc **"1 new notification"** (the badge shows up there, not as text). A "Zillit Dev" push also arrives. *The push is not asserted (it lands on Drafts anyway)* |
| CSC-61 | Approvals for a view-only approver | Approvals appears within ~8 s of opening the tool (async signature probe); reopen once if not |
| CSC-62 | Received row | Row with the creator's serial: blank name, "Pending Signature", `badge_stage` "For Signature", `text_created_by` "Created By: <creator>", "0/1 approved" |
| CSC-63 | Received menu | ⋮ → **View · View History · Chat · Comment · Approve · Reject** |
| CSC-64 | Reject needs a reason | Reject → empty → `btn_reject` → inline error **"Please provide a rejection reason"**; dialog stays |
| CSC-65 | Reject | Reason "ZL_AUTO reject <run>" → Reject → after a **pull to refresh** (the approver's list does not update by itself) the row shows **"Final Rejected"**, "0/1 approved", and the menu is View · View History · Chat · Comment. The creator's Sent row turns "Final Rejected" live |
| CSC-66 | Creator sees the rejection | Emulator: Sent row "Final Rejected"; History (newest first): "Rejected" `badge_stage` "Final", `text_by` "Vidya Pixel (Screen writer)", `text_reason` **"Reason: <reason>"**; "Sent for Signature" "Final"; "Created" `badge_revision` "v1". The role is inside `text_by`, not a separate field |
| CSC-67 | Resubmit after rejection | Emulator: Sent ⋮ → Edit → Save → "Confirm save" / "This file is already shared for review. Saving will move it back to Draft…" → Continue → back in Drafts as "Draft" → Send for Signature → Pixel gets a **new** Received request (round 2) with Approve/Reject |
| CSC-68 | Approve without signature | Pixel: ⋮ → Approve → `text_title` "Approve <sheet name>", "Approved By:" `text_approver_name` "Vidya Pixel (Screen writer)", `text_approve_timestamp`; two cards found by text: "Attach Signature" and **"Approve Without Signature"** → the row leaves Received and, with nothing else pending, the **Received sub-tab disappears** |
| CSC-69 | Creator sees the approval | Emulator: row moves from Sent to **Finalized**, pill **"Final Approved"**; ⋮ has no Edit/Delete (locked) |
| CSC-70 | Finalized on the approver | Pixel: Finalized has the serial; ⋮ = View · View History · Comment — **no Publish** |
| CSC-71 | Approve with signature | Approve → **Attach Signature** → pick a saved signature → approved. *Skips when the approver has no saved signature in Forms & Signatures* |
| CSC-72 | Chat with the creator | Received ⋮ → Chat → member picker → 1:1 chat opens; picking yourself → "You can't chat with yourself." |
| CSC-73 | Received hides when empty | With no pending request the Received sub-tab is absent after load; with one it is present |

## 7. Publish · `CSC-6`

Publishing posts into the real **Call Sheet** Home unit and notifies people. These cases are
**opt-in** (`-Dzillit.callsheet.publish=true`) and use **Continuation** only — never **New**,
which moves the unit's chat to History.

| # | Case | Expected |
|---|---|---|
| CSC-80 | Publish destinations | Finalized ⋮ → Publish → "Where should this be published?" with Publish in App / via Document Distribution / on Both / Cancel; the DD options are disabled with "Needs Document Distribution posting rights" without DD rights |
| CSC-81 | Publish in App | Publish in App → upload-mode sheet (`umContinuation`) → "Publish Call Sheet" → **Skip** → the row leaves Finalized; Published shows "Currently Published" / "Live" with the serial |
| CSC-82 | PDF lands in the unit | Home → Call Sheet unit has a new "Call Sheet" PDF message (reuse `HomeChatPage`) |
| CSC-83 | Approver after publish **[2D]** | Pixel: row gone from Finalized; no Published tab (view-only) |
| CSC-84 | Published card | `btn_latest_view` opens the PDF; `btn_latest_history` lists the publish; `btn_latest_comments` opens a **read-only** thread; `btn_history_toggle` lists older publishes |
| CSC-85 | Cancel publishes nothing | Publish → Cancel → row still in Finalized |

## 8. Permission tab (admin) · `CSC-7`

| # | Case | Expected |
|---|---|---|
| CSC-90 | Tab shape | "To allow users to create a call sheet.", search, members with `switch_permission`; I am not listed |
| CSC-91 | Grant and revoke | Toggle a test member on → off; no "Failed to update permission". *Admin only; restores the original state* |

## 9. Offline · `CSC-8`

| # | Case | Expected |
|---|---|---|
| CSC-95 | Pull to refresh offline **[QA CS-07]** | Wi-Fi and data off → pull Drafts → a readable message, **the app keeps running**. Sheet: raw `UnknownHostException` with the API host, then a crash (High). *Expected to fail until fixed; restores network in `finally`* |
| CSC-96 | Sent offline **[QA CS-05]** | Offline → Approvals › Sent shows an error or the last rows, not "No approval requests". *Expected to fail until fixed* |

## 10. Cleanup · `CSC-99`

| # | Case | Expected |
|---|---|---|
| CSC-99 | Sweep | Every `ZL_AUTO_CS_*` draft and Sent sheet the run made is deleted; saved templates named `ZL_AUTO_*` are deleted. Finalized/Published sheets are locked and cannot be deleted — that is why publish is opt-in |

---

## From the QA sheet

*Module testing report → Call Sheet* tab, read 2026-11-23. Android status as the sheet
gives it.

| Sheet row | Android | Severity | What it says | Case |
|---|---|---|---|---|
| CS-01 | Not affected | Low | Save As sheet had the wrong title | CSC-24 |
| CS-02 | Not checked | Medium | Version stays v1 after saving again | CSC-25 |
| CS-03 | Not checked | Medium | No comment badge for the other user | CSC-46 |
| CS-04 | Not checked | Low | New draft listed last in Drafts | CSC-07 |
| CS-05 | Not checked | Low | Offline Sent reads "No sent approvals." | CSC-96 |
| CS-06 | **Open** | Medium | View screen has no status/version/creator/Day Type header | CSC-32 |
| CS-07 | **Open** | **High** | Offline pull-to-refresh on Drafts crashes the app | CSC-95 |
| CS-08 | **Open** | Low | Toolbar back always says "Unsaved changes" | CSC-20 |
| CS-09 | **Open** | Low | Edits lost on activity recreation | CSC-22 |
| CS-10 | **Open** | Low | Select Recipients counts hidden ids; Select All doesn't repaint | CSC-41 |
| CS-11 | **Open** | Low | PDF fetched and rendered twice per open | CSC-33 |

## Found in the source (not on the sheet)

1. **No "Send for Signature" on a rejected sheet in Sent** (`SentListAdapter.kt:88-89`); web
   has it. Resubmitting needs Edit → Save → Send (CSC-67).
2. **The editor ignores admin** (`CallSheetEditorActivity.kt:182-187`): an admin without a
   posting-rights row sees the FAB, but the editor closes with "You don't have posting rights".
3. **Manage Approvers overwrites the project default** `final_approver_ids` — it can supersede
   other sheets' pending requests.
4. **Reminder list's `btn_send_reminder` has no click handler**; Drafts' "Reminder" can never
   show (Drafts never holds PENDING_APPROVAL).
5. **Send-for-Comments step 1** (For Comments / For Signature cards) is unreachable.
6. **A push for an approval request lands on Drafts**, not Approvals.
7. **Drafts "Send for Signature" fires with no confirm** and is offered on another poster's draft.

## Device walk — 2026-11-23 (emulator, creator, read-only)

Nothing was saved or sent. What the device showed, where it differs from or adds to the source:

- **Accounts.** Emulator = **Vidya Testing** (Production coordinator, admin). The project has
  **two members**: Vidya Testing and **Vidya Pixel** (Writer, Screen writer) — the Pixel's user.
  **No approvers are set** on the project yet.
- **Vidya Pixel already has create rights**: on the Permission tab their `switch_permission`
  is **on and disabled**. So the Pixel is *not* a view-only user, and the view-only cases
  (CSC-02 second half, CSC-06, CSC-61) skip unless that changes.
- Tool header is `headerText` "CALL SHEET CREATION"; tabs Drafts · Approvals · Published ·
  Permission; Drafts empty ("No drafts yet", chips "All 0").
- Template picker: Template 1 / Template 2, "Create your own template", "Use Template 1".
- Editor Preview is a WebView whose text **is readable** in the accessibility tree
  ("CALL SHEET 1 of 0", "Day Type: —", section headings…), so Preview assertions work.
- Sections (Template 1): Header, then ~20 Section/Table/Notes rows, **Approvers last**.
- New-sheet overflow: **Save As, Save as Template** — confirmed.
- First save dialog title: **"Save As New Draft"**; Save disabled while blank — confirmed.
- System back with changes: "Unsaved changes" with Cancel / Discard / Save & Leave — confirmed;
  Discard leaves nothing behind.
- Approvals for the creator with nothing sent: sub-tabs **Sent, Finalized** only (Received is
  dropped when empty — confirmed); "No approval requests".
- Published empty: "No published call sheets".

## Device walk — 2026-11-23 (both devices, approver path)

With the user's OK, **Vidya Pixel is now the approver** (and project default) for Call Sheet
Creation. One sheet, **`ZL_AUTO_CS_walk1` (Serial No. 1)**, went the whole way:

1. Created and saved on the emulator → Drafts "Draft", "Serial No.: 1", "1 of 0".
2. Drafts ⋮ menu exactly as CSC-30. Send for Signature → leaves Drafts → Sent "Pending Signature".
3. Pixel: tile badge "1", Approvals / Received content-desc "1 new notification"; Received row
   as CSC-62 with "0/1 approved"; menu as CSC-63.
4. Empty reject reason → "Please provide a rejection reason" (`textinput_error`). Reject with a
   reason → "Final Rejected" after pull-to-refresh; creator's Sent updated live.
5. Creator: Sent ⋮ after rejection has no Send for Signature (finding 1 confirmed). History as CSC-66.
6. Edit → overflow Save · Save As · Send for Signature · Send for Comments → Save → "Confirm save"
   → Continue → the sheet is back in **Drafts** as "Draft" → Send for Signature (round 2).
7. Pixel: Received row pending again, Approve/Reject back → Approve Without Signature.
8. Both devices: **Finalized** row "Final Approved", Serial No. 1, blank name. Creator's menu:
   View · View History · Comment · **Publish**; approver's: View · View History · Comment.

**Left behind:** `ZL_AUTO_CS_walk1` is now Final Approved and therefore **locked** — it cannot be
deleted from the app and stays in Finalized (not published).

## Automation status — 2026-11-23

- Suites: [`CallSheetCreationTest`](src/test/java/com/zillit/automation/tests/callsheetcreation/CallSheetCreationTest.java)
  (one device, `TestOrder.CALLSHEET_CREATION = 180`) and
  [`CallSheetApprovalTest`](src/test/java/com/zillit/automation/tests/callsheetcreation/CallSheetApprovalTest.java)
  (two devices, `= 181`), on the shared `pages/workflow` page objects (`DocWorkflowPage`, `DocEditorPage`)
  and `pages/callsheetcreation/CallSheetCreationPage`. Run: `./run-ui.sh --call-sheet-creation`
  (set `ZILLIT_RECEIVER_UDID` to the approver's device for the approval round).
- Best single-device run so far (run 4 of 6): **26 passed, 5 failed, 2 skipped of 33**. Every case has
  passed in at least one run except the real app bugs below; the remaining instability was test-side
  (menu taps swallowed while the editor settles, the Preview's accessibility tree lagging) and is fixed.
- **App bugs confirmed by the suite:** CSC-20 (QA CS-08, toolbar back always asks), CSC-32 (QA CS-06,
  no header on the viewer), CSC-41 (QA CS-10, counter reads "2 of 1 selected" / "Send to 2 Recipients"
  with one person listed), CSC-13b (the header's day type reaches only the title line; the DAY TYPE box
  stays "—").
- Not automated yet: publish (opt-in), offline CSC-95/96, recreation CSC-22, templates CSC-26.

## From the web E2E report (`Zillit E2E Report`, generated 2026-11-17)

The web report's **Call Sheet** module (9 tests, `callsheet.spec.js`) covers the Home **Call Sheet unit**
(post / edit / reply / delete a message, emojis, upload in Continuation / New) — that is
`CallSheetTest` / `CallSheetUploadModeTest` on Android, not this tool. It has no Call Sheet Creation
tests. The cases it suggests by analogy with its Production Report suite were added here:
**CSC-02** (tabs and sub-tabs switch, web PR-S02/S03), **CSC-84** (Published card or "No published call
sheets", web PR-P03/P05), **CSC-10b** (Template 2 relabels the confirm button, Cancel opens no editor,
web PR-C02/C03). Its sheet bugs CS-01…03 are the QA sheet rows already mapped above.
