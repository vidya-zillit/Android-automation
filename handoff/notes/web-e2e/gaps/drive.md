# Drive — web vs Android gaps

Sources: web `web-e2e/drive.md` (Zillit E2E Report, 326 web tests + 3 sheet bugs) vs Android Appium suites `Android-automation/.../tests/drive/` (15 suites) and app source `Zillit-Android/app/src/main` · 2026-11-23.

Path shorthands used below: **DRV** = `Zillit-Android/app/src/main/java/com/zillit/zillitapp/bottomNav/tools/drive/`, **LAY** = `Zillit-Android/app/src/main/res/layout/`, **STR** = `Zillit-Android/app/src/main/res/values/strings.xml`, **T** = `Android-automation/src/test/java/com/zillit/automation/tests/drive/`. Web tests are cited by their line in `drive.md` (`L123`) and spec line.

## Summary

| Classification | Web tests |
|---|---|
| COVERED | 18 |
| MISSING | 186 (153 testable with the current harness + 33 that need a server stub or proxy, see group M32) |
| WEB-ONLY | 121 |
| CONFLICT | 1 |
| **Total** | **326** |

A test that is only partly covered is counted as MISSING (partial). The block says what the Android test leaves out.

**Sheet bugs**

| Bug | Web | Android (per source) | Classification | Where |
|---|---|---|---|---|
| DRIVE-01 Folder Info shows "0 files, 0 folders" | Not checked | Android's Folder Details sheet shows no counts at all (LAY `bottom_sheet_folder_details.xml`: Type, Created, Modified, Owner, Access, Tags, Activity). The counts are in the **row subtitle** and are hidden when zero (DRV `presentation/adapter/DriveItemAdapter.kt:299-316`) | MISSING (check the row subtitle) | Group M21 |
| DRIVE-02 Favorites drill-in shows an empty breadcrumb bar | Not checked | Not applicable: the breadcrumb bar is commented out (DRV `presentation/home/HomeFragment.kt:1031,1323`) and the Favorites screen is hidden (LAY `fragment_drive_drawer.xml:146-153`, `visibility="gone"`) | WEB-ONLY | — |
| DRIVE-03 Restoring a folder from Trash brings back only its direct files | Not checked | Applicable: Trash has Restore (LAY `item_trash.xml` `btnRestore`; DRV `presentation/trash/TrashViewModel.kt:74`), and the bug is on the server | MISSING | Group M01 (step 4) |

---

## CONFLICT

### C-1 — Under "Shared with me", Move is never offered, even to an Editor

- **Web:** "under Shared with me: no Move for anyone (ZL-21869); Share only with the full set; Request files with edit" — L1053, drive.spec.js:5295. Supported by L589: an Editor never sees Move.
- **Our Android assertion:** `DriveSharedWithMeTest` "RIGHTS: the recipient's menu matches EXACTLY the rights they were granted" (T `DriveSharedWithMeTest.java:229-232`). With `GRANTED_RIGHTS` set to `"edit"`, which is the **default** (`:63-64`), it runs `assertTrue(move, "An editor should be offered Move to…")`.
- **The web is right, per Android source.** DRV `presentation/home/HomeFragment.kt:1459-1469`:
  > Move — same gating as Share, and never under Shared with me (any depth) … even an Editor of a shared folder doesn't get it (Drive hand-off §2, web ZL-21869)

  The code is `if (canEditItem && hasPostingAccess && !viewModel.isUnderSharedWithMe)`, where `isUnderSharedWithMe` is DRV `presentation/home/HomeViewModel.kt:252-259`. The bulk bar hides Move there too (`HomeFragment.kt:958-959`).
- **Fix:** in the edit branch, change the check to `assertFalse(move, "Move is never offered under Shared with me (ZL-21869)")`. Keep `assertTrue(edit)` and `assertFalse(delete)`. Also update the javadoc at `:55-62`, which says the editor "gained Share, Move and Edit Info". With the default rights, the test as written fails against a correct build.

**Checked and not a conflict:** `DriveShareTest` "Rights toggle independently — Download can be granted without Edit" (T `DriveShareTest.java:205-226`). The web's file Share drawer uses nested levels, where a higher level ticks the lower ones (L484). Android uses three independent switches by design: Edit and Download only need View (DRV `presentation/share/ShareFragment.kt:955-980`, `isEnabled = entry.canView && …`). The Android test matches Android.

---

## MISSING

Groups are ordered by value. Each block lists the web test(s), why the case applies on Android, and a proposed case. Ids are from the app layouts and strings. Validation toasts and `EditText.error` never show in the page source (see the note in `DriveTest`), so "refused" is asserted as "the sheet or dialog stayed open, or nothing changed".

### M01 — Trash: restore, delete forever, empty, and folder restore · **High** · 3 tests + sheet bug DRIVE-03
- Web: "delete to trash, restore it, then delete it forever" L442 (drive.spec.js:2081); "Empty trash (faked): the confirm counts the items, 'Trash emptied'…" L1524 (drive-forms.spec.js:293); "Trash view opens; 'Empty trash' is only ever cancelled" L127 (drive.spec.js:605).
- Applicable: Trash rows carry `btnRestore` and `btnDelete` (LAY `item_trash.xml:53,67`). The handlers are in DRV `presentation/trash/TrashFragment.kt:89-118`: toast "%1$s restored", the dialog "Delete permanently?", and "Empty trash?" with the `Empty Trash` button. The page object already has `DriveTrashPage.restore()`, `deleteForever()`, `emptyTrash()` and `confirm()`, but no test calls them.
- Partial today: `DriveOrganiseTest` "TRASH: a deleted folder appears in Trash" passes when **either** the folder is listed **or** Trash is empty (T `DriveOrganiseTest.java:273-274`), so it passes vacuously on an empty trash. "TRASH: the Empty Trash action is offered" only checks the button is there.
- **Proposed** (suite `DriveOrganiseTest`):
  1. Create `QA_Trash_<stamp>`, then delete it from the row menu (`menuDelete`, then confirm `Delete`).
  2. Drawer `navTrash`. Assert the name is in the Trash list. Drop the `|| isEmptyStateShown()` fallback.
  3. Tap the row's `btnRestore`. Assert the row leaves Trash, and that the folder is back on Drive home under Folders.
  4. **DRIVE-03:** create a folder holding one subfolder (itself holding a file) and one file. Delete it, restore it, open it. Assert both the subfolder and the direct file are inside, then open the subfolder and assert its file.
  5. Delete again. In Trash, tap `btnDelete`, assert the "Delete permanently?" dialog and confirm `Delete`. Assert the row is gone from Trash and does not come back on Drive.
  6. Tap `btnEmptyTrash` and assert the "Empty trash?" dialog. Tap **Cancel** and assert the Trash rows are unchanged (L127). Optionally, on a throwaway item: confirm, then assert the empty state with `drive_trash_is_empty` "Trash is empty".
- Expected: restore puts the item back where it was; a folder comes back with its whole tree; delete forever and Empty Trash remove the items permanently; Cancel removes nothing.

