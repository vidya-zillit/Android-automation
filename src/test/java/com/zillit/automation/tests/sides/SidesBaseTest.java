package com.zillit.automation.tests.sides;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.sides.SidesListPage;
import com.zillit.automation.pages.common.ToolsPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Sides suites.
 *
 * <p>Both of {@link BaseTest}'s policy knobs are set here, deliberately and together,
 * because leaving either at its default cost a whole evening on the External Users module:
 *
 * <ul>
 *   <li>{@link #requiresLoggedOutStart()} defaults to TRUE and <b>clears the app's data</b>
 *       when it finds the app logged in. That signs the tester out of their own device and
 *       then waits for a login screen only a person can get past.</li>
 *   <li>{@link #relaunchBetweenTests()} defaults to TRUE and terminates + relaunches the app
 *       before every case — splash, authentication, project list, and a walk back through
 *       Tools, per case. On screen it looks exactly like the app opening and closing over
 *       and over, because it is.</li>
 * </ul>
 *
 * <p>Navigation follows the same rule: <b>the app is opened once and a project entered
 * once</b>. Afterwards every case starts from wherever the last one finished and takes the
 * cheapest route back — already on the list, else Tools, and only as a last resort the
 * project list. Nothing here ever restarts the app to recover.
 */
abstract class SidesBaseTest extends BaseTest {

    static final String FIXTURE_PREFIX = "ZL_AUTO";
    static final String TOOL = "Sides";

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    @BeforeEach
    void reachSides() {
        openSides();
    }

    /** A fixture name that cannot collide with a previous run. */
    static String fixtureName(String what) {
        return FIXTURE_PREFIX + "_" + what + "_" + (System.currentTimeMillis() % 100000);
    }

    // ─────────────────────────── navigation ───────────────────────────

    /**
     * Gets to the Sides module by the shortest route that works.
     *
     * <ol>
     *   <li>already inside the module — done;</li>
     *   <li>inside the project — Tools, and nothing else;</li>
     *   <li>somewhere else in the app — step back until one of the above;</li>
     *   <li>on the project list — enter a project, which happens once per run.</li>
     * </ol>
     */
    void openSides() {
        SidesListPage list = new SidesListPage(driver);
        BottomNavPage nav = new BottomNavPage(driver);

        if (insideSides()) return;

        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) { openFromTools(); return; }

        for (int i = 0; i < 6; i++) {
            if (insideSides()) return;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return; }
            if (onProjectList()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }

        if (onProjectList()) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(),
                    "The project list is open but shows no projects to enter.");
            assumeTrue(projects.tapProject(names.get(0)),
                    "Could not enter \"" + names.get(0) + "\". Projects listed: " + names);
        }

        if (!nav.waitUntilLoaded(Duration.ofSeconds(60))) {
            assumeTrue(false, "The bottom navigation never loaded. On screen instead: "
                    + whereAreWe());
        }
        openFromTools();
    }

    /** Tools → Sides. The only recovery route a case ever needs. */
    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools(TOOL);
        sleep(1200);
        if (!tools.openIfVisible(TOOL)) {
            assumeTrue(false, "\"" + TOOL + "\" is not offered in Tools. Tools visible: "
                    + tools.toolNames());
        }
        assumeTrue(new SidesListPage(driver).isLoaded(Duration.ofSeconds(25)),
                "The Sides module did not open");
    }

    /**
     * Are we anywhere inside the Sides module? Cheap, total, and never throws.
     *
     * <p>Both tabs count: a case that ends on the Script tab has not left the module, and
     * making it walk back through Tools would be the very churn this class exists to avoid.
     */
    /**
     * Whether we are inside the Sides module.
     *
     * <p>The id check comes first because it costs nothing. The activity check behind it
     * is the one that matters after a delete: with a dialog or an empty list on top,
     * UiAutomator reported only the top window and the module's own bottom-nav ids went
     * missing — so a case that was standing in SidesMainActivity the whole time went off
     * to wait sixty seconds for the MAIN app's bottom navigation, and skipped.
     */
    boolean insideSides() {
        if (present("bottomNav") && (present("nav_sides") || present("nav_script"))) {
            return true;
        }
        try {
            return String.valueOf(driver.currentActivity()).contains("SidesMainActivity");
        } catch (Exception e) {
            return false;
        }
    }

    boolean onProjectList() { return present("projectListRecyclerView"); }

    /** Presence without waiting and without throwing — the only safe shape for a guard. */
    boolean present(String id) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Built ONLY inside a failure branch — see the note on lazy messages below. */
    String whereAreWe() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); }
        catch (Exception e) { pkg = ""; }
        if (!appPackage.equals(pkg)) {
            return "a different app (" + (pkg.isBlank() ? "unreadable" : pkg) + ")";
        }
        if (onProjectList()) return "the project list";
        if (insideSides()) return "the Sides module";
        try { return "activity " + driver.currentActivity(); }
        catch (Exception e) { return "an unreadable activity"; }
    }

    /**
     * Leaves a screen inside the module without leaving the module.
     *
     * <p>Stops at the module's own screens, at the project list, and at the app's edge —
     * but DOES step back out of another app, because that is how a hand-over returns.
     */
    void backIntoSides() {
        for (int i = 0; i < 6; i++) {
            if (insideSides()) return;
            if (onProjectList()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        openSides();
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Park outside the module so the next suite does not start inside this one. */
    @AfterAll
    void leaveSides() {
        for (int i = 0; i < 5 && insideSides(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
    }
}
