package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxCreateSchedulePage;
import com.zillit.automation.pages.BoxDayScopePage;
import com.zillit.automation.pages.BoxListPage;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — UPDATING a schedule, which is a different path from creating one.
 *
 * <p>Creating puts a new schedule over an old day and asks how to resolve it. Updating
 * takes a day that already exists and changes its type, and it goes somewhere else
 * entirely: a scope prompt ("this date" or the complete schedule), then either
 * {@code PUT /days/:id/single-date} — where the backend SPLITS the source block and makes
 * a one-day block — or {@code PUT /days/:id} for the whole thing. None of that was covered
 * by any case until now.
 *
 * <p>The expectations below are the app's own promises, taken from its strings, not from
 * guesswork. That matters because the same three words mean different things here than at
 * create time:
 *
 * <ul>
 *   <li><b>Replace</b> — "the block will shrink by 1 day" → source −1, new type +1</li>
 *   <li><b>Extend</b> — "extend by 1 day at the end to keep the same total" → source
 *       UNCHANGED, new type +1</li>
 *   <li><b>Overlap</b> — "keep the existing block on this date and also add the new type.
 *       Both will appear" → source unchanged, new type +1, both on the day</li>
 * </ul>
 *
 * <p>And the scope prompt's "this date" option promises "Every other day in this schedule
 * stays as it is", which is the one sentence these cases exist to hold it to.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_EDIT)
class BoxScheduleEditTest extends BoxBaseTest {

    private static final String PREP   = "Prep";
    private static final String SHOOT  = "Shoot";
    private static final String DAYOFF = "Day Off";
    private static final String TRAVEL = "Travel";

    /** A fresh multi-day block is laid down here so every case has something to edit. */
    private int blockDays;
    private String blockType;

    @Test
    @Order(1)
    @DisplayName("ED-00 Lay down a multi-day Prep block to edit")
    void layDownBlock() {
        BoxCreateSchedulePage f = openForm();
        assumeTrue(pickType(f, PREP), "This project has no \"" + PREP + "\" type."
                + " Types offered: " + f.typeNames());
        assumeTrue(f.chooseCalendarTab(), "The Calendar tab is not offered");
        assumeTrue(f.calendarShown(), "The calendar grid did not appear");
        assumeTrue(f.calendarNextMonth(), "Could not move to a future month");
        sleep(1200);

        int picked = 0;
        for (String d : List.of("4", "5", "6", "7")) if (f.pickCalendarDay(d)) picked++;
        assumeTrue(picked >= 3, "Fewer than three days could be picked for the block");
        assumeTrue(f.save(), "Save was not available");
        sleep(4000);
        commitAnyConflict();

        blockType = PREP;
        blockDays = daysOnRecord(PREP);
        assertTrue(blockDays >= 3,
                "A " + picked + "-day " + PREP + " block was saved but the List view holds "
                        + blockDays + " " + PREP + " day(s)");
    }

    // ───────────────────────── the scope prompt ─────────────────────────

    @Test
    @Order(2)
    @DisplayName("ED-01 Editing a day of a multi-day block asks which scope it means")
    void editAsksForScope() {
        String date = openEditOnBlockDay();
        assumeTrue(date != null, "No " + blockType + " row could be opened for editing");

        BoxDayScopePage scope = new BoxDayScopePage(driver);
        assertTrue(scope.shownForEdit(Duration.ofSeconds(6)),
                "Editing one day of a " + blockDays + "-day block did not ask whether the"
                        + " change is for that date or the whole schedule. Without the"
                        + " question, one tap silently rewrites every other day.");
        assertTrue(scope.offersThisDateForEdit() && scope.offersCompleteForEdit(),
                "The scope prompt appeared without both options: thisDate="
                        + scope.offersThisDateForEdit() + " complete="
                        + scope.offersCompleteForEdit());
        assertTrue(scope.saysOthersStay(),
                "The prompt does not tell the user that every other day stays — that"
                        + " sentence is what makes the choice safe to make");
        scope.cancel();
        sleep(1200);
    }

    @Test
    @Order(3)
    @DisplayName("ED-02 Cancelling the scope prompt changes nothing at all")
    void cancelChangesNothing() {
        int before = daysOnRecord(blockType);
        String date = openEditOnBlockDay();
        assumeTrue(date != null, "No row could be opened for editing");
        BoxDayScopePage scope = new BoxDayScopePage(driver);
        assumeTrue(scope.shownForEdit(Duration.ofSeconds(6)), "The scope prompt did not appear");
        assumeTrue(scope.cancel(), "Cancel was not available on the scope prompt");
        sleep(2000);

        assertEquals(before, daysOnRecord(blockType),
                "Cancelling the scope prompt changed the " + blockType + " block from "
                        + before + " days. Cancel must not be a mutation.");
    }

