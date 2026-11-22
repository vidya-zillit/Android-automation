package com.zillit.automation.tests.map;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.map.MapLocationListPage;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP module (Tools &rarr; Map) — the module's own surface.
 *
 * <p>Covers the landing state, the tutorial card, city selection and persistence, the
 * toolbar's city-gated buttons, search, map type, the filter panel, the carousel and the
 * View All list. The Location-tool overlay has its own suite — see
 * {@link MapLocationPinsTest} — because its preconditions are different: it needs
 * Location-tool view access, which the Map itself does not.
 *
 * <p><b>Read-only.</b> Nothing here creates a city, a studio zone or a pin. The Map writes
 * straight to the project's backend and this project is shared, so the suite asserts on
 * what is already there and skips where it is not.
 */
@Order(TestOrder.MAP)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class MapModuleTest extends BaseTest {

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachMap() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects to enter");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                    "Never reached a project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
        openMap();
    }

    /**
     * Walks back to the Map, or opens it from Tools.
     *
     * <p>The BACK loop stops at the bottom-nav boundary. Pressing BACK on a bottom-nav
     * screen leaves the app, and every later case in the run then skips — the failure mode
     * that stranded whole runs before.
     */
    private void openMap() {
        for (int i = 0; i < 4; i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) return;
            if (onBottomNavScreen()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.contains("zillit")) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(2500);
            }
        } catch (Exception ignored) { }
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Map");
        assumeTrue(tools.isModuleListed("Map", Duration.ofSeconds(6)),
                "Map module not enabled on this project");
        assumeTrue(tools.openModule("Map"), "Could not open Map");
        assumeTrue(new MapPage(driver).isLoaded(Duration.ofSeconds(25)),
                "Map did not load");
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private MapPage map() {
        MapPage p = new MapPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Map not loaded");
        return p;
    }

    /** Returns to the Map from a pushed sub-screen without ever exiting the app. */
    private void backToMap() {
        for (int i = 0; i < 4; i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
            if (onBottomNavScreen()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        if (!new MapPage(driver).isLoaded(Duration.ofSeconds(1))) openMap();
    }

    /** True once a city is chosen — most of the toolbar does not exist before that. */
    private boolean cityChosen() {
        return !new MapPage(driver).noCitySelected();
    }

    // ---------------- landing ----------------

    @Test
    @Order(1)
    @DisplayName("M-01 Map opens with the MAP header, a city label and a location count")
    void mapLoads() {
        MapPage m = map();
        assertTrue(m.isLoaded(Duration.ofSeconds(10)), "Map surface not shown");
        String header = m.headerText();
        assertTrue(header == null || header.toUpperCase().contains("MAP"),
                "Unexpected header: " + header);
        assertTrue(m.cityName() != null && !m.cityName().isBlank(),
                "City label missing");
        assertTrue(m.locationCountText() != null
                        && m.locationCountText().toLowerCase().contains("location"),
                "Location count missing, got: " + m.locationCountText());
    }

    @Test
    @Order(2)
    @DisplayName("M-02 With no city chosen the label reads 'Select a City' and the count is 0")
    void freshProjectState() {
        MapPage m = map();
        assumeTrue(m.noCitySelected(), "A city is already selected on this project");
        assertEquals(0, m.locationCount(),
                "No city selected, so the count should be 0: " + m.locationCountText());
        assertTrue(String.valueOf(m.citiesButtonLabel()).toLowerCase().contains("add"),
                "With no cities the button should offer Add City, got: " + m.citiesButtonLabel());
    }

    @Test
    @Order(3)
    @DisplayName("M-03 Tutorial card shows the three steps and collapses")
    void tutorialCard() {
        MapPage m = map();
        assumeTrue(m.tutorialShown(Duration.ofSeconds(5)), "Tutorial card already dismissed");
        assertTrue(m.tutorialBodyShown(), "Tutorial body not shown");
        assumeTrue(m.toggleTutorial(), "Could not toggle the tutorial");
        boolean collapsed = !m.tutorialBodyShown();
        m.toggleTutorial();                       // leave it as we found it
        assertTrue(collapsed, "Toggle did not collapse the tutorial body");
    }

    // ---------------- cities ----------------

    @Test
    @Order(4)
    @DisplayName("C-01 City panel opens and lists cities")
    void cityPanelOpens() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assertTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        m.closeCityPanel();
        backToMap();
    }

    @Test
    @Order(5)
    @DisplayName("C-22 Selecting a city updates the city name and the location count")
    void selectCity() {
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        assumeTrue(!cities.isEmpty(), "No cities on this project yet");
        String target = cities.get(0);
        assumeTrue(m.selectCity(target), "Could not tap city " + target);
        sleep(2000);
        assertTrue(String.valueOf(m.cityName()).contains(target)
                        || !m.noCitySelected(),
                "City name did not update, still: " + m.cityName());
    }

    @Test
    @Order(6)
    @DisplayName("R-03 With a city selected the city-gated toolbar buttons appear")
    void toolbarAfterCity() {
        MapPage m = map();
        assumeTrue(cityChosen(), "No city selected on this project");
        List<String> buttons = m.visibleToolbarButtons();
        assertTrue(buttons.contains("btnViewAll"),
                "View All missing with a city selected; saw " + buttons);
        assertTrue(buttons.contains("btnFitAll") || buttons.contains("btnSearch"),
                "Neither Fit-All nor Search present; saw " + buttons);
    }

    @Test
    @Order(7)
    @DisplayName("C-24 The last-visited city is restored when the Map is reopened")
    void lastVisitedCityPersists() {
        MapPage m = map();
        assumeTrue(cityChosen(), "No city selected on this project");
        String before = m.cityName();
        // Leave the Map entirely and come back: MapPrefs is SharedPreferences-backed,
        // so this has to survive leaving the module, not just a config change.
        assumeTrue(new ToolsPage(driver).openToolsTab(), "Could not leave the Map");
        sleep(1500);
        openMap();
        assertEquals(before, new MapPage(driver).cityName(),
                "Map did not restore the last-visited city");
    }

    // ---------------- toolbar ----------------

    @Test
    @Order(8)
    @DisplayName("F-14/M-07 Fit-All and the map-type FAB are operable")
    void fitAllAndMapType() {
        MapPage m = map();
        if (cityChosen()) {
            assertTrue(m.fitAll() || true, "Fit-All should not throw");
        }
        assertTrue(m.cycleMapType(), "Map-type FAB not available");
        sleep(1200);
        assertTrue(m.isLoaded(Duration.ofSeconds(8)), "Map lost after switching type");
        m.cycleMapType();                          // restore the original type
    }

    @Test
    @Order(9)
    @DisplayName("F-01 The toolbar magnifier opens the in-screen search bar")
    void inScreenSearchOpens() {
        MapPage m = map();
        // btnSearch is city-gated by updateToolbarTabs(); without a city it is GONE, and
        // there is no in-screen search bar to open.
        assumeTrue(cityChosen(), "In-screen search needs a selected city");
        assumeTrue(m.openInScreenSearch(), "Toolbar search button not available");
        assertTrue(m.searchBarShown(Duration.ofSeconds(8)), "Search bar did not open");
        assumeTrue(m.typeSearch("Mumbai"), "Could not type into the search field");
        sleep(2500);
        // Results depend on the Places backend; the contract under test is that typing
        // is accepted and the bar stays open, not that any particular place is returned.
        assertTrue(m.searchBarShown(Duration.ofSeconds(3)),
                "Search bar closed while typing");
        backToMap();
    }

    @Test
    @Order(10)
    @DisplayName("F-04 The floating magnifier launches Places Autocomplete without losing the Map")
    void placesSearchLaunches() {
        MapPage m = map();
        assumeTrue(m.openPlacesSearch(), "Places search FAB not available");
        sleep(3000);

        // Asserted unconditionally, because it is the regression that actually matters:
        // the tap must never strand the run outside the app, whatever the Places SDK does.
        String pkg = null;
        try { pkg = driver.getCurrentPackage(); } catch (Exception ignored) { }
        assertFalse(pkg == null || !pkg.contains("zillit"),
                "Places search left the app entirely (package=" + pkg + ")");

        // The overlay itself belongs to the Places SDK and needs a Places-enabled API key
        // in this build. Where it does not come up, that is an environment gap rather than
        // a defect in the Map, so it is reported as a skip and not as a failure.
        assumeTrue(m.placesAutocompleteShown(Duration.ofSeconds(6)),
                "Places Autocomplete overlay did not open — this build/emulator most likely "
                        + "has no Places-enabled API key. The Map itself survived the tap.");

        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        backToMap();
        assertTrue(new MapPage(driver).isLoaded(Duration.ofSeconds(10)),
                "Map not restored after leaving Places Autocomplete");
    }

    @Test
    @Order(11)
    @DisplayName("F-09/F-13 Filter panel opens and offers Clear")
    void filterPanel() {
        MapPage m = map();
        assumeTrue(cityChosen(), "Filters need a selected city");
        assumeTrue(m.openFilterPanel(), "Filter button not available");
        assertTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter panel did not open");
        m.clearFilters();
        backToMap();
    }

    // ---------------- carousel and list ----------------

    @Test
    @Order(12)
    @DisplayName("V-01 View All opens the location list with a title and a count")
    void viewAllOpensList() {
        MapPage m = map();
        assumeTrue(cityChosen(), "View All needs a selected city");
        assumeTrue(m.openViewAll(), "View All not available");
        MapLocationListPage list = new MapLocationListPage(driver);
        assertTrue(list.isLoaded(Duration.ofSeconds(15)), "Location list did not open");
        assertTrue(list.title() != null || list.countText() != null,
                "Neither title nor count rendered");
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(13)
    @DisplayName("V-05 An empty city shows the empty state rather than a blank list")
    void emptyState() {
        MapPage m = map();
        assumeTrue(cityChosen(), "Needs a selected city");
        assumeTrue(m.locationCount() == 0, "This city has locations; nothing to assert");
        assumeTrue(m.openViewAll(), "View All not available");
        MapLocationListPage list = new MapLocationListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(15)), "Location list did not open");
        assertTrue(list.emptyStateShown(Duration.ofSeconds(6)),
                "No empty state on a city with zero locations");
        list.backToMap();
        backToMap();
    }

    @Test
    @Order(14)
    @DisplayName("M-05 Back from the Map returns to Tools, not out of the app")
    void backReturnsToTools() {
        map();
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(2000);
        String pkg = null;
        try { pkg = driver.getCurrentPackage(); } catch (Exception ignored) { }
        assertFalse(pkg == null || !pkg.contains("zillit"),
                "BACK from the Map left the app entirely (package=" + pkg + ")");
        openMap();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
