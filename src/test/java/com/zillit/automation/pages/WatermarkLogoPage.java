package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Settings → Admin Settings → "Watermark Logo of Company"
 * (header reads "UPDATE WATERMARK LOGO").
 *
 * <p>Ids confirmed by walking the screen: {@code removeWaterMarkIv} (delete),
 * {@code editWaterMarkIv} (edit), {@code imageView} inside
 * {@code watermarkHolder}, and {@code submit_btn_watermark}. The edit icon opens
 * a bottom sheet titled "Choose below" whose {@code tvLabel} rows are "Photo" and
 * "Gallery".
 *
 * <p><b>Camera and gallery are both IN-APP</b>, not system pickers — everything
 * stays inside {@code EditProjectActivity}. The camera exposes
 * {@code captureBtn}; the gallery is the Fallery picker
 * ({@code recyclerViewBuckets} → {@code recyclerViewBucketContent} →
 * {@code floatingButtonSendMedia}). Both funnel into the same preview screen,
 * whose {@code sendBtn} returns the image to the watermark form.
 *
 * <p><b>Whether a watermark exists cannot be read from the tree.</b>
 * {@code imageView} is always present and always displayed — with no watermark it
 * shows a default Zillit logo — and {@code removeWaterMarkIv} stays visible
 * either way (it hides only while an unsaved pick is pending). Byte-size is no
 * help either: the placeholder measured LARGER than a real photo. The app's own
 * answer is the only dependable one, so {@link #probeDelete} taps delete and
 * reads what comes back: a confirm dialog means a watermark exists, the snackbar
 * "No watermark available" means it does not.
 *
 * <p><b>Snackbars linger.</b> One left over from a previous action is still on
 * screen when the next one fires, so a naive read returns the OLD message —
 * that is why {@link #waitForSnackbarToClear} exists and every action that
 * expects a message calls it first.
 */
public class WatermarkLogoPage extends BasePage {

    // ---- messages, all read off the running app ----
    public static final String NO_WATERMARK = "No watermark available";
    public static final String CHOOSE_IMAGE = "Please choose an image to update watermark";
    /** Lower-case "details" and a trailing full stop — not as written in the spec. */
    public static final String PROJECT_UPDATED = "Project details have been updated successfully.";
    /** The app really does render TWO spaces after "Watermark". */
    public static final String WATERMARK_DELETED = "Watermark  has been deleted successfully.";
    public static final String REMOVE_CONFIRM = "Do you want to remove the watermark ?";

    /** What {@link #probeDelete} found. */
    public enum DeleteProbe { WATERMARK_PRESENT, NO_WATERMARK, UNKNOWN }

    public WatermarkLogoPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    sleep(1500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openWatermarkLogo() {
        return tapListRow("Watermark Logo of Company");
    }

    private boolean tapListRow(String label) {
        // "Watermark Logo of Company" sits near the bottom of a long list that
        // keeps its scroll position.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Gets to the watermark screen from wherever the app is, retrying each hop. */
    public boolean ensureOnWatermark(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isLoaded(Duration.ofMillis(800))) {
                return true;
            }
            if (isChooserOpen(Duration.ofMillis(300))) {
                dismissChooser();
                sleep(1200);
                continue;
            }
            if (isOnCamera(Duration.ofMillis(300)) || isOnPreview(Duration.ofMillis(300))
                    || isOnGallery(Duration.ofMillis(300))) {
                try {
                    driver.navigate().back();
                } catch (WebDriverException ignored) {
                }
                sleep(1500);
                continue;
            }
            if (isOnAdminSettings(Duration.ofMillis(400))) {
                openWatermarkLogo();
                sleep(2500);
                continue;
            }
            if (isOnSettingsTab(Duration.ofMillis(400))) {
                openAdminSettings();
                sleep(1500);
                continue;
            }
            if (hasBottomNav()) {
                openSettingsTab();
                sleep(1500);
                continue;
            }
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            sleep(900);
        }
        return false;
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- the watermark screen ----------------

    public boolean isLoaded(Duration timeout) {
        return headerIs("UPDATE WATERMARK LOGO", timeout);
    }

    public boolean isLoaded() {
        return isLoaded(Duration.ofSeconds(20));
    }

    public boolean tapDelete() {
        return tapById("removeWaterMarkIv");
    }

    public boolean tapEdit() {
        return tapById("editWaterMarkIv");
    }

    public boolean tapSubmit() {
        return tapById("submit_btn_watermark");
    }

    public boolean tapBack() {
        return tapById("backBtn");
    }

    /** True when the watermark image view is rendered at a sensible size. */
    public boolean imageShown() {
        try {
            List<WebElement> found = driver.findElements(byId("imageView"));
            if (found.isEmpty()) {
                return false;
            }
            Rectangle r = found.get(0).getRect();
            return r.getWidth() > 100 && r.getHeight() > 100;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Asks the APP whether a watermark exists, by tapping delete and reading the
     * response. Leaves the watermark untouched: a confirm dialog is dismissed
     * with "No".
     */
    public DeleteProbe probeDelete() {
        waitForSnackbarToClear(Duration.ofSeconds(12));
        if (!tapDelete()) {
            return DeleteProbe.UNKNOWN;
        }
        long deadline = System.currentTimeMillis() + 12_000;
        while (System.currentTimeMillis() < deadline) {
            if (isConfirmDialogShown(Duration.ofMillis(300))) {
                tapDialogNo();
                waitForSnackbarToClear(Duration.ofSeconds(8));
                return DeleteProbe.WATERMARK_PRESENT;
            }
            String s = currentSnackbar();
            if (s != null && s.contains(NO_WATERMARK)) {
                return DeleteProbe.NO_WATERMARK;
            }
            sleep(250);
        }
        return DeleteProbe.UNKNOWN;
    }

    /** Removes an existing watermark so the empty-state checks are meaningful. */
    public boolean clearWatermarkIfPresent(Duration timeout) {
        if (probeDelete() == DeleteProbe.NO_WATERMARK) {
            return true;
        }
        waitForSnackbarToClear(Duration.ofSeconds(10));
        if (!tapDelete() || !isConfirmDialogShown(Duration.ofSeconds(10))) {
            return false;
        }
        if (!tapDialogYes()) {
            return false;
        }
        if (!snackbarShown(WATERMARK_DELETED, timeout)) {
            return false;
        }
        waitForSnackbarToClear(Duration.ofSeconds(12));
        return true;
    }

    // ---------------- confirm dialog ----------------

    public boolean isConfirmDialogShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("message")).isEmpty()
                        && !driver.findElements(byId("rightButton")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    public String dialogMessage() {
        return textOf("message");
    }

    public boolean tapDialogNo() {
        return tapById("leftButton");
    }

    public boolean tapDialogYes() {
        return tapById("rightButton");
    }

    // ---------------- image source chooser ----------------

    public boolean isChooserOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("tvTitle")).isEmpty()
                        && !driver.findElements(byId("tvLabel")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(200);
        }
    }

    /** Labels offered by the "Choose below" sheet — Photo and Gallery. */
    public List<String> chooserOptions() {
        try {
            return driver.findElements(byId("tvLabel")).stream()
                    .map(this::safeText)
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public boolean chooseSource(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'tvLabel') and @text=\"" + label + "\"]")))
                    .click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void dismissChooser() {
        try {
            driver.navigate().back();
        } catch (WebDriverException ignored) {
        }
    }

    // ---------------- in-app camera ----------------

    public boolean isOnCamera(Duration timeout) {
        return presentWithin("captureBtn", timeout);
    }

    public boolean tapCapture() {
        return tapById("captureBtn");
    }

    // ---------------- in-app gallery (Fallery) ----------------

    public boolean isOnGallery(Duration timeout) {
        return presentWithin("recyclerViewBuckets", timeout)
                || presentWithin("recyclerViewBucketContent", Duration.ofMillis(300));
    }

    /** Opens the first folder in the bucket list. */
    public boolean openFirstFolder() {
        return tapXpath("(//*[contains(@resource-id,'constraintLayoutBucketItemView')])[1]");
    }

    /** Ticks the first photo in the open folder. */
    public boolean selectFirstPhoto() {
        return tapXpath("(//*[contains(@resource-id,'constraintLayoutPhotoContainer')])[1]");
    }

    /** The gallery's own send button, which appears once something is ticked. */
    public boolean tapGallerySend() {
        return tapById("floatingButtonSendMedia");
    }

    // ---------------- shared preview screen ----------------

    public boolean isOnPreview(Duration timeout) {
        return presentWithin("sendBtn", timeout);
    }

    /** Confirms the picked image and returns to the watermark form. */
    public boolean tapPreviewSend() {
        return tapById("sendBtn");
    }

    // ---------------- snackbars ----------------

    public String currentSnackbar() {
        try {
            List<WebElement> found = driver.findElements(byId("snackbar_text"));
            return found.isEmpty() ? null : safeText(found.get(0));
        } catch (WebDriverException e) {
            return null;
        }
    }

    public boolean snackbarShown(String message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            String s = currentSnackbar();
            if (s != null && s.contains(message)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    /**
     * Waits for any snackbar to disappear.
     *
     * <p>Load-bearing: a message from the previous step is still up when the next
     * action fires, and reading straight away returns that stale text instead of
     * the new one.
     */
    public boolean waitForSnackbarToClear(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (currentSnackbar() == null) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    // ---------------- helpers ----------------

    public String headerText() {
        return textOf("headerText");
    }

    private boolean headerIs(String expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (expected.equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private boolean presentWithin(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId(id)).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(900);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean tapXpath(String xpath) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(xpath)))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? "" : String.valueOf(safeText(found.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String safeText(WebElement e) {
        try {
            return e.getText();
        } catch (WebDriverException stale) {
            return "";
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
