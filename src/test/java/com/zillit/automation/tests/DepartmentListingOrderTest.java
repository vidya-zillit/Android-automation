package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DepartmentListingOrderPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "Change Department Listing Order for Crew List".
 *
 * A ladder walked on ONE screen instance: Test Case 2 continues in the SAME
 * priority popup Test Case 1 opens (real app behavior — an invalid priority
 * keeps the popup open with the typed value still shown; only a valid save
 * closes it). Test Case 3 taps Reset first so its "Art at position 9"
 * precondition holds regardless of what Test Case 2 changed.
 */
@Order(TestOrder.DEPARTMENT_LISTING_ORDER)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepartmentListingOrderTest extends BaseTest {

    private static final String ASSISTANT_DIRECTORS = "Assistant Directors";
    private static final String INVALID_PRIORITY_ERROR = "Please set valid priority to update";

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Department Listing screen, navigating (or recovering) to it as needed. */
    private DepartmentListingOrderPage dept() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                // Prefer "ZL TESTING Automation" — other accounts on this device
                // hold auto-generated E2E projects with far longer department
                // lists, where "Art at position 9" would not hold by default.
                String target = names.stream()
                        .filter(n -> n.contains("ZL TESTING"))
                        .findFirst()
                        .orElse(names.get(0));
                assumeTrue(list.tapProject(target), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        DepartmentListingOrderPage page = new DepartmentListingOrderPage(driver);
        // The priority popup lives in its own dialog window, which hides the
        // underlying departmentRecyclerView from UiAutomator2's single-window
        // traversal — so an open popup counts as "already on this screen" too.
        if (page.isLoaded() || page.isUpdatePopupOpen()) {
            return page;
        }
        // Recover: back out (confirming any pending "save changes?" prompt) until
        // Bottom Nav is visible again, then re-navigate the full path.
        for (int i = 0; i < 6 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            page.confirmYesIfShown(Duration.ofMillis(700));
            sleep(500);
        }
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(), "Admin Settings not present");
        assumeTrue(page.openChangeDepartmentListingOrder(),
                "Change Department Listing Order for Crew List not present");
        assumeTrue(page.isLoaded(), "Department listing screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Invalid priority (2222) is rejected")
    void invalidPriorityUpdateRejected() {
        DepartmentListingOrderPage page = dept();
        page.search("Assistant Director");
        assertTrue(page.tapUpdateFor("Assistant Director"), "Update should open the priority popup");
        assertTrue(page.isUpdatePopupOpen(), "Priority popup should be open");

        page.setPriority("2222");
        page.tapPopupSave();

        // The error is a transient Android Toast — not always caught by the
        // accessibility tree in time. The reliable fallback signal (same idiom
        // as CalendarPage's validation checks): an invalid priority does NOT
        // close the popup, unlike a valid one (see Test Case 2).
        boolean toastSeen = page.textShown(INVALID_PRIORITY_ERROR, Duration.ofSeconds(3));
        screenshot("dept-order-invalid-priority");
        assertTrue(toastSeen || page.isUpdatePopupOpen(),
                "Expected either the '" + INVALID_PRIORITY_ERROR + "' message, or the popup staying open");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Valid priority (1) moves Assistant Directors to position 1")
    void validPriorityUpdatesPosition() {
        DepartmentListingOrderPage page = dept();
        assertTrue(page.isUpdatePopupOpen(), "Popup from Test Case 1 should still be open");

        page.setPriority("1");
        page.tapPopupSave();
        screenshot("dept-order-valid-priority-saved");

        page.clearSearch();
        assertEquals(1, page.positionOf(ASSISTANT_DIRECTORS),
                ASSISTANT_DIRECTORS + " should now be at position 1");
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Drag-and-drop moves Art from position 9 to position 2")
    void dragAndDropReordersDepartment() {
        DepartmentListingOrderPage page = dept();

        // Test Case 2 may have shifted this screen's order — reset to the
        // default order first so this test's own precondition is guaranteed.
        assumeTrue(page.tapReset(), "Reset control not present");
        page.confirmYesIfShown(Duration.ofSeconds(3));
        page.clearSearch();
        assertEquals(9, page.positionOf("Art"), "Art should be at position 9 by default");

        assertTrue(page.dragDepartmentToPosition("Art", 2), "Drag should move Art to position 2");
        screenshot("dept-order-after-drag");

        page.tapMainSave();
        page.confirmYesIfShown(Duration.ofSeconds(3));
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)),
                "Save should redirect back to Admin Settings");
        screenshot("dept-order-after-save");
    }

    /** Returns the Admin Settings screen, continuing from Test Case 3 or recovering there if run standalone. */
    private DepartmentListingOrderPage adminSettings() {
        DepartmentListingOrderPage page = new DepartmentListingOrderPage(driver);
        if (page.isOnAdminSettings(Duration.ofSeconds(2))) {
            return page;
        }
        dept();
        page.tapBackButton();
        assumeTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Could not reach Admin Settings");
        return page;
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Reorder is reflected in Create New Department, then the order is reset")
    void reorderReflectedThenReset() {
        DepartmentListingOrderPage page = adminSettings();

        assertTrue(page.openCreateNewDepartment(), "Create New Department should open");
        int artPosition = page.referenceDepartmentNames().indexOf("Art") + 1; // 1-based, 0 if not found
        assertEquals(2, artPosition, "Art should be visible at position 2 in Create New Department");
        System.out.println(">>> SUCCESS: Art is visible at position 2 in Create New Department");
        screenshot("dept-order-createnewdept-art-position2");

        assertTrue(page.tapBackButton(), "Back button should return to Admin Settings");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Should be back on Admin Settings");

        assertTrue(page.openChangeDepartmentListingOrder(), "Change Department Listing Order should reopen");
        assertTrue(page.isLoaded(), "Department listing screen did not reopen");

        assumeTrue(page.tapReset(), "Reset control not present");
        page.confirmYesIfShown(Duration.ofSeconds(3));

        page.tapMainSave();
        page.confirmYesIfShown(Duration.ofSeconds(3));
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)),
                "Save should redirect back to Admin Settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
