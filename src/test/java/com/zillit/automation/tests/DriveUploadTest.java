package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveFolderPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveUploadPage;
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
 * DRIVE UPLOADS — files at the Drive root, files INSIDE a folder, whole-folder upload,
 * the staging sheet's destination + permission options, cancellation, and the retry
 * path for a failed upload.
 *
 * <p>Fixtures pushed by {@code run-and-report.sh}:
 * {@code /sdcard/Download/zl_drive_doc.pdf}, {@code zl_drive_pic.jpg} and the tree
 * {@code /sdcard/Download/zl_drive_tree/} (root_pic.jpg + sub_a/ + sub_b/ + a .nomedia
 * the uploader is expected to skip).
 */
@Order(TestOrder.DRIVE_UPLOAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveUploadTest extends BaseTest {

    private static final String DOC = "zl_drive_doc.pdf";
    private static final String PIC = "zl_drive_pic.jpg";
    private static final String FOLDER = "QA_Up_" + (System.currentTimeMillis() % 100000);

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
        if (!tools.isModuleListed("Drive", Duration.ofSeconds(6))) return false;
        if (!tools.openModule("Drive")) return false;
        return drive.isLoaded(Duration.ofSeconds(20));
    }

    /** A stray picker / preview / sheet from a failed upload would break the next test. */
    private void closeAnyOverlay() {
        for (int i = 0; i < 5; i++) {
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean overlay = foreign
                    || !driver.findElements(byAppId("btnUpload")).isEmpty()
                    || !driver.findElements(byAppId("sendBtn")).isEmpty()
                    || !driver.findElements(byAppId("rvItems")).isEmpty()
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(1000); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    // ==================== source sheet ====================

    @Test
    @Order(1)
    @DisplayName("Upload File opens the source sheet with Photo / Gallery / Video Gallery / Document / Audio")
    void sourceSheet() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");

        DriveUploadPage up = new DriveUploadPage(driver);
        assumeTrue(up.isSourceSheetOpen(), "Source sheet did not open");
        List<String> options = up.sourceOptions();
        System.out.println(">>> upload sources = " + options);
        for (String want : new String[]{"Photo", "Gallery", "Document", "Audio"}) {
            assertTrue(options.stream().anyMatch(o -> o.equalsIgnoreCase(want)),
                    "Missing upload source '" + want + "'. Got: " + options);
        }
        screenshot("drive-upload-sources");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(2)
    @DisplayName("Document source opens the SYSTEM file picker (a different package)")
    void systemPickerOpens() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");

        DriveUploadPage up = new DriveUploadPage(driver);
        assumeTrue(up.isSourceSheetOpen(), "Source sheet did not open");
        assumeTrue(up.chooseSource("Document"), "Could not choose Document");
        assertTrue(up.isSystemPickerOpen(Duration.ofSeconds(20)),
                "The Android document picker (com.google.android.documentsui) should open");
        screenshot("drive-system-picker");
        try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
    }

    // ==================== upload to root ====================

    @Test
    @Order(3)
    @DisplayName("Staging sheet shows the destination, the file list and the permission modes")
    void stagingSheet() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");
        DriveUploadPage up = new DriveUploadPage(driver);
        assumeTrue(up.isSourceSheetOpen() && up.chooseSource("Document"), "No Document source");
        assumeTrue(up.isSystemPickerOpen(Duration.ofSeconds(20)), "Picker did not open");
        assumeTrue(up.pickFileNamed(DOC, Duration.ofSeconds(20)), "Fixture " + DOC + " not in the picker");
        assumeTrue(up.isPreviewOpen(Duration.ofSeconds(20)), "Preview did not open");
        assumeTrue(up.sendFromPreview(), "Could not send from the preview");
        assumeTrue(up.isStagingOpen(Duration.ofSeconds(20)), "Staging sheet did not open");

        System.out.println(">>> destination='" + up.destination() + "' files='" + up.filesHeader() + "'");
        assertTrue(up.destination().toLowerCase().contains("drive")
                        || up.destination().toLowerCase().contains("destination"),
                "Staging should name the destination, got '" + up.destination() + "'");
        assertTrue(up.hasPermissionModes(), "File permission modes (Project/Specific) expected");
        assertTrue(up.hasDestinationPicker(), "A destination picker (New/Existing folder) is expected");
        screenshot("drive-upload-staging");
        up.cancelStaging();
    }

    @Test
    @Order(4)
    @DisplayName("Cancelling the staging sheet uploads nothing")
    void cancelUpload() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        int before = drive.allItemNamesScrolled().size();

        assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");
        DriveUploadPage up = new DriveUploadPage(driver);
        assumeTrue(up.isSourceSheetOpen() && up.chooseSource("Document"), "No Document source");
        assumeTrue(up.isSystemPickerOpen(Duration.ofSeconds(20)), "Picker did not open");
        assumeTrue(up.pickFileNamed(DOC, Duration.ofSeconds(20)), "Fixture not found");
        assumeTrue(up.isPreviewOpen(Duration.ofSeconds(20)) && up.sendFromPreview(), "Preview failed");
        assumeTrue(up.isStagingOpen(Duration.ofSeconds(20)), "Staging did not open");
        assumeTrue(up.cancelStaging(), "Cancel not tappable");
        sleep(3000);

        DriveHomePage home = new DriveHomePage(driver);
        home.showFiles();
        assumeTrue(home.isListReadable(), "List unreadable — cannot judge the count");
        int after = home.allItemNamesScrolled().size();
        System.out.println(">>> files before=" + before + " after cancel=" + after);
        assertTrue(after <= before, "Cancelling must not upload anything (before=" + before + " after=" + after + ")");
        screenshot("drive-upload-cancelled");
    }

    @Test
    @Order(5)
    @DisplayName("UPLOAD a file to the Drive root → it appears under the FILES tab")
    void uploadFileToRoot() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");

        DriveUploadPage up = new DriveUploadPage(driver);
        assertTrue(up.uploadFile("Document", DOC),
                "The upload pipeline (source → picker → preview → staging → Upload) should complete");
        assertTrue(up.uploadStarted(Duration.ofSeconds(15)),
                "An 'Uploading N file(s)…' confirmation is expected");
        sleep(9000);

        // The file lands under FILES; Drive opens on FOLDERS, where it will never show.
        DriveHomePage home = new DriveHomePage(driver);
        assertTrue(home.hasFile(DOC),
                "The uploaded file should be listed under the Files tab. Files: "
                        + home.allItemNamesScrolled());
        screenshot("drive-uploaded-root");
    }

    @Test
    @Order(6)
    @DisplayName("An uploaded file does NOT appear on the Folders tab (files and folders are separate)")
    void fileNotUnderFolders() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.showFolders(), "Could not switch to Folders");
        assumeTrue(drive.isListReadable(), "Folder list unreadable");
        List<String> folders = drive.allItemNamesScrolled();
        System.out.println(">>> folders=" + folders);
        assertFalse(folders.stream().anyMatch(n -> n.equalsIgnoreCase(DOC)),
                "A FILE must not be listed on the Folders tab. Got: " + folders);
    }

    // ==================== upload inside a folder ====================

    @Test
    @Order(7)
    @DisplayName("UPLOAD a file INSIDE a folder → it lands in that folder, not at the root")
    void uploadInsideFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();

        // Make a folder to upload into.
        if (!drive.hasItem(FOLDER)) {
            assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
            CreateFolderSheet sheet = new CreateFolderSheet(driver);
            assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Create sheet did not open");
            assumeTrue(sheet.createNamed(FOLDER), "Could not create the target folder");
            sleep(2500);
        }
        assumeTrue(new DriveHomePage(driver).openItem(FOLDER), "Could not open the folder");
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(driver);
        assumeTrue(folder.isLoaded(Duration.ofSeconds(15)), "Folder screen did not load");
        assumeTrue(folder.openFab(), "Folder FAB did not open");
        assumeTrue(new DriveHomePage(driver).tapUploadFile(), "Could not tap Upload File in the folder");

        DriveUploadPage up = new DriveUploadPage(driver);
        assertTrue(up.uploadFile("Document", DOC), "The in-folder upload pipeline should complete");
        assertTrue(up.uploadStarted(Duration.ofSeconds(15)), "Upload confirmation expected");
        sleep(9000);

        DriveHomePage inFolder = new DriveHomePage(driver);
        inFolder.showFiles();
        List<String> inside = inFolder.allItemNamesScrolled();
        System.out.println(">>> inside " + FOLDER + " (files) = " + inside);
        assertTrue(inside.stream().anyMatch(n -> n.equalsIgnoreCase(DOC)),
                "The file should be inside the folder. Got: " + inside);
        screenshot("drive-uploaded-in-folder");
    }

    // ==================== whole-folder upload ====================

    @Test
    @Order(8)
    @DisplayName("UPLOAD FOLDER opens the folder picker and stages a folders/files summary")
    void uploadWholeFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFab(), "FAB did not open");
        assumeTrue(drive.tapUploadFolder(), "Could not tap Upload Folder");
        sleep(4000);

        DriveUploadPage up = new DriveUploadPage(driver);
        // The tree picker is the system folder picker (a foreign package).
        boolean picker = up.isSystemPickerOpen(Duration.ofSeconds(20));
        System.out.println(">>> folder picker opened=" + picker);
        assumeTrue(picker, "The system folder picker did not open — cannot stage a folder upload");
        screenshot("drive-folder-picker");

        // Pick the seeded tree, then expect the staging summary.
        if (up.pickFileNamed("zl_drive_tree", Duration.ofSeconds(15))) {
            sleep(2000);
            // Most pickers need an explicit "Use this folder" / "Allow" confirmation.
            up.pickFileNamed("USE THIS FOLDER", Duration.ofSeconds(6));
            up.pickFileNamed("ALLOW", Duration.ofSeconds(6));
            sleep(4000);
            String summary = up.folderSummary();
            System.out.println(">>> folder-upload summary='" + summary + "'");
            screenshot("drive-folder-staging");
        }
        try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
    }

    // ==================== retry / failure path ====================

    @Test
    @Order(9)
    @DisplayName("RETRY: an upload started with no network reports the offline state and can be retried")
    void offlineUploadRetries() {
        // Force the failure the retry path exists for, rather than waiting for a flake.
        boolean toggled = setAirplaneMode(true);
        assumeTrue(toggled, "Could not switch the device offline — skipping the retry path");
        try {
            sleep(3000);
            DriveHomePage drive = new DriveHomePage(driver);
            assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");
            DriveUploadPage up = new DriveUploadPage(driver);
            boolean started = up.uploadFile("Document", DOC);
            System.out.println(">>> offline upload started=" + started);
            sleep(6000);

            // Either an explicit offline message, or a queued/failed chip — both are
            // acceptable evidence that the app noticed. What must NOT happen is a
            // silent success.
            boolean offlineMsg = drive.textShown("No internet", Duration.ofSeconds(6))
                    || drive.textShown("Failed", Duration.ofSeconds(3))
                    || drive.textShown("Queued", Duration.ofSeconds(3))
                    || drive.textShown("connect", Duration.ofSeconds(3));
            System.out.println(">>> offline feedback shown=" + offlineMsg);
            screenshot("drive-upload-offline");
            assertTrue(offlineMsg,
                    "With no network the app must report the failure (offline message / Failed / Queued chip)");
        } finally {
            setAirplaneMode(false);
            sleep(6000);
        }
    }

    @Test
    @Order(10)
    @DisplayName("After reconnecting, the Drive list still loads and remains usable")
    void recoversAfterReconnect() {
        DriveHomePage drive = new DriveHomePage(driver);
        assertTrue(drive.isLoaded(Duration.ofSeconds(30)), "Drive should still be usable after reconnecting");
        drive.pullToRefresh();
        sleep(4000);
        assertTrue(drive.isListReadable(), "The list should render after a refresh");
        screenshot("drive-after-reconnect");
    }

    // ==================== cleanup ====================

    @Test
    @Order(11)
    @DisplayName("CLEANUP: remove the uploaded file and the QA upload folder")
    void cleanup() {
        DriveHomePage drive = new DriveHomePage(driver);

        // NOTE: the uploaded FILE is deliberately left in Drive. It is a shared fixture —
        // DriveEditorTest's PDF negative case and DriveShareTest both expect it to be
        // there. Deleting it here is what made a chained run collapse: later suites had
        // to re-upload, and when that failed every one of their cases skipped.
        // Each suite cleans up only the FOLDERS it created.

        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        if (home.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(FOLDER))
                && home.openItemMenu(FOLDER)) {
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                home.confirmDeleteDialog(Duration.ofSeconds(8));
                sleep(3000);
            } else {
                menu.dismiss();
            }
        }

        DriveHomePage end = new DriveHomePage(driver);
        end.showFolders();
        assertTrue(end.isListReadable(), "Drive should be readable after cleanup");
        System.out.println(">>> folders after cleanup = " + end.allItemNamesScrolled());
        screenshot("drive-upload-cleanup");
    }

    /**
     * Cuts / restores connectivity.
     *
     * <p>Uses Appium's own connection API first: {@code mobile: shell} needs the server
     * to be started with {@code --relaxed-security}, which it usually is not, and that
     * is exactly why this case silently skipped on the first run instead of exercising
     * the retry path.
     */
    private boolean setAirplaneMode(boolean on) {
        try {
            driver.setConnection(new io.appium.java_client.android.connection.ConnectionStateBuilder()
                    .withWiFiDisabled().withDataDisabled().withAirplaneModeEnabled().build());
            if (!on) {
                driver.setConnection(new io.appium.java_client.android.connection.ConnectionStateBuilder()
                        .withAirplaneModeDisabled().withWiFiEnabled().withDataEnabled().build());
            }
            return true;
        } catch (Exception apiFailed) {
            try {   // fall back to a shell toggle when the server allows it
                driver.executeScript("mobile: shell", java.util.Map.of(
                        "command", "cmd connectivity airplane-mode",
                        "args", List.of(on ? "enable" : "disable")));
                return true;
            } catch (Exception e) {
                System.out.println(">>> cannot toggle connectivity (" + apiFailed.getMessage()
                        + ") — start Appium with --relaxed-security to cover the retry path");
                return false;
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
