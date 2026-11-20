package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Production Diary / Box Schedule — the module's main screen.
 *
 * <p>Two views (Calendar and List), each with its own sub-modes, plus the right-hand
 * drawer that holds every create and export action.
 *
 * <p>Ids read from `new_box_activity_box_schedule.xml`.
 */
public class BoxSchedulePage extends BasePage {

    public BoxSchedulePage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- the screen itself ----------------

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (present("viewToggle") || present("fragmentContainer")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /** "Prepared: …" — the header line naming who prepared the schedule. */
    public String preparedText() { return text("tvPrepared"); }

    public boolean legendShown() { return displayed("legendContainer"); }

    // ---------------- views ----------------

    public boolean openCalendarView() { return clickIfPresent("btnCalendarView"); }

    public boolean openListView() { return clickIfPresent("btnListView"); }

    public boolean calendarModeShown() {
        return present("btnMonth") || present("btnWeek") || present("modeToggle");
    }

    public boolean listModeShown() {
        return present("btnByDate") || present("btnBySchedule") || present("listModeToggle");
    }

    // The Month / Week / Day buttons live in the CALENDAR FRAGMENT's layout
    // (new_box_fragment_calendar.xml: modeToggle, btnMonth, btnWeek, btnDay), not in the
    // activity — so they only exist while the Calendar view is showing, and they sit in a
    // toggle group that can run off the edge on a narrow screen. BS-03 failed on "Could
    // not select the day view" for exactly that: btnDay was there and not reachable.
    public boolean chooseMonth() { return clickMode("btnMonth", "Month"); }

    public boolean chooseWeek()  { return clickMode("btnWeek", "Week"); }

    public boolean chooseDay()   { return clickMode("btnDay", "Day"); }

    /**
     * Taps a calendar mode by id, falling back to its label.
     *
     * <p>A MaterialButtonToggleGroup renders its buttons as plain Buttons whose id is
     * present but which may be partly off screen; the label is the more reliable handle
     * when that happens.
     */
    private boolean clickMode(String id, String label) {
        if (clickIfPresent(id)) return true;
        // Not reachable by id — try the visible label, then give the toggle a nudge.
        if (tapText(label)) return true;
        try {
            for (WebElement group : driver.findElements(byId("modeToggle"))) {
                for (WebElement child : group.findElements(
                        io.appium.java_client.AppiumBy.className("android.widget.Button"))) {
                    String t = child.getText();
                    if (t != null && t.trim().equalsIgnoreCase(label)) {
                        child.click();
                        pause(900);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean chooseByDate()     { return clickIfPresent("btnByDate"); }

    public boolean chooseBySchedule() { return clickIfPresent("btnBySchedule"); }

    /** Opens the "set as default" chooser. The options are plain text in a popup. */
    public boolean openSetDefault() { return clickIfPresent("btnSetDefaultView"); }

    // ---------------- drawer ----------------

    /**
     * Opens the right-hand drawer.
     *
     * <p>Two attempts with a settle between them. "The drawer did not open" was the single
     * biggest skip reason in the first full run — eight cases — and btnMenu is a plain
     * toolbar button, so the likeliest cause is a tap arriving while the toolbar is still
     * laying out rather than the button being missing.
     *
     * <p>Deliberately NOT falling back to an edge swipe: this is an END drawer, and a
     * swipe in from the right edge is the system BACK gesture, which would walk the app
     * out of the module — the exact trap the Map module took hours to find.
     */
    public boolean openDrawer() {
        if (displayed("drawerCreateSchedule")) return true;      // already open
        for (int attempt = 0; attempt < 2; attempt++) {
            if (clickIfPresent("btnMenu")
                    && waitFor("drawerCreateSchedule", Duration.ofSeconds(6))) {
                return true;
            }
            pause(1500);
        }
        return false;
    }

    /** What is on the toolbar — for saying why the drawer could not be opened. */
    public String toolbarState() {
        return "btnMenu=" + (displayed("btnMenu") ? "shown" : present("btnMenu") ? "hidden" : "absent")
                + " viewToggle=" + (displayed("viewToggle") ? "shown" : "no")
                + " fragment=" + (displayed("fragmentContainer") ? "shown" : "no");
    }

    public boolean closeDrawer() {
        if (!displayed("drawerCreateSchedule")) return true;
        if (!clickIfPresent("btnCloseDrawer")) return false;
        // Wait for the slide-out. BS-08b failed on "reports closed but its actions are
        // still on screen" — they were, for the length of the animation.
        for (int i = 0; i < 10; i++) {
            if (!displayed("drawerCreateSchedule")) return true;
            pause(400);
        }
        return !displayed("drawerCreateSchedule");
    }

    public boolean drawerCreateSchedule()  { return clickIfPresent("drawerCreateSchedule"); }
    public boolean drawerCreateEvent()     { return clickIfPresent("drawerCreateEvent"); }
    public boolean drawerCreateNote()      { return clickIfPresent("drawerCreateNote"); }
    public boolean drawerEditTypes()       { return clickIfPresent("drawerEditTypes"); }
    public boolean drawerPresets()         { return clickIfPresent("drawerPresets"); }
    public boolean drawerShare()           { return clickIfPresent("drawerShare"); }
    public boolean drawerPrint()           { return clickIfPresent("drawerPrint"); }
    public boolean drawerDistributeToDd()  { return clickIfPresent("drawerDistributeToDd"); }
    public boolean drawerHistory()         { return clickIfPresent("drawerHistory"); }

    /** Which drawer actions are actually on screen — for rights-gated cases. */
    public List<String> drawerActions() {
        List<String> found = new ArrayList<>();
        for (String id : List.of("drawerCreateSchedule", "drawerCreateEvent",
                "drawerCreateNote", "drawerEditTypes", "drawerPresets", "drawerShare",
                "drawerPrint", "drawerDistributeToDd", "drawerHistory")) {
            if (displayed(id)) found.add(id);
        }
        return found;
    }

    public String historyBadge() { return text("drawerHistoryBadge"); }

    // ---------------- helpers ----------------

    protected boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** On screen AND laid out — not merely somewhere in the hierarchy. */
    protected boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean waitFor(String id, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed(id)) return true;
            pause(300);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    protected String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    protected boolean clickIfPresent(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) { e.click(); pause(900); return true; }
            }
            return false;
        } catch (Exception e) { return false; }
    }

    /** Taps a plain label anywhere on screen — for popups built without ids. */
    public boolean tapText(String label) {
        try {
            List<WebElement> els = driver.findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + label + "\")"));
            for (WebElement e : els) {
                if (e.isDisplayed()) { e.click(); pause(900); return true; }
            }
            return false;
        } catch (Exception e) { return false; }
    }

    public boolean textShown(String label) {
        try {
            for (WebElement e : driver.findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + label + "\")"))) {
                if (e.isDisplayed()) return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    protected void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
