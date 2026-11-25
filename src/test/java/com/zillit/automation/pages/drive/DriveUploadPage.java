package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
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
        int step = 0;
        while (System.currentTimeMillis() < deadline) {
            if (tapPickerTitle(fileName)) return true;
            // The picker opens on RECENT, which rarely lists an adb-pushed fixture. In order:
            //   1. the picker's own SEARCH — finds the file wherever it is, one screen;
            //   2. the Downloads root (where every Drive fixture lives);
            //   3. scrolling whatever list is showing.
            // Search came first after build 32539's picker kept opening on Recent and the
            // roots drawer tap silently missed, which skipped every upload-dependent case.
            if (step == 0) { step++; searchPicker(fileName); continue; }
            if (step == 1) { step++; openDownloadsRoot(); continue; }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))"
                                + ".scrollIntoView(new UiSelector().textContains(\"" + fileName + "\"))"));
            } catch (WebDriverException ignored) { }
            sleep(700);
        }
        return false;
    }

    /** Taps a picker ROW title equal to the name — never the search box holding the same text. */
    private boolean tapPickerTitle(String fileName) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + fileName + "\")"))) {
                String cls = e.getAttribute("className");
                if (cls != null && (cls.contains("EditText") || cls.contains("AutoComplete"))) continue;
                e.click();
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** DocumentsUI search: the toolbar's Search action, type the name, submit. */
    private void searchPicker(String fileName) {
        try {
            // The toolbar Search action: by description, else by DocumentsUI's own id (the
            // description is missing when the picker opens scrolled, e.g. in Documents).
            List<WebElement> s1 = driver.findElements(AppiumBy.accessibilityId("Search"));
            if (s1.isEmpty()) s1 = driver.findElements(AppiumBy.id("com.google.android.documentsui:id/option_menu_search"));
            if (s1.isEmpty()) return;
            s1.get(0).click();
            sleep(1000);
            WebElement box = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.AutoCompleteTextView\")"));
            box.sendKeys(fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName);
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.ENTER));
            sleep(2500);
        } catch (WebDriverException ignored) { }
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

    /**
     * Renames a staged file before upload — the staging sheet lists each file as an EditText
     * holding its name. Used to upload a fixture under a run-unique {@code QA_} name, so a test
     * can edit and delete its own copy instead of the shared fixture.
     */
    public boolean renameStaged(String currentName, String newName) {
        try {
            for (WebElement e : driver.findElements(org.openqa.selenium.By.className("android.widget.EditText"))) {
                String t = e.getText();
                if (t != null && t.trim().equals(currentName)) {
                    e.clear();
                    e.sendKeys(newName);
                    hideKeyboardIfShown();
                    sleep(500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Whole pipeline, uploading {@code fileName} from the picker under {@code uploadAs}. */
    /** Which step the last {@link #uploadFileAs} stopped at (printed by the callers' skip reasons). */
    public static volatile String lastUploadProblem = "";

    public boolean uploadFileAs(String source, String fileName, String uploadAs) {
        lastUploadProblem = "";
        if (!isSourceSheetOpen()) return problem("the upload source sheet did not open");
        if (!chooseSource(source)) return problem("no '" + source + "' source");
        if (!isSystemPickerOpen(Duration.ofSeconds(20))) return problem("the system picker did not open");
        if (!pickFileNamed(fileName, Duration.ofSeconds(45))) return problem(fileName + " not found in the picker");
        // Some types go straight to staging without the caption preview — accept either.
        boolean preview = false, staging = false;
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline && !preview && !staging) {
            preview = isVisible("sendBtn", Duration.ofSeconds(1));
            staging = !preview && isVisible("btnUpload", Duration.ofSeconds(1));
        }
        if (!preview && !staging) return problem("neither the preview nor the staging sheet opened after picking");
        if (preview && !sendFromPreview()) return problem("preview Send failed");
        if (!isStagingOpen(Duration.ofSeconds(20))) return problem("the staging sheet did not open");
        if (!renameStaged(fileName, uploadAs)) return problem("the staged name field for " + fileName + " was not found");
        return upload() || problem("Upload tap failed");
    }

    private static boolean problem(String why) {
        lastUploadProblem = why;
        System.out.println(">>> upload stopped: " + why);
        return false;
    }

    /**
     * Staging "File Permissions" → Specific Users → tick {@code user} → set their level
     * ("View" / "Download" / "Edit", the row Spinner UploadStagingBottomSheet builds).
     */
    public boolean grantSpecificUser(String user, String level) {
        try {
            if (!chooseSpecificUsers()) return false;
            sleep(1200);
            WebElement search = driver.findElement(byId("etSearchMembers"));
            search.click(); search.clear(); search.sendKeys(user);
            hideKeyboardIfShown();
            sleep(2500);
            List<WebElement> boxes = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/rvMembers']//android.widget.CheckBox"));
            if (boxes.isEmpty()) return false;
            boxes.get(0).click();
            sleep(1500);
            List<WebElement> spinners = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage()
                    + ":id/rvMembers']//android.widget.Spinner"));
            if (spinners.isEmpty()) return false;
            spinners.get(0).click();
            sleep(1000);
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + level + "\")")).click();
            sleep(1000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- upload progress sheet (cdc957a23: pause / resume) ----------------

    /** The "Uploading N file(s)…" snackbar's View action opens UploadProgressBottomSheet. */
    public boolean openProgressSheet(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (click("snackbar_action")) { sleep(1200); return true; }
            sleep(300);
        }
        return false;
    }

    public boolean hasPauseResume() { return isVisible("btnPauseResume", Duration.ofSeconds(3)); }

    /** content-desc of the first row's pause/resume button: "Pause" or "Resume". */
    public String pauseResumeLabel() {
        try { return driver.findElement(byId("btnPauseResume")).getAttribute("content-desc"); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean tapPauseResume() { return click("btnPauseResume"); }

    /** The first row's status ("Uploading 37%", "Paused 45%", "Queued", "Finishing…", "Failed: …"). */
    public String progressStatus() { return text("tvStatus"); }

    public boolean cancelProgressRow() { return click("btnAction"); }
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
        // tvTreeSummary (bottom_sheet_upload_folder_staging.xml) first; the text match
        // is the fallback for builds that predate the id.
        String byIdText = text("tvTreeSummary");
        if (byIdText != null && !byIdText.isBlank()) return byIdText;
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"folders\")")).getText();
        } catch (WebDriverException e) { return ""; }
    }

    /**
     * Renames the top folder of a folder upload ("Folder name", {@code etFolderName},
     * pre-filled with the picked folder's name — UploadFolderStagingBottomSheet
     * setupFolderNameField). Lets a run upload the same fixture tree under a unique name.
     */
    public boolean setFolderUploadName(String name) {
        try {
            WebElement el = driver.findElement(byId("etFolderName"));
            el.clear();
            el.sendKeys(name);
            hideKeyboard();
            return name.equals(el.getText());
        } catch (WebDriverException e) { return false; }
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
