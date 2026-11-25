package com.zillit.automation.tests.cc;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.cc.CcChatThreadPage;
import com.zillit.automation.pages.cc.CcLandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C group chat thread — send text, reply, long-press options, and the call
 * buttons present. Operates on a "QA Cc Grp" test group (created if missing).
 */
@Order(TestOrder.CC_GROUP_CHAT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcGroupChatTest extends CcBaseTest {

    @Test
    @Order(1)
    @DisplayName("Group thread loads with input bar and audio/video call buttons")
    void threadLoads() {
        CcChatThreadPage t = openOrCreateTestGroup();
        assumeTrue(t != null && t.isLoaded(Duration.ofSeconds(15)), "Could not open a group thread — skipping");
        assertTrue(t.isInputBarVisible(), "Message input should be present");
        assertTrue(t.isAudioCallVisible(), "Audio-call button should be present in a C&C thread");
        assertTrue(t.isVideoCallVisible(), "Video-call button should be present in a C&C thread");
        System.out.println(">>> C&C group thread: " + t.title());
        screenshot("cc-group-thread");
        t.back();
    }

    @Test
    @Order(2)
    @DisplayName("Send a text message in the group — it posts")
    void sendText() {
        CcChatThreadPage t = openOrCreateTestGroup();
        assumeTrue(t != null && t.isLoaded(Duration.ofSeconds(15)), "No group thread — skipping");
        String marker = "cc-msg-" + (System.currentTimeMillis() % 1000000);
        t.sendMessage(marker);
        assertTrue(t.messageShown(marker, Duration.ofSeconds(20)),
                "The sent message '" + marker + "' should appear in the group");
        screenshot("cc-group-sent");
        t.back();
    }

    @Test
    @Order(3)
    @DisplayName("Long-press a message shows the options menu (Reply/Forward/Delete…)")
    void messageOptions() {
        CcChatThreadPage t = openOrCreateTestGroup();
        assumeTrue(t != null && t.isLoaded(Duration.ofSeconds(15)), "No group thread — skipping");
        String marker = "cc-opt-" + (System.currentTimeMillis() % 1000000);
        t.sendMessage(marker);
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(20)), "Need a message to long-press");
        assumeTrue(t.longPressMessage(marker), "Long-press did not open the menu");
        List<String> opts = t.menuOptions();
        System.out.println(">>> C&C message options: " + opts);
        assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase("Reply"))
                        || opts.stream().anyMatch(o -> o.toLowerCase().contains("delete")),
                "Options menu should include Reply/Delete. Got: " + opts);
        screenshot("cc-group-options");
        t.closeMenu();
        t.back();
    }

    @Test
    @Order(4)
    @DisplayName("Reply to a message shows the reply preview")
    void replyToMessage() {
        CcChatThreadPage t = openOrCreateTestGroup();
        assumeTrue(t != null && t.isLoaded(Duration.ofSeconds(15)), "No group thread — skipping");
        String marker = "cc-reply-" + (System.currentTimeMillis() % 1000000);
        t.sendMessage(marker);
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(20)), "Need a message to reply to");
        assumeTrue(t.longPressMessage(marker), "Long-press failed");
        assumeTrue(t.tapMenuOption("Reply"), "Reply option not present");
        assertTrue(t.isReplyPreviewVisible(), "Tapping Reply should show the reply preview");
        String reply = marker + "-r";
        t.sendMessage(reply);
        assertTrue(t.messageShown(reply, Duration.ofSeconds(15)), "The reply should post");
        screenshot("cc-group-reply");
        t.back();
    }

    /** Open the group and post a fresh unique message; returns (thread, marker). */
    private CcChatThreadPage grpWithMessage(String marker) {
        CcChatThreadPage t = openOrCreateTestGroup();
        if (t == null || !t.isLoaded(Duration.ofSeconds(15))) return null;
        t.sendMessage(marker);
        t.messageShown(marker, Duration.ofSeconds(15));
        return t;
    }

    // NOTE: "Copy" is a 1-1-only option — the GROUP long-press menu is
    // [Edit, Forward, Read By User, Reply, Share, Delete] (Copy is replaced by
    // "Read By User"). Copy is covered in CcDirectChatTest; there is no group Copy.

    @Test
    @Order(6)
    @DisplayName("Emoji react (👍) to a group message → reaction badge appears")
    void reactToMessage() {
        String m = "cc-react-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = grpWithMessage(m);
        assumeTrue(t != null, "No group thread — skipping");
        boolean reacted = t.reactTo(m, 0); // 👍
        System.out.println(">>> group reaction applied=" + reacted);
        assertTrue(reacted, "A 👍 reaction badge should attach to the message");
        screenshot("cc-group-react");
        t.back();
    }

    @Test
    @Order(7)
    @DisplayName("Edit an own group message → text updates")
    void editMessage() {
        String m = "cc-gedit-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = grpWithMessage(m);
        assumeTrue(t != null, "No group thread — skipping");
        assumeTrue(t.longPressMessage(m), "Long-press failed");
        boolean hasEdit = t.menuOptions().stream().anyMatch(o -> o.equalsIgnoreCase("Edit"));
        t.closeMenu();
        assumeTrue(hasEdit, "Edit not offered — skipping");
        String edited = m + "-X";
        assumeTrue(t.editMessage(m, edited), "Edit did not complete");
        assertTrue(t.messageShown(edited, Duration.ofSeconds(20)), "The edited text should appear");
        screenshot("cc-group-edit");
        t.back();
    }

    @Test
    @Order(8)
    @DisplayName("Forward a group message → the ForwardPage recipient picker opens")
    void forwardMessage() {
        String m = "cc-gfwd-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = grpWithMessage(m);
        assumeTrue(t != null, "No group thread — skipping");
        boolean opened = t.forwardMessage(m);
        System.out.println(">>> group Forward → ForwardPage open=" + opened);
        assertTrue(opened, "Forward should open the ForwardPage recipient picker");
        screenshot("cc-group-forward");
        t.leaveForwardPage();          // cancel WITHOUT actually forwarding
        recoverToCcLanding();
    }

    @Test
    @Order(9)
    @DisplayName("Share a group message → the OS share flow is invoked")
    void shareMessage() {
        String m = "cc-gshare-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = grpWithMessage(m);
        assumeTrue(t != null, "No group thread — skipping");
        boolean shareStarted = t.shareMessage(m);   // enters share select-mode + taps share icon
        boolean osSheet = t.isShareSurfaceShown(Duration.ofSeconds(5));
        System.out.println(">>> group Share: selectMode=" + shareStarted + " osSheet=" + osSheet);
        assertTrue(shareStarted || osSheet, "Share should start the share flow (select-mode / OS sheet)");
        screenshot("cc-group-share");
        try { driver.navigate().back(); } catch (Exception ignored) {}
        try { driver.navigate().back(); } catch (Exception ignored) {}
        recoverToCcLanding();
    }

    @Test
    @Order(10)
    @DisplayName("Delete an own group message (3-step) → it disappears")
    void deleteMessage() {
        String m = "cc-gdel-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = grpWithMessage(m);
        assumeTrue(t != null, "No group thread — skipping");
        assumeTrue(t.messageShown(m, Duration.ofSeconds(10)), "Message never posted — skipping");
        assumeTrue(t.deleteMessage(m), "Delete flow did not complete");
        sleep(1500);
        boolean gone = !t.messageShown(m, Duration.ofSeconds(4));
        System.out.println(">>> group delete: gone=" + gone);
        assertTrue(gone, "The deleted message should no longer be visible");
        screenshot("cc-group-delete");
        t.back();
    }

    // ====================================================== web-gap cases (C&C)

    /** System port for device B's UiAutomator2 (others: 8200-8300 range is taken by other suites). */
    private static final int B_PORT = 8340;

    // [WEB CC-15]
    @Test
    @Order(20)
    @DisplayName("[WEB CC-15] Two devices: a group message from A appears live on B, and B's reply live on A")
    void groupMessageArrivesLive() {
        String bUdid = firstNonBlank(com.zillit.automation.core.Config.get("zillit.receiver.udid"),
                System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(bUdid != null, "Needs ZILLIT_RECEIVER_UDID (device of " + PEER + ") — skipping");
        String name = "ZL AUTO live " + stamp();
        Boolean made = createPeerGroup(name);
        assumeTrue(made != null, "Could not set up a group with " + PEER + " — skipping");
        assertTrue(made, "Creating '" + name + "' should save");
        io.appium.java_client.android.AndroidDriver b = null;
        try {
            b = com.zillit.automation.core.DriverFactory.create(bUdid, B_PORT);
            assumeTrue(enterCcOn(b), "Device B could not open C&C — skipping");
            CcLandingPage pb = new CcLandingPage(b);
            boolean bLists = waitGroupOn(b, pb, name, Duration.ofSeconds(40));
            assumeTrue(bLists, "B does not list '" + name + "' — B is probably in another project; skipping");
            pb.typeInlineSearch(name);
            assumeTrue(pb.openRow(name, Duration.ofSeconds(8)), "B could not open the group");
            hideKeyboardOn(b);
            CcChatThreadPage tb = new CcChatThreadPage(b);
            assumeTrue(tb.isLoaded(Duration.ofSeconds(15)), "B's group thread did not open");

            CcChatThreadPage ta = openGroupNamed(name);
            assumeTrue(ta != null, "A could not open the group");
            String m1 = "[e2e] realtime A " + stamp();
            ta.sendMessage(m1);
            assertTrue(ta.messageShown(m1, Duration.ofSeconds(15)), "A should see its own message");
            assertTrue(tb.messageShown(m1, Duration.ofSeconds(20)),
                    "B should receive '" + m1 + "' live (no back / reopen) within 20 s");

            String m2 = "[e2e] realtime B " + stamp();
            tb.sendMessage(m2);
            assertTrue(tb.messageShown(m2, Duration.ofSeconds(15)), "B should see its own message");
            assertTrue(ta.messageShown(m2, Duration.ofSeconds(20)),
                    "A should receive '" + m2 + "' live (no back / reopen) within 20 s");
            screenshot("cc-group-realtime");
        } finally {
            if (b != null) {
                sweepLeftGroupsOn(b);
                try { b.quit(); } catch (Exception ignored) {}
            }
            deleteGroupQuietly(name);
        }
    }

    /** Device B: wait for the app, enter the first project, tap the C&C tab. */
    private boolean enterCcOn(io.appium.java_client.android.AndroidDriver d) {
        try {
            long deadline = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < deadline) {
                clickTextOn(d, "(?i)(allow|while using the app|not now|skip)");
                if (presentOn(d, "bottomNavigation") || presentOn(d, "projectListRecyclerView")) break;
                sleep(800);
            }
            if (presentOn(d, "projectListRecyclerView")) {
                com.zillit.automation.pages.common.ProjectListPage list =
                        new com.zillit.automation.pages.common.ProjectListPage(d);
                List<String> names = list.visibleProjectNames();
                if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
            }
            if (!new com.zillit.automation.pages.common.BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(60))) return false;
            for (var it : d.findElement(io.appium.java_client.AppiumBy.id(appPackage + ":id/bottomNavigation"))
                    .findElements(io.appium.java_client.AppiumBy.id(appPackage + ":id/menuText"))) {
                String txt = it.getText();
                if (txt != null && txt.contains("C&C")) { it.click(); break; }
            }
            sleep(1500);
            clickTextOn(d, "(?i)(allow|while using the app)");
            return new CcLandingPage(d).isLoaded(Duration.ofSeconds(30));
        } catch (org.openqa.selenium.WebDriverException e) {
            System.out.println(">>> enterCcOn failed: " + e.getMessage());
            return false;
        }
    }

    private boolean waitGroupOn(io.appium.java_client.android.AndroidDriver d, CcLandingPage p, String name, Duration t) {
        long deadline = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < deadline) {
            p.tapChip("Groups");
            sleep(1500);
            p.typeInlineSearch(name);
            hideKeyboardOn(d);
            if (p.hasRowNamed(name)) return true;
            p.tapChip("All");
            sleep(1500);
        }
        return false;
    }

    /**
     * Device B ({@link #PEER}) deletes the "ZL AUTO ren …" groups that
     * CcGroupManageTest.renamePromoteLeave leaves behind (A left them, B is their admin).
     */
    private void sweepLeftGroupsOn(io.appium.java_client.android.AndroidDriver d) {
        try {
            for (int i = 0; i < 4 && !new CcLandingPage(d).isLoaded(Duration.ofSeconds(1)); i++) {
                d.navigate().back();
                sleep(800);
            }
            CcLandingPage p = new CcLandingPage(d);
            for (int round = 0; round < 10; round++) {
                p.tapChip("Groups");
                sleep(1200);
                p.typeInlineSearch("ZL AUTO ren ");
                hideKeyboardOn(d);
                var left = p.rowNames().stream().filter(n -> n.startsWith("ZL AUTO ren ")).toList();
                if (left.isEmpty()) break;
                boolean ok = p.deleteGroupViaLongPress(left.get(0));
                System.out.println(">>> B sweep: deleted '" + left.get(0) + "' = " + ok);
                if (!ok) break;
            }
            p.tapChip("All");
        } catch (Exception e) {
            System.out.println(">>> B sweep failed: " + e.getMessage());
        }
    }

    private void hideKeyboardOn(io.appium.java_client.android.AndroidDriver d) {
        try { if (d.isKeyboardShown()) d.hideKeyboard(); } catch (Exception ignored) {}
    }

    private boolean presentOn(io.appium.java_client.android.AndroidDriver d, String id) {
        try { return !d.findElements(io.appium.java_client.AppiumBy.id(appPackage + ":id/" + id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private void clickTextOn(io.appium.java_client.android.AndroidDriver d, String regex) {
        try {
            var l = d.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).textMatches(\"" + regex + "\")"));
            if (!l.isEmpty()) l.get(0).click();
        } catch (Exception ignored) { }
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
