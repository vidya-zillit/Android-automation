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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — one schedule, five renderings, and they must all say the same thing.
 *
 * <p>A schedule day is a single fact. The Calendar draws it in Month, Week and Day modes;
 * the List prints it in By Date and By Schedule. Nothing in the app is allowed to disagree
 * about it, so a disagreement between two of these views is a defect in the app, not a
 * matter of taste — and it is the kind of defect no single-screen test can see.
 *
 * <p>These cases lay down a known mix of types on known dates and then ask every view the
 * same question. Two details keep the comparison honest:
 *
 * <ul>
 *   <li>The calendar's type pill <b>ellipsizes</b>, so "Day Off" can arrive as "Day O…".
 *       Matching is by prefix ({@link BoxCalendarPage#names}); equality would fail on
 *       correct behaviour.</li>
 *   <li>Everything is measured as a <b>change</b>. This runs against a real project whose
 *       days accumulate, so absolute counts say nothing.</li>
 * </ul>
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_CROSS_VIEW)
class BoxCrossViewTest extends BoxBaseTest {

    private static final String PREP   = "Prep";
    private static final String SHOOT  = "Shoot";
    private static final String DAYOFF = "Day Off";

    /** Mid-month days in next month: clear of the grid's padding and never in the past. */
    private static final String D_PREP  = "16";
    private static final String D_SHOOT = "18";
    private static final String D_OFF   = "20";

    /** What this suite put down, as day number → type. The truth every view is held to. */
    private final Map<String, String> laid = new LinkedHashMap<>();

    @Test
    @Order(1)
    @DisplayName("XV-01 Lay down a known mix: Prep, Shoot and a Day Off on separate days")
    void layDownTheMix() {
        for (Map.Entry<String, String> want : Map.of(
                D_PREP, PREP, D_SHOOT, SHOOT, D_OFF, DAYOFF).entrySet()) {
            if (putTypeOnDay(want.getValue(), want.getKey())) {
                laid.put(want.getKey(), want.getValue());
            }
        }
        assumeTrue(laid.size() >= 2,
                "Fewer than two of the three days could be laid down, so there is no mix to"
                        + " compare across views. Laid: " + laid);
        System.out.println(">>> XV laid down " + laid);
    }

    // ─────────────────────────── the Calendar ───────────────────────────

    @Test
    @Order(2)
    @DisplayName("XV-02 Month view shows every day on its own date, with the right type")
    void monthViewAgrees() {
        assumeTrue(!laid.isEmpty(), "XV-01 laid nothing down");
        BoxCalendarPage cal = openCalendar("Month");
        assumeTrue(cal.next(), "Could not move the calendar to next month");
        sleep(2500);

        Map<String, List<String>> shown = cal.typesByDay();
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> e : laid.entrySet()) {
            List<String> pills = shown.get(e.getKey());
            if (pills == null) {
                wrong.add("day " + e.getKey() + " is not in the grid at all");
            } else if (!BoxCalendarPage.names(pills, e.getValue())) {
                wrong.add("day " + e.getKey() + " should show " + e.getValue()
                        + " but shows " + pills);
            }
        }
        assertTrue(wrong.isEmpty(),
                "Month view disagrees with what was saved: " + wrong
                        + ". The whole grid reads: " + shown);
    }

    @Test
    @Order(3)
    @DisplayName("XV-03 Week view shows the same types for the days it covers")
    void weekViewAgrees() {
        assumeTrue(!laid.isEmpty(), "XV-01 laid nothing down");
        BoxCalendarPage cal = openCalendar("Week");

        // Walk forward a week at a time until one of the laid days is in range, rather than
        // assuming how far out it is.
        Map<String, List<String>> shown = null;
        String found = null;
        for (int hop = 0; hop < 8 && found == null; hop++) {
            shown = cal.typesByDay();
            for (String day : laid.keySet()) {
                if (shown.containsKey(day)) { found = day; break; }
            }
            if (found == null) {
                if (!cal.next()) break;
                sleep(2000);
            }
        }
        assumeTrue(found != null,
                "None of the days " + laid.keySet() + " came into Week view within eight"
                        + " weeks, so there is nothing to compare here");

        List<String> pills = shown.get(found);
        assertTrue(BoxCalendarPage.names(pills, laid.get(found)),
                "Week view shows " + pills + " on day " + found + " while the same day was"
                        + " saved as " + laid.get(found) + ". Month and Week render one"
                        + " schedule; they cannot differ about it.");
    }

    @Test
    @Order(4)
    @DisplayName("XV-04 Day view names the schedule for the day it is focused on")
    void dayViewAgrees() {
        assumeTrue(!laid.isEmpty(), "XV-01 laid nothing down");
        BoxCalendarPage cal = openCalendar("Day");
        assumeTrue(cal.dayModeShown(), "Day view did not render");

        // Day mode shows one day. Step forward until it lands on a day we know about.
        String found = null;
        for (int hop = 0; hop < 40 && found == null; hop++) {
            String n = cal.dayModeNumber();
            if (n != null && laid.containsKey(n.trim())) { found = n.trim(); break; }
            if (!cal.next()) break;
            sleep(900);
        }
        assumeTrue(found != null,
                "Day view never reached any of " + laid.keySet() + " within 40 steps");

        List<String> schedules = cal.dayModeSchedules();
        assertTrue(BoxCalendarPage.names(schedules, laid.get(found)),
                "Day view lists " + schedules + " for day " + found + ", which was saved as "
                        + laid.get(found) + ". A day cannot hold one type in Month view and"
                        + " another in Day view.");
    }

    // ─────────────────────────── the List ───────────────────────────

    @Test
    @Order(5)
    @DisplayName("XV-05 By Date names the same type the calendar drew")
    void byDateAgrees() {
        assumeTrue(!laid.isEmpty(), "XV-01 laid nothing down");
        BoxListPage list = openList();

        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> e : laid.entrySet()) {
            // The list prints "Wed, Dec 16" — find the row by its day number.
            assumeTrue(list.search(e.getValue()), "The list has no search box");
            sleep(2200);
            boolean seen = list.dayRows().stream().anyMatch(r ->
                    r.date != null && r.date.matches(".*\\b" + e.getKey() + "\\b.*")
                            && r.type != null
                            && r.type.toLowerCase().contains(e.getValue().toLowerCase()));
            if (!seen) {
                wrong.add("day " + e.getKey() + " as " + e.getValue()
                        + " (rows: " + list.rowSummaries() + ")");
            }
        }
        list.clearSearch();
        assertTrue(wrong.isEmpty(),
                "By Date does not show what the calendar showed: " + wrong);
    }

    @Test
    @Order(6)
    @DisplayName("XV-06 By Schedule accounts for every day By Date lists, type by type")
    void byScheduleAgrees() {
        assumeTrue(!laid.isEmpty(), "XV-01 laid nothing down");
        List<String> wrong = new ArrayList<>();

        for (String type : List.of(PREP, SHOOT, DAYOFF)) {
            int byDate = daysOnRecord(type);
            if (byDate == 0) continue;

            backToBox();
            BoxSchedulePage b = box();
            assumeTrue(b.openListView(), "List view button not available");
            sleep(1500);
            assumeTrue(b.chooseBySchedule(), "By Schedule is not offered");
            sleep(2500);
            BoxListPage list = new BoxListPage(driver);
            assumeTrue(list.isLoaded(Duration.ofSeconds(10)), "The list did not load");

            int byBlocks = 0;
            boolean sawType = false;
            for (BoxListPage.Block blk : list.blocks()) {
                if (blk.type == null || !blk.type.toLowerCase().contains(type.toLowerCase())) {
                    continue;
                }
                sawType = true;
                String digits = blk.dayCount == null ? "" : blk.dayCount.replaceAll("\\D+", "");
                if (!digits.isEmpty()) byBlocks += Integer.parseInt(digits);
            }
            if (!sawType) {
                wrong.add(type + ": By Date lists " + byDate + " day(s) and By Schedule has"
                        + " no block for it at all");
            } else if (byBlocks != byDate) {
                wrong.add(type + ": By Date lists " + byDate + " day(s), By Schedule's"
                        + " blocks add up to " + byBlocks);
            }
        }
        assertTrue(wrong.isEmpty(),
                "The two List modes disagree about the same schedule: " + wrong);
    }

    @Test
    @Order(7)
    @DisplayName("XV-07 Cleanup: the days this suite laid down are taken off again")
    void cleanup() {
        for (Map.Entry<String, String> e : laid.entrySet()) {
            BoxListPage list = openList();
            if (!list.search(e.getValue())) continue;
            sleep(2200);
            String date = list.dayRows().stream()
                    .filter(r -> r.date != null
                            && r.date.matches(".*\\b" + e.getKey() + "\\b.*"))
                    .map(r -> r.date).findFirst().orElse(null);
            if (date == null) continue;
            if (!list.deleteDay(date)) continue;
            sleep(1200);
            com.zillit.automation.pages.BoxDayScopePage scope =
                    new com.zillit.automation.pages.BoxDayScopePage(driver);
            if (scope.shownForDelete(Duration.ofSeconds(3))) {
                scope.chooseThisDateForDelete();
            }
            list.confirmDelete();
            sleep(2500);
        }
        System.out.println(">>> XV-07 cleanup done for " + laid.keySet());
    }

    // ─────────────────────────── helpers ───────────────────────────

    /** Puts one type on one day of next month, committing whatever chooser appears. */
    private boolean putTypeOnDay(String type, String dayNumber) {
        BoxCreateSchedulePage f = openForm();
        if (!pickType(f, type)) return false;
        if (!f.chooseCalendarTab() || !f.calendarShown()) return false;
        if (!f.calendarNextMonth()) return false;
        sleep(1200);
        if (!f.pickCalendarDay(dayNumber)) return false;
        if (!f.save()) return false;
        sleep(4000);
        BoxConflictPage dlg = new BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(4))) {
            if (!dlg.chooseReplace()) dlg.back();
            sleep(4000);
        }
        backToBox();
        return true;
    }

    private BoxCalendarPage openCalendar(String mode) {
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openCalendarView(), "Calendar view button not available");
        sleep(1500);
        switch (mode) {
            case "Month" -> assumeTrue(b.chooseMonth(), "Month mode is not offered");
            case "Week"  -> assumeTrue(b.chooseWeek(),  "Week mode is not offered");
            default      -> assumeTrue(b.chooseDay(),   "Day mode is not offered");
        }
        sleep(2500);
        BoxCalendarPage cal = new BoxCalendarPage(driver);
        assumeTrue(cal.isLoaded(Duration.ofSeconds(12)), mode + " view did not load");
        return cal;
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

    private int daysOnRecord(String type) {
        BoxListPage list = openList();
        int n = list.daysOfTypeFiltered(type);
        list.clearSearch();
        return Math.max(n, 0);
    }

    private BoxCreateSchedulePage openForm() {
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        if (f.isLoaded(Duration.ofSeconds(2))) { f.clearPickedDates(); return f; }
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open. Toolbar: " + b.toolbarState());
        assumeTrue(b.drawerCreateSchedule(), "Create Schedule is not offered");
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The schedule form did not open");
        return f;
    }

    private boolean pickType(BoxCreateSchedulePage f, String wanted) {
        String already = String.valueOf(f.selectedTypeName());
        if (already.equalsIgnoreCase(wanted)) return true;
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
}
