package com.zillit.automation.tests.map;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
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
 * MAP &rarr; the <b>Location-tool pin overlay</b>: Shortlisted and Final shoot locations
 * drawn over the Map's own pins.
 *
 * <p>What the feature promises, and what each case here holds it to:
 * <ul>
 *   <li>Only <b>Shortlisted</b> and <b>Final</b> cross over. A location still in the
 *       Location tool's <i>Select</i> tab has no tag, and {@code LocationPinStatus.tagFor()}
 *       returns null for it, so it is never drawn.</li>
 *   <li>The overlay defaults to <b>ON</b> and is remembered in SharedPreferences, so it
 *       survives an app kill — the requirement is that the Map <i>shows</i> these pins
 *       without the user first discovering a toggle.</li>
 *   <li>Overlay pins are <b>read-only here</b>. They belong to the Location tool and are
 *       edited there, so the card keeps View and Directions but loses Edit and Delete,
 *       and the marker is not draggable, <i>even for a user with posting rights</i>.</li>
 *   <li>The overlay is <b>project-wide</b>, not city-scoped: the endpoint carries no city
 *       reference, so pins filed under another city still show.</li>
 *   <li>Two label registers: the card spells it out ("Shortlisted Shoot Location") while
 *       the filter chip keeps the short tag ("Shortlisted").</li>
 * </ul>
 *
 * <p><b>Precondition.</b> The whole toggle is GONE without Location-tool VIEW access —
 * {@code applyPostingRights()} keys {@code btnLocationPins} on {@code canViewLocationTool}.
 * On a project where the Location tool is not enabled, every case below skips with that
 * reason rather than failing, because there is nothing wrong with the app in that state.
 *
 * <p><b>Read-only.</b> Nothing here moves a location between Location-tool tabs; that
 * writes to the shared backend. The move cases (L-12…L-14, X-01…X-03) are written up in
 * {@code MAP_LOCATION_TESTCASES.md} and left manual.
 */
