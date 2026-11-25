package com.zillit.automation.tests.casting;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestEmails;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.casting.CastingPage;
import com.zillit.automation.pages.casting.CastingPage.Folder;
import com.zillit.automation.pages.casting.CastingPage.Tool;
import com.zillit.automation.pages.homechat.HomeChatPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Casting (Main Cast) and Casting (Background Cast), single device — the full case list in
 * {@code CASTING_TESTCASES.md}. Every case runs once per tool: Main as {@code @Order 1–49},
 * Background as {@code @Order 51–99}, sharing one implementation per case.
 *
 * <p><b>Fixture.</b> One cast per tool, created in Selects with a LINK attachment (create
 * refuses a cast with no media or link, and a link needs no photo from the device): episode
 * {@code E} (the project is a TV project, so the home groups by episode), character
 * "ZL_AUTO_Char M12345", talent "ZL_AUTO_Talent M12345", gender Male, a character number and a
 * description. It is then moved Selects → Shortlist → Final through the folder menu, a Final
 * Cast Report is generated, and the cast is deleted from every tab it ended up in. The
 * Non-Binary case adds a second cast (Main only). Everything is removed by the last case of
 * each tool and again by {@link #sweep()} if a case died first; nothing else is touched.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5558 ./run-and-report.sh --only CastingTest
 * </pre>
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.CASTING)
class CastingTest extends CastingBaseTest {

    private static final Duration LIST_WAIT = Duration.ofSeconds(40);

    /** The cast a tool's run creates, and the tabs it currently sits in. */
    static final class Fx {
        final Tool tool;
        final String n;
        final String character, talent, episode, link, desc, charNo;
        final Set<String> tabs = new LinkedHashSet<>();
        String nb;            // the Non-Binary cast's character, when created
        String movedLeftCopy; // observation: does a move leave a copy behind?
        boolean attempted;    // a create was submitted (the sweep looks for it even if never seen listed)
        /** The PHOTO cast that carries the Final cases (a link-only cast cannot reach Final). */
        final String pic;
        final Set<String> picTabs = new LinkedHashSet<>();
        boolean picAttempted;

        Fx(Tool tool) {
            this.tool = tool;
            String l = tool == Tool.MAIN ? "M" : "B";
            this.n = l + (System.currentTimeMillis() % 100000);
            this.character = "ZL_AUTO_Char " + n;
            this.talent = "ZL_AUTO_Talent " + n;
            this.episode = String.valueOf(700 + (int) (System.currentTimeMillis() % 200) + (tool == Tool.MAIN ? 0 : 1));
            this.link = "https://example.com/zl-auto-cast-" + n.toLowerCase();
            this.desc = "ZL_AUTO cast note " + n;
            this.charNo = String.valueOf(10000 + (int) (System.currentTimeMillis() % 80000) + (tool == Tool.MAIN ? 0 : 7));
            this.pic = "ZL_AUTO_Pic " + n;
        }

        boolean created() { return !tabs.isEmpty(); }
    }

    private final Map<Tool, Fx> fx = new EnumMap<>(Tool.class);

    private Fx fx(Tool t) { return fx.computeIfAbsent(t, Fx::new); }

    // ═════════════════════════════ MAIN CAST (1–49) ═════════════════════════════

    @Test @Order(1)  @DisplayName("[CAST-01] Main: tile listed in Tools and opens its own home")
    void m01() { tileOpensHome(Tool.MAIN); }
    @Test @Order(2)  @DisplayName("[CAST-02] Main: home shows Selects/Shortlist/Final, search, filter, add buttons")
    void m02() { homeLayout(Tool.MAIN); }
    @Test @Order(3)  @DisplayName("[CAST-03] Main: unit chat icon on Selects and Shortlist, hidden on Final")
    void m03() { unitChatIconPerTab(Tool.MAIN); }
    @Test @Order(4)  @DisplayName("[CAST-04] Main: unit chat opens per tab, has no attachment button, sends text")
    void m04() { unitChat(Tool.MAIN); }
    @Test @Order(5)  @DisplayName("[CAST-05] Main: filter sheet offers Group By and gender Filter By with defaults")
    void m05() { filterSheet(Tool.MAIN); }
    @Test @Order(6)  @DisplayName("[CAST-06] Main: searching with an empty field is refused")
    void m06() { emptySearch(Tool.MAIN); }
    @Test @Order(7)  @DisplayName("[CAST-07] Main: Add Cast Details form layout")
    void m07() { addFormLayout(Tool.MAIN); }
    @Test @Order(8)  @DisplayName("[CAST-08] Main: submit without media or link is refused (Cast Attachment Required)")
    void m08() { attachmentRequired(Tool.MAIN); }
    @Test @Order(9)  @DisplayName("[CAST-09] Main: Upload Media offers Link on Selects but not on Final")
    void m09() { uploadMediaOptions(Tool.MAIN); }
    @Test @Order(10) @DisplayName("[CAST-10] Main: Upload Link dialog refuses an empty and an invalid URL")
    void m10() { linkDialogValidation(Tool.MAIN); }
    @Test @Order(11) @DisplayName("[CAST-11] Main: gender options; character number is 5 digits max and needs a character")
    void m11() { genderAndNumber(Tool.MAIN); }
    @Test @Order(12) @DisplayName("[CAST-12] Main: Contact Info validations, add and remove a contact")
    void m12() { contactInfo(Tool.MAIN); }
    @Test @Order(13) @DisplayName("[CAST-13] Main: Shortlist add form requires episode, character, talent and gender")
    void m13() { shortlistAddRequiresFields(Tool.MAIN); }
    @Test @Order(14) @DisplayName("[CAST-14] Main: create a cast in Selects (duplicate episodes refused first)")
    void m14() { createCast(Tool.MAIN); }
    @Test @Order(15) @DisplayName("[CAST-15] Main: episode folder drills down to character folder and cast card")
    void m15() { episodeDrillDown(Tool.MAIN); }
    @Test @Order(16) @DisplayName("[CAST-16] Main: Group By Character and Talent Name show the cast's folder")
    void m16() { groupings(Tool.MAIN); }
    @Test @Order(17) @DisplayName("[CAST-17] Main: search by talent/character (case-insensitive), no match, clear")
    void m17() { search(Tool.MAIN); }
    @Test @Order(18) @DisplayName("[CAST-18] Main: gender filter Male shows the cast, Female hides it")
    void m18() { genderFilter(Tool.MAIN); }
    @Test @Order(19) @DisplayName("[CAST-19] Main: Reset in the filter sheet restores the default grouping")
    void m19() { resetFilter(Tool.MAIN); }
    @Test @Order(20) @DisplayName("[CAST-20] Main: cast card shows character, talent, link, description, uploader, chat")
    void m20() { cardContent(Tool.MAIN); }
    @Test @Order(21) @DisplayName("[CAST-21] Main: cast card shows the cast's gender")
    void m21() { cardGender(Tool.MAIN); }
    @Test @Order(22) @DisplayName("[CAST-22] Main: own Selects card menu items")
    void m22() { cardMenuSelects(Tool.MAIN); }
    @Test @Order(23) @DisplayName("[CAST-23] Main: View Details is read-only and shows the saved values")
    void m23() { viewDetails(Tool.MAIN); }
    @Test @Order(24) @DisplayName("[CAST-24] Main: Edit refuses two episodes, saves a new description")
    void m24() { editCast(Tool.MAIN); }
    @Test @Order(25) @DisplayName("[CAST-25] Main: Select mode bar, Delete with nothing selected, Cancel")
    void m25() { selectMode(Tool.MAIN); }
    @Test @Order(26) @DisplayName("[CAST-26] Main: cast's own chat opens from the card and sends text")
    void m26() { cardChat(Tool.MAIN); }
    @Test @Order(27) @DisplayName("[CAST-27] Main: Selects folder menu offers moves and Delete only")
    void m27() { folderMenuSelects(Tool.MAIN); }
    @Test @Order(28) @DisplayName("[CAST-28] Main: Move To Shortlist from the folder menu")
    void m28() { moveToShortlist(Tool.MAIN); }
    @Test @Order(29) @DisplayName("[CAST-29] Main: Shortlist folder and card menus")
    void m29() { shortlistMenus(Tool.MAIN); }
    @Test @Order(30) @DisplayName("[CAST-30] Main: a link-only cast cannot be moved to Final from its card")
    void m30() { cardMoveFinalNeedsMedia(Tool.MAIN); }
    @Test @Order(31) @DisplayName("[CAST-31] Main: Move To Final refuses a link-only cast; a photo cast moves to Final")
    void m31() { moveToFinal(Tool.MAIN); }
    @Test @Order(32) @DisplayName("[CAST-32] Main: Final folder menu and read-only Final card")
    void m32() { finalMenus(Tool.MAIN); }
    @Test @Order(33) @DisplayName("[CAST-33] Main: Generate Full PDF produces the Final Cast Report")
    void m33() { generateFullPdf(Tool.MAIN); }
    @Test @Order(34) @DisplayName("[CAST-34] Main: a Non-Binary cast is listed under the Non Binary filter")
    void m34() { nonBinaryFilter(Tool.MAIN); }
    @Test @Order(35) @DisplayName("[CAST-35] Main: delete the cast from its card and folders in every tab")
    void m35() { deleteEverywhere(Tool.MAIN); }

