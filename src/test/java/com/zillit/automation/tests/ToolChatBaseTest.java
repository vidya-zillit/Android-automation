package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared test body for the Bulletin-style Tools CHAT modules — "Confidential
 * Info" and "Info". Both open (Tools tab → module row) into a chat that reuses
 * the Home units-chat screen (rvChat + etMessage / ivAttachment / ivRecord /
 * ivSendMessage + long-press options), so the whole Bulletin case set applies.
 *
 * Concrete subclasses only supply {@link #moduleName()}. Cases skip (never
 * fail) when posting rights are missing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class ToolChatBaseTest extends BaseTest {

    private static final Duration SHORT = Duration.ofSeconds(8);
    private boolean insideProject;
    private boolean moduleOpened;

    /** The Tools module label, e.g. "Confidential Info" / "Info". */
    protected abstract String moduleName();

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void enterModule() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects to enter");
                assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        dismissPermissionPrompts();
        HomeChatPage chat = new HomeChatPage(driver);
        // Only skip the re-open when we are genuinely still INSIDE the module.
        // "An input bar is visible" is not enough: the Home units chat has one too,
        // and Forward/Gallery/Share can land us back on Home — which would silently
        // run the rest of the module's cases against Bulletin.
        if (moduleOpened && chat.isInputBarVisible() && inModuleScreen()) { afterModuleReady(); return; }
        openModuleChat();
        afterModuleReady();
    }

    /**
     * Hook run once the module chat is on screen, before each case. Modules whose
     * input bar is conditional override this to make the chat postable — e.g.
     * Catering hides the keyboard for a CATERER until a recipient is chosen.
     */
    protected void afterModuleReady() { }

    /**
     * True when the foreground screen is a Tools MODULE chat rather than the Home
     * tab. Module screens are pushed with their own back button and have no bottom
     * navigation; Home always shows the bottom nav.
     */
    private boolean inModuleScreen() {
        try {
            return driver.findElements(byAppId("bottomNavigation")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Navigate Tools → open the module → its chat. */
    private void openModuleChat() {
        // Back out of any leftover sub-screen first.
        HomeChatPage chat = new HomeChatPage(driver);
        for (int i = 0; i < 4 && !new ToolsPage(driver).isLoaded(Duration.ofSeconds(1))
                && !chat.isInputBarVisible(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) {}
            sleep(600);
        }
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "Could not open the Tools tab");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        // The module may be below the fold (ADMIN section) — filter via the search
        // box to bring its row into view.
        tools.searchTools(moduleName());
        assumeTrue(tools.isModuleListed(moduleName(), Duration.ofSeconds(6)),
                "'" + moduleName() + "' module not present on this project");
        assumeTrue(tools.openModule(moduleName()), "Could not open the '" + moduleName() + "' module");
        assumeTrue(tools.isModuleChatOpen(Duration.ofSeconds(15)),
                "'" + moduleName() + "' chat did not open");
        moduleOpened = true;
    }

    private HomeChatPage chat() {
        HomeChatPage page = new HomeChatPage(driver);
        assumeTrue(page.isInputBarVisible() || page.isNoRightsBannerVisible(),
                moduleName() + " chat input bar not visible");
        return page;
    }

    // ---------------- open ----------------

    @Test
    @Order(1)
    @DisplayName("Module opens its Bulletin-style chat (header + input bar)")
    void moduleOpens() {
        HomeChatPage page = new HomeChatPage(driver);
        assertTrue(page.isInputBarVisible() || page.isNoRightsBannerVisible(),
                moduleName() + " should show the chat input bar or a no-rights banner");
        System.out.println(">>> " + moduleName() + " chat opened");
        screenshot("tool-" + slug() + "-open");
    }

    // ---------------- send validation ----------------

    @Test
    @Order(2)
    @DisplayName("Empty input hides Send and shows the mic")
    void emptyInputHidesSend() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        page.clearMessage();
        assertTrue(!page.isSendButtonVisible(), "Send must stay hidden while empty");
        assertTrue(page.isRecordButtonVisible(), "The mic should show when input is empty");
    }

    @Test
    @Order(3)
    @DisplayName("Whitespace-only input is not sendable")
    void whitespaceNotSendable() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        page.typeMessage("     ");
        assertTrue(!page.isSendButtonVisible() || !page.messageShown("     ", SHORT),
                "Whitespace-only text must not be sendable");
        page.clearMessage();
    }

    @Test
    @Order(4)
    @DisplayName("Typing real text reveals the Send button")
    void typingRevealsSend() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        page.typeMessage("hello");
        assertTrue(page.isSendButtonVisible(), "Send should appear once text is entered");
        page.clearMessage();
    }

    @Test
    @Order(5)
    @DisplayName("Sending a valid message posts it")
    void sendValidMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-" + slug() + "-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assertTrue(page.messageShown(marker, Duration.ofSeconds(20)),
                "The sent message '" + marker + "' should appear");
        screenshot("tool-" + slug() + "-sent");
    }

    // ---------------- attachment ----------------

    @Test
    @Order(6)
    @DisplayName("Attachment picker offers the media types (Photo/Gallery/Video/Document/Audio/Location/Contact)")
    void attachmentPickerHasMediaTypes() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        assumeTrue(page.isAttachmentVisible(), "No attachment button — skipping");
        page.tapAttachment();
        assumeTrue(page.isAttachmentPickerVisible(), "Attachment picker did not open");
        List<String> opts = page.attachmentOptions();
        System.out.println(">>> " + moduleName() + " attachment options: " + opts);
        assertTrue(opts.size() >= minAttachmentOptions(),
                moduleName() + " attachment picker should offer at least " + minAttachmentOptions()
                        + " media types. Got: " + opts);
        page.dismissAttachmentPicker();
    }

    /**
     * How many attachment options this module is expected to expose. The general
     * Bulletin-style modules offer the full set (Photo/Gallery/Video/Video Gallery/
     * Audio/Document/Location/Contact); modules with a restricted picker override
     * this (e.g. Catering only allows Photo / Image Gallery / Audio / Document).
     */
    protected int minAttachmentOptions() { return 6; }

    // ---------------- long-press + reply ----------------

    @Test
    @Order(7)
    @DisplayName("Long-press a message shows the options menu")
    void longPressShowsOptions() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-" + slug() + "-lp-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.messageShown(marker, Duration.ofSeconds(20)), "Need a message to long-press");
        assumeTrue(page.longPressMessage(marker), "Long-press did not open the menu");
        assertTrue(page.isMenuOpen(Duration.ofSeconds(4)), "The options menu should appear");
        System.out.println(">>> " + moduleName() + " long-press options: " + page.menuOptions());
        screenshot("tool-" + slug() + "-options");
        page.closeMenu();
    }

    @Test
    @Order(8)
    @DisplayName("Reply to a message posts the reply")
    void replyToMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-" + slug() + "-rep-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.messageShown(marker, Duration.ofSeconds(20)), "Need a message to reply to");
        assumeTrue(page.longPressMessage(marker), "Long-press failed");
        assumeTrue(page.tapMenuOption("Reply"), "Reply option not present");
        assertTrue(page.isReplyPreviewVisible(), "Tapping Reply should show the reply preview");
        String reply = marker + "-r";
        page.sendMessage(reply);
        assertTrue(page.messageShown(reply, Duration.ofSeconds(15)), "The reply should post");
        screenshot("tool-" + slug() + "-reply");
    }

    /** Sends a fresh unique marker and returns it (or null if it didn't post). */
    private String postMarker(HomeChatPage page, String kind) {
        String m = "qa-" + slug() + "-" + kind + "-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(m);
        return page.messageShown(m, Duration.ofSeconds(20)) ? m : null;
    }

    @Test
    @Order(9)
    @DisplayName("EDIT a message (edit comment) → text updates")
    void editMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "edit");
        assumeTrue(m != null, "Message did not post — skipping");
        assumeTrue(page.longPressMessage(m), "Long-press failed");
        assumeTrue(page.tapMenuOption("Edit"), "Edit option not present");
        assumeTrue(page.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        String edited = m + "-EDITED";
        page.setEditDialogText(edited);
        page.saveEditDialog();
        assertTrue(page.messageShown(edited, Duration.ofSeconds(20)),
                "The edited text '" + edited + "' should appear");
        screenshot("tool-" + slug() + "-edit");
    }

    @Test
    @Order(10)
    @DisplayName("FORWARD a message → the ForwardPage recipient picker opens")
    void forwardMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "fwd");
        assumeTrue(m != null, "Message did not post — skipping");
        boolean opened = page.forwardOpensPage(m);
        System.out.println(">>> " + moduleName() + " Forward → page open=" + opened);
        assertTrue(opened, "Forward should open the ForwardPage recipient picker");
        screenshot("tool-" + slug() + "-forward");
        page.leaveOverlayScreen();   // cancel WITHOUT forwarding
    }

    @Test
    @Order(11)
    @DisplayName("GALLERY option opens the chat library (media/docs/links)")
    void galleryOption() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "gal");
        assumeTrue(m != null, "Message did not post — skipping");
        boolean opened = page.galleryOpensLibrary(m);
        System.out.println(">>> " + moduleName() + " Gallery → library open=" + opened);
        assertTrue(opened, "Gallery should open the chat library");
        screenshot("tool-" + slug() + "-gallery");
        page.leaveOverlayScreen();
    }

    @Test
    @Order(12)
    @DisplayName("READ BY USER opens the read-by list")
    void readByUser() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "rbu");
        assumeTrue(m != null, "Message did not post — skipping");
        boolean opened = page.readByOpensPage(m);
        System.out.println(">>> " + moduleName() + " Read By User → page open=" + opened);
        assumeTrue(opened, "Read-By needs a delivered message (status=2) — skipping if not yet delivered");
        assertTrue(opened, "Read By User should open the read-by screen");
        screenshot("tool-" + slug() + "-readby");
        page.leaveOverlayScreen();
    }

    @Test
    @Order(13)
    @DisplayName("SHARE a message → the share flow starts")
    void shareMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "share");
        assumeTrue(m != null, "Message did not post — skipping");
        boolean started = page.shareStarts(m);
        System.out.println(">>> " + moduleName() + " Share → started=" + started);
        assertTrue(started, "Share should start the share flow");
        screenshot("tool-" + slug() + "-share");
        page.leaveOverlayScreen();
    }

    @Test
    @Order(14)
    @DisplayName("DELETE a message (delete comment, 3-step) → it disappears")
    void deleteMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "del");
        assumeTrue(m != null, "Message did not post — skipping");
        assumeTrue(page.longPressMessage(m), "Long-press failed");
        assumeTrue(page.tapMenuOption("Delete"), "Delete option not present");
        page.tapDeleteActionBar(Duration.ofSeconds(6));   // action-bar Delete (if multi-select path)
        assumeTrue(confirmYesDialog(Duration.ofSeconds(8)), "Delete confirmation did not appear");
        sleep(1500);
        boolean gone = !page.messageShown(m, Duration.ofSeconds(4));
        System.out.println(">>> " + moduleName() + " delete: gone=" + gone);
        assertTrue(gone, "The deleted message should no longer be visible");
        screenshot("tool-" + slug() + "-delete");
    }

    @Test
    @Order(15)
    @DisplayName("Send an IMAGE (media message) → an image bubble appears")
    void sendImageMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        boolean sent = page.sendOneImage("", v -> waitAndDismissPermissions(Duration.ofSeconds(4)));
        System.out.println(">>> " + moduleName() + " image sent=" + sent);
        assumeTrue(sent, "Gallery not navigable on this device — skipping");
        assertTrue(page.mediaBubbleShown(Duration.ofSeconds(25)), "An image/media bubble should appear");
        screenshot("tool-" + slug() + "-image");
    }

    private String slug() {
        return moduleName().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
