package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The DRIVE UPLOAD pipeline, which is four screens deep and crosses out of the app:
 *
 * <pre>
 *   FAB → Upload File
 *     → in-app SOURCE sheet   (Photo / Gallery / Video Gallery / Document / Audio)
 *     → SYSTEM picker         (com.google.android.documentsui — a different package!)
 *     → GalleryItemView       (shared chat media preview: caption + send)
 *     → UploadStagingSheet    (destination + file permissions + Upload)
 *     → snackbar "Uploading N file(s) to Drive…"
 * </pre>
 *
 * <p>Two things bite here. The picker belongs to <b>another package</b>, so
 * {@code byId()} (which prefixes the app package) cannot find anything in it — match
 * on text instead. And an uploaded FILE lands under the <b>Files</b> kind tab, while
 * Drive opens on <b>Folders</b>: right after uploading, the list still says "No folders
 * yet" and the file looks lost until you switch tabs.
 */
public class DriveUploadPage extends BasePage {

    public DriveUploadPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- 1. source sheet ----------------

    public boolean isSourceSheetOpen() {
        return isVisible("rvItems", Duration.ofSeconds(6))
                || textShown("Choose below", Duration.ofSeconds(2));
    }

    public List<String> sourceOptions() {
        try {
            return driver.findElements(byId("tvLabel")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /** Photo · Gallery · Video Gallery · Document · Audio */
    public boolean chooseSource(String label) {
        try {
            for (WebElement e : driver.findElements(byId("tvLabel"))) {
                String t = e.getText();
                if (t != null && t.equalsIgnoreCase(label)) { e.click(); return true; }
            }
        } catch (WebDriverException ignored) { }
        return tapText(label);
    }

    // ---------------- 2. system picker (another package) ----------------

    public boolean isSystemPickerOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && pkg.contains("documentsui")) return true;
            } catch (WebDriverException ignored) { }
            sleep(500);
        }
        return false;
    }

    /**
     * Picks a file by NAME in the system picker. The picker is a foreign package, so
     * this matches on visible text — never on a resource id.
     */
    public boolean pickFileNamed(String fileName, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        boolean browsed = false;
        while (System.currentTimeMillis() < deadline) {
            if (tapText(fileName)) return true;

            // The picker opens on RECENT, which only lists recently-touched files — an
            // adb-pushed fixture usually is NOT there, and every Drive fixture lives in
            // /sdcard/Download. So browse to Downloads immediately after the first miss
            // rather than halfway through the budget: waiting burned most of the timeout
            // on scrollIntoView calls against the Recent grid, and the pick then timed
            // out with the file plainly on screen.
            if (!browsed) {
                browsed = true;
                openDownloadsRoot();
                continue;
            }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))"
                                + ".scrollIntoView(new UiSelector().textContains(\"" + fileName + "\"))"));
            } catch (WebDriverException ignored) { }
            sleep(700);
        }
        return false;
    }

    /** Opens the picker's roots drawer and selects Downloads. */
    private void openDownloadsRoot() {
        try {
            driver.findElement(AppiumBy.accessibilityId("Show roots")).click();
            sleep(1500);
        } catch (WebDriverException e) {
            try {   // fall back to the hamburger by description
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"Show roots\")")).click();
                sleep(1500);
            } catch (WebDriverException ignored) { }
        }
        for (String root : new String[]{"Downloads", "Download"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + root + "\")")).click();
                sleep(2000);
                return;
            } catch (WebDriverException ignored) { }
        }
    }

    // ---------------- 3. media preview (shared with chat) ----------------

    public boolean isPreviewOpen(Duration timeout) {
        return isVisible("sendBtn", timeout) || isVisible("descVw", Duration.ofSeconds(2));
    }

    public boolean setCaption(String text) {
        try {
            WebElement el = driver.findElement(byId("description"));
            el.click(); el.sendKeys(text);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean sendFromPreview() { return click("sendBtn"); }
    public boolean cancelPreview()   { return click("cancel"); }

    // ---------------- 4. staging sheet ----------------

    public boolean isStagingOpen(Duration timeout) {
        return isVisible("btnUpload", timeout) || isVisible("tvDestination", Duration.ofSeconds(2));
    }

    public String destination()      { return text("tvDestination"); }
    public String filesHeader()      { return text("tvFilesHeader"); }
    public boolean hasPermissionModes() { return isVisible("rgPermissionMode", Duration.ofSeconds(3)); }
    public boolean chooseProjectUsers()  { return click("rbModeProject"); }
    public boolean chooseSpecificUsers() { return click("rbModeSpecific"); }
    public boolean hasDestinationPicker() {
        return isVisible("drive_pick_seg_new", Duration.ofSeconds(3))
                || isVisible("drive_pick_seg_existing", Duration.ofSeconds(2));
    }

    public boolean upload() { return click("btnUpload"); }
    public boolean cancelStaging() { return click("btnCancel"); }

    /** The post-upload snackbar ("Uploading N file(s) to Drive…"). */
    public boolean uploadStarted(Duration timeout) {
        return textShown("Uploading", timeout) || textShown("upload", Duration.ofSeconds(2));
    }

    // ---------------- whole-folder upload ----------------

    public boolean isFolderStagingOpen(Duration timeout) {
        return textShown("folders", timeout) || isVisible("btnUpload", Duration.ofSeconds(3));
    }

    /** "N folders · M files · size" summary on the folder-upload staging sheet. */
    public String folderSummary() {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"folders\")")).getText();
        } catch (WebDriverException e) { return ""; }
    }

    /**
     * Drives the whole pipeline for a single file, from the already-open FAB menu to a
     * started upload. Returns false at the first step that does not appear, so a caller
     * can report exactly where the flow broke.
     */
    public boolean uploadFile(String source, String fileName) {
        return uploadFile(source, fileName, Duration.ofSeconds(45));
    }

    /**
     * Same pipeline with an explicit budget for the PICK step — the slow part, because
     * it may have to open the roots drawer and browse to Downloads before the file is
     * even on screen.
     */
    public boolean uploadFile(String source, String fileName, Duration pickTimeout) {
        if (!isSourceSheetOpen()) return false;
        if (!chooseSource(source)) return false;
        if (!isSystemPickerOpen(Duration.ofSeconds(20))) return false;
        if (!pickFileNamed(fileName, pickTimeout)) return false;
        if (!isPreviewOpen(Duration.ofSeconds(20))) return false;
        if (!sendFromPreview()) return false;
        if (!isStagingOpen(Duration.ofSeconds(20))) return false;
        return upload();
    }

    // ---------------- helpers ----------------

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    private boolean tapText(String t) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + t + "\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
