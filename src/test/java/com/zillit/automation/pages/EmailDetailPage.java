package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * Email detail / trail (EmailDetailActivity, activity_email_detail.xml).
 * toolbar (subject), rv_emails (trail), tv_subject; bottom bar:
 * btn_reply_selector, iv_reply_icon, tv_reply_label, btn_delete, btn_more.
 * Trail row: iv_more (Reply/Reply All/Forward/Move/Delete/Print/Add to Contacts),
 * web_view (body), attachment_list, iv_download.
 */
public class EmailDetailPage extends BasePage {

    public EmailDetailPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("btn_reply_selector", timeout)
                || isVisible("bottom_bar", Duration.ofSeconds(2))
                || isVisible("tv_reply_label", Duration.ofSeconds(2))
                || isVisible("btn_delete", Duration.ofSeconds(2))
                || isVisible("web_view", Duration.ofSeconds(2));
    }

    public boolean isReplyAvailable() {
        return isVisible("tv_reply_label", Duration.ofSeconds(5))
                || isVisible("btn_reply_selector", Duration.ofSeconds(3));
    }

    /** Taps the reply-action selector (Reply / Reply All / Forward menu). */
    public boolean tapReplySelector() {
        try {
            waitVisible("btn_reply_selector").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Taps the current reply action (label) → opens Compose. */
    public ComposeEmailPage tapReply() {
        waitVisible("tv_reply_label").click();
        return new ComposeEmailPage(driver);
    }

    /** Picks an action from the reply selector popup (Reply/Reply All/Forward). */
    public ComposeEmailPage pickReplyAction(String action) {
        tapReplySelector();
        sleep(500);
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + action + "\")")).click();
        } catch (WebDriverException ignored) {
        }
        return new ComposeEmailPage(driver);
    }

    public boolean isBodyShown() {
        return isVisible("web_view", Duration.ofSeconds(6))
                || isVisible("tv_body", Duration.ofSeconds(3));
    }

    public int attachmentCount() {
        try {
            return driver.findElements(byId("iv_download")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    // ---------------- delete / more ----------------

    public boolean tapDelete() {
        try {
            waitVisible("btn_delete").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Confirms the "Are you sure you want to delete?" dialog. */
    public boolean confirmDelete(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (String pat : new String[]{"yes", "delete", "ok", "confirm"}) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + pat + "\")")).click();
                    return true;
                } catch (WebDriverException ignored) {
                }
            }
            sleep(400);
        }
        return false;
    }

    public boolean tapMore() {
        try {
            waitVisible("btn_more").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isMoreMenuOption(String label, Duration timeout) {
        return textShown(label, timeout);
    }

    public void close() {
        try {
            driver.navigate().back();
        } catch (WebDriverException ignored) {
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
