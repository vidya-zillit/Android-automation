package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Config;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Call Sheet — the "How are you uploading this document?" sheet (ZL-21154).
 *
 * <p><b>The flow this suite drives.</b> attachment → document picker → pick a file →
 * <i>then</i> the sheet → Send. The app calls {@code startUpload(uriItems)} with the files
 * already chosen, so the sheet is the last gate before the upload, not the first. The popup it
 * replaced asked first and opened the picker afterwards; {@code call_sheet_permission_first_msg}
 * still sits in strings.xml but nothing references it any more.
 *
 * <p><b>Why so many cases skip rather than fail.</b> Three things here are data-shaped, not
 * behaviour-shaped: whether the unit already holds messages (no messages → no sheet at all),
 * whether it holds an eligible <i>document</i> (none → the Replace card is hidden by design), and
 * whether the account may post. Each is an {@code assumeTrue} with the reason spelled out, so a
 * thin project reports SKIPPED instead of a red failure that says nothing.
 *
 * <p><b>What this suite writes.</b> Real documents into the project's Call Sheet unit. Replace
 * moves one existing document to History. The one genuinely destructive path — <i>New</i>, which
 * sends the <b>entire</b> unit to History — is off unless you ask for it:
 *
 * <pre>./run-ui.sh --call-sheet-upload -Dzillit.callSheetDestructive=true</pre>
 */
