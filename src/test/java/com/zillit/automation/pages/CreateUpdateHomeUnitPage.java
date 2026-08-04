package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Create/Update Home Units." (list label has a
 * trailing period; menu list still matches on a contains()).
 *
 * Validation here shows as a real Android system {@code android.widget.Toast}
 * — confirmed via live exploration to be invisible to a UiSelector-based
 * lookup (the mechanism {@link BasePage#textShown} uses) even though it IS
 * present in {@code getPageSource()}. hasToastMessage() below polls the page
 * source directly for exactly that reason — textShown() would never find it.
 *
 * Disabling a unit asks for confirmation via a native AlertDialog ("Alert" /
 * "Are you sure you want to disable this unit ?" / No-Yes); enabling needs no
 * confirmation. Deleting uses the usual Snackbar ("Are you sure you want to
 * delete?" + "Yes") that auto-dismisses — must be tapped promptly.
 *
 * A unit shows up as its own top tab on the Home screen (HomeChatPage's
 * recTab strip) while enabled, and disappears from it while disabled.
 */
public class CreateUpdateHomeUnitPage extends BasePage {

    public CreateUpdateHomeUnitPage(AndroidDriver driver) {
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

    public boolean openCreateUpdateHomeUnits() {
        return tapListRow("Create/Update Home Unit");
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

    /**
     * True on either the unit list (addFb) or the add form it opens
     * (unitNameEt) — a failed Submit leaves the form open rather than
     * returning to the list, and that's still "on this feature."
     */
    public boolean isLoaded() {
        return isVisible("addFb", Duration.ofSeconds(15)) || isVisible("unitNameEt", Duration.ofSeconds(5));
    }

    public boolean tapAddFab() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("addFb")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isAddFormOpen() {
        return isVisible("unitNameEt", Duration.ofSeconds(8));
    }

    /** Bounded wait + explicit click before clear()/sendKeys() — an unfocused EditText drops sendKeys() silently. */
    public void setUnitName(String value) {
        WebElement field = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(byId("unitNameEt")));
        field.click();
        field.clear();
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
        hideKeyboard();
    }

    public void tapSubmit() {
        new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.elementToBeClickable(byId("buttonBtn")))
                .click();
    }

    /**
     * Polls getPageSource() directly for the given text — the validation
     * message is a real system Toast, which a UiSelector-based textShown()
     * check cannot see even though it IS present in the page source.
     */
    public boolean hasToastMessage(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (driver.getPageSource().contains(message)) {
                return true;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public boolean isUnitListed(String name, Duration timeout) {
        return textShown(name, timeout);
    }

    public void searchUnit(String query) {
        WebElement field = waitVisible("search_widget_edit_textvw");
        field.clear();
        if (!query.isEmpty()) {
            field.sendKeys(query);
        }
        hideKeyboard();
    }

    /** True if the row's toggle is currently ON. */
    public boolean isUnitEnabled(String name) {
        try {
            return Boolean.parseBoolean(driver.findElement(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and @text=\"" + name
                                    + "\"]/following-sibling::*[contains(@resource-id,'switchBtn')]"))
                    .getAttribute("checked"));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps the row's enable/disable toggle. Disabling triggers a confirmation AlertDialog; enabling does not. */
    public boolean toggleUnit(String name) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and @text=\"" + name
                                    + "\"]/following-sibling::*[contains(@resource-id,'switchBtn')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Polls the row's toggle state until it matches expected or the timeout
     * elapses, returning whatever was last observed. Disabling is gated behind
     * a confirmation dialog, which naturally gives the switch's "checked"
     * attribute time to settle before it's read; re-enabling has no such
     * built-in delay, so a bare isUnitEnabled() right after the tap can read
     * the attribute before the app finishes updating it.
     */
    public boolean waitUnitEnabled(String name, boolean expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        boolean last = isUnitEnabled(name);
        while (last != expected && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            last = isUnitEnabled(name);
        }
        return last;
    }

    public boolean isDisableConfirmDialogOpen(Duration timeout) {
        return textShown("Are you sure you want to disable this unit ?", timeout);
    }

    /** Confirms the disable AlertDialog ("Yes" = rightButton). */
    public boolean confirmDisableYes() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(byId("rightButton")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Taps the delete (trash) icon on the row whose label matches the given
     * unit name exactly — nameTv and icDelete are siblings under the same
     * row container.
     */
    public boolean tapDeleteIcon(String name) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and @text=\"" + name
                                    + "\"]/following-sibling::*[contains(@resource-id,'icDelete')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The delete confirmation is a real Android Snackbar ("Are you sure you
     * want to delete?" + a "Yes" action) that auto-dismisses after a few
     * seconds — this must be tapped promptly after tapDeleteIcon().
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

    public boolean isUnitRemoved(String name, Duration timeout) {
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
