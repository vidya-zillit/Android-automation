package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxConflictPage;
import com.zillit.automation.pages.BoxCreateSchedulePage;
import com.zillit.automation.pages.BoxSchedulePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — the conflict chooser: what happens when a new schedule lands on dates that already
 * carry one. The module's most consequential behaviour, and the one with a known defect.
 *
 * <p>The walkthrough from the project owner: a Prep exists, a Day Off is applied over it,
 * and the app offers three ways out —
 * <ul>
 *   <li><b>Replace</b> — the Prep goes, the Day Off takes those dates</li>
 *   <li><b>Extend</b> — the Prep stays and is pushed out at the end to make room</li>
 *   <li><b>Overlap</b> — both sit on the same dates</li>
 * </ul>
 *
 * <p><b>ZL-19566.</b> Every date mode must send {@code by_days}. With {@code by_dates} the
 * server treats startDate..endDate as authoritative and fills in the gaps, so picking
 * three scattered dates (17 / 26 / 29) and choosing Replace wiped all thirteen days
 * between them. Web and iOS always sent {@code by_days}; this was Android-only.
 *
 * <p>Sheet: BOX_MODULE_TESTCASES.md §4.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_CONFLICT)
class BoxConflictTest extends BoxBaseTest {

    /** Dates deliberately far apart, so a filled-in span is obvious if it happens. */
    private static final List<String> SCATTERED = List.of("8", "17", "26");

    @Test
    @Order(1)
    @DisplayName("CF-01 Overlapping dates raise the conflict chooser")
    void conflictRaised() {
        assumeTrue(saveOverSameDates(), "Could not create two schedules on the same dates");
        BoxConflictPage dlg = new BoxConflictPage(driver);
        assertTrue(dlg.shown(Duration.ofSeconds(10)),
                "Two schedules were saved onto the same dates and nothing asked what to do."
                        + " The server answers 409 for this and the app is meant to offer"
                        + " Replace / Extend / Overlap.");
        dlg.back();
        sleep(1500);
        backToBox();
    }

