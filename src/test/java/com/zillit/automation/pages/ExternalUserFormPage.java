package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * EXTERNAL USERS — add, update and the detail screen, which are one Activity.
 *
 * <p>{@code AddUpdateExternalUserPage} serves all three. Which one you are on is told by
 * the header: "ADD EXTERNAL USER", "UPDATE EXTERNAL USER" or "USER DETAILS".
 *
 * <p><b>Every field is a composite widget.</b> The layout {@code include}s
 * {@code edit_text_widget_no_margins} and {@code edit_text_drop_down} once per field, so
 * the INNER ids repeat across the screen — Full Name, Email and Phone are all
 * {@code editTextInput}, and Gender, User Type, Department, Designation and Country are
 * all {@code autoCompleteDropDown}. A lookup by inner id would hit whichever came first.
 * So every field here is reached through its own outer container id
 * ({@code etFullName}, {@code etEmail}, {@code gender}, {@code userType},
 * {@code department}, {@code designation}, {@code countryCode}, {@code etPhone}) and only
 * then descended into.
 *
 * <p><b>The dropdowns cannot be typed into.</b> They are {@code AutoCompleteTextView} with
 * {@code inputType="none"} in a {@code CustomExposedDropdownMenu}, which UiAutomator
 * reports as {@code android.widget.Spinner}, and their value is read from the view's text.
 * Two of them — Gender and User Type — open an exposed-dropdown popup in a window of its
 * own. The other three — Department, Designation and Country — do not: they have their own
 * click handlers that open a bottom-sheet chooser instead, and Designation is filled from
 * the chosen Department, so it has nothing to offer until a Department is picked.
 */
public class ExternalUserFormPage extends BasePage {

    public static final String HEADER_ADD    = "ADD EXTERNAL USER";
    public static final String HEADER_UPDATE = "UPDATE EXTERNAL USER";
    public static final String HEADER_DETAIL = "USER DETAILS";

    /** The three user types, in the order the dropdown lists them. */
    public static final String TYPE_CREW   = "Crew Member";
    public static final String TYPE_VENDOR = "Vendor";
    public static final String TYPE_OTHERS = "others";

    /** Gender options, from {@code R.array.gender_type2}. */
    public static final List<String> GENDERS = List.of("Male", "Female", "Non-Binary");

    /** The app's own refusals, so a case can name the one it expects. */
    public static final String ERR_NAME       = "Please enter full name";
    public static final String ERR_EMAIL      = "Please enter email";
    public static final String ERR_EMAIL_BAD  = "Please enter valid email";
    public static final String ERR_TYPE       = "User type is required";
    public static final String ERR_DEPARTMENT = "Please select department";
    public static final String ERR_DESIGNATION= "Please select designation";
    public static final String ERR_COUNTRY    = "Please choose country";
    public static final String ERR_PHONE      = "Please enter valid phone number";

    /** Email is capped by an InputFilter, not by validation. */
    public static final int EMAIL_MAX = 50;

    public ExternalUserFormPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("etFullName") || displayed("etEmail")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public String header() { return rawText("headerText"); }

    public boolean onAddScreen()    { return HEADER_ADD.equalsIgnoreCase(trim(header())); }
    public boolean onUpdateScreen() { return HEADER_UPDATE.equalsIgnoreCase(trim(header())); }
    public boolean onDetailScreen() { return HEADER_DETAIL.equalsIgnoreCase(trim(header())); }

    // ───────────────────────── plain text fields ─────────────────────────

    public boolean setFullName(String v) { return fillIn("etFullName", v); }
    public boolean setEmail(String v)    { return fillIn("etEmail", v); }
    public boolean setPhone(String v)    { return fillIn("etPhone", v); }
    public boolean setOtherType(String v){ return fillIn("userTypeOther", v); }

    public String fullName()  { return innerText("etFullName", "editTextInput"); }
    public String email()     { return innerText("etEmail", "editTextInput"); }
    public String phone()     { return innerText("etPhone", "editTextInput"); }
    public String otherType() { return innerText("userTypeOther", "editTextInput"); }

