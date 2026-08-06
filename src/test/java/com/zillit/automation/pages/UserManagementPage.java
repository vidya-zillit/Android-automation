package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings → Admin Settings → "User Management", plus the CHANGE PROFILE screen
 * it opens and the C&amp;C → Contacts list used to confirm the effects.
 *
 * <p>Every id below was read off the running app:
 *
 * <ul>
 *   <li><b>USER MANAGEMENT</b> — search is the shared
 *       {@code search_widget_edit_textvw}; each card holds {@code nameTv},
 *       {@code tvUserDesignation}, the edit icon {@code changeProfile}, and two
 *       real {@code Switch} widgets: {@code icCheck} (Status) and
 *       {@code chkAdmin} (Admin Access). Both expose a {@code checked}
 *       attribute, so their state is readable rather than inferred.</li>
 *   <li><b>CHANGE PROFILE</b> — {@code select_department}/{@code departmentText}
 *       and {@code select_designation}/{@code designationText}, saved with
 *       {@code acceptBtn}. Both pickers are searchable.</li>
 *   <li><b>Confirm dialogs</b> — {@code message} with {@code leftButton} (No) and
 *       {@code rightButton} (Yes).</li>
 * </ul>
 *
 * <p><b>The list reuses {@code adminSettingsRecyclerView}</b>, the same id as the
 * Admin Settings menu, and neither screen has a bottom nav — so the two are only
 * distinguishable by {@code headerText}. {@link BasePage#onAdminSettingsScreen}
 * checks the header for exactly this reason.
 *
 * <p><b>Saving a department change is a two-step confirm.</b> Submit raises an
 * alert warning that communication in the affected tools will be erased; the
 * change only lands after "Yes". "No" returns to the form with the edits intact.
 *
 * <p><b>Enabling Admin Access also confirms</b>, but toggling Status does not —
 * it flips immediately.
 */
public class UserManagementPage extends BasePage {

    /** Stable fragments of the department-change warning. The tool names inside it vary. */
    public static final String DEPT_CHANGE_WARNING_START = "If you change department";
    public static final String DEPT_CHANGE_WARNING_END = "Do you still want to change Department?";
    /** Stable fragment of the admin-access warning. */
    public static final String ADMIN_ACCESS_WARNING = "You have chosen to make another user an Admin";
    /** Suffix C&amp;C → Contacts appends to an admin's name. */
    public static final String ADMIN_TAG = "(Admin)";
    /** Designation field value once a department change clears it. */
    public static final String NO_DESIGNATION = "Select Designation";

    public UserManagementPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        return tapBottomNav("Settings");
    }

    public boolean openCncTab() {
        return tapBottomNav("C&C");
    }

    private boolean tapBottomNav(String label) {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase(label)) {
                    it.click();
                    sleep(1500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openUserManagement() {
        return tapListRow("User Management");
    }

    private boolean tapListRow(String label) {
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

    /** Gets to USER MANAGEMENT from wherever the app is, retrying each hop. */
    public boolean ensureOnUserManagement(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isLoaded(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnChangeProfile(Duration.ofMillis(400))) {
                tapBack();
                sleep(1500);
                continue;
            }
            if (isOnAdminSettings(Duration.ofMillis(400))) {
                openUserManagement();
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

    // ---------------- USER MANAGEMENT ----------------

    /** Keyed on the header — the list id is shared with the Admin Settings menu. */
    public boolean isLoaded(Duration timeout) {
        return headerIs("USER MANAGEMENT", timeout);
    }

    public boolean isLoaded() {
        return isLoaded(Duration.ofSeconds(20));
    }

    /** Users currently rendered, name → designation. */
    public Map<String, String> users() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            List<WebElement> names = driver.findElements(byId("nameTv"));
            List<WebElement> desigs = driver.findElements(byId("tvUserDesignation"));
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

    /** Every user, scrolling — the list recycles rows out of the tree. */
    public Map<String, String> allUsersScrolled() {
        scrollToTop();
        Map<String, String> all = new LinkedHashMap<>();
        merge(all, users());
        int quiet = 0;
        for (int i = 0; i < 25 && quiet < 2; i++) {
            int before = all.size();
            scrollForward();
            merge(all, users());
            quiet = (all.size() == before) ? quiet + 1 : 0;
        }
        scrollToTop();
        return all;
    }

    private void merge(Map<String, String> into, Map<String, String> found) {
        found.forEach((k, v) -> {
            String existing = into.get(k);
            if (existing == null || existing.isBlank()) {
                into.put(k, v);
            }
        });
    }

    public boolean isUserListed(String name) {
        String needle = name.toLowerCase();
        return users().keySet().stream().anyMatch(n -> n.toLowerCase().contains(needle));
    }

    public boolean search(String text) {
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

    public boolean clearSearch() {
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

    public boolean tapEditIcon(String name) {
        WebElement icon = rowControl(name, "changeProfile");
        return click(icon);
    }

    /** Status switch state, or null when the user is not on screen. */
    public Boolean statusEnabled(String name) {
        return switchState(name, "icCheck");
    }

    /** Admin Access switch state, or null when the user is not on screen. */
    public Boolean adminAccessEnabled(String name) {
        return switchState(name, "chkAdmin");
    }

    private Boolean switchState(String name, String id) {
        WebElement sw = rowControl(name, id);
        if (sw == null) {
            return null;
        }
        try {
            return "true".equals(sw.getAttribute("checked"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean tapStatusToggle(String name) {
        return click(rowControl(name, "icCheck"));
    }

    public boolean tapAdminAccessToggle(String name) {
        return click(rowControl(name, "chkAdmin"));
    }

    /** Waits for a switch to settle on the wanted state (the save is a round trip). */
    public boolean waitForStatus(String name, boolean wanted, Duration timeout) {
        return waitForSwitch(name, "icCheck", wanted, timeout);
    }

    public boolean waitForAdminAccess(String name, boolean wanted, Duration timeout) {
        return waitForSwitch(name, "chkAdmin", wanted, timeout);
    }

    private boolean waitForSwitch(String name, String id, boolean wanted, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Boolean state = switchState(name, id);
            if (state != null && state == wanted) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    /**
     * A control belonging to the named user's card, matched by vertical position.
     *
     * <p>The switches and the edit icon carry no text, so they can only be tied to
     * a user by geometry. The window is generous downward because Status and Admin
     * Access sit BELOW the name inside the same card, not beside it.
     */
    private WebElement rowControl(String name, String controlId) {
        try {
            WebElement label = null;
            String needle = name.toLowerCase();
            for (WebElement n : driver.findElements(byId("nameTv"))) {
                String t = safeText(n);
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
            for (WebElement c : driver.findElements(byId(controlId))) {
                Rectangle cr = c.getRect();
                int delta = cr.getY() - lr.getY();
                if (delta < -lr.getHeight()) {
                    continue;                    // belongs to a card above
                }
                int score = Math.abs(delta);
                if (score < bestDelta && delta < 600) {
                    bestDelta = score;
                    best = c;
                }
            }
            return best;
        } catch (WebDriverException e) {
            return null;
        }
    }

    // ---------------- confirm dialog ----------------

    public boolean isDialogShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("message")).isEmpty()
                        && !driver.findElements(byId("rightButton")).isEmpty()) {
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

    public String dialogMessage() {
        return textOf("message");
    }

    /** "No" — dismisses without applying. */
    public boolean tapDialogNo() {
        return tapById("leftButton");
    }

    /** "Yes" — applies the change. */
    public boolean tapDialogYes() {
        return tapById("rightButton");
    }

    // ---------------- CHANGE PROFILE ----------------

    public boolean isOnChangeProfile(Duration timeout) {
        return headerIs("CHANGE PROFILE", timeout);
    }

    public String department() {
        return textOf("departmentText");
    }

    public String designation() {
        return textOf("designationText");
    }

    public boolean openDepartmentPicker() {
        return tapById("select_department");
    }

    public boolean openDesignationPicker() {
        return tapById("select_designation");
    }

    /** Options in the open picker. */
    public List<String> pickerOptions() {
        List<String> out = new ArrayList<>();
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

    /** Both pickers carry the shared search box. */
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

    /** Picks an option by EXACT text — a substring match takes the wrong row. */
    public boolean choosePickerValue(String value) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + value + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
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

    public boolean tapChangeProfileSubmit() {
        return tapById("acceptBtn");
    }

    // ---------------- C&C → Contacts ----------------

    public boolean openContactsTab() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)Contacts\")")))
                    .click();
            sleep(2500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Contacts, name → designation.
     *
     * <p>An admin's name arrives with "(Admin)" already appended — the tag is part
     * of {@code userNameTv}, not a separate view — so callers must match loosely.
     */
    public Map<String, String> contacts() {
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

    public boolean isContactListed(String name) {
        String needle = name.toLowerCase();
        if (contacts().keySet().stream().anyMatch(n -> n.toLowerCase().contains(needle))) {
            return true;
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + name + "\"))"));
        } catch (WebDriverException notFound) {
            return false;
        }
        return contacts().keySet().stream().anyMatch(n -> n.toLowerCase().contains(needle));
    }

    /** Designation shown for a contact, matched on the name fragment. */
    public String contactDesignation(String name) {
        String needle = name.toLowerCase();
        return contacts().entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(needle))
                .map(Map.Entry::getValue)
                .findFirst().orElse("");
    }

    /** The full contact label, e.g. "Bhavik Mehra (Admin)". */
    public String contactLabel(String name) {
        String needle = name.toLowerCase();
        return contacts().keySet().stream()
                .filter(k -> k.toLowerCase().contains(needle))
                .findFirst().orElse("");
    }

    /** Waits for a contact to appear or disappear — the change propagates server-side. */
    public boolean waitForContact(String name, boolean shouldBeListed, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (isContactListed(name) == shouldBeListed) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(1000);
        }
    }

    // ---------------- shared ----------------

    public boolean tapBack() {
        return tapById("backBtn");
    }

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

    private boolean click(WebElement e) {
        if (e == null) {
            return false;
        }
        try {
            e.click();
            sleep(900);
            return true;
        } catch (WebDriverException ex) {
            return false;
        }
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

    private void scrollToTop() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(15)"));
        } catch (WebDriverException ignored) {
        }
    }

    private void scrollForward() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            sleep(500);
        } catch (WebDriverException ignored) {
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
