# Costume (Main Cast) / Costume (Background Cast) — test cases and findings

Tools → **Costume (Main Cast)** and **Costume (Background Cast)** are the app's *Wardrobe* tools
(`WARDROBE_MAIN_TOOL` / `WARDROBE_BACKGROUND_TOOL`). Both tiles open the same activity,
`bottomNav/tools/wardrobe/WardrobePage`, with a different tool identifier; everything below holds
for both, and the suite runs every case once per tool. They are **not** "Costumes & Set Sync"
(`CssActivity`, `COSTUMES_SET_SYNC_TESTCASES` — another suite).

Source read: `app/src/main/java/com/zillit/zillitapp/bottomNav/tools/wardrobe/` (WardrobePage,
WardrobeCharacterListingPage, WardrobeSceneNoPage, WardrobeListingPage, AddEditWardrobePage,
ChooseCastPage, WardrobeVM, adapters) plus `utils/HierarchyUpdateBottomSheetDialog.kt`,
`casting/CastingDetailPage.kt` (temporary character form) and `otherchats/OtherChatsActivity.kt`.

**Status:** see *Run results* at the end.

Suites: [`CostumeTest`](src/test/java/com/zillit/automation/tests/costume/CostumeTest.java)
(`TestOrder.COSTUME = 235`, single device, parameterised over both tools) and
[`CostumeTwoUserTest`](src/test/java/com/zillit/automation/tests/costume/CostumeTwoUserTest.java)
(`TestOrder.COSTUME_TWO_USER = 236`, emulator + Pixel). Page objects in
`src/test/java/com/zillit/automation/pages/costume/` (`CostumePage`, `CostumeFormPage`).

```bash
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ./run-and-report.sh --only CostumeTest
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only CostumeTwoUserTest
```

---

## The shape of the module

```
WardrobePage  (header = tool name; tabs Selects | Shortlist | Final)
  search "Search by Talent Name/Character", filter icon, unit-chat icon (Selects/Shortlist only),
  FABs "+" (takeSceneShots) and camera (uploadFromCam)
  └─ folder rows grouped by the filter "Group by":
       TV project (this one) default  "Episode No : <n>"   → WardrobeCharacterListingPage
       Character                        "Character: <name>"  (detail: Talent Name / Scene No. / Gender)
       Scene No                         "Scene No. <s>"      (detail: Character / Talent Name / Gender)
        └─ "Character: <name>" → (one scene) WardrobeListingPage | (several) WardrobeSceneNoPage
             └─ records (library_item): Talent Name / Link / Uploaded by, Description, chat icon, ⋮
```