    @Test
    @Order(4)
    @DisplayName("ED-03 A single-day edit locks the date — only the type can change")
    void singleDayEditLocksTheDate() {
        String date = openEditOnBlockDay();
        assumeTrue(date != null, "No row could be opened for editing");
        BoxDayScopePage scope = new BoxDayScopePage(driver);
        assumeTrue(scope.shownForEdit(Duration.ofSeconds(6)), "The scope prompt did not appear");
        assumeTrue(scope.chooseThisDateForEdit(), "\"" + BoxDayScopePage.BTN_EDIT_THIS
                + "\" could not be chosen");
        sleep(3000);

        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(12)), "The edit form did not open");
        // The app states it outright: "Date cannot be changed when editing a single day".
        assertTrue(new BoxSchedulePage(driver).textShown("Date cannot be changed"),
                "A single-day edit opened without saying the date is fixed. If the date can"
                        + " be moved here, this stops being an edit of that day.");
        assertFalse(f.calendarShown(),
                "A single-day edit offered the calendar grid, so the locked date can be"
                        + " moved after all");
        closeForm();
    }

    // ──────────────── Prep → Shoot, each resolution by the numbers ────────────────

    @Test
    @Order(5)
    @DisplayName("ED-04 Replace on one day: the block shrinks by exactly one")
    void replaceShrinksByOne() {
        int prepBefore = daysOnRecord(PREP);
        int shootBefore = daysOnRecord(SHOOT);
        assumeTrue(prepBefore > 1, "Only " + prepBefore + " " + PREP + " day(s) — nothing"
                + " to shrink without emptying the block");

        assumeTrue(changeOneDayType(PREP, SHOOT, "replace"),
                "A single-day Prep→Shoot with Replace could not be carried out");

        // The app's wording for this option is exact: "The block will shrink by 1 day."
        assertEquals(prepBefore - 1, daysOnRecord(PREP),
                "Replace says the block shrinks by 1 day, but " + PREP + " went from "
                        + prepBefore + " to " + daysOnRecord(PREP));
        assertEquals(shootBefore + 1, daysOnRecord(SHOOT),
                "The day left " + PREP + " but " + SHOOT + " went from " + shootBefore
                        + " to " + daysOnRecord(SHOOT) + " — the day has gone nowhere");
    }

    @Test
    @Order(6)
    @DisplayName("ED-05 Extend on one day: the block keeps its total")
    void extendKeepsTheTotal() {
        int prepBefore = daysOnRecord(PREP);
        int offBefore = daysOnRecord(DAYOFF);
        assumeTrue(prepBefore > 1, "Only " + prepBefore + " " + PREP + " day(s) to extend");

        assumeTrue(changeOneDayType(PREP, DAYOFF, "extend"),
                "A single-day Prep→Day Off with Extend could not be carried out");

        // "Remove this date from the current block and assign it to the new type. The block
        // will extend by 1 day at the end to keep the same total." So the total is the
        // promise — it loses the edited day and gains one at the end.
        assertEquals(prepBefore, daysOnRecord(PREP),
                "Extend promises the block keeps the same total, but " + PREP + " went from "
                        + prepBefore + " to " + daysOnRecord(PREP) + ". If it shrank, this"
                        + " is Replace behaving under the Extend label.");
        assertEquals(offBefore + 1, daysOnRecord(DAYOFF),
                "Extend was chosen and " + DAYOFF + " went from " + offBefore + " to "
                        + daysOnRecord(DAYOFF) + " — the edited day never arrived");
    }

    @Test
    @Order(7)
    @DisplayName("ED-06 Overlap on one day: both types hold that day")
    void overlapKeepsBoth() {
        int prepBefore = daysOnRecord(PREP);
        assumeTrue(prepBefore >= 1, "No " + PREP + " day to overlap");
        String date = firstDateOf(PREP);
        assumeTrue(date != null, "No " + PREP + " date on screen");

        assumeTrue(changeOneDayType(PREP, TRAVEL, "overlap"),
                "A single-day Prep→Travel with Overlap could not be carried out");

        // "Keep the existing block on this date and also add the new type. Both will appear."
        assertEquals(prepBefore, daysOnRecord(PREP),
                "Overlap promises the existing block is kept, but " + PREP + " went from "
                        + prepBefore + " to " + daysOnRecord(PREP));
        assertTrue(daysOnRecord(TRAVEL) >= 1,
                "Overlap was chosen and " + TRAVEL + " is nowhere on record");
    }

    @Test
    @Order(8)
    @DisplayName("ED-07 Re-choosing the SAME type is a no-op, not a rewrite")
    void sameTypeIsANoOp() {
        int before = daysOnRecord(PREP);
        assumeTrue(before >= 1, "No " + PREP + " day to re-save");
        // The view model returns early when originalTypeId == newTypeId, so nothing should
        // move. A changed count here means a no-op edit is splitting the block anyway.
        assumeTrue(changeOneDayType(PREP, PREP, null),
                "Re-saving a day with its own type could not be carried out");
        assertEquals(before, daysOnRecord(PREP),
                "Re-choosing the same type took " + PREP + " from " + before + " to "
                        + daysOnRecord(PREP) + ". Changing nothing must change nothing.");
    }

    // ─────────────────────── the whole-block edit ───────────────────────

    @Test
    @Order(9)
    @DisplayName("ED-08 Editing the complete schedule moves every day of it")
    void completeEditMovesEveryDay() {
        int prepBefore = daysOnRecord(PREP);
        assumeTrue(prepBefore > 1, "Only " + prepBefore + " " + PREP + " day(s) — a"
                + " complete-schedule edit cannot be told apart from a single-day one");
        int shootBefore = daysOnRecord(SHOOT);

        String date = openEditOnBlockDay();
        assumeTrue(date != null, "No row could be opened for editing");
        BoxDayScopePage scope = new BoxDayScopePage(driver);
        assumeTrue(scope.shownForEdit(Duration.ofSeconds(6)), "The scope prompt did not appear");
        assumeTrue(scope.chooseCompleteForEdit(), "\"" + BoxDayScopePage.BTN_EDIT_ALL
                + "\" could not be chosen");
        sleep(3000);

        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(12)), "The edit form did not open");
        assumeTrue(pickType(f, SHOOT), "Could not change the type to " + SHOOT);
        assumeTrue(f.save(), "Save was not available on the edit form");
        sleep(5000);
        commitAnyConflict();

        // "Change the type and dates for all N days in this schedule." All of them.
        assertEquals(shootBefore + prepBefore, daysOnRecord(SHOOT),
                "A complete-schedule edit of a " + prepBefore + "-day " + PREP + " block"
                        + " left " + SHOOT + " at " + daysOnRecord(SHOOT) + ", up from "
                        + shootBefore + ". Every day of the block was supposed to move.");
        assertEquals(0, daysOnRecord(PREP),
                "The complete " + PREP + " block was retyped but " + daysOnRecord(PREP)
                        + " " + PREP + " day(s) are still on record");
    }

    // ───────────── the regression this module already had once ─────────────

    @Test
    @Order(10)
    @DisplayName("ED-09 Deleting one date asks first, and spares every other day")
    void deleteOneDateSparesTheRest() {
        // THE REGRESSION. The source records that Edit used to act on one day while Delete
        // removed the whole block with no warning, so picking a date and tapping Delete
        // wiped the schedule. Both verbs must ask now.
        String type = daysOnRecord(SHOOT) > 1 ? SHOOT : PREP;
        int before = daysOnRecord(type);
        assumeTrue(before > 1, "Only " + before + " " + type + " day(s), so 'every other"
                + " day' does not exist to spare");

        BoxListPage list = openList();
        assumeTrue(list.search(type), "The list has no search box");
        sleep(2500);
        String date = list.firstDateOfType(type);
        assumeTrue(date != null, "No " + type + " row on screen to delete");
        assumeTrue(list.deleteDay(date), "No delete control on the " + date + " row");
        sleep(1500);

        BoxDayScopePage scope = new BoxDayScopePage(driver);
        assertTrue(scope.shownForDelete(Duration.ofSeconds(6)),
                "Deleting one date of a " + before + "-day block did not ask what to"
                        + " delete. This is exactly the data loss the scope prompt was"
                        + " added to prevent.");
        assumeTrue(scope.chooseThisDateForDelete(), "\"" + BoxDayScopePage.BTN_DELETE_THIS
                + "\" could not be chosen");
        list.confirmDelete();
        sleep(4000);

        assertEquals(before - 1, daysOnRecord(type),
                "\"" + BoxDayScopePage.BTN_DELETE_THIS + "\" took " + type + " from "
                        + before + " days to " + daysOnRecord(type) + ". Its own"
                        + " description promises every other day stays.");
    }

    @Test
    @Order(11)
    @DisplayName("ED-10 Cleanup: the days this suite made are taken off again")
    void cleanup() {
        for (String type : List.of(SHOOT, DAYOFF, TRAVEL, PREP)) {
            for (int i = 0; i < 10; i++) {
                BoxListPage list = openList();
                if (!list.search(type)) break;
                sleep(2000);
                String date = list.firstDateOfType(type);
                if (date == null) break;
                if (!list.deleteDay(date)) break;
                sleep(1200);
                BoxDayScopePage scope = new BoxDayScopePage(driver);
                if (scope.shownForDelete(Duration.ofSeconds(3))) {
                    scope.chooseCompleteForDelete();
                }
                list.confirmDelete();
                sleep(2500);
            }
        }
        // Reported, never asserted: another suite's fixtures may share these type names, so
        // a leftover here is worth saying out loud but is not this suite's failure.
        System.out.println(">>> ED-10 cleanup finished. On record now — "
                + PREP + ":" + daysOnRecord(PREP) + " " + SHOOT + ":" + daysOnRecord(SHOOT)
                + " " + DAYOFF + ":" + daysOnRecord(DAYOFF)
                + " " + TRAVEL + ":" + daysOnRecord(TRAVEL));
    }

    // ─────────────────────────── helpers ───────────────────────────

    /**
     * Opens Edit on a day of the block under test and returns the date it opened.
     *
     * <p>Goes through the List view because that is where a user does this, and because the
     * row is the only place the edit control lives.
     */
    private String openEditOnBlockDay() {
        BoxListPage list = openList();
        if (!list.search(blockType == null ? PREP : blockType)) return null;
        sleep(2500);
        String date = list.firstDateOfType(blockType == null ? PREP : blockType);
        if (date == null) return null;
        return list.editDay(date) ? date : null;
    }

    private String firstDateOf(String type) {
        BoxListPage list = openList();
        if (!list.search(type)) return null;
        sleep(2000);
        return list.firstDateOfType(type);
    }

    /**
     * Changes ONE day from {@code from} to {@code to}, answering the inline chooser with
     * {@code action} ("replace" / "extend" / "overlap", or null when no change is expected).
     */
    private boolean changeOneDayType(String from, String to, String action) {
        BoxListPage list = openList();
        if (!list.search(from)) return false;
        sleep(2500);
        String date = list.firstDateOfType(from);
        if (date == null) return false;
        if (!list.editDay(date)) return false;
        sleep(1500);

        BoxDayScopePage scope = new BoxDayScopePage(driver);
        if (scope.shownForEdit(Duration.ofSeconds(5)) && !scope.chooseThisDateForEdit()) {
            return false;
        }
        sleep(3000);

        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        if (!f.isLoaded(Duration.ofSeconds(12))) return false;
        if (!pickType(f, to)) return false;
        sleep(1200);

        // The chooser here is INLINE on the form, not a dialog — it appears only once the
        // type differs from the day's current one (setupSingleDayEditUI).
        if (action != null) {
            BoxSchedulePage p = new BoxSchedulePage(driver);
            String label = action.substring(0, 1).toUpperCase() + action.substring(1);
            if (p.textShown(label)) p.tapText(label);
        }
        if (!f.save()) return false;
        sleep(5000);
        commitAnyConflict();
        return true;
    }

    private int daysOnRecord(String type) {
        BoxListPage list = openList();
        int n = list.daysOfTypeFiltered(type);
        list.clearSearch();
        return Math.max(n, 0);
    }

    private BoxListPage openList() {
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openListView(), "List view button not available");
        sleep(1500);
        b.chooseByDate();
        sleep(2500);
        BoxListPage list = new BoxListPage(driver);
        assumeTrue(list.isLoaded(Duration.ofSeconds(12)), "The list did not load");
        return list;
    }

    private BoxCreateSchedulePage openForm() {
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        if (f.isLoaded(Duration.ofSeconds(2))) { f.clearPickedDates(); return f; }
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open. Toolbar: " + b.toolbarState());
        assumeTrue(b.drawerCreateSchedule(), "Create Schedule is not offered");
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The schedule form did not open");
        return f;
    }

    private void closeForm() {
        BoxCreateSchedulePage f = new BoxCreateSchedulePage(driver);
        if (!f.isLoaded(Duration.ofSeconds(1))) return;
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1200);
    }

    private boolean pickType(BoxCreateSchedulePage f, String wanted) {
        String already = String.valueOf(f.selectedTypeName());
        if (already.equalsIgnoreCase(wanted)) return true;
        if (!f.openTypePicker()) return false;
        List<String> offered = f.typeNames();
        String match = offered.stream()
                .filter(n -> n != null && n.equalsIgnoreCase(wanted))
                .findFirst()
                .orElseGet(() -> offered.stream()
                        .filter(n -> n != null && n.toLowerCase().contains(wanted.toLowerCase()))
                        .findFirst().orElse(null));
        return match != null && f.chooseType(match);
    }

    /** A save may raise the create-time chooser; commit it so the state is known. */
    private void commitAnyConflict() {
        com.zillit.automation.pages.BoxConflictPage dlg =
                new com.zillit.automation.pages.BoxConflictPage(driver);
        if (dlg.shown(Duration.ofSeconds(4))) {
            if (!dlg.chooseReplace()) dlg.back();
            sleep(4000);
        }
    }
}
