package com.zillit.automation.tests.box;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.box.BoxDayScopePage;
import com.zillit.automation.pages.box.BoxListPage;
import com.zillit.automation.pages.box.BoxSchedulePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — housekeeping. Takes back off the project what the Box suites left on it.
 *
 * <p><b>Opt-in, and deliberately not part of {@code --box} or {@code --all}.</b> It deletes
 * real schedule days from a real project, which no ordinary run should ever do on its own.
 * It needs {@code -Dzillit.boxCleanup=true}, and it is the project owner's decision to pass
 * it.
 *
 * <p>Why it exists: the schedule suites write days of the project's OWN types — Prep,
 * Shoot, Day Off, Travel — so there is no fixture prefix to recognise them by. The only
 * thing that marks them as the suites' is <b>when</b> they are: a month window the suites
 * work in. So this deletes by date window and nothing else, prints every day it takes off
 * before taking it off, and never widens the window on its own.
 *
 * <p>Each delete uses the scope prompt's <b>"Delete this date"</b>, never "complete
 * schedule": one day at a time, so a block that happens to be partly outside the window
 * keeps the days outside it. That is the least destructive option the app offers, and the
 * difference between tidying up and losing somebody's schedule.
 *
 * <p>Run it:
 * <pre>./gradlew test --tests '*BoxCleanupTest' -Dzillit.boxCleanup=true</pre>
 * and optionally {@code -Dzillit.boxCleanupMonth=Dec} to name the window.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_CLEANUP)
class BoxCleanupTest extends BoxBaseTest {

    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("zillit.boxCleanup", "false"));

    /** The window, as the list prints its month — "Dec", "Jan". Defaults to next month. */
    private static final String MONTH = System.getProperty(
            "zillit.boxCleanupMonth",
            LocalDate.now().plusMonths(1).getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH));

    /** The project's own type names, which the suites write days of. */
    private static final List<String> TYPES =
            List.of("Prep", "Shoot", "Wrap", "Day Off", "Travel");

    @Test
    @Order(1)
    @DisplayName("CU-01 List what is in the window, before anything is touched")
    void reportFirst() {
        assumeTrue(ENABLED, "Cleanup is opt-in: pass -Dzillit.boxCleanup=true to run it."
                + " It deletes real schedule days.");
        List<String> found = daysInWindow();
        System.out.println(">>> CU-01 window = \"" + MONTH + "\"; "
                + found.size() + " schedule day(s) will be deleted:");
        for (String d : found) System.out.println(">>>     " + d);
        if (found.isEmpty()) {
            System.out.println(">>>     (nothing — the window is already clear)");
        }
    }

    @Test
    @Order(2)
    @DisplayName("CU-02 Delete every schedule day inside the window, one date at a time")
    void deleteScheduleDays() {
        assumeTrue(ENABLED, "Cleanup is opt-in: pass -Dzillit.boxCleanup=true");
        int removed = 0;
        List<String> failed = new ArrayList<>();

        for (String type : TYPES) {
            for (int guard = 0; guard < 40; guard++) {
                BoxListPage list = openList();
                if (!list.search(type)) break;
                sleep(2200);

                String date = list.dayRows().stream()
                        .filter(r -> r.date != null && r.date.contains(MONTH))
                        .filter(r -> r.type != null
                                && r.type.toLowerCase().contains(type.toLowerCase()))
                        .map(r -> r.date)
                        .findFirst().orElse(null);
                if (date == null) break;                 // none of this type left in window

                if (!list.deleteDay(date)) {
                    failed.add(type + " on " + date + " (no delete control)");
                    break;
                }
                sleep(1300);
                BoxDayScopePage scope = new BoxDayScopePage(driver);
                if (scope.shownForDelete(Duration.ofSeconds(4))) {
                    // One date only. "Delete complete schedule" could take days that are
                    // outside the window and were never ours.
                    if (!scope.chooseThisDate()) {
                        scope.cancel();
                        failed.add(type + " on " + date + " (could not choose this-date)");
                        break;
                    }
                }
                list.confirmDelete();
                sleep(2800);
                System.out.println(">>> CU-02 deleted " + type + " on " + date);
                removed++;
            }
        }
        System.out.println(">>> CU-02 removed " + removed + " schedule day(s) from \""
                + MONTH + "\"");
        if (!failed.isEmpty()) {
            System.out.println(">>> CU-02 could not remove: " + failed);
        }
    }

    @Test
    @Order(3)
    @DisplayName("CU-03 Delete the events and notes the suites made, by their fixture name")
    void deleteFixtureEventsAndNotes() {
        assumeTrue(ENABLED, "Cleanup is opt-in: pass -Dzillit.boxCleanup=true");
        int removed = 0;
        // These DO carry the fixture prefix, so they can be recognised by name and the date
        // window does not come into it.
        for (int guard = 0; guard < 30; guard++) {
            BoxListPage list = openList();
            if (!list.search(FIXTURE_PREFIX)) break;
            sleep(2200);
            BoxListPage.EventRow row = list.eventRows().stream()
                    .filter(r -> r.title != null && r.title.contains(FIXTURE_PREFIX))
                    .findFirst().orElse(null);
            if (row == null) break;
            if (!list.deleteEvent(row.title)) {
                System.out.println(">>> CU-03 no delete control for \"" + row.title + "\"");
                break;
            }
            sleep(1200);
            list.confirmDelete();
            sleep(2800);
            System.out.println(">>> CU-03 deleted \"" + row.title + "\"");
            removed++;
        }
        System.out.println(">>> CU-03 removed " + removed + " event(s)/note(s) named "
                + FIXTURE_PREFIX);
    }

    @Test
    @Order(4)
    @DisplayName("CU-04 Report what is left in the window")
    void reportAfter() {
        assumeTrue(ENABLED, "Cleanup is opt-in: pass -Dzillit.boxCleanup=true");
        List<String> left = daysInWindow();
        System.out.println(">>> CU-04 still in \"" + MONTH + "\": " + left.size()
                + " schedule day(s)");
        for (String d : left) System.out.println(">>>     " + d);
        // Reported, never asserted. A day left here may be the project's own, and this
        // suite has no way to tell — so it says so and leaves the judgement to a person.
        if (!left.isEmpty()) {
            System.out.println(">>> CU-04 these were not removed. If any belong to the"
                    + " project rather than to a test run, that is correct — this suite"
                    + " cannot tell them apart and does not guess.");
        }
        BoxListPage list = openList();
        list.clearSearch();
    }

    // ─────────────────────────── helpers ───────────────────────────

    private List<String> daysInWindow() {
        List<String> out = new ArrayList<>();
        for (String type : TYPES) {
            BoxListPage list = openList();
            if (!list.search(type)) continue;
            sleep(2000);
            for (BoxListPage.DayRow r : list.dayRows()) {
                if (r.date != null && r.date.contains(MONTH)) out.add(r.toString());
            }
            list.clearSearch();
        }
        return out;
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
}
