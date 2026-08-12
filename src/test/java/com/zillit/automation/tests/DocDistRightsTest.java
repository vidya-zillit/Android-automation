package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.ConfirmPopupPage;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistLibraryPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — area O (rights &amp; permissions) of
 * DOC_DISTRIBUTION_TEST_CASES.md.
 *
 * <p><b>The spec's expectations for this area are out of date, and the source says why.</b>
 * O-1/O-2 were written as "actions hidden/blocked" and "Download is hidden", which was the
 * ORIGINAL behaviour. QA then asked for the opposite, and the app now implements the
 * flipped rule in {@code PostingRightsGate.runWithPostingRights} /
 * {@code DownloadRightsGate.runWithDownloadRights}:
 *
 * <ul>
 *   <li>Every posting/download control stays VISIBLE and ENABLED regardless of rights
 *       ({@code LibraryFragment.render()} even hard-codes {@code adapter.canPost = true}).</li>
 *   <li>The CLICK is the gate: without the right, a confirmation dialog appears offering
 *       to request it from an admin ({@code dd_no_posting_rights_on_module} /
 *       {@code dd_no_download_rights_request}), and the action does NOT run.</li>
 *   <li>Admins bypass everything ({@code DocDistRights.current()} folds in {@code isAdmin}).</li>
 * </ul>
 *
 * <p>So "the button is missing" is now a FAILURE mode, not the expected behaviour. These
 * cases assert the flipped rule and report which branch this account took, because a
 * single account can only ever exercise one of them.
 *
 * <p><b>Deliberately not executed:</b> O-3 stops at the dialog — tapping "Proceed" calls
 * {@code BaseViewModel.requestToAdmin}, which posts a real message into a real admin's
 * chat. An automated suite must not spam a human, so the case asserts the request path is
 * OFFERED and declines. O-5 (rights changed elsewhere) and O-6 (no view rights hides the
 * tool) need a SECOND account whose rights differ; they skip with that stated.
 */
