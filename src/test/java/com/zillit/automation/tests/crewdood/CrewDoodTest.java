package com.zillit.automation.tests.crewdood;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.crewdood.CrewDoodCalendarPage;
import com.zillit.automation.pages.crewdood.CrewDoodCalendarPage.Holiday;
import com.zillit.automation.pages.crewdood.CrewDoodPage;
import com.zillit.automation.pages.crewdood.CrewDoodPage.Day;
import com.zillit.automation.pages.crewdood.CrewDoodPage.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CREW DOOD — the crew's Day Out Of Days (Tools → "Crew DOOD", {@code CrewDoodActivity}).
 * Cases and ids follow {@code CREW_DOOD_TESTCASES.md}.
 *
 * <p>The tool is read-only for crew data: bookings come from deal memos, rows from the crew
 * list. The one thing it writes is the project's <b>Days off &amp; holidays</b> calendar
 * (PUT replaces it whole). So the write cases add holidays named {@code ZL_AUTO_<run>_…}
 * within the next four weeks (inside the date range every crew detail shows) and CD-99
 * removes every {@code ZL_AUTO_} holiday and saves. <b>Weekly days off are never saved</b>:
 * the toggle cases close the editor without saving, and CD-99 puts the weekly days off back
 * to what CD-20 first read if they ever differ.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.CREW_DOOD)
class CrewDoodTest extends CrewDoodBaseTest {

    static final String PREFIX = "ZL_AUTO_";
    /** The deal-memo service's refusal when the project has no deals to export (seen on dev). */
    static final String NO_DEALS = "No matching deal memos found.";
    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    private final String holName = PREFIX + RUN + "_HOL";
    private final String renamed = PREFIX + RUN + "_REN";
    private final String discard = PREFIX + RUN + "_DISCARD";

    private static final Duration SAVE_WAIT = Duration.ofSeconds(25);
    private static final DateTimeFormatter MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final Pattern WEEKDAY = Pattern.compile("MON|TUE|WED|THU|FRI|SAT|SUN");
    private static final Pattern PHASE_LABEL = Pattern.compile("Shoot Day \\d+|Prep W\\d+|Post W\\d+|Rest day");

    /** Weekly days off as CD-20 first read them — what CD-99 restores. */
    private List<String> originalOff;
    /** The holiday date the write cases use: a working day 10–16 days out. */
    private LocalDate holDate;
    private boolean holidaySaved, holidayRenamed;

    private LocalDate holidayDate() {
        if (holDate == null) {
            List<String> off = originalOff == null ? List.of("Saturday", "Sunday") : originalOff;
            holDate = CrewDoodCalendarPage.nextWorkingDay(LocalDate.now().plusDays(10), off);
        }
        return holDate;
    }

