package com.zillit.automation.pages.casting;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tools &rarr; <b>Casting (Main Cast)</b> / <b>Casting (Background Cast)</b>. Both tiles open
 * the same activity, {@code CastingHomePage}, told apart only by the tool identifier
 * ({@code casting_main_tool} / {@code casting_background_tool}) and the header the tile passes.
 *
 * <p>Screens, all from {@code bottomNav/tools/casting} (build Zillit_32539-dev):
 * <pre>
 *   CastingHomePage            tabs Selects / Shortlist / Final (tabLayout), search,
 *                              unit chat (chatIv), filter (settingImage), folders (recFolder),
 *                              "+" (takeSceneShots) and camera (uploadFromCam)
 *   CastingCharactersFolderPage  one episode's folders (TV project, grouped by Episode)
 *   CastingSecondPage          a folder with more than one talent / character
 *   CastingListingPage         the cast cards (recLibrary), Select mode, card menu
 *   CastingDetailPage          add / edit / move / view form ("ADD CAST DETAILS", "CAST DETAILS")
 *   CastingContactInfoPage     one contact of a cast ("CONTACT INFO")
 *   OtherChatsActivity         the tab's unit chat and a cast's own chat
 * </pre>
 *
 * <p>Folder rows are {@code location_folder_item.xml} ({@code tvName}, {@code tvother},
 * {@code ivArrow}); cast cards are {@code library_item.xml} ({@code scene_no} carries
 * "Character: … / Talent Name: … / Link: … / Gender: … / Uploaded By …", {@code imgMore},
 * {@code icChat}). Names are stored through {@code capitalizeSentence()}, which lower-cases
 * all but the first letter of each word ("ZL_AUTO_Char 12" is saved as "Zl_auto_char 12"),
 * so every name comparison here is case-insensitive.
 */
public class CastingPage extends BasePage {

    // ───────────────────────── tools and strings ─────────────────────────

    /** The two tools, with the tile label (nameTv) and the header the tile hands over. */
    public enum Tool {
        MAIN("Casting (Main Cast)", "Main"),
        BACKGROUND("Casting (Background Cast)", "Background");

        public final String tile;
        public final String shortName;

        Tool(String tile, String shortName) {
            this.tile = tile;
            this.shortName = shortName;
        }

        /** {@code registerCallingToolBar} upper-cases the header: "CASTING (MAIN CAST)". */
        public String header() { return tile.toUpperCase(); }

        public Tool other() { return this == MAIN ? BACKGROUND : MAIN; }
    }

    public static final String TAB_SELECTS = "Selects";
    public static final String TAB_SHORTLIST = "Shortlist";
    public static final String TAB_FINAL = "Final";
    public static final List<String> TABS = List.of(TAB_SELECTS, TAB_SHORTLIST, TAB_FINAL);

    public static final String SEARCH_HINT = "Search by Talent Name/Character";
    public static final String EMPTY_SEARCH = "Please Enter something for search";
    public static final String ADD_HEADER = "ADD CAST DETAILS";
    public static final String VIEW_HEADER = "CAST DETAILS";
    public static final String ATTACHMENT_REQUIRED = "Cast Attachment Required.";
    public static final String NOT_ASSIGNED = "Not Assigned";
    public static final String EPISODE_HINT =
            "You can add multiple episodes by separating them with commas";
    public static final String KEEP_ONE = "Please keep at least one item from list.";
    public static final String PDF_QUESTION = "Generation of Cast list.";
    public static final String PDF_WITHOUT = "Publish without details";
    public static final String PDF_WITH = "Publish with details";
    public static final String PDF_DONE = "Report Generated Successfully.";
    public static final String DELETE_CONFIRM = "Are you sure you want to delete?";

    public static final String GROUP_TALENT = "Talent Name";
    public static final String GROUP_CHARACTER = "Character";
    public static final String GROUP_EPISODE = "Episode no.";
    public static final String SEX_MALE = "Male";
    public static final String SEX_FEMALE = "Female";
    public static final String SEX_NON_BINARY_FILTER = "Non Binary";
    public static final String SEX_ALL = "All";
    /** {@code R.array.gender_type2}, the form's dropdown. */
    public static final List<String> GENDERS = List.of("Male", "Female", "Non-Binary");

    /** Upload Media sheet labels ({@code openPicker}). */
    public static final List<String> PICKER_WITH_LINK =
            List.of("Photo", "Gallery", "Video", "Video Gallery", "Link");

