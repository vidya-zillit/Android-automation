package com.zillit.automation.pages.crewdood;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Days off &amp; holidays ({@code CrewDoodCalendarFragment}, layout
 * {@code fragment_crew_dood_calendar}) — one calendar for the whole project, added into
 * {@code detailContainer} above the crew list.
 *
 * <p><b>The month grid is one custom view</b> ({@code CrewDoodMonthView}) that draws every day
 * itself, so there are no day elements to find. Days are tapped by geometry, the same way the
 * view resolves a touch: a 28dp weekday header, then Monday-first rows of 44dp with 4dp gaps,
 * seven equal columns. The density is taken from the view's own measured height (it is exactly
 * {@code 28 + rows*44 + (rows-1)*4} dp), so nothing is tuned per device.
 *
 * <p>Weekday toggles are plain TextViews whose content-desc is the full English day name and
 * whose {@code selected} state means "weekly day off".
 */
public class CrewDoodCalendarPage extends CrewDoodPage {

    public static final String TITLE = "Days off & holidays";
    public static final String HINT = "One calendar for the whole project. Nobody is booked on a day off or a holiday — not even a shoot day.";
    public static final String WEEKLY_OFF_HELP = "Tap a weekday to select or deselect it as a weekly day off";
    public static final String NEED_WORKING_DAY = "Leave at least one working day in the week.";
    public static final String NO_HOLIDAYS = "No holidays yet. Tap dates on the calendar to add one.";
    public static final String PICK_END = "tap end date";
    public static final String RANGE_TOO_LONG = "A range can be at most 366 days.";
    public static final String RANGE_EXISTS = "Every day in that range is already a holiday.";
    public static final String SAVED = "Days off & holidays saved.";
    public static final String SAVE_FAILED = "Couldn't save the days off & holidays. Try again.";
    public static final int MAX_RANGE = 366;

    /** Monday first, as the toggles are laid out. */
    public static final List<String> WEEK = List.of("Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday");

    private static final DateTimeFormatter MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter WD_D_MON_Y = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter D_MON = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter D_MON_Y = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public CrewDoodCalendarPage(AndroidDriver driver) {
        super(driver);
    }

    /** The app's own range label: "Tue 13 Oct 2026", "12 – 16 Oct 2026", "28 Sep – 2 Oct 2026". */
    public static String rangeLabel(LocalDate start, LocalDate end) {
        if (start.equals(end)) return WD_D_MON_Y.format(start);
        String from;
        if (start.getYear() == end.getYear() && start.getMonth() == end.getMonth()) from = String.valueOf(start.getDayOfMonth());
        else if (start.getYear() == end.getYear()) from = D_MON.format(start);
        else from = D_MON_Y.format(start);
        return from + " – " + D_MON_Y.format(end);
    }

    public static String daysCount(int n) { return n == 1 ? "1 day" : n + " days"; }

    // ───────────────────────────── shell ─────────────────────────────

