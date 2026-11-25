package com.zillit.automation.pages.crewdood;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crew DOOD ({@code CrewDoodActivity}, layout {@code activity_crew_dood}): the crew list, its
 * search / department filter, the person detail ({@code fragment_crew_dood_detail}) and the
 * department bottom sheet ({@code sheet_crew_dood_dept}).
 *
 * <p><b>The id trap.</b> The detail and the Days-off editor are fragments ADDED into
 * {@code detailContainer} on top of the list; the list ({@code listPane}) stays in the
 * accessibility tree underneath. The list's person rows and the detail header share the ids
 * {@code name}, {@code role}, {@code avatar}, and the list's {@code days} TextView shares its
 * id with the detail's {@code days} RecyclerView. So every read here is scoped to one pane.
 */
public class CrewDoodPage extends VisiblePage {

    // ── strings (res/values/strings_crew_dood.xml) ──
    public static final String TOOL_TILE = "Crew DOOD";
    public static final String SEARCH_HINT = "Role or name";
    public static final String ALL_DEPTS = "All depts";
    public static final String ALL_DEPARTMENTS = "All departments";
    public static final String DEPARTMENT_TITLE = "Department";
    public static final String NO_DAYS = "–";
    public static final String TBC = "TBC";
    public static final String NO_CREW = "No crew on this project yet.";
    public static final String NO_MATCH = "No crew match your filters.";
    public static final String LOAD_FAILED = "Couldn't load the Crew DOOD. Check your connection and try again.";
    public static final String CALENDAR_BUTTON = "Days off & holidays";
    public static final String EXPORT = "Export";
    public static final String EXPORT_FAILED = "Couldn't export the Crew DOOD.";
    /** The detail's key chips, in order: badge → meaning. NONE is never drawn. */
    public static final List<String[]> KEY = List.of(
            new String[]{"SW", "Start work"}, new String[]{"W", "Working"},
            new String[]{"WF", "Work finish"}, new String[]{"SWF", "One day"},
            new String[]{"OFF", "Off"}, new String[]{"HOL", "Holiday"});

    public static final Pattern BOOKED = Pattern.compile("(\\d+)/(\\d+) booked");
    public static final Pattern DAYS = Pattern.compile("(\\d+) days?");
    public static final Pattern MONTH_WORKING = Pattern.compile("(\\d+) working");

