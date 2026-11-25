package com.zillit.automation.tests.location;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.location.LocationEntriesPage;
import com.zillit.automation.pages.map.LocationToolPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Navigation and fixture handling shared by the Location-tool suites.
 *
 * <p>Levels on a TELEVISION project (ZL-Android-Automation is one):
 * <pre>
 *   LocationPage          tabs Selects / Shortlist / Final, rows "Episode No : N"
 *   LocationListingPage   one episode: rows "Location Name : X" / "Scene No. 901"
 *   LibraryPage           one location: its entries (info block + imgMore menu)
 *   LocationDetailPage    the add / edit / move / view form
 * </pre>
 *
 * <p>DATA SAFETY: every location these suites create is named {@code ZL_AUTO_…}; the
 * cleanup deletes by that exact (unique) name only, from the listing page of the episode
 * it was filed under, in every tab ("Move" leaves a copy behind). Nothing else is deleted,
 * and Map cities are never touched.
 */
@ExtendWith(ReasonLogger.class)
public abstract class LocationBaseTest extends BaseTest {

    protected static final String EP = "1";
    protected static final String LINK = "https://example.com/zl-auto-location-tool";

    /** Every ZL_AUTO location this run created, by name → episodes it may be filed under. */
    protected final java.util.Map<String, Set<String>> created = new java.util.LinkedHashMap<>();

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachProject() {
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
    }

    protected static String uniqueName(String kind) {
        return "ZL_AUTO_" + kind + " " + (System.currentTimeMillis() % 100000);
    }

    // ───────────────────────── where am I ─────────────────────────

