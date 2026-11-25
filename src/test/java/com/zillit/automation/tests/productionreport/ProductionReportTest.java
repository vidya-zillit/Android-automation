package com.zillit.automation.tests.productionreport;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.productionreport.ProductionReportPage;
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
import static com.zillit.automation.pages.workflow.DocWorkflowPage.TAB_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PRODUCTION REPORT CREATION — the creator's side on ONE device, on the same workflow page
 * objects as Call Sheet Creation: the dashboard, the template picker and "No Call Sheet Found",
 * the editor's native parts (header with Crew Call / Unit Wrap, approvers, overflow, name and
 * discard dialogs), Drafts actions, the "Missing Call Times" guard, Send for Signature as far as
 * the creator sees it, and a sweep. Cases: {@code PRODUCTION_REPORT_CREATION_TESTCASES.md}.
 *
 * <p>Every report is named {@code ZL_AUTO_PR_<run>…} and swept by PRC-99 from Drafts and Sent.
 * Cases marked <b>[QA PR-nn]</b> re-check the QA module testing report.
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.PRODUCTION_REPORT)
class ProductionReportTest extends ProductionReportBaseTest {

    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    private final String nameA = PREFIX + RUN + "_A";   // no call times — cannot be sent
    private final String nameB = PREFIX + RUN + "_B";   // with call times — sent for signature
    private final String approver = System.getProperty("zillit.productionreport.approver", "Vidya Pixel");

    private String serialA, serialB, me;

    // ═══════════════════════════ 0. entry · PRC-0 ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("PRC-00/01 The tile opens the dashboard: Drafts · Approvals · Published, create button on Drafts")
    void shell() {
        openTool();
        ProductionReportPage p = page();
        assertEquals(ProductionReportPage.HEADER, p.header(), "Header");
        List<String> tabs = p.tabNames();
        assertTrue(tabs.size() >= 3 && tabs.subList(0, 3).equals(List.of(TAB_DRAFTS, TAB_APPROVALS, TAB_PUBLISHED)), "Tabs: " + tabs);
        assertTrue(p.tabSelected(TAB_DRAFTS), "Drafts not selected on opening");
        assertTrue(p.createShown(), "No create button for a creator with posting rights");
        assertEquals("Create Production Report", p.createDesc(), "Create button content-desc");
    }

    @Test
    @Order(20)
    @DisplayName("PRC-03 The filter chips read All / Drafts / For Comments and add up")
    void chipsAddUp() {
        onTab(TAB_DRAFTS);
        ProductionReportPage p = page();
        int all = p.chipCount("chip_filter_all");
        assertEquals(all, p.chipCount("chip_filter_drafts") + p.chipCount("chip_filter_for_comments"), "All ≠ Drafts + For Comments");
        assertEquals(all, p.rows(ListKind.DRAFTS).size(), "All chip vs rows listed");
    }

