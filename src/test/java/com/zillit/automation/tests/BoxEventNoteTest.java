package com.zillit.automation.tests;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BoxCreateEventPage;
import com.zillit.automation.pages.BoxSchedulePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BOX — events and notes. One screen, two tabs, two quite different records.
 *
 * <p>Three behaviours here are worth more than the rest, and all three come from the
 * project owner's walkthrough backed by the code:
 * <ul>
 *   <li>An event's link to a schedule day is <b>optional</b> — it is a reminder, not a
 *       child. The code only attaches `scheduleDayId` "when the user explicitly linked
 *       one".</li>
 *   <li>Saving a NEW event asks <b>"Would you like to set a reminder for this event on
 *       your Home Calendar?"</b> — and that question is {@code setCancelable(false)}, so
 *       it must be answered. Editing an existing event must NOT ask again; the code says
 *       this is deliberate, "to avoid accidental flips on minor edits".</li>
 *   <li>Notes are <b>general or personal</b>. A general note is distributed; a personal
 *       one is private, and the PDF asks separately whether to include it.</li>
 * </ul>
 *
 * <p>Sheet: BOX_MODULE_TESTCASES.md §5 and §6.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.BOX_EVENT_NOTE)
class BoxEventNoteTest extends BoxBaseTest {

    private String savedEvent;

    // ──────────────────────────── events ────────────────────────────

    @Test
    @Order(1)
    @DisplayName("EV-01 Create Event opens on the event tab")
    void eventFormOpens() {
        BoxCreateEventPage f = openEvent();
        assertTrue(f.onEventTab(),
                "Create Event opened on something other than the event tab");
        back();
    }

    @Test
    @Order(2)
    @DisplayName("EV-02 An event with no title is refused")
    void titleRequired() {
        // NEGATIVE. An untitled event is a row nobody can identify later.
        BoxCreateEventPage f = openEvent();
        assumeTrue(f.onEventTab(), "Not on the event tab");
        assumeTrue(f.save(), "Save was not available");
        sleep(2500);

        // The Home-Calendar question only comes AFTER validation passes, so seeing it here
        // would mean an empty title got through.
        assertFalse(f.calendarPromptShown(Duration.ofSeconds(3)),
                "An event with no title reached the Home-Calendar question, which is asked"
                        + " only once the event is about to be committed");
        assertTrue(f.stillOpen(), "The form closed with no title");
        back();
    }

    @Test
    @Order(3)
    @DisplayName("EV-04 The schedule-day link is optional")
    void linkIsOptional() {
        BoxCreateEventPage f = openEvent();
        assumeTrue(f.onEventTab(), "Not on the event tab");
        // Nothing linked: the spinner should sit on its placeholder, not auto-pick a day.
        String linked = f.linkedDay();
        assertTrue(linked.isEmpty() || linked.toLowerCase().contains("select")
                        || linked.equals("null"),
                "A schedule day is preselected (\"" + linked + "\"). The link is meant to"
                        + " be the user's choice — the code attaches scheduleDayId only"
                        + " when one is explicitly picked.");
        back();
    }

    @Test
    @Order(4)
    @DisplayName("EV-11 Saving a new event asks about the Home Calendar")
    void calendarPromptOnCreate() {
        BoxCreateEventPage f = openEvent();
        assumeTrue(f.onEventTab(), "Not on the event tab");
        String title = fixtureName("Event");
        assumeTrue(f.setTitle(title), "Could not type a title");

        // FULL DAY, deliberately. A new event starts with its start and end times equal,
        // and the form refuses that with "end time before start"
        // (CreateEventActivity:1684-1688) — so a save with only a title never reaches the
        // Home-Calendar question at all. Full day skips the time comparison entirely.
        assumeTrue(f.toggleFullDay(), "The full-day switch is not available");
        sleep(1200);

        // Distribute To is mandatory (CreateEventActivity:1850). "Only Me" keeps the
        // event off every other person's calendar.
        assumeTrue(f.distributeToSelf(),
                "Distribute To could not be set. It is required before save, so without it"
                        + " the Home-Calendar question is never reached.");
        assumeTrue(f.save(), "Save was not available");

        assertTrue(f.calendarPromptShown(Duration.ofSeconds(10)),
                "Saving a new event did not ask \"" + BoxCreateEventPage.CALENDAR_PROMPT
                        + "\". It is asked in create mode only, and the answer decides"
                        + " whether the event also appears in the Calendar module.");

        // Answer No: this suite is not here to push rows into another module.
        assumeTrue(f.answerCalendarPrompt(false), "Neither Yes nor No could be tapped");
        sleep(4000);
        savedEvent = title;
        backToBox();
    }

