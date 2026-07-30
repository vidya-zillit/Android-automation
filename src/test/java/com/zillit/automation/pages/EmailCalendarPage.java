package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * The EMAIL drawer Calendar (EmailCalendarActivity → the LEGACY FragmentCalendar).
 * NOTE: this is a DIFFERENT calendar from the Home unit's NEW calendar — the IDs
 * do not match CalendarPage. Legacy IDs: calendarView, tvMonthYear,
 * ivPreviousMonth / ivNextMonth, btnMonth / btnWeek (view toggle), and the
 * create-event FAB cluster fabMenuHolder → createEventHolder.
 */
public class EmailCalendarPage extends BasePage {

    public EmailCalendarPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("calendarView", timeout)
                || isVisible("tvMonthYear", Duration.ofSeconds(3))
                || isVisible("fabMenuHolder", Duration.ofSeconds(3));
    }

    public String monthTitle() {
        try {
            return driver.findElement(byId("tvMonthYear")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean nextMonth() {
        return tap("ivNextMonth");
    }

    public boolean previousMonth() {
        return tap("ivPreviousMonth");
    }

    public boolean toggleWeekView() {
        return tap("btnWeek");
    }

    public boolean toggleMonthView() {
        return tap("btnMonth");
    }

    /** Opens the create-event FAB cluster and taps "create event". */
    public boolean openCreateEvent() {
        // The FAB (fabMenuHolder) expands to reveal createEventHolder.
        tap("fabMenuHolder");
        sleep(700);
        if (tap("createEventHolder")) {
            sleep(900);
            return true;
        }
        if (isEventFormOpen(Duration.ofSeconds(3))) return true;
        // Fallback: a speed-dial FAB sits bottom-right; tap there, then the topmost
        // revealed action.
        try {
            var size = driver.manage().window().getSize();
            tapAt((int) (size.getWidth() * 0.9), (int) (size.getHeight() * 0.88));
            sleep(700);
            if (tap("createEventHolder") || textShown("Create", Duration.ofSeconds(2))) {
                tap("createEventHolder");
                sleep(800);
                return true;
            }
        } catch (WebDriverException ignored) {
        }
        return isEventFormOpen(Duration.ofSeconds(3)) || !isLoaded(Duration.ofSeconds(1));
    }

    /** A create/edit event form/screen opened (a title field, Save, or we left the grid). */
    public boolean isEventFormOpen(Duration timeout) {
        return isVisible("etTitle", timeout)
                || isVisible("etEventName", Duration.ofSeconds(1))
                || isVisible("etEventTitle", Duration.ofSeconds(1))
                || textShown("Create Event", Duration.ofSeconds(1))
                || textShown("Add Event", Duration.ofSeconds(1))
                || textShown("Event Title", Duration.ofSeconds(1));
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    private boolean tap(String id) {
        try {
            waitVisible(id).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
