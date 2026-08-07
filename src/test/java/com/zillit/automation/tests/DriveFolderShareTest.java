package com.zillit.automation.tests;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveSharePage;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * FOLDER SHARING, isolated — the open failure from DRIVE_COMPLETION_PLAN §2.1.
 *
 * <p>{@code DriveTwoDeviceE2ETest} cases 5 and 9 fail in BOTH directions while FILE
 * sharing passes, which is too symmetric to be a flake. That suite cannot say WHERE it
 * breaks, though, because one assertion covers the whole chain: grant → save → sync →
 * the recipient's list. This suite splits that chain into its links and reports each one:
 *
 * <ol>
 *   <li><b>Did the grant save at all?</b> Re-open the owner's own Share screen and read
 *       the access list back. If the user is missing here, nothing else matters — the
 *       bug is in granting on folders, and the recipient side is a red herring.</li>
 *   <li><b>Did it reach the recipient?</b> Poll the recipient's Shared with me → Folders.
 *       When it does not arrive, the failure message also reports what the recipient sees
 *       under Files, under My Drive, and via Search — so a folder that arrives
 *       MIS-CATEGORISED is distinguishable from one that never arrives at all.</li>
 * </ol>
 *
 * <p>Run it with both devices attached, signed in as DIFFERENT users on the SAME project:
 * <pre>
 *   ZILLIT_SENDER_UDID=&lt;A&gt; ZILLIT_RECEIVER_UDID=&lt;B&gt; ./run-and-report.sh --drive-folder-share
 * </pre>
 *
 * <p>Drives two drivers itself, so it does not extend {@code BaseTest}.
 */
