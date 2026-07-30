package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The main in-project screen (BottomNavigationActivity). Its toolbar shows
 * "Change Project" plus the current project name (tvProjectName) — that pair
 * is the reliable "we are inside a project" marker.
 */
public class BottomNavPage extends BasePage {

    public BottomNavPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Project switching triggers API calls and permission prompts, so this
     * clears any prompts on each poll and allows a generous wait.
     */
    public boolean waitUntilLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            dismissPermissionPrompts();
            if (isVisible("tvProjectName", Duration.ofSeconds(2))
                    || isVisible("bottomNavigation", Duration.ofSeconds(1))) {
                return true;
            }
        }
        return false;
    }

    /** Local copy of the permission dismisser so page methods can self-clear. */
    private void dismissPermissionPrompts() {
        By inAppContinue = byId("tvContinue");
        By systemAllow = AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i)(allow.*|while using the app)\")");
        By noThanks = AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"(?i)(no thanks|not now|skip)\")");
        for (int i = 0; i < 12; i++) {
            if (clickIfPresent(inAppContinue) || clickIfPresent(systemAllow)
                    || clickIfPresent(noThanks)) {
                continue;
            }
            break;
        }
    }

    private boolean clickIfPresent(By locator) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(locator))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** True if an element with the given app resource-id appears within timeout. */
    public boolean textShownOnAppId(String id, Duration timeout) {
        return isVisible(id, timeout);
    }

    public String currentProjectName() {
        try {
            return waitVisible("tvProjectName").getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Opens the in-app project switcher ("Change Project" in the toolbar). */
    public ProjectListPage tapChangeProject() {
        waitVisible("projectContainer").click();
        return new ProjectListPage(driver);
    }
}
