# Location tool — test cases (Tools → Location)

App: Zillit Android, build Zillit_32539-dev, project ZL-Android-Automation (a TELEVISION project).
Source: `app/src/main/java/com/zillit/zillitapp/bottomNav/tools/location/` (paths below are relative to it).
Suites:

| Suite | Order | Devices | Runner |
|---|---|---|---|
| `tests/map/LocationToolTest` (LT-xx, read-only smoke) | `TestOrder.LOCATION_TOOL` (72) | one emulator | `--location` (exists) |
| `tests/location/LocationToolFullTest` (LOC-01…LOC-99) | `TestOrder.LOCATION_TOOL_FULL` (225) | one emulator (Vidya Testing) | `--only LocationToolFullTest` (flag requested: `--location-full`) |
| `tests/location/LocationToolTwoUserTest` (LOC-2U-xx) | `TestOrder.LOCATION_TOOL_TWO_USER` (226) | emulator (Vidya Testing) + Pixel (Vidya Pixel, admin) | `--only LocationToolTwoUserTest` with `ZILLIT_RECEIVER_UDID` (flag requested: `--location-two-user`) |

Page objects: `pages/map/LocationToolPage` (tool, listing, form, dialogs) and `pages/location/LocationEntriesPage` (a location's entries).
Map overlay (Location → Map) is covered by `tests/map/MapLocationFlowTest` / `MapLocationPinsTest` (Map agent) and is not repeated here.

**Data safety.** The suites create only `ZL_AUTO_…` locations (and one episode number, 9901, that only a ZL_AUTO location uses, LOC-50). Deletes go by that exact unique name, in every tab (a Move leaves a copy behind), from the episode listing page. An episode-row Delete is never confirmed (LOC-34 taps **No** if a confirm ever appears). Map cities are never touched. Nobody's rights or settings change.

## Screens and behaviour (discovery)

| Level | Screen (class) | What it shows / does |
|---|---|---|
| 1 | `LocationPage` | Toolbar "Location"; toolbar group-by icon `settingImage` → `R.menu.library_filter` minus city/gender/talent/character/contact → "Filter by Scene no", "Filter by Episode No", "Filter by location name" (`filterIv` is always hidden). Tabs Selects / Shortlist / Final (`LocationFolderPage` × 3). Tab badges (red BadgeDrawable) from local badge DB. |
| 1 | `LocationFolderPage` (per tab) | Search "Search by Scene No/Location Name/City" (empty search → "Please Enter something for search"); unit chat icon `chatIv` (Selects/Shortlist only, opens `OtherChatsActivity` "Location: <tab>"); + (`takeSceneShots`) → add form; camera (`uploadFromCam`) → camera / multi-capture picker → add form with the media. Rows: TV project → "Episode No : N"; by name → "Location Name : X" + "Scene No. …"; by scene → "Scene No. N" + "Location Name : …". Row menu (`folder_options`) per tab: Selects = Move To Shortlist, Move To Final, Delete; Shortlist = Move To Final, Delete; Final = Generate Full PDF, Delete (Distribute/Edit/Generate PDF removed for named rows). On an episode row Move → toast "To move this folder, please navigate to the next page.", Delete → toast "To delete this folder, please navigate to the next page.". Delete elsewhere: admin only ("Only Admin can delete."), confirm "Are you sure you want to delete?". No posting rights on + → "You do not have Posting Rights on this Tool…" with No / Proceed (request admin). Socket observers (create / update / delete) refetch the list. |
| 2 | `LocationListingPage` (one episode) | Rows "Location Name : X" / "Scene No. 901", search, row menu as above (Selects: Move To Shortlist, Move To Final, Delete; Shortlist: Move To Final, Delete; Final: Generate Full PDF, Delete). Move → move form; a row with an unassigned scene asks "In this folder, data without an assigned scene number will not be moved…". Delete by name + status + episode. Generate Full PDF → "Generation of Location list." with "Publish without details" / "Publish with details" → "Report Generated Successfully." dialog (View, user, Info, Confidential Info, Close). Row tap → entries (one scene) or `LocationSceneNoPage` (several scenes). |
| 3 | `LibraryPage` (one location) | Toolbar "Location", "Select" mode (Move To Shortlist / Move To Final / Delete / Cancel; empty selection → "Please choose at least one user from the list." / "Please keep at least one item from list."), +/camera (name and scene pre-filled and locked), entry info (Location Name, Scene No, Link, Address, City, Contact, Email, Phone, Uploaded by, Description). Entry menu (`common_lcw_menu`): Selects = View Details, Edit, Forward, Download, Image Reply, Move To Shortlist, Move To Final, Read By, Delete; Shortlist = same minus Move To Shortlist; Final = View Details, Forward, Download, Read By, Delete. Entry delete: own entry or admin, else "You do not have rights to delete other user's 'Location'". Tap: link → browser, media → viewer. Download respects download rights. |
| 4 | `LocationDetailPage` | "Add Location Details". Upload Media (camera, gallery, video, video gallery, + Link except in Final) → Upload Link dialog (paste icon only; "Please Enter URL" / "Please Enter Valid URL"). Location block: map preview, Pick on map (`MapsActivity`), read-only maps link `https://www.google.com/maps?q=lat,lng` (6 dp, Locale.US), Copy → "Copied To Clipboard.". Fields: Episode no. (TV only, digits + commas), Location Name, Scene No. (alphanumeric, max 15), Address, City, Description (Optional), Contact Name, Email, Code + phone. Modes: create, view (all disabled, no Submit / Upload Media), edit, move (no Location block, name locked). Validation order in `validate()`: attachment → episode required outside Selects (TV) → one episode on move / edit → duplicate episodes → link URL → name → scene required outside Selects → scene starts with digit → contact needs email or phone → email → phone 5–20 digits → country code. Name saved through `capitalizeSentence()` ("ZL_AUTO_Loc 1" → "Zl_auto_loc 1"). |
| — | Rights | View access off → `LocationPage` / `LibraryPage` finish. Posting off → + asks to request admin; moves / edit / select → "You don't have posting rights" or the request dialog. Folder delete admin only; entry delete own-or-admin. |

## Cases

Result column: last run on emulator / Pixel, build Zillit_32539-dev (see "Run log" below).

### Read-only smoke — `LocationToolTest`

| ID | Case | Steps | Expected | Source | Result |
|---|---|---|---|---|---|
| LT-01 | Tool opens | Tools → search "Location" → tap the row | Tab strip + pager | `LocationPage.initUi` | PASS (run 2) |
| LT-02 | Tabs and order | Read the tab labels | Selects, Shortlist, Final | `LocationPage` TabLayoutMediator | PASS (run 2) |
| LT-02b | Short tab wording | Read labels | No "Shoot Location" in a tab | strings `selects/shortlisted/final_` | PASS (run 2) |
| LT-04 | Every tab renders | Tap each tab | Tool surface stays | — | PASS (run 2) |
| LT-12 | Group-by icon (retargeted) | Tap toolbar `settingImage` | Menu with "Filter by Episode No" (was: `filterIv`, which the app always hides → skipped forever) | `LocationPage.showHideFilter` | PASS (run 2) |
| LT-13 | BACK returns to Tools (strengthened) | BACK on the tool | Still in Zillit, on Tools / bottom nav | — | PASS (run 2) |

### Single device — `LocationToolFullTest`

| ID | Case | Steps | Expected | Source | Result |
|---|---|---|---|---|---|
| LOC-01 | Header and tabs | Open tool | Toolbar "Location"; Selects / Shortlist / Final | `Tools.kt:785`, `LocationPage` | PASS (run 5) |
| LOC-02 | Search, + and camera per tab | Each tab | Hint "Search by Scene No/Location Name/City"; `takeSceneShots`, `uploadFromCam` shown | `LocationFolderPage.handleSearch/handleUploadingIcon` | PASS (run 5) |
| LOC-03 | Unit chat per tab | Each tab | `chatIv` on Selects, Shortlist; not on Final | `LocationFolderPage.handleUnitChat` | PASS (run 5) |
| LOC-04 | Episode grouping | Each tab | Every row "Episode No : N" | `LocationFolderPage.onViewCreated` (TV → `R.id.episode`) | PASS (run 5) |
| LOC-05 | Group-by items | Tap `settingImage` | Exactly Filter by Scene no / Filter by Episode No / Filter by location name | `LocationPage` settingImage listener | PASS (run 5) |
| LOC-06 | Empty search | Clear field, tap search icon | "Please Enter something for search" | `handleSearch` | PASS (run 5) |
| LOC-07 | No-match search + clear | Search "zzqq_no_such_location", then X | 0 rows; X restores the count | `LocationVM.performSearch/clearSearch` | PASS (run 5) |
| LOC-10 | Form layout | Selects + | "Add Location Details", Upload Media, Pick on map, all fields, Code, Submit | `LocationDetailPage.initM/handleLocationSection` | PASS (run 5) |
| LOC-11 | Attachment required | Name only → Submit | "Location Attachment Required.", form stays | `validate()` l.995 | PASS (run 5) |
| LOC-12 | Link field opens the picker | Tap the Link field | Map picker (MapsActivity) opens — the link is never typed | `handleLocationSection` (keyListener = null, click → launchPlacePicker) | PASS (run 5) |
| LOC-13 | Scene filter | Type "12-AB#c45678901234567" | Only [A-Za-z0-9], ≤ 15 chars | `initM` filters | PASS (run 5) |
| LOC-14 | Episode filter | Type "1a,2b" | "1,2" | `getNumericWithCommaOnlyFilter` | PASS (run 5) |
| LOC-15 | Selects picker has Link | Upload Media | "Link" listed | `handleUploadMediaBtn` | PASS (run 5) |
| LOC-16 | Upload Link dialog | Submit empty; paste "not a url" → Submit; Cancel | "Please Enter URL"; "Please Enter Valid URL"; Cancel closes, form stays | `openPopupForUrl` | PASS (run 5) |
| LOC-17 | Link accepted | Upload Media → Link → paste → Submit | Dialog closes, form stays (no preview for an image-less page, LOC-Q3) | `findMetaData/loadImageForLink` | PASS (run 5) |
| LOC-18 | Name required | Empty name → Submit | "Please Enter Name" (focus on name) | `validate()` l.1039 | PASS (run 5) |
| LOC-19 | Scene starts with digit | Scene "A12" | "Scene No. always starts with a number." | l.1047 | PASS (run 5) |
| LOC-20 | Duplicate episodes | Episode "1,1" | "Duplicate episode numbers found. Please remove duplicates." | l.1027 | PASS (run 5) |
| LOC-21 | Contact needs email/phone | Contact name only | "Please enter email or phone number" | l.1059 | PASS (run 5) |
| LOC-22 | Invalid email | Email "zl_auto_not_an_email" | "Please Enter Valid Email." | l.1065 | PASS (run 5) |
| LOC-23 | Short phone | Phone "12" | "Phone number must be minimum 5 and maximum 20 digit…" | l.1071 | PASS (run 5) |
| LOC-24 | Country code | Phone "9876543210", no code | "Please Select Country Code First." (skips if a code is pre-filled — it would save) | l.1077 | PASS (run 5) |
| LOC-25 | BACK discards | BACK out of the filled form | Nothing saved under that name | `onBackPressedDispatcher` | PASS (run 5) |
| LOC-26 | Shortlist needs episode and scene | Shortlist + with link, name | "Episode no. cannot be empty." then "Please Enter Scene No." | l.1013, l.1043 | PASS (run 5) |
| LOC-27 | Final picker has no Link | Final + → Upload Media | No "Link" | `handleUploadMediaBtn` (PUBLISH) | PASS (run 5) |
| LOC-28 | Pick on map + Copy | Pick on map → tick; Copy | Link `https://www.google.com/maps?q=<lat>,<lng>` (6 dp); "Copied To Clipboard."; clipboard = link | `applyPin/buildMapsLink/copyMapLink` | PASS (run 5) |
| LOC-30 | Create in Selects | + → name ZL_AUTO_Loc n, scene 901, episode 1, address, city, description, link → Submit | Listed in Selects › Episode No : 1 | `hitApi` create, `resultLauncher` | PASS (run 5) |
| LOC-31 | Listing row text | Open Episode No : 1 | "Location Name : <name>", subtitle with "901" | `LocationVM.doFolderGrouping` | PASS (run 5) |
| LOC-32 | Search by name / city | Tab search name, then city; listing search name | Only "Episode No : 1"; listing shows exactly the fixture | `performSearch` | PASS (run 5) |
| LOC-33 | Group-by modes | Filter by location name; Filter by Scene no; Filter by Episode No | "Location Name : <name>"; "Scene No. 901"; episode groups back | `filterFolderAdapter` | PASS (run 5) |
| LOC-34 | Episode row refuses Move/Delete | Episode row menu → Move To Shortlist; → Delete | Menu = Move To Shortlist, Move To Final, Delete; toasts "To move/delete this folder, please navigate to the next page."; no form, no confirm | `LocationFolderPage.handleMoreOptions` | PASS (run 5) |
| LOC-35 | Selects listing menu | Row ⋮ | Move To Shortlist, Move To Final, Delete | `LocationListingPage.onRecyclerItemClick` | PASS (run 5) |
| LOC-36 | Entries page | Tap the row | Header "Location"; 1 entry with name, 901, city; "Description: …" | `LibraryPage`, `LibraryItemHolder` | PASS (run 5) |
| LOC-37 | Selects entry menu | Entry ⋮ | View Details, Edit, Forward, Download, Image Reply, Move To Shortlist, Move To Final, Read By, Delete | `LibraryPage` MORE | PASS (run 5) |
| LOC-38 | View Details read-only | View Details | City shown; fields disabled; no Submit, no Upload Media | `initM` VIEW branch | PASS (run 5) |
| LOC-39 | Edit | Episode "1,2" → Submit; then "1" + new description → Submit | "Only one episode will be allowed at a time during update."; then saved and shown | `validate()` l.1022, `updateLocationData` | PASS (run 5) |
| LOC-40 | Entries Select mode | Select → Delete; Move To Shortlist; Cancel | Buttons Move To Shortlist / Move To Final; "Please keep at least one item from list."; "Please choose at least one user from the list."; bar closes | `LibraryPage.handleBtnActions` | PASS (run 5) |
| LOC-41 | + inside a location | Entries + | Name and scene pre-filled and locked; **episode pre-filled "1"** | `LibraryPage.initView` | FAIL — bug LOC-B1 (runs 2–5) |
| LOC-42 | Second entry | Entries + → episode 1, description, link → Submit | Entries count +1, with its description | create from `LibraryPage` | FAIL — bug LOC-B2 (runs 3–5) |
| LOC-43 | Delete one entry | Second entry ⋮ Delete → No; → Yes | "Are you sure you want to delete?"; No keeps; Yes removes only it | `optionHandler` delete | pending run 6 (test fix: entry found by text, scrolled into view) |
| LOC-44 | Move to Shortlist | Listing ⋮ Move To Shortlist | Form without Location block, name locked, episode 1, scene 901; empty episode → "Episode no. cannot be empty."; "1,2" → "Only one episode will be allowed at a time during move."; Submit → in Shortlist | `handleIntentData` MOVE_BY_FOLDER, `validate()`, `/movelocation` | PASS (run 5) |
| LOC-45 | Shortlist menus | Listing ⋮; entry ⋮ | Move To Final, Delete; entry menu without Move To Shortlist, with Edit + Move To Final | as above | PASS (run 5) |
| LOC-46 | Move to Final | Shortlist listing ⋮ Move To Final → Submit | In Final | `/movelocation` | PASS (run 5) |
| LOC-47 | Final menus | Final listing ⋮; entry ⋮ | Generate Full PDF, Delete; View Details, Forward, Download, Read By, Delete | as above | PASS (run 5) |
| LOC-48 | Generate Full PDF | Final listing ⋮ Generate Full PDF → Publish without details | "Generation of Location list." with the two buttons; "Report Generated Successfully." with View; Close dismisses (nothing shared) | `showPopupForGeneratePdf/shareToInfo` | PASS (run 5) |
| LOC-49 | Listing delete | Final ⋮ Delete → No; → Yes | Confirm text; No keeps; Yes removes from Final | `deleteLocationByFolderName` | PASS (run 5) |
| LOC-50 | New episode group | Create ZL_AUTO_Ep n with episode 9901; delete it | "Episode No : 9901" appears (search 9901), then disappears | `doFolderGrouping` | PASS (run 5) |
| LOC-99 | Cleanup | Delete every ZL_AUTO location this run made from every tab | Gone everywhere | — | PASS (run 5) |

### Two devices — `LocationToolTwoUserTest` (A = emulator acts, B = Pixel watches)

| ID | Case | Steps | Expected | Source | Result |
|---|---|---|---|---|---|
| LOC-2U-01 | Create seen live | B opens Selects › Episode 1; A creates ZL_AUTO_2u n | B's open listing shows it within 60 s, no navigation | `BaseSocketListener.locationObserver` → `getList` | NOT RUN — Pixel held by the coordinator (app-lock prompt) since 05:20 |
| LOC-2U-02 | Move seen live | B opens Shortlist › Episode 1; A moves it | B lists it | `locationUpdateObserver` | NOT RUN — Pixel held by the coordinator (app-lock prompt) since 05:20 |
| LOC-2U-03 | Edit seen by other | A edits description; B opens the entry | B reads the new description | `updateLocationData` | NOT RUN — Pixel held by the coordinator (app-lock prompt) since 05:20 |
| LOC-2U-04 | Admin deletes another's entry | B (admin) entry ⋮ Delete → Yes; A has Shortlist listing open | B offered Delete; A's listing loses it live | `LibraryPage` delete (own or admin), `locationDeleteObserver` | NOT RUN — Pixel held by the coordinator (app-lock prompt) since 05:20 |
| LOC-2U-05 | Delete seen live | B opens Selects listing; A deletes the Selects copy | B's open listing loses it | `locationDeleteObserver` | NOT RUN — Pixel held by the coordinator (app-lock prompt) since 05:20 |

## Not automated (and why)

| ID | Case | Reason |
|---|---|---|
| LOC-N1 | Camera button / Upload Media → Camera, Gallery, Video | Needs device camera frames or seeded gallery media on shared emulators; picker internals are a separate module (MediaPicker). Final-tab create depends on it (Link is not offered there), so "create directly in Final" is manual. |
| LOC-N2 | No posting rights: + / Move / Select / Edit refused, "request admin" dialog | Needs a non-admin member with Location posting off. Both test accounts are admins and are shared by other agents running concurrently; toggling rights mid-run would break their suites. |
| LOC-N3 | View access off closes the tool | Same reason as LOC-N2. |
| LOC-N4 | Non-admin folder delete → "Only Admin can delete."; other user's entry → "You do not have rights to delete other user's 'Location'" | Same reason (needs a non-admin second account). LOC-2U-04 covers the admin side. |
| LOC-N5 | Unit chat (Selects / Shortlist) send / read | Sending posts to the project unit chat seen by every member of the shared project; only the icon is checked (LOC-03). |
| LOC-N6 | Forward / Read By / Image Reply / Download on an entry | Forward sends a C&C message; Image Reply posts to the entry chat; Download writes to the shared device gallery; entries made by the suite are links (no media to download). Menu presence is asserted (LOC-37/47). |
| LOC-N7 | Generate PDF → Info / Confidential Info / users | Publishes the PDF to project-wide feeds or users. LOC-48 stops at the generated dialog and closes it. |
| LOC-N8 | Tab / folder badges from another member's change | Badges come from the notification DB (`getLocationBadgesFromLocal`), driven by push delivery to the Pixel; the tab badge is a BadgeDrawable not exposed to UiAutomator. Not reliably observable. |
| LOC-N9 | Notification tap opens the Location tool | Needs a real push on a shared device; `handleIntentData` tab switching is commented out in source anyway. |
| LOC-N10 | Offline submit ("network error") | Network faults on shared emulators break other agents' runs. |
| LOC-N11 | Second screen `LocationSceneNoPage` (a location filed under several scenes) and the "unassigned scene number" move warning | Needs a location with several scene numbers / a blank scene in a non-Selects tab; reachable by data the suite would have to build with group-by name — left for a later batch. |
| LOC-N12 | Distribute | Removed from every named row menu in source (`folder_options` minus distribute); only reachable on unnamed rows. |

## Bugs and product questions found

| ID | Finding | Evidence |
|---|---|---|
| LOC-B1 | "+" inside a location (entries page) opens the form with an EMPTY Episode no. on a TV project, so the new entry is saved outside the episode it was added from unless the user retypes it. `LibraryPage.initView` builds `val intent = Intent(...)` and then forwards `EPISODE_NO` from that new Intent (always absent) instead of the activity's intent. | LOC-41 |
| LOC-B2 | An entry added with "+" inside a location (entries page) is saved WITHOUT its Description: the field reads "ZL_AUTO second entry" right before Submit, the entry is listed, its info block shows name / scene / link, but no "Description:" line ever appears (re-opened for 30 s). The same fields saved from the tab's "+" keep their description (LOC-30/36). Reproduced on two runs. | LOC-42 |
| LOC-Q1 | "Move" leaves a copy in the earlier tab(s) (Selects still lists a location moved to Shortlist / Final). Product to confirm whether Move should be a promote-and-keep or a move. Not asserted. | memory note; LOC-44/46 cleanup deletes all copies |
| LOC-Q3 | A link whose page has no preview image adds nothing visible to the form after Upload Link → Submit (`loadImageForLink` hides `imageHolder`; `linkLi` has no content), so the user cannot see that the attachment was taken. | LOC-17 (not asserted) |
| LOC-Q2 | The name is stored through `capitalizeSentence()`, which lower-cases everything after each word's first letter ("ZL_AUTO_Loc" → "Zl_auto_loc"). | LOC-31 / entries text (case-insensitive match used) |

## Run log

| Run | Device | Suite | Result |
|---|---|---|---|
| 1 (01:58) | emulator-5554 | Full + smoke | not started — UiAutomator2 instrumentation could not be initialised on 5554 (device-side) |
| 2 (03:49) | emulator-5560 | Full | 25 pass / 8 fail / 15 skip — test-side issues found (picker "Link" tap hit the form's label; Appium setText bypasses InputFilters; snackbar over the selection bar; stray BACK from hideKeyboard closing the form); LOC-41 bug |
| 2 | emulator-5560 | `LocationToolTest` | 6 / 6 pass (LT-12 now runs instead of skipping) |
| 3 (05:26) | emulator-5560 | Full | 37 pass / 3 fail / 8 skip — clipboard preview overlay ate the picker tap (Quick Share opened); LOC-41, LOC-42 bugs |
| 4 (06:08) | emulator-5556 | Full | 37 pass / 4 fail / 7 skip |
| 5 (06:50) | emulator-5556 | Full | **45 pass / 3 fail / 0 skip** — LOC-41 (bug B1), LOC-42 (bug B2), LOC-43 (test: popup of a lower card) |
| — | emulator + Pixel | `LocationToolTwoUserTest` | not run — Pixel lock held by the coordinator ("app-lock prompt showing — waiting for the user to unlock") from 05:20 |

Every run's LOC-99 passed: the ZL_AUTO locations were deleted from all three tabs and a by-name search of every tab came back empty (runs 3–5).
