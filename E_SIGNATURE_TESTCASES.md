# Z-E-Signature — test cases and findings

**Z-E-Signature** is the production's DocuSign-style e-signature tool: a sender uploads a PDF,
names signers (project members or outside emails), places Sign Here / Initial / other fields on
the pages and sends an *envelope*; signers sign in the app, and the sender tracks it.
Tools → **E-Signature** opens `bottomNav/tools/docSign/ui/main/DocSignMainActivity`, a single
activity hosting one navigation graph (entry → list / received / templates / bulk → compose →
placement → detail / signing). API: `{host}api/v2/docusign/…`.

> **Its Tools tile is not called "Z-E-Signature".** The label is server-supplied; on
> ZL-Android-Automation (Pixel 7, dev) it reads **"E-Signature"**, while the module's own
> header reads "Z-E-Signature" (identifier `e_signature_tool`). Searching Tools for "Signature"
> also finds **"Documents & Signature"** — that is the separate *Forms & Signature* tool
> (`FormsAndSignatureDashboard`), not this one. The suite matches `(Z-)?E-Signature` exactly.

**Status: automated (2026-11-23, project ZL-Android-Automation). Last clean run: emulator-5556
(Vidya Testing) — 40 passed, 4 failed (all four are app bugs), 0 skipped.**
Suite [`ESignatureTest`](src/test/java/com/zillit/automation/tests/esignature/ESignatureTest.java)
(+ [`ESignatureBaseTest`](src/test/java/com/zillit/automation/tests/esignature/ESignatureBaseTest.java)),
`TestOrder.E_SIGNATURE = 174`, page objects in `pages/esignature/`:
`ESignaturePage` (entry, envelope list, segments, saved-signature sheet),
`ESignCreateSignatureDialog` (the Type / Draw creator) and `ESignComposePage` (Prepare,
signer picker, placement). Results of the last run are under *Run results* at the end.

```bash
./run-and-report.sh --only ESignatureTest
```

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own; `id` in backticks is a real resource id from the layout.
Cases marked **[QA ESIGN-xx]** carry a row of the module report's *E-Signature* tab; cases
marked **[finding]** are defects read from the source and seen on the device (see *Findings*).

---

## What the suite will NOT do, and why

* **It never sends an envelope.** Android has no void and no delete for a sent envelope:
  the row menu offers Edit / Delete only on a DRAFT (`DocuSignEnvelopeAdapter`, `isDraft &&
  canPost`), and `DocuSignListFragment.promptDelete` returns early for any other status.
  A sent `ZL_AUTO_` envelope would sit in both users' Sent / Action Required lists for good.
  The suite builds the whole envelope — PDF, signer, a placed field — opens the
  "Send this document?" confirm and **Cancels** it, then saves a **draft** and deletes it.
* **Two-device signing is not covered** (it needs a sent envelope — see above). The signing
  surface (`DocuSignSigningFragment`, pre-sign gate "Choose or Draw Your Signature",
  decline) is therefore untested here. It can be added the day the backend offers a void
  that removes the envelope, or if a permanent sentinel envelope is accepted.
* **Saved signatures are per USER and carry no name** (a tile reads "Choose signature"), so
  the creation cases run only when the user starts with **none** of that kind, and ES-99
  deletes back to the starting count. Otherwise they skip, to leave the user's own alone.
* Bulk Sends is checked for shape only (a bulk send is a real send to a CSV of recipients).
  **"Use Template" does not exist on Android** — removed by product decision 2026-09-04
  (`DocuSignTemplatesFragment`: a saved template is View / Bulk Send / Delete only), so QA
  ESIGN-02 is re-checked through **View**, which renders the template's document over the same
  pre-signed-URL fetch.

---

## Screens and ids

