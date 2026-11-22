package com.zillit.automation.tests.map;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapCityFormPage;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP — a SECOND city, and everything that only a second city can prove.
 *
 * <p>Switching cities, city search and per-city pin scoping all need two cities to mean
 * anything, and the project has one. Those cases have been skipping all along with
 * "Needs at least two cities" — written, never exercised.
 *
 * <p><b>How a city gets created here.</b> Not through the city form's own search: that
 * field is a Google Places launcher ({@code CityFormFragment:249-250}) and Places does not
 * work on this build. The route that does work is the app's own out-of-city flow — pin a
 * point outside the current city, and the boundary dialog offers <b>Create Another
 * City</b>. {@code MapFragment.launchCreateAnotherCity} (:908) carries the reverse-geocoded
 * name, address, lat and lng into the form as arguments; {@code CityFormFragment:67-72}
 * reads them straight into its fields, and save only demands a name and non-zero
 * coordinates (:267-268). Places is never involved.
 *
 * <p><b>Kept small on purpose.</b> The long Map suites lose the device before they finish
 * on this emulator, and a case that never completes proves nothing. Six cases, one
 * fixture, cleaned up at the end.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.MAP_CITY_MULTI)
class MapCityMultiTest extends MapBaseTest {

    /** The city that was already there, to switch back to. */
    private String firstCity;
    /** The city this suite created, and must delete again. */
    private String secondCity;

