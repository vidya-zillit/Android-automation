package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateAdditionalShootingUnitPage;
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
 * Settings → Admin Settings → "Create Additional Shooting Unit".
 *
 * A ladder walked on ONE screen instance (relaunchBetweenTests()=false): Test
 * Case 2 continues in the SAME add-unit form Test Case 1 left open (a failed
 * Submit keeps the form open with its native field error shown), and Test
 * Case 3 continues from there once the field is cleared and replaced.
 */
@Order(TestOrder.CREATE_ADDITIONAL_SHOOTING_UNIT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateAdditionalShootingUnitTest extends BaseTest {

    private static final String LENGTH_ERROR =
            "Please enter unit name of length at least 3 characters or at most 25 characters.";

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Create Additional Shooting Unit list screen, navigating (or recovering) to it as needed. */
    private CreateAdditionalShootingUnitPage shootingUnit() {
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
        CreateAdditionalShootingUnitPage page = new CreateAdditionalShootingUnitPage(driver);
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
        assumeTrue(page.openCreateAdditionalShootingUnit(), "Create Additional Shooting Unit not present");
        assumeTrue(page.isLoaded(), "Create Additional Shooting Unit screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Verify Unit Name minimum length validation")
    void minimumUnitNameLength() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        assertTrue(page.tapAddFab(), "+ icon should open the add-unit form");
        assertTrue(page.isAddFormOpen(), "Add-unit form should be open");

        page.setUnitName("Ma");
        page.tapSubmit();

        screenshot("shooting-unit-min-length");
        assertTrue(page.hasValidationError(LENGTH_ERROR), "Expected error: " + LENGTH_ERROR);
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Verify Unit Name maximum length validation")
    void maximumUnitNameLength() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        page.setUnitName("diisopropylfluorophosphate");
        page.tapSubmit();

        screenshot("shooting-unit-max-length");
        assertTrue(page.hasValidationError(LENGTH_ERROR), "Expected error: " + LENGTH_ERROR);
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Verify successful creation of an additional shooting unit")
    void validShootingUnitCreated() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        page.setUnitName("Malaysia");
        page.tapSubmit();

        assertTrue(page.isUnitListed("Malaysia", Duration.ofSeconds(15)),
                "Malaysia should appear in the Additional Shooting Unit list");
        screenshot("shooting-unit-created");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Search for the created unit")
    void searchForCreatedUnit() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        page.searchUnit("Malaysia");

        screenshot("shooting-unit-search-malaysia");
        assertTrue(page.isUnitListed("Malaysia", Duration.ofSeconds(10)),
                "Malaysia should appear in the search results");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Edit icon opens the form prefilled with the unit name")
    void editIconOpensPrefilledForm() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        assertTrue(page.tapEditIcon("Malaysia"), "Edit icon should open the update form");
        assertTrue(page.isAddFormOpen(), "Update form should be open");

        screenshot("shooting-unit-edit-prefilled");
        assertEquals("Malaysia", page.unitNameFieldValue(),
                "Unit Name field should be prefilled with the existing name");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Verify Unit Name minimum length validation on Update")
    void minimumUnitNameLengthOnUpdate() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        page.setUnitName("Ma");
        page.tapUpdate();

        screenshot("shooting-unit-update-min-length");
        assertTrue(page.hasValidationError(LENGTH_ERROR), "Expected error: " + LENGTH_ERROR);
    }

    @Test
    @Order(7)
    @DisplayName("Test Case 7: Verify Unit Name maximum length validation on Update")
    void maximumUnitNameLengthOnUpdate() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        page.setUnitName("diisopropylfluorophosphate");
        page.tapUpdate();

        screenshot("shooting-unit-update-max-length");
        assertTrue(page.hasValidationError(LENGTH_ERROR), "Expected error: " + LENGTH_ERROR);
    }

    @Test
    @Order(8)
    @DisplayName("Test Case 8: Verify successful update of the shooting unit")
    void validShootingUnitUpdated() {
        CreateAdditionalShootingUnitPage page = shootingUnit();

        page.setUnitName("Kashmir");
        page.tapUpdate();

        // The search bar still holds "Malaysia" from Test Case 4 — now stale
        // since the unit was just renamed, so clear it before checking the list.
        page.searchUnit("");
        assertTrue(page.isUnitListed("Kashmir", Duration.ofSeconds(15)),
                "Kashmir should appear in the Additional Shooting Unit list");
        screenshot("shooting-unit-updated");

        assertTrue(page.tapDeleteIcon("Kashmir"), "Delete icon should open the confirmation snackbar");
        assertTrue(page.confirmDeleteYes(), "Yes action on the confirmation snackbar should be tappable");
        assertTrue(page.isUnitRemoved("Kashmir", Duration.ofSeconds(10)),
                "Kashmir should no longer appear in the list after confirming delete");
        screenshot("shooting-unit-deleted");

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
