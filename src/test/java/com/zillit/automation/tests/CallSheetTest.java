package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.MediaPreviewPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.UploadModePage;
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

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
