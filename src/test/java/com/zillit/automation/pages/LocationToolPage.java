package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools &rarr; <b>Location</b> ({@code LocationPage}) — the tool that FEEDS the Map overlay.
 *
 * <p>Three tabs, in this order, each a {@code LocationFolderPage} over a different
 * identifier:
 * <pre>
 *   Select     SELECTED_IDENTIFIER     — never reaches the Map
 *   Shortlist  SHORTLISTED_IDENTIFIER  — drawn as "Shortlisted Shoot Location"
 *   Final      PUBLISH_IDENTIFIER      — drawn as "Finalized Shoot Location"
 * </pre>
 *
 * <p>The tab LABELS and the Map's tags are not the same words, which is a standing trap:
 * the tab reads "Shortlist" ({@code R.string.shortlisted}) while the Map's tag is
 * "Shortlisted", and the tab reads "Final" while the wire status may be any of
 * {@code published} / {@code final} / {@code finalized}.
 *
 * <p>This tool is not enabled on every project. Callers should treat an absent tile as a
 * skip, not a failure — {@code Tools.kt} only dispatches {@code LOCATION_TOOL} when the
 * project has it switched on.
 */
public class LocationToolPage extends BasePage {

    public static final String TAB_SELECT = "Select";
    public static final String TAB_SHORTLIST = "Shortlist";
    public static final String TAB_FINAL = "Final";

    /** Tab labels in the order {@code LocationPage} attaches them. */
    public static final List<String> TABS_IN_ORDER =
            List.of(TAB_SELECT, TAB_SHORTLIST, TAB_FINAL);

    public LocationToolPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("tabLayout", timeout) && isVisible("viewPager", Duration.ofSeconds(3));
    }

    /** Tab titles as rendered, left to right. */
    public List<String> tabLabels() {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> tabs = driver.findElements(
                    AppiumBy.xpath("//*[@resource-id='" + appPackage() + ":id/tabLayout']"
                            + "//android.widget.TextView"));
            for (WebElement t : tabs) {
                String s = t.getText();
                if (s != null && !s.isBlank()) out.add(s.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean selectTab(String label) {
        try {
            WebElement tab = driver.findElement(
                    AppiumBy.xpath("//*[@resource-id='" + appPackage() + ":id/tabLayout']"
                            + "//android.widget.TextView[@text='" + label + "']"));
            tab.click();
            try { Thread.sleep(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean filterIconShown() {
        try { return !driver.findElements(byId("filterIv")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    public boolean openFilter() {
        try {
            List<WebElement> els = driver.findElements(byId("filterIv"));
            if (els.isEmpty()) return false;
            els.get(0).click();
            try { Thread.sleep(1200); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Folder / scene rows inside the current tab.
     *
     * <p>Rows are {@code location_folder_item.xml}, whose name view is {@code tvName};
     * the list itself is {@code recScene} on {@code fragment_location_folder_page.xml}.
     * Counting is scoped to that list — {@code tvName} is one of the most reused ids in
     * the app, so an unscoped count would pick up whatever else is on screen.
     */
    public int folderCount() {
        try {
            return driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName']")).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Folder / scene names in the current tab. */
    public List<String> folderNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/recScene']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** True once the multi-select action bar is up (Move To / Cancel / Delete). */
    public boolean selectionBarShown(Duration timeout) {
        return isVisible("deleteMoveRl", timeout) || isVisible("btnMoveTo", timeout);
    }

    /** The tool's own empty state for a tab with nothing filed under it. */
    public boolean emptyStateShown(Duration timeout) {
        return isVisible("emptyView", timeout);
    }

    public boolean listingShown(Duration timeout) {
        return isVisible("recScene", timeout) || isVisible("search", timeout);
    }
}
