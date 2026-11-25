# Casting (Main Cast) and Casting (Background Cast): test cases

Tools → **Casting (Main Cast)** (`casting_main_tool`) and **Casting (Background Cast)**
(`casting_background_tool`). Both tiles open the same activity, `CastingHomePage`. The only
differences are the tool identifier, the header and the unit-chat units
(`casting_main_unit_*` / `casting_background_unit_*`). They also keep separate data (separate `toolId`).
Source: `Zillit-Android-devbuild/app/src/main/java/com/zillit/zillitapp/bottomNav/tools/casting/`
(build Zillit_32539-dev). Project ZL-Android-Automation is a **TV project**, so the home
groups by episode by default and the form shows Episode no.

Automation: `src/test/java/com/zillit/automation/tests/casting/`
- `CastingTest` (`@Order(TestOrder.CASTING)` = 230): single device. It runs every case once for Main
  (`m01`–`m35`) and once for Background (`b01`–`b85`), and both share one implementation.
- `CastingTwoUserTest` (`@Order(TestOrder.CASTING_TWO_USER)` = 231): the emulator (Vidya Testing)
  acts and the Pixel (Vidya Pixel) watches.
- Page object: `src/test/java/com/zillit/automation/pages/casting/CastingPage.java`.

```
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5558 ./run-and-report.sh --only CastingTest
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5558 ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only CastingTwoUserTest
```

**Data.** Each tool's run creates one cast in Selects, named `ZL_AUTO_Char <M|B><n>` /
`ZL_AUTO_Talent <M|B><n>`. The app saves names with `capitalizeSentence()`, so it is stored as
"Zl_auto_char M12345" and every comparison ignores case. The cast has a unique episode, gender
Male, a character number and a description. Its attachment is a **link**
(`https://example.com/zl-auto-cast-…`). A link-only cast cannot reach Final (the server
refuses it), so CAST-31 also creates a **photo** cast (`ZL_AUTO_Pic …`) in Shortlist from the
app's own gallery. Before every run, `run-and-report.sh` pushes the `zl_test_*.jpg` fixtures
to `/sdcard/Pictures` on the emulator and touches them to the top, so the newest photo is
always a fixture. This runs on emulators only, never on the Pixel. Main also creates a
Non-Binary cast (`ZL_AUTO_Nb …`). The last case of each tool deletes everything
the run created, and the `@AfterAll` sweep deletes it again if a case died early. Unit-chat and
cast-chat test messages (`ZL_AUTO-cast-…`) are deleted after sending.

## Screens found (discovery, emulator-5558, 2026-11-23)

| Screen | What it holds |
|---|---|
| Tools | Both tiles are present on this project, under the "ADS" group: "Casting (Background Cast)" and "Casting (Main Cast)". |
| CastingHomePage | Header "CASTING (MAIN CAST)" / "CASTING (BACKGROUND CAST)". Tabs Selects · Shortlist · Final. Search "Search by Talent Name/Character". Unit chat icon (hidden on Final). Filter icon. Folder list. "+" and camera buttons on every tab, even Final. |
| Filter sheet | Group By: Talent Name / Character / Episode no. Episode no. is the default on a TV project; otherwise Character is the default and Episode is absent. Filter By (gender): Male / Female / Non Binary / All (All is the default). Buttons: Reset, Submit and close (X). |
| Folder rows | The title depends on the grouping: "Episode No : N", "Character: X" (detail line "Talent Name: …\nGender: …") or "Talent Name: X" (detail line "Character: …"). Each row has a ⋮ menu (`R.menu.folder_options_casting`, filtered per tab, below). |
| CastingCharactersFolderPage | Opens from an episode folder and lists that episode's character folders. |
| CastingSecondPage | Opens from a folder that holds more than one talent or character. |
| CastingListingPage | Cast cards: image or link icon, then "Character / Talent Name / Link / Gender / Uploaded By", then the description. Each card has a chat icon (not on Final) and a ⋮ menu (`R.menu.common_lcw_menu`). The toolbar has "Select" for multi-select, with Move To … / Cancel / Delete. "+" and camera show on every tab except Final. |
| CastingDetailPage | Add ("ADD CAST DETAILS"), Edit, Move (prefilled; hides Upload Media, Character Number, Availability and Contact Info) and View ("CAST DETAILS", read-only, empty fields read "Not Assigned", no Submit). Fields: Episode no. (TV only, digits and commas, with a hint about multiple episodes), Character, Talent Name, Gender (Male/Female/Non-Binary), Character Number (5 digits max, availability checked against the server), Description, Availability Dates (multi-date picker), Contact Info (+). |
| Upload Media sheet | "Choose below": Photo, Gallery, Video, Video Gallery, and Link (Link is missing on Final). Link opens the "Upload Link" dialog, whose field accepts paste only. |
| CastingContactInfoPage | Type (defaults to Actor; the dropdown also has Add new), Name, Company Name, Email, Code + Phone. |
| Unit chat (OtherChatsActivity) | Header "CASTING (MAIN CAST): SELECTS" / "…: SHORTLIST". Text, emoji, search and record. **No attachment button.** |
| Report dialog | Opened by Final → folder ⋮ → Generate Full PDF. First "Generation of Cast list." with Publish without details / Publish with details. Then "Report Generated Successfully.", offering View, Confidential Info and Close. Info, C&C members and Preset members are hidden. |

