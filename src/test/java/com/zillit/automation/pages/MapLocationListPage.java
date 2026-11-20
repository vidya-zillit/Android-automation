package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Map &rarr; <b>View All</b> ({@code locationListFragment}, {@code fragment_location_list}).
 *
 * <p>The flat list behind the carousel. It renders the same {@code PinnedLocation} objects,
 * so the Location-tool overlay appears here too and obeys the same read-only rule —
 * {@code LocationCardAdapter} gates Edit/Delete on
 * {@code postingAccess && !isFromLocationTool}, exactly as the carousel does.
 *
 * <p>Card ids differ from the carousel's: the type label is {@code tvTypeBadge} here,
 * {@code tvType} there. Getting that wrong reads an empty string and quietly passes a
 * label assertion, so they are kept in separate page objects on purpose.
 */
public class MapLocationListPage extends BasePage {

    public MapLocationListPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("rvLocations", timeout) || isVisible("tvTitle", timeout);
    }

    public String title() {
        return text("tvTitle");
    }

    public String countText() {
        return text("tvCount");
    }

    public boolean backToMap() {
        return clickIfPresent("btnBackToMap");
    }

    public boolean addLocationShown() {
        return present("btnAddLocation");
    }

    public boolean filterChipsShown(Duration timeout) {
        return isVisible("chipGroupFilter", timeout);
    }

    public boolean emptyStateShown(Duration timeout) {
        return isVisible("emptyState", timeout) || isVisible("tvEmptyTitle", timeout);
    }

    public String emptyTitle() {
        return text("tvEmptyTitle");
    }

    /** Row names, scoped to {@code rvLocations} so nothing else on screen is counted. */
    public List<String> names() {
        return textsIn("rvLocations", "tvName");
    }

    /** Type badges on the visible cards — overlay rows carry the spelled-out label. */
    public List<String> typeBadges() {
        return textsOf("tvTypeBadge");
    }

    /**
     * Counts the Edit buttons on screen.
     *
     * <p>Compared against the row count, this is how a test proves the read-only rule
     * without having to identify which specific row is an overlay pin: an overlay row
     * contributes a name but no Edit button.
     */
    public int editButtonCount() {
        return countOf("btnEdit");
    }

    public int deleteButtonCount() {
        return countOf("btnDelete");
    }

    public int viewButtonCount() {
        return countOf("btnView");
    }

    public int rowCount() {
        return names().size();
    }

    /**
     * Taps a per-row action on the first row whose name contains {@code nameContains}.
     *
     * <p>Rows are paired by INDEX rather than by walking up from the name to its card.
     * The ancestor axis is unreliable on this UiAutomator build — it has returned the
     * RecyclerView itself, whose first matching descendant is row 0's button, so a test
     * meaning to edit its own fixture quietly edited someone else's row. Index pairing
     * only holds while a row that has a name also has the button, which is exactly the
     * case this guards: a row without the action contributes no button, the lists come
     * out different lengths, and the method says so by returning false.
     */
    public boolean tapRowAction(String nameContains, String buttonId) {
        try {
            List<WebElement> names = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvLocations']"
                            + "//*[@resource-id='" + appPackage() + ":id/tvName']"));
            List<WebElement> buttons = driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/rvLocations']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']"));
            if (names.size() != buttons.size()) return false;
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i).getText();
                if (name != null && name.contains(nameContains)) {
                    buttons.get(i).click();
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean editLocation(String nameContains) {
        return tapRowAction(nameContains, "btnEdit");
    }

    public boolean viewLocation(String nameContains) {
        return tapRowAction(nameContains, "btnView");
    }

    public boolean deleteLocation(String nameContains) {
        return tapRowAction(nameContains, "btnDelete");
    }

    /** Texts of {@code childId} found INSIDE {@code containerId}. */
    private List<String> textsIn(String containerId, String childId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(io.appium.java_client.AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + containerId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + childId + "']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ---------------- helpers ----------------

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private int countOf(String id) {
        try { return driver.findElements(byId(id)).size(); }
        catch (Exception e) { return 0; }
    }

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private List<String> textsOf(String id) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    private boolean clickIfPresent(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            try { Thread.sleep(900); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return true;
        } catch (Exception e) { return false; }
    }
}
