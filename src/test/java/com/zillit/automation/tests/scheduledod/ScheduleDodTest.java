package com.zillit.automation.tests.scheduledod;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage.Folder;
import com.zillit.automation.pages.scheduledist.ScheduleDistPage.Row;
import com.zillit.automation.pages.scheduledod.ScheduleDodPage;
import com.zillit.automation.pages.scheduledod.ScheduleDodUploadPage;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SCHEDULE D.O.D. — Day-out-of-days PDFs filed in NAME folders. Cases and ids follow
 * {@code SCHEDULE_DOD_TESTCASES.md} (prefix {@code DOD-}; the QA sheet's own ids are quoted
 * as {@code [QA DOD-nn]}).
 *
 * <p>Everything is made fresh each run in two folders named {@code ZL_AUTO_<run> A} / {@code B}
 * — which the app SAVES as {@code Zl_auto_<run> A} ({@code generateCamelText}) — and swept by
 * DOD-99, which deletes every file in every folder whose name contains {@code zl_auto_}.
 *
 * <p>Opt-ins (both write outside the module or change the device):
 * <ul>
 *   <li>{@code -Dzillit.dod.publish=true} — DOD-32 really publishes to Document Distribution.
 *       Without it DOD-31 stops at Cancel.</li>
 *   <li>{@code -Dzillit.dod.offline=true} — DOD-55 switches the device's Wi-Fi and data off for
 *       a few seconds to re-check QA DOD-05.</li>
 * </ul>
 */
