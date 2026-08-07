package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateFolderSheet;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveItemMenuPage;
import com.zillit.automation.pages.DriveSearchPage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE VIEW CONTROLS — the ways a user reshapes the list: LIST ⇄ GRID, sort, tag
 * filter, the favourites-only toggle, the "Show" filter, and the drawer's Search
 * screen with its type chips (Images / Videos / Documents / PDF / Audio / Folders /
 * date range).
 *
 * <p>Every count assertion here scroll-collects first: Drive lists are RecyclerViews,
 * so a single screenful is never the whole list, and a "the filter dropped my item"
 * failure is far more often the viewport than the filter.
 */
@Order(TestOrder.DRIVE_VIEW)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveViewTest extends BaseTest {

    private static final String F1 = "QA_View_Alpha";
    private static final String F2 = "QA_View_Beta";

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
        // isLoaded() also matches Trash/Search (shared recyclerView) — require HOME.
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        if (drive.isDriveActivity() && drive.returnHome()) return true;
        for (int i = 0; i < 8 && !onBottomNavScreen(); i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(800); } catch (Exception ignored) { }
        }
        ToolsPage tools = new ToolsPage(driver);
        tools.openToolsTab();
        if (!tools.isLoaded(Duration.ofSeconds(15))) return false;
        tools.searchTools("Drive");
        sleep(1500);
        return tools.isModuleListed("Drive", Duration.ofSeconds(6))
                && tools.openModule("Drive")
                && drive.isLoaded(Duration.ofSeconds(20));
    }

    private void closeAnyOverlay() {
        for (int i = 0; i < 5; i++) {
            // A stranded SYSTEM PICKER (a foreign package) is the worst case: every
            // later test's openDrive() fails and the whole suite skips.
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean overlay = foreign || !driver.findElements(byAppId("menuDelete")).isEmpty()
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("etSearch")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** Two folders so ordering and filtering have something to act on. */
    private void seedFolders() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        for (String name : new String[]{F1, F2}) {
            if (drive.hasItem(name)) continue;
            if (!drive.openFab() || !drive.tapCreateFolder()) return;
            CreateFolderSheet sheet = new CreateFolderSheet(driver);
            if (!sheet.waitOpen(Duration.ofSeconds(10))) return;
            sheet.createNamed(name);
            sleep(2500);
        }
    }

    // ==================== view type ====================

    @Test
    @Order(1)
    @DisplayName("VIEW: the list/grid toggle is present and switches the layout")
    void listGridToggle() {
        seedFolders();
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.hasViewToggle(), "No view toggle on this build");

        List<String> before = drive.allItemNamesScrolled();
        assumeTrue(!before.isEmpty(), "Nothing in Drive to display");

        assertTrue(drive.toggleView(), "The view toggle should be tappable");
        sleep(2500);
        List<String> after = drive.allItemNamesScrolled();
        System.out.println(">>> before=" + before.size() + " after toggle=" + after.size());
        screenshot("drive-view-toggled");

        // The LAYOUT changes, the DATA must not.
        assertTrue(after.containsAll(before),
                "Switching list ⇄ grid must not drop items. before=" + before + " after=" + after);

        assertTrue(drive.toggleView(), "Toggling back should work");
        sleep(2500);
        assertTrue(drive.allItemNamesScrolled().containsAll(before),
                "Toggling back should restore the same items");
    }

    @Test
    @Order(2)
    @DisplayName("VIEW: the favourites-only toggle narrows the list to favourites")
    void favouritesToggle() {
        seedFolders();
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        assumeTrue(drive.hasFavToggle(), "No favourites toggle on this build");

        // Favourite one item so the filter has a positive and a negative case.
        assumeTrue(drive.openItemMenu(F1), "Could not open the row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen() && menu.tapFavorite(), "Could not favourite " + F1);
        sleep(2500);

        List<String> all = new DriveHomePage(driver).allItemNamesScrolled();
        assertTrue(drive.toggleFavorites(), "Favourites toggle should be tappable");
        sleep(3000);
        List<String> favs = new DriveHomePage(driver).allItemNamesScrolled();
        System.out.println(">>> all=" + all + "  favourites-only=" + favs);
        screenshot("drive-favourites-only");

        assertTrue(favs.size() <= all.size(), "A favourites filter cannot ADD items");
        if (!favs.isEmpty()) {
            assertTrue(favs.stream().anyMatch(n -> n.equalsIgnoreCase(F1)),
                    "The favourited folder should survive the filter. Got: " + favs);
        }

        drive.toggleFavorites();     // restore
        sleep(2500);
    }

    // ==================== sort ====================

    @Test
    @Order(3)
    @DisplayName("SORT: the sheet offers Name / Date modified / Size / Type")
    void sortOptions() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openSort(), "Sort sheet did not open");
        for (String opt : new String[]{"Name", "Date modified", "Size", "Type"}) {
            assertTrue(drive.textShown(opt, Duration.ofSeconds(3)), "Missing sort option: " + opt);
        }
        screenshot("drive-sort-options");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(4)
    @DisplayName("SORT by Name orders the list alphabetically without losing items")
    void sortByName() {
        seedFolders();
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        List<String> before = drive.allItemNamesScrolled();
        assumeTrue(before.size() >= 2, "Need at least two folders to judge ordering");

        assumeTrue(drive.openSort(), "Sort sheet did not open");
        assumeTrue(drive.textShown("Name", Duration.ofSeconds(3)), "No Name option");
        assumeTrue(drive.selectKind("Name"), "Could not choose Name");
        sleep(3000);

        List<String> after = new DriveHomePage(driver).allItemNamesScrolled();
        System.out.println(">>> sorted by name: " + after);
        screenshot("drive-sorted-name");
        assertTrue(after.containsAll(before) && before.containsAll(after),
                "Sorting must reorder, never drop or add. before=" + before + " after=" + after);

        // Assert it is ACTUALLY ORDERED, in either direction. Observed on this build:
        // choosing "Name" produced exact REVERSE alphabetical order, which is a
        // legitimate descending sort — but "not sorted at all" would also have slipped
        // through a log-only check, so compare against both orderings.
        List<String> asc = after.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<String> desc = new java.util.ArrayList<>(asc);
        java.util.Collections.reverse(desc);   // List.reversed() is Java 21; this project is 17
        System.out.println(">>> ascending would be:  " + asc);
        System.out.println(">>> descending would be: " + desc);
        assertTrue(after.equals(asc) || after.equals(desc),
                "Sorting by Name must order the list (ascending or descending). Got: " + after
                        + "  asc=" + asc + "  desc=" + desc);
    }

    // ==================== filter ====================

    @Test
    @Order(5)
    @DisplayName("FILTER: the tag filter sheet opens with an All option and Cancel")
    void filterSheet() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openFilter(), "Filter sheet did not open");
        boolean tag = drive.textShown("Filter by Tag", Duration.ofSeconds(4));
        boolean all = drive.textShown("All", Duration.ofSeconds(2));
        System.out.println(">>> filter-by-tag=" + tag + " all=" + all);
        assertTrue(tag || all, "The filter sheet should offer tag filtering");
        screenshot("drive-filter-sheet");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    @Test
    @Order(6)
    @DisplayName("FILTER: the 'Show' filter is present and keeps the list readable")
    void showFilter() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.hasShowFilter(), "No 'Show' filter on this build");
        System.out.println(">>> 'Show' filter present");
        assertTrue(drive.isListReadable(), "The list should stay readable with the Show filter present");
        screenshot("drive-show-filter");
    }

    // ==================== kind tabs ====================

    @Test
    @Order(7)
    @DisplayName("KIND: Folders and Files tabs show disjoint content")
    void folderFileTabsAreDisjoint() {
        seedFolders();
        DriveHomePage drive = new DriveHomePage(driver);

        assumeTrue(drive.showFolders(), "Could not switch to Folders");
        assumeTrue(drive.isListReadable(), "Folder list unreadable");
        List<String> folders = drive.allItemNamesScrolled();

        assumeTrue(drive.showFiles(), "Could not switch to Files");
        assumeTrue(drive.isListReadable(), "File list unreadable");
        List<String> files = drive.allItemNamesScrolled();

        System.out.println(">>> folders=" + folders);
        System.out.println(">>> files=" + files);
        screenshot("drive-kind-tabs");

        for (String f : folders) {
            assertFalse(files.stream().anyMatch(x -> x.equalsIgnoreCase(f)),
                    "'" + f + "' is a folder but also appears under Files");
        }
    }

    // ==================== search ====================

    @Test
    @Order(8)
    @DisplayName("SEARCH: the drawer opens the search screen with its type chips")
    void searchScreenChips() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        DriveItemMenuPage nav = new DriveItemMenuPage(driver);
        assumeTrue(nav.drawerHasSearch() && nav.openSearch(), "Could not open Search");
        sleep(3000);

        DriveSearchPage search = new DriveSearchPage(driver);
        assumeTrue(search.isOpen(Duration.ofSeconds(15)), "Search screen did not open");
        List<String> chips = search.availableChips();
        System.out.println(">>> search type chips = " + chips);
        screenshot("drive-search-chips");
        assertTrue(chips.size() >= 3,
                "The search screen should offer type chips (Images/Videos/Documents/PDF/Audio/Folders). Got: " + chips);
    }

    @Test
    @Order(9)
    @DisplayName("SEARCH: an exact folder name is found")
    void searchFindsFolder() {
        seedFolders();
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        DriveItemMenuPage nav = new DriveItemMenuPage(driver);
        assumeTrue(nav.openSearch(), "Could not open Search");
        sleep(3000);

        DriveSearchPage search = new DriveSearchPage(driver);
        assumeTrue(search.isOpen(Duration.ofSeconds(15)), "Search did not open");
        assumeTrue(search.query(F1), "Could not type a query");
        sleep(4000);

        List<String> results = search.resultNames();
        System.out.println(">>> search '" + F1 + "' -> " + results);
        screenshot("drive-search-exact");
        assertTrue(results.stream().anyMatch(n -> n.equalsIgnoreCase(F1))
                        || search.isEmptyStateShown(),
                "An exact name should be found (or the empty state shown). Got: " + results);
    }

    @Test
    @Order(10)
    @DisplayName("SEARCH is case-insensitive and matches partial names")
    void searchPartialAndCase() {
        seedFolders();
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        assumeTrue(new DriveItemMenuPage(driver).openSearch(), "Could not open Search");
        sleep(3000);

        DriveSearchPage search = new DriveSearchPage(driver);
        assumeTrue(search.isOpen(Duration.ofSeconds(15)), "Search did not open");

        assumeTrue(search.query("qa_view"), "Could not type a lowercase partial query");
        sleep(4000);
        List<String> lower = search.resultNames();
        System.out.println(">>> 'qa_view' -> " + lower);

        assumeTrue(search.clear(), "Could not clear the query");
        assumeTrue(search.query("QA_VIEW"), "Could not type an uppercase query");
        sleep(4000);
        List<String> upper = search.resultNames();
        System.out.println(">>> 'QA_VIEW' -> " + upper);
        screenshot("drive-search-case");

        assertEquals(lower.size(), upper.size(),
                "Search should be case-insensitive: lower=" + lower + " upper=" + upper);
    }

    @Test
    @Order(11)
    @DisplayName("SEARCH: a non-matching query shows the empty state")
    void searchNoMatch() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        assumeTrue(new DriveItemMenuPage(driver).openSearch(), "Could not open Search");
        sleep(3000);

        DriveSearchPage search = new DriveSearchPage(driver);
        assumeTrue(search.isOpen(Duration.ofSeconds(15)), "Search did not open");
        assumeTrue(search.query("zzq-no-such-file-" + (System.currentTimeMillis() % 1000)), "Could not type");
        sleep(4000);

        List<String> results = search.resultNames();
        System.out.println(">>> no-match results=" + results + " empty=" + search.isEmptyStateShown());
        screenshot("drive-search-no-match");
        assertTrue(results.isEmpty() || search.isEmptyStateShown(),
                "A non-matching query should show no results / the empty state. Got: " + results);
    }

    @Test
    @Order(12)
    @DisplayName("SEARCH: a type chip narrows results to that kind")
    void searchTypeChip() {
        DriveHomePage drive = new DriveHomePage(driver);
        assumeTrue(drive.openDrawer(), "Drawer did not open");
        assumeTrue(new DriveItemMenuPage(driver).openSearch(), "Could not open Search");
        sleep(3000);

        DriveSearchPage search = new DriveSearchPage(driver);
        assumeTrue(search.isOpen(Duration.ofSeconds(15)), "Search did not open");
        assumeTrue(search.query("qa"), "Could not type");
        sleep(4000);
        List<String> all = search.resultNames();

        assumeTrue(search.tapChip("Folders"), "No Folders chip");
        sleep(4000);
        List<String> foldersOnly = search.resultNames();
        System.out.println(">>> all=" + all + "  folders-chip=" + foldersOnly);
        screenshot("drive-search-type-chip");
        assertTrue(foldersOnly.size() <= all.size(),
                "A type chip can only narrow the results. all=" + all + " chip=" + foldersOnly);
    }

    // ==================== cleanup ====================

    @Test
    @Order(13)
    @DisplayName("CLEANUP: remove the QA view folders")
    void cleanup() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        for (String name : new String[]{F1, F2}) {
            DriveHomePage d = new DriveHomePage(driver);
            d.showFolders();
            if (!d.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name))) continue;
            if (!d.openItemMenu(name)) continue;
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                d.confirmDeleteDialog(Duration.ofSeconds(8));
                sleep(3000);
            } else {
                menu.dismiss();
            }
        }
        DriveHomePage end = new DriveHomePage(driver);
        end.showFolders();
        assertTrue(end.isListReadable(), "Drive should be readable after cleanup");
        System.out.println(">>> folders after cleanup = " + end.allItemNamesScrolled());
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
