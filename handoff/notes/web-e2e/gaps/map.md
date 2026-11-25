# Map: web E2E cases vs Android Appium suites (gap report)

Scope: every one of the 220 web test cases in `web-e2e/map.md` (report generated 2026-11-17, dev.zillit.com) was compared with the 14 Android Map suites in `Android-automation/src/test/java/com/zillit/automation/tests/map/` (plus page objects in `pages/map/` and the sheets `MAP_MODULE_TESTCASES.md` / `MAP_LOCATION_TESTCASES.md`). Before a case was called MISSING or WEB-ONLY, the feature was checked in the Android app source `Zillit-Android/app/src/main`. Source paths below are shortened: `new_map/...` means `java/com/zillit/zillitapp/bottomNav/tools/new_map/...`, and `res/layout/...` is under `app/src/main/res/layout/`. Web references are `map.md:<line>`. The 16 Location-tool overlay cases (MapLocationPinsTest L-series, LocationToolTest LT-series) skip on purpose, because the Location tool is not enabled on the test project. Rows that rely on them read "COVERED (skips: Location tool off)". Date: 2026-11-23.

## 1. Summary

| Class | Web tests | Groups |
|---|---|---|
| COVERED | 70 | 20 |
| MISSING | 108 | 15 |
| CONFLICT | 7 | 5 |
| WEB-ONLY | 35 | 12 |
| **Total** | **220** | |

### Top missing cases (priority order)

1. **M1 Deleting a location type must keep its locations (untyped).** The web run shows the backend deletes the locations along with the type, which is data loss. Android sends the same delete call. T-08/T-09 only check the dialog and the type row.
2. **M7 Studio zone form guards.** Two likely Android defects show up in the source. Create Zone has no saving guard, so a double tap can POST twice. A blank Custom radius silently saves as 30 miles. Z-05 never clears the pre-filled name, so it does not test what its title says.
3. **M10 Filter chip counts.** This is a likely Android defect. The filter dialog counts each chip with an exact type match on `viewModel.locations`, so the "Unknown" chip, and the Final/Shortlisted overlay chips, always show 0 (`new_map/ui/map/MapFragment.kt:2186`). Also: switching city should clear the filter, and the header count should equal the List View count.
4. **M3 Two users in real time.** In particular, when B deletes the city A has open, Android probably keeps the deleted city selected, the same as the web bug. A stale edit form overwrites B's change, because the save is a full PUT with no version check.
5. **M2 Failed and offline reads and writes.** Airplane mode can be toggled with adb, so the offline List View read and the offline save need no stub. The map silently ignores failed zone and type reads (`MapViewModel.kt:363-365, 445`).
6. **M8 Zone edit keeps address and description.** The Android zone update omits both fields (`new_map/repository/LocationRepository.kt:94-104`), so it has the same risk as the web bug. Also: when the active zone is deleted, the map falls back to the first zone.
7. **M4 View-only account.** This is R-01…R-07 in the Android sheet. Android hides the write controls instead of offering a rights request.

---

## 2. MISSING (108 web tests, 15 groups)

Each group lists the web cases it covers, why it matters, and a proposed Android case with real ids and strings.

### M1. Deleting a location type keeps its locations (1)
- Web: "deleting a location type keeps the locations that used it, untyped" (map.md:1357). This is a known web bug: the location is deleted together with its type.
- Why: this is silent data loss. The Android confirm dialog promises the opposite: "Those locations will appear without a type label" (`new_map/ui/headers/HeaderManagerFragment.kt:395-404`). Android calls the same `deleteHeader` endpoint (`new_map/ui/headers/HeaderManagerViewModel.kt:76-85`), so the backend bug applies to Android too.
- Proposed: **T-12 Deleting a type leaves its locations in place, untyped** (MapCrudLifecycleTest, after LC-00/LC-01).
  - Steps:
    1. Create type `ZL AUTO TypeDoomed` with LOC Types `btnTypes`, then `+ Add Type` `btnAdd`, then Create.
    2. Tag one fixture location with it: List View `btnViewAll`, row `btnEdit`, pick it in `actvType`, then `btnSave` ("Update Location").
    3. Delete the type: `btnTypes`, row `btnDelete`, dialog "Delete Location Type", Delete.
    4. Reopen List View.
  - Expected: the location is still listed and its `tvTypeBadge` is hidden. Under the "Unknown" chip in `chipGroupFilter`, the location is counted.

### M2. Failed, slow and offline reads and writes (26)
- Read failures (20):
  - refused city list (map.md:186)
  - HTTP 500 city list (map.md:192)
  - refused location list (map.md:199)
  - refused zone list (map.md:205)
  - refused type list (map.md:211)
  - slow-read loaders (map.md:223)
  - cities, locations, zones and types aborted or HTTP 500 (map.md:930, 936, 942, 949, 955, 962, 968)
  - failing Location-tool pin read (map.md:975)
  - List View error, then recover (map.md:981)
  - Zones drawer error, then recover (map.md:986)
  - type read recovers only after reopen (map.md:991)
  - offline List View (map.md:997)
  - slow cities shows "Add City" (map.md:1002)
  - slow types spinner (map.md:1008)
- Write failures (6):
  - refused zone save keeps the form (map.md:217)
  - refused city delete keeps the selection (map.md:792)
  - refused location save keeps the values (map.md:1206)
  - HTTP 500 save reported (map.md:1212)
  - network-failed save (map.md:1219)
  - refused deletes leave items (map.md:1225)
- Why: none of these paths is tested on Android, and the Android behaviour differs by surface:
  - The map toasts failed city and location reads ("Failed to fetch cities" / "Failed to fetch locations", `new_map/ui/map/MapViewModel.kt:306, 409`). It silently ignores failed zone and type reads (`MapViewModel.kt:445, 363-365`).
  - The Location-tool pin read toasts once ("Couldn't load shoot locations", `MapViewModel.kt:494-516`). The web read is silent. This is not testable while the tool is off.
  - The location form keeps itself open on error and only pops on Success (`new_map/ui/locations/LocationFormFragment.kt:440-450`). The server message comes through, with a fallback "Failed to save location. Please check your inputs and try again." (`LocationFormViewModel.kt:313-332`).
  - A refused city delete only clears the selection on success (`MapViewModel.kt:820-838`).
