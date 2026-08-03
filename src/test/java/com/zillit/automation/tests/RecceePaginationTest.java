package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.RecceFormPage;
import com.zillit.automation.pages.RecceListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RECCE PAGINATION — a pure BLACK-BOX check that needs only the APK.
 *
 * <p>Step 1 seeds N recces with strictly sequential, uniquely identifiable titles
 * ("QA Page 0001" … "QA Page 0300"). Seeding uses the DRAFT path because a draft
 * saves with no validation, which is by far the fastest create.
 *
 * <p>Step 2 then scrolls the list to the very bottom and collects every distinct
 * card title it can reach. If the app pages correctly, all N seeded recces are
 * reachable — in particular the HIGHEST-numbered one. If the app silently loads
 * only one fixed page, the reachable set plateaus and the last marker is never
 * found, which is exactly the failure this test is designed to catch.
 *
 * <p>Configure with env vars (both optional):
 * <ul>
 *   <li>{@code ZILLIT_RECCE_COUNT} — how many to seed / expect (default 120, i.e.
 *       just over a typical 100-item page so a single-page cap is exposed).</li>
 *   <li>{@code ZILLIT_RECCE_SEED=false} — skip seeding and only verify what is
 *       already there (use when the data was seeded server-side).</li>
 * </ul>
 *
 * <p>Seeding is RESUMABLE: it counts what already exists and only creates the
 * remainder, so an emulator hiccup mid-run doesn't force a restart from zero.
 */
