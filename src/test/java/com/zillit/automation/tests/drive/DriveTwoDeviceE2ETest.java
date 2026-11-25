package com.zillit.automation.tests.drive;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.CreateFolderSheet;
import com.zillit.automation.pages.drive.DriveEditorPage;
import com.zillit.automation.pages.drive.DriveFileContent;
import com.zillit.automation.pages.drive.DriveFileDetailPage;
import com.zillit.automation.pages.drive.DriveFlows;
import com.zillit.automation.pages.drive.DriveFolderPage;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveItemMenuPage;
import com.zillit.automation.pages.drive.DriveSharePage;
import com.zillit.automation.pages.drive.DriveUploadPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE — the full SYMMETRIC two-device journey.
 *
 * <p>Every other Drive suite is one-sided: the owner suites prove a grant was *saved*,
 * the recipient suite proves one grant *arrived*. This one runs the whole loop in BOTH
 * directions, so nothing is taken on trust:
 *
 * <pre>
 *   A uploads → A shares with B → B sees it → B's rights match the grant
 *   A upgrades B to editor      → B's menu gains the editor actions
 *   A shares a FOLDER           → B opens it and uploads INTO it → A sees B's file
 *   B uploads → B shares with A → A sees it   (the mirror image)
 *   A edits   → B sees the edit  ·  B edits → A sees the edit
 *   A revokes → the item disappears from B
 * </pre>
 *
 * <p>Run with both devices attached, signed in as DIFFERENT users on the SAME project:
 * <pre>
 *   ZILLIT_SENDER_UDID=&lt;A&gt; ZILLIT_RECEIVER_UDID=&lt;B&gt; ./run-and-report.sh --drive-e2e
 * </pre>
 *
 * <p>{@code ZILLIT_USER_A} / {@code ZILLIT_USER_B} are the two display names as they
 * appear in the share picker (defaults: "Vidya Sagar" / "Vidya Pixel").
 *
 * <p>Drives two drivers itself, so it does not extend {@code BaseTest}.
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE_E2E)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveTwoDeviceE2ETest {

    private static final long STAMP = System.currentTimeMillis() % 1000000;
    /** Uploaded by A, shared to B. */
    private static final String FILE_A = "zl_drive_book.xlsx";
    /** Uploaded by B, shared to A. */
    private static final String FILE_B = "zl_drive_doc.docx";
    private static final String FOLDER_A = "QA_E2E_A_" + STAMP;
    private static final String FOLDER_B = "QA_E2E_B_" + STAMP;

    private static final String USER_A = System.getenv().getOrDefault("ZILLIT_USER_A", "Vidya Testing");
    private static final String USER_B = System.getenv().getOrDefault("ZILLIT_USER_B", "Vidya Pixel");

    /**
     * How long a share/edit may take to reach the other device.
     *
     * <p>90s measured: {@code DriveFolderShareTest} saw a folder land on the second
     * device inside that window, while 60s was not always enough — a shared FOLDER
     * arrives noticeably slower than a shared file. 120s leaves headroom without letting
     * a genuinely lost share pass as "still syncing".
     */
    private static final Duration SYNC = Duration.ofSeconds(120);

    private AndroidDriver a;   // device A
    private AndroidDriver b;   // device B

    @BeforeAll
    void startBoth() {
        String udidA = System.getenv().getOrDefault("ZILLIT_SENDER_UDID", Config.get("zillit.udid"));
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is required — this suite needs TWO devices");
        assumeTrue(!udidA.equalsIgnoreCase(udidB), "The two devices must be different");
        a = DriverFactory.create(udidA, 8200);
        b = DriverFactory.create(udidB, 8210);
        System.out.println(">>> A=" + udidA + " (" + USER_A + ")   B=" + udidB + " (" + USER_B + ")");
    }

    @AfterAll
    void stopBoth() {
        for (AndroidDriver d : new AndroidDriver[]{a, b}) {
            if (d == null) continue;
            try { new DriveEditorPage(d).forceExit(); } catch (Exception ignored) { }
            try { d.quit(); } catch (Exception ignored) { }
        }
    }

    // ---------------- shared navigation ----------------

    private boolean openDrive(AndroidDriver d, String label) {
        DriveHomePage drive = new DriveHomePage(d);
        try { new DriveEditorPage(d).forceExit(); } catch (Exception ignored) { }
        for (int i = 0; i < 6; i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            if (!d.findElements(AppiumBy.id(d.getCurrentPackage() + ":id/bottomNavigation")).isEmpty()) break;
            try { d.navigate().back(); Thread.sleep(900); } catch (Exception ignored) { }
        }
        if (!new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(25))) {
            ProjectListPage list = new ProjectListPage(d);
            List<String> names = list.visibleProjectNames();
            if (!names.isEmpty()) list.tapProject(names.get(0));
            if (!new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(60))) return false;
        }
        ToolsPage tools = new ToolsPage(d);
        tools.openToolsTab();
        if (!tools.isLoaded(Duration.ofSeconds(15))) return false;
        tools.searchTools("Drive");
        sleep(1500);
        return tools.isModuleListed("Drive", Duration.ofSeconds(6))
                && tools.openModuleContaining("Drive")
                && drive.isLoaded(Duration.ofSeconds(20));
    }

    private boolean uploadFile(AndroidDriver d, String fileName, String label) {
        DriveHomePage drive = new DriveHomePage(d);
        if (drive.hasFile(fileName)) return true;
        if (!drive.openFab() || !drive.tapUploadFile()) return false;
        DriveUploadPage up = new DriveUploadPage(d);
        if (!up.uploadFile("Document", fileName)) return false;
        sleep(12000);
        boolean ok = new DriveHomePage(d).hasFile(fileName);
        System.out.println(">>> " + label + " uploaded " + fileName + " = " + ok);
        return ok;
    }

    /**
     * Creates a folder and waits until the list actually shows it.
     *
     * <p>Retried: the first FAB tap after landing on the list is regularly swallowed
     * mid-render, and a single attempt turned that into a skipped folder-sharing case
     * (with "A could not create …" pointing at the wrong thing).
     */
    private boolean makeFolder(AndroidDriver d, String name) {
        DriveHomePage drive = new DriveHomePage(d);
        drive.showFolders();
        if (drive.hasItem(name)) return true;
        boolean submitted = false;
        for (int attempt = 0; attempt < 3 && !submitted; attempt++) {
            if (drive.openFab() && drive.tapCreateFolder()) {
                CreateFolderSheet sheet = new CreateFolderSheet(d);
                if (sheet.waitOpen(Duration.ofSeconds(10)) && sheet.createNamed(name)) {
                    submitted = true;
                    break;
                }
                try { d.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
            }
            sleep(2000);
        }
        if (!submitted) return false;
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (new DriveHomePage(d).hasItem(name)) return true;
            sleep(2500);
        }
        return false;
    }

    /**
     * Opens Share for an item that is already visible in the current list.
     *
     * <p>The tap and its PROOF are retried together — {@code openItemMenu} only reports
     * that it clicked a ⋮, and trusting that return value skipped cases while the menu
     * had never appeared.
     */
    private DriveSharePage share(AndroidDriver d, String item) {
        DriveHomePage drive = new DriveHomePage(d);
        DriveItemMenuPage menu = new DriveItemMenuPage(d);
        boolean menuOpen = false;
        for (int attempt = 0; attempt < 4 && !menuOpen; attempt++) {
            menuOpen = drive.openItemMenu(item) && menu.isOpen();
            if (!menuOpen) {
                try { d.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
            }
        }
        assumeTrue(menuOpen, "The row menu for " + item + " never opened");
        assumeTrue(menu.hasShare() && menu.tapShare(), "Share not offered for " + item);
        DriveSharePage sp = new DriveSharePage(d);
        assumeTrue(sp.isOpen(Duration.ofSeconds(20)), "Share screen did not open for " + item);
        return sp;
    }

    /** Polls the other device's "Shared with me" until the item shows up. */
    /**
     * Polls the recipient's "Shared with me" for an item.
     *
     * <p>Each round leaves the section and comes BACK, rather than re-tapping a tab that
     * is already selected: a tap on the current tab is a no-op, so the list was often
     * only pull-to-refreshed — and a shared FOLDER stayed invisible here while the very
     * next case proved the recipient had it. Switching sections forces a genuine reload.
     */
    private boolean waitSharedItem(AndroidDriver d, String name, boolean isFolder, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            DriveHomePage drive = new DriveHomePage(d);
            drive.showMyDrive();          // leave, so coming back re-queries
            drive.showSharedWithMe();
            if (isFolder) drive.showFolders(); else drive.showFiles();
            drive.pullToRefresh();
            sleep(3000);
            if (drive.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
        }
        return false;
    }

    // ==================== A → B : FILE ====================

    @Test
    @Order(1)
    @DisplayName("A uploads a file and SHARES it with B as viewer (view + download)")
    void aSharesFileAsViewer() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        assumeTrue(uploadFile(a, FILE_A, "A"), "A could not upload " + FILE_A);

        new DriveHomePage(a).showFiles();
        DriveSharePage sp = share(a, FILE_A);
        assertTrue(sp.shareWith(USER_B, true, false, true),
                "A should be able to grant " + USER_B + " view+download on " + FILE_A);
        System.out.println(">>> A shared " + FILE_A + " with " + USER_B + " as VIEW+DOWNLOAD");
        try { a.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(2)
    @DisplayName("B RECEIVES A's file in Shared with me")
    void bReceivesFile() {
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        boolean arrived = waitSharedItem(b, FILE_A, false, SYNC);
        System.out.println(">>> B sees " + FILE_A + " = " + arrived);
        assertTrue(arrived, "A's file should reach B's 'Shared with me' within " + SYNC.toSeconds() + "s");
    }

    @Test
    @Order(3)
    @DisplayName("B's rights are VIEWER — no Share / Move / Edit / Delete")
    void bHasViewerRights() {
        DriveHomePage drive = new DriveHomePage(b);
        drive.showSharedWithMe();
        drive.showFiles();
        assumeTrue(drive.openItemMenu(FILE_A), "Could not open the row menu on B");
        DriveItemMenuPage menu = new DriveItemMenuPage(b);
        assumeTrue(menu.hasDetails() || menu.hasFavorite(), "Row menu did not open on B");

        System.out.println(">>> B viewer menu: share=" + menu.hasShare() + " move=" + menu.hasMove()
                + " edit=" + menu.hasEditInfo() + " delete=" + menu.hasDelete());
        assertFalse(menu.hasEditInfo(), "A viewer must NOT be offered Edit Info");
        assertFalse(menu.hasMove(), "A viewer must NOT be offered Move to…");
        assertFalse(menu.hasDelete(), "A non-owner must NEVER be offered Delete");
        menu.dismiss();
    }

    @Test
    @Order(4)
    @DisplayName("A UPGRADES B to editor → B's menu gains the editor actions")
    void aUpgradesBToEditor() {
        assumeTrue(openDrive(a, "A"), "Could not reopen Drive on A");
        new DriveHomePage(a).showFiles();
        DriveSharePage sp = share(a, FILE_A);
        assertTrue(sp.shareWith(USER_B, true, true, true),
                "A should be able to upgrade " + USER_B + " to editor");
        try { a.navigate().back(); } catch (Exception ignored) { }
        sleep(6000);

        DriveHomePage drive = new DriveHomePage(b);
        drive.showSharedWithMe();
        drive.showFiles();
        drive.pullToRefresh();
        sleep(5000);
        assumeTrue(drive.openItemMenu(FILE_A), "Could not open the row menu on B");
        DriveItemMenuPage menu = new DriveItemMenuPage(b);
        System.out.println(">>> B editor menu: share=" + menu.hasShare() + " move=" + menu.hasMove()
                + " edit=" + menu.hasEditInfo() + " delete=" + menu.hasDelete());
        assertTrue(menu.hasEditInfo(), "An editor SHOULD be offered Edit Info");
        assertFalse(menu.hasDelete(), "Even an editor must not be offered Delete — that is the owner's");
        menu.dismiss();
    }

    // ==================== A → B : FOLDER ====================

    @Test
    @Order(5)
    @DisplayName("A shares a FOLDER with B as editor, and B can open it")
    void aSharesFolder() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        assumeTrue(makeFolder(a, FOLDER_A), "A could not create " + FOLDER_A);
        new DriveHomePage(a).showFolders();
        DriveSharePage sp = share(a, FOLDER_A);
        assertTrue(sp.shareWith(USER_B, true, true, true), "A should share the folder with edit rights");
        try { a.navigate().back(); } catch (Exception ignored) { }

        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        assertTrue(waitSharedItem(b, FOLDER_A, true, SYNC),
                "A's folder should reach B's 'Shared with me' → Folders");

        DriveHomePage drive = new DriveHomePage(b);
        assumeTrue(drive.openItem(FOLDER_A), "B could not open the shared folder");
        sleep(2500);
        assertTrue(new DriveFolderPage(b).isLoaded(Duration.ofSeconds(20)),
                "A folder shared with edit rights should open for B");
    }

    @Test
    @Order(6)
    @DisplayName("B uploads INTO A's shared folder, and A sees B's file there")
    void bUploadsIntoSharedFolder() {
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        DriveHomePage drive = new DriveHomePage(b);
        drive.showSharedWithMe();
        drive.showFolders();
        assumeTrue(drive.openItem(FOLDER_A), "B could not open " + FOLDER_A);
        sleep(2500);

        DriveFolderPage folder = new DriveFolderPage(b);
        assumeTrue(folder.isLoaded(Duration.ofSeconds(20)), "Shared folder did not open on B");
        assumeTrue(folder.openFab(), "B has no FAB inside the shared folder (no edit rights?)");
        assumeTrue(new DriveHomePage(b).tapUploadFile(), "B could not start an upload");
        DriveUploadPage up = new DriveUploadPage(b);
        assumeTrue(up.uploadFile("Document", FILE_B), "B's upload into the shared folder failed");
        sleep(12000);

        // A owns the folder — B's file must appear there for A too.
        assumeTrue(openDrive(a, "A"), "Could not reopen Drive on A");
        DriveHomePage aDrive = new DriveHomePage(a);
        aDrive.showFolders();
        assumeTrue(aDrive.openItem(FOLDER_A), "A could not open their own folder");
        sleep(3000);
        new DriveHomePage(a).showFiles();
        List<String> inside = new DriveHomePage(a).allItemNamesScrolled();
        System.out.println(">>> A sees inside " + FOLDER_A + ": " + inside);
        assertTrue(inside.stream().anyMatch(n -> n.equalsIgnoreCase(FILE_B)),
                "A should see the file B uploaded into A's shared folder. Got: " + inside);
    }

    // ==================== B → A : the mirror image ====================

    @Test
    @Order(7)
    @DisplayName("B uploads a file of their own and SHARES it with A as editor")
    void bSharesFileWithA() {
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        DriveHomePage drive = new DriveHomePage(b);
        drive.showMyDrive();
        assumeTrue(uploadFile(b, FILE_B, "B"), "B could not upload " + FILE_B);

        new DriveHomePage(b).showFiles();
        DriveSharePage sp = share(b, FILE_B);
        assertTrue(sp.shareWith(USER_A, true, true, true),
                "B should be able to grant " + USER_A + " edit rights on " + FILE_B);
        System.out.println(">>> B shared " + FILE_B + " with " + USER_A + " as VIEW+EDIT+DOWNLOAD");
        try { b.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(8)
    @DisplayName("A RECEIVES B's file — sharing works in BOTH directions")
    void aReceivesBsFile() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        boolean arrived = waitSharedItem(a, FILE_B, false, SYNC);
        System.out.println(">>> A sees " + FILE_B + " = " + arrived);
        assertTrue(arrived, "B's file should reach A's 'Shared with me' — the reverse direction "
                + "must work as well as A→B");
    }

    @Test
    @Order(9)
    @DisplayName("B shares a FOLDER with A, and A can open it")
    void bSharesFolderWithA() {
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        new DriveHomePage(b).showMyDrive();
        assumeTrue(makeFolder(b, FOLDER_B), "B could not create " + FOLDER_B);
        new DriveHomePage(b).showFolders();
        DriveSharePage sp = share(b, FOLDER_B);
        assertTrue(sp.shareWith(USER_A, true, true, true), "B should share their folder with A");
        try { b.navigate().back(); } catch (Exception ignored) { }

        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        assertTrue(waitSharedItem(a, FOLDER_B, true, SYNC), "B's folder should reach A");
        DriveHomePage drive = new DriveHomePage(a);
        assumeTrue(drive.openItem(FOLDER_B), "A could not open B's shared folder");
        sleep(2500);
        assertTrue(new DriveFolderPage(a).isLoaded(Duration.ofSeconds(20)),
                "B's shared folder should open for A");
    }

    // ==================== editing from both sides ====================
    //
    // The editor is Zillit Sheets now: the grid is a canvas, so a value cannot be read back
    // from the accessibility tree. Each side types a marker into an empty part of the sheet,
    // leaves (Skip on "Let others know…"), and the OTHER side proves it arrived by downloading
    // the file and reading the xlsx XML (DriveFileContent).

    private String markerA, markerB;

    /** Opens FILE_A with btnEdit on {@code d} (My Drive for A, Shared with me for B). */
    private DriveEditorPage editOn(AndroidDriver d, String label, boolean shared) {
        assumeTrue(openDrive(d, label), "Could not open Drive on " + label);
        DriveHomePage drive = new DriveHomePage(d);
        if (shared) drive.showSharedWithMe(); else drive.showMyDrive();
        drive.showFiles();
        assumeTrue(drive.openItem(FILE_A), label + " could not open " + FILE_A);
        assumeTrue(waitId(d, "btnEdit", Duration.ofSeconds(25)), "No Edit button for " + label + " (not an editor?)");
        clickId(d, "btnEdit");
        DriveEditorPage ed = new DriveEditorPage(d);
        assumeTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)) && ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S)),
                "Editor did not load on " + label);
        return ed;
    }

    /** Downloads FILE_A on {@code d} and reports whether every marker is in the file. */
    private DriveFileContent.Result downloadedHolds(AndroidDriver d, String label, boolean shared, String marker) {
        assumeTrue(openDrive(d, label), "Could not open Drive on " + label);
        DriveHomePage drive = new DriveHomePage(d);
        if (shared) drive.showSharedWithMe(); else drive.showMyDrive();
        drive.showFiles();
        assumeTrue(drive.openItem(FILE_A), label + " could not open " + FILE_A);
        DriveFileDetailPage detail = new DriveFileDetailPage(d);
        assumeTrue(detail.waitFor("btnDownload", Duration.ofSeconds(25)), "No Download for " + label);
        detail.tapDownload();
        detail.waitDownloadDone(Duration.ofSeconds(40));
        DriveFileContent.Result r = new DriveFileContent(d).waitForMarker(FILE_A, marker, Duration.ofSeconds(60));
        assumeTrue(!r.copies.isEmpty(), "No downloaded copy of " + FILE_A + " on " + label + ": " + r);
        return r;
    }

    @Test
    @Order(10)
    @DisplayName("A EDITS the shared spreadsheet → B's download contains A's value")
    void aEditsBSees() {
        DriveEditorPage ed = editOn(a, "A", false);
        markerA = "zle2ea" + STAMP;
        assertTrue(ed.writeMarker(markerA), "A could not type into the sheet");
        ed.waitForAutosave();
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(15), true);
        assertTrue(r.closed, "A's editor should close: " + r);

        DriveFileContent.Result seen = downloadedHolds(b, "B", true, markerA);
        System.out.println(">>> B's copy of " + FILE_A + ": " + seen);
        assertTrue(seen.markerFound, "B must get the edit A made to the shared file: " + seen);
    }

    @Test
    @Order(11)
    @DisplayName("B EDITS the same shared file → A's download contains B's value")
    void bEditsASees() {
        DriveEditorPage ed = editOn(b, "B", true);
        markerB = "zle2eb" + STAMP;
        assertTrue(ed.writeMarker(markerB), "B could not type into the sheet");
        ed.waitForAutosave();
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(15), true);
        assertTrue(r.closed, "B's editor should close: " + r);

        DriveFileContent.Result seen = downloadedHolds(a, "A", false, markerB);
        System.out.println(">>> A's copy of " + FILE_A + ": " + seen);
        assertTrue(seen.markerFound, "A must get the edit B made to the file A owns: " + seen);
    }

    @Test
    @Order(12)
    @DisplayName("BOTH edits survive together — neither user's work is overwritten")
    void bothEditsSurvive() {
        assumeTrue(markerA != null && markerB != null, "Both edit cases must run first");
        DriveFileContent.Result withB = downloadedHolds(a, "A", false, markerB);
        DriveFileContent.Result withA = new DriveFileContent(a).waitForMarker(FILE_A, markerA, Duration.ofSeconds(20));
        System.out.println(">>> final: A's value " + withA + " | B's value " + withB);
        assertTrue(withA.markerFound, "A's edit must still be there");
        assertTrue(withB.markerFound, "B's edit must still be there — not last-writer-wins");
    }

    // ==================== revocation ====================

    @Test
    @Order(13)
    @DisplayName("A REVOKES B's access → the file disappears from B's Shared with me")
    void aRevokesAccess() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        DriveHomePage ad = new DriveHomePage(a);
        ad.showMyDrive();
        ad.showFiles();
        DriveSharePage sp = share(a, FILE_A);

        // Removing every right is how access is withdrawn from the People tab.
        assumeTrue(sp.searchUser(USER_B) && sp.userListed(USER_B), USER_B + " not listed");
        boolean revoked = sp.shareWith(USER_B, false, false, false);
        System.out.println(">>> A revoked all rights for " + USER_B + " = " + revoked);
        try { a.navigate().back(); } catch (Exception ignored) { }
        assumeTrue(revoked, "Could not clear the rights — revocation path unavailable on this build");
        sleep(8000);

        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        DriveHomePage bd = new DriveHomePage(b);
        bd.showSharedWithMe();
        bd.showFiles();
        bd.pullToRefresh();
        sleep(6000);
        assumeTrue(bd.isListReadable(), "B's list unreadable — cannot judge the revocation");
        List<String> left = bd.allItemNamesScrolled();
        System.out.println(">>> B's shared files after revoke = " + left);
        assertFalse(left.stream().anyMatch(n -> n.equalsIgnoreCase(FILE_A)),
                "After revoking, the file must disappear from B's 'Shared with me'. Got: " + left);
    }

    // ==================== cleanup ====================

    @Test
    @Order(14)
    @DisplayName("CLEANUP: each device removes the folders it created")
    void cleanup() {
        deleteFolder(a, FOLDER_A, "A");
        deleteFolder(b, FOLDER_B, "B");
    }

    // =====================================================================================
    // WEB GAP cases (web-e2e/gaps/drive.md M08 per-right detail controls, M09 folder roles,
    // M22 Shared-with-me presentation). Run on A = emulator (Vidya Testing), B = Pixel
    // (Vidya Pixel). Everything is QA_… and removed by gapCleanup2 on A.
    // =====================================================================================

    private static final String G_RT = "QA_Rt_" + STAMP + ".xlsx";
    private static final String G_FR = "QA_FRole_" + STAMP;
    private static final String G_P  = "QA_P_" + STAMP;
    private static final String G_C  = "QA_C_" + STAMP;

    /** Detail controls a recipient sees, by id. */
    private java.util.Map<String, Boolean> detailControlsOnB(String file) {
        assumeTrue(openDrive(b, "B"), "Drive on B");
        assumeTrue(waitSharedItem(b, file, false, SYNC), file + " never reached B");
        DriveHomePage bd = new DriveHomePage(b);
        assumeTrue(bd.openItem(file), "B could not open " + file);
        DriveFileDetailPage d = new DriveFileDetailPage(b);
        assumeTrue(d.isOpen(Duration.ofSeconds(30)) && d.waitFor("btnOpen", Duration.ofSeconds(25)), "Detail on B");
        sleep(2500);
        java.util.Map<String, Boolean> m = new java.util.LinkedHashMap<>();
        for (String id : List.of("btnOpen", "btnDownload", "btnEdit", "btnShare", "btnManageAccess", "btnDelete", "etComment")) {
            m.put(id, !b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/" + id)).isEmpty());
        }
        m.put("+ Add Tag", !b.findElements(AppiumBy.androidUIAutomator("new UiSelector().textStartsWith(\"+ Add Tag\")")).isEmpty()
                || d.isVisibleId("tagsContainer") && !b.findElements(AppiumBy.androidUIAutomator("new UiSelector().textStartsWith(\"+ Add Tag\")")).isEmpty());
        try { b.navigate().back(); } catch (Exception ignored) { }
        System.out.println(">>> B detail controls for " + file + " = " + m);
        return m;
    }

    private void grantFile(boolean view, boolean edit, boolean download) {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        new DriveHomePage(a).showMyDrive();
        new DriveHomePage(a).showFiles();
        DriveSharePage sp = share(a, G_RT);
        assertTrue(sp.shareWith(USER_B, view, edit, download), "A could not set " + USER_B + "'s rights");
        try { a.navigate().back(); } catch (Exception ignored) { }
        sleep(6000);
    }

    @Test
    @Order(20)
    @DisplayName("[WEB L601/L1047] RIGHTS: with View only, B's detail has Open and nothing else (no Download/Edit/Share/Manage/Delete/composer/tags)")
    void gRightsViewOnly() {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        assumeTrue(new DriveFlows(a).uploadAs(FILE_A, G_RT), "A could not upload " + G_RT);
        grantFile(true, false, false);
        var m = detailControlsOnB(G_RT);
        assertTrue(m.get("btnOpen"), "View lets B open the file");
        for (String id : List.of("btnDownload", "btnEdit", "btnShare", "btnManageAccess", "btnDelete", "etComment", "+ Add Tag")) {
            assertFalse(m.get(id), "With View only, B must not get " + id + ": " + m);
        }
    }

    @Test
    @Order(21)
    @DisplayName("[WEB L601] RIGHTS: adding Download gives B only the Download button")
    void gRightsDownload() {
        grantFile(true, false, true);
        var m = detailControlsOnB(G_RT);
        assertTrue(m.get("btnDownload"), "Download right → btnDownload: " + m);
        for (String id : List.of("btnEdit", "btnShare", "btnManageAccess", "btnDelete")) {
            assertFalse(m.get(id), "Download right alone must not give " + id + ": " + m);
        }
    }

    @Test
    @Order(22)
    @DisplayName("[WEB L601/L1071] RIGHTS: adding Edit gives Edit, Copy link, Manage access, the composer and tags — never Delete or Move")
    void gRightsEdit() {
        grantFile(true, true, true);
        var m = detailControlsOnB(G_RT);
        for (String id : List.of("btnEdit", "btnShare", "btnManageAccess", "etComment", "+ Add Tag")) {
            assertTrue(m.get(id), "Edit right should give " + id + ": " + m);
        }
        assertFalse(m.get("btnDelete"), "Delete stays the owner's: " + m);
        DriveHomePage bd = new DriveHomePage(b);
        bd.showSharedWithMe();
        bd.showFiles();
        assumeTrue(bd.openItemMenu(G_RT), "Row menu on B");
        DriveItemMenuPage menu = new DriveItemMenuPage(b);
        boolean share = menu.hasShare(), edit = menu.hasEditInfo(), move = menu.hasMove();
        menu.dismiss();
        assertTrue(share && edit, "An editor's row menu offers Share and Edit Info");
        assertFalse(move, "Move is never offered under Shared with me (ZL-21869)");
    }

    @Test
    @Order(23)
    @DisplayName("[WEB L1301] RIGHTS going down Edit → Download → View take B's controls away step by step")
    void gRightsStepDown() {
        grantFile(true, false, true);
        var m1 = detailControlsOnB(G_RT);
        assertFalse(m1.get("btnEdit") || m1.get("btnShare") || m1.get("btnManageAccess"), "Edit controls must go: " + m1);
        assertTrue(m1.get("btnDownload"), "Download stays: " + m1);
        grantFile(true, false, false);
        var m2 = detailControlsOnB(G_RT);
        assertFalse(m2.get("btnDownload"), "Download must go too: " + m2);
        assertTrue(m2.get("btnOpen"), "View stays: " + m2);
    }

    // ---------------- M09 folder roles ----------------

    private void setFolderRole(String folder, String role) {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        new DriveHomePage(a).showMyDrive();
        new DriveHomePage(a).showFolders();
        DriveSharePage sp = share(a, folder);
        boolean ok;
        if (sp.hasGrantFor(USER_B)) ok = sp.setRole(USER_B, role);
        else ok = sp.searchUser(USER_B) && sp.userListed(USER_B) && sp.addUser(USER_B) && sp.setRole(USER_B, role);
        sp.save();
        try { a.navigate().back(); } catch (Exception ignored) { }
        assertTrue(ok, "A could not give " + USER_B + " the " + role + " role on " + folder);
        sleep(6000);
    }

    /** B: menu entries for the shared folder and whether the + is offered inside it. */
    private java.util.Map<String, Boolean> folderViewOnB(String folder) {
        assumeTrue(openDrive(b, "B"), "Drive on B");
        assumeTrue(waitSharedItem(b, folder, true, SYNC), folder + " never reached B");
        DriveHomePage bd = new DriveHomePage(b);
        java.util.Map<String, Boolean> m = new java.util.LinkedHashMap<>();
        assumeTrue(bd.openItemMenu(folder), "Row menu on B");
        DriveItemMenuPage menu = new DriveItemMenuPage(b);
        m.put("share", menu.hasShare()); m.put("editInfo", menu.hasEditInfo());
        m.put("move", menu.hasMove()); m.put("delete", menu.hasDelete());
        menu.dismiss();
        assumeTrue(bd.openItem(folder), "B could not open " + folder);
        sleep(3000);
        m.put("fab", !b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/fabAdd")).isEmpty());
        try { b.navigate().back(); } catch (Exception ignored) { }
        System.out.println(">>> B on " + folder + " = " + m);
        return m;
    }

    @Test
    @Order(30)
    @DisplayName("[WEB L583] FOLDER ROLE Viewer: B gets no Edit Info / Move / Share / Delete and no + inside")
    void gFolderViewer() {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        assumeTrue(makeFolder(a, G_FR), "A could not create " + G_FR);
        new DriveFlows(a).openFolder(G_FR);
        new DriveFlows(a).createFolder("child");
        new DriveFlows(a).backToHome();
        setFolderRole(G_FR, "viewer");
        var m = folderViewOnB(G_FR);
        for (String k : List.of("editInfo", "move", "share", "delete", "fab")) assertFalse(m.get(k), "Viewer must not get " + k + ": " + m);
    }

    @Test
    @Order(31)
    @DisplayName("[WEB L589/L1059] FOLDER ROLE Editor: + inside and Edit Info — still no Move, Share or Delete")
    void gFolderEditor() {
        setFolderRole(G_FR, "editor");
        var m = folderViewOnB(G_FR);
        assertTrue(m.get("fab"), "An Editor may add inside the folder (ZL-21871): " + m);
        assertTrue(m.get("editInfo"), "An Editor gets Edit Info: " + m);
        assertFalse(m.get("move"), "Never Move under Shared with me (ZL-21869): " + m);
        assertFalse(m.get("delete"), "Delete stays the owner's: " + m);
    }

    @Test
    @Order(32)
    @DisplayName("[WEB L595] FOLDER ROLE Owner: B gets Share under Shared with me")
    void gFolderOwner() {
        setFolderRole(G_FR, "owner");
        var m = folderViewOnB(G_FR);
        assertTrue(m.get("share"), "An Owner role gives Share: " + m);
    }

    @Test
    @Order(33)
    @DisplayName("[WEB L504/L1313] FOLDER ROLE inherited: the child's share screen says 'Has access through the parent folder'")
    void gFolderInherited() {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        assumeTrue(new DriveFlows(a).openFolder(G_FR), "Open " + G_FR + " on A");
        new DriveHomePage(a).showFolders();
        DriveSharePage sp = share(a, "child");
        sleep(3000);
        boolean listed = sp.hasGrantFor(USER_B);
        boolean inherited = sp.textShown("Has access through the parent folder", Duration.ofSeconds(4));
        try { a.navigate().back(); } catch (Exception ignored) { }
        new DriveFlows(a).backToHome();
        assertTrue(listed, USER_B + " should be listed on the child through the parent");
        assertTrue(inherited, "The inherited row should say 'Has access through the parent folder'");
    }

    @Test
    @Order(34)
    @DisplayName("[WEB L1307] FOLDER ROLE going down Owner → Viewer takes Share and the + away")
    void gFolderStepDown() {
        setFolderRole(G_FR, "viewer");
        var m = folderViewOnB(G_FR);
        assertFalse(m.get("share"), "Share must go: " + m);
        assertFalse(m.get("fab"), "The + must go: " + m);
    }

    @Test
    @Order(35)
    @DisplayName("[WEB L1804] LIVE REVOKE: B inside the folder is sent back when A removes B (no touch on B)")
    void gLiveRevoke() {
        assumeTrue(openDrive(b, "B"), "Drive on B");
        assumeTrue(waitSharedItem(b, G_FR, true, SYNC), G_FR + " on B");
        assumeTrue(new DriveHomePage(b).openItem(G_FR) && new DriveFolderPage(b).isLoaded(Duration.ofSeconds(20)), "B inside " + G_FR);
        assumeTrue(openDrive(a, "A"), "Drive on A");
        new DriveHomePage(a).showMyDrive();
        new DriveHomePage(a).showFolders();
        DriveSharePage sp = share(a, G_FR);
        boolean removed = sp.removeFolderAccess(USER_B);
        sp.save();
        try { a.navigate().back(); } catch (Exception ignored) { }
        assumeTrue(removed, "A could not remove " + USER_B + " from " + G_FR);
        boolean kicked = false;
        for (int i = 0; i < 15 && !kicked; i++) {
            sleep(3000);
            kicked = !new DriveFolderPage(b).isFolderActivity();
        }
        System.out.println(">>> B left the revoked folder live = " + kicked);
        assertTrue(kicked, "B should be sent out of a folder they lost access to, without touching the screen (ZL-21815)");
    }

    @Test
    @Order(36)
    @DisplayName("[WEB L1828] LIVE: a subfolder A creates appears in B's open folder without a refresh")
    void gLiveSubfolder() {
        setFolderRole(G_FR, "viewer");
        assumeTrue(openDrive(b, "B"), "Drive on B");
        assumeTrue(waitSharedItem(b, G_FR, true, SYNC), G_FR + " on B");
        assumeTrue(new DriveHomePage(b).openItem(G_FR) && new DriveFolderPage(b).isLoaded(Duration.ofSeconds(20)), "B inside " + G_FR);
        String sub = "live_" + STAMP;
        assumeTrue(openDrive(a, "A") && new DriveFlows(a).openFolder(G_FR) && new DriveFlows(a).createFolder(sub), "A creates " + sub);
        new DriveFlows(a).backToHome();
        boolean seen = false;
        for (int i = 0; i < 15 && !seen; i++) {
            sleep(3000);
            seen = new DriveFolderPage(b).itemNames().contains(sub);
        }
        try { b.navigate().back(); } catch (Exception ignored) { }
        assertTrue(seen, "A's new subfolder should appear in B's open folder live (no refresh)");
    }

    @Test
    @Order(37)
    @DisplayName("[WEB L625] LIVE: A's rename of a shared folder reaches B's open list without a refresh")
    void gLiveRename() {
        assumeTrue(openDrive(b, "B"), "Drive on B");
        assumeTrue(waitSharedItem(b, G_FR, true, SYNC), G_FR + " on B");
        String renamed = G_FR + "_r";
        assumeTrue(openDrive(a, "A"), "Drive on A");
        var r = new DriveFlows(a).editInfo(G_FR, true, renamed, null);
        assumeTrue(r.saved, "A could not rename: " + r);
        boolean seen = false;
        for (int i = 0; i < 15 && !seen; i++) {
            sleep(3000);
            seen = new DriveHomePage(b).itemNames().contains(renamed);
        }
        // Put the name back so later cases and cleanup find it.
        new DriveFlows(a).openDrive();
        new DriveFlows(a).editInfo(renamed, true, G_FR, null);
        assertTrue(seen, "B's list should show the new name live");
    }

    // ---------------- M22 Shared with me presentation ----------------

    @Test
    @Order(40)
    @DisplayName("[WEB L72/L1770] SHARED ROOT: no + and no Show filter; a shared row carries the shared indicator and 'Shared by'")
    void gSharedRootPresentation() {
        assumeTrue(openDrive(b, "B"), "Drive on B");
        DriveHomePage bd = new DriveHomePage(b);
        bd.showSharedWithMe();
        bd.showFolders();
        sleep(2000);
        boolean fab = !b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/fabAdd")).isEmpty();
        boolean show = !b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/spMyDriveFilter")).isEmpty();
        boolean indicator = !b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/ivSharedIndicator")).isEmpty();
        List<String> by = b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/tvSharedBy")).stream()
                .map(org.openqa.selenium.WebElement::getText).toList();
        bd.showMyDrive();
        boolean fabMine = !b.findElements(AppiumBy.id(b.getCurrentPackage() + ":id/fabAdd")).isEmpty();
        System.out.println(">>> shared root fab=" + fab + " show=" + show + " indicator=" + indicator + " sharedBy=" + by + " myDriveFab=" + fabMine);
        assertFalse(fab, "No + at the Shared with me root");
        assertFalse(show, "No Show filter at the Shared with me root");
        assertTrue(fabMine, "My Drive does offer the +");
        assumeTrue(!by.isEmpty() || indicator, "Nothing is shared with B to read the indicator from");
        assertTrue(indicator, "Shared rows carry the 'Shared with you' indicator");
        assertTrue(by.stream().anyMatch(t -> t.startsWith("Shared by")), "Shared rows read 'Shared by …': " + by);
    }

    @Test
    @Order(41)
    @DisplayName("[WEB L608] SHARED ROOT lists the folder B can open, not its ancestor (REL-BUG-05)")
    void gSharedChildNotParent() {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        assumeTrue(makeFolder(a, G_P), "A could not create " + G_P);
        assumeTrue(new DriveFlows(a).openFolder(G_P) && new DriveFlows(a).createFolder(G_C), "child");
        new DriveHomePage(a).showFolders();
        DriveSharePage sp = share(a, G_C);
        assertTrue(sp.shareWith(USER_B, true, false, true), "Share the child");
        try { a.navigate().back(); } catch (Exception ignored) { }
        new DriveFlows(a).backToHome();
        assumeTrue(openDrive(b, "B"), "Drive on B");
        boolean child = waitSharedItem(b, G_C, true, SYNC);
        List<String> names = new DriveHomePage(b).allItemNamesScrolled();
        assertTrue(child, "The shared child should be listed for B");
        assertFalse(names.contains(G_P), "The unshared parent must not be listed: " + names);
        // [WEB L1319] B's search never finds A's private folder.
        DriveHomePage bd = new DriveHomePage(b);
        assumeTrue(bd.openDrawer() && new DriveItemMenuPage(b).openSearch(), "Search on B");
        com.zillit.automation.pages.drive.DriveSearchPage search = new com.zillit.automation.pages.drive.DriveSearchPage(b);
        assumeTrue(search.isOpen(Duration.ofSeconds(10)) && search.query(G_P), "query");
        sleep(5000);
        List<String> r = search.resultNames();
        search.back();
        assertFalse(r.contains(G_P), "B's search must not find A's private folder: " + r);
    }

    @Test
    @Order(42)
    @DisplayName("[WEB L1319] STARS are per user: B starring a shared folder does not star it for A")
    void gStarIsPerUser() {
        assumeTrue(openDrive(b, "B"), "Drive on B");
        assumeTrue(waitSharedItem(b, G_C, true, SYNC), "child on B");
        assertTrue(new DriveFlows(b).tapRowStar(G_C), "B's row star");
        try {
            assumeTrue(openDrive(a, "A"), "Drive on A");
            assumeTrue(new DriveFlows(a).openFolder(G_P), "Open parent on A");
            DriveHomePage ad = new DriveHomePage(a);
            assumeTrue(ad.openItemMenu(G_C), "A's row menu");
            String label = new DriveItemMenuPage(a).favoriteLabel();
            new DriveItemMenuPage(a).dismiss();
            new DriveFlows(a).backToHome();
            assertEquals("Add to favorites", label, "B's star must not show as A's favourite");
        } finally {
            openDrive(b, "B");
            waitSharedItem(b, G_C, true, Duration.ofSeconds(30));
            new DriveFlows(b).tapRowStar(G_C);
        }
    }

    private static final String G_PERM = "QA_Perm_" + STAMP + ".pdf";

    @Test
    @Order(45)
    @DisplayName("[WEB L1956] UPLOAD with Specific Users = B at Download reaches B at exactly Download")
    void gUploadPermissionDownload() {
        assumeTrue(openDrive(a, "A"), "Drive on A");
        DriveHomePage ad = new DriveHomePage(a);
        ad.showMyDrive();
        ad.showFiles();
        assumeTrue(ad.openFab() && ad.tapUploadFile(), "Upload File on A");
        DriveUploadPage up = new DriveUploadPage(a);
        assumeTrue(up.isSourceSheetOpen() && up.chooseSource("Document"), "Document source");
        assumeTrue(up.isSystemPickerOpen(Duration.ofSeconds(20)) && up.pickFileNamed("zl_drive_doc.pdf", Duration.ofSeconds(45)), "pick");
        assumeTrue(up.isPreviewOpen(Duration.ofSeconds(20)) && up.sendFromPreview() && up.isStagingOpen(Duration.ofSeconds(20)), "staging");
        up.renameStaged("zl_drive_doc.pdf", G_PERM);
        assertTrue(up.grantSpecificUser(USER_B, "Download"), "Could not pick " + USER_B + " at Download in staging");
        assertTrue(up.upload(), "Upload");
        sleep(10000);
        var m = detailControlsOnB(G_PERM);
        assertTrue(m.get("btnDownload"), "B should get Download: " + m);
        assertFalse(m.get("btnEdit") || m.get("btnShare") || m.get("btnManageAccess"), "…and nothing above it: " + m);
    }

    @Test
    @Order(98)
    @DisplayName("WEB GAP CLEANUP (A): remove QA_Rt / QA_FRole / QA_P (Trash included)")
    void gapCleanup2() {
        DriveFlows f = new DriveFlows(a);
        List<String> failed = new java.util.ArrayList<>();
        if (!f.purge(G_RT, false)) failed.add(G_RT);
        f.purge(G_PERM, false);
        for (String n : List.of(G_FR, G_P)) if (!f.purge(n, true)) failed.add(n);
        assertTrue(failed.isEmpty(), "Could not remove: " + failed);
    }

    private void deleteFolder(AndroidDriver d, String name, String label) {
        try {
            if (!openDrive(d, label)) return;
            DriveHomePage drive = new DriveHomePage(d);
            drive.showMyDrive();
            drive.showFolders();
            if (!drive.isListReadable()) return;
            if (drive.allItemNamesScrolled().stream().noneMatch(n -> n.equalsIgnoreCase(name))) return;
            if (!drive.openItemMenu(name)) return;
            DriveItemMenuPage menu = new DriveItemMenuPage(d);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                drive.confirmDeleteDialog(Duration.ofSeconds(8));
                sleep(3000);
                System.out.println(">>> " + label + " deleted " + name);
            } else {
                menu.dismiss();
            }
        } catch (Exception e) {
            System.out.println(">>> cleanup of " + name + " on " + label + " skipped: " + e.getMessage());
        }
    }

    // ---------------- helpers ----------------

    private boolean waitId(AndroidDriver d, String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!d.findElements(AppiumBy.id(d.getCurrentPackage() + ":id/" + id)).isEmpty()) return true;
            } catch (Exception ignored) { }
            sleep(800);
        }
        return false;
    }

    private void clickId(AndroidDriver d, String id) {
        try { d.findElement(AppiumBy.id(d.getCurrentPackage() + ":id/" + id)).click(); }
        catch (Exception ignored) { }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
