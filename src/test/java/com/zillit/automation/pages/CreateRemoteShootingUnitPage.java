package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Create Remote Shooting Units".
 *
 * Same underlying screen/Activity as Create Additional Shooting Unit
 * (AccAndCaterSettingsActivity, identical add_fab/editTextInput/buttonBtn ids
 * and the same native field `error` attribute validation, confirmed byte-for-
 * byte identical message text) — just scoped to "remote" units instead of
 * "additional" ones. The add form also has Select Admin / Select Member
 * fields, but they are optional for name-length validation and creation.
 *
 * A created remote unit shows up as its own entry in the Project List
 * ("<unit name>(<parent project>) - Admin") — it is effectively its own
 * mini-project workspace, which is why it can independently be found via
 * search and have its own Admin Settings → Delete Project flow.
 */
public class CreateRemoteShootingUnitPage extends BasePage {

    public CreateRemoteShootingUnitPage(AndroidDriver driver) {
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

    public boolean openCreateRemoteShootingUnits() {
        return tapListRow("Create Remote Shooting Units");
    }

    private boolean tapListRow(String label) {
        // Bring the row on screen FIRST. Admin Settings is a long list and it keeps
        // its scroll position, so after visiting another screen and coming back a
        // row can sit off-screen — an unscrolled lookup then just times out and the
        // flow stops without ever opening the screen it was asked for.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list, or already visible - the lookup below decides.
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
     * True on either the unit list (add_fab) or the add form it opens
     * (editTextInput) — a failed Submit leaves the form open rather than
     * returning to the list, and that's still "on this feature."
     */
    public boolean isLoaded() {
        return isVisible("add_fab", Duration.ofSeconds(15)) || isVisible("editTextInput", Duration.ofSeconds(5));
    }

    public boolean tapAddFab() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("add_fab")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isAddFormOpen() {
        return isVisible("editTextInput", Duration.ofSeconds(8));
    }

    /** Bounded wait + explicit click before clear()/sendKeys() — an unfocused EditText drops sendKeys() silently. */
    public void setUnitName(String value) {
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

    /**
     * The Unit Name length validation is exposed as the EditText's native
     * `error` attribute, not as visible text/toast — a textShown() check can
     * never match it, so this greps the raw page source instead.
     */
    public boolean hasValidationError(String message) {
        return driver.getPageSource().contains(message);
    }

    /** Extracts the live text of the Unit Name field's native `error` attribute, or null if not set. */
    public String unitNameFieldError() {
        String src = driver.getPageSource();
        int idx = src.indexOf("error=\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + 7;
        int end = src.indexOf('"', start);
        return end > start ? src.substring(start, end) : null;
    }

    public boolean isUnitListed(String name, Duration timeout) {
        return textShown(name, timeout);
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
        return onAdminSettingsScreen(timeout);
    }
}
