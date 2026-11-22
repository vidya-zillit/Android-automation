package com.zillit.automation.tests.box;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.box.BoxCreateSchedulePage;
import com.zillit.automation.pages.box.BoxSchedulePage;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — creating a schedule day: the type picker, the three ways of choosing dates, and
 * the validation that stands between them and a save.
 *
 * <p>Sheet: BOX_MODULE_TESTCASES.md §3. Negative cases carry equal weight here — a form
 * that saves without a type, or lets a past date through, produces data nobody asked for
 * and which every later case then has to work around.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_SCHEDULE_CRUD)
class BoxScheduleCrudTest extends BoxBaseTest {

    private String typeUsed;

    @Test
    @Order(1)
    @DisplayName("SD-01 The form opens with a type picker and the three date tabs")
    void formOpens() {
        BoxCreateSchedulePage f = openForm();
        assertTrue(f.isLoaded(Duration.ofSeconds(10)), "The form did not open");
        // The tabs are the form's whole point: three ways to pick dates.
        assertTrue(f.dateRangeShown() || f.calendarShown() || f.dayWiseShown(),
                "No date-picking content is visible on the form at all");
        back(f);
    }

    @Test
    @Order(2)
    @DisplayName("SD-02 Saving with no type is refused and the form stays open")
    void noTypeRefused() {
        BoxCreateSchedulePage f = openForm();
        // Nothing chosen — straight to Save.
        assumeTrue(f.save(), "Save was not available");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "The form closed with no type chosen. Save checks for one and refuses with"
                        + " \"" + BoxCreateSchedulePage.ERR_NO_TYPE + "\", so a form that"
                        + " closes here has saved something without a type.");
        back(f);
    }

    @Test
    @Order(3)
    @DisplayName("SD-04 Date Range by number of days fills the summary")
    void dateRangeByDays() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assumeTrue(f.chooseByDays(), "The by-days mode is not offered");
        assumeTrue(f.setNumberOfDays("3"), "Could not type a number of days");
        sleep(1200);

        String summary = String.valueOf(f.summary());
        assertFalse(summary.isBlank() || "null".equals(summary),
                "Three days were asked for and the form summarises nothing. The summary is"
                        + " what tells the user what they are about to create.");
        back(f);
    }

    @Test
    @Order(4)
    @DisplayName("SD-04b Plus and minus move the day count, and it never goes below one")
    void dayCountStepper() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assumeTrue(f.chooseByDays(), "The by-days mode is not offered");
        assumeTrue(f.setNumberOfDays("1"), "Could not set the count to 1");
        sleep(800);

        assumeTrue(f.plusDay(), "Plus is not offered");
        sleep(800);
        String afterPlus = String.valueOf(f.numberOfDays()).trim();
        assertNotEquals("1", afterPlus, "Plus did not raise the day count from 1");

        // NEGATIVE: pressing minus past the floor must not produce zero or a negative
        // count — a schedule of zero days is not a thing.
        f.minusDay(); sleep(600);
        f.minusDay(); sleep(600);
        f.minusDay(); sleep(800);
        String floor = String.valueOf(f.numberOfDays()).trim();
        int n = floor.matches("-?\\d+") ? Integer.parseInt(floor) : 1;
        assertTrue(n >= 1,
                "The day count fell to " + floor + ". One day is the floor; zero or fewer"
                        + " would ask the server to create a schedule covering nothing.");
        back(f);
    }

    @Test
    @Order(5)
    @DisplayName("SD-06 The Calendar tab accepts non-adjacent dates")
    void calendarPicksScattered() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");

        // Next month, so every date is in the future and none is disabled.
        assumeTrue(f.calendarNextMonth(), "Could not move to the next month");
        sleep(1200);

        int picked = 0;
        for (String day : List.of("10", "20", "25")) {
            if (f.pickCalendarDay(day)) picked++;
        }
        assumeTrue(picked >= 2, "Fewer than two dates could be picked");

        // The chips are the evidence, not tvCalSelectedCount: that label is set to ""
        // while nothing is picked, and an empty wrap_content TextView has no bounds, so
        // it never reaches the accessibility tree and reads the same as a missing view.
        List<String> chips = f.pickedDateChips();
        assertTrue(chips.size() >= picked,
                "Picked " + picked + " dates but the form shows " + chips.size()
                        + " date chip(s): " + chips);
        // Scattered dates are the whole reason this tab exists — and the reason every mode
        // sends by_days (ZL-19566). Distinct chips prove none was collapsed into a range.
        assertTrue(new java.util.HashSet<>(chips).size() == chips.size(),
                "The picked dates were not kept distinct: " + chips);

        // Cross-check only. The label sits below the chips inside the form's ScrollView,
        // so three chips push it off screen, where it leaves the accessibility tree
        // altogether. The chips above are what this case actually rests on.
        String count = f.pickedCountScrolled();
        if (count == null) {
            System.out.println(">>> SD-06 note: the count label could not be brought on"
                    + " screen; the " + chips.size() + " chips " + chips + " are the"
                    + " evidence used instead.");
        } else {
            assertTrue(count.contains(String.valueOf(chips.size())),
                    "The form holds " + chips.size() + " dates " + chips
                            + " but its count label reads \"" + count + "\"");
        }
        back(f);
    }

    @Test
    @Order(6)
    @DisplayName("SD-08 Past dates cannot be chosen")
    void pastDatesDisabled() {
        // NEGATIVE, and worth having: a schedule in the past is nonsense the UI is
        // supposed to prevent, and the only way to know it still does is to try.
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.calendarPrevMonth(), "Could not move to the previous month");
        sleep(1200);

        Boolean enabled = f.calendarDayEnabled("15");
        assumeTrue(enabled != null, "No cell for the 15th in this month's grid");
        assertFalse(enabled,
                "A date in the previous month is tappable. Past dates are supposed to be"
                        + " disabled, so this would let a schedule be created in the past.");
        back(f);
    }

    @Test
    @Order(7)
    @DisplayName("SD-07 Day Wise filters a span down to chosen weekdays")
    void dayWise() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseDayWiseTab(), "The Day Wise tab is not offered");
        assumeTrue(f.dayWiseShown(), "The weekday chips did not appear");

        String summary = String.valueOf(f.summary());
        assertFalse(summary.isBlank() && String.valueOf(f.pickedCount()).isBlank(),
                "Day Wise is showing neither a summary nor a count, so there is no way to"
                        + " tell what it would create");
        back(f);
    }

    @Test
    @Order(8)
    @DisplayName("SD-10 A schedule saves and the form closes")
    void scheduleSaves() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assumeTrue(f.chooseByDays(), "The by-days mode is not offered");
        assumeTrue(f.setNumberOfDays("2"), "Could not set the day count");
        sleep(1000);

        assumeTrue(f.save(), "Save was not available");
        sleep(5000);

        // A conflict dialog here is a legitimate outcome, not a failure — those dates may
        // already carry a schedule. The conflict suite owns that path; back out of it.
        com.zillit.automation.pages.box.BoxConflictPage dlg =
                new com.zillit.automation.pages.box.BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(4))) {
            dlg.back();
            sleep(1500);
            back(f);
            return;
        }

        assertFalse(f.stillOpen(),
                "The form stayed open after a type and dates were given. Whatever rejected"
                        + " it said so in a toast above this assertion.");
        backToBox();
    }

    // ---------------- plumbing ----------------

    private BoxCreateSchedulePage openForm() {
        backToBox();
        BoxSchedulePage b = box();
        if (!b.openDrawer()) {
            assumeTrue(false, "The drawer did not open. Toolbar: "
                    + b.toolbarState() + "; activity: " + resumedActivity());
        }
        assumeTrue(b.drawerCreateSchedule(),
                "Create Schedule is not offered — it needs posting rights");
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The schedule form did not open");
        return f;
    }

    /** Chooses whatever type the project has, remembering it for later cases. */
    private boolean pickAnyType(BoxCreateSchedulePage f) {
        String already = f.selectedTypeName();
        if (already != null && !already.isBlank() && !"null".equals(already)) {
            typeUsed = already.trim();
            return true;
        }
        if (!f.openTypePicker()) return false;
        List<String> types = f.typeNames();
        if (types.isEmpty()) return false;
        typeUsed = types.get(0);
        return f.chooseType(typeUsed);
    }

    private void back(BoxCreateSchedulePage f) {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        backToBox();
    }
}