| Screen | Where | Key ids / wording |
|---|---|---|
| Entry (poster) | `DocuSignEntryFragment` | header "Z-E-Signature" / "Send, sign and track"; `btnManageSignatures` "Signatures"; segment chips `dsEntryChipManage` "Manage Envelopes", `dsEntryChipSign` "Sign Documents", `dsEntryChipTemplates` "Templates", `dsEntryChipBulk` "Bulk Sends" (in a HorizontalScrollView — "Bulk Sends" is past the right edge on a 1080-px screen); `btnUploadDocFab` "Upload Doc for Z-E-Signature" (Manage only) |
| Entry (viewer) | → `DocuSignReceivedFragment` | no chips; tabs `tabAction` "Action Required", `tabComplete`, `tabRejected` |
| Manage Envelopes | `DocuSignListFragment` | top tabs `topTabESignature` "Z-E-Signature", `topTabComplete` "Complete", `topTabRejected` "Rejected"; sub-tabs `subTabSent` "Sent" (default), `subTabDraft` "Draft" — only under Z-E-Signature (`subTabScroll` GONE otherwise); Sent chips "All / Awaiting / In progress" (only with ≥ 2 sent); `etSearch` "Search envelopes…"; `btnViewModeToggle`; rows `tvTitle`, `tvStatusChip`, `ivOverflow` → PopupMenu "View" (+ "Edit", "Delete" on a draft); empty `tvEmpty` |
| Empty copy | `strings.xml` | Sent "No sent envelopes" · Draft "No draft envelopes. Tap + to create one." · Complete "No completed envelopes yet" · Rejected "No rejected envelopes" |
| Sign Documents | `DocuSignReceivedFragment` | "Action Required" / "Complete" / "Rejected"; empty `tvEmptyTitle` "No documents waiting for your signature" |
| Templates | `DocuSignTemplatesFragment` | `etSearch` "Search templates…", `btnNewTemplate` "+ New Template", empty "No templates yet. Save an envelope as a template to reuse it."; cards `tvName`, `tvCategory`, `btnView` "View", `btnMore` → "Bulk Send" / "Delete" ("Delete template?"); View → `DocuSignTemplateDetailSheet` (`tvDetailName`, `tvDetailFieldsLabel` "FIELDS (n)", `detailPager`, `tvDetailDocError`, "Close") |
| Save template | `dialog_docusign_save_template` | `etTemplateName` (pre-filled from the title), `etTemplateDescription`, `etTemplateCategory`, "Cancel" / `btnSaveTemplateConfirm` "Save Template"; empty name → "Give the template a name", empty category → "Add a category (or type a new one)" |
| Bulk Sends | `DocuSignBulkSendsFragment` | `btnViewList`, `btnViewGrid`, `rvJobs`; empty "No bulk sends yet" |
| My Saved Signatures | `DocuSignPreSignGateDialog` (manage mode, bottom sheet) | `tvGateTitle` "My Saved Signatures", subtitle "Draw a new signature or initial, or long-press a saved one to delete it."; "YOUR SIGNATURES" `tvSignatureCount` "N / 5", `rowSignatures`; "YOUR INITIALS" `tvInitialCount`, `rowInitials`; tiles are id-less (label "Choose signature" / "Choose initials"), last tile "+ Draw New"; `btnContinue` "Done"; long-press → "Delete saved signature?" Cancel / Delete |
| Signature creator | `DocuSignCreateSignatureDialog` (AlertDialog) | `tvDialogTitle` "Manage Your Saved Signatures"; "STEP 1 OF 2" "How would you like to make your signature?"; `cardType` "Type your signature" (EASIEST), `cardDraw` "Draw your signature"; `cardUpload` compiled out; step 2: `etTyped` "Your name" + 6 styles, or `pad` + `btnClearPad`; `btnUse` "Use this signature" / "Use these initials"; `btnBack`, `btnClose` |
| Prepare Document | `DocuSignComposeFragment` | `docPickerRow` / `tvPickedFile` "Upload PDF Document"; `etTitle` "Title (e.g. Crew Deal Memo)"; `etDescription`; email subject/body; `btnSelectSigners` "Inside project", `btnAddExternalSigner` "Outside project"; `switchSelfSigner` "I need to sign this document"; `tvSignersHeading` "SIGNERS (n)"; CC section; `btnNext` "Next: Place Fields", `tvNextHint` |
| PDF pick | host `openMediaPicker` | "Choose below" sheet → `tvLabel` "Document" → media permission prompts → system picker → `GalleryItemView` preview → `sendBtn` |
| Select Signers | `DocuSignPickUsersBottomSheet` | "Select Signers", "Pick the people who will sign this envelope", `tvSelectedCount` "n selected", tabs "Project Users (n)" / "External Users (n)", rows `cb` `tvName` `tvEmail`, `btnCancel`, `btnDone` |
| Place Fields & Send | `DocuSignPlacementFragment` | `tvFieldCount` "n field(s) placed"; `btnSaveDraft` "Save as Draft", `btnSend` "Send"; stepper "Pick signer / Drag or click field / Place field"; `pagerPdf` → `zoomHost` / `pageOverlay`; `tvPageLabel` "Page 1 of 1"; `tvActiveSigner` "For: <name>"; `fieldChipRow` id-less chips "Sign Here", "Initial Here", "Check", "Radio", "Dropdown", "Text", … |
| Send confirm | AlertDialog `dialog_docusign_send_confirm` | "Send this document?"; `tvSendConfirmDoc` (title), `tvSendConfirmDocMeta`, `tvSendConfirmWhoHeader` "SIGNERS (n)", per-signer "n field(s)", Cancel / Send |
| Detail | `DocuSignDetailFragment` | status chip, title, "Recipient Activity" "0/1 signed", "Fields (1)", recipients with "Pending"; `btnRefresh`, `btnAuditTrail` |

