# Map Module — Complete Test Specification

Derived from the source, not from the screen: every fragment, view-model and dialog in
`bottomNav/tools/new_map/**` on `vidya_AI/bug_fix_nov` @ `bd8b8fc7`.

**Scope.** `NewMapPage` hosts `map_nav_graph`, which has six destinations:

```
mapFragment ──┬─ cityListFragment ─── cityFormFragment
              ├─ studioZoneListFragment ─── studioZoneFormFragment
              ├─ locationListFragment ─── locationFormFragment ─── headerManagerFragment
              ├─ cityFormFragment            (direct, from the boundary dialog)
              └─ headerManagerFragment       (direct, from btnTypes)
```

Everything below is keyed to a real function, guard or string in that tree. Where a case is
not yet automated the reason is stated — it is never left implicit.

---

## 0. The guards, in one place

Thirteen messages gate behaviour. Each is a case because each is the only signal the user
gets, and several are easy to regress into silence.

| Message | Raised by | Condition |
|---|---|---|
| "Please select a city first" | `togglePinMode()` | no city selected |
| "You don't have permission to add cities" | city search in the panel | `!hasPostingAccess` |
| "You don't have permission to pin a location" | `btnPlacePin` | `!hasPostingAccess` |
| "No locations to zoom to" | `fitAllMarkers()` | `locations.isEmpty()` |
| "No location types to filter" | `toggleFilter()` | `types.isEmpty()` |
| "No studio zones available" | `toggleZonePicker()` | `zones.isEmpty()` |
| "Please wait for the previous move to finish" | marker drag | `dragMoveInFlight` |
| "Please set pickup and destination first" | `shareDirections()`, `requestDriverFromMap()` | either endpoint unset |
| "Location permission required" | `enableMyLocation()` | permission denied |
| "Fetching current location, please try again" | `btnUseMyLocation` | no fix yet |
| "This location has no coordinates to share" | `shareLocationPin()` | lat/lng null |
| "You don't have access to the Location tool" | `toggleLocationPins()` | `!canViewLocationTool` |
| "Maximum 10 media allowed" | location form upload | 11th attachment |

Success messages, all via `MapEvent.Success`: City created / updated / deleted · Studio zone
created / updated / deleted · Location created successfully / updated successfully / deleted /
moved · Type created / updated / deleted.

---

## 1. Module shell

| # | Case | Expected |
|---|------|----------|
| M-01 | Tools → Map opens | `NewMapPage`; header "MAP"; Google map renders |
| M-02 | Fresh project | "Select a City", "0 locations", `btnCities` reads "Add City" |
| M-03 | Tutorial card | Three numbered steps; `btnToggleTutorial` collapses and expands |
| M-04 | Tutorial state persists | Collapsed state survives leaving and re-entering (`tutorialCollapsed` in the view-model) |
| M-05 | Back | Returns to Tools, never out of the app |
| M-06 | Camera restored | `saveCameraPosition()` / `restoreMapState()` — zoom and centre survive a return |
| M-07 | `fabMapType` | Toggles normal ↔ satellite |
| M-08 | Toolbar scroll arrows | `tvScrollArrowLeft` / `tvScrollArrowRight` appear and hide at the ends |
| M-09 | `onResume` refresh | Returning from a sub-screen refetches locations and zones |

## 2. Posting rights

`applyPostingRights()` and `updateToolbarTabs()` decide visibility. Two different gates —
posting rights, and whether a city is selected — and conflating them hides real bugs.

| # | Case | Expected |
|---|------|----------|
| R-01 | View-only user | `btnAddCity`, `btnPin`, `btnTypes`, `btnPlacePin` hidden |
| R-02 | View-only user keeps reading | Directions, View, Share, the city panel all still work |
| R-03 | City-gated, not rights-gated | `btnFitAll`, `btnSearch`, `btnFilter`, `btnViewAll`, `btnZoneList` appear on city select regardless of rights |
| R-04 | Both gates | `btnPin` and `btnTypes` need a city **and** posting rights |
| R-05 | Overlay toggle is its own gate | `btnLocationPins` keys on Location-tool **view** access, independent of Map posting rights |
| R-06 | City search without rights | Toast "You don't have permission to add cities"; the field is cleared |
| R-07 | Place pin without rights | Toast "You don't have permission to pin a location" |

## 3. Cities

`CityListFragment`, `CityFormFragment`, `MapViewModel.createCity/createCityFull/updateCityById/deleteCityById/reorderCities`.

| # | Case | Expected |
|---|------|----------|
| C-01 | City panel opens | `cityListContainer`, `rvCities`, `etCitySearch`, `btnAddCity`, `btnCloseCityPanel` |
| C-02 | City form opens | `tvFormTitle`, `btnSave`, `chipGroupRadius` (NB: `etCitySearch` is on the panel too — never identify the form by it) |
| C-03 | No name | Toast "Please enter a city name"; form stays open |
| C-04 | No place picked | Toast "Please search and select a city location" |
| C-05 | Place picked | `tvCityName`, `tvLat`, `tvLng` auto-fill from the Places result |
| C-06 | Center point — map mode | `btnCenterPoint` → `etCenterSearch` → `tvCenterLat`/`tvCenterLng` |
| C-07 | Center point — intersection | `btnIntersection` → `etStreet1` + `etStreet2` + `btnFindIntersection` |
| C-08 | One street blank | Toast "Please enter both street names" |
| C-09 | Intersection found | Toast "Intersection found!"; `tvIntersectionResult` fills |
| C-10 | Intersection not found | Toast "No intersection found. Try different street names." |
| C-11 | Geocoding error | Toast "Failed to find intersection" |
| C-12 | Switch center mode | Toggling swaps the field groups and keeps what was entered |
| C-13 | No center point | Toast "Please set a center point for the zone" |
| C-14 | Radius chips | Presets selectable; `etCustomRadius` overrides (both below the fold — scroll first) |
| C-15 | City outside its own radius | Toast "&lt;city&gt; is N miles from center point, but zone radius is R miles."; blocked |
| C-16 | Create succeeds | "City created"; row appears; `tvCount` increments |
| C-17 | Cancel | No city created |
| C-18 | Edit | `btnEdit` → prefilled (`parseEditCity`); saving gives "City updated" |
| C-19 | Delete confirm | "Delete City" / *Delete "&lt;name&gt;" and all its locations?* · Delete / Cancel |
| C-20 | Delete cancelled | City stays |
| C-21 | Delete confirmed | "City deleted"; row goes |
| C-22 | Switch city | `tvCityName` and `tvLocationCount` follow; pins reload |
| C-23 | Pins are per city | City A's pins are not drawn under city B (contrast L-19) |
| C-24 | Last-visited restored | `MapPrefs.lastVisitedCityId` — survives app kill |
| C-25 | Reorder | Drag in the panel; order persists (`reorderCities`) |
| C-26 | Search | `etCitySearch` filters `rvCities` |
| C-27 | Current-city card | `cardCurrentCity` shows name/address/action, hidden for view-only users |
| C-28 | Badges | `tvCitiesBadge` / `zBadge` show unread counts |
| C-29 | Badge auto-read | A live badge on the **selected** city clears; badges on other cities stay |
| C-30 | Zoom for radius | `getZoomForRadius()` — a small-radius city lands zoomed in further than a large one |

