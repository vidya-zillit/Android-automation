package com.zillit.automation.pages.common;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Clicks the control if it is on screen RIGHT NOW — no waiting.
     *
     * <p>This used to wait 2s per locator. It is called for three locators, in a loop of
     * up to twelve, on every poll of {@link #waitUntilLoaded(Duration)} — so entering a
     * project burned ~6 seconds per poll looking for permission dialogs that were not
     * there, before it even checked whether the screen had loaded. Waiting for a dialog
     * that has not appeared is pointless anyway: the enclosing poll loop is the wait, and
     * it comes back around in a moment.
     */
    private boolean clickIfPresent(By locator) {
        try {
            for (WebElement candidate : driver.findElements(locator)) {
                if (candidate.isDisplayed()) {
                    candidate.click();
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
            // Gone between the find and the click — treat as nothing to do.
        }
        return false;
    }

    /**
     * Taps a BOTTOM-NAV tab by its label (Home / Email / Tools / C&C / Settings).
     *
     * <p>Scoped to the bottomNavigation container on purpose: the nav tabs and the Home
     * UNIT chips both use the id {@code menuText}, so an unscoped lookup happily taps a
     * unit chip believing it tapped a tab — the same trap documented on
     * {@code HomeChatPage.unitChipElements()}.
     */
    public boolean selectTab(String label) {
        try {
            WebElement nav = driver.findElement(byId("bottomNavigation"));
            for (WebElement tab : nav.findElements(byId("menuText"))) {
                String text = tab.getText();
                if (text != null && text.trim().equalsIgnoreCase(label)) {
                    tab.click();
                    Thread.sleep(1200);
                    return true;
                }
            }
        } catch (WebDriverException | InterruptedException ignored) {
            // Nav not up, or the tab is absent on this build — caller decides.
        }
        return false;
    }

    /** The bottom-nav tab labels, for diagnostics when a tap finds nothing. */
    public List<String> tabLabels() {
        List<String> labels = new ArrayList<>();
        try {
            WebElement nav = driver.findElement(byId("bottomNavigation"));
            for (WebElement tab : nav.findElements(byId("menuText"))) {
                String text = tab.getText();
                if (text != null && !text.isBlank()) labels.add(text.trim());
            }
        } catch (WebDriverException ignored) {
        }
        return labels;
    }

    /** Switches to the Home tab — the only tab that carries the units strip. */
    public boolean goHome() {
        return selectTab("Home");
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