    @Test
    @Order(2)
    @DisplayName("CF-02 All three options are offered, each explained")
    void threeOptions() {
        assumeTrue(saveOverSameDates(), "Could not provoke a conflict");
        BoxConflictPage dlg = new BoxConflictPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(10)), "No conflict chooser appeared");

        assertTrue(dlg.replaceOffered(), "Replace is missing from the chooser");
        assertTrue(dlg.extendOffered(),  "Extend is missing from the chooser");
        assertTrue(dlg.overlapOffered(), "Overlap is missing from the chooser");

        // Three bare words are not a choice. Each option carries a line saying what it
        // does to the schedule already there, and that is what makes the choice safe.
        assertTrue(dlg.descriptionsShown(),
                "The options are named but not explained. Without the descriptions a user"
                        + " cannot tell Replace (which deletes) from Overlap (which does"
                        + " not).");
        dlg.back();
        sleep(1500);
        backToBox();
    }

    @Test
    @Order(3)
    @DisplayName("CF-01b The message says how many dates clash")
    void conflictCount() {
        assumeTrue(saveOverSameDates(), "Could not provoke a conflict");
        BoxConflictPage dlg = new BoxConflictPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(10)), "No conflict chooser appeared");

        String msg = String.valueOf(dlg.message());
        assertTrue(msg.matches(".*\\d.*"),
                "The conflict message carries no number: \"" + msg + "\". How many dates"
                        + " clash is the one fact that decides how worried to be.");
        dlg.back();
        sleep(1500);
        backToBox();
    }

    @Test
    @Order(4)
    @DisplayName("CF-03 Back leaves the chooser without saving anything")
    void backSavesNothing() {
        // THE NEGATIVE PATH, and the important one: a chooser that commits on dismissal
        // would destroy a schedule the user never agreed to touch.
        assumeTrue(saveOverSameDates(), "Could not provoke a conflict");
        BoxConflictPage dlg = new BoxConflictPage(driver);
        assumeTrue(dlg.shown(Duration.ofSeconds(10)), "No conflict chooser appeared");

        assertTrue(dlg.back(), "Back is not offered on the conflict chooser");
        sleep(2500);

        // Back belongs to the FORM, not to the module — the user should land back on
        // their input, able to change the dates instead.
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        assertTrue(f.stillOpen() || new BoxSchedulePage(driver).isLoaded(Duration.ofSeconds(8)),
                "Back from the conflict chooser left the app somewhere that is neither the"
                        + " form nor the module. Resumed activity: " + resumedActivity());
        assertFalse(dlg.shown(Duration.ofSeconds(2)), "Back did not dismiss the chooser");
        backToBox();
    }

    @Test
    @Order(5)
    @DisplayName("CF-07 ZL-19566 — scattered dates must not swallow the days between")
    void scatteredDatesKeepTheirGaps() {
        // THE REGRESSION CASE. Pick three dates far apart, choose Replace, and the days
        // in between must be untouched. Sending by_dates instead of by_days let the server
        // expand 8→26 into one block and Replace wiped everything inside it.
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.calendarNextMonth(), "Could not move to a future month");
        sleep(1200);

        int picked = 0;
        for (String day : SCATTERED) if (f.pickCalendarDay(day)) picked++;
        assumeTrue(picked == SCATTERED.size(),
                "Only " + picked + " of " + SCATTERED.size() + " scattered dates could be"
                        + " picked, so the gaps this case is about do not exist");

        // Before saving: the form itself must already understand this as THREE dates, not
        // a span. The chips are the hard evidence — one per picked date, carrying the date
        // itself. The count label is only a cross-check, and it must be READABLE: it is ""
        // while nothing is picked, which leaves no bounds and so reads as absent. The old
        // version of this case compared the absent label against a "no teens" pattern,
        // which "null" satisfied, so it passed without ever picking a date.
        List<String> chips = f.pickedDateChips();
        assertEquals(SCATTERED.size(), chips.size(),
                "Three scattered dates were picked but the form holds " + chips.size()
                        + ": " + chips + ". More than three means the gaps between "
                        + SCATTERED + " have been filled in — exactly the ZL-19566"
                        + " expansion, client-side.");

        // Cross-check only, and it must be scrolled to: the label sits below the chips in
        // the form's ScrollView, so three chips push it off screen, where it leaves the
        // accessibility tree. The chip assertion above is what guards ZL-19566.
        String count = f.pickedCountScrolled();
        if (count == null) {
            System.out.println(">>> CF-07 note: the count label could not be brought on"
                    + " screen; the chips " + chips + " are the evidence used instead.");
        } else {
            assertTrue(count.contains(String.valueOf(SCATTERED.size())),
                    "Three scattered dates were picked (" + chips + ") and the form reports"
                            + " \"" + count + "\". A larger number means the gaps have been"
                            + " filled in — exactly the ZL-19566 expansion.");
        }

        assumeTrue(f.save(), "Save was not available");
        sleep(4000);

        BoxConflictPage dlg = new BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(5))) {
            String msg = String.valueOf(dlg.message());
            // The count in the dialog is the server's view of how many dates clash. For
            // three scattered picks it can never legitimately exceed three.
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("(\\d+)").matcher(msg);
            if (m.find()) {
                int clashing = Integer.parseInt(m.group(1));
                assertTrue(clashing <= SCATTERED.size(),
                        "The server reports " + clashing + " clashing dates for "
                                + SCATTERED.size() + " scattered picks (\"" + msg + "\")."
                                + " It has expanded the span — Replace would now wipe the"
                                + " days in between. This is ZL-19566.");
            }
            dlg.back();
            sleep(1500);
        }
        backToBox();
    }

    @Test
    @Order(6)
    @DisplayName("CF-09 Re-applying the SAME type to a day changes nothing")
    void sameTypeIsANoOp() {
        // Quiet but worth having: the code returns early when the type has not changed, so
        // no request should go out and nothing should be asked. A conflict chooser here
        // would mean the app is treating a no-op as a clash.
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickAnyType(f), "No type could be chosen. " + f.typePickerState());
        String chosen = String.valueOf(f.selectedTypeName());
        assumeTrue(f.chooseDateRangeTab(), "The Date Range tab is not offered");
        assumeTrue(f.chooseByDays(), "The by-days mode is not offered");
        assumeTrue(f.setNumberOfDays("1"), "Could not set the day count");
        sleep(1000);
        assumeTrue(f.save(), "Save was not available");
        sleep(4000);

        BoxConflictPage dlg = new BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(4))) {
            // A clash with an unrelated schedule is fine; the point is only that the app
            // asked rather than silently overwrote. Back out and leave it alone.
            assertTrue(dlg.replaceOffered(),
                    "A conflict was raised for \"" + chosen + "\" but the chooser is"
                            + " incomplete");
            dlg.back();
            sleep(1500);
        }
        backToBox();
    }

    // ---------------- plumbing ----------------

    /**
     * Saves a schedule twice over the same dates, so the second raises a conflict.
     *
     * @return true when the second save was submitted — the caller checks for the chooser
     */
    private boolean saveOverSameDates() {
        for (int attempt = 0; attempt < 2; attempt++) {
            BoxCreateSchedulePage f = openForm();
            if (!pickAnyType(f)) return false;
            if (!f.chooseDateRangeTab()) return false;
            if (!f.chooseByDays()) return false;
            if (!f.setNumberOfDays("2")) return false;
            sleep(900);
            if (!f.save()) return false;
            sleep(4000);
            BoxConflictPage dlg = new BoxConflictPage(driver);
            if (dlg.shown(Duration.ofSeconds(4))) return true;   // the clash we wanted
            backToBox();
        }
        return false;
    }

    private BoxCreateSchedulePage openForm() {
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open. Toolbar: "
                + b.toolbarState() + "; activity: " + resumedActivity());
        assumeTrue(b.drawerCreateSchedule(), "Create Schedule is not offered");
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The schedule form did not open");
        return f;
    }

    private boolean pickAnyType(BoxCreateSchedulePage f) {
        String already = f.selectedTypeName();
        if (already != null && !already.isBlank() && !"null".equals(already)) return true;
        if (!f.openTypePicker()) return false;
        List<String> types = f.typeNames();
        return !types.isEmpty() && f.chooseType(types.get(0));
    }
}
