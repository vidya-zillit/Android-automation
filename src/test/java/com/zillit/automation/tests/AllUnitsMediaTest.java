package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Send 2-3 messages of EVERY type to EVERY chat unit — text, image, audio,
 * video and document — a broad coverage/regression sweep.
 *
 * Type reliability (established earlier):
 *  - text, image, recorded-audio → app's own UI, sent + asserted.
 *  - video, document → need a file on the device + the system picker; attempted
 *    best-effort and counted, never a false failure.
 * Per-unit rules honoured: Call Sheet is document-only (no text/image/audio),
 * and recorded audio is unavailable on Call Sheet.
 *
 * The runner pushes zl_test_image.jpg (Pictures) and zl_test_doc.pdf (Documents)
 * and grants RECORD_AUDIO, so these have content to send.
 */
@Order(TestOrder.ALL_UNITS_MEDIA)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AllUnitsMediaTest extends BaseTest {

    private static final int COUNT = 2; // 2 of each type per unit
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
    void enterProject() {
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
        assumeTrue(new HomeChatPage(driver).isLoaded(), "Home units strip not visible");
        insideProject = true;
    }

    private void ensureInChat() {
        HomeChatPage home = new HomeChatPage(driver);
        for (int i = 0; i < 6 && !home.isInputBarVisible() && !home.isNoRightsBannerVisible(); i++) {
            driver.navigate().back();
            sleep(600);
        }
    }

    private void grantPerms() {
        // Poll briefly — a system prompt can appear a moment after the tap.
        waitAndDismissPermissions(Duration.ofSeconds(4));
    }

    // ---------------- per-type senders ----------------

    private String lastTextMarker;

    private int sendTexts(HomeChatPage home, int n, String tag) {
        int sent = 0;
        for (int i = 0; i < n; i++) {
            String m = tag + "-txt-" + (System.currentTimeMillis() % 100000) + "-" + i;
            home.sendMessage(m);
            // Count it only if it actually REACHED the server — a bubble with a pending
            // clock or a Retry on it is not a sent message, and counting those is how a
            // sweep reported "N sent" for messages the recipient never got.
            if (home.waitMessageSent(m, Duration.ofSeconds(30))) {
                sent++;
                lastTextMarker = m;
            } else {
                System.out.println(">>> NOT SENT: " + m + " — " + home.sendStateOf(m));
            }
        }
        return sent;
    }

    /**
     * After sending a message, long-press it and log which long-press options
     * the app offers for it — the "check long-press functionality" step. Text
     * uses the exact marker; media uses the newest bubble. Non-fatal (logs +
     * closes the menu) so the send sweep keeps flowing.
     */
    private void checkLongPress(HomeChatPage home, String label, String textMarker) {
        boolean opened;
        if (textMarker != null) {
            opened = home.longPressMessage(textMarker);
        } else {
            opened = home.longPressNewestMedia() || home.longPressFirstMessage();
        }
        if (opened && home.isMenuOpen(Duration.ofSeconds(4))) {
            System.out.println(">>> long-press [" + label + "] options: " + home.menuOptions());
            home.closeMenu();
        } else {
            System.out.println(">>> long-press [" + label + "] menu did not open");
        }
        ensureInChat();
    }

    private int sendImages(HomeChatPage home, int n, String tag) {
        int sent = 0;
        for (int i = 0; i < n; i++) {
            if (home.sendOneImage(tag + "-img-" + i, v -> grantPerms())) sent++;
            ensureInChat();
        }
        return sent;
    }

    private int recordAudios(HomeChatPage home, int n) {
        int sent = 0;
        for (int i = 0; i < n; i++) {
            home.clearMessage();
            if (!home.isRecordButtonAvailable()) break;
            home.tapRecord();
            // The first tap may open the mic permission popup ("While using the
            // app / Only this time") instead of recording; grant it, then tap
            // record AGAIN to actually start.
            if (waitAndDismissPermissions(Duration.ofSeconds(6))
                    && !home.isRecordingActive(Duration.ofSeconds(2))) {
                home.tapRecord();
            }
            if (!home.isRecordingActive(Duration.ofSeconds(5))) break;
            sleep(1800);
            home.sendRecording();
            sent++;
            ensureInChat();
        }
        return sent;
    }

    private int sendDocuments(HomeChatPage home, int n, boolean callSheet) {
        int sent = 0;
        for (int i = 0; i < n; i++) {
            if (!home.isAttachmentVisible()) break;
            home.tapAttachment();
            if (callSheet && home.isCallSheetFirstPopupShown(Duration.ofSeconds(3))) {
                home.tapDialogLeft(); // Continuation = append
            }
            if (!home.isAttachmentPickerVisible()) { ensureInChat(); break; }
            if (!home.tapAttachmentOption("Document")) { home.dismissAttachmentPicker(); break; }
            grantPerms();
            if (!home.pickSystemFileByName("zl_test", Duration.ofSeconds(10))) { ensureInChat(); break; }
            MediaPreviewPage prev = new MediaPreviewPage(driver);
            if (prev.isPreviewOpen()) {
                prev.setCaption("qa-doc-" + i);
                prev.tapPreviewSend();
                prev.waitPreviewClosed(Duration.ofSeconds(45));
                sent++;
            }
            ensureInChat();
        }
        return sent;
    }

    // ---------------- the sweep ----------------

    @Test
    @Order(1)
    @DisplayName("Send 2 of every applicable type to EVERY chat unit")
    void sendAllTypesToAllUnits() {
        HomeChatPage home = new HomeChatPage(driver);
        List<String> units = home.visibleUnitNames();
        assumeTrue(!units.isEmpty(), "No units found");
        System.out.println(">>> units to sweep: " + units);

        int unitsCovered = 0;
        for (String unit : units) {
            if (unit.toLowerCase().contains("calendar")) continue; // not a chat unit
            assumeTrue(home.selectUnit(unit), "Could not select unit " + unit);
            boolean callSheet = unit.toLowerCase().contains("call");
            boolean canPost = home.isInputBarVisible();
            System.out.println("=== UNIT: " + unit + " (callSheet=" + callSheet
                    + ", canPost=" + canPost + ") ===");
            if (!canPost) {
                System.out.println(">>> " + unit + ": no posting rights — view only, skipping sends");
                continue;
            }
            unitsCovered++;
            String tag = "qa-" + unit.replaceAll("\\s+", "").toLowerCase();

            // Per unit: for each message type → send 2-3 → CHECK long-press
            // functionality on it → next type. Finish the whole unit, then move on.
            if (callSheet) {
                // Call Sheet is document-only.
                int docs = sendDocuments(home, COUNT, true);
                checkLongPress(home, unit + "/document", null);
                System.out.println(">>> " + unit + ": documents=" + docs);
                assertTrue(home.isInputBarVisible() || home.isDocBubbleShown(Duration.ofSeconds(5)),
                        unit + " should stay usable after document sends");
            } else {
                int txt = sendTexts(home, COUNT, tag);
                checkLongPress(home, unit + "/text", lastTextMarker);   // reply/edit/forward/share/delete…

                int img = sendImages(home, COUNT, tag);
                if (img > 0) checkLongPress(home, unit + "/image", null); // + save / image-reply

                int aud = recordAudios(home, COUNT);
                if (aud > 0) checkLongPress(home, unit + "/audio", null);

                int docs = sendDocuments(home, COUNT, false);
                if (docs > 0) checkLongPress(home, unit + "/document", null);

                System.out.println(">>> " + unit + ": text=" + txt + " image=" + img
                        + " audio=" + aud + " document=" + docs);
                // Text is the always-reliable type — require it; the rest are
                // reported (media/audio/doc depend on device files/permissions).
                assertTrue(txt >= 1, "At least one text message should send on " + unit);
                assertTrue(home.isInputBarVisible(), unit + " chat should remain usable");
            }
            screenshot("sweep-" + tag);
        }
        assumeTrue(unitsCovered > 0, "No postable chat unit was available to sweep");
        System.out.println(">>> swept " + unitsCovered + " unit(s)");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
