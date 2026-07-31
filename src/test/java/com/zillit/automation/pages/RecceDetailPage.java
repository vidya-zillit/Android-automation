package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * RECCE detail screen (RecceDetailFragment / fragment_recce_detail.xml) —
 * read-only render of a recce: status/title/unit/date, the info tiles
 * (rendezvous / station / weather / stops), the route map, the stop timeline
 * and the personnel list. Actions: Export (PDF), Print, Edit.
 *
 * NOTE there is deliberately NO delete and NO share here — delete lives only on
 * the list card, and Edit only shows with posting rights.
 */
public class RecceDetailPage extends BasePage {

    public RecceDetailPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("detailTitle", timeout)
                || isVisible("detailScroll", Duration.ofSeconds(3))
                || isVisible("detailStatus", Duration.ofSeconds(3));
    }

    public String title()  { return textOf("detailTitle"); }
    public String status() { return textOf("detailStatus"); }
    public String unit()   { return textOf("detailUnit"); }
    public String date()   { return textOf("detailDate"); }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    /** The four info tiles (rendezvous / station / weather / stops) rendered. */
    public int infoTileCount() {
        try { return driver.findElements(byId("tileValue")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    public boolean isRouteMapShown() {
        return isVisible("routeMap", Duration.ofSeconds(5))
                || isVisible("routeMapEmpty", Duration.ofSeconds(2));
    }

    /**
     * The stop count from the detail screen's own "Stops" info tile — the app's
     * number, so it is not affected by how many rows happen to be on screen and
     * it counts stops that have no place name.
     * Returns -1 if the tile can't be read.
     */
    public int stopsTileCount() {
        try {
            var labels = driver.findElements(byId("tileLabel"));
            var values = driver.findElements(byId("tileValue"));
            for (int i = 0; i < labels.size() && i < values.size(); i++) {
                String l = labels.get(i).getText();
                if (l != null && l.toLowerCase().contains("stop")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("(\\d+)").matcher(values.get(i).getText());
                    if (m.find()) return Integer.parseInt(m.group(1));
                }
            }
        } catch (WebDriverException ignored) { }
        return -1;
    }

    /** Stop rows rendered right now (only a few fit on screen). */
    public int timelineStopCount() {
        try { return driver.findElements(byId("stopPlace")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /**
     * Scrolls the detail screen and counts DISTINCT stops in the timeline by their
     * place text. Counting only rendered rows under-reports badly — just a few
     * stops fit on screen at once.
     */
    public int timelineStopCountScrolled() {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (int i = 0; i < 10; i++) swipe(0.35, 0.85);      // rewind to top
        int stable = 0;
        for (int i = 0; i < 25 && stable < 3; i++) {
            int before = seen.size();
            try {
                for (var e : driver.findElements(byId("stopPlace"))) {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) seen.add(t);
                }
            } catch (WebDriverException ignored) { }
            stable = (seen.size() == before) ? stable + 1 : 0;
            swipe(0.80, 0.30);
        }
        return seen.size();
    }

    /** All distinct stop place labels in the timeline (scrolled). */
    public java.util.List<String> timelineStopPlaces() {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (int i = 0; i < 10; i++) swipe(0.35, 0.85);
        int stable = 0;
        for (int i = 0; i < 25 && stable < 3; i++) {
            int before = seen.size();
            try {
                for (var e : driver.findElements(byId("stopPlace"))) {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) seen.add(t);
                }
            } catch (WebDriverException ignored) { }
            stable = (seen.size() == before) ? stable + 1 : 0;
            swipe(0.80, 0.30);
        }
        return new java.util.ArrayList<>(seen);
    }

    private void swipe(double from, double to) {
        var size = driver.manage().window().getSize();
        var f = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var s = new org.openqa.selenium.interactions.Sequence(f, 1);
        s.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * from)));
        s.addAction(f.createPointerDown(0));
        s.addAction(f.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.getWidth() / 2, (int) (size.getHeight() * to)));
        s.addAction(f.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(s));
        sleep(450);
    }

    public int personnelCount() {
        try { return driver.findElements(byId("personName")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    // ---------------- actions ----------------

    public boolean isEditVisible()   { return isVisible("detailEditBtn", Duration.ofSeconds(4)); }
    public boolean isExportVisible() { return isVisible("detailExportBtn", Duration.ofSeconds(3)); }
    public boolean isPrintVisible()  { return isVisible("detailPrintBtn", Duration.ofSeconds(3)); }

    public RecceFormPage tapEdit() {
        try { driver.findElement(byId("detailEditBtn")).click(); sleep(2500); }
        catch (WebDriverException ignored) { }
        return new RecceFormPage(driver);
    }

    public boolean tapExport() {
        try { driver.findElement(byId("detailExportBtn")).click(); sleep(3000); return true; }
        catch (WebDriverException e) { return false; }
    }

    public void back() {
        try { driver.findElement(byId("backBtn")).click(); }
        catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        sleep(1500);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
