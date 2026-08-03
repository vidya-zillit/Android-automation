package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
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
 * Project List — the screen logged-in users pick their project from.
 * Requires a logged-in app with projects (the E2E create test produces that
 * state); every case skips with a clear message when the state is missing.
 * Runs after the E2E, on one shared session.
 */
@Order(TestOrder.PROJECT_LIST)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectListTest extends BaseTest {

    private static final Duration ENTER_TIMEOUT = Duration.ofSeconds(60);

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the list page, skipping the test when the app isn't on it. */
    private ProjectListPage listPage() {
        assumeTrue(landing == Landing.PROJECT_LIST,
                "Project list not shown (landing=" + landing + "). It appears when the "
                        + "app is logged in with projects — run CreateProjectE2ETest first "
                        + "(twice for a multi-project list).");
        return new ProjectListPage(driver);
    }

    @Test
    @Order(1)
    @DisplayName("Project list shows projects, search and Start/Join actions")
    void listElementsVisible() {
        ProjectListPage page = listPage();
        assertTrue(page.isLoaded(), "Project list should be visible");
        List<String> names = page.visibleProjectNames();
        assertTrue(!names.isEmpty(), "At least one project row should be listed");
        assertTrue(page.isSearchVisible(), "Search field should be visible");
        assertTrue(page.isStartProjectVisible(), "Start Project button should be visible");
        assertTrue(page.isJoinProjectVisible(), "Join Project button should be visible");
        System.out.println(">>> Projects listed: " + names);
        screenshot("project-list");
    }

    @Test
    @Order(2)
    @DisplayName("Search filters the project list by name")
    void searchFiltersList() {
        ProjectListPage page = listPage();
        List<String> all = page.visibleProjectNames();
        assumeTrue(!all.isEmpty(), "No projects to filter");
        // Use a distinctive tail of the first project's name (e.g. its number).
        String probe = all.get(0).replaceAll("[^0-9]", "");
        assumeTrue(!probe.isEmpty(), "First project name has no unique digits to search by");
        page.search(probe);
        List<String> filtered = page.visibleProjectNames();
        assertTrue(!filtered.isEmpty(), "Matching project should remain while searching " + probe);
        assertTrue(filtered.stream().allMatch(n -> n.contains(probe)),
                "Every visible row should match the search, got: " + filtered);
        screenshot("project-list-filtered");
    }

    @Test
    @Order(3)
    @DisplayName("Search with a non-matching term shows no projects")
    void searchWithGarbageShowsEmpty() {
        ProjectListPage page = listPage();
        page.search("zzqqxx0099");
        assertTrue(page.isEmptyStateShown(),
                "A nonsense search term should leave no visible projects");
        screenshot("project-list-empty-search");
    }

    @Test
    @Order(4)
    @DisplayName("Clearing the search restores the full project list")
    void clearingSearchRestoresList() {
        ProjectListPage page = listPage();
        page.clearSearch();
        assertTrue(!page.visibleProjectNames().isEmpty(),
                "Projects should reappear after clearing the search");
    }

    @Test
    @Order(5)
    @DisplayName("Tapping a project enters it — toolbar shows the project name")
    void tapProjectEntersIt() {
        ProjectListPage page = listPage();
        List<String> names = page.visibleProjectNames();
        assumeTrue(!names.isEmpty(), "No project rows to tap");
        String target = names.get(0);
        assertTrue(page.tapProject(target), "Should be able to tap project: " + target);
        BottomNavPage main = new BottomNavPage(driver);
        boolean entered = main.waitUntilLoaded(ENTER_TIMEOUT);
        if (!entered) {
            screenshot("project-enter-timeout"); // capture what blocked the entry
        }
        assertTrue(entered, "Main project screen should open for: " + target);
        String expected = target.split(" - ")[0]; // rows append the role ("… - Admin")
        assertTrue(main.currentProjectName().contains(expected),
                "Toolbar should show '" + expected + "', got '" + main.currentProjectName() + "'");
        System.out.println(">>> Entered project: " + main.currentProjectName());
        screenshot("project-entered");
    }

    @Test
    @Order(6)
    @DisplayName("'Change Project' switches to another project from inside the app")
    void changeProjectSwitches() {
        assumeTrue(landing == Landing.PROJECT_LIST,
                "Needs the multi-project state used by the previous tests");
        BottomNavPage main = new BottomNavPage(driver);
        assumeTrue(main.waitUntilLoaded(Duration.ofSeconds(10)),
                "Must already be inside a project (previous test)");
        String before = main.currentProjectName();

        ProjectListPage list = main.tapChangeProject();
        assertTrue(list.isLoaded(), "Change Project should open the project list");
        List<String> others = list.visibleProjectNames().stream()
                .filter(n -> !n.contains(before)).toList();
        assumeTrue(!others.isEmpty(), "No second project to switch to");

        assertTrue(list.tapProject(others.get(0)), "Should tap: " + others.get(0));
        assertTrue(main.waitUntilLoaded(ENTER_TIMEOUT), "Switched project should open");
        String after = main.currentProjectName();
        assertTrue(!after.isEmpty() && !after.equals(before),
                "Toolbar project should change (was '" + before + "', now '" + after + "')");
        System.out.println(">>> Switched from '" + before + "' to '" + after + "'");
        screenshot("project-switched");
    }

    @Test
    @Order(7)
    @DisplayName("Relaunching the app returns to the project list (multi-project account)")
    void relaunchReturnsToList() {
        relaunchApp();
        assumeTrue(landing != Landing.BOTTOM_NAV,
                "Single-project accounts reopen straight into the project — list not expected");
        assertTrue(landing == Landing.PROJECT_LIST,
                "Multi-project account should land back on the project list, got " + landing);
        assertTrue(new ProjectListPage(driver).isLoaded(), "Project list should be shown again");
    }
}
