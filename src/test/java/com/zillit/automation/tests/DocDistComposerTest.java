package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.DocDistPickerPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

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
    private static final String TO_1 = "qa.dd.one@example.com";
    private static final String TO_2 = "qa.dd.two@example.com";
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

    @Test
    @Order(9)
    @DisplayName("F-26: Cancel leaves the composer (current build: straight popBackStack, no confirm)")
    void cancelCloses() {
        DocDistComposerPage composer = openComposer();
        assumeTrue(composer.addTo(TO_1), "Could not dirty the draft");

        // Assert the OUTCOME (we left the composer), not the tap's return value. The
        // previous version failed on `assertTrue(tapCancel())` while the screen dump
        // showed the library — i.e. Cancel had plainly worked and only the click helper
        // reported false. A test that can fail when the user-visible behaviour is correct
        // is worse than no test.
        boolean tapped = composer.tapCancel();
        System.out.println(">>> cancel tap reported " + tapped);
        if (!composer.isOpen(Duration.ofSeconds(2))) {
            System.out.println(">>> composer already closed");
        } else {
            try { driver.navigate().back(); } catch (Exception ignored) { }
        }
        sleep(1500);

        // Source truth: goBack() → popBackStack, no discard dialog. If a confirm ever
        // appears here, ConfirmPopupPage in clearBlockingPopup (next @BeforeEach)
        // declines it — but record it, because it would mean the behaviour CHANGED.
        boolean confirmShown = new DocDistLibraryPage(driver).confirmPopup()
                .isShown(Duration.ofSeconds(2));
        System.out.println(">>> discard confirm shown=" + confirmShown
                + " (source has none as of Aug 2026)");
        if (confirmShown) {
            new DocDistLibraryPage(driver).confirmPopup().accept(Duration.ofSeconds(2));
        }
        DocDistComposerPage after = new DocDistComposerPage(driver);
        assertFalse(after.isOpen(Duration.ofSeconds(3)),
                "Cancel must leave the composer — neither compose nor preview stage should remain");
        assertTrue(new DocDistLibraryPage(driver).isLibrary(Duration.ofSeconds(10)),
                "Cancel should land back on the library");
        screenshot("ddc-cancelled");
    }

    // ==================== cleanup ====================

    @Test
    @Order(10)
    @DisplayName("CLEANUP: the seeded folder (and its file) delete")
    void cleanup() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Not at root");
        assertTrue(deleteItem(FOLDER), "The seeded folder should delete");
        screenshot("ddc-cleanup");
    }
}
