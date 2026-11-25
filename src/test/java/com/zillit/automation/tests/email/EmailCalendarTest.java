package com.zillit.automation.tests.email;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.calendar.CalendarPage.EventFormView;
import com.zillit.automation.pages.email.EmailCalendarPage;
import com.zillit.automation.pages.email.EmailPage;
import com.zillit.automation.pages.common.ProjectListPage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The email drawer's CALENDAR opens EmailCalendarActivity
 * (Zillit-Android new_email/ui/folders/FolderDrawerFragment.kt:87-89), which hosts
 * the NEW calendar's CalendarFragment — the same screen as the Home unit
 * (new_email/ui/calendar/EmailCalendarActivity.kt:15-38). So the ids are the new
 * calendar's (fab, monthGrid, tvMonthTitle, btnPrevMonth/btnNextMonth,
 * btnViewMode/tvViewMode) and EmailCalendarPage reuses CalendarPage. Covers:
 * load, month navigation, Month/Week view toggle, and the add-event FAB.
 */
@Order(TestOrder.EMAIL_CALENDAR)
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
    @DisplayName("Email calendar loads — the new calendar (month grid + add-event FAB)")
    void emailCalendarLoads() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        assertTrue(cal.isCalendarLoaded(),
                "The email calendar (new CalendarFragment: fab / monthGrid) should load");
        assertTrue(!cal.monthTitle().isEmpty(), "The month title (tvMonthTitle) should be shown");
        screenshot("email-calendar-loaded");
    }

    @Test
    @Order(2)
    @DisplayName("Email calendar month navigation changes the title")
    void emailCalendarMonthNav() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        String start = cal.monthTitle();
        assumeTrue(!start.isEmpty(), "Month title (tvMonthTitle) unavailable — skipping");
        cal.nextMonth();
        assertNotEquals(start, cal.monthTitle(), "Next-month (btnNextMonth) should change the title");
        cal.previousMonth();
        assertEquals(start, cal.monthTitle(), "Previous-month (btnPrevMonth) should restore the title");
        assertTrue(cal.isLoaded(Duration.ofSeconds(5)), "Calendar should stay usable after nav");
    }

    @Test
    @Order(3)
    @DisplayName("Email calendar Month/Week view toggle works")
    void emailCalendarViewToggle() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        // btnViewMode opens a Month/Week/Day popup; tvViewMode shows the current mode
        // (new_calendar/ui/calendar/CalendarFragment.kt:172-193).
        assumeTrue(cal.toggleViewMode("Week"), "View-mode selector (btnViewMode) not present");
        assertEquals("Week", cal.viewModeLabel(), "View-mode label should read Week");
        assertTrue(cal.isLoaded(Duration.ofSeconds(5)), "Calendar usable in week view");
        assumeTrue(cal.toggleViewMode("Month"), "Could not switch back to Month");
        assertEquals("Month", cal.viewModeLabel(), "View-mode label should read Month");
        assertTrue(cal.isLoaded(Duration.ofSeconds(5)), "Calendar usable back in month view");
    }

    @Test
    @Order(4)
    @DisplayName("Email calendar add-event FAB opens the event form")
    void emailCalendarCreateEvent() {
        ensureOnEmailCalendar();
        EmailCalendarPage cal = new EmailCalendarPage(driver);
        assumeTrue(cal.isCalendarLoaded(), "Add-event FAB (fab) not present");
        // The FAB starts EventFormActivity (new_calendar/ui/calendar/CalendarFragment.kt:323-336).
        EventFormView form = cal.openEventForm();
        dismissPermissionPrompts();
        assertTrue(form.isLoaded(), "The add-event FAB should open the event form (etTitle + btnSave)");
        screenshot("email-calendar-create");
        form.goBack();
    }

    @Test
    @Order(5)
    @DisplayName("[WEB CAL #132] An event created in Email → Calendar appears on Home → Calendar")
    void emailCalendarEventOnHomeCalendar() {
        ensureOnEmailCalendar();
        com.zillit.automation.pages.calendar.CalendarPage cal = new com.zillit.automation.pages.calendar.CalendarPage(driver);
        java.time.LocalDate d = java.time.LocalDate.now().plusDays(8);
        String t = "ZL_AUTO EmailCal " + (System.currentTimeMillis() % 10000);
        assumeTrue(cal.selectDay(d), "Could not select " + d);
        EventFormView form = cal.openEventForm();
        dismissPermissionPrompts();
        assumeTrue(form.isLoaded(), "Event form did not open");
        form.selectPersonalType();
        form.setTitle(t);
        form.tapSave();
        assertTrue(form.waitFormClosed(Duration.ofSeconds(30)), "The event did not save from the Email calendar");
        // leave Email → Home → Calendar unit
        for (int i = 0; i < 4 && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
            driver.navigate().back();
            sleep(700);
        }
        onCalendar = false;
        BottomNavPage nav = new BottomNavPage(driver);
        assumeTrue(nav.waitUntilLoaded(Duration.ofSeconds(20)) && nav.goHome(), "Could not reach Home");
        com.zillit.automation.pages.homechat.HomeChatPage home = new com.zillit.automation.pages.homechat.HomeChatPage(driver);
        assumeTrue(home.isLoaded() && home.selectUnit("Calendar"), "No Calendar unit on Home");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        assumeTrue(cal.isCalendarLoaded(), "Home Calendar did not load");
        assertTrue(cal.selectDay(d), "Could not select " + d + " on Home Calendar");
        boolean shown = cal.waitAgendaRow(t, Duration.ofSeconds(20));
        // clean up: cancel it
        if (shown && cal.openAgendaEvent(t)) {
            cal.detailTapDelete();
            cal.confirmDelete();
            sleep(2000);
        }
        assertTrue(shown, "The Email-calendar event '" + t + "' should be on Home → Calendar for " + d
                + " (same CalendarFragment, EmailCalendarActivity.kt:35)");
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
