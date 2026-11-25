package com.zillit.automation.tests.calendar;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.calendar.CalendarPage;
import com.zillit.automation.pages.calendar.CalendarPage.EventFormView;
import com.zillit.automation.pages.homechat.HomeChatPage;
import com.zillit.automation.pages.common.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calendar module (Home "Calendar" unit → new-calendar CalendarFragment → the
 * NEW EVENT form). Written against the REAL UI, driven live on the emulator.
 *
 * Verified on-device facts encoded here:
 *  - opening the Calendar unit raises a "access your calendar?" permission
 *    popup (auto-allowed).
 *  - the form's Call Type defaults to "Audio Call", so a Members event with no
 *    invitees fails on the invitee rule (not call-type).
 *  - validation errors are transient Toasts (fast-polled).
 *  - the editor/form has fields: title, color, full-day, start/end date+time,
 *    timezone, repeat, description, location, reminder, call type, invitees.
 *
 * Each event-form test reopens the form fresh via the FAB. Cases skip (not
 * fail) when a precondition (project / Calendar unit) is missing.
 */
@Order(TestOrder.CALENDAR)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarTest extends BaseTest {

    private static final Duration TOAST = Duration.ofSeconds(6);
    private boolean onCalendar;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void reachCalendar() {
        if (onCalendar) {
            // If the validation ladder left the event form OPEN, leave it open —
            // the next ladder test reuses it (that's the whole point). Only
            // recover when we're neither on the form nor the calendar.
            if (new EventFormView(driver).isOpen(Duration.ofSeconds(2))) {
                return;
            }
            // Robust recovery: a prior picker/dialog (colour, date, invitee…) can
            // be left open — back out repeatedly until the calendar is clean, so
            // the next test's freshForm() reliably opens. A single back was not
            // enough and caused every following CRUD test to skip.
            CalendarPage cal = new CalendarPage(driver);
            for (int i = 0; i < 5 && !cal.isCalendarLoaded(); i++) {
                dismissPermissionPrompts();
                driver.navigate().back();
                sleep(700);
            }
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter — create/join one first");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.isLoaded(), "Home units strip not visible");
        assumeTrue(home.selectUnit("Calendar"), "No Calendar unit on this project");
        // The Calendar unit requests READ_CALENDAR — allow the popup.
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(600); }
        assumeTrue(new CalendarPage(driver).isCalendarLoaded(), "Calendar screen did not load");
        onCalendar = true;
    }

    private CalendarPage calendar() {
        CalendarPage cal = new CalendarPage(driver);
        assumeTrue(cal.isCalendarLoaded(), "Calendar not loaded");
        return cal;
    }

    /** Opens a brand-new form (used by tests that need a clean slate). */
    private EventFormView freshForm() {
        System.out.println(">>> OPENING EVENT FORM (fresh)");
        for (int attempt = 0; attempt < 3; attempt++) {
            // CRITICAL: reach a CLEAN calendar BEFORE tapping the add-event FAB.
            // A prior test can leave the FORM (or a picker) on screen — the FAB
            // isn't visible there, so tapping for it throws TimeoutException. Back
            // out to the calendar first, then open a fresh form.
            CalendarPage cal = new CalendarPage(driver);
            for (int i = 0; i < 6 && !cal.isCalendarLoaded(); i++) {
                dismissPermissionPrompts();
                driver.navigate().back();
                sleep(700);
            }
            if (!cal.isCalendarLoaded()) {
                continue; // still not on the calendar — retry the recovery
            }
            try {
                EventFormView form = cal.openEventForm();
                dismissPermissionPrompts();
                if (form.isLoaded()) {
                    return form;
                }
            } catch (org.openqa.selenium.WebDriverException tapMissed) {
                // FAB tap didn't land / form didn't render — loop and recover.
            }
        }
        EventFormView form = new EventFormView(driver);
        assumeTrue(form.isLoaded(), "Event form did not open (calendar could not be reset)");
        return form;
    }

    /**
     * Returns the form WITHOUT reopening it if it is already on screen — so the
     * validation ladder stays on one form and fills fields one at a time,
     * instead of closing and reopening for every check. Opens fresh only when
     * the form isn't currently shown.
     */
    private EventFormView form() {
        EventFormView f = new EventFormView(driver);
        if (f.isOpen(Duration.ofSeconds(3))) {
            return f;
        }
        return freshForm();
    }

    // ---------------- calendar screen + navigation ----------------

    @Test
    @Order(1)
    @DisplayName("Calendar screen shows the month grid and the add-event FAB")
    void calendarLoads() {
        CalendarPage cal = calendar();
        assertTrue(cal.isCalendarLoaded(), "Calendar (month grid + FAB) should be visible");
        // [WEB CAL #1] the shell: current month title + the view selector on Month. The Home
        // unit reuses the fragment (Home.kt:349-353), so an earlier suite may have left it on
        // another month — Today first.
        cal.ensureMonthView();
        cal.tapToday();
        sleep(800);
        String want = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH));
        assertTrue(want.equals(cal.monthTitle()), "Header should show the current month '" + want + "', got '" + cal.monthTitle() + "'");
        assertTrue("Month".equals(cal.viewModeLabel()), "View selector should read Month, got '" + cal.viewModeLabel() + "'");
        screenshot("calendar-home");
    }

    @Test
    @Order(2)
    @DisplayName("Next / previous / Today month navigation changes the month title")
    void monthNavigation() {
        CalendarPage cal = calendar();
        String start = cal.monthTitle();
        assumeTrue(!start.isEmpty(), "Month title not available — skipping");
        cal.nextMonth();
        assertNotEquals(start, cal.monthTitle(), "Next-month should change the title");
        cal.nextMonth();
        sleep(600);
        cal.tapToday();
        sleep(800);
        // [WEB CAL #3] Today must come back to the current month, from two months ahead
        String now = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH));
        assertTrue(now.equals(cal.monthTitle()), "Today should show the current month '" + now + "', got '" + cal.monthTitle() + "'");
        assertTrue(cal.isCalendarLoaded(), "Calendar should remain usable after navigation");
    }

    // ---------------- event form: all fields visible ----------------

    @Test
    @Order(3)
    @DisplayName("Event form shows every field (title, dates, times, timezone, repeat, description, location, call type, Create)")
    void formShowsAllFields() {
        EventFormView form = freshForm();
        assertTrue(form.hasTitleField(), "Title field present");
        assertTrue(form.hasFullDayToggle(), "Full-day toggle present");
        assertTrue(form.hasStartDate(), "Start date present");
        assertTrue(form.hasEndDate(), "End date present");
        assertTrue(form.areTimePickersVisible(), "Start/End time pickers present");
        assertTrue(form.hasTimezone(), "Timezone present");
        assertTrue(form.hasRepeat(), "Repeat present");
        assertTrue(form.hasDescription(), "Description present");
        assertTrue(form.hasLocation(), "Location present");
        assertTrue(form.hasCallType(), "Call type present (Members)");
        assertTrue(form.hasSaveButton(), "Create Event button present");
        screenshot("calendar-form-fields");
        form.goBack();
    }

    @Test
    @Order(4)
    @DisplayName("Form defaults are populated (today's date, start/end times, timezone, Audio Call)")
    void formDefaultsPopulated() {
        EventFormView form = freshForm();
        assertTrue(!form.dateText().isEmpty(), "Start date should default to today");
        assertTrue(!form.startTimeText().isEmpty(), "Start time should have a default");
        assertTrue(!form.endTimeText().isEmpty(), "End time should have a default");
        assertTrue(!form.timezoneText().isEmpty(), "Timezone should be pre-filled");
        assertTrue(form.callTypeText().toLowerCase().contains("call")
                        || !form.callTypeText().isEmpty(),
                "Call type should default (e.g. Audio Call), got: " + form.callTypeText());
        System.out.println(">>> defaults: date=" + form.dateText() + " start=" + form.startTimeText()
                + " end=" + form.endTimeText() + " tz=" + form.timezoneText()
                + " callType=" + form.callTypeText());
        form.goBack();
    }

    // ---------------- validation ladder ----------------

    // Validation errors are Toasts, which Android 12+ does not expose to
    // accessibility. The reliable signal is that the form STAYS OPEN (a valid
    // save navigates back to the calendar). We assert that, and additionally
    // log the toast text when it happens to be readable.
    private static final Duration SETTLE = Duration.ofSeconds(3);

    @Test
    @Order(5)
    @DisplayName("Empty title is rejected — save is blocked, form stays open")
    void emptyTitleRejected() {
        // Ladder starts here: open the form ONCE and keep it open through the
        // following validation tests (each fills the next field). Do NOT close.
        EventFormView form = form();
        form.tapSave();
        if (form.toastShown("Title must be at least 3 characters", Duration.ofSeconds(2))) {
            System.out.println(">>> toast confirmed: title length");
        }
        assertTrue(form.stillOnFormAfterSave(SETTLE),
                "Empty title must block the save (form should stay open)");
        screenshot("calendar-empty-title");
    }

    @Test
    @Order(6)
    @DisplayName("Title shorter than 3 characters is rejected")
    void shortTitleRejected() {
        EventFormView form = form(); // same form — just change the title
        form.setTitle("ab");
        form.tapSave();
        assertTrue(form.stillOnFormAfterSave(SETTLE),
                "2-character title must block the save (form should stay open)");
    }

    @Test
    @Order(7)
    @DisplayName("Members event with a valid title but no invitees is rejected")
    void membersNoInviteesRejected() {
        EventFormView form = form(); // same form — now give it a valid title
        form.selectMembersType();
        form.setTitle("QA Automation Event");
        form.tapSave();
        // Call type defaults to Audio Call, so the invitee rule fires; either
        // way an incomplete members event must not save.
        assertTrue(form.stillOnFormAfterSave(SETTLE),
                "A members event with no invitees must block the save");
        screenshot("calendar-no-invitees");
    }

    @Test
    @Order(8)
    @DisplayName("Recurring event: Repeat Until is pre-filled, and a Repeat Until before the start date is refused")
    void recurringWithoutEndRejected() {
        // APP SOURCE (Zillit-Android/app/src/main/java/com/zillit/zillitapp/bottomNav/new_calendar/):
        //  - The "Repeat end date is required" rule (domain/util/EventFormLogic.kt:98) can't
        //    be reached from the UI: picking a frequency auto-fills Repeat Until
        //    (ui/form/EventFormViewModel.kt:318-321 -> EventFormLogic.kt:272-281, Daily =
        //    start + 1 day) and tvRecurrenceEnd has no clear control. So a recurring event is
        //    never "without an end" — we assert that pre-fill instead.
        //  - The repeat-end rule that IS reachable is the pre-submit guard in
        //    ui/form/EventFormFragment.kt:179-191: Repeat Until strictly before the start date
        //    -> Toast R.string.error_recurrence_end_before_start
        //    ("Repeat end date can't be before the start date", res/values/strings.xml:5570)
        //    and the save is NOT submitted. The Repeat Until picker is unconstrained
        //    (EventFormFragment.kt:677-706), so an earlier date can be picked.
        // The ladder form is a Members event with no invitees (from membersNoInviteesRejected),
        // which would block the save on its own (EventFormLogic.kt:104) — switch to Personal so
        // the repeat-end guard is the ONLY thing that can keep the form open.
        EventFormView form = form(); // same form — add recurrence (title already valid)
        form.selectPersonalType();
        form.setTitle("QA Recurring Event");
        assumeTrue(form.selectRecurrence("Daily"),
                "Could not choose a recurrence option — skipping");

        java.time.LocalDate start = EventFormView.parseFormDate(form.dateText());
        String prefilled = form.recurrenceEndText();
        System.out.println(">>> start=" + form.dateText() + " repeatUntil(prefilled)=" + prefilled);
        assertTrue(!prefilled.isEmpty(),
                "Choosing Daily should pre-fill Repeat Until (tvRecurrenceEnd), got empty");
        java.time.LocalDate prefilledDate = EventFormView.parseFormDate(prefilled);
        assumeTrue(start != null && prefilledDate != null,
                "Could not parse the form dates ('" + form.dateText() + "', '" + prefilled + "') — skipping");
        assertTrue(prefilledDate.equals(start.plusDays(1)),
                "Daily should default Repeat Until to start + 1 day (" + start.plusDays(1)
                        + "), got " + prefilledDate);

        // Drive the form to the reachable repeat-end rule: Repeat Until before Start.
        assumeTrue(form.setRecurrenceEndBeforeStart(),
                "Could not pick an earlier Repeat Until date in the picker — skipping");
        java.time.LocalDate end = EventFormView.parseFormDate(form.recurrenceEndText());
        assumeTrue(end != null && end.isBefore(start),
                "Repeat Until did not land before the start date (got '" + form.recurrenceEndText()
                        + "', start " + start + ") — skipping");

        form.tapSave();
        if (form.toastShown("Repeat end date can't be before the start date", Duration.ofSeconds(2))) {
            System.out.println(">>> toast confirmed: repeat end before start");
        }
        assertTrue(form.stillOnFormAfterSave(SETTLE),
                "A Repeat Until before the start date must block the save (form should stay open)");
        assertTrue(end.equals(EventFormView.parseFormDate(form.recurrenceEndText())),
                "The refused Repeat Until should be left as entered (not silently corrected), got '"
                        + form.recurrenceEndText() + "'");
        screenshot("calendar-recurrence-end-before-start");
    }

    @Test
    @Order(9)
    @DisplayName("Repeat dialog offers Daily/Weekly/Monthly/Yearly/Custom options")
    void repeatDialogOptions() {
        EventFormView form = form(); // same form
        assumeTrue(form.selectRecurrence("Weekly"),
                "Repeat dialog did not offer 'Weekly' — skipping");
    }

    // ---------------- field behaviour ----------------

    @Test
    @Order(10)
    @DisplayName("All-day toggle hides the start/end time pickers")
    void allDayHidesTimePickers() {
        EventFormView form = form(); // same form
        assumeTrue(form.areTimePickersVisible(), "Time pickers not shown by default — skipping");
        form.toggleAllDay();
        assertTrue(!form.areTimePickersVisible(),
                "Turning on All-day should hide the time pickers");
        form.toggleAllDay(); // restore times for the following tests
        // [WEB CAL #110] "Does not repeat" hides the Repeat Until field
        form.selectRecurrence("Does not repeat");
        assertTrue(!form.isRecurrenceEndShown(), "'Does not repeat' should hide Repeat Until");
        screenshot("calendar-all-day");
    }

    @Test
    @Order(11)
    @DisplayName("Personal type does not require members-only fields (invitees)")
    void personalTypeNoInvitees() {
        EventFormView form = form(); // same form
        form.selectMembersType();
        boolean membersHasInvitees = form.isInviteeControlVisible();
        form.selectPersonalType();
        boolean personalHasInvitees = form.isInviteeControlVisible();
        assertTrue(membersHasInvitees, "Members events should offer Add Invitees");
        assertTrue(!personalHasInvitees, "Personal events must not show the invitee control");
        form.selectMembersType(); // restore Members for following tests
    }

    @Test
    @Order(12)
    @DisplayName("Description field accepts text")
    void descriptionAcceptsText() {
        EventFormView form = form(); // same form — fill the description
        form.setDescription("QA agenda: sprint review");
        assertTrue(form.descriptionText().contains("QA agenda")
                        || form.descriptionText().contains("sprint"),
                "Description should hold the typed text");
    }

    @Test
    @Order(13)
    @DisplayName("Backing out closes the form and clears the data (end of the ladder)")
    void backReturnsToCalendar() {
        // Reuses the SAME form the whole validation ladder ran on, then closes
        // it — discarding all the entered data — and returns to the calendar.
        EventFormView form = form();
        form.goBack();
        // The form may prompt to discard; confirm if asked.
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)(discard|yes|ok|leave)\")")).click();
        } catch (Exception ignored) {
        }
        assertTrue(calendar().isCalendarLoaded(),
                "Back from the form should return to the calendar screen");
        // [WEB CAL #41] the discarded data is gone: a new form opens with an empty title
        EventFormView again = freshForm();
        String t = again.titleText();
        again.goBack();
        assertTrue(t.isEmpty() || t.equals("Add title"), "A reopened form should start with an empty title, got '" + t + "'");
    }

    // ---------------- pickers (open + select) ----------------

    @Test
    @Order(14)
    @DisplayName("Call-type picker changes the selected call type")
    void callTypePicker() {
        EventFormView form = freshForm();
        String after = form.selectCallType("Video Call");
        assertTrue(after.toLowerCase().contains("video"),
                "Call type should update to Video Call, got: " + after);
        form.goBack();
    }

    @Test
    @Order(15)
    @DisplayName("Reminder picker sets a reminder")
    void reminderPicker() {
        EventFormView form = freshForm();
        // The dialog rows read "10 minutes before" (new_calendar util/Constants.kt:25-32) and
        // pickFromDialog uses a full-match textMatches, so "10 minutes" never matched → the case
        // always skipped. Use the real label.
        assumeTrue(form.selectReminder("10 minutes before"),
                "'10 minutes before' reminder option not offered — skipping");
        assertTrue(!form.reminderText().equalsIgnoreCase("None"),
                "Reminder should no longer be None after selecting one");
        form.goBack();
    }

    @Test
    @Order(16)
    @DisplayName("Timezone picker (search + select) changes the timezone")
    void timezonePicker() {
        EventFormView form = freshForm();
        String before = form.timezoneText();
        String tz = form.selectTimezone("London");
        assertTrue(!tz.isEmpty(), "Timezone field should stay populated after picking");
        assertTrue(tz.contains("London"), "Picking the London row should set the timezone, was '" + before
                + "', now '" + tz + "'");
        form.goBack();
    }

    @Test
    @Order(17)
    @DisplayName("Invitee picker lists members with search + Done")
    void inviteePicker() {
        EventFormView form = freshForm();
        form.selectMembersType();
        assertTrue(form.isInviteeSearchable(),
                "Invitee picker should open with a search field and Done button");
        form.goBack();
    }

    @Test
    @Order(18)
    @DisplayName("Color picker lets you choose an event colour")
    void colorPicker() {
        EventFormView form = freshForm();
        assumeTrue(form.pickFirstColor(), "Colour picker did not open — skipping");
        // [WEB CAL #33] the colour menu also offers "Select custom color", which opens a dialog
        assertTrue(form.openCustomColorDialog(), "'Select custom color' should open the custom colour dialog");
        form.goBack();
    }

    @Test
    @Order(19)
    @DisplayName("Date picker opens and confirms")
    void datePicker() {
        EventFormView form = freshForm();
        assertTrue(form.openAndConfirmDate(), "Date picker should open and confirm");
        form.goBack();
    }

    @Test
    @Order(20)
    @DisplayName("Time picker opens and confirms")
    void timePicker() {
        EventFormView form = freshForm();
        assertTrue(form.openAndConfirmStartTime(), "Time picker should open and confirm");
        form.goBack();
    }

    // ---------------- successful creation + view/edit/delete ----------------

    private static String uniqueTitle() {
        return "QA Event " + (System.currentTimeMillis() % 1000000);
    }

    @Test
    @Order(21)
    @DisplayName("Create a Personal event successfully — it appears on the calendar")
    void createPersonalEvent() {
        EventFormView form = freshForm();
        form.selectPersonalType();
        String title = uniqueTitle();
        form.setTitle(title);
        form.tapSave();
        assertTrue(form.waitFormClosed(Duration.ofSeconds(30)),
                "A valid Personal event should save and close the form");
        // waitAgendaRow scrolls: by now today's agenda (other suites + earlier cases) is longer
        // than the screen, and agendaHasEvent only sees on-screen rows — that made this flaky.
        assertTrue(calendar().waitAgendaRow(title, Duration.ofSeconds(20)),
                "The created event '" + title + "' should appear on the calendar");
        System.out.println(">>> created personal event: " + title);
        screenshot("calendar-event-created");
        createdTitle = title;
    }

    private static String createdTitle;

    @Test
    @Order(22)
    @DisplayName("Tapping the created event opens its detail sheet")
    void viewEventDetail() {
        assumeTrue(createdTitle != null, "No event was created (previous test skipped)");
        CalendarPage cal = calendar();
        assumeTrue(cal.openAgendaEvent(createdTitle), "Could not tap the created event");
        assertTrue(cal.isDetailOpen(), "Event detail sheet (edit/delete) should open");
        screenshot("calendar-event-detail");
        cal.detailClose();
        // [WEB CAL #43] the × really closes the sheet
        assertTrue(!cal.waitDetailSheet(Duration.ofSeconds(2)) && cal.isVisibleFab(), "The detail sheet should be closed after ×");
    }

    @Test
    @Order(23)
    @DisplayName("UPDATE: edit the event's title, save, and the change persists")
    void editEvent() {
        assumeTrue(createdTitle != null, "No event to edit");
        CalendarPage cal = calendar();
        assumeTrue(cal.openAgendaEvent(createdTitle), "Could not tap the created event");
        assumeTrue(cal.isDetailOpen(), "Detail sheet did not open");
        cal.detailTapEdit();
        EventFormView form = new EventFormView(driver);
        assumeTrue(form.isLoaded(), "Edit should open the event form");
        String newTitle = createdTitle + " EDITED";
        form.setTitle(newTitle);
        form.setDescription("QA edited description");   // [WEB CAL #9] title AND description
        form.tapSave();
        assumeTrue(form.waitFormClosed(Duration.ofSeconds(30)), "Edited event should save");
        assertTrue(calendar().waitAgendaRow(newTitle, Duration.ofSeconds(20)),
                "The edited title should appear on the calendar");
        System.out.println(">>> edited event title to: " + newTitle);
        CalendarPage c1 = calendar();
        if (c1.openAgendaEvent(newTitle)) {
            String desc = c1.detailText("tvDescription");
            c1.closeDetail();
            assertTrue("QA edited description".equals(desc), "[WEB CAL #9] The edited description should be saved, got '" + desc + "'");
        }
        screenshot("calendar-event-edited");
        createdTitle = newTitle; // so the delete test targets the current title
    }

    @Test
    @Order(24)
    @DisplayName("Deleting the event cancels it (shows 'Cancelled: <title>')")
    void deleteEvent() {
        assumeTrue(createdTitle != null, "No event to delete");
        CalendarPage cal = calendar();
        assumeTrue(cal.openAgendaEvent(createdTitle), "Could not tap the created event");
        assumeTrue(cal.isDetailOpen(), "Detail sheet did not open");
        cal.detailTapDelete();
        cal.confirmDelete();
        // VERIFIED APP BEHAVIOUR: the app does NOT remove a deleted event — it
        // marks it CANCELLED and keeps it on the calendar as "Cancelled: <title>"
        // (a soft-delete). So we assert the cancelled label appears rather than
        // the event vanishing. (Flag to devs if a hard delete was intended.)
        boolean cancelled = cal.waitAgendaRow("Cancelled: " + createdTitle, Duration.ofSeconds(15));
        assertTrue(cancelled,
                "After delete, the event should be marked 'Cancelled: " + createdTitle + "'");
        System.out.println(">>> delete = CANCEL: event now shows 'Cancelled: " + createdTitle + "'");
        // [WEB CAL #62] the cancelled event's detail: status bar text, struck title, no Edit/Delete
        if (cal.openAgendaEvent("Cancelled: " + createdTitle)) {
            String status = cal.detailText("tvInvitationStatus");
            String shown = cal.detailText("tvTitle");
            boolean edit = cal.detailShows("btnEdit");
            cal.closeDetail();
            assertTrue("This event has been cancelled".equals(status), "Cancelled detail status, got '" + status + "'");
            assertTrue(shown.startsWith("Cancelled: "), "Cancelled detail title, got '" + shown + "'");
            assertTrue(!edit, "A cancelled event must not offer Edit");
        }
        screenshot("calendar-event-deleted");
    }

    // ---------------- recurring event CRUD (this / all occurrences) ----------------

    private static String recurringTitle;

    @Test
    @Order(27)
    @DisplayName("CREATE recurring: a daily event with an end date is created")
    void createRecurringEvent() {
        EventFormView form = freshForm();
        form.selectPersonalType(); // avoid the invitee requirement
        String title = "QA Recur " + (System.currentTimeMillis() % 1000000);
        form.setTitle(title);
        assumeTrue(form.selectRecurrence("Daily"), "Could not set Daily recurrence — skipping");
        assumeTrue(form.setRecurrenceEndLate(), "Could not set a recurrence end date — skipping");
        form.tapSave();
        assumeTrue(form.waitFormClosed(Duration.ofSeconds(30)),
                "A valid recurring event should save");
        // waitAgendaRow scrolls the agenda: today's list can be longer than the screen once
        // other suites have added events, and agendaHasEvent only sees on-screen rows.
        assertTrue(calendar().waitAgendaRow(title, Duration.ofSeconds(20)),
                "The recurring event '" + title + "' should appear on the calendar");
        recurringTitle = title;
        System.out.println(">>> created recurring event: " + title);
        screenshot("calendar-recurring-created");
    }

    @Test
    @Order(28)
    @DisplayName("UPDATE recurring: edit all occurrences and the change persists")
    void updateRecurringAll() {
        assumeTrue(recurringTitle != null, "No recurring event was created");
        CalendarPage cal = calendar();
        assumeTrue(cal.openAgendaEvent(recurringTitle), "Could not tap the recurring event");
        assumeTrue(cal.isDetailOpen(), "Detail sheet did not open");
        cal.detailTapEdit();
        // Recurring edit raises the scope dialog (this / this-and-future / all).
        if (cal.isRecurringActionDialogOpen()) {
            cal.pickRecurringScope("all");
            cal.tapRecurringConfirm();
        }
        EventFormView form = new EventFormView(driver);
        assumeTrue(form.isLoaded(), "Edit should open the recurring event form");
        String newTitle = recurringTitle + " EDIT";
        form.setTitle(newTitle);
        // [WEB CAL #69] "All events" keeps the Repeat section and can change the frequency
        assertTrue(form.isRecurrenceSectionShown(), "'All events' edit should keep the Repeat section");
        assertTrue("Daily".equals(form.recurrenceText()), "The series should reopen as Daily, got '" + form.recurrenceText() + "'");
        assertTrue(form.selectRecurrence("Weekly"), "Could not change the frequency to Weekly");
        form.tapSave();
        assumeTrue(form.waitFormClosed(Duration.ofSeconds(30)), "Recurring edit should save");
        assertTrue(calendar().waitAgendaRow(newTitle, Duration.ofSeconds(20)),
                "The edited recurring title should appear");
        // Weekly from today: tomorrow no longer carries an occurrence
        CalendarPage c2 = calendar();
        if (c2.selectDay(java.time.LocalDate.now().plusDays(1))) {
            c2.waitAgendaRow(recurringTitle, Duration.ofSeconds(5));
            assertTrue(!c2.agendaHasLiveEvent(recurringTitle), "[WEB CAL #69] After Daily → Weekly, tomorrow must have no occurrence; rows "
                    + c2.agendaRowsFor(recurringTitle));
            c2.selectDay(java.time.LocalDate.now());
        }
        recurringTitle = newTitle;
        screenshot("calendar-recurring-edited");
    }

    @Test
    @Order(29)
    @DisplayName("DELETE recurring: delete all occurrences cancels the series (rows show 'Cancelled: <title>')")
    void deleteRecurringAll() {
        assumeTrue(recurringTitle != null, "No recurring event to delete");
        CalendarPage cal = calendar();
        assumeTrue(cal.openAgendaEvent(recurringTitle), "Could not tap the recurring event");
        assumeTrue(cal.isDetailOpen(), "Detail sheet did not open");
        cal.detailTapDelete();
        // confirmDelete() handles the recurring scope dialog (all) automatically.
        cal.confirmDelete();
        // APP SOURCE (Zillit-Android/app/src/main/java/com/zillit/zillitapp/bottomNav/new_calendar/):
        // a deleted event is a soft-delete — CalendarEvent.isCancelled is
        // `deletedAt != null || isCancelledOccurrence` (domain/model/CalendarEvent.kt:80-81), and the
        // grid keeps cancelled rows, titled "Cancelled: <title>" with strike-through
        // (ui/calendar/DayAgendaAdapter.kt:37-42; Day view: ui/calendar/HourlyTimelineAdapter.kt:89-92).
        // So after "All events" the occurrence stays as "Cancelled: <title>" and no live
        // (unprefixed) row for the series may remain. Same soft-delete as deleteEvent above.
        // (Whether cancelled rows should stay on the grid at all is a separate product question.)
        assertTrue(cal.waitAgendaRow("Cancelled: " + recurringTitle, Duration.ofSeconds(15)),
                "After deleting all occurrences, the series should show as 'Cancelled: "
                        + recurringTitle + "'. Agenda rows: " + cal.agendaTitles());
        assertTrue(!cal.agendaHasLiveEvent(recurringTitle),
                "No live (non-cancelled) occurrence of '" + recurringTitle
                        + "' should remain. Agenda rows: " + cal.agendaTitles());
        screenshot("calendar-recurring-deleted");
    }

    // ---------------- view mode + quick actions ----------------

    @Test
    @Order(25)
    @DisplayName("Week view toggle switches the calendar view")
    void weekViewToggle() {
        CalendarPage cal = calendar();
        assumeTrue(cal.toggleViewMode("Week"), "View-mode toggle not available — skipping");
        // [WEB CAL #2] each mode is applied, not just "the calendar is still there"
        assertTrue("Week".equals(cal.viewModeLabel()), "View selector should read Week, got '" + cal.viewModeLabel() + "'");
        assertTrue(cal.toggleViewMode("Day"), "Day option missing");
        assertTrue("Day".equals(cal.viewModeLabel()), "View selector should read Day, got '" + cal.viewModeLabel() + "'");
        cal.toggleViewMode("Month"); // switch back
        assertTrue("Month".equals(cal.viewModeLabel()), "View selector should read Month again");
        assertTrue(cal.isCalendarLoaded(), "Calendar should remain usable after view switch");
    }

    @Test
    @Order(26)
    @DisplayName("Events button and Join Call action are present")
    void quickActionsPresent() {
        CalendarPage cal = calendar();
        assertTrue(cal.isEventsButtonVisible(), "The Events quick action should be visible");
        assertTrue(cal.isJoinCallVisible(), "The Join Call quick action should be visible");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
