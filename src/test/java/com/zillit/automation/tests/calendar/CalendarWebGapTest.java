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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calendar — single-account cases from the web E2E report the Android suite lacked
 * ({@code web-e2e/gaps/calendar.md}: M3, M4, M7, M8, M9, M10, M11, M12, M13, M6, M15, M18,
 * M19, M22, M23). Recurring scopes live in {@link CalendarRecurringScopeTest}; two-account
 * RSVP cases in {@link CalendarInviteTest}.
 *
 * <p>Case ids are the web test numbers ("WEB CAL #N"). Cases marked "ANDROID BUG" are
 * expected to FAIL on build Zillit_32539-dev until the app is fixed — the failure message
 * names the defect and the source line.
 *
 * <p>Run: {@code --only CalendarWebGapTest}. TestOrder: CALENDAR_WEB_GAP (201).
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.CALENDAR_WEB_GAP)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarWebGapTest extends CalendarGapBase {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private static String range(LocalDate d, LocalTime s, LocalTime e) {
        return d.format(SHORT) + " " + s.format(TIME) + " - " + e.format(TIME);
    }

    // ───────────────────────────── P1: live Android bugs ─────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[WEB CAL #42] A 65-minute event's duration is not shown as '1.5 hours' (ANDROID BUG)")
    void duration65Minutes() {
        String t = title("Dur65");
        LocalDate d = TODAY.plusDays(2);
        assertTrue(createPersonal(t, d, LocalTime.of(12, 0), LocalTime.of(13, 5), null),
                "The 12:00 PM - 1:05 PM event did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        String range = cal.detailText("tvTimeRange");
        String dur = cal.detailText("tvDuration");
        System.out.println(">>> 65-min detail: range='" + range + "' duration='" + dur + "'");
        assertEquals(range(d, LocalTime.of(12, 0), LocalTime.of(13, 5)), range, "Detail time range");
        cal.closeDetail();
        assertTrue(dur.startsWith("Duration:"), "Detail should show a Duration line, got '" + dur + "'");
        assertTrue(!dur.contains("1.5 hours"),
                "ANDROID BUG (web #42): 1 hour 5 minutes reads '" + dur + "'. EventDetailBottomSheet.kt:300-301 "
                        + "builds \"Duration: ${hours}.${mins} hours\", so 65 min → '1.5 hours' and 90 min → '1.30 hours'.");
    }

    @Test
    @Order(2)
    @DisplayName("[WEB CAL #30, #105, #31] New York timezone keeps the instant; the Created card shows the detail's time (ANDROID BUG on the card)")
    void timezoneKeepsInstantAndCardMatchesDetail() {
        String t = title("TzNY");
        LocalDate d = TODAY.plusDays(3);
        LocalTime s = LocalTime.of(14, 0), e = LocalTime.of(15, 0);
        String deviceZone = ZoneId.systemDefault().getId();
        final String[] tzBefore = {""}, tzAfter = {""}, startAfterNy = {""};
        boolean saved = createPersonal(t, d, s, e, form -> {
            tzBefore[0] = form.timezoneText();
            // #105 (only meaningful on an India device): Kathmandu is +5:45, so 2:00 PM IST → 2:15 PM; back to Kolkata → 2:00 PM.
            if (tzBefore[0].contains("Kolkata") || tzBefore[0].contains("Calcutta")) {
                String k = form.selectTimezone("Kathmandu");
                System.out.println(">>> tz after picking Kathmandu: '" + k + "'");
                if (k.contains("Kathmandu")) {
                    assertEquals("2:15 PM", form.timeStartText(), "[#105] Kathmandu should convert 2:00 PM IST to 2:15 PM");
                    form.selectTimezone("Kolkata");
                    assertEquals("2:00 PM", form.timeStartText(), "[#105] Back to Kolkata should restore 2:00 PM");
                } else {
                    System.out.println(">>> [#105] no Kathmandu row in the picker list — that half is not checked");
                }
            }
            tzAfter[0] = form.selectTimezone("New York");
            startAfterNy[0] = form.timeStartText();
        });
        System.out.println(">>> tz: device=" + deviceZone + " before='" + tzBefore[0] + "' after='" + tzAfter[0]
                + "' start after NY='" + startAfterNy[0] + "'");
        assertTrue(tzAfter[0].contains("New_York") || tzAfter[0].contains("New York"),
                "Timezone should read New York after picking it, got '" + tzAfter[0] + "'");
        assumeTrue(!deviceZone.equals("America/New_York"), "Device is already on New York time — nothing to convert");
        assertTrue(!"2:00 PM".equals(startAfterNy[0]),
                "[#30] Switching to New York should convert the Start time (EventFormViewModel.kt:341-360), still '2:00 PM'");
        assertTrue(saved, "The New York event did not save / show on " + d);

        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        String detail = cal.detailText("tvTimeRange");
        cal.closeDetail();
        System.out.println(">>> NY event detail range: " + detail);
        assertEquals(range(d, s, e), detail, "[#30] The detail (device time) should still read 2:00 PM - 3:00 PM");

        // #31: Events → Created Events → Personal → search → card time
        assertTrue(cal.openEventsScreen("rowCreated"), "Created Events did not open");
        cal.selectListTab("Personal");
        cal.searchList(t);
        assertTrue(cal.waitCard(t, Duration.ofSeconds(20)), "The event is not listed in Created → Personal: " + cal.cardTitles());
        String card = cal.cardDateTime(t);
        System.out.println(">>> NY event card range: " + card);
        cal.backToCalendar();
        assertEquals(detail, card,
                "ANDROID BUG (web #31): the Created Events card shows the time in the event's own timezone "
                        + "(EventCardAdapter.kt:268 formats with event.timezone) while the detail uses the device zone "
                        + "(EventDetailBottomSheet.kt:295) — same instant, two different clock times, no zone label.");
    }

    // ───────────────────────────── P2: detail content, validation, edit ─────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[WEB CAL #26] Personal timed event detail: trimmed title, Personal, '1 hours', week, no member-only parts, no reminder")
    void personalDetailContent() {
        String t = title("Detail");
        LocalDate d = TODAY.plusDays(2);
        assertTrue(createPersonal("  " + t + "  ", d, LocalTime.of(10, 0), LocalTime.of(11, 0), null),
                "The 10:00-11:00 AM event did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        String shownTitle = cal.detailText("tvTitle");
        String type = cal.detailText("tvEventType");
        String range = cal.detailText("tvTimeRange");
        String dur = cal.detailText("tvDuration");
        String week = cal.detailText("tvWeek");
        boolean invitees = cal.detailShows("layoutInviteeSummary");
        boolean callType = cal.detailShows("layoutCallType");
        boolean description = cal.detailShows("layoutDescription");
        boolean reminder = cal.detailShows("layoutReminder");
        boolean bottomBar = cal.detailShows("layoutBottomBar");
        boolean edit = cal.detailShows("btnEdit");
        screenshot("cal-detail-personal");
        cal.closeDetail();
        int wk = d.get(WeekFields.of(Locale.getDefault()).weekOfYear());
        assertEquals(t, shownTitle, "Title should be saved trimmed (EventFormLogic.kt:245)");
        assertEquals("Personal", type, "Event type chip");
        assertEquals(range(d, LocalTime.of(10, 0), LocalTime.of(11, 0)), range, "Time range");
        assertEquals("Duration: 1 hours", dur, "Duration line (EventDetailBottomSheet.kt:300)");
        assertTrue(week.matches("Week \\d+ of the Year"), "Week line, got '" + week + "' (expected about week " + wk + ")");
        assertTrue(!invitees && !callType && !description && !bottomBar,
                "A Personal event without description must not show invitees/call type/description/RSVP bar: invitees="
                        + invitees + " callType=" + callType + " description=" + description + " bottomBar=" + bottomBar);
        assertTrue(!reminder, "[CAL-09] With reminder None the Reminder row must be hidden");
        assertTrue(edit, "The creator should get Edit on a future event");
    }

    @Test
    @Order(4)
    @DisplayName("[WEB CAL #46, #99] Whitespace-only title is refused; a 3-character title is accepted")
    void whitespaceTitleRefused() {
        EventFormView form = formOn(TODAY.plusDays(2));
        form.selectPersonalType();
        form.typeTitleRaw("   ");
        form.tapSave();
        assertTrue(form.stillOnFormAfterSave(Duration.ofSeconds(3)), "A whitespace-only title must be refused (title is trimmed, EventFormLogic.kt:75)");
        String three = "Z" + RUN.substring(2);    // e.g. "Z34" — 3 chars, unique per run
        form.setTitle(three);
        form.tapSave();
        boolean saved = form.waitFormClosed(Duration.ofSeconds(30));
        if (saved) CREATED.add(new Object[]{three, TODAY.plusDays(2)});
        assertTrue(saved, "A 3-character title must be accepted");
        assertTrue(hasLive(rowsOn(TODAY.plusDays(2), three), three), "The 3-character event is not on the agenda");
    }

    @Test
    @Order(5)
    @DisplayName("[WEB CAL #49, #102] End time under 30 minutes after Start is pushed to Start + 30 (web minimum is 15)")
    void endUnder30MinutesPushed() {
        EventFormView form = formOn(TODAY.plusDays(2));
        form.selectPersonalType();
        assumeTrue(form.setStartTime(LocalTime.of(10, 0)), "Could not set Start 10:00 AM");
        assertEquals("11:00 AM", form.timeEndText(), "Setting Start moves End to Start + 1 h (EventFormViewModel.kt:257-260)");
        assumeTrue(form.setEndTime(LocalTime.of(10, 10)), "Could not set End 10:10 AM");
        assertEquals("10:30 AM", form.timeEndText(),
                "End 10:10 AM is under the 30-minute minimum and should snap to 10:30 AM (EventFormViewModel.kt:272-280)");
        form.goBack();
    }

    @Test
    @Order(6)
    @DisplayName("[WEB CAL #54, #21] A past start time today is moved forward to now (rounded up to 5 min), End = Start + 1 h")
    void pastStartTodayMovedForward() {
        LocalTime now = LocalTime.now();
        assumeTrue(now.isAfter(LocalTime.of(0, 30)) && now.isBefore(LocalTime.of(22, 50)),
                "Too close to midnight for a same-day past-time check");
        EventFormView form = formOn(TODAY);
        form.selectPersonalType();
        assumeTrue(form.setStartTime(LocalTime.of(0, 0)), "Could not set Start 12:00 AM");
        LocalTime start = EventFormView.parseFormTime(form.timeStartText());
        LocalTime end = EventFormView.parseFormTime(form.timeEndText());
        System.out.println(">>> past start corrected to " + start + " end " + end + " (now " + now + ")");
        assertTrue(start != null && !start.isBefore(now.minusMinutes(1)) && start.isBefore(now.plusMinutes(11)),
                "12:00 AM today should snap to now rounded up to 5 min, got '" + form.timeStartText() + "' (now " + now + ")");
        assertEquals(0, start.getMinute() % 5, "The corrected start should sit on a 5-minute boundary");
        assertEquals(start.plusHours(1), end, "End should be the corrected Start + 1 h");
        form.goBack();
    }

    @Test
    @Order(7)
    @DisplayName("[WEB CAL #48] 'Meet in Person & Call' needs a location; 'Meet in Person' saves without one")
    void meetInPersonCallNeedsLocation() {
        EventFormView form = formOn(TODAY.plusDays(3));
        form.selectMembersType();
        String t = title("MIPC");
        form.setTitle(t);
        assumeTrue(form.addInvitee("Pixel", "Vidya Pixel"), "Could not add Vidya Pixel as invitee");
        String ct = form.selectCallType("Meet in Person & Call");
        assumeTrue(ct.contains("& Call"), "Could not pick 'Meet in Person & Call' (got '" + ct + "')");
        form.tapSave();
        assertTrue(form.stillOnFormAfterSave(Duration.ofSeconds(3)),
                "'Meet in Person & Call' without a location must be refused (EventFormLogic.kt:112-116)");
        String plain = form.selectCallType("Meet in Person");
        assumeTrue("Meet in Person".equals(plain), "Could not pick 'Meet in Person' (got '" + plain + "')");
        assertTrue(!form.isCallLineShown(), "'Meet in Person' hides the Call Line (EventFormLogic.kt:55)");
        form.tapSave();
        boolean saved = form.waitFormClosed(Duration.ofSeconds(30));
        if (saved) CREATED.add(new Object[]{t, TODAY.plusDays(3)});
        assertTrue(saved, "'Meet in Person' with an invitee and no location should save");
    }

    @Test
    @Order(8)
    @DisplayName("[WEB CAL #103, #104, #106] End Date rolls to the next day for overnight times, incl. Dec 31 → Jan 1")
    void endDateRolls() {
        LocalDate d = TODAY.plusDays(4);
        EventFormView form = formOn(d);
        form.selectPersonalType();
        assumeTrue(form.setStartTime(LocalTime.of(23, 55)), "Could not set Start 11:55 PM");
        assertEquals("12:55 AM", form.timeEndText(), "[#104] 11:55 PM start → End 12:55 AM");
        assertEquals(d.plusDays(1), EventFormView.parseFormDate(form.endDateText()), "[#104] End Date should be the next day");
        assumeTrue(form.setStartTime(LocalTime.of(0, 0)), "Could not set Start 12:00 AM");
        assertEquals("1:00 AM", form.timeEndText(), "[#104] 12:00 AM start → End 1:00 AM");
        assertEquals(d, EventFormView.parseFormDate(form.endDateText()), "[#104] End Date stays the same day");
        assumeTrue(form.setStartTime(LocalTime.of(10, 0)) && form.setEndTime(LocalTime.of(9, 0)), "Could not set 10:00 → 9:00 AM");
        assertEquals(d.plusDays(1), EventFormView.parseFormDate(form.endDateText()), "[#103] End before Start means next day");
        LocalDate dec31 = LocalDate.of(TODAY.getYear(), 12, 31);
        if (!dec31.isBefore(TODAY) && form.setDate(dec31)) {
            assumeTrue(form.setStartTime(LocalTime.of(23, 0)) && form.setEndTime(LocalTime.of(0, 30)), "Could not set 11 PM → 12:30 AM");
            assertEquals(dec31.plusDays(1), EventFormView.parseFormDate(form.endDateText()),
                    "[#106] Dec 31 11 PM → 12:30 AM should end on Jan 1 of the next year");
        }
        form.goBack();
    }

    @Test
    @Order(9)
    @DisplayName("[WEB CAL #29] An 11 PM → 12:30 AM event shows on both days (ANDROID GAP on day 2)")
    void overnightShowsOnBothDays() {
        String t = title("Night");
        LocalDate d = TODAY.plusDays(4);
        final LocalDate[] endDate = {null};
        assertTrue(createPersonal(t, d, LocalTime.of(23, 0), LocalTime.of(0, 30),
                f -> endDate[0] = EventFormView.parseFormDate(f.endDateText())), "The overnight event did not save");
        assertEquals(d.plusDays(1), endDate[0], "The form's End Date should roll to the next day");
        assertTrue(hasLive(rowsOn(d, t), t), "The event should be on its start day " + d);
        List<String> next = rowsOn(d.plusDays(1), t);
        assertTrue(hasLive(next, t),
                "ANDROID GAP (web #29): an 11 PM - 12:30 AM event is not listed on " + d.plusDays(1)
                        + ". The agenda and the month dots group by START date only (CalendarViewModel.kt:86-97); web shows both days. Rows: " + next);
    }

    @Test
    @Order(10)
    @DisplayName("[WEB CAL #58, #63] Edit moves date and time (type locked, 'Update Events'); Cancel in the delete confirm keeps it (save time-shift seen intermittently)")
    void editMovesDateAndTime() {
        String t = title("Move");
        LocalDate d2 = TODAY.plusDays(2), d3 = TODAY.plusDays(3);
        assertTrue(createPersonal(t, d2, LocalTime.of(16, 0), LocalTime.of(17, 0), null), "The 4-5 PM event did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d2, t), "Could not open the event");
        cal.detailTapDelete();
        assertTrue(cal.isConfirmDeleteDialogOpen(Duration.ofSeconds(6)), "[#63] Delete should ask 'Are you sure you want to delete this event?'");
        cal.cancelDialog();
        cal.closeDetail();
        List<String> kept = rowsOn(d2, t);
        assertTrue(hasLive(kept, t) && !hasCancelled(kept, t), "[#63] Cancel in the delete confirm must keep the event; rows " + kept);

        assertTrue(openEvent(d2, t), "Could not reopen the event");
        cal.detailTapEdit();
        EventFormView form = new EventFormView(driver);
        assertTrue(form.isLoaded(), "Edit form did not open");
        assertTrue(!form.isTypeToggleShown(), "Members/Personal toggle must be hidden on edit (EventFormFragment.kt:430)");
        assertEquals("Update Events", form.saveButtonText(), "Edit button label (strings.xml update_event)");
        assumeTrue(form.setDate(d3), "Could not move the date to " + d3);
        assumeTrue(form.setStartTime(LocalTime.of(13, 0)), "Could not set Start 1:00 PM");
        assertEquals("1:00 PM", form.timeStartText(), "The edit form should show the picked Start 1:00 PM");
        assertEquals("2:00 PM", form.timeEndText(), "End follows Start + 1 h");
        form.tapSave();
        assertTrue(form.waitFormClosed(Duration.ofSeconds(30)), "Edit did not save");
        CREATED.removeIf(c -> t.equals(c[0]));
        CREATED.add(new Object[]{t, d3});
        assertTrue(openEvent(d3, t), "The moved event is not on " + d3 + ": " + rowsOn(d3, t));
        String range = cal.detailText("tvTimeRange");
        cal.closeDetail();
        // INTERMITTENT: on 2026-11-24 at ~00:40 and ~01:20 IST the form read "1:00 PM / 2:00 PM" on d3
        // (asserted above) but the saved event read "12:30 AM - 1:30 AM"; at 03:56 IST it saved
        // correctly. Not reproduced since — keep the check so a recurrence fails with this message.
        assertEquals(range(d3, LocalTime.of(13, 0), LocalTime.of(14, 0)), range,
                "Web #58 (intermittent, seen twice after midnight IST): the edit form showed " + d3 + " 1:00-2:00 PM, but the saved event reads a different time");
        List<String> old = rowsOn(d2, t);
        assertTrue(!hasLive(old, t), "The event should have left " + d2 + "; rows " + old);
    }

    @Test
    @Order(11)
    @DisplayName("[WEB CAL #32] Reminder '30 minutes before' and preset colour Warm Blue are saved and shown")
    void reminderAndColourSaved() {
        String t = title("Remind");
        LocalDate d = TODAY.plusDays(2);
        final String[] hex = {""};
        assertTrue(createPersonal(t, d, LocalTime.of(9, 0), LocalTime.of(10, 0), f -> {
            assumeTrue(f.selectReminder("30 minutes before"), "Reminder dialog did not offer '30 minutes before'");
            assumeTrue(f.pickPresetColor(4), "Could not pick the 5th preset swatch");
            hex[0] = f.colorHexText();
        }), "The event did not save");
        assertEquals("#5B7FA5", hex[0].toUpperCase(Locale.ROOT), "The 5th preset is Warm Blue #5B7FA5 (Constants.kt:15)");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        String reminder = cal.detailText("tvReminder");
        cal.closeDetail();
        assertEquals("30 minutes before", reminder, "Detail reminder (EventDetailBottomSheet.kt:359-366)");
    }

    // ───────────────────────────── P2: things the app does NOT enforce ─────────────────────────────

    @Test
    @Order(12)
    @DisplayName("[WEB CAL #51] Custom repeat with no weekday ticked is refused")
    void customRepeatNeedsWeekday() {
        String t = title("NoDay");
        LocalDate d = TODAY.plusDays(2);
        EventFormView form = formOn(d);
        form.selectPersonalType();
        form.setTitle(t);
        form.toggleAllDay();
        assumeTrue(form.selectRecurrence("Custom"), "Repeat dialog did not offer Custom");
        form.tapSave();
        boolean refused = form.stillOnFormAfterSave(Duration.ofSeconds(4));
        if (!refused) CREATED.add(new Object[]{t, d});
        else form.goBack();
        // The client never checks selectedDays (EventFormLogic.validate, EventFormLogic.kt:59-118),
        // but on 2026-11-23 the save was still refused (form stayed open) — the server rejects
        // it. Asserting the user-visible outcome: no such event is created.
        assertTrue(refused, "Web #51: a Custom repeat with no weekday saved. EventFormLogic.validate() "
                + "(EventFormLogic.kt:59-118) never checks selectedDays, and the server accepted it.");
    }

    @Test
    @Order(13)
    @DisplayName("[WEB CAL #50] A reminder that is already in the past is refused (ANDROID BUG)")
    void pastReminderRefused() {
        String t = title("PastRem");
        EventFormView form = formOn(TODAY);
        form.selectPersonalType();
        form.setTitle(t);
        form.toggleAllDay();
        assumeTrue(form.selectReminder("5 minutes before"), "Reminder dialog did not offer '5 minutes before'");
        form.tapSave();
        boolean refused = form.stillOnFormAfterSave(Duration.ofSeconds(4));
        if (!refused) CREATED.add(new Object[]{t, TODAY});
        else form.goBack();
        assertTrue(refused, "ANDROID BUG (web #50): an all-day event today with a '5 minutes before' reminder "
                + "(already past) saved. Android has no reminder-in-the-past check (EventFormLogic.kt:59-118).");
    }

    private static String pastTitle;
    private static LocalDate pastDay;

    @Test
    @Order(14)
    @DisplayName("[WEB CAL #20] An event cannot be created on a past date picked on the grid (ANDROID BUG)")
    void pastDateFromGridRefused() {
        LocalDate y = TODAY.minusDays(1);
        String t = title("Past");
        CalendarPage cal = calendar();
        assumeTrue(cal.selectDay(y), "Could not select yesterday");
        EventFormView form = cal.openEventForm();
        assumeTrue(form.isLoaded(), "Form did not open");
        LocalDate shown = EventFormView.parseFormDate(form.dateText());
        form.selectPersonalType();
        form.setTitle(t);
        form.toggleAllDay();
        form.tapSave();
        boolean refused = form.stillOnFormAfterSave(Duration.ofSeconds(4));
        if (!refused) {
            pastTitle = t;
            pastDay = shown;
            CREATED.add(new Object[]{t, shown});
        } else {
            form.goBack();
        }
        assertTrue(refused || !y.equals(shown),
                "ANDROID BUG (web #20): the FAB pre-filled yesterday (" + shown + ") and an all-day event on it saved. "
                        + "CalendarFragment.kt:323-336 forwards the selected day and EventFormLogic.validate() has no past-date rule.");
    }

    @Test
    @Order(15)
    @DisplayName("[WEB CAL #25, #45] An expired event opens read-only: 'This event has expired', no Edit/Delete")
    void expiredEventReadOnly() {
        assumeTrue(pastTitle != null, "No past event to open (the app refused the past-date create — #20 fixed)");
        CalendarPage cal = calendar();
        assertTrue(openEvent(pastDay, pastTitle), "Could not open the past event");
        String status = cal.detailText("tvInvitationStatus");
        boolean edit = cal.detailShows("btnEdit"), delete = cal.detailShows("btnDelete");
        screenshot("cal-expired-detail");
        cal.closeDetail();
        assertEquals("This event has expired", status, "Status bar of an expired event (EventDetailBottomSheet.kt:461-469)");
        assertTrue(!edit && !delete, "Expired events must not offer Edit/Delete (EventDetailBottomSheet.kt:240-252): edit="
                + edit + " delete=" + delete);
        CREATED.removeIf(c -> pastTitle.equals(c[0]));   // expired: cannot be cancelled from the app
    }

    // ───────────────────────────── P2/P3: lists, views, options ─────────────────────────────

    @Test
    @Order(16)
    @DisplayName("[WEB CAL #4, #5, #72, #73, #74, #77, #79, #80, #83] Events menu, the three list screens, tabs and no-match search")
    void eventsMenuAndLists() {
        CalendarPage cal = calendar();
        List<String> rows = cal.openEventsMenu();
        assertEquals(List.of("Received Events", "Created Events", "Declined Events"), rows, "[#4] Events menu rows");
        String miss = "zz-qa-no-match-" + RUN;

        assertTrue(cal.openEventsScreen("rowReceived"), "[#5] Received Events did not open");
        assertTrue(cal.selectedListTab().startsWith("Pending"), "[#5] Received opens on Pending, got '" + cal.selectedListTab() + "'");
        for (String tab : new String[]{"Accepted", "Expired", "Pending"}) {
            assertTrue(cal.selectListTab(tab), "[#72/#73] Could not open the " + tab + " tab");
            cal.searchList(miss);
            assertTrue(cal.isListEmptyState(Duration.ofSeconds(15)), "[#74] Received/" + tab + ": no-match search should show the empty state");
            assertEquals("No events", cal.listEmptyText(), "Received empty-state text");
        }
        cal.backToCalendar();

        assertTrue(cal.openEventsScreen("rowCreated"), "Created Events did not open");
        assertTrue(cal.selectedListTab().startsWith("Invites Sent"), "Created opens on Invites Sent, got '" + cal.selectedListTab() + "'");
        for (String tab : new String[]{"Personal", "Expired"}) {
            assertTrue(cal.selectListTab(tab), "Could not open Created/" + tab);
            cal.searchList(miss);
            assertTrue(cal.isListEmptyState(Duration.ofSeconds(15)), "[#77] Created/" + tab + ": no-match search should show the empty state");
        }
        cal.backToCalendar();

        assertTrue(cal.openEventsScreen("rowDeclined"), "Declined Events did not open");
        for (String tab : new String[]{"Received", "Created"}) {
            assertTrue(cal.selectListTab(tab), "Could not open Declined/" + tab);
            cal.searchList(miss);
            assertTrue(cal.isListEmptyState(Duration.ofSeconds(15)), "[#79/#80] Declined/" + tab + ": no-match search should show the empty state");
            assertEquals("No declined events", cal.listEmptyText(), "Declined empty-state text");
        }
        cal.backToCalendar();

        // #83: each open is a new Activity — starts on its first tab with an empty search
        assertTrue(cal.openEventsScreen("rowReceived"), "Received Events did not reopen");
        assertTrue(cal.selectedListTab().startsWith("Pending"), "[#83] Reopened Received should start on Pending, got '" + cal.selectedListTab() + "'");
        assertEquals("", cal.listSearchText().replace("Search by event title...", ""), "[#83] Reopened Received should have an empty search");
        cal.backToCalendar();
    }

    @Test
    @Order(17)
    @DisplayName("[WEB CAL #81, #82] Join Call screen: no-match search shows 'No call events'")
    void joinCallScreen() {
        CalendarPage cal = calendar();
        assertTrue(cal.isJoinCallVisible(), "Join Call quick action missing");
        driver.findElement(io.appium.java_client.AppiumBy.id(appPackage + ":id/btnJoinCall")).click();
        sleep(3000);
        assertTrue(!driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)join call on events\")")).isEmpty(), "Join Call screen header 'Join Call on Events' (CalendarFragment.kt:308-313)");
        cal.searchList("zz-qa-no-match-" + RUN);
        assertTrue(cal.isListEmptyState(Duration.ofSeconds(15)), "No-match search should show the empty state");
        assertEquals("No call events", cal.listEmptyText(), "Join Call empty-state text");
        cal.backToCalendar();
    }

    @Test
    @Order(18)
    @DisplayName("[WEB CAL #16, #15, #17] View menu is Month/Week/Day; Week/Day paging moves 7/1 days; Today is marked")
    void viewsPagingAndToday() {
        CalendarPage cal = calendar();
        assertEquals(List.of("Month", "Week", "Day"), cal.viewModeOptions(), "[#16] View options");
        assumeTrue(cal.selectDay(TODAY), "Could not select today");
        assumeTrue(cal.toggleViewMode("Day"), "Could not switch to Day");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
        try {
            assertEquals(TODAY.format(dayFmt), cal.monthTitle(), "Day view title (CalendarViewModel.kt:58)");
            assertTrue(cal.textShown("TODAY", Duration.ofSeconds(4)), "[#17] Day view should show the TODAY badge");
            cal.nextMonth();
            sleep(800);
            assertEquals(TODAY.plusDays(1).format(dayFmt), cal.monthTitle(), "[#15] Day next moves one day");
            cal.tapToday();
            sleep(800);
            assertEquals(TODAY.format(dayFmt), cal.monthTitle(), "[#15] Today returns to today");

            assumeTrue(cal.toggleViewMode("Week"), "Could not switch to Week");
            String week = cal.monthTitle();
            cal.nextMonth();
            sleep(800);
            String next = cal.monthTitle();
            assertTrue(!week.equals(next), "[#15] Week next should change the range title");
            cal.previousMonth();
            sleep(800);
            assertEquals(week, cal.monthTitle(), "[#15] Week previous should come back");
        } finally {
            cal.toggleViewMode("Month");
        }
    }

    @Test
    @Order(19)
    @DisplayName("[WEB CAL #27] Tapping a future day then the FAB pre-fills that Start Date")
    void fabPrefillsSelectedDay() {
        LocalDate d = TODAY.plusDays(6);
        EventFormView form = formOn(d);   // formOn asserts the pre-fill
        assertEquals(d, EventFormView.parseFormDate(form.dateText()), "Start Date pre-fill");
        form.goBack();
    }

    @Test
    @Order(20)
    @DisplayName("[WEB CAL #35] Call Type lists exactly 4 options and drives Call Line; Personal hides the People section")
    void callTypeOptionsAndCallLine() {
        EventFormView form = formOn(TODAY.plusDays(2));
        form.selectMembersType();
        assertEquals(List.of("Audio Call", "Video Call", "Meet in Person", "Meet in Person & Call"), form.callTypeOptions(),
                "Call type options (EventFormFragment.kt:150)");
        for (String ct : new String[]{"Audio Call", "Video Call", "Meet in Person & Call"}) {
            form.selectCallType(ct);
            assertTrue(form.isCallLineShown(), "Call Line should show for " + ct);
        }
        form.selectCallType("Meet in Person");
        assertTrue(!form.isCallLineShown(), "Call Line should be hidden for Meet in Person");
        form.selectPersonalType();
        assertTrue(!form.isPeopleSectionShown(), "Personal hides the People section (EventFormFragment.kt:511-514)");
        form.goBack();
    }

    @Test
    @Order(21)
    @DisplayName("[WEB CAL #64, #65, #112] Each frequency pre-fills its Repeat Until; Custom shows weekday chips; option lists")
    void recurrenceDefaults() {
        LocalDate d = TODAY.plusDays(2);
        EventFormView form = formOn(d);
        form.selectPersonalType();
        assertEquals(List.of("Does not repeat", "Daily", "Weekly", "Monthly", "Yearly", "Custom"), form.dialogOptions("tvRecurrence"),
                "[#112] Repeat options");
        assertEquals(List.of("None", "5 minutes before", "10 minutes before", "15 minutes before", "30 minutes before", "1 hour before"),
                form.dialogOptions("tvNotify"), "[#112] Reminder options (Constants.kt:25-32)");
        String[][] cases = {{"Daily", "1d"}, {"Weekly", "1w"}, {"Monthly", "1m"}, {"Yearly", "1y"}, {"Custom", "1w"}};
        for (String[] c : cases) {
            assumeTrue(form.selectRecurrence(c[0]), "Repeat dialog did not offer " + c[0]);
            LocalDate want = switch (c[1]) {
                case "1d" -> d.plusDays(1);
                case "1w" -> d.plusWeeks(1);
                case "1m" -> d.plusMonths(1);
                default -> d.plusYears(1);
            };
            assertEquals(want, EventFormView.parseFormDate(form.recurrenceEndText()),
                    "[#64] " + c[0] + " should default Repeat Until (EventFormLogic.kt:272-281)");
            assertEquals("Custom".equals(c[0]), form.isChipGroupDaysShown(), "[#65] Weekday chips only for Custom (" + c[0] + ")");
        }
        assumeTrue(form.selectRecurrence("Weekly"), "Could not pick Weekly");
        if (form.setDate(d.plusDays(2))) {
            assertEquals(d.plusDays(2).plusWeeks(1), EventFormView.parseFormDate(form.recurrenceEndText()),
                    "[#64] Changing Start re-derives Repeat Until (EventFormViewModel.kt:231)");
        }
        form.goBack();
    }

    @Test
    @Order(22)
    @DisplayName("[WEB CAL #98, #100, #101] Unicode/RTL/HTML title and a 150-char title round-trip as plain text")
    void textEdgeCases() {
        LocalDate d = TODAY.plusDays(5);
        String uni = "ZL_AUTO Ünïcødé עברית <b>x</b> & " + RUN;
        String desc = "Line <i>one</i> — ñ 中文 " + RUN;
        assertTrue(createPersonal(uni, d, LocalTime.of(8, 0), LocalTime.of(9, 0), f -> f.setDescription(desc)),
                "The unicode-titled event did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, RUN), "Could not open the unicode event");
        String shownTitle = cal.detailText("tvTitle"), shownDesc = cal.detailText("tvDescription");
        cal.closeDetail();
        assertEquals(uni, shownTitle, "[#98/#100] Title should round-trip exactly as plain text");
        assertEquals(desc, shownDesc, "[#98] Description should round-trip exactly as plain text");

        StringBuilder sb = new StringBuilder("ZL_AUTO Long " + RUN + " ");
        while (sb.length() < 150) sb.append('x');
        String longTitle = sb.substring(0, 150);
        assertTrue(createPersonal(longTitle, d, LocalTime.of(10, 0), LocalTime.of(11, 0), null), "The 150-char event did not save");
        assertTrue(openEvent(d, "ZL_AUTO Long " + RUN), "Could not open the long-titled event");
        String shownLong = cal.detailText("tvTitle");
        cal.closeDetail();
        assertEquals(longTitle, shownLong, "[#101] A 150-character title should round-trip");
    }

    @Test
    @Order(24)
    @DisplayName("[WEB CAL #34 case B] Organizer-excluded Members event: detail shows '- Excluded'; listed in Created → Invites Sent")
    void organizerExcludedEvent() {
        String t = title("Excl");
        LocalDate d = TODAY.plusDays(5);
        EventFormView form = formOn(d);
        form.selectMembersType();
        form.setTitle(t);
        assumeTrue(form.setStartTime(LocalTime.of(15, 0)), "Could not set Start 3:00 PM");
        assumeTrue(form.addInvitee("Pixel", "Vidya Pixel"), "Could not add Vidya Pixel as invitee");
        assumeTrue(form.tickExcludeMe(), "Exclude-me checkbox not found");
        form.tapSave();
        boolean saved = form.waitFormClosed(Duration.ofSeconds(30));
        if (saved) CREATED.add(new Object[]{t, d});
        assertTrue(saved, "The organizer-excluded event did not save");
        CalendarPage cal = calendar();
        assertTrue(cal.openEventsScreen("rowCreated"), "Created Events did not open");
        cal.selectListTab("Invites Sent");
        cal.searchList(t);
        boolean listed = cal.waitCard(t, Duration.ofSeconds(20));
        boolean cardTag = cal.textShown("- Excluded", Duration.ofSeconds(3));
        String label = "";
        if (listed) {
            try {
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().text(\"View Details\")")).click();
                if (cal.waitDetailSheet(Duration.ofSeconds(10))) label = cal.detailText("tvCreatorExcluded");
                cal.closeDetail();
            } catch (org.openqa.selenium.WebDriverException ignored) {
            }
        }
        screenshot("cal-excluded");
        cal.backToCalendar();
        assertTrue(listed, "The event should be listed under Created → Invites Sent");
        assertTrue(cardTag, "The Created card should carry the '- Excluded' tag (tvExcludedTag)");
        assertTrue(label.contains("Excluded"), "Detail should mark the organizer '- Excluded' (EventDetailBottomSheet.kt:386), got '" + label + "'");
    }

    @Test
    @Order(23)
    @DisplayName("[WEB CAL #113] Timezone search with no match shows 'No timezones match your search'")
    void timezoneNoMatch() {
        EventFormView form = formOn(TODAY.plusDays(2));
        assertTrue(form.timezoneSearchShowsEmpty("zzqa-no-zone"), "A no-match timezone search should show the empty text");
        form.goBack();
    }

    // ───────────────────────────── batch 2 (UPDATE 23:55): every remaining row ─────────────────────────────

    @Test
    @Order(30)
    @DisplayName("[WEB CAL #34 case C] External-guest-only 'Meet in Person' event: Call Line hidden, no Join Call, guest listed")
    void externalGuestOnlyMeetInPerson() {
        String t = title("ExtMIP");
        String guest = com.zillit.automation.core.TestEmails.recipient();
        LocalDate d = TODAY.plusDays(6);
        EventFormView form = formOn(d);
        form.selectMembersType();
        form.setTitle(t);
        assumeTrue(form.setStartTime(LocalTime.of(11, 0)), "Could not set Start 11:00 AM");
        assumeTrue("Meet in Person".equals(form.selectCallType("Meet in Person")), "Could not pick Meet in Person");
        assertTrue(!form.isCallLineShown(), "Meet in Person hides the Call Line (EventFormLogic.kt:55)");
        assumeTrue(form.openExternalGuests(), "External guests screen did not open");
        form.externalAdd(guest, false);
        assertEquals(List.of(guest), form.externalEmails(), "The guest should be listed once");
        form.externalDone();
        assertEquals("1 external guest", form.externalCountText(), "External guest count on the form");
        form.tapSave();
        boolean saved = form.waitFormClosed(Duration.ofSeconds(30));
        if (saved) CREATED.add(new Object[]{t, d});
        assertTrue(saved, "An external-guest-only Meet in Person event should save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        boolean join = cal.detailShows("btnJoinCall");
        String callType = cal.detailText("tvCallType");
        boolean dialog = false, listed = false;
        if (cal.detailShows("layoutInviteeSummary")) {
            driver.findElement(io.appium.java_client.AppiumBy.id(appPackage + ":id/layoutInviteeSummary")).click();
            dialog = cal.textShown("External Guests", Duration.ofSeconds(8));
            listed = cal.textShown(guest, Duration.ofSeconds(4));
            cal.closeDetail();
        }
        cal.closeDetail();
        assertEquals("Meet in Person", callType, "Detail call type");
        assertTrue(!join, "Meet in Person is not joinable — no Join Call (EventDetailBottomSheet.kt:391)");
        assertTrue(dialog && listed, "The Invitees dialog should show an 'External Guests' section with " + guest);
    }

    @Test
    @Order(31)
    @DisplayName("[WEB CAL #38] External guests: invalid not added, duplicate not added twice, IME Done adds, remove empties, form Clear empties")
    void externalGuestInput() {
        String guest = com.zillit.automation.core.TestEmails.recipient();
        EventFormView form = formOn(TODAY.plusDays(2));
        form.selectMembersType();
        assumeTrue(form.openExternalGuests(), "External guests screen did not open");
        form.externalAdd("not-an-email", false);
        assertTrue(form.externalEmails().isEmpty(), "An invalid address must not be added: " + form.externalEmails());
        form.externalAdd(guest, true);
        assertEquals(List.of(guest), form.externalEmails(), "The keyboard Done action should add the address (ExternalEmailFragment.kt:158-162)");
        form.externalAdd(guest, false);
        assertEquals(1, form.externalEmails().size(), "A duplicate must not be added twice: " + form.externalEmails());
        assertTrue(form.externalRemoveFirst(), "Remove (btnRemove) missing");
        assertTrue(form.externalEmails().isEmpty(), "Remove should empty the list: " + form.externalEmails());
        form.externalAdd(guest, false);
        form.externalDone();
        assertEquals("1 external guest", form.externalCountText(), "Form count after Done");
        assertTrue(form.tapFormClear("tvClearExternalEmails"), "Form Clear for guests missing");
        assertEquals("", form.externalCountText(), "Form Clear should remove the guest count row");
        form.goBack();
    }

    @Test
    @Order(32)
    @DisplayName("[WEB CAL #39, #115] Invitee picker: admin tabs, Select All / Deselect All, department pick fills invitees, form Clear empties (ANDROID BUG on the label)")
    void inviteePickerModes() {
        EventFormView form = formOn(TODAY.plusDays(2));
        form.selectMembersType();
        assumeTrue(form.openInviteePicker(), "Invitee picker did not open");
        List<String> tabs = form.pickerTabs();
        assertEquals(List.of("All Departments", "Select Department", "Select Users"), tabs,
                "Admin picker tabs (InviteePickerFragment.kt:250-270)");
        assumeTrue(form.pickerTab("All Departments"), "Could not open All Departments");
        assumeTrue(form.pickerTapSelectAll(), "Select All card missing");
        String doneAll = form.pickerDoneText();
        assertTrue(doneAll.matches("Done \\(\\d+\\)") && !doneAll.equals("Done (0)"), "Select All should count people on Done, got '" + doneAll + "'");
        // Checked at the END so the rest of the flow still runs: the card label after Select All.
        String labelAfterSelectAll = form.pickerSelectAllText();
        form.pickerTapSelectAll();   // second tap: isAllSelected() → deselectAll() (InviteePickerFragment.kt:174)
        assertEquals("Done", form.pickerDoneText(), "A second tap (Deselect All) should clear the selection");
        assumeTrue(form.pickerTab("Select Department"), "Could not open Select Department");
        assumeTrue(form.pickerTapFirstDepartment(), "No department row");
        String doneDept = form.pickerDoneText();
        form.pickerDone();
        String count = form.inviteeCountText();
        assertTrue(count.matches("\\d+ (person|people) invited"), "A department pick should fill invitees, got '" + count + "' (Done was '" + doneDept + "')");
        assertTrue(form.tapFormClear("tvClearInvitees"), "Form Clear for invitees missing");
        assertTrue(driver.findElements(io.appium.java_client.AppiumBy.id(appPackage + ":id/tvInviteeCount")).isEmpty(),
                "Clear should remove the invitee count row");
        form.goBack();
        // ANDROID BUG: the card's label is only rebuilt when displayItems/currentTab EMIT. After Select
        // All the VM calls setTab(0) → updateDisplayList() → _displayItems.value = getFilteredUsers(),
        // an EQUAL list, and _currentTab is already 0 — both StateFlows drop equal values, so the
        // AllDepartmentsAdapter is never re-created (InviteePickerFragment.kt:156-178,
        // InviteePickerViewModel.kt:274-278, 330-334). The selection itself works (Done (N)), but
        // the button keeps reading "Select All (N)" instead of "Deselect All" (and "0 selected").
        assertEquals("Deselect All", labelAfterSelectAll,
                "ANDROID BUG (web #39): after Select All the card still reads '" + labelAfterSelectAll
                        + "' — the label does not toggle to 'Deselect All' because the card is not re-rendered");
    }

    @Test
    @Order(33)
    @DisplayName("[WEB CAL #36, #59] Call Line 2 is kept on reopen; the edit picker hides the creator")
    void line2KeptAndPickerHidesCreator() {
        String t = title("Line2");
        LocalDate d = TODAY.plusDays(6);
        EventFormView form = formOn(d);
        form.selectMembersType();
        form.setTitle(t);
        assumeTrue(form.setStartTime(LocalTime.of(16, 0)), "Could not set Start 4:00 PM");
        assumeTrue(form.selectCallType("Video Call").contains("Video"), "Could not pick Video Call");
        assumeTrue(form.selectCallLine("Line 2"), "Call Line dialog did not offer Line 2");
        assumeTrue(form.addInvitee("Pixel", "Vidya Pixel"), "Could not add Vidya Pixel");
        form.tapSave();
        boolean saved = form.waitFormClosed(Duration.ofSeconds(30));
        if (saved) CREATED.add(new Object[]{t, d});
        assertTrue(saved, "The Line 2 event did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        String creator = cal.detailText("tvCreatedBy");
        cal.detailTapEdit();
        EventFormView edit = new EventFormView(driver);
        assertTrue(edit.isLoaded(), "Edit form did not open");
        assertEquals("Line 2", edit.callLineText(), "[#36] Call Line should reopen as Line 2 (EventFormViewModel.kt:179)");
        String me = creator.split("[-(,]")[0].trim();
        String[] parts = me.split("\\s+");
        String query = parts.length > 1 ? parts[parts.length - 1] : me;
        assumeTrue(!query.isBlank(), "Could not read the creator's name ('" + creator + "')");
        assumeTrue(edit.openInviteePicker(), "Invitee picker did not open");
        edit.pickerTab("Select Users");
        edit.pickerSearch("Pixel");
        List<String> control = edit.pickerNames();   // positive control: the search itself works
        edit.pickerSearch(query);
        List<String> names = edit.pickerNames();
        edit.goBack();
        edit.goBack();
        System.out.println(">>> creator='" + creator + "' search '" + query + "' → " + names);
        assertTrue(control.stream().anyMatch(n -> n.contains("Vidya Pixel")), "Picker search should find Vidya Pixel (control), got " + control);
        assertTrue(names.stream().noneMatch(n -> n.startsWith(me)), "[#59] The creator '" + me + "' must not be offered as an invitee: " + names);
    }

    @Test
    @Order(34)
    @DisplayName("[WEB CAL #61, #62] Edit and delete from the Created Events card: description saved; the card goes, the grid shows it cancelled")
    void createdCardEditDelete() {
        String t = title("Card");
        LocalDate d = TODAY.plusDays(3);
        assertTrue(createPersonal(t, d, LocalTime.of(17, 0), LocalTime.of(18, 0), null), "The event did not save");
        CalendarPage cal = calendar();
        assertTrue(cal.openEventsScreen("rowCreated"), "Created Events did not open");
        cal.selectListTab("Personal");
        cal.searchList(t);
        assertTrue(cal.waitCard(t, Duration.ofSeconds(20)), "Card not listed");
        assertTrue(cal.tapOnCard(t, "btnCardEdit"), "Card Edit (btnCardEdit) missing");
        EventFormView form = new EventFormView(driver);
        assertTrue(form.isLoaded(), "Card Edit did not open the form");
        form.setDescription("ZL_AUTO card edit " + RUN);
        form.tapSave();
        assertTrue(form.waitFormClosed(Duration.ofSeconds(30)), "Card edit did not save");
        cal.searchList(t);
        assertTrue(cal.waitCard(t, Duration.ofSeconds(20)), "Card gone after edit");
        assertTrue(cal.tapOnCard(t, "btnCardDelete"), "Card Delete (btnCardDelete) missing");
        cal.confirmDelete();
        sleep(2500);
        // CreatedEventsFragment.performDelete (CreatedEventsFragment.kt:302-325) removes the row
        // locally and filters it from later refetches — on Android the card goes away (web keeps a
        // struck-through "Cancelled:" card). The grid keeps the "Cancelled:" row (checked below).
        cal.searchList(t);
        sleep(2000);
        List<String> left = cal.cardTitles();
        screenshot("cal-card-deleted");
        cal.backToCalendar();
        CREATED.removeIf(c -> t.equals(c[0]));
        assertTrue(left.stream().noneMatch(x -> x.equals(t)), "After the card Delete no live card may remain; cards " + left);
        assertTrue(openEvent(d, t), "Could not open the cancelled event from the grid");
        String desc = cal.detailText("tvDescription");
        String status = cal.detailText("tvInvitationStatus");
        cal.closeDetail();
        assertEquals("ZL_AUTO card edit " + RUN, desc, "The card edit should have saved the description");
        assertEquals("This event has been cancelled", status, "[#62] Cancelled status bar");
    }

    @Test
    @Order(35)
    @DisplayName("[WEB CAL #75] Received/Created: Today filter and Select date reload the list; the button shows the picked date")
    void listTodayAndDateFilter() {
        CalendarPage cal = calendar();
        assertTrue(cal.openEventsScreen("rowCreated"), "Created Events did not open");
        cal.selectListTab("Personal");
        assertTrue(cal.tapListToday(), "Today checkbox (cbToday) missing");
        LocalDate d = TODAY.plusDays(3);
        boolean picked = cal.pickListDate(d);
        String btn = cal.listDateButtonText();
        cal.backToCalendar();
        assertTrue(picked, "Select date did not accept " + d);
        assertTrue(!btn.isBlank() && !"Select date".equals(btn), "The date button should show the picked date, got '" + btn + "'");
        assertTrue(btn.contains(String.valueOf(d.getDayOfMonth())), "The date button should show day " + d.getDayOfMonth() + ", got '" + btn + "'");
    }

    @Test
    @Order(36)
    @DisplayName("[WEB CAL #78] Created → Personal loads more cards on scroll (page size 20)")
    void createdListPaginates() {
        CalendarPage cal = calendar();
        assertTrue(cal.openEventsScreen("rowCreated"), "Created Events did not open");
        cal.selectListTab("Personal");
        sleep(2000);
        int firstPage = cal.cardTitles().size();
        int total = cal.countCardsScrolling(15);
        cal.backToCalendar();
        System.out.println(">>> Created/Personal cards: first screen " + firstPage + ", after scrolling " + total);
        assumeTrue(total >= 20, "Fewer than 20 personal events — nothing to page (have " + total + ")");
        assertTrue(total > 20, "Scrolling past the first page (20, Constants.kt:7) should load more cards, got " + total);
    }

    @Test
    @Order(37)
    @DisplayName("[WEB CAL #53] Start Date picker greys out past days")
    void startDatePickerGreysPast() {
        LocalDate y = TODAY.minusDays(1);
        assumeTrue(y.getMonth() == TODAY.getMonth(), "Yesterday is in the previous month — not on the picker's first page");
        EventFormView form = formOn(TODAY);
        Boolean enabled = form.startDatePickerDayEnabled(y);
        form.goBack();
        assumeTrue(enabled != null, "Yesterday's cell not found in the picker");
        assertTrue(!enabled, "Yesterday must be disabled in the Start Date picker (DateValidatorPointForward, EventFormFragment.kt:618-631)");
    }

    @Test
    @Order(38)
    @DisplayName("[WEB CAL #12] The view mode survives a Home unit round trip")
    void viewModeSurvivesUnitSwitch() {
        CalendarPage cal = calendar();
        assumeTrue(cal.toggleViewMode("Week"), "Could not switch to Week");
        com.zillit.automation.pages.homechat.HomeChatPage home = new com.zillit.automation.pages.homechat.HomeChatPage(driver);
        try {
            assumeTrue(home.selectUnit("Bulletin") || home.selectUnit("Call Sheet"), "No other Home unit to switch to");
            sleep(1500);
            assumeTrue(home.selectUnit("Calendar"), "Could not come back to Calendar");
            for (int i = 0; i < 2; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
            assertTrue(cal.isCalendarLoaded(), "Calendar did not reload");
            assertEquals("Week", cal.viewModeLabel(), "View mode after the round trip (Home.kt:349-353 reuses the fragment)");
        } finally {
            if (new CalendarPage(driver).isVisibleFab()) new CalendarPage(driver).toggleViewMode("Month");
        }
    }

    @Test
    @Order(39)
    @DisplayName("[WEB CAL #116, CAL-32] Double-tapping Create makes ONE event")
    void doubleTapCreateMakesOne() {
        String t = title("Dbl");
        LocalDate d = TODAY.plusDays(7);
        EventFormView form = formOn(d);
        form.selectPersonalType();
        form.setTitle(t);
        form.tapSave();
        try { form.tapSave(); } catch (org.openqa.selenium.WebDriverException ignored) { }
        boolean closed = form.waitFormClosed(Duration.ofSeconds(30));
        if (closed) CREATED.add(new Object[]{t, d});
        assertTrue(closed, "The event did not save");
        recoverToCalendar();
        sleep(1500);
        List<String> rows = rowsOn(d, t);
        assertEquals(1, rows.stream().filter(r -> r.equals(t)).count(), "Two Create taps must make exactly one event; rows " + rows);
    }

    @Test
    @Order(40)
    @DisplayName("[WEB CAL #40] Location: the map picker sets a location and the detail shows it as a link")
    void locationPicker() {
        String t = title("Loc");
        LocalDate d = TODAY.plusDays(7);
        final String[] picked = {""};
        boolean saved = createPersonal(t, d, LocalTime.of(9, 0), LocalTime.of(10, 0), f -> {
            f.tapLocationField();
            for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
            assumeTrue(f.confirmMapPicker(), "Map picker did not open / confirm (map tiles or location unavailable)");
            picked[0] = f.locationText();
        });
        assumeTrue(!picked[0].isBlank(), "The map picker returned no address");
        assertTrue(saved, "The event with a location did not save");
        CalendarPage cal = calendar();
        assertTrue(openEvent(d, t), "Could not open the event");
        String loc = cal.detailText("tvLocation");
        cal.closeDetail();
        assertEquals(picked[0], loc, "Detail location (EventDetailBottomSheet.kt:315-348)");
    }
}
