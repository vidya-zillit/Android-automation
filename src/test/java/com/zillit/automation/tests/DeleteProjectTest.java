package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DeleteprojectEntertainment;
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
 * Settings → Admin Settings → "Delete Project".
 *
 * <p>Exercises the whole scheduling flow: the delay sheet and its Cancel, arming
 * a 48-hour deletion, stopping it (declining the confirmation first), then arming
 * a 12-hour one and checking the countdown the Settings tab puts up.
 *
 * <p><b>This suite arms a real deletion and leaves it armed.</b> The specified
 * flow ends after confirming the countdown, so the project stays scheduled for
 * destruction roughly twelve hours later. It is recoverable — "Stop Project
 * Deletion" cancels it — but the timer is live once the run finishes, and it is
 * re-armed by every run. That is why this sits at the very end of the destructive
 * range: nothing may run after it.
 *
 * <p>Behaviour confirmed on the device while building this:
 * <ul>
 *   <li>the Admin Settings row IS the state indicator, relabelling itself between
 *       "Delete Project" and "Stop Project Deletion";</li>
 *   <li>Cancel in the delay sheet arms nothing;</li>
 *   <li>stopping asks to confirm, and "No" leaves the timer running;</li>
 *   <li>the countdown appears on the Settings tab as {@code projectTimeLeft}, and
 *       a 12-hour choice starts just under 12:00:00.</li>
 * </ul>
 */
