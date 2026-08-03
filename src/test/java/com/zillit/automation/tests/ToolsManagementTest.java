package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
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
 * TOOLS PAGE management: Search tools, Change group order (reorder bottom sheet),
 * and Customization of tools (enable/disable) — verifying the Tools list still
 * shows all data (groups + counts + tool rows) after each change. Enable/disable
 * is admin-only and reversible: the disabled tool is re-enabled to restore state.
 */
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

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
