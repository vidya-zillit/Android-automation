package com.zillit.automation.pages.location;

import com.zillit.automation.pages.map.LocationToolPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The third level of the Location tool: one location's ENTRIES ({@code LibraryPage},
 * {@code activity_library_page.xml}, rows {@code library_item.xml}).
 *
 * <p>Reached from a TV project as Tab → "Episode No : N" ({@code LocationListingPage}) →
 * the location row. The toolbar reads "Location"; each entry shows an info block
 * ({@code scene_no}: "Location Name: …", "Scene No: …", "Link: …", "City: …",
 * "Uploaded by …"), an optional {@code descText} ("Description: …") and the {@code imgMore}
 * menu ({@code R.menu.common_lcw_menu}, trimmed per status in {@code LibraryPage}).
 *
 * <p>Extends {@link LocationToolPage} for the shared snackbar / popup / dialog / form helpers.
 */
public class LocationEntriesPage extends LocationToolPage {

    public LocationEntriesPage(AndroidDriver driver) {
        super(driver);
    }

    /** Entries are on screen: a recScene with library_item rows and no tab strip. */
    public boolean isEntriesPage(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                if (driver.findElements(byId("tabLayout")).isEmpty()
                        && (!driver.findElements(byId("imgMore")).isEmpty()
                        || !driver.findElements(byId("layMainCard")).isEmpty())) return true;
            } catch (Exception ignored) { }
            sleep(500);
        }
        return false;
    }

    /** The info block of every entry on screen. */
    public List<String> entryTexts() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']"
                            + "//*[@resource-id='" + appPackage() + ":id/scene_no']"))) {
                String t = e.getText();
                if (t != null) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public int entryCount() {
        try { return driver.findElements(byId("imgMore")).size(); }
        catch (Exception e) { return 0; }
    }

    /** "Description: …" lines on screen. */
    public List<String> descriptions() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("descText"))) {
                String t = e.getText();
                if (t != null) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Opens the menu of the entry at {@code index} (0-based); returns its items, menu left open. */
    public List<String> openEntryMenu(int index) {
        try {
            List<WebElement> more = driver.findElements(byId("imgMore"));
            if (more.size() <= index) return new ArrayList<>();
            more.get(index).click();
            sleep(1500);
        } catch (Exception e) { return new ArrayList<>(); }
        return popupItems();
    }

    /**
     * Opens the menu of the entry whose info block contains {@code part}, scrolling the list
     * until that card's ⋮ is on screen; returns the items, menu left open.
     */
    public List<String> openEntryMenuFor(String part) {
        String xp = "//*[@resource-id='" + appPackage() + ":id/layMainCard'][.//*[contains(@text,'" + part + "')]]"
                + "//*[@resource-id='" + appPackage() + ":id/imgMore']";
        for (int i = 0; i < 4; i++) {
            try {
                List<WebElement> more = driver.findElements(AppiumBy.xpath(xp));
                if (!more.isEmpty()) {
                    more.get(0).click();
                    sleep(1500);
                    return popupItems();
                }
            } catch (Exception ignored) { }
            swipeList(i < 2);
        }
        return new ArrayList<>();
    }

    /** Entry (found by text) menu → item. */
    public boolean entryActionFor(String part, String item) {
        List<String> items = openEntryMenuFor(part);
        if (items.isEmpty()) return false;
        if (tapPopupItem(item)) return true;
        back();
        return false;
    }

    private void swipeList(boolean towardsEnd) {
        try {
            org.openqa.selenium.Dimension d = driver.manage().window().getSize();
            int x = d.width / 2;
            int from = (int) (d.height * (towardsEnd ? 0.75 : 0.35));
            int to = (int) (d.height * (towardsEnd ? 0.35 : 0.75));
            driver.executeScript("mobile: dragGesture", java.util.Map.of(
                    "startX", x, "startY", from, "endX", x, "endY", to, "speed", 2500));
            sleep(800);
        } catch (Exception ignored) { }
    }

    /** Entry menu → item. */
    public boolean entryAction(int index, String item) {
        List<String> items = openEntryMenu(index);
        if (items.isEmpty()) return false;
        if (tapPopupItem(item)) return true;
        back();
        return false;
    }

    /** Toolbar "Select" ({@code selectTv}) → multi-select bar. */
    public boolean enterSelectMode() {
        if (!click("selectTv")) return false;
        sleep(1200);
        return isVisible("deleteMoveRl", Duration.ofSeconds(4));
    }

    public boolean selectionBarShown(Duration t) { return isVisible("deleteMoveRl", t); }

    public String buttonText(String id) {
        try {
            List<WebElement> b = driver.findElements(byId(id));
            if (b.isEmpty() || !b.get(0).isDisplayed()) return null;
            return b.get(0).getText();
        } catch (Exception e) { return null; }
    }

    public boolean click(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            sleep(1200);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Waits until the number of entries equals {@code n}. */
    public boolean waitEntryCount(int n, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (entryCount() == n) return true;
            sleep(1500);
        }
        return entryCount() == n;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
