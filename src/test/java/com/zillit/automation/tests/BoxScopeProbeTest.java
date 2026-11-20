package com.zillit.automation.tests;

import com.zillit.automation.pages.BoxListPage;
import com.zillit.automation.pages.BoxSchedulePage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Diagnostic only — not in any suite, not in --box, not in --all.
 *
 * <p>Answers one question: why can "Edit complete schedule" and "Delete this date" not be
 * tapped, when "Edit this date" on the same dialog can? Three cases and the cleanup are
 * stuck on it, and the same label match works for one option and not the others — which
 * means guessing has already been tried. This dumps the dialog instead.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoxScopeProbeTest extends BoxBaseTest {

    @Test
    @DisplayName("PROBE the edit/delete scope prompt")
    void probe() {
        reachBoxSchedule();
        BoxSchedulePage b = new BoxSchedulePage(driver);
        assumeTrue(b.openListView(), "List view button not available");
        sleep(1500);
        b.chooseByDate();
        sleep(2500);

        BoxListPage list = new BoxListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(12)), "The list did not load");

        // Any day will do; a multi-day block is what raises the prompt.
        List<BoxListPage.DayRow> rows = list.dayRows();
        assumeTrue(!rows.isEmpty(), "The list showed no schedule days");
        String date = rows.get(0).date;
        System.out.println(">>> PROBE opening Edit on " + date + " (rows: " + rows + ")");
        assumeTrue(list.editDay(date), "Edit was not available on that row");
        sleep(3000);

        dumpEverything("EDIT scope prompt");

        // Does scrolling inside the dialog reveal more?
        scrollInDialog();
        dumpEverything("EDIT scope prompt AFTER a scroll");

        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);

        // And the delete side, which has the same trouble.
        BoxListPage again = new BoxListPage(driver);
        if (again.isLoaded(Duration.ofSeconds(8))) {
            List<BoxListPage.DayRow> r2 = again.dayRows();
            if (!r2.isEmpty() && again.deleteDay(r2.get(0).date)) {
                sleep(2500);
                dumpEverything("DELETE scope prompt");
                try { driver.navigate().back(); } catch (Exception ignored) { }
            }
        }
    }

    /** Every element on screen with anything readable about it. */
    private void dumpEverything(String label) {
        System.out.println(">>> PROBE ===== " + label + " =====");
        int n = 0;
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath("//*"))) {
                if (n++ > 90) { System.out.println(">>> PROBE   …truncated"); break; }
                try {
                    String t = e.getText();
                    String cls = e.getAttribute("class");
                    String rid = e.getAttribute("resource-id");
                    if ((t == null || t.isBlank()) && (rid == null || rid.isBlank())) continue;
                    System.out.println(">>> PROBE   <" + shortCls(cls) + "> id="
                            + tail(rid) + " text=" + q(t)
                            + " clickable=" + e.getAttribute("clickable")
                            + " enabled=" + e.getAttribute("enabled")
                            + " displayed=" + e.isDisplayed()
                            + " bounds=" + e.getAttribute("bounds"));
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            System.out.println(">>> PROBE   dump threw " + e.getClass().getSimpleName());
        }
    }

    private void scrollInDialog() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            pauseLocal(1200);
            System.out.println(">>> PROBE scrolled forward inside the dialog");
        } catch (Exception e) {
            System.out.println(">>> PROBE nothing scrollable in the dialog ("
                    + e.getClass().getSimpleName() + ")");
        }
    }

    private void pauseLocal(long ms) { sleep(ms); }

    private static String shortCls(String c) {
        if (c == null) return "?";
        int i = c.lastIndexOf('.');
        return i > 0 ? c.substring(i + 1) : c;
    }

    private static String tail(String rid) {
        if (rid == null || rid.isBlank()) return "-";
        int i = rid.lastIndexOf('/');
        return i > 0 ? rid.substring(i + 1) : rid;
    }

    private static String q(String s) { return s == null ? "null" : "\"" + s + "\""; }
}
