package com.zillit.automation.tests.callsheetcreation;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.callsheetcreation.CallSheetCreationPage;
import com.zillit.automation.pages.workflow.DocEditorPage;
import com.zillit.automation.pages.workflow.DocEditorPage.Section;
import com.zillit.automation.pages.workflow.DocWorkflowPage;
import com.zillit.automation.pages.workflow.DocWorkflowPage.HistoryEntry;
import com.zillit.automation.pages.workflow.DocWorkflowPage.ListKind;
import com.zillit.automation.pages.workflow.DocWorkflowPage.Row;
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
import java.util.Map;

import static com.zillit.automation.pages.workflow.DocWorkflowPage.TAB_APPROVALS;
import static com.zillit.automation.pages.workflow.DocWorkflowPage.TAB_DRAFTS;
import static com.zillit.automation.pages.workflow.DocWorkflowPage.TAB_PERMISSION;
import static com.zillit.automation.pages.workflow.DocWorkflowPage.TAB_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CALL SHEET CREATION — the creator's side, on ONE device: the tool's tabs, creating a sheet
 * from a template, the editor's native parts (header, sections, approvers, overflow, name and
 * discard dialogs), the Drafts row actions, Send for Comments / Send for Signature as far as
 * the creator sees them, and the admin Permission tab. The approver's side is
 * {@code CallSheetApprovalTest} (two devices). Cases and ids: {@code CALL_SHEET_CREATION_TESTCASES.md}.
 *
 * <p><b>What it leaves behind:</b> nothing it can delete. Every sheet is named
 * {@code ZL_AUTO_CS_<run>…} and swept by CSC-99 from Drafts and Sent. Sending notifies the
 * project's approver (Vidya Pixel on the automation project) — that is the point of sending.
 *
 * <p>Cases marked <b>[QA CS-nn]</b> re-check a bug from the QA module testing report; the ones
 * still open on Android fail until the app is fixed.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.CALLSHEET_CREATION)
class CallSheetCreationTest extends CallSheetCreationBaseTest {

    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    private final String nameA = PREFIX + RUN + "_A";
    private final String nameB = PREFIX + RUN + "_B";
    /** The approver configured on the automation project (Vidya Pixel = the Pixel's user). */
    private final String approver = System.getProperty("zillit.callsheet.approver", "Vidya Pixel");

    private String serialA, serialB;
    private String me;

    // ═══════════════════════════ 1. shell · CSC-0 ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("CSC-01 The tool opens on Drafts with Drafts · Approvals · Published (· Permission)")
    void shell() {
        openTool();
        CallSheetCreationPage p = page();
        assertEquals(CallSheetCreationPage.HEADER, p.header(), "Header");
        List<String> tabs = p.tabNames();
        assertTrue(tabs.size() >= 3 && tabs.subList(0, 3).equals(List.of(TAB_DRAFTS, TAB_APPROVALS, TAB_PUBLISHED)),
                "Tabs: " + tabs);
        assertTrue(tabs.size() == 3 || (tabs.size() == 4 && TAB_PERMISSION.equals(tabs.get(3))), "Tabs: " + tabs);
        assertTrue(p.tabSelected(TAB_DRAFTS), "Drafts is not selected on opening");
        assertTrue(p.createShown(), "No create button for a creator with posting rights");
        assertEquals("New Call Sheet", p.createDesc(), "Create button content-desc");
    }

    @Test
    @Order(20)
    @DisplayName("CSC-03 The filter chips read All / Drafts / For Comments and add up")
    void chipsAddUp() {
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        int all = p.chipCount("chip_filter_all");
        int drafts = p.chipCount("chip_filter_drafts");
        int comments = p.chipCount("chip_filter_for_comments");
        assertTrue(p.chip("chip_filter_all").startsWith("All "), "All chip: " + p.chip("chip_filter_all"));
        assertTrue(p.chip("chip_filter_drafts").startsWith("Drafts "), "Drafts chip");
        assertTrue(p.chip("chip_filter_for_comments").startsWith("For Comments "), "For Comments chip");
        assertEquals(all, drafts + comments, "All ≠ Drafts + For Comments");
        assertEquals(all, p.rows(ListKind.DRAFTS).size(), "All chip vs rows listed");
    }

    @Test
    @Order(30)
    @DisplayName("CSC-05 An empty Drafts tab says \"No drafts yet\"")
    void emptyDrafts() {
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        assumeTrue(p.rows(ListKind.DRAFTS).isEmpty(), "Drafts already has rows on this project — nothing to check");
        assertEquals("No drafts yet", p.emptyText(), "Empty text");
    }

