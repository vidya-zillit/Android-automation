package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxSchedulePage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX SHELL — the module's two views, their sub-modes, the default-view setting and the
 * drawer. Read-only: this suite creates nothing, so it can say whether the module is
 * reachable before any later failure gets blamed on it.
 *
 * <p>Sheet: BOX_MODULE_TESTCASES.md §1.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_SHELL)
class BoxShellTest extends BoxBaseTest {

    @Test
    @Order(1)
    @DisplayName("BS-01 The module opens from Tools with its header")
    void moduleOpens() {
        BoxSchedulePage b = box();
        assertTrue(b.isLoaded(Duration.ofSeconds(10)), "The module did not load");
        // "Prepared: …" is the line naming who prepared the schedule. Absent is not a
        // failure on a project that has never been prepared, so this only checks that
        // when it IS there it carries something.
        String prepared = b.preparedText();
        if (prepared != null && !prepared.isBlank()) {
            assertTrue(prepared.toLowerCase().contains("prepared"),
                    "The header line reads \"" + prepared + "\", which does not look like"
                            + " the \"Prepared: …\" label");
        }
    }

    @Test
    @Order(2)
    @DisplayName("BS-02 Calendar and List views both open, and switching back works")
    void bothViewsOpen() {
        BoxSchedulePage b = box();
        assumeTrue(b.openCalendarView(), "Calendar view button not available");
        sleep(1500);
        assertTrue(b.calendarModeShown(),
                "Calendar view is selected but its Month/Week/Day controls are absent");

        assumeTrue(b.openListView(), "List view button not available");
        sleep(1500);
        assertTrue(b.listModeShown(),
                "List view is selected but its By Date / By Schedule controls are absent");

        // Back again — a toggle that only works one way is a real defect.
        assumeTrue(b.openCalendarView(), "Could not switch back to Calendar");
        sleep(1500);
        assertTrue(b.calendarModeShown(), "Switching back to Calendar did not restore it");
    }

    @Test
    @Order(3)
    @DisplayName("BS-03 Month, Week and Day each render")
    void calendarSubModes() {
        BoxSchedulePage b = box();
        assumeTrue(b.openCalendarView(), "Calendar view not available");
        sleep(1200);
        assumeTrue(b.calendarModeShown(), "The Month/Week/Day controls are absent");

        for (String mode : List.of("week", "day", "month")) {
            boolean picked = switch (mode) {
                case "week" -> b.chooseWeek();
                case "day" -> b.chooseDay();
                default -> b.chooseMonth();
            };
            assertTrue(picked, "Could not select the " + mode + " view");
            sleep(1800);
            // The controls must survive the switch — a mode that blanks the toggle has
            // taken the user somewhere they cannot get back from.
            assertTrue(b.calendarModeShown(),
                    "After choosing " + mode + " the Month/Week/Day controls disappeared");
        }
    }

    @Test
    @Order(4)
    @DisplayName("BS-04 By Date and By Schedule each render")
    void listSubModes() {
        BoxSchedulePage b = box();
        assumeTrue(b.openListView(), "List view not available");
        sleep(1200);
        assumeTrue(b.listModeShown(), "The By Date / By Schedule controls are absent");

        assertTrue(b.chooseBySchedule(), "Could not select By Schedule");
        sleep(1800);
        assertTrue(b.listModeShown(), "By Schedule blanked the mode controls");

        assertTrue(b.chooseByDate(), "Could not select By Date");
        sleep(1800);
        assertTrue(b.listModeShown(), "By Date blanked the mode controls");
    }

    @Test
    @Order(5)
    @DisplayName("BS-05 Set as Default offers both views and remembers List")
    void defaultViewList() {
        BoxSchedulePage b = box();
        assumeTrue(b.openSetDefault(), "Set-as-default is not offered");
        sleep(1200);
        assertTrue(b.textShown("Calendar") && b.textShown("List"),
                "The default-view chooser does not offer both views");

        assumeTrue(b.tapText("List"), "Could not choose List as the default");
        sleep(1500);
        backToBox();
        leaveAndReturn();
        assertTrue(new BoxSchedulePage(driver).listModeShown(),
                "The module was set to open on List and did not. The setting lives in"
                        + " SharedPref, so this should survive leaving the module.");
    }

    @Test
    @Order(6)
    @DisplayName("BS-06 …and remembers Calendar, when set back")
    void defaultViewCalendar() {
        // DELIBERATELY SEPARATE FROM BS-05. Setting the default to the value it already
        // holds proves nothing — only a change in both directions shows the setting is
        // read rather than defaulted.
        BoxSchedulePage b = box();
        assumeTrue(b.openSetDefault(), "Set-as-default is not offered");
        sleep(1200);
        assumeTrue(b.tapText("Calendar"), "Could not choose Calendar as the default");
        sleep(1500);
        backToBox();
        leaveAndReturn();
        assertTrue(new BoxSchedulePage(driver).calendarModeShown(),
                "The module was set to open on Calendar and did not");
    }

    @Test
    @Order(7)
    @DisplayName("BS-08 The drawer offers every action")
    void drawerActions() {
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open");
        List<String> actions = b.drawerActions();

        // Create actions are posting-gated; the read-only ones are not. Assert the ones
        // that must always be there, and report the whole set when something is missing.
        assertTrue(actions.contains("drawerHistory"),
                "History is missing from the drawer. Present: " + actions);
        assertTrue(actions.contains("drawerCreateSchedule")
                        || actions.contains("drawerCreateEvent")
                        || actions.contains("drawerCreateNote"),
                "Not one create action is offered. With posting rights all three should"
                        + " be. Present: " + actions);
        b.closeDrawer();
    }

    @Test
    @Order(8)
    @DisplayName("BS-08b The drawer closes again without leaving the module")
    void drawerCloses() {
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open");
        assertTrue(b.closeDrawer(), "The drawer would not close");
        sleep(1200);
        assertTrue(b.isLoaded(Duration.ofSeconds(8)),
                "Closing the drawer left the module altogether");
        assertFalse(b.drawerActions().contains("drawerCreateSchedule"),
                "The drawer reports closed but its actions are still on screen");
    }

    @Test
    @Order(9)
    @DisplayName("BS-10 Back returns to Tools, not out of the app")
    void backGoesToTools() {
        BoxSchedulePage b = box();
        assumeTrue(b.isLoaded(Duration.ofSeconds(8)), "The module is not loaded");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(2500);

        assertTrue(onBottomNavScreen() || new ToolsPage(driver).isLoaded(Duration.ofSeconds(10)),
                "Back from the module did not land on Tools. Resumed activity: "
                        + resumedActivity());
        // And emphatically NOT out of the app.
        assertFalse(onProjectList(),
                "Back from the module walked all the way out to the project list");
        openBoxSchedule();
    }

    /** Leaves the module and comes back, to prove a setting was stored and not remembered. */
    private void leaveAndReturn() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(2000);
        openBoxSchedule();
        sleep(1500);
    }
}
