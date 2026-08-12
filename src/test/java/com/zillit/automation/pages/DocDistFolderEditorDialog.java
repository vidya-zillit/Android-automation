package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * DOCUMENT DISTRIBUTION — the folder EDIT dialog (folder row ⋮ → "Edit").
 *
 * <p><b>This is NOT the create-folder sheet, and assuming it was cost two failing
 * cases.</b> Creating inflates {@code bottomsheet_dd_create_folder} (ids {@code dd_name},
 * {@code dd_description}, {@code dd_create}); editing calls
 * {@code LibraryFragment.promptEditFolder()}, which inflates a DIFFERENT layout,
 * {@code dialog_dd_folder_editor}, inside a {@code MaterialAlertDialogBuilder}:
 *
 * <pre>
 *   create sheet   dd_create_title · dd_name · dd_description · dd_date_button · dd_create
 *   edit dialog    dd_folder_name  · dd_folder_description · framework Save / Cancel
 * </pre>
 *
 * <p>So a page object built for the sheet can never see the edit dialog — it reported
 * "the edit sheet did not open" while the dialog was open and perfectly usable, with the
 * name and description already filled in. (The menu id is still {@code dd_folder_rename}
 * for back-compat; the label became "Edit" when ZL-19534 made it update the description
 * too.)
 *
 * <p>Two app rules worth knowing before writing a case against it:
 * <ul>
 *   <li><b>Save is a no-op when nothing changed.</b> The positive button returns early
 *       unless the name or the description actually differs — so "tap Save" is not proof
 *       of an update.</li>
 *   <li><b>A blank name is rejected silently</b> — {@code if (name.isBlank()) return},
 *       with no error text. The dialog simply stays open.</li>
 * </ul>
 */
public class DocDistFolderEditorDialog extends BasePage {

    /** Framework AlertDialog buttons — this is a MaterialAlertDialog, not a bottom sheet. */
    private static final String SAVE = "android:id/button1";
    private static final String CANCEL = "android:id/button2";

    public DocDistFolderEditorDialog(AndroidDriver driver) {
        super(driver);
    }

    public boolean waitOpen(Duration timeout) {
        return isVisible("dd_folder_name", timeout);
    }

    public boolean isOpen() { return presentNow("dd_folder_name"); }

    public boolean waitClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!presentNow("dd_folder_name")) return true;
            sleep(300);
        }
        return false;
    }

    public String nameText()        { return textOf("dd_folder_name"); }
    public String descriptionText() { return textOf("dd_folder_description"); }

    public boolean setName(String v)        { return type("dd_folder_name", v); }
    public boolean setDescription(String v) { return type("dd_folder_description", v); }

    /** True when the field holds this exact content (an empty field reports its hint). */
    public boolean descriptionIs(String expected) {
        String actual = descriptionText();
        return actual != null && actual.trim().equals(expected.trim());
    }

    public boolean nameIs(String expected) {
        String actual = nameText();
        return actual != null && actual.trim().equals(expected.trim());
    }

    /**
     * Taps Save.
     *
     * <p>Remember the app's early return: with neither the name nor the description
     * changed, this closes the dialog having done nothing. Assert the OUTCOME (the row
     * renamed, the description re-read) rather than the tap.
     */
    public boolean save() { return clickFramework(SAVE); }

    public boolean cancel() { return clickFramework(CANCEL); }

    public boolean saveAndWaitClosed(Duration timeout) {
        return save() && waitClosed(timeout);
    }

    /** Leaves the dialog however it can — for a failed case's finally block. */
    public void dismissQuietly() {
        if (!isOpen()) return;
        if (cancel()) { waitClosed(Duration.ofSeconds(4)); return; }
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        sleep(500);
    }

    // ---------------- helpers ----------------

    private boolean presentNow(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean type(String id, String value) {
        try {
            WebElement f = driver.findElement(byId(id));
            f.click();
            f.clear();
            if (!value.isEmpty()) f.sendKeys(value);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean clickFramework(String resourceId) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + resourceId + "\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
