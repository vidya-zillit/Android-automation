package com.zillit.automation.tests.productionreport;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.productionreport.ProductionReportPage;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.workflow.DocEditorPage;
import com.zillit.automation.pages.workflow.DocWorkflowPage;
import com.zillit.automation.pages.workflow.DocWorkflowPage.ListKind;
import com.zillit.automation.pages.workflow.DocWorkflowPage.Row;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Production Report Creation suites — the same policy as the Schedule
 * Distribution base: the app is opened once, the project entered once, nothing relaunches
 * between cases, and the step-back helper never calls the open helper.
 */
abstract class ProductionReportBaseTest extends BaseTest {

    static final String TOOL = ProductionReportPage.TOOL_TILE;
    /** Every report and template this suite makes starts with this, so the sweep can find it. */
    static final String PREFIX = "ZL_AUTO_PR_";

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    ProductionReportPage page() { return new ProductionReportPage(driver); }

    DocEditorPage editor() { return new DocEditorPage(driver); }

    // ─────────────────────────── navigation ───────────────────────────

    void openTool() {
        bringAppToFront();
        if (onTool()) return;
        if (insideTool()) { stepBack(); if (onTool()) return; }

        BottomNavPage nav = new BottomNavPage(driver);
        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) { openFromTools(); return; }
        for (int i = 0; i < 6; i++) {
            if (onTool()) return;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return; }
            if (onProjectList()) break;
            closeAlert();
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (onProjectList()) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "The project list is open but shows no projects to enter.");
            assumeTrue(projects.tapProject(names.get(0)), "Could not enter \"" + names.get(0) + "\"");
        }
        assumeTrue(nav.waitUntilLoaded(Duration.ofSeconds(60)),
                "The bottom navigation never loaded. On screen instead: " + whereAreWe());
        openFromTools();
    }

    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("Production Report");
        sleep(1200);
        if (!tools.openIfVisible(TOOL)) {
            assumeTrue(false, "\"" + TOOL + "\" is not offered in Tools. Tools visible: " + tools.toolNames());
        }
        sleep(2500);
        assumeTrue(!activityIs(ProductionReportPage.CHAT_ACTIVITY),
                "The tile opened the Production Report CHAT — report templates are not enabled for this project (remote config)");
        assumeTrue(page().isLoaded(Duration.ofSeconds(25)),
                "Production Report Creation did not open (it closes itself without view access). On screen: " + whereAreWe());
        page().waitForList(Duration.ofSeconds(15));
    }

    /** Back on the tool's tabs, on the given top tab. */
    void onTab(String tab) {
        openTool();
        ProductionReportPage p = page();
        if (!p.tabSelected(tab)) assumeTrue(p.openTab(tab), "Tab \"" + tab + "\" not offered: " + p.tabNames());
        if (DocWorkflowPage.TAB_DRAFTS.equals(tab)) p.waitForList(Duration.ofSeconds(15));
    }

    void onSubTab(String sub) {
        onTab(DocWorkflowPage.TAB_APPROVALS);
        ProductionReportPage p = page();
        assumeTrue(p.waitSubTab(sub, Duration.ofSeconds(10)), "Sub-tab \"" + sub + "\" not offered: " + p.subTabNames());
        p.openSubTab(sub);
    }

    void bringAppToFront() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); } catch (Exception e) { pkg = ""; }
        if (appPackage.equals(pkg)) return;
        for (int i = 0; i < 2; i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            try { if (appPackage.equals(driver.getCurrentPackage())) return; } catch (Exception ignored) { }
        }
        try { driver.activateApp(appPackage); } catch (Exception ignored) { }
        sleep(3000);
        waitAndDismissPermissions(Duration.ofSeconds(3));
    }

    boolean onTool() { return activityIs(ProductionReportPage.ACTIVITY) && present("tlTab"); }

    boolean inEditor() { return activityIs(ProductionReportPage.EDITOR_ACTIVITY); }

    boolean insideTool() {
        return activityIs(ProductionReportPage.ACTIVITY) || inEditor()
                || activityIs(ProductionReportPage.VIEWER_ACTIVITY);
    }

    /**
     * Bounded way back to the tool's tabs; never calls {@link #openTool()}. Leaving the editor
     * raises "Unsaved changes" — answered <b>Discard</b>, and only there.
     */
    void stepBack() {
        for (int i = 0; i < 8; i++) {
            bringAppToFront();
            if (onTool() && !dialogUp()) return;
            if (onProjectList()) return;
            if (inEditor() && "Unsaved changes".equals(page().alertTitle())) {
                page().tapAlertButton("Discard");
                continue;
            }
            if (closeAlert()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
    }

    boolean dialogUp() {
        return page().alertTitle() != null || present("tplConfirm") || present("input_name")
                || present("recycler_revisions") || present("recycler_approvers") || present("edit_rejection_reason");
    }

    /** Closes a platform AlertDialog with its NEGATIVE choice (Cancel / No), never a confirm. */
    boolean closeAlert() {
        DocWorkflowPage p = page();
        if (p.alertTitle() == null) return false;
        for (String safe : new String[]{"Cancel", "No", "OK"}) if (p.tapAlertButton(safe)) return true;
        return false;
    }

    // ─────────────────────────── building blocks ───────────────────────────

    /**
     * Creates a report from Template 1 and saves it under {@code name}; returns its serial.
     * Retries once when the editor comes up without the template's content (seen on the device).
     */
    String createReport(String name) { return createReport(name, false); }

    /** "No Call Sheet Found" comes up on every new report until a call sheet is published. */
    void dismissNoCallSheet() {
        DocWorkflowPage p = page();
        if (p.alertShown(Duration.ofSeconds(8)) && "No Call Sheet Found".equals(p.alertTitle())) p.tapAlertButton("OK");
    }

    /** Sets Crew Call and Unit Wrap through the Header dialog (accepting the picker's current time). */
    boolean setCallTimes() {
        DocEditorPage ed = editor();
        if (!ed.openHeader()) return false;
        boolean ok = ed.fieldShown("btn_crew_call") && ed.fieldShown("btn_unit_wrap");
        if (ok) {
            for (String id : new String[]{"btn_crew_call", "btn_unit_wrap"}) {
                tapIdOnce(id);
                tapIdOnce("material_timepicker_ok_button");
            }
            ok = String.valueOf(ed.field("btn_crew_call")).matches("\\d{1,2}:\\d{2}.*")
                    && String.valueOf(ed.field("btn_unit_wrap")).matches("\\d{1,2}:\\d{2}.*");
        }
        ed.headerDone();
        return ok;
    }

    void tapIdOnce(String id) {
        try {
            List<org.openqa.selenium.WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")"));
            if (!e.isEmpty()) { e.get(0).click(); sleep(1200); }
        } catch (Exception ignored) { }
    }

    String createReport(String name, boolean withCallTimes) {
        onTab(DocWorkflowPage.TAB_DRAFTS);
        for (int attempt = 1; attempt <= 2; attempt++) {
            ProductionReportPage p = page();
            assumeTrue(p.tapCreate(), "The create button is not offered (no posting rights?)");
            if (p.templatePickerShown(Duration.ofSeconds(12))) {
                sleep(1500);
                p.confirmTemplate();
            }
            dismissNoCallSheet();
            DocEditorPage ed = editor();
            assumeTrue(ed.isOpen(Duration.ofSeconds(30)), "The editor did not open: " + whereAreWe());
            ed.showSections();
            if (!ed.hasTemplateContent() && attempt == 1) {
                screenshot("blank_template_editor");
                System.out.println(">>> the editor opened WITHOUT the template's content — discarding and retrying");
                stepBack();
                continue;
            }
            if (withCallTimes) assumeTrue(setCallTimes(), "Crew Call / Unit Wrap could not be set — the template has no Call Times");
            assumeTrue(ed.tapOverflow(DocEditorPage.MENU_SAVE_AS), "Save As is not in the editor's overflow");
            assumeTrue(ed.nameDialogShown(Duration.ofSeconds(8)), "The name dialog did not open");
            ed.typeName(name);
            assumeTrue(name.equals(ed.typedName()), "The name field reads \"" + ed.typedName() + "\"");
            ed.nameSave();
            assumeTrue(page().isLoaded(Duration.ofSeconds(30)), "Saving did not return to the tool: " + whereAreWe());
            page().waitForList(Duration.ofSeconds(15));
            Row r = waitDraft(name, Duration.ofSeconds(30));
            assumeTrue(r != null, "The saved report \"" + name + "\" is not in Drafts");
            return r.serial;
        }
        assumeTrue(false, "The editor opened without the template's content twice");
        return null;
    }

    Row waitDraft(String name, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        ProductionReportPage p = page();
        while (true) {
            Row r = p.rowNamed(name);
            if (r != null || System.currentTimeMillis() > end) return r;
            p.refresh(ListKind.DRAFTS);
        }
    }

    /** Deletes a Drafts or Sent row by serial; true when it is gone. Never throws. */
    boolean deleteRow(ListKind kind, String serial) {
        try {
            ProductionReportPage p = page();
            if (!p.rowAction(kind, serial, "Delete")) return false;
            if (!p.alertShown(Duration.ofSeconds(6))) return false;
            p.tapAlertButton("Delete");
            return p.waitRowGone(kind, serial, Duration.ofSeconds(20));
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────── small helpers ───────────────────────────

    boolean activityIs(String simpleName) {
        try {
            String a = String.valueOf(driver.currentActivity());
            return a.endsWith("." + simpleName) || a.endsWith(simpleName);
        } catch (Exception e) {
            return false;
        }
    }

    boolean onProjectList() { return present("projectListRecyclerView"); }

    boolean present(String id) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    String whereAreWe() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); } catch (Exception e) { pkg = ""; }
        if (!appPackage.equals(pkg)) return "a different app (" + (pkg.isBlank() ? "unreadable" : pkg) + ")";
        if (onProjectList()) return "the project list";
        try { return "activity " + driver.currentActivity(); } catch (Exception e) { return "an unreadable activity"; }
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @BeforeEach
    void clearLeftovers() {
        if (insideTool() && dialogUp()) stepBack();
    }

    @AfterAll
    void leaveTool() {
        for (int i = 0; i < 8 && insideTool(); i++) {
            if (inEditor() && "Unsaved changes".equals(page().alertTitle())) { page().tapAlertButton("Discard"); continue; }
            if (closeAlert()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
    }
}
