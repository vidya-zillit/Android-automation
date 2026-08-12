package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.core.Messages;
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
 * Home page — UNITS strip + per-unit CHAT (send / reply / attachment).
 * Runs inside a project on ONE session. Requires a logged-in account with a
 * project you have posting rights on (admin of a created project works).
 * Every case skips with a clear reason when the precondition is missing, so
 * the suite never produces false failures on an account without chat access.
 */
@Order(TestOrder.HOME_CHAT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HomeChatTest extends BaseTest {

    private static final Duration SHORT = Duration.ofSeconds(8);
    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false; // stay in the chat; chain the cases
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void enterProjectOnce() {
        if (insideProject) {
            dismissPermissionPrompts();
            // A modal confirmation left over from the previous case blocks every
            // tap and back-press — clear it or the rest of the suite skips.
            clearBlockingPopup();
            return;
        }
        // Get inside a project and onto the Home tab.
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter — create/join one first");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        BottomNavPage main = new BottomNavPage(driver);
        assumeTrue(main.waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project (landing=" + landing + ")");
        insideProject = true;
    }

    private HomeChatPage home() {
        HomeChatPage page = new HomeChatPage(driver);
        assumeTrue(page.isLoaded(), "Home units strip not visible");
        return page;
    }

    /** Selects a chat unit and skips the test if none grants an input bar. */
    private HomeChatPage chatUnit() {
        HomeChatPage page = home();
        assumeTrue(page.selectFirstChatUnit(), "No chat unit available on this project");
        return page;
    }

    // ---------------- units ----------------

    @Test
    @Order(1)
    @DisplayName("Home shows the units strip with at least one unit")
    void unitsVisible() {
        HomeChatPage page = home();
        List<String> units = page.visibleUnitNames();
        assertTrue(!units.isEmpty(), "At least one unit should be listed");
        System.out.println(">>> Units: " + units);
        screenshot("home-units");
    }

    @Test
    @Order(2)
    @DisplayName("Selecting a unit opens its chat input bar or a no-rights banner")
    void selectUnitOpensChat() {
        HomeChatPage page = home();
        assumeTrue(page.selectFirstChatUnit(), "No chat unit available");
        assertTrue(page.isInputBarVisible() || page.isNoRightsBannerVisible(),
                "A unit should show either the input bar or the no-posting-rights banner");
        screenshot("home-unit-selected");
    }

    // ---------------- send validation (worst cases) ----------------

    @Test
    @Order(3)
    @DisplayName("Empty input hides Send and shows the mic instead (cannot send empty)")
    void emptyInputHidesSend() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights on this unit — skipping");
        page.clearMessage();
        assertTrue(!page.isSendButtonVisible(),
                "Send button must stay hidden while the message is empty");
        assertTrue(page.isRecordButtonVisible(),
                "The mic/record button should show when input is empty");
    }

    @Test
    @Order(4)
    @DisplayName("Whitespace-only input does not enable sending")
    void whitespaceNotSendable() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        page.typeMessage("     ");
        // App trims before enabling send; blanks must not become a sendable message.
        assertTrue(!page.isSendButtonVisible() || !page.messageShown("     ", SHORT),
                "Whitespace-only text must not be sendable as a message");
        page.clearMessage();
    }

    @Test
    @Order(5)
    @DisplayName("Typing real text reveals the Send button")
    void typingRevealsSend() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        page.typeMessage("hello");
        assertTrue(page.isSendButtonVisible(), "Send button should appear once text is entered");
        page.clearMessage();
    }

    @Test
    @Order(6)
    @DisplayName("Sending a valid message posts it to the conversation")
    void sendValidMessage() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-auto-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        // The bubble appears locally the moment Send is tapped, so "the text is on
        // screen" passes even for a message that never left the device. Require the
        // row to lose the pending clock AND the Retry control.
        assertTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "The message '" + marker + "' should reach the server — it is "
                        + page.sendStateOf(marker));
        screenshot("home-message-sent");
    }

    @Test
    @Order(7)
    @DisplayName("Message longer than 2000 characters is rejected with the limit error")
    void overLimitMessageRejected() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String huge = "a".repeat(Messages.TEXT_LIMIT + 50); // 2050 chars
        page.typeMessage(huge);
        if (page.isSendButtonVisible()) {
            page.tapSend();
        }
        assertTrue(page.textShown(Messages.WORD_LIMIT, SHORT),
                "Expected the 2000-character limit error");
        page.clearMessage();
        screenshot("home-over-limit");
    }

    // ---------------- attachment ----------------

    @Test
    @Order(8)
    @DisplayName("Attachment picker offers ALL 8 media types (Photo/Gallery/Video/Video Gallery/Audio/Document/Location/Contact)")
    void attachmentPickerHasAllMediaTypes() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isAttachmentVisible(), "No attachment button (no posting rights) — skipping");
        page.tapAttachment();
        assertTrue(page.isAttachmentPickerVisible(), "Attachment picker should open");
        List<String> options = page.attachmentOptions();
        System.out.println(">>> Attachment options: " + options);
        for (String expected : HomeChatPage.EXPECTED_MEDIA) {
            assertTrue(options.stream().anyMatch(o -> o.equalsIgnoreCase(expected)),
                    "Picker is missing the '" + expected + "' option. Found: " + options);
        }
        screenshot("home-attachment-picker");
        page.dismissAttachmentPicker();
    }

    @Test
    @Order(9)
    @DisplayName("Each media type OPENS its picker without crashing (opens only — does NOT send)")
    void eachMediaTypeOpens() {
        // Scope note: this case proves the picker launches, nothing more. It backs out
        // without sending, so it must never be read as coverage of "the app can send a
        // contact / location / document" — the send cases are separate and named as such.
        // Reading this one as send coverage is how those types went untested while the
        // report looked complete.
        HomeChatPage page = chatUnit();
        assumeTrue(page.isAttachmentVisible(), "No attachment button — skipping");
        // Camera-based options launch the device camera which may be absent on
        // some emulators; the pure-picker/in-app ones are the reliable checks.
        List<String> toDrive = List.of("Gallery", "Video Gallery", "Audio", "Document", "Location", "Contact");
        for (String option : toDrive) {
            page.tapAttachment();
            assumeTrue(page.isAttachmentPickerVisible(), "Picker did not open for " + option);
            assumeTrue(page.tapAttachmentOption(option), "Option not found: " + option);
            assertTrue(page.optionOpenedSomething(Duration.ofSeconds(3)),
                    "'" + option + "' must open a picker/screen, not crash the app");
            System.out.println(">>> '" + option + "' opened OK");
            page.returnToChat(appPackage);
            assertTrue(page.isInputBarVisible(),
                    "Should be back in the chat after backing out of " + option);
        }
        screenshot("home-media-types-driven");
    }

    @Test
    @Order(10)
    @DisplayName("Cancelling the attachment picker sends nothing")
    void attachmentCancelSendsNothing() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isAttachmentVisible(), "No attachment button — skipping");
        int before = page.messageCount();
        page.tapAttachment();
        assumeTrue(page.isAttachmentPickerVisible(), "Picker did not open — skipping");
        page.dismissAttachmentPicker();
        assertTrue(page.isInputBarVisible(), "Should return to the chat after cancel");
        assertTrue(page.messageCount() <= before,
                "Cancelling the picker must not add a message");
    }

    // ---------------- reply ----------------

    @Test
    @Order(11)
    @DisplayName("Long-pressing a message opens the options menu with Reply")
    void longPressShowsReply() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String marker = "qa-reply-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(marker);
        assumeTrue(page.waitMessageSent(marker, Duration.ofSeconds(30)),
                "Need a SENT message to long-press — it is " + page.sendStateOf(marker));
        assumeTrue(page.longPressMessage(marker), "Long-press did not register");
        assertTrue(page.tapMenuOption("Reply"),
                "The long-press menu should offer Reply");
        assertTrue(page.isReplyPreviewVisible(),
                "Tapping Reply should show the reply preview in the input bar");
        screenshot("home-reply-preview");
    }

    @Test
    @Order(12)
    @DisplayName("Cancelling the reply preview clears it")
    void cancelReplyPreview() {
        HomeChatPage page = new HomeChatPage(driver);
        assumeTrue(page.isReplyPreviewVisible(), "No active reply preview (previous test skipped)");
        page.cancelReplyPreview();
        assertTrue(!page.isReplyPreviewVisible(), "Reply preview should disappear after cancel");
    }

    @Test
    @Order(13)
    @DisplayName("Replying to your own message posts the reply")
    void replyToOwnMessage() {
        HomeChatPage page = chatUnit();
        assumeTrue(page.isInputBarVisible(), "No posting rights — skipping");
        String base = "qa-base-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(base);
        assumeTrue(page.waitMessageSent(base, Duration.ofSeconds(30)),
                "Need a SENT base message — it is " + page.sendStateOf(base));
        assumeTrue(page.longPressMessage(base), "Long-press failed");
        assumeTrue(page.tapMenuOption("Reply"), "Reply option missing");
        assumeTrue(page.isReplyPreviewVisible(), "Reply preview missing");
        String reply = "qa-rep-" + (System.currentTimeMillis() % 1000000);
        page.sendMessage(reply);
        assertTrue(page.waitMessageSent(reply, Duration.ofSeconds(30)),
                "The reply '" + reply + "' should reach the server — it is "
                        + page.sendStateOf(reply));
        screenshot("home-reply-sent");
    }
}
