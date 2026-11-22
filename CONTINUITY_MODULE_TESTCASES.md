# Continuity — test cases and findings

**Continuity** is the department's photo record of how a scene looked: a picture (or video,
or document) of the set, the costume, the props, tagged with the scene number it belongs to,
so the next day's shoot can match it. Tools → **Continuity** opens
`bottomNav/tools/continuity/ContinuityPage`, a two-tab `TabLayout` + `ViewPager2` over its
own backend at `{continuity host}api/v2/continuity/`.

> Not to be confused with the **Continuity** screen inside Costumes & Set Sync
> (`bottomNav/tools/costumesetsync/ui/continuity/CtnScreen.kt`). That one is a different
> host (`SYNC_ON_SET_BASE_URL …/projects/{pid}/continuity`), a different data model keyed by
> scene and character, and a different tool. Nothing here applies to it.

**Status: automated and run on the emulator (2026-11-21, project ZL-Android-Automation).**
Suite [`ContinuityTest`](src/test/java/com/zillit/automation/tests/continuity/ContinuityTest.java),
`TestOrder.CONTINUITY = 175`, page objects `ContinuityPage`, `ContinuityAddPage`,
`ContinuityScenePage` (on a shared `VisiblePage` base). Last full run: **57 cases — 53
passed, 3 failed, 1 skipped, 25 minutes; no app crash or ANR in logcat.** The three failures
are app defects reproduced on the device, not test faults: CT-32 (finding 6), CT-44
(finding 11), CT-84 (finding 4). CT-83 skips on a project where only one department holds
the scene. Not automated: CT-90 / CT-91 (need a second, non-posting account) and the
stamp-size half of CT-62 (needs relaxed security). One run:

```bash
./run-ui.sh --continuity
```

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own, because the automation will match on it; `id` in backticks
is a real resource id, read from the layout. Cases marked **[QA <row>]** carry a row of the
module's tab on the QA bug sheet (see *From the QA sheet*); the rest come from reading the
source.

---

## The shape of the data

```
Scene record                 ONE uploaded file (image / video / document) plus:
  ├── scene_number           required, must start with a digit, ≤ 15 chars on the client
  ├── episode                TV projects only (projectSubType = TELEVISION)
  ├── scene_notes            optional, ≤ 2000 chars
  ├── talent_info[]          "Add More Details": free label / value pairs
  ├── actor_name             still sent and still searched — but never shown (finding 7)
  ├── visibility             intra (my department) → all (forwarded to everyone)
  └── deleted{intra, all}    deleting is PER LEVEL: removing it from All keeps it in My Dept
```

There is **no scene object**. The tab lists are just the distinct scene-number strings the
server has records for (`GET scenes` → `data: ["1", "12A", …]`), and a scene number vanishes
from the list when its last record goes. Records with the same scene number but different
episodes land in the same row.

Two tabs, two views of the same records:

| Tab (`tv_title`) | List call | Tap a row | Who can add |
|---|---|---|---|
| "My Department" | `GET scenes` | the grid of *my department's* records for that scene | admin, or posting access — the `takeSceneShots` FAB |
| "All\nDepartment" (a real newline) | `GET scenes?showall=true` | a **department list** for that scene, then that department's grid | nobody — records only arrive here by **Forward → All Department** |

Creating a record is **asynchronous and has no result on screen.** The Add dialog only
queues an upload (S3 or Box, by `storageType`) and writes a pending row to Realm; the
`POST scene` fires after the upload finishes, with empty success and error callbacks, and the
new row only appears when the `continuity:scenes:created` socket arrives. So every "after
adding" assertion is a **wait for the row**, never a check for a confirmation.

Gates (all read from `postingRightsList[continuity_tool]` unless noted):

| Action | Gate | When it is shut |
|---|---|---|
| Open the tool | `viewAccess == true` | the screen `finish()`es with no message |
| Add (FAB) | My Department tab AND (admin OR `postingAccess`) | FAB absent |
| Edit Details | My Department AND (admin OR `postingAccess`) — **no owner check** | item absent from ⋮ |
| Forward / Distribute | admin OR `postingAccess` | items absent from ⋮ |
| Distribute (execute) | `document_distribution_tool` posting access | toast "You don't have posting rights" |
| Download | `downloadAccess` | "Alert" — "You do not have Downloading Rights on this Tool. Do you want to request one of the Admins…" |
| Delete | admin OR uploader — item **always shown** | snackbar "You can't delete a scene which is not uploaded by you." |

---

## 1. Module shell · `CT-0`

