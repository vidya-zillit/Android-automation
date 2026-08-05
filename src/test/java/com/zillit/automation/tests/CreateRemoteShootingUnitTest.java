package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateRemoteShootingUnitPage;
import com.zillit.automation.pages.DeleteProjectPage;
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
 * Settings → Admin Settings → "Create Remote Shooting Units", followed by the
 * created unit's own project workspace → Admin Settings → "Delete Project".
 *
 * A Remote Shooting Unit is its own mini-project: it shows up in the Project
 * List as "{@code <unit name>(<parent project>) - Admin}" and can be opened,
 * searched, and deleted independently of the project it was created in. This
 * suite creates ONE disposable unit and schedules ITS deletion — never the
 * parent project — so the destructive final step only ever affects data this
 * same run created.
 *
 * relaunchBetweenTests()=false: the ladder deliberately leaves the create-unit
 * screen, switches project via the in-app switcher, and re-enters Admin
 * Settings on the newly created unit's own workspace — one continuous session.
 */
@Order(TestOrder.CREATE_REMOTE_SHOOTING_UNIT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateRemoteShootingUnitTest extends BaseTest {

    private static final String LENGTH_ERROR =
            "Please enter unit name of length at least 3 characters or at most 25 characters.";
    private static final String DELETE_SUCCESS_MESSAGE = "Project has been marked for deletion successfully.";

    private boolean insideProject;
    // Picked fresh per run (see pickUnitName()) — a fixed literal would collide with a
    // same-named unit left over from a prior run that hasn't finished its scheduled
    // deletion yet, and Test Case 4's search/tapProject would then risk opening the
    // WRONG (stale, already-scheduled-for-deletion) one instead of this run's.
    private String unitName;
    // The project the unit is created FROM. Captured on the way in, because the
    // final test case has to come back to it — after scheduling the unit's
    // deletion the session is sitting inside a workspace that is on its way out,
    // and every later suite would otherwise run against that instead.
    private String parentProjectName;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Create Remote Shooting Units list screen, navigating (or recovering) to it as needed. */
    private CreateRemoteShootingUnitPage remoteUnit() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            if (parentProjectName == null || parentProjectName.isBlank()) {
                parentProjectName = new BottomNavPage(driver).currentProjectName();
                System.out.println(">>> parent project: " + parentProjectName);
            }
            insideProject = true;
        }
        CreateRemoteShootingUnitPage page = new CreateRemoteShootingUnitPage(driver);
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
        assumeTrue(page.openCreateRemoteShootingUnits(), "Create Remote Shooting Units not present");
        assumeTrue(page.isLoaded(), "Create Remote Shooting Units screen did not open");
        return page;
    }

    /**
     * Starts with "ZL", stays under 20 characters, and is verified absent from the
     * CURRENT units list before use — checked here, on the list screen, because once
     * Test Case 1 opens the add form the list underneath is a different screen and
     * no longer queryable the same way.
     */
    private String pickUnitName(CreateRemoteShootingUnitPage page) {
        String candidate;
        do {
            candidate = "ZL" + (System.currentTimeMillis() % 1_000_000);
        } while (page.isUnitListed(candidate, Duration.ofMillis(500)));
        return candidate;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Verify Unit Name minimum length validation")
    void minimumUnitNameLength() {
        CreateRemoteShootingUnitPage page = remoteUnit();
        unitName = pickUnitName(page);
        System.out.println("Unit name picked for this run: " + unitName);

        assertTrue(page.tapAddFab(), "+ icon should open the add-unit form");
        assertTrue(page.isAddFormOpen(), "Add-unit form should be open");

        page.setUnitName("H");
        page.tapSubmit();

        String error = page.unitNameFieldError();
        System.out.println("Validation message (min length): " + error);
        screenshot("remote-unit-min-length");
        assertEquals(LENGTH_ERROR, error, "Expected error: " + LENGTH_ERROR);
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Verify Unit Name maximum length validation")
    void maximumUnitNameLength() {
        CreateRemoteShootingUnitPage page = remoteUnit();

        page.setUnitName("Hippopotomonstrosesquippedaliophobia");
        page.tapSubmit();

        String error = page.unitNameFieldError();
        System.out.println("Validation message (max length): " + error);
        screenshot("remote-unit-max-length");
        assertEquals(LENGTH_ERROR, error, "Expected error: " + LENGTH_ERROR);
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Verify successful creation of a remote shooting unit")
    void validRemoteUnitCreated() {
        CreateRemoteShootingUnitPage page = remoteUnit();

        page.setUnitName(unitName);
        page.tapSubmit();

        assertTrue(page.isUnitListed(unitName, Duration.ofSeconds(15)),
                unitName + " should appear in the Create Remote Shooting Units list");
        screenshot("remote-unit-created");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Back out to Project Listing and reopen the new unit as its own project")
    void backOutAndReopenAsProject() {
        CreateRemoteShootingUnitPage page = remoteUnit();

        assertTrue(page.tapBackButton(), "Back button should be tappable from the units list");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Back button should return to Admin Settings");

        assertTrue(page.tapBackButton(), "Back button should be tappable from Admin Settings");
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(10)),
                "Back button should return to the project's Bottom Navigation");

        ProjectListPage projectList = new BottomNavPage(driver).tapChangeProject();
        assertTrue(projectList.isLoaded(), "Project selector should open the Project Listing screen");
        screenshot("remote-unit-project-listing");

        projectList.search(unitName);
        assertTrue(projectList.visibleProjectNames().stream()
                        .anyMatch(name -> name.toLowerCase().contains(unitName.toLowerCase())),
                unitName + " should appear in the project search results");
        screenshot("remote-unit-project-search");

        assertTrue(projectList.tapProject(unitName), "Should be able to open " + unitName + " as a project");
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(20)),
                unitName + " project should finish loading");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Delete Project dialog shows all options and Cancel dismisses it")
    void deleteProjectDialogCancel() {
        DeleteProjectPage page = new DeleteProjectPage(driver);
        assertTrue(page.openSettingsTab(), "Settings tab not present");
        assertTrue(page.openAdminSettings(), "Admin Settings not present");

        assertTrue(page.tapDeleteProject(), "Delete Project row should be tappable");
        assertTrue(page.isConfirmDialogOpen(Duration.ofSeconds(10)), "Delete confirmation dialog should be open");
        assertTrue(page.hasOption("12 Hours", Duration.ofSeconds(3)), "12 Hours option should be visible");
        assertTrue(page.hasOption("24 Hours", Duration.ofSeconds(3)), "24 Hours option should be visible");
        assertTrue(page.hasOption("48 Hours", Duration.ofSeconds(3)), "48 Hours option should be visible");
        assertTrue(page.hasOption("Cancel", Duration.ofSeconds(3)), "Cancel option should be visible");
        screenshot("delete-project-dialog");

        assertTrue(page.tapOption("Cancel"), "Cancel should be tappable");
        assertTrue(!page.isConfirmDialogOpen(Duration.ofMillis(800)), "Dialog should be dismissed after Cancel");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Confirming 12 Hours schedules the deletion")
    void deleteProjectConfirm12Hours() {
        DeleteProjectPage page = new DeleteProjectPage(driver);

        assertTrue(page.tapDeleteProject(), "Delete Project row should be tappable");
        assertTrue(page.isConfirmDialogOpen(Duration.ofSeconds(10)), "Delete confirmation dialog should be open");

        assertTrue(page.tapOption("12 Hours"), "12 Hours option should be tappable");
        assertTrue(page.hasMessage(DELETE_SUCCESS_MESSAGE, Duration.ofSeconds(10)),
                "Expected success: " + DELETE_SUCCESS_MESSAGE);
        screenshot("delete-project-scheduled");

        assertTrue(page.tapBackButton(), "Back button should be tappable");

        // Get OUT of the unit that was just scheduled for deletion. Ending here
        // would leave the session inside a doomed workspace, so every suite
        // ordered after this one (CreateUpdateHomeUnitTest and on) would run
        // against the wrong project. Toolbar arrow -> Project Listing -> parent.
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(20)),
                "Back should land on the unit's Bottom Navigation");
        ProjectListPage projectList = new BottomNavPage(driver).tapChangeProject();
        assertTrue(projectList.isLoaded(), "Toolbar arrow should open the Project Listing screen");
        screenshot("delete-project-project-listing");

        assumeTrue(parentProjectName != null && !parentProjectName.isBlank(),
                "Parent project name was never captured, so there is nothing to return to");
        projectList.search(parentProjectName);
        assertTrue(projectList.tapProject(parentProjectName),
                "Should be able to reopen the parent project " + parentProjectName);
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(30)),
                parentProjectName + " should finish loading");
        screenshot("delete-project-back-in-parent");

        // Walk back into Admin Settings in the PARENT project before finishing.
        // Two reasons: it proves the area is still reachable after switching
        // workspaces, and it hands the next admin-settings suite (order 82+) a
        // working screen instead of letting it be the one to discover a stranded
        // session — which would surface as an unrelated failure over there.
        assertTrue(page.openSettingsTab(), "Settings tab should be present in " + parentProjectName);
        assertTrue(page.openAdminSettings(), "Admin Settings should open in " + parentProjectName);
        assertTrue(new CreateRemoteShootingUnitPage(driver).isOnAdminSettings(Duration.ofSeconds(10)),
                "Should finish on Admin Settings in " + parentProjectName + ", ready for the next module");
        screenshot("delete-project-parent-admin-settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
