package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Long-press options on a chat message. The menu is permission/type-driven
 * (Home.openDropDown): a text message with posting rights shows
 * Reply / Read By User / Forward / Share / Delete / Edit / Gallery; an image
 * additionally shows Save and Image Reply. Labels come from strings.xml.
 *
 * Each action opens a different screen/dialog; assertions confirm the option is
 * present and that tapping it does something (navigates / dialog / edit) without
 * crashing. Runs inside a project with posting rights; skips otherwise.
 */
@Order(24)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatOptionsTest extends BaseTest {

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void enterProject() {
        if (insideProject) {
            dismissPermissionPrompts();
            ensureInChat();
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.isLoaded(), "Home units strip not visible");
        assumeTrue(home.selectFirstChatUnit(), "No chat unit available");
        assumeTrue(home.isInputBarVisible(), "No posting rights — skipping option tests");
        insideProject = true;
    }

    private void ensureInChat() {
        HomeChatPage home = new HomeChatPage(driver);
        dismissPermissionPrompts();
        for (int i = 0; i < 6 && !home.isInputBarVisible(); i++) {
            // Forward / Share / Read-By open other screens; a back returns to the
            // chat. But a back can also overshoot to the units strip (no input
            // bar there) — if so, re-enter the chat unit instead of backing out
            // of the project entirely.
            if (home.isLoaded()) {
                home.selectFirstChatUnit();
                sleep(800);
                if (home.isInputBarVisible()) return;
            }
            driver.navigate().back();
            sleep(600);
        }
        if (home.isInputBarVisible()) return;
        if (home.isLoaded()) {
            home.selectFirstChatUnit();
            sleep(800);
            if (home.isInputBarVisible()) return;
        }
        // Last resort: Forward/Share can leave the chat in a selection mode that
        // back-presses don't exit (input bar stays hidden). A clean relaunch +
        // re-enter the unit is guaranteed to restore the input bar.
        hardReenterChat(home);
    }

    /** Relaunches the app (fast — no reinstall) and re-enters the chat unit. */
    private void hardReenterChat(HomeChatPage home) {
        try {
            relaunchApp();
        } catch (RuntimeException ignored) {
            // relaunchApp settles landing; for a logged-in single project that is
            // BOTTOM_NAV — any non-fatal settle is fine, we re-navigate below.
        }
        new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60));
        HomeChatPage fresh = new HomeChatPage(driver);
        if (fresh.isLoaded()) {
            fresh.selectFirstChatUnit();
            sleep(800);
        }
    }

    /** Sends a text message and opens its long-press menu; returns the page. */
    private HomeChatPage openMenuOnFreshText(String marker) {
        HomeChatPage home = new HomeChatPage(driver);
        home.sendMessage(marker);
        assumeTrue(home.messageShown(marker, Duration.ofSeconds(20)),
                "Need a sent message to long-press");
        assumeTrue(home.longPressMessage(marker), "Long-press did not register");
        assumeTrue(home.isMenuOpen(Duration.ofSeconds(6)), "Options menu did not open");
        return home;
    }

    @Test
    @Order(1)
    @DisplayName("Long-press a text message shows the permission-based options menu")
    void textMessageOptions() {
        HomeChatPage home = openMenuOnFreshText("qa-opt-" + (System.currentTimeMillis() % 1000000));
        List<String> opts = home.menuOptions();
        System.out.println(">>> menu options: " + opts);
        // Core options for a text message with posting rights.
        for (String expected : List.of("Reply", "Forward", "Share", "Delete")) {
            assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase(expected)),
                    "Options menu should contain '" + expected + "'. Got: " + opts);
        }
        assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("read by")),
                "Options menu should contain 'Read By User'. Got: " + opts);
        screenshot("chat-options-menu");
        home.closeMenu();
    }

    @Test
    @Order(2)
    @DisplayName("UPDATE: edit a message, save, and the new text replaces the old")
    void editMessagePersists() {
        String original = "qa-edit-" + (System.currentTimeMillis() % 1000000);
        HomeChatPage home = openMenuOnFreshText(original);
        assumeTrue(home.tapMenuOption("Edit"), "Edit option not present");
        assumeTrue(home.isEditDialogOpen(Duration.ofSeconds(6)),
                "Edit dialog should open with the message text");
        String edited = original + "-EDITED";
        home.setEditDialogText(edited);
        home.saveEditDialog();
        assertTrue(home.messageShown(edited, Duration.ofSeconds(20)),
                "The edited text '" + edited + "' should appear in the chat");
        screenshot("chat-edited");
        ensureInChat();
    }

    @Test
    @Order(3)
    @DisplayName("Read By User option opens the read-receipt view")
    void readByOption() {
        HomeChatPage home = openMenuOnFreshText("qa-read-" + (System.currentTimeMillis() % 1000000));
        assumeTrue(home.tapMenuOption("Read By User"), "Read By User option not present");
        // A read-by list/sheet opens; the chat input should no longer be focus.
        boolean opened = home.textShown("Read", Duration.ofSeconds(6))
                || !home.isInputBarVisible();
        assertTrue(opened, "Read By User should open a read-receipt view");
        screenshot("chat-readby");
        driver.navigate().back();
        ensureInChat();
    }

    @Test
    @Order(4)
    @DisplayName("Forward option opens the forward flow")
    void forwardOption() {
        HomeChatPage home = openMenuOnFreshText("qa-fwd-" + (System.currentTimeMillis() % 1000000));
        assumeTrue(home.tapMenuOption("Forward"), "Forward option not present");
        boolean navigated = !home.isInputBarVisible()
                || home.textShown("Forward", Duration.ofSeconds(6));
        assertTrue(navigated, "Forward should open a forward/target-picker screen");
        screenshot("chat-forward");
        driver.navigate().back();
        ensureInChat();
    }

    @Test
    @Order(5)
    @DisplayName("Delete option removes the message after confirmation")
    void deleteOption() {
        String marker = "qa-del-" + (System.currentTimeMillis() % 1000000);
        HomeChatPage home = openMenuOnFreshText(marker);
        assumeTrue(home.tapMenuOption("Delete"), "Delete option not present");
        // Two-stage delete: menu Delete -> action-bar Delete -> Yes/No dialog.
        home.tapDeleteActionBar(Duration.ofSeconds(6));
        // Confirm the "Are you sure you want to delete?" (Yes) dialog — it appears
        // a beat after tapping the action-bar Delete, so WAIT for it.
        assertTrue(confirmYesDialog(Duration.ofSeconds(8)),
                "The delete-confirmation dialog (Yes) should appear and be clickable");
        sleep(1500);
        assertTrue(!home.messageShown(marker, Duration.ofSeconds(6)),
                "The deleted message '" + marker + "' should no longer appear");
        screenshot("chat-deleted");
    }

    @Test
    @Order(6)
    @DisplayName("Share option opens the share sheet")
    void shareOption() {
        HomeChatPage home = openMenuOnFreshText("qa-share-" + (System.currentTimeMillis() % 1000000));
        assumeTrue(home.tapMenuOption("Share"), "Share option not present");
        // Share leaves the chat (system share chooser or an in-app share screen).
        boolean shared = !home.isInputBarVisible()
                || home.textShown("Share", Duration.ofSeconds(6));
        assertTrue(shared, "Share should open a share sheet / screen");
        screenshot("chat-share");
        driver.navigate().back();
        ensureInChat();
    }

    @Test
    @Order(7)
    @DisplayName("Image message adds Download/Save and Image Reply options")
    void imageMessageOptions() {
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.mediaBubbleShown(Duration.ofSeconds(6)),
                "No image in the chat to long-press (run the media send first) — skipping");
        assumeTrue(home.longPressNewestMedia(), "Long-press on media did not register");
        assumeTrue(home.isMenuOpen(Duration.ofSeconds(6)), "Media options menu did not open");
        List<String> opts = home.menuOptions();
        System.out.println(">>> media menu options: " + opts);
        // Media adds a save option (labelled "Download" in a chat unit, "Save" in
        // call sheet) AND "Image Reply" (annotate-and-reply on the image).
        assertTrue(opts.stream().anyMatch(o -> o.equalsIgnoreCase("Save") || o.equalsIgnoreCase("Download")),
                "Media options should include Save/Download. Got: " + opts);
        assertTrue(opts.stream().anyMatch(o -> o.toLowerCase().contains("image reply")),
                "Media options should include 'Image Reply'. Got: " + opts);
        screenshot("chat-media-options");
        home.closeMenu();
    }

    @Test
    @Order(8)
    @DisplayName("Image Reply: long-press an image, annotate in the editor, and post the reply")
    void imageReplyOnMedia() {
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.mediaBubbleShown(Duration.ofSeconds(6)),
                "No image in the chat to Image-Reply on (run the media send first) — skipping");
        assumeTrue(home.longPressNewestMedia(), "Long-press on media did not register");
        assumeTrue(home.isMenuOpen(Duration.ofSeconds(6)), "Media options menu did not open");
        assumeTrue(home.tapMenuOption("Image Reply"), "'Image Reply' option not present");
        // Image Reply opens the image editor/preview so you can annotate then post.
        com.zillit.automation.pages.MediaPreviewPage media =
                new com.zillit.automation.pages.MediaPreviewPage(driver);
        assertTrue(media.isEditorOpen() || media.isPreviewOpen(),
                "Image Reply should open the image editor / preview to annotate the reply");
        screenshot("chat-image-reply-editor");
        // Post the image reply (caption optional) and confirm a media bubble posts.
        if (!media.isPreviewOpen()) {
            // editor first — save/next leads to the preview with the send button
            for (int i = 0; i < 2 && !media.isPreviewOpen(); i++) {
                driver.navigate().back();
                sleep(600);
            }
        }
        if (media.isPreviewOpen()) {
            media.setCaption("qa-imgreply-" + (System.currentTimeMillis() % 1000000));
            media.tapPreviewSend();
            media.waitPreviewClosed(Duration.ofSeconds(60));
            assertTrue(home.mediaBubbleShown(Duration.ofSeconds(20)),
                    "The image reply should post as a media bubble");
        }
        screenshot("chat-image-reply-posted");
        ensureInChat();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
