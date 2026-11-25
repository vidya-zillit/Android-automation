package com.zillit.automation.tests.scheduledist;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage;
import com.zillit.automation.pages.scheduledist.ScheduleUploadPage;
import com.zillit.automation.pages.common.ToolsPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Schedule Distribution suite. Same policy as the Sides and Continuity
 * bases — the app is opened once, a project entered once, nothing relaunches between cases —
 * plus the recovery rules the Continuity runs taught:
 *
 * <ul>
 *   <li>bring the app back to the front first: after a rebuilt Appium session, or after
 *       "View" hands a PDF to an external app, something else is on top;</li>
 *   <li>close the non-cancelable confirm dialog with its own button — BACK does nothing to it;</li>
 *   <li>never let the "step back" helper and the "open the tool" helper call each other.</li>
 * </ul>
 */
abstract class ScheduleDistBaseTest extends BaseTest {

    /** The Tools tile's label on the automation project — server-supplied, NOT "Schedule Distribution". */
    static final String TOOL = ScheduleDistPage.TOOL_TILE;

    /** Full file name, not a prefix: the picker matches on "contains" (see SidesTest.SCRIPT_PDF). */
    static final String PDF = "zl_test_doc.pdf";
    static final Duration UPLOAD_WAIT = Duration.ofSeconds(120);

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    // ─────────────────────────── shared upload / folder steps ───────────────────────────

    ScheduleDistPage tab(String name) {
        openSchedule();
        ScheduleDistPage page = new ScheduleDistPage(driver);
        if (!page.isSelected(name)) assumeTrue(page.openTab(name), "Tab " + name + " could not be tapped");
        page.waitLoaded(Duration.ofSeconds(30));
        return page;
    }

    /** The upload button → the picker. */
    ScheduleUploadPage startUpload(String tabName) {
        ScheduleDistPage page = tab(tabName);
        assumeTrue(page.uploadShown(), "No upload button on " + tabName + ": this user cannot upload");
        assumeTrue(page.tapUpload(), "The upload button could not be tapped");
        if (page.messageShown("You do not have Posting Rights", Duration.ofSeconds(3))) {
            assumeTrue(false, "Upload is shown but refused: admin without the posting flag (finding 3)");
        }
        ScheduleUploadPage up = new ScheduleUploadPage(driver);
        assumeTrue(up.pickerShown(Duration.ofSeconds(10)), "The picker did not open");
        return up;
    }

    /** The upload button → picker → the PDF fixture → the form. */
    ScheduleUploadPage openForm(String tabName) {
        ScheduleUploadPage up = startUpload(tabName);
        boolean ok = up.pickPdf(PDF, () -> waitAndDismissPermissions(Duration.ofSeconds(4)));
        if (!ok) backToTabs();
        assumeTrue(ok, "Could not pick " + PDF + " in the system picker. The runner pushes it to"
                + " /sdcard/Documents; on screen: " + whereAreWe());
        return up;
    }

