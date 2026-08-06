package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Pre-Approved Users"
 * (header reads "CREATE PRE-APPROVED USER").
 *
 * <p>All ids confirmed against the live screen: name fields live inside
 * {@code enter_name} / {@code enterLastName} as {@code editTextInput};
 * department and designation are {@code select_department} /
 * {@code select_designation} with their labels in {@code departmentText} /
 * {@code designationText}; unit and country are Material EXPOSED DROPDOWNS
 * ({@code autoCompleteDropDown}); email and phone are {@code etEmail} /
 * {@code etPhone}; Submit is {@code submit_btn} → {@code buttonBtn}.
 *
 * <p><b>Validation messages are Material snackbars</b> ({@code snackbar_text}),
 * not Toasts — so unlike Manage Tool Groups a direct id lookup works and is fast.
 *
 * <p><b>The unit dropdown needs two tricks</b>, both established by testing on the
 * device rather than assumed:
 * <ol>
 *   <li>It only expands when tapped near the BOTTOM of the field. A tap at the
 *       vertical centre lands on the label and merely takes focus — the field
 *       shows its focus ring and nothing opens.</li>
 *   <li>The opened list is INVISIBLE to the accessibility tree. UiAutomator
 *       reports the options absent at the very moment a screenshot shows them
 *       rendered, so rows have to be tapped by coordinate, measured from the
 *       field's own bounds.</li>
 * </ol>
 * Selecting by a real tap matters: {@code JoinUserDetailPage} binds the unit ID
 * only in {@code setOnItemClickListener}. Typing the text sets the label but
 * leaves the id null, and the server then rejects the request.
 */
public class PreApprovedUsersPage extends BasePage {

    public PreApprovedUsersPage(AndroidDriver driver) {
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
                    "new UiSelector().textMatches(\"(?i)Settings\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openPreApprovedUsers() {
        return tapListRow("Pre-Approved Users");
    }

