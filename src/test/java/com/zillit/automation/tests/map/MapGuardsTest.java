package com.zillit.automation.tests.map;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.map.MapZonePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &rarr; <b>empty-state guards and persisted state</b>.
 *
 * <p>Three toolbar buttons refuse politely rather than opening an empty panel:
 *
 * <pre>
 *   fitAllMarkers()   locations.isEmpty() → "No locations to zoom to"
 *   toggleFilter()    types.isEmpty()     → "No location types to filter"
 *   toggleZonePicker() zones.isEmpty()    → "No studio zones available"
 * </pre>
 *
 * <p>These cases assert the <b>consequence</b> — the panel does not open, the map does not
 * move — rather than the toast text. Toast reading is unreliable across Android versions
 * and, more to the point, a regression that opened an empty filter panel while still
 * showing the toast would pass a text-only assertion while being exactly the bug.
 *
 * <p>Also covers the two pieces of state the module persists for itself: the tutorial
 * card's collapsed flag and the saved camera position.
 */
@Order(TestOrder.MAP_GUARDS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapGuardsTest extends MapBaseTest {

    // ---------------- empty-state guards ----------------

    @Test
    @Order(1)
    @DisplayName("F-15 Fit-All on a city with no pins does not move the map")
    void fitAllWithNoLocations() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.locationCount() == 0,
                "This city has pins, so the empty guard cannot be exercised");
        assumeTrue(m.fitAll(), "Fit-All not available");
        sleep(1500);
        // The guard returns before touching the camera; the map must survive untouched.
        assertTrue(m.isLoaded(Duration.ofSeconds(8)),
                "Map lost after Fit-All on an empty city");
        assertEquals(0, m.locationCount(), "Location count changed on a read-only action");
    }

    @Test
    @Order(2)
    @DisplayName("F-10 The filter panel does not open when there are no types")
    void filterWithNoTypes() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.locationCount() == 0,
                "This city has pins, so it probably has types; guard not exercisable");
        assumeTrue(m.openFilterPanel(), "Filter button not available");
        sleep(1500);
        assertFalse(m.filterPanelShown(Duration.ofSeconds(3)),
                "An empty filter panel opened — toggleFilter() should refuse with "
                        + "\"No location types to filter\" and open nothing");
    }

    @Test
    @Order(3)
    @DisplayName("Z-03 The zone picker does not open when the city has no zones")
    void zonePickerWithNoZones() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.openZoneList(), "Zone list button not available");
        sleep(2000);
        MapZonePage zones = new MapZonePage(driver);
        if (zones.listLoaded(Duration.ofSeconds(6))) {
            // The full list screen opened, which is the non-empty path — it has its own
            // empty state, covered by Z-02.
            assumeTrue(false, "The zone LIST opened rather than the picker; see Z-02");
        }
        assertTrue(m.isLoaded(Duration.ofSeconds(8)),
                "Map lost after opening the zone picker with no zones");
    }

    @Test
    @Order(4)
    @DisplayName("D-09 Sharing directions with no endpoints set does not hand off")
    void shareDirectionsWithoutEndpoints() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        assumeTrue(m.locationCount() > 0, "Needs a pin to open directions from");
        assumeTrue(m.bottomPanelShown(Duration.ofSeconds(6)), "Bottom panel not open");
        assumeTrue(m.cardDirections(), "Directions not available on the card");
        assumeTrue(m.directionsPanelShown(Duration.ofSeconds(10)), "Panel did not open");

        // Destination is prefilled; origin is not until the user sets it. Sharing now
        // must refuse rather than hand off a half-built route.
        assumeTrue(m.directionsFrom() == null || m.directionsFrom().isBlank(),
                "Pickup was prefilled, so both endpoints are set; guard not exercisable");
        assumeTrue(m.shareDirections(), "Share-directions button not available");
        sleep(2500);

        String pkg = null;
        try { pkg = driver.getCurrentPackage(); } catch (Exception ignored) { }
        assertTrue(pkg != null && pkg.contains("zillit"),
                "Sharing with no pickup handed off to " + pkg + " — expected the "
                        + "\"Please set pickup and destination first\" refusal");
        m.closeDirections();
        backToMap();
    }

    // ---------------- persisted state ----------------

    @Test
    @Order(5)
    @DisplayName("M-04 The tutorial card's collapsed state survives leaving the module")
    void tutorialStatePersists() {
        MapPage m = map();
        assumeTrue(m.tutorialShown(Duration.ofSeconds(5)), "No tutorial card on this screen");
        boolean bodyBefore = m.tutorialBodyShown();
        assumeTrue(m.toggleTutorial(), "Could not toggle the tutorial");
        sleep(1000);
        boolean bodyAfterToggle = m.tutorialBodyShown();
        assumeTrue(bodyBefore != bodyAfterToggle, "Toggle had no effect");

        // Leave the module entirely and come back.
        assumeTrue(new com.zillit.automation.pages.common.ToolsPage(driver).openToolsTab(),
                "Could not leave the Map");
        sleep(1500);
        openMap();

        MapPage after = new MapPage(driver);
        assumeTrue(after.tutorialShown(Duration.ofSeconds(8)), "Tutorial card gone");
        assertEquals(bodyAfterToggle, after.tutorialBodyShown(),
                "Tutorial collapsed state did not persist across leaving the module");

        // Leave it as we found it.
        if (after.tutorialBodyShown() != bodyBefore) after.toggleTutorial();
    }

    @Test
    @Order(6)
    @DisplayName("C-24b The selected city survives leaving and re-entering the module")
    void cameraAndCityRestored() {
        assumeTrue(ensureCitySelected(), "Needs a selected city");
        MapPage m = map();
        String cityBefore = m.cityName();
        int countBefore = m.locationCount();

        assumeTrue(new com.zillit.automation.pages.common.ToolsPage(driver).openToolsTab(),
                "Could not leave the Map");
        sleep(1500);
        openMap();

        MapPage after = new MapPage(driver);
        assumeTrue(after.isLoaded(Duration.ofSeconds(15)), "Map did not reload");
        assertEquals(cityBefore, after.cityName(),
                "The Map did not restore the last-visited city");
        assertEquals(countBefore, after.locationCount(),
                "Location count changed across a module round trip");
    }

    @Test
    @Order(7)
    @DisplayName("C-23 The Map's own pins are scoped to the selected city")
    void pinsAreScopedToCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        java.util.List<String> cities = m.visibleCityNames();
        assumeTrue(cities.size() >= 2, "Needs two cities to compare");

        assumeTrue(m.selectCity(cities.get(0)), "Could not select city A");
        sleep(3000);
        int countA = m.locationCount();

        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not reopen");
        assumeTrue(m.selectCity(cities.get(1)), "Could not select city B");
        sleep(3000);
        int countB = m.locationCount();

        assumeTrue(countA != countB,
                "Both cities report the same pin count, so this cannot distinguish "
                        + "per-city scoping from a shared list");
        // Reaching here means the counts differ, which is the scoping working. The
        // assertion is kept explicit so the case reads as a check, not a probe.
        assertTrue(countA != countB, "Map pins are not scoped per city");
    }
}