@Order(TestOrder.DOC_DISTRIBUTION_RIGHTS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistRightsTest extends DocDistBaseTest {

    /** Recognisable fragments of the two request-permission dialogs. */
    private static final String[] REQUEST_DIALOG_MARKERS =
            {"permission", "rights", "access", "request", "admin"};

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    // ==================== O-1 / O-4 · posting ====================

    @Test
    @Order(1)
    @DisplayName("O-1/O-4: Compose is always OFFERED; the click either composes (canPost) or asks to request rights")
    void composeIsGatedOnClickNotVisibility() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);

        // The flipped rule: visibility is unconditional. A missing control here is the
        // pre-QA-flip build, not a rights outcome (trap 3.17).
        assertTrue(lib.hasCompose(),
                "Compose must be OFFERED regardless of rights — the current app gates the "
                        + "CLICK via runWithPostingRights, it does not hide the control. "
                        + "An absent Compose means a pre-flip build; check versionName (3.17).");

        assumeTrue(lib.openCompose(), "Could not tap Compose");
        sleep(1500);

        ConfirmPopupPage dialog = new ConfirmPopupPage(driver);
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        boolean asked = dialog.isShown(Duration.ofSeconds(4));
        boolean opened = !asked && composer.isOpen(Duration.ofSeconds(10));
        System.out.println(">>> compose click → requestDialog=" + asked + " composerOpened=" + opened
                + (asked ? " dialog='" + dialog.text() + "'" : ""));
        screenshot("ddr-compose-gate");

        try {
            if (asked) {
                // No canPost. The dialog must be the request-rights one, and it must offer
                // both a decline and a proceed — that IS the O-1 behaviour.
                String text = dialog.text().toLowerCase();
                assertTrue(mentionsRights(text),
                        "Without posting rights the click should raise the request-permission "
                                + "dialog. Got: '" + dialog.text() + "'");
            } else {
                // canPost (or admin) — O-4: the composer is genuinely reachable.
                assertTrue(opened,
                        "With posting rights, tapping Compose must open the composer; "
                                + "neither the composer nor a rights dialog appeared");
            }
        } finally {
            if (asked) dialog.cancel(Duration.ofSeconds(3));
            else composer.tapCancel();
        }
    }

    // ==================== O-2 · download ====================

    @Test
    @Order(2)
    @DisplayName("O-2: Download stays in the file row menu regardless of rights (the click is the gate)")
    void downloadIsOfferedAndGatedOnClick() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // Documents live INSIDE folders (Upload is folder-scoped — trap 3.11), and every
        // other suite deletes the folder it created, so the root is usually folders-only.
        // Looking for a file at the root therefore found nothing and the case failed for
        // a reason that had nothing to do with rights. Walk into folders until a file
        // turns up, and skip honestly if the project genuinely has no documents.
        String file = firstFileName(lib);
        if (file == null) {
            for (String folder : lib.itemNames()) {
                if (!lib.openItem(folder)) continue;
                sleep(2000);
                file = firstFileName(lib);
                if (file != null) break;
                lib.returnToRoot();
                sleep(800);
            }
        }
        // Still nothing? SEED one. Every other suite deletes the folder it created, so
        // whether this case runs at all depended on which suites happened to go first —
        // it skipped in the verify run purely because it is ordered ahead of the two that
        // upload. A case that tests the row menu should provide its own row.
        if (file == null) {
            // Step by step, and SAY which step failed — the first attempt printed only
            // "seeded its own document: null", which named none of the four things that
            // could have gone wrong.
            String seedFolder = "QA_RIGHTS_" + (System.currentTimeMillis() % 100000);
            boolean created = createFolder(seedFolder);
            boolean listed = created && lib.waitForItem(seedFolder, Duration.ofSeconds(45));
            boolean opened = listed && lib.openItem(seedFolder);
            if (opened) sleep(1200);
            boolean upload = opened && lib.hasUpload();
            boolean pushed = upload && uploadFixtureIntoOpenFolder("zl_dd_doc.pdf");
            boolean landed = pushed && lib.waitForItem("zl_dd_doc.pdf", Duration.ofSeconds(90));
            if (landed) file = "zl_dd_doc.pdf";
            System.out.println(">>> O-2 seed: created=" + created + " listed=" + listed
                    + " opened=" + opened + " uploadOffered=" + upload + " pushed=" + pushed
                    + " landed=" + landed);
        }
        assumeTrue(file != null,
                "This project has no document anywhere in Document Distribution and one "
                        + "could not be uploaded, so there is no file row menu to inspect.");

        assumeTrue(lib.openRowMenu(file), "Could not open the file's ⋮ menu");
        sleep(1200);
        boolean download = lib.textShown("Download", Duration.ofSeconds(3));
        System.out.println(">>> file row menu offers Download = " + download);
        screenshot("ddr-download-offered");
        try {
            assertTrue(download,
                    "LibraryFragment sets dd_file_download.isVisible = true unconditionally; "
                            + "the download-rights check happens on click "
                            + "(runWithDownloadRights). A hidden Download means a pre-flip build.");
        } finally {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
            lib.returnToRoot();   // this case may have walked into a folder to find a file
        }
    }

    // ==================== O-3 · request path ====================

    @Test
    @Order(3)
    @DisplayName("O-3: the request-rights dialog offers Proceed — asserted, then DECLINED on purpose")
    void requestRightsIsOfferedButNotSent() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openCompose(), "Could not tap Compose");
        sleep(1500);

        ConfirmPopupPage dialog = new ConfirmPopupPage(driver);
        boolean asked = dialog.isShown(Duration.ofSeconds(4));
        if (!asked) {
            // This account HAS the right, so the request path cannot be reached at all.
            DocDistComposerPage composer = new DocDistComposerPage(driver);
            if (composer.isOpen(Duration.ofSeconds(5))) composer.tapCancel();
            assumeTrue(false,
                    "This account has posting rights, so the request-rights dialog never "
                            + "appears. O-3 needs an account WITHOUT canPost — the same "
                            + "second account O-5/O-6 need.");
        }

        System.out.println(">>> request dialog: '" + dialog.text()
                + "' confirm='" + dialog.confirmLabel() + "' cancel='" + dialog.cancelLabel() + "'");
        screenshot("ddr-request-dialog");
        try {
            assertTrue(!dialog.confirmLabel().isBlank(),
                    "The request dialog must offer a Proceed action (it calls "
                            + "BaseViewModel.requestToAdmin)");
        } finally {
            // NOT accepted on purpose: Proceed posts a real permission request into a real
            // admin's chat. Asserting the affordance is the honest limit for automation.
            dialog.cancel(Duration.ofSeconds(3));
        }
    }

    // ==================== O-5 / O-6 · need a second account ====================

    @Test
    @Order(4)
    @DisplayName("O-5: rights changed elsewhere apply on reload — needs an admin to flip them mid-run")
    void rightsChangeAppliesOnReload() {
        assumeTrue(false,
                "NOT AUTOMATED HERE. The app keeps rights fresh from the socket "
                        + "(updatePostingRightsList → BaseViewModel.postingRightsList, read on "
                        + "demand by DocDistRights.current()). Exercising it needs a SECOND, "
                        + "admin session to revoke/grant this user's document_distribution_tool "
                        + "rights while this suite watches — the same two-device harness "
                        + "DRIVE_FOLDER_SHARE uses. Wire that up before claiming coverage.");
    }

    @Test
    @Order(5)
    @DisplayName("O-6: no view rights hides the tool entirely — needs a second account without canView")
    void noViewRightsHidesTool() {
        assumeTrue(false,
                "NOT AUTOMATED HERE. Entry is gated at the Tools screen by "
                        + "DocDistRights.canView, so proving it needs an account WITHOUT view "
                        + "access; this account plainly has it (every other case in this suite "
                        + "just used the module). Needs a second logged-in user.");
    }

    // ---------------- helpers ----------------

    private boolean mentionsRights(String lowercaseText) {
        for (String marker : REQUEST_DIALOG_MARKERS) {
            if (lowercaseText.contains(marker)) return true;
        }
        return false;
    }

    /**
     * The first row that is a FILE.
     *
     * <p>Discriminates on {@code dd_send}, NOT on {@code dd_folder_badge}. The badge is
     * the item COUNT and {@code LibraryAdapter.bindBadge()} sets it {@code View.GONE}
     * whenever the count is zero — so every EMPTY folder is badge-less, absent from the
     * accessibility tree, and was being read as a file. That is exactly what happened
     * here: the root held nothing but emptied QA_* folders, the first one was picked as
     * "the file", and O-2 opened a FOLDER's menu (Edit / Delete) and then reported that
     * the app had dropped Download from the file menu.
     *
     * <p>{@code dd_send} and {@code dd_publish} are declared in item_dd_library_folder and
     * NOT in item_dd_library_file, and the rights model gates their CLICK rather than
     * their visibility — so their presence is a structural fact about the row type,
     * independent of both content and permissions.
     */
    private String firstFileName(DocDistLibraryPage lib) {
        for (String name : lib.itemNames()) {
            String xpath = "//*[@resource-id='" + appPackage + ":id/dd_card']"
                    + "[.//*[@text='" + name + "']]"
                    + "//*[@resource-id='" + appPackage + ":id/dd_send']";
            try {
                if (driver.findElements(io.appium.java_client.AppiumBy.xpath(xpath)).isEmpty()) {
                    return name;
                }
            } catch (Exception ignored) { }
        }
        return null;
    }
}
