package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DriveEditorPage;
import com.zillit.automation.pages.DriveHomePage;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EDITOR CELL WRITES — a probe for the open failure in DRIVE_COMPLETION_PLAN §2.2.
 *
 * <p>The symptom is precise and misleading: after {@code writeCell("H1", marker)} the
 * FORMULA BAR reads the marker back, so the write looks fine — but the other device sees
 * an empty cell, and so does this one after a reload. The value reaches the input field
 * and never reaches the document.
 *
 * <p>This suite is a <b>experiment harness</b>, not coverage. It writes a distinct marker
 * per strategy and judges each one by the only test that matters — the ROUND TRIP:
 *
 * <pre>
 *   write → move to a far cell → come back → read
 * </pre>
 *
 * <p>Reading straight after typing proves nothing: the formula bar still holds the text
 * you just put in it whether or not the cell took it. Every strategy is reported, so one
 * run says which paths commit and which only decorate the input field.
 *
 * <p>Needs Appium started with {@code --relaxed-security} (the ADB input service) and the
 * editable fixture {@code zl_drive_book.xlsx}.
 */
@Order(TestOrder.DRIVE_EDITOR_PROBE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveEditorWriteProbeTest extends BaseTest {

    private static final String XLSX = "zl_drive_book.xlsx";
    private static final long STAMP = System.currentTimeMillis() % 100000;

    /** A far-away cell to move through, so returning forces a real re-read. */
    private static final String PARKING_CELL = "A20";

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
    }

    private boolean openDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        try { new DriveEditorPage(driver).forceExit(); } catch (Exception ignored) { }
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        for (int i = 0; i < 6 && !onBottomNavScreen(); i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception ignored) { }
        }
        ToolsPage tools = new ToolsPage(driver);
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
        return false;
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private boolean ensureFile() {
        DriveHomePage drive = new DriveHomePage(driver);
        if (drive.hasFile(XLSX)) return true;
        if (!drive.openFab() || !drive.tapUploadFile()) return false;
        DriveUploadPage up = new DriveUploadPage(driver);
        if (!up.uploadFile("Document", XLSX)) return false;
        sleep(10000);
        return new DriveHomePage(driver).hasFile(XLSX);
    }

    /** Opens the spreadsheet in the editor, in edit mode. */
    private DriveEditorPage openEditor() {
        assumeTrue(openDrive(), "Could not open Drive");
        assumeTrue(ensureFile(), "Could not get " + XLSX + " into Drive");
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        assumeTrue(drive.openItem(XLSX), "Could not open " + XLSX);

        assumeTrue(isVisibleId("btnEdit", Duration.ofSeconds(25)), "No Edit button on the file detail");
        clickId("btnEdit");

        DriveEditorPage ed = new DriveEditorPage(driver);
        assumeTrue(ed.waitLoaded(Duration.ofSeconds(150)), "The editor did not load");
        assumeTrue(ed.enterEditMode(), "Could not enter edit mode");
        return ed;
    }

    private boolean isVisibleId(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(byAppId(id)).isEmpty()) return true;
            sleep(500);
        }
        return false;
    }

    private void clickId(String id) {
        try { driver.findElement(byAppId(id)).click(); } catch (Exception ignored) { }
    }

    // ==================== the experiment ====================

    @Test
    @Order(1)
    @DisplayName("2.2 PROBE: which write strategy actually commits a value to the cell?")
    void whichWriteStrategyCommits() {
        DriveEditorPage ed = openEditor();

        // Each strategy gets its own cell so they cannot mask each other.
        Map<String, String> cells = new LinkedHashMap<>();
        cells.put("formulaBar+shellInput", "H1");   // what writeCell() does today
        cells.put("selectedCell+shellInput", "H2"); // type with the CELL selected, no field click
        cells.put("formulaBar+sendKeys", "H3");     // the plain Appium path, for the record

        Map<String, String> committed = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();

        for (Map.Entry<String, String> e : cells.entrySet()) {
            String strategy = e.getKey();
            String cell = e.getValue();
            String marker = "P" + STAMP + cells.keySet().stream().toList().indexOf(strategy);

            boolean typed;
            switch (strategy) {
                case "selectedCell+shellInput" -> typed = ed.writeCellDirect(cell, marker);
                case "formulaBar+sendKeys"     -> typed = ed.writeCellSendKeys(cell, marker);
                default                        -> typed = ed.writeCell(cell, marker);
            }

            // Read straight back — this is the check that LIES, kept only to show the
            // difference between "the field holds it" and "the document holds it".
            String immediate = ed.readCell(cell);

            // The real test: leave the cell entirely, come back, read again.
            ed.gotoCell(PARKING_CELL);
            sleep(1500);
            String roundTrip = ed.readCell(cell);

            committed.put(strategy, roundTrip);
            notes.add(String.format("%-24s cell=%s typed=%-5s immediate='%s' roundTrip='%s' %s",
                    strategy, cell, typed, immediate, roundTrip,
                    marker.equals(roundTrip) ? "✅ COMMITTED" : "✗ lost"));
            System.out.println(">>> " + notes.get(notes.size() - 1));
        }

        // acceptEdit() is a blind coordinate tap, so its return value is the only thing
        // that says whether the ✓ was actually hit on THIS screen size. Grab the screen
        // first: when it misses, this shot is what the ratio gets re-measured from.
        screenshot("editor-edit-mode-before-accept");
        boolean accepted = ed.acceptEdit();
        System.out.println(">>> acceptEdit (✓ at 4.5% × 5.5%) left edit mode = " + accepted);
        ed.waitForSave();
        ed.forceExit();

        System.out.println(">>> ---- write-strategy probe ----");
        notes.forEach(n -> System.out.println(">>> " + n));

        // At least one strategy has to reach the document, or cell editing is not
        // automatable on this build and §2.2 needs the app team rather than another
        // locator.
        assertTrue(committed.values().stream().anyMatch(v -> v != null && v.startsWith("P" + STAMP)),
                "No write strategy survived the round trip — the value never reaches the "
                        + "document. Results: " + notes);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
