package com.zillit.automation.tests.costume;

import com.zillit.automation.pages.homechat.HomeChatPage;
import io.appium.java_client.android.AndroidDriver;

import java.time.Duration;

/**
 * The Costume chats — the unit chat per tab (chat icon on WardrobePage) and the per-record
 * chat (chat icon on a record) — are {@code OtherChatsActivity}, which reuses the Home
 * chat's keyboard fragment ({@code etMessage / ivSendMessage / ivAttachment}) and row ids,
 * so {@link HomeChatPage} drives them. The attachment button is switched off for these
 * chats on purpose ({@code isShowAttachment(false)} on every path of OtherChatsActivity).
 */
class CostumeChat {

    private final HomeChatPage chat;

    CostumeChat(AndroidDriver driver) {
        this.chat = new HomeChatPage(driver);
    }

    boolean isOpen(Duration timeout) { return chat.canViewMessages(); }

    boolean inputShown() { return chat.isInputBarVisible(); }

    boolean attachmentShown() { return chat.isAttachmentVisible(); }

    void send(String text) { chat.sendMessage(text); }

    boolean waitMessage(String text, Duration timeout) { return chat.waitMessageSent(text, timeout); }

    boolean messageShown(String text) { return chat.messageShown(text, Duration.ofSeconds(2)); }

    boolean waitMessageGone(String text, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!chat.messageShown(text, Duration.ofMillis(700))) return true;
            CostumeBaseTest.sleep(500);
        }
        return false;
    }

    /** Long-press → Delete → (action-bar Delete) → Yes. */
    boolean deleteOwn(String text) {
        try {
            if (!chat.messageShown(text, Duration.ofSeconds(3))) return false;
            if (!chat.longPressMessage(text) || !chat.tapMenuOption("Delete")) { chat.closeMenu(); return false; }
            chat.tapDeleteActionBar(Duration.ofSeconds(6));
            boolean yes = chat.confirmPopup().accept(Duration.ofSeconds(6));
            CostumeBaseTest.sleep(1500);
            return yes;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
