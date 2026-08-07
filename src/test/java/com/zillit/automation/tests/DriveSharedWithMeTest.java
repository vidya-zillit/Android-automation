package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
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
 * DRIVE — the RECIPIENT side of sharing. This is the only suite that proves a share
 * actually works: everything in {@link DriveShareTest} runs as the owner and can only
 * show that a grant was *saved*, not that the other user received it.
 *
 * <p><b>Run this on the SECOND device</b>, signed in as the user
 * {@code DriveShareTest} shared with:
 *
 * <pre>
 *   ZILLIT_UDID=&lt;second-device&gt; ./run-and-report.sh --drive-shared
 * </pre>
 *
 * <p>Both devices must be in the SAME project, as two DIFFERENT users. Run
 * {@code --drive-share} on device A first; this suite then verifies device B sees it.
 *
 * <p>Rights granted by {@code DriveShareTest}: the file gets <b>View + Download but NOT
 * Edit</b>, the folder gets all three. The point of the permission cases below is that
 * a view-only recipient must NOT be offered Share / Move / Edit Info / Delete — verified
 * live during mapping, where the owner's row menu had 6 entries and the recipient's had
 * exactly 2 (Add to favorites, Details).
 */
@Order(TestOrder.DRIVE_SHARED_WITH_ME)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveSharedWithMeTest extends BaseTest {

    /** The file the owner shared. Override with {@code ZILLIT_SHARED_FILE}. */
    private static final String SHARED_FILE =
            System.getenv().getOrDefault("ZILLIT_SHARED_FILE", "zl_drive_book.xlsx");

    /**
     * What the OWNER actually granted: {@code "view"} (view+download) or {@code "edit"}.
     *
     * <p>The menu a recipient sees is a FUNCTION of their rights, so the expectation has
     * to follow the grant. Hard-coding the viewer matrix made this suite fail the moment
     * the file was re-shared with edit access — the app was right and the test was stale.
     * Verified both ways on a real second device: with view+download the recipient's row
     * menu held exactly two entries (Add to favorites, Details) against the owner's six;
     * with edit it gained Share, Move and Edit Info — and never Delete.
     */
    private static final String GRANTED_RIGHTS =
            System.getenv().getOrDefault("ZILLIT_SHARED_RIGHTS", "edit");

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests()   { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachDrive() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        assumeTrue(openDrive(), "Could not open Drive");
    }

    private boolean openDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        closeAnyOverlay();
        // isLoaded() also matches Trash/Search (shared recyclerView) — require HOME.
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        if (drive.isDriveActivity() && drive.returnHome()) return true;
        for (int i = 0; i < 8 && !onBottomNavScreen(); i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(800); } catch (Exception ignored) { }
        }
        ToolsPage tools = new ToolsPage(driver);
        tools.openToolsTab();
        if (!tools.isLoaded(Duration.ofSeconds(15))) return false;
        tools.searchTools("Drive");
        sleep(1500);
        return tools.isModuleListed("Drive", Duration.ofSeconds(6))
                && tools.openModule("Drive")
                && drive.isLoaded(Duration.ofSeconds(20));
    }

    private void closeAnyOverlay() {
        for (int i = 0; i < 5; i++) {
            // A stranded SYSTEM PICKER (a foreign package) is the worst case: every
            // later test's openDrive() fails and the whole suite skips.
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean overlay = foreign || !driver.findElements(byAppId("menuDelete")).isEmpty()
                    || !driver.findElements(byAppId("menuDetails")).isEmpty()
                    || !driver.findElements(byAppId("navTrash")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    // ==================== Shared with me ====================

    @Test
    @Order(1)
    @DisplayName("'Shared with me' section exists and loads")
    void sharedSectionLoads() {
        DriveHomePage drive = new DriveHomePage(driver);
        assertTrue(drive.hasSectionTabs(), "Section tabs expected");
        assumeTrue(drive.showSharedWithMe(), "Could not switch to 'Shared with me'");
        assertTrue(drive.isListReadable(),
                "'Shared with me' should render either items or an empty state");
        System.out.println(">>> shared-with-me empty title='" + drive.emptyTitle() + "'");
        screenshot("drive-shared-section");
    }

    @Test
    @Order(2)
    @DisplayName("An empty 'Shared with me' explains itself ('Folders someone shares with you will appear here')")
    void sharedEmptyStateWording() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showSharedWithMe(), "Could not switch section");
        drive.showFolders();
        if (drive.isEmptyStateShown()) {
            String title = drive.emptyTitle();
            String sub = drive.emptySubtitle();
            System.out.println(">>> empty: '" + title + "' / '" + sub + "'");
            assertTrue((title + " " + sub).toLowerCase().contains("shar"),
                    "The empty state should say these are items shared WITH you. Got: '"
                            + title + "' / '" + sub + "'");
        } else {
            System.out.println(">>> folders already shared with this user — empty-state case not applicable");
        }
        screenshot("drive-shared-empty");
    }

    @Test
    @Order(3)
    @DisplayName("A file shared with this user APPEARS under Shared with me → Files")
    void sharedFileArrives() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showSharedWithMe(), "Could not switch to 'Shared with me'");
        drive.showFiles();
        drive.pullToRefresh();
        sleep(4000);

        List<String> files = drive.allItemNamesScrolled();
        System.out.println(">>> shared-with-me files = " + files);
        assertTrue(drive.isListReadable(), "The shared list must be readable before judging its contents");
        assertTrue(files.stream().anyMatch(n -> n.equalsIgnoreCase(SHARED_FILE)),
                "The file shared by the other user should appear here. Expected '" + SHARED_FILE
                        + "', got: " + files
                        + " — run --drive-share on the OWNER device first.");
        screenshot("drive-shared-file-arrived");
    }

    @Test
    @Order(4)
    @DisplayName("The shared item is NOT in this user's own My Drive (it belongs to the owner)")
    void notInMyDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showMyDrive(), "Could not switch to My Drive");
        drive.showFiles();
        assumeTrue(drive.isListReadable(), "My Drive list unreadable");
        List<String> mine = drive.allItemNamesScrolled();
        System.out.println(">>> my own files = " + mine);
        assertFalse(mine.stream().anyMatch(n -> n.equalsIgnoreCase(SHARED_FILE)),
                "A file shared WITH you must not appear in your own My Drive. Got: " + mine);
        screenshot("drive-shared-not-in-mydrive");
    }

    // ==================== permission enforcement ====================

    @Test
    @Order(5)
    @DisplayName("RIGHTS: the recipient's menu matches EXACTLY the rights they were granted")
    void rightsMatchGrant() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showSharedWithMe(), "Could not switch section");
        drive.showFiles();
        assumeTrue(drive.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(SHARED_FILE)),
                "Shared file not present — run --drive-share on the owner device first");
        assumeTrue(drive.openItemMenu(SHARED_FILE), "Could not open the row menu");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.hasDetails() || menu.hasFavorite(), "The row menu did not open");

        boolean share = menu.hasShare(), move = menu.hasMove(),
                edit = menu.hasEditInfo(), delete = menu.hasDelete();
        System.out.println(">>> granted='" + GRANTED_RIGHTS + "' menu: favourite=" + menu.hasFavorite()
                + " details=" + menu.hasDetails()
                + " share=" + share + " move=" + move + " edit=" + edit + " delete=" + delete);
        screenshot("drive-recipient-menu-" + GRANTED_RIGHTS);

        // Read-only actions are always available to anyone with access.
        assertTrue(menu.hasFavorite(), "A recipient should always be able to favourite");
        assertTrue(menu.hasDetails(), "A recipient should always be able to open Details");

        // DELETE belongs to the owner alone, at every rights level.
        assertFalse(delete, "A non-owner must NEVER be offered Delete, whatever their rights");

        if ("edit".equalsIgnoreCase(GRANTED_RIGHTS)) {
            // An EDITOR may reshape the item…
            assertTrue(edit, "An editor should be offered Edit Info");
            assertTrue(move, "An editor should be offered Move to…");
        } else {
            // …a VIEWER may not. Verified live: with View+Download the menu held exactly
            // two entries (Add to favorites, Details) against the owner's six.
            assertFalse(share, "A viewer must NOT be offered Share");
            assertFalse(move, "A viewer must NOT be offered Move to…");
            assertFalse(edit, "A viewer must NOT be offered Edit Info");
        }
        menu.dismiss();
    }

    @Test
    @Order(6)
    @DisplayName("RIGHTS: the recipient CAN still favourite and view Details on a shared item")
    void viewerCanReadOnlyActions() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showSharedWithMe(), "Could not switch section");
        drive.showFiles();
        assumeTrue(drive.openItemMenu(SHARED_FILE), "Could not open the row menu");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assertTrue(menu.hasFavorite(), "A recipient should still be able to favourite a shared item");
        assertTrue(menu.hasDetails(), "A recipient should still be able to open Details");
        screenshot("drive-recipient-readonly-actions");
        menu.dismiss();
    }

    @Test
    @Order(7)
    @DisplayName("Details of a shared item names the OWNER, not this user")
    void sharedDetailsShowOwner() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showSharedWithMe(), "Could not switch section");
        drive.showFiles();
        assumeTrue(drive.openItemMenu(SHARED_FILE), "Could not open the row menu");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.hasDetails() && menu.tapDetails(), "Could not open Details");
        assumeTrue(menu.detailsOpen(), "Details sheet did not open");
        System.out.println(">>> shared item details: name='" + menu.detailsFolderName()
                + "' owner shown=" + menu.detailsHasOwner());
        assertTrue(menu.detailsHasOwner(), "Details should name the owner of a shared item");
        screenshot("drive-shared-details");
        menu.dismiss();
    }

    @Test
    @Order(8)
    @DisplayName("A shared FOLDER can be opened by the recipient")
    void sharedFolderOpens() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showSharedWithMe(), "Could not switch section");
        drive.showFolders();
        List<String> folders = drive.allItemNamesScrolled();
        System.out.println(">>> shared folders = " + folders);
        assumeTrue(!folders.isEmpty(), "No folder has been shared with this user yet");

        String first = folders.get(0);
        assumeTrue(drive.openItem(first), "Could not open the shared folder '" + first + "'");
        sleep(2500);
        assertTrue(new com.zillit.automation.pages.DriveFolderPage(driver)
                        .isLoaded(Duration.ofSeconds(15)),
                "A shared folder should open for a recipient with access");
        screenshot("drive-shared-folder-open");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
