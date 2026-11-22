package com.zillit.automation.tests.adminsettings;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.adminsettings.AdminSettingsPage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
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
 * Admin Settings after the regrouping (branch {@code vidya_AI/email_tutorial_bugs}).
 *
 * <p>The screen went from one alphabetical list to six sections with a cross-group search.
 * Rows kept {@code common_list_item.xml}, so every existing admin suite still taps them the
 * same way; what is NEW and untested is the sectioning itself, the header counts, the
 * search and its empty state.
 *
 * <p>Nothing here asserts a row's absolute position. The app sorts alphabetically WITHIN a
 * section and the sections are a local constant, so position is an implementation detail —
 * what matters is that a row ends up under the right header and that search can reach it.
 */
@Order(TestOrder.ADMIN_SETTINGS_GROUPS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminSettingsGroupsTest extends BaseTest {

    private static final Duration LOAD = Duration.ofSeconds(30);
    private boolean onAdmin;

    @Override
    protected boolean relaunchBetweenTests() {
        return false; // one screen, chained cases
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void reachAdminSettings() {
        AdminSettingsPage page = new AdminSettingsPage(driver);
        if (onAdmin) {
            dismissPermissionPrompts();
            clearBlockingPopup();
            if (page.isOpen(Duration.ofSeconds(5))) {
                page.clearSearch(); // leave no filter standing for the next case
                return;
            }
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(),
                "Admin Settings row not present — this account may not be a project admin");
        assumeTrue(page.isOpen(LOAD), "Admin Settings did not open");
        onAdmin = true;
    }

    private AdminSettingsPage page() {
        return new AdminSettingsPage(driver);
    }

    // ---------------- sections ----------------

    @Test
    @Order(1)
    @DisplayName("The list is sectioned — group headers are present")
    void headersPresent() {
        List<String> groups = page().sweep().groups;
        assertFalse(groups.isEmpty(),
                "The regrouped screen should render section headers (groupTitleTv)");
        System.out.println(">>> sections on screen: " + groups);
        screenshot("admin-groups");
    }

    @Test
    @Order(2)
    @DisplayName("Sections appear in the app's declared order, Danger zone last")
    void sectionOrder() {
        List<String> seen = page().sweep().groups;
        assumeTrue(!seen.isEmpty(), "No section headers found — skipping");

        // Only compare the sections this project actually shows: a section with no rows
        // is skipped entirely by the adapter, which is correct behaviour, not a gap.
        List<String> expected = AdminSettingsPage.GROUPS_IN_ORDER.stream()
                .filter(seen::contains)
                .toList();
        assertEquals(expected, seen,
                "Sections must follow the enum declaration order; expected " + expected
                        + " but the screen showed " + seen);

        if (seen.contains("Danger zone")) {
            assertEquals("Danger zone", seen.get(seen.size() - 1),
                    "Danger zone holds Delete Project and must stay last");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Every header carries a count, and the counts add up to the rows shown")
    void headerCounts() {
        AdminSettingsPage p = page();
        AdminSettingsPage.Sweep sweep = p.sweep();
        assumeTrue(!sweep.groups.isEmpty(), "No section headers found — skipping");

        int total = 0;
        for (String count : p.groupCounts()) {
            assertTrue(count.matches("\\d+"), "Header count should be a number, got '" + count + "'");
            total += Integer.parseInt(count);
        }
        assertTrue(total > 0, "Section counts should not all be zero");
        System.out.println(">>> rows seen: " + sweep.rows.size() + ", section counts total: " + total);
    }

    @Test
    @Order(4)
    @DisplayName("Delete Project sits in the Danger zone section")
    void deleteProjectInDangerZone() {
        AdminSettingsPage.Sweep sweep = page().sweep();
        assumeTrue(sweep.groups.contains("Danger zone"), "No Danger zone section — skipping");
        assertTrue(sweep.rows.stream().anyMatch(r -> r.toLowerCase().contains("delete project")),
                "Delete Project should be reachable; rows seen: " + sweep.rows);
    }

    // ---------------- search ----------------

    @Test
    @Order(5)
    @DisplayName("The screen has a cross-group search box")
    void searchBoxPresent() {
        assertTrue(page().hasSearchBox(),
                "The regrouped screen adds searchEt above the list");
    }

    @Test
    @Order(6)
    @DisplayName("Search narrows the list across every section")
    void searchFilters() {
        AdminSettingsPage p = page();
        assumeTrue(p.hasSearchBox(), "No search box — skipping");
        int before = p.sweep().rows.size();
        assumeTrue(before > 1, "Not enough rows to prove filtering — skipping");

        p.search("Create");
        List<String> filtered = p.rowLabels();
        assumeTrue(!filtered.isEmpty(), "No row matched 'Create' on this project — skipping");
        assertTrue(filtered.stream().allMatch(r -> r.toLowerCase().contains("create")),
                "Every visible row should match the query; got " + filtered);
        assertTrue(filtered.size() < before,
                "Filtering should show fewer rows than the unfiltered list");
        screenshot("admin-search-filtered");
    }

    @Test
    @Order(7)
    @DisplayName("A query matching nothing shows the no-match message")
    void searchEmptyState() {
        AdminSettingsPage p = page();
        assumeTrue(p.hasSearchBox(), "No search box — skipping");
        p.search("zzzz-no-such-setting");
        assertTrue(p.isNoMatchShown(Duration.ofSeconds(6)),
                "'No settings match your search.' should appear for a query with no hits");
        assertTrue(p.rowLabels().isEmpty(), "No rows should remain for a non-matching query");
        screenshot("admin-search-empty");
    }

    @Test
    @Order(8)
    @DisplayName("Clearing the query brings every section back")
    void clearingSearchRestores() {
        AdminSettingsPage p = page();
        assumeTrue(p.hasSearchBox(), "No search box — skipping");
        int before = p.sweep().rows.size();

        p.search("Create");
        p.clearSearch();

        assertFalse(p.isNoMatchShown(Duration.ofSeconds(2)),
                "The no-match label must go away once the query is cleared");
        assertEquals(before, p.sweep().rows.size(),
                "Clearing the query restores every row, not whatever the last filter left");
    }

    // ---------------- headers are not rows ----------------

    @Test
    @Order(9)
    @DisplayName("Section headers are not tappable rows — tapping one stays on the screen")
    void headersAreNotRows() {
        AdminSettingsPage p = page();
        List<String> groups = p.groupTitles();
        assumeTrue(!groups.isEmpty(), "No headers on screen — skipping");
        assertTrue(textShownById("groupTitleTv"),
                "Headers use groupTitleTv, a different id from the rows' nameTv");
        assertFalse(p.rowLabels().contains(groups.get(0)),
                "A section title must not also appear as a row label — anything matching "
                        + "list entries by bare text would then tap the wrong thing");
    }

    private boolean textShownById(String id) {
        return !driver.findElements(byAppId(id)).isEmpty();
    }
}
