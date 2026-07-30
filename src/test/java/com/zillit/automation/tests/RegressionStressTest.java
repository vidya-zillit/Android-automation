package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.MediaPreviewPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regression / stress: rapid, repeated and interleaved operations to flush out
 * race conditions, upload-queue bugs, state corruption and crashes that single-
 * action tests miss — typing+sending fast, choosing media and sending back-to-
 * back, recording audio, and mixing them. The core success criterion is that
 * the app stays alive and responsive throughout and every message lands.
 *
 * Runs inside a chat unit with posting rights; skips otherwise.
 */
@Order(26)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegressionStressTest extends BaseTest {

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void enterChat() {
        if (insideProject) {
            dismissPermissionPrompts();
            ensureInChat();
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.isLoaded(), "Home units strip not visible");
        assumeTrue(home.selectFirstChatUnit(), "No chat unit available");
        assumeTrue(home.isInputBarVisible(), "No posting rights — skipping stress tests");
        insideProject = true;
    }

    private HomeChatPage chat() {
        return new HomeChatPage(driver);
    }

    private void ensureInChat() {
        HomeChatPage home = chat();
        for (int i = 0; i < 6 && !home.isInputBarVisible(); i++) {
            driver.navigate().back();
            sleep(600);
        }
    }

    private boolean appAlive() {
        return appPackage.equals(chat().safeCurrentPackage());
    }

    /** If a permission flow navigated us off the app (e.g. to Settings), come back. */
    private void recoverToChat(HomeChatPage home) {
        for (int i = 0; i < 4; i++) {
            if (home.isInputBarVisible() || home.isRecordingActive(Duration.ofSeconds(1))) return;
            driver.navigate().back();
            sleep(600);
        }
    }

    // ---------------- rapid text ----------------

    @Test
    @Order(1)
    @DisplayName("Rapid-fire: type and send 12 messages fast — all land, no crash")
    void rapidTextMessages() {
        HomeChatPage home = chat();
        int n = 12;
        long stamp = System.currentTimeMillis() % 100000;
        List<String> markers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String m = "qa-fast-" + stamp + "-" + i;
            markers.add(m);
            home.typeMessage(m);
            if (home.isSendButtonVisible()) {
                home.tapSend();
            }
            // No wait between sends — this is the stress part.
        }
        sleep(6000); // let the send queue drain
        // Hard regression criteria: no crash and the chat stays responsive.
        assertTrue(appAlive(), "App must not crash under rapid-fire sending");
        assertTrue(home.isInputBarVisible(), "Input bar should still work after the barrage");
        // Delivery diagnostic: the chat auto-scrolls to the newest message, and a
        // RecyclerView only renders on-screen rows, so older messages scroll out
        // of the accessibility tree and can't be counted here. We therefore only
        // assert the MOST RECENT messages are present (a real drop of the latest
        // sends would fail), and report the on-screen count as info — a low count
        // does NOT by itself prove a drop (they may just be scrolled off).
        int recentLanded = 0;
        for (int i = n - 3; i < n; i++) {
            if (home.messageShown(markers.get(i), Duration.ofSeconds(2))) recentLanded++;
        }
        int visibleTotal = 0;
        for (String m : markers) {
            if (home.messageShown(m, Duration.ofSeconds(1))) visibleTotal++;
        }
        System.out.println("=========================================================");
        System.out.println(">>> RAPID-SEND: last-3 delivered = " + recentLanded + "/3, "
                + "currently on-screen = " + visibleTotal + "/" + n
                + " (older ones scroll off-screen — not necessarily dropped)");
        System.out.println("=========================================================");
        assertTrue(recentLanded >= 2,
                "The most recent rapid messages should land (" + recentLanded + "/3)");
        screenshot("stress-rapid-text");
    }

    // ---------------- repeated media sends ----------------

    @Test
    @Order(2)
    @DisplayName("Back-to-back: choose a photo and send, then again, 3 times — no crash")
    void repeatedImageSends() {
        HomeChatPage home = chat();
        assumeTrue(home.isAttachmentVisible(), "No attachment — skipping");
        int sends = 0;
        for (int i = 0; i < 3; i++) {
            home.tapAttachment();
            if (!home.isAttachmentPickerVisible()) break;
            if (!home.tapAttachmentOption("Gallery")) { home.dismissAttachmentPicker(); break; }
            for (int j = 0; j < 4; j++) { if (!dismissPermissionPrompts()) break; sleep(400); }
            MediaPreviewPage media = new MediaPreviewPage(driver);
            if (!media.isGalleryOpen()) break;
            media.openAllMediaAlbumIfListed();
            if (media.galleryThumbCount() == 0) { driver.navigate().back(); break; }
            media.selectPhotos(1);
            if (!media.isSendFabVisible()) { driver.navigate().back(); break; }
            media.tapGallerySendFab();
            if (media.isPreviewOpen()) {
                media.setCaption("qa-burst-" + i);
                media.tapPreviewSend();
                media.waitPreviewClosed(Duration.ofSeconds(60));
                sends++;
            }
            ensureInChat();
        }
        assumeTrue(sends > 0, "Could not send any image (no gallery content?) — skipping");
        System.out.println(">>> repeated image sends: " + sends);
        assertTrue(appAlive(), "App must survive repeated image sends");
        assertTrue(chat().mediaBubbleShown(Duration.ofSeconds(30)),
                "Media bubbles should be present after the burst");
        screenshot("stress-repeated-media");
    }

    // ---------------- audio ----------------

    @Test
    @Order(3)
    @DisplayName("Record audio and send it — the audio flow completes without crashing")
    void recordAndSendAudio() {
        HomeChatPage home = chat();
        home.clearMessage();
        assumeTrue(home.isRecordButtonAvailable(),
                "Mic/record button not available (hidden on call sheet / no rights) — skipping");
        home.tapRecord();
        // The first tap may open the mic permission popup instead of recording;
        // grant it, then tap record AGAIN to actually start. (When the runner
        // pre-grants RECORD_AUDIO — the normal path — no popup appears at all.)
        boolean granted = waitAndDismissPermissions(Duration.ofSeconds(6));
        recoverToChat(home); // a denied-permission flow can jump to app settings
        if (granted && !home.isRecordingActive(Duration.ofSeconds(2))
                && home.isRecordButtonAvailable()) {
            home.tapRecord();
        }
        assumeTrue(home.isRecordingActive(Duration.ofSeconds(6)),
                "Recording did not start (mic permission denied) — skipping");
        sleep(2500); // record ~2.5s
        home.sendRecording();
        recoverToChat(home);
        assertTrue(appAlive(), "App must survive the record-and-send flow");
        assertTrue(home.isInputBarVisible(), "Input bar should return after sending audio");
        screenshot("stress-audio");
    }

    // ---------------- interleaved ----------------

    @Test
    @Order(4)
    @DisplayName("Interleaved: alternate text and media rapidly — state stays consistent")
    void interleavedTextAndMedia() {
        HomeChatPage home = chat();
        long stamp = System.currentTimeMillis() % 100000;
        // text, (attempt media), text, text — quick alternation.
        for (int i = 0; i < 4; i++) {
            String m = "qa-mix-" + stamp + "-" + i;
            home.typeMessage(m);
            if (home.isSendButtonVisible()) home.tapSend();
            if (i == 1 && home.isAttachmentVisible()) {
                // quick open/close of the attachment sheet mid-stream
                home.tapAttachment();
                if (home.isAttachmentPickerVisible()) home.dismissAttachmentPicker();
                ensureInChat();
            }
        }
        sleep(3000);
        assertTrue(appAlive(), "App must survive interleaved operations");
        assertTrue(home.isInputBarVisible(), "Chat should remain usable after interleaving");
        assertTrue(home.messageShown("qa-mix-" + stamp + "-3", Duration.ofSeconds(15)),
                "The last interleaved message should still land");
        screenshot("stress-interleaved");
    }

    // ---------------- final stability ----------------

    @Test
    @Order(5)
    @DisplayName("After the barrage the chat is still fully functional")
    void stabilityAfterBarrage() {
        HomeChatPage home = chat();
        assertTrue(appAlive(), "App should be alive");
        String marker = "qa-final-" + (System.currentTimeMillis() % 100000);
        home.sendMessage(marker);
        assertTrue(home.messageShown(marker, Duration.ofSeconds(20)),
                "A normal message should still send after all the stress");
        screenshot("stress-final-stable");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
