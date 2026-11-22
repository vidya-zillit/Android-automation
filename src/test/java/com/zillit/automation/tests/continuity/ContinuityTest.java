package com.zillit.automation.tests.continuity;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.continuity.ContinuityAddPage;
import com.zillit.automation.pages.continuity.ContinuityPage;
import com.zillit.automation.pages.continuity.ContinuityScenePage;
import com.zillit.automation.pages.continuity.ContinuityScenePage.Card;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CONTINUITY — the department's photo record of how a scene looked, filed by scene number.
 * Cases and their ids follow {@code CONTINUITY_MODULE_TESTCASES.md}.
 *
 * <p><b>Fixtures.</b> A scene number must start with a digit and is cut at 15 characters,
 * so the {@code ZL_AUTO} marker cannot lead it: every scene is {@code 99ZLAUTO<run>} and
 * friends, every note {@code ZL_AUTO_<run>_…}. Records are made from the gallery's first
 * photos (the runner pushes {@code zl_test_*.jpg} and touches them to the top).
 *
 * <p><b>Adding is asynchronous and silent.</b> The sheet only queues an upload; the
 * {@code POST scene} runs once the file is up, with empty callbacks, and the row appears on
 * the {@code continuity:scenes:created} socket. So "it was added" is always a wait for the
 * row, never a confirmation message — there is none.
 *
 * <p><b>What the records are for.</b>
 * <ul>
 *   <li>R1 — scene {@link #scene}, note {@link #r1Note}, one extra detail. Edited, forwarded
 *       to All Department, finally deleted there.</li>
 *   <li>R2 — the same scene, NO note. The card a recycled view could lend a note to, the
 *       one NOT forwarded (so CT-86 can see whether it leaks), and the one deleted in My
 *       Department.</li>
 *   <li>RZ — the zero-padded scene {@link #padScene}, renamed by CT-42.</li>
 *   <li>G1…G6 — {@link #gridScene}, enough cards to make the grid scroll (CT-36).</li>
 * </ul>
 * CT-99 deletes everything carrying the marker on both tabs.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.CONTINUITY)
class ContinuityTest extends ContinuityBaseTest {

    /** Marker in every scene number (no underscore — it has to fit 15 characters). */
    static final String SCENE_MARK = "ZLAUTO";
    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);

    private final String scene = "99" + SCENE_MARK + RUN;
    private final String padScene = "099" + SCENE_MARK + RUN;
    private final String renamedScene = "098" + SCENE_MARK + RUN;
    private final String gridScene = "96" + SCENE_MARK + RUN;
    private final String cancelledScene = "97" + SCENE_MARK + RUN;
    /** CT-32's own scene: one record with a note, one without (finding 6). */
    private final String noteScene = "95" + SCENE_MARK + RUN;

    private final String note1 = "ZL_AUTO_" + RUN + "_one";
    private final String note1Edited = "ZL_AUTO_" + RUN + "_one_ed";
    private final String padNote = "ZL_AUTO_" + RUN + "_pad";
    private final String note2 = "ZL_AUTO_" + RUN + "_two";
    private final String noteSceneNote = "ZL_AUTO_" + RUN + "_noted";
    private static final String EXTRA_LABEL = "ZLAUTO Label";
    private static final String EXTRA_VALUE = "ZLAUTO value";

    /** Set only once the step that makes it has succeeded. */
    private String r1Note;
    private boolean r2Made;
    private boolean noteSceneMade;
    private boolean padMade;
    private boolean forwarded;
    private String myDepartment;
    private Boolean tvProject;

    private static final Duration ROW_WAIT = Duration.ofSeconds(150);

    // ═══════════════════════════ 1. shell · CT-0 ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("CT-01 Continuity opens on My Department, with both tabs")
    void moduleOpens() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        assertTrue(ContinuityPage.HEADER.equalsIgnoreCase(String.valueOf(page.header())),
                "The header reads \"" + page.header() + "\"");
        List<String> tabs = page.tabNames();
        assertEquals(2, tabs.size(), "Expected two tabs, saw " + tabs);
        assertEquals(ContinuityPage.TAB_MY, tabs.get(0), "First tab: " + tabs);
        assertTrue(tabs.get(1).contains("All") && tabs.get(1).contains("Department"),
                "Second tab: " + tabs);
        assertTrue(page.isSelected(true), "My Department is not the selected tab on opening");
    }

    @Test
    @Order(20)
    @DisplayName("CT-02 Each tab carries its own note banner")
    void bannersPerTab() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        assumeTrue(page.openMyDepartment(), "The My Department tab could not be tapped");
        assertEquals(ContinuityPage.NOTE_MY, page.notesText(), "My Department banner");
        assumeTrue(page.openAllDepartment(), "The All Department tab could not be tapped");
        assertEquals(ContinuityPage.NOTE_ALL, page.notesText(), "All Department banner");
        page.openMyDepartment();
    }

    @Test
    @Order(30)
    @DisplayName("CT-03 The banner's X hides it")
    void bannerCloses() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        page.openMyDepartment();
        assumeTrue(page.notesShown(), "No banner on My Department to close");
        assumeTrue(page.closeNotes(), "The banner's X could not be tapped");
        assertFalse(page.notesShown(), "The banner is still shown after its X was tapped");
    }

    @Test
    @Order(40)
    @DisplayName("CT-04 A tab's info icon explains the tab, with Ok and Watch Video")
    void tabInfoDialog() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        assumeTrue(page.tapTabInfo(true), "The info icon on My Department could not be tapped");
        assertTrue(page.dialogShown(), "No dialog opened from the info icon");
        try {
            assertEquals(ContinuityPage.TAB_MY, page.dialogHeader(), "Info dialog header");
            assertEquals("Ok", page.dialogLeft(), "Info dialog left button");
            assertEquals("Watch Video", page.dialogRight(), "Info dialog right button");
        } finally {
            page.tapDialogLeft();   // Ok — never Watch Video, which opens a web player
        }
        assertFalse(page.dialogShown(), "Ok did not close the info dialog");
    }

    @Test
    @Order(50)
    @DisplayName("CT-05 Adding is offered on My Department only")
    void addOnMyDepartmentOnly() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        page.openMyDepartment();
        assumeTrue(page.addShown(), "No add button on My Department: this user has neither admin"
                + " nor posting access to Continuity, so every case that adds a record will skip");
        page.openAllDepartment();
        boolean onAll = page.addShown();
        page.openMyDepartment();
        assertFalse(onAll, "The add button is offered on All Department, where records only"
                + " ever arrive by being forwarded");
    }

    @Test
    @Order(60)
    @DisplayName("CT-06 The tab search says it searches scene numbers")
    void searchHint() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        page.openMyDepartment();
        assertEquals(ContinuityPage.SEARCH_HINT, page.searchHint(), "Search hint on My Department");
    }

    @Test
    @Order(70)
    @DisplayName("CT-07 An empty tab shows the empty state")
    void emptyState() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        page.openAllDepartment();
        List<String> rows = page.rowTexts();
        boolean empty = page.emptyShown();
        page.openMyDepartment();
        assumeTrue(rows.isEmpty(), "All Department already lists scenes " + rows + " — nothing to check");
        assertTrue(empty, "All Department lists nothing and shows no empty state");
    }

    // ═══════════════════════════ 2. adding · CT-1 ═══════════════════════════

    @Test
    @Order(100)
    @DisplayName("CT-10 The add button offers the five media sources, in order")
    void pickerOptions() {
        ContinuityAddPage add = myTab();
        assumeTrue(add.tapAdd(), "The add button is not offered");
        assertTrue(add.pickerShown(Duration.ofSeconds(10)), "The picker did not open");
        try {
            assertEquals(ContinuityAddPage.PICKER_TITLE, add.pickerTitle(), "Picker title");
            assertEquals(ContinuityAddPage.PICKER_OPTIONS, add.pickerOptions(), "Picker options");
        } finally {
            driver.navigate().back();   // the picker is a cancelable bottom sheet
            sleep(1000);
        }
    }

    @Test
    @Order(110)
    @DisplayName("CT-11 Picking a photo opens the form: Scene No. and Scene Note, no actor")
    void formShape() {
        ContinuityAddPage add = openSheet();
        assertEquals(ContinuityAddPage.ADD_TITLE, add.sheetTitle(), "Sheet title");
        assertTrue(add.fieldShown(ContinuityAddPage.SCENE_NO), "No Scene No. field");
        assertTrue(add.fieldShown(ContinuityAddPage.NOTES), "No Scene Note field");
        assertFalse(add.fieldShown(ContinuityAddPage.ACTOR), "Actor Name is offered — every build"
                + " read hides it; if it is back, update finding 7");
        assertFalse(add.fieldShown(ContinuityAddPage.GENDER), "The gender dropdown is offered");
        tvProject = add.fieldShown(ContinuityAddPage.EPISODE);
    }

    @Test
    @Order(120)
    @DisplayName("CT-12 A blank Scene No. is refused")
    void blankSceneRefused() {
        ContinuityAddPage add = openSheet();
        assumeTrue(add.setField(ContinuityAddPage.SCENE_NO, ""), "Scene No. could not be cleared");
        add.submit();
        assertTrue(add.sheetShown(), "A record was submitted with no scene number");
    }

    @Test
    @Order(130)
    @DisplayName("CT-13 A Scene No. starting with a letter is refused")
    void letterFirstRefused() {
        ContinuityAddPage add = openSheet();
        assumeTrue(add.setField(ContinuityAddPage.SCENE_NO, "A" + RUN), "Scene No. could not be typed");
        add.submit();
        assertTrue(add.sheetShown(), "A scene number starting with a letter was accepted —"
                + " \"Scene No. always starts with a number.\"");
    }

    @Test
    @Order(140)
    @DisplayName("CT-14 Scene No. keeps at most 15 characters")
    void sceneNoCappedAt15() {
        ContinuityAddPage add = openSheet();
        assumeTrue(add.setField(ContinuityAddPage.SCENE_NO, "12345678901234567890"), "Scene No. could not be typed");
        String kept = add.fieldText(ContinuityAddPage.SCENE_NO);
        assertNotNull(kept, "Scene No. could not be read back");
        assertEquals(15, kept.length(), "Scene No. holds \"" + kept + "\"");
    }

    @Test
    @Order(150)
    @DisplayName("CT-15 On a TV project, a blank Episode is refused")
    void tvEpisodeRequired() {
        ContinuityAddPage add = openSheet();
        assumeTrue(Boolean.TRUE.equals(tvProject) || add.fieldShown(ContinuityAddPage.EPISODE),
                "Not a TV project: the Episode field is not offered");
        add.setField(ContinuityAddPage.SCENE_NO, scene);
        add.setField(ContinuityAddPage.EPISODE, "");
        add.submit();
        assertTrue(add.sheetShown(), "A TV record was submitted with no episode");
    }

    @Test
    @Order(160)
    @DisplayName("CT-16 Add More Details refuses a blank title, then a blank description")
    void moreDetailsBlanks() {
        ContinuityAddPage add = openSheet();
        assumeTrue(add.openMoreDetails(), "\"Add More Details\" did not open");
        try {
            add.fillMoreDialog("", "");
            add.tapMoreAdd();
            assertTrue(add.moreDialogShown(), "A detail with no title was accepted");
            add.fillMoreDialog(EXTRA_LABEL, "");
            add.tapMoreAdd();
            assertTrue(add.moreDialogShown(), "A detail with no description was accepted");
        } finally {
            if (add.moreDialogShown()) add.tapMoreCancel();
        }
    }

    @Test
    @Order(170)
    @DisplayName("CT-17 An extra detail is listed, and its delete removes it at once")
    void moreDetailsAddAndRemove() {
        ContinuityAddPage add = openSheet();
        assumeTrue(add.openMoreDetails(), "\"Add More Details\" did not open");
        add.fillMoreDialog(EXTRA_LABEL, EXTRA_VALUE);
        add.tapMoreAdd();
        assertFalse(add.moreDialogShown(), "The detail dialog stayed open with both fields filled");
        String row = EXTRA_LABEL + ": " + EXTRA_VALUE;
        assertTrue(add.moreRows().contains(row), "The detail is not listed. Rows: " + add.moreRows());
        assertTrue(add.deleteMoreRow(EXTRA_LABEL), "The detail's delete icon could not be tapped");
        assertFalse(add.moreRows().contains(row), "The detail is still listed after delete");
    }

    @Test
    @Order(180)
    @DisplayName("CT-18 A valid record is uploaded and its scene appears in My Department")
    void addRecord() {
        ContinuityAddPage add = openSheet();
        // Re-add the extra detail CT-17 removed — R1 carries one for the viewer and edit cases.
        if (add.moreRows().isEmpty() && add.openMoreDetails()) {
            add.fillMoreDialog(EXTRA_LABEL, EXTRA_VALUE);
            add.tapMoreAdd();
        }
        assumeTrue(add.setField(ContinuityAddPage.SCENE_NO, scene), "Scene No. could not be typed");
        add.setField(ContinuityAddPage.NOTES, note1);
        if (add.fieldShown(ContinuityAddPage.EPISODE)) add.setField(ContinuityAddPage.EPISODE, "1");
        add.submit();
        assertFalse(add.sheetShown(Duration.ofSeconds(3)), "The sheet stayed open on a valid record");
        assertTrue(waitForScene(scene, true, ROW_WAIT), "Scene " + scene + " never appeared in My"
                + " Department. The app shows no success or error for an add (finding 1), so a failed"
                + " upload or create looks exactly like this");
        r1Note = note1;
        assertTrue(new ContinuityPage(driver).waitProgressGone(Duration.ofSeconds(30)),
                "The top progress bar was still spinning 30s after the record appeared");
    }

    @Test
    @Order(190)
    @DisplayName("CT-19 Closing the form with X uploads nothing")
    void closeUploadsNothing() {
        ContinuityAddPage add = openSheet();
        add.setField(ContinuityAddPage.SCENE_NO, cancelledScene);
        assumeTrue(add.close(), "The X could not be tapped");
        assertFalse(add.sheetShown(), "The X did not close the form");
        sleep(15000);
        assertFalse(waitForScene(cancelledScene, true, Duration.ofSeconds(25)),
                "Scene " + cancelledScene + " appeared although the form was closed with X");
    }

    // ═══════════════════════════ 3. the tab list · CT-2 ═══════════════════════════

    @Test
    @Order(200)
    @DisplayName("CT-20 Search by scene number filters the rows")
    void searchFilters() {
        assumeTrue(r1Note != null, "CT-18 made no record to search for");
        ContinuityPage page = myTab();
        try {
            page.search(scene);
            List<String> rows = page.sceneNumbers();
            assertTrue(rows.contains(scene), "The scene is not listed for its own number: " + rows);
            for (String r : rows) assertTrue(r.contains(scene), "\"" + r + "\" is listed for \"" + scene + "\"");
        } finally {
            page.clearSearch();
        }
    }

    @Test
    @Order(210)
    @DisplayName("CT-21 Search for nothing leaves no row")
    void searchForNothing() {
        ContinuityPage page = myTab();
        try {
            page.search("ZLQXNOPE" + RUN);
            List<String> rows = page.rowTexts();
            assertTrue(rows.isEmpty(), "Rows listed for a number that does not exist: " + rows
                    + " (empty view shown: " + page.emptyShown() + ")");
        } finally {
            page.clearSearch();
        }
    }

    @Test
    @Order(220)
    @DisplayName("CT-22 A row reads \"Scene No-<n>\"")
    void rowShape() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityPage page = myTab();
        assertTrue(page.rowTexts().contains(ContinuityPage.ROW_PREFIX + scene),
                "No row reads \"" + ContinuityPage.ROW_PREFIX + scene + "\". Rows: " + page.rowTexts());
    }

    @Test
    @Order(230)
    @DisplayName("CT-23 Long-pressing a row offers nothing (finding 3)")
    void rowLongPress() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityPage page = myTab();
        List<String> items = page.longPressScene(scene);
        assumeTrue(items != null, "The row could not be found to long-press");
        if (!items.isEmpty()) { driver.navigate().back(); sleep(800); }
        assertTrue(items.isEmpty(), "The long-press menu now offers " + items
                + " — it used to have every item removed. Update finding 3 and this case");
    }

    @Test
    @Order(240)
    @DisplayName("CT-24 [QA BUG-18] Searching for [ or ( does not break the list")
    void searchSpecialCharacters() {
        ContinuityPage page = myTab();
        try {
            for (String q : List.of("[", "(")) {
                page.search(q);
                assertTrue(page.isLoaded(Duration.ofSeconds(5)), "The screen went away searching \"" + q + "\"");
                List<String> rows = page.sceneNumbers();
                for (String r : rows) assertTrue(r.contains(q), "\"" + r + "\" listed for \"" + q + "\"");
            }
        } finally {
            page.clearSearch();
        }
    }

    @Test
    @Order(250)
    @DisplayName("CT-25 [QA WEB-06] A zero-padded scene number arrives live")
    void paddedSceneLive() {
        ContinuityAddPage add = openSheet();
        add.setField(ContinuityAddPage.SCENE_NO, padScene);
        add.setField(ContinuityAddPage.NOTES, padNote);
        if (add.fieldShown(ContinuityAddPage.EPISODE)) add.setField(ContinuityAddPage.EPISODE, "1");
        add.submit();
        assumeTrue(!add.sheetShown(Duration.ofSeconds(3)), "The form refused " + padScene);
        boolean live = waitForScene(padScene, false, Duration.ofSeconds(120));
        if (!live) {
            boolean atAll = waitForScene(padScene, true, Duration.ofSeconds(40));
            padMade = atAll;
            fail(atAll ? "Scene " + padScene + " was created but only appeared after a refetch —"
                    + " the live update missed it"
                    : "Scene " + padScene + " never appeared, live or after a refetch");
        }
        padMade = true;
    }

    // ═══════════════════════════ 4. the grid · CT-3 ═══════════════════════════

    @Test
    @Order(300)
    @DisplayName("CT-30 A scene opens on its grid, titled with its number")
    void sceneOpens() {
        ContinuityScenePage grid = openGrid(scene);
        assertTrue(("Scene No. - " + scene).equalsIgnoreCase(String.valueOf(grid.header())),
                "Grid title: \"" + grid.header() + "\"");
        assertEquals(ContinuityScenePage.GRID_SEARCH_HINT, grid.searchHint(), "Grid search hint");
    }

    @Test
    @Order(310)
    @DisplayName("CT-31 A card shows its scene and its note")
    void cardShape() {
        ContinuityScenePage grid = openGrid(scene);
        Card c = grid.cardWithNote(r1Note);
        assertNotNull(c, "No card carries note " + r1Note + ". Cards: " + grid.allCards());
        assertEquals("Scene - " + scene, c.scene, "Card scene line");
        assertEquals("Scene Note - " + r1Note, c.notes, "Card note line");
        assertFalse(c.video, "A photo card shows the video play icon");
    }

    /**
     * R2: a second record in the main scene, with a note of its OWN. Every case that has to
     * pick a card picks it by its note, so no two records in {@link #scene} may share one —
     * and a note-less card here would be lent R1's note by the recycling defect (finding 6),
     * which is exactly how a first draft of this suite ended up editing the wrong record.
     */
    @Test
    @Order(315)
    @DisplayName("CT-31b A second record in the same scene is listed beside the first")
    void secondRecord() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        if (!r2Made) {
            assumeTrue(addRecord(scene, note2), "The form refused the second record");
            r2Made = waitForCards(scene, cs -> cs.stream().anyMatch(c -> note2.equals(c.noteValue())), ROW_WAIT);
        }
        assertTrue(r2Made, "The second record (" + note2 + ") never appeared in scene " + scene);
        ContinuityScenePage grid = openGrid(scene);
        List<Card> cards = grid.allCards();
        assertNotNull(grid.cardWithNote(r1Note), "The first record went missing when the second arrived: " + cards);
    }

    /**
     * Finding 6, seen on the device: a record saved with NO note showed another record's note
     * after the grid was rebound (back from the viewer, a search cleared). Freshly drawn, the
     * card is right — which is why the first version of this case passed while the bug sat on
     * screen two cases later. So this reads the grid, forces a rebind, and reads it again.
     * Its own scene, so the lent note cannot confuse the cases that pick cards in {@link #scene}.
     */
    @Test
    @Order(320)
    @DisplayName("CT-32 A card with no note shows no note line, even after a rebind (finding 6)")
    void cardWithoutNote() {
        if (!noteSceneMade) {
            assumeTrue(addRecord(noteScene, noteSceneNote), "The form refused the noted record");
            assumeTrue(addRecord(noteScene, ""), "The form refused a record with no note");
            noteSceneMade = waitForCards(noteScene, cs -> cs.size() >= 2, ROW_WAIT);
            assumeTrue(noteSceneMade, "Scene " + noteScene + " never showed both records");
        }
        ContinuityScenePage grid = openGrid(noteScene);
        List<Card> fresh = grid.visibleCards();
        // Rebind: a search that narrows to the noted card, cleared again.
        grid.search(noteSceneNote);
        grid.clearSearch();
        List<Card> rebound = grid.visibleCards();
        // And the other rebind seen on the device: out to the viewer and back.
        if (grid.tapCard(noteSceneNote)) { driver.navigate().back(); sleep(1500); }
        List<Card> afterViewer = grid.visibleCards();
        for (List<Card> cards : List.of(fresh, rebound, afterViewer)) {
            long withNote = cards.stream().filter(c -> c.notes != null).count();
            assertTrue(cards.size() >= 2 && withNote == 1, "Scene " + noteScene + " holds one record with a note"
                    + " and one without, but the grid shows " + withNote + " note line(s) on " + cards.size()
                    + " card(s) — a recycled card kept another card's note (finding 6)."
                    + " Fresh: " + fresh + " · after a search: " + rebound + " · after the viewer: " + afterViewer);
        }
    }

    @Test
    @Order(330)
    @DisplayName("CT-33 Tapping a card opens the in-app viewer with its description")
    void viewerDescription() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityScenePage grid = openGrid(scene);
        assumeTrue(grid.tapCard(r1Note), "The card could not be tapped");
        try {
            String d = grid.viewerDescription(Duration.ofSeconds(20));
            assertNotNull(d, "The viewer opened with no description, or did not open. On screen: " + whereAreWe());
            assertTrue(d.contains("Scene : " + scene), "Description lacks the scene: " + d);
            assertTrue(d.contains("Scene Note : " + r1Note), "Description lacks the note: " + d);
            assertTrue(d.contains(EXTRA_LABEL + ": " + EXTRA_VALUE), "Description lacks the extra detail: " + d);
        } finally {
            driver.navigate().back();
            sleep(1200);
        }
    }

    @Test
    @Order(340)
    @DisplayName("CT-34 Grid search by note leaves that record")
    void gridSearchByNote() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityScenePage grid = openGrid(scene);
        try {
            grid.search(r1Note);
            List<Card> cards = grid.visibleCards();
            assertEquals(1, cards.size(), "Search by note shows " + cards);
            assertEquals(r1Note, cards.get(0).noteValue(), "Search by note shows " + cards);
        } finally {
            grid.clearSearch();
        }
    }

    @Test
    @Order(350)
    @DisplayName("CT-35 The card menu offers Edit Details, Forward, Distribute, Download, Delete")
    void cardMenu() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityScenePage grid = openGrid(scene);
        List<String> items = grid.openMenu(r1Note);
        assumeTrue(items != null, "The card's ⋮ could not be tapped");
        grid.closeMenu();
        assertEquals(ContinuityScenePage.ADMIN_MENU_MY, items,
                "Card menu (an admin, or a user with posting access, sees all five)");
    }

    // ═══════════════════════════ 5. edit · CT-4 ═══════════════════════════

    @Test
    @Order(400)
    @DisplayName("CT-40 Edit Details opens prefilled")
    void editPrefilled() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityAddPage form = openEdit(scene, r1Note);
        try {
            assertEquals(ContinuityAddPage.EDIT_TITLE, form.sheetTitle(), "Edit sheet title");
            assertEquals(scene, form.fieldText(ContinuityAddPage.SCENE_NO), "Prefilled Scene No.");
            assertEquals(r1Note, form.fieldText(ContinuityAddPage.NOTES), "Prefilled Scene Note");
            assertTrue(form.moreRows().contains(EXTRA_LABEL + ": " + EXTRA_VALUE),
                    "The extra detail is not listed. Rows: " + form.moreRows());
        } finally {
            form.close();
        }
    }

    @Test
    @Order(410)
    @DisplayName("CT-41 [QA BUG-14] Editing the note keeps the record and its picture")
    void editKeepsPicture() {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityScenePage grid = openGrid(scene);
        int[] before = grid.pictureColour(r1Note);
        ContinuityAddPage form = openEdit(scene, r1Note);
        form.setField(ContinuityAddPage.NOTES, note1Edited);
        form.submit();
        assertFalse(form.sheetShown(Duration.ofSeconds(15)), "The edit sheet did not close");
        r1Note = note1Edited;

        backToTabs();
        ContinuityScenePage again = openGrid(scene);
        Card c = again.cardWithNote(note1Edited);
        assertNotNull(c, "After the edit, the record is gone from its scene. The Android edit sends no"
                + " attachment and the backend replaces the whole item (QA BUG-14); a record that loses"
                + " its 'deleted' object is dropped by the grid (finding 14). Cards: " + again.allCards());
        int[] after = again.pictureColour(note1Edited);
        int distance = ContinuityScenePage.colourDistance(before, after);
        assertTrue(before == null || after == null || distance < 90,
                "The card's picture changed after a note-only edit (colour distance " + distance
                        + ") — the file was likely erased by the edit (QA BUG-14)");
    }

    @Test
    @Order(420)
    @DisplayName("CT-42 Changing the scene number closes the page and moves the record")
    void editMovesScene() {
        assumeTrue(padMade, "CT-25 made no zero-padded record to rename");
        ContinuityAddPage form = openEdit(padScene, padNote);
        form.setField(ContinuityAddPage.SCENE_NO, renamedScene);
        form.submit();
        sleep(3000);
        assertTrue(onTabs(), "Changing the scene number did not return to the tabs. On screen: " + whereAreWe());
        assertTrue(waitForScene(renamedScene, true, Duration.ofSeconds(60)),
                "Scene " + renamedScene + " is not listed after the rename");
        assertFalse(new ContinuityPage(driver).hasScene(padScene),
                "Scene " + padScene + " is still listed though its only record moved");
    }

    @Test
    @Order(430)
    @DisplayName("CT-43 [QA BUG-14] Removing every extra detail is saved")
    void editClearsDetails() {
        assumeTrue(r1Note != null, "No record to edit");
        ContinuityAddPage form = openEdit(scene, r1Note);
        assumeTrue(form.deleteMoreRow(EXTRA_LABEL), "The record has no extra detail to remove. Rows: " + form.moreRows());
        form.submit();
        assumeTrue(!form.sheetShown(Duration.ofSeconds(15)), "The edit sheet did not close");
        ContinuityAddPage again = openEdit(scene, r1Note);
        try {
            assertTrue(again.moreRows().isEmpty(), "The removed detail came back: " + again.moreRows()
                    + ". An empty list is left out of the request (finding 10)");
        } finally {
            again.close();
        }
    }

    @Test
    @Order(440)
    @DisplayName("CT-44 Retyping the same scene number keeps the page open (finding 11)")
    void sameSceneNumberStays() {
        assumeTrue(r1Note != null, "No record to edit");
        ContinuityAddPage form = openEdit(scene, r1Note);
        form.setField(ContinuityAddPage.SCENE_NO, scene);
        form.submit();
        sleep(3000);
        boolean stayed = onScenePage();
        if (!stayed) openContinuity();
        assertTrue(stayed, "Saving with the scene number retyped unchanged closed the page — any"
                + " keystroke in Scene No. marks it changed (finding 11)");
    }

    // ═══════════════════════════ 6. forward · CT-5 ═══════════════════════════

    @Test
    @Order(500)
    @DisplayName("CT-50 Forward starts selection with the card ticked")
    void forwardStartsSelection() {
        ContinuityScenePage grid = startSelection("Forward");
        try {
            assertEquals("1", grid.selectionCount(), "Selection count");
            assertEquals("Forward", grid.selectionAction(), "Selection button");
            Card c = grid.cardWithNote(r1Note);
            assertTrue(c != null && c.selected, "The forwarded card is not shown ticked: " + c);
        } finally {
            grid.cancelSelection();
        }
    }

    @Test
    @Order(510)
    @DisplayName("CT-51 Tapping another card adds it, tapping again removes it")
    void selectionToggles() {
        assumeTrue(r2Made, "CT-32 made no second record");
        ContinuityScenePage grid = startSelection("Forward");
        try {
            assumeTrue(grid.tapCard(note2), "The second record's card could not be tapped");
            assertEquals("2", grid.selectionCount(), "Count after adding a card");
            grid.tapCard(note2);
            assertEquals("1", grid.selectionCount(), "Count after removing it again");
        } finally {
            grid.cancelSelection();
        }
    }

    @Test
    @Order(520)
    @DisplayName("CT-52 Cancel ends selection and unticks everything")
    void selectionCancels() {
        ContinuityScenePage grid = startSelection("Forward");
        assumeTrue(grid.cancelSelection(), "Cancel could not be tapped");
        assertFalse(grid.selectionBarShown(), "The selection bar is still shown after Cancel");
        for (Card c : grid.visibleCards()) assertFalse(c.selected, "Still ticked after Cancel: " + c);
    }

    @Test
    @Order(530)
    @DisplayName("CT-53 Forward offers All Department and Select User")
    void forwardTargets() {
        ContinuityScenePage grid = startSelection("Forward");
        try {
            assumeTrue(grid.tapSelectionAction(), "The Forward button could not be tapped");
            List<String> targets = grid.popupMenuTitles();
            if (!targets.isEmpty()) { driver.navigate().back(); sleep(800); }
            assertTrue(targets.contains("All Department") && targets.contains("Select User"),
                    "Forward targets: " + targets);
        } finally {
            if (grid.selectionBarShown()) grid.cancelSelection();
        }
    }

    @Test
    @Order(540)
    @DisplayName("CT-54 Forwarding to All Department asks first, and No forwards nothing")
    void forwardAsksFirst() {
        ContinuityScenePage grid = startSelection("Forward");
        try {
            assumeTrue(grid.tapSelectionAction() && grid.tapPopupMenu("All Department"),
                    "Could not reach All Department from Forward");
            assertTrue(grid.dialogShown(), "Forwarding to everyone did not ask first");
            String msg = grid.dialogMessage();
            grid.tapDialogLeft();
            assertTrue(msg != null && msg.contains("visible to all"), "Confirm message: " + msg);
        } finally {
            if (grid.selectionBarShown()) grid.cancelSelection();
        }
    }

    @Test
    @Order(550)
    @DisplayName("CT-55 Forwarding to All Department is confirmed")
    void forwardToAll() {
        ContinuityScenePage grid = startSelection("Forward");
        assumeTrue(grid.tapSelectionAction() && grid.tapPopupMenu("All Department"),
                "Could not reach All Department from Forward");
        assumeTrue(grid.dialogShown(), "No confirm to say Yes to");
        grid.tapDialogRight();
        assertTrue(grid.messageShown("Scene shared with All Department", Duration.ofSeconds(20)),
                "No \"Scene shared with All Department\" after confirming");
        forwarded = true;
    }

    @Test
    @Order(560)
    @DisplayName("CT-56 Select User refuses an empty selection")
    void selectUserEmpty() {
        ContinuityScenePage grid = startSelection("Forward");
        assumeTrue(grid.tapSelectionAction() && grid.tapPopupMenu("Select User"),
                "Could not reach Select User from Forward");
        try {
            assumeTrue(present("btnAddMoreDepartmentLevel"), "The users list did not open. On screen: " + whereAreWe());
            // custom_button_orange: the clickable view is buttonBtn inside the include.
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/buttonBtn\")")).click();
            boolean told = grid.messageShown("Please choose at least one user", Duration.ofSeconds(4));
            assertTrue(told || present("btnAddMoreDepartmentLevel"),
                    "An empty user selection was submitted — the users list closed with nobody picked");
        } finally {
            if (present("btnAddMoreDepartmentLevel")) { driver.navigate().back(); sleep(1200); }
            if (grid.selectionBarShown()) grid.cancelSelection();
        }
    }

    // ═══════════════════════ 7. distribute, download · CT-6 ═══════════════════════

    @Test
    @Order(600)
    @DisplayName("CT-60 Distribute starts selection with a Distribute button")
    void distributeStartsSelection() {
        ContinuityScenePage grid = startSelection("Distribute");
        try {
            assertEquals("Distribute", grid.selectionAction(), "Selection button");
        } finally {
            grid.cancelSelection();
        }
    }

    @Test
    @Order(610)
    @DisplayName("CT-61 Distribute asks first, and No distributes nothing")
    void distributeAsksFirst() {
        ContinuityScenePage grid = startSelection("Distribute");
        try {
            assumeTrue(grid.tapSelectionAction(), "The Distribute button could not be tapped");
            if (grid.messageShown("You don't have posting rights", Duration.ofSeconds(3))) {
                assumeTrue(false, "This user has no posting access to Document Distribution");
            }
            assertTrue(grid.dialogShown(), "Distributing did not ask first");
            String msg = grid.dialogMessage();
            grid.tapDialogLeft();
            assertTrue(msg != null && msg.toLowerCase().contains("distribut"), "Confirm message: " + msg);
        } finally {
            if (grid.dialogShown()) grid.tapDialogLeft();
            if (grid.selectionBarShown()) grid.cancelSelection();
        }
    }

    @Test
    @Order(620)
    @DisplayName("CT-62 [QA BUG-15] Download saves the file, or offers to request the right")
    void download() {
        assumeTrue(r1Note != null, "No record to download");
        ContinuityScenePage grid = openGrid(scene);
        assumeTrue(grid.openMenu(r1Note) != null, "The card's ⋮ could not be tapped");
        assumeTrue(grid.tapMenuItem("Download"), "Download is not in the menu");
        if (grid.dialogShown()) {
            String msg = grid.dialogMessage();
            grid.tapDialogLeft();   // Cancel — Yes would message the admins
            assertTrue(msg != null && msg.contains("Downloading Rights"), "Unexpected dialog on Download: " + msg);
            return;
        }
        assertTrue(grid.messageShown("File Downloaded Successfully.", Duration.ofSeconds(30)),
                "Download ended with neither \"File Downloaded Successfully.\" nor the rights Alert");
    }

    // ═══════════════════════ 9. All Department · CT-8 ═══════════════════════
    // Runs before the deletes: CT-72 deletes R1 here.

    @Test
    @Order(700)
    @DisplayName("CT-80 On All Department a scene opens on its department list")
    void allDepartmentList() {
        assumeTrue(forwarded, "CT-55 forwarded nothing to All Department");
        ContinuityScenePage page = openAllScene(scene);
        assertTrue(ContinuityScenePage.DEPT_LIST_TITLE.equalsIgnoreCase(String.valueOf(page.header())),
                "Title: \"" + page.header() + "\"");
        assertEquals(ContinuityScenePage.DEPT_SEARCH_HINT, page.searchHint(), "Search hint");
        assertFalse(page.departments().isEmpty(), "No department is listed for a forwarded scene");
    }

    @Test
    @Order(710)
    @DisplayName("CT-81 A department's grid names that department on every card")
    void allDepartmentGrid() {
        assumeTrue(forwarded, "CT-55 forwarded nothing");
        ContinuityScenePage page = openAllScene(scene);
        String dept = findDepartmentHolding(page, r1Note);
        assumeTrue(dept != null, "No department's grid holds the forwarded record. Departments: " + page.departments());
        myDepartment = dept;
        assertTrue(("Scene No - " + scene).equalsIgnoreCase(String.valueOf(page.header())),
                "Grid title: \"" + page.header() + "\"");
        for (Card c : page.allCards()) {
            assertEquals("Department - " + dept, c.detail, "Card in " + dept + "'s grid: " + c);
        }
    }

    @Test
    @Order(720)
    @DisplayName("CT-86 [QA BUG-07] A record not forwarded stays out of All Department")
    void unforwardedStaysPrivate() {
        assumeTrue(forwarded && r2Made && myDepartment != null, "Needs R1 forwarded and R2 kept back");
        ContinuityScenePage page = openAllScene(scene);
        assumeTrue(page.openDepartment(myDepartment), "Could not open " + myDepartment);
        page.waitLoaded(Duration.ofSeconds(30));
        List<Card> cards = page.allCards();
        assertTrue(cards.stream().anyMatch(c -> r1Note.equals(c.noteValue())), "The forwarded record is missing: " + cards);
        assertTrue(cards.stream().noneMatch(c -> note2.equals(c.noteValue())), "The record that was never forwarded"
                + " is listed on All Department (QA BUG-07): " + cards);
    }

    @Test
    @Order(730)
    @DisplayName("CT-85 The card menu on All Department has no Edit Details")
    void allDepartmentMenu() {
        assumeTrue(forwarded && myDepartment != null, "Needs R1 forwarded");
        ContinuityScenePage page = openAllScene(scene);
        assumeTrue(page.openDepartment(myDepartment), "Could not open " + myDepartment);
        page.waitLoaded(Duration.ofSeconds(30));
        List<String> items = page.openMenu(r1Note);
        assumeTrue(items != null, "The card's ⋮ could not be tapped");
        page.closeMenu();
        assertFalse(items.contains("Edit Details"), "Edit Details is offered on All Department: " + items);
        assertTrue(items.contains("Download") && items.contains("Delete"), "Menu: " + items);
    }

    @Test
    @Order(740)
    @DisplayName("CT-82 Back steps grid → department list → tabs")
    void allDepartmentBack() {
        assumeTrue(forwarded && myDepartment != null, "Needs R1 forwarded");
        ContinuityScenePage page = openAllScene(scene);
        assumeTrue(page.openDepartment(myDepartment), "Could not open " + myDepartment);
        driver.navigate().back();
        sleep(1500);
        assertTrue(page.onDepartmentList(), "Back from a department's grid did not return to its list");
        assertTrue(ContinuityScenePage.DEPT_LIST_TITLE.equalsIgnoreCase(String.valueOf(page.header())),
                "Title after back: \"" + page.header() + "\"");
        driver.navigate().back();
        sleep(1500);
        assertTrue(onTabs(), "Back from the department list did not leave the scene. On screen: " + whereAreWe());
    }

    @Test
    @Order(750)
    @DisplayName("CT-83 Opening a second department shows only its own records (finding 2)")
    void secondDepartmentNotStale() {
        assumeTrue(forwarded, "Needs a forwarded scene");
        ContinuityScenePage page = openAllScene(scene);
        List<String> depts = page.departments();
        assumeTrue(depts.size() >= 2, "Only " + depts + " holds scene " + scene + " — a second department is needed");
        assumeTrue(page.openDepartment(depts.get(0)), "Could not open " + depts.get(0));
        page.waitLoaded(Duration.ofSeconds(30));
        driver.navigate().back();
        sleep(1500);
        assumeTrue(page.openDepartment(depts.get(1)), "Could not open " + depts.get(1));
        page.waitLoaded(Duration.ofSeconds(30));
        for (Card c : page.allCards()) {
            assertEquals("Department - " + depts.get(1), c.detail,
                    "A card from the previous department is still in the grid (finding 2): " + c);
        }
    }

    @Test
    @Order(760)
    @DisplayName("CT-84 A department's long-press \"Edit\" does not distribute (finding 4)")
    void departmentEditIsNotDistribute() {
        assumeTrue(forwarded && myDepartment != null, "Needs R1 forwarded");
        ContinuityScenePage page = openAllScene(scene);
        List<String> items = page.longPressDepartment(myDepartment);
        assumeTrue(items != null && !items.isEmpty(), "Long-pressing a department opened no menu");
        assumeTrue(items.contains("Edit"), "The menu offers " + items + " — no Edit to check");
        page.tapPopupMenu("Edit");
        sleep(2000);
        String msg = page.dialogShown() ? page.dialogMessage() : null;
        // Answer No to anything it raised: Yes would distribute the department's folder.
        for (int i = 0; i < 2 && page.dialogShown(); i++) { page.tapDialogLeft(); sleep(800); }
        assertFalse(msg != null && msg.toLowerCase().contains("distribut"),
                "\"Edit\" on a department raised a Distribute confirm: \"" + msg + "\" (finding 4)");
    }

    // ═══════════════════════════ 8. delete · CT-7 ═══════════════════════════

    @Test
    @Order(800)
    @DisplayName("CT-70 Delete asks first, and No keeps the card")
    void deleteAsksFirst() {
        assumeTrue(r2Made, "No second record to delete");
        ContinuityScenePage grid = openGrid(scene);
        assumeTrue(grid.openMenu(note2) != null, "The second record's ⋮ could not be tapped");
        assumeTrue(grid.tapMenuItem("Delete"), "Delete is not in the menu");
        assertTrue(grid.dialogShown(), "Delete did not ask first");
        assertEquals("Delete", grid.dialogHeader(), "Confirm header");
        assertEquals("Are you sure you want to delete?", grid.dialogMessage(), "Confirm message");
        grid.tapDialogLeft();
        assertNotNull(grid.cardWithNote(note2), "No took the card anyway");
    }

    @Test
    @Order(810)
    @DisplayName("CT-71 [QA BUG-20] Delete removes the card, with a readable message")
    void deleteFromMyDepartment() {
        assumeTrue(r2Made, "No second record to delete");
        ContinuityScenePage grid = openGrid(scene);
        assumeTrue(grid.openMenu(note2) != null, "The second record's ⋮ could not be tapped");
        assumeTrue(grid.tapMenuItem("Delete") && grid.dialogShown(), "Delete did not ask first");
        grid.tapDialogRight();
        String snack = grid.snackbarText(Duration.ofSeconds(10));
        boolean gone = waitForCards(scene, cs -> cs.stream().noneMatch(c -> note2.equals(c.noteValue())), Duration.ofSeconds(60));
        if (gone) r2Made = false;
        assertTrue(gone, "The deleted card is still in scene " + scene);
        assertTrue(snack == null || !snack.matches("[a-z0-9_.]+"),
                "The delete message is an internal key, not text: \"" + snack + "\" (QA BUG-20)");
    }

    @Test
    @Order(820)
    @DisplayName("CT-72 Deleting on All Department keeps the My Department copy")
    void deleteFromAllKeepsMine() {
        assumeTrue(forwarded && myDepartment != null && r1Note != null, "Needs R1 forwarded");
        ContinuityScenePage page = openAllScene(scene);
        assumeTrue(page.openDepartment(myDepartment), "Could not open " + myDepartment);
        page.waitLoaded(Duration.ofSeconds(30));
        assumeTrue(page.openMenu(r1Note) != null && page.tapMenuItem("Delete") && page.dialogShown(),
                "Could not reach Delete for the forwarded record");
        page.tapDialogRight();
        sleep(4000);
        backToTabs();
        ContinuityScenePage mine = openGrid(scene);
        assertNotNull(mine.cardWithNote(r1Note), "Deleting the All Department copy also removed the"
                + " record from My Department. Cards: " + mine.allCards());
    }

    // ═══════════════════════ 4b. a grid taller than the screen · CT-36 ═══════════════════════

    @Test
    @Order(900)
    @DisplayName("CT-36 Every card of a grid taller than the screen is read back")
    void tallGrid() {
        List<String> notes = new ArrayList<>();
        for (int i = 1; i <= 6; i++) notes.add("ZL_AUTO_" + RUN + "_g" + i);
        for (String n : notes) {
            ContinuityAddPage add = openSheet();
            add.setField(ContinuityAddPage.SCENE_NO, gridScene);
            add.setField(ContinuityAddPage.NOTES, n);
            if (add.fieldShown(ContinuityAddPage.EPISODE)) add.setField(ContinuityAddPage.EPISODE, "1");
            add.submit();
            assumeTrue(!add.sheetShown(Duration.ofSeconds(3)), "The form refused " + n);
            new ContinuityPage(driver).waitProgressGone(Duration.ofSeconds(90));
        }
        boolean all = waitForCards(gridScene, cs -> cs.size() >= notes.size(), Duration.ofSeconds(180));
        ContinuityScenePage grid = openGrid(gridScene);
        List<Card> cards = grid.allCards();
        for (String n : notes) {
            assertTrue(cards.stream().anyMatch(c -> n.equals(c.noteValue())),
                    "Card " + n + " was not read back (all arrived: " + all + "). Read: " + cards);
        }
        List<String> menu = grid.openMenu(notes.get(notes.size() - 1));
        grid.closeMenu();
        assertNotNull(menu, "The last card's ⋮ could not be reached by scrolling");
    }

    // ═══════════════════════════ 11. cleanup · CT-99 ═══════════════════════════

    @Test
    @Order(990)
    @DisplayName("CT-99 Nothing carrying the ZLAUTO marker is left on either tab")
    void sweep() {
        openContinuity();
        ContinuityPage page = new ContinuityPage(driver);
        page.openMyDepartment();
        for (String s : markedScenes(page)) {
            if (page.openScene(s)) { deleteEveryCardHere(); backToTabs(); }
        }
        page = new ContinuityPage(driver);
        page.openAllDepartment();
        for (String s : markedScenes(page)) {
            if (!page.openScene(s)) continue;
            ContinuityScenePage sp = new ContinuityScenePage(driver);
            sp.waitLoaded(Duration.ofSeconds(30));
            for (String d : sp.departments()) {
                if (sp.openDepartment(d)) { deleteEveryCardHere(); driver.navigate().back(); sleep(1500); }
            }
            backToTabs();
        }
        sleep(5000);
        page = new ContinuityPage(driver);
        page.openMyDepartment();
        List<String> leftMine = markedScenes(page);
        page.openAllDepartment();
        List<String> leftAll = markedScenes(page);
        page.openMyDepartment();
        assertTrue(leftMine.isEmpty() && leftAll.isEmpty(),
                "Marked scenes left behind — My Department: " + leftMine + ", All Department: " + leftAll);
    }

    // ═══════════════════════════ helpers ═══════════════════════════

    /** On the tabs, My Department selected. */
    private ContinuityAddPage myTab() {
        openContinuity();
        ContinuityAddPage page = new ContinuityAddPage(driver);
        if (!page.isSelected(true)) page.openMyDepartment();
        return page;
    }

    /** The Add sheet — the one already up, or a fresh one: + → Gallery → one photo. */
    private ContinuityAddPage openSheet() {
        ContinuityAddPage add = new ContinuityAddPage(driver);
        if (add.sheetShown()) {
            if (ContinuityAddPage.ADD_TITLE.equals(add.sheetTitle())) return add;
            add.close();   // an Edit sheet left over — never type a new record into it
        }
        add = myTab();
        assumeTrue(add.addShown(), "No add button: this user cannot add Continuity records");
        assumeTrue(add.tapAdd(), "The add button could not be tapped");
        assumeTrue(add.pickerShown(Duration.ofSeconds(10)), "The picker did not open");
        boolean ok = add.pickPhotos(1, () -> waitAndDismissPermissions(Duration.ofSeconds(4)));
        if (!ok) backToTabs();
        assumeTrue(ok, "Could not pick a photo from the app's gallery. The runner pushes"
                + " zl_test_*.jpg to /sdcard/Pictures; on screen: " + whereAreWe());
        return add;
    }

    /** Adds one record (one gallery photo) and returns whether the form accepted it. */
    private boolean addRecord(String sceneNo, String note) {
        ContinuityAddPage add = openSheet();
        add.setField(ContinuityAddPage.SCENE_NO, sceneNo);
        add.setField(ContinuityAddPage.NOTES, note);
        if (add.fieldShown(ContinuityAddPage.EPISODE)) add.setField(ContinuityAddPage.EPISODE, "1");
        add.submit();
        boolean accepted = !add.sheetShown(Duration.ofSeconds(3));
        if (accepted) new ContinuityPage(driver).waitProgressGone(Duration.ofSeconds(90));
        return accepted;
    }

    /** A scene's grid, opened from My Department. */
    private ContinuityScenePage openGrid(String sceneNo) {
        ContinuityScenePage grid = new ContinuityScenePage(driver);
        if (onScenePage() && grid.onGrid()
                && ("Scene No. - " + sceneNo).equalsIgnoreCase(String.valueOf(grid.header()))) {
            return grid;
        }
        if (!onTabs()) backToTabs();
        ContinuityPage tabs = new ContinuityPage(driver);
        tabs.openMyDepartment();
        assumeTrue(tabs.openScene(sceneNo), "Scene " + sceneNo + " is not listed on My Department");
        assumeTrue(grid.isLoaded(Duration.ofSeconds(20)), "The scene page did not open");
        assumeTrue(grid.waitLoaded(Duration.ofSeconds(40)), "The grid's loader was still animating"
                + " after 40s — it is only hidden on a successful response, so this is an API error"
                + " the app does not report (finding 5)");
        return grid;
    }

    /** A scene opened from All Department — its department list. */
    private ContinuityScenePage openAllScene(String sceneNo) {
        if (!onTabs()) backToTabs();
        ContinuityPage tabs = new ContinuityPage(driver);
        tabs.openAllDepartment();
        assumeTrue(waitForScene(sceneNo, false, Duration.ofSeconds(5)) || waitForAllScene(sceneNo),
                "Scene " + sceneNo + " is not listed on All Department");
        assumeTrue(tabs.openScene(sceneNo), "Scene " + sceneNo + " could not be opened on All Department");
        ContinuityScenePage page = new ContinuityScenePage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(20)), "The scene page did not open");
        page.waitLoaded(Duration.ofSeconds(30));
        return page;
    }

    private boolean waitForAllScene(String sceneNo) {
        ContinuityPage tabs = new ContinuityPage(driver);
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            tabs.openMyDepartment();
            tabs.openAllDepartment();
            if (tabs.hasScene(sceneNo)) return true;
            sleep(3000);
        }
        return false;
    }

    /** Opens the Edit sheet for the card carrying {@code note} in {@code sceneNo}. */
    private ContinuityAddPage openEdit(String sceneNo, String note) {
        ContinuityScenePage grid = openGrid(sceneNo);
        assumeTrue(grid.openMenu(note) != null, "The ⋮ of card \"" + note + "\" could not be tapped");
        assumeTrue(grid.tapMenuItem("Edit Details"), "Edit Details is not in the menu");
        ContinuityAddPage form = new ContinuityAddPage(driver);
        assumeTrue(form.sheetShown(Duration.ofSeconds(10)), "The Edit sheet did not open");
        return form;
    }

    /** R1's ⋮ → {@code action} (Forward / Distribute). */
    private ContinuityScenePage startSelection(String action) {
        assumeTrue(r1Note != null, "CT-18 made no record");
        ContinuityScenePage grid = openGrid(scene);
        if (grid.selectionBarShown()) grid.cancelSelection();
        assumeTrue(grid.openMenu(r1Note) != null, "The card's ⋮ could not be tapped");
        assumeTrue(grid.tapMenuItem(action), action + " is not in the menu");
        assumeTrue(grid.selectionBarShown(), action + " did not start selection");
        return grid;
    }

    /**
     * Waits for a scene row on the current tab.
     *
     * @param refetch when true, re-selects the tab every 20s — {@code onResume} refetches —
     *                so a missed socket does not fail the wait. CT-25, which is ABOUT the
     *                live update, passes false.
     */
    private boolean waitForScene(String sceneNo, boolean refetch, Duration timeout) {
        ContinuityPage tabs = new ContinuityPage(driver);
        boolean my = tabs.isSelected(true);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        long nextRefetch = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (tabs.hasScene(sceneNo)) return true;
            if (refetch && System.currentTimeMillis() > nextRefetch) {
                if (my) { tabs.openAllDepartment(); tabs.openMyDepartment(); }
                else    { tabs.openMyDepartment(); tabs.openAllDepartment(); }
                nextRefetch = System.currentTimeMillis() + 20_000;
            }
            sleep(3000);
        }
        return false;
    }

    /** Re-opens a scene's grid until its cards satisfy {@code ok}. */
    private boolean waitForCards(String sceneNo, Predicate<List<Card>> ok, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onTabs()) backToTabs();
            ContinuityPage tabs = new ContinuityPage(driver);
            tabs.openMyDepartment();
            if (tabs.hasScene(sceneNo) && tabs.openScene(sceneNo)) {
                ContinuityScenePage grid = new ContinuityScenePage(driver);
                grid.waitLoaded(Duration.ofSeconds(30));
                if (ok.test(grid.allCards())) return true;
                backToTabs();
            } else if (ok.test(List.of())) {
                return true;
            }
            sleep(8000);
        }
        return false;
    }

    private String findDepartmentHolding(ContinuityScenePage page, String note) {
        for (String d : page.departments()) {
            if (!page.openDepartment(d)) continue;
            page.waitLoaded(Duration.ofSeconds(30));
            if (page.cardWithNote(note) != null) return d;
            driver.navigate().back();
            sleep(1500);
        }
        return null;
    }

    private List<String> markedScenes(ContinuityPage page) {
        List<String> out = new ArrayList<>();
        for (String s : page.sceneNumbers()) if (s.contains(SCENE_MARK)) out.add(s);
        return out;
    }

    /** Deletes every card in the open grid, one at a time, waiting for each to go. */
    private void deleteEveryCardHere() {
        ContinuityScenePage grid = new ContinuityScenePage(driver);
        grid.waitLoaded(Duration.ofSeconds(30));
        for (int i = 0; i < 30; i++) {
            List<Card> cards = grid.visibleCards();
            if (cards.isEmpty()) return;
            Card c = cards.get(0);
            if (grid.openMenu(c.noteValue()) == null) return;
            if (!grid.tapMenuItem("Delete")) { grid.closeMenu(); return; }
            if (!grid.dialogShown()) return;
            grid.tapDialogRight();
            sleep(3500);
        }
    }
}
