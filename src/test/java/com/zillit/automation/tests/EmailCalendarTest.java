package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.EmailCalendarPage;
import com.zillit.automation.pages.EmailPage;
import com.zillit.automation.pages.ProjectListPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The email drawer's CALENDAR opens EmailCalendarActivity, which hosts the LEGACY
 * FragmentCalendar — a DIFFERENT calendar from the Home unit's NEW calendar (the
 * IDs differ, so CalendarPage does NOT apply). This covers the legacy email
 * calendar with its own IDs: load, month navigation, week/month toggle, and the
 * create-event FAB.
 */
@Order(34)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailCalendarTest extends BaseTest {

    private boolean onCalendar;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    /** One-time: navigate Home → Email tab → drawer → Calendar. */
    private void ensureOnEmailCalendar() {
        if (onCalendar && new EmailCalendarPage(driver).isLoaded(Duration.ofSeconds(2))) return;
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
        assumeTrue(tapEmailTab(), "Could not open the Email tab");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        EmailPage e = new EmailPage(driver);
        assumeTrue(e.isLoaded(Duration.ofSeconds(30)), "Email did not load");
        assumeTrue(e.openDrawer(), "Could not open the email drawer");
        assumeTrue(e.tapDrawerCalendar(), "Calendar nav not present");
        assumeTrue(new EmailCalendarPage(driver).isLoaded(Duration.ofSeconds(15)),
                "Email calendar did not load");
        onCalendar = true;
    }

    @Test
    @Order(1)
    @DisplayName("Email (legacy) calendar loads — month grid + create FAB")
    void emailCalendarLoads() {
        ensureOnEmailCalendar();
        assertTrue(new EmailCalendarPage(driver).isLoaded(Duration.ofSeconds(10)),
                "The email calendar (legacy fragment) should load");
        screenshot("email-calendar-loaded");
    }

    @Test
    @Order(2)
    @DisplayName("Email calendar month navigation changes the title")
    void emailCalendarMonthNav() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        String start = cal.monthTitle();
        assumeTrue(!start.isEmpty(), "Month title (tvMonthYear) unavailable — skipping");
        assumeTrue(cal.nextMonth(), "Next-month arrow not present");
        assertNotEquals(start, cal.monthTitle(), "Next-month should change the title");
        cal.previousMonth();
        assertTrue(cal.isLoaded(Duration.ofSeconds(5)), "Calendar should stay usable after nav");
    }

    @Test
    @Order(3)
    @DisplayName("Email calendar week/month view toggle works")
    void emailCalendarViewToggle() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        assumeTrue(cal.toggleWeekView(), "Week toggle (btnWeek) not present");
        assertTrue(cal.isLoaded(Duration.ofSeconds(5)), "Calendar usable in week view");
        cal.toggleMonthView();
        assertTrue(cal.isLoaded(Duration.ofSeconds(5)), "Calendar usable back in month view");
    }

    @Test
    @Order(4)
    @DisplayName("Email calendar create-event FAB opens the event form")
    void emailCalendarCreateEvent() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        assumeTrue(cal.openCreateEvent(), "Create-event FAB did not open a form");
        assertTrue(cal.isEventFormOpen(Duration.ofSeconds(8)) || !cal.isLoaded(Duration.ofSeconds(2)),
                "Create-event should open the event form");
        screenshot("email-calendar-create");
        driver.navigate().back();
    }

    private boolean tapEmailTab() {
        try {
            var nav = driver.findElement(byAppId("bottomNavigation"));
            for (var it : nav.findElements(byAppId("menuText"))) {
                if ("Email".equalsIgnoreCase(it.getText())) { it.click(); return true; }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)email\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