Validation order on **Next: Place Fields** (`DocuSignComposeViewModel.missingPlacementReason`):
"Upload a PDF to continue." → "Add a title to continue." → "Add at least one signer to continue.".
**Send** on placement: no Sign Here / Initial field → toast "Place at least one signature field
before sending — each signer needs somewhere to sign on the document."; a signer without a
field → "No signature field placed for %s…". **Save as Draft** has no field requirement.

---

## Cases

| Id | Case | Expected |
|---|---|---|
| ES-01 | Open Tools → E-Signature | Lands on Manage Envelopes (selected); chips "Manage Envelopes", "Sign Documents", "Templates", "Bulk Sends"; "Signatures"; "Upload Doc for Z-E-Signature" |
| ES-02 | Tap the top tabs | "Z-E-Signature" / "Complete" / "Rejected"; "Search envelopes…" box; Sent/Draft only under Z-E-Signature |
| ES-03 | Visit Sent, Draft, Complete, Rejected | Each shows rows or its own empty copy (table above) |
| ES-04 **[finding 1]** | Empty Draft tab | Copy points at the real create control (it says "Tap + to create one." — there is no +) |
| ES-05 | Search a string matching nothing; clear | The box keeps the query; list empties with an empty state; clearing restores the same list |
| ES-06 | List/grid toggle twice | Layout switches and comes back; list screen and "Search envelopes…" box intact |
| ES-07 | Visit Sign Documents, Templates, Bulk Sends | Upload button hidden there; back on Manage it shows |
| ES-08 | Sign Documents | "Action Required" / "Complete" / "Rejected"; rows or "No documents waiting for your signature" |
| ES-09 | Templates | "+ New Template", "Search templates…" |
| ES-10 | Bulk Sends | list/grid buttons; jobs or "No bulk sends yet" |
| ES-20 | Tap "Signatures" | "My Saved Signatures" sheet, subtitle, YOUR SIGNATURES / YOUR INITIALS with "N / 5", Draw New tiles, "Done"; tile count = counter |
| ES-21 | Draw New (signature) | Creator "STEP 1 OF 2", Type / Draw cards, no Upload card; Type → "STEP 2 OF 2" + "Use this signature"; Back → step 1; Close dismisses |
| ES-22 | Type pane, Use with no name | Toast "Please type a name first"; creator stays |
| ES-23 | Draw pane, Use on an empty pad | Toast "Please draw a signature first"; creator stays |
| ES-24 | Type "ZL_AUTO …" → Use | Sheet reopens; signatures counter +1, one more tile ("Choose signature") |
| ES-25 | Draw New (initials) → draw → "Use these initials" | Initials counter +1 |
| ES-26 **[finding 2]** | Save signatures up to 3, then a 4th | 4th refused with "Max 3 saved. You can still use this one now."; the counter's limit should read 3 (it reads 5) |
| ES-27 | Long-press a saved tile → Cancel | "Delete saved signature?" with its message; Cancel keeps the tile |
| ES-28 **[finding 3]** | Long-press → Delete | Tile removed AND counter decremented (counter stays at the old value) |
| ES-30 | Tap "Upload Doc for Z-E-Signature" | "Prepare Document"; "Upload PDF Document"; "Next: Place Fields" |
| ES-31 | Next with nothing | "Upload a PDF to continue."; stays on Prepare |
| ES-32 | Upload `zl_test_doc.pdf` (Documents) | Row reads "zl_test_doc.pdf"; title pre-filled "zl_test_doc"; "Replace" and the rename icon shown |
| ES-33 | Clear the title → Next | "Add a title to continue."; then title `ZL_AUTO_ESIGN_<run>` |
| ES-34 | Next with no signer | "Add at least one signer to continue."; "SIGNERS (0)" |
| ES-47 | "Inside project CC" → Cancel | "Select CC" / "Pick the people who get a copy when it's done"; Cancel leaves "CC (0)" |
| ES-35 | Inside project → tick a member → Done | "Select Signers", "0 selected" → "1 selected"; "SIGNERS (1)", member listed "Needs to Sign" |
| ES-36 | "I need to sign this document" on / off | "SIGNERS (2)" then back to "SIGNERS (1)" |
| ES-48 | ADD MESSAGE: subject + message | Section opens (state is persisted, so it may already be open); both fields keep the typed text |
| ES-37 | Next | "Place Fields & Send"; page renders; "0 field(s) placed"; "Page 1 of 1"; toolbar has "Sign Here", "Initial Here"; "Save as Draft"; "For: <signer>" |
| ES-38 | Send with no field | Toast "Place at least one signature field before sending…"; no confirm |
| ES-39 | Arm "Sign Here", tap the page | "1 field(s) placed" |
| ES-40 | Send → confirm → Cancel | "Send this document?" names the title, "SIGNERS (1)", the signer, "1 field(s)"; Cancel stays on placement, nothing sent |
| ES-41 | Save as Draft | Back on the list; under Draft the `ZL_AUTO` row with status "Draft" |
| ES-42 | Draft row ⋮ | "View", "Edit", "Delete" |
| ES-43 | Tap the draft | Detail: title, "Recipient Activity" "0/1 signed", "Fields (1)", signer "Pending" |
| ES-44 **[QA ESIGN-01]** | Edit → Next, back, Edit → Next again | Page renders both times; the saved field is restored ("1 field(s) placed") |
| ES-45 **[finding 4]** | Edit the draft | Document row shows "zl_test_doc.pdf" (shows the upload key "…_dsuploadzl_test_doc.pdf") |
| ES-46 | ⋮ → Delete → Cancel, then Delete | "Delete this draft?" / "This draft will be permanently removed."; Cancel keeps it; Delete removes it |
| ES-50 | Templates → "+ New Template" → upload, title `ZL_AUTO_TPL_<run>` → Next | Placement in template mode: "Save Template" instead of Save as Draft / Send; no signer needed; page renders |
| ES-51 | Place a Sign Here; Save Template with an empty category | Dialog name pre-filled from the title; toast "Add a category (or type a new one)"; dialog stays |
| ES-52 | Category "zl auto" → Save Template | "Template saved"; back in the module; the template listed under Templates (search) |
| ES-53 **[QA ESIGN-02]** | View the template, close, View again | Detail sheet names it; its document renders both times; "FIELDS (1)" |
| ES-54 | Template ⋮ → Delete | Menu "Bulk Send", "Delete"; "Delete template?" → Delete removes it |
| ES-99 | Cleanup | No `ZL_AUTO` draft or template; saved signatures/initials made by the run deleted |

