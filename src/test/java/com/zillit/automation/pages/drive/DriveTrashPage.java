package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
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

    /**
     * Whether a trashed row named exactly {@code name} exists, scrolling the Trash list
     * ({@code recyclerView}, rows {@code item_trash.xml} → {@code tvName}) past the fold.
     */
    public boolean hasItem(String name) {
        return scrollToRow("recyclerView", "tvName", name);
    }

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

    /**
     * Deletes ONE trashed row for good. Trash rows carry their own buttons — {@code btnRestore}
     * ("Restore") and {@code btnDelete} ("Delete permanently") — there is no per-row ⋯ menu, so
     * the n-th name goes with the n-th delete button. Confirms the "Delete permanently?" dialog.
     */
    /** Taps the row's own Restore ({@code btnRestore}) for {@code name}. */
    public boolean restore(String name) {
        if (!hasItem(name)) return false;
        try {
            List<WebElement> names = driver.findElements(byId("tvName"));
            List<WebElement> btns = driver.findElements(byId("btnRestore"));
            for (int i = 0; i < names.size() && i < btns.size(); i++) {
                if (name.equalsIgnoreCase(names.get(i).getText())) {
                    btns.get(i).click();
                    Thread.sleep(3000);
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Opens the row's "Delete permanently?" dialog without answering it. */
    public boolean tapDeleteForever(String name) {
        if (!hasItem(name)) return false;
        try {
            List<WebElement> names = driver.findElements(byId("tvName"));
            List<WebElement> dels = driver.findElements(byId("btnDelete"));
            for (int i = 0; i < names.size() && i < dels.size(); i++) {
                if (name.equalsIgnoreCase(names.get(i).getText())) { dels.get(i).click(); Thread.sleep(1200); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean dialogShown(String title) { return textShown(title, Duration.ofSeconds(5)); }

    /** Answers the open dialog with its negative button (Cancel). */
    public boolean cancelDialog() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button2\")")).click();
            Thread.sleep(800);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Every trashed name, scrolling the list. */
    public List<String> allNames() {
        // Start from the top: a previous pass leaves the list scrolled to its end.
        for (int i = 0; i < 12; i++) {
            try {
                var size = driver.manage().window().getSize();
                driver.executeScript("mobile: swipeGesture", java.util.Map.of("left", size.width / 2 - 100,
                        "top", (int) (size.height * 0.30), "width", 200, "height", (int) (size.height * 0.45),
                        "direction", "down", "percent", 0.9));
                Thread.sleep(250);
            } catch (Exception ignored) { }
        }
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>(itemNames());
        for (int i = 0; i < 25; i++) {
            int before = seen.size();
            try {
                var size = driver.manage().window().getSize();
                driver.executeScript("mobile: swipeGesture", java.util.Map.of("left", size.width / 2 - 100,
                        "top", (int) (size.height * 0.40), "width", 200, "height", (int) (size.height * 0.35),
                        "direction", "up", "percent", 0.8));
                Thread.sleep(500);
            } catch (Exception ignored) { }
            seen.addAll(itemNames());
            if (seen.size() == before) break;
        }
        return new java.util.ArrayList<>(seen);
    }

    public boolean deleteForever(String name) {
        if (!hasItem(name)) return false;
        try {
            List<WebElement> names = driver.findElements(byId("tvName"));
            List<WebElement> dels = driver.findElements(byId("btnDelete"));
            for (int i = 0; i < names.size() && i < dels.size(); i++) {
                if (name.equalsIgnoreCase(names.get(i).getText())) {
                    dels.get(i).click();
                    Thread.sleep(1200);
                    boolean ok = confirm(Duration.ofSeconds(8));
                    Thread.sleep(2500);
                    return ok;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

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
