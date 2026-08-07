package com.zillit.automation.tests;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveEditorPage;
import com.zillit.automation.pages.DriveFolderPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveSharePage;
import com.zillit.automation.pages.DriveUploadPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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

    private static final String USER_A = System.getenv().getOrDefault("ZILLIT_USER_A", "Vidya Sagar");
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
                && tools.openModule("Drive")
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

    @Test
    @Order(10)
    @DisplayName("A EDITS the shared spreadsheet → the change reaches B")
    void aEditsBSees() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        DriveHomePage drive = new DriveHomePage(a);
        drive.showMyDrive();
        drive.showFiles();
        assumeTrue(drive.openItem(FILE_A), "A could not open " + FILE_A);
        assumeTrue(waitId(a, "btnEdit", Duration.ofSeconds(25)), "No Edit button for A");
        clickId(a, "btnEdit");

        DriveEditorPage ed = new DriveEditorPage(a);
        assumeTrue(ed.waitLoaded(Duration.ofSeconds(150)), "Editor did not load on A");
        assumeTrue(ed.enterEditMode(), "A could not enter edit mode");

        String marker = "A" + STAMP;
        assumeTrue(ed.writeCell("H1", marker), "A could not write the cell");
        assertEquals(marker, ed.readCell("H1"), "A's own edit should read back");
        ed.acceptEdit();
        ed.waitForSave();
        ed.forceExit();

        // B opens the same file and must see A's value.
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        DriveHomePage bd = new DriveHomePage(b);
        bd.showSharedWithMe();
        bd.showFiles();
        assumeTrue(bd.openItem(FILE_A), "B could not open the shared file");
        assumeTrue(waitId(b, "btnEdit", Duration.ofSeconds(25)), "B has no Edit button (not an editor?)");
        clickId(b, "btnEdit");

        DriveEditorPage bEd = new DriveEditorPage(b);
        assumeTrue(bEd.waitLoaded(Duration.ofSeconds(150)), "Editor did not load on B");
        assumeTrue(bEd.enterEditMode(), "B could not enter edit mode");
        String seen = bEd.readCell("H1");
        System.out.println(">>> B sees H1 = '" + seen + "' (A wrote '" + marker + "')");
        bEd.forceExit();
        assertEquals(marker, seen, "B must see the edit A made to the shared file");
    }

    @Test
    @Order(11)
    @DisplayName("B EDITS the same shared file → the change reaches A")
    void bEditsASees() {
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");
        DriveHomePage bd = new DriveHomePage(b);
        bd.showSharedWithMe();
        bd.showFiles();
        assumeTrue(bd.openItem(FILE_A), "B could not open the shared file");
        assumeTrue(waitId(b, "btnEdit", Duration.ofSeconds(25)), "B has no Edit button");
        clickId(b, "btnEdit");

        DriveEditorPage bEd = new DriveEditorPage(b);
        assumeTrue(bEd.waitLoaded(Duration.ofSeconds(150)), "Editor did not load on B");
        assumeTrue(bEd.enterEditMode(), "B could not enter edit mode");

        String marker = "B" + STAMP;
        assumeTrue(bEd.writeCell("H2", marker), "B could not write the cell");
        bEd.acceptEdit();
        bEd.waitForSave();
        bEd.forceExit();

        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        DriveHomePage ad = new DriveHomePage(a);
        ad.showMyDrive();
        ad.showFiles();
        assumeTrue(ad.openItem(FILE_A), "A could not open their file");
        assumeTrue(waitId(a, "btnEdit", Duration.ofSeconds(25)), "No Edit button for A");
        clickId(a, "btnEdit");

        DriveEditorPage ed = new DriveEditorPage(a);
        assumeTrue(ed.waitLoaded(Duration.ofSeconds(150)), "Editor did not load on A");
        assumeTrue(ed.enterEditMode(), "A could not enter edit mode");
        String seen = ed.readCell("H2");
        System.out.println(">>> A sees H2 = '" + seen + "' (B wrote '" + marker + "')");
        ed.forceExit();
        assertEquals(marker, seen, "A must see the edit B made to the file A owns");
    }

    @Test
    @Order(12)
    @DisplayName("BOTH edits survive together — neither user's work is overwritten")
    void bothEditsSurvive() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        DriveHomePage ad = new DriveHomePage(a);
        ad.showMyDrive();
        ad.showFiles();
        assumeTrue(ad.openItem(FILE_A), "A could not open " + FILE_A);
        assumeTrue(waitId(a, "btnEdit", Duration.ofSeconds(25)), "No Edit button");
        clickId(a, "btnEdit");

        DriveEditorPage ed = new DriveEditorPage(a);
        assumeTrue(ed.waitLoaded(Duration.ofSeconds(150)), "Editor did not load");
        assumeTrue(ed.enterEditMode(), "Could not enter edit mode");

        String fromA = ed.readCell("H1");
        String fromB = ed.readCell("H2");
        System.out.println(">>> final: H1(A)='" + fromA + "'  H2(B)='" + fromB + "'");
        ed.forceExit();

        assertEquals("A" + STAMP, fromA, "A's edit must still be there");
        assertEquals("B" + STAMP, fromB, "B's edit must still be there — collaboration must not "
                + "be last-writer-wins");
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
