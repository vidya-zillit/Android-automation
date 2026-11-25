package com.zillit.automation.tests.map;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.map.LocationToolPage;
import com.zillit.automation.pages.map.MapCityFormPage;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MAP &larr; LOCATION TOOL, end to end: a shoot location uploaded in the Location tool, with
 * city details matching a city the Map already has, is drawn on the Map once it reaches
 * <b>Shortlist</b> and again (re-tagged) once it reaches <b>Final</b> — and never while it
 * sits in <b>Selects</b>.
 *
 * <p><b>How the Map decides where an overlay pin shows</b> ({@code development}):
 * {@code MapViewModel.visibleLocationPins()} → {@code LocationPinCities.pinsForCity()}. A
 * Location-tool pin is filed under the Map city whose CIRCLE contains its coordinates
 * (nearest centre wins); a pin inside no city's circle shows under every city. The typed
 * <i>City</i> text is not used for filing at all — {@code toPinnedLocation()} drops it — so
 * "the city exists in the Map" holds because the pin lies inside that city, and the City
 * text is checked separately to match it. The toggle's "(n)" is the unfiltered,
 * project-wide count ({@code locationPins.size}).
 *
 * <p><b>Fixture route.</b> Location tool → Selects → "+" → "Search / pick on map" (the
 * picker drops its marker on the device's GPS fix and reverse-geocodes Address and City) →
 * name, scene, episode → Upload Media → Link (create refuses an entry with no media or
 * link) → Submit. Moves go through the row menu ({@code ivArrow}) → "Move To Shortlist" /
 * "Move To Final" → the move form → Submit. The Map city is the one containing that GPS
 * fix; if the project has none, it is made from the city panel's "Add Current City" card
 * and deleted again at the end.
 *
 * <p><b>Writes to the backend.</b> One {@code ZL_AUTO_} location (and possibly one city),
 * removed by the last case and again by {@link #cleanupLeftovers()} if a case died first.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.MAP_LOCATION_FLOW)
class MapLocationFlowTest extends MapBaseTest {

    private static final String SCENE = "901";
    private static final String EPISODE = "1";
    private static final String LINK = "https://example.com/zl-auto-shoot-location";

    /** The location this suite creates, as typed ("ZL_AUTO_Shoot 12345"). */
    private final String locName = "ZL_AUTO_Shoot " + (System.currentTimeMillis() % 100000);
    /** The Map city the pin lies in, and that the location's City field names. */
    private String cityName;
    /** True when this suite added {@link #cityName} and must delete it. */
    private boolean createdCity;
    /** Another Map city, whose circle does NOT contain the pin — for L-19. */
    private String otherCity;
    /** Address / City the form ended up with, as saved. */
    private String formAddress;
    private String formCity;
    /** Project-wide overlay count before the fixture reached Shortlist. */
    private int baselineCount = -1;
    /** Which tab the fixture currently sits in, or null when it does not exist. */
    private String fixtureTab;

    // ───────────────────────── navigation ─────────────────────────

    /** Opens Tools → Location from wherever the previous step left the app. */
    private LocationToolPage openLocationTool() {
        LocationToolPage tool = new LocationToolPage(driver);
        if (tool.isLoaded(Duration.ofSeconds(2))) return tool;
        // One level down (an episode's listing page, or a form): BACK is the way up.
        for (int i = 0; i < 3 && !onBottomNavScreen(); i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1500);
            if (tool.isLoaded(Duration.ofSeconds(2))) return tool;
        }
        for (int i = 0; i < 5 && !onBottomNavScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        assumeTrue(recoverToBottomNav(), "Could not get back to the bottom nav");
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Location");
        assumeTrue(tools.isModuleListed("Location", Duration.ofSeconds(6)),
                "Location tool not enabled on this project — switch it on under Tools →"
                        + " 'Click here to select tools'");
        assumeTrue(tool.tapToolRow("Location"), "The Location row could not be tapped");
        assumeTrue(tool.isLoaded(Duration.ofSeconds(25)), "Location tool did not load");
        return tool;
    }

    private LocationToolPage openTab(String tab) {
        LocationToolPage tool = openLocationTool();
        assumeTrue(tool.selectTab(tab), "Could not select the " + tab + " tab");
        sleep(2500);
        return tool;
    }

    /**
     * Opens the tab and, on a TELEVISION project, the "Episode No : 1" group the fixture
     * is filed under (the tabs group by episode there; the location rows and their
     * Move / Delete menu live on the listing page one level down).
     *
     * @return the page showing the location's row, or null when the tab does not list it
     */
    private LocationToolPage rowInTab(String tab, Duration wait) {
        long deadline = System.currentTimeMillis() + wait.toMillis();
        do {
            LocationToolPage tool = openTab(tab);
            if (tool.rowShown(locName)) return tool;
            if (tool.openEpisodeGroup(EPISODE)) {
                if (tool.waitForRow(locName, Duration.ofSeconds(8))) return tool;
            } else if (tool.hasRow(locName)) {
                return tool;
            }
            sleep(3000);
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    /**
     * Re-enters the Map from Tools so it is a fresh {@code NewMapPage}: onResume refetches
     * {@code /location/map-pins}, which is the "refresh" the cases rely on.
     */
    private MapPage freshMapOn(String city) {
        for (int i = 0; i < 5 && !onBottomNavScreen(); i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) {
            try { driver.navigate().back(); } catch (Exception ignored) { }   // leave it
            sleep(1500);
        }
        openMap();
        MapPage m = map();
        if (city != null) selectCity(m, city);
        sleep(4000);    // the overlay fetch lands after the city's own locations
        return m;
    }

    private void selectCity(MapPage m, String city) {
        if (city.equals(String.valueOf(m.cityName()).trim())) return;
        assumeTrue(m.openCityPanel(), "City panel did not open");
        assumeTrue(m.selectCity(city), "Could not select the city \"" + city + "\" in "
                + m.visibleCityNames());
        sleep(2500);
        if (m.cityPanelShown(Duration.ofSeconds(1))) m.closeCityPanel();
        assumeTrue(city.equals(String.valueOf(m.cityName()).trim()),
                "Map still shows \"" + m.cityName() + "\" after selecting \"" + city + "\"");
    }

    /** Every card in the bottom panel's list mode; closes the panel again. */
    private List<MapPage.PanelCard> panelCards(MapPage m) {
        // tvLocationCount is getFilteredLocations().size — the city's own pins PLUS the
        // visible overlay. At zero there is no list to open (the panel has nothing to cycle
        // to), and zero is itself the answer: nothing at all is drawn for this city.
        if (m.locationCount() == 0) {
            System.out.println(">>> " + m.cityName() + " draws 0 locations (" + m.locationCountText() + ")");
            return new java.util.ArrayList<>();
        }
        assumeTrue(m.openPanelList(),
                "The bottom location panel did not reach list mode (rvLocationList); count reads '"
                        + m.locationCountText() + "'");
        List<MapPage.PanelCard> cards = m.panelListCards();
        System.out.println(">>> panel cards on " + m.cityName() + " (" + cards.size() + "): " + cards);
        m.closeBottomPanel();
        sleep(800);
        return cards;
    }

    private List<MapPage.PanelCard> ours(List<MapPage.PanelCard> cards) {
        return cards.stream()
                .filter(c -> c.name != null && c.name.trim().equalsIgnoreCase(locName))
                .collect(Collectors.toList());
    }

    /** The overlay must be ON for absence to mean anything. Turns it on if it is off. */
    private void requireOverlayOn(MapPage m) {
        assumeTrue(m.overlayToggleShown(Duration.ofSeconds(6)),
                "No 'Shoot Locations' toggle — this account has no Location-tool view access");
        Boolean on = m.overlayTintActive();
        if (on == null) on = m.overlayPinCount() >= 0;
        if (!on) {
            System.out.println(">>> overlay was OFF — switching it on");
            m.toggleOverlay();
            sleep(3000);
            on = m.overlayTintActive();
        }
        assumeTrue(Boolean.TRUE.equals(on),
                "Could not confirm the Shoot Locations overlay is ON (label '"
                        + m.overlayToggleLabel() + "')");
    }

    private int overlayCount(MapPage m) {
        int n = m.overlayPinCount();
        return Math.max(n, 0);   // "(n)" is omitted at zero
    }

    // ───────────────────────── setup ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("LF-01 The Map has a city containing the device's location (added via Add Current City if missing)")
    void mapCityForTheDeviceLocation() {
        ensureToolbarReady();
        MapPage m = map();
        assumeTrue(m.openCityPanel(), "City panel did not open");
        List<String> before = m.visibleCityNames();
        System.out.println(">>> Map cities: " + before);

        // updateCurrentCityCard() hides the card when the detected city is already on the
        // project — in which case that city IS the one we want, and LF-02 learns its name
        // from the reverse-geocoded City field.
        if (m.currentCityCardShown(Duration.ofSeconds(6))) {
            String suggested = String.valueOf(m.currentCityName()).trim();
            assumeTrue(!suggested.isEmpty(), "Current-city card shows no name");
            assumeTrue(m.addCurrentCity(), "Could not tap Add Current City");
            sleep(3000);
            MapCityFormPage form = new MapCityFormPage(driver);
            assumeTrue(form.isLoaded(Duration.ofSeconds(10)), "City form did not open");
            assumeTrue(form.placePicked(), "City form arrived without coordinates");
            String named = String.valueOf(form.cityNameShown()).trim();
            assumeTrue(form.save(), "Save not available on the city form");
            sleep(5000);
            backToMap();
            m = map();
            assumeTrue(m.openCityPanel(), "City panel did not reopen");
            List<String> after = m.visibleCityNames();
            String added = after.stream().filter(c -> !before.contains(c)).findFirst()
                    .orElse(named.isEmpty() ? suggested : named);
            assertTrue(after.contains(added),
                    "Saved the current city \"" + added + "\" but the panel lists " + after);
            cityName = added;
            createdCity = true;
        }
        m.closeCityPanel();
        sleep(800);
        System.out.println(">>> city for the pin: " + cityName + " (created=" + createdCity + ")");
    }

    // ───────────────────────── create in Selects ─────────────────────────

    @Test
    @Order(2)
    @DisplayName("LF-02 A ZL_AUTO_ location is added to Selects with a map pin and the Map city as its City")
    void createInSelects() {
        LocationToolPage tool = openTab(LocationToolPage.TAB_SELECT);
        assumeTrue(tool.openAddForm(), "The add (+) form did not open from Selects");

        String link = tool.pickCurrentLocationOnMap();
        assumeTrue(link != null, "The map picker returned no pin — the Google Maps link stayed"
                + " empty (link field: " + tool.fieldText(LocationToolPage.FIELD_LINK) + ")");

        formCity = String.valueOf(tool.fieldText(LocationToolPage.FIELD_CITY)).trim();
        formAddress = String.valueOf(tool.fieldText(LocationToolPage.FIELD_ADDRESS)).trim();
        System.out.println(">>> picker filled City='" + formCity + "' Address='" + formAddress + "'");
        if (cityName == null) {
            // LF-01 found no current-city card, i.e. the detected city is on the project.
            cityName = formCity;
        }
        if (!formCity.equalsIgnoreCase(cityName)) {
            assertTrue(tool.setField(LocationToolPage.FIELD_CITY, cityName),
                    "Could not set City to the Map city \"" + cityName + "\"");
            formCity = cityName;
        }
        if (formAddress.isEmpty() || "Address".equals(formAddress)) {
            formAddress = "1 ZL Auto Street, " + cityName;
            assertTrue(tool.setField(LocationToolPage.FIELD_ADDRESS, formAddress), "Could not set Address");
        }

        assertTrue(tool.setField(LocationToolPage.FIELD_NAME, locName), "Could not type the name");
        assertTrue(tool.setField(LocationToolPage.FIELD_SCENE, SCENE), "Could not type the scene no.");
        tool.setField(LocationToolPage.FIELD_EPISODE, EPISODE);   // TV projects only

        assumeTrue(tool.addLinkAttachment(LINK),
                "Could not add a link attachment (Upload Media → Link → paste → Submit)");
        assertTrue(tool.submitForm(), "Submit not available");
        sleep(4000);
        String snack = tool.snackbarText();
        assertFalse(tool.formShown(Duration.ofSeconds(2)) && snack != null,
                "The form refused the location: " + snack);

        assumeTrue(new LocationToolPage(driver).isLoaded(Duration.ofSeconds(20)),
                "Did not return to the Location tool");
        // Recorded before looking, so the cleanup still tries if the row is slow to list.
        fixtureTab = LocationToolPage.TAB_SELECT;
        LocationToolPage list = rowInTab(LocationToolPage.TAB_SELECT, Duration.ofSeconds(45));
        assertNotNull(list, "\"" + locName + "\" never appeared in Selects");
    }

    // ───────────────────────── Selects: not on the Map ─────────────────────────

    @Test
    @Order(3)
    @DisplayName("L-12 While in Selects the location is NOT on the Map, with the overlay ON")
    void selectsNeverOnMap() {
        assumeTrue(LocationToolPage.TAB_SELECT.equals(fixtureTab), "LF-02 did not create the location");
        MapPage m = freshMapOn(cityName);
        requireOverlayOn(m);
        baselineCount = overlayCount(m);
        List<MapPage.PanelCard> mine = ours(panelCards(m));
        assertTrue(mine.isEmpty(),
                "A Selects-stage location is drawn on the Map — tagFor() must return null for"
                        + " anything but shortlisted/final. Cards: " + mine);
    }

    // ───────────────────────── Shortlist ─────────────────────────

    private void move(String fromTab, String menuItem, String toTab) {
        LocationToolPage tool = rowInTab(fromTab, Duration.ofSeconds(10));
        assumeTrue(tool != null, "\"" + locName + "\" is not in " + fromTab);
        assumeTrue(tool.rowMenu(locName, menuItem),
                "'" + menuItem + "' not offered for the row in " + fromTab);
        // "data without an assigned scene number will not be moved" — only when a scene is
        // missing; ours has one, but accept it rather than strand the case.
        if (!tool.formShown(Duration.ofSeconds(6))) tool.confirmDialog(Duration.ofSeconds(3));
        assumeTrue(tool.formShown(Duration.ofSeconds(10)), "The move form did not open");

        String scene = String.valueOf(tool.fieldText(LocationToolPage.FIELD_SCENE)).trim();
        if (!scene.matches("\\d.*")) tool.setField(LocationToolPage.FIELD_SCENE, SCENE);
        String ep = String.valueOf(tool.fieldText(LocationToolPage.FIELD_EPISODE)).trim();
        if (!ep.matches("\\d.*") && tool.fieldEnabled(LocationToolPage.FIELD_EPISODE)) {
            tool.setField(LocationToolPage.FIELD_EPISODE, EPISODE);
        }
        assertTrue(tool.submitForm(), "Submit not available on the move form");
        sleep(5000);
        assertFalse(tool.formShown(Duration.ofSeconds(2)) && tool.snackbarText() != null,
                "The move form refused: " + tool.snackbarText());

        LocationToolPage there = rowInTab(toTab, Duration.ofSeconds(45));
        if (there != null) fixtureTab = toTab;
        assertNotNull(there, "\"" + locName + "\" did not arrive in " + toTab);

        // NOT asserted: whether the source tab still lists it. On this build it does — the
        // "move" promotes a copy (the backend's map-pins endpoint is documented to collapse
        // a place that "sits in both stages", LocationToolPin.kt). Recorded for the report;
        // what the Map makes of the two copies is held by L-14 ("drawn once").
        LocationToolPage source = openTab(fromTab);
        boolean stillThere = source.rowShown(locName)
                || (source.openEpisodeGroup(EPISODE) && source.hasRow(locName));
        System.out.println(">>> after '" + menuItem + "': still listed in " + fromTab + " = " + stillThere);
    }

    @Test
    @Order(4)
    @DisplayName("LT-08 Move To Shortlist (from Selects): the location appears in Shortlist")
    void moveToShortlist() {
        assumeTrue(LocationToolPage.TAB_SELECT.equals(fixtureTab), "No location in Selects to move");
        move(LocationToolPage.TAB_SELECT, "Move To Shortlist", LocationToolPage.TAB_SHORTLIST);
    }

    @Test
    @Order(5)
    @DisplayName("L-13 Shortlisted: the Map (refreshed) draws it as 'Shortlisted Shoot Location' in its city")
    void shortlistOnMap() {
        assumeTrue(LocationToolPage.TAB_SHORTLIST.equals(fixtureTab), "The location is not in Shortlist");
        MapPage m = freshMapOn(cityName);
        requireOverlayOn(m);
        List<MapPage.PanelCard> mine = ours(panelCards(m));
        assertEquals(1, mine.size(),
                "Expected exactly one card for \"" + locName + "\" on " + cityName + " once it is"
                        + " Shortlisted, got " + mine);
        MapPage.PanelCard c = mine.get(0);
        assertTrue(c.type != null && c.type.contains(MapPage.LABEL_SHORTLISTED),
                "The card should read '" + MapPage.LABEL_SHORTLISTED + "', got: " + c.type);
        if (baselineCount >= 0) {
            assertEquals(baselineCount + 1, overlayCount(m),
                    "The toggle's project-wide count should grow by one (label '"
                            + m.overlayToggleLabel() + "')");
        }
    }

    @Test
    @Order(6)
    @DisplayName("L-13b The Shortlisted card carries the city details: the address the location was saved with")
    void shortlistCardDetails() {
        assumeTrue(LocationToolPage.TAB_SHORTLIST.equals(fixtureTab), "The location is not in Shortlist");
        MapPage m = freshMapOn(cityName);
        requireOverlayOn(m);
        List<MapPage.PanelCard> mine = ours(panelCards(m));
        assumeTrue(mine.size() == 1, "No single card for the location: " + mine);
        MapPage.PanelCard c = mine.get(0);
        assertNotNull(c.address, "The overlay card shows no address (tvAddress)");
        assertEquals(formAddress, c.address.trim(),
                "The card's address should be the one saved in the Location tool");
        assertTrue(c.address.contains(cityName) || formAddress.contains(cityName),
                "Neither the saved address nor the card names the city \"" + cityName + "\": "
                        + c.address);
        // Read-only on the Map: the overlay belongs to the Location tool.
        assertFalse(c.edit, "Overlay card offers Edit: " + c);
        assertFalse(c.delete, "Overlay card offers Delete: " + c);
        // L-17 (web map.md:124): read-only, but still viewable and routable.
        assertTrue(c.view, "Overlay card lost View: " + c);
        assertTrue(c.directions, "Overlay card lost Directions: " + c);
    }

    // ───────────────────────── Final ─────────────────────────

    @Test
    @Order(7)
    @DisplayName("LT-09 Move To Final (from Shortlist): the location appears in Final")
    void moveToFinal() {
        assumeTrue(LocationToolPage.TAB_SHORTLIST.equals(fixtureTab), "No location in Shortlist to move");
        move(LocationToolPage.TAB_SHORTLIST, "Move To Final", LocationToolPage.TAB_FINAL);
    }

    @Test
    @Order(8)
    @DisplayName("L-14 Final: the Map re-tags it 'Finalized Shoot Location' and draws it once")
    void finalOnMap() {
        assumeTrue(LocationToolPage.TAB_FINAL.equals(fixtureTab), "The location is not in Final");
        MapPage m = freshMapOn(cityName);
        requireOverlayOn(m);
        List<MapPage.PanelCard> mine = ours(panelCards(m));
        assertEquals(1, mine.size(),
                "A Final location must be drawn exactly once (not once per stage). Cards: " + mine);
        MapPage.PanelCard c = mine.get(0);
        assertTrue(c.type != null && c.type.contains(MapPage.LABEL_FINAL),
                "The card should read '" + MapPage.LABEL_FINAL + "', got: " + c.type);
        assertFalse(c.type.contains(MapPage.LABEL_SHORTLISTED),
                "The card still carries the Shortlisted label: " + c.type);
        assertEquals(formAddress, String.valueOf(c.address).trim(),
                "The Final card lost the saved address");
        if (baselineCount >= 0) {
            assertEquals(baselineCount + 1, overlayCount(m),
                    "Moving Shortlist → Final must not add a second overlay pin (label '"
                            + m.overlayToggleLabel() + "')");
        }
    }

    // ───────────────────────── city scoping ─────────────────────────

    @Test
    @Order(9)
    @DisplayName("L-19 Overlay pins are filed by city: absent under another city, the toggle count unchanged")
    void scopedToItsCity() {
        assumeTrue(LocationToolPage.TAB_FINAL.equals(fixtureTab)
                        || LocationToolPage.TAB_SHORTLIST.equals(fixtureTab),
                "The location never reached Shortlist/Final");
        MapPage m = freshMapOn(cityName);
        requireOverlayOn(m);
        int countHere = overlayCount(m);
        assumeTrue(m.openCityPanel(), "City panel did not open");
        otherCity = m.visibleCityNames().stream()
                .filter(c -> c != null && !c.trim().equalsIgnoreCase(cityName))
                .findFirst().orElse(null);
        m.closeCityPanel();
        sleep(800);
        assumeTrue(otherCity != null, "Only one city on the project — nothing to compare against");

        selectCity(m, otherCity);
        sleep(4000);
        List<MapPage.PanelCard> mine = ours(panelCards(m));
        // LocationPinCities.pinsForCity(): a pin inside a city's circle is shown under that
        // city only (nearest centre wins); our pin sits at the city's own centre.
        assertTrue(mine.isEmpty(),
                "\"" + locName + "\" lies in " + cityName + " but is also drawn under " + otherCity
                        + " — overlay pins are filed by position (pinsForCity). Cards: " + mine);
        assertEquals(countHere, overlayCount(m),
                "The toggle's count is project-wide and must not change with the city");
        selectCity(m, cityName);
    }

    // ───────────────────────── cleanup ─────────────────────────

    private boolean deleteFixture() {
        if (fixtureTab == null) return true;
        // Look in the tab we believe it is in first, then the others (a move may have
        // landed without the case seeing it).
        List<String> tabs = new java.util.ArrayList<>(LocationToolPage.TABS_IN_ORDER);
        tabs.remove(fixtureTab);
        tabs.add(0, fixtureTab);
        for (String tab : tabs) {
            LocationToolPage tool = rowInTab(tab, Duration.ofSeconds(5));
            if (tool == null) continue;
            if (!tool.rowMenu(locName, "Delete")) return false;
            tool.confirmDialog(Duration.ofSeconds(6));
            sleep(4000);
            tool.search("");
            if (!tool.waitForRowGone(locName, Duration.ofSeconds(30))) return false;
        }
        fixtureTab = null;
        return true;
    }

    private boolean deleteCreatedCity() {
        if (!createdCity || cityName == null) return true;
        MapPage m = freshMapOn(null);
        assumeTrue(m.openCityPanel(), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        String other = cities.stream().filter(c -> !c.equals(cityName)).findFirst().orElse(null);
        if (other != null && cityName.equals(String.valueOf(m.cityName()).trim())) {
            m.selectCity(other);
            sleep(2500);
            m.openCityPanel();
        }
        if (!new MapCityFormPage(driver).deleteCity(cityName)) return false;
        MapDialogPage dlg = new MapDialogPage(driver);
        if (dlg.shown(Duration.ofSeconds(6))) {
            if (!dlg.tap(MapDialogPage.BTN_DELETE)) dlg.tap(MapDialogPage.BTN_YES);
        }
        sleep(4000);
        m.openCityPanel();
        List<String> after = m.visibleCityNames();
        m.closeCityPanel();
        System.out.println(">>> cities before delete " + cities + ", after " + after);
        boolean gone = after.stream().noneMatch(c -> c.equals(cityName));
        if (gone) createdCity = false;
        // Nothing else may have gone with it.
        for (String c : cities) {
            if (!c.equals(cityName)) {
                assertTrue(after.contains(c), "Deleting \"" + cityName + "\" also removed \"" + c
                        + "\" — cities now " + after);
            }
        }
        return gone;
    }

    @Test
    @Order(10)
    @DisplayName("LF-99 Cleanup: the ZL_AUTO_ location (and any city this suite added) is deleted")
    void cleanup() {
        assumeTrue(fixtureTab != null || createdCity, "Nothing was created");
        String tab = fixtureTab;
        assertTrue(deleteFixture(), "\"" + locName + "\" could not be deleted from " + tab);
        assertTrue(deleteCreatedCity(), "The city \"" + cityName + "\" this suite added is still on the Map");
    }

    /** Safety net when a case died before {@link #cleanup()}. Never fails the run. */
    @AfterAll
    void cleanupLeftovers() {
        try { if (fixtureTab != null) deleteFixture(); } catch (Throwable t) {
            System.out.println(">>> leftover location not removed: " + t.getMessage());
        }
        try { if (createdCity) deleteCreatedCity(); } catch (Throwable t) {
            System.out.println(">>> leftover city not removed: " + t.getMessage());
        }
    }
}
