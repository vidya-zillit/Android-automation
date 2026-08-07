package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * DRIVE TRASH (TrashFragment / fragment_trash.xml) — reached from the Drive drawer →
 * Trash. Lists soft-deleted items, offers per-item restore / permanent delete, and an
 * "Empty Trash" action in the header.
 */
public class DriveTrashPage extends BasePage {

    public DriveTrashPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        return isVisible("btnEmptyTrash", timeout)
                || isVisible("recyclerView", Duration.ofSeconds(3))
                || isVisible("emptyState", Duration.ofSeconds(3));
    }

    public boolean hasEmptyTrash() { return isVisible("btnEmptyTrash", Duration.ofSeconds(5)); }

    public boolean isEmptyStateShown() {
        return isVisible("emptyState", Duration.ofSeconds(3));
    }

    /** Rendered rows only — scroll-collect for the full set. */
    public List<String> itemNames() {
        try {
            return driver.findElements(byId("tvName")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    public int itemCount() { return itemNames().size(); }

    /** Opens the per-item menu (restore / delete permanently) for a trashed item. */
    public boolean openItemMenu(String name) {
        try {
            List<WebElement> names = driver.findElements(byId("tvName"));
            List<WebElement> menus = driver.findElements(byId("btnMore"));
            for (int i = 0; i < names.size() && i < menus.size(); i++) {
                String t = names.get(i).getText();
                if (t != null && t.equalsIgnoreCase(name)) { menus.get(i).click(); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public boolean restore() { return tapText("Restore"); }

    public boolean deleteForever() {
        return tapText("Delete permanently") || tapText("Delete forever") || tapText("Delete");
    }

    public boolean emptyTrash() {
        try { driver.findElement(byId("btnEmptyTrash")).click(); Thread.sleep(1500); return true; }
        catch (Exception e) { return false; }
    }

    /**
     * Confirms a Trash dialog. Drive uses a "Delete"/"Cancel" pair rather than the
     * app-wide "Yes"/"No", so the shared confirm helper does not dismiss it.
     */
    public boolean confirm(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/button1\")")).click();
                return true;
            } catch (WebDriverException ignored) { }
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    public boolean back() {
        try { driver.findElement(byId("btnBack")).click(); Thread.sleep(1200); return true; }
        catch (Exception e) {
            try { driver.navigate().back(); Thread.sleep(1200); return true; }
            catch (Exception x) { return false; }
        }
    }

    private boolean tapText(String t) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + t + "\")")).click();
            Thread.sleep(1200);
            return true;
        } catch (Exception e) { return false; }
    }
}
