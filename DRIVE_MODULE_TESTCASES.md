# Drive (Z-Drive) — editor and new-feature test cases

Suites: `src/test/java/com/zillit/automation/tests/drive/` (15 `Drive*` classes, all with `@ExtendWith(ReasonLogger.class)`)
Pages: `pages/drive/` — `DriveEditorPage` (office editor), `DriveTextEditorPage` (md/txt),
`DriveFileContent` (download read-back), `DriveFlows` (navigation, QA uploads, purge incl. Trash)
Run one suite: `./run-and-report.sh --only DriveEditorTest` (also `--drive-editor`, `--drive-all`)
App: Zillit-Android `origin/development` @ `cc3dc2a96`, device build **Zillit_32539-dev** (emulator-5554/5556, user Vidya Testing)
Map of the new editor: `side-session-notes/…/drive-editor-map.md`

## Why three suites skipped everything (fixed)

The Tools row is labelled per project — **"Z-Drive"** on ZL-Android-Automation — and every Drive suite opened it
with `ToolsPage.openModule("Drive")`, a whole-label regex. It never matched, `openDrive()` returned false, and every
case skipped in `@BeforeEach` with no reason printed (no `ReasonLogger`). All Drive suites now use
`openModuleContaining("Drive")` and carry `ReasonLogger`.

## The editor as automation sees it (E-00, build 32539)

`OnlyOfficeEditorActivity` hosts Zillit's own page (Zillit Sheets / Docs / Slides); Collabora is gone. Exposed nodes:

| Node | Where | Use |
|---|---|---|
| TextView "Loading editor…" / "Saving…" (no id) | native, centre | activity loading; Back in EDIT mode |
| Button "Edit" (no id), bottom-right | native | VIEW mode + may edit |
| `#status-text` "Opening…" → "Connecting…" → "Live" | page strip | loaded / live |
| `#loading`, `#loading-text` "Opening…", `#zillit_sheets_loading` "Loading Zillit Sheets…" | page | page's own loader (new in 32539) |
| `#people` "People in this file" › initials ("VT") | page strip | presence |
| `#notice` "View only." | page | VIEW mode |
| "Zillit Sheets" / "Zillit Docs" / "Zillit Slides", `#zillit_*_canvas` | page | editor kind |
| EditText 15×10 px at the caret | page | hidden input |

Cells, paragraphs, toolbar, sheet tabs and name box are canvas-painted and **not** in the tree. Edits are proven by
Download → `/sdcard/Download/Drive/<name>` → Appium `pullFolder` → search the xlsx/docx XML (`DriveFileContent`).
Typing uses real key events (`pressKey`), which needs no `--relaxed-security`. The page takes **100–250 s** to reach
"Live" on the emulators with this build (it took ~12 s on build 32537), so `DriveEditorPage.LOAD_S` = 240 s.
Notify prompt "Let others know you updated this file?" is always answered **Skip**.

## Cases and results (build 32539, 2026-11-23)

Legend: ✅ pass · ❌ fails on a real app/server bug · ⏭ skips with the stated reason · 🆕 new case

### DriveEditorTest — office editor, new file, text editor, versions (`TestOrder.DRIVE_EDITOR`)

