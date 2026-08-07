package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The DOCUMENT EDITOR — {@code OnlyOfficeEditorActivity}, a WebView hosting Collabora
 * Online. Reached from a file's detail screen via {@code btnEdit}, which the app only
 * shows when ALL THREE hold:
 *
 * <pre>
 *   FileUtils.isEditableFile(ext)   // docx xlsx pptx doc xls ppt odt ods odp csv txt
 *   &amp;&amp; perms.canEdit == true      // the user's posting access on this file
 *   &amp;&amp; hasPostingAccess           // module-level posting right
 * </pre>
 *
 * <p><b>The toolbar IS automatable.</b> Collabora publishes its controls to the
 * accessibility tree with {@code content-desc} labels — "Undo", "Font Color",
 * "InsertRowsBefore", "Function Wizard" and so on — so they can be driven by
 * {@code AppiumBy.accessibilityId} without any webview context. Cell contents and the
 * sheet tabs are readable as ordinary text nodes too.
 *
 * <p><b>Two things block a naive test.</b> A Collabora "What's New" modal covers the
 * whole editor on first open and swallows every tap ({@link #dismissWelcomeDialog()}),
 * and the document opens READ-ONLY — the blue pencil ("Edit document") must be tapped
 * before any toolbar control appears ({@link #enterEditMode()}).
 */
public class DriveEditorPage extends BasePage {

    /** Controls Collabora only exposes once the document is in edit mode. */
    public static final List<String> EDIT_TOOLS = List.of(
            "Undo", "Redo", "Font Color", "Background Color", "Align Text",
            "Sort Ascending", "Sort Descending", "InsertRowsBefore", "InsertColumnsBefore",
            "NumberFormatStandard", "Add Decimal Place", "Delete Decimal Place",
            "Show the search bar");

    /** Controls available even read-only. */
    public static final List<String> VIEW_TOOLS = List.of(
            "Function Wizard", "Expand Formula Bar", "Open A1 list",
            "Insert sheet", "Show sheet list");

    public DriveEditorPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isEditorActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("editor.OnlyOfficeEditorActivity");
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Waits for the editor ACTIVITY to come up. Launching it is asynchronous, so a
     * check made straight after tapping Edit sees the previous screen and reports that
     * the editor never opened.
     */
    public boolean waitForEditorActivity(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isEditorActivity()) return true;
            sleep(700);
        }
        return false;
    }

    /** Waits for Collabora to finish loading — a remote editor, so allow real time. */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (hasControl("Edit document") || hasControl("Open A1 list") || hasControl("Undo")) return true;
            sleep(1500);
        }
        return false;
    }

    // ---------------- the two blockers ----------------

    public boolean welcomeDialogShown() {
        return textShown("Collabora Online Development Edition", Duration.ofSeconds(3))
                || textShown("Development Edition", Duration.ofSeconds(2));
    }

    /**
     * Closes the "What's New" modal. Its × carries no id or usable bounds in the tree,
     * so it is tapped proportionally (≈90.5% across, ≈26.8% down) — verified on a
     * 1280×2856 screen. Falls back to a "Close" control when one is exposed.
     */
    public boolean dismissWelcomeDialog() {
        if (!welcomeDialogShown()) return true;
        try {
            WebElement close = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").text(\"Close\")"));
            if (close.getRect().getWidth() > 0) { close.click(); sleep(2500); }
        } catch (WebDriverException ignored) { }
        if (!welcomeDialogShown()) return true;
        try {
            Dimension d = driver.manage().window().getSize();
            driver.executeScript("mobile: clickGesture", java.util.Map.of(
                    "x", (int) (d.getWidth() * 0.905), "y", (int) (d.getHeight() * 0.268)));
            sleep(2500);
        } catch (Exception ignored) { }
        return !welcomeDialogShown();
    }

    public boolean isReadOnly() { return hasControl("Edit document"); }

    /**
     * Taps the blue pencil; the document opens read-only until this is done.
     *
     * <p>Waits for the toolbar to SETTLE rather than returning the moment "Undo"
     * appears. Collabora renders the toolbar progressively — returning on first sight
     * found only 8 of 20 controls and made a healthy build look like it had lost Sort,
     * NumberFormat and the decimal-place buttons.
     */
    public boolean enterEditMode() {
        dismissWelcomeDialog();
        if (!tapControl("Edit document")) return false;

        long deadline = System.currentTimeMillis() + 45000;
        int lastCount = -1, stable = 0;
        while (System.currentTimeMillis() < deadline) {
            if (hasControl("Undo") || hasControl("Font Color")) {
                int n = allControls().size();
                stable = (n == lastCount && n > 0) ? stable + 1 : 0;
                lastCount = n;
                if (stable >= 2) return true;      // two identical reads = settled
            }
            sleep(1500);
        }
        return lastCount > 0;
    }

    // ---------------- toolbar ----------------

    public boolean hasControl(String contentDesc) {
        try {
            return !driver.findElements(AppiumBy.accessibilityId(contentDesc)).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    public boolean tapControl(String contentDesc) {
        try {
            driver.findElement(AppiumBy.accessibilityId(contentDesc)).click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Which of the given controls are currently present. */
    public List<String> presentControls(List<String> wanted) {
        List<String> found = new ArrayList<>();
        for (String w : wanted) if (hasControl(w)) found.add(w);
        return found;
    }

    /** Every content-desc on screen — useful when a build changes the toolbar. */
    public List<String> allControls() {
        try {
            return driver.findElements(AppiumBy.xpath("//*[@clickable='true' and @content-desc!='']"))
                    .stream().map(e -> e.getAttribute("contentDescription"))
                    .filter(s -> s != null && !s.isBlank()).distinct().sorted().toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    // ---------------- reading & writing cells ----------------

    /**
     * The cell-address box ("A1"). It carries a real id from the Collabora DOM —
     * {@code pos_window-input-address} — with NO app-package prefix, so it must be
     * matched as a raw resourceId rather than through {@code byId()}.
     */
    private WebElement addressBox() {
        return driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().resourceId(\"pos_window-input-address\")"));
    }

    /**
     * The formula-bar input holding the SELECTED cell's content.
     *
     * <p>This is the only reliable way to read a cell. Collabora paints the grid to a
     * canvas, so a value can be plainly visible on screen and still be absent from the
     * accessibility tree — grepping the page source for it returns nothing and a test
     * built that way reports data loss that did not happen. Verified: a marker written
     * to D1 was on screen after a reopen while the tree contained zero occurrences.
     */
    private WebElement contentInput() {
        List<WebElement> fields = driver.findElements(
                AppiumBy.className("android.widget.EditText"));
        for (WebElement f : fields) {
            String rid = f.getAttribute("resourceId");
            if (rid == null || !rid.contains("pos_window-input-address")) {
                if (f.getRect().getWidth() > 200) return f;   // the wide formula bar
            }
        }
        throw new WebDriverException("Formula-bar input not found");
    }

    /**
     * Selects a cell by reference, e.g. "D1", and CONFIRMS the address box actually
     * moved there.
     *
     * <p>The commit must be a real ENTER key — {@code mobile: performEditorAction}
     * leaves the address box untouched, so every read silently returned the previously
     * selected cell instead. That produced the worst kind of green-looking failure:
     * {@code readCell("B1")} handing back A1's value.
     */
    public boolean gotoCell(String ref) {
        try {
            WebElement box = addressBox();
            box.click();
            sleep(700);
            box.clear();
            sleep(300);
            box.sendKeys(ref);
            sleep(500);
            pressEnter();
            sleep(2500);
            // Prove the move happened rather than trusting the keystroke.
            String now = addressBox().getText();
            if (!ref.equalsIgnoreCase(now)) {
                pressEnter();
                sleep(1500);
                now = addressBox().getText();
            }
            return ref.equalsIgnoreCase(now);
        } catch (Exception e) {
            return false;
        }
    }

    private void pressEnter() {
        try {
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.ENTER));
        } catch (Exception ignored) { }
    }

    /**
     * The selected cell's content, read from the formula bar.
     *
     * <p>Two quirks are handled here, both of which produced wrong-but-plausible
     * results before:
     * <ul>
     *   <li>the formula bar refreshes ASYNCHRONOUSLY after the address box moves, so an
     *       immediate read returns the PREVIOUS cell's value — {@code readCell("B1")}
     *       cheerfully handed back A1's "Scene". We wait for two identical reads.</li>
     *   <li>Collabora appends a trailing newline to the field ({@code "Scene\n"}), so a
     *       raw comparison against "Scene" fails on a correct value.</li>
     * </ul>
     */
    public String readCell(String ref) {
        if (!gotoCell(ref)) return "";
        String last = null;
        for (int i = 0; i < 8; i++) {
            String now;
            try { now = contentInput().getText(); }
            catch (Exception e) { now = ""; }
            now = now == null ? "" : now.trim();
            if (now.equals(last)) return now;      // settled
            last = now;
            sleep(900);
        }
        return last == null ? "" : last;
    }

    /** Types a value into a cell and commits it. */
    public boolean writeCell(String ref, String value) {
        if (!gotoCell(ref)) return false;
        try {
            WebElement input = contentInput();
            input.click();
            sleep(1000);
            input.clear();
            sleep(400);

            // Typing into Collabora's field is the one thing Appium cannot do reliably
            // here. Tried and rejected, each verified with an in-session round-trip
            // (write → select another cell → return → read):
            //   element.sendKeys()   text appears in the formula bar, cell NOT updated
            //   Actions.sendKeys()   nothing arrives at all
            // What DOES work is the ADB input service — proven by hand: the value lands
            // in the cell, autosaves, and survives a close/reopen. Reaching it from a
            // test needs Appium started with --relaxed-security:
            //     npx appium server --use-drivers uiautomator2 --relaxed-security
            // Without that flag this call throws and the case fails honestly rather than
            // reporting a write that never happened.
            driver.executeScript("mobile: shell", java.util.Map.of(
                    "command", "input",
                    "args", List.of("text", value)));
            sleep(800);
            pressEnter();
            sleep(3500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes WITHOUT touching the formula bar: select the cell, then type.
     *
     * <p>In Collabora, typing while a cell is selected starts editing THAT CELL — the
     * same thing a person does. The existing {@link #writeCell} clicks into the formula
     * bar first, and that is the suspect: the text demonstrably lands in the field (it
     * reads back from there) while the document stays empty on the other device.
     *
     * <p>Used by {@code DriveEditorWriteProbeTest} to settle which path actually commits.
     */
    public boolean writeCellDirect(String ref, String value) {
        if (!gotoCell(ref)) return false;
        try {
            // No click, no clear: the cell is already selected by gotoCell, and typing
            // over a selected cell replaces its contents.
            driver.executeScript("mobile: shell", java.util.Map.of(
                    "command", "input",
                    "args", List.of("text", value)));
            sleep(800);
            pressEnter();
            sleep(3500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** The plain Appium path, kept so the probe can report it alongside the others. */
    public boolean writeCellSendKeys(String ref, String value) {
        if (!gotoCell(ref)) return false;
        try {
            WebElement input = contentInput();
            input.click();
            sleep(800);
            input.clear();
            sleep(300);
            input.sendKeys(value);
            sleep(800);
            pressEnter();
            sleep(3500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gives Collabora time to push an edit to the server before the editor is torn down.
     *
     * <p>Autosave is not instantaneous. An automated test can type a value, read it back
     * correctly from the formula bar, and still lose it — because BACK → LEAVE happens
     * milliseconds later and the save never reaches the server. A human never types this
     * fast, which is why this only shows up under automation.
     */
    public void waitForSave() {
        sleep(SAVE_SETTLE_MS);
    }

    /** How long to let autosave flush. Generous on purpose — losing an edit is worse. */
    public static final long SAVE_SETTLE_MS = 12000;

    /**
     * The ✓ at top-left commits the edit and drops back to read-only.
     *
     * <p>The ✓ is painted into Collabora's canvas — no node, no content-desc — so it can
     * only be tapped by position (4.5% × 5.5%, verified on 1280×2856). A blind gesture
     * cannot report failure, though, and this used to {@code return true} unconditionally:
     * on a screen where the ratio is off it would "succeed" every time while the edit was
     * never committed, which is precisely the kind of green-looking nothing this suite
     * keeps getting caught by.
     *
     * <p>So it now checks the ONE observable consequence: leaving edit mode takes the
     * editing tools off the toolbar. If they are still there, the tap missed.
     */
    public boolean acceptEdit() {
        try {
            Dimension d = driver.manage().window().getSize();
            driver.executeScript("mobile: clickGesture", java.util.Map.of(
                    "x", (int) (d.getWidth() * 0.045), "y", (int) (d.getHeight() * 0.055)));
            sleep(4000);
            return waitLeftEditMode(Duration.ofSeconds(15));
        } catch (Exception e) { return false; }
    }

    /** True once the edit-only toolbar controls are gone — i.e. the editor is read-only. */
    public boolean waitLeftEditMode(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!hasControl("Undo") && !hasControl("Font Color")) return true;
            sleep(1000);
        }
        return false;
    }

    /** Collabora states its own save behaviour in the leave dialog. */
    public boolean leaveDialogMentionsAutosave() {
        return textShown("saved automatically", Duration.ofSeconds(5));
    }

    // ---------------- document content ----------------

    /** The cell-reference box (e.g. "A1"). */
    public String cellReference() {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.EditText\")")).getText();
        } catch (WebDriverException e) { return ""; }
    }

    public boolean documentTextShown(String text) {
        return textShown(text, Duration.ofSeconds(6));
    }

    public boolean sheetTabShown(String name) {
        return textShown(name, Duration.ofSeconds(5));
    }

    // ---------------- leaving ----------------

    /** BACK raises a "Leave Editor" dialog with LEAVE / CANCEL. */
    public boolean pressBack() {
        try { driver.navigate().back(); sleep(2000); return true; }
        catch (Exception e) { return false; }
    }

    public boolean leaveDialogShown() {
        return textShown("Leave Editor", Duration.ofSeconds(6))
                || textShown("LEAVE", Duration.ofSeconds(2));
    }

    public boolean confirmLeave() {
        if (tapText("LEAVE")) { sleep(2500); return true; }
        return tapText("Leave");
    }

    public boolean cancelLeave() {
        if (tapText("CANCEL")) { sleep(1500); return true; }
        return tapText("Cancel");
    }

    /** Leaves the editor whatever state it is in — used by test cleanup. */
    public void forceExit() {
        for (int i = 0; i < 4 && isEditorActivity(); i++) {
            pressBack();
            if (leaveDialogShown()) confirmLeave();
            sleep(1200);
        }
    }

    private boolean tapText(String t) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + t + "\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