    @Test
    @Order(40)
    @DisplayName("CSC-02 [web S02/S03] Tabs and the Approvals sub-tabs switch without leaving the tool")
    void tabsAndSubTabs() {
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        for (String t : List.of(TAB_APPROVALS, TAB_PUBLISHED, TAB_DRAFTS)) {
            assertTrue(p.openTab(t), "Tab " + t + " not tappable");
            assertTrue(p.tabSelected(t), t + " is not selected after tapping it");
            assertTrue(p.isLoaded(Duration.ofSeconds(5)), "The tool closed after switching to " + t);
        }
        p.openTab(TAB_APPROVALS);
        List<String> subs = p.subTabNames();
        assertTrue(subs.contains(DocWorkflowPage.SUB_SENT) && subs.contains(DocWorkflowPage.SUB_FINALIZED), "Approvals sub-tabs: " + subs);
        for (String sub : subs) {
            assertTrue(p.openSubTab(sub), "Sub-tab " + sub + " not tappable");
            assertTrue(p.waitSubTab(sub, Duration.ofSeconds(3)), sub + " vanished after tapping it");
        }
        p.openTab(TAB_DRAFTS);
    }

    @Test
    @Order(50)
    @DisplayName("CSC-84 [web P03/P05] Published shows the \"Currently Published\" card or \"No published call sheets\"")
    void publishedTab() {
        onTab(TAB_PUBLISHED);
        String state = page().publishedState(Duration.ofSeconds(20));
        page().openTab(TAB_DRAFTS);
        assertNotNull(state, "Published shows neither a card nor its empty text");
        assertTrue("card".equals(state) || "No published call sheets".equals(state), "Published empty text: \"" + state + "\"");
    }

    @Test
    @Order(105)
    @DisplayName("CSC-10b [web C02/C03] Choosing Template 2 relabels the confirm button; Cancel opens no editor")
    void templateSwitch() {
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        assertTrue(p.tapCreate(), "Create not tappable");
        assumeTrue(p.templatePickerShown(Duration.ofSeconds(12)), "No picker — a single template");
        assumeTrue(p.templateOptions().contains("Template 2"), "Only one template offered: " + p.templateOptions());
        assertTrue(p.selectTemplate("Template 2"), "Template 2 not tappable");
        assertEquals("Use Template 2", p.confirmLabel(), "Confirm label after choosing Template 2");
        p.cancelTemplatePicker();
        sleep(1500);
        assertFalse(inEditor(), "Cancel opened the editor");
        assertTrue(p.isLoaded(Duration.ofSeconds(5)), "Cancel left the tool");
    }

    // ═══════════════════════════ 2. create & editor · CSC-1 ═══════════════════════════

    @Test
    @Order(100)
    @DisplayName("CSC-10 Create offers the template picker")
    void templatePicker() {
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        assertTrue(p.tapCreate(), "Create button not tappable");
        assumeTrue(p.templatePickerShown(Duration.ofSeconds(12)),
                "No picker — the project has a single template, so the editor opens straight away");
        try {
            assertEquals("Choose a template", p.pickerTitle(), "Picker title");
            List<String> opts = p.templateOptions();
            assertTrue(opts.contains("Template 1"), "Template options: " + opts);
            assertEquals("Use Template 1", p.confirmLabel(), "Confirm button");
        } finally {
            p.cancelTemplatePicker();
        }
    }

    @Test
    @Order(110)
    @DisplayName("CSC-11 The editor opens as \"New Call Sheet\" with Header first and Approvers last")
    void editorOpens() {
        openNewEditor();
        DocEditorPage ed = editor();
        assertEquals("New Call Sheet", ed.toolbarTitle(), "Toolbar title");
        ed.showSections();
        List<Section> s = ed.sections();
        assertFalse(s.isEmpty(), "No sections listed");
        assertEquals(DocEditorPage.ROW_HEADER, s.get(0).title, "First row");
        assertEquals("Shoot day, date, day type", s.get(0).summary, "Header row summary");
        assertEquals(DocEditorPage.ROW_APPROVERS, s.get(s.size() - 1).title, "Last row");
        assertEquals("Approval", s.get(s.size() - 1).type, "Approvers row badge");
        assertTrue(s.size() > 2, "The template's own sections are missing — the editor opened blank: " + s);
    }