    // ───────────────────────── dropdown values ─────────────────────────

    public String gender()      { return innerText("gender", "autoCompleteDropDown"); }
    public String userType()    { return innerText("userType", "autoCompleteDropDown"); }
    public String department()  { return innerText("department", "autoCompleteDropDown"); }
    public String designation() { return innerText("designation", "autoCompleteDropDown"); }
    public String country()     { return innerText("countryCode", "autoCompleteDropDown"); }

    // ───────────────────────── what is on screen ─────────────────────────

    public boolean genderShown()      { return displayed("gender"); }
    public boolean departmentShown()  { return displayed("department"); }
    public boolean designationShown() { return displayed("designation"); }
    public boolean otherTypeShown()   { return displayed("userTypeOther"); }
    public boolean phoneShown()       { return displayed("phoneLi"); }
    public boolean moreInfoShown()    { return displayed("moreFileHolder"); }
    public boolean saveShown()        { return displayed("btnSave"); }

    /** What the form is showing, for a failure message that can be acted on. */
    public String visibility() {
        return "gender=" + genderShown()
                + " department=" + departmentShown()
                + " designation=" + designationShown()
                + " otherType=" + otherTypeShown()
                + " phone=" + phoneShown()
                + " save=" + saveShown();
    }

    // ───────────────────────── the two dropdowns ─────────────────────────

    /**
     * Picks a User Type.
     *
     * <p>Opens the exposed-dropdown popup and taps the item. The popup is its own window,
     * which Appium sees but a plain UiAutomator dump of the activity does not.
     */
    /** User Type, by the order the dropdown lists them: Crew Member, Vendor, others. */
    public boolean chooseUserType(String label) {
        int i = TYPE_CREW.equalsIgnoreCase(label) ? 0
              : TYPE_VENDOR.equalsIgnoreCase(label) ? 1
              : TYPE_OTHERS.equalsIgnoreCase(label) ? 2 : -1;
        if (i < 0) return false;
        return pickByPosition("userType", i, label);
    }

    /** Gender, in the order of R.array.gender_type2: Male, Female, Non-Binary. */
    public boolean chooseGender(String label) {
        int i = GENDERS.indexOf(label);
        return i >= 0 && pickByPosition("gender", i, label);
    }

