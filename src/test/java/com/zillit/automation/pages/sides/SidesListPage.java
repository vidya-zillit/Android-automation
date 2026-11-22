package com.zillit.automation.pages.sides;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SIDES — the Sides tab: the generated-sides list, its row overflow, the collapsible
 * History section and the two action buttons.
 *
 * <p>Ids from {@code activity_sides_main.xml}, {@code fragment_sides_list.xml},
 * {@code item_sides.xml} and {@code menu/sides_row_actions.xml}, all read back off a
 * device rather than taken from the source alone.
 */
public class SidesListPage extends BasePage {

    public static final String HEADER = "SIDES";
    /** The scrollable the sides list lives in — what a reader follows past the fold. */
    private static final String SCROLLER = "swipeRefresh";
    public static final String TAB_SIDES = "Sides";
    public static final String TAB_SCRIPT = "Script";

    /** The row overflow's four actions, as the menu renders them. */
    public static final String ACTION_VIEW = "View";
    public static final String ACTION_DOWNLOAD = "Download";
    public static final String ACTION_SEND_DD = "Send via Document Distribution";
    public static final String ACTION_DELETE = "Delete";

    /** The two gate messages, which are the whole point of the §9.1 gates. */
    public static final String GATE_NO_ACTIVE =
            "No active script or pages found";
    public static final String GATE_NO_PUBLISHED =
            "No published script available";

    public SidesListPage(AndroidDriver driver) {
        super(driver);
    }

    /** One row of the sides list. */
    public static final class Row {
        public final String title;
        public final String status;
        public final String meta;
        public final String error;

        Row(String title, String status, String meta, String error) {
            this.title = title; this.status = status; this.meta = meta; this.error = error;
        }

        @Override public String toString() {
            return "\"" + title + "\" [" + status + "] " + meta
                    + (error == null ? "" : " ERROR: " + error);
        }
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("btnGenerate") || displayed("layEmpty") || displayed("rvSides")) {
                return true;
            }
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public String header() { return text("headerText"); }

    /** True while the module shows the thin in-flight bar (E23). */
    public boolean apiLoaderShown() { return displayed("apiLoader"); }

    // ───────────────────────────── tabs ─────────────────────────────

    /**
     * Switches tab by the bottom-nav item's own id, falling back to its label.
     *
     * <p>A {@code BottomNavigationView}'s label is drawn by the item view and does not
     * always reach the accessibility tree as its own text node — measured here: the same
     * {@code tapText("Script")} that works on a fresh screen quietly finds nothing later
     * in a run, which took the cleanup case down with it. The item id is always there.
     */
    public boolean openSidesTab()  { return tapNav("nav_sides", TAB_SIDES); }
    public boolean openScriptTab() { return tapNav("nav_script", TAB_SCRIPT); }

    private boolean tapNav(String itemId, String label) {
        if (clickIfPresent(itemId)) return true;
        return tapText(label);
    }

    public boolean onSidesTab()  { return HEADER.equalsIgnoreCase(trim(header())); }
    public boolean onScriptTab() { return "SCRIPT".equalsIgnoreCase(trim(header())); }

    // ───────────────────────── action buttons ─────────────────────────

    public boolean generateShown()     { return displayed("btnGenerate"); }
    public boolean autogenerateShown() { return displayed("btnAutogenerate"); }

    public boolean tapGenerate()     { return clickIfPresent("btnGenerate"); }
    public boolean tapAutogenerate() { return clickIfPresent("btnAutogenerate"); }

    // ───────────────────────────── rows ─────────────────────────────

    public boolean emptyStateShown() { return displayed("layEmpty"); }

    /**
     * Every row on screen.
     *
     * <p>Found by each row's own title id and then sliced by {@link #bandsOf(String)},
     * never as children of the list: a RecyclerView may nest, and treating a wrapper as
     * one row collapses the whole list into a single entry — a mistake that once made a
     * five-item list read as one.
     */
    public List<Row> rows() {
        return readWholeList(SCROLLER, "tv_title", this::rowsOnScreen, r -> r.title);
    }

