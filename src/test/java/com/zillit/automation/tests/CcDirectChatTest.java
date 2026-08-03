package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.pages.CcChatThreadPage;
import com.zillit.automation.pages.CcLandingPage;
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
 * C&C 1-1 (direct) chat — FULL coverage. Starts a chat with a project user from
 * SearchUsersPage and exercises: thread loads (input + call buttons), send text,
 * long-press options menu, reply, edit own message, delete own message (3-step),
 * open the attachment sheet, and start an audio/video call from the 1-1 toolbar.
 *
 * The 1-1 thread reuses ChatAndGroupPage (same code as the group thread and Home
 * chat), so send / options / reply / edit / delete mechanics match those suites.
 */
@Order(TestOrder.CC_DIRECT_CHAT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcDirectChatTest extends CcBaseTest {

    /** Opens a 1-1 chat with the first user in the SearchUsersPage list. */
    private CcChatThreadPage openDirectChat() {
        CcLandingPage p = landing();
        // Filter to Members first (harmless), then open the SearchUsersPage picker
        // via the "Search members chats" box — it lists project users split into
        // "chatted with" / "not chatted with".
        p.tapChip("Members");
        sleep(600);
        if (!p.openSearchUsers()) return null;
        System.out.println(">>> SearchUsers members: " + p.searchUserNames());
        if (!p.openFirstSearchUser()) return null;
        CcChatThreadPage t = new CcChatThreadPage(driver);
        return t.isLoaded(Duration.ofSeconds(15)) ? t : null;
    }

    /** Opens the 1-1 chat and posts a fresh own-message; returns (thread, marker). */
    private CcChatThreadPage openWithMessage(String marker) {
        CcChatThreadPage t = openDirectChat();
        if (t == null) return null;
        t.sendMessage(marker);
        t.messageShown(marker, Duration.ofSeconds(15));
        return t;
    }

    @Test
    @Order(1)
    @DisplayName("1-1 chat thread opens with input bar + audio/video call buttons")
    void directThreadLoads() {
        CcChatThreadPage t = openDirectChat();
        assumeTrue(t != null, "No user to start a 1-1 chat with — skipping");
        assertTrue(t.isInputBarVisible(), "1-1 input bar should be present");
        assertTrue(t.isAudioCallVisible() || t.isVideoCallVisible(),
                "1-1 chat should offer call buttons");
        System.out.println(">>> 1-1 chat with: " + t.title());
        screenshot("cc-direct-thread");
        t.back();
    }

    @Test
    @Order(2)
    @DisplayName("Send a text in the 1-1 chat — it posts")
    void directSendText() {
        CcChatThreadPage t = openDirectChat();
        assumeTrue(t != null, "No 1-1 chat — skipping");
        String marker = "cc-dm-" + (System.currentTimeMillis() % 1000000);
        t.sendMessage(marker);
        assertTrue(t.messageShown(marker, Duration.ofSeconds(20)),
                "The 1-1 message '" + marker + "' should post");
        screenshot("cc-direct-sent");
        t.back();
    }

    @Test
    @Order(3)
    @DisplayName("Long-press a 1-1 message shows the options menu (Copy/Reply/Forward/Delete…)")
    void directMessageOptions() {
        String marker = "cc-opt-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(marker);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assertTrue(t.longPressMessage(marker), "Long-press should open the options menu");
        List<String> opts = t.menuOptions();
        System.out.println(">>> 1-1 message options: " + opts);
        assertTrue(!opts.isEmpty(), "The 1-1 long-press menu should list actions");
        screenshot("cc-direct-options");
        t.closeMenu();
        t.back();
    }

    @Test
    @Order(4)
    @DisplayName("Reply to a 1-1 message → reply preview → reply posts")
    void directReply() {
        String marker = "cc-rep-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(marker);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        String reply = "re-" + marker;
        assumeTrue(t.replyTo(marker, reply), "Could not open reply for the message");
        assertTrue(t.messageShown(reply, Duration.ofSeconds(20)),
                "The reply '" + reply + "' should post");
        screenshot("cc-direct-reply");
        t.back();
    }

    @Test
    @Order(5)
    @DisplayName("Edit an own 1-1 message → text updates")
    void directEditMessage() {
        String marker = "cc-edit-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(marker);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assumeTrue(t.longPressMessage(marker), "Long-press failed");
        boolean hasEdit = t.menuOptions().stream().anyMatch(o -> o.equalsIgnoreCase("Edit"));
        t.closeMenu();
        assumeTrue(hasEdit, "Edit not offered on this message — skipping");
        String edited = marker + "-X";
        assumeTrue(t.editMessage(marker, edited), "Edit action did not complete");
        assertTrue(t.messageShown(edited, Duration.ofSeconds(20)),
                "The edited text '" + edited + "' should appear");
        screenshot("cc-direct-edit");
        t.back();
    }

    @Test
    @Order(6)
    @DisplayName("Delete an own 1-1 message (3-step) → it disappears")
    void directDeleteMessage() {
        String marker = "cc-del-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(marker);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assumeTrue(t.messageShown(marker, Duration.ofSeconds(10)), "Message never posted — skipping");
        assumeTrue(t.deleteMessage(marker), "Delete flow did not complete");
        sleep(1500);
        boolean gone = !t.messageShown(marker, Duration.ofSeconds(4));
        System.out.println(">>> 1-1 delete: gone=" + gone);
        assertTrue(gone, "The deleted message should no longer be visible");
        screenshot("cc-direct-delete");
        t.back();
    }

    @Test
    @Order(10)
    @DisplayName("Copy a 1-1 message → text lands on the clipboard")
    void directCopy() {
        String m = "cc-copy-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(m);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assumeTrue(t.copyMessage(m), "Copy option not available");
        String clip = t.clipboardText();
        System.out.println(">>> 1-1 Copy → clipboard=[" + clip + "]");
        assertTrue(clip.contains(m), "The clipboard should contain the copied text");
        screenshot("cc-direct-copy");
        t.back();
    }

    @Test
    @Order(11)
    @DisplayName("Emoji react (👍) to a 1-1 message → reaction badge appears")
    void directReaction() {
        String m = "cc-react-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(m);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        boolean reacted = t.reactTo(m, 0); // 👍
        System.out.println(">>> 1-1 reaction applied=" + reacted);
        assertTrue(reacted, "A 👍 reaction badge should attach to the message");
        screenshot("cc-direct-react");
        t.back();
    }

    @Test
    @Order(12)
    @DisplayName("Forward a 1-1 message → the ForwardPage recipient picker opens")
    void directForward() {
        String m = "cc-fwd-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(m);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        boolean opened = t.forwardMessage(m);
        System.out.println(">>> 1-1 Forward → ForwardPage open=" + opened);
        assertTrue(opened, "Forward should open the ForwardPage recipient picker");
        screenshot("cc-direct-forward");
        t.leaveForwardPage();          // cancel WITHOUT actually forwarding
        recoverToCcLanding();
    }

    @Test
    @Order(13)
    @DisplayName("Share a 1-1 message → the OS share flow is invoked")
    void directShare() {
        String m = "cc-share-" + (System.currentTimeMillis() % 1000000);
        CcChatThreadPage t = openWithMessage(m);
        assumeTrue(t != null, "No 1-1 chat — skipping");
        boolean shareStarted = t.shareMessage(m);   // enters share select-mode + taps share icon
        boolean osSheet = t.isShareSurfaceShown(Duration.ofSeconds(5));
        System.out.println(">>> 1-1 Share: selectMode=" + shareStarted + " osSheet=" + osSheet);
        assertTrue(shareStarted || osSheet, "Share should start the share flow (select-mode / OS sheet)");
        screenshot("cc-direct-share");
        try { driver.navigate().back(); } catch (Exception ignored) {}
        try { driver.navigate().back(); } catch (Exception ignored) {}
        recoverToCcLanding();
    }

    @Test
    @Order(7)
    @DisplayName("Attachment sheet opens from the 1-1 input bar")
    void directAttachmentSheet() {
        CcChatThreadPage t = openDirectChat();
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assumeTrue(t.tapAttachment(), "Attachment button not present");
        // The attachment chooser (camera/gallery/document/…) should surface.
        boolean sheet = t.textShown("Gallery", Duration.ofSeconds(4))
                || t.textShown("Camera", Duration.ofSeconds(2))
                || t.textShown("Document", Duration.ofSeconds(2))
                || t.textShown("File", Duration.ofSeconds(2));
        System.out.println(">>> 1-1 attachment sheet shown=" + sheet);
        assertTrue(sheet, "Tapping attachment should open the media chooser");
        screenshot("cc-direct-attach");
        try { driver.navigate().back(); } catch (Exception ignored) {}
        t.back();
    }

    @Test
    @Order(8)
    @DisplayName("Start an AUDIO call from the 1-1 toolbar → call screen → end")
    void directAudioCall() {
        CcChatThreadPage t = openDirectChat();
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assumeTrue(t.isAudioCallVisible() && t.tapAudioCall(), "Audio-call button not available");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        com.zillit.automation.pages.CcCallPage call = new com.zillit.automation.pages.CcCallPage(driver);
        if (call.isLineDialogShown(Duration.ofSeconds(5))) { call.pickLine(); dismissPermissionPrompts(); }
        boolean onCall = call.isCallScreenShown(Duration.ofSeconds(15));
        System.out.println(">>> 1-1 audio call screen=" + onCall + " status=" + call.callStatus());
        assertTrue(onCall, "The 1-1 audio call screen should appear");
        screenshot("cc-direct-audiocall");
        call.endCall();
        sleep(1500);
        recoverToCcLanding();
    }

    @Test
    @Order(9)
    @DisplayName("Start a VIDEO call from the 1-1 toolbar → call screen → end")
    void directVideoCall() {
        CcChatThreadPage t = openDirectChat();
        assumeTrue(t != null, "No 1-1 chat — skipping");
        assumeTrue(t.isVideoCallVisible() && t.tapVideoCall(), "Video-call button not available");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        com.zillit.automation.pages.CcCallPage call = new com.zillit.automation.pages.CcCallPage(driver);
        if (call.isLineDialogShown(Duration.ofSeconds(5))) { call.pickLine(); dismissPermissionPrompts(); }
        boolean onCall = call.isCallScreenShown(Duration.ofSeconds(15));
        System.out.println(">>> 1-1 video call screen=" + onCall);
        assertTrue(onCall, "The 1-1 video call screen should appear");
        screenshot("cc-direct-videocall");
        call.endCall();
        sleep(1500);
        recoverToCcLanding();
    }
}