| # | Case | Expected |
|---|---|---|
| CT-01 | Tools → Continuity | Header "Continuity"; two tabs, "My Department" and "All Department" (the second is `All\nDepartment` — match on `Department` and position, not the literal); My Department selected |
| CT-02 | The banner on each tab | My Department: "Note: You need to upload in 'My Department' folder in order to forward to 'All Departments' folder". All Department: "Note: Pictures shown in 'All Departments' folder must be forwarded from 'My Department' folder." |
| CT-03 | Close the banner | `ivCloseNotes` hides `notesHolder`; the list moves up into its space |
| CT-04 | Tab info | `img_info` in a tab opens the info dialog with that tab's name as header, "Ok" and "Watch Video". Ok closes it. *(The tap goes to the icon, not the tab — switching tabs is a tap on `tv_title`.)* |
| CT-05 | The FAB follows the tab | `takeSceneShots` on My Department, absent on All Department — adding is only ever done in your own department |
| CT-06 | Search hint | Both tabs: "Search By Scene No." (the first hint, "Search by Scene no./ Scene notes", is overwritten in `onResume` — notes are not searchable on this screen) |
| CT-07 | Empty state | On a tab with no scenes, `emptyView` is shown. It is an image with **no text** (`data_not_found_yet` is an empty string), so the case asserts on the id. *Skips when the project already has scenes* |

## 2. Adding a record · `CT-1`