## 4. Studio zones

| # | Case | Expected |
|---|------|----------|
| Z-01 | Zone list opens | `tvTitle`, `tvCount`, `etZoneSearch`, `rvZones`, `btnAdd` |
| Z-02 | Empty list | `emptyState` |
| Z-03 | Zone picker with no zones | Toast "No studio zones available" |
| Z-04 | Form names its city | `tvCityBadge` |
| Z-05 | No name | Toast "Please enter a zone name" |
| Z-06 | No center | Toast "Please search and select a center point location" |
| Z-07 | Intersection, one street | Toast "Please enter both street names for the intersection" |
| Z-08 | Radius 0 | Toast "Radius must be greater than 0" |
| Z-09 | City outside the zone | Toast "&lt;city&gt; is N miles from the center point, but zone radius is R miles. Please adjust…"; blocked **before** any API call |
| Z-10 | Live preview | `mapPreview` redraws as radius/center change; the marker is draggable |
| Z-11 | Create | "Studio zone created"; row appears |
| Z-12 | Card content | `tvName`, `tvRadius`, `tvCoords`, `radiusDot` + Select/View/Edit/Delete |
| Z-13 | Apply to map | `btnSelect` → row highlights, circle drawn, `btnZoneList` carries the name |
| Z-14 | Circle geometry | Radius on the map = `GeoUtil.milesToMeters(zone.miles)` |
| Z-15 | Zone detail | `btnView` → "Studio Zone Details" dialog |
| Z-16 | Edit | Prefilled (`parseEditZone`); "Studio zone updated" |
| Z-17 | Delete | Confirm → "Studio zone deleted"; circle clears |
| Z-18 | Delete the active zone | Highlight clears; the Map auto-selects the next zone |
| Z-19 | Search | `etZoneSearch` filters |
| Z-20 | City-scoped | Switching city shows only that city's zones |
| Z-21 | Toggle zone circles | `showStudioZones` shows/hides all circles |

## 5. Pinning and boundaries

`evaluatePinBoundary()` → zone first, then city. Radius 0 ⇒ **30 miles**. Missing coordinates
⇒ **Inside** (a data problem must not read as a rejection).

| # | Case | Expected |
|---|------|----------|
| P-01 | No city | Toast "Please select a city first" |
| P-02 | Enter pin mode | `pinModeBanner`; `btnPin` reads "Close", active tint |
| P-03 | Leave pin mode | Banner hides; label "Pin Location"; provisional marker removed |
| P-04 | Inside the zone | No dialog |
| P-05 | Inside the city, no zone | No dialog |
| P-06 | Outside zone, inside city | "Outside Studio Zone" · Yes / No |
| P-07 | …→ No | Nothing pinned |
| P-08 | …→ Yes | Pin created |
| P-09 | Outside both | "Outside &lt;city&gt;" · Create Another City / Pin in &lt;city&gt; / Cancel |
| P-10 | …→ Cancel or dismiss | Nothing pinned (dismiss listener ⇒ Cancel) |
| P-11 | …→ Pin in &lt;city&gt; | Pin created against the current city |
| P-12 | …→ Create Another City | City form prefilled from reverse geocode; after save the pin lands in the new city (`stashPendingPin` / `consumePendingPin`) |
| P-13 | Dialog body | Carries "New location:" and the geocoded address |
| P-14 | Drag always confirms | Even when the new point is inside ("Move here?") |
| P-15 | Drag outside city | "Outside &lt;city&gt;" · Move / Cancel |
| P-16 | Drag never offers a new city | `cityId` is locked on drag |
| P-17 | Drag cancelled | Marker snaps back |
| P-18 | Overlay pins not draggable | `draggable(canPost && !isFromLocationTool)` |
| P-19 | Form address outside city | "Outside &lt;city&gt;" · *This address is outside &lt;city&gt;. Do you want to use it anyway?* · Yes / No |
| P-20 | Concurrent drag | Second drag before the first settles → "Please wait for the previous move to finish" |
| P-21 | Pin address resolution | `resolvePinAddress()` reverse-geocodes the dropped point into the form |

## 6. Locations