### M02 — Edit Info (rename and description) · **High** · 5 tests
- Web: "rename keeps the extension (ZL-18310); the description shows in its column" L424 (:1955); "file rename with dots…" L1159 (:5831); "renaming a folder onto a sibling's name shows the server message and keeps the drawer (ZL-21807)" L307 (:1528); "a blank rename is refused in the form" L313 (:1559); "renaming to the same name, and a name with outer spaces" L1172 (:5886).
- Applicable: the row menu has `menuRename` labelled "Edit Info" (LAY `bottom_sheet_context_menu.xml:120-138`). The dialog is `dialog_edit_item_info.xml` with `etEditName`, `etEditDescription` and `tvEditNameError`; its title is "Edit File" or "Edit Folder" and its button is "Save Changes". The rules are in DRV `presentation/home/HomeFragment.kt:1593-1645`: the name is trimmed; an empty name gives the toast `drive_name_is_required` and sends nothing; an unchanged name sends nothing; a server `duplicate_*_name` reopens the dialog with an inline `tvEditNameError` saying `drive_duplicate_name` "A file or folder with this name already exists". The file detail shows the description (`tvDescriptionLabel` and `tvDescription`, DRV `presentation/file/FileDetailFragment.kt:441-450`). **No Android test opens Edit Info.**
- **Proposed** (new suite `DriveRenameTest`, or append to `DriveOrganiseTest`):
  1. Upload the fixture `zl_drive_doc.pdf` and open Edit Info. Type `QA ren.v2` with no extension and description `QA desc <stamp>`, then Save Changes. Under Files, assert the row reads `QA ren.v2.pdf` (the web rule; if Android stores it without the extension, record that as a bug). Open the detail and assert `tvDescription` equals the description.
  2. Rename the file to `.hidden`, then to `UPPER.PDF`. Record the stored names (rule L1159: the extension is added unless the name already ends in it, in any case).
  3. Create two sibling folders A and B. Rename B to A's name. Assert the dialog reopens with `tvEditNameError` shown, and that B keeps its old name in the list.
  4. Rename a folder to three spaces (`"   "`). Assert the list still shows the old name. (A toast cannot be asserted; the dialog closes on Save.)
  5. Rename a folder to `"  QA_sp_<stamp>  "`. Assert the list shows the trimmed name.
- Expected: the extension is kept, the description round-trips, a duplicate name gets the inline error, and blank or whitespace-only names never reach the server.

### M03 — Tags: add, create, filter by, remove · **High** · 5 tests
- Web: "tag a file with e2e-drive, filter the list by it, then remove the tag" L463 (:2200); "the tag filter asks the server which items carry the tag" L117 (:558); "the tag filter … shows only those" L1750 (drive-lists:746); "a tag with no items says the filter found nothing" L1756 (drive-lists:772); "a tag name stops at 50; a refused tag (e.g. a duplicate name) is reported" L1510 (drive-forms:200).
- Applicable: the file detail has the "Tags" card (`tagsContainer`, `tvNoTags` "No tags assigned"), a "+ Add Tag" chip that opens the "Add Tag" list with "+ Create New Tag", then the "Create Tag" dialog (hint "Tag name", button "Create"). A chip's close icon removes the tag (DRV `presentation/file/FileDetailFragment.kt:486-527,670-720`). Home has `chipFilter`, which opens "Filter by Tag" with "All". `DriveViewTest` and `DriveTest` only open that sheet and assert its title.
- **Proposed** (suite `DriveViewTest`, or a new `DriveTagsTest`):
  1. Open the `zl_drive_doc.pdf` detail and tap "+ Add Tag", then "+ Create New Tag". Type `qa-tag-<stamp>` and tap Create. Assert a chip with that name is in `tagsContainer` and `tvNoTags` is gone.
  2. Back on home, tap `chipFilter` and pick `qa-tag-<stamp>`. On the Files tab, assert the list holds only `zl_drive_doc.pdf`. On the Folders tab, assert the list is empty.
  3. Pick "All" and assert the full list is back.
  4. Create a second tag with no items and filter by it. Assert the empty state shows and record its wording. Android shows the folder or file empty copy and has no "no tagged items" case (`HomeFragment.kt:971-993`), which is the same known bug as on web.
  5. Create a tag with the **same** name and record the outcome (`drive_err_failed_to_create_tag` or a duplicate chip).
  6. Tap the chip's close icon and assert the chip is gone and "No tags assigned" is back.
- Expected: a tag is saved on the file, the filter narrows to the tagged items, and removing the tag works.

### M04 — Detail "Share" makes a Copy link with expiry 1 hour / 24 hours / 7 days · **High** · 1 test
- Web: "the Info panel's Copy link offers 1 hour / 24 hours / 7 days" L536 (:2680).
- Applicable: on the file detail, `btnShare` opens the "Share Link Expiry" dialog with 1 hour, 24 hours and 7 days (DRV `presentation/file/FileDetailFragment.kt:703-717`, values `1h`, `24h`, `7d`). The result appears in `cardShareLink`, `tvShareLink` and `tvShareExpiry` ("Expires: …", `:453-469`). `btnCopyLink` shows the toast "Link copied to clipboard". **Untested**: `DriveShareTest` only covers the Share screen's link tab.
- **Proposed** (suite `DriveShareTest`): open the `zl_drive_doc.pdf` detail and tap `btnShare`. Assert exactly the three options. Pick "7 days". Assert `cardShareLink` is visible, `tvShareLink` starts with `http`, and the date in `tvShareExpiry` is about 7 days from today. Seed the clipboard with a sentinel, tap `btnCopyLink`, and assert the clipboard equals the URL (same technique as `DriveRequestFilesTest` S-7).

### M05 — Share-via-link: listing and revoking a link · **High** · 2 tests
- Web: "Copy link makes a tracked anyone-with-the-link URL, reused while fresh, listed and revocable" L512 (:2508); "Generate link → an outsider sees the file; after revoke, 'Link revoked'" L524 (:2580). The outsider page itself is web-only.
- Applicable: the link tab has "Active share links" (`emailLinksContainer`; `tvNoEmailLinks` "No active share links for this file yet."). Each link has a Revoke action (content description "Revoke link"). The confirm reads "Revoke this link? All recipient URLs stop working immediately.", followed by the toast "Link revoked" (DRV `presentation/share/ShareFragment.kt:399-445,555-565`). `DriveShareTest` "GENERATE a share link" only asserts that a URL appears.
- **Proposed** (suite `DriveShareTest`, after GENERATE): reopen the link tab and assert the generated URL is listed under "Active share links". Tap Revoke, assert the confirm text and confirm. Assert the row leaves the list (or "No active share links…" shows). Optional: open the revoked URL in Chrome on the device (`driver.get(url)` via a browser intent) and assert the page says the link is revoked.

### M06 — Downloads: single, bulk, and server zip · **High** · 6 tests
- Web: "the row menu's Download saves the file" L752 (:4018); "the preview modal's Download saves the file" L758 (:4031); "bulk Download of one file opens it" L770 (:4068); "bulk Download of two files is one server .zip" L776 (:4082); "bulk Download on mixed rights zips the downloadable files and names the one left out" L1724; "a folder in a bulk Download is named as left out" L1730.
- Applicable: the file detail has `btnDownload`, `downloadProgressContainer` and `tvDownloadStatus` ("Downloading… N%"). When done, the button reads "Downloaded ✓" (DRV `presentation/file/FileDetailFragment.kt:578-593`). On home: `chipSelect`, then `btnBulkDownload` (toast "Download started"). One file downloads directly; two or more become a single server zip named "Drive files (N).zip" (DRV `presentation/home/HomeViewModel.kt:2492-2530`; `data/repository/DriveRepositoryImpl.kt:850`). **Folders are dropped silently** from a bulk download, because only `DriveItem.File` ids are sent (`HomeViewModel.kt:2494-2500`). That is the L1730 bug on Android too. Android has no row-menu Download; the detail is the preview equivalent.
- **Proposed** (new `DriveDownloadTest`):
  1. Open the `zl_drive_doc.pdf` detail and tap `btnDownload`. Wait until the button text is "Downloaded ✓". Assert `Download/zl_drive_doc.pdf` exists on the device (`driver.pullFile` or `mobile: shell ls /sdcard/Download`).
  2. On home, Files tab: Select, tick 1 file, tap `btnBulkDownload`. Assert that one file arrives with no zip.
  3. Tick 2 files and tap Download. Assert `Drive files (2).zip` arrives and holds both. Exit select mode.
  4. Tick one folder and one file and tap Download. Record that the folder is skipped without a message (gap against web L1730).
- Expected: one file downloads as itself; two or more download as one server zip.