| # | Case | Status |
|---|---|---|
| E-00 🆕 | Dump the editor tree (xlsx/docx × Edit/Open) to `build/editor-dumps/*.xml` | ✅ (env-failed once: a run killed by `gradle --stop` left the system picker open) |
| 2 | xlsx detail shows Edit and Open | ✅ |
| 3 | pdf detail has no Edit | ✅ |
| E-01 🆕 | `btnEdit` → Zillit Sheets in EDIT mode (no switch, no notice, no Collabora) | ✅ |
| 5 🆕 | Strip shows "Live" and this user in "People in this file" | ✅ |
| E-02 | `btnOpen` → VIEW: `#notice` "View only." + native Edit bottom-right (old case 6, direction fixed) | ✅ |
| E-03 🆕 | Edit switch reloads into EDIT mode | ✅ |
| E-04 🆕 | Back in VIEW closes, no "Saving…", no dialog (close took 1.1–6.5 s across runs; budget 10 s) | ✅ |
| E-05 🆕 | Back in EDIT shows "Saving…", closes, no Leave dialog, no notify without an edit | ✅ |
| E-09 🆕 | Rotation does not reload | ⏭ app locks orientation programmatically ("Screen rotation cannot be changed") |
| E-08 🆕 | Editor background #FFFFFF / #0F1115 | ✅ |
| E-07 🆕 | docx → Zillit Docs EDIT; Open → VIEW with switch | ✅ ✅ |
| E-11 🆕 | Type into a `QA_Edit_*.xlsx` copy → Download → value in the xlsx | ❌ BUG-1 (notify prompt fired, download unchanged) |
| E-11 🆕 | Same for `QA_Edit_*.docx` | ❌ BUG-1 (or ⏭ when the page needs >240 s to load) |
| V-01 🆕 | Office edit adds a version | ❌ BUG-1 (no Versions card at all) |
| N-01 🆕 | After a real edit, "Let others know you updated this file?" with note box, Send/Skip; Skip closes | ✅ (⏭ once: page load >240 s) |
| F-01 🆕 | + sheet New group: Document / Spreadsheet / Presentation / Markdown file / Text file | ✅ |
| F-03/F-04 🆕 | Name dialog "Untitled spreadsheet" + ".xlsx"; Cancel creates nothing | ✅ |
| F-05 🆕 | New Spreadsheet → editor in EDIT mode, listed, "v1 · Created" | ❌ BUG-1 (no versions for office files) |
| F-06 🆕 | New Document / Presentation → Docs / Slides in EDIT mode | ✅ |
| F-07 🆕 | Typed ".xlsx" is not doubled | ✅ |
| F-10 🆕 | New Markdown → `DriveTextFileActivity` editing (EditText + Save, no Edit) | ✅ |
| T-02/T-03/T-05 🆕 | Save → "Saved"; unchanged → "No changes to save"; reopened read-only shows the text | ✅ |
| T-04 🆕 | Unsaved Back → "Discard your changes?"; Keep editing / Discard | ✅ |
| V-01 text 🆕 | Rows "v2 · Saved", "v1 · Created", "Saved by …", Open/Download | ✅ |
| V-04 🆕 | Older text version opens read-only titled "<name> · version N" | ✅ (failed once with the plain name — not reproduced) |
| V-02 🆕 | Restore hidden on latest; "Restore Version" dialog; new "Restored" version | ✅ |
| V-03 🆕 | Office version opens read-only | ⏭ no office versions (BUG-1) |
| 99 | CLEANUP: purge every `QA_` file (Trash too) | ✅ |

Removed (Collabora-only, no equivalent): What's New modal, Edit-document pencil, toolbar content-descs, Leave Editor
dialog, formula-bar read/write, "saved automatically" wording.

### Other suites

| Suite | Change | Status |
|---|---|---|
| DriveEditorWriteProbeTest | Rewritten: key events / hidden-EditText sendKeys / W3C actions, judged by the downloaded file | ⏭ last run could not upload its copy (a stray Select mode hid the FAB — guarded since); rerun pending, devices busy |
| DriveCollabEditTest | Rewritten: both users in EDIT mode, presence in "People in this file", both values in A's download | not run (needs Pixel + emulator together) |
| DriveTwoDeviceE2ETest 10–12 | Rewritten: type, Skip, read the other side's download | not run (two devices) |
| DriveViewTest S-01..S-03 🆕 | Show: All / Shared by Me / Last 7 days / Large files, hints, small files absent | ✅ ✅ ✅ |
| DriveViewTest search | Poll results (debounce); scroll the chip row to "Folders"; reset a remembered Show filter | case-insensitive search + Folders chip ✅; last run 10/3/3 had 3 "list unreadable" fails from slow loads / a remembered Show filter — fixed, rerun pending |
| DriveOrganiseTest Z-01/Z-02 🆕 | Folder ⋯ "Download as ZIP" → "Download started"; none on files | ✅ ✅ |
| DriveOrganiseTest move/bulk/trash | Move sheet: a destination tap now moves at once; bulk rows picked by position; Empty Trash needs an item | ✅ (10/11, only BUG-2 fails) |
| DriveOrganiseTest 🆕 | Rows keep their names in Select mode | ❌ BUG-2 |
| DriveUploadTest | Picker uses DocumentsUI search first (Recent never lists the fixtures) | ✅ except folder upload ❌ BUG-4 |
| DriveUploadTest U-01 🆕 | Pause → "Paused N%" / Resume → completes (16 MB pushed via Appium, emulator link throttled to EDGE) | ⏭ row already "Finishing…" when the sheet opens — too fast to pause |
| DriveShareTest L-01..L-03 🆕 | "Can edit" only for office; drops "Never"; needs a recipient | ✅ ✅ ✅ |
| DriveShareTest L-04 🆕 | Can-edit link to `TestEmails.recipient()` → "Can edit" badge, "Not edited yet" | ✅ |
| DriveShareTest L-05 🆕 | File details Share → 1 hour / 24 hours / 7 days | ✅ |
| DriveShareTest generate / invalid email / folder share | Link listed under "Active share links"; invalid email rejected by toast; folder roles are Viewer/Editor/Owner checkboxes | ✅ (suite 17/0/2) |