Not automated: send / sign / decline / remind / audit trail (needs a sent envelope — see
above); Outside-project signers and CC (would register external contacts); "Preview as";
Save as Template from a normal envelope; Bulk Send; the viewer-only route (needs a user without
posting rights); the "no viewing rights" prompt.

---

## From the web E2E report

`web-e2e/e-signature.md` (Zillit E2E Report, 2026-11-17, dev.zillit.com — all 15 skipped on web
because the tool was not enabled in the web sandbox project). Naming differs between the
platforms: web's "Upload Document" segment is Android's **"Manage Envelopes"**, web's
"Completed" tabs are Android's **"Complete"**, web's "My Saved Signatures" button is Android's
**"Signatures"** (its content description is "My Saved Signatures").

| Web test (esignature.spec.js) | Our case | Notes |
|---|---|---|
| module loads with the manager mode switch (:66) | ES-01 | Android has four segments; the first is "Manage Envelopes", not "Upload Document" |
| Upload Document segment shows the envelope tabs and toolbar (:76) | ES-01, ES-02 | ES-02 now also checks the "Search envelopes…" box (added from the web case) |
| Sent tab renders its envelope panel (:96) | ES-02, ES-03 | Sent is the default sub-tab on Android |
| Sign Documents segment shows the receiver tabs (:108) | ES-08 | "Action Required" / "Complete" / "Rejected" |
| Templates segment opens (:123) | ES-09 | |
| Bulk Sends segment opens (:131) | ES-10 | **Web-only detail:** web checks a Refresh button; Android has none (pull-to-refresh + list/grid buttons), so ES-10 checks those |
| My Saved Signatures picker opens (:139) | ES-20 | Android goes further: ES-21…28 create / validate / delete |
| envelope list/card view toggle works (:150) | ES-06 | Added the web's check that the search box survives both toggles |
| envelope search accepts a query (:158) | ES-05 | Added the web's check that the box keeps the query; ES-05 also checks the filtering |
| upload a document opens the editor Prepare step (:196) | ES-32 | Added "Replace" (and the rename icon) from the web case. Android's hint after upload is "Add at least one signer…" only once Next is tapped (ES-34) |
| Add Signer drawer opens (:202) | ES-35 | Android: "Inside project" → "Select Signers" sheet |
| Add CC drawer opens (:210) | **ES-47 (new)** | Was missing; Android: "Inside project CC" → "Select CC" |
| "I need to sign this document" adds self as a signer (:218) | ES-36 | Run after a member is picked, so it reads "SIGNERS (2)", then back to 1 |
| Add message accepts a subject and body (:228) | **ES-48 (new)** | Was missing |
| advancing to Place fields shows the field toolbar (:241) | ES-37 | Added the web's "Save Draft" check ("Save as Draft"); Android's tile is "Sign Here", not "Drag or click to place Signature" |
| Sheet bug ESIGN-01 | ES-44 | as in the QA table below |
| Sheet bug ESIGN-02 | ES-53 | as in the QA table below |

