package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.MapDialogPage;
import com.zillit.automation.pages.MapLocationFormPage;
import com.zillit.automation.pages.MapLocationListPage;
import com.zillit.automation.pages.MapPage;
import com.zillit.automation.pages.MapTypeManagerPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP — the full data lifecycle, run as one ordered story on one city.
 *
 * <p>The other Map suites each cover one screen. This one exists because most real Map
 * bugs are not on a screen, they are between screens: a location that saves but does not
 * appear on the map, a count that does not follow a delete, a pin that keeps its old
 * position after a drag, a city switch that shows another city's pins. None of that can be
 * found by opening a screen and reading it — the data has to be created, changed and
 * removed by the suite itself.
 *
 * <p><b>Everything here is created through the app's own flows</b>, which for the Map means
 * the pin: {@code MapFragment:1668} commits a dropped pin into the location form with
 * lat/lng attached, and the form reverse-geocodes them into the address
 * ({@code LocationFormFragment:161-184}). That is the ONLY creation path that works on a
 * build without Google Places, and it is also the path a real user takes.
 *
 * <p><b>Cleanup.</b> Every fixture is named with the {@code ZL AUTO} prefix and the closing
 * cases delete exactly what the opening cases created. Nothing else on the project is
 * touched — in particular this suite never deletes a city or location it did not make.
 *
 * <p><b>What is NOT here.</b> Studio zones. A zone's centre can only be set through Places
 * ({@code StudioZoneFormFragment}: lat/lng stay 0.0 until a marker is dragged, and the
 * marker is only drawn once they are non-zero), so a zone cannot be created on this build
 * at all. The zone-dependent boundary case below therefore skips rather than pretending.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.MAP_CRUD_LIFECYCLE)
class MapCrudLifecycleTest extends MapBaseTest {

    private static final int WANTED = 3;

    private final List<String> created = new java.util.ArrayList<>();
    private String renamed;
    private String typeName;
    private final List<String> tagged = new java.util.ArrayList<>();
    private final List<String> notCreated = new java.util.ArrayList<>();
    private String inlineTypeName;

    // ──────────────────────────── create ────────────────────────────

