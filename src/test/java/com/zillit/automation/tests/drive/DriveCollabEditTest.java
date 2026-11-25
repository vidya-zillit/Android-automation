package com.zillit.automation.tests.drive;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.DriveEditorPage;
import com.zillit.automation.pages.drive.DriveFileContent;
import com.zillit.automation.pages.drive.DriveFileDetailPage;
import com.zillit.automation.pages.drive.DriveFlows;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.common.ProjectListPage;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * COLLABORATIVE EDITING — the same spreadsheet open in the Zillit Sheets editor on TWO devices
 * at once, as two different users.
 *
 * <p>Rewritten for the in-house editor. The grid is a canvas, so a cell typed on A cannot be READ
 * on B from the accessibility tree; what the page does expose is presence — {@code #people}
 * "People in this file" lists everyone in the document. So this suite proves:
 * <ol>
 *   <li>both users can hold the file open in EDIT mode at the same time;</li>
 *   <li>each sees the other appear under "People in this file" (live session);</li>
 *   <li>each types a marker into an empty part of the sheet;</li>
 *   <li>after both leave (Skip on the notify prompt), the file A downloads contains BOTH markers —
 *       collaboration is not last-writer-wins.</li>
 * </ol>
 *
 * <pre>
 *   ZILLIT_SENDER_UDID=&lt;device A&gt; ZILLIT_RECEIVER_UDID=&lt;device B&gt; \
 *     ./run-and-report.sh --drive-collab
 * </pre>
 *
 * <p>Both devices must be signed in as DIFFERENT users on the SAME project, and B needs edit
 * rights on A's {@code zl_drive_book.xlsx} (DriveShareTest / DriveTwoDeviceE2ETest grant it).
 * It does not extend {@code BaseTest}: it drives two drivers itself.
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE_COLLAB)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveCollabEditTest {

    private static final String XLSX = "zl_drive_book.xlsx";
    /** How long presence may take to reach the other device. */
    private static final Duration SYNC = Duration.ofSeconds(45);

    private AndroidDriver driverA;
    private AndroidDriver driverB;
    private String markerA;
    private String markerB;

    @BeforeAll
    void startBothDevices() {
        String udidA = System.getenv().getOrDefault("ZILLIT_SENDER_UDID", Config.get("zillit.udid"));
        String udidB = System.getenv().getOrDefault("ZILLIT_RECEIVER_UDID", "");
        assumeTrue(!udidB.isBlank(),
                "ZILLIT_RECEIVER_UDID is required — collaborative editing needs a SECOND device");
        assumeTrue(!udidA.equalsIgnoreCase(udidB), "The two devices must be different");
        driverA = DriverFactory.create(udidA, 8200);
        driverB = DriverFactory.create(udidB, 8210);
        System.out.println(">>> device A=" + udidA + "  device B=" + udidB);
    }

    @AfterAll
    void stopBothDevices() {
        for (AndroidDriver d : new AndroidDriver[]{driverA, driverB}) {
            if (d == null) continue;
            try { new DriveEditorPage(d).forceExit(); } catch (Exception ignored) { }
            try { d.quit(); } catch (Exception ignored) { }
        }
    }

    private boolean openDriveOn(AndroidDriver d) {
        if (!new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(30))) {
            ProjectListPage list = new ProjectListPage(d);
            List<String> names = list.visibleProjectNames();
            if (!names.isEmpty()) list.tapProject(names.get(0));
        }
        return new DriveFlows(d).openDrive();
    }

    /** Opens the shared workbook with btnEdit (EDIT mode) on one device. */
    private DriveEditorPage openEditorOn(AndroidDriver d, String label) {
        assumeTrue(openDriveOn(d), "Could not open Drive on " + label);
        DriveHomePage drive = new DriveHomePage(d);
        drive.showMyDrive();
        drive.showFiles();
        // On device B the file lives under "Shared with me".
        if (drive.allItemNamesScrolled().stream().noneMatch(n -> n.equalsIgnoreCase(XLSX))) {
            drive.showSharedWithMe();
            drive.showFiles();
        }
        assumeTrue(drive.openItem(XLSX), XLSX + " not reachable on " + label
                + " — share it with edit access first (--drive-share)");
        DriveFileDetailPage detail = new DriveFileDetailPage(d);
        assumeTrue(detail.waitFor("btnEdit", Duration.ofSeconds(25)),
                "No Edit button on " + label + " — that user lacks edit rights on the file");
        detail.tapEdit();
        DriveEditorPage ed = new DriveEditorPage(d);
        assumeTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)) && ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S)),
                "Editor did not load on " + label);
        return ed;
    }

    private List<String> waitPeople(DriveEditorPage ed, int atLeast, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        List<String> seen = ed.peopleInFile();
        while (System.currentTimeMillis() < deadline && seen.size() < atLeast) {
            sleep(2000);
            seen = ed.peopleInFile();
        }
        return seen;
    }

    private static final String USER_B = System.getenv().getOrDefault("ZILLIT_USER_B", "Vidya Pixel");

    /** A gives B edit rights on the workbook, so the suite does not depend on another run. */
    private boolean grantB(boolean edit) {
        if (!openDriveOn(driverA)) return false;
        DriveHomePage drive = new DriveHomePage(driverA);
        drive.showMyDrive();
        drive.showFiles();
        if (!new DriveFlows(driverA).ensureFile(XLSX)) return false;
        if (!drive.openItemMenu(XLSX)) return false;
        com.zillit.automation.pages.drive.DriveItemMenuPage menu = new com.zillit.automation.pages.drive.DriveItemMenuPage(driverA);
        if (!menu.isOpen() || !menu.tapShare()) return false;
        com.zillit.automation.pages.drive.DriveSharePage sp = new com.zillit.automation.pages.drive.DriveSharePage(driverA);
        if (!sp.isOpen(Duration.ofSeconds(20))) return false;
        boolean ok = sp.shareWith(USER_B, edit, edit, edit);
        try { driverA.navigate().back(); } catch (Exception ignored) { }
        return ok;
    }

    // ==================== tests ====================

    @Test
    @Order(0)
    @DisplayName("SETUP: A grants B edit rights on the shared workbook")
    void setupGrant() {
        assumeTrue(grantB(true), "A could not grant " + USER_B + " edit rights on " + XLSX);
        sleep(8000);
    }

    @Test
    @Order(1)
    @DisplayName("Both devices hold the SAME document open in EDIT mode at the same time")
    void bothOpenSameDocument() {
        DriveEditorPage a = openEditorOn(driverA, "device A");
        DriveEditorPage b = openEditorOn(driverB, "device B");
        sleep(3000);
        assertTrue(a.isEditorActivity() && b.isEditorActivity(), "Both devices should be in the editor");
        assertFalse(a.isViewMode(), "Device A should be in EDIT mode");
        assertFalse(b.isViewMode(), "Device B should be in EDIT mode (it has edit rights)");
    }

    @Test
    @Order(2)
    @DisplayName("PRESENCE: each device lists the other under 'People in this file'")
    void eachSeesTheOther() {
        DriveEditorPage a = new DriveEditorPage(driverA);
        DriveEditorPage b = new DriveEditorPage(driverB);
        assumeTrue(a.isEditorActivity() && b.isEditorActivity(), "Both devices must still be in the editor");
        List<String> onA = waitPeople(a, 2, SYNC);
        List<String> onB = waitPeople(b, 2, SYNC);
        System.out.println(">>> people on A=" + onA + " on B=" + onB);
        assertTrue(onA.size() >= 2, "Device A should see device B's user join: " + onA);
        assertTrue(onB.size() >= 2, "Device B should see device A's user: " + onB);
    }

    @Test
    @Order(3)
    @DisplayName("Each device types its own value into the shared sheet")
    void bothType() {
        DriveEditorPage a = new DriveEditorPage(driverA);
        DriveEditorPage b = new DriveEditorPage(driverB);
        assumeTrue(a.isEditorActivity() && b.isEditorActivity(), "Both devices must be in the editor");
        long t = System.currentTimeMillis() % 1000000;
        markerA = "zlca" + t;
        markerB = "zlcb" + t;
        assertTrue(a.writeMarker(markerA), "Device A could not type");
        assertTrue(b.writeMarker(markerB), "Device B could not type");
        a.waitForAutosave();
    }

    @Test
    @Order(4)
    @DisplayName("PERSISTENCE: after both leave, the downloaded file holds BOTH users' values")
    void bothEditsPersist() {
        assumeTrue(markerA != null && markerB != null, "The typing case must run first");
        DriveEditorPage.CloseResult ra = new DriveEditorPage(driverA).backAndObserve(Duration.ofSeconds(15), true);
        DriveEditorPage.CloseResult rb = new DriveEditorPage(driverB).backAndObserve(Duration.ofSeconds(15), true);
        System.out.println(">>> close A " + ra + " | B " + rb);
        sleep(4000);

        DriveFileDetailPage d = new DriveFileDetailPage(driverA);
        if (!d.waitFor("btnDownload", Duration.ofSeconds(8))) {
            assumeTrue(openDriveOn(driverA), "Drive on A");
            d = new DriveFlows(driverA).openDetail(XLSX);
            assumeTrue(d != null, "A could not reopen " + XLSX);
        }
        assumeTrue(d.tapDownload(), "Download not tappable on A");
        d.waitDownloadDone(Duration.ofSeconds(40));
        DriveFileContent reader = new DriveFileContent(driverA);
        DriveFileContent.Result resA = reader.waitForMarker(XLSX, markerA, Duration.ofSeconds(60));
        DriveFileContent.Result resB = reader.waitForMarker(XLSX, markerB, Duration.ofSeconds(30));
        System.out.println(">>> A's marker " + resA + " | B's marker " + resB);
        assumeTrue(!resA.copies.isEmpty(), "No downloaded copy to read on A: " + resA);
        assertTrue(resA.markerFound, "Device A's edit should be saved: " + resA);
        assertTrue(resB.markerFound, "Device B's edit should be saved too — both users' work must survive: " + resB);
    }

    @Test
    @Order(9)
    @DisplayName("CLEANUP: A withdraws B's rights on the workbook")
    void cleanupGrant() {
        new DriveEditorPage(driverA).forceExit();
        new DriveEditorPage(driverB).forceExit();
        assertTrue(grantB(false), "A could not withdraw " + USER_B + "'s rights");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