    public CrewDoodPage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────────── where am I ─────────────────────────────

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!onScreen("search").isEmpty() && !onScreen("calendarButton").isEmpty()) return true;
            pause(500);
        }
        return false;
    }

    /** True while the person detail or the Days-off editor sits on top of the list. */
    public boolean overlayOpen() {
        // The editor's weekday row scrolls away; its toolbar (close / save) never does.
        return !inPane("detailContainer", "key").isEmpty() || !inPane("detailContainer", "close").isEmpty();
    }

    public boolean detailOpen() { return !inPane("detailContainer", "key").isEmpty(); }

    /** Waits for the first load: rows, the empty/no-match message, or the error box. */
    public boolean waitForData(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!inPane("listPane", "deptName").isEmpty()) return true;
            if (!elementsById("stateText").isEmpty()) return true;
            pause(500);
        }
        return false;
    }

    public String toolbarTitle() { return firstText("headerText"); }

    // ───────────────────────────── list ─────────────────────────────

    /** One list row: a department header, or a person under it. */
    public static final class Row {
        public final boolean header;
        public final String dept;          // header: its name; person: the header above it
        public final String text;          // header: deptCount ("0/1 booked"); person: name
        public final String role;          // person only; "" when hidden
        public final String days;          // person only: "–" or "N day(s)"
        final WebElement anchor;

        Row(boolean header, String dept, String text, String role, String days, WebElement anchor) {
            this.header = header; this.dept = dept; this.text = text; this.role = role;
            this.days = days; this.anchor = anchor;
        }

        public String name() { return header ? dept : text; }

        @Override public String toString() {
            return header ? "[" + dept + " " + text + "]" : dept + " › " + text + " / " + role + " / " + days;
        }
    }

    /** The rows on screen, top to bottom (headers and people). */
    public List<Row> visibleRows() {
        List<Rectangle> rects = new ArrayList<>();
        List<WebElement> anchors = new ArrayList<>();
        List<Boolean> isHeader = new ArrayList<>();
        for (WebElement e : inPane("listPane", "deptName")) { anchors.add(e); isHeader.add(true); }
        for (WebElement e : inPane("listPane", "name")) { anchors.add(e); isHeader.add(false); }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < anchors.size(); i++) { order.add(i); rects.add(rect(anchors.get(i))); }
        order.sort(Comparator.comparingInt(i -> rects.get(i).getY()));

        List<WebElement> counts = inPane("listPane", "deptCount");
        List<WebElement> roles = inPane("listPane", "role");
        List<WebElement> days = inPane("listPane", "days");
        List<Row> out = new ArrayList<>();
        String dept = null;
        for (int k = 0; k < order.size(); k++) {
            int i = order.get(k);
            Rectangle r = rects.get(i);
            int from = r.getY() - r.getHeight() / 2;
            int to = k + 1 < order.size() ? rects.get(order.get(k + 1)).getY() - 1 : Integer.MAX_VALUE;
            if (isHeader.get(i)) {
                dept = text(anchors.get(i));
                out.add(new Row(true, dept, textInBand(counts, from, to), "", "", anchors.get(i)));
            } else {
                out.add(new Row(false, dept, text(anchors.get(i)), textInBand(roles, from, to),
                        textInBand(days, from, to), anchors.get(i)));
            }
        }
        return out;
    }

    /** Every row, following the list past the fold; keyed by dept + name. Scrolls back after. */
    public List<Row> allRows() {
        List<Row> seen = new ArrayList<>();
        java.util.Set<String> keys = new java.util.HashSet<>();
        int scrolls = 0;
        for (int pass = 0; pass < 12; pass++) {
            int before = keys.size();
            for (Row r : visibleRows()) if (keys.add(r.header + "|" + r.dept + "|" + r.text)) seen.add(r);
            if (pass > 0 && keys.size() == before) break;
            if (!listCanScrollFurther()) break;
            scrollList("list", true);
            scrolls++;
        }
        // Back up exactly as far as we went: one drag too many at the top is a pull-to-refresh.
        for (int i = 0; i < scrolls; i++) scrollList("list", false);
        return seen;
    }

    public List<Row> people() {
        List<Row> out = new ArrayList<>();
        for (Row r : allRows()) if (!r.header) out.add(r);
        return out;
    }

    public List<Row> headers() {
        List<Row> out = new ArrayList<>();
        for (Row r : allRows()) if (r.header) out.add(r);
        return out;
    }

    private boolean listCanScrollFurther() {
        List<WebElement> list = inPane("listPane", "list");
        if (list.isEmpty()) return false;
        List<WebElement> anchors = new ArrayList<>(inPane("listPane", "name"));
        anchors.addAll(inPane("listPane", "deptName"));
        if (anchors.size() <= 1) return false;
        int bottom = rect(list.get(0)).getY() + rect(list.get(0)).getHeight();
        int last = anchors.stream().mapToInt(e -> rect(e).getY()).max().orElse(0);
        return last + 250 > bottom;   // a row of ~280px (person row) would not fit below
    }

    public boolean tapPerson(String name) {
        for (int pass = 0; pass < 8; pass++) {
            for (Row r : visibleRows()) {
                if (!r.header && name.equals(r.text)) {
                    try { r.anchor.click(); pause(1500); return detailOpen(); }
                    catch (WebDriverException e) { return false; }
                }
            }
            if (!listCanScrollFurther()) break;
            scrollList("list", true);
        }
        return false;
    }

    /** Taps a department header (collapse / expand). */
    public boolean tapHeader(String dept) {
        for (Row r : visibleRows()) {
            if (r.header && dept.equals(r.dept)) {
                try { r.anchor.click(); pause(900); return true; }
                catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }

    // ───────────────────────────── search / filter ─────────────────────────────

    public void search(String text) {
        try {
            WebElement f = driver.findElement(byId("search"));
            f.click();
            f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
        } catch (WebDriverException ignored) { }
        pause(900);
        hideKeyboardIfShown();
    }

    public void clearSearch() { search(""); }

    /** The search box's text; the hint when empty. */
    public String searchText() { return firstText("search"); }

    public String deptLabel() { return firstText("deptLabel"); }

    /** The message box shown in place of the list (empty / no match / error), or null. */
    public String stateMessage() {
        List<WebElement> s = onScreen("stateText");
        return s.isEmpty() ? null : text(s.get(0));
    }

    public boolean retryShown() { return !onScreen("retry").isEmpty(); }

    public boolean openDepartmentSheet() {
        if (!tapFirst("deptButton")) return false;
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            if (!elementsById("optionLabel").isEmpty()) return true;
            pause(300);
        }
        return false;
    }

    public boolean departmentSheetShown() { return !elementsById("optionLabel").isEmpty(); }

    public List<String> departmentOptions() {
        List<String> out = new ArrayList<>();
        for (WebElement e : elementsById("optionLabel")) out.add(text(e));
        return out;
    }

    /** The option whose check mark is shown, or null. */
    public String checkedDepartment() {
        List<WebElement> labels = elementsById("optionLabel");
        List<WebElement> checks = elementsById("optionCheck");
        for (WebElement c : checks) {
            try {
                if (!c.isDisplayed()) continue;
                int cy = rect(c).getY() + rect(c).getHeight() / 2;
                for (WebElement l : labels) {
                    Rectangle r = rect(l);
                    if (cy >= r.getY() - 20 && cy <= r.getY() + r.getHeight() + 20) return text(l);
                }
            } catch (WebDriverException ignored) { }
        }
        return null;
    }

    public boolean pickDepartment(String label) {
        for (WebElement e : elementsById("optionLabel")) {
            // The sheet shows department names as typed; list headers are textAllCaps.
            if (label.equalsIgnoreCase(text(e))) {
                try { e.click(); pause(1200); return true; }
                catch (WebDriverException ex) { return false; }
            }
        }
        return false;
    }

    /** The sheet is a BottomSheetDialog: BACK closes it without choosing. */
    public void closeDepartmentSheet() {
        if (departmentSheetShown()) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            pause(800);
        }
    }

    public boolean pullToRefresh() {
        List<WebElement> list = inPane("listPane", "list");
        if (list.isEmpty()) return false;
        Rectangle r = rect(list.get(0));
        int x = r.getX() + r.getWidth() / 2;
        drag(x, r.getY() + 60, x, r.getY() + (int) (r.getHeight() * 0.6));
        pause(2500);
        return true;
    }

    // ───────────────────────────── toolbar buttons ─────────────────────────────

    public boolean calendarButtonEnabled() { return enabled("calendarButton"); }
    public boolean exportButtonEnabled()   { return enabled("exportButton"); }

    public boolean tapCalendar() { return tapFirst("calendarButton"); }
    public boolean tapExport()   { return tapFirst("exportButton"); }

    public boolean exportInProgress() { return !onScreen("exportProgress").isEmpty(); }

    /** The phase band (Prep / Shoot / Post) — drawn only when the project has production dates. */
    public boolean phaseBandShown() { return !onScreen("phaseBand").isEmpty(); }

    // ───────────────────────────── detail ─────────────────────────────

    public String detailDept() { return firstInPane("detailContainer", "dept"); }
    public String detailName() { return firstInPane("detailContainer", "name"); }
    public String detailRole() {
        String r = firstInPane("detailContainer", "role");
        return r == null ? "" : r;
    }

    /** The key's texts, flattened in reading order: "SW", "Start work", "W", … */
    public List<String> detailKeyTexts() {
        List<WebElement> key = inPane("detailContainer", "key");
        List<String> out = new ArrayList<>();
        if (key.isEmpty()) return out;
        List<WebElement> texts = new ArrayList<>();
        try { texts.addAll(key.get(0).findElements(By.className("android.widget.TextView"))); }
        catch (WebDriverException ignored) { }
        texts.sort(Comparator.<WebElement>comparingInt(e -> rect(e).getY() / 40).thenComparingInt(e -> rect(e).getX()));
        for (WebElement t : texts) out.add(text(t));
        return out;
    }

    /** Month headers on screen in the detail: "November 2026" → "0 working". */
    public List<String[]> detailMonths() {
        List<String[]> out = new ArrayList<>();
        List<WebElement> titles = inPane("detailContainer", "monthTitle");
        List<WebElement> working = inPane("detailContainer", "monthWorking");
        for (WebElement t : titles) {
            Rectangle r = rect(t);
            out.add(new String[]{text(t), textInBand(working, r.getY() - 30, r.getY() + r.getHeight() + 30)});
        }
        return out;
    }

    /** One day row in the detail: date number, weekday, label (phase or holiday name), badge. */
    public static final class Day {
        public final String number, weekday, label, badge;
        Day(String n, String w, String l, String b) { number = n; weekday = w; label = l; badge = b; }
        @Override public String toString() { return number + " " + weekday + " '" + label + "' [" + badge + "]"; }
    }

    public List<Day> detailDaysOnScreen() {
        List<Day> out = new ArrayList<>();
        List<WebElement> numbers = inPane("detailContainer", "dayNumber");
        List<WebElement> weekdays = inPane("detailContainer", "weekday");
        List<WebElement> labels = inPane("detailContainer", "dayLabel");
        List<WebElement> badges = inPane("detailContainer", "badge");
        numbers.sort(Comparator.comparingInt(e -> rect(e).getY()));
        for (int i = 0; i < numbers.size(); i++) {
            Rectangle r = rect(numbers.get(i));
            int from = r.getY() - 40;
            int to = i + 1 < numbers.size() ? rect(numbers.get(i + 1)).getY() - 40 : r.getY() + 3 * r.getHeight();
            out.add(new Day(text(numbers.get(i)), textInBand(weekdays, from, to),
                    textInBand(labels, from, to), textInBand(badges, from, to)));
        }
        return out;
    }

    /** Scrolls the detail's day list one window down (true) or up. */
    public boolean scrollDetail(boolean down) {
        List<WebElement> days = inPane("detailContainer", "days");
        // The detail's "days" is the RecyclerView (the list's "days" are TextViews).
        WebElement rv = null;
        for (WebElement d : days) {
            try { if (String.valueOf(d.getAttribute("className")).contains("RecyclerView")) rv = d; }
            catch (WebDriverException ignored) { }
        }
        if (rv == null) return false;
        Rectangle r = rect(rv);
        int x = r.getX() + r.getWidth() / 2;
        // A short, slow drag that HOLDS before lifting: a quick one flings and skips a week.
        int low = r.getY() + (int) (r.getHeight() * 0.75), high = r.getY() + (int) (r.getHeight() * 0.40);
        if (down) slowDrag(x, low, x, high); else slowDrag(x, high, x, low);
        pause(700);
        return true;
    }

    protected void slowDrag(int x1, int y1, int x2, int y2) {
        try {
            PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x1, y1));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(900), PointerInput.Origin.viewport(), x2, y2));
            s.addAction(f.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), x2, y2));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
        } catch (WebDriverException ignored) { }
    }

    /** BACK pops the top fragment (detail or editor). */
    public void back() {
        hideKeyboardIfShown();
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(1200);
    }

    // ───────────────────────────── toasts ─────────────────────────────

    private static final Pattern TOAST = Pattern.compile(
            "<android\\.widget\\.Toast[^>]*?text=\"([^\"]*)\"");

    /** The text of a toast that shows up within the timeout, or null. */
    public String toastText(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String src = driver.getPageSource();
                Matcher m = TOAST.matcher(src == null ? "" : src);
                if (m.find()) return unescape(m.group(1));
            } catch (WebDriverException ignored) { }
            pause(250);
        }
        return null;
    }

    static String unescape(String s) {
        return s.replace("&apos;", "'").replace("&quot;", "\"").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&#10;", "\n").replace("&amp;", "&");
    }

    // ───────────────────────────── helpers ─────────────────────────────

    /** Elements with {@code id} that sit inside the view {@code paneId}, top to bottom. */
    protected List<WebElement> inPane(String paneId, String id) {
        List<WebElement> out = new ArrayList<>();
        String p = appPackage() + ":id/";
        try {
            out.addAll(driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + p + paneId + "']//*[@resource-id='" + p + id + "']")));
        } catch (WebDriverException ignored) { }
        out.sort(Comparator.comparingInt(e -> rect(e).getY()));
        return out;
    }

    protected String firstInPane(String paneId, String id) {
        List<WebElement> e = inPane(paneId, id);
        return e.isEmpty() ? null : text(e.get(0));
    }

    protected boolean enabled(String id) {
        List<WebElement> e = elementsById(id);
        if (e.isEmpty()) return false;
        try { return Boolean.parseBoolean(e.get(0).getAttribute("enabled")); }
        catch (WebDriverException ex) { return false; }
    }

    protected static Rectangle rect(WebElement e) {
        try { return e.getRect(); }
        catch (WebDriverException ex) { return new Rectangle(0, Integer.MAX_VALUE / 2, 0, 0); }
    }

    protected static String textInBand(List<WebElement> candidates, int from, int to) {
        for (WebElement c : candidates) {
            Rectangle r = rect(c);
            int cy = r.getY() + r.getHeight() / 2;
            if (cy >= from && cy < to) return text(c);
        }
        return "";
    }

    protected void drag(int x1, int y1, int x2, int y2) {
        try {
            PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x1, y1));
            s.addAction(f.createPointerDown(0));
            for (int i = 1; i <= 10; i++) {
                s.addAction(f.createPointerMove(Duration.ofMillis(40), PointerInput.Origin.viewport(),
                        x1 + (x2 - x1) * i / 10, y1 + (y2 - y1) * i / 10));
            }
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
        } catch (WebDriverException ignored) { }
    }

    protected void tapAt(int x, int y) {
        try {
            PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(60), PointerInput.Origin.viewport(), x, y));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
        } catch (WebDriverException ignored) { }
    }
}
