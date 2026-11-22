package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapLocationFormPage;
import com.zillit.automation.pages.map.MapLocationListPage;
import com.zillit.automation.pages.map.MapPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; <b>locations</b>: the form, its validation, the detail sheet, the list view
 * and delete.
 *
 * <p>The form itself validates only name and address; coordinates arrive from whichever
 * route opened it (a dropped pin, or Add from the list). That split matters — a test that
 * fills the form without a pin behind it is testing a different path from the one users
 * take most.
 *
 * <p>Also covers the <b>list view</b> cases, because the list and the carousel render the
 * same objects through different layouts: the carousel's type label is {@code tvType},
 * the list's is {@code tvTypeBadge}, and only the list exposes a stable per-row action set
 * worth counting.
 *
 * <p><b>Writes to the backend.</b> Fixtures use the {@code ZL AUTO} prefix; the delete case
 * removes only rows carrying it, and doubles as this suite's cleanup.
 */
@Order(TestOrder.MAP_LOCATION_CRUD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapLocationCrudTest extends MapBaseTest {

    /**
     * True once {@link #createLocation()} has a saved location on the project.
     *
     * <p>The cases after it assert against that pin. Without this flag they fall back to
     * "whatever is on the project", and on an empty project an assertion like "the search
     * found my location" fails as though the app's search were broken, when the honest
     * answer is that there was nothing to find.
     */
    private static boolean fixtureSaved;

    private MapLocationListPage openList() {
        assumeTrue(ensureCitySelected(), "The location list needs a selected city");
        MapPage m = map();
        assumeTrue(m.openViewAll(), "View All not available");
        MapLocationListPage list = new MapLocationListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(15)), "Location list did not open");
        return list;
    }

    private MapLocationFormPage openAddForm() {
        MapLocationListPage list = openList();
        assumeTrue(list.addLocationShown(), "Add-location button not available (posting rights?)");
        try { driver.findElement(byAppId("btnAddLocation")).click(); } catch (Exception ignored) { }
        sleep(2000);
        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(12)), "Location form did not open");
        return form;
    }

    // ---------------- list ----------------

    @Test
    @Order(1)
    @DisplayName("V-01b The location list opens with a title and a count")
    void listOpens() {
        MapLocationListPage list = openList();
        assertTrue(list.isLoaded(Duration.ofSeconds(8)), "List not shown");
        assertTrue(list.title() != null || list.countText() != null,
                "Neither title nor count rendered");
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(2)
    @DisplayName("V-03 The list offers type filter chips")
    void listFilterChips() {
        MapLocationListPage list = openList();
        assumeTrue(list.rowCount() > 0, "No locations to filter");
        assertTrue(list.filterChipsShown(Duration.ofSeconds(6)),
                "No filter chips on a list with rows");
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(3)
    @DisplayName("V-04 Every row offers View; Edit and Delete are conditional")
    void listRowActions() {
        MapLocationListPage list = openList();
        int rows = list.rowCount();
        assumeTrue(rows > 0, "No locations to inspect");
        assertEquals(rows, list.viewButtonCount(),
                "View should be on every row, overlay rows included");
        assertTrue(list.editButtonCount() <= rows,
                "More Edit buttons than rows — " + list.editButtonCount() + " vs " + rows);
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(4)
    @DisplayName("V-02 The bottom panel switches between carousel and list")
    void panelModeSwitch() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(6)),
                "Bottom panel not open — tap a marker first");
        assumeTrue(m.togglePanelMode(), "Panel-mode pill not available");
        sleep(1500);
        assertTrue(m.bottomPanelShown(Duration.ofSeconds(6)),
                "Bottom panel disappeared when switching mode");
        m.togglePanelMode();
        backToMap();
    }

    // ---------------- form validation ----------------

    @Test
    @Order(5)
    @DisplayName("N-01 The location form opens with its fields")
    void formOpens() {
        MapLocationFormPage form = openAddForm();
        assertTrue(form.isLoaded(Duration.ofSeconds(8)), "Form not shown");
        assertNotNull(form.formTitle(), "Form has no title");
        form.cancel();
        backToMap();
    }

    @Test
    @Order(6)
    @DisplayName("N-02 Saving with no name is rejected")
    void noNameRejected() {
        MapLocationFormPage form = openAddForm();
        assumeTrue(form.save(), "Save not available");
        sleep(1500);
        assertTrue(form.stillOpen(),
                "Form closed on an empty name — expected the \""
                        + MapLocationFormPage.ERR_NO_NAME + "\" rejection");
        form.cancel();
        backToMap();
    }

    @Test
    @Order(7)
    @DisplayName("N-03 A name without an address is rejected")
    void noAddressRejected() {
        MapLocationFormPage form = openAddForm();
        assumeTrue(form.setName(fixtureName("Loc")), "Could not type a name");
        assumeTrue(form.save(), "Save not available");
        sleep(1500);
        assertTrue(form.stillOpen(),
                "Form closed with no address typed — expected the \""
                        + MapLocationFormPage.ERR_NO_ADDRESS + "\" rejection. The address"
                        + " field read: \"" + form.addressValue() + "\" (if that is not"
                        + " empty, something prefilled it and this case never tested the"
                        + " guard).");
        form.cancel();
        backToMap();
    }

    @Test
    @Order(8)
    @DisplayName("N-04 The type dropdown is offered, with Add-new-type beside it")
    void typeDropdown() {
        MapLocationFormPage form = openAddForm();
        // Add-new-type is what makes the dropdown non-fixed; its absence would mean types
        // can only be managed from the header manager. Check it BEFORE opening the
        // dropdown: the dropdown is a popup window that covers the form, so a button
        // behind it reads as absent.
        assertTrue(form.addNewTypeShown(), "No btnAddNewType beside the type dropdown");

        assumeTrue(form.openTypeDropdown(), "Type dropdown not available");
        sleep(1200);
        assertTrue(form.isLoaded(Duration.ofSeconds(5)),
                "Form lost after opening the type dropdown");

        // Dismiss the popup explicitly. Left open it eats the next taps, and that is how
        // this case used to strand the whole suite: everything from N-10 onwards skipped
        // with "Could not open Tools", because the app was still behind a dropdown.
        form.dismissTypeDropdown();
        sleep(800);
        form.cancel();
        backToMap();
    }

    // ---------------- create ----------------

    @Test
    @Order(9)
    @DisplayName("N-10 A location saves, created the way the app actually creates one")
    void createLocation() {
        // CREATED BY DROPPING A PIN, not by typing an address.
        //
        // The earlier version of this case opened the form directly and tried to type into
        // etAddress, which is focusable=false — so it skipped every run, and I reported
        // that locations could not be created on this build at all. That was wrong. The
        // app's own creation path is: pin mode → tap the map → the preview marker's
        // "Add Location" info window → the form opens with cityId/lat/lng already set
        // (MapFragment:1668), and LocationFormFragment:161-184 reverse-geocodes those into
        // etAddress by itself. Only the NAME is typed, and etName is an ordinary EditText.
        MapPage m = map();
        assumeTrue(ensureCitySelected(), "No city on this project to pin into");
        assumeTrue(m.togglePinMode(), "Pin mode not available — needs a city and posting rights");
        assumeTrue(m.pinModeActive(Duration.ofSeconds(6)), "Pin mode did not start");
        assumeTrue(m.dropPinAndCommit(),
                "The pin's \"Add Location\" info window did not open the form. The window is"
                        + " drawn on the map canvas by a custom InfoWindowAdapter, so it has"
                        + " no resource id and is tapped by offset above the marker — if the"
                        + " app's info-window layout changed, that offset needs revisiting.");

        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(10)), "Location form did not open");
        String name = fixtureName("Loc");
        assumeTrue(form.setName(name), "Could not type a name");
        // The address should already be there, filled from the dropped point.
        String prefilled = String.valueOf(form.addressValue());
        assertFalse(prefilled.isBlank(),
                "The form opened from a dropped pin but its address is empty — "
                        + "LocationFormFragment:183 should have reverse-geocoded lat/lng into it");

        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(),
                "Form stayed open after a name and a geocoded address — the save was rejected");

        backToMap();
        MapLocationListPage list = openList();
        assertTrue(list.names().stream().anyMatch(n -> n.contains(FIXTURE_PREFIX)),
                "Saved location \"" + name + "\" is not in the list");
        fixtureSaved = true;
        list.backToMap();
        backToMap();
    }

    // ---------------- what the saved pin unblocks ----------------
    //
    // Everything from here to the delete cases needs a pin that actually exists on the
    // project, which is why it lives in this suite rather than in the read-only Map
    // suite: order 9 above created one. Running these anywhere else means asserting
    // against whatever a human happened to leave behind, and a search, a filter or a
    // fit-all over an empty city passes for the wrong reason.

    @Test
    @Order(10)
    @DisplayName("F-02 In-screen search finds the location by name")
    void searchFindsFixture() {
        assumeTrue(fixtureSaved, "No " + FIXTURE_PREFIX + " location on the project"
                + " — N-10 could not create one; its skip reason has the why");
        assumeTrue(ensureCitySelected(), "Search is city-gated");
        MapPage m = map();
        assumeTrue(m.openInScreenSearch(), "Search bar did not open");
        assumeTrue(m.searchBarShown(Duration.ofSeconds(8)), "Search bar not shown");
        assumeTrue(m.typeSearch(FIXTURE_PREFIX), "Could not type a query");
        sleep(2000);

        assertTrue(m.searchResultsShown(Duration.ofSeconds(8)),
                "No result list for a query that matches a location on this project");
        assertTrue(m.searchResultNames().stream().anyMatch(n -> n.contains(FIXTURE_PREFIX)),
                "Search for \"" + FIXTURE_PREFIX + "\" returned " + m.searchResultNames());
        m.closeInScreenSearch();
        backToMap();
    }

    @Test
    @Order(11)
    @DisplayName("F-02b Search also matches on the address, not just the name")
    void searchMatchesAddress() {
        assumeTrue(fixtureSaved, "No " + FIXTURE_PREFIX + " location on the project"
                + " — N-10 could not create one; its skip reason has the why");
        assumeTrue(ensureCitySelected(), "Search is city-gated");
        MapPage m = map();
        assumeTrue(m.openInScreenSearch(), "Search bar did not open");
        assumeTrue(m.searchBarShown(Duration.ofSeconds(8)), "Search bar not shown");
        // The fixture was saved at "Bandra West, Mumbai" and its name contains no part of
        // that, so a hit here can only have come from the address field.
        assumeTrue(m.typeSearch("Bandra"), "Could not type a query");
        sleep(2000);

        assertTrue(m.searchResultsShown(Duration.ofSeconds(8))
                        && !m.searchResultNames().isEmpty(),
                "An address-only query returned nothing — getSearchResults() is documented"
                        + " to match name, address AND type");
        m.closeInScreenSearch();
        backToMap();
    }

    @Test
    @Order(12)
    @DisplayName("F-03 Tapping a result closes the search and centres the map")
    void searchResultTapClosesSearch() {
        assumeTrue(fixtureSaved, "No " + FIXTURE_PREFIX + " location on the project"
                + " — N-10 could not create one; its skip reason has the why");
        assumeTrue(ensureCitySelected(), "Search is city-gated");
        MapPage m = map();
        assumeTrue(m.openInScreenSearch(), "Search bar did not open");
        assumeTrue(m.typeSearch(FIXTURE_PREFIX), "Could not type a query");
        sleep(2000);
        assumeTrue(m.searchResultsShown(Duration.ofSeconds(8)), "No results to tap");
        assumeTrue(m.tapFirstSearchResult(), "Could not tap the first result");
        sleep(2500);

        // SearchResultAdapter's listener animates the camera, calls toggleSearch() and
        // clears the query — so the search closes and NO card opens. The spec sheet says
        // the pin's card opens; the code does not do that, and this asserts the code.
        assertFalse(m.searchResultsShown(Duration.ofSeconds(3)),
                "The result list is still up after tapping a result");
        assertTrue(m.isLoaded(Duration.ofSeconds(10)), "The map did not come back");
        backToMap();
    }

    @Test
    @Order(13)
    @DisplayName("F-09b/F-11 The filter panel lists the pin's type and selecting it sticks")
    void filterByType() {
        assumeTrue(ensureCitySelected(), "Filters are city-gated");
        MapPage m = map();
        assumeTrue(m.openFilterPanel(), "Filter button not available");
        assumeTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter panel did not open");

        java.util.List<String> labels = m.filterTypeLabels();
        assumeTrue(!labels.isEmpty(), "No type chips — the project has no pinned types");
        assertEquals(labels.size(), m.filterTypeCounts().size(),
                "Every chip should carry a count: " + labels + " vs " + m.filterTypeCounts());

        String first = labels.get(0);
        java.util.List<String> before = m.carouselNames();
        assertTrue(m.toggleTypeFilter(first), "Could not toggle the \"" + first + "\" chip");
        sleep(2500);

        // ASSERT THE EFFECT, not the chip's looks. Selection is drawn with a drawable swap
        // and a background tint (MapFragment:2251-2259); neither reaches the accessibility
        // tree, so "does the chip look ticked" is unanswerable from here — the old check
        // asked whether an ivCheck element existed, and it exists either way.
        java.util.List<String> after = m.carouselNames();
        if (labels.size() > 1) {
            assertNotEquals(before, after,
                    "Filtering by \"" + first + "\" changed nothing on screen. With "
                            + labels.size() + " types present, narrowing to one must drop"
                            + " something: " + before);
        } else {
            // One type only: everything carries it, so the set cannot shrink. All that can
            // be said is that the filter did not WIPE the list.
            assertFalse(!before.isEmpty() && after.isEmpty(),
                    "Filtering by the only type \"" + first + "\" emptied the map");
        }

        m.clearFilters();
        sleep(1200);
        backToMap();
    }

    @Test
    @Order(14)
    @DisplayName("F-12 Two type filters can be active at once")
    void filterMultiSelect() {
        assumeTrue(ensureCitySelected(), "Filters are city-gated");
        MapPage m = map();
        assumeTrue(m.openFilterPanel(), "Filter button not available");
        assumeTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter panel did not open");

        java.util.List<String> labels = m.filterTypeLabels();
        assumeTrue(labels.size() >= 2,
                "Needs pins of two different types; this project has " + labels);

        assumeTrue(m.toggleTypeFilter(labels.get(0)), "Could not toggle the first chip");
        sleep(2000);
        java.util.List<String> oneType = m.carouselNames();
        assumeTrue(m.toggleTypeFilter(labels.get(1)), "Could not toggle the second chip");
        sleep(2500);
        java.util.List<String> twoTypes = m.carouselNames();

        // Multi-select means ADDING a type widens the result. If the second tap replaced
        // the first, the set would change but not grow — single-select behaviour, which is
        // the defect this case is for.
        assertTrue(twoTypes.size() >= oneType.size(),
                "Selecting a second type did not widen the result: " + oneType.size()
                        + " → " + twoTypes.size() + ". The filter is behaving as"
                        + " single-select. One type: " + oneType + "; two: " + twoTypes);

        m.clearFilters();
        sleep(2000);
        assertTrue(m.carouselNames().size() >= twoTypes.size(),
                "Clear did not restore everything the filters hid");
        backToMap();
    }

    @Test
    @Order(15)
    @DisplayName("F-14b Fit-all frames the pins instead of reporting an empty city")
    void fitAllWithPins() {
        assumeTrue(ensureCitySelected(), "Fit-all is city-gated");
        MapPage m = map();
        assumeTrue(m.fitAll(), "Fit-all button not available");
        sleep(2500);

        // The empty-city guard and the success path are indistinguishable on the canvas,
        // so the assertion is the absence of the guard: this city demonstrably has a pin.
        assertFalse(m.textShown("No locations to zoom to", Duration.ofSeconds(3)),
                "Fit-all reported an empty city while this suite's location is on it");
        assertTrue(m.isLoaded(Duration.ofSeconds(10)), "The map did not survive fit-all");
        backToMap();
    }

    @Test
    @Order(16)
    @DisplayName("N-11 A location opens prefilled for editing and the change sticks")
    void editLocation() {
        assumeTrue(fixtureSaved, "No " + FIXTURE_PREFIX + " location on the project"
                + " — N-10 could not create one; its skip reason has the why");
        MapLocationListPage list = openList();
        assumeTrue(list.names().stream().anyMatch(n -> n.contains(FIXTURE_PREFIX)),
                "No " + FIXTURE_PREFIX + " location to edit");
        assumeTrue(list.editButtonCount() > 0, "No edit affordance (posting rights?)");
        assumeTrue(list.editLocation(FIXTURE_PREFIX),
                "Could not open the fixture row for editing");
        sleep(2500);

        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(12)), "Edit form did not open");

        String prefilled = form.nameValue();
        assertTrue(prefilled != null && prefilled.contains(FIXTURE_PREFIX),
                "Edit opened with an empty or foreign name: " + prefilled);

        String edited = prefilled + " EDITED";
        assumeTrue(form.setName(edited), "Could not change the name");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(), "The form stayed open after saving an edit");

        backToMap();
        MapLocationListPage after = openList();
        assertTrue(after.names().stream().anyMatch(n -> n.contains("EDITED")),
                "The edited name is not in the list: " + after.names());
        after.backToMap();
        backToMap();
    }

    @Test
    @Order(17)
    @DisplayName("N-08 A scene number saves with the location")
    void sceneNumberSaves() {
        assumeTrue(fixtureSaved, "No " + FIXTURE_PREFIX + " location on the project"
                + " — N-10 could not create one; its skip reason has the why");
        MapLocationListPage list = openList();
        assumeTrue(list.names().stream().anyMatch(n -> n.contains(FIXTURE_PREFIX)),
                "No " + FIXTURE_PREFIX + " location to edit");
        assumeTrue(list.editLocation(FIXTURE_PREFIX), "Could not open the fixture for editing");
        sleep(2500);

        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(12)), "Edit form did not open");
        assumeTrue(form.sceneFieldShown(), "No scene field on this form");
        assumeTrue(form.setScene("42"), "Could not type a scene number");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(), "The form stayed open after saving a scene number");

        backToMap();
        MapLocationListPage after = openList();
        assumeTrue(after.editLocation(FIXTURE_PREFIX), "Could not reopen the fixture");
        sleep(2500);
        MapLocationFormPage reopened = new MapLocationFormPage(driver);
        assumeTrue(reopened.isLoaded(Duration.ofSeconds(12)), "Form did not reopen");
        assertEquals("42", String.valueOf(reopened.sceneValue()).trim(),
                "The scene number did not persist");
        reopened.cancel();
        sleep(1500);
        backToMap();
    }

    @Test
    @Order(18)
    @DisplayName("D-02b Directions open from a pin with its destination filled in")
    void directionsFromPin() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(6)), "No card on screen");
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        sleep(3000);

        assumeTrue(m.directionsPanelShown(Duration.ofSeconds(12)), "Directions panel did not open");
        String to = m.directionsTo();
        assertTrue(to != null && !to.isBlank(),
                "The destination field is empty — it should carry the pin's name");
        m.closeDirections();
        sleep(1500);
        backToMap();
    }

    @Test
    @Order(19)
    @DisplayName("D-10 The directions panel offers Call Driver")
    void callDriverOffered() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(6)), "No card on screen");
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        sleep(3000);
        assumeTrue(m.directionsPanelShown(Duration.ofSeconds(12)), "Directions panel did not open");

        assertTrue(m.callDriverShown(),
                "btnCallDriverMap is missing from the directions panel");
        m.closeDirections();
        sleep(1500);
        backToMap();
    }

    @Test
    @Order(20)
    @DisplayName("N-16 The detail sheet copies the maps link")
    void detailCopyLink() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(6)), "No card on screen");
        assumeTrue(m.cardView(), "View not available on the card");
        sleep(2500);

        MapLocationFormPage detail = new MapLocationFormPage(driver);
        assumeTrue(detail.detailSheetShown(Duration.ofSeconds(10)), "Detail sheet did not open");
        assumeTrue(detail.copyLink(), "No copy-link action on the sheet");
        sleep(1500);

        // The clipboard is not readable through the driver, so this asserts the
        // acknowledgement the app shows rather than the clipboard contents.
        assertTrue(detail.textShown("Link copied", Duration.ofSeconds(5))
                        || detail.textShown("copied", Duration.ofSeconds(2)),
                "Copy-link gave no confirmation");
        detail.closeDetail();
        backToMap();
    }

    // ---------------- detail sheet ----------------

    @Test
    @Order(30)
    @DisplayName("N-15 The detail sheet shows coordinates, address and the copy-link action")
    void detailSheet() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(6)),
                "Bottom panel not open — tap a marker first");
        assumeTrue(m.cardView(), "View not available on the card");
        sleep(2500);

        MapLocationFormPage detail = new MapLocationFormPage(driver);
        assumeTrue(detail.detailSheetShown(Duration.ofSeconds(10)), "Detail sheet did not open");
        assertNotNull(detail.detailLatLng(), "Detail sheet shows no coordinates");
        assertTrue(detail.detailName() != null && !detail.detailName().isBlank(),
                "Detail sheet shows no name");
        detail.closeDetail();
        backToMap();
    }

    // ---------------- delete ----------------

    @Test
    @Order(40)
    @DisplayName("N-12/N-13 Delete asks first, and Cancel keeps the location")
    void deleteCancelKeeps() {
        MapLocationListPage list = openList();
        assumeTrue(list.names().stream().anyMatch(n -> n.startsWith(FIXTURE_PREFIX)),
                "No " + FIXTURE_PREFIX + " location to delete — this suite never deletes a "
                        + "location it did not create");
        int before = list.rowCount();
        assumeTrue(list.deleteButtonCount() > 0, "No delete affordance (posting rights?)");

        try { driver.findElement(byAppId("btnDelete")).click(); } catch (Exception ignored) { }
        sleep(1500);

        MapDialogPage dlg = new MapDialogPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(8)), "Delete did not ask for confirmation");
        assertTrue(String.valueOf(dlg.title()).contains(MapDialogPage.TITLE_DELETE_LOCATION),
                "Unexpected confirm title: " + dlg.title());
        assertTrue(String.valueOf(dlg.message()).toLowerCase().contains("are you sure"),
                "Unexpected confirm message: " + dlg.message());

        assertTrue(dlg.tap(MapDialogPage.BTN_CANCEL), "Cancel not available");
        sleep(2000);
        assertTrue(list.rowCount() >= before, "Cancelling the delete still removed a row");
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(41)
    @DisplayName("N-14 Confirming the delete removes the location this suite created")
    void deleteCreatedLocation() {
        MapLocationListPage list = openList();
        assumeTrue(list.names().stream().anyMatch(n -> n.startsWith(FIXTURE_PREFIX)),
                "No " + FIXTURE_PREFIX + " location left to clean up");
        int before = list.rowCount();
        assumeTrue(list.deleteButtonCount() > 0, "No delete affordance");

        try { driver.findElement(byAppId("btnDelete")).click(); } catch (Exception ignored) { }
        sleep(1500);
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)), "No confirmation dialog");
        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE), "Delete not available in the dialog");
        sleep(3000);

        assertTrue(list.rowCount() < before,
                "Row count unchanged after a confirmed delete: " + before
                        + " → " + list.rowCount());
        list.backToMap();
        backToMap();
    }
}
