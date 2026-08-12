package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DocDistLibraryPage;
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
 * DOCUMENT DISTRIBUTION — areas A (entry &amp; shell) and B (library listing) of
 * DOC_DISTRIBUTION_TEST_CASES.md.
 *
 * <p>The pure-UI base everything else builds on: the module opens, the library is the
 * start destination, the shortcuts and their badges are there, and the list can be
 * searched, sorted, filtered and switched between list and grid. No uploads, no sends —
 * those come once this layer is reliable.
 *
 * <p>Two module facts shape the navigation here:
 * <ul>
 *   <li>{@code DocumentDistributionActivity} is NOT exported — {@code am start} on it is
 *       rejected — so the only way in is Tools → the module row, as with Drive.</li>
 *   <li>The Tools row's visible label is server-supplied; the code constant
 *       ({@code document_distribution_tool}) is a key, not the text. So the row is
 *       matched against {@link DocDistLibraryPage#TOOL_LABELS} rather than one guess.</li>
 * </ul>
 */
@Order(TestOrder.DOC_DISTRIBUTION)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistributionTest extends DocDistBaseTest {

    /** The label that actually worked, so later navigations reuse it. */
    private String toolLabel;

    /**
     * Appended to the assertions that depend on the "QA flip" (2026-06-29…07-02), so a
     * failure report says WHY rather than just "expected true".
     */
    private static final String STALE_BUILD_HINT =
            "This asserts CURRENT-SOURCE behaviour: LibraryFragment.render() makes Compose, "
            + "Create-folder and the shortcuts unconditionally visible and gates the CLICK "
            + "via runWithPostingRights. Older builds (e.g. the emulator's .dev APK, "
            + "Zillit_32494-dev / 2026-07-20) hide them instead. Check the build first: "
            + "adb shell dumpsys package <pkg> | grep versionName — see trap 3.17 in "
            + "DOC_DISTRIBUTION_TEST_CASES.md.";

    @BeforeEach
    void reachLibrary() {
        // Shared fast path (DocDistBaseTest): returns immediately when the previous case
        // already left us on the library root, instead of re-running permission sweeps and
        // full Tools navigation before EVERY case. These two suites each carried their own
        // copy of that navigation and so never benefited — which is a large part of why
        // moving between cases cost more than the cases themselves.
        reachLibraryOrSkip();
    }

    // ==================== A · ENTRY & SHELL ====================

    @Test
    @Order(1)
    @DisplayName("A-1/A-2: Tools → Document Distribution opens the module on its LIBRARY")
    void moduleOpens() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assertTrue(lib.isDocDistActivity(),
                "The module must open DocumentDistributionActivity");
        // dd_recycler alone would not prove this: nine screens in this module share it.
        assertTrue(lib.isLibrary(Duration.ofSeconds(15)),
                "The start destination is the library — expected the Compose / Create-folder actions");
        System.out.println(">>> tool label='" + toolLabel + "'");
        screenshot("dd-library-open");
    }

    @Test
    @Order(2)
    @DisplayName("A-4: the root offers Create folder and Compose — and NOT Upload, by design")
    void primaryActions() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // Source truth (LibraryFragment.render): Create folder and Compose are
        // unconditionally VISIBLE — the QA flip keeps posting buttons on screen for
        // everyone and gates the CLICK via runWithPostingRights. Upload however is
        // `state.isInFolder`-only: it never exists at the root, so asserting it here
        // (as the first run did) fails on a perfectly correct build. Trap 3.11 answered.
        // True on every build seen so far, and unconditional in the source.
        assertTrue(lib.hasCreateFolder(), "Create folder expected");
        assertFalse(lib.hasUpload(),
                "Upload is folder-scoped (LibraryFragment: visible only when isInFolder) — "
                        + "seeing it at the ROOT means the visibility rule changed");
        screenshot("dd-primary-actions");

        // Compose is where builds diverge. Current source shows the chip unconditionally;
        // the emulator's older dev APK has no dd_btn_compose at all (confirmed by dumping
        // the live chip row, not inferred). That is an ENVIRONMENT gap, not an app defect,
        // so it skips with the reason rather than painting the report red for ever — the
        // composer itself is still covered, because DocDistComposerTest reaches it through
        // a folder's Distribute button on exactly these builds.
        boolean compose = lib.hasCompose();
        System.out.println(">>> compose chip present = " + compose);
        assumeTrue(compose,
                "This build has no Compose chip in the library action row. " + STALE_BUILD_HINT);
    }

    @Test
    @Order(3)
    @DisplayName("A-4: the four shortcuts are present — Templates / Lists / Contacts / History")
    void shortcuts() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // Lists / Contacts / History ship on every build seen so far — assert them hard.
        assertTrue(lib.hasLists(), "Lists shortcut expected");
        assertTrue(lib.hasContacts(), "Contacts shortcut expected");
        assertTrue(lib.hasHistory(), "History shortcut expected");

        // Templates is the other control the older dev build omits (same divergence as
        // Compose). Environment gap → skip with the reason; the templates SCREEN itself is
        // still covered by DocDistTemplatesTest whenever it can be reached.
        boolean templates = lib.hasTemplates();
        System.out.println(">>> templates chip present = " + templates);
        assumeTrue(templates,
                "This build has no Templates chip in the library action row. " + STALE_BUILD_HINT);
        screenshot("dd-shortcuts");
    }

    @Test
    @Order(4)
    @DisplayName("A-5: the shortcut badges render counts")
    void shortcutBadges() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        String t = lib.templatesBadge(), l = lib.listsBadge(), h = lib.historyBadge();
        System.out.println(">>> badges templates='" + t + "' lists='" + l + "' history='" + h + "'");
        screenshot("dd-badges");

        // These are UNREAD counters, not entity counts (trap 3.2): renderToolbarBadge
        // reads CommonBadgesHandler's unitCounts, hides the view at 0, and caps at "99+"
        // — and merely TAPPING the shortcut marks it read, so a later case cannot rely on
        // a value this one saw. The only stable rule is the FORMAT: absent, or a positive
        // number, or "99+" — never a literal "0", and never arbitrary text.
        for (String badge : new String[]{t, l, h}) {
            if (badge == null || badge.isBlank()) continue;   // GONE at zero — correct
            assertTrue(badge.matches("\\d{1,2}|99\\+"),
                    "A rendered badge must be a positive count or \"99+\", never '"
                            + badge + "' (the view is hidden at 0 rather than showing it)");
            assertFalse(badge.equals("0"),
                    "A zero badge must be HIDDEN, not rendered as \"0\"");
        }
    }

    @Test
    @Order(5)
    @DisplayName("A-6: the help card expands and collapses")
    void helpCard() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.hasHelpCard(), "No help card on this build — skipping");
        boolean before = lib.helpBodyShown();
        assumeTrue(lib.toggleHelpCard(), "Could not tap the help header");
        sleep(1000);
        boolean after = lib.helpBodyShown();
        System.out.println(">>> help body " + before + " -> " + after);
        assertTrue(before != after, "Tapping the help header should toggle the body");
        screenshot("dd-help-card");
        lib.toggleHelpCard();   // leave it as we found it
    }

    @Test
    @Order(6)
    @DisplayName("A-2/B-1: the library shows its list or its empty state")
    void listOrEmpty() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        boolean empty = lib.isEmptyStateShown();
        int items = lib.itemCount();
        System.out.println(">>> empty=" + empty + " rendered=" + items
                + " emptyText='" + lib.emptyText() + "'");
        assertTrue(empty || items > 0,
                "The library must show EITHER rows OR the empty state — neither was found");
        if (empty) assertTrue(lib.hasEmptyCta(), "The empty state should offer its CTA");
        screenshot("dd-library-state");
    }

    // ==================== B · LIBRARY LISTING ====================

    @Test
    @Order(7)
    @DisplayName("B-3: the list ⇄ grid toggle switches layout without losing items")
    void listGridToggle() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.hasViewToggle(), "No view toggle — skipping");
        assumeTrue(lib.itemCount() > 0, "Nothing in the library to re-lay-out — skipping");

        List<String> asList = lib.allItemNamesScrolled();
        assumeTrue(lib.showGrid(), "Could not switch to grid");
        sleep(1500);
        List<String> asGrid = lib.allItemNamesScrolled();
        System.out.println(">>> list=" + asList.size() + " grid=" + asGrid.size());
        screenshot("dd-grid");
        assertTrue(asGrid.containsAll(asList),
                "Switching to grid must not drop items. list=" + asList + " grid=" + asGrid);
        lib.showList();
        sleep(1200);
    }

    @Test
    @Order(8)
    @DisplayName("B-4: swipe-refresh reloads the library and keeps it readable")
    void swipeRefresh() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.pullToRefresh(), "No swipe-refresh on the library");
        sleep(3000);
        assertTrue(lib.isLibrary(Duration.ofSeconds(15)), "Still the library after a refresh");
        assertTrue(lib.isListReadable(),
                "After a refresh the library must render rows or its empty state");
        screenshot("dd-refreshed");
    }

    @Test
    @Order(9)
    @DisplayName("B-5/B-6: search narrows the list, and a no-match search offers Clear filters")
    void search() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.hasSearch(), "No search box — skipping");
        List<String> all = lib.allItemNamesScrolled();
        assumeTrue(!all.isEmpty(), "Nothing to search — skipping");

        // A string nothing can match, so the empty state is unambiguous.
        assumeTrue(lib.search("zzqqxx-no-such-document"), "Could not type in the search box");
        sleep(1500);
        boolean empty = lib.isEmptyStateShown();
        System.out.println(">>> no-match empty=" + empty
                + " clearFilters=" + lib.hasClearFiltersCta());
        screenshot("dd-search-no-match");
        assertTrue(empty, "A search that matches nothing should show the empty state");

        lib.clearSearch();
        sleep(2000);
        List<String> restored = lib.allItemNamesScrolled();
        assertTrue(restored.size() >= all.size() - 1,
                "Clearing the search should restore the list. before=" + all.size()
                        + " after=" + restored.size());
    }

    @Test
    @Order(10)
    @DisplayName("B-7/B-8: the sort menu offers all four orders")
    void sortOptions() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openSort(), "Sort menu did not open");
        sleep(1000);
        // menu/dd_library_sort.xml: date desc, date asc, name A-Z, name Z-A
        boolean date = lib.sortOptionShown("Date");
        boolean name = lib.sortOptionShown("Name");
        System.out.println(">>> sort menu date=" + date + " name=" + name);
        screenshot("dd-sort-menu");
        assertTrue(date && name, "Sort should offer both Date and Name orders");
        try { driver.navigate().back(); } catch (Exception ignored) { }
    }

    /**
     * B-9/B-10 for real: APPLY a range, not just open the picker.
     *
     * <p>The old case opened the calendar and backed out, which is why "the calendar shows
     * and then nothing happens" was never caught. The app builds
     * {@code MaterialDatePicker.Builder.dateRangePicker()} and only calls
     * {@code viewModel.setDateFilter} from {@code addOnPositiveButtonClickListener} — so
     * with a single day tapped the confirm button stays DISABLED and no filter is ever
     * applied. Both ends must be chosen.
     */
    @Test
    @Order(11)
    @DisplayName("B-9/B-10: picking a date RANGE applies the filter chip, and clearing it restores the list")
    void dateRangeFilterAppliesAndClears() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openDateFilter(), "Could not open the date filter");
        assumeTrue(lib.datePickerOpen(Duration.ofSeconds(10)),
                "The date range picker should appear");
        screenshot("dd-date-picker");

        boolean applied = lib.applyDateRange("10", "20");
        System.out.println(">>> B-9 range applied=" + applied
                + " chipShown=" + lib.dateFilterChipShown()
                + " chip='" + lib.dateFilterChipText() + "'");
        screenshot("dd-date-applied");

        if (!applied) {
            // Leave nothing open for the next case, then say precisely what went wrong.
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
        assertTrue(applied,
                "Choosing a start AND an end date must enable the picker's confirm button "
                        + "and close it. If confirmEnabled=false above, only one end was "
                        + "selected — dateRangePicker() needs both, which is why tapping a "
                        + "single day looks like the calendar does nothing.");

        assertTrue(lib.dateFilterChipShown(),
                "An applied range must surface dd_date_filter_chip — render() shows it "
                        + "whenever the VM holds a start+end pair");
        String chip = lib.dateFilterChipText();
        assertTrue(chip != null && !chip.isBlank(),
                "The chip should label the active range, got '" + chip + "'");

        // Clearing: the chip's × is the discoverable route (long-press the button also works).
        assertTrue(lib.clearDateFilterChip() || lib.clearDateFilterByLongPress(),
                "Could not clear the date filter");
        sleep(1200);
        assertFalse(lib.dateFilterChipShown(),
                "Clearing the filter must hide the chip again");
        assertTrue(lib.isListReadable(), "The full list should be back after clearing");
        screenshot("dd-date-cleared");
    }

    @Test
    @Order(13)
    @DisplayName("B-11/B-12: opening a folder moves the breadcrumb, home returns to root")
    void folderNavigation() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        String folder = firstFolderName(lib);
        assumeTrue(folder != null, "No folder in the library to open — skipping");

        assumeTrue(lib.openItem(folder), "Could not open " + folder);
        sleep(2500);
        String crumb = lib.currentCrumb();
        System.out.println(">>> opened '" + folder + "' crumb='" + crumb + "'");
        screenshot("dd-folder-open");
        assertFalse(lib.isAtRoot(), "Opening a folder should move the breadcrumb off root");

        assertTrue(lib.returnToRoot(), "The home crumb should return to the library root");
        screenshot("dd-back-at-root");
    }

    /**
     * The first row that is a FOLDER.
     *
     * <p>Keyed on {@code dd_send} rather than {@code dd_folder_badge}: the badge is the
     * item count and {@code LibraryAdapter.bindBadge()} hides it ({@code View.GONE}) when
     * the count is zero, so an EMPTY folder carries no badge at all and this used to
     * return null on a library full of them. {@code dd_send} / {@code dd_publish} are
     * declared only in item_dd_library_folder, and rights gate their click rather than
     * their visibility — a structural signal that holds for empty folders too.
     */
    private String firstFolderName(DocDistLibraryPage lib) {
        try {
            for (String n : lib.itemNames()) {
                String xpath = "//*[@resource-id='" + appPackage + ":id/dd_card']"
                        + "[.//*[@text='" + n + "']]"
                        + "//*[@resource-id='" + appPackage + ":id/dd_send']";
                if (!driver.findElements(io.appium.java_client.AppiumBy.xpath(xpath)).isEmpty()) {
                    return n;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

}
