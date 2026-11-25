package com.zillit.automation.pages.costume;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The Costume record form — {@code AddEditWardrobePage} ({@code add_edit_wardrobe_page.xml}) —
 * and the two screens it opens: the character picker ({@code ChooseCastPage}, header
 * "Casting") and the temporary-character form ({@code CastingDetailPage} opened with
 * {@code HIDE_UPLOADING_ICON}).
 *
 * <p>One activity, four modes, told apart by the toolbar header:
 * <pre>
 *   "Add Costume Details"     create — button Submit, toolbar "Upload Media"
 *   "Update Costume Details"  edit   — button Update, no Upload Media
 *   "Move Costume"            move   — button Move,   no Upload Media
 *   "Costume Details"         view   — no button, fields disabled
 * </pre>
 * Fields are includes whose EditText is {@code editTextInput}: {@code episode} (TV only),
 * {@code sceneNo}, {@code descInc}. The character is the {@code contactInfo} card: its
 * {@code tvTitle} reads "Select Character" until one is chosen, {@code btnAdd} opens the picker.
 *
 * <p>Validation order ({@code AddEditWardrobePage.validate}): attachment (create only) →
 * episode empty (TV, not Selects, or a move) → one episode on move / edit → duplicate
 * episodes → character (not Selects, or a move) → scene empty (not Selects, or a move) →
 * scene must start with a digit.
 */
public class CostumeFormPage extends BasePage {

    public static final String HDR_ADD = "Add Costume Details";
    public static final String HDR_UPDATE = "Update Costume Details";
    public static final String HDR_MOVE = "Move Costume";
    public static final String HDR_VIEW = "Costume Details";

    public static final String FIELD_EPISODE = "episode";
    public static final String FIELD_SCENE = "sceneNo";
    public static final String FIELD_DESC = "descInc";

    public static final String MSG_ATTACHMENT = "Costume Attachment Required.";
    public static final String ERR_EPISODE_EMPTY = "Episode no. cannot be empty.";
    public static final String ERR_EPISODE_DUP = "Duplicate episode numbers found. Please remove duplicates.";
    public static final String ERR_ONE_EPISODE_MOVE = "Only one episode will be allowed at a time during move.";
    public static final String ERR_ONE_EPISODE_EDIT = "Only one episode will be allowed at a time during update.";
    public static final String MSG_SELECT_CHARACTER = "Please Select Character";
    public static final String ERR_SCENE_EMPTY = "Please Enter Scene No.";
    public static final String ERR_SCENE_DIGIT = "Scene No. always starts with a number.";
    public static final String MSG_BACK = "Are you sure you want to go back? Unsaved changes will be lost.";
    public static final String ERR_URL_EMPTY = "Please Enter URL";
    public static final String ERR_URL_INVALID = "Please Enter Valid URL";
    public static final String SELECT_CHARACTER = "Select Character";

    public CostumeFormPage(AndroidDriver driver) {
        super(driver);
    }

