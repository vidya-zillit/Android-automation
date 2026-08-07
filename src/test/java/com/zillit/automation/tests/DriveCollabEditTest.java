package com.zillit.automation.tests;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DriveEditorPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * COLLABORATIVE EDITING — the same spreadsheet open in the Collabora editor on TWO
 * devices at once, as two different users.
 *
 * <p>This is the only way to prove the editor is genuinely collaborative rather than
 * last-writer-wins: a single-device test cannot tell the difference. Device A types
 * into one cell, device B into another, and each must end up seeing BOTH.
 *
 * <pre>
 *   ZILLIT_SENDER_UDID=&lt;device A&gt; ZILLIT_RECEIVER_UDID=&lt;device B&gt; \
 *     ./run-and-report.sh --drive-collab
 * </pre>
 *
 * <p>Both devices must be signed in as DIFFERENT users on the SAME project, and both
 * users need edit rights on the file (share it with edit access first — see
 * {@code DriveShareTest}).
 *
 * <p>Unlike the rest of the Drive suites this one drives two drivers itself, so it does
 * not extend {@code BaseTest}.
 */
@Order(TestOrder.DRIVE_COLLAB)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveCollabEditTest {

    private static final String XLSX = "zl_drive_book.xlsx";
    /** Each device owns a cell, so a conflict cannot be mistaken for a sync failure. */
    private static final String CELL_A = "F1";
    private static final String CELL_B = "F2";
    /** How long a change may take to reach the other device. */
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

        // Distinct systemPorts — two UiAutomator2 servers cannot share one port.
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

    // ---------------- navigation shared by both devices ----------------

    private boolean openDriveOn(AndroidDriver d) {
        DriveHomePage drive = new DriveHomePage(d);
        try { new DriveEditorPage(d).forceExit(); } catch (Exception ignored) { }

        if (!new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(30))) {
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

    /** Opens the shared workbook in edit mode on one device. */
    private DriveEditorPage openEditorOn(AndroidDriver d, String label) {
        assumeTrue(openDriveOn(d), "Could not open Drive on " + label);
        DriveHomePage drive = new DriveHomePage(d);
        drive.showFiles();
        // On device B the file lives under "Shared with me" until it is their own.
        if (!drive.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(XLSX))) {
            drive.showSharedWithMe();
            drive.showFiles();
        }
        assumeTrue(drive.openItem(XLSX), XLSX + " not reachable on " + label
                + " — share it with edit access first (--drive-share)");

        long deadline = System.currentTimeMillis() + 25000;
        boolean edit = false;
        while (System.currentTimeMillis() < deadline && !edit) {
            edit = !d.findElements(io.appium.java_client.AppiumBy.id(
                    d.getCurrentPackage() + ":id/btnEdit")).isEmpty();
            if (!edit) sleep(800);
        }
        assumeTrue(edit, "No Edit button on " + label + " — that user lacks edit rights on the file");
        d.findElement(io.appium.java_client.AppiumBy.id(
                d.getCurrentPackage() + ":id/btnEdit")).click();

        DriveEditorPage ed = new DriveEditorPage(d);
        assumeTrue(ed.waitLoaded(Duration.ofSeconds(120)), "Editor did not load on " + label);
        assumeTrue(ed.enterEditMode(), "Could not enter edit mode on " + label);
        return ed;
    }

    /** Polls a cell until it holds the expected value, or the sync window expires. */
    private String waitForCell(DriveEditorPage ed, String cell, String expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String seen = "";
        while (System.currentTimeMillis() < deadline) {
            seen = ed.readCell(cell);
            if (expected.equals(seen)) return seen;
            sleep(3000);
        }
        return seen;
    }

    // ==================== tests ====================

    @Test
    @Order(1)
    @DisplayName("Both devices can open the SAME document in the editor at the same time")
    void bothOpenSameDocument() {
        DriveEditorPage a = openEditorOn(driverA, "device A");
        DriveEditorPage b = openEditorOn(driverB, "device B");

        assertTrue(a.isEditorActivity(), "Device A should be in the editor");
        assertTrue(b.isEditorActivity(), "Device B should be in the editor");
        System.out.println(">>> both devices have " + XLSX + " open in edit mode");
    }

    @Test
    @Order(2)
    @DisplayName("LIVE SYNC: a cell typed on device A appears on device B")
    void aToB() {
        DriveEditorPage a = new DriveEditorPage(driverA);
        DriveEditorPage b = new DriveEditorPage(driverB);
        assumeTrue(a.isEditorActivity() && b.isEditorActivity(),
                "Both devices must still be in the editor — run the previous case first");

        markerA = "A" + (System.currentTimeMillis() % 1000000);
        assumeTrue(a.writeCell(CELL_A, markerA), "Device A could not write " + CELL_A);
        System.out.println(">>> A wrote '" + markerA + "' to " + CELL_A);

        String seen = waitForCell(b, CELL_A, markerA, SYNC);
        System.out.println(">>> B sees " + CELL_A + " = '" + seen + "'");
        assertEquals(markerA, seen,
                "A change made on device A should reach device B within " + SYNC.toSeconds() + "s");
    }

    @Test
    @Order(3)
    @DisplayName("LIVE SYNC: a cell typed on device B appears on device A")
    void bToA() {
        DriveEditorPage a = new DriveEditorPage(driverA);
        DriveEditorPage b = new DriveEditorPage(driverB);
        assumeTrue(a.isEditorActivity() && b.isEditorActivity(), "Both devices must be in the editor");

        markerB = "B" + (System.currentTimeMillis() % 1000000);
        assumeTrue(b.writeCell(CELL_B, markerB), "Device B could not write " + CELL_B);
        System.out.println(">>> B wrote '" + markerB + "' to " + CELL_B);

        String seen = waitForCell(a, CELL_B, markerB, SYNC);
        System.out.println(">>> A sees " + CELL_B + " = '" + seen + "'");
        assertEquals(markerB, seen,
                "A change made on device B should reach device A within " + SYNC.toSeconds() + "s");
    }

    @Test
    @Order(4)
    @DisplayName("CONCURRENT: simultaneous edits to different cells BOTH survive")
    void concurrentEditsBothSurvive() {
        DriveEditorPage a = new DriveEditorPage(driverA);
        DriveEditorPage b = new DriveEditorPage(driverB);
        assumeTrue(a.isEditorActivity() && b.isEditorActivity(), "Both devices must be in the editor");

        String cA = "G1", cB = "G2";
        String mA = "CA" + (System.currentTimeMillis() % 100000);
        String mB = "CB" + (System.currentTimeMillis() % 100000);

        // As close to simultaneous as two sequential driver calls allow — enough to
        // catch a last-writer-wins implementation that drops one side.
        Thread tA = new Thread(() -> a.writeCell(cA, mA));
        Thread tB = new Thread(() -> b.writeCell(cB, mB));
        tA.start(); tB.start();
        try { tA.join(60000); tB.join(60000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String aSeesB = waitForCell(a, cB, mB, SYNC);
        String bSeesA = waitForCell(b, cA, mA, SYNC);
        System.out.println(">>> A sees " + cB + "='" + aSeesB + "' (expected '" + mB + "')");
        System.out.println(">>> B sees " + cA + "='" + bSeesA + "' (expected '" + mA + "')");

        assertEquals(mB, aSeesB, "Device A must see device B's concurrent edit — neither may be lost");
        assertEquals(mA, bSeesA, "Device B must see device A's concurrent edit — neither may be lost");
    }

    @Test
    @Order(5)
    @DisplayName("PERSISTENCE: after both devices leave, the merged content is still there")
    void mergedContentPersists() {
        assumeTrue(markerA != null && markerB != null, "The sync cases must run first");
        DriveEditorPage a = new DriveEditorPage(driverA);
        DriveEditorPage b = new DriveEditorPage(driverB);

        a.forceExit();
        b.forceExit();
        sleep(6000);

        DriveEditorPage reopened = openEditorOn(driverA, "device A (reopen)");
        String valA = reopened.readCell(CELL_A);
        String valB = reopened.readCell(CELL_B);
        System.out.println(">>> after both left: " + CELL_A + "='" + valA + "' " + CELL_B + "='" + valB + "'");

        assertEquals(markerA, valA, "Device A's edit should have persisted");
        assertEquals(markerB, valB, "Device B's edit should have persisted — both users' work must survive");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