    boolean uploadPage(String sceneNo, String pageNo, String colour, String type) {
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_PAGES);
        up.setField(ScheduleUploadPage.SCENE, sceneNo);
        boolean colourSet = up.chooseDropdown(ScheduleUploadPage.COLOUR, colour);
        boolean typeSet = up.chooseDropdown(ScheduleUploadPage.TYPE, type);
        if (!colourSet || !typeSet) {
            up.leaveForm();
            backToTabs();
            assumeTrue(false, "The " + (colourSet ? "type" : "colour") + " dropdown could not be set to \""
                    + (colourSet ? type : colour) + "\" — not uploading a page that would be missing it");
        }
        up.setField(ScheduleUploadPage.PAGE_NO, pageNo);
        if (up.fieldShown(ScheduleUploadPage.EPISODE)) up.setField(ScheduleUploadPage.EPISODE, "1");
        up.submit();
        boolean closed = up.waitFormClosed(UPLOAD_WAIT);
        if (!closed) { up.leaveForm(); }
        backToTabs();
        return closed;
    }

    ScheduleDistPage openFolder(String sceneNo) {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        page.searchPages(sceneNo);
        boolean opened = page.openFolder("Scene No. " + sceneNo);
        if (!opened) page.searchPages("");
        assumeTrue(opened, "The folder for scene " + sceneNo + " is not on the Pages tab");
        ScheduleDistPage inside = new ScheduleDistPage(driver);
        inside.waitLoaded(Duration.ofSeconds(30));
        return inside;
    }

    // ─────────────────────────── navigation ───────────────────────────

    void openSchedule() {
        bringAppToFront();
        if (onTabs()) return;
        if (insideSchedule()) { stepBackToTabs(); if (onTabs()) return; }

        BottomNavPage nav = new BottomNavPage(driver);
        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) { openFromTools(); return; }
        for (int i = 0; i < 6; i++) {
            if (onTabs()) return;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return; }
            if (onProjectList()) break;
            if (closeBlockingDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (onProjectList()) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "The project list is open but shows no projects to enter.");
            assumeTrue(projects.tapProject(names.get(0)),
                    "Could not enter \"" + names.get(0) + "\". Projects listed: " + names);
        }
        if (!nav.waitUntilLoaded(Duration.ofSeconds(60))) {
            assumeTrue(false, "The bottom navigation never loaded. On screen instead: " + whereAreWe());
        }
        openFromTools();
    }

    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("Schedule Full");
        sleep(1200);
        if (!tools.openIfVisible(TOOL)) {
            assumeTrue(false, "\"" + TOOL + "\" is not offered in Tools — Schedule Distribution's tile."
                    + " Tools visible: " + tools.toolNames());
        }
        assumeTrue(new ScheduleDistPage(driver).isLoaded(Duration.ofSeconds(25)),
                "Schedule Distribution did not open. It closes itself with no message when this user"
                        + " has no view access to it. On screen instead: " + whereAreWe());
    }

    void bringAppToFront() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); } catch (Exception e) { pkg = ""; }
        if (appPackage.equals(pkg)) return;
        for (int i = 0; i < 2; i++) {   // an external PDF viewer usually leaves on BACK
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            try { if (appPackage.equals(driver.getCurrentPackage())) return; } catch (Exception ignored) { }
        }
        try { driver.activateApp(appPackage); }
        catch (Exception e) {
            try { driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage)); }
            catch (Exception ignored) { }
        }
        sleep(3000);
        waitAndDismissPermissions(Duration.ofSeconds(3));
    }

    boolean onTabs() { return activityIs("ScheduleDistActivity") && present("tabLayout"); }

    boolean insideSchedule() {
        return activityIs("ScheduleDistActivity") || activityIs("SchedulePageDetailActivity")
                || activityIs("UploadDocumentActivity") || activityIs("UserChatHistoryPage")
                || activityIs("UserHistoryFolderPage") || activityIs("UsersCountListPage")
                || activityIs("GalleryItemView");
    }

    boolean activityIs(String simpleName) {
        try {
            String a = String.valueOf(driver.currentActivity());
            return a.endsWith("." + simpleName) || a.endsWith(simpleName);
        } catch (Exception e) {
            return false;
        }
    }

    void backToTabs() {
        stepBackToTabs();
        if (!onTabs()) openSchedule();
    }

    /** Bounded; never calls {@link #openSchedule()}. */
    void stepBackToTabs() {
        for (int i = 0; i < 8; i++) {
            bringAppToFront();
            if (onTabs()) return;
            if (onProjectList()) return;
            if (closeBlockingDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            // Leaving GalleryItemView raises "Are you sure you want to exit?" — answer Yes there,
            // and ONLY there: anywhere else a "Yes" may be a delete confirm.
            if (activityIs("GalleryItemView")) tapTextIfShown("(?i)yes");
        }
    }

    /** The app's confirm dialog is not cancelable: its left button is Cancel / No / Ok. */
    boolean closeBlockingDialog() {
        if (present("leftButton") && present("message")) return tapId("leftButton");
        return false;
    }

    boolean tapId(String id) {
        try {
            List<org.openqa.selenium.WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")"));
            if (e.isEmpty()) return false;
            e.get(0).click();
            sleep(1000);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void tapTextIfShown(String regex) {
        try {
            List<org.openqa.selenium.WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"" + regex + "\")"));
            if (!e.isEmpty()) { e.get(0).click(); sleep(800); }
        } catch (Exception ignored) { }
    }

    boolean onProjectList() { return present("projectListRecyclerView"); }

    boolean present(String id) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    String whereAreWe() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); }
        catch (Exception e) { pkg = ""; }
        if (!appPackage.equals(pkg)) return "a different app (" + (pkg.isBlank() ? "unreadable" : pkg) + ")";
        if (onProjectList()) return "the project list";
        try { return "activity " + driver.currentActivity(); }
        catch (Exception e) { return "an unreadable activity"; }
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @BeforeEach
    void clearLeftovers() {
        if (insideSchedule()) closeBlockingDialog();
    }

    @AfterAll
    void leaveSchedule() {
        for (int i = 0; i < 8 && insideSchedule(); i++) {
            if (closeBlockingDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
            if (activityIs("GalleryItemView")) tapTextIfShown("(?i)yes");
        }
    }
}
