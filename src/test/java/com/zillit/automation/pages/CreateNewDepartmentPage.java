package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Create New Department".
 *
 * A single "Enter Department Name" field (editTextInput) + Submit (buttonBtn)
 * above a live DEPARTMENTS list (memberNameTv rows; only user-created
 * departments show a deleteIv trash icon — built-in departments don't).
 * Unlike Create Additional Shooting Unit, validation here IS a real Snackbar
 * (snackbar_text/snackbar_action), not a native field `error` attribute.
 */
public class CreateNewDepartmentPage extends BasePage {

    public CreateNewDepartmentPage(AndroidDriver driver) {
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

    public boolean openCreateNewDepartment() {
        return tapListRow("Create New Department");
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

    /** Bounded wait + explicit click before clear()/sendKeys() — an unfocused EditText drops sendKeys() silently. */
    public void setDepartmentName(String value) {
        WebElement field = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(byId("editTextInput")));
        field.click();
        field.clear();
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
        hideKeyboard();
    }

    public String departmentNameFieldValue() {
        return driver.findElement(byId("editTextInput")).getText();
    }

    public void tapSubmit() {
        driver.findElement(byId("buttonBtn")).click();
    }

    /** Real Snackbar validation/success messages (unlike Shooting Unit's field `error` attribute). */
    public boolean hasMessage(String message, Duration timeout) {
        return textShown(message, timeout);
    }

    /**
     * The empty-name Snackbar text is identical to the field's own hint text
     * ("Enter Department Name"), so a plain textShown() would always be true
     * even with no Snackbar — .instance(1) requires a SECOND simultaneous
     * match (hint + Snackbar) to exist, which only happens when the Snackbar
     * is actually showing.
     */
    public boolean hasEmptyNameSnackbar(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"Enter Department Name\").instance(1)")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The DEPARTMENTS list is long and a newly created department can land
     * well below the fold — RecyclerView only keeps rendered items near the
     * viewport, so a lookup can miss it entirely until scrolled into view.
     * UiScrollable.scrollIntoView() only scrolls forward (down) and never
     * reverses, so if an earlier step already left the list scrolled past
     * the target, it would never be found — scrolling to the top first
     * guarantees a known starting position before searching forward.
     */
    private void scrollToText(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(10)"));
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + text + "\"))"));
        } catch (WebDriverException ignored) {
            // Not scrollable or already on screen — the follow-up lookup will decide.
        }
    }

    public boolean isDepartmentListed(String name, Duration timeout) {
        scrollToText(name);
        return textShown(name, timeout);
    }

    /**
     * Taps the delete (trash) icon on the row whose label matches the given
     * department name exactly — memberNameTv and deleteIv are siblings under
     * the same row container. Only user-created departments have this icon.
     */
    public boolean tapDeleteIcon(String departmentName) {
        scrollToText(departmentName);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'memberNameTv') and @text=\"" + departmentName
                                    + "\"]/following-sibling::*[contains(@resource-id,'deleteIv')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The delete confirmation is a real Android Snackbar ("Do You really
     * want to delete this department?" + a "Yes" action) that auto-dismisses
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

    public boolean isDepartmentRemoved(String name, Duration timeout) {
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