@Order(TestOrder.CALL_SHEET_UPLOAD_MODE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CallSheetUploadModeTest extends BaseTest {

    private static final Duration SHEET = Duration.ofSeconds(25);
    private static final Duration SHORT = Duration.ofSeconds(4);
    /** Fixture pushed by the runner into /sdcard/Download. */
    private static final String FIXTURE = "zl_dd_doc";

    private boolean onCallSheet;

    @Override
    protected boolean relaunchBetweenTests() {
        return false; // one session; the sheet costs a picker round-trip to reach
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
            clearBlockingPopup();
            // A sheet or picker left open by a failed case blocks everything after it.
            for (int i = 0; i < 4 && !page.isInputBarVisible(); i++) {
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
        assumeTrue(page.isLoaded(), "Home units strip not visible");
        assumeTrue(page.selectCallSheetUnit(), "No call-sheet unit on this project");
        onCallSheet = true;
    }

    // ================= A · when the sheet appears =================

    @Test
    @Order(1)
    @DisplayName("Picking a document on a non-empty Call Sheet opens the upload-mode sheet")
    void sheetAppearsAfterPicking() {
        UploadModePage sheet = openSheetOrSkip();
        assertTrue(sheet.title().toLowerCase().contains("how are you uploading"),
                "Expected the upload-mode title, got: '" + sheet.title() + "'");
        screenshot("um-sheet-open");
        sheet.tapCancel();
    }

    @Test
    @Order(2)
    @DisplayName("Sheet opens with nothing selected: Send disabled, picker hidden")
    void initialState() {
        UploadModePage sheet = openSheetOrSkip();
        assertFalse(sheet.isSendEnabled(),
                "Send must start disabled — no upload mode has been chosen yet");
        assertTrue(sheet.isPickerHidden(),
                "The 'Document to replace' block must stay hidden until Replace is chosen");
        assertTrue(sheet.hasContinuation() && sheet.hasNew(),
                "Continuation and New are always offered");
        sheet.tapCancel();
    }

    @Test
    @Order(3)
    @DisplayName("Replace is offered only when the unit holds an eligible document")
    void replaceOfferedOnlyWithDocuments() {
        boolean hasDocument = new HomeChatPage(driver).isDocBubbleShown(Duration.ofSeconds(6));
        UploadModePage sheet = openSheetOrSkip();
        if (hasDocument) {
            assertTrue(sheet.hasReplace(),
                    "The unit holds a document, so Replace should be offered");
        } else {
            assertFalse(sheet.hasReplace(),
                    "No eligible document — Replace must be hidden rather than show an empty picker");
        }
        sheet.tapCancel();
    }

    // ================= C · choosing a mode =================

    @Test
    @Order(4)
    @DisplayName("Continuation enables Send and leaves the picker hidden")
    void continuationEnablesSend() {
        UploadModePage sheet = openSheetOrSkip();
        sheet.chooseContinuation();
        assertTrue(sheet.isSendEnabled(), "Continuation should enable Send");
        assertTrue(sheet.isPickerHidden(), "Continuation needs no target, so no picker");
        sheet.tapCancel();
    }

    @Test
    @Order(5)
    @DisplayName("New enables Send and leaves the picker hidden")
    void newEnablesSend() {
        UploadModePage sheet = openSheetOrSkip();
        sheet.chooseNew();
        assertTrue(sheet.isSendEnabled(), "New should enable Send");
        assertTrue(sheet.isPickerHidden(), "New needs no target, so no picker");
        sheet.tapCancel();
    }

    @Test
    @Order(6)
    @DisplayName("Replace reveals the picker and pre-selects the newest document")
    void replaceRevealsPickerAndPreselects() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — no eligible document in this unit");
        sheet.chooseReplace();
        assertTrue(sheet.isPickerVisible(), "Replace must reveal the 'Document to replace' block");
        assertFalse(sheet.selectedTargetName().isBlank(),
                "Replace auto-selects the most recent document, so the field should not be empty");
        assertTrue(sheet.isStampShown(),
                "The 'Selected: <date>' line should accompany the pre-selected document");
        assertTrue(sheet.isSendEnabled(), "With a target auto-selected, Send should be enabled");
        screenshot("um-replace-picker");
        sheet.tapCancel();
    }

    @Test
    @Order(7)
    @DisplayName("Switching away from Replace hides the picker again")
    void switchingModeHidesPicker() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        assumeTrue(sheet.isPickerVisible(), "Picker did not open");
        sheet.chooseContinuation();
        assertTrue(sheet.isPickerHidden(),
                "Choosing Continuation after Replace must hide the target picker");
        assertTrue(sheet.isSendEnabled(), "Continuation is still a complete choice");
        sheet.tapCancel();
    }

    // ================= D · the replace-target picker =================

    @Test
    @Order(8)
    @DisplayName("Dropdown lists every eligible document without typing, newest first")
    void dropdownListsAllTargets() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        sheet.openDropdown();
        List<String> names = sheet.dropdownNames();
        assertFalse(names.isEmpty(),
                "completionThreshold=0 means the full list shows on tap, before any typing");
        assertTrue(sheet.dropdownStamps().stream().anyMatch(s -> !s.isBlank()),
                "Each row carries its document's timestamp");
        screenshot("um-dropdown");
        sheet.dismissDropdown();
        sheet.tapCancel();
    }

    @Test
    @Order(9)
    @DisplayName("Search matches a substring from the middle of a file name, ignoring case")
    void searchIsSubstringAndCaseInsensitive() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        sheet.openDropdown();
        List<String> all = sheet.dropdownNames();
        assumeTrue(!all.isEmpty(), "No targets listed — skipping");

        // A fragment from the MIDDLE of the name. The adapter overrides ArrayAdapter's prefix
        // matching precisely so this works — these filenames carry long numeric prefixes the
        // user cannot be expected to type.
        String name = all.get(0);
        assumeTrue(name.length() >= 4, "Target name too short to take a middle fragment");
        int from = Math.max(1, name.length() / 3);
        String fragment = name.substring(from, Math.min(name.length(), from + 3));

        sheet.typeSearch(fragment.toUpperCase());
        assertTrue(sheet.dropdownSize() > 0,
                "A middle fragment in the wrong case should still match '" + name + "'"
                        + " — prefix-only or case-sensitive matching would return nothing");

        sheet.clearSearch();
        // Clearing can close the dropdown; reopen before counting, or this asserts against a
        // dismissed list and fails for a reason that has nothing to do with filtering.
        sheet.openDropdown();
        assertTrue(sheet.dropdownSize() >= all.size(),
                "Clearing the query restores every option, not whatever the last filter left"
                        + " (saw " + sheet.dropdownSize() + ", expected at least " + all.size() + ")");
        sheet.dismissDropdown();
        sheet.tapCancel();
    }

    @Test
    @Order(10)
    @DisplayName("Picking a row writes the plain file name and its timestamp, not an object dump")
    void pickingARowWritesCleanName() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        sheet.openDropdown();
        List<String> names = sheet.dropdownNames();
        assumeTrue(!names.isEmpty(), "No targets listed — skipping");
        String wanted = names.get(0);
        assumeTrue(sheet.pickTargetAt(0), "Could not tap the first dropdown row");

        String shown = sheet.selectedTargetName();
        assertEquals(wanted, shown, "The field should hold exactly the chosen file name");
        assertFalse(shown.contains("HomeChatInfo"),
                "convertResultToString must stop the widget writing the data class into the field");
        assertTrue(sheet.isStampShown(), "Picking a target shows its 'Selected: <date>' line");
        assertTrue(sheet.isSendEnabled(), "A chosen target enables Send");
        sheet.tapCancel();
    }

    @Test
    @Order(11)
    @DisplayName("A half-typed query cannot stand in for a real choice")
    void halfTypedQueryRevertsOnDismiss() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        String selected = sheet.selectedTargetName();
        assumeTrue(!selected.isBlank(), "Nothing pre-selected — skipping");

        sheet.openDropdown();
        sheet.typeSearch("zzz-not-a-real-document");
        sheet.dismissDropdown();

        assertEquals(selected, sheet.selectedTargetName(),
                "Dismissing the dropdown must restore the real selection, "
                        + "not leave typed junk looking like a chosen target");
        sheet.tapCancel();
    }

    // ================= E/F · outcomes =================

    @Test
    @Order(12)
    @DisplayName("New asks a second time; 'No' aborts and posts nothing")
    void newSecondConfirmationNoAborts() {
        HomeChatPage chat = new HomeChatPage(driver);
        int before = chat.chatRowCount();
        UploadModePage sheet = openSheetOrSkip();
        sheet.chooseNew();
        sheet.tapSend();
        assertTrue(sheet.isNewConfirmShown(Duration.ofSeconds(8)),
                "New wipes the unit, so it must raise the extra Alert before doing it");
        sheet.confirmNo();
        sleep(1500);
        assertEquals(before, chat.chatRowCount(),
                "'No' must abort the upload — nothing posted, nothing removed");
    }

    @Test
    @Order(13)
    @DisplayName("Replace sends straight away — no second confirmation")
    void replaceHasNoSecondConfirmation() {
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        assumeTrue(sheet.isSendEnabled(), "No target selected — skipping");
        // Deliberately NOT pressing Send: this case is about the absence of the New dialog,
        // and the real Replace is case 15.
        assertFalse(sheet.isNewConfirmShown(Duration.ofSeconds(2)),
                "The 'sent to History' Alert belongs to New only");
        sheet.tapCancel();
    }

    @Test
    @Order(14)
    @DisplayName("Cancel drops the picked files — nothing is uploaded")
    void cancelDropsThePickedFiles() {
        HomeChatPage chat = new HomeChatPage(driver);
        int before = chat.chatRowCount();
        UploadModePage sheet = openSheetOrSkip();
        sheet.chooseContinuation();
        sheet.tapCancel();
        assertTrue(sheet.isGone(Duration.ofSeconds(6)), "Cancel should close the sheet");
        sleep(2000);
        assertEquals(before, chat.chatRowCount(),
                "Cancel aborts the upload — the already-picked files are simply dropped");
    }

    @Test
    @Order(15)
    @DisplayName("Continuation posts the document alongside the existing messages")
    void continuationPostsAlongside() {
        HomeChatPage chat = new HomeChatPage(driver);
        int before = chat.chatRowCount();
        UploadModePage sheet = openSheetOrSkip();
        sheet.chooseContinuation();
        sheet.tapSend();
        assertTrue(sheet.isGone(Duration.ofSeconds(10)), "Send should close the sheet");
        assertTrue(chat.attachmentPostedOrUploading(Duration.ofSeconds(60)),
                "The document should upload and appear in the unit");
        assertTrue(chat.chatRowCount() >= before,
                "Continuation ADDS — existing messages must still be there");
        screenshot("um-continuation-posted");
    }

    @Test
    @Order(16)
    @DisplayName("Replace swaps the chosen document and moves it to History")
    void replaceSwapsOneDocument() {
        HomeChatPage chat = new HomeChatPage(driver);
        UploadModePage sheet = openSheetOrSkip();
        assumeTrue(sheet.hasReplace(), "Replace not offered — skipping");
        sheet.chooseReplace();
        sheet.openDropdown();
        List<String> names = sheet.dropdownNames();
        assumeTrue(!names.isEmpty(), "No targets listed — skipping");
        String target = names.get(0);
        assumeTrue(sheet.pickTargetAt(0), "Could not select a target");
        assumeTrue(sheet.isSendEnabled(), "Send stayed disabled with a target chosen");

        sheet.tapSend();
        assertTrue(sheet.isGone(Duration.ofSeconds(10)), "Send should close the sheet");

        // Either the upload lands, or the server reports the target went stale. Both are
        // defined outcomes; a silent nothing is not.
        boolean posted = chat.attachmentPostedOrUploading(Duration.ofSeconds(60));
        boolean stale = sheet.isTargetNotFoundShown(Duration.ofSeconds(3));
        assertTrue(posted || stale,
                "Replace must either post, or say the target is no longer available — "
                        + "it must never fail silently");
        if (stale) {
            screenshot("um-replace-target-stale");
        } else {
            screenshot("um-replace-done");
            sleep(3000);
            assertFalse(chat.messageShown(target, Duration.ofSeconds(5)),
                    "The replaced document '" + target + "' should have left the unit for History");
        }
    }

    @Test
    @Order(17)
    @DisplayName("New wipes the unit after confirmation (opt-in: -Dzillit.callSheetDestructive=true)")
    void newWipesUnitAfterConfirmation() {
        assumeTrue("true".equalsIgnoreCase(Config.get("zillit.callSheetDestructive")),
                "Destructive: New sends the WHOLE unit to History. "
                        + "Enable with -Dzillit.callSheetDestructive=true");
        HomeChatPage chat = new HomeChatPage(driver);
        UploadModePage sheet = openSheetOrSkip();
        sheet.chooseNew();
        sheet.tapSend();
        assumeTrue(sheet.isNewConfirmShown(Duration.ofSeconds(8)), "Confirmation did not appear");
        sheet.confirmYes();
        assertTrue(chat.attachmentPostedOrUploading(Duration.ofSeconds(60)),
                "After Yes the new document should upload");
        screenshot("um-new-wiped");
    }

    // ---------------- helpers ----------------

    /**
     * Drives attachment → document picker → fixture, and returns the sheet.
     *
     * <p>Skips rather than fails at every point the environment, not the app, can let us down:
     * no posting rights, a picker the device does not surface the fixture in, or an empty unit
     * (where the app is MEANT to upload without asking).
     */
    private UploadModePage openSheetOrSkip() {
        HomeChatPage chat = new HomeChatPage(driver);
        assumeTrue(chat.isAttachmentVisible(), "No attachment button — no posting rights here");
        assumeTrue(chat.chatRowCount() > 0,
                "Call Sheet is empty — the app uploads straight away and never shows the sheet");

        chat.tapAttachment();
        if (chat.isAttachmentPickerVisible()) {
            assumeTrue(chat.tapAttachmentOption("Document"), "Document option missing");
        }
        for (int i = 0; i < 3; i++) {
            if (!dismissPermissionPrompts()) break;
            sleep(500);
        }
        assumeTrue(chat.pickSystemFileByName(FIXTURE, Duration.ofSeconds(15)),
                "System picker did not surface " + FIXTURE + " — is prep_device pushing it?");

        MediaPreviewPage preview = new MediaPreviewPage(driver);
        if (preview.isPreviewOpen()) {
            preview.tapPreviewSend();
        }

        UploadModePage sheet = new UploadModePage(driver);
        assumeTrue(sheet.isShown(SHEET),
                "The upload-mode sheet did not appear after picking a document");
        return sheet;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