@ExtendWith(ReasonLogger.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(TestOrder.SCHEDULE_DOD)
class ScheduleDodTest extends ScheduleDodBaseTest {

    static final String MARK = "zl_auto_";
    private static final String RUN = String.format("%04d", System.currentTimeMillis() % 10000);
    /** Full file names, not prefixes: the picker matches on "contains". Same PDF, two folders. */
    private static final List<String> FIXTURES = List.of("zl_test_doc.pdf", "zl_dd_doc.pdf");
    private static String workingPdf;
    private static final Duration UPLOAD_WAIT = Duration.ofSeconds(120);
    private static final boolean PUBLISH = Boolean.getBoolean("zillit.dod.publish");
    private static final boolean OFFLINE = Boolean.getBoolean("zillit.dod.offline");

    /** BLUE = #ADD8E6, GREEN = #98FB98 ({@code colors.xml}). */
    private static final int[] BLUE = {0xAD, 0xD8, 0xE6};
    private static final int[] GREEN = {0x98, 0xFB, 0x98};

    private final String typedA = "ZL_AUTO_" + RUN + " A";
    private final String typedB = "ZL_AUTO_" + RUN + " B";
    private final String folderA = ScheduleDodUploadPage.savedName(typedA);
    private final String folderB = ScheduleDodUploadPage.savedName(typedB);

    private boolean madeA, madeB, secondInA, viewed, downloaded, moved;

    private Predicate<Row> inFolder(String folder) { return r -> ("Name: " + folder).equals(r.name); }

    // ═══════════════════════════ 1. the list · DOD-0 ═══════════════════════════

    @Test
    @Order(10)
    @DisplayName("DOD-01 The tool opens on its folder list, with Upload D.O.D. and History")
    void moduleOpens() {
        ScheduleDodPage page = openDod();
        assertNotNull(toolTitle, "No toolbar title");
        assertTrue(toolTitle.toUpperCase().contains("D.O.D") || toolTitle.toUpperCase().contains("DOD"),
                "Toolbar title: " + toolTitle);
        assertTrue(page.uploadShown(), "No upload button for an admin");
        assertEquals(ScheduleDodPage.UPLOAD_LABEL, page.uploadLabel(), "Upload label");
        assertTrue(page.historyShown(), "No History button");
    }

    @Test
    @Order(20)
    @DisplayName("DOD-02 [QA DOD-03] The list settles on folders or the empty view, never a blank screen")
    void listSettles() {
        ScheduleDodPage page = openDod();
        assertTrue(page.waitListShown(Duration.ofSeconds(15)),
                "After 15 s the list shows neither folders nor the empty view — QA DOD-03 (blank list, seen on the first open after launch)");
    }

    @Test
    @Order(30)
    @DisplayName("DOD-03 The list offers no search (finding 1)")
    void noSearchOnList() {
        ScheduleDodPage page = openDod();
        assertFalse(page.searchOffered(), "The list now offers search — update finding 1 and add search cases");
    }

    @Test
    @Order(170)   // after the uploads, so there is a folder to look at
    @DisplayName("DOD-04 A folder card has no ⋮ menu (finding 2)")
    void noFolderMenu() {
        ScheduleDodPage page = openDod();
        assumeTrue(!page.visibleFolders().isEmpty(), "No folders on the list yet");
        assertFalse(page.folderMenuShown(), "Folder cards now show a ⋮ — DodActivity's handler is commented out");
    }

    // ═══════════════════════════ 2. upload · DOD-1 ═══════════════════════════

    @Test
    @Order(100)
    @DisplayName("DOD-10 Upload offers a single source, Document")
    void pickerOffersDocument() {
        ScheduleDodUploadPage up = startUpload();
        try {
            assertEquals(ScheduleDodUploadPage.PICKER_TITLE, up.pickerTitle(), "Picker title");
            assertEquals(List.of("Document"), up.pickerOptions(), "Picker options");
        } finally {
            driver.navigate().back();
            sleep(1000);
            backToList();
        }
    }

    @Test
    @Order(110)
    @DisplayName("DOD-11 The form: a Name, an optional colour, no date, scene or page number")
    void formShape() {
        ScheduleDodUploadPage up = openForm();
        try {
            assertTrue(up.fieldShown(ScheduleDodUploadPage.NAME), "No Name field");
            // Hints are read from the page source: UiAutomator2 returned no "hint" attribute here.
            assertTrue(up.messageShown("\"Name\"", Duration.ofSeconds(2)), "No \"Name\" hint on the form");
            assertTrue(up.fieldShown(ScheduleDodUploadPage.COLOUR), "No colour field");
            assertTrue(up.messageShown("Select Color (Optional)", Duration.ofSeconds(2)),
                    "No \"Select Color (Optional)\" hint on the colour field");
            assertFalse(up.fieldShown(ScheduleDodUploadPage.SCENE), "A scene field is offered");
            assertFalse(up.fieldShown(ScheduleDodUploadPage.PAGE_NO), "A page-number field is offered");
            assertEquals("Submit", up.submitLabel(), "Submit label");
        } finally {
            up.leaveForm();
            backToList();
        }
    }

    @Test
    @Order(120)
    @DisplayName("DOD-12 A blank name is refused")
    void blankNameRefused() {
        ScheduleDodUploadPage up = openForm();
        try {
            up.setName("");
            if (up.fieldShown(ScheduleDodUploadPage.EPISODE)) up.setField(ScheduleDodUploadPage.EPISODE, "1");
            up.submit();
            sleep(2500);
            assertTrue(up.formShown(), "A D.O.D. was uploaded with no name");
        } finally {
            if (up.formShown()) up.leaveForm();
            backToList();
        }
    }

    @Test
    @Order(130)
    @DisplayName("DOD-13 The colour dropdown offers the eleven page colours")
    void colourOptions() {
        ScheduleDodUploadPage up = openForm();
        try {
            List<String> rows = up.openDropdown(ScheduleDodUploadPage.COLOUR);
            up.closeDropdown();
            assertEquals(List.of("WHITE", "BLUE", "PINK", "YELLOW", "GREEN", "GOLDENROD", "BUFF",
                    "SALMON", "CHERRY", "TAN", "IVORY"), rows, "Colour options");
        } finally {
            if (up.formShown()) up.leaveForm();
            backToList();
        }
    }

    @Test
    @Order(140)
    @DisplayName("DOD-14 Uploading with a name and colour files it in a folder of that name, re-cased")
    void uploadNamed() {
        madeA = upload(typedA, "BLUE");
        assertTrue(madeA, "The upload form did not finish");
        Folder f = waitForFolder(folderA, Duration.ofSeconds(60));
        assertNotNull(f, "No folder \"" + folderA + "\" (typed \"" + typedA + "\") after the upload. Marked folders: "
                + openDod().foldersContaining(MARK));
    }

    @Test
    @Order(150)
    @DisplayName("DOD-15 A colour is optional — a second D.O.D. without one makes its own folder")
    void uploadNoColour() {
        madeB = upload(typedB, null);
        assertTrue(madeB, "The upload form did not finish");
        assertNotNull(waitForFolder(folderB, Duration.ofSeconds(60)), "No folder \"" + folderB + "\" after the upload");
    }

    @Test
    @Order(160)
    @DisplayName("DOD-16 The Name field suggests existing folders, and the same name files into that folder")
    void sameNameSameFolder() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodUploadPage up = openForm();
        List<String> suggestions = up.nameSuggestions(MARK + RUN);
        up.setName(typedA.toLowerCase());
        if (!up.chooseDropdown(ScheduleDodUploadPage.COLOUR, "GREEN")) {
            up.leaveForm();
            backToList();
            assumeTrue(false, "The colour dropdown could not be set to GREEN");
        }
        if (up.fieldShown(ScheduleDodUploadPage.EPISODE)) up.setField(ScheduleDodUploadPage.EPISODE, "1");
        up.submit();
        secondInA = up.waitFormClosed(UPLOAD_WAIT);
        if (!secondInA) up.leaveForm();
        backToList();
        assertTrue(secondInA, "The upload form did not finish");
        assertTrue(suggestions.contains(folderA), "Typing \"" + MARK + RUN + "\" did not suggest \"" + folderA
                + "\": " + suggestions);
        ScheduleDodPage folder = waitForRows(folderA, 2);
        List<Row> rows = folder.allRows();
        assertEquals(2, rows.stream().filter(inFolder(folderA)).count(), "Folder " + folderA + " holds " + rows);
        assertEquals(1, openDod().foldersContaining(folderA).size(), "A second folder of the same name was made");
    }

    // ═══════════════════════════ 3. inside a folder · DOD-2 ═══════════════════════════

    @Test
    @Order(200)
    @DisplayName("DOD-20 A folder is titled with the tool and lists its files by name")
    void folderOpens() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assertEquals(toolTitle, page.header(), "Folder title");
        List<Row> rows = page.allRows();
        assertTrue(rows.stream().anyMatch(inFolder(folderA)), "No \"Name: " + folderA + "\" row: " + rows);
    }

    @Test
    @Order(210)
    @DisplayName("DOD-21 A folder offers no upload, History or search (finding 3)")
    void folderHasNoTools() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assertFalse(page.uploadShown(), "A folder now offers upload — update finding 3");
        assertFalse(page.historyShown(), "A folder now offers History — update finding 3");
        assertFalse(page.searchOffered(), "A folder now offers search — update finding 1");
    }

    @Test
    @Order(220)
    @DisplayName("DOD-22 A file card names its uploader and upload time, tinted with its colour")
    void rowShape() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        List<Row> rows = page.allRows();
        Row r = rows.stream().filter(inFolder(folderA)).findFirst().orElse(null);
        assertNotNull(r, "No row in " + folderA);
        assertTrue(r.createdBy != null && r.createdBy.startsWith("Uploaded by : "), "Uploader line: " + r);
        assertTrue(r.date.startsWith("Uploaded On :"), "Uploaded line: " + r);
        boolean blue = false, green = false;
        for (Row x : page.visibleRows()) {
            int[] c = page.rowColour(y -> y.date.equals(x.date) && java.util.Objects.equals(y.name, x.name));
            blue |= near(c, BLUE);
            green |= near(c, GREEN);
        }
        assertTrue(blue, "No file card in " + folderA + " is tinted BLUE (#ADD8E6)");
        if (secondInA) assertTrue(green, "No file card in " + folderA + " is tinted GREEN (#98FB98)");
    }

    @Test
    @Order(230)
    @DisplayName("DOD-23 [QA DOD-01] A D.O.D. uploaded without a date shows no date row")
    void noDateRow() {
        assumeTrue(madeB, "DOD-15 made no folder");
        ScheduleDodPage page = openFolder(folderB);
        List<Row> rows = page.allRows();
        assumeTrue(!rows.isEmpty(), "Folder " + folderB + " lists nothing");
        for (Row r : rows) {
            assertFalse(r.toString().contains("D.O.D. date"), "A card carries a D.O.D. date: " + r);
        }
        assertTrue(page.onScreenIds("tv_page_date").isEmpty(), "A date row is shown on a file card");
    }

    @Test
    @Order(240)
    @DisplayName("DOD-24 A file's menu: View, Download, Publish, Move, the counts and Delete — no Replace")
    void fileMenu() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        List<String> items = page.openRowMenu(inFolder(folderA));
        assumeTrue(items != null, "The file's ⋮ could not be tapped");
        page.closeMenu();
        assertEquals(List.of(ScheduleDodPage.VIEW, ScheduleDodPage.DOWNLOAD, ScheduleDodPage.PUBLISH,
                ScheduleDodPage.MOVE, ScheduleDodPage.DOWNLOAD_COUNT, ScheduleDodPage.VIEW_COUNT,
                ScheduleDodPage.DELETE), items, "Menu for an admin");
    }

    @Test
    @Order(250)
    @DisplayName("DOD-25 Tapping a file opens the PDF")
    void tapViews() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assumeTrue(page.tapRow(inFolder(folderA)), "The file could not be tapped");
        String where = waitForViewer(Duration.ofSeconds(40));
        bringAppToFront();
        viewed = where != null;
        assertNotNull(where, "Tapping a file did not open the PDF");
    }

    @Test
    @Order(260)
    @DisplayName("DOD-26 View in the menu opens the PDF")
    void menuViews() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assumeTrue(page.openRowMenu(inFolder(folderA)) != null && page.tapMenuItem(ScheduleDodPage.VIEW),
                "View could not be reached");
        String where = waitForViewer(Duration.ofSeconds(40));
        bringAppToFront();
        viewed |= where != null;
        assertNotNull(where, "View did not open the PDF");
    }

    @Test
    @Order(270)
    @DisplayName("DOD-27 Download saves the file, or says the user may not download")
    void download() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assumeTrue(page.openRowMenu(inFolder(folderA)) != null && page.tapMenuItem(ScheduleDodPage.DOWNLOAD),
                "Download could not be reached");
        if (page.messageShown("You don't have permission to Download any file.", Duration.ofSeconds(3))) {
            assumeTrue(false, "This user has no download access to Schedule D.O.D.");
        }
        downloaded = page.messageShown("File Downloaded Successfully.", Duration.ofSeconds(45));
        assertTrue(downloaded, "Download ended without \"File Downloaded Successfully.\"");
    }

    // ═══════════════════════════ 4. counts · DOD-3 ═══════════════════════════

    @Test
    @Order(300)
    @DisplayName("DOD-30 View Count opens its own screen and lists the viewer")
    void viewCount() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openCount(ScheduleDodPage.VIEW_COUNT);
        try {
            assertTrue(ScheduleDodPage.VIEW_COUNT.equalsIgnoreCase(String.valueOf(page.header())), "Count title: " + page.header());
            assertEquals("Search By User Name", page.countSearchHint(), "Search hint");
            if (viewed) assertFalse(page.countRowTexts().isEmpty(), "View Count lists nobody after this user viewed the file");
        } finally {
            backToList();
        }
    }

    @Test
    @Order(310)
    @DisplayName("DOD-31 Download Count opens its own screen and lists the downloader")
    void downloadCount() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openCount(ScheduleDodPage.DOWNLOAD_COUNT);
        try {
            assertTrue(ScheduleDodPage.DOWNLOAD_COUNT.equalsIgnoreCase(String.valueOf(page.header())), "Count title: " + page.header());
            if (downloaded) assertFalse(page.countRowTexts().isEmpty(), "Download Count lists nobody after a download");
        } finally {
            backToList();
        }
    }

    @Test
    @Order(320)
    @DisplayName("DOD-32 The count search filters by user name")
    void countSearch() {
        assumeTrue(viewed, "Nobody has viewed the file, so View Count has no one to search for");
        ScheduleDodPage page = openCount(ScheduleDodPage.VIEW_COUNT);
        try {
            List<String> all = page.countRowTexts();
            assumeTrue(!all.isEmpty(), "View Count lists nobody");
            String part = all.get(0).length() > 3 ? all.get(0).substring(0, 3) : all.get(0);
            page.countSearch(part);
            List<String> hit = page.countRowTexts();
            page.countSearch("zzqq" + RUN);
            List<String> miss = page.countRowTexts();
            boolean emptyView = page.countEmptyShown();
            page.countSearch("");
            assertFalse(hit.isEmpty(), "Searching \"" + part + "\" hid " + all.get(0));
            assertTrue(miss.isEmpty() && emptyView, "Searching for nobody still lists " + miss + " (empty view: " + emptyView + ")");
        } finally {
            backToList();
        }
    }

    @Test
    @Order(330)
    @DisplayName("DOD-33 [QA DOD-02] View Count's search text is not carried into Download Count")
    void countSearchNotCarried() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openCount(ScheduleDodPage.VIEW_COUNT);
        page.countSearch("zzqq");
        driver.navigate().back();   // keyboard already hidden by countSearch
        sleep(1200);
        if (activityIs("UsersCountListPage")) { driver.navigate().back(); sleep(1200); }
        page = openCountHere(ScheduleDodPage.DOWNLOAD_COUNT);
        try {
            String text = page.countSearchText();
            assertEquals("", text, "Download Count opened with the search \"" + text + "\" from View Count");
            if (downloaded) assertFalse(page.countRowTexts().isEmpty(), "Download Count lists nobody");
        } finally {
            backToList();
        }
    }

    // ═══════════════════════ 5. Publish to Doc Distribution · DOD-4 ═══════════════════════

    @Test
    @Order(400)
    @DisplayName("DOD-40 Publish asks first, naming the destination; Cancel publishes nothing")
    void publishAsks() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assumeTrue(page.openRowMenu(inFolder(folderA)) != null && page.tapMenuItem(ScheduleDodPage.PUBLISH),
                "Publish could not be reached");
        if (page.messageShown("You don't have permission to distribute", Duration.ofSeconds(4))) {
            assumeTrue(false, "This user cannot post to Document Distribution");
        }
        try {
            assertTrue(page.textShown("Publish to Document Distribution", Duration.ofSeconds(20)), "No publish confirm");
            assertTrue(page.textShown("to the Document Distribution library?", Duration.ofSeconds(3)), "Confirm body");
        } finally {
            if (!PUBLISH && !page.tapDialogLeft()) page.tapText("Cancel");
        }
    }

    @Test
    @Order(410)
    @DisplayName("DOD-41 Publish adds the file to Document Distribution (opt-in)")
    void publish() {
        assumeTrue(PUBLISH, "Opt-in: run with -Dzillit.dod.publish=true — it writes into Document Distribution");
        ScheduleDodPage page = new ScheduleDodPage(driver);
        page.tapText("Publish");
        assertTrue(page.textShown("Published.", Duration.ofSeconds(60)), "No \"Published.\"");
        page.tapText("Ok");
    }

    @Test
    @Order(420)
    @DisplayName("DOD-42 Cancelling a publish leaves View opening the PDF")
    void viewAfterPublishCancel() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        assumeTrue(page.tapRow(inFolder(folderA)), "The file could not be tapped");
        boolean publishAgain = page.textShown("Publish to Document Distribution", Duration.ofSeconds(5));
        if (publishAgain) { if (!page.tapDialogLeft()) page.tapText("Cancel"); }
        String where = waitForViewer(Duration.ofSeconds(30));
        bringAppToFront();
        assertFalse(publishAgain, "Tapping the file after a publish raised the publish confirm again");
        assertNotNull(where, "Tapping the file after a publish did not open the PDF");
    }

    // ═══════════════════════════ 6. Move · Delete · DOD-5 ═══════════════════════════

    @Test
    @Order(500)
    @DisplayName("DOD-50 Move offers the other folders, not the file's own, and moves the file")
    void moveFile() {
        assumeTrue(madeA && madeB, "DOD-14 / DOD-15 left no two folders to move between");
        ScheduleDodPage page = openFolder(folderB);
        assumeTrue(page.openRowMenu(inFolder(folderB)) != null && page.tapMenuItem(ScheduleDodPage.MOVE),
                "Move could not be reached");
        sleep(1500);
        List<String> sheet = page.sheetTexts();
        assertTrue(sheet.contains("Choose Folder"), "No \"Choose Folder\" sheet: " + sheet);
        assertFalse(sheet.contains(folderB), "The sheet offers the file's own folder: " + sheet);
        assertTrue(page.tapText(folderA), "Folder " + folderA + " is not offered: " + sheet);
        moved = true;
        sleep(3000);
        backToList();
        ScheduleDodPage a = waitForRows(folderA, (secondInA ? 3 : 2));
        long inA = a.allRows().size();
        backToList();
        assertEquals(secondInA ? 3 : 2, inA, "Folder " + folderA + " holds " + inA + " files after the move");
    }

    @Test
    @Order(510)
    @DisplayName("DOD-51 A folder whose last file moved out leaves the list")
    void emptiedFolderLeaves() {
        assumeTrue(moved, "DOD-50 moved nothing");
        Folder still = null;
        for (int i = 0; i < 4; i++) {
            backToList();
            still = openDod().folder(folderB);
            if (still == null) break;
            sleep(4000);
        }
        if (still != null) {
            ScheduleDodPage page = openFolder(folderB);
            List<Row> rows = page.allRows();
            backToList();
            assertTrue(false, "Folder " + folderB + " is still listed with " + rows.size() + " file(s) after its"
                    + " only file moved out: " + rows);
        }
    }

    @Test
    @Order(520)
    @DisplayName("DOD-52 Delete asks first, and No keeps the file")
    void deleteAsks() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        int before = page.allRows().size();
        assumeTrue(page.openRowMenu(inFolder(folderA)) != null && page.tapMenuItem(ScheduleDodPage.DELETE),
                "Delete could not be reached");
        assertTrue(page.dialogShown(), "Delete did not ask first");
        assertEquals("Are you sure you want to delete?", page.dialogMessage(), "Confirm message");
        page.tapDialogLeft();
        assertEquals(before, page.allRows().size(), "No took a file anyway");
    }

    @Test
    @Order(530)
    @DisplayName("DOD-53 Delete removes the file")
    void deleteRemoves() {
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        int before = page.allRows().size();
        assumeTrue(before > 1, "Only one file in " + folderA + " — kept for the later cases, swept by DOD-99");
        assumeTrue(page.openRowMenu(inFolder(folderA)) != null && page.tapMenuItem(ScheduleDodPage.DELETE)
                && page.dialogShown(), "Delete could not be reached");
        page.tapDialogRight();
        String snack = page.snackbarText(Duration.ofSeconds(10));
        int after = before;
        for (int i = 0; i < 8 && after >= before; i++) {
            sleep(2500);
            after = page.allRows().size();
        }
        assertEquals(before - 1, after, "Files in " + folderA + " after Delete (message: " + snack + ")");
        assertTrue(snack == null || !snack.matches("[a-z0-9_.]+"), "The delete message is an internal key: " + snack);
    }

    // ═══════════════════════════ 7. History · DOD-6 ═══════════════════════════

    @Test
    @Order(600)
    @DisplayName("DOD-60 History opens on deleted folders, titled after the tool, with no error")
    void history() {
        ScheduleDodPage page = openDod();
        assumeTrue(page.tapHistory(), "History could not be tapped");
        sleep(3500);
        try {
            assertTrue(activityIs("UserHistoryFolderPage"), "History did not open: " + whereAreWe());
            // The toolbar shows its title upper-cased ("SCHEDULE D.O.D.").
            assertTrue((toolTitle + " History").equalsIgnoreCase(String.valueOf(page.header())),
                    "History title: " + page.header());
            String snack = page.snackbarText(Duration.ofSeconds(3));
            assertTrue(snack == null, "History reported an error: \"" + snack + "\"");
            boolean listed = !page.onScreenIds("layMainHolder").isEmpty();
            assertTrue(listed || page.emptyShown(), "History showed neither folders nor the empty state");
            assertFalse(page.searchOffered(), "History now offers search");
        } finally {
            backToList();
        }
    }

    @Test
    @Order(610)
    @DisplayName("DOD-61 A History folder opens its deleted files")
    void historyFolderOpens() {
        ScheduleDodPage page = openDod();
        assumeTrue(page.tapHistory(), "History could not be tapped");
        sleep(3500);
        try {
            List<Folder> folders = page.visibleFolders();
            assumeTrue(!folders.isEmpty(), "History lists no deleted folders");
            assertTrue(page.openFolder(folders.get(0).title), "The History folder could not be tapped");
            sleep(2500);
            assertTrue(activityIs("UserChatHistoryPage"), "A History folder did not open its files: " + whereAreWe());
            String snack = page.snackbarText(Duration.ofSeconds(3));
            assertTrue(snack == null, "The History folder reported an error: \"" + snack + "\"");
        } finally {
            backToList();
        }
    }

    // ═══════════════════════════ 8. QA sheet re-checks · DOD-7 ═══════════════════════════

    @Test
    @Order(700)
    @DisplayName("DOD-70 [QA DOD-04] The upload form offers an optional D.O.D. date")
    void dateOnForm() {
        ScheduleDodUploadPage up = openForm();
        boolean date;
        try {
            date = up.dateShown();
        } finally {
            up.leaveForm();
            backToList();
        }
        assertTrue(date, "No date field on the D.O.D. upload form (UploadDocumentActivity.kt:493 hides"
                + " dateSelector for DOD_PAGES) — QA DOD-04, open; web has none either, product to decide");
    }

    @Test
    @Order(710)
    @DisplayName("DOD-71 [QA DOD-03] After an app restart the list shows its folders on every open")
    void listAfterRestart() {
        assumeTrue(madeA, "No folder of ours is known to exist, so a blank list could be correct");
        List<String> blanks = new ArrayList<>();
        for (int attempt = 1; attempt <= 2; attempt++) {
            try { driver.terminateApp(appPackage); } catch (Exception ignored) { }
            sleep(2000);
            bringAppToFront();
            for (int open = 1; open <= 2; open++) {
                ScheduleDodPage page = openDod();
                boolean shown = page.waitListShown(Duration.ofSeconds(10));
                boolean ours = shown && !page.visibleFolders().isEmpty();
                if (!ours) blanks.add("restart " + attempt + ", open " + open + (shown ? " (empty view)" : " (blank)"));
                stepBackToTools();
            }
        }
        assertTrue(blanks.isEmpty(), "The folder list was blank after a restart: " + blanks
                + " — QA DOD-03 (DodDbManager.getFolderList off the main thread)");
    }

    @Test
    @Order(720)
    @DisplayName("DOD-72 [QA DOD-05] A publish that failed offline does not hijack the next View (opt-in)")
    void failedPublishDoesNotHijackView() {
        assumeTrue(OFFLINE, "Opt-in: run with -Dzillit.dod.offline=true — it turns the device's Wi-Fi and data off");
        assumeTrue(madeA, "DOD-14 made no folder");
        ScheduleDodPage page = openFolder(folderA);
        boolean hijacked;
        String where;
        try {
            setOnline(false);
            sleep(3000);
            assumeTrue(page.openRowMenu(inFolder(folderA)) != null && page.tapMenuItem(ScheduleDodPage.PUBLISH),
                    "Publish could not be reached");
            sleep(4000);
            if (page.dialogShown()) page.tapDialogLeft();
        } finally {
            setOnline(true);
        }
        sleep(8000);
        assumeTrue(page.tapRow(inFolder(folderA)), "The file could not be tapped");
        hijacked = page.textShown("Publish to Document Distribution", Duration.ofSeconds(8));
        if (hijacked && !page.tapDialogLeft()) page.tapText("Cancel");
        where = hijacked ? null : waitForViewer(Duration.ofSeconds(30));
        bringAppToFront();
        assertFalse(hijacked, "View raised the publish confirm left over from the failed offline publish"
                + " (QA DOD-05: pendingDistribute is not cleared on error)");
        assertNotNull(where, "View did not open the PDF after coming back online");
    }

    // ═══════════════════════════ 9. cleanup · DOD-99 ═══════════════════════════

    @Test
    @Order(990)
    @DisplayName("DOD-99 No ZL_AUTO folder is left")
    void sweep() {
        List<Folder> left = List.of();
        for (int pass = 0; pass < 3; pass++) {
            backToList();
            List<Folder> marked = openDod().foldersContaining(MARK);
            if (marked.isEmpty()) { left = marked; break; }
            for (Folder f : marked) {
                ScheduleDodPage p = openDod();
                if (!p.openFolder(f.title)) continue;
                p.waitLoaded(Duration.ofSeconds(20));
                sleep(1500);
                for (int i = 0; i < 20; i++) {
                    List<Row> rows = p.visibleRows();
                    if (rows.isEmpty()) break;
                    Row first = rows.get(0);
                    if (p.openRowMenu(r -> r.toString().equals(first.toString())) == null) break;
                    if (!p.tapMenuItem(ScheduleDodPage.DELETE) || !p.dialogShown()) { p.closeMenu(); break; }
                    p.tapDialogRight();
                    sleep(4000);
                }
                backToList();
            }
            sleep(5000);
            backToList();
            stepBackToTools();
            left = openDod().foldersContaining(MARK);
        }
        assertTrue(left.isEmpty(), "Marked folders left behind: " + left);
    }

    // ═══════════════════════════ helpers ═══════════════════════════

    private ScheduleDodUploadPage startUpload() {
        ScheduleDodPage page = openDod();
        assumeTrue(page.uploadShown(), "No upload button: this user cannot upload to Schedule D.O.D.");
        assumeTrue(page.tapUpload(), "The upload button could not be tapped");
        ScheduleDodUploadPage up = new ScheduleDodUploadPage(driver);
        assumeTrue(up.pickerShown(Duration.ofSeconds(10)), "The picker did not open");
        return up;
    }

    /**
     * The upload button → picker → a fixture PDF → the form. {@code zl_test_doc.pdf} lives in
     * /sdcard/Documents, which the shared picker walk could not reach on the Pixel 7 (its roots
     * drawer showed no Documents / Recent root); the runner pushes the SAME file to
     * /sdcard/Download/zl_dd_doc.pdf, which the Downloads root reaches. The first that works
     * is remembered for the rest of the run.
     */
    private ScheduleDodUploadPage openForm() {
        List<String> order = new ArrayList<>(FIXTURES);
        if (workingPdf != null) { order.remove(workingPdf); order.add(0, workingPdf); }
        for (String pdf : order) {
            ScheduleDodUploadPage up = startUpload();
            if (up.pickPdf(pdf, () -> waitAndDismissPermissions(Duration.ofSeconds(4)))) {
                workingPdf = pdf;
                return up;
            }
            backToList();
        }
        assumeTrue(false, "Could not pick any of " + FIXTURES + " in the system picker. The runner pushes"
                + " them to /sdcard/Documents and /sdcard/Download; on screen: " + whereAreWe());
        return null;
    }

    /** Fills and submits an upload; true when the form finished. */
    private boolean upload(String name, String colour) {
        ScheduleDodUploadPage up = openForm();
        if (colour != null && !up.chooseDropdown(ScheduleDodUploadPage.COLOUR, colour)) {
            up.leaveForm();
            backToList();
            assumeTrue(false, "The colour dropdown could not be set to " + colour);
        }
        assumeTrue(up.setName(name), "The Name field could not be typed into");
        if (up.fieldShown(ScheduleDodUploadPage.EPISODE)) up.setField(ScheduleDodUploadPage.EPISODE, "1");
        up.submit();
        boolean closed = up.waitFormClosed(UPLOAD_WAIT);
        if (!closed) up.leaveForm();
        backToList();
        return closed;
    }

    /** Leaves the tool for the Tools tab, so the next {@link #openDod()} creates the list afresh. */
    private void stepBackToTools() {
        for (int i = 0; i < 4 && insideDod(); i++) {
            if (closeBlockingDialog()) continue;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
        }
    }

    /** The list only refetches on a socket event or a fresh open, so reopen until listed. */
    private Folder waitForFolder(String title, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Folder f = openDod().folder(title);
            if (f != null) return f;
            stepBackToTools();
            sleep(4000);
        }
        return null;
    }

    private ScheduleDodPage openFolder(String title) {
        if (inFolder()) {
            ScheduleDodPage here = new ScheduleDodPage(driver);
            if (here.allRows().stream().anyMatch(inFolder(title))) return here;
            backToList();
        }
        ScheduleDodPage page = openDod();
        boolean opened = page.openFolder(title);
        assumeTrue(opened, "Folder " + title + " is not on the list: " + page.allFolders());
        ScheduleDodPage inside = new ScheduleDodPage(driver);
        inside.waitLoaded(Duration.ofSeconds(30));
        inside.waitListShown(Duration.ofSeconds(15));
        return inside;
    }

    /** Opens the folder (refetching) until it lists {@code atLeast} files; returns it open. */
    private ScheduleDodPage waitForRows(String title, int atLeast) {
        ScheduleDodPage page = null;
        for (int i = 0; i < 6; i++) {
            backToList();
            page = openFolder(title);
            if (page.allRows().size() >= atLeast) return page;
            sleep(4000);
        }
        return page;
    }

    private ScheduleDodPage openCount(String which) {
        ScheduleDodPage page = openFolder(folderA);
        return openCountHere(which);
    }

    private ScheduleDodPage openCountHere(String which) {
        ScheduleDodPage page = inFolder() ? new ScheduleDodPage(driver) : openFolder(folderA);
        List<String> items = page.openRowMenu(inFolder(folderA));
        assumeTrue(items != null && items.contains(which), which + " is not offered (admin only): " + items);
        page.tapMenuItem(which);
        assertTrue(page.countScreenShown(Duration.ofSeconds(20)), which + " did not open the count screen: " + whereAreWe());
        sleep(1500);
        return page;
    }

    /** A viewer opened: another app in front, or an in-app viewer over the folder. */
    private String waitForViewer(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && !pkg.equals(appPackage)) return pkg;
                String act = String.valueOf(driver.currentActivity());
                if (!act.endsWith("DodDetailActivity") && !act.endsWith("DodActivity")) return act;
            } catch (Exception ignored) { }
            sleep(1000);
        }
        return null;
    }

    private void setOnline(boolean on) {
        try {
            driver.executeScript("mobile: setConnectivity", java.util.Map.of("wifi", on, "data", on));
        } catch (Exception e) {
            System.out.println(">>> setConnectivity(" + on + ") failed: " + e.getMessage());
        }
    }

    private static boolean near(int[] c, int[] want) {
        if (c == null) return false;
        for (int i = 0; i < 3; i++) if (Math.abs(c[i] - want[i]) > 18) return false;
        return true;
    }
}
