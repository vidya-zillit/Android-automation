package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Create Additional Shooting Unit".
 *
 * The list screen (add_fab) opens a "Create/Update Project Join Unit" form
 * (editTextInput + buttonBtn Submit). A failed Submit does NOT show a toast —
 * the validation message is set as the EditText's native `error` attribute
 * (Android's setError()), which only shows up in the page source, not via a
 * text-based UiAutomator selector — hence hasValidationError() below instead
 * of the textShown()-based toast checks used elsewhere. A successful Submit
 * navigates back to the list automatically, and the new unit appears in the
 * list after a short delay.
 */
public class CreateAdditionalShootingUnitPage extends BasePage {

    public CreateAdditionalShootingUnitPage(AndroidDriver driver) {
        super(driver);
    }

    /** Same menuText-in-bottomNavigation idiom as ToolsPage.openToolsTab(), for the Settings tab. */
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

    public boolean openCreateAdditionalShootingUnit() {
        return tapListRow("Create Additional Shooting Unit");
    }

    /**
     * Same ancestor-row xpath idiom already used by CompanyDetailsPage.tapListRow()
     * and DepartmentListingOrderPage.tapListRow(), with a bounded wait — this is
     * called right after an Activity transition, and an instant lookup can fire
     * before that screen's RecyclerView has finished rendering.
     */
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

    /**
     * True on either the unit list (add_fab) or the add/edit form it opens
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

    /** Searches the list's own search bar (same search widget id used elsewhere in the app). */
    public void searchUnit(String query) {
        WebElement field = waitVisible("search_widget_edit_textvw");
        field.clear();
        if (!query.isEmpty()) {
            field.sendKeys(query);
        }
        hideKeyboard();
    }

    /**
     * Taps the edit (pencil) icon on the row whose label matches the given
     * unit name exactly — labelTextView and editImage are siblings under the
     * same row container.
     */
    public boolean tapEditIcon(String unitName) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'labelTextView') and @text=\"" + unitName
                                    + "\"]/following-sibling::*[contains(@resource-id,'editImage')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Current text in the Unit Name field — used to verify edit-form prefill. */
    public String unitNameFieldValue() {
        return driver.findElement(byId("editTextInput")).getText();
    }

    /**
     * Taps the delete (trash) icon on the row whose label matches the given
     * unit name exactly — same labelTextView/deleteImage sibling idiom as
     * tapEditIcon().
     */
    public boolean tapDeleteIcon(String unitName) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'labelTextView') and @text=\"" + unitName
                                    + "\"]/following-sibling::*[contains(@resource-id,'deleteImage')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The delete confirmation is a real Android Snackbar ("Are you sure you
     * want to delete this unit?" + a "Yes" action) that auto-dismisses after
     * a few seconds — this must be tapped promptly after tapDeleteIcon().
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

    /** True once the given unit's row is no longer present. */
    public boolean isUnitRemoved(String name, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + name + "\")")));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Bounded wait + explicit click before clear()/sendKeys() — an unfocused
     * EditText silently drops sendKeys() text without an explicit tap first.
     */
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

    /** Same buttonBtn element as tapSubmit() — the edit flow just labels it "Update". */
    public void tapUpdate() {
        tapSubmit();
    }

    /**
     * The Unit Name length validation is exposed as the EditText's native
     * `error` attribute, not as visible text/toast — a textShown() check can
     * never match it, so this greps the raw page source instead.
     */
    public boolean hasValidationError(String message) {
        return driver.getPageSource().contains(message);
    }

    /** True once the newly created unit appears back on the list screen. */
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
        return textShown("ADMIN SETTINGS", timeout);
    }
}
