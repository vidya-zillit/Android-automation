package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistLibraryPage;
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
 * DOCUMENT DISTRIBUTION — NESTED FOLDERS: folder inside folder inside folder.
 *
 * <p>Everything else in this family works one level deep, which quietly assumed the
 * library is flat. It is not: {@code CreateFolderBottomSheet.newInstance} takes a
 * {@code parentId} / {@code parentName}, and {@code LibraryFragment.openCreateFolder()}
 * passes {@code currentState.folderId} — so Create-folder INSIDE a folder makes a child,
 * and the tree can go arbitrarily deep.
 *
 * <p>Depth is where several of this module's known traps compound, which is the real
 * reason to test it:
 * <ul>
 *   <li><b>The breadcrumb.</b> {@code dd_crumb_current} shows only the CURRENT folder's
 *       name, and the home crumb is already known to be inert one level down (trap 3.21 —
 *       {@code popBackStack(ddLibraryFragment, false)} pops nothing because opening a
 *       folder self-navigates). At depth 3 that means BACK is the only way out, and this
 *       suite proves how many presses it really takes.</li>
 *   <li><b>Upload is folder-scoped</b> ({@code state.isInFolder}), so it must work at ANY
 *       depth, not just the first — and the deepest level is where a wrong parentId would
 *       show up as a file landing in the wrong folder.</li>
 *   <li><b>Cleanup.</b> Deleting the root folder must take the whole subtree with it —
 *       the app's own confirmation says "and everything inside it".</li>
 * </ul>
 *
 * <p>Builds L1 → L2 → L3, uploads every supported media family at the deepest level,
 * checks the breadcrumb and the way back at each step, then deletes L1 and verifies the
 * entire subtree is gone.
 */
