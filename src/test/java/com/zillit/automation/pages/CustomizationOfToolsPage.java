package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings → Admin Settings → "Customization of tools" — a distinct nav entry
 * from the same "ToolsEnableDisablePage" screen {@link ToolsPage} reaches via
 * the Tools tab banner (identical resource-ids: nameTv/infoIv/submitBtn),
 * confirmed by live inspection. Kept as its own page (rather than reusing
 * ToolsPage) since ToolsPage is a pre-existing file this session does not
 * modify.
 *
 * Enabling a tool applies instantly with no confirmation; disabling shows an
 * AlertDialog ("Disabling the tool will deactivate all associated
 * communication and data in it. Do you still want to proceed?" / Cancel /
 * Yes) and only removes the tick once Yes is tapped. Multiple tools can be
 * toggled locally before a single tap on Update persists them all — tapping
 * Update with a real pending change saves and auto-navigates back to Admin
 * Settings; with no change it just refreshes this screen in place.
 */
public class CustomizationOfToolsPage extends BasePage {

    public CustomizationOfToolsPage(AndroidDriver driver) {
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

    public boolean openCustomizationOfTools() {
        return tapListRow("Customization of tools");
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
        return textShown("Customization of tools", Duration.ofSeconds(10))
                || isVisible("submitBtn", Duration.ofSeconds(5));
    }

    /** Tool names currently rendered (viewport only — the list scrolls). */
    public List<String> toolNames() {
        try {
            return driver.findElements(byId("nameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    /** Every tool name in the list, scrolling from the top to collect the full set. */
    public List<String> allToolNamesScrolled() {
        scrollToBeginning();
        Set<String> seen = new LinkedHashSet<>(toolNames());
        for (int i = 0; i < 20; i++) {
            int before = seen.size();
            swipeUp();
            seen.addAll(toolNames());
            if (seen.size() == before && i > 0) break;   // reached the bottom
        }
        return List.copyOf(seen);
    }

    /** True if the tool's tick (infoIv) is present — absent entirely when disabled. */
    public boolean isToolEnabled(String name) {
        try {
            return !driver.findElements(AppiumBy.xpath(
                    "//*[contains(@resource-id,'nameTv') and @text=\"" + name
                            + "\"]/following-sibling::*[contains(@resource-id,'infoIv')]")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps a tool row by name (scrolls to find it). Enabling applies instantly; disabling opens a confirm dialog. */
    public boolean tapTool(String name) {
        String sel = "new UiSelector().resourceIdMatches(\".*:id/nameTv\").text(\"" + name + "\")";
        scrollToBeginning();
        for (int i = 0; i < 20; i++) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(sel)).click();
                return true;
            } catch (WebDriverException notVisible) {
                swipeUp();
            }
        }
        return false;
    }

    public boolean isDisableConfirmDialogOpen(Duration timeout) {
        return textShown("Disabling the tool will deactivate all associated communication and data in it", timeout);
    }

    public boolean tapCancel() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(byId("leftButton")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapYes() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(byId("rightButton")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps Update. With a real pending change this saves and returns to Admin Settings. */
    public boolean tapUpdate() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("submitBtn")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return textShown("ADMIN SETTINGS", timeout);
    }

    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void scrollToBeginning() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(10)"));
        } catch (WebDriverException ignored) {
        }
    }

    private void swipeUp() {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        int x = size.getWidth() / 2;
        int yFrom = (int) (size.getHeight() * 0.75);
        int yTo = (int) (size.getHeight() * 0.35);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, yFrom));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, yTo));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
