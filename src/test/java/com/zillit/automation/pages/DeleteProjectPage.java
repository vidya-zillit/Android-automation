package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings → Admin Settings → "Delete Project", as driven by
 * {@link com.zillit.automation.tests.CreateRemoteShootingUnitTest} Test Cases 5
 * and 6, which open the delay sheet on a freshly created shooting unit and
 * schedule its deletion.
 *
 * <p>Scope is deliberately narrow: the row, the delay sheet and its options, the
 * result message, and back. The full lifecycle — arming, stopping, the
 * confirmation dialog and the Settings-tab countdown — lives in
 * {@link DeleteprojectEntertainment}, which backs its own suite. The two are kept
 * apart on purpose so a change to that module cannot disturb this one.
 *
 * <p>Ids confirmed on the device: the Admin Settings row is a {@code nameTv}, the
 * sheet is {@code header} plus {@code valueTextView} rows (12 Hours, 24 Hours,
 * 48 Hours, Cancel), and the result arrives as a {@code snackbar_text} snackbar.
 */
public class DeleteProjectPage extends BasePage {

    public DeleteProjectPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    sleep(1500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/menuText\").textMatches(\"(?i)Settings\")"))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    /**
     * Gets to ADMIN SETTINGS from wherever the app is, and confirms it arrived.
     *
     * <p>Replaces a pair of blind calls that assumed the app was sitting on the
     * bottom nav. From the SETTINGS TAB, tapping "Settings" again does not
     * renavigate, so the follow-up row tap could miss and leave the flow
     * stranded — and because the callers guarded it with an assumption, the case
     * SKIPPED rather than failing, hiding the problem.
     */
    public boolean ensureOnAdminSettings(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOnAdminSettings(Duration.ofMillis(800))) {
                return true;
            }
            if (onSettingsTabScreen(Duration.ofMillis(500))) {
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

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapDeleteProject() {
        return tapListRow("Delete Project");
    }

    private boolean tapListRow(String label) {
        // The Admin Settings list is long and keeps its scroll position, so bring
        // the row on screen before looking for it.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The delay sheet doubles as the delete confirmation. */
    public boolean isConfirmDialogOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("valueTextView")).isEmpty()
                        && !driver.findElements(byId("header")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    /** Options currently offered by the sheet. */
    public List<String> options() {
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

    public boolean hasOption(String label, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (options().stream().anyMatch(o -> o.equalsIgnoreCase(label))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** Taps an option by EXACT label — "Cancel" must not match anything else. */
    public boolean tapOption(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'valueTextView') and @text=\"" + label + "\"]")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean hasMessage(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                for (WebElement e : driver.findElements(byId("snackbar_text"))) {
                    String t = safeText(e);
                    if (t != null && t.contains(message)) {
                        return true;
                    }
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    public boolean tapBackButton() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("backBtn")))
                    .click();
            sleep(900);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String safeText(WebElement e) {
        try {
            return e.getText();
        } catch (WebDriverException stale) {
            return "";
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