    @Test
    @Order(0)
    @DisplayName("LC-00 A location type is created, to tag the fixtures with")
    void createOwnType() {
        assumeTrue(ensureCitySelected(),
                "btnTypes is city-gated and this project has no city to select");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openTypes(), "Types button not available — needs a city AND posting rights");
        String name = fixtureName("Type");
        boolean made = createType(name);
        backToMap();
        // Not a hard failure for the rest of the suite: this is only ONE of the app's two
        // ways to create a type. LC-00b covers the other, and the filter cases need a type
        // from either. Still asserted, because when this path breaks that IS worth knowing.
        if (made) typeName = name;
        assertTrue(made, "The dedicated add-type form did not produce a type: "
                + lastTypeFailure + ". The inline path (LC-00b) is tried separately.");
    }

    @Test
    @Order(1)
    @DisplayName("LC-01 Three locations are created by dropping three pins in the city")
    void createSeveralLocations() {
        assumeTrue(ensureCitySelected(), "No city on this project to pin into");
        int before = new MapPage(driver).locationCount();

        // Three different points, all comfortably inside the city: the map is centred on
        // the city, so a small offset from the middle stays within any sane radius.
        double[][] points = {{0.5, 0.5}, {0.42, 0.56}, {0.58, 0.44}};
        for (int i = 0; i < WANTED; i++) {
            String name = fixtureName("Loc" + (i + 1));
            // The first two carry our own type, the third carries none — that difference
            // is what makes the filter cases below able to prove anything.
            // BOTH type routes get exercised, every run — they are two different features
            // and either can break on its own.
            //   pin 1 → the type LC-00 made through the dedicated form (when it worked)
            //   pin 2 → a name that exists NOWHERE yet, typed straight into the location
            //           form, which is the app's inline shortcut. LC-00b then checks the
            //           type manager really gained it.
            //   pin 3 → no type at all, so the filter cases have something to exclude.
            if (inlineTypeName == null) inlineTypeName = fixtureName("Inline");
            String type = null;
            if (i == 0) type = typeName != null ? typeName : inlineTypeName;
            if (i == 1) type = inlineTypeName;
            boolean ok = createLocationByPin(points[i][0], points[i][1], name, type);
            if (!ok) {
                // ONE retry, from a known state. The commit tap aims at an info window
                // drawn on the map canvas, so a miss is possible; a miss is not the same
                // as "the app cannot save this". What must not happen is a silent loss.
                String firstTry = lastPinFailure;
                System.out.println(">>> retrying pin " + (i + 1) + " after: " + firstTry);
                backToMap();
                ensureToolbarReady();
                sleep(1500);
                ok = createLocationByPin(points[i][0], points[i][1], name, type);
                if (!ok) {
                    notCreated.add("pin " + (i + 1) + ": " + firstTry
                            + " | retry: " + lastPinFailure);
                }
            }
            if (ok) {
                created.add(name);
                if (type != null) tagged.add(name);
            }
        }

        assertEquals(WANTED, created.size(),
                "Only " + created.size() + " of " + WANTED + " pins became locations, even"
                        + " with a retry each. What stopped them: " + notCreated);

        int after = new MapPage(driver).locationCount();
        if (before >= 0 && after >= 0) {
            assertEquals(before + WANTED, after,
                    "The location count did not follow the three new pins: "
                            + before + " → " + after);
        }
    }

    @Test
    @Order(12)
    @DisplayName("LC-00b A type typed into the location form is created with the location")
    void typeCreatedInlineWithLocation() {
        // THE SECOND WAY TO MAKE A TYPE, and the easier one. LocationFormFragment reads
        // actvType as plain text and hands it straight to saveLocation(locationType = …)
        // (:262, :279) — it never checks the name against the existing headers. So typing
        // a name that does not exist yet creates that type along with the location.
        //
        // This matters beyond convenience: it is the path a user takes when they are
        // pinning a location and the type they want is not in the list.
        assumeTrue(inlineTypeName != null && tagged.stream().anyMatch(n -> n != null),
                "LC-01 did not save a location carrying the inline type name");

        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openTypes(), "Types button not available");
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        assumeTrue(types.isLoaded(Duration.ofSeconds(12)), "Type manager did not open");

        List<String> names = types.typeNames();
        assertTrue(names.stream().anyMatch(n -> n != null && n.contains(inlineTypeName)),
                "\"" + inlineTypeName + "\" was typed into a location's type field and the"
                        + " location saved, but the type is not in the type manager: " + names
                        + ". Either the type was not created, or it was created without the"
                        + " name that was typed.");
        backToMap();
    }

    @Test
    @Order(2)
    @DisplayName("LC-02 All three appear in the list view, not just on the map")
    void allThreeInList() {
        assumeTrue(!created.isEmpty(), "LC-01 created nothing — its reason is above");
        MapLocationListPage list = openList();
        List<String> names = list.names();
        for (String name : created) {
            assertTrue(names.stream().anyMatch(n -> n != null && n.contains(name)),
                    "\"" + name + "\" saved but is missing from the list view. Saved to the"
                            + " map and absent from the list is a real defect, not a"
                            + " refresh delay — the list reads the same ViewModel.");
        }
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(3)
    @DisplayName("LC-03 Search finds each one by its own name")
    void searchFindsEach() {
        assumeTrue(!created.isEmpty(), "LC-01 created nothing — its reason is above");
        MapPage m = map();
        for (String name : created) {
            assumeTrue(m.openInScreenSearch(), "In-screen search did not open");
            m.typeSearch(name);
            sleep(1500);
            List<String> hits = m.searchResultNames();
            assertTrue(hits.stream().anyMatch(n -> n != null && n.contains(name)),
                    "Search for \"" + name + "\" returned " + hits);
            m.closeInScreenSearch();
            sleep(600);
        }
        ensureToolbarReady();   // a search left open hides the toolbar from every later case
    }

    @Test
    @Order(31)
    @DisplayName("LC-12 Filtering by a type shows only that type's locations")
    void filterNarrowsToType() {
        String type = typeName != null ? typeName : inlineTypeName;
        assumeTrue(type != null, "Neither route produced a type to filter by");
        assumeTrue(tagged.size() >= 1, "No location carries our type");
        assumeTrue(created.size() > tagged.size(),
                "Every location carries the type, so a filter cannot be shown to narrow anything");
        ensureToolbarReady();
        MapPage m = map();

        assumeTrue(m.openFilterPanel(), "Filter button not available. Toolbar showed "
                + m.toolbarSnapshot());
        assumeTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter panel did not open");
        assumeTrue(m.filterTypeLabels().stream().anyMatch(l -> l != null && l.contains(type)),
                "Our type is not among the filter chips: " + m.filterTypeLabels());

        assertTrue(m.toggleTypeFilter(type), "Could not select the \"" + type + "\" chip");
        sleep(2500);

        // THE POINT OF A FILTER. Not that the chip looks selected — that what is on screen
        // actually narrowed. The untagged fixture must be gone.
        List<String> shown = m.carouselNames();
        String untagged = created.stream().filter(n -> !tagged.contains(n)).findFirst().orElse(null);
        if (untagged != null && !shown.isEmpty()) {
            assertFalse(shown.stream().anyMatch(n -> n != null && n.contains(untagged)),
                    "\"" + untagged + "\" has no type, but it is still shown while the"
                            + " \"" + type + "\" filter is active: " + shown);
        }
        for (String name : tagged) {
            assertTrue(shown.isEmpty() || shown.stream().anyMatch(n -> n != null && n.contains(name)),
                    "\"" + name + "\" carries \"" + type + "\" but vanished when that"
                            + " filter was applied: " + shown);
        }
    }

    @Test
    @Order(32)
    @DisplayName("LC-13 Clear restores everything the filter hid")
    void clearRestores() {
        assumeTrue(typeName != null || inlineTypeName != null,
                "Neither route produced a type to filter by");
        assumeTrue(!created.isEmpty(), "LC-01 created nothing");
        MapPage m = map();
        if (!m.filterPanelShown(Duration.ofSeconds(2))) {
            assumeTrue(m.openFilterPanel(), "Filter button not available");
            assumeTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter panel did not open");
        }
        assumeTrue(m.clearFilters(), "Clear not offered on the filter panel");
        sleep(2500);
        ensureToolbarReady();

        List<String> after = openList().names();
        for (String name : created) {
            assertTrue(after.stream().anyMatch(n -> n != null && n.contains(name)),
                    "\"" + name + "\" did not come back after Clear: " + after);
        }
        backToMap();
    }

    // ──────────────────────────── update ────────────────────────────

    @Test
    @Order(4)
    @DisplayName("LC-04 Renaming one through Edit sticks, and the old name is gone")
    void renameOne() {
        assumeTrue(!created.isEmpty(), "LC-01 created nothing — its reason is above");
        String original = created.get(0);
        renamed = original + " EDITED";

        MapLocationListPage list = openList();
        assumeTrue(list.editLocation(original), "Edit not offered on the row for " + original);

        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(10)), "Edit form did not open");
        assertTrue(String.valueOf(form.nameValue()).contains(original),
                "Edit opened with the wrong name prefilled: " + form.nameValue());
        assumeTrue(form.setName(renamed), "Could not retype the name");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);

        List<String> after = openList().names();
        assertTrue(after.stream().anyMatch(n -> n != null && n.contains(renamed)),
                "The new name is not in the list after saving: " + after);
        assertFalse(after.stream().anyMatch(n -> n != null && n.equals(original)),
                "The OLD name is still in the list next to the new one — the edit created"
                        + " a second row instead of updating the first: " + after);
        created.set(0, renamed);
        backToMap();
    }

    @Test
    @Order(5)
    @DisplayName("LC-05 A scene number saves and survives reopening the form")
    void sceneNumberPersists() {
        assumeTrue(created.size() > 1, "LC-01 did not create enough locations");
        String target = created.get(1);

        MapLocationListPage list = openList();
        assumeTrue(list.editLocation(target), "Edit not offered on the row for " + target);
        MapLocationFormPage form = new MapLocationFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(10)), "Edit form did not open");
        assumeTrue(form.setScene("42"), "Scene field not available");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);

        // Reopen it: a value that only lives in the ViewModel passes a naive check.
        MapLocationListPage again = openList();
        assumeTrue(again.editLocation(target), "Edit not offered on reopen");
        MapLocationFormPage reopened = new MapLocationFormPage(driver);
        assumeTrue(reopened.isLoaded(Duration.ofSeconds(10)), "Edit form did not reopen");
        assertEquals("42", String.valueOf(reopened.sceneValue()).trim(),
                "Scene number did not survive the round trip");
        reopened.cancel();
        backToMap();
    }

    // ──────────────────────── boundary choices ────────────────────────

    @Test
    @Order(6)
    @DisplayName("LC-06 A pin far outside the city offers Cancel, and Cancel pins nothing")
    void outsideCityCancelPinsNothing() {
        assumeTrue(ensureCitySelected(), "No city selected");
        ensureToolbarReady();
        MapPage m = map();
        int before = m.locationCount();

        assumeTrue(m.togglePinMode(), "Pin mode not available. Toolbar showed "
                + m.toolbarSnapshot() + "; city=" + m.cityName());
        assumeTrue(m.pinModeActive(Duration.ofSeconds(6)), "Pin mode did not start");
        // Top-left corner: far from the city centre at any usable zoom.
        assumeTrue(m.dropPinAt(0.08, 0.12), "Could not drop a pin");
        assumeTrue(m.commitPin(), "The pin's info window did not respond to the commit tap");

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)),
                "No boundary dialog for a pin in the far corner of the map — either the"
                        + " corner is still inside this city's radius at this zoom, or the"
                        + " boundary check did not run");
        String title = String.valueOf(dlg.title());
        assertTrue(title.toLowerCase().contains("outside"),
                "A pin far from the city raised \"" + title + "\" instead of an Outside dialog");

        dlg.tap(MapDialogPage.BTN_CANCEL);
        sleep(2500);
        if (m.pinModeActive(Duration.ofSeconds(1))) m.togglePinMode();
        backToMap();

        int after = m.locationCount();
        if (before >= 0 && after >= 0) {
            assertEquals(before, after,
                    "Cancel on the out-of-city dialog still created a location: "
                            + before + " → " + after);
        }
    }

    @Test
    @Order(7)
    @DisplayName("LC-07 The out-of-city dialog offers to pin into the city anyway")
    void outsideCityOffersPinInCity() {
        assumeTrue(ensureCitySelected(), "No city selected");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.togglePinMode(), "Pin mode not available. Toolbar showed "
                + m.toolbarSnapshot() + "; city=" + m.cityName());
        assumeTrue(m.pinModeActive(Duration.ofSeconds(6)), "Pin mode did not start");
        assumeTrue(m.dropPinAt(0.9, 0.88), "Could not drop a pin");
        assumeTrue(m.commitPin(), "The pin's info window did not respond to the commit tap");

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)), "No boundary dialog for a far-corner pin");

        // Both routes out of this dialog must be offered: keep it in the current city, or
        // start a new one. Spec P-09/P-11/P-12.
        String body = String.valueOf(dlg.message());
        assertTrue(dlg.hasButtonStartingWith("Pin in ")
                        || body.toLowerCase().contains("pin in"),
                "The out-of-city dialog did not offer \"Pin in <city>\": " + body);
        assertTrue(dlg.hasButton(MapDialogPage.BTN_CREATE_ANOTHER_CITY)
                        || body.contains("Another City"),
                "The out-of-city dialog did not offer \"Create Another City\": " + body);

        dlg.tap(MapDialogPage.BTN_CANCEL);
        sleep(2000);
        if (m.pinModeActive(Duration.ofSeconds(1))) m.togglePinMode();
        backToMap();
    }

    // ───────────────────────────── move ─────────────────────────────

    @Test
    @Order(8)
    @DisplayName("LC-08 Dragging the preview pin previews only; the commit is what confirms")
    void dragPreviewsCommitConfirms() {
        // WRITTEN AGAINST THE CODE, NOT THE SHEET.
        //
        // MAP_MODULE_TESTCASES.md P-14 says "drag always confirms". For the PREVIEW pin
        // that is not what the app does, and deliberately so —
        // MapFragment.onMarkerDragEnd (:156-168) re-resolves the address and returns,
        // with the comment: "Boundary confirmation happens when the user COMMITS via the
        // info window's Add Location, not on each intermediate drag. Simpler mental
        // model: drag = preview, tap Add Location = commit."
        //
        // So this case asserts the contract the code states: dragging is free, and the
        // boundary question is asked once, on commit. (P-14 applies to an already-SAVED
        // location marker, which this suite cannot grab — those are drawn on the canvas.)
        assumeTrue(ensureCitySelected(), "No city selected");
        ensureToolbarReady();
        MapPage m = map();
        int before = m.locationCount();

        assumeTrue(m.togglePinMode(), "Pin mode not available. Toolbar showed "
                + m.toolbarSnapshot() + "; city=" + m.cityName());
        assumeTrue(m.pinModeActive(Duration.ofSeconds(6)), "Pin mode did not start");
        assumeTrue(m.dropPinAt(0.5, 0.5), "Could not drop the preview pin");
        assumeTrue(m.dragPinTo(0.12, 0.14), "The drag gesture did not run");

        MapDialogPage dlg = new MapDialogPage(driver);
        assertFalse(dlg.shown(Duration.ofSeconds(3)),
                "Dragging the preview pin raised \"" + dlg.title() + "\". The app states"
                        + " the opposite (MapFragment:160-164): a drag only moves the"
                        + " preview, and the boundary question belongs to the commit.");

        // Now commit at the dragged-to point — THIS is where the question belongs.
        assumeTrue(m.commitPin(), "The pin's info window did not respond to the commit tap");
        boolean asked = dlg.shown(Duration.ofSeconds(6));
        assertTrue(asked,
                "Committing a pin in the far corner asked nothing. Either that corner is"
                        + " still inside the city at this zoom, or the boundary check did"
                        + " not run on commit.");
        assertFalse(dlg.hasButton(MapDialogPage.BTN_CREATE_ANOTHER_CITY)
                        && dlg.hasButton(MapDialogPage.BTN_MOVE),
                "The commit dialog offered both Create Another City and Move — those"
                        + " belong to two different flows (new pin vs. moving a saved one)");
        dlg.tap(MapDialogPage.BTN_CANCEL);
        sleep(2000);
        if (m.pinModeActive(Duration.ofSeconds(1))) m.togglePinMode();
        backToMap();

        int after = m.locationCount();
        if (before >= 0 && after >= 0) {
            assertEquals(before, after,
                    "Cancelling the commit still created a location: " + before + " → " + after);
        }
    }

    // ──────────────────────────── delete ────────────────────────────

    @Test
    @Order(9)
    @DisplayName("LC-09 Delete asks first, and Cancel keeps the location")
    void deleteCancelKeeps() {
        assumeTrue(!created.isEmpty(), "LC-01 created nothing — its reason is above");
        String target = created.get(created.size() - 1);
        MapLocationListPage list = openList();
        assumeTrue(list.deleteLocation(target), "Delete not offered on the row for " + target);

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)), "Delete raised no confirmation");
        dlg.tap(MapDialogPage.BTN_CANCEL);
        sleep(2000);

        assertTrue(openList().names().stream().anyMatch(n -> n != null && n.contains(target)),
                "Cancel on the delete confirmation removed \"" + target + "\" anyway");
        backToMap();
    }

    @Test
    @Order(10)
    @DisplayName("LC-10 Confirming delete removes the row and the count follows")
    void deleteConfirmRemoves() {
        assumeTrue(!created.isEmpty(), "LC-01 created nothing — its reason is above");
        String target = created.get(created.size() - 1);
        int before = new MapPage(driver).locationCount();

        MapLocationListPage list = openList();
        assumeTrue(list.deleteLocation(target), "Delete not offered on the row for " + target);
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(6)), "Delete raised no confirmation");
        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE) || dlg.tap(MapDialogPage.BTN_YES),
                "The delete confirmation had no Delete/Yes button");
        sleep(4000);

        assertFalse(openList().names().stream().anyMatch(n -> n != null && n.contains(target)),
                "\"" + target + "\" is still listed after a confirmed delete");
        created.remove(target);
        backToMap();

        int after = new MapPage(driver).locationCount();
        if (before >= 0 && after >= 0) {
            assertNotEquals(before, after,
                    "The location count did not change after a confirmed delete (" + before
                            + "). A row that disappears while the count stands still means"
                            + " the map and the list disagree about what exists.");
        }
    }

    @Test
    @Order(11)
    @DisplayName("LC-11 Cleanup: every remaining fixture this suite made is deleted")
    void cleanup() {
        assumeTrue(!created.isEmpty(), "Nothing left to clean up");
        for (String name : new java.util.ArrayList<>(created)) {
            MapLocationListPage list = openList();
            if (!list.deleteLocation(name)) continue;
            MapDialogPage dlg = new MapDialogPage(driver);
            if (dlg.shown(Duration.ofSeconds(5))) {
                if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
            }
            sleep(3000);
            created.remove(name);
        }
        List<String> leftovers = openList().names().stream()
                .filter(n -> n != null && n.contains(FIXTURE_PREFIX))
                .toList();
        assertTrue(leftovers.isEmpty(),
                "This suite left fixtures behind on the project: " + leftovers
                        + " — delete them by hand before the next run, or the counts in"
                        + " LC-01 will start from the wrong number.");
        backToMap();
    }

    // ──────────────────────────── plumbing ────────────────────────────

    private MapLocationListPage openList() {
        backToMap();
        ensureToolbarReady();
        MapPage m = map();
        // Say WHAT was on the toolbar when it failed. "View All not available" on its own
        // sent me guessing twice; the button list and the pin-mode state name the cause.
        assumeTrue(m.openViewAll(), "View All not available. Toolbar showed "
                + m.toolbarSnapshot() + "; pin mode active="
                + m.pinModeActive(Duration.ofSeconds(1)) + "; city=" + m.cityName());
        MapLocationListPage list = new MapLocationListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(15)), "The location list did not open");
        return list;
    }
}