| # | Case | Expected |
|---|------|----------|
| N-01 | Form fields | `etName`, `actvType`, `etAddress`, `etLat`, `etLng`, `etDescription`, `btnSave` |
| N-02 | No name | "Please enter a location name" |
| N-03 | No address | "Please enter an address" |
| N-04 | Type dropdown | `actvType` is filterable; `btnAddNewType` opens the add dialog |
| N-05 | Blank new type | "Type name cannot be empty" |
| N-06 | New type created | Toast *Type "&lt;name&gt;" created*; it joins the dropdown |
| N-07 | Subtypes | `chipGroupSubtypes` follows the chosen type (`showSubtypesForHeader`) |
| N-08 | Scene number | `etScene` saves; shows as `tvScene` on the card |
| N-09 | Media | `uploadArea` → `rvPhotos`; the 11th raises "Maximum 10 media allowed" |
| N-10 | Create | "Location created successfully"; pin and row appear; count increments |
| N-11 | Edit | Prefilled; "Location updated successfully" |
| N-12 | Delete confirm | "Delete Location" / *Are you sure you want to delete "&lt;name&gt;"?* |
| N-13 | Delete cancelled | Nothing removed |
| N-14 | Delete confirmed | "Location deleted"; marker and row go |
| N-15 | Detail sheet | `tvName`, `tvLatLng`, `tvAddress`, `tvType`, `tvSubType`, `tvDesc`, `rvImages` |
| N-16 | Copy link | `tvCopyLink` copies the maps URL |
| N-17 | Move location | "Move Location" dialog; on success "Location moved" |
| N-18 | Marker clustering | Nearby pins cluster; tapping a cluster expands it |
| N-19 | Info window | Marker info window shows name and type |

## 7. Location types (header manager)

`btnTypes` → `headerManagerFragment`. Entirely programmatic UI — the form is built in code,
not from a layout, so it has no ids and must be driven by text.

| # | Case | Expected |
|---|------|----------|
| T-01 | Opens | `tvCount`, `btnAdd`, `etTypeSearch`, `rvHeaders` |
| T-02 | Card content | `tvIcon`, `tvName`, `tvSubtypes`, Edit, Delete |
| T-03 | Add is rights-gated | `btnAdd` hidden without posting access |
| T-04 | Add dialog | Name field hinted "Type name (e.g., Hotel)", "Select Icon" emoji grid, "No icon selected" until one is picked |
| T-05 | Create | "Type created" |
| T-06 | Edit confirm | "Update Location Type" / *Updating &lt;name&gt; will change the type classification…* · Update / Cancel |
| T-07 | Update | "Type updated" |
| T-08 | Delete confirm | "Delete Location Type" / *Deleting &lt;name&gt; will remove its type classification…* · Delete / Cancel |
| T-09 | Delete | "Type deleted" |
| T-10 | Search | `etTypeSearch` filters |
| T-11 | Subtypes | Add/remove subtype rows inside the form |

## 8. Search, filters, list view

| # | Case | Expected |
|---|------|----------|
| F-01 | In-screen search | `btnSearch` (city-gated) → `searchBar` + `etSearch` + `rvSearchResults` |
| F-02 | Search results | `getSearchResults()` matches name, type and address |
| F-03 | Result tap | Centres the map and opens that pin's card |
| F-04 | Places autocomplete | `fabSearch` → Places overlay (a **different** feature from `btnSearch`) |
| F-05 | Places result card | `tvPlaceName`, `tvPlaceAddress`, `tvPlaceCoords` |
| F-06 | Pin a place | `btnPlacePin` (rights-gated) runs the full boundary check |
| F-07 | Directions to a place | `btnPlaceDirections` |
| F-08 | Close places card | `btnClosePlacesResult` → `hidePlacesResult()` |
| F-09 | Filter panel | `btnFilter` → `rvFilterTypes` with per-type counts (`getTypeCount`) |
| F-10 | No types | Toast "No location types to filter" |
| F-11 | Filter applies | Only matching markers and cards remain |
| F-12 | Multi-select | Several types at once (`toggleTypeFilter`) |
| F-13 | Clear | `tvClearFilters` restores everything |
| F-14 | Fit all | `btnFitAll` frames every marker |
| F-15 | Fit all, empty | Toast "No locations to zoom to" |
| F-16 (superseded by V-01) | List view | `btnViewAll` → `tvTitle`, `tvCount`, `chipGroupFilter`, `rvLocations` |
| F-17 (superseded by V-05) | Empty list | `emptyState`, `tvEmptyTitle`, `tvEmptyMessage` |
| F-18 (superseded by V-02) | Panel mode | `btnLocationPill` switches carousel ↔ list |
| F-19 (superseded; see V-series) | Back to map | `btnBackToMap` keeps the city |
| V-01 | List view | `btnViewAll` → `rvLocations` with `tvTitle` and `tvCount` (the F-16 view, numbered as the tests use it) |
| V-02 | Carousel ↔ list | `btnLocationPill` switches `rvCarousel` and `rvLocationList` |
| V-03 | List filter chips | `chipGroupFilter` narrows by type |
| V-04 | List row actions | `btnView` always; `btnEdit` / `btnDelete` only with posting rights |
| V-05 | Empty list | An empty city shows `emptyState`, not a blank list |

## 9. Directions

| # | Case | Expected |
|---|------|----------|
| D-01 | Open | Card `btnDirections` → `directionsPanel` |
| D-02 | Destination prefilled | `etDirectionsTo` carries the pin name |
| D-03 | Pickup prefilled | `prefillPickupWithCurrentAddress()` |
| D-04 | Use my location | `btnUseMyLocation` fills from GPS |
| D-05 | No fix yet | Toast "Fetching current location, please try again" |
| D-06 | Permission denied | Toast "Location permission required" |
| D-07 | Route | `tvDirectionsDistance` reads "N km (M mi)"; `tvDirectionsDuration` fills; the route is drawn |
| D-08 | Share directions | System chooser with the route text |
| D-09 | Share with no endpoints | Toast "Please set pickup and destination first" |
| D-10 | Call driver | `btnCallDriverMap` → `requestDriverFromMap()`; same endpoint guard |
| D-11 | Close | `btnCloseDirections` hides the panel, map intact |

## 10. Share

| # | Case | Expected |
|---|------|----------|
| S-01 | Map pin | 📍 name / type / Scene No / address / coords / `View: https://www.google.com/maps?q=lat,lng` |
| S-02 | Overlay pin | Fuller: "Shoot Location: &lt;name&gt;", spelled-out stage, Scene No, Episode No, Address, Coordinates, Description, View |
| S-03 | No coordinates | Toast "This location has no coordinates to share"; no chooser |
| S-04 | Coordinate format | Six decimals under `Locale.US` |
| S-05 | Subject line | Overlay pins use "Shoot Location: &lt;name&gt;"; Map pins use the bare name |

---

## Automation status

### Against the eleven things you asked for

