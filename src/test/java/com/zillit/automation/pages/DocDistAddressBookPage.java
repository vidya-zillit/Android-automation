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
 * DOCUMENT DISTRIBUTION — the ADDRESS BOOK ({@code addressBookFragment} /
 * fragment_dd_address_book.xml) and its child CONTACT DETAIL screen
 * ({@code contactDetailFragment} / fragment_dd_contact_detail.xml).
 *
 * <p>Reached from the library's Contacts shortcut
 * ({@code LibraryFragment: ddBtnContacts → navigate(addressBookFragment)}), which is a
 * plain navigation — no rights gate on the way IN. The gates are on the ACTIONS:
 * {@code runWithPostingRights} wraps Add / Send / Edit / Delete, and
 * {@code runWithDownloadRights} wraps Export CSV. Both surface the app's CUSTOM
 * confirmation family ({@code showDialogWithButton} → {@code leftButton}/{@code rightButton}),
 * so a rights-less user gets a modal — see {@link #confirmPopup()}.
 *
 * <p><b>Why the identity checks look the way they do.</b> {@code dd_recycler} is shared by
 * NINE screens in this module and {@code dd_name} / {@code dd_email} are shared by the
 * library rows, the composer attachments, the CSV preview and this screen's own rows — so
 * neither can say "am I on the address book". The two ids that CAN are:
 *
 * <ul>
 *   <li>{@code dd_address_export} — declared in fragment_dd_address_book.xml and nowhere
 *       else in the whole app. This is the address book's fingerprint.</li>
 *   <li>{@code dd_copy} / {@code dd_usage} / {@code dd_mutate_row} / {@code dd_sent_*} —
 *       declared only in fragment_dd_contact_detail.xml. These are the detail's.</li>
 * </ul>
 *
 * {@code dd_add} is NOT enough on its own: the CSV-import sheet, the preset editor and the
 * watermark batch screen all declare it too.
 *
 * <p><b>Two source behaviours shape every case built on this page.</b>
 *
 * <ul>
 *   <li><b>Saving a NEW contact leaves this screen.</b>
 *       {@code AddressBookViewModel.createContact} emits {@code Event.Created}, and
 *       {@code AddressBookFragment} answers it with {@code openContact(contact)} — the app
 *       navigates straight to the contact detail. "Did the list gain a row?" is therefore
 *       NOT the create's success signal; "did the detail open" is. (The dedupe path does
 *       the same thing with an "already exists — opened it" snackbar.)</li>
 *   <li><b>Search is CLIENT-side.</b> {@code AddressBookState.visible} filters the already
 *       loaded list on {@code name} / {@code email} / list names — it never re-queries. So
 *       unlike the library (trap 3.12, where search sees a create the list has not caught up
 *       with) search here can only find what the last {@code /contacts} response contained:
 *       {@link #waitForContact} has to REFRESH between attempts, not just re-type.</li>
 * </ul>
 */
public class DocDistAddressBookPage extends BasePage {

    public DocDistAddressBookPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- identity ----------------

    /**
     * True on the ADDRESS BOOK specifically.
     *
     * <p>Pairs the screen-unique {@code dd_address_export} with {@code dd_recycler} rather
     * than trusting either alone: the export button is the fingerprint, the recycler proves
     * the list surface is actually attached.
     */
    public boolean isOpen(Duration timeout) {
        return isVisible("dd_address_export", timeout) && present("dd_recycler");
    }

    /** True on the CONTACT DETAIL. {@code dd_copy} exists in no other layout. */
    public boolean isDetailOpen(Duration timeout) {
        return isVisible("dd_copy", timeout) || isVisible("dd_usage", Duration.ofSeconds(2));
    }

    /**
     * Waits until the list is actually READABLE — rows rendered, or the empty text up.
     *
     * <p>{@code dd_progress} is declared {@code visibility="visible"} in the layout and only
     * ever HIDDEN by {@code render()} (ZL-19555, so there is no empty-text flash before the
     * first {@code /contacts} response). Asserting "no contacts" against the spinner frame
     * is therefore a guaranteed false negative.
     */
    public boolean waitListReady(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!contactEmails().isEmpty()) return true;
            if (isEmptyStateShown() && !isLoading()) return true;
            sleep(300);
        }
        return false;
    }

    public boolean isLoading()        { return anyDisplayed("dd_progress"); }
    public boolean isEmptyStateShown(){ return anyDisplayed("dd_empty"); }

    /**
     * The empty text, which is filter-aware (ZL-19487): "No contacts yet." when the address
     * book is genuinely empty, "No matching contacts." when a search matched nothing.
     */
    public String emptyText() { return textOf("dd_empty"); }

    // ---------------- header actions ----------------

    public boolean hasAddFab()   { return present("dd_add"); }
    public boolean hasExport()   { return present("dd_address_export"); }

    /** {@code render()}: {@code ddAdd.isEnabled = !state.isSaving}. */
    public boolean isAddEnabled() { return enabled("dd_add"); }

    /** {@code render()}: {@code ddAddressExport.isEnabled = state.canExport} (loaded AND non-empty). */
    public boolean isExportEnabled() { return enabled("dd_address_export"); }

    public String exportLabel() { return textOf("dd_address_export"); }

    /** Opens the contact editor. Gated by {@code runWithPostingRights} — may pop a dialog. */
    public boolean openAdd()    { return click("dd_add"); }

    /** Fires the CSV export. Gated by {@code runWithDownloadRights} — may pop a dialog. */
    public boolean tapExport()  { return click("dd_address_export"); }

    // ---------------- search ----------------

    public boolean hasSearch() { return present("dd_search_input"); }

    public boolean search(String text) {
        try {
            WebElement f = driver.findElement(byId("dd_search_input"));
            f.click();
            f.clear();
            if (!text.isEmpty()) f.sendKeys(text);
            hideKeyboardIfShown();
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean clearSearch() { return search(""); }

    // ---------------- rows ----------------

    /**
     * Rendered contact emails, scoped to {@code dd_row} so a stray {@code dd_email}
     * elsewhere in the tree (the editor sheet's INPUT field carries the same id, and it
     * floats in its own window over this list) cannot be mistaken for a row.
     */
    public List<String> contactEmails() { return rowTexts("dd_email"); }

    /** Rendered contact names — the row's {@code dd_name} shows {@code Contact.displayName}. */
    public List<String> contactNames()  { return rowTexts("dd_name"); }

    private List<String> rowTexts(String childId) {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                                    + "//*[@resource-id='" + appPackage() + ":id/" + childId + "']"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public int rowCount() { return contactEmails().size(); }

    /**
     * The full list, scroll-collected — the address book is a RecyclerView, so rows below
     * the fold are simply ABSENT from the tree (trap 3.4). Asserting absence against one
     * screenful is how "the contact was never created" gets reported for a contact that is
     * plainly there, three swipes down.
     */
    public List<String> allEmailsScrolled() {
        Set<String> seen = new LinkedHashSet<>(contactEmails());
        int stable = 0;
        for (int i = 0; i < 12 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(contactEmails());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 12; i++) swipe(0.35, 0.75);
        return new ArrayList<>(seen);
    }

    public boolean hasContact(String email) {
        return allEmailsScrolled().stream().anyMatch(e -> e.equalsIgnoreCase(email));
    }

    /**
     * Waits for a contact to appear.
     *
     * <p>Search goes first because it is cheap, but — unlike the library's
     * {@code waitForItem} — search here CANNOT see a create the list has not fetched yet
     * ({@code AddressBookState.visible} filters an in-memory list). So every miss is
     * followed by a real {@link #pullToRefresh()}, which re-runs {@code /contacts}.
     */
    public boolean waitForContact(String email, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (contactEmails().stream().anyMatch(e -> e.equalsIgnoreCase(email))) return true;
            if (search(email)) {
                boolean found = contactEmails().stream().anyMatch(e -> e.equalsIgnoreCase(email));
                clearSearch();
                if (found) return true;
            }
            if (hasContact(email)) return true;
            pullToRefresh();
            sleep(350);
        }
        return false;
    }

    /** Opens the contact whose row carries this email. */
    public boolean openContact(String email) {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 20; i++) {
                if (tapRow(email, "dd_row")) return true;
                swipe(0.75, 0.35);
            }
            for (int i = 0; i < 12; i++) swipe(0.35, 0.75);
        }
        return false;
    }

    /** The row's ✉ button — {@code AddressBookAdapter.onSend} → composer pre-seeded with TO. */
    public boolean tapRowSend(String email) { return tapRow(email, "dd_send"); }

    /**
     * Anchors on the ROW that carries this email, then taps either the row itself or one of
     * its children.
     *
     * <p>Anchoring matters: pairing two flat id lists by index is how the wrong contact gets
     * opened, because {@code dd_send} only renders on contact rows while {@code dd_email} also
     * turns up in other row types — the n-th email and the n-th ✉ are not the same row.
     */
    private boolean tapRow(String email, String targetId) {
        String row = "//*[@resource-id='" + appPackage() + ":id/dd_row']"
                + "[.//*[@resource-id='" + appPackage() + ":id/dd_email' and @text=" + xpathLiteral(email) + "]]";
        String xpath = "dd_row".equals(targetId)
                ? row
                : row + "//*[@resource-id='" + appPackage() + ":id/" + targetId + "']";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            sleep(500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean pullToRefresh() {
        try {
            driver.findElement(byId("dd_swipe"));
            swipe(0.35, 0.80);
            sleep(500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- contact DETAIL ----------------
    //
    // The detail is the address book's own child destination (same nav graph, opened by
    // AddressBookFragment.openContact) and shares nothing with any other screen, so its
    // accessors live here rather than in a third page object.
    //
    // NOTE ON IDS: dd_name / dd_email mean something DIFFERENT here than on the list — on
    // the detail they are the single contact's header fields, not row children. That is safe
    // only because navigate() destroys the address book's view: the two never coexist. When
    // the EDITOR sheet floats over this screen they DO coexist, which is why
    // DocDistContactEditorSheet scopes every field under its own *_layout wrapper.

    /** {@code render()}: {@code ddName.text = contact.displayName}. */
    public String detailName()  { return textOf("dd_name"); }

    /** Rendered as {@code "✉  " + email} — assert with contains(), never equals(). */
    public String detailEmail() { return textOf("dd_email"); }

    /** Rendered as {@code "👤  " + job}, and GONE when the job is blank. */
    public String detailJob()   { return textOf("dd_job"); }
    public boolean detailJobShown() { return anyDisplayed("dd_job"); }

    /** {@code "🔥 " + getString(dd_used_in, usageCount)} — always visible. */
    public String detailUsage() { return textOf("dd_usage"); }

    /**
     * The DISTRIBUTION LISTS chip group. {@code renderLists()} always appends a
     * "+ Add to list…" chip, so this group is never empty even for a brand-new contact —
     * "chips.size() &gt; 0" proves nothing about membership. Read the texts instead.
     */
    public List<String> detailListChips() {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/dd_lists']/*"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    public boolean hasListsSection() { return present("dd_lists_title") && present("dd_lists"); }

    // -- Sent emails section (three-state: loading → empty → populated) --

    public boolean hasSentSection()   { return present("dd_sent_title"); }
    public boolean sentLoading()      { return anyDisplayed("dd_sent_loading"); }
    public boolean sentEmptyShown()   { return anyDisplayed("dd_sent_empty"); }
    public boolean sentListShown()    { return anyDisplayed("dd_sent_recycler"); }

    /** Waits out {@code state.sentEmailsLoading} so the three-state render has settled. */
    public boolean waitSentSectionSettled(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!sentLoading() && (sentEmptyShown() || sentListShown())) return true;
            sleep(600);
        }
        return false;
    }

    public int sentEmailCount() {
        try {
            return driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_sent_recycler']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_subject']")).size();
        } catch (WebDriverException e) { return 0; }
    }

    // -- detail actions --

    public boolean hasCopy()      { return present("dd_copy"); }
    public boolean hasDetailSend(){ return present("dd_send"); }
    public boolean hasMutateRow() { return present("dd_mutate_row"); }
    public boolean hasEdit()      { return present("dd_edit"); }
    public boolean hasDelete()    { return present("dd_delete"); }

    public boolean tapCopy()       { return click("dd_copy"); }
    public boolean tapDetailSend() { return click("dd_send"); }
    public boolean tapEdit()       { return click("dd_edit"); }
    public boolean tapDelete()     { return click("dd_delete"); }

    /**
     * The Copy confirmation the APP owns.
     *
     * <p>{@code copyEmail()} only shows this snackbar when
     * {@code SDK_INT < TIRAMISU} — from Android 13 the platform draws its own clipboard
     * preview and the app deliberately stays quiet, so an absent snackbar on a modern
     * device is CORRECT behaviour, not a failure.
     */
    public boolean copyConfirmationShown(Duration timeout) {
        return textShown("Email copied", timeout);
    }

    /** The post-delete snackbar ({@code dd_contact_deleted}). */
    public boolean deletedConfirmationShown(Duration timeout) {
        return textShown("Contact deleted", timeout);
    }

    /** The post-edit snackbar ({@code dd_contact_updated}). */
    public boolean updatedConfirmationShown(Duration timeout) {
        return textShown("Contact updated", timeout);
    }

    // ---------------- Export CSV → the SYSTEM save picker ----------------
    //
    // Export is a SAF round-trip: ActivityResultContracts.CreateDocument("text/csv")
    // launched with AddressBookCsvBuilder.FILENAME ("Address Book.csv"), and the bytes are
    // written to whatever URI the user picks. The picker is DocumentsUI — a FOREIGN
    // package, so byId() (which prefixes OUR package) matches nothing in it. Everything
    // below matches on visible TEXT, exactly like DriveUploadPage.

    /** {@code AddressBookCsvBuilder.FILENAME} — the name the app hands to the picker. */
    public static final String CSV_FILENAME = "Address Book.csv";

    /**
     * The app's post-write snackbar, composed exactly as
     * {@code getString(R.string.dd_address_export_saved, AddressBookCsvBuilder.FILENAME)}
     * with {@code dd_address_export_saved = "Saved %1$s"}.
     *
     * <p>Matched WHOLE, never on the filename alone: the picker's own name field carries
     * "Address Book.csv" as well, so a filename-only probe would go green against a save
     * dialog that has not written a byte.
     */
    private static final String SAVED_SNACKBAR = "Saved " + CSV_FILENAME;

    /** The picker's own name — Drive learned this the hard way; reuse the same probe. */
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

    public boolean stillInSystemPicker() {
        try {
            String pkg = driver.getCurrentPackage();
            return pkg != null && pkg.contains("documentsui");
        } catch (WebDriverException e) { return false; }
    }

    /** True when the save sheet shows the pre-filled name the app asked for. */
    public boolean pickerShowsFileName(String name, Duration timeout) {
        return textShown(name, timeout);
    }

    /**
     * Commits the save and RETURNS IMMEDIATELY.
     *
     * <p>It used to {@code sleep(350)} after the tap, and that sleep is the whole reason
     * I-13 failed on its first run. The confirmation this action produces is a
     * {@code Snackbar.LENGTH_LONG} — about 2.75 s of screen time — that
     * {@code AddressBookFragment}'s SAF callback raises the instant
     * {@code openOutputStream(uri).write(bytes)} returns, i.e. while DocumentsUI is still
     * animating away. Two seconds of sleeping spends nearly all of that window before the
     * first UiAutomator scan even starts; the recorded run then burned its whole 25 s
     * timeout plus a 3 s fallback looking for a snackbar that had already dismissed itself.
     * The caller must go straight from here into
     * {@link #exportSavedConfirmationShown(Duration)}.
     *
     * <p>Both label spellings are tried in ONE selector pass rather than one lookup each,
     * so a wrong first guess does not cost a full hierarchy scan out of that same window.
     */
    public boolean saveInSystemPicker() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)\\\\s*(save|done)\\\\s*\")")).click();
            return true;
        } catch (WebDriverException ignored) {
            // Older DocumentsUI builds expose the label with different casing only.
        }
        for (String label : new String[]{"SAVE", "Save", "DONE", "Done"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + label + "\")")).click();
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    /** Backs out of the picker — the recovery path, so it never throws. */
    public void cancelSystemPicker() {
        for (int i = 0; i < 4 && stillInSystemPicker(); i++) {
            try { driver.navigate().back(); Thread.sleep(350); }
            catch (Exception e) { return; }
        }
    }

    /**
     * The app's post-write confirmation — {@code "Saved Address Book.csv"}.
     *
     * <p>{@code AddressBookFragment} emits it ONLY from the {@code ok} branch of its SAF
     * callback, after {@code openOutputStream(uri).write(bytes)} came back without
     * throwing; the failure branch says "Failed to export" instead. So this string is the
     * write, and it is the furthest a test can follow the bytes — they land at a URI chosen
     * inside a foreign package that nothing here can read back.
     *
     * <p>Two things this deliberately does NOT do. It does not fall back to a bare "Saved",
     * and it does not match the filename on its own: the save picker's name field carries
     * "Address Book.csv" too, so either shortcut could report success against DocumentsUI
     * before anything was written. And it polls with a plain {@code findElements} rather
     * than a {@code WebDriverWait}, because the target lives for ~2.75 s and every wasted
     * half-second of setup is a real chance of missing it — call this the instant
     * {@link #saveInSystemPicker()} returns, with nothing in between.
     */
    public boolean exportSavedConfirmationShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        do {
            try {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + SAVED_SNACKBAR + "\")")).isEmpty()) {
                    return true;
                }
            } catch (WebDriverException ignored) {
                // Mid-transition between the picker and the app — try again.
            }
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    // ---------------- shared plumbing ----------------

    /**
     * The app's confirmation dialog. BOTH families turn up around this screen:
     * the CUSTOM one ({@code leftButton}/{@code rightButton}) for the rights prompts that
     * {@code runWithPostingRights} / {@code runWithDownloadRights} raise, and the FRAMEWORK
     * one ({@code android:id/button1}) for "Delete contact / Delete this contact?" and the
     * remove-from-list confirm. {@link ConfirmPopupPage} handles both; an unhandled modal
     * takes out every later case.
     */
    public ConfirmPopupPage confirmPopup() { return new ConfirmPopupPage(driver); }

    /** Leaves the detail (or the list) by BACK, without caring which one is up. */
    public boolean back() {
        try { driver.navigate().back(); Thread.sleep(250); return true; }
        catch (Exception e) { return false; }
    }

    // ---------------- helpers ----------------

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean anyDisplayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    private boolean enabled(String id) {
        try { return "true".equalsIgnoreCase(driver.findElement(byId(id)).getAttribute("enabled")); }
        catch (WebDriverException e) { return false; }
    }

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(400); return true; }
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
