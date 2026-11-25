package com.zillit.automation.pages.scheduledod;

import com.zillit.automation.pages.scheduledist.ScheduleDistPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools → "Schedule D.O.D." (identifier {@code dod_tool}): {@code DodActivity}, a list of
 * NAME folders ({@code item_script_folder}), each opening {@code DodDetailActivity}, a list of
 * files ({@code documents_script_item}).
 *
 * <p>It is the Pages tab of Schedule Distribution without the tabs, so the folder and row
 * readers are inherited from {@link ScheduleDistPage}. What differs, read from the source
 * (see {@code SCHEDULE_DOD_TESTCASES.md}):
 * <ul>
 *   <li><b>No search on either screen.</b> Both layouts declare {@code searchHolder} as
 *       {@code gone} and nothing makes it visible, although {@code DodActivity} still wires a
 *       colour search to it.</li>
 *   <li><b>A folder screen has no upload and no History</b> ({@code addFb} / {@code userHistoryBtn}
 *       hidden in {@code initView}).</li>
 *   <li><b>A folder card has no ⋮</b> ({@code imgMore} is {@code gone}); every file action is on
 *       the file card inside the folder.</li>
 *   <li>A file row's name line is {@code tv_name} = "Name: &lt;name&gt;"; uploader
 *       {@code tv_created_by}, upload time {@code tv_date}.</li>
 *   <li>View Count / Download Count are their own activity ({@code UsersCountListPage}) with a
 *       local name search, {@code countRecyclerView} and a {@code noDataFound} empty view.</li>
 *   <li>History is {@code UserHistoryFolderPage}: deleted FOLDERS, titled "&lt;tool&gt; History",
 *       no search.</li>
 * </ul>
 */
public class ScheduleDodPage extends ScheduleDistPage {

    /** Server-supplied tile labels seen for this tool (UiSelector regexes, full match). */
    public static final List<String> TILE_LABELS = List.of("Schedule D.O.D.?", "Schedule DOD", "D.O.D.?");
    public static final String UPLOAD_LABEL = "Upload D.O.D.";
    public static final String HISTORY_LABEL = "History";

    public static final String VIEW = "View";
    public static final String DOWNLOAD = "Download";
    public static final String PUBLISH = "Publish to Doc Distribution";
    public static final String MOVE = "Move";
    public static final String REPLACE = "Replace";
    public static final String DOWNLOAD_COUNT = "Download Count";
    public static final String VIEW_COUNT = "View Count";
    public static final String DELETE = "Delete";

    public ScheduleDodPage(AndroidDriver driver) {
        super(driver);
    }

    // ─────────────────────────── the folder list ───────────────────────────

