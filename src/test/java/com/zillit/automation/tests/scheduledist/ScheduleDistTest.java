package com.zillit.automation.tests.scheduledist;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage.Folder;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage.Row;
import com.zillit.automation.pages.scheduledist.ScheduleUploadPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SCHEDULE DISTRIBUTION — the schedule handed out as PDFs: Schedule Full, Schedule One Line,
 * and schedule pages filed by scene. Cases and ids follow {@code SCHEDULE_DIST_MODULE_TESTCASES.md}.
 *
 * <p><b>Full and One Line can never be deleted from the app</b> (no Delete on those rows —
 * finding 1), so this suite uploads ONE sentinel row per tab the first time it meets a project
 * and Replaces it on every run after. The sentinel is recognised by its episode,
 * {@link #SENTINEL_EPISODE} — rows carry no name ({@code schedule_name} is always null), and on
 * a TV project the episode is shown on every row. On a project that is not TV there is no way
 * to tell our row from a real one, so the Full / One Line upload cases skip there.
 *
 * <p>Pages ARE deletable: they are made fresh each run under scene {@code 99ZLAUTO<run>} and
 * swept by SD-99.
 *
 * <p>Publishing to Document Distribution writes into another module's library, so it is
 * opt-in: {@code -Dzillit.schedule.publish=true}. Without it the publish case stops at Cancel.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.SCHEDULE_DIST)
class ScheduleDistTest extends ScheduleDistBaseTest {

