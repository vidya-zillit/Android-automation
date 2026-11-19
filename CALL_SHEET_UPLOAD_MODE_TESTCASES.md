# Call Sheet — "How are you uploading this document?" (ZL-21154)

Test cases for the upload-mode bottom sheet, written from the app source at
`~/Studio Project/Zillit-Android` (branch `development`).

Source of truth:

| Thing | Where |
|---|---|
| Sheet | `bottomNav/common/replacefile/UploadModeBottomSheet.kt` |
| Eligibility filter | `bottomNav/common/replacefile/ReplaceableMessages.kt` |
| Picker adapter | `bottomNav/common/replacefile/ReplaceTargetAdapter.kt` |
| Result / error bus | `bottomNav/common/replacefile/ReplaceFileEvents.kt` |
| Gate (Home chat) | `bottomNav/home/Home.kt` → `startUpload()` |
| Off-screen entry | `bottomNav/common/replacefile/UploadModePrompt.kt` |
| Layout | `res/layout/bottomsheet_upload_mode.xml` |

## Locators

All ids are on the app package, i.e. `byAppId("umTitle")` →
`com.zillit.zillitapp.qa:id/umTitle`.

| Element | id | Text |
|---|---|---|
| Title | `umTitle` | How are you uploading this document? |
| Continuation card | `umContinuation` | Continuation / Add alongside existing messages in the chat. |
| New card | `umNew` | New / Replace all previous messages with this new upload. |
| Replace card | `umReplace` | Replace / Swap one existing document. It moves to History with its comments. |
| Picker block | `umPickerBlock` | label: Document to replace |
| Search field | `umTargetInput` | hint: Search documents |
| Field wrapper | `umTargetLayout` | — |
| Selected stamp | `umTargetMeta` | `Selected: 31 Aug 2026, 19:25` |
| Cancel | `umCancel` | Cancel |
| Send | `umSend` | Send |
| Dropdown row | `rtName` / `rtMeta` | file name / timestamp |

Second confirmation (New only): title **Alert**, body *"Doing this will send all
current data, posted here, to the History. It will be replaced with the new
upload. Do you still want to proceed?"*, buttons **No** / **Yes**.

Stale-target error: *"That document is no longer available — it may have been
replaced or deleted. Please choose another."* (server key
`unit_chat_replace_target_not_found`).

## Preconditions

- Logged in, inside a project, **Call Sheet** unit, with posting rights.
- The Call Sheet picker is restricted to **documents only** (`PICKER_ITEM_DOCUMENT`)
  — no camera or gallery entry, so every case uses a PDF/doc fixture.
- Fixtures already pushed by `prep_device`: `/sdcard/Download/zl_dd_doc.pdf`,
  `zl_dd_word.docx`, `zl_dd_book.xlsx`.

---

## A. When the sheet appears (gating)

`startUpload()` asks only when `identifier == HOME_UNIT_CALL_IDENTIFIER` **and**
`currentList.size > 0`.

| # | Case | Steps | Expected |
|---|---|---|---|
| A1 | Empty Call Sheet uploads with no prompt | Clear/open a Call Sheet unit with zero messages → attach a document → Send | Upload starts immediately; sheet never appears |
| A2 | Call Sheet with existing messages shows the sheet | Unit has ≥1 message → attach a document | Sheet appears with title `um_title` |
| A3 | Other Home units never show the sheet | In Bulletin (or any non-Call-Sheet unit) with messages → attach a document | No sheet; upload starts immediately |
| A4 | Prompt precedes the upload, not follows it | Attach → observe | No upload progress row appears until Send is pressed |

## B. Initial state

| # | Case | Expected |
|---|---|---|
| B1 | Title is shown | `umTitle` = "How are you uploading this document?" |
| B2 | Continuation and New are always offered | Both `umContinuation` and `umNew` displayed with their body text |
| B3 | Send is disabled before any choice | `umSend.enabled == false` (layout sets `android:enabled="false"`) |
| B4 | Picker is hidden before any choice | `umPickerBlock` not displayed |
| B5 | Replace is offered when eligible documents exist | Unit holds ≥1 live document → `umReplace` displayed |
| B6 | **Replace is hidden when nothing is eligible** | Unit holds only text/images → `umReplace` NOT displayed, Continuation and New still are |

