package com.zillit.automation.tests.casting;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.casting.CastingPage;
import com.zillit.automation.pages.casting.CastingPage.Tool;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Navigation shared by the single-device Casting suite: reach the project once, open a
 * Casting tool from Tools, get back to that tool's home from wherever a case ended, and park
 * on the bottom nav at the end so the next suite starts from a known screen.
 *
 * <p>The suite stays inside the app between cases ({@code relaunchBetweenTests() == false}):
 * every case opens the tool itself, and {@link #recoverToBottomNav()} unwinds whatever a
 * failed case left on screen (a dialog, the filter sheet, a form, a chat).
 */
@ExtendWith(ReasonLogger.class)
abstract class CastingBaseTest extends BaseTest {

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachProject() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects to enter");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                    "Never reached a project");
            insideProject = true;
        }
        dismissPermissionPrompts();
    }

    CastingPage page() { return new CastingPage(driver); }

    /**
     * Opens the tool's home (Selects tab, default grouping) from anywhere. When the home of the
     * same tool is already on screen it is reused unless {@code fresh} — a fresh open resets the
     * filter (the filter lives in the activity's ViewModel).
     */
    CastingPage openTool(Tool tool, boolean fresh) {
        CastingPage p = page();
        if (!fresh && p.onHomeOf(tool, Duration.ofSeconds(1))) return p;
        // Unwind: listing / form / chat / sheet → the home, or out to the bottom nav.
        for (int i = 0; i < 6 && !onBottomNavScreen(); i++) {
            if (!fresh && p.onHomeOf(tool, Duration.ofSeconds(1))) return p;
            try { clearBlockingPopup(); } catch (Exception ignored) { }
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        assumeTrue(recoverToBottomNav(), "The app could not be returned to a bottom-nav screen");
        ToolsPage tools = new ToolsPage(driver);
        if (!tools.openToolsTab()) {
            clearBlockingPopup();
            sleep(600);
            assumeTrue(tools.openToolsTab(), "The Tools tab could not be tapped");
        }
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Casting");
        sleep(1500);
        assumeTrue(p.toolRowListed(tool) || tools.isModuleListed(tool.tile, Duration.ofSeconds(6)),
                "'" + tool.tile + "' is not listed in Tools on this project (tool not enabled)");
        assumeTrue(p.tapToolRow(tool), "The '" + tool.tile + "' row could not be tapped");
        assumeTrue(p.onHomeOf(tool, Duration.ofSeconds(25)),
                tool.tile + " did not open (header read '" + p.headerText() + "')");
        sleep(1500);
        return p;
    }

    CastingPage openTab(Tool tool, String tab, boolean fresh) {
        CastingPage p = openTool(tool, fresh);
        assumeTrue(p.selectTab(tab), "Could not select the " + tab + " tab");
        return p;
    }

    /** Back to the tool's home from a listing page / form opened from it (no re-open). */
    CastingPage backToHome(Tool tool) {
        CastingPage p = page();
        for (int i = 0; i < 4; i++) {
            if (p.onHomeOf(tool, Duration.ofSeconds(1))) return p;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        return p.onHomeOf(tool, Duration.ofSeconds(2)) ? p : openTool(tool, true);
    }

    boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    boolean onProjectList() {
        try { return !driver.findElements(byAppId("projectListRecyclerView")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** Cheapest first: clear a modal, BACK, re-enter the project, foreground, restart. */
    boolean recoverToBottomNav() {
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        if (onBottomNavScreen()) return true;
        for (int i = 0; i < 5 && !onBottomNavScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.contains("zillit")) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(2500);
            }
        } catch (Exception ignored) { }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
        System.out.println(">>> Nothing responds to BACK — restarting the app to recover the bottom nav");
        try {
            driver.executeScript("mobile: terminateApp", java.util.Map.of("appId", appPackage));
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
        } catch (Exception e) {
            return false;
        }
        landing = settleLanding(90, false);
        return onBottomNavScreen() || enterProjectFromList();
    }

    private boolean enterProjectFromList() {
        if (!onProjectList()) return false;
        ProjectListPage list = new ProjectListPage(driver);
        List<String> names = list.visibleProjectNames();
        if (names.isEmpty()) return false;
        if (!list.tapProject(names.get(0))) return false;
        return new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(45));
    }

    @AfterAll
    void parkOnBottomNav() {
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        for (int i = 0; i < 10; i++) {
            if (onBottomNavScreen()) return;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        try {
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
            sleep(2500);
        } catch (Exception ignored) { }
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
