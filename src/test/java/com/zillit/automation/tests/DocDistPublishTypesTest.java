package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.DocDistPublishSheet;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — PUBLISH TYPE RULES: which destinations accept which files.
 *
 * <p>Publishing is not "any file anywhere". {@code DocDistEnums} gives every destination
 * its own constraints, and most of them are PDF-ONLY:
 *
 * <pre>
 *   pdfOnly = true   CALL_SHEET_UNIT · PRODUCTION_REPORT · SCHEDULE_DOD ·
 *                    SCHEDULE_FULL · SCHEDULE_PAGE · SCHEDULE_ONELINE ·
 *                    SCRIPT_DISTRIBUTION · PAGE_DISTRIBUTION
 *   unrestricted     INFO · CONFIDENTIAL_INFO
 *   singleFile also  SCHEDULE_FULL · SCHEDULE_PAGE · SCHEDULE_ONELINE ·
 *                    SCRIPT_DISTRIBUTION · PAGE_DISTRIBUTION
 * </pre>
 *
 * <p>The UI enforces it per ROW, not with an error at the end
 * ({@code PublishBottomSheet.renderFiles}): with a pdf-only category selected, every
 * non-PDF row is rendered {@code enabled = false} and tagged
 * {@code dd_disabled_tag} = "PDF only", and the sheet's note reads
 * <i>"Only PDF files can be published as %s."</i> The publish rows reuse
 * {@code item_dd_picker_file}, so the tag id is the picker's.
 *
 * <p>None of that was covered: the existing publish cases drive the categories and the
 * required fields, but always over a single PDF, so the whole type gate was invisible.
 * This suite uploads one file of EVERY supported family into a folder, selects them all,
 * and checks what each destination does with them.
 *
 * <p><b>Nothing is ever published.</b> A real publish writes into Call Sheet / Info /
 * Production Report and cannot be undone from here — every case ends on Cancel.
 */
