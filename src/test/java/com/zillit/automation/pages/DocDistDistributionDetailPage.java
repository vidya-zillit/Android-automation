package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCUMENT DISTRIBUTION — the DISTRIBUTION DETAIL
 * ({@code distributionDetailFragment} / fragment_dd_distribution_detail.xml), reached by
 * tapping a row in {@link DocDistHistoryPage}.
 *
 * <p><b>Identity.</b> {@code dd_recycler} is not on this screen at all — the recipients
 * list is {@code dd_recipients} — but the module-wide rule still applies: pair on
 * something screen-unique. Two signals, and they mean different things:
 * <ul>
 *   <li>{@code dd_duplicate_btn} — the action row lives OUTSIDE the scroll view, so it is
 *       painted immediately. "The detail screen is up." ({@link #isOpen})</li>
 *   <li>{@code dd_donut} — inside {@code dd_scroll}, which the fragment leaves
 *       {@code View.GONE} until the record actually loads. "The record arrived."
 *       ({@link #isLoaded})</li>
 * </ul>
 * Asserting content against {@link #isOpen} alone reads an empty screen as a missing
 * field.
 *
 * <p><b>{@code dd_message_id} DOES NOT EXIST on this build.</b> The TextView is commented
 * out in fragment_dd_distribution_detail.xml and so is the {@code binding.ddMessageId}
 * block in {@code DistributionDetailFragment.render()}. The case table lists it under
 * "Distribution detail" — that is stale. The message id is still reachable per RECIPIENT
 * through the long-press copy menu ({@code menu/dd_recipient_copy}), which is where it
 * now lives.
 *
 * <p><b>Duplicate does NOT copy recipients.</b> {@code onDuplicateReady()} seeds the
 * composer with subject, body, attachments and the watermark trio, and the source says
 * why in as many words: "Recipients intentionally NOT seeded — the whole point of
 * Duplicate is 'send to someone new'." A case asserting "same subject/recipients" would
 * be asserting a bug.
 *
 * <p><b>Three of the four actions are rights-gated at CLICK time</b>, not by visibility
 * (the "QA flip"): Duplicate and Save-as-list run inside {@code runWithPostingRights},
 * Export CSV inside {@code runWithDownloadRights}. Without the right the tap raises the
 * app's request-permission dialog — {@link ConfirmPopupPage}'s custom family — instead of
 * doing anything. Every action helper here therefore expects the caller to check
 * {@link #rightsDialogShown()} before concluding the feature is broken.
 *
 * <p><b>The legend is built in code and always has six rows</b> (Opened · Delivered ·
 * Sending · Rejected · Bounced · Failed), whatever the data — {@code buildLegend()} walks
 * a fixed {@code legendOrder} list. Its numbers deliberately do not sum to the recipient
 * total: Delivered counts accepted + opened, so an opened recipient is in both.
 */
public class DocDistDistributionDetailPage extends BasePage {

    /** The six legend rows the fragment always renders, in order. */
    public static final List<String> LEGEND_LABELS =
            List.of("Opened", "Delivered", "Sending", "Rejected", "Bounced", "Failed");

    /** Title of the Save-as-list prompt (R.string.dd_history_save_list_prompt_title). */
    public static final String SAVE_LIST_PROMPT_TITLE = "Name your distribution list";

    public DocDistDistributionDetailPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- identity ----------------

    /** The screen is up (action row painted) — says nothing about the record. */
    public boolean isOpen(Duration timeout) {
        return isVisible("dd_duplicate_btn", timeout);
    }

    /** The record LOADED — dd_scroll revealed, so the donut and the fields exist. */
    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (displayed("dd_donut")) return true;
            sleep(400);
        }
        return false;
    }

    // ---------------- header fields ----------------

    public String subject() { return textOf("dd_subject"); }
    public String date()    { return textOf("dd_date"); }

    /**
     * The shared common_toolbar header. {@code render()} pushes the SUBJECT into it, which
     * is also what distinguishes this screen from History ("History") in a header check.
     */
    public String headerText() { return textOf("headerText"); }

    /**
     * Present only on a build that still has the commented-out TextView. Kept so a run
     * against an older APK reports the difference rather than silently agreeing with a
     * stale case table.
     */
    public boolean hasMessageId() { return present("dd_message_id"); }
    public String messageId()     { return textOf("dd_message_id"); }

    // ---------------- donut + legend ----------------

    public boolean donutShown()  { return displayed("dd_donut"); }
    public boolean legendShown() { return displayed("dd_legend"); }

    /**
     * The legend's flat text, in render order: label, count, label, count …
     *
     * <p>The rows are built programmatically with no ids, so they can only be read as the
     * TextViews under {@code dd_legend} — the dot is a plain View and contributes nothing.
     */
    public List<String> legendTexts() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_legend']//android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** The number beside a legend label, or -1 when the label or its count is missing. */
    public int legendCount(String label) {
        List<String> texts = legendTexts();
        for (int i = 0; i < texts.size() - 1; i++) {
            if (texts.get(i).equalsIgnoreCase(label)) {
                try { return Integer.parseInt(texts.get(i + 1).trim()); }
                catch (NumberFormatException e) { return -1; }
            }
        }
        return -1;
    }

    // ---------------- optional blocks ----------------

    // The three optional blocks below sit between the donut and the recipients table, and
    // all three default to View.GONE — the fragment only ever reveals them. A GONE view is
    // absent from the accessibility tree, so PRESENCE is already the "is it shown" answer
    // here, and it is the safer test: isDisplayed() also goes false for a view merely
    // scrolled out of the NestedScrollView, which would read "the send had no attachments"
    // for a send that plainly did. Read them BEFORE scrolling down the screen.

    /** {@code dd_attachments} — GONE when the send carried no documents. */
    public boolean hasAttachments()  { return present("dd_attachments"); }
    public String attachmentsText()  { return textOf("dd_attachments"); }

    /** {@code dd_presets_used} — GONE unless the send applied at least one list. */
    public boolean hasPresetsUsed()  { return present("dd_presets_used"); }
    public String presetsUsedText()  { return textOf("dd_presets_used"); }

    /** {@code dd_error} — FAILED records, or QUEUED ones that carry an error. */
    public boolean hasErrorBanner()  { return present("dd_error"); }
    public String errorText()        { return textOf("dd_error"); }

    // ---------------- recipients ----------------

    public int recipientCount() { return recipientEmails().size(); }

    public List<String> recipientEmails() { return underRecipients("dd_email"); }
    public List<String> recipientNames()  { return underRecipients("dd_name"); }
    public List<String> recipientStatuses() { return underRecipients("dd_status"); }

    /** ZL-20299 pills — one per recipient a distribution list contributed. */
    public int presetTagCount() { return underRecipients("dd_preset_tag").size(); }

    /**
     * Anchored INSIDE {@code dd_recipients}. {@code dd_name} / {@code dd_email} /
     * {@code dd_status} are all reused elsewhere in the module, so an unanchored lookup
     * would happily collect from another screen's leftovers.
     */
    private List<String> underRecipients(String id) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_recipients']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + id + "']"))) {
                String t = e.getText();
                out.add(t == null ? "" : t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Scrolls the detail's NestedScrollView so the recipients table is on screen. */
    public void scrollToRecipients() {
        scrollToId("dd_recipients");
    }

    // ---------------- action row ----------------

    public boolean hasDuplicate()   { return present("dd_duplicate_btn"); }
    public boolean hasSaveAsList()  { return present("dd_save_as_list_btn"); }
    public boolean hasExportCsv()   { return present("dd_export_csv_btn"); }
    public boolean hasRefresh()     { return present("dd_refresh_btn"); }

    public boolean isDuplicateEnabled()  { return enabled("dd_duplicate_btn"); }
    public boolean isSaveAsListEnabled() { return enabled("dd_save_as_list_btn"); }
    public boolean isExportCsvEnabled()  { return enabled("dd_export_csv_btn"); }

    public boolean tapDuplicate()  { return click("dd_duplicate_btn"); }
    public boolean tapSaveAsList() { return click("dd_save_as_list_btn"); }
    public boolean tapExportCsv()  { return click("dd_export_csv_btn"); }
    public boolean tapRefresh()    { return click("dd_refresh_btn"); }

    // ---------------- dialogs raised by the action row ----------------

    public ConfirmPopupPage confirmPopup() { return new ConfirmPopupPage(driver); }

    /**
     * True when the dialog on screen is the app's REQUEST-PERMISSION one rather than a
     * feature dialog — i.e. the tap hit the rights gate. Distinguished by the absence of
     * the Save-as-list prompt title, because both are modal and both answer
     * {@code ConfirmPopupPage.isShown()}.
     */
    public boolean rightsDialogShown() {
        return confirmPopup().isShown(Duration.ofSeconds(3)) && !saveListPromptShown();
    }

    /** The Save-as-list name prompt (MaterialAlertDialog + an EditText). */
    public boolean saveListPromptShown() {
        return textShown(SAVE_LIST_PROMPT_TITLE, Duration.ofSeconds(4));
    }

    /** Whatever the prompt pre-filled — expected to be "&lt;subject&gt; — recipients". */
    public String saveListPrefill() {
        try {
            return driver.findElement(By.className("android.widget.EditText")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** Backs out of the Save-as-list prompt WITHOUT creating anything. */
    public boolean cancelSaveListPrompt() {
        return confirmPopup().cancel(Duration.ofSeconds(5));
    }

    // ---------------- navigation ----------------

    /** BACK to the history list — one pop, this is a nav destination, not an activity. */
    public boolean goBack() {
        try { driver.navigate().back(); sleep(500); return true; }
        catch (WebDriverException e) { return false; }
    }

    // ---------------- helpers ----------------

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    private boolean enabled(String id) {
        try { return driver.findElement(byId(id)).isEnabled(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(400); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
