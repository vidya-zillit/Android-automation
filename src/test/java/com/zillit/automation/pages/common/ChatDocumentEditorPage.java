package com.zillit.automation.pages.common;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The app's own document editor ({@code OnlyOfficeEditorActivity}, "Zillit Docs / Sheets") as it
 * is reached from a CHAT message: an Office file tapped in any chat opens it READ-ONLY.
 *
 * <p><b>What the app decides</b> ({@code ChatAttachmentEditor.openChatAttachment}, development):
 * an Office extension (docx/doc/odt/rtf, xlsx/xls/ods/csv, pptx/ppt/odp), a stored 24-hex message
 * id, a non-Box project and a known chat unit. The chat first asks Drive for the editor config
 * ({@code /editor/chat/<id>/config} for C&amp;C, {@code /editor/unit-chat/<unit>/<id>/config} for
 * the unit chats) and only then starts the activity with {@code mode=view}. ANY failure there
 * falls back, silently, to the old {@code ACTION_VIEW} hand-off, so the file opens in ANOTHER APP.
 * PDF / txt / md always take that other-app route. Repeat taps within 1.5 s are dropped
 * (a global guard), so two opens must be at least {@link #TAP_GUARD} apart.
 *
 * <p><b>What can be seen.</b> The activity's layout is built in code with NO resource ids: a
 * WebView, a progress bar, a centred status TextView ("Loading editor…" / "Failed to load
 * editor…") and a bottom-end bar holding one native {@code Button} "Edit". That button is GONE
 * for a chat file ({@code canSwitchToEdit = source == null && …}). The action bar is hidden, so
 * only system Back closes it — and in view mode Back finishes the activity at once (no "Saving…",
 * no notify prompt). Everything else (title, "View only" notice, toolbars) is drawn by the web
 * page; WebView debugging is off, so only the native accessibility tree is reachable.
 *
 * <p>Calibrated against live dumps on the Pixel (dev flavour), 2026-11-23 — see
 * {@code CHAT_DOCUMENT_EDITOR_TESTCASES.md}.
 */
public class ChatDocumentEditorPage extends BasePage {

    /** {@code driver.currentActivity()} contains this while the editor is in front. */
    public static final String EDITOR_ACTIVITY = "OnlyOfficeEditorActivity";
    /** {@code drive_loading_editor} — note the U+2026 ellipsis. */
    public static final String LOADING = "Loading editor…";
    /** {@code drive_failed_to_load_editor}; the {@code _format} variant starts with the same words. */
    public static final String FAILED = "Failed to load editor";
    /** {@code drive_saving} — edit mode only; must never show when a chat file closes. */
    public static final String SAVING = "Saving…";
    /** {@code no_application_to_open_file}: the other-app route found no app for the file. */
    public static final String NO_APP = "proper application for viewing this file";
    /** The global repeat-tap guard is 1.5 s; wait a little longer between two opens. */
    public static final Duration TAP_GUARD = Duration.ofMillis(1700);

    /**
     * Editing affordances that must NOT be on screen in view mode (text or content-desc).
     * Calibrated: the Zillit Docs/Sheets page draws its document on a canvas and exposes no
     * toolbar nodes in EITHER mode, so in practice this list can only catch a regression to a
     * different editor (the old Collabora pencil "Edit document", Undo, Font Color…). The real
     * mode signals are {@link #viewOnlyNoticeShown()} and {@link #editButtonShown()}.
     */
    public static final List<String> EDIT_TOOLS = List.of(
            "Edit document", "Undo", "Redo", "Font Color", "Background Color", "Align Text",
            "InsertRowsBefore", "InsertColumnsBefore");

    /**
     * The web page's own nodes, by their HTML id (the WebView reports it as resource-id, with
     * no package prefix). Measured on Zillit_32539-dev (origin/development cc3dc2a96):
     * <ul>
     *   <li>a CHAT file (view, no switch): {chrome, strip, status-dot, STATUS-TEXT "View only",
     *       stage, editor, zillit_docs_canvas} — no {@code people}, no {@code notice}, no Button;</li>
     *   <li>a Drive file opened with "Open" (view, may switch; older build): status-text "Live",
     *       NOTICE "View only.", plus the native "Edit" Button;</li>
     *   <li>edit mode: status-text "Live", no notice.</li>
     * </ul>
     * So "View only" in either {@code status-text} or {@code notice} is the read-only signal.
     */
    public static final String NOTICE_ID = "notice";
    public static final String STATUS_ID = "status-text";
    public static final String VIEW_ONLY_TEXT = "View only";

    /** How a tap on a chat document ended. */
    public enum Outcome {
        /** {@code OnlyOfficeEditorActivity} came to the front. */
        EDITOR,
        /** Another package came to the front (ACTION_VIEW hand-off: chooser, viewer, …). */
        EXTERNAL_APP,
        /** The other-app route found no app ("You may don't have proper application…"). */
        NO_APP_TOAST,
        /** Nothing changed at all. */
        NOTHING
    }

    private String lastPackage = "";
    private String lastActivity = "";

    public ChatDocumentEditorPage(AndroidDriver driver) {
        super(driver);
    }

    // ───────────────────────────── where are we ─────────────────────────────

    public String currentActivity() {
        try {
            String a = driver.currentActivity();
            return a == null ? "" : a;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public String currentPackage() {
        try {
            String p = driver.getCurrentPackage();
            return p == null ? "" : p;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean isEditorActivity() {
        return currentActivity().contains(EDITOR_ACTIVITY);
    }

    /** The package seen when {@link #waitForOutcome} decided — for EXTERNAL_APP, the other app. */
    public String lastPackage() { return lastPackage; }

    /** The activity seen when {@link #waitForOutcome} decided. */
    public String lastActivity() { return lastActivity; }

    /**
     * Polls until the tap has an outcome. The chat fetches the Drive config BEFORE any screen
     * appears, so the first few seconds look like "nothing happened" — hence the long budget.
     */
    public Outcome waitForOutcome(Duration timeout) {
        String app = appPackage();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            lastActivity = currentActivity();
            lastPackage = currentPackage();
            if (lastActivity.contains(EDITOR_ACTIVITY)) return Outcome.EDITOR;
            if (!lastPackage.isEmpty() && !lastPackage.equals(app)) return Outcome.EXTERNAL_APP;
            if (anyTextContains(NO_APP)) return Outcome.NO_APP_TOAST;
            pause(500);
        }
        return Outcome.NOTHING;
    }

    // ───────────────────────────── editor state ─────────────────────────────

    public boolean webViewPresent() {
        return !find(By.className("android.webkit.WebView")).isEmpty();
    }

    public boolean loadingShown() {
        return anyTextContains(LOADING) || anyTextContains("Loading editor");
    }

    public boolean failedShown() {
        return anyTextContains(FAILED);
    }

    /** The status TextView's error text, if any (e.g. "Failed to load editor: …"). */
    public String failureText() {
        for (WebElement e : find(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + FAILED + "\")"))) {
            try { return e.getText(); } catch (WebDriverException ignored) { }
        }
        for (String s : new String[]{"SSL certificate error", "File ID missing"}) {
            if (anyTextContains(s)) return s;
        }
        return "";
    }

    /**
     * Waits for the page to finish: the WebView is in the tree and "Loading editor…" has gone.
     * Returns early (false) the moment a load error is drawn.
     */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isEditorActivity()) return false;
            if (failedShown() || !failureText().isEmpty()) return false;
            if (webViewPresent() && !loadingShown()) return true;
            pause(1000);
        }
        return false;
    }

    /**
     * The APP-drawn "Edit" switch: a native {@code android.widget.Button}, outside the WebView.
     * GONE for every chat file.
     */
    public boolean editButtonShown() {
        return !find(By.xpath("//android.widget.Button[@text='Edit' or @text='EDIT']"
                + "[not(ancestor::android.webkit.WebView)]")).isEmpty();
    }

    /** Editing tools on screen (text or content-desc), anywhere — expected empty in view mode. */
    public List<String> editToolsShown() {
        List<String> out = new ArrayList<>();
        for (String t : EDIT_TOOLS) {
            if (!find(AppiumBy.accessibilityId(t)).isEmpty()
                    || !find(By.xpath("//*[@text=" + xpathLiteral(t) + "]")).isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * The page's "View only" marker — {@code status-text} for a chat file, {@code notice} for a
     * Drive file opened read-only; an edit-mode load shows "Live" and no notice. The page paints
     * it a moment after "Loading editor…" goes, so this polls.
     */
    public boolean viewOnlyNoticeShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (!find(By.xpath("//*[(@resource-id='" + NOTICE_ID + "' or @resource-id='" + STATUS_ID
                    + "') and contains(@text,'" + VIEW_ONLY_TEXT + "')]")).isEmpty()) return true;
            pause(700);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    /** The page's status text ("View only" / "Live"), or "" when the page has not drawn it. */
    public String statusText() {
        for (WebElement e : find(By.xpath("//*[@resource-id='" + STATUS_ID + "']"))) {
            try { return e.getText(); } catch (WebDriverException ignored) { }
        }
        return "";
    }

    /**
     * Why Drive refused the editor config, read from the app's own HTTP log in logcat
     * (ktor_logger prints the {@code /editor/…/config} request and the JSON body). Returns e.g.
     * {@code 400 chat_attachment_storage_unsupported}, or "" when nothing was logged. The
     * fallback to another app is otherwise silent, so this is the only diagnostic there is.
     */
    public String editorConfigFailure() {
        List<String> lines = new ArrayList<>();
        try {
            for (org.openqa.selenium.logging.LogEntry e : driver.manage().logs().get("logcat")) {
                lines.add(e.getMessage());
            }
        } catch (RuntimeException ignored) { }
        String r = parseEditorAnswer(lines);
        if (!r.isEmpty()) return r;
        // Appium's logcat capture can miss lines on a busy device; adb's ring buffer is the
        // fallback (last ~4000 lines — the tap is seconds old).
        try {
            String udid = com.zillit.automation.core.Config.get("zillit.udid");
            if (markEpoch.isEmpty()) return "";
            return parseEditorAnswer(adb(udid, "logcat", "-d", "-T", markEpoch).lines().toList());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * "" when no editor-config call happened, "&lt;status&gt; &lt;message&gt;" when one was answered,
     * "requested, no answer logged" when only the request is there.
     */
    static String parseEditorAnswer(List<String> lines) {
        String status = "";
        boolean inEditorResponse = false;
        boolean requested = false;
        String result = "";
        for (String l : lines) {
            if (!l.contains("ktor_logger")) continue;
            if (l.contains("REQUEST:") && l.contains("/editor/")) {
                requested = true;
                result = "";
            } else if (l.contains("RESPONSE:")) {
                status = l.substring(l.indexOf("RESPONSE:") + 9).trim();
                inEditorResponse = false;
            } else if (l.contains("FROM:") && l.contains("/editor/")) {
                inEditorResponse = true;
                result = status;
            } else if (inEditorResponse && l.contains("\"message\":\"")) {
                int i = l.indexOf("\"message\":\"") + 11;
                int j = l.indexOf('"', i);
                result = (status + " " + (j > i ? l.substring(i, j) : "")).trim();
                inEditorResponse = false;
            }
        }
        if (result.isEmpty() && requested) return "requested, no answer logged";
        return result;
    }

    /** Clears the logcat buffer Appium keeps, so {@link #editorConfigFailure} sees only what follows. */
    public void markLogcat() {
        try { driver.manage().logs().get("logcat"); } catch (RuntimeException ignored) { }
        try {
            String udid = com.zillit.automation.core.Config.get("zillit.udid");
            String t = adb(udid, "shell", "date", "+%s").trim();
            markEpoch = t.matches("\\d+") ? t + ".000" : "";
        } catch (Exception e) {
            markEpoch = "";
        }
    }

    /** Device time (epoch seconds) when {@link #markLogcat} ran — the adb fallback reads from here. */
    private String markEpoch = "";

    /** The page's live-status chip ("Live") — proof the editor page itself rendered. */
    public boolean pageChromeShown() {
        return !find(By.xpath("//*[@resource-id='status-text' or @resource-id='editor'"
                + " or starts-with(@resource-id,'zillit_')]")).isEmpty();
    }

    /** True when the file name (without extension is enough) appears anywhere in the tree. */
    public boolean fileNameShown(String namePart) {
        return anyTextOrDescContains(namePart);
    }

    /**
     * A compact description of what the editor exposes — native nodes and any web nodes with
     * text/desc. Printed into the run log so a failure says what WAS there.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("activity=").append(currentActivity());
        try {
            List<WebElement> all = driver.findElements(By.xpath("//*[@text!='' or @content-desc!='']"));
            int n = 0;
            for (WebElement e : all) {
                if (n++ >= 40) { sb.append(" …"); break; }
                String cls = attr(e, "className");
                String txt = attr(e, "text");
                String desc = attr(e, "content-desc");
                sb.append(" | ").append(cls.substring(cls.lastIndexOf('.') + 1)).append(':')
                        .append(!txt.isEmpty() ? txt : "[" + desc + "]");
            }
        } catch (WebDriverException e) {
            sb.append(" (tree unreadable: ").append(e.getClass().getSimpleName()).append(')');
        }
        return sb.toString();
    }

    // ───────────────────────────── leaving ─────────────────────────────

    /**
     * ONE system Back, then the editor must be gone and {@code chatVisible} true within the
     * budget — with no "Saving…" and no leave/notify dialog on the way out.
     *
     * @return "" on success, otherwise why it failed.
     */
    public String backToChat(BooleanSupplier chatVisible, Duration timeout) {
        try {
            driver.navigate().back();
        } catch (WebDriverException e) {
            return "Back could not be sent: " + e.getMessage();
        }
        boolean sawSaving = false;
        boolean sawDialog = false;
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (anyTextContains(SAVING)) sawSaving = true;
            if (anyTextContains("Leave Editor") || anyTextContains("Let others know")) sawDialog = true;
            if (!isEditorActivity() && chatVisible.getAsBoolean()) {
                if (sawSaving) return "returned, but \"Saving…\" showed — the file was opened in EDIT mode";
                if (sawDialog) return "returned, but a leave/notify dialog showed on the way out";
                return "";
            }
            pause(400);
        }
        return "one Back did not return to the chat within " + timeout.toSeconds() + "s (activity now "
                + currentActivity() + (sawSaving ? ", \"Saving…\" seen" : "")
                + (sawDialog ? ", a leave/notify dialog seen" : "") + ")";
    }

    /** Back to the app under test after an other-app outcome. */
    public boolean leaveExternalApp() {
        return returnToAppUnderTest();
    }

    // ───────────────────────────── C&C bubble target ─────────────────────────────

    /**
     * C&amp;C ({@code ChatAndGroupAdapter}): a document bubble's click is on {@code srcImage}
     * inside {@code mainCardVw}. Text cards share {@code mainCardVw}, so the target is the
     * bottom-most {@code srcImage}, which only media/document cards have.
     *
     * @return true once a tap was sent.
     */
    public boolean tapNewestCcDocument() {
        // A document card carries a mimeTypeImage badge inside its srcImage (measured:
        // srcImage [312,512][1063,1263], mimeTypeImage [338,1158][417,1237]); a photo has none.
        List<Rectangle> badges = new ArrayList<>();
        for (WebElement b : find(By.id(appPackage() + ":id/mimeTypeImage"))) {
            try { badges.add(b.getRect()); } catch (WebDriverException ignored) { }
        }
        List<WebElement> docs = new ArrayList<>();
        for (WebElement img : find(By.id(appPackage() + ":id/srcImage"))) {
            try {
                Rectangle r = img.getRect();
                if (badges.stream().anyMatch(o -> o.getX() >= r.getX() && o.getY() >= r.getY()
                        && o.getY() + o.getHeight() <= r.getY() + r.getHeight() + 2)) docs.add(img);
            } catch (WebDriverException ignored) { }
        }
        WebElement target = bottomMost(docs.isEmpty() ? find(By.id(appPackage() + ":id/srcImage")) : docs);
        if (target == null) return false;
        try {
            target.click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ───────────────────────────── build probe ─────────────────────────────

    /**
     * Does the INSTALLED build contain the chat → editor code at all?
     *
     * <p>Added by f824d9211 (2026-11-19). A build cut before it opens every chat document in
     * another app, which would make every positive case here fail for a reason that has
     * nothing to do with the app's current behaviour — it happened: the dev build on the
     * Pixel on 2026-11-23 was Zillit_32535-dev, built from a branch without it. The check
     * greps the APK's dex for the {@code ChatAttachmentEditor} class on the device itself
     * (about 1.5 s, nothing pulled to the Mac).
     *
     * @return TRUE / FALSE, or null when adb could not answer (then the cases run anyway).
     */
    public static Boolean buildHasChatEditor(String udid, String pkg) {
        try {
            String apk = adb(udid, "shell", "pm", "path", pkg).lines()
                    .filter(l -> l.startsWith("package:") && l.endsWith("base.apk"))
                    .map(l -> l.substring("package:".length()).trim())
                    .findFirst().orElse("");
            if (apk.isEmpty()) return null;
            String out = adb(udid, "shell", "unzip -p " + apk
                    + " 'classes*.dex' | grep -a -c ChatAttachmentEditor").trim();
            if (!out.matches("\\d+")) return null;
            return Integer.parseInt(out) > 0;
        } catch (Exception e) {
            return null;
        }
    }

    private static String adb(String udid, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(adbBinary());
        if (udid != null && !udid.isBlank()) { cmd.add("-s"); cmd.add(udid); }
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
        return out;
    }

    private static String adbBinary() {
        for (String var : new String[]{"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
            String root = System.getenv(var);
            if (root == null || root.isBlank()) continue;
            java.nio.file.Path c = java.nio.file.Paths.get(root, "platform-tools", "adb");
            if (java.nio.file.Files.isExecutable(c)) return c.toString();
        }
        java.nio.file.Path home = java.nio.file.Paths.get(System.getProperty("user.home"),
                "Library", "Android", "sdk", "platform-tools", "adb");
        return java.nio.file.Files.isExecutable(home) ? home.toString() : "adb";
    }

    // ───────────────────────────── helpers ─────────────────────────────

    static WebElement bottomMost(List<WebElement> els) {
        return els.stream()
                .filter(e -> {
                    try { return e.isDisplayed() && e.getRect().getHeight() > 0; }
                    catch (WebDriverException ex) { return false; }
                })
                .max(Comparator.comparingInt(e -> {
                    Rectangle r = e.getRect();
                    return r.getY() + r.getHeight();
                }))
                .orElse(null);
    }

    private List<WebElement> find(By by) {
        try {
            return driver.findElements(by);
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    private boolean anyTextContains(String s) {
        return !find(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + s.replace("\"", "\\\"") + "\")")).isEmpty();
    }

    private boolean anyTextOrDescContains(String s) {
        String q = s.replace("\"", "\\\"");
        return anyTextContains(s) || !find(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + q + "\")")).isEmpty();
    }

    private static String attr(WebElement e, String name) {
        try {
            String v = e.getAttribute(name);
            return v == null ? "" : v;
        } catch (WebDriverException ex) {
            return "";
        }
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    public static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