    public boolean isOpen(java.time.Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (isOpen()) return true;
            pause(400);
        }
        return false;
    }

    /** The editor's toolbar (close + title + save) stays put while its body scrolls. */
    public boolean isOpen() { return !inPane("detailContainer", "close").isEmpty(); }

    public boolean saveEnabled() {
        List<WebElement> s = inPane("detailContainer", "save");
        if (s.isEmpty()) return false;
        try { return Boolean.parseBoolean(s.get(0).getAttribute("enabled")); }
        catch (WebDriverException e) { return false; }
    }

    public boolean saveShown() { return !inPane("detailContainer", "save").isEmpty(); }

    public boolean tapSave() {
        List<WebElement> s = inPane("detailContainer", "save");
        if (s.isEmpty()) return false;
        hideKeyboardIfShown();
        try { s.get(0).click(); return true; } catch (WebDriverException e) { return false; }
    }

    public boolean tapClose() {
        hideKeyboardIfShown();
        List<WebElement> c = inPane("detailContainer", "close");
        if (c.isEmpty()) return false;
        try { c.get(0).click(); pause(1200); return true; } catch (WebDriverException e) { return false; }
    }

    /** Waits until the editor is gone (a successful save pops it). */
    public boolean waitClosed(java.time.Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!isOpen()) return true;
            pause(400);
        }
        return false;
    }

    /** All visible texts in the editor — used to check fixed copy. */
    public boolean showsText(String s) {
        return !driver.findElements(org.openqa.selenium.By.xpath(
                "//*[@resource-id='" + appPackage() + ":id/detailContainer']//*[@text=" + xq(s) + "]")).isEmpty();
    }

    // ───────────────────────────── weekly days off ─────────────────────────────

    public String weekSummary() { return firstInPane("detailContainer", "weekSummary"); }
    public String weekHelp()    { return firstInPane("detailContainer", "weekHelp"); }

    private WebElement weekday(String fullName) {
        scrollEditorTo("weekDays", true);
        List<WebElement> e = driver.findElements(org.openqa.selenium.By.xpath(
                "//*[@resource-id='" + appPackage() + ":id/weekDays']/*[@content-desc='" + fullName + "']"));
        return e.isEmpty() ? null : e.get(0);
    }

    /** Full day names (Monday…) currently marked as weekly days off, Monday first. */
    public List<String> weeklyOff() {
        scrollEditorTo("weekDays", true);
        List<String> out = new ArrayList<>();
        for (String d : WEEK) {
            List<WebElement> e = driver.findElements(org.openqa.selenium.By.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/weekDays']/*[@content-desc='" + d + "']"));
            if (e.isEmpty()) continue;
            try { if (Boolean.parseBoolean(e.get(0).getAttribute("selected"))) out.add(d); }
            catch (WebDriverException ignored) { }
        }
        return out;
    }

    public boolean toggleWeekday(String fullName) {
        WebElement e = weekday(fullName);
        if (e == null) return false;
        try { e.click(); pause(500); return true; } catch (WebDriverException ex) { return false; }
    }

    /** "Sat, Sun off · 5-day week" for the given off days (Monday first); just the length for 0 or 7. */
    public static String expectedSummary(List<String> offMondayFirst) {
        String len = (7 - offMondayFirst.size()) + "-day week";
        if (offMondayFirst.isEmpty() || offMondayFirst.size() == 7) return len;
        List<String> shortNames = new ArrayList<>();
        for (String d : offMondayFirst) shortNames.add(d.substring(0, 3));
        return String.join(", ", shortNames) + " off · " + len;
    }

    // ───────────────────────────── month grid ─────────────────────────────

    public String monthTitle() { return firstInPane("detailContainer", "monthTitle"); }

    public YearMonth shownMonth() {
        String t = monthTitle();
        if (t == null) return null;
        try { return YearMonth.parse(t, MONTH_TITLE); } catch (Exception e) { return null; }
    }

    public boolean nextMonth() { return tapInEditor("nextMonth"); }
    public boolean prevMonth() { return tapInEditor("prevMonth"); }

    /** Pages the grid to {@code target} with the arrows. */
    public boolean goToMonth(YearMonth target) {
        scrollEditorTo("monthTitle", true);
        for (int i = 0; i < 40; i++) {
            YearMonth m = shownMonth();
            if (m == null) return false;
            if (m.equals(target)) return true;
            if (m.isBefore(target)) nextMonth(); else prevMonth();
            pause(250);
        }
        return target.equals(shownMonth());
    }

    /** Taps one day in the grid, paging the month and scrolling the editor as needed. */
    public boolean tapDay(LocalDate d) {
        if (!goToMonth(YearMonth.from(d))) return false;
        YearMonth ym = YearMonth.from(d);
        int lead = ym.atDay(1).getDayOfWeek().getValue() - 1;          // Monday = 0
        int rows = (lead + ym.lengthOfMonth() + 6) / 7;
        int idx = lead + d.getDayOfMonth() - 1;
        int row = idx / 7, col = idx % 7;
        for (int attempt = 0; attempt < 4; attempt++) {
            List<WebElement> mv = inPane("detailContainer", "monthView");
            if (mv.isEmpty()) return false;
            Rectangle r = rect(mv.get(0));
            double dp = r.getHeight() / (28.0 + rows * 44 + (rows - 1) * 4);
            int x = r.getX() + (int) ((col + 0.5) * r.getWidth() / 7.0);
            int y = r.getY() + (int) ((28 + row * 48 + 22) * dp);
            int visibleBottom = visibleBottom();
            int visibleTop = editorTop();
            if (y > visibleTop + 10 && y < visibleBottom - 10) {
                tapAt(x, y);
                pause(600);
                return true;
            }
            // Bring that row into the open part of the editor.
            int scrollTop = visibleTop, mid = (visibleTop + visibleBottom) / 2;
            int cx = r.getX() + r.getWidth() / 2;
            if (y >= visibleBottom - 10) drag(cx, Math.max(mid, scrollTop + 200), cx, Math.max(scrollTop + 60, mid - (y - mid)));
            else drag(cx, mid, cx, Math.min(visibleBottom - 60, mid + (visibleTop - y) + 200));
            pause(600);
        }
        return false;
    }

    /** Selects one day, or a range by two taps. */
    public boolean selectRange(LocalDate first, LocalDate second) {
        if (!tapDay(first)) return false;
        return second == null || tapDay(second);
    }

    private int editorTop() {
        List<WebElement> s = inPane("detailContainer", "scroll");
        return s.isEmpty() ? 0 : rect(s.get(0)).getY();
    }

    /** The bottom of the open part of the editor: the draft panel's top when it is up. */
    private int visibleBottom() {
        List<WebElement> s = inPane("detailContainer", "scroll");
        int bottom = s.isEmpty() ? driver.manage().window().getSize().getHeight()
                : rect(s.get(0)).getY() + rect(s.get(0)).getHeight();
        if (draftShown()) {
            List<WebElement> d = inPane("detailContainer", "draft");
            if (!d.isEmpty()) bottom = Math.min(bottom, rect(d.get(0)).getY());
        }
        return bottom;
    }

    // ───────────────────────────── the draft panel ─────────────────────────────

    public boolean draftShown() {
        List<WebElement> d = inPane("detailContainer", "draftRange");
        if (d.isEmpty()) return false;
        try { return d.get(0).isDisplayed() && rect(d.get(0)).getY() < driver.manage().window().getSize().getHeight(); }
        catch (WebDriverException e) { return false; }
    }

    public String draftRange() { return firstInPane("detailContainer", "draftRange"); }
    public String draftCount() { return firstInPane("detailContainer", "draftCount"); }

    public String draftError() {
        List<WebElement> e = inPane("detailContainer", "draftError");
        return e.isEmpty() ? null : text(e.get(0));
    }

    public void typeDraftName(String name) {
        List<WebElement> f = inPane("detailContainer", "draftName");
        if (f.isEmpty()) return;
        try { f.get(0).click(); f.get(0).clear(); if (!name.isEmpty()) f.get(0).sendKeys(name); }
        catch (WebDriverException ignored) { }
        pause(400);
        hideKeyboardIfShown();
    }

    public boolean tapAdd()   { return tapInEditor("draftAdd"); }
    public boolean tapClear() { return tapInEditor("draftClear"); }

    // ───────────────────────────── holiday list ─────────────────────────────

    /** One row of the holiday list: the name box, "label · N days", the date tile. */
    public static final class Holiday {
        public final String name, meta, tileMonth, tileDay;
        Holiday(String n, String m, String tm, String td) { name = n; meta = m; tileMonth = tm; tileDay = td; }
        @Override public String toString() { return "'" + name + "' " + meta + " [" + tileMonth + " " + tileDay + "]"; }
    }

    public String holidayCount() { return firstInPane("detailContainer", "holidayCount"); }

    public boolean holidayEmptyShown() {
        scrollEditorTo("holidayEmpty", false);
        return !inPane("detailContainer", "holidayEmpty").isEmpty();
    }

    /** Every row of the holiday list, scrolling the editor to its end. */
    public List<Holiday> holidays() {
        java.util.LinkedHashMap<String, Holiday> seen = new java.util.LinkedHashMap<>();
        scrollEditorTo("holidayCount", false);
        for (int pass = 0; pass < 15; pass++) {
            int before = seen.size();
            for (Holiday h : holidaysOnScreen()) seen.putIfAbsent(h.meta + "|" + h.name, h);
            if (pass > 0 && seen.size() == before) break;
            scrollEditor(true);
        }
        return new ArrayList<>(seen.values());
    }

    private List<Holiday> holidaysOnScreen() {
        List<Holiday> out = new ArrayList<>();
        List<WebElement> metas = inPane("holidayList", "meta");
        List<WebElement> names = inPane("holidayList", "name");
        List<WebElement> tm = inPane("holidayList", "tileMonth");
        List<WebElement> td = inPane("holidayList", "tileDay");
        for (int i = 0; i < metas.size(); i++) {
            Rectangle r = rect(metas.get(i));
            int from = r.getY() - 150, to = r.getY() + r.getHeight() + 40;
            String name = nameText(textInBand(names, from, to));
            out.add(new Holiday(name, text(metas.get(i)), textInBand(tm, from, to), textInBand(td, from, to)));
        }
        return out;
    }

    /** An empty name box reads as its hint. */
    private static String nameText(String s) { return "Add a name".equals(s) ? "" : s; }

    /** Removes the row whose name is {@code name} (its trash icon). */
    public boolean removeHoliday(String name) {
        scrollEditorTo("holidayCount", false);
        for (int pass = 0; pass < 15; pass++) {
            List<WebElement> names = inPane("holidayList", "name");
            List<WebElement> deletes = inPane("holidayList", "delete");
            for (WebElement n : names) {
                if (!name.equals(text(n))) continue;
                Rectangle r = rect(n);
                for (WebElement d : deletes) {
                    Rectangle dr = rect(d);
                    int cy = dr.getY() + dr.getHeight() / 2;
                    if (cy > r.getY() - 60 && cy < r.getY() + r.getHeight() + 100) {
                        try { d.click(); pause(700); return true; } catch (WebDriverException e) { return false; }
                    }
                }
            }
            if (!scrollEditor(true)) break;
        }
        return false;
    }

    /** Removes every row whose name starts with {@code prefix}; returns how many. */
    public int removeHolidaysStartingWith(String prefix) {
        int removed = 0;
        for (int guard = 0; guard < 30; guard++) {
            String hit = null;
            for (Holiday h : holidays()) if (h.name.startsWith(prefix)) { hit = h.name; break; }
            if (hit == null || !removeHoliday(hit)) break;
            removed++;
        }
        return removed;
    }

    public boolean renameHoliday(String from, String to) {
        scrollEditorTo("holidayCount", false);
        for (int pass = 0; pass < 15; pass++) {
            for (WebElement n : inPane("holidayList", "name")) {
                if (!from.equals(text(n))) continue;
                try {
                    n.click();
                    n.clear();
                    n.sendKeys(to);
                    pause(400);
                    hideKeyboardIfShown();
                    return true;
                } catch (WebDriverException e) { return false; }
            }
            if (!scrollEditor(true)) break;
        }
        return false;
    }

    // ───────────────────────────── scrolling the editor ─────────────────────────────

    private boolean scrollEditor(boolean down) {
        List<WebElement> s = inPane("detailContainer", "scroll");
        if (s.isEmpty()) return false;
        Rectangle r = rect(s.get(0));
        int x = r.getX() + r.getWidth() / 2;
        int bottom = Math.min(r.getY() + r.getHeight(), visibleBottom());
        int low = bottom - 120, high = r.getY() + 150;
        if (low <= high + 100) return false;
        if (down) drag(x, low, x, high); else drag(x, high, x, low);
        pause(600);
        return true;
    }

    /** Scrolls until a view with {@code id} is on screen; {@code up} starts by going up. */
    private void scrollEditorTo(String id, boolean up) {
        for (int i = 0; i < 8; i++) {
            List<WebElement> e = inPane("detailContainer", id);
            if (!e.isEmpty()) {
                Rectangle r = rect(e.get(0));
                if (r.getY() >= editorTop() && r.getY() + r.getHeight() <= visibleBottom()) return;
            }
            if (!scrollEditor(!up)) return;
        }
    }

    private boolean tapInEditor(String id) {
        List<WebElement> e = inPane("detailContainer", id);
        if (e.isEmpty()) return false;
        try { e.get(0).click(); pause(700); return true; } catch (WebDriverException ex) { return false; }
    }

    private static String xq(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    /** The next date on or after {@code from} that is not one of the weekly days off given. */
    public static LocalDate nextWorkingDay(LocalDate from, List<String> off) {
        LocalDate d = from;
        for (int i = 0; i < 7; i++) {
            String name = d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
            if (!off.contains(name)) return d;
            d = d.plusDays(1);
        }
        return from;
    }

    static DayOfWeek dow(String fullName) { return DayOfWeek.valueOf(fullName.toUpperCase(Locale.ROOT)); }
}