    // ═════════════════════════════ BACKGROUND CAST (51–99) ═════════════════════════════

    @Test @Order(51) @DisplayName("[CAST-01] Background: tile listed in Tools and opens its own home")
    void b01() { tileOpensHome(Tool.BACKGROUND); }
    @Test @Order(52) @DisplayName("[CAST-02] Background: home shows Selects/Shortlist/Final, search, filter, add buttons")
    void b02() { homeLayout(Tool.BACKGROUND); }
    @Test @Order(53) @DisplayName("[CAST-03] Background: unit chat icon on Selects and Shortlist, hidden on Final")
    void b03() { unitChatIconPerTab(Tool.BACKGROUND); }
    @Test @Order(54) @DisplayName("[CAST-04] Background: unit chat opens per tab, has no attachment button, sends text")
    void b04() { unitChat(Tool.BACKGROUND); }
    @Test @Order(55) @DisplayName("[CAST-05] Background: filter sheet offers Group By and gender Filter By with defaults")
    void b05() { filterSheet(Tool.BACKGROUND); }
    @Test @Order(56) @DisplayName("[CAST-06] Background: searching with an empty field is refused")
    void b06() { emptySearch(Tool.BACKGROUND); }
    @Test @Order(57) @DisplayName("[CAST-07] Background: Add Cast Details form layout")
    void b07() { addFormLayout(Tool.BACKGROUND); }
    @Test @Order(58) @DisplayName("[CAST-08] Background: submit without media or link is refused (Cast Attachment Required)")
    void b08() { attachmentRequired(Tool.BACKGROUND); }
    @Test @Order(59) @DisplayName("[CAST-09] Background: Upload Media offers Link on Selects but not on Final")
    void b09() { uploadMediaOptions(Tool.BACKGROUND); }
    @Test @Order(60) @DisplayName("[CAST-10] Background: Upload Link dialog refuses an empty and an invalid URL")
    void b10() { linkDialogValidation(Tool.BACKGROUND); }
    @Test @Order(61) @DisplayName("[CAST-11] Background: gender options; character number is 5 digits max and needs a character")
    void b11() { genderAndNumber(Tool.BACKGROUND); }
    @Test @Order(62) @DisplayName("[CAST-12] Background: Contact Info validations, add and remove a contact")
    void b12() { contactInfo(Tool.BACKGROUND); }
    @Test @Order(63) @DisplayName("[CAST-13] Background: Shortlist add form requires episode, character, talent and gender")
    void b13() { shortlistAddRequiresFields(Tool.BACKGROUND); }
    @Test @Order(64) @DisplayName("[CAST-14] Background: create a cast in Selects (duplicate episodes refused first)")
    void b14() { createCast(Tool.BACKGROUND); }
    @Test @Order(65) @DisplayName("[CAST-15] Background: episode folder drills down to character folder and cast card")
    void b15() { episodeDrillDown(Tool.BACKGROUND); }
    @Test @Order(66) @DisplayName("[CAST-16] Background: Group By Character and Talent Name show the cast's folder")
    void b16() { groupings(Tool.BACKGROUND); }
    @Test @Order(67) @DisplayName("[CAST-17] Background: search by talent/character (case-insensitive), no match, clear")
    void b17() { search(Tool.BACKGROUND); }
    @Test @Order(68) @DisplayName("[CAST-18] Background: gender filter Male shows the cast, Female hides it")
    void b18() { genderFilter(Tool.BACKGROUND); }
    @Test @Order(69) @DisplayName("[CAST-19] Background: Reset in the filter sheet restores the default grouping")
    void b19() { resetFilter(Tool.BACKGROUND); }
    @Test @Order(70) @DisplayName("[CAST-20] Background: cast card shows character, talent, link, description, uploader, chat")
    void b20() { cardContent(Tool.BACKGROUND); }
    @Test @Order(71) @DisplayName("[CAST-21] Background: cast card shows the cast's gender")
    void b21() { cardGender(Tool.BACKGROUND); }
    @Test @Order(72) @DisplayName("[CAST-22] Background: own Selects card menu items")
    void b22() { cardMenuSelects(Tool.BACKGROUND); }
    @Test @Order(73) @DisplayName("[CAST-23] Background: View Details is read-only and shows the saved values")
    void b23() { viewDetails(Tool.BACKGROUND); }
    @Test @Order(74) @DisplayName("[CAST-24] Background: Edit refuses two episodes, saves a new description")
    void b24() { editCast(Tool.BACKGROUND); }
    @Test @Order(75) @DisplayName("[CAST-25] Background: Select mode bar, Delete with nothing selected, Cancel")
    void b25() { selectMode(Tool.BACKGROUND); }
    @Test @Order(76) @DisplayName("[CAST-26] Background: cast's own chat opens from the card and sends text")
    void b26() { cardChat(Tool.BACKGROUND); }
    @Test @Order(77) @DisplayName("[CAST-27] Background: Selects folder menu offers moves and Delete only")
    void b27() { folderMenuSelects(Tool.BACKGROUND); }
    @Test @Order(78) @DisplayName("[CAST-28] Background: Move To Shortlist from the folder menu")
    void b28() { moveToShortlist(Tool.BACKGROUND); }
    @Test @Order(79) @DisplayName("[CAST-29] Background: Shortlist folder and card menus")
    void b29() { shortlistMenus(Tool.BACKGROUND); }
    @Test @Order(80) @DisplayName("[CAST-30] Background: a link-only cast cannot be moved to Final from its card")
    void b30() { cardMoveFinalNeedsMedia(Tool.BACKGROUND); }
    @Test @Order(81) @DisplayName("[CAST-31] Background: Move To Final refuses a link-only cast; a photo cast moves to Final")
    void b31() { moveToFinal(Tool.BACKGROUND); }
    @Test @Order(82) @DisplayName("[CAST-32] Background: Final folder menu and read-only Final card")
    void b32() { finalMenus(Tool.BACKGROUND); }
    @Test @Order(83) @DisplayName("[CAST-33] Background: Generate Full PDF produces the Final Cast Report")
    void b33() { generateFullPdf(Tool.BACKGROUND); }
    @Test @Order(84) @DisplayName("[CAST-36] Main and Background keep separate cast lists")
    void b84() { toolsAreSeparate(); }
    @Test @Order(85) @DisplayName("[CAST-35] Background: delete the cast from its card and folders in every tab")
    void b85() { deleteEverywhere(Tool.BACKGROUND); }

    // ═════════════════════════════ case bodies ═════════════════════════════

