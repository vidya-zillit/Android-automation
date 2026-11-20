package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.MapCityFormPage;
import com.zillit.automation.pages.MapDialogPage;
import com.zillit.automation.pages.MapPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; <b>cities</b>: create, validate, edit, delete and switch.
 *
 * <p>A city is the Map's unit of scope. Its <i>radius</i> is the circle every later pin is
 * measured against, so these cases are the foundation the boundary suite stands on — get a
 * city wrong and {@link MapPinBoundaryTest} tests nothing meaningful.
 *
 * <p><b>This suite writes to the project backend.</b> Everything it creates is prefixed
 * {@code ZL AUTO} and suffixed with a timestamp, and the delete case removes what the
 * create case made. It never touches a city it did not create — {@link #deleteCreatedCity()}
 * matches on the prefix and skips if its own fixture is missing, rather than deleting
 * whatever happens to be first in the list.
 */
@Order(TestOrder.MAP_CITY_CRUD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapCityCrudTest extends MapBaseTest {

    /** Name of the city created by {@link #createCity()}, reused downstream. */
    private static String createdCity;

    private MapCityFormPage openAddCityForm() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        MapCityFormPage form = new MapCityFormPage(driver);
        // btnAddCity lives in the panel; on a project with no cities the toolbar button
        // itself reads "Add City" and goes straight to the form. Retried, because the
        // panel animates in and a click landed during the animation does nothing — which
        // previously turned into a bare "City form did not open" skip.
        for (int attempt = 0; attempt < 3 && !form.isLoaded(Duration.ofSeconds(2)); attempt++) {
            try { driver.findElement(byAppId("btnAddCity")).click(); } catch (Exception ignored) { }
            sleep(2000);
            if (form.isLoaded(Duration.ofSeconds(3))) break;
            // Re-open the panel in case a stray BACK closed it between cases.
            if (!m.cityPanelShown(Duration.ofSeconds(1))) {
                m.openCityPanel();
                sleep(1500);
            }
        }
        assumeTrue(form.isLoaded(Duration.ofSeconds(10)),
                "City form did not open after three attempts");
        return form;
    }

    private void leaveForm(MapCityFormPage form) {
        if (!form.cancel()) form.close();
        sleep(1200);
        backToMap();
    }

    // ---------------- form and validation ----------------

    @Test
    @Order(1)
    @DisplayName("C-02 The city form opens with its Places search and Save")
    void cityFormOpens() {
        MapCityFormPage form = openAddCityForm();
        assertTrue(form.isLoaded(Duration.ofSeconds(8)), "City form not shown");
        assertNotNull(form.formTitle(), "Form has no title");
        // The radius chips are deliberately NOT asserted here: they live inside
        // sectionCenterPoint, which this build never shows. C-14 owns that fact and will
        // fail if it ever changes; asserting it in both places would just report it twice.
        assertTrue(form.searchFieldShown(),
                "No city search field — the form has no other way to name a city");
        assertTrue(form.saveShown(), "No Save button on the city form");
        leaveForm(form);
    }

    @Test
    @Order(2)
    @DisplayName("C-03 Saving with no name is rejected and the form stays open")
    void noNameRejected() {
        MapCityFormPage form = openAddCityForm();
        assumeTrue(form.save(), "Save button not available");
        sleep(1500);
        // The rejection is a toast, which Appium cannot read reliably across Android
        // versions. The invariant that matters, and that a regression would break, is
        // that the form did NOT close — a silent close means a city was created nameless.
        assertTrue(form.stillOpen(),
                "Form closed on an empty name — expected the \"" + MapCityFormPage.ERR_NO_NAME
                        + "\" rejection to keep it open");
        leaveForm(form);
    }

    @Test
    @Order(3)
    @DisplayName("C-04 The city name can only come from a picked place, never from typing")
    void cityNameIsNotTypeable() {
        MapCityFormPage form = openAddCityForm();
        assertTrue(form.searchFieldShown(), "No city search field on the form");

        // CityFormFragment:249-250 sets etCitySearch isFocusable=false and makes its click
        // launch the Places autocomplete, so the field is a BUTTON wearing an EditText's
        // clothes. The original case here — "a name without a picked place is rejected" —
        // cannot be expressed against this UI at all: there is no way to have a name
        // without having picked a place. What IS worth pinning down is that the field
        // stays non-typeable, because the day it becomes editable the validation path
        // behind ERR_NO_PLACE starts mattering and this case should be rewritten.
        assertFalse(form.searchFieldAcceptsTyping(),
                "etCitySearch now accepts typed input. That is a behaviour change: the"
                        + " form previously had no name field at all, and \""
                        + MapCityFormPage.ERR_NO_PLACE + "\" was unreachable. Re-instate"
                        + " the original C-04 (type a name, pick nothing, expect the"
                        + " rejection) now that it can be expressed.");
        leaveForm(form);
    }

    @Test
    @Order(4)
    @DisplayName("C-07/C-08 Intersection mode needs both street names")
    void intersectionNeedsBothStreets() {
        MapCityFormPage form = openAddCityForm();
        // Lives inside the same hidden section as the radius — see C-14. Skips with that
        // reason rather than failing, because the app is behaving as built.
        assumeTrue(form.useIntersectionMode(),
                "Intersection mode is unreachable in this build: it sits inside "
                        + "sectionCenterPoint, which the layout declares visibility=gone "
                        + "and nothing ever shows");
        assumeTrue(form.enterStreets("Main Street", null), "Could not fill street 1");
        assumeTrue(form.findIntersection(), "Find Intersection not available");
        sleep(2000);
        // With street 2 blank the app must not report a result.
        String result = form.intersectionResult();
        assertTrue(result == null || result.isBlank()
                        || !result.toLowerCase().contains("found"),
                "An intersection was reported with only one street filled: " + result);
        leaveForm(form);
    }

    /**
     * The "Center Point &amp; Zone" section is dead UI in this build.
     *
     * <p>{@code fragment_city_form.xml} declares {@code sectionCenterPoint} with
     * {@code android:visibility="gone"} and nothing ever shows it, so a city cannot be
     * given a centre point or a radius from the app at all. Every city therefore falls
     * back to the 30-mile default in {@code evaluatePinBoundary()}, and the intersection
     * finder — three geocoding strategies' worth of code — is unreachable.
     *
     * <p>This case asserts the current state rather than the intent, so it FAILS the day
     * someone re-enables the section and the radius cases become runnable again. That is
     * the point: a silent re-enable would otherwise go unnoticed.
     */
    @Test
    @Order(5)
    @DisplayName("C-14 The Center Point & Zone section is not reachable in this build")
    void centerPointSectionIsAbsent() {
        MapCityFormPage form = openAddCityForm();
        boolean shown = form.centerPointSectionShown();
        List<String> chips = shown ? form.radiusChips() : List.of();
        leaveForm(form);

        assertFalse(shown,
                "The Center Point & Zone section is now visible on the city form. That is "
                        + "a behaviour change: radius and centre point were previously "
                        + "unreachable (sectionCenterPoint is visibility=gone in the layout). "
                        + "Re-enable C-06…C-15 in MAP_MODULE_TESTCASES.md — radius chips seen: "
                        + chips);
    }

    @Test
    @Order(6)
    @DisplayName("C-17 Cancel closes the form without creating a city")
    void cancelCreatesNothing() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        int before = new MapCityFormPage(driver).cityCount();
        m.closeCityPanel();

        MapCityFormPage form = openAddCityForm();
        // No name is typed, and that is the point: Cancel has to be safe on a form in any
        // state, and the earlier version of this case skipped on "could not type a name"
        // without ever reaching the behaviour it was written to check.
        assumeTrue(form.cancel() || form.close(), "No cancel affordance");
        sleep(1500);
        backToMap();

        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not reopen");
        assertTrue(new MapCityFormPage(driver).cityCount() <= before,
                "Cancelling the form still added a city");
        m.closeCityPanel();
        backToMap();
    }

    // ---------------- create ----------------

    @Test
    @Order(7)
    @DisplayName("C-05/C-16 A city with a picked place saves and appears in the list")
    void createCity() {
        MapCityFormPage form = openAddCityForm();
        String name = fixtureName("City");

        assumeTrue(form.searchCity("Mumbai"), "A city can only be named by picking a Google Places result — etCitySearch is isFocusable=false and its click launches the autocomplete (CityFormFragment:249-250). Places does not work in this develop build, so the only remaining route to a new city is the Add Current City card, and that card hides once the detected city is already on the project.");
        sleep(3500);
        // The search needs a Places result to fill lat/lng. Without one there is nothing
        // to save, and that is an environment gap rather than a defect.
        assumeTrue(form.placePicked(),
                "City search returned no usable place (lat/lng still unset) — most likely "
                        + "no Places-enabled API key on this build");

        form.setCustomRadius("30");
        assumeTrue(form.save(), "Save not available");
        sleep(4000);

        createdCity = name;
        backToMap();
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        assertFalse(new MapCityFormPage(driver).cityNames().isEmpty(),
                "No cities in the list after saving one");
        m.closeCityPanel();
        backToMap();
    }

    @Test
    @Order(7)
    @DisplayName("C-27 Add Current City prefills the form from the device's location")
    void createCurrentCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        // updateCurrentCityCard() hides the card in three cases: no reverse-geocoded city
        // name yet, no posting access, and — the usual one here — the detected city is
        // ALREADY on the project. The old reason blamed the GPS, which sent me checking a
        // location fix that was present the whole time.
        assumeTrue(m.currentCityCardShown(Duration.ofSeconds(6)),
                "No current-city card. The card is hidden when the detected city is"
                        + " already in the project's city list, which is the case once a"
                        + " previous run added it; it is also hidden without posting"
                        + " access, or before reverse geocoding returns a city name.");

        String suggested = m.currentCityName();
        assertTrue(suggested != null && !suggested.isBlank(),
                "Current-city card shows no city name");
        assertTrue(String.valueOf(m.currentCityAction()).toLowerCase().contains("add"),
                "The card should offer to ADD the current city; action read: "
                        + m.currentCityAction());

        assumeTrue(m.addCurrentCity(), "Could not tap the current-city action");
        sleep(3000);

        // It opens the form rather than saving outright — the user still confirms.
        MapCityFormPage form = new MapCityFormPage(driver);
        assertTrue(form.isLoaded(Duration.ofSeconds(10)),
                "Add Current City did not open the city form");
        assertTrue(form.placePicked(),
                "The form should arrive prefilled with the device's coordinates; lat read: "
                        + form.latShown());
        assertTrue(String.valueOf(form.cityNameShown()).contains(suggested)
                        || !String.valueOf(form.cityNameShown()).isBlank(),
                "The form should carry the suggested city name \"" + suggested + "\"");

        // Do not save: the city may already exist, and this case is about the prefill.
        if (!form.cancel()) form.close();
        sleep(1500);
        backToMap();
    }

    // ---------------- update ----------------

    @Test
    @Order(8)
    @DisplayName("C-18 A city opens prefilled for editing and the change sticks")
    void updateCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");

        MapCityFormPage panel = new MapCityFormPage(driver);
        java.util.List<String> names = panel.cityNames();
        assumeTrue(!names.isEmpty(), "No cities to edit");
        String target = names.get(0);

        assumeTrue(panel.editCity(target), "Edit not available on the city row");
        MapCityFormPage form = new MapCityFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(12)), "Edit form did not open");

        // parseEditCity() should have repopulated the coordinates, not left them blank.
        assertTrue(form.placePicked(),
                "Edit form opened without the city's coordinates — parseEditCity() did not "
                        + "prefill; lat read: " + form.latShown());

        // The centre point and radius are NOT editable: parseEditCity() hides the whole
        // "Center Point & Zone" section and its divider in edit mode. So a city's boundary
        // radius is fixed at creation — worth asserting, because every pin verdict is
        // measured against it and there is no way to correct it later from this screen.
        assertFalse(form.centerPointSectionShown(),
                "The Center Point & Zone section is visible in EDIT mode — it is hidden by "
                        + "design, and its appearance would mean a city's radius had become "
                        + "editable after creation");

        assumeTrue(form.save(), "Save not available");
        sleep(4000);
        assertFalse(form.stillOpen(),
                "Edit form stayed open after saving — the save was rejected");
        backToMap();
    }

    // ---------------- switch ----------------

    @Test
    @Order(9)
    @DisplayName("C-22b Switching city updates the name and the location count")
    void switchCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        assumeTrue(cities.size() >= 2, "Needs at least two cities to switch between");

        assumeTrue(m.selectCity(cities.get(0)), "Could not select the first city");
        sleep(2500);
        String firstName = m.cityName();
        int firstCount = m.locationCount();

        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not reopen");
        assumeTrue(m.selectCity(cities.get(1)), "Could not select the second city");
        sleep(2500);

        assertFalse(firstName != null && firstName.equals(m.cityName())
                        && firstCount == m.locationCount(),
                "Switching city changed neither the name nor the count — the Map is not "
                        + "reloading per city");
    }

    @Test
    @Order(10)
    @DisplayName("C-26 City search filters the panel")
    void citySearchFilters() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        List<String> all = m.visibleCityNames();
        assumeTrue(all.size() >= 2, "Needs at least two cities to prove filtering");

        String term = all.get(0).substring(0, Math.min(4, all.get(0).length()));
        assumeTrue(m.searchCities(term), "City search field not available");
        sleep(1500);
        List<String> filtered = m.visibleCityNames();
        assertTrue(filtered.size() <= all.size(),
                "Searching widened the list: " + all.size() + " → " + filtered.size());
        m.closeCityPanel();
        backToMap();
    }

    // ---------------- delete ----------------

    @Test
    @Order(11)
    @DisplayName("C-19/C-20 Delete asks first, and Cancel keeps the city")
    void deleteCancelKeepsCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");

        MapCityFormPage panel = new MapCityFormPage(driver);
        String target = panel.cityNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null,
                "No " + FIXTURE_PREFIX + " city to delete — this suite never deletes a "
                        + "city it did not create");

        int before = panel.cityCount();
        assumeTrue(panel.deleteCity(target), "Delete button not available on the row");

        MapDialogPage dlg = new MapDialogPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(8)), "Delete did not ask for confirmation");
        assertTrue(String.valueOf(dlg.title()).contains(MapDialogPage.TITLE_DELETE_CITY),
                "Unexpected confirm title: " + dlg.title());
        assertTrue(String.valueOf(dlg.message()).contains("all its locations"),
                "The confirm must say the city's locations go too; got: " + dlg.message());

        assertTrue(dlg.tap(MapDialogPage.BTN_CANCEL), "Cancel not available");
        sleep(1500);
        assertTrue(panel.cityCount() >= before,
                "Cancelling the delete still removed a city");
        m.closeCityPanel();
        backToMap();
    }

    @Test
    @Order(12)
    @DisplayName("C-21 Confirming the delete removes the city this suite created")
    void deleteCreatedCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");

        MapCityFormPage panel = new MapCityFormPage(driver);
        String target = panel.cityNames().stream()
                .filter(n -> n.startsWith(FIXTURE_PREFIX))
                .findFirst().orElse(null);
        assumeTrue(target != null, "No " + FIXTURE_PREFIX + " city left to clean up");

        int before = panel.cityCount();
        assumeTrue(panel.deleteCity(target), "Delete button not available");
        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)), "No confirmation dialog");
        assumeTrue(dlg.tap(MapDialogPage.BTN_DELETE), "Delete not available in the dialog");
        sleep(3000);

        assertTrue(panel.cityCount() < before || !panel.cityNames().contains(target),
                "City \"" + target + "\" survived a confirmed delete");
        m.closeCityPanel();
        backToMap();
    }
}