    @Test
    @Order(5)
    @DisplayName("EV-14 Editing an event does NOT ask again")
    void noPromptOnEdit() {
        // The deliberate half of the behaviour: re-asking on every edit would let a stray
        // tap flip an event into or out of the Calendar module. The code preserves the
        // existing flag on edit for exactly this reason.
        assumeTrue(savedEvent != null, "EV-11 did not save an event to edit");
        BoxSchedulePage b = box();
        assumeTrue(b.tapText(savedEvent), "\"" + savedEvent + "\" is not on screen to open");
        sleep(2500);

        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(10)),
                "Tapping the event did not open anything editable");
        assumeTrue(f.setTitle(savedEvent + " EDITED"), "Could not change the title");
        sleep(800);
        assumeTrue(f.save(), "Save was not available");

        assertFalse(f.calendarPromptShown(Duration.ofSeconds(6)),
                "Editing an event asked the Home-Calendar question again. It is a"
                        + " create-mode question; asking on edit risks flipping the flag on"
                        + " an unrelated change.");
        sleep(3000);
        savedEvent = savedEvent + " EDITED";
        backToBox();
    }

    // ──────────────────────────── notes ────────────────────────────

    @Test
    @Order(6)
    @DisplayName("NT-01 Create Note opens on the note tab")
    void noteFormOpens() {
        BoxCreateEventPage f = openNote();
        assertTrue(f.onNoteTab(),
                "Create Note opened on something other than the note tab");
        back();
    }

    @Test
    @Order(7)
    @DisplayName("NT-03 A general note offers distribution")
    void generalNoteDistributes() {
        BoxCreateEventPage f = openNote();
        assumeTrue(f.onNoteTab(), "Not on the note tab");
        assumeTrue(f.noteTypeToggleShown(), "No note-type choice is offered");

        assumeTrue(f.chooseNoteType("General"), "Could not choose the general type");
        sleep(1500);
        assertTrue(f.noteDistributeShown(),
                "A general note offers no distribution. A general note is the one that"
                        + " goes to other people — without this it is indistinguishable"
                        + " from a personal one.");
        back();
    }

    @Test
    @Order(8)
    @DisplayName("NT-04 A personal note offers no distribution")
    void personalNoteIsPrivate() {
        // The mirror image, and the one that matters for privacy: a personal note that
        // offers distribution is one tap away from being shared with the whole unit.
        BoxCreateEventPage f = openNote();
        assumeTrue(f.onNoteTab(), "Not on the note tab");
        assumeTrue(f.noteTypeToggleShown(), "No note-type choice is offered");

        assumeTrue(f.chooseNoteType("Personal"), "Could not choose the personal type");
        sleep(1500);
        assertFalse(f.noteDistributeShown(),
                "A personal note is offering distribution. Personal means private to its"
                        + " author; the PDF even asks separately before including them.");
        back();
    }

    @Test
    @Order(9)
    @DisplayName("NT-02 A note with no title is refused")
    void noteTitleRequired() {
        BoxCreateEventPage f = openNote();
        assumeTrue(f.onNoteTab(), "Not on the note tab");
        assumeTrue(f.save(), "Save was not available");
        sleep(2500);
        assertTrue(f.stillOpen(), "A note with no title was accepted");
        back();
    }

    // ---------------- plumbing ----------------

    private BoxCreateEventPage openEvent() {
        backToBox();
        BoxSchedulePage b = box();
        if (!b.openDrawer()) {
            assumeTrue(false, "The drawer did not open. Toolbar: "
                    + b.toolbarState() + "; activity: " + resumedActivity());
        }
        assumeTrue(b.drawerCreateEvent(), "Create Event is not offered — needs posting rights");
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The event form did not open");
        return f;
    }

    private BoxCreateEventPage openNote() {
        backToBox();
        BoxSchedulePage b = box();
        if (!b.openDrawer()) {
            assumeTrue(false, "The drawer did not open. Toolbar: "
                    + b.toolbarState() + "; activity: " + resumedActivity());
        }
        assumeTrue(b.drawerCreateNote(), "Create Note is not offered — needs posting rights");
        BoxCreateEventPage f = new BoxCreateEventPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(15)), "The note form did not open");
        return f;
    }

    private void back() {
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        // A form with unsaved input may ask before closing; answer it so the next case
        // does not start behind a dialog.
        new com.zillit.automation.pages.MapDialogPage(driver).tap("Discard");
        new com.zillit.automation.pages.MapDialogPage(driver).tap("Yes");
        sleep(800);
        backToBox();
    }
}
