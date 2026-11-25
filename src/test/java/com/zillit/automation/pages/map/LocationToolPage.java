package com.zillit.automation.pages.map;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools &rarr; <b>Location</b> ({@code LocationPage}) — the tool that FEEDS the Map overlay.
 *
 * <p>Three tabs, in this order, each a {@code LocationFolderPage} over a different
 * identifier:
 * <pre>
 *   Selects    SELECTED_IDENTIFIER     — never reaches the Map
 *   Shortlist  SHORTLISTED_IDENTIFIER  — drawn as "Shortlisted Shoot Location"
 *   Final      PUBLISH_IDENTIFIER      — drawn as "Finalized Shoot Location"
 * </pre>
 *
 * <p>The tab LABELS and the Map's tags are not the same words, which is a standing trap:
 * the tab reads "Shortlist" ({@code R.string.shortlisted}) while the Map's tag is
 * "Shortlisted", and the tab reads "Final" while the wire status may be any of
 * {@code published} / {@code final} / {@code finalized}.
 *
 * <p>This tool is not enabled on every project. Callers should treat an absent tile as a
 * skip, not a failure — {@code Tools.kt} only dispatches {@code LOCATION_TOOL} when the
 * project has it switched on.
 */
public class LocationToolPage extends BasePage {

    /** {@code R.string.selects} — the tab reads "Selects", plural, not "Select". */
    public static final String TAB_SELECT = "Selects";
    public static final String TAB_SHORTLIST = "Shortlist";
    public static final String TAB_FINAL = "Final";

    /** Tab labels in the order {@code LocationPage} attaches them. */
    public static final List<String> TABS_IN_ORDER =
            List.of(TAB_SELECT, TAB_SHORTLIST, TAB_FINAL);

