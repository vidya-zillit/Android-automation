package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Delete Project" — a BottomSheetDialog with
 * "Are you sure you want to delete this project?" and four options (12/24/48
 * Hours + Cancel), each a valueTextView row (same idiom as the country picker
 * in CompanyDetailsPage). Selecting a duration schedules real deletion — the
 * project remains recoverable via the "Stop Project Deletion" row that then
 * replaces "Delete Project" in this same list, until the window elapses.
 */
public class DeleteProjectPage extends BasePage {

    public DeleteProjectPage(AndroidDriver driver) {
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
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"Admin Settings\")]"
                                    + "/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** "Delete Project" is near the bottom of a long, dynamically-ordered list — scroll to find it. */
    public boolean tapDeleteProject() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"Delete Project\"))"));
        } catch (WebDriverException ignored) {
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"Delete Project\")]"
                                    + "/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isConfirmDialogOpen(Duration timeout) {
        return textShown("Are you sure you want to delete this project?", timeout);
    }

    public boolean hasOption(String label, Duration timeout) {
        return textShown(label, timeout);
    }

    public boolean tapOption(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceId(\"" + appPackage() + ":id/valueTextView\")"
                                    + ".text(\"" + label + "\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean hasMessage(String message, Duration timeout) {
        return textShown(message, timeout);
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
}
