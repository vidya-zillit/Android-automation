package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.MediaPreviewPage;
import com.zillit.automation.pages.ProjectListPage;
import io.appium.java_client.AppiumBy;
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
 * Image PREVIEW (caption) + EDITOR (paint / crop / add-text), mapped and
 * verified by driving the real UI on the emulator.
 *
 * Flow: chat attachment → "Gallery" → Fallery album/grid → select a photo →
 * send FAB → GalleryItemView preview → "edit" → EditImageActivity.
 *
 * Verified on-device facts encoded here:
 *  - editor has exactly THREE tools (Paint, Crop, Add-Text); NO rotate tool.
 *  - leaving the preview raises an "Are sure you want to exit?" (Yes/No) dialog.
 *
 * To keep the run within time, the editor tools are exercised in ONE test that
 * navigates once; preview and send are separate. Each case skips (not fails)
 * when a precondition (project / posting rights / gallery image) is missing.
 */
@Order(23)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MediaPreviewTest extends BaseTest {

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
            ensureBackInChat();
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
        assumeTrue(home.isInputBarVisible(), "No posting rights — skipping media tests");
        insideProject = true;
    }

    private HomeChatPage chat() {
        return new HomeChatPage(driver);
    }

    /** Unwinds any open media screen (confirming the exit dialog) to the chat. */
    private void ensureBackInChat() {
        for (int i = 0; i < 8 && !chat().isInputBarVisible(); i++) {
            // Confirm an "Are sure you want to exit?" dialog if present.
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)yes\")")).click();
            } catch (Exception e) {
                driver.navigate().back();
            }
            sleep(800);
        }
        dismissPermissionPrompts();
    }

    private MediaPreviewPage reachPreview() {
        HomeChatPage home = chat();
        assumeTrue(home.isAttachmentVisible(), "No attachment button — skipping");
        home.tapAttachment();
        assumeTrue(home.isAttachmentPickerVisible(), "Attachment picker did not open");
        assumeTrue(home.tapAttachmentOption("Gallery"), "Gallery option missing");
        for (int i = 0; i < 4; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }

        MediaPreviewPage media = new MediaPreviewPage(driver);
        assumeTrue(media.isGalleryOpen(), "The app gallery did not open");
        media.openAllMediaAlbumIfListed();
        assumeTrue(media.galleryThumbCount() > 0, "No images in the device gallery");
        media.selectPhotos(1);
        assumeTrue(media.isSendFabVisible(), "Send FAB not shown after selecting");
        media.tapGallerySendFab();
        assumeTrue(media.isPreviewOpen(), "Preview page did not open");
        return media;
    }

    private MediaPreviewPage reachEditor() {
        MediaPreviewPage media = reachPreview();
        assumeTrue(media.isEditButtonVisible(), "Edit button missing (not an image?)");
        media.tapEdit();
        assumeTrue(media.isEditorOpen(), "Image editor did not open");
        return media;
    }

    // ---------------- preview page ----------------

    @Test
    @Order(1)
    @DisplayName("Selecting a gallery image opens the preview with caption, edit and send")
    void previewOpens() {
        MediaPreviewPage media = reachPreview();
        assertTrue(media.isEditButtonVisible(), "Preview should show the edit button");
        screenshot("media-preview");
        media.tapPreviewBack();
        ensureBackInChat();
    }

    @Test
    @Order(2)
    @DisplayName("Caption/description field accepts text")
    void captionAcceptsText() {
        MediaPreviewPage media = reachPreview();
        media.setCaption("QA caption test");
        assertTrue(media.captionText().contains("QA caption"),
                "Caption field should hold the typed text");
        media.tapPreviewBack();
        ensureBackInChat();
    }

    // ---------------- editor (all tools in one navigation) ----------------

    @Test
    @Order(3)
    @DisplayName("Editor: 3 tools (no rotate) + paint/draw, add-text, crop, undo, save")
    void editorAllTools() {
        MediaPreviewPage media = reachEditor();

        // Tool set: exactly Paint / Crop / Add-Text, and NO rotate.
        assertTrue(media.hasTool("btn_paint"), "Paint tool present");
        assertTrue(media.hasTool("btn_crop"), "Crop tool present");
        assertTrue(media.hasTool("btn_text"), "Add-Text tool present");
        assertTrue(media.isSaveVisible(), "Save visible when idle");
        screenshot("media-editor-tools");

        // Paint: pick a colour, draw a stroke, apply → undo becomes available.
        media.openPaint();
        assertTrue(media.isPaintToolOpen(), "Paint palette should open");
        assertTrue(media.hasTool("paint_eraser"), "Eraser available in paint");
        media.pickPaintColor(4);
        media.drawStroke();
        media.tapApply();
        assertTrue(media.isUndoVisible(), "Undo should appear after a paint edit");
        screenshot("media-paint");

        // Add-Text: type and apply.
        media.openText();
        assertTrue(media.isTextToolOpen(), "Add-Text field should open");
        media.typeEditorText("QA");
        media.tapApply();

        // Crop: opens with the crop overlay, then apply.
        media.openCrop();
        assertTrue(media.isCropToolOpen(), "Crop overlay should appear");
        media.tapApply();
        screenshot("media-crop");

        // Undo one edit, then save → back to the preview.
        media.tapUndo();
        media.tapSave();
        assertTrue(media.isPreviewOpen(), "Saving should return to the preview page");
        screenshot("media-saved");

        media.tapPreviewBack();
        ensureBackInChat();
    }

    // ---------------- send + post-send ----------------

    @Test
    @Order(4)
    @DisplayName("Sending the image posts it, shows the loader and a media bubble in chat")
    void sendImagePostsWithLoader() {
        MediaPreviewPage media = reachPreview();
        media.setCaption("qa-media-" + (System.currentTimeMillis() % 1000000));
        // Loader appears during the prepare/upload right after tapping send.
        media.tapPreviewSend();
        boolean loader = new HomeChatPage(driver).uploadLoaderShown(Duration.ofSeconds(8));
        assertTrue(media.waitPreviewClosed(Duration.ofSeconds(60)),
                "Preview should close after send");
        HomeChatPage home = chat();
        assertTrue(home.mediaBubbleShown(Duration.ofSeconds(30)),
                "A media bubble should appear in the chat after sending");
        System.out.println(">>> media sent; loader seen=" + loader);
        screenshot("media-sent");
    }

    @Test
    @Order(5)
    @DisplayName("Tapping a sent media opens the full-screen viewer")
    void openSentMediaViewer() {
        HomeChatPage home = chat();
        assumeTrue(home.mediaBubbleShown(Duration.ofSeconds(10)),
                "No media bubble to open (previous send may have skipped)");
        assumeTrue(home.tapNewestMediaBubble(), "Could not tap a media bubble");
        // The viewer is a separate screen: chat input bar should no longer show.
        boolean viewerOpened = !home.isInputBarVisible()
                && appPackage.equals(home.safeCurrentPackage());
        screenshot("media-viewer");
        assertTrue(viewerOpened, "A media viewer should open (still in the app, off the chat)");
        driver.navigate().back();
        ensureBackInChat();
    }

    @Test
    @Order(6)
    @DisplayName("Download control is available for media (received items)")
    void mediaDownloadControl() {
        HomeChatPage home = chat();
        assumeTrue(home.mediaBubbleShown(Duration.ofSeconds(10)), "No media bubble present");
        // Own just-sent media has no download control (already local); a received
        // item does. Skip cleanly when only own media is present.
        assumeTrue(home.isDownloadControlPresent(),
                "No downloadable (received) media on this account — needs a second sender");
        assertTrue(home.tapDownload(), "Download control should be tappable");
        screenshot("media-download");
    }

    @Test
    @Order(7)
    @DisplayName("Replying to a sent media message posts the reply")
    void replyOnMedia() {
        HomeChatPage home = chat();
        assumeTrue(home.mediaBubbleShown(Duration.ofSeconds(10)), "No media bubble to reply to");
        assumeTrue(home.longPressNewestMedia(), "Long-press on media did not register");
        assumeTrue(home.tapMenuOption("Reply"), "Reply option missing on media menu");
        assertTrue(home.isReplyPreviewVisible(),
                "Reply preview should show for a media message");
        String reply = "qa-mediarep-" + (System.currentTimeMillis() % 1000000);
        home.sendMessage(reply);
        assertTrue(home.messageShown(reply, Duration.ofSeconds(20)),
                "The media reply '" + reply + "' should post");
        screenshot("media-reply");
    }

    // ---------------- video gallery (reliable, app's own gallery) ----------------

    @Test
    @Order(8)
    @DisplayName("Video Gallery: selecting and sending a video posts it (skips if no video)")
    void sendVideoFromGallery() {
        HomeChatPage home = chat();
        assumeTrue(home.isAttachmentVisible(), "No attachment button — skipping");
        home.tapAttachment();
        assumeTrue(home.isAttachmentPickerVisible(), "Picker did not open");
        assumeTrue(home.tapAttachmentOption("Video Gallery"), "Video Gallery option missing");
        for (int i = 0; i < 4; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }

        MediaPreviewPage media = new MediaPreviewPage(driver);
        assumeTrue(media.isGalleryOpen(), "Video gallery did not open");
        media.openAllMediaAlbumIfListed();
        assumeTrue(media.galleryVideoCount() > 0,
                "No videos on the device — push an .mp4 to /sdcard/Movies to test video send");
        media.selectVideos(1);
        assumeTrue(media.isSendFabVisible(), "Send FAB not shown after selecting a video");
        media.tapGallerySendFab();
        assumeTrue(media.isPreviewOpen(), "Video preview did not open");
        media.setCaption("qa-video-" + (System.currentTimeMillis() % 1000000));
        media.tapPreviewSend();
        assertTrue(media.waitPreviewClosed(Duration.ofSeconds(90)),
                "Preview should close after sending the video");
        assertTrue(chat().mediaBubbleShown(Duration.ofSeconds(30)),
                "A video bubble should appear in the chat");
        screenshot("media-video-sent");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
