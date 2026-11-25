package com.zillit.automation.tests.docdist;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.ConfirmPopupPage;
import com.zillit.automation.pages.docdist.DocDistFolderEditorDialog;
import com.zillit.automation.pages.docdist.DocDistLibraryPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — the library cases the web E2E report (2026-11-17) has and the
 * Android suites lacked: Move that actually moves, search vs selection, library-wide
 * search, the folder bar under a no-match search, Edit with a blank name, sort order,
 * the help card across folders and the library's screen-reader labels.
 *
 * <p>Gap analysis: {@code web-e2e/gaps/document-distribution.md}. Each case carries its
 * web id ({@code [WEB …]}) and the sheet bug it guards ({@code DD-xx}). The installed dev
 * build (Zillit_32539-dev) already carries fixes for most of these bugs, so on that build
 * they are expected to PASS; on a build without the fix they FAIL naming the bug.
 *
 * <p>Seeds its own world, every name carrying this run's stamp so a library-wide search
 * for the stamp lists exactly this world:
 * <pre>
 *   QA_ZL_AUTO_&lt;ts&gt;_A        zl_dd_doc.pdf, zl_dd_word.docx, and subfolder _SUB
 *   QA_ZL_AUTO_&lt;ts&gt;_B        empty, with a description — the move destination
 *   QA_ZL_AUTO_&lt;ts&gt;_X        empty — the folder that MOVE-3 moves into _B
 * </pre>
 * C-12 removes the lot with a bulk delete; the last case is a safety net, and the
 * {@code QA_*} sweeper in {@link DocDistCleanupTest} is the backstop.
 */
