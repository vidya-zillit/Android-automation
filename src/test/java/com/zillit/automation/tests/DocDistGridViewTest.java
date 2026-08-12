package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistFilePreviewPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import io.appium.java_client.AppiumBy;
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
 * DOCUMENT DISTRIBUTION — GRID VIEW, the half of the library nothing else covers.
 *
 * <p>The list/grid switch sits to the right of the search box ({@code dd_view_toggle} →
 * {@code dd_view_list} / {@code dd_view_grid}), and until now every case ran in LIST view.
 * That is not a cosmetic gap: the two layouts are genuinely different surfaces, and the
 * FILE row is not even close to the same control set.
 *
 * <pre>
 *   item_dd_library_file       dd_card dd_check dd_icon dd_name dd_meta dd_more
 *   item_dd_library_file_grid  dd_card dd_check dd_icon dd_name dd_meta dd_action_chip
 *                              dd_send dd_publish dd_watermark dd_download dd_delete
 * </pre>
 *
 * <p>So in GRID view a file exposes all five actions as INLINE buttons and has no
 * {@code dd_more} at all, while in LIST view the same five live behind the ⋮ overflow.
 * D-2 asserts the overflow; nothing asserted the inline set, which means half of the
 * module's action surface was never touched. Folder rows differ too — the grid variant
 * adds {@code dd_action_chip} alongside {@code dd_send} / {@code dd_publish} /
 * {@code dd_more}.
 *
 * <p>{@code dd_action_chip} exists ONLY in the grid layouts, which makes it the reliable
 * "am I in grid view?" signal — far better than counting columns or guessing from bounds.
 *
 * <p>The suite seeds one folder with one document, exercises both views over it, and
 * deletes the folder at the end. It always restores LIST view, because every other suite
 * assumes it.
 */
