package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * A folder's contents (FolderFragment inside {@code FolderActivity}) — pushed when a
 * folder row is opened from {@link DriveHomePage}.
 *
 * <p>Same shell as home (Sort / Filter / Select chips, view toggles, FAB) plus a
 * breadcrumb, and a FAB that gains TWO extra actions here: Request Files and View
 * File Requests.
 *
 * <p>Note the header text is UPPER-CASED for display — a folder created as
 * {@code QA_Drive_Folder_01} renders as {@code QA_DRIVE_FOLDER_01} — so
 * {@link #headerMatches(String)} compares case-insensitively.
 */
public class DriveFolderPage extends BasePage {

    public DriveFolderPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("drive_folder_nav_host", timeout)
                || isVisible("headerText", Duration.ofSeconds(3))
                || isVisible("fabAdd", Duration.ofSeconds(3));
    }

    public boolean isFolderActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("folder.FolderActivity");
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- header / breadcrumb ----------------

    public String header() {
        String h = text("headerText");
        return h.isBlank() ? text("tvTitle") : h;
    }

    /** The folder title is upper-cased on screen, so never compare case-sensitively. */
    public boolean headerMatches(String expected) {
        return header().equalsIgnoreCase(expected);
    }

    public boolean hasBreadcrumb() {
        return isVisible("breadcrumbContainer", Duration.ofSeconds(3))
                || isVisible("breadcrumbScroll", Duration.ofSeconds(2));
    }

    public List<String> breadcrumbCrumbs() {
        try {
            return driver.findElements(byId("breadcrumbContainer")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    // ---------------- content ----------------

    public boolean isEmptyStateShown() {
        return isVisible("emptyState", Duration.ofSeconds(4))
                || isVisible("tvEmptyTitle", Duration.ofSeconds(2));
    }

    public String emptyTitle()    { return text("tvEmptyTitle"); }
    public String emptySubtitle() { return text("tvEmptySubtitle"); }

    public List<String> itemNames() {
        try {
            return driver.findElements(byId("tvName")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * The FULL contents, scroll-collected. {@link #itemNames()} returns only the rows
     * the RecyclerView has rendered, so an item below the fold is invisible to it.
     */
    public List<String> allItemNamesScrolled() {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(itemNames());
        int stable = 0;
        for (int i = 0; i < 25 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(itemNames());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 10; i++) swipe(0.35, 0.75);   // back to the top
        return new java.util.ArrayList<>(seen);
    }

    /**
     * Waits for an item to appear in this folder.
     *
     * <p>Creating a folder is a server round-trip followed by a list reload, so a fixed
     * sleep then a single rendered-rows read is two ways to miss it at once: too early,
     * and too shallow. Retries with a refresh, scroll-collecting each time.
     */
    public boolean waitForItem(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
            refresh();
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    /** Pull-to-refresh on the folder list. */
    public boolean refresh() {
        try {
            driver.findElement(byId("swipeRefresh"));
            swipe(0.35, 0.75);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void swipe(double fromY, double toY) {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * Math.min(fromY, toY)),
                    "width", 200,
                    "height", (int) (size.height * Math.abs(fromY - toY)),
                    "direction", fromY > toY ? "up" : "down",
                    "percent", 0.8));
            Thread.sleep(600);
        } catch (Exception ignored) { }
    }

    // ---------------- FAB (folder-scoped) ----------------

    public boolean openFab() {
        return click("fabAdd") && isVisible("menuCreateFolder", Duration.ofSeconds(6));
    }

    public boolean fabHasRequestFiles()     { return isVisible("menuRequestFiles", Duration.ofSeconds(3)); }
    public boolean fabHasViewFileRequests() { return isVisible("menuViewFileRequests", Duration.ofSeconds(2)); }
    public boolean fabHasCreateFolder()     { return isVisible("menuCreateFolder", Duration.ofSeconds(2)); }
    public boolean fabHasUploadFile()       { return isVisible("menuUploadFile", Duration.ofSeconds(2)); }
    public boolean fabHasUploadFolder()     { return isVisible("menuUploadFolder", Duration.ofSeconds(2)); }

    public boolean tapCreateFolder()     { return click("menuCreateFolder"); }
    public boolean tapUploadFile()       { return click("menuUploadFile"); }
    public boolean tapUploadFolder()     { return click("menuUploadFolder"); }
    /** Folder-scoped: opens the Request Files sheet with THIS folder as the destination. */
    public boolean tapRequestFiles()     { return click("menuRequestFiles"); }
    public boolean tapViewFileRequests() { return click("menuViewFileRequests"); }

    // ---------------- chips ----------------

    public boolean hasSortChip()   { return isVisible("chipSort", Duration.ofSeconds(3)); }
    public boolean hasFilterChip() { return isVisible("chipFilter", Duration.ofSeconds(3)); }
    public boolean hasSelectChip() { return isVisible("chipSelect", Duration.ofSeconds(3)); }

    public boolean back() {
        try { driver.navigate().back(); Thread.sleep(1200); return true; }
        catch (Exception e) { return false; }
    }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }
}
