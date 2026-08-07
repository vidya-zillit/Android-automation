package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveFolderPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveRequestFilesPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import io.appium.java_client.clipboard.HasClipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE FILE REQUESTS — area S of DRIVE_TEST_CASES.md: the "Request Files" sheet that
 * turns a folder into an upload inbox for people outside the app, and the read-only
 * "View File Requests" sheet that lists and revokes them.
 *
 * <p>Both entry points are <b>folder-scoped</b> — they do not exist on the Drive root
 * FAB — so this suite creates its own folder and works inside it.
 *
 * <p>⚠ The form's validation is a Toast ("Please enter a title for the request."), which
 * appears in neither the page source nor a uiautomator dump. Rejection is therefore
 * asserted as "the form is still showing and no request was created" (trap 3.2).
 */
@Order(TestOrder.DRIVE_REQUESTS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveRequestFilesTest extends BaseTest {

    private static final String STAMP = String.valueOf(System.currentTimeMillis() % 100000);
    /** This suite's own folder — file requests need one, and it must not be shared. */
    private static final String FOLDER = "QA_Req_" + STAMP;

    private static final String REQ_MAIN  = "QA Request " + STAMP;
    private static final String REQ_COPY  = "QA Request copy " + STAMP;
    private static final String REQ_RESET = "QA Request reset " + STAMP;
    private static final String RECIPIENT = "qa.recipient@example.com";

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
        // Retried: the FIRST test of a suite regularly finds the Tools list with "Drive"
        // typed and the row highlighted, yet Drive never comes up — the tap lands while
        // the filtered list is still settling. One attempt turns that into a skipped case
        // blamed on Drive.
        ToolsPage tools = new ToolsPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            tools.openToolsTab();
            if (!tools.isLoaded(Duration.ofSeconds(15))) { sleep(2000); continue; }
            tools.searchTools("Drive");
            sleep(2000);
            if (tools.isModuleListed("Drive", Duration.ofSeconds(6)) && tools.openModule("Drive")) {
                // 40s, not 20s: on a loaded emulator Drive's first open takes longer than
                // that, and the retry then pressed Tools again — cancelling a launch that
                // was about to succeed, over and over.
                if (drive.isLoaded(Duration.ofSeconds(40))) return true;
            }
            if (drive.isDriveActivity()) return true;
            sleep(2000);
        }
        return false;
    }

    private void closeAnyOverlay() {
        for (int i = 0; i < 6; i++) {
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean overlay = foreign
                    || !driver.findElements(byAppId("btnCreateRequest")).isEmpty()   // request sheet
                    || !driver.findElements(byAppId("btnDone")).isEmpty()            // view-requests sheet
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()
                    || !driver.findElements(byAppId("menuDelete")).isEmpty()
                    || !driver.findElements(byAppId("navTrash")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(1000); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    // ---------------- fixture ----------------

    /**
     * Creates this suite's folder once, then opens it.
     *
     * <p>Both taps are retried. Coming back to the list from another screen, the first
     * FAB tap is sometimes swallowed mid-render — a single attempt turns that into a
     * skipped case with a misleading reason.
     */
    private DriveFolderPage openFolder() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        if (!drive.hasItem(FOLDER)) {
            boolean created = false;
            for (int attempt = 0; attempt < 3 && !created; attempt++) {
                if (drive.openFab() && drive.tapCreateFolder()) {
                    CreateFolderSheet sheet = new CreateFolderSheet(driver);
                    if (sheet.waitOpen(Duration.ofSeconds(10)) && sheet.createNamed(FOLDER)) {
                        created = true;
                        break;
                    }
                    try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
                }
                sleep(2000);
            }
            assumeTrue(created, "Could not create " + FOLDER);
            sleep(3000);
        }
        assumeTrue(new DriveHomePage(driver).openItem(FOLDER), "Could not open " + FOLDER);
        sleep(2500);
        DriveFolderPage folder = new DriveFolderPage(driver);
        assumeTrue(folder.isLoaded(Duration.ofSeconds(15)), "Folder screen did not load");
        return folder;
    }

    /** FAB → Request Files, retried — the FAB menu entry can miss on the first tap. */
    private DriveRequestFilesPage openRequestSheet() {
        DriveFolderPage folder = openFolder();
        DriveRequestFilesPage req = new DriveRequestFilesPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            if (openFolderFab(folder) && folder.tapRequestFiles()
                    && req.isOpen(Duration.ofSeconds(15))) {
                return req;
            }
            System.out.println(">>> Request Files sheet did not open (attempt " + (attempt + 1) + ")");
            try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
        }
        assumeTrue(false, "The Request Files sheet did not open");
        return req;
    }

    private DriveRequestFilesPage openViewRequestsSheet() {
        DriveFolderPage folder = openFolder();
        DriveRequestFilesPage req = new DriveRequestFilesPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            if (openFolderFab(folder) && folder.tapViewFileRequests()
                    && req.isViewSheetOpen(Duration.ofSeconds(15))) {
                sleep(2500);
                return req;
            }
            System.out.println(">>> View File Requests sheet did not open (attempt " + (attempt + 1) + ")");
            try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
        }
        assumeTrue(false, "The View File Requests sheet did not open");
        return req;
    }

    private boolean openFolderFab(DriveFolderPage folder) {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (folder.openFab()) return true;
            sleep(1500);
        }
        return false;
    }

    /** Creates a request and leaves the sheet on its success card. */
    private DriveRequestFilesPage createRequest(String title) {
        DriveRequestFilesPage req = openRequestSheet();
        boolean typed = req.setTitle(title);
        System.out.println(">>> createRequest('" + title + "') typed=" + typed
                + " sheetOpen=" + req.isOpen(Duration.ofSeconds(2)));
        assumeTrue(typed, "Could not type the request title");

        boolean tapped = req.create();
        System.out.println(">>> create tapped=" + tapped
                + " form=" + req.isFormShown() + " sheetOpen=" + req.isOpen(Duration.ofSeconds(2)));
        assumeTrue(tapped, "Could not tap Create request");
        assumeTrue(req.isSuccessShown(), "The request was not created (no success card appeared)");
        return req;
    }

    private String clipboard() {
        try { return ((HasClipboard) driver).getClipboardText(); }
        catch (WebDriverException e) { return null; }
    }

    // ==================== S · FILE REQUESTS ====================

    @Test
    @Order(1)
    @DisplayName("S-1: Request Files and View File Requests are offered INSIDE a folder")
    void requestFilesOfferedInFolder() {
        DriveFolderPage folder = openFolder();
        assumeTrue(folder.openFab(), "Folder FAB did not open");

        assertTrue(folder.fabHasRequestFiles(), "'Request Files' should be offered inside a folder");
        assertTrue(folder.fabHasViewFileRequests(), "'View File Requests' should be offered inside a folder");
        screenshot("drive-request-fab");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(2)
    @DisplayName("S-2: the request form shows every field")
    void formFields() {
        DriveRequestFilesPage req = openRequestSheet();

        assertTrue(req.hasTitleField(), "Title field expected");
        assertTrue(req.hasInstructionsField(), "Instructions field expected");
        assertTrue(req.hasExpirySpinner(), "Expiry spinner expected");
        assertTrue(req.hasMaxFilesSpinner(), "Max-files spinner expected");
        assertTrue(req.hasSizeSpinner(), "Size spinner expected");
        assertTrue(req.hasFileTypeSpinner(), "File-type spinner expected");
        assertTrue(req.hasRequireEmail(), "'Require uploader email' expected");
        assertTrue(req.hasRequireName(), "'Require uploader name' expected");
        assertTrue(req.hasRecipientInput(), "Recipient input expected");
        assertTrue(req.hasCreateButton(), "Create-request button expected");
        System.out.println(">>> thankYou=" + req.hasThankYouField()
                + " requests=" + req.requestsCount());
        screenshot("drive-request-form");
        req.close();
    }

    @Test
    @Order(3)
    @DisplayName("S-4: the destination line names the folder uploads will land in")
    void destinationNamesFolder() {
        DriveRequestFilesPage req = openRequestSheet();

        String dest = req.destinationInfo();
        System.out.println(">>> destination='" + dest + "'");
        screenshot("drive-request-destination");
        assertTrue(dest.toLowerCase().contains(FOLDER.toLowerCase()),
                "The destination line should name '" + FOLDER + "'. Got: '" + dest + "'");
        req.close();
    }

    @Test
    @Order(4)
    @DisplayName("S-3: an EMPTY title is rejected — the form stays and no request is created")
    void emptyTitleRejected() {
        DriveRequestFilesPage req = openRequestSheet();

        String countBefore = req.requestsCount();
        assumeTrue(req.setTitle(""), "Could not clear the title");
        req.create();
        sleep(2500);

        // The rejection is a Toast — invisible to the tree. Assert on behaviour instead:
        // the form did not hand over to the success card.
        assertTrue(req.isFormShown(), "The form should still be showing after an empty title");
        assertFalse(req.isSuccessShown(Duration.ofSeconds(5)),
                "No success card should appear — an untitled request must not be created");
        System.out.println(">>> requests before='" + countBefore + "' after='" + req.requestsCount() + "'");
        screenshot("drive-request-empty-title");
        req.close();
    }

    @Test
    @Order(5)
    @DisplayName("S-5: a recipient email becomes a chip")
    void recipientChip() {
        DriveRequestFilesPage req = openRequestSheet();

        assumeTrue(req.addRecipient(RECIPIENT), "Could not type a recipient");
        sleep(1500);
        List<String> chips = req.recipientChips();
        System.out.println(">>> recipient chips = " + chips);
        screenshot("drive-request-recipient-chip");
        assertTrue(chips.stream().anyMatch(c -> c.equalsIgnoreCase(RECIPIENT)),
                "The committed email should become a chip. Got: " + chips);
        req.close();
    }

    @Test
    @Order(6)
    @DisplayName("S-6: Create produces the success state with a shareable link")
    void createRequestProducesLink() {
        DriveRequestFilesPage req = createRequest(REQ_MAIN);

        String url = req.createdUrl();
        System.out.println(">>> created url='" + url + "' chips=" + req.createdChips());
        screenshot("drive-request-created");
        assertTrue(req.createdBannerShown(), "A '✓ Request created' confirmation is expected");
        assertTrue(url != null && url.startsWith("http"),
                "The success card should show an upload URL. Got: '" + url + "'");
        req.close();
    }

    @Test
    @Order(7)
    @DisplayName("S-7: Copy puts the created link on the clipboard")
    void copyCreatedLink() {
        DriveRequestFilesPage req = createRequest(REQ_COPY);
        String url = req.createdUrl();
        assumeTrue(url != null && url.startsWith("http"), "No URL to copy");

        // Seed a sentinel first: without it, a clipboard that already held this URL (or
        // a copy button that does nothing) would look like a pass.
        String sentinel = "zl-sentinel-" + STAMP;
        try { ((HasClipboard) driver).setClipboardText(sentinel); }
        catch (WebDriverException e) { assumeTrue(false, "Clipboard is not writable on this device"); }

        assumeTrue(req.copyCreatedUrl(), "Could not tap Copy");
        sleep(1500);
        String clip = clipboard();
        System.out.println(">>> url='" + url + "' clipboard='" + clip + "'");
        screenshot("drive-request-copy-link");

        assumeTrue(clip != null && !clip.isBlank(),
                "The clipboard could not be read on this device — copy is unverifiable here");
        assertFalse(sentinel.equals(clip), "Copy did not change the clipboard — nothing was copied");
        assertTrue(clip.contains(url) || url.contains(clip),
                "The clipboard should hold the created link. url='" + url + "' clipboard='" + clip + "'");
    }

    @Test
    @Order(8)
    @DisplayName("S-8: 'Create another' returns to a blank form")
    void createAnotherResetsForm() {
        DriveRequestFilesPage req = createRequest(REQ_RESET);

        assumeTrue(req.createAnother(), "Could not tap 'Create another request'");
        sleep(2000);
        String title = req.titleText();
        System.out.println(">>> after reset: form=" + req.isFormShown() + " title='" + title + "'");
        screenshot("drive-request-create-another");
        assertTrue(req.isFormShown(), "The form should be showing again");
        assertFalse(req.isSuccessShown(), "The success card should be gone");
        // The hint remains when the field is empty, so an empty field can read as the hint.
        assertFalse(title.equals(REQ_RESET), "The title field should have been cleared, was '" + title + "'");
        req.close();
    }

    @Test
    @Order(9)
    @DisplayName("S-9: View File Requests lists the created request")
    void viewFileRequestsListsIt() {
        DriveRequestFilesPage req = openViewRequestsSheet();

        boolean listed = req.waitForViewSheetRequest(REQ_MAIN, Duration.ofSeconds(30));
        System.out.println(">>> view sheet cards = " + req.viewSheetCardTexts());
        screenshot("drive-request-view-list");
        assertTrue(listed,
                "'" + REQ_MAIN + "' should be listed among the active requests. Got: "
                        + req.viewSheetCardTexts());
        req.viewSheetDone();
    }

    @Test
    @Order(10)
    @DisplayName("S-10: Revoke asks for confirmation and says the link stops working")
    void revokeAsksConfirmation() {
        DriveRequestFilesPage req = openViewRequestsSheet();
        assumeTrue(req.waitForViewSheetRequest(REQ_MAIN, Duration.ofSeconds(30)), "Request not listed");

        assumeTrue(req.openCardMenu(REQ_MAIN), "Could not open the request's ⋯ menu");
        assertTrue(req.menuHasRevoke(), "The menu should offer 'Revoke request'");
        assumeTrue(req.tapRevokeMenu(), "Could not tap Revoke request");

        assertTrue(req.revokeConfirmShown(),
                "Revoking must warn that the upload link stops working immediately");
        screenshot("drive-request-revoke-confirm");

        // Cancel — S-11 does the destructive half, so this case proves the GUARD only.
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button2\")")).click();
        } catch (WebDriverException ignored) { }
        sleep(1500);
        assertTrue(req.viewSheetHasRequest(REQ_MAIN),
                "Cancelling the confirmation must leave the request alive. Got: "
                        + req.viewSheetCardTexts());
        req.viewSheetDone();
    }

    @Test
    @Order(11)
    @DisplayName("S-11: a revoked request disappears from the list")
    void revokedRequestDisappears() {
        DriveRequestFilesPage req = openViewRequestsSheet();
        assumeTrue(req.waitForViewSheetRequest(REQ_MAIN, Duration.ofSeconds(30)), "Request not listed");

        assumeTrue(req.openCardMenu(REQ_MAIN), "Could not open the request's ⋯ menu");
        assumeTrue(req.tapRevokeMenu(), "Could not tap Revoke request");
        assumeTrue(req.revokeConfirmShown(), "No confirmation appeared");
        assertTrue(req.confirmRevoke(Duration.ofSeconds(10)), "Revoke should be confirmable");
        sleep(3000);

        // The sheet must still be READABLE before "it is gone" means anything — a
        // dialog left on top empties the card list and would fake a pass.
        assertTrue(req.isViewSheetOpen(Duration.ofSeconds(10)),
                "The requests sheet should still be open after revoking");
        assertTrue(req.waitForViewSheetRequestGone(REQ_MAIN, Duration.ofSeconds(30)),
                "The revoked request should disappear. Got: " + req.viewSheetCardTexts());
        screenshot("drive-request-revoked");
        req.viewSheetDone();
    }

    @Test
    @Order(12)
    @DisplayName("CLEANUP: revoke the remaining requests and delete the QA folder")
    void cleanup() {
        DriveRequestFilesPage req = openViewRequestsSheet();
        for (String title : new String[]{REQ_COPY, REQ_RESET}) {
            if (!req.viewSheetHasRequest(title)) continue;
            if (!req.openCardMenu(title)) continue;
            if (req.tapRevokeMenu() && req.revokeConfirmShown()) {
                req.confirmRevoke(Duration.ofSeconds(8));
                sleep(2500);
            }
        }
        System.out.println(">>> requests left = " + req.viewSheetCardTexts());
        req.viewSheetDone();
        sleep(1500);

        // Back out of the folder, then delete it from the root list. The exit condition is
        // the ACTIVITY, not isHome(): a folder screen has fabAdd as well, so "am I home?"
        // said yes from inside the folder, the delete then ran against the folder's own
        // (empty) contents, and cleanup reported success while the folder was still there.
        DriveFolderPage open = new DriveFolderPage(driver);
        for (int i = 0; i < 5 && open.isFolderActivity(); i++) {
            try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
        }
        DriveHomePage home = new DriveHomePage(driver);
        assumeTrue(home.isDriveActivity() && home.isHome(Duration.ofSeconds(10)),
                "Could not get back to the Drive root to delete " + FOLDER);
        home.showFolders();
        if (home.hasItem(FOLDER) && home.openItemMenu(FOLDER)) {
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                home.confirmDeleteDialog(Duration.ofSeconds(10));
                sleep(3000);
            } else {
                menu.dismiss();
            }
        }
        DriveHomePage end = new DriveHomePage(driver);
        end.showFolders();
        assertTrue(end.isListReadable(), "Drive should be readable after cleanup");
        List<String> folders = end.allItemNamesScrolled();
        System.out.println(">>> folders after cleanup = " + folders);
        // Assert the folder really went: a cleanup that quietly does nothing leaves QA
        // folders piling up on a shared project, and it looked green for a whole run.
        assertFalse(folders.stream().anyMatch(n -> n.equalsIgnoreCase(FOLDER)),
                "The QA folder should be gone after cleanup. Got: " + folders);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
