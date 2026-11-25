package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestEmails;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.CreateFolderSheet;
import com.zillit.automation.pages.drive.DriveFileDetailPage;
import com.zillit.automation.pages.drive.DriveFlows;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveItemMenuPage;
import com.zillit.automation.pages.drive.DriveSharePage;
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
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE_SHARE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveShareTest extends BaseTest {

    private static final String SHARE_USER =
            System.getenv().getOrDefault("ZILLIT_SHARE_USER", "Vidya Pixel");
    private static final String DOC = "zl_drive_doc.pdf";
    private static final String SHARED_FOLDER = "QA_Shared_" + (System.currentTimeMillis() % 100000);
    /** Created and deleted by the no-access case, so it is new on every run. */
    private static final String FRESH_FOLDER = "QA_NoAccess_" + (System.currentTimeMillis() % 100000);

    private boolean insideProject;
    /** True only when ensureFile() uploaded DOC in THIS run (so nobody can have been granted it yet). */
    private boolean docUploadedHere;

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
        boolean listed = new DriveHomePage(driver).hasFile(DOC);
        docUploadedHere = listed;
        return listed;
    }

    /**
     * Reads the People-tab access list the way ShareFragment renders it
     * (Zillit-Android {@code drive/presentation/share/ShareFragment.kt:634-654}): the
     * creator is filtered out of {@code accessContainer}, and {@code tvNoAccess}
     * ("No one has access yet. Add people above to share.") is VISIBLE only when the
     * filtered list is empty and loading has finished. Before the load starts the hint
     * can flash, and before {@code creatorUserId} resolves the creator can show — so let
     * the screen settle first.
     *
     * @return the texts listed under who-has-access (empty when nobody is listed)
     */
    private List<String> settledAccessList(DriveSharePage share) {
        sleep(3000);
        return share.grantedUsers();
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
        // DOC is a fixed fixture name that this suite SHARES later (case 6) and that
        // persists between runs, so it only counts as "new" when this run uploaded it.
        // Otherwise the access list legitimately holds the earlier grant — skip, don't fail.
        assumeTrue(docUploadedHere,
                DOC + " already existed (and may have been shared by an earlier run) — "
                        + "the fresh-folder case covers 'new item has no access'");
        DriveSharePage share = openShareFor(DOC);
        List<String> access = settledAccessList(share);
        boolean none = share.noAccessYet();
        System.out.println(">>> 'no one has access yet' = " + none + " access=" + access);
        screenshot("drive-share-no-access");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(none, "A newly uploaded file should show 'No one has access yet' (tvNoAccess). "
                + "Access list: " + access);
        assertTrue(access.isEmpty(),
                "A newly uploaded file should list nobody under who-has-access. Got: " + access);
    }

    @Test
    @Order(2)
    @DisplayName("A newly created folder starts with NO ONE having access")
    void newFolderStartsWithNoAccess() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.openFab() && drive.tapCreateFolder(), "Could not open Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Create sheet did not open");
        assumeTrue(sheet.createNamed(FRESH_FOLDER), "Could not create " + FRESH_FOLDER);
        sleep(2500);
        drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.hasItem(FRESH_FOLDER), FRESH_FOLDER + " is not listed after create");

        DriveSharePage share = openShareFor(FRESH_FOLDER);
        List<String> access = settledAccessList(share);
        boolean none = share.noAccessYet();
        System.out.println(">>> new folder: 'no one has access yet' = " + none + " access=" + access);
        screenshot("drive-share-folder-no-access");
        try { driver.navigate().back(); } catch (Exception ignored) { }

        // Best-effort cleanup before asserting, so a failure does not strand the folder.
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        if (home.openItemMenu(FRESH_FOLDER)) {
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                home.confirmDeleteDialog(Duration.ofSeconds(10));
                sleep(2000);
            }
        }

        assertTrue(none, "A newly created folder should show 'No one has access yet' (tvNoAccess). "
                + "Access list: " + access);
        assertTrue(access.isEmpty(),
                "A newly created folder should list nobody under who-has-access. Got: " + access);
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
    @DisplayName("GENERATE a link with no recipients → a new 'Active share links' row (expiry, views)")
    void generateLink() {
        assumeTrue(ensureFile(), "No file to share");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Could not switch to the link tab");
        sleep(2000);
        assumeTrue(share.hasGenerateLink(), "No generate-link control on this build");
        // Build 32539: the link tab no longer shows a URL field (tvShareLinkUrl); a generated link
        // is listed under "Active share links" in emailLinksContainer, and the copyable URL
        // lives on File details → Share (see L-05).
        List<String> before = share.emailLinkTexts().stream().filter(t -> t.startsWith("Expires")).toList();
        share.setRecipients("");
        assumeTrue(share.generateLink(), "Generate not tappable");
        // The newest row is listed first, with an expiry to the minute — so a new link shows up
        // either as an extra row or as an "Expires: …" line that was not there before.
        boolean added = false;
        List<String> after = before;
        for (int i = 0; i < 15 && !added; i++) {
            sleep(1000);
            after = share.emailLinkTexts().stream().filter(t -> t.startsWith("Expires")).toList();
            added = after.size() > before.size() || after.stream().anyMatch(t -> !before.contains(t));
        }
        System.out.println(">>> active links before=" + before + " after=" + after);
        screenshot("drive-link-generated");
        assertTrue(added, "Generating should add an Active share links row: " + share.emailLinkTexts());
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
        // ShareFragment.onGenerateEmailLink rejects with a TOAST, "Please enter valid email
        // addresses." — read it straight away (toasts last ~2 s), then the older inline error.
        boolean toast = share.toastShown("Please enter valid email addresses.", Duration.ofSeconds(6));
        String err = share.errorText();
        boolean rejected = toast || (err != null && !err.isBlank())
                || share.textShown("valid email", Duration.ofSeconds(2));
        System.out.println(">>> invalid-email rejected=" + rejected + " error='" + err + "'");
        screenshot("drive-link-invalid-email");
        assertTrue(rejected, "A malformed recipient email should be rejected");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    // ==================== "Can edit" emailed links + copy-link expiry (fe79a4620, 0c909b7fb) ====================

    /** An office copy this suite owns — "Can edit" exists only for xlsx / xlsm / docx / pptx. */
    private static final String SHARE_XLSX = "QA_Share_" + (System.currentTimeMillis() % 100000) + ".xlsx";
    private static boolean shareXlsxUploaded;

    private DriveSharePage linkTabFor(String name) {
        DriveFlows f = new DriveFlows(driver);
        if (!shareXlsxUploaded && name.equals(SHARE_XLSX)) {
            assumeTrue(f.uploadAs("zl_drive_book.xlsx", SHARE_XLSX), "Could not upload " + SHARE_XLSX);
            shareXlsxUploaded = true;
        }
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(name);
        assumeTrue(share.openLinkTab(), "Could not switch to the link tab");
        sleep(2000);
        assumeTrue(share.hasPermissionSpinner(), "No link permission spinner");
        return share;
    }

    private DriveSharePage canEditTab() {
        DriveSharePage share = linkTabFor(SHARE_XLSX);
        List<String> perms = share.spinnerOptions("spPermission");
        assumeTrue(perms.contains("Can edit"), "Link permissions for an xlsx are " + perms
                + " — no 'Can edit'; this build predates Zillit-Android fe79a4620");
        assertTrue(share.selectSpinner("spPermission", "Can edit"), "Could not choose Can edit");
        return share;
    }

    @Test
    @Order(13)
    @DisplayName("L-01 'Can edit' is offered for an xlsx link but NOT for a PDF")
    void canEditOnlyForOffice() {
        DriveSharePage share = linkTabFor(SHARE_XLSX);
        List<String> xlsx = share.spinnerOptions("spPermission");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        System.out.println(">>> L-01 xlsx permissions " + xlsx);
        assumeTrue(xlsx.contains("Can edit"), "No 'Can edit' for an xlsx — this build predates fe79a4620: " + xlsx);
        assumeTrue(ensureFile(), "No PDF to compare with");
        DriveSharePage pdf = linkTabFor(DOC);
        List<String> pdfPerms = pdf.spinnerOptions("spPermission");
        System.out.println(">>> L-01 pdf permissions " + pdfPerms);
        assertFalse(pdfPerms.contains("Can edit"), "A PDF can never be shared for editing: " + pdfPerms);
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(14)
    @DisplayName("L-02 Choosing 'Can edit' drops 'Never' from the expiry (24 hours / 7 days / 30 days)")
    void canEditDropsNever() {
        DriveSharePage share = canEditTab();
        List<String> exp = share.spinnerOptions("spExpires");
        System.out.println(">>> L-02 expiry with Can edit " + exp);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertFalse(exp.contains("Never"), "An edit link must expire: " + exp);
        assertTrue(exp.containsAll(List.of("24 hours", "7 days", "30 days")), "Expected 24 hours / 7 days / 30 days: " + exp);
    }

    @Test
    @Order(15)
    @DisplayName("L-03 'Can edit' with no recipient is refused: 'Add at least one email to share with edit access.'")
    void canEditNeedsRecipient() {
        DriveSharePage share = canEditTab();
        share.setRecipients("");
        assertTrue(share.generateEmailLink(), "Generate not tappable");
        boolean toast = share.toastShown("Add at least one email to share with edit access.", Duration.ofSeconds(8));
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(toast, "An edit link without a recipient must be refused with the documented message");
    }

    @Test
    @Order(16)
    @DisplayName("L-04 A 'Can edit' link to the test recipient lists with a 'Can edit' badge and 'Not edited yet'")
    void canEditLinkRow() {
        DriveSharePage share = canEditTab();
        // The ONLY address automation may send to — the project's own second test account.
        String to = TestEmails.recipient();
        assertTrue(share.setRecipients(to), "Could not enter the recipient");
        assertTrue(share.generateEmailLink(), "Generate not tappable");
        sleep(6000);
        List<String> rows = share.emailLinkTexts();
        System.out.println(">>> L-04 link rows " + rows);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(rows.stream().anyMatch(t -> t.equals("Can edit")), "The new link should carry a 'Can edit' badge: " + rows);
        assertTrue(rows.stream().anyMatch(t -> t.contains("Not edited yet")), "A fresh edit link reads 'Not edited yet': " + rows);
    }

    @Test
    @Order(17)
    @DisplayName("L-05 File details Share asks for a copy-link expiry of 1 hour / 24 hours / 7 days only")
    void copyLinkExpiryOptions() {
        assumeTrue(ensureFile(), "No file");
        DriveFileDetailPage d = new DriveFlows(driver).openDetail(DOC);
        assumeTrue(d != null && d.waitFor("btnShare", Duration.ofSeconds(20)), "No Share on File details");
        List<String> opts = d.openShareLinkExpiryOptions();
        System.out.println(">>> L-05 copy-link expiry " + opts);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assumeTrue(!opts.isEmpty(), "File details Share did not show a 'Share Link Expiry' list on this build");
        assertEquals(List.of("1 hour", "24 hours", "7 days"), opts, "30 days was removed in 0c909b7fb");
    }

    @Test
    @Order(18)
    @DisplayName("L CLEANUP: remove the QA share spreadsheet")
    void canEditCleanup() {
        assumeTrue(shareXlsxUploaded, "Nothing was uploaded");
        assertTrue(new DriveFlows(driver).purge(SHARE_XLSX, false), "Could not remove " + SHARE_XLSX);
    }

    // =====================================================================================
    // WEB GAP cases (web-e2e/gaps/drive.md M04 copy-link card, M05 revoke, M14 create-folder
    // access, M27 share-screen details, M28 link button label).
    // =====================================================================================

    private static final long GS = System.currentTimeMillis() % 100000;
    private static final String G_ACC1 = "QA_Acc1_" + GS;
    private static final String G_ACC2 = "QA_Acc2_" + GS;
    private static final java.util.Set<String> G_FOLDERS = new java.util.LinkedHashSet<>();

    private DriveFlows gFlows() { return new DriveFlows(driver); }

    @Test
    @Order(30)
    @DisplayName("[WEB L536] COPY LINK: 7 days makes an http link card whose expiry is ~7 days out, and Copy puts it on the clipboard")
    void gCopyLinkCard() throws Exception {
        assumeTrue(ensureFile(), "No file");
        DriveFileDetailPage d = gFlows().openDetail(DOC);
        assumeTrue(d != null && d.waitFor("btnShare", Duration.ofSeconds(20)), "No Share on the detail");
        assertTrue(d.makeCopyLink("7 days"), "Choosing 7 days should produce a link card");
        String url = d.shareLink();
        String exp = d.shareLinkExpiry();
        System.out.println(">>> copy link " + url + " | " + exp);
        assertTrue(url.startsWith("http"), "The link should be a URL: " + url);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Expires: (\\w{3} \\d{2}, \\d{4})").matcher(exp);
        assertTrue(m.find(), "Expiry should read 'Expires: MMM dd, yyyy at HH:mm', was '" + exp + "'");
        java.time.LocalDate when = java.time.LocalDate.parse(m.group(1),
                java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.ENGLISH));
        long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), when);
        assertTrue(days >= 6 && days <= 8, "A 7-day link should expire ~7 days out, got " + when + " (" + days + " days)");
        driver.setClipboardText("zl-sentinel-" + GS);
        assertTrue(d.copyShareLink(), "Copy not tappable");
        sleep(1500);
        assertEquals(url, driver.getClipboardText(), "Copy should put the link on the clipboard");
    }

    @Test
    @Order(31)
    @DisplayName("[WEB L512/L524] LINKS: a generated link is listed; Revoke asks first and removes it")
    void gRevokeLink() {
        assumeTrue(ensureFile(), "No file");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Link tab");
        sleep(2000);
        share.setRecipients("");
        // Rows are newest-first and the list is longer than the screen, so a row is identified
        // by its "Expires: <minute>" line rather than by counting what happens to be visible.
        assertTrue(share.generateLink(), "Generate not tappable");
        sleep(6000);
        String mine = share.firstLinkExpiry();
        String msg = share.revokeFirstLink();
        sleep(3500);
        String firstAfter = share.firstLinkExpiry();
        boolean emptyNow = share.textShown("No active share links", Duration.ofSeconds(2));
        System.out.println(">>> revoke: mine='" + mine + "' first after='" + firstAfter + "' empty=" + emptyNow + " msg='" + msg + "'");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(msg != null, "Each active link should have a 'Revoke link' action");
        assertTrue(msg.contains("stop working"), "Revoke should warn that recipient URLs stop working: '" + msg + "'");
        assertTrue(emptyNow || !mine.equals(firstAfter),
                "The revoked link (" + mine + ") should leave the top of Active share links");
    }

    @Test
    @Order(32)
    @DisplayName("[WEB L1089] LINKS: the button reads 'Generate link' with no recipients and 'Send link by email' with some")
    void gLinkButtonLabel() {
        assumeTrue(ensureFile(), "No file");
        new DriveHomePage(driver).showFiles();
        DriveSharePage share = openShareFor(DOC);
        assumeTrue(share.openLinkTab(), "Link tab");
        sleep(1500);
        share.setRecipients("");
        String empty = share.generateButtonLabel();
        share.setRecipients(TestEmails.recipient() + "; " + TestEmails.self());
        sleep(800);
        String filled = share.generateButtonLabel();
        share.setRecipients("");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        System.out.println(">>> link button '" + empty + "' / '" + filled + "'");
        assertEquals("Generate link", empty, "No recipients");
        assertEquals("Send link by email", filled, "With recipients (split on ';' and space)");
    }

    @Test
    @Order(33)
    @DisplayName("[WEB L484/L491] SHARE SCREEN: titles per kind; no Save bar before a change; folder roles Viewer/Editor/Owner")
    void gShareScreenDetails() {
        assumeTrue(ensureFile(), "No file");
        new DriveHomePage(driver).showFiles();
        DriveSharePage file = openShareFor(DOC);
        String fileTitle = file.screenTitle();
        // ShareFragment shows the save bar whenever the file has grantees, but Save Permissions
        // stays DISABLED until something changes — that is Android's "no empty save".
        boolean barBefore = file.saveEnabled();
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        assertEquals("Manage File Access", fileTitle, "File share title");
        assertFalse(barBefore, "Save Permissions must not be tappable before anything changed");

        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        if (!home.hasItem(G_ACC2)) assumeTrue(gFlows().createFolder(G_ACC2), "Could not create " + G_ACC2);
        G_FOLDERS.add(G_ACC2);
        home.showFolders();
        DriveSharePage folder = openShareFor(G_ACC2);
        String folderTitle = folder.screenTitle();
        boolean added = folder.searchUser(SHARE_USER) && folder.userListed(SHARE_USER) && folder.addUser(SHARE_USER);
        List<String> roles = folder.folderRoleLabels(SHARE_USER);
        System.out.println(">>> folder share title='" + folderTitle + "' added=" + added + " roles=" + roles);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertEquals("Manage Folder Access", folderTitle, "Folder share title");
        assumeTrue(added, "Could not add " + SHARE_USER + " to read the role controls");
        assertEquals(List.of("Viewer", "Editor", "Owner"), roles, "Folder roles");
    }

    @Test
    @Order(34)
    @DisplayName("[WEB L301] CREATE FOLDER with Specific Users = the test user grants exactly that user, as Viewer")
    void gCreateFolderSpecificUser() {
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assumeTrue(home.openFab() && home.tapCreateFolder(), "Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet");
        sheet.setName(G_ACC1);
        assertTrue(sheet.chooseSpecificUsers(), "Specific Users");
        assertTrue(sheet.memberSearchShown(), "Member search should show for Specific Users");
        assertTrue(sheet.tickMember(SHARE_USER), "Could not tick " + SHARE_USER);
        assertTrue(sheet.create(), "Create");
        sleep(4000);
        G_FOLDERS.add(G_ACC1);
        home.showFolders();
        DriveSharePage share = openShareFor(G_ACC1);
        sleep(3000);
        List<String> granted = share.grantedUsers();
        String role = share.currentRole(SHARE_USER);
        System.out.println(">>> " + G_ACC1 + " access=" + granted + " role=" + role);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(share.hasGrantFor(SHARE_USER) || granted.stream().anyMatch(t -> t.contains(SHARE_USER)),
                SHARE_USER + " should have access: " + granted);
        assertEquals("viewer", role, "The default role for a picked member is Viewer");
    }

    @Test
    @Order(35)
    @DisplayName("[WEB L1628/L1640] CREATE FOLDER: Project Users with no role keeps it private; the sheet starts clean next time")
    void gCreateFolderProjectNoRoleAndReset() {
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        String name = "QA_Acc3_" + GS;
        assumeTrue(home.openFab() && home.tapCreateFolder(), "Create Folder");
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet");
        sheet.setName(name);
        assertTrue(sheet.chooseProjectUsers(), "Project Users");
        assertTrue(sheet.create(), "Create");
        sleep(4000);
        G_FOLDERS.add(name);
        home.showFolders();
        DriveSharePage share = openShareFor(name);
        sleep(3000);
        boolean none = share.noAccessYet();
        List<String> granted = share.grantedUsers();
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(none, "Project Users with no permission picked must grant nobody: " + granted);
        assumeTrue(home.openFab() && home.tapCreateFolder(), "Create Folder again");
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet");
        // Start a Specific Users pick, abandon the sheet, reopen: it must start clean
        // (Project Users default, nobody selected) — web L1640.
        sheet.chooseSpecificUsers();
        sheet.tickMember(SHARE_USER);
        sheet.close();
        sleep(1500);
        assumeTrue(home.openFab() && home.tapCreateFolder(), "Create Folder once more");
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(10)), "Sheet");
        String count = sheet.selectedCountText();
        boolean specific = sheet.specificModeChecked();
        sheet.close();
        System.out.println(">>> reopened sheet selected='" + count + "' specific=" + specific);
        assertFalse(specific, "A reopened Create Folder sheet should start on the default mode, not the last one");
        assertFalse(count.startsWith("1 "), "A reopened sheet must not keep the last picked user: '" + count + "'");
    }

    @Test
    @Order(98)
    @DisplayName("WEB GAP CLEANUP: remove the QA_ share folders")
    void gShareCleanup() {
        List<String> failed = new java.util.ArrayList<>();
        for (String f : G_FOLDERS) if (!gFlows().purge(f, true)) failed.add(f);
        // The suite's own folders from the original cases (they were never removed before).
        gFlows().purge(SHARED_FOLDER, true);
        gFlows().purge(FRESH_FOLDER, true);
        assertTrue(failed.isEmpty(), "Could not remove: " + failed);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