    protected boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    protected boolean onProjectList() {
        try { return !driver.findElements(byAppId("projectListRecyclerView")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** On LocationPage itself: the tab strip with a "Selects" tab. */
    protected boolean onToolRoot() {
        try {
            return !driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage
                    + ":id/tabLayout']//android.widget.TextView[@text='Selects']")).isEmpty();
        } catch (Exception e) { return false; }
    }

    // ───────────────────────── getting there ─────────────────────────

    protected LocationToolPage openTool() {
        LocationToolPage tool = new LocationToolPage(driver);
        if (onToolRoot()) return tool;
        for (int i = 0; i < 6 && !onBottomNavScreen(); i++) {
            try { clearBlockingPopup(); } catch (Exception ignored) { }
            if (onToolRoot()) return tool;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
            if (onToolRoot()) return tool;
        }
        assumeTrue(recoverToBottomNav(), "The app could not be returned to a bottom-nav screen");
        ToolsPage tools = new ToolsPage(driver);
        if (!tools.openToolsTab()) { clearBlockingPopup(); sleep(600); }
        assumeTrue(tools.openToolsTab() || tools.isLoaded(Duration.ofSeconds(3)), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Location");
        assumeTrue(tools.isModuleListed("Location", Duration.ofSeconds(6)),
                "Location tool not enabled on this project");
        assumeTrue(tool.tapToolRow("Location"), "The Location row could not be tapped");
        assumeTrue(tool.isLoaded(Duration.ofSeconds(25)), "Location tool did not load");
        return tool;
    }

    protected LocationToolPage openTab(String tab) {
        LocationToolPage tool = openTool();
        assumeTrue(tool.selectTab(tab), "Could not select the " + tab + " tab");
        sleep(2500);
        return tool;
    }

    /** Tab → "Episode No : ep" listing page; null when that episode is not in the tab. */
    protected LocationToolPage openEpisode(String tab, String ep) {
        LocationToolPage tool = openTab(tab);
        tool.clearSearch();
        if (tool.openEpisodeGroup(ep)) return tool;
        sleep(3000);
        return tool.openEpisodeGroup(ep) ? tool : null;
    }

    /** The listing page of {@code ep} in {@code tab} showing {@code name}, or null. */
    protected LocationToolPage rowInTab(String tab, String name, String ep, Duration wait) {
        long deadline = System.currentTimeMillis() + wait.toMillis();
        do {
            LocationToolPage tool = openEpisode(tab, ep);
            if (tool != null && tool.waitForRow(name, Duration.ofSeconds(6))) return tool;
            sleep(2500);
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    /** Tab → episode → the location's entries page, or null. */
    protected LocationEntriesPage entries(String tab, String name, String ep) {
        LocationToolPage tool = rowInTab(tab, name, ep, Duration.ofSeconds(20));
        if (tool == null) return null;
        if (!tool.openRow(name)) return null;
        LocationEntriesPage e = new LocationEntriesPage(driver);
        return e.isEntriesPage(Duration.ofSeconds(15)) ? e : null;
    }

    // ───────────────────────── fixtures ─────────────────────────

    /**
     * Creates a location through the tab's "+" with a link attachment.
     *
     * @return true when the form accepted it and closed
     */
    protected boolean createLocation(String tab, String name, String scene, String episode,
                                     String city, String address, String description) {
        LocationToolPage tool = openTab(tab);
        assumeTrue(tool.openAddForm(), "The add (+) form did not open from " + tab);
        // Recorded BEFORE submitting: a slow list must not leave a stray row uncleaned.
        created.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(episode == null ? EP : episode);
        if (!tool.setField(LocationToolPage.FIELD_NAME, name)) return false;
        if (scene != null) tool.setField(LocationToolPage.FIELD_SCENE, scene);
        if (episode != null) tool.setField(LocationToolPage.FIELD_EPISODE, episode);
        if (address != null) tool.setField(LocationToolPage.FIELD_ADDRESS, address);
        if (city != null) tool.setField(LocationToolPage.FIELD_CITY, city);
        if (description != null) tool.setField("descInc", description);
        assumeTrue(tool.addLinkAttachment(LINK), "Could not add a link attachment (Upload Media → Link)");
        if (!tool.submitForm()) return false;
        sleep(4000);
        for (int i = 0; i < 10 && tool.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        return !tool.formShown(Duration.ofSeconds(1));
    }

    /**
     * Deletes the ZL_AUTO location from every tab it is listed in (listing-page row menu →
     * Delete → Yes). Refuses any name without the ZL_AUTO prefix.
     *
     * @return true when no tab lists it any more
     */
    protected boolean deleteEverywhere(String name, Set<String> episodes) {
        if (name == null || !name.startsWith("ZL_AUTO")) {
            throw new IllegalArgumentException("Refusing to delete a non-ZL_AUTO location: " + name);
        }
        boolean allGone = true;
        for (String ep : episodes) {
            for (String tab : LocationToolPage.TABS_IN_ORDER) {
                LocationToolPage tool = rowInTab(tab, name, ep, Duration.ofSeconds(4));
                if (tool == null) continue;
                List<String> items = tool.openRowMenu(name);
                if (!items.contains("Delete")) { if (!items.isEmpty()) tool.back(); allGone = false; continue; }
                tool.tapPopupItem("Delete");
                if (tool.dialogShown(Duration.ofSeconds(6))) tool.tapDialogButton(true);
                sleep(3500);
                tool.search("");
                if (!tool.waitForRowGone(name, Duration.ofSeconds(25))) allGone = false;
            }
        }
        return allGone;
    }

    /** Deletes everything this run created; never throws. */
    protected void sweepCreated() {
        for (java.util.Map.Entry<String, Set<String>> e : new ArrayList<>(created.entrySet())) {
            try {
                if (deleteEverywhere(e.getKey(), e.getValue())) created.remove(e.getKey());
                else System.out.println(">>> leftover ZL_AUTO location not fully removed: " + e.getKey());
            } catch (Throwable t) {
                System.out.println(">>> sweep of " + e.getKey() + " failed: " + t.getMessage());
            }
        }
    }

    @AfterAll
    void sweepAndPark() {
        try { if (sessionAlive()) sweepCreated(); } catch (Throwable ignored) { }
        try {
            for (int i = 0; i < 8 && !onBottomNavScreen(); i++) {
                try { clearBlockingPopup(); } catch (Exception ignored) { }
                driver.navigate().back();
                sleep(900);
            }
        } catch (Throwable ignored) { }
    }

    // ───────────────────────── recovery ─────────────────────────

    protected boolean recoverToBottomNav() {
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        if (onBottomNavScreen()) return true;
        for (int i = 0; i < 6 && !onBottomNavScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.contains("zillit")) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(2500);
            }
        } catch (Exception ignored) { }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
        try {
            driver.executeScript("mobile: terminateApp", java.util.Map.of("appId", appPackage));
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
        } catch (Exception e) {
            return false;
        }
        landing = settleLanding(90, false);
        return onBottomNavScreen() || enterProjectFromList();
    }

    private boolean enterProjectFromList() {
        if (!onProjectList()) return false;
        ProjectListPage list = new ProjectListPage(driver);
        List<String> names = list.visibleProjectNames();
        if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
        return new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(45));
    }

    protected static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
