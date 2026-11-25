package com.zillit.automation.pages.box;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BOX — the Calendar view, in all three of its modes.
 *
 * <p>This exists so the calendar can be compared against the List view. A schedule is one
 * truth with several renderings, and any rendering that disagrees with the others is a
 * defect — but the comparison is only possible because a calendar cell's type "pill"
 * carries the type NAME in a TextView beside its colour dot (CalendarAdapter.createPillView).
 * The colour alone would be useless: colour never reaches the accessibility tree.
 *
 * <p><b>Names here may be cut short.</b> The pill's TextView is {@code 0dp + weight=1} with
 * {@code ellipsize=END}, so on a narrow cell "Prep" survives whole while a longer name
 * arrives as "Day O…". Comparisons against the List view must therefore be made on a
 * prefix, never on equality — asserting equality would report the app as broken for doing
 * exactly what its layout asks.
 *
 * <p>Ids from {@code new_box_fragment_calendar.xml}, {@code new_box_item_calendar_cell.xml}
 * and {@code new_box_item_day_focus.xml}.
 */
public class BoxCalendarPage extends BoxSchedulePage {

    /** How much of a name is safe to compare, given the cell ellipsizes. */
    private static final int SAFE_PREFIX = 3;

    public BoxCalendarPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("calendarGrid") || displayed("dayViewContainer")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /** The header — "November 2026", or the week/day it is showing. */
    public String title() { return text("tvCalTitle"); }

    public boolean next()  { return clickIfPresent("btnNext"); }
    public boolean prev()  { return clickIfPresent("btnPrev"); }
    public boolean today() { return clickIfPresent("btnToday"); }

    /**
     * Day number → the type names shown on that day, as the grid renders them.
     *
     * <p>Month and Week modes both use {@code calendarGrid}, so this serves both. Only
     * cells on screen are reported: a cell scrolled out of the grid is absent from the
     * accessibility tree, not merely hidden.
     */
    public Map<String, List<String>> typesByDay() {
        return textsByDay("pillContainer");
    }