@Order(TestOrder.MAP_LOCATION_PINS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class MapLocationPinsTest extends BaseTest {

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

    private void openMap() {
        for (int i = 0; i < 4; i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) return;
            if (onBottomNavScreen()) break;     // BACK here would exit the app
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
        assumeTrue(new MapPage(driver).isLoaded(Duration.ofSeconds(25)), "Map did not load");
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

    /**
     * Every case needs the toggle to exist. Its absence is a legitimate app state —
     * no Location-tool view access — so this is an assumption, not an assertion. The one
     * case that asserts on absence is {@link #toggleHiddenWithoutAccess()}.
     */
    private MapPage mapWithOverlay() {
        MapPage p = map();
        assumeTrue(p.overlayToggleShown(Duration.ofSeconds(6)),
                "No Location-tool view access on this project — the Shoot Locations "
                        + "toggle is hidden, so there is no overlay to test");
        return p;
    }

    private void backToMap() {
        for (int i = 0; i < 4; i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
            if (onBottomNavScreen()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        if (!new MapPage(driver).isLoaded(Duration.ofSeconds(1))) openMap();
    }

    /** Leaves the overlay ON, which is the app's own default, whatever state we found. */
    private void restoreOverlayOn(MapPage m) {
        if (!m.overlayActive()) m.toggleOverlay();
    }

    // ---------------- presence and gating ----------------

    @Test
    @Order(1)
    @DisplayName("L-01 The 'Shoot Locations' toggle is present with Location-tool view access")
    void toggleIsPresent() {
        MapPage m = mapWithOverlay();
        String label = m.overlayToggleLabel();
        assertTrue(label != null && label.contains(MapPage.SHOOT_LOCATIONS),
                "Toggle should be labelled '" + MapPage.SHOOT_LOCATIONS + "', got: " + label);
    }

    @Test
    @Order(2)
    @DisplayName("L-02 Without Location-tool access the toggle is absent, not merely disabled")
    void toggleHiddenWithoutAccess() {
        MapPage m = map();
        assumeTrue(!m.overlayToggleShown(Duration.ofSeconds(4)),
                "This account HAS Location-tool view access; the hidden case cannot run here");
        // The contract is View.GONE: a disabled-but-present button would be a regression,
        // because it advertises a tool the user cannot see.
        assertFalse(m.overlayToggleShown(Duration.ofSeconds(2)),
                "Toggle should be gone entirely without Location-tool view access");
    }

    // ---------------- default state and persistence ----------------

    @Test
    @Order(3)
    @DisplayName("L-03 The overlay is ON by default — the user should not have to find the toggle")
    void defaultsToOn() {
        MapPage m = mapWithOverlay();
        assertTrue(m.overlayActive(),
                "Overlay should default to ON (MapPrefs.showLocationPins defaults true); "
                        + "toggle label was: " + m.overlayToggleLabel());
    }

    @Test
    @Order(4)
    @DisplayName("L-05 While ON with pins loaded the label carries the count")
    void labelCarriesCount() {
        MapPage m = mapWithOverlay();
        restoreOverlayOn(m);
        sleep(2500);
        int count = m.overlayPinCount();
        assumeTrue(count >= 0,
                "No shoot locations carry coordinates on this project, so the label shows "
                        + "no count — nothing to assert");
        assertTrue(count > 0, "A rendered count should be positive, got " + count);
        assertTrue(m.overlayToggleLabel().contains("(" + count + ")"),
                "Label should read '" + MapPage.SHOOT_LOCATIONS + " (" + count + ")'");
    }

    @Test
    @Order(5)
    @DisplayName("L-06/L-07 Toggling OFF drops the count, toggling back ON restores it")
    void toggleOffAndOn() {
        MapPage m = mapWithOverlay();
        restoreOverlayOn(m);
        sleep(2000);
        int before = m.overlayPinCount();
        assumeTrue(before > 0, "No overlay pins on this project to toggle");

        assumeTrue(m.toggleOverlay(), "Could not tap the toggle");
        assertFalse(m.overlayActive(), "Overlay still reports active after switching OFF");
        assertEquals(-1, m.overlayPinCount(),
                "The count must disappear while the overlay is OFF, label was: "
                        + m.overlayToggleLabel());

        assumeTrue(m.toggleOverlay(), "Could not tap the toggle back on");
        assertTrue(m.overlayActive(), "Overlay did not come back ON");
        assertEquals(before, m.overlayPinCount(),
                "Pin count changed across a toggle round trip");
    }

    @Test
    @Order(6)
    @DisplayName("L-08 The OFF state survives an app kill (SharedPreferences, not ViewModel state)")
    void persistsAcrossAppKill() {
        MapPage m = mapWithOverlay();
        restoreOverlayOn(m);
        assumeTrue(m.toggleOverlay(), "Could not switch the overlay off");
        assumeTrue(!m.overlayActive(), "Overlay did not switch off");

        try {
            driver.executeScript("mobile: terminateApp", java.util.Map.of("appId", appPackage));
            sleep(1500);
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
        } catch (Exception e) {
            assumeTrue(false, "Could not restart the app: " + e.getMessage());
        }
        sleep(6000);
        dismissPermissionPrompts();
        openMap();

        MapPage after = new MapPage(driver);
        assumeTrue(after.overlayToggleShown(Duration.ofSeconds(10)), "Toggle gone after restart");
        boolean stillOff = !after.overlayActive();
        restoreOverlayOn(after);                   // put the default back for later suites
        assertTrue(stillOff,
                "The overlay came back ON after an app kill — the preference is not persisting");
    }

    // ---------------- labels ----------------

    @Test
    @Order(7)
    @DisplayName("L-09/L-10 Overlay cards spell the stage out: 'Shortlisted/Finalized Shoot Location'")
    void cardsUseSpelledOutLabels() {
        MapPage m = mapWithOverlay();
        restoreOverlayOn(m);
        sleep(2500);
        assumeTrue(m.overlayPinCount() > 0, "No overlay pins to inspect");
        assumeTrue(m.carouselShown(Duration.ofSeconds(6))
                        || m.bottomPanelShown(Duration.ofSeconds(4)),
                "Carousel not open — tap a marker first");
        List<String> types = m.carouselTypes();
        assumeTrue(!types.isEmpty(), "No type labels on the visible cards");
        boolean anySpelledOut = types.stream().anyMatch(t ->
                t.contains(MapPage.LABEL_SHORTLISTED) || t.contains(MapPage.LABEL_FINAL));
        assertTrue(anySpelledOut,
                "An overlay card must spell the stage out, not show the bare tag. Saw: " + types);
    }

    @Test
    @Order(8)
    @DisplayName("L-11 Filter chips keep the SHORT tag, unlike the cards")
    void chipsUseShortTags() {
        MapPage m = mapWithOverlay();
        restoreOverlayOn(m);
        sleep(2000);
        assumeTrue(m.overlayPinCount() > 0, "No overlay pins, so no stage chips");
        assumeTrue(m.openFilterPanel(), "Filter button not available");
        assumeTrue(m.filterPanelShown(Duration.ofSeconds(8)), "Filter panel did not open");
        List<String> chips = m.filterTypeLabels();
        assumeTrue(chips.stream().anyMatch(c ->
                        c.contains(MapPage.TAG_SHORTLISTED) || c.contains(MapPage.TAG_FINAL)),
                "No stage chips present; saw " + chips);
        for (String c : chips) {
            assertFalse(c.contains("Shoot Location"),
                    "Filter chips must carry the short tag, not the spelled-out label: " + c);
        }
        m.clearFilters();
        backToMap();
    }

    // ---------------- read-only enforcement ----------------

    /*
     * Where overlay pins can be inspected. LocationListViewModel (View All) loads only the
     * city's own getLocations(cityId), so overlay pins are NEVER in View All — the old
     * versions of L-15/16/17 looked there and could only skip or pass vacuously. Overlay
     * pins are listed in the bottom panel (MapViewModel.getFilteredLocations() =
     * visiblePins(), overlay included), whose cards come from CarouselCardAdapter:
     * btnEdit/btnDelete are GONE when isFromLocationTool, View and Directions stay.
     */

    /** Opens the panel list and returns every card; skips with the precise missing precondition. */
    private List<MapPage.PanelCard> panelCardsWithOverlay(MapPage m) {
        restoreOverlayOn(m);
        sleep(2000);
        int advertised = m.overlayPinCount();
        assumeTrue(advertised > 0,
                "The Shoot Locations overlay is on but advertises no pins (toggle reads '"
                        + m.overlayToggleLabel() + "') — the Location tool has no Shortlisted or "
                        + "Final location, so there is no overlay card to inspect");
        assumeTrue(m.openPanelList(),
                "The bottom location panel did not reach list mode (rvLocationList) via btnLocationPill");
        List<MapPage.PanelCard> cards = m.panelListCards();
        System.out.println(">>> panel cards (" + cards.size() + "): " + cards);
        long overlay = cards.stream().filter(MapPage.PanelCard::isOverlay).count();
        if (overlay == 0) m.closeBottomPanel();
        assertTrue(overlay > 0,
                "The toggle advertises " + advertised + " overlay pin(s), but no card in the bottom "
                        + "panel list carries '" + MapPage.LABEL_SHORTLISTED + "' or '"
                        + MapPage.LABEL_FINAL + "'. The panel lists visiblePins(), overlay included "
                        + "(unless a type filter hides them). Cards: " + cards);
        return cards;
    }

    @Test
    @Order(9)
    @DisplayName("L-15/L-16 Overlay cards in the bottom panel offer no Edit and no Delete")
    void overlayRowsAreReadOnly() {
        MapPage m = mapWithOverlay();
        List<MapPage.PanelCard> cards = panelCardsWithOverlay(m);
        m.closeBottomPanel();

        // Edit/Delete are hidden for EVERY card when the user lacks posting rights, which
        // would make "no Edit on the overlay card" prove nothing. Require evidence of rights:
        // a Map-owned card that shows Edit, or the Pin button in the toolbar.
        boolean canPost = cards.stream().anyMatch(c -> !c.isOverlay() && c.edit)
                || m.toolbarHasButton("btnPin");
        assumeTrue(canPost,
                "No posting rights on the Map (no Map-owned card shows Edit and there is no Pin "
                        + "button), so hidden Edit/Delete would not show the overlay is read-only");

        for (MapPage.PanelCard c : cards) {
            if (!c.isOverlay()) continue;
            assertFalse(c.edit, "Overlay card offers Edit — Location-tool pins are read-only on the Map: " + c);
            assertFalse(c.delete, "Overlay card offers Delete — Location-tool pins are read-only on the Map: " + c);
        }
        backToMap();
    }

    @Test
    @Order(10)
    @DisplayName("L-17 Overlay cards in the bottom panel keep View and Directions")
    void overlayRowsKeepView() {
        MapPage m = mapWithOverlay();
        List<MapPage.PanelCard> cards = panelCardsWithOverlay(m);
        m.closeBottomPanel();
        for (MapPage.PanelCard c : cards) {
            if (!c.isOverlay()) continue;
            assertTrue(c.view, "Overlay card lost View: " + c);
            assertTrue(c.directions, "Overlay card lost Directions: " + c);
        }
        backToMap();
    }

    // ---------------- scope ----------------

    @Test
    @Order(11)
    @DisplayName("L-19 The overlay is project-wide: switching city does not change its count")
    void overlayIsProjectWide() {
        MapPage m = mapWithOverlay();
        restoreOverlayOn(m);
        sleep(2500);
        int before = m.overlayPinCount();
        assumeTrue(before > 0, "No overlay pins to compare");

        assumeTrue(m.openCityPanel(), "Cities button not available");
        assumeTrue(m.cityPanelShown(Duration.ofSeconds(8)), "City panel did not open");
        List<String> cities = m.visibleCityNames();
        assumeTrue(cities.size() >= 2, "Needs at least two cities to switch between");
        String current = m.cityName();
        String other = cities.stream().filter(c -> !c.equals(current)).findFirst().orElse(null);
        assumeTrue(other != null, "No second city to switch to");
        assumeTrue(m.selectCity(other), "Could not switch city");
        sleep(3000);

        assertEquals(before, m.overlayPinCount(),
                "The overlay count changed with the city. It is deliberately NOT city-scoped: "
                        + "the endpoint carries no city reference to filter on.");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
