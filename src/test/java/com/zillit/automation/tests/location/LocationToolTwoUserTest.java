package com.zillit.automation.tests.location;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.location.LocationEntriesPage;
import com.zillit.automation.pages.map.LocationToolPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The Location tool with TWO accounts: A = emulator (Vidya Testing) creates / moves / edits,
 * B = Pixel (Vidya Pixel, admin) watches with the same screen open and, as admin, deletes
 * A's entry. Proves the socket refresh paths ({@code BaseSocketListener.locationObserver},
 * {@code locationUpdateObserver}, {@code locationDeleteObserver} → {@code getList()}) and the
 * admin delete of another member's entry ({@code LibraryPage}: uploadedBy == me || isAdmin).
 *
 * <p>Writes one ZL_AUTO location and deletes it (both tabs) before it ends; nobody's rights change.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC \
 *   ./run-and-report.sh --only LocationToolTwoUserTest
 * </pre>
 */
@Order(TestOrder.LOCATION_TOOL_TWO_USER)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class LocationToolTwoUserTest {

    private static final Duration LIVE = Duration.ofSeconds(60);
    private static final String EP = "1";
    private static final String SCENE = "903";
    private static final String DESC = "ZL_AUTO 2u description";
    private static final String DESC_EDITED = "ZL_AUTO 2u description edited";

    private AndroidDriver a;   // actor (emulator)
    private AndroidDriver b;   // watcher (Pixel, admin)
    private String pkg;
    private boolean ready;

    private final String loc = "ZL_AUTO_2u " + (System.currentTimeMillis() % 100000);
    private boolean createdLoc;
    private boolean inShortlist;

    @BeforeAll
    void setUp() {
        String bUdid = first(Config.get("zillit.receiver.udid"), System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(bUdid != null && !bUdid.isBlank(),
                "Two-device cases need ZILLIT_RECEIVER_UDID (the second device) — skipping");
        String aUdid = first(Config.get("zillit.sender.udid"), System.getenv("ZILLIT_SENDER_UDID"),
                System.getenv("ZILLIT_UDID"), Config.get("zillit.udid"), "emulator-5554");
        a = DriverFactory.create(aUdid, 8340);
        b = DriverFactory.create(bUdid, 8350);
        pkg = Config.configuredAppPackage();
        if (pkg == null || pkg.isEmpty()) pkg = a.getCurrentPackage();
        ready = enterProject(a) && enterProject(b);
        System.out.println(">>> Location two-user setup: A=" + aUdid + " B=" + bUdid + " ready=" + ready);
    }

    @AfterAll
    void tearDown() {
        try {
            if (createdLoc && a != null) {
                for (String tab : LocationToolPage.TABS_IN_ORDER) deleteFrom(a, tab);
            }
        } catch (Throwable t) {
            System.out.println(">>> two-user cleanup failed: " + t.getMessage());
        }
        for (AndroidDriver d : new AndroidDriver[]{a, b}) {
            try { if (d != null) { park(d); d.quit(); } } catch (Exception ignored) { }
        }
    }

    // ───────────────────────── cases ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("[LOC-2U-01] A location A saves in Selects appears on B's open Episode 1 listing without B navigating")
    void createSeenLive() {
        assumeTrue(ready, "Both devices are not inside the project");
        LocationToolPage pb = episode(b, LocationToolPage.TAB_SELECT);
        assumeTrue(pb != null, "B could not open Selects › Episode No : " + EP);
        assumeTrue(!pb.rowShown(loc), "The name is already listed");

        LocationToolPage pa = tab(a, LocationToolPage.TAB_SELECT);
        assumeTrue(pa.openAddForm(), "A's + form did not open");
        createdLoc = true;
        pa.setField(LocationToolPage.FIELD_NAME, loc);
        pa.setField(LocationToolPage.FIELD_SCENE, SCENE);
        pa.setField(LocationToolPage.FIELD_EPISODE, EP);
        pa.setField("descInc", DESC);
        assumeTrue(pa.addLinkAttachment("https://example.com/zl-auto-2u"), "A could not add a link attachment");
        assertTrue(pa.submitForm(), "A's Submit not tappable");
        for (int i = 0; i < 10 && pa.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        assumeTrue(!pa.formShown(Duration.ofSeconds(1)), "A's form refused the location: " + pa.snackbarText());

        assertTrue(waitLive(pb, loc), "B's open Selects › Episode " + EP + " listing never showed A's new \"" + loc + "\"");
    }

    @Test
    @Order(2)
    @DisplayName("[LOC-2U-02] A's Move To Shortlist appears on B's open Shortlist Episode 1 listing")
    void moveSeenLive() {
        assumeTrue(createdLoc, "LOC-2U-01 created nothing");
        LocationToolPage pb = episode(b, LocationToolPage.TAB_SHORTLIST);
        assumeTrue(pb != null, "B could not open Shortlist › Episode No : " + EP
                + " (no Episode 1 group in Shortlist yet)");

        LocationToolPage pa = row(a, LocationToolPage.TAB_SELECT);
        assumeTrue(pa != null, "A cannot find its location in Selects");
        assumeTrue(pa.rowMenu(loc, "Move To Shortlist"), "A: 'Move To Shortlist' not offered");
        if (!pa.formShown(Duration.ofSeconds(6))) pa.confirmDialog(Duration.ofSeconds(3));
        assumeTrue(pa.formShown(Duration.ofSeconds(10)), "A's move form did not open");
        assertTrue(pa.submitForm(), "A's Submit not tappable");
        for (int i = 0; i < 10 && pa.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        assumeTrue(!pa.formShown(Duration.ofSeconds(1)), "A's move was refused: " + pa.snackbarText());
        inShortlist = true;

        assertTrue(waitLive(pb, loc), "B's open Shortlist listing never showed A's moved \"" + loc + "\"");
    }

    @Test
    @Order(3)
    @DisplayName("[LOC-2U-03] A's edited description is what B reads on the entry")
    void editSeenByOther() {
        assumeTrue(inShortlist, "The location is not in Shortlist");
        LocationEntriesPage ea = entries(a, LocationToolPage.TAB_SHORTLIST);
        assumeTrue(ea != null, "A's entries page did not open");
        assumeTrue(ea.entryAction(0, "Edit") && ea.formShown(Duration.ofSeconds(10)), "A's Edit did not open");
        ea.setField("descInc", DESC_EDITED);
        assertTrue(ea.submitForm(), "A's Submit not tappable");
        for (int i = 0; i < 10 && ea.formShown(Duration.ofSeconds(1)); i++) sleep(1500);
        assumeTrue(!ea.formShown(Duration.ofSeconds(1)), "A's edit was refused: " + ea.snackbarText());

        long end = System.currentTimeMillis() + LIVE.toMillis();
        List<String> seen = List.of();
        while (System.currentTimeMillis() < end) {
            LocationEntriesPage eb = entries(b, LocationToolPage.TAB_SHORTLIST);
            if (eb != null) {
                seen = eb.descriptions();
                if (seen.stream().anyMatch(d -> d.contains(DESC_EDITED))) return;
            }
            sleep(4000);
        }
        throw new AssertionError("B never read A's edited description; B sees " + seen);
    }

    @Test
    @Order(4)
    @DisplayName("[LOC-2U-04] B (admin) may delete A's entry; A's open Shortlist listing loses it live")
    void adminDeletesOthersEntry() {
        assumeTrue(inShortlist, "The location is not in Shortlist");
        LocationToolPage pa = row(a, LocationToolPage.TAB_SHORTLIST);
        assumeTrue(pa != null, "A cannot find its location in Shortlist");
        pa.search("");   // leave A on the plain listing

        LocationEntriesPage eb = entries(b, LocationToolPage.TAB_SHORTLIST);
        assumeTrue(eb != null, "B's entries page did not open");
        List<String> items = eb.openEntryMenu(0);
        assertTrue(items.contains("Delete"), "B (admin) is not offered Delete on A's entry: " + items);
        eb.tapPopupItem("Delete");
        assertTrue(eb.dialogShown(Duration.ofSeconds(6)), "No delete confirmation on B");
        eb.tapDialogButton(true);
        assertTrue(eb.waitEntryCount(0, Duration.ofSeconds(30)) || !eb.isEntriesPage(Duration.ofSeconds(2)),
                "B's delete did not remove the entry");
        inShortlist = false;

        long end = System.currentTimeMillis() + LIVE.toMillis();
        while (System.currentTimeMillis() < end && pa.rowShown(loc)) sleep(2000);
        assertFalse(pa.rowShown(loc), "A's open Shortlist listing still shows \"" + loc + "\" after B deleted it");
    }

    @Test
    @Order(5)
    @DisplayName("[LOC-2U-05] A deletes the Selects copy; B's open Selects listing loses it live")
    void deleteSeenLive() {
        assumeTrue(createdLoc, "Nothing to delete");
        LocationToolPage pb = episode(b, LocationToolPage.TAB_SELECT);
        assumeTrue(pb != null && waitLive(pb, loc), "B does not list the Selects copy");
        LocationToolPage pa = row(a, LocationToolPage.TAB_SELECT);
        assumeTrue(pa != null, "A cannot find the Selects copy");
        assumeTrue(pa.rowMenu(loc, "Delete") && pa.dialogShown(Duration.ofSeconds(6)), "A: Delete not offered");
        pa.tapDialogButton(true);
        assertTrue(pa.waitForRowGone(loc, Duration.ofSeconds(30)), "A's delete did not remove the Selects copy");
        pb.search("");
        long end = System.currentTimeMillis() + LIVE.toMillis();
        while (System.currentTimeMillis() < end && pb.rowShown(loc)) sleep(2000);
        assertFalse(pb.rowShown(loc), "B's open Selects listing still shows \"" + loc + "\" after A deleted it");
        boolean anyLeft = false;
        for (String t : LocationToolPage.TABS_IN_ORDER) anyLeft |= row(a, t) != null;
        if (!anyLeft) createdLoc = false;
    }

    // ───────────────────────── helpers ─────────────────────────

    /** Polls the open listing without navigating; searches only after half the window (long lists scroll). */
    private boolean waitLive(LocationToolPage p, String name) {
        long end = System.currentTimeMillis() + LIVE.toMillis();
        long half = System.currentTimeMillis() + LIVE.toMillis() / 2;
        while (System.currentTimeMillis() < end) {
            if (p.rowShown(name)) return true;
            if (System.currentTimeMillis() > half && p.hasRow(name)) return true;
            sleep(2000);
        }
        return false;
    }

    private boolean onRoot(AndroidDriver d) {
        try {
            return !d.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg
                    + ":id/tabLayout']//android.widget.TextView[@text='Selects']")).isEmpty();
        } catch (Exception e) { return false; }
    }

    private boolean onNav(AndroidDriver d) {
        try { return !d.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private LocationToolPage tool(AndroidDriver d) {
        LocationToolPage t = new LocationToolPage(d);
        if (onRoot(d)) return t;
        for (int i = 0; i < 6 && !onNav(d); i++) {
            if (onRoot(d)) return t;
            try { d.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        if (onRoot(d)) return t;
        assumeTrue(onNav(d) || enterProject(d), "Could not get back to the bottom nav");
        ToolsPage tools = new ToolsPage(d);
        assumeTrue(tools.openToolsTab(), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Location");
        assumeTrue(tools.isModuleListed("Location", Duration.ofSeconds(6)), "Location tool not enabled for this user");
        assumeTrue(t.tapToolRow("Location"), "The Location row could not be tapped");
        assumeTrue(t.isLoaded(Duration.ofSeconds(25)), "Location tool did not load");
        return t;
    }

    private LocationToolPage tab(AndroidDriver d, String tab) {
        LocationToolPage t = tool(d);
        assumeTrue(t.selectTab(tab), "Could not select " + tab);
        sleep(2500);
        return t;
    }

    private LocationToolPage episode(AndroidDriver d, String tab) {
        LocationToolPage t = tab(d, tab);
        t.clearSearch();
        if (t.openEpisodeGroup(EP)) return t;
        sleep(3000);
        return t.openEpisodeGroup(EP) ? t : null;
    }

    private LocationToolPage row(AndroidDriver d, String tab) {
        long end = System.currentTimeMillis() + 20000;
        do {
            LocationToolPage t = episode(d, tab);
            if (t != null && t.waitForRow(loc, Duration.ofSeconds(5))) return t;
            sleep(2000);
        } while (System.currentTimeMillis() < end);
        return null;
    }

    private LocationEntriesPage entries(AndroidDriver d, String tab) {
        LocationToolPage t = row(d, tab);
        if (t == null || !t.openRow(loc)) return null;
        LocationEntriesPage e = new LocationEntriesPage(d);
        return e.isEntriesPage(Duration.ofSeconds(15)) ? e : null;
    }

    private void deleteFrom(AndroidDriver d, String tab) {
        if (!loc.startsWith("ZL_AUTO")) return;
        LocationToolPage t = row(d, tab);
        if (t == null) return;
        if (t.rowMenu(loc, "Delete") && t.dialogShown(Duration.ofSeconds(6))) {
            t.tapDialogButton(true);
            t.waitForRowGone(loc, Duration.ofSeconds(20));
        }
    }

    private void park(AndroidDriver d) {
        for (int i = 0; i < 8 && !onNav(d); i++) {
            try { d.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
    }

    private boolean enterProject(AndroidDriver d) {
        try {
            long end = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < end) {
                if (onNav(d) || !d.findElements(AppiumBy.id(pkg + ":id/projectListRecyclerView")).isEmpty()) break;
                try { d.navigate().back(); } catch (WebDriverException ignored) { }
                sleep(1500);
                if (!pkg.equals(d.getCurrentPackage())) d.executeScript("mobile: activateApp", Map.of("appId", pkg));
            }
            if (!d.findElements(AppiumBy.id(pkg + ":id/projectListRecyclerView")).isEmpty()) {
                ProjectListPage list = new ProjectListPage(d);
                List<String> names = list.visibleProjectNames();
                if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
            }
            return new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(60));
        } catch (WebDriverException e) {
            return false;
        }
    }

    private static String first(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
