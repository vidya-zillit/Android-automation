package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.DocDistWatermarkPage;
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
 * DOCUMENT DISTRIBUTION — area M (watermark) of DOC_DISTRIBUTION_TEST_CASES.md, M-1…M-12.
 *
 * <p>Seeds its own world exactly like {@link DocDistFilesTest}: one QA folder, the fixture
 * PDF {@code zl_dd_doc.pdf} (pushed to /sdcard/Download by run-and-report.sh) uploaded
 * into it, and the folder deleted — contents included — by the last case. Watermarking
 * acts on a FILE, so every case here starts by entering that folder.
 *
 * <p><b>Area M is four screens, not one.</b> The spec table reads as though "watermark" is
 * a single flow; the app disagrees, and the difference changes what several cases can even
 * assert. {@link DocDistWatermarkPage} documents the four surfaces and the id that
 * identifies each. The consequences for this suite:
 *
 * <ul>
 *   <li><b>M-1 is written against what the app does, not what the table says.</b> The
 *       table says the file row's Watermark "opens the wizard". It does not:
 *       {@code LibraryFragment.onFileWatermark()} opens
 *       {@code WatermarkDownloadBottomSheet} (bottomsheet_dd_watermark_download). The
 *       WIZARD ({@code WatermarkWizardBottomSheet}) is only reachable from the COMPOSER's
 *       {@code dd_watermark_bar}. Both are asserted — the download sheet as M-1, the
 *       wizard as M-1b — and the contradiction is reported rather than papered over.</li>
 *   <li><b>M-2…M-10 live on the batch screen</b>, which is reached ONLY through the
 *       selection bar's "Watermark PDF and download". That path is wrapped in
 *       {@code runWithDownloadRights}, so a user without {@code canDownload} gets the
 *       admin-request dialog instead of the screen. That is a genuine environment gap and
 *       is skipped, never failed.</li>
 *   <li><b>M-11 and M-12 are composer cases.</b> "Watermark PDF and share" does not open a
 *       watermark screen at all — it opens the email composer and snacks a hint. The
 *       composer is where the switch, the bar, the wizard and the per-file preview live.</li>
 * </ul>
 *
 * <p><b>What this suite does NOT verify (M-10).</b> Download ZIP genuinely writes a file
 * to /sdcard/Download and then fires {@code ACTION_VIEW} at it. The case asserts the
 * app's own confirmation — the "Watermarked ZIP saved to Downloads" toast and the batch
 * screen popping back to the library — and nothing more. It does not open, unzip or
 * checksum the archive, and it does not claim the stamped pages are correct. The
 * {@code ACTION_VIEW} can hand the foreground to a third-party viewer, so the case
 * unwinds through {@link DocDistWatermarkPage#returnToApp()} in a finally block.
 *
 * <p><b>Real selection-bar labels.</b> The menu strings are "Watermark PDF and share" and
 * "Watermark PDF and download" (strings_docdist.xml), not the spec's "Watermark &amp;
 * share" shorthand — a prefix match on "Watermark" would hit whichever of the two came
 * first in the tree.
 *
 * <p><b>Three corrections made after the first live run (2026-08-08).</b> Every one was a
 * wrong assumption in the TEST, proved wrong by the app source:
 * <ul>
 *   <li><b>Half of each screen starts below the fold.</b> {@code dd_info} is the last
 *       child of the batch screen's NestedScrollView and the {@code dd_appearance}
 *       controls sit under a 320dp preview card on the download sheet, so both were
 *       missing from the dump and read as "the app rendered nothing". They are revealed
 *       before they are asserted now — see {@link DocDistWatermarkPage} for the layout
 *       evidence.</li>
 *   <li><b>A {@code .invalid} recipient is silently dropped.</b>
 *       {@code WatermarkBatchViewModel.addRecipientEmail} returns early unless
 *       {@code EmailValidator.isValid}, which (ADDRESS_BOOK_MOBILE_PROMPT change #2)
 *       requires the TLD to be in the bundled IANA set {@code res/raw/dd_iana_tlds.txt}.
 *       {@code .invalid} is an RFC-2606 reserved label and is NOT in that file, so the
 *       old fixture address could never produce a chip and could never enable Download
 *       ZIP. See {@link #RECIPIENT}.</li>
 *   <li><b>The composer's watermark switch arrives ON.</b>
 *       {@code ComposerAttachment.watermarkOn} defaults to {@code true} (ZL-19624), so
 *       {@code dd_watermark_bar} is already visible when the composer opens; the old case
 *       "turned watermarking on" by flipping the switch, which turned it OFF and hid the
 *       bar it then asserted on.</li>
 * </ul>
 */
@Order(TestOrder.DOC_DISTRIBUTION_WATERMARK)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistWatermarkTest extends DocDistBaseTest {

    /** Fixture pushed by run-and-report.sh → /sdcard/Download/zl_dd_doc.pdf. */
    private static final String DOC = "zl_dd_doc.pdf";
    private static final String FOLDER = "QA_WM_" + (System.currentTimeMillis() % 100000);

    /**
     * A throwaway recipient — nothing is ever SENT to it; the address is only a label
     * stamped onto a copy inside the ZIP.
     *
     * <p><b>It must have a REAL top-level domain.</b> The first run used
     * {@code qa.watermark@example.invalid} and both M-6 and M-10 failed with an empty
     * {@code dd_recipients} and a permanently disabled Download ZIP. That is the app
     * behaving correctly: {@code WatermarkBatchViewModel.addRecipientEmail} starts with
     * {@code if (!EmailValidator.isValid(e)) return}, and {@code EmailValidator} checks
     * the TLD against {@code IanaTlds.set()} — the 1452-entry IANA list bundled at
     * {@code res/raw/dd_iana_tlds.txt}. {@code invalid} is an RFC-2606 reserved label and
     * is not in that list, so the address was rejected without any UI feedback.
     *
     * <p>{@code example.com} keeps the address undeliverable (RFC 2606 reserves the whole
     * domain) while passing the validator, since {@code com} IS in the bundled list.
     */
    private static final String RECIPIENT = "qa.watermark@example.com";

    /** Set once the fixture has been proven to exist in the work folder. */
    private static boolean seeded;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    // ==================== seeding ====================

    /** Enters the suite's work folder, creating it on first need. */
    private DocDistLibraryPage enterWorkFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.waitForItem(FOLDER, Duration.ofSeconds(10))) {
            assumeTrue(createFolder(FOLDER), "Could not create the work folder");
            assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(45)), "Work folder not listed");
        }
        assumeTrue(lib.openItem(FOLDER), "Could not open the work folder");
        sleep(2000);
        assumeTrue(lib.isInFolder(), "Should be inside the work folder now");
        return lib;
    }

    /**
     * Guarantees the fixture PDF is inside the work folder, uploading it once.
     *
     * <p>The whole area needs a watermarkable file — {@code FileTypeDetector.canWatermark}
     * allows PDF/JPEG/PNG/WEBP only, and the fixture is a PDF, so the supported branch of
     * every screen is the one under test.
     */
    private DocDistLibraryPage enterWorkFolderWithFixture() {
        DocDistLibraryPage lib = enterWorkFolder();
        if (seeded && lib.waitForItem(DOC, Duration.ofSeconds(15))) return lib;
        if (!lib.waitForItem(DOC, Duration.ofSeconds(10))) {
            assumeTrue(lib.hasUpload(),
                    "Upload is not offered inside the folder on this build (stale build or "
                            + "rights-hidden, traps 3.11/3.17) — area M needs a seeded file");
            assumeTrue(uploadFixtureIntoOpenFolder(DOC), "The upload pipeline did not complete");
            assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(60)),
                    "The uploaded fixture never appeared in the folder");
        }
        seeded = true;
        return lib;
    }

    /**
     * Opens the selection actions menu with the fixture ticked.
     *
     * @return false when the selection bar or its menu did not come up
     */
    private boolean openSelectionMenuOnFixture(DocDistLibraryPage lib) {
        if (!lib.selectRow(DOC)) return false;
        if (!lib.isSelectionMode()) return false;
        if (!lib.openSelectionActions()) return false;
        sleep(1300);
        return true;
    }

    // ==================== M-1 · file row → Watermark ====================

    @Test
    @Order(1)
    @DisplayName("M-1: Watermark on a file row opens the single-doc watermark sheet (NOT the wizard)")
    void fileRowWatermarkOpensDownloadSheet() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);

        assumeTrue(lib.openRowMenu(DOC), "Could not open the file's ⋮ menu");
        sleep(1200);
        assumeTrue(lib.tapMenuItem(DocDistWatermarkPage.MENU_WATERMARK), "Could not tap Watermark");

        // Watermark itself is NOT posting-gated (onFileWatermark opens the sheet
        // unconditionally so a viewer can review the stamp) — only the Download button
        // inside it is. So a rights dialog here would be a finding, not an expectation.
        try {
            if (lib.confirmPopup().isShown(Duration.ofSeconds(3))) {
                System.out.println(">>> M-1 unexpected dialog: '" + lib.confirmPopup().text() + "'");
                lib.confirmPopup().cancel(Duration.ofSeconds(3));
            }

            boolean open = wm.isDownloadSheetOpen(Duration.ofSeconds(15));
            boolean wizard = wm.isWizardOpen(Duration.ofSeconds(2));
            System.out.println(">>> M-1 downloadSheet=" + open + " wizard=" + wizard
                    + " filename='" + wm.downloadSheetFilename() + "'"
                    + " supported=" + wm.sheetSaysSupported());
            screenshot("wm-file-row-sheet");

            assertTrue(open,
                    "The file row's Watermark must open WatermarkDownloadBottomSheet — "
                            + "the spec table's 'wizard' is wrong for this entry point");
            assertFalse(wizard,
                    "This entry point does not open the wizard; that sheet lives behind the "
                            + "composer's dd_watermark_bar (asserted by M-1b)");
            assertTrue(wm.downloadSheetFilename().contains(DOC),
                    "The sheet should name the document it will stamp, got '"
                            + wm.downloadSheetFilename() + "'");
            assertTrue(wm.sheetSaysSupported(),
                    "A PDF is in FileTypeDetector's watermark allow-list, so the supported "
                            + "block (dd_supported) should render, not dd_unsupported");
        } finally {
            wm.tapSheetCancel();
            sleep(1000);
        }
    }

    @Test
    @Order(2)
    @DisplayName("M-1 (cont.): the single-doc sheet takes two stamp lines and previews page 1")
    void downloadSheetFieldsAndPreview() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);

        assumeTrue(lib.openRowMenu(DOC), "Could not open the file's ⋮ menu");
        sleep(1200);
        assumeTrue(lib.tapMenuItem(DocDistWatermarkPage.MENU_WATERMARK), "Could not tap Watermark");
        assumeTrue(wm.isDownloadSheetOpen(Duration.ofSeconds(15)), "The watermark sheet did not open");

        try {
            // Line 1 resets to "CONFIDENTIAL" on every open, Line 2 to empty.
            String line1 = wm.stampLine1();
            System.out.println(">>> sheet defaults line1='" + line1 + "' line2='" + wm.stampLine2() + "'");

            assertTrue(wm.setStampLine1("QA STAMP ONE"), "Line 1 should accept text");
            assertTrue(wm.setStampLine2("QA STAMP TWO"), "Line 2 should accept text");
            assertEquals("QA STAMP ONE", wm.stampLine1(), "Line 1 should keep what was typed");
            assertEquals("QA STAMP TWO", wm.stampLine2(), "Line 2 should keep what was typed");

            // "Use full confidential notice" overwrites BOTH fields.
            assumeTrue(wm.tapUseFullNotice(), "Could not tap the full-notice shortcut");
            System.out.println(">>> after full notice line1='" + wm.stampLine1()
                    + "' line2='" + wm.stampLine2() + "'");
            assertFalse(wm.stampLine2().isBlank(),
                    "The full-notice shortcut fills BOTH lines, so Line 2 must not stay empty");

            // The live first-page render — Loading → Ready or Failed. Either settled state
            // is a pass; only a spinner that never resolves is a problem.
            boolean settled = wm.waitPreviewSettled(Duration.ofSeconds(40));
            System.out.println(">>> preview settled=" + settled + " image=" + wm.previewImageShown()
                    + " failed=" + wm.previewFailedShown());
            screenshot("wm-sheet-preview");
            assertTrue(settled,
                    "The first-page preview must resolve to an image or the failure state, "
                            + "not sit on dd_preview_progress forever");

            // The appearance block is the shared partial — its <include> id is what exists
            // at runtime (trap 3.16), and the controls inside it are this partial's own.
            //
            // FIRST-RUN CORRECTION. This asserted sizeChipsShown() straight away and got
            // false. The chips are not missing — they are BELOW THE FOLD.
            // bottomsheet_dd_watermark_download.xml stacks a 320dp preview card, Line 1,
            // Line 2, the full-notice button and the stamp hint above the <include> at
            // line 264, so on open only the block's "APPEARANCE" heading is on screen
            // (which is exactly why hasAppearanceBlock() passed while the chips did not).
            // A uiautomator dump carries only what is on screen, so this has to scroll
            // before it can conclude anything.
            assertTrue(wm.hasAppearanceBlock(), "The sheet embeds the appearance picker");
            assertTrue(wm.revealAppearance(),
                    "The appearance controls should be reachable by scrolling the sheet body — "
                            + "the sticky Download footer means the body is the only thing that "
                            + "scrolls");
            assertTrue(wm.sizeChipsShown(), "Small / Medium / Large chips should be offered");
        } finally {
            wm.tapSheetCancel();
            sleep(1000);
        }
    }

    // ==================== M-2 / M-9 · the batch screen ====================

    @Test
    @Order(3)
    @DisplayName("M-2/M-9: the batch screen chips the chosen files and the info block explains the output")
    void batchListsFilesAndExplainsOutput() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);
        assumeTrue(openBatchScreen(lib, wm), batchSkipReason());

        try {
            // dd_files is the FIRST thing in the scroll body, so read the chips before
            // anything scrolls the screen.
            List<String> chips = wm.fileChips();
            screenshot("wm-batch");
            assertTrue(chips.stream().anyMatch(c -> c.contains(DOC)),
                    "dd_files should chip the selected document, got " + chips);

            // M-9 — FIRST-RUN CORRECTION. This read dd_info with the screen untouched and
            // got "", which read as "the app rendered no info". It did render it: in
            // fragment_dd_watermark_batch.xml the info MaterialCardView (line 188) is the
            // LAST child of the NestedScrollView, below the dd_appearance include, and on
            // a phone the viewport ends at the opacity slider. infoText() now reveals it
            // first. Only dd_download_zip lives outside the scroll view, which is why the
            // enablement check below needs no scrolling.
            String info = wm.infoText();
            System.out.println(">>> batch files=" + chips + " info='" + info + "'");
            assertFalse(info.isBlank(),
                    "dd_info must not be blank — the fragment sets it unconditionally in "
                            + "render() from R.string.dd_watermark_will_generate, so a blank "
                            + "readout means the card was never brought on screen");
            assertTrue(info.toLowerCase().contains("zip") || info.toLowerCase().contains("generate"),
                    "dd_info should describe the ZIP output, got '" + info + "'");

            // With docs but no recipients the build is impossible — the app's own gate.
            assertFalse(wm.isDownloadZipEnabled(),
                    "Download ZIP must stay disabled until at least one recipient exists "
                            + "(canBuild = watermarkableCount > 0 && recipients.isNotEmpty())");
        } finally {
            leaveBatchScreen();
        }
    }

    // ==================== M-3 / M-4 / M-5 · line configuration ====================

    @Test
    @Order(4)
    @DisplayName("M-3/M-4/M-5: line 1 offers Name or Custom, line 2 None or Custom, and Custom reveals its input")
    void lineOptions() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);
        assumeTrue(openBatchScreen(lib, wm), batchSkipReason());

        try {
            // M-3 / M-5 — both radio pairs are always present; the custom INPUTS are gone
            // until their radio is picked.
            assertTrue(wm.line1NameShown() && wm.line1CustomShown(),
                    "Line 1 offers Recipient name and Custom");
            assertTrue(wm.line2NoneShown() && wm.line2CustomShown(),
                    "Line 2 offers None and Custom");

            // M-4 — Custom reveals dd_l1_custom_layout and the input keeps its text.
            assumeTrue(wm.chooseLine1Custom(), "Could not select Custom for line 1");
            assertTrue(wm.line1CustomInputShown(),
                    "Selecting Custom must reveal the line 1 input (it is View.GONE otherwise)");
            assertTrue(wm.typeLine1Custom("QA LINE ONE"), "The line 1 custom input should accept text");
            assertEquals("QA LINE ONE", wm.line1CustomText(), "Custom line 1 should reflect the input");

            assumeTrue(wm.chooseLine2Custom(), "Could not select Custom for line 2");
            assertTrue(wm.line2CustomInputShown(),
                    "Selecting Custom must reveal the line 2 input");
            assertTrue(wm.typeLine2Custom("QA LINE TWO"), "The line 2 custom input should accept text");
            assertEquals("QA LINE TWO", wm.line2CustomText(), "Custom line 2 should reflect the input");

            // Back to Name — the input hides again, which is the other half of the rule.
            assumeTrue(wm.chooseLine1Name(), "Could not switch line 1 back to Name");
            sleep(800);
            System.out.println(">>> after switching back to Name, l1 input shown="
                    + wm.line1CustomInputShown());
            screenshot("wm-batch-lines");
        } finally {
            leaveBatchScreen();
        }
    }

    // ==================== M-6 / M-7 · recipients ====================

    @Test
    @Order(5)
    @DisplayName("M-6/M-7: a typed email becomes a recipient chip; Choose offers the saved lists")
    void recipients() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);
        assumeTrue(openBatchScreen(lib, wm), batchSkipReason());

        try {
            // M-6 — nothing is SENT here: the batch fragment builds a ZIP with one stamped
            // copy per recipient. The address is a label on a file, not a mailbox.
            //
            // FIRST-RUN CORRECTION. This failed with "got []" against
            // qa.watermark@example.invalid. Tapping Add DID work — commitEmail() ran — but
            // addRecipientEmail() drops anything EmailValidator rejects, and the validator
            // gates on the bundled IANA TLD set, which has no `invalid` entry. The app was
            // right and the fixture address was wrong; see RECIPIENT.
            int before = wm.recipientChips().size();
            assumeTrue(wm.addRecipientEmail(RECIPIENT), "Could not type and add the recipient");
            sleep(1200);
            List<String> after = wm.recipientChips();
            System.out.println(">>> recipients before=" + before + " after=" + after);
            screenshot("wm-batch-recipient");
            assertTrue(after.size() > before,
                    "Adding a valid email should append a chip to dd_recipients (the chip group "
                            + "sits directly under the Add row, so it is on screen), got " + after);
            assertTrue(after.stream().anyMatch(c -> c.toLowerCase().contains(RECIPIENT.toLowerCase())),
                    "The new chip should carry the address, got " + after);

            // M-9 again, now with real numbers behind it.
            System.out.println(">>> info with one recipient: '" + wm.infoText() + "'");

            // M-7 — Choose opens a MaterialAlertDialog listing the saved distribution
            // lists. showPresetPicker() returns EARLY and silently when the project has
            // none, so "no dialog" is the app behaving correctly on an empty project.
            assumeTrue(wm.tapChoose(), "Could not tap Choose…");
            sleep(1500);
            boolean picker = wm.presetPickerShown(Duration.ofSeconds(5));
            System.out.println(">>> M-7 preset picker shown=" + picker);
            screenshot("wm-batch-choose");
            if (picker) {
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(900);
            } else {
                System.out.println(">>> M-7: no preset dialog — this project has no saved "
                        + "distribution lists, so showPresetPicker() returns without opening one");
            }
        } finally {
            leaveBatchScreen();
        }
    }

    // ==================== M-8 · appearance ====================

    @Test
    @Order(6)
    @DisplayName("M-8: size chips, the eight colour swatches and the opacity slider all apply")
    void appearanceOptions() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);
        assumeTrue(openBatchScreen(lib, wm), batchSkipReason());

        try {
            assertTrue(wm.hasAppearanceBlock(),
                    "The batch screen embeds partial_dd_watermark_appearance under the "
                            + "<include> id dd_appearance (trap 3.16 — the partial's own root id "
                            + "does not exist at runtime)");
            assertTrue(wm.revealAppearance(),
                    "The appearance controls should be reachable by scrolling the batch body");
            assertTrue(wm.sizeChipsShown(), "Small / Medium / Large should all be offered");

            int swatches = wm.swatchCount();
            System.out.println(">>> appearance swatches=" + swatches);

            // FIRST-RUN CORRECTION. The old case said "seven" and only asserted >= 2.
            // WatermarkAppearance.PRESET_SWATCHES lists EIGHT hexes (#0891b2 teal was
            // added with ZL-19547) and WatermarkAppearanceBinder.buildSwatches() adds one
            // frame per entry, so the exact count is knowable and worth pinning; the live
            // run reported 8, matching the source.
            assertEquals(8, swatches,
                    "WatermarkAppearanceBinder builds one frame per WatermarkAppearance."
                            + "PRESET_SWATCHES entry, and that list holds 8 hexes");
            // Index 0 is #6b7280, the DEFAULT — onSwatchSelected() returns early when the
            // hex already matches, so tapping it would prove nothing. Index 1 (#dc2626) is
            // a real change and sits well inside the screen, unlike the last swatch which
            // is clipped by the right edge.
            assertTrue(wm.chooseSwatch(1), "A non-default colour swatch should be tappable");

            // FIRST-RUN CORRECTION. The old case tapped LARGE and asserted Large was
            // checked — which was true before it tapped anything. WatermarkSize.fromWire
            // returns LARGE for a null/unknown wire value ("Was MEDIUM prior to
            // 2026-06-04"), and applyToUi() checks that chip on bind, so Large is the
            // DEFAULT; with app:selectionRequired="true" a tap on the already-checked chip
            // is a no-op. Tapping SMALL is the only version of this that proves anything.
            System.out.println(">>> size on arrival: small=" + wm.sizeSelected("dd_wm_size_small")
                    + " medium=" + wm.sizeSelected("dd_wm_size_medium")
                    + " large=" + wm.sizeSelected("dd_wm_size_large"));
            assertTrue(wm.sizeSelected("dd_wm_size_large"),
                    "WatermarkAppearance defaults to LARGE, so the Large chip arrives checked");
            assumeTrue(wm.chooseSizeSmall(), "Could not tap the Small size chip");
            sleep(700);
            System.out.println(">>> after tapping Small: small=" + wm.sizeSelected("dd_wm_size_small")
                    + " large=" + wm.sizeSelected("dd_wm_size_large"));
            assertTrue(wm.sizeSelected("dd_wm_size_small"), "Tapping Small should check the Small chip");
            assertFalse(wm.sizeSelected("dd_wm_size_large"),
                    "app:singleSelection=\"true\" means checking Small must uncheck Large");

            // FIRST-RUN CORRECTION. The gesture, not the expectation, was wrong: a
            // clickGesture plus a two-point dragGesture left the readout on 40%. Material
            // 1.13's BaseSlider ignores ACTION_DOWN inside a NestedScrollView and bails out
            // of ACTION_MOVE until the horizontal delta passes scaledTouchSlop, so the
            // scrub is now a real pointer sequence (see setOpacityFraction).
            String opacityBefore = wm.opacityValue();
            boolean moved = wm.setOpacityFraction(0.95);
            String opacityAfter = wm.opacityValue();
            int pctAfter = wm.opacityPercent();
            System.out.println(">>> opacity gesture reported moved=" + moved);
            System.out.println(">>> opacity before='" + opacityBefore + "' after='" + opacityAfter
                    + "' pct=" + pctAfter);
            screenshot("wm-batch-appearance");

            // The readout is formatted "%d%%", so a live value always ends in '%'.
            assertTrue(opacityAfter.endsWith("%"),
                    "dd_wm_opacity_value renders as '<n>%'; got '" + opacityAfter + "'");
            // The layout pins valueFrom=5 / valueTo=70, so a scrub to 95 % of the track
            // must land ON the cap, never above it. This is the CLAMP half of M-8 and it
            // holds whether or not the gesture landed.
            assertTrue(pctAfter >= 5 && pctAfter <= 70,
                    "partial_dd_watermark_appearance pins the slider to valueFrom=5 / "
                            + "valueTo=70, so the readout can never leave that band; got " + pctAfter);
            assertFalse(opacityAfter.equals(opacityBefore),
                    "Scrubbing the slider must move the readout — it stayed at '" + opacityBefore
                            + "'. The default is 40 % (WatermarkAppearance.DEFAULT_OPACITY) and "
                            + "95 % of the track clamps to 70 %, so this is a genuine change.");
        } finally {
            leaveBatchScreen();
        }
    }

    // ==================== M-10 · download ZIP ====================

    @Test
    @Order(7)
    @DisplayName("M-10: with docs and a recipient, Download ZIP enables and the app confirms the save")
    void downloadZip() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);
        assumeTrue(openBatchScreen(lib, wm), batchSkipReason());

        boolean tapped = false;
        try {
            assertFalse(wm.isDownloadZipEnabled(),
                    "Download ZIP starts disabled — there are no recipients yet");

            // FIRST-RUN CORRECTION. This failed with "should enable ==> false". Nothing
            // was wrong with canBuild: the fixture address ended in .invalid, so
            // EmailValidator rejected it and addRecipientEmail() never added a recipient
            // (same root cause as M-6). RECIPIENT now carries a real TLD.
            assumeTrue(wm.addRecipientEmail(RECIPIENT), "Could not add a recipient");
            sleep(1500);
            assumeTrue(!wm.recipientChips().isEmpty(),
                    "The recipient was typed and Added but produced no chip — "
                            + "WatermarkBatchViewModel.addRecipientEmail dropped it, which only "
                            + "happens when EmailValidator.isValid is false for '" + RECIPIENT + "'");
            boolean enabled = wm.isDownloadZipEnabled();
            System.out.println(">>> M-10 downloadZip enabled after one recipient=" + enabled
                    + " info='" + wm.infoText() + "'");
            screenshot("wm-batch-ready");
            assertTrue(enabled,
                    "One watermarkable doc + one recipient satisfies canBuild, so Download ZIP "
                            + "should enable");

            // BOUNDARY: from here the app writes a real ZIP into /sdcard/Download and then
            // fires ACTION_VIEW at it. This case asserts the app's OWN confirmation — the
            // "Watermarked ZIP saved to Downloads" toast, or the batch screen popping back
            // to the library, which the fragment only does on success. It does NOT open,
            // unzip or checksum the archive, and it makes no claim about the stamped pages.
            assumeTrue(wm.tapDownloadZip(), "Could not tap Download ZIP");
            tapped = true;

            boolean toast = wm.textShown(DocDistWatermarkPage.ZIP_SAVED_TOAST, Duration.ofSeconds(25));
            boolean left = !wm.isBatchOpen(Duration.ofSeconds(10));
            boolean foreign = wm.inForeignPackage();
            System.out.println(">>> M-10 toast=" + toast + " batchClosed=" + left
                    + " foregroundLeftApp=" + foreign);
            screenshot("wm-zip-result");

            assertTrue(toast || left || foreign,
                    "A successful build shows the 'saved to Downloads' toast and pops the batch "
                            + "screen (and may hand off to a viewer) — none of those happened, so "
                            + "the ZIP build did not complete. NOTE: this case never inspects the "
                            + "file on disk.");
        } finally {
            // The ACTION_VIEW can put a third-party archive viewer in front of everything.
            // Left alone that takes out every later case, so unwind unconditionally.
            if (tapped) {
                wm.returnToApp();
                sleep(1200);
            }
            leaveBatchScreen();
        }
    }

    // ==================== M-11 · watermark & share ====================

    @Test
    @Order(8)
    @DisplayName("M-11: 'Watermark PDF and share' from the selection bar opens the composer with the doc")
    void watermarkAndShareOpensComposer() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistComposerPage composer = new DocDistComposerPage(driver);

        assumeTrue(openSelectionMenuOnFixture(lib), "Could not open the selection actions menu");
        assumeTrue(lib.tapMenuItem(DocDistWatermarkPage.MENU_WATERMARK_SHARE),
                "Could not tap 'Watermark PDF and share'");

        // This action is posting-gated (runWithPostingRights), so a user without canPost
        // gets the admin-request dialog instead of the composer. Decline and skip.
        if (lib.confirmPopup().isShown(Duration.ofSeconds(4))) {
            String text = lib.confirmPopup().text();
            lib.confirmPopup().cancel(Duration.ofSeconds(3));
            lib.clearSelection();
            assumeTrue(false, "Watermark & share is posting-gated for this user: '" + text + "'");
        }

        try {
            boolean open = composer.isOpen(Duration.ofSeconds(25));
            // The action does NOT open a watermark screen — it opens the composer and
            // snacks "Toggle Watermark on each PDF before sending". That hint is the only
            // watermark-specific thing about it, and it is transient.
            boolean hint = composer.textShown("Toggle Watermark", Duration.ofSeconds(5));
            List<String> attached = composer.attachmentNames();
            System.out.println(">>> M-11 composer=" + open + " hint=" + hint + " attachments=" + attached);
            screenshot("wm-share-composer");

            assertTrue(open,
                    "'Watermark PDF and share' resolves the selection and opens the email "
                            + "composer — there is no separate watermark-and-share screen");
            assertTrue(attached.stream().anyMatch(a -> a.contains(DOC)),
                    "The composer should arrive with the selected document attached, got " + attached);
        } finally {
            leaveComposer();
        }
    }

    // ==================== M-1b / M-12 · the composer's watermark surfaces ====================

    @Test
    @Order(9)
    @DisplayName("M-1b/M-12: the watermark switch reveals the bar (→ wizard) and the eye opens the preview")
    void composerWizardAndPreview() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);

        assumeTrue(openSelectionMenuOnFixture(lib), "Could not open the selection actions menu");
        assumeTrue(lib.tapMenuItem(DocDistWatermarkPage.MENU_WATERMARK_SHARE),
                "Could not tap 'Watermark PDF and share'");
        if (lib.confirmPopup().isShown(Duration.ofSeconds(4))) {
            String text = lib.confirmPopup().text();
            lib.confirmPopup().cancel(Duration.ofSeconds(3));
            lib.clearSelection();
            assumeTrue(false, "Watermark & share is posting-gated for this user: '" + text + "'");
        }
        assumeTrue(composer.isOpen(Duration.ofSeconds(25)), "The composer did not open");

        try {
            // FIRST-RUN CORRECTION — the whole premise of this block was backwards.
            //
            // The old case flipped the switch and then asserted the bar was VISIBLE; it
            // got GONE. The switch only exists on watermarkable rows (AttachmentsAdapter
            // line 69 hides it otherwise) but it arrives ALREADY ON:
            // ComposerAttachment.watermarkOn defaults to `true` (ZL-19624 — "Every
            // freshly-picked attachment opens with watermark enabled; the user opts OUT"),
            // and EmailComposerViewModel exposes
            //   anyWatermark = attachments.any { it.watermarkOn && it.canWatermark }.
            // One attached PDF therefore satisfies it before anything is tapped, and the
            // old "flip it on" actually flipped it OFF and hid the bar being asserted.
            //
            // The rule is now exercised in BOTH directions, which is strictly stronger
            // than the original one-way assertion.
            Boolean initial = wm.attachmentWatermarkOn(DOC);
            System.out.println(">>> M-12 switch on arrival=" + initial
                    + " bar=" + wm.watermarkBarShown());
            assumeTrue(initial != null,
                    "No dd_watermark_switch on the '" + DOC + "' row — AttachmentsAdapter only "
                            + "shows it for FileTypeDetector.canWatermark types, so the fixture "
                            + "did not arrive as a PDF");
            assertTrue(initial,
                    "ComposerAttachment.watermarkOn defaults to true (ZL-19624), so a freshly "
                            + "attached PDF must arrive with its watermark switch already on");
            assertTrue(wm.watermarkBarShown(),
                    "dd_watermark_bar is bound to state.anyWatermark, and one watermarkable "
                            + "attachment with watermarkOn=true satisfies it — so the bar is "
                            + "visible the moment the composer opens, before anything is tapped");

            // OFF → the bar must disappear. This is the half the old case thought it was
            // testing.
            assumeTrue(wm.setAttachmentWatermark(DOC, false),
                    "Could not switch the row's watermark off");
            assertTrue(wm.watermarkBarHidden(Duration.ofSeconds(6)),
                    "With the only watermarkable attachment switched off, anyWatermark is false "
                            + "and dd_watermark_bar goes View.GONE");

            // ON again → back it comes, and that is the state the wizard needs.
            assumeTrue(wm.setAttachmentWatermark(DOC, true),
                    "Could not switch the row's watermark back on");
            sleep(1200);
            boolean bar = wm.watermarkBarShown();
            System.out.println(">>> M-12 watermark bar=" + bar + " summary='" + wm.watermarkSummary() + "'");
            assertTrue(bar,
                    "Switching the PDF's watermark back on must restore dd_watermark_bar");

            // M-1b — the WIZARD, at its real entry point.
            assumeTrue(wm.tapWatermarkBar(), "Could not tap the watermark bar");
            boolean wizard = wm.isWizardOpen(Duration.ofSeconds(15));
            System.out.println(">>> M-1b wizard=" + wizard + " preview='" + wm.wizardPreviewText() + "'");
            screenshot("wm-wizard");
            assertTrue(wizard,
                    "The composer's watermark bar opens WatermarkWizardBottomSheet — this, "
                            + "not the file row, is where the wizard lives");
            assertTrue(wm.line1NameShown() && wm.line1CustomShown() && wm.line2NoneShown()
                            && wm.line2CustomShown(),
                    "The wizard carries the same line 1 / line 2 choices as the batch screen");

            // Save with Custom selected but blank ABORTS — the sheet stays open with an
            // inline error. That is the assertable half of the app's own validation.
            assumeTrue(wm.chooseLine1Custom(), "Could not select Custom for line 1 in the wizard");
            assumeTrue(wm.typeLine1Custom(""), "Could not clear the wizard's custom line 1");
            wm.tapWizardSave();
            sleep(1200);
            boolean stillOpen = wm.isWizardOpen(Duration.ofSeconds(4));
            System.out.println(">>> wizard blocked-save keeps sheet open=" + stillOpen);
            assertTrue(stillOpen,
                    "Saving with Custom selected and blank must abort the dismiss "
                            + "(dd_watermark_custom_line1_required)");

            // Fix it and save for real, which closes the sheet.
            assumeTrue(wm.typeLine1Custom("QA WIZARD"), "Could not type into the wizard's custom line 1");
            assumeTrue(wm.tapWizardSave(), "Could not save the wizard");
            sleep(1500);
            assertFalse(wm.isWizardOpen(Duration.ofSeconds(4)),
                    "A valid Save should close the wizard and return the config to the composer");

            // M-12 — the eye button appears only while watermarkOn && canWatermark.
            assumeTrue(wm.attachmentWatermarkPreviewAvailable(DOC),
                    "The eye button should be on the watermarked attachment row");
            assumeTrue(wm.openAttachmentWatermarkPreview(DOC), "Could not tap the eye button");

            boolean preview = wm.isComposerPreviewOpen(Duration.ofSeconds(20));
            boolean settled = preview && wm.waitComposerPreviewSettled(Duration.ofSeconds(40));
            System.out.println(">>> M-12 previewSheet=" + preview + " settled=" + settled
                    + " filename='" + wm.composerPreviewFilename() + "'"
                    + " fallback=" + wm.composerPreviewFallbackShown());
            screenshot("wm-composer-preview");

            assertTrue(preview,
                    "The eye opens WatermarkComposerPreviewBottomSheet — identified by "
                            + "dd_filename + dd_close together, since each alone matches another screen");
            assertTrue(settled,
                    "The preview must resolve to a rendered page or the fallback, not sit on "
                            + "dd_preview_progress");
            wm.closeComposerPreview();
            sleep(1000);
        } finally {
            leaveComposer();
        }
    }

    // ==================== CLEANUP ====================

    @Test
    @Order(10)
    @DisplayName("CLEANUP: the QA watermark folder and its fixture are removed")
    void cleanup() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Could not return to the root for cleanup");
        assertTrue(deleteItem(FOLDER), "Cleanup: the QA folder should delete");
        screenshot("wm-cleanup");
    }

    // ---------------- shared navigation ----------------

    /**
     * Selection bar → "Watermark PDF and download" → the batch screen.
     *
     * <p>The menu item routes through {@code runWithDownloadRights}, so a user without
     * {@code canDownload} gets the admin-request dialog and never reaches the fragment.
     * That dialog is declined here (never "Proceed" — that posts a real request to an
     * admin's chat) and the caller skips.
     *
     * @return false when the batch screen did not come up, for any reason
     */
    private boolean openBatchScreen(DocDistLibraryPage lib, DocDistWatermarkPage wm) {
        if (!openSelectionMenuOnFixture(lib)) return false;
        if (!lib.tapMenuItem(DocDistWatermarkPage.MENU_WATERMARK_DOWNLOAD)) return false;

        if (lib.confirmPopup().isShown(Duration.ofSeconds(4))) {
            System.out.println(">>> batch blocked by rights dialog: '" + lib.confirmPopup().text() + "'");
            lib.confirmPopup().cancel(Duration.ofSeconds(3));
            lib.clearSelection();
            return false;
        }
        return wm.isBatchOpen(Duration.ofSeconds(25));
    }

    private String batchSkipReason() {
        return "The watermark batch screen was not reachable — 'Watermark PDF and download' "
                + "is gated by runWithDownloadRights, so a user without canDownload gets the "
                + "admin-request dialog instead (trap 3.6). Environment gap, not a defect.";
    }

    /** BACK out of the batch fragment and clear any leftover selection. */
    private void leaveBatchScreen() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistWatermarkPage wm = new DocDistWatermarkPage(driver);
        wm.returnToApp();
        for (int i = 0; i < 4 && wm.isBatchOpen(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception e) { break; }
        }
        lib.clearSelection();
        sleep(800);
    }

    /**
     * BACK out of the composer without sending anything.
     *
     * <p>The composer may ask for confirmation on discard; whichever family it uses,
     * {@code ConfirmPopupPage} handles both, and an unhandled MODAL here would take out
     * cleanup and everything after it.
     *
     * <p>The right button is only pressed once the dialog has been READ. A discard prompt's
     * confirm throws the draft away, which is what this suite wants — but the same button
     * on a rights prompt is "Proceed", which posts a real permission request into an
     * admin's chat. Anything mentioning rights is declined instead.
     */
    private void leaveComposer() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        for (int i = 0; i < 6 && composer.isOpen(Duration.ofSeconds(2)); i++) {
            try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception e) { break; }
            if (lib.confirmPopup().isShown(Duration.ofSeconds(2))) {
                String text = lib.confirmPopup().text();
                String right = lib.confirmPopup().confirmLabel();
                System.out.println(">>> composer exit dialog: '" + text + "' right='" + right + "'");
                boolean rightsPrompt = text.toLowerCase().contains("rights")
                        || text.toLowerCase().contains("permission")
                        || right.equalsIgnoreCase("Proceed");
                if (rightsPrompt) lib.confirmPopup().cancel(Duration.ofSeconds(3));
                else lib.confirmPopup().accept(Duration.ofSeconds(3));
            }
        }
        lib.clearSelection();
        sleep(800);
    }
}