Web-only: none of the 15 is impossible on Android; only the Bulk Sends Refresh button has no
Android counterpart. Android-only (no web case): ES-04, ES-07, ES-21…28, ES-31, ES-33/34,
ES-38…46, ES-50…54.

## From the QA sheet

Module report, tab **E-Signature**:

| QA row | Title | Android status on the sheet | Automated as | Result |
|---|---|---|---|---|
| ESIGN-01 | Reopening an envelope in the same session never shows the document | Not checked (fixed in `af29d7261` per notes) | ES-44, on a draft (Edit → Place Fields twice). The signing-screen variant needs a sent envelope. | see *Run results* |
| ESIGN-02 | Use Template does not load the template's document | Not checked | ES-53. "Use Template" was removed from Android (product decision 2026-09-04), so the same fetch is exercised by View ×2 on a ZL_AUTO template (ES-50…54 create and delete it). The QA note "Save Template stays disabled until Category is filled" is ES-51 (on Android it is a toast, not a disabled button). | see *Run results* |

Bug sheet, tab **Forms & Signature** (BUG-01/02/08/09/10/16/19/20/22/23, WEB-01/02/03): every
row is a **backend or web** defect of the separate *Forms & Signature* tool (signing-link
tokens, storage paths, document chat, revise, discard notifications, external sign page,
`isParentChat || true`, …). None is reachable from the native Z-E-Signature screens, so none
is automated here.

---

## Findings

1. **Draft empty copy points at a control that is gone** (ES-04). "No draft envelopes. Tap +
   to create one." — the "+" menu was moved off the list (`bindNewMenu()` removed); the only
   create control is "Upload Doc for Z-E-Signature". Low.