    /** The options a dropdown is currently offering, with its popup open. */
    public List<String> openDropdownOptions(String containerId) {
        if (!tapField(containerId)) return List.of();
        pause(1500);
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(
                    AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public List<String> userTypeOptions() { return openDropdownOptions("userType"); }

    /**
     * Picks from an exposed dropdown BY POSITION, because its items cannot be found.
     *
     * <p>The popup opens and renders — a screenshot shows all three user types listed — but
     * it is entirely absent from the accessibility tree: with it open, the page source
     * holds no PopupWindow, no ListView and no CheckedTextView, and does not grow by a
     * single character. So there is nothing to match a label against, and every attempt to
     * tap an item by its text failed while the item sat visible on the screen.
     *
     * <p>(That is worth knowing beyond this test: a view missing from the accessibility
     * tree is a view a screen reader cannot announce, so these dropdowns are unreachable
     * with TalkBack. It is the app's behaviour, not the harness's.)
     *
     * <p>So the item is tapped where it is drawn. The popup anchors directly under its
     * field and lists items at the Material standard 48dp each, which is why the height is
     * computed from the device's density rather than hard-coded: 48dp is 126px at 420dpi
     * and something else on the next device. The result is then VERIFIED by reading the
     * field back, so a mis-aimed tap fails loudly instead of silently selecting the wrong
     * person's user type.
     *
     * @param index which item, counting from 0 in the order the dropdown lists them
     */
    private boolean pickByPosition(String containerId, int index, String expected) {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (!tapField(containerId)) return false;
            pause(1400);
            org.openqa.selenium.Rectangle r;
            try {
                List<WebElement> boxes = driver.findElements(byId(containerId));
                if (boxes.isEmpty()) return false;
                r = boxes.get(0).getRect();
            } catch (Exception e) { return false; }

            int itemH = itemHeightPx();
            // A small gap sits between the field and the first row; attempt 0 assumes none,
            // and the retries nudge down by a third of a row each time.
            int gap = attempt * (itemH / 3);
            int x = r.getX() + r.getWidth() / 2;
            int y = r.getY() + r.getHeight() + gap + itemH * index + itemH / 2;
            tapAt(x, y);
            pause(1400);

            String now = innerText(containerId, "autoCompleteDropDown");
            if (now != null && now.trim().equalsIgnoreCase(expected)) return true;
            // Something else was selected, or nothing was: close whatever is open and
            // retry with a different offset rather than leaving a popup up.
            dismissPopup();
        }
        return false;
    }

    /** One dropdown row, in pixels: the Material standard 48dp at this device's density. */
    private int itemHeightPx() {
        try {
            Object d = driver.getCapabilities().getCapability("deviceScreenDensity");
            if (d != null) {
                int dpi = Integer.parseInt(String.valueOf(d).trim());
                if (dpi > 0) return Math.round(48f * dpi / 160f);
            }
        } catch (Exception ignored) { }
        return 126;   // 420dpi, the density this was measured on
    }

    private void tapAt(int x, int y) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(0));
        tap.addAction(new org.openqa.selenium.interactions.Pause(
                finger, Duration.ofMillis(120)));
        tap.addAction(finger.createPointerUp(0));
        driver.perform(List.of(tap));
    }

    // ───────────────── the three bottom-sheet choosers ─────────────────

    /**
     * Picks a Department from its bottom-sheet chooser.
     *
     * @param label the department name, or null to take the first on offer
     */
    public boolean chooseDepartment(String label) { return pickFromSheet("department", label); }

    /** Designation — only has anything to offer once a Department is chosen. */
    public boolean chooseDesignation(String label) { return pickFromSheet("designation", label); }

    public boolean chooseCountry(String label) { return pickFromSheet("countryCode", label); }

    /**
     * What a chooser is offering, with its screen open. Empty when it opened nothing.
     *
     * <p>Department, Designation and Country do not open a sheet or a popup at all: they
     * launch {@code CommonSearchListActivity}, a screen of its own with a search box and a
     * list ({@code commonRecyclerView}). That is why its rows ARE in the accessibility tree
     * while the exposed dropdowns' are not, and why they can be matched by text.
     */
    public List<String> chooserOptions(String containerId) {
        if (!tapField(containerId)) return List.of();
        pause(2500);
        List<String> out = new ArrayList<>();
        try {
            for (WebElement list : driver.findElements(byId("commonRecyclerView"))) {
                for (WebElement e : list.findElements(
                        AppiumBy.className("android.widget.TextView"))) {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) out.add(t.trim());
                }
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** True while the chooser screen is up. */
    public boolean chooserShown() { return displayed("commonRecyclerView"); }

    private boolean pickFromSheet(String containerId, String label) {
        if (!tapField(containerId)) return false;
        pause(2500);
        try {
            List<WebElement> lists = driver.findElements(byId("commonRecyclerView"));
            if (lists.isEmpty()) {
                // Nothing opened. For Designation with no Department chosen that is the
                // app behaving correctly — its list is built from the department — so the
                // caller is told plainly rather than this being treated as a failure.
                System.out.println(">>> external user: " + containerId
                        + " opened no chooser (CommonSearchListActivity did not appear)");
                return false;
            }
            for (WebElement e : lists.get(0).findElements(
                    AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t == null || t.isBlank()) continue;
                if (label == null || t.trim().contains(label)) {
                    e.click();
                    pause(2500);
                    return true;
                }
            }
            System.out.println(">>> external user: " + containerId + " offered "
                    + chooserRowsFor(lists.get(0)) + ", none matching " + label);
        } catch (Exception ignored) { }
        dismissPopup();
        return false;
    }

    private List<String> chooserRowsFor(WebElement list) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : list.findElements(
                    AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ───────────────────────── more information ─────────────────────────

    public boolean addMoreInfo() { return clickInner("addMoreField", "btnAdd"); }

    public String moreInfoTitle() { return innerText("addMoreField", "tvTitle"); }

    // ───────────────────────────── save ─────────────────────────────

    /** The Save button's label: "Submit" when adding, "Update" when editing. */
    public String saveLabel() { return innerText("btnSave", "buttonBtn"); }

    public boolean save() { return clickInner("btnSave", "buttonBtn"); }

    public boolean stillOpen() { return isLoaded(Duration.ofSeconds(3)); }

    /** The inline error a field is showing, or null. */
    public String errorOn(String containerId) {
        try {
            List<WebElement> boxes = driver.findElements(byId(containerId));
            if (boxes.isEmpty()) return null;
            for (WebElement e : boxes.get(0).findElements(AppiumBy.xpath(".//*"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) {
                    String s = t.trim();
                    // TextInputLayout renders the error as a sibling TextView under the box.
                    if (s.toLowerCase().startsWith("please") || s.toLowerCase().contains("required")) {
                        return s;
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ───────────────────── detail-mode routing targets ─────────────────────

    /** Taps the email in detail mode, which hands over to the Email module. */
    public boolean tapEmail() { return clickInner("etEmail", "editTextInput"); }

    /** Taps the phone in detail mode, which should open the dialer. */
    public boolean tapPhone() { return clickInner("etPhone", "editTextInput"); }

    // ───────────────────────────── helpers ─────────────────────────────

    /** Types into a composite field by descending from its container. */
    private boolean fillIn(String containerId, String value) {
        try {
            List<WebElement> boxes = driver.findElements(byId(containerId));
            if (boxes.isEmpty()) return false;
            WebElement input = boxes.get(0).findElement(
                    AppiumBy.className("android.widget.EditText"));
            input.click();
            input.clear();
            if (!value.isEmpty()) input.sendKeys(value);
            hideKeyboard();
            pause(700);
            return true;
        } catch (Exception e) { return false; }
    }

    private String innerText(String containerId, String innerId) {
        try {
            List<WebElement> boxes = driver.findElements(byId(containerId));
            if (boxes.isEmpty()) return null;
            List<WebElement> inner = boxes.get(0).findElements(byId(innerId));
            if (inner.isEmpty()) return null;
            String t = inner.get(0).getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception e) { return null; }
    }

    private boolean clickInner(String containerId, String innerId) {
        try {
            List<WebElement> boxes = driver.findElements(byId(containerId));
            if (boxes.isEmpty()) return false;
            List<WebElement> inner = boxes.get(0).findElements(byId(innerId));
            if (inner.isEmpty()) return false;
            inner.get(0).click();
            pause(1500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Taps a field's own control — the dropdown text, falling back to its end icon. */
    private boolean tapField(String containerId) {
        try {
            List<WebElement> boxes = driver.findElements(byId(containerId));
            if (boxes.isEmpty()) return false;
            WebElement box = boxes.get(0);
            scrollToId(containerId);
            List<WebElement> drop = box.findElements(byId("autoCompleteDropDown"));
            if (!drop.isEmpty() && drop.get(0).isDisplayed()) {
                drop.get(0).click();
                return true;
            }
            List<WebElement> icon = box.findElements(byId("text_input_end_icon"));
            if (!icon.isEmpty() && icon.get(0).isDisplayed()) {
                icon.get(0).click();
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean tapExactText(String label) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"))) {
                if (e.isDisplayed()) { e.click(); return true; }
            }
            // Fall back to a contains match: a label may carry trailing spaces.
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + label + "\")"))) {
                if (e.isDisplayed()) { e.click(); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private void dismissPopup() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        pause(800);
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private String rawText(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
