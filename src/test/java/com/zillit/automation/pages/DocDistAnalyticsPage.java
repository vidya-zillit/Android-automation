package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCUMENT DISTRIBUTION — the ANALYTICS screen ({@code historyAnalyticsFragment} /
 * fragment_dd_analytics.xml): a range toggle, three KPI tiles, an MPAndroidChart
 * timeline and three top-N cards.
 *
 * <p><b>⚠ On the current source this screen has no way IN.</b> ZL-19532 removed the
 * Analytics button from History ("Per QA, analytics now live INSIDE each individual
 * distribution detail"), {@code menu/dd_history_list.xml} still declares
 * {@code dd_action_analytics} but is never inflated, and nothing anywhere calls
 * {@code navigate(R.id.historyAnalyticsFragment)} — the destination sits in dd_nav_graph
 * orphaned. The fragment itself is complete and unchanged, so this page object is written
 * against it in full: an older APK that still has the entry point exercises every method,
 * and a current one skips the area with a precise reason instead of a guess.
 *
 * <p><b>Every KPI id is an {@code <include>} id, and the values collide</b> (trap 3.16
 * again, three times over). {@code dd_kpi_sent} / {@code dd_kpi_opened} /
 * {@code dd_kpi_rate} are the include ids on three copies of view_dd_kpi_tile — at
 * runtime each is the CARD, and all three cards contain a {@code dd_kpi_label} and a
 * {@code dd_kpi_value} with the SAME id. Reading {@code dd_kpi_value} directly returns
 * whichever tile the tree happened to serialise first. Every read here is XPath-anchored
 * on the include id. The three top-N cards ({@code dd_top_recipients} /
 * {@code dd_top_folders} / {@code dd_top_lists} over view_dd_top_list) have exactly the
 * same shape with {@code dd_top_title} / {@code dd_top_container}.
 *
 * <p><b>Render order matters when reading state.</b> {@code render()} returns early when
 * {@code state.data == null}, so on a slow first load the KPI values are still the
 * layout's blanks and NEITHER {@code dd_chart} nor {@code dd_chart_empty} has been
 * touched — the chart is visible by default. {@link #waitLoaded} waits for a KPI value to
 * be non-blank rather than for the spinner alone.
 *
 * <p><b>{@code dd_unavailable} is the graceful 404 path</b>: any analytics failure flips
 * it on and hides {@code dd_scroll} wholesale. That is a valid outcome, not a bug — an
 * older deployment simply has no analytics endpoint.
 */
public class DocDistAnalyticsPage extends BasePage {

    /** The header the fragment sets on the shared common_toolbar (R.string.dd_analytics). */
    public static final String HEADER = "Analytics";

    /** KPI include ids, in render order. */
    public static final String KPI_SENT   = "dd_kpi_sent";
    public static final String KPI_OPENED = "dd_kpi_opened";
    public static final String KPI_RATE   = "dd_kpi_rate";

    /** Top-N include ids. */
    public static final String TOP_RECIPIENTS = "dd_top_recipients";
    public static final String TOP_FOLDERS    = "dd_top_folders";
    public static final String TOP_LISTS      = "dd_top_lists";

    public DocDistAnalyticsPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- identity ----------------

    /**
     * True on the analytics screen. {@code dd_kpi_sent} is the card itself, so it exists
     * as soon as the layout inflates — but the "unavailable" path hides the whole scroll
     * view, so that state is accepted as identity too.
     */
    public boolean isOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed(KPI_SENT) || displayed("dd_unavailable")) return true;
            if (HEADER.equalsIgnoreCase(textOf("headerText").trim())
                    && displayed("dd_range")) {
                return true;
            }
            sleep(400);
        }
        return false;
    }

    /**
     * Waits for the first response: either data landed (a KPI value is filled in) or the
     * endpoint answered "unavailable". Returns false if it never settled.
     */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isUnavailable()) return true;
            if (!displayed("dd_progress") && !kpiValue(KPI_SENT).isBlank()) return true;
            sleep(500);
        }
        return false;
    }

    public boolean isUnavailable()  { return displayed("dd_unavailable"); }
    public String unavailableText() { return textOf("dd_unavailable"); }
    public boolean contentShown()   { return displayed("dd_scroll"); }

    // ---------------- range toggle ----------------

    public boolean hasRangeToggle() { return displayed("dd_range"); }

    public boolean selectRange7()      { return click("dd_range_7"); }
    public boolean selectRange30()     { return click("dd_range_30"); }
    public boolean selectRange90()     { return click("dd_range_90"); }
    public boolean selectRangeCustom() { return click("dd_range_custom"); }

    /**
     * Whether a range button is the checked one.
     *
     * <p>MaterialButton inside a MaterialButtonToggleGroup reports its state through the
     * "checked" attribute; some driver / widget combinations only expose "selected". Both
     * are consulted, and an unreadable button answers false rather than throwing — the
     * caller should report the value, not hang a whole case on it.
     */
    public boolean isRangeChecked(String id) {
        try {
            WebElement b = driver.findElement(byId(id));
            return "true".equalsIgnoreCase(String.valueOf(b.getAttribute("checked")))
                    || "true".equalsIgnoreCase(String.valueOf(b.getAttribute("selected")));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * The MaterialDatePicker RANGE dialog that Custom opens (title = "Custom", from
     * {@code setTitleText(R.string.dd_range_custom)}).
     */
    public boolean datePickerShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (anyResourceIdMatching(".*:id/(mtrl_picker_title_text|mtrl_picker_header_"
                    + "selection_text|confirm_button|cancel_button)")) {
                return true;
            }
            sleep(400);
        }
        return false;
    }

    /** Dismisses the date picker without applying a range. */
    public boolean dismissDatePicker() {
        try {
            List<WebElement> cancel = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/cancel_button\")"));
            if (!cancel.isEmpty()) { cancel.get(0).click(); sleep(400); return true; }
        } catch (WebDriverException ignored) { }
        try { driver.navigate().back(); sleep(400); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- KPI tiles ----------------

    /** True when a KPI tile's card is in the tree (the include id IS the card). */
    public boolean hasKpiTile(String includeId) { return present(includeId); }

    /** The label of one tile — "Sent" / "Opened" / "Open rate", set in onViewCreated. */
    public String kpiLabel(String includeId) { return inside(includeId, "dd_kpi_label"); }

    /** The value of one tile. Blank until the first response lands. */
    public String kpiValue(String includeId) { return inside(includeId, "dd_kpi_value"); }

    /**
     * A KPI as a number, or {@link Double#NaN} when it is not one — a tile can legitimately
     * read "—" or "N/A" before data arrives, and a case comparing rates must skip on that
     * rather than fail.
     *
     * <p>The rate is formatted {@code "%.2f%%"} with the DEVICE locale, so the percent sign
     * is stripped and a comma decimal separator is normalised. Sent/Opened are plain
     * {@code Int.toString()} with no grouping, so nothing else needs undoing.
     */
    public double kpiNumber(String includeId) {
        String raw = kpiValue(includeId).trim().replace("%", "").replace(" ", "").trim();
        if (raw.isEmpty()) return Double.NaN;
        if (raw.indexOf(',') >= 0 && raw.indexOf('.') < 0) raw = raw.replace(',', '.');
        try { return Double.parseDouble(raw); }
        catch (NumberFormatException e) { return Double.NaN; }
    }

    // ---------------- chart ----------------

    public boolean chartShown()      { return displayed("dd_chart"); }
    public boolean chartEmptyShown() { return displayed("dd_chart_empty"); }
    public String chartEmptyText()   { return textOf("dd_chart_empty"); }

    // ---------------- top-N cards ----------------

    public boolean hasTopCard(String includeId) { return present(includeId); }

    public String topTitle(String includeId) { return inside(includeId, "dd_top_title"); }

    /**
     * Rows in one top-N card. They are built in code with no ids, so they are counted as
     * the direct children of {@code dd_top_container} — zero is a legitimate answer for a
     * project with no sends in the window.
     */
    public int topRowCount(String includeId) {
        try {
            return driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + includeId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_top_container']/*")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public List<String> topRowTexts(String includeId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + includeId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_top_container']"
                            + "//android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Scrolls the NestedScrollView so a top-N card is on screen before reading it. */
    public void scrollTo(String includeId) { scrollToId(includeId); }

    // ---------------- helpers ----------------

    /** A child id read from INSIDE one include — the only safe way here (trap 3.16). */
    private String inside(String includeId, String childId) {
        try {
            return driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + includeId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + childId + "']")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private boolean anyResourceIdMatching(String regex) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\"" + regex + "\")")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(600); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