2. **Saved-signature counter shows the wrong limit** (ES-26). The sheet renders "N / 5"
   (`DocuSignPreSignGateDialog.SAVED_SIG_CAP = 5`) but the app enforces 3 per kind
   (`DocuSignManageSignaturesViewModel.maxPerType = 3`, `integer/docusign_saved_sig_max_per_type
   = 3`): at "3 / 5" the 4th is refused with "Max 3 saved. You can still use this one now.".
   Medium (the UI promises two slots that do not exist).
3. **Deleting a saved signature leaves the counter stale** (ES-28). `promptDelete` removes the
   tile but never rewrites `tvSignatureCount` / `tvInitialCount` (set once in
   `populateCarousel`), so after deleting the only signature the sheet reads "1 / 5" over an
   empty carousel until it is reopened. Low.
4. **Editing a draft shows the storage key as the file name** (ES-45). A fresh upload reads
   "zl_test_doc.pdf"; the same draft opened with Edit reads
   "4734295459135_dsuploadzl_test_doc.pdf" — the S3 object name, not the document's name. Low.
5. **The PDF picker asks for "music and audio" and "photos and videos" access** before the
   system file picker opens (`PickerDialog` launches `pdfFilePickerPermissionRequest` with
   `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO` + `READ_MEDIA_AUDIO` on API 33+). The system
   document picker needs none of them. Low / privacy (not asserted).
6. **Send confirm meta loses its space**: "1 page(s) · 1 field(s) to complete· use Preview to
   see it as each person will" — `docusign_send_confirm_doc_meta_preview_hint` starts with a
   space, which aapt trims. Cosmetic (not asserted).
7. **"Use your mouse, or your finger on a touchscreen."** — web copy on the Android Draw card.
   Cosmetic (not asserted).
8. The stale **`tvNextHint`** stays on screen after the missing piece is supplied (it is only
   refreshed on the next tap of Next). Cosmetic (not asserted).

---

## Run results

| Run | Device / user | Result | Notes |
|---|---|---|---|
| 1 | Pixel 7 · Vidya Pixel | 14 ✓ · 3 ✗ · 27 skipped | Environment: the run started with the system picker in front (left by an earlier suite) → ES-01/02 failed in BaseTest's first-launch check; the shared `SystemFilePickerPage` could not reach `/sdcard/Documents` on the Pixel (picks a wrong drawer root "by elimination") → every case after the upload skipped. Both handled since (run script backs out of a foreign app; `ESignComposePage.pickViaDeviceStorage`). |
| 2 | emulator-5556 · Vidya Testing | 32 ✓ · 5 ✗ · 7 skipped | Test bug: the row / template menus are AppCompat PopupMenus (app `:id/title`, not `android:id/title`) — fixed; template "rendered" signal wrong — now `tvDetailPageLabel` "Page n of m". |
| 3 | emulator-5554 · Vidya Testing | 40 ✓ · 3 ✗ · 1 skipped | Fails = ES-26, ES-28, ES-45 (app). Skip = ES-04 (run 2's draft still listed); ES-99 swept run 2's leftovers. |
| **4** | **emulator-5556 · Vidya Testing** | **40 ✓ · 4 ✗ · 0 skipped (9 m 49 s)** | **Only app bugs fail: ES-04 (finding 1), ES-26 (finding 2), ES-28 (finding 3), ES-45 (finding 4).** QA ESIGN-01 (ES-44) and ESIGN-02 (ES-53) pass — not reproduced. Nothing ZL_AUTO left (ES-99 ✓). |

| 5 | Pixel 7 · Vidya Pixel | 0 ✓ · 44 skipped | Not a result: the phone was back on its PIN lock screen when the run got the device (18:31), so nothing past Tools could run. **The suite has not yet had a clean run on the Pixel**; the device-storage picker route it needs there was verified on emulator-5556 (run 4) and by hand on the Pixel's drawer ("Pixel 7" root). |

Known flake, handled: once per run, "Draw New" was tapped but the creator did not appear
(ES-24, right after ES-23 closed a creator without saving). Likely cause, from the source but
not proven: closing the creator without saving leaves `DocuSignEntryFragment.reopenManageSheet`
set, so the next signatures load re-shows the sheet under the tap. `openCreator` retries once;
it has always recovered.

Nothing is ever sent; drafts, the template and the saved signatures/initials made by a run are
deleted by the cases themselves or ES-99.
