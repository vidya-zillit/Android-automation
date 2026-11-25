package com.zillit.automation.tests.scheduledod;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.scheduledod.ScheduleDodPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Schedule D.O.D. suite — the Schedule Distribution base's policy (open
 * the app once, enter a project once, never relaunch between cases) and its recovery rules:
 * bring the app back to the front first (View hands the PDF to another app), close the
 * non-cancelable confirm with its own left button, and never let "step back" and "open the
 * tool" call each other.
 */
abstract class ScheduleDodBaseTest extends BaseTest {

    /** The tile label the tool was opened from — the toolbar title of every DOD screen. */
    String toolTitle;

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    // ─────────────────────────── navigation ───────────────────────────

    /** Ends on the folder list ({@code DodActivity}). */
    ScheduleDodPage openDod() {
        bringAppToFront();
        if (onList()) return page();
        if (insideDod()) { stepBackToList(); if (onList()) return page(); }

        BottomNavPage nav = new BottomNavPage(driver);
        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) { openFromTools(); return page(); }
        for (int i = 0; i < 6; i++) {
            if (onList()) return page();
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return page(); }
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
        return page();
    }

    private ScheduleDodPage page() {
        ScheduleDodPage p = new ScheduleDodPage(driver);
        p.waitListShown(Duration.ofSeconds(20));
        return p;
    }

    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        boolean opened = false;
        for (String search : List.of("D.O.D", "DOD", "Schedule")) {
            tools.searchTools(search);
            sleep(1200);
            for (String label : ScheduleDodPage.TILE_LABELS) {
                if (tools.openIfVisible(label)) { opened = true; break; }
            }
            if (opened) break;
        }
        if (!opened) {
            assumeTrue(false, "Schedule D.O.D. is not offered in Tools. Tools visible: " + tools.toolNames());
        }
        ScheduleDodPage p = new ScheduleDodPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(25)),
                "Schedule D.O.D. did not open. On screen instead: " + whereAreWe());
        if (toolTitle == null) toolTitle = p.header();
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

    boolean onList() { return activityIs("DodActivity") && present("userHistoryBtn"); }
    boolean inFolder() { return activityIs("DodDetailActivity"); }

    boolean insideDod() {
        return activityIs("DodActivity") || activityIs("DodDetailActivity")
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

    void backToList() {
        stepBackToList();
        if (!onList()) openDod();
    }

    /** Bounded; never calls {@link #openDod()}. */
    void stepBackToList() {
        for (int i = 0; i < 8; i++) {
            bringAppToFront();
            if (onList()) return;
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

    void tapTextIfShown(String regex) {
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
        if (insideDod()) closeBlockingDialog();
    }

    @AfterAll
    void leaveDod() {
        for (int i = 0; i < 8 && insideDod(); i++) {
            if (closeBlockingDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
            if (activityIs("GalleryItemView")) tapTextIfShown("(?i)yes");
        }
    }
}