    /** The rows currently rendered. {@link #rows()} follows the list past the fold. */
    private List<Row> rowsOnScreen() {
        List<Row> out = new ArrayList<>();
        List<WebElement> titles = elementsById("tv_title");
        List<Band> bands = bandsOf("tv_title");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            Band band = bands.get(i);
            // btn_more is on every side row and nowhere else on this screen, so it is what
            // tells a side row apart from any other list that happens to use tv_title.
            if (elementIn(band, "btn_more") == null) continue;
            out.add(new Row(
                    safe(titles.get(i)),
                    textIn(band, "tv_status"),
                    textIn(band, "tv_meta"),
                    textIn(band, "tv_error")));
        }
        return out;
    }

    public List<String> titles() {
        List<String> out = new ArrayList<>();
        for (Row r : rows()) if (r.title != null) out.add(r.title);
        return out;
    }

    /** The row whose title matches EXACTLY, or null. */
    public Row find(String title) {
        for (Row r : rows()) if (title.equals(r.title)) return r;
        return null;
    }

    public boolean has(String title) { return find(title) != null; }

    /** Opens a row's three-dot overflow. */
    public boolean openRowMenu(String title) {
        // The row has to be on screen before it can be tapped at all.
        if (!scrollToRow(SCROLLER, "tv_title", title)) return false;
        List<WebElement> titles = elementsById("tv_title");
        List<Band> bands = bandsOf("tv_title");
        for (int i = 0; i < titles.size() && i < bands.size(); i++) {
            if (!title.equals(safe(titles.get(i)))) continue;
            WebElement more = elementIn(bands.get(i), "btn_more");
            if (more == null) return false;
            try { more.click(); } catch (Exception e) { return false; }
            pause(1500);
            return true;
        }
        return false;
    }

    /**
     * What the open row menu is offering.
     *
     * <p>The overflow is a {@code PopupMenu}, so each item's label is
     * {@code android:id/title} — the ANDROID package's id, not the app's. Looking it up
     * with the app's own prefix finds nothing, which is why this matches the id across
     * whatever package owns it.
     */
    public List<String> rowMenuOptions() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/title\")"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean chooseRowAction(String label) {
        boolean tapped = tapText(label);
        if (tapped) pause(1500);
        return tapped;
    }

    public boolean dismissRowMenu() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        pause(900);
        return true;
    }

    // ───────────────────────── delete confirm ─────────────────────────

    public static final String DELETE_CONFIRM_HINT = "delete";

    public boolean deleteConfirmShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (textShown("Cancel", Duration.ofMillis(400))
                    && textShown("Delete", Duration.ofMillis(400))) {
                return true;
            }
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /**
     * Confirms the delete.
     *
     * <p>Tapped by the dialog's own button id, not by its text: a
     * {@code MaterialAlertDialog} built with {@code setTitle(delete)} and
     * {@code setPositiveButton(delete)} puts the word "Delete" on screen TWICE, and the
     * first match is the title, which is not clickable. The row overflow — also carrying a
     * "Delete" — may still be in the tree behind the dialog on top of that.
     */
    public boolean confirmDelete() {
        if (tapDialogButton("button1")) { pause(3000); return true; }
        return false;
    }

    public boolean cancelDelete() {
        if (tapDialogButton("button2")) { pause(1500); return true; }
        return false;
    }

    private boolean tapDialogButton(String androidId) {
        try {
            for (WebElement b : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + androidId + "\")"))) {
                if (!b.isDisplayed()) continue;
                b.click();
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // ───────────────────────────── history ─────────────────────────────

    /**
     * Whether the History section is on this screen at all.
     *
     * <p>It sits BELOW the active sides list, so on a project with a screenful of sides it
     * is off the bottom and absent from the tree. Scrolling to it first is what makes the
     * difference between "this project has no History section" and "I did not look".
     */
    public boolean historyShown() {
        if (displayed("historyHeader")) return true;
        return scrollDownTo("historyHeader");
    }

    public boolean toggleHistory() {
        if (!historyShown()) return false;
        boolean tapped = clickIfPresent("historyHeader");
        if (tapped) pause(2500);
        return tapped;
    }

    /** Scrolls the list until a view with this id is on screen. */
    private boolean scrollDownTo(String id) {
        for (int pass = 0; pass < 10; pass++) {
            if (!scrollList(SCROLLER, true)) return false;
            if (displayed(id)) return true;
        }
        return false;
    }

    /** True while the archived list is on screen (it is GONE when collapsed). */
    public boolean historyExpanded() { return displayed("rvHistory"); }

    public boolean historyLoading() { return displayed("historyProgress"); }

    // ───────────────────────── pull to refresh ─────────────────────────

    /** Rows currently drawn in the History list. */
    public int historyRowCount() {
        if (!displayed("rvHistory") && !scrollDownTo("rvHistory")) return 0;
        try {
            int n = 0;
            for (WebElement list : driver.findElements(byId("rvHistory"))) {
                // Child lookups DO work from an element — it is only ancestor:: that never
                // leaves the subtree — but an archived row below the fold is still unread,
                // so this counts what the screen shows after scrolling to the section.
                n += list.findElements(byId("tv_title")).size();
            }
            return n;
        } catch (Exception e) { return 0; }
    }

    /** See {@link SidesGeneratePage#emptyExplanationShown(String)}. */
    public boolean emptyExplanationShown(String keyword) {
        return anyTextMatches(SCROLLER, t -> saysEmpty(t, keyword));
    }

    /**
     * Whether a line both names {@code keyword} and says there is none of it.
     *
     * <p>Shared with {@link SidesGeneratePage} by shape, not by inheritance — the two
     * screens have no common page object between them.
     */
    static boolean saysEmpty(String text, String keyword) {
        String low = text.toLowerCase();
        if (!low.contains(keyword.toLowerCase())) return false;
        return low.startsWith("no ") || low.startsWith("there are no")
                || low.startsWith("there is no") || low.contains("not found")
                || low.contains("nothing");
    }

    public void pullToRefresh() {
        try {
            List<WebElement> sr = driver.findElements(byId("swipeRefresh"));
            if (sr.isEmpty()) return;
            org.openqa.selenium.Rectangle r = sr.get(0).getRect();
            int x = r.getX() + r.getWidth() / 2;
            swipe(x, r.getY() + r.getHeight() / 4, x, r.getY() + (int) (r.getHeight() * 0.8));
            pause(4000);
        } catch (Exception ignored) { }
    }

    // ───────────────────────────── helpers ─────────────────────────────

    /** The card that holds this title — the nearest ancestor that also has {@code marker}. */
    private String childText(WebElement parent, String id) {
        try {
            List<WebElement> els = parent.findElements(byId(id));
            if (els.isEmpty()) return null;
            return safe(els.get(0));
        } catch (Exception e) { return null; }
    }

    private String safe(WebElement e) {
        try {
            String t = e.getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception ex) { return null; }
    }

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean clickIfPresent(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) { e.click(); pause(1200); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    boolean tapText(String label) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + label + "\")"))) {
                if (e.isDisplayed()) { e.click(); pause(900); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Exact text, for dialog buttons where "Delete" must not match "Delete this date". */
    boolean tapExact(String label) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"))) {
                if (e.isDisplayed()) { e.click(); pause(900); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private void swipe(int x1, int y1, int x2, int y2) {
        org.openqa.selenium.interactions.PointerInput f =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence s =
                new org.openqa.selenium.interactions.Sequence(f, 1);
        s.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        s.addAction(f.createPointerDown(0));
        for (int i = 1; i <= 8; i++) {
            s.addAction(f.createPointerMove(Duration.ofMillis(45),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                    x1 + (x2 - x1) * i / 8, y1 + (y2 - y1) * i / 8));
        }
        s.addAction(f.createPointerUp(0));
        driver.perform(List.of(s));
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