| # | Case | Expected |
|---|---|---|
| CT-10 | FAB → picker | "Choose below" offers, in order: "Photo", "Gallery", "Video", "Video Gallery", "Document" |
| CT-11 | Pick an image → the form | "Add Scene Details", with Scene No. and Scene Note and the "Add More Details" card. Actor Name and the dead Episode/Gender fields are **not** offered (finding 7); Episode is offered only on a TV project |
| CT-12 | Submit with Scene No. blank | Refused, dialog stays open, error "Please Enter Scene No." on the field |
| CT-13 | Scene No. that starts with a letter | Refused — "Scene No. always starts with a number." This is the rule every fixture name has to respect (see *Fixtures*) |
| CT-14 | Scene No. longer than 15 | The field keeps 15 characters (`LengthFilter(15)`, which replaces the XML's 40) |
| CT-15 | TV project, Episode blank | Refused — "Episode no. cannot be empty." *Skips on a non-TV project* |
| CT-16 | Add More Details, blanks | The "Add More Details" dialog refuses a blank Title ("Please enter title to continue") and then a blank Description ("Please Enter Description") |
| CT-17 | Add More Details, filled | The pair appears as a row under the card; its delete icon removes it **immediately, with no confirm** (see finding 9) |
| CT-18 | Submit a valid record | The dialog closes; **within the socket wait** a row "Scene No-`<n>`" is in My Department, and the top progress bar has gone. The wait is the assertion: the app offers no success message (finding 1) |
| CT-19 | Close with the X | `btn_cancel` closes the form and nothing is uploaded — the scene number does not appear. The sheet is `setCancelable(false)`, so back/outside taps are not a way out |

## 3. The tab list · `CT-2`

| # | Case | Expected |
|---|---|---|
| CT-20 | Search by scene number | Only rows whose scene number contains the text remain |
| CT-21 | Search for nothing | No row remains. (Whether the empty view appears is unverified — the filter never touches it; record what the screen shows) |
| CT-22 | Row shape | "Scene No-`<n>`" (no spaces — hard-coded), a chevron, and an unread badge only when > 0 |
| CT-23 | Long-press a row | Nothing actionable: the popup has every item removed (finding 3). The case pins today's behaviour |
| CT-24 | Search with `[` and `(` **[QA BUG-18]** | No crash, no error; the list simply filters to nothing. The backend's search fails with a server error on these characters; Android filters locally (`contains`), so this is a guard that it stays that way |
| CT-25 | A zero-padded scene number arrives live **[QA WEB-06]** | Add a record with scene number `099ZLAUTO<run id>` while My Department is open: its row appears without leaving the screen, and opening it lists the record. Web misses these on its socket because it compares `Number(scene_number)`; Android compares strings, so this is expected to pass |

## 4. A scene's grid (My Department) · `CT-3`

| # | Case | Expected |
|---|---|---|
| CT-30 | Open a scene | Title "Scene No. - `<n>`"; a 2-column grid; search hint "Search by Scene no./ Scene notes" |
| CT-31 | Card shape | "Scene - `<n>`", "Scene Note - `<notes>`" when notes were given, a ⋮; a play icon only on a video |
| CT-31b | A second record in the same scene | It is listed beside the first, and the first is still there. R2 carries a note of its own: every case picks cards by note, and a note-less card here would borrow R1's (finding 6) |
| CT-32 | Card with no note, after a rebind | In its own scene, one record with a note and one without: exactly one card shows a note line — read fresh, again after a search is cleared, and again after the viewer. **Seen failing on the device**: fresh it is right, after a rebind both cards carry the note (finding 6) |
| CT-33 | Tap a card | The in-app viewer (`CustomImageViewer`), header = scene number, description "Scene : `<n>`", one "`<label>`: `<value>`" line per extra detail, "Scene Note : `<notes>`". *Never tap a document card — it leaves the app* |
| CT-34 | Search by notes | Typing part of one record's notes leaves that record only |
| CT-35 | The ⋮ menu (admin) | "Edit Details", "Forward", "Distribute", "Download", "Delete" — in that order. "File Cabinet" is not offered (commented out) |
| CT-36 | A grid taller than the screen | Builds enough records that the grid scrolls, and reads every one back. Without this the readers below are never exercised on a list that needs scrolling |

## 5. Edit · `CT-4`

| # | Case | Expected |
|---|---|---|
| CT-40 | Edit Details opens prefilled | Title "Edit Details"; Scene No. and Scene Note hold the record's values; its extra details are listed |
| CT-41 | Change the notes **[QA BUG-14]** | Submit → the dialog closes and the card's note line shows the new text, without a refresh. Then leave the scene and open it again: the card is **still listed and still shows its picture**. The Android edit sends no `attachment` and no `unique_id` (`ContinuitySceneListPage.kt:1506-1516`), and the backend replaces the whole item on edit (sheet BUG-14), so an Android edit may erase the record's file — and a record that loses its `deleted` object is dropped from the grid altogether (finding 14) |
| CT-42 | Change the scene number | The page closes (by design: `sceneNoChanged` → `finish()`), the old scene number disappears from My Department if it had only this record, and the new one appears |
| CT-43 | Remove every extra detail **[QA BUG-14]** | After saving, reopening Edit shows **no** extra details. The client leaves `talent_info` out of the body when the list is empty (finding 10); whether that clears them depends on the backend — per BUG-14 it wipes any field not sent, so this case is expected to pass for the wrong reason, and CT-41 is where the same behaviour hurts |
| CT-44 | Type the same scene number back | The page should stay open. Today any keystroke in Scene No. closes it on success (finding 11) |

## 6. Forward · `CT-5`

| # | Case | Expected |
|---|---|---|
| CT-50 | ⋮ → Forward | Selection mode: `layForward` bar with `tvCount` "1", `btnForward` "Forward", the card dimmed with a tick (`layOverlay`); ⋮ taps are ignored while it is on |
| CT-51 | Tap another card | Count goes to 2; tapping it again goes back to 1 |
| CT-52 | Cancel | The bar goes, no card stays ticked, the search text is cleared |
| CT-53 | Forward → targets | A menu offering "All Department" and "Select User" |
| CT-54 | Forward to All Department asks first | "Please note, doing this will make your material visible to all. Do you still want to proceed?" — No keeps everything as it was |
| CT-55 | Forward to All Department | Yes → snackbar "Scene shared with All Department"; on the All Department tab the scene number is listed, its department list includes the forwarding user's department, and the record is in that department's grid with "Department - `<name>`" |
| CT-56 | Select User, submit nothing | The users list refuses an empty submit — "Please choose at least one user from the list." (Sending to a user posts a real 1:1 chat message per scene, so the positive path is left to a two-device run) |

## 7. Distribute and Download · `CT-6`

| # | Case | Expected |
|---|---|---|
| CT-60 | ⋮ → Distribute | Selection mode with `btnForward` reading "Distribute" |
| CT-61 | Distribute asks first | "Are you sure you want to distribute?" — No leaves selection mode untouched. The positive path posts to Document Distribution and is opt-in only |
| CT-62 | ⋮ → Download **[QA BUG-15]** | "File Downloaded Successfully." — or, for a user without download rights, the request-access Alert, answered **Cancel** (Yes messages the admins). Either outcome is a pass; anything else is a fail. For the PDF record the app asks the backend for the stamped copy (`scene/watermark/{id}`), which sheet BUG-15 says skips the stamp unless the type field is exactly `pdf` — where `mobile: shell` is allowed, the case compares the saved file's size with the fixture's, so a missing stamp shows; without relaxed security that half skips |

## 8. Delete · `CT-7`

| # | Case | Expected |
|---|---|---|
| CT-70 | Delete asks first | "Delete" — "Are you sure you want to delete?" with No / Yes; No keeps the card |
| CT-71 | Delete from My Department **[QA BUG-20]** | Yes → the card goes (on the `continuity:scene:deleted` socket, not locally), and the scene number leaves My Department when it was the last record. The snackbar is the server's `message`: it must be readable text, not an internal key — sheet BUG-20 found raw codes in Continuity messages |
| CT-72 | Delete from All Department keeps My Department | Deleting a forwarded record on All Department (`DELETE scene/all/{id}`) removes it there only — it is still in My Department. This is the business rule the per-level `deleted{intra, all}` exists for |

## 9. All Department · `CT-8`

| # | Case | Expected |
|---|---|---|
| CT-80 | Open a scene | Title "Department List Continuity"; search hint "Search By Department Name"; a department list, not a grid |
| CT-81 | Open a department | Title "Scene No - `<n>`"; the grid; every card carries "Department - `<that department>`" |
| CT-82 | Back from the grid | Returns to the department list, title and hint restored; back again leaves the screen |
| CT-83 | Two departments in a row | Open department A, back, open department B: **every** card names department B. Expected to fail where both have records (finding 2) |
| CT-84 | Long-press a department | The only item is "Edit". Expected to fail as a case of its own: it runs Distribute (finding 4). The case taps it only as far as the confirm and answers No |
| CT-85 | The ⋮ menu here | "Forward" and "Distribute" (posting access), "Download", "Delete" — **no** "Edit Details", which is My Department only |
| CT-86 | Unforwarded records stay private **[QA BUG-07]** | Make two records under one scene number and forward only one to All Department. On All Department → that scene → your department, **only the forwarded one** is listed. The Android grid request carries `departmentId` and `showall=true` (`ContinuitySceneListPage.kt:1596-1623`), and the backend does not check department rights on reads (sheet BUG-07) — this is the one place a user can see the leak without crafting a request |

## 10. Rights · `CT-9` *(needs a second, non-posting account — skips without one)*

| # | Case | Expected |
|---|---|---|
| CT-90 | No posting access | No FAB; ⋮ offers only "Download" and "Delete" |
| CT-91 | Delete someone else's record | Snackbar "You can't delete a scene which is not uploaded by you." — and the record stays |

## 11. Cleanup · `CT-99`

| # | Case | Expected |
|---|---|---|
| CT-99 | Sweep | No record whose scene number or notes carry the run's `ZL…AUTO` marker is left on either tab — both levels, since deleting from My Department does not delete the All Department copy — including leftovers from a run that broke halfway and ones below the fold |

---

## Findings

Read from the source; each is something a user can see. File paths are under
`bottomNav/tools/continuity/` unless given in full.

**1. A failed upload or create tells the user nothing — `CT-18`**
The Add dialog never calls the API. `POST scene` runs from
`uploadingHelper/UploadingApisHandler.kt:865-907` once the file has uploaded, with **empty**
`onSuccess` and `onError`. The "Scene Created Successfully" snackbar
(`ContinuityDepartment.kt:722-742`) is dead because `createScene()` is never called. So the
dialog closes, the top progress bar spins, and if anything fails the record simply never
appears. An upload error is routed to `fragment.updateList()`, which shows the generic
"Something went wrong" — and only on whichever tab is visible at that moment
(`ContinuityPage.kt:272-282`).

**2. All Department shows the previous department's records — `CT-83`**
Going back from a department's grid (`ContinuitySceneListPage.kt:279-297`) does not clear
`newList` or reset the page. Opening the next department then fetches as a *next page*
(`REQ_GET_SCENE`, append) with the previous department's last timestamp as the cursor
(`:1596-1623`), so the old department's cards stay in the new department's grid. A
`continuity:scenes:created` socket while the department list is showing makes it worse: it
fetches with no department id and prepends the result (`:1163`).

**3. Long-press on a tab row offers nothing — `CT-23`**
`ContinuityDepartment.kt:370-389` opens `folder_options_casting` after removing all seven of
its items. The folder-level Distribute behind it is unreachable.

**4. "Edit" on a department row runs Distribute — `CT-84`**
`ContinuitySceneListPage.kt:810-833` removes six of the seven items, leaving "Edit", and the
click handler calls `distributeData()` whatever was tapped. A user who wants to edit
distributes the department's whole folder to everyone (`visibility=all`).

**5. The grid's shimmer never stops after an error**
`loaderView` is hidden only in the `ApiStatus.Success` branch (`:902`); the Error branch
(`:1117-1130`) leaves it animating over the grid. Besides covering the content, a
continuously animating view keeps UiAutomator from ever reaching idle — so a failing API
here looks to the suite like a hang, not an error. Every grid case must wait on
`loaderView` with an explicit timeout and fail with that reason.

**6. Recycled cards keep another card's text — `CT-32`** · *confirmed on the emulator*
`adapter/holder/SceneItemHolder.kt:36-53` sets `tvNotes` only when notes are non-empty and
never re-hides `tvDetail`. Seen on the device on 2026-11-21: a record created with
`"scene_notes": ""` (the request is in logcat) showed "Scene Note - ZL_AUTO_…_one" — the other
record's note — once the grid had been rebound. Freshly drawn it was right, so a check that
reads only once passes. A user is told a note belongs to a picture it was never written for,
and opening Edit from that card shows an empty note field.

**7. Actor name is collected, sent and searched, but never shown**
Both branches of `SceneItemHolder.kt:29-34` set `actorName` GONE, and both dialogs hide the
`actor_name` field (`ContinuityDepartment.kt:447`, `ContinuitySceneListPage.kt:1371`). Yet the
grid search still matches on actor name (`SceneItemAdapter.kt:85`) — a search can return a
card with nothing on it that explains why it matched. The `gender` dropdown and the `episode`
include are also permanently GONE.

**8. The help text describes gestures that do not exist — `CT-04`**
Both tab info dialogs say "Long press on picture to Delete, Edit, and Forward…". Cards have
no long-press (`SceneItemAdapter.kt:41-51`); those actions live in ⋮.

**9. Extra details cannot be edited, and delete without asking — `CT-17`**
`imgEdit` in `item_continuity_more_layout.xml` is GONE and `showEditBtn` is never set by any
caller; `imgDelete` removes the row immediately.

**10. An edit leaves out everything it does not show — `CT-41`, `CT-43`**
The edit body is built from the visible fields only (`ContinuitySceneListPage.kt:1494-1517`):
no `attachment`, no `unique_id`, and `talent_info` / `episode` omitted when empty, because the
Ktor client has `encodeDefaults = false` (`network/NetworkClient.kt:68-80`). On its own that
would mean "cannot clear the extra details". Combined with the backend's whole-item replace
(QA sheet BUG-14) it means the opposite and worse: every Android edit can erase the record's
file. CT-41 settles which.

**11. Any keystroke in Scene No. closes the page — `CT-44`**
The edit dialog's TextWatcher sets `sceneNoChanged = true` on any change, including typing
the same value back, and success then `finish()`es (`ContinuitySceneListPage.kt:1064-1066,
1578-1580`).

**12. Every refresh wipes the search box**
`clearSearch()` runs on every list success (`:926, 962, 1001`) — a socket event or the next
page landing while the user types clears the query. On the tabs, a badge update resubmits
the full list off the main thread (`ContinuityDepartment.kt:268-301`), dropping the filter
while the typed text stays.

**13. A null department name crashes the department list**
`adapter/ContinuityDepartmentItemAdapter.kt:46` — `departmentName?.getDataFromLabelKey()!!`.

**14. A record without a `deleted` object disappears from the grid — `CT-41`**
`adapter/SceneItemAdapter.kt:53-65` keeps a record only when `deleted.intra` (or `deleted.all`
on All Department) is present and `<= 0`; `deleted == null` drops it. The empty view is driven
by the unfiltered list, so a scene whose records are all dropped shows a blank grid with no
empty state. Together with finding 10, an edit that wipes `deleted` makes the record vanish.

Smaller, not worth a case of their own:

- Five spellings of one label: "Scene No-X" (tab row), "Scene - X" (card), "Scene No. - X"
  (My Department title), "Scene No - X" (department title), "Scene No. %s" (File Cabinet).
- Delete is always in ⋮ and refused afterwards for non-owners; Edit has no owner check at all
  (`ContinuitySceneVM.kt:153-161`, owner check commented out).
- "No" on the first-time Distribute reminder also suppresses it for good
  (`utils/UIExtentions.kt:754-759`).
- `continuity:scene:shared` is ignored in My Department mode (`:1280-1282`).
- Strings: "Please choose at list one Media from the list", "Updating Details......".
- Unreachable: `FileCabinetContinuityActivity` (launcher commented out in
  `FileCabinetDocumentMainPage.kt:112-121`), the File Cabinet menu item and archive flow, the
  Inter Department tab and every branch for it.

---

## Screens and ids

Read from the layouts, not from a device, the way `BOX_IDS_AND_ROUTES.md` does it. Each list
is every `android:id` the file declares — including the ids put on `<include>` tags, which
**replace** the included layout's own root id at runtime (so `emptyView`, never `emptyMainLi`).
The toolbar's own chrome (`custom_toolbar` and its calling / no-internet strips) is listed once
at the end.

### `activity_continuity_page.xml` — 4 ids

`ContinuityPage` — the two tabs and the shared `progress` bar.

```
  progress  tabLayout  toolbar  viewPager
```

### `custome_tab_with_icon.xml` — 3 ids

The custom view of each tab: name in `tv_title`, info icon `img_info`, unread `badge`.

```
  badge  img_info  tv_title
```

### `continuity_department.xml` — 7 ids

Both tabs (`ContinuityDepartment`). Search include id `searchView`. Includes: `empty_view`, `search_edit_text_widget`.

```
  emptyView  ivCloseNotes  notesHolder  recDepartment  searchView
  takeSceneShots  textNotes
```

### `continuity_department_item.xml` — 3 ids

Tab row and All-Department department row. The row root has no id.

```
  arrowIv  badgeTxt  textView
```

### `continuity_scene_list_page.xml` — 12 ids

`ContinuitySceneListPage` — the grid, the department list, and the selection bar `layForward`. Includes: `empty_view`, `progress_layout_with_overlay`, `search_edit_text_widget`, `shimmer_dummy_grid_view`.

```
  btnCancel  btnForward  emptyView  layForward  linearProgress
  loaderView  progress_circular  recDepartmentScene  recScene  searchView
  toolbar  tvCount
```

### `scene_item.xml` — 11 ids

Grid card. `actorName` is always GONE.

```
  actorName  bottomLayout  imgMore  imgScene  imgSelection
  layMainCard  layOverlay  playPauseImage  tvDetail  tvNotes
  tvScene
```

### `dialog_add_scene_continuity.xml` — 12 ids

Add Scene / Edit Details sheet. Fields by include id: `scene_no`, `episodeContinuity`, `scene_notes` (`episode`, `actor_name`, `gender` never shown); extra details card `addMoreField`; submit `submit_btn`. Includes: `card_po_expandable`, `common_button_with_progress`, `edit_text_drop_down`, `edit_text_widget_no_margins`.

```
  actor_name  addMoreField  btn_cancel  episode  episodeContinuity
  gender  moreFileHolder  recMoreField  scene_no  scene_notes
  submit_btn  tvTitle
```

### `item_continuity_more_layout.xml` — 5 ids

One extra-detail row. `imgEdit` is never shown.

```
  descriptionTv  iconHolder  imgDelete  imgEdit  titleTv
```

### `dialog_edit_text_multiple.xml` — 5 ids

"Add More Details" — `editText1` Title, `editText2` Description. Includes: `edit_text_widget`.

```
  editText1  editText2  header  leftButton  rightButton
```

### `dialog_picker.xml` — 2 ids

`PickerDialog` — "Choose below".

```
  rvItems  tvTitle
```

### `item_picker_grid.xml` — 2 ids

Picker item (`tvLabel`: Photo · Gallery · Video · Video Gallery · Document).

```
  ivIcon  tvLabel
```

### `rv_option_view.xml` — 4 ids

One row of the card ⋮ `PopupWindow`.

```
  badge  imgaeIcon  mainTitle  view13
```

### `common_dialog_layout_with_button.xml` — 9 ids

Delete / Forward / Distribute confirms, rights Alert, tab info.

```
  btn_cancel  btns  checkBoxContainer  distributeBtn  header
  leftButton  message  rightButton  viewBtn
```

### `custom_image_viewer.xml` — 14 ids

`CustomImageViewer` — the in-app viewer a card opens.

```
  backBtn  backdata  constraintLayout14  constraintLayout5  forward
  headerName  imageVw  mainViewPage  materialLinearProgressBar  moreActions
  nameText  swipeHandler  timeData  viewpager
```

### `activity_users_list_page.xml` — 9 ids

`UsersListPage` (purchase_order package) — Forward → Select User. Includes: `custom_button_orange`, `empty_view`, `progress_layout`, `search_edit_text_widget`.

```
  btnAddMoreDepartmentLevel  emptyView  horizontalRv  progress  recDepartments
  refresh  searchView  selectAll  toolbar
```

### `activity_file_cabinet_continuity.xml` — 5 ids

`FileCabinetContinuityActivity` — **unreachable**, listed for completeness. Includes: `no_data_found_layout`, `progress_layout_with_blur_back`.

```
  emptyView  progress  recScene  recTab  toolbar
```

### `custom_tabbar_item.xml` — 4 ids

File Cabinet chip — unreachable with its screen.

```
  badge  line  menuText  shimmerView
```

### `common_list_item_with_delete.xml` — 3 ids

Imported by `ContinuityMoreAdapter`, never inflated.

```
  deleteIv  memberHolder  memberNameTv
```

### `edit_text_widget_no_margins.xml` — 3 ids

Text field include — every dialog field repeats these.

```
  editTextInput  editTextWidget  mainView
```

### `edit_text_widget.xml` — 3 ids

Text field include in "Add More Details".

```
  editTextInput  editTextWidget  mainView
```

### `edit_text_drop_down.xml` — 3 ids

The `gender` include — never shown.

```
  autoCompleteDropDown  editTextWidget  text_dropdown_parent
```

### `card_po_expandable.xml` — 18 ids

The "Add More Details" card (`addMoreField`); only `tvTitle` and `btnAdd` are used. Includes: `custom_button_green`.

```
  addressTv  addressValue  bookSupplierBtn  btnAdd  btnDropDown
  card_po_holder  contactPersonTv  contactPersonValue  editBtn  emailTv
  emailValue  laySupplier  removeBtn  supplierDetailsLi  supplierNameTv
  supplierNameValue  tvCountry  tvTitle
```

### `common_button_with_progress.xml` — 3 ids

Submit include (`submit_btn`).

```
  btnProgress  layBtnProgress  tvButton
```

### `search_edit_text_widget.xml` — 4 ids

Search include (`searchView`).

```
  imageView6  main_view_search  searchHolder  search_widget_edit_textvw
```

### `empty_view.xml` — 3 ids

Empty state — no text.

```
  emptyMainLi  iconIv  textTv
```

### `shimmer_dummy_grid_view.xml` — 5 ids

Grid shimmer (`loaderView`) — stays on after an error.

```
  lay1  lay2  lay3  lay4  loadingShimmer
```

### `progress_layout_with_overlay.xml` — 3 ids

Lottie overlay during delete (`progress_circular`).

```
  animationView  progressMain  progress_widget_util
```

### `custom_toolbar.xml` — 17 ids

Every screen's toolbar (include id `toolbar`).

```
  arrowDownIv  backBtn  badge  callingTimer  customIcn
  headerText  imgCustIcon  infoTap  mainView  moreHolder
  noInternet  projectName  scannerBtn  selectTv  settingImage
  toolLi  toolbar2
```

---

## How each screen has to be read

The four traps, screen by screen. Where a screen is not listed under a trap, it does not have
it.

**1. Element-relative XPath never looks upward.**
Every card repeats the same ids (`layMainCard`, `tvScene`, `tvNotes`, `tvDetail`, `imgMore`,
`layOverlay`). Finding "the ⋮ of the card that says Scene - 12" by walking up from `tvScene`
returns nothing. Downward scoping *is* safe and is the right tool for the dialog (trap 2).

**2. Containers are pruned — slice by geometry.**
- *Tab rows* (`continuity_department_item`): the row root has **no id**; anchor on
  `textView` and use `bandsOf("textView")` to pair a row with its `badgeTxt` and `arrowIv`.
- *Grid cards* (`scene_item`): two columns, so a band holds **two** cards. Anchor on
  `tvScene`, band by y, then split the band by the anchor's x — `bandsOf` alone is not enough
  for a grid. Selection is only visible as `layOverlay` / `imgSelection` being present; there
  is no `checked` or `selected` attribute.
- *The Add/Edit dialog*: every field is the same include, so `editTextInput` appears up to
  four times, each with `content-desc="Zillit"`. Scope **downward from the include id**:
  `scene_no`, `episodeContinuity`, `scene_notes` (`actor_name` and `episode` are GONE).
  There are also two `tvTitle`s — the dialog header and the "Add More Details" card's title
  inside `addMoreField`.
- *Add More Details dialog*: `editText1` (Title) and `editText2` (Description), each wrapping
  an `editTextInput`; buttons `leftButton` "Cancel", `rightButton` "Add".

**3. Absent means not rendered, not "the app shows nothing".**
- `recScene` and `recDepartmentScene` swap between GONE and VISIBLE — the one not in use is
  not in the tree at all.
- The empty state has no text; assert on `emptyView`.
- A grid taller than the screen hides its lower cards; `readWholeList` / `scrollToRow` before
  every per-card tap (CT-36 exists to prove they work here).
- `ViewPager2` keeps the other tab alive once visited, so there are two `searchView`s, two
  `recDepartment`s. Scope to the visible page.
- Validation errors are `EditText.setError` popups, not snackbars or toasts. Assert that the
  dialog is still open and read the error text from the field's error window; if that proves
  unreadable, the dialog staying open is the assertion.
- Snackbars use the Material `snackbar_text` id; the Distribute and Select-User messages are
  **toasts**.

**4. Widget classes that are not what they look like.**
- Tabs are **custom views** (`custome_tab_with_icon`): the name is in `tv_title`, not the
  TabLayout's own text, and `img_info` inside the tab is a clickable icon that steals taps.
- The ⋮ menu is a `PopupWindow` whose rows carry `mainTitle` (label) and `imgaeIcon` (sic).
  The Forward target menu and the long-press menus are platform `PopupMenu`s: item titles are
  `android:id/title` — the **android** package's id, as with Spinner rows.
- The submit "button" is a `MaterialCardView` (`submit_btn`) whose label is `tvButton`.
- No Spinner or working AutoCompleteTextView anywhere in the module — the one dropdown
  (`gender`) is never shown.
- Icon-only views with **no contentDescription**: `takeSceneShots`, `imgMore`, `ivCloseNotes`,
  `img_info`, `btn_cancel`, `imgDelete`, `arrowIv`. Locate by id.

**Non-cancelable layers, learned on the device.** The Add/Edit sheet, "Add More Details" and
every confirm dialog are `setCancelable(false)`: BACK does nothing to them. A first version of
the suite pressed BACK at an Edit sheet in a loop for twenty minutes. Recovery now closes them
with their own buttons (`btn_cancel`, `leftButton`) before any BACK, and every case starts by
clearing whatever a failed case left on top.

**Things that animate.** `loaderView` (shimmer, auto-start, visible at launch and stuck on
error — finding 5), `progress` (indeterminate bar on the pager, spins until the create socket
lands), `progress_circular` (Lottie, during delete). Video does **not** auto-play; it only
runs in the viewer after `playBtn`, so never tap play.

---

## Fixtures

Already pushed by the runner — nothing new is needed:

| File | Used for |
|---|---|
| `/sdcard/Pictures/zl_test_1.jpg` … `_3.jpg` | the records themselves, picked through "Gallery" (the system photo picker the chat media suites already drive) |
| `/sdcard/Documents/zl_test_doc.pdf` | at most one document record, to check the card shape — never tapped |

**Naming.** A scene number must start with a digit and is cut at 15 characters, so the
`ZL_AUTO` prefix cannot lead it. Proposed: scene number `99ZLAUTO<4-digit run id>`
(12 characters, e.g. `99ZLAUTO4821`) and notes `ZL_AUTO_<run id> …`. The leading `99`
keeps them clear of a real production's scene numbers (the list order is the server's — there
is no client sort, so do not rely on where they land); CT-99 sweeps on `ZLAUTO` in the scene
number.