@Order(TestOrder.DOC_DISTRIBUTION_NESTED)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistNestedFoldersTest extends DocDistBaseTest {

    private static final String STAMP = String.valueOf(System.currentTimeMillis() % 100000);
    private static final String L1 = "QA_NEST1_" + STAMP;
    private static final String L2 = "QA_NEST2_" + STAMP;
    private static final String L3 = "QA_NEST3_" + STAMP;

    /** One per supported family — PDF, image, Word, Excel, vCard (FileTypeDetector). */
    private static final String[] MEDIA =
            {"zl_dd_doc.pdf", "zl_dd_pic.jpg", "zl_dd_word.docx", "zl_dd_book.xlsx", "zl_dd_card.vcf"};

    private static boolean builtL1, builtL2, builtL3;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    /** Walks root → L1 → … as deep as asked, asserting the breadcrumb follows. */
    private DocDistLibraryPage descendTo(String... names) {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        for (String name : names) {
            assumeTrue(lib.waitForItem(name, Duration.ofSeconds(30)), "Missing folder: " + name);
            assumeTrue(lib.openItem(name), "Could not open folder: " + name);
            sleep(1200);
            assumeTrue(lib.isInFolder(), "Should be inside " + name);
        }
        return lib;
    }

    // ==================== building the tree ====================

    @Test
    @Order(1)
    @DisplayName("NEST-1: a folder is created at the ROOT")
    void createLevelOne() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(createFolder(L1), "Could not create the level-1 folder");
        assertTrue(lib.waitForItem(L1, Duration.ofSeconds(45)), L1 + " should be listed at the root");
        builtL1 = true;
        screenshot("ddn-l1");
    }

    @Test
    @Order(2)
    @DisplayName("NEST-2: Create-folder INSIDE a folder makes a CHILD, not another root folder")
    void createLevelTwoInsideLevelOne() {
        assumeTrue(builtL1, "Level 1 must exist");
        DocDistLibraryPage lib = descendTo(L1);

        assumeTrue(createFolder(L2), "Could not create a folder inside " + L1);
        assertTrue(lib.waitForItem(L2, Duration.ofSeconds(45)),
                L2 + " should be listed INSIDE " + L1);
        System.out.println(">>> NEST-2 inside " + L1 + ": " + lib.itemNames()
                + " crumb='" + lib.currentCrumb() + "'");
        builtL2 = true;
        screenshot("ddn-l2");

        // The child must NOT also appear at the root — that would mean parentId was lost.
        // Scroll the WHOLE root: itemNames() is only what happens to be on screen, and a
        // negative assertion over a partial list passes by luck rather than by evidence.
        assertTrue(lib.returnToRoot(), "Could not get back to the root");
        assertFalse(lib.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(L2)),
                L2 + " is a CHILD of " + L1 + " and must not appear at the root as well "
                        + "(that would mean the create sheet ignored parentId)");
    }

    @Test
    @Order(3)
    @DisplayName("NEST-3: a THIRD level nests under the second, and the breadcrumb names it")
    void createLevelThree() {
        assumeTrue(builtL2, "Level 2 must exist");
        DocDistLibraryPage lib = descendTo(L1, L2);

        assumeTrue(createFolder(L3), "Could not create a folder inside " + L2);
        assertTrue(lib.waitForItem(L3, Duration.ofSeconds(45)),
                L3 + " should be listed inside " + L2);
        builtL3 = true;

        assumeTrue(lib.openItem(L3), "Could not open " + L3);
        sleep(1200);
        String crumb = lib.currentCrumb();
        String header = lib.folderHeaderName();
        System.out.println(">>> NEST-3 at depth 3: crumb='" + crumb + "' header='" + header + "'");
        screenshot("ddn-l3");
        assertTrue(lib.isInFolder(), "Depth 3 must still count as being in a folder");
        // dd_crumb_current shows only the CURRENT folder, not a path — assert that, rather
        // than a full "L1 › L2 › L3" trail the app never renders.
        assertTrue(crumb != null && crumb.contains(L3),
                "The breadcrumb names the CURRENT folder at depth 3, got '" + crumb + "'");
        lib.returnToRoot();
    }

    // ==================== uploading at depth ====================

    @Test
    @Order(4)
    @DisplayName("NEST-4: every supported media family uploads at the DEEPEST level")
    void uploadEveryTypeAtDepth() {
        assumeTrue(builtL3, "The three-level tree must exist");
        DocDistLibraryPage lib = descendTo(L1, L2, L3);
        assumeTrue(lib.hasUpload(),
                "Upload must be offered at depth 3 too — it is gated on state.isInFolder, "
                        + "which does not care how deep we are");

        assertTrue(uploadFixturesIntoOpenFolder(MEDIA),
                "The multi-select upload should complete at depth 3");
        StringBuilder missing = new StringBuilder();
        for (String f : MEDIA) {
            boolean listed = lib.waitForItem(f, Duration.ofSeconds(60));
            System.out.println(">>> NEST-4 " + f + " listed=" + listed);
            if (!listed) missing.append(f).append(' ');
        }
        screenshot("ddn-uploads");
        assertTrue(missing.length() == 0,
                "Every supported family must upload at depth 3. Missing: " + missing);
        lib.returnToRoot();
    }

    @Test
    @Order(5)
    @DisplayName("NEST-5: the deep files stay in THEIR folder — the parent and root do not show them")
    void deepFilesDoNotLeak() {
        assumeTrue(builtL3, "The tree must exist");
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);

        // Root: only L1 (plus other suites' leftovers) — never the deep documents.
        assumeTrue(lib.returnToRoot(), "Not at the root");
        boolean atRoot = lib.allItemNamesScrolled().stream().anyMatch(n -> n.equals(MEDIA[0]));
        assertFalse(atRoot, MEDIA[0] + " belongs to " + L3 + " and must not appear at the root");

        // The MIDDLE folder holds L3 and nothing uploaded. Wait for L3 FIRST: the run
        // before this read the middle folder as empty (`inside QA_NEST2_436: []`) simply
        // because the list had not rendered yet, which made the "no documents here" check
        // below pass without inspecting anything. A negative assertion needs a list that
        // is known to have loaded, and L3's presence is exactly that proof.
        descendTo(L1, L2);
        assertTrue(lib.waitForItem(L3, Duration.ofSeconds(30)),
                L3 + " is a child of " + L2 + " and must be listed inside it");
        var mid = lib.allItemNamesScrolled();
        System.out.println(">>> NEST-5 inside " + L2 + ": " + mid);
        assertFalse(mid.stream().anyMatch(n -> n.equals(MEDIA[0])),
                MEDIA[0] + " was uploaded into " + L3 + ", so " + L2 + " must not list it — "
                        + "seeing it here would mean the upload used the wrong parent");
        lib.returnToRoot();
    }

    // ==================== getting back out ====================

    @Test
    @Order(6)
    @DisplayName("NEST-6: BACK unwinds the tree one level at a time (the home crumb cannot — trap 3.21)")
    void backUnwindsLevelByLevel() {
        assumeTrue(builtL3, "The tree must exist");
        DocDistLibraryPage lib = descendTo(L1, L2, L3);

        int presses = 0;
        for (int i = 0; i < 6 && lib.isInFolder(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { break; }
            sleep(900);
            presses++;
        }
        System.out.println(">>> NEST-6 BACK presses to leave depth 3 = " + presses
                + " atRoot=" + lib.isAtRoot());
        screenshot("ddn-unwound");
        assertTrue(lib.isAtRoot(), "BACK must eventually reach the library root from depth 3");
        assertTrue(lib.isDocDistActivity(),
                "Unwinding the tree must NOT leave the module — one BACK too many exits it, "
                        + "which is why every helper here stops at the root rather than counting");
    }

    // ==================== cleanup proves cascade delete ====================

    @Test
    @Order(7)
    @DisplayName("NEST-7 / CLEANUP: deleting the top folder removes the whole subtree")
    void deletingRootFolderRemovesSubtree() {
        assumeTrue(builtL1, "Nothing to clean up");
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Not at the root");

        assertTrue(deleteItem(L1),
                "Deleting " + L1 + " should confirm and remove it (the app's own wording is "
                        + "\"Delete this folder and everything inside it?\")");
        sleep(1500);
        assertFalse(lib.hasItem(L1), L1 + " should be gone from the root");

        // The children went with it: they were only reachable through L1, so the honest
        // check is that the parent is gone and nothing from the subtree surfaced at root.
        var root = lib.allItemNamesScrolled();
        System.out.println(">>> NEST-7 root after delete: " + root);
        assertFalse(root.stream().anyMatch(n -> n.equals(L2) || n.equals(L3)),
                "Deleting the parent must not orphan " + L2 + " / " + L3 + " up to the root");
        screenshot("ddn-deleted");
    }
}
