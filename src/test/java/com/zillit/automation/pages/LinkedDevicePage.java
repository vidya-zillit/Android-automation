package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Settings → "Connect to Zillit from your laptop" (header reads
 * "LINKED DEVICE"), and the QR scanner it opens.
 *
 * <p>Ids confirmed on the device: the instructions live in a single
 * {@code useZillitTv} text view, the button is {@code btnLinkedADevice}, already
 * linked devices are listed in {@code linkedDeviceRec}, and the scanner is a
 * separate screen whose header is {@code headerTv} ("SCAN ZILLIT QR CODE") over
 * a {@code svCamera} preview.
 *
 * <p><b>The scanner screen uses a different header id.</b> This screen has
 * {@code headerText}; the scanner has {@code headerTv}. Checking only the first
 * makes the scanner look like "no screen at all".
 */
public class LinkedDevicePage extends BasePage {

    /** The Settings row that opens this screen. */
    public static final String SETTINGS_ROW = "Connect to Zillit from your laptop";

    /**
     * Fragments of the instruction paragraph, checked individually.
     *
     * <p>Matched as fragments rather than one exact string: the app writes
     * "Step 1 :" with a space before the colon and says "link YOUR mobile
     * device", neither of which matches the wording in the written steps, and the
     * paragraph carries embedded newlines.
     */
    public static final List<String> INSTRUCTION_FRAGMENTS = List.of(
            "Steps to link",
            "mobile device with your computer",
            "Open a browser on your computer",
            "https://web.zillit.com",
            "QR code will appear on the screen",
            "click Link Device button",
            "device linked successfully");

    public LinkedDevicePage(AndroidDriver driver) {
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

    public boolean openLinkedDevice() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"Connect to Zillit\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"Connect to Zillit\")]"
                                    + "/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to LINKED DEVICE from wherever the app is, retrying each hop. */
    public boolean ensureOnLinkedDevice(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isLoaded(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnScanner(Duration.ofMillis(400))) {
                tapBack();
                sleep(1500);
                continue;
            }
            if (isOnSettingsTab(Duration.ofMillis(500))) {
                openLinkedDevice();
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

    // ---------------- LINKED DEVICE ----------------

    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if ("LINKED DEVICE".equalsIgnoreCase(textOf("headerText"))) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** The whole instruction paragraph, newlines and all. */
    public String instructions() {
        return textOf("useZillitTv");
    }

    public boolean isLinkDeviceButtonShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                for (WebElement e : driver.findElements(byId("btnLinkedADevice"))) {
                    if (e.isDisplayed()) {
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

    public String linkDeviceButtonLabel() {
        try {
            for (WebElement e : driver.findElements(byId("btnLinkedADevice"))) {
                String t = safeText(e);
                if (t != null && !t.isBlank()) {
                    return t.trim();
                }
            }
        } catch (WebDriverException ignored) {
        }
        return textOf("buttonBtn");
    }

    public boolean tapLinkDevice() {
        return tapById("btnLinkedADevice");
    }

    // ---------------- QR scanner ----------------

    /**
     * True once the scanner is up.
     *
     * <p>Keyed on {@code svCamera}, the camera preview itself — the honest signal
     * that the camera opened rather than merely that a screen changed.
     */
    public boolean isOnScanner(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("svCamera")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /** The scanner's own header — note the different id from this page's. */
    public String scannerHeader() {
        return textOf("headerTv");
    }

    public String scannerSubtitle() {
        return textOf("titleTv");
    }

    // ---------------- shared ----------------

    public boolean tapBack() {
        return tapById("backBtn");
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(1200);
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
