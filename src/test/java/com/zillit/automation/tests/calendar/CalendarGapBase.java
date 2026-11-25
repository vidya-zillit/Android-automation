package com.zillit.automation.tests.calendar;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.calendar.CalendarPage;
import com.zillit.automation.pages.calendar.CalendarPage.EventFormView;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared plumbing for the Calendar web-gap suites (cases from the web E2E report that the
 * Android suite lacked — see {@code web-e2e/gaps/calendar.md}, ids "WEB CAL #N").
 *
 * <p>Reaches Home → Calendar once per class, creates events on a chosen day through the
 * Month grid + FAB (the FAB pre-fills the selected day, CalendarFragment.kt:323-336), and
 * cancels every event it created in an {@link AfterAll} sweep (delete = soft delete: the
 * rows stay as "Cancelled: &lt;title&gt;", DayAgendaAdapter.kt:37-42).
 *
 * <p>All data is prefixed {@code ZL_AUTO}.
 */
abstract class CalendarGapBase extends BaseTest {

    protected static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    protected static final LocalDate TODAY = LocalDate.now();

    /** (title, day) pairs created by this class — cancelled in the sweep. */
    protected static final List<Object[]> CREATED = new ArrayList<>();

    private boolean onCalendar;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachCalendar() {
        if (onCalendar) {
            recoverToCalendar();
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter — create/join one first");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "Could not enter a project");
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.isLoaded(), "Home units strip not visible");
        assumeTrue(home.selectUnit("Calendar"), "No Calendar unit on this project");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(600); }
        assumeTrue(new CalendarPage(driver).isCalendarLoaded(), "Calendar screen did not load");
        onCalendar = true;
    }

    /** Backs out of forms, sheets, dialogs and list screens until the calendar FAB shows. */
    protected void recoverToCalendar() {
        CalendarPage cal = new CalendarPage(driver);
        for (int i = 0; i < 6 && !cal.isVisibleFab(); i++) {
            dismissPermissionPrompts();
            // Never back out of the app: on Home → Calendar one BACK too many leaves Zillit.
            if (!appInForeground() || !driver.findElements(byAppId("bottomNavigation")).isEmpty()) {
                reenterCalendar();
                continue;
            }
            driver.navigate().back();
            sleep(700);
        }
    }

    private boolean appInForeground() {
        try {
            return appPackage.equals(driver.getCurrentPackage());
        } catch (Exception e) {
            return true;
        }
    }

    /** Foregrounds the app and opens Home → Calendar (used when recovery left the calendar). */
    private void reenterCalendar() {
        try {
            if (!appInForeground()) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(1500);
            }
            BottomNavPage nav = new BottomNavPage(driver);
            if (nav.waitUntilLoaded(Duration.ofSeconds(10))) {
                nav.goHome();
                HomeChatPage home = new HomeChatPage(driver);
                if (home.isLoaded()) home.selectUnit("Calendar");
                for (int i = 0; i < 2; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
            } else {
                driver.navigate().back();
                sleep(700);
            }
        } catch (Exception ignored) {
        }
    }

    protected CalendarPage calendar() {
        CalendarPage cal = new CalendarPage(driver);
        if (!cal.isVisibleFab()) recoverToCalendar();   // a sheet/dialog left on top
        assumeTrue(cal.isCalendarLoaded(), "Calendar not loaded");
        return cal;
    }

    protected static String title(String tag) {
        return "ZL_AUTO Cal " + tag + " " + RUN;
    }

    /** Selects the day on the grid and opens a fresh form pre-filled with it. */
    protected EventFormView formOn(LocalDate day) {
        CalendarPage cal = calendar();
        assumeTrue(cal.selectDay(day), "Could not select " + day + " on the Month grid");
        EventFormView form = cal.openEventForm();
        dismissPermissionPrompts();
        assumeTrue(form.isLoaded(), "Event form did not open");
        LocalDate shown = EventFormView.parseFormDate(form.dateText());
        assumeTrue(day.equals(shown), "The FAB did not pre-fill " + day + " (form shows '" + form.dateText() + "')");
        return form;
    }

    /**
     * Creates a Personal event on {@code day}. {@code start}/{@code end} may be null (form
     * defaults). {@code extra} runs on the form before Save. Returns true when it saved and
     * the agenda for that day lists it. Registers it for the cleanup sweep.
     */
    protected boolean createPersonal(String title, LocalDate day, LocalTime start, LocalTime end,
                                     Consumer<EventFormView> extra) {
        EventFormView form = formOn(day);
        form.selectPersonalType();
        form.setTitle(title);
        if (start != null) assumeTrue(form.setStartTime(start), "Could not set the start time " + start);
        if (end != null) assumeTrue(form.setEndTime(end), "Could not set the end time " + end);
        if (extra != null) extra.accept(form);
        form.tapSave();
        boolean closed = form.waitFormClosed(Duration.ofSeconds(30));
        if (!closed) return false;
        String shown = title.trim();
        CREATED.add(new Object[]{shown, day});
        CalendarPage cal = calendar();
        cal.selectDay(day);
        if (!cal.waitAgendaRow(shown, Duration.ofSeconds(15))) {
            cal.pullToRefresh();
            return cal.waitAgendaRow(shown, Duration.ofSeconds(15));
        }
        return true;
    }

    /** Opens the event (by title text) on the given day; true when the detail sheet is up. */
    protected boolean openEvent(LocalDate day, String titleText) {
        CalendarPage cal = calendar();
        assumeTrue(cal.selectDay(day), "Could not select " + day);
        if (cal.openAgendaEvent(titleText)) return true;
        cal.pullToRefresh();
        return cal.openAgendaEvent(titleText);
    }

    /** Agenda titles for that day containing the text. */
    protected List<String> rowsOn(LocalDate day, String text) {
        CalendarPage cal = calendar();
        cal.selectDay(day);
        cal.waitAgendaRow(text, Duration.ofSeconds(8));
        return cal.agendaRowsFor(text);
    }

    /** A live (not "Cancelled:"/"Declined:") row whose title equals {@code exact}. */
    protected static boolean hasLive(List<String> rows, String exact) {
        return rows.stream().anyMatch(r -> r.equals(exact));
    }

    protected static boolean hasCancelled(List<String> rows, String exact) {
        return rows.stream().anyMatch(r -> r.equals("Cancelled: " + exact));
    }

    /** Cancels one event (all occurrences when recurring). Best effort, used by the sweep. */
    protected void cancelEvent(LocalDate day, String titleText) {
        try {
            recoverToCalendar();
            CalendarPage cal = new CalendarPage(driver);
            if (!cal.selectDay(day)) return;
            if (!cal.waitAgendaRow(titleText, Duration.ofSeconds(6))) return;
            if (!cal.agendaHasLiveEvent(titleText)) return;
            if (!cal.openAgendaEvent(titleText)) return;
            if (!cal.detailShows("btnDelete")) { cal.closeDetail(); return; }
            cal.detailTapDelete();
            if (cal.isRecurringActionDialogOpen()) {
                cal.pickRecurringScope("all");
                cal.tapRecurringConfirm();
            } else {
                cal.confirmDelete();
            }
            sleep(2500);
        } catch (Exception e) {
            System.out.println(">>> sweep: could not cancel '" + titleText + "' on " + day + ": " + e.getMessage());
        }
    }

    /** Cancels everything this class created (runs before BaseTest closes the session). */
    @AfterAll
    void sweepCreatedEvents() {
        if (driver == null || CREATED.isEmpty()) return;
        sweep();
    }

    protected void sweep() {
        List<Object[]> todo = new ArrayList<>(CREATED);
        CREATED.clear();
        for (Object[] c : todo) {
            cancelEvent((LocalDate) c[1], (String) c[0]);
        }
    }

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
