package com.zillit.automation.pages.continuity;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools → Continuity: {@code ContinuityPage}, a TabLayout + ViewPager2 over two copies of
 * the {@code ContinuityDepartment} fragment — "My Department" and "All\nDepartment".
 *
 * <p>What this screen does to a locator, read from the layouts (see
 * {@code CONTINUITY_MODULE_TESTCASES.md}, "How each screen has to be read"):
 *
 * <ul>
 *   <li><b>The tabs are custom views.</b> The name is in {@code tv_title}, never in the
 *       TabLayout's own text, and the second tab's name holds a REAL newline
 *       ("All\nDepartment"). A clickable {@code img_info} sits inside each tab and steals
 *       a tap aimed at the tab's middle — so a tab is switched by tapping {@code tv_title}.</li>
 *   <li><b>ViewPager2 keeps the other tab alive</b> once it has been visited: two
 *       {@code recDepartment}s, two {@code searchView}s, two {@code takeSceneShots}. Every
 *       lookup here keeps only elements whose centre is on screen.</li>
 *   <li><b>A tab row's root has no id</b>; the row is its {@code textView} ("Scene No-12" —
 *       no spaces, hard-coded).</li>
 *   <li><b>The empty state has no text</b> — {@code data_not_found_yet} is an empty string —
 *       so emptiness is the {@code emptyView} id being displayed.</li>
 * </ul>
 */
public class ContinuityPage extends VisiblePage {

    public static final String HEADER = "Continuity";
    public static final String TAB_MY = "My Department";
    /** Matched by containment: the real text is "All\nDepartment". */
    public static final String TAB_ALL_PART = "All";
    public static final String ROW_PREFIX = "Scene No-";

    public static final String NOTE_MY = "Note: You need to upload in 'My Department' folder"
            + " in order to forward to 'All Departments' folder";
    public static final String NOTE_ALL = "Note: Pictures shown in 'All Departments' folder"
            + " must be forwarded from 'My Department' folder.";
    public static final String SEARCH_HINT = "Search By Scene No.";