    /** The costume form (contactInfo + sceneNo) or the Casting cast form (characterName), with its button. */
    public boolean isOpen(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (present("submit_btn") || present("sceneNo")) {
                if ((present("contactInfo") && present("sceneNo")) || present("characterName")) return true;
            }
            pause(300);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public String header() {
        try { return driver.findElement(byId("headerText")).getText(); } catch (Exception e) { return ""; }
    }

    public boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); } catch (Exception e) { return false; }
    }

    public boolean displayed(String id) {
        try {
            List<WebElement> e = driver.findElements(byId(id));
            return !e.isEmpty() && e.get(0).isDisplayed();
        } catch (Exception e) { return false; }
    }

    /** Text of the submit button ({@code submit_btn → tvButton}): Submit / Update / Move; "" when hidden. */
    public String submitLabel() {
        try {
            List<WebElement> b = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/submit_btn']//*[@resource-id='" + appPackage() + ":id/tvButton']"));
            if (b.isEmpty()) b = driver.findElements(byId("tvButton"));
            return b.isEmpty() || !b.get(0).isDisplayed() ? "" : b.get(0).getText();
        } catch (Exception e) { return ""; }
    }

    public boolean submit() {
        closeKeyboardIfStablyShown();
        scrollToId("submit_btn");
        try {
            List<WebElement> b = driver.findElements(byId("layBtnProgress"));
            if (b.isEmpty()) b = driver.findElements(byId("submit_btn"));
            if (b.isEmpty()) return false;
            b.get(0).click();
            pause(1200);
            return true;
        } catch (Exception e) { return false; }
    }

    // ───────────────────────── fields ─────────────────────────

    private WebElement input(String containerId) {
        scrollToId(containerId);
        try {
            List<WebElement> c = driver.findElements(byId(containerId));
            if (c.isEmpty()) return null;
            List<WebElement> in = c.get(0).findElements(byId("editTextInput"));
            return in.isEmpty() ? null : in.get(0);
        } catch (Exception e) { return null; }
    }

    public boolean fieldShown(String containerId) {
        WebElement e = input(containerId);
        try { return e != null && e.isDisplayed(); } catch (Exception ex) { return false; }
    }

    public String field(String containerId) {
        WebElement e = input(containerId);
        try { return e == null ? null : e.getText(); } catch (Exception ex) { return null; }
    }

    public boolean fieldEnabled(String containerId) {
        WebElement e = input(containerId);
        try { return e != null && e.isEnabled(); } catch (Exception ex) { return false; }
    }

    public boolean setField(String containerId, String value) {
        WebElement e = input(containerId);
        if (e == null) return false;
        try {
            e.click();
            e.clear();
            if (value != null && !value.isEmpty()) e.sendKeys(value);
            closeKeyboardIfStablyShown();
            return true;
        } catch (Exception ex) { return false; }
    }

    /**
     * The inline error of a field ({@code EditText.setError}). UiAutomator does not expose
     * the error popup as a view on every API level, so this looks for the text anywhere.
     */
    public boolean errorShown(String text, Duration timeout) {
        return textShown(text, timeout);
    }

    public String characterTitle() {
        try {
            WebElement card = driver.findElement(byId("contactInfo"));
            return card.findElement(byId("tvTitle")).getText();
        } catch (Exception e) { return ""; }
    }

    public boolean characterAddShown() {
        try {
            List<WebElement> b = driver.findElement(byId("contactInfo")).findElements(byId("btnAdd"));
            return !b.isEmpty() && b.get(0).isDisplayed();
        } catch (Exception e) { return false; }
    }

    /** "+" on the character card → ChooseCastPage. */
    public boolean openCharacterPicker() {
        try {
            driver.findElement(byId("contactInfo")).findElement(byId("btnAdd")).click();
            pause(2500);
            return pickerOpen(Duration.ofSeconds(10));
        } catch (Exception e) { return false; }
    }

    /** The attachment pager ({@code imageHolder}) — visible once media or a link is attached. */
    public boolean attachmentShown() { return displayed("imageHolder") || displayed("viewPager"); }

    // ───────────────────────── upload media ─────────────────────────

    /** Toolbar "Upload Media" ({@code selectTv}) — create mode only. */
    public boolean uploadMediaShown() {
        try {
            List<WebElement> s = driver.findElements(byId("selectTv"));
            return !s.isEmpty() && s.get(0).isDisplayed() && "Upload Media".equalsIgnoreCase(s.get(0).getText().trim());
        } catch (Exception e) { return false; }
    }

    /** Opens the picker sheet and returns the option labels it shows (sheet left open). */
    public List<String> openUploadMedia() {
        List<String> out = new ArrayList<>();
        if (!tap("selectTv")) return out;
        pause(1800);
        try {
            for (WebElement t : driver.findElements(By.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s == null) continue;
                s = s.trim();
                if (List.of("Camera", "Gallery", "Video", "Video Gallery", "Link", "Photo", "Image",
                        "Take Photo", "Record Video", "Photo Gallery", "Files", "Document").contains(s)) out.add(s);
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * Upload Media → Link → paste (the dialog's field is TYPE_NULL and not focusable; only
     * its paste icon {@code copyIv} fills it, from the clipboard) → Submit.
     *
     * @return the text the dialog showed after pasting, or null when the dialog never opened
     */
    public String pasteLinkInDialog(String url) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { driver.setClipboardText(url); } catch (Exception e) {
                System.out.println(">>> setClipboardText failed: " + e.getMessage());
                pause(1000);
                continue;
            }
            if (!linkDialogShown()) {
                List<String> opts = openUploadMedia();
                if (!opts.contains("Link")) {
                    System.out.println(">>> link attempt " + attempt + ": media sheet options " + opts);
                    if (!opts.isEmpty()) back();
                    pause(1200);
                    continue;
                }
                if (!tapText("Link")) continue;
                if (!isVisible("copyIv", Duration.ofSeconds(6))) {
                    System.out.println(">>> link attempt " + attempt + ": the link dialog did not open");
                    continue;
                }
            }
            tap("copyIv");
            pause(1000);
            String t = linkDialogText();
            if (t != null && !t.isBlank() && !t.equals("Link")) return t;
            System.out.println(">>> link attempt " + attempt + ": dialog text after paste '" + t + "'");
            pause(1000);
        }
        return linkDialogShown() ? linkDialogText() : null;
    }

    public boolean linkDialogShown() { return isVisible("copyIv", Duration.ofSeconds(2)); }

    public String linkDialogHeader() {
        try { return driver.findElement(byId("header")).getText(); } catch (Exception e) { return ""; }
    }

    public String linkDialogText() {
        try {
            return driver.findElement(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/message']//*[@resource-id='" + appPackage() + ":id/editTextInput']")).getText();
        } catch (Exception e) { return null; }
    }

    public boolean linkDialogFieldTypeable() {
        try {
            WebElement e = driver.findElement(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/message']//*[@resource-id='" + appPackage() + ":id/editTextInput']"));
            return "true".equals(e.getAttribute("focusable"));
        } catch (Exception e) { return false; }
    }

    public boolean linkDialogSubmit() { boolean ok = tap("rightButton"); pause(1500); return ok; }

    public boolean linkDialogCancel() { boolean ok = tap("leftButton"); pause(800); return ok; }

    /** Adds a link attachment end to end; true when the dialog closed and the form shows it. */
    public boolean addLink(String url) {
        String pasted = pasteLinkInDialog(url);
        if (pasted == null || !pasted.startsWith("http")) {
            System.out.println(">>> addLink: pasted '" + pasted + "'");
            if (linkDialogShown()) linkDialogCancel();
            return false;
        }
        linkDialogSubmit();
        pause(4000);          // findMetaDataFromUrl runs before the attachment is listed
        boolean closed = !linkDialogShown();
        if (!closed) System.out.println(">>> addLink: the dialog stayed open after Submit");
        return closed;
    }

    /**
     * Upload Media → Gallery → the app's own gallery (Fallery) → the album {@code album} →
     * its first photo → send. The album is one this suite pushed, so nothing of the device
     * owner's is ever picked.
     */
    public boolean addGalleryPhoto(String album) {
        List<String> opts = openUploadMedia();
        String g = opts.contains("Gallery") ? "Gallery" : opts.contains("Photo Gallery") ? "Photo Gallery" : null;
        if (g == null) { back(); return false; }
        tapText(g);
        pause(2500);
        dismissPermission();
        // Album list → our album. Some builds open straight into "All Media"; then go back to the list.
        if (!tapAlbum(album)) {
            if (isVisible("recyclerViewBucketContent", Duration.ofSeconds(2))) {
                back();
                pause(1200);
            }
            if (!tapAlbum(album)) return false;
        }
        pause(2000);
        List<WebElement> thumbs = driver.findElements(byId("imageViewPhotoMedia"));
        if (thumbs.isEmpty()) return false;
        thumbs.get(0).click();
        pause(800);
        if (isVisible("floatingButtonSendMedia", Duration.ofSeconds(4))) tap("floatingButtonSendMedia");
        pause(2500);
        // Some flows show the GalleryItemView preview (caption + send) before returning.
        if (isVisible("sendBtn", Duration.ofSeconds(4))) tap("sendBtn");
        pause(2500);
        return isOpen(Duration.ofSeconds(10)) && attachmentShown();
    }

    private boolean tapAlbum(String album) {
        try {
            List<WebElement> a = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + album + "\")"));
            if (a.isEmpty()) {
                driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                + "new UiSelector().textContains(\"" + album + "\"))"));
                a = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + album + "\")"));
            }
            if (a.isEmpty()) return false;
            a.get(0).click();
            return true;
        } catch (Exception e) { return false; }
    }

    private void dismissPermission() {
        for (String txt : List.of("Allow", "While using the app", "Allow all", "ALLOW")) {
            try {
                List<WebElement> b = driver.findElements(AppiumBy.xpath("//android.widget.Button[@text='" + txt + "']"));
                if (!b.isEmpty()) { b.get(0).click(); pause(1000); }
            } catch (Exception ignored) { }
        }
    }

    // ───────────────────────── character picker (ChooseCastPage) ─────────────────────────

    public boolean pickerOpen(Duration timeout) {
        return isVisible("recLibrary", timeout) && "Casting".equalsIgnoreCase(header().trim());
    }

    /** The picker's "+" (temporary character) — offered only from a Selects create. */
    public boolean pickerAddShown() { return displayed("takeSceneShots"); }

    public List<String> pickerNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/recLibrary']//*[@resource-id='" + appPackage() + ":id/nameTv']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public String pickerSearchHint() {
        try { return driver.findElement(byId("search_widget_edit_textvw")).getText(); } catch (Exception e) { return ""; }
    }

    public void pickerSearch(String term) {
        try {
            WebElement e = driver.findElement(byId("search_widget_edit_textvw"));
            e.click();
            e.clear();
            if (term != null && !term.isEmpty()) e.sendKeys(term);
            pause(1500);
        } catch (Exception ignored) { }
    }

    /**
     * Picks the picker row whose label contains {@code name} (case-insensitive) — rows read
     * "Character: &lt;name&gt;". Returns true once back on the costume form.
     */
    public boolean pickCast(String name) {
        String want = name.toLowerCase();
        pickerSearch(name);
        pause(1500);
        for (int i = 0; i < 4; i++) {
            try {
                for (WebElement e : driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                        + ":id/recLibrary']//*[@resource-id='" + appPackage() + ":id/nameTv']"))) {
                    String t = e.getText();
                    if (t != null && t.toLowerCase().contains(want)) {
                        e.click();
                        pause(2000);
                        return isOpen(Duration.ofSeconds(8)) && !present("recLibrary");
                    }
                }
            } catch (Exception ignored) { }
            pause(2000);
        }
        System.out.println(">>> pickCast: no picker row for '" + name + "' — rows " + pickerNames());
        return false;
    }

    /** "+" → the temporary character form. */
    public boolean openTempCharacterForm() {
        if (!tap("takeSceneShots")) return false;
        return isVisible("characterName", Duration.ofSeconds(10));
    }

    public boolean tempFormShown(String id) { return displayed(id); }

    /**
     * Fills Character Name, Talent Name and Gender on the temporary-character form and
     * submits it; the form returns straight to the costume form with the character set.
     * The app stores the names through {@code capitalizeSentence()}.
     */
    public boolean createTempCharacter(String character, String talent, String gender) {
        if (!openTempCharacterForm()) { System.out.println(">>> temp: the character form did not open"); return false; }
        if (character != null && !setField("characterName", character)) { System.out.println(">>> temp: character name not typed"); return false; }
        if (talent != null && !setField("talentName", talent)) { System.out.println(">>> temp: talent name not typed"); return false; }
        if (gender != null && !pickGender(gender)) { System.out.println(">>> temp: gender '" + gender + "' not picked"); return false; }
        if (!submit()) { System.out.println(">>> temp: Submit not tapped"); return false; }
        pause(2500);
        // Both forms have a contactInfo card; the costume form is the one without characterName.
        long end = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < end) {
            if (!present("characterName") && isOpen(Duration.ofSeconds(1))) return true;
            pause(500);
        }
        System.out.println(">>> temp: still on the character form after Submit (" + snackbar() + ")");
        return false;
    }

    public boolean pickGender(String gender) {
        try {
            scrollToId("gender");
            WebElement dd = driver.findElement(byId("gender")).findElement(byId("autoCompleteDropDown"));
            org.openqa.selenium.Rectangle r = dd.getRect();
            dd.click();
            pause(1200);
            List<WebElement> opt = driver.findElements(AppiumBy.xpath("//*[@text='" + gender + "' and not(contains(@resource-id,'autoCompleteDropDown'))]"));
            if (!opt.isEmpty()) {
                opt.get(opt.size() - 1).click();
            } else {
                // The Spinner popup is a separate window UiAutomator does not always expose: the
                // items (gender_type2: Male, Female, Non-Binary) sit right under the field, each
                // about 0.91 × the field's height tall (measured on a 1080×2400 screen).
                int idx = List.of("Male", "Female", "Non-Binary").indexOf(gender);
                if (idx < 0) return false;
                int y = r.getY() + r.getHeight() + (int) (r.getHeight() * (0.64 + 0.91 * idx));
                driver.executeScript("mobile: clickGesture", java.util.Map.of("x", r.getX() + r.getWidth() / 2, "y", y));
            }
            pause(800);
            String now = driver.findElement(byId("gender")).findElement(byId("autoCompleteDropDown")).getText();
            return gender.equalsIgnoreCase(now == null ? "" : now.trim());
        } catch (Exception e) { return false; }
    }

    // ───────────────────────── helpers ─────────────────────────

    public String snackbar() {
        try {
            List<WebElement> s = driver.findElements(byId("snackbar_text"));
            if (!s.isEmpty()) return s.get(0).getText();
            List<WebElement> t = driver.findElements(By.xpath("//android.widget.Toast"));
            if (!t.isEmpty()) return t.get(0).getText();
        } catch (Exception ignored) { }
        return "";
    }

    public String waitSnackbar(String contains, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            String s = snackbar();
            if (!s.isEmpty() && (contains == null || s.toLowerCase().contains(contains.toLowerCase()))) return s;
            pause(300);
        }
        return "";
    }

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
