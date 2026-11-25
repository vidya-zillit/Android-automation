package com.zillit.automation.pages.scheduledist;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools → "Schedule Full &amp; One Line" (identifier {@code schedule_distribution_tool}):
 * {@code ScheduleDistActivity}, a TabLayout + ViewPager2 over three fragments.
 *
 * <p>Traps, read from the layouts (see {@code SCHEDULE_DIST_MODULE_TESTCASES.md}):
 * <ul>
 *   <li><b>The tile is not called "Schedule Distribution".</b> The label is server-supplied;
 *       on the automation project it is "Schedule Full &amp; One Line".</li>
 *   <li><b>All three fragments share ids</b> ({@code recDocument}, {@code addFb},
 *       {@code userHistoryBtn}, {@code emptyView}) and {@code offscreenPageLimit = 1} keeps the
 *       neighbour attached — every lookup goes through {@link #onScreen}.</li>
 *   <li><b>Swiping is disabled</b> ({@code isUserInputEnabled = false}): tabs are tapped.</li>
 *   <li><b>Plain Material tabs</b>: the name is a TextView inside the tab; the unread count is a
 *       BadgeDrawable overlay, not a view.</li>
 *   <li><b>Row TextViews are snake_case</b>: {@code tv_name}, {@code tv_date},
 *       {@code tv_created_by}, {@code tv_episode_no}; a row's root is {@code layMainCard}.</li>
 *   <li><b>The empty state has no text</b>; assert on {@code emptyView}.</li>
 * </ul>
 */
public class ScheduleDistPage extends VisiblePage {

    public static final String TOOL_TILE = "Schedule Full & One Line";
    public static final String TAB_FULL = "Schedule Full";
    public static final String TAB_PAGES = "Pages";
    public static final String TAB_ONE_LINE = "Schedule One Line";
    public static final String UPLOAD_FULL = "Upload Full Schedule";
    public static final String UPLOAD_ONE_LINE = "Upload One Line";
    public static final String UPLOAD_PAGE = "Upload Page";

    public ScheduleDistPage(AndroidDriver driver) {
        super(driver);
    }

    /** One Full / One Line row (or one page inside a Pages folder). */
    public static final class Row {
        public final String createdBy, name, date, episode, scene, subTitle;
        Row(String createdBy, String name, String date, String episode, String scene, String subTitle) {
            this.createdBy = createdBy; this.name = name; this.date = date;
            this.episode = episode; this.scene = scene; this.subTitle = subTitle;
        }
        @Override public String toString() {
            StringBuilder b = new StringBuilder();
            for (String s : new String[]{scene, name, episode, date, createdBy, subTitle}) {
                if (s != null && !s.isEmpty()) { if (b.length() > 0) b.append(" / "); b.append(s); }
            }
            return b.toString();
        }
    }

    /** One Pages folder row ({@code item_script_folder}). */
    public static final class Folder {
        public final String title, pageDate, subTitle, detail, episode;
        Folder(String title, String pageDate, String subTitle, String detail, String episode) {
            this.title = title; this.pageDate = pageDate; this.subTitle = subTitle;
            this.detail = detail; this.episode = episode;
        }
        @Override public String toString() { return title + (subTitle == null ? "" : " / " + subTitle); }
    }

    // ─────────────────────────── shell and tabs ───────────────────────────

    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen("tabLayout").isEmpty() && tabNames().contains(TAB_FULL)) return true;
            pause(400);
        }
        return false;
    }

    public String header() { return firstText("headerText"); }

    /** Tab names, left to right. */
    public List<String> tabNames() {
        List<String> out = new ArrayList<>();
        for (WebElement t : tabTexts()) out.add(text(t));
        return out;
    }

    private List<WebElement> tabTexts() {
        List<WebElement> out = new ArrayList<>();
        try {
            List<WebElement> layout = driver.findElements(byId("tabLayout"));
            if (layout.isEmpty()) return out;
            out.addAll(layout.get(0).findElements(AppiumBy.className("android.widget.TextView")));
        } catch (WebDriverException ignored) { }
        out.sort((a, b) -> Integer.compare(centreX(a), centreX(b)));
        return out;
    }

    public boolean openTab(String name) {
        for (WebElement t : tabTexts()) {
            if (name.equals(text(t))) {
                try { t.click(); } catch (WebDriverException e) { return false; }
                pause(1500);
                waitLoaded(Duration.ofSeconds(30));
                return true;
            }
        }
        return false;
    }

    public boolean isSelected(String name) {
        for (WebElement t : tabTexts()) {
            if (name.equals(text(t))) {
                try { return Boolean.parseBoolean(t.getAttribute("selected")); }
                catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }

    /** The shimmer (Full / One Line) and the linear bar (Pages) gone. */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (onScreen("loaderView").isEmpty() && onScreen("progress").isEmpty()
                    && onScreen("circularProgress").isEmpty()) return true;
            pause(500);
        }
        return false;
    }

    public String uploadLabel() { return firstText("addFb"); }
    public boolean uploadShown() { return !onScreen("addFb").isEmpty(); }
    public boolean tapUpload() { return tapFirst("addFb"); }
    public boolean historyShown() { return !onScreen("userHistoryBtn").isEmpty(); }
    public boolean tapHistory() { return tapFirst("userHistoryBtn"); }
    public boolean emptyShown() { return !onScreen("emptyView").isEmpty(); }

    /** Horizontal swipe across the list. The pager has user input switched off, so nothing should move. */
    public void swipeLeft() {
        try {
            int w = driver.manage().window().getSize().getWidth();
            int h = driver.manage().window().getSize().getHeight();
            org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence s = new org.openqa.selenium.interactions.Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), (int) (w * 0.85), h / 2));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(300), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), (int) (w * 0.15), h / 2));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
            pause(1200);
        } catch (WebDriverException ignored) { }
    }

    /** Pull-to-refresh from the top of the list. */
    public void pullToRefresh() {
        List<WebElement> rec = onScreen("recDocument");
        if (rec.isEmpty()) return;
        try {
            org.openqa.selenium.Rectangle r = rec.get(0).getRect();
            int x = r.getX() + r.getWidth() / 2;
            org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence s = new org.openqa.selenium.interactions.Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, r.getY() + 40));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(600), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, r.getY() + r.getHeight() / 2));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
            pause(2500);
        } catch (WebDriverException ignored) { }
    }

    // ─────────────────────────── Full / One Line rows ───────────────────────────

    /** Rows on screen, read downward from each {@code layMainCard}. */
    public List<Row> visibleRows() {
        List<Row> out = new ArrayList<>();
        for (WebElement r : onScreen("layMainCard")) {
            Row row = readRow(r);
            if (row != null) out.add(row);
        }
        return out;
    }

    private Row readRow(WebElement root) {
        String date = childText(root, "tv_date");
        if (date == null) return null;
        return new Row(childText(root, "tv_created_by"), childText(root, "tv_name"), date,
                childText(root, "tv_episode_no"), childText(root, "tv_scene_no"), childText(root, "tv_sub_title"));
    }

    /** Every row, following the list past the fold. */
    public List<Row> allRows() {
        return readWholeList("recDocument", "tv_date", this::visibleRows, Row::toString);
    }

    public List<Row> rowsWith(java.util.function.Predicate<Row> match) {
        List<Row> out = new ArrayList<>();
        for (Row r : allRows()) if (match.test(r)) out.add(r);
        return out;
    }

    /** The on-screen root of the first row matching, scrolling to it. */
    private WebElement rowRoot(java.util.function.Predicate<Row> match) {
        for (int pass = 0; pass < 14; pass++) {
            for (WebElement r : onScreen("layMainCard")) {
                Row row = readRow(r);
                if (row != null && match.test(row)) return r;
            }
            if (!scrollList("recDocument", true)) break;
        }
        return null;
    }

    /** Opens the ⋮ of the first matching row and returns the menu's items, or null. */
    public List<String> openRowMenu(java.util.function.Predicate<Row> match) {
        WebElement r = rowRoot(match);
        if (r == null) return null;
        try {
            List<WebElement> more = r.findElements(byId("imgMore"));
            if (more.isEmpty()) return null;
            more.get(0).click();
            pause(1200);
        } catch (WebDriverException e) { return null; }
        return menuItems();
    }

    public boolean tapRow(java.util.function.Predicate<Row> match) {
        WebElement r = rowRoot(match);
        if (r == null) return false;
        try {
            List<WebElement> t = r.findElements(byId("tv_date"));
            (t.isEmpty() ? r : t.get(0)).click();
            pause(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ─────────────────────────── Pages folders ───────────────────────────

    /** The filter's current mode: "Sn No." / "Color" / "Episode". */
    public String filterMode() { return firstText("tvOperationType"); }

    /** Opens the filter popup and picks a mode by its label. */
    public boolean chooseFilter(String label) {
        if (!tapFirst("operationHolder")) return false;
        boolean ok = tapMenuItem(label);
        pause(1500);
        return ok;
    }

    public List<String> filterOptions() {
        if (!tapFirst("operationHolder")) return List.of();
        List<String> items = menuItems();
        closeMenu();
        return items;
    }

    /** Search on the Pages tab is server-side behind a 700ms debounce — wait it out. */
    public boolean searchPages(String query) {
        List<WebElement> roots = onScreen("searchIncluded");
        WebElement box = null;
        try {
            if (!roots.isEmpty()) {
                List<WebElement> in = roots.get(0).findElements(byId("search_widget_edit_textvw"));
                if (!in.isEmpty()) box = in.get(0);
            }
            if (box == null) {
                List<WebElement> any = onScreen("search_widget_edit_textvw");
                if (any.isEmpty()) return false;
                box = any.get(0);
            }
            box.click();
            box.clear();
            if (!query.isEmpty()) box.sendKeys(query);
        } catch (WebDriverException e) { return false; }
        pause(2500);
        waitLoaded(Duration.ofSeconds(20));
        return true;
    }

    /**
     * Types {@code query} into the Pages search one character at a time, {@code gapMs} apart —
     * the way a fast typist does it, so each keystroke fires its own text change and the 700ms
     * debounce sees a burst (web gap SCH-02). Does NOT wait for the search to settle.
     *
     * <p>Whether an element {@code sendKeys} appends or replaces differs between Appium
     * versions, so the first two keystrokes decide: if the box then reads the two-character
     * prefix, keys append; otherwise each step sets the growing prefix instead.
     */
    public boolean typePagesSearchSlowly(String query, long gapMs) {
        WebElement box = pagesSearchBox();
        if (box == null || query.isEmpty()) return false;
        try {
            box.click();
            box.clear();
            pause(gapMs);
            box.sendKeys(query.substring(0, 1));
            pause(gapMs);
            if (query.length() == 1) return true;
            box.sendKeys(query.substring(1, 2));
            pause(gapMs);
            boolean appends = query.substring(0, 2).equals(text(box));
            if (!appends) box.sendKeys(query.substring(0, 2));
            for (int i = 3; i <= query.length(); i++) {
                box.sendKeys(appends ? query.substring(i - 1, i) : query.substring(0, i));
                pause(gapMs);
            }
            return query.equals(text(box));
        } catch (WebDriverException e) { return false; }
    }

    private WebElement pagesSearchBox() {
        try {
            List<WebElement> roots = onScreen("searchIncluded");
            if (!roots.isEmpty()) {
                List<WebElement> in = roots.get(0).findElements(byId("search_widget_edit_textvw"));
                if (!in.isEmpty()) return in.get(0);
            }
            List<WebElement> any = onScreen("search_widget_edit_textvw");
            return any.isEmpty() ? null : any.get(0);
        } catch (WebDriverException e) { return null; }
    }

    public String pagesSearchHint() {
        List<WebElement> any = onScreen("search_widget_edit_textvw");
        if (any.isEmpty()) return null;
        try {
            String h = any.get(0).getAttribute("hint");
            return h == null || h.isBlank() ? text(any.get(0)) : h.trim();
        } catch (WebDriverException e) { return null; }
    }

    /**
     * Colour search the way a user does it: in colour mode the search icon ({@code imageView6})
     * opens the colour popup, and picking one writes its name into the box, which then runs the
     * server search behind its debounce.
     */
    public boolean searchByColour(String colourName) {
        List<WebElement> icons = onScreen("imageView6");
        if (icons.isEmpty()) return false;
        try { icons.get(0).click(); } catch (WebDriverException e) { return false; }
        pause(1200);
        if (!tapMenuItem(colourName)) { closeMenu(); return false; }
        pause(1500);
        waitLoaded(Duration.ofSeconds(20));
        return true;
    }

    /**
     * The folder list once it has stopped changing. The Pages search is server-side behind a
     * 700ms debounce, so a read straight after typing can return the PREVIOUS query's rows —
     * which is how "SALMON" once appeared to find a BLUE page.
     */
    public List<Folder> settledFolders() {
        List<Folder> last = null;
        for (int i = 0; i < 8; i++) {
            waitLoaded(Duration.ofSeconds(15));
            List<Folder> now = visibleFolders();
            if (last != null && now.toString().equals(last.toString())) return now;
            last = now;
            pause(1500);
        }
        return last == null ? List.of() : last;
    }

    public List<Folder> visibleFolders() {
        List<Folder> out = new ArrayList<>();
        for (WebElement r : onScreen("layMainHolder")) {
            String title = childText(r, "tv_revision_name");
            if (title == null) continue;
            out.add(new Folder(title, childText(r, "tv_page_date"), childText(r, "tv_sub_title"),
                    childText(r, "tv_detail"), childText(r, "tv_episode_no")));
        }
        return out;
    }

    public List<Folder> allFolders() {
        return readWholeList("recDocument", "tv_revision_name", this::visibleFolders, f -> f.title + "|" + f.subTitle);
    }

    public boolean openFolder(String title) {
        if (!scrollToRow("recDocument", "tv_revision_name", title)) return false;
        for (WebElement e : onScreen("tv_revision_name")) {
            if (title.equals(text(e))) {
                try { e.click(); pause(2500); return true; }
                catch (WebDriverException ex) { return false; }
            }
        }
        return false;
    }

    /** A folder row's background colour, sampled from its card — the page colour picked on upload. */
    public int[] folderColour(String title) {
        for (WebElement r : onScreen("layMainHolder")) {
            if (!title.equals(childText(r, "tv_revision_name"))) continue;
            try {
                List<WebElement> holder = r.findElements(byId("linearLayoutHolder"));
                WebElement target = holder.isEmpty() ? r : holder.get(0);
                byte[] png = target.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
                if (bi == null) return null;
                int p = bi.getRGB(bi.getWidth() - 8, bi.getHeight() / 2);
                return new int[]{(p >> 16) & 0xff, (p >> 8) & 0xff, p & 0xff};
            } catch (Exception e) { return null; }
        }
        return null;
    }

    // ─────────────────────────── helpers ───────────────────────────

    /**
     * History's own search (Full / One Line): the {@code searchView} include, filtered locally
     * on episode, folder, schedule name, scene and page number with a 300ms debounce. There
     * is a second, hidden search widget in the same layout, so the visible one is used.
     */
    public boolean searchHistory(String query) {
        List<WebElement> roots = onScreen("searchView");
        try {
            WebElement box = null;
            for (WebElement r : roots) {
                List<WebElement> in = r.findElements(byId("search_widget_edit_textvw"));
                if (!in.isEmpty()) { box = in.get(0); break; }
            }
            if (box == null) {
                List<WebElement> any = onScreen("search_widget_edit_textvw");
                if (any.isEmpty()) return false;
                box = any.get(0);
            }
            box.click();
            box.clear();
            if (!query.isEmpty()) box.sendKeys(query);
            pause(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Text of the first on-screen element with this id — e.g. a History screen's {@code headerMsg}. */
    public String firstTextOf(String id) { return firstText(id); }

    /** On-screen elements with this id (the ViewPager's hidden neighbour excluded). */
    public List<WebElement> onScreenIds(String id) { return onScreen(id); }

    protected String childText(WebElement root, String id) {
        try {
            List<WebElement> c = root.findElements(byId(id));
            if (c.isEmpty()) return null;
            String s = text(c.get(0));
            return s.isEmpty() ? null : s;
        } catch (WebDriverException e) { return null; }
    }
}
