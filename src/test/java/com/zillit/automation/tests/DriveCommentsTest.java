package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.DriveCommentsPage;
import com.zillit.automation.pages.DriveFileDetailPage;
import com.zillit.automation.pages.DriveHomePage;
import com.zillit.automation.pages.DriveUploadPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DRIVE COMMENTS — area P of DRIVE_TEST_CASES.md: the comments block on a file's detail
 * screen, and the full-screen list behind "View all N comments".
 *
 * <p>Three things shape every test here:
 *
 * <ul>
 *   <li><b>Rows have no ids.</b> Both comment adapters build their views in code, so a
 *       comment is located by its TEXT and its edit/delete buttons are reached by walking
 *       up from that text. Every body posted here is therefore stamped unique — two runs
 *       leaving identically-named comments would make the anchor ambiguous.</li>
 *   <li><b>The detail previews only two comments.</b> {@code FileDetailFragment} renders
 *       {@code comments.take(2)} and hides the rest behind the View-all button, and the
 *       view model APPENDS a new comment to the end of that list. So a just-posted
 *       comment is not guaranteed to be in the preview — {@link #commentLanded} accepts
 *       either surface, which is what "the comment reached the document" actually
 *       means.</li>
 *   <li><b>Nothing announces a rejected send.</b> An empty comment is dropped by a
 *       silent {@code return}: no toast, no error, no disabled button. The only readable
 *       proof is that the list did not change (3.1).</li>
 * </ul>
 */
@Order(TestOrder.DRIVE_COMMENTS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriveCommentsTest extends BaseTest {

    /**
     * This suite's OWN fixture. Sharing a file with DriveShareTest / DriveEditorTest is
     * how a cleanup step in one suite skipped dozens of cases in another.
     */
    private static final String NOTE = "zl_drive_note.txt";

    private static final String STAMP = String.valueOf(System.currentTimeMillis() % 100000);
    private static final String C_POST = "QA comment " + STAMP;
    private static final String C_EDITED = "QA comment edited " + STAMP;
    /** Three at once, so "View all" (which needs > 2) is rendered. */
    private static final String[] C_MANY = {
            "QA bulk one " + STAMP, "QA bulk two " + STAMP, "QA bulk three " + STAMP
    };

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
        assumeTrue(openDrive(), "Could not open Drive");
    }

    private boolean openDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        closeAnyOverlay();
        // isLoaded() also matches Trash/Search (shared recyclerView) — require HOME.
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        if (drive.isDriveActivity() && drive.returnHome()) return true;
        for (int i = 0; i < 8 && !onBottomNavScreen(); i++) {
            if (drive.isDriveActivity() && drive.isLoaded(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); Thread.sleep(800); } catch (Exception ignored) { }
        }
        // Retried: the FIRST test of a suite regularly finds the Tools list with "Drive"
        // typed and the row highlighted, yet Drive never comes up — the tap lands while
        // the filtered list is still settling. One attempt turns that into a skipped case
        // blamed on Drive.
        ToolsPage tools = new ToolsPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            tools.openToolsTab();
            if (!tools.isLoaded(Duration.ofSeconds(15))) { sleep(2000); continue; }
            tools.searchTools("Drive");
            sleep(2000);
            if (tools.isModuleListed("Drive", Duration.ofSeconds(6)) && tools.openModule("Drive")) {
                // 40s, not 20s: on a loaded emulator Drive's first open takes longer than
                // that, and the retry then pressed Tools again — cancelling a launch that
                // was about to succeed, over and over.
                if (drive.isLoaded(Duration.ofSeconds(40))) return true;
            }
            if (drive.isDriveActivity()) return true;
            sleep(2000);
        }
        return false;
    }

    private void closeAnyOverlay() {
        for (int i = 0; i < 5; i++) {
            // A stranded SYSTEM PICKER (a foreign package) is the worst case: every
            // later openDrive() fails and the rest of the suite skips.
            boolean foreign = false;
            try {
                String pkg = driver.getCurrentPackage();
                foreign = pkg != null && !pkg.contains("zillitapp");
            } catch (Exception ignored) { }
            boolean overlay = foreign
                    || !driver.findElements(byAppId("btnSend")).isEmpty()          // comments screen
                    || !driver.findElements(byAppId("btnSendComment")).isEmpty()   // file detail
                    || !driver.findElements(byAppId("menuDelete")).isEmpty()
                    || !driver.findElements(byAppId("navTrash")).isEmpty()
                    || !driver.findElements(byAppId("menuCreateFolder")).isEmpty()
                    || !driver.findElements(byAppId("btnUpload")).isEmpty();
            if (!overlay) return;
            try { driver.navigate().back(); Thread.sleep(1000); } catch (Exception ignored) { return; }
        }
    }

    private boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    // ---------------- fixture ----------------

    /**
     * Ensures this suite's file exists in Drive, uploading it if needed.
     *
     * <p>The FAB tap is retried: the first tap after landing on the list is sometimes
     * swallowed, and the upload then never starts — which skips every case here with a
     * misleading "could not get the file into Drive".
     */
    private boolean ensureFile() {
        DriveHomePage drive = new DriveHomePage(driver);
        // Poll before concluding the file is missing: the Files list is briefly EMPTY
        // while it reloads, and one read taken then sent this suite off to re-upload a
        // file that was already there — and skipped the case when that failed.
        if (waitForFixtureFile(Duration.ofSeconds(20))) return true;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (drive.openFab() && drive.tapUploadFile()) {
                DriveUploadPage up = new DriveUploadPage(driver);
                if (up.uploadFile("Document", NOTE)) {
                    sleep(10000);
                    return waitForFixtureFile(Duration.ofSeconds(30));
                }
            }
            // Unwind whatever half-open surface the failed attempt left (source sheet,
            // system picker, staging) before trying again.
            for (int i = 0; i < 4; i++) {
                if (new DriveHomePage(driver).isHome(Duration.ofSeconds(2))) break;
                try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
            }
        }
        return false;
    }

    /** Polls the Files tab for this suite's fixture. */
    private boolean waitForFixtureFile(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (new DriveHomePage(driver).hasFile(NOTE)) return true;
            sleep(2500);
        }
        return false;
    }

    /**
     * Opens the file's detail screen from Drive home.
     *
     * <p>Retried, and then WAITS for the comments block to bind: the file loads first and
     * its comments a beat later, so reading the list immediately reports an empty section
     * for a file that has comments — indistinguishable from "the comment was never
     * saved", which is precisely the question this suite has to answer.
     */
    private DriveFileDetailPage openDetail() {
        assumeTrue(ensureFile(), "Could not get " + NOTE + " into Drive");
        DriveFileDetailPage detail = new DriveFileDetailPage(driver);
        boolean open = false;
        for (int attempt = 0; attempt < 3 && !open; attempt++) {
            DriveHomePage drive = new DriveHomePage(driver);
            drive.showFiles();
            if (drive.openItem(NOTE)) {
                open = detail.isOpen(Duration.ofSeconds(20));
            }
            if (!open) {
                System.out.println(">>> file detail did not open (attempt " + (attempt + 1) + ")");
                for (int i = 0; i < 3; i++) {
                    if (new DriveHomePage(driver).isHome(Duration.ofSeconds(2))) break;
                    try { driver.navigate().back(); Thread.sleep(1200); } catch (Exception ignored) { }
                }
            }
        }
        assumeTrue(open, "File detail for " + NOTE + " did not open");
        detail.scrollToComments();
        detail.waitForCommentsLoaded(Duration.ofSeconds(25));
        return detail;
    }

    /**
     * Deletes every comment on the fixture file, so the cases below start from a known
     * zero.
     *
     * <p>Needed because the detail previews {@code comments.take(2)} and the view model
     * APPENDS: with leftovers from an earlier run, a freshly posted comment can sit
     * outside the preview entirely, and the edit/delete cases then cannot reach it. The
     * file is this suite's own fixture and every comment on it was posted by this suite,
     * so clearing it is safe.
     */
    private void clearComments(DriveFileDetailPage detail) {
        for (int pass = 0; pass < 10; pass++) {
            detail.waitForCommentsLoaded(Duration.ofSeconds(20));
            if (detail.commentCount() == 0 && detail.commentTexts().isEmpty()) return;
            boolean deleted = false;
            for (String row : detail.commentTexts()) {
                if (!detail.tapDeleteComment(row)) continue;
                if (detail.deleteDialogOpen() && detail.confirmDialog(Duration.ofSeconds(8))) {
                    sleep(2500);
                    deleted = true;
                    break;
                }
            }
            if (!deleted) return;   // nothing left we can act on
        }
    }

    /**
     * True when a posted comment is readable on EITHER surface.
     *
     * <p>The detail renders {@code comments.take(2)} while the view model appends new
     * comments to the end of the list, so on a file that already has two comments the
     * newest one is only visible behind View-all. Asserting on the preview alone would
     * fail for a comment that posted perfectly.
     */
    private boolean commentLanded(DriveFileDetailPage detail, String body) {
        if (detail.waitForComment(body, Duration.ofSeconds(25))) return true;
        if (!detail.hasViewAllComments()) return false;
        if (!detail.tapViewAllComments()) return false;
        sleep(2500);
        DriveCommentsPage all = new DriveCommentsPage(driver);
        boolean found = all.isOpen(Duration.ofSeconds(15)) && all.hasCommentScrolled(body);
        all.back();
        sleep(1500);
        return found;
    }

    /** Deletes a comment if it is on the detail preview. Used by cleanup. */
    private boolean deleteIfPresent(DriveFileDetailPage detail, String body) {
        if (!detail.hasComment(body)) return false;
        if (!detail.tapDeleteComment(body)) return false;
        if (!detail.deleteDialogOpen()) return false;
        detail.confirmDialog(Duration.ofSeconds(8));
        detail.waitForCommentGone(body, Duration.ofSeconds(20));
        return true;
    }

    // ==================== P · COMMENTS ====================

    @Test
    @Order(1)
    @DisplayName("P-1: the file detail shows a comments section — the list or 'no comments'")
    void commentsSectionShown() {
        DriveFileDetailPage detail = openDetail();

        assertTrue(detail.hasCommentsSection(), "A comments header is expected on the file detail");
        System.out.println(">>> file='" + detail.fileName() + "' header='" + detail.commentsHeader()
                + "' noComments=" + detail.noCommentsShown() + " input=" + detail.hasCommentInput());
        // One of the two valid states must render. Neither means the section failed to
        // bind — which looks identical to "no comments" if you only check the list.
        assertTrue(detail.commentsStateShown(),
                "The comments section must show either the list or the empty placeholder");
        assertTrue(detail.hasCommentInput(),
                "The owner of a file must get the comment composer (it is hidden only for "
                        + "view-only recipients)");
        screenshot("drive-comments-section");
        detail.back();
    }

    @Test
    @Order(2)
    @DisplayName("P-2: an EMPTY comment cannot be sent — the list is unchanged")
    void emptyCommentRejected() {
        DriveFileDetailPage detail = openDetail();
        assumeTrue(detail.hasCommentInput(), "No comment composer on this file");

        // The HEADER count, not the rendered rows: the preview binds lazily, so two row
        // reads seconds apart can differ (0 then 2) purely from rendering — which failed
        // this case while nothing had been added at all.
        int before = detail.commentCount();
        assumeTrue(detail.typeComment(""), "Could not clear the composer");
        detail.tapSend();
        sleep(3000);

        // The app drops an empty comment with a bare `return` — no toast, no error, no
        // disabled button. "The count did not move" is the only readable proof.
        int after = detail.commentCount();
        System.out.println(">>> comment count before=" + before + " after=" + after);
        assertEquals(before, after, "Sending an empty comment must not add anything");
        assertTrue(detail.commentsStateShown(),
                "The comments section must still be readable after the rejected send");
        screenshot("drive-comment-empty-rejected");
        detail.back();
    }

    @Test
    @Order(3)
    @DisplayName("P-3: posting a comment adds it to the file — and it survives reopening")
    void postComment() {
        DriveFileDetailPage detail = openDetail();
        assumeTrue(detail.hasCommentInput(), "No comment composer on this file");

        // Start from zero so the posted comment is inside the two-comment preview and the
        // edit/delete cases that follow can reach it.
        clearComments(detail);
        assumeTrue(detail.postComment(C_POST), "Could not type/send the comment");
        boolean landed = commentLanded(detail, C_POST);
        System.out.println(">>> posted '" + C_POST + "' landed=" + landed
                + " header='" + detail.commentsHeader() + "'");
        screenshot("drive-comment-posted");
        assertTrue(landed, "The posted comment should be readable on the file. Preview showed: "
                + detail.commentTexts());

        // ROUND TRIP — the only proof the comment reached the DOCUMENT rather than just
        // the screen. `addComment` appends to local state optimistically, so the list and
        // the "Comments (1)" header both update even if the server never stored it. Leave
        // the detail, come back, and read what the server actually returns.
        detail.back();
        sleep(2500);
        DriveFileDetailPage reopened = openDetail();
        boolean survived = commentLanded(reopened, C_POST);
        System.out.println(">>> after reopening: survived=" + survived
                + " header='" + reopened.commentsHeader() + "' preview=" + reopened.commentTexts());
        screenshot("drive-comment-round-trip");
        assertTrue(survived,
                "A posted comment must still be on the file after leaving and reopening it. "
                        + "It was visible immediately after posting but gone on reload, which means "
                        + "the comment never reached the server (or getComments drops it — the "
                        + "load failure is swallowed silently). Preview after reload: "
                        + reopened.commentTexts());
        reopened.back();
    }

    @Test
    @Order(4)
    @DisplayName("P-4: editing a comment replaces its text")
    void editComment() {
        DriveFileDetailPage detail = openDetail();
        assumeTrue(detail.waitForComment(C_POST, Duration.ofSeconds(25)),
                "The posted comment is not on the detail preview — the post step must pass first");

        assumeTrue(detail.tapEditComment(C_POST), "Could not tap Edit on that comment");
        assumeTrue(detail.editDialogOpen(), "The Edit Comment dialog did not open");
        assumeTrue(detail.setEditedText(C_POST, C_EDITED), "Could not type the new text");
        assertTrue(detail.confirmDialog(Duration.ofSeconds(10)), "Save should be tappable");
        sleep(2000);

        assertTrue(detail.waitForComment(C_EDITED, Duration.ofSeconds(25)),
                "The edited text should replace the old one. Got: " + detail.commentTexts());
        // Prove the list is readable before asserting the OLD text is gone — a dialog
        // left on top would make that assertion pass against nothing.
        assertFalse(detail.isDialogOpen(), "The edit dialog should be closed");
        assertTrue(detail.commentsStateShown(), "The comments list must be readable");
        assertFalse(detail.hasComment(C_POST),
                "The original text should no longer be listed. Got: " + detail.commentTexts());
        screenshot("drive-comment-edited");
        detail.back();
    }

    @Test
    @Order(5)
    @DisplayName("P-5: deleting a comment (confirm) removes it")
    void deleteComment() {
        DriveFileDetailPage detail = openDetail();
        assumeTrue(detail.waitForComment(C_EDITED, Duration.ofSeconds(20)),
                "The edited comment is missing — earlier steps must pass first");

        assumeTrue(detail.tapDeleteComment(C_EDITED), "Could not tap Delete on that comment");
        assertTrue(detail.deleteDialogOpen(), "A delete confirmation is expected");
        assertTrue(detail.confirmDialog(Duration.ofSeconds(10)), "Delete should be tappable");
        sleep(2500);

        assertFalse(detail.isDialogOpen(), "The confirm dialog should be gone");
        assertTrue(detail.commentsStateShown(),
                "The comments section must be readable before asserting the comment is gone");
        assertTrue(detail.waitForCommentGone(C_EDITED, Duration.ofSeconds(20)),
                "The deleted comment should disappear. Got: " + detail.commentTexts());
        screenshot("drive-comment-deleted");
        detail.back();
    }

    @Test
    @Order(6)
    @DisplayName("P-6: 'View all' opens the full comments screen with every comment")
    void viewAllComments() {
        DriveFileDetailPage detail = openDetail();
        assumeTrue(detail.hasCommentInput(), "No comment composer on this file");

        // Exactly three, from a clean slate: View-all only appears above two comments,
        // and leftovers would make the assertion depend on what earlier runs left behind.
        clearComments(detail);
        for (String body : C_MANY) {
            assumeTrue(detail.postComment(body), "Could not post " + body);
            sleep(3000);
        }
        sleep(2000);

        // View-all is rendered only above two comments — that is the case under test.
        assertTrue(detail.hasViewAllComments(),
                "With three comments the 'View all' button should be shown. Preview: "
                        + detail.commentTexts());
        System.out.println(">>> view-all label='" + detail.viewAllLabel()
                + "' preview=" + detail.commentTexts());
        assumeTrue(detail.tapViewAllComments(), "Could not tap View all");
        sleep(3000);

        DriveCommentsPage all = new DriveCommentsPage(driver);
        // btnSend exists ONLY here — checking rvComments/etComment would also match the
        // detail we just came from.
        assertTrue(all.isOpen(Duration.ofSeconds(15)),
                "The full comments screen (CommentsFragment) should open");
        List<String> texts = all.allCommentTextsScrolled();
        System.out.println(">>> full screen title='" + all.title() + "' texts=" + texts);
        screenshot("drive-comments-view-all");
        for (String body : C_MANY) {
            assertTrue(texts.contains(body),
                    "'" + body + "' should be listed on the full comments screen. Got: " + texts);
        }
        all.back();
    }

    @Test
    @Order(7)
    @DisplayName("CLEANUP: remove the comments this suite posted")
    void cleanup() {
        DriveFileDetailPage detail = openDetail();

        // Named ones first (so a partial run still tidies its own), then anything left —
        // only two are previewed at a time, so deleting the visible pair reveals the next.
        for (int pass = 0; pass < 4; pass++) {
            boolean deletedAny = false;
            for (String body : C_MANY) {
                if (deleteIfPresent(detail, body)) { deletedAny = true; sleep(1500); }
            }
            deleteIfPresent(detail, C_POST);
            deleteIfPresent(detail, C_EDITED);
            if (!deletedAny) break;
        }
        clearComments(detail);

        List<String> left = detail.commentTexts();
        System.out.println(">>> comments left on " + NOTE + ": " + left);
        assertTrue(detail.commentsStateShown(),
                "The comments section should still be readable after cleanup");
        detail.back();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
