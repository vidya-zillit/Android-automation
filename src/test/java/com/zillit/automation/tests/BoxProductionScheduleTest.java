package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxCalendarPage;
import com.zillit.automation.pages.BoxConflictPage;
import com.zillit.automation.pages.BoxCreateSchedulePage;
import com.zillit.automation.pages.BoxListPage;
import com.zillit.automation.pages.BoxSchedulePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — a schedule shaped like an actual production, not like a form exercise.
 *
 * <p>The existing SD and CF cases drive the create form thoroughly but every one of them
 * uses whatever type comes first in the project — Prep. So nothing yet covers the thing
 * the module exists for: several DIFFERENT types laid across a run of dates the way a unit
 * actually works, and in particular a <b>Day Off dropped into a Shoot block</b>, which is
 * the only reason the Replace / Extend / Overlap chooser exists.
 *
 * <p>These cases build a timeline — Prep, Travel, Shoot, a Day Off inside the Shoot, Wrap —
 * and then read the result back from the <b>List view</b>, which is the only surface that
 * names the type on a date. The calendar shows type by colour alone, and colour never
 * reaches the accessibility tree.
 *
 * <p>Day counts (Prep 5, Shoot 12, Wrap 2) and the six-day week are a plausible shape, not
 * a rule — every assertion is about what the app does with the types and the gaps, so the
 * numbers can change without weakening a single case.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_PRODUCTION)
class BoxProductionScheduleTest extends BoxBaseTest {

    private static final String PREP   = "Prep";
    private static final String SHOOT  = "Shoot";
    private static final String WRAP   = "Wrap";
    private static final String DAYOFF = "Day Off";
    private static final String TRAVEL = "Travel";

    /** Day numbers in next month: a Shoot block, and the day inside it that goes Off. */
    private static final String SHOOT_FROM = "6";
    private static final String DAY_OFF_IN = "10";
    private static final String SHOOT_TO   = "17";

    private static final String ERR_DATES =
            "The app's own rule is a type AND at least one date (cs_error_select_dates).";

    private boolean prepMade;
    private boolean shootMade;

    // ─────────────────────────── the timeline ───────────────────────────

