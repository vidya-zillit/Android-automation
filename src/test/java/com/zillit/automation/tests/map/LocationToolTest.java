package com.zillit.automation.tests.map;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.map.LocationToolPage;
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
 * LOCATION tool (Tools &rarr; Location) — the source of the Map's shoot-location overlay.
 *
 * <p>Three tabs, in the order {@code LocationPage} attaches them: <b>Select</b>,
 * <b>Shortlist</b>, <b>Final</b>. Only the last two reach the Map; anything sitting in
 * Select has no stage tag and {@code LocationPinStatus.tagFor()} drops it.
 *
 * <p><b>This tool is not enabled on every project.</b> {@code Tools.kt} only dispatches
 * {@code LOCATION_TOOL} where the project has it switched on, and on
 * {@code ZL-Android-Automation} it currently is not — which is also why the Map's
 * "Shoot Locations" toggle is hidden there. Every case skips with that reason rather than
 * failing; switch the tool on via Tools &rarr; "Click here to select tools" to make them run.
 *
 * <p><b>Read-only.</b> Moving a location between tabs writes to the shared backend, so the
 * move cases (LT-07…LT-10, and the cross-module round trips) stay manual for now — they are
 * written up in {@code MAP_LOCATION_TESTCASES.md}.
 */
@Order(TestOrder.LOCATION_TOOL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class LocationToolTest extends BaseTest {

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachLocationTool() {
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
        if (new LocationToolPage(driver).isLoaded(Duration.ofSeconds(2))) return;
        openLocationTool();
    }

    private void openLocationTool() {
        for (int i = 0; i < 4; i++) {
            if (new LocationToolPage(driver).isLoaded(Duration.ofSeconds(1))) return;
            if (onBottomNavScreen()) break;      // BACK here would exit the app
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
        tools.searchTools("Location");
        assumeTrue(tools.isModuleListed("Location", Duration.ofSeconds(6)),
                "Location tool not enabled on this project — switch it on under "
                        + "Tools → 'Click here to select tools' to run this suite");
        assumeTrue(tools.openModule("Location"), "Could not open the Location tool");
        assumeTrue(new LocationToolPage(driver).isLoaded(Duration.ofSeconds(25)),
                "Location tool did not load");
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private LocationToolPage page() {
        LocationToolPage p = new LocationToolPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Location tool not loaded");
        return p;
    }

    // ---------------- structure ----------------

    @Test
    @Order(1)
    @DisplayName("LT-01 The Location tool opens with its tab strip")
    void opens() {
        LocationToolPage p = page();
        assertTrue(p.isLoaded(Duration.ofSeconds(10)), "Tool did not load");
    }

    @Test
    @Order(2)
    @DisplayName("LT-02 Three tabs in order: Select, Shortlist, Final")
    void tabsInOrder() {
        LocationToolPage p = page();
        List<String> tabs = p.tabLabels();
        assumeTrue(!tabs.isEmpty(), "No tab labels read");
        assertEquals(3, tabs.size(), "Expected exactly three tabs, got " + tabs);
        assertEquals(LocationToolPage.TABS_IN_ORDER, tabs,
                "Tab order drives which stages reach the Map; got " + tabs);
    }

    @Test
    @Order(3)
    @DisplayName("LT-02b The tab labels are the SHORT forms, not the Map's spelled-out ones")
    void tabLabelsAreShortForms() {
        LocationToolPage p = page();
        List<String> tabs = p.tabLabels();
        assumeTrue(!tabs.isEmpty(), "No tab labels read");
        for (String t : tabs) {
            assertFalse(t.contains("Shoot Location"),
                    "Tabs use the tool's own wording; the spelled-out label belongs on the "
                            + "Map card only. Got: " + t);
        }
    }

    // ---------------- navigation between stages ----------------

    @Test
    @Order(4)
    @DisplayName("LT-04 Each of the three tabs opens and renders its own content")
    void everyTabOpens() {
        LocationToolPage p = page();
        for (String tab : LocationToolPage.TABS_IN_ORDER) {
            assumeTrue(p.selectTab(tab), "Could not select the " + tab + " tab");
            sleep(1500);
            assertTrue(p.isLoaded(Duration.ofSeconds(8)),
                    "Tool surface lost after selecting " + tab);
        }
        p.selectTab(LocationToolPage.TAB_SELECT);   // leave it where we found it
    }

    @Test
    @Order(5)
    @DisplayName("LT-12 The filter icon is available on the toolbar")
    void filterAvailable() {
        LocationToolPage p = page();
        assumeTrue(p.filterIconShown(), "No filter icon on this build");
        assertTrue(p.openFilter(), "Filter icon did not respond");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1000);
    }

    @Test
    @Order(6)
    @DisplayName("LT-13 Back from the Location tool returns to Tools, not out of the app")
    void backReturnsToTools() {
        page();
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(2000);
        String pkg = null;
        try { pkg = driver.getCurrentPackage(); } catch (Exception ignored) { }
        assertFalse(pkg == null || !pkg.contains("zillit"),
                "BACK from the Location tool left the app entirely (package=" + pkg + ")");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
