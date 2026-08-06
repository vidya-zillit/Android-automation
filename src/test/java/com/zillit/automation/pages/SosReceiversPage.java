package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings → Admin Settings → "Set/View SOS Receivers".
 *
 * <p>Three screens, all confirmed by walking them on the device:
 *
 * <ol>
 *   <li><b>SET/VIEW SOS RECEIVERS</b> — the form plus the current receiver list.
 *       The form is {@code selectMember} (whose label is {@code tvMemName}) and
 *       {@code etDesignation}, submitted with {@code btnSubmit}. Saved receivers
 *       live in {@code rvContact}, each row carrying {@code userNameTv},
 *       {@code designationTv}, {@code contactTypeTv} and the two action icons
 *       {@code btnEdit} / {@code btnDel}. Two tabs sit above it, Member and
 *       Outsider; this page drives the Member tab.</li>
 *   <li><b>SELECT USERS</b> — the member picker. List is {@code recDepartments}
 *       with {@code nameTv} / {@code tvDesignation} per row, filtered by the
 *       shared {@code search_widget_edit_textvw}. Tapping a row ticks it
 *       ({@code icCheck}) and adds a chip to {@code horizontalRv}; the picker's
 *       own {@code buttonBtn} ("Submit") applies the choice and closes it.</li>
 *   <li><b>EDIT SOS RECEIVERS</b> — the same form prefilled, reached from
 *       {@code btnEdit} and left with {@code backBtn}.</li>
 * </ol>
 *
 * <p><b>The picker searches designation as well as name</b>, despite its hint
 * reading "Search By User Name" — verified on the device, where a designation
 * query kept the matching member and a nonsense query emptied the list.
 *
 * <p><b>Delete is immediate.</b> {@code btnDel} removes the receiver with no
 * confirmation dialog, so nothing needs to be acknowledged after tapping it.
 *
 * <p><b>The designation value is nested.</b> {@code etDesignation} is a wrapper;
 * its text lives in the {@code editTextInput} inside it, and reading the wrapper
 * directly returns an empty string.
 */
public class SosReceiversPage extends BasePage {

    /** What the member field reads when nothing is chosen. */
    public static final String MEMBER_PLACEHOLDER = "Select Member";
    /** What the designation field reads when nothing is chosen. */
    public static final String DESIGNATION_PLACEHOLDER = "Designation";
    /** Validation shown when Submit is tapped on an empty form. */
    public static final String FILL_ALL_DETAILS = "Please fill all details.";

    // ---- Outsider tab. All read off the running app, not the written spec:
    // the phone message says "minimum 5 and", not "minimum 5 number and", and
    // both of the others end in a full stop.
    public static final String PHONE_LENGTH =
            "Phone number must be minimum 5 and maximum 20 digit. Please enter correct phone number first";
    public static final String PROFILE_UPDATED = "Profile updated successfully.";
    /** A TOAST, not a snackbar — see {@link #toastShown}. */
    public static final String SELECT_COUNTRY_CODE = "Please Select Country Code First.";
    /** The only inline field error this form publishes as text. */
    public static final String SELECT_RELATION = "Please select relation";
    /** What the Outsider name field reads when empty. */
    public static final String NAME_PLACEHOLDER = "Name";
    /** What the relation field reads when nothing is chosen. */
    public static final String RELATION_PLACEHOLDER = "Select Relation";