    /**
     * Day number → the texts inside {@code containerId} in that day's cell, matched by
     * POSITION. Each container is filed under the day label in its column that sits closest
     * above it.
     *
     * <p>Not by walking up from the day label with an {@code ./ancestor::} XPath: UiAutomator2
     * evaluates an element-scoped XPath inside that element's own subtree, so the walk found
     * no cell and every day read as empty — XV-02..04 failed with a blank grid on screen
     * that plainly had pills, and FL-10 could not see an event on its day.
     *
     * <p>Padding days of the neighbouring months repeat a number; the first non-empty
     * reading wins.
     */
    private Map<String, List<String>> textsByDay(String containerId) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        try {
            List<WebElement> labels = driver.findElements(byId("tvDayNumber"));
            List<int[]> boxes = new ArrayList<>();          // x, y of each label
            List<String> days = new ArrayList<>();
            for (WebElement l : labels) {
                String d = safe(l);
                if (d == null) continue;
                org.openqa.selenium.Rectangle r = l.getRect();
                boxes.add(new int[]{r.getX(), r.getY()});
                days.add(d);
                out.putIfAbsent(d, new ArrayList<>());
            }
            int col = columnWidth(boxes);
            List<List<String>> perLabel = new ArrayList<>();
            for (int k = 0; k < days.size(); k++) perLabel.add(new ArrayList<>());
            for (WebElement holder : driver.findElements(byId(containerId))) {
                org.openqa.selenium.Rectangle r = holder.getRect();
                int cx = r.getX() + Math.min(r.getWidth(), col) / 2;
                int best = -1;
                for (int k = 0; k < boxes.size(); k++) {
                    int[] b = boxes.get(k);
                    boolean sameColumn = cx >= b[0] - col / 4 && cx < b[0] + col;
                    if (sameColumn && b[1] <= r.getY() && (best < 0 || b[1] > boxes.get(best)[1])) best = k;
                }
                if (best < 0) continue;
                for (WebElement t : holder.findElements(AppiumBy.className("android.widget.TextView"))) {
                    String txt = safe(t);
                    if (txt != null) perLabel.get(best).add(txt);
                }
            }
            for (int k = 0; k < days.size(); k++) {
                if (out.get(days.get(k)).isEmpty() && !perLabel.get(k).isEmpty()) out.put(days.get(k), perLabel.get(k));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** The smallest gap between distinct label columns — one cell's width. */
    private static int columnWidth(List<int[]> boxes) {
        java.util.TreeSet<Integer> xs = new java.util.TreeSet<>();
        for (int[] b : boxes) xs.add(b[0]);
        int best = Integer.MAX_VALUE;
        Integer prev = null;
        for (int x : xs) {
            if (prev != null && x - prev > 20) best = Math.min(best, x - prev);
            prev = x;
        }
        return best == Integer.MAX_VALUE ? 150 : best;
    }

    private String safe(WebElement e) {
        try {
            String t = e.getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception ex) { return null; }
    }

    /** The types the grid shows on one day, or null when that day is not on screen. */
    public List<String> typesOn(String dayNumber) {
        return typesByDay().get(dayNumber);
    }

    /** Event and note titles the grid shows on a day — the other half of a cell. */
    public List<String> titlesOn(String dayNumber) {
        List<String> t = textsByDay("eventContainer").get(dayNumber);
        return t == null ? new ArrayList<>() : t;
    }

    // ───────────────────────────── search (shared with the List) ─────────────────────────────

    /** Types into the calendar's search box ({@code etCalendarSearch}); "" clears it. */
    public boolean search(String term) {
        try {
            for (WebElement e : driver.findElements(byId("etCalendarSearch"))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!term.isEmpty()) e.sendKeys(term);
                hideKeyboard();
                pause(1500);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    public String searchText() { return text("etCalendarSearch"); }

    /** Every schedule pill and event/note title the grid draws on screen, as text. */
    public List<String> everythingDrawn() {
        List<String> out = new ArrayList<>();
        for (String holder : List.of("pillContainer", "eventContainer")) {
            try {
                for (WebElement h : driver.findElements(byId(holder))) {
                    for (WebElement t : h.findElements(AppiumBy.className("android.widget.TextView"))) {
                        String s = safe(t);
                        if (s != null) out.add(s);
                    }
                }
            } catch (Exception ignored) { }
        }
        return out;
    }

    // ───────────────────────────── Day mode ─────────────────────────────

    public boolean dayModeShown() { return displayed("dayViewContainer"); }

    /** The schedules Day mode lists for the day it is focused on. */
    public List<String> dayModeSchedules() { return dayModeTexts("dayFocusScheduleContainer"); }

    /** The events Day mode lists. */
    public List<String> dayModeEvents() { return dayModeTexts("dayFocusEventContainer"); }

    /** The notes Day mode lists. */
    public List<String> dayModeNotes() { return dayModeTexts("dayFocusNoteContainer"); }

    public String dayModeNumber() { return text("tvDayFocusNumber"); }

    public boolean dayModeEmpty() { return displayed("dayFocusEmptyState"); }

    private List<String> dayModeTexts(String containerId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement holder : driver.findElements(byId(containerId))) {
                for (WebElement t : holder.findElements(
                        AppiumBy.className("android.widget.TextView"))) {
                    String s = t.getText();
                    if (s != null && !s.isBlank()) out.add(s.trim());
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ───────────────────────────── matching ─────────────────────────────

    /**
     * Does any of these rendered names stand for {@code wanted}?
     *
     * <p>Prefix-based on purpose: the cell ellipsizes, so "Day Off" can arrive as "Day O…".
     * Equality here would fail on correct behaviour.
     */
    public static boolean names(List<String> rendered, String wanted) {
        if (rendered == null || wanted == null) return false;
        String w = wanted.toLowerCase();
        String head = w.substring(0, Math.min(SAFE_PREFIX, w.length()));
        for (String r : rendered) {
            if (r == null) continue;
            String s = r.toLowerCase().replace("…", "").replace("...", "").trim();
            if (s.isEmpty()) continue;
            if (w.startsWith(s) || s.startsWith(head)) return true;
        }
        return false;
    }

    private String childText(WebElement parent, String id) {
        try {
            List<WebElement> els = parent.findElements(byId(id));
            if (els.isEmpty()) return null;
            String t = els.get(0).getText();
            return t == null || t.isBlank() ? null : t;
        } catch (Exception e) { return null; }
    }
}
