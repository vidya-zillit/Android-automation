package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.EditProjectNamePage;
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
 * Settings → Admin Settings → "Edit Project Name".
 *
 * <p>A ladder on ONE screen instance ({@code relaunchBetweenTests()=false}): each
 * case continues on the form the previous one left, matching how the app really
 * behaves — validation only fires on Submit, and the field keeps whatever was
 * last typed.
 *
 * <p>Every expected string here was read off the running app rather than copied
 * from the spec. One differs: the success message ends with a full stop
 * ("…updated successfully."), so the assertions match on CONTAINS.
 *
 * <p><b>This suite renames the project.</b> It ends with the project called
 * {@value #NEW_NAME}, which is the specified behaviour, not an accident.
 */
@Order(TestOrder.EDIT_PROJECT_NAME)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EditProjectNameTest extends BaseTest {

    private static final String EMPTY_ERROR = "Project name cannot be empty";
    private static final String LENGTH_ERROR =
            "Project name should be of at least 3 characters or at most 25 characters.";
    private static final String SUCCESS_MESSAGE = "Project details have been updated successfully";
    private static final String NO_CHANGE_MESSAGE = "Please enter a new project name";

    private static final String NEW_NAME = "ZL TESTING E";
    /**
     * Used when the project is ALREADY called {@link #NEW_NAME}. Submitting the
     * name a project already has is rejected with "Please enter a new project
     * name", so a suite pinned to one literal passes once and fails every run
     * after that. Alternating between the two keeps it repeatable — which it has
     * to be now that it runs as part of {@code --admin-settings}.
     */
    private static final String ALT_NAME = "ZL TESTING F";
    /** 45 characters — comfortably past the field's 40-character ceiling. */
    private static final String OVERLONG_NAME = "ZLTESTINGABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final int MAX_FIELD_LENGTH = 40;

    private static final Duration SNACKBAR_WAIT = Duration.ofSeconds(6);

    private boolean insideProject;
    /** Whichever of NEW_NAME / ALT_NAME Test Case 4 actually applied this run. */
    private String appliedName;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Edit Project Name screen, navigating (or recovering) to it as needed. */
    private EditProjectNamePage editProjectName() {
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
        EditProjectNamePage page = new EditProjectNamePage(driver);
        if (page.isLoaded()) {
            return page;
        }

        // On the SETTINGS TAB: open Admin Settings first, then the screen. Without
        // this the caller falls through to the unwind below, which tries to back out
        // to the bottom nav — but we are ALREADY on it, so the loop exits at once and
        // the recovery never really happens.
        if (page.isOnSettingsTab(Duration.ofSeconds(2))) {
            assumeTrue(page.openAdminSettings(), "Admin Settings row not present on the Settings tab");
            assumeTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Admin Settings did not open");
            assumeTrue(page.openEditProjectName(), "Edit Project Name row not present");
            assumeTrue(page.isLoaded(), "Edit Project Name screen did not open");
            return page;
        }

        // Recover: back out until Bottom Nav is visible again, then re-navigate.
        for (int i = 0; i < 8 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(), "Admin Settings not present");
        assumeTrue(page.openEditProjectName(), "Edit Project Name not present");
        assumeTrue(page.isLoaded(), "Edit Project Name screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Empty project name is rejected")
    void emptyProjectNameRejected() {
        EditProjectNamePage page = editProjectName();
        screenshot("edit-project-name-opened");

        page.clearProjectName();
        assertTrue(page.tapSubmit(), "Submit should be tappable");

        assertTrue(page.snackbarShown(EMPTY_ERROR, SNACKBAR_WAIT), "Expected: " + EMPTY_ERROR);
        screenshot("edit-project-name-empty-error");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Single character is rejected by the length rule")
    void tooShortProjectNameRejected() {
        EditProjectNamePage page = editProjectName();

        page.setProjectName("Z");
        assertTrue(page.tapSubmit(), "Submit should be tappable");

        assertTrue(page.snackbarShown(LENGTH_ERROR, SNACKBAR_WAIT), "Expected: " + LENGTH_ERROR);
        screenshot("edit-project-name-too-short-error");
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: The field itself refuses more than 40 characters")
    void fieldCapsAtFortyCharacters() {
        EditProjectNamePage page = editProjectName();

        page.setProjectName(OVERLONG_NAME);
        String actual = page.projectNameFieldText();
        screenshot("edit-project-name-max-length");

        // No error to assert here — the ceiling is an input filter, so the extra
        // characters are simply never accepted. Measure what the field kept.
        assertEquals(MAX_FIELD_LENGTH, actual.length(),
                "Field should stop accepting input at " + MAX_FIELD_LENGTH
                        + " characters, but held " + actual.length() + ": \"" + actual + "\"");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: A valid name is saved and shown at the top of the screen")
    void validProjectNameIsSaved() {
        EditProjectNamePage page = editProjectName();

        // Pick a name that is genuinely DIFFERENT from the current one, or the save
        // is rejected as "no change" and this case fails on every run after the first.
        String current = page.toolbarProjectName();
        appliedName = NEW_NAME.equalsIgnoreCase(current) ? ALT_NAME : NEW_NAME;
        System.out.println(">>> renaming \"" + current + "\" -> \"" + appliedName + "\"");

        page.setProjectName(appliedName);
        assertTrue(page.tapSubmit(), "Submit should be tappable");

        assertTrue(page.snackbarShown(SUCCESS_MESSAGE, SNACKBAR_WAIT), "Expected: " + SUCCESS_MESSAGE);
        screenshot("edit-project-name-success");

        // The toolbar is repainted from the save response, so give it a moment
        // before reading — this is the app updating, not the test settling.
        sleep(2000);
        assertEquals(appliedName, page.toolbarProjectName(),
                "The new name should be shown at the top of the screen");
        screenshot("edit-project-name-toolbar-updated");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Submitting again with no change is rejected")
    void resubmitWithoutChangeRejected() {
        EditProjectNamePage page = editProjectName();

        assertTrue(page.tapSubmit(), "Submit should be tappable");

        assertTrue(page.snackbarShown(NO_CHANGE_MESSAGE, SNACKBAR_WAIT), "Expected: " + NO_CHANGE_MESSAGE);
        screenshot("edit-project-name-no-change-error");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Back button from the top returns to Admin Settings")
    void backButtonReturnsToAdminSettings() {
        EditProjectNamePage page = editProjectName();

        assertTrue(page.tapBackButton(), "Back button should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)),
                "Back button should return to Admin Settings");
        screenshot("edit-project-name-back-on-admin-settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
