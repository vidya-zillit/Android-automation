package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The "How are you uploading this document?" bottom sheet (ZL-21154).
 *
 * <p><b>When it appears.</b> After the documents are picked, not before. The app calls
 * {@code startUpload(uriItems)} with the chosen files already in hand and only then asks, and
 * only when {@code identifier == HOME_UNIT_CALL_IDENTIFIER && currentList.size() > 0}. So the
 * order is: attachment → document picker → pick a file → THIS sheet → Send.
 *
 * <p>That is the reverse of the popup this screen replaced, which asked first and opened the
 * picker afterwards. Anything still looking for "continuation of the existing Call Sheet" is
 * looking for a dialog the app no longer builds.
 *
 * <p>Ids come from {@code res/layout/bottomsheet_upload_mode.xml}; the dropdown rows come from
 * {@code item_replace_target.xml}.
 */
public class UploadModePage extends BasePage {

    private static final Duration SHORT = Duration.ofSeconds(4);

    public UploadModePage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- presence ----------------

    public boolean isShown(Duration timeout) {
        return isVisible("umTitle", timeout);
    }

    public boolean isGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(byId("umTitle")).isEmpty()) return true;
            sleep(300);
        }
        return driver.findElements(byId("umTitle")).isEmpty();
    }

    public String title() {
        return textOf("umTitle");
    }

    public boolean hasContinuation() { return isVisible("umContinuation", SHORT); }

    public boolean hasNew() { return isVisible("umNew", SHORT); }

    /**
     * The Replace card is hidden outright when the unit holds no eligible document
     * ({@code binding.umReplace.isVisible = targets.isNotEmpty()}), so its absence is a
     * legitimate state, not a failure.
     */
    public boolean hasReplace() { return isVisible("umReplace", SHORT); }

    // ---------------- selection ----------------

    public void chooseContinuation() { tap("umContinuation"); }

    public void chooseNew() { tap("umNew"); }

    public void chooseReplace() { tap("umReplace"); }

    /** The "Document to replace" block — revealed only by Replace. */
    public boolean isPickerVisible() {
        return isVisible("umPickerBlock", SHORT);
    }

    public boolean isPickerHidden() {
        return driver.findElements(byId("umPickerBlock")).isEmpty()
                || !driver.findElement(byId("umPickerBlock")).isDisplayed();
    }

    /**
     * Send is the whole selection state machine in one boolean: disabled with nothing chosen,
     * enabled for Continuation/New, and for Replace only once a target id is set.
     */
    public boolean isSendEnabled() {
        try {
            return waitVisible("umSend").isEnabled();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public void tapSend() { tap("umSend"); }

    public void tapCancel() { tap("umCancel"); }

    // ---------------- replace target picker ----------------

    /** Current text in the "Search documents" field — the selected file name once one is picked. */
    public String selectedTargetName() {
        return textOf("umTargetInput");
    }

    /** The "Selected: 31 Aug 2026, 19:25" line under the field; empty when nothing is selected. */
    public String selectedStamp() {
        return textOf("umTargetMeta");
    }

    public boolean isStampShown() {
        return !selectedStamp().isBlank();
    }

    /**
     * Opens the dropdown. {@code threshold = 0} and an explicit click listener mean every option
     * shows without typing, so a tap is enough.
     */
    public void openDropdown() {
        tap("umTargetInput");
        sleep(600);
    }

    /** Visible option labels, in the order the adapter lists them (newest first). */
    public List<String> dropdownNames() {
        List<String> names = new ArrayList<>();
        for (WebElement row : driver.findElements(byId("rtName"))) {
            String text = safeText(row);
            if (!text.isBlank()) names.add(text);
        }
        return names;
    }

    /** Timestamps shown beside each option. */
    public List<String> dropdownStamps() {
        List<String> stamps = new ArrayList<>();
        for (WebElement row : driver.findElements(byId("rtMeta"))) {
            stamps.add(safeText(row));
        }
        return stamps;
    }

    public int dropdownSize() {
        return driver.findElements(byId("rtName")).size();
    }

    /** Taps the first dropdown row whose label contains {@code part}, case-insensitively. */
    public boolean pickTargetContaining(String part) {
        for (WebElement row : driver.findElements(byId("rtName"))) {
            String text = safeText(row);
            if (text.toLowerCase().contains(part.toLowerCase())) {
                row.click();
                sleep(500);
                return true;
            }
        }
        return false;
    }

    /** Taps the row at {@code index} of the open dropdown. */
    public boolean pickTargetAt(int index) {
        List<WebElement> rows = driver.findElements(byId("rtName"));
        if (index < 0 || index >= rows.size()) return false;
        rows.get(index).click();
        sleep(500);
        return true;
    }

    /**
     * Types a search query into the field.
     *
     * <p>Uses {@code sendKeys} WITHOUT a trailing hideKeyboard: UiAutomator2 implements
     * hideKeyboard as a BACK press, and a BACK inside a bottom sheet dismisses the sheet —
     * taking the upload with it. The keyboard overlapping the dropdown is harmless; losing
     * the sheet is not.
     */
    public void typeSearch(String query) {
        WebElement field = waitVisible("umTargetInput");
        field.clear();
        if (!query.isEmpty()) field.sendKeys(query);
        sleep(700);
    }

    public void clearSearch() {
        waitVisible("umTargetInput").clear();
        sleep(700);
    }

    /**
     * Closes the dropdown without choosing anything, which makes the field snap back to the
     * real selection ({@code setOnDismissListener { syncSelectionToField() }}).
     */
    public void dismissDropdown() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + appPackage() + ":id/umTitle\")")).click();
        } catch (WebDriverException ignored) {
            // Title not tappable — the next assertion decides.
        }
        sleep(500);
    }

    // ---------------- the "New" confirmation ----------------

    /**
     * The extra Alert that only "New" raises: "Doing this will send all current data, posted
     * here, to the History…". Replace deliberately has no second confirmation.
     */
    public boolean isNewConfirmShown(Duration timeout) {
        return textShown("replaced with the new upload", timeout);
    }

    /** "No" on the New confirmation — aborts the upload. */
    public void confirmNo() { tap("leftButton"); }

    /** "Yes" on the New confirmation — wipes the unit and posts. */
    public void confirmYes() { tap("rightButton"); }

    // ---------------- stale target ----------------

    /** The snackbar shown when the chosen target is no longer a live document. */
    public boolean isTargetNotFoundShown(Duration timeout) {
        return textShown("no longer available", timeout);
    }

    // ---------------- internals ----------------

    private void tap(String id) {
        waitVisible(id).click();
        sleep(400);
    }

    private String textOf(String id) {
        try {
            return safeText(waitVisible(id));
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String safeText(WebElement element) {
        try {
            String text = element.getText();
            return text == null ? "" : text.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