    @Test
    @Order(40)
    @DisplayName("PRC-02 [web S02/S03] Tabs and the Approvals sub-tabs switch without leaving the tool")
    void tabsAndSubTabs() {
        onTab(TAB_DRAFTS);
        ProductionReportPage p = page();
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
    @DisplayName("PRC-84 [web P03/P05] Published shows the \"Currently Published\" card or \"No published production reports\"")
    void publishedTab() {
        onTab(TAB_PUBLISHED);
        String state = page().publishedState(Duration.ofSeconds(20));
        page().openTab(TAB_DRAFTS);
        assertNotNull(state, "Published shows neither a card nor its empty text");
        assertTrue("card".equals(state) || "No published production reports".equals(state), "Published empty text: \"" + state + "\"");
    }

    @Test
    @Order(105)
    @DisplayName("PRC-10b [web C02/C03] Choosing Template 2 relabels the confirm button; Cancel opens no editor")
    void templateSwitch() {
        onTab(TAB_DRAFTS);
        ProductionReportPage p = page();
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

    // ═══════════════════════════ 1. create & editor · PRC-1 ═══════════════════════════

    @Test
    @Order(100)
    @DisplayName("PRC-10 Create offers the template picker")
    void templatePicker() {
        onTab(TAB_DRAFTS);
        ProductionReportPage p = page();
        assertTrue(p.tapCreate(), "Create button not tappable");
        assumeTrue(p.templatePickerShown(Duration.ofSeconds(12)), "No picker — a single template");
        try {
            assertEquals("Choose a template", p.pickerTitle(), "Picker title");
            assertTrue(p.templateOptions().contains("Template 1"), "Options: " + p.templateOptions());
            assertEquals("Use Template 1", p.confirmLabel(), "Confirm button");
        } finally {
            p.cancelTemplatePicker();
        }
    }

    @Test
    @Order(110)
    @DisplayName("PRC-11/12 A new report warns \"No Call Sheet Found\" (none published), then opens as \"New Production Report\"")
    void editorOpens() {
        onTab(TAB_DRAFTS);
        ProductionReportPage p = page();
        assertTrue(p.tapCreate(), "Create not tappable");
        if (p.templatePickerShown(Duration.ofSeconds(12))) { sleep(1500); p.confirmTemplate(); }
        if (p.alertShown(Duration.ofSeconds(10))) {
            assertEquals("No Call Sheet Found", p.alertTitle(), "Alert title");
            assertTrue(String.valueOf(p.alertMessage()).startsWith("No published call sheet was found for the shoot day."),
                    "Alert text: " + p.alertMessage());
            p.tapAlertButton("OK");
        }
        DocEditorPage ed = editor();
        assertTrue(ed.isOpen(Duration.ofSeconds(30)), "Editor did not open");
        assertEquals("New Production Report", ed.toolbarTitle(), "Toolbar title");
        ed.showSections();
        List<Section> s = ed.sections();
        assertEquals(DocEditorPage.ROW_HEADER, s.get(0).title, "First row");
        assertEquals(DocEditorPage.ROW_APPROVERS, s.get(s.size() - 1).title, "Last row");
        assertTrue(s.size() > 2, "The editor opened WITHOUT the template's content (finding 7): " + s);
    }

    @Test
    @Order(120)
    @DisplayName("PRC-13/14 The header has Crew Call / Unit Wrap; the picker stores 24-hour times")
    void headerCallTimes() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.openHeader(), "Header dialog did not open");
        try {
            assertEquals("Production Report info.", ed.headerDialogTitle(), "Header dialog title");
            assertTrue(ed.fieldShown("btn_crew_call") && ed.fieldShown("btn_unit_wrap"), "No Crew Call / Unit Wrap on this template");
            assertEquals("SELECT CREW CALL TIME", ed.field("btn_crew_call"), "Crew Call before picking");
            tapIdOnce("btn_crew_call");
            tapIdOnce("material_timepicker_ok_button");
            String v = ed.field("btn_crew_call");
            assertTrue(v != null && v.matches("\\d{2}:\\d{2}"), "Crew Call reads \"" + v + "\" — expected 24-hour HH:mm");
        } finally {
            ed.headerCancel();
        }
    }