    private static List<String> upper(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) out.add(s.toUpperCase(Locale.ROOT));
        return out;
    }

    // ═══════════════════════════ 1. shell ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("CD-01 The Crew DOOD tile opens the tool with search, department filter, Days off & holidays and Export")
    void opens() {
        CrewDoodPage page = openDood();
        assertTrue(activityIs(ACTIVITY), "Not on CrewDoodActivity: " + whereAreWe());
        String title = page.toolbarTitle();
        assertNotNull(title, "No tool title in the toolbar");
        assertEquals(TOOL.toUpperCase(Locale.ROOT), title.toUpperCase(Locale.ROOT), "Toolbar title");
        assertEquals(CrewDoodPage.SEARCH_HINT, page.searchText(), "Empty search box should show its hint");
        assertEquals(CrewDoodPage.ALL_DEPTS, page.deptLabel(), "Department filter label on opening");
        assertTrue(page.textShown(CrewDoodPage.CALENDAR_BUTTON), "No \"Days off & holidays\" button");
        assertTrue(page.textShown(CrewDoodPage.EXPORT), "No \"Export\" button");
        assertTrue(page.calendarButtonEnabled(), "\"Days off & holidays\" is disabled after the data loaded");
        assertTrue(page.exportButtonEnabled(), "\"Export\" is disabled after the data loaded");
    }

    @Test
    @Order(20)
    @DisplayName("CD-02 The list groups people under department headers with \"n/m booked\" counts")
    void listShape() {
        CrewDoodPage page = openDood();
        String state = page.stateMessage();
        assumeTrue(state == null || !state.equals(CrewDoodPage.NO_CREW), "The project has no crew: " + state);
        assertNotEquals(CrewDoodPage.LOAD_FAILED, state, "Crew DOOD failed to load");
        List<Row> rows = page.allRows();
        assertFalse(rows.isEmpty(), "No rows and no message. State box: " + state);
        assertTrue(rows.get(0).header, "The list does not start with a department header: " + rows);
        int i = 0;
        while (i < rows.size()) {
            Row h = rows.get(i);
            assertTrue(h.header, "Expected a header at " + i + ": " + rows);
            var m = CrewDoodPage.BOOKED.matcher(h.text);
            assertTrue(m.matches(), "Header count of " + h.dept + " reads \"" + h.text + "\"");
            int booked = Integer.parseInt(m.group(1)), total = Integer.parseInt(m.group(2));
            int people = 0, bookedSeen = 0;
            i++;
            while (i < rows.size() && !rows.get(i).header) {
                Row p = rows.get(i);
                assertFalse(p.text.isBlank(), "A person row without a name (TBC expected): " + p);
                assertTrue(p.days.equals(CrewDoodPage.NO_DAYS) || CrewDoodPage.DAYS.matcher(p.days).matches(),
                        "Day count of " + p.text + " reads \"" + p.days + "\"");
                if (!p.days.equals(CrewDoodPage.NO_DAYS)) bookedSeen++;
                people++;
                i++;
            }
            assertTrue(people > 0, "Department " + h.dept + " is listed with nobody under it");
            assertEquals(total, people, h.dept + ": header says " + h.text + " but " + people + " people are listed");
            assertEquals(booked, bookedSeen, h.dept + ": header says " + h.text + " but " + bookedSeen
                    + " people show booked days");
            assertTrue(booked <= total, h.dept + ": more booked than members: " + h.text);
        }
    }

    // ═══════════════════════════ 2. search & filter ═══════════════════════════

    @Test
    @Order(30)
    @DisplayName("CD-03 Search matches part of a name, ignoring case")
    void searchByName() {
        CrewDoodPage page = openDood();
        List<Row> people = page.people();
        assumeTrue(!people.isEmpty(), "No crew to search for");
        Row target = people.get(people.size() - 1);
        String n = target.text;
        assumeTrue(n.length() >= 4 && !n.equals(CrewDoodPage.TBC), "Name too short to cut: " + n);
        String q = n.substring(1, Math.min(n.length(), 6)).toLowerCase(Locale.ROOT);
        try {
            page.search(q);
            List<Row> hits = page.people();
            assertTrue(hits.stream().anyMatch(r -> r.text.equals(n)),
                    "Searching \"" + q + "\" (inside \"" + n + "\", lower case) does not list them. Listed: " + hits);
            for (Row r : hits) {
                assertTrue((r.role + " " + r.text).toLowerCase(Locale.ROOT).contains(q),
                        "\"" + r.text + " / " + r.role + "\" is listed for \"" + q + "\" but contains it nowhere");
            }
        } finally {
            page.clearSearch();
        }
    }

    @Test
    @Order(40)
    @DisplayName("CD-04 Search also matches the role (designation)")
    void searchByRole() {
        CrewDoodPage page = openDood();
        Row target = null;
        for (Row r : page.people()) if (r.role.length() >= 5) { target = r; break; }
        assumeTrue(target != null, "Nobody on the project has a role to search for");
        String role = target.role;
        String q = role.substring(2, Math.min(role.length(), 8)).toUpperCase(Locale.ROOT);
        String name = target.text;
        try {
            page.search(q);
            List<Row> hits = page.people();
            assertTrue(hits.stream().anyMatch(r -> r.text.equals(name)),
                    "Searching \"" + q + "\" (inside role \"" + role + "\") does not list " + name + ". Listed: " + hits);
        } finally {
            page.clearSearch();
        }
    }

    @Test
    @Order(50)
    @DisplayName("CD-05 A search with no match says so; clearing it brings everyone back")
    void searchNoMatch() {
        CrewDoodPage page = openDood();
        int before = page.people().size();
        assumeTrue(before > 0, "No crew on the project");
        try {
            page.search("zzqq" + RUN);
            assertEquals(CrewDoodPage.NO_MATCH, page.stateMessage(), "Message for a search with no match");
            assertTrue(page.visibleRows().isEmpty(), "Rows still listed for a search with no match");
            assertFalse(page.retryShown(), "\"Try again\" shown for a no-match search");
        } finally {
            page.clearSearch();
        }
        assertEquals(before, page.people().size(), "Clearing the search did not bring everyone back");
        assertEquals(CrewDoodPage.SEARCH_HINT, page.searchText(), "The search box did not clear");
    }

    @Test
    @Order(60)
    @DisplayName("CD-06 Filters never change a department's \"n/m booked\" count")
    void countsIgnoreFilters() {
        CrewDoodPage page = openDood();
        List<Row> people = page.people();
        assumeTrue(!people.isEmpty(), "No crew on the project");
        java.util.Map<String, String> counts = new java.util.HashMap<>();
        for (Row h : page.headers()) counts.put(h.dept, h.text);
        Row target = people.get(0);
        try {
            page.search(target.text);
            List<Row> headers = page.headers();
            assertFalse(headers.isEmpty(), "Searching an exact name listed nobody: " + target);
            for (Row h : headers) {
                assertEquals(counts.get(h.dept), h.text, "Count of " + h.dept + " changed under a search");
            }
        } finally {
            page.clearSearch();
        }
    }

    @Test
    @Order(70)
    @DisplayName("CD-07 The department sheet lists All departments plus every department, with a check on the current one")
    void departmentSheet() {
        CrewDoodPage page = openDood();
        List<String> depts = new ArrayList<>();
        for (Row h : page.headers()) depts.add(h.dept);
        assumeTrue(!depts.isEmpty(), "No departments on the project");
        assertTrue(page.openDepartmentSheet(), "The department sheet did not open");
        try {
            assertTrue(page.textShown(CrewDoodPage.DEPARTMENT_TITLE), "The sheet has no \"Department\" title");
            List<String> options = page.departmentOptions();
            List<String> expected = new ArrayList<>();
            expected.add(CrewDoodPage.ALL_DEPARTMENTS);
            expected.addAll(depts);
            // List headers are textAllCaps ("WRITER"); the sheet shows the name as typed ("Writer").
            assertEquals(upper(expected), upper(options), "Department options");
            assertEquals(CrewDoodPage.ALL_DEPARTMENTS, page.checkedDepartment(), "Checked option on opening");
        } finally {
            page.closeDepartmentSheet();
        }
        assertEquals(CrewDoodPage.ALL_DEPTS, page.deptLabel(), "Closing the sheet without a pick changed the filter");
    }

    @Test
    @Order(80)
    @DisplayName("CD-08 Picking a department shows only that department; All departments shows everyone again")
    void departmentFilter() {
        CrewDoodPage page = openDood();
        List<Row> headers = page.headers();
        assumeTrue(headers.size() >= 2, "Fewer than two departments: filtering can't narrow anything. " + headers);
        int everyone = page.people().size();
        String dept = headers.get(headers.size() - 1).dept;
        try {
            assertTrue(page.openDepartmentSheet(), "The department sheet did not open");
            assertTrue(page.pickDepartment(dept), "Could not pick " + dept);
            assertTrue(dept.equalsIgnoreCase(page.deptLabel()), "The filter label after picking " + dept
                    + " reads " + page.deptLabel());
            List<Row> shown = page.headers();
            assertEquals(1, shown.size(), "Departments listed with " + dept + " picked: " + shown);
            assertEquals(dept, shown.get(0).dept, "Wrong department listed");
            assertTrue(page.openDepartmentSheet(), "The department sheet did not open again");
            assertTrue(dept.equalsIgnoreCase(String.valueOf(page.checkedDepartment())),
                    "The sheet checks " + page.checkedDepartment() + ", not the picked " + dept);
            assertTrue(page.pickDepartment(CrewDoodPage.ALL_DEPARTMENTS), "Could not pick All departments");
            assertEquals(CrewDoodPage.ALL_DEPTS, page.deptLabel(), "Label after All departments");
            assertEquals(everyone, page.people().size(), "All departments does not list everyone again");
        } finally {
            if (!CrewDoodPage.ALL_DEPTS.equals(page.deptLabel()) && page.openDepartmentSheet()) {
                page.pickDepartment(CrewDoodPage.ALL_DEPARTMENTS);
            }
        }
    }

    @Test
    @Order(90)
    @DisplayName("CD-09 Tapping a department header collapses it; tapping again expands it")
    void collapseDepartment() {
        CrewDoodPage page = openDood();
        List<Row> rows = page.visibleRows();
        assumeTrue(rows.size() >= 2 && rows.get(0).header, "No department with people on screen: " + rows);
        String dept = rows.get(0).dept;
        long under = rows.stream().filter(r -> !r.header && dept.equals(r.dept)).count();
        assumeTrue(under > 0, "Nobody under " + dept);
        boolean collapsed = false;
        try {
            assertTrue(page.tapHeader(dept), "Could not tap the " + dept + " header");
            collapsed = true;
            List<Row> after = page.visibleRows();
            assertTrue(after.stream().anyMatch(r -> r.header && dept.equals(r.dept)),
                    "The collapsed header itself disappeared: " + after);
            assertEquals(0, after.stream().filter(r -> !r.header && dept.equals(r.dept)).count(),
                    "People still listed under the collapsed " + dept + ": " + after);
            assertTrue(page.tapHeader(dept), "Could not tap the collapsed header");
            collapsed = false;
            assertEquals(under, page.visibleRows().stream().filter(r -> !r.header && dept.equals(r.dept)).count(),
                    "Expanding " + dept + " did not bring its people back");
        } finally {
            if (collapsed) page.tapHeader(dept);
        }
    }

    @Test
    @Order(100)
    @DisplayName("CD-10 Pull to refresh reloads and keeps the same list")
    void pullToRefresh() {
        CrewDoodPage page = openDood();
        List<String> before = new ArrayList<>();
        for (Row r : page.allRows()) before.add(r.toString());
        assumeTrue(!before.isEmpty(), "Nothing listed to refresh");
        assertTrue(page.pullToRefresh(), "Could not pull the list");
        assertTrue(page.waitForData(Duration.ofSeconds(20)), "The list did not come back after a refresh");
        List<String> after = new ArrayList<>();
        for (Row r : page.allRows()) after.add(r.toString());
        assertEquals(before, after, "The list changed across a refresh with no data change");
    }

    // ═══════════════════════════ 3. person detail ═══════════════════════════

    @Test
    @Order(110)
    @DisplayName("CD-11 A person opens their day-by-day detail: department, name, role and the badge key")
    void personDetail() {
        CrewDoodPage page = openDood();
        List<Row> people = page.people();
        assumeTrue(!people.isEmpty(), "No crew on the project");
        Row p = people.get(0);
        assertTrue(page.tapPerson(p.text), "Tapping " + p.text + " did not open the detail");
        try {
            assertEquals(p.dept, page.detailDept(), "Department in the detail");
            assertEquals(p.text, page.detailName(), "Name in the detail");
            assertEquals(p.role, page.detailRole(), "Role in the detail");
            List<String> expected = new ArrayList<>();
            for (String[] k : CrewDoodPage.KEY) { expected.add(k[0]); expected.add(k[1]); }
            assertEquals(expected, page.detailKeyTexts(), "The detail's badge key");
        } finally {
            page.back();
        }
        assertFalse(page.overlayOpen(), "BACK did not close the detail");
        assertTrue(activityIs(ACTIVITY), "BACK from the detail left Crew DOOD: " + whereAreWe());
    }

    @Test
    @Order(120)
    @DisplayName("CD-12 The detail opens on today, with month headers \"MMMM yyyy · N working\" and a weekday on every day")
    void detailDays() {
        CrewDoodPage page = openDood();
        List<Row> people = page.people();
        assumeTrue(!people.isEmpty(), "No crew on the project");
        Row p = people.get(0);
        assertTrue(page.tapPerson(p.text), "Could not open " + p.text);
        try {
            List<Day> days = page.detailDaysOnScreen();
            assertFalse(days.isEmpty(), "The detail lists no days");
            LocalDate today = LocalDate.now();
            List<String[]> months = page.detailMonths();
            assertFalse(months.isEmpty(), "No month header on screen");
            for (String[] m : months) {
                try { YearMonth.parse(m[0], MONTH_TITLE); }
                catch (Exception e) { fail("Month header \"" + m[0] + "\" is not \"MMMM yyyy\""); }
                assertTrue(CrewDoodPage.MONTH_WORKING.matcher(m[1]).matches(),
                        "Month " + m[0] + " working count reads \"" + m[1] + "\"");
                if (CrewDoodPage.NO_DAYS.equals(p.days)) {
                    assertEquals("0 working", m[1], p.text + " has no booked days but " + m[0] + " says " + m[1]);
                }
            }
            for (Day d : days) assertTrue(WEEKDAY.matcher(d.weekday).matches(), "Weekday of day " + d);
            // Opens on today when the range covers it — with no bookings the range is this week
            // to four weeks out, so today is always in it.
            String todayNum = String.valueOf(today.getDayOfMonth());
            String todayWd = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase(Locale.ROOT);
            assertTrue(days.stream().anyMatch(d -> d.number.equals(todayNum) && d.weekday.equals(todayWd)),
                    "The detail did not open on today (" + today + "). On screen: " + days);
        } finally {
            page.back();
        }
    }

    @Test
    @Order(130)
    @DisplayName("CD-13 Phase labels follow production dates: Shoot Day / Prep W / Post W / Rest day, blank without them")
    void phaseLabels() {
        CrewDoodPage page = openDood();
        boolean band = page.phaseBandShown();
        List<Row> people = page.people();
        assumeTrue(!people.isEmpty(), "No crew on the project");
        assertTrue(page.tapPerson(people.get(0).text), "Could not open the detail");
        try {
            for (Day d : page.detailDaysOnScreen()) {
                if (d.label.startsWith(PREFIX)) continue;   // our own holiday
                if (band) {
                    assertTrue(d.label.isEmpty() || PHASE_LABEL.matcher(d.label).matches() || "HOL".equals(d.label),
                            "Unexpected day label with production dates: " + d);
                } else {
                    assertTrue(d.label.isEmpty() || "HOL".equals(d.label),
                            "No production dates (no phase band) but a day carries a phase label: " + d);
                }
            }
        } finally {
            page.back();
        }
    }

    @Test
    @Order(140)
    @DisplayName("CD-14 A booked person's bar and day count match the detail (needs deal memos)")
    void bookedPerson() {
        CrewDoodPage page = openDood();
        Row booked = null;
        for (Row r : page.people()) if (!CrewDoodPage.NO_DAYS.equals(r.days)) { booked = r; break; }
        assumeTrue(booked != null, "Nobody on the project is booked: bookings come from deal memos, and this"
                + " project has none with start/end dates. Not automatable here without creating a deal memo.");
        var m = CrewDoodPage.DAYS.matcher(booked.days);
        assertTrue(m.matches(), "Day count reads " + booked.days);
        int total = Integer.parseInt(m.group(1));
        assertTrue(page.tapPerson(booked.text), "Could not open " + booked.text);
        try {
            java.util.Map<String, Integer> working = new java.util.LinkedHashMap<>();
            for (int i = 0; i < 40; i++) {
                for (String[] mo : page.detailMonths()) {
                    var w = CrewDoodPage.MONTH_WORKING.matcher(mo[1]);
                    if (w.matches()) working.putIfAbsent(mo[0], Integer.parseInt(w.group(1)));
                }
                if (!page.scrollDetail(true)) break;
            }
            int sum = working.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(total, sum, booked.text + ": list says " + booked.days + ", months say " + working);
        } finally {
            page.back();
        }
    }

    // ═══════════════════════════ 4. days off & holidays ═══════════════════════════

    private CrewDoodCalendarPage openCalendar() {
        CrewDoodPage page = openDood();
        assertTrue(page.tapCalendar(), "Could not tap \"Days off & holidays\"");
        CrewDoodCalendarPage cal = new CrewDoodCalendarPage(driver);
        assertTrue(cal.isOpen(Duration.ofSeconds(8)), "The Days off & holidays editor did not open");
        return cal;
    }

    private void closeCalendar(CrewDoodCalendarPage cal) {
        if (cal.isOpen()) {
            if (!cal.tapClose()) cal.back();
        }
    }

    @Test
    @Order(200)
    @DisplayName("CD-20 The editor opens with its copy, Save off, and a week summary that matches the toggles")
    void calendarOpens() {
        CrewDoodCalendarPage cal = openCalendar();
        try {
            assertTrue(cal.showsText(CrewDoodCalendarPage.TITLE), "No \"Days off & holidays\" title");
            assertTrue(cal.showsText(CrewDoodCalendarPage.HINT), "The one-calendar hint is missing");
            assertTrue(cal.saveShown(), "No Save button");
            assertFalse(cal.saveEnabled(), "Save is enabled before anything changed");
            List<String> off = cal.weeklyOff();
            if (originalOff == null) originalOff = off;
            assertTrue(off.size() < 7, "Every day is a weekly day off: " + off);
            assertEquals(CrewDoodCalendarPage.expectedSummary(off), cal.weekSummary(), "Week summary for " + off);
            assertEquals(CrewDoodCalendarPage.WEEKLY_OFF_HELP, cal.weekHelp(), "Weekly off help text");
            assertEquals(YearMonth.now(), cal.shownMonth(), "The grid does not open on this month");
            List<Holiday> hols = cal.holidays();
            if (hols.isEmpty()) {
                assertTrue(cal.holidayEmptyShown(), "No holidays and no \"" + CrewDoodCalendarPage.NO_HOLIDAYS + "\"");
            } else {
                assertEquals(hols.size() + " added", cal.holidayCount(), "Holiday count for " + hols);
            }
        } finally {
            closeCalendar(cal);
        }
    }

    @Test
    @Order(210)
    @DisplayName("CD-21 Toggling a weekday updates the summary and turns Save on; toggling it back turns Save off")
    void toggleWeekday() {
        CrewDoodCalendarPage cal = openCalendar();
        try {
            List<String> off = cal.weeklyOff();
            String day = null;
            for (String d : CrewDoodCalendarPage.WEEK) if (!off.contains(d)) { day = d; break; }
            assertNotNull(day, "No working day to toggle");
            assertTrue(cal.toggleWeekday(day), "Could not tap " + day);
            List<String> now = cal.weeklyOff();
            assertTrue(now.contains(day), day + " is not marked off after a tap: " + now);
            assertEquals(CrewDoodCalendarPage.expectedSummary(now), cal.weekSummary(), "Summary after marking " + day);
            assertTrue(cal.saveEnabled(), "Save stays off after a weekly day off changed");
            assertTrue(cal.toggleWeekday(day), "Could not tap " + day + " again");
            assertEquals(off, cal.weeklyOff(), "Tapping " + day + " twice did not restore the week");
            assertFalse(cal.saveEnabled(), "Save stays on although the week is back to what was loaded");
        } finally {
            closeCalendar(cal);   // never saved
        }
    }

    @Test
    @Order(220)
    @DisplayName("CD-22 With all seven days off the editor asks for a working day and Save stays off")
    void allDaysOff() {
        CrewDoodCalendarPage cal = openCalendar();
        List<String> toggled = new ArrayList<>();
        try {
            List<String> off = cal.weeklyOff();
            for (String d : CrewDoodCalendarPage.WEEK) {
                if (!off.contains(d)) { assertTrue(cal.toggleWeekday(d), "Could not tap " + d); toggled.add(d); }
            }
            assertEquals(7, cal.weeklyOff().size(), "Not every day is off: " + cal.weeklyOff());
            assertEquals(CrewDoodCalendarPage.NEED_WORKING_DAY, cal.weekHelp(), "Help text with no working day");
            assertEquals("0-day week", cal.weekSummary(), "Summary with every day off");
            assertFalse(cal.saveEnabled(), "Save is on with no working day in the week");
            String back = toggled.get(0);
            assertTrue(cal.toggleWeekday(back), "Could not tap " + back);
            toggled.remove(0);
            assertEquals(CrewDoodCalendarPage.WEEKLY_OFF_HELP, cal.weekHelp(), "Help text once a working day is back");
        } finally {
            closeCalendar(cal);   // never saved
        }
    }

    @Test
    @Order(230)
    @DisplayName("CD-23 Tapping one day starts a selection; a second (earlier) day makes a range; a third starts over; Clear drops it")
    void selectRange() {
        CrewDoodCalendarPage cal = openCalendar();
        try {
            LocalDate a = LocalDate.now().plusDays(60);
            LocalDate b = a.minusDays(3);
            assertTrue(cal.tapDay(a), "Could not tap " + a);
            assertTrue(cal.draftShown(), "No draft panel after tapping a day");
            assertEquals(CrewDoodCalendarPage.rangeLabel(a, a), cal.draftRange(), "Draft label for one day");
            assertEquals("1 day · " + CrewDoodCalendarPage.PICK_END, cal.draftCount(), "Draft count for one day");
            assertTrue(cal.tapDay(b), "Could not tap " + b);
            assertEquals(CrewDoodCalendarPage.rangeLabel(b, a), cal.draftRange(), "Draft label for a backward range");
            assertEquals("4 days", cal.draftCount(), "Draft count for a 4-day range");
            LocalDate c = a.plusDays(1);
            assertTrue(cal.tapDay(c), "Could not tap " + c);
            assertEquals(CrewDoodCalendarPage.rangeLabel(c, c), cal.draftRange(), "A tap after a range should start over");
            assertTrue(cal.tapClear(), "Could not tap Clear");
            sleep(600);
            assertFalse(cal.draftShown(), "The draft panel stays up after Clear");
            assertFalse(cal.saveEnabled(), "Save turned on by a selection alone");
        } finally {
            closeCalendar(cal);
        }
    }

    @Test
    @Order(240)
    @DisplayName("CD-24 A named holiday is added to the list and saved; the editor closes and the holiday is there on reopening")
    void addAndSaveHoliday() {
        CrewDoodCalendarPage cal = openCalendar();
        LocalDate d;
        try {
            if (originalOff == null) originalOff = cal.weeklyOff();
            d = holidayDate();
            int before = cal.holidays().size();
            assertTrue(cal.tapDay(d), "Could not tap " + d);
            cal.typeDraftName(holName);
            assertTrue(cal.tapAdd(), "Could not tap Add holiday");
            sleep(600);
            assertFalse(cal.draftShown(), "The draft panel stays up after Add holiday");
            List<Holiday> hols = cal.holidays();
            Holiday mine = hols.stream().filter(h -> h.name.equals(holName)).findFirst().orElse(null);
            assertNotNull(mine, "The new holiday is not listed: " + hols);
            assertEquals(CrewDoodCalendarPage.rangeLabel(d, d) + " · 1 day", mine.meta, "Holiday row meta");
            assertEquals(d.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT),
                    mine.tileMonth, "Date tile month");
            assertEquals(String.valueOf(d.getDayOfMonth()), mine.tileDay, "Date tile day");
            assertEquals((before + 1) + " added", cal.holidayCount(), "Holiday count after adding");
            assertTrue(cal.saveEnabled(), "Save stays off after adding a holiday");
            assertTrue(cal.tapSave(), "Could not tap Save");
            String toast = new CrewDoodPage(driver).toastText(Duration.ofSeconds(6));
            boolean closed = cal.waitClosed(SAVE_WAIT);
            assertTrue(closed, "The editor did not close after Save. Toast: " + toast);
            holidaySaved = true;
            if (toast != null) {
                assertNotEquals(CrewDoodCalendarPage.SAVE_FAILED, toast, "Save reported failure");
            }
        } finally {
            closeCalendar(cal);
        }
        CrewDoodCalendarPage again = openCalendar();
        try {
            List<Holiday> hols = again.holidays();
            assertTrue(hols.stream().anyMatch(h -> h.name.equals(holName)),
                    "The saved holiday is gone on reopening: " + hols);
            assertFalse(again.saveEnabled(), "Save is on right after reopening");
        } finally {
            closeCalendar(again);
        }
    }

    @Test
    @Order(250)
    @DisplayName("CD-25 A saved holiday shows in a person's detail under its name")
    void holidayInDetail() {
        assumeTrue(holidaySaved, "CD-24 did not save a holiday");
        CrewDoodPage page = openDood();
        List<Row> people = page.people();
        assumeTrue(!people.isEmpty(), "No crew on the project");
        assertTrue(page.tapPerson(people.get(0).text), "Could not open the detail");
        LocalDate d = holidayDate();
        String num = String.valueOf(d.getDayOfMonth());
        String wd = d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
        Day found = null;
        List<Day> seen = new ArrayList<>();
        try {
            for (int i = 0; i < 8 && found == null; i++) {
                for (Day day : page.detailDaysOnScreen()) {
                    if (seen.stream().noneMatch(x -> x.toString().equals(day.toString()))) seen.add(day);
                    if (day.number.equals(num) && day.weekday.equals(wd)) { found = day; break; }
                }
                if (found == null) page.scrollDetail(true);
            }
            assertNotNull(found, d + " never came up in the detail. Seen: " + seen);
            assertEquals(holName, found.label, "Label of the holiday's day in the detail");
        } finally {
            page.back();
        }
    }

    @Test
    @Order(260)
    @DisplayName("CD-26 Adding a day that is already a holiday is refused")
    void rangeAlreadyHoliday() {
        assumeTrue(holidaySaved, "CD-24 did not save a holiday");
        CrewDoodCalendarPage cal = openCalendar();
        try {
            LocalDate d = holidayDate();
            assertTrue(cal.tapDay(d), "Could not tap " + d);
            assertTrue(cal.tapAdd(), "Could not tap Add holiday");
            assertEquals(CrewDoodCalendarPage.RANGE_EXISTS, cal.draftError(), "Error for a day that is already a holiday");
            assertTrue(cal.draftShown(), "The draft closed although nothing was added");
            assertFalse(cal.saveEnabled(), "Save turned on although nothing was added");
            cal.tapClear();
        } finally {
            closeCalendar(cal);
        }
    }

    @Test
    @Order(270)
    @DisplayName("CD-27 A range longer than 366 days is refused")
    void rangeTooLong() {
        CrewDoodCalendarPage cal = openCalendar();
        try {
            LocalDate a = LocalDate.now().plusDays(30);
            LocalDate b = a.plusDays(400);
            assertTrue(cal.tapDay(a), "Could not tap " + a);
            assertTrue(cal.tapDay(b), "Could not tap " + b);
            assertEquals("401 days", cal.draftCount(), "Draft count for a 401-day range");
            assertTrue(cal.tapAdd(), "Could not tap Add holiday");
            assertEquals(CrewDoodCalendarPage.RANGE_TOO_LONG, cal.draftError(), "Error for a 401-day range");
            assertFalse(cal.saveEnabled(), "Save turned on although nothing was added");
            cal.tapClear();
        } finally {
            closeCalendar(cal);
        }
    }

    @Test
    @Order(280)
    @DisplayName("CD-28 A range is listed as one row; closing without Save discards it")
    void rangeRowAndDiscard() {
        CrewDoodCalendarPage cal = openCalendar();
        LocalDate a = LocalDate.now().plusDays(45);
        LocalDate b = a.plusDays(2);
        try {
            int before = cal.holidays().size();
            assertTrue(cal.selectRange(a, b), "Could not select " + a + " – " + b);
            cal.typeDraftName(discard);
            assertTrue(cal.tapAdd(), "Could not tap Add holiday");
            sleep(600);
            List<Holiday> hols = cal.holidays();
            List<Holiday> mine = hols.stream().filter(h -> h.name.equals(discard)).toList();
            assertEquals(1, mine.size(), "A 3-day range should be one row: " + hols);
            assertEquals(CrewDoodCalendarPage.rangeLabel(a, b) + " · 3 days", mine.get(0).meta, "Range row meta");
            if (a.getMonth() == b.getMonth()) {
                assertEquals(a.getDayOfMonth() + "–" + b.getDayOfMonth(), mine.get(0).tileDay, "Range date tile");
            }
            assertEquals((before + 1) + " added", cal.holidayCount(), "Count after adding a range");
            assertTrue(cal.saveEnabled(), "Save off after adding a range");
        } finally {
            closeCalendar(cal);   // close WITHOUT saving
        }
        CrewDoodCalendarPage again = openCalendar();
        try {
            List<Holiday> hols = again.holidays();
            assertTrue(hols.stream().noneMatch(h -> h.name.equals(discard)),
                    "A holiday added and closed without Save is still there: " + hols);
        } finally {
            closeCalendar(again);
        }
    }

    @Test
    @Order(290)
    @DisplayName("CD-29 Renaming a holiday turns Save on and the new name is kept after Save")
    void renameHoliday() {
        assumeTrue(holidaySaved, "CD-24 did not save a holiday");
        CrewDoodCalendarPage cal = openCalendar();
        try {
            assertTrue(cal.renameHoliday(holName, renamed), "Could not find " + holName + " to rename");
            assertTrue(cal.saveEnabled(), "Save stays off after a rename");
            assertTrue(cal.tapSave(), "Could not tap Save");
            String toast = new CrewDoodPage(driver).toastText(Duration.ofSeconds(6));
            assertTrue(cal.waitClosed(SAVE_WAIT), "The editor did not close after Save. Toast: " + toast);
            holidayRenamed = true;
        } finally {
            closeCalendar(cal);
        }
        CrewDoodCalendarPage again = openCalendar();
        try {
            List<Holiday> hols = again.holidays();
            assertTrue(hols.stream().anyMatch(h -> h.name.equals(renamed)), "Renamed holiday missing: " + hols);
            assertTrue(hols.stream().noneMatch(h -> h.name.equals(holName)), "Old name still listed: " + hols);
        } finally {
            closeCalendar(again);
        }
    }

    // ═══════════════════════════ 5. export ═══════════════════════════

    @Test
    @Order(300)
    @DisplayName("CD-30 Export builds the workbook and opens it (or saves it to Downloads)")
    void export() {
        CrewDoodPage page = openDood();
        boolean nobodyBooked = page.people().stream().allMatch(r -> CrewDoodPage.NO_DAYS.equals(r.days));
        assertTrue(page.exportButtonEnabled(), "Export is disabled");
        assertTrue(page.tapExport(), "Could not tap Export");
        String toast = null, otherPkg = null, otherActivity = null;
        long end = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < end) {
            try {
                String pkg = String.valueOf(driver.getCurrentPackage());
                if (!appPackage.equals(pkg)) { otherPkg = pkg; break; }
                if (!activityIs(ACTIVITY)) { otherActivity = String.valueOf(driver.currentActivity()); break; }
            } catch (Exception ignored) { }
            toast = page.toastText(Duration.ofMillis(800));
            if (toast != null) break;
        }
        System.out.println("[CD-30] export → pkg=" + otherPkg + " activity=" + otherActivity + " toast=" + toast);
        if (otherPkg != null || otherActivity != null) screenshot("CD-30_export_viewer");
        bringAppToFront();
        if (!activityIs(ACTIVITY)) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        assertNotEquals(CrewDoodPage.EXPORT_FAILED, toast, "Export failed with the app's own message");
        // The server refuses a project with no deal memos with its own text (web shows the same
        // message: "shows the server message when the export is refused"). Not a defect here.
        if (toast != null && toast.equals(NO_DEALS) && nobodyBooked) {
            assumeTrue(false, "Nothing to export: the server answered \"" + toast + "\" and nobody on the"
                    + " project is booked. Needs deal memos to check the workbook itself.");
        }
        assertTrue(otherPkg != null || otherActivity != null || toast != null,
                "Export did nothing visible within 40s: no viewer, no chooser, no toast");
        if (toast != null && otherPkg == null && otherActivity == null) {
            // Not the generic failure: either the file name (saved to Downloads), the skipped-deals
            // note, or the server's own refusal text.
            assertTrue(toast.endsWith(".xlsx") || toast.contains("left out of the export"),
                    "Export answered with a message instead of the workbook: \"" + toast + "\"");
        }
    }

    // ═══════════════════════════ 6. not automatable here ═══════════════════════════

    @Test
    @Order(400)
    @DisplayName("CD-40 Losing view access while inside closes the tool with \"You no longer have access to Crew DOOD.\"")
    void accessLost() {
        assumeTrue(false, "Needs a second admin to take this user's Crew DOOD view right away while the"
                + " tool is open; the second device (emulator) is reserved for other modules today.");
    }

    @Test
    @Order(410)
    @DisplayName("CD-41 A failed load shows the error and \"Try again\" reloads")
    void loadFailure() {
        assumeTrue(false, "Needs the crew-list / deals / calendar reads to fail (network fault injection);"
                + " the suite does not cut the device's network.");
    }

    // ═══════════════════════════ 99. cleanup ═══════════════════════════

    @Test
    @Order(990)
    @DisplayName("CD-99 Cleanup: remove every ZL_AUTO_ holiday and restore the weekly days off")
    void cleanup() {
        CrewDoodCalendarPage cal = openCalendar();
        int removed;
        boolean weekChanged = false;
        try {
            removed = cal.removeHolidaysStartingWith(PREFIX);
            if (originalOff != null) {
                List<String> now = cal.weeklyOff();
                for (String d : CrewDoodCalendarPage.WEEK) {
                    if (now.contains(d) != originalOff.contains(d)) { cal.toggleWeekday(d); weekChanged = true; }
                }
            }
            if (removed == 0 && !weekChanged) return;
            assertTrue(cal.saveEnabled(), "Save is off after removing " + removed + " holiday(s)");
            assertTrue(cal.tapSave(), "Could not tap Save");
            String toast = new CrewDoodPage(driver).toastText(Duration.ofSeconds(6));
            assertTrue(cal.waitClosed(SAVE_WAIT), "The editor did not close after the cleanup Save. Toast: " + toast);
        } finally {
            closeCalendar(cal);
        }
        CrewDoodCalendarPage again = openCalendar();
        try {
            List<Holiday> left = again.holidays().stream().filter(h -> h.name.startsWith(PREFIX)).toList();
            assertTrue(left.isEmpty(), "ZL_AUTO_ holidays left after cleanup: " + left);
        } finally {
            closeCalendar(again);
        }
    }
}
