package com.zillit.automation.tests.calendar;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.calendar.CalendarPage;
import com.zillit.automation.pages.calendar.CalendarPage.EventFormView;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Calendar — invitations across TWO accounts (web gap M1, M5, M14: web #34, #10, #37, #11, #90,
 * #91, #92, #94, #96, #133, #134). The creator is BaseTest's own driver ({@code ZILLIT_UDID},
 * the emulator, Vidya Testing); the invitee is {@code ZILLIT_RECEIVER_UDID} (the Pixel, Vidya
 * Pixel). Only those two test accounts are ever invited.
 *
 * <p>Strings: "Pending invitation", "You accepted this event", "You declined this event"
 * (strings.xml:5519-5521); Accept/Decline Invitation sheet (AcceptRejectDialog, btnSubmit,
 * etReason, rbSingle/rbThisAndFuture/rbAll); decliner rows (DeclinedEventCardAdapter).
 *
 * <p>Run: {@code ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC
 * ./run-and-report.sh --only CalendarInviteTest}. TestOrder: CALENDAR_INVITE (202).
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.CALENDAR_INVITE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarInviteTest extends CalendarGapBase {

    private static final String INVITEE = "Vidya Pixel";
    private static final LocalDate D = TODAY.plusDays(2);
    private static final String E1 = title("Invite");
    private static final String E2 = title("InvRec");
    private static final String REASON = "ZL_AUTO reason " + RUN;

    private AndroidDriver creator, invitee;
    private boolean e1Created, e1Accepted, e1Declined, e2Created, e2Declined;

    @BeforeAll
    void secondDevice() {
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is required — the invitee (Vidya Pixel) needs its own device");
        creator = driver;
        invitee = DriverFactory.create(udidB, 8212);
    }

    @AfterAll
    void closeSecondDevice() {
        driver = creator;
        if (invitee != null) { try { invitee.quit(); } catch (Exception ignored) { } }
    }

    private <T> T on(AndroidDriver d, Supplier<T> body) {
        AndroidDriver keep = driver;
        driver = d;
        try { return body.get(); } finally { driver = keep; }
    }

    private void on(AndroidDriver d, Runnable body) { on(d, () -> { body.run(); return null; }); }

    /** Gets the invitee's app onto Home → Calendar (does not rely on BaseTest's landing). */
    private void inviteeCalendar() {
        try {
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
        } catch (WebDriverException ignored) { }
        CalendarPage cal = new CalendarPage(driver);
        if (cal.isVisibleFab()) return;
        BottomNavPage nav = new BottomNavPage(driver);
        for (int i = 0; i < 6 && !nav.waitUntilLoaded(Duration.ofSeconds(2)); i++) {
            if (!driver.findElements(AppiumBy.id(appPackage + ":id/fab")).isEmpty()) return;
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            if (!names.isEmpty()) { list.tapProject(names.get(0)); continue; }
            driver.navigate().back();
            sleep(800);
        }
        assumeTrue(nav.waitUntilLoaded(Duration.ofSeconds(30)), "Invitee device: bottom navigation not reachable");
        nav.goHome();
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.isLoaded(), "Invitee device: Home units strip not visible");
        assumeTrue(home.selectUnit("Calendar"), "Invitee device: no Calendar unit");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(600); }
        assumeTrue(cal.isCalendarLoaded(), "Invitee device: Calendar did not load");
    }

    /** Creator: Members event on {@code day} inviting Vidya Pixel. */
    private boolean createMembers(String t, LocalDate day, String recurrence, LocalDate until) {
        EventFormView form = formOn(day);
        form.selectMembersType();
        form.setTitle(t);
        assumeTrue(form.setStartTime(LocalTime.of(10, 0)), "Could not set Start 10:00 AM");
        if (recurrence != null) {
            assumeTrue(form.selectRecurrence(recurrence), "No " + recurrence + " repeat");
            assumeTrue(form.setRecurrenceEnd(until), "Could not set Repeat Until " + until);
        }
        assertEquals("Audio Call", form.callTypeText(), "Members default call type");
        assertEquals("Line 1", form.callLineText(), "Default call line");
        assertTrue(form.addInvitee("Pixel", INVITEE), "Could not invite " + INVITEE);
        assertEquals("1 person invited", form.inviteeCountText(), "Invitee count on the form (EventFormFragment.kt:517-520)");
        form.tapSave();
        boolean saved = form.waitFormClosed(Duration.ofSeconds(30));
        if (saved) CREATED.add(new Object[]{t, day});
        return saved;
    }

    // ───────────────────────────── creator ─────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[WEB CAL #34, #10, #37] Members Audio Call event with one invitee: detail shows Members, Audio Call, Join Call, 1 Invitee (Pending 1)")
    void creatorCreatesMembersEvent() {
        on(creator, () -> {
            e1Created = createMembers(E1, D, null, null);
            assertTrue(e1Created, "The Members event did not save");
            CalendarPage cal = calendar();
            assertTrue(openEvent(D, E1), "Could not open the event");
            assertEquals("Members", cal.detailText("tvEventType"), "Type chip");
            assertEquals("Audio Call", cal.detailText("tvCallType"), "Call type");
            assertTrue(cal.detailShows("btnJoinCall"), "Join Call should show for a future Audio Call event");
            assertTrue(!cal.detailShows("layoutBottomBar"), "The creator gets no RSVP bar (EventDetailBottomSheet.kt:473-476)");
            assertEquals("1 Invitee", cal.detailText("tvInviteeSummaryCount"), "Invitee summary");
            driver.findElement(AppiumBy.id(appPackage + ":id/layoutInviteeSummary")).click();
            assertTrue(cal.textShown("Invitees", Duration.ofSeconds(8)), "The Invitees dialog did not open");
            String pending = cal.detailText("tvPendingCount");
            boolean named = cal.textShown(INVITEE, Duration.ofSeconds(5));
            screenshot("cal-invitees-dialog");
            cal.closeDetail();
            cal.closeDetail();
            assertEquals("Pending 1", pending, "Invitees dialog pending chip (InviteeListDialog.kt:158)");
            assertTrue(named, "The Invitees dialog should list " + INVITEE);
        });
    }

    // ───────────────────────────── invitee ─────────────────────────────

    @Test
    @Order(2)
    @DisplayName("[WEB CAL #11, #90, #96] Invitee sees 'Pending invitation' with Accept/Decline (admin also gets Edit); Accept marks it accepted")
    void inviteeAccepts() {
        assumeTrue(e1Created, "No invite was created");
        on(invitee, () -> {
            inviteeCalendar();
            CalendarPage cal = new CalendarPage(driver);
            assertTrue(openEvent(D, E1), "The invite is not on the invitee's calendar for " + D);
            assertEquals("Pending invitation", cal.detailText("tvInvitationStatus"), "Invitee status");
            assertTrue(cal.detailShows("btnAccept") && cal.detailShows("btnReject"), "Accept and Decline should both show");
            boolean adminEdit = cal.detailShows("btnEdit");
            System.out.println(">>> invitee (project admin) sees Edit on the creator's event: " + adminEdit);
            assertTrue(adminEdit, "[#96] Vidya Pixel is a project admin — admins may edit any event (EventDetailBottomSheet.kt:240)");
            driver.findElement(AppiumBy.id(appPackage + ":id/btnAccept")).click();
            assertTrue(cal.textShown("Accept Invitation", Duration.ofSeconds(8)), "Accept sheet title");
            assertTrue(cal.textShown("Are you sure you want to accept this invitation?", Duration.ofSeconds(3)), "Accept confirm text");
            driver.findElement(AppiumBy.id(appPackage + ":id/btnSubmit")).click();
            sleep(3000);
            cal.closeDetail();
            assertTrue(openEvent(D, E1), "Could not reopen the invite");
            String status = cal.detailText("tvInvitationStatus");
            boolean accept = cal.detailShows("btnAccept"), reject = cal.detailShows("btnReject");
            cal.closeDetail();
            assertEquals("You accepted this event", status, "Status after Accept");
            assertTrue(!accept && reject, "After Accept only Decline remains: accept=" + accept + " reject=" + reject);
            e1Accepted = true;
        });
    }

    @Test
    @Order(3)
    @DisplayName("[WEB CAL #91, #133] Invitee declines with a reason: 'You declined this event', no Join Call, listed in Declined → Received")
    void inviteeDeclinesWithReason() {
        assumeTrue(e1Created, "No invite was created");
        on(invitee, () -> {
            inviteeCalendar();
            CalendarPage cal = new CalendarPage(driver);
            assertTrue(openEvent(D, E1), "Could not open the invite");
            driver.findElement(AppiumBy.id(appPackage + ":id/btnReject")).click();
            assertTrue(cal.textShown("Decline Invitation", Duration.ofSeconds(8)), "Decline sheet title");
            WebElement reason = driver.findElement(AppiumBy.id(appPackage + ":id/etReason"));
            reason.click();
            reason.sendKeys(REASON);
            driver.findElement(AppiumBy.id(appPackage + ":id/btnSubmit")).click();
            sleep(3000);
            cal.closeDetail();
            e1Declined = true;
            List<String> rows = rowsOn(D, E1);
            System.out.println(">>> invitee agenda after decline: " + rows);
            assertTrue(!hasLive(rows, E1), "[CAL-29] The declined event must not stay as a live row; rows " + rows);

            assertTrue(cal.openEventsScreen("rowDeclined"), "Declined Events did not open");
            cal.selectListTab("Received");
            cal.searchList(E1);
            boolean listed = cal.textShown(E1, Duration.ofSeconds(20));
            screenshot("cal-invitee-declined-list");
            assertTrue(listed, "The declined invite is not in Declined Events → Received");
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"View Details\")")).click();
            assertTrue(cal.waitDetailSheet(Duration.ofSeconds(10)), "View Details did not open the event");
            String status = cal.detailText("tvInvitationStatus");
            boolean join = cal.detailShows("btnJoinCall");
            cal.closeDetail();
            cal.backToCalendar();
            assertEquals("You declined this event", status, "Status after Decline");
            assertTrue(!join, "[CAL-34] Join Call must be hidden after declining (EventDetailBottomSheet.kt:388-395)");
        });
    }

    @Test
    @Order(4)
    @DisplayName("[WEB CAL #92, CAL-06] Organizer sees the decliner by name with the saved reason under Declined → Created")
    void creatorSeesDecliner() {
        assumeTrue(e1Declined, "The invitee did not decline");
        on(creator, () -> {
            CalendarPage cal = calendar();
            assertTrue(cal.openEventsScreen("rowDeclined"), "Declined Events did not open");
            cal.selectListTab("Created");
            cal.searchList(E1);
            assertTrue(cal.textShown(E1, Duration.ofSeconds(20)), "The declined event is not in Declined → Created");
            assertTrue(cal.textShown("Show decliners (1)", Duration.ofSeconds(5)), "Toggle should read 'Show decliners (1)'");
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().textStartsWith(\"Show decliners\")")).click();
            boolean name = cal.textShown(INVITEE, Duration.ofSeconds(8));
            boolean reason = cal.textShown(REASON, Duration.ofSeconds(3));
            boolean request = cal.textShown("Request Reason", Duration.ofSeconds(2));
            screenshot("cal-creator-decliners");
            cal.backToCalendar();
            assertTrue(name, "[CAL-06] The decliner row should name " + INVITEE);
            assertTrue(reason, "The decliner row should quote the saved reason '" + REASON + "'");
            assertTrue(!request, "[CAL-06] 'Request Reason' must not be offered when a reason was saved");
        });
    }

    @Test
    @Order(5)
    @DisplayName("[WEB CAL #94, #134] Recurring invite: decline 'This and following' from day 2 leaves day 1 pending")
    void scopedDeclineOnRecurringInvite() {
        on(creator, () -> e2Created = createMembers(E2, D.plusDays(1), "Daily", D.plusDays(3)));
        assumeTrue(e2Created, "The recurring invite did not save");
        on(invitee, () -> {
            inviteeCalendar();
            CalendarPage cal = new CalendarPage(driver);
            assertTrue(openEvent(D.plusDays(2), E2), "The 2nd occurrence is not on the invitee's calendar");
            driver.findElement(AppiumBy.id(appPackage + ":id/btnReject")).click();
            assertTrue(cal.textShown("APPLY TO", Duration.ofSeconds(8)), "A recurring decline should offer the scope (APPLY TO)");
            driver.findElement(AppiumBy.id(appPackage + ":id/rbThisAndFuture")).click();
            driver.findElement(AppiumBy.id(appPackage + ":id/btnSubmit")).click();   // no reason: the organizer can request one
            sleep(3000);
            cal.closeDetail();
            e2Declined = true;
            assertTrue(openEvent(D.plusDays(1), E2), "The 1st occurrence is gone from the invitee's calendar");
            String first = cal.detailText("tvInvitationStatus");
            cal.closeDetail();
            assertEquals("Pending invitation", first, "The 1st occurrence must stay pending");
            List<String> third = rowsOn(D.plusDays(3), E2);
            assertTrue(!hasLive(third, E2), "The 3rd occurrence should be declined; rows " + third);
        });
    }

    @Test
    @Order(6)
    @DisplayName("[WEB CAL #92] Request Reason: Send Request stays disabled for an empty or blank message (not sent)")
    void requestReasonNeedsText() {
        assumeTrue(e2Declined, "The recurring invite was not declined");
        on(creator, () -> {
            CalendarPage cal = calendar();
            assertTrue(cal.openEventsScreen("rowDeclined"), "Declined Events did not open");
            cal.selectListTab("Created");
            cal.searchList(E2);
            assertTrue(cal.textShown(E2, Duration.ofSeconds(20)), "The declined series is not in Declined → Created");
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().textStartsWith(\"Show decliners\")")).click();
            assertTrue(cal.textShown("Request Reason", Duration.ofSeconds(8)), "A decline without a reason should offer 'Request Reason'");
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"Request Reason\")")).click();
            assertTrue(cal.textShown("Request decline reason", Duration.ofSeconds(8)), "Request Reason dialog did not open");
            WebElement send = driver.findElement(AppiumBy.id(appPackage + ":id/btnSendRequest"));
            boolean emptyDisabled = !send.isEnabled();
            WebElement msg = driver.findElement(AppiumBy.id(appPackage + ":id/etMessage"));
            msg.click();
            msg.sendKeys("   ");
            boolean blankDisabled = !driver.findElement(AppiumBy.id(appPackage + ":id/btnSendRequest")).isEnabled();
            driver.findElement(AppiumBy.id(appPackage + ":id/btnCancel")).click();   // never send
            sleep(600);
            cal.backToCalendar();
            assertTrue(emptyDisabled, "Send Request must be disabled while the message is empty (RequestReasonDialog.kt:141-142)");
            assertTrue(blankDisabled, "Send Request must stay disabled for a spaces-only message");
        });
    }

    // ───────────────────────────── batch 2: badge, card accept, live updates ─────────────────────────────

    private static final String E3 = title("InvCard");
    private boolean e3Created, e3Accepted;
    private String badgeBefore = "";

    private static int num(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D", "")); } catch (NumberFormatException e) { return 0; }
    }

    @Test
    @Order(7)
    @DisplayName("[WEB CAL #137, CAL-21] The invitee's Events badge goes up with a new invite")
    void badgeGoesUpWithInvite() {
        on(invitee, () -> { inviteeCalendar(); badgeBefore = new CalendarPage(driver).eventsBadge(); });
        on(creator, () -> e3Created = createMembers(E3, D.plusDays(4), null, null));
        assumeTrue(e3Created, "The third invite did not save");
        on(invitee, () -> {
            inviteeCalendar();
            CalendarPage cal = new CalendarPage(driver);
            cal.pullToRefresh();
            String after = cal.eventsBadge();
            for (int i = 0; i < 10 && num(after) <= num(badgeBefore); i++) { sleep(2000); after = cal.eventsBadge(); }
            System.out.println(">>> invitee Events badge: before='" + badgeBefore + "' after='" + after + "'");
            assertTrue(num(after) > num(badgeBefore), "The Events badge should go up after a new invite: '" + badgeBefore + "' → '" + after + "'");
        });
    }

    @Test
    @Order(8)
    @DisplayName("[WEB CAL #93, #137] Card-level Accept in Received → Pending moves it out of Pending and the badge goes down")
    void cardAcceptLeavesPending() {
        assumeTrue(e3Created, "The third invite did not save");
        on(invitee, () -> {
            inviteeCalendar();
            CalendarPage cal = new CalendarPage(driver);
            String before = cal.eventsBadge();
            assertTrue(cal.openEventsScreen("rowReceived"), "Received Events did not open");
            cal.selectListTab("Pending");
            cal.searchList(E3);
            assertTrue(cal.waitCard(E3, Duration.ofSeconds(20)), "The invite is not under Received → Pending: " + cal.cardTitles());
            assertTrue(cal.tapOnCard(E3, "btnCardAccept"), "Card Accept (btnCardAccept) missing");
            if (cal.textShown("Accept", Duration.ofSeconds(4))) {
                try { driver.findElement(AppiumBy.id(appPackage + ":id/btnSubmit")).click(); } catch (WebDriverException ignored) { }
            }
            sleep(3000);
            cal.searchList(E3);
            boolean stillPending = cal.waitCard(E3, Duration.ofSeconds(5));
            cal.selectListTab("Accepted");
            cal.searchList(E3);
            boolean accepted = cal.waitCard(E3, Duration.ofSeconds(15));
            cal.backToCalendar();
            String after = new CalendarPage(driver).eventsBadge();
            e3Accepted = accepted;
            assertTrue(!stillPending, "After card Accept the invite must leave Pending");
            assertTrue(accepted, "After card Accept the invite should be under Accepted");
            System.out.println(">>> badge before/after card accept: '" + before + "' → '" + after + "'");
            assertTrue(num(after) < num(before) || (before.isBlank() && after.isBlank()),
                    "[CAL-21] The Events badge should go down after accepting: '" + before + "' → '" + after + "'");
        });
    }

    @Test
    @Order(9)
    @DisplayName("[WEB CAL #95] The organizer renames the event; the invitee sees the new title")
    void organizerEditReachesInvitee() {
        assumeTrue(e3Created, "The third invite did not save");
        String renamed = E3 + " RENAMED";
        on(creator, () -> {
            CalendarPage cal = calendar();
            assertTrue(openEvent(D.plusDays(4), E3), "Organizer could not open the invite");
            cal.detailTapEdit();
            EventFormView f = new EventFormView(driver);
            assertTrue(f.isLoaded(), "Edit form did not open");
            f.setTitle(renamed);
            f.tapSave();
            assertTrue(f.waitFormClosed(Duration.ofSeconds(30)), "Organizer edit did not save");
            CREATED.removeIf(c -> E3.equals(c[0]));
            CREATED.add(new Object[]{renamed, D.plusDays(4)});
        });
        on(invitee, () -> {
            inviteeCalendar();
            CalendarPage cal = new CalendarPage(driver);
            cal.selectDay(D.plusDays(4));
            cal.pullToRefresh();
            assertTrue(cal.waitAgendaRow(renamed, Duration.ofSeconds(30)), "The invitee should see the renamed title; rows " + cal.agendaRowsFor(E3));
        });
    }

    @Test
    @Order(10)
    @DisplayName("[WEB CAL #135] Organizer cancels while the invitee's detail is open: the open sheet shows it cancelled (likely fails like web)")
    void cancelWhileInviteeDetailOpen() {
        assumeTrue(e3Created, "The third invite did not save");
        String t = E3 + " RENAMED";
        on(invitee, () -> {
            inviteeCalendar();
            assertTrue(openEvent(D.plusDays(4), t), "Invitee could not open the event");
        });
        on(creator, () -> {
            CalendarPage cal = calendar();
            assertTrue(openEvent(D.plusDays(4), t), "Organizer could not open the event");
            cal.detailTapDelete();
            cal.confirmDelete();
            sleep(3000);
            CREATED.removeIf(c -> t.equals(c[0]));
        });
        on(invitee, () -> {
            CalendarPage cal = new CalendarPage(driver);
            sleep(5000);
            String status = cal.detailText("tvInvitationStatus");
            boolean accept = cal.detailShows("btnAccept") || cal.detailShows("btnReject");
            cal.closeDetail();
            assertTrue("This event has been cancelled".equals(status) && !accept,
                    "Web #135: the open detail should switch to 'This event has been cancelled' with no RSVP buttons; it still shows '"
                            + status + "' (accept/decline shown=" + accept + "). The sheet is not refreshed by RefreshBus/socket.");
        });
    }
}
