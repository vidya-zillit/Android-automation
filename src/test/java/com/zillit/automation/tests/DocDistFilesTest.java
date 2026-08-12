package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistFilePreviewPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.SystemFilePickerPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — area D (files &amp; selection) of DOC_DISTRIBUTION_TEST_CASES.md.
 *
 * <p>Seeds its own world: one QA folder, one fixture PDF uploaded into it
 * ({@code zl_dd_doc.pdf}, pushed to /sdcard/Download by run-and-report.sh), and deletes
 * the folder — contents included — at the end. Upload lives INSIDE a folder only
 * (trap 3.11 answered), so every case here starts by entering the work folder.
 *
 * <p>The file's confirm dialogs are the FRAMEWORK family (MaterialAlertDialog,
 * android:id/button1 — trap 3.15); {@code ConfirmPopupPage} handles both families.
 */
@Order(TestOrder.DOC_DISTRIBUTION_FILES)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistFilesTest extends DocDistBaseTest {

    /** Fixture pushed by run-and-report.sh → /sdcard/Download/zl_dd_doc.pdf. */
    private static final String DOC = "zl_dd_doc.pdf";
    private static final String FOLDER = "QA_DDF_" + (System.currentTimeMillis() % 100000);

    /** Set once the upload case has proven the file is listed. */
    private static boolean uploaded;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

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

    // ==================== D-1 · upload ====================

    @Test
    @Order(1)
    @DisplayName("D-1: Upload (inside the folder) adds the fixture PDF as a row")
    void uploadAddsDocument() {
        DocDistLibraryPage lib = enterWorkFolder();
        // The emulator's dev build (built from another branch) offers NO Upload even
        // inside a folder — skip there rather than fail; the current source always
        // shows it inside a folder (trap 3.11), which uploadAppearsInsideFolder asserts.
        assumeTrue(lib.hasUpload(),
                "Upload is not offered inside the folder on this build — stale build "
                        + "or rights-hidden (3.6/3.11); skipping the upload pipeline");
        assertTrue(uploadFixtureIntoOpenFolder(DOC),
                "The upload pipeline (picker → confirm) should complete");
        // The upload is asynchronous; the row appears when the API round-trip lands.
        boolean listed = lib.waitForItem(DOC, Duration.ofSeconds(60));
        System.out.println(">>> after upload: " + lib.itemNames());
        screenshot("ddf-uploaded");
        assertTrue(listed, "The uploaded document should be listed in the folder");
        uploaded = true;
    }

    // ==================== D-2 · row menu ====================

    @Test
    @Order(2)
    @DisplayName("D-2: the file row menu offers Send / Publish / Watermark / Download / Delete")
    void fileRowMenu() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(lib.openRowMenu(DOC), "Could not open the file's ⋮ menu");
        sleep(1200);

        boolean send = lib.textShown("Send", Duration.ofSeconds(3));
        boolean publish = lib.textShown("Publish", Duration.ofSeconds(2));
        boolean watermark = lib.textShown("Watermark", Duration.ofSeconds(2));
        boolean download = lib.textShown("Download", Duration.ofSeconds(2));
        boolean delete = lib.textShown("Delete", Duration.ofSeconds(2));
        System.out.println(">>> file menu send=" + send + " publish=" + publish
                + " watermark=" + watermark + " download=" + download + " delete=" + delete);
        screenshot("ddf-file-menu");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(send && publish && watermark && download && delete,
                "menu/dd_file_row_actions offers all five actions");
    }

    // ==================== D-3/D-4/D-5 · preview ====================

    @Test
    @Order(3)
    @DisplayName("D-3/D-4/D-5: tapping the PDF opens FilePreviewActivity, pages render, BACK returns")
    void previewOpensAndReturns() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(lib.openItem(DOC), "Could not tap the file row");

        DocDistFilePreviewPage preview = new DocDistFilePreviewPage(driver);
        assertTrue(preview.isOpen(Duration.ofSeconds(20)),
                "Tapping a file must open FilePreviewActivity (trap 3.8 — an ACTIVITY, not a fragment)");
        boolean pages = preview.hasPdfPage(Duration.ofSeconds(30));
        System.out.println(">>> preview open, pdf pages rendered=" + pages
                + " count=" + preview.pdfPageCount());
        screenshot("ddf-preview");
        assertTrue(pages, "A PDF should render at least one dd_page row");

        assertTrue(preview.close(), "BACK should finish the preview activity");
        // Not isLibrary(): that leans on the Compose chip, which the stale dev build
        // hides (3.17). The activity + the folder context are the build-agnostic proof.
        assertTrue(lib.isDocDistActivity(),
                "BACK from the preview must land back in DocumentDistributionActivity");
        assertTrue(lib.isInFolder() || lib.isLibrary(Duration.ofSeconds(5)),
                "BACK from the preview lands on the folder list it came from");
    }

    // ==================== D-8/D-9/D-10 · selection ====================

    @Test
    @Order(4)
    @DisplayName("D-8/D-9/D-10: checking a row enters selection mode, counts it, and Clear exits")
    void selectionMode() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");

        assumeTrue(lib.selectRow(DOC), "Could not tick the row's checkbox");
        assertTrue(lib.isSelectionMode(), "The selection bar should appear");
        String count = lib.selectionCount();
        System.out.println(">>> selection count reads '" + count + "'");
        screenshot("ddf-selection");
        assertTrue(count != null && count.contains("1"),
                "One ticked row should read as 1 selected, got '" + count + "'");

        assertTrue(lib.clearSelection(), "Could not tap Clear");
        sleep(1000);
        assertFalse(lib.isSelectionMode(), "Clear should dismiss the selection bar");
    }

    // ==================== D-11 · selection actions ====================

    @Test
    @Order(5)
    @DisplayName("D-11: the selection menu offers Share / Watermark&share / Watermark&download / Publish / Move / Delete")
    void selectionActionsMenu() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(lib.selectRow(DOC), "Could not tick the row");
        assumeTrue(lib.isSelectionMode(), "Selection bar expected");
        assumeTrue(lib.openSelectionActions(), "Could not open the selection actions menu");
        sleep(1200);

        // The labels are the app's OWN strings, read from res/values/strings.xml — the
        // map's "Watermark & share" / "Watermark & download" appear nowhere in the app
        // and could never have matched:
        //   dd_share_documents  = "Share documents"
        //   dd_watermark_share  = "Watermark PDF and share"
        //   dd_watermark_download = "Watermark PDF and download"
        boolean share = lib.textShown("Share documents", Duration.ofSeconds(3));
        boolean wmShare = lib.textShown("Watermark PDF and share", Duration.ofSeconds(2));
        boolean wmDownload = lib.textShown("Watermark PDF and download", Duration.ofSeconds(2));
        boolean publish = lib.textShown("Publish", Duration.ofSeconds(2));
        boolean move = lib.textShown("Move", Duration.ofSeconds(2));
        boolean delete = lib.textShown("Delete", Duration.ofSeconds(2));
        System.out.println(">>> selection menu share=" + share + " wmShare=" + wmShare
                + " wmDownload=" + wmDownload + " publish=" + publish
                + " move=" + move + " delete=" + delete);
        screenshot("ddf-selection-menu");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        lib.clearSelection();
        assertTrue(share && wmShare && wmDownload && publish && move && delete,
                "menu/dd_selection_actions offers all six actions");
    }

    // ==================== D-13 · move chooser ====================

    @Test
    @Order(6)
    @DisplayName("D-13: Move opens the folder chooser sheet")
    void moveOpensChooser() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(lib.selectRow(DOC), "Could not tick the row");
        assumeTrue(lib.openSelectionActions(), "Could not open the selection menu");
        sleep(1200);
        assumeTrue(lib.tapMenuItem("Move"), "Could not tap Move");

        boolean helper = isVisibleApp("dd_move_helper", Duration.ofSeconds(10));
        boolean moveBtn = isVisibleApp("dd_move", Duration.ofSeconds(2));
        System.out.println(">>> move sheet helper=" + helper + " moveBtn=" + moveBtn);
        screenshot("ddf-move-sheet");
        // Close the sheet WITHOUT moving — this suite's world stays in one folder.
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1000);
        lib.clearSelection();
        assertTrue(helper && moveBtn,
                "The move sheet should show its helper text and Move button");
    }

    // ==================== D-1b · EVERY supported media type ====================

    /**
     * The module's allow-list is not "documents" — {@code FileTypeDetector.ALLOWED_EXT} is
     * PDF + raster/modern images + TIFF + SVG/ICO + Word + Excel + vCard, and only
     * PDF/JPEG/PNG/WEBP can be watermarked. One PDF proves almost nothing about that, so
     * this uploads one fixture per FAMILY in a single multi-select pick and checks each
     * lands.
     */
    @Test
    @Order(8)
    @DisplayName("D-1b: image, Word, Excel and vCard all upload (every supported family)")
    void uploadsEverySupportedType() {
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.hasUpload(), "Upload not offered on this build (3.17) — skipping");

        String[] fixtures = {"zl_dd_pic.jpg", "zl_dd_word.docx", "zl_dd_book.xlsx", "zl_dd_card.vcf"};
        assertTrue(uploadFixturesIntoOpenFolder(fixtures),
                "The multi-select upload of one file per supported family should complete");

        StringBuilder missing = new StringBuilder();
        for (String f : fixtures) {
            boolean listed = lib.waitForItem(f, Duration.ofSeconds(60));
            System.out.println(">>> " + f + " listed=" + listed);
            if (!listed) missing.append(f).append(' ');
        }
        screenshot("ddf-all-types");
        assertTrue(missing.length() == 0,
                "Every supported type should upload. Missing: " + missing
                        + " (allow-list is PDF + images + Word + Excel + vCard)");
    }

    @Test
    @Order(9)
    @DisplayName("D-1c: an UNSUPPORTED type (.txt) is refused — the allow-list actually bites")
    void unsupportedTypeIsRejected() {
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.hasUpload(), "Upload not offered on this build (3.17) — skipping");

        // WHERE the rejection actually happens: the app launches the picker with
        // FileTypeDetector.PICKER_MIME_TYPES — its entire allow-list — so DocumentsUI
        // itself refuses the file. It still LISTS a .txt (greyed out, enabled="false"),
        // but it cannot be chosen, and the app never sees it. The in-app
        // "skipped unsupported" snackbar is a second line of defence that the UI cannot
        // normally reach.
        //
        // So the assertion is "listed but NOT selectable", which is checkable and cannot
        // false-pass. Asserting "no row appeared" alone would pass for any reason at all,
        // including a broken picker — which is exactly how this case passed earlier while
        // no upload had even been attempted.
        assumeTrue(lib.openUpload(), "Upload chip not reachable");
        SystemFilePickerPage picker = new SystemFilePickerPage(driver);
        assumeTrue(picker.isOpen(Duration.ofSeconds(20)), "The system picker did not open");

        Boolean selectable = picker.fileSelectable("zl_dd_bad.txt");
        System.out.println(">>> zl_dd_bad.txt selectable in the picker = " + selectable);
        screenshot("ddf-unsupported");
        try {
            assumeTrue(selectable != null,
                    "zl_dd_bad.txt was not listed in the picker at all — push the fixture "
                            + "(prep_device) before claiming anything about the allow-list");
            assertFalse(selectable,
                    "A .txt is outside FileTypeDetector's allow-list, and the app passes "
                            + "that list to the picker as its MIME filter — so the file must "
                            + "be shown DISABLED and be impossible to choose");
        } finally {
            picker.cancel();
            sleep(1500);
        }
        assertFalse(lib.hasItem("zl_dd_bad.txt"),
                "Nothing was chosen, so no .txt may appear in the folder");
    }

    // ==================== D-8b · long-press (the map's other half) ====================

    /**
     * The 152-case map says "Long-press / check enters selection mode". The checkbox half
     * is true (D-8). The long-press half is NOT: {@code LibraryAdapter} binds
     * {@code dd_card} to a plain click ({@code onFileClick} / {@code onFolderClick}) and
     * registers no long-click listener anywhere in the library — the module's only
     * long-presses are the date-filter shortcut, the tooltip helper and history recipient
     * rows. This case proves it on the device instead of asserting a rule that does not
     * exist, so the map can be corrected from evidence.
     */
    @Test
    @Order(10)
    @DisplayName("D-8b: long-pressing a row does NOT enter selection mode (checkbox-only, by design)")
    void longPressDoesNotSelect() {
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(!lib.isSelectionMode(), "Must start out of selection mode");

        assumeTrue(lib.longPressRow(DOC), "Could not long-press the row");
        boolean selectionStarted = lib.isSelectionMode();
        boolean previewOpened = new DocDistFilePreviewPage(driver).isPreviewActivity();
        System.out.println(">>> long-press → selectionMode=" + selectionStarted
                + " previewActivity=" + previewOpened);
        screenshot("ddf-long-press");
        try {
            assertFalse(selectionStarted,
                    "The library registers no long-click listener — selection is entered "
                            + "through the always-visible dd_check checkbox. If this ever "
                            + "starts passing, the app gained long-press and the map's D-8 "
                            + "wording became right.");
        } finally {
            if (previewOpened) new DocDistFilePreviewPage(driver).close();
            if (lib.isSelectionMode()) lib.clearSelection();
        }
    }

    // ==================== D-16b · date-group bulk select ====================

    /**
     * Not in the original map at all: rows are grouped under
     * {@code item_dd_library_date_header}, and each header carries its own
     * {@code dd_header_check} that selects the whole day's group in one tap.
     */
    @Test
    @Order(11)
    @DisplayName("EXTRA: the date-header checkbox selects a whole day's group at once")
    void dateHeaderSelectsGroup() {
        // At the ROOT, not inside the work folder: the observed grouping headers
        // ("Yesterday · Aug 7, 2026") sit above the root's folder list, while inside a
        // folder the similar-looking "Today · Aug 8, 2026 · 426 B" text is the FILE row's
        // own dd_meta — not a header, and carrying no dd_header_check to tick.
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Need the library root for date grouping");
        assumeTrue(lib.hasDateHeaders(), "No date headers rendered — nothing to group-select");
        System.out.println(">>> date headers: " + lib.dateHeaders());

        assumeTrue(lib.selectFirstDateGroup(), "Could not tick the date header");
        boolean selecting = lib.isSelectionMode();
        String count = lib.selectionCount();
        System.out.println(">>> group select → selectionMode=" + selecting + " count='" + count + "'");
        screenshot("ddf-group-select");
        try {
            assertTrue(selecting,
                    "Ticking a date header should enter selection mode with that group selected");
            assertTrue(count != null && count.matches(".*\\d.*"),
                    "The selection bar should report how many rows the group selected, got '"
                            + count + "'");
        } finally {
            if (lib.isSelectionMode()) lib.clearSelection();
        }
    }

    // ==================== EXTRA · folder-level distribute / publish ====================

    /**
     * The folder header bar and the folder ROW both expose Publish and Distribute — the
     * "send/publish this whole folder" paths. The map lists their ids but has no case for
     * them, so neither entry point was covered.
     */
    @Test
    @Order(12)
    @DisplayName("EXTRA: inside a folder with files, the header offers Publish and Distribute")
    void folderHeaderOffersPublishAndDistribute() {
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)),
                "The header's actions only appear when the folder HAS files");

        boolean actions = lib.hasFolderHeaderActions();
        System.out.println(">>> folder header actions shown=" + actions
                + " emptyVariant=" + lib.folderHeaderEmptyShown());
        screenshot("ddf-folder-header");
        assertTrue(actions,
                "LibraryFragment shows ddFolderHeaderActions when the folder holds files "
                        + "(and ddFolderHeaderEmpty when it does not)");

        // Distribute opens the composer pre-loaded with the folder's documents. Open it,
        // prove it, and cancel — this suite never sends (see DocDistSendTest for F-25).
        assumeTrue(lib.tapFolderDistribute(), "Could not tap Distribute");
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        boolean opened = composer.isOpen(Duration.ofSeconds(15));
        System.out.println(">>> folder Distribute → composer opened=" + opened
                + " attachments=" + (opened ? composer.attachmentNames() : "n/a"));
        screenshot("ddf-folder-distribute");
        try {
            assertTrue(opened, "Distribute should open the composer for the folder's files");
        } finally {
            if (opened) composer.tapCancel();
            sleep(1500);
        }
    }

    // ==================== EXTRA · distribute ONE document by email ====================

    /**
     * The single-file "Send" path — the row menu's own entry into the composer, distinct
     * from Compose (empty composer) and from the folder header's Distribute (whole
     * folder). The composer should come up already carrying this document, which is the
     * whole point of the action.
     */
    @Test
    @Order(13)
    @DisplayName("EXTRA: file row → Send opens the composer with THAT document attached")
    void fileRowSendOpensComposerWithAttachment() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(lib.openRowMenu(DOC), "Could not open the file's ⋮ menu");
        sleep(1200);
        assumeTrue(lib.tapMenuItem("Send"), "Could not tap Send in the row menu");

        DocDistComposerPage composer = new DocDistComposerPage(driver);
        boolean opened = composer.isOpen(Duration.ofSeconds(15));
        var attachments = opened ? composer.attachmentNames() : java.util.List.<String>of();
        System.out.println(">>> row Send → composer=" + opened + " attachments=" + attachments);
        screenshot("ddf-row-send");
        try {
            assertTrue(opened, "Send from a file row should open the composer");
            assertTrue(attachments.stream().anyMatch(n -> n.contains("zl_dd_doc")),
                    "The composer should already carry the document that was sent from, "
                            + "got " + attachments);
        } finally {
            // Never sends: DocDistSendTest owns the only real send (F-25).
            if (opened) composer.tapCancel();
            sleep(1500);
        }
    }

    // ==================== EXTRA · publish a document into another module ==============

    /**
     * Publish is how this module writes into the REST of the app (Call Sheet, Info,
     * Confidential Info, Production Report …). This case opens the sheet from a file row
     * and proves it is the publish sheet, then cancels — actually publishing would drop a
     * document into another module that this suite cannot clean up, which is
     * {@code DocDistPublishTest}'s documented boundary (N-9/N-14).
     */
    @Test
    @Order(14)
    @DisplayName("EXTRA: file row → Publish opens the publish sheet (then cancels — no cross-module write)")
    void fileRowPublishOpensSheet() {
        assumeTrue(uploaded, "Needs the uploaded fixture");
        DocDistLibraryPage lib = enterWorkFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Fixture missing");
        assumeTrue(lib.openRowMenu(DOC), "Could not open the file's ⋮ menu");
        sleep(1200);
        assumeTrue(lib.tapMenuItem("Publish"), "Could not tap Publish in the row menu");
        sleep(2000);

        boolean sheet = isVisibleApp("dd_publish", Duration.ofSeconds(10))
                || isVisibleApp("dd_files", Duration.ofSeconds(2))
                || lib.textShown("Call sheet", Duration.ofSeconds(2));
        System.out.println(">>> row Publish → sheet=" + sheet);
        screenshot("ddf-row-publish");
        try {
            assertTrue(sheet,
                    "Publish from a file row should open the publish sheet (its categories "
                            + "and dd_publish button)");
        } finally {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
            lib.confirmPopup().clearIfBlocking();
        }
    }

    // ==================== D-7 · delete + CLEANUP ====================

    @Test
    @Order(15)
    @DisplayName("D-7 / CLEANUP: deleting the file confirms and removes it; the folder goes too")
    void deleteFileAndCleanup() {
        DocDistLibraryPage lib = enterWorkFolder();
        if (lib.waitForItem(DOC, Duration.ofSeconds(15))) {
            assumeTrue(lib.openRowMenu(DOC), "Could not open the file's ⋮ menu");
            sleep(1200);
            assumeTrue(lib.tapMenuItem("Delete"), "Could not tap Delete");
            assertTrue(lib.confirmPopup().isShown(Duration.ofSeconds(8)),
                    "Deleting a file must ask for confirmation");
            System.out.println(">>> file delete confirm: '" + lib.confirmPopup().text() + "'");
            assertTrue(lib.confirmPopup().accept(Duration.ofSeconds(3)), "Could not confirm");
            sleep(2500);

            boolean gone = false;
            for (int i = 0; i < 8 && !gone; i++) {
                gone = !lib.hasItem(DOC);
                if (!gone) { lib.pullToRefresh(); sleep(2000); }
            }
            screenshot("ddf-file-deleted");
            assertTrue(gone, "The deleted file should be gone from the folder");
        }

        // Cleanup: back to root, remove the QA folder (and anything left inside it).
        assumeTrue(lib.returnToRoot(), "Could not return to the root for cleanup");
        assertTrue(deleteItem(FOLDER), "Cleanup: the QA folder should delete");
        screenshot("ddf-cleanup");
    }

    // ---------------- helpers ----------------

    private boolean isVisibleApp(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                for (var e : driver.findElements(byAppId(id))) {
                    if (e.isDisplayed()) return true;
                }
            } catch (Exception ignored) { }
            sleep(300);
        }
        return false;
    }
}
