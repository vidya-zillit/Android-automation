package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DRIVE SEARCH (SearchFragment / fragment_drive_search.xml) — reached from the Drive
 * drawer → Search.
 *
 * <p>A query box plus type chips: All · Images · Videos · Documents · PDF · Audio ·
 * Folders · Date range.
 */
public class DriveSearchPage extends BasePage {

    /** Chip id → the label a reader would use. */
    private static final List<String[]> CHIPS = List.of(
            new String[]{"chipAll", "All"},
            new String[]{"chipImages", "Images"},
            new String[]{"chipVideos", "Videos"},
            new String[]{"chipDocuments", "Documents"},
            new String[]{"chipPdf", "PDF"},
            new String[]{"chipAudio", "Audio"},
            new String[]{"chipFolders", "Folders"},
            new String[]{"chipDateRange", "Date range"});

    public DriveSearchPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        return isVisible("etSearch", timeout) || isVisible("chipGroupFilters", Duration.ofSeconds(3));
    }

    // ---------------- query ----------------

    public boolean query(String text) {
        try {
            WebElement el = driver.findElement(byId("etSearch"));
            el.click(); el.clear(); el.sendKeys(text);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean clear() {
        try { driver.findElement(byId("btnClear")).click(); Thread.sleep(1200); return true; }
        catch (Exception e) {
            try {
                WebElement el = driver.findElement(byId("etSearch"));
                el.clear();
                return true;
            } catch (WebDriverException x) { return false; }
        }
    }

    public String currentQuery() {
        try { return driver.findElement(byId("etSearch")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    // ---------------- chips ----------------

    /** The chips this build actually renders. */
    public List<String> availableChips() {
        List<String> out = new ArrayList<>();
        for (String[] c : CHIPS) {
            if (isVisible(c[0], Duration.ofMillis(600))) out.add(c[1]);
        }
        return out;
    }

    public boolean tapChip(String label) {
        for (String[] c : CHIPS) {
            if (c[1].equalsIgnoreCase(label)) {
                try {
                    driver.findElement(byId(c[0])).click();
                    Thread.sleep(1500);
                    return true;
                } catch (Exception e) { return false; }
            }
        }
        // fall back to the visible label
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")")).click();
            Thread.sleep(1500);
            return true;
        } catch (Exception e) { return false; }
    }

    // ---------------- results ----------------

    public List<String> resultNames() {
        try {
            return driver.findElements(byId("tvName")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    public int resultCount() { return resultNames().size(); }

    public boolean isEmptyStateShown() {
        return isVisible("emptyState", Duration.ofSeconds(3))
                || isVisible("tvEmptyMessage", Duration.ofSeconds(2));
    }

    public String emptyMessage() {
        try { return driver.findElement(byId("tvEmptyMessage")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean errorShown() { return isVisible("tvError", Duration.ofSeconds(2)); }

    public boolean back() {
        try { driver.findElement(byId("btnBack")).click(); Thread.sleep(1200); return true; }
        catch (Exception e) {
            try { driver.navigate().back(); Thread.sleep(1200); return true; }
            catch (Exception x) { return false; }
        }
    }
}
