package com.zillit.automation.tests.scheduledist;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage;
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

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

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
