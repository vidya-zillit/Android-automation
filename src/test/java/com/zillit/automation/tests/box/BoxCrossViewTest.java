package com.zillit.automation.tests.box;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.box.BoxCalendarPage;
import com.zillit.automation.pages.box.BoxConflictPage;
import com.zillit.automation.pages.box.BoxCreateEventPage;
import com.zillit.automation.pages.box.BoxCreateSchedulePage;
import com.zillit.automation.pages.box.BoxHistoryPage;
import com.zillit.automation.pages.box.BoxListPage;
import com.zillit.automation.pages.box.BoxSchedulePage;
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
            com.zillit.automation.pages.box.BoxDayScopePage scope =
                    new com.zillit.automation.pages.box.BoxDayScopePage(driver);
            if (scope.shownForDelete(Duration.ofSeconds(3))) {
                scope.chooseThisDate();
            }
            list.confirmDelete();
            sleep(2500);
        }
        System.out.println(">>> XV-07 cleanup done for " + laid.keySet());
    }

    // ─────────────────────── web gap cases: search and History ───────────────────────

    /**
     * FL-10 [WEB BOX-01]. The Calendar and the List share one search text
     * ({@code BoxScheduleViewModel.filterSearchText}) but filter differently: the Calendar
     * matches events on their own title (CalendarFragment), the List matches a DAY only on its
     * title, type or dates ({@code ListFragment.filterDays}) — so a day whose only match is an
     * event title is dropped from the List. Expected to FAIL today on that last check.
     */
    @Test
    @Order(8)
    @DisplayName("FL-10 [WEB BOX-01] Search gives the same answer in Calendar and List")
    void searchAgreesAcrossViews() {
        String evt = fixtureName("evt-search");
        String query = evt.substring(FIXTURE_PREFIX.length() + 1);   // "evt-search NNNNN"
        String day = createEvent(evt);
        assumeTrue(day != null, "The fixture event could not be created (see the >>> line for the step)");
        try {
            // 1. A query that matches nothing: both views empty.
            BoxCalendarPage cal = openCalendar("Month");
            cal.today();
            sleep(1500);
            assumeTrue(cal.search("zzqqnomatch"), "The calendar has no search box");
            sleep(2500);
            List<String> drawnOnNoMatch = cal.everythingDrawn();
            BoxListPage list = switchToList();
            String carried = String.valueOf(list.searchText()).trim();
            boolean listEmpty = list.isEmpty();
            String emptyText = list.emptyStateText();
            System.out.println(">>> FL-10 no-match: calendar draws " + drawnOnNoMatch + "; list box=\"" + carried
                    + "\" empty=" + listEmpty + " text=\"" + emptyText + "\"");

            // 2. The event's own title: Calendar draws it on its day; the List must list it too.
            assumeTrue(list.search(query), "The list has no search box");
            sleep(2500);
            BoxListPage.EventRow inList = list.findEvent(evt);
            List<String> listRows = list.eventRows().stream().map(Object::toString).toList();
            boolean listEmptyOnMatch = list.isEmpty();
            BoxCalendarPage cal2 = switchToCalendar();
            cal2.today();
            sleep(2000);
            String calBox = String.valueOf(cal2.searchText()).trim();
            List<String> onDay = cal2.titlesOn(day);
            System.out.println(">>> FL-10 match \"" + query + "\": calendar day " + day + " shows " + onDay
                    + "; list row=" + inList + " rows=" + listRows + " empty=" + listEmptyOnMatch);

            org.junit.jupiter.api.Assertions.assertAll(
                    () -> assertTrue(drawnOnNoMatch.isEmpty(),
                            "A search that matches nothing still draws " + drawnOnNoMatch + " in the calendar"),
                    () -> assertEquals("zzqqnomatch", carried,
                            "The search typed in the Calendar did not carry to the List (the text is shared)"),
                    () -> assertTrue(listEmpty, "A search that matches nothing still lists rows in the List"),
                    () -> assertTrue(emptyText.contains("No Results") || emptyText.contains("No results"),
                            "[BOX-01] The List's empty state for a search with no match reads \"" + emptyText
                                    + "\" — it should say No Results, not that the project has no schedule"),
                    () -> assertEquals(query, calBox, "The search typed in the List did not carry to the Calendar"),
                    () -> assertTrue(onDay.stream().anyMatch(t -> t.contains("evt-search")),
                            "The Calendar does not draw \"" + evt + "\" on day " + day + " for its own title: " + onDay),
                    () -> assertTrue(inList != null,
                            "[BOX-01] Searching \"" + query + "\" draws the event in the Calendar but the List drops"
                                    + " it (ListFragment.filterDays matches a day on its title/type/dates only, never"
                                    + " on its events). List rows: " + listRows + (listEmptyOnMatch ? " (empty state)" : "")));
        } finally {
            deleteEvent(evt);
        }
    }

    /**
     * HS-09 [WEB BOX-03]. History's card text ({@code tvDetails}) comes from the server; it
     * must name the schedule's own first and last date, not the day before (a timezone shift).
     */
    @Test
    @Order(9)
    @DisplayName("HS-09 [WEB BOX-03] History shows a new schedule's own dates, not a day early")
    void historyShowsScheduleDates() {
        List<String> days = List.of("10", "11", "12");
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, PREP), "This project has no \"" + PREP + "\" type. Types: " + f.typeNames());
        assumeTrue(f.chooseCalendarTab() && f.calendarShown(), "The Calendar tab is not offered");
        java.time.YearMonth now = java.time.YearMonth.now();
        java.time.YearMonth shown = shownMonth(f.calendarMonth());
        for (int i = 0; i < 3 && (shown == null || !shown.isAfter(now)); i++) {
            f.calendarNextMonth();
            sleep(1500);
            shown = shownMonth(f.calendarMonth());
        }
        System.out.println(">>> HS-09 form calendar shows \"" + f.calendarMonth() + "\"");
        assumeTrue(shown != null && shown.isAfter(now), "The form's calendar could not be moved past "
                + now + " (it shows \"" + f.calendarMonth() + "\")");
        java.time.LocalDate first = shown.atDay(10);
        int picked = 0;
        for (String d : days) if (f.pickCalendarDay(d)) picked++;
        assumeTrue(picked == 3, "Only " + picked + " of the three days could be picked");
        assumeTrue(f.save(), "Save was not available");
        sleep(4000);
        BoxConflictPage dlg = new BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(4))) { if (!dlg.chooseReplace()) dlg.back(); sleep(4000); }
        hsDays.addAll(days);
        System.out.println(">>> HS-09 device time: " + safeDeviceTime() + "; schedule " + first + " .. " + first.plusDays(2));
        try {
            // The ADDED card carries only the day count ("3 day(s)"); the range is printed on
            // the REMOVED / CHANGED cards' titles ("Prep · Dec 8 – Dec 9"). So remove the
            // schedule first, then read both.
            removeDays(PREP, hsDays, first.getMonth());
            hsDays.clear();
            backToBox();
            BoxSchedulePage b = box();
            assumeTrue(b.openDrawer() && b.drawerHistory(), "History is not offered in the drawer");
            BoxHistoryPage h = new BoxHistoryPage(driver);
            assumeTrue(h.isLoaded(Duration.ofSeconds(20)), "History did not load");
            sleep(1500);
            // The log is not newest-first (it opened on Nov 21 rows), so read it to the end and
            // pick today's Schedule cards for Prep (ADDED, then REMOVED). The card title carries the
            // range itself: “Prep · Dec 8 – Dec 9”.
            List<BoxHistoryPage.Card> cards = h.allCards(40);
            if (cards.isEmpty()) { sleep(5000); cards = h.allCards(40); }
            String screen = cards.isEmpty() ? h.describe() : "";
            String today = java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH));
            List<BoxHistoryPage.Card> mine = cards.stream()
                    .filter(c -> "Schedule".equalsIgnoreCase(String.valueOf(c.target))
                            && String.valueOf(c.title).contains(PREP)
                            && String.valueOf(c.when).startsWith(today))
                    .toList();
            System.out.println(">>> HS-09 " + cards.size() + " History cards; today's Prep schedule cards: " + mine
                    + (screen.isEmpty() ? "" : " — on screen: " + screen));
            assertTrue(!mine.isEmpty(), "History lists no Prep schedule card dated " + today
                    + " after one was saved. Cards read: " + cards.size() + (screen.isEmpty() ? "" : " — on screen: " + screen));
            java.time.format.DateTimeFormatter mmmD = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH);
            java.time.format.DateTimeFormatter dMmm = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH);
            java.util.function.BiPredicate<String, java.time.LocalDate> names = (t, d) -> {
                String x = String.valueOf(t).replace('\u00a0', ' ');
                return x.matches("(?s).*\\b" + java.util.regex.Pattern.quote(d.format(mmmD)) + "\\b.*")
                        || x.matches("(?s).*\\b" + java.util.regex.Pattern.quote(d.format(dMmm)) + "\\b.*");
            };
            BoxHistoryPage.Card card = mine.stream()
                    .filter(c -> names.test(c.title + " " + c.details, first) && names.test(c.title + " " + c.details, first.plusDays(2)))
                    .findFirst().orElse(null);
            List<BoxHistoryPage.Card> shifted = mine.stream()
                    .filter(c -> names.test(c.title + " " + c.details, first.minusDays(1))
                            && names.test(c.title + " " + c.details, first.plusDays(1)))
                    .toList();
            String expect = first.format(mmmD) + " – " + first.plusDays(2).format(mmmD);
            assertTrue(card != null, "[BOX-03] No History card for today's Prep schedule names its own dates " + expect
                    + (shifted.isEmpty() ? "" : " — shifted a day early: " + shifted) + ". Today's Prep cards: " + mine);
            // "N day(s)" on today's ADDED Prep card(s): ours must say 3.
            List<String> counts = mine.stream().filter(c -> "ADDED".equalsIgnoreCase(String.valueOf(c.action)))
                    .map(c -> String.valueOf(c.details)).toList();
            assertTrue(counts.stream().anyMatch(d -> d.matches("(?s).*\\b3 day\\(?s?\\)?.*")),
                    "[BOX-03] No ADDED Prep card today counts the 3 days that were saved: " + counts);
        } finally {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            if (!hsDays.isEmpty()) removeDays(PREP, hsDays, first.getMonth());
        }
    }

    private final List<String> hsDays = new ArrayList<>();

    /** "December 2026" (or "Dec 2026") → that month; null when unreadable. */
    private static java.time.YearMonth shownMonth(String label) {
        if (label == null) return null;
        for (String pat : List.of("MMMM yyyy", "MMM yyyy")) {
            try {
                return java.time.YearMonth.parse(label.trim(), new java.time.format.DateTimeFormatterBuilder()
                        .parseCaseInsensitive().appendPattern(pat).toFormatter(java.util.Locale.ENGLISH));
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Creates a full-day event to Only Me; returns its day of month, or null. */
    private String createEvent(String title) {
        backToBox();
        BoxSchedulePage b = box();
        if (!b.openDrawer() || !b.drawerCreateEvent()) return fixtureFail("drawer / Create Event: " + b.toolbarState());
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        if (!f.isLoaded(Duration.ofSeconds(15))) return fixtureFail("the event form did not open");
        if (!(f.onEventTab() || f.chooseEventTab())) return fixtureFail("no event tab");
        if (!f.setTitle(title)) return fixtureFail("title could not be typed");
        f.toggleFullDay();
        sleep(800);
        String start = String.valueOf(f.startDate());
        if (!f.chooseMeetInPerson()) return fixtureFail("Call Type could not be set to Meet in Person");
        if (!f.distributeToSelf()) return fixtureFail("Distribute To could not be set to Only Me (now \"" + f.distributeTo() + "\")");
        if (!f.save()) return fixtureFail("Save not available");
        sleep(800);
        String toast = f.toastText();
        sleep(2200);
        if (f.calendarPromptShown(Duration.ofSeconds(6))) f.answerCalendarPrompt(false);
        sleep(3000);
        if (f.stillOpen()) return fixtureFail("the form stayed open after Save (toast: " + toast + ")");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<!\\d)(\\d{1,2})(?!\\d)").matcher(start);
        String day = m.find() ? String.valueOf(Integer.parseInt(m.group(1))) : String.valueOf(java.time.LocalDate.now().getDayOfMonth());
        java.util.regex.Matcher md = java.util.regex.Pattern.compile("([A-Z][a-z]{2}) (\\d{1,2})").matcher(start);
        eventShortDate = md.find() ? md.group(1) + " " + md.group(2) : null;
        System.out.println(">>> fixture event \"" + title + "\" on \"" + start + "\" (day " + day + ")");
        return day;
    }

    private String fixtureFail(String why) {
        System.out.println(">>> fixture event not created: " + why);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1000);
        new BoxSchedulePage(driver).tapText("Discard");
        return null;
    }

    /** Day label of the last fixture event ("Nov 24"), for finding it again without its title. */
    private String eventShortDate;

    /**
     * Deletes every {@code ZL AUTO evt-search} event (this one and any a broken run left).
     * The List search cannot find an event by its title — that is BOX-01 itself — so it
     * searches the event's date ("Nov 24", which the List does match on), then falls back
     * to the unfiltered list.
     */
    private void deleteEvent(String title) {
        String prefix = FIXTURE_PREFIX + " evt-search";
        try {
            BoxListPage list = openList();
            int removed = 0;
            for (String q : new String[]{title, eventShortDate, ""}) {
                if (q == null) continue;
                for (int guard = 0; guard < 5; guard++) {
                    list.search(q);
                    sleep(2200);
                    BoxListPage.EventRow r = q.isEmpty() ? null : list.findEvent(prefix);
                    if (r == null && q.isEmpty()) { list.scrollToTop(); r = list.findEventScrolling(prefix, 25); }
                    if (r == null || !list.deleteEvent(r.title)) break;
                    sleep(1200);
                    list.confirmDelete();
                    sleep(2500);
                    removed++;
                    list = openList();
                }
            }
            System.out.println(">>> cleanup: removed " + removed + " \"" + prefix + "\" event(s)");
            list.clearSearch();
            switchToCalendar().search("");
        } catch (Exception | AssertionError e) {
            System.out.println(">>> cleanup of \"" + title + "\" failed: " + e);
        }
    }

    /** Removes the given days of {@code type} in {@code month} (whole schedule when asked). */
    private void removeDays(String type, List<String> dayNumbers, java.time.Month month) {
        String mon = month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
        for (String d : dayNumbers) {
            try {
                BoxListPage list = openList();
                if (!list.search(type)) continue;
                sleep(2200);
                List<String> onDay = list.dayRows().stream()
                        .filter(r -> r.date != null && r.date.matches(".*\\b" + d + "\\b.*"))
                        .map(r -> r.date).toList();
                String date = onDay.stream().filter(x -> x.contains(mon)).findFirst()
                        .orElse(onDay.isEmpty() ? null : onDay.get(0));
                if (date == null || !list.deleteDay(date)) continue;
                sleep(1200);
                com.zillit.automation.pages.box.BoxDayScopePage scope =
                        new com.zillit.automation.pages.box.BoxDayScopePage(driver);
                if (scope.shownForDelete(Duration.ofSeconds(3))) scope.chooseComplete();
                list.confirmDelete();
                sleep(2500);
            } catch (Exception | AssertionError e) {
                System.out.println(">>> cleanup of " + type + " " + mon + " " + d + " failed: " + e);
            }
        }
        try { openList().clearSearch(); } catch (Exception | AssertionError ignored) { }
    }

    private BoxListPage switchToList() {
        BoxSchedulePage b = new BoxSchedulePage(driver);
        assumeTrue(b.openListView() || revealHeader(b) && b.openListView(), "List view button not available");
        sleep(1500);
        b.chooseByDate();
        sleep(2500);
        BoxListPage list = new BoxListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(12)), "The list did not load");
        return list;
    }

    private BoxCalendarPage switchToCalendar() {
        BoxSchedulePage b = new BoxSchedulePage(driver);
        assumeTrue(b.openCalendarView() || revealHeader(b) && b.openCalendarView(), "Calendar view button not available");
        sleep(1500);
        b.chooseMonth();
        sleep(2500);
        BoxCalendarPage cal = new BoxCalendarPage(driver);
        assumeTrue(cal.isLoaded(Duration.ofSeconds(12)), "The calendar did not load");
        return cal;
    }

    /** The view toggle scrolls away with a long list; drag the content down to bring it back. */
    private boolean revealHeader(BoxSchedulePage b) {
        for (int i = 0; i < 3; i++) {
            try {
                var size = driver.manage().window().getSize();
                var finger = new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
                int x = size.getWidth() / 2;
                seq.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.40)));
                seq.addAction(finger.createPointerDown(0));
                seq.addAction(finger.createPointerMove(Duration.ofMillis(500),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, (int) (size.getHeight() * 0.80)));
                seq.addAction(finger.createPointerUp(0));
                driver.perform(List.of(seq));
            } catch (Exception ignored) { }
            sleep(900);
            if (b.textShown("Calendar View") && b.textShown("List View")) return true;
        }
        return false;
    }

    private String safeDeviceTime() {
        try { return driver.getDeviceTime(); } catch (Exception e) { return "(unreadable)"; }
    }

    // ─────────────────────────── helpers ───────────────────────────

    /** Puts one type on one day of next month, committing whatever chooser appears. */
    private boolean putTypeOnDay(String type, String dayNumber) {
        BoxCreateSchedulePage f = openForm();
        if (!pickType(f, type)) return false;
        if (!f.chooseCalendarTab() || !f.calendarShown()) return false;
        // NEXT month, checked on the form's own label: a tap on btnCalNext that did not take
        // used to file the days in the current month, and every view case then failed.
        java.time.YearMonth next = java.time.YearMonth.now().plusMonths(1);
        for (int i = 0; i < 3 && !next.equals(shownMonth(f.calendarMonth())); i++) {
            if (!f.calendarNextMonth()) return false;
            sleep(1500);
        }
        if (!next.equals(shownMonth(f.calendarMonth()))) {
            System.out.println(">>> XV form calendar shows \"" + f.calendarMonth() + "\", not " + next);
            return false;
        }
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
