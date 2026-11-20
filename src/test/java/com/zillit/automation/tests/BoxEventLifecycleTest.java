package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxCalendarPage;
import com.zillit.automation.pages.BoxCreateEventPage;
import com.zillit.automation.pages.BoxInviteePage;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — the whole life of an event and of a note: made, changed, re-addressed, removed.
 *
 * <p>The existing event cases stop at "the form refuses rubbish and a save closes it".
 * Nothing yet followed an event after it was saved — renamed it, moved its date, changed
 * who it goes to, then checked that every view agrees about the result. That is where the
 * real defects are: a rename that leaves the old title in the list, a date change that
 * creates a second copy instead of moving one, a removed recipient who still gets it.
 *
 * <p>Each change is verified from the OUTSIDE — in the List row, which prints an event's
 * title, its date and its Distribute To together, and in the Calendar, which draws it on
 * its day. The form agreeing with itself proves nothing.
 *
 * <p><b>On recipients.</b> Saving an event addressed to a real person notifies that person.
 * So the picker is exercised in full — pick, see the chip, unpick, see it go — and then
 * cancelled; the only addressee this suite ever SAVES is the tester's own account, unless
 * {@code -Dzillit.boxDistributeReal=true} says the project owner has arranged otherwise
 * (a second device of their own, say). {@link #EN_06_presets} is skipped rather than
 * guessed when the project has no presets or no second user.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_EVENT_LIFECYCLE)
class BoxEventLifecycleTest extends BoxBaseTest {

    /** Opt-in: only then will a save address anyone but the tester. */
    private static final boolean DISTRIBUTE_REAL =
            Boolean.parseBoolean(System.getProperty("zillit.boxDistributeReal", "false"));

    private static final String ONLY_ME = "Only Me";

    private String eventTitle;
    private String noteTitle;
    private String movedToDay;

    // ─────────────────────── what this project offers ───────────────────────

    @Test
    @Order(1)
    @DisplayName("EN-00 Report who and what Distribute To can offer")
    void reportDistributeOptions() {
        // Informational, and deliberately not an assertion: whether a project has a second
        // user or any presets is a fact about the project, not a defect. The later cases
        // read this to decide whether they can run, and the report says why when they skip.
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.openDistribute(), "Distribute To could not be opened");
        BoxInviteePage inv = new BoxInviteePage(driver);
        assumeTrue(inv.isLoaded(Duration.ofSeconds(12)), "The invitee screen did not open");

        inv.openUsers();
        sleep(1800);
        List<String> users = inv.userNames();
        inv.openPresets();
        sleep(1800);
        List<String> presets = inv.presetNames();

