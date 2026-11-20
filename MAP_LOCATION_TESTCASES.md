# Map & Location — Test Cases

Source: `vidya_AI/bug_fix_nov` @ `bd8b8fc7`, app `com.zillit.zillitapp.dev` (Zillit_32537-dev).

Read from:
- `bottomNav/tools/new_map/**` — the Map module (`NewMapPage` → `map_nav_graph` → `MapFragment`)
- `bottomNav/tools/location/**` — the Location tool (`LocationPage`, 3 tabs)
- `data/model/LocationToolPin.kt`, `util/MapPrefs.kt`, `ui/map/MapViewModel.kt`

---


> **Numbering:** `MAP_MODULE_TESTCASES.md` is the canonical sheet for case IDs. It was
> written later, off the Android source, and renumbered most series — its C-, Z-, N-, D-
> and F- numbers are the ones the tests use. The IDs in *this* sheet's tables are the
> older numbering and no longer match the suites, except for the **L-** (overlay),
> **X-** (cross-module) and **LT-** (Location tool's own screens) series, which this
> sheet still owns. The Location-tool series was renamed from `T-` to `LT-` on 2026-11-20
> so that `T-` means only the type manager.

## 1. How the two modules connect

The Location tool holds shoot locations in three tabs — **Selects → Shortlist → Final**
(`SELECTED_IDENTIFIER` / `SHORTLISTED_IDENTIFIER` / `PUBLISH_IDENTIFIER`).

The Map module draws its **own** pins per city, and on top of them an **overlay** of the
Location tool's Shortlist and Final entries, fetched from `GET /api/v2/location/map-pins`
and projected onto the Map's own `PinnedLocation` shape by `toPinnedLocation()`.

Six behaviours fall out of that design, and each needs covering:

| Behaviour | Where it is decided |
|---|---|
| Only **Shortlisted** and **Final** cross over — Selects never do | `LocationPinStatus.tagFor()` returns null for anything else |
| The wire status may be `published`, `final` **or** `finalized` — all three mean Final | same function |
| The overlay is **project-wide**, not city-scoped | `visibleLocationPins()`; the endpoint carries no city reference |
| Overlay pins are **read-only** on the Map | `canMutate = postingAccess && !isFromLocationTool` |
| The overlay defaults to **ON** and survives app kill | `MapPrefs.showLocationPins`, default `true`, SharedPreferences |
| The toggle only appears with **Location-tool view access** | `canViewLocationTool` gates `btnLocationPins` visibility |

Stage tags are reused as the marker *type*, so the type filter, chips and counts work on
them with no extra plumbing (`Constants.LOCATION_TYPE_ICONS`: Shortlisted `#F99300`,
Final `#C0392B`).

Two label registers, deliberately different:
- Carousel / list card → **"Shortlisted Shoot Location"** / **"Finalized Shoot Location"**
- Filter chip → the short tag **"Shortlisted"** / **"Final"**

---

## 2. Map module — core

| # | Case | Expected |
|---|------|----------|
| M-01 | Open Tools → Map | `NewMapPage` opens; header `MAP`; Google map renders |
| M-02 | Fresh project, no cities | `tvCityName` = "Select a City", `tvLocationCount` = "0 locations", `btnCities` labelled "Add City" |
| M-03 | Tutorial card | "How to Use the Map Module" with the 3 numbered steps; `btnToggleTutorial` collapses and expands it |
| M-04 | Open the city panel | `cityListContainer` shows; `rvCities` lists cities; `etCitySearch` filters them |
| M-05 | Select a city | `tvCityName` becomes the city name; `tvLocationCount` becomes that city's count; panel closes |
| M-06 | Toolbar after a city is selected | `btnPin`, `btnTypes`, `btnViewAll`, `btnZoneList`, `btnFitAll`, `btnSearch`, `btnFilter` all appear |
| M-07 | Last-visited city persists | Select city B, leave Map, re-enter → B is selected, not the alphabetical first (`MapPrefs.lastVisitedCityId`) |
| M-08 | Horizontal toolbar scroll | `tvScrollArrowRight` / `tvScrollArrowLeft` appear and hide at the ends |
| M-09 | `btnFitAll` | Camera fits every drawn marker |
| M-10 | `fabMapType` | Cycles map type (normal / satellite / hybrid) |
| M-11 | `fabSearch` / `btnSearch` | `searchBar` + `etSearch` show; `rvSearchResults` fills as you type |
| M-12 | Places result card | Tapping a Places hit shows `placesResultCard` with `tvPlaceName`, `tvPlaceAddress`, `tvPlaceCoords` |
| M-13 | `btnClosePlacesResult` | Dismisses the card |
| M-14 | View-only user | `btnAddCity`, `btnPin`, `btnTypes`, `btnPlacePin` hidden; Directions still available |
| M-15 | Back from Map | Returns to Tools, not out of the app |

## 3. Map — pins, carousel and filters

| # | Case | Expected |
|---|------|----------|
| M-20 | Tap a marker | `bottomLocationPanel` opens with `rvCarousel` scrolled to that pin |
| M-21 | Carousel card content | `tvName`, `tvType`, `tvCoords` (3dp), `tvAddress`, `tvImageCount` ("📷 n"); `tvScene` only when a scene number exists |
| M-22 | Selected card highlight | Stroke `map_accent_navy`, width 4, background `map_selected_item_bg` |
| M-23 | `btnLocationPill` / `btnCloseLocationPanel` | Toggle between carousel and `rvLocationList`; close dismisses the panel |
| M-24 | `btnViewAll` | Navigates to `locationListFragment` with `tvTitle`, `tvCount`, `chipGroupFilter`, `rvLocations` |
| M-25 | Empty state | With no locations, `emptyState` + `tvEmptyTitle` + `tvEmptyMessage` show |
| M-26 | `btnFilter` | `filterPanel` opens; `rvFilterTypes` lists each type; `tvClearFilters` resets |
| M-27 | Filter by type | Only matching markers and cards remain; `tvLocationCount` follows |
| M-28 | `btnBackToMap` | Returns from the list to the map with the city intact |

## 4. The Location-tool overlay — the shortlist/final feature

This is the heart of the request.

| # | Case | Expected |
|---|------|----------|
| **L-01** | `btnLocationPins` is present | Labelled **"Shoot Locations"**; visible only when the user has Location-tool **view** access |
| **L-02** | No Location-tool access | `btnLocationPins` is **GONE** entirely (not merely disabled) |
| **L-03** | Default state is ON | On a first-ever open the overlay is already on — `MapPrefs.showLocationPins` defaults to `true` |
| **L-04** | Active styling | While ON: background `map_active_button_bg`, text `map_active_button_text` |
| **L-05** | Count in the label | With pins loaded the label reads **"Shoot Locations (n)"**; with 0 it stays plain "Shoot Locations" |
| **L-06** | Toggle OFF | Overlay markers disappear; label drops the count and reverts to the default tint |
| **L-07** | Toggle back ON | Pins return; a fresh `fetchLocationPins()` runs |
| **L-08** | Persists across app kill | Toggle OFF, force-stop, relaunch, reopen Map → still OFF (SharedPreferences, not SavedStateHandle) |
| **L-09** | Shortlisted pin label | Carousel card `tvType` reads **"Shortlisted Shoot Location"**, colour `#F99300` |
| **L-10** | Final pin label | Carousel card `tvType` reads **"Finalized Shoot Location"**, colour `#C0392B` |
| **L-11** | Filter chip register | The chip shows the **short** tag "Shortlisted" / "Final", not the spelled-out label |
| **L-12** | Selects never appear | A location still in the Selects tab is absent from the Map at any zoom, with the overlay ON |
| **L-13** | Shortlist → appears | Move a location Selects → Shortlist in the Location tool, return to Map, refresh → it is now drawn as Shortlisted |
| **L-14** | Shortlist → Final | Move the same one on to Final → tag flips to Finalized; it is **not** drawn twice |
| **L-15** | Read-only: no Edit | An overlay card shows **no** `btnEdit`, even for a user with posting rights |
| **L-16** | Read-only: no Delete | Same card shows **no** `btnDelete` |
| **L-17** | View and Directions remain | `btnView` and `btnDirections` are present and work on an overlay card |
| **L-18** | Marker not draggable | An overlay marker cannot be dragged, even with posting rights (`draggable(canPost && !isFromLocationTool)`) |
| **L-19** | Project-wide, not city-scoped | A pin filed under city B is still drawn while city A is selected (pins with no city are always shown) |
| **L-20** | Pins without coordinates | A shoot location with no lat/lng, or exactly `0,0`, is skipped — `isDrawable` is false |
| **L-21** | Unnamed pin | A pin with a blank `file_name` is labelled **"Unnamed"** |
| **L-22** | Share text differs | Sharing an overlay pin uses the Location-tool wording, not the Map's own |
| **L-23** | Fetch failure | An error toast appears **once** per session, not on every resume/socket refresh (`locationPinsErrorReported`) |
| **L-24** | Toggle without access | If `canViewLocationTool` is false, tapping yields the warning "You don't have access to the Location tool" |
| **L-25** | Final wins a duplicate | A place in both stages is drawn once, as Final — the backend collapses it; the client must not de-duplicate again |

## 4b. Cities — create, edit, delete, switch

City form is `fragment_city_form.xml` (`dialog_add_city.xml` is the same field set in a dialog).
A city is a **name + coordinates + a center point + a radius**; the radius is what every later
boundary check measures against.

| # | Case | Expected |
|---|------|----------|
| C-01 | Open Add City | `btnAdd` / `btnCities` → form with `tvFormTitle`, `etCitySearch`, `chipGroupRadius`, `btnSave`, `btnCancel` |
| C-02 | Save with no name | Toast **"Please enter a city name"**; form stays open |
| C-03 | Save with a name but no place picked | Toast **"Please search and select a city location"** (`cityLat == 0.0 && cityLng == 0.0`) |
| C-04 | Pick a city from search | `tvCityName`, `tvLat`, `tvLng` auto-fill |
| C-05 | Center point — map mode | `toggleCenterPointType` → `btnCenterPoint`; `etCenterSearch` fills `tvCenterLat` / `tvCenterLng` |
| C-06 | Center point — intersection mode | `btnIntersection` → `etStreet1` + `etStreet2` + `btnFindIntersection`; success toasts **"Intersection found!"** |
| C-07 | Intersection with one street blank | Toast **"Please enter both street names"** |
| C-08 | Intersection that does not exist | Toast **"No intersection found. Try different street names."** |
| C-09 | Radius chips | `chipGroupRadius` selects a preset; `etCustomRadius` overrides it |
| C-10 | City outside its own center radius | Toast *"&lt;city&gt; is N miles from center point, but zone radius is R miles."*; save blocked |
| C-11 | Create succeeds | City appears in `rvCities`; `tvCount` increments |
| C-12 | Cancel | `btnCancel` / `btnClose` closes without creating |
| C-13 | Edit a city | `btnEdit` on `item_city_card` → form prefilled; saving updates the row |
| C-14 | Delete — confirmation wording | `btnDelete` → **"Delete City"** / *Delete "&lt;name&gt;" and all its locations?* with **Delete** / **Cancel** |
| C-15 | Delete — Cancel | City stays; count unchanged |
| C-16 | Delete — confirm | City leaves `rvCities`; `tvCount` decrements |
| C-17 | Switch city | Selecting another city reloads its pins; `tvCityName` + `tvLocationCount` follow |
| C-18 | Pins are per city | City A's pins are not drawn while city B is selected (**the Map's own pins** — the Location-tool overlay is deliberately different, see L-19) |
| C-19 | Reorder | Cities drag-reorder in the panel and the order persists |
| C-20 | City search | `etCitySearch` filters `rvCities` |

