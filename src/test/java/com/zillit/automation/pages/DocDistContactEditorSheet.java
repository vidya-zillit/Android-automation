package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * DOCUMENT DISTRIBUTION — the ADD / EDIT CONTACT sheet
 * ({@code ContactEditorBottomSheet} / bottomsheet_dd_contact_editor.xml).
 *
 * <p>Fields: email, name, job, with Cancel / Save. It is form-only — it owns no
 * networking, it just posts {@code {email, name, job, isEdit}} back over a FragmentResult
 * and dismisses. Whoever hosts it does the write:
 * {@code AddressBookFragment} for ADD, {@code ContactDetailFragment} for EDIT.
 *
 * <p><b>Validation is honest here, like the create-folder sheet.</b>
 * {@code updateSaveEnabled()} is the whole rule:
 *
 * <pre>
 *   ddSave.isEnabled = EmailValidator.isValid(ddEmail.text.trim())
 * </pre>
 *
 * so an invalid form DISABLES Save rather than hiding an error in an invisible
 * {@code TextInputLayout.error} — assert {@link #isSaveEnabled()}, no error text needed.
 * And {@code EmailValidator} is stricter than a regex: the address must match
 * {@code local@domain.tld} AND its TLD must be in the bundled IANA set, so
 * {@code wdd@aa.dd} is rejected even though it "looks like" an email. (It fails OPEN if the
 * raw TLD asset cannot be loaded — a packaging bug would make that case pass wrongly, which
 * is why the failure message says so.)
 *
 * <p><b>Name and job have NO validation at all</b> — only the email decides Save. A contact
 * with a blank name is legal; {@code Contact.displayName} then falls back to the part of
 * the address before the '@'.
 *
 * <p><b>Edit mode locks the email.</b> {@code isEdit} sets
 * {@code ddEmail.isEnabled = false} + helper text, and the host only ever calls
 * {@code updateContact(name, job)} — the address is the contact's stable identity and there
 * is no path that changes it. A case that "edits the email" is testing something the app
 * does not offer.
 *
 * <p><b>Why open-detection uses {@code dd_email_layout}.</b> This is the same trap that cost
 * C-3 (3.13b): {@code dd_name} is ALSO the id of every library row, every contact row, every
 * composer attachment and the create-folder sheet's own field, and {@code dd_title} is
 * shared with the publish sheet — matching either makes "the sheet is open" true against the
 * list BEHIND the sheet, and makes "the sheet has closed" impossible to observe.
 * {@code dd_email_layout} and {@code dd_job_layout} are declared in this layout and in no
 * other, so they are the only safe fingerprints.
 *
 * <p><b>Why every field is scoped under its wrapper.</b> A BottomSheetDialogFragment lives
 * in its OWN window, so the address book (or the contact detail) is still in the accessibility
 * tree underneath it — and both carry {@code dd_email} / {@code dd_name}. A bare
 * {@code findElement(byId("dd_email"))} can therefore return a row's TextView instead of this
 * sheet's EditText, silently typing nowhere. Each accessor below matches
 * {@code dd_*_layout // dd_*} instead.
 *
 * <p><b>NOTHING IN THIS PAGE MAY PRESS BACK TO HIDE THE KEYBOARD.</b> UiAutomator2
 * implements {@code hideKeyboard()} as a BACK press, and {@code hideKeyboardIfShown()} only
 * narrows that to "when {@code isKeyboardShown()} says a keyboard is up" — which is still a
 * BACK, and {@code isKeyboardShown()} reads true for a moment after the IME has already
 * begun closing. Inside a modal sheet that stray BACK does not hide a keyboard, it CANCELS
 * THE DIALOG: {@code BottomSheetDialog} is cancelable, so the sheet is simply gone, and the
 * NEXT call finds nothing — {@link #setJob} reports "field missing" and
 * {@link #isSaveEnabled()} reports "false" for a button that no longer exists. That is how
 * I-11 failed on its first run: the editor was open and Save was enabled in the screenshot
 * taken seconds earlier, and by the time {@code editAndSave} read the button the whole sheet
 * had been dismissed. {@code settleKeyboard()} replaces it with IME_ACTION_DONE, which the
 * focused editor consumes and the dialog never sees. There is no downside to leaving the IME
 * up either: {@code BottomSheetDialog} forces {@code SOFT_INPUT_ADJUST_RESIZE}, so Save and
 * Cancel are laid out ABOVE the keyboard and stay both visible and tappable.
 */
public class DocDistContactEditorSheet extends BasePage {

    public DocDistContactEditorSheet(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- open / closed ----------------

    public boolean waitOpen(Duration timeout) {
        return isVisible("dd_email_layout", timeout)
                || isVisible("dd_job_layout", Duration.ofSeconds(2));
    }

    public boolean isOpen() { return waitOpen(Duration.ofSeconds(3)); }

    /** True once the sheet is GONE — presence checks only, no visibility waits. */
    public boolean waitClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!presentNow("dd_email_layout") && !presentNow("dd_job_layout")) return true;
            sleep(500);
        }
        return false;
    }

    // ---------------- chrome ----------------

    /** "Add contact" in add mode, "Edit contact" in edit mode. */
    public String title() { return textOf("dd_title"); }

    /** "Save" in add mode, "Save changes" in edit mode. */
    public String saveLabel() { return textOf("dd_save"); }

    public boolean hasEmailField() { return presentNow("dd_email_layout"); }
    public boolean hasNameField()  { return field("dd_name_layout", "dd_name") != null; }
    public boolean hasJobField()   { return field("dd_job_layout", "dd_job") != null; }
    public boolean hasSaveButton() { return presentNow("dd_save"); }
    public boolean hasCancelButton() { return presentNow("dd_cancel"); }

    // ---------------- fields ----------------

    public boolean setEmail(String value) { return type("dd_email_layout", "dd_email", value); }
    public boolean setName(String value)  { return type("dd_name_layout", "dd_name", value); }
    public boolean setJob(String value)   { return type("dd_job_layout", "dd_job", value); }

    public String emailValue() {
        WebElement f = field("dd_email_layout", "dd_email");
        return f == null ? "" : f.getText();
    }

    public String nameValue() {
        WebElement f = field("dd_name_layout", "dd_name");
        return f == null ? "" : f.getText();
    }

    /**
     * False in EDIT mode — the address identifies the contact in lists and history, so the
     * sheet disables the input and explains why in helper text.
     */
    public boolean isEmailEditable() {
        WebElement f = field("dd_email_layout", "dd_email");
        if (f == null) return false;
        return "true".equalsIgnoreCase(f.getAttribute("enabled"));
    }

    /**
     * The locked-email helper text. Matched on a middle fragment of the sentence —
     * the full string carries a typographic apostrophe and an em dash
     * ("Email can't be changed — it identifies the contact in lists and history."),
     * neither of which survives a naive UiSelector literal.
     */
    public boolean emailLockedHelperShown() {
        return textShown("identifies the contact", Duration.ofSeconds(3));
    }

    /** The app's whole validation rule, readable straight off the button. */
    public boolean isSaveEnabled() {
        try { return "true".equalsIgnoreCase(driver.findElement(byId("dd_save")).getAttribute("enabled")); }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- actions ----------------

    /**
     * Taps Save.
     *
     * <p>Settles the IME first — with the BACK-free helper, never {@code hideKeyboard()} —
     * so the tap cannot land on a keyboard key laid over the button row. If the IME cannot
     * be settled the tap still goes ahead: {@code BottomSheetDialog} resizes for the
     * keyboard, so the row is above it either way.
     */
    public boolean save()   { settleKeyboard(); return click("dd_save"); }

    public boolean cancel() { return click("dd_cancel"); }

    /**
     * The add-mode happy path: fill the form, prove Save became enabled, submit, and
     * return true only once the sheet has actually CLOSED.
     *
     * <p>The sheet dismisses itself the moment it posts the FragmentResult, so "it closed"
     * means the FORM was accepted — NOT that the contact was created. The create is the
     * host's job and lands asynchronously; its own signal is the app navigating to the new
     * contact's detail ({@code Event.Created → openContact}).
     */
    public boolean fillAndSave(String email, String name, String job) {
        if (!setEmail(email)) return false;
        if (name != null && !setName(name)) return false;
        if (job != null && !setJob(job)) return false;
        if (!isSaveEnabled()) return false;
        if (!save()) return false;
        return waitClosed(Duration.ofSeconds(12));
    }

    /**
     * The edit-mode happy path — email is locked, so only name / job are touched.
     *
     * <p>Note what is NOT re-derived here: Save's enabled state. {@code updateSaveEnabled()}
     * is wired to {@code ddEmail.doAfterTextChanged} and nothing else, and edit mode locks
     * the email — so Save's state is fixed the moment the sheet opens and typing a name or
     * a job cannot change it. The check below is a liveness probe, not a validation step:
     * with the sheet open it can only be false if the button vanished with the sheet (see
     * the class note on BACK presses), which is why a case is better off asserting the
     * steps individually than reading one boolean out of here.
     */
    public boolean editAndSave(String name, String job) {
        if (name != null && !setName(name)) return false;
        if (job != null && !setJob(job)) return false;
        if (!isSaveEnabled()) return false;
        if (!save()) return false;
        return waitClosed(Duration.ofSeconds(12));
    }

    /**
     * Dismisses the sheet however it can — recovery path, never throws.
     *
     * <p>Gated on an INSTANT presence check rather than {@link #isOpen()}: this runs from an
     * @AfterEach on every case, and a visibility wait would spend five seconds proving
     * nothing is open, fourteen times over.
     */
    public void dismissQuietly() {
        for (int i = 0; i < 3 && (presentNow("dd_email_layout") || presentNow("dd_job_layout")); i++) {
            if (cancel()) { waitClosed(Duration.ofSeconds(4)); continue; }
            try { driver.navigate().back(); Thread.sleep(350); } catch (Exception e) { return; }
        }
    }

    // ---------------- helpers ----------------

    /** The input INSIDE this sheet's wrapper, never a same-id view in the window behind. */
    private WebElement field(String layoutId, String fieldId) {
        try {
            return driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + layoutId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + fieldId + "']"));
        } catch (WebDriverException e) {
            return null;
        }
    }

    private boolean type(String layoutId, String fieldId, String value) {
        WebElement f = field(layoutId, fieldId);
        if (f == null) return false;
        try {
            f.click();
            f.clear();
            if (!value.isEmpty()) f.sendKeys(value);
            // NOT hideKeyboardIfShown() — see the class note. That is a BACK press, and a
            // BACK that arrives once the IME has already started closing cancels this
            // BottomSheetDialog outright.
            settleKeyboard();
            sleep(600);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Puts the soft keyboard away WITHOUT a BACK press, and never at the cost of the sheet.
     *
     * <p>IME_ACTION_DONE is delivered to the FOCUSED EDITOR, so the dialog never sees a key
     * event it could treat as a cancel. These fields declare no {@code imeOptions} and no
     * editor-action listener, so "done" does nothing beyond closing the IME.
     *
     * <p>When the driver cannot perform the action the keyboard is left UP on purpose:
     * {@code BottomSheetDialog} sets {@code SOFT_INPUT_ADJUST_RESIZE}, so the sheet — Save
     * and Cancel included — sits above the IME and stays usable. A visible keyboard costs
     * this page nothing; a dismissed sheet costs it the case.
     */
    private void settleKeyboard() {
        try {
            if (!driver.isKeyboardShown()) return;
            driver.executeScript("mobile: performEditorAction", java.util.Map.of("action", "done"));
        } catch (WebDriverException ignored) {
            // Nothing focused, or the action is unsupported — do NOT fall back to BACK.
        }
    }

    private boolean presentNow(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(350); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
