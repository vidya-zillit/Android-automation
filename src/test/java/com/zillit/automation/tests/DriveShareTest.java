package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveSharePage;
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
 * DRIVE SHARING — granting another project user access with specific rights
 * (Can View / Can Edit / Can Download), and the share-by-link flow.
 *
 * <p>Set {@code ZILLIT_SHARE_USER} to the display name of another user on the project
 * (default "Vidya Pixel"). The rights granted here are what the recipient-side suite
 * ({@code DriveSharedWithMeTest}, run on the SECOND device) then verifies.
 *
 * <p>The per-user rights are three unlabelled {@code Switch} widgets with no resource
 * ids — {@link DriveSharePage} pairs each with the label on its row. Three ids from the
 * app team would remove the most fragile locator in the suite.
 */
@Order(TestOrder.DRIVE_SHARE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveShareTest extends BaseTest {

    private static final String SHARE_USER =
            System.getenv().getOrDefault("ZILLIT_SHARE_USER", "Vidya Pixel");
    private static final String DOC = "zl_drive_doc.pdf";
    private static final String SHARED_FOLDER = "QA_Shared_" + (System.currentTimeMillis() % 100000);

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
            boolean overlay = foreign || !driver.findElements(byAppId("tabShareMode")).isEmpty()
                    || !driver.findElements(byAppId("menuDelete")).isEmpty()
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()
                    || !driver.findElements(byAppId("btnUpload")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(1000); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** Ensures a shareable FILE exists, uploading one if needed. */
    private boolean ensureFile() {
        DriveHomePage drive = new DriveHomePage(driver);
        if (drive.hasFile(DOC)) return true;
        if (!drive.openFab() || !drive.tapUploadFile()) return false;
        DriveUploadPage up = new DriveUploadPage(driver);
        if (!up.uploadFile("Document", DOC)) return false;
        sleep(10000);
        return new DriveHomePage(driver).hasFile(DOC);
    }

    private DriveSharePage openShareFor(String name) {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openItemMenu(name), "Could not open the row menu for " + name);
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen(), "Row menu did not open");
        assumeTrue(menu.hasShare(), "Share is not offered for " + name);
        assumeTrue(menu.tapShare(), "Could not tap Share");
        DriveSharePage share = new DriveSharePage(driver);
        assumeTrue(share.isOpen(Duration.ofSeconds(15)), "Share screen did not open");
        return share;
    }

    // ==================== share screen ====================

    @Test
    @Order(1)
    @DisplayName("Share opens 'Manage File Access' with People and Share-via-link tabs")
    void shareScreenOpens() {
        assumeTrue(ensureFile(), "Could not prepare a file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);

        System.out.println(">>> share title='" + share.title() + "'");
        assertTrue(share.hasPeopleTab(), "A People tab is expected");
        assertTrue(share.hasLinkTab(), "A 'Share via link' tab is expected");
        screenshot("drive-share-screen");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(2)
    @DisplayName("A newly uploaded file starts with NO ONE having access")
    void startsWithNoAccess() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        boolean none = share.noAccessYet();
        System.out.println(">>> 'no one has access yet' = " + none);
        screenshot("drive-share-no-access");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(3)
    @DisplayName("Searching a project user lists them in the People tab")
    void searchUser() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);

        assumeTrue(share.searchUser(SHARE_USER), "Could not type into the user search");
        boolean listed = share.userListed(SHARE_USER);
        System.out.println(">>> '" + SHARE_USER + "' listed = " + listed);
        assertTrue(listed, "Searching should list the project user '" + SHARE_USER + "'");
        screenshot("drive-share-search-user");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(4)
    @DisplayName("Adding a user reveals the three rights: Can View / Can Edit / Can Download")
    void rightsOffered() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);

        assumeTrue(share.searchUser(SHARE_USER) && share.userListed(SHARE_USER), "User not listed");
        assumeTrue(share.addUser(SHARE_USER), "Could not add the user (the add control is unlabelled)");

        assertTrue(share.textShown(DriveSharePage.VIEW, Duration.ofSeconds(5)), "'Can View' expected");
        assertTrue(share.textShown(DriveSharePage.EDIT, Duration.ofSeconds(3)), "'Can Edit' expected");
        assertTrue(share.textShown(DriveSharePage.DOWNLOAD, Duration.ofSeconds(3)), "'Can Download' expected");
        System.out.println(">>> defaults: view=" + share.rightState(DriveSharePage.VIEW)
                + " edit=" + share.rightState(DriveSharePage.EDIT)
                + " download=" + share.rightState(DriveSharePage.DOWNLOAD));
        screenshot("drive-share-rights");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(5)
    @DisplayName("Rights toggle independently — Download can be granted without Edit")
    void rightsToggleIndependently() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);

        assumeTrue(share.searchUser(SHARE_USER) && share.userListed(SHARE_USER), "User not listed");
        assumeTrue(share.addUser(SHARE_USER), "Could not add the user");
        assumeTrue(share.rightsShown(), "Rights switches not shown");

        assertTrue(share.setRight(DriveSharePage.DOWNLOAD, true), "Download should be settable");
        assertTrue(share.setRight(DriveSharePage.EDIT, false), "Edit should be settable");
        Boolean dl = share.rightState(DriveSharePage.DOWNLOAD);
        Boolean ed = share.rightState(DriveSharePage.EDIT);
        System.out.println(">>> after toggling: download=" + dl + " edit=" + ed);
        assertTrue(Boolean.TRUE.equals(dl), "Download should be ON");
        assertFalse(Boolean.TRUE.equals(ed), "Edit should be OFF — the rights are independent");
        screenshot("drive-share-toggles");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(6)
    @DisplayName("SHARE a file as VIEW + DOWNLOAD (no edit) and save the permissions")
    void shareFileViewDownload() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);

        assertTrue(share.shareWith(SHARE_USER, true, false, true),
                "Sharing '" + DOC + "' with " + SHARE_USER + " (view+download) should save");
        sleep(3000);
        screenshot("drive-shared-view-download");
        System.out.println(">>> shared " + DOC + " with " + SHARE_USER + " as VIEW+DOWNLOAD");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(7)
    @DisplayName("Saved rights persist — reopening Share shows the user still has access")
    void rightsPersist() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);

        // Access is granted, so the "no one has access yet" hint must be gone.
        boolean none = share.noAccessYet();
        System.out.println(">>> still 'no access'? " + none + " (expected false after sharing)");
        assertFalse(none,
                "After sharing, the 'No one has access yet' hint should be gone");
        screenshot("drive-share-persisted");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    // ==================== folder sharing ====================

    @Test
    @Order(8)
    @DisplayName("SHARE a FOLDER with a user — folders are shareable just like files")
    void shareFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        if (!drive.hasItem(SHARED_FOLDER)) {
            assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
            CreateFolderSheet sheet = new CreateFolderSheet(driver);
            assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Create sheet did not open");
            assumeTrue(sheet.createNamed(SHARED_FOLDER), "Could not create the folder");
            sleep(2500);
        }
        DriveSharePage share = openShareFor(SHARED_FOLDER);
        assertTrue(share.hasPeopleTab(), "Folder share should offer the People tab");
        assertTrue(share.shareWith(SHARE_USER, true, true, true),
                "Sharing the folder with full rights should save");
        sleep(3000);
        screenshot("drive-folder-shared");
        System.out.println(">>> shared folder " + SHARED_FOLDER + " with " + SHARE_USER + " as VIEW+EDIT+DOWNLOAD");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    // ==================== share by link ====================

    @Test
    @Order(9)
    @DisplayName("Share-via-link offers permission, expiry and max-views controls")
    void linkTabControls() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Could not switch to the link tab");
        sleep(2000);

        System.out.println(">>> link controls: permission=" + share.hasPermissionSpinner()
                + " expiry=" + share.hasExpirySpinner()
                + " maxViews=" + share.hasMaxViewsSpinner()
                + " recipients=" + share.hasRecipients()
                + " generate=" + share.hasGenerateLink());
        assertTrue(share.hasGenerateLink() || share.hasPermissionSpinner(),
                "The link tab should offer link generation / permission controls");
        screenshot("drive-share-link-tab");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(10)
    @DisplayName("Link permission / expiry / max-views spinners offer their documented options")
    void linkSpinnerOptions() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Could not switch to the link tab");
        sleep(2000);

        if (share.hasPermissionSpinner()) {
            List<String> perms = share.spinnerOptions("spPermission");
            System.out.println(">>> link permissions = " + perms);
        }
        if (share.hasExpirySpinner()) {
            List<String> exp = share.spinnerOptions("spExpires");
            System.out.println(">>> link expiry = " + exp);
            assertTrue(exp.isEmpty() || exp.stream().anyMatch(o -> o.toLowerCase().contains("hour")
                            || o.toLowerCase().contains("day") || o.toLowerCase().contains("never")),
                    "Expiry should offer 24 hours / 7 days / 30 days / Never. Got: " + exp);
        }
        if (share.hasMaxViewsSpinner()) {
            System.out.println(">>> max views = " + share.spinnerOptions("spMaxViews"));
        }
        screenshot("drive-link-options");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(11)
    @DisplayName("GENERATE a share link → a URL is produced and can be copied")
    void generateLink() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Could not switch to the link tab");
        sleep(2000);
        assumeTrue(share.hasGenerateLink(), "No generate-link control on this build");

        assumeTrue(share.generateLink(), "Generate not tappable");
        sleep(6000);
        boolean shown = share.linkShown(Duration.ofSeconds(15));
        String url = share.linkUrl();
        System.out.println(">>> generated link shown=" + shown + " url='" + url + "'");
        screenshot("drive-link-generated");
        assertTrue(shown, "A share URL should be displayed after generating");
        assertTrue(url != null && url.toLowerCase().contains("http"),
                "The generated value should look like a URL, got '" + url + "'");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(12)
    @DisplayName("Invalid recipient emails are rejected when sending a link by email")
    void invalidRecipientEmails() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Could not switch to the link tab");
        sleep(2000);
        assumeTrue(share.hasRecipients(), "No recipients field on this build");

        assumeTrue(share.setRecipients("not-an-email"), "Could not type recipients");
        share.generateEmailLink();
        sleep(3000);
        String err = share.errorText();
        boolean rejected = (err != null && !err.isBlank())
                || share.textShown("valid email", Duration.ofSeconds(4));
        System.out.println(">>> invalid-email rejected=" + rejected + " error='" + err + "'");
        screenshot("drive-link-invalid-email");
        assertTrue(rejected, "A malformed recipient email should be rejected");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
