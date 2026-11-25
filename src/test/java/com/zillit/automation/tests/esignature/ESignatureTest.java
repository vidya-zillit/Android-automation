package com.zillit.automation.tests.esignature;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.esignature.ESignComposePage;
import com.zillit.automation.pages.esignature.ESignCreateSignatureDialog;
import com.zillit.automation.pages.esignature.ESignaturePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Z-E-SIGNATURE — Tools → "E-Signature" ({@code DocSignMainActivity}). Cases and ids follow
 * {@code E_SIGNATURE_TESTCASES.md}.
 *
 * <p><b>Nothing is ever sent.</b> Android has no void and no delete for a sent envelope (only a
 * DRAFT has Delete — {@code DocuSignListFragment.promptDelete} returns early for anything else),
 * so a sent ZL_AUTO envelope would stay in both users' lists for good. The suite builds a real
 * envelope — PDF, signer, a placed field — stops at the "Send this document?" confirm with
 * Cancel, saves it as a DRAFT, exercises the draft, and deletes it. Two-device signing is
 * therefore not covered here.
 *
 * <p>Saved signatures are per USER, not per project, and carry no name (a tile reads "Choose
 * signature"), so the suite only creates them when the user starts with none of that kind, and
 * ES-99 deletes back to the count it found.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.E_SIGNATURE)
class ESignatureTest extends ESignatureBaseTest {

    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    private static final String PDF = "zl_test_doc.pdf";
    /** The per-type limit the app enforces ({@code DocuSignManageSignaturesViewModel.maxPerType}). */
    private static final int ENFORCED_SIG_CAP = 3;

    private final String title = "ZL_AUTO_ESIGN_" + RUN;

    /** Saved-signature counts found at ES-20; -1 = unknown. */
    private int baseSigs = -1, baseInitials = -1;
    private int madeSigs = 0, madeInitials = 0;

    /** How far the envelope build got: 1 PDF, 2 signer, 3 placement, 4 field, 5 draft saved. */
    private int built = 0;
    private String signerName;