@Order(TestOrder.RECCE_PAGINATION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecceePaginationTest extends BaseTest {

    /** Marker prefix — distinct from RecceTest's "QA Recce" data. */
    private static final String MARK = "QA Page";

    private boolean insideProject;

    private static int wantedCount() {
        String v = System.getenv("ZILLIT_RECCE_COUNT");
        try { return v == null || v.isBlank() ? 120 : Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return 120; }
    }

    private static boolean seedingEnabled() {
        return !"false".equalsIgnoreCase(System.getenv("ZILLIT_RECCE_SEED"));
    }

    private static String markerFor(int n) { return MARK + " " + String.format("%04d", n); }

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachRecce() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage l = new ProjectListPage(driver);
                List<String> names = l.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(l.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        if (new RecceListPage(driver).isLoaded(Duration.ofSeconds(2))) return;
        openRecce();
    }

    private void openRecce() {
        for (int i = 0; i < 4; i++) {
            if (new RecceListPage(driver).isLoaded(Duration.ofSeconds(1))) return;
            if (onBottomNav()) break;
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
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools did not load");
        tools.searchTools("Recce");
        assumeTrue(tools.openModule("Recce"), "Could not open Recce");
        assumeTrue(new RecceListPage(driver).isLoaded(Duration.ofSeconds(20)), "Recce list did not load");
    }

    private boolean onBottomNav() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private RecceListPage list() {
        RecceListPage p = new RecceListPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Recce list not loaded");
        return p;
    }

    private void backToList() {
        for (int i = 0; i < 4; i++) {
            if (new RecceListPage(driver).isLoaded(Duration.ofSeconds(2))) return;
            if (onBottomNav()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (!new RecceListPage(driver).isLoaded(Duration.ofSeconds(1))) openRecce();
    }

    // ---------------- step 1: seed ----------------

    @Test
    @Order(1)
    @DisplayName("SEED: create N recces with sequential markers (resumable, drafts for speed)")
    void seedRecces() {
        assumeTrue(seedingEnabled(), "ZILLIT_RECCE_SEED=false — using existing data, skipping seeding");
        int want = wantedCount();
        RecceListPage p = list();
        p.tapChip("All");

        // Resume: find the highest marker already present so a re-run tops up
        // instead of starting again.
        List<String> existing = p.collectAllTitlesByScrolling(200, 3);
        int have = 0;
        for (String t : existing) {
            if (t == null || !t.startsWith(MARK)) continue;
            try {
                int n = Integer.parseInt(t.replace(MARK, "").trim());
                if (n > have) have = n;
            } catch (NumberFormatException ignored) { }
        }
        System.out.println(">>> SEED: want=" + want + " already present (highest marker)=" + have);
        assumeTrue(have < want, "Already seeded to " + have + " — nothing to create");

        long t0 = System.currentTimeMillis();
        int created = 0, failed = 0;
        RecceListPage l = new RecceListPage(driver);
        for (int n = have + 1; n <= want; n++) {
            String title = markerFor(n);
            try {
                // Every step POLLS for the next screen instead of sleeping a fixed
                // amount — fixed sleeps were ~6.5s of the ~11.7s per recce.
                if (!l.isListUp() && !l.waitListUp(Duration.ofSeconds(8))) { backToList(); }
                RecceFormPage f = l.tapCreateFast(Duration.ofSeconds(12));
                if (f == null) { failed++; backToList(); continue; }
                if (!f.setTitleFast(title)) { failed++; backToList(); continue; }
                if (!f.saveDraftFast(Duration.ofSeconds(15))) { failed++; backToList(); continue; }
                if (!l.waitListUp(Duration.ofSeconds(10))) backToList();
                created++;
            } catch (Exception e) {
                failed++;
                System.out.println(">>> seed error at " + title + ": " + e.getMessage());
                backToList();
            }
            if (created % 25 == 0 && created > 0) {
                long ms = System.currentTimeMillis() - t0;
                System.out.println(">>> seeded " + created + "/" + (want - have)
                        + " (" + (ms / 1000) + "s elapsed, " + (ms / created) + " ms each, "
                        + failed + " failed)");
            }
        }
        System.out.println(">>> SEED DONE: created=" + created + " failed=" + failed
                + " in " + ((System.currentTimeMillis() - t0) / 1000) + "s");
        assertTrue(created > 0, "Seeding should create at least one recce");
    }

    // ---------------- step 2: verify pagination ----------------

    @Test
    @Order(2)
    @DisplayName("PAGINATION: every seeded recce is reachable by scrolling the list")
    void allSeededReccesAreReachable() {
        int want = wantedCount();
        RecceListPage p = list();
        p.tapChip("All");
        List<String> all = p.collectAllTitlesByScrolling(400, 4);
        List<String> markers = all.stream().filter(t -> t != null && t.startsWith(MARK)).distinct().toList();
        int highest = 0;
        for (String t : markers) {
            try {
                int n = Integer.parseInt(t.replace(MARK, "").trim());
                if (n > highest) highest = n;
            } catch (NumberFormatException ignored) { }
        }
        String last = markerFor(want);
        boolean lastReachable = markers.contains(last);
        System.out.println(">>> PAGINATION: cards reachable=" + all.size()
                + " | seeded markers reachable=" + markers.size()
                + " | highest marker reached=" + highest + " (expected " + want + ")"
                + " | last marker '" + last + "' reachable=" + lastReachable);
        screenshot("recce-pagination");

        assumeTrue(markers.size() > 0, "No seeded markers found — run the seed step first");
        if (!lastReachable || markers.size() < want) {
            System.out.println("!!! PAGINATION ISSUE: only " + markers.size() + " of " + want
                    + " seeded recces are reachable by scrolling (highest reached = " + highest
                    + "). Items beyond that are not loaded — the list appears to fetch a single"
                    + " fixed page with no load-more on scroll.");
        }
        assertTrue(lastReachable,
                "The LAST seeded recce '" + last + "' must be reachable by scrolling. "
                        + "Only " + markers.size() + " of " + want + " were reachable (highest="
                        + highest + ") — pagination is not loading further pages.");
    }

    @Test
    @Order(3)
    @DisplayName("PAGINATION: the All chip count matches what is actually reachable")
    void chipCountMatchesReachable() {
        RecceListPage p = list();
        p.tapChip("All");
        String chip = p.chipText("All");
        List<String> all = p.collectAllTitlesByScrolling(400, 4);
        System.out.println(">>> All chip = '" + chip + "' | reachable cards = " + all.size());
        // Pull the number out of e.g. "All  120"
        int chipNum = -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(chip);
        if (m.find()) chipNum = Integer.parseInt(m.group(1));
        assumeTrue(chipNum >= 0, "Could not read a count from the All chip ('" + chip + "')");
        if (chipNum != all.size())
            System.out.println("!!! COUNT MISMATCH: the All chip says " + chipNum
                    + " but only " + all.size() + " cards are reachable by scrolling.");
        assertTrue(chipNum == all.size(),
                "The All chip count (" + chipNum + ") should match the number of recces actually "
                        + "reachable (" + all.size() + ").");
    }

    @Test
    @Order(4)
    @DisplayName("PAGINATION: search finds a recce that lives beyond the first page")
    void searchFindsLateItem() {
        int want = wantedCount();
        RecceListPage p = list();
        p.tapChip("All");
        String late = markerFor(want);          // the last one seeded
        p.search(late);
        sleep(1500);
        boolean found = p.cardTitles().stream().anyMatch(t -> t.contains(late))
                || p.textShown(late, Duration.ofSeconds(6));
        System.out.println(">>> search for late item '" + late + "' found=" + found);
        p.clearSearch();
        if (!found)
            System.out.println("!!! SEARCH ISSUE: '" + late + "' cannot be found by search. If search "
                    + "filters only the already-loaded page, later items are unsearchable.");
        assertTrue(found,
                "Search should find '" + late + "' even though it is beyond the first page.");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
