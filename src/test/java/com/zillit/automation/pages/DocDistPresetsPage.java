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
 * DOCUMENT DISTRIBUTION — DISTRIBUTION LISTS ("presets"): the overview
 * ({@code presetOverviewFragment} / fragment_dd_preset_overview.xml) AND the full-screen
 * editor it opens ({@code PresetEditorDialog} / dialog_dd_preset_editor.xml).
 *
 * <p>Both live here because the editor is a child DialogFragment of the overview — a case
 * never has one without the other, and keeping them together means the try/finally that
 * unwinds a failed case can close the dialog through the same object it opened it with.
 *
 * <p><b>Screen identity is the hard part on this screen.</b> The overview and the TEMPLATE
 * manager are the same layout twice: {@code dd_swipe} · {@code dd_recycler} ·
 * {@code dd_empty} · {@code dd_empty_cta} · {@code dd_progress} · {@code dd_new} appear in
 * fragment_dd_preset_overview.xml and fragment_dd_template_manager.xml with identical ids.
 * So {@code dd_new} says "one of the two manager screens" and {@code dd_recycler} (trap
 * 3.3 — NINE screens share it) says nothing at all. The one structural difference is that
 * PresetOverviewAdapter always emits SECTION HEADERS ({@code dd_header_title}, from
 * item_dd_list_section_header.xml — used nowhere else in the app), because
 * {@code PresetOverviewFragment.render()} unconditionally prepends
 * "Smart lists" → "All contacts" → "Custom lists". The template manager has no headers.
 * That, plus the activity toolbar's {@code headerText} ("Distribution lists"), is what
 * {@link #isOverview} matches.
 *
 * <p><b>There is no empty state here, ever.</b> {@code render()} ends with a flat
 * {@code binding.ddEmpty.visibility = View.GONE} — the comment on the line says why: the
 * smart list is always present, so the full-screen empty state "no longer applies".
 * {@code dd_empty_cta} is a CHILD of {@code dd_empty}, so it is not in the view tree
 * either. Anything asserting "empty library → CTA" (the case table's G-2) is asserting
 * behaviour the current source cannot produce.
 *
 * <p><b>Two dialog families, and only one of them is safe to accept.</b> Deleting a list
 * confirms through {@code MaterialAlertDialogBuilder} (framework {@code android:id/button1}
 * — "Delete?" / "Delete this distribution list? This can't be undone."). But EVERY
 * mutating tap on this screen is wrapped in {@code runWithPostingRights}, and a user
 * without {@code canPost} gets the app's CUSTOM {@code showDialogWithButton} instead,
 * whose right button MESSAGES AN ADMIN. {@link ConfirmPopupPage#isFrameworkDialog()}
 * separates them; a case must never blind-accept.
 */
public class DocDistPresetsPage extends BasePage {

    public DocDistPresetsPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- overview identity ----------------

    /**
     * True on the preset OVERVIEW — never on the template manager, which shares every
     * layout id with it (see the class note). Pairs the shared {@code dd_new} with the
     * section headers only PresetOverviewAdapter emits, falling back to the activity
     * toolbar header for the (unlikely) case where a scrolled list has recycled the
     * headers out of the tree.
     */
    public boolean isOverview(Duration timeout) {
        if (!isVisible("dd_new", timeout)) return false;
        return presentNow("dd_header_title")
                || headerText().toLowerCase().contains("distribution list");
    }

    /** The shared activity toolbar's title — set per fragment via setDdToolbarHeader(). */
    public String headerText() { return textOf("headerText"); }

    public boolean hasNewButton() { return isVisible("dd_new", Duration.ofSeconds(3)); }
    public boolean tapNew()       { return click("dd_new"); }

    /**
     * The full-screen empty state. Kept so a case can PROVE it is absent — see the class
     * note: render() forces it GONE on every pass, so this is expected to be false always.
     */
    public boolean hasEmptyState() { return isVisible("dd_empty", Duration.ofSeconds(3)); }
    public boolean hasEmptyCta()   { return isVisible("dd_empty_cta", Duration.ofSeconds(2)); }

    /** "SMART LISTS" / "CUSTOM LISTS" — always two of them on a rendered overview. */
    public List<String> sectionHeaders() { return textsOf("dd_header_title"); }

    // ---------------- overview list ----------------

    /**
     * Rendered row names. Includes the "All contacts" SMART row, which is not a preset —
     * PresetOverviewAdapter binds smart and custom rows through the same item_dd_preset
     * view holder, so both carry {@code dd_name}.
     *
     * <p>Scoped under {@code dd_row} on purpose: a bare {@code dd_name} sweep would also
     * pick up the EDITOR's name field when the dialog is open (its window and the rows
     * behind it are both in the dumped hierarchy — trap 3.13b).
     */
    public List<String> listNames() {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                                    + "//*[@resource-id='" + appPackage() + ":id/dd_name']"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * The full list, scroll-collected.
     *
     * <p>This is a RecyclerView with no search box (unlike the library), so the scroll IS
     * the only way to see past the fold — a row below it is simply absent from the tree,
     * and asserting on one screenful is how "the list was not created" gets reported for a
     * list that is plainly there (trap 3.4).
     */
    public List<String> allListNamesScrolled() {
        Set<String> seen = new LinkedHashSet<>(listNames());
        int stable = 0;
        for (int i = 0; i < 12 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(listNames());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 5; i++) swipe(0.35, 0.75);
        return new ArrayList<>(seen);
    }

    public boolean hasList(String name) {
        return allListNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    /** Waits for a row to exist, refreshing between passes (a save round-trips the API). */
    /**
     * Waits for a row, and gives up IMMEDIATELY if the app is no longer in the module.
     *
     * <p>G-7 spent its full 45 seconds here swiping at the phone's system Theme picker:
     * the app had gone to the background, every {@code listNames()} honestly returned
     * empty, and the case reported "the renamed list is not in the overview" — an
     * accusation about a screen it was not even looking at. Absence is only evidence when
     * we are somewhere it could have been present, so check WHERE we are before believing
     * WHAT we see. Two consecutive samples, because {@code currentActivity()} reports the
     * transition mid-navigation (trap 3.29).
     */
    public boolean waitForList(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (listNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) return true;
            if (hasList(name)) return true;
            if (!inModule() && !inModule()) {
                System.out.println(">>> waitForList('" + name + "') ABANDONED: the app is no "
                        + "longer in Document Distribution (on " + safeActivity() + ") — this is "
                        + "a lost-the-app problem, NOT a missing row");
                return false;
            }
            pullToRefresh();
            sleep(350);
        }
        return false;
    }

    private boolean inModule() {
        String a = safeActivity();
        return a.contains("docdistribution");
    }

    private String safeActivity() {
        try {
            String a = driver.currentActivity();
            return a == null ? "?" : a;
        } catch (WebDriverException e) { return "?"; }
    }

    /** Waits for a row to DISAPPEAR — the delete assertion, with the same refresh loop. */
    public boolean waitForListGone(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!hasList(name)) return true;
            pullToRefresh();
            sleep(350);
        }
        return !hasList(name);
    }

    /** Opens a list's editor by tapping its row (PresetOverviewAdapter: onCustomClick). */
    public boolean openList(String name) {
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
     * Taps the trash icon on the row carrying this name.
     *
     * <p>Anchored on the {@code dd_row} that CONTAINS the name rather than pairing two id
     * lists by index: the smart row hides its {@code dd_delete} and {@code dd_export}
     * (PresetOverviewAdapter.bindSmart), so the n-th name and the n-th delete button are
     * NOT the same row.
     */
    public boolean tapDelete(String name) { return tapRowControl(name, "dd_delete"); }

    /** The per-row CSV export icon — visible only with {@code canDownload}. */
    public boolean hasExportFor(String name) { return rowControlPresent(name, "dd_export"); }

    public boolean hasDeleteFor(String name) { return rowControlPresent(name, "dd_delete"); }

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

    /** The recipient count line on a row ("N recipients" / "N people" for the smart row). */
    public String countTextFor(String name) {
        try {
            String t = driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                            + "[.//*[@text=" + xpathLiteral(name) + "]]"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_count']")).getText();
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

    // ---------------- editor (PresetEditorDialog) ----------------

    /**
     * True once the editor dialog is up.
     *
     * <p>Matched on {@code dd_recipients_recycler} / {@code dd_in_email} / {@code dd_import_csv},
     * each of which exists in dialog_dd_preset_editor.xml and NOWHERE else in the app.
     * Deliberately NOT {@code dd_name} (trap 3.3 / 3.13b — it is also every overview row's
     * id, so it would report "the editor is open" against a plain list, and worse, make
     * "the editor has closed" unobservable after a successful save) and not {@code dd_save}
     * (shared with the contact editor and the watermark wizard).
     */
    public boolean editorOpen(Duration timeout) {
        return isVisible("dd_recipients_recycler", timeout)
                || isVisible("dd_in_email", Duration.ofSeconds(2));
    }

    /** True once the editor is GONE — instant presence checks, no visibility waits. */
    public boolean editorClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!presentNow("dd_recipients_recycler")
                    && !presentNow("dd_in_email")
                    && !presentNow("dd_import_csv")) {
                return true;
            }
            sleep(500);
        }
        return false;
    }

    /**
     * The editor's own toolbar title — "Create distribution list" vs "Edit distribution
     * list", which is how a case tells create mode from edit mode. A MaterialToolbar's
     * title has no id of its own, so it is read as the first non-blank TextView inside
     * {@code dd_toolbar}.
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
     * <p>{@code dd_name} and {@code dd_description} are ALSO the ids of every overview row
     * (item_dd_preset.xml) — trap 3.3, and precisely the bug that made
     * DocDistCreateFolderSheet report "the sheet is open" against a plain list and, worse,
     * made "the sheet has closed" unobservable (trap 3.13b). A full-screen DialogFragment
     * is a separate window, and the rows underneath it stay in the dumped hierarchy, so a
     * bare {@code findElement(dd_name)} can return a ROW that happens to sort first.
     * Every field lookup below therefore pins the widget class: the editor's fields are
     * {@code android.widget.EditText}, the rows' are {@code android.widget.TextView}.
     */
    public boolean hasNameField()        { return fieldVisible("dd_name", Duration.ofSeconds(3)); }
    public boolean hasDescriptionField() { return fieldVisible("dd_description", Duration.ofSeconds(2)); }
    public boolean hasEmailField()       { return fieldVisible("dd_in_email", Duration.ofSeconds(2)); }
    public boolean hasRecipientNameField() { return fieldVisible("dd_in_name", Duration.ofSeconds(2)); }
    public boolean hasRecipientJobField()  { return fieldVisible("dd_in_job", Duration.ofSeconds(2)); }
    public boolean hasAddButton()        { return isVisible("dd_add", Duration.ofSeconds(2)); }
    public boolean hasImportCsvButton()  { return isVisible("dd_import_csv", Duration.ofSeconds(2)); }
    public boolean hasSaveButton()       { return isVisible("dd_save", Duration.ofSeconds(2)); }
    public boolean hasCancelButton()     { return isVisible("dd_cancel", Duration.ofSeconds(2)); }

    public boolean setName(String name)        { return typeField("dd_name", name); }
    public boolean setDescription(String text) { return typeField("dd_description", text); }

    public String nameFieldText()  { return fieldText("dd_name"); }

    /**
     * The RAW text the add-recipient email input reports.
     *
     * <p><b>An EMPTY field does not report "".</b> {@code dd_in_email} is a
     * TextInputEditText whose hint lives on the wrapping TextInputLayout
     * ({@code android:hint="@string/dd_field_email"} → "Email"), and Material's
     * {@code TextInputLayout.AccessibilityDelegate} publishes that hint as the NODE'S TEXT
     * whenever the editor is empty (<i>showingText ? setText(inputText) :
     * setText(hintText)</i>). The same construction is visible in any address-book dump,
     * where the untouched {@code dd_search_input} reads "Search name, email, or list".
     * Use {@link #emailFieldCleared()} to ask "did the app empty this field"; keep this
     * accessor for diagnostics only.
     */
    public String emailFieldText() { return fieldText("dd_in_email"); }

    /**
     * True when the add-recipient email input holds no typed address.
     *
     * <p>This is the app's own success signal: {@code PresetEditorDialog.addRecipient()}
     * sets {@code ddInEmail.text = null} (and the name / job with it) ONLY after
     * {@code EmailValidator.isValid} passed — the reject path errors the field and returns
     * with the text still in place.
     *
     * <p>Asked as "holds no address" rather than "text is blank" or "text equals the hint",
     * for the reason spelled out on {@link #emailFieldText()}: a cleared field dumps as
     * "Email", never as "". An address always carries an '@' and neither the hint nor any
     * error decoration the TextInputLayout adds ever does, so this stays correct even if
     * the hint string is re-worded.
     */
    public boolean emailFieldCleared() { return !emailFieldText().contains("@"); }

    /**
     * The app's own gate, and the whole of it:
     * {@code ddSave.isEnabled = !ddName.text.isNullOrBlank()}.
     * Recipients are NOT required — an empty list saves fine (the source comment is
     * explicit: "user can create an empty list and add contacts later").
     */
    /**
     * True when Save is enabled — after making sure Save is on screen at all.
     *
     * <p>Same trap as the template editor (H-4): typing leaves the IME up, the IME covers
     * the foot of the dialog, and an off-screen view is ABSENT from the accessibility
     * tree — so the find throws and an enabled button reads back as disabled.
     */
    public boolean isSaveEnabled() {
        revealSave();
        try {
            return "true".equalsIgnoreCase(driver.findElement(byId("dd_save")).getAttribute("enabled"));
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Makes Save both PRESENT and TAPPABLE.
     *
     * <p>Closing the IME comes FIRST, unconditionally — "present in the accessibility
     * tree" is not the same as "not covered by the keyboard", and this dialog keeps
     * {@code dd_save} in the tree while the IME is drawn on top of it. UiAutomator clicks
     * an element's centre COORDINATES with no occlusion check, so the tap went to whatever
     * the keyboard had at that spot: G-7 hit Gboard's palette icon on one run (landing in
     * its theme picker, which I first mistook for a system screen) and its gear on the
     * next, ending in {@code inputmethod.latin.preference.SettingsActivity}. Both times
     * the case then reported an empty overview — about a screen that was not even the
     * app's. Checking presence first is what let that through.
     */
    public boolean revealSave() {
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

    /**
     * Fills the add-recipient row and taps Add.
     *
     * <p>Only the email is validated ({@code EmailValidator.isValid}); an invalid one sets
     * the field's error and returns WITHOUT adding a row, so the count is the assertion,
     * not an error string. On success the app CLEARS all three inputs — that is the
     * cheapest success signal available, see {@link #emailFieldText()}.
     */
    public boolean addRecipient(String email, String name, String job) {
        if (!typeField("dd_in_email", email)) return false;
        if (name != null && !typeField("dd_in_name", name)) return false;
        if (job != null && !typeField("dd_in_job", job)) return false;

        // The app reads dd_in_email AT CLICK TIME and bails out silently when it does not
        // pass EmailValidator (PresetEditorDialog.addRecipient: sets .error and returns).
        // So the one fact that matters is what the field holds the instant before Add is
        // tapped — log it, because "row never appeared" alone cannot distinguish
        // "typing was lost" from "the app rejected it".
        String atClick = fieldText("dd_in_email");
        System.out.println(">>> addRecipient: dd_in_email holds '" + atClick + "' before tapping Add");
        boolean added = click("dd_add");
        sleep(600);
        System.out.println(">>> addRecipient: after Add, dd_in_email holds '"
                + fieldText("dd_in_email") + "' (the app CLEARS it on success)");
        return added;
    }


    /** Recipient rows currently rendered, by email (item_dd_preset_recipient: dd_email). */
    /**
     * The recipient rows currently in the editor.
     *
     * <p>Reveals the list before reading it. Rows hidden BEHIND THE SOFT KEYBOARD are not
     * in the accessibility tree at all, so a plain count returns 0 for recipients that
     * were added perfectly well — which is exactly how G-6 reported "expected 1 but was 0"
     * one line after `addRecipient()` had returned true. (This only started showing once
     * the page stopped dismissing the IME with a BACK press: that BACK was accidentally
     * revealing the list. The right fix is to reveal it deliberately, not to go back to
     * relying on a side effect that also dismissed the whole dialog — see
     * {@code BasePage.settleImeSafely}.)
     */
    public List<String> recipientEmails() {
        // The IME hides the list COMPLETELY — with the keyboard up, dd_recipients_recycler
        // is not in the tree at all (proved by the DIAG line), so no amount of scrolling
        // finds a row. Close it first; BACK is safe while the keyboard is stably showing
        // because the input method consumes it.
        closeKeyboardIfStablyShown();
        List<String> rows = readRecipientRows();
        // Poll with reveals: the row is added asynchronously to the adapter, AND it can be
        // off-screen behind the keyboard. G-5 (which types nothing above the list) reads
        // it first time; G-6 (which fills name + description first, pushing the list
        // further down) read 0 until this loop existed.
        for (int i = 0; i < 4 && rows.isEmpty(); i++) {
            revealRecipients();
            rows = readRecipientRows();
            if (rows.isEmpty()) sleep(350);
        }
        if (rows.isEmpty()) dumpEditorState("recipient list read back EMPTY");
        return rows;
    }

    /**
     * Prints what is ACTUALLY on screen — the only thing that settles "is the row hidden,
     * is the dialog gone, or was it never added". Four rounds of reasoning from the
     * assertion text alone failed to answer that; one of these lines does. Kept
     * permanently because the same question recurs on every scrolling dialog.
     */
    public void dumpEditorState(String why) {
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

    private List<String> readRecipientRows() {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/dd_recipients_recycler']"
                                    + "//*[@resource-id='" + appPackage() + ":id/dd_email']"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /** Scrolls the editor so the recipients list is on screen (above the keyboard). */
    public void revealRecipients() {
        // Try the DIALOG's scroll container by class first. A bare `scrollable(true)`
        // matches whichever scrollable UiAutomator finds first — often the (empty)
        // recipients RecyclerView itself, which scrolls nowhere and hides the real
        // container underneath it.
        for (String container : new String[]{
                "new UiSelector().className(\"androidx.core.widget.NestedScrollView\")",
                "new UiSelector().className(\"android.widget.ScrollView\")",
                "new UiSelector().scrollable(true)"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(" + container + ").setMaxSearchSwipes(6)"
                                + ".scrollIntoView(new UiSelector().resourceId(\""
                                + appPackage() + ":id/dd_recipients_recycler\"))"));
                sleep(500);
                return;
            } catch (WebDriverException ignored) { }
        }
        // Last resort: a plain upward swipe over the dialog body.
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100, "top", (int) (size.height * 0.35),
                    "width", 200, "height", (int) (size.height * 0.3),
                    "direction", "up", "percent", 0.7));
            sleep(600);
        } catch (Exception ignored) { }
    }

    public int recipientCount() { return recipientEmails().size(); }

    /** Removes a recipient row through its × button. */
    public boolean removeRecipient(String email) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_recipients_recycler']"
                            + "//*[.//*[@resource-id='" + appPackage() + ":id/dd_email' and @text="
                            + xpathLiteral(email) + "]]"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_remove']")).click();
            sleep(400);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /**
     * The blocking overlay driven by the VM's three BusyControllers (seed / add-recipients
     * / save). It swallows every touch, so a tap that "did nothing" was probably this.
     */
    public boolean busyOverlayShown() {
        return isVisible("dd_preset_processing_overlay", Duration.ofSeconds(2));
    }

    public boolean tapSave()   { return revealSave() && click("dd_save"); }
    public boolean tapCancel() { return click("dd_cancel"); }

    /**
     * Save and wait for the dialog to actually CLOSE.
     *
     * <p>The dialog dismisses itself only on {@code result.onSuccess}; a failed save shows
     * a snackbar and stays put. So "it closed" is the app's own created/updated signal and
     * the one thing separating a saved list from a rejected one.
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

    // ---------------- the composer's "Pick a list" chooser ----------------

    /**
     * Taps the composer's Lists button.
     *
     * <p>{@code dd_btn_lists} is BOTH the library's shortcut chip and the composer's
     * button — the id alone cannot say which screen you are on (trap 3.3 in miniature).
     * Only call this with the composer on screen.
     */
    public boolean openListPickerInComposer() { return click("dd_btn_lists"); }

    /** The chooser is a MaterialAlertDialog with setItems() — matched on its title. */
    public boolean listPickerShown(Duration timeout) {
        return textShown("Pick a list", timeout);
    }

    /**
     * Picks a list from the chooser.
     *
     * <p>The row label is {@code "<name> (<recipientCount>)"} — the count is a SERVER
     * field, so it is matched by prefix rather than assumed to be the number of recipients
     * this suite happened to add.
     */
    /**
     * Picks a list from the ALREADY-OPEN chooser.
     *
     * <p>Do not poll this dialog waiting for it to fill up — it never will.
     * {@code EmailComposerFragment.showListPicker()} builds a MaterialAlertDialog with
     * {@code setItems(...)} from {@code viewModel.state.value.presets}, so its rows are a
     * SNAPSHOT taken at construction. If the composer's state has not hydrated yet the
     * dialog shows only "Pick a list" and "+ Create new list" — and stays that way however
     * long you watch it. (A twelve-second poll here proved exactly that: same two rows at
     * the end as at the start.) The retry belongs at the OPEN, not here — see
     * {@link #chooseListReopening}.
     */
    public boolean chooseListInPicker(String name) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textStartsWith(\"" + name + "\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            // Say what WAS on offer. "Not found" on its own cannot distinguish a list the
            // app failed to show from a chooser that never rendered from a name this suite
            // got wrong — and G-10 failed with no output at all to tell them apart.
            System.out.println(">>> chooseListInPicker('" + name + "') MISSED; chooser offers "
                    + pickerItems());
            return false;
        }
    }

    /**
     * Opens the composer's list chooser and picks {@code name}, REOPENING it while the
     * composer's state catches up.
     *
     * <p>The dialog snapshots {@code state.presets} when it is built, so a chooser opened
     * too early can only ever be thrown away and built again. Each attempt dismisses and
     * re-opens; that is the only thing that makes a newly-saved list appear.
     */
    public boolean chooseListReopening(String name, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (!openListPickerInComposer()) { sleep(1200); continue; }
            if (!listPickerShown(Duration.ofSeconds(8))) { sleep(1200); continue; }
            if (chooseListInPicker(name)) return true;
            System.out.println(">>> chooseListReopening attempt " + (i + 1) + "/" + attempts
                    + ": chooser held " + pickerItems() + " — dismissing and rebuilding it");
            dismissListPicker();
            sleep(2000);
        }
        return false;
    }

    /** Closes the chooser without picking anything. */
    public void dismissListPicker() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        sleep(600);
    }

    /** Every row label currently in the chooser — diagnostic only. */
    public List<String> pickerItems() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(
                    AppiumBy.className("android.widget.CheckedTextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
            if (out.isEmpty()) {
                for (WebElement e : driver.findElements(
                        AppiumBy.className("android.widget.TextView"))) {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) out.add(t);
                    if (out.size() >= 15) break;
                }
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** The app's shared confirmation dialog — both families (trap 3.1 / 3.15). */
    public ConfirmPopupPage confirmPopup() { return new ConfirmPopupPage(driver); }

    // ---------------- helpers ----------------

    private boolean presentNow(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private List<String> textsOf(String id) {
        try {
            return driver.findElements(byId(id)).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
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
            // settleImeSafely, NOT hideKeyboardIfShown: the editor is a cancelable
            // dialog, and the guarded hideKeyboard() can still fire a BACK while the IME
            // animates away — dismissing the EDITOR, after which Save reads as "disabled"
            // for a button that no longer exists.
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
