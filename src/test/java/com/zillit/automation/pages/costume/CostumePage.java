package com.zillit.automation.pages.costume;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tools &rarr; <b>Costume (Main Cast)</b> / <b>Costume (Background Cast)</b> — the app's
 * "Wardrobe" tools ({@code WARDROBE_MAIN_TOOL} / {@code WARDROBE_BACKGROUND_TOOL}), both
 * opening {@code bottomNav/tools/wardrobe/WardrobePage}. Not "Costumes &amp; Set Sync".
 *
 * <p>The screens, all with the same list ids:
 * <pre>
 *   WardrobePage                  tabLayout (Selects / Shortlist / Final), search, chatIconHolder,
 *                                 recFolder of location_folder_item rows (tvName, tvother, ivArrow),
 *                                 FABs takeSceneShots ("+") and uploadFromCam (camera)
 *   WardrobeCharacterListingPage  (TV projects) the characters inside one "Episode No : n" row
 *   WardrobeSceneNoPage           the scenes of one character when it has more than one
 *   WardrobeListingPage           the costume records: recCast of library_item (sceneNo text,
 *                                 descText, imgMore, icChat), toolbar "Select" → deleteMoveRl bar
 * </pre>
 * On a TELEVISION project (this one) the tabs group by EPISODE by default
 * ({@code WardrobeVM.groupBy}), so a record is reached as
 * {@code "Episode No : 77" → "Character: Zl_auto_cos 1" → record}.
 *
 * <p>Row labels come from {@code WardrobeVM.doFolderGrouping}: {@code "Episode No : %s"},
 * {@code "Character: %s"}, {@code "Scene No. %s"}; the detail line ({@code tvother}) carries
 * {@code "Talent Name: …\nScene No. …\nGender: …"}.
 */
public class CostumePage extends BasePage {

    public static final String TAB_SELECTS = "Selects";
    public static final String TAB_SHORTLIST = "Shortlist";
    public static final String TAB_FINAL = "Final";
    public static final List<String> TABS = List.of(TAB_SELECTS, TAB_SHORTLIST, TAB_FINAL);

    /** {@code R.string.casting_search_hint} — the first page's search. */
    public static final String SEARCH_HINT = "Search by Talent Name/Character";
    /** {@code R.string.wardrobe_sc_search_hint} — the character / scene pages' search. */
    public static final String SEARCH_HINT_INNER = "Search by Talent Name/Character/Scene No.";
    public static final String MSG_EMPTY_SEARCH = "Please Enter something for search";
    public static final String MSG_MOVE_NEXT_PAGE = "To move this folder, please navigate to the next page.";
    public static final String MSG_DELETE_NEXT_PAGE = "To delete this folder, please navigate to the next page.";
    public static final String MSG_DELETE_CONFIRM = "Are you sure you want to delete?";
    public static final String MSG_NONE_CHOSEN = "Please choose at least one user from the list.";
    public static final String MSG_KEEP_ONE = "Please keep at least one item from list.";
    public static final String MSG_ATTACHMENT = "Costume Attachment Required.";

    public CostumePage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────── where am I ─────────────────────────

    /** WardrobePage: the tab strip plus the folder list. */
    public boolean isLoaded(Duration timeout) {
        return isVisible("tabLayout", timeout) && !driver.findElements(byId("recFolder")).isEmpty();
    }

    /** Character / scene page: a folder list without the tab strip. */
    public boolean onInnerFolderPage(Duration timeout) {
        return isVisible("recFolder", timeout) && driver.findElements(byId("tabLayout")).isEmpty();
    }

    /** WardrobeListingPage: the record list ({@code recCast}). */
    public boolean onRecordList(Duration timeout) {
        return isVisible("recCast", timeout);
    }

    public String headerText() {
        try { return driver.findElement(byId("headerText")).getText(); }
        catch (Exception e) { return ""; }
    }

    // ───────────────────────── tabs ─────────────────────────