Every item has automation written for it. The right-hand column is what the run actually
does with it on this build, which is not the same question.

| # | You asked for | Cases | Where it stands |
|---|---|---|---|
| 1 | Create city | C-02, C-03, C-04, C-05/C-16, C-17 | Form, both rejections and Cancel PASS. The save itself skips: a city can only be named by picking a Google Places result (finding 2) |
| 2 | Create current city | C-27 | Skips. The Add Current City card is hidden once the detected city is already on the project (finding 3) |
| 3 | Create zone | Z-01, Z-02, Z-05, Z-06, Z-08, Z-09, Z-11, Z-12 | Written. Blocked on Places for the centre point (finding 5) |
| 4 | Apply zone on map | Z-13 | Written. Needs a saved zone, so blocked behind item 3 |
| 5 | Pin inside / outside zone and city | P-01…P-07, P-09, P-10, P-10b, P-13 | The boundary verdicts are the best-covered part of the module — dialog text, Cancel, dismiss-is-Cancel, and the inside case raising nothing |
| 6 | Delete city / location / zone | C-19/C-20, C-21, N-12/N-13, N-14, Z-17, T-08, T-09 | Each suite deletes only what it created, so these skip when the create they depend on skipped. The type-manager pair (T-08, T-09) runs end to end |
| 7 | Update city / location / zone | C-18, N-11, Z-16, T-06 | Same shape as item 6 — written, gated on a fixture existing |
| 8 | List view | V-01, V-01b, V-02…V-05, F-02, F-02b, F-03, F-09/F-13, F-09b/F-11, F-10, F-12, F-14, F-14b, F-15 | Title, count, carousel/list switch, row actions, type chips, search by name and by address, multi-select, fit-all |
| 9 | Direction | D-01/D-02, D-02b, D-07, D-08, D-09, D-10, D-11 | Panel, prefilled destination, km/miles, Call Driver, share hand-off, and the return trip leaving the Map intact |
| 10 | Share location | S-01, S-01b, D-08 | Chooser opens and the Map survives the round trip. Reading what was actually shared (S-02…S-05) needs an intent receiver and is not written |
| 11 | Switch city, see pings on that city | C-22, C-22b, C-23, C-24, C-24b, L-19 | Selection, the restored last-visited city, pins scoped to the city, and the overlay deliberately staying project-wide |

109 cases across 10 suites. Items 1–4 and the create-dependent halves of 6 and 7 are held up
by one backend setting, not by missing automation — see finding 6.

### Measured run — 2026-11-20, after the stability fixes

All ten suites, one Appium session each (`./run-ui.sh --only <Class>`):
**30 passed · 2 failed · 77 skipped** of 109.

| Suite | Passed | Failed | Skipped | Session rebuilds |
|---|---|---|---|---|
| `MapModuleTest` | 8 | 0 | 6 | 0 |
| `MapCityCrudTest` | 5 | 0 | 8 | 0 |
| `MapZoneCrudTest` | 3 | 0 | 10 | 0 |
| `MapPinBoundaryTest` | 2 | 0 | 5 | 0 |
| `MapLocationCrudTest` | 5 | 2 | 16 | 5 |
| `MapTypeManagerTest` | 4 | 0 | 4 | 0 |
| `MapDirectionsShareTest` | 0 | 0 | 7 | 0 |
| `MapGuardsTest` | 2 | 0 | 5 | 0 |
| `MapLocationPinsTest` | 1 | 0 | 10 | 0 |
| `LocationToolTest` | 0 | 0 | 6 | 0 |

Nine of the ten suites finished with **no failures and no session rebuilds**. The location
suite's own best, on a settled device, was 6 passed · 0 failed · 17 skipped with 0 rebuilds;
the 2 failures above are both the instrumentation dying mid-case, not assertions.

**Why the 77 skips**, which is the only number that tells you anything:

| Skips | Cause | Is it the app? |
|---|---|---|
| ~40 | Google Places is not configured on this build, so no city, zone or location can be created — and nothing downstream has data to read | No: a backend setting (finding 6) |
| 16 | **The Location tool is not enabled on this project** — confirmed by the project owner on 2026-11-20, deferred deliberately. The Shoot Locations toggle is hidden (`MapLocationPinsTest`, 10) and the Location tool will not open (`LocationToolTest`, 6) | No: expected, not a fault |
| 7 | "This city has no pins to act on" — directions and share need a pin, which needs Places | No: downstream of the first row |
| ~8 | Honest preconditions: needs two cities, two zones, or a fixture this suite created | No |
| ~6 | Unreachable in this build: the centre-point section the layout never shows (finding 1) | **Yes** — dead code behind a `visibility="gone"` |

So of 77 skips, exactly one group of about six points at the app, and it is the one already
written up as finding 1. The rest are two configuration switches.

### Deferred by decision: the Location tool

The Location tool is **not enabled** on `ZL-Android-Automation`, and as of 2026-11-20 that
is deliberate — the project owner asked to leave it until they enable it.

Both affected suites are kept, not deleted or disabled, and both skip with the reason
spelled out: `LocationToolTest` (6 cases: LT-01, LT-02, LT-02b, LT-04, LT-12, LT-13) and
`MapLocationPinsTest` (10 of 11: the whole L- overlay series). They will start exercising
themselves the moment the tool is switched on — nothing needs rewriting, so do not treat
these 16 skips as a gap to close or re-diagnose them as a configuration bug.

The one case that still runs without the tool is the overlay suite's negative: **L-02**,
"Without Location-tool access the toggle is absent, not merely disabled" — which is passing,
and is exactly the right thing to be asserting while the tool is off.

### Three stability fixes behind these numbers

1. **`waitForIdleTimeout` was at its 10s default.** A GoogleMap renders continuously and so
   never reports idle; every lookup in a map-canvas suite paid the full wait, and the
   UiAutomator2 server eventually died inside `UiAutomation.disconnect` — which the
   device's own crash log records. `MapPinBoundaryTest` and `MapZoneCrudTest` went from 4
   session rebuilds each to **0**. Set in `DriverFactory`, overridable with
   `zillit.waitForIdleTimeout`.