    /** The folder list: the only DOD screen with the History button. */
    @Override
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen("userHistoryBtn").isEmpty() && !elementsById("recDocument").isEmpty()) return true;
            pause(400);
        }
        return false;
    }

    /** List settled: rows on screen or the empty view, and no progress bar. */
    public boolean waitListShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            boolean busy = !onScreen("progress").isEmpty() || !onScreen("circularProgress").isEmpty();
            if (!busy && (!onScreen("layMainHolder").isEmpty() || !onScreen("layMainCard").isEmpty() || emptyShown())) {
                return true;
            }
            pause(500);
        }
        return false;
    }

    public boolean searchOffered() { return !onScreen("search_widget_edit_textvw").isEmpty(); }

    /** The folder card's ⋮ — declared {@code gone} in {@code item_script_folder}. */
    public boolean folderMenuShown() { return !onScreen("imgMore").isEmpty() && onScreen("layMainCard").isEmpty(); }

    /** Every folder whose title contains {@code part}, case-insensitive. */
    public List<Folder> foldersContaining(String part) {
        List<Folder> out = new ArrayList<>();
        String p = part.toLowerCase();
        for (Folder f : allFolders()) if (f.title.toLowerCase().contains(p)) out.add(f);
        return out;
    }

    /** Re-reads the list (leave + reopen is done by the test) until {@code title} is listed. */
    public Folder folder(String title) {
        for (Folder f : allFolders()) if (title.equals(f.title)) return f;
        return null;
    }

    // ─────────────────────────── inside a folder ───────────────────────────

    /** A file card's background, sampled in its top band (above the text scrim), as {r,g,b}. */
    public int[] rowColour(java.util.function.Predicate<Row> match) {
        for (WebElement r : onScreen("layMainCard")) {
            Row row = readRowPublic(r);
            if (row == null || !match.test(row)) continue;
            try {
                List<WebElement> holder = r.findElements(byId("relativeLayoutHolder"));
                WebElement target = holder.isEmpty() ? r : holder.get(0);
                byte[] png = target.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
                if (bi == null) return null;
                // The lower half of the card is a dark text scrim; the colour shows above it.
                int p = bi.getRGB(bi.getWidth() * 17 / 20, Math.max(2, bi.getHeight() / 10));
                return new int[]{(p >> 16) & 0xff, (p >> 8) & 0xff, p & 0xff};
            } catch (Exception e) { return null; }
        }
        return null;
    }

    /** The row a card shows (ScheduleDistPage's reader is private; matched on date + name). */
    private Row readRowPublic(WebElement root) {
        String date = childText(root, "tv_date");
        String name = childText(root, "tv_name");
        if (date == null) return null;
        for (Row r : visibleRows()) {
            if (date.equals(r.date) && java.util.Objects.equals(name, r.name)) return r;
        }
        return null;
    }

    /** The "Choose Folder" sheet Move opens: its row texts. */
    public List<String> sheetTexts() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(By.className("android.widget.TextView"))) {
                String t = text(e);
                if (!t.isEmpty()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean tapText(String exact) {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + exact.replace("\"", "\\\"") + "\")"));
            if (e.isEmpty()) return false;
            e.get(0).click();
            pause(1200);
            return true;
        } catch (WebDriverException ex) { return false; }
    }

    // ─────────────────────────── View / Download Count ───────────────────────────

    public boolean countScreenShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!elementsById("countRecyclerView").isEmpty() || !elementsById("noDataFound").isEmpty()) {
                if (onScreen("progress").isEmpty() || !onScreen("countRecyclerView").isEmpty()) return true;
            }
            pause(400);
        }
        return false;
    }

    public String countSearchText() {
        List<WebElement> in = onScreen("search_widget_edit_textvw");
        if (in.isEmpty()) return null;
        String t = text(in.get(0));
        try {
            String hint = in.get(0).getAttribute("hint");
            if (hint != null && t.equals(hint.trim())) return "";
        } catch (WebDriverException ignored) { }
        return t;
    }

    public String countSearchHint() {
        List<WebElement> in = onScreen("search_widget_edit_textvw");
        if (in.isEmpty()) return null;
        try {
            String h = in.get(0).getAttribute("hint");
            return h == null || h.isBlank() ? text(in.get(0)) : h.trim();
        } catch (WebDriverException e) { return null; }
    }

    public boolean countSearch(String query) {
        List<WebElement> in = onScreen("search_widget_edit_textvw");
        if (in.isEmpty()) return false;
        try {
            in.get(0).click();
            in.get(0).clear();
            if (!query.isEmpty()) in.get(0).sendKeys(query);
        } catch (WebDriverException e) { return false; }
        pause(1500);
        hideKeyboardIfShown();
        return true;
    }

    /** The texts of the count list's rows (empty when "No Data found" shows instead). */
    public List<String> countRowTexts() {
        List<String> out = new ArrayList<>();
        List<WebElement> rec = onScreen("countRecyclerView");
        if (rec.isEmpty()) return out;
        try {
            for (WebElement t : rec.get(0).findElements(By.className("android.widget.TextView"))) {
                String s = text(t);
                if (!s.isEmpty()) out.add(s);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean countEmptyShown() { return !onScreen("noDataFound").isEmpty(); }
}
