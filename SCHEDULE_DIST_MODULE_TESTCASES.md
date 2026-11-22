# Schedule Distribution — test cases and findings

**Schedule Distribution** is where the production's shooting schedule is handed out as PDFs:
the full schedule, the one-line schedule, and individual schedule *pages* filed by scene.
Tools → **Schedule Full & One Line** opens `bottomNav/tools/schedule/ScheduleDistActivity`, a
three-tab `TabLayout` + `ViewPager2` over `{host}api/v2/schedule-distribution/`.

> **Its Tools tile is not called "Schedule Distribution".** The label is server-supplied, and
> on the automation project it reads **"Schedule Full & One Line"** (checked on the emulator;
> the identifier is `schedule_distribution_tool`). Searching Tools for "Schedule" also finds
> "Schedule D.O.D." (`DodActivity`, a different tool) and "Production Diary/Box Schedule".
> The suite must open the tile by that exact name.

It is a near line-for-line clone of **Script Distribution** (`bottomNav/tools/script`):
`ScheduleVM` and `ScriptVM` are each 436 lines, and the two share the row layouts, the upload
screen (`UploadDocumentActivity`), the count and history screens, the request codes and one
socket key. A bug found here is worth checking there. **Sides does not share this code**
despite its stale "Script-Distribution API" comment.