    private void tileOpensHome(Tool tool) {
        CastingPage p = openTool(tool, true);
        assertEquals(tool.header(), String.valueOf(p.headerText()).trim(),
                "The home header should name the tool the tile opened");
        assertTrue(p.isHomeLoaded(Duration.ofSeconds(3)), "CastingHomePage (tabs + folder list) should be up");
    }

    private void homeLayout(Tool tool) {
        CastingPage p = openTool(tool, true);
        assertEquals(CastingPage.TABS, p.tabLabels(), "Tabs, in order");
        String sel = p.selectedTab();
        assertTrue(sel.isEmpty() || sel.contains(CastingPage.TAB_SELECTS),
                "Selects should be the tab selected on open, was '" + sel + "'");
        assertEquals(CastingPage.SEARCH_HINT, String.valueOf(p.searchHint()).trim(), "Search hint");
        assertTrue(p.filterIconShown(), "The filter icon should be in the toolbar");
        assertTrue(p.addFabShown(), "The '+' (add cast) button should show for an admin");
        assertTrue(p.cameraFabShown(), "The camera button should show for an admin");
    }

    private void unitChatIconPerTab(Tool tool) {
        CastingPage p = openTool(tool, true);
        sleep(2500);    // unit info arrives after the unit list fetch
        assertTrue(p.unitChatIconShown(), "Selects should offer its unit chat icon");
        assertTrue(p.selectTab(CastingPage.TAB_SHORTLIST));
        sleep(1500);
        assertTrue(p.unitChatIconShown(), "Shortlist should offer its unit chat icon");
        assertTrue(p.selectTab(CastingPage.TAB_FINAL));
        sleep(1500);
        assertFalse(p.unitChatIconShown(), "Final has no unit chat (charRl hidden on the Final tab)");
        assertTrue(p.addFabShown(), "Final still offers '+'");
    }

    private void unitChat(Tool tool) {
        for (String tab : new String[]{CastingPage.TAB_SELECTS, CastingPage.TAB_SHORTLIST}) {
            CastingPage p = openTab(tool, tab, false);
            sleep(2000);
            assumeTrue(p.unitChatIconShown(), "No unit chat icon on " + tab);
            assertTrue(p.openUnitChat(), "The " + tab + " unit chat should open");
            String header = String.valueOf(p.headerText()).trim();
            assertEquals(tool.header() + ": " + tab.toUpperCase(), header, "Unit chat header");
            HomeChatPage chat = new HomeChatPage(driver);
            assertFalse(chat.isAttachmentVisible(), "The Casting unit chat offers no attachment button");
            if (tab.equals(CastingPage.TAB_SELECTS)) {
                String msg = "ZL_AUTO-cast-unit-" + fx(tool).n;
                chat.sendMessage(msg);
                assertTrue(chat.waitMessageSent(msg, Duration.ofSeconds(30)),
                        "A text sent in the " + tab + " unit chat should reach the server");
                deleteOwnMessage(chat, msg);
            }
            backToHome(tool);
        }
    }

    private void filterSheet(Tool tool) {
        CastingPage p = openTool(tool, true);
        assertTrue(p.openFilter(), "The filter sheet should open");
        try {
            List<String> group = p.filterOptions("recFilterBy");
            List<String> sex = p.filterOptions("recSortBy");
            boolean tv = group.contains(CastingPage.GROUP_EPISODE);
            assertEquals(tv ? List.of(CastingPage.GROUP_TALENT, CastingPage.GROUP_CHARACTER, CastingPage.GROUP_EPISODE)
                            : List.of(CastingPage.GROUP_TALENT, CastingPage.GROUP_CHARACTER), group, "Group By options");
            assertEquals(tv ? CastingPage.GROUP_EPISODE : CastingPage.GROUP_CHARACTER, p.filterChecked("recFilterBy"),
                    "Default Group By (Episode on a TV project)");
            assertEquals(List.of(CastingPage.SEX_MALE, CastingPage.SEX_FEMALE, CastingPage.SEX_NON_BINARY_FILTER, CastingPage.SEX_ALL),
                    sex, "Filter By (gender) options");
            assertEquals(CastingPage.SEX_ALL, p.filterChecked("recSortBy"), "Default gender filter");
        } finally {
            p.filterClose();
        }
    }

    private void emptySearch(Tool tool) {
        CastingPage p = openTool(tool, false);
        p.search("");
        assertTrue(p.snackbarShows(CastingPage.EMPTY_SEARCH, Duration.ofSeconds(5)),
                "Tapping search on an empty field should say '" + CastingPage.EMPTY_SEARCH + "', got '" + p.snackbar() + "'");
    }

    private void addFormLayout(Tool tool) {
        CastingPage p = openTool(tool, false);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        try {
            assertEquals(CastingPage.ADD_HEADER, String.valueOf(p.headerText()).trim(), "Form header");
            assertTrue(p.uploadMediaShown(), "'Upload Media' should be in the toolbar");
            for (String f : new String[]{CastingPage.F_EPISODE, CastingPage.F_CHARACTER, CastingPage.F_TALENT,
                    "gender", CastingPage.F_CHAR_NO, CastingPage.F_DESC}) {
                assertTrue(p.fieldShown(f), "Field '" + f + "' should be on the form");
            }
            assertTrue(p.episodeHintShown(), "The multiple-episodes hint should show (TV project)");
            assertTrue(p.availabilityShown(), "Availability Dates should be on the create form");
            assertTrue(p.contactCardShown(), "Contact Info should be on the create form");
            assertTrue(p.submitShown(), "Submit should be on the form");
        } finally {
            p.backFromForm();
        }
    }

    private void attachmentRequired(Tool tool) {
        CastingPage p = openTool(tool, false);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        try {
            p.setField(CastingPage.F_CHARACTER, "ZL_AUTO_NoMedia");
            p.setField(CastingPage.F_TALENT, "ZL_AUTO_NoMedia");
            p.submitForm();
            assertTrue(p.snackbarShows(CastingPage.ATTACHMENT_REQUIRED, Duration.ofSeconds(5)),
                    "Submit with no media or link should say '" + CastingPage.ATTACHMENT_REQUIRED + "', got '" + p.snackbar() + "'");
            assertTrue(p.formShown(Duration.ofSeconds(2)), "The form should stay open");
        } finally {
            p.backFromForm();
        }
    }

