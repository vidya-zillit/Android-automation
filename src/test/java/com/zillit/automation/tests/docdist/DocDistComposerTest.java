package com.zillit.automation.tests.docdist;

import com.zillit.automation.core.TestEmails;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.ConfirmPopupPage;
import com.zillit.automation.pages.docdist.DocDistComposerPage;
import com.zillit.automation.pages.docdist.DocDistLibraryPage;
import com.zillit.automation.pages.docdist.DocDistPickerPage;
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
 * DOCUMENT DISTRIBUTION — area F (composer) core plus area E (document picker) of
 * DOC_DISTRIBUTION_TEST_CASES.md.
 *
 * <p><b>No email is ever SENT here.</b> Everything up to and including the preview stage
 * is covered; the actual send (F-25, with its History verification — trap 3.5) is left
 * to a dedicated suite so a full run does not spam real recipients every time.
 *
 * <p>Seeds its own world (folder + one uploaded PDF) so the attach-from-library picker
 * has something to list, and deletes the folder at the end.
 *
 * <p>Behaviour notes baked in from EmailComposerFragment (not guesses):
 * <ul>
 *   <li>Chips commit on comma / IME-done / focus loss — the page types a trailing comma.</li>
 *   <li>An INVALID address still becomes a chip (styled red); rejection is at send time.
 *       F-3 therefore asserts the chip exists AND that send stays gated on valid input
 *       elsewhere — not "no chip".</li>
 *   <li>Send and Preview are disabled until a recipient exists (F-24 is a button-state
 *       assertion).</li>
 *   <li>Cancel is {@code goBack()} → popBackStack — the current build shows NO discard
 *       confirmation (F-26's "(confirm)" from the layout map was wrong).</li>
 * </ul>
 */
@Order(TestOrder.DOC_DISTRIBUTION_COMPOSER)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistComposerTest extends DocDistBaseTest {

    private static final String DOC = "zl_dd_doc.pdf";
    private static final String FOLDER = "QA_DDC_" + (System.currentTimeMillis() % 100000);
    private static final String TO_1 = TestEmails.recipient();
    private static final String TO_2 = TestEmails.self();
    private static final String BAD = "not-an-email";
    private static final String SUBJECT = "QA DD composer " + (System.currentTimeMillis() % 100000);

    private static boolean seeded;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    /**
     * Opens a FRESH composer.
     *
     * <p>Two entry points, because the Compose CHIP does not exist on every build (the
     * emulator's dev APK has no {@code dd_btn_compose} at all — verified in a live dump,
     * trap 3.17) while the composer FRAGMENT is perfectly present and reachable from a
     * folder's <b>Distribute</b> button. Falling back to that keeps this whole area
     * testable on such a build instead of skipping 20-odd cases over a missing chip.
     *
     * <p>The two entries differ in one way worth knowing: Compose opens EMPTY, Distribute
     * opens pre-loaded with the folder's documents. Cases that care about the attachment
     * list account for that rather than assuming a clean composer.
     */
    private DocDistComposerPage openComposer() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistComposerPage composer = new DocDistComposerPage(driver);

        if (lib.openCompose() && composer.isOpen(Duration.ofSeconds(12))) {
            return composer;
        }
        // No Compose chip on this build — go in through the seeded folder's Distribute.
        System.out.println(">>> no Compose chip; entering the composer via folder Distribute");
        assumeTrue(seeded, "The seed case must have created a folder with a document");
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(20)), "Seed folder missing");
        assumeTrue(lib.openItem(FOLDER), "Could not open the seed folder");
        sleep(2000);
        assumeTrue(lib.tapFolderDistribute(),
                "Neither the Compose chip nor the folder's Distribute button is available");
        assumeTrue(composer.isOpen(Duration.ofSeconds(15)), "The composer did not open");
        return composer;
    }

    /** Back to the library root from wherever a composer case finished. */
    private void leaveComposer(DocDistComposerPage composer) {
        try { composer.tapCancel(); } catch (Exception ignored) { }
        sleep(1200);
        // A dirty composer now asks "Keep this email as a draft?" — answer Discard so no
        // QA draft is left in Drafts (Keep draft would save one).
        composer.discardDraftIfAsked(Duration.ofSeconds(2));
        new DocDistLibraryPage(driver).returnToRoot();
    }

    // ==================== seed ====================

    @Test
    @Order(1)
    @DisplayName("SEED: a folder with one uploaded PDF, so the picker has content")
    void seedLibrary() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.waitForItem(FOLDER, Duration.ofSeconds(8))) {
            assumeTrue(createFolder(FOLDER), "Could not create the seed folder");
            assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(45)), "Seed folder not listed");
        }
        assumeTrue(lib.openItem(FOLDER), "Could not open the seed folder");
        sleep(2000);
        assumeTrue(lib.hasUpload(),
                "Upload is not offered inside the folder on this build (3.6/3.11) — "
                        + "the picker cases need a seeded file, skipping");
        assumeTrue(uploadFixtureIntoOpenFolder(DOC), "Upload pipeline did not complete");
        boolean listed = lib.waitForItem(DOC, Duration.ofSeconds(60));
        System.out.println(">>> seeded: " + lib.itemNames());
        assumeTrue(lib.returnToRoot(), "Could not return to root after seeding");
        assertTrue(listed, "The seed document should be listed");
        seeded = true;
    }

    // ==================== F-1 ====================

    @Test
    @Order(2)
    @DisplayName("F-1: Compose opens the composer on its compose stage")
    void composerOpens() {
        DocDistComposerPage composer = openComposer();
        assertTrue(composer.isComposeStage(Duration.ofSeconds(5)),
                "dd_compose_stage should be showing");
        screenshot("ddc-composer-open");
        composer.tapCancel();
    }

    // ==================== F-2 / F-3 / F-6 ====================

    @Test
    @Order(3)
    @DisplayName("F-2/F-3/F-6: valid AND invalid addresses chip (invalid styled, not dropped); chips remove")
    void toChips() {
        DocDistComposerPage composer = openComposer();
        int before = composer.chipCount("dd_to_chips");

        assertTrue(composer.addTo(TO_1), "Could not type into the To field");
        int afterValid = composer.chipCount("dd_to_chips");
        assertTrue(afterValid > before,
                "A valid address + comma should become a chip (" + before + " → " + afterValid + ")");

        // Source truth: renderChips colours an invalid address red but STILL adds it —
        // rejection happens at send. So the assertion is "chip added", and the map's
        // original "no chip / error" expectation is recorded as wrong.
        assertTrue(composer.addTo(BAD), "Could not type the invalid address");
        int afterBad = composer.chipCount("dd_to_chips");
        System.out.println(">>> chips valid=" + afterValid + " afterInvalid=" + afterBad
                + " texts=" + composer.chipTexts("dd_to_chips"));
        screenshot("ddc-chips");
        assertTrue(afterBad > afterValid,
                "An invalid address still chips (styled as invalid) — it is not silently dropped");

        // F-6: remove them both via the close icon.
        assertTrue(composer.removeChip("dd_to_chips", BAD), "Could not remove the invalid chip");
        assertTrue(composer.removeChip("dd_to_chips", TO_1), "Could not remove the valid chip");
        sleep(800);
        assertEquals(before, composer.chipCount("dd_to_chips"),
                "Removing both chips should restore the starting count");
        composer.tapCancel();
    }

    // ==================== F-4 / F-5 ====================

    @Test
    @Order(4)
    @DisplayName("F-4/F-5: the Cc/Bcc toggle reveals the block, and both fields accept chips")
    void ccBcc() {
        DocDistComposerPage composer = openComposer();
        if (!composer.ccBlockShown()) {
            assumeTrue(composer.toggleCcBcc(), "Could not tap the Cc/Bcc toggle");
            sleep(800);
        }
        assertTrue(composer.ccBlockShown(), "dd_cc_block should be revealed");

        assertTrue(composer.addCc(TO_1), "Cc should accept input");
        assertTrue(composer.addBcc(TO_2), "Bcc should accept input");
        int cc = composer.chipCount("dd_cc_chips");
        int bcc = composer.chipCount("dd_bcc_chips");
        System.out.println(">>> cc=" + cc + " bcc=" + bcc);
        screenshot("ddc-ccbcc");
        assertTrue(cc >= 1, "Cc should hold a chip");
        assertTrue(bcc >= 1, "Bcc should hold a chip");
        composer.tapCancel();
    }

    // ==================== F-10 / F-24 ====================

    /**
     * F-24 as the app actually builds it.
     *
     * <p>The map says "Send with no recipient is rejected", but the compose stage has NO
     * Send button: {@code ddSend.visibility = if (preview) VISIBLE else GONE}, with
     * Cancel/Preview the mirror image. Sending is compose → Preview → Send, so the gate a
     * missing recipient hits is <b>Preview</b>, and that is what this asserts. (Asserting
     * Send here previously produced a false failure: the button was absent, not disabled.)
     *
     * <p>The gate itself is stricter than "some text in To":
     * {@code recipientCount = to.count { isValid }}, so an invalid address chips but does
     * not unlock Preview — checked here directly.
     */
    @Test
    @Order(5)
    @DisplayName("F-24/F-10: PREVIEW is the recipient gate on the compose stage (Send lives on preview)")
    void previewGatedOnValidRecipient() {
        DocDistComposerPage composer = openComposer();

        assertFalse(composer.hasSendButton(),
                "The compose stage must not offer Send — it belongs to the preview stage");
        assertFalse(composer.isPreviewEnabled(),
                "With no recipient, Preview must be disabled (recipientCount == 0)");

        assertTrue(composer.setSubject(SUBJECT), "Subject should accept text");
        assertTrue(composer.subject().contains(SUBJECT.substring(0, 10)),
                "The subject text should be present");
        assertFalse(composer.isPreviewEnabled(),
                "A subject alone must not unlock Preview — a valid recipient is the gate");

        // An INVALID address chips (F-3) but must not count: recipientCount only counts
        // addresses EmailValidator accepts.
        assumeTrue(composer.addTo(BAD), "Could not type the invalid address");
        sleep(800);
        boolean afterInvalid = composer.isPreviewEnabled();
        System.out.println(">>> preview enabled after an INVALID recipient = " + afterInvalid);
        assertFalse(afterInvalid,
                "An invalid address chips but must not satisfy the gate "
                        + "(recipientCount counts only valid addresses)");

        assertTrue(composer.addTo(TO_1), "Could not add a valid recipient");
        sleep(1000);
        boolean enabled = composer.isPreviewEnabled();
        System.out.println(">>> preview enabled after a VALID recipient = " + enabled);
        screenshot("ddc-send-gate");
        assertTrue(enabled, "With a valid recipient, Preview should enable");
        composer.tapCancel();
    }

    // ==================== E-1 / E-4..E-7 · picker ====================

    @Test
    @Order(6)
    @DisplayName("E-1/E-4/E-5/E-6: attach-from-library opens the picker; select-all / count / clear work")
    void pickerSelection() {
        assumeTrue(seeded, "Needs the seeded library");
        DocDistComposerPage composer = openComposer();
        assumeTrue(composer.openAttachFromLibrary(), "Could not tap attach-from-library");

        DocDistPickerPage picker = new DocDistPickerPage(driver);
        assertTrue(picker.isOpen(Duration.ofSeconds(15)),
                "The document picker should open (dd_footer_count is its unique id)");
        System.out.println(">>> picker files=" + picker.fileNames()
                + " scopeChips=" + picker.hasScopeChips());
        assumeTrue(!picker.fileNames().isEmpty() || picker.search(DOC),
                "Nothing listed in the picker");

        assertTrue(picker.tapSelectAll(), "Could not tap Select all");
        sleep(800);
        int all = picker.selectedCount();
        assertTrue(all >= 1, "Select all should select at least the seeded file, got " + all);

        assertTrue(picker.tapClear(), "Could not tap Clear");
        sleep(800);
        assertEquals(0, picker.selectedCount(), "Clear should reset the count to 0");
        assertFalse(picker.isAttachEnabled(),
                "With nothing selected, Attach must be disabled (E-8's no-op, by construction)");
        screenshot("ddc-picker");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1000);
        DocDistComposerPage back = new DocDistComposerPage(driver);
        if (back.isOpen(Duration.ofSeconds(5))) back.tapCancel();
    }

    @Test
    @Order(7)
    @DisplayName("E-7/F-16/F-19: attaching the seeded file lists it in the composer; remove deletes the row")
    void attachAndRemove() {
        assumeTrue(seeded, "Needs the seeded library");
        DocDistComposerPage composer = openComposer();
        assumeTrue(composer.openAttachFromLibrary(), "Could not tap attach-from-library");

        DocDistPickerPage picker = new DocDistPickerPage(driver);
        assumeTrue(picker.isOpen(Duration.ofSeconds(15)), "Picker did not open");
        if (!picker.selectFile(DOC)) {
            assumeTrue(picker.search(DOC), "Could not search the picker");
            assumeTrue(picker.selectFile(DOC), "Could not select the seeded file");
        }
        int n = picker.selectedCount();
        assertTrue(n >= 1, "Selecting the file should count it, got " + n);
        assertTrue(picker.isAttachEnabled(), "Attach should enable with a selection");
        assertTrue(picker.tapAttach(), "Could not tap Attach");
        sleep(1500);

        assertTrue(composer.isOpen(Duration.ofSeconds(10)), "Attach should return to the composer");
        var names = composer.attachmentNames();
        System.out.println(">>> attachments after attach: " + names);
        screenshot("ddc-attached");
        assertTrue(names.stream().anyMatch(t -> t.contains("zl_dd_doc")),
                "The attached file should be listed, got " + names);

        // F-19: remove it again.
        assertTrue(composer.removeAttachment("zl_dd_doc"), "Could not tap the remove button");
        sleep(1000);
        assertTrue(composer.attachmentNames().stream().noneMatch(t -> t.contains("zl_dd_doc")),
                "The removed attachment should be gone");
        composer.tapCancel();
    }

    // ==================== F-22 / F-23 · preview ====================

    @Test
    @Order(8)
    @DisplayName("F-22/F-23: Preview shows the recipient and subject; Edit returns to compose")
    void previewRoundTrip() {
        DocDistComposerPage composer = openComposer();
        assumeTrue(composer.addTo(TO_1), "Could not add a recipient");
        assumeTrue(composer.setSubject(SUBJECT), "Could not set the subject");
        assumeTrue(composer.isPreviewEnabled(), "Preview should be enabled now");
        assumeTrue(composer.tapPreview(), "Could not tap Preview");

        assertTrue(composer.isPreviewStage(Duration.ofSeconds(10)),
                "dd_preview_stage should be showing");
        String to = composer.previewTo();
        String subject = composer.previewSubject();
        System.out.println(">>> preview to='" + to + "' subject='" + subject + "'");
        screenshot("ddc-preview");
        assertTrue(to != null && to.contains(TO_1), "Preview To should show the recipient");
        assertTrue(subject != null && subject.contains(SUBJECT.substring(0, 10)),
                "Preview subject should match what was typed");

        assertTrue(composer.tapEdit(), "Could not tap Edit");
        assertTrue(composer.isComposeStage(Duration.ofSeconds(10)),
                "Edit should return to the compose stage");
        composer.tapCancel();
    }

    // ==================== F-26 · cancel ====================

    /**
     * Taps Cancel and, if the tap left the composer standing with no prompt, falls back to
     * system Back (same goBack() path). Returns whether a confirmation popup appeared.
     */
    private boolean cancelAndDetectPrompt(DocDistComposerPage composer) {
        boolean tapped = composer.tapCancel();
        System.out.println(">>> cancel tap reported " + tapped);
        sleep(1500);
        ConfirmPopupPage confirm = new DocDistLibraryPage(driver).confirmPopup();
        if (confirm.isShown(Duration.ofSeconds(2))) return true;
        if (composer.isOpen(Duration.ofSeconds(2))) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1500);
        }
        return confirm.isShown(Duration.ofSeconds(2));
    }

    /** Leaves a composer for good, confirming a discard prompt if the build shows one. */
    private void leaveComposerDiscarding(DocDistComposerPage composer) {
        try { composer.tapCancel(); } catch (Exception ignored) { }
        sleep(1200);
        // Discard (neutral button) when the build asks "Keep this email as a draft?";
        // accepting would KEEP a QA draft. Older builds with a two-button confirm fall back.
        if (!composer.discardDraftIfAsked(Duration.ofSeconds(2))) {
            new DocDistLibraryPage(driver).confirmPopup().acceptIfShown(Duration.ofSeconds(2));
        }
        new DocDistLibraryPage(driver).returnToRoot();
    }

    @Test
    @Order(9)
    @DisplayName("F-26a: Cancel on an untouched composer leaves at once")
    void cancelEmptyCloses() {
        DocDistComposerPage composer = openComposer();

        // Assert the OUTCOME (we left the composer), not the tap's return value: the click
        // helper has reported false while the screen dump showed the library.
        boolean prompt = cancelAndDetectPrompt(composer);
        System.out.println(">>> confirm shown on an untouched composer = " + prompt);
        if (prompt) {
            // Nothing was typed; a build that still asks is over-cautious, not wrong.
            new DocDistLibraryPage(driver).confirmPopup().accept(Duration.ofSeconds(2));
        }
        DocDistComposerPage after = new DocDistComposerPage(driver);
        assertFalse(after.isOpen(Duration.ofSeconds(3)),
                "Cancel must leave the composer — neither compose nor preview stage should remain");
        assertTrue(new DocDistLibraryPage(driver).isLibrary(Duration.ofSeconds(10)),
                "Cancel should land back on the library");
        screenshot("ddc-cancelled");
    }

    /**
     * Known bug DD-24 (sheet: Android "In progress"): goBack() → leave() →
     * popBackStack() with no dirty check (Zillit-Android
     * {@code docdistribution/ui/composer/EmailComposerFragment.kt:1140-1152}; Cancel at
     * :605, system Back at :167), so typed content is lost with no prompt and no draft.
     * Correct behaviour: either a discard prompt, or Compose reopens with the draft.
     * This case FAILS until DD-24 is fixed.
     */
    @Test
    @Order(10)
    @DisplayName("F-26 [QA DD-24]: Cancel with typed content asks first or keeps the draft")
    void cancelWithContentPromptsOrKeepsDraft() {
        String draftSubject = "QA DD-24 draft " + (System.currentTimeMillis() % 100000);
        DocDistComposerPage composer = openComposer();
        assumeTrue(composer.setSubject(draftSubject), "Could not type a subject");
        assumeTrue(composer.addTo(TO_1), "Could not add a To recipient");
        assumeTrue(composer.subject().contains(draftSubject), "The subject did not take the typed text");

        boolean prompt = cancelAndDetectPrompt(composer);
        System.out.println(">>> discard prompt shown = " + prompt);
        screenshot("ddc-dd24-after-cancel");

        if (prompt) {
            // Fixed build, prompt flavour: declining must keep the user in the composer
            // with what they typed.
            ConfirmPopupPage confirm = new DocDistLibraryPage(driver).confirmPopup();
            System.out.println(">>> prompt text='" + confirm.text() + "'");
            confirm.cancel(Duration.ofSeconds(3));
            DocDistComposerPage still = new DocDistComposerPage(driver);
            boolean open = still.isOpen(Duration.ofSeconds(5));
            String kept = open ? still.subject() : "";
            if (open) leaveComposerDiscarding(still);
            assertTrue(open && kept != null && kept.contains(draftSubject),
                    "[QA DD-24] Declining the discard prompt should keep the composer open with the typed "
                            + "subject. open=" + open + " subject='" + kept + "'");
            return;
        }

        // No prompt: the composer must have closed AND kept the draft for next time.
        assumeTrue(!new DocDistComposerPage(driver).isOpen(Duration.ofSeconds(3)),
                "Neither Cancel nor Back closed the composer — cannot judge the draft");
        assumeTrue(new DocDistLibraryPage(driver).returnToRoot(), "Could not return to the library root");
        DocDistComposerPage reopened = openComposer();
        String subjectAfter = reopened.subject();
        List<String> toAfter = reopened.chipTexts("dd_to_chips");
        System.out.println(">>> reopened subject='" + subjectAfter + "' to=" + toAfter);
        screenshot("ddc-dd24-reopened");
        leaveComposerDiscarding(reopened);

        assertTrue(subjectAfter != null && subjectAfter.contains(draftSubject),
                "[QA DD-24] Cancel with a typed subject closed the composer with no prompt, and "
                        + "Compose reopened without the draft (subject='" + subjectAfter + "', to="
                        + toAfter + "). EmailComposerFragment.kt:1140-1152 goBack() -> leave() -> "
                        + "popBackStack() has no dirty check.");
    }

    // ==================== cleanup ====================

    // ==================== WEB gap cases (2026-11-23) ====================

    /**
     * [WEB L173] Preview's To line lists an invalid address. Sheet DD-26: Preview joined
     * every chip, valid or not, while send() emails only the valid ones.
     */
    @Test
    @Order(11)
    @DisplayName("F-27 [WEB DD-26]: Preview's To lists only the valid addresses")
    void previewToExcludesInvalid() {
        DocDistComposerPage composer = openComposer();
        try {
            assumeTrue(composer.addTo(TO_1), "Could not add the valid recipient");
            assumeTrue(composer.addTo(BAD), "Could not add the invalid address");
            assumeTrue(composer.chipCount("dd_to_chips") >= 2, "Both chips should be in To");
            assumeTrue(composer.setSubject(SUBJECT), "Could not set the subject");
            assumeTrue(composer.isPreviewEnabled(), "Preview should be enabled with one valid recipient");
            assumeTrue(composer.tapPreview(), "Could not tap Preview");
            assumeTrue(composer.isPreviewStage(Duration.ofSeconds(10)), "Preview stage did not show");
            String to = composer.previewTo();
            System.out.println(">>> F-27 preview To='" + to + "'");
            screenshot("ddc-f27-preview");
            assertTrue(to != null && to.contains(TO_1), "Preview To should list the valid recipient, got '" + to + "'");
            assertFalse(to.contains(BAD), "[DD-26] Preview To must not list the invalid address '" + BAD
                    + "' (it is never emailed), got '" + to + "'");
        } finally {
            composer.leaveDiscarding();
            new DocDistLibraryPage(driver).returnToRoot();
        }
    }

    /**
     * [WEB L178, F10 L270] Sheet DD-28: "Distribute folder" is the heading for a whole
     * folder (the folder row's Send), and only for that.
     */
    @Test
    @Order(12)
    @DisplayName("F-28 [WEB DD-28]: a folder row's Send opens 'Distribute folder' with its files attached")
    void folderRowSendIsDistributeFolder() {
        assumeTrue(seeded, "The seed case must have created the folder with a document");
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        try {
            assumeTrue(lib.search(FOLDER), "Could not search for the seed folder");
            sleep(1500);
            assumeTrue(lib.tapRowSend(FOLDER), "Could not tap the folder row's Send");
            assumeTrue(composer.isOpen(Duration.ofSeconds(20)), "The composer did not open");
            sleep(1500);
            String title = composer.headerTitle();
            int attachments = composer.attachmentCount();
            System.out.println(">>> F-28 title='" + title + "' attachments=" + composer.attachmentNames());
            screenshot("ddc-f28");
            assertTrue("Distribute folder".equalsIgnoreCase(title.trim()),
                    "A folder's Send should open 'Distribute folder', got '" + title + "'");
            assertTrue(attachments >= 1, "The folder's files should be attached, got " + composer.attachmentNames());
        } finally {
            if (composer.isOpen(Duration.ofSeconds(1))) composer.leaveDiscarding();
            lib.returnToRoot();
            lib.clearSearch();
        }
    }

    /**
     * Sheet DD-28, the other half: Compose and Share documents INSIDE a folder used to
     * inherit "Distribute folder" because the open folder's name was always passed on.
     */
    @Test
    @Order(13)
    @DisplayName("F-29 [WEB DD-28]: Compose and Share documents inside a folder are titled 'Compose email'")
    void composeInsideFolderIsComposeEmail() {
        assumeTrue(seeded, "The seed case must have created the folder with a document");
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        String composeTitle = "";
        String shareTitle = "";
        List<String> shareAttachments = List.of();
        try {
            assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(20)), "Seed folder missing");
            assumeTrue(lib.openItem(FOLDER), "Could not open the seed folder");
            sleep(2000);
            assumeTrue(lib.isInFolder(), "Should be inside the seed folder");

            boolean opened = lib.openCompose() && composer.isOpen(Duration.ofSeconds(15));
            assumeTrue(opened, "Compose is not reachable inside the folder on this build");
            composeTitle = composer.headerTitle();
            composer.leaveDiscarding();
            sleep(1200);
            assumeTrue(lib.isInFolder(), "Should be back in the seed folder after Compose");
            assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Seed document missing");
            assumeTrue(lib.selectRow(DOC), "Could not tick the document");
            assumeTrue(lib.openSelectionActions(), "Could not open the selection menu");
            sleep(1000);
            assumeTrue(lib.tapMenuItem("Share documents"), "No 'Share documents' in the selection menu");
            assumeTrue(composer.isOpen(Duration.ofSeconds(15)), "Share documents did not open the composer");
            sleep(1000);
            shareTitle = composer.headerTitle();
            shareAttachments = composer.attachmentNames();
            System.out.println(">>> F-29 compose title='" + composeTitle + "' share title='" + shareTitle
                    + "' attachments=" + shareAttachments);
            screenshot("ddc-f29");
            // The toolbar renders its heading in capitals ("COMPOSE EMAIL"), so compare
            // case-insensitively; the bug was the WORDS ("Distribute folder").
            assertTrue("Compose email".equalsIgnoreCase(composeTitle.trim()),
                    "[DD-28] Compose inside a folder should be titled 'Compose email', got '" + composeTitle + "'");
            assertTrue("Compose email".equalsIgnoreCase(shareTitle.trim()),
                    "[DD-28] Share documents inside a folder should be titled 'Compose email', got '" + shareTitle + "'");
            assertTrue(shareAttachments.stream().anyMatch(n -> n.contains("zl_dd_doc")),
                    "Share documents should attach the ticked file, got " + shareAttachments);
        } finally {
            if (composer.isOpen(Duration.ofSeconds(1))) composer.leaveDiscarding();
            if (lib.isSelectionMode()) lib.clearSelection();
            lib.returnToRoot();
        }
    }

    /**
     * [WEB L125] Sheet DD-30, the Attach half: picker rows showed name, size and date with
     * no folder, so same-named files from different folders could not be told apart.
     */
    @Test
    @Order(14)
    @DisplayName("F-30 [WEB DD-30]: Attach from library names each file's folder")
    void pickerRowsNameTheirFolder() {
        assumeTrue(seeded, "Needs the seeded library");
        DocDistComposerPage composer = openComposer();
        DocDistPickerPage picker = new DocDistPickerPage(driver);
        try {
            assumeTrue(composer.openAttachFromLibrary(), "Could not tap attach-from-library");
            assumeTrue(picker.isOpen(Duration.ofSeconds(15)), "The document picker did not open");
            picker.search(DOC);
            sleep(1500);
            List<String> metas = picker.metasOf(DOC);
            System.out.println(">>> F-30 metas of " + DOC + " = " + metas);
            screenshot("ddc-f30");
            assumeTrue(!metas.isEmpty(), "The seeded document is not listed in the picker");
            assertTrue(metas.stream().anyMatch(m -> m.startsWith(FOLDER)),
                    "[DD-30] The picker row should lead with its folder '" + FOLDER + "', got " + metas);
        } finally {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            DocDistComposerPage back = new DocDistComposerPage(driver);
            if (back.isOpen(Duration.ofSeconds(3))) back.leaveDiscarding();
            new DocDistLibraryPage(driver).returnToRoot();
        }
    }

    @Test
    @Order(20)
    @DisplayName("CLEANUP: the seeded folder (and its file) delete")
    void cleanup() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Not at root");
        assertTrue(deleteItem(FOLDER), "The seeded folder should delete");
        screenshot("ddc-cleanup");
    }
}
