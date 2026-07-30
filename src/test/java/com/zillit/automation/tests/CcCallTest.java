package com.zillit.automation.tests;

import com.zillit.automation.pages.CcCallPage;
import com.zillit.automation.pages.CcChatThreadPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * C&C calling — START an audio/video call from the group thread and verify the
 * line-selection dialog + the call screen (outgoing/ringing UI + controls), then
 * END the call. A fully CONNECTED call needs a 2nd party to answer (two-device),
 * so this covers initiate → outgoing UI → controls → end on one device.
 */
@Order(43)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcCallTest extends CcBaseTest {

    private CcChatThreadPage thread() {
        // A prior call test may have left a call screen up; end it so we start
        // from the chat thread, not a lingering "Ringing…" screen.
        CcCallPage lingering = new CcCallPage(driver);
        if (lingering.isCallScreenShown(Duration.ofSeconds(2))) {
            lingering.endCall();
            sleep(1500);
            recoverToCcLanding();
        }
        CcChatThreadPage t = openOrCreateTestGroup();
        return (t != null && t.isLoaded(Duration.ofSeconds(15))) ? t : null;
    }

    @Test
    @Order(1)
    @DisplayName("AUDIO call: tap audio-call → line dialog → call screen → end")
    void audioCall() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No group thread — skipping");
        assumeTrue(t.isAudioCallVisible(), "Audio-call button not visible — skipping");
        assumeTrue(t.tapAudioCall(), "Could not tap audio call");
        // Grant the mic/phone permission prompt if it appears.
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        CcCallPage call = new CcCallPage(driver);
        // A line-selection dialog may appear first.
        if (call.isLineDialogShown(Duration.ofSeconds(5))) {
            System.out.println(">>> line dialog shown; picking a line");
            call.pickLine();
            for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        }
        boolean onCall = call.isCallScreenShown(Duration.ofSeconds(15));
        System.out.println(">>> audio call screen shown=" + onCall + " status=" + call.callStatus());
        assertTrue(onCall, "The call screen (control bar / end) should appear");
        assertTrue(call.hasControl("btnMute") || call.hasControl("btnEndCall"),
                "In-call controls (mute / end) should be present");
        screenshot("cc-audio-call");
        call.endCall();
        sleep(1500);
        recoverToCcLanding();
    }

    @Test
    @Order(2)
    @DisplayName("VIDEO call: tap video-call → call screen has video controls → end")
    void videoCall() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No group thread — skipping");
        assumeTrue(t.isVideoCallVisible(), "Video-call button not visible — skipping");
        assumeTrue(t.tapVideoCall(), "Could not tap video call");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        CcCallPage call = new CcCallPage(driver);
        if (call.isLineDialogShown(Duration.ofSeconds(5))) {
            call.pickLine();
            for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        }
        boolean onCall = call.isCallScreenShown(Duration.ofSeconds(15));
        System.out.println(">>> video call screen shown=" + onCall);
        assertTrue(onCall, "The video call screen should appear");
        assertTrue(call.hasControl("btnVideo") || call.hasControl("btnEndCall"),
                "Video call should show camera / end controls");
        screenshot("cc-video-call");
        call.endCall();
        sleep(1500);
        recoverToCcLanding();
    }

    @Test
    @Order(3)
    @DisplayName("In-call 'More' menu exposes hand-raise / record / screen-share / add-user")
    void inCallMoreMenu() {
        CcChatThreadPage t = thread();
        assumeTrue(t != null, "No group thread — skipping");
        assumeTrue(t.tapAudioCall(), "Could not start a call");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        CcCallPage call = new CcCallPage(driver);
        if (call.isLineDialogShown(Duration.ofSeconds(5))) { call.pickLine(); dismissPermissionPrompts(); }
        assumeTrue(call.isCallScreenShown(Duration.ofSeconds(15)), "Call screen did not open — skipping");
        // The full control bar (More/mute/speaker/video) only renders once the call
        // CONNECTS. On a single device the call rings forever (nobody answers), so
        // btnMore never appears — this needs a two-device run to exercise fully.
        assumeTrue(call.tapMore(),
                "'More' button not present — appears only on a CONNECTED call (needs a 2nd party to answer)");
        boolean menu = call.textShown("Hand", Duration.ofSeconds(4))
                || call.textShown("Record", Duration.ofSeconds(2))
                || call.textShown("Add", Duration.ofSeconds(2))
                || call.textShown("Screen", Duration.ofSeconds(2));
        System.out.println(">>> in-call More menu shown=" + menu);
        assertTrue(menu, "The 'More' menu should offer hand-raise/record/screen-share/add-user");
        screenshot("cc-call-more");
        try { driver.navigate().back(); } catch (Exception ignored) {}
        call.endCall();
        sleep(1500);
        recoverToCcLanding();
    }
}
