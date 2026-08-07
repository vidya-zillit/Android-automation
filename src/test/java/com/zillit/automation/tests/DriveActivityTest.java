package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveActivityPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE ACTIVITY LOG — area M of DRIVE_TEST_CASES.md: the drawer's Activity screen,
 * which records what happened in this project's Drive (create / upload / move / rename /
 * share / delete), newest first.
 *
 * <p>⚠ The screen shares {@code recyclerView} / {@code emptyState} / {@code swipeRefresh}
 * with Drive home AND Trash, so "the list is there" proves nothing about WHICH list.
 * {@link DriveActivityPage#isOpen} insists on a rendered {@code tvAction} row or the
 * "No recent activity" text — the only markers unique to this screen.
 */
@Order(TestOrder.DRIVE_ACTIVITY)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveActivityTest extends BaseTest {

    /** Stamped so an entry found in the log can only be the one this run created. */
    private static final String FOLDER = "QA_Act_" + (System.currentTimeMillis() % 100000);

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
        // isLoaded() also matches Trash/Activity (shared recyclerView) — require HOME.
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

    private void closeAnyOverlay() {
        for (int i = 0; i < 5; i++) {
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean overlay = foreign
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()
                    || !driver.findElements(byAppId("menuDelete")).isEmpty()
                    || !driver.findElements(byAppId("etFolderName")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(1000); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /**
     * Creates a folder at the Drive root and waits until the list actually shows it.
     *
     * <p>Retried end-to-end: coming back from another Drive screen, the FIRST tap on the
     * FAB is sometimes swallowed while the list re-renders — which skipped this suite's
     * only real case with a misleading "could not open Create Folder". Creation is also
     * a server round-trip, so the row is polled for rather than read once.
     */
    private boolean createFolder(String name) {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        boolean submitted = false;
        for (int attempt = 0; attempt < 3 && !submitted; attempt++) {
            if (drive.openFab() && drive.tapCreateFolder()) {
                CreateFolderSheet sheet = new CreateFolderSheet(driver);
                if (sheet.waitOpen(Duration.ofSeconds(10)) && sheet.createNamed(name)) {
                    submitted = true;
                    break;
                }
                // Half-open sheet — close it so the next attempt starts from the list.
                try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
            }
            sleep(2000);
        }
        if (!submitted) return false;

        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (new DriveHomePage(driver).hasItem(name)) return true;
            sleep(2500);
        }
        return false;
    }

    /**
     * Drawer → Activity.
     *
     * <p>The drawer tap is retried: right after a create, the list is still reloading and
     * a single {@code btnDrawer} click can be swallowed — which skipped this suite's
     * "create is recorded" case even though the folder had been created correctly.
     */
    private DriveActivityPage openActivityLog() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.returnHome();
        boolean opened = false;
        for (int attempt = 0; attempt < 3 && !opened; attempt++) {
            opened = drive.openDrawer();
            if (!opened) sleep(2000);
        }
        assumeTrue(opened, "Drawer did not open");
        DriveItemMenuPage nav = new DriveItemMenuPage(driver);
        assumeTrue(nav.drawerHasActivity(), "Activity is not offered in the drawer");
        assumeTrue(nav.openActivity(), "Could not tap Activity");
        sleep(3000);
        DriveActivityPage log = new DriveActivityPage(driver);
        assumeTrue(log.isOpen(Duration.ofSeconds(20)), "The activity log did not open");
        return log;
    }

    // ==================== M · ACTIVITY LOG ====================

    @Test
    @Order(1)
    @DisplayName("M-1: drawer → Activity opens the log (entries, or 'No recent activity')")
    void activityLogOpens() {
        DriveActivityPage log = openActivityLog();

        List<String> entries = log.entries();
        System.out.println(">>> activity entries (first screen) = " + entries
                + " empty=" + log.isEmptyShown());
        screenshot("drive-activity-log");
        // One of the two valid states must render; neither means the screen failed to bind.
        assertTrue(log.isReadable(),
                "The activity log must show either entries or its empty state");
        log.back();
    }

    @Test
    @Order(2)
    @DisplayName("M-2: creating a folder is recorded in the log")
    void createIsRecorded() {
        assumeTrue(createFolder(FOLDER), "Could not create the folder to look for in the log");

        DriveActivityPage log = openActivityLog();
        // The entry is written server-side after the create completes, so poll with a
        // refresh between attempts rather than reading the list once.
        boolean recorded = log.waitForEntryContaining(FOLDER, Duration.ofSeconds(45));
        System.out.println(">>> entries mentioning " + FOLDER + " = "
                + log.allEntriesScrolled().stream().filter(e -> e.contains(FOLDER)).toList());
        screenshot("drive-activity-recorded");
        assertTrue(recorded,
                "Creating '" + FOLDER + "' should appear in the activity log. Got: "
                        + log.allEntriesScrolled());
        log.back();
    }

    @Test
    @Order(3)
    @DisplayName("M-3: swipe-refresh reloads the log without losing it")
    void swipeRefreshReloads() {
        DriveActivityPage log = openActivityLog();
        boolean wasEmpty = log.isEmptyShown();
        // The TOP screenful, deliberately — not the scroll-collected set. The log pages
        // in as you scroll, so a reload restarts at page one and a second scroll-collect
        // legitimately gathers fewer rows; comparing full sets fails on pagination
        // timing rather than on anything the refresh did. The newest entries, which sit
        // at the top, are the part a reload must preserve.
        List<String> before = log.entries();

        assumeTrue(log.refresh(), "No swipe-refresh on the activity screen");
        sleep(4000);

        List<String> after = log.entries();
        System.out.println(">>> top entries before=" + before.size() + " after refresh=" + after.size());
        screenshot("drive-activity-refreshed");
        // A refresh must leave a usable screen — still the LOG (not a blank recycler that
        // any Drive screen would satisfy), and still holding what it had.
        assertTrue(log.isOpen(Duration.ofSeconds(15)),
                "The activity log should still be showing after a refresh");
        if (wasEmpty) {
            assertTrue(log.isReadable(), "An empty log must still render its empty state after a refresh");
        } else {
            assertTrue(after.containsAll(before),
                    "A refresh must not drop the newest entries. before=" + before + " after=" + after);
        }
        log.back();
    }

    @Test
    @Order(4)
    @DisplayName("CLEANUP: delete the QA folder this suite created")
    void cleanup() {
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assumeTrue(home.hasItem(FOLDER), "Nothing to clean up");
        assumeTrue(home.openItemMenu(FOLDER), "Could not open the row overflow");

        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapDelete(), "Could not tap Delete");
        sleep(1500);
        home.confirmDeleteDialog(Duration.ofSeconds(10));
        sleep(3000);

        DriveHomePage end = new DriveHomePage(driver);
        end.showFolders();
        assertTrue(end.isListReadable(), "Drive should be readable after cleanup");
        System.out.println(">>> folders after cleanup = " + end.allItemNamesScrolled());
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