- Proposed (offline cases need no backend stub; use `adb shell cmd connectivity airplane-mode enable|disable` or `svc wifi/data disable`):
  - **E-01 Offline List View read reports an error and recovers online.**
    - Steps: with a city selected, go offline, then tap `btnViewAll`.
    - Expected: a toast (server or "Failed to load locations", `new_map/ui/locations/LocationListViewModel.kt:90`). `emptyState` / `tvEmptyTitle` "No Locations" shows, with no crash.
    - Then go back online, press `btnBackToMap`, and tap `btnViewAll` again. Expected: rows return.
  - **E-02 Offline save keeps the form and its values.**
    - Steps: edit a fixture location (row `btnEdit`), change `etDescription`, go offline, then tap `btnSave`.
    - Expected: an error toast, `tvFormTitle` still shown, and the typed `etDescription` text kept. Online again, `btnSave` gives "Location updated successfully".
  - **E-03 Offline zone save keeps the form.**
    - Steps: Studio Zone `btnZoneList`, then `+ Add Zone` `btnAdd`. This needs a centre point, so it is blocked by Places today. If the centre can be set, go offline and tap `btnSave` ("Create Zone").
    - Expected: "Failed to create zone" or the server text (`new_map/ui/studio/StudioZoneViewModel.kt:56`), and the form stays open.
  - **E-04 Offline city delete keeps the city selected.**
    - Steps: city panel `btnCities`, row `btnCityDelete`, "Delete City", Delete, while offline.
    - Expected: an error toast, `tvCityName` unchanged, and the row still in `rvCities`.
  - **E-05 (stubbed, later) Refused zone and type reads.** This needs a proxy such as mitmproxy. Expected per source: the map shows nothing for zones and types. Record this as a parity finding against the web toast.
  - Slow-read loader checks can use the network-speed shaping of the emulator (`adb emu network speed gsm`). They are low priority.

### M3. Two users in real time (15)
- Web:
  - city added by B appears for A (map.md:800)
  - pin added by B updates A's count with no badge (map.md:806)
  - zone added by B is drawn for A (map.md:812)
  - type added or deleted by B shows for A (map.md:818)
  - reorder by B (map.md:824 skipped, map.md:1349)
  - location added by B appears in A's List View (map.md:1239)
  - stale edit form (map.md:1245)
  - B deletes while A edits (map.md:1252)
  - B deletes A's drawn zone (map.md:1259)
  - same type name at once (map.md:1265)
  - B renames a type (map.md:1272)
  - same city at once (map.md:1278)
  - B deletes A's open city (map.md:1284)
  - unread badge in a city A is not viewing (map.md:1291)
- Why: Android has the socket listeners, so the feature exists:
  - `BaseSocketListener.cityUpdate` triggers `fetchCities`, `mapLocationUpdate` triggers `fetchLocations` + `fetchStudioZones`, and `mapHeaderUpdate` triggers `fetchHeaders` (`new_map/ui/map/MapFragment.kt:1591-1609`).
  - List and zone list collectors: `new_map/ui/locations/LocationListFragment.kt:140-145`, `new_map/ui/studio/StudioZoneListFragment.kt:153-156`, `new_map/ui/headers/HeaderManagerFragment.kt:111-114`.
  - Badges: `tvCitiesBadge`, the row `tvBadge`, and `MapViewModel.autoReadCurrentCityBadgeIfAny` (`MapViewModel.kt:269-276`).
  - Likely Android defect: after B deletes A's open city, `fetchCities` refreshes the list, but `applyAutoSelectionIfNeeded` returns early because a selection exists (`MapViewModel.kt:317-319`). The deleted city stays in `tvCityName`, the same as the web bug.
  - The location save is a full record replace (`new_map/ui/locations/LocationFormViewModel.kt:296-309`), so the stale-form overwrite applies to Android as well.
- Proposed: these cases need a second session. They can run with the web client as slot B (Playwright), or with a second Android device running the same suite.
  - **RT-01 A city B adds appears in A's panel without a reload.**
    - Expected: the `btnCities` label goes from "Cities (N)" to "Cities (N+1)" (`MapFragment.kt:1349`).
  - **RT-02 A pin B adds in A's open city updates `tvLocationCount`, with no `tvCitiesBadge` increase.**
  - **RT-03 A type B adds shows in the open LOC Types list (`rvHeaders`); B's delete removes it.**
  - **RT-04 B deletes A's open city: A moves off it.**
    - Expected: `tvCityName` reads "Select a City" or another city. Per the source this will probably fail, so raise it as a bug.
  - **RT-05 B pins in another city: A's `tvCitiesBadge` and that row's `tvBadge` increment, and opening the city clears them.** This is C-28/C-29 in the Android sheet.
  - **RT-06 B deletes the zone A has drawn: `btnZoneList` stops showing its name** (it falls back to the next zone or to "Studio Zone").
  - **RT-07 Stale edit.**
    - Steps: A opens the `btnEdit` form, then B edits the description. A then edits the name and taps `btnSave`.
    - Expected (web expectation): B's description survives. Per the source it will not, so record it as a backend issue.

### M4. Rights: view-only and demoted accounts (8)
- Web:
  - view right off: tile hidden but the route opens (map.md:173)
  - viewer asked to request rights (map.md:467)
  - viewer has no zone Edit/Delete (map.md:591)
  - demoted admin with Post can still write (map.md:1308)
  - no Post: every control gated (map.md:1314)
  - revoking Post while open (map.md:1320)
  - no View: tile hidden, deep link opens (map.md:1326)
  - restore rights (map.md:1333)