@Order(TestOrder.DOC_DISTRIBUTION_PUBLISH_TYPES)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistPublishTypesTest extends DocDistBaseTest {

    private static final String FOLDER = "QA_PTYPE_" + (System.currentTimeMillis() % 100000);
    private static final String PDF = "zl_dd_doc.pdf";
    private static final String[] NON_PDF =
            {"zl_dd_pic.jpg", "zl_dd_word.docx", "zl_dd_book.xlsx", "zl_dd_card.vcf"};
    private static final String[] ALL_MEDIA =
            {PDF, "zl_dd_pic.jpg", "zl_dd_word.docx", "zl_dd_book.xlsx", "zl_dd_card.vcf"};

    /** A PDF-only destination and an unrestricted one, by category id. */
    private static final String CAT_PDF_ONLY = "dd_cat_callsheet";
    private static final String CAT_ANY_TYPE = "dd_cat_info";

    private static boolean seeded;

    /** The files actually selected into the CURRENT sheet — assertions run over these only. */
    private final List<String> present = new ArrayList<>();

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    private DocDistLibraryPage enterFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.waitForItem(FOLDER, Duration.ofSeconds(10))) {
            assumeTrue(createFolder(FOLDER), "Could not create the work folder");
            assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(45)), "Work folder not listed");
        }
        assumeTrue(lib.openItem(FOLDER), "Could not open the work folder");
        sleep(1200);
        return lib;
    }

    /**
     * Selects every uploaded file and opens the publish sheet from the selection bar.
     *
     * <p>Each row is SCROLLED INTO VIEW first. Without that, {@code selectRow}'s xpath only
     * ever sees the rows currently rendered, so a folder holding five files quietly
     * selected three of them — the sheet then opened on "3 documents", and the two missing
     * files were reported by the case as enabled-and-untagged rather than as absent.
     * Anything not selected is dropped from {@link #present}, so the assertions below run
     * over what is actually in the sheet.
     */
    private DocDistPublishSheet openPublishWithAllFiles(DocDistLibraryPage lib) {
        present.clear();
        for (String f : ALL_MEDIA) {
            if (!lib.waitForItem(f, Duration.ofSeconds(10))) continue;
            if (lib.selectRow(f)) present.add(f);
        }
        System.out.println(">>> selected for publish: " + present
                + (present.size() == ALL_MEDIA.length ? "" : " (INCOMPLETE)"));
        assumeTrue(present.contains(PDF),
                "The PDF must be selectable — the PDF row is the control in every case here");
        assumeTrue(present.size() >= 2,
                "Need at least a PDF and one non-PDF selected to test the gate, got " + present);
        assumeTrue(lib.openSelectionActions(), "Could not open the selection actions menu");
        sleep(800);
        assumeTrue(lib.tapMenuItem("Publish"), "Could not tap Publish");
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(sheet.isOpen(Duration.ofSeconds(15)), "The publish sheet did not open");
        return sheet;
    }

    // ==================== seed ====================

    @Test
    @Order(1)
    @DisplayName("SEED: one file of every supported family in a single folder")
    void seedEveryType() {
        DocDistLibraryPage lib = enterFolder();
        assumeTrue(lib.hasUpload(), "Upload not offered inside the folder on this build");

        List<String> missing = new ArrayList<>();
        for (String f : ALL_MEDIA) {
            if (!lib.waitForItem(f, Duration.ofSeconds(5))) missing.add(f);
        }
        if (!missing.isEmpty()) {
            assumeTrue(uploadFixturesIntoOpenFolder(ALL_MEDIA), "Upload pipeline did not complete");
            // Name the stragglers in STDOUT. Gradle does not log assumption messages, so a
            // per-file assumeTrue skipped the whole suite while saying nothing about WHICH
            // file was late — the run showed "picker selected 5 of 5" and "Upload 5 files
            // to this folder?" and then simply stopped.
            List<String> late = new ArrayList<>();
            for (String f : ALL_MEDIA) {
                if (!lib.waitForItem(f, Duration.ofSeconds(90))) late.add(f);
            }
            System.out.println(">>> SEED landed " + (ALL_MEDIA.length - late.size()) + "/"
                    + ALL_MEDIA.length + (late.isEmpty() ? "" : " — still missing " + late)
                    + "; folder now holds " + lib.itemNames());
            assumeTrue(late.isEmpty(), "These fixtures never appeared in the folder: " + late);
        }
        System.out.println(">>> seeded types: " + lib.itemNames());
        seeded = true;
        lib.returnToRoot();
    }

    // ==================== the gate ====================

    @Test
    @Order(2)
    @DisplayName("PTYPE-1: a PDF-only destination DISABLES every non-PDF row and tags it 'PDF only'")
    void pdfOnlyCategoryDisablesNonPdfRows() {
        assumeTrue(seeded, "Needs the seeded folder");
        DocDistLibraryPage lib = enterFolder();
        DocDistPublishSheet sheet = openPublishWithAllFiles(lib);

        try {
            assumeTrue(sheet.hasCategory(CAT_PDF_ONLY), "Call Sheet destination not offered");
            assumeTrue(sheet.tapCategory(CAT_PDF_ONLY), "Could not select the Call Sheet destination");
            sleep(1200);
            // Schedule/Script open a sub-selection; Call Sheet may too on some projects.
            sheet.dismissBlockingDialog();
            sleep(500);

            boolean pdfEnabled = rowEnabled(PDF);
            boolean pdfTagged = rowTagged(PDF);
            System.out.println(">>> PTYPE-1 pdf: enabled=" + pdfEnabled + " tagged=" + pdfTagged);

            StringBuilder wrong = new StringBuilder();
            for (String f : NON_PDF) {
                if (!present.contains(f)) continue;   // never selected — not the app's problem
                boolean enabled = rowEnabled(f);
                boolean tagged = rowTagged(f);
                System.out.println(">>> PTYPE-1 " + f + ": enabled=" + enabled + " tagged=" + tagged);
                if (enabled || !tagged) wrong.append(f).append(enabled ? "(enabled)" : "(untagged)").append(' ');
            }
            screenshot("ddpt-pdf-only");

            assertTrue(pdfEnabled, "The PDF row must stay selectable for a PDF-only destination");
            assertFalse(pdfTagged, "The PDF row must NOT carry the 'PDF only' disabled tag");
            assertTrue(wrong.length() == 0,
                    "Every non-PDF row must be DISABLED and tagged 'PDF only' for a pdfOnly "
                            + "destination (PublishBottomSheet.renderFiles: enabled = !pdfOnly || isPdf). "
                            + "Offenders: " + wrong);
        } finally {
            closeSheet(lib);
        }
    }

    @Test
    @Order(3)
    @DisplayName("PTYPE-2: the sheet explains the rule — \"Only PDF files can be published as …\"")
    void pdfOnlyNoteIsShown() {
        assumeTrue(seeded, "Needs the seeded folder");
        DocDistLibraryPage lib = enterFolder();
        DocDistPublishSheet sheet = openPublishWithAllFiles(lib);

        try {
            assumeTrue(sheet.tapCategory(CAT_PDF_ONLY), "Could not select the PDF-only destination");
            sleep(1200);
            sheet.dismissBlockingDialog();
            boolean note = sheet.textShown("Only PDF files", Duration.ofSeconds(5));
            System.out.println(">>> PTYPE-2 pdf-only note shown = " + note);
            screenshot("ddpt-note");
            assertTrue(note,
                    "A pdfOnly destination must explain itself with dd_publish_pdf_only_note "
                            + "(\"Only PDF files can be published as %s.\") — a disabled row with "
                            + "no explanation is the kind of dead end users report as a bug");
        } finally {
            closeSheet(lib);
        }
    }

    @Test
    @Order(4)
    @DisplayName("PTYPE-3: an UNRESTRICTED destination (Info) leaves every type selectable")
    void unrestrictedCategoryAcceptsEveryType() {
        assumeTrue(seeded, "Needs the seeded folder");
        DocDistLibraryPage lib = enterFolder();
        DocDistPublishSheet sheet = openPublishWithAllFiles(lib);

        try {
            assumeTrue(sheet.hasCategory(CAT_ANY_TYPE), "Info destination not offered");
            assumeTrue(sheet.tapCategory(CAT_ANY_TYPE), "Could not select the Info destination");
            sleep(1200);
            sheet.dismissBlockingDialog();

            StringBuilder blocked = new StringBuilder();
            for (String f : ALL_MEDIA) {
                boolean enabled = rowEnabled(f);
                boolean tagged = rowTagged(f);
                System.out.println(">>> PTYPE-3 " + f + ": enabled=" + enabled + " tagged=" + tagged);
                if (!enabled || tagged) blocked.append(f).append(' ');
            }
            screenshot("ddpt-info-any-type");
            assertTrue(blocked.length() == 0,
                    "INFO and CONFIDENTIAL_INFO carry neither pdfOnly nor documentsOnly, so "
                            + "every uploaded family must stay selectable. Blocked: " + blocked);
        } finally {
            closeSheet(lib);
        }
    }

    @Test
    @Order(5)
    @DisplayName("PTYPE-4: switching from Info to a PDF-only destination re-gates the rows live")
    void switchingCategoryRegatesRows() {
        assumeTrue(seeded, "Needs the seeded folder");
        DocDistLibraryPage lib = enterFolder();
        DocDistPublishSheet sheet = openPublishWithAllFiles(lib);

        // Pick a non-PDF that is genuinely IN the sheet rather than assuming NON_PDF[0]
        // was selectable — rowEnabled() now refuses to answer for an absent row.
        String probe = null;
        for (String f : NON_PDF) {
            if (present.contains(f)) { probe = f; break; }
        }
        assumeTrue(probe != null, "No non-PDF made it into the sheet, so there is nothing to re-gate");

        try {
            assumeTrue(sheet.tapCategory(CAT_ANY_TYPE), "Could not select Info");
            sleep(1000);
            sheet.dismissBlockingDialog();
            boolean beforeEnabled = rowEnabled(probe);

            assumeTrue(sheet.tapCategory(CAT_PDF_ONLY), "Could not switch to the PDF-only destination");
            sleep(1200);
            sheet.dismissBlockingDialog();
            boolean afterEnabled = rowEnabled(probe);

            System.out.println(">>> PTYPE-4 " + probe + " enabled: Info=" + beforeEnabled
                    + " → CallSheet=" + afterEnabled);
            screenshot("ddpt-regate");
            assumeTrue(beforeEnabled, "Info should have left the non-PDF selectable to begin with");
            assertFalse(afterEnabled,
                    "renderFiles() re-runs on every category change, so switching to a pdfOnly "
                            + "destination must disable the non-PDF row immediately — a stale "
                            + "enabled row would let a user check a file the payload then drops");
        } finally {
            closeSheet(lib);
        }
    }

    @Test
    @Order(6)
    @DisplayName("CLEANUP: the type-test folder and its files are removed")
    void cleanup() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Not at the root");
        assertTrue(deleteItem(FOLDER), "The type-test folder should delete");
    }

    // ---------------- helpers ----------------

    /** The publish row's own enabled state (rows reuse item_dd_picker_file → dd_row). */
    /**
     * True when the row is ENABLED — read off the row's ROOT, which is its PARENT.
     *
     * <p>{@code PublishFileAdapter} does {@code b.root.isEnabled = row.enabled}, and
     * {@code b.root} is the OUTER {@code LinearLayout} of item_dd_picker_file.xml — which
     * carries no id at all. {@code dd_row} is the layout's *inner* container (line 18), and
     * {@code enabled} is not inherited by children in Android, so that node reads
     * {@code enabled=true} no matter what the adapter did. Reading it reported every
     * non-PDF row as enabled and accused the app of not gating a PDF-only destination.
     *
     * <p>The tag alone already disproved that: {@code disabledTagText} is assigned
     * {@code if (enabled) null else "PDF only"}, so a tagged row is BY CONSTRUCTION a
     * disabled one — the run showed {@code tagged=true} on exactly the four non-PDF rows
     * and {@code tagged=false} on the PDF. Read the parent, and cross-check with the tag.
     */
    private boolean rowEnabled(String fileName) {
        try {
            String attr = driver.findElement(AppiumBy.xpath(rowXpath(fileName) + "/.."))
                    .getAttribute("enabled");
            return "true".equalsIgnoreCase(attr);
        } catch (Exception e) {
            // NOT a fallback to "enabled". An ABSENT row means the file never made it into
            // the sheet, and answering "enabled=true" for it is exactly the kind of
            // confident lie this suite exists to catch: the PDF and the JPG were missing
            // from a 3-document sheet and both read back as enabled-and-untagged, which
            // looked like the app failing to gate a JPG. Fail loudly instead.
            throw new AssertionError("No row for '" + fileName + "' in the publish sheet — it "
                    + "was not selected, so nothing can be said about its enabled state. "
                    + "Sheet holds: " + present, e);
        }
    }

    /** True when the row carries the "PDF only" disabled tag. */
    private boolean rowTagged(String fileName) {
        String xpath = rowXpath(fileName) + "//*[@resource-id='" + appPackage + ":id/dd_disabled_tag']";
        try {
            return !driver.findElements(AppiumBy.xpath(xpath)).isEmpty();
        } catch (Exception e) { return false; }
    }

    private String rowAttr(String fileName, String id, String attr) {
        try {
            return driver.findElement(AppiumBy.xpath(rowXpath(fileName))).getAttribute(attr);
        } catch (Exception e) { return ""; }
    }

    private String rowXpath(String fileName) {
        return "//*[@resource-id='" + appPackage + ":id/dd_row'][.//*[contains(@text,'"
                + fileName + "')]]";
    }

    /** Cancels out of the sheet and clears the selection — never publishes. */
    private void closeSheet(DocDistLibraryPage lib) {
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        try { sheet.dismissBlockingDialog(); } catch (Exception ignored) { }
        try { driver.navigate().back(); } catch (Exception ignored) { }
        sleep(1000);
        try { lib.confirmPopup().clearIfBlocking(); } catch (Exception ignored) { }
        if (lib.isSelectionMode()) lib.clearSelection();
        lib.returnToRoot();
    }
}
