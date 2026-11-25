package com.zillit.automation.tests.drive;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.drive.DriveEditorPage;
import com.zillit.automation.pages.drive.DriveFileContent;
import com.zillit.automation.pages.drive.DriveFileDetailPage;
import com.zillit.automation.pages.drive.DriveFlows;
import com.zillit.automation.pages.drive.DriveHomePage;
import com.zillit.automation.pages.drive.DriveTextEditorPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE EDITORS — the office editor ({@code OnlyOfficeEditorActivity} hosting Zillit Sheets /
 * Docs / Slides), the new blank-file flow, the Markdown/text editor and version history.
 *
 * <p>Rewritten for the in-house editor (Collabora is gone since Zillit-Android 113fcbef6).
 * Every in-editor assertion uses only what E-00 found in the accessibility tree — see
 * {@link DriveEditorPage}. The document itself is a canvas, so an edit is proven by
 * DOWNLOADING the file and reading its XML ({@link DriveFileContent}), not by reading cells.
 *
 * <p>Fixtures {@code zl_drive_book.xlsx}, {@code zl_drive_doc.docx} and {@code zl_drive_doc.pdf}
 * are pushed to /sdcard/Download by the runner and uploaded here when missing. Cases that TYPE
 * never touch them: they upload a {@code QA_Edit_<stamp>} copy and purge it at the end. Every
 * file the suite creates is {@code QA_…} and is removed by the CLEANUP case (Trash included).
 *
 * <p>Notify prompt: answered with SKIP, always. {@code Send} notifies real collaborators.
 */