    // Folder (home) menu — R.menu.folder_options_casting.
    public static final String M_MOVE_SHORTLIST = "Move To Shortlist";
    public static final String M_MOVE_FINAL = "Move To Final";
    public static final String M_GEN_PDF = "Generate PDF & Publish.";
    public static final String M_FULL_PDF = "Generate Full PDF";
    public static final String M_DISTRIBUTE = "Distribute";
    public static final String M_EDIT = "Edit";
    public static final String M_DELETE = "Delete";
    // Card menu — R.menu.common_lcw_menu.
    public static final String M_VIEW = "View Details";
    public static final String M_CHAT = "Chat";
    public static final String M_FORWARD = "Forward";
    public static final String M_DOWNLOAD = "Download";
    public static final String M_IMAGE_REPLY = "Image Reply";
    public static final String M_READ_BY = "Read By";

    // Form include ids (every text field's EditText is editTextInput inside these).
    public static final String F_EPISODE = "episode";
    public static final String F_CHARACTER = "characterName";
    public static final String F_TALENT = "talentName";
    public static final String F_CHAR_NO = "characterNumber";
    public static final String F_DESC = "descInc";

    private static final Pattern ERROR_ATTR = Pattern.compile("error=\"([^\"]*)\"");

    public CastingPage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────── opening ─────────────────────────

