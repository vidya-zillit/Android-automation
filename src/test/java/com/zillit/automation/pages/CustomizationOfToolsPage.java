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

    /**
     * True only when the tools screen itself is up.
     *
     * <p>Deliberately does NOT look for the text "Customization of tools". The
     * ADMIN SETTINGS screen carries a row with exactly that label — it is the
     * entry point to this screen — and {@code textShown()} matches with
     * textContains, so the old check answered "loaded" while still sitting on
     * Admin Settings. Callers then hunted for tool rows that were never on
     * screen, which is what made the re-enable loop fail on its very first tool.
     *
     * <p>{@code submitBtn} (Update) exists here and not on Admin Settings, so it
     * is the honest marker. The row list is waited for separately because it
     * arrives from an API call a beat after the button renders.
     */
    public boolean isLoaded() {
        // PRESENCE, not visibility. submitBtn ("Update") sits below the fold on
        // this scrollable screen, so isVisible() answers "no" for a screen that
        // is plainly open — which made every case skip and sent the recovery
        // path backing out to the bottom nav (i.e. the Tools tab) instead of
        // staying in Admin Settings -> Customization where the flow belongs.
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (hasUpdateButton() && !toolNames().isEmpty()) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    /** Update button present in the tree (Admin Settings has no such button). */
    private boolean hasUpdateButton() {
        try {
            return !driver.findElements(byId("submitBtn")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Waits until the tool rows have actually populated (they load via an API call). */
    public boolean waitForToolsLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!toolNames().isEmpty()) {
                return true;
            }
            sleep(300);
        }
        return false;
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

    /**
     * True if the tool's tick (infoIv) is present — the icon is absent from the
     * tree entirely when the tool is disabled.
     *
     * <p>Scrolls the row into view FIRST. UiAutomator only exposes on-screen
     * RecyclerView rows, so a viewport-only query returns "no tick" for every
     * tool below the fold — i.e. reports enabled tools as disabled. Both toggle
     * loops branch on this answer, so getting it wrong either skips a tool that
     * needed disabling or taps an already-enabled one, which opens the confirm
     * dialog and leaves a modal blocking the next tap.
     */
    public boolean isToolEnabled(String name) {
        if (!scrollToTool(name)) {
            return false;   // genuinely not in the list
        }
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
        if (!scrollToTool(name)) {
            return false;
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(toolSelector(name))).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Brings the named row on screen. Cheap no-op when it is already visible. */
    private boolean scrollToTool(String name) {
        String sel = toolSelector(name);
        try {
            driver.findElement(AppiumBy.androidUIAutomator(sel));
            return true;                     // already on screen — don't disturb the scroll
        } catch (WebDriverException notYet) {
            // fall through and go looking for it
        }
        scrollToBeginning();
        for (int i = 0; i < 20; i++) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(sel));
                return true;
            } catch (WebDriverException notVisible) {
                swipeUp();
            }
        }
        return false;
    }

    private String toolSelector(String name) {
        return "new UiSelector().resourceIdMatches(\".*:id/nameTv\").text(\"" + name + "\")";
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
        sleep(400);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