    // ═══════════════════════════ 1. shell · ES-0x ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("ES-01 The tool opens on Manage Envelopes with four segments, Signatures and the upload button")
    void moduleOpens() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        assertEquals(List.of(ESignaturePage.SEG_MANAGE, ESignaturePage.SEG_SIGN,
                        ESignaturePage.SEG_TEMPLATES, ESignaturePage.SEG_BULK),
                page.segmentLabels(), "Segment chips");
        assertTrue(page.segmentSelected(ESignaturePage.SEG_MANAGE), "Manage Envelopes is not selected on opening");
        assertTrue(page.textShown("Signatures", Duration.ofSeconds(3)), "No \"Signatures\" button in the header");
        assertTrue(page.uploadFabShown(), "No \"" + ESignaturePage.FAB_UPLOAD + "\" button on Manage Envelopes");
        assertTrue(page.textShown(ESignaturePage.FAB_UPLOAD, Duration.ofSeconds(2)), "Upload button label");
    }

    @Test
    @Order(20)
    @DisplayName("ES-02 Manage Envelopes: three top tabs, Sent/Draft only under Z-E-Signature, Sent first")
    void topAndSubTabs() {
        ESignaturePage page = openManage(null);
        assertTrue(page.textShown(ESignaturePage.TOP_ESIGN, Duration.ofSeconds(2))
                && page.textShown(ESignaturePage.TOP_COMPLETE, Duration.ofSeconds(1))
                && page.textShown(ESignaturePage.TOP_REJECTED, Duration.ofSeconds(1)), "Top tabs");
        assertTrue(page.subTabsShown(), "Sent / Draft sub-tabs missing under Z-E-Signature");
        assertTrue(page.searchBoxShown() && page.textShown("Search envelopes", Duration.ofSeconds(2)),
                "No \"Search envelopes…\" box");
        try {
            page.openTopTab(ESignaturePage.TOP_COMPLETE);
            assertFalse(page.subTabsShown(), "Sent / Draft still shown under Complete");
            page.openTopTab(ESignaturePage.TOP_REJECTED);
            assertFalse(page.subTabsShown(), "Sent / Draft still shown under Rejected");
        } finally {
            page.openTopTab(ESignaturePage.TOP_ESIGN);
        }
        assertTrue(page.subTabsShown(), "Sent / Draft did not come back on Z-E-Signature");
    }

    @Test
    @Order(30)
    @DisplayName("ES-03 Every bucket shows its rows or its own empty message")
    void bucketsShowRowsOrEmpty() {
        ESignaturePage page = openManage(ESignaturePage.SUB_SENT);
        List<String> problems = new ArrayList<>();
        checkBucket(page, "Sent", ESignaturePage.EMPTY_SENT, problems);
        page.openSubTab(ESignaturePage.SUB_DRAFT);
        checkBucket(page, "Draft", ESignaturePage.EMPTY_DRAFTS, problems);
        page.openTopTab(ESignaturePage.TOP_COMPLETE);
        checkBucket(page, "Complete", ESignaturePage.EMPTY_COMPLETE, problems);
        page.openTopTab(ESignaturePage.TOP_REJECTED);
        checkBucket(page, "Rejected", ESignaturePage.EMPTY_REJECTED, problems);
        page.openTopTab(ESignaturePage.TOP_ESIGN);
        assertTrue(problems.isEmpty(), String.join("; ", problems));
    }

    private void checkBucket(ESignaturePage page, String name, String emptyCopy, List<String> problems) {
        List<String> rows = page.envelopeTitles();
        String empty = page.emptyText();
        System.out.println(">>> " + name + ": rows=" + rows + " empty=" + empty);
        if (rows.isEmpty() && empty == null) problems.add(name + " shows neither rows nor an empty message");
        if (rows.isEmpty() && empty != null && !emptyCopy.equals(empty)
                && !empty.startsWith("No envelopes match")) {
            problems.add(name + " empty message is \"" + empty + "\", expected \"" + emptyCopy + "\"");
        }
    }

    @Test
    @Order(40)
    @DisplayName("ES-04 [finding] The empty Draft message points at a \"+\" button the screen does not have")
    void draftEmptyCopyMatchesUi() {
        ESignaturePage page = openManage(ESignaturePage.SUB_DRAFT);
        String empty = page.emptyText();
        assumeTrue(empty != null, "Draft is not empty on this project — the message is not shown");
        // The only create control is the "Upload Doc for Z-E-Signature" button; the "+" menu
        // was removed from the list (see the comment over bindNewMenu in DocuSignListFragment).
        // Exact text: textContains("+") would match the empty message itself.
        boolean plusShown = !driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                "new UiSelector().textMatches(\"\\\\+.*\")")).isEmpty();
        assertFalse(empty.contains("Tap +") && !plusShown,
                "Draft empty state says \"" + empty + "\" but there is no + button — the create action"
                        + " is \"" + ESignaturePage.FAB_UPLOAD + "\"");
    }

    @Test
    @Order(50)
    @DisplayName("ES-05 Search: a query that matches nothing empties the list, clearing it restores the list")
    void searchFilters() {
        ESignaturePage page = openManage(ESignaturePage.SUB_SENT);
        List<String> before = page.envelopeTitles();
        String emptyBefore = page.emptyText();
        try {
            page.search("zzqq_no_such_envelope_" + RUN);
            assertEquals("zzqq_no_such_envelope_" + RUN, page.searchText(), "The box did not keep the query");
            assertTrue(page.envelopeTitles().isEmpty(), "Rows still listed for a query matching nothing: "
                    + page.envelopeTitles());
            assertTrue(page.emptyStateShown(), "No empty state for a query matching nothing");
        } finally {
            page.clearSearch();
        }
        assertEquals(before, page.envelopeTitles(), "Clearing the search did not restore the list");
        assertEquals(emptyBefore, page.emptyText(), "Empty message changed after clearing the search");
    }

    @Test
    @Order(60)
    @DisplayName("ES-06 The list / grid toggle switches the layout and back")
    void viewModeToggle() {
        ESignaturePage page = openManage(ESignaturePage.SUB_SENT);
        assertTrue(page.toggleViewMode(), "View-mode toggle could not be tapped");
        page.waitListSettled();
        assertTrue(page.listShown(Duration.ofSeconds(5)), "List screen lost after toggling the view mode");
        assertTrue(page.searchBoxShown(), "Search box gone in the other view mode");
        assertTrue(page.toggleViewMode(), "View-mode toggle could not be tapped back");
        page.waitListSettled();
        assertTrue(page.listShown(Duration.ofSeconds(5)), "List screen lost after toggling back");
        assertTrue(page.searchBoxShown(), "Search box gone after toggling back");
    }

    @Test
    @Order(70)
    @DisplayName("ES-07 The upload button shows on Manage Envelopes only")
    void fabOnlyOnManage() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        List<String> wrong = new ArrayList<>();
        try {
            for (String seg : List.of(ESignaturePage.SEG_SIGN, ESignaturePage.SEG_TEMPLATES, ESignaturePage.SEG_BULK)) {
                page.openSegment(seg);
                sleep(800);
                if (page.uploadFabShown()) wrong.add(seg);
            }
        } finally {
            page.openSegment(ESignaturePage.SEG_MANAGE);
        }
        assertTrue(wrong.isEmpty(), "Upload button still shown on: " + wrong);
        assertTrue(page.uploadFabShown(), "Upload button did not come back on Manage Envelopes");
    }

    @Test
    @Order(80)
    @DisplayName("ES-08 Sign Documents: Action Required / Complete / Rejected, rows or \"No documents waiting…\"")
    void signDocumentsSegment() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        try {
            assertTrue(page.openSegment(ESignaturePage.SEG_SIGN), "Sign Documents chip could not be tapped");
            assertTrue(page.receivedTabsShown(Duration.ofSeconds(15)), "Received tabs did not show");
            assertEquals(List.of("Action Required", "Complete", "Rejected"), page.receivedTabLabels(), "Received tabs");
            List<String> rows = page.envelopeTitles();
            String empty = page.receivedEmptyTitle();
            System.out.println(">>> Action Required: rows=" + rows + " empty=" + empty);
            assertTrue(!rows.isEmpty() || "No documents waiting for your signature".equals(empty),
                    "Action Required shows neither rows nor its empty message (got \"" + empty + "\")");
            for (String tab : List.of("tabComplete", "tabRejected")) {
                page.openReceivedTab(tab);
                assertTrue(!page.envelopeTitles().isEmpty() || page.receivedEmptyTitle() != null,
                        tab + " shows neither rows nor an empty message");
            }
        } finally {
            page.openSegment(ESignaturePage.SEG_MANAGE);
        }
    }

    @Test
    @Order(90)
    @DisplayName("ES-09 Templates: search, + New Template, and templates or \"No templates yet…\"")
    void templatesSegment() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        try {
            assertTrue(page.openSegment(ESignaturePage.SEG_TEMPLATES), "Templates chip could not be tapped");
            assertTrue(page.templatesShown(Duration.ofSeconds(15)), "Templates screen did not show");
            assertTrue(page.textShown("+ New Template", Duration.ofSeconds(3)), "No \"+ New Template\"");
            assertTrue(page.textShown("Search templates…", Duration.ofSeconds(2)), "No template search box");
        } finally {
            page.openSegment(ESignaturePage.SEG_MANAGE);
        }
    }

    @Test
    @Order(100)
    @DisplayName("ES-10 Bulk Sends: list / grid buttons, and jobs or \"No bulk sends yet\"")
    void bulkSendsSegment() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        try {
            assertTrue(page.openSegment(ESignaturePage.SEG_BULK), "Bulk Sends chip could not be tapped");
            assertTrue(page.bulkSendsShown(Duration.ofSeconds(15)), "Bulk Sends screen did not show");
            sleep(1500);
            boolean jobsOrEmpty = page.textShown("No bulk sends yet", Duration.ofSeconds(3))
                    || !driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/rvJobs\").childSelector("
                            + "new UiSelector().index(0))")).isEmpty();
            assertTrue(jobsOrEmpty, "Bulk Sends shows neither jobs nor \"No bulk sends yet\"");
        } finally {
            page.openSegment(ESignaturePage.SEG_MANAGE);
        }
    }

    // ═══════════════════════════ 2. saved signatures · ES-2x ═══════════════════════════

    @Test
    @Order(200)
    @DisplayName("ES-20 Signatures opens \"My Saved Signatures\" with both sections and Draw New")
    void signaturesSheetOpens() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        assertTrue(page.openSignaturesSheet(), "\"" + ESignaturePage.MANAGE_SHEET_TITLE + "\" did not open");
        try {
            assertEquals("Draw a new signature or initial, or long-press a saved one to delete it.",
                    page.sheetSubtitle(), "Sheet subtitle");
            assertEquals("Done", page.sheetButton(), "Sheet button");
            assertTrue(page.textShown("YOUR SIGNATURES", Duration.ofSeconds(2)), "No YOUR SIGNATURES section");
            assertTrue(page.textShown("YOUR INITIALS", Duration.ofSeconds(2)), "No YOUR INITIALS section");
            baseSigs = ESignaturePage.countOf(page.signatureCountLabel());
            baseInitials = ESignaturePage.countOf(page.initialCountLabel());
            System.out.println(">>> saved signatures=" + page.signatureCountLabel()
                    + " initials=" + page.initialCountLabel());
            assertTrue(baseSigs >= 0 && baseInitials >= 0, "Counters unreadable: "
                    + page.signatureCountLabel() + " / " + page.initialCountLabel());
            assertEquals(baseSigs, page.savedTileLabels(false).size(), "Signature tiles vs counter");
        } finally {
            page.tapSheetDone();
        }
    }

    @Test
    @Order(210)
    @DisplayName("ES-21 Draw New opens the two-step creator: Type / Draw, no Upload; Back and Close")
    void createDialogSteps() {
        ESignCreateSignatureDialog d = openCreator(false);
        try {
            assertEquals(ESignCreateSignatureDialog.TITLE, d.title(), "Dialog title");
            assertEquals("STEP 1 OF 2", d.stepEyebrow(), "Step eyebrow");
            assertEquals("How would you like to make your signature?", d.stepTitle(), "Step 1 title");
            assertEquals("Type your signature", d.typeCardTitle(), "Type card");
            assertEquals("Draw your signature", d.drawCardTitle(), "Draw card");
            assertFalse(d.uploadCardShown(), "Upload card shown although SHOW_UPLOAD_METHOD is false");
            d.chooseType();
            assertEquals("STEP 2 OF 2", d.stepEyebrow(), "Step eyebrow after choosing Type");
            assertTrue(d.typePaneShown(), "Type pane not shown");
            assertEquals("Use this signature", d.useLabel(), "Footer button");
            d.tapBackStep();
            assertEquals("STEP 1 OF 2", d.stepEyebrow(), "Back did not return to step 1");
            d.close();
            assertTrue(d.isGone(Duration.ofSeconds(4)), "Close did not dismiss the creator");
        } finally {
            closeAnyDialog();
            closeAnyDialog();
        }
    }

    @Test
    @Order(220)
    @DisplayName("ES-22 Type pane: \"Use this signature\" with no name warns \"Please type a name first\"")
    void typeEmptyWarns() {
        ESignCreateSignatureDialog d = openCreator(false);
        try {
            d.chooseType();
            assumeTrue(d.typePaneShown(), "Type pane did not open");
            d.tapUse();
            assertTrue(d.messageShown(ESignCreateSignatureDialog.WARN_TYPE, Duration.ofSeconds(4)),
                    "No \"" + ESignCreateSignatureDialog.WARN_TYPE + "\" toast");
            assertTrue(d.typePaneShown(), "The creator closed on an empty name");
        } finally {
            d.close();
            closeAnyDialog();
        }
    }

    @Test
    @Order(230)
    @DisplayName("ES-23 Draw pane: \"Use this signature\" on an empty pad warns \"Please draw a signature first\"")
    void drawEmptyWarns() {
        ESignCreateSignatureDialog d = openCreator(false);
        try {
            d.chooseDraw();
            assumeTrue(d.drawPaneShown(), "Draw pane did not open");
            d.tapUse();
            assertTrue(d.messageShown(ESignCreateSignatureDialog.WARN_DRAW, Duration.ofSeconds(4)),
                    "No \"" + ESignCreateSignatureDialog.WARN_DRAW + "\" toast");
            assertTrue(d.drawPaneShown(), "The creator closed on an empty pad");
        } finally {
            d.close();
            closeAnyDialog();
        }
    }

    @Test
    @Order(240)
    @DisplayName("ES-24 A typed signature is saved: counter +1 and a new tile")
    void typedSignatureSaved() {
        assumeTrue(baseSigs == 0, "Skipped to leave the user's own saved signatures alone (found "
                + baseSigs + "). Saved signatures carry no name, so ours could not be told apart.");
        assertTrue(saveTypedSignature("ZL_AUTO " + RUN), "Saving a typed signature failed");
        ESignaturePage page = new ESignaturePage(driver);
        try {
            assertTrue(page.signaturesSheetShown(Duration.ofSeconds(10)), "Sheet did not reopen after saving");
            assertEquals(baseSigs + 1, ESignaturePage.countOf(page.signatureCountLabel()),
                    "Signature counter after saving (" + page.signatureCountLabel() + ")");
            assertEquals(baseSigs + 1, page.savedTileLabels(false).size(), "Signature tiles after saving");
        } finally {
            page.tapSheetDone();
        }
    }

    @Test
    @Order(250)
    @DisplayName("ES-25 A drawn initial is saved under YOUR INITIALS")
    void drawnInitialSaved() {
        assumeTrue(baseInitials == 0, "Skipped to leave the user's own saved initials alone (found "
                + baseInitials + ")");
        ESignCreateSignatureDialog d = openCreator(true);
        assertEquals("How would you like to make your initials?", d.stepTitle(), "Initials step 1 title");
        d.chooseDraw();
        assumeTrue(d.drawPaneShown(), "Draw pane did not open");
        assertTrue(d.drawOnPad(), "Could not draw on the pad");
        assertEquals("Use these initials", d.useLabel(), "Footer button for initials");
        d.tapUse();
        assertTrue(d.isGone(Duration.ofSeconds(6)), "Creator did not close after Use");
        madeInitials++;
        ESignaturePage page = new ESignaturePage(driver);
        try {
            assertTrue(page.signaturesSheetShown(Duration.ofSeconds(10)), "Sheet did not reopen after saving");
            assertEquals(baseInitials + 1, ESignaturePage.countOf(page.initialCountLabel()),
                    "Initials counter after saving (" + page.initialCountLabel() + ")");
        } finally {
            page.tapSheetDone();
        }
    }

    @Test
    @Order(260)
    @DisplayName("ES-26 [finding] The counter's limit is the limit the app enforces (shows 5, refuses the 4th)")
    void counterCapMatchesEnforcedCap() {
        assumeTrue(baseSigs == 0 && madeSigs >= 1, "Needs ES-24's signature on a user with none of their own");
        while (madeSigs < ENFORCED_SIG_CAP) {
            assumeTrue(saveTypedSignature("ZL_AUTO " + RUN + "_" + madeSigs), "Could not save signature " + (madeSigs + 1));
            new ESignaturePage(driver).tapSheetDone();
        }
        // The 4th: the VM refuses it with a toast and makes no API call.
        ESignCreateSignatureDialog d = openCreator(false);
        d.chooseType();
        d.typeName("ZL_AUTO " + RUN + "_over");
        d.tapUse();
        boolean refused = d.messageShown("Max " + ENFORCED_SIG_CAP + " saved", Duration.ofSeconds(5));
        ESignaturePage page = new ESignaturePage(driver);
        if (!page.signaturesSheetShown(Duration.ofSeconds(3))) {
            closeAnyDialog();
            page.openSignaturesSheet();
        }
        String label;
        int tiles;
        try {
            label = page.signatureCountLabel();
            tiles = page.savedTileLabels(false).size();
        } finally {
            page.tapSheetDone();
        }
        if (tiles > madeSigs) madeSigs = tiles;   // the cap did not hold: sweep what was made
        assertTrue(refused, "A 4th saved signature was not refused with \"Max " + ENFORCED_SIG_CAP + " saved…\"");
        assertEquals(ENFORCED_SIG_CAP, ESignaturePage.capOf(label),
                "The counter reads \"" + label + "\" while the app refuses a " + (ENFORCED_SIG_CAP + 1)
                        + "th signature (sheet SAVED_SIG_CAP = 5, VM maxPerType = 3)");
    }

    @Test
    @Order(270)
    @DisplayName("ES-27 Long-press → \"Delete saved signature?\": Cancel keeps it")
    void deleteSignatureCancel() {
        assumeTrue(madeSigs > 0, "No signature made by this run to long-press");
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        assertTrue(page.openSignaturesSheet(), "Sheet did not open");
        try {
            int before = page.savedTileLabels(false).size();
            assertTrue(page.longPressFirstSavedTile(false), "Could not long-press a saved tile");
            assertTrue(page.alertShown(ESignaturePage.DELETE_SIG_TITLE, Duration.ofSeconds(4)),
                    "No \"" + ESignaturePage.DELETE_SIG_TITLE + "\" confirm");
            assertTrue(page.alertMessage() != null && page.alertMessage().startsWith(
                    "This will permanently remove the saved signature"), "Confirm message: " + page.alertMessage());
            assertTrue(page.tapAlertButton("Cancel"), "No Cancel on the confirm");
            assertEquals(before, page.savedTileLabels(false).size(), "Cancel removed a tile");
        } finally {
            page.tapSheetDone();
        }
    }

    @Test
    @Order(280)
    @DisplayName("ES-28 [finding] Deleting a saved signature removes the tile and updates the counter")
    void deleteSignatureUpdatesCounter() {
        assumeTrue(madeSigs > 0, "No signature made by this run to delete");
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        assertTrue(page.openSignaturesSheet(), "Sheet did not open");
        String after;
        int tilesBefore, tilesAfter, countBefore;
        try {
            countBefore = ESignaturePage.countOf(page.signatureCountLabel());
            tilesBefore = page.savedTileLabels(false).size();
            assertTrue(page.longPressFirstSavedTile(false), "Could not long-press a saved tile");
            assertTrue(page.alertShown(ESignaturePage.DELETE_SIG_TITLE, Duration.ofSeconds(4)), "No delete confirm");
            assertTrue(page.tapAlertButton("Delete"), "No Delete on the confirm");
            madeSigs--;
            sleep(1500);
            tilesAfter = page.savedTileLabels(false).size();
            after = page.signatureCountLabel();
        } finally {
            page.tapSheetDone();
        }
        assertEquals(tilesBefore - 1, tilesAfter, "Tile not removed");
        assertEquals(countBefore - 1, ESignaturePage.countOf(after),
                "The tile went but the counter still reads \"" + after + "\" (the sheet sets the count once,"
                        + " in populateCarousel; promptDelete never updates it)");
    }

    // ── helpers ──

    /**
     * Signatures → Draw New → the creator. One retry: closing the creator without saving leaves
     * the entry fragment's {@code reopenManageSheet} flag set, so the NEXT signatures load can
     * re-show the sheet under our tap (seen once: ES-24 right after ES-23).
     */
    private ESignCreateSignatureDialog openCreator(boolean initials) {
        ESignCreateSignatureDialog d = new ESignCreateSignatureDialog(driver);
        for (int attempt = 1; attempt <= 2; attempt++) {
            openESign();
            ESignaturePage page = new ESignaturePage(driver);
            if (!page.signaturesSheetShown(Duration.ofSeconds(1))) {
                assumeTrue(page.openSignaturesSheet(), "\"My Saved Signatures\" did not open");
            }
            boolean tapped = page.tapDrawNew(initials);
            if (tapped && d.isShown(Duration.ofSeconds(6))) return d;
            System.out.println(">>> creator not open on attempt " + attempt + " (Draw New tapped=" + tapped
                    + "); on screen: " + whereAreWe());
            screenshot("ES_creator_attempt_" + attempt);
            for (int i = 0; i < 3 && closeAnyDialog(); i++) { /* drain */ }
        }
        assumeTrue(false, "The signature creator did not open (twice)");
        return d;
    }

    /** Type → name → Use. Leaves the reopened sheet on screen. */
    private boolean saveTypedSignature(String name) {
        ESignCreateSignatureDialog d = openCreator(false);
        d.chooseType();
        if (!d.typePaneShown()) return false;
        d.typeName(name);
        if (!name.equals(d.typedText())) return false;
        d.tapUse();
        if (!d.isGone(Duration.ofSeconds(6))) return false;
        madeSigs++;
        return new ESignaturePage(driver).signaturesSheetShown(Duration.ofSeconds(10));
    }

    // ═══════════════════════════ 3. compose & place · ES-3x ═══════════════════════════

    @Test
    @Order(300)
    @DisplayName("ES-30 The upload button opens \"Prepare Document\" with an empty document row")
    void composeOpens() {
        ESignaturePage page = openManage(null);
        assertTrue(page.tapUploadFab(), "Upload button could not be tapped");
        ESignComposePage c = new ESignComposePage(driver);
        assertTrue(c.isLoaded(Duration.ofSeconds(10)), "Prepare Document did not open");
        assertTrue(c.textShown("Prepare Document", Duration.ofSeconds(2)), "No \"Prepare Document\" header");
        assertEquals(ESignComposePage.UPLOAD_PDF, c.pickedFileLabel(), "Document row on a fresh envelope");
        assertTrue(c.textShown(ESignComposePage.NEXT, Duration.ofSeconds(2)), "No \"Next: Place Fields\"");
    }

    @Test
    @Order(310)
    @DisplayName("ES-31 Next with nothing filled: \"Upload a PDF to continue.\"")
    void nextNeedsPdf() {
        ESignComposePage c = composeOrSkip();
        assumeTrue(!c.hasDocument(), "A document is already attached");
        assertTrue(c.tapNext(), "Next could not be tapped");
        assertEquals(ESignComposePage.HINT_NEED_PDF, c.nextHint(), "Hint without a PDF");
        assertTrue(c.isLoaded(Duration.ofSeconds(2)), "Next left the Prepare screen without a PDF");
    }

    @Test
    @Order(320)
    @DisplayName("ES-32 Upload " + PDF + ": the row names the file and the title is pre-filled from it")
    void uploadPdf() {
        ESignComposePage c = composeOrSkip();
        boolean ok = c.uploadPdf(PDF, Duration.ofSeconds(60));
        bringAppToFront();
        assumeTrue(ok || c.hasDocument(), PDF + " could not be picked (is the fixture in /sdcard/Documents?)."
                + " On screen: " + whereAreWe());
        built = 1;
        assertEquals(PDF, c.pickedFileLabel(), "Document row after upload");
        assertEquals("zl_test_doc", c.titleText(), "Title pre-filled from the file name");
        assertTrue(c.replaceShown(), "No \"Replace\" next to the attached file");
        assertTrue(c.renameShown(), "No rename icon next to the attached file");
    }

    @Test
    @Order(330)
    @DisplayName("ES-33 Next with the title cleared: \"Add a title to continue.\"")
    void nextNeedsTitle() {
        assumeTrue(built >= 1, "No PDF attached (ES-32)");
        ESignComposePage c = composeOrSkip();
        c.setTitle("");
        c.tapNext();
        assertEquals(ESignComposePage.HINT_NEED_TITLE, c.nextHint(), "Hint without a title");
        c.setTitle(title);
        assertEquals(title, c.titleText(), "Title not taken");
    }

    @Test
    @Order(340)
    @DisplayName("ES-34 Next with no signer: \"Add at least one signer to continue.\"")
    void nextNeedsSigner() {
        assumeTrue(built >= 1, "No PDF attached (ES-32)");
        ESignComposePage c = composeOrSkip();
        assumeTrue(title.equals(c.titleText()), "Title not set (ES-33)");
        c.tapNext();
        assertEquals(ESignComposePage.HINT_NEED_SIGNER, c.nextHint(), "Hint without a signer");
        assertEquals("signers (0)", String.valueOf(c.signersHeading()).toLowerCase(), "Signers heading");
    }

    @Test
    @Order(345)
    @DisplayName("ES-47 Inside project CC opens \"Select CC\"; Cancel adds nobody")
    void ccPickerOpens() {
        assumeTrue(built >= 1, "No PDF attached (ES-32)");
        ESignComposePage c = composeOrSkip();
        String before = c.ccHeading();
        assertTrue(c.tapInsideProjectCc(), "\"Inside project CC\" could not be tapped");
        assertTrue(c.pickUsersShown(Duration.ofSeconds(10)), "The CC picker did not open");
        assertEquals("Select CC", c.pickUsersTitle(), "CC picker title");
        assertTrue(c.textShown("Pick the people who get a copy when it", Duration.ofSeconds(2)), "CC picker subtitle");
        assertTrue(c.pickUsersCancel(), "Cancel could not be tapped");
        sleep(800);
        assertEquals("cc (0)", String.valueOf(before).toLowerCase(), "CC heading before");
        assertEquals(before, c.ccHeading(), "Cancel changed the CC list");
    }

    @Test
    @Order(350)
    @DisplayName("ES-35 Inside project: \"Select Signers\" lists project members; picking one adds a signer")
    void pickInsideSigner() {
        assumeTrue(built >= 1, "No PDF attached (ES-32)");
        ESignComposePage c = composeOrSkip();
        assertTrue(c.tapInsideProjectSigners(), "\"Inside project\" could not be tapped");
        assertTrue(c.pickUsersShown(Duration.ofSeconds(10)), "The signer picker did not open");
        assertEquals(ESignComposePage.PICK_SIGNERS_TITLE, c.pickUsersTitle(), "Picker title");
        List<String> names = c.pickUserNames();
        System.out.println(">>> signer candidates: " + names);
        if (names.isEmpty()) { c.pickUsersCancel(); assumeTrue(false, "No other project member to pick"); }
        assertEquals("0 selected", c.pickedCountLabel(), "Count before picking");
        signerName = names.get(0);
        assertTrue(c.pickUser(signerName), "Could not tick " + signerName);
        assertEquals("1 selected", c.pickedCountLabel(), "Count after picking");
        assertTrue(c.pickUsersDone(), "Done could not be tapped");
        sleep(800);
        assertEquals("signers (1)", String.valueOf(c.signersHeading()).toLowerCase(), "Signers heading after picking");
        assertTrue(c.textShown(signerName, Duration.ofSeconds(3)), signerName + " not listed as a signer");
        assertTrue(c.textShown("Needs to Sign", Duration.ofSeconds(2)), "Signer row lacks \"Needs to Sign\"");
        built = 2;
    }

    @Test
    @Order(360)
    @DisplayName("ES-36 \"I need to sign this document\" adds and removes the sender as a signer")
    void selfSignerToggle() {
        assumeTrue(built >= 2, "No signer picked (ES-35)");
        ESignComposePage c = composeOrSkip();
        assumeTrue(!c.selfSignerOn(), "Self-signer already on");
        try {
            assertTrue(c.toggleSelfSigner(), "Switch could not be tapped");
            assertTrue(c.selfSignerOn(), "Switch did not turn on");
            assertEquals("signers (2)", String.valueOf(c.signersHeading()).toLowerCase(), "Signers with self");
        } finally {
            if (c.selfSignerOn()) c.toggleSelfSigner();
        }
        assertEquals("signers (1)", String.valueOf(c.signersHeading()).toLowerCase(), "Signers after switching self off");
    }

    @Test
    @Order(365)
    @DisplayName("ES-48 ADD MESSAGE takes an email subject and a message")
    void addMessage() {
        assumeTrue(built >= 1, "No PDF attached (ES-32)");
        ESignComposePage c = composeOrSkip();
        c.openAddMessage();
        assertTrue(c.addMessageOpen(), "ADD MESSAGE did not open");
        String subject = "ZL_AUTO subject " + RUN;
        c.setEmailSubject(subject);
        c.setEmailBody("ZL_AUTO message body");
        assertEquals(subject, c.emailSubject(), "Subject not kept");
        assertEquals("ZL_AUTO message body", c.emailBody(), "Message not kept");
    }

    @Test
    @Order(370)
    @DisplayName("ES-37 Next opens \"Place Fields & Send\": the page renders, 0 fields, the field toolbar")
    void placementOpens() {
        assumeTrue(built >= 2, "No signer picked (ES-35)");
        ESignComposePage c = composeOrSkip();
        assertTrue(c.tapNext(), "Next could not be tapped");
        assertTrue(c.placementShown(Duration.ofSeconds(20)), "Placement did not open. Hint: " + c.nextHint());
        built = 3;
        assertTrue(c.documentRendered(Duration.ofSeconds(30)), "The PDF page never rendered: " + c.docError());
        assertEquals("0 field(s) placed", c.fieldCountLabel(), "Field count on arrival");
        assertEquals("Page 1 of 1", c.pageLabel(), "Page label");
        List<String> tools = c.fieldToolbarLabels();
        assertTrue(tools.containsAll(List.of("Sign Here", "Initial Here")), "Field toolbar: " + tools);
        assertTrue(c.textShown(ESignComposePage.SAVE_DRAFT, Duration.ofSeconds(2)), "No \"Save as Draft\"");
        assertTrue(String.valueOf(c.activeSignerLabel()).contains(signerName),
                "Active signer \"" + c.activeSignerLabel() + "\" is not " + signerName);
    }

    @Test
    @Order(380)
    @DisplayName("ES-38 Send with no field placed is refused: \"Place at least one signature field…\"")
    void sendNeedsField() {
        assumeTrue(built == 3, "Not on placement with 0 fields (ES-37)");
        ESignComposePage c = new ESignComposePage(driver);
        assumeTrue(c.placementShown(Duration.ofSeconds(5)), "Not on placement");
        c.tapSend();
        assertTrue(c.messageShown(ESignComposePage.SEND_NO_FIELDS, Duration.ofSeconds(5)), "No refusal toast");
        assertFalse(c.sendConfirmShown(Duration.ofSeconds(1)), "Send confirm opened with no field placed");
        assertTrue(c.placementShown(Duration.ofSeconds(3)), "Left placement");
    }

    @Test
    @Order(390)
    @DisplayName("ES-39 Arm \"Sign Here\" and tap the page: one field is placed for the signer")
    void placeSignHere() {
        assumeTrue(built >= 3, "Not on placement (ES-37)");
        ESignComposePage c = new ESignComposePage(driver);
        assumeTrue(c.placementShown(Duration.ofSeconds(5)), "Not on placement");
        assertTrue(c.armField("Sign Here"), "\"Sign Here\" chip not found");
        assertTrue(c.tapDocument(0.5, 0.45), "Could not tap the page");
        if (c.fieldEditorShown(Duration.ofSeconds(2))) c.closeFieldEditor();
        assertEquals("1 field(s) placed", c.fieldCountLabel(), "Field count after placing");
        built = 4;
    }

    @Test
    @Order(400)
    @DisplayName("ES-40 Send asks first — \"Send this document?\" names document and signer; Cancel sends nothing")
    void sendConfirmCancel() {
        assumeTrue(built >= 4, "No field placed (ES-39)");
        ESignComposePage c = new ESignComposePage(driver);
        assertTrue(c.tapSend(), "Send could not be tapped");
        assertTrue(c.sendConfirmShown(Duration.ofSeconds(6)), "No send confirm");
        try {
            assertEquals(title, c.sendConfirmDoc(), "Document named in the confirm");
            assertEquals("signers (1)", String.valueOf(c.sendConfirmWho()).toLowerCase(), "Signer count in the confirm");
            assertTrue(c.textShown(signerName, Duration.ofSeconds(2)), "Signer not named in the confirm");
            assertTrue(c.textShown("1 field(s)", Duration.ofSeconds(2)), "Signer's field count not shown");
        } finally {
            assertTrue(c.cancelSendConfirm(), "Cancel could not be tapped");
        }
        assertTrue(c.placementShown(Duration.ofSeconds(5)), "Cancel left placement");
        assertFalse(c.textShown("Your envelope has been sent.", Duration.ofSeconds(2)), "The envelope was sent");
    }

    @Test
    @Order(410)
    @DisplayName("ES-41 Save as Draft returns to the list; the draft shows under Draft with status Draft")
    void saveDraft() {
        assumeTrue(built >= 4, "No field placed (ES-39)");
        ESignComposePage c = new ESignComposePage(driver);
        assumeTrue(c.placementShown(Duration.ofSeconds(5)), "Not on placement");
        assertTrue(c.tapSaveDraft(), "Save as Draft could not be tapped");
        ESignaturePage page = new ESignaturePage(driver);
        assertTrue(page.listShown(Duration.ofSeconds(30)), "Did not return to the envelope list");
        built = 5;
        page.openSubTab(ESignaturePage.SUB_DRAFT);
        assertTrue(page.rowShown(title, Duration.ofSeconds(15)), title + " not under Draft: " + page.envelopeTitles());
        assertEquals("Draft", page.statusOf(title), "Status chip");
    }

    // ═══════════════════════════ 4. the draft · ES-4x ═══════════════════════════

    @Test
    @Order(420)
    @DisplayName("ES-42 A draft's row menu offers View, Edit and Delete")
    void draftMenu() {
        ESignaturePage page = draftOrSkip();
        List<String> items = page.openRowMenu(title);
        page.closePopup();
        assertEquals(List.of("View", "Edit", "Delete"), items, "Draft row menu");
    }

    @Test
    @Order(430)
    @DisplayName("ES-43 View opens the tracker: Draft, the title, Fields (1), the signer Pending")
    void draftDetail() {
        ESignaturePage page = draftOrSkip();
        assertTrue(page.tapRow(title), "Row could not be tapped");
        try {
            assertTrue(page.textShown("Recipient Activity", Duration.ofSeconds(15)), "Detail did not open");
            assertTrue(page.textShown(title, Duration.ofSeconds(2)), "Title not on the detail");
            assertTrue(page.textShown("Fields (1)", Duration.ofSeconds(2)), "No \"Fields (1)\"");
            assertTrue(page.textShown("0/1 signed", Duration.ofSeconds(2)), "No \"0/1 signed\"");
            assertTrue(page.textShown(signerName, Duration.ofSeconds(2)), "Signer not listed");
            assertTrue(page.textShown("Pending", Duration.ofSeconds(2)), "Signer not Pending");
        } finally {
            page.tapBack();
        }
        assertTrue(page.listShown(Duration.ofSeconds(10)), "Back did not return to the list");
    }

    @Test
    @Order(440)
    @DisplayName("ES-44 [QA ESIGN-01] Editing the draft twice in one session renders the document both times")
    void reopenDraftRendersTwice() {
        for (int round = 1; round <= 2; round++) {
            ESignaturePage page = draftOrSkip();
            List<String> items = page.openRowMenu(title);
            assumeTrue(items.contains("Edit"), "No Edit on the draft: " + items);
            page.tapPopupItem("Edit");
            ESignComposePage c = new ESignComposePage(driver);
            assertTrue(c.isLoaded(Duration.ofSeconds(15)), "Round " + round + ": editor did not open");
            assertEquals(title, c.titleText(), "Round " + round + ": title not pre-filled");
            c.tapNext();
            assertTrue(c.placementShown(Duration.ofSeconds(20)), "Round " + round + ": placement did not open");
            assertTrue(c.documentRendered(Duration.ofSeconds(30)),
                    "Round " + round + ": the document never rendered (spinner / error: " + c.docError() + ")");
            assertEquals("1 field(s) placed", c.fieldCountLabel(), "Round " + round + ": saved field not restored");
            stepBackToLanding();
        }
    }

    @Test
    @Order(450)
    @DisplayName("ES-45 [finding] Editing a draft shows the document's own name, not its storage key")
    void editShowsFileName() {
        ESignaturePage page = draftOrSkip();
        List<String> items = page.openRowMenu(title);
        assumeTrue(items.contains("Edit"), "No Edit on the draft: " + items);
        page.tapPopupItem("Edit");
        ESignComposePage c = new ESignComposePage(driver);
        String shown;
        try {
            assertTrue(c.isLoaded(Duration.ofSeconds(15)), "Editor did not open");
            shown = c.pickedFileLabel();
        } finally {
            stepBackToLanding();
        }
        assertEquals(PDF, shown, "The edit screen names the PDF by its upload key");
    }

    @Test
    @Order(460)
    @DisplayName("ES-46 Delete asks \"Delete this draft?\": Cancel keeps it, Delete removes it")
    void deleteDraft() {
        ESignaturePage page = draftOrSkip();
        List<String> items = page.openRowMenu(title);
        assumeTrue(items.contains("Delete"), "No Delete on the draft: " + items);
        page.tapPopupItem("Delete");
        assertTrue(page.alertShown(ESignaturePage.DELETE_DRAFT_TITLE, Duration.ofSeconds(5)), "No delete confirm");
        assertEquals("This draft will be permanently removed.", page.alertMessage(), "Confirm message");
        assertTrue(page.tapAlertButton("Cancel"), "No Cancel");
        assertTrue(page.rowShown(title, Duration.ofSeconds(5)), "Cancel removed the draft");
        assertTrue(page.deleteDraft(title), "Delete flow failed");
        sleep(1500);
        assertFalse(page.rowShown(title, Duration.ofSeconds(3)), title + " still listed after Delete");
        built = 0;
    }

    // ═══════════════════════════ 5. templates · ES-5x ═══════════════════════════
    //
    // QA ESIGN-02 is "Use Template twice does not load the document". On Android "Use Template"
    // was removed by product decision (2026-09-04, DocuSignTemplatesFragment: a saved template
    // is View / Bulk Send / Delete only). The same pre-signed-URL fetch renders the template's
    // document in the View sheet, so ES-53 opens it twice in one session instead.

    private final String templateName = "ZL_AUTO_TPL_" + RUN;
    private boolean templateSaved;

    @Test
    @Order(500)
    @DisplayName("ES-50 + New Template opens the editor in template mode; Next needs no signer")
    void newTemplateEditor() {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        page.openSegment(ESignaturePage.SEG_TEMPLATES);
        assumeTrue(page.templatesShown(Duration.ofSeconds(15)), "Templates did not show");
        assertTrue(page.tapNewTemplate(), "+ New Template could not be tapped");
        ESignComposePage c = new ESignComposePage(driver);
        assertTrue(c.isLoaded(Duration.ofSeconds(10)), "Template editor did not open");
        boolean ok = c.uploadPdf(PDF, Duration.ofSeconds(60));
        bringAppToFront();
        assumeTrue(ok || c.hasDocument(), PDF + " could not be picked for the template");
        c.setTitle(templateName);
        assertTrue(c.tapNext(), "Next could not be tapped");
        assertTrue(c.templateModeShown(Duration.ofSeconds(20)),
                "No \"Save Template\" on placement (template mode). Hint: " + c.nextHint());
        assertFalse(c.textShown(ESignComposePage.SAVE_DRAFT, Duration.ofSeconds(1)), "Save as Draft shown in template mode");
        assertTrue(c.documentRendered(Duration.ofSeconds(30)), "Template page never rendered: " + c.docError());
    }

    @Test
    @Order(510)
    @DisplayName("ES-51 Save Template without a category is refused: \"Add a category (or type a new one)\"")
    void templateNeedsCategory() {
        ESignComposePage c = new ESignComposePage(driver);
        assumeTrue(c.templateModeShown(Duration.ofSeconds(5)), "Not on template placement (ES-50)");
        if (c.fieldCountLabel() != null && c.fieldCountLabel().startsWith("0")) {
            c.armField("Sign Here");
            c.tapDocument(0.5, 0.45);
            if (c.fieldEditorShown(Duration.ofSeconds(2))) c.closeFieldEditor();
        }
        assertTrue(c.tapSaveTemplate(), "Save Template could not be tapped");
        assertTrue(c.saveTemplateDialogShown(Duration.ofSeconds(6)), "Save-template dialog did not open");
        assertEquals(templateName, c.templateNameField(), "Name not pre-filled from the title");
        c.setTemplateCategory("");
        c.confirmSaveTemplate();
        assertTrue(c.messageShown("Add a category (or type a new one)", Duration.ofSeconds(4)), "No category refusal");
        assertTrue(c.saveTemplateDialogShown(Duration.ofSeconds(2)), "Dialog closed without a category");
    }

    @Test
    @Order(520)
    @DisplayName("ES-52 Save Template with a category: \"Template saved\", listed under Templates")
    void saveTemplate() {
        ESignComposePage c = new ESignComposePage(driver);
        assumeTrue(c.saveTemplateDialogShown(Duration.ofSeconds(3)), "Save-template dialog not open (ES-51)");
        c.setTemplateCategory("zl auto");
        assertTrue(c.confirmSaveTemplate(), "Save Template could not be tapped");
        boolean toast = c.messageShown("Template saved", Duration.ofSeconds(20));
        ESignaturePage page = new ESignaturePage(driver);
        assertTrue(page.isLoaded(Duration.ofSeconds(30)) || page.listShown(Duration.ofSeconds(5)),
                "Did not return to the module after saving. On screen: " + whereAreWe());
        page.openSegment(ESignaturePage.SEG_TEMPLATES);
        assertTrue(page.templatesShown(Duration.ofSeconds(15)), "Templates did not show");
        page.search(templateName);
        templateSaved = page.templateShown(templateName, Duration.ofSeconds(15));
        assertTrue(templateSaved, templateName + " not listed: " + page.templateNames());
        assertTrue(toast, "No \"Template saved\" toast (the template was saved)");
    }

    @Test
    @Order(530)
    @DisplayName("ES-53 [QA ESIGN-02] Viewing the template twice in one session renders its document both times")
    void viewTemplateTwice() {
        assumeTrue(templateSaved, "No ZL_AUTO template saved (ES-52)");
        for (int round = 1; round <= 2; round++) {
            ESignaturePage page = templatesFiltered();
            assertTrue(page.viewTemplate(templateName), "Round " + round + ": View could not be tapped");
            try {
                assertTrue(page.templateDetailShown(Duration.ofSeconds(10)), "Round " + round + ": detail did not open");
                assertEquals(templateName, page.templateDetailName(), "Round " + round + ": detail name");
                assertTrue(page.templateDocumentRendered(Duration.ofSeconds(30)),
                        "Round " + round + ": the template's document never rendered (" + page.templateDocError() + ")");
                assertEquals("fields (1)", String.valueOf(page.templateDetailFieldsLabel()).toLowerCase(),
                        "Round " + round + ": field count");
            } finally {
                page.closeTemplateDetail();
                sleep(800);
            }
        }
    }

    @Test
    @Order(540)
    @DisplayName("ES-54 Template ⋮ offers Bulk Send and Delete; Delete asks \"Delete template?\" and removes it")
    void deleteTemplate() {
        assumeTrue(templateSaved, "No ZL_AUTO template saved (ES-52)");
        ESignaturePage page = templatesFiltered();
        List<String> items = page.openTemplateMenu(templateName);
        page.closePopup();
        assertEquals(List.of("Bulk Send", "Delete"), items, "Template menu");
        assertTrue(page.deleteTemplate(templateName), "Delete flow failed");
        assertFalse(page.templateShown(templateName, Duration.ofSeconds(4)), templateName + " still listed");
        templateSaved = false;
        page.clearSearch();
    }

    private ESignaturePage templatesFiltered() {
        stepBackToLanding();
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        page.openSegment(ESignaturePage.SEG_TEMPLATES);
        assumeTrue(page.templatesShown(Duration.ofSeconds(15)), "Templates did not show");
        page.search(templateName);
        assumeTrue(page.templateShown(templateName, Duration.ofSeconds(10)), templateName + " not listed");
        return page;
    }

    // ═══════════════════════════ 9. cleanup · ES-99 ═══════════════════════════

    @Test
    @Order(990)
    @DisplayName("ES-99 No ZL_AUTO draft and no saved signature made by this run is left")
    void cleanup() {
        stepBackToLanding();
        ESignaturePage page = openManage(ESignaturePage.SUB_DRAFT);
        List<String> left = new ArrayList<>();
        page.search("ZL_AUTO");
        for (int i = 0; i < 15; i++) {
            String next = page.envelopeTitles().stream().filter(t -> t.startsWith("ZL_AUTO"))
                    .filter(t -> !left.contains(t)).findFirst().orElse(null);
            if (next == null) break;
            if (!page.deleteDraft(next)) left.add(next);
        }
        page.clearSearch();
        // ZL_AUTO templates (any run's).
        page.openSegment(ESignaturePage.SEG_TEMPLATES);
        if (page.templatesShown(Duration.ofSeconds(10))) {
            page.search("ZL_AUTO");
            sleep(1500);
            for (int i = 0; i < 10; i++) {
                String next = page.templateNames().stream().filter(t -> t.startsWith("ZL_AUTO"))
                        .filter(t -> !left.contains(t)).findFirst().orElse(null);
                if (next == null) break;
                if (!page.deleteTemplate(next)) left.add(next);
            }
            page.clearSearch();
        }
        page.openSegment(ESignaturePage.SEG_MANAGE);
        // Saved signatures / initials this run made (tiles have no name: delete back to the start count).
        if (madeSigs > 0 || madeInitials > 0) {
            openESign();
            if (page.openSignaturesSheet()) {
                try {
                    for (boolean initials : new boolean[]{false, true}) {
                        int n = initials ? madeInitials : madeSigs;
                        for (int k = 0; k < n; k++) {
                            if (!page.longPressFirstSavedTile(initials)) break;
                            if (!page.alertShown(ESignaturePage.DELETE_SIG_TITLE, Duration.ofSeconds(4))) break;
                            page.tapAlertButton("Delete");
                            sleep(1200);
                            if (initials) madeInitials--; else madeSigs--;
                        }
                    }
                } finally {
                    page.tapSheetDone();
                }
            }
        }
        assertTrue(left.isEmpty(), "ZL_AUTO drafts left: " + left);
        assertEquals(0, madeSigs + madeInitials, "Saved signatures/initials made by this run still there");
    }

    // ── helpers ──

    private ESignComposePage composeOrSkip() {
        ESignComposePage c = new ESignComposePage(driver);
        assumeTrue(c.isLoaded(Duration.ofSeconds(5)), "Not on Prepare Document (ES-30). On screen: " + whereAreWe());
        return c;
    }

    private ESignaturePage draftOrSkip() {
        assumeTrue(built >= 5, "No ZL_AUTO draft saved (ES-41)");
        stepBackToLanding();
        ESignaturePage page = openManage(ESignaturePage.SUB_DRAFT);
        assumeTrue(page.rowShown(title, Duration.ofSeconds(10)), title + " not under Draft");
        return page;
    }
}
