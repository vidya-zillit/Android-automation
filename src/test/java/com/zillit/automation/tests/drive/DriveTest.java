package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.CreateFolderSheet;
import com.zillit.automation.pages.drive.DriveFolderPage;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveItemMenuPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE — Tools → ADMIN → Drive ({@code DriverMainActivity}).
 *
 * <p>Covers areas A (home shell), B (create folder) and F (folder navigation) of
 * DRIVE_TEST_CASES.md — the pure-UI layer everything else builds on. Uploads,
 * sharing, trash and file requests come later; they need real files and a second
 * account.
 *
 * <p>Two module-specific traps are baked into these tests:
 * <ul>
 *   <li>Validation errors use {@code EditText.error} and Toasts, which appear in
 *       NEITHER the page source NOR a uiautomator dump — so invalid input is asserted
 *       as "the sheet refused to close", never as an error string.</li>
 *   <li>Drive sits in the ADMIN group below the fold on the Tools list, so it is
 *       reached via the tools SEARCH box — and the search field then contains the word
 *       "Drive" itself, so the tool row must be matched on its row id, not on text.</li>
 * </ul>
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveTest extends BaseTest {

    private static final String FOLDER_A = "QA_Drive_A_" + (System.currentTimeMillis() % 100000);
    /** Stamped: a fixed name collides with copies left by earlier runs. */
    private static final String FOLDER_NESTED = "QA_Nested_" + (System.currentTimeMillis() % 100000);

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests()   { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachDrive() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects available");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                    "Never reached a project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        assumeTrue(openDrive(), "Could not open Drive from Tools");
    }

    /**
     * Gets back to Drive home from wherever the previous test left off. Presses BACK
     * only while still inside the app — backing out of a bottom-nav screen would exit
     * to the launcher and poison every later test.
     */
    private boolean openDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        // A previous test may have left the drawer or a bottom sheet open, which hides
        // the FAB and would skip this test with a misleading "FAB did not open".
        closeAnyOverlay();
        // isLoaded() also matches Trash/Search (shared recyclerView) — require HOME.
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        if (drive.isDriveActivity() && drive.returnHome()) return true;

        for (int i = 0; i < 6 && !onBottomNavScreen(); i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(800); } catch (Exception ignored) { }
        }

        ToolsPage tools = new ToolsPage(driver);
        tools.openToolsTab();
        if (!tools.isLoaded(Duration.ofSeconds(15))) return false;
        // Drive lives in the ADMIN group below the fold — filter with the search box
        // rather than scroll-hunting. openModule() matches the row TextView, not the
        // search field (whose text is now "Drive" too).
        tools.searchTools("Drive");
        sleep(1500);
        if (!tools.isModuleListed("Drive", Duration.ofSeconds(6))) return false;
        if (!tools.openModuleContaining("Drive")) return false;
        return drive.isLoaded(Duration.ofSeconds(20));
    }

    /** Dismisses a drawer / bottom sheet left open by an earlier test, if any. */
    private void closeAnyOverlay() {
        for (int i = 0; i < 3; i++) {
            boolean overlay = !driver.findElements(byAppId("navTrash")).isEmpty()      // drawer
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()      // FAB sheet
                    || !driver.findElements(byAppId("menuDelete")).isEmpty()            // row menu
                    || !driver.findElements(byAppId("etFolderName")).isEmpty();         // create sheet
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    // ==================== A · HOME SHELL ====================

    @Test
    @Order(1)
    @DisplayName("Drive opens from Tools → ADMIN → Drive (DriverMainActivity, not the legacy module)")
    void driveOpens() {
        DriveHomePage drive = new DriveHomePage(driver);
        assertTrue(drive.isLoaded(Duration.ofSeconds(20)), "Drive home should load");
        // Asserting the ACTIVITY, not just some widgets: the codebase still contains the
        // legacy zillit_drive module, and a routing regression would otherwise pass.
        assertTrue(drive.isDriveActivity(),
                "Tools → Drive must open drive.presentation.DriverMainActivity (the legacy "
                        + "zillit_drive module is still in the manifest)");
        screenshot("drive-home");
    }

    @Test
    @Order(2)
    @DisplayName("Home shows the section tabs (My Drive / Shared with me) and kind tabs (Folders / Files)")
    void homeTabs() {
        DriveHomePage drive = new DriveHomePage(driver);
        assertTrue(drive.hasSectionTabs(), "Section tabs should be present");
        assertTrue(drive.textShown("My Drive", Duration.ofSeconds(5)), "'My Drive' tab expected");
        assertTrue(drive.textShown("Shared with me", Duration.ofSeconds(3)), "'Shared with me' tab expected");
        assertTrue(drive.hasKindTabs(), "Kind tabs should be present");
        System.out.println(">>> tabs OK; kind tabs visible=" + drive.hasKindTabs());
        screenshot("drive-tabs");
    }

    @Test
    @Order(3)
    @DisplayName("Home shows the Sort / Filter / Select chips and the add FAB")
    void homeChips() {
        DriveHomePage drive = new DriveHomePage(driver);
        assertTrue(drive.hasSortChip(), "Sort chip expected");
        assertTrue(drive.hasFilterChip(), "Filter chip expected");
        assertTrue(drive.hasSelectChip(), "Select chip expected");
        assertTrue(drive.hasFab(), "Add FAB expected");
        System.out.println(">>> viewToggle=" + drive.hasViewToggle()
                + " favToggle=" + drive.hasFavToggle()
                + " showFilter=" + drive.hasShowFilter()
                + " storageBar=" + drive.hasStorageBar());
        screenshot("drive-chips");
    }

    @Test
    @Order(4)
    @DisplayName("An empty Drive shows its empty state, otherwise the list renders items")
    void emptyOrList() {
        DriveHomePage drive = new DriveHomePage(driver);
        boolean empty = drive.isEmptyStateShown();
        int items = drive.itemCount();
        System.out.println(">>> empty=" + empty + " renderedItems=" + items
                + " title='" + drive.emptyTitle() + "' subtitle='" + drive.emptySubtitle() + "'");
        assertTrue(empty || items > 0,
                "Drive must show EITHER the empty state OR at least one item — neither was found");
        screenshot("drive-list-state");
    }

    @Test
    @Order(5)
    @DisplayName("The add FAB offers Create Folder / Upload File / Upload Folder")
    void fabActions() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab(), "FAB did not open");
        assertTrue(drive.fabHasCreateFolder(), "'Create Folder' expected");
        assertTrue(drive.fabHasUploadFile(), "'Upload File' expected");
        assertTrue(drive.fabHasUploadFolder(), "'Upload Folder' expected");
        // Request Files / View File Requests are FOLDER-scoped — they must NOT be here.
        assertFalse(drive.fabHasRequestFiles(),
                "'Request Files' is a folder-level action and should not appear at Drive root");
        screenshot("drive-fab");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(6)
    @DisplayName("Sort offers Name / Date modified / Size / Type")
    void sortOptions() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openSort(), "Sort sheet did not open");
        for (String opt : new String[]{"Name", "Date modified", "Size", "Type"}) {
            assertTrue(drive.textShown(opt, Duration.ofSeconds(3)), "Sort option missing: " + opt);
        }
        screenshot("drive-sort");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(7)
    @DisplayName("Filter opens the tag filter with a Cancel action")
    void filterSheet() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFilter(), "Filter sheet did not open");
        boolean tag = drive.textShown("Filter by Tag", Duration.ofSeconds(4));
        System.out.println(">>> filter-by-tag shown=" + tag);
        assertTrue(tag || drive.textShown("All", Duration.ofSeconds(2)),
                "The filter sheet should offer tag filtering");
        screenshot("drive-filter");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(8)
    @DisplayName("Select mode shows the bulk bar (0 selected + Download/Move/Delete/Cancel), Cancel exits")
    void selectMode() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.enterSelectMode(), "Select mode did not start");
        String count = drive.selectedCount();
        System.out.println(">>> selectedCount='" + count + "'");
        assertTrue(count != null && count.toLowerCase().contains("0"),
                "Bulk bar should start at 0 selected, was: " + count);
        assertTrue(drive.textShown("DELETE", Duration.ofSeconds(3))
                        || drive.textShown("Delete", Duration.ofSeconds(2)),
                "Bulk Delete expected");
        screenshot("drive-select-mode");
        assertTrue(drive.bulkCancel(), "Cancel should be tappable");
        sleep(1200);
        assertFalse(drive.isSelectModeOn(), "Cancel should leave select mode");
    }

    @Test
    @Order(9)
    @DisplayName("The Drive drawer lists Search / Trash / Activity (Uploads + Favorites are hidden in this build)")
    void drawerNav() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        DriveItemMenuPage nav = new DriveItemMenuPage(driver);
        try {
            assertTrue(nav.drawerHasProfile(), "The profile row is expected at the top of the drawer");
            assertTrue(nav.drawerHasSearch(), "Search expected in the drawer");
            assertTrue(nav.drawerHasTrash(), "Trash expected");
            assertTrue(nav.drawerHasActivity(), "Activity expected");

            // Uploads and Favorites are android:visibility="gone" in
            // fragment_drive_drawer.xml and nothing makes them visible at runtime —
            // the fragments and nav destinations exist, but the entry points are not
            // shipped yet. Assert they are STILL hidden, so this test starts passing
            // for the right reason the day they are switched on (it will fail, and the
            // Favorites/Uploads suites can then be written).
            assertFalse(nav.drawerHasUploads(),
                    "Uploads is hidden (visibility=gone) in this build — if it is now visible, "
                            + "the feature shipped: enable the Uploads-queue cases (area L).");
            assertFalse(nav.drawerHasFavorites(),
                    "Favorites is hidden (visibility=gone) in this build — if it is now visible, "
                            + "the feature shipped: enable the Favorites cases (area J).");
            screenshot("drive-drawer");
        } finally {
            // Always close the drawer: leaving it open hides the FAB and skips the next test.
            try { driver.navigate().back(); Thread.sleep(1000); } catch (Exception ignored) { }
        }
    }

    // ==================== B · CREATE FOLDER ====================

    @Test
    @Order(10)
    @DisplayName("Create Folder sheet shows name, description + counter, Location and Access Control")
    void createFolderSheetFields() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Create Folder sheet did not open");
        assertTrue(sheet.hasNameField(), "Folder name field expected");
        assertTrue(sheet.hasDescriptionField(), "Description field expected");
        assertTrue(sheet.hasCharCount(), "Character counter expected");
        assertTrue(sheet.hasLocationPicker(), "Location picker expected");
        assertTrue(sheet.hasAccessControl(), "Access control expected");
        assertTrue(sheet.hasCreateButton(), "Create button expected");
        System.out.println(">>> charCount='" + sheet.charCount() + "'");
        screenshot("drive-create-folder-sheet");
        sheet.cancel();
    }

    @Test
    @Order(11)
    @DisplayName("Create with an EMPTY name is rejected — the sheet stays open and no folder is created")
    void emptyNameRejected() {
        DriveHomePage drive = new DriveHomePage(driver);
        int before = drive.allItemNamesScrolled().size();

        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open");

        sheet.create();
        sleep(1500);

        // The app sets etName.error — an EditText error popup that is invisible to both
        // getPageSource and uiautomator dump. So the ONLY reliable signal is that the
        // sheet refused to close and nothing was created.
        assertTrue(sheet.isOpen(),
                "An empty folder name must be rejected — the sheet should stay open");
        screenshot("drive-empty-name-rejected");
        sheet.cancel();
        sleep(1500);

        int after = new DriveHomePage(driver).allItemNamesScrolled().size();
        assertEquals(before, after, "No folder should have been created from an empty name");
    }

    @Test
    @Order(12)
    @DisplayName("Whitespace-only folder name is rejected too")
    void whitespaceNameRejected() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open");

        sheet.setName("   ");
        sheet.create();
        sleep(1500);
        assertTrue(sheet.isOpen(), "A whitespace-only name must be rejected");
        screenshot("drive-whitespace-name");
        sheet.cancel();
    }

    @Test
    @Order(13)
    @DisplayName("Description counter tracks typed characters (N / 500)")
    void descriptionCounter() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open");

        String before = sheet.charCount();
        assumeTrue(sheet.setDescription("hello drive"), "Could not type a description");
        sleep(800);
        String after = sheet.charCount();
        System.out.println(">>> counter '" + before + "' -> '" + after + "'");
        assertTrue(after.contains("11") || !after.equals(before),
                "The character counter should react to typing (was '" + before + "', now '" + after + "')");
        screenshot("drive-desc-counter");
        sheet.cancel();
    }

    @Test
    @Order(14)
    @DisplayName("Access Control offers Project Users and Specific Users, with an affected-user count")
    void accessControlOptions() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open");

        assertTrue(sheet.textShown("Project Users", Duration.ofSeconds(4)), "'Project Users' expected");
        assertTrue(sheet.textShown("Specific Users", Duration.ofSeconds(2)), "'Specific Users' expected");
        sheet.chooseProjectUsers();
        sleep(700);
        System.out.println(">>> project users count='" + sheet.projectUsersCount() + "'");
        assertTrue(sheet.projectUsersCountShown() || sheet.hasPermissionSpinner(),
                "Project Users mode should show who it applies to / a permission spinner");

        sheet.chooseSpecificUsers();
        sleep(1200);
        System.out.println(">>> member search shown=" + sheet.memberSearchShown());
        screenshot("drive-access-control");
        sheet.cancel();
    }

    @Test
    @Order(15)
    @DisplayName("Location defaults to Drive root and offers 'Inside a folder'")
    void locationPicker() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open");

        assertTrue(sheet.textShown("Drive root", Duration.ofSeconds(4)), "'Drive root' option expected");
        assertTrue(sheet.textShown("Inside a folder", Duration.ofSeconds(2)), "'Inside a folder' option expected");
        screenshot("drive-location-picker");
        sheet.cancel();
    }

    @Test
    @Order(16)
    @DisplayName("CREATE: a folder with a valid name is created and appears in the list")
    void createFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open");

        assertTrue(sheet.createNamed(FOLDER_A),
                "The sheet should close once a valid name is submitted");
        sleep(2500);

        DriveHomePage home = new DriveHomePage(driver);
        List<String> names = home.allItemNamesScrolled();
        System.out.println(">>> created '" + FOLDER_A + "'; list=" + names);
        assertTrue(names.stream().anyMatch(n -> n.equalsIgnoreCase(FOLDER_A)),
                "The new folder should appear in the list. Got: " + names);
        screenshot("drive-folder-created");
    }

    // ==================== E/F · ROW MENU + FOLDER NAVIGATION ====================

    @Test
    @Order(17)
    @DisplayName("Row overflow offers Favorite / Share / Move to… / Edit Info / Details / Delete")
    void rowMenu() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing — create step must run first");
        assumeTrue(drive.openItemMenu(FOLDER_A), "Could not open the row overflow");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen(), "Row menu did not open");
        assertTrue(menu.hasFavorite(), "Favorite entry expected");
        assertTrue(menu.hasShare(), "Share entry expected");
        assertTrue(menu.hasMove(), "Move to… entry expected");
        assertTrue(menu.hasEditInfo(), "Edit Info entry expected");
        assertTrue(menu.hasDetails(), "Details entry expected");
        assertTrue(menu.hasDelete(), "Delete entry expected");
        System.out.println(">>> favourite label='" + menu.favoriteLabel() + "'");
        screenshot("drive-row-menu");
        menu.dismiss();
    }

    @Test
    @Order(18)
    @DisplayName("Details shows the folder's name, dates, owner, access and activity")
    void folderDetails() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing");
        assumeTrue(drive.openItemMenu(FOLDER_A), "Could not open the row overflow");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapDetails(), "Could not tap Details");
        assumeTrue(menu.detailsOpen(), "Details sheet did not open");

        System.out.println(">>> details name='" + menu.detailsFolderName()
                + "' created=" + menu.detailsHasCreatedOn()
                + " owner=" + menu.detailsHasOwner()
                + " access=" + menu.detailsHasAccess()
                + " activity=" + menu.detailsHasActivity());
        assertTrue(menu.detailsFolderName().equalsIgnoreCase(FOLDER_A),
                "Details should name the folder, got '" + menu.detailsFolderName() + "'");
        assertTrue(menu.detailsHasCreatedOn(), "A created-on date is expected");
        screenshot("drive-folder-details");
        menu.dismiss();
    }

    @Test
    @Order(19)
    @DisplayName("Opening a folder pushes FolderActivity with the folder name in the header")
    void openFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing");
        assumeTrue(drive.openItem(FOLDER_A), "Could not open the folder");
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(driver);
        assertTrue(folder.isFolderActivity(), "Opening a folder should push FolderActivity");
        // The header is UPPER-CASED for display, so compare case-insensitively.
        System.out.println(">>> header='" + folder.header() + "' expected~" + FOLDER_A);
        assertTrue(folder.headerMatches(FOLDER_A),
                "Header should be the folder name (case-insensitive): '" + folder.header() + "'");
        screenshot("drive-folder-open");
    }

    @Test
    @Order(20)
    @DisplayName("A new folder is empty and shows the folder-specific empty state")
    void folderEmptyState() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing");
        assumeTrue(drive.openItem(FOLDER_A), "Could not open the folder");
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(driver);
        assumeTrue(folder.isLoaded(Duration.ofSeconds(15)), "Folder screen did not load");
        System.out.println(">>> empty='" + folder.emptyTitle() + "' / '" + folder.emptySubtitle() + "'");
        assertTrue(folder.isEmptyStateShown(), "A freshly created folder should be empty");
        screenshot("drive-folder-empty");
    }

    @Test
    @Order(21)
    @DisplayName("The FAB inside a folder ADDS Request Files and View File Requests")
    void folderFabExtras() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing");
        assumeTrue(drive.openItem(FOLDER_A), "Could not open the folder");
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(driver);
        assumeTrue(folder.isLoaded(Duration.ofSeconds(15)), "Folder screen did not load");
        assumeTrue(folder.openFab(), "Folder FAB did not open");

        assertTrue(folder.fabHasCreateFolder(), "Create Folder expected");
        assertTrue(folder.fabHasUploadFile(), "Upload File expected");
        assertTrue(folder.fabHasUploadFolder(), "Upload Folder expected");
        // These two are the difference from the root FAB — the whole point of this case.
        assertTrue(folder.fabHasRequestFiles(),
                "'Request Files' should be offered INSIDE a folder");
        assertTrue(folder.fabHasViewFileRequests(),
                "'View File Requests' should be offered INSIDE a folder");
        screenshot("drive-folder-fab");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(22)
    @DisplayName("A nested folder can be created inside a folder")
    void createNestedFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing");
        assumeTrue(drive.openItem(FOLDER_A), "Could not open the folder");
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(driver);
        assumeTrue(folder.isLoaded(Duration.ofSeconds(15)), "Folder screen did not load");
        assumeTrue(folder.openFab() && folder.tapCreateFolder(), "Could not start Create Folder");

        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet did not open inside the folder");
        assertTrue(sheet.createNamed(FOLDER_NESTED), "Nested folder should be created");

        // Poll rather than sleep-then-peek: creation is a server round-trip plus a list
        // reload, and the row may also land below the fold. A single rendered-rows read
        // after a fixed 2.5s misses it on both counts.
        DriveFolderPage inFolder = new DriveFolderPage(driver);
        boolean found = inFolder.waitForItem(FOLDER_NESTED, Duration.ofSeconds(30));
        List<String> inside = inFolder.allItemNamesScrolled();
        System.out.println(">>> inside " + FOLDER_A + ": " + inside);
        assertTrue(found,
                "The nested folder should be listed inside its parent. Got: " + inside);
        screenshot("drive-nested-folder");
    }

    @Test
    @Order(23)
    @DisplayName("BACK from a folder returns to the Drive root list")
    void backFromFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing");
        assumeTrue(drive.openItem(FOLDER_A), "Could not open the folder");
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(driver);
        assumeTrue(folder.isFolderActivity(), "Not inside FolderActivity");
        folder.back();
        sleep(2000);

        DriveHomePage home = new DriveHomePage(driver);
        assertTrue(home.isDriveActivity(),
                "BACK from a folder should return to Drive home, not leave the module");
        assertTrue(home.hasItem(FOLDER_A), "The parent folder should still be listed");
        screenshot("drive-back-to-root");
    }

    @Test
    @Order(24)
    @DisplayName("CLEANUP: delete the QA folder (confirm) → it disappears from the list")
    void deleteFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasItem(FOLDER_A), "Test folder missing — nothing to clean up");
        assumeTrue(drive.openItemMenu(FOLDER_A), "Could not open the row overflow");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapDelete(), "Could not tap Delete");
        sleep(1500);

        // Drive's confirm dialog uses "Delete"/"Cancel", NOT the app-wide "Yes"/"No" —
        // confirmYesDialog() does not dismiss it.
        DriveHomePage home = new DriveHomePage(driver);
        assertTrue(home.confirmDeleteDialog(Duration.ofSeconds(10)),
                "The delete confirmation dialog should be confirmable via its Delete button");
        sleep(3500);

        // A dialog left on screen covers the list, so tvName matches nothing and an
        // "is absent" assertion would pass against an EMPTY list while the folder is
        // still there. Prove the list is readable BEFORE asserting absence.
        assertFalse(home.isDialogOpen(), "The confirm dialog should be gone after confirming");
        assertTrue(home.isListReadable(),
                "The list must be readable before asserting the folder is gone — otherwise "
                        + "an empty/covered list makes this assertion pass vacuously");

        List<String> after = home.allItemNamesScrolled();
        System.out.println(">>> after delete: " + after);
        assertFalse(after.stream().anyMatch(n -> n.equalsIgnoreCase(FOLDER_A)),
                "The deleted folder should be gone from the list. Got: " + after);
        screenshot("drive-folder-deleted");
    }

    // =====================================================================================
    // WEB GAP cases (web-e2e/gaps/drive.md M13 create-folder rules, M21 details, M26 nesting,
    // M33 back to Tools / description cap). Every folder is QA_… and purged at the end.
    // =====================================================================================

    private static final long GS = System.currentTimeMillis() % 100000;
    private static final String G_DA = "QA_DA_" + GS;
    private static final String G_DB = "QA_DB_" + GS;
    private static final String G_IN = "QA_In_" + GS;
    private static final java.util.Set<String> G_FOLDERS = new java.util.LinkedHashSet<>();

    private com.zillit.automation.pages.drive.DriveFlows gf() { return new com.zillit.automation.pages.drive.DriveFlows(driver); }

    private void gFolder(String name) {
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        if (!home.hasItem(name)) assumeTrue(gf().createFolder(name), "Could not create " + name);
        G_FOLDERS.add(name);
    }

    private CreateFolderSheet openCreateSheet() {
        DriveHomePage home = new DriveHomePage(driver);
        assumeTrue(home.openFab() && home.tapCreateFolder(), "Create Folder not offered");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Create Folder sheet did not open");
        return sheet;
    }

    private boolean pickDestination(String folder) {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.xpath("//*[@resource-id='" + driver.getCurrentPackage()
                    + ":id/drive_pick_folder_name' and @text='" + folder + "']")).click();
            sleep(1200);
            return true;
        } catch (Exception e) { return false; }
    }

    @Test
    @Order(40)
    @DisplayName("[WEB L137/L289] CREATE FOLDER: 'Inside a folder' with nothing picked is refused; picking a folder creates it there")
    void gCreateInsidePicked() {
        gFolder(G_DA);
        CreateFolderSheet sheet = openCreateSheet();
        sheet.setName(G_IN);
        assertTrue(sheet.chooseInsideFolder(), "'Inside a folder' option");
        sleep(1500);
        sheet.create();
        sleep(2500);
        boolean stillOpen = sheet.isOpen();
        assertTrue(stillOpen, "With no destination picked, Create must not go through");
        assertTrue(pickDestination(G_DA), G_DA + " should be offered as a destination");
        assertTrue(sheet.create(), "Create");
        sleep(4000);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertFalse(home.hasItem(G_IN), G_IN + " must not land at the root");
        assertTrue(gf().openFolder(G_DA), "Open " + G_DA);
        List<String> inside = new DriveFolderPage(driver).allItemNamesScrolled();
        gf().backToHome();
        assertTrue(inside.contains(G_IN), G_IN + " should be inside " + G_DA + ": " + inside);
    }

    @Test
    @Order(41)
    @DisplayName("[WEB L295] CREATE FOLDER: a second folder with the same name at the root is refused")
    void gCreateDuplicate() {
        gFolder(G_DA);
        CreateFolderSheet sheet = openCreateSheet();
        boolean closed = sheet.createNamed(G_DA);
        // On a duplicate the app closes the sheet, waits for the server's refusal and then
        // REOPENS it with tvFolderNameError (HomeViewModel.createFolderDuplicate) — poll for it.
        boolean err = false;
        for (int i = 0; i < 24 && !err; i++) {
            err = !driver.findElements(byAppId("tvFolderNameError")).isEmpty();
            if (!err) sleep(500);
        }
        if (sheet.isOpen()) sheet.close();
        sleep(1500);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        long copies = home.allItemNamesScrolled().stream().filter(G_DA::equals).count();
        System.out.println(">>> duplicate create closed=" + closed + " inlineError=" + err + " copies=" + copies);
        assertEquals(1, copies, "Only one folder may carry the name " + G_DA);
        assertTrue(err || !closed, "The duplicate should be refused in the sheet (tvFolderNameError)");
    }

    @Test
    @Order(42)
    @DisplayName("[WEB L1166] CREATE FOLDER: the same name is accepted inside ANOTHER folder")
    void gCreateSameNameElsewhere() {
        gFolder(G_DA);
        gFolder(G_DB);
        assumeTrue(gf().openFolder(G_DB), "Open " + G_DB);
        boolean ok = gf().createFolder(G_DA);
        List<String> inside = new DriveFolderPage(driver).allItemNamesScrolled();
        gf().backToHome();
        assertTrue(ok && inside.contains(G_DA), "The same name should be allowed in another folder: " + inside);
    }

    @Test
    @Order(43)
    @DisplayName("[WEB L1135] CREATE FOLDER: a 100-character name is accepted; 101 is refused")
    void gCreateLongNames() {
        String base = "QA_L" + GS + "_";
        String n100 = base + "x".repeat(100 - base.length());
        String n101 = base + "y".repeat(101 - base.length());
        DriveHomePage home = new DriveHomePage(driver);
        CreateFolderSheet s1 = openCreateSheet();
        s1.createNamed(n100);
        if (s1.isOpen()) s1.close();
        CreateFolderSheet s2 = openCreateSheet();
        s2.createNamed(n101);
        boolean refusedInSheet = s2.isOpen();
        if (s2.isOpen()) s2.close();
        home.showFolders();
        List<String> all = home.allItemNamesScrolled();
        if (all.contains(n100)) G_FOLDERS.add(n100);
        if (all.contains(n101)) G_FOLDERS.add(n101);
        System.out.println(">>> 100 listed=" + all.contains(n100) + " 101 listed=" + all.contains(n101) + " 101 sheetOpen=" + refusedInSheet);
        assertTrue(all.contains(n100), "A 100-character name must be accepted");
        assertFalse(all.contains(n101), "A 101-character name should be refused (web ZL-21806; Android has no client cap)");
    }

    @Test
    @Order(44)
    @DisplayName("[WEB L902] CREATE FOLDER: the description stops at 500 characters and the counter says 500 / 500")
    void gDescriptionCap() {
        CreateFolderSheet sheet = openCreateSheet();
        sheet.setDescription("d".repeat(510));
        sleep(800);
        String count = sheet.charCount();
        String text = "";
        try { text = driver.findElement(byAppId("etDescription")).getText(); } catch (Exception ignored) { }
        sheet.close();
        System.out.println(">>> description len=" + text.length() + " counter='" + count + "'");
        assertEquals(500, text.length(), "The description must stop at 500 characters");
        assertTrue(count.replace(" ", "").startsWith("500/500"), "The counter should read 500 / 500, was '" + count + "'");
    }

    @Test
    @Order(45)
    @DisplayName("[WEB DRIVE-01] ROW: a folder with two subfolders says so in its subtitle, never '0 files, 0 folders'")
    void gRowSubtitleCounts() {
        gFolder(G_DB);
        assumeTrue(gf().openFolder(G_DB), "Open " + G_DB);
        List<String> inside = new DriveFolderPage(driver).allItemNamesScrolled();
        if (!inside.contains("s1")) gf().createFolder("s1");
        if (!inside.contains("s2")) gf().createFolder("s2");
        inside = new DriveFolderPage(driver).allItemNamesScrolled();
        assumeTrue(inside.contains("s1") && inside.contains("s2"), "Subfolders s1/s2 were not created: " + inside);
        gf().backToHome();
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        home.pullToRefresh();
        sleep(3000);
        home.hasItem(G_DB);
        String sub = gf().rowSubtitle(G_DB);
        System.out.println(">>> subtitle of " + G_DB + " = '" + sub + "'");
        assertTrue(sub != null && sub.contains("folder"), "The row should count its subfolders: '" + sub + "'");
        assertFalse(sub.contains("0 files, 0 folders"), "DRIVE-01: never '0 files, 0 folders'");
    }

    @Test
    @Order(46)
    @DisplayName("[WEB L457] DETAILS: a favourited folder's Details show Favorited, owner, access, tags and activity")
    void gFolderDetailsContent() {
        gFolder(G_DA);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        gf().tapRowStar(G_DA);
        assumeTrue(home.openItemMenu(G_DA), "Row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapDetails() && menu.detailsOpen(), "Details sheet");
        boolean pill = menu.detailsFavoritePill();
        String owner = menu.detailsOwner();
        // The sheet fills its metadata after it opens; give the owner line a moment.
        for (int i = 0; i < 16 && owner.isBlank(); i++) { sleep(500); owner = menu.detailsOwner(); }
        String access = menu.detailsAccessHeader();
        boolean tags = menu.detailsTagsShown();
        boolean activity = menu.detailsActivityShown();
        System.out.println(">>> details pill=" + pill + " owner='" + owner + "' access='" + access + "' tags=" + tags + " activity=" + activity);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        gf().tapRowStar(G_DA);
        assertTrue(pill, "A favourited folder should show its favourite pill");
        assertFalse(owner.isBlank(), "Owner should be shown");
        assertTrue(access.startsWith("Access"), "Access header expected, was '" + access + "'");
        assertTrue(tags, "Tags (or 'No tags') should be shown");
        assertTrue(activity, "Activity (or 'No activity yet') should be shown");
    }

    @Test
    @Order(47)
    @DisplayName("[WEB L283] NESTED: inside a folder the Create Folder sheet has no Location picker")
    void gNoLocationInsideFolder() {
        gFolder(G_DA);
        assumeTrue(gf().openFolder(G_DA), "Open " + G_DA);
        DriveFolderPage in = new DriveFolderPage(driver);
        assumeTrue(in.openFab() && in.tapCreateFolder(), "Create Folder inside");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet");
        boolean picker = !driver.findElements(byAppId("drive_pick_root")).isEmpty()
                && driver.findElement(byAppId("drive_pick_root")).isDisplayed();
        sheet.close();
        gf().backToHome();
        assertFalse(picker, "Inside a folder the new folder always lands there — no Location picker");
    }

    @Test
    @Order(48)
    @DisplayName("[WEB L62] BACK from Drive home leaves Drive for the Tools list")
    void gBackLeavesDrive() {
        DriveHomePage home = new DriveHomePage(driver);
        assumeTrue(home.isHome(Duration.ofSeconds(5)), "Not on Drive home");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(2500);
        boolean left = !home.isDriveActivity();
        boolean tools = new ToolsPage(driver).isLoaded(Duration.ofSeconds(10));
        assertTrue(left, "Back from Drive home should leave DriverMainActivity");
        assertTrue(tools, "…and land on the Tools list");
    }

    @Test
    @Order(97)
    @DisplayName("WEB GAP CLEANUP: remove the QA_ folders these cases created")
    void gDriveCleanup() {
        List<String> failed = new java.util.ArrayList<>();
        for (String f : G_FOLDERS) if (!gf().purge(f, true)) failed.add(f);
        assertTrue(failed.isEmpty(), "Could not remove: " + failed);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