2. **`MapModuleTest`, `LocationToolTest` and `MapLocationPinsTest` never registered
   `ReasonLogger`** — they extend `BaseTest`, not `MapBaseTest`. Sixteen skips had no
   reason in the log or the XML. All three now register it.
3. **`ReasonLogger` could not report a `@BeforeEach` abort at all.** `TestWatcher` only
   covers the test method, so a suite whose entry code gave up skipped silently by design.
   It now also implements `LifecycleMethodExecutionExceptionHandler`.

Two findings that looked like app defects and were not: `R-03` ("View All missing with a
city selected") and `P-01` ("pin mode started with no city"). `R-03` was reading a dying
session — it passes with fix 1 in place. `P-01` was my precondition racing the
`lastVisitedCityId` restore; `MapViewModel.togglePinMode()` (:586) guards correctly, and the
case now keys on `btnPin`'s presence instead of the label.

### What is implemented, and what is not

Counted from the suites themselves on 2026-11-20, not maintained by hand: **114 of the 201
specified cases** have automation, spread over ten suites.

| Suite | IDs covered |
|---|---|
| `MapModuleTest` | M-01…M-03, M-05, C-01, C-22, C-24, R-03, F-01, F-04, F-09, F-13, F-14, M-07, V-01, V-05 |
| `MapCityCrudTest` | C-02…C-05, C-07, C-08, C-14, C-16…C-22, C-26, C-27 |
| `MapZoneCrudTest` | Z-01, Z-02, Z-04…Z-06, Z-08, Z-09, Z-11…Z-13, Z-16, Z-17, Z-19 |
| `MapPinBoundaryTest` | P-01…P-07, P-09, P-10, P-13 |
| `MapLocationCrudTest` | N-01…N-04, N-08, N-10…N-16, V-01b…V-04, F-02, F-03, F-09b, F-11, F-12, F-14b, D-02b, D-10 |
| `MapTypeManagerTest` | T-01, T-02, T-04…T-06, T-08…T-10 |
| `MapDirectionsShareTest` | D-01, D-02, D-07, D-08, D-11, S-01, S-01b, F-05 |
| `MapGuardsTest` | F-10, F-15, Z-03, D-09, M-04, C-23, C-24b |
| `MapLocationPinsTest` | L-01…L-03, L-05…L-11, L-15…L-17, L-19 |
| `LocationToolTest` | LT-01, LT-02, LT-02b, LT-04, LT-12, LT-13 |

**Not implemented**, grouped by what each group actually waits on:

| Cases | Waiting on |
|---|---|
| C-06, C-09…C-13, C-15 | The centre-point section, which this build never shows (finding 1) — unreachable, not merely unwritten |
| Z-07, Z-10, Z-14, Z-18, Z-20, Z-21 | A saved zone, so blocked behind Places (finding 5) |
| N-05…N-07, N-09 | A saved location, media fixtures and subtypes (finding 6) |
| P-08, P-11, P-12, P-14…P-21 | A saved pin to drag, and multi-screen drag flows |
| R-01, R-02, R-04…R-07, T-03 | A second, view-only account |
| C-25, C-28…C-30 | A second city, drag-reorder, and badge traffic |
| D-03…D-06 | A pickup address, a GPS fix on demand, and a denied-permission run |
| S-02…S-05 | Reading the share-sheet payload, which needs an intent receiver |
| F-06…F-08 | The Places result card's own actions (pin it, directions to it, close it) |
| M-06, M-08, M-09 | Camera restore, toolbar scroll arrows, `onResume` refetch — written up, not yet coded |
| T-07, T-11, LT-03, LT-05…LT-11 | Type update/subtype rows, and the Location tool's own move flows |
| L-04, L-12…L-14, L-18, L-20…L-25, X-01…X-05 | Moving an item between Location-tool tabs, which writes to the backend |
| F-16…F-19 | Nothing — **superseded** by V-01…V-05, which the suites use instead |

The biggest single unblocker is still **Places**: it gates items 1–4 of your list and, through
them, roughly forty of the rows above.

### Case IDs: one numbering, and how it was reconciled

There were **two** sheets numbering the same module differently.
`MAP_LOCATION_TESTCASES.md` was written first; this sheet was written later off the Android
source and renumbered almost everything — it has 30 city rows where the older sheet has 20,
because inserting the real validation order pushed every later row down. The tests were
written against the older sheet, so their IDs were right about one sheet and wrong about the
other, and no one reading either sheet could tell which.

**This sheet is now the canonical numbering.** The tests were renamed to match it
(2026-11-20). Rules:

- One ID means one behaviour, across all ten suites.
- A second suite covering the same behaviour from another angle takes a **`b` suffix** —
  `V-01` is the read-only suite opening the list, `V-01b` is the location suite opening it
  with its own fixture in place. Already the convention for `F-02b` and `M-11b`.
- `T-` is the **type manager** (section 7). The Location tool's own screens, which also
  used `T-`, are now `LT-` in `MAP_LOCATION_TESTCASES.md` section 5.
- `L-` (overlay) and `X-` (cross-module) stay specified in `MAP_LOCATION_TESTCASES.md`
  sections 4 and 6; they never collided, so they were left alone.

Where the renaming landed, for anyone reading an older report:

| Was, in the code | Is now | Why |
|---|---|---|
| C-12 Cancel | C-17 | Old sheet's C-12; this sheet's C-12 is "switch center mode" |
| C-14/C-15 Delete + Cancel | C-19/C-20 | C-14 here is the radius chips — it was doing double duty |
| C-16 Delete confirmed | C-21 | |
| C-17 Switch city | C-22b | C-22 is the module suite's selection case |
| C-20 City search | C-26 | |
| M-24 / M-25 / M-26 | V-01 / V-05 / F-09+F-13 | This sheet files list view and filters under V- and F- |
| M-15 Back from the Map | M-05 | |
| Z-08 Create succeeds | Z-11 | Old sheet had no intersection rows for zones |
| Z-13 Delete a zone | Z-17 | |
| N-09 Create succeeds | N-10 | N-09 here is Media |
| N-13 / N-14 | N-14 / N-15 | |
| T-01…T-13 (Location tool) | LT-01…LT-13 | Freed `T-` for the type manager |
| S-01 (twice) | S-01 / S-01b | Two cases shared one ID |
| P-10 (twice) | P-10 / P-10b | Same |

Verified after the rename: 123 distinct IDs across the ten suites, none used by two cases,
and every one has a row in one of the two sheets.

---

## What the code says, where it differs from this sheet

These are findings about the **app**, not about the tests. Each one is why a block of cases
above cannot pass, and each is read off the source rather than inferred from a failing run.

### 1. A city's centre point and radius cannot be set from the Android app

`fragment_city_form.xml:173` declares `sectionCenterPoint` with `android:visibility="gone"`,
and the only line in `CityFormFragment` that touches it (`:140`) sets it to `GONE` again for
edit mode. Nothing anywhere makes it visible. Everything inside lines 173–460 is therefore
unreachable in this build:

* the centre-point switch (`switchCenterPoint`) and its content panel,
* the Google-Places centre search (`etCenterSearch`),
* the street-intersection finder (`btnIntersection`, `etStreet1`, `etStreet2`,
  `btnFindIntersection`) and its three geocoding strategies,
* **the radius chips** (`chipGroupRadius`, `etCustomRadius`).

Consequence for behaviour, not just for tests: every city falls back to the 30-mile default
in `evaluatePinBoundary()`, so the pin-boundary verdicts are the same for every city on
every project. **C-05…C-13 cannot pass.** C-14 is written as the regression detector — it
fails if the section is ever shown — so the day this is fixed, the suite says so.

### 2. The city form has no name field

The add-city form's only input is `etCitySearch`, a Google Places `AutoCompleteTextView`.
`tvCityName`, `tvCityDesc`, `tvLat` and `tvLng` are TextViews filled from whatever the user
picks. There are exactly two ways to name a city:

1. pick a Places result — unavailable in the develop build, which has no Places-enabled key;
2. the "Add Current City" card, which navigates to the form with `prefillName` /
   `prefillAddress` / `prefillLat` / `prefillLng` and disables the search field
   (`CityFormFragment:108`).

And the search field is not even typeable: `CityFormFragment:249-250` sets
`etCitySearch.isFocusable = false` and makes its click launch `launchCityAutocomplete()`, so
it is a button wearing an EditText's clothes. That is why the city cases skipped with
"could not type a city name" — a locator-shaped message for a UI that has no such input.

So C-03's "a name without a picked place" is **not expressible in this UI** — you cannot
type a name without picking a place — and city creation on a develop build depends entirely
on the current-city card. C-03 is now written the other way round, as a regression detector
that fails if the field ever becomes editable, with instructions to restore the original
case when that happens.

### 3. The current-city card hides once that city exists

`updateCurrentCityCard()` returns early when `viewModel.cities` already contains the
reverse-geocoded city name. On this emulator that name is "Mountain View", a previous run
added it, and the card has been hidden ever since. It is also hidden without posting access,
and before geocoding returns. C-27's skip now names all three rather than blaming the GPS —
the device has had a fix (37.422, -122.084) throughout.

### 4. Tapping a search result does not open the pin's card

F-03 above expects "centres the map and opens that pin's card". `SearchResultAdapter`'s
listener (`MapFragment:1048`) animates the camera, calls `toggleSearch()` and clears the
query — so the search closes and no card opens. The implemented case asserts the code's
behaviour, and this note is the discrepancy.

### 5. A studio zone cannot be created without Google Places either

`StudioZoneFormFragment` sets the centre only three ways, and all three are shut on this
build:

* `etCenterSearch` is `isFocusable = false` and its click launches the Places autocomplete
  (`:269-270`), so it cannot be typed into;
* the street-intersection finder goes through `findIntersectionViaPlaces()`;
* the preview map's **draggable marker** would set `lat`/`lng` on drag end (`:202`), but
  `updateMapPreview()` returns early while `lat == 0.0 && lng == 0.0` (`:214`) — so on the
  add path no marker is ever drawn, and the one route that does not need Places is
  unreachable by construction.

`saveZone()` then rejects with "Please search and select a center point location". **Z-08
and everything that depends on a saved zone (Z-09…Z-21, and zone-based pin boundaries)
cannot pass here.** The suite now skips with this explanation rather than "Center-point
search not available", which read like a broken locator.

### 6. A location cannot be created without Google Places either

`fragment_location_form.xml:219-226` declares `etAddress` with `focusable="false"`,
`inputType="none"`, `clickable="true"` and the hint "Tap to search address...", and
`LocationFormFragment:285` makes its click launch the Places autocomplete. There is no
typed-address path, and `saveLocation()` rejects a blank address (`:266`).

That completes the picture, and it is the single most useful thing in this document:

| What | Only way to create it | Works here? |
|---|---|---|
| City | Places search, or the Add Current City card | No — Places dead; card hidden once the city exists |
| Studio zone | Places search (centre point) only | No |
| Location | Places search (address) only | No |

**Nothing in the Map module can be created on this build.** Every write-path case therefore
skips, and so does every read-path case that needs data to read. One configuration change
on the backend unblocks all of them.

How much of the skipping this actually accounts for, measured rather than assumed: in the
five-suite verification run of 2026-11-20, 24 cases skipped and only **4** of those skips
named Places or the dead centre-point section. **14** said "Could not open Tools (app is not
on a bottom-nav screen)" — a harness defect, not an app one: a case left a dialog or an
autocomplete popup open, such a popup swallows BACK, and no Map suite ever restarted the
app, so one stranded case took every case after it. The remaining 6 were honest
preconditions (needs two cities, no fixture of ours to delete). So Places is the blocker on
the write paths, but it was *not* what produced most of the skips on that run; the harness
was. `MapBaseTest.recoverToBottomNav()` now escalates — decline the modal, unwind with
BACK, re-enter the project, restart the app — instead of skipping.

