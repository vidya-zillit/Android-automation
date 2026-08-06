package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    /**
     * Runtime resource-id prefix. Flavors rewrite it to the full application id
     * (e.g. com.zillit.zillitapp.dev), so resolve it from the foreground app.
     */
    private static String idPrefix;

    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        if (idPrefix == null) {
            String pkg = com.zillit.automation.core.Config.has("zillit.resourcePackage")
                    ? com.zillit.automation.core.Config.get("zillit.resourcePackage")
                    : driver.getCurrentPackage();
            idPrefix = pkg + ":id/";
        }
    }

    protected By byId(String id) {
        return AppiumBy.id(idPrefix + id);
    }

    /** The app package resource-ids resolve against (config override or foreground app). */
    protected String appPackage() {
        return idPrefix.substring(0, idPrefix.indexOf(":id/"));
    }

    protected WebElement waitVisible(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
    }

    protected boolean isVisible(String id, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Scrolls the first scrollable container until the given id is on screen. */
    protected void scrollToId(String id) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceId(\"" + idPrefix + id + "\"))"));
        } catch (WebDriverException ignored) {
            // Not scrollable or already on screen — the follow-up lookup will decide.
        }
    }

    /** Types into the EditText nested inside a widget container. */
    protected void typeInto(String containerId, String text) {
        scrollToId(containerId);
        WebElement field = waitVisible(containerId)
                .findElement(By.className("android.widget.EditText"));
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
    }

    protected void hideKeyboard() {
        try {
            driver.hideKeyboard();
        } catch (WebDriverException ignored) {
            // Keyboard was not open.
        }
    }

    /** True when any element containing the text (snackbar, toast, label) appears. */
    public boolean textShown(String text, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.androidUIAutomator(
                                    "new UiSelector().textContains(\"" + text + "\")")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean textShown(String text) {
        return textShown(text, Duration.ofSeconds(10));
    }

    /**
     * True only when the ADMIN SETTINGS screen itself is up.
     *
     * <p>Do NOT implement this as {@code textShown("ADMIN SETTINGS")}.
     * {@code UiSelector.textContains()} is case-INSENSITIVE, so that check also
     * matches the Settings tab's row labelled "Admin Settings" — verified on
     * device, where it returned the {@code nameTv} whose text is "Admin
     * Settings". Every page object carried that version, so a caller standing on
     * the Settings tab was told it had already arrived, skipped tapping the row,
     * and then hunted for Admin Settings entries that were never on screen.
     *
     * <p>The two screens are separated on structure instead:
     * {@code adminSettingsRecyclerView} exists only here, and the bottom nav is
     * absent here but always present on the Settings tab.
     */
    protected boolean onAdminSettingsScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("adminSettingsRecyclerView")).isEmpty()
                        && driver.findElements(byId("bottomNavigation")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
                // Tree in flux mid-transition — retry until the budget runs out.
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            pause(200);
        }
    }

    /**
     * True when the SETTINGS TAB is up — the bottom-nav screen carrying the
     * "Admin Settings" row. Mutually exclusive with
     * {@link #onAdminSettingsScreen} by construction: this one requires the
     * bottom nav, that one requires its absence.
     */
    protected boolean onSettingsTabScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                boolean bottomNav = !driver.findElements(byId("bottomNavigation")).isEmpty();
                boolean adminRow = !driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/nameTv\")"
                                + ".textMatches(\"(?i)Admin Settings\")")).isEmpty();
                if (bottomNav && adminRow) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            pause(200);
        }
    }

    /**
     * Private on purpose: JoinUserDetailPage and StartProjectPage already declare
     * their own private pause(long), and a protected one here would clash with
     * both as a weaker-access override.
     */
    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