    static final String SENTINEL_EPISODE = "9900";
    static final String SCENE_MARK = "ZLAUTO";
    /** SD-25b's opt-in undated One Line row (web gap SCH-01). */
    static final String UNDATED_EPISODE = "9901";
    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);

    private final String scene = "99" + SCENE_MARK + RUN;
    /** SD-46's own scene: one page type only, so finding 4 cannot hide any of its pages. */
    private final String tallScene = "98" + SCENE_MARK + RUN;
    private final String page1 = "ZL_AUTO_" + RUN + "_1";
    private final String page2 = "ZL_AUTO_" + RUN + "_2";

    private Boolean tvProject;
    private boolean fullSentinel, oneLineSentinel, pageMade;

    private static final Predicate<Row> SENTINEL = r -> ("Episode: " + SENTINEL_EPISODE).equals(r.episode);

    // ═══════════════════════════ 1. shell · SD-0 ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("SD-01 The tool opens on Schedule Full, with three tabs and History")
    void moduleOpens() {
        openSchedule();
        ScheduleDistPage page = new ScheduleDistPage(driver);
        assertEquals(List.of(ScheduleDistPage.TAB_FULL, ScheduleDistPage.TAB_PAGES, ScheduleDistPage.TAB_ONE_LINE),
                page.tabNames(), "Tabs");
        assertTrue(page.isSelected(ScheduleDistPage.TAB_FULL), "Schedule Full is not selected on opening");
        assertTrue(page.historyShown(), "No History button on Schedule Full");
    }

    @Test
    @Order(20)
    @DisplayName("SD-02 Each tab shows its own upload button")
    void tabsHaveOwnUpload() {
        openSchedule();
        ScheduleDistPage page = new ScheduleDistPage(driver);
        assumeTrue(page.uploadShown(), "No upload button: this user has neither posting access nor admin");
        try {
            assertEquals(ScheduleDistPage.UPLOAD_FULL, page.uploadLabel(), "Schedule Full upload label");
            assumeTrue(page.openTab(ScheduleDistPage.TAB_PAGES), "Pages tab could not be tapped");
            assertEquals(ScheduleDistPage.UPLOAD_PAGE, page.uploadLabel(), "Pages upload label");
            assumeTrue(page.openTab(ScheduleDistPage.TAB_ONE_LINE), "One Line tab could not be tapped");
            assertEquals(ScheduleDistPage.UPLOAD_ONE_LINE, page.uploadLabel(), "One Line upload label");
        } finally {
            page.openTab(ScheduleDistPage.TAB_FULL);
        }
    }

    @Test
    @Order(40)
    @DisplayName("SD-04 An empty tab shows the empty state")
    void emptyState() {
        // Full and One Line hold the suite's permanent sentinel after the first run, so the tab
        // most likely to be empty is Pages — every page the suite makes is swept by SD-99.
        openSchedule();
        ScheduleDistPage page = new ScheduleDistPage(driver);
        page.openTab(ScheduleDistPage.TAB_PAGES);
        // The folder list renders from a cache first and then from the server, so "no rows yet"
        // is not "empty". Wait until the tab shows either folders or its empty state.
        boolean pagesEmpty = true, pagesEmptyView = false;
        for (int i = 0; i < 10; i++) {
            pagesEmpty = page.visibleFolders().isEmpty();
            pagesEmptyView = page.emptyShown();
            if (!pagesEmpty || pagesEmptyView) break;
            sleep(1500);
        }
        if (pagesEmpty && !pagesEmptyView) screenshot("SD-04_pages_blank");
        page.openTab(ScheduleDistPage.TAB_ONE_LINE);
        boolean oneEmpty = page.allRows().isEmpty();
        boolean oneEmptyView = page.emptyShown();
        page.openTab(ScheduleDistPage.TAB_FULL);
        assumeTrue(pagesEmpty || oneEmpty, "Every tab already has rows on this project — nothing to check");
        if (pagesEmpty) assertTrue(pagesEmptyView, "Pages lists nothing and shows no empty state");
        if (oneEmpty) assertTrue(oneEmptyView, "Schedule One Line lists nothing and shows no empty state");
    }

    @Test
    @Order(50)
    @DisplayName("SD-05 A swipe does not change the tab")
    void swipeDoesNothing() {
        openSchedule();
        ScheduleDistPage page = new ScheduleDistPage(driver);
        page.openTab(ScheduleDistPage.TAB_FULL);
        page.swipeLeft();
        assertTrue(page.isSelected(ScheduleDistPage.TAB_FULL), "A swipe moved off Schedule Full — swiping is"
                + " meant to be disabled, and every tab case assumes it");
    }

    // ═══════════════════════════ 2. Schedule Full · SD-1 ═══════════════════════════

    @Test
    @Order(100)
    @DisplayName("SD-10 Upload offers a single source, Document")
    void pickerOffersDocument() {
        ScheduleUploadPage up = startUpload(ScheduleDistPage.TAB_FULL);
        try {
            assertEquals(ScheduleUploadPage.PICKER_TITLE, up.pickerTitle(), "Picker title");
            assertEquals(List.of("Document"), up.pickerOptions(), "Picker options");
        } finally {
            driver.navigate().back();
            sleep(1000);
        }
    }

    @Test
    @Order(110)
    @DisplayName("SD-11 The Full upload form: a date, an episode on TV, nothing else")
    void fullFormShape() {
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_FULL);
        try {
            tvProject = up.fieldShown(ScheduleUploadPage.EPISODE);
            assertNotNull(up.dateText(), "No date row on the form");
            assertFalse(up.fieldShown(ScheduleUploadPage.SCENE), "A scene field is offered for a Full schedule");
            assertFalse(up.fieldShown(ScheduleUploadPage.TYPE), "A schedule-type field is offered for a Full schedule");
            assertFalse(up.fieldShown(ScheduleUploadPage.COLOUR), "A colour field is offered for a Full schedule");
            assertEquals("Submit", up.submitLabel(), "Submit label");
        } finally {
            up.leaveForm();
            backToTabs();
        }
    }

    @Test
    @Order(120)
    @DisplayName("SD-12 On a TV project, a blank episode is refused")
    void fullEpisodeRequired() {
        assumeTrue(!Boolean.FALSE.equals(tvProject), "Not a TV project: there is no episode field");
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_FULL);
        try {
            assumeTrue(up.fieldShown(ScheduleUploadPage.EPISODE), "Not a TV project: there is no episode field");
            up.setField(ScheduleUploadPage.EPISODE, "");
            up.submit();
            sleep(2000);
            assertTrue(up.formShown(), "A Full schedule was uploaded with no episode on a TV project");
        } finally {
            if (up.formShown()) up.leaveForm();
            backToTabs();
        }
    }

    @Test
    @Order(130)
    @DisplayName("SD-13 Our sentinel Full schedule is on the list (uploaded once, replaced after)")
    void fullSentinelUploaded() {
        fullSentinel = ensureSentinel(ScheduleDistPage.TAB_FULL);
        assertTrue(fullSentinel, "No Schedule Full row carries episode " + SENTINEL_EPISODE + " after the upload");
    }

    @Test
    @Order(140)
    @DisplayName("SD-14 A Full row names its uploader, its schedule date and when it was uploaded")
    void fullRowShape() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        Row r = sentinelRow(ScheduleDistPage.TAB_FULL);
        assertNotNull(r, "The sentinel row is gone");
        assertTrue(r.createdBy != null && r.createdBy.startsWith("Uploaded by : "), "Uploader line: " + r);
        assertTrue(r.name != null && r.name.startsWith("Schedule date :"), "Date line: " + r);
        assertTrue(r.date.startsWith("Uploaded On :"), "Uploaded line: " + r);
    }

    @Test
    @Order(150)
    @DisplayName("SD-15 A Full row's menu has no Delete (finding 1)")
    void fullMenu() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        List<String> items = sentinelMenu(ScheduleDistPage.TAB_FULL);
        new ScheduleDistPage(driver).closeMenu();
        assertTrue(items.containsAll(List.of("View", "Download", "Publish to Doc Distribution")), "Menu: " + items);
        assertFalse(items.contains("Delete"), "A Full row now offers Delete: " + items
                + " — update finding 1 and this case");
    }

    @Test
    @Order(160)
    @DisplayName("SD-16 [QA BUG-12] Replace leaves exactly one current version")
    void fullReplace() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        assertTrue(replaceSentinel(ScheduleDistPage.TAB_FULL), "Replace did not finish");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        List<Row> mine = waitForRows(page, SENTINEL, 1);
        assertEquals(1, mine.size(), "After Replace the sentinel shows " + mine.size() + " times: " + mine
                + " — the old version should move to History, never stay beside the new one or vanish with it");
    }

    @Test
    @Order(170)
    @DisplayName("SD-17 View hands the PDF to another app")
    void fullView() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        assumeTrue(page.tapRow(SENTINEL), "The sentinel row could not be tapped");
        String pkg = waitForForeignApp(Duration.ofSeconds(40));
        bringAppToFront();
        assertNotNull(pkg, "Tapping a row did not open the PDF in another app (it uses ACTION_VIEW)");
    }

    @Test
    @Order(180)
    @DisplayName("SD-18 Download saves the file, or offers to request the right")
    void fullDownload() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        assumeTrue(page.openRowMenu(SENTINEL) != null && page.tapMenuItem("Download"), "Download could not be reached");
        if (page.dialogShown()) {
            String msg = page.dialogMessage();
            page.tapDialogLeft();   // Cancel — Yes messages the admins
            assertTrue(msg != null && msg.contains("Downloading Rights"), "Unexpected dialog: " + msg);
            return;
        }
        assertTrue(page.messageShown("File Downloaded Successfully.", Duration.ofSeconds(45)),
                "Download ended with neither \"File Downloaded Successfully.\" nor the rights Alert");
    }

    @Test
    @Order(190)
    @DisplayName("SD-19 View Count opens the count screen (admin)")
    void fullViewCount() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        List<String> items = page.openRowMenu(SENTINEL);
        assumeTrue(items != null && items.contains("View Count"), "View Count is not offered (admin only): " + items);
        page.tapMenuItem("View Count");
        sleep(2500);
        try {
            assertTrue(activityIs("UsersCountListPage"), "View Count did not open the count screen: " + whereAreWe());
            assertTrue("View Count".equalsIgnoreCase(String.valueOf(page.header())), "Count title: " + page.header());
        } finally {
            backToTabs();
        }
    }

    // ═══════════════════════════ 3. Schedule One Line · SD-2 ═══════════════════════════

    @Test
    @Order(200)
    @DisplayName("SD-20 Our sentinel One Line schedule is on the list, dated as One Line")
    void oneLineSentinel() {
        oneLineSentinel = ensureSentinel(ScheduleDistPage.TAB_ONE_LINE);
        assertTrue(oneLineSentinel, "No One Line row carries episode " + SENTINEL_EPISODE);
        Row r = sentinelRow(ScheduleDistPage.TAB_ONE_LINE);
        assertTrue(r != null && r.name != null && r.name.startsWith("One Line date :"), "Date line: " + r);
    }

    @Test
    @Order(210)
    @DisplayName("SD-21 A One Line row's menu has no Delete (finding 1)")
    void oneLineMenu() {
        assumeTrue(oneLineSentinel, "SD-20 left no sentinel row");
        List<String> items = sentinelMenu(ScheduleDistPage.TAB_ONE_LINE);
        new ScheduleDistPage(driver).closeMenu();
        assertFalse(items.contains("Delete"), "A One Line row now offers Delete: " + items);
    }

    @Test
    @Order(220)
    @DisplayName("SD-22 [QA BUG-12] One Line Replace leaves exactly one current version")
    void oneLineReplace() {
        assumeTrue(oneLineSentinel, "SD-20 left no sentinel row");
        assertTrue(replaceSentinel(ScheduleDistPage.TAB_ONE_LINE), "Replace did not finish");
        List<Row> mine = waitForRows(tab(ScheduleDistPage.TAB_ONE_LINE), SENTINEL, 1);
        assertEquals(1, mine.size(), "After Replace the One Line sentinel shows " + mine.size() + " times: " + mine);
    }

    // ── web gap SCH-01: an undated One Line does not inherit an earlier upload's date ──

    @Test
    @Order(225)
    @DisplayName("SD-25 [WEB SCH-01] A new One Line upload starts with no date, after a dated upload and Replace")
    void undatedOneLineFormStartsEmpty() {
        // [WEB SCH-01] SD-13/SD-16/SD-22 have just uploaded and replaced dated rows in this
        // session. A FRESH upload must not start with any of those dates: on Android the date
        // is an instance field of UploadDocumentActivity, seeded only on Replace.
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_ONE_LINE);
        String date;
        try {
            date = up.dateText();
        } finally {
            up.leaveForm();
            backToTabs();
        }
        assertTrue(date == null || !date.matches(".*\\d.*"),
                "[SCH-01] A fresh One Line upload form already carries a date (\"" + date + "\") — it"
                        + " inherited an earlier upload's date; it should start empty (\"Set Date (Optional)\")");
    }

    @Test
    @Order(226)
    @DisplayName("SD-25b [WEB SCH-01] An undated One Line row shows its date label with no date (opt-in)")
    void undatedOneLineRowHasNoDate() {
        // Full / One Line rows can never be deleted, so this upload is opt-in for a throw-away
        // project: -Dzillit.schedule.undated=true. Episode 9901 marks the row (TV projects only).
        assumeTrue(Boolean.getBoolean("zillit.schedule.undated"),
                "Opt-in: run with -Dzillit.schedule.undated=true — it leaves an undeletable One Line row");
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_ONE_LINE);
        boolean closed;
        try {
            assumeTrue(up.fieldShown(ScheduleUploadPage.EPISODE), "Not a TV project: the undated row could not be"
                    + " told apart from a real schedule");
            up.setField(ScheduleUploadPage.EPISODE, UNDATED_EPISODE);
            up.submit();
            closed = up.waitFormClosed(UPLOAD_WAIT);
        } finally {
            if (up.formShown()) up.leaveForm();
            backToTabs();
        }
        assumeTrue(closed, "The undated upload did not finish");
        Predicate<Row> undated = r -> ("Episode: " + UNDATED_EPISODE).equals(r.episode);
        List<Row> rows = waitForRows(tab(ScheduleDistPage.TAB_ONE_LINE), undated, 1);
        assumeTrue(!rows.isEmpty(), "The undated row never appeared");
        Row newest = rows.get(0);
        assertTrue(newest.name != null && newest.name.matches("^One Line date :\\s*$"),
                "[SCH-01] An undated One Line upload shows \"" + newest.name + "\" — expected the label with no date");
    }

    @Test
    @Order(230)
    @DisplayName("SD-23 Pull to refresh clears its spinner and keeps the list (finding 6)")
    void oneLinePull() {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_ONE_LINE);
        List<Row> before = page.visibleRows();
        page.pullToRefresh();
        assertTrue(page.waitLoaded(Duration.ofSeconds(15)), "A loader was still showing after the pull");
        assertEquals(before.size(), page.visibleRows().size(), "The pull changed the list");
    }

    @Test
    @Order(240)
    @DisplayName("SD-24 The tabs keep their own data")
    void tabsSeparate() {
        assumeTrue(fullSentinel && oneLineSentinel, "Needs both sentinels");
        Row full = sentinelRow(ScheduleDistPage.TAB_FULL);
        Row one = sentinelRow(ScheduleDistPage.TAB_ONE_LINE);
        assertTrue(full != null && full.name.startsWith("Schedule date"), "Full row: " + full);
        assertTrue(one != null && one.name.startsWith("One Line date"), "One Line row: " + one);
    }

    // ═══════════════════════════ 4. Pages — folders · SD-3 ═══════════════════════════

    @Test
    @Order(300)
    @DisplayName("SD-30 The page upload form: scene, colour, page date, type, page number")
    void pageFormShape() {
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_PAGES);
        try {
            assertTrue(up.fieldShown(ScheduleUploadPage.SCENE), "No Scene No. field");
            assertTrue(up.fieldShown(ScheduleUploadPage.COLOUR), "No colour field");
            assertTrue(up.fieldShown(ScheduleUploadPage.TYPE), "No schedule-type field");
            assertTrue(up.fieldShown(ScheduleUploadPage.PAGE_NO), "No page-number field");
            assertNotNull(up.dateText(), "No page-date row");
        } finally {
            up.leaveForm();
            backToTabs();
        }
    }

    @Test
    @Order(310)
    @DisplayName("SD-31 A blank scene number is refused")
    void pageBlankScene() {
        assertRefused(up -> {
            up.setField(ScheduleUploadPage.SCENE, "");
            up.chooseDropdown(ScheduleUploadPage.TYPE, ScheduleUploadPage.TYPE_FULL_PAGES);
        }, "A page was uploaded with no scene number");
    }

    @Test
    @Order(320)
    @DisplayName("SD-32 A scene number starting with a letter is refused")
    void pageLetterScene() {
        assertRefused(up -> {
            up.setField(ScheduleUploadPage.SCENE, "A" + RUN);
            up.chooseDropdown(ScheduleUploadPage.TYPE, ScheduleUploadPage.TYPE_FULL_PAGES);
        }, "A scene number starting with a letter was accepted");
    }

    @Test
    @Order(330)
    @DisplayName("SD-33 A page with no schedule type is refused")
    void pageNoType() {
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_PAGES);
        try {
            up.setField(ScheduleUploadPage.SCENE, scene);
            if (up.fieldShown(ScheduleUploadPage.EPISODE)) up.setField(ScheduleUploadPage.EPISODE, "1");
            up.submit();
            boolean told = up.messageShown("Please select if these pages are full schedule or one line schedule",
                    Duration.ofSeconds(5));
            assertTrue(up.formShown(), "A page was uploaded with no schedule type");
            assertTrue(told, "Refused, but without saying why");
        } finally {
            if (up.formShown()) up.leaveForm();
            backToTabs();
        }
    }

    @Test
    @Order(340)
    @DisplayName("SD-34 The colour and type dropdowns offer the expected options")
    void pageDropdowns() {
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_PAGES);
        try {
            List<String> colours = up.openDropdown(ScheduleUploadPage.COLOUR);
            up.closeDropdown();
            assertEquals(List.of("WHITE", "BLUE", "PINK", "YELLOW", "GREEN", "GOLDENROD", "BUFF", "SALMON",
                    "CHERRY", "TAN", "IVORY"), colours, "Colour options");
            List<String> types = up.openDropdown(ScheduleUploadPage.TYPE);
            up.closeDropdown();
            assertEquals(List.of(ScheduleUploadPage.TYPE_FULL_PAGES, ScheduleUploadPage.TYPE_ONE_LINE_PAGES), types,
                    "Type options");
        } finally {
            if (up.formShown()) up.leaveForm();
            backToTabs();
        }
    }

    @Test
    @Order(350)
    @DisplayName("SD-35 Uploading a page files it in a folder for its scene")
    void pageUpload() {
        boolean closed = uploadPage(page1, "BLUE", ScheduleUploadPage.TYPE_FULL_PAGES);
        assertTrue(closed, "The page upload form did not finish");
        Folder f = waitForFolder("Scene No. " + scene, Duration.ofSeconds(90));
        assertNotNull(f, "No folder \"Scene No. " + scene + "\" on the Pages tab after the upload");
        pageMade = true;
        assertTrue(f.subTitle != null && f.subTitle.startsWith("Schedule Type :"), "Folder type line: " + f);
    }

    @Test
    @Order(360)
    @DisplayName("SD-36 Search by scene number finds the folder")
    void pageSearchScene() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        try {
            assertEquals("Sn No.", page.filterMode(), "Filter mode on opening");
            page.searchPages(scene);
            List<Folder> found = page.settledFolders();
            assertTrue(found.stream().anyMatch(f -> ("Scene No. " + scene).equals(f.title)), "Search by scene: " + found);
        } finally {
            page.searchPages("");
        }
    }

    @Test
    @Order(365)
    @DisplayName("SD-36b [WEB SCH-02] Scene search ignores case and survives fast typing")
    void pageSearchCaseAndSpeed() {
        // [WEB SCH-02] The search is server-side behind .debounce(700).onEach{} — onEach does not
        // cancel an earlier request, so an older response can land last.
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        String want = "Scene No. " + scene;
        List<Folder> lower;
        List<String> raced = new java.util.ArrayList<>();
        try {
            page.searchPages(scene.toLowerCase());
            lower = page.settledFolders();
            for (int i = 1; i <= 3; i++) {
                page.searchPages("");
                page.settledFolders();
                boolean typed = page.typePagesSearchSlowly(scene, 80);
                sleep(3000);
                page.waitLoaded(Duration.ofSeconds(15));
                List<Folder> now = page.visibleFolders();
                boolean empty = page.emptyShown();
                if (empty || now.stream().noneMatch(f -> want.equals(f.title))) {
                    raced.add("try " + i + " (typed=" + typed + "): empty state=" + empty + ", folders=" + now);
                }
            }
        } finally {
            page.searchPages("");
        }
        List<Folder> lowerFinal = lower;
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertTrue(lowerFinal.stream().anyMatch(f -> want.equals(f.title)),
                        "[SCH-02] Searching the scene in lower case (\"" + scene.toLowerCase() + "\") did not find \""
                                + want + "\" — scene search is case-sensitive. Found: " + lowerFinal),
                () -> assertTrue(raced.isEmpty(),
                        "[SCH-02] Typing the scene quickly ended on the wrong result (an older response landed"
                                + " last): " + raced));
    }

    @Test
    @Order(370)
    @DisplayName("SD-37 Search by colour finds the folder, and another colour does not")
    void pageSearchColour() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        try {
            assumeTrue(page.chooseFilter("Search by color"), "The colour filter could not be chosen");
            assertEquals("Color", page.filterMode(), "Filter mode label");
            assumeTrue(page.searchByColour("BLUE"), "The colour popup did not offer BLUE");
            List<Folder> blue = page.settledFolders();
            assumeTrue(page.searchByColour("SALMON"), "The colour popup did not offer SALMON");
            List<Folder> salmon = page.settledFolders();
            String want = "Scene No. " + scene;
            assertTrue(blue.stream().anyMatch(f -> want.equals(f.title)), "BLUE did not find the BLUE page: " + blue);
            assertTrue(salmon.stream().noneMatch(f -> want.equals(f.title)), "SALMON found the BLUE page: " + salmon);
        } finally {
            page.chooseFilter("Search By Scene No.");
            page.searchPages("");
        }
    }

    @Test
    @Order(380)
    @DisplayName("SD-38 Episode search is offered on a TV project")
    void pageSearchEpisode() {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        List<String> modes = page.filterOptions();
        assumeTrue(Boolean.TRUE.equals(tvProject), "Not a TV project");
        assertTrue(modes.contains("Search By Episode No."), "Episode search is missing on a TV project: " + modes);
    }

    @Test
    @Order(390)
    @DisplayName("SD-39 A scene's Full and One Line pages are both reachable (finding 4)")
    void pageBothTypes() {
        assumeTrue(pageMade, "SD-35 made no page");
        assumeTrue(uploadPage(page2, "BLUE", ScheduleUploadPage.TYPE_ONE_LINE_PAGES), "The second page did not upload");
        sleep(5000);
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        page.searchPages(scene);
        List<Folder> folders = page.settledFolders();
        page.searchPages("");
        long forScene = folders.stream().filter(f -> ("Scene No. " + scene).equals(f.title)).count();
        if (forScene >= 2) return;   // two folders: both types reachable
        // One folder: are both pages inside it?
        assumeTrue(page.openFolder("Scene No. " + scene), "The scene's folder could not be opened");
        List<Row> rows = new ScheduleDistPage(driver).allRows();
        backToTabs();
        boolean both = rows.stream().anyMatch(r -> ("Page No. " + page1).equals(r.name))
                && rows.stream().anyMatch(r -> ("Page No. " + page2).equals(r.name));
        assertTrue(both, "Scene " + scene + " has a Full Schedule page and a One Line page, but the Pages tab shows"
                + " one folder holding " + rows + " — the other type is unreachable (finding 4)");
    }

    // ═══════════════════════════ 5. Pages — inside a folder · SD-4 ═══════════════════════════

    @Test
    @Order(400)
    @DisplayName("SD-40 A folder lists its pages under the title Pages")
    void folderOpens() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = openFolder();
        assertTrue("Pages".equalsIgnoreCase(String.valueOf(page.header())), "Folder title: " + page.header());
        List<Row> rows = page.allRows();
        assertTrue(rows.stream().anyMatch(r -> ("Scene No. " + scene).equals(r.scene) && ("Page No. " + page1).equals(r.name)),
                "Page " + page1 + " is not listed: " + rows);
    }

    @Test
    @Order(410)
    @DisplayName("SD-41 A folder offers no search, upload or History (finding 2)")
    void folderHasNoTools() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = openFolder();
        assertFalse(page.uploadShown(), "A folder now offers Upload — update finding 2");
        assertFalse(page.historyShown(), "A folder now offers History — update finding 2");
        assertTrue(page.pagesSearchHint() == null, "A folder now offers search — update finding 2");
    }

    @Test
    @Order(420)
    @DisplayName("SD-42 A page's menu offers Delete and no Replace")
    void pageMenu() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = openFolder();
        List<String> items = page.openRowMenu(r -> ("Page No. " + page1).equals(r.name));
        assumeTrue(items != null, "The page's ⋮ could not be tapped");
        page.closeMenu();
        assertTrue(items.contains("Delete"), "Delete is missing for an admin or the uploader: " + items);
        assertFalse(items.contains("Replace"), "A page offers Replace: " + items);
    }

    @Test
    @Order(430)
    @DisplayName("SD-43 Delete asks first, and No keeps the page")
    void pageDeleteAsks() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = openFolder();
        assumeTrue(page.openRowMenu(r -> ("Page No. " + page1).equals(r.name)) != null && page.tapMenuItem("Delete"),
                "Delete could not be reached");
        assertTrue(page.dialogShown(), "Delete did not ask first");
        assertEquals("Are you sure you want to delete?", page.dialogMessage(), "Confirm message");
        page.tapDialogLeft();
        assertTrue(page.allRows().stream().anyMatch(r -> ("Page No. " + page1).equals(r.name)), "No took the page anyway");
    }

    @Test
    @Order(440)
    @DisplayName("SD-44 Delete removes the page")
    void pageDelete() {
        assumeTrue(pageMade, "SD-35 made no page");
        ScheduleDistPage page = openFolder();
        assumeTrue(page.openRowMenu(r -> ("Page No. " + page1).equals(r.name)) != null && page.tapMenuItem("Delete")
                && page.dialogShown(), "Delete could not be reached");
        page.tapDialogRight();
        String snack = page.snackbarText(Duration.ofSeconds(10));
        boolean gone = false;
        for (int i = 0; i < 10 && !gone; i++) {
            sleep(3000);
            gone = page.allRows().stream().noneMatch(r -> ("Page No. " + page1).equals(r.name));
        }
        assertTrue(gone, "Page " + page1 + " is still listed after Delete (message: " + snack + ")");
        assertTrue(snack == null || !snack.matches("[a-z0-9_.]+"), "The delete message is an internal key: " + snack);
    }

    @Test
    @Order(450)
    @DisplayName("SD-45b [WEB SCH-04] Deleting a scene's last page takes its folder off the Pages tab")
    void lastPageDeleteRemovesFolder() {
        // [WEB SCH-04] Its own one-page scene. The Pages tab refreshes on resume only when a
        // socket event set isScheduleDistHasChange, so the folder may linger.
        String lone = "97" + SCENE_MARK + RUN;
        String title = "Scene No. " + lone;
        String pageNo = "ZL_AUTO_" + RUN + "_L";
        assumeTrue(uploadPage(lone, pageNo, "BLUE", ScheduleUploadPage.TYPE_FULL_PAGES), "The lone page did not upload");
        assumeTrue(waitForFolder(lone, title, Duration.ofSeconds(90)) != null, "No folder \"" + title + "\" after the upload");

        ScheduleDistPage inside = openFolder(lone);
        assumeTrue(inside.openRowMenu(r -> ("Page No. " + pageNo).equals(r.name)) != null
                && inside.tapMenuItem("Delete") && inside.dialogShown(), "Delete could not be reached");
        inside.tapDialogRight();
        String snack = inside.snackbarText(Duration.ofSeconds(10));
        boolean pageGone = false;
        for (int i = 0; i < 8 && !pageGone; i++) {
            sleep(2500);
            pageGone = new ScheduleDistPage(driver).visibleRows().stream().noneMatch(r -> ("Page No. " + pageNo).equals(r.name));
        }
        assumeTrue(pageGone, "The page itself did not leave its folder (message: " + snack + ") — SD-44's ground");

        // Back to Pages inside the tool, WITHOUT searching again: a new search refetches and
        // would hide the bug.
        stepBackToTabs();
        ScheduleDistPage pages = new ScheduleDistPage(driver);
        List<Folder> seen = List.of();
        boolean listed = true;
        for (int i = 0; i < 6 && listed; i++) {
            pages.waitLoaded(Duration.ofSeconds(10));
            seen = pages.visibleFolders();
            listed = seen.stream().anyMatch(f -> title.equals(f.title));
            if (listed) sleep(2500);
        }
        List<Row> stale = List.of();
        if (listed && pages.openFolder(title)) {
            ScheduleDistPage again = new ScheduleDistPage(driver);
            again.waitLoaded(Duration.ofSeconds(20));
            sleep(2000);
            stale = again.visibleRows();
            backToTabs();
        }
        tab(ScheduleDistPage.TAB_PAGES).searchPages("");
        List<Row> staleFinal = stale;
        assertTrue(staleFinal.stream().noneMatch(r -> ("Page No. " + pageNo).equals(r.name)),
                "[SCH-04] The deleted page is still listed when its lingering folder is reopened: " + staleFinal);
        assertFalse(listed, "[SCH-04] \"" + title + "\" is still on the Pages tab 15 s after its last page was deleted"
                + " (an empty scene row; opening it listed " + staleFinal + "). Folders shown: " + seen);
    }

    @Test
    @Order(460)
    @DisplayName("SD-46 Every page of a folder taller than the screen is read back")
    void tallFolder() {
        List<String> pages = List.of("ZL_AUTO_" + RUN + "_t1", "ZL_AUTO_" + RUN + "_t2",
                "ZL_AUTO_" + RUN + "_t3", "ZL_AUTO_" + RUN + "_t4");
        for (String p : pages) {
            assumeTrue(uploadPage(tallScene, p, "GREEN", ScheduleUploadPage.TYPE_FULL_PAGES), "Page " + p + " did not upload");
        }
        sleep(5000);
        ScheduleDistPage folder = openFolder(tallScene);
        List<Row> rows = folder.allRows();
        for (String p : pages) {
            assertTrue(rows.stream().anyMatch(r -> ("Page No. " + p).equals(r.name)),
                    "Page " + p + " was not read back from the folder. Read: " + rows);
        }
        List<String> menu = folder.openRowMenu(r -> ("Page No. " + pages.get(pages.size() - 1)).equals(r.name));
        folder.closeMenu();
        assertNotNull(menu, "The last page's ⋮ could not be reached by scrolling");
    }

    // ═══════════════════════ 6. Publish to Doc Distribution · SD-5 ═══════════════════════

    @Test
    @Order(500)
    @DisplayName("SD-50 Publish asks first, naming the destination; Cancel publishes nothing")
    void publishAsks() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        assumeTrue(page.openRowMenu(SENTINEL) != null && page.tapMenuItem("Publish to Doc Distribution"),
                "Publish could not be reached");
        if (page.messageShown("You don't have permission to distribute", Duration.ofSeconds(4))) {
            assumeTrue(false, "This user cannot post to Document Distribution");
        }
        try {
            assertTrue(page.textShown("Publish to Document Distribution", Duration.ofSeconds(20)), "No publish confirm");
            assertTrue(page.textShown("to the Document Distribution library?", Duration.ofSeconds(3)), "Confirm body");
        } finally {
            if (!Boolean.getBoolean("zillit.schedule.publish") && !page.tapDialogLeft()) {
                tapText("Cancel");
            }
        }
    }

    @Test
    @Order(510)
    @DisplayName("SD-51 Publish adds the file to Document Distribution (opt-in)")
    void publish() {
        assumeTrue(Boolean.getBoolean("zillit.schedule.publish"),
                "Opt-in: run with -Dzillit.schedule.publish=true — it writes into Document Distribution");
        tapText("Publish");
        assertTrue(new ScheduleDistPage(driver).textShown("Published.", Duration.ofSeconds(60)), "No \"Published.\"");
        tapText("Ok");
    }

    @Test
    @Order(520)
    @DisplayName("SD-52 View straight after a publish opens the PDF, not another publish (finding 5, opt-in)")
    void viewAfterPublish() {
        assumeTrue(Boolean.getBoolean("zillit.schedule.publish"),
                "Opt-in with SD-51: run with -Dzillit.schedule.publish=true");
        assumeTrue(fullSentinel, "SD-13 left no sentinel row");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        assumeTrue(page.tapRow(SENTINEL), "The sentinel row could not be tapped");
        boolean publishAgain = page.textShown("Publish to Document Distribution", Duration.ofSeconds(5));
        if (publishAgain) tapText("Cancel");
        String pkg = waitForForeignApp(Duration.ofSeconds(30));
        bringAppToFront();
        assertFalse(publishAgain, "Tapping a row after a publish raised the publish confirm again (finding 5)");
        assertNotNull(pkg, "Tapping a row after a publish did not open the PDF");
    }

    // ═══════════════════════════ 7. History · SD-6 ═══════════════════════════

    @Test
    @Order(600)
    @DisplayName("SD-60 Schedule Full History lists the replaced version")
    void fullHistory() {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        assumeTrue(page.tapHistory(), "History could not be tapped");
        sleep(3000);
        try {
            assertTrue("Schedule Full History".equalsIgnoreCase(String.valueOf(page.header())), "Title: " + page.header());
            assertEquals("Records of deleted messages", page.firstTextOf("headerMsg"), "Banner");
            if (fullSentinel) {
                assertTrue(waitForHistory(page, SENTINEL), "The replaced sentinel version is not in History");
            }
        } finally {
            backToTabs();
        }
    }

    @Test
    @Order(610)
    @DisplayName("SD-61 Schedule One Line History is its own")
    void oneLineHistory() {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_ONE_LINE);
        assumeTrue(page.tapHistory(), "History could not be tapped");
        sleep(3000);
        try {
            assertTrue("Schedule One Line History".equalsIgnoreCase(String.valueOf(page.header())), "Title: " + page.header());
        } finally {
            backToTabs();
        }
    }

    @Test
    @Order(620)
    @DisplayName("SD-62 Pages History opens on deleted folders")
    void pagesHistory() {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        assumeTrue(page.tapHistory(), "History could not be tapped");
        sleep(3000);
        try {
            assertTrue("Schedule Pages History".equalsIgnoreCase(String.valueOf(page.header())), "Title: " + page.header());
            assertEquals("Records of deleted Folders", page.firstTextOf("headerMsg"), "Banner");
        } finally {
            backToTabs();
        }
    }

    @Test
    @Order(630)
    @DisplayName("SD-63 History search filters the rows")
    void historySearch() {
        assumeTrue(fullSentinel, "SD-13 left no sentinel row, so History has nothing of ours to find");
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_FULL);
        assumeTrue(page.tapHistory(), "History could not be tapped");
        sleep(3000);
        try {
            assumeTrue(waitForHistory(page, SENTINEL), "No replaced sentinel version in History to search for");
            assumeTrue(page.searchHistory(SENTINEL_EPISODE), "History has no search box");
            List<Row> hit = page.visibleRows();
            page.searchHistory("ZLQXNOPE" + RUN);
            List<Row> miss = page.visibleRows();
            page.searchHistory("");
            assertTrue(!hit.isEmpty() && hit.stream().allMatch(SENTINEL), "Searching " + SENTINEL_EPISODE + " shows " + hit);
            assertTrue(miss.isEmpty(), "Searching for nothing still shows " + miss);
        } finally {
            backToTabs();
        }
    }

    @Test
    @Order(640)
    @DisplayName("SD-64 [QA BUG-04] Every History loads — a list or the empty state, never an error")
    void historiesLoad() {
        for (String t : List.of(ScheduleDistPage.TAB_FULL, ScheduleDistPage.TAB_ONE_LINE, ScheduleDistPage.TAB_PAGES)) {
            ScheduleDistPage page = tab(t);
            assumeTrue(page.tapHistory(), "History could not be tapped on " + t);
            sleep(4000);
            String snack = page.snackbarText(Duration.ofSeconds(3));
            boolean listed = !page.onScreenIds("rvChat").isEmpty() || !page.onScreenIds("recDocument").isEmpty();
            boolean empty = page.emptyShown();
            backToTabs();
            assertTrue(snack == null, t + " History reported an error: \"" + snack + "\" (QA BUG-04)");
            assertTrue(listed || empty, t + " History showed neither a list nor the empty state");
        }
    }

    // ═══════════════════════════ 8. cleanup · SD-99 ═══════════════════════════

    @Test
    @Order(990)
    @DisplayName("SD-99 No ZLAUTO page is left, and at most one sentinel per tab")
    void sweep() {
        // Several passes: with finding 4, a scene's One Line page hides behind its Full page's
        // folder and only surfaces once that page is deleted — a single pass left it behind.
        List<Folder> left = List.of();
        for (int pass = 0; pass < 3; pass++) {
            List<Folder> marked = markedFolders();
            if (marked.isEmpty()) { left = marked; break; }
            for (Folder f : marked) {
                ScheduleDistPage p = tab(ScheduleDistPage.TAB_PAGES);
                // Search by the folder's FULL scene number — a partial one matches nothing.
                p.searchPages(f.title.replaceFirst("^Scene No\\. ", ""));
                if (!p.openFolder(f.title)) { p.searchPages(""); continue; }
                for (int i = 0; i < 20; i++) {
                    List<Row> rows = p.visibleRows();
                    if (rows.isEmpty()) break;
                    Row first = rows.get(0);
                    if (p.openRowMenu(r -> r.toString().equals(first.toString())) == null) break;
                    if (!p.tapMenuItem("Delete") || !p.dialogShown()) { p.closeMenu(); break; }
                    p.tapDialogRight();
                    sleep(4000);
                }
                backToTabs();
            }
            sleep(5000);
            left = markedFolders();
        }
        assertTrue(left.isEmpty(), "Marked folders left behind: " + left);
        for (String t : List.of(ScheduleDistPage.TAB_FULL, ScheduleDistPage.TAB_ONE_LINE)) {
            int n = tab(t).rowsWith(SENTINEL).size();
            assertTrue(n <= 1, t + " holds " + n + " sentinel rows — the suite should only ever replace its one");
        }
    }

    /**
     * Every marked folder, read from the UNFILTERED list. Not via the search box: the app's
     * scene search does not match part of a scene number — searching "ZLAUTO" returned
     * nothing, and the first sweep "passed" with two marked folders still on screen.
     */
    private List<Folder> markedFolders() {
        ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
        page.searchPages("");
        page.settledFolders();
        List<Folder> all = page.allFolders();
        List<Folder> out = new java.util.ArrayList<>();
        for (Folder f : all) if (f.title.contains(SCENE_MARK)) out.add(f);
        return out;
    }

    // ═══════════════════════════ helpers ═══════════════════════════

    private void assertRefused(Consumer<ScheduleUploadPage> fill, String why) {
        ScheduleUploadPage up = openForm(ScheduleDistPage.TAB_PAGES);
        try {
            fill.accept(up);
            if (up.fieldShown(ScheduleUploadPage.EPISODE)) up.setField(ScheduleUploadPage.EPISODE, "1");
            up.submit();
            sleep(2500);
            assertTrue(up.formShown(), why);
        } finally {
            if (up.formShown()) up.leaveForm();
            backToTabs();
        }
    }

    /** Fills and submits a page upload; returns whether the form finished. */
    private boolean uploadPage(String pageNo, String colour, String type) {
        return uploadPage(scene, pageNo, colour, type);
    }

    /** Uploads the sentinel the first time; afterwards it is already there. */
    private boolean ensureSentinel(String tabName) {
        ScheduleDistPage page = tab(tabName);
        if (!page.rowsWith(SENTINEL).isEmpty()) return true;
        ScheduleUploadPage up = openForm(tabName);
        assumeTrue(up.fieldShown(ScheduleUploadPage.EPISODE), "Not a TV project: a Full / One Line row carries no"
                + " episode, so the suite cannot tell its own row from a real schedule — and these rows can"
                + " never be deleted. Skipping rather than leave an unidentifiable upload behind.");
        up.setField(ScheduleUploadPage.EPISODE, SENTINEL_EPISODE);
        up.pickTodayDate();
        up.submit();
        boolean closed = up.waitFormClosed(UPLOAD_WAIT);
        if (!closed) up.leaveForm();
        assumeTrue(closed, "The upload form did not finish");
        return !waitForRows(tab(tabName), SENTINEL, 1).isEmpty();
    }

    private boolean replaceSentinel(String tabName) {
        ScheduleDistPage page = tab(tabName);
        List<String> items = page.openRowMenu(SENTINEL);
        assumeTrue(items != null && items.contains("Replace"), "Replace is not offered: " + items);
        page.tapMenuItem("Replace");
        ScheduleUploadPage up = new ScheduleUploadPage(driver);
        assumeTrue(up.pickerShown(Duration.ofSeconds(10)), "Replace did not open the picker");
        boolean picked = up.pickPdf(PDF, () -> waitAndDismissPermissions(Duration.ofSeconds(4)));
        if (!picked) { backToTabs(); return false; }
        if (up.fieldShown(ScheduleUploadPage.EPISODE)) up.setField(ScheduleUploadPage.EPISODE, SENTINEL_EPISODE);
        up.submit();
        boolean closed = up.waitFormClosed(UPLOAD_WAIT);
        if (!closed) up.leaveForm();
        backToTabs();
        return closed;
    }

    private Row sentinelRow(String tabName) {
        List<Row> mine = tab(tabName).rowsWith(SENTINEL);
        return mine.isEmpty() ? null : mine.get(0);
    }

    private List<String> sentinelMenu(String tabName) {
        List<String> items = tab(tabName).openRowMenu(SENTINEL);
        assumeTrue(items != null, "The sentinel row's ⋮ could not be tapped");
        return items;
    }

    /** Re-reads a tab (onResume refetches) until at least {@code atLeast} rows match. */
    private List<Row> waitForRows(ScheduleDistPage page, Predicate<Row> match, int atLeast) {
        List<Row> rows = page.rowsWith(match);
        for (int i = 0; i < 8 && rows.size() < atLeast; i++) {
            sleep(5000);
            String current = page.isSelected(ScheduleDistPage.TAB_FULL) ? ScheduleDistPage.TAB_FULL : ScheduleDistPage.TAB_ONE_LINE;
            page.openTab(ScheduleDistPage.TAB_PAGES);
            page.openTab(current);
            rows = page.rowsWith(match);
        }
        return rows;
    }

    private boolean waitForHistory(ScheduleDistPage page, Predicate<Row> match) {
        for (int i = 0; i < 4; i++) {
            if (page.visibleRows().stream().anyMatch(match)) return true;
            sleep(3000);
        }
        return false;
    }

    private Folder waitForFolder(String title, Duration timeout) {
        return waitForFolder(scene, title, timeout);
    }

    private Folder waitForFolder(String sceneNo, String title, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ScheduleDistPage page = tab(ScheduleDistPage.TAB_PAGES);
            page.searchPages(sceneNo);
            for (Folder f : page.settledFolders()) if (title.equals(f.title)) { page.searchPages(""); return f; }
            page.searchPages("");
            sleep(5000);
        }
        return null;
    }

    private ScheduleDistPage openFolder() {
        return openFolder(scene);
    }

    private String waitForForeignApp(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && !pkg.equals(appPackage)) return pkg;
            } catch (Exception ignored) { }
            sleep(1000);
        }
        return null;
    }

    private void tapText(String text) {
        try {
            List<org.openqa.selenium.WebElement> e = driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")"));
            if (!e.isEmpty()) { e.get(0).click(); sleep(1000); }
        } catch (Exception ignored) { }
    }
}
