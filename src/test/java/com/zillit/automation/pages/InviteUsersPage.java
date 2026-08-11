package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings → "Invite Users" (header reads "SHARE PROJECT CODE").
 *
 * <p>Ids confirmed on the device: the instruction paragraph is {@code textView6},
 * the code is {@code invite_code_view} rendered as "CODE: 983246", and the
 * illustration is {@code imageView5}.
 *
 * <p><b>There is no Share button element.</b> The share control is an icon drawn
 * INSIDE the code row — it has no id of its own and is not reported as
 * clickable, so only {@code invite_code_view} covers that area. Reaching it would
 * mean tapping the right-hand end of that row by coordinate.
 *
 * <p><b>The code is per project and regenerates</b>, so only its shape can be
 * asserted, never a fixed value.
 */
public class InviteUsersPage extends BasePage {

    /** The Settings row that opens this screen. */
    public static final String SETTINGS_ROW = "Invite Users";

    /** Exactly what the screen says — confirmed character for character. */
    public static final String INVITE_TEXT =
            "The only way anyone can join this project is through this digital code listed, "
                    + "so press 'Share' below to send an invitation. If you are an Admin, you can "
                    + "pre-approve users through 'Admin Settings' > 'Pre-Approved Users'";

    /** "CODE: " followed by digits. The observed code was six digits starting 98. */
    private static final Pattern CODE = Pattern.compile("CODE:\\s*(\\d+)");

    public InviteUsersPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    sleep(2000);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    public boolean openInviteUsers() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + SETTINGS_ROW + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + SETTINGS_ROW
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to SHARE PROJECT CODE from wherever the app is, retrying each hop. */
    public boolean ensureOnInviteUsers(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isLoaded(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnSettingsTab(Duration.ofMillis(500))) {
                openInviteUsers();
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

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the screen ----------------

    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("SHARE PROJECT CODE".equalsIgnoreCase(textOf("headerText"))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** The instruction paragraph shown above the illustration. */
    public String inviteText() {
        return textOf("textView6");
    }

    /** The raw label, e.g. "CODE: 983246". */
    public String codeLabel() {
        return textOf("invite_code_view");
    }

    /** Just the digits, or "" when the label has not rendered yet. */
    public String projectCode() {
        Matcher m = CODE.matcher(codeLabel());
        return m.find() ? m.group(1) : "";
    }

    /** Waits for the code to arrive — it renders a beat after the screen. */
    public boolean codeShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (!projectCode().isEmpty()) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    public boolean tapBack() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId("backBtn")))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? "" : String.valueOf(safeText(found.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
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