---

## From the QA sheet

The sheet's **Continuity** tab, read 2026-11-21. It is a **bug sheet**, not a verdict grid:
each row has a *Layer* (Web / Backend) and no Web / Android / Desktop / iOS columns, so
"blank means never tested on Android" applies to every row — none of them was found on
Android. All eleven are Open. Every row was found on the web client or by calling the backend
directly; the column below says whether an Android user can reach it.

| Sheet row | Layer · severity | What it says | Reachable from the Android UI? | Case |
|---|---|---|---|---|
| BUG-06 | Backend · High | Edit, share, archive or delete work on another project's item — the lookup checks only the id | No — the app only ever sends ids from the open project's own lists | — |
| BUG-07 | Backend · High | Reads are not department-checked: asking with another department's id returns its private items | **Yes, possibly** — the All Department grid sends a `departmentId` with `showall=true` | **CT-86** |
| BUG-13 | Backend · Medium | Create accepts client-set `visibility`, `deleted`, `created` | No — the app never sends them | — |
| BUG-14 | Backend · Medium | Edit replaces the whole item, wiping any field not sent; notifies before validating | **Yes** — the Android edit sends no `attachment` / `unique_id` (finding 10) | **CT-41**, CT-43 |
| BUG-15 | Backend · Medium | Download does not check deleted / rights; PDF stamp only when the type is exactly `pdf` | Partly — the stamp half, through ⋮ → Download | **CT-62** |
| BUG-17 | Backend · Low | A non-number paging timestamp is a server error | No — the app sends a real timestamp | — |
| BUG-18 | Backend · Low | Search with `[` or `(` is a server error | No server search on Android; guarded anyway | **CT-24** |
| BUG-20 | Backend · Low | Some messages show a raw internal code, untranslated | **Yes** — the delete snackbar and error snackbars show the server's `message` | **CT-71** |
| BUG-22 | Backend · Low | Archive with a bad id is a server error | No — archive (File Cabinet) is unreachable on Android | — |
| WEB-06 | Web · Low | Web's live updates miss zero-padded / non-numeric scene numbers; a typo stops its delete handler | Android's own socket path, same scenario | **CT-25** |
| WEB-08 | Web · Low | Web's upload `accept` list is malformed | No — web only | — |

---

## Wiring (when the suite is written)

| Place | Value |
|---|---|
| `TestOrder` | `CONTINUITY = 175` in a new **170–189 tools overflow ← VIDYA** range (see the note in `SCHEDULE_DIST_MODULE_TESTCASES.md`) |
| Runner flag | `--continuity` |
| `MODULES` | `"Continuity::ContinuityTest"` — not per-suite; nothing in the module animates continuously once loaded |
| `make-report.mjs` | `n.includes('continuity')` → `Continuity`, nav "Tools → Continuity" |
| `.run` | `Zillit_Continuity.run.xml`, `Zillit ▸ Continuity`, `--continuity` |