@Order(TestOrder.DOCDIST_WEB_GAP)
@ExtendWith(ReasonLogger.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistWebGapTest extends DocDistBaseTest {

    private static final String STAMP = "QA_ZL_AUTO_" + (System.currentTimeMillis() % 100000);
    private static final String A = STAMP + "_A";
    private static final String B = STAMP + "_B";
    private static final String X = STAMP + "_X";
    private static final String SUB = STAMP + "_SUB";
    private static final String B_DESC = "ZL_AUTO move target";
    /** Subfolders MOVE-1 adds when the project has too few folders to overflow the Move sheet. */
    private static final int EXTRA_FOLDERS = 16;

    private static final String PDF = "zl_dd_doc.pdf";
    private static final String DOCX = "zl_dd_word.docx";

    private static boolean seededFolders;   // A, B, X exist
    private static boolean seededFiles;     // PDF + DOCX inside A
    private static boolean seededSub;       // SUB inside A
    private static boolean docxDeleted;     // DD-11 removed the docx
    private static boolean pdfMovedToB;     // MOVE-2 done
    private static boolean xMovedToB;       // MOVE-3 done

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    // ==================== helpers ====================

    private DocDistLibraryPage lib() { return new DocDistLibraryPage(driver); }

    /**
     * Opens a ROOT folder by searching for it first. The shared project's root holds
     * dozens of folders, and a library-wide search (DD-18) lists the row straight away.
     */
    private DocDistLibraryPage openRootFolder(String name) {
        DocDistLibraryPage lib = lib();
        assumeTrue(lib.isAtRoot() || lib.returnToRoot(), "Could not get to the library root");
        assumeTrue(lib.search(name), "Could not type into the library search");
        boolean listed = waitRendered(lib, name, Duration.ofSeconds(20));
        assumeTrue(listed, "Folder " + name + " is not listed (search) — seed missing");
        assumeTrue(lib.openItem(name), "Could not open folder " + name);
        sleep(1500);
        assumeTrue(lib.isInFolder(), "Should be inside " + name);
        // The search box belongs to the folder screen now; make sure it is empty there.
        lib.clearSearch();
        return lib;
    }

    /** Waits for a row with exactly this name to be RENDERED (no search, no scrolling). */
    private boolean waitRendered(DocDistLibraryPage lib, String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (lib.itemNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
            sleep(500);
        }
        return false;
    }

    private boolean waitGoneRendered(DocDistLibraryPage lib, String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (lib.itemNames().stream().noneMatch(n -> n.equalsIgnoreCase(name))) return true;
            sleep(500);
        }
        return false;
    }

    /** Leaves selection mode and closes any menu / sheet, back to a clean root. */
    private void tidy() {
        DocDistLibraryPage lib = lib();
        try {
            lib.confirmPopup().clearIfBlocking();
            if (moveSheetUp()) {
                driver.navigate().back();
                sleep(800);
            }
            if (lib.isSelectionMode()) lib.clearSelection();
            lib.clearSearch();
            lib.returnToRoot();
            lib.clearSearch();
        } catch (Exception ignored) { }
    }

    private boolean moveSheetUp() {
        try { return !driver.findElements(byAppId("dd_move")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** Ticks a row, opens the selection actions menu and taps Move. */
    private void openMoveFor(DocDistLibraryPage lib, String... rows) {
        for (String r : rows) {
            assumeTrue(lib.selectRow(r), "Could not tick " + r);
        }
        assumeTrue(lib.isSelectionMode(), "Selection bar expected after ticking " + List.of(rows));
        assumeTrue(lib.openSelectionActions(), "Could not open the selection actions menu");
        sleep(1000);
        assumeTrue(lib.tapMenuItem("Move"), "No Move in the selection menu: " + lib.visibleTexts(12));
        assumeTrue(lib.moveSheetOpen(Duration.ofSeconds(10)), "The Move sheet did not open");
        // The folder list loads asynchronously: at first only "Root" is there.
        assumeTrue(lib.waitMoveRowsLoaded(Duration.ofSeconds(15)),
                "The Move sheet's folder list never loaded beyond " + lib.moveRowNames());
        sleep(500);
    }

    // ==================== SEED ====================

    @Test
    @Order(1)
    @DisplayName("SEED: folders _A (PDF + DOCX + subfolder), _B (with description) and _X")
    void seed() {
        DocDistLibraryPage lib = lib();
        assumeTrue(createFolder(A), "Could not create " + A);
        sleep(1500);
        // B carries a description so C-4b can read it back off the row (WEB F02).
        boolean bMade = false;
        for (int attempt = 0; attempt < 3 && !bMade; attempt++) {
            var sheet = new com.zillit.automation.pages.docdist.DocDistCreateFolderSheet(driver);
            if (lib.openCreateFolder() && sheet.waitOpen(Duration.ofSeconds(8))) {
                bMade = sheet.createNamed(B, B_DESC);
            }
            if (!bMade) sleep(1500);
        }
        assumeTrue(bMade, "Could not create " + B);
        sleep(1500);
        assumeTrue(createFolder(X), "Could not create " + X);
        sleep(1500);
        assumeTrue(lib.waitForItem(A, Duration.ofSeconds(45)), A + " not listed");
        assumeTrue(lib.waitForItem(B, Duration.ofSeconds(45)), B + " not listed");
        assumeTrue(lib.waitForItem(X, Duration.ofSeconds(45)), X + " not listed");
        seededFolders = true;

        openRootFolder(A);
        assumeTrue(lib.hasUpload(), "Upload is not offered inside the folder on this build");
        assumeTrue(uploadFixturesIntoOpenFolder(new String[]{PDF, DOCX}),
                "The upload pipeline (picker → confirm) did not complete for " + PDF + " + " + DOCX);
        boolean pdf = lib.waitForItem(PDF, Duration.ofSeconds(60));
        boolean docx = lib.waitForItem(DOCX, Duration.ofSeconds(60));
        System.out.println(">>> seed files pdf=" + pdf + " docx=" + docx + " rows=" + lib.itemNames());
        assertTrue(pdf && docx, "Both fixtures should upload into " + A);
        seededFiles = true;

        // Subfolder inside A: Create folder inside a folder makes a child (NEST-2).
        seededSub = createFolder(SUB) && lib.waitForItem(SUB, Duration.ofSeconds(45));
        System.out.println(">>> seed subfolder " + SUB + " = " + seededSub);
        screenshot("ddw-seeded");
        tidy();
    }

    // ==================== MOVE-1 · DD-23 (High) ====================

    /**
     * [WEB L77, dd.spec.js:672] "Move here stays on screen when the folder list is long".
     * Sheet DD-23: the Move sheet stacked a wrap_content list above "Move here", so a long
     * folder list pushed the button off the screen. The fix (weight=1 on the list) keeps
     * the button pinned. The shared dev project holds far more folders than one screen.
     */
    @Test
    @Order(2)
    @DisplayName("MOVE-1 [WEB DD-23]: 'Move here' stays on screen with a long folder list")
    void moveHereStaysVisibleWithLongList() {
        assumeTrue(seededFiles, "Needs the seeded PDF in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        try {
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered in " + A);
            openMoveFor(lib, PDF);
            if (!lib.moveListScrollable()) {
                // Not enough folders in this project to overflow the sheet (the emulator
                // account's project holds a handful). Make the list long with subfolders
                // of X — not of A, where folders sort above the files and push them off
                // screen. X travels into B in MOVE-3 and goes with B in C-12.
                int have = lib.moveRowNames().size();
                System.out.println(">>> MOVE-1 only " + have + " rows — adding " + EXTRA_FOLDERS + " subfolders to " + X);
                tidy();
                openRootFolder(X);
                int made = 0;
                for (int i = 1; i <= EXTRA_FOLDERS; i++) {
                    if (createFolder(String.format("%s_F%02d", STAMP, i))) made++;
                    sleep(700);
                }
                System.out.println(">>> MOVE-1 created " + made + " extra subfolders");
                tidy();
                openRootFolder(A);
                assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered after adding folders");
                openMoveFor(lib, PDF);
            }

            boolean atOpen = lib.moveButtonOnScreen();
            String rectOpen = lib.moveButtonRect();
            List<String> firstScreen = lib.moveRowNames();
            System.out.println(">>> MOVE-1 at open: onScreen=" + atOpen + " rect=" + rectOpen
                    + " rows=" + firstScreen.size() + " scrollable=" + lib.moveListScrollable());
            screenshot("ddw-move1-open");
            assertTrue(atOpen, "[DD-23] 'Move here' (dd_move) must be on screen when the Move sheet "
                    + "opens; rect " + rectOpen);

            List<String> all = lib.moveScrollToEnd();
            boolean atEnd = lib.moveButtonOnScreen();
            String rectEnd = lib.moveButtonRect();
            System.out.println(">>> MOVE-1 after scrolling: rows seen=" + all.size()
                    + " onScreen=" + atEnd + " rect=" + rectEnd);
            screenshot("ddw-move1-end");
            assertTrue(atEnd, "[DD-23] 'Move here' must stay on screen after scrolling the folder "
                    + "list to its end; rect " + rectEnd);
            // Only meaningful when the list really is longer than the sheet.
            assumeTrue(all.size() > firstScreen.size(),
                    "The project has too few folders to overflow the Move sheet ("
                            + all.size() + " rows) — DD-23's long-list condition not met");
        } finally {
            tidy();
        }
    }

    // ==================== NEST-9 · DD-30 ====================

    /**
     * [WEB L125] Move and Attach can't tell same-named items apart. Sheet DD-30: the
     * Move rows now carry the full "Parent / Child" path. The subfolder must show as
     * "_A / _SUB", not a bare "_SUB".
     */
    @Test
    @Order(3)
    @DisplayName("NEST-9 [WEB DD-30]: Move lists a nested folder by its full path")
    void moveRowsShowFolderPath() {
        assumeTrue(seededFiles && seededSub, "Needs the seeded PDF and subfolder in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        try {
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered in " + A);
            openMoveFor(lib, PDF);
            List<String> rows = lib.moveScrollToEnd();
            String expected = A + " / " + SUB;
            boolean pathRow = rows.contains(expected);
            boolean bareRow = rows.contains(SUB);
            System.out.println(">>> NEST-9 path row '" + expected + "' present=" + pathRow
                    + " bare row present=" + bareRow);
            screenshot("ddw-nest9");
            assertTrue(pathRow, "[DD-30] The Move list should name the subfolder by its path '"
                    + expected + "'. Rows containing the stamp: "
                    + rows.stream().filter(r -> r.contains(STAMP)).toList());
            assertFalse(bareRow, "[DD-30] The nested folder must not be listed by its bare name");
        } finally {
            tidy();
        }
    }

    // ==================== D-15b · Watermark disabled for a .docx ====================

    /**
     * [WEB D02/05/06/07/09/10, L480 — partial] Watermark is only for PDF and images
     * ({@code FileTypeDetector.canWatermark}); the row menu keeps the item visible but
     * DISABLED for a Word file, and enabled for the PDF.
     */
    @Test
    @Order(4)
    @DisplayName("D-15 [WEB D-lifecycle]: the row menu's Watermark is disabled for .docx, enabled for PDF")
    void watermarkDisabledForDocx() {
        assumeTrue(seededFiles, "Needs the seeded files in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        com.zillit.automation.pages.docdist.DocDistWatermarkPage wm =
                new com.zillit.automation.pages.docdist.DocDistWatermarkPage(driver);
        try {
            assumeTrue(waitRendered(lib, DOCX, Duration.ofSeconds(20)), "DOCX not rendered");
            assumeTrue(lib.openRowMenu(DOCX), "Could not open the docx row menu");
            sleep(1000);
            Boolean docxWm = lib.menuItemEnabled("Watermark");
            System.out.println(">>> docx menu: " + lib.visibleTexts(10) + " watermarkEnabled(a11y)=" + docxWm);
            assertNotNull(docxWm, "The docx row menu should list Watermark");
            // Behaviour, not the accessibility flag: tap it. A disabled item does nothing;
            // an enabled one must at least open the sheet in its "not supported" state.
            lib.tapMenuItem("Watermark");
            sleep(1500);
            boolean sheet = wm.isDownloadSheetOpen(Duration.ofSeconds(4));
            boolean unsupported = sheet && wm.sheetSaysUnsupported();
            boolean downloadEnabled = sheet && wm.isDownloadEnabled();
            System.out.println(">>> docx Watermark tap → sheet=" + sheet + " unsupported=" + unsupported
                    + " downloadEnabled=" + downloadEnabled);
            screenshot("ddw-docx-watermark");
            if (sheet) { wm.tapSheetCancel(); sleep(800); }
            else if (!lib.isLibraryNow()) { driver.navigate().back(); sleep(700); }
            assertTrue(!sheet || (unsupported && !downloadEnabled),
                    "Watermark on a .docx must be refused (disabled item, or the sheet's 'not supported' "
                            + "state with Download off) — only PDF/images can be stamped. sheet=" + sheet
                            + " unsupported=" + unsupported + " downloadEnabled=" + downloadEnabled);

            assumeTrue(lib.openRowMenu(PDF), "Could not open the PDF row menu");
            sleep(1000);
            Boolean pdfWm = lib.menuItemEnabled("Watermark");
            System.out.println(">>> pdf menu watermarkEnabled=" + pdfWm);
            driver.navigate().back();
            sleep(700);
            assertTrue(Boolean.TRUE.equals(pdfWm), "Watermark must be enabled for a PDF");
        } finally {
            tidy();
        }
    }

    // ==================== EXTRA-2 · DD-05 ====================

    /**
     * [WEB L27] A search that hides every file hides the folder's Publish/Distribute.
     * Sheet DD-05: the folder bar must follow the folder's real files, not the filtered rows.
     */
    @Test
    @Order(5)
    @DisplayName("EXTRA-2 [WEB DD-05]: a no-match search keeps the folder's Publish and Distribute")
    void noMatchSearchKeepsFolderBar() {
        assumeTrue(seededFiles, "Needs the seeded files in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        try {
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered");
            assumeTrue(lib.hasFolderHeaderActions(), "The folder bar should show before searching");
            assumeTrue(lib.search("zzz-nomatch-" + STAMP), "Could not type the search");
            sleep(1000);
            String empty = lib.emptyText();
            boolean actions = lib.hasFolderHeaderActions();
            boolean emptyVariant = lib.folderHeaderEmptyShown();
            System.out.println(">>> EXTRA-2 empty='" + empty + "' actions=" + actions
                    + " emptyVariant=" + emptyVariant + " rows=" + lib.itemNames());
            screenshot("ddw-extra2");
            assertTrue(lib.itemNames().isEmpty(), "A no-match search should list nothing");
            assertEquals("No matching files or folders.", empty == null ? "" : empty.trim(),
                    "The no-match empty state should say so");
            assertTrue(actions, "[DD-05] Publish / Distribute this folder must stay while the search "
                    + "hides every file — the folder still holds files");
            assertFalse(emptyVariant, "[DD-05] 'Upload files to enable distribution' must not show for a "
                    + "folder that has files");
        } finally {
            tidy();
        }
    }

    // ==================== B-14 · DD-06 (guard) ====================

    @Test
    @Order(6)
    @DisplayName("B-14 [WEB DD-06]: a trailing space in the search is ignored")
    void trailingSpaceIgnored() {
        assumeTrue(seededFiles, "Needs the seeded files in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        try {
            assumeTrue(lib.search("pdf "), "Could not type the search");
            boolean listed = waitRendered(lib, PDF, Duration.ofSeconds(6));
            System.out.println(">>> B-14 'pdf ' → rows " + lib.itemNames());
            screenshot("ddw-b14");
            assertTrue(listed, "[DD-06] Searching 'pdf ' (trailing space) should still list " + PDF);
            assertFalse(lib.itemNames().contains(DOCX), "The docx does not match 'pdf'");
        } finally {
            tidy();
        }
    }

    // ==================== NEST-8 · DD-18 ====================

    /**
     * [WEB L64] A root search finds nested files and folders. Sheet DD-18: the search was
     * a filter over the open level only, so from the root (where no file lives) it could
     * never find a document. Each result now leads its meta line with its folder path.
     */
    @Test
    @Order(7)
    @DisplayName("NEST-8 [WEB DD-18]: a root search finds a nested folder and a nested file, with their path")
    void rootSearchFindsNested() {
        assumeTrue(seededFiles && seededSub, "Needs the seeded files and subfolder in " + A);
        DocDistLibraryPage lib = lib();
        try {
            assumeTrue(lib.isAtRoot(), "Must start at the root");
            assumeTrue(lib.search(SUB), "Could not type the search");
            boolean sub = waitRendered(lib, SUB, Duration.ofSeconds(8));
            String subMeta = lib.rowMeta(SUB);
            System.out.println(">>> NEST-8 '" + SUB + "' rows=" + lib.itemNames() + " meta='" + subMeta + "'");
            screenshot("ddw-nest8-folder");
            assertTrue(sub, "[DD-18] A root search for a nested folder's name should list it, got "
                    + lib.itemNames() + " empty='" + lib.emptyText() + "'");
            assertTrue(subMeta.contains(A), "[DD-18] The nested result should name its folder ('" + A
                    + "') in its meta line, got '" + subMeta + "'");

            assumeTrue(lib.search("zl_dd_word"), "Could not type the second search");
            boolean doc = waitRendered(lib, DOCX, Duration.ofSeconds(8));
            List<String> metas = lib.metasOfRowsNamed(DOCX);
            System.out.println(">>> NEST-8 'zl_dd_word' rows=" + lib.itemNames() + " metas=" + metas);
            screenshot("ddw-nest8-file");
            assertTrue(doc, "[DD-18] A root search should find the docx inside " + A
                    + ", got " + lib.itemNames() + " empty='" + lib.emptyText() + "'");
            // The project may hold other copies of the fixture; ours is the one under A.
            // Only rendered rows are read, so look further down when it is not on screen.
            boolean ours = metas.stream().anyMatch(m -> m.contains(A));
            assertTrue(ours, "[DD-18] One docx result should carry the path '" + A + "', metas=" + metas);
        } finally {
            tidy();
        }
    }

    // ==================== C-4b · WEB F02 ====================

    @Test
    @Order(8)
    @DisplayName("C-4b [WEB F02]: a folder's description shows on its row")
    void folderDescriptionOnRow() {
        assumeTrue(seededFolders, "Needs the seeded folders");
        DocDistLibraryPage lib = lib();
        try {
            assumeTrue(lib.search(B), "Could not search");
            assumeTrue(waitRendered(lib, B, Duration.ofSeconds(10)), B + " not listed");
            String meta = lib.rowMeta(B);
            System.out.println(">>> C-4b meta of " + B + " = '" + meta + "'");
            assertTrue(meta.contains(B_DESC),
                    "The folder row's meta line should carry its description '" + B_DESC + "', got '" + meta + "'");
        } finally {
            tidy();
        }
    }

    // ==================== C-11 · DD-07 ====================

    /**
     * [WEB L39] Edit folder with a blank name. Sheet DD-07: the dialog's Save closed it
     * silently on a blank name. Now Save is disabled while the name is blank or spaces.
     */
    @Test
    @Order(9)
    @DisplayName("C-11 [WEB DD-07]: Edit with a blank or spaces-only name keeps Save disabled; the name stays")
    void editBlankNameRefused() {
        assumeTrue(seededFolders, "Needs the seeded folders");
        DocDistLibraryPage lib = lib();
        DocDistFolderEditorDialog dialog = new DocDistFolderEditorDialog(driver);
        try {
            assumeTrue(lib.search(X), "Could not search");
            assumeTrue(waitRendered(lib, X, Duration.ofSeconds(10)), X + " not listed");
            assumeTrue(lib.openRowMenu(X), "Could not open the folder's ⋮");
            sleep(1000);
            assumeTrue(lib.tapMenuItem("Edit"), "No Edit in the folder menu");
            assumeTrue(dialog.waitOpen(Duration.ofSeconds(8)), "The Edit folder dialog did not open");

            assertTrue(dialog.setName(""), "Could not clear the name");
            sleep(400);
            boolean emptyEnabled = dialog.isSaveEnabled();
            assertTrue(dialog.setName("   "), "Could not type spaces");
            sleep(400);
            boolean spacesEnabled = dialog.isSaveEnabled();
            System.out.println(">>> C-11 save enabled: empty=" + emptyEnabled + " spaces=" + spacesEnabled);
            screenshot("ddw-c11");
            assertFalse(emptyEnabled, "[DD-07] Save must be disabled with an empty name");
            assertFalse(spacesEnabled, "[DD-07] Save must be disabled with a spaces-only name");
            assertTrue(dialog.isOpen(), "The dialog should still be open");
        } finally {
            dialog.dismissQuietly();
            sleep(600);
            tidy();
        }
        // The folder keeps its name.
        assertTrue(lib.search(X) && waitRendered(lib, X, Duration.ofSeconds(10)),
                "[DD-07] " + X + " should keep its name after the refused edit");
        tidy();
    }

    // ==================== SORT · WEB D13 ====================

    @Test
    @Order(10)
    @DisplayName("SORT-1 [WEB D13]: Name (Z–A) and Name (A–Z) really reorder the rows")
    void sortOrderChanges() {
        assumeTrue(seededFolders, "Needs the seeded folders");
        DocDistLibraryPage lib = lib();
        boolean changed = false;
        boolean restored = false;
        try {
            assumeTrue(lib.search(STAMP), "Could not search");
            assumeTrue(waitRendered(lib, A, Duration.ofSeconds(10)), "Seeded rows not listed");

            assumeTrue(lib.openSort(), "Could not open the sort menu");
            changed = true;
            assumeTrue(lib.chooseSort("Z–A"), "No 'Name (Z–A)' in the sort menu");
            sleep(2500);
            lib.scrollListToTop();
            List<String> desc = lib.itemNames().stream().filter(n -> n.startsWith(STAMP)).toList();

            assumeTrue(lib.openSort(), "Could not reopen the sort menu");
            assumeTrue(lib.chooseSort("A–Z"), "No 'Name (A–Z)' in the sort menu");
            restored = true;
            sleep(2500);
            lib.scrollListToTop();
            List<String> asc = lib.itemNames().stream().filter(n -> n.startsWith(STAMP)).toList();
            System.out.println(">>> SORT-1 Z–A=" + desc + " A–Z=" + asc);
            screenshot("ddw-sort");

            // Only the rendered rows are read (the search also lists MOVE-1's subfolders),
            // so assert their ORDER, and that the extremes land at the top when shown.
            List<String> descSorted = new java.util.ArrayList<>(desc);
            descSorted.sort(String.CASE_INSENSITIVE_ORDER.reversed());
            List<String> ascSorted = new java.util.ArrayList<>(asc);
            ascSorted.sort(String.CASE_INSENSITIVE_ORDER);
            assertTrue(desc.size() >= 3 && desc.equals(descSorted),
                    "Name (Z–A) should list the rows in descending order, got " + desc);
            assertTrue(asc.size() >= 3 && asc.equals(ascSorted),
                    "Name (A–Z) should list the rows in ascending order, got " + asc);
            assertEquals(A, asc.get(0), "Name (A–Z) should put " + A + " first");
            assertEquals(X, desc.get(0), "Name (Z–A) should put " + X + " first");
        } finally {
            // Leave the default (Name A–Z) in place even when the case failed half way.
            if (changed && !restored) {
                try { if (lib.openSort()) lib.chooseSort("A–Z"); } catch (Exception ignored) { }
            }
            tidy();
        }
    }

    // ==================== A-7 · DD-29 ====================

    @Test
    @Order(11)
    @DisplayName("A-7 [WEB DD-29]: a collapsed help card stays collapsed in a folder and back at the root")
    void helpCardStateCarries() {
        assumeTrue(seededFolders, "Needs the seeded folders");
        DocDistLibraryPage lib = lib();
        assumeTrue(lib.hasHelpCard(), "No help card on this build");
        if (!lib.helpBodyShownNow()) {          // start from the expanded default
            lib.toggleHelpCard();
            sleep(600);
        }
        assumeTrue(lib.helpBodyShownNow(), "The help card could not be expanded to start from");
        try {
            assertTrue(lib.toggleHelpCard(), "Could not tap the help card header");
            sleep(700);
            assertFalse(lib.helpBodyShownNow(), "Tapping the header should collapse the help card");

            openRootFolder(B);
            sleep(800);
            boolean inFolder = lib.helpBodyShownNow();
            lib.returnToRoot();
            sleep(800);
            boolean backAtRoot = lib.helpBodyShownNow();
            System.out.println(">>> A-7 body shown: inFolder=" + inFolder + " backAtRoot=" + backAtRoot);
            screenshot("ddw-a7");
            assertFalse(inFolder, "[DD-29] The collapsed help card re-expanded inside a folder");
            assertFalse(backAtRoot, "[DD-29] The collapsed help card re-expanded back at the root");
        } finally {
            tidy();
            if (!lib.helpBodyShownNow()) lib.toggleHelpCard();   // restore the default
        }
    }

    // ==================== A11Y-1 · DD-34 ====================

    @Test
    @Order(12)
    @DisplayName("A11Y-1 [WEB DD-34]: the view toggles, home crumb and row checkboxes are named for what they do")
    void accessibilityLabels() {
        assumeTrue(seededFiles, "Needs the seeded files");
        DocDistLibraryPage lib = lib();
        String list = lib.contentDescriptionOf("dd_view_list");
        String grid = lib.contentDescriptionOf("dd_view_grid");
        try {
            openRootFolder(A);
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered");
            String home = lib.contentDescriptionOf("dd_crumb_home");
            String check = lib.rowCheckDescription(PDF);
            System.out.println(">>> A11Y-1 list='" + list + "' grid='" + grid + "' home='" + home
                    + "' check='" + check + "'");
            assertEquals("List view", list, "[DD-34] The list toggle should be read as 'List view'");
            assertEquals("Grid view", grid, "[DD-34] The grid toggle should be read as 'Grid view'");
            assertTrue(home.toLowerCase().contains("home") || home.toLowerCase().contains("library"),
                    "[DD-34] The home crumb should mention home / the library, got '" + home + "'");
            assertFalse(check.equalsIgnoreCase("Select all"),
                    "[DD-34] A row checkbox must not be read as 'Select all'");
            assertTrue(check.contains(PDF), "[DD-34] The row checkbox should name its row, got '" + check + "'");
        } finally {
            tidy();
        }
    }

    // ==================== SEL-SEARCH · DD-11 ====================

    /**
     * [WEB L45, dd.spec.js:535] "a search unticks the files it hides, so Delete removes only
     * what is listed". Sheet DD-11: the hidden file stayed ticked and Delete took it too.
     * This case DOES delete — only the docx, which is this suite's own fixture — and then
     * proves the hidden PDF survived.
     */
    @Test
    @Order(13)
    @DisplayName("SEL-SEARCH [WEB DD-11]: a search unticks what it hides, so Delete removes only the listed file")
    void searchNarrowsSelectionAndDelete() {
        assumeTrue(seededFiles, "Needs the seeded files in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        try {
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20))
                    && waitRendered(lib, DOCX, Duration.ofSeconds(5)), "Both fixtures must be rendered");
            assumeTrue(lib.selectRow(PDF) && lib.selectRow(DOCX), "Could not tick both files");
            String two = lib.selectionCount();
            assumeTrue(two.contains("2"), "Two ticked rows should read '2 selected', got '" + two + "'");

            assumeTrue(lib.search("docx"), "Could not type the search");
            sleep(1000);
            List<String> rows = lib.itemNames();
            String narrowed = lib.selectionCount();
            System.out.println(">>> DD-11 after 'docx': rows=" + rows + " count='" + narrowed + "'");
            screenshot("ddw-dd11-search");
            assertTrue(rows.contains(DOCX) && !rows.contains(PDF), "Only the docx should be listed, got " + rows);
            assertEquals("1 selected", narrowed.trim(),
                    "[DD-11] The search hid the ticked PDF, so the selection must drop to '1 selected'");

            assumeTrue(lib.openSelectionActions(), "Could not open the selection menu");
            sleep(900);
            assumeTrue(lib.tapMenuItem("Delete"), "No Delete in the selection menu");
            ConfirmPopupPage confirm = lib.confirmPopup();
            assumeTrue(confirm.isShown(Duration.ofSeconds(8)), "No delete confirmation");
            System.out.println(">>> DD-11 confirm: '" + confirm.text() + "'");
            assertTrue(confirm.accept(Duration.ofSeconds(3)), "Could not confirm the delete");
            sleep(2500);
            docxDeleted = true;

            lib.clearSearch();
            sleep(1500);
            boolean pdfKept = waitRendered(lib, PDF, Duration.ofSeconds(15));
            boolean docxGone = waitGoneRendered(lib, DOCX, Duration.ofSeconds(15));
            System.out.println(">>> DD-11 after delete: rows=" + lib.itemNames());
            screenshot("ddw-dd11-after");
            assertTrue(pdfKept, "[DD-11] Delete removed the PDF that the search had hidden");
            assertTrue(docxGone, "The listed (ticked) docx should be deleted");
        } finally {
            tidy();
        }
    }

    // ==================== MOVE-2 · WEB D08 / L118 / DD-17 ====================

    @Test
    @Order(14)
    @DisplayName("MOVE-2 [WEB D08, DD-17]: a document moves to another folder, with one 'Moved 1 item' message")
    void documentMovesToAnotherFolder() {
        assumeTrue(seededFiles, "Needs the seeded PDF in " + A);
        DocDistLibraryPage lib = openRootFolder(A);
        String snack;
        try {
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered in " + A);
            openMoveFor(lib, PDF);
            assertTrue(lib.moveChooseDestination(B), "The Move list should offer " + B
                    + "; rows: " + lib.moveRowNames());
            assertTrue(lib.moveButtonEnabled(), "'Move here' should enable once a destination is chosen");
            assertTrue(lib.tapMoveHere(), "Could not tap 'Move here'");
            snack = lib.waitSnackbarText(Duration.ofSeconds(10));
            System.out.println(">>> MOVE-2 snackbar='" + snack + "'");
            screenshot("ddw-move2-snack");
            boolean closed = false;
            for (int i = 0; i < 10 && !closed; i++) { closed = !moveSheetUp(); if (!closed) sleep(500); }
            assertTrue(closed, "The Move sheet should close after a successful move");
            pdfMovedToB = true;

            boolean gone = waitGoneRendered(lib, PDF, Duration.ofSeconds(15));
            assertTrue(gone, "The PDF should have left " + A + ", rows=" + lib.itemNames());
            assertEquals("Moved 1 item", snack.trim(),
                    "[DD-14/DD-17] One move should read 'Moved 1 item' (singular)");
        } finally {
            tidy();
        }
        DocDistLibraryPage inB = openRootFolder(B);
        boolean arrived = inB.waitForItem(PDF, Duration.ofSeconds(30));
        System.out.println(">>> MOVE-2 rows in " + B + " = " + inB.itemNames());
        screenshot("ddw-move2-arrived");
        tidy();
        assertTrue(arrived, "The moved PDF should be listed in " + B);
    }

    // ==================== DL-1 · WEB D-lifecycle (a) ====================

    /**
     * [WEB D02/05/06/07/09/10, L480 — partial (a)] An actual Download from the row menu,
     * with its "saved to Downloads" confirmation. The copy the app writes to Downloads is
     * removed again afterwards so it cannot shadow the upload fixtures.
     */
    @Test
    @Order(15)
    @DisplayName("DL-1 [WEB D-lifecycle]: row menu Download saves the file and says '… saved to Downloads'")
    void rowDownloadSaves() {
        assumeTrue(seededFiles, "Needs the seeded PDF");
        String folder = pdfMovedToB ? B : A;
        DocDistLibraryPage lib = openRootFolder(folder);
        String snack = "";
        try {
            assumeTrue(waitRendered(lib, PDF, Duration.ofSeconds(20)), "PDF not rendered in " + folder);
            assumeTrue(lib.openRowMenu(PDF), "Could not open the PDF row menu");
            sleep(1000);
            assumeTrue(lib.tapMenuItem("Download"), "No Download in the row menu");
            if (lib.confirmPopup().isShown(Duration.ofSeconds(2))) {
                String t = lib.confirmPopup().text();
                lib.confirmPopup().cancel(Duration.ofSeconds(3));
                assumeTrue(false, "Download hit the rights dialog on this account: '" + t + "'");
            }
            snack = lib.waitSnackbarText(Duration.ofSeconds(20));
            System.out.println(">>> DL-1 snackbar='" + snack + "'");
            screenshot("ddw-dl1");
            assertTrue(snack.endsWith("saved to Downloads") && snack.contains("zl_dd_doc"),
                    "Download should confirm '<file> saved to Downloads', got '" + snack + "'");
        } finally {
            // Remove the downloaded copy (never the fixture itself, which is named exactly PDF).
            String saved = snack.replace(" saved to Downloads", "").trim();
            if (!saved.isEmpty() && !saved.equals(PDF)) {
                try {
                    driver.executeScript("mobile: deleteFile",
                            java.util.Map.of("remotePath", "/sdcard/Download/" + saved));
                    System.out.println(">>> DL-1 removed /sdcard/Download/" + saved);
                } catch (Exception e) {
                    System.out.println(">>> DL-1 could not remove the downloaded copy '" + saved + "': " + e.getMessage());
                }
            }
            tidy();
        }
    }

    // ==================== D-14 · DD-14 guard ====================

    /** [WEB L52 group] Date-group header count: one item reads "1 item" (guard; plural-aware). */
    @Test
    @Order(16)
    @DisplayName("D-14 [WEB DD-14]: a one-item date group reads '1 item'")
    void oneItemDateGroup() {
        assumeTrue(pdfMovedToB && docxDeleted && seededSub, "Needs A reduced to its one subfolder (MOVE-2, DD-11)");
        DocDistLibraryPage lib = openRootFolder(A);
        try {
            assumeTrue(lib.openSort(), "Could not open the sort menu");
            assumeTrue(lib.chooseSort("newest first"), "No 'Date (newest first)' in the sort menu");
            sleep(2500);
            List<String> headers = lib.dateHeaders();
            System.out.println(">>> D-14 rows=" + lib.itemNames() + " headers=" + headers);
            screenshot("ddw-d14");
            assertEquals(1, lib.itemNames().size(), "A should hold only " + SUB + " now, got " + lib.itemNames());
            assertTrue(headers.size() == 1 && headers.get(0).endsWith("· 1 item"),
                    "[DD-14] The one-item date group should read '… · 1 item', got " + headers);
        } finally {
            try { if (lib.openSort()) lib.chooseSort("A–Z"); } catch (Exception ignored) { }
            tidy();
        }
    }

    // ==================== MOVE-3 · WEB F06 ====================

    @Test
    @Order(17)
    @DisplayName("MOVE-3 [WEB F06]: a folder moves into another folder")
    void folderMovesIntoAnotherFolder() {
        assumeTrue(seededFolders, "Needs the seeded folders");
        DocDistLibraryPage lib = lib();
        try {
            assumeTrue(lib.search(X), "Could not search");
            assumeTrue(waitRendered(lib, X, Duration.ofSeconds(10)), X + " not listed");
            openMoveFor(lib, X);
            assertTrue(lib.moveChooseDestination(B), "The Move list should offer " + B
                    + "; rows: " + lib.moveRowNames());
            assertTrue(lib.tapMoveHere(), "Could not tap 'Move here'");
            String snack = lib.waitSnackbarText(Duration.ofSeconds(10));
            System.out.println(">>> MOVE-3 snackbar='" + snack + "'");
            xMovedToB = true;
            sleep(2000);
            // Still searching the whole library: X is found again, now under B's path.
            lib.search(X);
            boolean listed = waitRendered(lib, X, Duration.ofSeconds(10));
            String meta = lib.rowMeta(X);
            System.out.println(">>> MOVE-3 search after move: meta='" + meta + "'");
            screenshot("ddw-move3");
            assertTrue(listed && meta.contains(B),
                    X + " should now live under " + B + " (its search result's path), meta='" + meta + "'");
        } finally {
            tidy();
        }
        // Left the root: no unsearched root row by that name.
        DocDistLibraryPage root = lib();
        boolean atRoot = root.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(X));
        assertFalse(atRoot, X + " should no longer be listed at the root");
        DocDistLibraryPage inB = openRootFolder(B);
        boolean inside = inB.waitForItem(X, Duration.ofSeconds(30));
        System.out.println(">>> MOVE-3 rows in " + B + " = " + inB.itemNames());
        tidy();
        assertTrue(inside, X + " should be listed inside " + B);
    }

    // ==================== C-12 · WEB F09 (and the cleanup) ====================

    @Test
    @Order(18)
    @DisplayName("C-12 [WEB F09]: ticking two folders and Delete removes both (and everything inside)")
    void bulkDeleteTwoFolders() {
        assumeTrue(seededFolders, "Needs the seeded folders");
        DocDistLibraryPage lib = lib();
        try {
            assumeTrue(lib.search(STAMP), "Could not search");
            boolean listed = waitRendered(lib, A, Duration.ofSeconds(10)) && waitRendered(lib, B, Duration.ofSeconds(5));
            assumeTrue(listed, "Seeded folders not listed; search '" + STAMP + "' shows " + lib.itemNames()
                    + " empty='" + lib.emptyText() + "' selection=" + lib.isSelectionMode());
            assumeTrue(lib.selectRow(A) && lib.selectRow(B), "Could not tick both folders");
            String count = lib.selectionCount();
            assertTrue(count.contains("2"), "Two ticked folders should read '2 selected', got '" + count + "'");
            assumeTrue(lib.openSelectionActions(), "Could not open the selection menu");
            sleep(900);
            assumeTrue(lib.tapMenuItem("Delete"), "No Delete in the selection menu");
            int confirmations = 0;
            for (int i = 0; i < 3; i++) {
                if (!lib.confirmPopup().isShown(Duration.ofSeconds(i == 0 ? 8 : 2))) break;
                lib.confirmPopup().accept(Duration.ofSeconds(2));
                confirmations++;
                sleep(800);
            }
            assertTrue(confirmations >= 1, "Bulk delete must ask for confirmation");
            sleep(2500);
            lib.search(STAMP);
            boolean aGone = waitGoneRendered(lib, A, Duration.ofSeconds(20));
            boolean bGone = waitGoneRendered(lib, B, Duration.ofSeconds(5));
            System.out.println(">>> C-12 after delete, search '" + STAMP + "' → " + lib.itemNames());
            screenshot("ddw-c12");
            assertTrue(aGone && bGone, "Both folders should be gone, still listed: " + lib.itemNames());
            if (xMovedToB) {
                assertTrue(waitGoneRendered(lib, X, Duration.ofSeconds(5)),
                        X + " lived inside " + B + " and should have gone with it");
            }
            assertTrue(waitGoneRendered(lib, SUB, Duration.ofSeconds(5)),
                    SUB + " lived inside " + A + " and should have gone with it");
        } finally {
            tidy();
        }
    }

    // ==================== LIVE-1 · WEB F11 (two devices) ====================

    /**
     * [WEB F11, L540] A folder created on another device appears in the library without a
     * refresh, and disappears when deleted there. Needs a second device
     * ({@code ZILLIT_RECEIVER_UDID}); device B is driven by swapping this suite's driver for
     * the duration of each step, so all of DocDistBaseTest's navigation is reused.
     */
    @Test
    @Order(97)
    @DisplayName("LIVE-1 [WEB F11]: a folder created and deleted on another device shows up and goes without a refresh")
    void liveFolderFromOtherDevice() {
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is not set — this case needs a second device");
        String live = STAMP + "_RT";
        io.appium.java_client.android.AndroidDriver a = driver;
        io.appium.java_client.android.AndroidDriver b = null;
        DocDistLibraryPage libA = lib();
        try {
            b = com.zillit.automation.core.DriverFactory.create(udidB, 8210);
            // Device A: sit on the root with the stamp searched, so a new row is visible at once.
            assumeTrue(libA.isAtRoot(), "Device A should be on the library root");
            assumeTrue(libA.search(STAMP), "Could not search on device A");
            sleep(1500);

            driver = b;
            try {
                try { b.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage)); } catch (Exception ignored) { }
                assumeTrue(settleAfterRelaunch() || onBottomNavScreen() || new DocDistLibraryPage(b).isDocDistActivity(),
                        "Device B did not reach the app");
                assumeTrue(openModule(), "Device B could not open Document Distribution");
                assumeTrue(createFolder(live), "Device B could not create " + live);
            } finally {
                driver = a;
            }

            boolean appeared = waitRendered(libA, live, Duration.ofSeconds(45));
            System.out.println(">>> LIVE-1 appeared on A without refresh = " + appeared + " rows=" + libA.itemNames());
            screenshot("ddw-live-appeared");

            driver = b;
            boolean deletedOnB;
            try {
                deletedOnB = deleteItem(live);
            } finally {
                driver = a;
            }
            boolean gone = deletedOnB && waitGoneRendered(libA, live, Duration.ofSeconds(45));
            System.out.println(">>> LIVE-1 deletedOnB=" + deletedOnB + " gone on A = " + gone);
            assertTrue(appeared, "A folder created on another device should appear on this one without a refresh");
            assertTrue(deletedOnB, "Device B could not delete " + live);
            assertTrue(gone, "A folder deleted on another device should disappear here without a refresh");
        } finally {
            driver = a;
            if (b != null) {
                try { b.quit(); } catch (Exception ignored) { }
            }
            tidy();
        }
    }

    // ==================== safety-net cleanup ====================

    @Test
    @Order(99)
    @DisplayName("CLEANUP: anything this run left behind is deleted")
    void cleanup() {
        DocDistLibraryPage lib = lib();
        tidy();
        lib.search(STAMP);
        sleep(1500);
        List<String> left = lib.itemNames().stream().filter(n -> n.startsWith(STAMP)).toList();
        lib.clearSearch();
        System.out.println(">>> CLEANUP leftovers: " + left);
        for (String name : List.of(A, B, X, STAMP + "_RT")) {
            if (left.contains(name)) {
                lib.search(name);
                if (waitRendered(lib, name, Duration.ofSeconds(5)) && lib.openRowMenu(name)) {
                    sleep(1000);
                    if (lib.tapMenuItem("Delete")) {
                        for (int i = 0; i < 3 && lib.confirmPopup().isShown(Duration.ofSeconds(i == 0 ? 6 : 2)); i++) {
                            lib.confirmPopup().accept(Duration.ofSeconds(2));
                            sleep(600);
                        }
                    }
                }
                tidy();
            }
        }
        lib.search(STAMP);
        sleep(1500);
        List<String> after = lib.itemNames().stream()
                .filter(n -> n.equals(A) || n.equals(B) || n.equals(X)).toList();
        tidy();
        assertTrue(after.isEmpty(), "Cleanup: still listed " + after);
    }
}
