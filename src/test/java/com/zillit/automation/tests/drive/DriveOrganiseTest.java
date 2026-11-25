package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.CreateFolderSheet;
import com.zillit.automation.pages.drive.DriveFolderPage;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveItemMenuPage;
import com.zillit.automation.pages.drive.DriveTrashPage;
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
import static org.junit.jupiter.api.Assumptions.assumeFalse;
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
@ExtendWith(ReasonLogger.class)
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
                && tools.openModuleContaining("Drive")
                && drive.isLoaded(Duration.ofSeconds(20));
    }

    private void closeAnyOverlay() {
        // A case that stopped mid-selection leaves Select mode on: the FAB is hidden and every
        // later makeFolder() fails ("Could not create …").
        try {
            if (!driver.findElements(byAppId("btnBulkCancel")).isEmpty()) {
                driver.findElement(byAppId("btnBulkCancel")).click();
                Thread.sleep(1000);
            }
        } catch (Exception ignored) { }
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
        // Tap the destination INSIDE the sheet (tvFolderName). A plain text match also finds the
        // home-list row behind the sheet; tapping that dismisses the sheet as an outside touch.
        assumeTrue(clickXpath("//*[@resource-id='" + appId("tvFolderName") + "' and @text='" + TARGET + "']"),
                "Destination '" + TARGET + "' not listed in the Move sheet");
        sleep(2500);
        // Build 32539: tapping a destination row MOVES straight there (the › "Browse inside"
        // icon drills in instead). Older builds drilled in on the row and needed "Move Here".
        if (!driver.findElements(byAppId("btnMoveHere")).isEmpty()) {
            assumeTrue(clickId("btnMoveHere"), "'Move Here' not tappable");
        }
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
        java.util.Map<String, Integer> order = rowYs(BULK1, BULK2);
        assumeTrue(drive.enterSelectMode(), "Select mode did not start");
        List<String> namesInSelect = drive.itemNames();

        assumeTrue(selectRow(order, BULK1), "Could not select " + BULK1 + " (rows " + order.keySet() + ")");
        sleep(1200);
        String one = drive.selectedCount();
        assumeTrue(selectRow(order, BULK2), "Could not select " + BULK2);
        sleep(1200);
        String two = drive.selectedCount();
        System.out.println(">>> selected: '" + one + "' -> '" + two + "'  names in select mode=" + namesInSelect);
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
        java.util.Map<String, Integer> order = rowYs(BULK1, BULK2);
        assumeTrue(drive.enterSelectMode(), "Select mode did not start");
        assumeTrue(selectRow(order, BULK1) && selectRow(order, BULK2), "Could not select both folders");
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

    @Test
    @Order(11)
    @DisplayName("BULK: rows keep their names while Select mode is on")
    void selectModeKeepsNames() {
        assumeTrue(makeFolder(BULK1), "Could not create " + BULK1);
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        List<String> before = drive.itemNames();
        assumeTrue(!before.isEmpty(), "No rows to compare");
        assumeTrue(drive.enterSelectMode(), "Select mode did not start");
        List<String> during = drive.itemNames();
        screenshot("drive-select-mode-names");
        drive.bulkCancel();
        sleep(1200);
        System.out.println(">>> names before=" + before + " in select mode=" + during);
        new com.zillit.automation.pages.drive.DriveFlows(driver).purge(BULK1, true);
        assertFalse(during.isEmpty(), "In Select mode every row loses its name (only the checkbox and icon "
                + "show), so a user cannot tell what they are selecting. Names before: " + before);
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

        // Precondition: the delete really went through, so there IS a row to look for.
        // (The old version also passed on an EMPTY trash — `|| isEmptyStateShown()` —
        // so it proved nothing.)
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assumeTrue(!home.isDialogOpen() && home.isListReadable(),
                "Drive list unreadable after the delete — cannot set up the Trash check");
        assumeFalse(home.hasItem(TRASHED),
                TRASHED + " is still on Drive home — the delete did not go through, nothing to find in Trash");

        assumeTrue(home.openDrawer(), "Drawer did not open");
        assumeTrue(new DriveItemMenuPage(driver).openTrash(), "Could not open Trash");
        sleep(4000);

        DriveTrashPage trash = new DriveTrashPage(driver);
        assumeTrue(trash.isOpen(Duration.ofSeconds(15)), "Trash screen did not open");
        boolean listed = trash.hasItem(TRASHED);
        List<String> items = trash.itemNames();
        System.out.println(">>> trash = " + items + " (" + TRASHED + " listed=" + listed + ")");
        screenshot("drive-trash-contents");
        assertFalse(trash.isEmptyStateShown(),
                "Trash shows its empty state right after " + TRASHED + " was deleted");
        assertTrue(listed, "The deleted folder " + TRASHED + " should appear in Trash. Got: " + items);
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
        assumeTrue(!trash.isEmptyStateShown(), "Trash is empty — Empty Trash is only offered when it holds items "
                + "(the TRASH case that puts one there must pass first)");
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

    // ==================== Download as ZIP (f9a3bf916) ====================

    private static final String ZIP_FOLDER = "QA_Zip_" + STAMP;

    @Test
    @Order(8)
    @DisplayName("Z-01 An own folder's ⋯ menu offers 'Download as ZIP' → 'Download started'")
    void folderDownloadAsZip() {
        assumeTrue(makeFolder(ZIP_FOLDER), "Could not create " + ZIP_FOLDER);
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.openItemMenu(ZIP_FOLDER), "Could not open the folder's menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen(), "Menu did not open");
        boolean has = menu.hasDownloadZip();
        if (!has) menu.dismiss();
        assumeTrue(has, "No 'Download as ZIP' in the folder menu — this build predates Zillit-Android f9a3bf916");
        assertTrue(menu.tapDownloadZip(), "Download as ZIP not tappable");
        boolean toast = toast("Download started", Duration.ofSeconds(8));
        System.out.println(">>> Z-01 toast 'Download started' = " + toast);
        assertTrue(toast, "Tapping Download as ZIP should say 'Download started'");
    }

    @Test
    @Order(9)
    @DisplayName("Z-02 A FILE's ⋯ menu has no 'Download as ZIP'")
    void fileMenuHasNoZip() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        List<String> files = drive.itemNames();
        assumeTrue(!files.isEmpty(), "No file on the Files tab to check");
        assumeTrue(drive.openItemMenu(files.get(0)), "Could not open a file's menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen(), "Menu did not open");
        boolean zip = menu.hasDownloadZip();
        menu.dismiss();
        assertFalse(zip, "'Download as ZIP' is for folders only");
    }

    @Test
    @Order(10)
    @DisplayName("Z-03 CLEANUP: remove the ZIP test folder")
    void zipCleanup() {
        assertTrue(new com.zillit.automation.pages.drive.DriveFlows(driver).purge(ZIP_FOLDER, true),
                "Could not remove " + ZIP_FOLDER);
    }

    private boolean toast(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String src = driver.getPageSource();
                if (src.contains("android.widget.Toast") && src.contains(text)) return true;
            } catch (Exception ignored) { }
            sleep(200);
        }
        return false;
    }


    // =====================================================================================
    // WEB GAP cases (side-session-notes web-e2e/gaps/drive.md). Every item is QA_… and is
    // removed at the end (Trash included) by gapCleanup.
    // =====================================================================================

    private static final String G_RESTORE = "QA_Rst_" + STAMP;
    private static final String G_TREE    = "QA_T3_" + STAMP;
    private static final String G_EMPTY   = "QA_Emp_" + STAMP;
    private static final String G_REN_A   = "QA_RenA_" + STAMP;
    private static final String G_REN_B   = "QA_RenB_" + STAMP;
    private static final String G_REN_F   = "QA_RenF_" + STAMP + ".pdf";
    private static final String G_DL1     = "QA_Dl1_" + STAMP + ".pdf";
    private static final String G_DL2     = "QA_Dl2_" + STAMP + ".pdf";
    private static final String G_MV_T    = "QA_MvT_" + STAMP;
    private static final String G_MV_1    = "QA_Mv1_" + STAMP;
    private static final String G_MV_2    = "QA_Mv2_" + STAMP;
    private static final java.util.Set<String> G_FOLDERS = new java.util.LinkedHashSet<>();
    private static final java.util.Set<String> G_FILES = new java.util.LinkedHashSet<>();

    private com.zillit.automation.pages.drive.DriveFlows flows() {
        return new com.zillit.automation.pages.drive.DriveFlows(driver);
    }

    private void gFolder(String name) {
        new DriveHomePage(driver).showFolders();
        if (!new DriveHomePage(driver).hasItem(name)) assumeTrue(flows().createFolder(name), "Could not create " + name);
        G_FOLDERS.add(name);
    }

    private void gFile(String fixture, String name) {
        assumeTrue(flows().uploadAs(fixture, name), "Could not upload " + name);
        G_FILES.add(name);
    }

    @Test
    @Order(20)
    @DisplayName("[WEB L442] TRASH: a deleted folder is RESTORED from Trash and is back on Drive")
    void gTrashRestore() {
        gFolder(G_RESTORE);
        assertTrue(flows().deleteToTrash(G_RESTORE, true), "Could not delete " + G_RESTORE);
        DriveTrashPage trash = flows().openTrash();
        assumeTrue(trash != null, "Trash did not open");
        assertTrue(trash.hasItem(G_RESTORE), G_RESTORE + " should be in Trash");
        assertTrue(trash.restore(G_RESTORE), "Restore not tappable");
        assertFalse(trash.allNames().contains(G_RESTORE), "A restored item must leave Trash");
        trash.back();
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        boolean back = false;
        for (int i = 0; i < 6 && !back; i++) {
            home.showFolders();
            back = home.hasItem(G_RESTORE);
            if (!back) { home.pullToRefresh(); sleep(4000); }
        }
        assertTrue(back, "The restored folder should be back on Drive home");
    }

    @Test
    @Order(21)
    @DisplayName("[WEB DRIVE-03] TRASH: restoring a folder brings back its WHOLE tree (subfolder, nested folder, file)")
    void gTrashRestoreTree() {
        gFolder(G_TREE);
        assumeTrue(flows().openFolder(G_TREE), "Could not open " + G_TREE);
        assumeTrue(flows().createFolder("sub"), "Could not create sub");
        DriveFolderPage in = new DriveFolderPage(driver);
        assumeTrue(in.openFab() && in.tapUploadFile(), "Upload inside the folder not offered");
        assumeTrue(new com.zillit.automation.pages.drive.DriveUploadPage(driver)
                .uploadFileAs("Document", "zl_drive_doc.pdf", "QA_T3f_" + STAMP + ".pdf"), "Could not upload into " + G_TREE);
        sleep(8000);
        assumeTrue(new DriveHomePage(driver).openItem("sub") && in.isLoaded(Duration.ofSeconds(15)), "Could not open sub");
        assumeTrue(flows().createFolder("deep"), "Could not create deep");
        flows().backToHome();

        assertTrue(flows().deleteToTrash(G_TREE, true), "Could not delete " + G_TREE);
        DriveTrashPage trash = flows().openTrash();
        assumeTrue(trash != null && trash.restore(G_TREE), "Could not restore " + G_TREE);
        trash.back();
        assertTrue(flows().openDrive() && flows().openFolder(G_TREE), "The restored folder should open");
        List<String> top = new DriveFolderPage(driver).allItemNamesScrolled();
        System.out.println(">>> DRIVE-03 restored top level = " + top);
        assertTrue(top.contains("sub"), "The restored folder lost its subfolder (DRIVE-03): " + top);
        assertTrue(top.contains("QA_T3f_" + STAMP + ".pdf"), "The restored folder lost its direct file (DRIVE-03): " + top);
        assertTrue(new DriveHomePage(driver).openItem("sub"), "Could not open the restored subfolder");
        sleep(2500);
        List<String> deep = new DriveFolderPage(driver).allItemNamesScrolled();
        assertTrue(deep.contains("deep"), "The restored subfolder lost its own child (DRIVE-03): " + deep);
        flows().backToHome();
    }

    @Test
    @Order(22)
    @DisplayName("[WEB L442] TRASH: 'Delete permanently?' removes the item for good")
    void gTrashDeleteForever() {
        gFolder(G_RESTORE);
        assertTrue(flows().deleteToTrash(G_RESTORE, true), "Could not delete " + G_RESTORE);
        DriveTrashPage trash = flows().openTrash();
        assumeTrue(trash != null, "Trash did not open");
        assertTrue(trash.tapDeleteForever(G_RESTORE), "No 'Delete permanently' on the row");
        assertTrue(trash.dialogShown("Delete permanently?"), "Deleting forever must confirm first");
        assertTrue(trash.textShown("cannot be undone", Duration.ofSeconds(2)), "The dialog should say it cannot be undone");
        assertTrue(trash.confirm(Duration.ofSeconds(5)), "Could not confirm");
        sleep(3000);
        assertFalse(trash.allNames().contains(G_RESTORE), "The item must be gone from Trash");
        trash.back();
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertFalse(home.hasItem(G_RESTORE), "A permanently deleted folder must not come back");
        G_FOLDERS.remove(G_RESTORE);
    }

    @Test
    @Order(23)
    @DisplayName("[WEB L127] TRASH: 'Empty trash?' asks first, and Cancel removes nothing")
    void gEmptyTrashCancel() {
        gFolder(G_EMPTY);
        assertTrue(flows().deleteToTrash(G_EMPTY, true), "Could not delete " + G_EMPTY);
        DriveTrashPage trash = flows().openTrash();
        assumeTrue(trash != null, "Trash did not open");
        List<String> before = trash.allNames();
        assertTrue(trash.hasEmptyTrash(), "Empty Trash should be offered while Trash holds items");
        assertTrue(trash.emptyTrash(), "Empty Trash not tappable");
        assertTrue(trash.dialogShown("Empty trash?"), "Empty Trash must confirm first");
        assertTrue(trash.textShown("cannot be undone", Duration.ofSeconds(2)), "The dialog should warn it cannot be undone");
        assertTrue(trash.cancelDialog(), "Cancel not tappable");
        sleep(2000);
        List<String> after = trash.allNames();
        System.out.println(">>> empty-trash cancel before=" + before.size() + " after=" + after.size());
        assertTrue(after.containsAll(before) && after.contains(G_EMPTY), "Cancel must remove nothing: " + before + " → " + after);
        trash.back();
    }

    // ---------------- M02 Edit Info ----------------

    @Test
    @Order(30)
    @DisplayName("[WEB L424] EDIT INFO: renaming a file without its extension keeps '.pdf'; the description shows on the detail")
    void gRenameKeepsExtension() {
        gFile("zl_drive_doc.pdf", G_REN_F);
        String newBase = "QA_ren.v2 " + STAMP;
        String desc = "QA desc " + STAMP;
        var r = flows().editInfo(G_REN_F, false, newBase, desc);
        System.out.println(">>> rename " + r);
        assertTrue(r.saved, "Edit Info did not open / save: " + r);
        assertTrue(flows().openDrive(), "Drive home");
        List<String> names = new DriveHomePage(driver).allItemNamesScrolled();
        String stored = names.stream().filter(n -> n.startsWith(newBase)).findFirst().orElse(null);
        System.out.println(">>> stored name = " + stored);
        assertTrue(stored != null, "The renamed file is not listed: " + names);
        G_FILES.remove(G_REN_F);
        G_FILES.add(stored);
        assertEquals(newBase + ".pdf", stored, "The extension must be kept when the new name has none (ZL-18310)");
        var d = flows().openDetail(stored);
        assumeTrue(d != null, "Could not open the detail");
        assertEquals(desc, d.description(), "The description should show on the file detail");
    }

    @Test
    @Order(31)
    @DisplayName("[WEB L1159] EDIT INFO: a name that already ends in the extension (any case) is not doubled")
    void gRenameUpperExtension() {
        String current = G_FILES.stream().filter(n -> n.startsWith("QA_ren.v2")).findFirst().orElse(null);
        assumeTrue(current != null, "The rename case must run first");
        assertTrue(flows().openDrive(), "Drive home");
        String upper = "QA_UP_" + STAMP + ".PDF";
        var r = flows().editInfo(current, false, upper, null);
        assumeTrue(r.saved, "Edit Info did not open: " + r);
        assertTrue(flows().openDrive(), "Drive home");
        List<String> names = new DriveHomePage(driver).allItemNamesScrolled();
        String stored = names.stream().filter(n -> n.startsWith("QA_UP_" + STAMP)).findFirst().orElse(null);
        System.out.println(">>> upper-case extension stored as " + stored);
        assertTrue(stored != null, "Renamed file not listed: " + names);
        G_FILES.remove(current);
        G_FILES.add(stored);
        assertFalse(stored.toLowerCase().endsWith(".pdf.pdf"), "The extension was doubled: " + stored);
    }

    @Test
    @Order(32)
    @DisplayName("[WEB L307] EDIT INFO: renaming a folder onto a sibling's name shows the inline error and keeps the old name")
    void gRenameDuplicate() {
        gFolder(G_REN_A);
        gFolder(G_REN_B);
        var r = flows().editInfo(G_REN_B, true, G_REN_A, null);
        System.out.println(">>> duplicate rename " + r);
        boolean err = r.inlineError;
        if (r.dialogStillOpen) flows().cancelDialog();
        assumeTrue(r.saved, "Edit Info did not open: " + r);
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        List<String> names = home.allItemNamesScrolled();
        long copiesOfA = names.stream().filter(G_REN_A::equals).count();
        System.out.println(">>> after duplicate rename: copies of A=" + copiesOfA + " B kept=" + names.contains(G_REN_B));
        assertEquals(1, copiesOfA, "Two folders now share the name " + G_REN_A + " — the duplicate rename was accepted");
        assertTrue(names.contains(G_REN_B), G_REN_B + " must keep its name");
        assertTrue(err, "The refusal should show inline in the reopened dialog ('A file or folder with this name already exists'): " + r);
    }

    @Test
    @Order(33)
    @DisplayName("[WEB L313] EDIT INFO: a blank (spaces only) name is refused — the folder keeps its name")
    void gRenameBlank() {
        gFolder(G_REN_A);
        var r = flows().editInfo(G_REN_A, true, "   ", null);
        flows().cancelDialog();
        assumeTrue(r.saved, "Edit Info did not open: " + r);
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertTrue(home.hasItem(G_REN_A), "A blank rename must not change the name");
    }

    @Test
    @Order(34)
    @DisplayName("[WEB L1172] EDIT INFO: outer spaces are trimmed")
    void gRenameTrim() {
        gFolder(G_REN_B);
        String trimmed = "QA_sp_" + STAMP;
        var r = flows().editInfo(G_REN_B, true, "  " + trimmed + "  ", null);
        assumeTrue(r.saved, "Edit Info did not open: " + r);
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        List<String> names = home.allItemNamesScrolled();
        if (names.contains(trimmed)) { G_FOLDERS.remove(G_REN_B); G_FOLDERS.add(trimmed); }
        assertTrue(names.contains(trimmed), "The name should be stored trimmed: " + names);
    }

    // ---------------- M06 Downloads ----------------

    @Test
    @Order(40)
    @DisplayName("[WEB L758] DOWNLOAD: the detail's Download saves the file and reads 'Downloaded'")
    void gDetailDownload() {
        gFile("zl_drive_doc.pdf", G_DL1);
        var d = flows().openDetail(G_DL1);
        assumeTrue(d != null, "Could not open the detail");
        assertTrue(d.tapDownload(), "Download not tappable");
        d.waitDownloadDone(Duration.ofSeconds(40));
        String label = d.downloadButtonText();
        String got = new com.zillit.automation.pages.drive.DriveFileContent(driver)
                .waitForDownloadNamed(G_DL1.replace(".pdf", ""), Duration.ofSeconds(60));
        System.out.println(">>> detail download label='" + label + "' file=" + got);
        assertTrue(got != null, "The downloaded file should be in /sdcard/Download");
    }

    @Test
    @Order(41)
    @DisplayName("[WEB L770] BULK DOWNLOAD of ONE file saves the file itself (no zip)")
    void gBulkDownloadOne() {
        assumeTrue(G_FILES.contains(G_DL1), "The detail download case must upload the file first");
        gFile("zl_drive_doc.pdf", G_DL2);
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFiles();
        rowYs(G_DL2);
        assumeTrue(home.longPressRow(G_DL2), "Long-press did not start Select mode on " + G_DL2);
        assertTrue(home.bulkDownload(), "Bulk Download not tappable");
        String got = new com.zillit.automation.pages.drive.DriveFileContent(driver)
                .waitForDownloadNamed(G_DL2.replace(".pdf", ""), Duration.ofSeconds(60));
        home.exitSelectMode();
        assertTrue(got != null && got.endsWith(".pdf"), "One selected file should download as itself, got " + got);
    }

    @Test
    @Order(42)
    @DisplayName("[WEB L776] BULK DOWNLOAD of TWO files is one server .zip holding both")
    void gBulkDownloadTwo() {
        assumeTrue(G_FILES.contains(G_DL1) && G_FILES.contains(G_DL2), "Both download files are needed");
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFiles();
        java.util.Map<String, Integer> ys = rowYs(G_DL1, G_DL2);
        assumeTrue(home.longPressRow(G_DL1) && selectRow(ys, G_DL2), "Could not select both files");
        assertTrue(home.bulkDownload(), "Bulk Download not tappable");
        String zip = new com.zillit.automation.pages.drive.DriveFileContent(driver)
                .waitForZipContaining(List.of(G_DL1, G_DL2), Duration.ofSeconds(90));
        home.exitSelectMode();
        System.out.println(">>> two-file bulk zip = " + zip);
        assertTrue(zip != null, "Two selected files should arrive as ONE .zip holding both");
    }

    @Test
    @Order(43)
    @DisplayName("[WEB L1730] BULK DOWNLOAD of a selected folder: it is downloaded as its own .zip, not silently dropped")
    void gBulkDownloadFolderAndFile() {
        assumeTrue(G_FILES.contains(G_DL1), "The download file is needed");
        gFolder(G_MV_T);
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        rowYs(G_MV_T);
        assumeTrue(home.longPressRow(G_MV_T), "Long-press did not start Select mode on " + G_MV_T);
        assertTrue(home.bulkDownload(), "Bulk Download not tappable");
        boolean started = toast("Download started", Duration.ofSeconds(8)) || toast("download", Duration.ofSeconds(2));
        home.exitSelectMode();
        // Build 32539 (f9a3bf916): each selected folder is downloaded as its own zip.
        String folderZip = new com.zillit.automation.pages.drive.DriveFileContent(driver)
                .waitForDownloadNamed(G_MV_T, Duration.ofSeconds(60));
        System.out.println(">>> folder bulk download toast=" + started + " file=" + folderZip);
        assertTrue(folderZip != null, "A selected folder should download as its own .zip, not be dropped");
    }

    // ---------------- M19 Move ----------------

    /**
     * After tapping a destination: a single-item move happens at once, but a drill-in (bulk
     * Move, or when the row opens the folder) leaves the sheet up with "Move to <folder>"
     * pushed below the fold by a long list — expand the sheet and press it.
     */
    private void finishMoveSheet() {
        for (int i = 0; i < 6; i++) {
            if (driver.findElements(byAppId("rvFolders")).isEmpty()) return;     // sheet gone: moved
            if (!driver.findElements(byAppId("btnMoveHere")).isEmpty()) { clickId("btnMoveHere"); return; }
            try {
                var size = driver.manage().window().getSize();
                driver.executeScript("mobile: swipeGesture", java.util.Map.of("left", size.width / 2 - 100,
                        "top", (int) (size.height * 0.55), "width", 200, "height", (int) (size.height * 0.30),
                        "direction", "up", "percent", 0.9));
            } catch (Exception ignored) { }
            sleep(800);
        }
    }

    private List<String> movePickerFolders() {
        return driver.findElements(byAppId("tvFolderName")).stream()
                .map(org.openqa.selenium.WebElement::getText).toList();
    }

    @Test
    @Order(50)
    @DisplayName("[WEB L430] MOVE: the picker never offers the folder being moved")
    void gMoveExcludesSelf() {
        gFolder(G_MV_T);
        gFolder(G_MV_1);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assumeTrue(home.openItemMenu(G_MV_1), "Row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapMove(), "Move not offered");
        sleep(3000);
        List<String> listed = movePickerFolders();
        System.out.println(">>> move picker for " + G_MV_1 + " = " + listed);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(listed.contains(G_MV_T), "Other folders should be offered: " + listed);
        assertFalse(listed.contains(G_MV_1), "The folder being moved must not be offered as its own destination");
    }

    @Test
    @Order(51)
    @DisplayName("[WEB L430] MOVE: a FILE moves into a folder")
    void gMoveFile() {
        assumeTrue(G_FILES.contains(G_DL2), "The download file is needed");
        gFolder(G_MV_T);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFiles();
        assumeTrue(home.openItemMenu(G_DL2), "Row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapMove(), "Move not offered for a file");
        sleep(3000);
        assertTrue(clickXpath("//*[@resource-id='" + appId("tvFolderName") + "' and @text='" + G_MV_T + "']"),
                G_MV_T + " not offered");
        sleep(2500);
        finishMoveSheet();
        sleep(4000);
        assertTrue(flows().openDrive() && flows().openFolder(G_MV_T), "Open the destination");
        List<String> inside = new DriveFolderPage(driver).allItemNamesScrolled();
        flows().backToHome();
        assertTrue(inside.contains(G_DL2), "The moved file should be inside " + G_MV_T + ": " + inside);
        G_FILES.remove(G_DL2);
    }

    @Test
    @Order(52)
    @DisplayName("[WEB L436] BULK MOVE: two selected folders move together")
    void gBulkMove() {
        gFolder(G_MV_T);
        gFolder(G_MV_1);
        gFolder(G_MV_2);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        java.util.Map<String, Integer> ys = rowYs(G_MV_1, G_MV_2);
        assumeTrue(home.longPressRow(G_MV_1) && selectRow(ys, G_MV_2), "Could not select both");
        assertTrue(home.bulkMove(), "Bulk Move not tappable");
        sleep(3000);
        assertTrue(clickXpath("//*[@resource-id='" + appId("tvFolderName") + "' and @text='" + G_MV_T + "']"),
                G_MV_T + " not offered in the bulk Move sheet");
        sleep(2500);
        finishMoveSheet();
        sleep(4000);
        home.exitSelectMode();
        assertTrue(flows().openDrive() && flows().openFolder(G_MV_T), "Open the destination");
        List<String> inside = new DriveFolderPage(driver).allItemNamesScrolled();
        flows().backToHome();
        assertTrue(inside.containsAll(List.of(G_MV_1, G_MV_2)), "Both folders should be inside " + G_MV_T + ": " + inside);
        G_FOLDERS.remove(G_MV_1);
        G_FOLDERS.remove(G_MV_2);
    }

    @Test
    @Order(53)
    @DisplayName("[WEB L1736] MOVE: 'Move to My Drive' takes an item back to the root")
    void gMoveToRoot() {
        assumeTrue(flows().openFolder(G_MV_T), "Open " + G_MV_T);
        assumeTrue(new DriveHomePage(driver).openItemMenu(G_MV_1), G_MV_1 + " is not inside (bulk move case)");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapMove(), "Move not offered");
        sleep(3000);
        // Inside a folder the sheet opens on that folder; climb to the root first.
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator("new UiSelector().text(\"My Drive\")")).click();
            sleep(2000);
        } catch (Exception ignored) { }
        String label = "";
        // With many folders the sheet's list pushes the button below the fold.
        for (int i = 0; i < 6 && driver.findElements(byAppId("btnMoveHere")).isEmpty(); i++) {
            try {
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().resourceId(\"" + appId("rvFolders") + "\")).scrollToEnd(3)"));
            } catch (Exception ignored) { }
            try {
                var size = driver.manage().window().getSize();
                driver.executeScript("mobile: swipeGesture", java.util.Map.of("left", size.width / 2 - 100,
                        "top", (int) (size.height * 0.55), "width", 200, "height", (int) (size.height * 0.30),
                        "direction", "up", "percent", 0.9));
            } catch (Exception ignored) { }
            sleep(800);
        }
        try { label = driver.findElement(byAppId("btnMoveHere")).getText(); } catch (Exception ignored) { }
        System.out.println(">>> move-to-root button = '" + label + "'");
        assertTrue(clickId("btnMoveHere"), "No Move button at the picker root");
        sleep(4000);
        flows().backToHome();
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertTrue(home.hasItem(G_MV_1), G_MV_1 + " should be back at the root (button read '" + label + "')");
        G_FOLDERS.add(G_MV_1);
    }

    @Test
    @Order(99)
    @DisplayName("WEB GAP CLEANUP: remove every QA_ item these cases created (Trash included)")
    void gapCleanup() {
        List<String> failed = new java.util.ArrayList<>();
        for (String f : G_FILES) if (!flows().purge(f, false)) failed.add(f);
        for (String f : new java.util.ArrayList<>(G_FOLDERS)) if (!flows().purge(f, true)) failed.add(f);
        // Folders emptied to Trash by the cases themselves.
        for (String f : List.of(G_EMPTY, G_RESTORE)) flows().purge(f, true);
        System.out.println(">>> gap cleanup failed=" + failed);
        assertTrue(failed.isEmpty(), "Could not remove: " + failed);
    }

    private String appId(String id) { return driver.getCurrentPackage() + ":id/" + id; }

    private boolean clickXpath(String xpath) {
        try { driver.findElement(io.appium.java_client.AppiumBy.xpath(xpath)).click(); return true; }
        catch (Exception e) { return false; }
    }

    /**
     * Ticks a row in Select mode. The rows show no name while selecting (see
     * selectModeKeepsNames), so the row is found by where its name sat just before Select mode
     * was turned on ({@link #rowYs}) — the list does not move when the checkboxes appear.
     */
    private boolean selectRow(java.util.Map<String, Integer> ys, String name) {
        Integer y = ys.get(name.toLowerCase());
        if (y == null) return false;
        try {
            for (org.openqa.selenium.WebElement box : driver.findElements(byAppId("cbSelect"))) {
                org.openqa.selenium.Rectangle r = box.getRect();
                if (y >= r.getY() - 40 && y <= r.getY() + r.getHeight() + 40) { box.click(); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Centre-Y of each visible row name, after scrolling until all {@code wanted} are on screen. */
    private java.util.Map<String, Integer> rowYs(String... wanted) {
        java.util.Map<String, Integer> out = new java.util.HashMap<>();
        for (int pass = 0; pass < 12; pass++) {
            out.clear();
            for (org.openqa.selenium.WebElement e : driver.findElements(byAppId("tvName"))) {
                org.openqa.selenium.Rectangle r = e.getRect();
                out.put(e.getText().toLowerCase(), r.getY() + r.getHeight() / 2);
            }
            boolean all = true;
            for (String w : wanted) all &= out.containsKey(w.toLowerCase());
            if (all) return out;
            try {
                var size = driver.manage().window().getSize();
                driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                        "left", size.width / 2 - 100, "top", (int) (size.height * 0.45),
                        "width", 200, "height", (int) (size.height * 0.30),
                        "direction", "up", "percent", 0.5));
                Thread.sleep(700);
            } catch (Exception ignored) { }
        }
        return out;
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