## C. Choosing a mode

| # | Case | Steps | Expected |
|---|---|---|---|
| C1 | Continuation enables Send | Tap `umContinuation` | Send enabled; `umPickerBlock` stays hidden |
| C2 | New enables Send | Tap `umNew` | Send enabled; picker stays hidden |
| C3 | Replace reveals the picker | Tap `umReplace` | `umPickerBlock` displayed; Send enabled (a target auto-selects) |
| C4 | Replace auto-selects the newest document | Tap `umReplace` | `umTargetInput` pre-filled with the most recent eligible file name; `umTargetMeta` shows `Selected: <dd MMM yyyy, HH:mm>` |
| C5 | Only one card is active at a time | Tap Replace, then Continuation | Picker hides again; selection moves to Continuation (stroke highlight follows) |
| C6 | Switching back to Replace keeps the earlier target | Replace → Continuation → Replace | Previously selected target still in the field (`selectedTargetId` is not cleared) |

## D. "Document to replace" picker

Adapter filters **substring, case-insensitive**, `threshold = 0`.

| # | Case | Steps | Expected |
|---|---|---|---|
| D1 | Dropdown opens on tap with no typing | Tap `umTargetInput` | Full list of eligible documents shown |
| D2 | Dropdown opens on focus | Focus the field | List shown |
| D3 | Rows show name and timestamp | Open dropdown | Each row has `rtName` (file name) and `rtMeta` (`dd MMM yyyy, HH:mm`) |
| D4 | Newest first | Open dropdown | Order is descending by `updated ?: created` |
| D5 | **Substring search, not prefix** | Type a fragment from the MIDDLE of a file name | The file still matches (prefix-only would fail — this is the point of the custom filter) |
| D6 | Search is case-insensitive | Type the name in the wrong case | Still matches |
| D7 | Clearing the query restores every option | Type a filter, then clear it | Full list returns, not the last filtered set |
| D8 | Picking a row writes the clean name | Tap a row | Field shows the file name only — never a `HomeChatInfo(...)` dump |
| D9 | Picking updates the stamp | Tap a row | `umTargetMeta` = `Selected: <that row's timestamp>` |
| D10 | Half-typed text cannot stand in for a choice | Type junk, dismiss the dropdown without picking | Field reverts to the actual selection; Send still refers to the selected id |
| D11 | Dropdown closes after a pick | Tap a row | Dropdown closes and stays closed (no re-filter loop) |
| D12 | No match leaves nothing selectable | Type a string matching nothing | Dropdown shows no rows. **Note:** `um_no_match` ("No matching document") is defined in strings but never used in code — there is no visible empty state. Treat as a defect, not a passing case. |

## E. Sending

| # | Case | Steps | Expected |
|---|---|---|---|
| E1 | Continuation posts alongside | Continuation → Send | Sheet closes; file posts; **previous messages remain**; `replacePreviousChats = false`, no `replace_chat_id` |
| E2 | New asks a second time | New → Send | **Alert** dialog with the "sent to the History" message and No/Yes |
| E3 | New → Yes wipes and posts | New → Send → Yes | Unit cleared of previous messages; new file posted; `replacePreviousChats = true` |
| E4 | New → No aborts | New → Send → No | Dialog closes, **nothing uploaded**, previous messages intact |
| E5 | Replace swaps one document | Replace → pick a target → Send | New file posts; the chosen document disappears from the list; other messages untouched |
| E6 | Replace sends no second confirmation | Replace → Send | No Alert dialog (only New has one) |
| E7 | Replaced document moves to History with comments | After E5, open History | The replaced file is there, with its comments |
| E8 | Send stays blocked without a target | Replace, clear the field so nothing is selected | `umSend.enabled == false` |

## F. Cancelling

| # | Case | Steps | Expected |
|---|---|---|---|
| F1 | Cancel aborts the upload | Attach → Cancel | Sheet closes; **picked files are dropped**; nothing posted |
| F2 | Cancel after choosing Replace | Replace → pick target → Cancel | Nothing posted |
| F3 | Back / swipe-down dismiss behaves like Cancel | Attach → press Back (or swipe the sheet down) | Nothing posted |
| F4 | Re-attaching after Cancel shows a fresh sheet | Cancel → attach again | Sheet reopens with nothing selected and Send disabled |

