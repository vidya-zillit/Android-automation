package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.DocDistPublishSheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — area N (publish) of DOC_DISTRIBUTION_TEST_CASES.md, N-1…N-14.
 *
 * <p><b>THIS SUITE NEVER PUBLISHES ANYTHING, AND THAT IS DELIBERATE.</b> Publishing is the
 * one action in this module that writes OUTSIDE it: a published document lands in Call
 * Sheet, Production Report, Info, Confidential Info, the D.O.D tool, the schedule
 * distribution tool or the script distribution tool — as a real chat post in a real
 * project, visible to everyone on it. This suite has no way to delete any of that. So
 * every case here drives the sheet to the edge of the Publish button and then CANCELS
 * (which is N-14, asserted properly rather than used as a shortcut). N-9 — "publishing
 * posts the document" — is the one case that cannot be honestly automated against a shared
 * project; it is present, explicitly {@code @Disabled}, and documents what it would need.
 *
 * <p>Everything else in area N is assertable without publishing, because the sheet does all
 * of its work locally: the seven destination cards, their access badges, the sub-selection
 * PopupMenus, the per-category required-field rules, and Publish's own enabled state
 * ({@code updatePublishEnabled()} → {@code canPublish()}) are all client-side.
 *
 * <p><b>Three findings from the source that contradict the spec table</b> — each is
 * asserted here as the app actually behaves, not as the table describes:
 * <ol>
 *   <li><b>N-10/N-11/N-12 (republish) do not exist on this build.</b> The entire ZL-20245
 *       block — {@code dd_republish_section}, the three radios, the target dropdown — is
 *       commented out in BOTH {@code PublishBottomSheet.kt} AND
 *       {@code bottomsheet_dd_publish.xml}. Those ids are not compiled into the layout, so
 *       there is nothing to drive. The suite asserts the absence and reports it.</li>
 *   <li><b>N-4: view-only categories ARE selectable.</b> The table says they are not.
 *       {@code wireToolDestinations()} says the opposite in a comment: "Tapping a tile
 *       selects it regardless of badge — that's the path the user takes to reach the
 *       Request Posting Rights banner". The gate is on the Publish CLICK, not on
 *       selection.</li>
 *   <li><b>The badge ids are asymmetric.</b> Only the four module cards have
 *       {@code _badge}; the three tool cards have {@code _view_only}; only schedule and
 *       script have {@code _chevron} / {@code _selected}. See
 *       {@link DocDistPublishSheet}.</li>
 * </ol>
 *
 * <p><b>The modal trap.</b> Selecting a destination whose tool has no rights row fires
 * {@code promptToolNotEnabled} immediately — every time, not once — and that dialog is
 * built with a null left button and {@code setCancelable(false)}. BACK does not close it
 * and there is nothing to decline; only "OK" gets out.
 * {@link DocDistPublishSheet#dismissBlockingDialog()} handles both dialog shapes and never
 * presses "Proceed" (which would post a real permission request into an admin's chat).
 * Every case that taps a card unwinds through it in a finally block.
 *
 * <p>Seeds its own world — one QA folder plus the fixture {@code zl_dd_doc.pdf} — and
 * deletes it at the end, exactly like {@link DocDistFilesTest}.
 */
@Order(TestOrder.DOC_DISTRIBUTION_PUBLISH)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistPublishTest extends DocDistBaseTest {

    /** Fixture pushed by run-and-report.sh → /sdcard/Download/zl_dd_doc.pdf. */
    private static final String DOC = "zl_dd_doc.pdf";
    private static final String FOLDER = "QA_PUB_" + (System.currentTimeMillis() % 100000);

    private static boolean seeded;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    // ==================== seeding ====================

    private DocDistLibraryPage enterWorkFolder() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.waitForItem(FOLDER, Duration.ofSeconds(10))) {
            assumeTrue(createFolder(FOLDER), "Could not create the work folder");
            assumeTrue(lib.waitForItem(FOLDER, Duration.ofSeconds(45)), "Work folder not listed");
        }
        assumeTrue(lib.openItem(FOLDER), "Could not open the work folder");
        sleep(2000);
        assumeTrue(lib.isInFolder(), "Should be inside the work folder now");
        return lib;
    }

    /**
     * Guarantees the fixture is present.
     *
     * <p>The fixture is a PDF on purpose: every one of the seven categories is
     * {@code pdfOnly}, so a non-PDF would be rendered disabled in the sheet's checklist and
     * every required-field case would be blocked behind the wrong gate.
     */
    private DocDistLibraryPage enterWorkFolderWithFixture() {
        DocDistLibraryPage lib = enterWorkFolder();
        if (seeded && lib.waitForItem(DOC, Duration.ofSeconds(15))) return lib;
        if (!lib.waitForItem(DOC, Duration.ofSeconds(10))) {
            assumeTrue(lib.hasUpload(),
                    "Upload is not offered inside the folder on this build (stale build or "
                            + "rights-hidden, traps 3.11/3.17) — area N needs a seeded file");
            assumeTrue(uploadFixtureIntoOpenFolder(DOC), "The upload pipeline did not complete");
            assumeTrue(lib.waitForItem(DOC, Duration.ofSeconds(60)),
                    "The uploaded fixture never appeared in the folder");
        }
        seeded = true;
        return lib;
    }

    /**
     * File row ⋮ → Publish → the sheet.
     *
     * <p>{@code onFilePublish} is wrapped in {@code runWithPostingRights}, so a user
     * without {@code canPost} on Document Distribution ITSELF gets the admin-request dialog
     * and never reaches the sheet. That is declined (never "Proceed") and reported.
     */
    private boolean openPublishSheet(DocDistLibraryPage lib, DocDistPublishSheet sheet) {
        if (!lib.openRowMenu(DOC)) return false;
        sleep(1200);
        if (!lib.tapMenuItem("Publish")) return false;
        if (lib.confirmPopup().isShown(Duration.ofSeconds(4))) {
            System.out.println(">>> publish blocked by the module posting-rights dialog: '"
                    + lib.confirmPopup().text() + "'");
            lib.confirmPopup().cancel(Duration.ofSeconds(3));
            return false;
        }
        return sheet.isOpen(Duration.ofSeconds(20));
    }

    private String sheetSkipReason() {
        return "The publish sheet was not reachable — the file row's Publish is wrapped in "
                + "runWithPostingRights, so a user without canPost on Document Distribution "
                + "gets the admin-request dialog instead (trap 3.6). Environment gap.";
    }

    /** Cancels out of the sheet, clearing any modal first. Safe to call twice. */
    private void closeSheet(DocDistPublishSheet sheet) {
        for (int i = 0; i < 3 && sheet.dialogShown(); i++) sheet.dismissBlockingDialog();
        sheet.tapCancel();
        sheet.waitClosed(Duration.ofSeconds(8));
        sleep(700);
    }

    // ==================== N-1 · the sheet opens with the files ====================

    @Test
    @Order(1)
    @DisplayName("N-1: Publish on a file row opens the sheet listing that document")
    void publishSheetOpensWithFiles() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            String title = sheet.title();
            List<String> files = sheet.fileNames();
            System.out.println(">>> N-1 title='" + title + "' files=" + files);
            screenshot("pub-sheet-open");

            // dd_files is scoped on purpose: dd_name is ALSO every library row's id, and
            // the library is directly behind this sheet (trap 3.3, in a sheet).
            assertTrue(files.stream().anyMatch(f -> f.contains(DOC)),
                    "dd_files should list the document publish was invoked on, got " + files);
            assertFalse(title.isBlank(), "The sheet header should name what is being published");

            // Every file arrives pre-checked (checkedIds is seeded from the argument list),
            // but Publish is still disabled: no category is selected yet.
            assertFalse(sheet.isPublishEnabled(),
                    "Publish must be disabled until a destination is chosen "
                            + "(canPublish returns false on a null selectedCategory)");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-2 / N-3 · categories and badges ====================

    @Test
    @Order(2)
    @DisplayName("N-2/N-3: all seven destinations render, each with its own access-badge id")
    void sevenCategoriesAndBadges() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            List<String> shown = sheet.visibleCategories();
            System.out.println(">>> N-2 categories on screen=" + shown);
            screenshot("pub-categories");

            // The tool cards used to be hidden without view access; since
            // PUBLISH_SHOW_ALL_REQUEST_RIGHTS they are unconditionally VISIBLE
            // ("card.visibility = View.VISIBLE" in wireToolDestinations) so that the
            // request-rights path is discoverable. All seven must therefore be present.
            assertEquals(DocDistPublishSheet.CATEGORY_IDS.size(), shown.size(),
                    "All seven destinations are unconditionally visible; missing "
                            + DocDistPublishSheet.CATEGORY_IDS.stream()
                                    .filter(id -> !shown.contains(id)).toList());

            // N-3 — badges are STATE, not decoration: applyAccessBadge hides the badge
            // entirely when the user has posting access. So the assertion is on the two
            // strings the app can render, never on a badge being present (trap 3.2's
            // lesson: read the badge, do not demand one).
            Map<String, String> badged = sheet.badgedCategories();
            System.out.println(">>> N-3 badged categories=" + badged);
            for (Map.Entry<String, String> e : badged.entrySet()) {
                assertTrue(DocDistPublishSheet.BADGE_VIEW_ONLY.equalsIgnoreCase(e.getValue())
                                || DocDistPublishSheet.BADGE_NO_ACCESS.equalsIgnoreCase(e.getValue()),
                        "applyAccessBadge only ever renders 'View only' or 'No access'; "
                                + e.getKey() + " reads '" + e.getValue() + "'");
            }
            if (badged.isEmpty()) {
                System.out.println(">>> N-3: no badges at all — this user has posting access "
                        + "on every destination, which is a valid (and unbadged) state");
            }

            // The asymmetry the spec's identifier list hides.
            boolean scheduleChevron = sheet.chevronShown("dd_cat_schedule");
            boolean scriptChevron = sheet.chevronShown("dd_cat_script");
            boolean dodChevron = sheet.chevronShown("dd_cat_dod");
            System.out.println(">>> chevrons schedule=" + scheduleChevron + " script="
                    + scriptChevron + " dod=" + dodChevron);
            assertTrue(scheduleChevron && scriptChevron,
                    "Multi-option destinations carry a chevron");
            assertFalse(dodChevron,
                    "D.O.D is single-option, so wireToolDestinations sets its chevron GONE — "
                            + "the spec's blanket '+ _chevron variants' is wrong for this card");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-4 · view-only categories ====================

    @Test
    @Order(3)
    @DisplayName("N-4: a badged destination is still SELECTABLE — the gate is on Publish, not on the tile")
    void viewOnlyCategoriesAreStillSelectable() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            Map<String, String> badged = sheet.badgedCategories();
            assumeTrue(!badged.isEmpty(),
                    "This user has posting access on all seven destinations, so there is no "
                            + "view-only / no-access tile to exercise. Genuine environment "
                            + "state, not a dodged assertion — re-run as a view-only user.");

            String catId = badged.keySet().iterator().next();
            String badge = badged.get(catId);
            System.out.println(">>> N-4 exercising '" + catId + "' badged '" + badge + "'");

            assumeTrue(sheet.tapCategory(catId), "Could not tap the badged destination");
            sleep(1200);
            // Multi-option cards open a PopupMenu first; take the first option so the
            // category actually activates.
            List<String> menu = sheet.popupMenuItems();
            if (!menu.isEmpty()) {
                System.out.println(">>> N-4 sub-options=" + menu);
                sheet.tapMenuOptionAt(0);
                sleep(1200);
            }

            // Two possible outcomes, both correct, both proving the tile WAS selected:
            //  - NO_POSTING  → dd_request_rights_banner appears under the grid
            //  - NOT_ENABLED → the "Module not enabled" modal fires immediately
            boolean modal = sheet.dialogShown();
            String modalText = modal ? sheet.dialogText() : "";
            if (modal) sheet.dismissBlockingDialog();
            boolean banner = !modal && sheet.requestRightsBannerShown();

            System.out.println(">>> N-4 modal=" + modal + " '" + modalText + "' banner=" + banner);
            screenshot("pub-view-only");

            assertTrue(modal || banner,
                    "Selecting a badged destination must produce feedback — the 'Module not "
                            + "enabled' popup (no rights row) or the request-rights banner "
                            + "(row without posting access). Neither appeared, so the tap was "
                            + "swallowed. NOTE: the spec's 'not selectable' is wrong — "
                            + "wireToolDestinations selects regardless of badge.");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-5 / N-6 · sub-selection ====================

    @Test
    @Order(4)
    @DisplayName("N-5: Schedule opens a three-option sub-selection and records the pick")
    void scheduleSubSelection() {
        assertSubSelection("dd_cat_schedule", 3, DocDistPublishSheet.SCHEDULE_OPTIONS, "pub-schedule-menu");
    }

    @Test
    @Order(5)
    @DisplayName("N-6: Script opens a two-option sub-selection and records the pick")
    void scriptSubSelection() {
        assertSubSelection("dd_cat_script", 2, DocDistPublishSheet.SCRIPT_OPTIONS, "pub-script-menu");
    }

    /**
     * Shared body for N-5 and N-6.
     *
     * <p>Asserts the COUNT and the recorded selection rather than the option strings:
     * {@code resolveOptionLabel()} runs every label through the project's server-supplied
     * label dictionary first, so a project shipping its own copy legitimately renders
     * different text. The count and order are what the app guarantees.
     */
    private void assertSubSelection(String catId, int expectedOptions,
                                    List<String> localLabels, String shot) {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            assertTrue(sheet.chevronShown(catId),
                    catId + " is a multi-option destination and must show its chevron");
            String before = sheet.selectedLabel(catId);

            assumeTrue(sheet.tapCategory(catId), "Could not tap " + catId);
            sleep(1300);
            List<String> options = sheet.popupMenuItems();
            System.out.println(">>> " + catId + " options=" + options
                    + " (local strings would be " + localLabels + ")");
            screenshot(shot);

            assertEquals(expectedOptions, options.size(),
                    catId + " should offer exactly " + expectedOptions + " options, got " + options);

            assumeTrue(sheet.tapMenuOptionAt(0), "Could not pick the first option");
            sleep(1500);
            // Selecting a category can raise the not-enabled modal — clear it before
            // reading the card, or the read happens through a dialog.
            if (sheet.dialogShown()) {
                System.out.println(">>> " + catId + " selection dialog: '" + sheet.dialogText() + "'");
                sheet.dismissBlockingDialog();
            }

            String after = sheet.selectedLabel(catId);
            System.out.println(">>> " + catId + " sub-label before='" + before + "' after='" + after + "'");
            assertFalse(after.isBlank(), catId + "_selected should carry the picked option");
            assertFalse(after.equals(before),
                    "Picking an option rewrites " + catId + "_selected from the card subtitle to "
                            + "the 'Selected: …' form; it still reads '" + after + "'");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-7 · publication name required ====================

    @Test
    @Order(6)
    @DisplayName("N-7: D.O.D requires a publication Name — Publish stays DISABLED until it has one")
    void publicationNameRequired() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            // Name is required by exactly ONE category: SCHEDULE_DOD (requiresName = true
            // in PublishCategory). D.O.D is single-option, so one tap activates it.
            assumeTrue(sheet.tapCategory("dd_cat_dod"), "Could not select Schedule D.O.D");
            sleep(1500);
            if (sheet.dialogShown()) {
                String text = sheet.dialogText();
                sheet.dismissBlockingDialog();
                assumeTrue(false,
                        "Schedule D.O.D is not enabled / not permitted for this user, so its "
                                + "form never renders: '" + text + "'. Environment state.");
            }

            assertTrue(sheet.nameFieldShown(),
                    "renderExtras shows dd_pub_name_layout for a requiresName category");
            assertTrue(sheet.ensureFileChecked(DOC),
                    "The PDF row must be ticked so the only thing blocking Publish is the name");

            // THE GATE THE APP ACTUALLY USES: canPublish() returns false while the name is
            // blank, and updatePublishEnabled() pushes that straight onto the button. There
            // is no error text and no snackbar — the button state IS the validation.
            boolean blocked = !sheet.isPublishEnabled();
            System.out.println(">>> N-7 publish disabled with a blank name=" + blocked);
            screenshot("pub-name-blank");
            assertTrue(blocked,
                    "Publish must be DISABLED while the required Name is blank — the app "
                            + "gates by button state, not by an error message");

            assumeTrue(sheet.typeName("QA Publication " + (System.currentTimeMillis() % 1000)),
                    "Could not type into the publication name field");
            sleep(1200);
            boolean enabledNow = sheet.isPublishEnabled();
            boolean episodeNeeded = sheet.episodeFieldShown();
            System.out.println(">>> N-7 after typing a name: publishEnabled=" + enabledNow
                    + " episodeFieldShown=" + episodeNeeded + " name='" + sheet.nameText() + "'");

            assertFalse(sheet.nameText().isBlank(), "The name field should keep what was typed");

            if (!enabledNow && episodeNeeded) {
                // requiresEpisodeNumberOnTv — on a TELEVISION project the episode number is
                // a second required field, so a still-disabled button here is correct.
                assumeTrue(sheet.typeEpisode("1"), "Could not type an episode number");
                sleep(1200);
                enabledNow = sheet.isPublishEnabled();
                System.out.println(">>> N-7 after the episode number too: publishEnabled=" + enabledNow);
            }
            screenshot("pub-name-filled");
            assertTrue(enabledNow,
                    "With the file ticked and every required field filled, Publish should "
                            + "enable. It is NOT pressed — see the class javadoc (N-9).");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-8 · episode and scene ====================

    @Test
    @Order(7)
    @DisplayName("N-8: the scene (and, on TV projects, episode) inputs accept and keep their values")
    void episodeAndSceneAcceptInput() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            // Scene number is required by SCHEDULE_PAGE and PAGE_DISTRIBUTION only. The
            // Script card's SECOND option (Page) is the shorter route to it.
            assumeTrue(sheet.tapCategory("dd_cat_script"), "Could not tap Script & Pages");
            sleep(1300);
            List<String> options = sheet.popupMenuItems();
            assumeTrue(options.size() >= 2, "Script should offer two options, got " + options);
            assumeTrue(sheet.tapMenuOptionAt(1), "Could not pick the Page option");
            sleep(1500);
            if (sheet.dialogShown()) {
                String text = sheet.dialogText();
                sheet.dismissBlockingDialog();
                assumeTrue(false,
                        "Script & Pages Distribution is not enabled for this project: '"
                                + text + "'. Environment state.");
            }

            assertTrue(sheet.sceneFieldShown(),
                    "PAGE_DISTRIBUTION sets requiresSceneNumber, so dd_pub_scene_layout shows");
            assumeTrue(sheet.typeScene("42"), "Could not type a scene number");
            sleep(900);
            System.out.println(">>> N-8 scene='" + sheet.sceneText() + "'");
            assertEquals("42", sheet.sceneText(), "The scene number should keep what was typed");

            // Episode is TV-only: renderExtras hides it entirely unless
            // AppHelper.isProjectTypeTelevision(). On a film project its absence is the
            // correct behaviour, so it is reported rather than asserted.
            if (sheet.episodeFieldShown()) {
                assumeTrue(sheet.typeEpisode("7"), "Could not type an episode number");
                sleep(900);
                System.out.println(">>> N-8 episode='" + sheet.episodeText() + "'");
                assertEquals("7", sheet.episodeText(), "The episode number should keep what was typed");
            } else {
                System.out.println(">>> N-8: no episode field — renderExtras only shows it when "
                        + "AppHelper.isProjectTypeTelevision(), so this is a non-TV project");
            }

            // The rule note is the other half of the per-category chrome.
            System.out.println(">>> N-8 rule note='" + sheet.ruleNote() + "'");
            screenshot("pub-scene-episode");

            // Single-file categories CLEAR the checklist on selection — proving the sheet
            // reset the selection is part of asserting the extras really did switch.
            System.out.println(">>> N-8 fixture still checked after the category switch="
                    + sheet.fileChecked(DOC));
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-9 · NOT EXECUTED ====================

    /**
     * N-9 — publishing the document for real.
     *
     * <p><b>Deliberately not executed.</b> A successful publish posts the document into
     * another module of a SHARED project — a Call Sheet entry, an Info post, a
     * Confidential Info post, a schedule or a script distribution — as a real chat message
     * visible to every member. There is no in-app undo, no delete-published-file endpoint
     * this suite can reach, and no way to distinguish the test's post from a real one
     * afterwards. Running it once leaves permanent litter in someone's production project;
     * running it on every regression pass leaves a pile.
     *
     * <p>Everything N-9 would prove up to the button press IS covered:
     * {@link #publicationNameRequired()} drives a real category to the point where Publish
     * becomes ENABLED with a valid payload, which is the last observable state before the
     * network call. What is missing is only the round-trip and the appearance in the
     * target module.
     *
     * <p><b>To enable this case</b> it needs a disposable project — created by the suite,
     * published into, then deleted whole — plus a reader for the target module so
     * "appears in Call Sheet" is verified rather than assumed. Until that exists, a
     * passing N-9 would either be a lie (asserting only that the sheet closed) or a mess.
     */
    @Test
    @Order(8)
    @Disabled("N-9 writes into another module of a shared project and cannot be cleaned up — "
            + "needs a disposable project plus a target-module reader. See the javadoc.")
    @DisplayName("N-9: publishing posts the document (NOT RUN — no disposable project)")
    void publishingPostsTheDocument() {
        throw new UnsupportedOperationException(
                "N-9 must not run against a shared project: a publish is permanent and this "
                        + "suite cannot delete it.");
    }

    // ==================== N-10 / N-11 / N-12 · republish ====================

    @Test
    @Order(9)
    @DisplayName("N-10/N-11/N-12: the Replace / Addition / Resend block is NOT in this build")
    void republishSectionIsNotShipped() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            // Call Sheet and Production Report are the two categories ZL-20245 gated the
            // republish UI behind — if the block existed anywhere, it would be here.
            assumeTrue(sheet.tapCategory("dd_cat_callsheet"), "Could not select Call Sheet");
            sleep(2000);
            if (sheet.dialogShown()) {
                System.out.println(">>> call sheet selection dialog: '" + sheet.dialogText() + "'");
                sheet.dismissBlockingDialog();
            }
            boolean afterCallSheet = sheet.republishSectionShipped();

            assumeTrue(sheet.tapCategory("dd_cat_report"), "Could not select Production Report");
            sleep(2000);
            if (sheet.dialogShown()) {
                System.out.println(">>> production report selection dialog: '" + sheet.dialogText() + "'");
                sheet.dismissBlockingDialog();
            }
            boolean afterReport = sheet.republishSectionShipped();

            System.out.println(">>> N-10/11/12 republish present: callSheet=" + afterCallSheet
                    + " report=" + afterReport);
            screenshot("pub-republish-absent");

            // This is a real assertion, not a skip. The whole ZL-20245 block —
            // dd_republish_section, dd_republish_row_replace/_addition/_resend, their
            // radios and dd_republish_target_input — is commented out in BOTH
            // PublishBottomSheet.kt AND bottomsheet_dd_publish.xml, so those ids are not
            // compiled into the layout at all. If this ever fails, the feature has been
            // re-enabled and N-10/N-11/N-12 need writing against it.
            assertFalse(afterCallSheet || afterReport,
                    "The republish block is commented out in both the fragment and the layout "
                            + "on this build, so none of the dd_republish_* ids should exist. "
                            + "One of them did — ZL-20245 has landed and N-10/N-11/N-12 must be "
                            + "rewritten to drive the radios instead of asserting their absence.");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-13 · request rights ====================

    @Test
    @Order(10)
    @DisplayName("N-13: a destination without posting rights shows the request-rights banner and its dialog")
    void requestRightsBanner() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        try {
            Map<String, String> badged = sheet.badgedCategories();
            System.out.println(">>> N-13 badged categories=" + badged);
            assumeTrue(!badged.isEmpty(),
                    "This user has posting access on all seven destinations, so no destination "
                            + "can produce the request-rights state. Skipped because of the "
                            + "account's rights, not because the assertion was avoided — "
                            + "re-run as a view-only user to exercise it.");

            // The banner is the NO_POSTING state only: renderRequestRightsBanner explicitly
            // does NOT show it for NOT_ENABLED, where the modal is the only feedback. So
            // walk the badged cards until one produces a banner.
            boolean bannerFound = false;
            String producedBy = null;
            for (String catId : badged.keySet()) {
                if (!sheet.tapCategory(catId)) continue;
                sleep(1200);
                if (!sheet.popupMenuItems().isEmpty()) {
                    sheet.tapMenuOptionAt(0);
                    sleep(1500);
                }
                if (sheet.dialogShown()) {
                    System.out.println(">>> " + catId + " → not-enabled modal: '"
                            + sheet.dialogText() + "'");
                    sheet.dismissBlockingDialog();
                    continue;   // NOT_ENABLED — the banner is suppressed by design
                }
                if (sheet.requestRightsBannerShown()) {
                    bannerFound = true;
                    producedBy = catId;
                    break;
                }
            }

            System.out.println(">>> N-13 banner shown=" + bannerFound + " via " + producedBy);
            screenshot("pub-request-rights");

            assumeTrue(bannerFound,
                    "Every badged destination on this project reads 'No access' (no rights row "
                            + "at all), which renderRequestRightsBanner deliberately suppresses in "
                            + "favour of the 'Module not enabled' popup. The banner needs a "
                            + "destination the user can VIEW but not POST to.");

            assertTrue(sheet.requestRightsButtonShown(),
                    "The banner ships with its Request Posting Rights button");
            String message = sheet.requestRightsMessage();
            System.out.println(">>> N-13 banner message='" + message + "'");
            assertFalse(message.isBlank(),
                    "dd_request_rights_message should name the destination the rights are for");

            // THE BUILD'S ACTUAL BEHAVIOUR, reported rather than assumed: since the
            // ZL-19795 follow-up, missing rights never HIDE or DISABLE anything — Publish's
            // enabled state is decided purely by the field prerequisites, and the rights
            // popup chain fires on the CLICK. Whether the button is enabled here therefore
            // depends only on whether the file is ticked, so it is logged, not asserted.
            System.out.println(">>> N-13 publish button enabled without posting rights="
                    + sheet.isPublishEnabled()
                    + " (rights are enforced on click, never by hiding or disabling)");

            assumeTrue(sheet.tapRequestRights(), "Could not tap Request Posting Rights");
            boolean dialog = sheet.dialogShown();
            String text = dialog ? sheet.dialogText() : "";
            System.out.println(">>> N-13 request dialog='" + text + "'");
            screenshot("pub-request-dialog");

            // DECLINE. The right button is "Proceed", which runs
            // BaseViewModel.requestToAdmin and posts a real permission request into a real
            // admin's chat. dismissBlockingDialog presses the left button whenever one
            // exists, which is exactly this case.
            sheet.dismissBlockingDialog();

            assertTrue(dialog,
                    "Request Posting Rights opens the admin-request confirmation "
                            + "(dd_no_posting_rights_on_module). It was DECLINED — this suite "
                            + "never sends a real request.");
        } finally {
            closeSheet(sheet);
        }
    }

    // ==================== N-14 · cancel ====================

    @Test
    @Order(11)
    @DisplayName("N-14: Cancel closes the sheet and publishes nothing")
    void cancelPublishesNothing() {
        DocDistLibraryPage lib = enterWorkFolderWithFixture();
        DocDistPublishSheet sheet = new DocDistPublishSheet(driver);
        assumeTrue(openPublishSheet(lib, sheet), sheetSkipReason());

        // Take the sheet as far as a real publish attempt would go — a category selected,
        // the file ticked — and then back out. This is the exit every other case in the
        // suite uses, so N-14 is also the guarantee that none of them published anything.
        assumeTrue(sheet.tapCategory("dd_cat_info"), "Could not select Info");
        sleep(1500);
        if (sheet.dialogShown()) {
            System.out.println(">>> N-14 info selection dialog: '" + sheet.dialogText() + "'");
            sheet.dismissBlockingDialog();
        }
        sheet.ensureFileChecked(DOC);
        System.out.println(">>> N-14 before cancel: publishEnabled=" + sheet.isPublishEnabled());
        screenshot("pub-before-cancel");

        assertTrue(sheet.tapCancel(), "Could not tap Cancel");
        boolean closed = sheet.waitClosed(Duration.ofSeconds(10));
        sleep(1500);

        // LibraryFragment snacks "N document(s) published" on a successful publish result
        // (the PublishBottomSheet.REQUEST_KEY listener). Its absence is the assertable
        // proof that Cancel published nothing.
        boolean publishedSnack = lib.textShown("published", Duration.ofSeconds(4));
        System.out.println(">>> N-14 sheetClosed=" + closed + " publishedSnack=" + publishedSnack);
        screenshot("pub-after-cancel");

        assertTrue(closed, "Cancel should dismiss the publish sheet");
        assertFalse(publishedSnack,
                "Cancel must not publish — the library's 'published' confirmation snackbar "
                        + "should never appear");
        assertTrue(lib.isDocDistActivity(),
                "Cancelling returns to the library, not out of the module");
    }

    // ==================== CLEANUP ====================

    @Test
    @Order(12)
    @DisplayName("CLEANUP: the QA publish folder and its fixture are removed")
    void cleanup() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.returnToRoot(), "Could not return to the root for cleanup");
        assertTrue(deleteItem(FOLDER), "Cleanup: the QA folder should delete");
        screenshot("pub-cleanup");
    }
}
