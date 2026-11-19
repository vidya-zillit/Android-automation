package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin Settings, as REGROUPED on {@code vidya_AI/email_tutorial_bugs}.
 *
 * <p>The flat, alphabetically-sorted list became six sections — Approvals, Permission &amp;
 * User Management, Creation, Tools, The project, Danger zone — with a header per section
 * and a cross-group search box. Rows still use {@code common_list_item.xml} and
 * {@code commonListItemHolder}/{@code nameTv} untouched, which is why the existing row
 * taps keep working; headers are a NEW view type with their own ids.
 *
 * <p>Two ids matter more than they look:
 * <ul>
 *   <li>{@code searchEt} is now on this screen too, and it was previously enough on its
 *       own to identify the Tools screen — see {@code ToolsPage.isLoaded()}.</li>
 *   <li>{@code groupTitleTv} is a header, NOT a row. Anything matching list entries by
 *       bare text can now land on a section title instead of the row it wanted, and
 *       "Permission &amp; User Management" contains the row label "User Management".</li>
 * </ul>
 */
public class AdminSettingsPage extends BasePage {

    /** Section titles in their on-screen order (enum declaration order in the app). */
    public static final List<String> GROUPS_IN_ORDER = List.of(
            "Approvals",
            "Permission & User Management",
            "Creation",
            "Tools",
            "The project",
            "Danger zone");

    public AdminSettingsPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        return new BottomNavPage(driver).selectTab("Settings");
    }

    /**
     * Opens the Admin Settings row on the Settings tab.
     *
     * <p>Scoped to {@code nameTv} for the same reason the admin rows are: "ADMIN SETTINGS"
     * also appears as the screen's own header once you are there, and {@code textContains}
     * is case-insensitive in this framework.
     */
    public boolean openAdminSettings() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceIdMatches(\".*nameTv\")"
                            + ".textContains(\"Admin Settings\"))"));
        } catch (WebDriverException ignored) {
            // Short list or already visible.
        }
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[contains(@resource-id,'nameTv') and contains(@text,'Admin Settings')]"
                            + "/ancestor::*[contains(@resource-id,'commonListItemHolder')]")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOpen(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    // ---------------- sections ----------------

    /** Section titles currently rendered, top to bottom. */
    public List<String> groupTitles() {
        return textsOf("groupTitleTv");
    }

    /** The count badge on each header, in the same order as {@link #groupTitles()}. */
    public List<String> groupCounts() {
        return textsOf("groupCountTv");
    }

    /** Row labels currently rendered — headers excluded, because they use another id. */
    public List<String> rowLabels() {
        return textsOf("nameTv");
    }

    /**
     * Scrolls the list to the bottom, collecting every section title and row label on the
     * way. The list is longer than one screen, so a single read only ever sees a slice.
     */
    public Sweep sweep() {
        Sweep sweep = new Sweep();
        String lastSeen = "";
        for (int i = 0; i < 12; i++) {
            sweep.groups.addAll(groupTitles());
            sweep.rows.addAll(rowLabels());
            List<String> rows = rowLabels();
            String marker = rows.isEmpty() ? "" : rows.get(rows.size() - 1);
            if (marker.equals(lastSeen)) break; // bottom reached — nothing new appeared
            lastSeen = marker;
            scrollListDown();
        }
        return sweep;
    }

    /** Ordered, de-duplicated view of one full scroll through the screen. */
    public static final class Sweep {
        public final List<String> groups = new DistinctList();
        public final List<String> rows = new DistinctList();
    }

    private static final class DistinctList extends ArrayList<String> {
        @Override
        public boolean add(String value) {
            if (value == null || value.isBlank() || contains(value)) return false;
            return super.add(value);
        }
    }

    private void scrollListDown() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            pauseFor(500);
        } catch (WebDriverException ignored) {
            // Not scrollable / already at the end.
        }
    }

    // ---------------- search ----------------

    public boolean hasSearchBox() {
        return isVisible("searchEt", Duration.ofSeconds(5));
    }

    /**
     * Types a query into the cross-group search.
     *
     * <p>No hideKeyboard afterwards: UiAutomator2 implements it as a BACK press, and BACK
     * on this screen finishes the Activity — the assertions would then run against the
     * Settings tab and report something that has nothing to do with search.
     */
    public void search(String query) {
        WebElement box = waitVisible("searchEt");
        box.clear();
        if (!query.isEmpty()) box.sendKeys(query);
        pauseFor(900);
    }

    public void clearSearch() {
        waitVisible("searchEt").clear();
        pauseFor(900);
    }

    /** The "No settings match your search." label. */
    public boolean isNoMatchShown(Duration timeout) {
        return isVisible("noMatchTv", timeout);
    }

    // ---------------- internals ----------------

    private List<String> textsOf(String id) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                String text = e.getText();
                if (text != null && !text.isBlank()) out.add(text.trim());
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    private void pauseFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