    /** Taps the tool's row in the (already searched) Tools list — matched on nameTv, exactly. */
    public boolean tapToolRow(Tool tool) {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> rows = byXpath("//*[@resource-id='" + id("nameTv") + "' and @text='" + tool.tile + "']");
            if (!rows.isEmpty()) {
                try { rows.get(0).click(); } catch (WebDriverException e) { return false; }
                pause(2500);
                return true;
            }
            pause(500);
        }
        return false;
    }

    public boolean toolRowListed(Tool tool) {
        return !byXpath("//*[@resource-id='" + id("nameTv") + "' and @text='" + tool.tile + "']").isEmpty();
    }

    /** CastingHomePage: the tab strip plus the folder list. */
    public boolean isHomeLoaded(Duration timeout) {
        return isVisible("tabLayout", timeout) && present("recFolder");
    }

    public String headerText() { return textOf("headerText"); }

    /** Waits for the home of THIS tool (header check), so Main and Background can't be confused. */
    public boolean onHomeOf(Tool tool, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (present("tabLayout") && present("recFolder") && tool.header().equals(trim(headerText()))) return true;
            pause(500);
        }
        return false;
    }

    // ───────────────────────── tabs / toolbar ─────────────────────────

    public List<String> tabLabels() {
        List<String> out = new ArrayList<>();
        for (WebElement t : byXpath("//*[@resource-id='" + id("tabLayout") + "']//android.widget.TextView")) {
            String s = safeText(t);
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    public boolean selectTab(String label) {
        List<WebElement> t = byXpath("//*[@resource-id='" + id("tabLayout") + "']//android.widget.TextView[@text='" + label + "']");
        if (t.isEmpty()) return false;
        try { t.get(0).click(); } catch (WebDriverException e) { return false; }
        pause(2500);
        return true;
    }

    /** Which tab is selected, read from the tab's selected state. */
    public String selectedTab() {
        for (WebElement t : byXpath("//*[@resource-id='" + id("tabLayout") + "']//*[@selected='true']")) {
            String d = t.getAttribute("content-desc");
            if (d != null && !d.isBlank()) return d.trim();
            String s = safeText(t);
            if (!s.isBlank()) return s.trim();
        }
        return "";
    }

    public boolean unitChatIconShown() { return displayed("chatIv"); }
    public boolean addFabShown() { return displayed("takeSceneShots"); }
    public boolean cameraFabShown() { return displayed("uploadFromCam"); }
    public boolean filterIconShown() { return displayed("settingImage"); }

    public boolean openUnitChat() {
        if (!clickId("chatIv")) return false;
        return isVisible("etMessage", Duration.ofSeconds(15)) || isVisible("rvChat", Duration.ofSeconds(3));
    }

    // ───────────────────────── search ─────────────────────────

    public String searchHint() {
        try {
            WebElement e = driver.findElement(byId("search_widget_edit_textvw"));
            String h = e.getAttribute("hint");
            if (h != null && !h.isBlank()) return h;
            return safeText(e);
        } catch (WebDriverException e) { return null; }
    }

    /** Types the term and taps the search icon (imageView6). An empty term taps it on an empty field. */
    public void search(String term) {
        try {
            WebElement e = driver.findElement(byId("search_widget_edit_textvw"));
            e.click();
            e.clear();
            if (term != null && !term.isEmpty()) e.sendKeys(term);
            settleKeyboard();
            clickId("imageView6");
            pause(2000);
        } catch (WebDriverException ignored) { }
    }

    /** Taps the search icon in its "clear" state (it toggles after a search). */
    public void clearSearch() {
        try {
            WebElement e = driver.findElement(byId("search_widget_edit_textvw"));
            String t = safeText(e);
            if (!t.isBlank() && !t.equals(SEARCH_HINT)) {
                clickId("imageView6");
                pause(1500);
            }
        } catch (WebDriverException ignored) { }
    }

    // ───────────────────────── filter sheet ─────────────────────────

    public boolean openFilter() {
        if (!clickId("settingImage")) return false;
        return isVisible("headerFilter", Duration.ofSeconds(6));
    }

    public boolean filterShown() { return present("headerFilter"); }

    /** Option labels of one list of the sheet: recFilterBy (Group By) or recSortBy (Filter By). */
    public List<String> filterOptions(String listId) {
        List<String> out = new ArrayList<>();
        for (WebElement v : byXpath("//*[@resource-id='" + id(listId) + "']//*[@resource-id='" + id("valueTextView") + "']")) {
            String s = safeText(v);
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    /** The ticked option of a list (its row carries icCheck). */
    public String filterChecked(String listId) {
        for (WebElement row : byXpath("//*[@resource-id='" + id(listId) + "']//*[@resource-id='" + id("itemCommonListContainer") + "']")) {
            if (!row.findElements(byId("icCheck")).isEmpty()) {
                List<WebElement> v = row.findElements(byId("valueTextView"));
                if (!v.isEmpty()) return safeText(v.get(0)).trim();
            }
        }
        return "";
    }

    public boolean filterPick(String listId, String label) {
        List<WebElement> v = byXpath("//*[@resource-id='" + id(listId) + "']//*[@resource-id='" + id("valueTextView") + "' and @text='" + label + "']");
        if (v.isEmpty()) return false;
        try { v.get(0).click(); } catch (WebDriverException e) { return false; }
        pause(500);
        return true;
    }

    public boolean filterApply() {
        if (!clickId("btnApply")) return false;
        pause(2500);
        return true;
    }

    public boolean filterReset() {
        List<WebElement> r = byXpath("//*[@resource-id='" + id("button") + "' and @text='Reset']");
        if (r.isEmpty()) return false;
        try { r.get(0).click(); } catch (WebDriverException e) { return false; }
        pause(2500);
        return true;
    }

    public void filterClose() {
        if (!clickId("ivClose")) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        pause(800);
    }

    /** Opens the sheet, sets Group By (and optionally Filter By), applies. */
    public boolean groupBy(String group, String sex) {
        if (!openFilter()) return false;
        boolean ok = filterPick("recFilterBy", group);
        if (sex != null) ok &= filterPick("recSortBy", sex);
        return ok && filterApply();
    }

    // ───────────────────────── folders (home, characters page, second page) ─────────────────────────

    /** A folder row as shown: its title (tvName) and the line under it (tvother). */
    public static final class Folder {
        public final String name;
        public final String other;
        Folder(String name, String other) { this.name = name; this.other = other; }
        @Override public String toString() { return name + " | " + (other == null ? "" : other.replace('\n', '/')); }
    }

    public List<Folder> folders() {
        List<Folder> out = new ArrayList<>();
        for (WebElement row : byXpath("//*[@resource-id='" + id("recFolder") + "']/*")) {
            try {
                List<WebElement> n = row.findElements(byId("tvName"));
                if (n.isEmpty()) continue;
                List<WebElement> o = row.findElements(byId("tvother"));
                out.add(new Folder(safeText(n.get(0)).trim(), o.isEmpty() ? "" : safeText(o.get(0)).trim()));
            } catch (WebDriverException ignored) { }
        }
        return out;
    }

    /** The folder row whose title contains {@code part} (case-insensitive), or null. */
    public WebElement folderRow(String part) {
        String want = part.toLowerCase();
        for (WebElement row : byXpath("//*[@resource-id='" + id("recFolder") + "']/*")) {
            try {
                List<WebElement> n = row.findElements(byId("tvName"));
                if (n.isEmpty()) continue;
                if (safeText(n.get(0)).toLowerCase().contains(want)) return row;
            } catch (WebDriverException ignored) { }
        }
        return null;
    }

    public Folder folder(String part) {
        String want = part.toLowerCase();
        for (Folder f : folders()) if (f.name.toLowerCase().contains(want)) return f;
        return null;
    }

    /** Waits for a folder whose title contains {@code part}, scrolling the list once per round. */
    public boolean waitFolder(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (folderRow(part) != null) return true;
            if (scrollRowIntoView("recFolder", "tvName", part)) return true;
            pause(1500);
        }
        return false;
    }

    public boolean waitFolderGone(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (folderRow(part) == null && !scrollRowIntoView("recFolder", "tvName", part)) return true;
            pause(1500);
        }
        return false;
    }

    public boolean openFolder(String part) {
        WebElement row = folderRow(part);
        if (row == null && scrollRowIntoView("recFolder", "tvName", part)) row = folderRow(part);
        if (row == null) return false;
        try {
            row.findElements(byId("tvName")).get(0).click();
        } catch (WebDriverException e) { return false; }
        pause(3000);
        return true;
    }

    /** Menu items the folder's overflow (ivArrow) offers; closes the menu again. */
    public List<String> folderMenuItems(String part) {
        List<String> out = new ArrayList<>();
        if (!openFolderMenu(part)) return out;
        out.addAll(popupItems());
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(700);
        return out;
    }

    public boolean folderMenu(String part, String item) {
        if (!openFolderMenu(part)) return false;
        return clickPopupItem(item);
    }

    private boolean openFolderMenu(String part) {
        WebElement row = folderRow(part);
        if (row == null && scrollRowIntoView("recFolder", "tvName", part)) row = folderRow(part);
        if (row == null) return false;
        try {
            List<WebElement> a = row.findElements(byId("ivArrow"));
            if (a.isEmpty()) return false;
            a.get(0).click();
        } catch (WebDriverException e) { return false; }
        pause(1300);
        return true;
    }

    // ───────────────────────── listing page (cast cards) ─────────────────────────

    /** {@code CastingListingPage}: cards list (recLibrary), no tab strip. */
    public boolean onListing(Duration timeout) {
        return isVisible("recLibrary", timeout) && !present("tabLayout");
    }

    /** Text of every card on screen (scene_no), newlines kept. */
    public List<String> cards() {
        List<String> out = new ArrayList<>();
        for (WebElement e : byXpath("//*[@resource-id='" + id("recLibrary") + "']//*[@resource-id='" + id("scene_no") + "']")) {
            String s = safeText(e);
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    public WebElement cardRow(String part) {
        String want = part.toLowerCase();
        for (WebElement row : byXpath("//*[@resource-id='" + id("recLibrary") + "']/*")) {
            try {
                List<WebElement> t = row.findElements(byId("scene_no"));
                if (!t.isEmpty() && safeText(t.get(0)).toLowerCase().contains(want)) return row;
            } catch (WebDriverException ignored) { }
        }
        return null;
    }

    public String cardText(String part) {
        WebElement row = cardRow(part);
        if (row == null) return null;
        try { return safeText(row.findElements(byId("scene_no")).get(0)); } catch (Exception e) { return null; }
    }

    public boolean cardHas(String part, String childId) {
        WebElement row = cardRow(part);
        if (row == null) return false;
        try {
            for (WebElement c : row.findElements(byId(childId))) if (c.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return false;
    }

    public boolean waitCard(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (cardRow(part) != null) return true;
            if (scrollRowIntoView("recLibrary", "scene_no", part)) return true;
            pause(1500);
        }
        return false;
    }

    public boolean waitCardGone(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (cardRow(part) == null) return true;
            pause(1500);
        }
        return false;
    }

    public List<String> cardMenuItems(String part) {
        List<String> out = new ArrayList<>();
        if (!openCardMenu(part)) return out;
        out.addAll(popupItems());
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(700);
        return out;
    }

    public boolean cardMenu(String part, String item) {
        if (!openCardMenu(part)) return false;
        return clickPopupItem(item);
    }

    private boolean openCardMenu(String part) {
        WebElement row = cardRow(part);
        if (row == null) return false;
        try {
            List<WebElement> m = row.findElements(byId("imgMore"));
            if (m.isEmpty()) return false;
            m.get(0).click();
        } catch (WebDriverException e) { return false; }
        pause(1300);
        return true;
    }

    public boolean openCardChat(String part) {
        WebElement row = cardRow(part);
        if (row == null) return false;
        try {
            List<WebElement> c = row.findElements(byId("icChat"));
            if (c.isEmpty()) return false;
            c.get(0).click();
        } catch (WebDriverException e) { return false; }
        return isVisible("etMessage", Duration.ofSeconds(15)) || isVisible("rvChat", Duration.ofSeconds(3));
    }

    /** Listing toolbar "Select" (selectTv) → multi-select bar (deleteMoveRl). */
    public boolean enterSelectMode() {
        if (!clickId("selectTv")) return false;
        return isVisible("deleteMoveRl", Duration.ofSeconds(5));
    }

    public boolean selectBarShown() { return displayed("deleteMoveRl"); }

    public String buttonText(String id) { return textOf(id); }

    /** Taps a bottom-bar button, after any snackbar (which covers that bar) has gone. */
    public boolean tap(String id) {
        waitSnackbarGone(Duration.ofSeconds(8));
        boolean ok = clickId(id);
        pause(1200);
        return ok;
    }

    /** In select mode a card is toggled by tapping its overlay. */
    public boolean toggleCard(String part) {
        WebElement row = cardRow(part);
        if (row == null) return false;
        try {
            List<WebElement> o = row.findElements(byId("layOverlay"));
            (o.isEmpty() ? row : o.get(0)).click();
        } catch (WebDriverException e) { return false; }
        pause(800);
        return true;
    }

    // ───────────────────────── add / edit / move / view form ─────────────────────────

    /**
     * The "+" FAB. A snackbar (e.g. "Please Enter something for search") sits right over the
     * FAB at the bottom of the screen and swallows the tap, so wait it out and retry once.
     */
    public boolean openAddForm() {
        for (int attempt = 0; attempt < 2; attempt++) {
            waitSnackbarGone(Duration.ofSeconds(8));
            if (!clickId("takeSceneShots")) return false;
            if (formShown(Duration.ofSeconds(8))) return true;
        }
        return false;
    }

    public void waitSnackbarGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (present("snackbar_text") && System.currentTimeMillis() < deadline) pause(500);
    }

    public boolean formShown(Duration timeout) {
        return isVisible("characterName", timeout) && present("talentName");
    }

    public boolean submitShown() { return present("submit_btn"); }

    /** After a refused Submit: the form is still up a few seconds later (a create posts after ~2 s). */
    public boolean formStays() {
        pause(2500);
        return present("characterName") && present("talentName");
    }

    /** After an accepted Submit: waits for the form to close. */
    public boolean waitFormGone(Duration timeout) {
        lastSnackbar = null;
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String sb = snackbar();
            if (sb != null && !sb.isBlank()) lastSnackbar = sb;
            if (!present("characterName") && !present("submit_btn")) return true;
            pause(700);
        }
        return false;
    }

    /** The last snackbar seen while {@link #waitFormGone} waited (an API refusal shows there). */
    public String lastSnackbar;

    public boolean uploadMediaShown() { return displayed("selectTv") && "Upload Media".equals(trim(textOf("selectTv"))); }

    private WebElement input(String containerId) {
        scrollToId(containerId);
        for (int i = 0; i < 3; i++) {
            List<WebElement> c = driver.findElements(byId(containerId));
            if (!c.isEmpty()) {
                List<WebElement> in = c.get(0).findElements(byId("editTextInput"));
                if (!in.isEmpty()) return in.get(0);
            }
            swipe(true);
        }
        return null;
    }

    public boolean fieldShown(String containerId) {
        return !driver.findElements(byId(containerId)).isEmpty() || (scrollToIdQuiet(containerId) && present(containerId));
    }

    /** The field's text; the hint comes back when it is empty, so compare with care. */
    public String field(String containerId) {
        try {
            WebElement e = input(containerId);
            return e == null ? null : e.getText();
        } catch (WebDriverException e) { return null; }
    }

    public boolean fieldEnabled(String containerId) {
        try {
            WebElement e = input(containerId);
            return e != null && e.isEnabled();
        } catch (WebDriverException e) { return false; }
    }

    public boolean setField(String containerId, String value) {
        try {
            WebElement e = input(containerId);
            if (e == null) return false;
            e.click();
            e.clear();
            if (value != null && !value.isEmpty()) e.sendKeys(value);
            settleKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Hides the IME with ESCAPE. hideKeyboard is a BACK press underneath; fired while the IME
     * is already closing, the BACK lands on CastingDetailPage, which finishes — the form
     * silently disappears (seen on CAST-13 / CAST-14 / CAST-08).
     */
    public void settleKeyboard() {
        try {
            if (!driver.isKeyboardShown()) return;
            // ESCAPE closes the IME and, unlike BACK, does nothing to the activity if the IME
            // has already gone.
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.ESCAPE));
            pause(600);
        } catch (WebDriverException ignored) { }
    }

    /** The {@code setError} text on a field's EditText, read from the page source ("" when clean). */
    public String fieldError(String containerId) {
        try {
            String src = driver.getPageSource();
            int at = src.indexOf(":id/" + containerId + "\"");
            if (at < 0) return "";
            int next = src.indexOf("resource-id=\"" + appPackage() + ":id/editTextInput\"", at);
            if (next < 0) return "";
            int tagStart = src.lastIndexOf('<', next);
            int tagEnd = src.indexOf('>', next);
            Matcher m = ERROR_ATTR.matcher(src.substring(tagStart, tagEnd));
            return m.find() ? m.group(1) : "";
        } catch (WebDriverException e) { return ""; }
    }

    public String gender() {
        scrollToId("gender");
        return textOf("autoCompleteDropDown");
    }

    /**
     * Picks the i-th entry of the gender dropdown. The dropdown is a ListPopupWindow in its own
     * window, which neither Appium's page source nor uiautomator dump includes, so its rows are
     * tapped by position: they hang directly under the field, one row ≈ 5.25% of the screen
     * height each (126 px on the 1080×2400 devices; measured 2026-11-24).
     */
    public boolean pickGenderAt(int index) {
        scrollToId("gender");
        try {
            WebElement field = driver.findElement(byId("autoCompleteDropDown"));
            field.click();
            pause(1300);
            org.openqa.selenium.Rectangle r = field.getRect();
            int screenH = driver.manage().window().getSize().height;
            int rowH = (int) Math.round(screenH * 0.0525);
            int y = r.y + r.height + rowH * index + rowH / 2 + 24;
            if (y > screenH - 120) return false;   // the popup would open upwards — not handled
            driver.executeScript("mobile: clickGesture", Map.of("x", r.x + r.width / 3, "y", y));
            pause(800);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The dropdown's entries, read back by picking each row in turn (field ends on the last). */
    public List<String> genderOptions() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < GENDERS.size(); i++) {
            if (!pickGenderAt(i)) break;
            out.add(trim(gender()));
        }
        return out;
    }

    public boolean pickGender(String label) {
        int i = GENDERS.indexOf(label);
        if (i < 0) return false;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (pickGenderAt(i) && label.equals(trim(gender()))) return true;
            dismissPopupByTap();
        }
        // Fallback: the form saves whatever text the field holds (hitApi matches it against
        // R.array.gender_type2), so setting the text is equivalent to picking the row.
        try {
            WebElement f = driver.findElement(byId("autoCompleteDropDown"));
            f.clear();
            f.sendKeys(label);
            pause(800);
            dismissPopupByTap();
        } catch (WebDriverException ignored) { }
        return label.equals(trim(gender()));
    }

    /** Closes an open dropdown without BACK (BACK with no popup up would close the form). */
    private void dismissPopupByTap() {
        try {
            WebElement h = driver.findElement(byId("headerText"));
            h.click();
            pause(600);
        } catch (WebDriverException ignored) { }
    }

    public boolean episodeHintShown() {
        return textShown(EPISODE_HINT, Duration.ofSeconds(2));
    }

    public boolean availabilityShown() { return fieldShown("cardAvailibility"); }
    public boolean contactCardShown() { return fieldShown("contactInfo"); }

    /** Opens the Upload Media sheet; returns its labels (sheet left open). */
    public List<String> openUploadMedia() {
        List<String> out = new ArrayList<>();
        if (!clickId("selectTv")) return out;
        if (!isVisible("tvLabel", Duration.ofSeconds(6))) return out;
        for (WebElement l : driver.findElements(byId("tvLabel"))) {
            String s = safeText(l);
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    public void closeSheet() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(800);
    }

    /**
     * Opens the Upload Link dialog (Upload Media → Link). The dialog's field is TYPE_NULL and
     * not focusable; the only way to fill it is its paste icon (copyIv), which reads the clipboard.
     */
    public boolean openLinkDialog() {
        if (openUploadMedia().isEmpty()) return false;
        if (!clickText("Link")) return false;
        return isVisible("copyIv", Duration.ofSeconds(6));
    }

    public boolean linkDialogShown() { return present("copyIv"); }

    public boolean pasteIntoLinkDialog(String text) {
        try { driver.setClipboardText(text); } catch (WebDriverException e) { return false; }
        if (!clickId("copyIv")) return false;
        pause(700);
        return true;
    }

    public String linkDialogText() {
        List<WebElement> e = byXpath("//*[@resource-id='" + id("message") + "']//*[@resource-id='" + id("editTextInput") + "']");
        return e.isEmpty() ? null : safeText(e.get(0));
    }

    public String linkDialogError() { return fieldError("message"); }

    public boolean linkDialogSubmit() { boolean ok = clickId("rightButton"); pause(1200); return ok; }
    public boolean linkDialogCancel() { boolean ok = clickId("leftButton"); pause(800); return ok; }

    /** Upload Media → Link → paste → Submit; waits for the link's preview to be added. */
    public boolean addLinkAttachment(String url) {
        if (!openLinkDialog()) return false;
        if (!pasteIntoLinkDialog(url)) { linkDialogCancel(); return false; }
        String pasted = linkDialogText();
        if (pasted == null || !pasted.startsWith("http")) { linkDialogCancel(); return false; }
        linkDialogSubmit();
        // findMetaDataFromUrl runs before the attachment is listed (imageHolder / linkLi).
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline) {
            if (displayed("imageHolder") || displayed("viewPager")) return true;
            pause(1000);
        }
        return displayed("imageHolder") || displayed("linkLi");
    }

    /**
     * Upload Media → Gallery → the app's own gallery → the newest photo → its send FAB → (the
     * preview's sendBtn, when shown). The newest photos are the zl_test_*.jpg fixtures that
     * run-and-report pushes to /sdcard/Pictures and touches to the top before every run, so
     * no personal photo is ever picked (this runs only on the emulators).
     */
    public boolean addGalleryPhoto(Runnable dismissPermissions) {
        if (openUploadMedia().isEmpty()) return false;
        if (!clickText("Gallery")) return false;
        if (dismissPermissions != null) dismissPermissions.run();
        com.zillit.automation.pages.common.MediaPreviewPage media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        if (!media.isGalleryOpen()) { if (!formShown(Duration.ofSeconds(1))) driver.navigate().back(); return false; }
        media.openAllMediaAlbumIfListed();
        if (media.galleryThumbCount() == 0 || media.selectPhotos(1) < 1) { driver.navigate().back(); return false; }
        if (media.isSendFabVisible()) media.tapGallerySendFab();
        if (isVisible("sendBtn", Duration.ofSeconds(10))) clickId("sendBtn");
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            if (formShown(Duration.ofSeconds(1)) && attachmentShown()) return true;
            pause(1000);
        }
        return false;
    }

    public boolean attachmentShown() { return displayed("imageHolder") || displayed("viewPager"); }

    public boolean submitForm() {
        settleKeyboard();
        scrollToId("submit_btn");
        boolean ok = clickId("submit_btn");
        pause(1500);
        return ok;
    }

    public String snackbar() { return textOf("snackbar_text"); }

    public boolean snackbarShows(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String s = snackbar();
            if (s != null && s.contains(text)) return true;
            pause(400);
        }
        return false;
    }

    /** Fills the add form. Null values are left alone. */
    public void fill(String episode, String character, String talent, String gender,
                     String characterNumber, String description) {
        if (episode != null) setField(F_EPISODE, episode);
        if (character != null) setField(F_CHARACTER, character);
        if (talent != null) setField(F_TALENT, talent);
        if (gender != null) {
            settleKeyboard();
            pickGender(gender);
        }
        if (characterNumber != null) {
            setField(F_CHAR_NO, characterNumber);
            pause(1500);       // the 700 ms debounce runs the number-availability check
            clearAvailabilityDialog();
        }
        if (description != null) setField(F_DESC, description);
    }

    /** "Character Number N is already assigned …" asks Change Character / Use Number — take Use Number. */
    public String clearAvailabilityDialog() {
        if (!present("rightButton") || !present("message")) return null;
        String msg = textOf("message");
        clickId("rightButton");
        pause(700);
        return msg;
    }

    public boolean backFromForm() {
        try { driver.navigate().back(); } catch (WebDriverException e) { return false; }
        pause(1500);
        return true;
    }

    // ───────────────────────── contact info ─────────────────────────

    public boolean openContactInfo() {
        scrollToId("contactInfo");
        List<WebElement> add = byXpath("//*[@resource-id='" + id("contactInfo") + "']//*[@resource-id='" + id("btnAdd") + "']");
        if (add.isEmpty()) return false;
        try { add.get(0).click(); } catch (WebDriverException e) { return false; }
        return isVisible("etType", Duration.ofSeconds(8));
    }

    public boolean contactPageShown() { return present("etType") && present("phoneEdit"); }

    public String contactField(String containerId) {
        List<WebElement> e = byXpath("//*[@resource-id='" + id(containerId) + "']//*[@resource-id='" + id("editTextInput") + "']");
        return e.isEmpty() ? null : safeText(e.get(0));
    }

    public boolean setContactField(String containerId, String value) {
        List<WebElement> e = byXpath("//*[@resource-id='" + id(containerId) + "']//*[@resource-id='" + id("editTextInput") + "']");
        if (e.isEmpty()) return false;
        try {
            e.get(0).click();
            e.get(0).clear();
            if (value != null && !value.isEmpty()) e.get(0).sendKeys(value);
            settleKeyboard();
            return true;
        } catch (WebDriverException ex) { return false; }
    }

    public boolean setPhone(String value) {
        try {
            WebElement p = driver.findElement(byId("phoneEdit"));
            p.click();
            p.clear();
            if (value != null && !value.isEmpty()) p.sendKeys(value);
            settleKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public String phoneError() {
        try {
            String src = driver.getPageSource();
            int at = src.indexOf(":id/phoneEdit\"");
            if (at < 0) return "";
            int tagStart = src.lastIndexOf('<', at);
            int tagEnd = src.indexOf('>', at);
            Matcher m = ERROR_ATTR.matcher(src.substring(tagStart, tagEnd));
            return m.find() ? m.group(1) : "";
        } catch (WebDriverException e) { return ""; }
    }

    public boolean contactSubmit() {
        boolean ok = clickId("submit_btn");
        pause(1500);
        return ok;
    }

    /** Contact rows listed back on the cast form (recContactInfo). */
    public List<String> contactRows() {
        List<String> out = new ArrayList<>();
        // The list sits under the Contact Info card at the very bottom of the scroll view.
        scrollToId("contactInfo");
        swipe(true);
        swipe(true);
        for (String rowId : new String[]{"nameTv", "typeTv", "emailTv", "companyTv", "phoneTv"}) {
            for (WebElement t : byXpath("//*[@resource-id='" + id(rowId) + "']")) {
                String s = safeText(t);
                if (!s.isBlank()) out.add(s.trim());
            }
        }
        return out;
    }

    /** Taps a contact row's delete icon (casting_contact_item ivDelete / icDelete). */
    public boolean deleteFirstContact() {
        scrollToId("contactInfo");
        swipe(true);
        for (String idName : new String[]{"deleteIv", "ivDelete", "icDelete"}) {
            List<WebElement> d = byXpath("//*[@resource-id='" + id(idName) + "']");
            if (!d.isEmpty()) {
                try { d.get(0).click(); pause(1000); return true; } catch (WebDriverException ignored) { }
            }
        }
        return false;
    }

    // ───────────────────────── dialogs ─────────────────────────

    public boolean dialogShown(Duration timeout) {
        return isVisible("rightButton", timeout);
    }

    public String dialogHeader() { return textOf("header"); }
    public String dialogMessage() { return textOf("message"); }
    public String rightButtonText() { return textOf("rightButton"); }
    public String leftButtonText() { return textOf("leftButton"); }

    public boolean dialogRight() { boolean ok = clickId("rightButton"); pause(1500); return ok; }
    public boolean dialogLeft() { boolean ok = clickId("leftButton"); pause(1000); return ok; }

    /** The "Report Generated Successfully." dialog (generate_pdf_dialog). */
    public boolean pdfDoneShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (present("closeTv") && PDF_DONE.equals(trim(textOf("header")))) return true;
            pause(1000);
        }
        return false;
    }

    public List<String> pdfDoneButtons() {
        List<String> out = new ArrayList<>();
        for (String b : new String[]{"preSetBtn", "userBtn", "confidentialInfoBtn", "infoBtn", "viewBtn", "closeTv"}) {
            if (displayed(b)) out.add(b);
        }
        return out;
    }

    public boolean pdfDoneClose() { boolean ok = clickId("closeTv"); pause(1000); return ok; }

    // ───────────────────────── small helpers ─────────────────────────

    public boolean present(String idName) {
        try { return !driver.findElements(byId(idName)).isEmpty(); } catch (WebDriverException e) { return false; }
    }

    public boolean displayed(String idName) {
        try {
            for (WebElement e : driver.findElements(byId(idName))) if (e.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return false;
    }

    public String textOf(String idName) {
        try {
            List<WebElement> e = driver.findElements(byId(idName));
            return e.isEmpty() ? null : e.get(0).getText();
        } catch (WebDriverException e) { return null; }
    }

    public boolean clickId(String idName) {
        try {
            List<WebElement> e = driver.findElements(byId(idName));
            if (e.isEmpty()) return false;
            e.get(0).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean clickText(String text) {
        try {
            List<WebElement> e = byXpath("//android.widget.TextView[@text='" + text + "']");
            if (e.isEmpty()) return false;
            e.get(0).click();
            pause(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Titles in an open PopupMenu (android.widget.ListView of TextViews). */
    public List<String> popupItems() {
        List<String> out = new ArrayList<>();
        for (WebElement t : byXpath("//android.widget.ListView//*[@resource-id='android:id/title']")) {
            String s = safeText(t);
            if (!s.isBlank()) out.add(s.trim());
        }
        if (out.isEmpty()) {
            for (WebElement t : byXpath("//android.widget.ListView//android.widget.TextView")) {
                String s = safeText(t);
                if (!s.isBlank()) out.add(s.trim());
            }
        }
        return out;
    }

    private boolean clickPopupItem(String item) {
        List<WebElement> t = byXpath("//android.widget.ListView//*[@text='" + item + "']");
        if (t.isEmpty()) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            return false;
        }
        try { t.get(0).click(); } catch (WebDriverException e) { return false; }
        pause(1500);
        return true;
    }

    private boolean scrollRowIntoView(String listId, String textId, String part) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().resourceId(\"" + id(listId) + "\").scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceId(\"" + id(textId) + "\").textContains(\""
                            + part.replace("\"", "") + "\"))"));
            pause(500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean scrollToIdQuiet(String idName) {
        scrollToId(idName);
        return true;
    }

    private void swipe(boolean up) {
        try {
            org.openqa.selenium.Dimension d = driver.manage().window().getSize();
            int x = d.width / 2;
            int from = (int) (d.height * (up ? 0.72 : 0.35));
            int to = (int) (d.height * (up ? 0.35 : 0.72));
            driver.executeScript("mobile: dragGesture", Map.of(
                    "startX", x, "startY", from, "endX", x, "endY", to, "speed", 2500));
            pause(700);
        } catch (WebDriverException ignored) { }
    }

    private List<WebElement> byXpath(String xp) {
        try { return driver.findElements(AppiumBy.xpath(xp)); } catch (WebDriverException e) { return new ArrayList<>(); }
    }

    private String id(String idName) { return appPackage() + ":id/" + idName; }

    private static String safeText(WebElement e) {
        try { String t = e.getText(); return t == null ? "" : t; } catch (WebDriverException ex) { return ""; }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    public static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