    public LocationToolPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("tabLayout", timeout) && isVisible("viewPager", Duration.ofSeconds(3));
    }

    /** Tab titles as rendered, left to right. */
    public List<String> tabLabels() {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> tabs = driver.findElements(
                    AppiumBy.xpath("//*[@resource-id='" + appPackage() + ":id/tabLayout']"
                            + "//android.widget.TextView"));
            for (WebElement t : tabs) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean selectTab(String label) {
        try {
            WebElement tab = driver.findElement(
                    AppiumBy.xpath("//*[@resource-id='" + appPackage() + ":id/tabLayout']"
                            + "//android.widget.TextView[@text='" + label + "']"));
            tab.click();
            try { Thread.sleep(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean filterIconShown() {
        try { return !driver.findElements(byId("filterIv")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean openFilter() {
        try {
            List<WebElement> els = driver.findElements(byId("filterIv"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            try { Thread.sleep(1200); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Folder / scene rows inside the current tab.
     *
     * <p>Rows are {@code location_folder_item.xml}, whose name view is {@code tvName};
     * the list itself is {@code recScene} on {@code fragment_location_folder_page.xml}.
     * Counting is scoped to that list — {@code tvName} is one of the most reused ids in
     * the app, so an unscoped count would pick up whatever else is on screen.
     */
    public int folderCount() {
        try {
            return driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName']")).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Folder / scene names in the current tab. */
    public List<String> folderNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** True once the multi-select action bar is up (Move To / Cancel / Delete). */
    public boolean selectionBarShown(Duration timeout) {
        return isVisible("deleteMoveRl", timeout) || isVisible("btnMoveTo", timeout);
    }

    /** The tool's own empty state for a tab with nothing filed under it. */
    public boolean emptyStateShown(Duration timeout) {
        return isVisible("emptyView", timeout);
    }

    public boolean listingShown(Duration timeout) {
        return isVisible("recScene", timeout) || isVisible("search", timeout);
    }

    // ───────────────────────── opening the tool ─────────────────────────

    /**
     * Taps the Location ROW on the Tools tab (after a search for "Location").
     *
     * <p>Not {@code ToolsPage.openModule("Location")}: that matches the first TextView
     * whose text matches {@code (?i)Location}, and on this project the first one is the
     * GROUP header "LOCATION" ({@code groupTitleTv}) above the row. Tapping it does
     * nothing useful, and every case then skipped with "Location tool did not load".
     * The row's own label is {@code nameTv}.
     */
    public boolean tapToolRow(String label) {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            try {
                List<WebElement> rows = driver.findElements(AppiumBy.xpath(
                        "//*[@resource-id='" + appPackage() + ":id/nameTv' and @text='" + label + "']"));
                if (!rows.isEmpty()) {
                    rows.get(0).click();
                    pause(2000);
                    return true;
                }
            } catch (Exception ignored) { }
            pause(500);
        }
        return false;
    }

    // ───────────────────────── the add / move form ─────────────────────────
    //
    // Both are LocationDetailPage (activity_location_detail_page.xml). Every text field is
    // an include whose EditText is "editTextInput", so a field is addressed by its
    // CONTAINER id: fileNameInc, scene_no, episode, locationAddress, locationCity, linkInc.

    public static final String FIELD_NAME = "fileNameInc";
    public static final String FIELD_SCENE = "scene_no";
    public static final String FIELD_EPISODE = "episode";
    public static final String FIELD_ADDRESS = "locationAddress";
    public static final String FIELD_CITY = "locationCity";
    public static final String FIELD_LINK = "linkInc";

    /** "Location Attachment Required." — create refuses an entry with no media or link. */
    public static final String ERR_ATTACHMENT = "Location Attachment Required.";

    /** The "+" FAB on a tab: opens LocationDetailPage in create mode for THAT tab. */
    public boolean openAddForm() {
        if (!clickId("takeSceneShots")) return false;
        return formShown(Duration.ofSeconds(10));
    }

    public boolean formShown(Duration timeout) {
        return isVisible("submit_btn", timeout) && isVisible("fileNameInc", Duration.ofSeconds(2))
                || isVisible("fileNameInc", Duration.ofSeconds(1));
    }

    /** The form's toolbar header, e.g. "ADD LOCATION DETAILS". */
    public String headerText() {
        try { return driver.findElement(byId("headerText")).getText(); }
        catch (Exception e) { return null; }
    }

    private WebElement fieldInput(String containerId) {
        scrollToId(containerId);
        for (int i = 0; i < 3; i++) {
            List<WebElement> c = driver.findElements(byId(containerId));
            if (!c.isEmpty()) {
                List<WebElement> in = c.get(0).findElements(byId("editTextInput"));
                if (!in.isEmpty()) return in.get(0);
            }
            swipeForm(true);
        }
        return null;
    }

    /** Reads a form field. The hint is returned when the field is empty, so callers trim. */
    public String fieldText(String containerId) {
        try {
            WebElement e = fieldInput(containerId);
            return e == null ? null : e.getText();
        } catch (Exception e) { return null; }
    }

    public boolean fieldEnabled(String containerId) {
        try {
            WebElement e = fieldInput(containerId);
            return e != null && e.isEnabled();
        } catch (Exception e) { return false; }
    }

    public boolean setField(String containerId, String value) {
        try {
            WebElement e = fieldInput(containerId);
            if (e == null) return false;
            e.click();
            e.clear();
            e.sendKeys(value);
            closeKeyboardIfStablyShown();
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Drops the pin through the app's own picker (MapsActivity): "Search / pick on map",
     * accept the marker it places on the device's GPS fix, tick.
     *
     * <p>The Places search at the top of that screen is not used — Places does not work
     * on this build. The current-location route is what the picker does on its own as
     * soon as the map is ready ({@code onMapReady → getCurrentLocationOfUser}), and its
     * result is reverse-geocoded, so Address and City arrive filled
     * ({@code fillAddressAndCityFromPin}). The tick refuses with "Location Not Selected"
     * until that geocode lands, hence the retry.
     *
     * @return the Google Maps link the form derived from the pin, or null
     */
    public String pickCurrentLocationOnMap() {
        scrollToId("btnPickOnMap");
        if (!clickId("btnPickOnMap")) return null;
        if (!isVisible("sendLocation", Duration.ofSeconds(20))) return null;
        pause(5000);
        for (int i = 0; i < 4; i++) {
            clickId("sendLocation");
            pause(3000);
            if (formShown(Duration.ofSeconds(3))) break;
            pause(3000);
        }
        if (!formShown(Duration.ofSeconds(8))) return null;
        // Address and City come from an async reverse geocode after the result lands.
        for (int i = 0; i < 10; i++) {
            String link = fieldText(FIELD_LINK);
            if (link != null && link.startsWith("https://www.google.com/maps?q=")) {
                pause(2500);
                return link;
            }
            pause(1000);
        }
        return null;
    }

    /**
     * Adds a link attachment: Upload Media → Link → paste → Submit.
     *
     * <p>The link field in that dialog is {@code inputType=TYPE_NULL} and not focusable;
     * the only way in is its paste icon ({@code copyIv}), which reads the clipboard. So the
     * clipboard is set through the driver first. Create refuses an entry with neither
     * media nor a link ("Location Attachment Required.").
     */
    public boolean addLinkAttachment(String url) {
        // Clipboard set only once the dialog is up: Android 13+ shows a clipboard preview
        // (with a Share button that opens Quick Share) in the bottom corner for a few seconds
        // after every clipboard write, and a tap on the picker sheet underneath landed on it.
        if (!tapPickerLink()) return false;
        if (!isVisible("copyIv", Duration.ofSeconds(6))) return false;
        try { driver.setClipboardText(url); } catch (Exception e) { return false; }
        clickId("copyIv");
        pause(800);
        String pasted = null;
        try {
            pasted = driver.findElement(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/message']//*[@resource-id='" + appPackage() + ":id/editTextInput']")).getText();
        } catch (Exception ignored) { }
        if (pasted == null || !pasted.startsWith("http")) {
            clickId("leftButton");
            return false;
        }
        clickId("rightButton");
        pause(4000);      // findMetaDataFromUrl runs before the attachment is listed
        return true;
    }

    public boolean submitForm() {
        scrollToId("submit_btn");
        return clickId("submit_btn");
    }

    /** The snackbar's text, or null when none is up. */
    public String snackbarText() {
        try {
            List<WebElement> s = driver.findElements(byId("snackbar_text"));
            return s.isEmpty() ? null : s.get(0).getText();
        } catch (Exception e) { return null; }
    }

    // ───────────────────────── rows and their menu ─────────────────────────

    /**
     * Finds the folder row for a location, case-insensitively — the app stores the name
     * through {@code capitalizeSentence()}, which LOWER-cases everything after the first
     * letter of each word ("ZL_AUTO_Shoot 123" is saved as "Zl_auto_shoot 123").
     */
    private WebElement rowFor(String name) {
        WebElement row = rowOnScreen(name);
        if (row != null) return row;
        search(name);
        return rowOnScreen(name);
    }

    /**
     * A row whose label is the name itself (folder page, grouped by location) or ends in
     * ": name" — the listing page prefixes it: "Location Name : Zl_auto_shoot 123".
     */
    private WebElement rowOnScreen(String name) {
        String want = name.toLowerCase();
        try {
            for (WebElement row : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']/*"))) {
                List<WebElement> n = row.findElements(byId("tvName"));
                if (n.isEmpty()) continue;
                String t = n.get(0).getText();
                if (t == null) continue;
                String l = t.trim().toLowerCase();
                if (l.equals(want) || l.endsWith(": " + want)) return row;
            }
        } catch (Exception ignored) { }
        return null;
    }

    /**
     * On a TELEVISION project the tabs group by EPISODE, not by location name: the folder
     * page lists "Episode No : 1", and the locations sit one level down on
     * {@code LocationListingPage}. The row menu on the episode row refuses to move
     * ("p_move_from_next_page"), so moves and deletes happen on the listing page.
     *
     * @return true when the listing page for that episode is open
     */
    public boolean openEpisodeGroup(String episode) {
        try {
            List<WebElement> rows = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName'"
                            + " and @text='Episode No : " + episode + "']"));
            if (rows.isEmpty()) return false;
            rows.get(0).click();
            pause(3500);
            return onListingPage(Duration.ofSeconds(10));
        } catch (Exception e) { return false; }
    }

    /** {@code LocationListingPage}: a row list and search, but no tab strip. */
    public boolean onListingPage(Duration timeout) {
        return isVisible("recScene", timeout)
                && driver.findElements(byId("tabLayout")).isEmpty();
    }

    /** The row is on screen right now (no search). */
    public boolean rowShown(String name) {
        return rowOnScreen(name) != null;
    }

    /** True when the current tab lists the location. Searches when it is not in view. */
    public boolean hasRow(String name) {
        return rowFor(name) != null;
    }

    /** Waits for the row to appear (after a create or a move, which refetch). */
    public boolean waitForRow(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (rowFor(name) != null) return true;
            pause(2000);
        }
        return false;
    }

    /** Waits for the row to leave the current tab. */
    public boolean waitForRowGone(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (rowFor(name) == null) return true;
            pause(2000);
        }
        return false;
    }

    /** Types into the tab's search ("Search by Scene No/Location Name/City"). */
    public void search(String term) {
        try {
            List<WebElement> f = driver.findElements(byId("search_widget_edit_textvw"));
            if (f.isEmpty()) return;
            WebElement e = f.get(0);
            e.click();
            e.clear();
            if (term != null && !term.isEmpty()) e.sendKeys(term);
            closeKeyboardIfStablyShown();
            clickId("imageView6");
            pause(2500);
        } catch (Exception ignored) { }
    }

    /**
     * Opens the row's overflow menu ({@code ivArrow} → {@code R.menu.folder_options}) and
     * taps one item: "Move To Shortlist", "Move To Final" or "Delete". Which items exist
     * depends on the tab — LocationFolderPage hides the move that would be a no-op.
     */
    public boolean rowMenu(String name, String item) {
        WebElement row = rowFor(name);
        if (row == null) return false;
        try {
            List<WebElement> more = row.findElements(byId("ivArrow"));
            if (more.isEmpty()) return false;
            more.get(0).click();
            pause(1500);
        } catch (Exception e) { return false; }
        return clickText(item);
    }

    /** Menu items offered for a row, read before tapping one (for messages). */
    public List<String> rowMenuItems(String name) {
        List<String> out = new ArrayList<>();
        WebElement row = rowFor(name);
        if (row == null) return out;
        try {
            row.findElements(byId("ivArrow")).get(0).click();
            pause(1500);
            for (WebElement t : driver.findElements(AppiumBy.xpath(
                    "//android.widget.ListView//android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
            driver.navigate().back();
            pause(800);
        } catch (Exception ignored) { }
        return out;
    }

    /** The app's two-button dialog (showDialogWithButton): taps the RIGHT button (Yes / Submit). */
    public boolean confirmDialog(Duration timeout) {
        if (!isVisible("rightButton", timeout)) return false;
        return clickId("rightButton");
    }

    // ───────────────────────── toolbar, group-by, search ─────────────────────────

    /** The toolbar header (custom_toolbar {@code headerText}), e.g. "Location". */
    public String toolbarHeader() {
        try {
            List<WebElement> h = driver.findElements(byId("headerText"));
            return h.isEmpty() ? null : h.get(0).getText();
        } catch (Exception e) { return null; }
    }

    /**
     * The GROUP-BY icon. {@code LocationPage} hides {@code filterIv} for good
     * ({@code showHideFilter}: {@code filterIv.isVisible = false}) and puts the
     * {@code R.menu.library_filter} popup on the toolbar's {@code settingImage} instead.
     */
    public boolean groupByIconShown() {
        return isVisible("settingImage", Duration.ofSeconds(3));
    }

    /** Opens the group-by popup and returns its items (left open). */
    public List<String> openGroupByMenu() {
        if (!clickId("settingImage")) return new ArrayList<>();
        pause(1200);
        return popupItems();
    }

    /** Texts of the open PopupMenu. */
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

    /** Taps an item of the open popup. */
    public boolean tapPopupItem(String text) {
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(
                    "//android.widget.ListView//android.widget.TextView[@text='" + text + "']"));
            if (els.isEmpty()) {
                // A popup opened near the bottom is clipped and scrolls: bring the item in.
                try {
                    els = List.of(driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiScrollable(new UiSelector().className(\"android.widget.ListView\"))"
                                    + ".scrollIntoView(new UiSelector().text(\"" + text + "\"))")));
                } catch (Exception e) { return false; }
            }
            els.get(0).click();
            pause(2500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Taps a popup item and returns at once — for items that answer with a short toast. */
    public boolean tapPopupItemQuick(String text) {
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(
                    "//android.widget.ListView//android.widget.TextView[@text='" + text + "']"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            return true;
        } catch (Exception e) { return false; }
    }

    /** Closes an open popup / keyboard / dialog with one BACK. */
    public void back() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        pause(1200);
    }

    /** Row labels ({@code tvName}) on the current list. */
    public List<String> rowLabels() { return folderNames(); }

    /** The secondary line ({@code tvother}) of the row whose label ends with the name. */
    public String rowSubtitle(String name) {
        WebElement row = rowOnScreen(name);
        if (row == null) return null;
        try {
            List<WebElement> o = row.findElements(byId("tvother"));
            return o.isEmpty() ? null : o.get(0).getText();
        } catch (Exception e) { return null; }
    }

    /** The row whose label equals {@code label} exactly ("Episode No : 1"). */
    private WebElement rowByLabel(String label) {
        try {
            for (WebElement row : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']/*"))) {
                List<WebElement> n = row.findElements(byId("tvName"));
                if (!n.isEmpty() && label.equals(String.valueOf(n.get(0).getText()).trim())) return row;
            }
        } catch (Exception ignored) { }
        return null;
    }

    public boolean labelShown(String label) { return rowByLabel(label) != null; }

    /** Opens the row menu ({@code ivArrow}) of the row with that exact label; returns the items, menu left open. */
    public List<String> openMenuOfLabel(String label) {
        WebElement row = rowByLabel(label);
        if (row == null) return new ArrayList<>();
        try {
            List<WebElement> more = row.findElements(byId("ivArrow"));
            if (more.isEmpty()) return new ArrayList<>();
            more.get(0).click();
            pause(1500);
        } catch (Exception e) { return new ArrayList<>(); }
        return popupItems();
    }

    /** Opens the menu of the location row (listing page); returns the items, menu left open. */
    public List<String> openRowMenu(String name) {
        WebElement row = rowFor(name);
        if (row == null) return new ArrayList<>();
        try {
            List<WebElement> more = row.findElements(byId("ivArrow"));
            if (more.isEmpty()) return new ArrayList<>();
            more.get(0).click();
            pause(1500);
        } catch (Exception e) { return new ArrayList<>(); }
        return popupItems();
    }

    /** Taps the location row itself (listing page): opens its entries ({@code LibraryPage}). */
    public boolean openRow(String name) {
        WebElement row = rowFor(name);
        if (row == null) return false;
        try {
            List<WebElement> n = row.findElements(byId("tvName"));
            (n.isEmpty() ? row : n.get(0)).click();
            pause(3500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** The search field's hint / text. */
    public String searchFieldText() {
        try {
            List<WebElement> f = driver.findElements(byId("search_widget_edit_textvw"));
            return f.isEmpty() ? null : f.get(0).getText();
        } catch (Exception e) { return null; }
    }

    /** Taps the search icon with whatever is in the field (empty → "Please Enter something…"). */
    public void tapSearchIcon() {
        clickId("imageView6");
        pause(800);
    }

    /** Clears an applied search through the field's X (imageView6 while selected). */
    public void clearSearch() {
        try {
            List<WebElement> f = driver.findElements(byId("search_widget_edit_textvw"));
            if (f.isEmpty()) return;
            String t = f.get(0).getText();
            String hint = null;
            try { hint = f.get(0).getAttribute("hint"); } catch (Exception ignored) { }
            if (t != null && !t.isBlank() && !t.equals(hint) && !t.startsWith("Search by")) {
                clickId("imageView6");
                pause(2000);
            }
        } catch (Exception ignored) { }
    }

    public boolean addButtonShown() { return isVisible("takeSceneShots", Duration.ofSeconds(3)); }
    public boolean cameraButtonShown() { return isVisible("uploadFromCam", Duration.ofSeconds(2)); }
    public boolean unitChatShown() { return isVisible("chatIv", Duration.ofSeconds(3)); }

    /** A toast with this text (UiAutomator2 exposes toasts as android.widget.Toast). */
    public boolean toastShown(String text, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                for (WebElement t : driver.findElements(AppiumBy.xpath("//android.widget.Toast"))) {
                    String s = t.getText();
                    if (s != null && s.contains(text)) return true;
                }
            } catch (Exception ignored) { }
            if (textShown(text, Duration.ofMillis(300))) return true;
            pause(250);
        }
        return false;
    }

    // ───────────────────────── dialogs ─────────────────────────

    /** The app's common two-button dialog is up (header / message / leftButton / rightButton). */
    public boolean dialogShown(Duration timeout) {
        return isVisible("rightButton", timeout) && isVisible("leftButton", Duration.ofSeconds(1));
    }

    public String dialogMessage() {
        try {
            List<WebElement> m = driver.findElements(byId("message"));
            return m.isEmpty() ? null : m.get(0).getText();
        } catch (Exception e) { return null; }
    }

    public String dialogButton(boolean right) {
        try {
            List<WebElement> b = driver.findElements(byId(right ? "rightButton" : "leftButton"));
            return b.isEmpty() ? null : b.get(0).getText();
        } catch (Exception e) { return null; }
    }

    public boolean tapDialogButton(boolean right) {
        boolean ok = clickId(right ? "rightButton" : "leftButton");
        pause(1500);
        return ok;
    }

    public boolean tapId(String id) {
        boolean ok = clickId(id);
        pause(1200);
        return ok;
    }

    public boolean idShown(String id, Duration timeout) { return isVisible(id, timeout); }

    // ───────────────────────── the form (LocationDetailPage) ─────────────────────────

    /** "Upload Media" in the form's toolbar ({@code selectTv}). */
    public boolean uploadMediaShown() {
        try {
            for (WebElement e : driver.findElements(byId("selectTv"))) {
                if ("Upload Media".equalsIgnoreCase(String.valueOf(e.getText()).trim()) && e.isDisplayed()) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Opens Upload Media and returns the texts of the picker it raises (left open). */
    public List<String> openUploadMediaOptions() {
        List<String> out = new ArrayList<>();
        if (!clickId("selectTv")) return out;
        pause(2000);
        try {
            for (WebElement t : driver.findElements(AppiumBy.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Opens the Upload Link dialog (Upload Media → Link). */
    public boolean openLinkDialog() {
        if (!tapPickerLink()) return false;
        return isVisible("copyIv", Duration.ofSeconds(6));
    }

    /**
     * Upload Media → "Link". The form itself carries a "Link" label in its Location block
     * (visible while the form is scrolled to the top), so a plain text match tapped THAT and
     * opened the map picker instead. Count the "Link" texts before opening the picker and tap
     * the NEW one (the picker sheet is drawn last).
     */
    private boolean tapPickerLink() {
        By link = AppiumBy.xpath("//android.widget.TextView[@text='Link']");
        int before;
        try { before = driver.findElements(link).size(); } catch (Exception e) { before = 0; }
        if (!clickId("selectTv")) return false;
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            try {
                List<WebElement> els = driver.findElements(link);
                if (els.size() > before) {
                    els.get(els.size() - 1).click();
                    pause(1200);
                    return true;
                }
            } catch (Exception ignored) { }
            pause(400);
        }
        return false;
    }

    /**
     * Types like a keyboard does — one key event per character — so the field's InputFilters
     * see each keystroke (Appium's sendKeys sets the whole string at once, and a filter that
     * rejects one character then drops all of it). Letters, digits, '-', '#', ',' and space.
     */
    public String typeKeys(String containerId, String value) {
        try {
            WebElement e = fieldInput(containerId);
            if (e == null) return null;
            e.click();
            e.clear();
            for (char ch : value.toCharArray()) {
                io.appium.java_client.android.nativekey.AndroidKey k;
                if (Character.isLetter(ch)) k = io.appium.java_client.android.nativekey.AndroidKey.valueOf(String.valueOf(Character.toUpperCase(ch)));
                else if (Character.isDigit(ch)) k = io.appium.java_client.android.nativekey.AndroidKey.valueOf("DIGIT_" + ch);
                else if (ch == '-') k = io.appium.java_client.android.nativekey.AndroidKey.MINUS;
                else if (ch == '#') k = io.appium.java_client.android.nativekey.AndroidKey.POUND;
                else if (ch == ',') k = io.appium.java_client.android.nativekey.AndroidKey.COMMA;
                else if (ch == ' ') k = io.appium.java_client.android.nativekey.AndroidKey.SPACE;
                else continue;
                driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(k));
            }
            closeKeyboardIfStablyShown();
            String v = e.getText();
            return v == null ? null : v.trim();
        } catch (Exception ex) { return null; }
    }

    /** Taps the read-only maps Link field; true when that opened the map picker (MapsActivity). */
    public boolean tapLinkFieldOpensPicker() {
        try {
            WebElement e = fieldInput(FIELD_LINK);
            if (e == null) return false;
            e.click();
        } catch (Exception ex) { return false; }
        return isVisible("sendLocation", Duration.ofSeconds(15));
    }

    /** The URL shown in the link dialog's field. */
    public String linkDialogValue() {
        try {
            return driver.findElement(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/message']//*[@resource-id='" + appPackage() + ":id/editTextInput']")).getText();
        } catch (Exception e) { return null; }
    }

    /** Pastes the clipboard into the link dialog through its paste icon. */
    public boolean pasteIntoLinkDialog(String clip) {
        try { driver.setClipboardText(clip); } catch (Exception e) { return false; }
        boolean ok = clickId("copyIv");
        pause(800);
        return ok;
    }

    public boolean linkDialogShown(Duration timeout) { return isVisible("copyIv", timeout); }

    /** True when the field's EditText currently has focus (validate() calls requestFocus on the bad field). */
    public boolean fieldFocused(String containerId) {
        try {
            List<WebElement> c = driver.findElements(byId(containerId));
            if (c.isEmpty()) return false;
            List<WebElement> in = c.get(0).findElements(byId("editTextInput"));
            return !in.isEmpty() && "true".equals(in.get(0).getAttribute("focused"));
        } catch (Exception e) { return false; }
    }

    /**
     * An EditText error ({@code setError}) or snackbar carrying {@code text}. The error
     * balloon is a popup window, so the lookup also asks UiAutomator2 for every window.
     */
    public boolean messageShown(String text, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            String s = snackbarText();
            if (s != null && s.contains(text)) return true;
            if (textShown(text, Duration.ofMillis(400))) return true;
            try {
                driver.setSetting("enableMultiWindows", true);
                boolean hit = driver.getPageSource().contains(escapeXml(text));
                driver.setSetting("enableMultiWindows", false);
                if (hit) return true;
            } catch (Exception ignored) { }
            pause(400);
        }
        return false;
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Types into a field and returns what the field holds afterwards (input filters applied). */
    public String typeAndRead(String containerId, String value) {
        setField(containerId, value);
        String v = fieldText(containerId);
        return v == null ? null : v.trim();
    }

    /** Sends keys to the field WITHOUT clicking it (the link field swallows taps into the picker). */
    public String sendKeysOnly(String containerId, String value) {
        try {
            WebElement e = fieldInput(containerId);
            if (e == null) return null;
            try { e.sendKeys(value); } catch (Exception ignored) { }
            closeKeyboardIfStablyShown();
            return e.getText();
        } catch (Exception e) { return null; }
    }

    /** The phone EditText ({@code phoneEdit}) and country code ({@code countryCodeText}). */
    public boolean setPhone(String value) {
        try {
            scrollToId("phoneEdit");
            WebElement e = driver.findElement(byId("phoneEdit"));
            e.click();
            e.clear();
            if (!value.isEmpty()) e.sendKeys(value);
            closeKeyboardIfStablyShown();
            return true;
        } catch (Exception e) { return false; }
    }

    public String countryCode() {
        try {
            scrollToId("countryCodeText");
            String t = driver.findElement(byId("countryCodeText")).getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) { return null; }
    }

    /** True when the "Location" block (map preview / Pick on map) is on the form. */
    public boolean locationSectionShown() {
        scrollToId("btnPickOnMap");
        return !driver.findElements(byId("btnPickOnMap")).isEmpty()
                || !driver.findElements(byId("locationSection")).isEmpty();
    }

    /** The Submit button's label ({@code submit_btn} › {@code tvButton}) is on screen. */
    public boolean submitShown() {
        scrollToId("submit_btn");
        try {
            for (WebElement t : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/submit_btn']//*[@resource-id='" + appPackage() + ":id/tvButton']"))) {
                if (t.isDisplayed() && "Submit".equalsIgnoreCase(String.valueOf(t.getText()).trim())) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Taps "Copy" next to the maps link; returns the snackbar text it raised. */
    public String copyMapLink() {
        scrollToId("btnCopyLink");
        if (!clickId("btnCopyLink")) return null;
        pause(800);
        return snackbarText();
    }

    public String clipboard() {
        try { return driver.getClipboardText(); } catch (Exception e) { return null; }
    }

    // ───────────────────────── small helpers ─────────────────────────

    private boolean clickId(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean clickText(String text) {
        try {
            List<WebElement> els = driver.findElements(AppiumBy.xpath(
                    "//android.widget.TextView[@text='" + text + "']"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            pause(1500);
            return true;
        } catch (Exception e) { return false; }
    }

    private void swipeForm(boolean up) {
        try {
            org.openqa.selenium.Dimension d = driver.manage().window().getSize();
            int x = d.width / 2;
            int from = (int) (d.height * (up ? 0.75 : 0.35));
            int to = (int) (d.height * (up ? 0.35 : 0.75));
            driver.executeScript("mobile: dragGesture", java.util.Map.of(
                    "startX", x, "startY", from, "endX", x, "endY", to, "speed", 2500));
            pause(700);
        } catch (Exception ignored) { }
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