### Menus (from the source, removal lists in `CastingHomePage.onRecyclerItemClick` / `CastingListingPage.getPopoupItems`)

| Where | Offered |
|---|---|
| Selects folder, named | Move To Shortlist, Move To Final, Delete |
| Selects folder, unnamed ("Unknown") | Edit (hierarchy rename), Delete |
| Shortlist folder, named | Move To Final, Delete |
| Shortlist folder, unnamed | Delete |
| Final folder | Generate Full PDF, Delete |
| Episode folder (any tab) | Moves and Delete answer "To move/delete this folder, please navigate to the next page." |
| Selects card, own or admin | View Details, Edit, Forward, Download, Image Reply, Move To Shortlist, Move To Final, Read By, Delete |
| Selects card, someone else's (non-admin) | Same, minus Edit |
| Shortlist card, own or admin | View Details, Edit, Forward, Download, Image Reply, Move To Final, Read By, Delete |
| Final card | View Details, Forward, Download, Read By, Delete |

"Distribute" and "Generate PDF & Publish." are removed from every menu in this build, so they
cannot be reached.

## Cases: single device (`CastingTest`, both tools)

| ID | Steps | Expected | Source | Main (run 6) | Background (run 7) |
|---|---|---|---|---|---|
| CAST-01 | Tools → search "Casting" → tap the tile | The tile is listed. The home header is the tool name in capitals. | Tools.kt `CASTING_*_TOOL` | pass | pass |
| CAST-02 | Open the tool | Tabs Selects/Shortlist/Final, in that order. Selects is selected. Search hint "Search by Talent Name/Character". Filter icon, "+" and camera buttons are present (admin). | `handleTabLayout`, `handleSearch` | pass | pass |
| CAST-03 | Visit each tab | The unit chat icon shows on Selects and Shortlist and is hidden on Final. "+" stays on Final. | `handleUnitInfo` | pass | pass |
| CAST-04 | Open the unit chat on Selects and on Shortlist. Send a ZL_AUTO text on Selects, then delete it. | Header is "<TOOL>: SELECTS" / ": SHORTLIST". No attachment button. The message reaches the server. | `chatIv` click | pass | pass |
| CAST-05 | Open the filter | Group By lists Talent Name/Character/Episode no., with Episode no. ticked (TV). Filter By lists Male/Female/Non Binary/All, with All ticked. | `CastingVM.initializeFilter` | pass | pass |
| CAST-06 | Tap search with the field empty | Snackbar "Please Enter something for search" | `handleSearch` | pass | pass |
| CAST-07 | Tap "+" | Header "ADD CAST DETAILS". Upload Media, every field, the episode hint, Availability, Contact Info and Submit are present. | `CastingDetailPage.initM` | pass | pass |
| CAST-08 | "+" → enter character and talent → Submit | Snackbar "Cast Attachment Required."; the form stays open | `validate()` | pass | pass |
| CAST-09 | Open Upload Media from "+" on Selects, then on Final | Selects: Photo, Gallery, Video, Video Gallery, Link. Final: no Link. | `handleUploadMediaBtn` | pass | pass |
| CAST-10 | Upload Link → Submit empty → paste "zl auto not a url" → Submit | The dialog stays both times ("Please Enter URL" / "Please Enter Valid URL"). Nothing is attached. | `openPopupForUrl` | pass | pass |
| CAST-11 | Gender dropdown; Character Number typed with no character; "12345678" typed | Options are Male/Female/Non-Binary. A number with no character is cleared (toast "Please Enter Character Name"). The field keeps at most 5 digits. | `initM`, `hideShowEpisodeField` | pass | pass |
| CAST-12 | Contact Info + → Submit as each field is added → enter an invalid email → enter `TestEmails.self()` → delete the row | Type defaults to "Actor". The contact is refused without a name, a company, or an email/phone, and with an invalid email. A valid contact returns to the form and is listed. Delete asks "Are you sure you want to delete?" and Yes removes the row. | `CastingContactInfoPage.validate` | pass | pass |
| CAST-13 | Shortlist "+" → add a link → Submit as each field is filled | Refused while episode, then character, then talent, then gender is empty. Nothing is created. | `validate()` (non-Selects rules) | pass | pass |
| CAST-14 | Selects "+" → link → episode "E,E" → Submit → episode "E" → Submit | The duplicate is refused ("Duplicate episode numbers found…"). The cast saves, and "Episode No : E" is listed. | `validate()`, `hitApi()` | pass | pass |
| CAST-15 | Open "Episode No : E" → open the character folder | The characters page lists "Character: X" with the talent. The listing shows the card. | `openCharacterFolder`, `openThirdScreen` | pass | pass |
| CAST-16 | Group By Character, then Talent Name | Grouping by Character gives "Character: X" with the detail "Talent Name: Y". Grouping by Talent Name gives "Talent Name: Y" with the detail "Character: X". | `doFolderGrouping` | pass | pass |
| CAST-17 | Search the talent in capitals, then the character in lower case, then a term that matches nothing, then clear | Matching ignores case and every result matches the term. The unmatched term gives an empty list. Clearing restores the list. | `performFolderSearch` | pass | pass |
| CAST-18 | Character + Male, then Female, then All | A Male cast shows under Male, is hidden under Female and shows again under All. | `doFolderGrouping` gender filter | pass | pass |
| CAST-19 | Group by Talent + Female → Reset | Gender returns to All and Group By returns to the default (Episode no.). | `resetFilter` | **FAIL: CAST-BUG-02** | **FAIL: CAST-BUG-02** |
| CAST-20 | Open the cast's listing | The card shows character, talent, link, "Uploaded By", the description, the chat icon and the menu. | `LibraryItemHolder.bindCasting` | pass | pass |
| CAST-21 | The same card | It shows "Gender: Male". | `setCastingName` | **FAIL: CAST-BUG-01** | **FAIL: CAST-BUG-01** |
| CAST-22 | Card ⋮ (own, Selects) | View Details, Edit, Forward, Download, Image Reply, Move To Shortlist, Move To Final, Read By, Delete. No "Chat". | `getPopoupItems` | pass | pass |
| CAST-23 | Card ⋮ → View Details | Header "CAST DETAILS". No Submit and no Upload Media. The saved values show, and the fields are read-only. | `intent VIEW` branch | pass | pass |
| CAST-24 | Card ⋮ → Edit → episode "E,1" → Submit → episode E + new description → Submit | Two episodes are refused ("Only one episode … during update."). The edit saves and the card shows the new description. | `validate()`, `updateCasting` | pass | pass |
| CAST-25 | Listing → Select | The bar shows Move To Shortlist / Move To Final / Cancel / Delete and "+" is hidden. Delete with nothing selected → "Please keep at least one item from list.". Cancel closes the bar. | `enableSelection`, `btnDelete` | pass | pass |
| CAST-26 | Card chat icon → send a text, then delete it | The cast chat opens and the message reaches the server. | `CHAT_USER` → OtherChatsActivity | pass | pass |
| CAST-27 | Selects folder ⋮ (grouped by Character) | Exactly Move To Shortlist, Move To Final, Delete | home `MORE` | pass | pass |
| CAST-28 | Folder ⋮ → Move To Shortlist → Submit the move form | The move form is prefilled (character, talent, episode, gender) and hides Upload Media, Character Number and Availability. The cast is listed in Shortlist. (Whether Selects keeps a copy is logged, not asserted.) | `moveToDetailPage`, `moveToOther` | pass | pass |
| CAST-29 | Shortlist folder ⋮, card ⋮ | The folder menu has exactly Move To Final and Delete. The card has Move To Final but not Move To Shortlist, and keeps its chat icon. | | pass | pass |
| CAST-30 | Shortlist card ⋮ → Move To Final (the cast has only a link) | Snackbar "Cast Attachment Required." (media is needed for Final from a card) | `moveToDetailsPage` | pass | pass |
| CAST-31 | (a) Shortlist folder ⋮ → Move To Final → Submit on the **link-only** cast. (b) Shortlist "+" → Gallery → the newest test photo → episode/character/talent/gender → Submit (`ZL_AUTO_Pic …`) → folder ⋮ → Move To Final → Submit | (a) The API refuses: snackbar "You cannot move a 'Link' from 'Selects' or 'Short List' into the 'Final Folder'. Please select a media…". (b) The photo cast is listed in Final. A copy stays in Shortlist. | `moveToOther` (server rule) | pass | pass |
| CAST-32 | Final folder ⋮, card (photo cast) | The folder menu has exactly Generate Full PDF and Delete. The card has no chat icon, and the card menu is exactly View Details, Forward, Download, Read By, Delete. Whether the listing shows "+" is only logged: `handleBtnActions` hides it on Final, `handleUploadingIcon` shows it again, and it is shown on the device. | | fail (test-side: asserted no "+" on the Final listing; assertion removed, see note) | pass |
| CAST-33 | Final folder ⋮ → Generate Full PDF → Publish with details | The question "Generation of Cast list." appears. Then "Report Generated Successfully." with View, Confidential Info and Close. The test closes it and publishes nothing. | `generatePdf`, `shareToInfo` | pass | pass |
| CAST-34 (Main only) | Create a Non-Binary cast → Character + "Non Binary" filter | The cast is listed. | `hitApi` gender, `doFolderGrouping` | **FAIL: CAST-BUG-03** | n/a |
| CAST-35 | Final card ⋮ → Delete → Yes (photo cast), then folder ⋮ → Delete in every tab that still lists either cast | Confirms with "Are you sure you want to delete?". The card and the folders disappear. | `optionHandler delete`, `deleteFolder` | pass | pass |
| CAST-36 | Search Main's Final for the Background cast (and the reverse) | Neither tool lists the other's cast. | separate `toolId` | (run in Background block) | pass |