## 4c. Studio zones — create, edit, delete, apply

Zone form is `fragment_studio_zone_form.xml`; the list is `fragment_studio_zone_list.xml`,
reached from `btnZoneList`. A zone is a **named circle inside a city**, and once applied it
becomes the *first* boundary a new pin is measured against.

| # | Case | Expected |
|---|------|----------|
| Z-01 | Open the zone list | `btnZoneList` → `tvTitle`, `tvCount`, `etZoneSearch`, `rvZones`, `btnAdd` |
| Z-02 | Empty state | With no zones, `emptyState` shows |
| Z-03 | Open Add Zone | Form with `tvCityBadge` naming the parent city |
| Z-04 | Save with no name | Toast **"Please enter a zone name"** |
| Z-05 | Save with no center | Toast **"Please search and select a center point location"** |
| Z-06 | Radius 0 | Toast **"Radius must be greater than 0"** |
| Z-07 | **City outside the zone radius** | Toast *"&lt;city&gt; is N miles from the center point, but zone radius is R miles. Please adjust the center or increase the radius."*; save blocked |
| Z-08 | Create succeeds | Zone appears in `rvZones`; `tvCount` increments |
| Z-09 | Zone card content | `tvName`, `tvRadius`, `tvCoords`, `radiusDot`, and `btnSelect` / `btnView` / `btnEdit` / `btnDelete` |
| Z-10 | **Apply a zone on the map** | `btnSelect` → the row highlights, the map draws the zone circle, `btnZoneList` carries the active zone's name |
| Z-11 | Zone circle geometry | Circle radius on the map equals `GeoUtil.milesToMeters(zone.miles)` |
| Z-12 | Edit a zone | `btnEdit` → prefilled; saving updates radius/name and redraws the circle |
| Z-13 | Delete a zone | **"Delete"** / **"Cancel"** confirm; on confirm the row goes and the circle clears |
| Z-14 | Deleting the active zone | Highlight clears and the Map auto-selects the next zone |
| Z-15 | Zone search | `etZoneSearch` filters `rvZones` |
| Z-16 | Zone is city-scoped | Switching city shows that city's zones only |

