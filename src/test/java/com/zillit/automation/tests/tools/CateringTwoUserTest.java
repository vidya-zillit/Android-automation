package com.zillit.automation.tests.tools;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import com.zillit.automation.pages.tools.CateringPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Catering between a CREW member and the CATERER, live on two devices (web gap groups
 * M28 realtime and M29 unread badges, plus the cross-device form of rules 2–5).
 *
 * <p>Roles on the automation project since 2026-11-23: device A ({@code ZILLIT_UDID},
 * an emulator, Vidya Testing) is crew; device B ({@code ZILLIT_RECEIVER_UDID}, the Pixel,
 * Vidya Pixel) is in the Catering department. The suite reads the role from the screen
 * (the caterer has the {@code addMember} recipient chip) and skips if they are swapped.
 *
 * <p>Rules from CateringActivity / TextVH: a crew post goes to the caterers only ("To : All
 * Caterer"); a caterer posts to "All Users" or to one picked user; a caterer-authored
 * message offers no Reply; the caterer may reply to crew posts. Every marker is ZL_AUTO
 * prefixed; the messages the caterer posts are deleted again where the case allows.
 *
 * <p>Run: {@code ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC
 * ./run-and-report.sh --only CateringTwoUserTest}.
 */
@Order(TestOrder.CATERING_TWO_USER)
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CateringTwoUserTest {

    private static final Duration LIVE = Duration.ofSeconds(30);

    private AndroidDriver crewDriver, catererDriver;
    private String pkg;
    private boolean ready;

    @BeforeAll
    void setUp() {
        String b = firstNonBlank(Config.get("zillit.receiver.udid"), System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(b != null, "Needs ZILLIT_RECEIVER_UDID (the caterer's device) — skipping");
        String a = firstNonBlank(Config.get("zillit.sender.udid"), System.getenv("ZILLIT_SENDER_UDID"),
                Config.get("zillit.udid"), System.getenv("ZILLIT_UDID"), "emulator-5554");
        // 8280/8290: TwoUserChatTest uses 8260/8270, single-device runs take 8200 upwards.
        crewDriver = DriverFactory.create(a, 8280);
        catererDriver = DriverFactory.create(b, 8290);
        pkg = crewDriver.getCurrentPackage();
        boolean inA = enterCatering(crewDriver);
        boolean inB = enterCatering(catererDriver);
        boolean aCaterer = new CateringPage(crewDriver).isCatererMode();
        boolean bCaterer = new CateringPage(catererDriver).isCatererMode();
        System.out.println(">>> two-device Catering: A(" + a + ") open=" + inA + " caterer=" + aCaterer
                + " | B(" + b + ") open=" + inB + " caterer=" + bCaterer);
        ready = inA && inB && !aCaterer && bCaterer;
        if (inA && inB && aCaterer && !bCaterer) {
            System.out.println(">>> roles are swapped (A is the caterer) — the cases will skip");
        }
    }

    @AfterAll
    void tearDown() {
        quit(crewDriver);
        quit(catererDriver);
    }

    private HomeChatPage chat(AndroidDriver d) { return new HomeChatPage(d); }
    private CateringPage cat(AndroidDriver d) { return new CateringPage(d); }

    private void requireReady() {
        assumeTrue(ready, "Needs Catering open on both devices with A = crew and B = caterer");
    }

    /** Both devices on the given meal; the caterer broadcasting to All Users. */
    private void bothOn(String meal) {
        for (AndroidDriver d : List.of(crewDriver, catererDriver)) {
            CateringPage c = cat(d);
            if (!c.isOnCatering()) assumeTrue(c.openCateringFromTools(), "Catering did not reopen");
            assumeTrue(chat(d).selectUnit(meal), "Could not select " + meal);
            sleep(1200);
        }
        CateringPage caterer = cat(catererDriver);
        if (!"All Users".equalsIgnoreCase(caterer.selectedRecipient())
                && caterer.openRecipientPicker() && caterer.isRecipientDialogOpen(Duration.ofSeconds(6))) {
            caterer.chooseAllUsers();
        }
        assumeTrue(chat(catererDriver).isInputBarVisible(), "The caterer has no composer after choosing All Users");
        assumeTrue(chat(crewDriver).isInputBarVisible(), "The crew member has no composer");
    }

    private String post(AndroidDriver d, String kind) {
        String m = "ZL_AUTO-cat2-" + kind + "-" + (System.currentTimeMillis() % 1000000);
        HomeChatPage c = chat(d);
        c.sendMessage(m);
        return c.waitMessageSent(m, Duration.ofSeconds(30)) ? m : null;
    }

    private boolean waitText(AndroidDriver d, String text, Duration t) {
        HomeChatPage c = chat(d);
        long deadline = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (c.messageShown(text, Duration.ofMillis(800))) return true;
        }
        return false;
    }

    private boolean waitGone(AndroidDriver d, String text, Duration t) {
        HomeChatPage c = chat(d);
        long deadline = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!c.messageShown(text, Duration.ofMillis(800))) return true;
            sleep(700);
        }
        return false;
    }

    private void deleteOwn(AndroidDriver d, String text) {
        try {
            HomeChatPage c = chat(d);
            if (!c.longPressMessage(text) || !c.tapMenuOption("Delete")) { c.leaveOverlayScreen(); return; }
            c.tapDeleteActionBar(Duration.ofSeconds(6));
            c.confirmPopup().acceptIfShown(Duration.ofSeconds(8));
            sleep(1500);
        } catch (WebDriverException ignored) { }
    }

    // ---------------- M28 realtime ----------------

    @Test
    @Order(1)
    @DisplayName("M28 [WEB CAT-677] The caterer's new, edited and deleted message reach the crew member's open Catering live")
    void catererPostEditDeleteLive() {
        requireReady();
        bothOn("Breakfast");
        String m = post(catererDriver, "live");
        assertTrue(m != null, "The caterer's broadcast should send");
        boolean gone = false;
        try {
            assertTrue(waitText(crewDriver, m, LIVE),
                    "The crew member should see the caterer's broadcast '" + m + "' live, without reopening");
            HomeChatPage cc = chat(catererDriver);
            assumeTrue(cc.longPressMessage(m) && cc.tapMenuOption("Edit"), "Edit not offered to the caterer");
            assumeTrue(cc.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
            String edited = m + "-EDITED";
            cc.setEditDialogText(edited);
            cc.saveEditDialog();
            assertTrue(cc.waitMessageSent(edited, Duration.ofSeconds(30)), "The caterer's edit should save");
            assertTrue(waitText(crewDriver, edited, LIVE), "The edit should reach the crew member live");
            deleteOwn(catererDriver, edited);
            gone = waitGone(catererDriver, edited, Duration.ofSeconds(8));
            assertTrue(gone, "The caterer's delete should remove the message on the caterer's side");
            assertTrue(waitGone(crewDriver, edited, LIVE), "The delete should reach the crew member live");
        } finally {
            if (!gone) { deleteOwn(catererDriver, m + "-EDITED"); deleteOwn(catererDriver, m); }
        }
    }

    @Test
    @Order(2)
    @DisplayName("M28 [WEB CAT-632/683] A crew post reaches the caterer live ('To : All Caterer'), and the caterer's reply reaches the crew member live")
    void crewPostAndCatererReplyLive() {
        requireReady();
        bothOn("Breakfast");
        String m = post(crewDriver, "ask");
        assertTrue(m != null, "The crew member's message should send");
        assertTrue(waitText(catererDriver, m, LIVE), "The caterer should see the crew post '" + m + "' live");
        CateringPage.Row row = cat(catererDriver).visibleRows().stream()
                .filter(r -> r.text.contains(m)).findFirst().orElse(null);
        System.out.println(">>> crew post on the caterer's screen: " + row);
        assertTrue(row != null && row.audience.contains("All Caterer"),
                "A crew post should be labelled 'To : All Caterer' on the caterer's screen: " + row);
        HomeChatPage cc = chat(catererDriver);
        assumeTrue(cc.longPressMessage(m), "Caterer could not long-press the crew post");
        List<String> opts = cc.menuOptions();
        assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase("Reply")),
                "RULE 4: the caterer should be offered Reply on a crew post. Menu: " + opts);
        assertTrue(cc.tapMenuOption("Reply"), "Reply should be tappable");
        assumeTrue(cc.isReplyPreviewVisible(), "The reply preview did not open");
        if (!cc.isInputBarVisible()) cat(catererDriver).ensurePostable();
        String reply = m + "-r";
        cc.sendMessage(reply);
        assertTrue(cc.waitMessageSent(reply, Duration.ofSeconds(30)), "The caterer's reply should send");
        assertTrue(waitText(crewDriver, reply, LIVE), "The caterer's reply should reach the crew member live");
    }

    // ---------------- rules 3 and 5 across devices ----------------

    @Test
    @Order(3)
    @DisplayName("RULE 3/5 [WEB CAT-626/646/665] The crew member sees the caterer's broadcast as 'To : All Users' and gets no Reply on it")
    void crewSeesBroadcastWithoutReply() {
        requireReady();
        bothOn("Breakfast");
        String m = post(catererDriver, "bcast");
        assertTrue(m != null, "The caterer's broadcast should send");
        try {
            assertTrue(waitText(crewDriver, m, LIVE), "The crew member should receive the broadcast");
            CateringPage.Row row = cat(crewDriver).visibleRows().stream()
                    .filter(r -> r.text.contains(m)).findFirst().orElse(null);
            System.out.println(">>> broadcast on the crew screen: " + row);
            assertTrue(row != null && row.audience.contains("All Users"),
                    "The caterer's broadcast should read 'To : All Users' for the crew member: " + row);
            HomeChatPage ac = chat(crewDriver);
            assumeTrue(ac.longPressMessage(m), "Crew could not long-press the broadcast");
            List<String> opts = ac.menuOptions();
            ac.closeMenu();
            System.out.println(">>> crew menu on a caterer message: " + opts);
            assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Reply")),
                    "RULE 3: Reply must not be offered on a caterer-authored message. Menu: " + opts);
            assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Edit")),
                    "The crew member must not be offered Edit on the caterer's message. Menu: " + opts);
        } finally {
            deleteOwn(catererDriver, m);
        }
    }

    @Test
    @Order(4)
    @DisplayName("M28 [WEB CAT-622] A caterer message directed to one user reaches that user")
    void directedMessageReachesUser() {
        requireReady();
        bothOn("Breakfast");
        CateringPage caterer = cat(catererDriver);
        assumeTrue(caterer.openRecipientPicker() && caterer.isRecipientDialogOpen(Duration.ofSeconds(6)),
                "Recipient dialog did not open");
        assumeTrue(caterer.chooseSelectUser(), "Could not choose Select User");
        String picked = caterer.pickFirstUserFromList();
        assumeTrue(!picked.isBlank(), "No user to pick");
        String m = null;
        try {
            assumeTrue(chat(catererDriver).isInputBarVisible(), "No composer after picking " + picked);
            m = post(catererDriver, "direct");
            assertTrue(m != null, "The directed message should send");
            final String sent = m;
            String label = cat(catererDriver).visibleRows().stream().filter(r -> r.text.contains(sent))
                    .map(r -> r.audience).findFirst().orElse("");
            System.out.println(">>> directed to '" + picked + "', caterer-side label '" + label + "'");
            assertTrue(label.contains(picked), "The directed message should be labelled 'To : " + picked + "': '" + label + "'");
            assertTrue(waitText(crewDriver, m, LIVE),
                    "The picked user (" + picked + ", the crew device) should receive the directed message");
        } finally {
            if (m != null) deleteOwn(catererDriver, m);
            if (caterer.openRecipientPicker() && caterer.isRecipientDialogOpen(Duration.ofSeconds(6))) caterer.chooseAllUsers();
        }
    }

    // ---------------- M29 unread badge ----------------

    @Test
    @Order(5)
    @DisplayName("M29 [WEB CAT-689] A caterer post in Lunch badges the crew member's Lunch tab; opening it shows 'New Messages' and clears the badge")
    void lunchBadgeAndNewMessages() {
        requireReady();
        bothOn("Breakfast");
        assumeTrue(chat(catererDriver).selectUnit("Lunch"), "Caterer could not select Lunch");
        sleep(1200);
        cat(catererDriver).ensurePostable();
        String before = cat(crewDriver).mealTabBadge("Lunch");
        String m = post(catererDriver, "badge");
        assertTrue(m != null, "The caterer's Lunch post should send");
        try {
            String badge = "";
            long deadline = System.currentTimeMillis() + LIVE.toMillis();
            while (System.currentTimeMillis() < deadline) {
                badge = cat(crewDriver).mealTabBadge("Lunch");
                if (!badge.isBlank() && !badge.equals(before)) break;
                sleep(1000);
            }
            System.out.println(">>> crew Lunch badge before='" + before + "' after='" + badge + "'");
            assertTrue(!badge.isBlank(), "The crew member's Lunch tab should show an unread badge after the caterer posts there");
            assertTrue(chat(crewDriver).selectUnit("Lunch"), "Crew could not open Lunch");
            sleep(2000);
            boolean header = chat(crewDriver).textShown("New Messages", Duration.ofSeconds(6));
            assertTrue(waitText(crewDriver, m, Duration.ofSeconds(10)), "The caterer's Lunch post should be listed");
            sleep(1500);
            String afterOpen = cat(crewDriver).mealTabBadge("Lunch");
            System.out.println(">>> New Messages header=" + header + " badge after opening='" + afterOpen + "'");
            assertTrue(header, "Opening Lunch should show the 'New Messages' header above the unread post");
            assertTrue(afterOpen.isBlank(), "Opening Lunch should clear its badge (still '" + afterOpen + "')");
        } finally {
            deleteOwn(catererDriver, m);
            chat(catererDriver).selectUnit("Breakfast");
            chat(crewDriver).selectUnit("Breakfast");
        }
    }

    // ---------------- M24 delete rights across roles ----------------

    @Test
    @Order(6)
    @DisplayName("M24 [WEB CAT-628/724] In delete mode the crew member cannot select the caterer's message")
    void crewCannotSelectCatererMessageForDelete() {
        requireReady();
        bothOn("Breakfast");
        String theirs = post(catererDriver, "theirs");
        assertTrue(theirs != null, "The caterer's message should send");
        String mine = post(crewDriver, "mine");
        assertTrue(mine != null, "The crew message should send");
        CateringPage a = cat(crewDriver);
        HomeChatPage ac = chat(crewDriver);
        try {
            assertTrue(waitText(crewDriver, theirs, LIVE), "The crew member should see the caterer's message");
            assumeTrue(ac.longPressMessage(mine) && ac.tapMenuOption("Delete"), "Delete not offered on the own message");
            assertTrue(a.selectionBarShown(), "Delete should open the selection bar");
            a.tapRow(theirs);
            boolean refused = a.snackShown("no permission to delete other user message", Duration.ofSeconds(4));
            String count = a.selectionCount();
            System.out.println(">>> crew tapped the caterer's row in delete mode: refused=" + refused + " count=" + count);
            assertTrue("1".equals(count), "The caterer's message must not join the crew member's delete selection (count "
                    + count + "). A project-admin crew account is allowed to by CateringActivity — check the role.");
            assertTrue(refused, "The refusal should say 'You have no permission to delete other user message.'");
        } finally {
            a.leaveSelection();
            deleteOwn(catererDriver, theirs);
        }
    }

    // ---------------- M28 units live ----------------

    @Test
    @Order(7)
    @DisplayName("M28 [WEB CAT-706] A unit the caterer creates appears on the crew member's open tabs live, and goes when deleted")
    void unitCreateDeleteLive() {
        requireReady();
        bothOn("Breakfast");
        CateringPage b = cat(catererDriver);
        String unit = "ZL_AUTO Unit " + (System.currentTimeMillis() % 100000);
        assumeTrue(b.openCreateUnit() && b.isCreateUnitScreenOpen(Duration.ofSeconds(10)), "Create-unit screen did not open");
        b.setUnitName(unit);
        b.submitUnit();
        boolean created = false;
        try {
            sleep(2500);
            created = chat(catererDriver).visibleUnitNames().stream().anyMatch(u -> u.equalsIgnoreCase(unit));
            assertTrue(created, "The caterer should see the new unit '" + unit + "'");
            boolean live = waitTab(crewDriver, unit, true);
            System.out.println(">>> crew sees new unit live=" + live);
            assertTrue(live, "The crew member's open Catering should gain the '" + unit + "' tab live");
            assertTrue(b.openCateringSettings() && b.deleteUnitInSettings(unit), "The caterer should delete the unit in Catering Settings");
            created = false;
            sleep(3000);
            b.leaveCateringSettings();
            b.openCateringFromTools();
            boolean gone = waitTab(crewDriver, unit, false);
            assertTrue(gone, "The deleted unit should leave the crew member's tabs live");
        } finally {
            if (created && b.openCateringSettings()) { b.deleteUnitInSettings(unit); sleep(3000); b.leaveCateringSettings(); }
            if (!b.isOnCatering()) b.openCateringFromTools();
        }
    }

    private boolean waitTab(AndroidDriver d, String unit, boolean present) {
        long deadline = System.currentTimeMillis() + LIVE.toMillis();
        while (System.currentTimeMillis() < deadline) {
            boolean has = chat(d).visibleUnitNames().stream().anyMatch(u -> u.equalsIgnoreCase(unit));
            if (has == present) return true;
            sleep(1500);
        }
        return false;
    }

    // ---------------- M29 tile badge ----------------

    @Test
    @Order(8)
    @DisplayName("M29 [WEB CAT-695] A caterer post raises the crew member's Catering tile badge on the Tools tab")
    void toolsTileBadge() {
        requireReady();
        bothOn("Breakfast");
        CateringPage a = cat(crewDriver);
        assumeTrue(a.showToolsRow(), "Crew could not show the Catering row on the Tools tab");
        String before = a.toolsTileBadge();
        String m = post(catererDriver, "tile");
        assertTrue(m != null, "The caterer's post should send");
        try {
            String badge = "";
            long deadline = System.currentTimeMillis() + LIVE.toMillis();
            while (System.currentTimeMillis() < deadline) {
                badge = a.toolsTileBadge();
                if (!badge.isBlank() && !badge.equals(before)) break;
                sleep(1500);
            }
            System.out.println(">>> Catering tile badge '" + before + "' → '" + badge + "'");
            assertTrue(!badge.isBlank() && !badge.equals(before), "The Catering tile should show (or raise) an unread badge");
        } finally {
            deleteOwn(catererDriver, m);
            a.openCateringFromTools();
        }
    }

    // ---------------- M30 notification ----------------

    @Test
    @Order(9)
    @DisplayName("M30 [WEB CAT-701] The notification for a caterer's Lunch post opens Catering on Lunch")
    void notificationOpensUnit() {
        requireReady();
        bothOn("Breakfast");
        assumeTrue(chat(catererDriver).selectUnit("Lunch"), "Caterer could not select Lunch");
        sleep(1200);
        cat(catererDriver).ensurePostable();
        try { crewDriver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.HOME)); } catch (WebDriverException ignored) { }
        sleep(2000);
        String m = post(catererDriver, "notif");
        assertTrue(m != null, "The caterer's Lunch post should send");
        boolean found = false, opened = false;
        try {
            crewDriver.openNotifications();
            long deadline = System.currentTimeMillis() + LIVE.toMillis();
            while (System.currentTimeMillis() < deadline && !found) {
                var n = crewDriver.findElements(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + m + "\")"));
                if (!n.isEmpty()) { found = true; n.get(0).click(); }
                else sleep(1500);
            }
            System.out.println(">>> crew notification for '" + m + "' found=" + found);
            if (found) {
                sleep(4000);
                opened = waitText(crewDriver, m, Duration.ofSeconds(15));
            } else {
                crewDriver.navigate().back();
            }
        } finally {
            try { crewDriver.executeScript("mobile: activateApp", Map.of("appId", pkg)); } catch (WebDriverException ignored) { }
            deleteOwn(catererDriver, m);
            chat(catererDriver).selectUnit("Breakfast");
        }
        assertTrue(found, "The crew device should get a notification carrying the caterer's message");
        assertTrue(opened, "Tapping the notification should open Catering on the Lunch post");
    }

    // ---------------- M25 rights (changed on the caterer device, restored) ----------------

    @Test
    @Order(10)
    @DisplayName("M25 [WEB CAT-726/748/634] Without the Catering Download right, Download offers the admin request (right restored)")
    void downloadRightAlert() {
        assumeTrue(System.getenv("ZILLIT_ROLE_CHANGES_OK") != null,
                "Changes the crew member's Catering rights: run only while holding both devices' locks "
                        + "(set ZILLIT_ROLE_CHANGES_OK=1)");
        requireReady();
        bothOn("Breakfast");
        String cap = "ZL_AUTO-cat2-dlr-" + (System.currentTimeMillis() % 1000000);
        assumeTrue(cat(crewDriver).sendGalleryImage(cap), "Crew could not post an image");
        assumeTrue(chat(crewDriver).waitMessageSent(cap, Duration.ofSeconds(60)), "The crew image did not post");
        String crewName = cat(crewDriver).senderOfRow(cap);
        assumeTrue(!crewName.isBlank(), "Could not read the crew member's name");
        com.zillit.automation.pages.settings.AdminRightsPage.RightsRow original = openCateringRights(crewName);
        assumeTrue(original != null, "The caterer device could not open " + crewName + "'s Catering rights (needs a project admin)");
        assumeTrue(Boolean.TRUE.equals(original.download) && Boolean.TRUE.equals(original.downloadOn),
                crewName + " has no updatable Catering Download right to take away: " + original);
        boolean changed = false;
        try {
            var r = new com.zillit.automation.pages.settings.AdminRightsPage(catererDriver);
            assertTrue(r.tapRight("Catering", "chkDownload") , "Download switch should be tappable");
            changed = true;
            assertTrue(r.waitRight("Catering", "chkDownload", false, Duration.ofSeconds(25)), "Download should settle OFF");
            sleep(4000);
            HomeChatPage ac = chat(crewDriver);
            assumeTrue(ac.longPressMessage(cap) && ac.tapMenuOption("Download"), "Download not offered to the crew member");
            boolean alert = cat(crewDriver).snackShown("You do not have Downloading Rights on this Tool", Duration.ofSeconds(8));
            System.out.println(">>> rights alert shown=" + alert);
            if (alert) ac.tapDialogLeft();   // Cancel — no admin request is sent
            assertTrue(alert, "Without the Download right, Download should ask to request the right from an admin");
        } finally {
            if (changed) {
                boolean ok = restoreCateringRight(original);
                System.out.println(">>> Catering Download right restored=" + ok);
                assertTrue(ok, "Could not restore " + crewName + "'s Catering Download right");
            }
            leaveRights();
            deleteOwn(crewDriver, cap);
        }
    }

    @Test
    @Order(11)
    @DisplayName("M25 [WEB CAT-728] Without the Catering View right the Tools list has no Catering (right restored)")
    void viewRightHidesTool() {
        assumeTrue(System.getenv("ZILLIT_ROLE_CHANGES_OK") != null,
                "Changes the crew member's Catering rights: run only while holding both devices' locks "
                        + "(set ZILLIT_ROLE_CHANGES_OK=1)");
        requireReady();
        bothOn("Breakfast");
        String m = post(crewDriver, "who");
        assumeTrue(m != null, "Crew marker did not send");
        String crewName = cat(crewDriver).senderOfRow(m);
        deleteOwn(crewDriver, m);
        com.zillit.automation.pages.settings.AdminRightsPage.RightsRow original = openCateringRights(crewName);
        assumeTrue(original != null, "The caterer device could not open " + crewName + "'s Catering rights");
        assumeTrue(Boolean.TRUE.equals(original.view) && Boolean.TRUE.equals(original.viewOn),
                crewName + " has no updatable Catering View right: " + original);
        boolean changed = false;
        try {
            var r = new com.zillit.automation.pages.settings.AdminRightsPage(catererDriver);
            assertTrue(r.tapRight("Catering", "chkView"), "View switch should be tappable");
            changed = true;
            assertTrue(r.waitRight("Catering", "chkView", false, Duration.ofSeconds(25)), "View should settle OFF");
            sleep(5000);
            var a = cat(crewDriver);
            for (int i = 0; i < 4 && crewDriver.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty(); i++) {
                crewDriver.navigate().back(); sleep(800);
            }
            var tools = new com.zillit.automation.pages.common.ToolsPage(crewDriver);
            tools.openToolsTab();
            tools.isLoaded(Duration.ofSeconds(15));
            tools.searchTools("Catering");
            boolean listed = tools.isModuleListed("Catering", Duration.ofSeconds(5));
            System.out.println(">>> Catering listed without View=" + listed);
            assertTrue(!listed, "Without the Catering View right the Tools list should not offer Catering");
        } finally {
            if (changed) {
                boolean ok = restoreCateringRight(original);
                System.out.println(">>> Catering View right restored=" + ok);
                assertTrue(ok, "Could not restore " + crewName + "'s Catering View right");
            }
            leaveRights();
            sleep(3000);
            cat(crewDriver).openCateringFromTools();
        }
    }

    /** On the caterer device: Admin Settings → Rights Grid (Select User) → the member → Tools tab → the Catering row. */
    private com.zillit.automation.pages.settings.AdminRightsPage.RightsRow openCateringRights(String member) {
        var r = new com.zillit.automation.pages.settings.AdminRightsPage(catererDriver);
        var admin = new com.zillit.automation.pages.adminsettings.AdminSettingsPage(catererDriver);
        for (int i = 0; i < 4 && catererDriver.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty(); i++) {
            catererDriver.navigate().back(); sleep(800);
        }
        if (!admin.openSettingsTab()) return null;
        sleep(1200);
        if (!admin.openAdminSettings() || !admin.isOpen(Duration.ofSeconds(20))) return null;
        if (!r.tapAdminRow(com.zillit.automation.pages.settings.AdminRightsPage.GRID_ROW)) return null;
        if (r.gridMainOpen(Duration.ofSeconds(3))) r.openSection("Tools");   // older builds: Select Section
        if (!r.userListOpen(Duration.ofSeconds(20))) return null;
        if (!r.openGridUser(member) || !r.userDetailsOpen(Duration.ofSeconds(20))) return null;
        r.tapToolsTab();
        sleep(1500);
        for (int i = 0; i < 6; i++) {
            var row = r.rightsRow("Catering");
            if (row != null) return row;
            sleep(800);
        }
        return null;
    }

    /** Puts the Catering row back: View first (granting), then Post, then Download. */
    private boolean restoreCateringRight(com.zillit.automation.pages.settings.AdminRightsPage.RightsRow o) {
        try {
            var r = new com.zillit.automation.pages.settings.AdminRightsPage(catererDriver);
            for (String sw : List.of("chkView", "chkPost", "chkDownload")) {
                var now = r.rightsRow("Catering");
                if (now == null) return false;
                boolean want = switch (sw) { case "chkView" -> Boolean.TRUE.equals(o.view); case "chkPost" -> Boolean.TRUE.equals(o.post); default -> Boolean.TRUE.equals(o.download); };
                boolean cur = switch (sw) { case "chkView" -> Boolean.TRUE.equals(now.view); case "chkPost" -> Boolean.TRUE.equals(now.post); default -> Boolean.TRUE.equals(now.download); };
                if (cur != want) { r.tapRight("Catering", sw); r.waitRight("Catering", sw, want, Duration.ofSeconds(25)); }
            }
            var end = r.rightsRow("Catering");
            return end != null && Boolean.TRUE.equals(end.view) == Boolean.TRUE.equals(o.view)
                    && Boolean.TRUE.equals(end.post) == Boolean.TRUE.equals(o.post)
                    && Boolean.TRUE.equals(end.download) == Boolean.TRUE.equals(o.download);
        } catch (Exception e) {
            return false;
        }
    }

    private void leaveRights() {
        for (int i = 0; i < 5 && catererDriver.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty(); i++) {
            try { catererDriver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(800);
        }
        cat(catererDriver).openCateringFromTools();
    }

    // ---------------- setup helpers ----------------

    private boolean enterCatering(AndroidDriver d) {
        try {
            long deadline = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < deadline) {
                clickText(d, "(?i)(allow|while using the app|not now|skip)");
                if (present(d, "bottomNavigation") || present(d, "projectListRecyclerView") || present(d, "recTab")) break;
                sleep(800);
            }
            if (present(d, "projectListRecyclerView")) {
                ProjectListPage list = new ProjectListPage(d);
                List<String> names = list.visibleProjectNames();
                if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
            }
            if (!present(d, "recTab") && !new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(60))) return false;
            return new CateringPage(d).openCateringFromTools();
        } catch (WebDriverException e) {
            System.out.println(">>> enterCatering failed: " + e.getMessage());
            return false;
        }
    }

    private boolean present(AndroidDriver d, String id) {
        try { return !d.findElements(AppiumBy.id(pkg + ":id/" + id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private void clickText(AndroidDriver d, String regex) {
        try {
            var l = d.findElements(AppiumBy.androidUIAutomator("new UiSelector().clickable(true).textMatches(\"" + regex + "\")"));
            if (!l.isEmpty()) l.get(0).click();
        } catch (WebDriverException ignored) { }
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private void quit(AndroidDriver d) {
        try {
            if (d != null) {
                try { d.executeScript("mobile: activateApp", Map.of("appId", pkg)); } catch (WebDriverException ignored) { }
                d.quit();
            }
        } catch (WebDriverException ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
