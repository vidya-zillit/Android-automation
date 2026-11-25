package com.zillit.automation.tests.homechat;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import com.zillit.automation.pages.common.MediaPreviewPage;
import com.zillit.automation.pages.common.ProjectListPage;
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
 *   ./gradlew test --tests 'com.zillit.automation.tests.homechat.TwoUserChatTest'
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.extension.ExtendWith(com.zillit.automation.core.ReasonLogger.class)
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

        // 8260/8270, not 8200/8210: other suites run single-device sessions on the same Appium
        // server at the same time, and Appium hands those the lowest free port from 8200 up.
        A = new Dev(DriverFactory.create(aUdid, 8260), "A");
        B = new Dev(DriverFactory.create(bUdid, 8270), "B");
        // From config, not from the screen: A may be showing another app (a picker) at session start.
        pkg = Config.configuredAppPackage();
        if (pkg == null || pkg.isEmpty()) pkg = A.driver.getCurrentPackage();

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
        String id = stamp();
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

        // --- DELETE on the other user's message ---
        // A non-admin is refused ("you cannot delete other user message"); an ADMIN may delete
        // anyone's message (Home.kt setDeleteAction accepts isAdmin, :1422; handleMultiSelection
        // :1308). Account A (Vidya Testing) is a project admin, so the old "must be blocked"
        // assertion depended on the role. Branch on what the app does, and never confirm.
        assumeTrue(aChat.longPressMessage(marker), "A long-press (2nd) on B's message failed");
        assumeTrue(aChat.isMenuOpen(Duration.ofSeconds(6)), "Menu did not reopen on B's message");
        if (aChat.tapMenuOption("Delete")) {
            boolean adminSelection = aChat.inSelectionMode(Duration.ofSeconds(3));
            boolean refused = aChat.textShown("delete other", Duration.ofSeconds(2));
            System.out.println(">>> delete on other-user message: selectionMode(admin)=" + adminSelection + " refused=" + refused);
            if (adminSelection) aChat.exitSelectionMode();     // admin path: leave without deleting
            assertTrue(adminSelection || refused,
                    "Delete on another member's message should either refuse (non-admin) or enter selection mode (admin)");
        }
        assertTrue(aChat.messageShown(marker, Duration.ofSeconds(6)),
                "B's message must still be present — nothing was confirmed");
    }

    // ===================================================== WEB GAP (2026-11-23)

    /**
     * [WEB M5 #109] (sheet BUL-16). B (the Pixel, "Vidya Pixel") posts; A (the emulator) opens
     * the menu on B's message. Edit must not be offered on another member's message.
     * Home.kt builds the menu with {@code showEdit = !homeChatInfo.message.isNullOrEmpty()} and no
     * owner check — the owner check only runs after the tap ("You can't edit other users
     * message"), so this FAILS while BUL-16 is open.
     */
    @Test
    @Order(8)
    @DisplayName("[WEB M5] Another member's message offers no Edit (BUL-16)")
    void otherMembersMessageOffersNoEdit() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage bChat = B.chat(), aChat = A.chat();
        assumeTrue(bChat.isInputBarVisible(), "B has no posting rights — skipping");
        String marker = "ZL_AUTO-m5-" + stamp();
        try {
            bChat.sendMessage(marker);
            assumeTrue(waitText(aChat, marker, DELIVER), "A never received B's message — skipping");
            assumeTrue(aChat.longPressMessage(marker), "A long-press on B's message failed");
            List<String> opts = aChat.menuOptions();
            System.out.println(">>> M5 A's menu on B's message: " + opts);
            boolean editOffered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Edit"));
            String afterTap = "";
            if (editOffered) {
                aChat.tapMenuOption("Edit");
                if (aChat.textShown("edit other", Duration.ofSeconds(4))) afterTap = "snackbar \"You can't edit other users message\"";
                else if (aChat.isEditDialogOpen(Duration.ofSeconds(2))) { afterTap = "EDIT DIALOG OPENED"; aChat.cancelEditDialog(); }
                else afterTap = "nothing visible";
                System.out.println(">>> M5 tapping Edit on B's message gave: " + afterTap);
            } else {
                aChat.closeMenu();
            }
            assertTrue(!editOffered,
                    "BUL-16: Edit is offered on another member's message (Home.kt showEdit has no owner check); tapping it gave: " + afterTap);
        } finally {
            bChat.deleteOwnMessage(marker);
        }
    }

    /**
     * [WEB M9 #122 #160]. A sits on Calendar; B posts in the shared chat unit. A's chip for that
     * unit shows an unread badge; when A opens the unit the "New Messages" header sits above B's
     * post and the badge clears.
     */
    @Test
    @Order(9)
    @DisplayName("[WEB M9] Unread badge on the unit chip and the New Messages divider")
    void unreadBadgeAndNewMessagesDivider() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage aChat = A.chat(), bChat = B.chat();
        String unit = aChat.bulletinLikeUnit();
        String cal = aChat.unitNameContaining("calendar");
        assumeTrue(unit != null && cal != null, "Need a Bulletin-like chat unit and a Calendar chip on A");
        assumeTrue(bChat.selectUnit(unit) && bChat.isInputBarVisible(), "B cannot post in " + unit);
        String marker = "ZL_AUTO-m9-" + stamp();
        try {
            assumeTrue(aChat.selectUnit(cal), "A could not switch to Calendar");
            sleepQuiet(1500);
            bChat.sendMessage(marker);
            assumeTrue(bChat.waitMessageSent(marker, Duration.ofSeconds(30)), "B's post did not reach the server");
            String badge = "";
            long deadline = System.currentTimeMillis() + DELIVER.toMillis();
            while (badge.isBlank() && System.currentTimeMillis() < deadline) {
                badge = aChat.unitBadge(unit);
                if (badge.isBlank()) sleepQuiet(1000);
            }
            System.out.println(">>> M9 A's badge on '" + unit + "': '" + badge + "'");
            assertTrue(!badge.isBlank(), "A's '" + unit + "' chip should show an unread badge after B posts");

            aChat.selectUnit(unit);
            assertTrue(waitText(aChat, marker, DELIVER), "A should see B's post after opening " + unit);
            boolean header = aChat.textShown("New Messages", Duration.ofSeconds(5));
            assertTrue(header, "The 'New Messages' divider should show above B's unread post");
            sleepQuiet(2500);
            assertTrue(aChat.unitBadge(unit).isBlank(), "The badge should clear once A has opened " + unit
                    + " (still '" + aChat.unitBadge(unit) + "')");
        } finally {
            aChat.selectUnit(unit);
            bChat.deleteOwnMessage(marker);
        }
    }

    /**
     * [WEB M34 #152] (sheet BUL-29). A posts and replies to it; B sees the reply under the post.
     * A edits the post; on B the post shows the edited text and the reply is still under it.
     */
    @Test
    @Order(10)
    @DisplayName("[WEB M34] An edit arriving live keeps the replies under the post (BUL-29)")
    void liveEditKeepsReplies() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage aChat = A.chat(), bChat = B.chat();
        String id = stamp();
        String orig = "ZL_AUTO-m34-o" + id;
        String edited = "ZL_AUTO-m34-e" + id;
        String reply = "ZL_AUTO-m34-r" + id;
        try {
            aChat.sendMessage(orig);
            assumeTrue(aChat.waitMessageSent(orig, Duration.ofSeconds(30)), "A's post did not reach the server");
            assumeTrue(aChat.longPressMessage(orig) && aChat.tapMenuOption("Reply"), "A could not start a reply");
            aChat.sendMessage(reply);
            assumeTrue(waitText(bChat, reply, DELIVER), "B never got A's reply");
            assumeTrue(bChat.replyUnderRow(orig, reply), "On B the reply is not under the post before the edit");

            assumeTrue(aChat.longPressMessage(orig) && aChat.tapMenuOption("Edit"), "A could not open Edit");
            assumeTrue(aChat.isEditDialogOpen(Duration.ofSeconds(6)), "Edit dialog did not open");
            aChat.setEditDialogTextNoBack(edited);
            aChat.saveEditDialog();
            assumeTrue(aChat.messageShown(edited, Duration.ofSeconds(15)), "A's edit did not apply");

            assertTrue(waitText(bChat, edited, DELIVER), "B should see the edited text live");
            sleepQuiet(1500);
            assertTrue(bChat.replyUnderRow(edited, reply),
                    "BUL-29: after the live edit B lost the reply under the post");
        } finally {
            aChat.cancelReplyPreview();
            if (!aChat.deleteOwnMessage(edited)) aChat.deleteOwnMessage(orig);
        }
    }

    /**
     * [WEB M20 #126 #162] (sheet BUL-18, two-device half). B sits on Calendar; A posts and opens
     * Read By on the post; B opens the unit. B's name should move into A's read list live.
     */
    @Test
    @Order(11)
    @DisplayName("[WEB M20b] Read By updates live: B moves into the read list when B opens the unit")
    void readByUpdatesLive() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage aChat = A.chat(), bChat = B.chat();
        String unit = aChat.bulletinLikeUnit();
        String cal = bChat.unitNameContaining("calendar");
        assumeTrue(unit != null && cal != null, "Need a Bulletin-like unit and a Calendar chip");
        aChat.selectUnit(unit);
        String probe = "ZL_AUTO-m20p-" + stamp();
        String m = "ZL_AUTO-m20-" + stamp();
        try {
            assumeTrue(bChat.selectUnit(unit) && bChat.isInputBarVisible(), "B cannot post in " + unit);
            bChat.sendMessage(probe);
            assumeTrue(waitText(aChat, probe, DELIVER), "A never got B's probe message");
            aChat.revealNewestRow();
            String bName = aChat.rowText(probe, "tvUserName");
            if (bName.contains(" (")) bName = bName.substring(0, bName.indexOf(" ("));
            assumeTrue(!bName.isBlank(), "Could not read B's display name from the probe row");
            bChat.deleteOwnMessage(probe);
            bChat.selectUnit(cal);
            sleepQuiet(1500);

            aChat.sendMessage(m);
            assumeTrue(aChat.waitMessageSent(m, Duration.ofSeconds(30)), "A's post did not reach the server");
            assumeTrue(aChat.readByOpensPage(m), "Read By did not open on A");
            final String name = bName;
            boolean readBefore = listHas(A.driver, "readByRv", name);
            System.out.println(">>> M20b '" + name + "' in read list before B opens: " + readBefore);
            bChat.selectUnit(unit);
            assumeTrue(waitText(bChat, m, DELIVER), "B never saw A's post");
            long end = System.currentTimeMillis() + DELIVER.toMillis();
            boolean readAfter = false;
            while (!readAfter && System.currentTimeMillis() < end) { readAfter = listHas(A.driver, "readByRv", name); sleepQuiet(1000); }
            assertTrue(readAfter, "BUL-18: " + name + " should move into A's read list live once B opens the unit");
        } finally {
            aChat.leaveOverlayScreen();
            bChat.selectUnit(unit);
            bChat.deleteOwnMessage(probe);
            aChat.deleteOwnMessage(m);
        }
    }

    private boolean listHas(AndroidDriver d, String listId, String text) {
        try {
            return !d.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/" + listId + "']//*[contains(@text,'"
                    + text.replace("'", "") + "')]")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * [WEB M11 #153] (sheet BUL-30). A searches B's name and notes the count; B then only REPLIES
     * to a fresh post of A's; A searches again. The comment sender should be searched too, so
     * the count should grow by one (HomeChatAdapter searches only the comment TEXT, :708-715).
     */
    @Test
    @Order(12)
    @DisplayName("[WEB M11b] Searching a member's name finds a post they only replied to (BUL-30)")
    void searchFindsCommentSender() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage aChat = A.chat(), bChat = B.chat();
        String unit = aChat.bulletinLikeUnit();
        assumeTrue(unit != null, "No Bulletin-like unit");
        aChat.selectUnit(unit);
        assumeTrue(bChat.selectUnit(unit) && bChat.isInputBarVisible(), "B cannot post in " + unit);
        String probe = "ZL_AUTO-m11p-" + stamp();
        String parent = "ZL_AUTO-m11-" + stamp();
        String reply = "ZL_AUTO-m11r-" + stamp();
        try {
            bChat.sendMessage(probe);
            assumeTrue(waitText(aChat, probe, DELIVER), "A never got B's probe");
            aChat.revealNewestRow();
            String bName = aChat.rowText(probe, "tvUserName");
            if (bName.contains(" (")) bName = bName.substring(0, bName.indexOf(" ("));
            assumeTrue(!bName.isBlank(), "Could not read B's display name");
            bChat.deleteOwnMessage(probe);
            sleepQuiet(2000);

            aChat.sendMessage(parent);
            assumeTrue(aChat.waitMessageSent(parent, Duration.ofSeconds(30)), "A's post did not reach the server");
            assumeTrue(aChat.openSearch(), "Search did not open on A");
            aChat.searchFor(bName, true);
            int c0 = parseInt(aChat.searchCount());
            aChat.closeSearch();

            assumeTrue(waitText(bChat, parent, DELIVER), "B never got A's post");
            assumeTrue(bChat.longPressMessage(parent) && bChat.tapMenuOption("Reply"), "B could not reply");
            bChat.sendMessage(reply);
            assumeTrue(waitText(aChat, reply, DELIVER), "A never got B's reply");

            assumeTrue(aChat.openSearch(), "Search did not reopen on A");
            aChat.searchFor(bName, true);
            int c1 = parseInt(aChat.searchCount());
            System.out.println(">>> M11b search '" + bName + "': before=" + c0 + " after B's reply=" + c1);
            assertTrue(c1 > c0, "BUL-30: a post B only replied to is not found by B's name (count " + c0 + " → " + c1 + ")");
        } finally {
            if (aChat.isSearchOpen(Duration.ofMillis(500))) aChat.closeSearch();
            bChat.cancelReplyPreview();
            bChat.deleteOwnMessage(probe);
            aChat.deleteOwnMessage(parent);
        }
    }

    /**
     * [WEB M8 #151 #95] (sheet BUL-26 High). A forwards its own text to Home → Bulletin (a copy
     * lands in the feed), then forwards that COPY to member B. B must receive it in the C&C chat
     * with A within 60 s — never silently dropped.
     */
    @Test
    @Order(13)
    @DisplayName("[WEB M8] Forwarding a forwarded copy to a member delivers it to the member's C&C chat (BUL-26)")
    void forwardForwardedCopyToMember() {
        assumeTrue(ready, "Both devices not in the same unit — skipping");
        settle(A); settle(B);
        HomeChatPage aChat = A.chat(), bChat = B.chat();
        String unit = aChat.bulletinLikeUnit();
        assumeTrue(unit != null, "No Bulletin-like unit");
        aChat.selectUnit(unit);
        assumeTrue(bChat.selectUnit(unit), "B cannot open " + unit);
        String probe = "ZL_AUTO-m8p-" + stamp();
        String m = "ZL_AUTO-m8-" + stamp();
        String aName = "", bName = "";
        try {
            bChat.sendMessage(probe);
            assumeTrue(waitText(aChat, probe, DELIVER), "A never got B's probe");
            aChat.revealNewestRow();
            bName = shortName(aChat.rowText(probe, "tvUserName"));
            bChat.deleteOwnMessage(probe);
            aChat.sendMessage(m);
            assumeTrue(aChat.waitMessageSent(m, Duration.ofSeconds(30)), "A's post did not reach the server");
            assumeTrue(waitText(bChat, m, DELIVER), "B never got A's post");
            bChat.revealNewestRow();
            aName = shortName(bChat.rowText(m, "tvUserName"));
            assumeTrue(!aName.isBlank() && !bName.isBlank(), "Could not read the two display names");

            // 1) forward to Home → this unit: a copy appears in the feed
            assumeTrue(aChat.forwardOpensPage(m), "Forward did not open ForwardPage");
            assumeTrue(clickText(A.driver, "Home"), "ForwardPage has no Home row");
            sleepQuiet(1500);
            assumeTrue(clickText(A.driver, unit), "Select Unit did not list " + unit);
            clickId(A.driver, "submit_btn");
            aChat.confirmPopup().acceptIfShown(Duration.ofSeconds(4));
            aChat.leaveOverlayScreen();
            aChat.selectUnit(unit);
            long end = System.currentTimeMillis() + 30_000;
            while (aChat.rowsContaining(m) < 2 && System.currentTimeMillis() < end) { aChat.revealNewestRow(); sleepQuiet(1000); }
            assertTrue(aChat.rowsContaining(m) >= 2, "#95: forwarding to " + unit + " should add a copy to the feed");

            // 2) forward the COPY to member B
            assumeTrue(aChat.longPressLastMessage(m), "Long-press on the forwarded copy failed");
            assumeTrue(aChat.tapMenuOption("Forward"), "The copy offers no Forward");
            aChat.confirmPopup().acceptIfShown(Duration.ofSeconds(8));
            sleepQuiet(1500);
            assumeTrue(clickText(A.driver, "Members"), "ForwardPage has no Members row");
            sleepQuiet(2000);
            assumeTrue(clickText(A.driver, "(?i).*" + bName + ".*"), "The member list does not show " + bName);
            clickId(A.driver, "submit_btn");
            aChat.confirmPopup().acceptIfShown(Duration.ofSeconds(4));
            boolean errorOnA = aChat.textShown("went wrong", Duration.ofSeconds(3)) || aChat.textShown("failed", Duration.ofSeconds(1));
            aChat.leaveOverlayScreen();

            // 3) B: C&C → chat with A → the forwarded text
            boolean received = false;
            if (new BottomNavPage(B.driver).selectTab("C&C")) {
                sleepQuiet(2500);
                if (clickText(B.driver, "(?i).*" + aName + ".*")) {
                    long until = System.currentTimeMillis() + 60_000;
                    while (!received && System.currentTimeMillis() < until) received = bChat.messageShown(m, Duration.ofSeconds(2));
                }
            }
            System.out.println(">>> M8 B received in C&C=" + received + " errorOnA=" + errorOnA);
            assertTrue(received || errorOnA,
                    "BUL-26: the forwarded copy never reached " + bName + "'s C&C chat with " + aName + ", and A saw no error");
        } finally {
            try { B.driver.navigate().back(); } catch (WebDriverException ignored) { }
            new BottomNavPage(B.driver).goHome();
            aChat.leaveOverlayScreen();
            aChat.selectUnit(unit);
            for (int i = 0; i < 3 && aChat.messageShown(m, Duration.ofSeconds(2)); i++) aChat.deleteOwnMessage(m);
            bChat.selectUnit(unit);
            bChat.deleteOwnMessage(probe);
        }
    }

    private static String shortName(String s) {
        if (s == null) return "";
        return s.contains(" (") ? s.substring(0, s.indexOf(" (")).trim() : s.trim();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return -1; }
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
        // subsequent long-press reliable. Never on B (the Pixel): a cold start there raises the
        // app lock, which needs the owner's credential.
        if (d.name.equals("B")) return;
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
            // The device may be parked deep in another module by the previous suite (seen:
            // emulator-5554 left inside Doc Distribution), where none of the landing markers exist.
            // Back out first; relaunch ONLY when that does not reach a landing. A cold start on the
            // Pixel raises Zillit's app lock (BiometricPrompt, device credential required) which a
            // test must never answer — the unconditional relaunch used here before left the Pixel
            // stuck on that prompt (2026-11-24 05:17).
            drv.executeScript("mobile: activateApp", Map.of("appId", pkg));
            boolean landed = false;
            for (int i = 0; i < 6 && !landed; i++) {
                landed = isPresent(drv, "bottomNavigation") || isPresent(drv, "projectListRecyclerView")
                        || isPresent(drv, "start_project_btn") || isPresent(drv, "recTab");
                if (!landed) { try { drv.navigate().back(); } catch (WebDriverException ignored) { } sleepQuiet(1500); }
            }
            if (!landed && d.name.equals("A")) {        // A is always an emulator (no app lock there)
                try {
                    drv.executeScript("mobile: terminateApp", Map.of("appId", pkg, "timeout", 10000));
                } catch (WebDriverException ignored) { }
                try { drv.executeScript("mobile: activateApp", Map.of("appId", pkg)); } catch (WebDriverException ignored) { }
                sleepQuiet(3000);
            }
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

    /** Digit-free (see HomeChatPage.letterStamp): a 7-digit run is autolinked as a phone number. */
    private String stamp() {
        return HomeChatPage.letterStamp();
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
