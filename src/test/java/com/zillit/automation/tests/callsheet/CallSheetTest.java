package com.zillit.automation.tests.callsheet;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import com.zillit.automation.pages.common.MediaPreviewPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.callsheet.UploadModePage;
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
 * Call-sheet unit — its distinct workflow, all verified live on the emulator:
 *  - attachment picker is DOCUMENT-ONLY (no Photo/Gallery/Video/etc.)
 *  - a document is sent via the system picker → preview → PDF bubble w/ file size
 *  - a History button (userHistoryBtn) views replaced call sheets
 *  - once content exists, tapping attachment shows a two-popup replace flow
 *    (Continuation / New / Cancel → New → replace-warning No / Yes)
 *  - long-press options are the call-sheet set: Download, Gallery,
 *    "Publish to Doc Distribution", Read By User, Reply, Delete —
 *    Forward and Share are HIDDEN on call sheet.
 *
 * Requires a project with a call-sheet unit + posting rights. The runner pushes
 * zl_test_doc.pdf so the document send has content. Skips cleanly otherwise.
 */
@Order(TestOrder.CALL_SHEET)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CallSheetTest extends BaseTest {

    private static final Duration POPUP = Duration.ofSeconds(6);
    private boolean onCallSheet;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void reachCallSheet() {
        HomeChatPage page = new HomeChatPage(driver);
        if (onCallSheet) {
            dismissPermissionPrompts();
            // A modal confirmation left over from the previous case blocks every
            // tap and back-press — clear it or the rest of the suite skips.
            clearBlockingPopup();
            // Clear any leftover popup/menu.
            for (int i = 0; i < 3 && !page.isInputBarVisible(); i++) {
                driver.navigate().back();
                sleep(500);
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
        assumeTrue(page.isLoaded(), "Home units strip not visible");
        assumeTrue(page.selectCallSheetUnit(), "No call-sheet unit on this project");
        onCallSheet = true;
    }

    private HomeChatPage page() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isAttachmentVisible(), "No attachment on call sheet (no posting rights) — skipping");
        return p;
    }

    // ---------------- structure ----------------

    @Test
    @Order(1)
    @DisplayName("Call-sheet unit shows the History button")
    void historyButtonPresent() {
        HomeChatPage p = new HomeChatPage(driver);
        assertTrue(p.isHistoryButtonVisible(),
                "The call-sheet History button (userHistoryBtn) should be visible");
        // [WEB CS #1] the Call Sheet tab shows the composer too, not only History.
        assertTrue(p.isInputBarVisible() || p.isNoRightsBannerVisible(),
                "The call-sheet unit should show the composer (etMessage) or the no-rights banner");
        screenshot("callsheet-home");
    }

    // ---------------- CREATE: send a document ----------------

    @Test
    @Order(2)
    @DisplayName("CREATE: send a document — picker is document-only, PDF posts with file size")
    void sendDocument() {
        HomeChatPage p = page();
        p.tapAttachment();
        assumeTrue(p.isAttachmentPickerVisible(), "Document picker did not open");
        // Picker must be document-only.
        if (p.isAttachmentPickerVisible()) {
            assertTrue(p.isDocumentOnlyPicker(),
                    "Call-sheet picker must offer only Document, got: " + p.attachmentOptions());
            assumeTrue(p.tapAttachmentOption("Document"), "Document option missing");
        }
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        assumeTrue(p.pickSystemFileByName("zl_test", Duration.ofSeconds(12)),
                "System document picker could not surface the test PDF — skipping");
        // Document goes through the preview → send.
        MediaPreviewPage prev = new MediaPreviewPage(driver);
        if (prev.isPreviewOpen()) {
            prev.tapPreviewSend();
        }
        // ZL-21154: with content already in the unit the upload-mode sheet is the LAST gate
        // before the upload. Left unanswered it stays on screen, and every later case fails
        // against a sheet rather than the chat.
        UploadModePage mode = new UploadModePage(driver);
        if (mode.isShown(Duration.ofSeconds(15))) {
            mode.chooseContinuation();
            mode.tapSend();
        }
        assertTrue(p.isDocBubbleShown(Duration.ofSeconds(45)),
                "The document should appear as a bubble with a file size");
        screenshot("callsheet-document-sent");
    }

    // ---------------- long-press options (call-sheet set) ----------------

    @Test
    @Order(3)
    @DisplayName("Document options: Publish to Doc Distribution present; Forward & Share HIDDEN")
    void documentOptions() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isDocBubbleShown(Duration.ofSeconds(6)),
                "No document in the call sheet to long-press — run the send first");
        assumeTrue(p.longPressNewestMedia() || p.longPressFirstMessage(),
                "Long-press on the document did not register");
        assumeTrue(p.isMenuOpen(Duration.ofSeconds(6)), "Options menu did not open");
        List<String> opts = p.menuOptions();
        System.out.println(">>> call-sheet doc options: " + opts);
        assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("doc distribution")),
                "Call-sheet doc should offer 'Publish to Doc Distribution'. Got: " + opts);
        assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Forward")),
                "Forward must be HIDDEN on call sheet. Got: " + opts);
        assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Share")),
                "Share must be HIDDEN on call sheet. Got: " + opts);
        screenshot("callsheet-doc-options");
        p.closeMenu();
    }

    // ---------------- upload-mode sheet (ZL-21154) ----------------
    //
    // Three cases lived here that drove the OLD two-popup flow: tap attachment, answer
    // "Continuation / New / Cancel", THEN pick a file. ZL-21154 reversed that — the files are
    // chosen first and the sheet is the last gate before the upload — and replaced the dialog
    // with a bottom sheet that also offers Replace.
    //
    // They were not failing. They detected the old dialog by its text
    // ("continuation of the existing Call Sheet"), that string is no longer referenced by any
    // code, so the detection returned false and all three skipped on every run — silently
    // reporting nothing about a feature that had changed underneath them.
    //
    // Coverage now lives in CallSheetUploadModeTest, against the real umContinuation / umNew /
    // umReplace ids.

    // ---------------- History (Read) ----------------

    @Test
    @Order(7)
    @DisplayName("READ: History button opens the call-sheet history")
    void openHistory() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isHistoryButtonVisible(), "History button not visible");
        p.tapHistory();
        // A history screen/sheet opens; the input bar should no longer be primary.
        boolean opened = p.textShown("History", Duration.ofSeconds(6))
                || !p.isInputBarVisible();
        assertTrue(opened, "History should open a history view");
        screenshot("callsheet-history");
        driver.navigate().back();
    }

    // ---------------- DELETE ----------------

    @Test
    @Order(8)
    @DisplayName("DELETE: remove a call-sheet document")
    void deleteDocument() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isDocBubbleShown(Duration.ofSeconds(6)), "No document to delete");
        assumeTrue(p.longPressNewestMedia() || p.longPressFirstMessage(), "Long-press failed");
        assumeTrue(p.isMenuOpen(Duration.ofSeconds(6)), "Menu did not open");
        assumeTrue(p.tapMenuOption("Delete"), "Delete option missing");
        // Two-stage delete: menu Delete -> action-bar Delete -> Yes/No dialog.
        p.tapDeleteActionBar(Duration.ofSeconds(6));
        // Wait for the "Are you sure you want to delete?" (Yes) dialog and confirm.
        assertTrue(confirmYesDialog(Duration.ofSeconds(8)),
                "The delete-confirmation dialog (Yes) should appear and be clickable");
        sleep(1500);
        screenshot("callsheet-deleted");
    }

    // ---------------- [WEB CS-M1] TEXT messages on the Call Sheet unit ----------------
    //
    // The composer is registered for every unit (Home.kt:405-408); on Call Sheet only the mic
    // is hidden and the picker is document-only. No case had ever posted, edited, replied to
    // or deleted a TEXT message here (AllUnitsMediaTest assumed "document-only").
    // Web #2 #3 #4 #5 #6 #7. Every row is ZL_AUTO-prefixed and deleted in CS-M1f.

    private final String csTs = HomeChatPage.letterStamp();   // digit-free: see letterStamp()
    private final String csPost = "ZL_AUTO cs-post " + csTs;
    private final String csEdited = csPost + " EDITED";
    private final String csReply = "ZL_AUTO cs-reply " + csTs;
    private final String csEmoji = "ZL_AUTO cs-emoji " + csTs + " \uD83C\uDFAC\uD83D\uDD25\u2728\uD83D\uDC4D";
    private boolean csPosted;

    private HomeChatPage composer() {
        HomeChatPage p = new HomeChatPage(driver);
        assumeTrue(p.isInputBarVisible(), "No composer on call sheet (no posting rights) — skipping");
        return p;
    }

    @Test
    @Order(20)
    // [WEB CS-M1 #2]
    @DisplayName("[WEB CS-M1] TEXT: a typed message posts on the Call Sheet unit and the composer clears")
    void textPosts() {
        HomeChatPage p = composer();
        p.sendMessage(csPost);
        csPosted = true;
        assertTrue(p.waitMessageSent(csPost, Duration.ofSeconds(30)),
                "The call-sheet text should reach the server — it is " + p.sendStateOf(csPost));
        assertTrue(p.composerText().isEmpty(), "The composer should be empty after Send, has '" + p.composerText() + "'");
    }

    @Test
    @Order(21)
    // [WEB CS-M1 #3]
    @DisplayName("[WEB CS-M1] TEXT options: Reply / Edit / Delete; no Forward, Share or Publish to Doc Distribution")
    void textOptions() {
        HomeChatPage p = composer();
        assumeTrue(csPosted && p.messageShown(csPost, Duration.ofSeconds(5)), "The call-sheet text was not posted");
        assumeTrue(p.longPressMessage(csPost), "Long-press on the call-sheet text failed");
        List<String> opts = p.menuOptions();
        System.out.println(">>> call-sheet TEXT options: " + opts);
        try {
            for (String want : List.of("Reply", "Edit", "Delete")) {
                assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase(want)), "Call-sheet text should offer " + want + ": " + opts);
            }
            assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Forward")), "Forward must be hidden on call sheet: " + opts);
            assertTrue(opts.stream().noneMatch(o -> o.equalsIgnoreCase("Share")), "Share must be hidden on call sheet: " + opts);
            assertTrue(opts.stream().noneMatch(o -> o.toLowerCase().contains("doc distribution")),
                    "Publish to Doc Distribution is for documents only (Home.kt isShowDistributeToDD): " + opts);
        } finally {
            p.closeMenu();
        }
    }

    @Test
    @Order(22)
    // [WEB CS-M1 #4]
    @DisplayName("[WEB CS-M1] TEXT edit: Update shows the new text marked Edited")
    void textEdit() {
        HomeChatPage p = composer();
        assumeTrue(csPosted && p.messageShown(csPost, Duration.ofSeconds(5)), "The call-sheet text was not posted");
        assumeTrue(p.longPressMessage(csPost), "Long-press failed");
        assertTrue(p.tapMenuOption("Edit"), "Edit should be offered");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(6)), "The edit dialog should open");
        p.setEditDialogTextNoBack(csEdited);
        p.saveEditDialog();
        assertTrue(p.messageShown(csEdited, Duration.ofSeconds(15)), "The edited call-sheet text should show");
        sleep(1500);
        assertTrue(p.rowTime(csEdited).startsWith("Edited"),
                "The edited row should be marked Edited, tvMsgTime='" + p.rowTime(csEdited) + "'");
    }

    @Test
    @Order(23)
    // [WEB CS-M1 #5]
    @DisplayName("[WEB CS-M1] TEXT reply: reply bar quotes the row, paperclip and mic hidden, reply nests under it")
    void textReply() {
        HomeChatPage p = composer();
        assumeTrue(p.messageShown(csEdited, Duration.ofSeconds(5)), "The edited call-sheet text is not there");
        assumeTrue(p.longPressMessage(csEdited), "Long-press failed");
        assertTrue(p.tapMenuOption("Reply"), "Reply should be offered");
        try {
            assertTrue(p.replyBarShown(Duration.ofSeconds(5)), "The reply bar should open");
            assertTrue(p.replyBarQuote().contains(csPost), "The reply bar should quote the row: '" + p.replyBarQuote() + "'");
            assertTrue(!p.isAttachmentVisible(), "The paperclip must be hidden while replying");
            assertTrue(!p.isRecordButtonVisible(), "The mic must be hidden while replying");
            p.sendMessage(csReply);
            sleep(5000);
            assertTrue(p.replyUnderRow(csEdited, csReply), "The reply should nest under the call-sheet row");
        } finally {
            p.cancelReplyPreview();
        }
    }

    @Test
    @Order(24)
    // [WEB CS-M1 #7]
    @DisplayName("[WEB CS-M1] TEXT emoji: a message with four emoji posts them all")
    void textEmoji() {
        HomeChatPage p = composer();
        p.sendMessage(csEmoji);
        assertTrue(p.waitMessageSent("ZL_AUTO cs-emoji " + csTs, Duration.ofSeconds(30)),
                "The emoji text should post — it is " + p.sendStateOf("ZL_AUTO cs-emoji " + csTs));
        String row = p.rowMessage("ZL_AUTO cs-emoji " + csTs);
        for (String e : new String[]{"\uD83C\uDFAC", "\uD83D\uDD25", "\u2728", "\uD83D\uDC4D"}) {
            assertTrue(row.contains(e), "The row should contain " + e + ", got '" + row + "'");
        }
    }

    @Test
    @Order(25)
    // [WEB CS-M1 #6] (strengthens web #6: the row must actually disappear)
    @DisplayName("[WEB CS-M1] TEXT delete: the row is gone after Delete → Yes")
    void textDelete() {
        HomeChatPage p = composer();
        String target = p.messageShown(csEdited, Duration.ofSeconds(3)) ? csEdited : csPost;
        assumeTrue(p.messageShown(target, Duration.ofSeconds(3)), "No call-sheet text to delete");
        assumeTrue(p.longPressMessage(target), "Long-press failed");
        assertTrue(p.tapMenuOption("Delete"), "Delete should be offered");
        p.tapDeleteActionBar(Duration.ofSeconds(6));
        assertTrue(confirmYesDialog(Duration.ofSeconds(8)), "The delete confirmation (Yes) should appear");
        sleep(3000);
        assertTrue(!p.messageShown(target, Duration.ofSeconds(2)), "The deleted call-sheet text must be gone");
        // cleanup of the emoji row
        p.deleteOwnMessage("ZL_AUTO cs-emoji " + csTs);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
