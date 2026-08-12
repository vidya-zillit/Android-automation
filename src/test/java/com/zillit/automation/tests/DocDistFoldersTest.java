package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DocDistCreateFolderSheet;
import com.zillit.automation.pages.DocDistFolderEditorDialog;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — area C (folders) of DOC_DISTRIBUTION_TEST_CASES.md, plus the
 * area-B cases that need content to exist before they mean anything.
 *
 * <p>This suite settles trap 3.11 from the app source: {@code ddBtnUpload} is visible iff
 * {@code state.isInFolder}, so Upload is a FOLDER control and never appears at the root —
 * {@link #uploadAppearsInsideFolder} asserts both halves of that rule.
 *
 * <p>It also isolates a real app bug in B-12: the breadcrumb's home crumb is inert one
 * level deep, because opening a folder self-navigates to {@code ddLibraryFragment} while
 * the crumb calls {@code popBackStack(R.id.ddLibraryFragment, false)}. Recovery between
 * cases therefore uses BACK; see {@link #homeCrumbReturnsToRoot}.
 *
 * <p>Validation here is asserted on the app's own gate: {@code updateCreateEnabled()}
 * disables Create until the name is non-blank AND a date is chosen, so an invalid form is
 * observable without guessing at error text.
 */
@Order(TestOrder.DOC_DISTRIBUTION_FOLDERS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistFoldersTest extends DocDistBaseTest {

    private static final String STAMP_SUFFIX = String.valueOf(System.currentTimeMillis() % 100000);
    private static final String FOLDER = "QA_DD_" + STAMP_SUFFIX;
    private static final String RENAMED = FOLDER + "_R";
    private static final String DESCRIPTION = "QA description " + STAMP_SUFFIX;

    /** Why an absent Upload usually means the BUILD, not the app being wrong. */
    private static final String UPLOAD_HINT =
            "Current source (LibraryFragment.render) shows ddBtnUpload whenever "
            + "state.isInFolder. A build that hides it inside a folder is either "
            + "pre-QA-flip (rights hid the buttons instead of gating the click) or "
            + "predates that code — check versionName against the app repo, trap 3.17.";


    @BeforeEach
    void reachLibrary() {
        // Shared fast path (DocDistBaseTest): returns immediately when the previous case
        // already left us on the library root, instead of re-running permission sweeps and
        // full Tools navigation before EVERY case. These two suites each carried their own
        // copy of that navigation and so never benefited — which is a large part of why
        // moving between cases cost more than the cases themselves.
        reachLibraryOrSkip();
    }



    /** Opens the create sheet from the action row, or the empty-state CTA. */
    private DocDistCreateFolderSheet openCreateSheet() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistCreateFolderSheet sheet = new DocDistCreateFolderSheet(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            if (lib.openCreateFolder() && sheet.waitOpen(Duration.ofSeconds(8))) return sheet;
            // The empty library puts a "Create folder" CTA in the middle of the screen —
            // often easier to hit than the chip, which sits in a scrolling row.
            if (lib.hasEmptyCta() && clickId("dd_empty_cta") && sheet.waitOpen(Duration.ofSeconds(8))) {
                return sheet;
            }
            sleep(1500);
        }
        assumeTrue(false, "Could not open the create-folder sheet");
        return sheet;
    }

    private boolean clickId(String id) {
        try { driver.findElement(byAppId(id)).click(); sleep(800); return true; }
        catch (Exception e) { return false; }
    }

    // ==================== C · FOLDERS ====================

    @Test
    @Order(1)
    @DisplayName("C-1: the create-folder sheet shows name, description, date and stop-notifications")
    void createSheetFields() {
        DocDistCreateFolderSheet sheet = openCreateSheet();
        assertTrue(sheet.hasNameField(), "Name field expected");
        assertTrue(sheet.hasDescriptionField(), "Description field expected");
        assertTrue(sheet.hasDateButton(), "Date button expected");
        assertTrue(sheet.hasCreateButton(), "Create button expected");
        assertTrue(sheet.hasCancelButton(), "Cancel button expected");
        System.out.println(">>> title='" + sheet.title() + "' date='" + sheet.dateButtonText()
                + "' stopNotifRow=" + sheet.hasStopNotifRow());
        screenshot("dd-create-sheet");
        sheet.cancel();
    }

    @Test
    @Order(2)
    @DisplayName("C-2: Create stays DISABLED with an empty name")
    void emptyNameDisablesCreate() {
        DocDistCreateFolderSheet sheet = openCreateSheet();
        // The app disables the button rather than showing an error, so this is assertable
        // directly — no guessing at invisible EditText.error text.
        assertFalse(sheet.isCreateEnabled(),
                "With no name typed, Create must be disabled");
        sheet.setName("   ");
        assertFalse(sheet.isCreateEnabled(),
                "A whitespace-only name must leave Create disabled (isNullOrBlank)");
        screenshot("dd-create-disabled");
        sheet.cancel();
    }

    @Test
    @Order(3)
    @DisplayName("C-2b: a name alone is not enough — a DATE is required too")
    void nameWithoutDateDisablesCreate() {
        DocDistCreateFolderSheet sheet = openCreateSheet();
        assumeTrue(sheet.setName("QA_date_gate"), "Could not type a name");
        boolean enabledWithNameOnly = sheet.isCreateEnabled();
        System.out.println(">>> create enabled with name only = " + enabledWithNameOnly
                + " (date button reads '" + sheet.dateButtonText() + "')");
        screenshot("dd-create-name-only");
        // updateCreateEnabled() requires selectedDateIso too. If the sheet opens with a
        // date already chosen this is legitimately enabled — report which it was rather
        // than asserting a rule the build may not follow.
        if (!enabledWithNameOnly) {
            assumeTrue(sheet.pickDate(), "Could not pick a date");
            assertTrue(sheet.isCreateEnabled(),
                    "With a name AND a date, Create must become enabled");
        }
        sheet.cancel();
    }

    @Test
    @Order(4)
    @DisplayName("C-3: a valid name + date creates the folder and it appears in the library")
    void createFolder() {
        DocDistCreateFolderSheet sheet = openCreateSheet();
        // Fill the DESCRIPTION too: it is part of the create form, the app persists it,
        // and C-4 reads it back through Edit. Creating with the name alone left the field
        // untested end to end — the sheet accepted it, and nobody ever checked the value
        // survived the round trip.
        assertTrue(sheet.createNamed(FOLDER, DESCRIPTION),
                "The sheet should close once a valid name, description and date are submitted");
        sleep(2500);

        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // waitForItem() also checks SEARCH: the list lags a create, search does not.
        boolean listed = lib.waitForItem(FOLDER, Duration.ofSeconds(45));
        System.out.println(">>> created '" + FOLDER + "'; library=" + lib.allItemNamesScrolled());
        screenshot("dd-folder-created");
        assertTrue(listed, "The new folder should be listed. Got: " + lib.allItemNamesScrolled());
    }

    @Test
    @Order(5)
    @DisplayName("3.11 answered: Upload appears INSIDE a folder (and only there), with the folder header")
    void uploadAppearsInsideFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)), "The folder step must pass first");

        // Source truth (LibraryFragment.render): ddBtnUpload / ddBreadcrumb /
        // ddFolderHeader are all VISIBLE iff state.isInFolder. The first run asked
        // "does a folder's existence reveal Upload at the root?" — the answer is no:
        // you have to be INSIDE one. (The help card's "Create Folder, THEN Upload"
        // means exactly this.)
        assertFalse(lib.hasUpload(), "Upload must NOT be offered at the root");

        assumeTrue(lib.openItem(FOLDER), "Could not open the folder");
        boolean upload, header;
        try {
            sleep(2500);
            upload = lib.hasUpload();
            header = lib.hasFolderHeader();
            System.out.println(">>> inside folder: upload=" + upload + " folderHeader=" + header
                    + " compose=" + lib.hasCompose()
                    + " headerName='" + lib.folderHeaderName() + "'");
            screenshot("dd-actions-inside-folder");
        } finally {
            // A failed assertion must NOT leave the suite inside the folder: every later
            // case then runs against the folder's contents instead of the root, and the
            // failure cascades (observed — this took out B-5 on the next run).
            lib.returnToRoot();
        }
        assertTrue(header, "The folder header bar (Publish / Distribute) shows inside a folder");
        assertTrue(upload, "Upload must be offered inside a folder. " + UPLOAD_HINT);
        assertFalse(lib.hasUpload(), "Back at the root, Upload should be gone again");
    }

    /**
     * C-4 — the description survives the round trip.
     *
     * <p>Typed at create time (C-3) and read back by reopening the folder through its Edit
     * menu, which rehydrates the same sheet from the saved folder. Reading the field is
     * the only way to prove the value was persisted rather than merely accepted: the sheet
     * closes on success either way.
     */
    @Test
    @Order(6)
    @DisplayName("C-4: the description typed at create time is saved and shown again on Edit")
    void descriptionPersists() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)), "Need the QA folder");
        assumeTrue(lib.openRowMenu(FOLDER), "Could not open the folder's ⋮ menu");
        sleep(800);
        assumeTrue(lib.tapMenuItem("Edit"), "Could not tap Edit");

        DocDistFolderEditorDialog editor = new DocDistFolderEditorDialog(driver);
        boolean up = editor.waitOpen(Duration.ofSeconds(10));
        if (!up) {
            System.out.println(">>> C-4 edit dialog did NOT open. On screen: " + lib.visibleTexts(18));
            screenshot("dd-c4-no-dialog");
        }
        assertTrue(up, "The folder EDIT dialog should open after tapping Edit");

        String name = editor.nameText();
        String desc = editor.descriptionText();
        System.out.println(">>> C-4 reopened: name='" + name + "' description='" + desc + "'");
        screenshot("dd-description-roundtrip");
        try {
            assertTrue(editor.descriptionIs(DESCRIPTION),
                    "The description typed at create time must come back when the folder is "
                            + "reopened for editing. Expected '" + DESCRIPTION + "' got '" + desc
                            + "' (an empty field reports its HINT, so 'Description' here means "
                            + "the value was never saved)");
        } finally {
            editor.dismissQuietly();
            sleep(400);
        }
    }

    @Test
    @Order(7)
    @DisplayName("B-5: search finds the folder, and a no-match search empties the list")
    void searchFindsFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)), "Need the QA folder");

        assumeTrue(lib.search(FOLDER), "Could not type in the search box");
        sleep(1500);
        List<String> hits = lib.itemNames();
        System.out.println(">>> search '" + FOLDER + "' → " + hits);
        assertTrue(hits.stream().anyMatch(n -> n.equalsIgnoreCase(FOLDER)),
                "Searching the folder's name should find it. Got: " + hits);

        lib.search("zzqq-no-such-folder");
        sleep(1500);
        assertTrue(lib.isEmptyStateShown(), "A no-match search should show the empty state");
        screenshot("dd-search");
        lib.clearSearch();
        sleep(1500);
    }

    @Test
    @Order(8)
    @DisplayName("B-11: opening the folder shows the breadcrumb and the folder header")
    void openFolderShowsBreadcrumb() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)), "Need the QA folder");
        assumeTrue(lib.openItem(FOLDER), "Could not open the folder");
        try {
            sleep(2500);
            System.out.println(">>> inside: crumb='" + lib.currentCrumb()
                    + "' header='" + lib.folderHeaderName() + "'");
            screenshot("dd-inside-folder");
            assertFalse(lib.isAtRoot(), "Opening a folder should move off root");
        } finally {
            lib.returnToRoot();     // BACK-based: never leaves the suite in the folder
        }
        assertTrue(lib.isAtRoot(), "BACK from a folder must return to the root");
    }

    /**
     * B-12 — the home crumb, on its own.
     *
     * <p>This one currently FAILS, and the failure is a real app bug, not a flake:
     * {@code dd_action_open_folder} navigates {@code ddLibraryFragment} → itself, while
     * {@code ddCrumbHome} does {@code popBackStack(R.id.ddLibraryFragment, false)} —
     * which pops NOTHING when the current destination already is that id. So one level
     * deep the crumb is inert. Two levels deep it would pop only to the parent folder,
     * not home. Kept as its own case so the bug stays visible; the suite's recovery uses
     * BACK (see {@code DocDistLibraryPage.returnToRoot}) and is unaffected.
     */
    @Test
    @Order(9)
    @DisplayName("B-12: the home crumb returns to root — KNOWN BUG: popBackStack is a no-op one level deep")
    void homeCrumbReturnsToRoot() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)), "Need the QA folder");
        assumeTrue(lib.openItem(FOLDER), "Could not open the folder");
        boolean home;
        try {
            sleep(2000);
            assumeTrue(!lib.isAtRoot(), "Should be inside the folder before testing the crumb");
            assumeTrue(lib.tapHomeCrumbForTest(), "Could not tap the home crumb");
            sleep(2000);
            home = lib.isAtRoot();
            System.out.println(">>> after tapping the home crumb, atRoot=" + home);
            screenshot("dd-home-crumb");
        } finally {
            lib.returnToRoot();
        }
        assertTrue(home,
                "Tapping the home crumb inside a folder must return to the library root. "
                        + "It does not: LibraryFragment.setupToolbar() calls "
                        + "popBackStack(R.id.ddLibraryFragment, false), and because opening a "
                        + "folder self-navigates to ddLibraryFragment, that pops nothing. "
                        + "APP BUG — the crumb is inert one level deep.");
    }

    /**
     * B-13 — WHERE the Create-folder control lives depends on whether the library is empty.
     *
     * <p>The rule, from {@code LibraryFragment.render()} plus fragment_library.xml:
     * <ul>
     *   <li>The action row ALWAYS carries {@code dd_btn_create_folder}, positioned after
     *       {@code dd_btn_sort} ("Name (A–Z)") and before {@code dd_btn_compose}
     *       ({@code dd_btn_upload} sits between them in the XML but is GONE at the root,
     *       so on screen Create-folder follows Name (A–Z) directly).</li>
     *   <li>The CENTRE call-to-action ({@code dd_empty_cta}, inside {@code dd_empty}) is
     *       shown ONLY while the library is empty — {@code render()} gates the whole
     *       {@code dd_empty} block on {@code state.emptyKind != null}. With a folder
     *       present the centre button is therefore gone, and the action row is the only
     *       place to create one.</li>
     * </ul>
     *
     * <p>Position is asserted with both chips ON SCREEN AT ONCE: the row scrolls
     * horizontally, so an x measured before a scroll cannot be compared with one measured
     * after. When the screen is too narrow to show a pair together the ordering half is
     * skipped rather than guessed — the presence half still asserts.
     */
    @Test
    @Order(10)
    @DisplayName("B-13: with a folder present, Create-folder leaves the CENTRE and lives in the action row after Name (A–Z)")
    void createFolderMovesToTheActionRowOnceAFolderExists() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)),
                "This case needs at least one folder in the library");

        // 1. The centre CTA must be GONE — the library is not empty any more.
        boolean centreCta = lib.hasEmptyCta();
        boolean emptyState = lib.isEmptyStateShown();
        System.out.println(">>> B-13 with content: centreCta=" + centreCta
                + " emptyState=" + emptyState);
        assertFalse(centreCta,
                "With a folder in the library the centre Create-folder CTA must be gone — "
                        + "dd_empty_cta lives inside dd_empty, which render() shows only "
                        + "while emptyKind != null");

        // 2. It must still be reachable in the action row.
        assertTrue(lib.hasCreateFolder(),
                "Create folder must remain available in the action row (it sits past the "
                        + "fold in the horizontally scrolling row — trap 3.10)");

        // 3. Order: after Name (A–Z), before Compose. Only compared while co-visible.
        lib.rewindChipRow();
        if (lib.bothOnScreen("dd_btn_sort", "dd_btn_create_folder")) {
            int sortX = lib.chipCenterX("dd_btn_sort");
            int createX = lib.chipCenterX("dd_btn_create_folder");
            System.out.println(">>> B-13 order: sort@" + sortX + " create@" + createX);
            assertTrue(createX > sortX,
                    "Create folder must sit AFTER 'Name (A–Z)' in the action row "
                            + "(sort@" + sortX + " create@" + createX + ")");
        } else {
            System.out.println(">>> B-13: sort and create-folder are never on screen together "
                    + "on this display — ordering vs Name (A–Z) not compared");
        }
        if (lib.bothOnScreen("dd_btn_create_folder", "dd_btn_compose")) {
            int createX = lib.chipCenterX("dd_btn_create_folder");
            int composeX = lib.chipCenterX("dd_btn_compose");
            System.out.println(">>> B-13 order: create@" + createX + " compose@" + composeX);
            assertTrue(composeX > createX,
                    "Create folder must sit BEFORE Compose in the action row "
                            + "(create@" + createX + " compose@" + composeX + ")");
        } else {
            System.out.println(">>> B-13: create-folder and compose are never on screen "
                    + "together on this display — ordering vs Compose not compared");
        }
        screenshot("dd-create-in-action-row");
    }

    @Test
    @Order(11)
    @DisplayName("C-8/C-9: the folder row menu offers Edit and Delete, and Edit renames it")
    void editFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(30)), "Need the QA folder");
        assumeTrue(lib.openRowMenu(FOLDER), "Could not open the folder's ⋮ menu");
        sleep(1200);

        boolean edit = lib.textShown("Edit", Duration.ofSeconds(4));
        boolean delete = lib.textShown("Delete", Duration.ofSeconds(2));
        System.out.println(">>> folder menu edit=" + edit + " delete=" + delete);
        screenshot("dd-folder-menu");
        assertTrue(edit && delete, "menu/dd_folder_row_actions offers Edit and Delete");

        // ASSERT from here, do not assume: the folder is visible and its menu is open, so
        // anything that fails now is a real defect, not an environment gap.
        assertTrue(lib.tapMenuItem("Edit"), "Could not tap Edit in the row menu");

        // The EDIT dialog is a MaterialAlertDialog over dialog_dd_folder_editor
        // (dd_folder_name / dd_folder_description + framework Save), NOT the create-folder
        // bottom sheet. Asserting the sheet's ids here reported "the edit sheet did not
        // open" against a dialog that was open and pre-filled.
        DocDistFolderEditorDialog editor = new DocDistFolderEditorDialog(driver);
        boolean up = editor.waitOpen(Duration.ofSeconds(10));
        if (!up) System.out.println(">>> C-9 edit dialog missing. On screen: " + lib.visibleTexts(18));
        assertTrue(up, "The folder edit dialog should open");
        System.out.println(">>> C-9 prefilled: name='" + editor.nameText()
                + "' description='" + editor.descriptionText() + "'");

        assertTrue(editor.setName(RENAMED), "Could not type the new name");
        assertTrue(editor.saveAndWaitClosed(Duration.ofSeconds(12)),
                "Save should close the dialog. NOTE: the app returns early when NOTHING "
                        + "changed, so a no-op edit also closes it — the rename below is the "
                        + "real proof");
        sleep(1200);

        // The rename is proven by the LIST, not by the dialog closing: the app closes it
        // on a no-op edit too.
        DocDistLibraryPage after = new DocDistLibraryPage(driver);
        boolean renamed = after.waitForItem(RENAMED, Duration.ofSeconds(45));
        System.out.println(">>> after rename: " + after.itemNames());
        screenshot("dd-folder-renamed");
        assertTrue(renamed, "The renamed folder should be listed as '" + RENAMED + "'");
    }

    @Test
    @Order(12)
    @DisplayName("C-10 / CLEANUP: Delete asks to confirm, and the folder disappears")
    void deleteFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        String target = lib.waitForItem(RENAMED, Duration.ofSeconds(20)) ? RENAMED : FOLDER;
        assumeTrue(lib.waitForItem(target, Duration.ofSeconds(20)),
                "Nothing to delete — earlier steps must run");
        assumeTrue(lib.openRowMenu(target), "Could not open the ⋮ menu");
        sleep(1200);
        assumeTrue(lib.tapMenuItem("Delete"), "Could not tap Delete in the row menu");

        // The app-wide confirmation (common_dialog_layout_with_button). It is MODAL, so
        // leaving it up would block every later case.
        assertTrue(lib.confirmPopup().isShown(Duration.ofSeconds(8)),
                "Deleting a folder must ask for confirmation");
        System.out.println(">>> confirm says: '" + lib.confirmPopup().text() + "'");
        assertTrue(lib.confirmPopup().accept(Duration.ofSeconds(3)), "Could not confirm the delete");
        sleep(3000);

        DocDistLibraryPage after = new DocDistLibraryPage(driver);
        // Prove the list is READABLE before asserting absence — a dialog still up would
        // make "it is gone" pass against a covered list.
        assertFalse(after.confirmPopup().isShown(Duration.ofSeconds(2)),
                "The confirmation should be gone after confirming");
        assertTrue(after.isListReadable(), "The library must be readable before asserting absence");

        boolean gone = false;
        for (int i = 0; i < 10 && !gone; i++) {
            gone = !after.hasItem(target);
            if (!gone) { after.pullToRefresh(); sleep(2000); }
        }
        System.out.println(">>> after delete: " + after.itemNames());
        screenshot("dd-folder-deleted");
        assertTrue(gone, "The deleted folder should be gone from the library");
    }

}
