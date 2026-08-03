package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateNewDesignationPage;
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
 * Settings → Admin Settings → "Create New Designation".
 *
 * A ladder walked on ONE screen instance (relaunchBetweenTests()=false): each
 * test case continues editing the same open form the previous one left.
 */
@Order(54)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateNewDesignationTest extends BaseTest {

    private static final String MIN_LENGTH_ERROR = "Name must contain at least 3 characters";
    private static final String SUCCESS_MESSAGE = "Designation has been created successfully.";
    private static final String DELETE_CONFIRM_MESSAGE = "Do You really want to delete this Designation?";
    private static final String DUPLICATE_NAME_ERROR = "Designation already exists.";
    // Discovered by scrolling the department Spinner to its true end and
    // counting back — "Studio" sits 3rd from the last item (0-indexed: 2 ups).
    private static final int STUDIO_UPS_FROM_END = 2;

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Create New Designation screen, navigating (or recovering) to it as needed. */
    private CreateNewDesignationPage designation() {
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
        CreateNewDesignationPage page = new CreateNewDesignationPage(driver);
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
        assumeTrue(page.openCreateNewDesignation(), "Create New Designation not present");
        assumeTrue(page.isLoaded(), "Create New Designation screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Selecting a different department updates the field")
    void selectingDepartmentUpdatesField() {
        CreateNewDesignationPage page = designation();

        page.selectDepartmentFromEnd(STUDIO_UPS_FROM_END);

        screenshot("designation-department-studio");
        assertEquals("Studio", page.selectedDepartment(), "Department field should now show Studio");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Verify Designation minimum length validation")
    void minimumDesignationLength() {
        CreateNewDesignationPage page = designation();

        page.setDesignationName("M");
        page.tapSubmit();

        screenshot("designation-min-length");
        assertTrue(page.hasMessage(MIN_LENGTH_ERROR, Duration.ofSeconds(5)), "Expected error: " + MIN_LENGTH_ERROR);
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Verify successful creation of a designation")
    void validDesignationCreated() {
        CreateNewDesignationPage page = designation();

        page.setDesignationName("AK TALKS");
        page.tapSubmit();

        assertTrue(page.hasMessage(SUCCESS_MESSAGE, Duration.ofSeconds(15)), "Expected success: " + SUCCESS_MESSAGE);
        assertTrue(page.isDesignationListed("AK TALKS", Duration.ofSeconds(10)),
                "AK TALKS should appear in the Designations list");
        screenshot("designation-created");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Verify empty Designation validation")
    void emptyDesignationNameValidation() {
        CreateNewDesignationPage page = designation();

        page.tapSubmit();

        screenshot("designation-empty-name");
        assertTrue(page.hasEmptyNameSnackbar(Duration.ofSeconds(5)), "Expected error: Enter Designation");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Verify the created designation can be deleted")
    void createdDesignationCanBeDeleted() {
        CreateNewDesignationPage page = designation();

        assertTrue(page.tapDeleteIcon("AK TALKS"), "Delete icon should open the confirmation snackbar");
        assertTrue(page.hasMessage(DELETE_CONFIRM_MESSAGE, Duration.ofSeconds(5)),
                "Expected confirmation: " + DELETE_CONFIRM_MESSAGE);

        assertTrue(page.confirmDeleteYes(), "Yes action on the confirmation snackbar should be tappable");
        assertTrue(page.isDesignationRemoved("AK TALKS", Duration.ofSeconds(10)),
                "AK TALKS should no longer appear in the Designations list");
        screenshot("designation-deleted");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Verify duplicate Designation validation")
    void duplicateDesignationNameValidation() {
        CreateNewDesignationPage page = designation();

        page.setDesignationName("Executive");
        page.tapSubmit();

        screenshot("designation-duplicate-name");
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