    private void uploadMediaOptions(Tool tool) {
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, false);
        assertTrue(p.openAddForm(), "'+' should open the add form on Selects");
        try {
            assertEquals(CastingPage.PICKER_WITH_LINK, p.openUploadMedia(), "Upload Media options on Selects");
            p.closeSheet();
        } finally {
            p.backFromForm();
        }
        p = backToHome(tool);
        assertTrue(p.selectTab(CastingPage.TAB_FINAL));
        assertTrue(p.openAddForm(), "'+' should open the add form on Final");
        try {
            List<String> opts = p.openUploadMedia();
            assertFalse(opts.isEmpty(), "Upload Media should open on Final");
            assertFalse(opts.contains("Link"), "Final offers no Link — only camera/gallery media: " + opts);
            p.closeSheet();
        } finally {
            p.backFromForm();
        }
    }

    private void linkDialogValidation(Tool tool) {
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, false);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        try {
            assertTrue(p.openLinkDialog(), "Upload Media → Link should open the Upload Link dialog");
            p.linkDialogSubmit();
            assertTrue(p.linkDialogShown(), "Submit with an empty link should keep the dialog open");
            String err = p.linkDialogError();
            assertTrue(err.isEmpty() || err.contains("Please Enter URL"), "Empty link error, got '" + err + "'");
            assertTrue(p.pasteIntoLinkDialog("zl auto not a url"), "Could not paste into the link field");
            p.linkDialogSubmit();
            assertTrue(p.linkDialogShown(), "An invalid URL should keep the dialog open");
            err = p.linkDialogError();
            assertTrue(err.isEmpty() || err.contains("Please Enter Valid URL"), "Invalid link error, got '" + err + "'");
            p.linkDialogCancel();
            assertFalse(p.attachmentShown(), "Nothing should have been attached");
        } finally {
            if (p.linkDialogShown()) p.linkDialogCancel();
            p.backFromForm();
        }
    }

    private void genderAndNumber(Tool tool) {
        CastingPage p = openTool(tool, false);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        try {
            assertEquals(CastingPage.GENDERS, p.genderOptions(), "Gender dropdown options");
            assertTrue(p.pickGender("Female"), "Picking Female should fill the field");
            // A number with no character: the debounce clears it again (toast "Please Enter Character Name").
            p.setField(CastingPage.F_CHARACTER, "");
            p.setField(CastingPage.F_CHAR_NO, "77");
            sleep(2500);
            String num = String.valueOf(p.field(CastingPage.F_CHAR_NO)).trim();
            assertTrue(num.isEmpty() || num.equals("Character Number"),
                    "A character number with no character should be cleared, field reads '" + num + "'");
            // Max 5 digits (LengthFilter(5)).
            p.setField(CastingPage.F_CHARACTER, "ZL_AUTO_Len " + fx(tool).n);
            p.setField(CastingPage.F_CHAR_NO, "12345678");
            sleep(1500);
            p.clearAvailabilityDialog();
            num = String.valueOf(p.field(CastingPage.F_CHAR_NO)).trim();
            assertTrue(num.length() <= 5, "Character Number should keep at most 5 digits, reads '" + num + "'");
        } finally {
            p.backFromForm();
        }
    }

    private void contactInfo(Tool tool) {
        CastingPage p = openTool(tool, false);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        try {
            assertTrue(p.openContactInfo(), "Contact Info '+' should open the contact page");
            assertEquals("Actor", String.valueOf(p.contactField("etType")).trim(), "Type defaults to Actor");
            p.contactSubmit();
            assertTrue(p.contactPageShown(), "No name: the contact page should stay");
            p.setContactField("nameTv", "ZL_AUTO Agent " + fx(tool).n);
            p.contactSubmit();
            assertTrue(p.contactPageShown(), "No company: the contact page should stay");
            p.setContactField("company", "ZL_AUTO Agency");
            p.contactSubmit();
            assertTrue(p.snackbarShows("Please enter email or phone number", Duration.ofSeconds(5)) || p.contactPageShown(),
                    "No email or phone should be refused");
            assertTrue(p.contactPageShown(), "No email or phone: the contact page should stay");
            p.setContactField("email", "zl auto not an email");
            p.contactSubmit();
            assertTrue(p.contactPageShown(), "An invalid email should be refused");
            p.setContactField("email", TestEmails.self());
            p.contactSubmit();
            assertTrue(p.formShown(Duration.ofSeconds(8)), "A valid contact should return to the cast form");
            List<String> rows = p.contactRows();
            assertTrue(rows.stream().anyMatch(r -> r.contains("ZL_AUTO Agent " + fx(tool).n)),
                    "The contact should be listed on the form: " + rows);
            assertTrue(p.deleteFirstContact(), "The contact row should offer delete");
            assertTrue(p.dialogShown(Duration.ofSeconds(5)), "Deleting a contact should ask to confirm");
            assertEquals(CastingPage.DELETE_CONFIRM, String.valueOf(p.dialogMessage()).trim());
            p.dialogRight();
            rows = p.contactRows();
            assertFalse(rows.stream().anyMatch(r -> r.contains("ZL_AUTO Agent " + fx(tool).n)),
                    "The contact should be gone after Yes: " + rows);
        } finally {
            if (p.contactPageShown()) p.backFromForm();
            p.backFromForm();
        }
    }

    private void shortlistAddRequiresFields(Tool tool) {
        CastingPage p = openTab(tool, CastingPage.TAB_SHORTLIST, false);
        assertTrue(p.openAddForm(), "'+' should open the add form on Shortlist");
        try {
            assumeTrue(p.addLinkAttachment(fx(tool).link + "-v"), "Could not attach a link (clipboard / Upload Link)");
            // Each submit must be refused while a required field is empty; the form stays.
            p.submitForm();
            assertTrue(p.formStays(), "Shortlist: empty episode should be refused");
            assertErrorIfReadable(p, CastingPage.F_EPISODE, "Episode no. cannot be empty.");
            p.setField(CastingPage.F_EPISODE, "9");
            p.submitForm();
            assertTrue(p.formStays(), "Shortlist: empty character should be refused");
            assertErrorIfReadable(p, CastingPage.F_CHARACTER, "Please Enter Character Name");
            p.setField(CastingPage.F_CHARACTER, "ZL_AUTO_Req " + fx(tool).n);
            p.submitForm();
            assertTrue(p.formStays(), "Shortlist: empty talent should be refused");
            assertErrorIfReadable(p, CastingPage.F_TALENT, "Please Enter Talent Name");
            p.setField(CastingPage.F_TALENT, "ZL_AUTO_Req " + fx(tool).n);
            p.submitForm();
            assertTrue(p.formStays(), "Shortlist: empty gender should be refused");
        } finally {
            p.backFromForm();   // nothing submitted — the cast is never created
        }
        CastingPage home = backToHome(tool);
        home.selectTab(CastingPage.TAB_SHORTLIST);
        home.groupBy(CastingPage.GROUP_CHARACTER, null);
        assertFalse(home.waitFolder("ZL_AUTO_Req " + fx(tool).n, Duration.ofSeconds(4)),
                "The refused cast must not have been created");
    }

    private void createCast(Tool tool) {
        Fx f = fx(tool);
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, true);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        boolean submitted = false;
        try {
            assumeTrue(p.addLinkAttachment(f.link), "Could not attach a link (clipboard / Upload Link)");
            p.fill(f.episode + "," + f.episode, f.character, f.talent, "Male", f.charNo, f.desc);
            assertEquals("Male", String.valueOf(p.gender()).trim(), "Test setup: gender could not be set to Male");
            p.submitForm();
            assertTrue(p.formStays(), "Duplicate episodes '" + f.episode + "," + f.episode + "' should be refused");
            assertErrorIfReadable(p, CastingPage.F_EPISODE, "Duplicate episode numbers found");
            p.setField(CastingPage.F_EPISODE, f.episode);
            p.clearAvailabilityDialog();
            p.submitForm();
            submitted = p.waitFormGone(Duration.ofSeconds(16));
            assertTrue(submitted, "Submit should close the form; snackbar '" + p.snackbar() + "', character no. error '"
                    + p.fieldError(CastingPage.F_CHAR_NO) + "'");
        } finally {
            if (!submitted) p.backFromForm();
        }
        f.attempted = true;
        CastingPage home = backToHome(tool);
        boolean listed = home.waitFolder("Episode No : " + f.episode, LIST_WAIT) || showCharacterFolder(tool, CastingPage.TAB_SELECTS);
        assertTrue(listed, "The new cast should be listed in Selects. Folders: " + page().folders());
        f.tabs.add(CastingPage.TAB_SELECTS);
    }

    private void episodeDrillDown(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, true);
        if (!p.waitFolder("Episode No : " + f.episode, LIST_WAIT)) {
            List<Folder> fs = p.folders();
            assertTrue(false, "Selects (default Episode grouping) should list 'Episode No : " + f.episode + "'; folders: " + fs);
        }
        assertTrue(p.openFolder("Episode No : " + f.episode), "The episode folder should open");
        assertTrue(p.waitFolder(f.character, Duration.ofSeconds(20)),
                "The episode page should list 'Character: " + f.character + "'. Folders: " + p.folders());
        Folder cf = p.folder(f.character);
        assertNotNull(cf);
        assertTrue(cf.other.toLowerCase().contains(f.talent.toLowerCase()),
                "The character folder should name the talent: " + cf);
        assertTrue(p.openFolder(f.character), "The character folder should open");
        assertTrue(p.onListing(Duration.ofSeconds(15)), "The cast listing should open");
        assertTrue(p.waitCard(f.character, Duration.ofSeconds(20)), "The cast card should be listed: " + p.cards());
    }

    private void groupings(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, true);
        assertTrue(p.waitFolder("Episode No : " + f.episode, LIST_WAIT), "The cast's episode folder should load first");
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, null), "Group By Character should apply");
        assertTrue(p.waitFolder(f.character, LIST_WAIT), "Group By Character should list the cast: " + p.folders());
        Folder c = p.folder(f.character);
        assertTrue(c.name.toLowerCase().startsWith("character: "), "Character folder title: " + c);
        assertTrue(c.other.toLowerCase().contains("talent name: " + f.talent.toLowerCase()), "Character folder detail: " + c);
        assertTrue(p.groupBy(CastingPage.GROUP_TALENT, null), "Group By Talent Name should apply");
        assertTrue(p.waitFolder(f.talent, LIST_WAIT), "Group By Talent Name should list the cast: " + p.folders());
        Folder t = p.folder(f.talent);
        assertTrue(t.name.toLowerCase().startsWith("talent name: "), "Talent folder title: " + t);
        assertTrue(t.other.toLowerCase().contains("character: " + f.character.toLowerCase()), "Talent folder detail: " + t);
    }

    private void search(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, true);
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, null), "Group By Character should apply");
        assertTrue(p.waitFolder(f.character, LIST_WAIT), "Cast folder before searching: " + p.folders());
        p.search(f.talent.toUpperCase());
        assertTrue(p.waitFolder(f.character, Duration.ofSeconds(8)), "Searching the talent in capitals should find the cast");
        for (Folder x : p.folders()) {
            assertTrue((x.name + " " + x.other).toLowerCase().contains(f.talent.toLowerCase()),
                    "Every result should match the search term; got " + x);
        }
        p.clearSearch();
        p.search("zl_auto_char " + f.n.toLowerCase());
        assertTrue(p.waitFolder(f.character, Duration.ofSeconds(8)), "Searching the character should find the cast");
        p.clearSearch();
        p.search("zl_auto_nomatch_" + f.n);
        assertTrue(p.folders().isEmpty(), "A term nothing matches should empty the list: " + p.folders());
        p.clearSearch();
        assertTrue(p.waitFolder(f.character, Duration.ofSeconds(10)), "Clearing the search should bring the cast back");
    }

    private void genderFilter(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, true);
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, CastingPage.SEX_MALE), "Character + Male should apply");
        assertTrue(p.waitFolder(f.character, LIST_WAIT), "A Male cast should be listed under Male: " + p.folders());
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, CastingPage.SEX_FEMALE), "Character + Female should apply");
        assertTrue(p.waitFolderGone(f.character, Duration.ofSeconds(6)), "A Male cast should not be listed under Female");
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, CastingPage.SEX_ALL), "Character + All should apply");
        assertTrue(p.waitFolder(f.character, LIST_WAIT), "All should list the cast again");
    }

    private void resetFilter(Tool tool) {
        CastingPage p = openTool(tool, true);
        assertTrue(p.openFilter());
        String def = p.filterChecked("recFilterBy");
        p.filterClose();
        assertTrue(p.groupBy(CastingPage.GROUP_TALENT, CastingPage.SEX_FEMALE), "Talent + Female should apply");
        assertTrue(p.openFilter());
        assertTrue(p.filterReset(), "Reset should be tappable");
        assertTrue(p.openFilter(), "The sheet should reopen after Reset");
        try {
            assertEquals(CastingPage.SEX_ALL, p.filterChecked("recSortBy"), "Reset should restore the gender filter to All");
            assertEquals(def, p.filterChecked("recFilterBy"),
                    "[CAST-BUG-02] Reset should restore the default Group By ('" + def + "'); on a TV project"
                            + " CastingVM.resetFilter() calls initializeFilter(context) without the episode flag,"
                            + " so Reset falls back to Character grouping");
        } finally {
            p.filterClose();
        }
    }

    private void cardContent(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        String card = String.valueOf(p.cardText(f.character)).toLowerCase();
        assertTrue(card.contains("character: " + f.character.toLowerCase()), "Card should show the character: " + card);
        assertTrue(card.contains("talent name: " + f.talent.toLowerCase()), "Card should show the talent: " + card);
        assertTrue(card.contains(f.link.toLowerCase()), "Card should show the link: " + card);
        assertTrue(card.contains("uploaded by"), "Card should show who uploaded it: " + card);
        assertTrue(p.cardHas(f.character, "descText"), "Card should show the description");
        assertTrue(p.cardHas(f.character, "icChat"), "A Selects card offers its chat icon");
        assertTrue(p.cardHas(f.character, "imgMore"), "A card offers its menu");
    }

    private void cardGender(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        String card = String.valueOf(p.cardText(f.character)).replace("\n", " / ");
        assertTrue(card.toLowerCase().contains("gender: male"),
                "[CAST-BUG-01] The cast card should show 'Gender: Male' (LibraryItemHolder.setCastingName builds a"
                        + " gender line, but `gender?.let { … }?.run { gender = \"\" }` always clears it). Card: " + card);
    }

    private void cardMenuSelects(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        List<String> items = p.cardMenuItems(f.character);
        for (String want : new String[]{CastingPage.M_VIEW, CastingPage.M_EDIT, CastingPage.M_FORWARD, CastingPage.M_DOWNLOAD,
                CastingPage.M_IMAGE_REPLY, CastingPage.M_MOVE_SHORTLIST, CastingPage.M_MOVE_FINAL, CastingPage.M_READ_BY,
                CastingPage.M_DELETE}) {
            assertTrue(items.contains(want), "Own Selects card menu should offer '" + want + "': " + items);
        }
        assertFalse(items.contains(CastingPage.M_CHAT), "Chat is the card's icon, not a menu item: " + items);
    }

    private void viewDetails(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        assertTrue(p.cardMenu(f.character, CastingPage.M_VIEW), "View Details should be tappable");
        try {
            assertTrue(p.formShown(Duration.ofSeconds(10)), "View Details should open the cast form");
            assertEquals(CastingPage.VIEW_HEADER, String.valueOf(p.headerText()).trim(), "View header");
            assertFalse(p.submitShown() && p.displayed("submit_btn"), "View Details has no Submit");
            assertFalse(p.uploadMediaShown(), "View Details has no Upload Media");
            assertTrue(String.valueOf(p.field(CastingPage.F_CHARACTER)).equalsIgnoreCase(f.character), "Character shown");
            assertTrue(String.valueOf(p.field(CastingPage.F_TALENT)).equalsIgnoreCase(f.talent), "Talent shown");
            assertEquals(f.episode, String.valueOf(p.field(CastingPage.F_EPISODE)).trim(), "Episode shown");
            assertEquals("Male", String.valueOf(p.gender()).trim(), "Gender shown");
            assertEquals(f.desc, String.valueOf(p.field(CastingPage.F_DESC)).trim(), "Description shown");
            assertFalse(p.fieldEnabled(CastingPage.F_CHARACTER), "Character is read-only in View Details");
            assertFalse(p.fieldEnabled(CastingPage.F_TALENT), "Talent is read-only in View Details");
        } finally {
            p.backFromForm();
        }
    }

    private void editCast(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        assertTrue(p.cardMenu(f.character, CastingPage.M_EDIT), "Edit should be tappable");
        String newDesc = f.desc + " edited";
        boolean saved = false;
        try {
            assertTrue(p.formShown(Duration.ofSeconds(10)), "Edit should open the cast form");
            assertTrue(String.valueOf(p.field(CastingPage.F_CHARACTER)).equalsIgnoreCase(f.character), "Edit prefills the character");
            assertFalse(p.uploadMediaShown(), "Edit offers no Upload Media");
            p.setField(CastingPage.F_EPISODE, f.episode + ",1");
            p.submitForm();
            assertTrue(p.formStays(), "Two episodes should be refused on update");
            assertErrorIfReadable(p, CastingPage.F_EPISODE, "Only one episode will be allowed at a time during update.");
            p.setField(CastingPage.F_EPISODE, f.episode);
            p.setField(CastingPage.F_DESC, newDesc);
            p.submitForm();
            saved = p.waitFormGone(Duration.ofSeconds(16));
            assertTrue(saved, "Edit should save and close the form; snackbar '" + p.snackbar() + "'");
        } finally {
            if (!saved) p.backFromForm();
        }
        long end = System.currentTimeMillis() + 30000;
        String card = "";
        while (System.currentTimeMillis() < end) {
            card = String.valueOf(p.cardText(f.character));
            String d = descOfCard(p, f.character);
            if (d != null && d.contains("edited")) return;
            sleep(2000);
        }
        // Re-open the listing (a refetch) before deciding.
        p = openListing(tool, CastingPage.TAB_SELECTS);
        String d = descOfCard(p, f.character);
        assertTrue(d != null && d.contains("edited"), "The card should show the edited description, shows '" + d + "' / " + card);
    }

    private void selectMode(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        assertTrue(p.enterSelectMode(), "'Select' should show the selection bar");
        try {
            assertEquals("Move To Shortlist", String.valueOf(p.buttonText("btnMoveTo")).trim(), "Selects bar: first move");
            assertEquals("Move To Final", String.valueOf(p.buttonText("btnMoveToPublish")).trim(), "Selects bar: second move");
            assertFalse(p.addFabShown(), "'+' hides in select mode");
            p.tap("btnDelete");
            assertTrue(p.snackbarShows(CastingPage.KEEP_ONE, Duration.ofSeconds(5)),
                    "Delete with nothing selected should say '" + CastingPage.KEEP_ONE + "', got '" + p.snackbar() + "'");
        } finally {
            p.tap("btnCancel");
        }
        assertFalse(p.selectBarShown(), "Cancel should close the selection bar");
        assertTrue(p.cardRow(f.character) != null, "The cast should still be listed");
    }

    private void cardChat(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = openListing(tool, CastingPage.TAB_SELECTS);
        assertTrue(p.openCardChat(f.character), "The card's chat icon should open the cast chat");
        HomeChatPage chat = new HomeChatPage(driver);
        String msg = "ZL_AUTO-cast-card-" + f.n;
        chat.sendMessage(msg);
        assertTrue(chat.waitMessageSent(msg, Duration.ofSeconds(30)), "A text in the cast chat should reach the server");
        deleteOwnMessage(chat, msg);
    }

    private void folderMenuSelects(Tool tool) {
        Fx f = requireFixture(tool);
        CastingPage p = characterHome(tool, CastingPage.TAB_SELECTS);
        List<String> items = p.folderMenuItems(f.character);
        assertEquals(List.of(CastingPage.M_MOVE_SHORTLIST, CastingPage.M_MOVE_FINAL, CastingPage.M_DELETE), items,
                "Selects folder (named character) menu");
    }

    private void moveToShortlist(Tool tool) {
        Fx f = requireFixture(tool);
        moveFolder(f, CastingPage.TAB_SELECTS, CastingPage.M_MOVE_SHORTLIST, CastingPage.TAB_SHORTLIST);
    }

    private void shortlistMenus(Tool tool) {
        Fx f = requireFixture(tool);
        assumeTrue(f.tabs.contains(CastingPage.TAB_SHORTLIST), "The cast never reached Shortlist");
        CastingPage p = characterHome(tool, CastingPage.TAB_SHORTLIST);
        assertEquals(List.of(CastingPage.M_MOVE_FINAL, CastingPage.M_DELETE), p.folderMenuItems(f.character),
                "Shortlist folder (named character) menu");
        assertTrue(p.openFolder(f.character) && p.onListing(Duration.ofSeconds(15)), "Shortlist listing should open");
        assertTrue(p.waitCard(f.character, Duration.ofSeconds(20)), "Shortlist card: " + p.cards());
        List<String> items = p.cardMenuItems(f.character);
        assertTrue(items.contains(CastingPage.M_MOVE_FINAL), "Shortlist card offers Move To Final: " + items);
        assertFalse(items.contains(CastingPage.M_MOVE_SHORTLIST), "Shortlist card does not offer Move To Shortlist: " + items);
        assertTrue(p.cardHas(f.character, "icChat"), "A Shortlist card offers its chat icon");
    }

    private void cardMoveFinalNeedsMedia(Tool tool) {
        Fx f = requireFixture(tool);
        assumeTrue(f.tabs.contains(CastingPage.TAB_SHORTLIST), "The cast never reached Shortlist");
        CastingPage p = openListing(tool, CastingPage.TAB_SHORTLIST);
        assertTrue(p.cardMenu(f.character, CastingPage.M_MOVE_FINAL), "Move To Final should be in the card menu");
        boolean refused = p.snackbarShows(CastingPage.ATTACHMENT_REQUIRED, Duration.ofSeconds(5));
        if (p.formShown(Duration.ofSeconds(1))) p.backFromForm();
        assertTrue(refused, "Moving a link-only cast to Final from its card should say '" + CastingPage.ATTACHMENT_REQUIRED
                + "' (moveToDetailsPage requires attachment media), got '" + p.snackbar() + "'");
    }

    private void moveToFinal(Tool tool) {
        Fx f = requireFixture(tool);
        assumeTrue(f.tabs.contains(CastingPage.TAB_SHORTLIST), "The cast never reached Shortlist");
        // 1) The link-only cast: the folder route reaches the API, which refuses it.
        CastingPage p = characterHome(tool, CastingPage.TAB_SHORTLIST);
        assertTrue(p.folderMenu(f.character, CastingPage.M_MOVE_FINAL), "Move To Final should be in the Shortlist folder menu");
        assertTrue(p.formShown(Duration.ofSeconds(10)), "The move form should open");
        p.submitForm();
        boolean closed = p.waitFormGone(Duration.ofSeconds(12));
        String sb = p.lastSnackbar;
        if (!closed) p.backFromForm();
        assertTrue(sb != null && sb.contains("cannot move a 'Link'"),
                "Moving a link-only cast to Final should be refused with \"You cannot move a 'Link' … into the 'Final Folder'\"; snackbar seen: '"
                        + sb + "', form closed=" + closed);
        // 2) A photo cast (Shortlist "+" → Gallery → test photo) moves to Final.
        CastingPage home = openTab(tool, CastingPage.TAB_SHORTLIST, true);
        assertTrue(home.openAddForm(), "'+' should open the add form on Shortlist");
        boolean submitted = false;
        try {
            assumeTrue(home.addGalleryPhoto(this::dismissPermissionPrompts),
                    "Could not attach a test photo from the in-app gallery (run-and-report pushes zl_test_*.jpg to /sdcard/Pictures)");
            home.fill(f.episode, f.pic, f.pic, "Male", null, null);
            assertEquals("Male", String.valueOf(home.gender()).trim(), "Test setup: gender could not be set to Male");
            home.submitForm();
            f.picAttempted = true;
            submitted = home.waitFormGone(Duration.ofSeconds(30));
            assertTrue(submitted, "The photo cast should save; snackbar seen '" + home.lastSnackbar + "'");
        } finally {
            if (!submitted) home.backFromForm();
        }
        CastingPage sl = backToHome(tool);
        sl.selectTab(CastingPage.TAB_SHORTLIST);
        sl.groupBy(CastingPage.GROUP_CHARACTER, null);
        // A photo uploads first, then a background create: allow for it.
        assertTrue(sl.waitFolder(f.pic, Duration.ofSeconds(120)) || characterHomeQuiet(tool, CastingPage.TAB_SHORTLIST, f.pic),
                "The photo cast should be listed in Shortlist: " + sl.folders());
        f.picTabs.add(CastingPage.TAB_SHORTLIST);
        moveFolder(f, f.pic, f.pic, f.picTabs, CastingPage.TAB_SHORTLIST, CastingPage.M_MOVE_FINAL, CastingPage.TAB_FINAL);
    }

    private boolean characterHomeQuiet(Tool tool, String tab, String character) {
        CastingPage p = openTab(tool, tab, true);
        p.groupBy(CastingPage.GROUP_CHARACTER, null);
        return p.waitFolder(character, LIST_WAIT);
    }

    private void finalMenus(Tool tool) {
        Fx f = requireFixture(tool);
        assumeTrue(f.picTabs.contains(CastingPage.TAB_FINAL), "The photo cast never reached Final (CAST-31)");
        CastingPage p = characterHome(tool, CastingPage.TAB_FINAL, f.pic);
        assertEquals(List.of(CastingPage.M_FULL_PDF, CastingPage.M_DELETE), p.folderMenuItems(f.pic), "Final folder menu");
        assertTrue(p.openFolder(f.pic) && p.onListing(Duration.ofSeconds(15)), "Final listing should open");
        assertTrue(p.waitCard(f.pic, Duration.ofSeconds(20)), "Final card: " + p.cards());
        assertFalse(p.cardHas(f.pic, "icChat"), "A Final card has no chat icon");
        // Not asserted: handleBtnActions() hides "+" on a Final listing but handleUploadingIcon()
        // shows it again for FINALIZE; on device it is shown (2026-11-24). Logged only.
        System.out.println(">>> Final listing '+' shown: " + p.addFabShown());
        List<String> items = p.cardMenuItems(f.pic);
        assertEquals(List.of(CastingPage.M_VIEW, CastingPage.M_FORWARD, CastingPage.M_DOWNLOAD, CastingPage.M_READ_BY, CastingPage.M_DELETE),
                items, "Final card menu (no Edit, Chat, moves or Image Reply)");
    }

    private void generateFullPdf(Tool tool) {
        Fx f = requireFixture(tool);
        assumeTrue(f.picTabs.contains(CastingPage.TAB_FINAL), "The photo cast never reached Final (CAST-31)");
        CastingPage p = characterHome(tool, CastingPage.TAB_FINAL, f.pic);
        assertTrue(p.folderMenu(f.pic, CastingPage.M_FULL_PDF), "Generate Full PDF should be tappable");
        assertTrue(p.dialogShown(Duration.ofSeconds(6)), "Generate Full PDF should ask how to publish");
        assertEquals(CastingPage.PDF_QUESTION, String.valueOf(p.dialogMessage()).trim(), "PDF question");
        assertEquals(CastingPage.PDF_WITHOUT, String.valueOf(p.leftButtonText()).trim(), "Left choice");
        assertEquals(CastingPage.PDF_WITH, String.valueOf(p.rightButtonText()).trim(), "Right choice");
        p.dialogRight();
        boolean done = p.pdfDoneShown(Duration.ofSeconds(60));
        try {
            assertTrue(done, "The report should be generated ('" + CastingPage.PDF_DONE + "'); snackbar '" + p.snackbar() + "'");
            List<String> buttons = p.pdfDoneButtons();
            assertTrue(buttons.contains("viewBtn") && buttons.contains("confidentialInfoBtn") && buttons.contains("closeTv"),
                    "Report dialog should offer View, Confidential Info and Close: " + buttons);
            assertFalse(buttons.contains("infoBtn") || buttons.contains("userBtn") || buttons.contains("preSetBtn"),
                    "Info, C&C members and preset members are hidden on the report dialog: " + buttons);
        } finally {
            if (done) p.pdfDoneClose();   // Close only — never publish to Confidential Info
        }
    }

    private void nonBinaryFilter(Tool tool) {
        Fx f = fx(tool);
        f.nb = "ZL_AUTO_Nb " + f.n;
        CastingPage p = openTab(tool, CastingPage.TAB_SELECTS, true);
        assertTrue(p.openAddForm(), "'+' should open the add form");
        boolean submitted = false;
        try {
            assumeTrue(p.addLinkAttachment(f.link + "-nb"), "Could not attach a link");
            p.fill(f.episode, f.nb, f.nb, "Non-Binary", null, null);
            assertEquals("Non-Binary", String.valueOf(p.gender()).trim(), "Test setup: gender could not be set to Non-Binary");
            p.submitForm();
            submitted = p.waitFormGone(Duration.ofSeconds(16));
            assertTrue(submitted, "The Non-Binary cast should save; snackbar '" + p.snackbar() + "'");
        } finally {
            if (!submitted) { p.backFromForm(); f.nb = null; }
        }
        p = backToHome(tool);
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, null));
        assertTrue(p.waitFolder(f.nb, LIST_WAIT), "The Non-Binary cast should be listed under All: " + p.folders());
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, CastingPage.SEX_NON_BINARY_FILTER), "Non Binary filter should apply");
        assertTrue(p.waitFolder(f.nb, Duration.ofSeconds(10)),
                "[CAST-BUG-03] A Non-Binary cast should be listed under the 'Non Binary' filter. The form saves"
                        + " Non-Binary as \"others\" (Constants.OTHERS) but the filter compares against \"Other\""
                        + " (R.string.other), so it never matches. Folders: " + p.folders());
    }

    private void toolsAreSeparate() {
        Fx m = fx.get(Tool.MAIN), b = fx.get(Tool.BACKGROUND);
        assumeTrue(b != null && b.created(), "No Background cast to compare");
        CastingPage p = openTab(Tool.MAIN, CastingPage.TAB_FINAL, true);
        p.groupBy(CastingPage.GROUP_CHARACTER, null);
        p.search(b.character);
        assertFalse(p.waitFolder(b.character, Duration.ofSeconds(5)), "Main Cast must not list the Background cast");
        if (m != null && m.created()) {
            p = openTab(Tool.BACKGROUND, CastingPage.TAB_FINAL, true);
            p.groupBy(CastingPage.GROUP_CHARACTER, null);
            p.search(m.character);
            assertFalse(p.waitFolder(m.character, Duration.ofSeconds(5)), "Background Cast must not list the Main cast");
        }
    }

    private void deleteEverywhere(Tool tool) {
        Fx f = fx.get(tool);
        assumeTrue(f != null && (f.created() || f.attempted || f.nb != null || f.picAttempted), "Nothing was created for " + tool.tile);
        // 1) The card route, on the photo cast in Final.
        if (f.picTabs.contains(CastingPage.TAB_FINAL)) {
            CastingPage p = openListing(tool, CastingPage.TAB_FINAL, f.pic);
            assertTrue(p.cardMenu(f.pic, CastingPage.M_DELETE), "Delete should be in the Final card menu");
            assertTrue(p.dialogShown(Duration.ofSeconds(5)), "Delete should ask to confirm");
            assertEquals(CastingPage.DELETE_CONFIRM, String.valueOf(p.dialogMessage()).trim());
            p.dialogRight();
            assertTrue(p.waitCardGone(f.pic, Duration.ofSeconds(20)), "The card should leave Final after Yes");
            f.picTabs.remove(CastingPage.TAB_FINAL);
            CastingPage home = backToHome(tool);
            home.selectTab(CastingPage.TAB_FINAL);
            home.groupBy(CastingPage.GROUP_CHARACTER, null);
            assertTrue(home.waitFolderGone(f.pic, Duration.ofSeconds(20)), "Final should no longer list the photo cast");
        }
        // 2) The folder route (admin only) for every copy left in any tab.
        for (String tab : List.of(CastingPage.TAB_SHORTLIST, CastingPage.TAB_SELECTS, CastingPage.TAB_FINAL)) {
            assertTrue(deleteFolderIfListed(tool, tab, f.character), "Could not delete the cast's folder in " + tab);
            if (f.picAttempted) assertTrue(deleteFolderIfListed(tool, tab, f.pic), "Could not delete the photo cast's folder in " + tab);
        }
        if (f.nb != null) {
            assertTrue(deleteFolderIfListed(tool, CastingPage.TAB_SELECTS, f.nb), "Could not delete the Non-Binary cast");
            f.nb = null;
        }
        f.tabs.clear();
        f.picTabs.clear();
        f.attempted = false;
        f.picAttempted = false;
    }

    // ═════════════════════════════ helpers ═════════════════════════════

    private Fx requireFixture(Tool tool) {
        Fx f = fx.get(tool);
        assumeTrue(f != null && f.created(), "The " + tool.shortName + " cast was not created (CAST-14 failed or skipped)");
        return f;
    }

    /** Home of the tool, on the tab, grouped by Character, with the fixture's folder listed. */
    private CastingPage characterHome(Tool tool, String tab) {
        return characterHome(tool, tab, fx(tool).character);
    }

    private CastingPage characterHome(Tool tool, String tab, String character) {
        CastingPage p = openTab(tool, tab, true);
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, null), "Group By Character should apply");
        if (!p.waitFolder(character, LIST_WAIT)) {
            // A refetch: leave and re-enter the tab (handleTabChange → getList), group again.
            p.selectTab(tab.equals(CastingPage.TAB_SELECTS) ? CastingPage.TAB_SHORTLIST : CastingPage.TAB_SELECTS);
            p.selectTab(tab);
            p.groupBy(CastingPage.GROUP_CHARACTER, null);
            if (!p.waitFolder(character, Duration.ofSeconds(20))) p.search(character);
        }
        assertTrue(p.waitFolder(character, Duration.ofSeconds(10)),
                tab + " should list the cast's character folder. Folders: " + p.folders());
        return p;
    }

    private boolean showCharacterFolder(Tool tool, String tab) {
        CastingPage p = openTab(tool, tab, true);
        p.groupBy(CastingPage.GROUP_CHARACTER, null);
        return p.waitFolder(fx(tool).character, LIST_WAIT);
    }

    private CastingPage openListing(Tool tool, String tab) {
        return openListing(tool, tab, fx(tool).character);
    }

    private CastingPage openListing(Tool tool, String tab, String character) {
        CastingPage p = characterHome(tool, tab, character);
        assertTrue(p.openFolder(character), "The character folder should open");
        assertTrue(p.onListing(Duration.ofSeconds(15)), "The cast listing should open");
        assertTrue(p.waitCard(character, Duration.ofSeconds(25)), "The cast card should be listed: " + p.cards());
        return p;
    }

    private String descOfCard(CastingPage p, String part) {
        org.openqa.selenium.WebElement row = p.cardRow(part);
        if (row == null) return null;
        try {
            List<org.openqa.selenium.WebElement> d = row.findElements(io.appium.java_client.AppiumBy.id(appPackage + ":id/descText"));
            return d.isEmpty() ? null : d.get(0).getText();
        } catch (Exception e) { return null; }
    }

    /** Folder menu move → the prefilled move form → Submit → the folder shows in the target tab. */
    private void moveFolder(Fx f, String fromTab, String menuItem, String toTab) {
        moveFolder(f, f.character, f.talent, f.tabs, fromTab, menuItem, toTab);
    }

    private void moveFolder(Fx f, String character, String talent, Set<String> tabs,
                            String fromTab, String menuItem, String toTab) {
        CastingPage p = characterHome(f.tool, fromTab, character);
        assertTrue(p.folderMenu(character, menuItem), "'" + menuItem + "' should be in the " + fromTab + " folder menu");
        boolean submitted = false;
        try {
            assertTrue(p.formShown(Duration.ofSeconds(10)), "The move form should open");
            assertTrue(String.valueOf(p.field(CastingPage.F_CHARACTER)).equalsIgnoreCase(character), "Move form prefills the character");
            assertTrue(String.valueOf(p.field(CastingPage.F_TALENT)).equalsIgnoreCase(talent), "Move form prefills the talent");
            assertEquals(f.episode, String.valueOf(p.field(CastingPage.F_EPISODE)).trim(), "Move form prefills the episode");
            assertEquals("Male", String.valueOf(p.gender()).trim(), "Move form prefills the gender");
            assertFalse(p.uploadMediaShown(), "The move form offers no Upload Media");
            assertFalse(p.displayed(CastingPage.F_CHAR_NO), "The move form hides Character Number");
            assertFalse(p.displayed("cardAvailibility"), "The move form hides Availability Dates");
            p.submitForm();
            submitted = p.waitFormGone(Duration.ofSeconds(20));
            assertTrue(submitted, "The move to " + toTab + " should submit and close the form; snackbar seen: '"
                    + p.lastSnackbar + "', episode error '" + p.fieldError(CastingPage.F_EPISODE) + "'");
        } finally {
            if (!submitted) p.backFromForm();
        }
        tabs.add(toTab);
        CastingPage home = backToHome(f.tool);
        home.selectTab(toTab);
        home.groupBy(CastingPage.GROUP_CHARACTER, null);
        assertTrue(home.waitFolder(character, LIST_WAIT), toTab + " should list the moved cast: " + home.folders());
        // Observation only (product rule unknown): does the source tab keep a copy?
        home.selectTab(fromTab);
        boolean copy = home.waitFolder(character, Duration.ofSeconds(6));
        f.movedLeftCopy = copy ? fromTab : null;
        if (!copy) tabs.remove(fromTab);
        System.out.println(">>> After '" + menuItem + "' the " + fromTab + " tab " + (copy ? "STILL lists" : "no longer lists")
                + " the cast (" + f.tool.shortName + ")");
    }

    private boolean deleteFolderIfListed(Tool tool, String tab, String character) {
        CastingPage p = openTab(tool, tab, true);
        p.groupBy(CastingPage.GROUP_CHARACTER, null);
        if (!p.waitFolder(character, Duration.ofSeconds(12))) return true;
        if (!p.folderMenu(character, CastingPage.M_DELETE)) return false;
        if (!p.dialogShown(Duration.ofSeconds(5))) return false;
        p.dialogRight();
        return p.waitFolderGone(character, Duration.ofSeconds(20));
    }

    private void assertErrorIfReadable(CastingPage p, String field, String expected) {
        String err = p.fieldError(field);
        if (!err.isEmpty()) {
            assertTrue(err.contains(expected), "Field '" + field + "' error should read '" + expected + "', got '" + err + "'");
        } else {
            System.out.println(">>> (" + field + " error text not exposed in the page source; checked the form stayed open)");
        }
    }

    private void deleteOwnMessage(HomeChatPage chat, String text) {
        try {
            if (!chat.longPressMessage(text) || !chat.tapMenuOption("Delete")) {
                System.out.println(">>> could not open Delete on '" + text + "' (left in the chat, ZL_AUTO-prefixed)");
                try { driver.navigate().back(); } catch (Exception ignored) { }
                return;
            }
            chat.tapDeleteActionBar(Duration.ofSeconds(5));
            chat.confirmPopup().accept(Duration.ofSeconds(6));
            sleep(1500);
        } catch (RuntimeException e) {
            System.out.println(">>> deleting '" + text + "' failed: " + e.getMessage());
        }
    }

    /** Removes whatever this run created if a case died before CAST-35 did it. */
    @AfterAll
    void sweep() {
        for (Fx f : fx.values()) {
            try {
                if (f.created() || f.attempted) {
                    for (String tab : List.of(CastingPage.TAB_FINAL, CastingPage.TAB_SHORTLIST, CastingPage.TAB_SELECTS)) {
                        System.out.println(">>> sweep " + f.tool.shortName + "/" + tab + ": "
                                + deleteFolderIfListed(f.tool, tab, f.character));
                    }
                }
                if (f.nb != null) deleteFolderIfListed(f.tool, CastingPage.TAB_SELECTS, f.nb);
                if (f.picAttempted || !f.picTabs.isEmpty()) {
                    for (String tab : List.of(CastingPage.TAB_FINAL, CastingPage.TAB_SHORTLIST, CastingPage.TAB_SELECTS)) {
                        deleteFolderIfListed(f.tool, tab, f.pic);
                    }
                }
            } catch (Throwable t) {
                System.out.println(">>> sweep of " + f.tool.shortName + " failed: " + t);
            }
        }
    }
}