        System.out.println(">>> EN-00 Distribute To offers " + users.size() + " user(s): "
                + users);
        System.out.println(">>> EN-00 and " + presets.size() + " preset(s): " + presets);
        System.out.println(">>> EN-00 saving to real recipients is "
                + (DISTRIBUTE_REAL ? "ENABLED" : "OFF")
                + " (-Dzillit.boxDistributeReal)");
        inv.cancel();
        sleep(1200);
    }

    // ─────────────────────────── the event ───────────────────────────

    @Test
    @Order(2)
    @DisplayName("EN-01 A new event appears in the list under its own title and date")
    void createEvent() {
        eventTitle = fixtureName("Event");
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.onEventTab() || f.chooseEventTab(), "The event tab is not offered");
        assumeTrue(f.setTitle(eventTitle), "Could not type a title");
        // Full day sidesteps the start/end time rule, which EN-09 tests on purpose.
        f.toggleFullDay();
        sleep(800);
        assumeTrue(f.distributeToSelf(), "Distribute To could not be set to " + ONLY_ME
                + ", and it is required before save");
        assumeTrue(f.save(), "Save was not available");
        sleep(3000);
        if (f.calendarPromptShown(Duration.ofSeconds(6))) f.answerCalendarPrompt(false);
        sleep(3000);

        BoxListPage.EventRow row = findEvent(eventTitle);
        assertNotNull(row, "\"" + eventTitle + "\" was saved but no list row carries it."
                + " Rows: " + eventRowsNow());
        assertNotNull(row.date, "The list shows \"" + eventTitle + "\" with no date: " + row);
    }

    @Test
    @Order(3)
    @DisplayName("EN-02 Renaming an event replaces the old title, it does not add a second")
    void renameEvent() {
        assumeTrue(eventTitle != null, "EN-01 created no event to rename");
        String renamed = eventTitle + " RENAMED";

        assumeTrue(openEventForEdit(eventTitle), "Edit was not available on the event row");
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(12)), "The event form did not reopen");
        assumeTrue(f.setTitle(renamed), "Could not retype the title");
        assumeTrue(f.save(), "Save was not available on the edit");
        sleep(4000);
        // Edit mode must not ask the Home-Calendar question again; answer it if it does, so
        // the case still reports the rename rather than dying here.
        if (f.calendarPromptShown(Duration.ofSeconds(4))) f.answerCalendarPrompt(false);
        sleep(2500);

        assertNotNull(findEvent(renamed),
                "The event was renamed to \"" + renamed + "\" but no row carries that."
                        + " Rows: " + eventRowsNow());
        assertNull(findEvent(eventTitle),
                "After renaming, a row still carries the OLD title \"" + eventTitle
                        + "\" — a rename has produced two events. Rows: " + eventRowsNow());
        eventTitle = renamed;
    }

    @Test
    @Order(4)
    @DisplayName("EN-03 Moving an event's date moves it — it does not leave a copy behind")
    void moveEventDate() {
        assumeTrue(eventTitle != null, "EN-01 created no event to move");
        BoxListPage.EventRow before = findEvent(eventTitle);
        assumeTrue(before != null && before.date != null,
                "The event has no readable date to move from");

        assumeTrue(openEventForEdit(eventTitle), "Edit was not available on the event row");
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(12)), "The event form did not reopen");

        // A day late enough in the month to be a real move, and never in the past: the
        // picker's minimum is today.
        movedToDay = "26";
        assumeTrue(f.setStartDay(movedToDay),
                "The start date could not be moved to day " + movedToDay);
        f.setEndDay(movedToDay);
        assumeTrue(f.save(), "Save was not available after moving the date");
        sleep(4000);
        if (f.calendarPromptShown(Duration.ofSeconds(4))) f.answerCalendarPrompt(false);
        sleep(2500);

        BoxListPage.EventRow after = findEvent(eventTitle);
        assertNotNull(after, "After moving the date the event vanished from the list."
                + " Rows: " + eventRowsNow());
        assertTrue(after.date != null && after.date.matches(".*\\b" + movedToDay + "\\b.*"),
                "The event was moved to day " + movedToDay + " but the list still shows it"
                        + " on \"" + after.date + "\"");
        assertEquals(1, countEvents(eventTitle),
                "Moving the date left " + countEvents(eventTitle) + " rows titled \""
                        + eventTitle + "\". A move must not duplicate the event.");
    }

    @Test
    @Order(5)
    @DisplayName("EN-04 Full day hides the time row, and turning it off brings it back")
    void fullDayTogglesTheTimeRow() {
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.onEventTab() || f.chooseEventTab(), "The event tab is not offered");
        boolean timesAtFirst = f.timeRowShown();
        assumeTrue(f.toggleFullDay(), "Full day could not be toggled");
        sleep(1200);
        assertTrue(timesAtFirst != f.timeRowShown(),
                "Full day was toggled and the time row stayed " 
                        + (timesAtFirst ? "visible" : "hidden")
                        + ". A full-day event has no times to set.");
        assumeTrue(f.toggleFullDay(), "Full day could not be toggled back");
        sleep(1200);
        assertEquals(timesAtFirst, f.timeRowShown(),
                "Toggling full day off did not restore the time row");
        closeForm();
    }

    // ───────────────────── recipients: add, see, remove ─────────────────────

    @Test
    @Order(6)
    @DisplayName("EN-05 A recipient can be added and taken off again, and Cancel keeps both out")
    void addAndRemoveRecipient() {
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.onEventTab() || f.chooseEventTab(), "The event tab is not offered");
        String before = String.valueOf(f.distributeTo());

        assumeTrue(f.openDistribute(), "Distribute To could not be opened");
        BoxInviteePage inv = new BoxInviteePage(driver);
        assumeTrue(inv.isLoaded(Duration.ofSeconds(12)), "The invitee screen did not open");
        assumeTrue(inv.openUsers(), "The Users tab is not offered");
        sleep(2000);

        List<String> users = inv.userNames();
        assumeTrue(!users.isEmpty(),
                "This project lists no users to distribute to, so adding and removing one"
                        + " cannot be tested. " + inv.availability());
        String who = users.get(0);

        // ADD — the chip strip is the app telling us who it thinks is chosen.
        assumeTrue(inv.setUserChecked(who, true), "\"" + who + "\" could not be ticked");
        sleep(1200);
        assertTrue(inv.selectedChips().stream().anyMatch(c -> c.contains(firstWord(who))),
                "\"" + who + "\" was ticked but no chip shows it: " + inv.selectedChips());
        assertTrue(inv.isUserChecked(who),
                "\"" + who + "\" shows a chip but its own row reads unticked");

        // REMOVE — and the chip must go with it.
        assumeTrue(inv.setUserChecked(who, false), "\"" + who + "\" could not be unticked");
        sleep(1200);
        assertFalse(inv.selectedChips().stream().anyMatch(c -> c.contains(firstWord(who))),
                "\"" + who + "\" was unticked and a chip still shows it: "
                        + inv.selectedChips() + ". A recipient left behind here is a"
                        + " recipient who gets the event.");

        // CANCEL — nothing chosen here may reach the form.
        assumeTrue(inv.cancel(), "Cancel was not available");
        sleep(1800);
        assertEquals(before, String.valueOf(f.distributeTo()),
                "Cancelling the invitee screen changed Distribute To from \"" + before
                        + "\" to \"" + f.distributeTo() + "\"");
        closeForm();
    }

    @Test
    @Order(7)
    @DisplayName("EN-06 A preset can be chosen as the recipients")
    void EN_06_presets() {
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.onEventTab() || f.chooseEventTab(), "The event tab is not offered");
        assumeTrue(f.openDistribute(), "Distribute To could not be opened");
        BoxInviteePage inv = new BoxInviteePage(driver);
        assumeTrue(inv.isLoaded(Duration.ofSeconds(12)), "The invitee screen did not open");
        assumeTrue(inv.openPresets(), "The Presets tab is not offered");
        sleep(2500);

        List<String> presets = inv.presetNames();
        assumeTrue(!presets.isEmpty(),
                "This project has no distribution presets, so choosing one cannot be"
                        + " tested. Make a preset with two or more users and this case"
                        + " starts working. " + inv.availability());

        String preset = presets.get(0);
        assumeTrue(inv.choosePreset(preset), "Preset \"" + preset + "\" could not be chosen");
        sleep(1500);
        assertFalse(inv.selectedChips().isEmpty(),
                "Preset \"" + preset + "\" was chosen and nobody appeared in the chips."
                        + " A preset that selects no one is an empty distribution list.");
        // Not saved: a preset addresses real people.
        inv.cancel();
        sleep(1200);
        closeForm();
    }

    // ─────────────────────────── negatives ───────────────────────────

    @Test
    @Order(8)
    @DisplayName("EN-07 A title under three characters is refused")
    void shortTitleRefused() {
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.onEventTab() || f.chooseEventTab(), "The event tab is not offered");
        assumeTrue(f.setTitle("ab"), "Could not type a short title");
        f.toggleFullDay();
        f.distributeToSelf();
        assumeTrue(f.save(), "Save was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "A two-character title was accepted. The app's own rule is three or more"
                        + " (CreateEventActivity:1657).");
        closeForm();
    }

    @Test
    @Order(9)
    @DisplayName("EN-08 An event with no recipients is refused")
    void noRecipientRefused() {
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.onEventTab() || f.chooseEventTab(), "The event tab is not offered");
        assumeTrue(f.setTitle(fixtureName("NoDist")), "Could not type a title");
        f.toggleFullDay();
        String dist = String.valueOf(f.distributeTo());
        assumeTrue(dist.isBlank() || "null".equals(dist) || dist.toLowerCase().contains("select"),
                "The form already has a Distribute To value (\"" + dist + "\"), so 'no"
                        + " recipients' is not what would be tested");
        assumeTrue(f.save(), "Save was not clickable");
        sleep(2500);
        assertTrue(f.stillOpen(),
                "An event with nobody to send it to was accepted. Distribute To is required"
                        + " (CreateEventActivity:1850), and an event addressed to no one"
                        + " reaches no one.");
        closeForm();
    }

    // ─────────────────────── the event in every view ───────────────────────

    @Test
    @Order(10)
    @DisplayName("EN-09 The event shows on its day in the calendar, not only in the list")
    void eventShowsInCalendar() {
        assumeTrue(eventTitle != null && movedToDay != null,
                "EN-03 did not move an event to a known day");
        BoxCalendarPage cal = openCalendar();
        List<String> titles = cal.titlesOn(movedToDay);
        assumeTrue(titles != null, "Day " + movedToDay + " is not in the grid on screen");
        assertTrue(titles.stream().anyMatch(t -> t.contains(firstWord(eventTitle))),
                "The list has \"" + eventTitle + "\" on day " + movedToDay + " and the"
                        + " calendar shows " + titles + " there. One event cannot be on a"
                        + " day in one view and missing from it in another.");
    }

    // ─────────────────────────── notes ───────────────────────────

    @Test
    @Order(11)
    @DisplayName("EN-10 A general note saves and offers distribution; a personal one does not")
    void notesDifferOnDistribution() {
        noteTitle = fixtureName("Note");
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.chooseNoteTab(), "The note tab is not offered");
        assumeTrue(f.onNoteTab(), "The note tab did not render");
        assumeTrue(f.noteTypeToggleShown(), "The note type toggle is not offered");

        // General → distribution section present.
        assumeTrue(f.chooseNoteType("General"), "\"General\" could not be chosen");
        sleep(1500);
        assertTrue(f.noteDistributeShown(),
                "A general note offered no distribution section. A general note is by"
                        + " definition one that goes to other people.");

        // Personal → distribution gone.
        assumeTrue(f.chooseNoteType("Personal"), "\"Personal\" could not be chosen");
        sleep(1500);
        assertFalse(f.noteDistributeShown(),
                "A personal note still offers distribution. A personal note that can be"
                        + " sent to the unit is not personal.");
        closeForm();
    }

    @Test
    @Order(12)
    @DisplayName("EN-11 A note saves, is renamed, and the list follows both times")
    void noteLifecycle() {
        noteTitle = fixtureName("Note");
        BoxCreateEventPage f = openEventForm();
        assumeTrue(f.chooseNoteTab(), "The note tab is not offered");
        assumeTrue(f.setNoteTitle(noteTitle), "Could not type a note title");
        f.setNoteText("Written by automation.");
        if (f.noteTypeToggleShown()) f.chooseNoteType("Personal");   // reaches nobody
        sleep(1200);
        assumeTrue(f.save(), "Save was not available for the note");
        sleep(4000);
        if (f.calendarPromptShown(Duration.ofSeconds(4))) f.answerCalendarPrompt(false);
        sleep(2500);

        assertNotNull(findEvent(noteTitle),
                "\"" + noteTitle + "\" was saved but no list row carries it. Rows: "
                        + eventRowsNow());

        String renamed = noteTitle + " EDITED";
        assumeTrue(openEventForEdit(noteTitle), "Edit was not available on the note row");
        BoxCreateEventPage g = new BoxCreateEventPage(driver);
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "The note form did not reopen");
        assumeTrue(g.setNoteTitle(renamed) || g.setTitle(renamed),
                "Could not retype the note title");
        assumeTrue(g.save(), "Save was not available on the note edit");
        sleep(4000);
        if (g.calendarPromptShown(Duration.ofSeconds(4))) g.answerCalendarPrompt(false);
        sleep(2500);

        assertNotNull(findEvent(renamed),
                "The note was renamed to \"" + renamed + "\" but no row carries that."
                        + " Rows: " + eventRowsNow());
        assertNull(findEvent(noteTitle),
                "After renaming, a row still carries the OLD note title \"" + noteTitle
                        + "\" — the rename made a second note");
        noteTitle = renamed;
    }

    // ─────────────────────────── cleanup ───────────────────────────

    @Test
    @Order(13)
    @DisplayName("EN-12 Cleanup: everything this suite made is deleted, and really gone")
    void cleanup() {
        for (String title : new String[] { eventTitle, noteTitle }) {
            if (title == null) continue;
            BoxListPage list = openList();
            if (!list.search(title)) continue;
            sleep(2000);
            if (!list.deleteEvent(title)) {
                System.out.println(">>> EN-12 no delete control for \"" + title + "\"");
                continue;
            }
            sleep(1200);
            list.confirmDelete();
            sleep(3000);
            // Deleting must actually delete — a row that survives here would be left on the
            // project for the next run to trip over.
            assertNull(findEvent(title),
                    "\"" + title + "\" was deleted and a list row still carries it");
        }
        BoxListPage list = openList();
        list.clearSearch();
    }

    // ─────────────────────────── helpers ───────────────────────────

    private BoxCreateEventPage openEventForm() {
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        if (f.isLoaded(Duration.ofSeconds(2))) return f;        // stay on the page
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openDrawer(), "The drawer did not open. Toolbar: " + b.toolbarState());
        assumeTrue(b.drawerCreateEvent(), "Create Event is not offered");
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The event form did not open");
        return f;
    }

    private void closeForm() {
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        if (!f.isLoaded(Duration.ofSeconds(1))) return;
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1200);
        // A form with content may ask before closing; say yes to leaving.
        new BoxSchedulePage(driver).tapText("Discard");
        sleep(800);
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

    private BoxCalendarPage openCalendar() {
        backToBox();
        BoxSchedulePage b = box();
        assumeTrue(b.openCalendarView(), "Calendar view button not available");
        sleep(1500);
        b.chooseMonth();
        sleep(2500);
        BoxCalendarPage cal = new BoxCalendarPage(driver);
        assumeTrue(cal.isLoaded(Duration.ofSeconds(12)), "The calendar did not load");
        return cal;
    }

    private BoxListPage.EventRow findEvent(String title) {
        BoxListPage list = openList();
        if (list.search(title)) sleep(2200);
        BoxListPage.EventRow row = list.findEvent(title);
        list.clearSearch();
        return row;
    }

    private int countEvents(String title) {
        BoxListPage list = openList();
        if (list.search(title)) sleep(2200);
        int n = (int) list.eventRows().stream()
                .filter(r -> r.title != null && r.title.contains(title)).count();
        list.clearSearch();
        return n;
    }

    private List<BoxListPage.EventRow> eventRowsNow() {
        return openList().eventRows();
    }

    private boolean openEventForEdit(String title) {
        BoxListPage list = openList();
        if (!list.search(title)) return false;
        sleep(2200);
        return list.editEvent(title);
    }

    private static String firstWord(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 1 ? parts[0] + " " + parts[1] : parts[0];
    }
}