## Cases: two devices (`CastingTwoUserTest`)

| ID | Steps | Expected | Result |
|---|---|---|---|
| CAST-2U-01 | A creates a Main cast in Selects | B's Main → Selects lists it (refetching) within 60 s | not run yet |
| CAST-2U-02 | B waits on the Main Selects home while A sends a unit-chat text | The Selects chat icon on B's home shows an unread badge, and the message appears in B's Selects unit chat. | not run yet |
| CAST-2U-03 | A moves the cast to Shortlist | B's Shortlist lists it | not run yet |
| CAST-2U-04 | A deletes the cast from every tab | B no longer lists it | not run yet |
| CAST-2U-05 | A creates a Background cast | B's Background Cast lists it and B's Main Cast does not | not run yet |

## Bugs the suite reproduces (each fails with this id in its message)

| ID | Case | Bug |
|---|---|---|
| CAST-BUG-01 | CAST-21 | The cast card never shows the gender. `LibraryItemHolder.setCastingName` does `libraryInfo?.gender?.let { gender = … }?.run { gender = "" }`. The `let` returns `Unit` (non-null), so the `run` always clears it. Job frequency has the same pattern. |
| CAST-BUG-02 | CAST-19 | On a TV project, Reset in the filter sheet changes the grouping to Character instead of the default Episode no. `CastingVM.resetFilter()` calls `initializeFilter(context)` without `isShowEpisodeFilter = true`, so the Episode option disappears from the sheet after Reset. |
| CAST-BUG-03 | CAST-34 | A Non-Binary cast is never listed under the "Non Binary" filter. The form saves Non-Binary as `"others"` (`Constants.OTHERS`), but the filter compares the value against `R.string.other` ("Other"). |

