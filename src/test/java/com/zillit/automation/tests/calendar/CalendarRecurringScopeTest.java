package com.zillit.automation.tests.calendar;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.calendar.CalendarPage;
import com.zillit.automation.pages.calendar.CalendarPage.EventFormView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calendar — recurring scope (web gap M2: web #66, #67, #68, #70, #71).
 *
 * <p>Edit / delete one occurrence ("This event"), this-and-following, and the Custom weekday
 * repeat. App source: RecurringActionDialog (rbSingle "This event", rbThisAndFuture "This and
 * following events (from &lt;date&gt;)", rbAll, btnOk "Continue", btnCancel); the edit form
 * hides the Repeat section for single / thisAndFuture (EventFormFragment.kt:479-484); a
 * recurring delete goes straight from the scope dialog to the server (no second confirm,
 * EventDetailBottomSheet.kt:127-137); cancelled occurrences stay as "Cancelled: &lt;title&gt;".
 *
 * <p>Two Personal Daily series, D+2 … D+5: A for the edit cases, B for the delete cases.
 * Everything is cancelled in the sweep. Run: {@code --only CalendarRecurringScopeTest}.
 * TestOrder: CALENDAR_RECURRING_SCOPE (200).
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.CALENDAR_RECURRING_SCOPE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarRecurringScopeTest extends CalendarGapBase {

    private static final LocalDate D2 = TODAY.plusDays(2), D3 = TODAY.plusDays(3),
            D4 = TODAY.plusDays(4), D5 = TODAY.plusDays(5);
    private static final String A = title("RecA");
    private static final String B = title("RecB");
    private static boolean seriesA, seriesB, splitA;

    /** Personal Daily series D+2 … D+5. */
    private boolean createSeries(String t) {
        return createPersonal(t, D2, null, null, form -> {
            assumeTrue(form.selectRecurrence("Daily"), "Repeat dialog did not offer Daily");
            assumeTrue(form.setRecurrenceEnd(D5), "Could not set Repeat Until to " + D5);
        });
    }

    // ───────────────────────────── edit scopes (series A) ─────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[WEB CAL #66] Edit 'This event' renames only that occurrence (Repeat section hidden)")
    void editSingleOccurrence() {
        seriesA = createSeries(A);
        assertTrue(seriesA, "The Daily series '" + A + "' did not save / show on " + D2);
        assertTrue(hasLive(rowsOn(D3, A), A), "Series occurrence missing on " + D3 + ": " + rowsOn(D3, A));

        CalendarPage cal = calendar();
        assertTrue(openEvent(D3, A), "Could not open the " + D3 + " occurrence");
        assertTrue(cal.detailShows("tvRecurringBadge"), "Detail of a series occurrence should show the Recurring badge");
        cal.detailTapEdit();
        assertTrue(cal.isRecurringActionDialogOpen(), "Edit on a recurring event should ask This / Following / All");
        cal.pickRecurringScope("single");
        cal.tapRecurringConfirm();
        EventFormView form = new EventFormView(driver);
        assertTrue(form.isLoaded(), "Edit form did not open");
        assertTrue(!form.isRecurrenceSectionShown(),
                "Editing 'This event' must hide the Repeat section (EventFormFragment.kt:479-484)");
        form.setTitle(A + " ONE");
        form.tapSave();
        assertTrue(form.waitFormClosed(Duration.ofSeconds(30)), "Single-occurrence edit did not save");

        List<String> d3 = rowsOn(D3, A);
        assertTrue(hasLive(d3, A + " ONE"), D3 + " should show '" + A + " ONE'; rows " + d3);
        List<String> d4 = rowsOn(D4, A);
        assertTrue(hasLive(d4, A), D4 + " should keep the original title; rows " + d4);
        assertTrue(!d4.contains(A + " ONE"), "The rename leaked to " + D4 + ": " + d4);
        List<String> d2 = rowsOn(D2, A);
        assertTrue(hasLive(d2, A), D2 + " should keep the original title; rows " + d2);
        screenshot("cal-rec-edit-single");
    }

    @Test
    @Order(2)
    @DisplayName("[WEB CAL #67] Edit 'This and following' from day 3 splits the series: earlier days keep the title")
    void editThisAndFollowing() {
        assumeTrue(seriesA, "Series A was not created");
        CalendarPage cal = calendar();
        assertTrue(openEvent(D4, A), "Could not open the " + D4 + " occurrence");
        cal.detailTapEdit();
        assertTrue(cal.isRecurringActionDialogOpen(), "Edit should ask the scope");
        String label = cal.scopeLabel("rbThisAndFuture");
        System.out.println(">>> this-and-following label: " + label);
        assertTrue(label.startsWith("This and following events"), "Scope label: '" + label + "'");
        cal.pickRecurringScope("future");
        cal.tapRecurringConfirm();
        EventFormView form = new EventFormView(driver);
        assertTrue(form.isLoaded(), "Edit form did not open");
        assertTrue(!form.isRecurrenceSectionShown(), "'This and following' must hide the Repeat section");
        form.setTitle(A + " LATER");
        form.tapSave();
        assertTrue(form.waitFormClosed(Duration.ofSeconds(30)), "This-and-following edit did not save");
        splitA = true;
        CREATED.add(new Object[]{A + " LATER", D4});

        List<String> d5 = rowsOn(D5, A);
        assertTrue(hasLive(d5, A + " LATER"), D5 + " should carry the new title; rows " + d5);
        List<String> d4 = rowsOn(D4, A);
        assertTrue(hasLive(d4, A + " LATER"), D4 + " should carry the new title; rows " + d4);
        List<String> d3 = rowsOn(D3, A);
        assertTrue(!d3.contains(A + " LATER"), "The split must not reach " + D3 + ": " + d3);
        List<String> d2 = rowsOn(D2, A);
        assertTrue(hasLive(d2, A), D2 + " should keep the original title; rows " + d2);
        screenshot("cal-rec-edit-following");
    }

    // ───────────────────────────── delete scopes (series B) ─────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[WEB CAL #70] Delete 'This event': Cancel deletes nothing; then only that day is cancelled")
    void deleteSingleOccurrence() {
        seriesB = createSeries(B);
        assertTrue(seriesB, "The Daily series '" + B + "' did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(D3, B), "Could not open the " + D3 + " occurrence");
        cal.detailTapDelete();
        assertTrue(cal.isRecurringActionDialogOpen(), "Delete on a recurring event should ask the scope");
        cal.tapRecurringCancel();
        cal.closeDetail();
        List<String> afterCancel = rowsOn(D3, B);
        assertTrue(hasLive(afterCancel, B) && !hasCancelled(afterCancel, B),
                "Cancel in the scope dialog must delete nothing; rows " + afterCancel);

        assertTrue(openEvent(D3, B), "Could not reopen the " + D3 + " occurrence");
        cal.detailTapDelete();
        assertTrue(cal.isRecurringActionDialogOpen(), "Scope dialog did not reopen");
        cal.pickRecurringScope("single");
        cal.tapRecurringConfirm();
        sleep(2500);
        List<String> d3 = rowsOn(D3, B);
        assertTrue(hasCancelled(d3, B) && !hasLive(d3, B), D3 + " should show only 'Cancelled: " + B + "'; rows " + d3);
        List<String> d4 = rowsOn(D4, B);
        assertTrue(hasLive(d4, B), D4 + " should still be live; rows " + d4);
        List<String> d2 = rowsOn(D2, B);
        assertTrue(hasLive(d2, B), D2 + " should still be live; rows " + d2);
        screenshot("cal-rec-delete-single");
    }

    @Test
    @Order(4)
    @DisplayName("[WEB CAL #71] Delete 'This and following' from day 4 cancels day 4 and 5, earlier days stay live")
    void deleteThisAndFollowing() {
        assumeTrue(seriesB, "Series B was not created");
        CalendarPage cal = calendar();
        assertTrue(openEvent(D4, B), "Could not open the " + D4 + " occurrence");
        cal.detailTapDelete();
        assertTrue(cal.isRecurringActionDialogOpen(), "Delete should ask the scope");
        cal.pickRecurringScope("future");
        cal.tapRecurringConfirm();
        sleep(2500);
        List<String> d4 = rowsOn(D4, B), d5 = rowsOn(D5, B), d2 = rowsOn(D2, B);
        assertTrue(!hasLive(d4, B), D4 + " should be cancelled; rows " + d4);
        assertTrue(!hasLive(d5, B), D5 + " should be cancelled; rows " + d5);
        assertTrue(hasLive(d2, B), D2 + " must stay live; rows " + d2);
        screenshot("cal-rec-delete-following");
    }

    // ───────────────────────────── custom weekdays ─────────────────────────────

    @Test
    @Order(5)
    @DisplayName("[WEB CAL #68] Custom repeat on Mon + Wed lands on Monday and Wednesday, not Tuesday")
    void customWeekdays() {
        String c = title("RecMW");
        LocalDate mon = TODAY.plusDays(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        boolean saved = createPersonal(c, mon, null, null, form -> {
            assumeTrue(form.selectRecurrence("Custom"), "Repeat dialog did not offer Custom");
            assertTrue(form.isChipGroupDaysShown(), "Custom should show the weekday chips (EventFormFragment.kt:488)");
            assumeTrue(form.tickDayChip("Mon") && form.tickDayChip("Wed"), "Could not tick Mon and Wed");
        });
        assertTrue(saved, "The Custom Mon/Wed series did not save / show on " + mon);
        List<String> tue = rowsOn(mon.plusDays(1), c);
        List<String> wed = rowsOn(mon.plusDays(2), c);
        assertTrue(hasLive(wed, c), "Wednesday " + mon.plusDays(2) + " should have the event; rows " + wed);
        assertTrue(tue.isEmpty(), "Tuesday " + mon.plusDays(1) + " must not have the event; rows " + tue);
        screenshot("cal-rec-custom-mon-wed");
    }
}
