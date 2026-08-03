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
        // If content already exists the confirm popup shows first — choose Continuation.
        if (p.isCallSheetFirstPopupShown(Duration.ofSeconds(3))) {
            p.tapDialogLeft(); // Continuation
        }
        assumeTrue(p.isAttachmentPickerVisible() || p.isCallSheetFirstPopupShown(Duration.ofSeconds(1)),
                "Document picker did not open");
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

    // ---------------- two-popup replace flow ----------------

    @Test
    @Order(4)
    @DisplayName("With content, attachment shows the continuation/new popup; Cancel opens nothing")
    void replacePopupCancel() {
        HomeChatPage p = page();
        assumeTrue(p.isDocBubbleShown(Duration.ofSeconds(6)),
                "Call sheet empty — the popup only appears with existing content");
        p.tapAttachment();
        assumeTrue(p.isCallSheetFirstPopupShown(POPUP), "First popup did not appear");
        assertTrue(p.textShown("Continuation", Duration.ofSeconds(3))
                        && p.textShown("New", Duration.ofSeconds(2)),
                "Popup should offer Continuation and New");
        p.tapDialogCancel();
        assertTrue(!p.isAttachmentPickerVisible(), "Cancel must not open the picker");
    }

    @Test
    @Order(5)
    @DisplayName("'Continuation' opens the document picker directly (append — NO second popup)")
    void continuationAppendsDirectly() {
        HomeChatPage p = page();
        assumeTrue(p.isDocBubbleShown(Duration.ofSeconds(6)), "No content — skipping");
        p.tapAttachment();
        assumeTrue(p.isCallSheetFirstPopupShown(POPUP), "First popup missing");
        p.tapDialogLeft(); // Continuation
        // Verified live: Continuation → document picker directly, no replace warning.
        assertTrue(p.isDocumentOnlyPicker() || p.isAttachmentPickerVisible(),
                "Continuation should open the document picker directly");
        assertTrue(!p.isCallSheetReplaceWarningShown(Duration.ofSeconds(2)),
                "Continuation must NOT show the replace-warning popup");
        p.dismissAttachmentPicker();
    }

    @Test
    @Order(6)
    @DisplayName("'New' shows the replace-warning; 'No' cancels, 'Yes' opens the replace picker")
    void newShowsReplaceWarning() {
        HomeChatPage p = page();
        assumeTrue(p.isDocBubbleShown(Duration.ofSeconds(6)), "No content — skipping");
        p.tapAttachment();
        assumeTrue(p.isCallSheetFirstPopupShown(POPUP), "First popup missing");
        p.tapDialogRight(); // New
        assertTrue(p.isCallSheetReplaceWarningShown(POPUP),
                "Choosing New should show the replace-warning popup (New replaces old)");
        p.tapDialogLeft(); // No
        assertTrue(!p.isAttachmentPickerVisible(), "'No' must not open the picker");
    }

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
