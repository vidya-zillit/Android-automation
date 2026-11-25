# Document Distribution — web vs Android gaps

Sources: web `web-e2e/document-distribution.md` (Zillit E2E Report of 2026-11-17: 101 web tests and 36 sheet bugs), compared with the Android Appium suites in `Android-automation/.../tests/docdist/` (18 suites) and the app source in `Zillit-Android/app/src/main`. Written 2026-11-23. Read-only comparison: nothing was run.

Path shorthands: **APP** = `Zillit-Android/app/src/main/java/com/zillit/zillitapp/docdistribution/`, **RES** = `Zillit-Android/app/src/main/res/`, **STR** = `RES/values/strings_docdist.xml`, **T** = `Android-automation/src/test/java/com/zillit/automation/tests/docdist/`. Web tests are cited by their line in `document-distribution.md` (`L123`).

## Summary

| Classification | Web tests (101) | Unique scenarios (reruns folded, 93) |
|---|---|---|
| COVERED | 35 | 32 |
| MISSING | 54 (12 of them partial: we have a case, but it stops short of the web's expected outcome) | 49 |
| WEB-ONLY | 11 | 11 |
| CONFLICT | 1 | 1 |
| **Total** | **101** | **93** |

"Reruns" are the second listings at L390–L406 (H07, T02/04, C07, C06, P03, R01, R02, R99) and the not-run R99 at L580. Each takes the same class as its first listing.

**Sheet bugs (36), by Android applicability (from the app source, not from the sheet's Android column)**

| Applies on Android? | Bugs | Count |
|---|---|---|
| Yes, bug present in source; no Android case catches it | DD-04, 05, 07, 08, 09, 10*, 11, 14 (Publish half), 16, 18, 19, 20, 22, 23, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36 | 25 |
| Yes, and an Android case already catches it | DD-25 (`B-12` fails on the bug, as intended) | 1 |
| Yes, but our case **asserts the buggy behaviour** | DD-24 (see CONFLICT) | 1 |
| Product decision pending (no × on Android) | DD-15 | 1 |
| Android not affected (source differs); regression guard worth adding | DD-06 (search trims), DD-17 (one snack only, but reads "Moved 1 items") | 2 |
| Android not affected and already covered | DD-13 (`E-1/E-4/E-5/E-6` asserts Attach disabled at 0) | 1 |
| Not applicable on Android (no such feature or flow) | DD-01, 02, 03, 12, 21 | 5 |

\* DD-10 is likely but not confirmed: the Preview WebView has JS off by default (`APP/ui/composer/EmailComposerFragment.kt:973`, no `javaScriptEnabled` anywhere in `ui/composer`), but loading the template pushes its HTML into the RichEditor WebView, which runs JS (`ComposerEvent.LoadBody`, `EmailComposerViewModel.kt:422`). So an `onerror` handler can fire in the editor rather than in Preview.

---

## CONFLICT

### C1. Closing the composer with typed content: our F-26 treats "no prompt" as correct
- **Web**: "closing the composer with a typed subject asks first or keeps the draft" (L167, sheet-bugs/dd.spec.js:1127). Expected: a discard prompt, or the draft kept for next time. Sheet bug **DD-24** (Medium) lists Android as *In progress*: the composer closes at once and Compose reopens blank.
- **Ours**: `DocDistComposerTest` "F-26: Cancel leaves the composer (current build: straight popBackStack, no confirm)" (`T/DocDistComposerTest.java:347-385`). It dirties the draft, taps Cancel, accepts any confirm that does appear, and asserts only that the composer closed and the library is showing. It never checks that the typed data survives or that a prompt appeared. So it passes on the buggy build, and its comment records the bug as "source truth".
- **Android source**: `goBack()` → `leave()` → `popBackStack()` with no dirty check (`APP/ui/composer/EmailComposerFragment.kt:1140-1152`; Cancel at `:605`, system Back at `:167`). The bug is present.
- **Which is right**: the web/sheet expectation (DD-24 is an accepted bug). **Fix**: rewrite F-26 as "Cancel/Back with a typed subject either shows a discard prompt or Compose reopens with the subject". Type a subject (`dd_subject`) and add a chip (`dd_to_chips`), then tap `dd_cancel`. If a dialog appears, assert it and decline. Otherwise reopen `dd_btn_compose` and assert that `dd_subject` still holds the text. Expect a FAIL on the current build (known bug DD-24). Keep a separate "Cancel on an *empty* composer leaves at once" case for the current happy path.

---

## MISSING

Priority: **H** = data loss or a blocked task, **M** = a wrong result the user can see, **L** = wording or polish. The offline cases can reuse the `setConnectivity(false/true)` pattern from `DocDistRegressionTest` P-1 (`T/DocDistRegressionTest.java:174-200`). No case needs a server stub unless it says so.

### M1 [H]. "Move here" is pushed off-screen when the folder list is long. Sheet DD-23.
- Web: "Move here stays on screen when the folder list is long" (L77, dd.spec.js:672).
- Android applies: the Move sheet stacks `dd_recycler` (`wrap_content`) above `dd_move` ("Move here") with nothing pinning the button (`RES/layout/bottomsheet_dd_move.xml:33-47`). With many folders the button falls below the sheet.
- **Proposed case**: `DocDistFilesTest` "MOVE-1: Move here stays visible with a long folder list". Steps: open a folder with the fixture PDF, tick its checkbox, then `dd_selection_actions` → "Move" (`dd_action_move`). Assert that `dd_move` ("Move here") is displayed. Scroll `dd_recycler` to the end and assert that `dd_move` is still displayed and inside the screen bounds. Then cancel. Expected: the button is always reachable. Run it on a project with 20+ folders (the shared dev project qualifies), and assume-skip when fewer than one screen of folders exists.

### M2 [H]. A search that hides ticked files still deletes them. Sheet DD-11.
- Web: "a search unticks the files it hides, so Delete removes only what is listed" (L45, dd.spec.js:535).
- Android applies: `setSearchQuery` only rebuilds the visible list (`APP/ui/library/LibraryViewModel.kt:364-367`). `selectedFileIds` are never intersected with the filtered rows (pruning happens only on reload, `:338`). The bar keeps "2 selected" (`dd_selection_count`), and Delete sends both ids (`LibraryFragment.kt:594-596`).
- **Proposed case**: `DocDistFilesTest` "SEL-SEARCH: a search narrows the selection to what is listed". Setup: a folder with `sample.pdf` and `sample.docx` (fixtures exist, `D-1b`). Steps: tick both and assert that `dd_selection_count` reads "2 selected". Type `docx` in `dd_search_input`, then assert that only the docx row is listed and the count reads "1 selected". Do **not** confirm Delete: open `dd_selection_actions` → Delete and read the confirm text only, then decline. Expected: the count follows the visible rows. Known bug: it stays at 2.

### M3 [M]. Real move of a document and a folder, with one singular success message. Web F06, D08 and bug test L118. Sheet DD-17 (Android not checked).
- Web: "DD-F06: move folder into another folder" (L468), "DD-D08: move a document to another folder" (L486), "a move shows one success message, not two" (L118).
- Ours: `D-13: Move opens the folder chooser sheet` (`T/DocDistFilesTest.java:201`) only opens the sheet. No case ever moves anything.
- Android: on success the sheet sets one fragment result, and the library shows one snack, `dd_moved_ok` = "Moved %1$d items" (`LibraryFragment.kt:122`, `STR:453`). That is a single message (DD-17 is not present), but it reads "Moved 1 items", which is not plural-aware.
- **Proposed cases** (`DocDistFoldersTest`/`DocDistFilesTest`):
  - "MOVE-2: a document moves to another folder". Folders A and B, with the PDF in A. Tick the PDF, Move, tap B in `dd_recycler`, then tap `dd_move`. Assert that exactly one snackbar appears, reading "Moved 1 item" (expect "Moved 1 items" today: file it as a wording bug). The PDF is gone from A; open B (via the root) and assert that the PDF is listed there.
  - "MOVE-3: a folder moves into another folder". Tick folder X at the root, Move into Y. Assert that X has left the root, and open Y to assert that X is listed inside it.

### M4 [M]. Lowered rights for a second account: no Post, or no View. Web R01, R02 and R99 (×2 each: L370, L377, L402, L404, L406, L580).
- Ours: `O-1/O-2/O-3` run with the admin account only. `O-5` and `O-6` are `assumeTrue(false)` placeholders (`T/DocDistRightsTest.java:209-230`).
- Android design differs from web: controls stay **visible and enabled**, and the click is gated by `runWithPostingRights` / `runWithDownloadRights`, which open the request-rights dialog (`LibraryFragment.kt:252-253, 448-460, 782-787`; publish banner `dd_request_rights_banner`). So the Android case must assert the dialog, not disabled buttons.
- **Proposed cases** (two-device harness as in `DriveTwoDeviceE2ETest`, or a second login):
  - "O-7: without Post, every write asks for rights; browse and download still work". As admin, in User Management turn off the second user's Admin switch and Document Distribution Post (View and Download on). As the second user, open Doc Distribution. Tapping `dd_btn_compose`, `dd_btn_create_folder`, a folder's Send and a file's Delete each shows the request-rights dialog (the `REQUEST_DIALOG_MARKERS` already in `DocDistRightsTest`), declined each time. A file row's Download proceeds with no dialog. Templates, Lists and Contacts open.
  - "O-8: without View, the tool is hidden". Turn View off. The Document Distribution tile is absent from Tools (`DocDistRights.canView`).
  - Teardown (= R99): restore the rights and the Admin switch, then assert that both are back as they were.

### M5 [M]. Loading a template over a written message, and the signature. Web L191, L198, C06 (L331, rerun L396). Sheet DD-20.
- Android applies: `applyTemplate` overwrites the subject and body with no prompt (`APP/ui/composer/EmailComposerViewModel.kt:420-423`). The auto-inserted signature (one-shot `hasInsertedSignature`, `:183-188`, `:434-445`) is lost. The Android "Signature" button opens the manage screen (`SignatureListActivity`, `EmailComposerFragment.kt:596-597`), not an "insert this signature" picker, so the web's "Signature appends the one you pick" half is web-only.
- **Proposed cases** (`DocDistTemplatesTest`, after `H-5` has made a QA template):
  - "H-10: a new compose starts with the saved signature". Open `dd_btn_compose` and assert that `dd_editor` contains the account's default signature text (falls back to "Sent from Android" when none is saved, `DEFAULT_SIGNATURE`).
  - "H-11: loading a template over a typed message asks first". Type `QA-NOTE-<ts>` into `dd_editor`, then `dd_btn_templates` → the QA template. Expected: a replace/keep prompt. Known bug: none appears, and the subject and body are replaced.
  - "H-12: a loaded template keeps the signature at the end". After loading, `dd_editor` still ends with the signature. Known bug: it is dropped.

### M6 [M]. Preview's To line lists an invalid address. Web L173. Sheet DD-26.
- Android applies: `formatPreviewRecipients(state.to)` joins every chip, valid or not (`EmailComposerFragment.kt:984-992`), while the footer counts only valid ones.
- Ours: `F-2/F-3/F-6` asserts that the invalid chip is kept, and `F-22/F-23` asserts that Preview shows the recipient. Neither checks the invalid one is excluded.
- **Proposed case**: `DocDistComposerTest` "F-27: Preview's To lists only valid addresses". Add `TO_1` and `notanemail` (two chips in `dd_to_chips`), type a subject, then tap `dd_preview_btn`. Assert that `dd_preview_to` contains `TO_1` and **not** `notanemail`. Then tap `dd_edit` and Cancel.

### M7 [M]. Composer headings: "Distribute folder" only for a whole folder. Web L178 and F10 (L270). Sheet DD-28.
- Android applies: the heading is "Distribute folder" whenever `folderPath` is set (`EmailComposerFragment.kt:159-163`). `openComposer()` passes the open folder's name by default, so Compose and Share documents inside a folder get it too (`LibraryFragment.kt:252-253, 626-634, 956-957`).
- **Proposed cases** (`DocDistComposerTest`):
  - "F-28: a folder row's Send opens 'Distribute folder' with its files attached". At the root, tap the folder row's send icon (`dd_cd_send`). Assert the toolbar title "Distribute folder" and at least one row in `dd_attachments_recycler`.
  - "F-29: Compose and Share documents inside a folder are titled 'Compose email'". Inside the folder, tap `dd_btn_compose` and assert "Compose email" (known bug: "Distribute folder"). Cancel, then tick the PDF → `dd_selection_actions` → "Share documents" (`dd_action_share`), and assert "Compose email" again.

### M8 [M]. Library search: nested results from the root, and a trailing space. Web L64, L33. Sheet DD-18 (applies), DD-06 (Android not affected).
- Android: the search is a client-side filter over the open folder's loaded rows, trimmed (`LibraryViewModel.kt:794-806`). At the root, therefore, files (which always live in folders) and subfolders can never match. That is DD-18. Trimming means DD-06 is fine.
- **Proposed cases** (`DocDistNestedFoldersTest`, which already builds a 3-level tree):
  - "NEST-8: a root search finds a nested file and a nested folder". At the root, search the deepest PDF's name, then the level-2 folder's name. Expected: both listed. Known bug: "No matching files or folders." (`dd_empty_text`).
  - "B-14: a trailing space is ignored". Inside the folder, search `pdf ` (with the space) and assert that the PDF row is listed. This is a regression guard.

### M9 [M]. A search that hides every file hides the folder's Publish/Distribute. Web L27. Sheet DD-05.
- Android applies: `hasFiles` is computed from the *filtered* items, so `dd_folder_header_actions` goes and `dd_folder_header_empty` ("Upload files to enable distribution", `STR:862`) shows (`LibraryFragment.kt:461-464`).
- **Proposed case**: `DocDistFilesTest` "EXTRA-2: a no-match search keeps the folder's Publish and Distribute". Inside the folder with the PDF, type `zzz-nomatch-<ts>` in `dd_search_input`. Assert that `dd_empty_text` shows "No matching files or folders.", **and** that `dd_folder_publish` and `dd_folder_distribute` are still displayed, **and** that `dd_folder_header_empty` is not.

### M10 [M]. Leaving a distribution list with unsaved members. Web L217. Sheet DD-16.
- Android applies: the toolbar nav and `dd_cancel` both just `dismiss()` (`APP/ui/preset/PresetEditorDialog.kt:171-172`). A CSV import already snacks "Added N recipients from CSV" before anything is saved (`:212`).
- **Proposed case**: `DocDistPresetsTest` "G-11: leaving a list with unsaved members asks first". Open New (`dd_new`) and name it. Add a member via `dd_in_email`/`dd_in_name` → `dd_add`, without saving. Tap the toolbar back. Expected: an unsaved-changes prompt. Known bug: it closes, and reopening shows 0 people.

### M11 [M]. Duplicate names for a distribution list and an email template. Web L223, L235. Sheet DD-19, DD-31.
- Android applies: neither `PresetEditorViewModel.save` (`APP/ui/preset/PresetEditorViewModel.kt:211-238`) nor the template editor checks for an existing name.
- **Proposed cases**: `DocDistPresetsTest` "G-12" and `DocDistTemplatesTest` "H-13". Create `QA_LIST_<ts>`, then New again with the same name and Save. Expected: an error, and one row with that name in `dd_recycler`. Known bug: two rows. Clean up both rows in `finally` (the `QA_*` sweeper in `DocDistCleanupTest` is the backstop).

### M12 [M]. A History record that fails to load. Web L241. Sheet DD-08.
- Android applies: `DistributionDetailViewModel` stores `error` (`APP/ui/history/DistributionDetailViewModel.kt:86`), but the fragment never renders it. It only hides `dd_progress` (`DistributionDetailFragment.kt:222`), so the user sees a blank detail with no Retry.
- **Proposed case**: `DocDistHistoryTest` "K-11: a detail that fails to load says so and offers Retry". Open History, turn connectivity off, then tap row 1. Expected: an error text and a Retry control, and the toolbar Back returns to the list. Restore the network in `finally`.

### M13 [M]. Offline errors are plain and keep the user's input: Create folder, Move, Watermark download. Web L105, L111, L137. Sheet DD-35, DD-04, DD-17 (failure half).
- Android applies: raw `e.message` is shown. Create folder: `CreateFolderBottomSheet.kt:108`. Move: `MoveToFolderBottomSheet.kt:67`. Watermark: `WatermarkDownloadViewModel.kt:331`. Offline, that message is `Unable to resolve host "documentdistributionapi-dev.zillit.com"…`. `DocDistApiErrorLabel` exists but is not used on these paths.
- **Proposed cases** (`DocDistRegressionTest`, alongside P-1):
  - "P-10: Create folder offline shows a plain error and keeps the sheet". Open `dd_btn_create_folder`, fill `dd_name` and the date, go offline, then tap `dd_create`. Assert that the snackbar text contains no `http`, `.zillit.com` or `Unable to resolve host`, that the sheet is still open, and that `dd_name` keeps the text.
  - "P-11: a Move that fails offline shows an error and no 'Moved'". The file stays in its folder.
  - "P-12: Watermark → Download offline shows a plain error and saves nothing". Use `dd_download` in the watermark sheet. No "saved" confirmation appears.

### M14 [M]. Contacts that fail to load. Web L255. Sheet DD-36.
- Android applies: on failure the VM sets `error`, but the screen shows `dd_empty` = "No contacts yet." (`APP/ui/contacts/AddressBookViewModel.kt:104`, `AddressBookFragment.kt:245-253`). There is no Retry, and it stays that way after reconnecting.
- **Proposed case**: `DocDistContactsTest` "I-15: a failed load shows an error with Retry, not 'No contacts yet.'" Go offline, open `dd_btn_contacts`, and assert that `dd_empty` does not read "No contacts yet." and that a Retry is shown. Go back online and tap Retry (or swipe `dd_swipe`); assert that rows load.

### M15 [M]. Edit folder with a blank name. Web L39. Sheet DD-07.
- Android applies: the positive button returns early on a blank name, but `MaterialAlertDialog` has already dismissed. There is no message (`LibraryFragment.kt:759-762`).
- **Proposed case**: `DocDistFoldersTest` "C-11: Edit with a blank or spaces-only name is refused and stays open". Folder ⋮ → Edit, clear the name (then try spaces only), and tap Save. Expected: the dialog stays open with an error, or Save is disabled. Known bug: it closes silently. Assert that the folder keeps its name.

### M16 [L]. Counts and wording: "1 documents", "1 people", "any one of the module", "this send only". Web L52, L143, L229. Sheet DD-14, DD-22.
- Android applies:
  - Publish header: `"Publish documents · " + dd_publish_n_documents` = "%1$d documents" (`APP/ui/publish/PublishBottomSheet.kt:141`, `STR:284`).
  - List row: `dd_n_people` = "%1$d people" (`PresetOverviewAdapter.kt:119`, `STR:875`).
  - Publish heading: "Publish in any one of the module below *" (`STR:878`, used at `RES/layout/bottomsheet_dd_publish.xml:36`).
  - Watermark note: "…applies to this send only." is also shown in the download sheet (`STR:722` via `WatermarkAppearanceBinder.kt:143`).
  - The date header is fine: it uses the `dd_items` plural (`LibraryAdapter.kt:113-116`).
- **Proposed cases**:
  - `DocDistPublishTest` "N-15: one file reads '1 document' and 'Publish in one of the modules below'". Open Publish on one PDF, read `dd_title` and the heading.
  - `DocDistPresetsTest` "G-13: a one-member list reads '1 person'". Read `dd_count`.
  - `DocDistWatermarkTest` "M-13: the download sheet's note says 'this download only'".
  - `DocDistFilesTest` "D-14: a one-file date group reads '1 item'". This is a guard, expected to pass.

### M17 [L]. The help card's collapsed state does not carry into folders. Web L89. Sheet DD-29.
- Android: the card collapses rather than dismisses. `helpExpanded = true` is per fragment instance, and every folder is a new `LibraryFragment` (`LibraryFragment.kt:187-206`).
- **Proposed case**: `DocDistributionTest` "A-7: a collapsed help card stays collapsed in a folder and back at the root". Tap `dd_help_header`, assert that `dd_help_body` is gone, then open a folder and assert that it is still gone. Do the same after Back.

### M18 [L]. Accessibility labels in the library. Web L94, L99. Sheet DD-34.
- Android applies: the list toggle's `contentDescription` is `@string/dd_action_history` (`RES/layout/fragment_library.xml:152-156`). The home crumb is `dd_cd_back` (`:413-417`). Every row checkbox is `dd_select_all` (`item_dd_library_file.xml:33`, `item_dd_library_folder.xml:33`).
- **Proposed case**: `DocDistGridViewTest` "A11Y-1: the toggles, home crumb and row checkboxes are named for what they do". Read the `content-desc` of `dd_view_list` (expect "List view"; today "History") and of `dd_view_grid`. Read the content-desc of `dd_crumb_home` (expect it to mention home or root). The first file row's checkbox should not read "Select all".

### M19 [L]. Move and Attach can't tell same-named items apart. Web L125. Sheet DD-30.
- Android applies: Move rows show the name only (`APP/ui/move/MoveFolderAdapter.kt:28`). Picker rows show name, size and date with no folder path (`APP/ui/documentpicker/DocumentPickerAdapter.kt:26-27`).
- **Proposed case**: `DocDistNestedFoldersTest` "NEST-9: same-named subfolders are distinguishable in Move and Attach". Make A/dup and B/dup, each holding the same PDF. In Move, the two dup rows must differ (path or indent). In the Compose → `dd_btn_attach_library` picker, the two PDF rows must show their folder.

### M20 [L]. "Watermark PDF and download" with a non-PDF ticked. Web L150. Sheet DD-33.
- Android applies (per the sheet). The batch screen (`APP/ui/watermark/WatermarkBatchFragment.kt`) has no PDF filter. The single-doc sheet does have `dd_unsupported`.
- **Proposed case**: `DocDistWatermarkTest` "M-14: a ticked .docx is left off the batch, or marked as skipped". Tick the PDF and the docx → `dd_action_watermark_download`. Either the docx chip is absent, or a "skipped / not supported" text shows. Do not download.

### M21 [L]. A template's stored script must not run. Web L184. Sheet DD-10 (High on web).
- Android: Preview is a plain `WebView` (`fragment_email_composer.xml:566`, JS off by default). The RichEditor that receives the template HTML runs JS, though (see the footnote at the top).
- **Proposed case** (needs the template seeded through the templates API, or created in the web app). `DocDistTemplatesTest` "H-14: a template with `<img src=x onerror=…>` never executes". The handler should write a visible marker, e.g. `this.insertAdjacentText('afterend','XSS-RAN')`. Load the template in the composer, then Preview. Assert that "XSS-RAN" appears in neither `dd_editor` nor `dd_preview_body`. Delete the template afterwards.

### M22 [L]. A signature with a name but no text. Web L205. Sheet DD-32.
- Android applies (shared with the Email module): `EditSignatureActivity.saveSignature()` validates only the name, then creates and `finish()`es whatever the content is (`APP/../bottomNav/new_email/ui/settings/EditSignatureActivity.kt:131-150`).
- **Proposed case**, best in the email suite since the screen is shared: "a signature with an empty body is refused and the form stays open". Composer `dd_btn_signature` → add, type a name, leave the body empty, Save. Expected: an error, and the form stays open. Known bug: it closes and nothing is created. Never edit an existing signature.

### M23 [L]. Contacts Export CSV has no UTF-8 BOM. Web L248. Sheet DD-09.
- Android applies **by design comment**: "UTF-8. No BOM (web doesn't write one)" (`APP/util/AddressBookCsvBuilder.kt:21`). The History recipients export does write one (`DistributionRecipientsCsvBuilder.kt:18,47`).
- **Proposed case**: extend `I-13` (`T/DocDistContactsTest.java:527`). After the save, `driver.pullFile(<saved path>)` and assert that the first bytes are `EF BB BF`, and that an accented QA contact name round-trips.

### M24 [M]. A distribution list's per-row CSV download. Web L337.
- Android has it: the list row's `dd_export` (content-desc `dd_cd_export_csv` "Export list as CSV", `RES/layout/item_dd_preset.xml:72-80`).
- **Proposed case**: `DocDistPresetsTest` "G-14: Export list as CSV saves the members". On the QA list row, tap `dd_export`, then accept the system save dialog (same pattern as `K-9`/`I-13`). Assert the app's confirmation, and if possible pull the file and check its header plus the member's email.

### M25 [M]. CSV import into a list: valid rows added, invalid and duplicate rows flagged. Web L343.
- Android has it: `dd_import_csv` → `CsvImportBottomSheet` with `dd_choose_file`, `dd_summary` ("%d rows · %d ready · %d duplicates · %d invalid", `STR:776`), per-row `dd_status_label` (Ready/Duplicate/Invalid, `STR:779-781`), and `dd_add` ("Add %d contacts", `STR:777`).
- Ours: `G-3` only asserts that the button exists.
- **Proposed case**: `DocDistPresetsTest` "G-15: CSV import adds 2 of 4 rows". Push a fixture CSV (two good rows, one `not-an-email`, one repeat) to `/sdcard/Download` and pick it via the system picker. Assert that `dd_summary` reads "4 rows · 2 ready · 1 duplicates · 1 invalid". Tap `dd_add` ("Add 2 contacts") and Save. Reopen the list and assert that both members are present, with the job kept.

### M26 [M]. Publish to Doc Distribution from Info. Web P03 (L355, rerun L398).
- Android has it: the Info message options offer "Publish to Doc Distribution" (`Zillit-Android/.../utils/OptionsHandler.kt:493-497`). It files into the `Info` folder (`APP/data/repository/DocDistTool.kt:36`; `bottomNav/home/Home.kt:1610-1630`), with a confirm and a "Published." popup (`RES/values/strings.xml:5042-5046`).
- Ours: only Call Sheet and Schedule assert that the *option is present* (`tests/callsheet/CallSheetTest.java:140`). Nothing publishes, and nothing checks the library.
- **Proposed case** (homechat/Info suite, or `DocDistPublishTest`): post a QA PDF in Info. Long-press it → "Publish to Doc Distribution" → confirm "Publish" → assert the "Published." popup. In Doc Distribution, open `Info`, search the file name, and assert the row. Delete the row and the Info message afterwards.

### M27 [M]. Live updates: History refreshes on a teammate's send, and a folder created elsewhere appears. Web H11 (L362), F11 (L540).
- Android has it: History reloads page 0 on `DistributionSent`/`DistributionOpened` (`APP/ui/history/HistoryListViewModel.kt:89-96`). Folder events exist (`APP/data/socket/DocDistSocketEvent.kt:20-28`).
- Ours: `P-9` is a "needs a second session" placeholder (`T/DocDistRegressionTest.java:222`).
- **Proposed case** (two-device harness): device B sends a QA distribution, or creates and deletes a `QA_RT_<ts>` folder. Device A, sitting on History or the library root, sees the row appear and disappear without a refresh.

### M28 [M]. History no-match text. Web "History search filters server-side and clears back" (L410). Sheet DD-27.
- Ours: `J-3` asserts that `dd_empty` shows and that zero rows are listed on a no-match search, but not its text (`T/DocDistHistoryTest.java:263-292`). Android shows the static `dd_history_empty` "No emails sent yet." (`STR:480`).
- **Proposed**: add to J-3 an assertion that, while a search is active, `dd_empty`'s text is not "No emails sent yet." (expect a "no matching…" text). It fails today (DD-27). Then clear the search and assert that rows return.

### M29 [M]. Publish to Info actually posts. Web P02 (L349).
- Ours: `N-9` is `@Disabled` because it writes into another module of a shared project (`T/DocDistPublishTest.java:515-517`). The web test stubbed the request, which the Android harness cannot do.
- **Proposed**: keep it blocked until there is a disposable project, or run it with cleanup: publish one QA PDF to Info (`dd_cat_info` → `dd_publish`). Assert the "Published 1 document" snack (today `dd_published_ok` reads "Published %1$d documents", `STR:287`, the same singular slip). Then delete the Info post.

### M30 [L]. Folder description on the row. Web F02 (L446). Partial.
- Ours: `C-4` checks the description only in the Edit dialog. Android also shows it in the row meta (`LibraryAdapter.kt:141`) and in the folder header (`dd_folder_header_desc`).
- **Proposed**: extend C-4 to assert that the folder row's meta text contains the typed description.

### M31 [M]. Bulk delete of two folders. Web F09 (L474).
- Ours: `D-11` checks only that the selection menu offers Delete.
- **Proposed**: `DocDistFoldersTest` "C-12: tick two QA folders → `dd_action_delete_selected` → confirm → both gone". Use `QA_*` names so the cleanup sweeper is the backstop.

### M32 [M]. Document lifecycle details. Web D02/05/06/07/09/10 (L480). Partial.
- Covered already: upload of every family (`D-1b`), preview (`D-3/4/5`), row delete (`D-7`). Missing:
  - (a) an actual **Download** from the row menu, with a "saved" confirmation;
  - (b) Watermark **disabled for a .docx** row (`FileTypeDetector.canWatermark`, `LibraryAdapter.kt:220-222`; the list-view menu item should be disabled or refuse);
  - (c) **bulk** Delete via the selection bar.
- **Proposed**: three small cases in `DocDistFilesTest`.

### M33 [L]. Sort order actually changes. Web D13 (L498). Partial.
- Ours: `B-7/B-8` asserts only that the four menu items exist.
- **Proposed**: create `QA_aaa_<ts>` and `QA_zzz_<ts>` and search `<ts>`. Pick `dd_sort_name_asc` and assert that aaa comes first; pick `dd_sort_name_desc` and assert that zzz comes first.

### M34 [L]. Save recipients as a list really creates it. Web H08 (L557). Partial.
- Ours: `K-8` opens the name prompt and cancels.
- **Proposed**: K-8b names the list `QA_FROMREC_<ts>` and confirms. Assert that it appears under Lists (`dd_btn_lists`) with the record's recipient count, then delete it.

---

## COVERED

| Web test(s) | Android test |
|---|---|
| DD shell renders: title, toolbar, library (L10) | `DocDistributionTest` A-1/A-2, A-4 (×2) |
| History drawer opens with the server total (L15) | `DocDistHistoryTest` J-1, J-2 |
| the breadcrumb home button leaves a nested folder (L83) | `DocDistFoldersTest` B-12 (fails on bug DD-25, as intended); `DocDistNestedFoldersTest` NEST-6 for Back. B-12 tests one level, not two |
| Attach from library with nothing ticked has a disabled Attach button (L162) | `DocDistComposerTest` E-1/E-4/E-5/E-6 (`assertFalse(picker.isAttachEnabled())` after Clear) |
| composer: recipient chip + subject reach the preview stage (L264) | `DocDistComposerTest` F-22/F-23, F-24/F-10 |
| DD-H07 Duplicate opens a prefilled composer (L276, L390) | `DocDistHistoryTest` K-7 |
| DD-C02 an invalid address alone does not enable Preview (L282) | `DocDistComposerTest` F-24/F-10 |
| DD-C03 Cc/Bcc toggle reveals both rows (L288) | `DocDistComposerTest` F-4/F-5 |
| DD-C08 Edit from preview returns with fields intact (L293) | `DocDistComposerTest` F-22/F-23 |
| DD-C04 attach a library file and remove it (L299) | `DocDistComposerTest` E-7/F-16/F-19 |
| DD-T02/04 template create, use in composer, delete (L305, L392) | `DocDistTemplatesTest` H-3, H-5, H-9, H-7 |
| DD-C07 a distribution list fills composer recipients (L312, L394) | `DocDistPresetsTest` G-10 |
| leftovers of the dd-ph1 run (L385) | `DocDistCleanupTest` (QA_* templates, lists, folders) |
| History paginates: 50 rows, scroll loads more (L415) | `DocDistHistoryTest` J-7 (`PAGE_LIMIT = 50`, `HistoryListViewModel.kt:231`) |
| History row opens the detail view and Back returns (L420) | `DocDistHistoryTest` J-8, K-1 |
| create a folder, open it, delete it (L425) | `DocDistFoldersTest` C-3, "3.11 answered", B-11, C-10 |
| upload a PDF into a folder, then delete doc and folder (L430) | `DocDistFilesTest` D-1, D-7 |
| DD-S02 info banner dismisses (L436) | `DocDistributionTest` A-6 (on Android the card collapses rather than dismisses) |
| DD-F04 empty name does not create (L451) | `DocDistFoldersTest` C-2 (plus C-2b: a date is also required on Android) |
| DD-F05 rename folder via ⋯ → Edit (L456) | `DocDistFoldersTest` C-8/C-9 |
| DD-F03+F08 subfolder create, cascade delete (L462) | `DocDistNestedFoldersTest` NEST-2, NEST-7 |
| DD-D11 library search filters and clears (L492) | `DocDistFoldersTest` B-5; `DocDistributionTest` B-5/B-6 |
| DD-D14 list ⇄ grid toggle (L504) | `DocDistributionTest` B-3; `DocDistGridViewTest` GRID-1, GRID-6 |
| DD-H05/H09 detail analytics and Refresh (L509) | `DocDistHistoryTest` K-2, K-3, K-6 |
| DD-H06 export recipients as CSV (L514) | `DocDistHistoryTest` K-9 |
| DD-L02/03/04 distribution list lifecycle (L519) | `DocDistPresetsTest` G-3…G-8 |
| DD-A02/03/04/05 contact lifecycle (L524) | `DocDistContactsTest` I-3, I-5, I-2, I-12 |
| DD-P01 Publish modal opens with destination cards (L529) | `DocDistPublishTest` N-1, N-2/N-3 |
| DD-H10 send a real distribution and see it in History (L535) | `DocDistSendTest` F-25/P-8, J-10/F-25 |
| DD-D12 date filter applies and clears (L551) | `DocDistributionTest` B-9/B-10 |
| DD-A03 edit a contact and see the change (L568) | `DocDistContactsTest` I-11 |
| DD-T03 edit a template and the list reflects it (L573) | `DocDistTemplatesTest` H-6 |

(35 web tests: the 32 rows above plus three reruns, H07, T02/04 and C07. The shell row counts once.)

## WEB-ONLY

| Web test(s) | Why it does not apply on Android (evidence) |
|---|---|
| a double Enter in Create Folder makes one folder (L22). Sheet DD-02 | Android creates from a bottom sheet with a `dd_create` button gated on name **and** date (`RES/layout/bottomsheet_dd_create_folder.xml`; `T` C-2b). There is no Enter-to-submit. The sheet also marks Android "Not affected". |
| the library search × clears the term (L59). Sheet DD-15 | There is no clear icon: `dd_search_layout` has no `endIconMode` (`RES/layout/fragment_library.xml:126-135`). The sheet leaves it to product; add a case if a × is added. |
| in a 700 px window the Name column keeps its width (L71). Sheet DD-21 | This is a browser-width layout. Android rows are a single-column list or grid (`item_dd_library_file.xml`). |
| Watermark PDF and share opens the composer watermarked, with no toggle advice (L131). Sheet DD-03 | Android deliberately opens the composer with watermark **off** and snacks "Toggle Watermark on each PDF before sending" (`LibraryFragment.kt:958-961`, `STR:83`). That advice is correct there, and the sheet marks Android "Not affected". The open itself is covered by `DocDistWatermarkTest` M-11. |
| unticking an attached file in Attach from library removes it (L156). Sheet DD-01 | The Android picker never pre-ticks already-attached files (`selectedIds` starts empty, `APP/ui/documentpicker/DocumentPickerViewModel.kt:21`), so there is nothing to untick. |
| Share some documents mails the members on screen (L211). Sheet DD-12 | The Android list editor has no "Share some documents" action (`RES/layout/dialog_dd_preset_editor.xml` ids: `dd_name, dd_description, dd_in_email, dd_in_name, dd_in_job, dd_add, dd_import_csv, dd_recipients_recycler, dd_stop_notif, dd_cancel, dd_save`). |
| DD-D04 the upload card names each file while it uploads (L319) | Android shows a generic busy overlay, "Uploading…" (`STR:770`, `dd_busy_uploading`), after an "Upload N files to this folder?" confirm (`STR:397-399`). There is no per-file "Uploading 1 of 2" card. |
| DD-D15 the library lists 50 files, then loads more on scroll (L325) | Android fetches **all** pages up front in a loop (`APP/data/repository/DocumentsRepository.kt:51-63`, `PAGE_SIZE = 50`). There is no scroll-triggered page and no "Loading more…" row. "Every row is reachable" is covered by `DocDistRegressionTest` P-5. |
| DD-S03 theme toggle flips data-theme (L441) | There is no theme toggle in the module: the `fragment_library.xml` toolbar ids have none, and the theme follows the system (`RES/values-night/colors_docdist.xml`). |
| DD-D03 drag-and-drop uploads into the open folder (L545) | There is no drop target: `dd_drop_title` ("Drop files to upload", `STR:86`) is unused in `APP/ui` and the layouts. |
| DD-H12 a legacy flat-array backend degrades to client-side history (L562) | This is a web client fallback driven by a stubbed response. Android History only calls the paged API (`limit = PAGE_LIMIT`, `HistoryListViewModel.kt:156,199`), and the Android harness cannot stub responses. |

---

### Notes for whoever implements these
- Several proposals are **expected to FAIL on today's build** because the bug is live in source: M2, M5 (H-11/H-12), M6, M7 (F-29), M8 (NEST-8), M9, M10, M11, M12, M13, M14, M15, M16, M17, M18, M19, M23, and C1. Give the failure messages the sheet ID (DD-xx) so the report reads as "known bug", the way B-12 already does.
- Two existing cases pass without proving the web's point and should be tightened: **F-26** (see C1) and **J-3** (M28: it asserts that the empty state shows, not what it says).