**Status: automated and run on the emulator (2026-11-21, project ZL-Android-Automation).**
Suite [`ScheduleDistTest`](src/test/java/com/zillit/automation/tests/scheduledist/ScheduleDistTest.java),
`TestOrder.SCHEDULE_DIST = 170`, page objects `ScheduleDistPage` and `ScheduleUploadPage`
(on the shared `VisiblePage` base). Last full run: **44 cases — 40 passed, 2 failed, 2
skipped, 13 minutes; no app crash or ANR in logcat.** Both failures are app defects
reproduced on the device: SD-04 (finding 11, a blank Pages tab) and SD-39 (finding 4). The
skips are the opt-in publish pair SD-51 / SD-52 (`-Dzillit.schedule.publish=true`). Fixes
made after that run (SD-46's own scene, the sweep reading the unfiltered folder list) were
each re-run on their own and pass. Not automated: SD-03's no-rights half, SD-06 and SD-45
(need a second account without view or posting rights). One run:

```bash
./run-ui.sh --schedule-dist
```

**How to read this sheet.** Every case says what a user does and what must then be true.
Quoted wording is the app's own, because the automation will match on it; `id` in backticks
is a real resource id, read from the layout. Cases marked **[QA <row>]** carry a row of the
module's tab on the QA bug sheet (see *From the QA sheet*); the rest come from reading the
source.

---

## The shape of the data

```
Schedule Full                one PDF per upload — NO name (schedule_name is always null)
  ├── schedule_date          optional, the only thing the user types besides the file
  ├── episode                TV projects only
  └── versions               Replace uploads a new one; the old one moves to History
Schedule One Line            identical shape, its own endpoint (oneline)
Pages                        PDFs filed by scene:
  └── folder per scene       ← keyed by scene_number ALONE (finding 4)
        └── page             scene_number, schedule_type (Full / One Line pages),
                             page number (free text, ≤ 40), colour, page date, episode
```

The three tabs (`tabLayout`, plain Material tabs — no custom view):

| Tab | Fragment | Upload button (`addFb`) | Row actions (⋮) | Delete? |
|---|---|---|---|---|
| "Schedule Full" | `ScheduleFragment` | "Upload Full Schedule" | View · Download · Publish to Doc Distribution · Replace · [Download Count · View Count] | **never** |
| "Pages" | `SchedulePageFolderFragment` | "Upload Page" | folders have **no** ⋮; inside a folder: View · Download · Publish to Doc Distribution · [counts] · Delete | pages only, admin or uploader |
| "Schedule One Line" | `ScheduleOneLineFragment` | "Upload One Line" | as Schedule Full | **never** |

`[…]` = admin only. Swiping between tabs is switched off (`isUserInputEnabled = false`), so
every tab change is a tap.

Gates (from `postingRightsList[schedule_distribution_tool]` unless noted):

| Action | Gate | When it is shut |
|---|---|---|
| Open the tool | `viewAccess == true` | `finish()`, no message |
| See an Upload button | `postingAccess` **or** admin | button absent |
| Actually upload | `postingAccess` only — admin is not enough (finding 3) | toast "You do not have Posting Rights on this Tool. Do you want to request one of the Admins…" |
| Replace (Full / One Line) | `postingAccess` or admin | item absent |
| Download | `downloadAccess` — no admin override | "Alert" dialog offering to **message the admins** (Yes is a real side effect) |
| Download / View Count | admin | items absent |
| Delete (pages) | admin or uploader | snackbar "Only Admin or `<uploader>` can delete." |
| Publish to Doc Distribution | admin or `document_distribution_tool` posting access | snackbar "You don't have permission to distribute to Document Distribution" |

**Full and One Line cannot be cleaned up from the UI.** There is no Delete for them anywhere
(finding 1), so every upload the suite makes stays on the project. The plan therefore
**uploads once and replaces thereafter** — see *Fixtures*.

---

## 1. Module shell · `SD-0`

| # | Case | Expected |
|---|---|---|
| SD-01 | Tools → "Schedule Full & One Line" | Three tabs in order: "Schedule Full", "Pages", "Schedule One Line"; Schedule Full selected; "History" offered |
| SD-02 | Tap each tab | Each shows its own list and its own Upload label. The page next door is already attached off-screen (`offscreenPageLimit = 1`), so every lookup must be scoped to the visible page — the same ids live in all three |
| SD-03 | Upload buttons follow the rights | With posting access, each tab offers its Upload button; without it, none does. *Skips the negative half without a non-posting account* |
| SD-04 | Empty tab | `emptyView` is shown. It is an image with **no text** — assert on the id. *Skips when the tab has rows* |
| SD-05 | Swipe does nothing | A horizontal swipe on the list leaves the same tab selected (swiping is disabled on purpose — this guards against an "it was always on Schedule Full" false pass) |
| SD-06 | No view access **[QA BUG-11]** | The Schedule Distribution tile is not on Tools, and opening the screen any other way closes it. Sheet BUG-11 found the backend serves the lists and downloads to anyone in the project, so this client gate is the **only** gate. *Needs an account without view access — skips without one* |

## 2. Schedule Full · `SD-1`

| # | Case | Expected |
|---|---|---|
| SD-10 | Upload Full Schedule → picker | "Choose below" offers a single item, "Document"; it opens the system picker filtered to PDF |
| SD-11 | Pick the PDF → confirm → form | `GalleryItemView` (confirm with `sendBtn`, an icon-only card), then the upload form: a date row "Click to add date (Optional)" and Submit. No name field, no colour, no scene, no type. Episode only on a TV project |
| SD-12 | TV project, bad episode | Refused — "Please enter a valid episode no. only digits allowed." *Skips on a non-TV project* |
| SD-13 | Submit with a schedule date | The button reads "Uploading Document" while it works; the screen closes with "File Uploaded Successfully" (or the server's message); the list has a new row reading "Schedule date :  `<DATE>`" with the date picked. Dates are shown **upper-case** (`JAN 05, 2026`) and with a double space after the colon |
| SD-14 | Row shape | "Uploaded by : `<me> (<department>)`", "Schedule date :  …", "Uploaded On :  `<created>`", ⋮. "Episode: …" only on a TV project |
| SD-15 | The ⋮ menu (admin, posting) | "View", "Download", "Publish to Doc Distribution", "Replace", "Download Count", "View Count" — in that order, and **no "Delete"** (finding 1 — the case pins it so that adding Delete is noticed) |
| SD-16 | Replace **[QA BUG-12]** | A new upload against the same row: afterwards the list shows the new version **exactly once** — never zero rows, never both (the old one is filtered out as `replaced`) — and **History** lists the old one. Sheet BUG-12: a replace the backend rejects half-way can leave the project with no current version |
| SD-17 | View | Opens the PDF **in an external app** (the app fires `ACTION_VIEW`). The case confirms a foreign package is in front, then presses back; the list is refetched on return |
| SD-18 | Download | "File Downloaded Successfully." — or the rights Alert, answered **Cancel** (never Yes: Yes messages the admins). Where `mobile: shell` is allowed, the file is in `/sdcard/Documents/Zillit/` and its name does not contain `null` (expected to fail — finding 7). *The file check skips without relaxed security* |
| SD-19 | View Count after a View | Count screen titled "View Count" lists the viewing user. Download Count likewise after SD-18 |

## 3. Schedule One Line · `SD-2`

| # | Case | Expected |
|---|---|---|
| SD-20 | Upload One Line | As SD-10…13, against `oneline`; the row reads "One Line date :  `<DATE>`" |
| SD-21 | ⋮ menu | As SD-15 — again with no Delete |
| SD-22 | Replace **[QA BUG-12]** | As SD-16, and the old version is in One Line's own History, not Schedule Full's |
| SD-23 | Pull to refresh | The pull should reload the list. Today it only stops its own spinner (finding 6). Proving "it did not reload" needs a change made elsewhere while the tab is open, which one device cannot do without the socket refreshing the list anyway — so on one device the case checks only that the spinner clears and the list is intact. The reload itself belongs to a two-device run |
| SD-24 | The tabs keep their own data | Nothing uploaded in SD-13 appears on One Line, and nothing from SD-20 on Full |

## 4. Pages — folders · `SD-3`

| # | Case | Expected |
|---|---|---|
| SD-30 | Upload Page → the form | After the picker: Scene No. (required), "Select Color (Optional)", Page Date, "Select Schedule Type" (required), "Schedule Page no. (Optional)"; Episode on a TV project |
| SD-31 | Blank scene | Refused — "Please Enter Scene No." |
| SD-32 | Scene starting with a letter | Refused — "Scene No. always starts with a number." |
| SD-33 | No schedule type | Refused — snackbar "Please select if these pages are full schedule or one line schedule" |
| SD-34 | The two dropdowns | Colour offers the eleven page colours WHITE · BLUE · PINK · YELLOW · GREEN · GOLDENROD · BUFF · SALMON · CHERRY · TAN · IVORY; type offers "Full Schedule Pages" and "One Line Schedule Pages" |
| SD-35 | Upload a page | Back on Pages, a folder row "Scene No. `<n>`" whose background is the colour picked, with "Page Date : …", "Schedule Type : …" and the uploader |
| SD-36 | Search by scene | The filter shows "Sn No."; typing the scene leaves that folder (server-side search — allow the 700 ms debounce before reading) |
| SD-37 | Search by colour | Filter → "Search by color" → pick the fixture's colour → its folder is listed; a different colour does not list it |
| SD-38 | Search by episode | Offered only on a TV project ("Search By Episode No."). *Skips elsewhere* |
| SD-39 | One scene, both types | Upload a "Full Schedule Pages" page and a "One Line Schedule Pages" page for the **same** scene: both must be reachable. Expected to fail — the folder list collapses them into one (finding 4) |

## 5. Pages — inside a folder · `SD-4`

| # | Case | Expected |
|---|---|---|
| SD-40 | Open a folder | Title "Pages" (not the scene — the scene is on each row); the page rows: "Scene No. `<n>`", "Page No. `<p>`", "Uploaded On :  …", "Schedule Type : …" |
| SD-41 | What this screen does not offer | No search, no filter, no Upload, no History — all GONE (finding 2). The case pins it |
| SD-42 | ⋮ menu | "View", "Download", "Publish to Doc Distribution", [counts], "Delete" — and **no "Replace"** |
| SD-43 | Delete asks first | "Delete" — "Are you sure you want to delete?" — No keeps the page |
| SD-44 | Delete | Yes → snackbar with the server's message; the page leaves the folder, and the folder leaves the Pages tab once it was the last page |
| SD-45 | Delete someone else's page | Snackbar "Only Admin or `<uploader>` can delete." — the page stays. *Needs a second, non-admin account* |
| SD-46 | Many pages | Builds enough pages in one folder to scroll, reads them all back. Exercises the next/previous paging this screen has and the scrolling readers |

## 6. Publish to Doc Distribution · `SD-5`

| # | Case | Expected |
|---|---|---|
| SD-50 | Confirm wording | "Publish to Document Distribution" — "\"`<file>`\" to the Document Distribution library?" with Cancel / Publish. Cancel publishes nothing |
| SD-51 | Publish | "Sending to Document Distribution…", then "Published." — "\"`<file>`\" was added to Document Distribution." In Document Distribution the file is under **Schedule Full & One Line › `<tab>` › `<episode or scene>`**. Opt-in only: it writes to another module's library |
| SD-52 | View after Publish | Tapping a row straight after a publish opens the PDF, **not** another Publish dialog (guards finding 5 on the success path) |

## 7. History · `SD-6`

| # | Case | Expected |
|---|---|---|
| SD-60 | Schedule Full → History | "Schedule Full History", banner "Records of deleted messages"; after SD-16 the replaced version is listed, with View and Download only |
| SD-61 | Schedule One Line → History | "Schedule One Line History", with One Line's replaced version |
| SD-62 | Pages → History | "Schedule Pages History", banner "Records of deleted Folders"; after SD-44 the deleted page's folder is reachable here. Expected to fail as written: level 1 calls the same request as the live tab (finding 9) |
| SD-63 | History search | The search box filters the history rows locally |
| SD-64 | Every history loads, as admin **[QA BUG-04]** | Schedule Full, Schedule One Line and Pages history each end in a list or the empty view — not an error snackbar, and not a loader that never goes. Sheet BUG-04: the backend's *archived* Schedule and Pages lists fail with a server error for admins while One Line's works. Whether Android's History calls those same archived lists (it sends `replaced=` / `deleted=` filters on the ordinary list routes) is not visible from the client — this case is what settles it |

## 8. Cleanup · `SD-99`

| # | Case | Expected |
|---|---|---|
| SD-99 | Sweep | No **page** whose page number carries `ZL_AUTO` (or scene number `99ZLAUTO…`) is left on the project, including leftovers from a broken run and folders below the fold. Full and One Line cannot be swept — the sweep asserts instead that there is **at most one** automation row on each, the sentinel (see *Fixtures*) |

---

## Findings

Read from the source; each is something a user can see. Paths are under
`bottomNav/tools/schedule/` unless given in full.

**1. A Full or One Line schedule can never be deleted — `SD-15`, `SD-21`**
`viewmodel/ScheduleVM.kt:199` adds "Delete" only when the screen is *neither* Full nor One
Line. The delete confirm, the request and its handler all exist in both fragments
(`views/ScheduleFragment.kt:238-251, 442-459`; `ScheduleOneLineFragment.kt:232-239, 425-441`)
and are dead. A wrong upload can only be buried under a Replace.

**2. Inside a Pages folder there is no search, filter, upload or history — `SD-41`**
`activity_schedule_page_detail.xml:33` declares `searchHolder` GONE and nothing shows it;
`views/SchedulePageDetailActivity.kt:235, 238` force `addFb` and `userHistoryBtn` GONE. The
search, filter and colour menus behind them (`:150-209`) are complete and unreachable.

**3. Admins are offered an Upload they cannot use — `SD-03`**
The button shows for posting access *or* admin (`ScheduleVM.kt:312-320`); the tap requires
posting access alone (`ScheduleFragment.kt:140`, `ScheduleOneLineFragment.kt:137`,
`SchedulePageFolderFragment.kt:150`). An admin without the posting flag gets a toast worded as
a question — "Do you want to request one of the Admins…?" — with no way to answer it.

**4. Full and One Line pages for the same scene collapse into one folder — `SD-39`** · *confirmed on the emulator (4 of 5 runs)*
Seen on 2026-11-21: scene `99ZLAUTO2506` held a Full Schedule page and a One Line page; the
Pages tab showed one folder, and inside it only the Full page. On another run the same case
passed — which folder survives the collapse depends on list order and the cache race of the
"smaller" list below, so expect this case to fail intermittently rather than always.
`adapters/SchedulePageFolderAdapter.kt:107` — `distinctBy { it.sceneNumber }`; the diff key
and the Realm primary key (`databases/ScheduleDistDBManager.kt:479-480`) are the scene alone.
The surviving folder opens filtered to its own `scheduleType`, so the other type's pages for
that scene are unreachable. The Realm key also ignores the project, so the same scene number
in two projects overwrites one cached folder with the other.

**5. A failed publish turns the next View into a publish — `SD-52`**
`pendingDistribute` is set before the fetch and cleared only on success
(`ScheduleFragment.kt:519-524`; same in One Line and the folder detail). After one failed
fetch, the next successful **View** of *any* row skips opening it and runs the publish for
the old row — the user sees "Publish to Document Distribution" instead of a PDF.

**6. Pull to refresh does nothing — `SD-23`**
One Line's `SwipeRefreshLayout` listener only stops its own spinner
(`ScheduleOneLineFragment.kt:185-187`). Schedule Full's is commented out
(`fragment_schedule.xml:19-35`).

**7. Downloaded file names contain "null" — `SD-18`**
Full and One Line uploads always send `schedule_name = null`
(`bottomNav/tools/forms_signature/activity/UploadDocumentActivity.kt:1189, 1252`), and the download names the
file after it: `/Documents/Zillit/<Project>_null_<timestamp>.pdf`.

**8. A failed download freezes that file**
The spinner is cleared only when the download succeeds (`ScheduleFragment.kt:485-486`), and
`DownloadingHelper` removes the file from its queue only on success and returns early while
it is queued (`downloadingHandler/DownloadingHelper.kt:562, 613-647`, under `java/com/zillit/zillitapp/`). After one failure, View and
Download of that file do nothing and the Lottie overlay stays until the user leaves. On the
folder detail the download callback also touches views off the main thread
(`SchedulePageDetailActivity.kt:567-583`) — a likely crash on AWS-storage projects.

**9. Pages History's first screen is not history — `SD-62`**
`bottomNav/settings/activity/UserHistoryFolderPage.kt:203-204` has the `deleted` parameter
commented out, so it lists the *live* folders under the banner "Records of deleted Folders".
Full and One Line history also stop at 100 rows (no paging for these identifiers).

**10. The download gate ignores admin and can message the admins**
`utils/OptionsHandler.kt:837-866, 920-933` checks `downloadAccess` with no admin override
before the screen sees the tap; "Yes" sends a request to the admins. The screens' own
"You don't have permission to Download any file." is unreachable behind it.

**11. With every folder deleted, the Pages tab is blank — `SD-04`** · *confirmed on the emulator*
`GET page-folders` keeps returning folders after their pages are deleted, each marked
`deleted` (seen in logcat: `"status":1` with three folders, all with a `deleted` timestamp).
The tab hides them, but decides the empty state from the unfiltered list
(`SchedulePageFolderFragment.kt:429-430`, `isVisible = data.isNullOrEmpty()`), so the user
gets neither rows nor the empty image — a blank screen indistinguishable from a failed load.

Smaller, not worth a case of their own:

- The Pages cache (`ScheduleDistDBManager`) stores only scene, type, created and deleted, and
  the cached list can land *after* the fresh one (`ScheduleVM.kt:326-342`), replacing complete
  rows with bare ones. Every Pages-tab read should wait for a row field the cache lacks (page
  date or uploader) before trusting the list.
- Recycled rows keep stale text: holders set fields VISIBLE and never back to GONE
  (`holder/ScheduleHolder.kt:20-23`, `holder/SchedulePageHolder.kt:40-48, 65-68`).
- "Episode: " shows with an empty value on TV projects; "Uploaded by : " shows with a blank
  name for an unknown user; double spaces after every "…: " label; the string `updated_on`
  reads "Uploaded On" and prints `created`.
- `tv_page_date` is filled and then hidden on every folder-detail row.
- The `page:replaced` socket key is shared with Script Distribution, and on the Pages tab it
  throws away the current search filter.
- The tab chosen by a push notification is ignored when the screen is already open
  (`SINGLE_TOP` with no `onNewIntent`).
- `filterIv`'s content-desc is "Search".

---

## Screens and ids

Read from the layouts, not from a device, the way `BOX_IDS_AND_ROUTES.md` does it. Each list
is every `android:id` the file declares — including the ids put on `<include>` tags, which
**replace** the included layout's own root id at runtime (so `emptyView`, never `emptyMainLi`).
The toolbar's own chrome (`custom_toolbar` and its calling / no-internet strips) is listed once
at the end.

### `activity_schedule_dist.xml` — 3 ids

`ScheduleDistActivity` — the three tabs.

```
  tabLayout  toolbar  viewpager
```

### `fragment_schedule.xml` — 8 ids

Tab "Schedule Full" (`ScheduleFragment`). `refresh` is commented out in the XML; `addFb` is GONE until rights allow. Includes: `empty_view`, `progress_layout`, `shimmer_dummy_grid_view`.

```
  addFb  circularProgress  emptyView  loaderView  progress
  recDocument  refresh  userHistoryBtn
```

### `fragment_schedule_one_line.xml` — 8 ids

Tab "Schedule One Line" (`ScheduleOneLineFragment`). `refresh` exists but does not reload. Includes: `empty_view`, `progress_layout`, `shimmer_dummy_grid_view`.

```
  addFb  circularProgress  emptyView  loaderView  progress
  recDocument  refresh  userHistoryBtn
```

### `fragment_schedule_page_folder.xml` — 12 ids

Tab "Pages" (`SchedulePageFolderFragment`). `searchHolder` appears twice (this row and the widget's own); the shimmer include is commented out. Includes: `empty_view`, `progress_layout`, `search_edit_text_widget`, `shimmer_list_view`.

```
  addFb  circularProgress  emptyView  filterIv  loaderView
  operationHolder  progress  recDocument  searchHolder  searchIncluded
  tvOperationType  userHistoryBtn
```

### `activity_schedule_page_detail.xml` — 11 ids

`SchedulePageDetailActivity` — one folder's pages. `searchHolder`, `addFb`, `userHistoryBtn` are never shown. Includes: `empty_view`, `progress_layout`, `search_edit_text_widget`, `shimmer_list_view`.

```
  addFb  circularProgress  emptyView  filterIv  loaderView
  progress  recDocument  searchHolder  searchIncluded  toolbar
  userHistoryBtn
```

### `documents_script_item.xml` — 13 ids

Row for Full / One Line and for folder-detail pages. Snake_case TextView ids; `selectDocRadio` and `layProcessing` are never used.

```
  imgMore  imgPdf  layMainCard  layProcessing  relativeLayoutHolder
  selectDocRadio  tv_created_by  tv_date  tv_episode_no  tv_name
  tv_page_date  tv_scene_no  tv_sub_title
```

### `item_script_folder.xml` — 18 ids

Pages folder row. `imgMore`, `infoIv`, `checkBox`, `imgSelected` are GONE and never shown.

```
  badge  bottomHolder  cardView  checkBox  imgFolder
  imgMore  imgSelected  infoIv  layMainHolder  layProcessing
  linearLayoutHolder  tv_desc  tv_detail  tv_episode_no  tv_page_date
  tv_revision_date  tv_revision_name  tv_sub_title
```

### `activity_upload_document.xml` — 23 ids

`UploadDocumentActivity` (shared with Script Distribution and Forms) — the upload / replace form. Fields by include id: `etEpisodeNo`, `etChooser` (colour), `etSceneName`, `etTypeChooser`, `etDocumentName`; Submit is `btnSubmit`. Includes: `common_button_with_progress`, `edit_text_drop_down`, `edit_text_widget_no_margins`, `progress_layout`.

```
  btnSignature  btnSubmit  dateSelector  etAutoName  etChooser
  etDocumentName  etEpisodeNo  etSceneName  etTypeChooser  layBtn
  layController  laySignController  lay_drag  pdf_viewer  progress
  scrollview  toolbar  tvCancel  tvCount  tvNext
  tvPrev  tvSelectedDate  tvSignPage
```

### `activity_gallery_item_viewer.xml` — 22 ids

`GalleryItemView` — confirm the picked file with `sendBtn`. Includes: `progress_layout`.

```
  allRV  cancel  cancelPreparing  compressText  constraintLayout
  delete  descVw  description  edit  hdImage
  hdTickImg  mediaSelected  progress  progressBar  progressView
  receiverName  sendBtn  speaker  speakerIcon  taggingRvView
  tgVw  vpViewPager
```

### `dialog_picker.xml` — 2 ids

`PickerDialog` — "Choose below".

```
  rvItems  tvTitle
```

### `item_picker_grid.xml` — 2 ids

Picker item (`tvLabel` "Document").

```
  ivIcon  tvLabel
```

### `rv_option_view.xml` — 4 ids

One row of the ⋮ / filter / colour `PopupWindow`.

```
  badge  imgaeIcon  mainTitle  view13
```

### `common_dialog_layout_with_button.xml` — 9 ids

Delete confirm, rights Alert.

```
  btn_cancel  btns  checkBoxContainer  distributeBtn  header
  leftButton  message  rightButton  viewBtn
```

### `activity_user_chat_history_page.xml` — 13 ids

`UserChatHistoryPage` — Full / One Line history, and Pages history level 2. Two `search_widget_edit_textvw`s. Includes: `empty_view`, `progress_layout`, `search_edit_text_widget`.

```
  arrowDownCard  emptyView  headerMsg  ivRemoveSearch  main
  operationHolder  progress  recTab  rvChat  searchHolder
  searchIncluded  searchView  toolbar
```

### `activity_user_history_folder_page.xml` — 12 ids

`UserHistoryFolderPage` — Pages history level 1. Includes: `empty_view`, `progress_layout`, `search_edit_text_widget`.

```
  circularProgress  emptyView  filterIv  headerMsg  main
  operationHolder  progress  recDocument  searchHolder  searchIncluded
  toolbar  tvOperationType
```

### `activity_users_count_list_page.xml` — 5 ids

`UsersCountListPage` (script package) — Download / View Count. Includes: `empty_view`, `progress_layout`, `search_edit_text_widget`.

```
  countRecyclerView  noDataFound  progress  searchIncluded  toolbar
```

### `edit_text_widget_no_margins.xml` — 3 ids

Text field include — every field repeats these.

```
  editTextInput  editTextWidget  mainView
```

### `edit_text_drop_down.xml` — 3 ids

Dropdown include — rows are `android:id/text1`.

```
  autoCompleteDropDown  editTextWidget  text_dropdown_parent
```

### `common_button_with_progress.xml` — 3 ids

Submit button include.

```
  btnProgress  layBtnProgress  tvButton
```

### `search_edit_text_widget.xml` — 4 ids

Search include.

```
  imageView6  main_view_search  searchHolder  search_widget_edit_textvw
```

### `empty_view.xml` — 3 ids

Empty state — `textTv` is GONE and its string is empty.

```
  emptyMainLi  iconIv  textTv
```

### `progress_layout.xml` — 3 ids

`circularProgress` (Lottie) during View / Download.

```
  animationView  progressMain  progress_widget_util
```

### `custom_toolbar.xml` — 17 ids

Every screen's toolbar (include id `toolbar`); `headerText` is textAllCaps.

```
  arrowDownIv  backBtn  badge  callingTimer  customIcn
  headerText  imgCustIcon  infoTap  mainView  moreHolder
  noInternet  projectName  scannerBtn  selectTv  settingImage
  toolLi  toolbar2
```

---

## How each screen has to be read

**1. Element-relative XPath never looks upward.** Full / One Line rows repeat `tv_name`,
`tv_date`, `tv_created_by`, `imgMore` in every row; the ⋮ for "the row dated JAN 05, 2026"
cannot be found by walking up from `tv_name`. Downward scoping is safe (the upload form,
below).

**2. Containers are pruned — slice by geometry.**
- *Full / One Line rows* (`documents_script_item`): the bottom strip that holds the texts and
  `imgMore` has no id. Anchor on `tv_date` (the one TextView visible in every row) and use
  `bandsOf("tv_date")`. Rows are a fixed 150sdp, so only three or four fit on a screen.
- *Folder rows* (`item_script_folder`): root `layMainHolder` does carry an id, but
  `bottomHolder` and `linearLayoutHolder` are layout-only. Anchor on `tv_revision_name`
  ("Scene No. `<n>`").
- *Folder-detail rows* reuse `documents_script_item`; anchor on `tv_scene_no`.
- *The upload form* repeats `editTextWidget` / `editTextInput` / `autoCompleteDropDown` in
  every include. Scope **downward from the include id**: `etEpisodeNo`, `etSceneName`,
  `etDocumentName`, `etChooser` (colour), `etTypeChooser` (type). Submit is `btnSubmit` (a
  card) with `tvButton` "Submit".
- *The Pages search row* has **two** `searchHolder`s (the outer row and the widget's own);
  scope by `searchIncluded` → `search_widget_edit_textvw`.

**3. Absent means not rendered.**
- The three fragments share ids, and the neighbouring page is attached off-screen: two
  `recDocument`s, two `addFb`s. Visible-only lookups.
- `addFb` is GONE without rights — absent is the expected state there, not a failure to load.
- The empty state has no text; assert on `emptyView`.
- `loaderView` (shimmer) covers Full / One Line until the first response; the Pages tab has
  only the linear `progress` bar. Wait for them to go before reading.
- Validation errors are `EditText.setError` popups (except the schedule-type snackbar);
  assert that the form is still open.
- Toasts, not snackbars: posting-rights, upload success, "Loading please wait...", publish
  errors. Snackbars (`snackbar_text`): download done, delete result, "Only Admin…", the
  schedule-type refusal.

**4. Widget classes that are not what they look like.**
- Tabs are plain Material tabs: a `TabLayout$TabView` (reports as `LinearLayout`) with a
  TextView child holding the name. The unread count is a `BadgeDrawable` — **an overlay, not a
  view** — so badge counts are not readable from the tree; do not locate a tab by content-desc,
  which the badge may rewrite.
- The colour and type dropdowns are `AutoCompleteTextView`s; their rows are
  `android.widget.CheckedTextView` with **`android:id/text1`** — the android package's id.
  **Seen on the device:** the rows are drawn but ABSENT from the tree. An AutoCompleteTextView's
  popup is a non-focusable window (the field keeps focus) and UiAutomator2 reads only the
  focused window — a Spinner's popup takes focus, which is why Spinner rows are found. The
  page object switches Appium's `enableMultiWindows` on while a dropdown is open, and scrolls
  inside the popup: it shows nine of the eleven colours.
- The ⋮, filter and colour menus are focusable `PopupWindow`s: rows are cards with `mainTitle`
  (label) and `imgaeIcon` (sic). Not dismissed by an outside tap — press back.
- Upload / History are `ExtendedFloatingActionButton`s, which report as `android.widget.Button`
  with text; the `srcCompat` icon does not render.
- The toolbar title (`headerText`) is `textAllCaps` — match case-insensitively.
- Icon-only views with no content-desc: `imgMore`, `iconIv`, `imageView6`, `sendBtn`.
- Two screens are another app: the system PDF picker (DocumentsUI) and the external viewer
  "View" opens.

---

## Fixtures

| File | Used for |
|---|---|
| `/sdcard/Documents/zl_test_doc.pdf` | already pushed and `touch`ed by the runner, so it is on the picker's first screen. Every upload uses it |

**Naming.** Full and One Line rows carry no name at all, so they are recognised by a
**sentinel episode**, `9900`: on a TV project every row shows "Episode: <n>", and the form
requires one. The first run on a project uploads one sentinel row per tab; every later run
**Replaces** it (SD-16, SD-22), so the live lists never hold more than one automation row
each. (History keeps the superseded versions; there is no way to remove them.) On a project
that is not TV the rows carry nothing the suite could recognise, so the Full / One Line
upload cases skip there rather than leave an unidentifiable, undeletable row behind. (An
earlier draft of this plan used a far-future schedule date; the system date picker makes
reaching 2099 impractical, and the episode is already on every row.)

Pages are named properly: scene number `99ZLAUTO<4-digit run id>` (a scene must start with a
digit and is cut at 15) and page number `ZL_AUTO_<run id>_<n>`. Pages are deletable, and
SD-99 sweeps them.

---

## From the QA sheet

The sheet's **Schedule Distribution** tab, read 2026-11-21. It is a **bug sheet**, not a
verdict grid: each row has a *Layer* (Web / Backend) and no Web / Android / Desktop / iOS
columns, so none of these was tried on Android. Several rows are shared with Script
Distribution, which runs on the same backend (`zillit_script_ops`).

| Sheet row | Layer · severity · status | What it says | Reachable from the Android UI? | Case |
|---|---|---|---|---|
| BUG-04 | Backend · High · fix ready, not submitted | The archived Schedule, Pages and DOD lists are a server error for admins; One Line's works | Unknown — depends on whether History calls the archived lists | **SD-64** |
| BUG-05 | Web + Backend · High · Open | DOD routes check `schedule_distribution_tool` rights instead of DOD's own | Not in this module — on Android, DOD is its own tool (`DodActivity`) | — (belongs to a DOD plan) |
| BUG-11 | Backend · Medium · Open | Lists and downloads are not view-rights-checked on the server | Only the client gate is visible from the UI | **SD-06** |
| BUG-12 | Backend · Medium · Open | Bad input is a server error; a failed replace can leave no current version | The replace half — Android sends `parent_schedule_id` on every Replace | **SD-16**, **SD-22** |
| BUG-21 | Backend · Medium · Open | A by-id GET with a bad id is a server error | No — the app only sends ids it was given | — |

---

## Wiring (when the suites are written) — both modules

**TestOrder.** The tools ranges are full: 48–69 end to end, and the 90–99 destructive block
is taken (`TOOLS_MANAGEMENT = 90`, Doc Distribution 91–96, Map 97–99). Sides squeezed into 119, between
Bhavik's admin constants, and the only numbers still free near it (120, 122–123, 125–126 …)
are the gaps Bhavik's block keeps "for new admin modules". Rather than take two more of
those, claim the next untouched range, as `TestOrder`'s own header says to:

```java
// 170–189 · tools overflow · VIDYA ---------------------------------------
public static final int SCHEDULE_DIST = 170;
public static final int CONTINUITY    = 175;
// free: 171–174, 176–189
```

Gaps of five leave room for a second suite per module (say, an opt-in publish or two-device
suite) without renumbering. Neither module mutates global state, so nothing needs them to run
late. The `170–189` line also belongs in the range table at the top of `TestOrder.java`.

| Place | Schedule Distribution | Continuity |
|---|---|---|
| Suite | `ScheduleDistTest` | `ContinuityTest` |
| Runner flag | `--schedule-dist` | `--continuity` |
| `MODULES` entry | `"Schedule Distribution::ScheduleDistTest"` | `"Continuity::ContinuityTest"` |
| Per-suite session | no | no |
| `make-report.mjs` | `n.includes('scheduledist')` → "Schedule Distribution", nav "Tools → Schedule Full & One Line" | `n.includes('continuity')` → "Continuity", nav "Tools → Continuity" |
| `.run` | `Zillit_Schedule_Distribution.run.xml`, `Zillit ▸ Schedule Distribution` | `Zillit_Continuity.run.xml`, `Zillit ▸ Continuity` |

One flag and one `.run` file per module. The opt-in pieces (SD-51 publish, CT-61's positive
Distribute) are cases inside the one suite that `assumeTrue` on an environment switch, not
flags of their own.