## 4d. Pinning — the boundary rules

This is the part with the most behaviour per line of code. `evaluatePinBoundary()` runs the
**zone check first, then the city check**, and returns one of three verdicts. Both radii
default to **30 miles** when the stored value is 0 or missing, and a city or zone with no
coordinates returns `Inside` as a safe default — a data-integrity problem must not read to
the user as a rejection.

| # | Case | Expected |
|---|------|----------|
| P-01 | Pin mode needs a city | `btnPin` with no city → warning **"Please select a city first"** |
| P-02 | Enter pin mode | `pinModeBanner` shows; `btnPin` text becomes **"Close"** and takes the active tint |
| P-03 | Leave pin mode | Banner hides, label returns to **"Pin Location"**, the provisional marker is removed |
| P-04 | **Pin inside the zone** | No dialog at all — proceeds silently |
| P-05 | **Pin inside the city, no zone active** | No dialog — proceeds silently |
| P-06 | **Pin outside the zone but inside the city** | Dialog **"Outside Studio Zone"** / *Location is outside the &lt;zone&gt;. Do you want to pin the location?* with **Yes** / **No** |
| P-07 | P-06 → No | Nothing is pinned |
| P-08 | P-06 → Yes | The pin is created inside the city |
| P-09 | **Pin outside both city and zone** | Dialog **"Outside &lt;city&gt;"** / *This location is outside the selected city and studio zone. What would you like to do?* with three options: **Create Another City**, **Pin in &lt;city&gt;**, **Cancel** |
| P-10 | P-09 → Cancel, or dismiss | Nothing is pinned (the dismiss listener counts as Cancel) |
| P-11 | P-09 → Pin in &lt;city&gt; | The pin is created against the current city despite being outside it |
| P-12 | P-09 → Create Another City | City form opens, prefilled from the reverse-geocoded name/address; after saving, the pin lands in the new city |
| P-13 | Dialog shows the address | The body carries *"New location:"* and the reverse-geocoded address |
| P-14 | **Drag always confirms** | Dragging an existing pin confirms even when the new position is inside |
| P-15 | Drag outside the city | Dialog **"Outside &lt;city&gt;"** / *The new position is outside &lt;city&gt; and any studio zone. Move the pin anyway?* with **Move** / **Cancel** |
| P-16 | **Drag never offers Create Another City** | Only Move / Cancel — a pin's `cityId` is locked, so moving cities means re-pinning |
| P-17 | Drag → Cancel | The marker snaps back to its original position |
| P-18 | Overlay pins are not draggable | `draggable(canPost && !isFromLocationTool)` |
| P-19 | Address picked outside the city in the form | Dialog **"Outside &lt;city&gt;"** / *This address is outside &lt;city&gt;. Do you want to use it anyway?* with **Yes** / **No** |

