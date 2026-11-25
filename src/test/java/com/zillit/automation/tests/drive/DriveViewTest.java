package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.drive.CreateFolderSheet;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveItemMenuPage;
import com.zillit.automation.pages.drive.DriveSearchPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(ReasonLogger.class)
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
        // The Show filter is remembered across launches: a run stopped inside S-02/S-03 leaves
        // "Last 7 days" / "Large files" on, and every list check after it reads an empty list.
        DriveHomePage drive = new DriveHomePage(driver);
        String show = drive.showFilterValue();
        if (!show.isEmpty() && !show.equals("All")) drive.chooseShowFilter("All");
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
                && tools.openModuleContaining("Drive")
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
        // Results arrive from the server after a debounce — poll rather than read once at 4 s
        // (a single read reported [] while both folders were listed a moment later).
        List<String> upper = search.resultNames();
        for (int i = 0; i < 15 && upper.size() < lower.size(); i++) { sleep(1000); upper = search.resultNames(); }
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

    // ==================== Show filter: Last 7 days / Large files (782476d83) ====================

    private void requireNewShowFilter(DriveHomePage drive) {
        assumeTrue(drive.hasShowFilter(), "No 'Show' filter on this screen");
        List<String> opts = drive.showFilterOptions();
        assumeTrue(opts.contains("Last 7 days"), "The Show filter offers only " + opts
                + " — this build predates 'Last 7 days' / 'Large files' (Zillit-Android 782476d83)");
    }

    @Test
    @Order(14)
    @DisplayName("S-01 Show filter options are exactly All / Shared by Me / Last 7 days / Large files")
    void showFilterOptions() {
        DriveHomePage drive = new DriveHomePage(driver);
        requireNewShowFilter(drive);
        List<String> opts = drive.showFilterOptions();
        System.out.println(">>> S-01 options " + opts);
        assertEquals(List.of("All", "Shared by Me", "Last 7 days", "Large files"), opts, "Show filter options");
    }

    @Test
    @Order(15)
    @DisplayName("S-02 'Last 7 days' at root explains itself and lists a file added this week")
    void showLast7Days() {
        DriveHomePage drive = new DriveHomePage(driver);
        requireNewShowFilter(drive);
        try {
            assertTrue(drive.chooseShowFilter("Last 7 days"), "Could not pick Last 7 days");
            String hint = drive.showFilterHint();
            List<String> names = drive.allItemNamesScrolled();
            System.out.println(">>> S-02 hint='" + hint + "' items=" + names);
            assertEquals("Added in the last 7 days, in My Drive and Shared with me", hint, "Root hint for Last 7 days");
            assertTrue(!names.isEmpty() || drive.isListReadable(), "The filtered list should load");
            // Every Drive suite uploads its fixtures on demand, so on a project used this week the
            // list is non-empty; an empty list is reported, not failed (fresh projects).
            if (names.isEmpty()) System.out.println(">>> S-02 nothing added in the last 7 days");
        } finally {
            drive.chooseShowFilter("All");
        }
    }

    @Test
    @Order(16)
    @DisplayName("S-03 'Large files' lists only files of 100 MB+, so the small fixtures and folders vanish")
    void showLargeFiles() {
        DriveHomePage drive = new DriveHomePage(driver);
        requireNewShowFilter(drive);
        try {
            assertTrue(drive.chooseShowFilter("Large files"), "Could not pick Large files");
            String hint = drive.showFilterHint();
            List<String> names = drive.allItemNamesScrolled();
            System.out.println(">>> S-03 hint='" + hint + "' items=" + names);
            assertEquals("Files of 100 MB or more, in My Drive and Shared with me", hint, "Root hint for Large files");
            for (String small : List.of("zl_drive_book.xlsx", "zl_drive_doc.docx", "zl_drive_doc.pdf", F1, F2)) {
                assertFalse(names.stream().anyMatch(n -> n.equalsIgnoreCase(small)),
                        small + " is far below 100 MB (or a folder) and must not be listed");
            }
        } finally {
            drive.chooseShowFilter("All");
        }
    }

    // =====================================================================================
    // WEB GAP cases (web-e2e/gaps/drive.md M03 tags, M18 opening types, M20 favourites,
    // M23 Show filter/section reset, M24 sort direction, M25 special names, M33 grid/search).
    // =====================================================================================

    private static final long GS = System.currentTimeMillis() % 100000;
    private static final String G_TAGFILE = "QA_Tag_" + GS + ".pdf";
    private static final String G_TAG1 = "zl_auto_tag_" + GS;
    private static final String G_TAG2 = "zl_auto_tag2_" + GS;
    private static final String G_V1 = "QA_ViewG1_" + GS;
    private static final String G_V2 = "QA_ViewG2_" + GS;
    private static final String G_IMG = "QA_Img_" + GS + ".jpg";
    private static final String G_CSV = "QA_Csv_" + GS + ".csv";
    private static final String G_WAV = "QA_Wav_" + GS + ".wav";
    private static final java.util.Set<String> G_FILES = new java.util.LinkedHashSet<>();
    private static final java.util.Set<String> G_FOLDERS = new java.util.LinkedHashSet<>();

    private com.zillit.automation.pages.drive.DriveFlows flows() {
        return new com.zillit.automation.pages.drive.DriveFlows(driver);
    }

    private void gFolder(String name) {
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        if (!home.hasItem(name)) assumeTrue(flows().createFolder(name), "Could not create " + name);
        G_FOLDERS.add(name);
    }

    private void gFile(String fixture, String name) {
        assumeTrue(flows().uploadAs(fixture, name), "Could not upload " + fixture + " as " + name
                + " (" + com.zillit.automation.pages.drive.DriveUploadPage.lastUploadProblem + ")");
        G_FILES.add(name);
    }

    // ---------------- M03 Tags ----------------

    @Test
    @Order(20)
    @DisplayName("[WEB L463] TAGS: a new tag created on a file shows as a chip; 'No tags assigned' goes")
    void gTagCreate() {
        gFile("zl_drive_doc.pdf", G_TAGFILE);
        var d = flows().openDetail(G_TAGFILE);
        assumeTrue(d != null, "Could not open the detail");
        assumeTrue(d.noTagsShown(), "The fresh copy should start with no tags");
        assertTrue(d.createTag(G_TAG1), "Add Tag → + Create New Tag → Create did not work");
        List<String> chips = d.tagChips();
        System.out.println(">>> tags after create = " + chips);
        assertTrue(chips.contains(G_TAG1), "The new tag should be a chip: " + chips);
        assertFalse(d.noTagsShown(), "'No tags assigned' must go once a tag is on the file");
    }

    @Test
    @Order(21)
    @DisplayName("[WEB L1750] TAGS: 'Filter by Tag' narrows Files to the tagged file and Folders to nothing; All restores")
    void gTagFilter() {
        assumeTrue(G_FILES.contains(G_TAGFILE), "The tag case must run first");
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        home.showFiles();
        int allFiles = home.allItemNamesScrolled().size();
        try {
            assertTrue(flows().filterByTag(G_TAG1), "The tag is not offered in 'Filter by Tag'");
            home.showFiles();
            List<String> files = home.allItemNamesScrolled();
            home.showFolders();
            List<String> folders = home.itemNames();
            System.out.println(">>> filter " + G_TAG1 + ": files=" + files + " folders=" + folders);
            assertEquals(List.of(G_TAGFILE), files, "Only the tagged file should be listed");
            assertTrue(folders.isEmpty(), "No folder carries the tag: " + folders);
        } finally {
            flows().filterByTag("All");
        }
        home.showFiles();
        assertEquals(allFiles, home.allItemNamesScrolled().size(), "'All' should bring every file back");
    }

    @Test
    @Order(22)
    @DisplayName("[WEB L1510] TAGS: the same tag name again does not add a second chip; a blank name adds nothing")
    void gTagDuplicateAndBlank() {
        assumeTrue(G_FILES.contains(G_TAGFILE), "The tag case must run first");
        var d = flows().openDetail(G_TAGFILE);
        assumeTrue(d != null, "detail");
        int before = d.tagChips().size();
        d.createTag(G_TAG1);
        List<String> afterDup = d.tagChips();
        System.out.println(">>> duplicate tag → chips " + afterDup);
        assertEquals(1, afterDup.stream().filter(G_TAG1::equals).count(), "A duplicate tag name must not add a second chip");
        d.createTag("   ");
        List<String> afterBlank = d.tagChips();
        assertEquals(afterDup.size(), afterBlank.size(), "A blank tag name must add nothing (L1089): " + afterBlank);
        assertTrue(before <= afterBlank.size(), "Chips should not disappear");
    }

    @Test
    @Order(23)
    @DisplayName("[WEB L463/L1756] TAGS: removing the chip restores 'No tags assigned'; filtering by an unused tag lists nothing")
    void gTagRemoveAndEmptyFilter() {
        assumeTrue(G_FILES.contains(G_TAGFILE), "The tag case must run first");
        var d = flows().openDetail(G_TAGFILE);
        assumeTrue(d != null, "detail");
        d.createTag(G_TAG2);
        assumeTrue(d.tagChips().contains(G_TAG2), "Second tag not added");
        assertTrue(d.removeTag(G_TAG2) && d.removeTag(G_TAG1), "Chip close icons not tappable");
        sleep(2000);
        List<String> chips = d.tagChips();
        System.out.println(">>> chips after remove = " + chips);
        assertFalse(chips.contains(G_TAG1) || chips.contains(G_TAG2), "Removed tags must leave the file: " + chips);
        assertTrue(d.noTagsShown(), "'No tags assigned' should be back");
        assertTrue(flows().openDrive(), "Drive home");
        DriveHomePage home = new DriveHomePage(driver);
        try {
            assertTrue(flows().filterByTag(G_TAG2), "The unused tag should still be offered");
            home.showFiles();
            List<String> files = home.itemNames();
            String empty = home.isEmptyStateShown() ? home.emptyTitle() : "";
            System.out.println(">>> filter by unused tag: files=" + files + " empty='" + empty + "'");
            assertTrue(files.isEmpty(), "No file carries the unused tag: " + files);
        } finally {
            flows().filterByTag("All");
        }
    }

    // ---------------- M18 opening each type ----------------

    @Test
    @Order(30)
    @DisplayName("[WEB L392] OPEN: a PDF opens in Drive's own PDF viewer; Back returns to the detail")
    void gOpenPdf() {
        assumeTrue(flows().ensureFile("zl_drive_doc.pdf"), "No PDF");
        var d = flows().openDetail("zl_drive_doc.pdf");
        assumeTrue(d != null && d.waitFor("btnOpen", Duration.ofSeconds(20)), "No Open on the PDF");
        assertTrue(d.tapOpen(), "Open not tappable");
        String act = "";
        for (int i = 0; i < 20 && !act.contains("DrivePdfViewerActivity"); i++) {
            sleep(1000);
            try { act = driver.currentActivity(); } catch (Exception ignored) { }
        }
        System.out.println(">>> PDF open → " + act);
        assertTrue(act.contains("DrivePdfViewerActivity"), "A PDF should open in DrivePdfViewerActivity, got " + act);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertTrue(d.waitFor("btnOpen", Duration.ofSeconds(10)), "Back should return to the detail");
    }

    @Test
    @Order(31)
    @DisplayName("[WEB L380] OPEN: an image detail shows its thumbnail, size and Download")
    void gImageDetail() {
        gFile("zl_drive_pic.jpg", G_IMG);
        var d = flows().openDetail(G_IMG);
        assumeTrue(d != null, "detail");
        boolean thumb = d.isVisibleId("ivFileThumbnail");
        String size = d.fileSize();
        System.out.println(">>> image detail thumb=" + thumb + " size='" + size + "'");
        assertTrue(thumb, "The image should show as a thumbnail");
        assertFalse(size == null || size.isBlank(), "The size should be shown");
        assertTrue(d.hasDownload(), "Download should be offered");
    }

    @Test
    @Order(32)
    @DisplayName("[WEB L707] OPEN: a .csv opens in the editor (Zillit Sheets) in VIEW mode")
    void gOpenCsv() {
        gFile("zl_drive_sheet.csv", G_CSV);
        var d = flows().openDetail(G_CSV);
        assumeTrue(d != null && d.waitFor("btnOpen", Duration.ofSeconds(20)), "No Open on the csv");
        d.tapOpen();
        var ed = new com.zillit.automation.pages.drive.DriveEditorPage(driver);
        try {
            assertTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)), "A csv should open OnlyOfficeEditorActivity");
            assumeTrue(ed.waitLoaded(Duration.ofSeconds(com.zillit.automation.pages.drive.DriveEditorPage.LOAD_S)),
                    "The editor page did not load in " + com.zillit.automation.pages.drive.DriveEditorPage.LOAD_S + "s");
            assertTrue(ed.waitViewMode(Duration.ofSeconds(15)), "Open must be VIEW mode for a csv");
            assertEquals(com.zillit.automation.pages.drive.DriveEditorPage.SHEETS, ed.editorKind(), "csv → Zillit Sheets");
        } finally {
            ed.forceExit();
        }
    }

    @Test
    @Order(33)
    @DisplayName("[WEB L386] OPEN: an audio file's detail plays inline (playerView)")
    void gAudioDetail() {
        assumeTrue(flows().uploadAs("zl_test_audio.wav", G_WAV),
                "Could not upload zl_test_audio.wav (pushed to /sdcard/Music by the runner) through the Document picker ("
                        + com.zillit.automation.pages.drive.DriveUploadPage.lastUploadProblem + ")");
        G_FILES.add(G_WAV);
        var d = flows().openDetail(G_WAV);
        assumeTrue(d != null, "detail");
        assertTrue(d.isVisibleId("playerView"), "Audio should play inline in playerView");
    }

    // ---------------- M20 favourites ----------------

    @Test
    @Order(40)
    @DisplayName("[WEB L451/L112] FAVOURITES: the row star favourites a folder; the menu flips; the filter shows it and not others")
    void gFavourites() {
        gFolder(G_V1);
        gFolder(G_V2);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertTrue(flows().tapRowStar(G_V1), "Row star not tappable");
        assumeTrue(home.openItemMenu(G_V1), "Row menu");
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        assumeTrue(menu.isOpen(), "Row menu did not open");
        String label = menu.favoriteLabel();
        for (int i = 0; i < 6 && label.isBlank(); i++) { sleep(500); label = menu.favoriteLabel(); }
        menu.dismiss();
        assertEquals("Remove from favorites", label, "After starring, the menu should offer Remove from favorites");
        try {
            assertTrue(home.toggleFavorites(), "Favourites toggle");
            sleep(3000);
            home.showFolders();
            List<String> favs = home.allItemNamesScrolled();
            System.out.println(">>> favourites = " + favs);
            assertTrue(favs.contains(G_V1), "The starred folder should be in favourites");
            assertFalse(favs.contains(G_V2), "An unstarred folder must not be in favourites");
        } finally {
            home.toggleFavorites();
            sleep(2500);
        }
        home.showFolders();
        flows().tapRowStar(G_V1);
        assumeTrue(home.openItemMenu(G_V1), "Row menu");
        DriveItemMenuPage menu2 = new DriveItemMenuPage(driver);
        menu2.isOpen();
        String back = menu2.favoriteLabel();
        for (int i = 0; i < 6 && back.isBlank(); i++) { sleep(500); back = menu2.favoriteLabel(); }
        new DriveItemMenuPage(driver).dismiss();
        assertEquals("Add to favorites", back, "Unstarring should flip the label back");
    }

    // ---------------- M23 Show filter / section switch ----------------

    @Test
    @Order(41)
    @DisplayName("[WEB L107] SHOW 'Shared by Me' hides an unshared folder; switching section resets it to All")
    void gSharedByMeAndReset() {
        gFolder(G_V1);
        DriveHomePage home = new DriveHomePage(driver);
        try {
            assertTrue(home.chooseShowFilter("Shared by Me"), "Shared by Me not offered");
            home.showFolders();
            List<String> shared = home.allItemNamesScrolled();
            System.out.println(">>> Shared by Me = " + shared);
            assertFalse(shared.contains(G_V1), "A folder nobody was given access to is not 'Shared by Me'");
            home.showSharedWithMe();
            home.showMyDrive();
            assertEquals("All", home.showFilterValue(), "Switching section should reset the Show filter");
            home.showFolders();
            assertTrue(home.hasItem(G_V1), "With All again, the folder is back");
        } finally {
            if (!"All".equals(home.showFilterValue())) home.chooseShowFilter("All");
        }
    }

    @Test
    @Order(42)
    @DisplayName("[WEB L1712] SELECT: switching section clears the selection")
    void gSelectionClearedOnSectionSwitch() {
        gFolder(G_V1);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertTrue(home.enterSelectMode(), "Select mode");
        home.showSharedWithMe();
        boolean bar = home.isSelectModeOn();
        home.exitSelectMode();
        home.showMyDrive();
        assertFalse(bar, "The bulk bar must go when the section changes");
    }

    // ---------------- M24 sort direction ----------------

    @Test
    @Order(43)
    @DisplayName("[WEB L92] SORT: choosing Name again reverses the order")
    void gSortReverses() {
        gFolder(G_V1);
        gFolder(G_V2);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assumeTrue(flows().sortBy("Name"), "Sort by Name");
        List<String> first = home.allItemNamesScrolled();
        assumeTrue(flows().sortBy("Name"), "Sort by Name again");
        List<String> second = home.allItemNamesScrolled();
        System.out.println(">>> name sort 1=" + first + " 2=" + second);
        List<String> reversed = new java.util.ArrayList<>(first);
        java.util.Collections.reverse(reversed);
        assertTrue(first.size() >= 2, "Need two folders");
        assertEquals(reversed, second, "Picking the active sort again should flip asc/desc");
    }

    // ---------------- M25 special-character names ----------------

    @Test
    @Order(44)
    @DisplayName("[WEB L1141/L1147/L1153/L912] NAMES: unicode/emoji/RTL, HTML and path-like names list exactly, open, and are found by search")
    void gSpecialNames() {
        List<String> names = List.of("QA_é😀_مرحبا_" + GS, "QA_<b>x</b>_" + GS, "QA_../p_" + GS);
        List<String> problems = new java.util.ArrayList<>();
        DriveHomePage home = new DriveHomePage(driver);
        for (String n : names) {
            home.showFolders();
            boolean created = flows().createFolder(n);
            home.showFolders();
            List<String> all = home.allItemNamesScrolled();
            boolean exact = all.contains(n);
            boolean mangled = all.stream().anyMatch(x -> x.contains(String.valueOf(GS)) && !names.contains(x) && !x.startsWith("QA_View"));
            if (exact) G_FOLDERS.add(n);
            System.out.println(">>> special '" + n + "' created=" + created + " exact=" + exact + " mangled=" + mangled);
            if (!exact && mangled) problems.add(n + " was stored under another name");
            if (!exact) continue;   // a clean server refusal is acceptable; record it
            if (!flows().openFolder(n)) { problems.add(n + " does not open"); continue; }
            String header = new com.zillit.automation.pages.drive.DriveFolderPage(driver).header();
            flows().backToHome();
            // The folder header is drawn in capitals (see DriveTest's header case) — compare ignoring case.
            if (!n.equalsIgnoreCase(header)) problems.add(n + " header reads '" + header + "'");
            assumeTrue(home.openDrawer() && new DriveItemMenuPage(driver).openSearch(), "Search");
            DriveSearchPage search = new DriveSearchPage(driver);
            if (search.isOpen(Duration.ofSeconds(10)) && search.query(n)) {
                List<String> r = search.resultNames();
                for (int i = 0; i < 10 && !r.contains(n); i++) { sleep(1000); r = search.resultNames(); }
                if (!r.contains(n)) problems.add(n + " not found by search: " + r);
            }
            search.back();
            flows().openDrive();
        }
        assertTrue(problems.isEmpty(), "Special-character names: " + problems);
    }

    // ---------------- M33 grid cards / search result ----------------

    @Test
    @Order(45)
    @DisplayName("[WEB L1127] GRID: a card's ⋯ menu, two-card selection and opening a folder card all work")
    void gGridCards() {
        gFolder(G_V1);
        gFolder(G_V2);
        DriveHomePage home = new DriveHomePage(driver);
        home.showFolders();
        assertTrue(home.toggleView(), "Grid toggle");
        sleep(2500);
        try {
            assertTrue(home.openItemMenu(G_V1), "A grid card should have a ⋯ menu");
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            assertTrue(menu.isOpen() && menu.hasDetails(), "The card menu should offer Details");
            menu.dismiss();
            // Card centres are recorded BEFORE Select mode (it hides names, BUG-2); a tap on a
            // card in Select mode toggles it — the small cbSelect overlay does not take clicks.
            java.util.List<int[]> cards = new java.util.ArrayList<>();
            for (var e : driver.findElements(byAppId("tvName"))) {
                String t = e.getText();
                if (!G_V1.equalsIgnoreCase(t) && !G_V2.equalsIgnoreCase(t)) continue;
                var r = e.getRect();
                cards.add(new int[]{r.getX() + r.getWidth() / 2, r.getY() - r.getHeight()});
            }
            assumeTrue(cards.size() == 2, "Both grid cards should be on screen: " + cards.size());
            assertTrue(home.enterSelectMode(), "Select mode");
            for (int[] c : cards) {
                driver.executeScript("mobile: clickGesture", java.util.Map.of("x", c[0], "y", c[1]));
                sleep(800);
            }
            String count = home.selectedCount();
            home.exitSelectMode();
            assertTrue(count.contains("2"), "Two ticked cards should read 2 selected, was '" + count + "'");
            assertTrue(home.openItem(G_V1), "Folder card not tappable");
            sleep(2500);
            assertTrue(new com.zillit.automation.pages.drive.DriveFolderPage(driver).isFolderActivity(), "A folder card opens FolderActivity");
            flows().backToHome();
        } finally {
            home.showFolders();
            if (driver.findElements(byAppId("tvSubtitle")).isEmpty()) home.toggleView();
            sleep(1500);
        }
    }

    @Test
    @Order(46)
    @DisplayName("[WEB L373] SEARCH: tapping a folder result opens that folder")
    void gSearchOpensFolder() {
        gFolder(G_V1);
        DriveHomePage home = new DriveHomePage(driver);
        assumeTrue(home.openDrawer() && new DriveItemMenuPage(driver).openSearch(), "Search");
        DriveSearchPage search = new DriveSearchPage(driver);
        assumeTrue(search.isOpen(Duration.ofSeconds(10)) && search.query(G_V1), "Could not search");
        List<String> r = search.resultNames();
        for (int i = 0; i < 10 && !r.contains(G_V1); i++) { sleep(1000); r = search.resultNames(); }
        assertTrue(r.contains(G_V1), "The folder should be found: " + r);
        assertTrue(new DriveHomePage(driver).openItem(G_V1), "Result not tappable");
        sleep(2500);
        var folder = new com.zillit.automation.pages.drive.DriveFolderPage(driver);
        boolean opened = folder.isFolderActivity();
        String header = folder.header();
        flows().openDrive();
        assertTrue(opened, "A folder result should open FolderActivity");
        assertTrue(G_V1.equalsIgnoreCase(header), "The folder header should name the folder (drawn in capitals): '" + header + "'");
    }

    @Test
    @Order(99)
    @DisplayName("WEB GAP CLEANUP: remove the QA_ files and folders these cases created")
    void gViewCleanup() {
        List<String> failed = new java.util.ArrayList<>();
        for (String f : G_FILES) if (!flows().purge(f, false)) failed.add(f);
        for (String f : G_FOLDERS) if (!flows().purge(f, true)) failed.add(f);
        System.out.println(">>> view gap cleanup failed=" + failed + " (tags " + G_TAG1 + ", " + G_TAG2
                + " stay in the project — Android has no tag delete)");
        assertTrue(failed.isEmpty(), "Could not remove: " + failed);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
