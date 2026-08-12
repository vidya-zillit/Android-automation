package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.ConfirmPopupPage;
import com.zillit.automation.pages.DocDistAddressBookPage;
import com.zillit.automation.pages.DocDistComposerPage;
import com.zillit.automation.pages.DocDistContactEditorSheet;
import com.zillit.automation.pages.DocDistLibraryPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — area I (address book &amp; contacts, I-1…I-14) of
 * DOC_DISTRIBUTION_TEST_CASES.md.
 *
 * <p>The suite owns exactly one contact for its whole life
 * ({@code QA_CT_<stamp>} / {@code qa.ct.<stamp>@example.com}), creates it in I-5 and
 * deletes it in the last case. The stamp keeps every run's data distinct, which matters
 * more here than in the folder suite: {@code AddressBookAdapter.Diff} keys rows on EMAIL,
 * so a reused address would collide with a leftover row from a previous run instead of
 * producing a second one.
 *
 * <p><b>Four source facts shape how these cases are written.</b>
 *
 * <ul>
 *   <li><b>Creating a contact NAVIGATES AWAY.</b> {@code AddressBookViewModel.createContact}
 *       emits {@code Event.Created} and {@code AddressBookFragment} answers it with
 *       {@code openContact(contact)} — so the app is on the contact DETAIL when the save
 *       lands, and "a new row appeared in the list" is not the create's signal. I-5 asserts
 *       the detail, then comes back and proves the row too.</li>
 *   <li><b>Validation is a DISABLED button, not an error message.</b>
 *       {@code ContactEditorBottomSheet.updateSaveEnabled()} is
 *       {@code ddSave.isEnabled = EmailValidator.isValid(email)} and nothing else — name and
 *       job are unvalidated. I-4 reads the button.</li>
 *   <li><b>Both confirmation families turn up on this screen.</b> The delete confirm is the
 *       FRAMEWORK one ({@code MaterialAlertDialogBuilder}, {@code android:id/button1} —
 *       trap 3.15), while the rights prompts that {@code runWithPostingRights} /
 *       {@code runWithDownloadRights} raise are the CUSTOM one
 *       ({@code leftButton}/{@code rightButton}). {@link ConfirmPopupPage} handles both, and
 *       {@link #skipIfRightsPrompt} tells them apart so a rights gate SKIPS while a real
 *       confirmation is answered.</li>
 *   <li><b>Search never re-queries.</b> {@code AddressBookState.visible} filters the loaded
 *       list client-side, so — unlike the library (trap 3.12) — search cannot see a create
 *       the list has not fetched. And the query lives in the ViewModel, which SURVIVES the
 *       trip to the detail and back while the recreated search box renders EMPTY: a stale
 *       filter with a blank-looking search field is how "the contact is gone" passes against
 *       a list that is merely filtered. Every case that asserts absence clears the search
 *       first.</li>
 * </ul>
 *
 * <p>Two spec rows do not survive contact with the source; both are handled in place rather
 * than faked — see {@link #copyEmailConfirms} (I-9) and
 * {@link #csvImportIsNotAnAddressBookAffordance} (I-14).
 */
@Order(TestOrder.DOC_DISTRIBUTION_CONTACTS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistContactsTest extends DocDistBaseTest {

    private static final long STAMP = System.currentTimeMillis() % 100000;

    private static final String NAME    = "QA_CT_" + STAMP;
    private static final String RENAMED = NAME + "_R";
    private static final String EMAIL   = "qa.ct." + STAMP + "@example.com";
    private static final String JOB     = "QA Automation";

    /**
     * Set the moment the contact is known to exist on the backend, BEFORE any assertion
     * that could abort the case — a leaked contact must still be picked up by the cleanup.
     */
    private static boolean created;

    @BeforeEach
    void reachAddressBook() {
        reachLibraryOrSkip();
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // The shortcut lives in the horizontally-scrolling action row, which is cut off
        // after two chips on a 1280-wide screen (trap 3.10) — hasContacts()/openContacts()
        // scroll it into view and rewind. An absent chip means a stale build (3.17), not a
        // broken app: skip rather than fail the whole area.
        assumeTrue(lib.hasContacts(), "This build's library offers no Contacts shortcut (trap 3.17)");
        assumeTrue(lib.openContacts(), "Could not tap the Contacts shortcut");

        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        assumeTrue(book.isOpen(Duration.ofSeconds(25)), "The address book did not open");
        book.waitListReady(Duration.ofSeconds(30));
    }

    /**
     * Unwinds anything a failed case left standing. A bottom sheet or a modal confirmation
     * survives the next @BeforeEach's back-presses and takes out every remaining case, which
     * is exactly how one bad assertion turns into thirteen.
     */
    @AfterEach
    void unwind() {
        try {
            new DocDistContactEditorSheet(driver).dismissQuietly();
            // DECLINE, never confirm — a blind "yes" on an unknown dialog can delete
            // something no case asked to delete.
            new ConfirmPopupPage(driver).clearIfBlocking();
        } catch (Exception ignored) {
            // Recovery only; the next @BeforeEach re-establishes the screen anyway.
        }
    }

    // ==================== I-1 · the address book ====================

    @Test
    @Order(1)
    @DisplayName("I-1: Contacts opens the address book — Add FAB, search and the export header")
    void contactsOpensAddressBook() {
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);

        // dd_recycler is shared by NINE screens (trap 3.3), so the screen is identified by
        // dd_address_export — declared in fragment_dd_address_book.xml and nowhere else in
        // the app. dd_add alone would not do: the CSV sheet, the preset editor and the
        // watermark batch screen all declare it too.
        assertTrue(book.isOpen(Duration.ofSeconds(10)), "Should be on the address book");
        assertTrue(book.hasAddFab(),
                "The Add-contact FAB is VISIBLE for the Contacts-tab entry — AddressBookFragment "
                        + "only hides it in the smart-list view-only mode (ZL-19466); rights gate "
                        + "the CLICK, not the visibility");
        assertTrue(book.hasSearch(), "The address book has a search field");
        assertTrue(book.hasExport(), "Export CSV is part of the header row");

        System.out.println(">>> address book: rows=" + book.rowCount()
                + " export='" + book.exportLabel() + "' exportEnabled=" + book.isExportEnabled()
                + " empty='" + book.emptyText() + "'");
        screenshot("ddc-address-book");
    }

    // ==================== I-3 · the editor opens ====================

    @Test
    @Order(2)
    @DisplayName("I-3: Add opens the contact editor with an editable email and a disabled Save")
    void addOpensContactEditor() {
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        DocDistContactEditorSheet sheet = openEditorOrSkip(book);
        try {
            assertTrue(sheet.hasSaveButton(), "The editor offers Save");
            assertTrue(sheet.hasCancelButton(), "The editor offers Cancel");
            assertTrue(sheet.hasEmailField(), "The editor offers the email field");
            assertTrue(sheet.hasNameField(), "The editor offers the name field");
            assertTrue(sheet.hasJobField(), "The editor offers the job field");

            assertTrue(sheet.isEmailEditable(),
                    "ADD mode leaves the email editable — only EDIT mode locks it");
            assertFalse(sheet.isSaveEnabled(),
                    "Save starts DISABLED (layout enabled=false + updateSaveEnabled on an empty email)");

            System.out.println(">>> editor: title='" + sheet.title() + "' save='" + sheet.saveLabel() + "'");
            screenshot("ddc-editor-add");
        } finally {
            sheet.dismissQuietly();
        }
    }

    // ==================== I-4 · email validation ====================

    @Test
    @Order(3)
    @DisplayName("I-4: Save stays DISABLED until the email is valid — shape AND a real TLD")
    void contactNeedsAValidEmail() {
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        DocDistContactEditorSheet sheet = openEditorOrSkip(book);
        try {
            assertFalse(sheet.isSaveEnabled(), "An empty email must leave Save disabled");

            assumeTrue(sheet.setEmail("not-an-email"), "Could not type into the email field");
            assertFalse(sheet.isSaveEnabled(), "'not-an-email' fails EmailValidator's shape regex");

            assumeTrue(sheet.setEmail("qa.ct@nowhere"), "Could not type into the email field");
            assertFalse(sheet.isSaveEnabled(),
                    "'qa.ct@nowhere' has no dotted TLD — the regex requires local@domain.tld");

            // The interesting one: structurally perfect, but '.dd' is not an IANA TLD.
            // EmailValidator checks the TLD against res/raw/dd_iana_tlds.txt so the client
            // matches the backend's Joi .email() byte-for-byte (this is the exact example
            // its KDoc calls out).
            assumeTrue(sheet.setEmail("wdd@aa.dd"), "Could not type into the email field");
            assertFalse(sheet.isSaveEnabled(),
                    "'wdd@aa.dd' is shape-valid but '.dd' is not in the bundled IANA TLD set. "
                            + "A pass here means IanaTlds.set() came back EMPTY and the validator "
                            + "failed OPEN — i.e. res/raw/dd_iana_tlds.txt did not ship, not that "
                            + "the address is acceptable.");

            // Name and job are NOT validated — only the email decides Save.
            assumeTrue(sheet.setEmail(""), "Could not clear the email field");
            assumeTrue(sheet.setName(NAME), "Could not type a name");
            assumeTrue(sheet.setJob(JOB), "Could not type a job");
            assertFalse(sheet.isSaveEnabled(),
                    "A name and a job cannot enable Save — updateSaveEnabled() reads the email alone");

            assumeTrue(sheet.setEmail(EMAIL), "Could not type the QA address");
            assertTrue(sheet.isSaveEnabled(), "A valid address must enable Save");
            screenshot("ddc-editor-validation");
        } finally {
            // Cancel — this case must create NOTHING; I-5 owns the create.
            sheet.dismissQuietly();
        }
        assertTrue(sheet.waitClosed(Duration.ofSeconds(8)), "Cancel should close the editor");
    }

    // ==================== I-5 · create ====================

    @Test
    @Order(4)
    @DisplayName("I-5: saving creates the contact — the app opens its detail, and the row is listed")
    void savingCreatesTheContact() {
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        DocDistContactEditorSheet sheet = openEditorOrSkip(book);
        assertTrue(sheet.fillAndSave(EMAIL, NAME, JOB),
                "A valid form should be accepted and the sheet should dismiss itself");

        // The create's OWN success signal. AddressBookFragment answers Event.Created with
        // openContact(created), so landing on the detail means the POST came back 2xx; a
        // failure would instead keep us on the list with an error snackbar. (The dedupe path
        // also lands here, with an "already exists — opened it" snack — impossible with a
        // stamped address, but worth knowing when reading a failure.)
        boolean detail = book.isDetailOpen(Duration.ofSeconds(30));
        if (detail) created = true;      // set BEFORE any assertion, so cleanup still runs
        System.out.println(">>> after save: detailOpen=" + detail
                + " name='" + book.detailName() + "' email='" + book.detailEmail() + "'");
        screenshot("ddc-created");
        assertTrue(detail, "Creating a contact should open its detail screen");
        assertTrue(book.detailEmail().contains(EMAIL),
                "The detail should show the address just saved. Got: '" + book.detailEmail() + "'");
        assertTrue(NAME.equalsIgnoreCase(book.detailName()),
                "The detail header shows Contact.displayName. Got: '" + book.detailName() + "'");

        // …and only NOW the list, which is a separate question: the address book behind us
        // re-runs /contacts, so the row can lag the navigation by a round-trip.
        assertTrue(book.back(), "Could not go back to the address book");
        assertTrue(book.isOpen(Duration.ofSeconds(20)), "BACK should return to the address book");
        boolean listed = book.waitForContact(EMAIL, Duration.ofSeconds(60));
        System.out.println(">>> address book now: " + book.contactEmails());
        assertTrue(listed, "The new contact should be listed. Visible: " + book.contactEmails());
    }

    // ==================== I-2 · search ====================

    @Test
    @Order(5)
    @DisplayName("I-2: search filters to matching rows, and a no-match shows the FILTER-aware empty text")
    void searchFiltersContacts() {
        assumeTrue(created, "Needs the QA contact from I-5");
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        assumeTrue(book.waitForContact(EMAIL, Duration.ofSeconds(45)), "The QA contact is not listed");

        assumeTrue(book.search(NAME), "Could not type in the search box");
        List<String> emails = book.contactEmails();
        List<String> names = book.contactNames();
        System.out.println(">>> search '" + NAME + "' → names=" + names + " emails=" + emails);
        assertTrue(emails.stream().anyMatch(e -> e.equalsIgnoreCase(EMAIL)),
                "Searching the contact's name should find it. Got: " + emails);
        // The real claim is "only matching rows". state.visible matches on name / email /
        // list name, and the stamped name cannot appear in a list name, so every surviving
        // row must carry it in one of the two fields the row actually renders.
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            String e = i < emails.size() ? emails.get(i) : "";
            assertTrue(containsIgnoreCase(n, NAME) || containsIgnoreCase(e, NAME),
                    "Row '" + n + "' / '" + e + "' does not match the query '" + NAME + "'");
        }

        assumeTrue(book.search("zzqq-no-such-contact-" + STAMP), "Could not type in the search box");
        assertTrue(book.isEmptyStateShown(), "A no-match search should show the empty state");
        // ZL-19487 — the empty text is filter-aware: "No matching contacts." while searching,
        // never the "No contacts yet." that would falsely imply an empty address book.
        String emptyText = book.emptyText();
        System.out.println(">>> no-match empty text = '" + emptyText + "'");
        screenshot("ddc-search");
        assertTrue(containsIgnoreCase(emptyText, "matching"),
                "A filtered empty list should read 'No matching contacts.'. Got: '" + emptyText + "'");

        // Leave no query behind — the VM keeps it across navigation while the recreated
        // search box renders empty, which would silently filter every later case.
        assertTrue(book.clearSearch(), "Could not clear the search box");
        assertTrue(book.waitListReady(Duration.ofSeconds(20)), "The unfiltered list should come back");
    }

    // ==================== I-6 · open the detail ====================

    @Test
    @Order(6)
    @DisplayName("I-6: tapping a contact row opens its detail with the name and email")
    void tappingAContactOpensDetail() {
        DocDistAddressBookPage book = openDetailOrSkip();
        System.out.println(">>> detail: name='" + book.detailName() + "' email='" + book.detailEmail() + "'");
        screenshot("ddc-detail");
        assertTrue(book.detailEmail().contains(EMAIL),
                "The detail's email line is rendered as '✉  <email>'. Got: '" + book.detailEmail() + "'");
        assertTrue(NAME.equalsIgnoreCase(book.detailName()) || RENAMED.equalsIgnoreCase(book.detailName()),
                "The detail header shows the contact's display name. Got: '" + book.detailName() + "'");
    }

    // ==================== I-7 · job / usage / lists ====================

    @Test
    @Order(7)
    @DisplayName("I-7: the detail shows job, usage and the distribution-lists section")
    void detailShowsJobUsageAndLists() {
        DocDistAddressBookPage book = openDetailOrSkip();

        // ddJob is GONE when the job is blank — this contact was created WITH one, so it
        // must render; an absent job here means the job never reached the backend.
        assertTrue(book.detailJobShown(),
                "dd_job is only hidden for a blank job, and this contact was saved with '" + JOB + "'");
        assertTrue(containsIgnoreCase(book.detailJob(), JOB),
                "The job line is rendered as '👤  <job>'. Got: '" + book.detailJob() + "'");

        // "🔥 " + getString(dd_used_in, usageCount) — always present, so assert it is
        // non-blank rather than guessing a count (trap 3.2's lesson, applied to usage).
        String usage = book.detailUsage();
        assertFalse(usage.isBlank(), "The usage line should render a 'Used in N emails / lists' string");

        assertTrue(book.hasListsSection(), "The DISTRIBUTION LISTS section should render");
        // renderLists() always appends a "+ Add to list…" chip, so a non-empty chip group
        // proves nothing about membership — read the texts.
        List<String> chips = book.detailListChips();
        System.out.println(">>> detail: job='" + book.detailJob() + "' usage='" + usage + "' chips=" + chips);
        screenshot("ddc-detail-fields");
        assertTrue(chips.stream().anyMatch(c -> c.contains("Add to list")),
                "The lists section always offers the '+ Add to list…' chip. Got: " + chips);
    }

    // ==================== I-8 · sent emails ====================

    @Test
    @Order(8)
    @DisplayName("I-8: the Sent emails section settles on either the list or its empty state")
    void sentEmailsRenderOrEmptyState() {
        DocDistAddressBookPage book = openDetailOrSkip();
        assertTrue(book.hasSentSection(), "The 'Sent emails' section title is always present");

        // Three-state render (loading → empty → populated). Asserting before the load
        // finishes would read the SPINNER frame and call it an empty state.
        assertTrue(book.waitSentSectionSettled(Duration.ofSeconds(30)),
                "The Sent emails section should stop loading and show one of its two states");

        boolean empty = book.sentEmptyShown();
        boolean list = book.sentListShown();
        int rows = book.sentEmailCount();
        System.out.println(">>> sent emails: empty=" + empty + " list=" + list + " rows=" + rows);
        screenshot("ddc-sent-emails");

        assertTrue(empty ^ list, "Exactly one of the empty state and the list should show, not both/neither");
        if (list) {
            assertTrue(rows > 0, "A shown Sent-emails list must actually contain rows");
        } else {
            // A contact created moments ago has never been emailed, so this is the expected
            // branch — the ^ above already proved the other one is hidden.
            assertTrue(rows == 0, "The empty state should not coexist with rendered rows");
        }
    }

    // ==================== I-9 · copy ====================

    @Test
    @Order(9)
    @DisplayName("I-9: Copy copies the email — asserted through the app's own confirmation, not the clipboard")
    void copyEmailConfirms() {
        DocDistAddressBookPage book = openDetailOrSkip();
        assertTrue(book.hasCopy(), "Copy is offered outside the smart-list view-only mode");

        assumeTrue(book.tapCopy(), "Could not tap Copy");
        boolean snack = book.copyConfirmationShown(Duration.ofSeconds(5));
        int major = androidMajor();
        System.out.println(">>> copy: androidMajor=" + major + " appSnackbar=" + snack);
        screenshot("ddc-copy");

        // WHY THE CLIPBOARD ITSELF IS NOT READ.
        // Since Android 10 only the FOREGROUND app may read the clipboard. Appium's
        // clipboard endpoint works around that by bringing its own io.appium.settings
        // helper to the front and back again — i.e. it BACKGROUNDS the app under test
        // mid-suite, and it lands us on an unknown screen if the restore misses. That is a
        // bad trade for one assertion, so this case asserts what the APP tells us instead.
        //
        // And what the app tells us is version-dependent: ContactDetailFragment.copyEmail()
        // shows its "Email copied" snackbar ONLY when SDK_INT < TIRAMISU, because from
        // Android 13 the platform draws its own clipboard preview and a second toast would
        // be duplicate chrome. So an absent snackbar on a modern device is CORRECT — the
        // hard assertion below only fires where the app promises one.
        assertTrue(book.isDetailOpen(Duration.ofSeconds(5)), "Copy must not navigate away from the detail");
        assertFalse(book.confirmPopup().isShown(Duration.ofSeconds(2)), "Copy must not raise a dialog");
        assertTrue(book.detailEmail().contains(EMAIL),
                "The contact's email should still be on screen after the copy");
        if (major > 0 && major < 13) {
            assertTrue(snack, "Below Android 13 the app shows its own 'Email copied' snackbar "
                    + "(copyEmail() gates it on SDK_INT < TIRAMISU)");
        }
    }

    // ==================== I-10 · send from a contact ====================

    @Test
    @Order(10)
    @DisplayName("I-10: Send from a contact opens the composer pre-filled with that address — and cancels out")
    void sendFromContactOpensPrefilledComposer() {
        DocDistAddressBookPage book = openDetailOrSkip();
        DocDistComposerPage composer = new DocDistComposerPage(driver);
        assertTrue(book.hasDetailSend(), "Send email is offered on the contact detail");

        try {
            assumeTrue(book.tapDetailSend(), "Could not tap Send email");
            skipIfRightsPrompt("Send from a contact");

            assertTrue(composer.isOpen(Duration.ofSeconds(25)),
                    "sendEmail() navigates to emailComposerFragment with ARG_INITIAL_TO");
            assertTrue(composer.isComposeStage(Duration.ofSeconds(5)),
                    "The composer should open on its COMPOSE stage, not the preview");

            List<String> chips = composer.chipTexts("dd_to_chips");
            System.out.println(">>> composer TO chips: " + chips);
            screenshot("ddc-send-prefilled");

            // WHAT A CHIP ACTUALLY SAYS — and why matching only the address is wrong.
            // EmailComposerFragment.renderChips() is:
            //     val contactName = nameByEmail[r.email.trim().lowercase()]
            //     val displayText = contactName?.takeIf { it.isNotBlank() } ?: r.email
            // so a recipient whose address matches an ADDRESS-BOOK entry renders as that
            // contact's NAME, and the address survives only as a TooltipCompat tooltip —
            // which UiAutomator does not dump in any form. This contact is in the address
            // book (I-5 put it there), so the chip reads "QA_CT_<stamp>", never the
            // address; the first run of this case failed on exactly that, with the
            // correctly pre-filled chip sitting in the screenshot next to the assertion.
            //
            // Accepting the NAME is not a weaker claim. nameByEmail is keyed BY EMAIL, so
            // the only way this contact's name can reach this chip is for the seeded
            // address to have been this contact's address — the name IS the proof that
            // ARG_INITIAL_TO carried it. The raw address stays accepted because /contacts
            // can land after the first chip render (the source comment: "chips initially
            // show raw emails then update to names"), and because I-11's rename has not
            // happened yet at this point in the ordered suite.
            assertTrue(chips.stream().anyMatch(c -> containsIgnoreCase(c, EMAIL)
                            || containsIgnoreCase(c, NAME)),
                    "ARG_INITIAL_TO should have seeded the contact as a TO chip, rendered "
                            + "as either '" + EMAIL + "' or its address-book name '" + NAME
                            + "'. Got: " + chips);
        } finally {
            // THIS CASE MUST NOT SEND. A real send would email example.com and put a
            // distribution in History that J-10 would then have to reason about. Cancel is a
            // plain popBackStack in this build — no discard confirmation (F-26).
            if (composer.isOpen(Duration.ofSeconds(2))) composer.tapCancel();
            new ConfirmPopupPage(driver).clearIfBlocking();
        }

        assertTrue(book.isDetailOpen(Duration.ofSeconds(20)) || book.isOpen(Duration.ofSeconds(5)),
                "Cancelling the composer should pop back to the contact detail");
    }

    // ==================== I-11 · edit ====================

    @Test
    @Order(11)
    @DisplayName("I-11: Edit updates name and job, and LOCKS the email (it is the contact's identity)")
    void editUpdatesTheContact() {
        DocDistAddressBookPage book = openDetailOrSkip();
        DocDistContactEditorSheet sheet = new DocDistContactEditorSheet(driver);
        try {
            assumeTrue(book.tapEdit(), "Could not tap Edit");
            skipIfRightsPrompt("Edit contact");
            assertTrue(sheet.waitOpen(Duration.ofSeconds(12)), "The editor should open in edit mode");

            assertTrue(containsIgnoreCase(sheet.title(), "Edit"),
                    "Edit mode retitles the sheet. Got: '" + sheet.title() + "'");
            assertTrue(EMAIL.equalsIgnoreCase(sheet.emailValue()),
                    "The locked field should show the contact's address. Got: '" + sheet.emailValue() + "'");
            // The strongest assertion in this case: there is NO path that changes a
            // contact's email. The sheet disables the input and ContactDetailFragment only
            // ever calls updateContact(name, job).
            assertFalse(sheet.isEmailEditable(),
                    "EDIT mode must lock the email — it identifies the contact in lists and history");
            System.out.println(">>> edit sheet: title='" + sheet.title() + "' save='" + sheet.saveLabel()
                    + "' lockedHelper=" + sheet.emailLockedHelperShown());
            screenshot("ddc-editor-edit");

            // SPELLED OUT STEP BY STEP, not editAndSave()'s single boolean. The first run
            // of this case lost the sheet MID-EDIT — a keyboard-hiding BACK press cancelled
            // the BottomSheetDialog (see DocDistContactEditorSheet's class note, which is
            // also where that BACK was removed) — and all the one-boolean form could say
            // was "Save changes should be accepted", which pointed at the app. Each step
            // now names itself, so a repeat says WHICH one went.
            assertTrue(sheet.setName(RENAMED), "Could not retype the name");
            assertTrue(sheet.setJob(JOB), "Could not retype the job");
            assertTrue(sheet.isOpen(),
                    "The editor must still be up after typing — if it is not, something "
                            + "dismissed the sheet (a stray BACK, not the app)");
            // Not a validation step: updateSaveEnabled() reads the EMAIL alone, and edit
            // mode locks the email, so Save has been enabled since the sheet opened and
            // name / job cannot have changed it.
            assertTrue(sheet.isSaveEnabled(),
                    "Edit mode prefills a valid, locked address, so Save is enabled from "
                            + "the moment the sheet opens");
            assertTrue(sheet.save(), "Could not tap Save changes");
            assertTrue(sheet.waitClosed(Duration.ofSeconds(12)),
                    "The sheet dismisses itself as soon as it posts its FragmentResult");
        } finally {
            sheet.dismissQuietly();
        }

        boolean updated = book.updatedConfirmationShown(Duration.ofSeconds(10));
        boolean renamed = waitForDetailName(book, RENAMED, Duration.ofSeconds(25));
        System.out.println(">>> after edit: snackbar=" + updated + " detailName='" + book.detailName() + "'");
        screenshot("ddc-edited");
        assertTrue(book.isDetailOpen(Duration.ofSeconds(10)), "The edit stays on the contact detail");
        assertTrue(renamed, "The detail re-renders from the PUT response and should show '"
                + RENAMED + "'. Got: '" + book.detailName() + "'");
    }

    // ==================== I-13 · export ====================

    @Test
    @Order(12)
    @DisplayName("I-13: Export CSV hands 'Address Book.csv' to the system save picker and confirms the write")
    void exportProducesACsvFile() {
        assumeTrue(created, "Needs at least the QA contact — canExport is false on an empty list");
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        assumeTrue(book.waitForContact(EMAIL, Duration.ofSeconds(45)), "The QA contact is not listed");

        assertTrue(book.hasExport(), "Export CSV lives in the address book's header row");
        assertTrue(book.isExportEnabled(),
                "canExport = !isLoading && contacts.isNotEmpty(), and the QA contact exists");

        boolean saved = false;
        try {
            assumeTrue(book.tapExport(), "Could not tap Export CSV");
            skipIfRightsPrompt("Export CSV");

            // The picker is DocumentsUI — a FOREIGN package, so byId() (which prefixes OUR
            // package) matches nothing inside it. Everything here is matched on visible
            // TEXT, the same rule DriveUploadPage learned.
            assumeTrue(book.isSystemPickerOpen(Duration.ofSeconds(25)),
                    "The SAF CreateDocument('text/csv') picker did not open");
            boolean prefilled = book.pickerShowsFileName("Address Book", Duration.ofSeconds(10));
            System.out.println(">>> save picker: prefilledNameVisible=" + prefilled);
            screenshot("ddc-export-picker");
            assertTrue(prefilled,
                    "The picker is launched with AddressBookCsvBuilder.FILENAME = 'Address Book.csv'");

            // NOTHING BETWEEN THESE TWO LINES. The confirmation is a Snackbar.LENGTH_LONG
            // (~2.75 s) that AddressBookFragment raises the moment its SAF callback
            // finishes openOutputStream(uri).write(bytes) — which is while DocumentsUI is
            // still animating away. saveInSystemPicker() used to sleep 2 s after the tap,
            // and that sleep spent the confirmation: the first run of this case then spent
            // its full 25 s timeout, plus a 3 s fallback, staring at a screen the snackbar
            // had already left. The bytes were written; only the watcher was late.
            assertTrue(book.saveInSystemPicker(), "Could not commit the save in the system picker");
            saved = book.exportSavedConfirmationShown(Duration.ofSeconds(20));
        } finally {
            // Never leave the foreign picker in front of the app — every later case would
            // run against DocumentsUI.
            book.cancelSystemPicker();
        }

        System.out.println(">>> export saved-confirmation = " + saved);
        screenshot("ddc-export-saved");
        assertTrue(book.isOpen(Duration.ofSeconds(25)), "The picker should hand control back to the address book");
        // BOUNDARY: the bytes land at a URI chosen inside a foreign picker, and this test has
        // no path to the device filesystem — so the assertion stops at the app's own
        // post-write confirmation (dd_address_export_saved = "Saved %1$s"), which
        // AddressBookFragment only emits after openOutputStream(uri).write(bytes) succeeded.
        // The CSV's CONTENT (header row + one line per contact) is AddressBookCsvBuilder's
        // job and belongs in a unit test, not here.
        assertTrue(saved, "The app should confirm 'Saved Address Book.csv' once the bytes are "
                + "written. If this fails again with the picker plainly gone, the two causes "
                + "worth separating are: (a) the write FAILED, which snackbars 'Failed to "
                + "export' instead — look for that string in the capture; and (b) the callback "
                + "returned early because pendingExportBytes was null, which happens when the "
                + "activity was destroyed behind DocumentsUI (check 'Don't keep activities').");
    }

    // ==================== I-14 · CSV import (spec correction) ====================

    @Test
    @Order(13)
    @DisplayName("I-14: CSV import is NOT an address-book affordance — it belongs to the Lists editor")
    void csvImportIsNotAnAddressBookAffordance() {
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        assertTrue(book.isOpen(Duration.ofSeconds(10)), "Should be on the address book");

        // SOURCE: CsvImportBottomSheet is constructed in exactly ONE place in the whole app —
        // PresetEditorDialog.launchCsvImport() — and what it returns is an ArrayList<Recipient>
        // that the PRESET being edited absorbs (KEY_RECIPIENTS → viewModel.addRecipients).
        // It never touches ContactsRepository. AddressBookFragment has a single add path:
        // dd_add → ContactEditorBottomSheet, one contact at a time.
        //
        // So the spec's I-14 ("CSV import adds contacts") is filed under the wrong area: the
        // flow exists, but it is an area-G case about distribution lists. Rather than fake a
        // pass by driving the preset editor from a contacts suite, this case asserts the
        // boundary that makes the spec row wrong.
        assertTrue(book.hasAddFab(), "The address book's only add path is the single-contact FAB");
        assertFalse(csvSheetPresent(),
                "The CSV-import sheet's own ids (dd_choose_file / dd_summary) must not exist here");
        assertFalse(book.textShown("Import CSV", Duration.ofSeconds(3)),
                "There is no Import-CSV affordance on the address book — see PresetEditorDialog "
                        + "(area G) for the only entry point");
        screenshot("ddc-no-csv-import");
    }

    // ==================== I-12 · delete (and the suite's cleanup) ====================

    @Test
    @Order(14)
    @DisplayName("I-12 / CLEANUP: Delete confirms with the FRAMEWORK dialog, then the contact is gone")
    void deleteRemovesTheContact() {
        assumeTrue(created, "Nothing to delete — I-5 must have created the contact");
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        assumeTrue(book.waitForContact(EMAIL, Duration.ofSeconds(45)), "The QA contact is already gone");
        assumeTrue(book.openContact(EMAIL), "Could not open the QA contact");
        assumeTrue(book.isDetailOpen(Duration.ofSeconds(20)), "The contact detail did not open");

        assertTrue(book.hasMutateRow() && book.hasDelete(),
                "The Edit / Delete row is shown outside the smart-list view-only mode");
        assumeTrue(book.tapDelete(), "Could not tap Delete");
        // A CUSTOM-family dialog here would be the posting-rights gate, not the confirmation
        // — skipIfRightsPrompt tells them apart and lets the framework one through.
        skipIfRightsPrompt("Delete contact");

        ConfirmPopupPage confirm = book.confirmPopup();
        assertTrue(confirm.isShown(Duration.ofSeconds(10)), "Deleting a contact must ask for confirmation");
        System.out.println(">>> delete confirm says: '" + confirm.text()
                + "' framework=" + confirm.isFrameworkDialog());
        screenshot("ddc-delete-confirm");
        assertTrue(confirm.accept(Duration.ofSeconds(6)), "Could not confirm the delete");
        // Logged, not asserted: dd_contact_deleted is a LENGTH_SHORT snackbar fired
        // immediately before popBackStack, so whether it is still on screen when we look is a
        // race. The list is the durable evidence.
        System.out.println(">>> delete snackbar seen = "
                + book.deletedConfirmationShown(Duration.ofSeconds(3)));
        sleep(2500);

        // Prove the screen is honest BEFORE asserting absence:
        //  · no dialog still covering the list,
        //  · we actually popped back to the address book (onContactDeleted → popBackStack),
        //  · and the list is NOT filtered — the ViewModel keeps its search query across the
        //    detail round-trip while the recreated search box renders empty, so a stale
        //    filter would make "it is gone" pass against a list that is merely hiding it.
        assertFalse(book.confirmPopup().isShown(Duration.ofSeconds(2)),
                "The confirmation should be gone after confirming");
        assertTrue(book.isOpen(Duration.ofSeconds(25)), "Deleting should pop back to the address book");
        book.clearSearch();
        book.pullToRefresh();
        assertTrue(book.waitListReady(Duration.ofSeconds(30)),
                "The address book must be readable before asserting absence");

        boolean gone = false;
        for (int i = 0; i < 6 && !gone; i++) {
            gone = !book.hasContact(EMAIL);
            if (!gone) { book.pullToRefresh(); sleep(2500); }
        }
        System.out.println(">>> after delete: " + book.contactEmails());
        screenshot("ddc-deleted");
        if (gone) created = false;
        assertTrue(gone, "The deleted contact should be gone from the address book. "
                + "ZL-19530 note: the backend re-derives contacts from past-distribution history, "
                + "so an address that was ever EMAILED reappears on the next GET — this one never "
                + "was (I-10 cancels the composer), so it must stay gone.");
    }

    // ==================== helpers ====================

    /** Opens the Add-contact editor, skipping the case when posting rights gate the tap. */
    private DocDistContactEditorSheet openEditorOrSkip(DocDistAddressBookPage book) {
        DocDistContactEditorSheet sheet = new DocDistContactEditorSheet(driver);
        assumeTrue(book.hasAddFab(), "No Add-contact FAB on this screen");
        assumeTrue(book.openAdd(), "Could not tap Add contact");
        skipIfRightsPrompt("Add contact");
        assumeTrue(sheet.waitOpen(Duration.ofSeconds(12)), "The contact editor did not open");
        return sheet;
    }

    /** Reaches the QA contact's detail, or skips. */
    private DocDistAddressBookPage openDetailOrSkip() {
        assumeTrue(created, "Needs the QA contact from I-5");
        DocDistAddressBookPage book = new DocDistAddressBookPage(driver);
        assumeTrue(book.waitForContact(EMAIL, Duration.ofSeconds(45)), "The QA contact is not listed");
        assumeTrue(book.openContact(EMAIL), "Could not open the QA contact's row");
        assumeTrue(book.isDetailOpen(Duration.ofSeconds(20)), "The contact detail did not open");
        return book;
    }

    /**
     * Skips the case when the dialog on screen is a RIGHTS prompt.
     *
     * <p>Both {@code runWithPostingRights} and {@code runWithDownloadRights} raise the app's
     * CUSTOM dialog ({@code showDialogWithButton} → {@code leftButton}/{@code rightButton}),
     * while every real confirmation in this area is the FRAMEWORK one. Telling them apart
     * matters twice over: a rights gate is an environment gap that should SKIP (trap 3.6),
     * and mistaking the delete confirmation for one would skip a case that was working.
     *
     * <p>The prompt is DECLINED, never proceeded — "Proceed" posts a permission request into
     * a real admin's chat, which is not something a test run should do.
     */
    private void skipIfRightsPrompt(String what) {
        ConfirmPopupPage popup = new ConfirmPopupPage(driver);
        if (!popup.isShown(Duration.ofSeconds(3))) return;
        if (popup.isFrameworkDialog()) return;          // the app's own confirmation — let it through
        String text = popup.text();
        popup.cancel(Duration.ofSeconds(4));
        assumeTrue(false, what + " is gated by rights on this account: '" + text + "' (trap 3.6)");
    }

    private boolean waitForDetailName(DocDistAddressBookPage book, String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (name.equalsIgnoreCase(book.detailName())) return true;
            sleep(700);
        }
        return false;
    }

    /** True when the CSV-import sheet's own controls are on screen. */
    private boolean csvSheetPresent() {
        try {
            return !driver.findElements(byAppId("dd_choose_file")).isEmpty()
                    || !driver.findElements(byAppId("dd_summary")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The device's Android major version, or -1 when the session did not report one.
     * Only used to decide whether the app OWES us a snackbar (I-9).
     */
    private int androidMajor() {
        try {
            Object v = driver.getCapabilities().getCapability("platformVersion");
            if (v == null) return -1;
            String s = v.toString().trim();
            int dot = s.indexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
