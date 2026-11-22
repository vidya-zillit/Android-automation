package com.zillit.automation.tests.box;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.box.BoxSchedulePage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Production Diary / Box Schedule suites.
 *
 * <p>Everything learned the hard way on the Map module is applied here from the start:
 * the module is reached once and stayed in, recovery knows the module has its own
 * Activity, and nothing expensive runs on a failure path.
 */
@ExtendWith(ReasonLogger.class)
abstract class BoxBaseTest extends BaseTest {

    /** Prefix for every fixture these suites create, so leftovers are identifiable. */
    static final String FIXTURE_PREFIX = "ZL AUTO";

    /** The module's own Activity, as adb reports it. */
    private static final String BOX_ACTIVITY = "BoxScheduleActivity";

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachBoxSchedule() {
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
        if (new BoxSchedulePage(driver).isLoaded(Duration.ofSeconds(2))) return;
        openBoxSchedule();
    }

    void openBoxSchedule() {
        BoxSchedulePage box = new BoxSchedulePage(driver);
        if (box.isLoaded(Duration.ofSeconds(1))) return;

        // ALREADY INSIDE THE MODULE, just on one of its sub-screens. Like the Map, this is
        // its own Activity with the create forms and detail screens stacked on top — so
        // there is no bottom nav to find and no Tools tab to tap. BACK walks up to it.
        // Asked through adb so a degraded session cannot give a misleading answer.
        if (resumedActivity().contains(BOX_ACTIVITY)) {
            for (int i = 0; i < 5; i++) {
                if (box.isLoaded(Duration.ofSeconds(2))) return;
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(900);
            }
            if (box.isLoaded(Duration.ofSeconds(2))) return;
        }

        assumeTrue(recoverToBottomNav(),
                "The app could not be returned to a bottom-nav screen, even after clearing"
                        + " popups, backing out and restarting it");
        ToolsPage tools = new ToolsPage(driver);
        if (!tools.openToolsTab()) {
            clearBlockingPopup();
            sleep(600);
            assumeTrue(tools.openToolsTab(), "The Tools tab could not be tapped. Resumed"
                    + " activity: " + resumedActivity());
        }
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        // The tool's label comes from the SERVER per project (toolsInfo.unitName), so it
        // cannot be hard-coded. Try the names this module is known by, widest last, and
        // match on a CONTAINED word — the row reads "Production Diary/Box Schedule", which
        // no exact match on "Production" will ever hit.
        boolean opened = false;
        String tried = "";
        // The exact label first, confirmed by the project owner: "Production Diary/Box
        // Schedule". The shorter ones stay as fallbacks because this name is
        // server-supplied per project and another project may word it differently.
        for (String candidate : List.of("Production Diary/Box Schedule", "Production Diary",
                "Box Schedule", "Production", "Box")) {
            tried += "\"" + candidate + "\" ";
            // Search on the first word only: a slash in the query ("Production Diary/Box
            // Schedule") narrows the list to nothing on some builds, while the row itself
            // still carries the full label for the contains-match below.
            tools.searchTools(candidate.split("[/ ]")[0]);
            sleep(1200);
            if (tools.openModuleContaining(candidate)) { opened = true; break; }
        }
        assumeTrue(opened,
                "No tool matching any of [" + tried.trim() + "] could be opened. Tools on"
                        + " screen: " + tools.visibleModuleNames()
                        + ". Tool labels are server-supplied per project, so the right name"
                        + " may simply be different here.");

        assumeTrue(box.isLoaded(Duration.ofSeconds(25)),
                "The module did not load. Resumed activity: " + resumedActivity());
    }

    /** Gets the app back onto a bottom-nav screen, cheapest escalation first. */
    boolean recoverToBottomNav() {
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        if (onBottomNavScreen()) return true;
        for (int i = 0; i < 4 && !onBottomNavScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
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

    boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    boolean onProjectList() {
        try { return !driver.findElements(byAppId("projectListRecyclerView")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** The activity Android currently has resumed, straight from adb, never through Appium. */
    String resumedActivity() {
        try {
            String udid = com.zillit.automation.core.Config.get("zillit.udid");
            java.util.List<String> cmd = new java.util.ArrayList<>(java.util.List.of(adbBinary()));
            if (udid != null && !udid.isBlank()) { cmd.add("-s"); cmd.add(udid); }
            cmd.addAll(java.util.List.of("shell", "dumpsys", "activity", "activities"));
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("mResumedActivity") || line.contains("ResumedActivity:")) {
                        return line.trim();
                    }
                }
            }
            return "(no resumed activity line)";
        } catch (Exception e) {
            return "(adb unavailable: " + e.getMessage() + ")";
        }
    }

    private static String adbBinary() {
        for (String var : new String[]{"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
            String root = System.getenv(var);
            if (root == null || root.isBlank()) continue;
            java.nio.file.Path c = java.nio.file.Paths.get(root, "platform-tools", "adb");
            if (java.nio.file.Files.isExecutable(c)) return c.toString();
        }
        java.nio.file.Path home = java.nio.file.Paths.get(System.getProperty("user.home"),
                "Library", "Android", "sdk", "platform-tools", "adb");
        return java.nio.file.Files.isExecutable(home) ? home.toString() : "adb";
    }

    BoxSchedulePage box() {
        BoxSchedulePage p = new BoxSchedulePage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "The module is not loaded");
        return p;
    }

    /** Back to the module's main screen from wherever a case ended. */
    /**
     * Returns to the module's own screen from a page inside it — and no further.
     *
     * <p>It used to press BACK up to five times and then re-enter from Tools if the Box
     * screen still had not been recognised. A single slow frame was enough to overshoot:
     * one BACK too many leaves the module, the next ones land on Tools and then Home, and
     * the run pays for a full Tools → Production Diary walk after every case. Worse, it
     * throws away the state the next case was going to build on.
     *
     * <p>So the activity is the authority now: while the module's Activity is still
     * resumed, a BACK is a step inside the module and may be repeated; the moment it is
     * not, pressing BACK again would only take us further out, so we stop and come back in
     * deliberately.
     */
    void backToBox() {
        BoxSchedulePage box = new BoxSchedulePage(driver);
        for (int i = 0; i < 4; i++) {
            if (box.isLoaded(Duration.ofSeconds(2))) { box.closeDrawer(); return; }
            if (!resumedActivity().contains(BOX_ACTIVITY)) break;   // already outside
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (box.isLoaded(Duration.ofSeconds(2))) { box.closeDrawer(); return; }
        openBoxSchedule();
    }

    /** True when the module's own Activity is on top — we are still inside it. */
    boolean insideModule() {
        return resumedActivity().contains(BOX_ACTIVITY);
    }

    /** A fixture name that cannot collide with a previous run. */
    static String fixtureName(String what) {
        return FIXTURE_PREFIX + " " + what + " " + (System.currentTimeMillis() % 100000);
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    /**
     * Parks the app back on the bottom nav when the suite ends, so the next suite does not
     * start inside this module's Activity — the mistake that cost the Map suites a whole
     * run before anyone noticed.
     */
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
}
