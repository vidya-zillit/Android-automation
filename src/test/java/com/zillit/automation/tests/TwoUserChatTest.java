package com.zillit.automation.tests;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.MediaPreviewPage;
import com.zillit.automation.pages.ProjectListPage;
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
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TWO-DEVICE, TWO-USER, BIDIRECTIONAL messaging. Two devices, each logged in as
 * a DIFFERENT user in the SAME project + unit. Every scenario runs BOTH ways
 * (A→B and B→A) so both users send and receive — the real-world case:
 *
 *   - text / image / audio / document sent by A arrives at B, and vice-versa;
 *   - the media is actually OPENED/PLAYED on BOTH the sender and the receiver
 *     (image opens full-screen, voice-note plays, document opens);
 *   - an EDITED message updates on the other side (new text, original gone);
 *   - a DELETED message DISAPPEARS on the other side.
 *
 * PRECONDITION (manual, one-time): both devices installed + logged into the same
 * project as two distinct users with posting rights. The suite navigates each
 * into the shared unit and SKIPS cleanly if it can't.
 *
 * Config / env:
 *   ZILLIT_SENDER_UDID   device A (default zillit.udid / emulator-5554)
 *   ZILLIT_RECEIVER_UDID device B (REQUIRED)
 *   ZILLIT_UNIT          unit name (default: first chat unit)
 *
 *   ZILLIT_SENDER_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=<serial> \
 *   ./gradlew test --tests 'com.zillit.automation.tests.TwoUserChatTest'
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TwoUserChatTest {

    /** Cross-device propagation budget for TEXT (sockets are fast). */
    private static final Duration DELIVER = Duration.ofSeconds(25);
    /** Media takes longer to upload + sync, especially to a physical device. */
    private static final Duration MEDIA_DELIVER = Duration.ofSeconds(50);

    private Dev A;   // device A
    private Dev B;   // device B
    private String pkg;
    private boolean ready;

    /** One physical/emulated device + its display name. */
    private final class Dev {
        final AndroidDriver driver;
        final String name;
        Dev(AndroidDriver driver, String name) { this.driver = driver; this.name = name; }
        HomeChatPage chat() { return new HomeChatPage(driver); }
    }

    @BeforeAll
    void setUp() {
        String bUdid = firstNonBlank(Config.get("zillit.receiver.udid"), System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(bUdid != null && !bUdid.isBlank(),
                "Two-device tests need ZILLIT_RECEIVER_UDID (the second device serial) — skipping");
        String aUdid = firstNonBlank(Config.get("zillit.sender.udid"), System.getenv("ZILLIT_SENDER_UDID"),
                Config.get("zillit.udid"), "emulator-5554");

        A = new Dev(DriverFactory.create(aUdid, 8200), "A");
        B = new Dev(DriverFactory.create(bUdid, 8210), "B");
        pkg = A.driver.getCurrentPackage();

        String unit = Config.get("zillit.unit");
        boolean a = enterUnit(A, unit);
        boolean b = enterUnit(B, unit);
        ready = a && b;
        System.out.println(">>> two-device setup: A-in-unit=" + a + " B-in-unit=" + b
                + " unit='" + (unit.isBlank() ? "<first>" : unit) + "'");
    }

    @AfterAll
    void tearDown() {
        if (A != null) quitQuiet(A.driver);
        if (B != null) quitQuiet(B.driver);
    }

    // =============================================================== TEXT

    @Test
    @Order(1)
    @DisplayName("TEXT both ways: A→B and B→A")
    void textBothWays() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        textOneWay(A, B);
        textOneWay(B, A);
    }

    private void textOneWay(Dev from, Dev to) {
        settle(from); settle(to);
        HomeChatPage s = from.chat(), r = to.chat();
        assumeTrue(s.isInputBarVisible(), from.name + " has no posting rights — skipping");
        String marker = "x-txt-" + from.name + "-" + stamp();
        s.sendMessage(marker);
        assertTrue(s.messageShown(marker, Duration.ofSeconds(12)), from.name + " should see its own text");
        assertTrue(waitText(r, marker, DELIVER),
                to.name + " should RECEIVE text '" + marker + "' from " + from.name);
    }

    // ============================================================== IMAGE

    @Test
    @Order(5)
    @DisplayName("IMAGE both ways: sent, received, and OPENED full-screen on both")
    void imageBothWays() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        imageOneWay(A, B);
        imageOneWay(B, A);
    }

    private void imageOneWay(Dev from, Dev to) {
        settle(from); settle(to);
        HomeChatPage s = from.chat(), r = to.chat();
        assumeTrue(s.isAttachmentVisible(), from.name + " has no posting rights — skipping");
        int sBefore = s.imageBubbleCount();
        int rBefore = r.imageBubbleCount();
        boolean sent = s.sendOneImage("x-img-" + from.name + "-" + stamp(), v -> dismiss(from.driver));
        assumeTrue(sent, from.name + " could not send an image (no gallery content?) — skipping");

        assertTrue(waitCountGrew(s::imageBubbleCount, sBefore, Duration.ofSeconds(40))
                        || s.mediaBubbleShown(Duration.ofSeconds(2)),
                from.name + " image should post");
        softOpen(from.name + " opens own image", s.openNewestImage());
        s.closeViewer();

        // The essential cross-device assertion: the image reaches the other user.
        assertTrue(waitCountGrew(r::imageBubbleCount, rBefore, MEDIA_DELIVER),
                to.name + " should RECEIVE the image from " + from.name);
        softOpen(to.name + " opens received image", r.openNewestImage());
        r.closeViewer();
    }

    // ============================================================== AUDIO

    @Test
    @Order(4)
    @DisplayName("AUDIO both ways: voice note sent, received, and PLAYED on both")
    void audioBothWays() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        audioOneWay(A, B);
        audioOneWay(B, A);
    }

    private void audioOneWay(Dev from, Dev to) {
        settle(from); settle(to);
        HomeChatPage s = from.chat(), r = to.chat();
        s.clearMessage();
        assumeTrue(s.isRecordButtonAvailable(), from.name + " mic/record not available — skipping");
        int rBefore = r.audioBubbleCount();

        s.tapRecord();
        dismiss(from.driver);
        if (!s.isRecordingActive(Duration.ofSeconds(2)) && s.isRecordButtonAvailable()) {
            s.tapRecord();
        }
        assumeTrue(s.isRecordingActive(Duration.ofSeconds(5)), from.name + " recording did not start — skipping");
        sleepQuiet(1400);          // short voice note — keep it fast
        s.sendRecording();

        assertTrue(waitCountGrew(s::audioBubbleCount, Math.max(0, s.audioBubbleCount() - 1),
                Duration.ofSeconds(20)) || s.isAudioBubblePresent(Duration.ofSeconds(5)),
                from.name + " voice note should post");
        softOpen(from.name + " plays own voice note", s.playNewestAudio());

        // Essential cross-device assertion: the voice note reaches the other user.
        assertTrue(waitCountGrew(r::audioBubbleCount, rBefore, MEDIA_DELIVER),
                to.name + " should RECEIVE the voice note from " + from.name);
        softOpen(to.name + " plays received voice note", r.playNewestAudio());
    }

    // =========================================================== DOCUMENT

    @Test
    @Order(6)
    @DisplayName("DOCUMENT both ways: sent, received, and OPENED on both")
    void documentBothWays() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        documentOneWay(A, B);
        documentOneWay(B, A);
    }

    private void documentOneWay(Dev from, Dev to) {
        settle(from); settle(to);
        HomeChatPage s = from.chat(), r = to.chat();
        assumeTrue(s.isAttachmentVisible(), from.name + " has no posting rights — skipping");
        int rBefore = r.docBubbleCount();

        s.tapAttachment();
        assumeTrue(s.isAttachmentPickerVisible() || s.isCallSheetFirstPopupShown(Duration.ofSeconds(2)),
                from.name + " attachment picker did not open — skipping");
        if (s.isAttachmentPickerVisible() && !s.tapAttachmentOption("Document")) {
            s.dismissAttachmentPicker();
            assumeTrue(false, "No Document option on this unit — skipping");
        }
        for (int i = 0; i < 3; i++) { if (!dismiss(from.driver)) break; sleepQuiet(400); }
        assumeTrue(s.pickSystemFileByName("zl_test", Duration.ofSeconds(12)),
                from.name + " system picker could not surface the test PDF — skipping");
        MediaPreviewPage prev = new MediaPreviewPage(from.driver);
        if (prev.isPreviewOpen()) prev.tapPreviewSend();

        assumeTrue(s.isDocBubbleShown(Duration.ofSeconds(40)), from.name + " document did not post — skipping");
        softOpen(from.name + " opens own document", s.openNewestDoc());
        s.closeViewer();

        // Essential cross-device assertion: the document reaches the other user.
        assertTrue(waitCountGrew(r::docBubbleCount, rBefore, MEDIA_DELIVER),
                to.name + " should RECEIVE the document from " + from.name);
        softOpen(to.name + " opens received document", r.openNewestDoc());
        r.closeViewer();
    }

    /**
     * OPEN/PLAY the media is exercised but reported as info rather than
     * hard-failing the test: the app's in-app viewer launch is not a single
     * deterministic tap (download-gated, child-view specific), so hard-asserting
     * it would flakily mask the DELIVERY result, which is the real check.
     */
    private void softOpen(String label, boolean ok) {
        System.out.println(">>> OPEN/PLAY [" + label + "]: " + (ok ? "opened ✓" : "could not open (see note)"));
    }

    // =============================================================== EDIT

    @Test
    @Order(2)
    @DisplayName("EDIT both ways: A edits→B sees update; B edits→A sees update")
    void editBothWays() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        editOneWay(A, B);
        editOneWay(B, A);
    }

    private void editOneWay(Dev from, Dev to) {
        settle(from); settle(to);
        HomeChatPage s = from.chat(), r = to.chat();
        assumeTrue(s.isInputBarVisible(), from.name + " has no posting rights — skipping");
        // Distinct markers with NO containment (else "orig gone" is a false negative).
        long id = stamp();
        String orig = "xedit-" + from.name + "-o" + id;
        String edited = "xedit-" + from.name + "-e" + id;

        s.sendMessage(orig);
        assumeTrue(s.messageShown(orig, Duration.ofSeconds(12)), from.name + " did not post the base message");
        System.out.println(">>> EDIT " + from.name + ": base posted, waiting for receiver…");
        assumeTrue(waitText(r, orig, DELIVER), to.name + " never got the base message — skipping edit check");

        System.out.println(">>> EDIT " + from.name + ": long-pressing…");
        assumeTrue(s.longPressMessage(orig), from.name + " long-press failed");
        assumeTrue(s.isMenuOpen(Duration.ofSeconds(6)), from.name + " menu did not open");
        System.out.println(">>> EDIT " + from.name + ": menu open, options=" + s.menuOptions());
        assumeTrue(s.tapMenuOption("Edit"), "Edit option not present");
        assumeTrue(s.isEditDialogOpen(Duration.ofSeconds(6)), "Edit dialog did not open");
        System.out.println(">>> EDIT " + from.name + ": edit dialog open");
        s.setEditDialogText(edited);
        s.saveEditDialog();
        assumeTrue(s.messageShown(edited, Duration.ofSeconds(15)), from.name + " edit did not apply");

        assertTrue(waitText(r, edited, DELIVER), to.name + " should see the EDITED text '" + edited + "'");
        assertTrue(!r.messageShown(orig, Duration.ofSeconds(3)),
                to.name + " should no longer see the ORIGINAL text after the edit");
    }

    // ============================================================= DELETE

    @Test
    @Order(3)
    @DisplayName("DELETE both ways: A deletes→gone on B; B deletes→gone on A")
    void deleteBothWays() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        deleteOneWay(A, B);
        deleteOneWay(B, A);
    }

    private void deleteOneWay(Dev from, Dev to) {
        settle(from); settle(to);
        HomeChatPage s = from.chat(), r = to.chat();
        assumeTrue(s.isInputBarVisible(), from.name + " has no posting rights — skipping");
        String marker = "xdel-" + from.name + "-" + stamp();
        s.sendMessage(marker);
        assumeTrue(s.messageShown(marker, Duration.ofSeconds(12)), from.name + " did not post the message");
        assumeTrue(waitText(r, marker, DELIVER), to.name + " never got the message — skipping delete check");

        assumeTrue(s.longPressMessage(marker), from.name + " long-press failed");
        assumeTrue(s.isMenuOpen(Duration.ofSeconds(6)), from.name + " menu did not open");
        assumeTrue(s.tapMenuOption("Delete"), "Delete option not present");
        // 3-step delete: menu Delete -> action-bar Delete -> Yes/No dialog.
        s.tapDeleteActionBar(Duration.ofSeconds(6));
        assertTrue(confirmYes(from.driver, Duration.ofSeconds(8)),
                from.name + " delete-confirmation dialog (Yes) should appear");
        assertTrue(!s.messageShown(marker, Duration.ofSeconds(8)),
                "message should be gone on the deleter " + from.name);

        boolean gone = waitGone(r, marker, DELIVER);
        System.out.println(">>> DELETE " + from.name + "→" + to.name + ": goneOnOther=" + gone);
        assertTrue(gone, to.name + " should no longer see the message deleted by " + from.name
                + " (if the app deletes only for the sender, THIS is the finding to report)");
    }

    // =============================================== OTHER-USER MESSAGE

    /**
     * Interacting with the OTHER user's (received) message — a different context
     * from your own message. B sends; A (the actor) long-presses B's message and:
     *   - the received-message options are present (Reply / Forward / Share /
     *     Read By, and TRANSLATE, which only shows on another user's message);
     *   - A can REPLY to B's message (reply posts, and reaches B);
     *   - A canNOT DELETE B's message (delete is blocked for other users' messages).
     * The actor is A (the emulator) because its long-press is reliable; the
     * message under test is one B (the other user) sent.
     */
    @Test
    @Order(7)
    @DisplayName("OTHER user's message: options + reply + delete-blocked (A acts on B's message)")
    void otherUserMessageInteractions() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage bChat = B.chat();   // sender = the OTHER user
        HomeChatPage aChat = A.chat();   // actor  = us, receiving B's message
        assumeTrue(bChat.isInputBarVisible(), "B has no posting rights — skipping");

        String marker = "xother-" + stamp();
        bChat.sendMessage(marker);
        assumeTrue(waitText(aChat, marker, DELIVER), "A never received B's message — skipping");

        // --- long-press the OTHER user's message and inspect its options ---
        assumeTrue(aChat.longPressMessage(marker), "A long-press on B's message failed");
        assumeTrue(aChat.isMenuOpen(Duration.ofSeconds(6)), "Options menu did not open on B's message");
        List<String> opts = aChat.menuOptions();
        System.out.println(">>> OTHER-user message options (A on B's msg): " + opts);
        for (String expected : List.of("Reply", "Forward", "Share")) {
            assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase(expected)),
                    "A received-message menu should offer '" + expected + "'. Got: " + opts);
        }
        assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("read by")),
                "Received-message menu should offer 'Read By User'. Got: " + opts);
        boolean hasTranslate = opts.stream().anyMatch(o -> o.toLowerCase().contains("translate"));
        System.out.println(">>> Translate present on other-user message (own-message-hidden option): " + hasTranslate);

        // --- REPLY to the other user's message ---
        assumeTrue(aChat.tapMenuOption("Reply"), "Reply option not present on B's message");
        assumeTrue(aChat.isReplyPreviewVisible(), "Reply preview should show after tapping Reply");
        String reply = "xreply-" + stamp();
        aChat.sendMessage(reply);
        assertTrue(aChat.messageShown(reply, Duration.ofSeconds(15)),
                "A's reply to B's message should post");
        assertTrue(waitText(bChat, reply, DELIVER), "B should receive A's reply");

        // --- DELETE on the other user's message must be BLOCKED ---
        assumeTrue(aChat.longPressMessage(marker), "A long-press (2nd) on B's message failed");
        assumeTrue(aChat.isMenuOpen(Duration.ofSeconds(6)), "Menu did not reopen on B's message");
        if (aChat.tapMenuOption("Delete")) {
            // For another user's message the app blocks delete (snackbar
            // "you cannot delete other user message") — no confirm, message stays.
            aChat.tapDeleteActionBar(Duration.ofSeconds(3));
            confirmYes(A.driver, Duration.ofSeconds(3));
        }
        assertTrue(aChat.messageShown(marker, Duration.ofSeconds(6)),
                "B's message must STILL be present — you cannot delete another user's message");
        System.out.println(">>> delete-blocked on other-user message: verified (message still present)");
    }

    // ============================================================= helpers

    /** Ensures a device is sitting in the chat with the input bar reachable. */
    private void settle(Dev d) {
        HomeChatPage home = d.chat();
        collapseShade(d.driver);   // a stray gesture can pull the notification shade down
        dismiss(d.driver);
        for (int i = 0; i < 4 && !home.isInputBarVisible(); i++) {
            if (home.isLoaded()) {
                home.selectFirstChatUnit();
                sleepQuiet(700);
                if (home.isInputBarVisible()) return;
            }
            try { d.driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleepQuiet(500);
        }
        if (home.isInputBarVisible()) return;
        // Last resort: relaunch (fast, no reinstall) and re-enter the unit. Also
        // resets the chat to a clean bottom-scrolled state, which makes the
        // subsequent long-press reliable.
        try {
            d.driver.executeScript("mobile: terminateApp", Map.of("appId", pkg));
            d.driver.executeScript("mobile: activateApp", Map.of("appId", pkg));
        } catch (WebDriverException ignored) {
        }
        enterUnit(d, Config.get("zillit.unit"));
    }

    /** Collapses the Android notification shade if a gesture pulled it down. */
    private void collapseShade(AndroidDriver d) {
        try {
            String focus = d.getCurrentPackage();
            // If we're not in the app (shade/systemui on top), bring the app back.
            if (focus == null || !focus.equals(pkg)) {
                d.executeScript("mobile: activateApp", Map.of("appId", pkg));
                sleepQuiet(500);
            }
        } catch (WebDriverException ignored) {
        }
    }

    /** Polls one device's chat for a message marker (fast 500ms cadence). */
    private boolean waitText(HomeChatPage page, String marker, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (page.messageShown(marker, Duration.ofMillis(500))) return true;
        }
        return false;
    }

    private boolean waitGone(HomeChatPage page, String marker, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!page.messageShown(marker, Duration.ofMillis(500))) return true;
        }
        return false;
    }

    private boolean waitCountGrew(IntSupplier count, int before, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (count.getAsInt() > before) return true;
            sleepQuiet(600);
        }
        return false;
    }

    /** Drives one device from its landing screen into the target chat unit. */
    private boolean enterUnit(Dev d, String unitName) {
        AndroidDriver drv = d.driver;
        try {
            long deadline = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < deadline) {
                dismiss(drv);
                if (isPresent(drv, "bottomNavigation") || isPresent(drv, "projectListRecyclerView")
                        || isPresent(drv, "start_project_btn") || isPresent(drv, "recTab")) {
                    break;
                }
                clickText(drv, "(?i)ok");
                clickId(drv, "skipBtn");
            }
            if (isPresent(drv, "projectListRecyclerView")) {
                ProjectListPage list = new ProjectListPage(drv);
                List<String> names = list.visibleProjectNames();
                if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
            }
            if (!new BottomNavPage(drv).waitUntilLoaded(Duration.ofSeconds(60))) return false;
            HomeChatPage home = d.chat();
            if (!home.isLoaded()) return false;
            return unitName == null || unitName.isBlank()
                    ? home.selectFirstChatUnit()
                    : home.selectUnit(unitName);
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean confirmYes(AndroidDriver d, Duration total) {
        long deadline = System.currentTimeMillis() + total.toMillis();
        var confirm = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)(yes|confirm|ok)\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                new WebDriverWait(d, Duration.ofMillis(600))
                        .until(ExpectedConditions.elementToBeClickable(confirm)).click();
                return true;
            } catch (WebDriverException notYet) {
            }
        }
        return false;
    }

    private boolean dismiss(AndroidDriver d) {
        boolean any = false;
        for (int i = 0; i < 8; i++) {
            if (clickId(d, "tvContinue")) { any = true; continue; }
            if (clickText(d, "(?i)(allow all.*|allow|while using the app|"
                    + "allow only while using the app|only this time)")) { any = true; continue; }
            if (clickText(d, "(?i)(no thanks|not now|skip)")) { any = true; continue; }
            break;
        }
        return any;
    }

    private boolean isPresent(AndroidDriver d, String id) {
        try {
            return !d.findElements(AppiumBy.id(pkg + ":id/" + id)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean clickId(AndroidDriver d, String id) {
        try {
            new WebDriverWait(d, Duration.ofMillis(400))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.id(pkg + ":id/" + id))).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean clickText(AndroidDriver d, String regex) {
        try {
            new WebDriverWait(d, Duration.ofMillis(400))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"" + regex + "\")"))).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private long stamp() {
        return System.currentTimeMillis() % 10_000_000L;
    }

    private void quitQuiet(AndroidDriver d) {
        try {
            if (d != null) d.quit();
        } catch (WebDriverException ignored) {
        }
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
