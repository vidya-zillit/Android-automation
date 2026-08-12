package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DOCUMENT DISTRIBUTION — EMAIL TEMPLATES: the manager
 * ({@code templateManagerFragment} / fragment_dd_template_manager.xml) AND the full-screen
 * editor it opens ({@code EmailTemplateEditorDialog} / dialog_dd_template_editor.xml).
 *
 * <p><b>This screen and the preset overview are the same layout twice.</b>
 * {@code dd_swipe} · {@code dd_recycler} · {@code dd_empty} · {@code dd_empty_cta} ·
 * {@code dd_progress} · {@code dd_new} exist in both fragment files with identical ids, so
 * {@code dd_new} narrows to "one of the two manager screens" and never to this one
 * (and {@code dd_recycler} — trap 3.3, NINE screens — narrows to nothing). The separator
 * is structural: PresetOverviewAdapter always emits section headers
 * ({@code dd_header_title}) because "Smart lists / All contacts / Custom lists" is
 * unconditional over there, while EmailTemplateManagerAdapter emits nothing but template
 * rows. {@link #isManager} therefore requires {@code dd_new} AND the ABSENCE of
 * {@code dd_header_title}, with the activity toolbar's {@code headerText} ("Templates")
 * available for logging and cross-checks.
 *
 * <p><b>Unlike the preset overview, the empty state here is real.</b>
 * {@code EmailTemplateManagerFragment.render()} does
 * {@code ddEmpty.visibility = if (state.isEmpty) VISIBLE else GONE}, so
 * {@code dd_empty_cta} is genuinely reachable on an account with no templates. The CTA's
 * own {@code visibility = VISIBLE} in onViewCreated is a red herring: it is a child of the
 * GONE container, so it is not in the view tree when templates exist.
 *
 * <p><b>The body is a WebView.</b> {@code dd_editor} is a {@code jp.wasabeef.richeditor
 * .RichEditor}, i.e. a contenteditable WebView. Its content is not reliably typeable or
 * readable from the native tree, so {@link #setBody} and {@link #bodyText} report what
 * they managed rather than promising it — a case that needs the body must check the
 * return value and say so, not assert against an empty string it never wrote.
 *
 * <p><b>Two dialog families, one of them dangerous.</b> Delete confirms through
 * {@code MaterialAlertDialogBuilder} ("Delete?" / "Delete this template? This can't be
 * undone."); a user without {@code canPost} instead gets the app's CUSTOM
 * {@code showDialogWithButton}, whose right button MESSAGES AN ADMIN.
 * {@link ConfirmPopupPage#isFrameworkDialog()} tells them apart.
 */
public class DocDistTemplatesPage extends BasePage {

    public DocDistTemplatesPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- manager identity ----------------

    /** True on the TEMPLATE manager — explicitly not the preset overview (class note). */
    public boolean isManager(Duration timeout) {
        if (!isVisible("dd_new", timeout)) return false;
        if (presentNow("dd_header_title")) return false;   // that is the preset overview
        return true;
    }

    /** The shared activity toolbar's title — set per fragment via setDdToolbarHeader(). */
    public String headerText() { return textOf("headerText"); }

    public boolean hasNewButton() { return isVisible("dd_new", Duration.ofSeconds(3)); }
    public boolean tapNew()       { return click("dd_new"); }

    /** The empty state — reachable here (render() gates it on state.isEmpty). */
    public boolean hasEmptyState() { return isVisible("dd_empty", Duration.ofSeconds(3)); }
    public boolean hasEmptyCta()   { return isVisible("dd_empty_cta", Duration.ofSeconds(2)); }
    public boolean tapEmptyCta()   { return click("dd_empty_cta"); }

    // ---------------- manager list ----------------

    /**
     * Rendered row names, scoped under {@code dd_row} on purpose: a bare {@code dd_name}
     * sweep would also pick up the EDITOR's name field when the dialog is open (its window
     * and the rows behind it are both in the dumped hierarchy — trap 3.13b).
     */
    public List<String> templateNames() {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                                    + "//*[@resource-id='" + appPackage() + ":id/dd_name']"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * The full list, scroll-collected. No search box on this screen, so scrolling is the
     * only way past the fold and a one-screenful assertion is how a template that exists
     * gets reported missing (trap 3.4).
     */
    public List<String> allTemplateNamesScrolled() {
        Set<String> seen = new LinkedHashSet<>(templateNames());
        int stable = 0;
        for (int i = 0; i < 12 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(templateNames());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 5; i++) swipe(0.35, 0.75);
        return new ArrayList<>(seen);
    }

    public boolean hasTemplate(String name) {
        return allTemplateNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    public boolean waitForTemplate(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (templateNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
            if (hasTemplate(name)) return true;
            pullToRefresh();
            sleep(350);
        }
        return false;
    }

    public boolean waitForTemplateGone(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!hasTemplate(name)) return true;
            pullToRefresh();
            sleep(350);
        }
        return !hasTemplate(name);
    }

    /**
     * The row's subject line, rendered as {@code "Subject: <subject>"} by
     * EmailTemplateManagerAdapter (and hidden entirely when the subject is blank).
     */
    public String subjectLineFor(String name) { return rowText(name, "dd_subject"); }

    /** The row's body snippet — the HTML body flattened to plain text by the adapter. */
    public String snippetFor(String name) { return rowText(name, "dd_snippet"); }

    /** Opens the editor via the row's pencil (same callback the row tap uses). */
    public boolean tapEdit(String name) { return tapRowControl(name, "dd_edit"); }

    public boolean tapDelete(String name) { return tapRowControl(name, "dd_delete"); }

    public boolean hasEditFor(String name)   { return rowControlPresent(name, "dd_edit"); }
    public boolean hasDeleteFor(String name) { return rowControlPresent(name, "dd_delete"); }

    /** Opens the editor by tapping the row body (EmailTemplateManagerAdapter: onClick). */
    public boolean openTemplate(String name) {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 20; i++) {
                if (tapRowByName(name)) return true;
                swipe(0.75, 0.35);
            }
            for (int i = 0; i < 5; i++) swipe(0.35, 0.75);
        }
        return false;
    }

    private boolean tapRowByName(String name) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_name']"))) {
                String t = e.getText();
                if (t != null && t.equalsIgnoreCase(name)) { e.click(); sleep(500); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /**
     * Row controls are anchored on the {@code dd_row} CONTAINING the name — never by
     * pairing the n-th name with the n-th icon, because a row whose subject or snippet is
     * blank renders fewer children and the indices stop lining up.
     */
    private boolean tapRowControl(String name, String controlId) {
        for (int i = 0; i < 20; i++) {
            try {
                driver.findElement(AppiumBy.xpath(rowControlXpath(name, controlId))).click();
                sleep(400);
                return true;
            } catch (WebDriverException e) {
                swipe(0.75, 0.35);
            }
        }
        return false;
    }

    private boolean rowControlPresent(String name, String controlId) {
        try {
            return !driver.findElements(AppiumBy.xpath(rowControlXpath(name, controlId))).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    private String rowControlXpath(String name, String controlId) {
        return "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                + "[.//*[@text=" + xpathLiteral(name) + "]]"
                + "//*[@resource-id='" + appPackage() + ":id/" + controlId + "']";
    }

    private String rowText(String name, String childId) {
        try {
            String t = driver.findElement(AppiumBy.xpath(rowControlXpath(name, childId))).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) { return ""; }
    }

    public boolean pullToRefresh() {
        try {
            driver.findElement(byId("dd_swipe"));
            swipe(0.35, 0.80);
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- editor (EmailTemplateEditorDialog) ----------------

    /**
     * True once the editor dialog is up.
     *
     * <p>{@code dd_editor} + {@code dd_save} is the pairing that means "template editor":
     * the composer also has {@code dd_editor} but has no {@code dd_save} (its footer is
     * Cancel / Preview / Send), while the contact editor and watermark wizard have
     * {@code dd_save} but no rich editor. Deliberately not {@code dd_name} (trap 3.3 — it
     * is every manager row's id too) and not {@code dd_subject} alone (seven layouts).
     */
    /**
     * <p>Both ids are checked by PRESENCE, and {@code dd_editor} is scrolled for before
     * giving up. The editor dialog scrolls, and the rich-text body sits below the name,
     * description and subject fields — with the keyboard up after any typing it is simply
     * off-screen, and an off-screen view is absent from the accessibility tree entirely.
     * The visibility-only version reported "the rich-text body editor should be present"
     * for an editor that was open and perfectly healthy, and because that case then left
     * the dialog dirty, the NEXT case saw a pre-filled name and failed too.
     */
    public boolean editorOpen(Duration timeout) {
        // Identified by dd_save + dd_subject, NOT by dd_editor.
        //
        // dd_editor is NOT in this build's template editor at all. Repeated live dumps of
        // the open dialog show exactly [dd_cancel, dd_description, dd_name, dd_save,
        // dd_subject, dd_toolbar] — keyboard down, after scrolling, and at open time as
        // well as later. Requiring it made "the editor is open" false for an editor that
        // was plainly open and usable, which failed H-3 and then H-4 behind it.
        //
        // dd_save is the discriminator that matters: the template MANAGER behind this
        // dialog has dd_new but no dd_save, and template ROWS carry dd_subject but no
        // dd_save either — so the pair identifies the editor without depending on a node
        // this build does not publish.
        if (!isVisible("dd_save", timeout)) return false;
        return presentNow("dd_subject") || presentNow("dd_name");
    }

    /** Prints the dd_ ids actually on screen — see DocDistPresetsPage.dumpEditorState. */
    public void dumpScreen(String why) {
        try {
            var ids = driver.findElements(AppiumBy.xpath("//*[@resource-id]"))
                    .stream()
                    .map(e -> {
                        try { return e.getAttribute("resource-id"); }
                        catch (Exception ex) { return ""; }
                    })
                    .filter(r -> r != null && r.contains(":id/dd_"))
                    .map(r -> r.substring(r.indexOf(":id/") + 4))
                    .distinct().sorted().toList();
            boolean kb = false;
            try { kb = driver.isKeyboardShown(); } catch (Exception ignored) { }
            System.out.println(">>> DIAG (" + why + "): keyboardShown=" + kb
                    + " dd_ids_on_screen=" + ids);
        } catch (WebDriverException e) {
            System.out.println(">>> DIAG (" + why + "): could not read the screen: " + e.getMessage());
        }
    }

    /** Scrolls the editor dialog down to the rich-text body. */
    public void revealBodyEditor() {
        for (String container : new String[]{
                "new UiSelector().className(\"androidx.core.widget.NestedScrollView\")",
                "new UiSelector().className(\"android.widget.ScrollView\")",
                "new UiSelector().scrollable(true)"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(" + container + ").setMaxSearchSwipes(6)"
                                + ".scrollIntoView(new UiSelector().resourceId(\""
                                + appPackage() + ":id/dd_editor\"))"));
                sleep(500);
                return;
            } catch (WebDriverException ignored) { }
        }
    }

    public boolean editorClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!presentNow("dd_editor") || !presentNow("dd_save")) return true;
            sleep(500);
        }
        return false;
    }

    /**
     * The editor's own toolbar title — "Create template" vs "Edit template", which is how
     * a case tells the two modes apart. A MaterialToolbar's title has no id, so it is read
     * as the first non-blank TextView inside {@code dd_toolbar}.
     */
    public String editorTitle() {
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_toolbar']//android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) return t;
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    /**
     * <b>The editor's text fields MUST be matched as EditTexts.</b>
     *
     * <p>{@code dd_name} and {@code dd_subject} are ALSO the ids of every manager row
     * (item_dd_template.xml) — trap 3.3, and exactly the bug that made
     * DocDistCreateFolderSheet report an open sheet against a plain list and made "the
     * sheet has closed" unobservable (trap 3.13b). A full-screen DialogFragment is its own
     * window and the rows underneath stay in the dumped hierarchy, so a bare
     * {@code findElement(dd_subject)} can hand back a ROW's subject line ("Subject: …")
     * instead of the field. Every field lookup below pins the widget class: the editor's
     * fields are {@code android.widget.EditText}, the rows' are {@code android.widget.TextView}.
     */
    public boolean hasNameField()        { return fieldVisible("dd_name", Duration.ofSeconds(3)); }
    public boolean hasDescriptionField() { return fieldVisible("dd_description", Duration.ofSeconds(2)); }
    public boolean hasSubjectField()     { return fieldVisible("dd_subject", Duration.ofSeconds(2)); }
    /**
     * The rich-text body.
     *
     * <p>Reveals before answering. {@code dd_editor} is the LAST field in a scrolling
     * dialog (name → description → subject → body), so by the time the earlier fields have
     * been checked or typed into it is below the fold — and an off-screen view is absent
     * from the accessibility tree, not merely invisible. A plain visibility check reported
     * "the rich-text body editor should be present" for an editor that was open and whose
     * toolbar title read "Create template", and because the case then left the dialog
     * dirty, the NEXT case opened onto a pre-filled name and failed too.
     */
    public boolean hasBodyEditor() {
        if (isVisible("dd_editor", Duration.ofSeconds(2))) return true;
        revealBodyEditor();
        boolean found = presentNow("dd_editor");
        if (!found) dumpScreen("hasBodyEditor: dd_editor absent even after revealing");
        return found;
    }
    public boolean hasSaveButton()       { return isVisible("dd_save", Duration.ofSeconds(2)); }
    public boolean hasCancelButton()     { return isVisible("dd_cancel", Duration.ofSeconds(2)); }

    public boolean setName(String name)        { return typeField("dd_name", name); }
    public boolean setDescription(String text) { return typeField("dd_description", text); }
    public boolean setSubject(String text)     { return typeField("dd_subject", text); }

    public String nameFieldText()    { return fieldText("dd_name"); }
    public String subjectFieldText() { return fieldText("dd_subject"); }

    /**
     * Attempts to type into the rich-text body.
     *
     * <p>{@code dd_editor} is a contenteditable WebView, not an EditText — on most builds
     * sendKeys either throws or silently lands nowhere. Returns true only when the text is
     * READABLE BACK afterwards, so a caller can tell "the body is seeded" from "this build
     * will not let automation type a body" instead of asserting against something it never
     * managed to write.
     */
    public boolean setBody(String text) {
        try {
            WebElement e = driver.findElement(byId("dd_editor"));
            e.click();
            sleep(600);
            e.sendKeys(text);
            // settleImeSafely, NOT hideKeyboardIfShown: the editor is a cancelable
            // dialog, and the guarded hideKeyboard() can still fire a BACK while the IME
            // animates away — dismissing the EDITOR, after which Save reads as "disabled"
            // for a button that no longer exists.
            settleImeSafely();
            sleep(350);
        } catch (WebDriverException ignored) {
            settleImeSafely();
            return false;
        }
        return bodyText().contains(text);
    }

    /** Whatever plain text the WebView exposes to the native tree — often nothing. */
    public String bodyText() {
        StringBuilder sb = new StringBuilder();
        try {
            WebElement editor = driver.findElement(byId("dd_editor"));
            String own = editor.getText();
            if (own != null && !own.isBlank()) sb.append(own);
            for (WebElement child : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_editor']//*"))) {
                String t = child.getText();
                if (t != null && !t.isBlank()) sb.append(' ').append(t);
            }
        } catch (WebDriverException ignored) { }
        return sb.toString().trim();
    }

    /**
     * The app's own gate, and the whole of it: name non-blank. Subject is NOT required
     * ({@code updateSaveEnabled()} reads {@code ddName} and nothing else).
     *
     * <p>Always {@link #revealSave()} first. Typing the name leaves the IME up, the IME
     * covers the bottom of the dialog where Save lives, and an off-screen view is simply
     * ABSENT from the accessibility tree — so the find throws and a perfectly enabled
     * button reads back as disabled. That misread is what made H-4 accuse the app of
     * ignoring its own gate: the screenshot showed "QA_TPL_..._probe" typed into NAME
     * with the keyboard filling the lower half and no Save button anywhere on screen.
     */
    public boolean isSaveEnabled() {
        revealSave();
        try {
            return "true".equalsIgnoreCase(driver.findElement(byId("dd_save")).getAttribute("enabled"));
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Brings the Save button back into the tree after typing.
     *
     * <p>Dismisses the IME (sampling twice, so a keyboard already animating away is not
     * met with a BACK that would close the dialog instead), then scrolls the dialog if
     * Save is still below the fold. Returns true when Save is findable.
     */
    public boolean revealSave() {
        // IME first, unconditionally. Presence is NOT tappability: a view under the
        // keyboard is still in the tree, and UiAutomator clicks raw centre coordinates
        // with no occlusion check — so the tap lands on the keyboard instead. That is how
        // the sibling presets dialog ended up in Gboard's own settings mid-case.
        closeKeyboardIfStablyShown();
        sleep(400);
        if (presentNow("dd_save")) return true;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceId(\""
                            + appPackage() + ":id/dd_save\"))"));
        } catch (WebDriverException ignored) { }
        return presentNow("dd_save");
    }

    public boolean tapSave()   { return revealSave() && click("dd_save"); }
    public boolean tapCancel() { return click("dd_cancel"); }

    /**
     * Save and wait for the dialog to actually CLOSE. The dialog dismisses only on
     * {@code result.onSuccess}; a failed save snackbars and stays put, so "it closed" is
     * the app's own saved signal.
     */
    public boolean saveAndWaitClosed(Duration timeout) {
        if (!isSaveEnabled()) return false;
        if (!tapSave()) return false;
        return editorClosed(timeout);
    }

    /** Closes the editor however it can — for try/finally unwinding, never for a case. */
    public void forceCloseEditor() {
        for (int i = 0; i < 4 && !editorClosed(Duration.ofMillis(600)); i++) {
            if (tapCancel()) { sleep(350); continue; }
            try { driver.navigate().back(); Thread.sleep(350); } catch (Exception e) { return; }
        }
    }

    // ---------------- the composer's "Pick a template" chooser ----------------

    /**
     * Taps the composer's Templates button. {@code dd_btn_templates} is BOTH the library's
     * shortcut chip and the composer's button, so only call this with the composer up.
     */
    /**
     * Taps the composer's Templates button and waits for the chooser, retrying the tap.
     *
     * <p>A single {@code click()} reported success while nothing opened: a live dump right
     * after showed the composer intact ({@code dd_compose_stage}, {@code dd_btn_templates})
     * with neither the chooser nor the "no templates" snackbar. The button sits in the
     * composer's own scrolling toolbar row, so the first tap can land while the row is
     * still settling — "the click returned true" is not "the dialog opened", the same
     * lesson as F-26's Cancel.
     */
    public boolean openTemplatePickerInComposer() {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (!click("dd_btn_templates")) {
                scrollToId("dd_btn_templates");
                if (!click("dd_btn_templates")) continue;
            }
            if (templatePickerShown(Duration.ofSeconds(6))) return true;
            // The app answers an empty template list with a snackbar instead of a dialog —
            // that is a legitimate outcome, so stop rather than tap again.
            if (textShown("No email templates", Duration.ofSeconds(2))) return false;
            sleep(500);
        }
        return templatePickerShown(Duration.ofSeconds(2));
    }

    /**
     * The chooser is a MaterialAlertDialog with setItems(), matched on its title.
     *
     * <p>With no templates at all the app shows a snackbar ("No email templates yet")
     * instead of a dialog — so a false here can mean "empty", not "broken".
     */
    public boolean templatePickerShown(Duration timeout) {
        return textShown("Pick a template", timeout);
    }

    /**
     * Picks a template by name.
     *
     * <p>The list is prefixed by "Save current as template" ONLY when the composer already
     * has a subject or body, so indices shift — always match the name, never a position.
     */
    public boolean chooseTemplateInPicker(String name) {
        // The chooser is a MaterialAlertDialog setItems() list, and it SCROLLS: with
        // several saved templates the wanted row is below the fold, absent from the tree,
        // and a direct match fails on a chooser that is open and correct. Scroll to it
        // first — the same "off-screen is not absent" rule as every other list here.
        for (String locator : new String[]{
                "new UiSelector().textStartsWith(\"" + name + "\")",
                "new UiSelector().textContains(\"" + name + "\")"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(locator)).click();
                sleep(600);
                return true;
            } catch (WebDriverException ignored) { }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))"
                                + ".setMaxSearchSwipes(12).scrollIntoView(" + locator + ")")).click();
                sleep(600);
                return true;
            } catch (WebDriverException ignored) { }
        }
        dumpScreen("chooseTemplateInPicker: '" + name + "' not in the chooser after scrolling");
        return false;
    }

    /** The app's shared confirmation dialog — both families (trap 3.1 / 3.15). */
    public ConfirmPopupPage confirmPopup() { return new ConfirmPopupPage(driver); }

    // ---------------- helpers ----------------

    private boolean presentNow(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private String textOf(String id) {
        try {
            String t = driver.findElement(byId(id)).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) { return ""; }
    }

    // --- EditText-scoped field access (see the note above hasNameField) ---

    private String fieldXpath(String id) {
        return "//android.widget.EditText[@resource-id='" + appPackage() + ":id/" + id + "']";
    }

    private boolean fieldPresent(String id) {
        try { return !driver.findElements(AppiumBy.xpath(fieldXpath(id))).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean fieldVisible(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (fieldPresent(id)) return true;
            sleep(300);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    private String fieldText(String id) {
        try {
            String t = driver.findElement(AppiumBy.xpath(fieldXpath(id))).getText();
            return t == null ? "" : t;
        } catch (WebDriverException e) { return ""; }
    }

    private boolean typeField(String id, String value) {
        try {
            WebElement f = driver.findElement(AppiumBy.xpath(fieldXpath(id)));
            f.click();
            f.clear();
            if (!value.isEmpty()) f.sendKeys(value);
            settleImeSafely();
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(350); return true; }
        catch (WebDriverException e) { return false; }
    }

    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private void swipe(double fromY, double toY) {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * Math.min(fromY, toY)),
                    "width", 200,
                    "height", (int) (size.height * Math.abs(fromY - toY)),
                    "direction", fromY > toY ? "up" : "down",
                    "percent", 0.8));
            Thread.sleep(250);
        } catch (Exception ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
