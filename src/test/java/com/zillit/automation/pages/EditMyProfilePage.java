package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings → "Edit My Profile", EDIT PREFERENCE tab.
 *
 * <p>Ids confirmed by walking the screen: two tabs
 * ({@code editProfileTabText} / {@code editPreferenceTabText}); the avatar
 * {@code user_image} with its camera badge {@code changeImage}; exposed
 * dropdowns {@code selectUnit} and {@code selectGender}; {@code addMoreField}
 * opening a dialog of {@code editText1} / {@code editText2} with
 * {@code leftButton} / {@code rightButton}; and saved rows in
 * {@code recMoreField} carrying {@code titleTv}, {@code descriptionTv},
 * {@code imgEdit} and {@code imgDelete}.
 *
 * <p><b>Field validation lives in an {@code error} ATTRIBUTE, not in the text.</b>
 * "Please enter title to continue" renders as a tooltip balloon that has no text
 * node at all — reading the tree returns nothing. The message is carried on the
 * input as {@code error="..."} alongside {@code content-invalid="true"}, so it is
 * read from the page source instead. Errors also LINGER until the field is
 * revalidated, so an assertion must be made against the specific field right
 * after the tap that provoked it.
 *
 * <p><b>Comparing profile images needs the thumbnail, and needs it settled.</b>
 * The full-screen viewer keeps repainting — successive screenshots of
 * {@code imageVw} differ by a few percent forever — so it cannot be hashed.
 * {@code user_image} on the form is stable once loaded, but reads a DIFFERENT
 * value while still loading, so {@link #settledAvatarHash} waits for two
 * consecutive identical samples before returning.
 *
 * <p><b>Camera and gallery are in-app</b>, the same components the watermark
 * screen uses: {@code captureBtn}, the Fallery picker, and a shared preview whose
 * {@code sendBtn} returns the image.
 */
public class EditMyProfilePage extends BasePage {

    /** Confirmed on the app — note the trailing full stop. */
    public static final String PROFILE_UPDATED = "Profile updated successfully.";
    public static final String TITLE_REQUIRED = "Please enter title to continue";
    public static final String DESCRIPTION_REQUIRED = "Please Enter Description";
    public static final String DELETE_CONFIRM = "Are you sure you want to delete?";

    // ---- EDIT PROFILE tab. Read off the running app: the app says "should be OF
    // at least", which the written steps omit, and the same wording is used on
    // Pre-Approved Users.
    public static final String FIRST_NAME_EMPTY = "First Name cannot be empty";
    public static final String FIRST_NAME_LENGTH =
            "First Name should be of at least 3 characters or at most 15 characters.";
    public static final String LAST_NAME_EMPTY = "Last Name cannot be empty";
    public static final String LAST_NAME_LENGTH =
            "Last Name should be of at least 3 characters or at most 15 characters.";
    public static final String SELECT_DESIGNATION = "Please select designation";
    /** Stable ends of the department warning; the tool names inside it vary by project. */
    public static final String DEPT_WARNING_START = "If you change department";
    public static final String DEPT_WARNING_END = "Do you still want to change Department?";
    /** Every Zillit mailing address ends with this. */
    public static final String ZILLIT_MAIL_SUFFIX = "@zillit.info";

    /** Gender options, in the order the dropdown renders them. */
    public static final List<String> GENDERS = List.of("Male", "Female", "Non-Binary");

    private static final Pattern ERROR_ATTR = Pattern.compile("error=\"([^\"]*)\"");

    public EditMyProfilePage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    sleep(1500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    public boolean openEditMyProfile() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"Edit My Profile\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"Edit My Profile\")]"
                                    + "/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to the EDIT PREFERENCE tab from wherever the app is, retrying each hop. */
    public boolean ensureOnEditPreference(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOnPreferenceTab(Duration.ofMillis(700))) {
                return true;
            }
            if (isDialogOpen(Duration.ofMillis(300))) {
                tapDialogLeft();
                sleep(1200);
                continue;
            }
            if (isOnFullImageView(Duration.ofMillis(300)) || isChooserOpen(Duration.ofMillis(300))
                    || isOnCamera(Duration.ofMillis(300)) || isOnPreview(Duration.ofMillis(300))
                    || isOnGallery(Duration.ofMillis(300))) {
                try {
                    driver.navigate().back();
                } catch (WebDriverException ignored) {
                }
                sleep(1500);
                continue;
            }
            if (isLoaded(Duration.ofMillis(500))) {
                openPreferenceTab();
                sleep(1500);
                continue;
            }
            if (onSettingsTabScreen(Duration.ofMillis(400))) {
                openEditMyProfile();
                sleep(2000);
                continue;
            }
            if (hasBottomNav()) {
                openSettingsTab();
                sleep(1500);
                continue;
            }
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            sleep(900);
        }
        return false;
    }

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the profile screen ----------------

    public boolean isLoaded(Duration timeout) {
        return headerIs("EDIT MY PROFILE", timeout);
    }

    public boolean openPreferenceTab() {
        return tapById("editPreferenceTabText");
    }

    /** Keyed on a field that exists only on the preference tab. */
    public boolean isOnPreferenceTab(Duration timeout) {
        return presentWithin("selectGender", timeout);
    }

    public boolean tapBack() {
        return tapById("backBtn");
    }

    /** Submit appears only once something has changed. */
    public boolean tapSubmit() {
        return tapById("submitBtn");
    }

    // ---------------- avatar ----------------

    public boolean tapCameraIcon() {
        return tapById("changeImage");
    }

    public boolean tapAvatar() {
        return tapById("user_image");
    }

    public boolean isOnFullImageView(Duration timeout) {
        return presentWithin("imageVw", timeout);
    }

    /**
     * A stable fingerprint of the avatar thumbnail.
     *
     * <p>Samples until two consecutive screenshots hash the same, because the
     * image renders progressively — sampling too early returns a partly-loaded
     * frame, which then differs from itself moments later and makes any
     * comparison meaningless.
     *
     * @return the hash, or "" if it never settled
     */
    public String settledAvatarHash(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String previous = null;
        while (System.currentTimeMillis() < deadline) {
            String current = avatarHash();
            if (current != null && current.equals(previous)) {
                return current;
            }
            previous = current;
            sleep(1000);
        }
        return "";
    }

    private String avatarHash() {
        try {
            WebElement img = driver.findElement(byId("user_image"));
            byte[] png = img.getScreenshotAs(OutputType.BYTES);
            if (png == null || png.length == 0) {
                return null;
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(png);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- image source chooser / camera / gallery ----------------

    public boolean isChooserOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("tvLabel")).isEmpty()
                        && !driver.findElements(byId("tvTitle")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    public List<String> chooserOptions() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement v : driver.findElements(byId("tvLabel"))) {
                String t = safeText(v);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    public boolean chooseSource(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'tvLabel') and @text=\"" + label + "\"]")))
                    .click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnCamera(Duration timeout) {
        return presentWithin("captureBtn", timeout);
    }

    public boolean tapCapture() {
        return tapById("captureBtn");
    }

    public boolean isOnGallery(Duration timeout) {
        return presentWithin("recyclerViewBuckets", timeout)
                || presentWithin("recyclerViewBucketContent", Duration.ofMillis(300));
    }

    public boolean openFirstFolder() {
        return tapXpath("(//*[contains(@resource-id,'constraintLayoutBucketItemView')])[1]");
    }

    public boolean selectFirstPhoto() {
        return tapXpath("(//*[contains(@resource-id,'constraintLayoutPhotoContainer')])[1]");
    }

    public boolean tapGallerySend() {
        return tapById("floatingButtonSendMedia");
    }

    public boolean isOnPreview(Duration timeout) {
        return presentWithin("sendBtn", timeout);
    }

    public boolean tapPreviewSend() {
        return tapById("sendBtn");
    }

    // ---------------- unit and gender dropdowns ----------------

    public String unit() {
        return dropdownText("selectUnit");
    }

    public String gender() {
        return dropdownText("selectGender");
    }

    private String dropdownText(String container) {
        try {
            WebElement inner = driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + container
                            + "')]//*[contains(@resource-id,'autoCompleteDropDown')]"));
            return String.valueOf(safeText(inner)).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Opens a dropdown by its ARROW — tapping the field only takes focus. */
    public boolean openDropdown(String container) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + container
                            + "')]//*[contains(@resource-id,'text_input_end_icon')]")).click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Taps a row in an open dropdown by INDEX.
     *
     * <p>The expanded list is absent from the accessibility tree, so rows are hit
     * by coordinate measured off the closed field: the first sits about 0.61 of a
     * field-height below it, and rows repeat every 0.885. Same geometry as the
     * unit dropdown elsewhere in the app, re-verified here.
     */
    public boolean selectDropdownRow(String container, int index) {
        try {
            Rectangle r = driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + container
                            + "')]//*[contains(@resource-id,'autoCompleteDropDown')]")).getRect();
            int h = r.getHeight();
            int firstRowCentre = r.getY() + h + (int) (h * 0.61);
            int rowStep = (int) (h * 0.885);
            int x = r.getX() + Math.max(40, r.getWidth() / 6);
            tapAt(x, firstRowCentre + index * rowStep);
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- Add More Details ----------------

    // ---------------- EDIT PROFILE tab ----------------

    public boolean openProfileTab() {
        return tapById("editProfileTabText");
    }

    /** Keyed on the name fields, which exist only on this tab. */
    public boolean isOnProfileTab(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("editFirstName")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public String firstName() {
        return nestedText("editFirstName");
    }

    public String lastName() {
        return nestedText("editLastName");
    }

    /** The read-only Zillit mailing address; "" when the field is absent or blank. */
    public String zillitEmail() {
        return nestedText("zillitEmailTxtVw");
    }

    public String department() {
        return textOf("departmentText");
    }

    public String designation() {
        return textOf("designationText");
    }

    public boolean typeFirstName(String text) {
        return typeNested("editFirstName", text);
    }

    public boolean typeLastName(String text) {
        return typeNested("editLastName", text);
    }

    public boolean clearFirstName() {
        return clearNested("editFirstName");
    }

    public boolean clearLastName() {
        return clearNested("editLastName");
    }

    public boolean openDepartmentPicker() {
        return tapById("select_department");
    }

    public boolean openDesignationPicker() {
        return tapById("select_designation");
    }

    /**
     * True when the "change department" warning is up.
     *
     * <p>Shares {@code message}/{@code leftButton}/{@code rightButton} with the
     * delete confirmation, so callers should check the wording too rather than
     * assume which dialog they are looking at.
     */
    public boolean isDepartmentWarningShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            String m = confirmMessage();
            if (m != null && m.startsWith(DEPT_WARNING_START)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    // ---------------- department / designation picker ----------------

    public boolean isOnPicker(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("commonRecyclerView")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean searchPicker(String text) {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.click();
            box.sendKeys(text);
            hideKeyboard();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public List<String> pickerOptions() {
        List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement v : driver.findElements(byId("valueTextView"))) {
                String t = safeText(v);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /** Picks by EXACT text — a substring match would take the wrong row. */
    public boolean choosePickerValue(String value) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'valueTextView') and @text=\"" + value + "\"]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Name shown on the Settings tab header, used to confirm a rename landed. */
    public String settingsUserName() {
        return textOf("user_name_settings");
    }

    /** Designation shown on the Settings tab header. */
    public String settingsUserDesignation() {
        return textOf("user_designation_settings");
    }

    private String nestedText(String container) {
        try {
            for (WebElement c : driver.findElements(byId(container))) {
                for (WebElement inner : c.findElements(byId("editTextInput"))) {
                    String t = safeText(inner);
                    if (t != null) {
                        return t.trim();
                    }
                }
            }
        } catch (WebDriverException ignored) {
        }
        return "";
    }

    private boolean typeNested(String container, String text) {
        try {
            WebElement f = driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + container
                            + "')]//*[contains(@resource-id,'editTextInput')]"));
            f.click();
            sleep(400);
            f.sendKeys(text);
            hideKeyboard();
            sleep(700);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Empties a field with the DELETE key — {@code clear()} hangs on these inputs,
     * blocking until the command times out.
     */
    private boolean clearNested(String container) {
        try {
            WebElement f = driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + container
                            + "')]//*[contains(@resource-id,'editTextInput')]"));
            f.click();
            sleep(400);
            String cur = safeText(f);
            int len = cur == null ? 0 : cur.length();
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.MOVE_END));
            for (int i = 0; i < len + 5; i++) {
                driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.DEL));
            }
            hideKeyboard();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapAddMoreDetails() {
        return tapById("addMoreField");
    }

    public boolean isDialogOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("editText1")).isEmpty()
                        && !driver.findElements(byId("rightButton")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    public String dialogHeader() {
        return textOf("header");
    }

    /** Cancel on both the add and update dialogs. */
    public boolean tapDialogLeft() {
        return tapById("leftButton");
    }

    /** Add on the add dialog, Update on the edit dialog, Yes on delete. */
    public boolean tapDialogRight() {
        return tapById("rightButton");
    }

    public boolean typeTitle(String text) {
        return fillField("editText1", text);
    }

    public boolean typeDescription(String text) {
        return fillField("editText2", text);
    }

    public boolean clearTitle() {
        return clearField("editText1");
    }

    public boolean clearDescription() {
        return clearField("editText2");
    }

    public String titleValue() {
        return innerText("editText1");
    }

    public String descriptionValue() {
        return innerText("editText2");
    }

    /**
     * The validation message attached to a dialog field, or "" when clean.
     *
     * <p>Read from the {@code error} attribute in the page source. There is no
     * text node for it — the message is drawn as a tooltip outside the
     * accessibility tree entirely.
     */
    public String fieldError(String container) {
        try {
            String src = driver.getPageSource();
            int idx = src.indexOf(container);
            if (idx < 0) {
                return "";
            }
            // Look ahead from the container to the next error= attribute, which
            // belongs to the input nested inside it.
            Matcher m = ERROR_ATTR.matcher(src);
            String best = "";
            while (m.find()) {
                if (m.start() > idx) {
                    best = m.group(1);
                    break;
                }
            }
            return best;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public String titleError() {
        return fieldError("editText1");
    }

    public String descriptionError() {
        return fieldError("editText2");
    }

    /** True while neither dialog field is on screen. */
    public boolean dialogFieldsGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (driver.findElements(byId("editText1")).isEmpty()
                        && driver.findElements(byId("editText2")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    // ---------------- saved detail rows ----------------

    /** Saved details, title → description. */
    public Map<String, String> details() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            List<WebElement> titles = driver.findElements(byId("titleTv"));
            List<WebElement> descs = driver.findElements(byId("descriptionTv"));
            for (WebElement t : titles) {
                String title = safeText(t);
                if (title != null && !title.isBlank()) {
                    out.put(title.trim(), nearest(t, descs));
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    public boolean hasDetail(String title) {
        return details().keySet().stream().anyMatch(t -> t.equalsIgnoreCase(title));
    }

    public boolean waitForDetail(String title, boolean shouldExist, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (hasDetail(title) == shouldExist) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(600);
        }
    }

    public boolean tapDetailEdit(String title) {
        return clickRowIcon(title, "imgEdit");
    }

    public boolean tapDetailDelete(String title) {
        return clickRowIcon(title, "imgDelete");
    }

    /** Row icons carry no text, so they are paired to a title by vertical position. */
    private boolean clickRowIcon(String title, String iconId) {
        try {
            WebElement label = null;
            for (WebElement t : driver.findElements(byId("titleTv"))) {
                String txt = safeText(t);
                if (txt != null && txt.trim().equalsIgnoreCase(title)) {
                    label = t;
                    break;
                }
            }
            if (label == null) {
                return false;
            }
            Rectangle lr = label.getRect();
            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement icon : driver.findElements(byId(iconId))) {
                int delta = Math.abs(icon.getRect().getY() - lr.getY());
                if (delta < bestDelta && delta < Math.max(lr.getHeight() * 4, 200)) {
                    bestDelta = delta;
                    best = icon;
                }
            }
            if (best == null) {
                return false;
            }
            best.click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- delete confirmation ----------------

    public boolean isDeleteConfirmShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (DELETE_CONFIRM.equalsIgnoreCase(textOf("message"))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    public String confirmMessage() {
        return textOf("message");
    }

    // ---------------- snackbars ----------------

    public String currentSnackbar() {
        try {
            List<WebElement> found = driver.findElements(byId("snackbar_text"));
            return found.isEmpty() ? null : safeText(found.get(0));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            String s = currentSnackbar();
            if (s != null && s.contains(message)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean waitForSnackbarToClear(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (currentSnackbar() == null) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    // ---------------- helpers ----------------

    public String headerText() {
        return textOf("headerText");
    }

    private boolean headerIs(String expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (expected.equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private WebElement innerInput(String container) {
        try {
            return driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + container
                            + "')]//*[contains(@resource-id,'editTextInput')]"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    private String innerText(String container) {
        WebElement e = innerInput(container);
        return e == null ? "" : String.valueOf(safeText(e)).trim();
    }

    // Named fillField, not typeInto: BasePage already declares a protected
    // typeInto(String,String) and a private override here is a weaker-access clash.
    private boolean fillField(String container, String text) {
        WebElement field = innerInput(container);
        if (field == null) {
            return false;
        }
        try {
            field.click();
            sleep(400);
            field.sendKeys(text);
            hideKeyboard();
            sleep(700);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Empties a field with DELETE presses — {@code clear()} hangs on these inputs. */
    private boolean clearField(String container) {
        WebElement field = innerInput(container);
        if (field == null) {
            return false;
        }
        try {
            field.click();
            sleep(400);
            String current = safeText(field);
            int len = current == null ? 0 : current.length();
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.MOVE_END));
            for (int i = 0; i < len + 4; i++) {
                driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.DEL));
            }
            hideKeyboard();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean presentWithin(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId(id)).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(900);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean tapXpath(String xpath) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(xpath)))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(120),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private String nearest(WebElement anchor, List<WebElement> candidates) {
        try {
            Rectangle ar = anchor.getRect();
            String best = "";
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement c : candidates) {
                int delta = Math.abs(c.getRect().getY() - ar.getY());
                if (delta < bestDelta && delta < ar.getHeight() * 4) {
                    bestDelta = delta;
                    best = safeText(c);
                }
            }
            return best == null ? "" : best.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String textOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? "" : String.valueOf(safeText(found.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String safeText(WebElement e) {
        try {
            return e.getText();
        } catch (WebDriverException stale) {
            return "";
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