@ExtendWith(ReasonLogger.class)
@Order(TestOrder.DRIVE_EDITOR)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveEditorTest extends BaseTest {

    private static final String XLSX = "zl_drive_book.xlsx";
    private static final String DOCX = "zl_drive_doc.docx";
    private static final String PDF  = "zl_drive_doc.pdf";

    private static final long STAMP = System.currentTimeMillis() % 100000;
    /** Scratch copies the TYPING cases edit — never the shared fixtures. */
    private static final String EDIT_XLSX = "QA_Edit_" + STAMP + ".xlsx";
    private static final String EDIT_DOCX = "QA_Edit_" + STAMP + ".docx";
    /** Blank files from the + sheet "New" group. */
    private static final String NEW_XLSX = "QA_New_" + STAMP;
    private static final String NEW_DOCX = "QA_NewDoc_" + STAMP;
    private static final String NEW_PPTX = "QA_NewDeck_" + STAMP;
    private static final String NEW_MD   = "QA_Note_" + STAMP;
    private static final String NEW_EXT  = "QA_Ext_" + STAMP;

    /** Everything this run created, for CLEANUP. */
    private static final List<String> CREATED = new ArrayList<>();
    /** Version count of EDIT_XLSX before the first typed edit (-1 = no version card). */
    private static int versionsBeforeEdit = -1;

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
        assumeTrue(flows().openDrive(), "Could not open Drive (Tools → a row containing 'Drive')");
    }

    /** The editors are separate activities — never leave a case inside one. */
    @AfterEach
    void leaveEditor() {
        new DriveEditorPage(driver).forceExit();
    }

    private DriveFlows flows() { return new DriveFlows(driver); }

    private DriveFileDetailPage detail(String name) {
        DriveFileDetailPage d = flows().openDetail(name);
        assumeTrue(d != null, "Could not open the detail screen of " + name);
        return d;
    }

    /** File details → btnEdit/btnOpen → the editor page loaded. */
    private DriveEditorPage openEditor(String file, boolean edit) {
        DriveFileDetailPage d = detail(file);
        String id = edit ? "btnEdit" : "btnOpen";
        assumeTrue(d.waitFor(id, Duration.ofSeconds(25)), id + " never appeared for " + file);
        assumeTrue(edit ? d.tapEdit() : d.tapOpen(), id + " not tappable");
        DriveEditorPage ed = new DriveEditorPage(driver);
        assumeTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)),
                id + " did not launch OnlyOfficeEditorActivity for " + file);
        boolean loaded = ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S));
        String err = ed.loadError();
        assertTrue(err == null, "The editor showed a load error for " + file + ": '" + err + "'");
        assumeTrue(loaded, "The editor page did not finish loading in " + DriveEditorPage.LOAD_S + "s for " + file);
        return ed;
    }

    // ==================== E-00 discovery ====================

    @Test
    @Order(1)
    @DisplayName("E-00 DISCOVERY: dump the editor's accessibility tree — xlsx/docx, Edit and Open")
    void e00DumpEditorTree() throws Exception {
        assumeTrue(flows().ensureFile(XLSX), "Could not get " + XLSX + " into Drive");
        assumeTrue(flows().ensureFile(DOCX), "Could not get " + DOCX + " into Drive");
        Path dir = Paths.get("build", "editor-dumps");
        Files.createDirectories(dir);
        Map<String, String> kinds = new LinkedHashMap<>();
        for (String file : List.of(XLSX, DOCX)) {
            for (boolean edit : new boolean[]{true, false}) {
                assertTrue(flows().openDrive(), "Lost Drive home");
                DriveEditorPage ed = openEditor(file, edit);
                if (!edit) ed.waitViewMode(Duration.ofSeconds(10));
                String tag = (file.endsWith("xlsx") ? "sheet" : "doc") + "-" + (edit ? "edit" : "view");
                Files.writeString(dir.resolve(tag + ".xml"), driver.getPageSource());
                List<String> labels = ed.exposedLabels();
                System.out.println(">>> E-00 " + tag + " (" + labels.size() + " nodes):");
                labels.forEach(l -> System.out.println(">>>     " + l));
                kinds.put(tag, ed.editorKind() + " | switch=" + ed.hasEditSwitch()
                        + " | notice=" + ed.viewOnlyNotice() + " | status=" + ed.statusText());
                screenshot("drive-editor-e00-" + tag);
                ed.forceExit();
            }
        }
        System.out.println(">>> E-00 summary " + kinds);
        assertTrue(kinds.values().stream().noneMatch(v -> v.startsWith("null")),
                "Every mode should expose the page surface (Zillit Sheets/Docs): " + kinds);
    }

    // ==================== the Edit gate ====================

    @Test
    @Order(2)
    @DisplayName("An EDITABLE file (xlsx) shows Edit and Open on its detail screen")
    void editButtonShownForSpreadsheet() {
        assumeTrue(flows().ensureFile(XLSX), "Could not get " + XLSX + " into Drive");
        DriveFileDetailPage d = detail(XLSX);
        boolean edit = d.waitFor("btnEdit", Duration.ofSeconds(25));
        System.out.println(">>> xlsx detail: btnEdit=" + edit + " btnOpen=" + d.hasOpen());
        assertTrue(edit, "An editable type with edit rights must offer Edit "
                + "(gate: (isEditableFile || isTextFile) && canEdit && posting access)");
        assertTrue(d.hasOpen(), "An office file must also offer Open (view mode)");
    }

    @Test
    @Order(3)
    @DisplayName("A NON-editable file (pdf) does NOT show the Edit button")
    void editButtonHiddenForPdf() {
        assumeTrue(flows().ensureFile(PDF), "Could not get " + PDF + " into Drive");
        DriveFileDetailPage d = detail(PDF);
        assumeTrue(d.waitFor("btnDownload", Duration.ofSeconds(25)),
                "The detail screen never finished loading — cannot judge Edit's absence");
        boolean absent = !d.waitFor("btnEdit", Duration.ofSeconds(6));
        assertTrue(absent, "A PDF is not an editable type — Edit must stay hidden");
    }

    // ==================== open, mode, close ====================

    @Test
    @Order(4)
    @DisplayName("E-01 Edit opens the editor straight in EDIT mode (no switch, no View-only notice)")
    void editOpensEditMode() {
        assumeTrue(flows().ensureFile(XLSX), "No spreadsheet");
        DriveEditorPage ed = openEditor(XLSX, true);
        sleep(3000);   // the notice / switch arrive a beat after the surface
        System.out.println(">>> E-01 kind=" + ed.editorKind() + " switch=" + ed.hasEditSwitch()
                + " notice=" + ed.viewOnlyNotice());
        screenshot("drive-editor-e01-edit");
        assertEquals(DriveEditorPage.SHEETS, ed.editorKind(), "An xlsx must open in Zillit Sheets");
        assertFalse(ed.hasEditSwitch(), "Edit mode must not offer the view-mode 'Edit' switch");
        assertFalse(ed.viewOnlyNoticeShown(), "Edit mode must not show the 'View only' notice");
        assertFalse(ed.textShown("Collabora", Duration.ofSeconds(1))
                        || ed.hasExactText("Edit document") || ed.hasExactText("Leave Editor"),
                "No Collabora surface may remain");
    }

    @Test
    @Order(5)
    @DisplayName("The editor strip shows the live connection and this user among 'People in this file'")
    void statusStripAndPresence() {
        DriveEditorPage ed = openEditor(XLSX, true);
        sleep(2500);
        String status = ed.statusText();
        List<String> people = ed.peopleInFile();
        System.out.println(">>> status='" + status + "' people=" + people);
        assertEquals("Live", status, "The page's #status-text should report the live connection");
        assertFalse(people.isEmpty(), "'People in this file' should list at least the current user");
    }

    @Test
    @Order(6)
    @DisplayName("E-02 Open opens VIEW mode: 'View only' notice + the native Edit switch bottom-right")
    void openOpensViewMode() {
        DriveEditorPage ed = openEditor(XLSX, false);
        assertTrue(ed.waitViewMode(Duration.ofSeconds(15)), "Open must load the editor in VIEW mode");
        String notice = ed.viewOnlyNotice();
        Rectangle b = ed.editSwitchRect();
        var screen = ed.screen();
        System.out.println(">>> E-02 notice='" + notice + "' switch=" + b + " screen=" + screen);
        screenshot("drive-editor-e02-view");
        assertNotNull(notice, "The page should show its View-only notice");
        assertTrue(notice.startsWith("View only"), "Unexpected notice text: " + notice);
        assertNotNull(b, "The owner may edit, so the native 'Edit' switch must be offered");
        assertTrue(b.getX() > screen.getWidth() / 2 && b.getY() > screen.getHeight() * 0.8,
                "The Edit switch should sit bottom-right, was at " + b);
    }

    @Test
    @Order(7)
    @DisplayName("E-03 The view-mode Edit switch reloads the editor in EDIT mode")
    void editSwitchReloadsInEditMode() {
        DriveEditorPage ed = openEditor(XLSX, false);
        assumeTrue(ed.waitViewMode(Duration.ofSeconds(15)), "Not in view mode");
        assumeTrue(ed.hasEditSwitch(), "No Edit switch to tap");
        DriveEditorPage.SwitchResult r = ed.switchToEdit(Duration.ofSeconds(120));
        System.out.println(">>> E-03 " + r);
        assertTrue(r.editMode, "After Edit the notice and the switch must be gone: " + r);
        assertTrue(ed.isEditorActivity(), "Switching must stay in OnlyOfficeEditorActivity");
    }

    @Test
    @Order(8)
    @DisplayName("E-04 Back in VIEW mode closes at once — no 'Saving…', no dialog")
    void backInViewClosesImmediately() {
        DriveEditorPage ed = openEditor(XLSX, false);
        assumeTrue(ed.waitViewMode(Duration.ofSeconds(15)), "Not in view mode");
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(8), false);
        System.out.println(">>> E-04 " + r);
        assertTrue(r.closed, "Back in view mode must close the editor: " + r);
        assertFalse(r.sawSaving, "View mode has nothing to save — 'Saving…' must not show");
        assertFalse(r.sawNotify, "View mode must never ask to notify collaborators");
        // closeMs includes Appium's own polling (~1 s per round of lookups) and, on a loaded
        // emulator, measured 1.1–6.5 s across runs. The contract is "no save step", asserted
        // above; this only catches a close that hangs.
        assertTrue(r.closeMs < 10000, "View mode should close without a save step, took " + r.closeMs + "ms");
        assertTrue(new DriveFileDetailPage(driver).waitFor("btnOpen", Duration.ofSeconds(8)),
                "Back should land on File details");
    }

    @Test
    @Order(9)
    @DisplayName("E-05 Back in EDIT mode shows 'Saving…' then closes — no Leave dialog, no prompt without an edit")
    void backInEditSavesThenCloses() {
        DriveEditorPage ed = openEditor(XLSX, true);
        sleep(2000);
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(12), true);
        System.out.println(">>> E-05 " + r);
        assertFalse(r.sawLegacyLeaveDialog, "The Collabora 'Leave Editor' dialog was removed");
        assertTrue(r.sawSaving, "Back in edit mode must show 'Saving…' while it commits");
        assertTrue(r.closed, "The editor must close after saving: " + r);
        assertFalse(r.sawNotify, "N-02: nothing was edited, so 'Let others know…' must not appear");
    }

    @Test
    @Order(10)
    @DisplayName("E-09 Rotating a loaded editor does not reload it")
    void rotationDoesNotReload() {
        DriveEditorPage ed = openEditor(XLSX, true);
        boolean reloaded = false;
        try {
            assumeTrue(ed.rotate(true), "The device refused to rotate: " + ed.lastRotateError);
            for (int i = 0; i < 10; i++) { if (ed.loadingShown()) reloaded = true; sleep(400); }
            ed.rotate(false);
            for (int i = 0; i < 10; i++) { if (ed.loadingShown()) reloaded = true; sleep(400); }
        } finally {
            ed.rotate(false);
        }
        assertFalse(reloaded, "configChanges covers orientation — 'Loading editor…' must not come back");
        assertTrue(ed.isEditorActivity() && ed.editorKind() != null, "Still the same loaded editor");
    }

    @Test
    @Order(11)
    @DisplayName("E-08 The editor background follows the app theme (#FFFFFF light / #0F1115 dark)")
    void themeBackground() throws Exception {
        DriveFileDetailPage d = detail(XLSX);
        assumeTrue(d.waitFor("btnOpen", Duration.ofSeconds(25)), "No Open");
        d.tapOpen();
        DriveEditorPage ed = new DriveEditorPage(driver);
        assumeTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)), "Editor did not launch");
        // The container paints before the page: sample it while "Loading editor…" is up.
        int rgb = -1;
        for (int i = 0; i < 20 && rgb < 0; i++) {
            if (ed.loadingShown()) {
                var img = javax.imageio.ImageIO.read(new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)));
                rgb = img.getRGB(img.getWidth() / 2, (int) (img.getHeight() * 0.3)) & 0xFFFFFF;
            }
            sleep(250);
        }
        assumeTrue(rgb >= 0, "'Loading editor…' was gone before a sample could be taken");
        System.out.println(">>> E-08 background #" + String.format("%06X", rgb));
        assertTrue(near(rgb, 0xFFFFFF) || near(rgb, 0x0F1115),
                "Background should be #FFFFFF or #0F1115, was #" + String.format("%06X", rgb));
    }

    private static boolean near(int a, int b) {
        for (int s = 0; s <= 16; s += 8) if (Math.abs(((a >> s) & 0xFF) - ((b >> s) & 0xFF)) > 10) return false;
        return true;
    }

    // ==================== WORD ====================

    @Test
    @Order(12)
    @DisplayName("E-07 WORD: a .docx offers Edit, opens Zillit Docs in EDIT mode")
    void wordOpensDocsInEditMode() {
        assumeTrue(flows().ensureFile(DOCX), "Could not get " + DOCX + " into Drive");
        DriveEditorPage ed = openEditor(DOCX, true);
        sleep(3000);
        assertEquals(DriveEditorPage.DOCS, ed.editorKind(), "A docx must open in Zillit Docs, not Sheets");
        assertFalse(ed.hasEditSwitch() || ed.viewOnlyNoticeShown(), "btnEdit must open EDIT mode");
    }

    @Test
    @Order(13)
    @DisplayName("E-07 WORD: Open shows the .docx read-only with the Edit switch")
    void wordOpenIsViewMode() {
        DriveEditorPage ed = openEditor(DOCX, false);
        assertTrue(ed.waitViewMode(Duration.ofSeconds(15)), "Open must be VIEW mode for a docx too");
        assertTrue(ed.hasEditSwitch(), "The owner should get the Edit switch");
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(8), false);
        assertTrue(r.closed && !r.sawSaving, "View-mode Back closes without saving: " + r);
    }

    // ==================== EDITING + PERSISTENCE ====================

    @Test
    @Order(20)
    @DisplayName("E-11 EDIT a spreadsheet → after Back, the downloaded file contains the typed value")
    void editSpreadsheetPersists() {
        assumeTrue(flows().uploadAs(XLSX, EDIT_XLSX), "Could not upload the scratch copy " + EDIT_XLSX);
        CREATED.add(EDIT_XLSX);
        DriveFileDetailPage d = detail(EDIT_XLSX);
        versionsBeforeEdit = d.versionCount();

        DriveEditorPage ed = openEditor(EDIT_XLSX, true);
        String marker = "zlqa" + STAMP;
        assertTrue(ed.writeMarker(marker), "The sheet never reached Live (status strip) or could not be focused to type");
        screenshot("drive-editor-e11-typed");
        ed.waitForAutosave();
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(15), true);
        System.out.println(">>> E-11 close " + r);
        assertTrue(r.closed, "The editor should close after Back: " + r);

        assertDownloadedContains(EDIT_XLSX, marker);
    }

    @Test
    @Order(21)
    @DisplayName("E-11 WORD: text typed into a .docx is in the downloaded document")
    void editDocumentPersists() {
        assumeTrue(flows().uploadAs(DOCX, EDIT_DOCX), "Could not upload the scratch copy " + EDIT_DOCX);
        CREATED.add(EDIT_DOCX);
        DriveEditorPage ed = openEditor(EDIT_DOCX, true);
        String marker = "zlqa" + STAMP + "doc";
        assertTrue(ed.writeMarker(marker), "The document never reached Live (status strip) or could not be focused to type");
        screenshot("drive-editor-e11-doc-typed");
        ed.waitForAutosave();
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(15), true);
        assertTrue(r.closed, "The editor should close after Back: " + r);
        assertDownloadedContains(EDIT_DOCX, marker);
    }

    private void assertDownloadedContains(String file, String marker) {
        DriveFileDetailPage d = new DriveFileDetailPage(driver);
        if (!d.waitFor("btnDownload", Duration.ofSeconds(10))) d = detail(file);
        assumeTrue(d.tapDownload(), "Download not tappable");
        d.waitDownloadDone(Duration.ofSeconds(40));
        DriveFileContent.Result res = new DriveFileContent(driver).waitForMarker(file, marker, Duration.ofSeconds(60));
        System.out.println(">>> download read-back of " + file + ": " + res);
        assumeTrue(!res.copies.isEmpty(),
                "No downloaded copy of " + file + " reached /sdcard/Download[/Drive] — cannot read it back: " + res);
        assertTrue(res.markerFound, "The typed value '" + marker + "' must be saved into " + file
                + " — the downloaded copies do not contain it: " + res);
    }

    @Test
    @Order(22)
    @DisplayName("V-01 An office edit adds a 'vN · <label>' version saved by this user")
    void versionAfterEdit() {
        assumeTrue(CREATED.contains(EDIT_XLSX), "The spreadsheet edit case must run first");
        DriveFileDetailPage d = detail(EDIT_XLSX);
        sleep(2000);
        int count = d.versionCount();
        List<String> labels = List.of();
        List<String> texts = List.of();
        if (count >= 0) { d.scrollToVersions(); labels = d.versionLabels(); texts = d.versionTexts(); }
        System.out.println(">>> V-01 before=" + versionsBeforeEdit + " now=" + count + " labels=" + labels + " texts=" + texts);
        // FileDetailFragment hides the card only when the server lists no versions — "for media /
        // PDF / archives". An edited office file must have them (text files do: see V-02).
        assertTrue(count > 0, "After a saved edit the xlsx shows no 'Versions' card — the server lists no "
                + "versions for an office file edited in Zillit Sheets");
        if (versionsBeforeEdit >= 0) {
            assertTrue(count > versionsBeforeEdit, "Editing must add a version (" + versionsBeforeEdit + " → " + count + ")");
        }
        for (String l : labels) {
            assertTrue(DriveFileDetailPage.VERSION_LABEL.matcher(l).matches(), "Unexpected version label '" + l + "'");
        }
        assertTrue(texts.stream().anyMatch(t -> t.startsWith("Saved by ")), "A row should say who saved it: " + texts);
    }

    // ==================== NOTIFY COLLABORATORS ====================

    @Test
    @Order(25)
    @DisplayName("N-01 After a real edit, Back asks 'Let others know you updated this file?' — Skip closes")
    void notifyPromptAfterEdit() {
        assumeTrue(CREATED.contains(EDIT_DOCX), "The document edit case must run first");
        DriveEditorPage ed = openEditor(EDIT_DOCX, true);
        assertTrue(ed.writeMarker("zlqa" + STAMP + "n"), "Could not type");
        ed.waitForAutosave();
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(15), false);
        System.out.println(">>> N-01 " + r);
        // The prompt only follows an edit the PAGE recorded (window.zillitHost.debug.edited), so
        // no prompt here means the keys never reached the document — E-11 says which.
        assumeTrue(r.sawNotify, "No notify prompt — the page did not record an edit from the typed keys "
                + "(see E-11 WORD read-back): " + r);
        screenshot("drive-editor-notify");
        assertTrue(ed.textShown(DriveEditorPage.NOTIFY_MESSAGE, Duration.ofSeconds(2)), "Message missing");
        assertTrue(ed.notifyHasNoteField(), "The optional note box ('Add a note (optional)') is missing");
        assertTrue(ed.notifyHasButtons(), "Send and Skip should both be offered");
        assertTrue(ed.skipNotify(), "Skip not tappable");
        sleep(2500);
        assertFalse(ed.isEditorActivity(), "Skip must close the editor");
    }

    // ==================== NEW BLANK FILE ====================

    private boolean openPlusSheetWithNew() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        assumeTrue(drive.openFab(), "The + sheet did not open");
        boolean has = drive.fabHasNewGroup();
        if (!has) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
        }
        assumeTrue(has, "The + sheet has no 'New' group — this build predates Zillit-Android 3ab6a41d0");
        return true;
    }

    @Test
    @Order(30)
    @DisplayName("F-01 The + sheet at root shows New: Document / Spreadsheet / Presentation / Markdown file / Text file")
    void newGroupAtRoot() {
        openPlusSheetWithNew();
        DriveHomePage drive = new DriveHomePage(driver);
        Map<String, String> rows = drive.newRowsShown();
        System.out.println(">>> F-01 " + rows);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        assertEquals(DriveHomePage.NEW_ROWS, rows, "The New group rows and labels");
    }

    @Test
    @Order(31)
    @DisplayName("F-03/F-04 Spreadsheet asks for a Name (default 'Untitled spreadsheet', .xlsx); Cancel creates nothing")
    void newNameDialogDefaultsAndCancel() {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        List<String> before = drive.allItemNamesScrolled();
        openPlusSheetWithNew();
        assertTrue(drive.tapNew("menuNewSpreadsheet"), "Spreadsheet row not tappable");
        assertTrue(drive.newNameDialogShown(Duration.ofSeconds(6)), "The Name dialog did not open");
        String value = drive.newNameField().getText();
        boolean ext = drive.textShown(".xlsx", Duration.ofSeconds(2));
        boolean title = drive.textShown("Spreadsheet", Duration.ofSeconds(2));
        System.out.println(">>> F-03 value='" + value + "' ext=" + ext + " title=" + title);
        assertEquals("Untitled spreadsheet", value, "Default name");
        assertTrue(ext, "The extension '.xlsx' should be shown next to the name");
        assertTrue(drive.answerNewNameDialog(null, false), "Cancel not tappable");
        sleep(2500);
        List<String> after = new DriveHomePage(driver).allItemNamesScrolled();
        assertEquals(before.size(), after.size(), "Cancel must create nothing: " + before + " → " + after);
    }

    private void createNew(String rowId, String name) {
        openPlusSheetWithNew();
        DriveHomePage drive = new DriveHomePage(driver);
        assertTrue(drive.tapNew(rowId), rowId + " not tappable");
        assertTrue(drive.newNameDialogShown(Duration.ofSeconds(6)), "The Name dialog did not open");
        assertTrue(drive.answerNewNameDialog(name, true), "Create not tappable");
    }

    @Test
    @Order(32)
    @DisplayName("F-05 New Spreadsheet → Create opens the editor in EDIT mode; the file is listed with 'v1 · Created'")
    void newSpreadsheetOpensEditor() {
        createNew("menuNewSpreadsheet", NEW_XLSX);
        CREATED.add(NEW_XLSX + ".xlsx");
        DriveEditorPage ed = new DriveEditorPage(driver);
        assertTrue(ed.waitForEditorActivity(Duration.ofSeconds(40)), "Create should open the editor");
        assertTrue(ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S)), "The new file did not load");
        sleep(3000);
        assertEquals(DriveEditorPage.SHEETS, ed.editorKind(), "A new spreadsheet opens Zillit Sheets");
        assertFalse(ed.isViewMode(), "A new file opens in EDIT mode");
        ed.backAndObserve(Duration.ofSeconds(12), true);
        assertTrue(flows().openDrive(), "Back to Drive");
        assertTrue(flows().waitListed(NEW_XLSX + ".xlsx", Duration.ofSeconds(30)),
                NEW_XLSX + ".xlsx should be in Files");
        DriveFileDetailPage d = detail(NEW_XLSX + ".xlsx");
        d.scrollToVersions();
        List<String> labels = d.versionLabels();
        System.out.println(">>> F-05 versions " + labels);
        assertTrue(labels.stream().anyMatch(l -> l.startsWith("v1") && l.contains("Created")),
                "The first version should read 'v1 · Created': " + labels);
    }

    @Test
    @Order(33)
    @DisplayName("F-06 New Document and Presentation open Zillit Docs / Slides in EDIT mode")
    void newDocumentAndPresentation() {
        for (String[] t : new String[][]{{"menuNewDocument", NEW_DOCX, ".docx", DriveEditorPage.DOCS},
                                         {"menuNewPresentation", NEW_PPTX, ".pptx", DriveEditorPage.SLIDES}}) {
            assertTrue(flows().openDrive(), "Drive home");
            createNew(t[0], t[1]);
            CREATED.add(t[1] + t[2]);
            DriveEditorPage ed = new DriveEditorPage(driver);
            assertTrue(ed.waitForEditorActivity(Duration.ofSeconds(40)), t[2] + ": the editor did not open");
            assertTrue(ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S)), t[2] + ": did not load");
            sleep(3000);
            System.out.println(">>> F-06 " + t[2] + " kind=" + ed.editorKind());
            assertEquals(t[3], ed.editorKind(), t[2] + " editor kind");
            assertFalse(ed.isViewMode(), t[2] + " opens in EDIT mode");
            ed.backAndObserve(Duration.ofSeconds(12), true);
        }
    }

    @Test
    @Order(34)
    @DisplayName("F-07 A typed own extension is dropped ('X.xlsx' for a Spreadsheet is not 'X.xlsx.xlsx')")
    void typedExtensionStripped() {
        createNew("menuNewSpreadsheet", NEW_EXT + ".xlsx");
        CREATED.add(NEW_EXT + ".xlsx");
        new DriveEditorPage(driver).waitForEditorActivity(Duration.ofSeconds(40));
        new DriveEditorPage(driver).forceExit();
        assertTrue(flows().openDrive(), "Drive home");
        List<String> names = new DriveHomePage(driver).allItemNamesScrolled();
        System.out.println(">>> F-07 " + names);
        assertTrue(names.stream().anyMatch(n -> n.equalsIgnoreCase(NEW_EXT + ".xlsx")), "File named " + NEW_EXT + ".xlsx");
        assertFalse(names.stream().anyMatch(n -> n.equalsIgnoreCase(NEW_EXT + ".xlsx.xlsx")), "Doubled extension");
    }

    // ==================== TEXT / MARKDOWN ====================

    @Test
    @Order(40)
    @DisplayName("F-10 New Markdown file opens the text editor already EDITING (EditText + Save, no Edit)")
    void newMarkdownOpensTextEditor() {
        createNew("menuNewMarkdown", NEW_MD);
        CREATED.add(NEW_MD + ".md");
        DriveTextEditorPage t = new DriveTextEditorPage(driver);
        assertTrue(t.waitOpen(Duration.ofSeconds(30)), "Markdown should open DriveTextFileActivity");
        assertTrue(t.waitLoaded(Duration.ofSeconds(30)), "The text editor did not load");
        sleep(1500);
        assertTrue(t.isEditing(), "A new Markdown file opens with the edit box");
        assertTrue(t.hasSaveButton(), "Save is shown while editing");
        assertFalse(t.hasEditButton(), "Edit is not shown while already editing");
    }

    @Test
    @Order(41)
    @DisplayName("T-02/T-03 Save makes a version ('Saved'); saving again unchanged says 'No changes to save'")
    void textSaveAndNoChanges() {
        assumeTrue(CREATED.contains(NEW_MD + ".md"), "The Markdown file must be created first");
        DriveTextEditorPage t = openText(NEW_MD + ".md", true);
        String line = "zlqa " + STAMP + " markdown";
        assertTrue(t.append("\n" + line), "Could not type into the edit box");
        assertTrue(t.tapSave(), "Save not tappable");
        assertTrue(t.toastShown("Saved", Duration.ofSeconds(10)), "Saving should toast 'Saved'");
        sleep(1500);
        assertTrue(t.tapSave(), "Save not tappable the second time");
        assertTrue(t.toastShown("No changes to save", Duration.ofSeconds(10)), "An unchanged save should say so");
        DriveEditorPage ed = new DriveEditorPage(driver);
        ed.forceExit();   // Back → notify prompt → Skip
        DriveTextEditorPage again = openText(NEW_MD + ".md", false);
        assertTrue(again.bodyContains(line), "T-05: the saved text must be shown when reopened read-only");
        assertFalse(again.isEditing(), "Open shows the read-only body");
    }

    @Test
    @Order(42)
    @DisplayName("T-04 Back with unsaved text asks 'Discard your changes?'; Keep editing stays, Discard drops it")
    void textDiscardPrompt() {
        assumeTrue(CREATED.contains(NEW_MD + ".md"), "The Markdown file must be created first");
        DriveTextEditorPage t = openText(NEW_MD + ".md", true);
        String junk = "zlqadiscard" + STAMP;
        assertTrue(t.append(junk), "Could not type");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        assertTrue(t.discardDialogShown(), "Unsaved text must ask before closing");
        assertTrue(t.textShown("Your changes haven't been saved.", Duration.ofSeconds(2)), "Discard message");
        assertTrue(t.keepEditing(), "Keep editing not tappable");
        assertTrue(t.isOpen() && t.isEditing(), "Keep editing must stay in the editor");
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        assertTrue(t.discard(), "Discard not tappable");
        sleep(2000);
        assertFalse(t.isOpen(), "Discard closes the editor");
        DriveTextEditorPage again = openText(NEW_MD + ".md", false);
        assertFalse(again.bodyContains(junk), "Discarded text must not have been saved");
    }

    private DriveTextEditorPage openText(String name, boolean edit) {
        assertTrue(flows().openDrive(), "Drive home");
        DriveFileDetailPage d = detail(name);
        String id = edit ? "btnEdit" : "btnOpen";
        assumeTrue(d.waitFor(id, Duration.ofSeconds(25)), id + " missing on " + name);
        assumeTrue(edit ? d.tapEdit() : d.tapOpen(), id + " not tappable");
        DriveTextEditorPage t = new DriveTextEditorPage(driver);
        assumeTrue(t.waitOpen(Duration.ofSeconds(30)), id + " on " + name + " should open the text editor "
                + "(DriveTextFileActivity, b5ae04154)");
        assumeTrue(t.waitLoaded(Duration.ofSeconds(30)), "The text editor did not load");
        sleep(1200);
        return t;
    }

    // ==================== VERSION HISTORY (on the Markdown file T-02 saved) ====================

    private DriveFileDetailPage mdVersions(int atLeast) {
        assumeTrue(CREATED.contains(NEW_MD + ".md"), "The Markdown file must be created and saved first");
        assertTrue(flows().openDrive(), "Drive home");
        DriveFileDetailPage d = detail(NEW_MD + ".md");
        int count = d.versionCount();
        assumeTrue(count >= atLeast, "Needs " + atLeast + "+ versions, the Markdown file has " + count);
        d.scrollToVersions();
        return d;
    }

    @Test
    @Order(43)
    @DisplayName("V-01 TEXT: rows read 'vN · Saved' / 'v1 · Created', 'Saved by <user>', with Open/Download")
    void textVersionRows() {
        DriveFileDetailPage d = mdVersions(2);
        List<String> labels = d.versionLabels();
        List<String> texts = d.versionTexts();
        System.out.println(">>> V-01 text labels=" + labels + " texts=" + texts);
        for (String l : labels) {
            assertTrue(DriveFileDetailPage.VERSION_LABEL.matcher(l).matches(), "Unexpected version label '" + l + "'");
        }
        assertTrue(labels.get(0).endsWith("· Saved"), "The latest (saved) version reads 'vN · Saved': " + labels);
        assertTrue(labels.get(labels.size() - 1).equals("v1 · Created"), "The first version reads 'v1 · Created': " + labels);
        assertTrue(texts.stream().anyMatch(t -> t.startsWith("Saved by ")), "Rows say who saved them: " + texts);
        assertTrue(d.versionRowActions(0).containsAll(List.of("Open", "Download")), "Open and Download on each row");
    }

    @Test
    @Order(44)
    @DisplayName("V-04 Opening an older text version shows '<name> · version N' read-only, no Edit")
    void openTextVersion() {
        DriveFileDetailPage d = mdVersions(2);
        int rows = d.versionLabels().size();
        String label = d.versionLabels().get(rows - 1);
        assertTrue(d.tapVersionAction(rows - 1, "Open"), "Open not tappable on " + label);
        DriveTextEditorPage t = new DriveTextEditorPage(driver);
        assertTrue(t.waitOpen(Duration.ofSeconds(30)) && t.waitLoaded(Duration.ofSeconds(30)),
                "A text version opens in the text viewer");
        String title = t.title();
        System.out.println(">>> V-04 title='" + title + "'");
        assertFalse(t.hasEditButton(), "A version is never editable");
        assertFalse(t.isEditing(), "A version opens read-only");
        assertTrue(title.matches(".+ · version \\d+"), "Title should read '<name> · version N' "
                + "(drive_text_version_title_format, set only when the preview carries versionNumber): " + title);
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1500);
        assertFalse(new DriveEditorPage(driver).notifyDialogShown(), "Closing a version never asks to notify");
    }

    @Test
    @Order(45)
    @DisplayName("V-02 Restore is hidden on the latest version; restoring an older one adds a 'Restored' version")
    void restoreTextVersion() {
        DriveFileDetailPage d = mdVersions(2);
        int before = d.versionCount();
        List<String> top = d.versionRowActions(0);
        int last = d.versionLabels().size() - 1;
        List<String> older = d.versionRowActions(last);
        System.out.println(">>> V-02 top=" + top + " older=" + older);
        assertFalse(top.contains("Restore"), "The latest version cannot be restored onto itself");
        assertTrue(older.contains("Restore"), "An older version offers Restore: " + older);
        assertTrue(d.tapVersionAction(last, "Restore"), "Restore not tappable");
        assertTrue(d.textShown("Restore Version", Duration.ofSeconds(6)), "Restore must confirm first");
        assertTrue(d.textShown("The current version will be replaced", Duration.ofSeconds(2)),
                "The dialog says the current version is replaced");
        assertTrue(d.confirmDialog(Duration.ofSeconds(5)), "Could not confirm Restore");
        sleep(5000);
        DriveFileDetailPage after = mdVersions(1);
        List<String> labels = after.versionLabels();
        System.out.println(">>> V-02 after restore count=" + after.versionCount() + " " + labels);
        assertTrue(after.versionCount() > before, "Restoring adds a version");
        assertTrue(labels.get(0).contains("Restored"), "The newest version is labelled 'Restored': " + labels);
    }

    @Test
    @Order(47)
    @DisplayName("[WEB L782] VERSIONS: a version's Download saves that version")
    void downloadTextVersion() {
        DriveFileDetailPage d = mdVersions(2);
        int last = d.versionLabels().size() - 1;
        assertTrue(d.versionRowActions(last).contains("Download"), "Each version row offers Download");
        assertTrue(d.tapVersionAction(last, "Download"), "Download not tappable");
        String got = new DriveFileContent(driver).waitForDownloadNamed(NEW_MD, Duration.ofSeconds(60));
        System.out.println(">>> version download = " + got);
        assertTrue(got != null, "The version should land in /sdcard/Download");
    }

    @Test
    @Order(46)
    @DisplayName("V-03 Opening an office version shows it read-only — no Edit switch, Back closes at once")
    void openOfficeVersion() {
        assumeTrue(CREATED.contains(EDIT_XLSX), "The spreadsheet edit case must run first");
        DriveFileDetailPage d = detail(EDIT_XLSX);
        assumeTrue(d.versionCount() >= 1, "The edited xlsx lists no versions to open (see V-01)");
        d.scrollToVersions();
        int last = d.versionLabels().size() - 1;
        assumeTrue(last >= 0 && d.versionRowActions(last).contains("Open"), "No Open on the office version row");
        assertTrue(d.tapVersionAction(last, "Open"), "Open not tappable");
        DriveEditorPage ed = new DriveEditorPage(driver);
        assertTrue(ed.waitForEditorActivity(Duration.ofSeconds(30)), "A version should open in the editor");
        assertTrue(ed.waitLoaded(Duration.ofSeconds(DriveEditorPage.LOAD_S)), "The version did not load");
        sleep(3000);
        assertFalse(ed.hasEditSwitch(), "A version is never editable — no Edit switch");
        DriveEditorPage.CloseResult r = ed.backAndObserve(Duration.ofSeconds(8), false);
        assertTrue(r.closed && !r.sawSaving && !r.sawNotify, "A version closes like view mode: " + r);
    }

    // ==================== CLEANUP ====================

    @Test
    @Order(99)
    @DisplayName("CLEANUP: remove every QA_ file this run created (and from Trash)")
    void cleanup() {
        List<String> failed = new ArrayList<>();
        for (String name : CREATED) {
            if (!flows().purge(name, false)) failed.add(name);
        }
        System.out.println(">>> cleanup created=" + CREATED + " failed=" + failed);
        assertTrue(flows().openDrive(), "Drive should be usable after cleanup");
        assertTrue(failed.isEmpty(), "Could not fully remove: " + failed);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
