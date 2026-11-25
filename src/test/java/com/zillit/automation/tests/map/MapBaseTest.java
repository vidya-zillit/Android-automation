package com.zillit.automation.tests.map;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.map.MapDialogPage;
import com.zillit.automation.pages.map.MapLocationFormPage;
import com.zillit.automation.pages.map.MapPage;
import com.zillit.automation.pages.map.MapTypeManagerPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared navigation for every Map suite.
 *
 * <p>Pulled out of {@code MapModuleTest} once a fourth suite needed the same twenty lines.
 * The parts worth keeping together are the two that have broken runs before:
 *
 * <ul>
 *   <li>{@link #backToMap()} stops its BACK loop at the bottom-nav boundary. BACK on a
 *       bottom-nav screen leaves the app, and every later case in the run then skips.</li>
 *   <li>{@link #openMap()} reactivates the app before navigating, so a suite that followed
 *       one which ended outside the app (a share sheet, the Places overlay) recovers
 *       instead of failing on the first assertion.</li>
 * </ul>
 */
@ExtendWith(ReasonLogger.class)
abstract class MapBaseTest extends BaseTest {

    /** Prefix for every fixture these suites create, so leftovers are identifiable. */
    static final String FIXTURE_PREFIX = "ZL AUTO";

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachMap() {
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
        if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
        openMap();
    }

    void openMap() {
        if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) return;

        // ALREADY INSIDE THE MAP MODULE, just not on the map itself.
        //
        // The Map is its own activity (NewMapPage) with the zone list, the location list
        // and both forms as fragments inside it. When a case ends on one of those, there
        // is no bottom nav and no Tools tab to tap — the diagnostic caught exactly this:
        //   "The Tools tab could not be tapped. Resumed activity: …/com.zillit.zillitapp.NewMapPage"
        // Going out to Tools and back in would be wrong as well as impossible; BACK walks
        // up to the map in one or two presses. Asked through adb, so a degraded session
        // cannot give a misleading answer.
        if (resumedActivity().contains("NewMapPage")) {
            for (int i = 0; i < 5; i++) {
                if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(900);
            }
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
        }
        // 14 of the 24 skips in the five-suite verification run read "Could not open
        // Tools (app is not on a bottom-nav screen)". Not one of them was an app
        // limitation: a case had left a dialog or an autocomplete popup open, such a
        // popup swallows BACK, and nothing in a Map suite ever restarts the app
        // (relaunchBetweenTests() is false here) — so a single stranded case silently
        // took every case after it. Recover properly rather than skipping.
        assumeTrue(recoverToBottomNav(),
                "The app could not be returned to a bottom-nav screen, even after"
                        + " clearing popups, backing out and restarting it");
        ToolsPage tools = new ToolsPage(driver);
        // The bottom nav being PRESENT does not mean it is reachable: a dialog or an
        // autocomplete popup sits on top of it, keeps it in the accessibility tree, and
        // swallows the tap. recoverToBottomNav() is satisfied in that state, so clear the
        // popup and try once more before calling it a skip.
        if (!tools.openToolsTab()) {
            clearBlockingPopup();
            sleep(600);
            // SAY WHAT IS ACTUALLY ON SCREEN. This message has appeared six times in one
            // run and I have guessed at its cause twice, wrongly. The resumed activity
            // comes from adb (outside the session, so it still answers when the session is
            // degraded) and names the screen the app is really on.
            assumeTrue(tools.openToolsTab(),
                    "The Tools tab could not be tapped. Resumed activity: " + resumedActivity()
                            + "; bottom nav present=" + onBottomNavScreen()
                            + "; project list present=" + onProjectList()
                            + "; map loaded=" + new MapPage(driver).isLoaded(Duration.ofSeconds(1)));
        }
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Map");
        assumeTrue(tools.isModuleListed("Map", Duration.ofSeconds(6)),
                "Map module not enabled on this project");
        assumeTrue(tools.openModule("Map"), "Could not open Map");
        assumeTrue(new MapPage(driver).isLoaded(Duration.ofSeconds(25)), "Map did not load");
    }

    /**
     * Gets the app back onto a bottom-nav screen, whatever the previous case left behind.
     *
     * <p>Escalates deliberately, cheapest first: decline a modal, unwind with BACK,
     * re-enter the project from its list, bring the app forward if another package has
     * it, and only as a last resort restart the process.
     *
     * @return true when the bottom nav is on screen
     */
    boolean recoverToBottomNav() {
        // Clear a modal FIRST, whatever screen we are on. A popup over a bottom-nav screen
        // leaves onBottomNavScreen() true while every tap goes to the popup, which is how
        // C-14 and C-18 skipped with the nav sitting right there on screen.
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        if (onBottomNavScreen()) return true;
        for (int i = 0; i < 4 && !onBottomNavScreen(); i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(1))) return true;
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
        // Last resort. A restart lands on the PROJECT LIST, outside the project, which is
        // why @AfterAll below avoids it — but enterProjectFromList() walks straight back
        // in, so the price is one cold start instead of a lost suite.
        System.out.println(">>> Nothing on screen responds to BACK — restarting the app"
                + " to recover the bottom nav");
        try {
            driver.executeScript("mobile: terminateApp", java.util.Map.of("appId", appPackage));
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
        } catch (Exception e) {
            return false;
        }
        landing = settleLanding(90, false);
        return onBottomNavScreen() || enterProjectFromList();
    }

    /** Re-enters the first project, when the app is sitting on the project list. */
    private boolean enterProjectFromList() {
        if (!onProjectList()) return false;
        ProjectListPage list = new ProjectListPage(driver);
        List<String> names = list.visibleProjectNames();
        if (names.isEmpty()) return false;
        if (!list.tapProject(names.get(0))) return false;
        return new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(45));
    }

    /**
     * Parks the app back on the bottom nav when the suite ends.
     *
     * <p>Without this, the next Map suite starts while the app sits on {@code NewMapPage}
     * — a pushed activity that {@code BaseTest.settleLanding()} does not recognise as any
     * known landing. It then waits the full 120s and throws
     * "No known landing screen appeared", failing every case in that suite before a single
     * assertion runs. The Map is the first module in this project reached as its own
     * activity rather than a bottom-nav tab, which is why nothing hit this before.
     */
    @AfterAll
    void parkOnBottomNav() {
        // Clear anything modal first: a dialog swallows BACK, and that is how the loop
        // used to give up with the app still deep inside NewMapPage.
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        for (int i = 0; i < 10; i++) {
            if (onBottomNavScreen()) return;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        // Deliberately NOT terminateApp here. Killing the process does land the next
        // session on a known screen, but it lands it on the PROJECT LIST — outside the
        // project — and every following suite then fails to find the bottom nav at all
        // ("Could not open Tools"). Bringing the existing process forward keeps the
        // in-project state that the Map suites depend on.
        try {
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
            sleep(2500);
        } catch (Exception ignored) { }
    }

    /**
     * The activity Android currently has resumed, straight from adb.
     *
     * <p>Deliberately NOT asked through the Appium session: when the session is the thing
     * that is unwell, its answers are what need checking. adb sees the device directly.
     */
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

    /** Did the app end up back on the project list? Asked instantly, never throws. */
    boolean onProjectList() {
        try { return !driver.findElements(byAppId("projectListRecyclerView")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    MapPage map() {
        MapPage p = new MapPage(driver);
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Map not loaded");
        return p;
    }

    void backToMap() {
        for (int i = 0; i < 5; i++) {
            if (new MapPage(driver).isLoaded(Duration.ofSeconds(2))) return;
            if (onBottomNavScreen()) break;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
        if (!new MapPage(driver).isLoaded(Duration.ofSeconds(1))) openMap();
    }

    /**
     * Closes anything covering the Map toolbar, so the next toolbar tap can land.
     *
     * <p>The in-screen search bar REPLACES the toolbar strip while it is open. A case that
     * searched and did not close it leaves every later case reporting "View All not
     * available" or "Pin mode not available" — which reads like a missing button, when the
     * button is simply behind the search bar. Cheap to call; does nothing when nothing is
     * open.
     */
    void ensureToolbarReady() {
        MapPage m = new MapPage(driver);
        if (m.searchBarShown(Duration.ofSeconds(1))) {
            // A bar that was JUST closed is still animating out and reads as shown; a
            // second tap on the toggle then RE-OPENS it, and with the bar open the strip
            // drops View All / Zones / Types (seen 2026-11-23: "View All not available.
            // Toolbar showed [btnSearch, btnFilter, btnPin]"). Let it settle, look again.
            sleep(1000);
            if (m.searchBarShown(Duration.ofMillis(200))) {
                m.closeInScreenSearch();
                sleep(1200);
            }
            // Still open: with the IME up the bar overlays the strip and the toggle tap lands
            // in etSearch instead. BACK closes the IME, then the bar — checked before each
            // press so it never walks the app out of the Map.
            for (int i = 0; i < 2 && m.searchBarShown(Duration.ofMillis(300)); i++) {
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(1000);
            }
        }
        // Pin mode left ON by an earlier case: createLocationByPin returns early on
        // several paths, and the diagnostic caught it — "pin mode active=true" while a
        // later case was asking for View All.
        if (m.pinModeActive(Duration.ofSeconds(1))) {
            m.togglePinMode();
            sleep(800);
        }
        try { clearBlockingPopup(); } catch (Exception ignored) { }
    }

    /** True once a city is selected — most of the toolbar does not exist before that. */
    boolean cityChosen() {
        return !new MapPage(driver).noCitySelected();
    }

    /**
     * Selects the first city on the project, if any.
     *
     * @return true when a city is now selected
     */
    boolean ensureCitySelected() {
        MapPage m = new MapPage(driver);
        if (cityChosen()) return true;
        if (!m.openCityPanel()) return false;
        if (!m.cityPanelShown(Duration.ofSeconds(8))) return false;
        List<String> cities = m.visibleCityNames();
        if (cities.isEmpty()) { m.closeCityPanel(); return false; }
        boolean ok = m.selectCity(cities.get(0));
        sleep(2500);
        return ok && cityChosen();
    }

    // ───────────────────────── fixtures ─────────────────────────
    //
    // These exist because the Map module cannot be exercised by opening screens. A case
    // that asserts "delete removes the row" needs a row it created, and one that asserts
    // "search finds it" needs something to find. Every helper creates data through the
    // app's OWN flow, and every suite deletes only what it created (the ZL AUTO prefix).

    /**
     * Creates a location by dropping a pin on the map — the app's real creation path.
     *
     * <p>Not the address field: {@code etAddress} is focusable=false and only Places can
     * fill it. The pin flow supplies lat/lng to the form, which reverse-geocodes them into
     * the address itself ({@code LocationFormFragment:161-184}), so only the name is typed.
     *
     * @param fx    where to drop it, as a fraction of the map width
     * @param fy    same, vertically. (0.5, 0.5) is the city centre and therefore inside it
     * @param name  the location name
     * @return true once the location is saved
     */
    boolean createLocationByPin(double fx, double fy, String name, String type) {
        MapPage m = new MapPage(driver);
        lastPinFailure = null;
        if (!cityChosen() && !ensureCitySelected()) return pinFailed("no city could be selected");
        if (!m.pinModeActive(Duration.ofSeconds(1)) && !m.togglePinMode()) {
            return pinFailed("pin mode would not start — btnPin not reachable");
        }
        if (!m.pinModeActive(Duration.ofSeconds(6))) return pinFailed("pin-mode banner never appeared");
        if (!m.dropPinAt(fx, fy)) return pinFailed("could not tap the map to drop a preview pin");
        if (!m.commitPin()) {
            return pinFailed("the \"Add Location\" info window did not open the form —"
                    + " the commit tap missed it at every offset");
        }

        // A pin outside the boundary raises a dialog BEFORE the form. Accept it, because
        // this helper's job is to produce a fixture — the dialog's own wording and its
        // Cancel path are asserted by the P-series, which must see it untouched.
        MapDialogPage dlg = new MapDialogPage(driver);
        if (dlg.shown(Duration.ofSeconds(3))) {
            if (!dlg.tap(MapDialogPage.BTN_YES)) {
                dlg.tapStartsWith("Pin in ");
            }
            sleep(2000);
        }

        MapLocationFormPage form = new MapLocationFormPage(driver);
        if (!form.isLoaded(Duration.ofSeconds(12))) return pinFailed("location form never loaded");
        if (!form.setName(name)) return pinFailed("could not type the name");
        if (type != null && !type.isBlank()) {
            // actvType is an AutoCompleteTextView: typing the name is enough, but the
            // dropdown it opens covers the Save button, so it has to be dismissed.
            form.setType(type);
            form.dismissTypeDropdown();
            sleep(600);
        }
        if (!form.save()) return pinFailed("Save was not available on the form");
        sleep(4000);
        boolean saved = !form.stillOpen();
        if (!saved) pinFailed("the form stayed open after Save — it was rejected");
        backToMap();
        if (m.pinModeActive(Duration.ofSeconds(1))) m.togglePinMode();   // leave pin mode off
        return saved;
    }

    /** Why the last createLocationByPin gave up. Null when it succeeded. */
    String lastPinFailure;

    private boolean pinFailed(String why) {
        lastPinFailure = why;
        System.out.println(">>> pin not created: " + why);
        return false;
    }

    /** Same, with no type set. */
    boolean createLocationByPin(double fx, double fy, String name) {
        return createLocationByPin(fx, fy, name, null);
    }

    /** Convenience: a pin at the city centre, which is inside the city by construction. */
    boolean createLocationByPin(String name) {
        return createLocationByPin(0.5, 0.5, name, null);
    }

    /**
     * Creates a location type through the header manager.
     *
     * <p>The add form is built in code with no resource ids at all
     * ({@code HeaderManagerFragment:156-369}), so its field is found by hint and its
     * button by the text "Create".
     */
    boolean createType(String name) {
        MapTypeManagerPage types = new MapTypeManagerPage(driver);
        lastTypeFailure = null;
        if (!types.isLoaded(Duration.ofSeconds(10))) return typeFailed("type manager never loaded");
        if (!types.openAdd()) return typeFailed("Add button not available — no posting access?");
        if (!types.formShown(Duration.ofSeconds(10))) return typeFailed("add-type sheet never appeared");
        // A BottomSheetDialog opens at its peek height. The Cancel/Create row is the LAST
        // thing in a sheet that also holds the name field, the emoji grid and the subtype
        // rows, so on a phone it starts below the fold — and a tap at an off-screen
        // coordinate does nothing. Expand the sheet before reaching for the button.
        types.expandSheet();
        if (!types.setTypeName(name)) return typeFailed("could not type into the name field");
        if (!types.submitForm()) {
            return typeFailed("the Create button could not be tapped. Buttons on screen: "
                    + types.visibleButtonTexts());
        }
        sleep(3000);
        if (types.typeNames().stream().anyMatch(n -> n != null && n.contains(name))) return true;
        return typeFailed("Create was tapped but \"" + name + "\" is not in the list: "
                + types.typeNames());
    }

    /** Why the last createType gave up. Null when it succeeded. */
    String lastTypeFailure;

    private boolean typeFailed(String why) {
        lastTypeFailure = why;
        System.out.println(">>> type not created: " + why);
        return false;
    }

    /** A fixture name that cannot collide with a previous run. */
    static String fixtureName(String what) {
        return FIXTURE_PREFIX + " " + what + " " + (System.currentTimeMillis() % 100000);
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
