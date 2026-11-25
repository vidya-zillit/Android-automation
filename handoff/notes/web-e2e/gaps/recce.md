# Recce: sheet bugs vs Android suite

Web input: `recce.md` (0 web tests, 5 sheet bugs).
Android suites: `tests/recce/RecceTest.java` and `RecceePaginationTest.java`.
App source: `Zillit-Android/app/src/main/java/com/zillit/zillitapp/recce/`, called `recce/` below.

## Summary

| Bug | Title | Status | Android case / note |
|---|---|---|---|
| RECCE-01 | Crew picker: tapping a member while a search is active does not tick them | **MISSING** | `RecceFormPage.openAddFromCrew()` exists, but no test uses it |
| RECCE-02 | A blank Lunch stop added in Edit is dropped on publish | **MISSING** | `multipleStopsPersistToDetail` names every stop and checks the Stops tile, which does not count Lunch |
| RECCE-03 | Detail page grows wider than the screen when the Route map loads | WEB-ONLY | Android draws a live `MapView`, not a static snapshot image |
| RECCE-04 | Server PDF shows "Map error: g.co/staticmaperror" | **MISSING** (server-side) | The Android Export calls the same server PDF. No test exports |
| RECCE-05 | Disabled "Use" in the location picker stays orange | **MISSING** | The Android source has the same styling, so this would fail today |

Counts: 4 MISSING, 1 WEB-ONLY, 0 COVERED, 0 CONFLICT.

---

## MISSING, in full

### RECCE-01: picking a crew member while a search is active

What the Android code does:
- `fAddFromCrew` ("Add from project crew…") opens `RecceCrewPickerBottomSheet`.
- The sheet's ids are `crewSearch`, `crewRecycler`, `crewEmpty` ("No crew members found"), `crewChosen` ("%d chosen") and `crewAdd` ("Done").
- Each row (`item_recce_crew.xml`) has `crewName`, `crewRole` and `crewCheck`.
- A row click toggles a `selected` set by user id, and the filter rebuilds the list (recce/ui/form/RecceCrewPickerBottomSheet.kt:97-110 and :137-143). So it **should** work. This case is a regression guard for the sheet bug.

**Proposed case:** `RecceTest.crewPickerTicksWhileSearching`

**Steps:**
1. Tap the Create FAB (`recceFab`) → `openAddFromCrew()`.
2. Read the first `crewName` → N. Type its first 3 letters into `crewSearch`.
3. Tap the row whose `crewName` equals N.

**Expected:**
- That row's `crewCheck` is checked.
- `crewChosen` reads "1 chosen".

**Then:**
1. Tap `crewAdd` ("Done"). Expect `personCount()` to grow by 1, with a `personName` row equal to N (RecceFormFragment.kt:404-420).
2. Reopen the picker. N is still ticked, with no search typed.
3. Untick N and tap Done. The row is removed.
4. Leave the form with back. Nothing is saved.

### RECCE-02: a blank Lunch stop added in Edit survives publish

How the Android client builds the stops:
- It sends every stop row (`stopRows.map { RecceStop(...) }`, RecceFormFragment.kt:584-596), including blank ones. If the stop goes missing, the server dropped it, and Android will show that too.
- Lunch stops are **not counted** in the Stops summary or tile (`stopRows.count { it.kind != LUNCH }`, RecceFormFragment.kt:629). So check the timeline rows, not `stopsTileCount()`.
- The detail screen draws Lunch with the fork icon `recce_ic_fork` and no number (RecceDetailFragment.kt:263-289). The kind label `stopKind` reads "Lunch".

**Proposed case:** `RecceTest.blankLunchStopSurvivesEditPublish`

**Steps:**
1. Create and publish `QA Recce Lunch <epoch>` with title, unit, date, RDV time and RDV place. Reuse the `createAndPublish` steps.
2. Open the recce from the list → `detailEditBtn`.
3. `addStop()`. On the new row, `setStopKind(last, "Lunch")`. Leave its place, time and notes empty.
4. Publish (`fPublish`).
5. Reopen the detail screen and scroll `detailTimeline`.

**Expected:** exactly one timeline row has `stopKind` = "Lunch".

**Then:** delete the recce from the list card (the `deleteRecce` flow).

### RECCE-04: the exported PDF draws the route map

- `RecceDetailViewModel.generatePdf()` calls `repository.generatePdf(recceId)` on the server (RecceDetailViewModel.kt:79-90).
- So the "staticmaperror" text comes from the server, and the same PDF reaches Android.
- Appium cannot read inside a PDF viewer, so do this in two parts.

**Proposed case:** `RecceTest.exportOpensPdf`

**Steps:**
1. Open a published QA recce that has stops.
2. Tap `detailExportBtn`. Wait for the PDF loading state to clear.

**Expected:**
- A PDF viewer or chooser opens: a foreign package is in front, or the in-app viewer is shown.
- No "PDF failed" toast.

**Also:** check the map part against the API in an API test or a manual step. Download the attachment the export returns, extract its text, and assert it does not contain `staticmaperror`. Mention this in the run notes. Appium alone cannot prove it.

### RECCE-05: the disabled "Use this location" button looks disabled

What the Android code does:
- `pickerUse` is set `isEnabled = false` until a place is chosen (RecceLocationPickerFragment.kt:102, enabled at :252).
- Its style uses `app:backgroundTint="@color/recce_brand"`, a plain `#F99300` (fragment_recce_location_picker.xml:194; colors_recce.xml:11). That is not a colour state list, so the disabled button stays orange. **This bug is present on Android too.**

**Proposed case:** `RecceTest.disabledUseLooksDisabled`

**Steps:**
1. Open the Create form → `openRdvLocationPicker()`. Wait for `isLocationPickerOpen`.
2. Before touching the map or W3W field, read `pickerUse`.

**Expected:**
- (a) `getAttribute("enabled")` is "false". This part passes today.
- (b) Crop the element screenshot (`pickerUse.getScreenshotAs`) and sample its centre pixel. Expect it **not** to be within ±12 per channel of `#F99300` (for example, grey or reduced alpha). This part **fails today**: mark it `known-bug` and report it.

**Note:** the map picker depends on the location tool. If the map does not render, skip as `locationPickerOpens` already does.

---

## WEB-ONLY

- **RECCE-03:** on Android the route map is a live `MapView` (`binding.routeMap.onCreate` / `getMapAsync`, RecceDetailFragment.kt:81-82), inside `detailScroll`, with `routeMapEmpty` when there are no points (:354). There is no static snapshot image that could widen the page, so the web/iOS cause does not apply.
- **Optional, cheap guard:** in `detailOpens`, assert that the width of `detailScroll` equals the window width once `routeMap` is displayed.
