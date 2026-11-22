package com.zillit.automation.tests.continuity;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.continuity.ContinuityPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Continuity suite — the same policy as {@code SidesBaseTest}, for the
 * same reasons: the app is opened once and a project entered once, nothing relaunches the
 * app between cases, and every case starts from wherever the last one ended and takes the
 * cheapest way back (already in the tool, else Tools, else the project list).
 *
 * <p>{@link #requiresLoggedOutStart()} is false because the default CLEARS APP DATA when it
 * finds the app logged in, signing the tester out of their own device.
 */
abstract class ContinuityBaseTest extends BaseTest {

    static final String TOOL = "Continuity";

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    // ─────────────────────────── navigation ───────────────────────────

    /** Gets to the Continuity tabs by the shortest route that works. */
    void openContinuity() {
        bringAppToFront();
        if (onTabs()) return;
        if (insideContinuity()) { stepBackToTabs(); if (onTabs()) return; }

        BottomNavPage nav = new BottomNavPage(driver);
        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) { openFromTools(); return; }

        for (int i = 0; i < 6; i++) {
            if (onTabs()) return;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return; }
            if (onProjectList()) break;
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

    /**
     * Puts the app under test in front when something else is — the launcher after a rebuilt
     * Appium session (seen on the device: every later case waited 60s for a bottom bar that
     * was never going to appear, because BACK on the launcher goes nowhere), or the external
     * viewer a document card hands off to.
     */
    void bringAppToFront() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); } catch (Exception e) { pkg = ""; }
        if (appPackage.equals(pkg)) return;
        try {
            driver.activateApp(appPackage);
        } catch (Exception e) {
            try { driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage)); }
            catch (Exception ignored) { }
        }
        sleep(3000);
        waitAndDismissPermissions(Duration.ofSeconds(3));
    }

    /** Tools → Continuity. */
    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools(TOOL);
        sleep(1200);
        if (!tools.openIfVisible(TOOL)) {
            assumeTrue(false, "\"" + TOOL + "\" is not offered in Tools. Tools visible: " + tools.toolNames());
        }
        // ContinuityPage finish()es with no message when the rights list has no
        // continuity_tool entry or viewAccess is false — so "did not open" is that, usually.
        assumeTrue(new ContinuityPage(driver).isLoaded(Duration.ofSeconds(25)),
                "The Continuity tool did not open. It closes itself with no message when this"
                        + " user has no view access to it. On screen instead: " + whereAreWe());
    }

    /** On the two-tab screen itself (not a scene, not a dialog over it). */
    boolean onTabs() {
        return activityIs("ContinuityPage") && present("tabLayout");
    }

    /** Anywhere inside the module: the tabs, a scene's grid, a dialog over either. */
    boolean insideContinuity() {
        return activityIs("ContinuityPage") || activityIs("ContinuitySceneListPage");
    }

    boolean onScenePage() { return activityIs("ContinuitySceneListPage"); }

    private boolean activityIs(String simpleName) {
        try {
            String a = String.valueOf(driver.currentActivity());
            return a.endsWith("." + simpleName) || a.endsWith(simpleName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Back to the tabs from a scene page (BACK steps department grid → department list →
     * tabs on All Department) or from another app the viewer handed us to.
     *
     * <p>Never called with the Add sheet up: that sheet is not cancelable, so BACK would do
     * nothing there — the suite closes it with its X first.
     */
    void backToTabs() {
        stepBackToTabs();
        if (!onTabs()) openContinuity();
    }

    /**
     * The bounded part of {@link #backToTabs()}: never calls {@link #openContinuity()}, so the
     * two cannot recurse into each other.
     *
     * <p>That recursion is what hung the first full run for twenty minutes. The Add/Edit sheet
     * and the confirm dialogs are {@code setCancelable(false)} — BACK does nothing to them —
     * so a case that ended with one open left a screen BACK could not leave: six BACKs, then
     * openContinuity, which saw "inside Continuity" and came straight back here. Each pass
     * therefore closes those with their OWN buttons first, and BACK is only for real screens.
     */
    void stepBackToTabs() {
        for (int i = 0; i < 8; i++) {
            if (onTabs()) return;
            if (onProjectList()) return;
            if (closeBlockingSheetOrDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
    }

    /**
     * Closes whichever non-cancelable layer is on top. The order matters: the small "Add More
     * Details" dialog sits over the Add/Edit sheet and shares {@code leftButton} with the
     * confirm dialogs.
     *
     * @return true when it closed something
     */
    boolean closeBlockingSheetOrDialog() {
        // "Add More Details" (editText1/editText2) or a confirm (header/message): leftButton is
        // Cancel / No / Ok in every one of them — never the action.
        if (present("leftButton") && (present("editText1") || present("message"))) {
            return tapId("leftButton");
        }
        // The Add / Edit sheet: its X.
        if (present("submit_btn") && present("scene_no")) {
            return tapId("btn_cancel");
        }
        // The card ⋮ PopupWindow is not dismissed by an outside tap, but BACK closes it — leave
        // it to the caller's BACK.
        return false;
    }

    private boolean tapId(String id) {
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

    boolean onProjectList() { return present("projectListRecyclerView"); }

    /** Presence without waiting and without throwing. */
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

    /** Park outside the module so the next suite does not start inside this one. */
    @AfterAll
    void leaveContinuity() {
        for (int i = 0; i < 8 && insideContinuity(); i++) {
            if (closeBlockingSheetOrDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
    }

    /**
     * Every case starts from a screen nothing is covering. A case that failed half-way can leave
     * a non-cancelable sheet or dialog up, and the next case's first lookup would then read the
     * sheet instead of the screen under it.
     */
    @org.junit.jupiter.api.BeforeEach
    void clearLeftovers() {
        for (int i = 0; i < 3 && insideContinuity(); i++) {
            if (!closeBlockingSheetOrDialog()) break;
        }
    }
}