@Order(TestOrder.DRIVE_FOLDER_SHARE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveFolderShareTest {

    private static final long STAMP = System.currentTimeMillis() % 1000000;
    private static final String FOLDER = "QA_FShare_" + STAMP;

    private static final String USER_A = System.getenv().getOrDefault("ZILLIT_USER_A", "Vidya Sagar");
    private static final String USER_B = System.getenv().getOrDefault("ZILLIT_USER_B", "Vidya Pixel");

    /** How long a share may take to reach the other device. */
    private static final Duration SYNC = Duration.ofSeconds(90);

    private AndroidDriver a;
    private AndroidDriver b;

    @BeforeAll
    void startBoth() {
        String udidA = System.getenv().getOrDefault("ZILLIT_SENDER_UDID", Config.get("zillit.udid"));
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(), "ZILLIT_RECEIVER_UDID is required — this suite needs TWO devices");
        assumeTrue(!udidA.equalsIgnoreCase(udidB), "The two devices must be different");
        // Ports 8220/8230 so this can never collide with DriveTwoDeviceE2ETest (8200/8210).
        a = DriverFactory.create(udidA, 8220);
        b = DriverFactory.create(udidB, 8230);
        System.out.println(">>> A=" + udidA + " (" + USER_A + ")   B=" + udidB + " (" + USER_B + ")");
    }

    @AfterAll
    void stopBoth() {
        for (AndroidDriver d : new AndroidDriver[]{a, b}) {
            if (d == null) continue;
            try { d.quit(); } catch (Exception ignored) { }
        }
    }

    // ---------------- navigation ----------------

    private boolean openDrive(AndroidDriver d, String label) {
        DriveHomePage drive = new DriveHomePage(d);
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
        for (int attempt = 0; attempt < 3; attempt++) {
            tools.openToolsTab();
            if (!tools.isLoaded(Duration.ofSeconds(15))) { sleep(2000); continue; }
            tools.searchTools("Drive");
            sleep(2000);
            if (tools.isModuleListed("Drive", Duration.ofSeconds(6)) && tools.openModule("Drive")
                    && drive.isLoaded(Duration.ofSeconds(40))) {
                return true;
            }
            if (drive.isDriveActivity()) return true;
            sleep(2000);
        }
        System.out.println(">>> " + label + ": could not open Drive");
        return false;
    }

    private boolean makeFolder(AndroidDriver d, String name) {
        DriveHomePage drive = new DriveHomePage(d);
        drive.showMyDrive();
        drive.showFolders();
        if (drive.hasItem(name)) return true;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (drive.openFab() && drive.tapCreateFolder()) {
                CreateFolderSheet sheet = new CreateFolderSheet(d);
                if (sheet.waitOpen(Duration.ofSeconds(10)) && sheet.createNamed(name)) break;
                try { d.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
            }
            sleep(2000);
        }
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (new DriveHomePage(d).hasItem(name)) return true;
            sleep(2500);
        }
        return false;
    }

    /**
     * Row ⋮ → Share, on whichever device.
     *
     * <p>Every step prints: a silent {@code assumeTrue} chain here just reports "skipped"
     * and leaves you guessing which link broke — the exact problem this suite exists to
     * fix for the E2E cases.
     */
    private DriveSharePage openShare(AndroidDriver d, String name) {
        DriveHomePage drive = new DriveHomePage(d);
        drive.showFolders();

        // Retry the TAP AND THE PROOF together. openItemMenu() only reports that it
        // clicked a ⋮ — right after a create, that click is regularly swallowed, and a
        // retry loop that trusts the return value stops after one useless attempt.
        DriveItemMenuPage menu = new DriveItemMenuPage(d);
        boolean menuOpen = false;
        for (int attempt = 0; attempt < 4 && !menuOpen; attempt++) {
            boolean clicked = drive.openItemMenu(name);
            menuOpen = clicked && menu.isOpen();
            System.out.println(">>> attempt " + (attempt + 1) + ": ⋮ clicked=" + clicked
                    + " menu open=" + menuOpen);
            if (!menuOpen) {
                try { d.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
            }
        }
        assumeTrue(menuOpen, "The row menu for " + name + " never opened");

        boolean tapped = menu.tapShare();
        System.out.println(">>> tapShare=" + tapped);
        assumeTrue(tapped, "Could not tap Share on " + name);

        DriveSharePage share = new DriveSharePage(d);
        boolean shareOpen = share.isOpen(Duration.ofSeconds(20));
        System.out.println(">>> share screen open=" + shareOpen);
        assumeTrue(shareOpen, "The Share screen did not open");
        return share;
    }

    /** Everything the recipient can see, for a failure message that names the cause. */
    private String recipientInventory() {
        DriveHomePage drive = new DriveHomePage(b);
        drive.showSharedWithMe();
        drive.showFolders();
        List<String> sharedFolders = drive.allItemNamesScrolled();
        drive.showFiles();
        List<String> sharedFiles = drive.allItemNamesScrolled();
        drive.showMyDrive();
        drive.showFolders();
        List<String> ownFolders = drive.allItemNamesScrolled();
        return "\n  Shared with me → Folders: " + sharedFolders
                + "\n  Shared with me → Files:   " + sharedFiles
                + "\n  My Drive → Folders:       " + ownFolders;
    }

    private boolean waitSharedFolder(AndroidDriver d, String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            DriveHomePage drive = new DriveHomePage(d);
            drive.showSharedWithMe();
            drive.showFolders();
            drive.pullToRefresh();
            sleep(3000);
            if (drive.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
        }
        return false;
    }

    // ==================== the two links of the chain ====================

    @Test
    @Order(1)
    @DisplayName("2.1a: A grants B access to a FOLDER and the grant survives reopening the Share screen")
    void folderGrantPersists() {
        boolean opened = openDrive(a, "A");
        System.out.println(">>> A openDrive=" + opened);
        assumeTrue(opened, "Could not open Drive on A");

        boolean created = makeFolder(a, FOLDER);
        System.out.println(">>> A created " + FOLDER + " = " + created
                + " list=" + new DriveHomePage(a).allItemNamesScrolled());
        assumeTrue(created, "A could not create " + FOLDER);

        DriveSharePage share = openShare(a, FOLDER);
        assumeTrue(share.noAccessYet() || share.grantedUsers().isEmpty(),
                "A brand-new folder should start with nobody having access");
        boolean shared = share.shareWith(USER_B, true, true, true);
        System.out.println(">>> shareWith(" + USER_B + ")=" + shared);
        assertTrue(shared,
                "A should be able to grant " + USER_B + " view+edit+download on a FOLDER");
        System.out.println(">>> after save, access list = " + share.grantedUsers());
        try { a.navigate().back(); Thread.sleep(2000); } catch (Exception ignored) { }

        // Re-open: the save is only real if the server gave it back. This is the link the
        // E2E suite could not see — if the grant is gone HERE, the recipient never had a
        // chance and the bug is in granting on folders, not in sync.
        DriveSharePage reopened = openShare(a, FOLDER);
        List<String> granted = reopened.grantedUsers();
        System.out.println(">>> reopened access list = " + granted);
        assertTrue(reopened.hasGrantFor(USER_B),
                "The grant to " + USER_B + " should still be listed after reopening the Share "
                        + "screen. Got: " + granted);
        try { a.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }
    }

    @Test
    @Order(2)
    @DisplayName("2.1b: the shared FOLDER reaches B's 'Shared with me' → Folders")
    void folderReachesRecipient() {
        assumeTrue(openDrive(b, "B"), "Could not open Drive on B");

        boolean arrived = waitSharedFolder(b, FOLDER, SYNC);
        System.out.println(">>> B sees " + FOLDER + " = " + arrived);
        if (!arrived) {
            // Name what B DOES see, so "never arrived" and "arrived in the wrong place"
            // are distinguishable without another run.
            System.out.println(">>> B's inventory:" + recipientInventory());
        }
        assertTrue(arrived,
                "A folder shared with " + USER_B + " should appear in their 'Shared with me' → "
                        + "Folders within " + SYNC.toSeconds() + "s. It did not."
                        + (arrived ? "" : recipientInventory()));
    }

    @Test
    @Order(3)
    @DisplayName("CLEANUP: A deletes the shared folder")
    void cleanup() {
        assumeTrue(openDrive(a, "A"), "Could not open Drive on A");
        DriveHomePage drive = new DriveHomePage(a);
        drive.showMyDrive();
        drive.showFolders();
        assumeTrue(drive.hasItem(FOLDER), "Nothing to clean up");
        assumeTrue(drive.openItemMenu(FOLDER), "Could not open the row overflow");

        DriveItemMenuPage menu = new DriveItemMenuPage(a);
        assumeTrue(menu.isOpen() && menu.tapDelete(), "Could not tap Delete");
        sleep(1500);
        drive.confirmDeleteDialog(Duration.ofSeconds(10));
        sleep(3000);

        DriveHomePage end = new DriveHomePage(a);
        end.showFolders();
        assertTrue(end.isListReadable(), "Drive should be readable after cleanup");
        List<String> left = end.allItemNamesScrolled();
        System.out.println(">>> folders after cleanup = " + left);
        assertTrue(left.stream().noneMatch(n -> n.equalsIgnoreCase(FOLDER)),
                "The QA folder should be gone after cleanup. Got: " + left);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
