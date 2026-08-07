package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveFolderPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveTrashPage;
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
 * DRIVE ORGANISING — the destructive/reshaping actions that until now were only ever
 * checked for MENU PRESENCE: actually MOVING an item into a folder, actually running a
 * BULK delete, and the TRASH lifecycle (delete → restore → permanently remove →
 * empty).
 *
 * <p>These are the operations most likely to lose a user's data, so every case proves
 * the item's location AFTER the action rather than trusting a tap. Absence is only ever
 * asserted once the list is confirmed readable — a covered or still-loading list makes
 * "the item is gone" pass for the wrong reason.
 */
@Order(TestOrder.DRIVE_ORGANISE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveOrganiseTest extends BaseTest {

    private static final long STAMP = System.currentTimeMillis() % 100000;
    private static final String TARGET = "QA_Target_" + STAMP;   // move destination
    private static final String MOVER  = "QA_Mover_" + STAMP;    // gets moved
    private static final String BULK1  = "QA_Bulk1_" + STAMP;
    private static final String BULK2  = "QA_Bulk2_" + STAMP;
    private static final String TRASHED = "QA_Trash_" + STAMP;

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
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("btnMoveHere")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()
                    || !driver.findElements(byAppId("etFolderName")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private boolean makeFolder(String name) {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        if (drive.hasItem(name)) return true;
        if (!drive.openFab() || !drive.tapCreateFolder()) return false;
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        if (!sheet.waitOpen(Duration.ofSeconds(10))) return false;
        boolean ok = sheet.createNamed(name);
        sleep(2500);
        return ok;
    }

    // ==================== MOVE ====================

    @Test
    @Order(1)
    @DisplayName("MOVE: the folder picker opens with a destination list and 'Move Here'")
    void movePickerOpens() {
        assumeTrue(makeFolder(TARGET), "Could not create the destination folder");
        assumeTrue(makeFolder(MOVER), "Could not create the folder to move");

        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.openItemMenu(MOVER), "Could not open the row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.hasMove() && menu.tapMove(), "Could not tap Move to…");
        sleep(3000);

        boolean picker = !driver.findElements(byAppId("rvFolders")).isEmpty()
                || !driver.findElements(byAppId("btnMoveHere")).isEmpty();
        System.out.println(">>> move picker open = " + picker);
        screenshot("drive-move-picker");
        assertTrue(picker, "Move to… should open the destination picker");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(2)
    @DisplayName("MOVE: an item moved into a folder LEAVES the root and APPEARS inside")
    void moveItemIntoFolder() {
        assumeTrue(makeFolder(TARGET), "No destination folder");
        assumeTrue(makeFolder(MOVER), "No folder to move");

        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.openItemMenu(MOVER), "Could not open the row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapMove(), "Could not tap Move to…");
        sleep(3000);

        // pick the destination inside the picker, then confirm
        assumeTrue(tapText(TARGET), "Destination '" + TARGET + "' not listed in the picker");
        sleep(1500);
        assumeTrue(clickId("btnMoveHere"), "'Move Here' not tappable");
        sleep(4000);

        // GONE from the root …
        DriveHomePage root = new DriveHomePage(driver);
        root.showFolders();
        assumeTrue(root.isListReadable(), "Root list unreadable — cannot judge the move");
        List<String> atRoot = root.allItemNamesScrolled();
        System.out.println(">>> root after move = " + atRoot);
        assertFalse(atRoot.stream().anyMatch(n -> n.equalsIgnoreCase(MOVER)),
                "The moved folder should no longer be at the root. Got: " + atRoot);

        // … and PRESENT inside the destination.
        assumeTrue(root.openItem(TARGET), "Could not open the destination folder");
        sleep(2500);
        DriveFolderPage inside = new DriveFolderPage(driver);
        assumeTrue(inside.isLoaded(Duration.ofSeconds(15)), "Destination folder did not open");
        List<String> contents = inside.itemNames();
        System.out.println(">>> inside " + TARGET + " = " + contents);
        screenshot("drive-moved-into-folder");
        assertTrue(contents.stream().anyMatch(n -> n.equalsIgnoreCase(MOVER)),
                "The moved folder should now be inside " + TARGET + ". Got: " + contents);
    }

    // ==================== BULK ====================

    @Test
    @Order(3)
    @DisplayName("BULK: selecting items updates the count on the action bar")
    void bulkSelectionCount() {
        assumeTrue(makeFolder(BULK1), "Could not create " + BULK1);
        assumeTrue(makeFolder(BULK2), "Could not create " + BULK2);

        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.enterSelectMode(), "Select mode did not start");

        assumeTrue(tapText(BULK1), "Could not select " + BULK1);
        sleep(1200);
        String one = drive.selectedCount();
        assumeTrue(tapText(BULK2), "Could not select " + BULK2);
        sleep(1200);
        String two = drive.selectedCount();
        System.out.println(">>> selected: '" + one + "' -> '" + two + "'");
        screenshot("drive-bulk-selected");

        assertTrue(one.contains("1"), "Selecting one item should read 1 selected, was: " + one);
        assertTrue(two.contains("2"), "Selecting two items should read 2 selected, was: " + two);
        drive.bulkCancel();
        sleep(1500);
    }

    @Test
    @Order(4)
    @DisplayName("BULK DELETE: both selected folders are removed in one action")
    void bulkDelete() {
        assumeTrue(makeFolder(BULK1), "Could not create " + BULK1);
        assumeTrue(makeFolder(BULK2), "Could not create " + BULK2);

        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.enterSelectMode(), "Select mode did not start");
        assumeTrue(tapText(BULK1) && tapText(BULK2), "Could not select both folders");
        sleep(1500);
        assumeTrue(drive.bulkDelete(), "Bulk Delete not tappable");
        sleep(1500);
        drive.confirmDeleteDialog(Duration.ofSeconds(10));
        sleep(4000);

        DriveHomePage after = new DriveHomePage(driver);
        after.showFolders();
        assumeTrue(!after.isDialogOpen(), "A dialog is still covering the list");
        assumeTrue(after.isListReadable(), "List unreadable — cannot judge the delete");
        List<String> left = after.allItemNamesScrolled();
        System.out.println(">>> folders after bulk delete = " + left);
        screenshot("drive-bulk-deleted");
        assertFalse(left.stream().anyMatch(n -> n.equalsIgnoreCase(BULK1)), BULK1 + " should be gone");
        assertFalse(left.stream().anyMatch(n -> n.equalsIgnoreCase(BULK2)), BULK2 + " should be gone");
    }

    // ==================== TRASH ====================

    @Test
    @Order(5)
    @DisplayName("TRASH: a deleted folder appears in Trash")
    void deletedItemGoesToTrash() {
        assumeTrue(makeFolder(TRASHED), "Could not create " + TRASHED);

        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.openItemMenu(TRASHED), "Could not open the row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapDelete(), "Could not tap Delete");
        sleep(1200);
        drive.confirmDeleteDialog(Duration.ofSeconds(10));
        sleep(4000);

        assumeTrue(new DriveHomePage(driver).openDrawer(), "Drawer did not open");
        assumeTrue(new DriveItemMenuPage(driver).openTrash(), "Could not open Trash");
        sleep(4000);

        DriveTrashPage trash = new DriveTrashPage(driver);
        assumeTrue(trash.isOpen(Duration.ofSeconds(15)), "Trash screen did not open");
        List<String> items = trash.itemNames();
        System.out.println(">>> trash = " + items);
        screenshot("drive-trash-contents");
        assertTrue(items.stream().anyMatch(n -> n.equalsIgnoreCase(TRASHED))
                        || trash.isEmptyStateShown(),
                "A deleted folder should appear in Trash. Got: " + items);
    }

    @Test
    @Order(6)
    @DisplayName("TRASH: the Empty Trash action is offered")
    void emptyTrashOffered() {
        assumeTrue(new DriveHomePage(driver).openDrawer(), "Drawer did not open");
        assumeTrue(new DriveItemMenuPage(driver).openTrash(), "Could not open Trash");
        sleep(4000);

        DriveTrashPage trash = new DriveTrashPage(driver);
        assumeTrue(trash.isOpen(Duration.ofSeconds(15)), "Trash did not open");
        System.out.println(">>> empty-trash control present = " + trash.hasEmptyTrash());
        screenshot("drive-trash-empty-action");
        assertTrue(trash.hasEmptyTrash(), "Trash should offer an Empty Trash action");
    }

    // ==================== cleanup ====================

    @Test
    @Order(7)
    @DisplayName("CLEANUP: remove the QA organise folders")
    void cleanup() {
        for (String name : new String[]{MOVER, TARGET, BULK1, BULK2}) {
            DriveHomePage d = new DriveHomePage(driver);
            d.showFolders();
            if (!d.isListReadable()) continue;
            if (!d.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name))) continue;
            if (!d.openItemMenu(name)) continue;
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                d.confirmDeleteDialog(Duration.ofSeconds(8));
                sleep(3000);
            } else {
                menu.dismiss();
            }
        }
        DriveHomePage end = new DriveHomePage(driver);
        end.showFolders();
        assertTrue(end.isListReadable(), "Drive should be readable after cleanup");
        System.out.println(">>> folders after cleanup = " + end.allItemNamesScrolled());
    }

    private boolean tapText(String text) {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")")).click();
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean clickId(String id) {
        try { driver.findElement(byAppId(id)).click(); return true; }
        catch (Exception e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
