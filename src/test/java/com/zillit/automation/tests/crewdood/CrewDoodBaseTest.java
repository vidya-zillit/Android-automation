package com.zillit.automation.tests.crewdood;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.crewdood.CrewDoodPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Crew DOOD suite. Same policy as the Schedule Distribution base: the app
 * is opened once, a project entered once, nothing relaunches between cases.
 *
 * <ul>
 *   <li>The project is {@code -Dzillit.crewdood.project} (default {@code ZL-Android-Automation});
 *       when the project list is up it is searched for, not "the first project".</li>
 *   <li>Crew DOOD's detail and Days-off editor are fragments on a back stack inside the same
 *       activity: BACK pops them one at a time, and one BACK too many closes the tool.</li>
 *   <li>Export hands the workbook to another app (a spreadsheet viewer or a chooser); bring
 *       Zillit back to the front before anything else.</li>
 * </ul>
 */
abstract class CrewDoodBaseTest extends BaseTest {

    static final String TOOL = CrewDoodPage.TOOL_TILE;
    static final String ACTIVITY = "CrewDoodActivity";
    static final String PROJECT = !ProjectListPage.runProject().isEmpty() ? ProjectListPage.runProject()
            : System.getProperty("zillit.crewdood.project", "ZL-Android-Automation");

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    // ─────────────────────────── navigation ───────────────────────────

    /** Lands on the Crew DOOD list with nothing on top of it and the data loaded. */
    CrewDoodPage openDood() {
        bringAppToFront();
        CrewDoodPage page = new CrewDoodPage(driver);
        if (activityIs(ACTIVITY)) {
            for (int i = 0; i < 3 && page.overlayOpen(); i++) page.back();
            page.closeDepartmentSheet();
            if (activityIs(ACTIVITY) && page.isLoaded(Duration.ofSeconds(3))) {
                assumeTrue(page.waitForData(Duration.ofSeconds(30)), "Crew DOOD never finished loading");
                return page;
            }
        }

        BottomNavPage nav = new BottomNavPage(driver);
        if (!nav.waitUntilLoaded(Duration.ofSeconds(3))) {
            for (int i = 0; i < 6; i++) {
                if (nav.waitUntilLoaded(Duration.ofSeconds(2))) break;
                if (onProjectList()) break;
                if (closeBlockingDialog()) continue;
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(900);
            }
            if (onProjectList()) enterProject();
            if (!nav.waitUntilLoaded(Duration.ofSeconds(60))) {
                assumeTrue(false, "The bottom navigation never loaded. On screen instead: " + whereAreWe());
            }
        }
        openFromTools();
        assumeTrue(page.waitForData(Duration.ofSeconds(30)), "Crew DOOD never finished loading");
        return page;
    }

    private void enterProject() {
        ProjectListPage projects = new ProjectListPage(driver);
        projects.search(PROJECT);
        sleep(1200);
        List<String> names = projects.visibleProjectNames();
        String pick = null;
        for (String n : names) if (n.startsWith(PROJECT)) { pick = n; break; }
        if (pick == null) projects.clearSearch();
        assumeTrue(pick != null, "Project \"" + PROJECT + "\" is not in the project list. Listed: " + names);
        assumeTrue(projects.tapProject(pick), "Could not enter \"" + pick + "\"");
    }

    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("DOOD");
        sleep(1200);
        if (!tools.openIfVisible(TOOL)) {
            assumeTrue(false, "\"" + TOOL + "\" is not offered in Tools (identifier crew_dood_tool)."
                    + " It is hidden when this user has no view access. Tools visible: " + tools.toolNames());
        }
        assumeTrue(new CrewDoodPage(driver).isLoaded(Duration.ofSeconds(25)),
                "Crew DOOD did not open. On screen instead: " + whereAreWe());
    }

    void bringAppToFront() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); } catch (Exception e) { pkg = ""; }
        if (appPackage.equals(pkg)) return;
        for (int i = 0; i < 2; i++) {   // a spreadsheet viewer / chooser usually leaves on BACK
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

    boolean activityIs(String simpleName) {
        try {
            String a = String.valueOf(driver.currentActivity());
            return a.endsWith("." + simpleName) || a.endsWith(simpleName);
        } catch (Exception e) {
            return false;
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

    @AfterAll
    void leaveDood() {
        bringAppToFront();
        for (int i = 0; i < 5 && activityIs(ACTIVITY); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
    }
}