### M07 — Versions on the file detail: list, restore, download · **Medium** · 4 tests
- Web: "viewing and restoring an older version" L726 (:3873); "Version history opens from the row menu and from Info" L738 (:3950); "the Info panel downloads a saved version" L782 (:4107); "Info: Restore → 'Restore this version?' → 'Restored to version N', and the list refreshes" L1476 (drive-editors:1411).
- Applicable: `cardVersions` shows when the server returns versions: `tvVersionsHeader` "Versions (N)" and `rvVersions`. Each row has Restore, which opens the "Restore Version" dialog (`drive_restore_version_message`) with a "Restore" button, and Download, whose DownloadManager title is "Drive: <name> (vN)" (DRV `presentation/file/FileDetailFragment.kt:114-116,559-576,641-650`; `FileDetailViewModel.kt:492-530`). An edit through the editor creates the versions.
- **Proposed** (suite `DriveEditorTest`, after the PERSISTENCE cases, which already make an edit):
  1. Open the `zl_drive_book.xlsx` detail and scroll to `cardVersions`. Assert the header reads "Versions (N)" with N ≥ 1.
  2. Restore the oldest version and confirm. Assert the header count goes up or the list refreshes. Reopen the editor and assert `D1` no longer holds `lastMarker`.
  3. Tap Download on a version and assert a file named `… (vN)` arrives.

### M08 — Per-item rights decide the detail and menu actions · **High** · 6 tests
- Web: "file levels as B sees them: View, then Download, then Edit (ZL-21915)" L601 (:3192); "a file's menu follows its permission: view, download, edit, full" L1047 (:5269); "a file level going down (Edit → Download → View) takes B's actions away step by step" L1301 (:6464); "Info: Manage, Copy link and the tag picker only with edit" L1071 (:5383); "a per-file 'no download' hides the preview Download" L1077 (:5418); "hover delete and the bulk bar follow each item's rights" L1065 (:5352).
- Applicable: the detail gates each control (DRV `presentation/file/FileDetailFragment.kt:386-420,486-527`): `btnDownload` needs `canDownload`; `btnEdit` needs an editable type, `canEdit` and posting; `btnShare` (Copy link) and `btnManageAccess` need `canEdit` and posting; `btnDelete` needs `canDelete`; the comment composer and "+ Add Tag" need edit. Row menu: Share needs edit and posting; Edit Info needs edit; Delete needs `canDelete` (`HomeFragment.kt:1420-1555`).
- Partial today: `DriveTwoDeviceE2ETest` "B's rights are VIEWER" asserts no Edit Info, Move or Delete in the **row menu** only. "A UPGRADES B to editor" asserts Edit Info, but no Share and nothing on the detail screen. **Nothing checks the Download right.**
- **Proposed** (suite `DriveTwoDeviceE2ETest`, after case 2; A owns `zl_drive_book.xlsx`):
  1. A grants B **View only** (Can View on; Edit and Download off). On B, open the detail. Assert `btnDownload`, `btnEdit`, `btnShare`, `btnManageAccess`, `btnDelete`, `etComment` and the "+ Add Tag" chip are all absent, and `btnOpen` is present.
  2. A adds **Download**. On B, pull to refresh and reopen. Assert only `btnDownload` appeared.
  3. A adds **Edit**. On B, assert `btnEdit`, `btnShare`, `btnManageAccess`, the composer and "+ Add Tag" appear; `btnDelete` stays absent; the row menu has Share and Edit Info but **no Move** (see C-1).
  4. Step back down Edit → Download → View and assert the controls disappear in reverse order (L1301).

### M09 — Folder roles for B: Viewer, Editor, Owner, and inherited · **High** · 7 tests
- Web: "a Viewer on a folder: view-only menu, no add actions, no delete or move" L583 (:3086); "an Editor adds files to the folder (ZL-21871) but never sees Move (ZL-21869); A sees the file live" L589 (:3130); "an Owner role on a folder gives B 'Share' under Shared with me" L595 (:3172); "inside a shared folder the add actions follow the folder's can_edit" L1059 (:5323); "a folder role going down (Owner → Editor → Viewer)…" L1307 (:6490); "an Editor role on a folder reaches its subfolder ('through the parent folder')" L504 (:2446); "inherited Viewer and direct Editor…" L1313 (:6517).
- Applicable: the folder Share screen is "Manage Folder Access" with a role spinner offering Viewer, Editor and Owner. An inherited row shows "Has access through the parent folder" and lower roles are locked (DRV `presentation/share/ShareFragment.kt:1055-1105`). The FAB inside a shared folder follows `openFolderCanEdit` (`HomeViewModel.kt:2301-2305`, `HomeFragment.kt:404`).
- Partial today: `DriveTwoDeviceE2ETest` "B uploads INTO A's shared folder" runs `assumeTrue(folder.openFab())`. It only covers the Editor's positive path and skips instead of failing when the FAB is missing. It never asserts Move, Share or Delete absence, and A only sees the file after reopening, not live.
- **Proposed** (suite `DriveTwoDeviceE2ETest` or `DriveFolderShareTest`):
  1. A shares `QA_FRole_<stamp>`, which holds the subfolder `child`, with B as **Viewer**. On B, under Shared with me, Folders: the row menu holds only "Add to favorites" and "Details". Open the folder and assert `fabAdd` is absent.
  2. A raises B to **Editor**. On B, `fabAdd` is present inside the folder; the row menu has Edit Info, with **no Move, Share or Delete**.
  3. A raises B to **Owner**. On B, the row menu has Share.
  4. On A, open the Share screen of `child`. Assert B's row reads "Has access through the parent folder", the roles at or below the inherited one are disabled, and there is no remove (×) control.
  5. Lower B Owner → Editor → Viewer and assert Share, then `fabAdd`, disappear (refresh between steps).

### M10a — Losing access live (revoke while open) · **High** · 3 tests
- Web: "A revokes B while B is inside the folder: … back at the Shared with me root (ZL-21815)" L1804; "a file shared with B appears in B's open list live, and goes live when A revokes it" L1810; "removing B (one of two people) saves a shorter list; B's row goes without a reload" L614.
- Applicable: on a `FolderUnshared` or `AccessChanged` socket event while inside the folder, Android shows the toast "You no longer have access to this folder." and finishes `FolderActivity` (DRV `presentation/home/HomeFragment.kt:828-836`; `domain/model/DriveSocketEvent.kt:17,26,33`). The Share screen has a per-person remove control ("Remove access for %1$s?", `ShareFragment.kt:859-866`).
- Partial today: `DriveTwoDeviceE2ETest` "A REVOKES B's access" clears all of B's rights, then checks B's list **after a pull-to-refresh**. With only one grantee, it says nothing about live removal or a list of two.
- **Proposed** (suite `DriveTwoDeviceE2ETest`): A shares folder F with B **and a third user** (set `ZILLIT_USER_C`). On B, open F (`FolderActivity`). On A, open Share for F and tap B's remove (×), then Remove. On B, **without touching the screen**, within 30 s assert B is back on home under Shared with me and F is not listed. On A, reopen Share and assert only the third user remains.

### M10b — Live updates between two devices (socket) · **Medium** · 13 tests
- Web: L625 A's rename reaches B live; L631 A deletes a shared folder and B's row goes, then restore brings it back; L637 a comment A adds shows in B's open Info and goes when deleted; L1816 a comment edit is live; L1264 B renames a file in A's folder and A follows live; L1270 B deletes a file while A has its Info open, and a late comment is refused; L1276 A moves a folder out of the shared parent; L1282 A and B rename the same folder at once; L1288 lowering B Editor → Viewer while B uploads takes the add actions away; L1295 lowering B to View while B is in the editor; L1822 a bulk delete leaves B's list live; L1828 a subfolder A creates appears live; L1834 a subfolder B makes in A's folder opens for A.
- Applicable: Android handles `FolderCreated`, `FolderUpdated`, `FolderDeleted`, `FolderMoved`, `File*`, `BulkDeleted`, `AccessChanged` and `CommentAdded`/`Updated`/`Deleted` (DRV `domain/model/DriveSocketEvent.kt:11-38`). The file detail reloads on these (`presentation/file/FileDetailViewModel.kt:100-110`).
- **Proposed** (new two-device suite `DriveLiveSyncTest`, reusing the `DriveTwoDeviceE2ETest` setup): keep B's screen open with no refresh and give each step a 45 s poll.
  1. A renames F, and B's row shows the new name.
  2. A creates a subfolder in F while B has F open, and it appears.
  3. A bulk-deletes two files in F, and both leave B's list.
  4. A deletes F, and B's row goes. A restores F from Trash, and it comes back.
  5. With B on the file detail, A posts, edits and deletes a comment, and B's `rvComments` follows each change.
  6. With A on the detail, B deletes the file. A posts a comment and is refused (`drive_err_failed_to_add_comment` toast, or the screen pops with "File deleted").
  7. A lowers B to Viewer while B is inside F, and B's `fabAdd` disappears.
  8. B creates a subfolder in A's folder, and A can open it without the "no longer have access" toast.