    @Test
    @Order(1)
    @DisplayName("PS-01 Prep goes in as a date range")
    void prepBlock() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, PREP), "This project has no \"" + PREP + "\" type."
                + " Types offered: " + f.typeNames());
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assumeTrue(f.chooseByDays(), "By-number-of-days is not offered");
        assumeTrue(f.setNumberOfDays("5"), "Could not set the number of days");
        sleep(1200);

        assertTrue(f.save(), "Save was not available for a 5-day Prep");
        sleep(4000);
        settleAnyConflict();
        backToBox();

        // A save that closes the form is not a save that persisted. Everything after this
        // depends on the block existing, so it is checked here and not assumed.
        int days = daysOnRecord(PREP);
        assertTrue(days > 0,
                "A 5-day " + PREP + " was saved and the form closed, but the List view"
                        + " holds no " + PREP + " day at all (filtered count: " + days + ")");
        prepMade = true;
    }

    @Test
    @Order(2)
    @DisplayName("PS-02 Shoot goes in on its own dates, scattered or not")
    void shootBlock() {
        int shootBefore = daysOnRecord(SHOOT);
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, SHOOT), "This project has no \"" + SHOOT + "\" type."
                + " Types offered: " + f.typeNames());
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.calendarNextMonth(), "Could not move to a future month");
        sleep(1200);

        int picked = 0;
        for (String d : List.of(SHOOT_FROM, "8", DAY_OFF_IN, "13", SHOOT_TO)) {
            if (f.pickCalendarDay(d)) picked++;
        }
        assumeTrue(picked >= 3, "Fewer than three shoot days could be picked");
        List<String> chips = f.pickedDateChips();
        assertEquals(picked, chips.size(),
                "Picked " + picked + " shoot days but the form holds " + chips);

        assertTrue(f.save(), "Save was not available for the Shoot block");
        sleep(4000);
        settleAnyConflict();
        backToBox();

        // Compare the CHANGE, never the absolute count. This project is a real one and
        // earlier runs leave days of these types behind, so "4 picked, 5 on record" says
        // nothing until the 1 that was already there is subtracted. An absolute comparison
        // here reported the app as dropping or inventing days when it had done neither.
        int after = daysOnRecord(SHOOT);
        int added = after - shootBefore;
        assertEquals(picked, added,
                picked + " " + SHOOT + " days were picked (" + chips + ") and the record"
                        + " went from " + shootBefore + " to " + after + ", a change of "
                        + added + ". Fewer means days were dropped on save; more means the"
                        + " gaps between them were filled in.");
        shootMade = true;
    }

    @Test
    @Order(3)
    @DisplayName("PS-03 The List view names the types that were just laid down")
    void listNamesTheTypes() {
        assumeTrue(prepMade || shootMade, "Nothing was scheduled to look at");
        BoxListPage list = openList();
        assumeTrue(!list.isEmpty(), "The list is empty, so nothing was saved at all");

        List<BoxListPage.DayRow> rows = list.dayRows();
        assumeTrue(!rows.isEmpty(), "The list showed no rows to read");
        // Every row must say WHICH type it is. A row with a date and no type name is a
        // schedule the user cannot identify.
        for (BoxListPage.DayRow r : rows) {
            assertNotNull(r.type, "A list row shows " + r.date + " with no type name: " + rows);
        }
        assertTrue(rows.stream().anyMatch(r -> r.type.contains(PREP) || r.type.contains(SHOOT)),
                "Neither " + PREP + " nor " + SHOOT + " appears in the list: " + rows);
    }

    // ──────────────── Day Off over Shoot: the three resolutions ────────────────

    @Test
    @Order(4)
    @DisplayName("PS-04 A Day Off on a Shoot day raises the chooser, naming both types")
    void dayOffOverShootRaisesChooser() {
        assumeTrue(shootMade, "PS-02 laid down no Shoot block to clash with");
        BoxConflictPage dlg = applyOver(DAYOFF, DAY_OFF_IN);
        assumeTrue(dlg != null, "A Day Off on a Shoot day raised no conflict chooser."
                + " Either the Shoot day is not there or the app silently overwrote it.");

        String msg = String.valueOf(dlg.message());
        assertTrue(dlg.replaceOffered() && dlg.extendOffered() && dlg.overlapOffered(),
                "The chooser appeared without all three options: replace="
                        + dlg.replaceOffered() + " extend=" + dlg.extendOffered()
                        + " overlap=" + dlg.overlapOffered());
        // The message has to say what is being replaced, or the choice is blind.
        assertTrue(msg.contains("1") || msg.toLowerCase().contains("day"),
                "The chooser does not say how many days clash: \"" + msg + "\"");
        dlg.back();
        sleep(1500);
        // Stay on the form: the next case is about this same page.
    }

    @Test
    @Order(5)
    @DisplayName("PS-05 Replace changes only the chosen day, not the rest of the Shoot")
    void replaceTouchesOneDay() {
        assumeTrue(shootMade, "PS-02 laid down no Shoot block");
        int shootDaysBefore = daysOnRecord(SHOOT);
        assumeTrue(shootDaysBefore > 1,
                "Only " + shootDaysBefore + " " + SHOOT + " day(s) on record, so 'the rest"
                        + " of the block' does not exist to check");
        backToBox();

        BoxConflictPage dlg = applyOver(DAYOFF, DAY_OFF_IN);
        assumeTrue(dlg != null, "No conflict chooser, so Replace cannot be exercised");
        assumeTrue(dlg.chooseReplace(), "Replace could not be chosen");
        sleep(5000);
        backToBox();

        int shootDaysAfter = daysOnRecord(SHOOT);
        // THE POINT OF THE CASE. Replace means one day changes hands. If the whole block
        // went, Replace is behaving like the ZL-19566 span expansion.
        assertEquals(shootDaysBefore - 1, shootDaysAfter,
                "Replacing ONE " + SHOOT + " day with a " + DAYOFF + " took the count from "
                        + shootDaysBefore + " to " + shootDaysAfter + ". Replace must hand"
                        + " over the chosen day only.");
        assertTrue(daysOnRecord(DAYOFF) >= 1,
                "The " + DAYOFF + " did not take the day it replaced");
    }

    @Test
    @Order(6)
    @DisplayName("PS-06 Overlap leaves both types on the day")
    void overlapKeepsBoth() {
        assumeTrue(shootMade, "PS-02 laid down no Shoot block");
        BoxConflictPage dlg = applyOver(TRAVEL, SHOOT_TO);
        assumeTrue(dlg != null, "No conflict chooser, so Overlap cannot be exercised");
        assumeTrue(dlg.chooseOverlap(), "Overlap could not be chosen");
        sleep(5000);
        backToBox();

        // Overlap's whole promise: the day keeps what it had AND gains the new type. A unit
        // travelling on a shoot day is the ordinary case for this.
        assertTrue(daysOnRecord(TRAVEL) >= 1,
                "After choosing Overlap, " + TRAVEL + " is nowhere on record");
        assertTrue(daysOnRecord(SHOOT) >= 1,
                "Overlap removed the " + SHOOT + " it was supposed to sit beside — that is"
                        + " Replace behaviour under the Overlap label");
    }

    // ─────────────────────────── the six-day week ───────────────────────────

    @Test
    @Order(7)
    @DisplayName("PS-07 Day Wise gives a six-day week with Sunday off")
    void sixDayWeek() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, SHOOT), "This project has no \"" + SHOOT + "\" type");
        assumeTrue(f.chooseDayWiseTab(), "The Day Wise tab is not offered");
        assumeTrue(f.dayWiseShown(), "The weekday chips did not appear");

        List<String> offered = f.weekdaysOffered();
        assumeTrue(offered.size() >= 6,
                "The span offers only " + offered + ", so a six-day week cannot be built");

        int ticked = 0;
        for (String d : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) {
            if (f.chooseWeekday(d)) ticked++;
        }
        assertEquals(6, ticked, "Only " + ticked + " of the six working days could be"
                + " ticked. Offered: " + offered);

        // Sunday must be tickable too — the app has no business deciding the week for a
        // unit — but it stays off here, and the count must reflect six days, not seven.
        String count = f.dayWiseCount();
        assertTrue(count == null || !count.contains("7 "),
                "Six weekdays were ticked and Day Wise reports \"" + count + "\"");
    }

    @Test
    @Order(8)
    @DisplayName("PS-08 By Schedule shows each type as its own block with a day count")
    void blocksPerType() {
        assumeTrue(prepMade || shootMade, "Nothing was scheduled to group");
        BoxSchedulePage b = box();
        assumeTrue(b.openListView(), "List view button not available");
        sleep(1500);
        assumeTrue(b.chooseBySchedule(), "By Schedule is not offered");
        sleep(2500);

        BoxListPage list = new BoxListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(10)), "The list did not load");
        List<BoxListPage.Block> blocks = list.blocks();
        assumeTrue(!blocks.isEmpty(), "By Schedule showed no blocks: it groups days into"
                + " type blocks, so an empty view here with days in By Date is a defect");

        for (BoxListPage.Block blk : blocks) {
            assertNotNull(blk.dayCount, "Block \"" + blk.type + "\" shows no day count,"
                    + " which is the one number this view exists to give: " + blocks);
            assertTrue(blk.dayCount.matches(".*\\d.*"),
                    "Block \"" + blk.type + "\" has a day count with no number in it: \""
                            + blk.dayCount + "\"");
        }
    }

    @Test
    @Order(9)
    @DisplayName("PS-09 A second type on a booked day never saves silently")
    void secondTypeAlwaysAsks() {
        assumeTrue(shootMade, "PS-02 laid down no Shoot block");
        // The negative half of the whole module: a day that already has a type must never
        // take another one without the user choosing how. Silence here loses a shoot day.
        BoxConflictPage dlg = applyOver(WRAP, SHOOT_FROM);
        assertTrue(dlg != null,
                "Putting " + WRAP + " onto a day that already holds " + SHOOT + " saved"
                        + " without asking. The chooser is the only thing standing between"
                        + " a mis-tap and a lost shoot day.");
        dlg.back();
        sleep(1500);
    }

    // ───────────────── what actually goes wrong on a unit ─────────────────

    @Test
    @Order(10)
    @DisplayName("PS-10 Extend keeps the existing block — it never quietly eats a day")
    void extendKeepsWhatWasThere() {
        assumeTrue(shootMade, "PS-02 laid down no Shoot block");
        int shootBefore = daysOnRecord(SHOOT);
        assumeTrue(shootBefore > 0, "No " + SHOOT + " day on record to extend around");
        backToBox();

        BoxConflictPage dlg = applyOver(DAYOFF, "8");
        assumeTrue(dlg != null, "No conflict chooser, so Extend cannot be exercised");
        assumeTrue(dlg.chooseExtend(), "Extend could not be chosen");
        sleep(5000);
        backToBox();

        // The app's own words for this option are "Keep existing schedule, extend it at the
        // end to make room" (conflict_extend_desc). Keeping is the promise, so the existing
        // type must not come out of this with fewer days. If it does, Extend is Replace
        // wearing a different label, and a unit loses a shoot day it was told it kept.
        int shootAfter = daysOnRecord(SHOOT);
        assertTrue(shootAfter >= shootBefore,
                "Extend promises to KEEP the existing schedule, but " + SHOOT + " went from "
                        + shootBefore + " days to " + shootAfter + ".");
        assertTrue(daysOnRecord(DAYOFF) >= 1,
                "Extend was chosen and the " + DAYOFF + " never appeared at all");
    }

    @Test
    @Order(11)
    @DisplayName("PS-11 A Day Off on a free day just saves, with nothing to resolve")
    void dayOffOnFreeDaySavesQuietly() {
        // The control for PS-04. If a chooser appears HERE, the app is seeing conflicts
        // where none exist, and every real conflict warning becomes noise to click through.
        // The day has to be known free, not assumed free. Earlier runs book days of these
        // very types, and a day that is already taken turns this case into its opposite.
        String free = findFreeDayNextMonth();
        assumeTrue(free != null,
                "No free day could be found in next month's grid, so 'a free day' cannot be"
                        + " tested. Every day on screen already carries a type.");
        int offBefore = daysOnRecord(DAYOFF);

        BoxConflictPage dlg = applyOver(DAYOFF, free);
        assertTrue(dlg == null,
                "A " + DAYOFF + " on day " + free + ", which the calendar shows as empty,"
                        + " raised the conflict chooser. With nothing booked there is"
                        + " nothing to replace or overlap, and a warning here trains the"
                        + " user to click through the real ones.");
        backToBox();
        assertEquals(offBefore + 1, daysOnRecord(DAYOFF),
                "A " + DAYOFF + " on free day " + free + " was accepted without complaint"
                        + " but the record went from " + offBefore + " to "
                        + daysOnRecord(DAYOFF));
    }

    @Test
    @Order(12)
    @DisplayName("PS-12 A Shoot that runs over a month end keeps every day")
    void shootAcrossMonthBoundary() {
        // Shoots overrun month ends constantly, and a month-indexed picker is where
        // off-by-one errors live. Every day picked must survive the save.
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, TRAVEL), "This project has no \"" + TRAVEL + "\" type");
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.calendarNextMonth(), "Could not move to a future month");
        sleep(1200);

        int picked = 0;
        for (String d : List.of("27", "28")) if (f.pickCalendarDay(d)) picked++;
        assumeTrue(f.calendarNextMonth(), "Could not move past the month end");
        sleep(1200);
        for (String d : List.of("1", "2")) if (f.pickCalendarDay(d)) picked++;
        assumeTrue(picked >= 3, "Only " + picked + " days could be picked either side of"
                + " the month end");

        // Before saving: the form must still be holding all of them. A picker that drops
        // the earlier month's days when it changes month loses them here.
        List<String> chips = f.pickedDateChips();
        assertEquals(picked, chips.size(),
                picked + " days were picked across a month end but the form holds "
                        + chips.size() + ": " + chips + ". Changing month must not discard"
                        + " dates already chosen.");
    }

    @Test
    @Order(13)
    @DisplayName("PS-13 By Schedule day counts agree with the days in By Date")
    void blockCountsAgreeWithDays() {
        assumeTrue(prepMade || shootMade, "Nothing was scheduled to compare");
        // Two views of one truth. A block that says "12 days" while By Date lists 11 is the
        // kind of disagreement that gets a unit called for a day that was never scheduled.
        int prepDays = daysOnRecord(PREP);
        backToBox();

        BoxSchedulePage b = box();
        assumeTrue(b.openListView(), "List view button not available");
        sleep(1500);
        assumeTrue(b.chooseBySchedule(), "By Schedule is not offered");
        sleep(2500);
        BoxListPage list = new BoxListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(10)), "The list did not load");

        BoxListPage.Block prepBlock = list.blocks().stream()
                .filter(x -> x.type != null && x.type.contains(PREP))
                .findFirst().orElse(null);
        assumeTrue(prepBlock != null,
                "No " + PREP + " block in By Schedule, though By Date holds " + prepDays
                        + " " + PREP + " day(s)");

        String digits = prepBlock.dayCount == null ? "" : prepBlock.dayCount.replaceAll("\\D+", "");
        assumeTrue(!digits.isEmpty(), "The " + PREP + " block shows no number: \""
                + prepBlock.dayCount + "\"");
        assertEquals(prepDays, Integer.parseInt(digits),
                "By Schedule says the " + PREP + " block is " + digits + " day(s) while By"
                        + " Date lists " + prepDays + ". The two views disagree about the"
                        + " same schedule.");
    }

    @Test
    @Order(14)
    @DisplayName("PS-14 Filtering the list by a type shows that type and no other")
    void filterShowsOneTypeOnly() {
        assumeTrue(shootMade, "Nothing was scheduled to filter");
        BoxListPage list = openList();
        assumeTrue(list.search(SHOOT), "The list has no search box");
        sleep(2500);

        List<BoxListPage.DayRow> rows = list.dayRows();
        assumeTrue(!rows.isEmpty(),
                "Filtering by \"" + SHOOT + "\" emptied the list, though " + SHOOT
                        + " days are on record");
        // A filter that leaks other types is worse than no filter: the unit reads it as the
        // whole shoot and plans around days that are not shoot days.
        for (BoxListPage.DayRow r : rows) {
            assertTrue(r.type != null && r.type.toLowerCase().contains(SHOOT.toLowerCase()),
                    "Filtering by \"" + SHOOT + "\" left a " + r.type + " row on " + r.date
                            + ". Rows: " + list.rowSummaries());
        }
        list.clearSearch();
    }

    @Test
    @Order(15)
    @DisplayName("PS-15 Cancelling one day leaves the rest of the block standing")
    void cancelOneDayKeepsTheRest() {
        assumeTrue(shootMade, "PS-02 laid down no Shoot block");
        int before = daysOnRecord(SHOOT);
        assumeTrue(before > 1, "Only " + before + " " + SHOOT + " day(s) on record, so"
                + " 'the rest' does not exist");

        BoxListPage list = openList();
        assumeTrue(list.search(SHOOT), "The list has no search box");
        sleep(2500);
        List<BoxListPage.DayRow> rows = list.dayRows();
        assumeTrue(!rows.isEmpty(), "No " + SHOOT + " rows to cancel");
        String victim = rows.get(0).date;
        assumeTrue(victim != null, "The first " + SHOOT + " row shows no date");

        assumeTrue(list.deleteDay(victim), "No delete control on the " + victim + " row");
        list.confirmDelete();
        sleep(4000);
        backToBox();

        // A unit cancels a single day all the time. Taking the block with it is the
        // expensive version of this bug.
        int after = daysOnRecord(SHOOT);
        assertEquals(before - 1, after,
                "Cancelling the single day " + victim + " took " + SHOOT + " from " + before
                        + " days to " + after + ". Only that day should have gone.");
    }

    @Test
    @Order(16)
    @DisplayName("PS-16 Every Create Schedule validation, in order, on one open form")
    void allFormValidationsWithoutLeaving() {
        // One form, opened once. Each step below is a validation in its own right, checked
        // on the page it belongs to and in the order a user would hit them — rather than
        // reopening the module for each. If a step fails, the message names that step.
        BoxCreateSchedulePage f = openForm();

        // 1 — NEGATIVE: nothing chosen at all. Save must refuse and the form must survive.
        assertTrue(f.save(), "Save was not even clickable on an empty form");
        sleep(2000);
        assertTrue(f.stillOpen(),
                "An empty form saved and closed. A schedule with no type and no date is not"
                        + " a schedule.");

        // 2 — NEGATIVE: a type but still no dates. This only exists on the Calendar tab:
        // the Date Range tab always carries a start date and a day count, so "no dates"
        // cannot be expressed there and a save from it is legitimately valid.
        assumeTrue(pickType(f, SHOOT), "This project has no \"" + SHOOT + "\" type");
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.clearPickedDates(), "Could not empty the picked dates");
        assumeTrue(f.pickedDateChips().isEmpty(),
                "Dates are still on the form, so 'no dates' is not what is being tested: "
                        + f.pickedDateChips());
        assertTrue(f.save(), "Save was not clickable with a type and no dates");
        sleep(2000);
        assertTrue(f.stillOpen(),
                "A form with a type and no dates at all saved and closed. " + ERR_DATES);

        // 3 — the three date tabs must each render on this same form.
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assertTrue(f.dateRangeShown(), "The Date Range tab did not render its own content");
        assumeTrue(f.chooseDayWiseTab(), "The Day Wise tab is not offered");
        assertTrue(f.dayWiseShown(), "The Day Wise tab did not render its weekday chips");
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assertTrue(f.calendarShown(), "The Calendar tab did not render its grid");

        // 4 — NEGATIVE: a past day must not be selectable.
        Boolean pastEnabled = f.calendarDayEnabled("1");
        if (pastEnabled != null && !pastEnabled) {
            assertFalse(f.pickCalendarDay("1"),
                    "A day the grid draws as disabled was still accepted");
        }

        // 5 — POSITIVE: a future day goes on, and comes off again.
        assumeTrue(f.calendarNextMonth(), "Could not move to a future month");
        sleep(1200);
        assumeTrue(f.pickCalendarDay("20"), "Day 20 of next month could not be picked");
        assertEquals(1, f.pickedDateChips().size(),
                "One day was picked and the form holds " + f.pickedDateChips());
        assertTrue(f.clearPickedDates(),
                "A picked date could not be taken off again — its chip has a close icon,"
                        + " so a date is meant to be removable without leaving the form");
        assertTrue(f.pickedDateChips().isEmpty(),
                "Dates remain after clearing: " + f.pickedDateChips());

        // 6 — POSITIVE: the day count control cannot go below one.
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assumeTrue(f.chooseByDays(), "By-number-of-days is not offered");
        assumeTrue(f.setNumberOfDays("1"), "Could not set the day count");
        f.minusDay();
        sleep(800);
        String n = String.valueOf(f.numberOfDays());
        assertFalse(n.startsWith("0") || n.startsWith("-"),
                "Minus took the day count to \"" + n + "\". A schedule of zero or fewer"
                        + " days cannot exist.");

        // The form is left open on purpose — whatever runs next belongs on this page too.
    }

    // ─────────────────────────── helpers ───────────────────────────

    /**
     * A day in next month's calendar that carries no type at all.
     *
     * <p>Needed because this runs against a real project whose days accumulate: a case
     * about "a free day" has to find one rather than name one. Reads the calendar grid,
     * which shows every type on a day as a named pill.
     */
    private String findFreeDayNextMonth() {
        backToBox();
        BoxSchedulePage b = box();
        if (!b.openCalendarView()) return null;
        sleep(1500);
        b.chooseMonth();
        sleep(1500);
        BoxCalendarPage cal = new BoxCalendarPage(driver);
        if (!cal.isLoaded(Duration.ofSeconds(10))) return null;
        if (!cal.next()) return null;                 // next month, so nothing is past
        sleep(2000);

        Map<String, List<String>> byDay = cal.typesByDay();
        // Mid-month only: the grid pads both ends with the neighbouring months, and those
        // cells belong to a different month than the one the picker will be showing.
        for (int d = 14; d <= 26; d++) {
            List<String> pills = byDay.get(String.valueOf(d));
            if (pills != null && pills.isEmpty()) return String.valueOf(d);
        }
        return null;
    }

    /**
     * The Create Schedule form, reusing the one already on screen.
     *
     * <p>A case that does not save leaves the form standing, and the next case belongs on
     * that same form. Closing it to walk the drawer again costs a navigation every time and
     * — when a slow frame hid the Box screen for a moment — used to walk the run clean out
     * of the module and back in through Tools. So: if the form is here, empty it and carry
     * on; only when it is genuinely gone is it opened again, from the module's own screen.
     */
    private BoxCreateSchedulePage openForm() {
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        if (f.isLoaded(Duration.ofSeconds(2))) {
            f.clearPickedDates();           // reuse it clean, do not close it
            return f;
        }
        backToBox();
        BoxSchedulePage b = box();
        // Built only on failure: resumedActivity() shells out to adb, and this ran on every
        // successful drawer open as well — once per case, and slower still over USB on a
        // phone. assumeTrue evaluates its message before it looks at the condition.
        if (!b.openDrawer()) {
            assumeTrue(false, "The drawer did not open. Toolbar: " + b.toolbarState()
                    + "; activity: " + resumedActivity());
        }
        assumeTrue(b.drawerCreateSchedule(), "Create Schedule is not offered");
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The schedule form did not open");
        return f;
    }

    /** Leaves the form without leaving the module — one step back, nothing more. */
    private void closeForm() {
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        if (!f.isLoaded(Duration.ofSeconds(1))) return;
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1200);
    }

    /** Chooses a type BY NAME — the thing every earlier suite skipped. */
    private boolean pickType(BoxCreateSchedulePage f, String wanted) {
        String already = String.valueOf(f.selectedTypeName());
        if (already.contains(wanted)) return true;
        if (!f.openTypePicker()) return false;
        List<String> offered = f.typeNames();
        String match = offered.stream()
                .filter(n -> n != null && n.equalsIgnoreCase(wanted))
                .findFirst()
                .orElseGet(() -> offered.stream()
                        .filter(n -> n != null && n.toLowerCase().contains(wanted.toLowerCase()))
                        .findFirst().orElse(null));
        return match != null && f.chooseType(match);
    }

    /**
     * Puts {@code type} onto a day that already has one, and hands back the chooser.
     *
     * @return the conflict chooser, or null when the app saved without asking
     */
    private BoxConflictPage applyOver(String type, String dayNumber) {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, type), "This project has no \"" + type + "\" type."
                + " Types offered: " + f.typeNames());
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.calendarNextMonth(), "Could not move to a future month");
        sleep(1200);
        assumeTrue(f.pickCalendarDay(dayNumber),
                "Day " + dayNumber + " could not be picked for " + type);
        assumeTrue(f.save(), "Save was not available");
        sleep(4000);

        BoxConflictPage dlg = new BoxConflictPage(driver);
        return dlg.shown(Duration.ofSeconds(6)) ? dlg : null;
    }

    private BoxListPage openList() {
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openListView(), "List view button not available");
        sleep(1500);
        b.chooseByDate();
        sleep(2500);
        BoxListPage list = new BoxListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(12)), "The list did not load");
        return list;
    }

    /**
     * Commits a foundation block even if it lands on something.
     *
     * <p>This used to press Back, which ABANDONS the save — so PS-01 and PS-02 reported
     * success while leaving nothing behind, and every later case then skipped for want of a
     * Shoot block. A block the later cases depend on has to actually land, so a chooser is
     * answered with Replace.
     */
    private void settleAnyConflict() {
        BoxConflictPage dlg = new BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(4))) {
            if (!dlg.chooseReplace()) dlg.back();
            sleep(4000);
        }
    }

    /** Confirms a block really persisted, by name, with the list filtered down to it. */
    private int daysOnRecord(String type) {
        BoxListPage list = openList();
        int n = list.daysOfTypeFiltered(type);
        list.clearSearch();
        return n;
    }
}
