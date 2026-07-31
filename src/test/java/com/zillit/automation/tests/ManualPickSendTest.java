package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Config;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
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
 * "YOU PICK, AUTOMATION SENDS." For the media types whose pickers are not
 * reliably automatable (Video Gallery, Location, Contact), the human does the
 * fiddly selection ON THE DEVICE and stops at the app's send/preview screen;
 * this test waits for that screen to appear, then AUTOMATES the rest —
 * caption (where applicable) → Send → verify a new message actually posts.
 *
 * It is INTERACTIVE: run it, then perform the picking on the device within the
 * wait window (default 120s, override with ZILLIT_PICK_WAIT seconds). If the
 * send screen doesn't appear in time the test skips cleanly (no false failure).
 *
 * Run:
 *   ZILLIT_UDID=<serial> ZILLIT_APP_PACKAGE=com.zillit.zillitapp.qa \
 *   ./gradlew test --tests 'com.zillit.automation.tests.ManualPickSendTest'
 */
@Order(28)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ManualPickSendTest extends BaseTest {

    private boolean insideProject;
    private Duration pickWait;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void enterChat() {
        int secs = 120;
        try { secs = Integer.parseInt(Config.get("zillit.pickWait")); } catch (RuntimeException ignored) {}
        pickWait = Duration.ofSeconds(Math.max(30, secs));
        if (insideProject) {
            dismissPermissionPrompts();
            HomeChatPage home = new HomeChatPage(driver);
            for (int i = 0; i < 5 && !home.isInputBarVisible(); i++) {
                driver.navigate().back();
                sleep(600);
            }
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
        assumeTrue(home.isInputBarVisible(), "No posting rights — skipping");
        insideProject = true;
    }

    // ---------------------------------------------------------------- VIDEO

    @Test
    @Order(1)
    @DisplayName("VIDEO: you pick a video & reach the preview → automation sends & verifies")
    void videoPickThenSend() {
        HomeChatPage home = new HomeChatPage(driver);
        int before = home.chatRowCount();
        banner("VIDEO", "tap Attachment → 'Video Gallery' → pick a video → reach the PREVIEW screen");
        assumeTrue(waitUntil(() -> home.isMediaPreviewUp(Duration.ofSeconds(1)), pickWait),
                "Preview screen never appeared — did you reach it within " + pickWait.getSeconds() + "s? Skipping");
        System.out.println(">>> preview detected — automation taking over: caption + send");
        home.setPreviewCaption("qa-video-" + (System.currentTimeMillis() % 1000000));
        assertTrue(home.tapSendBtn(), "Send button (sendBtn) should be tappable on the preview");
        assertTrue(verifyPosted(home, before), "A new VIDEO message should post to the chat");
        screenshot("manual-video-sent");
    }

    // ------------------------------------------------------------- LOCATION

    @Test
    @Order(2)
    @DisplayName("LOCATION: you open the map & pick a spot → automation sends the pin & verifies")
    void locationPickThenSend() {
        HomeChatPage home = new HomeChatPage(driver);
        int before = home.chatRowCount();
        banner("LOCATION", "tap Attachment → 'Location' → allow permission → pick a spot → "
                + "reach the screen with the SEND-location button");
        assumeTrue(waitUntil(() -> home.isLocationSendUp(Duration.ofSeconds(1)), pickWait),
                "The map 'send location' button never appeared within " + pickWait.getSeconds() + "s — skipping");
        System.out.println(">>> map send button detected — automation sending the location");
        assertTrue(home.tapLocationSend(), "The 'send location' button (sendLocation) should be tappable");
        assertTrue(verifyPosted(home, before), "A new LOCATION message should post to the chat");
        screenshot("manual-location-sent");
    }

    // -------------------------------------------------------------- CONTACT

    @Test
    @Order(3)
    @DisplayName("CONTACT: you pick a contact & reach the confirm screen → automation sends & verifies")
    void contactPickThenSend() {
        HomeChatPage home = new HomeChatPage(driver);
        int before = home.chatRowCount();
        banner("CONTACT", "tap Attachment → 'Contact' → allow permission → pick a contact → "
                + "reach the CONFIRM screen (contact name + Send)");
        assumeTrue(waitUntil(() -> home.isContactSendUp(Duration.ofSeconds(1)), pickWait),
                "The contact confirm screen never appeared within " + pickWait.getSeconds() + "s — skipping");
        System.out.println(">>> contact confirm detected — automation sending the contact");
        assertTrue(home.tapSendBtn(), "The contact Send button (sendBtn) should be tappable");
        assertTrue(verifyPosted(home, before), "A new CONTACT message should post to the chat");
        screenshot("manual-contact-sent");
    }

    // ================================================================ helpers

    /** Prints a clear, hard-to-miss instruction block and gives the user a beat. */
    private void banner(String type, String steps) {
        System.out.println("\n============================================================");
        System.out.println(">>> MANUAL STEP — " + type);
        System.out.println(">>> On the device now: " + steps);
        System.out.println(">>> You have " + pickWait.getSeconds() + " seconds. Automation is watching…");
        System.out.println("============================================================\n");
        sleep(1500);
    }

    /** After Send, confirm the send screen closed and a new row appeared in chat. */
    private boolean verifyPosted(HomeChatPage home, int rowsBefore) {
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            dismissPermissionPrompts();
            boolean backInChat = home.isInputBarVisible();
            if (backInChat && (home.chatRowCount() > rowsBefore
                    || home.mediaBubbleShown(Duration.ofSeconds(1)))) {
                return true;
            }
            sleep(1000);
        }
        return false;
    }

    private boolean waitUntil(java.util.function.BooleanSupplier cond, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            dismissPermissionPrompts();
            if (cond.getAsBoolean()) return true;
            sleep(800);
        }
        return false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
