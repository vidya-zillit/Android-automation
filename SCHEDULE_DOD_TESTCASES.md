# Schedule D.O.D. — test cases and findings

**Schedule D.O.D.** (day-out-of-days) hands out D.O.D. PDFs filed in **name folders**.
Tools → **Schedule D.O.D.** (identifier `dod_tool`) opens
`bottomNav/tools/dod/DodActivity` — a folder list over `{schedule-distribution}/dod-folders` —
and a folder opens `DodDetailActivity`, the files in it (`{schedule-distribution}/dod`).

It is a sibling of Schedule Distribution's **Pages** tab without the tabs: the same folder card
(`item_script_folder`), the same file card (`documents_script_item`), the same upload screen
(`UploadDocumentActivity`, branch `DOD_PAGES`), the same count screen (`UsersCountListPage`) and
History screen (`UserHistoryFolderPage`). A bug found here is worth checking in
[`SCHEDULE_DIST_MODULE_TESTCASES.md`](SCHEDULE_DIST_MODULE_TESTCASES.md) and the other way round.

**Status: automated and run on two emulators (2026-11-23, project ZL-Android-Automation, dev).** Last run (emulator-5554): **36 cases — 32 passed, 2 failed, 2 skipped**; both failures are open QA bugs (DOD-02 = QA DOD-03, DOD-70 = QA DOD-04), the skips are the opt-ins. See *Run results* at the end. Suite
[`ScheduleDodTest`](src/test/java/com/zillit/automation/tests/scheduledod/ScheduleDodTest.java)
(+ `ScheduleDodBaseTest`), `TestOrder.SCHEDULE_DOD = 171`, page objects `ScheduleDodPage`
(extends `ScheduleDistPage`) and `ScheduleDodUploadPage` (extends `ScheduleUploadPage`). One run:

```bash
./run-and-report.sh --only ScheduleDodTest
```

Opt-ins: `-Dzillit.dod.publish=true` (DOD-41 really publishes to Document Distribution;
without it DOD-40 stops at Cancel) and `-Dzillit.dod.offline=true` (DOD-72 turns the device's
Wi-Fi and data off for a few seconds).

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own; `id` in backticks is a real resource id from the layout.
Cases marked **[QA DOD-nn]** carry a row of the QA sheet's `Schedule DOD` tab.

---

## The shape of the data

```
Schedule D.O.D.
  └── folder                  keyed by NAME (what the user types, saved Title-Cased — finding 4)
        └── file              PDF, colour (optional), episode on TV, uploader, upload time
                              NO date (finding 5 / QA DOD-04), no scene, no page number
```

| Screen | Activity | Upload | History | Search | Card menu |
|---|---|---|---|---|---|
| Folder list | `DodActivity` | `addFb` "Upload D.O.D." (admin or posting right) | `userHistoryBtn` "History" | none (finding 1) | none — `imgMore` is `gone` (finding 2) |
| Folder | `DodDetailActivity` | hidden (finding 3) | hidden | none | View · Download · Publish to Doc Distribution · Move · Download Count · View Count · Delete |

Menu wording from `DodVM.createMenuList`: "View", "Download", "Publish to Doc Distribution",
"Move", ("Replace" — never, finding 6), "Download Count" (admin), "View Count" (admin),
"Delete" (always added; refused with a snackbar for a non-admin who is not the uploader).

---

## 1. The list · `DOD-0`

