package com.zillit.automation.pages;

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
        Map<String, List<String>> out = new LinkedHashMap<>();
        try {
            // NOT the grid's direct children. The grid nests its cells, so ./* returns one
            // wrapper; reading a day number off that wrapper returns the FIRST one in the
            // whole month and lumps every pill in the grid under it. That produced a map of
            // a single entry — "{30=[Prep, Prep, ...]}" — and made the calendar look as
            // though it had lost every day that was saved.
            //
            // So start from the day numbers themselves, and walk up from each to the cell
            // that holds it. One tvDayNumber is one cell, however the grid is nested.
            for (WebElement label : driver.findElements(byId("tvDayNumber"))) {
                String day = safe(label);
                if (day == null) continue;
                out.put(day, pillsBeside(label));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** The type names in the cell that holds this day number. */
    private List<String> pillsBeside(WebElement dayLabel) {
        List<String> pills = new ArrayList<>();
        try {
            WebElement cell = dayLabel.findElement(AppiumBy.xpath(
                    "./ancestor::*[.//*[contains(@resource-id,'pillContainer')]][1]"));
            for (WebElement holder : cell.findElements(byId("pillContainer"))) {
                for (WebElement t : holder.findElements(
                        AppiumBy.className("android.widget.TextView"))) {
                    String s = safe(t);
                    if (s != null) pills.add(s);
                }
            }
        } catch (Exception ignored) {
            // A cell with nothing on it has no pillContainer to walk up to, which is the
            // ordinary case for a free day — an empty list, not a failure.
        }
        return pills;
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
        List<String> out = new ArrayList<>();
        try {
            for (WebElement label : driver.findElements(byId("tvDayNumber"))) {
                if (!dayNumber.equals(safe(label))) continue;
                WebElement cell = label.findElement(AppiumBy.xpath(
                        "./ancestor::*[.//*[contains(@resource-id,'eventContainer')]][1]"));
                for (WebElement holder : cell.findElements(byId("eventContainer"))) {
                    for (WebElement t : holder.findElements(
                            AppiumBy.className("android.widget.TextView"))) {
                        String s = safe(t);
                        if (s != null) out.add(s);
                    }
                }
                return out;
            }
        } catch (Exception ignored) { }
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