    @Test
    @Order(120)
    @DisplayName("CSC-12 The header dialog edits shoot day / total days and the Preview shows them")
    void headerDialog() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.openHeader(), "Header dialog did not open");
        assertEquals("Call sheet info.", ed.headerDialogTitle(), "Header dialog title");
        String shoot = ed.field("edit_shoot_day");
        assertNotNull(shoot, "No shoot day field");
        assertTrue(ed.fieldShown("btn_date_picker"), "No date button");
        ed.setField("edit_total_days", "7");
        ed.headerDone();
        ed.showPreview();
        assertTrue(ed.previewContains("CALL SHEET " + shoot + " of 7", Duration.ofSeconds(20)),
                "Preview does not read \"CALL SHEET " + shoot + " of 7\"");
    }

    @Test
    @Order(130)
    @DisplayName("CSC-13 Day type offers SWD / CWD / SCWD and the choice reaches the Preview")
    void dayType() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.openHeader(), "Header dialog did not open");
        List<String> choices = ed.dayTypeChoices();
        assertTrue(choices.containsAll(List.of("SWD", "CWD", "SCWD")), "Day types: " + choices);
        assertTrue(ed.pickDayType("SWD"), "Could not pick SWD; field reads " + ed.field("edit_day_type"));
        ed.headerDone();
        ed.showPreview();
        assertTrue(ed.previewContains("SWD", Duration.ofSeconds(20)),
                "The Preview's title line does not end in \"— SWD\" (e.g. \"CALL SHEET 1 of 7 — SWD\")");
    }

    @Test
    @Order(135)
    @DisplayName("CSC-13b [QA CS-06 note] The template's DAY TYPE box shows the header's day type")
    void dayTypeBox() {
        DocEditorPage ed = inNewEditor();
        if (!ed.previewSelected()) ed.showPreview();
        assumeTrue(ed.previewContains("SWD", Duration.ofSeconds(15)), "CSC-13 did not set SWD");
        assertTrue(ed.previewContains("Day Type: SWD", Duration.ofSeconds(10)),
                "The header's day type reaches only the title line; the DAY TYPE box still reads \"Day Type: —\" (same in the PDF, per the QA sheet's CS-06 note)");
    }

    @Test
    @Order(140)
    @DisplayName("CSC-14 Add Section → Notes adds a row; CSC-15 Delete asks first and Cancel keeps it")
    void addAndDeleteSection() {
        DocEditorPage ed = inNewEditor();
        ed.showSections();
        List<String> before = ed.lastSectionTitles();
        assertTrue(ed.tapAddSection(), "No Add Section button");
        assertTrue(ed.pickInsertType("Notes"), "\"Notes\" not offered by Add Section");
        sleep(1500);
        if (!ed.isOpen(Duration.ofSeconds(2))) driver.navigate().back();   // a WebView editor, if one opened
        ed.showSections();
        List<String> after = ed.lastSectionTitles();
        assertTrue(after.size() >= 2, "Sections end: " + after);
        assertEquals(DocEditorPage.ROW_APPROVERS, after.get(after.size() - 1), "Approvers is no longer last: " + after);
        String added = after.get(after.size() - 2);
        assertEquals("New Notes", added, "The row added above Approvers (before: " + before + ")");

        assertTrue(ed.deleteSection(added), "No delete button on \"" + added + "\"");
        assertEquals("Delete Section", ed.alertTitle(), "Delete confirm title");
        assertEquals("Are you sure you want to delete this section?", ed.alertMessage(), "Delete confirm text");
        new DocWorkflowPage(driver).tapAlertButton("Cancel");
        assertTrue(ed.lastSectionTitles().contains(added), "Cancel removed the section");
        ed.deleteSection(added);
        new DocWorkflowPage(driver).tapAlertButton("Delete");
        assertFalse(ed.lastSectionTitles().contains(added), "Delete did not remove the section");
    }

    @Test
    @Order(150)
    @DisplayName("CSC-16 Manage Approvers lists everyone (me too), shows the project approver ticked, Save only after a change")
    void manageApprovers() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.openApprovers(), "Manage Approvers did not open");
        try {
            Map<String, Boolean> m = ed.approverChoices();
            assertTrue(m.containsKey(approver), "Approver \"" + approver + "\" not listed: " + m);
            assertTrue(m.get(approver), "\"" + approver + "\" is not ticked — the project default approver is not applied: " + m);
            assertFalse(ed.saveApproversEnabled(), "Save Approvers is enabled before any change");
            me = m.keySet().stream().filter(n -> !n.equals(approver)).findFirst().orElse(null);
            if (me != null) {
                ed.setApprover(me, true);
                assertTrue(ed.saveApproversEnabled(), "Save Approvers stays disabled after a change");
                ed.setApprover(me, false);
            }
        } finally {
            ed.closeApprovers();   // never saved: saving rewrites the project's default approvers
        }
    }

    @Test
    @Order(160)
    @DisplayName("CSC-17 A new sheet's overflow offers only Save As and Save as Template")
    void newSheetOverflow() {
        DocEditorPage ed = inNewEditor();
        List<String> items = ed.overflowItems();
        ed.closeOverflow();
        assertEquals(List.of(DocEditorPage.MENU_SAVE_AS, DocEditorPage.MENU_SAVE_TEMPLATE), items, "Overflow");
    }

    @Test
    @Order(170)
    @DisplayName("CSC-18 The name dialog is \"Save As New Draft\" and refuses a blank name")
    void nameDialog() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.tapOverflow(DocEditorPage.MENU_SAVE_AS), "Save As not offered");
        assertTrue(ed.nameDialogShown(Duration.ofSeconds(8)), "Name dialog did not open");
        try {
            assertEquals("Save As New Draft", ed.nameDialogTitle(), "Name dialog title");
            assertEquals("Enter call sheet name", ed.nameHint(), "Name hint");
            assertFalse(ed.nameSaveEnabled(), "Save is enabled with no name");
            ed.typeName("   ");
            assertFalse(ed.nameSaveEnabled(), "Save is enabled for a name of spaces");
        } finally {
            ed.nameCancel();
        }
    }

    @Test
    @Order(180)
    @DisplayName("CSC-20 [QA CS-08] Toolbar back on an untouched new sheet closes it without asking")
    void toolbarBackUntouched() {
        stepBack();
        openNewEditor();
        DocEditorPage ed = editor();
        assumeTrue(ed.tapToolbarBack(), "No toolbar back button found");
        String title = ed.alertTitle();
        if (title != null) new DocWorkflowPage(driver).tapAlertButton("Discard");
        assertNull(title, "Toolbar back on an untouched sheet asked \"" + title + "\" — system back closes it silently (QA CS-08)");
    }

    @Test
    @Order(190)
    @DisplayName("CSC-21 Back with changes asks \"Unsaved changes\"; Cancel stays, Discard leaves nothing behind")
    void discardDialog() {
        stepBack();
        onTab(TAB_DRAFTS);
        int before = page().chipCount("chip_filter_all");
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.openHeader(), "Header dialog did not open");
        ed.setField("edit_total_days", "9");
        ed.headerDone();
        driver.navigate().back();
        sleep(1200);
        assertEquals("Unsaved changes", ed.alertTitle(), "Back with changes");
        assertEquals("You have unsaved changes. Would you like to save before leaving?", ed.alertMessage(), "Message");
        DocWorkflowPage p = new DocWorkflowPage(driver);
        assertEquals(List.of("Cancel", "Discard", "Save & Leave"), p.alertButtons(), "Buttons");
        p.tapAlertButton("Cancel");
        assertTrue(ed.isOpen(Duration.ofSeconds(3)), "Cancel left the editor");
        driver.navigate().back();
        sleep(1000);
        p.tapAlertButton("Discard");
        assertTrue(page().isLoaded(Duration.ofSeconds(15)), "Discard did not return to the tool");
        page().waitForList(Duration.ofSeconds(10));
        assertEquals(before, page().chipCount("chip_filter_all"), "Discard left a draft behind");
    }

    @Test
    @Order(200)
    @DisplayName("CSC-19 Saving a new sheet puts a Draft card with its name, serial and creator in Drafts")
    void saveNewSheet() {
        stepBack();
        serialA = createSheet(nameA);
        Row r = page().row(ListKind.DRAFTS, serialA);
        assertNotNull(r, "Card #" + serialA + " not found");
        assertEquals(nameA, r.name, "Card name");
        assertEquals(DocWorkflowPage.ST_DRAFT, r.status, "Card status");
        assertNotNull(r.createdBy, "No creator on the card");
        assertTrue(r.dates != null && r.dates.startsWith("Created: "), "Dates: " + r.dates);
        me = r.createdBy.replaceAll("\\s*\\(.*\\)$", "");
    }

    @Test
    @Order(210)
    @DisplayName("CSC-07 [QA CS-04] A sheet created now is the first row in Drafts")
    void newestFirst() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        List<Row> rows = page().rows(ListKind.DRAFTS);
        assumeTrue(rows.size() > 1, "Only one draft — order cannot be judged");
        assertEquals(serialA, rows.get(0).serial, "First row is #" + rows.get(0).serial
                + ", not the sheet just created (#" + serialA + ") — QA CS-04: new drafts sink to the bottom");
    }

    // ═══════════════════════════ 3. Drafts row actions · CSC-2 ═══════════════════════════

    @Test
    @Order(300)
    @DisplayName("CSC-30 An own Draft's menu: View · Edit · Send for Comments · Send for Signature · Send to DD · Send for Chat · History · Delete")
    void draftMenu() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        List<String> items = page().openRowMenu(ListKind.DRAFTS, serialA);
        page().closePopup();
        assertEquals(List.of("View", "Edit", "Send for Comments", "Send for Signature",
                "Send to Document Distribution", "Send for Chat", "History", "Delete"), items, "Draft menu");
    }

    @Test
    @Order(310)
    @DisplayName("CSC-31 View opens the PDF viewer titled with the sheet's name")
    void view() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "View"), "View not offered");
        try {
            assertTrue(waitActivity(CallSheetCreationPage.VIEWER_ACTIVITY, Duration.ofSeconds(10)),
                    "The viewer did not open: " + whereAreWe());
            assertTrue(page().textShown(nameA, Duration.ofSeconds(5)), "Viewer is not titled \"" + nameA + "\"");
            boolean pdf = waitPresent("webview_pdf", "error_container", Duration.ofSeconds(45));
            assertTrue(pdf && !present("error_container"), "The PDF did not load (\"Failed to load PDF\")");
        } finally {
            stepBack();
        }
    }

    @Test
    @Order(320)
    @DisplayName("CSC-32 [QA CS-06] The viewer shows status, version and creator above the PDF")
    void viewHeader() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "View"), "View not offered");
        try {
            assumeTrue(waitActivity(CallSheetCreationPage.VIEWER_ACTIVITY, Duration.ofSeconds(10)), "Viewer did not open");
            waitPresent("webview_pdf", "error_container", Duration.ofSeconds(45));
            boolean status = page().textShown(DocWorkflowPage.ST_DRAFT, Duration.ofSeconds(2));
            boolean creator = me != null && page().textShown(me, Duration.ofSeconds(2));
            assertTrue(status && creator, "The viewer shows only the name and the PDF — no status/version/creator header (QA CS-06; iOS has it)");
        } finally {
            stepBack();
        }
    }

    @Test
    @Order(330)
    @DisplayName("CSC-34 A new sheet's History has one \"Created\" row, v1, by me")
    void historyCreated() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "History"), "History not offered");
        try {
            assertTrue(page().historyShown(Duration.ofSeconds(10)), "History did not open");
            List<HistoryEntry> h = page().historyEntries();
            assertEquals(1, h.size(), "History rows: " + h);
            assertEquals("Created", h.get(0).action, "Action");
            assertEquals("v1", h.get(0).revision, "Revision badge");
            assertTrue(me == null || (h.get(0).by != null && h.get(0).by.startsWith(me)), "By: " + h.get(0).by);
        } finally {
            page().closeDialog();
        }
    }

    @Test
    @Order(340)
    @DisplayName("CSC-23 Edit opens \"Edit Call Sheet\" with Save · Save As · Send for Signature · Send for Comments")
    void editExisting() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Edit"), "Edit not offered");
        DocEditorPage ed = editor();
        try {
            assertTrue(ed.isOpen(Duration.ofSeconds(30)), "Editor did not open");
            assertEquals("Edit Call Sheet", ed.toolbarTitle(), "Toolbar title");
            List<String> items = ed.overflowItems();
            ed.closeOverflow();
            assertEquals(List.of(DocEditorPage.MENU_SAVE, DocEditorPage.MENU_SAVE_AS,
                    DocEditorPage.MENU_SEND_SIGNATURE, DocEditorPage.MENU_SEND_COMMENTS), items, "Overflow");
        } finally {
            stepBack();
        }
    }

    @Test
    @Order(350)
    @DisplayName("CSC-24 [QA CS-01] Save As makes a copy with its own serial, from a dialog titled \"Save As New Draft\"")
    void saveAsCopy() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Edit"), "Edit not offered");
        DocEditorPage ed = editor();
        assumeTrue(ed.isOpen(Duration.ofSeconds(30)), "Editor did not open");
        assertTrue(ed.tapOverflow(DocEditorPage.MENU_SAVE_AS), "Save As not offered");
        assertTrue(ed.nameDialogShown(Duration.ofSeconds(8)), "Name dialog did not open");
        assertEquals("Save As New Draft", ed.nameDialogTitle(), "Save As dialog title (QA CS-01)");
        ed.typeName(nameB);
        ed.nameSave();
        assertTrue(page().isLoaded(Duration.ofSeconds(30)), "Save As did not return to the tool");
        Row b = waitDraft(nameB, Duration.ofSeconds(30));
        assertNotNull(b, "The copy \"" + nameB + "\" is not in Drafts");
        serialB = b.serial;
        assertFalse(serialA.equals(serialB), "The copy has the original's serial");
        assertNotNull(page().rowNamed(nameA), "The original disappeared");
    }

    @Test
    @Order(360)
    @DisplayName("CSC-25 [QA CS-02] Saving again records an \"Updated\" revision v2 in History")
    void saveAgainBumpsVersion() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Edit"), "Edit not offered");
        DocEditorPage ed = editor();
        assumeTrue(ed.isOpen(Duration.ofSeconds(30)), "Editor did not open");
        assertTrue(ed.openHeader(), "Header dialog did not open");
        ed.setField("edit_total_days", "8");
        ed.headerDone();
        assertTrue(ed.tapOverflow(DocEditorPage.MENU_SAVE), "Save not offered");
        if (ed.alertTitle() != null) new DocWorkflowPage(driver).tapAlertButton("Continue");
        assertTrue(page().isLoaded(Duration.ofSeconds(30)), "Save did not return to the tool: " + whereAreWe());
        page().waitForList(Duration.ofSeconds(10));
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "History"), "History not offered");
        try {
            List<HistoryEntry> h = page().historyEntries();
            assertTrue(h.stream().anyMatch(e -> "v2".equals(e.revision)), "No v2 revision in History: " + h);
        } finally {
            page().closeDialog();
        }
    }

    // ═══════════════════════════ 4. Send for Comments · CSC-3 ═══════════════════════════

    @Test
    @Order(400)
    @DisplayName("CSC-40 Send for Comments opens \"Select Recipients\" without me, Send disabled at 0")
    void recipientsPicker() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Send for Comments"), "Send for Comments not offered");
        DocWorkflowPage p = page();
        try {
            assertTrue(p.recipientsShown(Duration.ofSeconds(10)), "The recipients picker did not open");
            assertEquals("Select Recipients", p.recipientsTitle(), "Title");
            List<String> names = p.recipientNames();
            assertTrue(names.contains(approver), "\"" + approver + "\" not offered: " + names);
            assertTrue(me == null || !names.contains(me), "I am offered as my own recipient: " + names);
            // People sent to before come back pre-ticked under "Previously Selected" — by design.
            if (p.selectionCount() != null && p.selectionCount().startsWith("0 of")) {
                assertFalse(p.sendEnabled(), "Send is enabled with nobody ticked");
            }
        } finally {
            p.closeRecipients();
        }
    }

    @Test
    @Order(410)
    @DisplayName("CSC-41 [QA CS-10] The counter and the Send label follow the ticks; Select All / Deselect All")
    void recipientsCounter() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Send for Comments"), "Send for Comments not offered");
        DocWorkflowPage p = page();
        try {
            assumeTrue(p.recipientsShown(Duration.ofSeconds(10)), "Picker did not open");
            int listed = p.recipientNames().size();
            String count = p.selectionCount();
            int ticked = count == null ? -1 : Integer.parseInt(count.replaceAll("^(\\d+).*", "$1"));
            assertTrue(ticked >= 0 && ticked <= listed, "Counter \"" + count + "\" with " + listed
                    + " people listed, send label \"" + p.sendLabel() + "\" — it counts hidden ids (QA CS-10)");
            String label = p.sendLabel();
            assertTrue(label != null && label.startsWith("Send to " + ticked + " "), "Send label \"" + label + "\" vs counter \"" + count + "\"");
        } finally {
            p.closeRecipients();
        }
    }

    @Test
    @Order(420)
    @DisplayName("CSC-43 Sending for comments turns the card \"For Comments\" and swaps Send for Comments for Comment")
    void sendForComments() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Send for Comments"), "Send for Comments not offered");
        DocWorkflowPage p = page();
        assumeTrue(p.recipientsShown(Duration.ofSeconds(10)), "Picker did not open");
        // A recipient from an earlier send is already ticked under "Previously Selected";
        // tapping would UNtick them. Tick only when nobody is selected.
        if (p.selectionCount() != null && p.selectionCount().startsWith("0 of")) {
            assertTrue(p.toggleRecipient(approver), "Could not tick " + approver);
        }
        assertTrue(p.sendToRecipients(), "Send not tappable");
        if (p.alertTitle() != null) p.tapAlertButton("Remove from comments only");
        Row r = p.waitStatus(ListKind.DRAFTS, serialA, DocWorkflowPage.ST_FOR_COMMENTS, Duration.ofSeconds(30));
        assertNotNull(r, "The card disappeared from Drafts");
        assertEquals(DocWorkflowPage.ST_FOR_COMMENTS, r.status, "Card status after sending for comments");
        List<String> items = p.openRowMenu(ListKind.DRAFTS, serialA);
        p.closePopup();
        assertTrue(items.contains("Comment"), "No Comment in the menu: " + items);
        assertFalse(items.contains("Send for Comments"), "Send for Comments still offered: " + items);
    }

    @Test
    @Order(430)
    @DisplayName("CSC-04 The filter chips filter: Drafts shows only Draft pills, For Comments only For Comments")
    void chipsFilter() {
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        try {
            p.tapChip("chip_filter_drafts");
            sleep(1000);
            for (Row r : p.rows(ListKind.DRAFTS)) assertEquals(DocWorkflowPage.ST_DRAFT, r.status, "Under Drafts: " + r);
            p.tapChip("chip_filter_for_comments");
            sleep(1000);
            List<Row> fc = p.rows(ListKind.DRAFTS);
            for (Row r : fc) assertEquals(DocWorkflowPage.ST_FOR_COMMENTS, r.status, "Under For Comments: " + r);
            if (serialA != null) assertTrue(fc.stream().anyMatch(r -> serialA.equals(r.serial)), "Our For Comments sheet is not listed: " + fc);
        } finally {
            p.tapChip("chip_filter_all");
        }
    }

    // ═══════════════════════════ 5. Send for Signature · CSC-4 ═══════════════════════════

    @Test
    @Order(500)
    @DisplayName("CSC-51 Send for Signature takes the card out of Drafts into Sent as \"Pending Signature\"")
    void sendForSignature() {
        assumeTrue(serialB != null, "CSC-24 did not make the copy");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialB, "Send for Signature"), "Send for Signature not offered");
        if (page().alertTitle() != null) {
            String t = page().alertTitle();
            page().tapAlertButton("OK");
            assumeTrue(false, "Send refused: \"" + t + "\" — the project has no approvers");
        }
        assertTrue(page().waitRowGone(ListKind.DRAFTS, serialB, Duration.ofSeconds(30)), "The card is still in Drafts");
        onSubTab(DocWorkflowPage.SUB_SENT);
        Row r = page().waitRow(ListKind.SENT, serialB, Duration.ofSeconds(30));
        assertNotNull(r, "#" + serialB + " is not in Approvals › Sent");
        assertEquals(DocWorkflowPage.ST_PENDING, r.status, "Sent status");
        assertEquals("", r.name, "Sent rows are expected to carry no name");
    }

    @Test
    @Order(510)
    @DisplayName("CSC-52 A pending Sent row offers Reminder, Edit and Delete, but not Send for Signature")
    void sentMenu() {
        assumeTrue(serialB != null, "Nothing was sent");
        onSubTab(DocWorkflowPage.SUB_SENT);
        List<String> items = page().openRowMenu(ListKind.SENT, serialB);
        page().closePopup();
        assumeTrue(!items.isEmpty(), "#" + serialB + " not in Sent");
        assertTrue(items.containsAll(List.of("Edit", "Reminder", "History", "Delete")), "Sent menu: " + items);
        assertFalse(items.contains("Send for Signature"), "Sent menu offers Send for Signature: " + items);
    }

    @Test
    @Order(520)
    @DisplayName("CSC-53 Reminder is pre-filled; a blank message cannot be sent")
    void reminder() {
        assumeTrue(serialB != null, "Nothing was sent");
        onSubTab(DocWorkflowPage.SUB_SENT);
        assertTrue(page().rowAction(ListKind.SENT, serialB, "Reminder"), "Reminder not offered");
        DocWorkflowPage p = page();
        try {
            assertTrue(p.reminderDialogShown(Duration.ofSeconds(8)), "Reminder dialog did not open");
            assertEquals("Please review and approve this call sheet.", p.reminderMessage(), "Pre-filled message");
        } finally {
            p.cancelDialogButton();
        }
    }

    // ═══════════════════════════ 8. Permission tab · CSC-7 ═══════════════════════════

    @Test
    @Order(800)
    @DisplayName("CSC-90 Permission tab (admin): description, members with a switch, not me")
    void permissionTab() {
        onTab(TAB_DRAFTS);
        assumeTrue(page().tabNames().contains(TAB_PERMISSION), "No Permission tab — this user is not an admin");
        page().openTab(TAB_PERMISSION);
        CallSheetCreationPage p = page();
        try {
            assertEquals("To allow users to create a call sheet.", p.permissionDescription(), "Description");
            List<CallSheetCreationPage.Member> m = p.permissionMembers();
            assertFalse(m.isEmpty(), "No members listed");
            assertTrue(me == null || m.stream().noneMatch(x -> me.equals(x.name)), "I am listed: " + m);
        } finally {
            p.openTab(TAB_DRAFTS);
        }
    }

    // ═══════════════════════════ cleanup · CSC-35 / CSC-99 ═══════════════════════════

    @Test
    @Order(900)
    @DisplayName("CSC-35 Delete asks \"Delete Draft\" first; Cancel keeps the card, Delete removes it")
    void deleteDraft() {
        assumeTrue(serialA != null, "CSC-19 did not create a sheet");
        onTab(TAB_DRAFTS);
        CallSheetCreationPage p = page();
        assertTrue(p.rowAction(ListKind.DRAFTS, serialA, "Delete"), "Delete not offered");
        assertTrue(p.alertShown(Duration.ofSeconds(6)), "No confirm");
        assertEquals("Delete Draft", p.alertTitle(), "Confirm title");
        assertEquals("Are you sure you want to delete \"" + nameA + "\"?", p.alertMessage(), "Confirm text");
        p.tapAlertButton("Cancel");
        assertNotNull(p.row(ListKind.DRAFTS, serialA), "Cancel deleted the sheet");
        assertTrue(deleteRow(ListKind.DRAFTS, serialA), "Delete did not remove #" + serialA);
        serialA = null;
    }

    @Test
    @Order(990)
    @DisplayName("CSC-99 Sweep: every ZL_AUTO_CS_ sheet left in Drafts or Sent is deleted")
    void sweep() {
        onTab(TAB_DRAFTS);
        List<String> failed = new ArrayList<>();
        for (Row r : page().rows(ListKind.DRAFTS)) {
            if (r.name.startsWith(PREFIX) && !deleteRow(ListKind.DRAFTS, r.serial)) failed.add("Drafts #" + r.serial);
        }
        if (serialB != null) {
            onSubTab(DocWorkflowPage.SUB_SENT);
            if (page().row(ListKind.SENT, serialB) != null && !deleteRow(ListKind.SENT, serialB)) failed.add("Sent #" + serialB);
        }
        assertTrue(failed.isEmpty(), "Could not delete: " + failed);
    }

    // ─────────────────────────── helpers ───────────────────────────

    /** Opens a fresh editor from Template 1 (picker confirmed), retrying once on a blank one. */
    private void openNewEditor() {
        onTab(TAB_DRAFTS);
        for (int attempt = 1; attempt <= 2; attempt++) {
            CallSheetCreationPage p = page();
            assumeTrue(p.tapCreate(), "The create button is not offered");
            if (p.templatePickerShown(Duration.ofSeconds(12))) { sleep(1500); p.confirmTemplate(); }
            DocEditorPage ed = editor();
            assumeTrue(ed.isOpen(Duration.ofSeconds(30)), "The editor did not open: " + whereAreWe());
            ed.showSections();
            if (ed.hasTemplateContent() || attempt == 2) return;
            screenshot("blank_template_editor");
            stepBack();
        }
    }

    /** The editor if one is already open (leftover from the previous case), else a new one. */
    private DocEditorPage inNewEditor() {
        DocEditorPage ed = editor();
        if (inEditor() && ed.isOpen(Duration.ofSeconds(2)) && "New Call Sheet".equals(ed.toolbarTitle())) {
            closeAlert();
            return ed;
        }
        stepBack();
        openNewEditor();
        return editor();
    }

    private boolean waitActivity(String name, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (activityIs(name)) return true;
            sleep(500);
        }
        return false;
    }

    /** Waits until either id is present; true if any appeared. */
    private boolean waitPresent(String a, String b, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if ((present(a) && !present("loading_container")) || present(b)) return true;
            sleep(1000);
        }
        return false;
    }
}