## Real app/server bugs found

* **BUG-1 — office edits never reach the Drive file.** Typing in Zillit Docs/Sheets is kept by the editor (reopening
  shows it, and the notify prompt fires), but File details keeps the old size/Modified time, lists **no versions**
  (no Versions card, even "v1 · Created" for a New file), and Download returns the original bytes 10+ minutes later.
  Text files (md/txt) version correctly.
* **BUG-2 — Select mode hides every row name.** With Select on, `item_drive_file` rows show only the checkbox and
  icon: `tvName` ends at `barrierTextEnd`, whose referenced views (`btnFavorite`, `btnMore`) go GONE in Select mode.
* **BUG-4 — Upload Folder renames every file** to `<epoch-ms>_<n>_<name>` (e.g. `1795446062732_14763_root_pic.jpg`).
* **BUG-5 — Edit Info drops the extension.** Renaming `x.pdf` to `QA_ren.v2 <n>` stores `QA_ren.v2 <n>` (no `.pdf`);
  the web keeps the original extension when the typed name has none (L424). DriveOrganiseTest gRenameKeepsExtension.
* **BUG-6 (candidate) — duplicate folder rename gives no inline error.** Renaming folder B to A's name is refused
  (no second A appears) but `tvEditNameError` never shows and the dialog just closes (web L1159 shows "already exists").
* **BUG-7 — no 100-character cap on folder names.** A 101-character folder name is created and listed; the web
  refuses it (L1135, ZL-21806). DriveTest gCreateLongNames.
* **DRIVE-03 — restoring a folder from Trash loses its sub-tree.** Delete `F/sub/deep` → restore F → F comes back
  with its direct file only; `sub` and `deep` stay in Trash as separate items. DriveOrganiseTest gTrashRestoreTree.
* Observation: the editor page takes 100–250 s to reach "Live" on build 32539 (≈12 s on 32537).


## Web gap cases (web-e2e/gaps/drive.md, 186 MISSING rows)

Each case carries its web line in the display name (`[WEB L442]`). Status column: ⏳ written, run pending ·
✅ / ❌ (bug) / ⏭ (reason) after a run. Data is `QA_…` / `zl_auto_tag_…` and purged (Trash included).

