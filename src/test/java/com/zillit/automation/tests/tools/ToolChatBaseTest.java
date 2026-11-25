package com.zillit.automation.tests.tools;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
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
        // A confirmation left open by the previous case is MODAL: it swallows every tap
        // and back-press, so the next case cannot even reach the chat and the whole
        // remainder of the suite skips. Clear it before anything else.
        clearBlockingPopup();
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
        // A case (or an earlier Back) can leave the app in the background: bring it forward.
        try {
            if (!appPackage.equals(driver.getCurrentPackage())) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(2000);
            }
        } catch (Exception ignored) { }
        // Stop as soon as the bottom navigation is up: any bottom-nav screen (Home with the
        // Calendar unit selected has no composer) can reach Tools by its tab, and one more
        // Back from there leaves the app for the launcher.
        for (int i = 0; i < 4 && !new ToolsPage(driver).isLoaded(Duration.ofSeconds(1))
                && !chat.isInputBarVisible()
                && driver.findElements(byAppId("bottomNavigation")).isEmpty(); i++) {
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
        // NOT messageShown(): the bubble is inserted locally the instant Send is tapped,
        // so "the text is on screen" is true even when the message never left the device.
        // waitMessageSent() requires the row to lose BOTH the pending clock and the Retry
        // control — i.e. the server actually took it.
        assertTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "The message '" + marker + "' should reach the server — it is "
                        + page.sendStateOf(marker));
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
     * Audio/Document/Location/Contact). Catering overrides the whole case instead,
     * because its picker is an exact set (Photo / Image Gallery / Audio / Document).
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
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Need a SENT message to long-press — it is " + page.sendStateOf(marker));
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
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Need a SENT message to reply to — it is " + page.sendStateOf(marker));
        assumeTrue(page.longPressMessage(marker), "Long-press failed");
        assumeTrue(page.tapMenuOption("Reply"), "Reply option not present");
        assertTrue(page.isReplyPreviewVisible(), "Tapping Reply should show the reply preview");
        String reply = marker + "-r";
        page.sendMessage(reply);
        assertTrue(page.waitMessageSent(reply, Duration.ofSeconds(30)),
                "The reply should reach the server — it is " + page.sendStateOf(reply));
        screenshot("tool-" + slug() + "-reply");
    }

    /**
     * Sends a fresh unique marker and returns it, or null if it did not actually SEND.
     *
     * <p>Deliberately strict: the later cases (edit / forward / delete) all operate on
     * this message, and acting on one that is still pending tests nothing — the app
     * refuses most operations on an unsent message, which then surfaces as a confusing
     * failure three steps away from the real cause.
     */
    private String postMarker(HomeChatPage page, String kind) {
        String m = "qa-" + slug() + "-" + kind + "-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(m);
        return page.waitMessageSent(m, Duration.ofSeconds(30)) ? m : null;
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
        assertTrue(page.waitMessageSent(edited, Duration.ofSeconds(30)),
                "The edited text '" + edited + "' should reach the server — it is "
                        + page.sendStateOf(edited));
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

    /**
     * Info / Confidential Info (ConfidentialInfo.kt): {@code showReadBy = status == 2}. A
     * message that {@link #postMarker} returned has lost its clock and Retry, i.e. the
     * server acknowledged it, which is exactly when the app sets status 2. So a missing
     * option or a screen that does not open is a product failure, not a precondition.
     * Catering hard-codes Read By off and overrides this case.
     */
    @Test
    @Order(12)
    @DisplayName("READ BY USER is offered on a sent message and opens the read-by list")
    void readByUser() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "rbu");
        assumeTrue(m != null, "Message did not post — skipping");
        assumeTrue(page.longPressMessage(m), "Long-press did not open the menu");
        List<String> opts = page.menuOptions();
        System.out.println(">>> " + moduleName() + " options on a sent message: " + opts);
        boolean offered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Read By User"));
        if (!offered) page.closeMenu();
        assertTrue(offered, "A sent message must offer 'Read By User' in " + moduleName()
                + " (showReadBy = status == 2). Options were: " + opts);
        assertTrue(page.tapMenuOption("Read By User"), "'Read By User' was listed but could not be tapped");
        boolean opened = readByScreenOpen(Duration.ofSeconds(10));
        System.out.println(">>> " + moduleName() + " Read By User → page open=" + opened);
        screenshot("tool-" + slug() + "-readby");
        page.leaveOverlayScreen();
        assertTrue(opened, "Read By User should open the read-by screen (ReadByUserPage / readByRv)");
    }

    /**
     * The read-by screen itself, recognised by structure only. HomeChatPage.readByOpensPage
     * also accepts any text containing "Read By" or "Delivered", which can match chat
     * content and so cannot tell a screen that opened from one that did not.
     */
    private boolean readByScreenOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String a = driver.currentActivity();
                if (a != null && a.contains("ReadByUser")) return true;
                if (!driver.findElements(byAppId("readByRv")).isEmpty()) return true;
            } catch (Exception ignored) { }
            sleep(500);
        }
        return false;
    }

    /**
     * Share in these modules (ConfidentialInfo.kt / CateringActivity.kt setShareAction) does
     * NOT open anything straight away: it enters selection mode with an action bar whose
     * {@code tvAction} reads "Share", and only tapping that opens the Android share sheet
     * ({@code openShareMultipleIntent}). Both steps are asserted.
     *
     * <p>HomeChatPage.shareStarts() is not used: its fallback {@code !currentActivityContains("home")}
     * is always true inside a Tools module (the activity is ConfidentialInfo / CateringActivity),
     * so it could never fail here.
     */
    @Test
    @Order(13)
    @DisplayName("SHARE a message → the Share action bar appears, and it opens the Android share sheet")
    void shareMessage() {
        HomeChatPage page = chat();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String m = postMarker(page, "share");
        assumeTrue(m != null, "Message did not post — skipping");
        assumeTrue(page.longPressMessage(m), "Long-press did not open the menu");
        List<String> opts = page.menuOptions();
        boolean offered = opts.stream().anyMatch(o -> o.equalsIgnoreCase("Share"));
        if (!offered) page.closeMenu();
        assertTrue(offered, "A sent own message must offer 'Share' in " + moduleName()
                + ". Options were: " + opts);
        assertTrue(page.tapMenuOption("Share"), "'Share' was listed but could not be tapped");

        boolean bar = shareActionBarShown(Duration.ofSeconds(8));
        System.out.println(">>> " + moduleName() + " Share → action bar shown=" + bar);
        screenshot("tool-" + slug() + "-share");
        if (!bar) page.leaveOverlayScreen();
        assertTrue(bar, "Tapping Share should enter selection mode with a 'Share' action bar (tvAction)");

        String before = page.safeCurrentPackage();
        driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*:id/tvAction\").textMatches(\"(?i)share\")")).click();
        String sheetPkg = waitForForeignPackage(Duration.ofSeconds(12));
        System.out.println(">>> " + moduleName() + " Share bar tapped: " + before + " → " + sheetPkg);
        screenshot("tool-" + slug() + "-share-sheet");
        // Close the share sheet without sharing, then leave selection mode.
        if (sheetPkg != null) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        page.leaveOverlayScreen();
        assertTrue(sheetPkg != null,
                "Tapping the 'Share' action bar should open the Android share sheet, but the "
                        + "foreground stayed in " + appPackage);
    }

    private boolean shareActionBarShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/tvAction\").textMatches(\"(?i)share\")"))
                        .isEmpty()) return true;
            } catch (Exception ignored) { }
            sleep(400);
        }
        return false;
    }

    /** The package that took the foreground (the share sheet), or null if the app kept it. */
    private String waitForForeignPackage(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        HomeChatPage page = new HomeChatPage(driver);
        while (System.currentTimeMillis() < deadline) {
            String pkg = page.safeCurrentPackage();
            if (pkg != null && !pkg.equals(appPackage)) return pkg;
            sleep(500);
        }
        return null;
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

    // =====================================================================================
    // WEB-GAP CASES (2026-11-23): the shared unit-feed.suite.js groups from
    // web-e2e/gaps/info.md and gaps/confidential-info.md. Written against ConfidentialInfo.kt,
    // the one activity behind both Info and Confidential Info.
    //
    // Gated by infoFeedCases(): Info and Confidential Info opt in. Catering
    // (CateringActivity) has a different composer and menu, so it inherits these only once
    // its suite overrides the hook — nothing here runs for it by default.
    // =====================================================================================

    /**
     * Whether this module is a ConfidentialInfo.kt feed, for which the web-gap cases below
     * were written. Default false so a module that inherits this base without opting in
     * (Catering) is not run against assumptions that were never checked for it.
     */
    protected boolean infoFeedCases() { return false; }

    /** Markers posted by the web-gap cases, deleted by the clean-up case at the end. */
    private final java.util.List<String> autoPosted = new java.util.ArrayList<>();

    private com.zillit.automation.pages.tools.InfoFeedPage feed() {
        assumeTrue(infoFeedCases(), moduleName()
                + " has not opted in to the Info-feed cases (infoFeedCases() is false)");
        com.zillit.automation.pages.tools.InfoFeedPage p =
                new com.zillit.automation.pages.tools.InfoFeedPage(driver);
        assumeTrue(p.isInputBarVisible() || p.isNoRightsBannerVisible(),
                moduleName() + " chat input bar not visible");
        return p;
    }

    /** feed() plus posting rights, which every case that sends needs. */
    private com.zillit.automation.pages.tools.InfoFeedPage postingFeed() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        assumeTrue(p.isInputBarVisible(),
                "This account has no posting rights in " + moduleName() + " — skipping");
        return p;
    }

    private String autoMarker(String kind) {
        return "ZL_AUTO-" + slug() + "-" + kind + "-" + (System.currentTimeMillis() % 1000000);
    }

    /** Posts a ZL_AUTO marker and returns it once the server has it; null otherwise. */
    private String postAuto(HomeChatPage page, String kind) {
        String m = autoMarker(kind);
        page.sendMessage(m);
        autoPosted.add(m);
        return page.waitMessageSent(m, Duration.ofSeconds(30)) ? m : null;
    }

    /** Back to Tools and into the module again: a real leave-and-reopen. */
    private void reopenModule() {
        com.zillit.automation.pages.tools.InfoFeedPage p =
                new com.zillit.automation.pages.tools.InfoFeedPage(driver);
        p.hideKeyboardSafely();
        for (int i = 0; i < 3 && inModuleScreen(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        openModuleChat();
        sleep(1500);
    }

    /** Leaves search or selection mode if a case left either open. */
    private void leaveOptionBar(com.zillit.automation.pages.tools.InfoFeedPage p) {
        if (p.isSearchOpen() || p.isSelectionMode()) p.tapOptionBack();
    }

    private static String repeatWords(int length) {
        StringBuilder sb = new StringBuilder();
        String[] words = {"alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf"};
        int i = 0;
        while (sb.length() < length) {
            sb.append(words[i++ % words.length]).append(' ');
        }
        return sb.substring(0, length);
    }

    // ---------------- G-HEADER / G-RIGHTS ----------------

    // [WEB INFO info.spec.js:50] [WEB CI confidential-info.spec.js:53]
    @Test
    @Order(100)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-HEADER] The toolbar title is the module name")
    void headerShowsModuleName() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        String header = p.toolbarHeader();
        System.out.println(">>> " + moduleName() + " toolbar header: " + header);
        assertTrue(header != null && header.trim().equalsIgnoreCase(moduleName()),
                "The toolbar title should read '" + moduleName() + "', was '" + header + "'");
    }

    // [WEB G-RIGHTS] invariant behind the view-only REQUEST bar (CI.kt handlePostingRights)
    @Test
    @Order(101)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-RIGHTS] Exactly one of composer or REQUEST bar is shown")
    void composerXorRequestBar() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        boolean composer = p.shownNow("layPostMsg") || p.shownNow("etMessage");
        boolean request = p.shownNow("layNoMsg");
        System.out.println(">>> " + moduleName() + " composer=" + composer + " requestBar=" + request);
        assertTrue(composer ^ request, moduleName() + " must show the composer OR the no-rights "
                + "REQUEST bar, never both or neither. composer=" + composer + " requestBar=" + request);
    }

    // [WEB INFO info.spec.js:86/:241] [WEB CI confidential-info.spec.js:106/:619]
    @Test
    @Order(102)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-RIGHTS] A view-only member sees the REQUEST bar instead of the composer")
    void viewOnlyShowsRequestBar() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        // Both project members (Vidya Testing, Vidya Pixel) are admins with posting rights.
        // Making one view-only means changing a real user's rights, which this suite does
        // not do; the case runs for real when the device's account is view-only here.
        assumeTrue(!p.shownNow("etMessage"), "This account has posting rights in " + moduleName()
                + "; the REQUEST bar needs a view-only account (real users' rights are not changed)");
        assertTrue(p.isNoRightsBannerVisible(), "A view-only member should see the no-rights bar (layNoMsg)");
        assertTrue(p.isRequestRightsButtonVisible(), "The no-rights bar should offer REQUEST (tvRequest)");
        String label = p.textOf("tvRequest");
        assertTrue(label != null && label.trim().equalsIgnoreCase("REQUEST"),
                "The request button should read REQUEST, was '" + label + "'");
        screenshot("tool-" + slug() + "-request-bar");
    }

    // ---------------- G-FAB ----------------

    // [WEB unit-feed.suite.js:187 / :1596]
    @Test
    @Order(103)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-FAB] Feed sits on the newest row: History shown, scroll-to-bottom hidden")
    void feedOpensAtNewest() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "fab");
        assumeTrue(m != null, "Marker did not post — skipping");
        p.hideKeyboardSafely();
        sleep(800);
        assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "The newest message should be on screen");
        assertTrue(p.visible("userHistoryBtn", Duration.ofSeconds(3)), "The History button should show");
        assertTrue(!p.isArrowDownShown(),
                "The scroll-to-bottom card should be hidden while the newest row is in view");
    }

    // [WEB unit-feed.suite.js:1623]
    @Test
    @Order(104)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-FAB] Scrolling up shows the scroll-to-bottom card; tapping it returns to the newest row")
    void scrollToBottomCard() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "bottom");
        assumeTrue(m != null, "Marker did not post — skipping");
        p.hideKeyboardSafely();
        p.scrollOlder(4);
        boolean shown = p.visible("arrowDownCard", Duration.ofSeconds(3));
        assumeTrue(shown || p.messageShown(m, Duration.ofSeconds(1)) == false,
                "The feed is shorter than one screen, so it cannot scroll away from the newest row");
        assertTrue(shown, "Scrolled away from the newest row, the scroll-to-bottom card should appear");
        p.tap("arrowDownCard");
        sleep(1500);
        assertTrue(p.messageShown(m, Duration.ofSeconds(4)), "Tapping the card should bring back the newest row");
        sleep(800);
        assertTrue(!p.isArrowDownShown(), "The card should hide again at the bottom");
    }

    // ---------------- G-SEARCH ----------------

    // [WEB unit-feed.suite.js:1471] [WEB INFO-09]
    @Test
    @Order(110)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SEARCH] Search finds a posted marker and counts 1 match; composer hidden meanwhile")
    void searchFindsMarker() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "find");
        assumeTrue(m != null, "Marker did not post — skipping");
        try {
            assertTrue(p.openSearch(), "The composer's search icon should open the search bar (laySearch)");
            assertTrue(!p.shownNow("etMessage"), "The composer should be hidden while searching");
            p.search(m);
            String count = p.waitCount(Duration.ofSeconds(6));
            System.out.println(">>> " + moduleName() + " search '" + m + "' count=" + count);
            assertTrue("1".equals(count), "Searching a unique (hyphenated) marker should count 1 match, was '" + count + "'");
            assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "The match should be scrolled into view");
        } finally {
            leaveOptionBar(p);
        }
    }

    // [WEB unit-feed.suite.js:1493]
    @Test
    @Order(111)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SEARCH] Searching for nothing says 'Please type text to search.'")
    void emptySearchRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        try {
            assertTrue(p.openSearch(), "Search bar did not open");
            p.setSearchText("");
            p.submitSearch();
            assertTrue(p.messageAppears(com.zillit.automation.pages.tools.InfoFeedPage.EMPTY_SEARCH, Duration.ofSeconds(4)),
                    "An empty search should show '" + com.zillit.automation.pages.tools.InfoFeedPage.EMPTY_SEARCH + "'");
        } finally {
            leaveOptionBar(p);
        }
    }

    // [WEB unit-feed.suite.js:1493]
    @Test
    @Order(112)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SEARCH] No-match search counts 0; regex characters do not crash it")
    void noMatchAndSpecialCharsSearch() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        try {
            assertTrue(p.openSearch(), "Search bar did not open");
            String none = "zzqq-nomatch-" + (System.currentTimeMillis() % 1000000);
            p.search(none);
            String c0 = p.waitCount(Duration.ofSeconds(6));
            assertTrue("0".equals(c0), "A search with no match should count 0, was '" + c0 + "'");
            p.search(".*[(");
            String c1 = p.waitCount(Duration.ofSeconds(6));
            System.out.println(">>> " + moduleName() + " search '.*[(' count=" + c1);
            assertTrue(appPackage.equals(p.safeCurrentPackage()), "The app should survive a regex-like query");
            assertTrue(c1.matches("\\d+"), "The count should still be a number, was '" + c1 + "'");
        } finally {
            leaveOptionBar(p);
        }
    }

    // [WEB sheet-bugs INFO-20 / CONF-18] arrows after a changed word must search again
    @Test
    @Order(113)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SEARCH] INFO-20/CONF-18: after changing the word, the arrow searches the new word")
    void changedWordReSearches() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String a = postAuto(p, "wordA");
        String bStem = autoMarker("wordB");
        p.sendMessage(bStem + "-1");
        autoPosted.add(bStem + "-1");
        p.sendMessage(bStem + "-2");
        autoPosted.add(bStem + "-2");
        assumeTrue(a != null && p.waitMessageSent(bStem + "-2", Duration.ofSeconds(30)),
                "Markers did not post — skipping");
        try {
            assertTrue(p.openSearch(), "Search bar did not open");
            p.search(a);
            assertTrue("1".equals(p.waitCount(Duration.ofSeconds(6))), "First search should count 1");
            p.setSearchText(bStem);          // changed word, NOT submitted
            // No keyboard dismissal here: a BACK that reaches the screen closes the search bar.
            assertTrue(p.isSearchOpen(), "The search bar closed while editing the word");
            p.tapSearchUp();
            String c = p.waitCount(Duration.ofSeconds(6));
            System.out.println(">>> " + moduleName() + " changed word + arrow → count=" + c
                    + " searchOpen=" + p.isSearchOpen());
            assertTrue("2".equals(c), "INFO-20/CONF-18: tapping the arrow after changing the word should "
                    + "search the new word (2 matches), but the count is '" + c + "'");
        } finally {
            leaveOptionBar(p);
        }
    }

    // [WEB sheet-bugs INFO-12 / CONF-12]
    @Test
    @Order(114)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SEARCH] Closing search restores the composer and clears the count")
    void closeSearchRestoresFeed() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "close");
        assumeTrue(m != null, "Marker did not post — skipping");
        assertTrue(p.openSearch(), "Search bar did not open");
        p.search(m);
        p.waitCount(Duration.ofSeconds(6));
        assertTrue(p.tapOptionBack(), "The search bar's back button should be tappable");
        assertTrue(!p.isSearchOpen(), "Back should close the search bar");
        assertTrue(p.isInputBarVisible(), "The composer should be back after closing search");
        assertTrue(m.equals(p.messageTextContaining(m)),
                "The searched row should read exactly as before, was '" + p.messageTextContaining(m) + "'");
    }

    // [WEB unit-feed.suite.js:1520]
    @Test
    @Order(115)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SEARCH] Searching your own name lists your own rows")
    void searchByOwnName() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "name");
        assumeTrue(m != null, "Marker did not post — skipping");
        String who = p.rowUserName(m);
        assumeTrue(who != null && !who.isBlank(), "Could not read the sender name on the own row");
        String name = who.split("[(·|,\\n]")[0].trim();
        try {
            assertTrue(p.openSearch(), "Search bar did not open");
            p.search(name);
            String c = p.waitCount(Duration.ofSeconds(6));
            System.out.println(">>> " + moduleName() + " search own name '" + name + "' count=" + c);
            assertTrue(c.matches("\\d+") && Integer.parseInt(c) >= 1,
                    "Searching own name '" + name + "' should find own rows, count was '" + c + "'");
        } finally {
            leaveOptionBar(p);
        }
    }

    // ---------------- G-HISTORY ----------------

    // [WEB unit-feed.suite.js:1661] [WEB CI confidential-info.spec.js:69]
    @Test
    @Order(120)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-HISTORY] History opens titled with the module and 'Records of deleted messages'; Back returns")
    void historyOpensAndReturns() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        assertTrue(p.openHistory(), "The History button should open the deleted-messages history");
        try {
            String header = p.toolbarHeader();
            String msg = p.historyHeaderMessage();
            System.out.println(">>> " + moduleName() + " History header='" + header + "' strip='" + msg + "'");
            screenshot("tool-" + slug() + "-history");
            // The toolbar is all-caps and the app appends "History": "INFO HISTORY".
            assertTrue(header != null && header.trim().equalsIgnoreCase(moduleName() + " History"),
                    "History should be titled '" + moduleName() + " History', was '" + header + "'");
            assertTrue(com.zillit.automation.pages.tools.InfoFeedPage.HISTORY_HEADER.equals(msg == null ? null : msg.trim()),
                    "History should say '" + com.zillit.automation.pages.tools.InfoFeedPage.HISTORY_HEADER + "', was '" + msg + "'");
        } finally {
            driver.navigate().back();
            sleep(1200);
        }
        assertTrue(p.isInputBarVisible() || p.isNoRightsBannerVisible(), "Back should return to the feed");
        assertTrue(inModuleScreen(), "Back should land on the module feed, not Home");
    }

    // [WEB unit-feed.suite.js:1681] [CONF-11 to verify]
    @Test
    @Order(121)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-HISTORY] A deleted message is listed in History")
    void deletedMessageInHistory() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String d = postAuto(p, "hist");
        assumeTrue(d != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(d), "Long-press failed");
        assumeTrue(p.tapMenuOption("Delete"), "Delete option not present");
        p.tapDeleteActionBar(Duration.ofSeconds(6));
        assumeTrue(confirmYesDialog(Duration.ofSeconds(8)), "Delete confirmation did not appear");
        sleep(2000);
        autoPosted.remove(d);
        assumeTrue(!p.messageShown(d, Duration.ofSeconds(3)), "The message was not deleted — skipping");
        assertTrue(p.openHistory(), "History did not open");
        try {
            boolean listed = p.historyShows(d, 4);
            screenshot("tool-" + slug() + "-history-deleted");
            assertTrue(listed, "The deleted message '" + d + "' should be listed in History");
        } finally {
            driver.navigate().back();
            sleep(1200);
        }
    }

    // [WEB sheet-bugs INFO-22 / CONF-20] History has no search on Android
    @Test
    @Order(122)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-HISTORY] INFO-22/CONF-20: History offers a search")
    void historyHasSearch() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        assertTrue(p.openHistory(), "History did not open");
        boolean reachable;
        try {
            sleep(1500);
            reachable = p.historySearchReachable();
        } finally {
            driver.navigate().back();
            sleep(1200);
        }
        assertTrue(reachable, "INFO-22/CONF-20: " + moduleName() + " History should offer a search; "
                + "UserChatHistoryPage shows its search icon only for Script Notes tools");
    }

    // ---------------- G-SELECT / G-DELETE ----------------

    // [WEB unit-feed.suite.js:833]
    @Test
    @Order(130)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-SELECT] Delete enters selection mode: count 1, then 2; Back leaves it with both kept")
    void selectionModeCountsAndBacksOut() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String a = postAuto(p, "selA");
        String b = postAuto(p, "selB");
        assumeTrue(a != null && b != null, "Markers did not post — skipping");
        assumeTrue(p.longPressMessage(a), "Long-press failed");
        assumeTrue(p.tapMenuOption("Delete"), "Delete option not present");
        try {
            assertTrue(p.waitSelectionMode(Duration.ofSeconds(6)), "Delete should enter selection mode (layAction)");
            assertTrue("1".equals(p.countText()), "Selection should count 1, was '" + p.countText() + "'");
            assertTrue(!p.shownNow("etMessage"), "The composer should be hidden in selection mode");
            assertTrue(p.tapMessage(b), "Could not tap the second message");
            assertTrue("2".equals(p.countText()), "Selecting a second message should count 2, was '" + p.countText() + "'");
        } finally {
            p.tapOptionBack();
        }
        assertTrue(!p.isSelectionMode(), "Back should leave selection mode");
        assertTrue(p.isInputBarVisible(), "The composer should be back");
        assertTrue(p.messageShown(a, Duration.ofSeconds(3)) && p.messageShown(b, Duration.ofSeconds(3)),
                "Backing out must keep both messages");
    }

    // [WEB unit-feed.suite.js:1000]
    @Test
    @Order(131)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-DELETE] Answering No at the delete dialog keeps the message")
    void deleteBackOutAtDialog() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "delNo");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Delete"), "Delete option not present");
        assertTrue(p.tapDeleteActionBar(Duration.ofSeconds(6)), "The selection bar's Delete should be tappable");
        assertTrue(confirmPopupShown(Duration.ofSeconds(6)), "The 'Are you sure you want to delete?' dialog should show");
        assertTrue(confirmPopupCancel(Duration.ofSeconds(4)), "Could not answer No");
        sleep(1200);
        assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "Answering No must keep the message");
        assertTrue(!p.isSelectionMode(), "No should also leave selection mode");
        assertTrue(p.isInputBarVisible(), "The composer should be back after No");
    }

    // [WEB unit-feed.suite.js:1016 / :1033]
    @Test
    @Order(132)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-DELETE] Two selected messages are deleted together and stay gone after a reopen")
    void multiDeleteGoneAfterReopen() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String a = postAuto(p, "mdA");
        String b = postAuto(p, "mdB");
        assumeTrue(a != null && b != null, "Markers did not post — skipping");
        assumeTrue(p.longPressMessage(a), "Long-press failed");
        assumeTrue(p.tapMenuOption("Delete"), "Delete option not present");
        assertTrue(p.waitSelectionMode(Duration.ofSeconds(6)), "Selection mode did not start");
        assertTrue(p.tapMessage(b), "Could not select the second message");
        assertTrue("2".equals(p.countText()), "Two messages should be selected, count was '" + p.countText() + "'");
        assertTrue(p.tapDeleteActionBar(Duration.ofSeconds(6)), "The selection bar's Delete should be tappable");
        assertTrue(confirmYesDialog(Duration.ofSeconds(8)), "Delete confirmation did not appear");
        sleep(2500);
        autoPosted.remove(a);
        autoPosted.remove(b);
        assertTrue(!p.messageShown(a, Duration.ofSeconds(2)) && !p.messageShown(b, Duration.ofSeconds(2)),
                "Both selected messages should be deleted");
        reopenModule();
        assertTrue(!p.messageShown(a, Duration.ofSeconds(4)) && !p.messageShown(b, Duration.ofSeconds(2)),
                "Deleted messages must stay gone after leaving and reopening " + moduleName());
    }

    // ---------------- G-EDIT ----------------

    // [WEB unit-feed.suite.js:859] the existing editMessage, plus the "Edited" tag
    @Test
    @Order(140)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-EDIT] An edit is prefilled, and the edited row is marked 'Edited'")
    void editShowsEditedTag() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "edtag");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Edit"), "Edit option not present");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        String prefilled = p.textOf("editTextInput");
        assertTrue(m.equals(prefilled == null ? null : prefilled.trim()),
                "The edit field should be prefilled with the message, was '" + prefilled + "'");
        String edited = m + "-E";
        p.setEditDialogText(edited);
        p.saveEditDialog();
        autoPosted.add(edited);
        assertTrue(p.waitMessageSent(edited, Duration.ofSeconds(30)), "The edit did not reach the server");
        String time = p.rowTime(edited);
        System.out.println(">>> " + moduleName() + " edited row time: " + time);
        assertTrue(time != null && time.trim().startsWith("Edited"),
                "An edited row should show 'Edited' before its time, was '" + time + "'");
    }

    // [WEB unit-feed.suite.js:884]
    @Test
    @Order(141)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-EDIT] Cancel leaves the message unchanged and not 'Edited'")
    void editCancelKeepsText() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "edcan");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Edit"), "Edit option not present");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        p.setEditDialogText(m + "-NOT-SAVED");
        p.cancelEditDialog();
        sleep(1500);
        assertTrue(!p.visible("editTextInput", Duration.ofSeconds(1)), "Cancel should close the dialog");
        assertTrue(m.equals(p.messageTextContaining(m)), "Cancel must keep the original text, row reads '"
                + p.messageTextContaining(m) + "'");
        String time = p.rowTime(m);
        assertTrue(time != null && !time.contains("Edited"), "A cancelled edit must not mark 'Edited', was '" + time + "'");
    }

    // [WEB unit-feed.suite.js:903]
    @Test
    @Order(142)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-EDIT] An empty edit is refused and the dialog stays open")
    void emptyEditRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "edempty");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Edit"), "Edit option not present");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        try {
            p.setEditDialogText("");
            p.saveEditDialog();
            sleep(1200);
            assertTrue(p.visible("editTextInput", Duration.ofSeconds(2)),
                    "Update with an empty text must keep the Edit dialog open ('Message could not be empty')");
        } finally {
            if (p.visible("editTextInput", Duration.ofSeconds(1))) p.cancelEditDialog();
            sleep(800);
        }
        assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "The message must be unchanged");
    }

    // [WEB unit-feed.suite.js:903] over-limit edit (Android limit 2,000)
    @Test
    @Order(143)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-EDIT] An edit over 2,000 characters is refused with the limit message")
    void overLimitEditRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "edlong");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Edit"), "Edit option not present");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        boolean stillOpen;
        try {
            p.setEditDialogText(m + " " + repeatWords(com.zillit.automation.pages.tools.InfoFeedPage.TEXT_LIMIT));
            p.saveEditDialog();
            // The limit snackbar is drawn on the activity BEHIND the (non-cancelable) dialog,
            // where UiAutomator cannot see it; the observable refusal is that Update does
            // not close the dialog (a valid Update dismisses it at once).
            boolean snack = p.messageAppears("Characters limit allowed to send", Duration.ofSeconds(2));
            System.out.println(">>> " + moduleName() + " over-limit edit: snackbar visible=" + snack);
            stillOpen = p.visible("editTextInput", Duration.ofSeconds(2));
        } finally {
            if (p.visible("editTextInput", Duration.ofSeconds(1))) p.cancelEditDialog();
            sleep(800);
        }
        assertTrue(stillOpen, "Update with over 2,000 characters must be refused ('"
                + com.zillit.automation.pages.tools.InfoFeedPage.LIMIT_ERROR + "') and keep the dialog open");
        assertTrue(m.equals(p.messageTextContaining(m)), "The refused edit must leave the text unchanged");
    }

    // [WEB sheet-bugs INFO-19 / CONF-17]
    @Test
    @Order(144)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-EDIT] INFO-19/CONF-17: Update with unchanged text does not mark 'Edited'")
    void unchangedUpdateNotEdited() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "edsame");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Edit"), "Edit option not present");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
        p.saveEditDialog();
        sleep(3000);
        String time = p.rowTime(m);
        System.out.println(">>> " + moduleName() + " unchanged update → row time: " + time);
        assertTrue(time != null && !time.contains("Edited"),
                "INFO-19/CONF-17: an unchanged Update must not mark the message 'Edited', was '" + time + "'");
    }

    // ---------------- G-MENU ----------------

    // [WEB unit-feed.suite.js:750]
    @Test
    @Order(150)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-MENU] Own text message: Reply/Read By User/Forward/Share/Delete/Edit/Gallery, no media options")
    void textMenuMatrix() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "menu");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press did not open the menu");
        List<String> opts = p.menuOptions();
        p.closeMenu();
        sleep(600);
        System.out.println(">>> " + moduleName() + " own text menu: " + opts);
        java.util.Set<String> got = new java.util.HashSet<>();
        for (String o : opts) got.add(o.trim().toLowerCase());
        for (String want : List.of("Reply", "Read By User", "Forward", "Share", "Delete", "Edit", "Gallery")) {
            assertTrue(got.contains(want.toLowerCase()),
                    "An own sent text should offer '" + want + "'. Options were: " + opts);
        }
        for (String not : List.of("Download", "Image Reply", "Publish to Doc Distribution")) {
            assertTrue(!got.contains(not.toLowerCase()),
                    "A text message must not offer '" + not + "'. Options were: " + opts);
        }
    }

    // ---------------- G-REPLY / G-COMMENT ----------------

    // [WEB unit-feed.suite.js:1086 / :1102]
    @Test
    @Order(160)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-REPLY] Reply bar quotes sender and text, hides the paperclip; dismissing restores it")
    void replyBarQuotesAndDismisses() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "rbar");
        assumeTrue(m != null, "Marker did not post — skipping");
        String sender = p.rowUserName(m);
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Reply"), "Reply option not present");
        try {
            assertTrue(p.isReplyPreviewVisible(), "Reply should show the quote bar (llReply)");
            String qText = p.replyQuote("tvMessage");
            String qName = p.replyQuote("tvUserName");
            System.out.println(">>> " + moduleName() + " reply quote: '" + qName + "' / '" + qText + "' (row sender '" + sender + "')");
            assertTrue(m.equals(qText == null ? null : qText.trim()), "The quote should show the parent text, was '" + qText + "'");
            assertTrue(qName != null && !qName.isBlank()
                            && (sender == null || sender.contains(qName.trim()) || qName.contains(sender.trim())),
                    "The quote should name the sender ('" + sender + "'), was '" + qName + "'");
            assertTrue(!p.shownNow("ivAttachment"), "The paperclip should be hidden while replying");
        } finally {
            p.closeReplyQuote();
        }
        assertTrue(!p.shownNow("llReply"), "Dismissing the quote should remove it");
        assertTrue(p.composerText().isEmpty(), "The composer should be empty after dismissing");
        assertTrue(p.visible("ivAttachment", Duration.ofSeconds(3)), "The paperclip should come back");
    }

    // [WEB unit-feed.suite.js:1114]
    @Test
    @Order(161)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-REPLY] A whitespace-only reply sends nothing and keeps the quote")
    void whitespaceReplyRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "rws");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Reply"), "Reply option not present");
        try {
            assertTrue(p.isReplyPreviewVisible(), "Reply quote did not show");
            p.typeMessage("     ");
            boolean sendShown = p.isSendButtonVisible();
            if (sendShown) {
                p.tapSend();
                assertTrue(p.messageAppears(com.zillit.automation.pages.tools.InfoFeedPage.EMPTY_SEND, Duration.ofSeconds(4)),
                        "A whitespace reply should be refused with '" + com.zillit.automation.pages.tools.InfoFeedPage.EMPTY_SEND + "'");
            }
            assertTrue(p.shownNow("llReply"), "The quote should stay after a refused whitespace reply");
        } finally {
            p.clearMessage();
            p.closeReplyQuote();
        }
    }

    // [WEB unit-feed.suite.js:1071 / :1129 / :1153 / :1174] [CONF-05/CONF-08]
    @Test
    @Order(162)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-COMMENT] A comment renders once; its menu offers Read By/Edit/Delete; edit and delete work")
    void commentRendersOnceAndMenuWorks() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "cmt");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assumeTrue(p.tapMenuOption("Reply"), "Reply option not present");
        assumeTrue(p.isReplyPreviewVisible(), "Reply quote did not show");
        String c = m + "-c";
        p.sendMessage(c);
        assertTrue(p.waitMessageSent(c, Duration.ofSeconds(30)), "The comment did not reach the server");
        p.hideKeyboardSafely();
        assertTrue(p.exactTextCount(c) == 1, "The comment should render exactly once, found " + p.exactTextCount(c));

        assertTrue(p.openCommentMenu(c), "The comment's chevron should open its menu");
        List<String> opts = p.menuOptions();
        System.out.println(">>> " + moduleName() + " comment menu: " + opts);
        java.util.Set<String> got = new java.util.HashSet<>();
        for (String o : opts) got.add(o.trim().toLowerCase());
        for (String want : List.of("Read By User", "Edit", "Delete")) {
            if (!got.contains(want.toLowerCase())) p.closeMenu();
            assertTrue(got.contains(want.toLowerCase()), "The comment menu should offer '" + want + "'. Options: " + opts);
        }
        // Edit the comment.
        assertTrue(p.tapMenuOption("Edit"), "Could not tap Edit on the comment");
        sleep(1000);
        String ce = c + "E";
        boolean dialog = p.isEditDialogOpen(Duration.ofSeconds(4));
        if (dialog) {
            p.setEditDialogText(ce);
            p.saveEditDialog();
        } else {
            // Comments are edited in the composer (CI.kt showCommentOnEditView path).
            p.typeMessage(ce);
            p.tapSend();
        }
        assertTrue(p.waitMessageSent(ce, Duration.ofSeconds(30)), "The edited comment '" + ce + "' should be shown");
        // Delete: No keeps it, Yes removes it.
        assertTrue(p.openCommentMenu(ce), "Comment menu did not reopen");
        assertTrue(p.tapMenuOption("Delete"), "Could not tap Delete on the comment");
        assertTrue(confirmPopupShown(Duration.ofSeconds(6)), "Deleting a comment should ask to confirm");
        confirmPopupCancel(Duration.ofSeconds(4));
        sleep(1000);
        assertTrue(p.messageShown(ce, Duration.ofSeconds(3)), "Answering No must keep the comment");
        assertTrue(p.openCommentMenu(ce), "Comment menu did not reopen");
        assertTrue(p.tapMenuOption("Delete"), "Could not tap Delete on the comment");
        assertTrue(confirmYesDialog(Duration.ofSeconds(6)), "Delete confirmation did not appear");
        sleep(2000);
        assertTrue(!p.messageShown(ce, Duration.ofSeconds(2)), "A deleted comment should be gone (CONF-08)");
    }

    // ---------------- G-LIMIT ----------------

    // [WEB unit-feed.suite.js:311] [INFO-25 / CONF-22: Android 2,000 vs web 8,000]
    @Test
    @Order(170)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-LIMIT] A 2,000-character message posts; 2,001 is refused with the limit message")
    void messageLengthLimit() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        int limit = com.zillit.automation.pages.tools.InfoFeedPage.TEXT_LIMIT;
        String okHead = autoMarker("lim2000");
        String ok = okHead + " " + repeatWords(limit - okHead.length() - 1);
        p.sendMessage(ok);
        autoPosted.add(okHead);
        p.hideKeyboardSafely();
        assertTrue(p.messageShown(okHead, Duration.ofSeconds(20)), "A 2,000-character message should post");
        assertTrue(waitNoPendingRow(p, okHead, Duration.ofSeconds(30)), "The 2,000-character message did not reach the server");

        String badHead = autoMarker("lim2001");
        String bad = badHead + " " + repeatWords(limit - badHead.length());
        assertTrue(bad.length() == limit + 1, "fixture length");
        try {
            p.typeMessage(bad);
            p.tapSend();
            assertTrue(p.messageAppears("Characters limit allowed to send", Duration.ofSeconds(4)),
                    "2,001 characters should be refused with '" + com.zillit.automation.pages.tools.InfoFeedPage.LIMIT_ERROR + "'");
            sleep(1000);
            p.hideKeyboardSafely();
            assertTrue(p.messageTextContaining(badHead) == null || p.shownNow("etMessage")
                            && p.composerText().startsWith(badHead) && p.containsTextCount(badHead) == 1,
                    "The over-limit message must not be posted");
        } finally {
            p.clearMessage();
        }
    }

    /** No clock and no Retry on the card whose message CONTAINS the text (long rows are truncated). */
    private boolean waitNoPendingRow(com.zillit.automation.pages.tools.InfoFeedPage p, String part, Duration t) {
        long deadline = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String clock = p.rowChildText(part, "ivClock");
            String retry = p.rowChildText(part, "btnTryAgain");
            if (clock == null && retry == null) return true;
            sleep(1000);
        }
        return false;
    }

    // ---------------- G-RENDER ----------------

    // [WEB unit-feed.suite.js:327]
    @Test
    @Order(171)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-RENDER] A 1,500-character message is collapsed with 'Read more' and expands to 'Read less'")
    void readMoreReadLess() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String head = autoMarker("rmore");
        p.sendMessage(head + " " + repeatWords(1500));
        autoPosted.add(head);
        p.hideKeyboardSafely();
        assertTrue(p.messageShown(head, Duration.ofSeconds(20)), "The long message did not appear");
        String shown = p.messageTextContaining(head);
        assertTrue(shown != null && shown.trim().endsWith("Read more"),
                "A message over 1,000 characters should end with 'Read more', was '…"
                        + (shown == null ? null : shown.substring(Math.max(0, shown.length() - 40))) + "'");
        boolean expanded = false;
        for (int i = 0; i < 16 && !expanded; i++) {
            p.tapReadToggle(head, i);
            String now = p.messageTextContaining(head);
            expanded = now != null && now.trim().endsWith("Read less");
        }
        assertTrue(expanded, "Tapping 'Read more' should show the full text ending in 'Read less'");
    }

    // [WEB unit-feed.suite.js:343 / :394]
    @Test
    @Order(172)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-RENDER] HTML, markdown and special characters render literally")
    void literalAndSpecialCharacters() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String head = autoMarker("lit");
        String text = head + " <b>x</b><script>1</script> **y** Ünïcødé ✓ שלום & \"q\"";
        p.typeMessage(text);
        String typed = p.composerText();
        assumeTrue(text.equals(typed), "The driver could not type the special characters (got '" + typed + "')");
        p.tapSend();
        autoPosted.add(head);
        p.hideKeyboardSafely();
        assertTrue(p.waitMessageSent(text, Duration.ofSeconds(30)), "The message did not reach the server");
        String shown = p.messageTextContaining(head);
        assertTrue(text.equals(shown), "The text should render exactly as typed.\n typed: " + text + "\n shown: " + shown);
    }

    // [WEB unit-feed.suite.js:1583]
    @Test
    @Order(173)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-RENDER] A row shows the sender and a 'MMM dd, yyyy hh:mm a' time")
    void rowHeaderAndTimestamp() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "row");
        assumeTrue(m != null, "Marker did not post — skipping");
        p.hideKeyboardSafely();
        String who = p.rowUserName(m);
        String time = p.rowTime(m);
        // The time sits at the bottom of the card, which can be under the composer.
        for (int i = 0; i < 3 && time == null; i++) {
            p.scrollNewer(1);
            time = p.rowTime(m);
        }
        System.out.println(">>> " + moduleName() + " row header: '" + who + "' time '" + time + "'");
        assertTrue(who != null && !who.isBlank(), "The row should name its sender");
        assertTrue(time != null && time.trim().matches("(?i)[a-z]{3} \\d{2}, \\d{4} \\d{2}:\\d{2} (AM|PM)"),
                "The time should read like 'NOV 23, 2026 09:41 PM' (MMM dd, yyyy hh:mm a), was '" + time + "'");
    }

    // ---------------- G-DRAFT / G-PERSIST / G-DOUBLE ----------------

    // [WEB unit-feed.suite.js:449] [INFO-23 / CONF-16]
    @Test
    @Order(180)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-DRAFT] INFO-23/CONF-16: an unsent draft survives leaving and reopening")
    void draftSurvivesReopen() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String d = autoMarker("draft");
        p.typeDraft(d);
        try {
            reopenModule();
            com.zillit.automation.pages.tools.InfoFeedPage again = new com.zillit.automation.pages.tools.InfoFeedPage(driver);
            assumeTrue(again.isInputBarVisible(), moduleName() + " did not reopen");
            String restored = again.composerText();
            System.out.println(">>> " + moduleName() + " draft after reopen: '" + restored + "'");
            assertTrue(d.equals(restored), "INFO-23/CONF-16: the unsent draft '" + d
                    + "' should be restored after reopening, composer held '" + restored + "'");
        } finally {
            new com.zillit.automation.pages.tools.InfoFeedPage(driver).clearMessage();
        }
    }

    // [WEB unit-feed.suite.js:466]
    @Test
    @Order(181)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-PERSIST] A posted message is still there after leaving and reopening")
    void messagePersistsAfterReopen() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "persist");
        assumeTrue(m != null, "Marker did not post — skipping");
        reopenModule();
        com.zillit.automation.pages.tools.InfoFeedPage again = new com.zillit.automation.pages.tools.InfoFeedPage(driver);
        assertTrue(again.messageShownScrolled(m, 3), "The message should still be in " + moduleName() + " after a reopen");
        assertTrue(again.waitMessageSent(m, Duration.ofSeconds(10)), "After a reopen the message should show as sent");
    }

    // [WEB unit-feed.suite.js:501]
    @Test
    @Order(182)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-DOUBLE] A fast double tap on Send posts the message once")
    void doubleTapSendPostsOnce() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = autoMarker("dbl");
        p.typeMessage(m);
        assumeTrue(p.isSendButtonVisible(), "Send button not shown");
        p.doubleTapSend();
        autoPosted.add(m);
        // The send button turns into the mic after the first tap, so the second tap can start
        // a voice recording (and the mic permission prompt): cancel it, never post it.
        waitAndDismissPermissions(Duration.ofSeconds(2));
        if (p.isRecordingActive(Duration.ofSeconds(1))) {
            System.out.println(">>> " + moduleName() + " second tap started a recording — cancelling it");
            p.cancelRecording();
            sleep(800);
        }
        p.hideKeyboardSafely();
        assertTrue(p.waitMessageSent(m, Duration.ofSeconds(30)), "The message did not post");
        sleep(2000);
        int n = p.exactTextCount(m);
        assertTrue(n == 1, "A double tap on Send should post once, found " + n + " copies");
    }

    // ---------------- G-EMOJI ----------------

    // [WEB unit-feed.suite.js:418 / :438]
    @Test
    @Order(185)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-EMOJI] The emoji picker appends an emoji, backspace removes it whole, and it sends")
    void emojiPickerAppendsAndSends() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = autoMarker("emoji");
        try {
            p.typeMessage(m);
            assertTrue(p.openEmojiPicker(), "The emoji button should open the picker (emojiPicker)");
            String e = p.pickFirstEmoji();
            assumeTrue(e != null, "No emoji cell found in the picker");
            String withEmoji = p.composerText();
            assertTrue(withEmoji.equals(m + e), "The emoji should be appended: expected '" + m + e + "', was '" + withEmoji + "'");
            p.tapEmojiBackspace();
            assertTrue(p.composerText().equals(m),
                    "Backspace should remove the whole emoji, composer was '" + p.composerText() + "'");
            p.pickFirstEmoji();
            p.tapSend();
            autoPosted.add(m);
            p.hideKeyboardSafely();
            assertTrue(p.waitMessageSent(m + e, Duration.ofSeconds(30)), "The message with the emoji did not post");
        } finally {
            p.clearMessage();
            if (p.shownNow("emojiPicker")) { try { driver.navigate().back(); } catch (Exception ignored) { } }
        }
    }

    // ---------------- READ BY (INFO-21 / CONF-19) ----------------

    // [WEB unit-feed.suite.js:1357] [INFO-21 / CONF-19]
    @Test
    @Order(190)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-READBY] INFO-21/CONF-19: a Read By search that matches nobody says 'No Users found'")
    void readBySearchNoMatch() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "rbs");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press failed");
        assertTrue(p.tapMenuOption("Read By User"), "Read By User is not offered on a sent message");
        try {
            assertTrue(p.readByOpen(Duration.ofSeconds(10)), "The Read By screen did not open");
            assertTrue(p.readBySearch("zzqq" + (System.currentTimeMillis() % 1000)), "Read By has no search field");
            p.hideKeyboardSafely();
            assertTrue(p.visible("tvNoUsersFound", Duration.ofSeconds(4)),
                    "INFO-21/CONF-19: a search matching nobody should say 'No Users found'");
        } finally {
            p.leaveOverlayScreen();
        }
    }

    // ---------------- G-OTHER ----------------

    // [WEB unit-feed.suite.js:1638] Android offers Edit on others' messages, then refuses it
    @Test
    @Order(195)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-OTHER] Editing another member's message is refused")
    void otherMembersMessageEditRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "own");
        assumeTrue(m != null, "Marker did not post — skipping");
        String me = p.rowUserName(m);
        assumeTrue(me != null && !me.isBlank(), "Could not read own sender name");
        String other = null;
        for (int i = 0; i < 6 && other == null; i++) {
            other = firstTextNotBy(me);
            if (other == null) p.scrollOlder(1);
        }
        assumeTrue(other != null, "No message from another member in the loaded " + moduleName() + " feed");
        System.out.println(">>> " + moduleName() + " other member's message: '" + other + "'");
        assumeTrue(p.longPressMessage(other), "Long-press on the other member's message failed");
        List<String> opts = p.menuOptions();
        boolean editOffered = opts.stream().anyMatch(o -> o.trim().equalsIgnoreCase("Edit"));
        if (!editOffered) {
            p.closeMenu();
            return;   // matches web: Edit is not offered on someone else's message
        }
        assertTrue(p.tapMenuOption("Edit"), "Could not tap Edit");
        boolean refused = p.messageAppears("Permission To Edit", Duration.ofSeconds(4));
        boolean dialog = p.visible("editTextInput", Duration.ofSeconds(1));
        if (dialog) p.cancelEditDialog();
        assertTrue(refused && !dialog, "Edit on another member's message must be refused with '"
                + com.zillit.automation.pages.tools.InfoFeedPage.NO_EDIT_PERMISSION + "' and no edit dialog");
    }

    /** The tvMessage of the first on-screen row whose sender is not {@code me}. */
    private String firstTextNotBy(String me) {
        try {
            String pkg = appPackage;
            String xp = "//*[@resource-id='" + pkg + ":id/materialCardView'][.//*[@resource-id='" + pkg
                    + ":id/tvUserName' and @text!=" + xq(me) + "]]//*[@resource-id='" + pkg + ":id/tvMessage']";
            for (org.openqa.selenium.WebElement e : driver.findElements(io.appium.java_client.AppiumBy.xpath(xp))) {
                String t = e.getText();
                if (t != null && t.length() >= 4 && t.length() < 200 && !t.contains("\"")) return t;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static String xq(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    // ---------------- G-UPLOAD / G-CAPTION / G-GALLERY ----------------
    //
    // Fixtures come from the runner's prep step (run-and-report.sh): zl_test_doc.pdf in
    // Documents, zl_dd_word.docx / zl_dd_book.xlsx / zl_drive_sheet.csv in Download.

    /** The caption of the PDF posted by uploadDocumentsWithCaption, for the later cases. */
    private String pdfCaption;

    /**
     * Attach → Document → pick → (caption, when the build shows a preview) → send. Info posts a
     * picked document straight away (openPicker doUpload=false, no preview), so success is a NEW
     * document bubble that finishes uploading. Returns the caption used, "" for none, or null.
     */
    private String sendDocument(com.zillit.automation.pages.tools.InfoFeedPage p, String file, String caption) {
        p.hideKeyboardSafely();
        boolean opened = false;
        int before = 0;
        for (int attempt = 0; attempt < 2 && !opened; attempt++) {
            if (!inModuleScreen() || !p.isAttachmentVisible()) { reopenModule(); p.hideKeyboardSafely(); }
            if (!p.isAttachmentVisible()) continue;
            toNewest(p);
            before = p.docBubbleCountByBadge();
            p.tapAttachment();
            if (!p.isAttachmentPickerVisible()) { System.out.println(">>> picker sheet did not open"); continue; }
            opened = p.tapAttachmentOption("Document");
            if (!opened) {
                System.out.println(">>> no 'Document' in the picker: " + p.attachmentOptions());
                p.dismissAttachmentPicker();
                sleep(800);
            }
        }
        if (!opened) return null;
        waitAndDismissPermissions(Duration.ofSeconds(3));
        if (!p.pickSystemFileByName(file, Duration.ofSeconds(20))) { p.leaveOverlayScreen(); return null; }
        com.zillit.automation.pages.common.MediaPreviewPage prev =
                new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        String used = "";
        if (prev.isPreviewOpen()) {
            prev.setCaption(caption); keepPreview();
            prev.tapPreviewSend();
            prev.waitPreviewClosed(Duration.ofSeconds(45));
            used = caption;
            autoPosted.add(caption);
        }
        confirmPopupAccept(Duration.ofSeconds(2));   // "Keep name private?" when that setting is on
        // Only two or three tall document bubbles fit on screen, so a count that stays put can
        // still be a new post: the caption on the newest row settles it.
        long end = System.currentTimeMillis() + 30000;
        boolean landed = false;
        while (System.currentTimeMillis() < end && !landed) {
            toNewest(p);
            landed = p.docBubbleCountByBadge() > before
                    || (!used.isEmpty() && p.messageShown(used, Duration.ofSeconds(1)));
            if (!landed) sleep(1000);
        }
        System.out.println(">>> " + moduleName() + " document " + file + ": bubbles " + before + " → "
                + p.docBubbleCountByBadge() + ", preview=" + !used.isEmpty() + ", landed=" + landed);
        if (!landed) return null;
        return p.waitAttachmentSent(Duration.ofSeconds(60)) ? used : null;
    }

    // [WEB unit-feed.suite.js:556 / :570 / :581 / :765]
    @Test
    @Order(210)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-UPLOAD] PDF, Word and Excel documents post with a caption; a document's menu adds Download/Publish, no Image Reply")
    void uploadDocumentsWithCaption() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        // The PDF goes LAST so it is the newest document for the later cases.
        String[][] files = {{"zl_dd_word", "docx"}, {"zl_dd_book", "xlsx"}, {"zl_test_doc", "pdf"}};
        List<String> failed = new java.util.ArrayList<>();
        int sent = 0;
        String pdfCap = null;
        for (String[] f : files) {
            String cap = autoMarker("doc" + f[1]);
            String used = sendDocument(p, f[0], cap);
            System.out.println(">>> " + moduleName() + " document " + f[0] + "." + f[1] + " used caption='" + used
                    + "' (" + p.attachmentSendState() + ")");
            if (used != null) { sent++; if ("pdf".equals(f[1])) pdfCap = used; } else failed.add(f[0] + "." + f[1]);
        }
        assumeTrue(sent > 0, "No document could be picked on this device (fixtures missing?)");
        assertTrue(failed.isEmpty(), "These documents did not post: " + failed);
        pdfPosted = true;
        if (pdfCap != null && !pdfCap.isEmpty()) {
            boolean shown = p.messageShown(pdfCap, Duration.ofSeconds(5));
            System.out.println(">>> " + moduleName() + " PDF caption shown on the row: " + shown);
            if (shown) pdfCaption = pdfCap;
        }

        assertTrue(p.longPressNewestDocBubble(), "Long-press on the newest document (the PDF) did not open the menu");
        List<String> opts = p.menuOptions();
        p.closeMenu();
        sleep(600);
        System.out.println(">>> " + moduleName() + " own PDF menu: " + opts);
        java.util.Set<String> got = new java.util.HashSet<>();
        for (String o : opts) got.add(o.trim().toLowerCase());
        for (String want : List.of("Download", "Publish to Doc Distribution", "Forward", "Share", "Delete", "Gallery")) {
            assertTrue(got.contains(want.toLowerCase()), "An own document should offer '" + want + "'. Options: " + opts);
        }
        assertTrue(!got.contains("image reply"), "A document must not offer 'Image Reply'. Options: " + opts);
        if (pdfCaption != null) assertTrue(got.contains("edit"), "A captioned document should offer Edit. Options: " + opts);
    }

    /** Set once uploadDocumentsWithCaption posted the PDF. */
    private boolean pdfPosted;

    // [WEB unit-feed.suite.js:570 csv] [INFO-27 / CONF-23]
    @Test
    @Order(211)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-UPLOAD] INFO-27/CONF-23: a CSV can be chosen in the Document picker")
    void csvDocumentSelectable() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        p.hideKeyboardSafely();
        p.tapAttachment();
        assumeTrue(p.isAttachmentPickerVisible(), "Attachment picker did not open");
        assumeTrue(p.tapAttachmentOption("Document"), "No Document option");
        waitAndDismissPermissions(Duration.ofSeconds(3));
        com.zillit.automation.pages.common.SystemFilePickerPage picker =
                new com.zillit.automation.pages.common.SystemFilePickerPage(driver);
        Boolean selectable;
        try {
            assumeTrue(picker.isOpen(Duration.ofSeconds(10)), "The system picker did not open");
            selectable = picker.fileSelectable("zl_drive_sheet.csv");
        } finally {
            picker.cancel();
            p.leaveOverlayScreen();
        }
        System.out.println(">>> " + moduleName() + " CSV selectable=" + selectable);
        assumeTrue(selectable != null, "zl_drive_sheet.csv is not on the device (fixture missing)");
        assertTrue(selectable, "INFO-27/CONF-23: a CSV is listed but greyed out — FilePickerContract's "
                + "document MIME list has no text/csv");
    }

    // [WEB unit-feed.suite.js:581]
    @Test
    @Order(227)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-CAPTION] A captioned media message's Edit is prefilled with the caption and updates it")
    void documentCaptionEdit() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        // Info posts documents without a preview, so the captioned media is the image.
        String target = null;
        for (String c : new String[]{imageCaption, pdfCaption}) {
            if (target == null && c != null && p.messageShownScrolled(c, 10)) target = c;
        }
        assumeTrue(target != null,
                "Needs a captioned document or image (uploadDocumentsWithCaption / imageWithCaptionAndMenu)");
        String original = target;
        assumeTrue(p.longPressMessage(original), "Long-press failed");
        assertTrue(p.tapMenuOption("Edit"), "A captioned media message should offer Edit");
        assertTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "The Edit description dialog should open");
        String pre = p.textOf("editTextInput");
        assertTrue(original.equals(pre == null ? null : pre.trim()),
                "The dialog should be prefilled with the caption, was '" + pre + "'");
        String updated = original + "-E";
        p.setEditDialogText(updated);
        p.saveEditDialog();
        autoPosted.add(updated);
        assertTrue(p.messageShown(updated, Duration.ofSeconds(30)), "The new caption should show on the media row");
        if (original.equals(pdfCaption)) pdfCaption = updated; else imageCaption = updated;
    }

    // [WEB unit-feed.suite.js:1551] [INFO-24 / CONF-21]
    @Test
    @Order(213)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-GALLERY] INFO-24/CONF-21: Gallery Docs lists the document by name with a size and unit")
    void galleryDocsShowsNameAndSize() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        toNewest(p);
        assumeTrue(pdfPosted || p.docBubbleCountByBadge() > 0, "Needs the PDF posted by uploadDocumentsWithCaption");
        assumeTrue(p.longPressNewestDocBubble(), "Long-press on the newest document failed");
        assertTrue(p.tapMenuOption("Gallery"), "Gallery should be offered");
        try {
            assertTrue(p.tapLibraryTab("Docs"), "The library should have a Docs tab");
            String label = p.libraryTabLabel("Docs");
            List<String> titles = p.texts("tvTitle");
            List<String> subs = p.texts("tvSubtitle");
            System.out.println(">>> " + moduleName() + " Docs tab '" + label + "' titles=" + titles + " subtitles=" + subs);
            screenshot("tool-" + slug() + "-gallery-docs");
            assertTrue(label != null && label.matches("(?i)Docs \\(\\d+\\)"), "The Docs tab should show a count, was '" + label + "'");
            assertTrue(titles.stream().anyMatch(t -> t.contains("zl_test_doc") && !t.contains("/")),
                    "The PDF should be listed by its file name (not a storage path). Titles: " + titles);
            assertTrue(subs.stream().anyMatch(s -> s.matches("(?i).*\\d+(\\.\\d+)?\\s?(B|KB|MB|GB|bytes)\\b.*")),
                    "INFO-24/CONF-21: a document should show its size WITH a unit. Subtitles: " + subs);
        } finally {
            p.leaveOverlayScreen();
        }
    }

    // ---------------- batch 2: media menus, viewers, DD, picker cancel, PDF/JS ----------------

    private String imageCaption;

    /** Media caption row's options, read and closed. */
    private java.util.Set<String> optionsOf(com.zillit.automation.pages.tools.InfoFeedPage p) {
        List<String> opts = p.menuOptions();
        p.closeMenu();
        sleep(600);
        java.util.Set<String> got = new java.util.HashSet<>();
        for (String o : opts) got.add(o.trim().toLowerCase());
        System.out.println(">>> " + moduleName() + " menu: " + opts);
        return got;
    }

    // [WEB unit-feed.suite.js:604 / :787]
    @Test
    @Order(215)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-UPLOAD/G-MENU] An image with a caption posts; its menu adds Download, Image Reply, Publish and Edit")
    void imageWithCaptionAndMenu() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String cap = autoMarker("imgcap");
        boolean sent = p.sendOneImage(cap, v -> waitAndDismissPermissions(Duration.ofSeconds(4)));
        assumeTrue(sent, "The in-app gallery had no image to pick on this device");
        autoPosted.add(cap);
        toNewest(p);
        assertTrue(p.messageShown(cap, Duration.ofSeconds(30)) || p.messageShownScrolled(cap, 3),
                "The caption should show on the image row");
        assertTrue(p.waitAttachmentSent(Duration.ofSeconds(60)), "The image did not finish uploading: " + p.attachmentSendState());
        imageCaption = cap;
        assertTrue(p.longPressMessage(cap), "Long-press on the image caption did not open the menu");
        java.util.Set<String> got = optionsOf(p);
        for (String want : List.of("Download", "Image Reply", "Publish to Doc Distribution", "Edit", "Forward", "Share", "Delete")) {
            assertTrue(got.contains(want.toLowerCase()), "An own captioned image should offer '" + want + "'. Options: " + got);
        }
    }

    // [WEB unit-feed.suite.js:731]
    @Test
    @Order(216)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-IMGREPLY] Image Reply posts a NEW image row with its caption (not a comment)")
    void imageReplyPostsNewImage() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        assumeTrue(imageCaption != null && p.messageShownScrolled(imageCaption, 10), "Needs the image from imageWithCaptionAndMenu");
        int before = p.imageBubbleCount();
        assumeTrue(p.longPressMessage(imageCaption), "Long-press failed");
        assertTrue(p.tapMenuOption("Image Reply"), "Image Reply should be offered on an own image");
        waitAndDismissPermissions(Duration.ofSeconds(3));
        com.zillit.automation.pages.common.MediaPreviewPage media =
                new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        sleep(2500);
        // Image Reply opens the image editor / preview on the PARENT image (CI.kt handleImageReply).
        boolean preview = media.isPreviewOpen() || media.isEditorOpen();
        System.out.println(">>> " + moduleName() + " Image Reply opened: preview=" + media.isPreviewOpen()
                + " editor=" + media.isEditorOpen() + " activity=" + p.currentActivity());
        screenshot("tool-" + slug() + "-image-reply");
        if (!preview) p.leaveOverlayScreen();
        assertTrue(preview, "Image Reply should open the image editor/preview");
        String cap = autoMarker("imgreply");
        try {
            if (media.isEditorOpen() && media.isSaveVisible()) { media.tapSave(); sleep(1500); }
            if (media.isPreviewOpen()) {
                media.setCaption(cap); keepPreview();
                media.tapPreviewSend();
                media.waitPreviewClosed(Duration.ofSeconds(60));
            }
        } finally {
            exitPreview();
        }
        autoPosted.add(cap);
        p.leaveOverlayScreen();
        toNewest(p);
        assertTrue(p.messageShown(cap, Duration.ofSeconds(40)) || p.messageShownScrolled(cap, 3), "The Image Reply should post a row captioned '" + cap + "'");
        assertTrue(p.imageBubbleCount() >= before, "Image Reply should add an image row, not a comment");
    }

    // [WEB unit-feed.suite.js:765]
    @Test
    @Order(217)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-MENU] An own document WITHOUT caption offers Download and Publish, and no Edit or Image Reply")
    void documentWithoutCaptionMenu() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        p.hideKeyboardSafely();
        p.tapAttachment();
        assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Document"), "Document picker not offered");
        waitAndDismissPermissions(Duration.ofSeconds(3));
        assumeTrue(p.pickSystemFileByName("zl_test_doc", Duration.ofSeconds(20)), "zl_test_doc.pdf not pickable on this device");
        com.zillit.automation.pages.common.MediaPreviewPage prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        sleep(1500);
        if (prev.isPreviewOpen()) { prev.tapPreviewSend(); prev.waitPreviewClosed(Duration.ofSeconds(45)); }
        toNewest(p);
        assertTrue(p.waitAttachmentSent(Duration.ofSeconds(60)), "The document did not post: " + p.attachmentSendState());
        toNewest(p);
        assertTrue(p.longPressNewestDocBubble(), "Long-press on the newest document did not open the menu");
        java.util.Set<String> got = optionsOf(p);
        assertTrue(got.contains("download"), "A document should offer Download. Options: " + got);
        assertTrue(got.contains("publish to doc distribution"), "A document should offer Publish to Doc Distribution. Options: " + got);
        assertTrue(!got.contains("edit"), "A document without a caption must not offer Edit. Options: " + got);
        assertTrue(!got.contains("image reply"), "A document must not offer Image Reply. Options: " + got);
    }

    // [WEB unit-feed.suite.js:1399] Download (CI: through the watermark path, CI.kt handleSave)
    @Test
    @Order(218)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-DOWNLOAD] Download on a document saves it ('File Downloaded Successfully.')")
    void downloadDocument() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        toNewest(p);
        assumeTrue(p.docBubbleCountByBadge() > 0 || p.docBubbleCount() > 0, "No document in view (documentWithoutCaptionMenu did not run)");
        assumeTrue(p.longPressNewestDocBubble(), "Long-press on the newest document failed");
        assertTrue(p.tapMenuOption("Download"), "Download should be offered on a document");
        waitAndDismissPermissions(Duration.ofSeconds(3));
        boolean saved = p.toastShown("Downloaded Successfully", Duration.ofSeconds(30));
        boolean refused = p.textShown("Downloading Rights", Duration.ofSeconds(1));
        System.out.println(">>> " + moduleName() + " download: saved=" + saved + " refusedForRights=" + refused);
        assertTrue(!refused, "INFO-31/CONF-28: an admin with download rights was refused ('You do not have Downloading Rights')");
        assertTrue(saved, "Download should end with 'File Downloaded Successfully.'");
    }

    // [WEB unit-feed.suite.js:1435] Android opens documents in another app or its own viewer
    @Test
    @Order(219)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-VIEWER] Tapping a document opens a viewer, and Back returns to the feed")
    void documentTapOpensViewer() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        toNewest(p);
        assumeTrue(p.docBubbleCountByBadge() > 0 || p.docBubbleCount() > 0, "No document in view");
        boolean opened = p.openNewestDoc();
        System.out.println(">>> " + moduleName() + " doc viewer: opened=" + opened + " pkg=" + p.safeCurrentPackage()
                + " activity=" + p.currentActivity());
        screenshot("tool-" + slug() + "-doc-viewer");
        p.returnToChat(appPackage);
        p.leaveOverlayScreen();
        assertTrue(opened, "Tapping a PDF should open a viewer (another app or an in-app viewer)");
        assertTrue(inModuleScreen() && p.isInputBarVisible(), "Back should return to " + moduleName());
    }

    // [WEB unit-feed.suite.js:1456]
    @Test
    @Order(220)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-VIEWER] Tapping an image opens the image viewer, and Back returns")
    void imageViewerOpens() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        assumeTrue(imageCaption != null && p.messageShownScrolled(imageCaption, 10), "Needs the image from imageWithCaptionAndMenu");
        // The newest imageParent may be a DOCUMENT bubble (they share the view), so tap the
        // image itself: it sits directly above its caption.
        p.hideKeyboardSafely();
        org.openqa.selenium.Rectangle cap = p.messageRect(imageCaption);
        assumeTrue(cap != null, "Image caption not on screen");
        if (cap.getY() < 500) { p.nudgeChatUp(); cap = p.messageRect(imageCaption); }
        String before = p.currentActivity();
        p.tapAt(cap.getX() + cap.getWidth() / 2, Math.max(cap.getY() - 200, 400));
        sleep(2000);
        String after = p.currentActivity();
        boolean opened = p.isImageViewerOpen(Duration.ofSeconds(3)) || (after != null && !after.equals(before));
        System.out.println(">>> " + moduleName() + " image tap: " + before + " → " + after);
        screenshot("tool-" + slug() + "-image-viewer");
        p.closeViewer();
        p.leaveOverlayScreen();
        assertTrue(opened, "Tapping an image should open the image viewer");
        assertTrue(p.isInputBarVisible(), "Back should return to the feed");
    }

    // [WEB unit-feed.suite.js:1382] Publish to Doc Distribution, cancelled
    @Test
    @Order(221)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-DD] Publish to Doc Distribution asks to publish the file; Cancel publishes nothing")
    void publishToDocDistCancel() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        toNewest(p);
        assumeTrue(p.docBubbleCountByBadge() > 0 || p.docBubbleCount() > 0, "No document in view");
        String newestBefore = lastOf(p.visibleMessages());
        assumeTrue(p.longPressNewestDocBubble(), "Long-press on the newest document failed");
        assertTrue(p.tapMenuOption("Publish to Doc Distribution"), "A document should offer Publish to Doc Distribution");
        // This build asks first: 'Publish to Document Distribution — "<file>" to the Document
        // Distribution library?' with Cancel / Publish (an AlertDialog).
        boolean ask = p.textShown("Document Distribution library", Duration.ofSeconds(8));
        boolean noRights = !ask && p.textShown("permission", Duration.ofSeconds(1));
        System.out.println(">>> " + moduleName() + " DD publish: confirm=" + ask + " noRights=" + noRights);
        screenshot("tool-" + slug() + "-dd-confirm");
        boolean cancelled = ask && p.tapExactText("Cancel");
        sleep(1200);
        boolean gone = !p.textShown("Document Distribution library", Duration.ofSeconds(1));
        if (!gone) { try { driver.navigate().back(); } catch (Exception ignored) { } }
        p.leaveOverlayScreen();
        assertTrue(ask || noRights, "Publish should ask to publish the file to the Document Distribution library "
                + "(or explain missing DD rights)");
        if (ask) assertTrue(cancelled && gone, "Cancel should close the publish prompt");
        toNewest(p);
        assertTrue(java.util.Objects.equals(newestBefore, lastOf(p.visibleMessages())),
                "Cancelling must not post anything here (newest row changed)");
        assertTrue(inModuleScreen() && p.isInputBarVisible(), "Cancelling the sheet should return to " + moduleName());
    }

    // [WEB unit-feed.suite.js:631] audio file, and the Android voice note
    @Test
    @Order(222)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-UPLOAD/G-MENU] A voice note posts as an audio row; its menu offers Download and no Edit")
    void voiceNotePostsAndMenu() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        int before = p.audioBubbleCount();
        p.hideKeyboardSafely();
        p.clearMessage();
        assumeTrue(p.isRecordButtonAvailable(), "No record button");
        p.tapRecord();
        waitAndDismissPermissions(Duration.ofSeconds(4));
        if (!p.isRecordingActive(Duration.ofSeconds(3))) { p.tapRecord(); }
        assumeTrue(p.isRecordingActive(Duration.ofSeconds(4)), "Recording did not start (microphone unavailable)");
        sleep(3000);
        p.sendRecording();
        toNewest(p);
        long deadline = System.currentTimeMillis() + 40000;
        while (System.currentTimeMillis() < deadline && p.audioBubbleCount() <= before) sleep(1000);
        assertTrue(p.audioBubbleCount() > before || p.isAudioBubblePresent(Duration.ofSeconds(2)),
                "A recorded voice note should post an audio row");
        assertTrue(p.waitAttachmentSent(Duration.ofSeconds(60)), "The voice note did not finish uploading: " + p.attachmentSendState());
        p.hideKeyboardSafely();
        p.scrollNewer(3);          // the new audio row sits under the composer until scrolled
        // The row's long-press is on mainLi; clickable children (sender name, play button,
        // seek bar) consume a press, so try the plain labels at the bottom of the newest row.
        boolean menu = p.longPressNewest("fileSize") || p.longPressNewest("durationCount")
                || p.longPressRowAround("ivPlayPause");
        if (!menu) {
            // AudioVH attaches the row's long-press only when data.deleted == 0L; a row
            // inserted locally on send may carry deleted == null until the feed is reloaded.
            reopenModule();
            toNewest(p);
            boolean afterReload = p.longPressNewest("fileSize") || p.longPressRowAround("ivPlayPause");
            if (afterReload) p.closeMenu();
            assertTrue(false, "The fresh voice-note row has no long-press menu"
                    + (afterReload ? "; it appears only after leaving and reopening " + moduleName()
                    + " (AudioVH sets the listener only when deleted == 0L)" : ", not even after a reopen"));
        }
        java.util.Set<String> got = optionsOf(p);
        assertTrue(got.contains("download"), "An audio row should offer Download. Options: " + got);
        assertTrue(!got.contains("edit"), "An audio row without caption must not offer Edit. Options: " + got);
    }

    // [WEB unit-feed.suite.js:615]
    @Test
    @Order(223)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-UPLOAD] Two images picked together post two image rows")
    void twoImagesInOnePick() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        // Rows are counted through the Gallery's "Media (n)" tab: on screen only a couple of
        // tall image rows fit, so counting bubbles cannot tell one new image from two.
        String probe = postAuto(p, "twoprobe");
        assumeTrue(probe != null, "Probe message did not post");
        int before = mediaCount(p, probe);
        assumeTrue(before >= 0, "Could not read the Gallery's Media count");
        p.hideKeyboardSafely();
        p.tapAttachment();
        assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Gallery"), "Gallery option not offered");
        waitAndDismissPermissions(Duration.ofSeconds(4));
        com.zillit.automation.pages.common.MediaPreviewPage media = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        assumeTrue(media.isGalleryOpen(), "In-app gallery did not open");
        media.openAllMediaAlbumIfListed();
        assumeTrue(media.galleryThumbCount() >= 2, "Fewer than two images on the device");
        int picked = media.selectPhotos(2);
        assumeTrue(picked >= 2 && media.isSendFabVisible(), "Could not select two images");
        media.tapGallerySendFab();
        assumeTrue(media.isPreviewOpen(), "Preview did not open");
        String cap = autoMarker("two");
        try {
            media.setCaption(cap); keepPreview();
            media.tapPreviewSend();
            media.waitPreviewClosed(Duration.ofSeconds(60));
        } finally {
            exitPreview();
        }
        autoPosted.add(cap);
        toNewest(p);
        assertTrue(p.waitAttachmentSent(Duration.ofSeconds(90)), "The images did not finish uploading: " + p.attachmentSendState());
        sleep(2000);
        int after = mediaCount(p, probe);
        System.out.println(">>> " + moduleName() + " Gallery media before=" + before + " after=" + after);
        assertTrue(after == before + 2, "Two picked images should add two media items (before " + before + ", after " + after + ")");
    }

    /** The Gallery's "Media (n)" count, opened from the message's menu; -1 if unreadable. */
    private int mediaCount(com.zillit.automation.pages.tools.InfoFeedPage p, String anchor) {
        try {
            if (!p.messageShownScrolled(anchor, 4) || !p.longPressMessage(anchor) || !p.tapMenuOption("Gallery")) return -1;
            sleep(2000);
            String label = p.libraryTabLabel("Media");
            if (label == null) return -1;
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(label);
            return mm.find() ? Integer.parseInt(mm.group(1)) : 0;
        } finally {
            p.leaveOverlayScreen();
        }
    }

    // [WEB unit-feed.suite.js:645 / :661]
    @Test
    @Order(224)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-PICKER-CANCEL] Backing out of Location and of the media preview posts nothing")
    void pickerCancelPostsNothing() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        p.hideKeyboardSafely();
        int rows = p.chatRowCount();
        String newest = lastOf(p.visibleMessages());
        p.tapAttachment();
        assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Location"), "Location option not offered");
        waitAndDismissPermissions(Duration.ofSeconds(5));
        boolean locUp = p.isLocationSendUp(Duration.ofSeconds(10));
        System.out.println(">>> " + moduleName() + " location picker up=" + locUp);
        p.returnToChat(appPackage);
        p.leaveOverlayScreen();
        sleep(1500);
        assertTrue(inModuleScreen() && p.isInputBarVisible(), "Backing out of Location should return to " + moduleName());
        assertTrue(java.util.Objects.equals(newest, lastOf(p.visibleMessages())),
                "Backing out of Location must post nothing (newest row changed)");
        // Media preview: pick a document, then back out of the preview.
        p.tapAttachment();
        assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Document"), "Document option not offered");
        waitAndDismissPermissions(Duration.ofSeconds(3));
        assumeTrue(p.pickSystemFileByName("zl_test_doc", Duration.ofSeconds(20)), "zl_test_doc.pdf not pickable");
        com.zillit.automation.pages.common.MediaPreviewPage prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
        sleep(1500);
        assumeTrue(prev.isPreviewOpen(), "This build posts a picked document without a preview, so there is no preview to back out of");
        prev.tapPreviewBack();
        prev.confirmExitIfPrompted();
        sleep(1500);
        p.leaveOverlayScreen();
        assertTrue(java.util.Objects.equals(newest, lastOf(p.visibleMessages())) && p.chatRowCount() <= rows + 0,
                "Backing out of the preview must post nothing");
    }

    /**
     * MediaPreviewPage.setCaption closes the keyboard with BACK; when the IME was already
     * gone that BACK reaches the preview, which asks "Are sure you want to exit?". Stay.
     */
    private void keepPreview() {
        com.zillit.automation.pages.tools.InfoFeedPage p = new com.zillit.automation.pages.tools.InfoFeedPage(driver);
        if (p.textShown("want to exit", Duration.ofSeconds(1))) {
            p.tapExactText("No");
            sleep(800);
        }
    }

    /** Leaves a media preview left open by a failed step, discarding it ("exit? Yes"). */
    private void exitPreview() {
        com.zillit.automation.pages.tools.InfoFeedPage p = new com.zillit.automation.pages.tools.InfoFeedPage(driver);
        for (int i = 0; i < 2; i++) {
            String a = p.currentActivity();
            if (a == null || !a.contains("GalleryItemView")) return;
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            if (p.textShown("want to exit", Duration.ofSeconds(2))) p.tapExactText("Yes");
        }
    }

    /** Brings the newest row into view (the feed can sit above it after a reopen). */
    private void toNewest(com.zillit.automation.pages.tools.InfoFeedPage p) {
        p.hideKeyboardSafely();
        if (p.isArrowDownShown()) { p.tap("arrowDownCard"); sleep(1200); }
        p.scrollNewer(2);
    }

    private static String lastOf(List<String> l) { return l.isEmpty() ? null : l.get(l.size() - 1); }

    // [WEB unit-feed.suite.js:686] [INFO-26 / CONF-15]
    @Test
    @Order(225)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-PDFJS] INFO-26/CONF-15: a PDF carrying JavaScript is refused")
    void pdfWithJavascriptRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String name = "zl_auto_js_" + (System.currentTimeMillis() % 100000) + ".pdf";
        String path = "/sdcard/Documents/" + name;
        try {
            driver.pushFile(path, jsPdf());
        } catch (Exception e) {
            assumeTrue(false, "Could not push the JavaScript PDF fixture: " + e.getMessage());
        }
        try {
            toNewest(p);
            int docsBeforeOpen = p.docBubbleCountByBadge();
            p.tapAttachment();
            assumeTrue(p.isAttachmentPickerVisible() && p.tapAttachmentOption("Document"), "Document option not offered");
            waitAndDismissPermissions(Duration.ofSeconds(3));
            int docsBefore = docsBeforeOpen;   // measured with the feed at its newest row
            assumeTrue(p.pickSystemFileByName(name.replace(".pdf", ""), Duration.ofSeconds(25)), "The pushed fixture was not pickable");
            com.zillit.automation.pages.common.MediaPreviewPage prev = new com.zillit.automation.pages.common.MediaPreviewPage(driver);
            String cap = autoMarker("pdfjs");
            boolean preview = prev.isPreviewOpen();
            if (preview) {
                prev.setCaption(cap); keepPreview();
                prev.tapPreviewSend();
                prev.waitPreviewClosed(Duration.ofSeconds(45));
                autoPosted.add(cap);
            }
            confirmPopupAccept(Duration.ofSeconds(2));
            long end = System.currentTimeMillis() + 25000;
            boolean landed = false;
            while (System.currentTimeMillis() < end && !landed) {
                toNewest(p);
                landed = p.docBubbleCountByBadge() > docsBefore || (preview && p.messageShown(cap, Duration.ofSeconds(1)));
                if (!landed) sleep(1000);
            }
            boolean posted = landed && p.waitAttachmentSent(Duration.ofSeconds(45));
            System.out.println(">>> " + moduleName() + " JS PDF: preview=" + preview + " landed=" + landed + " posted=" + posted);
            p.leaveOverlayScreen();
            assertTrue(!posted, "INFO-26/CONF-15: a PDF with an /OpenAction JavaScript was posted; it should be refused");
        } finally {
            try { driver.executeScript("mobile: shell", java.util.Map.of("command", "rm", "args", List.of("-f", path))); }
            catch (Exception ignored) { }
        }
    }

    /** A minimal, valid one-page PDF whose catalog runs JavaScript on open. */
    private static byte[] jsPdf() {
        String[] objs = {
                "<< /Type /Catalog /Pages 2 0 R /OpenAction 4 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >>",
                "<< /S /JavaScript /JS (app.alert\\('ZL_AUTO'\\);) >>"};
        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        int[] off = new int[objs.length];
        for (int i = 0; i < objs.length; i++) {
            off[i] = sb.length();
            sb.append(i + 1).append(" 0 obj\n").append(objs[i]).append("\nendobj\n");
        }
        int xref = sb.length();
        sb.append("xref\n0 ").append(objs.length + 1).append("\n0000000000 65535 f \n");
        for (int o : off) sb.append(String.format("%010d 00000 n \n", o));
        sb.append("trailer\n<< /Size ").append(objs.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    // [WEB unit-feed.suite.js:1681] deleted PDF in History offers Download/View
    @Test
    @Order(226)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-HISTORY] A deleted document is listed in History with Download/View")
    void deletedDocumentInHistoryOffersDownloadView() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        toNewest(p);
        // Identify the document by its caption (captions render under document bubbles).
        String cap = pdfCaption;
        assumeTrue(cap != null, "Needs the captioned PDF from uploadDocumentsWithCaption");
        assertTrue(p.messageShownScrolled(cap, 10), "The captioned PDF '" + cap + "' is no longer in the feed");
        assumeTrue(p.longPressMessage(cap), "Long-press on the PDF caption failed");
        assumeTrue(p.tapMenuOption("Delete"), "Delete not offered");
        p.tapDeleteActionBar(Duration.ofSeconds(6));
        assumeTrue(confirmYesDialog(Duration.ofSeconds(8)), "Delete confirmation did not appear");
        sleep(2500);
        autoPosted.remove(cap);
        pdfCaption = null;
        assumeTrue(!p.messageShown(cap, Duration.ofSeconds(2)), "The document was not deleted");
        assertTrue(p.openHistory(), "History did not open");
        try {
            sleep(2500);
            assertTrue(p.historyShows(cap, 4), "The deleted document '" + cap + "' should be listed in History");
            screenshot("tool-" + slug() + "-history-doc");
            assertTrue(p.longPressMessage(cap), "Long-press on the History row did not open its menu");
            java.util.Set<String> got = optionsOf(p);
            assertTrue(got.contains("download") || got.contains("view"),
                    "A deleted document in History should offer Download/View. Options: " + got);
        } finally {
            p.leaveOverlayScreen();
            if (!inModuleScreen()) reopenModule();
        }
    }

    // ---------------- batch 2: forward, paging, FAB overlap, links, composer, menu dismiss ----------------

    /** Long-press → Forward → confirm; true once ForwardPage is up. */
    private boolean openForward(com.zillit.automation.pages.tools.InfoFeedPage p, String m) {
        return p.forwardOpensPage(m);
    }

    // [WEB unit-feed.suite.js:1290]
    @Test
    @Order(230)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-FORWARD] Forwarding a message into the same tool posts a second copy")
    void forwardIntoSameToolCopies() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "fwdcopy");
        assumeTrue(m != null, "Marker did not post — skipping");
        assertTrue(openForward(p, m), "Forward should open the destination list");
        boolean tapped = p.tapExactText(moduleName());
        System.out.println(">>> " + moduleName() + " forward → '" + moduleName() + "' tapped=" + tapped);
        if (!tapped) p.leaveOverlayScreen();
        assertTrue(tapped, "ForwardPage should list '" + moduleName() + "' as a destination");
        sleep(3000);
        p.leaveOverlayScreen();
        if (!inModuleScreen()) reopenModule();
        p.hideKeyboardSafely();
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline && p.exactTextCount(m) < 2) sleep(1000);
        int n = p.exactTextCount(m);
        assertTrue(n == 2, "Forwarding into " + moduleName() + " should leave two copies of the message, found " + n);
    }

    // [WEB unit-feed.suite.js:1262] [INFO-02 / CONF-02]
    @Test
    @Order(231)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-FORWARD] Forward → Home lists the Home units incl. Bulletin, without Calendar")
    void forwardHomeListsUnits() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "fwdhome");
        assumeTrue(m != null, "Marker did not post — skipping");
        assertTrue(openForward(p, m), "Forward should open the destination list");
        List<String> units;
        try {
            assertTrue(p.tapExactText("Home"), "ForwardPage should offer Home");
            sleep(3000);
            units = p.allTexts();
            System.out.println(">>> " + moduleName() + " forward → Home lists: " + units);
            screenshot("tool-" + slug() + "-forward-home");
        } finally {
            p.leaveOverlayScreen();
            if (!inModuleScreen()) reopenModule();
        }
        assertTrue(units.stream().anyMatch(u -> u.trim().equalsIgnoreCase("Bulletin")), "Home should list Bulletin. Seen: " + units);
        assertTrue(units.stream().noneMatch(u -> u.trim().equalsIgnoreCase("Calendar")),
                "Home must not list Calendar as a forward target. Seen: " + units);
        // Platform difference (web skips this case): Android lists Call Sheet for TEXT and
        // documents and hides it only for image/video/audio (ForwardPage unitList collector).
        System.out.println(">>> " + moduleName() + " Call Sheet offered for a text forward: "
                + units.stream().anyMatch(u -> u.trim().equalsIgnoreCase("Call Sheet")));
    }

    // [WEB unit-feed.suite.js:1316]
    @Test
    @Order(232)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-FORWARD] Forward → Remote Projects opens 'Select Remote Project' or says none exist; nothing is sent")
    void forwardRemoteProjects() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "fwdremote");
        assumeTrue(m != null, "Marker did not post — skipping");
        assertTrue(openForward(p, m), "Forward should open the destination list");
        boolean listed, outcome = false;
        try {
            listed = p.tapExactText("Remote Projects");
            if (listed) {
                // The "none yet" toast lasts ~2 s, so poll for either outcome together.
                long end = System.currentTimeMillis() + 10000;
                while (!outcome && System.currentTimeMillis() < end) {
                    outcome = p.toastShown("Remote Project Not Created", Duration.ofMillis(600))
                            || p.textShown("Select Remote Project", Duration.ofMillis(600))
                            || p.textShown("SELECT REMOTE PROJECT", Duration.ofMillis(300));
                }
                System.out.println(">>> " + moduleName() + " Remote Projects outcome=" + outcome
                        + " activity=" + p.currentActivity() + " texts=" + p.allTexts());
                screenshot("tool-" + slug() + "-forward-remote");
            }
        } finally {
            p.leaveOverlayScreen();
            if (!inModuleScreen()) reopenModule();
        }
        assumeTrue(listed, "Remote Projects is offered only on Entertainment projects");
        assertTrue(outcome, "Remote Projects should open 'Select Remote Project' or say no remote project exists");
        assertTrue(p.exactTextCount(m) == 1, "Backing out must not forward anything");
    }

    // [WEB unit-feed.suite.js:1603]
    @Test
    @Order(105)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-PAGING] Scrolling to the top loads older pages")
    void olderPageLoads() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        p.hideKeyboardSafely();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(p.visibleMessages());
        int stale = 0;
        for (int i = 0; i < 60 && stale < 5; i++) {
            p.scrollOlder(1);
            int before = seen.size();
            seen.addAll(p.visibleMessages());
            stale = seen.size() == before ? stale + 1 : 0;
            if (stale > 0) sleep(1500);   // a page may still be loading
        }
        System.out.println(">>> " + moduleName() + " distinct rows seen scrolling up: " + seen.size());
        try { p.tap("arrowDownCard"); } catch (Exception ignored) { }
        sleep(1500);
        // The first page holds 50 rows (API_PAGINATION_LIMIT): fewer than that reachable means
        // the whole feed fits in one page, so there is no older page to load.
        assumeTrue(seen.size() >= 50, "The feed holds " + seen.size() + " rows, fewer than one page (50), so there is no older page to load");
        assertTrue(seen.size() > 50, "Older pages should load when scrolling up: only " + seen.size()
                + " distinct rows were reachable (page size 50)");
    }

    // [WEB sheet-bugs INFO-11]
    @Test
    @Order(106)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-FAB] INFO-11: the floating History button does not cover the newest message")
    void fabDoesNotCoverNewestRow() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "fabcover-" + repeatWords(60).replace(' ', '-'));
        assumeTrue(m != null, "Marker did not post — skipping");
        p.hideKeyboardSafely();
        sleep(1000);
        org.openqa.selenium.Rectangle row = p.messageRect(m.substring(0, 30));
        org.openqa.selenium.Rectangle hist = p.rectOf("userHistoryBtn");
        org.openqa.selenium.Rectangle down = p.rectOf("arrowDownCard");
        System.out.println(">>> " + moduleName() + " newest row " + rectStr(row) + " history " + rectStr(hist) + " arrow " + rectStr(down));
        assumeTrue(row != null && hist != null, "Could not read the row or the History button bounds");
        assertTrue(!com.zillit.automation.pages.tools.InfoFeedPage.overlaps(row, hist)
                        && !com.zillit.automation.pages.tools.InfoFeedPage.overlaps(row, down),
                "INFO-11: the floating button covers the newest message's text");
    }

    private static String rectStr(org.openqa.selenium.Rectangle r) {
        return r == null ? "null" : "[" + r.getX() + "," + r.getY() + " " + r.getWidth() + "x" + r.getHeight() + "]";
    }

    // [WEB unit-feed.suite.js:355]
    @Test
    @Order(174)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-RENDER] A posted URL is a live link that opens the browser")
    void urlIsLinked() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String url = "https://example.com/zl-auto-" + (System.currentTimeMillis() % 1000000);
        p.sendMessage(url);
        autoPosted.add(url);
        p.hideKeyboardSafely();
        assumeTrue(p.waitMessageSent(url, Duration.ofSeconds(30)), "The URL message did not post");
        org.openqa.selenium.Rectangle r = p.messageRect(url);
        assumeTrue(r != null, "Row not found");
        p.tapAt(r.getX() + Math.min(r.getWidth() / 3, 120), r.getY() + r.getHeight() / 2);
        String pkg = null;
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            pkg = p.safeCurrentPackage();
            if (pkg != null && !pkg.equals(appPackage)) break;
            sleep(500);
        }
        System.out.println(">>> " + moduleName() + " URL tap → " + pkg);
        p.returnToChat(appPackage);
        if (!appPackage.equals(p.safeCurrentPackage())) {
            driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
            sleep(1500);
        }
        p.leaveOverlayScreen();
        assertTrue(pkg != null && !pkg.equals(appPackage), "Tapping a URL should open it outside the app (autoLink)");
    }

    // [WEB unit-feed.suite.js:408]
    @Test
    @Order(196)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-COMPOSER] Typing '@' opens no mention picker")
    void atSignOpensNoMentionPicker() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        try {
            p.typeMessage("@");
            sleep(1500);
            assertTrue("@".equals(p.composerText()), "The composer should hold '@', was '" + p.composerText() + "'");
            assertTrue(!p.isMenuOpen(Duration.ofSeconds(1)) && inModuleScreen(), "Typing '@' must not open a picker or leave the feed");
        } finally {
            p.clearMessage();
        }
    }

    // [WEB unit-feed.suite.js:819]
    @Test
    @Order(197)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-MENU-DISMISS] The options menu closes on a tap outside it")
    void menuClosesOnOutsideTap() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        String m = postAuto(p, "dismiss");
        assumeTrue(m != null, "Marker did not post — skipping");
        assumeTrue(p.longPressMessage(m), "Long-press did not open the menu");
        org.openqa.selenium.Rectangle menu = p.rectOf("rvVw");
        org.openqa.selenium.Rectangle bar = p.rectOf("toolbar");
        int y = bar != null ? bar.getY() + bar.getHeight() + 30 : 400;
        if (menu != null && y >= menu.getY() && y <= menu.getY() + menu.getHeight()) y = menu.getY() + menu.getHeight() + 40;
        p.tapAt(20, y);
        sleep(1200);
        boolean closed = !p.isMenuOpen(Duration.ofSeconds(1));
        if (!closed) p.closeMenu();
        assertTrue(closed, "Tapping outside the options menu should close it");
        assertTrue(inModuleScreen(), "The outside tap must not leave " + moduleName());
    }

    // ---------------- G-OFFLINE ----------------
    //
    // Only on an EMULATOR: toggling Wi-Fi/data on the shared Pixel would cut its network
    // for whoever uses it next if the run died mid-case. Each case restores the network in
    // finally; the device is one this run holds the lock for.

    private boolean onEmulator() {
        String udid = System.getenv("ZILLIT_UDID");
        return udid != null && udid.startsWith("emulator-");
    }

    private boolean setConnectivity(boolean on) {
        try {
            driver.executeScript("mobile: setConnectivity", java.util.Map.of("wifi", on, "data", on));
            return true;
        } catch (Exception e) {
            System.out.println(">>> setConnectivity(" + on + ") unavailable: " + e.getMessage());
            return false;
        }
    }

    // [WEB unit-feed.suite.js:1709] [CONF-25: row stays on the clock with no Retry]
    @Test
    @Order(200)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-OFFLINE] CONF-25: a send while offline offers Retry, and is delivered once back online")
    void offlineSendShowsRetryAndResends() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        assumeTrue(onEmulator(), "Offline cases run only on an emulator (ZILLIT_UDID=emulator-*)");
        String m = autoMarker("offline");
        boolean off = false;
        boolean retryShown;
        try {
            off = setConnectivity(false);
            assumeTrue(off, "This driver would not toggle connectivity — skipping");
            sleep(3000);
            p.sendMessage(m);
            autoPosted.add(m);
            p.hideKeyboardSafely();
            long deadline = System.currentTimeMillis() + 25000;
            retryShown = false;
            while (System.currentTimeMillis() < deadline && !retryShown) {
                retryShown = p.messageFailed(m);
                sleep(1000);
            }
            System.out.println(">>> " + moduleName() + " offline send: " + p.sendStateOf(m));
            screenshot("tool-" + slug() + "-offline-send");
        } finally {
            if (off) { setConnectivity(true); sleep(6000); }
        }
        // Back online: Retry (if offered) resends; either way exactly one copy must land. The
        // emulator's Wi-Fi takes a few seconds to reconnect, so Retry is tried more than once.
        boolean delivered = false;
        for (int attempt = 0; attempt < 4 && !delivered; attempt++) {
            p.hideKeyboardSafely();
            p.scrollNewer(2);
            if (p.messageFailed(m)) {
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage + ":id/materialCardView'][.//*[@text=" + xq(m)
                                    + "]]//*[@resource-id='" + appPackage + ":id/btnTryAgain']")).click();
                    sleep(1500);
                    if (confirmPopupShown(Duration.ofSeconds(2))) confirmPopupAccept(Duration.ofSeconds(3));
                } catch (Exception ignored) { }
            }
            delivered = p.waitMessageSent(m, Duration.ofSeconds(15));
            System.out.println(">>> " + moduleName() + " retry attempt " + attempt + ": " + p.sendStateOf(m));
        }
        assertTrue(retryShown, "CONF-25: a message sent offline should turn into a failed row offering "
                + "Retry, not stay on the pending clock");
        assertTrue(delivered, "Back online, the message should be delivered — it is " + p.sendStateOf(m));
        assertTrue(p.exactTextCount(m) == 1, "Exactly one copy should be posted, found " + p.exactTextCount(m));
    }

    // [WEB unit-feed.suite.js:932] a refused delete keeps the row
    @Test
    @Order(201)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-OFFLINE] Deleting while offline is refused and the message stays")
    void offlineDeleteRefused() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        assumeTrue(onEmulator(), "Offline cases run only on an emulator (ZILLIT_UDID=emulator-*)");
        String m = postAuto(p, "offdel");
        assumeTrue(m != null, "Marker did not post — skipping");
        boolean off = false;
        boolean refused;
        try {
            assumeTrue(p.longPressMessage(m), "Long-press failed");
            assumeTrue(p.tapMenuOption("Delete"), "Delete option not present");
            assumeTrue(p.waitSelectionMode(Duration.ofSeconds(6)), "Selection mode did not start");
            off = setConnectivity(false);
            assumeTrue(off, "This driver would not toggle connectivity — skipping");
            sleep(3000);
            p.tapDeleteActionBar(Duration.ofSeconds(4));
            refused = p.messageAppears("Internet connection lost", Duration.ofSeconds(5));
            if (confirmPopupShown(Duration.ofSeconds(1))) confirmPopupCancel(Duration.ofSeconds(2));
        } finally {
            if (off) { setConnectivity(true); sleep(6000); }
            leaveOptionBar(p);
        }
        assertTrue(refused, "Delete with no connection should say 'Internet connection lost…'");
        assertTrue(p.messageShown(m, Duration.ofSeconds(3)), "A refused delete must keep the message");
    }

    // [WEB unit-feed.suite.js:932] a failing edit keeps the text
    @Test
    @Order(202)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-OFFLINE] An edit saved while offline does not lose the message")
    void offlineEditDoesNotLoseMessage() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        assumeTrue(onEmulator(), "Offline cases run only on an emulator (ZILLIT_UDID=emulator-*)");
        String m = postAuto(p, "offedit");
        assumeTrue(m != null, "Marker did not post — skipping");
        boolean off = false;
        String edited = m + "-E";
        boolean snack;
        try {
            assumeTrue(p.longPressMessage(m), "Long-press failed");
            assumeTrue(p.tapMenuOption("Edit"), "Edit not offered");
            assumeTrue(p.isEditDialogOpen(Duration.ofSeconds(8)), "Edit dialog did not open");
            off = setConnectivity(false);
            assumeTrue(off, "This driver would not toggle connectivity — skipping");
            sleep(3000);
            p.setEditDialogText(edited);
            p.saveEditDialog();
            snack = p.messageAppears("nternet", Duration.ofSeconds(4));
            if (p.visible("editTextInput", Duration.ofSeconds(1))) p.cancelEditDialog();
            System.out.println(">>> " + moduleName() + " offline edit: snackbar=" + snack + " row=" + p.messageTextContaining(m));
        } finally {
            if (off) { setConnectivity(true); sleep(8000); }
        }
        autoPosted.add(edited);
        boolean original = p.messageShown(m, Duration.ofSeconds(5));
        assertTrue(original, "After an offline edit the message must still be in the feed (original or edited text)");
        String now = p.messageTextContaining(m);
        assertTrue(m.equals(now) || edited.equals(now), "The row should read the original or the edited text, was '" + now + "'");
        if (edited.equals(now)) {
            assertTrue(p.waitMessageSent(edited, Duration.ofSeconds(30)), "A queued edit should reach the server once online");
        }
    }

    // [WEB unit-feed.suite.js:1783]
    @Test
    @Order(203)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("[G-OFFLINE] The feed opens offline without a crash and the composer stays usable")
    void feedOpensOffline() {
        com.zillit.automation.pages.tools.InfoFeedPage p = postingFeed();
        assumeTrue(onEmulator(), "Offline cases run only on an emulator (ZILLIT_UDID=emulator-*)");
        boolean off = false;
        boolean composer, inApp;
        try {
            off = setConnectivity(false);
            assumeTrue(off, "This driver would not toggle connectivity — skipping");
            sleep(3000);
            reopenModule();
            com.zillit.automation.pages.tools.InfoFeedPage again = new com.zillit.automation.pages.tools.InfoFeedPage(driver);
            inApp = appPackage.equals(again.safeCurrentPackage());
            composer = again.isInputBarVisible();
            if (composer) { again.typeMessage("x"); composer = again.isSendButtonVisible(); again.clearMessage(); }
            screenshot("tool-" + slug() + "-offline-open");
        } finally {
            if (off) { setConnectivity(true); sleep(8000); }
        }
        assertTrue(inApp, "Opening " + moduleName() + " offline must not crash the app");
        assertTrue(composer, "Offline, the composer should still accept text");
    }

    // [WEB sheet-bugs INFO-28 / CONF-26]
    @Test
    @Order(204)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @org.junit.jupiter.api.Tag("known-bug")
    @DisplayName("[G-HISTORY] INFO-28/CONF-26: History opened offline is not silently blank")
    void historyOfflineNotBlank() {
        com.zillit.automation.pages.tools.InfoFeedPage p = feed();
        assumeTrue(onEmulator(), "Offline cases run only on an emulator (ZILLIT_UDID=emulator-*)");
        boolean off = false;
        boolean rows = false, explained = false, opened = false;
        try {
            off = setConnectivity(false);
            assumeTrue(off, "This driver would not toggle connectivity — skipping");
            sleep(3000);
            opened = p.openHistory();
            sleep(5000);
            rows = !p.visibleMessages().isEmpty();
            explained = p.shownNow("emptyView") || p.messageAppears("nternet", Duration.ofSeconds(2))
                    || p.toastShown("nternet", Duration.ofSeconds(1));
            System.out.println(">>> " + moduleName() + " offline History: rows=" + rows + " explained=" + explained);
            screenshot("tool-" + slug() + "-history-offline");
        } finally {
            if (opened) { driver.navigate().back(); sleep(1200); }
            if (off) { setConnectivity(true); sleep(6000); }
        }
        assumeTrue(opened, "History did not open offline");
        assertTrue(rows || explained, "INFO-28/CONF-26: History opened offline shows neither rows nor an error/empty message");
    }

    // ---------------- clean-up ----------------

    @Test
    @Order(299)
    @org.junit.jupiter.api.condition.EnabledIf(value = "infoFeedCases", disabledReason = "Not a ConfidentialInfo.kt feed (infoFeedCases() is false)")
    @DisplayName("Clean-up: this run's ZL_AUTO messages are deleted")
    void sweepAutoMessages() {
        assumeTrue(infoFeedCases(), "No Info-feed cases ran");
        com.zillit.automation.pages.tools.InfoFeedPage p = new com.zillit.automation.pages.tools.InfoFeedPage(driver);
        assumeTrue(p.isInputBarVisible(), "No posting rights — nothing was posted");
        leaveOptionBar(p);
        List<String> left = new java.util.ArrayList<>();
        for (String m : new java.util.ArrayList<>(autoPosted)) {
            if (p.messageShown(m, Duration.ofMillis(700))) { left.add(m); break; }
        }
        assumeTrue(!left.isEmpty(), "No ZL_AUTO message from this run is on screen");
        List<String> deleted = new java.util.ArrayList<>();
        if (p.longPressMessage(left.get(0)) && p.tapMenuOption("Delete")
                && p.waitSelectionMode(Duration.ofSeconds(6))) {
            deleted.add(left.get(0));
            java.util.List<String> todo = new java.util.ArrayList<>(autoPosted);
            String first = left.get(0);
            todo.removeIf(x -> x.contains(first) || first.contains(x));
            // Selection mode survives scrolling: walk up the feed selecting this run's rows.
            for (int pass = 0; pass < 8 && !todo.isEmpty(); pass++) {
                for (String m : new java.util.ArrayList<>(todo)) {
                    if (!todo.contains(m)) continue;
                    if (p.containsTextCount(m) > 0 && p.tapMessage(m)) {
                        // An edited marker (m + "-E") is the same row: never tap it twice,
                        // or the second tap deselects it.
                        deleted.add(m);
                        todo.removeIf(x -> x.contains(m) || m.contains(x));
                    }
                }
                p.scrollOlder(1);
            }
            System.out.println(">>> " + moduleName() + " clean-up selected " + deleted.size()
                    + " (count " + p.countText() + ")");
            p.tapDeleteActionBar(Duration.ofSeconds(6));
            confirmYesDialog(Duration.ofSeconds(8));
            sleep(2500);
        }
        leaveOptionBar(p);
        // Best-effort sweep (duplicates from the Forward case and rows that were scrolled past
        // may survive); it must at least have deleted the row it started from.
        int left2 = 0;
        for (String m : deleted) if (p.containsTextCount(m) > 0) left2++;
        System.out.println(">>> " + moduleName() + " clean-up: " + left2 + " of " + deleted.size() + " selected rows still visible");
        assertTrue(!deleted.isEmpty() && p.containsTextCount(deleted.get(0)) == 0,
                "Clean-up could not delete '" + (deleted.isEmpty() ? "-" : deleted.get(0)) + "'");
    }

    private String slug() {
        return moduleName().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
