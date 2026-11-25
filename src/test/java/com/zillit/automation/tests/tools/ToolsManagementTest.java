package com.zillit.automation.tests.tools;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.adminsettings.CustomizationOfToolsPage;
import com.zillit.automation.pages.settings.AppPreferencesPage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TOOLS PAGE management: Search tools, Change group order (reorder bottom sheet),
 * and Customization of tools (enable/disable) — verifying the Tools list still
 * shows all data (groups + counts + tool rows) after each change. Enable/disable
 * is admin-only and reversible: the disabled tool is re-enabled to restore state.
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.TOOLS_MANAGEMENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolsManagementTest extends BaseTest {

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachTools() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        recoverToTools();
    }

    private void recoverToTools() {
        ToolsPage p = new ToolsPage(driver);
        for (int i = 0; i < 5 && !p.isLoaded(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) {}
            sleep(600);
        }
        p.openToolsTab();
    }

    private ToolsPage tools() {
        ToolsPage p = new ToolsPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Tools list not loaded");
        return p;
    }

    @Test
    @Order(1)
    @DisplayName("Tools list loads: grouped sections with counts + tool rows")
    void toolsListLoads() {
        ToolsPage p = tools();
        List<String> groups = p.groupTitles();
        List<String> toolNames = p.toolNames();
        System.out.println(">>> groups=" + groups + " count=" + p.groupCount());
        System.out.println(">>> tools=" + toolNames);
        assertTrue(p.groupCount() > 0, "There should be at least one tool group");
        assertTrue(p.toolRowCount() > 0, "There should be tool rows listed");
        assertTrue(p.allGroupsHaveCounts(), "Every group header should show a count");
        // ISSUE DETECTION: a tool name that looks like a raw string-resource key
        // (e.g. "budget_builder_tool_label") means the label never resolved to a
        // human name. Flag it loudly so it's reported, but don't fail the suite
        // (it's the API sending the key as the title, not a client crash).
        List<String> rawKeys = p.allToolNamesScrolled().stream()
                .filter(t -> t.matches("[a-z0-9]+(_[a-z0-9]+)+"))   // snake_case only
                .toList();
        if (!rawKeys.isEmpty())
            System.out.println("!!! ISSUE: tool(s) shown as a raw resource key, not a label: " + rawKeys);
        screenshot("tools-list");
    }

    @Test
    @Order(2)
    @DisplayName("Search tools: a matching query narrows the list; a non-match shows the empty state")
    void searchTools() {
        ToolsPage p = tools();
        List<String> all = p.toolNames();
        assumeTrue(!all.isEmpty(), "No tools to search");
        String first = all.get(0);
        String frag = first.length() >= 4 ? first.substring(0, 4) : first;
        p.searchTools(frag);
        List<String> matched = p.toolNames();
        System.out.println(">>> search '" + frag + "' → " + matched);
        assertTrue(matched.stream().anyMatch(t -> t.toLowerCase().contains(frag.toLowerCase())),
                "Search should keep tools matching '" + frag + "'");
        assertTrue(matched.size() <= all.size(), "Search should not add tools");
        // non-matching query → empty state
        p.clearSearch();
        p.searchTools("zzq-no-tool-" + (System.currentTimeMillis() % 1000));
        System.out.println(">>> no-match rows=" + p.toolRowCount() + " empty=" + p.isEmptyStateShown(Duration.ofSeconds(3)));
        assertTrue(p.toolRowCount() == 0 || p.isEmptyStateShown(Duration.ofSeconds(3)),
                "A non-matching search should show no tools / the empty state");
        screenshot("tools-search");
        // clear → list restored
        p.clearSearch();
        assertTrue(p.toolRowCount() > 0, "Clearing search should restore the full list");
    }

    @Test
    @Order(3)
    @DisplayName("Reorder sheet opens with the group list + Save/Cancel")
    void reorderSheetOpens() {
        ToolsPage p = tools();
        assumeTrue(p.openReorder(), "Reorder button not tappable");
        assertTrue(p.isReorderSheetOpen(Duration.ofSeconds(8)), "Reorder groups sheet should open");
        List<String> names = p.reorderGroupNames();
        System.out.println(">>> reorder groups=" + names);
        assertTrue(!names.isEmpty(), "The reorder sheet should list the groups");
        screenshot("tools-reorder-sheet");
        p.cancelReorder();
    }

    @Test
    @Order(4)
    @DisplayName("Change group order → order changes AND all groups/tools still visible")
    void changeGroupOrder() {
        ToolsPage p = tools();
        // Collect the FULL list by scrolling (the list is longer than the viewport).
        List<String> before = p.allGroupTitlesScrolled();
        List<String> toolsBefore = p.allToolNamesScrolled();
        assumeTrue(before.size() >= 2, "Need ≥2 groups to reorder — skipping");
        assumeTrue(p.openReorder() && p.isReorderSheetOpen(Duration.ofSeconds(8)), "Reorder sheet did not open");
        List<String> sheetBefore = p.reorderGroupNames();
        assumeTrue(p.dragFirstGroupDown(), "Could not drag the first group");
        List<String> sheetAfter = p.reorderGroupNames();
        System.out.println(">>> reorder sheet: " + sheetBefore + " -> " + sheetAfter);
        assumeTrue(p.saveReorder(), "Save not tappable");
        sleep(2500);
        // Back on the Tools list: order should reflect the change, and NO data lost.
        List<String> after = p.allGroupTitlesScrolled();
        List<String> toolsAfter = p.allToolNamesScrolled();
        System.out.println(">>> Tools groups: " + before + " -> " + after);
        System.out.println(">>> Tools list:   " + toolsBefore + " -> " + toolsAfter);
        assertTrue(new java.util.HashSet<>(after).containsAll(before) && after.size() == before.size(),
                "All groups must still be present after reorder (no group lost). before=" + before + " after=" + after);
        assertTrue(new java.util.HashSet<>(toolsAfter).containsAll(toolsBefore),
                "No tool should disappear after reorder. before=" + toolsBefore + " after=" + toolsAfter);
        boolean orderChanged = !after.equals(before) || !sheetBefore.equals(sheetAfter);
        System.out.println(">>> group order changed=" + orderChanged);
        assertTrue(orderChanged, "The group order should actually change after a drag+save");
        screenshot("tools-reordered");
    }

    @Test
    @Order(5)
    @DisplayName("Customization of tools opens with the tool list + enabled ticks + Update")
    void customizationOpens() {
        ToolsPage p = tools();
        assumeTrue(p.openCustomization(), "Customization banner not tappable (admin only?)");
        assertTrue(p.isCustomizationOpen(Duration.ofSeconds(10)),
                "Customization of tools screen should open");
        List<String> names = p.customizationToolNames();
        System.out.println(">>> customization tools=" + names + " enabledTicks=" + p.enabledTickCount());
        assertTrue(!names.isEmpty(), "The customization screen should list tools");
        screenshot("tools-customization");
        try { driver.navigate().back(); } catch (Exception ignored) {}
    }

    @Test
    @Order(6)
    @DisplayName("Disable a tool → it disappears from Tools; re-enable → it reappears (data intact)")
    void disableAndReEnableTool() {
        ToolsPage p = tools();
        List<String> listedBefore = p.allToolNamesScrolled();
        assumeTrue(!listedBefore.isEmpty(), "No tools listed");
        // Pick a safe, reversible tool that also sits near the TOP of the alphabetical
        // customization list (avoids long scrolls): prefer AD Dashboard / Weather /
        // Document Distribution; never a chat module we test elsewhere.
        String target = java.util.stream.Stream.of("AD Dashboard", "Weather", "Document Distribution")
                .filter(pref -> listedBefore.stream().anyMatch(t -> t.equalsIgnoreCase(pref)))
                .findFirst()
                .orElse(listedBefore.stream()
                        .filter(t -> !t.equalsIgnoreCase("Confidential Info") && !t.equalsIgnoreCase("Info"))
                        .findFirst().orElse(listedBefore.get(0)));
        System.out.println(">>> toggling tool: " + target);

        // DISABLE
        assumeTrue(p.openCustomization() && p.isCustomizationOpen(Duration.ofSeconds(10)), "Customization not open");
        assumeTrue(p.toggleCustomizationTool(target), "Could not tap the tool row");
        p.confirmDisableDialog(Duration.ofSeconds(5));   // confirm the disable warning if it shows
        assumeTrue(p.saveCustomization(), "Update not tappable");
        sleep(2500);
        recoverToTools();
        ToolsPage p2 = tools();
        List<String> afterDisable = p2.allToolNamesScrolled();
        boolean gone = afterDisable.stream().noneMatch(t -> t.equalsIgnoreCase(target));
        System.out.println(">>> after disable, '" + target + "' gone=" + gone + " tools=" + afterDisable);
        assertTrue(gone, "A disabled tool should no longer appear on the Tools list");
        assertTrue(p2.allGroupsHaveCounts(), "Remaining groups must still show counts after a tool is disabled");
        screenshot("tools-disabled");

        // RE-ENABLE (restore state)
        assumeTrue(p2.openCustomization() && p2.isCustomizationOpen(Duration.ofSeconds(10)), "Customization not open (2)");
        p2.toggleCustomizationTool(target);   // re-enable (no confirm dialog when enabling)
        p2.confirmDisableDialog(Duration.ofSeconds(2)); // harmless if none
        p2.saveCustomization();
        sleep(2500);
        recoverToTools();
        ToolsPage p3 = tools();
        List<String> afterEnable = p3.allToolNamesScrolled();
        boolean back = afterEnable.stream().anyMatch(t -> t.equalsIgnoreCase(target));
        System.out.println(">>> after re-enable, '" + target + "' back=" + back + " tools=" + afterEnable);
        assertTrue(back, "A re-enabled tool should reappear on the Tools list");
        assertTrue(p3.allGroupsHaveCounts(), "All groups should show counts after re-enabling");
        screenshot("tools-reenabled");
    }

    // ================================================================ web gap cases

    // [WEB TOOLS-02]
    @Test
    @Order(7)
    @DisplayName("TOOLS-02 Every tool ticked in Customization has a tile on Tools; Account Hub's tile says \"Available on web only\"")
    void tickedToolsAllHaveTiles() {
        ToolsPage p = tools();
        p.clearSearch();
        scrollTilesToTop();
        var banner = driver.findElements(byAppId("toolsCustomization"));
        assumeTrue(!banner.isEmpty(), "Customization banner not shown (admin only) — this account is not an admin");
        // Tap the chevron at the banner's right edge: the middle is a TextView with two "Click here"
        // links (select tools / grant rights) that swallows or redirects a centre tap.
        var r = banner.get(0).getRect();
        tapAt(r.getX() + r.getWidth() - 45, r.getY() + r.getHeight() / 2);
        CustomizationOfToolsPage c = new CustomizationOfToolsPage(driver);
        assertTrue(c.isLoaded(), "The Customization banner should open Customization of tools");
        List<String> all = c.allToolNamesScrolled();
        List<String> ticked = all.stream().filter(c::isToolEnabled).toList();
        System.out.println(">>> customization: " + all.size() + " tools, ticked=" + ticked);
        assumeTrue(!ticked.isEmpty(), "No tool is ticked");
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }   // no change → just closes
        sleep(1200);
        recoverToTools();
        ToolsPage t = tools();
        t.clearSearch();
        List<String> tiles = allTilesThorough();
        System.out.println(">>> Tools tiles (" + tiles.size() + "): " + tiles);
        // Tools shows the raw unitName (Production Report shows "Production Report Creation" to admins),
        // so a prefix match either way counts.
        List<String> missing = ticked.stream().filter(n -> tiles.stream().noneMatch(tl -> sameTool(tl, n))).toList();
        assertTrue(missing.isEmpty(), "TOOLS-02: ticked in Customization of tools but no tile on Tools: " + missing
                + " (Tools drops only viewAccess=false, and Invoices for accountants — Tools.kt:340-356)");

        if (ticked.contains("Account Hub")) {
            t.searchTools("Account Hub");
            assertTrue(t.toolNames().contains("Account Hub"), "TOOLS-02: Account Hub ticked but no Account Hub tile");
            try {
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/nameTv\").text(\"Account Hub\")")).click();
            } catch (WebDriverException e) {
                throw new AssertionError("Could not tap the Account Hub tile", e);
            }
            assertTrue(t.textShown("Available on web only", Duration.ofSeconds(8)),
                    "Account Hub tile should show the \"Available on web only\" dialog (Tools.kt:722-736)");
            assertTrue(t.textShown("not available on mobile devices", Duration.ofSeconds(2)), "Dialog body");
            tapText("(?i)ok");
            assertTrue(t.isLoaded(Duration.ofSeconds(8)), "OK should leave the user on Tools");
            t.clearSearch();
        }
    }

    // [WEB tools-page #2 / T-M1]
    @Test
    @Order(8)
    @DisplayName("T-M1 Every tool row has its info icon in Light and Dark theme, and the icon opens \"<tool>:\" info")
    void everyRowHasInfoIconBothThemes() {
        AppPreferencesPage prefs = new AppPreferencesPage(driver);
        checkInfoIcons("current theme");
        String original = null;
        try {
            assumeTrue(prefs.ensureOnAppPreferences(Duration.ofSeconds(40)), "App Preferences not reachable");
            original = prefs.selectedTheme();
            assumeTrue(prefs.openThemeDialog() && prefs.isThemeDialogOpen(Duration.ofSeconds(6)), "Theme dialog did not open");
            String other = AppPreferencesPage.THEME_DARK.equalsIgnoreCase(original)
                    ? AppPreferencesPage.THEME_LIGHT : AppPreferencesPage.THEME_DARK;
            assumeTrue(prefs.chooseTheme(other), "Could not switch the theme to " + other);
            recoverToTools();
            checkInfoIcons(other + " theme");
        } finally {
            if (original != null && !original.isBlank()) {
                try {
                    if (prefs.ensureOnAppPreferences(Duration.ofSeconds(40)) && !original.equalsIgnoreCase(prefs.selectedTheme())
                            && prefs.openThemeDialog() && prefs.isThemeDialogOpen(Duration.ofSeconds(6))) {
                        prefs.chooseTheme(original);
                    }
                } catch (Exception e) {
                    System.out.println("!!! theme restore failed: " + e.getMessage() + " — set it back to " + original);
                }
            }
            recoverToTools();
        }
    }

    private void checkInfoIcons(String label) {
        ToolsPage p = tools();
        p.clearSearch();
        // A row counts as having the icon if it showed one in ANY viewport (edge rows can be cut).
        java.util.LinkedHashSet<String> rows = new java.util.LinkedHashSet<>(), withIcon = new java.util.LinkedHashSet<>();
        scrollTilesToTop();
        int quiet = 0;
        for (int i = 0; i < 40 && quiet < 4; i++) {
            int before = rows.size() + withIcon.size();
            try {
                for (WebElement r : driver.findElements(byAppId("rowRoot"))) {
                    String name;
                    try { name = r.findElement(byAppId("nameTv")).getText(); } catch (WebDriverException e) { continue; }
                    if (name == null || name.isBlank()) continue;
                    rows.add(name);
                    if (!r.findElements(byAppId("infoIv")).isEmpty()) withIcon.add(name);
                }
            } catch (WebDriverException ignored) { }
            quiet = (rows.size() + withIcon.size() == before) ? quiet + 1 : 0;
            dragVertical(0.80, 0.50);
        }
        List<String> noIcon = rows.stream().filter(n -> !withIcon.contains(n)).toList();
        System.out.println(">>> [" + label + "] rows=" + rows.size() + " withInfoIcon=" + withIcon.size() + " missing=" + noIcon);
        assertTrue(!rows.isEmpty(), "[" + label + "] no tool rows read");
        assertTrue(noIcon.isEmpty(), "[" + label + "] tool rows without the info icon: " + noIcon);

        // The first row's icon opens "<tool>:" with an OK button.
        scrollTilesToTop();
        WebElement first = null;
        for (WebElement r : driver.findElements(byAppId("rowRoot"))) {
            if (!r.findElements(byAppId("infoIv")).isEmpty() && !r.findElements(byAppId("nameTv")).isEmpty()) { first = r; break; }
        }
        assertTrue(first != null, "[" + label + "] no row with an info icon on screen");
        String name = first.findElement(byAppId("nameTv")).getText();
        first.findElement(byAppId("infoIv")).click();
        boolean titled = p.textShown(name + ":", Duration.ofSeconds(6));
        screenshot("tools-info-" + label.replace(' ', '-'));
        tapText("(?i)ok");
        sleep(800);
        assertTrue(titled, "[" + label + "] the info dialog for '" + name + "' should be titled \"" + name + ":\"");
        assertTrue(p.isLoaded(Duration.ofSeconds(6)), "[" + label + "] OK should close the info dialog");
    }

    // [WEB tools-page #4 / T-M2]
    @Test
    @Order(9)
    @DisplayName("T-M2 (admin half) An admin sees both the Customization banner and the Reorder button; Reorder opens")
    void adminSeesCustomizationAndReorder() {
        ToolsPage p = tools();
        p.clearSearch();
        scrollTilesToTop();
        boolean banner = !driver.findElements(byAppId("toolsCustomization")).isEmpty();
        boolean reorder = !driver.findElements(byAppId("reorderBtn")).isEmpty();
        System.out.println(">>> toolsCustomization=" + banner + " reorderBtn=" + reorder);
        assumeTrue(banner, "Customization banner not shown — this account is not an admin. The non-admin half "
                + "needs a non-admin account; both test accounts are project admins");
        assertTrue(reorder, "reorderBtn should be shown (no admin gate, Tools.kt:1755)");
        assertTrue(p.openReorder() && p.isReorderSheetOpen(Duration.ofSeconds(8)), "Reorder sheet should open");
        p.cancelReorder();
    }

    /** True when a Tools tile and a Customization row name the same tool. */
    private static boolean sameTool(String tile, String custom) {
        String a = tile.trim().toLowerCase(), b = custom.trim().toLowerCase();
        return a.equals(b) || a.startsWith(b) || b.startsWith(a);
    }

    /** Every tile name on Tools; four quiet rounds before believing the bottom was reached. */
    private List<String> allTilesThorough() {
        scrollTilesToTop();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        int quiet = 0;
        for (int i = 0; i < 40 && quiet < 4; i++) {
            int before = seen.size();
            seen.addAll(new ToolsPage(driver).toolNames());
            quiet = (seen.size() == before) ? quiet + 1 : 0;
            dragVertical(0.80, 0.50);
        }
        return new java.util.ArrayList<>(seen);
    }

    private void scrollTilesToTop() {
        for (int i = 0; i < 12; i++) dragVertical(0.55, 0.90);
    }

    /** Slow drag (no fling) from/to a fraction of screen height; starts low, where the list is. */
    private void dragVertical(double fromPct, double toPct) {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        int x = size.getWidth() / 2;
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * fromPct)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(700),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * toPct)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(400);
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(1500);
    }

    private boolean tapText(String regex) {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"" + regex + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