    public ContinuityPage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────────── shell ─────────────────────────────

    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen("tabLayout").isEmpty() && tabTitle(true) != null) return true;
            pause(400);
        }
        return false;
    }

    /** Toolbar title. {@code headerText} is textAllCaps on some builds — compare ignoring case. */
    public String header() {
        return firstText("headerText");
    }

    /** The two tab names as UiAutomator reports them, left to right. */
    public List<String> tabNames() {
        List<String> out = new ArrayList<>();
        for (WebElement e : leftToRight(onScreen("tv_title"))) out.add(text(e));
        return out;
    }

    public boolean openMyDepartment() { return tapTab(true); }
    public boolean openAllDepartment() { return tapTab(false); }

    /** Whether the given tab is the selected one (TabView passes setSelected to its custom view). */
    public boolean isSelected(boolean my) {
        WebElement t = tabTitle(my);
        if (t == null) return false;
        try { return Boolean.parseBoolean(t.getAttribute("selected")); }
        catch (WebDriverException e) { return false; }
    }

    private boolean tapTab(boolean my) {
        WebElement t = tabTitle(my);
        if (t == null) return false;
        try {
            t.click();
        } catch (WebDriverException e) {
            return false;
        }
        pause(1500);
        waitProgressGone(Duration.ofSeconds(20));
        return true;
    }

    private WebElement tabTitle(boolean my) {
        for (WebElement e : onScreen("tv_title")) {
            String s = text(e);
            if (my && s.equals(TAB_MY)) return e;
            if (!my && s.contains(TAB_ALL_PART) && s.contains("Department")) return e;
        }
        return null;
    }

    /** Taps the info icon inside a tab. It opens a dialog INSTEAD of selecting the tab. */
    public boolean tapTabInfo(boolean my) {
        WebElement title = tabTitle(my);
        if (title == null) return false;
        // Two img_info exist, one per tab: take the one nearest this tab's title.
        WebElement best = null;
        int bestDx = Integer.MAX_VALUE;
        int tx = centreX(title);
        for (WebElement i : onScreen("img_info")) {
            int dx = Math.abs(centreX(i) - tx);
            if (dx < bestDx) { bestDx = dx; best = i; }
        }
        if (best == null) return false;
        try { best.click(); pause(1200); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** The top linear progress bar shared by both tabs — spins while an add is in flight. */
    public boolean waitProgressGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (onScreen("progress").isEmpty()) return true;
            pause(500);
        }
        return false;
    }

    // ───────────────────────────── banner ─────────────────────────────

    public boolean notesShown() { return !onScreen("notesHolder").isEmpty() || !onScreen("textNotes").isEmpty(); }

    public String notesText() { return firstText("textNotes"); }

    public boolean closeNotes() {
        List<WebElement> x = onScreen("ivCloseNotes");
        if (x.isEmpty()) return false;
        try { x.get(0).click(); pause(800); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ───────────────────────────── add ─────────────────────────────

    public boolean addShown() { return !onScreen("takeSceneShots").isEmpty(); }

    public boolean tapAdd() {
        List<WebElement> fab = onScreen("takeSceneShots");
        if (fab.isEmpty()) return false;
        try { fab.get(0).click(); pause(1200); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ───────────────────────────── search ─────────────────────────────

    private WebElement searchBox() {
        List<WebElement> boxes = onScreen("search_widget_edit_textvw");
        return boxes.isEmpty() ? null : boxes.get(0);
    }

    public String searchHint() {
        WebElement b = searchBox();
        if (b == null) return null;
        try {
            String h = b.getAttribute("hint");
            if (h != null && !h.isBlank()) return h.trim();
            // An empty EditText reports its hint as text on older UiAutomator servers.
            return text(b);
        } catch (WebDriverException e) { return null; }
    }

    public boolean search(String query) {
        WebElement b = searchBox();
        if (b == null) return false;
        try {
            b.click();
            b.clear();
            if (!query.isEmpty()) b.sendKeys(query);
        } catch (WebDriverException e) { return false; }
        // Never hideKeyboard(): on UiAutomator2 it is a BACK, and BACK here leaves the tool.
        pause(1200);
        return true;
    }

    public void clearSearch() { search(""); }

    // ───────────────────────────── rows ─────────────────────────────

    /** "Scene No-X" for every row on the visible tab, following the list past the fold. */
    public List<String> rowTexts() {
        return readWholeList("recDepartment", "textView",
                () -> {
                    List<String> out = new ArrayList<>();
                    for (WebElement e : onScreen("textView")) {
                        String s = text(e);
                        if (!s.isEmpty()) out.add(s);
                    }
                    return out;
                },
                s -> s);
    }

    /** The scene numbers listed, i.e. {@link #rowTexts()} without the hard-coded prefix. */
    public List<String> sceneNumbers() {
        List<String> out = new ArrayList<>();
        for (String r : rowTexts()) out.add(r.startsWith(ROW_PREFIX) ? r.substring(ROW_PREFIX.length()) : r);
        return out;
    }

    public boolean hasScene(String sceneNo) {
        return sceneNumbers().contains(sceneNo);
    }

    /** Taps a scene row, scrolling to it first. */
    public boolean openScene(String sceneNo) {
        String want = ROW_PREFIX + sceneNo;
        if (!scrollToRow("recDepartment", "textView", want)) return false;
        for (WebElement e : onScreen("textView")) {
            if (want.equals(text(e))) {
                try { e.click(); pause(2000); return true; }
                catch (WebDriverException ex) { return false; }
            }
        }
        return false;
    }

    /** Long-presses a scene row; returns the titles of whatever menu opened (may be empty). */
    public List<String> longPressScene(String sceneNo) {
        String want = ROW_PREFIX + sceneNo;
        if (!scrollToRow("recDepartment", "textView", want)) return null;
        for (WebElement e : onScreen("textView")) {
            if (want.equals(text(e))) {
                longPress(e);
                pause(1200);
                return popupMenuTitles();
            }
        }
        return null;
    }

    public boolean emptyShown() { return !onScreen("emptyView").isEmpty(); }
}
