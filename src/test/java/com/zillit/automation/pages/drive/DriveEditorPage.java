package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The DRIVE OFFICE EDITOR — {@code editor.OnlyOfficeEditorActivity}, which (since Zillit-Android
 * 113fcbef6, 2026-10-16) hosts Zillit's OWN editor page — Zillit Sheets / Docs / Slides — in a
 * WebView built in code. It is no longer Collabora: there is no "What's New" modal, no "Edit
 * document" pencil, no formula bar, no ✓ and no "Leave Editor" dialog.
 *
 * <h2>What the accessibility tree really exposes (E-00, captured live 2026-11-23)</h2>
 * <pre>
 * native   FrameLayout › WebView (no resource-id)
 *          TextView "Loading editor…" / "Saving…" / an error — centred, no id
 *          Button "Edit"                     VIEW mode only, bottom-right, no id
 * page     WebView "Zillit Drive"
 *            #chrome › #strip › #status-dot, #status-text "Live"
 *                               #people "People in this file" › initials ("VT")
 *            #notice "View only. You can’t edit this file."   VIEW mode only
 *            #stage › #editor › "Zillit Sheets" | "Zillit Docs" | "Zillit Slides"
 *                                 #zillit_sheets_canvas | #zillit_docs_canvas  (an Image)
 *                                 an EditText 15×10 px — the page's hidden input, sitting at
 *                                 the caret / active cell
 * </pre>
 * The page's ids come through as resource-ids WITHOUT an app-package prefix, so they are matched
 * with {@code UiSelector().resourceId("…")}, never {@code byId()}. Everything inside the document
 * (cells, paragraphs, toolbar, sheet tabs, name box) is painted on the canvas and is NOT in the
 * tree — an edit can only be proven from outside the page (Download → read the file, or the
 * version history on builds that have it).
 *
 * <h2>Modes</h2>
 * File detail {@code btnEdit} opens EDIT mode directly. {@code btnOpen} opens VIEW mode: the page
 * shows {@code #notice} and the app adds a native Button "Edit" when the user may switch.
 * Back in VIEW closes at once; Back in EDIT shows "Saving…" (~1.5 s) and then closes, or — on
 * builds with commit 99120a767 — asks "Let others know you updated this file?". Automation ALWAYS
 * answers that with {@link #skipNotify() Skip}; {@code Send} notifies real people.
 */
public class DriveEditorPage extends BasePage {

    /**
     * How long the editor page may take to reach "Live". Build 32539 on the emulators shows its
     * own "Opening…" loader for 100–150 s before the document appears (measured 2026-11-23), so
     * the old 150 s budget skipped cases on a healthy page.
     */
    public static final int LOAD_S = 240;

    public static final String LOADING = "Loading editor…";
    public static final String SAVING = "Saving…";
    public static final String NOTIFY_TITLE = "Let others know you updated this file?";
    public static final String NOTIFY_MESSAGE = "Everyone with access gets a notification.";
    public static final String VIEW_ONLY_NOTICE = "View only";

    /** Page surfaces, by editor kind. */
    public static final String SHEETS = "Zillit Sheets";
    public static final String DOCS = "Zillit Docs";
    public static final String SLIDES = "Zillit Slides";

    /** Texts the activity's centre TextView shows instead of the page when loading fails. */
    private static final List<String> LOAD_ERRORS = List.of(
            "Failed to load editor", "SSL certificate error", "File ID missing",
            "too large to edit", "not editable", "unavailable");

    public DriveEditorPage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────── activity ─────────────────────────

    public boolean isEditorActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("editor.OnlyOfficeEditorActivity");
        } catch (WebDriverException e) { return false; }
    }

    /** Launching is asynchronous: a check made straight after the tap still sees File details. */
    public boolean waitForEditorActivity(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isEditorActivity()) return true;
            sleep(500);
        }
        return false;
    }

    // ───────────────────────── loading ─────────────────────────

    public boolean loadingShown() { return hasExactText(LOADING); }

    /**
     * Waits for the editor PAGE, not just the activity: "Loading editor…" gone and the page's own
     * surface ("Zillit Sheets/Docs/Slides", its canvas, or the status strip) in the tree.
     *
     * <p>The page's nodes join the tree a few seconds after the WebView turns visible, so a WebView
     * alone is not "loaded" — it is only accepted as a fallback once it has been up, with no
     * loading text, for {@link #WEBVIEW_ONLY_GRACE_MS}.
     */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        long webViewSince = 0;
        while (System.currentTimeMillis() < deadline) {
            if (!isEditorActivity()) { sleep(700); continue; }
            if (loadError() != null) return false;
            if (!loadingShown() && !pageLoading()) {
                if (editorKind() != null || hasPageId("status-text")) {
                    // Notice / Edit switch / presence land a beat after the loader goes.
                    sleep(1500);
                    if (!pageLoading()) return true;
                }
                if (webViewShown()) {
                    if (webViewSince == 0) webViewSince = System.currentTimeMillis();
                    else if (System.currentTimeMillis() - webViewSince > WEBVIEW_ONLY_GRACE_MS) return true;
                } else {
                    webViewSince = 0;
                }
            }
            sleep(800);
        }
        return false;
    }

    /**
     * The page's OWN loader (build 32539): {@code #loading} › {@code #loading-text} "Opening…",
     * {@code #zillit_sheets_loading} "Loading Zillit Sheets…", and {@code #status-text} "Opening…".
     * The editor surface is already in the tree underneath, so the surface alone is not "loaded".
     */
    /**
     * Waits for {@code #status-text} "Live". After the loader the strip reads "Connecting…" for a
     * while; the document is visible but typing before Live is not synced, so the cases that
     * TYPE wait for this on top of {@link #waitLoaded}.
     */
    public boolean waitLive(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if ("Live".equals(statusText())) return true;
            sleep(1500);
        }
        return false;
    }

    public boolean pageLoading() {
        String status = statusText();
        if ("Live".equals(status)) return false;   // connected: whatever loader node remains is stale
        if (hasPageId("loading-text") || hasPageId("loading")) return true;
        for (String id : List.of("zillit_sheets_loading", "zillit_docs_loading", "zillit_slides_loading")) {
            if (hasPageId(id)) return true;
        }
        return status != null && status.startsWith("Opening");
    }

    private static final long WEBVIEW_ONLY_GRACE_MS = 20000;

    public boolean webViewShown() {
        try {
            for (WebElement w : driver.findElements(By.className("android.webkit.WebView"))) {
                if (w.isDisplayed() && w.getRect().getHeight() > 200) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** The error the activity shows in place of the page, or null. */
    public String loadError() {
        try {
            for (WebElement t : driver.findElements(By.className("android.widget.TextView"))) {
                String s = t.getText();
                if (s == null || s.isBlank()) continue;
                for (String e : LOAD_ERRORS) if (s.contains(e)) return s;
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    /** "Zillit Sheets" / "Zillit Docs" / "Zillit Slides", or null before the page is up. */
    public String editorKind() {
        for (String k : List.of(SHEETS, DOCS, SLIDES)) if (hasExactText(k)) return k;
        if (hasPageId("zillit_sheets_canvas")) return SHEETS;
        if (hasPageId("zillit_docs_canvas")) return DOCS;
        if (hasPageId("zillit_slides_canvas")) return SLIDES;
        return null;
    }

    /** The page's connection state ("Live"), from {@code #status-text}. */
    public String statusText() { return pageIdText("status-text"); }

    /** Initials shown under {@code #people} — one per person in the file (this user included). */
    public List<String> peopleInFile() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement p : driver.findElements(byPageId("people"))) {
                for (WebElement t : p.findElements(By.className("android.widget.TextView"))) {
                    String s = t.getText();
                    if (s != null && !s.isBlank()) out.add(s.trim());
                }
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Every non-empty text / content-desc / page id on screen — the E-00 inventory. */
    public List<String> exposedLabels() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath("//*"))) {
                String t = e.getText();
                String cd = e.getAttribute("content-desc");
                String rid = e.getAttribute("resource-id");
                String cls = e.getAttribute("class");
                StringBuilder sb = new StringBuilder();
                if (rid != null && !rid.isBlank()) sb.append('#').append(rid.replaceFirst(".*:id/", "")).append(' ');
                if (t != null && !t.isBlank()) sb.append('"').append(t).append("\" ");
                if (cd != null && !cd.isBlank()) sb.append("cd=").append(cd).append(' ');
                if (sb.length() > 0) out.add((cls == null ? "" : cls.replaceFirst(".*\\.", "")) + " " + sb.toString().trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    // ───────────────────────── view vs edit ─────────────────────────

    private static final By EDIT_SWITCH =
            AppiumBy.xpath("//android.widget.Button[@text='Edit']");

    /** The app-drawn "Edit" button: VIEW mode, and the user may switch to editing. */
    public boolean hasEditSwitch() {
        try {
            for (WebElement b : driver.findElements(EDIT_SWITCH)) if (b.isDisplayed()) return true;
        } catch (WebDriverException ignored) { }
        return false;
    }

    public Rectangle editSwitchRect() {
        try { return driver.findElement(EDIT_SWITCH).getRect(); }
        catch (WebDriverException e) { return null; }
    }

    public boolean tapEditSwitch() {
        try { driver.findElement(EDIT_SWITCH).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    /** The page's own "View only. You can’t edit this file." banner ({@code #notice}). */
    public boolean viewOnlyNoticeShown() {
        String n = pageIdText("notice");
        if (n != null && n.contains(VIEW_ONLY_NOTICE)) return true;
        return textShown(VIEW_ONLY_NOTICE, Duration.ofMillis(300));
    }

    public String viewOnlyNotice() { return pageIdText("notice"); }

    /** VIEW mode as the page and the app both report it. */
    public boolean isViewMode() { return hasEditSwitch() || viewOnlyNoticeShown(); }

    /**
     * Waits for the mode to SETTLE on one answer. The notice and the Edit button arrive a beat
     * after the page surface, so a read taken the instant the page loads says "edit" for a view.
     */
    public boolean waitViewMode(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isViewMode()) return true;
            sleep(700);
        }
        return false;
    }

    /**
     * Taps the Edit switch and waits for the reload into edit mode: "Loading editor…" comes back,
     * goes again, and both the button and the notice are gone.
     */
    public SwitchResult switchToEdit(Duration timeout) {
        SwitchResult r = new SwitchResult();
        if (!tapEditSwitch()) return r;
        r.tapped = true;
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (loadingShown()) r.sawReload = true;
            boolean button = hasEditSwitch();
            boolean notice = viewOnlyNoticeShown();
            if (!button && !notice && !loadingShown() && editorKind() != null) {
                // Confirm it stays that way — the page can repaint the notice once more.
                sleep(2500);
                if (!hasEditSwitch() && !viewOnlyNoticeShown()) { r.editMode = true; return r; }
            }
            sleep(400);
        }
        return r;
    }

    public static final class SwitchResult {
        public boolean tapped, sawReload, editMode;
        @Override public String toString() {
            return "tapped=" + tapped + " sawReload=" + sawReload + " editMode=" + editMode;
        }
    }

    // ───────────────────────── typing into the page ─────────────────────────

    /**
     * Focuses the page on an EMPTY part of the document so the next keys go into it:
     * a cell well below the data for a sheet, blank page space below the text for a doc.
     *
     * <p>Positional on purpose — the canvas has no nodes to target. The point is chosen relative
     * to the canvas, at 70 % of its height, which is below any fixture's content on a phone.
     */
    public boolean focusEmptyArea() {
        Rectangle c = canvasRect();
        if (c == null) return false;
        int x = c.getX() + (int) (c.getWidth() * 0.45);
        int y = c.getY() + (int) (c.getHeight() * 0.70);
        tapAt(x, y);
        sleep(1500);
        return true;
    }

    /** The page canvas, or the editor surface as a fallback. */
    public Rectangle canvasRect() {
        for (String id : List.of("zillit_sheets_canvas", "zillit_docs_canvas", "zillit_slides_canvas", "editor")) {
            try {
                List<WebElement> els = driver.findElements(byPageId(id));
                if (!els.isEmpty()) return els.get(0).getRect();
            } catch (WebDriverException ignored) { }
        }
        return null;
    }

    /**
     * Types lower-case letters, digits and spaces as REAL key events (no IME composition,
     * no {@code adb shell}, so Appium needs no {@code --relaxed-security}).
     *
     * <p>{@code sendKeys()} on the page's hidden EditText and {@code adb input text} both go
     * through the IME: on a Docs page that replaced the whole paragraph instead of inserting
     * (seen live, E-00). Key events behave like a hardware keyboard.
     */
    public boolean typeKeys(String text) {
        try {
            for (char ch : text.toCharArray()) {
                AndroidKey k = keyFor(ch);
                if (k == null) continue;
                driver.pressKey(new KeyEvent(k));
                sleep(120);
            }
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public void pressEnter() {
        try { driver.pressKey(new KeyEvent(AndroidKey.ENTER)); } catch (WebDriverException ignored) { }
    }

    public void pressEscape() {
        try { driver.pressKey(new KeyEvent(AndroidKey.ESCAPE)); } catch (WebDriverException ignored) { }
    }

    private static AndroidKey keyFor(char ch) {
        if (ch >= 'a' && ch <= 'z') return AndroidKey.valueOf(String.valueOf(Character.toUpperCase(ch)));
        if (ch >= 'A' && ch <= 'Z') return AndroidKey.valueOf(String.valueOf(ch));
        if (ch >= '0' && ch <= '9') return AndroidKey.valueOf("DIGIT_" + ch);
        if (ch == ' ') return AndroidKey.SPACE;
        if (ch == '_') return null;
        return null;
    }

    /**
     * Types a marker into an empty part of the open document and commits it (ENTER — a sheet
     * commits the cell, a doc gets a new paragraph). Returns false if the page could not be
     * focused. Whether it SAVED is a separate question — see {@link DriveFileContent}.
     */
    public boolean writeMarker(String marker) {
        if (!waitLive(Duration.ofSeconds(LOAD_S))) return false;
        if (!focusEmptyArea()) return false;
        if (!typeKeys(marker)) return false;
        sleep(600);
        pressEnter();
        sleep(1500);
        hideKeyboardIfShown();
        return true;
    }

    /** Autosave runs while typing; give it time before Back so the edit is on the server. */
    public void waitForAutosave() { sleep(SAVE_SETTLE_MS); }

    public static final long SAVE_SETTLE_MS = 8000;

    // ───────────────────────── leaving ─────────────────────────

    /** What happened after one Back press, observed every ~200 ms. */
    public static final class CloseResult {
        public boolean sawSaving, sawNotify, closed, sawLegacyLeaveDialog;
        public long closeMs = -1;
        @Override public String toString() {
            return "saving=" + sawSaving + " notify=" + sawNotify + " closed=" + closed
                    + " closeMs=" + closeMs + " legacyLeave=" + sawLegacyLeaveDialog;
        }
    }

    /**
     * Presses Back once and watches: "Saving…", the notify dialog, the activity finishing.
     * Answers the notify dialog with SKIP (never Send) when {@code skipNotify} is set.
     */
    public CloseResult backAndObserve(Duration timeout, boolean skipNotify) {
        CloseResult r = new CloseResult();
        // With the IME up, the first Back only closes the keyboard.
        hideKeyboardIfShown();
        long start = System.currentTimeMillis();
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        long deadline = start + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!r.sawSaving && hasExactText(SAVING)) r.sawSaving = true;
            if (!r.sawSaving && hasExactText("Leave Editor")) r.sawLegacyLeaveDialog = true;
            if (notifyDialogShown()) {
                r.sawNotify = true;
                if (!skipNotify) return r;
                skipNotify();
            }
            if (!isEditorActivity()) {
                r.closed = true;
                r.closeMs = System.currentTimeMillis() - start;
                return r;
            }
            sleep(200);
        }
        return r;
    }

    public boolean notifyDialogShown() { return hasExactText(NOTIFY_TITLE); }

    /** Answers "Let others know…" with SKIP. Never taps Send. */
    public boolean skipNotify() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").text(\"Skip\")")).click();
            sleep(800);
            return true;
        } catch (WebDriverException e) {
            // The dialog is cancelable and cancel == Skip, so Back is a safe fallback.
            try { driver.navigate().back(); sleep(800); return true; }
            catch (WebDriverException ignored) { return false; }
        }
    }

    public boolean notifyHasNoteField() {
        try {
            for (WebElement e : driver.findElements(By.className("android.widget.EditText"))) {
                String t = e.getText();
                if (t != null && t.contains("Add a note")) return true;
            }
        } catch (WebDriverException ignored) { }
        return textShown("Add a note (optional)", Duration.ofMillis(500));
    }

    public boolean notifyHasButtons() {
        return hasButton("Send") && hasButton("Skip");
    }

    /**
     * Leaves the editor whatever state it is in — used by cleanup. Skips the notify prompt,
     * and in the text editor answers "Discard your changes?" with Discard.
     */
    public void forceExit() {
        for (int i = 0; i < 5 && (isEditorActivity() || isTextEditorActivity()); i++) {
            if (notifyDialogShown()) { skipNotify(); continue; }
            if (hasExactText("Discard your changes?")) { tapButton("Discard"); sleep(1200); continue; }
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            long until = System.currentTimeMillis() + 6000;
            while (System.currentTimeMillis() < until && (isEditorActivity() || isTextEditorActivity())) {
                if (notifyDialogShown() || hasExactText("Discard your changes?")) break;
                sleep(300);
            }
        }
        if (notifyDialogShown()) skipNotify();
    }

    public boolean isTextEditorActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("DriveTextFileActivity");
        } catch (WebDriverException e) { return false; }
    }

    // ───────────────────────── rotation ─────────────────────────

    public boolean rotate(boolean landscape) {
        try {
            driver.rotate(landscape ? org.openqa.selenium.ScreenOrientation.LANDSCAPE
                    : org.openqa.selenium.ScreenOrientation.PORTRAIT);
            return true;
        } catch (WebDriverException e) {
            lastRotateError = e.getMessage() == null ? "" : e.getMessage().split("\n")[0];
            return false;
        }
    }

    /** Why the last {@link #rotate} was refused. */
    public String lastRotateError = "";

    // ───────────────────────── helpers ─────────────────────────

    private By byPageId(String id) {
        return AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"" + id + "\")");
    }

    public boolean hasPageId(String id) {
        try { return !driver.findElements(byPageId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private String pageIdText(String id) {
        try {
            for (WebElement e : driver.findElements(byPageId(id))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) return t;
                String cd = e.getAttribute("content-desc");
                if (cd != null && !cd.isBlank()) return cd;
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    public boolean hasExactText(String text) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    private boolean hasButton(String text) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").text(\"" + text + "\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    private boolean tapButton(String text) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Button\").text(\"" + text + "\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void tapAt(int x, int y) {
        try { driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y)); }
        catch (WebDriverException ignored) { }
    }

    public Dimension screen() { return driver.manage().window().getSize(); }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