### M11 — Unread badges · **Medium** · 7 tests
- Web: L620 a new share raises B's Shared with me badge and opening the folder lowers it; L1860 an unread dot on a shared file clears on preview; L1866 the Folders and Files inner tabs count a new shared folder and file; L1872 a grandchild's unread rolls up to its ancestors; L1878 a moved unread folder counts on its new parent; L1884 deleting a shared folder clears its unread; L1890 the Drive tile on Tools counts unread items.
- Applicable: rows show `tvBadgeCount` (DRV `presentation/adapter/DriveItemAdapter.kt:117-120`; LAY `item_drive_file.xml:154`). Inner-tab badges are recomputed per section (`HomeViewModel.kt` `pushDriveBadgesToUi`, called from `switchSection` at :2163-2166). Opening an item marks it read (`HomeViewModel.kt:923,3447`). The Tools tile references `DRIVE_TOOL_IDENTIFIER` (`bottomNav/tools/Tools.kt`).
- **Proposed** (`DriveLiveSyncTest`):
  1. A shares a new folder and a new file with B.
  2. On B, assert badges appear on the Shared with me section tab and the Folders and Files tabs, and the row's `tvBadgeCount` is visible.
  3. Open the folder, return, and assert its badge is gone and the Folders count dropped.
  4. Open the file detail, return, and assert the same for the Files tab.
  5. Add a file two levels deep and assert the badge rolls up to the top folder.
  6. A deletes a shared folder that is still unread, and B's badge drops.
  7. Check the Drive row on the Tools list rises and then falls.