    public SosReceiversPage(AndroidDriver driver) {
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
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/menuText\").textMatches(\"(?i)Settings\")"))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openSosReceivers() {
        return tapListRow("Set/View SOS Receivers");
    }

    private boolean tapListRow(String label) {
        // "Set/View SOS Receivers" sits near the bottom of a long Admin Settings
        // list that keeps its scroll position, so bring the row on screen first.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to the SOS receivers screen from wherever the app is, retrying each hop. */
    public boolean ensureOnSosReceivers(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            // Either tab counts as "arrived". Checking the Member-only field here
            // made this press BACK while sitting on the Outsider tab, walking the
            // app off the screen it had just been asked to reach.
            if (isOnReceiversScreen(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnMemberPicker(Duration.ofMillis(400)) || isOnEditScreen(Duration.ofMillis(400))) {
                tapBack();
                sleep(1500);
                continue;
            }
            if (isOnAdminSettings(Duration.ofMillis(400))) {
                openSosReceivers();
                sleep(2000);
                continue;
            }
            if (isOnSettingsTab(Duration.ofMillis(400))) {
                openAdminSettings();
                sleep(1500);
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

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
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

    // ---------------- the receivers screen ----------------

    /**
     * True on the SOS receivers screen.
     *
     * <p>Matched on the form's own controls rather than the header: the EDIT
     * screen reuses {@code selectMember} and {@code btnSubmit}, so the header is
     * checked too and must read SET/VIEW rather than EDIT.
     */
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("selectMember")).isEmpty()
                        && "SET/VIEW SOS RECEIVERS".equalsIgnoreCase(headerText())) {
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

    public boolean isLoaded() {
        return isLoaded(Duration.ofSeconds(20));
    }

    /**
     * True on the receivers screen under EITHER tab.
     *
     * <p>{@link #isLoaded} is Member-tab specific — it keys on
     * {@code selectMember}, which the Outsider tab does not have. Anything that
     * only needs "we are on the receivers screen" (the saved list, its edit and
     * delete icons, the back button) must use this instead, or it reports false
     * while the screen is plainly open.
     */
    public boolean isOnReceiversScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("SET/VIEW SOS RECEIVERS".equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean isOnEditScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("EDIT SOS RECEIVERS".equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean tapSubmit() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("btnSubmit")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Text currently shown in the member field ("Select Member" when unset). */
    public String memberField() {
        return textOf("tvMemName");
    }

    /**
     * Text currently shown in the designation field ("Designation" when unset).
     *
     * <p>Read from the {@code editTextInput} INSIDE {@code etDesignation} —
     * the wrapper itself reports no text.
     */
    public String designationField() {
        try {
            List<WebElement> wrappers = driver.findElements(byId("etDesignation"));
            if (!wrappers.isEmpty()) {
                List<WebElement> inner = wrappers.get(0).findElements(byId("editTextInput"));
                if (!inner.isEmpty()) {
                    return safeText(inner.get(0));
                }
                String cls = safeText(wrappers.get(0).findElement(By.className("android.widget.EditText")));
                if (cls != null && !cls.isBlank()) {
                    return cls;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return textOf("editTextInput");
    }

    /** True when both form fields are back at their placeholders. */
    public boolean formIsReset() {
        return MEMBER_PLACEHOLDER.equalsIgnoreCase(memberField())
                && DESIGNATION_PLACEHOLDER.equalsIgnoreCase(designationField());
    }

    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                for (WebElement e : driver.findElements(byId("snackbar_text"))) {
                    String t = safeText(e);
                    if (t != null && t.contains(message)) {
                        return true;
                    }
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    // ---------------- saved receiver list ----------------

    /** Saved receivers, name → designation. */
    public Map<String, String> receivers() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            List<WebElement> names = driver.findElements(byId("userNameTv"));
            List<WebElement> desigs = driver.findElements(byId("designationTv"));
            for (WebElement n : names) {
                String name = safeText(n);
                if (name != null && !name.isBlank()) {
                    out.put(name.trim(), nearest(n, desigs));
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /**
     * True if a receiver row mentions this name.
     *
     * <p>Deliberately a CONTAINS match. Outsider rows render as
     * "Harsh - (+9195011832)" — name and phone in one label — so an exact
     * comparison would report every outsider as missing.
     */
    public boolean isReceiverListed(String name) {
        String needle = name.toLowerCase();
        return receivers().keySet().stream()
                .anyMatch(n -> n.toLowerCase().contains(needle));
    }

    /** True while BOTH row icons are present for the named receiver. */
    public boolean receiverHasEditAndDelete(String name) {
        return rowIcon(name, "btnEdit") != null && rowIcon(name, "btnDel") != null;
    }

    public boolean tapEditIcon(String name) {
        WebElement icon = rowIcon(name, "btnEdit");
        if (icon == null) {
            return false;
        }
        try {
            icon.click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapDeleteIcon(String name) {
        WebElement icon = rowIcon(name, "btnDel");
        if (icon == null) {
            return false;
        }
        try {
            icon.click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The named row's icon, matched by vertical position.
     *
     * <p>The icons carry no text, so they cannot be located by label; pairing
     * them with the name by row geometry is what keeps the right row's icon from
     * being tapped when several receivers are listed.
     */
    private WebElement rowIcon(String name, String iconId) {
        try {
            WebElement label = null;
            String needle = name.toLowerCase();
            for (WebElement n : driver.findElements(byId("userNameTv"))) {
                String t = safeText(n);
                // CONTAINS, for the same reason as isReceiverListed: outsider rows
                // carry the phone number alongside the name.
                if (t != null && t.toLowerCase().contains(needle)) {
                    label = n;
                    break;
                }
            }
            if (label == null) {
                return null;
            }
            Rectangle lr = label.getRect();
            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement icon : driver.findElements(byId(iconId))) {
                Rectangle ir = icon.getRect();
                int delta = Math.abs(ir.getY() - lr.getY());
                if (delta < bestDelta && delta < Math.max(lr.getHeight() * 3, 120)) {
                    bestDelta = delta;
                    best = icon;
                }
            }
            return best;
        } catch (WebDriverException e) {
            return null;
        }
    }

    /** Waits until the named receiver shows in the list (the save is a round trip). */
    public boolean waitUntilReceiverListed(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (isReceiverListed(name)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    /** Waits until the named receiver is gone from the list. */
    public boolean waitUntilReceiverGone(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (!isReceiverListed(name)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    // ---------------- SELECT USERS picker ----------------

    public boolean openMemberPicker() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("selectMember")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnMemberPicker(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("SELECT USERS".equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** Members currently listed in the picker, name → designation. */
    public Map<String, String> pickerMembers() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            List<WebElement> names = driver.findElements(byId("nameTv"));
            List<WebElement> desigs = driver.findElements(byId("tvDesignation"));
            for (WebElement n : names) {
                String name = safeText(n);
                if (name != null && !name.isBlank()) {
                    out.put(name.trim(), nearest(n, desigs));
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    public boolean searchMember(String text) {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.click();
            box.sendKeys(text);
            hideKeyboard();
            sleep(2000);            // the list re-filters asynchronously
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean clearMemberSearch() {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.clear();
            hideKeyboard();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps a member row, which ticks it and adds a chip to the selected strip. */
    public boolean selectMember(String name) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + name
                                    + "\")]/ancestor::*[@clickable='true'][1]")))
                    .click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** True once the tapped row shows its tick. */
    public boolean memberIsTicked(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("icCheck")).isEmpty()) {
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

    /** The picker's own Submit — applies the selection and closes the picker. */
    public boolean confirmMemberSelection() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("buttonBtn")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- tabs ----------------

    public boolean openMemberTab() {
        return tapTab("Member");
    }

    public boolean openOutsiderTab() {
        return tapTab("Outsider");
    }

    private boolean tapTab(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + label + "\")")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** True when the named tab is the selected one. */
    public boolean tabSelected(String label) {
        try {
            for (WebElement t : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")"))) {
                if ("true".equals(t.getAttribute("selected"))) {
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    /** The Outsider form is up (relation field exists only there). */
    public boolean isOnOutsiderForm(Duration timeout) {
        return presentWithin("etRelation", timeout);
    }

    // ---------------- "mobile not updated" alert ----------------

    /**
     * The alert shown when the Outsider tab is opened while the signed-in user's
     * profile has NO mobile number.
     *
     * <p><b>One-shot per account.</b> Saving a number satisfies it permanently, so
     * on any later run the Outsider tab opens straight to the form. Callers must
     * treat its absence as normal, not as a failure.
     */
    public boolean isMobileAlertShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("leftButton")).isEmpty()
                        && !driver.findElements(byId("message")).isEmpty()) {
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

    public String alertMessage() {
        return textOf("message");
    }

    /** Cancel — dismisses the alert and drops back to the Member tab. */
    public boolean tapAlertCancel() {
        return tapById("leftButton");
    }

    /** Update — opens the UPDATE MOBILE NUMBER screen. */
    public boolean tapAlertUpdate() {
        return tapById("rightButton");
    }

    // ---------------- UPDATE MOBILE NUMBER ----------------

    public boolean isOnUpdateMobile(Duration timeout) {
        return presentWithin("phoneEdit", timeout);
    }

    public String updateMobileNumber() {
        return textOf("phoneEdit");
    }

    public String updateMobileCountryCode() {
        return textOf("activity_email_phone_number_page_country_code_field");
    }

    public boolean typeUpdateMobile(String digits) {
        return typeInto(byId("phoneEdit"), digits);
    }

    public boolean clearUpdateMobile() {
        return clearField(byId("phoneEdit"));
    }

    public boolean openUpdateMobileCountryPicker() {
        return tapById("activity_email_phone_number_page_country_code_field");
    }

    /**
     * Submit on the update-mobile screen.
     *
     * <p>The button is NOT rendered while the number field is empty, and the soft
     * keyboard covers it — so the keyboard is dismissed before looking.
     */
    public boolean tapUpdateMobileSubmit() {
        hideKeyboard();
        sleep(600);
        return tapById("submitBtn");
    }

    // ---------------- country picker ----------------

    public boolean isOnCountryPicker(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("SELECT COUNTRY ISD CODE".equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean searchCountry(String text) {
        return typeInto(byId("search_widget_edit_textvw"), text);
    }

    public List<String> countryOptions() {
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

    /**
     * Picks a country by EXACT name. Matching on "contains" is wrong here:
     * searching "India" also returns "British Indian Ocean Territory", and a
     * loose match takes whichever renders first.
     */
    public boolean chooseCountry(String name) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'valueTextView') and @text=\"" + name + "\"]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- Outsider form ----------------

    /**
     * Relation options, in the order the dropdown renders them.
     *
     * <p>Held as a constant because the opened list is INVISIBLE to the
     * accessibility tree — rows can only be tapped by position, so the index has
     * to come from somewhere. Confirmed against the live dropdown.
     */
    public static final List<String> RELATIONS = List.of(
            "Medics", "Security", "Production Office", "Police Dial",
            "Ambulance Dial", "Friends", "Family", "Other");

    public String outsiderName() {
        return nestedInputText("etName");
    }

    public String outsiderPhone() {
        return nestedInputText("etPhone");
    }

    public String outsiderCountryCode() {
        return textOf("tvCode");
    }

    public String relation() {
        return textOf("autoCompleteDropDown");
    }

    /** The always-visible guidance paragraph under the phone row. */
    public String guidanceText() {
        return textOf("tvMsg");
    }

    /**
     * Inline field error text, e.g. "Please select relation".
     *
     * <p>Only the RELATION field publishes one. Name and mobile show a red icon
     * that is drawn outside the accessibility tree entirely — verified by dumping
     * both field subtrees, which contain nothing but their EditText — so their
     * error state cannot be asserted here. Assert on behaviour instead: an
     * invalid form must not add a receiver.
     */
    public List<String> fieldErrors() {
        List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("textinput_error"))) {
                String t = safeText(e);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    public boolean typeOutsiderName(String text) {
        return typeInto(nestedInput("etName"), text);
    }

    public boolean typeOutsiderPhone(String text) {
        return typeInto(nestedInput("etPhone"), text);
    }

    public boolean clearOutsiderName() {
        return clearField(nestedInput("etName"));
    }

    public boolean clearOutsiderPhone() {
        return clearField(nestedInput("etPhone"));
    }

    public boolean openOutsiderCountryPicker() {
        return tapById("selectCode");
    }

    /**
     * Opens the relation dropdown via its ARROW.
     *
     * <p>Tapping the field itself only takes focus. The arrow
     * ({@code text_input_end_icon}) is what expands the list — and tapping it a
     * second time closes it again, so it must not be re-tapped.
     */
    public boolean openRelationDropdown() {
        hideKeyboard();
        sleep(400);
        if (!tapById("text_input_end_icon")) {
            return false;
        }
        sleep(1500);
        return true;
    }

    /**
     * Taps a relation by name.
     *
     * <p>The opened list is absent from the accessibility tree, so the row is hit
     * by coordinate, measured off the closed field's own bounds. The two factors
     * below were derived from the rendered list and match the unit dropdown
     * elsewhere in the app: the first row sits about 0.61 of a field-height below
     * the field, and rows repeat every 0.885 of a field-height.
     */
    public boolean selectRelation(String name) {
        int index = RELATIONS.indexOf(name);
        if (index < 0) {
            return false;
        }
        try {
            Rectangle r = driver.findElement(byId("autoCompleteDropDown")).getRect();
            int h = r.getHeight();
            int firstRowCentre = r.getY() + h + (int) (h * 0.61);
            int rowStep = (int) (h * 0.885);
            int x = r.getX() + Math.max(40, r.getWidth() / 6);
            tapAt(x, firstRowCentre + index * rowStep);
            sleep(2000);
            return name.equalsIgnoreCase(relation());
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Waits for a TOAST, e.g. "Please Select Country Code First."
     *
     * <p>Read from the page source rather than by locator: a Toast lives in its
     * own window and {@code UiSelector} only searches the active one.
     */
    public boolean toastShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                String src = driver.getPageSource();
                if (src != null && src.contains(message)) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
        }
    }

    // ---------------- low-level helpers ----------------

    private WebElement nestedInput(String containerId) {
        try {
            return driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'" + containerId
                            + "')]//*[contains(@resource-id,'editTextInput')]"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    private String nestedInputText(String containerId) {
        WebElement e = nestedInput(containerId);
        return e == null ? "" : String.valueOf(safeText(e)).trim();
    }

    private boolean typeInto(org.openqa.selenium.By locator, String text) {
        try {
            WebElement field = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(locator));
            return typeInto(field, text);
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean typeInto(WebElement field, String text) {
        if (field == null) {
            return false;
        }
        try {
            field.click();
            sleep(500);
            field.sendKeys(text);
            hideKeyboard();
            sleep(800);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean clearField(org.openqa.selenium.By locator) {
        try {
            return clearField(driver.findElement(locator));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Empties a text field with the DELETE key rather than {@code clear()}.
     *
     * <p>{@code WebElement.clear()} hangs on these fields — it was left blocking
     * until the command timed out — so the cursor is sent to the end and the
     * content deleted one character at a time.
     */
    private boolean clearField(WebElement field) {
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

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(800);
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

    // ---------------- shared ----------------

    public boolean tapBack() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("backBtn")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public String headerText() {
        return textOf("headerText");
    }

    private String nearest(WebElement anchor, List<WebElement> candidates) {
        try {
            Rectangle ar = anchor.getRect();
            String best = "";
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement c : candidates) {
                Rectangle cr = c.getRect();
                int delta = Math.abs(cr.getY() - ar.getY());
                if (delta < bestDelta && delta < ar.getHeight() * 3) {
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

    /** Polls for an id to exist (presence, not visibility). */
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

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