## 4e. Locations — create, edit, delete

Form is `fragment_location_form.xml`, reached from pin mode or from `btnAddLocation` in the list.

| # | Case | Expected |
|---|------|----------|
| N-01 | Open the form | `tvFormTitle`, `etName`, `actvType`, `etAddress`, `etLat`, `etLng`, `btnSave`, `btnCancel` |
| N-02 | Save with no name | Toast **"Please enter a location name"** |
| N-03 | Save with no address | Toast **"Please enter an address"** |
| N-04 | Type dropdown | `actvType` lists types; `btnAddNewType` creates one, toast *Type "&lt;name&gt;" created* |
| N-05 | Blank new type | Toast **"Type name cannot be empty"** |
| N-06 | Subtypes | `chipGroupSubtypes` filters by the chosen type |
| N-07 | Scene number | `sceneLayout` / `etScene` saves and shows on the card as `tvScene` |
| N-08 | Photos | `uploadArea` → `rvPhotos`; past ten, toast **"Maximum 10 media allowed"** |
| N-09 | Create succeeds | Pin appears on the map and in `rvLocations`; `tvLocationCount` increments |
| N-10 | Edit a location | `btnEdit` → prefilled; saving updates the card and the marker |
| N-11 | Delete — wording | **"Delete Location"** / *Are you sure you want to delete "&lt;name&gt;"?* with **Delete** / **Cancel** |
| N-12 | Delete — Cancel | Nothing is removed |
| N-13 | Delete — confirm | Marker and row both go; count decrements |
| N-14 | Detail sheet | Tapping a marker's View opens `bottomsheet_view_pinned_location` — `tvName`, `tvLatLng`, `tvAddress`, `tvType`, `tvSubType`, `tvDesc`, `rvImages`, `tvCopyLink` |
| N-15 | Copy link | `tvCopyLink` copies the maps URL |
| N-16 | Move location | `dialog_move_location` — `chipGroupStatus`, `etCommonCity`, `etCommonAddress`, `btnSubmit` |

