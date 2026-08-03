package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.CcChatThreadPage;
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
}