    @Test
    @Order(1)
    @DisplayName("MC-01 A second city is created from the out-of-city pin dialog")
    void createSecondCity() {
        assumeTrue(ensureCitySelected(), "No city on this project to start from");
        ensureToolbarReady();
        MapPage m = map();
        firstCity = String.valueOf(m.cityName()).trim();
        assumeTrue(!firstCity.isEmpty() && !"Select a City".equals(firstCity),
                "No city is selected, so there is nothing to be outside of");

        assumeTrue(m.togglePinMode(), "Pin mode not available. Toolbar showed "
                + m.toolbarSnapshot());
        assumeTrue(m.pinModeActive(Duration.ofSeconds(6)), "Pin mode did not start");
        // Far corner: well outside any sane city radius at this zoom.
        assumeTrue(m.dropPinAt(0.08, 0.12), "Could not drop a pin");
        assumeTrue(m.commitPin(), "The pin's info window did not respond to the commit tap");

        MapDialogPage dlg = new MapDialogPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(8)),
                "No boundary dialog for a pin in the far corner — either that corner is"
                        + " still inside \"" + firstCity + "\" at this zoom, or the"
                        + " boundary check did not run");
        assumeTrue(dlg.hasButton(MapDialogPage.BTN_CREATE_ANOTHER_CITY),
                "The out-of-city dialog did not offer \"Create Another City\". Buttons: "
                        + dlg.buttonLabels());
        assertTrue(dlg.tap(MapDialogPage.BTN_CREATE_ANOTHER_CITY),
                "\"Create Another City\" could not be tapped");
        sleep(3000);

        MapCityFormPage form = new MapCityFormPage(driver);
        assumeTrue(form.isLoaded(Duration.ofSeconds(15)), "The city form did not open");

        // THE POINT OF THIS ROUTE: the form arrives already filled. If it does not, the
        // city cannot be saved at all on this build, because the search field is a dead
        // Places launcher — so an empty form here is a real finding, not a skip.
        String prefilled = String.valueOf(form.cityNameShown()).trim();
        assertFalse(prefilled.isEmpty() || "null".equals(prefilled),
                "The form opened from \"Create Another City\" with NO name. It is prefilled"
                        + " from the reverse-geocoded pin (CityFormFragment:67-72), and"
                        + " without it save is refused (\"" + MapCityFormPage.ERR_NO_NAME + "\")"
                        + " and no city can be created on this build at all.");
        String lat = String.valueOf(form.latShown());
        assertFalse(lat.isEmpty() || "null".equals(lat) || lat.startsWith("0.0"),
                "The form has a name but no coordinates (lat=\"" + lat + "\"). Save checks"
                        + " lat/lng too (:268), so this would be rejected.");

        secondCity = prefilled;
        assertTrue(form.save(), "Save was not available on the prefilled city form");
        sleep(5000);
        backToMap();
    }

    @Test
    @Order(2)
    @DisplayName("MC-02 The city panel now lists both cities")
    void panelListsBoth() {
        assumeTrue(secondCity != null, "MC-01 did not create a city");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        assertTrue(cities.stream().anyMatch(c -> c != null && c.contains(firstCity)),
                "The original city \"" + firstCity + "\" is gone from the panel: " + cities);
        assertTrue(cities.stream().anyMatch(c -> c != null && c.contains(secondCity)),
                "The new city \"" + secondCity + "\" saved but is not in the panel: " + cities);
        m.closeCityPanel();
    }

    @Test
    @Order(3)
    @DisplayName("MC-03 Switching city changes the name and the location count")
    void switchingCityFollows() {
        assumeTrue(secondCity != null, "MC-01 did not create a city");
        ensureToolbarReady();
        MapPage m = map();
        String beforeName = String.valueOf(m.cityName()).trim();
        int beforeCount = m.locationCount();

        assumeTrue(m.openCityPanel(), "City panel did not open");
        String target = beforeName.contains(secondCity) ? firstCity : secondCity;
        assumeTrue(m.selectCity(target), "Could not select \"" + target + "\"");
        sleep(3500);

        String afterName = String.valueOf(m.cityName()).trim();
        assertTrue(afterName.contains(target),
                "The header still reads \"" + afterName + "\" after switching to \""
                        + target + "\"");
        // The brand-new city has no locations; the original one does. If the count does
        // not follow the switch, the map is showing one city's data under another's name.
        int afterCount = m.locationCount();
        if (beforeCount >= 0 && afterCount >= 0 && beforeCount > 0) {
            assertNotEquals(beforeCount, afterCount,
                    "The location count stayed at " + beforeCount + " across a switch from \""
                            + beforeName + "\" to \"" + target + "\". The new city has no"
                            + " locations of its own, so an unchanged count means the pins"
                            + " did not reload.");
        }
    }

    @Test
    @Order(4)
    @DisplayName("MC-04 The Map's pins stay scoped to the selected city")
    void pinsAreScopedToCity() {
        assumeTrue(secondCity != null, "MC-01 did not create a city");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "City panel did not open");
        assumeTrue(m.selectCity(secondCity), "Could not select the new city");
        sleep(3500);

        // A city created moments ago owns nothing. Anything on its carousel belongs to
        // another city — which is the defect C-23 exists to catch.
        List<String> shown = m.carouselNames();
        assertTrue(shown.isEmpty(),
                "\"" + secondCity + "\" was created in this run and owns no locations, yet"
                        + " the map shows " + shown + " under it. Those belong to another"
                        + " city.");
    }

    @Test
    @Order(5)
    @DisplayName("MC-05 City search filters the panel down to one")
    void citySearchFilters() {
        assumeTrue(secondCity != null, "MC-01 did not create a city");
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "City panel did not open");
        assumeTrue(m.visibleCityNames().size() >= 2, "Fewer than two cities to filter");

        assumeTrue(m.searchCities(secondCity), "Could not type in the city search");
        sleep(1500);
        List<String> filtered = m.visibleCityNames();
        assertTrue(filtered.stream().anyMatch(c -> c != null && c.contains(secondCity)),
                "Searching for \"" + secondCity + "\" hid it: " + filtered);
        assertFalse(filtered.stream().anyMatch(c -> c != null && c.contains(firstCity)),
                "Searching for \"" + secondCity + "\" still lists \"" + firstCity + "\": "
                        + filtered + " — the filter is not narrowing anything");
        m.searchCities("");
        sleep(1000);
        m.closeCityPanel();
    }

    @Test
    @Order(6)
    @DisplayName("MC-06 Cleanup: the city this suite created is deleted")
    void deleteSecondCity() {
        assumeTrue(secondCity != null, "MC-01 did not create a city");
        ensureToolbarReady();
        MapPage m = map();
        // Switch away first: deleting the selected city leaves the Map in a state this
        // suite is not here to test.
        if (m.openCityPanel() && firstCity != null) {
            m.selectCity(firstCity);
            sleep(2500);
        }
        assumeTrue(m.openCityPanel(), "City panel did not open");
        // The panel's rows come from item_city.xml (btnCityDelete), NOT the city-list
        // screen's item_city_card.xml (btnDelete) — two layouts, two ids, same idea.
        // MapCityFormPage owns the panel-row actions.
        MapCityFormPage panel = new MapCityFormPage(driver);
        assumeTrue(panel.deleteCity(secondCity),
                "Delete not offered on the panel row for " + secondCity);

        MapDialogPage dlg = new MapDialogPage(driver);
        if (dlg.shown(Duration.ofSeconds(6))) {
            if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
        }
        sleep(4000);

        assumeTrue(m.openCityPanel(), "City panel did not reopen");
        assertFalse(m.visibleCityNames().stream()
                        .anyMatch(c -> c != null && c.contains(secondCity)),
                "\"" + secondCity + "\" is still in the panel after a confirmed delete —"
                        + " it will be left on the project for the next run");
        m.closeCityPanel();
        secondCity = null;
    }
}