    private boolean tapListRow(String label) {
        // Admin Settings is a long list — "Pre-Approved Users" sits below the fold,
        // so bring the row on screen before looking for it.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list, or already visible.
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

    /**
     * True once the form is up. Matched on the FORM (name field + Submit), never on
     * the header text alone — the Admin Settings row that opens this screen reads
     * "Pre-Approved Users", which a loose text match would happily accept.
     */
    public boolean isLoaded() {
        return isLoaded(Duration.ofSeconds(20));
    }

    /** Same check with an explicit budget — the state machine needs a cheap probe. */
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (present("submit_btn") && present("enter_name")) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    /**
     * Gets to ADMIN SETTINGS from wherever the app currently is, and confirms it
     * arrived.
     *
     * <p>A loop rather than a one-shot if-chain. Each pass re-reads the CURRENT
     * screen and takes one step toward Admin Settings, so a tap that does not land
     * is simply retried on the next pass instead of aborting the whole navigation:
     *
     * <ul>
     *   <li>already on Admin Settings — done;</li>
     *   <li>on the Settings TAB — tap the "Admin Settings" row;</li>
     *   <li>anywhere with a bottom nav — switch to the Settings tab first;</li>
     *   <li>otherwise — press back to unwind one level.</li>
     * </ul>
     */
    public boolean ensureOnAdminSettings(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOnAdminSettings(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnSettingsTab(Duration.ofMillis(800))) {
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

    /**
     * Gets all the way to the Pre-Approved Users form: Admin Settings first, then
     * the row. The row tap is retried, because one click landing on a mid-scroll
     * list is not proof of failure.
     */
    public boolean ensureOnPreApprovedUsers(Duration timeout) {
        if (isLoaded(Duration.ofSeconds(2))) {
            return true;
        }
        if (!ensureOnAdminSettings(timeout)) {
            return false;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            if (openPreApprovedUsers() && isLoaded(Duration.ofSeconds(8))) {
                return true;
            }
            // Land back on Admin Settings before trying again.
            ensureOnAdminSettings(Duration.ofSeconds(10));
            sleep(800);
        }
        return false;
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Waits for the loader to clear — the unit + department APIs run on open. */
    public boolean waitForLoaderGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!present("progressMain")) {
                sleep(500);              // let the adapters settle after the response
                return true;
            }
            sleep(300);
        }
        return false;
    }

    private boolean present(String id) {
        try {
            return !driver.findElements(byId(id)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- text fields ----------------

    private WebElement fieldIn(String containerId) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath(
                        "//*[contains(@resource-id,'" + containerId
                                + "')]//*[contains(@resource-id,'editTextInput')]")));
    }

    public void setFirstName(String value) {
        setField(fieldIn("enter_name"), value);
    }

    public void setLastName(String value) {
        setField(fieldIn("enterLastName"), value);
    }

    public void setEmail(String value) {
        setField(fieldIn("etEmail"), value);
    }

    public void setPhone(String value) {
        setField(fieldIn("etPhone"), value);
    }

    /**
     * Clears then types. {@code clear()} alone is not dependable on these fields,
     * and leftover text turns an "empty" check into a completely different result,
     * so the outcome is verified and topped up with deletes.
     */
    private void setField(WebElement field, String value) {
        field.click();
        try {
            field.clear();
        } catch (WebDriverException ignored) {
        }
        for (int attempt = 0; attempt < 3 && !readText(field).isEmpty(); attempt++) {
            int remaining = readText(field).length();
            for (int i = 0; i < remaining + 2; i++) {
                try {
                    driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                            io.appium.java_client.android.nativekey.AndroidKey.DEL));
                } catch (WebDriverException stop) {
                    break;
                }
            }
        }
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
        hideKeyboard();
    }

    private String readText(WebElement e) {
        try {
            String t = e.getText();
            return t == null ? "" : t;
        } catch (WebDriverException ex) {
            return "";
        }
    }

    public boolean tapSubmit() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'submit_btn')]//*[contains(@resource-id,'buttonBtn')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(byId("buttonBtn")).click();
                return true;
            } catch (WebDriverException e2) {
                return false;
            }
        }
    }

    // ---------------- department / designation pickers ----------------

    public boolean openDepartmentPicker() {
        return tapById("select_department");
    }

    public boolean openDesignationPicker() {
        return tapById("select_designation");
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id))).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The picker opens its own screen with a search box and a plain list. */
    public boolean isPickerOpen(Duration timeout) {
        return isVisible("search_widget_edit_textvw", timeout);
    }

    public void searchInPicker(String text) {
        try {
            WebElement box = driver.findElement(byId("search_widget_edit_textvw"));
            box.click();
            box.clear();
            box.sendKeys(text);
            sleep(1500);            // the list filters as you type
        } catch (WebDriverException ignored) {
        }
    }

    /** Picks a row by EXACT label — "2nd assistant director" also prefix-matches "Crowd 2nd assistant director". */
    public boolean selectFromPicker(String exactLabel) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceIdMatches(\".*:id/valueTextView\").text(\""
                                    + exactLabel + "\")")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public String departmentLabel() {
        return textOf("departmentText");
    }

    public String designationLabel() {
        return textOf("designationText");
    }

    private String textOf(String id) {
        try {
            String t = driver.findElement(byId(id)).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    // ---------------- exposed dropdowns (unit, country) ----------------

    private Rectangle dropdownRect(String hint) {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/autoCompleteDropDown\").text(\""
                            + hint + "\")")).getRect();
        } catch (WebDriverException e) {
            return null;
        }
    }

    /**
     * Opens an exposed dropdown by tapping near the BOTTOM of the field.
     *
     * <p>A tap at the vertical centre only focuses it — verified by screenshot,
     * where the field showed its focus ring with no list. Tapping low, just inside
     * the bottom edge, expands it reliably.
     */
    public boolean openDropdown(String hint) {
        Rectangle r = dropdownRect(hint);
        if (r == null) {
            return false;
        }
        int x = r.getX() + r.getWidth() / 2;
        int y = r.getY() + r.getHeight() - Math.max(12, r.getHeight() / 8);
        tapAt(x, y);
        sleep(1200);
        return true;
    }

    /**
     * Taps the Nth row of an open dropdown, counting from 0.
     *
     * <p>By coordinate, because the popup does not appear in the accessibility tree
     * at all — UiAutomator reports the options missing while a screenshot shows
     * them on screen. Row geometry is derived from the field's own height rather
     * than hardcoded pixels, so it survives density and layout changes.
     */
    public boolean selectDropdownRow(String hint, int rowIndex) {
        Rectangle r = dropdownRect(hint);
        if (r == null) {
            return false;
        }
        int h = r.getHeight();
        int firstRowCentre = r.getY() + h + (int) (h * 0.61);   // measured on device
        int rowStep = (int) (h * 0.885);
        int x = r.getX() + Math.max(40, r.getWidth() / 6);      // left-aligned label
        tapAt(x, firstRowCentre + rowIndex * rowStep);
        sleep(1200);
        return true;
    }

    /** What the dropdown currently displays (the hint until something is chosen). */
    public String dropdownText(String hintOrValue) {
        try {
            String t = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/autoCompleteDropDown\").text(\""
                            + hintOrValue + "\")")).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Country uses a full search screen, opened from its end icon. */
    public boolean openCountryPicker() {
        try {
            driver.findElements(byId("text_input_end_icon")).get(1).click();
            sleep(1500);
            return true;
        } catch (Exception e) {
            return openDropdown("Country");
        }
    }

    // ---------------- feedback / results ----------------

    /**
     * True if a snackbar containing {@code message} appears in time. These are
     * Material snackbars with a real id, so this is a cheap direct lookup — no
     * page-source scraping needed (unlike the Toast-based screens).
     */
    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                for (WebElement e : driver.findElements(byId("snackbar_text"))) {
                    String t = e.getText();
                    if (t != null && t.contains(message)) {
                        return true;
                    }
                }
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /** Most recent snackbar text, for diagnostics when an assertion fails. */
    public String lastSnackbarText() {
        try {
            for (WebElement e : driver.findElements(byId("snackbar_text"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) {
                    return t;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return "";
    }

    /**
     * True once the system share sheet is up.
     *
     * <p>This is the honest signal that the user was created. {@code shareText()}
     * runs only in the success branch of the create call, and the sheet belongs to
     * a DIFFERENT app ({@code com.android.intentresolver}) — which is exactly why
     * the success snackbar cannot be asserted instead: the chooser takes the
     * window before the app's own views can be read.
     */
    public boolean isSharingSheetOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && pkg.contains("intentresolver")) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (textShown("Sharing text", Duration.ofMillis(400))) {
                return true;
            }
        }
        return false;
    }

    /** Closes the share sheet with a downward swipe, as a user would. */
    public boolean dismissSharingSheet(Duration timeout) {
        var size = driver.manage().window().getSize();
        for (int i = 0; i < 3; i++) {
            swipeDown(size.getWidth() / 2,
                    (int) (size.getHeight() * 0.42), (int) (size.getHeight() * 0.96));
            sleep(1500);
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && !pkg.contains("intentresolver")) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
        }
        try {
            driver.navigate().back();          // last resort
            sleep(1000);
        } catch (WebDriverException ignored) {
        }
        return isLoaded();
    }

    /** True when the form has been reset — no values left from the previous entry. */
    public boolean isFormCleared() {
        boolean namesEmpty = readText(fieldIn("enter_name")).isEmpty()
                && readText(fieldIn("enterLastName")).isEmpty()
                && readText(fieldIn("etEmail")).isEmpty()
                && readText(fieldIn("etPhone")).isEmpty();
        boolean pickersDefault = departmentLabel().equalsIgnoreCase("Select Department")
                && designationLabel().equalsIgnoreCase("Select Designation");
        return namesEmpty && pickersDefault;
    }

    /**
     * True if the named user appears in the pre-approved list with the given
     * designation. Name and description are separate views ({@code userNameTv} /
     * {@code userDescTV}), paired by row position.
     */
    public boolean isUserListed(String name, String designation) {
        try {
            for (WebElement nameView : driver.findElements(byId("userNameTv"))) {
                String n = nameView.getText();
                if (n == null || !n.equalsIgnoreCase(name)) {
                    continue;
                }
                Rectangle nb = nameView.getRect();
                int centreY = nb.getY() + nb.getHeight() / 2;
                for (WebElement descView : driver.findElements(byId("userDescTV"))) {
                    Rectangle db = descView.getRect();
                    // Same row: the description sits just under the name.
                    if (Math.abs(db.getY() + db.getHeight() / 2 - centreY) <= nb.getHeight() * 2) {
                        String d = descView.getText();
                        if (d != null && d.equalsIgnoreCase(designation)) {
                            return true;
                        }
                    }
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    /** The pre-approved list screen is up (header "PRE-APPROVED USERS"). */
    public boolean isNotJoinedListOpen(Duration timeout) {
        return isVisible("preApprovedUsersRecyclerView", timeout);
    }

    private void swipeDown(int x, int fromY, int toY) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, fromY));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(600),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, toY));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    public boolean tapListOfNotJoinedUsers() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'preApprovedListBtn')]//*[contains(@resource-id,'buttonBtn')]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    /**
     * True when the SETTINGS TAB is showing — the bottom-nav screen whose list
     * contains the "Admin Settings" row.
     *
     * <p>Mutually exclusive with {@link #isOnAdminSettings}: this screen has the
     * bottom nav, that one does not. Without telling the two apart, a caller sitting
     * on the Settings tab looks like it is "somewhere unknown" and gets sent through
     * a full unwind instead of simply opening Admin Settings.
     */
    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    // ---------------- helpers ----------------

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(80),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