@Order(TestOrder.DELETE_PROJECT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeleteProjectTest extends BaseTest {

    private static final Duration MESSAGE_WAIT = Duration.ofSeconds(20);
    private static final Duration ROW_WAIT = Duration.ofSeconds(45);
    /** Twelve hours, with a little slack for the round trip. */
    private static final long TWELVE_HOURS = 12 * 3600;
    private static final long LOWER_BOUND = TWELVE_HOURS - 600;   // 11:50:00

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns Admin Settings, navigating (or recovering) to it as needed. */
    private DeleteprojectEntertainment adminSettings() {
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
        DeleteprojectEntertainment page = new DeleteprojectEntertainment(driver);
        assumeTrue(page.ensureOnAdminSettings(Duration.ofSeconds(60)),
                "Could not reach Settings -> Admin Settings");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: Delete Project asks to confirm and offers 12, 24, 48 hours and Cancel")
    void showsDelayOptions() {
        DeleteprojectEntertainment page = adminSettings();

        // A previous run leaves a timer armed, so start from a known state.
        if (page.deletionScheduled()) {
            System.out.println(">>> a deletion was already scheduled — stopping it first");
            assertTrue(page.tapStopRow(), "Stop Project Deletion should be tappable");
            assertTrue(page.isConfirmDialogShown(Duration.ofSeconds(15)), "Stop should confirm");
            assertTrue(page.tapDialogYes(), "Yes should be tappable");
            assertTrue(page.waitForRowLabel(DeleteprojectEntertainment.ROW_DELETE, ROW_WAIT),
                    "The row should revert to " + DeleteprojectEntertainment.ROW_DELETE);
            page.waitForSnackbarToClear(MESSAGE_WAIT);
        }

        assertTrue(page.tapDeleteRow(), "Delete Project should be tappable");
        assertTrue(page.isOptionsSheetOpen(Duration.ofSeconds(15)), "The delay sheet should open");
        screenshot("delete-project-options");

        assertEquals(DeleteprojectEntertainment.CONFIRM_DELETE, page.optionsSheetHeader(),
                "The sheet should ask for confirmation");

        List<String> options = page.options();
        System.out.println(">>> options: " + options);
        assertTrue(options.contains(DeleteprojectEntertainment.OPT_12H), "Expected 12 Hours; got " + options);
        assertTrue(options.contains(DeleteprojectEntertainment.OPT_24H), "Expected 24 Hours; got " + options);
        assertTrue(options.contains(DeleteprojectEntertainment.OPT_48H), "Expected 48 Hours; got " + options);
        assertTrue(options.contains(DeleteprojectEntertainment.OPT_CANCEL), "Expected Cancel; got " + options);
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Cancel closes the sheet without scheduling anything")
    void cancelSchedulesNothing() {
        DeleteprojectEntertainment page = new DeleteprojectEntertainment(driver);
        assumeTrue(page.isOptionsSheetOpen(Duration.ofSeconds(10)), "The delay sheet is not open");

        assertTrue(page.chooseOption(DeleteprojectEntertainment.OPT_CANCEL), "Cancel should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)),
                "Cancelling should return to ADMIN SETTINGS");
        assertEquals(DeleteprojectEntertainment.ROW_DELETE, page.deleteRowLabel(),
                "Cancelling should leave nothing scheduled");
        screenshot("delete-project-cancelled");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Choosing 48 Hours schedules the deletion")
    void schedule48Hours() {
        DeleteprojectEntertainment page = adminSettings();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDeleteRow(), "Delete Project should be tappable");
        assertTrue(page.isOptionsSheetOpen(Duration.ofSeconds(15)), "The delay sheet should open");
        assertTrue(page.chooseOption(DeleteprojectEntertainment.OPT_48H), "48 Hours should be tappable");

        assertTrue(page.snackbarShown(DeleteprojectEntertainment.MARKED, MESSAGE_WAIT),
                "Expected \"" + DeleteprojectEntertainment.MARKED + "\"");
        assertTrue(page.waitForRowLabel(DeleteprojectEntertainment.ROW_STOP, ROW_WAIT),
                "The row should become \"" + DeleteprojectEntertainment.ROW_STOP + "\"");
        screenshot("delete-project-scheduled-48h");
    }

    @Test
    @Order(4)
    @DisplayName("TC4: Stopping asks to confirm, and No keeps the deletion scheduled")
    void stopConfirmationDeclined() {
        DeleteprojectEntertainment page = adminSettings();
        assumeTrue(page.deletionScheduled(), "No deletion is scheduled");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapStopRow(), "Stop Project Deletion should be tappable");
        assertTrue(page.isConfirmDialogShown(Duration.ofSeconds(15)),
                "Stopping should ask for confirmation");
        assertEquals(DeleteprojectEntertainment.CONFIRM_STOP, page.dialogMessage(),
                "Confirmation wording should match the app");
        screenshot("delete-project-stop-confirm");

        assertTrue(page.tapDialogNo(), "No should be tappable");
        assertEquals(DeleteprojectEntertainment.ROW_STOP, page.deleteRowLabel(),
                "Declining should leave the deletion scheduled");
    }

    @Test
    @Order(5)
    @DisplayName("TC5: Confirming the stop cancels the deletion")
    void stopConfirmed() {
        DeleteprojectEntertainment page = adminSettings();
        assumeTrue(page.deletionScheduled(), "No deletion is scheduled");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapStopRow(), "Stop Project Deletion should be tappable");
        assertTrue(page.isConfirmDialogShown(Duration.ofSeconds(15)), "The dialog should appear again");
        assertTrue(page.tapDialogYes(), "Yes should be tappable");

        assertTrue(page.snackbarShown(DeleteprojectEntertainment.STOPPED, MESSAGE_WAIT),
                "Expected \"" + DeleteprojectEntertainment.STOPPED + "\"");
        assertTrue(page.waitForRowLabel(DeleteprojectEntertainment.ROW_DELETE, ROW_WAIT),
                "The row should revert to \"" + DeleteprojectEntertainment.ROW_DELETE + "\"");
        screenshot("delete-project-stopped");
    }

    @Test
    @Order(6)
    @DisplayName("TC6: Choosing 12 Hours schedules the deletion again")
    void schedule12Hours() {
        DeleteprojectEntertainment page = adminSettings();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDeleteRow(), "Delete Project should be tappable");
        assertTrue(page.isOptionsSheetOpen(Duration.ofSeconds(15)), "The delay sheet should open");
        assertTrue(page.chooseOption(DeleteprojectEntertainment.OPT_12H), "12 Hours should be tappable");

        assertTrue(page.snackbarShown(DeleteprojectEntertainment.MARKED, MESSAGE_WAIT),
                "Expected \"" + DeleteprojectEntertainment.MARKED + "\"");
        assertTrue(page.waitForRowLabel(DeleteprojectEntertainment.ROW_STOP, ROW_WAIT),
                "The row should become \"" + DeleteprojectEntertainment.ROW_STOP + "\"");
        screenshot("delete-project-scheduled-12h");
    }

    @Test
    @Order(7)
    @DisplayName("TC7: A running countdown appears on the Settings tab, matching 12 hours")
    void countdownRunsFromTwelveHours() {
        DeleteprojectEntertainment page = adminSettings();
        assumeTrue(page.deletionScheduled(), "No deletion is scheduled — TC6 did not arm one");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapBack(), "Back should be tappable on Admin Settings");
        assertTrue(page.isOnSettingsTab(Duration.ofSeconds(20)),
                "Back should land on the Settings tab");

        assertTrue(page.countdownShown(Duration.ofSeconds(30)),
                "A countdown should appear once a deletion is scheduled");

        String shown = page.countdownText();
        long seconds = page.countdownSeconds();
        System.out.println(">>> deletion countdown: " + shown + "  (" + seconds + "s remaining)");
        screenshot("delete-project-countdown");

        assertTrue(seconds > 0, "The countdown should parse as a time; got '" + shown + "'");
        assertTrue(seconds <= TWELVE_HOURS,
                "A 12-hour deletion should not show more than 12:00:00; got " + shown);
        assertTrue(seconds >= LOWER_BOUND,
                "A 12-hour deletion should start close to 12:00:00; got " + shown);

        // Presence is not enough — a frozen label would pass. Two samples prove it ticks.
        assertTrue(page.countdownIsRunning(Duration.ofSeconds(3)),
                "The countdown should be ticking down, not static");
        System.out.println(">>> countdown is running; deletion timing verified");
    }
}
