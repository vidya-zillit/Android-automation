package com.zillit.automation.tests.costume;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.costume.CostumeFormPage;
import com.zillit.automation.pages.costume.CostumePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Costume (Main Cast) and Costume (Background Cast), single device — every case runs once
 * per tool ({@code @MethodSource("tools")}). Cases and source references:
 * {@code COSTUME_TESTCASES.md}.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ./run-and-report.sh --only CostumeTest
 * </pre>
 *
 * <p>Records live under a per-run episode ({@link #episode}); the last cases delete them,
 * and COS-99 sweeps any "Zl_auto_cos" folder an earlier run left.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.COSTUME)
class CostumeTest extends CostumeBaseTest {

    private static final Duration LIST_WAIT = Duration.ofSeconds(40);

    /** The main link fixture per tool (Selects → Shortlist). */
    private final Map<Tool, String> linkChar = new EnumMap<>(Tool.class);
    /** The photo fixture per tool (Selects → Final). */
    private final Map<Tool, String> photoChar = new EnumMap<>(Tool.class);
    private final Map<Tool, Boolean> movedToShortlist = new EnumMap<>(Tool.class);
    private final Map<Tool, Boolean> movedToFinal = new EnumMap<>(Tool.class);
    private final Map<Tool, String> description = new EnumMap<>(Tool.class);
    /** The ZL_AUTO cast this run makes in Casting → Final, per tool. */
    private final Map<Tool, String> castChar = new EnumMap<>(Tool.class);
    private final Map<Tool, String> shortlistDesc = new EnumMap<>(Tool.class);

    // ═════════════════════════ landing ═════════════════════════

    @Test
    @Order(1)
    @DisplayName("COS-01 Tools lists both Costume tiles: 'Costume (Main Cast)' and 'Costume (Background Cast)'")
    void tilesListed() {
        assumeTrue(recoverToBottomNav(), "Not on a bottom-nav screen");
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab() && tools.isLoaded(Duration.ofSeconds(15)), "Tools did not open");
        tools.searchTools("Costume");
        sleep(1500);
        boolean main = toolListed(Tool.MAIN.label), bg = toolListed(Tool.BACKGROUND.label);
        System.out.println(">>> COS-01 tools: " + tools.visibleModuleNames());
        tools.clearSearch();
        assertTrue(main && bg, "Both tiles should be listed — Main: " + main + ", Background: " + bg
                + " (WARDROBE_MAIN_TOOL / WARDROBE_BACKGROUND_TOOL in Tools.kt)");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(2)
    @DisplayName("COS-02 Opening the tool: header, tabs Selects / Shortlist / Final, search, filter and add buttons")
    void landing(Tool t) {
        CostumePage p = openTool(t);
        assertEquals(t.label.toUpperCase(), p.headerText().trim().toUpperCase(), "Header should be the tool name");
        assertEquals(CostumePage.TABS, p.tabLabels(), "Tabs (WardrobePage.handleTabLayout)");
        assertEquals(CostumePage.SEARCH_HINT, p.searchHint().trim(), "Search hint (casting_search_hint)");
        assertTrue(p.present("settingImage"), "The filter icon should be in the toolbar");
        assertTrue(p.shown("takeSceneShots", Duration.ofSeconds(3)), "The '+' add button should be shown (admin has posting rights)");
        assertTrue(p.shown("uploadFromCam", Duration.ofSeconds(2)), "The camera add button should be shown");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(3)
    @DisplayName("COS-03 Filter sheet: Group by Scene No / Character / Episode (Episode on a TV project), Filter by Male / Female / Non Binary / All")
    void filterSheet(Tool t) {
        CostumePage p = openTool(t);
        assertTrue(p.openFilter(), "The filter sheet should open from the toolbar icon");
        try {
            List<String> group = p.filterOptions("recFilterBy"), filter = p.filterOptions("recSortBy");
            System.out.println(">>> COS-03 group=" + group + " filter=" + filter
                    + " selected=" + p.selectedFilterOption("recFilterBy") + "/" + p.selectedFilterOption("recSortBy"));
            assertEquals(List.of("Scene No", "Character", "Episode"), group, "Group by options (WardrobeVM.initializeFilter, TV project)");
            assertEquals(List.of("Male", "Female", "Non Binary", "All"), filter, "Filter by options");
            String g = p.selectedFilterOption("recFilterBy"), s = p.selectedFilterOption("recSortBy");
            if (!g.isEmpty()) assertEquals("Episode", g, "Default grouping on a TV project is Episode");
            if (!s.isEmpty()) assertEquals("All", s, "Default filter is All");
        } finally {
            p.back();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(4)
    @DisplayName("COS-04 The unit chat icon shows on Selects and Shortlist and is hidden on Final")
    void unitChatIconPerTab(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        boolean sel = p.unitChatShown();
        p.selectTab(CostumePage.TAB_SHORTLIST);
        boolean sl = p.unitChatShown();
        p.selectTab(CostumePage.TAB_FINAL);
        sleep(1500);
        boolean fin = p.unitChatShown();
        p.selectTab(CostumePage.TAB_SELECTS);
        assumeTrue(sel || sl, "No unit chat icon at all — the tool's units list did not load (fetch*WardrobeUnitList)");
        assertTrue(sel, "Selects should show the unit chat icon");
        assertTrue(sl, "Shortlist should show the unit chat icon");
        assertFalse(fin, "Final should hide the unit chat icon (handleUnitInfo: != FINALIZE_IDENTIFIER)");
    }

    // ═════════════════════════ add form ═════════════════════════

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(5)
    @DisplayName("COS-05 '+' opens 'Add Costume Details': Upload Media, Select Character, Episode no., Scene No., Description, Submit")
    void addFormLayout(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.tapAdd(), "'+' should be tappable");
        CostumeFormPage f = form();
        try {
            assertTrue(f.isOpen(Duration.ofSeconds(10)), "The add form should open");
            assertEquals(CostumeFormPage.HDR_ADD.toUpperCase(), f.header().trim().toUpperCase(), "Header (add_wardrobe_details)");
            assertTrue(f.uploadMediaShown(), "Toolbar should offer 'Upload Media'");
            assertEquals(CostumeFormPage.SELECT_CHARACTER, f.characterTitle().trim(), "Character card reads 'Select Character'");
            assertTrue(f.characterAddShown(), "Character card has its '+'");
            assertTrue(f.fieldShown(CostumeFormPage.FIELD_EPISODE), "Episode no. is shown on a TV project");
            assertTrue(f.fieldShown(CostumeFormPage.FIELD_SCENE), "Scene No. field");
            assertTrue(f.fieldShown(CostumeFormPage.FIELD_DESC), "Description field");
            assertEquals("Submit", f.submitLabel().trim(), "Button reads Submit");
        } finally {
            leaveForm();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(6)
    @DisplayName("COS-06 Submitting with no media and no link is refused: 'Costume Attachment Required.'")
    void attachmentRequired(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.tapAdd(), "'+' should be tappable");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            f.setField(CostumeFormPage.FIELD_SCENE, SCENE);
            f.submit();
            String s = f.waitSnackbar(CostumeFormPage.MSG_ATTACHMENT, Duration.ofSeconds(6));
            assertEquals(CostumeFormPage.MSG_ATTACHMENT, s.trim(), "Snackbar (wardrobe_attachment_requred)");
            assertTrue(f.isOpen(Duration.ofSeconds(2)), "The form should stay open");
        } finally {
            leaveForm();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(7)
    @DisplayName("COS-07 Upload Media on Selects offers Link; the 'Upload Link' dialog refuses an empty and an invalid URL")
    void linkDialogValidation(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.tapAdd(), "'+' should be tappable");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            List<String> opts = f.openUploadMedia();
            System.out.println(">>> COS-07 Selects upload options: " + opts);
            assertTrue(opts.contains("Link"), "Selects should offer Link (" + opts + ")");
            assertTrue(f.tapText("Link") && f.linkDialogShown(), "Link should open the link dialog");
            assertEquals("UPLOAD LINK", f.linkDialogHeader().trim().toUpperCase(), "Dialog header (upload_link)");
            assertFalse(f.linkDialogFieldTypeable(), "The URL field is paste-only (TYPE_NULL, not focusable)");
            f.linkDialogSubmit();
            assertTrue(f.errorShown(CostumeFormPage.ERR_URL_EMPTY, Duration.ofSeconds(4)) || f.linkDialogShown(),
                    "Empty URL should be refused (p_enter_url)");
            assertTrue(f.linkDialogShown(), "Dialog should stay open on an empty URL");
            f.linkDialogCancel();

            String pasted = f.pasteLinkInDialog("not a url zl_auto");
            assumeTrue(pasted != null, "Could not reopen the link dialog");
            f.linkDialogSubmit();
            assertTrue(f.linkDialogShown(), "An invalid URL ('" + pasted + "') should keep the dialog open (p_enter_valid_url)");
            f.linkDialogCancel();
            assertFalse(f.attachmentShown(), "Nothing should be attached after two refused links");
        } finally {
            if (f.linkDialogShown()) f.linkDialogCancel();
            leaveForm();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(8)
    @DisplayName("COS-08 Scene No. must start with a digit; duplicate episode numbers are refused")
    void sceneAndEpisodeValidation(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.tapAdd(), "'+' should be tappable");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            assumeTrue(f.addLink(LINK), "Could not attach a link (clipboard paste)");
            f.setField(CostumeFormPage.FIELD_SCENE, "A12");
            f.submit();
            sleep(1200);
            boolean sceneShown = f.errorShown(CostumeFormPage.ERR_SCENE_DIGIT, Duration.ofSeconds(3));
            System.out.println(">>> COS-08 scene error text on screen: " + sceneShown);
            assertFalse(p.isLoaded(Duration.ofSeconds(3)), "Scene 'A12' should be refused (txt_scene_no_validation) — the form closed and a record was created");

            f.setField(CostumeFormPage.FIELD_SCENE, SCENE);
            f.setField(CostumeFormPage.FIELD_EPISODE, episode(t) + "," + episode(t));
            f.submit();
            sleep(1200);
            assertFalse(p.isLoaded(Duration.ofSeconds(3)), "Duplicate episodes '" + episode(t) + "," + episode(t)
                    + "' should be refused (txt_episode_no_dublicate_validation) — the form closed and a record was created");
        } finally {
            leaveFormAndDropStray(t);
        }
    }

    /** If a validation case did create a record by mistake, it sits under the run episode / no character — delete it later in COS-30. */
    private void leaveFormAndDropStray(Tool t) {
        leaveForm();
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(9)
    @DisplayName("COS-09 Back from a filled form asks 'Are you sure you want to go back?' — No stays, Yes leaves")
    void backConfirm(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.tapAdd(), "'+' should be tappable");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            f.setField(CostumeFormPage.FIELD_DESC, PREFIX + " back " + run);
            p.back();
            assertTrue(p.dialogShown(Duration.ofSeconds(4)), "A confirmation should appear on Back");
            assertEquals(CostumeFormPage.MSG_BACK, p.dialogMessage().trim(), "Message (on_back_from_form_page)");
            p.dialogNo();
            sleep(800);
            assertTrue(f.isOpen(Duration.ofSeconds(3)), "'No' should keep the form");
            p.back();
            assertTrue(p.dialogShown(Duration.ofSeconds(4)), "Confirmation again");
            p.dialogYes();
            assertTrue(p.isLoaded(Duration.ofSeconds(8)), "'Yes' should return to the tool");
        } finally {
            leaveForm();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(10)
    @DisplayName("COS-10 Character picker from a Selects create: 'Casting' list with search and '+'; the temporary character form requires a name")
    void characterPickerFromSelects(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.tapAdd(), "'+' should be tappable");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            assertTrue(f.openCharacterPicker(), "The character '+' should open the 'Casting' picker (ChooseCastPage)");
            assertEquals(CostumePage.SEARCH_HINT, f.pickerSearchHint().trim(), "Picker search hint");
            System.out.println(">>> COS-10 picker (Casting Final of the matching Casting tool): " + f.pickerNames());
            assertTrue(f.pickerAddShown(), "From a Selects create the picker offers '+' (IS_SHOW_ADD_BUTTON)");
            assertTrue(f.openTempCharacterForm(), "'+' should open the character form");
            assertTrue(f.tempFormShown("characterName") && f.tempFormShown("talentName") && f.tempFormShown("gender"),
                    "Character Name, Talent Name and Gender are on the temporary character form");
            assertFalse(f.uploadMediaShown(), "The temporary character form hides Upload Media (HIDE_UPLOADING_ICON)");
            f.submit();
            assertTrue(f.errorShown("Please Enter Character Name", Duration.ofSeconds(4)) || f.tempFormShown("characterName"),
                    "An empty temporary character should be refused");
            assertTrue(f.tempFormShown("characterName"), "The temporary character form should stay open");
        } finally {
            leaveForm();
        }
    }

    // ═════════════════════════ create and browse ═════════════════════════

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(11)
    @DisplayName("COS-11 Create in Selects (link + temporary character + episode + scene + description): filed under Episode → Character → record")
    void createInSelects(Tool t) {
        String name = character(t, "L");
        String desc = PREFIX + " desc " + run;
        boolean ok = createRecord(t, CostumePage.TAB_SELECTS, "link", name, PREFIX + " Talent " + run, "Male",
                episode(t), SCENE, desc);
        assertTrue(ok, "The record should be created (form closed after Submit)");
        linkChar.put(t, name);
        description.put(t, desc);
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assertTrue(p.waitFolder(episodeRow(episode(t)), LIST_WAIT), "Selects should list '" + episodeRow(episode(t)) + "'");
        assertTrue(p.openFolder(episodeRow(episode(t))), "Episode row should open");
        assertTrue(p.waitFolder(characterRow(name), Duration.ofSeconds(15)), "Episode should list '" + characterRow(name) + "' — saw " + p.folderNames());
        String detail = p.folderDetail(characterRow(name));
        System.out.println(">>> COS-11 character row detail: " + detail.replace('\n', '|'));
        assertTrue(detail.toLowerCase().contains(("Talent Name: " + PREFIX + " talent " + run).toLowerCase()),
                "Row detail should carry the talent name: " + detail);
        assertTrue(detail.contains("Scene No. " + SCENE), "Row detail should carry the scene: " + detail);
        assertTrue(p.openFolder(characterRow(name)) && p.onRecordList(Duration.ofSeconds(10)), "Character row should open the records");
        String hdr = p.headerText();
        assertTrue(hdr.toLowerCase().contains(stored(name).toLowerCase()) && hdr.contains(SCENE),
                "Record page header should read 'Character: … / Scene No. …' — was '" + hdr + "'");
        assertTrue(p.waitRecord(desc, Duration.ofSeconds(15)), "The record should be listed with its description");
        String txt = p.recordText(p.record(desc));
        System.out.println(">>> COS-11 record: " + txt);
        assertTrue(txt.contains("Link: " + LINK), "Record shows 'Link: …': " + txt);
        assertTrue(txt.contains("Uploaded by"), "Record shows 'Uploaded by: …': " + txt);
        assertTrue(txt.contains("Description: " + desc), "Record shows 'Description: …': " + txt);
    }

    private CostumePage linkRecords(Tool t, String tab) {
        String name = linkChar.get(t);
        assumeTrue(name != null, "The Selects link record (COS-11) was not created");
        CostumePage p = openRecords(t, tab, episode(t), name, SCENE, LIST_WAIT);
        assumeTrue(p != null, "Could not reach the link record in " + tab);
        return p;
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(12)
    @DisplayName("COS-12 Record menu in Selects (own record, admin): View Details, Edit, Forward, Download, Image Reply, Move To Shortlist, Move To Final, Read By, Delete")
    void selectsRecordMenu(Tool t) {
        CostumePage p = linkRecords(t, CostumePage.TAB_SELECTS);
        List<String> items = p.openRecordMenu(description.get(t));
        p.dismissPopup();
        assertEquals(List.of("View Details", "Edit", "Forward", "Download", "Image Reply", "Move To Shortlist",
                "Move To Final", "Read By", "Delete"), items, "Selects record menu (common_lcw_menu minus chat)");
        assertTrue(p.recordChatIconShown(description.get(t)), "Selects records carry the chat icon");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(13)
    @DisplayName("COS-13 View Details opens 'Costume Details' read-only: fields disabled, no Upload Media, no button")
    void viewDetails(Tool t) {
        CostumePage p = linkRecords(t, CostumePage.TAB_SELECTS);
        assertTrue(p.recordMenu(description.get(t), "View Details"), "View Details should be offered");
        CostumeFormPage f = form();
        try {
            assertTrue(f.isOpen(Duration.ofSeconds(10)), "Details should open");
            assertEquals(CostumeFormPage.HDR_VIEW.toUpperCase(), f.header().trim().toUpperCase(), "Header (wardrobe_details)");
            assertEquals(SCENE, f.field(CostumeFormPage.FIELD_SCENE), "Scene shown");
            assertEquals(episode(t), f.field(CostumeFormPage.FIELD_EPISODE), "Episode shown");
            assertEquals(description.get(t), f.field(CostumeFormPage.FIELD_DESC), "Description shown");
            assertTrue(f.characterTitle().equalsIgnoreCase(stored(linkChar.get(t))), "Character shown: " + f.characterTitle());
            assertFalse(f.fieldEnabled(CostumeFormPage.FIELD_SCENE), "Scene is read-only");
            assertFalse(f.fieldEnabled(CostumeFormPage.FIELD_DESC), "Description is read-only");
            assertFalse(f.uploadMediaShown(), "No Upload Media in view mode");
            assertEquals("", f.submitLabel(), "No submit button in view mode");
        } finally {
            f.back();   // view mode finishes without the "go back" dialog
            if (page().dialogShown(Duration.ofSeconds(1))) page().dialogYes();
        }
        assertTrue(page().onRecordList(Duration.ofSeconds(6)), "Back from view should return to the records without a dialog");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(14)
    @DisplayName("COS-14 Edit: 'Update Costume Details' with Update; a new description is saved and listed")
    void editDescription(Tool t) {
        CostumePage p = linkRecords(t, CostumePage.TAB_SELECTS);
        assertTrue(p.recordMenu(description.get(t), "Edit"), "Edit should be offered");
        CostumeFormPage f = form();
        String edited = PREFIX + " edited " + run;
        try {
            assertTrue(f.isOpen(Duration.ofSeconds(10)), "Edit form should open");
            assertEquals(CostumeFormPage.HDR_UPDATE.toUpperCase(), f.header().trim().toUpperCase(), "Header (update_wardrobe_details)");
            assertEquals("Update", f.submitLabel().trim(), "Button reads Update");
            assertFalse(f.uploadMediaShown(), "No Upload Media while editing");
            assertTrue(f.setField(CostumeFormPage.FIELD_DESC, edited), "Description editable");
            f.submit();
            assertTrue(p.onRecordList(Duration.ofSeconds(15)), "Update should return to the records");
        } finally {
            if (f.isOpen(Duration.ofSeconds(1))) leaveForm();
        }
        assertTrue(p.waitRecord(edited, Duration.ofSeconds(30)), "The record should now show 'Description: " + edited + "'");
        description.put(t, edited);
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(15)
    @DisplayName("COS-15 Search: by character finds the episode, a nonsense term hides it, an empty search asks for a term")
    void search(Tool t) {
        assumeTrue(linkChar.get(t) != null, "No fixture (COS-11)");
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        try {
            p.search(stored(linkChar.get(t)));
            assertTrue(p.waitFolder(episodeRow(episode(t)), Duration.ofSeconds(8)),
                    "Searching the character should keep its episode row — saw " + p.folderNames());
            p.clearSearch();
            p.search("zz_no_such_costume_" + run);
            sleep(1500);
            assertFalse(p.hasFolder(episodeRow(episode(t))), "A nonsense search should not list the episode — saw " + p.folderNames());
            p.clearSearch();
            p.tapSearchIconEmpty();
            String msg = p.waitMessage(CostumePage.MSG_EMPTY_SEARCH, Duration.ofSeconds(5));
            assertEquals(CostumePage.MSG_EMPTY_SEARCH, msg.trim(), "Empty search message (p_enter_in_search_field)");
        } finally {
            p.clearSearch();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(16)
    @DisplayName("COS-16 Folder menus: the episode row refuses Move/Delete ('navigate to the next page'); the character row offers Move To Shortlist / Move To Final / Delete")
    void folderMenus(Tool t) {
        assumeTrue(linkChar.get(t) != null, "No fixture (COS-11)");
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assumeTrue(p.waitFolder(episodeRow(episode(t)), LIST_WAIT), "Episode row missing");
        List<String> epItems = p.openFolderMenu(episodeRow(episode(t)));
        System.out.println(">>> COS-16 episode row menu: " + epItems);
        assertEquals(List.of("Move To Shortlist", "Move To Final", "Delete"), epItems, "Episode row menu (Selects, folder_options_casting)");
        p.tapPopupItem("Move To Shortlist");
        String toast = p.waitMessage("next page", Duration.ofSeconds(4));
        System.out.println(">>> COS-16 move toast: '" + toast + "'");
        assertFalse(form().isOpen(Duration.ofSeconds(2)), "Move from an episode row must not open the move form");
        if (!toast.isEmpty()) assertEquals(CostumePage.MSG_MOVE_NEXT_PAGE, toast.trim(), "Toast (p_move_from_next_page)");
        p.openFolderMenu(episodeRow(episode(t)));
        p.tapPopupItem("Delete");
        assertFalse(p.dialogShown(Duration.ofSeconds(2)), "Delete on an episode row must not ask to delete (p_delete_from_next_page)");
        assertTrue(p.hasFolder(episodeRow(episode(t))), "The episode row is still there");

        assertTrue(p.openFolder(episodeRow(episode(t))), "Episode row should open");
        List<String> chItems = p.openFolderMenu(characterRow(linkChar.get(t)));
        p.dismissPopup();
        assertEquals(List.of("Move To Shortlist", "Move To Final", "Delete"), chItems, "Character row menu in Selects");
        assertEquals(CostumePage.SEARCH_HINT_INNER, p.searchHint().trim(), "Character page search hint (wardrobe_sc_search_hint)");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(17)
    @DisplayName("COS-17 Group by Character and by Scene No relabels the rows; Reset goes back to Episode")
    void groupBy(Tool t) {
        assumeTrue(linkChar.get(t) != null, "No fixture (COS-11)");
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        try {
            assertTrue(p.groupAndFilter("Character", null), "Group by Character should apply");
            assertTrue(p.waitFolder(characterRow(linkChar.get(t)), Duration.ofSeconds(10)),
                    "Grouped by character, '" + characterRow(linkChar.get(t)) + "' should be a first-level row — saw " + p.folderNames());
            assertTrue(p.groupAndFilter("Scene No", null), "Group by Scene No should apply");
            assertTrue(p.waitFolder("Scene No. " + SCENE, Duration.ofSeconds(10)), "Grouped by scene, 'Scene No. " + SCENE + "' should be listed");
            assertTrue(p.folderDetail("Scene No. " + SCENE).toLowerCase().contains(stored(linkChar.get(t)).toLowerCase()),
                    "The scene row's detail should name the character: " + p.folderDetail("Scene No. " + SCENE));
            assertTrue(p.openFilter() && p.resetFilter(), "Reset should apply");
            assertTrue(p.waitFolder(episodeRow(episode(t)), Duration.ofSeconds(10)), "Reset should return to Episode rows");
        } finally {
            if (!p.hasFolder(episodeRow(episode(t))) && p.openFilter()) p.resetFilter();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(18)
    @DisplayName("COS-18 Filter by gender: the Male character shows under Male and not under Female")
    void genderFilter(Tool t) {
        assumeTrue(linkChar.get(t) != null, "No fixture (COS-11)");
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        try {
            assertTrue(p.groupAndFilter(null, "Male"), "Filter Male should apply");
            assertTrue(p.waitFolder(episodeRow(episode(t)), Duration.ofSeconds(8)), "Male: the episode should be listed");
            assertTrue(p.groupAndFilter(null, "Female"), "Filter Female should apply");
            sleep(1500);
            assertFalse(p.hasFolder(episodeRow(episode(t))), "Female: the (male-only) episode should not be listed");
        } finally {
            if (p.openFilter()) p.resetFilter();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(19)
    @DisplayName("COS-19 Select mode on the records: Move To Shortlist / Move To Final / Cancel / Delete; nothing ticked is refused")
    void selectionMode(Tool t) {
        CostumePage p = linkRecords(t, CostumePage.TAB_SELECTS);
        assertTrue(p.startSelection(), "Toolbar 'Select' should show the action bar");
        try {
            List<String> b = p.selectionButtons();
            System.out.println(">>> COS-19 selection bar: " + b);
            assertTrue(b.contains("Move To Shortlist") && b.contains("Move To Final"), "Move buttons in Selects: " + b);
            assertFalse(p.shown("takeSceneShots", Duration.ofMillis(500)), "Add buttons hide in select mode");
            p.tapSelectionButton("btnMoveTo");
            assertEquals(CostumePage.MSG_NONE_CHOSEN, p.waitMessage("at least one", Duration.ofSeconds(4)).trim(),
                    "Move with nothing ticked (p_choose_at_list_one_user_from_the_list)");
            // Let the first snackbar go, or the second one queues behind it.
            for (int i = 0; i < 20 && !p.snackbarOrToast().isEmpty(); i++) sleep(500);
            p.tapSelectionButton("btnDelete");
            assertEquals(CostumePage.MSG_KEEP_ONE, p.waitMessage("at least one item", Duration.ofSeconds(8)).trim(),
                    "Delete with nothing ticked (please_select_atleast_one_item_from_list)");
        } finally {
            // The snackbar sits over the action bar; let it go before tapping Cancel.
            for (int i = 0; i < 20 && !p.snackbarOrToast().isEmpty(); i++) sleep(500);
            p.tapSelectionButton("btnCancel");
            sleep(1000);
        }
        assertFalse(p.selectionBarShown(), "Cancel should close the action bar");
    }

    // ═════════════════════════ Shortlist / Final ═════════════════════════

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(20)
    @DisplayName("COS-20 Shortlist create needs an episode, a character and a scene; its picker has no '+' (temporary characters not allowed)")
    void shortlistCreateValidation(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SHORTLIST);
        assertTrue(p.tapAdd(), "'+' should be tappable in Shortlist");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            assumeTrue(f.addLink(LINK), "Could not attach a link");
            f.submit();
            sleep(1000);
            assertTrue(f.isOpen(Duration.ofSeconds(2)), "No episode: the form must stay open (txt_episode_no_validation)");
            f.setField(CostumeFormPage.FIELD_EPISODE, episode(t));
            f.submit();
            String s = f.waitSnackbar("Character", Duration.ofSeconds(5));
            assertEquals(CostumeFormPage.MSG_SELECT_CHARACTER, s.trim(), "No character (p_select_character)");
            // The snackbar's OK opens the picker; wait it out instead.
            sleep(4500);
            if (f.pickerOpen(Duration.ofSeconds(1))) f.back();
            assertTrue(f.openCharacterPicker(), "Picker should open");
            assertFalse(f.pickerAddShown(), "From Shortlist the picker must not offer '+' (temporary characters)");
            List<String> names = f.pickerNames();
            System.out.println(">>> COS-20 Shortlist picker (Casting Final): " + names);
            f.back();
        } finally {
            leaveForm();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(21)
    @DisplayName("COS-21 Final's Upload Media offers no Link (media only)")
    void finalHasNoLink(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_FINAL);
        assertTrue(p.tapAdd(), "'+' should be tappable in Final");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            List<String> opts = f.openUploadMedia();
            System.out.println(">>> COS-21 Final upload options: " + opts);
            assumeTrue(!opts.isEmpty(), "The media sheet did not open");
            assertFalse(opts.contains("Link"), "Final must not offer Link: " + opts);
            f.back();
        } finally {
            leaveForm();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(22)
    @DisplayName("COS-22 A record whose character was typed in (temporary) cannot leave Selects: the move is refused and nothing reaches Shortlist")
    void tempCharacterCannotLeaveSelects(Tool t) {
        String moveHeader = "";
        CostumePage p = linkRecords(t, CostumePage.TAB_SELECTS);
        assertTrue(p.recordMenu(description.get(t), "Move To Shortlist"), "Move To Shortlist should be offered");
        CostumeFormPage f = form();
        String s;
        try {
            assertTrue(f.isOpen(Duration.ofSeconds(10)), "Move form should open");
            moveHeader = f.header().trim();
            assertEquals("Move", f.submitLabel().trim(), "Button reads Move");
            assertTrue(f.characterTitle().equalsIgnoreCase(stored(linkChar.get(t))), "Character prefilled: " + f.characterTitle());
            assertEquals(SCENE, f.field(CostumeFormPage.FIELD_SCENE), "Scene prefilled");
            assertEquals(episode(t), f.field(CostumeFormPage.FIELD_EPISODE), "Episode prefilled");
            f.submit();
            s = f.waitSnackbar(null, Duration.ofSeconds(10));
            System.out.println(">>> COS-22 move answer: '" + s + "'");
        } finally {
            sleep(2000);
            if (f.isOpen(Duration.ofSeconds(1))) leaveForm();
        }
        assertTrue(s.contains("created via the Costume module are not allowed"),
                "The server should refuse a temporary (Costume-made) character outside Selects — got '" + s + "'");
        CostumePage sl = openRecords(t, CostumePage.TAB_SHORTLIST, episode(t), linkChar.get(t), SCENE, Duration.ofSeconds(10));
        assertTrue(sl == null || sl.record(description.get(t)) == null, "The refused record must not appear in Shortlist");
        assertEquals(CostumeFormPage.HDR_MOVE.toUpperCase(), moveHeader.toUpperCase(),
                "COS-BUG-2: the record-level move form is titled '" + moveHeader + "' instead of 'Move Costume' —"
                        + " AddEditWardrobePage.handleMoveData sets move_wardrobe only for MOVE_DATA, not for the \"IDS\" (record menu) branch");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(23)
    @DisplayName("COS-23 A cast made in Casting → Final (ZL_AUTO, Non-Binary, photo) is offered by the Costume character picker")
    void castingFixtureInPicker(Tool t) {
        assumeTrue(onEmulator(), "Needs the pushed test photo (Casting Final refuses links) — emulator only, never the Pixel's gallery");
        assumeTrue(pushTestPhoto(), "The generated test PNG could not be pushed into the media store");
        String name = character(t, "C");
        boolean ok = createCastingFixture(t, name, "Non-Binary", episode(t));
        assumeTrue(ok, "The ZL_AUTO cast could not be created in " + castingTile(t) + " → Final");
        castChar.put(t, name);
        CostumePage p = openTab(t, CostumePage.TAB_SHORTLIST);
        assertTrue(p.tapAdd(), "'+' in Shortlist");
        CostumeFormPage f = form();
        try {
            assumeTrue(f.isOpen(Duration.ofSeconds(10)), "Add form did not open");
            assertTrue(f.openCharacterPicker(), "Picker should open");
            long end = System.currentTimeMillis() + 60_000;
            boolean listed = false;
            while (!listed && System.currentTimeMillis() < end) {
                f.pickerSearch(stored(name));
                listed = f.pickerNames().stream().anyMatch(n -> n.toLowerCase().contains(stored(name).toLowerCase()));
                if (!listed) { f.back(); sleep(4000); f.openCharacterPicker(); }
            }
            assertTrue(listed, "The new Casting Final cast should be offered by " + t + "'s picker — saw " + f.pickerNames());
        } finally {
            leaveForm();
        }
    }

    private boolean onEmulator() {
        String udid = String.valueOf(com.zillit.automation.core.Config.get("zillit.udid"));
        if ("null".equals(udid)) udid = String.valueOf(System.getenv("ZILLIT_UDID"));
        return udid.startsWith("emulator");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(24)
    @DisplayName("COS-24 Create in Shortlist with a Casting character and a link; Shortlist record menu and chat icon")
    void shortlistCreateWithCast(Tool t) {
        String name = castChar.get(t);
        assumeTrue(name != null, "No Casting fixture (COS-23)");
        String desc = PREFIX + " sl " + run;
        boolean ok = createRecord(t, CostumePage.TAB_SHORTLIST, "link", name, null, null, episode(t), "903", desc, true);
        assertTrue(ok, "A Shortlist record with a Casting character, episode and scene should be created");
        CostumePage p = openRecords(t, CostumePage.TAB_SHORTLIST, episode(t), name, "903", LIST_WAIT);
        assertNotNull(p, "Shortlist should list Episode → '" + characterRow(name) + "'");
        assertTrue(p.waitRecord(desc, Duration.ofSeconds(15)), "The Shortlist record should be listed");
        shortlistDesc.put(t, desc);
        List<String> items = p.openRecordMenu(desc);
        p.dismissPopup();
        assertEquals(List.of("View Details", "Edit", "Forward", "Download", "Image Reply", "Move To Final", "Read By", "Delete"),
                items, "Shortlist record menu");
        assertTrue(p.recordChatIconShown(desc), "Shortlist records carry the chat icon");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(25)
    @DisplayName("COS-25 A link-only record cannot go to Final: 'Costume Attachment Required.'")
    void linkCannotGoFinal(Tool t) {
        String desc = shortlistDesc.get(t);
        assumeTrue(desc != null, "No Shortlist record (COS-24)");
        CostumePage p = openRecords(t, CostumePage.TAB_SHORTLIST, episode(t), castChar.get(t), "903", LIST_WAIT);
        assumeTrue(p != null && p.waitRecord(desc, Duration.ofSeconds(10)), "Shortlist record not reachable");
        assertTrue(p.recordMenu(desc, "Move To Final"), "Move To Final should be offered");
        String s = p.waitMessage("Attachment", Duration.ofSeconds(5));
        boolean formOpened = form().isOpen(Duration.ofSeconds(2));
        if (formOpened) leaveForm();
        assertFalse(formOpened, "The move form must not open for a link-only record");
        assertEquals(CostumePage.MSG_ATTACHMENT, s.trim(), "Snackbar (wardrobe_attachment_requred)");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(26)
    @DisplayName("COS-26 A Selects photo record with the Non-Binary Casting character is listed under the 'Non Binary' filter")
    void photoRecordAndNonBinaryFilter(Tool t) {
        String name = castChar.get(t);
        assumeTrue(name != null, "No Casting fixture (COS-23)");
        boolean ok = createRecord(t, CostumePage.TAB_SELECTS, "photo", name, null, null, episode(t), "902", null, true);
        assumeTrue(ok, "The photo record could not be created (gallery album '" + ALBUM + "')");
        photoChar.put(t, name);
        CostumePage rec = openRecords(t, CostumePage.TAB_SELECTS, episode(t), name, "902", LIST_WAIT);
        assertNotNull(rec, "Selects should list the photo record under Episode → '" + characterRow(name) + "'");
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        p.openFolder(episodeRow(episode(t)));
        System.out.println(">>> COS-26 non-binary row detail: " + p.folderDetail(characterRow(name)).replace('\n', '|'));
        p = openTab(t, CostumePage.TAB_SELECTS);
        try {
            assertTrue(p.groupAndFilter("Character", "Non Binary"), "Filter Non Binary should apply");
            boolean listed = p.waitFolder(characterRow(name), Duration.ofSeconds(8));
            assertTrue(listed, "COS-BUG-1: a Non-Binary character is missing under 'Non Binary' — Non-Binary is stored as"
                    + " 'others' (Constants.OTHERS, CastingDetailPage.hitApi) but the filter compares with R.string.other"
                    + " ('Other') in WardrobeVM.doFolderGrouping. Rows seen: " + p.folderNames());
        } finally {
            if (p.openFilter()) p.resetFilter();
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(27)
    @DisplayName("COS-27 Move the photo record Selects → Final; Final's record menu is View Details / Forward / Download / Read By / Delete, with no chat")
    void photoToFinal(Tool t) {
        String name = photoChar.get(t);
        assumeTrue(name != null, "No photo record (COS-26)");
        CostumePage p = openRecords(t, CostumePage.TAB_SELECTS, episode(t), name, "902", LIST_WAIT);
        String marker = "Uploaded by";
        assumeTrue(p != null && p.waitRecord(marker, Duration.ofSeconds(20)), "Photo record not reachable in Selects");
        String moveHeader = "";
        assertTrue(p.recordMenu(marker, "Move To Final"), "Move To Final should be offered for a photo record");
        CostumeFormPage f = form();
        try {
            assertTrue(f.isOpen(Duration.ofSeconds(10)), "The move form should open for a record with media");
            moveHeader = f.header().trim();
            assertEquals("Move", f.submitLabel().trim(), "Button reads Move");
            f.submit();
            System.out.println(">>> COS-27 move answer: '" + f.waitSnackbar(null, Duration.ofSeconds(10)) + "'");
        } finally {
            sleep(2000);
            if (f.isOpen(Duration.ofSeconds(1))) leaveForm();
        }
        CostumePage fin = openRecords(t, CostumePage.TAB_FINAL, episode(t), name, "902", LIST_WAIT);
        assertNotNull(fin, "Final should list Episode → Character for the photo record");
        assertTrue(fin.waitRecord(marker, Duration.ofSeconds(15)), "The photo record should be in Final");
        movedToFinal.put(t, true);
        List<String> items = fin.openRecordMenu(marker);
        fin.dismissPopup();
        assertEquals(List.of("View Details", "Forward", "Download", "Read By", "Delete"), items, "Final record menu");
        assertFalse(fin.recordChatIconShown(marker), "Final records have no chat icon (WardrobeItemHolder: status != FINAL)");
        assertEquals(CostumeFormPage.HDR_MOVE.toUpperCase(), moveHeader.toUpperCase(),
                "COS-BUG-2: the record-level move form is titled '" + moveHeader + "' instead of 'Move Costume'");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(28)
    @DisplayName("COS-28 Final folder menus offer only Delete")
    void finalFolderMenu(Tool t) {
        assumeTrue(Boolean.TRUE.equals(movedToFinal.get(t)), "Nothing in Final (COS-27)");
        CostumePage p = openTab(t, CostumePage.TAB_FINAL);
        assumeTrue(p.waitFolder(episodeRow(episode(t)), LIST_WAIT), "Final has no run episode");
        List<String> ep = p.openFolderMenu(episodeRow(episode(t)));
        p.dismissPopup();
        assertEquals(List.of("Delete"), ep, "Final episode row menu");
        assertTrue(p.openFolder(episodeRow(episode(t))), "Episode opens");
        List<String> ch = p.openFolderMenu(characterRow(photoChar.get(t)));
        p.dismissPopup();
        assertEquals(List.of("Delete"), ch, "Final character row menu");
    }

    // ═════════════════════════ chats ═════════════════════════

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(29)
    @DisplayName("COS-29 Selects unit chat: header '<tool>: Selects', no attachment button, a message posts and can be deleted")
    void unitChat(Tool t) {
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assumeTrue(p.unitChatShown(), "No unit chat icon on Selects");
        assertTrue(p.openUnitChat(), "The unit chat should open");
        CostumeChat c = new CostumeChat(driver);
        String msg = PREFIX + " unit chat " + run + t.name().charAt(0);
        boolean sent = false;
        try {
            String hdr = p.headerText();
            assertTrue(hdr.toUpperCase().contains(t.label.toUpperCase()) && hdr.toUpperCase().contains("SELECTS"), "Header should be '" + t.label + ": Selects' — was '" + hdr + "'");
            assertFalse(c.attachmentShown(), "Costume chats hide the attachment button (OtherChatsActivity: isShowAttachment(false))");
            assumeTrue(c.inputShown(), "No message input — no posting rights");
            c.send(msg);
            sent = c.waitMessage(msg, Duration.ofSeconds(20));
            assertTrue(sent, "The message should appear in the unit chat");
            assertTrue(c.deleteOwn(msg), "The own message should be deletable");
            sent = !c.waitMessageGone(msg, Duration.ofSeconds(15));
            assertFalse(sent, "The deleted message should leave the chat");
        } finally {
            if (sent) c.deleteOwn(msg);
            backToToolHome(t);
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(30)
    @DisplayName("COS-30 Record chat (chat icon on a Selects record): opens the record's chat; a message posts and can be deleted")
    void recordChat(Tool t) {
        CostumePage p = openRecords(t, CostumePage.TAB_SELECTS, episode(t), linkChar.get(t), SCENE, LIST_WAIT);
        if (p == null || p.record(description.get(t)) == null) {
            // The move may not keep a Selects copy; use the Shortlist one.
            p = openRecords(t, CostumePage.TAB_SHORTLIST, episode(t), linkChar.get(t), SCENE, Duration.ofSeconds(20));
        }
        assumeTrue(p != null && p.record(description.get(t)) != null, "No link record to chat on");
        assertTrue(p.tapRecordChat(description.get(t)), "The record chat icon should be tappable");
        CostumeChat c = new CostumeChat(driver);
        String msg = PREFIX + " record chat " + run + t.name().charAt(0);
        boolean sent = false;
        try {
            assertTrue(c.isOpen(Duration.ofSeconds(15)), "The record chat should open");
            assumeTrue(c.inputShown(), "No message input — no posting rights");
            c.send(msg);
            sent = c.waitMessage(msg, Duration.ofSeconds(20));
            assertTrue(sent, "The message should appear in the record chat");
            assertTrue(c.deleteOwn(msg), "The own message should be deletable");
            sent = !c.waitMessageGone(msg, Duration.ofSeconds(15));
            assertFalse(sent, "The deleted message should leave the chat");
        } finally {
            if (sent) c.deleteOwn(msg);
            backToToolHome(t);
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(31)
    @DisplayName("COS-31 Forward opens the member picker (nothing sent); Read By opens the read-by list")
    void forwardAndReadBy(Tool t) {
        CostumePage p = openRecords(t, CostumePage.TAB_SELECTS, episode(t), linkChar.get(t), SCENE, LIST_WAIT);
        assumeTrue(p != null && p.waitRecord(description.get(t), Duration.ofSeconds(15)), "No link record in Selects");
        assertTrue(p.recordMenu(description.get(t), "Forward"), "Forward should be offered");
        sleep(2500);
        boolean picker = !p.onRecordList(Duration.ofSeconds(1));
        System.out.println(">>> COS-31 forward opened: " + resumedActivityName());
        p.back();
        assertTrue(picker, "Forward should open the member picker (UsersListPage)");
        assertTrue(p.onRecordList(Duration.ofSeconds(6)), "Back returns to the records with nothing sent");
        assertTrue(p.recordMenu(description.get(t), "Read By"), "Read By should be offered");
        sleep(2500);
        boolean readBy = !p.onRecordList(Duration.ofSeconds(1));
        System.out.println(">>> COS-31 read by opened: " + resumedActivityName());
        p.back();
        assertTrue(readBy, "Read By should open the read-by list");
    }

    private String resumedActivityName() {
        String s = adb("shell", "dumpsys", "activity", "activities");
        for (String line : s.split("\n")) if (line.contains("ResumedActivity")) return line.trim();
        return "?";
    }

    // ═════════════════════════ folder edit (no character) ═════════════════════════

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(32)
    @DisplayName("COS-32 A Selects record with no character and no scene files under 'Character: Not Assigned'; folder Edit fills the scene")
    void folderEditFillsScene(Tool t) {
        boolean ok = createRecord(t, CostumePage.TAB_SELECTS, "link", null, null, null, episode(t), null, PREFIX + " nochar " + run);
        assumeTrue(ok, "The character-less record could not be created");
        CostumePage p = openTab(t, CostumePage.TAB_SELECTS);
        assumeTrue(p.waitFolder(episodeRow(episode(t)), LIST_WAIT) && p.openFolder(episodeRow(episode(t))), "Run episode missing");
        sleep(1500);
        String unknown = null;
        for (String n : p.folderNames()) if (n.startsWith("Character:") && !n.toLowerCase().contains("zl_auto")) unknown = n;
        System.out.println(">>> COS-32 character rows: " + p.folderNames());
        assertNotNull(unknown, "A character-less record should get its own 'Character: …' row — saw " + p.folderNames());
        assertEquals("Character: Not Assigned", unknown, "Label for no character (unknown_label)");
        List<String> items = p.openFolderMenu(unknown);
        System.out.println(">>> COS-32 unknown row menu: " + items);
        assertTrue(items.contains("Edit"), "A folder without a character offers Edit: " + items);
        p.tapPopupItem("Edit");
        assertTrue(p.hierarchySheetShown(Duration.ofSeconds(6)), "Edit should open the update sheet");
        try {
            assertEquals("", p.levelText(2) == null ? "(hidden)" : p.levelText(2).replace("Scene Number", "").trim(),
                    "Scene Number should be empty and shown");
            assertTrue(p.setLevel(2, "905"), "Scene Number should be editable");
            p.tapHierarchyUpdate();
            String s = p.waitMessage(null, Duration.ofSeconds(8));
            System.out.println(">>> COS-32 update message: '" + s + "'");
        } finally {
            if (p.hierarchySheetShown(Duration.ofSeconds(1))) p.tapHierarchyCancel();
        }
        sleep(2500);
        CostumePage again = openTab(t, CostumePage.TAB_SELECTS);
        again.openFolder(episodeRow(episode(t)));
        sleep(1500);
        String d = again.folderDetail(unknown);
        assertTrue(d.contains("Scene No. 905"), "The folder should now carry Scene No. 905: " + d.replace('\n', '|'));
    }

    // ═════════════════════════ deletes and cleanup ═════════════════════════

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(40)
    @DisplayName("COS-40 Delete a record from its menu: 'Are you sure you want to delete?' → Yes removes it")
    void deleteRecord(Tool t) {
        String desc = shortlistDesc.get(t) != null ? shortlistDesc.get(t) : description.get(t);
        CostumePage p = shortlistDesc.get(t) != null
                ? openRecords(t, CostumePage.TAB_SHORTLIST, episode(t), castChar.get(t), "903", Duration.ofSeconds(20))
                : openRecords(t, CostumePage.TAB_SELECTS, episode(t), linkChar.get(t), SCENE, LIST_WAIT);
        assumeTrue(p != null && p.record(desc) != null, "No record of this run to delete");
        assertTrue(p.recordMenu(desc, "Delete"), "Delete should be offered");
        assertTrue(p.dialogShown(Duration.ofSeconds(5)), "A confirmation should appear");
        assertEquals(CostumePage.MSG_DELETE_CONFIRM, p.dialogMessage().trim(), "Message (are_you_sure_you_want_to_delete)");
        p.dialogYes();
        assertTrue(p.waitRecordGone(desc, Duration.ofSeconds(20)), "The record should leave the list");
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(41)
    @DisplayName("COS-41 Delete the run's character folders in every tab (admin) — the run episode disappears")
    void deleteRunFolders(Tool t) {
        int n = 0;
        for (String tab : CostumePage.TABS) n += deleteRunFolders(t, tab, episode(t));
        System.out.println(">>> COS-41 " + t + ": deleted " + n + " folder(s)");
        for (String tab : CostumePage.TABS) {
            CostumePage p = openTab(t, tab);
            sleep(1500);
            assertFalse(p.hasFolder(episodeRow(episode(t))), tab + " still lists " + episodeRow(episode(t)) + " after deleting its folders");
        }
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("tools")
    @Order(42)
    @DisplayName("COS-42 Delete this run's ZL_AUTO cast from Casting (all tabs)")
    void deleteCastingFixture(Tool t) {
        String name = castChar.get(t);
        assumeTrue(name != null, "No Casting fixture was made this run");
        int n = deleteCastingFixture(t, name, episode(t));
        System.out.println(">>> COS-42 " + t + ": deleted " + n + " Casting folder(s)");
        assertTrue(n > 0, "The ZL_AUTO cast '" + stored(name) + "' should be deletable from " + castingTile(t));
        castChar.remove(t);
    }

    @Test
    @Order(99)
    @DisplayName("COS-99 Sweep: no 'Zl_auto_cos' folder left in either Costume tool or in Casting; the pushed test album is removed")
    void sweep() {
        int n = 0;
        try {
            for (Tool t : tools())
                for (String tab : CostumePage.TABS) n += sweepOldFolders(t, tab);
            for (Map.Entry<Tool, String> c : new java.util.ArrayList<>(castChar.entrySet()))
                n += deleteCastingFixture(c.getKey(), c.getValue(), episode(c.getKey()));
            for (Tool t : tools())
                for (String tab : CostumePage.TABS) n += sweepCasting(t, tab);
        } finally {
            removeTestPhotos();
        }
        System.out.println(">>> COS-99 swept " + n + " leftover folder(s)");
    }
}