A record = attachment (photo / video / link) + optional character (from the matching Casting
tool's **Final** list, or a *temporary* character typed in on a Selects create) + episode (TV) +
scene + description. **Costume ↔ Casting:** the character picker (`ChooseCastPage`, header
"Casting") lists Casting (Main Cast) Final for Costume (Main Cast) and Casting (Background Cast)
Final for Costume (Background Cast) — that is the only link. The suite never creates or uses a
Casting record: it uses temporary characters (kept inside the costume record only;
`CastingDetailPage` with `HIDE_UPLOADING_ICON` returns `casting_info_data` and saves nothing to
Casting).

### Main vs Background — what differs
| | Main | Background |
|---|---|---|
| Tile / header | Costume (Main Cast) | Costume (Background Cast) |
| Unit identifiers | `wardrobe_main_unit_select/_shortlist/_final` | `wardrobe_background_unit_*` |
| Character picker source | Casting (Main Cast) → Final | Casting (Background Cast) → Final |
| Badge label key | `WARDROBE_MAIN_TOOL_LABEL` | `WARDROBE_BACKGROUND_TOOL_LABEL` |
Screens, menus, validation and wording are identical.

### Rights
Every write path checks `checkWardrobePostingRights()` (posting access of the tool) and otherwise
shows "You do not have Posting Rights on this Tool. Do you want to request one of the Admins to
give you that permission?" (No / Proceed). Chats check viewing rights. Record delete: uploader or
admin, else "You do not have rights to delete other user's 'Costume'". Folder delete: admin only,
else "Only Admin can delete.".

---

## Cases

Legend: **A** automated (case id in the suite), **N** not automated (reason given).

| ID | Case | Expected (app wording) | Source | |
|---|---|---|---|---|
| COS-01 | Tools → search "Costume" | Both tiles listed: "Costume (Main Cast)", "Costume (Background Cast)" | Tools.kt `WARDROBE_*_TOOL` | A |
| COS-02 | Open each tool | Header = tool name; tabs "Selects", "Shortlist", "Final"; search hint "Search by Talent Name/Character"; filter icon; "+" and camera FABs | WardrobePage.initUi/handleTabLayout | A |
| COS-03 | Filter icon | Sheet: Group by "Scene No", "Character", "Episode" (Episode default on TV); Filter by "Male", "Female", "Non Binary", "All" (All default); Reset / Submit | WardrobeVM.initializeFilter | A |
| COS-04 | Switch tabs | Unit chat icon on Selects and Shortlist, hidden on Final | WardrobePage.handleUnitInfo | A |
| COS-05 | "+" | "Add Costume Details": Upload Media, "Select Character" card with "+", Episode no. (TV only), Scene No., Description, Submit | AddEditWardrobePage.initView | A |
| COS-06 | Submit with no attachment | Snackbar "Costume Attachment Required." | validate() | A |
| COS-07 | Upload Media → Link | Selects offers Link; dialog "Upload Link", URL field paste-only; empty → "Please Enter URL"; invalid → "Please Enter Valid URL" | handleUploadMediaBtn, openPopupForUrl | A |
| COS-08 | Scene "A12"; episodes "n,n" | "Scene No. always starts with a number." / "Duplicate episode numbers found. Please remove duplicates." | validate() | A |
| COS-09 | Back from the form | "Are you sure you want to go back? Unsaved changes will be lost." No stays, Yes leaves | onBackPressedDispatcher | A |
| COS-10 | Character "+" from a Selects create | Picker "Casting" (search "Search by Talent Name/Character", "+" offered); temp form: Character Name, Talent Name, Gender, no Upload Media; empty → "Please Enter Character Name" | ChooseCastPage, CastingDetailPage | A |
| COS-11 | Create in Selects: link + temp character (Male) + episode + scene 901 + description | Filed under "Episode No : e" → "Character: Name" (detail "Talent Name: …", "Scene No. 901") → record page header "Character: … / Scene No. 901"; record shows "Link: …", "Uploaded by: …", "Description: …" | createWardrobe, doFolderGrouping, WardrobeItemHolder | A |
| COS-12 | ⋮ on own Selects record (admin) | View Details, Edit, Forward, Download, Image Reply, Move To Shortlist, Move To Final, Read By, Delete; chat icon on the card | WardrobeListingPage MORE | A |
| COS-13 | View Details | "Costume Details", all fields read-only, no Upload Media, no button; Back without a dialog | handleViewData | A |
| COS-14 | Edit | "Update Costume Details", button Update, no Upload Media; new description saved and listed | handleEditData, updateWardrobe | A |
| COS-15 | Search | Character name keeps its episode row; nonsense hides it; empty → "Please Enter something for search" | handleSearch, performFolderSearch | A |
| COS-16 | ⋮ on the episode row / character row (Selects) | Episode row: Move To Shortlist, Move To Final, Delete — Move → toast "To move this folder, please navigate to the next page.", Delete → "To delete this folder, please navigate to the next page."; character row: Move To Shortlist, Move To Final, Delete; inner search hint "Search by Talent Name/Character/Scene No." | WardrobePage.handleMoreOptions, CharacterListingPage | A |
| COS-17 | Group by Character / Scene No, Reset | Rows "Character: …" / "Scene No. …" with details; Reset → Episode rows | doFolderGrouping | A |
| COS-18 | Filter Male / Female | Male character listed under Male, not under Female | doFolderGrouping (gender) | A |
| COS-19 | Records → Select | Bar Move To Shortlist / Move To Final / Cancel / Delete, FABs hidden; Move with none → "Please choose at least one user from the list."; Delete with none → "Please keep at least one item from list." | handleMultiSelectActionButton | A |
| COS-20 | Shortlist "+" | Link allowed; no episode → "Episode no. cannot be empty."; no character → "Please Select Character"; picker has no "+" | validate(), selectCast() | A |
| COS-21 | Final "+" → Upload Media | No Link (Photo / Gallery / Video / Video Gallery) | handleUploadMediaBtn | A |
| COS-22 | ⋮ → Move To Shortlist on a record whose character was typed in (temporary) | Server refuses: "…costumes with cast names created via the Costume module are not allowed — please ensure that each costume has a proper cast name created via the Casting module, a scene number, and … an episode number …"; nothing reaches Shortlist. Move form button "Move", character / scene / episode prefilled | moveWardrobe (server) | A (+ finding 2) |
| COS-23 | Create a ZL_AUTO cast in Casting (Main/Background) → Final (pushed PNG, Non-Binary); open the Costume picker | The cast is offered by the matching Costume tool's picker | ChooseCastPage | A (emulator only) |
| COS-24 | Shortlist "+" with that Casting character, a link, episode, scene 903 | Created under Shortlist → Episode → Character; ⋮ = View Details, Edit, Forward, Download, Image Reply, Move To Final, Read By, Delete; chat icon | validate(), WardrobeListingPage MORE | A |
| COS-25 | Link-only Shortlist record → Move To Final | Refused: "Costume Attachment Required." | moveToDetailsPage | A |
| COS-26 | Selects photo record with the Non-Binary Casting character; filter "Non Binary" | Listed under Non Binary | doFolderGrouping vs CastingDetailPage.hitApi | A (finding 1) |
| COS-27 | Photo record → Move To Final | In Final; ⋮ = View Details, Forward, Download, Read By, Delete; no chat icon | WardrobeListingPage, WardrobeItemHolder | A (+ finding 2) |
| COS-28 | Final folder menus | Episode row and character row: Delete only | folder_options_casting removals | A |
| COS-29 | Selects unit chat | Header "<tool>: Selects"; no attachment button (by design); message posts and deletes | handleUnitInfo, OtherChatsActivity | A |
| COS-30 | Record chat icon | Record chat opens; message posts and deletes | OPEN_CHAT / CHAT_USER | A |
| COS-31 | ⋮ Forward / Read By | Member picker (UsersListPage) opens, nothing sent; ReadByUserPage opens | handleForward, openReadByUserPage | A |
| COS-32 | Record with no character / scene → "Character: Not Assigned"; ⋮ Edit | Row offers Move To Shortlist, Move To Final, Edit, Delete; sheet fills Scene Number; "Costume folder has been updated successfully."; row then "Scene No. 905" | HierarchyUpdateBottomSheetDialog | A |
| COS-40 | ⋮ Delete own record | "Are you sure you want to delete?" → Yes removes it | optionHandler delete | A |
| COS-41 | Admin deletes character folders in every tab | Run episode leaves every tab | CharacterListingPage delete | A |
| COS-42 | Delete this run's ZL_AUTO cast from Casting | Gone from Casting (all tabs) | Casting folder delete | A |
| COS-99 | Sweep | No "Zl_auto_cos" folder left; pushed album removed | — | A |
| COS-2U-01 | A creates a record; B has Selects open | B lists the new episode live (socket `wardrobeObserver`) | WardrobePage.listenObserver | A |
| COS-2U-02 | B opens A's record | "Uploaded by: Vidya Testing"; admin B gets the full menu | WardrobeListingPage MORE | A |
| COS-2U-03 | B posts in Selects unit chat | A's open unit chat shows it live | OtherChatsActivity | A |
| COS-2U-04 | A deletes the folder | B's open Selects drops the episode live (`wardrobeDeleteObserver`) | WardrobePage.listenObserver | A |
| COS-N1 | No posting rights: "+", Edit, Move, Delete, chat | "You do not have Posting Rights on this Tool…" (No / Proceed) | requestToAdmin | N — both project members are admins; revoking a real user's rights has no safe restore |
| COS-N2 | Non-admin deletes another member's record / a folder | "You do not have rights to delete other user's 'Costume'" / "Only Admin can delete." | optionHandler, handleMoreOptions | N — needs a non-admin second account |
| COS-N3 | Camera FAB / Upload Media → Camera, Video | Capture → form with the shot | openMediaPicker | N — needs the device camera; emulator camera content is not a controlled fixture |
| COS-N4 | ⋮ Download on a photo record | "File Downloaded Successfully." | handleSave | N — writes into the device gallery (DeviceGalleryFolder) with no way for the suite to find and remove the copy reliably |
| COS-N5 | ⋮ Image Reply | Opens the record chat in image-reply mode | optionHandler imageReply | N — needs drawing on the image editor; covered indirectly by COS-29 (same chat) |
| COS-N6 | Generate PDF / Distribute on folders | — | folder menus | N — every menu path removes these items (`generate_pdf`, `generate_full_pdf`, `distribute` are always in the removal list); dead in this build |
| COS-N8 | Offline retry (`btnTryAgain` on a failed upload) | Retry re-queues | RETRY | N — needs a network fault on a shared device |

---

## Findings

1. **COS-BUG-1 — "Non Binary" filter never matches a Non-Binary character** (reproduced on device,
   COS-26; also with a temporary character in earlier runs: the filter list goes empty).
   Non-Binary is stored as `Constants.OTHERS` = `"others"` (`CastingDetailPage.hitApi`:
   `if (selectedGenderPos > 1) Constants.OTHERS`) and shown back as "Gender: Non-Binary", while
   `WardrobeVM.doFolderGrouping` filters option 3 by `context.getString(R.string.other)` = `"Other"`
   with `equals(ignoreCase)` — `"others" != "other"`. The option's label is "Non Binary"
   (`R.string.non_conforming`).
2. **COS-BUG-2 — the record-level move form is titled "ADD COSTUME DETAILS"** (reproduced, COS-22 /
   COS-27). ⋮ → Move To Shortlist / Move To Final on a record opens `AddEditWardrobePage` through the
   `"IDS"` branch of `handleMoveData`, which never calls `registerCallingToolBar(…, move_wardrobe)`
   (only the folder `MOVE_DATA` branch does). The button reads "Move", the header "Add Costume Details".
3. **A temporary character cannot leave Selects** (server rule, not a bug): moving such a record is
   refused with "…costumes with cast names created via the Costume module are not allowed — please
   ensure that each costume has a proper cast name created via the Casting module, a scene number, and,
   if the project subtype is Television, an episode number…". The same message also covers
   "Link/Cast → Final". So Shortlist/Final can only be reached with a character from Casting → Final;
   the suite makes its own ZL_AUTO cast there (COS-23) and deletes it (COS-42).
4. **Temporary characters outlive their costume record.** A temporary character typed in on a Selects
   create later shows up in the Costume character picker ("Character: Zl_auto_cos 1714ml") even after
   the costume record was deleted, i.e. the server keeps it as a (temporary) Casting entry. There is no
   delete for it in the Costume UI. Worth asking product whether these should be purged with the record.
5. **No "Unknown" label: a record without a character files under "Character: Not Assigned".**
6. **Folder Edit only on folders with a missing level.** Every character / scene folder menu removes
   `R.id.edit` when the folder has a value; Edit stays only where something is missing (e.g. "Not
   Assigned"), and the sheet only unlocks blank levels (`shouldLockLevel`). Reply on success:
   "Costume folder has been updated successfully.".
7. **Generate PDF / Distribute are unreachable** — always removed from the folder menus (COS-N6).
8. **Costume chats have no attachment button** — `OtherChatsActivity` calls `isShowAttachment(false)`
   on every path; by design, not a regression (COS-29 asserts it).
9. Episode-row "Move"/"Delete" do nothing but a toast ("To move this folder, please navigate to the next
   page."); the toast was not readable through UiAutomator, so COS-16 asserts that no move form and no
   delete confirmation appear instead.

---

## Run results

**CostumeTest, both tools, emulator-5556, build Zillit_32539-dev, 2026-11-24 04:10–05:22 (1 h 12 min):
70 cases — 57 passed, 6 failed on app bugs, 4 failed on the environment, 3 skipped.**

| Result | Cases |
|---|---|
| Pass (both tools) | COS-01 … COS-18, COS-20, COS-21, COS-23, COS-24, COS-25, COS-28, COS-29, COS-30, COS-32, COS-40; COS-19 (Background), COS-31 (Main) |
| Fail — app bug | COS-22 ×2 and COS-27 ×2 (COS-BUG-2, header "ADD COSTUME DETAILS" on the move form — the moves themselves behave: refused for a temporary character, "Costume(s) have been moved successfully." to Final); COS-26 ×2 (COS-BUG-1, Non Binary filter empty) |
| Fail — environment | COS-41 (Background), COS-42 ×2, COS-99: emulator-5556's qemu process died at ~05:20 ("Device emulator-5556 was not in the list of connected devices"); the coordinator restarted it |
| Skip | COS-19 (Main), COS-31 (Background): the record page was not reached within the wait on that pass (both passed on the other tool and in the earlier Main run; waits since raised to 40 s); COS-41 (Main): the Tools list came back empty as the device was going |

Earlier Main-only run (05 → run5, emulator-5556, 30 min): COS-01…18, 20, 21, 29–32, 40, 41, 99 passed; it is
the run that found the server rule behind finding 3 (Shortlist/Final need a Casting cast), after which
COS-22…28 were rebuilt around a ZL_AUTO Casting cast.

Cleanup: the records of the 04:10 run (episodes 8544 / 9544) and its two Casting casts ("Zl_auto_cos 1544mc",
"Zl_auto_cos 1544bc") were left by the crash; COS-99 now sweeps any "Zl_auto_cos" folder in both Costume
tools and in Casting, plus "Not Assigned" folders inside this suite's own episodes — see *Sweep run* below.

**Sweep run** (COS-99 alone, emulator-5556, 05:39): deleted 8 leftovers of the crashed run — Costume
(Main/Background) "Zl_auto_cos 1544ml / 1544bl / 1544mc / 1544bc" and the "Not Assigned" folder, and the two
Casting casts "Zl_auto_cos 1544mc / 1544bc". A second pass (05:50, with
`COSTUME_SWEEP_EPISODES=8544,9544,8732,9732,8835,8705` so character-less folders of every earlier run are
checked too) found **0** — nothing of this suite is left in either Costume tool or in Casting. The pushed
album `/sdcard/Pictures/ZL_AUTO_Costume` was removed. Known remainder (finding 4): the temporary characters
typed in on Selects creates still show in the Costume character picker; neither Costume nor Casting offers a
way to delete them.

**CostumeTwoUserTest: written and compiling, not run.** The Pixel (second user) was held behind its app-lock
prompt (waiting for the device owner) from 05:20 onward. Run it with the Pixel lock first, then an emulator:

```bash
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC \
  ./run-and-report.sh --only CostumeTwoUserTest
```

Options: `COSTUME_TOOL=MAIN` or `BACKGROUND` (or `-Dcostume.tool=…`) runs one tool (each takes ~35 min on an
emulator; both ~70 min). `COSTUME_SWEEP_EPISODES=…` adds explicit episodes to the COS-99 sweep.
