package com.zillit.automation.pages.box;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * BOX — the List view, which is the only surface that says in words what sits on a day.
 *
 * <p>The calendar draws its types as coloured cells, and colour does not reach the
 * accessibility tree. The list does: in <b>By Date</b> each row carries a date and the
 * type on it ({@code tvDate}, {@code tvTypeName}), and in <b>By Schedule</b> each row is a
 * block with its type, its day count and its span ({@code tvBlockTypeName},
 * {@code tvDayCount}, {@code tvDateRange}). Ids from {@code new_box_item_schedule_day.xml}.
 *
 * <p>That makes this the page that can actually answer what Replace, Extend and Overlap
 * did — the questions the conflict cases could previously only ask of a dialog.
 */
public class BoxListPage extends BoxSchedulePage {

    public BoxListPage(AndroidDriver driver) {
        super(driver);
    }

    /** One day of the schedule: the date as shown, and the type sitting on it. */
    public static final class DayRow {
        public final String date;
        public final String type;
        DayRow(String date, String type) { this.date = date; this.type = type; }
        @Override public String toString() { return date + " = " + type; }
    }

    /** One block of consecutive days of a single type. */
    public static final class Block {
        public final String type;
        public final String dayCount;
        public final String range;
        Block(String type, String dayCount, String range) {
            this.type = type; this.dayCount = dayCount; this.range = range;
        }
        @Override public String toString() {
            return type + " × " + dayCount + " (" + range + ")";
        }
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("scheduleList") || displayed("emptyState")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean isEmpty() { return displayed("emptyState"); }

    /**
     * Every By-Date row on screen, as date → type.
     *
     * <p>Only what is on screen: rows scrolled out of a RecyclerView are absent from the
     * tree, so a caller that needs a particular date should search with
     * {@link #findDay(String)} rather than assume this list is complete.
     */
    public List<DayRow> dayRows() {
        List<DayRow> out = new ArrayList<>();
        for (WebElement row : rows()) {
            String date = childText(row, "tvDate");
            String type = childText(row, "tvTypeName");
            if (date != null || type != null) out.add(new DayRow(date, type));
        }
        return out;
    }

    /** Every By-Schedule block on screen. */
    public List<Block> blocks() {
        List<Block> out = new ArrayList<>();
        for (WebElement row : rowsOf("byScheduleLayout")) {
            String type = childText(row, "tvBlockTypeName");
            if (type == null) continue;
            out.add(new Block(type, childText(row, "tvDayCount"),
                    childText(row, "tvDateRange")));
        }
        return out;
    }

    /** The type on a given date, scrolling to find it; null when that date has no row. */
    public String typeOn(String dateFragment) {
        for (DayRow r : dayRows()) {
            if (r.date != null && r.date.contains(dateFragment)) return r.type;
        }
        if (!findDay(dateFragment)) return null;
        for (DayRow r : dayRows()) {
            if (r.date != null && r.date.contains(dateFragment)) return r.type;
        }
        return null;
    }

