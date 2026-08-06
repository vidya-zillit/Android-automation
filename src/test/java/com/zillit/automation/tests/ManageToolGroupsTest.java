package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import io.appium.java_client.AppiumBy;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ManageToolGroupsPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import org.openqa.selenium.WebDriverException;
import org.junit.jupiter.api.AfterAll;
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
 * Settings → Admin Settings → "Manage Tool Groups".
 *
 * <p>Creates a custom group, checks the empty-name rule on both the Create and
 * the Rename dialog, and renames the group. Every expected string was read off
 * the running app rather than copied from the spec.
 *
 * <p>Custom groups persist, so the suite ends by DELETING the group it made —
 * otherwise a second run would try to create a name that already exists and
 * behave differently from the first. Cleanup lives in {@code @AfterAll} rather
 * than a test case, so a mid-suite failure cannot leave the project littered.
 */
@Order(TestOrder.MANAGE_TOOL_GROUPS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ManageToolGroupsTest extends BaseTest {

    private static final String EMPTY_NAME_ERROR = "Enter a group name";
    private static final String CREATED_MESSAGE = "Group created";
    private static final String RENAMED_MESSAGE = "Group renamed";

    private static final String GROUP_NAME = "Mango";
    private static final String RENAMED_NAME = "Bag";

    /** The tool moved between groups, and the groups it starts and ends in. */
    private static final String TOOL = "Confidential Info";
    private static final String ORIGINAL_GROUP = "Admin";
    private static final String FINAL_GROUP = "Production";
    /** Confirmed wording differs slightly per build, so this is matched on CONTAINS. */
    private static final String DELETE_BLOCKED_MESSAGE =
            "Move this group's tools to another group before deleting it";

    private static final Duration SNACKBAR_WAIT = Duration.ofSeconds(6);

    private boolean insideProject;
    /** Set once the group exists, so teardown knows there is something to remove. */
    private boolean groupCreated;
    /** Set once the tool has been pulled out of its original group. */
    private boolean toolMoved;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Manage Tool Groups screen, navigating (or recovering) to it as needed. */
    private ManageToolGroupsPage manageGroups() {
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
        ManageToolGroupsPage page = new ManageToolGroupsPage(driver);
        if (page.isLoaded()) {
            return page;
        }
        // Fast path: if we happen to be sitting on Admin Settings, the screen is a
        // single row tap away. Only fall through to the full unwind below when we
        // are somewhere else entirely.
        if (page.isOnAdminSettings(Duration.ofSeconds(2)) && page.openManageToolGroups()) {
            assumeTrue(page.isLoaded(), "Manage Tool Groups screen did not open");
            return page;
        }

        // On the SETTINGS TAB: open Admin Settings first, then the screen. Without
        // this the caller falls through to the unwind below, which tries to back out
        // to the bottom nav — but we are ALREADY on it, so the loop exits at once and
        // the recovery never really happens.
        if (page.isOnSettingsTab(Duration.ofSeconds(2))) {
            assumeTrue(page.openAdminSettings(), "Admin Settings row not present on the Settings tab");
            assumeTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Admin Settings did not open");
            assumeTrue(page.openManageToolGroups(), "Manage Tool Groups row not present");
            assumeTrue(page.isLoaded(), "Manage Tool Groups screen did not open");
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
        assumeTrue(page.openManageToolGroups(), "Manage Tool Groups not present");
        assumeTrue(page.isLoaded(), "Manage Tool Groups screen did not open");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Saving a new group with no name is rejected")
    void createWithEmptyNameRejected() {
        ManageToolGroupsPage page = manageGroups();
        screenshot("tool-groups-opened");

        assertTrue(page.tapNew(), "NEW button should be tappable");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(8)), "Create group dialog should open");
        assertTrue(page.tapSave(), "Save should be tappable");

        assertTrue(page.snackbarShown(EMPTY_NAME_ERROR, SNACKBAR_WAIT), "Expected: " + EMPTY_NAME_ERROR);
        screenshot("tool-groups-create-empty-error");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: A named group is created and appears in the list")
    void createGroupSucceeds() {
        ManageToolGroupsPage page = manageGroups();

        assertTrue(page.tapNew(), "NEW button should be tappable");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(8)), "Create group dialog should open");
        page.typeGroupName(GROUP_NAME);
        assertTrue(page.tapSave(), "Save should be tappable");

        assertTrue(page.snackbarShown(CREATED_MESSAGE, SNACKBAR_WAIT), "Expected: " + CREATED_MESSAGE);
        groupCreated = true;
        screenshot("tool-groups-created");

        assertTrue(page.hasGroup(GROUP_NAME), GROUP_NAME + " should be listed under GROUPS");
        screenshot("tool-groups-listed");
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Renaming to an empty name is rejected")
    void renameWithEmptyNameRejected() {
        ManageToolGroupsPage page = manageGroups();
        assumeTrue(groupCreated, "Group was never created");

        assertTrue(page.tapEditIcon(GROUP_NAME), "Edit icon for " + GROUP_NAME + " should be tappable");
        assertTrue(page.tapMenuOption("Rename"), "Rename option should be present");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(8)), "Rename group dialog should open");

        page.clearGroupName();
        assertTrue(page.tapSave(), "Save should be tappable");

        assertTrue(page.snackbarShown(EMPTY_NAME_ERROR, SNACKBAR_WAIT), "Expected: " + EMPTY_NAME_ERROR);
        screenshot("tool-groups-rename-empty-error");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: The group is renamed successfully")
    void renameGroupSucceeds() {
        ManageToolGroupsPage page = manageGroups();
        assumeTrue(groupCreated, "Group was never created");

        assertTrue(page.tapEditIcon(GROUP_NAME), "Edit icon for " + GROUP_NAME + " should be tappable");
        assertTrue(page.tapMenuOption("Rename"), "Rename option should be present");
        assertTrue(page.isDialogOpen(Duration.ofSeconds(8)), "Rename group dialog should open");

        page.clearGroupName();
        page.typeGroupName(RENAMED_NAME);
        assertTrue(page.tapSave(), "Save should be tappable");

        assertTrue(page.snackbarShown(RENAMED_MESSAGE, SNACKBAR_WAIT), "Expected: " + RENAMED_MESSAGE);
        screenshot("tool-groups-renamed");

        assertTrue(page.hasGroup(RENAMED_NAME), RENAMED_NAME + " should be listed after the rename");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Removing a tool from its group moves it to NOT IN GROUP TOOLS")
    void removeToolFromGroup() {
        ManageToolGroupsPage page = manageGroups();

        assertEquals(ORIGINAL_GROUP, page.toolGroupLabel(TOOL),
                ORIGINAL_GROUP + " should be shown below " + TOOL);
        screenshot("tool-groups-tool-in-admin");

        assertTrue(page.tapMoveButton(TOOL), "Move button for " + TOOL + " should be tappable");
        assertTrue(page.tapMenuOption("Remove from group"), "Remove from group option should be present");
        toolMoved = true;
        sleep(2000);

        assertTrue(page.isUnderNotInGroupSection(TOOL),
                TOOL + " should be listed under NOT IN GROUP TOOLS");
        assertEquals("", page.toolGroupLabel(TOOL),
                "The group name below " + TOOL + " should be gone once it is in no group");
        screenshot("tool-groups-tool-ungrouped");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: The ungrouped tool shows under UNGROUPED on the Tools tab")
    void ungroupedToolShowsOnToolsTab() {
        assumeTrue(toolMoved, "Tool was never removed from its group");
        ManageToolGroupsPage page = manageGroups();

        assertTrue(page.tapBackButton(), "Back should leave Manage Tool Groups");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Should be on Admin Settings");
        assertTrue(page.tapBackButton(), "Back should leave Admin Settings");
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(15)),
                "Should be back on the project's Bottom Navigation");

        assertTrue(toolUnderSectionOnToolsTab("UNGROUPED", TOOL),
                TOOL + " should appear under UNGROUPED on the Tools tab");
        screenshot("tool-groups-tools-tab-ungrouped");
    }

    @Test
    @Order(7)
    @DisplayName("Test Case 7: Moving the tool into the custom group updates its label")
    void moveToolIntoCustomGroup() {
        ManageToolGroupsPage page = manageGroups();
        assumeTrue(groupCreated, "Group was never created");

        assertTrue(page.tapMoveButton(TOOL), "Move button for " + TOOL + " should be tappable");
        assertTrue(page.tapMenuOption(RENAMED_NAME), RENAMED_NAME + " should be offered in the Move menu");
        sleep(2000);

        assertFalse(page.isUnderNotInGroupSection(TOOL),
                TOOL + " should no longer be under NOT IN GROUP TOOLS");
        assertEquals(RENAMED_NAME, page.toolGroupLabel(TOOL),
                RENAMED_NAME + " should be shown below " + TOOL);
        screenshot("tool-groups-tool-in-bag");
    }

    @Test
    @Order(8)
    @DisplayName("Test Case 8: The tool shows under its new group on the Tools tab")
    void movedToolShowsUnderNewGroupOnToolsTab() {
        ManageToolGroupsPage page = manageGroups();

        assertTrue(page.tapBackButton(), "Back should leave Manage Tool Groups");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Should be on Admin Settings");
        assertTrue(page.tapBackButton(), "Back should leave Admin Settings");
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(15)),
                "Should be back on the project's Bottom Navigation");

        assertTrue(toolUnderSectionOnToolsTab(RENAMED_NAME.toUpperCase(), TOOL),
                TOOL + " should appear under " + RENAMED_NAME.toUpperCase() + " on the Tools tab");
        screenshot("tool-groups-tools-tab-bag");
    }

    @Test
    @Order(9)
    @DisplayName("Test Case 9: A group holding tools cannot be deleted")
    void groupWithToolsCannotBeDeleted() {
        ManageToolGroupsPage page = manageGroups();
        assumeTrue(groupCreated, "Group was never created");

        assertTrue(page.tapEditIcon(RENAMED_NAME), "Edit icon for " + RENAMED_NAME + " should be tappable");
        assertTrue(page.tapMenuOption("Delete"), "Delete option should be present");

        assertTrue(page.popupShown(DELETE_BLOCKED_MESSAGE, Duration.ofSeconds(8)),
                "Expected popup: " + DELETE_BLOCKED_MESSAGE);
        screenshot("tool-groups-delete-blocked");

        assertTrue(page.tapDialogOk(), "OK should dismiss the warning popup");
    }

    @Test
    @Order(10)
    @DisplayName("Test Case 10: Once emptied, the group can be deleted")
    void emptiedGroupCanBeDeleted() {
        ManageToolGroupsPage page = manageGroups();
        assumeTrue(groupCreated, "Group was never created");

        assertTrue(page.tapMoveButton(TOOL), "Move button for " + TOOL + " should be tappable");
        assertTrue(page.tapMenuOption(FINAL_GROUP), FINAL_GROUP + " should be offered in the Move menu");
        sleep(2000);
        assertEquals(FINAL_GROUP, page.toolGroupLabel(TOOL),
                FINAL_GROUP + " should be shown below " + TOOL);

        assertTrue(page.tapEditIcon(RENAMED_NAME), "Edit icon for " + RENAMED_NAME + " should be tappable");
        assertTrue(page.tapMenuOption("Delete"), "Delete option should be present");
        assertTrue(page.tapSave(), "Confirm (button1) should delete the group");
        sleep(2000);

        assertFalse(page.hasGroup(RENAMED_NAME), RENAMED_NAME + " should be gone after deletion");
        groupCreated = false;          // teardown has nothing left to remove
        screenshot("tool-groups-deleted");
    }

    /**
     * True when {@code tool} is listed below the {@code section} heading on the
     * Tools tab. Both have to be on screen together, so the list is scrolled until
     * they are — the section headings are {@code groupTitleTv}, tool rows are
     * {@code nameTv}, and only their vertical order distinguishes membership.
     */
    private boolean toolUnderSectionOnToolsTab(String section, String tool) {
        ToolsPage tools = new ToolsPage(driver);
        if (!tools.openToolsTab()) {
            return false;
        }
        tools.isLoaded(Duration.ofSeconds(15));
        sleep(2000);
        for (int i = 0; i < 18; i++) {
            try {
                var heading = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/groupTitleTv\").text(\"" + section + "\")"));
                var row = driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/nameTv\").text(\"" + tool + "\")"));
                return row.getRect().getY() > heading.getRect().getY();
            } catch (WebDriverException notBothYet) {
                swipeToolsList();
            }
        }
        return false;
    }

    /**
     * Best-effort restore of the moved tool when nothing else needs cleaning up.
     * Returns true if it managed to reach the screen.
     */
    private boolean restoreToolOnly() {
        try {
            ManageToolGroupsPage page = new ManageToolGroupsPage(driver);
            if (!page.isLoaded()) {
                for (int i = 0; i < 8 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
                    try {
                        driver.navigate().back();
                    } catch (Exception ignored) {
                    }
                    sleep(500);
                }
                if (!page.openSettingsTab() || !page.openAdminSettings()
                        || !page.openManageToolGroups() || !page.isLoaded()) {
                    return false;
                }
            }
            if (!ORIGINAL_GROUP.equals(page.toolGroupLabel(TOOL))
                    && page.tapMoveButton(TOOL) && page.tapMenuOption(ORIGINAL_GROUP)) {
                sleep(2000);
                System.out.println(">>> CLEANUP: put " + TOOL + " back into " + ORIGINAL_GROUP);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void swipeToolsList() {
        // Guarded: a W3C action can be refused outright if the screen is mid-
        // transition, and an unhandled InvalidElementStateException here aborts
        // the whole case rather than just costing one scroll attempt.
        try {
            doSwipeToolsList();
        } catch (WebDriverException ignored) {
            sleep(500);
        }
    }

    private void doSwipeToolsList() {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        int x = size.getWidth() / 2;
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.80)));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(700),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.50)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(400);
    }

    /**
     * Removes the group this run created, so the next run starts from the same
     * state this one did. Best-effort by design: never throws, so it cannot turn
     * a clean run red or bury the real failure.
     */
    @AfterAll
    void deleteTestGroup() {
        // Deliberately NOT gated on groupCreated. The tool has to go back to its
        // original group even when the suite fell over before creating anything —
        // an early return here is exactly why a failed run left the tool stranded
        // in another group and made the next run fail on a stale precondition.
        if (!groupCreated && !toolMoved) {
            // Still worth a look: an earlier aborted run may have left it moved.
            restoreToolOnly();
            finishOnAdminSettings();   // this path skips the finally below
            return;
        }
        try {
            ManageToolGroupsPage page = new ManageToolGroupsPage(driver);
            if (!page.isLoaded()) {
                for (int i = 0; i < 8 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
                    try {
                        driver.navigate().back();
                    } catch (Exception ignored) {
                    }
                    sleep(500);
                }
                if (!page.openSettingsTab() || !page.openAdminSettings()
                        || !page.openManageToolGroups() || !page.isLoaded()) {
                    System.out.println("!!! CLEANUP SKIPPED — could not reach Manage Tool Groups. "
                            + "Delete the test group by hand before the next run.");
                    return;
                }
            }
            // Put the tool back where it started, so the next run sees the same
            // world this one did. A group still holding tools CANNOT be deleted —
            // the app blocks it — so this has to happen first, not after.
            if (!ORIGINAL_GROUP.equals(page.toolGroupLabel(TOOL))
                    && page.tapMoveButton(TOOL) && page.tapMenuOption(ORIGINAL_GROUP)) {
                sleep(2000);
                System.out.println(">>> CLEANUP: moved " + TOOL + " back to " + ORIGINAL_GROUP);
            }
            // Whichever name it ended up with, depending on how far the suite got.
            for (String name : new String[]{RENAMED_NAME, GROUP_NAME}) {
                if (page.hasGroup(name) && page.tapEditIcon(name) && page.tapMenuOption("Delete")) {
                    // Confirmation is an AlertDialog: button1 = Delete, button2 = Cancel.
                    page.tapSave();
                    sleep(1500);
                    System.out.println(">>> CLEANUP: deleted group \"" + name + "\"");
                    return;
                }
            }
            System.out.println(">>> CLEANUP: no test group left to delete");
        } catch (Exception e) {
            System.out.println("!!! CLEANUP FAILED (" + e.getClass().getSimpleName() + ": "
                    + e.getMessage() + ") — remove the test group manually.");
        } finally {
            // Finish on the ADMIN SETTINGS list. This teardown walks back INTO
            // Manage Tool Groups to restore the tool, so without a closing back
            // press the suite would end sitting inside that screen rather than on
            // the list the flow started from.
            finishOnAdminSettings();
        }
    }

    /** Presses the toolbar back arrow until the Admin Settings list is showing. */
    private void finishOnAdminSettings() {
        try {
            ManageToolGroupsPage page = new ManageToolGroupsPage(driver);
            for (int i = 0; i < 3; i++) {
                if (page.isOnAdminSettings(Duration.ofSeconds(2))) {
                    System.out.println(">>> finished on ADMIN SETTINGS");
                    return;
                }
                if (!page.tapBackButton()) {
                    break;
                }
                sleep(800);
            }
            if (page.openSettingsTab() && page.openAdminSettings()
                    && page.isOnAdminSettings(Duration.ofSeconds(5))) {
                System.out.println(">>> finished on ADMIN SETTINGS");
            }
        } catch (Exception ignored) {
            // Housekeeping only.
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