### M12 — Drive rights from User Management (view / posting / download, admin) · **Medium** · 15 tests
- Web: L643 without the posting right, add actions hide and a guarded action asks; L1327 without Drive view, B cannot use Drive; L1333 view only; L1340 posting on, download off; L1346 download on, posting off; L1352 every right but not admin; L1358 an admin without download can still download a version; L1898 and L1904 an admin with View or Post off still gets Drive and Upload; L1910 and L1916 posting turned off while the Share or Edit Info drawer is open; L1930 without posting, the other paths ask first; and the teardown checks L1922, L1936 and L2066, "slot b is back to its admin flag".
- Applicable: without view access `DriverMainActivity` finishes (DRV `presentation/DriverMainActivity.kt:106`). Posting is observed live (`HomeFragment.kt:375-392`) and hides `fabAdd`, Share, Move, the detail's Edit, Share and Manage Access, and bulk Move. Owners without posting get "Request access from admin" (`menuRequestAccess`, `HomeFragment.kt:1527-1535`; `drive_request_access_message`). The bulk-bar note says Download is gated per item.
- **Proposed** (new two-device suite `DriveRightsTest`, with device A as project admin toggling B's Drive rights in User Management):
  1. View off: on B, Tools → Drive and assert Drive closes or is not offered.
  2. Posting off: on B, assert `fabAdd` is gone at the My Drive root; an own item's menu has no Share or Move, but does have Edit Info, Delete and "Request access from admin"; the detail has no `btnEdit`, `btnShare` or `btnManageAccess`.
  3. Posting off while B has the Share screen open: tap Save Permissions and assert the screen stays open with an error.
  4. Download off: the detail has no `btnDownload`.
  5. Make B an admin with posting and view off, and assert `fabAdd` and Drive are still available.
  6. In `@AfterAll`, restore B's admin flag and rights, and assert they match the snapshot taken in `@BeforeAll` (L1922, L1936, L2066).

### M13 — Create Folder: location, duplicates, long names · **Medium** · 5 tests
- Web: "at the root, 'Inside a folder' needs a pick, then creates there" L289 (:1434); "a second folder with the same name" L295 (:1477); "a case-only duplicate, and the same name in two folders" L1166 (:5859); "a 100-character folder name is accepted; 101 is refused" L1135 (:5738); "Create Folder refuses an empty, blank or long name and an unpicked destination" L137 (:658), which is partial.
- Applicable: at the root the sheet shows `drive_pick_root` (`DriveDestinationPicker`) with "Drive root" and "Inside a folder". Create is blocked with "Please select a destination folder" when nothing is picked (DRV `presentation/home/CreateFolderBottomSheet.kt:146-155,363-372`). A duplicate reopens the sheet with `tvFolderNameError` (`:192-200`; `HomeViewModel.kt:2683`). **There is no client length cap on the name**: only empty is checked (`:353-356`). `DriveTest` covers empty and whitespace names and asserts both location options exist, but never picks one.
- **Proposed** (suite `DriveTest`):
  1. Open Create Folder at the root, choose "Inside a folder" with nothing picked, then Create. Assert the sheet stays open and nothing was created.
  2. Pick `FOLDER_A` and Create `QA_In_<stamp>`. Assert it is absent from the root and present inside `FOLDER_A`.
  3. Create `FOLDER_A` again at the root. Assert the sheet reopens with `tvFolderNameError` visible, or, if duplicates are allowed, record it.
  4. Create the same name inside another folder and assert it is accepted.
  5. Create the name with only its case changed and record the result.
  6. Create names of 100 and 101 characters and record the server's answer. Android has no client cap, which is itself a parity gap against web ZL-21806.

### M14 — Create Folder access control (Project Users / Specific Users) · **Medium** · 4 tests
- Web: L301 Create Folder with one specific user sends one viewer entry; L1628 "Project Users" with no role keeps the folder private; L1634 "Project Users" + Editor gives one entry per user; L1640 Specific Users as Editor and Owner, and the drawer starts clean on every reopen.
- Applicable: the sheet has `rgPermissionMode` with `rbModeProject` and `rbModeSpecific`, plus `spProjectPermission` ("Select permission", Viewer, Editor, Owner), `tvProjectUsersCount` ("Applies to all %d project users with Drive view access."), `etSearchMembers`, `rvMembers` and `tvSelectedCount` (LAY `bottom_sheet_create_folder.xml:237-385`). `DriveTest` "Access Control offers…" only checks that the options exist and creates nothing.
- **Proposed** (suite `DriveShareTest`):
  1. Create `QA_Acc1` with Specific Users = `SHARE_USER` as Viewer. Open Share for it and assert `SHARE_USER` is listed as Viewer and is the only grant.
  2. Create `QA_Acc2` with Project Users chosen and no permission picked. Open Share and assert "No one has access yet…".
  3. Create `QA_Acc3` with Project Users + Editor. Assert the access list count matches `tvProjectUsersCount`.
  4. Reopen Create Folder and assert nothing is preselected (`tvSelectedCount` reads 0, and no mode or role carries over).

### M15 — Upload destination, repeat upload, staged name, duplicates · **Medium** · 6 tests
- Web: L334 three files upload into the open folder and the Operations panel tracks and clears them; L340 a second upload from the same folder also lands in it (ZL-21873); L346 at the root, "Existing folder" needs a pick, then uploads there; L358 the drawer's Name keeps the file's extension; L1223 uploading a name that already exists in the folder; L1178 a 230-character file name uploads.
- Applicable: the staging sheet has `tvDestination`, `drive_pick_root` (`drive_pick_seg_new` / `drive_pick_seg_existing`, "Please select a destination folder"), an **editable name per row** (DRV `presentation/home/UploadStagingBottomSheet.kt:487-508`), and `btnUpload`/`btnCancel`. A duplicate name opens "Resolve duplicates" / "Rename to upload" (STR `drive_resolve_duplicates_*`, LAY `bottom_sheet_resolve_duplicates.xml`). Android **does not keep both files** as web does; record this. The uploads sheet is `bottom_sheet_upload_progress.xml` (`tvHeader` "Uploads · N", `btnCancelAll`).
- **Proposed** (suite `DriveUploadTest`):
  1. Inside `FOLDER`, upload `zl_drive_doc.pdf`, then upload `zl_drive_pic.jpg` straight after. Assert both are inside the folder and neither is at the root.
  2. At the root, in staging, pick "Existing folder" with nothing chosen and tap Upload. Assert staging stays. Pick `FOLDER`, upload, and assert the file is inside it.
  3. In staging, change the row name to `QA_named.v2` and upload. Assert the listed name is `QA_named.v2.pdf`, with the extension kept.
  4. Upload `zl_drive_doc.pdf` again into the same folder. Assert the "Rename to upload" sheet appears, rename it, submit, and assert both rows are present.
  5. Optional: push a fixture with a 230-character name and assert it lists, recording the stored length.

### M16 — Folder upload really creates the tree · **High** (352) / Low · 3 tests
- Web: "a folder upload creates the tree first, then uploads into it" L352 (:1739); "a folder upload 7 levels deep…" L1199 (:5978); "uploading the same folder twice" L1976.
- Applicable: `menuUploadFolder`, then `UploadFolderStagingBottomSheet` shows the summary "%d folders · %d files · %s", then "Creating folder structure…" and "Folders ready — starting upload of %d file(s)…" (STR 4175-4178; DRV `util/FolderTreeUploader.kt`).
- Partial today: `DriveUploadTest` "UPLOAD FOLDER opens the folder picker and stages a folders/files summary" (T `DriveUploadTest.java:278-307`) has **no assertion**. It only prints the summary and backs out.
- **Proposed** (suite `DriveUploadTest`): pick the seeded `zl_drive_tree` (with a 7-level variant). Assert the staging summary matches the fixture's counts, then Upload. Open `zl_drive_tree`, walk each level, and assert every subfolder and the deepest file exist. Upload the same tree again and record whether a second `zl_drive_tree` appears or a duplicate error shows.

### M17 — Upload lifecycle: cancel, retry, background · **Medium** · 3 tests
- Web: "pause, resume and cancel a large upload" L802 (:4214); "an interrupted upload is offered for recovery and resumes" L808 (:4243); "an upload keeps going after leaving Drive" L814 (:4282).
- Applicable: rows show `tvPendingChip` ("Queued", "Uploading…", "Posting…", "Failed") and `btnRetry` (LAY `item_drive_file.xml:205-240`). The Uploads sheet has per-row `btnAction` (cancel or retry) and `btnCancelAll` "Cancel all" (DRV `presentation/upload/UploadProgressBottomSheet.kt:41-103`). Uploads run in a worker (`data/worker/UploadWorker.kt`). Pause and resume exist only on the hidden Uploads screen (`item_upload.xml` `btnPauseResume`; `navUploads` is `gone`), so web's pause and resume are not reachable.
- Partial today: `DriveUploadTest` "RETRY…" asserts only that some offline feedback appears ("No internet", "Failed", "Queued" or "connect"). It never retries to completion.
- **Proposed** (suite `DriveUploadTest`):
  1. Go offline and start an upload. Assert the row shows "Failed" or "Queued". Go back online and tap `btnRetry`. Assert the chip clears and the file is listed.
  2. Start a large fixture upload, open the Uploads sheet and tap Cancel. Assert the file never lists.
  3. Start an upload, press back out of Drive to Tools, wait, reopen Drive, and assert the file is listed.

### M18 — Opening each file type · **Medium** (PDF High) · 7 tests
- Web: L392 a PDF opens in the PdfViewer with Download; L380 an image opens with its size and Download; L386 audio opens with a player; L2058 an mp4 previews in a video player; L707 a .csv opens in the sheet editor; L398 an RTF opens in the document viewer; L701 an Office Preview opens view-only, and Edit reopens it for editing.
- Applicable: `btnOpen` routes PDFs to `DrivePdfViewerActivity` and media to the media viewer (DRV `presentation/file/FileDetailViewModel.kt` `openFile`; `presentation/viewer/DrivePdfViewerActivity.kt`). Audio and video play inline in `playerView`. Images show in `ivFileThumbnail` with `tvFileSize`. csv and rtf are **editable** types (DRV `util/FileUtils.kt:82-91`), so `btnOpen` opens `OnlyOfficeEditorActivity` with `MODE_VIEW`, and the Edit switch is offered when the user may edit (`FileDetailFragment.kt:191-204`; `OnlyOfficeEditorActivity.kt:369`). **No test taps `btnOpen`.**
- **Proposed** (new `DrivePreviewTest`):
  1. PDF detail: tap `btnOpen` and assert the current activity is `DrivePdfViewerActivity`. Press back and assert the detail is shown.
  2. `zl_drive_pic.jpg` detail: assert `ivFileThumbnail` is visible, `tvFileSize` is not blank and `btnDownload` is present.
  3. An mp3 or mp4 fixture: assert `playerView` is visible.
  4. A csv fixture: tap `btnOpen` and assert `OnlyOfficeEditorActivity` loads read-only (`DriveEditorPage.isReadOnly()`).
  5. xlsx: tap `btnOpen`, assert read-only, then tap the editor's Edit and assert the edit toolbar appears.

### M19 — Move: file move, bulk move, move to root · **Medium** · 3 tests
- Web: L430 "Move to… hides the moved folder and everything below it, and moves a file (REL-BUG-17)"; L436 "bulk bar: folders-only has no Download; bulk move; bulk delete"; L1736 "Move to… 'Drive (Root)'…".
- Applicable: `FolderPickerBottomSheet` excludes the moved ids (`excludeIds`, DRV `presentation/home/FolderPickerBottomSheet.kt:139`). Its button reads "Move to %1$s", with the root label at `:76-80,173-178`. Bulk Move uses `btnBulkMove` and `showFolderPickerForBulkMove`. The toasts are "Item moved" and "Items moved". On Android, bulk **Download is not hidden** for a folders-only selection; it gives "No files to download" instead (`HomeViewModel.kt:2501-2504`).
- Partial today: `DriveOrganiseTest` MOVE moves a **folder** only, and never checks the picker hides the moved folder. BULK deletes but never moves.
- **Proposed** (suite `DriveOrganiseTest`):
  1. Open Move for `MOVER` and assert `MOVER` is not listed in `rvFolders`.
  2. Move the file `zl_drive_doc.pdf` into `TARGET` and assert it is inside.
  3. Select two folders, tap `btnBulkMove`, pick `TARGET` and Move Here. Assert both are inside `TARGET`.
  4. From inside `TARGET`, move one item and, at the picker root, tap "Move to My Drive". Assert the item is back at the root.

### M20 — Favourites filter and row star · **Medium** · 3 tests
- Web: L112 "the Starred filter shows only starred rows, or says none are starred (ZL-21810)"; L451 "star from the row and the menu…"; L834 "Starred with nothing starred says so in grid view too".
- Applicable: rows have `btnFavorite`; `btnFavoritesToggle` filters; the menu label flips to "Remove from favorites"; the empty state reads "No favorites yet" / "Star files and folders to find them here" (DRV `presentation/home/HomeFragment.kt:1432-1437,971-973`).
- Partial today: `DriveViewTest` "VIEW: the favourites-only toggle…" only asserts the favourites list is ≤ all, and that F1 is in it **if it is not empty**. It never asserts `F2` is excluded, never uses the row star, and never checks the label flip.
- **Proposed** (suite `DriveViewTest`):
  1. Tap the row `btnFavorite` on `F1`. Open its menu and assert "Remove from favorites".
  2. Toggle favourites. Assert the list contains `F1` and does **not** contain `F2`.
  3. Unstar everything and toggle favourites. Assert the empty title "No favorites yet" in list view, then again after `btnViewToggle` in grid view.

### M21 — Details content (folder sheet and file detail); DRIVE-01 · **Medium** · 2 tests
- Web: L457 "Info shows details, access, tags, comments and activity; its Favorite button toggles"; L10 (sheet-bugs:66) "Folder Info never reports '0 files, 0 folders'…".
- Applicable: the folder details sheet has `tvFolderName`, `favoritePill`, `tvOwner`, `tvAccessHeader` "Access (N)" with `rvAccess`, `tagChipGroup` and `rvActivity` (LAY `bottom_sheet_folder_details.xml`). Folder counts live in the row subtitle (`DriveItemAdapter.kt:299-316`). `DriveTest` "Details shows…" only asserts the name and a created date (owner, access and activity are only printed).
- **Proposed** (suite `DriveTest`):
  1. In `FOLDER_A`, create two subfolders and upload two files. Back at the root, assert `FOLDER_A`'s row subtitle contains "2 files, 2 folders" and never "0 files".
  2. Favourite it and open Details. Assert `favoritePill` "Favorited" is visible, `tvOwner` is not blank, the access header and the activity list (or "No activity yet") are shown, and `tvNoTags` or chips are shown.

### M22 — Shared with me presentation · **Medium** · 4 tests
- Web: L72 "My Drive shows the add actions and quick filter; the Shared root does not"; L608 "Shared with me root lists the folder B can open, not its ancestor (REL-BUG-05)"; L1770 "Shared with me: the 'Shared with you' icon, and a re-share reads 'Shared by X · Created by Y'"; L1319 "B's star is B's own; B's search never finds A's private folder".
- Applicable: at the Shared root `canAddToOpenFolder()` is false, so there is no `fabAdd` (DRV `presentation/home/HomeViewModel.kt:2301-2305`), and `myDriveFilterRow` only shows on My Drive (`:1058-1066`). Rows show `ivSharedIndicator` (content description "Shared with you") and `tvSharedBy` "Shared by %1$s". The detail shows `rowSharedBy` (`FileDetailFragment.kt:340-355`). The Shared root lists only folders B can open (`HomeViewModel.kt:1032-1047`).
- **Proposed** (suite `DriveSharedWithMeTest`, plus two-device steps):
  1. On the Shared with me root, assert `fabAdd` and `spMyDriveFilter` are absent; on My Drive, assert both are present.
  2. Assert a shared row has `ivSharedIndicator` and a `tvSharedBy` "Shared by <owner>", and the detail shows `rowSharedBy`.
  3. Two devices: A shares only the child `P/C` with B. On B, assert `C` is listed and `P` is not.
  4. B stars the shared file, and on A it is not starred.
  5. On B, search for A's private folder name and assert no result.

### M23 — My Drive "Show" filter and selection reset on section switch · **Medium/Low** · 2 tests
- Web: L107 "'Shared by Me' … resets on a section switch"; L1712 "switching the Folders/Files tab or the section clears the selection".
- Applicable: `spMyDriveFilter` offers All and "Shared by Me" (`HomeViewModel.kt:1049-1070`). `switchSection` resets it to All and clears the selection (`:2150-2162`). `DriveViewTest` "FILTER: the 'Show' filter…" only asserts it is present.
- **Proposed** (suite `DriveViewTest`):
  1. Pick "Shared by Me". Assert only items you shared remain, using `QA_Shared_*` from `DriveShareTest`, and that unshared `F1` is gone.
  2. Switch to Shared with me and back. Assert the spinner reads "All" and `F1` is back.
  3. Select one item, switch section, and assert `bulkActionBar` is gone.

### M24 — Sort direction toggles · **Low** · 1 test
- Web: L92 "column sort … cycles asc → desc → off".
- Applicable: choosing the active sort option again flips asc and desc (DRV `presentation/home/HomeFragment.kt:2697-2699`). Android has no "off" state. `DriveViewTest` "SORT by Name" accepts either order and never re-picks.
- **Proposed** (suite `DriveViewTest`): sort by Name and record the order. Sort by Name again and assert the order is exactly reversed.

### M25 — Special-character names and search · **Low** · 4 tests
- Web: L1141 a unicode, emoji and RTL name; L1147 HTML in a name is shown as text; L1153 path-like names; L912 search takes special characters, RTL and emoji.
- **Proposed** (suite `DriveViewTest`): create folders `QA_é😀_مرحبا_<stamp>`, `<b>QA</b>_<stamp>` and `../QA_<stamp>`. Assert each lists exactly as typed (or record a server refusal), opens with `FolderActivity` showing the same name in its header, and is found by drawer Search with that string.

### M26 — New-folder privacy and nesting details · **Low** · 2 tests
- Web: L277 "a new folder lands in the open folder, private ('Only you'), dated in the past"; L283 "nested folders: no Location inside a folder, crumbs, crumb click and back arrow".
- Partial today: `DriveShareTest` "A newly uploaded file starts with NO ONE having access" (T `DriveShareTest.java:155-165`) **asserts nothing**; it only prints `noAccessYet()`. Inside a folder the Location picker is hidden (`CreateFolderBottomSheet.kt:148-154`), but this is never asserted.
- **Proposed:** in `DriveShareTest`, change that case to `assertTrue(share.noAccessYet())` and add the same check for a new folder. In `DriveTest`'s nested-folder case, assert `drive_pick_root` is not displayed in the sheet opened inside `FOLDER_A`.

### M27 — Share screen details: empty save and folder roles · **Low/Medium** · 2 tests
- Web: L484 "the file Share drawer: tabs, grid, level rules, and no empty save"; L491 "a folder's owner sees Share; the drawer is 'Manage Access' with roles".
- Applicable: the screen title is "Manage File Access" or "Manage Folder Access"; `saveBar`/`btnSave` "Save Permissions" only appears after a change (LAY `fragment_share.xml:603-617`). Folder rows have the Viewer, Editor and Owner spinner.
- **Proposed** (suite `DriveShareTest`): on the folder Share screen, assert the title is "Manage Folder Access" and that after adding a user the role spinner offers exactly Viewer, Editor and Owner. On the file Share screen with no change, assert `saveBar` is hidden.

### M28 — Link by email: recipients and button label · **Low** · 3 tests
- Web: L542 "'Send link by email' emails the recipient" (real send); L1582 "… recipients split on space, comma and semicolon; invisible characters stripped (ZL-21475)"; L1089 "empty comments and blank tag names cannot be sent; the link button reads by recipients". Empty comments are already covered by `DriveCommentsTest` P-2.
- Applicable: `btnGenerateEmailLink` reads "Generate link" with no recipients and "Send link by email" with recipients (DRV `presentation/share/ShareFragment.kt:310-313`). The result is "Link sent to %1$d recipient(s)". In the Create Tag dialog a blank name is ignored (`FileDetailFragment.kt` `showCreateTagDialog`, `name.isNotEmpty()`).
- **Proposed** (suite `DriveShareTest`):
  1. With `etRecipients` empty, assert the button reads "Generate link".
  2. Type `a@x.com, b@x.com;c@x.com d@x.com` and assert it reads "Send link by email". Send to a test inbox and assert the listed link shows 4 recipients.
  3. In Create Tag, enter three spaces and tap Create. Assert no chip was added.

### M29 — File requests: email result and recipient normalisation · **Low** · 2 tests
- Web: L575 "a request emailed to a recipient"; L1562 "from a folder's menu: … recipients lower-cased and de-duplicated; a partial email result warns".
- Applicable: `tvEmailInvitesHeader` and `emailResultsContainer` on the success card, plus `drive_request_email_not_sent` (LAY `bottom_sheet_request_files.xml:342-353`).
- **Proposed** (suite `DriveRequestFilesTest`): add the recipients `QA.User@Example.com` and `qa.user@example.com`, then Create. Assert `createdChips` or the email results list the address **once**, in lower case, and that the Email invites section shows a sent or failed status for it.

### M30 — Permissions chosen at upload · **Medium** · 3 tests
- Web: L1944 "the drawer's File Permissions lists a folder member first at their role and sends a higher level"; L1950 "… a folder Editor is never offered less; Project Users sends everyone else"; L1956 "a file uploaded with B at Download reaches B at exactly Download".
- Applicable: in staging, "File Permissions" has `rgPermissionMode`, `spProjectPermission` and `rvMembers`, with `tvFolderMembersNote` "People with access to this folder keep it on every file inside. You can give them more here, not less." (LAY `bottom_sheet_upload_staging.xml:188-287`; STR 4527).
- **Proposed** (suite `DriveTwoDeviceE2ETest`):
  1. Upload with Specific Users = B at **Download**. On B, the detail has `btnDownload` but no `btnEdit`, `btnShare` or Edit Info.
  2. Upload into a folder where B is Editor. Assert `tvFolderMembersNote` is visible, B is listed first, and B's level cannot be set below Edit.

### M31 — Network-failure paths reachable with airplane mode · **Medium/Low** · 7 tests
- Web: L171 a failed list load is reported, not shown as an empty Drive; L178 a failed trash load; L932 a network abort on the list; L945 after a failed load the next refresh recovers; L951 a preview that never answers; L957 an editor config that fails; L221 going offline keeps the editor open.
- Applicable: errors come back through `state.error` as toasts ("Failed to load contents", "Failed to load trash" at `TrashViewModel.kt:62-66`, "Failed to load editor"). Lists are cached in Realm (`data/local/DriveLocalDB.kt`), so an offline refresh should keep the cached rows rather than show an empty Drive.
- Partial today: `DriveUploadTest` "After reconnecting, the Drive list still loads" only asserts a readable list after reconnecting; it never fails a load first.
- **Proposed** (suite `DriveUploadTest`, reusing `setAirplaneMode`):
  1. With rows on screen, go offline and pull to refresh. Assert the rows are still listed, not the "No folders yet" empty state.
  2. Open Trash offline and assert it does not show "Trash is empty" when it held items before.
  3. Tap `btnOpen` on an uncached PDF offline. Assert no viewer opens and the detail remains.
  4. Tap `btnEdit` offline. Assert the editor shows its failure or leaves, with no crash.
  5. While in the editor, go offline for 10 s and back. Assert `OnlyOfficeEditorActivity` is still in front.
  6. Go back online, pull to refresh, and assert the rows are there.

### M32 — Server-refusal paths that need a stub or proxy · **Low** · 33 tests
These apply on Android because the feature exists and has an error branch. Appium cannot make the server refuse on demand, so they need a mock server or proxy (for example mitmproxy with a rule file), or unit or instrumented tests in the app repo. Tests: L185 403 on the whole Drive; L191 403 on an opened folder; L197 a failed preview; L203 a file the editors cannot open; L233 a refused upload start; L245 a refused move; L251 a refused share; L841 a refused star toggle; L848 a refused bulk delete; L855 a refused "Generate link"; L862 a refused file request; L925 a list failing with 404; L939 a slow list shows the spinner; L963 a failed access prefill; L970 the share-eligible list fails; L976 Move to… list fails; L983 the favourites load fails; L989 tags fail to load; L995 a refused Restore or Delete forever in Trash; L1482 a refused version restore; L1498 a refused comment; L1504 a refused comment edit; L1530 a refused Empty trash; L1542 a refused single delete; L1568 a refused request revoke; L1588 a refused link revoke; L1674 and L1680 a failed next page; L1686 and L1692 a late answer for a section or folder the user already left; L2024 a refused detail Download; L2030 a refused preview Download; L1964 a folder upload whose top folder is refused.
Notes from source for whoever builds the stubs:
- `toggleFavorite` reverts the star on failure (`HomeViewModel.kt` toggleFavorite, "revert on error"). That is the opposite of web known bug L841, and worth locking in.
- `bulkDelete` reports failures (`HomeViewModel.kt:1744-1746`), unlike the web known bug L848.
- `submitComment` clears the composer **before** the server answers (`FileDetailFragment.kt` submitComment), so web L1498 "the text is kept" fails on Android. This is likely an app bug.
- On a folder upload whose folders cannot be created, Android says "Some folders could not be created (%d). Files inside them have been marked for retry." (STR 4177). Web instead uploads into the open folder, so the behaviours differ.
- Android sorts out a late answer with `loadJob?.cancel()` in `switchSection` (`HomeViewModel.kt:2143`).

### M33 — Smaller gaps · **Low** · 10 tests
- L62 "'Back to Film Tools' leaves Drive": in `DriveTest`, press back from Drive home and assert `ToolsPage.isLoaded()`. Android's equivalent is the system back from `DriverMainActivity`.
- L902 "Create Folder caps the description at 500 and shows the count": `etDescription` has `maxLength="500"` (LAY `bottom_sheet_create_folder.xml:130`). Type 510 characters and assert `tvCharCount` reads "500 / 500" and the field holds 500. `DriveTest` "Description counter…" only types 11 characters.
- L1127 "grid cards: … ⋯ menu, checkbox selection, … opens": `item_drive_file_grid.xml` has `btnMore`, `btnFavorite` and `cbSelect`. In grid view, open a card's ⋯ menu (assert "Details"), select two cards (assert "2 selected"), and tap a folder card (assert `FolderActivity`).
- L373 "search … opening a folder from results": tap a folder result on the drawer Search screen and assert `FolderActivity` with the folder name in its header. Android search is not section-scoped and has no crumbs.
- L475 "the Activity Log lists this run's upload": in `DriveActivityTest`, upload a file and assert an entry contains its name. M-2 only covers folder creation.
- L1516 "a comment over 2000 characters": `etComment` has no limit. Post 2,001 characters and record whether the comment lands or "Failed to add comment" shows.
- L1217, L1662, L1668 (paging): Android pages at 50 (`HomeViewModel.kt:262`, `PAGE_SIZE = 50`). Seed 55 or more files, scroll to the end, and assert all are listed once. Sort by name and assert the count is unchanged.
- L1718 "bulk Delete and Move on mixed rights…": in a folder holding items owned by A and B, select both on B. Bulk Move is hidden under Shared with me; bulk Delete reports what was left out (record the message).

---

## COVERED

| Web test(s) | Android test (asserts the web outcome) |
|---|---|
| L35 clicking an editable file shows the actions (not the editor) | `DriveEditorTest` "An EDITABLE file (xlsx) shows the Edit button on its detail screen". On Android, a tap opens the detail and not the editor. |
| L40 an Office file opens in the live editor | `DriveEditorTest` "Edit opens the Collabora editor activity and loads the document". The token-in-URL part is WEB-ONLY: Android passes `access_token` by design (`OnlyOfficeEditorActivity.kt:41-43,398-401`). |
| L45 the editor's Back returns to the file list | `DriveEditorTest` "BACK raises a 'Leave Editor' confirmation…" and "LEAVE returns to the file detail screen" |
| L57 Drive opens from the Film Tools tile | `DriveTest` "Drive opens from Tools → ADMIN → Drive (DriverMainActivity…)" |
| L77 inner tabs split folders and files | `DriveViewTest` "KIND: Folders and Files tabs show disjoint content"; `DriveUploadTest` "An uploaded file does NOT appear on the Folders tab". Hiding during search is n/a: Search is its own screen on Android. |
| L82 open a folder, then climb out by the back arrow | `DriveTest` "Opening a folder pushes FolderActivity…" and "BACK from a folder returns to the Drive root list". Crumbs are WEB-ONLY. |
| L97 list ⇄ grid toggle | `DriveViewTest` "VIEW: the list/grid toggle is present and switches the layout" |
| L102 search shows "No results" | `DriveViewTest` "SEARCH: a non-matching query shows the empty state" and "SEARCH: an exact folder name is found" |
| L122 Activity Log: refresh and close | `DriveActivityTest` M-1 and M-3; `DriveSettingsTest` N-2. Chips are WEB-ONLY: `fragment_activity.xml` has none. |
| L147 the upload drawer uploads nothing on Cancel | `DriveUploadTest` "Cancelling the staging sheet uploads nothing". The unsupported-file list is WEB-ONLY, and per-row remove was dropped by design (`UploadStagingBottomSheet.kt:511-514`). |
| L152 Request files needs a title | `DriveRequestFilesTest` S-3. The "picked folder at the root" part is WEB-ONLY: Request Files is folder-only on Android, asserted by `DriveTest` "The add FAB offers…" (`assertFalse(fabHasRequestFiles)`). |
| L159 an empty My Drive shows the empty state | `DriveTest` "An empty Drive shows its empty state, otherwise the list renders items". The hint links are WEB-ONLY. |
| L165 Shared with me says nothing is shared | `DriveSharedWithMeTest` "An empty 'Shared with me' explains itself". The landing part is WEB-ONLY. |
| L367 each item type gets its own action set | `DriveTest` "Row overflow offers Favorite / Share / Move to… / Edit Info / Details / Delete"; `DriveEditorTest` cases 1-2 (Edit for xlsx, none for pdf) and "WORD: a .docx also shows the Edit button" |
| L469 comments: add, edit, delete | `DriveCommentsTest` P-3 (with a reopen round trip), P-4, P-5 |
| L497 sharing a file at Download level | `DriveShareTest` "SHARE a file as VIEW + DOWNLOAD…" and "Saved rights persist…"; `DriveTwoDeviceE2ETest` cases 1-3 (B receives the file with no Edit Info, Move or Delete) |
| L530 link form rejects a bad email | `DriveShareTest` "Invalid recipient emails are rejected…". The "Can edit" part is WEB-ONLY. |
| L556 a request from inside a folder: created, copied, revoked | `DriveRequestFilesTest` S-6, S-7, S-9, S-10, S-11. The outsider upload is WEB-ONLY (public page). |

## WEB-ONLY

| Web test(s) | Reason (Android evidence) |
|---|---|
| L25, L30, L67: landing on Shared with me and remembering the section | Android always opens on My Drive: `driveSection = DriveSection.MY_DRIVE` (DRV `presentation/home/HomeViewModel.kt:65`). Nothing chooses Shared with me on landing or restores a section. If ZL-21229 is meant to apply to Android too, it is a product gap to raise. |
| L16 (sheet-bugs:116, DRIVE-02) starred folder breadcrumb | The breadcrumb bar is commented out (`HomeFragment.kt:1031,1323`); folders open as a `FolderActivity` with a header title. The Favorites screen is hidden (`fragment_drive_drawer.xml:146-153`). |
| L52 opens by route (window, banner, landing requests); L1109 Classic layout; L87 the row menu stays inside the window and closes on Escape | MDI windows, URL routes and the keyboard are browser-only. Android uses activities and bottom sheets. |
| L132, L1097, L1103 theme toggle, dark and light drawers | There is no theme control in Drive (`fragment_drive_home.xml` header: `btnDrawer`, `tvTitle` only). The app follows the system theme. |
| L1115, L1121 phone and tablet widths | Browser viewport checks. |
| L142, L320, L326, L695, L1184, L1211, L897 New-file menu (Markdown, Text, Document), new-file name rules | The add menu has only Upload File, Take Photo, Upload Folder, Create Folder, Request Files and View File Requests (LAY `bottom_sheet_add_menu.xml:10-92`). There is no create-file entry. |
| L227, L1083, L1205, L1430, L1436, L1442, L1450, L1456, L1462, L1468, L1490, L1840, L1846 Markdown editor, its versions and its conflicts | There is no Markdown editor: `.md` is handed to an external app via `ACTION_VIEW` (`FileUtils.kt:78-79`; `FileDetailViewModel.kt` openFile). There is no `drive_version_created` socket event (`DriveSocketEvent.kt`). |
| L404, L410, L416, L794 "type the browser cannot show", Open in new tab, Markdown and .txt viewers | Non-PDF, non-media and non-Office files go to an external app via `ACTION_VIEW` (`FileDetailViewModel.kt` openFile; "No app available to open this file type"). |
| L209, L215, L1366, L1372, L1378, L1384, L1390, L1396, L1402, L1408 editor host messages (`zdrive:*`), failed pane, 30 s ready timeout | The Android editor is a full-screen WebView with "No JavaScript bridge" (`OnlyOfficeEditorActivity.kt:41-43`). Token expiry re-opens on a TTL timer (`:429-437`), not on a host message. |
| L713, L719, L732, L1416, L1422, L661, L655, L649 editor chrome, version naming, "Let others know" notify prompt, restore-reload notice, presence chips | There is no host header, notify prompt or presence UI on Android: no notify or presence strings in STR or `presentation/editor/`. Leaving shows only "Leave Editor" (`drive_leave_editor_message`). |
| L239, L1001, L1007 ETag, complete retries, chunk 503 retries | Browser multipart pipeline. Android uploads through the legacy S3 helper and then `createFile` (`HomeViewModel.kt:3683-3700`). |
| L257, L263, L269, L1015, L1021, L1027, L1033, L1039, L1602, L1608, L1614, L1620, L744, L788, L820, L826, L548 public share and upload pages (outsider) | Public web pages viewed by people without accounts. Android only creates and revokes the links. |
| L518, L1594 clipboard refusal | A browser permission. Android's `ClipboardManager` cannot refuse (`FileDetailFragment.kt:150-155`). |
| L562, L1550, L1556 Request files at the root ("existing folder" / "a new folder") | Request Files is folder-only: `menuRequestFiles` is `gone` at the root (`bottom_sheet_add_menu.xml:64-74`), asserted by `DriveTest` "The add FAB offers…". |
| L569 request rules enforced (Images only, 1 file) | Enforced on the public uploader page. Android only sets them (`spFileType`, `spMaxFiles`). |
| L907 request title 200 / instructions 2000 caps | No `maxLength` on `etTitle`/`etInstructions` (`bottom_sheet_request_files.xml:94-107`). There is no client cap. Raise as a parity issue if wanted. |
| L1576 "Can edit" offered for xlsx and docx links | The link permission spinner offers only "View only (no download)" and "View + download" (`ShareFragment.kt:255-258`). |
| L669, L675, L682, L688, L1778, L1784, L1790, L1796 drag and drop, page drop | Desktop drag and drop. |
| L871, L876, L881, L887, L892, L1192, L1970, L1984, L1990, L1996, L2002 picker extension rules, 100-file cap, 0-byte and 10 GB limits, nothing-chosen Upload, dropped folders | Android uses the system picker, and staging is only reached with files. No count, size or extension limits in `UploadStagingBottomSheet.kt` or `HomeViewModel.kt` (searched for `MAX_FILES`, `GB`, `unsupported`, `size == 0`). Folder upload skips empty files itself (STR 4181). |
| L764, L2036 folder downloads as a .zip | There is no folder download: the row menu has no Download (`bottom_sheet_context_menu.xml`), and bulk download sends file ids only (`HomeViewModel.kt:2494-2500`). |
| L2010, L2016 browser-built zip, save dialog | Chrome File System Access API. |
| L2044, L2050 Docs & Media Cabinet copies of downloads | No cabinet hand-off in the Drive package (searched for `cabinet` under DRV). |
| L917, L1231, L1238, L1256 saved path or URL restore, stale crumbs, reload mid-folder | No URL or saved path, and no crumbs. Each folder is its own `FolderActivity` (`HomeFragment.kt:1323` crumbs commented out). |
| L1244 deleting the open folder (found by search) | There is no action to delete the folder you are inside: the `FolderActivity` header has no menu (only `btnDrawer`/`tvTitle` in `fragment_drive_home.xml`). Deletion is from the parent row. |
| L1250 switching section inside a folder | Section tabs are hidden in subfolder mode (`HomeFragment.kt:461,720`, `tabSectionContainer` gone). |
| L1536 trash in grid view | Trash is list-only (`fragment_trash.xml`, single `recyclerView`; `TrashItemAdapter`). |
| L1648 Activity Log Files/Folders chips, Today/Yesterday groups; L1654 Activity Log paging | `fragment_activity.xml` has no chips. `ActivityViewModel.kt:50` calls `getActivity()` with no offset or limit. |
| L1700, L1706 header select-all checkbox, selection across table pages | There is no select-all control in `bulkActionBar` (`fragment_drive_home.xml:381-454`). Android scrolls infinitely, with no pages. |
| L1744 one search request after the debounce | Network-level, so Appium cannot see it. Android debounces 300 ms (`SearchViewModel.kt:81`). |
| L1764 My Drive Sharing column ("Only you", avatars, +N) | There is no sharing column on Android rows (`item_drive_file.xml`: only `ivSharedIndicator`). |
| L1854 `notification:read` vs `notification:level:read` | A socket-payload check. Android deliberately emits `notification:level:read` with the ancestor chain (`HomeViewModel.kt:923,3447-3463`). Worth confirming with the backend that this is not the ZL-18873 regression on Android. |

Total WEB-ONLY rows add up to 121 tests (checked against the line list).
