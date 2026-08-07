package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DriveActivityPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveSettingsPage;
import com.zillit.automation.pages.DriveTrashPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE SETTINGS ("More") — area N of DRIVE_TEST_CASES.md.
 *
 * <p><b>The screen is built but not shipped.</b> {@code SettingsFragment} and its
 * {@code settingsFragment} destination exist, and the layout carries every id area N
 * needs ({@code rowTrash}, {@code rowActivity}, {@code cardTeamMembers}, {@code
 * tvVersion}, the diagnostics block). But the ONE navigation into it —
 * {@code DriveDrawerFragment}'s profile row — is commented out and starts the app-wide
 * {@code EditProfileDataActivity} instead, and nothing else in the drive module
 * references {@code R.id.settingsFragment}. So N-1, N-3 and N-4 cannot be exercised.
 *
 * <p>This suite therefore does what {@code DriveSharedWithMeTest.drawerNav} does for the
 * hidden Uploads/Favorites entries: it <b>asserts the screen is still unreachable</b>.
 * The day the navigation is restored, {@link #settingsScreenIsNotReachable} fails — and
 * that failure is the instruction to enable the rest of area N using
 * {@link DriveSettingsPage}.
 *
 * <p>N-2 ("Settings exposes Trash and Activity rows — both navigate correctly") IS
 * covered, against the entry points this build actually ships: the drawer's own Trash
 * and Activity rows, which lead to the same two destinations.
 */
@Order(TestOrder.DRIVE_SETTINGS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveSettingsTest extends BaseTest {

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
        assumeTrue(openDrive(), "Could not open Drive");
    }

    private boolean openDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        closeAnyOverlay();
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        if (drive.isDriveActivity() && drive.returnHome()) return true;
        for (int i = 0; i < 8 && !onBottomNavScreen(); i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(800); } catch (Exception ignored) { }
        }
        // Retried: the FIRST test of a suite regularly finds the Tools list with "Drive"
        // typed and the row highlighted, yet Drive never comes up — the tap lands while
        // the filtered list is still settling. One attempt turns that into a skipped case
        // blamed on Drive.
        ToolsPage tools = new ToolsPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            tools.openToolsTab();
            if (!tools.isLoaded(Duration.ofSeconds(15))) { sleep(2000); continue; }
            tools.searchTools("Drive");
            sleep(2000);
            if (tools.isModuleListed("Drive", Duration.ofSeconds(6)) && tools.openModule("Drive")) {
                // 40s, not 20s: on a loaded emulator Drive's first open takes longer than
                // that, and the retry then pressed Tools again — cancelling a launch that
                // was about to succeed, over and over.
                if (drive.isLoaded(Duration.ofSeconds(40))) return true;
            }
            if (drive.isDriveActivity()) return true;
            sleep(2000);
        }
        return false;
    }

    /**
     * Also handles the EDIT PROFILE activity the profile row opens — that is a different
     * screen of the app, not a Drive overlay, and leaving it stranded would fail every
     * later test's openDrive().
     */
    private void closeAnyOverlay() {
        for (int i = 0; i < 6; i++) {
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean offDrive = false;
            try {
                String act = driver.currentActivity();
                offDrive = act != null && act.contains("EditProfileDataActivity");
            } catch (Exception ignored) { }
            boolean overlay = foreign || offDrive
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()
                    || !driver.findElements(byAppId("rowTrash")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private String currentActivity() {
        try { return driver.currentActivity(); } catch (Exception e) { return ""; }
    }

    private String textOf(String id) {
        try { return driver.findElement(byAppId(id)).getText(); }
        catch (Exception e) { return ""; }
    }

    // ==================== N · SETTINGS ====================

    @Test
    @Order(1)
    @DisplayName("N-1: the drawer profile row does NOT open Drive's More screen (area N is unshipped)")
    void settingsScreenIsNotReachable() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        DriveItemMenuPage nav = new DriveItemMenuPage(driver);
        assumeTrue(nav.drawerHasProfile(), "The drawer profile row is missing");

        // The drawer profile row carries the identity N-1 asks for (name + email); that
        // much IS shipped, so assert it rather than skipping the whole case.
        String name = textOf("tvUserName");
        String email = textOf("tvUserEmail");
        System.out.println(">>> drawer profile: name='" + name + "' email='" + email + "'");
        assertFalse(name.isBlank(), "The drawer profile row should name the signed-in user");
        screenshot("drive-drawer-profile");

        try {
            driver.findElement(byAppId("profileRow")).click();
        } catch (Exception e) {
            assumeTrue(false, "Could not tap the profile row");
        }
        sleep(3500);

        DriveSettingsPage settings = new DriveSettingsPage(driver);
        boolean opened = settings.isOpen(Duration.ofSeconds(8));
        String activity = currentActivity();
        System.out.println(">>> profile row → activity=" + activity + " driveMoreScreen=" + opened);
        screenshot("drive-settings-not-reachable");

        // If this fails, the navigation was restored: switch area N on — N-2/N-3/N-4 are
        // ready to write against DriveSettingsPage, whose ids are already mapped.
        assertFalse(opened,
                "Drive's More screen is NOT wired up in this build (DriveDrawerFragment's "
                        + "navigateTo(R.id.settingsFragment) is commented out). It is now "
                        + "reachable — enable the Settings cases (area N).");

        // Whatever the row opened, get back to Drive so the next test starts clean.
        for (int i = 0; i < 4; i++) {
            if (new DriveHomePage(driver).isDriveActivity()) break;
            try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
        }
    }

    @Test
    @Order(2)
    @DisplayName("N-2: Trash and Activity both navigate correctly from the drawer")
    void trashAndActivityNavigate() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        DriveItemMenuPage nav = new DriveItemMenuPage(driver);
        assumeTrue(nav.drawerHasTrash() && nav.openTrash(), "Could not open Trash");
        sleep(3000);

        DriveTrashPage trash = new DriveTrashPage(driver);
        assertTrue(trash.hasEmptyTrash(),
                "Trash should show its Empty-Trash action — btnEmptyTrash is what tells "
                        + "Trash apart from every other screen sharing 'recyclerView'");
        screenshot("drive-settings-trash-nav");
        try { driver.navigate().back(); Thread.sleep(1500); } catch (Exception ignored) { }

        DriveHomePage home = new DriveHomePage(driver);
        assumeTrue(home.returnHome(), "Could not get back to Drive home");
        assumeTrue(home.openDrawer(), "Drawer did not open the second time");
        DriveItemMenuPage nav2 = new DriveItemMenuPage(driver);
        assumeTrue(nav2.drawerHasActivity() && nav2.openActivity(), "Could not open Activity");
        sleep(3000);

        DriveActivityPage log = new DriveActivityPage(driver);
        assertTrue(log.isOpen(Duration.ofSeconds(20)),
                "Activity should open its own log (a tvAction row or 'No recent activity'), "
                        + "not just some list");
        screenshot("drive-settings-activity-nav");
        log.back();
    }

    @Test
    @Order(3)
    @DisplayName("N-3/N-4: team members + diagnostics — SKIPPED while the More screen is unreachable")
    void teamMembersAndDiagnostics() {
        DriveSettingsPage settings = new DriveSettingsPage(driver);
        // Deliberately a skip, not a pass: these two cases have no reachable surface in
        // this build, and a green tick would claim coverage that does not exist.
        assumeTrue(settings.isOpen(Duration.ofSeconds(3)),
                "Drive's More screen is not reachable in this build (see "
                        + "settingsScreenIsNotReachable) — N-3 (team members) and N-4 "
                        + "(diagnostics) cannot be exercised until the profile row navigates "
                        + "to settingsFragment again.");

        assertTrue(settings.hasTeamMembers(), "The team-members card is expected");
        List<String> diagnostics = List.of(
                settings.userName(), settings.userEmail(), settings.projectId(),
                settings.deviceId(), settings.environment(), settings.version());
        System.out.println(">>> diagnostics = " + diagnostics);
        screenshot("drive-settings-diagnostics");
        for (String value : diagnostics) {
            assertFalse(value == null || value.isBlank(),
                    "Every diagnostics field should be populated. Got: " + diagnostics);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
