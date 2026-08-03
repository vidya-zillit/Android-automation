package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Create New Designation".
 *
 * A department Spinner (autoCompleteDropDown) + "Enter Designation" field
 * (editTextInput) + Submit (buttonBtn), above a live DESIGNATIONS list
 * (memberNameTv rows) filtered by whichever department is selected.
 *
 * The Spinner's popup list items are NOT exposed to the accessibility tree
 * at all (confirmed empty even while the popup is visibly open — neither
 * uiautomator dump nor Appium's own getPageSource() show them), and touch
 * scroll+tap is unreliable because the list keeps drifting from fling
 * momentum well after it visually looks settled. D-pad key navigation
 * sidesteps both problems: it moves by discrete logical items regardless of
 * scroll physics, so pressing DOWN far more times than the list could ever
 * contain deterministically lands on the last item, then a fixed number of
 * UP presses reaches the target (see selectDepartmentFromEnd()).
 */
public class CreateNewDesignationPage extends BasePage {

    public CreateNewDesignationPage(AndroidDriver driver) {
        super(driver);
    }

    /** Same menuText-in-bottomNavigation idiom used by the other Admin Settings pages. */
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

    public boolean openCreateNewDesignation() {
        return tapListRow("Create New Designation");
    }

    private boolean tapListRow(String label) {
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

    public boolean isLoaded() {
        return isVisible("editTextInput", Duration.ofSeconds(15));
    }

    public String selectedDepartment() {
        return driver.findElement(byId("autoCompleteDropDown")).getText();
    }

    /**
     * Opens the department Spinner and selects the item {@code upsFromLastItem}
     * positions above the last one (0 = last item, 1 = second-to-last, ...).
     * The exact position of a given department name in this project's list is
     * discovered by live exploration (scroll to the true end and count back),
     * not assumed — it will differ if the department list itself changes.
     */
    public void selectDepartmentFromEnd(int upsFromLastItem) {
        driver.findElement(byId("autoCompleteDropDown")).click();
        for (int i = 0; i < 60; i++) {
            driver.pressKey(new KeyEvent(AndroidKey.DPAD_DOWN));
        }
        for (int i = 0; i < upsFromLastItem; i++) {
            driver.pressKey(new KeyEvent(AndroidKey.DPAD_UP));
        }
        driver.pressKey(new KeyEvent(AndroidKey.ENTER));
    }

    /** Bounded wait + explicit click before clear()/sendKeys() — an unfocused EditText drops sendKeys() silently. */
    public void setDesignationName(String value) {
        WebElement field = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(byId("editTextInput")));
        field.click();
        field.clear();
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
        hideKeyboard();
    }

    public void tapSubmit() {
        driver.findElement(byId("buttonBtn")).click();
    }

    public boolean hasMessage(String message, Duration timeout) {
        return textShown(message, timeout);
    }

    /**
     * The empty-name Snackbar text is identical to the field's own hint text
     * ("Enter Designation"), so a plain textShown() would always be true even
     * with no Snackbar — .instance(1) requires a SECOND simultaneous match
     * (hint + Snackbar) to exist, which only happens when the Snackbar is
     * actually showing.
     */
    public boolean hasEmptyNameSnackbar(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"Enter Designation\").instance(1)")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * A newly created designation is appended at the end of the DESIGNATIONS
     * list, which can be well below the fold — scroll to the top first (since
     * UiScrollable.scrollIntoView() only scrolls forward and never reverses)
     * before searching, so a prior step's scroll position can't hide it.
     */
    public boolean isDesignationListed(String name, Duration timeout) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(10)"));
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + name + "\"))"));
        } catch (WebDriverException ignored) {
        }
        return textShown(name, timeout);
    }

    /**
     * Taps the delete (trash) icon on the row whose label matches the given
     * designation name exactly — memberNameTv and deleteIv are siblings under
     * the same row container. Only user-created designations have this icon.
     */
    public boolean tapDeleteIcon(String designationName) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'memberNameTv') and @text=\"" + designationName
                                    + "\"]/following-sibling::*[contains(@resource-id,'deleteIv')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The delete confirmation is a real Android Snackbar ("Do You really want
     * to delete this Designation?" + a "Yes" action) that auto-dismisses
     * after a few seconds — this must be tapped promptly after tapDeleteIcon().
     */
    public boolean confirmDeleteYes() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(byId("snackbar_action")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isDesignationRemoved(String name, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + name + "\")")));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Top toolbar back arrow. */
    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return textShown("ADMIN SETTINGS", timeout);
    }
}
