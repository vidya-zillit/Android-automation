package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateNewDepartmentPage;
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
 * Settings → Admin Settings → "Create New Department".
 *
 * A ladder walked on ONE screen instance (relaunchBetweenTests()=false): each
 * test case continues editing the same open form/list the previous one left.
 */
@Order(53)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateNewDepartmentTest extends BaseTest {

    private static final String MIN_LENGTH_ERROR = "Name must contain at least 3 characters";
    private static final String SUCCESS_MESSAGE = "Department has been created successfully.";
    private static final String DELETE_CONFIRM_MESSAGE = "Do You really want to delete this department?";
    private static final String DUPLICATE_NAME_ERROR = "Department already exists.";
    private static final String MAX_LENGTH_NAME = "1111111111111111111111111111111111111111";

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Create New Department screen, navigating (or recovering) to it as needed. */
    private CreateNewDepartmentPage department() {
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
        CreateNewDepartmentPage page = new CreateNewDepartmentPage(driver);
        if (page.isLoaded()) {
            return page;
        }
        // Recover: back out until Bottom Nav is visible again, then re-navigate.
        for (int i = 0; i < 6 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(), "Admin Settings not present");
        assumeTrue(page.openCreateNewDepartment(), "Create New Department not present");
        assumeTrue(page.isLoaded(), "Create New Department screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Verify Department Name minimum length validation")
    void minimumDepartmentNameLength() {
        CreateNewDepartmentPage page = department();

        page.setDepartmentName("AC");
        page.tapSubmit();

        screenshot("department-min-length");
        assertTrue(page.hasMessage(MIN_LENGTH_ERROR, Duration.ofSeconds(5)), "Expected error: " + MIN_LENGTH_ERROR);
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Verify Department Name field enforces maximum length and creates successfully")
    void maximumLengthDepartmentCreated() {
        CreateNewDepartmentPage page = department();

        page.setDepartmentName(MAX_LENGTH_NAME);
        // Attempt to set a longer value than the max length allows — the field's own
        // input filter must cap it at MAX_LENGTH_NAME regardless.
        page.setDepartmentName(MAX_LENGTH_NAME + "22222");
        assertEquals(MAX_LENGTH_NAME, page.departmentNameFieldValue(),
                "Field should reject any input beyond the maximum length");

        page.tapSubmit();

        assertTrue(page.hasMessage(SUCCESS_MESSAGE, Duration.ofSeconds(15)), "Expected success: " + SUCCESS_MESSAGE);
        screenshot("department-max-length-created");

        // An empty EditText's getText() reflects its hint, not "" — "Enter Department
        // Name" showing back is exactly what confirms the field cleared itself.
        assertEquals("Enter Department Name", page.departmentNameFieldValue(),
                "Field should clear itself after successful creation");
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Verify empty Department Name validation")
    void emptyDepartmentNameValidation() {
        CreateNewDepartmentPage page = department();

        page.tapSubmit();

        screenshot("department-empty-name");
        assertTrue(page.hasEmptyNameSnackbar(Duration.ofSeconds(5)), "Expected error: Enter Department Name");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Verify the created department can be deleted")
    void createdDepartmentCanBeDeleted() {
        CreateNewDepartmentPage page = department();

        assertTrue(page.isDepartmentListed(MAX_LENGTH_NAME, Duration.ofSeconds(10)),
                MAX_LENGTH_NAME + " should appear in the Departments list");

        assertTrue(page.tapDeleteIcon(MAX_LENGTH_NAME), "Delete icon should open the confirmation snackbar");
        assertTrue(page.hasMessage(DELETE_CONFIRM_MESSAGE, Duration.ofSeconds(5)),
                "Expected confirmation: " + DELETE_CONFIRM_MESSAGE);

        assertTrue(page.confirmDeleteYes(), "Yes action on the confirmation snackbar should be tappable");
        assertTrue(page.isDepartmentRemoved(MAX_LENGTH_NAME, Duration.ofSeconds(10)),
                MAX_LENGTH_NAME + " should no longer appear in the Departments list");
        screenshot("department-deleted");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Verify duplicate Department Name validation")
    void duplicateDepartmentNameValidation() {
        CreateNewDepartmentPage page = department();

        page.setDepartmentName("Stunts");
        page.tapSubmit();

        screenshot("department-duplicate-name");
        assertTrue(page.hasMessage(DUPLICATE_NAME_ERROR, Duration.ofSeconds(5)),
                "Expected error: " + DUPLICATE_NAME_ERROR);

        assertTrue(page.tapBackButton(), "Back button should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Back button should return to Admin Settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