- Why: Android enforces rights but in a different way. It never offers a "request rights" prompt (no use of `you_don_t_have_posting_to_perform_this_action` or `RightsRequestGate` in the map code). Instead:
  - It hides `btnAddCity`, `btnPin`, `btnTypes` and `btnPlacePin` (`MapFragment.kt:970-989`).
  - It hides row Edit/Delete on carousel, list and zone cards (`new_map/ui/map/CarouselCardAdapter.kt:142-148`, `new_map/ui/studio/StudioZoneAdapter.kt:85`).
  - It toasts "You don't have permission to add cities" or "...to pin a location" (`MapFragment.kt:1194, 1294`).
  - It re-checks at runtime on `postingRightsListFlow` (`MapFragment.kt:1629-1633`).
  - It closes the screen when View is off (`MapFragment.kt:933-939`). The Android analog of the web deep link is the Stripboard "Show in Map tool" entry (`NewMapPage.EXTRA_LOCATION_ID`, `MapFragment.kt:2926-2930`).
  - The Android sheet already specifies R-01…R-07 and T-03, but none of them is automated.
- Proposed: this needs a second account whose rights can be switched in User Management (the web suite's slot B).
  - **R-01 A view-only user sees no `btnAddCity`, `btnPin`, `btnTypes` or `btnPlacePin`.** Carousel and list rows show `btnView` only. Zone rows show `btnSelect` "Show on Map" and `btnView` only. The type manager hides `btnAdd` (T-03).
  - **R-02 A view-only user still reads.** Directions (`btnDirections`), Share (`btnShare`), View and the city panel all work.
  - **R-06 A view-only user picks a Places result in `etCitySearch`.** Expected toast: "You don't have permission to add cities". The field clears.
  - **R-08 Post is revoked while the map is open.** Expected: `btnPin` hides without a restart (`postingRightsListFlow`).
  - **R-09 View is revoked.** Expected: the Map tile is gone from Tools. Opening the map from Stripboard "Show in Map tool" closes it immediately.
  - **R-10 Teardown.** Restore the account's admin flag and Map Post.

### M5. Saved-input hygiene (13)
- Web:
  - whitespace-only name/address/type refused (map.md:828)
  - regex and unicode queries (map.md:883)
  - HTML/script/RTL/long names as text (map.md:895)
  - trimmed name (map.md:1139)
  - unicode/emoji/RTL name (map.md:1145)
  - HTML/script name and description (map.md:1151)
  - 1,000-character name (map.md:1157)
  - multi-line and whitespace description (map.md:1163)
  - scene trimmed (map.md:1169, web bug)
  - duplicate location names (map.md:1176)
  - type name trimmed, case variant (map.md:1182)
  - HTML/unicode type name (map.md:1188)
  - duplicate zone names (map.md:1200)
- Why: Android trims name, address, description, scene and type before saving (`LocationFormFragment.kt:258-262`, `HeaderManagerFragment.kt:361`, `StudioZoneFormFragment.kt:557`). So the web scene-trim bug should not apply to Android, but nothing proves it. A very long name in the carousel could overflow (`res/layout/item_carousel_card.xml` `tvName`). Search uses plain `contains` on lowercased text (`MapViewModel.kt:647-659`), so regex characters are safe, but unicode case folding is unverified.
- Proposed (MapCrudLifecycleTest, on LC fixtures, through Edit, because create needs Places):
  - **H-01 Padded name and scene.**
    - Steps: in edit mode, set `etName` to `"   ZL AUTO Trim   "` and `etScene` to `"  12B  "`, then tap `btnSave`.
    - Expected: the List View `tvName` reads `ZL AUTO Trim` and `tvScene` reads "Scene #12B" (`new_map/ui/locations/LocationCardAdapter.kt:98`).
  - **H-02 Spaces-only name refused.**
    - Expected: the toast "Please enter a location name" and the form stays open.
  - **H-03 Unicode/emoji/RTL name saves, and search in lower case finds it** (`etSearch`, `rvSearchResults`).
  - **H-04 HTML-looking name and description show as literal text.** Check `tvName` / `tvDescription` text equality.
  - **H-05 A 1,000-character name.** Expected: the save outcome is recorded, and the carousel card stays within the screen width (the bounds of `tvName`).
  - **H-06 Multi-line description keeps its lines in `LocationDetailBottomSheet`. A spaces-only description saves empty.**
  - **H-07 Type name `"  ZL AUTO Pad  "` is listed trimmed in `rvHeaders`.**
  - **H-08 Search with `.* ( [ \ % $^` never crashes.** Expected: `rvSearchResults` is hidden or shows rows.

### M6. Unusual coordinates (2)
- Web: poles, antimeridian and missing coordinates (map.md:901); prime-meridian pin (map.md:907, web bug).
- Why: Android skips only an exact (0,0) pair (`MapFragment.kt:2078, 2394`), so a pin at longitude 0 should draw. The fixtures have to be seeded through the API or web, because Android cannot place a pin there without Places.
- Proposed: **G-01** seed "ZL AUTO greenwich" (51.4779, 0.0) and "ZL AUTO pole" (90, 180) through the API.
  - Expected: both are counted in `tvLocationCount`. Fit All (`btnFitAll`) works. The carousel `tvCoords` reads "51.477900, 0.000000".

### M7. Studio zone form validation and save guard (7)
- Web:
  - Custom radius must be filled and positive (map.md:543)
  - emptied Zone Name blocks the save (map.md:549)
  - one-mile Custom zone (map.md:555)
  - Street Intersection guard (map.md:838)
  - radius -5/0.5/201 never submit (map.md:843)
  - radius 1/200/1e2 posted (map.md:848)
  - Create Zone "Saving..." with a double click giving one POST (map.md:865)
- Why: these are likely Android defects.
  - `saveZone()` turns a blank Custom radius into 30: `etCustomRadius.text.toIntOrNull() ?: 30` (`new_map/ui/studio/StudioZoneFormFragment.kt:571-574`). So "Custom" with an empty field saves a 30-mile zone without a word.
  - There is no upper bound; only `<= 0` is refused (:576).
  - `etCustomRadius` is `inputType="number"` (`res/layout/fragment_studio_zone_form.xml:336`), so negatives and decimals cannot be typed. That part is web-only.
  - `btnSave` is never disabled while saving (`StudioZoneFormFragment.kt:299, 556-599`, unlike `LocationFormFragment.kt:435-437`), so a double tap can create two zones.
  - The existing **Z-05 does not test an empty name**: `openZoneForm()` (`MapZoneCrudTest.java:51-56`) leaves the pre-filled "<city> Zone" (`StudioZoneFormFragment.kt:131-132`) in place. The form stays open only because no centre point is set, which is Z-06's guard.
  - Intersection: a blank street gives the toast "Please enter both street names" (:280-284). The save in intersection mode gives "Please enter both street names for the intersection" (:563-565). Z-07 is specified but not automated.
- Proposed (MapZoneCrudTest):
  - **Z-05 (fix): clear the name first.**
    - Steps: `btnAdd`, clear `etZoneName`, then `btnSave`.
    - Expected: the toast "Please enter a zone name" and `tvFormTitle` "New Studio Zone" still shown.
  - **Z-08b Custom with an empty radius is refused.**
    - Steps: in `chipGroupRadius`, tap the "Custom" chip, leave `etCustomRadius` empty, set a centre if possible, then tap `btnSave`.
    - Expected (web parity): a refusal. Per the source, Android saves 30 mi, so this will fail. Raise it as a bug.
  - **Z-08c Custom 1 mile saves, and the card `tvRadius` shows 1.** Custom 201 mi: record whether it is accepted (Android has no cap).
  - **Z-07 Intersection mode with a blank street.**
    - Steps: `btnIntersection`, then `btnFindIntersection` with `etStreet1` empty.
    - Expected: the toast "Please enter both street names". Then `btnSave` gives "Please enter both street names for the intersection".
  - **Z-11c A double tap on Create Zone creates one zone.**
    - Steps: tap `btnSave` twice quickly.
    - Expected: `tvCount` "N zones" grows by exactly 1. This is a likely failure today.
  - Note: everything that saves needs a centre point, which needs Places (finding 5 in the Android sheet). Z-05, Z-07 and the blank-custom check fire before or regardless of the centre: blank name at :557, missing centre at :561. Order the steps so the guard under test fires first.

### M8. Zone selection, fallback, details and edit payload (6)
- Web:
  - a city with zones always draws one, named in the toolbar (map.md:537)
  - Details shows the zone, radius and preview (map.md:579)
  - deleting the drawn zone moves the map off it (map.md:779)
  - zone edit keeps the stored address and description (map.md:785, web bug)
  - stubbed zone drawn, listed and detailed (map.md:1026)
  - remembered zone gone falls back to the first zone (map.md:1121)
- Why:
  - Android auto-activates the first zone when a city opens (`MapViewModel.kt:441-443`) and names it on `btnZoneList` (`MapFragment.kt:1494, 1892`).
  - When the active zone is deleted, `syncActiveZoneWithFreshList` (`MapViewModel.kt:453-470`) clears it, then the first remaining zone is activated. This is Z-18, not automated.
  - Details is a plain dialog, "Studio Zone Details" with OK (`new_map/ui/studio/StudioZoneListFragment.kt:198-200`), with no preview. This is Z-15, not automated.
  - The zone update request sends only name, coordinates and miles (`new_map/repository/LocationRepository.kt:94-104`). `locationAddress` and `description` default to "" in `UpdateLocationRequest` (`new_map/data/remote/ApiService.kt:119-134`), and the project Json uses `encodeDefaults = false` (comment at ApiService.kt:71). So an Android zone edit either omits or blanks those fields, the same risk as the web bug.
- Proposed:
  - **Z-13b Opening a city with zones shows a zone name on `btnZoneList`, not "Studio Zone".**
  - **Z-15 Zone `btnView` opens "Studio Zone Details" with the radius and coordinates. OK closes it.**
  - **Z-18 Deleting the active zone moves the highlight to the next zone.** Expected: `btnZoneList` shows the next zone's name, or "Studio Zone" when none is left.
  - **Z-16b Zone edit keeps address and description.**
    - Steps: seed a zone with an address and description from web or the API. On Android, edit the radius chip "40 mi", then `btnSave` ("Update Zone").
    - Expected: the API (or a web read) still shows the address and description.

### M9. Cities: count, reorder, delete-open, duplicate, fallback (6)
- Web:
  - floating Cities button counts cities; chip names the open city (map.md:56)
  - reorder (map.md:325)
  - refused reorder reverts (map.md:331)
  - deleting the open city opens another (map.md:343)
  - Add City refuses an existing name (map.md:661)
  - remembered city gone falls back to the first (map.md:1115)
- Why:
  - `btnCities` reads "Cities (N)" or "Add City" (`MapFragment.kt:1349`). Only the "Add City" state is asserted.
  - Drag-to-reorder uses an `ItemTouchHelper` on `rvCities` (`MapFragment.kt:1014-1041`). A failure reverts silently with no toast (`MapViewModel.kt:350-357`), which differs from the web toast. This is C-25, not automated.
  - Deleting the open city clears the selection and then auto-selects the first city (`MapViewModel.kt:820-833` and `317-330`).
  - There is no client-side duplicate-name check (`new_map/ui/cities/CityFormFragment.kt:265-270`), so only the server can refuse.
- Proposed:
  - **C-16b `btnCities` reads "Cities (N)" with N equal to the row count of `rvCities`.**
  - **C-25 Long-press-drag the first `rvCities` row below the second. The order persists after leaving and re-entering. Drag it back.**
  - **C-21b Delete the currently open fixture city.** Expected: `tvCityName` shows another city, or "Select a City" with `btnCities` "Add City".
  - **C-24c Fallback.** Delete the last-visited city from web, then reopen the Map. Expected: the first city in the panel opens.
  - **C-31 Add a city that already exists** (current-city card `tvCurrentCityAction` "Add Current City", if shown). Expected: a server error toast and no new row. Blocked by C-27's precondition, because the card hides when the city exists (finding 3).

### M10. Filters, search limits and counts (7)
- Web:
  - toolbar count matches List View (map.md:113, web bug)
  - header count equals List View (map.md:395)
  - switching city clears the type filter (map.md:307)
  - type filter chip icon/count, "k / n", Unknown, Clear All (map.md:413)
  - type search matches subtypes (map.md:389)
  - more than eight matches (map.md:877)
  - filters, search and pin mode reset on reopen (map.md:1109)
- Why:
  - **Likely Android defect:** the filter dialog computes each chip's count with `viewModel.locations.value.count { (it.locationType ?: "") == type }` (`MapFragment.kt:2186`). For the synthetic "Unknown" chip, and for overlay "Final"/"Shortlisted" chips, the count is always 0. The view-model has a correct `getTypeCount` (`MapViewModel.kt:668-675`) that is not used here.
  - The Android header shows the filtered count as "k locations" (`MapFragment.kt:2029`), not "k / n". It deliberately excludes overlay pins (`MapViewModel.kt:636-645`), so the web count bug should not apply.
  - `selectCity` clears filters, search and pin mode (`MapViewModel.kt:369-378`).
  - Search shows at most 8 results with no "+N more" (`new_map/ui/map/SearchResultAdapter.kt:19-22`).
  - Type search matches subtype names (`HeaderManagerFragment.kt:71-76`). T-10 checks the name only.
- Proposed:
  - **F-16 Filter chip counts.**
    - Steps: with LC fixtures, one of them untyped (LC-01 creates the third untyped), tap `btnFilter`.
    - Expected: each chip's count equals the rows of that type in the List View. The "Unknown" chip count equals the number of untyped rows. This will fail today, so raise it as a bug.
    - Then pick a chip and tap Apply (`btnApply`, which reads "Apply (1)"). Expected: `btnFilter` reads "Filter (1)" and `tvLocationCount` reads "1 locations".
    - Then Reset (`btnReset`). Expected: back to "Filter".
  - **C-22c With "Filter (1)" active, switching city resets `btnFilter` to "Filter".**
  - **V-06 `tvLocationCount` N equals the List View `tvCount` "N total" (`LocationListFragment.kt:100`).**
  - **T-10b Typing a subtype name in `etTypeSearch` lists its parent type.**
  - **F-02c With 9 or more matching fixtures, `rvSearchResults` shows exactly 8 rows.**
  - **M-10 Pin mode and the filter are reset after leaving and re-entering the Map.**

### M11. Location form: media, subtypes, saving, address boundary (6)
- Web:
  - photos preview, remove, cap at 10 (map.md:108)
  - photo added on edit uploads (map.md:479)
  - removing the saved photo sends empty attachments (map.md:485)
  - type picker drives subtypes and scene (map.md:357)
  - Update Location "Saving..." with a double click giving one PUT (map.md:859)
  - address outside the city's zones asks for confirmation (map.md:673)
- Why:
  - `uploadArea` opens the picker with `maxSelectable = 10 - count`. An 11th file gets "Maximum 10 media allowed" (`LocationFormFragment.kt:196-201`). Photos show in `rvPhotos`. Removing one changes `attachments`, and the full list is always sent (`LocationFormViewModel.kt:283-285`).
  - Picking a type rebuilds `chipGroupSubtypes` (`LocationFormFragment.kt:348-359`).
  - On Android the Scene Number (`sceneLayout`) is always visible (`res/layout/fragment_location_form.xml:146`). It does not appear only for a "shoot" type as on web.
  - `btnSave` reads "Saving..." and is disabled while saving (`LocationFormFragment.kt:435-437`).
  - An address picked outside the boundary asks "Outside <city>" with "This address is outside <city>. Do you want to use it anyway?" and Yes/No (`LocationFormFragment.kt:87-104`). This is P-19 in the sheet.
  - The specified cases N-07 and N-09 are not automated.
- Proposed:
  - **N-09 Media.**
    - Steps: Edit a fixture, tap `uploadArea`, pick 2 gallery images (push test PNGs with `adb push` first), then `btnSave`.
    - Expected: `LocationDetailBottomSheet` shows "Photos" with 2.
    - Then edit again, remove one, and save. Expected: 1 photo.
    - Then add up to 10. A further tap on `uploadArea` gives the toast "Maximum 10 media allowed".
  - **N-07 Picking a type with subtypes shows them as checkboxes in `chipGroupSubtypes`. Picking another type replaces them.**
  - **N-11b Double tap on `btnSave` ("Update Location") gives "Saving..." and exactly one update.** Expected: one "Location updated successfully" toast and no duplicate rows.
  - **P-19 Picking an address outside the city in `etAddress`.** Expected: "Outside <city>". No keeps the old address; Yes applies it. This needs Places.

### M12. Markers, drag-move and map-card freshness (4)
- Web:
  - clicking a marker opens its card (map.md:611)
  - dragging a pin asks "Move Location"; Cancel keeps it, Move saves (map.md:623)
  - a refused move reports the error (map.md:744, web bug)
  - a List View edit shows on the map card at once (map.md:732)
- Why:
  - A marker tap opens the bottom panel and selects the card (`MapFragment.kt:1730-1755`).
  - Existing markers can be dragged by posters (`draggable(canPost && !isFromLocationTool)`, `MapFragment.kt:2344`).
  - The drag end asks "Move Location" with "Move '<name>' to the new position?" and Move/Cancel, or "Outside <city>" (`MapFragment.kt:589-627`).
  - A failure toasts the server message and snaps the marker back (`MapViewModel.kt:746-759`, `MapFragment.kt:262-273`). So the web bug should not apply.
  - LC-08 covers only the preview pin. P-14…P-17 and N-17 are not automated.
- Proposed:
  - **N-19 Tap a fixture marker** (zoom in with `btnFitAll`, then tap the marker at screen coordinates). Expected: `bottomLocationPanel` shows, with the carousel card `tvName` equal to the fixture.
  - **P-14/P-17 Long-press a fixture marker and drag it about 80 px.**
    - Expected: the "Move Location" dialog. Cancel puts the marker back and leaves `tvCoords` unchanged.
    - Repeat with Move. Expected: the toast "Location moved" and changed `tvCoords`.
  - **N-11c Edit the description from the List View, then `btnBackToMap`.** Expected: the carousel card (and the detail sheet) shows the new text. This also checks sheet bug MAP-01.

### M13. Geolocation, places pin, directions cleanup (3)
- Web:
  - without geolocation, directions ask for a pickup (map.md:147)
  - a Places result pins inside the city (map.md:637)
  - closing directions from a searched place removes its pin (map.md:691)
- Why:
  - Android toasts "Location permission required" and "Fetching current location, please try again" (`MapFragment.kt:2739-2742`).
  - `btnPlacePin` runs the boundary flow (`MapFragment.kt:1289-1320`).
  - `btnPlaceDirections` calls `hidePlacesResult()` before opening directions, which removes the place marker (`MapFragment.kt:1264-1270, 2678-2683`).
  - D-05, D-06 and F-06…F-08 are not automated.
- Proposed:
  - **D-06 Revoke location permission, open a card, tap `btnDirections`, then `btnUseMyLocation`.**
    - Run `adb shell pm revoke <pkg> android.permission.ACCESS_FINE_LOCATION` first.
    - Expected: the toast "Location permission required". `etDirectionsTo` is still filled.
  - **F-06 Pick a place with `fabSearch`, then `btnPlacePin`.** Expected: the form opens with coordinates. After the save, `placesResultCard` is gone. This needs Places.
  - **F-07 `btnPlaceDirections`, then `btnCloseDirections`.** Expected: `placesResultCard` is hidden and `directionsPanel` is hidden.

### M14. Detail and empty states (2)
- Web: a location with no address, description or photos shows only what it has (map.md:1032); deleting the last location of the selected List View type (map.md:1231).
- Why: `LocationDetailBottomSheet` adds the address, scene, description and photo sections only when present (`new_map/ui/locations/LocationDetailBottomSheet.kt:76-104`). The List View shows `emptyState` when the filtered list is empty (`LocationListFragment.kt:101`).
- Proposed:
  - **N-15b On an untyped, address-only fixture, the detail sheet has no Description or Photos section, and the type reads "Unknown"** (:36).
  - **V-07 Filter the List View to a type chip with one location and delete it.** Expected: `emptyState` "No Locations" while the chip stays selected.

### M15. Dark mode on every Map surface (2)
- Web: light theme (map.md:1074); dark theme (map.md:1082).
- Why: Android has no in-map theme toggle, but it follows the system theme. Recent fixes for unreadable dark-mode cards are visible in the source (ZL-21467 comments, `new_map/ui/map/CityAdapter.kt:71`, `CarouselCardAdapter.kt:122`).
- Proposed: **M-11 With the device in night mode** (`adb shell cmd uimode night yes`), open each of these and take a screenshot:
  - city panel `cityListContainer`
  - List View
  - detail sheet
  - zone list
  - LOC Types
  - filter dialog
  - Expected: text is not near-white on a white surface. Compare `tvName` color contrast, or attach the screenshots to the report.

---

## 3. CONFLICT (7 web tests, 5 groups)

| # | Web (map.md) | Web says | Android test says | Android source says | Verdict |
|---|---|---|---|---|---|
| C1 | :235 "a location opened from the search shows its card"; :407 "a search result opens the card with type, icon, subtype..." | Picking a search result closes search and opens that location's card with its actions | MapLocationCrudTest F-03 "Tapping a result closes the search and centres the map": asserts results gone, map back, no card | `SearchResultAdapter` callback only animates the camera, then `toggleSearch()` and clears the query; no card or bottom panel (`MapFragment.kt:1049-1055`). Compare the marker tap, which opens the panel (:1730-1755) | Android test matches Android source. The behaviour differs from web and from the Android sheet's own F-03. Raise it as an Android parity issue. If it gets fixed, F-03 must change. |
| C2 | :124 "a Location-tool pin has no Edit and View opens the Location tool"; :129 "a Location-tool pin is not listed in the List View drawer" | Overlay pins are not in List View; their card has no Edit; View opens the Location tool | MapLocationPinsTest L-15/L-16 and L-17 open `btnViewAll` and look for rows whose `tvTypeBadge` contains "Shoot Location" | `LocationListViewModel` loads only `getLocations(cityId)` (`LocationListViewModel.kt:84-92`), so overlay pins are never in View All, which agrees with web :129. Overlay pins appear only in the bottom panel (`rvCarousel`/`rvLocationList`), where Edit/Delete are hidden (`CarouselCardAdapter.kt:142-148`). View opens `LocationDetailBottomSheet` (`MapFragment.kt:1068-1070`), not the Location tool. | COVERED (skips: Location tool off) by design, but the assertions are wrong. Once the tool is enabled, L-15/L-16 will skip forever (`assumeTrue(overlayRows > 0)`), and L-17 passes vacuously. Retarget them to the bottom panel: `btnLocationPill`, then list mode `rvLocationList`. "View opens Location tool" is web-only. |
| C3 | :383 "editing a type asks first; Cancel keeps the form, Update saves the new subtype" | Edit opens the form. Pressing Update shows the "Update Location Type" confirm; Cancel returns to the form | MapTypeManagerTest T-06 asserts the confirm appears right after tapping Edit, with Update/Cancel | `showEditConfirm()` runs before the form; Update then calls `showFormDialog(header)` (`HeaderManagerFragment.kt:384-393`) | The Android test is right for Android. The order differs by platform. Also add T-07 (save an extra subtype after Update) to cover the "saves the new subtype" half. |
| C4 | :1194 "zone names: whitespace-only saves as '<city> Zone'..." | A spaces-only Zone Name falls back to "<city> Zone"; padded names are trimmed | MapZoneCrudTest Z-05 "Saving a zone with no name is rejected" | `zoneName.trim()`, then blank gives "Please enter a zone name" (`StudioZoneFormFragment.kt:557-558`). The default name is only pre-filled (:131-132) | Android source rejects, so the Z-05 intent is right for Android, and the platforms differ. Z-05 is ineffective as written (see M7), so fix it to clear the name first. |
| C5 | :103 "the location form validates name, address and a custom 'Other' type" | Messages "Please fill in required fields" and "Please enter an address or search for a location"; "Other" needs a custom type name | MapLocationCrudTest N-02/N-03, with message constants "Please enter a location name" and "Please enter an address" (`MapLocationFormPage.java:27-28`) | Same strings as the Android tests (`LocationFormFragment.kt:265-266`). There is no "Other"/custom-type rule: type is free text in `actvType` plus "+ New" `btnAddNewType` | Wording only. Android tests are right per source; no change. Note that N-02/N-03 only assert that the form stays open. |

### Test-quality notes found during mapping (not counted above)
- **Z-05 is ineffective.** See C4 and M7: the pre-filled name is never cleared.
- **C-18 "A city opens prefilled for editing" can never run.** The Map's city panel hides `btnCityEdit` ("Edit removed — cities can only be deleted", `new_map/ui/map/CityAdapter.kt:80-81`), so `editCity()` always falls to the `assumeTrue` skip. Either retire it or point it at `CityListFragment` if that screen is reachable.
- **L-19 needs a clearer message.** Its failure message says the overlay is "deliberately NOT city-scoped". That holds only for the toggle label count (`MapFragment.kt:2001-2005` counts all `locationPins`). The drawn overlay pins are filed per city (`MapViewModel.kt:537-567`), matching web :119. Add a companion assertion on drawn or filter counts when the tool is enabled.

---

## 4. COVERED (70 web tests, 20 groups)

| Web group (map.md lines) | Count | Android test (class: DisplayName) |
|---|---|---|
| Entry, Tools tile, back (:10, :15, :20) | 3 | MapModuleTest: M-01 Map opens with the MAP header...; M-05 Back from the Map returns to Tools |
| Auto-open city, last visited, switch city (:25, :301, :313) | 3 | MapModuleTest: C-22, C-24; MapGuardsTest: C-24b; MapCityCrudTest: C-22b; MapCityMultiTest: MC-03 |
| Toolbar buttons (:30) | 1 | MapModuleTest: R-03 With a city selected the city-gated toolbar buttons appear. Android has no Full View/theme/How To Use, and its header excludes overlay pins by design (`MapViewModel.kt:636-645`) |
| Guide collapse (:41) | 1 | MapModuleTest: M-03 Tutorial card...collapses; MapGuardsTest: M-04. Android has no dismiss X or "How To Use Map" button |
| Pin mode on/off (:61) | 1 | MapPinBoundaryTest: P-01, P-02/P-03. Android banner is static "Tap on the map to place a pin" (`fragment_map.xml:544`) |
| Cities panel, add form, search, pick existing (:68, :73, :289, :295, :655) | 5 | MapModuleTest: C-01; MapCityCrudTest: C-02, C-17, C-26; MapCityMultiTest: MC-02, MC-05. Android shows a Places dropdown instead of "No cities match" (`MapFragment.kt:1213-1217`) |
| In-screen search open and match (:78, :401) | 2 | MapModuleTest: F-01; MapLocationCrudTest: F-02, F-02b; MapCrudLifecycleTest: LC-03. Android shows no "No locations found" text |
| Type filter chips, hide-on-filter, Clear All, no types (:83, :419, :889, :1038) | 4 | MapLocationCrudTest: F-09b/F-11, F-12; MapModuleTest: F-09/F-13; MapCrudLifecycleTest: LC-12, LC-13; MapGuardsTest: F-10 |
| List View cards, tabs, empty, View/Back (:88, :167, :437, :443, :449) | 5 | MapModuleTest: V-01, V-05; MapLocationCrudTest: V-01b, V-02, V-03, V-04 |
| Studio zones list, create, apply, filter, edit, delete, checks (:93, :525, :531, :561, :567, :585, :597, :679, :773, :1020) | 10 | MapZoneCrudTest: Z-01, Z-02, Z-11, Z-12, Z-13, Z-16, Z-17, Z-19, Z-07b, Z-09, Z-11b; MapGuardsTest: Z-03. Saves skip because of Places. Android has no per-city zone memory |
| Location types (:98, :377, :461, :497, :833) | 5 | MapTypeManagerTest: T-01, T-02, T-04, T-05, T-09, T-10, T-11, T-11b; MapCrudLifecycleTest: LC-00, LC-00b; MapLocationCrudTest: N-04; MapCleanupTest: CL-03 |
| Overlay chips and access (:119, :134) | 2 | MapLocationPinsTest: L-11 (COVERED, skips: Location tool off); L-02 Without Location-tool access the toggle is absent (runs) |
| Empty project, Fit All empty (:155, :161) | 2 | MapModuleTest: M-02 With no city chosen the label reads 'Select a City'...; MapGuardsTest: F-15 |
| Share (:265, :1341) | 2 | MapDirectionsShareTest: S-01 Sharing a pin opens the system chooser; S-01b. Android uses the system share sheet, not email Compose (`MapFragment.kt:3006-3060`) |
| Cities create, current-city, second city, delete (:277, :283, :319, :337, :649, :667, :854) | 7 | MapCityCrudTest: C-27, C-19/C-20, C-21, C-05/C-16, C-04; MapCityMultiTest: MC-01 A second city is created from the out-of-city pin dialog. The dialog dismisses on its first tap, so a repeat delete is impossible (`MapFragment.kt:1001-1009`) |
| Locations create, edit, view, delete, count (:351, :363, :431, :455, :473, :491, :738) | 7 | MapCrudLifecycleTest: LC-01, LC-04, LC-05, LC-09, LC-10; MapLocationCrudTest: N-10, N-11, N-12/N-13, N-14, N-15, N-16 |
| Boundary dialogs (:505, :643) | 2 | MapPinBoundaryTest: P-04/P-05, P-06/P-09, P-07/P-10, P-10b, P-13; MapCrudLifecycleTest: LC-06, LC-07 |
| Directions (:685) | 1 | MapDirectionsShareTest: D-01/D-02, D-07, D-08, D-11; MapLocationCrudTest: D-02b, D-10; MapGuardsTest: D-09 |
| Seed steps (:371, :519, :605, :631, :699, :767, :1133) | 7 | Suite fixtures: `MapBaseTest` city selection, plus MapCityCrudTest C-27 / MapCrudLifecycleTest LC-00/LC-01 fixtures |
| Cleanup equivalents (in other rows) | 0 | MapCleanupTest CL-01…CL-03 (listed for completeness) |

---

## 5. WEB-ONLY (35 web tests, 12 groups)

| Web group (map.md lines) | Count | Reason and evidence |
|---|---|---|
| Window size, theme toggle, Full View, narrow widths (:35, :46, :51, :1090, :1097) | 5 | `fragment_map.xml` toolbar has only `btnCities`, `btnLocationPins`, `btnFitAll`, `btnSearch`, `btnFilter`, `btnPin`, `btnViewAll`, `btnZoneList`, `btnTypes` (:134-278). No theme or Full View control. The app follows the system theme instead (dark mode is covered by M15) |
| Classic view routes and URL params (:705, :711, :719, :725, :914, :921, :1046, :1051, :1056, :1062, :1067) | 11 | Android has no Classic/Windowed views or URLs; it uses a nav graph inside `NewMapPage`. Lat/Lng fields are disabled (`fragment_location_form.xml` `etLat`/`etLng` `android:enabled="false"`, :257-294) |
| Guide dismissed per tab session (:1104) | 1 | Tutorial has only collapse (`btnToggleTutorial`, `fragment_map.xml:55`); no dismiss X and no sessionStorage |
| Hover preview and drawn-zone card, including viewer and server variants (:511, :617, :751, :758, :1299) | 5 | No hover on touch. No zone circle click: no match for `setOnCircleClickListener` / "Edit Zone" in `new_map/` |
| Forward In App / Remote projects (:241, :247, :253, :259) | 4 | Share is the system chooser only (`MapFragment.kt:3006-3060`); no match for "Forward" in `new_map/` |
| Web loading states (:180 "Loading Google Maps...", :229 "Locating you...") | 2 | No match for either string in `new_map/` or `fragment_map.xml`. The Maps key is a build resource, and the camera falls back directly (`MapFragment.kt:1793-1800`) |
| Typed, unpicked address (:268) | 1 | `etAddress` is `focusable="false"` and opens Places on tap (`fragment_location_form.xml:219-226`, `LocationFormFragment.kt:285`). It cannot be typed |
| Search includes overlay pins; picking a filtered-out result clears the filter (:140, :425) | 2 | Search reads only `_locations` (`MapViewModel.kt:647-659`). Result tap does not touch filters (`MapFragment.kt:1049-1055`). Consider raising :425 as a UX gap: the camera can land on a hidden marker |
| Zone remembered across reopen (:573) | 1 | `MapPrefs` stores only `last_visited_city_id` and `show_location_pins` (`util/MapPrefs.kt:53-54`). `selectCity` clears the zone (`MapViewModel.kt:371-372`) |
| Pending delete locks Cancel/close/Escape (:871) | 1 | Delete confirms are `MaterialAlertDialog`s that dismiss on tap; there is no pending state (`MapFragment.kt:1001-1009`, `LocationListFragment.kt:176-184`) |
| Types empty state "Add First Type" (:1014) | 1 | `fragment_header_manager.xml` has no empty view; "+ Add Type" `btnAdd` is always shown |
| Pending pin in sessionStorage (:1126) | 1 | The pending pin lives in the nav back-stack `SavedStateHandle` and is consumed once (`MapFragment.kt:879-900`); there is no web-storage equivalent |

---

## 6. Sheet bugs (3): do they apply to Android?

- **MAP-01: card shows the old name and address after Edit or Move.** Probably not on Android, but unverified. After a move, `refreshData()` rebuilds the carousel from the locations flow (`MapViewModel.kt:746-750`). After an edit, the map re-reads on return. M12's N-11c would prove it.
- **MAP-02: holding a cluster shows no "Zoom in to move individual pins."** Partly. Android has no such message (no match for "Zoom in to move" in the app source). A cluster cannot move a hidden pin either, because `resolveClusterItem` returns null for cluster markers and the drag is ignored (`MapFragment.kt:221-229`). So the alert is missing, but the wrong-pin move from the 15 Nov web re-run should not happen.
- **MAP-03: type chip blank for an untyped pin.** Yes, on the carousel and the List View card. `tvType` and `tvTypeBadge` are hidden when the type is blank (`CarouselCardAdapter.kt:79-93`, `LocationCardAdapter.kt:56-57`). Only the detail sheet and the search rows say "Unknown" (`LocationDetailBottomSheet.kt:36`, `SearchResultAdapter.kt:42`). Note that web test map.md:443 asserts the opposite: no "Unknown" on the List View card. The expected behaviour needs a product decision.