| ID | Case | Expected |
|---|---|---|
| DOD-01 | Open Tools → Schedule D.O.D. | `DodActivity`; toolbar title is the tile label; `addFb` reads "Upload D.O.D."; `userHistoryBtn` "History" |
| DOD-02 | **[QA DOD-03]** Wait on the list (first open after launch) | Within 15 s: folder cards (`layMainHolder`) or the empty view (`emptyView`), progress gone — never a blank screen |
| DOD-03 | Look for a search box | None: `searchHolder` is `gone` (finding 1) |
| DOD-04 | Look at a folder card | No ⋮ (`imgMore` is `gone`; the activity's handler is commented out) (finding 2) |

## 2. Upload · `DOD-1`

| ID | Case | Expected |
|---|---|---|
| DOD-10 | Tap "Upload D.O.D." | Picker "Choose below" with one source, "Document" |
| DOD-11 | Document → `zl_test_doc.pdf` → confirm | Form: `etAutoName` hint "Name"; `etChooser` "Select Color (Optional)"; no `etSceneName`, no `etDocumentName`; button "Submit" |
| DOD-12 | Submit with a blank name | Refused (`setError("Please Enter Name")`), form stays |
| DOD-13 | Open the colour dropdown | WHITE, BLUE, PINK, YELLOW, GREEN, GOLDENROD, BUFF, SALMON, CHERRY, TAN, IVORY |
| DOD-14 | Upload as "ZL_AUTO_&lt;run&gt; A", BLUE | Folder **"Zl_auto_&lt;run&gt; A"** is listed (finding 4) |
| DOD-15 | Upload as "ZL_AUTO_&lt;run&gt; B", no colour | Accepted; folder "Zl_auto_&lt;run&gt; B" |
| DOD-16 | Type "zl_auto_&lt;run&gt;" in Name; upload as "zl_auto_&lt;run&gt; a", GREEN | The field suggests "Zl_auto_&lt;run&gt; A"; the file lands in that folder (2 files), no second folder |

## 3. Inside a folder · `DOD-2`

| ID | Case | Expected |
|---|---|---|
| DOD-20 | Open folder A | Title = the tool's title (the toolbar shows it upper-cased: "SCHEDULE D.O.D."); rows "Name: Zl_auto_&lt;run&gt; A" (`tv_name`) |
| DOD-21 | Look for upload / History / search | None (finding 3) |
| DOD-22 | Read a file card | `tv_created_by` "Uploaded by : &lt;user&gt;"; `tv_date` "Uploaded On : …"; card `relativeLayoutHolder` tinted BLUE (#ADD8E6) / GREEN (#98FB98) in its top band (the lower half is a dark text scrim); on a TV project an "Episode: 1" line comes first |
| DOD-23 | **[QA DOD-01]** Read folder B's card (uploaded right after a coloured one) | No date row on any card — the mechanism of DOD-01 (a stale picked date) no longer exists on Android |
| DOD-24 | Open a file's ⋮ as admin | Exactly View, Download, Publish to Doc Distribution, Move, Download Count, View Count, Delete |
| DOD-25 | Tap the file | The PDF opens (`viewPdfApiCall("view")` → download → external viewer) |
| DOD-26 | ⋮ → View | The PDF opens |
| DOD-27 | ⋮ → Download | "File Downloaded Successfully." (or "You don't have permission to Download any file." → skip) |

## 4. View / Download Count · `DOD-3`

| ID | Case | Expected |
|---|---|---|
| DOD-30 | ⋮ → View Count | `UsersCountListPage` titled "View Count" (shown "VIEW COUNT"), search hint "Search By User Name"; lists this user after DOD-25 |
| DOD-31 | ⋮ → Download Count | Titled "Download Count"; lists this user after DOD-27 |
| DOD-32 | Search the count list | Part of a name keeps the row; "zzqq…" empties it and shows `noDataFound` |
| DOD-33 | **[QA DOD-02]** Type "zzqq" in View Count, back, open Download Count | Download Count's search is empty and it lists users (a separate activity on Android) |

## 5. Publish to Doc Distribution · `DOD-4`

| ID | Case | Expected |
|---|---|---|
| DOD-40 | ⋮ → Publish to Doc Distribution | Confirm "Publish to Document Distribution" / "… to the Document Distribution library?"; Cancel publishes nothing |
| DOD-41 | (opt-in) Publish | "Published." |
| DOD-42 | Tap the file after the cancelled publish | The PDF opens, no second publish confirm |

## 6. Move · Delete · `DOD-5`

| ID | Case | Expected |
|---|---|---|
| DOD-50 | Folder B → ⋮ → Move | Sheet "Choose Folder" lists the other folders, NOT B; choosing A moves the file (A holds 3) |
| DOD-51 | Back on the list | Folder B, now empty, is no longer listed |
| DOD-52 | ⋮ → Delete → No | "Are you sure you want to delete?"; nothing removed |
| DOD-53 | ⋮ → Delete → Yes | One file fewer; the snackbar is readable text, not a key |

## 7. History · `DOD-6`

| ID | Case | Expected |
|---|---|---|
| DOD-60 | Tap History | `UserHistoryFolderPage` "&lt;tool&gt; History"; deleted folders or the empty view; no error snackbar; no search |
| DOD-61 | Tap a History folder | `UserChatHistoryPage` with that folder's deleted files, no error |

## 8. QA sheet re-checks · `DOD-7`

| ID | Case | Expected |
|---|---|---|
| DOD-70 | **[QA DOD-04]** Look for a date on the upload form | "Select Date (Optional)" — **absent** (`UploadDocumentActivity.kt:493`); open, product to decide |
| DOD-71 | **[QA DOD-03]** Kill the app, relaunch, open the tool twice, ×2 | Folders on every open within 10 s |
| DOD-72 | **[QA DOD-05]** (opt-in) Offline: ⋮ → Publish; online: tap the file | The PDF opens, no publish confirm |

## 9. Cleanup · `DOD-99`

Every folder whose name contains `zl_auto_` (case-insensitive): open it, delete every file;
up to three passes; none must be left on the list.

---

## Findings

1. **No search anywhere.** `activity_dod.xml` and `activity_dod_detail.xml` both declare
   `searchHolder` `gone` and nothing makes it visible, yet `DodActivity` still wires a colour
   search (`imageView6`, `operationHolder`, a 700 ms debounced server query) to it. Dead code or
   a feature that was switched off.
2. **Folder card ⋮ is dead.** `item_script_folder.imgMore` is `gone`; `DodActivity`
   `onRecyclerItemClick(MORE)` has `openMenu` commented out, and `openMenu` there only knows
   Download / View / Delete.
3. **A folder offers no upload and no History.** `DodDetailActivity.initView` hides `addFb`
   and `userHistoryBtn`; a file can only be added from the list by typing the folder's name.
4. **The name is re-cased.** `generateCamelText()` lower-cases the typed name and capitalises
   each space-separated word: "ZL_AUTO_1234 A" becomes folder "Zl_auto_1234 A". Typing the same
   name in another case files into the same folder. Acronyms ("EP2 DOD") are mangled ("Ep2 Dod").
5. **No date** (QA DOD-04): `dateSelector.isVisible = false` for `DOD_PAGES`, the request sends
   `revisionDate = null`, and the card's "D.O.D. date" row is commented out (`DodDetailHolder.kt`).
6. **Replace is never offered.** `createMenuList` adds it only when `from != DOD_PAGES`, and the
   detail screen is always opened with `DOD_PAGES`; its handler is commented out too.
7. **Delete is offered to everyone** and refused afterwards with "you don't have permission"
   for a non-admin who is not the uploader (not automated — needs a second account).
8. **A failed publish fetch leaves `pendingDistribute` set** (QA DOD-05): the Error branch of
   `DodDetailActivity.handleResponse` does not clear it, so the next View's success is treated
   as the publish.

---

## From the QA sheet (`Schedule DOD` tab, read 2026-11-23)

| QA row | Android status on the sheet | Case | Note |
|---|---|---|---|
| DOD-01 undated upload keeps the earlier date | Not checked (fixed af29d7261) | DOD-23 | The date field is gone on Android, so the mechanism cannot occur; the case asserts no card carries a date |
| DOD-02 View Count search carried into Download Count | Not checked | DOD-33 | **Not reproduced on Android** — a fresh `UsersCountListPage` each time; passes |
| DOD-03 list blank after app restart | Open | DOD-02, DOD-71 | `DodDbManager.getFolderList` `CalledFromWrongThreadException`. **Reproduced** by DOD-02 on emulator-5554 (run 3) and again on the Pixel 7 (run 4, DOD-02 alone): the first open after the Appium session launched the app showed no rows, no empty view and no progress bar for 15 s. DOD-71's own restarts (2 × 2 opens) showed folders every time on both emulators — intermittent, as the sheet says |
| DOD-04 no optional D.O.D. date on upload | Open | DOD-70 | **Fails, as expected** — still no date field. Web has none either — product to say which is right; expected to fail until then |
| DOD-05 failed DD fetch hijacks the next View | Open | DOD-72 | Opt-in: needs the device offline |

---

## From the web E2E report

Source: `web-e2e/schedule-dod.md` (Zillit E2E Report, generated 2026-11-17, dev.zillit.com).
It holds **no web test cases** for Schedule D.O.D. — only one sheet bug.

| Web item | Our case | Note |
|---|---|---|
| Sheet bug DOD-01 (undated upload keeps the earlier date) | DOD-23 | Same row as the QA sheet's DOD-01; Android has no date field, so the case asserts no card carries a date |

Nothing new to add from it. The web behaviour quoted on the QA sheet (name + optional colour, no
date on the form; cards show Uploaded By / Name / Uploaded On) matches DOD-11 / DOD-22 / DOD-70.

---

## Screens and ids

- `activity_dod.xml`: `toolbar`, `progress`, `searchHolder` (gone), `operationHolder`, `tvOperationType`, `filterIv`, `searchIncluded`, `recDocument`, `userHistoryBtn`, `addFb`, `emptyView`, `circularProgress`
- `activity_dod_detail.xml`: `toolbar`, `progress`, `searchHolder` (gone), `searchIncluded`, `filterIv`, `recDocument`, `userHistoryBtn` (hidden), `addFb` (hidden), `emptyView`, `circularProgress`
- `item_script_folder.xml` (folder): `layMainHolder`, `linearLayoutHolder`, `tv_revision_name` (name), `tv_detail`, `tv_episode_no`, `badge`, `imgMore` (gone)
- `documents_script_item.xml` (file): `layMainCard`, `relativeLayoutHolder` (colour), `tv_name` ("Name: …"), `tv_created_by`, `tv_date`, `tv_episode_no`, `imgMore`
- `UploadDocumentActivity` (DOD): `etAutoName` → `autoCompleteDropDown`, `etChooser` → `autoCompleteDropDown`, `etEpisodeNo` (TV), `btnSubmit` → `tvButton`
- `activity_users_count_list_page.xml`: `toolbar`, `searchIncluded`, `countRecyclerView`, `noDataFound`, `progress`
- `activity_user_history_folder_page.xml`: `headerMsg`, `searchHolder` (hidden for DOD), `recDocument`, `emptyView`

---

## Run results

| Run | Device | Result | Notes |
|---|---|---|---|
| 1 | Pixel 7 `34281FDH2005XC` | stopped after 9 cases | 4 passed (DOD-01, 02, 03, 10); 5 skipped: DOD-04 had no folders yet (moved after the uploads); DOD-11/12/13 could not pick `zl_test_doc.pdf` (see *Fixtures*); the run log was cut off by the stop, so the 5th skip is unconfirmed. Stopped and the Appium session deleted |
| 2 | emulator-5556 | 28 passed · 6 failed · 2 skipped | 5 test bugs, all fixed: hint read via page source (DOD-11), colour sampled above the text scrim (DOD-22), toolbar titles are upper-cased (DOD-30/31/60). DOD-70 = QA DOD-04 |
| 3 | emulator-5554 | **32 passed · 2 failed · 2 skipped** | DOD-02 = QA DOD-03 (blank list on first open), DOD-70 = QA DOD-04; skips = opt-ins DOD-41, DOD-72. DOD-99 left no `zl_auto_` folder |
| 4 | Pixel 7 | DOD-02 alone: failed again | Re-check because the user was tapping on emulator-5554 during run 3: the same blank list (no rows, no empty view, no progress) on the Pixel — confirmed app bug |

Every run ended with DOD-99 passing: no `zl_auto_` folder left on the list.

## Fixtures

`zl_test_doc.pdf` in /sdcard/Documents (runner prep). **On the Pixel 7 the shared
`SystemFilePickerPage` could not reach it:** the DocumentsUI roots drawer showed Downloads, Drive,
Zillit, apps — no Documents and no Recent root — so every walk ended in Downloads without the file.
The suite therefore falls back to `zl_dd_doc.pdf` in /sdcard/Download (the same 426-byte PDF, pushed
by the same prep) and remembers whichever worked. Worth fixing in the shared picker: Schedule
Distribution's `zl_test_doc.pdf` uploads will hit the same wall on the Pixel.
