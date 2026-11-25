package com.zillit.automation.pages.map;

import com.zillit.automation.pages.common.BasePage;
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

    // ---------------- web-gap helpers: chips, whole list, per-row reads ----------------
    //
    // LocationListFragment.setupFilterChips(): chip "All (N)" first, then one chip per
    // type reading "<icon> <type> (<count>)", blank types bucketed as "Unknown". Since the
    // Location tool was enabled the list also carries the city's Shortlisted/Final pins
    // (LocationListViewModel.locations = own + pins), and those rows have no Edit/Delete —
    // so pairing names with buttons by index (tapRowAction) breaks as soon as one overlay
    // row is on screen. The row methods below pair by geometry instead.

    /** Every chip label in {@code chipGroupFilter}, left to right as laid out. */
    public List<String> chipTexts() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/chipGroupFilter']/*"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * The chips sit in a HorizontalScrollView, and a chip scrolled off the right edge is
     * not in the tree. Swipes the strip left (well inside the screen, never from an edge,
     * which Android reads as BACK) until {@code type}'s chip is on screen.
     */
    public boolean revealChip(String type) {
        if (chipFor(type) != null) return true;
        for (int i = 0; i < 6; i++) {
            if (!swipeChipStrip(true)) return false;
            if (chipFor(type) != null) return true;
        }
        for (int i = 0; i < 6 && chipFor(type) == null; i++) swipeChipStrip(false);
        return chipFor(type) != null;
    }

    /** Every chip across the whole strip, then the strip is scrolled back to the start. */
    public List<String> allChipTexts() {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>(chipTexts());
        int swipes = 0;
        for (int i = 0; i < 6; i++) {
            if (!swipeChipStrip(true)) break;
            swipes++;
            int before = seen.size();
            seen.addAll(chipTexts());
            if (seen.size() == before) break;
        }
        for (int i = 0; i < swipes + 1; i++) swipeChipStrip(false);
        return new ArrayList<>(seen);
    }

    private boolean swipeChipStrip(boolean towardsEnd) {
        try {
            List<WebElement> g = driver.findElements(byId("chipGroupFilter"));
            if (g.isEmpty()) return false;
            org.openqa.selenium.Rectangle r = g.get(0).getRect();
            int w = driver.manage().window().getSize().getWidth();
            int y = r.getY() + r.getHeight() / 2;
            int a = (int) (w * 0.78), b = (int) (w * 0.22);
            org.openqa.selenium.interactions.PointerInput f =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence s =
                    new org.openqa.selenium.interactions.Sequence(f, 1);
            int from = towardsEnd ? a : b, to = towardsEnd ? b : a;
            s.addAction(f.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), from, y));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(450),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), to, y));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
            pauseMs(700);
            return true;
        } catch (Exception e) { return false; }
    }

    /** The chip for {@code type} ("… type (n)"), or null. "All" matches "All (n)". */
    public String chipFor(String type) {
        String q = java.util.regex.Pattern.quote(type);
        for (String c : chipTexts()) {
            if ("All".equals(type)) {
                if (c.startsWith("All (")) return c;
            } else if (c.matches("(?s)(.*\\s)?" + q + " \\(\\d+\\)$")) {
                return c;
            }
        }
        return null;
    }

    /** Count printed on the chip for {@code type}, or null when there is no such chip. */
    public Integer chipCount(String type) {
        if (!revealChip(type)) return null;
        String c = chipFor(type);
        if (c == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)\\)\\s*$").matcher(c);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    public boolean tapChip(String type) {
        if (!revealChip(type)) return false;
        String c = chipFor(type);
        if (c == null) return false;
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/chipGroupFilter']/*[@text="
                            + xpathLiteral(c) + "]")).click();
            pauseMs(1500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** N from "N total" in the header, or -1. */
    public int totalCount() {
        String t = countText();
        if (t == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(t);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Every row name, following the list past the bottom of the screen. */
    public List<String> allNames() {
        return readWholeList("rvLocations", "tvName", this::names, s -> s);
    }

    /**
     * Every row as "name|coords", following the list past the fold. Unlike {@link #allNames()}
     * two rows that share a NAME stay two entries — the merge key includes the coordinates.
     */
    public List<String> allRowKeys() {
        return readWholeList("rvLocations", "tvName", this::rowKeysOnScreen, s -> s);
    }

    private List<String> rowKeysOnScreen() {
        List<String> out = new ArrayList<>();
        for (WebElement n : elementsById("tvName")) {
            try {
                String name = n.getText() == null ? "" : n.getText().trim();
                String c = textIn(bandOfAnchor("tvName", n), "tvCoords");
                out.add(name + "|" + c);
            } catch (Exception ignored) { }
        }
        return out;
    }

    /** Scrolls until a row whose name reads exactly {@code name} is on screen. */
    public boolean revealRow(String name) {
        return scrollToRow("rvLocations", "tvName", name);
    }

    /** The row band for the row named exactly {@code name}, scrolling to it; null if absent. */
    private Band bandOf(String name) {
        if (!revealRow(name)) return null;
        for (WebElement e : elementsById("tvName")) {
            try {
                if (name.equals(e.getText() == null ? null : e.getText().trim())) {
                    return bandOfAnchor("tvName", e);
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    /**
     * Text of {@code childId} on the row named exactly {@code name}: "" when the row is
     * there but the child is hidden (GONE views are not in the tree), null when the row
     * itself is not in the list.
     */
    public String rowChildText(String name, String childId) {
        if (!ABOVE_NAME.contains(childId)) {
            Band b = bandOf(name);
            if (b == null) return null;
            String t = textIn(b, childId);
            return t == null ? "" : t;
        }
        // item_location_card.xml puts tvTypeBadge and tvScene ABOVE tvName, so the
        // name-anchored band holds the NEXT card's badge. Take the nearest one above the
        // name and below the previous card's name instead.
        if (!revealRow(name)) return null;
        List<WebElement> names = elementsById("tvName");
        for (int i = 0; i < names.size(); i++) {
            try {
                if (!name.equals(names.get(i).getText() == null ? null : names.get(i).getText().trim())) continue;
                int y = names.get(i).getRect().getY();
                int prev = i > 0 ? names.get(i - 1).getRect().getY() : Integer.MIN_VALUE;
                WebElement best = null; int bestY = Integer.MIN_VALUE;
                for (WebElement c : elementsById(childId)) {
                    int cy = c.getRect().getY();
                    if (cy < y && cy > prev && cy > bestY) { best = c; bestY = cy; }
                }
                return best == null ? "" : String.valueOf(best.getText()).trim();
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static final java.util.Set<String> ABOVE_NAME = java.util.Set.of("tvTypeBadge", "tvScene");

    /** Taps a row action, pairing the button to the row by position, not by index. */
    public boolean tapRowActionByBand(String name, String buttonId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            Band b = bandOf(name);
            if (b == null) return false;
            WebElement btn = elementIn(b, buttonId);
            if (btn != null) {
                try { btn.click(); pauseMs(1200); return true; } catch (Exception e) { return false; }
            }
            // The name is on screen but its button row is below the fold (last card).
            scrollList("rvLocations", true);
        }
        return false;
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private void pauseMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
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
