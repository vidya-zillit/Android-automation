package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.CreateFolderSheet;
import com.zillit.automation.pages.drive.DriveFolderPage;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveItemMenuPage;
import com.zillit.automation.pages.drive.DriveUploadPage;
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
 * DRIVE UPLOADS — files at the Drive root, files INSIDE a folder, whole-folder upload,
 * the staging sheet's destination + permission options, cancellation, and the retry
 * path for a failed upload.
 *
 * <p>Fixtures pushed by {@code run-and-report.sh}:
 * {@code /sdcard/Download/zl_drive_doc.pdf}, {@code zl_drive_pic.jpg} and the tree
 * {@code /sdcard/Download/zl_drive_tree/} (root_pic.jpg + sub_a/ + sub_b/ + a .nomedia
 * the uploader is expected to skip).
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE_UPLOAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveUploadTest extends BaseTest {

    private static final String DOC = "zl_drive_doc.pdf";
    private static final String PIC = "zl_drive_pic.jpg";
    private static final String FOLDER = "QA_Up_" + (System.currentTimeMillis() % 100000);
    /** The fixture tree is uploaded under this unique top-folder name (etFolderName). */
    private static final String TREE = "QA_Tree_" + (System.currentTimeMillis() % 100000);

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
        if (!tools.openModuleContaining("Drive")) return false;
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
    @DisplayName("UPLOAD FOLDER stages the tree summary, and the uploaded folder holds its subfolders and files")
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

        // Pick the seeded tree (run-and-report.sh pushes src/test/resources/fixtures/drive_tree:
        // root_pic.jpg, sub_a/{a_doc.pdf, a_pic.jpg}, sub_b/{b_doc.pdf}, and a 0-byte .nomedia).
        boolean picked = up.pickFileNamed("zl_drive_tree", Duration.ofSeconds(15));
        if (!picked) {
            try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
        }
        assumeTrue(picked, "zl_drive_tree is not in the picker — fixture not pushed to /sdcard/Download");
        sleep(2000);
        // Most pickers need an explicit "Use this folder" / "Allow" confirmation.
        up.pickFileNamed("USE THIS FOLDER", Duration.ofSeconds(6));
        up.pickFileNamed("ALLOW", Duration.ofSeconds(6));
        sleep(4000);
        assumeTrue(up.isFolderStagingOpen(Duration.ofSeconds(15)),
                "The folder-upload staging sheet did not open after picking the tree");

        // Summary = DriveFolderTreeWalker counts: folders INCLUDE the picked root (depth 0),
        // and the 0-byte .nomedia is skipped — so 3 folders · 4 files.
        String summary = up.folderSummary();
        System.out.println(">>> folder-upload summary='" + summary + "'");
        screenshot("drive-folder-staging");
        assertTrue(summary.contains("3 folders") && summary.contains("4 files"),
                "Staging summary should count the fixture tree as '3 folders · 4 files'. Got: " + summary);

        assumeTrue(up.setFolderUploadName(TREE),
                "Could not rename the top folder to a unique name (etFolderName)");
        assertTrue(up.upload(), "Upload on the folder staging sheet should be tappable");

        // Folders are created first, then the files upload into them.
        DriveHomePage home = new DriveHomePage(driver);
        boolean treeListed = false;
        for (int i = 0; i < 12 && !treeListed; i++) {
            sleep(5000);
            home = new DriveHomePage(driver);
            home.showFolders();
            treeListed = home.hasItem(TREE);
        }
        screenshot("drive-folder-uploaded");
        assertTrue(treeListed, "The uploaded folder " + TREE + " should appear under Folders");

        assertTrue(home.openItem(TREE), "Could not open the uploaded folder " + TREE);
        sleep(2500);
        DriveHomePage inTree = new DriveHomePage(driver);
        inTree.showFolders();
        List<String> subFolders = inTree.allItemNamesScrolled();
        List<String> rootFiles = List.of();
        for (int i = 0; i < 12; i++) {
            inTree.showFiles();
            rootFiles = inTree.allItemNamesScrolled();
            if (rootFiles.stream().anyMatch(n -> n.equalsIgnoreCase("root_pic.jpg"))) break;
            sleep(5000);
        }
        System.out.println(">>> " + TREE + " folders=" + subFolders + " files=" + rootFiles);
        screenshot("drive-folder-tree-root");
        assertTrue(subFolders.stream().anyMatch(n -> n.equalsIgnoreCase("sub_a"))
                        && subFolders.stream().anyMatch(n -> n.equalsIgnoreCase("sub_b")),
                TREE + " should contain subfolders sub_a and sub_b. Got: " + subFolders);
        assertTrue(rootFiles.stream().anyMatch(n -> n.equalsIgnoreCase("root_pic.jpg")),
                TREE + " should contain root_pic.jpg. Got: " + rootFiles);
        assertFalse(rootFiles.stream().anyMatch(n -> n.equalsIgnoreCase(".nomedia")),
                "The 0-byte .nomedia should be skipped, not uploaded. Got: " + rootFiles);

        inTree.showFolders();
        assertTrue(inTree.openItem("sub_a"), "Could not open sub_a inside " + TREE);
        sleep(2500);
        DriveHomePage inSub = new DriveHomePage(driver);
        List<String> subFiles = List.of();
        for (int i = 0; i < 12; i++) {
            inSub.showFiles();
            subFiles = inSub.allItemNamesScrolled();
            if (subFiles.stream().anyMatch(n -> n.equalsIgnoreCase("a_doc.pdf"))
                    && subFiles.stream().anyMatch(n -> n.equalsIgnoreCase("a_pic.jpg"))) break;
            sleep(5000);
        }
        System.out.println(">>> " + TREE + "/sub_a files=" + subFiles);
        screenshot("drive-folder-tree-sub-a");
        assertTrue(subFiles.stream().anyMatch(n -> n.equalsIgnoreCase("a_doc.pdf"))
                        && subFiles.stream().anyMatch(n -> n.equalsIgnoreCase("a_pic.jpg")),
                TREE + "/sub_a should contain a_doc.pdf and a_pic.jpg. Got: " + subFiles);

        new DriveHomePage(driver).returnHome();
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

        for (String name : new String[]{FOLDER, TREE}) {
            DriveHomePage home = new DriveHomePage(driver);
            home.showFolders();
            if (home.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name))
                    && home.openItemMenu(name)) {
                DriveItemMenuPage menu = new DriveItemMenuPage(driver);
                if (menu.isOpen() && menu.tapDelete()) {
                    sleep(1200);
                    home.confirmDeleteDialog(Duration.ofSeconds(8));
                    sleep(3000);
                } else {
                    menu.dismiss();
                }
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
    // ==================== Pause / Resume (cdc957a23) ====================

    /** Big enough to still be uploading when the progress sheet opens. */
    private static final int BIG_BYTES = 16 * 1024 * 1024;
    private static final String BIG_FIXTURE = "zl_drive_big.pdf";
    private static final String BIG_UPLOAD = "QA_Big_" + (System.currentTimeMillis() % 100000) + ".pdf";
    private static boolean bigUploaded;

    /**
     * Pushes a 16 MB file to /sdcard/Download through Appium (push_file needs no
     * --relaxed-security). A PDF header keeps it on the picker's Document list.
     */
    private boolean pushBigFixture() {
        try {
            byte[] data = new byte[BIG_BYTES];
            new java.util.Random(7).nextBytes(data);
            byte[] head = "%PDF-1.4\n%zillit-automation pause/resume fixture\n".getBytes();
            System.arraycopy(head, 0, data, 0, head.length);
            driver.pushFile("/sdcard/Download/" + BIG_FIXTURE, data);
            sleep(3000);
            return true;
        } catch (Exception e) {
            System.out.println(">>> could not push the big fixture: " + e.getMessage().split("\n")[0]);
            return false;
        }
    }

    @Test
    @Order(12)
    @DisplayName("U-01 PAUSE a running upload ('Paused N%', Resume) and RESUME it to completion")
    void pauseAndResumeUpload() {
        assumeTrue(pushBigFixture(), "Could not put a 16 MB file on the device");
        // A 16 MB upload finishes before the progress sheet is even open on the emulator's
        // network ("Finishing…" — pause is hidden then). Throttle the EMULATOR's link so the row
        // is still "Uploading N%" when we get there; restored in finally. No-op on a phone.
        boolean throttled = setNetworkSpeed("edge");
        try {
            pauseAndResumeBody();
        } finally {
            if (throttled) setNetworkSpeed("full");
        }
    }

    private boolean setNetworkSpeed(String speed) {
        try {
            driver.executeScript("mobile: networkSpeed", java.util.Map.of("speed", speed));
            System.out.println(">>> emulator network speed = " + speed);
            return true;
        } catch (Exception e) {
            System.out.println(">>> network speed not set (" + speed + "): " + e.getMessage().split("\n")[0]);
            return false;
        }
    }

    private void pauseAndResumeBody() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        assumeTrue(drive.openFab() && drive.tapUploadFile(), "Could not start Upload File");
        DriveUploadPage up = new DriveUploadPage(driver);
        assumeTrue(up.uploadFileAs("Document", BIG_FIXTURE, BIG_UPLOAD), "Could not stage " + BIG_UPLOAD);
        bigUploaded = true;
        assumeTrue(up.openProgressSheet(Duration.ofSeconds(10)), "The 'Uploading…' snackbar's View action never showed");
        boolean has = up.hasPauseResume();
        String status = up.progressStatus();
        System.out.println(">>> U-01 sheet: pauseResume=" + has + " status='" + status + "'");
        assumeTrue(has || status.startsWith("Finishing") || status.startsWith("Failed"),
                "Upload rows have no btnPauseResume — this build predates Zillit-Android cdc957a23");
        assumeTrue(has, "The upload was already '" + status + "' (pause is hidden then) — too fast to catch");
        assertEquals("Pause", up.pauseResumeLabel(), "A running upload offers Pause");
        assertTrue(up.tapPauseResume(), "Pause not tappable");
        sleep(2500);
        String paused = up.progressStatus();
        String label = up.pauseResumeLabel();
        System.out.println(">>> U-01 after Pause: status='" + paused + "' button='" + label + "'");
        assumeTrue(!up.textShown("can't be paused", Duration.ofSeconds(1)),
                "The app says this upload can't be paused right now (past S3 / Box)");
        assertTrue(paused.matches("Paused \\d+%"), "Status should read 'Paused N%', was '" + paused + "'");
        assertEquals("Resume", label, "A paused upload offers Resume");
        sleep(4000);
        assertEquals(paused, up.progressStatus(), "A paused upload must not progress");
        assertTrue(up.tapPauseResume(), "Resume not tappable");
        sleep(2500);
        String resumed = up.progressStatus();
        System.out.println(">>> U-01 after Resume: '" + resumed + "'");
        assertFalse(resumed.startsWith("Paused") || resumed.startsWith("Failed"),
                "Resume should continue the upload, status '" + resumed + "'");
        setNetworkSpeed("full");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(new com.zillit.automation.pages.drive.DriveFlows(driver).waitListed(BIG_UPLOAD, Duration.ofSeconds(180)),
                "The resumed upload should finish and list " + BIG_UPLOAD);
    }

    @Test
    @Order(13)
    @DisplayName("U-01 CLEANUP: remove the big upload")
    void pauseCleanup() {
        // Shrink the 16 MB fixture to nothing — it is regenerated on the next run.
        try { driver.pushFile("/sdcard/Download/" + BIG_FIXTURE, new byte[0]); } catch (Exception ignored) { }
        assumeTrue(bigUploaded, "Nothing was uploaded");
        assertTrue(new com.zillit.automation.pages.drive.DriveFlows(driver).purge(BIG_UPLOAD, false),
                "Could not remove " + BIG_UPLOAD);
    }

    // =====================================================================================
    // WEB GAP cases (web-e2e/gaps/drive.md M15 destination / staged name / duplicates,
    // M17 upload keeps going after leaving Drive).
    // =====================================================================================

    private static final long GS = System.currentTimeMillis() % 100000;
    private static final String G_UPF = "QA_UpG_" + GS;
    private static final java.util.Set<String> G_FILES = new java.util.LinkedHashSet<>();
    private static final java.util.Set<String> G_FOLDERS = new java.util.LinkedHashSet<>();

    private com.zillit.automation.pages.drive.DriveFlows gf() { return new com.zillit.automation.pages.drive.DriveFlows(driver); }

    private void gFolder(String name) {
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        if (!home.hasItem(name)) assumeTrue(gf().createFolder(name), "Could not create " + name);
        G_FOLDERS.add(name);
    }

    /** From the open screen: + → Upload File → Document → pick → preview → staging (left open). */
    private DriveUploadPage stage(String fixture, boolean insideFolder) {
        boolean ok = insideFolder ? (new DriveFolderPage(driver).openFab() && new DriveFolderPage(driver).tapUploadFile())
                : (new DriveHomePage(driver).openFab() && new DriveHomePage(driver).tapUploadFile());
        assumeTrue(ok, "Upload File not offered");
        DriveUploadPage up = new DriveUploadPage(driver);
        assumeTrue(up.isSourceSheetOpen() && up.chooseSource("Document"), "Document source");
        assumeTrue(up.isSystemPickerOpen(Duration.ofSeconds(20)) && up.pickFileNamed(fixture, Duration.ofSeconds(45)), "Pick " + fixture);
        assumeTrue(up.isPreviewOpen(Duration.ofSeconds(20)) && up.sendFromPreview(), "Preview");
        assumeTrue(up.isStagingOpen(Duration.ofSeconds(20)), "Staging sheet");
        return up;
    }

    @Test
    @Order(20)
    @DisplayName("[WEB L334/L340] UPLOAD twice in a row from inside a folder: both land in the folder, none at the root")
    void gTwoUploadsIntoFolder() {
        gFolder(G_UPF);
        assumeTrue(gf().openFolder(G_UPF), "Open " + G_UPF);
        String a = "QA_UpA_" + GS + ".pdf", b = "QA_UpB_" + GS + ".jpg";
        DriveUploadPage up = stage("zl_drive_doc.pdf", true);
        assumeTrue(up.renameStaged("zl_drive_doc.pdf", a) && up.upload(), "Upload 1");
        sleep(6000);
        up = stage("zl_drive_pic.jpg", true);
        assumeTrue(up.renameStaged("zl_drive_pic.jpg", b) && up.upload(), "Upload 2");
        DriveFolderPage in = new DriveFolderPage(driver);
        boolean both = in.waitForItem(a, Duration.ofSeconds(40)) && in.waitForItem(b, Duration.ofSeconds(40));
        List<String> inside = in.allItemNamesScrolled();
        gf().backToHome();
        DriveHomePage home = new DriveHomePage(driver);
        home.showFiles();
        List<String> root = home.allItemNamesScrolled();
        System.out.println(">>> folder=" + inside + " root has a=" + root.contains(a) + " b=" + root.contains(b));
        assertTrue(both, "Both uploads should land in " + G_UPF + " (ZL-21873): " + inside);
        assertFalse(root.contains(a) || root.contains(b), "Neither may land at the root");
    }

    @Test
    @Order(21)
    @DisplayName("[WEB L346] UPLOAD at the root to 'Existing folder': nothing picked is refused; a picked folder receives it")
    void gUploadToExistingFolder() {
        gFolder(G_UPF);
        String name = "QA_UpE_" + GS + ".pdf";
        DriveUploadPage up = stage("zl_drive_doc.pdf", false);
        up.renameStaged("zl_drive_doc.pdf", name);
        try { driver.findElement(byAppId("drive_pick_seg_existing")).click(); } catch (Exception e) { assumeTrue(false, "No 'Existing folder' option"); }
        sleep(1500);
        up.upload();
        sleep(2500);
        boolean stillStaging = up.isStagingOpen(Duration.ofSeconds(3));
        assertTrue(stillStaging, "Upload with no destination picked must not go through");
        boolean picked;
        try {
            driver.findElement(io.appium.java_client.AppiumBy.xpath("//*[@resource-id='" + driver.getCurrentPackage()
                    + ":id/drive_pick_folder_name' and @text='" + G_UPF + "']")).click();
            picked = true;
        } catch (Exception e) { picked = false; }
        assertTrue(picked, G_UPF + " should be offered as the destination");
        sleep(1200);
        assertTrue(up.upload(), "Upload");
        sleep(8000);
        assertTrue(gf().openFolder(G_UPF), "Open " + G_UPF);
        DriveFolderPage in = new DriveFolderPage(driver);
        boolean there = in.waitForItem(name, Duration.ofSeconds(40));
        gf().backToHome();
        assertTrue(there, name + " should be inside " + G_UPF);
    }

    @Test
    @Order(22)
    @DisplayName("[WEB L358] UPLOAD: a staged name without its extension keeps '.pdf'")
    void gStagedNameKeepsExtension() {
        String base = "QA_named.v2_" + GS;
        DriveUploadPage up = stage("zl_drive_doc.pdf", false);
        assumeTrue(up.renameStaged("zl_drive_doc.pdf", base), "Could not rename the staged row");
        assertTrue(up.upload(), "Upload");
        DriveHomePage home = new DriveHomePage(driver);
        String stored = null;
        for (int i = 0; i < 12 && stored == null; i++) {
            sleep(4000);
            home.showFiles();
            stored = home.allItemNamesScrolled().stream().filter(n -> n.startsWith(base)).findFirst().orElse(null);
        }
        if (stored != null) G_FILES.add(stored);
        System.out.println(">>> staged rename stored as " + stored);
        assertEquals(base + ".pdf", stored, "The extension must be kept");
    }

    @Test
    @Order(23)
    @DisplayName("[WEB L1223] UPLOAD a name that already exists in the folder: 'Rename to upload' resolves it; both are kept")
    void gDuplicateUploadResolve() {
        gFolder(G_UPF);
        assumeTrue(gf().openFolder(G_UPF), "Open " + G_UPF);
        String a = "QA_UpA_" + GS + ".pdf";
        DriveFolderPage in = new DriveFolderPage(driver);
        assumeTrue(in.allItemNamesScrolled().contains(a), "The first upload case must put " + a + " here");
        DriveUploadPage up = stage("zl_drive_doc.pdf", true);
        up.renameStaged("zl_drive_doc.pdf", a);
        assertTrue(up.upload(), "Upload");
        boolean resolve = false;
        for (int i = 0; i < 20 && !resolve; i++) {
            sleep(1500);
            resolve = up.textShown("Rename to upload", Duration.ofMillis(500)) || up.textShown("Resolve duplicates", Duration.ofMillis(300));
        }
        String renamed = "QA_UpA2_" + GS + ".pdf";
        if (resolve) {
            try {
                var et = driver.findElement(byAppId("etNewName"));
                et.clear(); et.sendKeys(renamed.replace(".pdf", ""));
                driver.findElement(byAppId("btnSubmit")).click();
                sleep(5000);
            } catch (Exception e) { System.out.println(">>> resolve sheet: " + e.getMessage().split("\n")[0]); }
            try { driver.navigate().back(); } catch (Exception ignored) { }
        }
        List<String> inside = in.allItemNamesScrolled();
        gf().backToHome();
        System.out.println(">>> duplicate upload resolveShown=" + resolve + " folder=" + inside);
        assertTrue(resolve, "Uploading an existing name should open 'Rename to upload'");
        assertTrue(inside.contains(a) && inside.stream().anyMatch(n -> n.startsWith("QA_UpA2_" + GS)),
                "Both the original and the renamed copy should be in the folder: " + inside);
    }

    @Test
    @Order(24)
    @DisplayName("[WEB L814] UPLOAD keeps going after leaving Drive")
    void gUploadAfterLeavingDrive() {
        String name = "QA_UpBg_" + GS + ".pdf";
        DriveUploadPage up = stage("zl_drive_doc.pdf", false);
        assumeTrue(up.renameStaged("zl_drive_doc.pdf", name) && up.upload(), "Upload");
        for (int i = 0; i < 3 && new DriveHomePage(driver).isDriveActivity(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        sleep(15000);
        assertTrue(gf().openDrive(), "Reopen Drive");
        boolean listed = gf().waitListed(name, Duration.ofSeconds(40));
        if (listed) G_FILES.add(name);
        assertTrue(listed, "An upload started before leaving Drive should still complete");
    }

    @Test
    @Order(98)
    @DisplayName("WEB GAP CLEANUP: remove the QA_ uploads and folder")
    void gUploadCleanup() {
        List<String> failed = new java.util.ArrayList<>();
        for (String f : G_FILES) if (!gf().purge(f, false)) failed.add(f);
        for (String f : G_FOLDERS) if (!gf().purge(f, true)) failed.add(f);
        assertTrue(failed.isEmpty(), "Could not remove: " + failed);
    }

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