## Not automated, and why

| Behaviour | Why |
|---|---|
| "Request to admin" dialog ("You do not have Posting Rights on this Tool…") on "+", camera, folder menu or card menu for a user without posting rights | Both test users are project admins, and `hasPostingRights()` returns true for any admin. Testing it would mean removing a real user's admin role or rights, with no safe restore. |
| Card ⋮ for another member's cast (no Edit), "You do not have rights to delete other user's 'Casting'", "Only Admin can delete." | Needs a non-admin second user. Both accounts are admins. |
| The tool closing when view access is revoked (`postingRightsListFlow` → `finish()`) | Changes a real user's rights. |
| Camera / multi-capture ("camera" button, Photo, Video) | Needs the device camera. The emulator camera produces nothing useful, and the Pixel's camera and gallery are personal. |
| Final-tab "+" create (Final offers no Link) and the Final-tab required-field rules | These need a photo plus the Final form. Only the Shortlist photo create is automated (CAST-31). The Final-tab form would reuse the same gallery step but is not wired up. |
| Video / Video Gallery | No video fixture is pushed to the devices. |
| Generate PDF → View (opens the PDF in an external viewer), → Confidential Info (posts the report into Confidential Info chat) | View leaves the app for a third-party viewer. Confidential Info publishes into another tool's shared feed. The test checks that the buttons are offered and taps Close. |
| Forward (card) → C&C users, Image Reply, Download, Read By | Forward sends a C&C message to a real member. Image Reply and Download need media (link-only casts have none: Download does nothing, and Image Reply opens an empty reply). Read By is shared chat infrastructure already covered by the chat suites. |
| Availability Dates picker | `CustomDatePicker` is a custom calendar whose day cells carry no ids. It is not stable enough to drive yet. |
| Character-number "already assigned to this character" dialog (Change Character / Use Number) | The outcome depends on server state for that character. The suite handles the dialog when it appears (`clearAvailabilityDialog`) but does not assert it. |
| Folder ⋮ → Edit (hierarchy rename) | Only offered on an unnamed ("Unknown") Selects folder. Creating one makes an unnamed cast that other casts also fall into. |
| Distribute, Generate PDF & Publish. | Removed from every menu in this build (unreachable). |
| Offline create / retry (`btnTryAgain`) | Needs a network fault on a shared device. |
| Notification deep links (`moveToMainCastingPage` / `moveToBackgroundCastingPage`) | Needs a push notification to be delivered and tapped. |

## Observations (logged, not asserted)

- A move leaves a copy behind. After "Move To Shortlist", Selects still lists the cast, and after
  "Move To Final", Shortlist still lists it. The Location tool behaves the same way. Ask product whether this is intended.
- The Final listing shows "+" (see CAST-32).
- The app on emulator-5558 once hung on the splash screen for over 2 minutes at the start of a run
  (run 1, CAST-01 and CAST-02 failed with "No known landing screen"). That was environmental and did not recur.

## Run history (2026-11-24, emulator-5558, build Zillit_32539-dev)

| Run | Scope | Result |
|---|---|---|
| 6 | `CastingTest.m*` (Main) | 31 pass, 4 fail: CAST-19, CAST-21 and CAST-34 are the bugs; CAST-32 was a test-side assertion, since removed |
| 7 | `CastingTest.b*` (Background) | 33 pass, 2 fail: the bugs CAST-19 and CAST-21 |
| — | `CastingTwoUserTest` | Not run. The Pixel was first held by other agents and then blocked by the app-lock prompt (05:20). The suite is written and compiles. |