## 4f. List view, directions and share

| # | Case | Expected |
|---|------|----------|
| V-01 | List view | `btnViewAll` → `rvLocations` with `tvTitle` and `tvCount` |
| V-02 | Carousel ↔ list | `btnLocationPill` switches `rvCarousel` and `rvLocationList` |
| V-03 | List filter chips | `chipGroupFilter` narrows by type |
| V-04 | List row actions | `btnView` always; `btnEdit` / `btnDelete` only with posting rights and not an overlay pin |
| D-01 | Open directions | Card `btnDirections` → `directionsPanel` with `etDirectionsFrom`, `etDirectionsTo` |
| D-02 | Destination prefilled | `etDirectionsTo` carries the pin's name |
| D-03 | Use my location | `btnUseMyLocation` fills `etDirectionsFrom` from GPS |
| D-04 | Distance and duration | `tvDirectionsDistance` reads *"N km (M mi)"*; `tvDirectionsDuration` fills |
| D-05 | Share directions | `btnShareDirections` opens the system chooser with the route text |
| D-06 | Call driver | `btnCallDriverMap` is offered where a driver exists |
| D-07 | Close | `btnCloseDirections` hides the panel, map intact |
| S-01 | Share a Map pin | Chooser with *📍 name / type / Scene No / address / coords / View: https://www.google.com/maps?q=lat,lng* |
| S-02 | Share an overlay pin | Fuller text: *Shoot Location: &lt;name&gt;*, the spelled-out stage, Scene No, Episode No, Address, Coordinates, Description, View link |
| S-03 | Share with no coordinates | Toast **"This location has no coordinates to share"**; no chooser |
| S-04 | Coordinate format | Six decimal places, `Locale.US`, so a comma-decimal locale cannot corrupt the link |