    @Test
    @Order(130)
    @DisplayName("PRC-16 Manage Approvers lists me too, shows the project approver ticked [finding: says \"call sheet\"]")
    void manageApprovers() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.openApprovers(), "Manage Approvers did not open");
        try {
            Map<String, Boolean> m = ed.approverChoices();
            assertTrue(Boolean.TRUE.equals(m.get(approver)), "\"" + approver + "\" not ticked: " + m);
            assertTrue(m.size() >= 2, "The current user is expected in the list too: " + m);
            assertFalse(ed.saveApproversEnabled(), "Save Approvers enabled before a change");
            assertFalse(ed.textShown("approve this call sheet", Duration.ofSeconds(2)),
                    "Manage Approvers says \"Select who needs to approve this call sheet\" in Production Report");
        } finally {
            ed.closeApprovers();
        }
    }

    @Test
    @Order(140)
    @DisplayName("PRC-17 A new report's overflow offers only Save As and Save as Template")
    void newOverflow() {
        DocEditorPage ed = inNewEditor();
        List<String> items = ed.overflowItems();
        ed.closeOverflow();
        assertEquals(List.of(DocEditorPage.MENU_SAVE_AS, DocEditorPage.MENU_SAVE_TEMPLATE), items, "Overflow");
    }

    @Test
    @Order(150)
    @DisplayName("PRC-18 [QA PR-01/PR-15] Name dialog: \"Save As New Draft\", blank refused, production-report hint")
    void nameDialog() {
        DocEditorPage ed = inNewEditor();
        assertTrue(ed.tapOverflow(DocEditorPage.MENU_SAVE_AS), "Save As not offered");
        assertTrue(ed.nameDialogShown(Duration.ofSeconds(8)), "Name dialog did not open");
        String hint;
        try {
            assertEquals("Save As New Draft", ed.nameDialogTitle(), "Title");
            assertFalse(ed.nameSaveEnabled(), "Save enabled with no name");
            hint = ed.nameHint();
        } finally {
            ed.nameCancel();
        }
        assertFalse(String.valueOf(hint).toLowerCase().contains("call sheet"), "Name hint reads \"" + hint + "\" (QA PR-15)");
    }

    @Test
    @Order(160)
    @DisplayName("PRC-20 [QA PR-11] Toolbar back on an untouched new report closes it without asking")
    void toolbarBackUntouched() {
        stepBack();
        DocEditorPage ed = inNewEditor();
        assumeTrue(ed.tapToolbarBack(), "No toolbar back button");
        String title = ed.alertTitle();
        if (title != null) new DocWorkflowPage(driver).tapAlertButton("Discard");
        assertNull(title, "Toolbar back on an untouched report asked \"" + title + "\" (QA PR-11)");
    }

    @Test
    @Order(200)
    @DisplayName("PRC-19 Saving a report without call times is allowed: a Draft card with name and serial")
    void saveWithoutTimes() {
        stepBack();
        serialA = createReport(nameA);
        Row r = page().row(ListKind.DRAFTS, serialA);
        assertNotNull(r, "#" + serialA + " not found");
        assertEquals(nameA, r.name, "Name");
        assertEquals(DocWorkflowPage.ST_DRAFT, r.status, "Status");
        me = r.createdBy == null ? null : r.createdBy.replaceAll("\\s*\\(.*\\)$", "");
    }

    @Test
    @Order(210)
    @DisplayName("PRC-30 An own Draft's menu: View · Edit · Send for Comments · Send for Signature · Send to DD · Send for Chat · History · Delete")
    void draftMenu() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        List<String> items = page().openRowMenu(ListKind.DRAFTS, serialA);
        page().closePopup();
        assertEquals(List.of("View", "Edit", "Send for Comments", "Send for Signature",
                "Send to Document Distribution", "Send for Chat", "History", "Delete"), items, "Draft menu");
    }

    @Test
    @Order(220)
    @DisplayName("PRC-40/51 Sending a report without Crew Call / Unit Wrap is refused with \"Missing Call Times\"")
    void missingCallTimes() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Send for Signature"), "Send for Signature not offered");
        ProductionReportPage p = page();
        assertTrue(p.alertShown(Duration.ofSeconds(8)), "No refusal — a report without call times was sent");
        try {
            assertEquals("Missing Call Times", p.alertTitle(), "Alert title");
            assertEquals("Please set Crew Call and Unit Wrap in the Call Times section before sending for approval.",
                    p.alertMessage(), "Alert text");
        } finally {
            p.tapAlertButton("OK");
        }
        assertNotNull(p.row(ListKind.DRAFTS, serialA), "The report left Drafts");
    }

    @Test
    @Order(230)
    @DisplayName("PRC-32 [QA PR-16] View opens the PDF within 15 s")
    void viewFast() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "View"), "View not offered");
        try {
            long t0 = System.currentTimeMillis();
            boolean loaded = false;
            while (System.currentTimeMillis() - t0 < 60000) {
                if (present("error_container")) break;
                if (present("webview_pdf") && !present("loading_container")) { loaded = true; break; }
                sleep(1000);
            }
            long secs = (System.currentTimeMillis() - t0) / 1000;
            assertTrue(loaded, "The PDF did not load in 60 s (or \"Failed to load PDF\")");
            assertTrue(secs <= 15, "The PDF took " + secs + " s to show (QA PR-16: ~40 s spinner, fetched twice)");
        } finally {
            stepBack();
        }
    }

    @Test
    @Order(240)
    @DisplayName("PRC-34 A new report's History has one \"Created\" row, v1, with the creator's role")
    void historyCreated() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "History"), "History not offered");
        try {
            List<HistoryEntry> h = page().historyEntries();
            assertEquals(1, h.size(), "History rows: " + h);
            assertEquals("Created", h.get(0).action, "Action");
            assertEquals("v1", h.get(0).revision, "Revision");
            assertTrue(String.valueOf(h.get(0).by).matches(".+\\(.+\\)"), "No role in \"" + h.get(0).by + "\" (QA PR-06)");
        } finally {
            page().closeDialog();
        }
    }

    @Test
    @Order(250)
    @DisplayName("PRC-23 Edit opens \"Edit Production Report\" with Save · Save As · Send for Signature · Send for Comments")
    void editExisting() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "Edit"), "Edit not offered");
        DocEditorPage ed = editor();
        try {
            assertTrue(ed.isOpen(Duration.ofSeconds(30)), "Editor did not open");
            assertEquals("Edit Production Report", ed.toolbarTitle(), "Toolbar title");
            List<String> items = ed.overflowItems();
            ed.closeOverflow();
            assertEquals(List.of(DocEditorPage.MENU_SAVE, DocEditorPage.MENU_SAVE_AS,
                    DocEditorPage.MENU_SEND_SIGNATURE, DocEditorPage.MENU_SEND_COMMENTS), items, "Overflow");
        } finally {
            stepBack();
        }
    }

    @Test
    @Order(260)
    @DisplayName("PRC-24 [web D03/E01] Edit → change → Save writes revision v2 without a second row")
    void saveRevision() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        int before = page().chipCount("chip_filter_all");
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
        assertEquals(before, page().chipCount("chip_filter_all"), "Saving made a second row");
        assertTrue(page().rowAction(ListKind.DRAFTS, serialA, "History"), "History not offered");
        try {
            List<HistoryEntry> h = page().historyEntries();
            assertTrue(h.stream().anyMatch(e -> "v2".equals(e.revision)), "No v2 revision in History: " + h);
        } finally {
            page().closeDialog();
        }
    }

    // ═══════════════════════════ 4. Send for Signature · PRC-4 ═══════════════════════════

    @Test
    @Order(400)
    @DisplayName("PRC-52 With call times set, Send for Signature confirms and moves the report to Sent \"Pending Signature\"")
    void sendForSignature() {
        stepBack();
        serialB = createReport(nameB, true);
        onTab(TAB_DRAFTS);
        assertTrue(page().rowAction(ListKind.DRAFTS, serialB, "Send for Signature"), "Send for Signature not offered");
        ProductionReportPage p = page();
        // the snackbar is gone within ~3 s — look for it before anything else
        boolean confirmed = p.messageShown(ProductionReportPage.SENT_OK, Duration.ofSeconds(6));
        if (!confirmed && p.alertTitle() != null) {
            String t = p.alertTitle();
            p.tapAlertButton("OK");
            assertTrue(false, "Send refused: \"" + t + "\"");
        }
        assertTrue(confirmed, "No \"" + ProductionReportPage.SENT_OK + "\" confirmation");
        assertTrue(p.waitRowGone(ListKind.DRAFTS, serialB, Duration.ofSeconds(30)), "Still in Drafts");
        onSubTab(DocWorkflowPage.SUB_SENT);
        Row r = page().waitRow(ListKind.SENT, serialB, Duration.ofSeconds(30));
        assertNotNull(r, "#" + serialB + " not in Sent");
        assertEquals(DocWorkflowPage.ST_PENDING, r.status, "Sent status");
    }

    @Test
    @Order(410)
    @DisplayName("PRC-53/54 A pending Sent row offers Reminder; the reminder needs a message")
    void sentMenuAndReminder() {
        assumeTrue(serialB != null, "Nothing was sent");
        onSubTab(DocWorkflowPage.SUB_SENT);
        List<String> items = page().openRowMenu(ListKind.SENT, serialB);
        page().closePopup();
        assertTrue(items.containsAll(List.of("Edit", "Reminder", "History", "Delete")), "Sent menu: " + items);
        assertFalse(items.contains("Send for Signature"), "Sent offers Send for Signature: " + items);
        assertTrue(page().rowAction(ListKind.SENT, serialB, "Reminder"), "Reminder not offered");
        try {
            assertTrue(page().reminderDialogShown(Duration.ofSeconds(8)), "Reminder dialog did not open");
        } finally {
            page().cancelDialogButton();
        }
    }

    // ═══════════════════════════ cleanup ═══════════════════════════

    @Test
    @Order(900)
    @DisplayName("PRC-33 Delete asks \"Delete Draft\"; Cancel keeps, Delete removes")
    void deleteDraft() {
        assumeTrue(serialA != null, "PRC-19 did not create a report");
        onTab(TAB_DRAFTS);
        ProductionReportPage p = page();
        assertTrue(p.rowAction(ListKind.DRAFTS, serialA, "Delete"), "Delete not offered");
        assertTrue(p.alertShown(Duration.ofSeconds(6)), "No confirm");
        assertEquals("Delete Draft", p.alertTitle(), "Confirm title");
        p.tapAlertButton("Cancel");
        assertNotNull(p.row(ListKind.DRAFTS, serialA), "Cancel deleted it");
        assertTrue(deleteRow(ListKind.DRAFTS, serialA), "Delete did not remove #" + serialA);
        serialA = null;
    }

    @Test
    @Order(990)
    @DisplayName("PRC-99 Sweep: every ZL_AUTO_PR_ report left in Drafts or Sent is deleted")
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

    private void openNewEditor() {
        onTab(TAB_DRAFTS);
        for (int attempt = 1; attempt <= 2; attempt++) {
            ProductionReportPage p = page();
            assumeTrue(p.tapCreate(), "The create button is not offered");
            if (p.templatePickerShown(Duration.ofSeconds(12))) { sleep(1500); p.confirmTemplate(); }
            dismissNoCallSheet();
            DocEditorPage ed = editor();
            assumeTrue(ed.isOpen(Duration.ofSeconds(30)), "The editor did not open: " + whereAreWe());
            ed.showSections();
            if (ed.hasTemplateContent() || attempt == 2) return;
            screenshot("blank_template_editor");
            stepBack();
        }
    }

    private DocEditorPage inNewEditor() {
        DocEditorPage ed = editor();
        if (inEditor() && ed.isOpen(Duration.ofSeconds(2)) && "New Production Report".equals(ed.toolbarTitle())) {
            closeAlert();
            return ed;
        }
        stepBack();
        openNewEditor();
        return editor();
    }
}