Note which cases this makes *more* trustworthy: N-02 ("no name is rejected") and N-03 ("no
address is rejected") pass precisely because neither field can be filled by typing, so their
guards always fire. They are honest passes, but they cover less than their titles suggest.

### 7. Places is a runtime, backend-supplied key — not a build setting

`MapFragment:682` initialises the SDK with `SharedPref.getGooglePlaceKey()`, a value fetched
from the backend and held in encrypted preferences. The Maps SDK key in `strings.xml` is a
separate thing and demonstrably works, since tiles render. So "Places does nothing in the
develop build" is an environment/configuration problem on the account side, not a missing
layout or a wrong id — and it is the single change that would unblock city creation, zone
creation and roughly thirty cases behind them.

### 8. In-screen search ignores the Location-tool overlay

`MapViewModel.getSearchResults()` filters `_locations` only, with a comment saying the web
client also searches the overlay and that the difference was raised separately. Worth
knowing before reading F-02 as full coverage of "search finds my shoot locations".

---

## Web gap cases (2026-11-23/24)

Source: `web-e2e/gaps/map.md` (220 web cases vs the Android suites). Every MISSING / partial /
conflict row ends below as **implemented** (with its last run result) or **not automatable**
(with the reason). App: Zillit_32539-dev (`Zillit-Android-devbuild`). Fixtures in the new
suites are named `ZL_AUTO …`, recorded when made, and deleted by exact name (plus a sweep of
this suite's own name shapes, so a crashed run is cleaned up by the next one). Real cities,
locations and zones are read, never edited or deleted.

New suites: `MapWebGapTest` (`TestOrder.MAP_WEB_GAP` = 220) and `MapZoneGuardTest`
(`TestOrder.MAP_ZONE_GUARDS` = 221). Fixed in place: `MapZoneCrudTest` Z-05 and Z-08,
`MapLocationFlowTest` L-13b (adds L-17).

Two-user cases run on their own with both devices:
`ZILLIT_UDID=<emulator> ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only 'MapWebGapTest.rt*'`.

### Bugs these cases reproduce

| Case | Web id | What happens on Android |
|---|---|---|
| **T-12** (intermittent) | map.md:1357 (M1) | Once, deleting a location type **also deleted the location using it** (confirmed by the later sweep); once it kept it. Same `deleteHeader` endpoint as web, where the bug is consistent. Needs more runs. |
| **H-06** | map.md:1163 | A description can never be **cleared**: the form trims it to "", `UpdateLocationRequest.description` defaults to "" and the Json uses `encodeDefaults = false`, so the field is left out of the PUT and the server keeps the old text. Same for scene and type. |
| **Z-08b** | map.md:543 (M7) | "Custom" radius left **empty** saves a zone ("Studio zone created") — `toIntOrNull() ?: 30`. |
| **Z-11c** | map.md:865 (M7) | A double tap on **Create Zone creates two zones** — `btnSave` is never disabled while saving. |
| **Z-08d** | map.md:843 (M7) | A **201-mile** zone saves (card "201 mi radius") — `saveZone` refuses only `<= 0`; no upper bound. |
| RT-07 | map.md:1245 | (pending run) a stale edit form is a full-record PUT. |

### Case list

| Group | Case | Web | Suite | Last result |
|---|---|---|---|---|
| M1 | T-12 Deleting a type keeps its locations, untyped | 1357 | MapWebGapTest | **Intermittent**: FAIL 23:59 run (location gone; the later sweep also no longer found it), PASS 06:21 run (location kept). Needs more runs before filing |
| M10 | F-16 Filter rows count each type incl. "Unknown", equal to List View chips | 413 | MapWebGapTest | PASS (devbuild uses `getTypeCount`; the gap's "always 0" defect is fixed) |
| M10 | F-16b Apply one type → "Filter (1)", header "1 locations"; Reset restores | 413 | MapWebGapTest | PASS |
| M10 | V-06 Header count = List View "N total" | 113, 395 | MapWebGapTest | PASS |
| M10 | M-10 Type filter cleared after leaving and re-entering the Map | 1109 | MapWebGapTest | PASS (23:19 run); 06:21 run failed on the test side (Filter chip unreachable, label null) — retry added, not yet rerun |
| M10 | C-22c Switching city clears the filter | 307 | MapWebGapTest | **not automatable here**: the project has one city (Mountain View); a second city cannot be ZL_AUTO-named |
| M10 | T-10b Subtype search lists its parent type | 389 | MapWebGapTest | PASS |
| M10 | F-02c >8 matches → exactly 8 results | 877 | MapWebGapTest | skip: fewer than 9 own locations match (the "+N more" line is web-only) |
| M5 | H-07 Padded type name saved trimmed | 1182 | MapWebGapTest | PASS |
| M5 | H-01 Padded name and scene saved trimmed | 1139, 1169 | MapWebGapTest | skip — etScene not reachable on the edit form (test-side) |
| M5 | H-02 Spaces-only name refused, form stays open | 828 | MapWebGapTest | PASS |
| M5 | H-03 Unicode / emoji / RTL name saves; lower-case search finds it | 1145, 883 | MapWebGapTest | PASS |
| M5 | H-04 HTML-looking name and description shown literally | 1151, 895 | MapWebGapTest | PASS |
| M5 | H-08 Regex / special-character search never crashes | 883 | MapWebGapTest | PASS (23:19 run) |
| M5 | H-06 Multi-line description keeps lines; spaces-only saves empty | 1163 | MapWebGapTest | **FAIL — bug (cannot clear)** |
| M5 | H-05 1,000-character name: outcome recorded, card stays on screen | 1157 | MapWebGapTest | FAIL: the save was refused and no toast was read. Unconfirmed — toasts are not always readable, so this may be test-side |
| M5 | H-09 Duplicate location names: list and search agree with the outcome | 1176 | MapWebGapTest | duplicate ACCEPTED; FAIL is a test bug (the whole-list reader merges rows by name, so two equal names count as one) |
| M5 | Z-19b Duplicate zone names: list agrees with the outcome | 1200 | MapZoneGuardTest | PASS (duplicate accepted, two rows) |
| M11 | N-07 Type pick drives the subtype checkboxes | 357 | MapWebGapTest | skip — the dropdown item could not be tapped (test-side) |
| M11 | N-11b Double tap on Update Location: one save, one row | 859 | MapWebGapTest | PASS |
| M14 | N-15b Untyped detail sheet: "Unknown", no Description / Photos | 1032 | MapWebGapTest | FAIL, not yet diagnosed — the 'Unknown' badge matcher found nothing; probably test-side |
| M14 | V-07 Deleting the last location of the selected chip: no blank list | 1231 | MapWebGapTest | PASS (Android falls back to "All"; web keeps the chip + empty state) |
| M12 | N-11c List View edits show on the map's bottom panel | 732 | MapWebGapTest | skip (depends on H-01/H-04 renames) |
| M12 | N-19 Marker tap opens its card | 611 | MapWebGapTest | FAIL, not yet diagnosed: marker found and tapped, but the panel carousel was read empty; probably test-side |
| M12 | P-14/P-17 Drag a saved marker: Move Location, Cancel keeps, Move moves | 623, 744 | MapWebGapTest | FAIL, not yet diagnosed: no dialog after the long-press drag; probably the gesture, not the app |
| M9 | C-16b "Cities (N)" equals the panel rows | 56 | MapWebGapTest | PASS |
| C3 | T-07 After the Update confirm, a subtype added in the edit form saves | 383 | MapWebGapTest | PASS |
| M7 | Z-07 Intersection with one street blank: no centre, cannot save | 838 | MapZoneGuardTest | PASS |
| M7 | Z-05b Blank zone name refused even with a centre | 549 | MapZoneGuardTest | PASS |
| M7 | Z-05 (fixed: clears the pre-filled name first) | 549, 1194 | MapZoneCrudTest | PASS |
| M7 | Z-08 (fixed: sets a centre first, so the radius guard is what fires) | 843 | MapZoneCrudTest | PASS |
| M7 | Z-08b Custom with empty radius refused | 543 | MapZoneGuardTest | **FAIL — bug** |
| M7 | Z-08c Small Custom radius saves, card shows it | 555, 848 | MapZoneGuardTest | PASS (4 mi — 1 mi cannot contain the city centre, 2.5 mi away, which Android requires) |
| M7 | Z-08d 201-mile radius refused | 843 | MapZoneGuardTest | **FAIL — bug (saved as 201 mi)** |
| M7 | Z-11c Double tap on Create Zone creates one zone | 865 | MapZoneGuardTest | **FAIL — bug (two zones)** |
| M8 | Z-15 View → "Studio Zone Details" with radius + coordinates; OK closes | 579 | MapZoneGuardTest | PASS |
| M8 | Z-13b City with zones opens with a zone named on the zone button | 537 | MapZoneGuardTest | PASS |
| M8 | Z-18 Deleting the active zone moves the map off it | 779, 1121 | MapZoneGuardTest | PASS (the label can take a few seconds; the case waits up to 20 s) |
| M3 | RT-02 B's pin updates A's count live; B's delete takes it back | 806, 1239 | MapWebGapTest (rt*) | pending (two devices) |
| M3 | RT-03 B's new / deleted type shows in A's open type list | 818, 1272 | MapWebGapTest (rt*) | pending |
| M3 | RT-06 B's zone becomes A's active zone; B's delete moves A off it | 812, 1259 | MapWebGapTest (rt*) | pending |
| M3 | RT-07 A stale edit form does not wipe B's newer description | 1245 | MapWebGapTest (rt*) | pending |
| C2 | L-15/L-16/L-17 overlay cards read-only but keep View/Directions | 124, 129 | MapLocationFlowTest L-13b | covered there with its own Shortlisted fixture; `MapLocationPinsTest` skips when the project has no overlay pin |
| C-18 | City rows offer no Edit | — | MapCityCrudTest | PASS |

### Not automatable (with the reason)

| Group | Web rows | Reason |
|---|---|---|
| M2 failed/slow/offline reads and writes (E-01…E-05, slow loaders) | 186–223, 792, 930–1008, 1206–1225 | Needs a network fault (airplane mode / Wi-Fi off / proxy) on a shared device that three other agents are using at the same time. Not done. The source behaviour is recorded in the gap file. |
| M3 RT-01 city added by B, RT-04 B deletes A's open city, RT-05 badge in another city | 800, 1278, 1284, 1291 | A city cannot carry the `ZL_AUTO` prefix: its name comes only from Places / reverse geocoding (C-04, finding 2). So these would create and delete real-named cities, which the data rule forbids. |
| M4 view-only / demoted accounts (R-01…R-10) | 173, 467, 591, 1308–1333 | Would change Map rights for the project's real users (Vidya Pixel / Vidya Testing are its only two members), with no safe restore if a run dies midway. |
| M6 poles / antimeridian / lng 0 pins | 901, 907 | Needs pins seeded by API at exact coordinates. Android can only pin where the camera is, and has no seeding route here. |
| M8 Z-16b zone edit keeps address/description; stubbed zone | 785, 1026 | Android cannot give a zone an address (`createStudioZone` sends none), so the address needs a web/API-seeded zone. A stub needs a proxy. |
| M9 C-25 reorder, C-21b delete the open city, C-24c fallback, C-31 duplicate city | 325, 331, 343, 661, 1115 | Reorder changes the real cities' order. The others need a test city, which cannot be `ZL_AUTO`-named (see M3). C-31 is also blocked because the current-city card hides once the city exists. |
| M11 N-09 media add / remove / cap 10; P-19 address outside the city | 108, 479, 485, 673 | N-09 goes through the system photo picker and uploads real media to the project's storage. P-19 needs Google Places to pick an address. |
| M13 D-06 directions without location permission; F-06/F-07 Places result pin / directions | 147, 637, 691 | D-06 revokes an app permission on a shared device mid-run. F-06/F-07 need Places. |
| M15 dark mode on every surface | 1074, 1082 | Needs `cmd uimode night` (system setting) on a shared device. |
| C1 F-03 search result opens the card | 235, 407 | Not a test gap: Android only centres the map (`MapFragment:1049-1055`). Parity issue to raise with product; F-03 asserts Android's behaviour. |
| C5 wording of the form messages | 103 | Wording only; the Android tests already match the Android strings. |
