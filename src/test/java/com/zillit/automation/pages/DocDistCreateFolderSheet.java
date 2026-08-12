package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * DOCUMENT DISTRIBUTION — the Create-folder sheet
 * ({@code CreateFolderBottomSheet} / bottomsheet_dd_create_folder.xml).
 *
 * <p>Fields: name, description, a date button and a stop-notifications toggle, with
 * Cancel / Create.
 *
 * <p><b>Validation here is honest, unlike most of this app.</b>
 * {@code updateCreateEnabled()} does:
 *
 * <pre>
 *   ddCreate.isEnabled = !ddName.text.isNullOrBlank() &amp;&amp; selectedDateIso.isNotBlank()
 * </pre>
 *
 * So an invalid form DISABLES the button rather than showing an invisible
 * {@code EditText.error} or a Toast. That means we can assert the button's own
 * {@code enabled} attribute — a far stronger check than Drive's "did the sheet refuse to
 * close", and it needs no guessing about error text.
 *
 * <p><b>A date is REQUIRED.</b> Name alone is not enough; anything creating a folder must
 * pick a date too, or Create stays grey forever.
 *
 * <p>⚠ The stop-notifications toggle is INERT in this build: the binding line is commented
 * out and {@code create()} always passes {@code stopNotification = false}. The row renders,
 * but toggling it changes nothing — assert its presence, never its effect.
 */
public class DocDistCreateFolderSheet extends BasePage {

    public DocDistCreateFolderSheet(AndroidDriver driver) {
        super(driver);
    }

    /**
     * Open-detection is on the sheet's UNIQUE ids ({@code dd_create_title} /
     * {@code dd_create}) — NEVER {@code dd_name}, which is also the id of every
     * library ROW (trap 3.3). Matching dd_name made "the sheet is open" true on any
     * non-empty library, and — worse — made "the sheet has closed" impossible to
     * observe after a successful create, which failed C-3 against a folder that was
     * plainly created.
     */
    public boolean waitOpen(Duration timeout) {
        return isVisible("dd_create_title", timeout) || isVisible("dd_create", Duration.ofSeconds(2));
    }

    public boolean isOpen() { return waitOpen(Duration.ofSeconds(3)); }

    /** True once the sheet is GONE — instant presence checks, no visibility waits. */
    public boolean waitClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!presentNow("dd_create_title") && !presentNow("dd_create")) return true;
            sleep(500);
        }
        return false;
    }

    private boolean presentNow(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- fields ----------------

    public boolean hasNameField()        { return isVisible("dd_name", Duration.ofSeconds(3)); }
    public boolean hasDescriptionField() { return isVisible("dd_description", Duration.ofSeconds(2)); }
    public boolean hasDateButton()       { return isVisible("dd_date_button", Duration.ofSeconds(2)); }
    public boolean hasStopNotifRow()     { return isVisible("dd_stop_notif", Duration.ofSeconds(2)); }
    public boolean hasCreateButton()     { return isVisible("dd_create", Duration.ofSeconds(2)); }
    public boolean hasCancelButton()     { return isVisible("dd_cancel", Duration.ofSeconds(2)); }

    public String title() { return textOf("dd_create_title"); }

    /**
     * What the name field currently holds. Note an EMPTY TextInputEditText reports its
     * HINT, so a reading of "Name"/"Folder name" means the field is empty, not that
     * someone typed the word.
     */
    public String nameText() { return textOf("dd_name"); }
    public String dateButtonText() { return textOf("dd_date_button"); }

    public boolean setName(String name)        { return type("dd_name", name); }
    public boolean setDescription(String text) { return type("dd_description", text); }

    /**
     * What the description field holds. An EMPTY TextInputEditText reports its HINT, so a
     * reading of "Description" means the field is empty, not that the word was typed.
     */
    public String descriptionText() { return textOf("dd_description"); }

    /** True when the field holds real content rather than its hint or nothing. */
    public boolean descriptionIs(String expected) {
        String actual = descriptionText();
        return actual != null && actual.trim().equals(expected.trim());
    }

    /**
     * Name + DESCRIPTION + date → Create, returning true only once the sheet has closed.
     *
     * <p>Separate from {@link #createNamed} because the description is optional in the app
     * and most callers do not care; C-4 does, and it needs the value typed AND read back.
     */
    public boolean createNamed(String name, String description) {
        if (!setName(name)) return false;
        if (description != null && !setDescription(description)) return false;
        if (!isCreateEnabled() && !pickDate()) return false;
        if (!isCreateEnabled()) return false;
        if (!create()) return false;
        return waitClosed(Duration.ofSeconds(12));
    }

    /** The app's own gate: name present AND a date chosen. */
    public boolean isCreateEnabled() {
        try {
            return "true".equalsIgnoreCase(
                    driver.findElement(byId("dd_create")).getAttribute("enabled"));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Opens the date picker and accepts whatever it opens on (today by default).
     *
     * <p>The picker is a Material date dialog, so its confirm button is the framework
     * positive button; a couple of label fallbacks cover themed builds.
     */
    public boolean pickDate() {
        if (!click("dd_date_button")) return false;
        sleep(600);
        for (int i = 0; i < 3; i++) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/button1\")")).click();
                sleep(500);
                return true;
            } catch (WebDriverException ignored) { }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)(ok|save|confirm|done|select)\")")).click();
                sleep(500);
                return true;
            } catch (WebDriverException ignored) { }
            sleep(600);
        }
        return false;
    }

    public boolean create() { return click("dd_create"); }
    public boolean cancel() { return click("dd_cancel"); }

    /**
     * The full happy path: name → date → Create, returning true only once the sheet has
     * actually CLOSED.
     *
     * <p>The sheet dismisses itself on success and shows a snackbar on failure, so "it
     * closed" is the app's own success signal — and it is the one thing that separates a
     * created folder from a rejected one.
     */
    public boolean createNamed(String name) {
        if (!setName(name)) return false;
        if (!isCreateEnabled() && !pickDate()) return false;
        if (!isCreateEnabled()) return false;
        if (!create()) return false;
        return waitClosed(Duration.ofSeconds(12));
    }

    // ---------------- helpers ----------------

    private boolean type(String id, String value) {
        try {
            WebElement f = driver.findElement(byId(id));
            f.click();
            f.clear();
            if (!value.isEmpty()) f.sendKeys(value);
            // settleImeSafely, NOT hideKeyboardIfShown: this is a cancelable
            // BottomSheetDialog, and the guarded hideKeyboard() can still fire a BACK
            // while the IME animates away — which dismisses the SHEET and makes every
            // later lookup report "field missing" / "Create disabled".
            settleImeSafely();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(300); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
