package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.drive.DriveEditorPage;
import com.zillit.automation.pages.drive.DriveFileContent;
import com.zillit.automation.pages.drive.DriveFileDetailPage;
import com.zillit.automation.pages.drive.DriveFlows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EDITOR INPUT PROBE — which way of typing actually reaches the Zillit Sheets page AND gets
 * saved. An experiment harness, not coverage.
 *
 * <p>The old Collabora probe (formula bar, ✓, {@code adb input text}) is gone with Collabora. The
 * new page draws everything on a canvas and types through one hidden 15×10 px EditText, so the
 * candidate paths are:
 * <pre>
 *   keyevents   tap an empty cell, then real key events (pressKey) — DriveEditorPage.writeMarker
 *   sendKeys    sendKeys() on the page's hidden EditText (goes through the IME / ACTION_SET_TEXT)
 *   actions     W3C keyboard Actions after the same tap
 * </pre>
 * Each path types its own marker into a run-unique {@code QA_Probe_<stamp>.xlsx}. The only test
 * that counts is the saved FILE: Back (Skip if asked), Download, and look for each marker in the
 * xlsx XML. The copy is purged at the end.
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE_EDITOR_PROBE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveEditorWriteProbeTest extends BaseTest {

    private static final String XLSX = "zl_drive_book.xlsx";
    private static final long STAMP = System.currentTimeMillis() % 100000;
    private static final String PROBE = "QA_Probe_" + STAMP + ".xlsx";

    private static final Map<String, String> MARKERS = new LinkedHashMap<>();
    private static boolean uploaded;

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
        assumeTrue(new DriveFlows(driver).openDrive(), "Could not open Drive");
    }

    @AfterEach
    void leave() { new DriveEditorPage(driver).forceExit(); }

    @Test
    @Order(1)
    @DisplayName("PROBE: type one marker per input path into a scratch spreadsheet")
    void typeWithEveryPath() {
        DriveFlows f = new DriveFlows(driver);
        assumeTrue(f.uploadAs(XLSX, PROBE), "Could not upload the scratch copy " + PROBE);
        uploaded = true;
        DriveFileDetailPage d = f.openDetail(PROBE);
        assumeTrue(d != null && d.waitFor("btnEdit", Duration.ofSeconds(25)), "No Edit on " + PROBE);
        d.tapEdit();
        DriveEditorPage ed = new DriveEditorPage(driver);
        assumeTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)) && ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S)),
                "The editor did not load");

        // 1. real key events
        String m1 = "zlkey" + STAMP;
        boolean ok1 = ed.writeMarker(m1);
        MARKERS.put("keyevents", m1);

        // 2. sendKeys on the hidden EditText — move down a few rows first so it is a new cell
        String m2 = "zlsend" + STAMP;
        boolean ok2 = false;
        try {
            ed.focusEmptyArea();
            for (int i = 0; i < 3; i++) ed.pressEnter();
            WebElement input = driver.findElement(By.xpath(
                    "//android.webkit.WebView//android.widget.EditText"));
            input.sendKeys(m2);
            ed.pressEnter();
            ok2 = true;
        } catch (Exception e) {
            System.out.println(">>> sendKeys path threw: " + e.getMessage().split("\n")[0]);
        }
        MARKERS.put("sendKeys", m2);

        // 3. W3C keyboard actions
        String m3 = "zlact" + STAMP;
        boolean ok3 = false;
        try {
            for (int i = 0; i < 3; i++) ed.pressEnter();
            new Actions(driver).sendKeys(m3).perform();
            ed.pressEnter();
            ok3 = true;
        } catch (Exception e) {
            System.out.println(">>> actions path threw: " + e.getMessage().split("\n")[0]);
        }
        MARKERS.put("actions", m3);

        System.out.println(">>> typed: keyevents=" + ok1 + " sendKeys=" + ok2 + " actions=" + ok3);
        screenshot("drive-editor-probe-typed");
        ed.waitForAutosave();
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(15), true);
        System.out.println(">>> close " + r);
        assertTrue(r.closed, "The editor should close: " + r);
    }

    @Test
    @Order(2)
    @DisplayName("PROBE: which markers reached the saved file (Download → read the xlsx)")
    void reportWhichPathsSaved() {
        assumeTrue(uploaded && !MARKERS.isEmpty(), "The typing case must run first");
        DriveFileDetailPage d = new DriveFlows(driver).openDetail(PROBE);
        assumeTrue(d != null && d.tapDownload(), "Could not download " + PROBE);
        d.waitDownloadDone(Duration.ofSeconds(40));
        DriveFileContent reader = new DriveFileContent(driver);
        Map<String, Boolean> saved = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : MARKERS.entrySet()) {
            DriveFileContent.Result res = reader.waitForMarker(PROBE, e.getValue(), Duration.ofSeconds(25));
            saved.put(e.getKey(), res.markerFound);
            if (res.copies.isEmpty()) {
                assumeTrue(false, "No downloaded copy of " + PROBE + " to read: " + res);
            }
        }
        System.out.println(">>> PROBE RESULT (path → saved) " + saved);
        assertTrue(saved.containsValue(true), "No input path got a value saved into the file: " + saved);
    }

    @Test
    @Order(99)
    @DisplayName("CLEANUP: remove the probe spreadsheet")
    void cleanup() {
        assumeTrue(uploaded, "Nothing was uploaded");
        assertTrue(new DriveFlows(driver).purge(PROBE, false), "Could not remove " + PROBE);
    }
}
