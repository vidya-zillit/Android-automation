package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * Tools → CATERING (CateringActivity). The chat itself is the Bulletin-style
 * Home units chat (drive it with {@link HomeChatPage}); this page covers the
 * two-sided CATERER-only affordances:
 *
 * <ul>
 *   <li>{@code addMember} — the "Select User" chip (caterer only). Opens an Alert
 *       with {@code leftButton}="All Users" (broadcast) and {@code rightButton}=
 *       "Select User" (directed to one user).</li>
 *   <li>{@code userName} — the chosen recipient label ("All Users" / a user name).</li>
 *   <li>{@code layNoMsg} — the grey hint bar shown while no recipient is chosen;
 *       the input bar is hidden until then.</li>
 *   <li>{@code createNewUnit} — the "+" that adds a meal unit (caterer only).</li>
 * </ul>
 *
 * A regular (non-caterer) user sees none of these and can always type.
 */
public class CateringPage extends BasePage {

    public CateringPage(AndroidDriver driver) {
        super(driver);
    }

    /** True when the current account is acting as a CATERER (recipient chip present). */
    public boolean isCatererMode() {
        return isVisible("addMember", Duration.ofSeconds(4));
    }

    public boolean isRecipientHintShown() {
        return isVisible("layNoMsg", Duration.ofSeconds(3));
    }

    public boolean isCreateUnitVisible() {
        return isVisible("createNewUnit", Duration.ofSeconds(3));
    }

    // ---------------- create meal unit (caterer only) ----------------
    // "+" (createNewUnit) → "ADD CATERING UNIT" screen: unitName → editTextInput,
    // submit via btnSubmit → buttonBtn ("Submit").

    public boolean openCreateUnit() {
        try { driver.findElement(byId("createNewUnit")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isCreateUnitScreenOpen(Duration timeout) {
        return textShown("ADD CATERING UNIT", timeout)
                || isVisible("unitName", Duration.ofSeconds(3));
    }

    public void setUnitName(String name) {
        try {
            var field = driver.findElement(byId("unitName")).findElement(byId("editTextInput"));
            field.click();
            field.clear();
            field.sendKeys(name);
            hideKeyboard();
        } catch (WebDriverException outer) {
            try {
                var f = driver.findElement(byId("editTextInput"));
                f.click(); f.clear(); f.sendKeys(name); hideKeyboard();
            } catch (WebDriverException ignored) { }
        }
    }

    public String unitNameText() {
        try { return driver.findElement(byId("unitName")).findElement(byId("editTextInput")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean submitUnit() {
        try {
            driver.findElement(byId("btnSubmit")).click();
            sleep(2500);
            return true;
        } catch (WebDriverException e) {
            try {
                driver.findElement(byId("buttonBtn")).click();
                sleep(2500);
                return true;
            } catch (WebDriverException e2) { return false; }
        }
    }

    /** Back out of the create-unit screen without creating anything. */
    public void cancelCreateUnit() {
        try { driver.findElement(byId("backBtn")).click(); }
        catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        }
        sleep(1200);
    }

    /** The recipient currently selected ("All Users", a user name, or "" if none). */
    public String selectedRecipient() {
        try { return driver.findElement(byId("userName")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    /** Tap the "Select User" chip to open the recipient dialog. */
    public boolean openRecipientPicker() {
        try { driver.findElement(byId("addMember")).click(); sleep(1200); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean isRecipientDialogOpen(Duration timeout) {
        return textShown("All Users", timeout) && textShown("Select User", Duration.ofSeconds(2));
    }

    /** Dialog → "All Users" (broadcast to every user + caterer). */
    public boolean chooseAllUsers() {
        try { driver.findElement(byId("leftButton")).click(); sleep(1500); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** Dialog → "Select User" (opens the user list for a directed message). */
    public boolean chooseSelectUser() {
        try { driver.findElement(byId("rightButton")).click(); sleep(1800); return true; }
        catch (WebDriverException e) { return false; }
    }

    public boolean cancelRecipientDialog() {
        try { driver.findElement(byId("btn_cancel")).click(); sleep(800); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** In the "Select User" list, pick the first user; returns their name ("" if none). */
    public String pickFirstUserFromList() {
        for (String id : new String[]{"userNameTv", "name", "nameTv", "title"}) {
            try {
                var rows = driver.findElements(byId(id));
                if (!rows.isEmpty()) {
                    String n = rows.get(0).getText();
                    rows.get(0).click();
                    sleep(1500);
                    confirmUserSelection();
                    return n == null ? "" : n;
                }
            } catch (WebDriverException ignored) {
            }
        }
        return "";
    }

    /** Some user pickers need an explicit submit/done after ticking a row. */
    private void confirmUserSelection() {
        for (String label : new String[]{"submit", "done", "ok", "save", "select"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().clickable(true).textMatches(\"(?i)" + label + "\")")).click();
                sleep(1000);
                return;
            } catch (WebDriverException ignored) {
            }
        }
    }

    /**
     * Makes the chat postable regardless of role: a caterer must pick a recipient
     * before the keyboard appears, a regular user can always type. Returns true if
     * the input bar is usable afterwards.
     */
    public boolean ensurePostable() {
        HomeChatPage chat = new HomeChatPage(driver);
        if (chat.isInputBarVisible()) return true;
        if (!isCatererMode()) return false;
        if (!openRecipientPicker()) return false;
        if (!isRecipientDialogOpen(Duration.ofSeconds(6))) return false;
        chooseAllUsers();
        return chat.isInputBarVisible();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