    public List<String> tabLabels() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement t : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/tabLayout']//android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank() && !s.trim().matches("\\d+")) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean selectTab(String label) {
        try {
            List<WebElement> tabs = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/tabLayout']//android.widget.TextView[@text='" + label + "']"));
            if (tabs.isEmpty()) return false;
            tabs.get(0).click();
            pause(2500);
            return true;
        } catch (Exception e) { return false; }
    }

    // ───────────────────────── toolbar / fabs ─────────────────────────

    public boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); } catch (Exception e) { return false; }
    }

    public boolean shown(String id, Duration timeout) { return isVisible(id, timeout); }

    /** The filter icon ({@code settingImage}, set to baseline_filter_list_24). */
    public boolean openFilter() {
        if (!tap("settingImage")) return false;
        return isVisible("recFilterBy", Duration.ofSeconds(6));
    }

    public boolean tapAdd() { return tap("takeSceneShots"); }

    /** Unit chat icon on WardrobePage — hidden on Final ({@code handleUnitInfo}). */
    public boolean unitChatShown() { return isVisible("chatIconHolder", Duration.ofSeconds(3)); }

    public boolean openUnitChat() {
        if (!tap("chatIconHolder")) return false;
        pause(3000);
        return isVisible("rvChat", Duration.ofSeconds(15)) || isVisible("keyboardLayout", Duration.ofSeconds(3));
    }

    // ───────────────────────── search ─────────────────────────

    public String searchHint() {
        try { return driver.findElement(byId("search_widget_edit_textvw")).getText(); }
        catch (Exception e) { return ""; }
    }

    /** Types the term and presses the search icon (the app searches on IME_ACTION_SEARCH or the icon). */
    public void search(String term) {
        try {
            WebElement e = driver.findElement(byId("search_widget_edit_textvw"));
            e.click();
            e.clear();
            if (term != null && !term.isEmpty()) e.sendKeys(term);
            hideKeyboardIfShown();
            tap("imageView6");
            pause(2500);
        } catch (Exception ignored) { }
    }

    /** Clears an applied search through the same icon (it toggles to a close icon). */
    public void clearSearch() {
        try {
            WebElement e = driver.findElement(byId("search_widget_edit_textvw"));
            String t = e.getText();
            if (t != null && !t.isEmpty() && !t.startsWith("Search by")) {
                tap("imageView6");
                pause(2000);
            }
        } catch (Exception ignored) { }
    }

    /** Taps the search icon with the field empty. */
    public void tapSearchIconEmpty() {
        try { driver.findElement(byId("search_widget_edit_textvw")).clear(); } catch (Exception ignored) { }
        hideKeyboardIfShown();
        tap("imageView6");
    }

    // ───────────────────────── folder rows ─────────────────────────

    /** Labels ({@code tvName}) of the folder rows on screen. */
    public List<String> folderNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/recFolder']//*[@resource-id='" + appPackage() + ":id/tvName']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** The row container whose label equals {@code label} (case-insensitive), scrolling to it. */
    public WebElement folderRow(String label) {
        for (int i = 0; i < 6; i++) {
            WebElement r = folderRowOnScreen(label);
            if (r != null) return r;
            if (!scrollFolders()) break;
        }
        return null;
    }

    private WebElement folderRowOnScreen(String label) {
        String want = label.trim().toLowerCase();
        try {
            for (WebElement row : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/recFolder']/*"))) {
                List<WebElement> n = row.findElements(byId("tvName"));
                if (n.isEmpty()) continue;
                String t = n.get(0).getText();
                if (t != null && t.trim().toLowerCase().equals(want)) return row;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private boolean scrollFolders() {
        try {
            WebElement list = driver.findElement(byId("recFolder"));
            Object more = driver.executeScript("mobile: scrollGesture", Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) list).getId(),
                    "direction", "down", "percent", 0.8));
            pause(600);
            return Boolean.TRUE.equals(more);
        } catch (Exception e) { return false; }
    }

    public boolean hasFolder(String label) { return folderRow(label) != null; }

    public boolean waitFolder(String label, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (folderRowOnScreen(label) != null || folderRow(label) != null) return true;
            pause(2000);
        }
        return false;
    }

    /** The detail line ({@code tvother}) of a folder row, or "". */
    public String folderDetail(String label) {
        WebElement row = folderRow(label);
        if (row == null) return "";
        try {
            List<WebElement> o = row.findElements(byId("tvother"));
            return o.isEmpty() ? "" : o.get(0).getText();
        } catch (Exception e) { return ""; }
    }

    public boolean openFolder(String label) {
        WebElement row = folderRow(label);
        if (row == null) return false;
        try {
            row.findElement(byId("tvName")).click();
            pause(3000);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Opens the row's overflow ({@code ivArrow}) and returns the popup's items; leaves it OPEN. */
    public List<String> openFolderMenu(String label) {
        List<String> out = new ArrayList<>();
        WebElement row = folderRow(label);
        if (row == null) return out;
        try {
            row.findElement(byId("ivArrow")).click();
            pause(1500);
            out.addAll(popupItems());
        } catch (Exception ignored) { }
        return out;
    }

    /** Items of an open PopupMenu. */
    public List<String> popupItems() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement t : driver.findElements(AppiumBy.xpath(
                    "//android.widget.ListView//android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean tapPopupItem(String item) {
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(
                    "//android.widget.ListView//android.widget.TextView[@text='" + item + "']"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            pause(1500);
            return true;
        } catch (Exception e) { return false; }
    }

    public void dismissPopup() {
        if (!popupItems().isEmpty()) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            pause(700);
        }
    }

    // ───────────────────────── record rows (WardrobeListingPage) ─────────────────────────

    /** Record cards on screen ({@code layMainCard}). */
    public List<WebElement> records() {
        try { return driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                + ":id/recCast']/*")); }
        catch (Exception e) { return List.of(); }
    }

    public int recordCount() { return records().size(); }

    /** All text of a record card joined with " | " (sceneNo html + descText). */
    public String recordText(WebElement card) {
        StringBuilder sb = new StringBuilder();
        try {
            for (WebElement t : card.findElements(By.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) sb.append(s.trim()).append(" | ");
            }
        } catch (Exception ignored) { }
        return sb.toString();
    }

    /** The first record whose text contains {@code marker} (case-insensitive). */
    public WebElement record(String marker) {
        String want = marker.toLowerCase();
        for (int i = 0; i < 5; i++) {
            for (WebElement c : records()) {
                if (recordText(c).toLowerCase().contains(want)) return c;
            }
            if (!scrollRecords()) break;
        }
        return null;
    }

    public boolean waitRecord(String marker, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (record(marker) != null) return true;
            pause(2000);
        }
        return false;
    }

    public boolean waitRecordGone(String marker, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (record(marker) == null) return true;
            pause(2000);
        }
        return false;
    }

    private boolean scrollRecords() {
        try {
            WebElement list = driver.findElement(byId("recCast"));
            Object more = driver.executeScript("mobile: scrollGesture", Map.of(
                    "elementId", ((org.openqa.selenium.remote.RemoteWebElement) list).getId(),
                    "direction", "down", "percent", 0.8));
            pause(600);
            return Boolean.TRUE.equals(more);
        } catch (Exception e) { return false; }
    }

    /** Opens a record's overflow ({@code imgMore}); returns the popup items, leaves it OPEN. */
    public List<String> openRecordMenu(String marker) {
        WebElement card = record(marker);
        if (card == null) return new ArrayList<>();
        try {
            card.findElement(byId("imgMore")).click();
            pause(1500);
        } catch (Exception e) { return new ArrayList<>(); }
        return popupItems();
    }

    public boolean recordMenu(String marker, String item) {
        List<String> items = openRecordMenu(marker);
        if (!items.contains(item)) { dismissPopup(); return false; }
        return tapPopupItem(item);
    }

    /** The per-record chat icon ({@code icChat}) — hidden for Final records. */
    public boolean recordChatIconShown(String marker) {
        WebElement card = record(marker);
        if (card == null) return false;
        try { return !card.findElements(byId("icChat")).isEmpty() && card.findElement(byId("icChat")).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public boolean tapRecordChat(String marker) {
        WebElement card = record(marker);
        if (card == null) return false;
        try { card.findElement(byId("icChat")).click(); pause(3000); return true; }
        catch (Exception e) { return false; }
    }

    // ───────────────────────── multi-select (WardrobeListingPage) ─────────────────────────

    /** Toolbar "Select" ({@code selectTv}). */
    public boolean startSelection() {
        if (!tap("selectTv")) return false;
        return isVisible("deleteMoveRl", Duration.ofSeconds(5));
    }

    public boolean selectionBarShown() { return isVisible("deleteMoveRl", Duration.ofSeconds(2)); }

    /** Texts of the selection bar buttons that are displayed. */
    public List<String> selectionButtons() {
        List<String> out = new ArrayList<>();
        for (String id : List.of("btnMoveTo", "btnMoveToPublish", "btnDistribute", "btnCancel", "btnDelete")) {
            try {
                for (WebElement e : driver.findElements(byId(id))) {
                    if (e.isDisplayed()) {
                        String t = e.getText();
                        out.add(t == null || t.isBlank() ? id : t.trim());
                    }
                }
            } catch (Exception ignored) { }
        }
        return out;
    }

    public boolean tapSelectionButton(String id) { return tap(id); }

    /** Ticks a record in selection mode (its {@code layOverlay}). */
    public boolean tickRecord(String marker) {
        WebElement card = record(marker);
        if (card == null) return false;
        try { card.findElement(byId("layOverlay")).click(); pause(800); return true; }
        catch (Exception e) { return false; }
    }

    // ───────────────────────── dialogs / messages ─────────────────────────

    public boolean dialogShown(Duration timeout) { return isVisible("rightButton", timeout); }

    public String dialogMessage() {
        try { return driver.findElement(byId("message")).getText(); } catch (Exception e) { return ""; }
    }

    public boolean dialogYes() { return tap("rightButton"); }

    public boolean dialogNo() { return tap("leftButton"); }

    /** Waits for a snackbar or toast containing the text; returns what it saw, or "". */
    public String waitMessage(String contains, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            String s = snackbarOrToast();
            if (!s.isEmpty() && (contains == null || s.toLowerCase().contains(contains.toLowerCase()))) return s;
            pause(300);
        }
        return "";
    }

    public String snackbarOrToast() {
        try {
            List<WebElement> s = driver.findElements(byId("snackbar_text"));
            if (!s.isEmpty()) return s.get(0).getText();
            List<WebElement> t = driver.findElements(By.xpath("//android.widget.Toast"));
            if (!t.isEmpty()) return t.get(0).getText();
        } catch (Exception ignored) { }
        return "";
    }

    // ───────────────────────── filter sheet ─────────────────────────

    /** Option labels in a filter list: {@code recFilterBy} (Group by) or {@code recSortBy} (Filter by). */
    public List<String> filterOptions(String listId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement t : driver.findElement(byId(listId)).findElements(By.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** The checked option of a filter list, read from the row's checked/selected state; "" when unreadable. */
    public String selectedFilterOption(String listId) {
        try {
            for (WebElement row : driver.findElement(byId(listId)).findElements(By.xpath("./*"))) {
                boolean on = false;
                for (WebElement any : row.findElements(By.xpath(".//*"))) {
                    if ("true".equals(any.getAttribute("checked")) || "true".equals(any.getAttribute("selected"))) { on = true; break; }
                }
                if ("true".equals(row.getAttribute("checked")) || "true".equals(row.getAttribute("selected"))) on = true;
                if (on) {
                    for (WebElement t : row.findElements(By.className("android.widget.TextView"))) {
                        String s = t.getText();
                        if (s != null && !s.isBlank()) return s.trim();
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }

    public boolean tapFilterOption(String listId, String label) {
        try {
            List<WebElement> els = driver.findElement(byId(listId)).findElements(By.xpath(
                    ".//android.widget.TextView[@text='" + label + "']"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            pause(500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Submit ({@code btnApply}). */
    public boolean applyFilter() { boolean ok = tap("btnApply"); pause(2500); return ok; }

    /** Reset ({@code btnRest} in the layout; {@code button} in older builds). */
    public boolean resetFilter() {
        // btnRest is the include's ConstraintLayout; the listener sits on its MaterialButton "button".
        boolean ok = tap("button") || tap("btnRest");
        pause(2500);
        return ok;
    }

    /** Applies "Group by" + "Filter by" in one go. */
    public boolean groupAndFilter(String groupBy, String filterBy) {
        if (!openFilter()) return false;
        boolean ok = (groupBy == null || tapFilterOption("recFilterBy", groupBy))
                && (filterBy == null || tapFilterOption("recSortBy", filterBy));
        if (!ok) { try { driver.navigate().back(); } catch (Exception ignored) { } return false; }
        return applyFilter();
    }

    // ───────────────────────── hierarchy (folder Edit) sheet ─────────────────────────

    public boolean hierarchySheetShown(Duration timeout) { return isVisible("btnUpdate", timeout); }

    /** {@code etLevelOne/Two/Three}: text, or null when hidden. */
    public String levelText(int level) {
        String id = level == 1 ? "etLevelOne" : level == 2 ? "etLevelTwo" : "etLevelThree";
        try {
            List<WebElement> e = driver.findElements(byId(id));
            return e.isEmpty() || !e.get(0).isDisplayed() ? null : e.get(0).getText();
        } catch (Exception e) { return null; }
    }

    public boolean levelEnabled(int level) {
        String id = level == 1 ? "etLevelOne" : level == 2 ? "etLevelTwo" : "etLevelThree";
        try {
            List<WebElement> e = driver.findElements(byId(id));
            return !e.isEmpty() && e.get(0).isEnabled() && "true".equals(e.get(0).getAttribute("focusable"));
        } catch (Exception e) { return false; }
    }

    public boolean setLevel(int level, String value) {
        String id = level == 1 ? "etLevelOne" : level == 2 ? "etLevelTwo" : "etLevelThree";
        try {
            WebElement e = driver.findElement(byId(id));
            e.click();
            e.clear();
            e.sendKeys(value);
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean tapHierarchyUpdate() { return tap("btnUpdate"); }

    public boolean tapHierarchyCancel() { return tap("btnCancel"); }

    // ───────────────────────── small helpers ─────────────────────────

    public boolean tap(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            pause(600);
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean tapText(String text) {
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath("//*[@text='" + text + "']"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            pause(1200);
            return true;
        } catch (Exception e) { return false; }
    }

    public void back() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        pause(1200);
    }

    static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