## G. Eligibility filter (what may be replaced)

From `ReplaceableMessages.eligible()` — a target qualifies only if **all** hold:
document/pdf type, has attachment media, not deleted, not archived, has a server `_id`.

| # | Case | Setup | Expected |
|---|---|---|---|
| G1 | Documents are eligible | Unit holds a PDF | Appears in the picker |
| G2 | `document` and `pdf` both count | One of each type | Both appear |
| G3 | Images are not eligible | Unit holds images only | Replace card hidden |
| G4 | Text messages are not eligible | Unit holds text only | Replace card hidden |
| G5 | Deleted documents excluded | Delete a document, reopen the sheet | It is gone from the picker |
| G6 | **Archived documents excluded** | Replace a document once, then attach again | The already-replaced file is NOT offered again (it is in History; offering it would send an id that can only fail) |
| G7 | Unsent documents excluded | Attach while offline so the message has no server `_id` | Not offered as a target |
| G8 | Name falls back sensibly | Document whose attachment has no name | Row shows caption, else translation, else the id — never blank |

## H. Errors and concurrency

| # | Case | Setup | Expected |
|---|---|---|---|
| H1 | Stale target is reported, not silently dropped | Device A opens the sheet and picks document X; device B deletes/replaces X; A presses Send | Snackbar *"That document is no longer available…"*, list refreshes, **nothing posted** |
| H2 | Retry after a stale target works | After H1, reopen and pick a live document | Replace succeeds |
| H3 | Other viewers see the replacement | A replaces a file; watch device B | B's list drops the replaced row (socket path) |

Two-device cases: `ZILLIT_SENDER_UDID` / `ZILLIT_RECEIVER_UDID`, as in the Drive suites.

## I. Other entry points (same sheet)

The sheet is also shown from Share, Forward and the Call Sheet Approvals screen,
where there is **no on-screen list** — eligible targets come from the local Realm
cache (`getUnitMessagesForReplace`).

| # | Case | Expected |
|---|---|---|
| I1 | Share into a Call Sheet unit shows the sheet | Same three options |
| I2 | Forward into a Call Sheet unit shows the sheet | Same three options |
| I3 | Approvals upload shows the sheet | Same three options |
| I4 | **Never-opened unit hides Replace** | Share into a Call Sheet unit never opened on this device → cache is empty → Replace card hidden; Continuation and New still work |

## J. Robustness

| # | Case | Expected |
|---|---|---|
| J1 | Rotation keeps the sheet usable | Rotate with Replace selected → sheet still shows the picker and the selection |
| J2 | Backgrounding and returning | Sheet is still there, nothing posted |
| J3 | Multi-file selection | Pick 2 documents → one sheet, choice applies to the batch |
| J4 | Long file names | Picker row and field do not clip into unreadability |

---

## Notes for whoever automates this

1. **`assumeTrue` the preconditions, don't fail them.** A6/B6/G-series depend on
   what the unit already holds. Follow `DocDistBaseTest`: skip with a clear
   reason instead of reporting a red failure for a data-shaped gap.
2. **Seed the unit first.** Most cases need ≥1 live document already present, so
   a `@Order(1)` case that uploads one via Continuation makes everything after it
   deterministic.
3. **Assert on state, not on toasts.** Android 12+ hides toasts from automation.
   For E4/F1 assert "no new bubble appeared and the sheet closed", and for H1
   assert the snackbar text via its TextView, not a toast.
4. **`umSend.enabled` is the cheapest real assertion in the whole sheet** — it
   encodes the entire selection state machine (B3, C1–C3, E8).
5. **Two genuine defects to raise before automating**, so nobody writes a test
   that enshrines them:
   - `um_no_match` is defined but never referenced — an empty search shows no
     message at all (D12).
   - The picker auto-selects the newest document the moment Replace is tapped
     (`selectMode`), so a user who taps Replace and immediately taps Send
     replaces a file they never consciously chose. Worth confirming that is
     intended before writing E5 as a pass.