    /** Scrolls the list until a row carrying this text is on screen. */
    public boolean findDay(String textFragment) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".setMaxSearchSwipes(12)"
                            + ".scrollIntoView(new UiSelector().textContains(\""
                            + textFragment + "\"))"));
            pause(500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** How many rows on screen carry this type. */
    public long daysOfType(String type) {
        return dayRows().stream()
                .filter(r -> r.type != null && r.type.contains(type)).count();
    }

    /** One event or note as the list prints it. */
    public static final class EventRow {
        public final String kind;
        public final String title;
        public final String date;
        public final String distributeTo;
        EventRow(String kind, String title, String date, String distributeTo) {
            this.kind = kind; this.title = title; this.date = date;
            this.distributeTo = distributeTo;
        }
        @Override public String toString() {
            return (kind == null ? "?" : kind) + " \"" + title + "\" on " + date
                    + " → " + distributeTo;
        }
    }

    /**
     * Every event and note row on screen.
     *
     * <p>The list mixes two kinds of row: schedule days ({@code byDateLayout}) and
     * events/notes ({@code new_box_item_list_event}). This reads the second kind, which is
     * the only place the app prints an event's title, its date and <b>who it went to</b>
     * together — so it is where an edit can be checked from the outside.
     */
    public List<EventRow> eventRows() {
        List<EventRow> out = new ArrayList<>();
        try {
            List<WebElement> lists = driver.findElements(byId("scheduleList"));
            if (lists.isEmpty()) return out;
            for (WebElement row : lists.get(0).findElements(byId("leTitle"))) {
                // leTitle is unique per event row, so each one found is one row; its
                // siblings are read from the row's own parent.
                out.add(new EventRow(
                        siblingText(row, "leKindBadge"),
                        safeText(row),
                        siblingText(row, "leDate"),
                        siblingText(row, "leDistributeTo")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** The event or note with this title, or null. */
    public EventRow findEvent(String title) {
        for (EventRow r : eventRows()) {
            if (r.title != null && r.title.contains(title)) return r;
        }
        return null;
    }

    /** Taps Edit on the event row carrying this title. */
    public boolean editEvent(String title) { return tapEventControl(title, "leBtnEdit"); }

    /** Taps Delete on the event row carrying this title. */
    public boolean deleteEvent(String title) { return tapEventControl(title, "leBtnDelete"); }

    private boolean tapEventControl(String title, String controlId) {
        try {
            List<WebElement> lists = driver.findElements(byId("scheduleList"));
            if (lists.isEmpty()) return false;
            List<WebElement> titles = lists.get(0).findElements(byId("leTitle"));
            List<WebElement> controls = lists.get(0).findElements(byId(controlId));
            // Paired by POSITION, not index: rows the user cannot edit (another member's
            // event) carry no controls, so the i-th control is not the i-th row's.
            for (int i = 0; i < titles.size(); i++) {
                String t = safeText(titles.get(i));
                if (t == null || !t.contains(title)) continue;
                int y = titles.get(i).getRect().getY();
                int next = Integer.MAX_VALUE;
                for (WebElement o : titles) { int oy = o.getRect().getY(); if (oy > y && oy < next) next = oy; }
                WebElement mine = null;
                for (WebElement c : controls) {
                    int cy = c.getRect().getY();
                    if (cy > y && cy < next && (mine == null || cy < mine.getRect().getY())) mine = c;
                }
                if (mine == null) {
                    System.out.println(">>> list: event \"" + t + "\" has no " + controlId + " on screen ("
                            + titles.size() + " rows, " + controls.size() + " controls)");
                    return false;
                }
                mine.click();
                pause(1800);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /**
     * Reads another field of the row that holds this element.
     *
     * <p>Walks up to the nearest ancestor that actually contains the wanted field, matched
     * on the id's tail so no package prefix is needed. Going up a fixed number of levels
     * would break the moment the row's layout gained a wrapper.
     */
    private String siblingText(WebElement anchorEl, String id) {
        try {
            WebElement row = anchorEl.findElement(AppiumBy.xpath(
                    "./ancestor::*[.//*[contains(@resource-id,'" + id + "')]][1]"));
            return childText(row, id);
        } catch (Exception e) { return null; }
    }

    private String safeText(WebElement e) {
        try {
            String t = e.getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception ex) { return null; }
    }

    public boolean search(String term) { return fill("etListSearch", term); }

    /** What the List's search box holds — the search text is shared with the Calendar. */
    public String searchText() { return text("etListSearch"); }

    /** Every visible text inside the empty state, joined — its title and description. */
    public String emptyStateText() {
        StringBuilder b = new StringBuilder();
        try {
            for (WebElement root : driver.findElements(byId("emptyState"))) {
                if (!root.isDisplayed()) continue;
                for (WebElement t : root.findElements(AppiumBy.className("android.widget.TextView"))) {
                    String s = safeText(t);
                    if (s != null) { if (b.length() > 0) b.append(" / "); b.append(s); }
                }
            }
        } catch (Exception ignored) { }
        return b.toString();
    }

    /**
     * The event or note with this title, scrolling the UNFILTERED list for it (bounded). For
     * cleanup when a search cannot find the row — the List search ignores event titles
     * (web gap BOX-01).
     */
    public EventRow findEventScrolling(String title, int maxSwipes) {
        for (int i = 0; i <= maxSwipes; i++) {
            EventRow r = findEvent(title);
            if (r != null) return r;
            if (!scrollList("scheduleList", true)) break;
        }
        return null;
    }

    /** Scrolls the list back to its top (bounded). */
    public void scrollToTop() {
        for (int i = 0; i < 8 && scrollList("scheduleList", false); i++) { }
    }

    public boolean clearSearch() { return fill("etListSearch", ""); }

    /**
     * How many days of this type the project really holds, searched and counted end to end.
     *
     * <p>Counted as a SET of dates gathered window by window, not as a number taken from
     * the screen. Two things made the old version undercount, and both of them lie quietly:
     *
     * <ol>
     *   <li>It only scrolled when the first window already showed eight rows — a magic
     *       number standing in for "a screenful". On a device that fits five, a block of
     *       eight days read as five and the list was never walked at all. That is how a
     *       complete-schedule edit that had worked perfectly reported eight days as five.</li>
     *   <li>It took the MAXIMUM count across windows. A row that scrolls out of the
     *       accessibility tree takes its count with it, so a list of fifteen read in
     *       windows of five never reports more than five, however far it is scrolled.</li>
     * </ol>
     *
     * <p>Collecting dates instead fixes both: every window adds what it can see, duplicates
     * merge, and the walk stops when a window brings nothing new.
     *
     * @return the number of distinct dates of that type, or -1 if the search box is missing
     */
    public int daysOfTypeFiltered(String type) {
        if (!applySearch(type)) return -1;
        settleAfterSearch(type);
        java.util.Set<String> dates = new java.util.LinkedHashSet<>();
        int sameWindows = 0;
        for (int pass = 0; pass < 14; pass++) {
            int before = dates.size();
            for (DayRow r : dayRows()) {
                if (r.type != null && r.type.contains(type) && r.date != null) {
                    dates.add(r.date.trim());
                }
            }
            if (dates.size() == before) {
                // Two windows in a row with nothing new: the end, or a list that does not
                // scroll. One is not enough — a window can be all duplicates mid-list.
                if (++sameWindows >= 2) break;
            } else {
                sameWindows = 0;
            }
            scrollListDown();
        }
        for (int i = 0; i < 14; i++) scrollListUp();
        return dates.size();
    }

    /** Every row on screen, as text, for a failure message that can be acted on. */
    public List<String> rowSummaries() {
        List<String> out = new ArrayList<>();
        for (DayRow r : dayRows()) out.add(r.toString());
        return out;
    }

    /**
     * Types a search term and checks it actually landed.
     *
     * <p>{@code fill} only reports that it typed something. It does not say the box kept
     * the text, and a count taken while the field is still empty is a count of the whole
     * list — or, worse, of a list mid-refresh, which is empty. That is how a project
     * holding seven Shoot days reported zero moments after reporting seven.
     *
     * @return false only when the box cannot be made to hold the term at all
     */
    private boolean applySearch(String term) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (fill("etListSearch", term)) {
                // POLL for the read-back. text() returns null both when the box holds
                // nothing AND when the box is not in the tree yet, and right after the
                // list view is (re)built it is the second one — a single read a second
                // later called a perfectly good search a failure.
                long end = System.currentTimeMillis() + 4000;
                String got;
                do {
                    got = text("etListSearch");
                    if (got != null && got.trim().equalsIgnoreCase(term.trim())) return true;
                    pause(400);
                } while (System.currentTimeMillis() < end);
                System.out.println(">>> box search: typed \"" + term + "\" but after 4s the"
                        + " box reads \"" + got + "\" (attempt " + attempt + " of 3)");
            }
            pause(1200);
        }
        return false;
    }

    /**
     * Waits for the filtered list to stop changing.
     *
     * <p>The list re-filters asynchronously. Counted too early it is either still the old
     * list or briefly empty, and "briefly empty" is indistinguishable from "this project
     * has none" unless you look twice.
     */
    private void settleAfterSearch(String type) {
        long end = System.currentTimeMillis() + 9000;
        int stable = 0;
        long previous = -1;
        do {
            pause(1200);
            long now = dayRows().stream()
                    .filter(r -> r.type != null && r.type.contains(type)).count();
            if (now > 0 && now == previous) return;       // settled with rows
            if (now == 0 && previous == 0 && ++stable >= 2) return;  // settled empty
            previous = now;
        } while (System.currentTimeMillis() < end);
    }

    private void scrollListUp() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollBackward()"));
            pause(500);
        } catch (Exception ignored) { }
    }

    private void scrollListDown() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            pause(700);
        } catch (Exception ignored) { }
    }

    /**
     * Deletes one day's row — the unit cancelling a single day out of a block.
     *
     * <p>Row actions live behind {@code rowActions} with {@code btnRowEdit} and
     * {@code btnRowDelete} ({@code new_box_item_schedule_day.xml}); they may need the row
     * tapped first to reveal them.
     */
    public boolean deleteDay(String dateFragment) { return tapRowControl(dateFragment, "btnRowDelete"); }

    /** Finds the row carrying this date and taps the named control on that same row. */
    private boolean tapRowControl(String dateFragment, String controlId) {
        List<WebElement> rows = rows();
        List<WebElement> controls = rowControls(controlId);
        for (int i = 0; i < rows.size(); i++) {
            String date = childText(rows.get(i), "tvDate");
            if (date == null || !date.contains(dateFragment)) continue;
            if (i >= controls.size()) {
                System.out.println(">>> list: row " + date + " has no " + controlId
                        + " (" + rows.size() + " rows, " + controls.size() + " controls)");
                return false;
            }
            try { controls.get(i).click(); pause(1500); return true; }
            catch (Exception e) { return false; }
        }
        return false;
    }

    /**
     * Taps a row's Edit — the start of the update flow, which is a different path from
     * create: it goes through the scope prompt and then a pre-filled form whose date is
     * locked when the scope is a single day.
     */
    public boolean editDay(String dateFragment) { return tapRowControl(dateFragment, "btnRowEdit"); }

    /** The first date on screen carrying this type, or null. */
    public String firstDateOfType(String type) {
        for (DayRow r : dayRows()) {
            if (r.type != null && r.type.toLowerCase().contains(type.toLowerCase())) {
                return r.date;
            }
        }
        return null;
    }

    /** Confirms whatever delete dialog the row raised. */
    public boolean confirmDelete() {
        for (String label : new String[] { "Delete", "DELETE", "Yes", "OK" }) {
            if (tapText(label)) { pause(2500); return true; }
        }
        return false;
    }

    /**
     * One element per visible row, in the order they are drawn.
     *
     * <p>NOT the list's direct children: {@code scheduleList} holds a single nested
     * RecyclerView, so {@code ./*} returns that one wrapper. Treating it as a row collapsed
     * the whole list into one entry — every count came back as 1, and a five-day block read
     * as one day while By Schedule correctly said five. That looked exactly like the app
     * contradicting itself across two views, and it was this method.
     *
     * <p>Each row contributes one {@code byDateLayout} (By Date mode) or one
     * {@code byScheduleLayout} (By Schedule mode), so those are what get counted.
     */
    private List<WebElement> rows() { return rowsOf("byDateLayout"); }

    private List<WebElement> rowsOf(String layoutId) {
        try {
            List<WebElement> lists = driver.findElements(byId("scheduleList"));
            if (lists.isEmpty()) return List.of();
            return lists.get(0).findElements(byId(layoutId));
        } catch (Exception e) { return List.of(); }
    }

    /**
     * The row controls, index-matched to {@link #rows()}.
     *
     * <p>Edit and Delete sit in the item's {@code rowActions}, a sibling of
     * {@code byDateLayout} rather than a child, so they cannot be read from inside a row.
     * Each item carries exactly one of each, and both lists come back in draw order, so
     * position n in one belongs to position n in the other.
     */
    private List<WebElement> rowControls(String id) {
        try {
            List<WebElement> lists = driver.findElements(byId("scheduleList"));
            if (lists.isEmpty()) return List.of();
            return lists.get(0).findElements(byId(id));
        } catch (Exception e) { return List.of(); }
    }

    private String childText(WebElement row, String id) {
        try {
            List<WebElement> els = row.findElements(byId(id));
            if (els.isEmpty()) return null;
            String t = els.get(0).getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception e) { return null; }
    }

    private boolean fill(String id, String value) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!value.isEmpty()) e.sendKeys(value);
                hideKeyboard();
                pause(800);
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }
}
