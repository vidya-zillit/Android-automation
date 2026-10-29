package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
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
 * Settings → Admin Settings → "Approve New User Requests".
 *
 * <p>Three screens, all inside {@code ApproveUserRequestActivity} and all
 * confirmed by walking them on the device:
 *
 * <ol>
 *   <li><b>SELECT USER</b> — the pending queue. List is
 *       {@code approveNewUserRecyclerView}; each row is a {@code userHolder}
 *       holding {@code userNameTv} (name) and {@code userDescTV} (designation).
 *       The search box is the shared {@code search_widget_edit_textvw} and
 *       filters on NAME <em>and</em> DESIGNATION — typing "Director" narrows the
 *       list to the user whose designation is Director, not just name matches.</li>
 *   <li><b>APPROVE NEW USER REQUESTS</b> — the request detail. Department and
 *       designation use the same picker pattern as Pre-Approved Users
 *       ({@code select_department}/{@code departmentText},
 *       {@code select_designation}/{@code designationText}). Buttons are
 *       {@code acceptBtn} ("Accept onboarding without Deal memo/NDA"),
 *       {@code declineBtn} and {@code sendFormBtn} ("Onboard with Deal
 *       Memo/NDA").</li>
 *   <li><b>Image viewer</b> — tapping {@code user_image} opens a full-screen
 *       {@code viewpager} overlay with the photo in {@code imageVw}; its own
 *       {@code backBtn} closes it.</li>
 * </ol>
 *
 * <p><b>Picking a department clears the designation.</b> The field falls back to
 * the literal "Select Designation", so a caller that changes department must
 * choose a designation again — the previous one is not retained.
 *
 * <p><b>Both pickers title themselves "SELECT DEPARTMENT".</b> The designation
 * picker reuses the department header, so {@code headerText} cannot tell them
 * apart; they are detected by {@code commonRecyclerView} + {@code valueTextView}
 * instead.
 *
 * <p><b>Resolving the LAST request closes the queue.</b> Once nothing is pending
 * the app pops straight back to ADMIN SETTINGS on its own, so a caller must not
 * assume it is still on SELECT USER after an accept or decline — observed on the
 * device, where the screen showed Admin Settings while the activity was still
 * reported as {@code ApproveNewUsersListActivity}.
 *
 * <p><b>This suite consumes real data.</b> Accept and Decline permanently resolve
 * a join request, so a user approved once is gone from the queue on the next run.
 * Callers should treat an empty queue as "nothing to do", not as a failure.
 */
public class ApproveNewUserRequestPage extends BasePage {

    /** Placeholder the designation field falls back to once department changes. */
    public static final String NO_DESIGNATION = "Select Designation";

    public ApproveNewUserRequestPage(AndroidDriver driver) {
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
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/menuText\").textMatches(\"(?i)"
                            + java.util.regex.Pattern.quote(label) + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openApproveNewUserRequests() {
        return tapListRow("Approve New User Requests");
    }

    private boolean tapListRow(String label) {
        // Admin Settings is a long list that keeps its scroll position, so bring
        // the row on screen before looking for it.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list, or already visible — the lookup below decides.
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
     * Gets to the SELECT USER queue from wherever the app is, looping so a tap
     * that does not land is retried rather than aborting the whole navigation.
     */
    public boolean ensureOnSelectUserList(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOnSelectUserList(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnUserDetail(Duration.ofMillis(500))) {
                tapBack();                       // detail → queue
                sleep(1500);
                continue;
            }
            if (isOnAdminSettings(Duration.ofMillis(500))) {
                openApproveNewUserRequests();
                sleep(2000);
                continue;
            }
            if (isOnSettingsTab(Duration.ofMillis(500))) {
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

    // ---------------- SELECT USER queue ----------------

    /**
     * True when the pending queue is up.
     *
     * <p>Accepts EITHER {@code approveNewUserRecyclerView} or a
     * {@code headerText} of exactly "SELECT USER". The list view alone is not
     * enough: it is tied to the rows, so a queue that has just been emptied can
     * drop it while the screen is still showing. Exact-matching the header keeps
     * this distinct from the department and designation pickers, which reuse the
     * same id with "SELECT DEPARTMENT".
     */
    public boolean isOnSelectUserList(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("approveNewUserRecyclerView")).isEmpty()
                        || "SELECT USER".equalsIgnoreCase(textOf("headerText"))) {
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

    /**
     * Gets back to the Settings tab from anywhere, unwinding first.
     *
     * <p>{@link #openSettingsTab()} can only work where a bottom nav exists, and
     * Admin Settings and the request screens have none — so a bare call there
     * fails on a screen the app is perfectly happy on. This presses back until
     * the nav reappears, then switches tab.
     */
    public boolean ensureOnSettingsTab(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOnSettingsTab(Duration.ofMillis(600))) {
                return true;
            }
            if (hasBottomNav()) {
                openSettingsTab();
                sleep(1200);
                continue;
            }
            if (!tapBack()) {
                try {
                    driver.navigate().back();
                } catch (WebDriverException ignored) {
                }
            }
            sleep(1200);
        }
        return false;
    }

    public boolean isOnSelectUserList() {
        return isOnSelectUserList(Duration.ofSeconds(20));
    }

    /** Name → designation for every row currently rendered. */
    public Map<String, String> listedUsers() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            List<WebElement> names = driver.findElements(byId("userNameTv"));
            List<WebElement> descs = driver.findElements(byId("userDescTV"));
            for (WebElement name : names) {
                String n = safeText(name);
                if (n == null || n.isBlank()) {
                    continue;
                }
                out.put(n.trim(), nearestBelow(name, descs));
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /**
     * Pairs a name with its designation by ROW GEOMETRY rather than list index.
     * The two id lists are collected separately, so a row whose designation is
     * blank would otherwise shift every later pairing by one.
     */
    private String nearestBelow(WebElement name, List<WebElement> descs) {
        try {
            Rectangle nr = name.getRect();
            String best = "";
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement d : descs) {
                Rectangle dr = d.getRect();
                int delta = Math.abs(dr.getY() - nr.getY());
                if (delta < bestDelta && delta < nr.getHeight() * 3) {
                    bestDelta = delta;
                    best = safeText(d);
                }
            }
            return best == null ? "" : best.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /**
     * Every pending request in the queue, not just the rows on screen.
     *
     * <p>{@code approveNewUserRecyclerView} recycles its rows: once a row scrolls
     * off, it leaves the accessibility tree entirely. A viewport-only read
     * therefore reports a long queue as four or five users and looks like a clean
     * pass, so the list is walked from the top and accumulated.
     *
     * <p>Merging prefers a non-blank designation: a row caught mid-bind can
     * report its name before its description text arrives, and a later pass over
     * the same row must not overwrite a good value with an empty one.
     */
    public Map<String, String> allListedUsersScrolled() {
        scrollListToTop();
        Map<String, String> all = new LinkedHashMap<>();
        merge(all, listedUsers());
        int quiet = 0;
        for (int i = 0; i < 25 && quiet < 2; i++) {
            int before = all.size();
            swipeListUp();
            merge(all, listedUsers());
            quiet = (all.size() == before) ? quiet + 1 : 0;
        }
        scrollListToTop();
        return all;
    }

    private void merge(Map<String, String> into, Map<String, String> found) {
        found.forEach((name, desig) -> {
            String existing = into.get(name);
            if (existing == null || existing.isBlank()) {
                into.put(name, desig);
            }
        });
    }

    private void scrollListToTop() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(15)"));
        } catch (WebDriverException notScrollable) {
            // Queue fits on one screen.
        }
    }

    private void swipeListUp() {
        try {
            var size = driver.manage().window().getSize();
            var finger = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
            int x = size.getWidth() / 2;
            // Start well below the toolbar: a drag begun near the top grabs the
            // header instead of the list and scrolls nothing.
            int yFrom = (int) (size.getHeight() * 0.65);
            int yTo = (int) (size.getHeight() * 0.35);
            seq.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, yFrom));
            seq.addAction(finger.createPointerDown(0));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(800),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, yTo));
            seq.addAction(finger.createPointerUp(0));
            driver.perform(java.util.Collections.singletonList(seq));
            sleep(500);
        } catch (WebDriverException ignored) {
        }
    }

    /** True if the named user is in the queue, scrolling the list to look. */
    public boolean isUserListed(String name) {
        if (listedUsers().keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name))) {
            return true;
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + name + "\"))"));
        } catch (WebDriverException notFound) {
            return false;
        }
        return listedUsers().keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    public boolean search(String text) {
        try {
            WebElement box = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("search_widget_edit_textvw")));
            box.click();
            box.sendKeys(text);
            hideKeyboard();
            sleep(1500);              // the list re-filters asynchronously
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
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openUser(String name) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'userNameTv') and contains(@text,\"" + name
                                    + "\")]/ancestor::*[contains(@resource-id,'userHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- request detail ----------------

    /** Keyed on the Accept button — present only on the detail screen. */
    public boolean isOnUserDetail(Duration timeout) {
        return presentWithin("acceptBtn", timeout);
    }

    public String department() {
        return textOf("departmentText");
    }

    public String designation() {
        return textOf("designationText");
    }

    public String unit() {
        return textOf("autoCompleteDropDown");
    }

    /** Opens the department picker and returns the options it offers. */
    public List<String> openDepartmentPicker() {
        return openPicker("select_department");
    }

    public List<String> openDesignationPicker() {
        return openPicker("select_designation");
    }

    private List<String> openPicker(String fieldId) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(fieldId)))
                    .click();
        } catch (WebDriverException e) {
            return List.of();
        }
        sleep(1500);
        return pickerOptions();
    }

    /** Options currently rendered in an open picker. */
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

    /** Taps a row in whichever picker is open, scrolling to reach it. */
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
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapAccept() {
        return tapButton("acceptBtn");
    }

    public boolean tapDecline() {
        return tapButton("declineBtn");
    }

    private boolean tapButton(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- image viewer ----------------

    public boolean openUserImage() {
        return tapButton("user_image");
    }

    /** True once the full-screen photo overlay is up ({@code imageVw}). */
    public boolean isImageViewerOpen(Duration timeout) {
        return presentWithin("imageVw", timeout);
    }

    /**
     * True when the photo actually rendered, not merely when the ImageView
     * exists.
     *
     * <p>An unloaded or failed image still leaves a displayed ImageView of full
     * size, so presence proves nothing. The element's own screenshot is used
     * instead: a real photo is a large, high-entropy PNG (~1 MB on the device
     * this was measured against), whereas a blank or flat placeholder compresses
     * to a few KB. The threshold sits far below the former and well above the
     * latter.
     */
    public boolean isImageLoaded() {
        try {
            WebElement img = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.visibilityOfElementLocated(byId("imageVw")));
            Rectangle r = img.getRect();
            if (r.getWidth() <= 0 || r.getHeight() <= 0) {
                return false;
            }
            byte[] png = img.getScreenshotAs(OutputType.BYTES);
            return png != null && png.length > 30_000;
        } catch (WebDriverException | ClassCastException e) {
            return false;
        }
    }

    /** The viewer's own back arrow — closes the overlay, not the screen. */
    public boolean closeImageViewer() {
        return tapBack();
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

    /** Contact names on the C&amp;C → Contacts tab ({@code recMembers}). */
    public Map<String, String> contacts() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            List<WebElement> names = driver.findElements(byId("userNameTv"));
            List<WebElement> desigs = driver.findElements(byId("designationTv"));
            for (WebElement name : names) {
                String n = safeText(name);
                if (n != null && !n.isBlank()) {
                    out.put(n.trim(), nearestBelow(name, desigs));
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /** True if the contact is on the Contacts tab, scrolling to look. */
    public boolean isContactListed(String name) {
        if (contacts().keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name))) {
            return true;
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + name + "\"))"));
        } catch (WebDriverException notFound) {
            return false;
        }
        return contacts().keySet().stream().anyMatch(n -> n.equalsIgnoreCase(name));
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

    /** Waits for a message anywhere on screen (snackbar or toast). */
    public boolean messageShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (textShown(message, Duration.ofMillis(500))) {
                return true;
            }
            // Toasts live in their own window, which UiSelector cannot see —
            // the page source can.
            try {
                String src = driver.getPageSource();
                if (src != null && src.contains(message)) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
        }
        return false;
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
