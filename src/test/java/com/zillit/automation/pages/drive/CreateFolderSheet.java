package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * CREATE FOLDER bottom sheet (CreateFolderBottomSheet / bottom_sheet_create_folder.xml).
 *
 * <p>Folder name (required), description with a 0/500 counter, a Location segment
 * (Drive root / Inside a folder), and Access Control (Project Users / Specific Users
 * + a permission spinner + "apply to future subfolders").
 *
 * <p><b>Validation is invisible to automation.</b> The sheet reports an empty name via
 * {@code etName.error = …}, an EditText error popup that appears in NEITHER
 * {@code getPageSource} NOR {@code uiautomator dump}; the destination error is a Toast,
 * equally invisible. So {@link #isOpen()} — "did the sheet refuse to close" — is the
 * assertion, never the message text.
 */
public class CreateFolderSheet extends BasePage {

    public CreateFolderSheet(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen() {
        return isVisible("etFolderName", Duration.ofSeconds(5))
                || isVisible("btnCreate", Duration.ofSeconds(2));
    }

    public boolean waitOpen(Duration timeout) { return isVisible("etFolderName", timeout); }

    // ---------------- fields ----------------

    public boolean hasNameField()        { return isVisible("etFolderName", Duration.ofSeconds(3)); }
    public boolean hasDescriptionField() { return isVisible("etDescription", Duration.ofSeconds(3)); }
    public boolean hasCharCount()        { return isVisible("tvCharCount", Duration.ofSeconds(3)); }
    public boolean hasAccessControl()    { return isVisible("rgPermissionMode", Duration.ofSeconds(3)); }
    public boolean hasLocationPicker()   { return isVisible("drive_pick_seg_new", Duration.ofSeconds(3)); }
    public boolean hasCreateButton()     { return isVisible("btnCreate", Duration.ofSeconds(3)); }

    public String charCount() { return text("tvCharCount"); }
    public String nameText()  { return text("etFolderName"); }

    public boolean setName(String name) {
        try {
            var el = driver.findElement(byId("etFolderName"));
            el.click(); el.clear(); el.sendKeys(name);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean setDescription(String text) {
        try {
            var el = driver.findElement(byId("etDescription"));
            el.click(); el.clear(); el.sendKeys(text);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- location ----------------

    public boolean chooseRoot()          { return click("drive_pick_seg_new"); }
    public boolean chooseInsideFolder()  { return click("drive_pick_seg_existing"); }

    // ---------------- access control ----------------

    public boolean chooseProjectUsers()  { return click("rbModeProject"); }
    public boolean chooseSpecificUsers() { return click("rbModeSpecific"); }

    public boolean projectUsersCountShown() {
        return isVisible("tvProjectUsersCount", Duration.ofSeconds(3));
    }

    public String projectUsersCount() { return text("tvProjectUsersCount"); }

    public boolean hasPermissionSpinner() {
        return isVisible("spProjectPermission", Duration.ofSeconds(3));
    }

    /** Opens the permission spinner and returns the offered roles (Viewer/Editor/Owner). */
    public java.util.List<String> permissionOptions() {
        try {
            driver.findElement(byId("spProjectPermission")).click();
            Thread.sleep(1200);
            var rows = driver.findElements(io.appium.java_client.AppiumBy.id("android:id/text1"));
            var out = rows.stream().map(org.openqa.selenium.WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
            driver.navigate().back();
            return out;
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    /** Specific Users: search a member and tick their checkbox (role spinner defaults to Viewer). */
    public boolean tickMember(String name) {
        try {
            var search = driver.findElement(byId("etSearchMembers"));
            search.click(); search.clear(); search.sendKeys(name);
            hideKeyboardIfShown();
            Thread.sleep(2500);
            var boxes = driver.findElements(io.appium.java_client.AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvMembers']//android.widget.TextView[@text='" + name
                            + "']/ancestor::*[.//android.widget.CheckBox][1]//android.widget.CheckBox"));
            if (boxes.isEmpty()) boxes = driver.findElements(io.appium.java_client.AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvMembers']//android.widget.CheckBox"));
            if (boxes.isEmpty()) return false;
            boxes.get(0).click();
            Thread.sleep(1500);
            // The row re-renders after the tick (a role spinner is added), so read the sheet's
            // own counter instead of the now-stale checkbox.
            String count = text("tvSelectedCount");
            return count.startsWith("1 ");
        } catch (Exception e) { return false; }
    }

    public String selectedCountText() { return text("tvSelectedCount"); }

    /** The default mode is Project Users; true when Specific Users is pre-selected. */
    public boolean specificModeChecked() {
        try {
            var els = driver.findElements(byId("rbModeSpecific"));
            return !els.isEmpty() && "true".equals(els.get(0).getAttribute("checked"));
        } catch (Exception e) { return false; }
    }

    public boolean anyModeChecked() {
        try {
            for (String id : new String[]{"rbModeProject", "rbModeSpecific"}) {
                var els = driver.findElements(byId(id));
                if (!els.isEmpty() && "true".equals(els.get(0).getAttribute("checked"))) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean memberSearchShown() {
        return isVisible("etSearchMembers", Duration.ofSeconds(4))
                || isVisible("rvMembers", Duration.ofSeconds(2));
    }

    // ---------------- actions ----------------

    /**
     * Taps Create. The sheet is one ScrollView, so with Specific Users (member list shown)
     * the button sits below the fold — hide the keyboard and scroll it into view first.
     */
    public boolean create() {
        hideKeyboardIfShown();
        if (click("btnCreate")) return true;
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().className(\"android.widget.ScrollView\"))"
                            + ".scrollIntoView(new UiSelector().resourceIdMatches(\".*:id/btnCreate\"))"));
        } catch (WebDriverException ignored) { }
        return click("btnCreate");
    }
    public boolean cancel() { return click("btnCancel"); }
    /** Closes the sheet (keyboard first; Back as the fallback) and reports whether it went. */
    public boolean close() {
        hideKeyboardIfShown();
        if (!click("btnClose")) {
            try {
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().className(\"android.widget.ScrollView\"))"
                                + ".scrollIntoView(new UiSelector().resourceIdMatches(\".*:id/btnClose\"))"));
            } catch (WebDriverException ignored) { }
            if (!click("btnClose")) try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        for (int i = 0; i < 10; i++) {
            if (!isVisible("etFolderName", Duration.ofMillis(300))) return true;
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    /** Fills a name and submits; returns true when the sheet actually closed. */
    public boolean createNamed(String name) {
        if (!setName(name)) return false;
        if (!create()) return false;
        for (int i = 0; i < 20; i++) {
            if (!isOpen()) return true;
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }
}