@Order(TestOrder.DOC_DISTRIBUTION_GRID)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistGridViewTest extends DocDistBaseTest {

    private static final String DOC = "zl_dd_doc.pdf";
    private static final String FOLDER = "QA_GRID_" + (System.currentTimeMillis() % 100000);

    private static boolean seeded;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    /** Enters the seeded folder, creating and filling it on first need. */
    private DocDistLibraryPage enterSeededFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.waitForItem(FOLDER, Duration.ofSeconds(10))) {
            assumeTrue(createFolder(FOLDER), "Could not create the grid work folder");
            assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(45)), "Work folder not listed");
        }
        assumeTrue(lib.openItem(FOLDER), "Could not open the work folder");
        sleep(1500);
        return lib;
    }

    @Test
    @Order(1)
    @DisplayName("SEED: a folder with one document, so both views have a file row to render")
    void seed() {
        DocDistLibraryPage lib = enterSeededFolder();
        if (!lib.waitForItem(DOC, Duration.ofSeconds(8))) {
            assumeTrue(lib.hasUpload(), "Upload not offered inside the folder on this build");
            assumeTrue(uploadFixtureIntoOpenFolder(DOC), "Upload pipeline did not complete");
            assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(60)), "The document did not land");
        }
        seeded = true;
        lib.returnToRoot();
    }

    // ==================== the toggle itself ====================

    @Test
    @Order(2)
    @DisplayName("GRID-1: the toggle switches to grid — grid-only ids appear, and no row is lost")
    void toggleToGrid() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.hasViewToggle(), "No list/grid toggle on this build");
        assumeTrue(!lib.itemNames().isEmpty(), "Nothing in the library to lay out");

        // ESTABLISH list view rather than assuming it. The toggle's state is the app's,
        // not this run's: a suite that died mid-case (or an earlier aborted run) leaves
        // grid view on, and then this case fails on its own stale precondition while
        // reporting it as "the app did not start in list view".
        lib.showList();
        sleep(1000);
        assumeTrue(!inGridView(), "Could not establish LIST view as the starting point");
        List<String> asList = lib.allItemNamesScrolled();

        assumeTrue(lib.showGrid(), "Could not switch to grid");
        sleep(1200);
        boolean grid = inGridView();
        List<String> asGrid = lib.allItemNamesScrolled();
        System.out.println(">>> GRID-1 list=" + asList.size() + " grid=" + asGrid.size()
                + " gridMarkerPresent=" + grid);
        screenshot("ddg-grid");
        try {
            assertTrue(grid,
                    "Grid view must render the grid layouts — dd_action_chip exists only in "
                            + "item_dd_library_*_grid, so its absence means the switch did nothing");
            assertTrue(asGrid.containsAll(asList),
                    "Switching view must not drop rows. list=" + asList + " grid=" + asGrid);
        } finally {
            lib.showList();
            sleep(1000);
        }
    }

    // ==================== the real difference ====================

    @Test
    @Order(3)
    @DisplayName("GRID-2: a FILE row in grid exposes Send/Publish/Watermark/Download/Delete inline — and no ⋮")
    void gridFileRowExposesInlineActions() {
        assumeTrue(seeded, "Needs the seeded document");
        DocDistLibraryPage lib = enterSeededFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Seeded document missing");

        assumeTrue(lib.showGrid(), "Could not switch to grid");
        sleep(1200);
        assumeTrue(inGridView(), "Grid layouts did not render");

        boolean send = rowHas(DOC, "dd_send");
        boolean publish = rowHas(DOC, "dd_publish");
        boolean watermark = rowHas(DOC, "dd_watermark");
        boolean download = rowHas(DOC, "dd_download");
        boolean delete = rowHas(DOC, "dd_delete");
        boolean overflow = rowHas(DOC, "dd_more");
        System.out.println(">>> GRID-2 inline send=" + send + " publish=" + publish
                + " watermark=" + watermark + " download=" + download + " delete=" + delete
                + " dd_more=" + overflow);
        screenshot("ddg-file-actions");
        try {
            assertTrue(send && publish && watermark && download && delete,
                    "item_dd_library_file_grid declares all five actions as inline buttons; "
                            + "got send=" + send + " publish=" + publish + " watermark=" + watermark
                            + " download=" + download + " delete=" + delete);
            assertFalse(overflow,
                    "The grid file row has NO dd_more — its actions are inline, which is "
                            + "exactly why the list-view D-2 case does not cover this surface");
        } finally {
            lib.showList();
            sleep(800);
            lib.returnToRoot();
        }
    }

    @Test
    @Order(4)
    @DisplayName("GRID-3: a FOLDER row in grid keeps Send/Publish and its ⋮, plus the grid action chip")
    void gridFolderRowActions() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(20)), "Work folder missing");
        assumeTrue(lib.showGrid(), "Could not switch to grid");
        sleep(1200);
        assumeTrue(inGridView(), "Grid layouts did not render");

        boolean send = rowHas(FOLDER, "dd_send");
        boolean publish = rowHas(FOLDER, "dd_publish");
        boolean more = rowHas(FOLDER, "dd_more");
        boolean badge = rowHas(FOLDER, "dd_folder_badge");
        System.out.println(">>> GRID-3 folder send=" + send + " publish=" + publish
                + " more=" + more + " badge=" + badge);
        screenshot("ddg-folder-actions");
        try {
            assertTrue(send && publish && more,
                    "item_dd_library_folder_grid keeps Send, Publish AND the ⋮ overflow; "
                            + "got send=" + send + " publish=" + publish + " more=" + more);
        } finally {
            lib.showList();
            sleep(800);
        }
    }

    // ==================== behaviour, not just presence ====================

    @Test
    @Order(5)
    @DisplayName("GRID-4: selection works in grid — the checkbox still drives the selection bar")
    void selectionWorksInGrid() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(20)), "Work folder missing");
        assumeTrue(lib.showGrid(), "Could not switch to grid");
        sleep(1200);
        assumeTrue(inGridView(), "Grid layouts did not render");

        try {
            assumeTrue(lib.selectRow(FOLDER), "Could not tick the grid row's checkbox");
            assertTrue(lib.isSelectionMode(),
                    "The selection bar must work in grid view too — dd_check is in both layouts");
            String count = lib.selectionCount();
            System.out.println(">>> GRID-4 selection count='" + count + "'");
            assertTrue(count != null && count.contains("1"), "One ticked row = 1 selected");
            screenshot("ddg-selection");
        } finally {
            if (lib.isSelectionMode()) lib.clearSelection();
            lib.showList();
            sleep(800);
        }
    }

    @Test
    @Order(6)
    @DisplayName("GRID-5: tapping a file in grid opens the preview, same as list view")
    void gridFileOpensPreview() {
        assumeTrue(seeded, "Needs the seeded document");
        DocDistLibraryPage lib = enterSeededFolder();
        assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(20)), "Seeded document missing");
        assumeTrue(lib.showGrid(), "Could not switch to grid");
        sleep(1200);

        DocDistFilePreviewPage preview = new DocDistFilePreviewPage(driver);
        try {
            assumeTrue(lib.openItem(DOC), "Could not tap the grid tile");
            assertTrue(preview.isOpen(Duration.ofSeconds(20)),
                    "A grid tile must open FilePreviewActivity just as a list row does");
            System.out.println(">>> GRID-5 preview pages=" + preview.pdfPageCount());
            screenshot("ddg-preview");
        } finally {
            preview.close();
            lib.showList();
            sleep(800);
            lib.returnToRoot();
        }
    }

    @Test
    @Order(7)
    @DisplayName("GRID-6 / CLEANUP: switching back to list restores the list layout; the folder goes")
    void backToListAndCleanup() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.showList(), "Could not switch back to list");
        sleep(1200);
        assertFalse(inGridView(),
                "Back in list view the grid-only dd_action_chip must be gone");
        assertTrue(lib.isListReadable(), "The library must still render after switching back");
        screenshot("ddg-back-to-list");

        assertTrue(deleteItem(FOLDER), "Cleanup: the grid work folder should delete");
    }

    // ---------------- helpers ----------------

    /**
     * True when the GRID layouts are rendered. {@code dd_action_chip} is declared only in
     * item_dd_library_file_grid / item_dd_library_folder_grid, so its presence is an exact
     * signal — no column counting, no geometry guessing.
     */
    private boolean inGridView() {
        try {
            return !driver.findElements(byAppId("dd_action_chip")).isEmpty();
        } catch (Exception e) { return false; }
    }

    /** True when the row carrying this name also carries the given control id. */
    private boolean rowHas(String name, String controlId) {
        String xpath = "//*[@resource-id='" + appPackage + ":id/dd_card']"
                + "[.//*[@text=" + xpathLiteral(name) + "]]"
                + "//*[@resource-id='" + appPackage + ":id/" + controlId + "']";
        try {
            return !driver.findElements(AppiumBy.xpath(xpath)).isEmpty();
        } catch (Exception e) { return false; }
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }
}