| Gap | Web lines | Suite → case(s) | Status |
|---|---|---|---|
| M01 Trash | L442, L127, DRIVE-03 | DriveOrganiseTest gTrashRestore, gTrashRestoreTree (DRIVE-03), gTrashDeleteForever, gEmptyTrashCancel | ✅ restore, delete-forever, Empty-trash cancel · ❌ DRIVE-03 reproduced (restored folder comes back with its direct file only; `sub/`, `deep/` stay in Trash as separate items) |
| M01 Trash (L1524 confirm-and-empty) | L1524 | not automatable — emptying Trash on the shared test account deletes other suites' and agents' trashed items for good | — |
| M02 Edit Info | L424, L1159, L307, L313, L1172 | DriveOrganiseTest gRenameKeepsExtension (+description), gRenameUpperExtension, gRenameDuplicate, gRenameBlank, gRenameTrim | ✅ blank refused, trim · ❌ BUG-5 extension dropped (`QA_ren.v2 <n>` stored without `.pdf`) · ❌ BUG-6 duplicate rename refused without the inline error (no second folder was created) · L1159 ⏭ in r2 (upload of the copy failed), ✅ in r1 |
| M03 Tags | L463, L117/L1750, L1756, L1510, L1089 | DriveViewTest gTagCreate, gTagFilter, gTagDuplicateAndBlank, gTagRemoveAndEmptyFilter (tags cannot be deleted on Android; `zl_auto_tag_*` remain) | ✅ create, filter, duplicate/blank, remove + empty filter (r2) |
| M04 Copy link card | L536 | DriveShareTest gCopyLinkCard (7 days, http link, expiry ≈ +7 d, clipboard) + L-05 options | ✅ |
| M05 Link listing / revoke | L512, L524 | DriveShareTest gRevokeLink (outsider page is web-only) | ✅ (r2) |
| M06 Downloads | L758, L770, L776, L1730 | DriveOrganiseTest gDetailDownload, gBulkDownloadOne, gBulkDownloadTwo (one .zip with both), gBulkDownloadFolderAndFile (folder → own zip). L752 row-menu Download: no such menu entry on Android | r1: detail download ❌ (read-back helper pulled only /sdcard/Download; fixed) · bulk cases ⏭ (copy upload failed in the picker; picker search hardened) — rerun pending |
| M07 Versions | L726, L738, L782, L1476 | DriveEditorTest V-01 text rows, V-02 restore, V-04 open, downloadTextVersion; office versions blocked by BUG-1 (V-03 skips) | ✅ text versions V-01/V-02/V-04 · office ❌ BUG-1 (V-03 ⏭) · downloadTextVersion added after that run |
| M08 Per-right controls (2 devices) | L601, L1047, L1301, L1071, L1077 | DriveTwoDeviceE2ETest gRightsViewOnly, gRightsDownload, gRightsEdit (+ no Move, C-1), gRightsStepDown | M08_ST |
| M09 Folder roles (2 devices) | L583, L589, L595, L1059, L1307, L504, L1313 | DriveTwoDeviceE2ETest gFolderViewer, gFolderEditor, gFolderOwner, gFolderInherited, gFolderStepDown | M09_ST |
| M10a Live revoke | L1804 | DriveTwoDeviceE2ETest gLiveRevoke | M10_ST |
| M10a "one of two people" | L614, L1810 | not automatable — needs a THIRD project user; the project has two test accounts | — |
| M10b Live sync | L1828, L625 | DriveTwoDeviceE2ETest gLiveSubfolder, gLiveRename | M10_ST |
| M10b other live cases | L631, L637, L1816, L1264, L1270, L1276, L1282, L1288, L1295, L1822, L1834 | not done this pass — same mechanism as gLiveSubfolder/gLiveRename, each a further two-device permutation (~5 min of both devices per case) | — |
| M11 Unread badges | L620, L1860, L1866, L1872, L1878, L1884, L1890 | not done this pass — needs a fresh unread share per case on the shared Pixel account; badge state is also changed by other agents' runs | — |
| M12 Rights from User Management | L643, L1327…L1358, L1898…L1936, L2066 | not automatable — toggles the Pixel user's real project rights/admin flag (brief: no changes to real users' rights without a safe restore) | — |
| M13 Create Folder rules | L289, L137, L295, L1166, L1135 | DriveTest gCreateInsidePicked, gCreateDuplicate, gCreateSameNameElsewhere, gCreateLongNames (100 ok / 101 refused) | ✅ same name in another folder · ❌ BUG-7 a 101-character name is accepted (100 is the web cap) · L289/L295 r2 test issues (Create below the fold; duplicate error arrives on a reopened sheet) — fixed, rerun queued |
| M14 Create Folder access | L301, L1628, L1640 | DriveShareTest gCreateFolderSpecificUser, gCreateFolderProjectNoRoleAndReset | r2 ❌ test issue (Create sits below the fold once the member list shows; create() now scrolls) — rerun queued |
| M14 Project Users + Editor | L1634 | not automatable — grants every project user access and notifies them | — |
| M15 Upload destination / names | L334, L340, L346, L358, L1223 | DriveUploadTest gTwoUploadsIntoFolder, gUploadToExistingFolder, gStagedNameKeepsExtension, gDuplicateUploadResolve | M15_ST |
| M15 230-char file name | L1178 | not automatable — needs a fixture with a 230-character file name; the runner (shared file) pushes none | — |
| M16 Folder upload tree | L352, L1199, L1976 | DriveUploadTest "UPLOAD FOLDER…" asserts the tree (fails BUG-4); 7-level tree needs a fixture the runner does not push | ❌ BUG-4 |
| M17 Upload lifecycle | L814 | DriveUploadTest gUploadAfterLeavingDrive | M17_ST |
| M17 pause/cancel, interrupted | L802, L808 | U-01 skips (upload finishes before the sheet opens); L808 needs a network fault on a shared device — not automatable | ⏭ |
| M18 Opening types | L392, L380, L707, L386, L701 | DriveViewTest gOpenPdf, gImageDetail, gOpenCsv, gAudioDetail; L701 = DriveEditorTest E-02/E-03 | ✅ PDF opens in Drive's viewer · image/csv/audio ⏭ r2 (fixture upload stopped in the picker; the skip now names the step) — rerun queued |
| M18 mp4 / rtf | L2058, L398 | not automatable — no video or rtf fixture is pushed by the runner | — |
| M19 Move | L430, L436, L1736 | DriveOrganiseTest gMoveExcludesSelf, gMoveFile, gBulkMove, gMoveToRoot | ✅ picker excludes self · r1 ✅ bulk move / r2 ❌ (Move button below the fold; fixed) · file move ⏭ (needs the download file) · move-to-root ❌→fixed (sheet expand) |
| M20 Favourites | L451, L112 | DriveViewTest gFavourites | r2 ❌ test issue (menu label read before the sheet filled; star paired by index) — fixed, rerun queued |
| M20 empty favourites | L834 | not automatable — the account is shared; other suites/agents leave favourites, so "nothing starred" cannot be arranged | — |
| M21 Details / DRIVE-01 | L457, DRIVE-01 | DriveTest gRowSubtitleCounts, gFolderDetailsContent | r2: DRIVE-01 subtitle was only "Just now" (subfolder creation now verified first) · L457 owner read before the sheet filled it — both rerun queued |
| M22 Shared with me | L72, L1770, L608, L1319 | DriveTwoDeviceE2ETest gSharedRootPresentation, gSharedChildNotParent (+ B's search), gStarIsPerUser | M22_ST |
| M23 Show filter / selection | L107, L1712 | DriveViewTest gSharedByMeAndReset, gSelectionClearedOnSectionSwitch; S-01…S-03 | ✅ L107 Shared-by-me reset, L1712 selection cleared, S-01, S-03 · S-02 test issue fixed |
| M24 Sort direction | L92 | DriveViewTest gSortReverses | ✅ |
| M25 Special names | L1141, L1147, L1153, L912 | DriveViewTest gSpecialNames | r2: names list, open and are found; header compare was case-sensitive (header is drawn in capitals) — fixed, rerun queued |
| M26 New-folder privacy / nesting | L277, L283 | DriveShareTest "A newly created folder starts with NO ONE having access"; DriveTest gNoLocationInsideFolder | ✅ |
| M27 Share screen | L484, L491 | DriveShareTest gShareScreenDetails | ✅ titles, Save disabled before a change, roles Viewer/Editor/Owner (r2) |
| M28 Link by email | L1089, L1582, L542 | DriveShareTest gLinkButtonLabel; L542 real send = L-04 (to TestEmails.recipient()); "4 recipients" needs addresses we may not invent | ✅ label (Generate link → Send link by email) |
| M29 File requests | L1562 | DriveRequestFilesTest gRecipientNormalised (test address typed twice in different case); L575 partial-send warning cannot be forced | M29_ST |
| M30 Permissions at upload (2 devices) | L1956 | DriveTwoDeviceE2ETest gUploadPermissionDownload | M30_ST |
| M30 folder-member note / minimum level | L1944, L1950 | not done this pass — needs B as a folder Editor at upload time (one more two-device case) | — |
| M31 Offline paths | L171…L957, L221 | not automatable — airplane mode on a shared emulator breaks the other agents' sessions | — |
| M32 Server refusals | 33 tests | not automatable — needs a server stub/proxy | — |
| M33 Smaller gaps | L62, L902, L1127, L373, L475, L1516 | DriveTest gBackLeavesDrive, gDescriptionCap; DriveViewTest gGridCards, gSearchOpensFolder; DriveActivityTest gUploadRecorded; DriveCommentsTest gLongComment | ✅ L62 Back leaves Drive, L902 description cap · L1127 grid / L373 search-opens-folder: r2 test issues (card select by tap; header capitals) — rerun queued · Activity/Comments pending |
| M33 paging / mixed rights | L1217, L1662, L1668, L1718 | not automatable here — paging needs 55+ uploads (~1 h of a shared device); mixed-rights bulk needs files owned by both users in one folder plus a second user's bulk bar (two devices) | — |
| C-1 Move under Shared with me | L1053 | DriveSharedWithMeTest asserts no Move for an editor | ✅ (already fixed) |