## 5. Location tool — its own screens

The Location tool tile is **not currently enabled** on `ZL-Android-Automation`; these cases
skip until it is switched on via Tools → "Click here to select tools".

| # | Case | Expected |
|---|------|----------|
| LT-01 | Open Tools → Location | `LocationPage` opens with header "Location" |
| LT-02 | Three tabs in order | **Select** · **Shortlist** · **Final** |
| LT-03 | Tab badges | Unread counts render on each tab |
| LT-04 | Folder listing | Each tab shows its folders / scenes; empty tabs show an empty state |
| LT-05 | Open a folder | `LocationListingPage` with `search`, `recScene` |
| LT-06 | Search | Filters within the current tab |
| LT-07 | Long-press → Move To Shortlist | `move_shortlisted` offered from Select |
| LT-08 | Move Select → Shortlist | Item leaves Select, appears in Shortlist |
| LT-09 | Move Shortlist → Final | Item leaves Shortlist, appears in Final |
| LT-10 | Multi-select move | `deleteMoveRl` with `btnMoveTo` / `btnCancel` / `btnDelete` |
| LT-11 | Location detail | `LocationDetailPage` — images, description, scene no. |
| LT-12 | Filter icon | `filterIv` opens the filter sheet |
| LT-13 | Back | Returns to Tools |

## 6. Cross-module

| # | Case | Expected |
|---|------|----------|
| X-01 | Round trip | Shortlist in Location tool → Map shows it → move to Final → Map re-tags it |
| X-02 | Remove from Shortlist | Moving back to Select removes the pin from the Map |
| X-03 | Overlay OFF isolates | With the overlay OFF, Location-tool changes never affect the Map |
| X-04 | Map's own pins unaffected | Map-module pins keep Edit/Delete while overlay pins do not, in the same carousel |
| X-05 | Count arithmetic | `tvLocationCount` = city pins + visible overlay pins |

---

## 7. Automation status

Implemented, in the canonical numbering (see `MAP_MODULE_TESTCASES.md` for the full
per-suite breakdown and the gap list):

- `MapModuleTest` — M-01…M-03, M-05, plus C-01, C-22, C-24, R-03, F-01, F-04, F-09, F-13,
  F-14, M-07, V-01, V-05 (this sheet's M-24…M-26 are filed under V- and F- there)
- `MapLocationPinsTest` — L-01…L-03, L-05…L-11, L-15…L-17, L-19
- `LocationToolTest` — LT-01, LT-02, LT-02b, LT-04, LT-12, LT-13 (LT-03, LT-05…LT-11 are
  the move flows, still manual)

Deliberately **not** automated, with reasons:

- **L-12…L-14, X-01…X-03** need a location moved between Location-tool tabs, which writes
  to the project's backend. They are written up above and left manual until the Location
  tool is enabled and we agree on a disposable fixture.
- **L-23, L-25** need a failing endpoint and a duplicate-stage row — server-side fixtures.
- **M-14** needs a second, view-only account.
