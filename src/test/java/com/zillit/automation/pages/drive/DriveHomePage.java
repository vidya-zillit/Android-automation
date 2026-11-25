package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DRIVE home screen (HomeFragment / fragment_drive_home.xml) inside
 * {@code DriverMainActivity} — reached from Tools → ADMIN → Drive.
 *
 * <p>Section tabs (My Drive / Shared with me), kind tabs (Folders / Files), the
 * Sort / Filter / Select chips, the favourites + list-grid toggles, the "Show"
 * filter spinner, the storage-quota bar, the item list, the bulk action bar and
 * the add FAB (Create Folder / Upload File / Upload Folder).
 */
public class DriveHomePage extends BasePage {

    public DriveHomePage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("fabAdd", timeout)
                || isVisible("tabSection", Duration.ofSeconds(3))
                || isVisible("recyclerView", Duration.ofSeconds(3))
                || isVisible("emptyState", Duration.ofSeconds(3));
    }

    /**
     * True only on Drive HOME — not Trash, Search, Favorites or any other destination
     * inside the same activity.
     *
     * <p>{@link #isLoaded} is deliberately loose (it accepts a bare {@code recyclerView})
     * and therefore ALSO matches the Trash screen, which shares that id. A recovery
     * routine built on isLoaded() concludes "already home" while sitting in Trash, so the
     * next test operates on the wrong list — that is exactly how two cases silently
     * skipped and a cleanup step listed Trash contents as though they were folders.
     * The section tabs exist only on home, so they are the reliable marker.
     */
    public boolean isHome(Duration timeout) {
        if (isVisible("tabSection", timeout)) return true;
        // The fabAdd fallback needs the activity guard: a FOLDER screen has fabAdd too,
        // so without it "am I home?" answers yes from inside a folder — which is how a
        // cleanup step ran its delete against the open folder's (empty) contents,
        // reported success, and left the folder behind.
        return isDriveActivity() && isVisible("fabAdd", Duration.ofSeconds(2));
    }

    /** Leaves Trash/Search/etc and returns to Drive home. */
    public boolean returnHome() {
        for (int i = 0; i < 5; i++) {
            if (isHome(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception e) { return false; }
        }
        return isHome(Duration.ofSeconds(3));
    }

    /**
     * Drive lives in its own activity, so the activity name is the one locator that
     * cannot be faked by a similar-looking screen. Also guards against a routing
     * regression back to the LEGACY zillit_drive module.
     */
    public boolean isDriveActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("drive.presentation.DriverMainActivity");
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- shell ----------------

    public boolean hasSectionTabs()  { return isVisible("tabSection", Duration.ofSeconds(5)); }
    public boolean hasKindTabs()     { return isVisible("tabKind", Duration.ofSeconds(3)); }
    public boolean hasSortChip()     { return isVisible("chipSort", Duration.ofSeconds(3)); }
    public boolean hasFilterChip()   { return isVisible("chipFilter", Duration.ofSeconds(3)); }
    public boolean hasSelectChip()   { return isVisible("chipSelect", Duration.ofSeconds(3)); }
    public boolean hasFab()          { return isVisible("fabAdd", Duration.ofSeconds(3)); }
    public boolean hasViewToggle()   { return isVisible("btnViewToggle", Duration.ofSeconds(3)); }
    public boolean hasFavToggle()    { return isVisible("btnFavoritesToggle", Duration.ofSeconds(3)); }
    public boolean hasShowFilter()   { return isVisible("spMyDriveFilter", Duration.ofSeconds(3)); }
    public boolean hasStorageBar()   { return isVisible("storageProgressBar", Duration.ofSeconds(3)); }

    public String storageUsage() { return textOf("tvStorageUsage"); }

    public boolean isEmptyStateShown() {
        return isVisible("emptyState", Duration.ofSeconds(4))
                || isVisible("tvEmptyTitle", Duration.ofSeconds(2));
    }

    public String emptyTitle()    { return textOf("tvEmptyTitle"); }
    public String emptySubtitle() { return textOf("tvEmptySubtitle"); }

    // ---------------- items ----------------

    /** Rendered rows only — see {@link #allItemNamesScrolled()} for the full set. */
    public List<String> itemNames() {
        try {
            return driver.findElements(byId("tvName")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    public int itemCount() { return itemNames().size(); }

    /**
     * The list is a RecyclerView, so only rendered rows exist in the tree. Scroll and
     * collect until the set stops growing — asserting on one screenful is how you get
     * a false "the item is missing" failure.
     */
    public List<String> allItemNamesScrolled() {
        // A slow swipe on a loaded emulator can register as a long-press and switch Select mode
        // on — rows then lose their names and btnMore. Never read the list in that state.
        exitSelectMode();
        Set<String> seen = new LinkedHashSet<>(itemNames());
        int stable = 0;
        for (int i = 0; i < 30 && stable < 2; i++) {
            int before = seen.size();
            swipeUp();
            if (!driver.findElements(byId("btnBulkCancel")).isEmpty()) exitSelectMode();
            seen.addAll(itemNames());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        scrollToTop();
        return new ArrayList<>(seen);
    }

    public boolean hasItem(String name) {
        return allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    /** Opens an item by name, scrolling to reach it if it is below the fold. */
    public boolean openItem(String name) {
        exitSelectMode();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 25; i++) {
                if (tapRowByName(name)) return true;
                swipeUp();
                if (!driver.findElements(byId("btnBulkCancel")).isEmpty()) exitSelectMode();
            }
            scrollToTop();
        }
        return false;
    }

    private boolean tapRowByName(String name) {
        try {
            for (WebElement e : driver.findElements(byId("tvName"))) {
                String t = e.getText();
                if (t != null && t.equalsIgnoreCase(name)) { e.click(); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Opens the row overflow (⋮) for the first row whose name matches. */
    public boolean openItemMenu(String name) {
        // Scroll to the row first, like openItem(): a menu for a row below the fold was a skip.
        exitSelectMode();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 25; i++) {
                if (tapMenuOfVisibleRow(name)) return true;
                swipeUp();
                if (!driver.findElements(byId("btnBulkCancel")).isEmpty()) exitSelectMode();
            }
            scrollToTop();
        }
        return false;
    }

    private boolean tapMenuOfVisibleRow(String name) {
        try {
            List<WebElement> names = driver.findElements(byId("tvName"));
            List<WebElement> menus = driver.findElements(byId("btnMore"));
            for (int i = 0; i < names.size() && i < menus.size(); i++) {
                String t = names.get(i).getText();
                if (t != null && t.equalsIgnoreCase(name)) { menus.get(i).click(); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }


    // ---------------- tabs / chips ----------------

    public boolean selectSection(String label) { return tapText(label); }  // My Drive / Shared with me
    public boolean selectKind(String label)    { return tapText(label); }  // Folders / Files

    /**
     * Switches to the Files tab. Drive opens on <b>Folders</b>, so a freshly uploaded
     * FILE is invisible until you come here — the list keeps saying "No folders yet"
     * and the upload looks like it failed when it actually succeeded.
     */
    public boolean showFiles() {
        boolean t = tapText("Files");
        try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return t;
    }

    public boolean showFolders() {
        boolean t = tapText("Folders");
        try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return t;
    }

    /** Switches to "Shared with me" — items other users granted access to. */
    public boolean showSharedWithMe() {
        boolean t = tapText("Shared with me");
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return t;
    }

    public boolean showMyDrive() {
        boolean t = tapText("My Drive");
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return t;
    }

    /** Looks for a FILE, switching to the Files tab first. */
    public boolean hasFile(String name) {
        showFiles();
        return allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    public boolean openSort() {
        return click("chipSort") && (textShown("Sort by", Duration.ofSeconds(5)));
    }

    public boolean openFilter() {
        return click("chipFilter") && textShown("Filter", Duration.ofSeconds(5));
    }

    public boolean enterSelectMode() {
        return click("chipSelect") && isVisible("bulkActionBar", Duration.ofSeconds(5));
    }

    /**
     * Leaves Select mode if a long-press (or a stopped case) left it on: the FAB is hidden
     * while selecting, so every "+" action after it fails with "the + sheet did not open".
     */
    public void exitSelectMode() {
        try {
            if (!driver.findElements(byId("btnBulkCancel")).isEmpty()) {
                driver.findElement(byId("btnBulkCancel")).click();
                Thread.sleep(1000);
            }
        } catch (Exception ignored) { }
    }

    /** Long-presses a visible row by name: Select mode starts with that row ticked. */
    public boolean longPressRow(String name) {
        try {
            for (WebElement e : driver.findElements(byId("tvName"))) {
                if (name.equalsIgnoreCase(e.getText())) {
                    driver.executeScript("mobile: longClickGesture", java.util.Map.of(
                            "elementId", ((org.openqa.selenium.remote.RemoteWebElement) e).getId(), "duration", 1200));
                    Thread.sleep(1500);
                    return isVisible("bulkActionBar", Duration.ofSeconds(4));
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean isSelectModeOn()  { return isVisible("bulkActionBar", Duration.ofSeconds(3)); }
    public String  selectedCount()   { return textOf("tvSelectedCount"); }
    public boolean bulkCancel()      { return click("btnBulkCancel"); }
    public boolean bulkDelete()      { return click("btnBulkDelete"); }
    public boolean bulkMove()        { return click("btnBulkMove"); }
    public boolean bulkDownload()    { return click("btnBulkDownload"); }

    public boolean toggleView()      { return click("btnViewToggle"); }
    public boolean toggleFavorites() { return click("btnFavoritesToggle"); }

    public boolean pullToRefresh() {
        try {
            driver.findElement(byId("swipeRefresh"));
            swipeDown();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- FAB ----------------

    public boolean openFab() {
        exitSelectMode();
        return click("fabAdd")
                && (isVisible("menuCreateFolder", Duration.ofSeconds(6))
                 || isVisible("menuUploadFile", Duration.ofSeconds(2)));
    }

    public boolean fabHasCreateFolder()     { return isVisible("menuCreateFolder", Duration.ofSeconds(3)); }
    public boolean fabHasUploadFile()       { return isVisible("menuUploadFile", Duration.ofSeconds(2)); }
    public boolean fabHasUploadFolder()     { return isVisible("menuUploadFolder", Duration.ofSeconds(2)); }
    /**
     * The + sheet is a NestedScrollView since the New group (3ab6a41d0) was added above these
     * rows, so on a phone they can sit below the fold — scroll before calling them absent.
     */
    public boolean fabHasRequestFiles()     { return visibleInSheet("menuRequestFiles"); }
    public boolean fabHasViewFileRequests() { return visibleInSheet("menuViewFileRequests"); }

    private boolean visibleInSheet(String id) {
        if (isVisible(id, Duration.ofSeconds(2))) return true;
        scrollToId(id);
        return isVisible(id, Duration.ofSeconds(1));
    }

    public boolean tapCreateFolder()        { return click("menuCreateFolder"); }
    public boolean tapUploadFile()          { return click("menuUploadFile"); }
    public boolean tapUploadFolder()        { return click("menuUploadFolder"); }
    public boolean tapRequestFiles()        { visibleInSheet("menuRequestFiles"); return click("menuRequestFiles"); }
    public boolean tapViewFileRequests()    { visibleInSheet("menuViewFileRequests"); return click("menuViewFileRequests"); }

    // ---------------- "New" blank file group (3ab6a41d0) ----------------

    /** Row ids of the New group, in sheet order, with the label each one shows. */
    public static final java.util.LinkedHashMap<String, String> NEW_ROWS = new java.util.LinkedHashMap<>();
    static {
        NEW_ROWS.put("menuNewDocument", "Document");
        NEW_ROWS.put("menuNewSpreadsheet", "Spreadsheet");
        NEW_ROWS.put("menuNewPresentation", "Presentation");
        NEW_ROWS.put("menuNewMarkdown", "Markdown file");
        NEW_ROWS.put("menuNewText", "Text file");
    }

    /**
     * True when the + sheet carries the New group. The sheet is a NestedScrollView now, so the
     * group can sit below the fold on a small phone — scroll it into view before deciding.
     */
    public boolean fabHasNewGroup() {
        if (isVisible("menuNewGroup", Duration.ofSeconds(2)) || isVisible("menuNewHeader", Duration.ofSeconds(1))) return true;
        scrollToId("menuNewHeader");
        return isVisible("menuNewHeader", Duration.ofSeconds(2)) || isVisible("menuNewDocument", Duration.ofSeconds(1));
    }

    /** id → shown text for every New row that is present (scrolling to reach them). */
    public java.util.LinkedHashMap<String, String> newRowsShown() {
        java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<>();
        for (String id : NEW_ROWS.keySet()) {
            if (!isVisible(id, Duration.ofSeconds(1))) scrollToId(id);
            String t = textIn(id);
            if (t != null) out.put(id, t);
        }
        return out;
    }

    /** Text of a New row: the row may be a LinearLayout with a TextView inside, or a TextView. */
    private String textIn(String id) {
        try {
            WebElement row = driver.findElement(byId(id));
            String t = row.getText();
            if (t != null && !t.isBlank()) return t;
            for (WebElement tv : row.findElements(org.openqa.selenium.By.className("android.widget.TextView"))) {
                String s = tv.getText();
                if (s != null && !s.isBlank()) return s;
            }
            return "";
        } catch (WebDriverException e) { return null; }
    }

    public boolean tapNew(String rowId) {
        if (!isVisible(rowId, Duration.ofSeconds(1))) scrollToId(rowId);
        return click(rowId);
    }

    /** The "Name" dialog after a New row: its single EditText (no id). */
    public WebElement newNameField() {
        try {
            for (WebElement e : driver.findElements(org.openqa.selenium.By.className("android.widget.EditText"))) {
                if (e.isDisplayed()) return e;
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    public boolean newNameDialogShown(Duration timeout) {
        return textShown("Name", timeout) && newNameField() != null;
    }

    /** Sets the name box (empty string = cleared) and taps Create or Cancel. */
    public boolean answerNewNameDialog(String name, boolean create) {
        WebElement f = newNameField();
        if (f == null) return false;
        try {
            if (name != null) { f.clear(); if (!name.isEmpty()) f.sendKeys(name); }
            return tapButtonText(create ? "Create" : "Cancel");
        } catch (WebDriverException e) { return false; }
    }

    private boolean tapButtonText(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").textMatches(\"(?i)" + text + "\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- Show filter (782476d83) ----------------

    /** Opens the Show spinner, reads its options, closes it again. */
    public List<String> showFilterOptions() {
        List<String> out = new ArrayList<>();
        if (!click("spMyDriveFilter")) return out;
        try {
            Thread.sleep(1200);
            for (WebElement e : driver.findElements(AppiumBy.id("android:id/text1"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
            driver.navigate().back();
            Thread.sleep(600);
        } catch (Exception ignored) { }
        return out;
    }

    public boolean chooseShowFilter(String option) {
        if (!click("spMyDriveFilter")) return false;
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        boolean ok = tapText(option);
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return ok;
    }

    public String showFilterHint() { return textOf("tvMyDriveFilterHint"); }

    /** The Show spinner's current value ("All", "Large files", …), or "" when absent. */
    public String showFilterValue() {
        try {
            WebElement sp = driver.findElement(byId("spMyDriveFilter"));
            for (WebElement t : sp.findElements(org.openqa.selenium.By.className("android.widget.TextView"))) {
                String v = t.getText();
                if (v != null && !v.isBlank()) return v;
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    public boolean openDrawer() {
        return click("btnDrawer") && isVisible("navTrash", Duration.ofSeconds(6));
    }

    /**
     * Confirms Drive's delete dialog, whose positive button is <b>"Delete"</b> — not the
     * "Yes"/"No" pair the rest of the app uses, so the shared {@code confirmYesDialog}
     * helper does NOT dismiss it.
     */
    public boolean confirmDeleteDialog(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/button1\")")).click();
                return true;
            } catch (WebDriverException ignored) { }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().className(\"android.widget.Button\").textMatches(\"(?i)delete\")")).click();
                return true;
            } catch (WebDriverException ignored) { }
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    /**
     * True when the list can actually be READ — i.e. the empty state is showing, or at
     * least one row is rendered.
     *
     * <p>Guards the false-pass that hid a broken delete: a dialog left open covers the
     * list, {@code tvName} then matches nothing, and an "item is absent" assertion
     * passes against an empty list even though the item is still there. Always confirm
     * the list is readable BEFORE asserting that something is missing from it.
     */
    public boolean isListReadable() {
        // The list is network-backed (the Show filters are network-only); give a slow response
        // up to ~12 s before calling the list unreadable.
        long deadline = System.currentTimeMillis() + 12000;
        do {
            if (itemCount() > 0 || isEmptyStateShown()) return true;
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    /** True when no modal dialog is on top of the list. */
    public boolean isDialogOpen() {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button1\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- helpers ----------------

    String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    boolean tapText(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    void swipeUp()   { swipe(0.75, 0.35); }
    void swipeDown() { swipe(0.35, 0.75); }

    private void swipe(double fromY, double toY) {
        try {
            var size = driver.manage().window().getSize();
            int x = size.width / 2;
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", x - 100, "top", (int) (size.height * Math.min(fromY, toY)),
                    "width", 200, "height", (int) (size.height * Math.abs(fromY - toY)),
                    "direction", fromY > toY ? "up" : "down", "percent", 0.8, "speed", 6000));
            Thread.sleep(600);
        } catch (Exception ignored) { }
    }

    void scrollToTop() {
        for (int i = 0; i < 12; i++) swipeDown();
    }
}
