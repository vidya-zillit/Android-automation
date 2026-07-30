package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Email Settings (GeneralSettingsActivity, email_fragment_settings.xml):
 * toolbar "Email Settings", switchConversationView, cardSignatures,
 * cardEmailGroups (admin), cardBccPresets, cardEmailForwarding, cardEmailSetup.
 * Forwarding sheet (bottom_sheet_email_forwarding.xml): etForwardEmail,
 * btnSaveForwarding, btnRemoveForwarding (email validated via Patterns.EMAIL_ADDRESS).
 */
public class EmailSettingsPage extends BasePage {

    public EmailSettingsPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("cardSignatures", timeout)
                || isVisible("switchConversationView", Duration.ofSeconds(3))
                || textShown("Email Settings", Duration.ofSeconds(3));
    }

    public boolean hasConversationToggle() {
        return isVisible("switchConversationView", Duration.ofSeconds(4));
    }

    public boolean toggleConversationView() {
        try {
            waitVisible("switchConversationView").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Current on/off state of the conversation-view switch. */
    public boolean isConversationOn() {
        try {
            return "true".equalsIgnoreCase(
                    driver.findElement(byId("switchConversationView")).getAttribute("checked"));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** BCC Presets opens the (legacy) preset screen. */
    public boolean isBccPresetsOpen(Duration timeout) {
        return textShown("Preset", timeout) || textShown("BCC", timeout) || textShown("Bcc", timeout);
    }

    /** Email Setup Externally opens the read-only IMAP/SMTP credentials sheet. */
    public boolean isEmailSetupOpen(Duration timeout) {
        return textShown("IMAP", timeout) || textShown("SMTP", timeout)
                || textShown("Password", timeout) || textShown("Server", timeout);
    }

    public boolean tapSignatures() {
        return tapCard("cardSignatures");
    }

    public boolean isEmailGroupsVisible() {   // admin-only
        return isVisible("cardEmailGroups", Duration.ofSeconds(4));
    }

    public boolean tapEmailGroups() {
        return tapCard("cardEmailGroups");
    }

    public boolean tapForwarding() {
        return tapCard("cardEmailForwarding");
    }

    public boolean tapBccPresets() {
        return tapCard("cardBccPresets");
    }

    public boolean tapEmailSetup() {
        return tapCard("cardEmailSetup");
    }

    private boolean tapCard(String id) {
        try {
            scrollToId(id);
            waitVisible(id).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- forwarding sheet ----------------

    public boolean isForwardingSheetOpen(Duration timeout) {
        return isVisible("etForwardEmail", timeout) || isVisible("btnSaveForwarding", timeout);
    }

    public void setForwardEmail(String email) {
        try {
            WebElement f = driver.findElement(byId("etForwardEmail"));
            f.click();
            f.clear();
            f.sendKeys(email);
            hideKeyboard();
        } catch (WebDriverException ignored) {
        }
    }

    public boolean tapSaveForwarding() {
        try {
            waitVisible("btnSaveForwarding").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Invalid email keeps the sheet open (save blocked). */
    public boolean isForwardingStillOpen() {
        return isVisible("etForwardEmail", Duration.ofSeconds(2));
    }
}
