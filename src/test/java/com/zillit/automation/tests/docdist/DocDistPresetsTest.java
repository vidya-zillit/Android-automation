package com.zillit.automation.tests.docdist;

import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.ConfirmPopupPage;
import com.zillit.automation.pages.docdist.DocDistComposerPage;
import com.zillit.automation.pages.docdist.DocDistLibraryPage;
import com.zillit.automation.pages.docdist.DocDistPresetsPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DOCUMENT DISTRIBUTION — area G (Presets / Distribution lists), G-1 … G-10 of
 * DOC_DISTRIBUTION_TEST_CASES.md.
 *
 * <p>Everything this suite creates, it deletes: one list, made in G-6, renamed in G-7 and
 * removed in the last case. Every case that opens the editor unwinds it in a finally —
 * the editor is a FULL-SCREEN DialogFragment, so leaving one up does not merely fail the
 * case, it hides the whole app from every case after it.
 *
 * <p><b>No email is ever sent.</b> G-10 applies the list in the composer and asserts the
 * chips, then cancels. Send is never tapped anywhere in this file.
 *
 * <p>Two source facts shape how these cases are written, and both contradict the case
 * table — see the individual DisplayNames:
 * <ul>
 *   <li><b>G-2 asks for an empty state that cannot exist.</b>
 *       {@code PresetOverviewFragment.render()} ends with an unconditional
 *       {@code binding.ddEmpty.visibility = View.GONE}, because the "Smart lists"
 *       section (header + "All contacts") is always prepended to the list. There is no
 *       data state that shows {@code dd_empty} or its CTA on this screen.</li>
 *   <li><b>G-9's badge is not a list count.</b> {@code dd_badge_lists} renders
 *       {@code state.unitCounts[DOC_DISTRIBUTION_PRESET_LABEL]}, an UNREAD-NOTIFICATION
 *       count, and {@code LibraryFragment} calls {@code viewModel.markListsRead()} on the
 *       very tap that opens this screen. So the act of testing lists clears the badge.
 *       Asserting "the badge equals the number of lists" would be asserting a coincidence
 *       (trap 3.2 in its strongest form).</li>
 * </ul>
 *
 * <p>Rights: every mutating tap here routes through {@code runWithPostingRights}. Without
 * {@code canPost} the app shows its CUSTOM confirm ({@code showDialogWithButton}) whose
 * right button MESSAGES AN ADMIN — so a case that meets that dialog DECLINES it and skips
 * (trap 3.6). It never blind-accepts: {@link ConfirmPopupPage#isFrameworkDialog()} is how
 * the safe delete confirmation is told apart from the dangerous rights request.
 */
@Order(TestOrder.DOC_DISTRIBUTION_PRESETS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocDistPresetsTest extends DocDistBaseTest {

    private static final String SUFFIX = String.valueOf(System.currentTimeMillis() % 100000);
    private static final String PRESET  = "QA_PRE_" + SUFFIX;
    private static final String RENAMED = PRESET + "_R";

    /**
     * One token shared by the recipient's name and address, because the composer's chips
     * render the NAME when there is one and keep the address in a tooltip
     * ({@code renderChips} → TooltipCompat) — a chip assertion that only knows the email
     * would miss a chip that is plainly correct.
     */
    private static final String TOKEN   = "qapre" + SUFFIX;
    private static final String EMAIL   = TOKEN + "@example.com";
    private static final String CONTACT = "QA " + TOKEN;
    private static final String BAD_EMAIL = "not-an-email";

    /** Tracks the rename in G-7 so the later cases target the name that actually exists. */
    private static String presetName = PRESET;
    private static boolean created;

    @BeforeEach
    void reachLibrary() {
        reachLibraryOrSkip();
    }

    // ==================== navigation helpers ====================

    /** Library → Lists, landing on the preset overview. */
    private DocDistPresetsPage openLists() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.openLists(), "The Lists shortcut is not reachable on this build");
        DocDistPresetsPage presets = new DocDistPresetsPage(driver);
        assumeTrue(presets.isOverview(Duration.ofSeconds(20)), "The preset overview did not open");
        return presets;
    }

    /**
     * Unwinds back to the library root.
     *
     * <p>Called from every finally. A case that ends on the overview (or worse, inside the
     * editor) leaves the next case's @BeforeEach to guess its way home — {@code openModule()}
     * does try, but a failure that cascades through nine cases is not a diagnosis.
     */
    private void backToLibrary() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // Guarded: press BACK only while still INSIDE DocumentDistributionActivity.
        // The unguarded version could walk straight out of the module and off the app —
        // with an editor dialog open and the keyboard up, six BACKs is more than enough —
        // and the NEXT suite then died with "No known landing screen appeared within
        // 120s", a failure that pointed nowhere near this teardown.
        for (int i = 0; i < 6; i++) {
            if (!lib.isDocDistActivity()) break;
            if (lib.isLibrary(Duration.ofSeconds(2))) break;
            try { driver.navigate().back(); Thread.sleep(900); } catch (Exception e) { break; }
        }
        if (lib.isDocDistActivity()) lib.returnToRoot();
    }

    /**
     * Taps New and waits for the editor — or skips when the build answers with the
     * posting-rights request dialog instead.
     *
     * <p>The request dialog is DECLINED, never accepted: its right button posts a
     * permission request into an admin's chat, which is not something a test run gets to
     * do to a real project.
     */
    private void openEditorFromNew(DocDistPresetsPage presets) {
        assumeTrue(presets.tapNew(), "Could not tap the New list button");
        if (presets.editorOpen(Duration.ofSeconds(12))) return;
        ConfirmPopupPage gate = presets.confirmPopup();
        if (gate.isShown(Duration.ofSeconds(2)) && !gate.isFrameworkDialog()) {
            String text = gate.text();
            gate.cancel(Duration.ofSeconds(4));
            assumeTrue(false, "No posting rights on this account — New showed the "
                    + "request-permission dialog instead of the editor: '" + text + "'");
        }
        assumeTrue(false, "The preset editor did not open");
    }

    // ==================== G-1 ====================

    @Test
    @Order(1)
    @DisplayName("G-1: the Lists shortcut opens the preset overview (dd_new + its section headers)")
    void listsOpensThePresetOverview() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assertTrue(lib.hasLists(), "The library's Lists shortcut should exist "
                + "(it sits past the fold in the horizontally scrolling action row — trap 3.10)");
        assertTrue(lib.openLists(), "Could not tap the Lists shortcut");

        DocDistPresetsPage presets = new DocDistPresetsPage(driver);
        try {
            assertTrue(presets.isOverview(Duration.ofSeconds(20)),
                    "The preset overview should open");
            assertTrue(presets.hasNewButton(), "dd_new (the 'New list' FAB) should be present");

            // dd_recycler is shared by NINE screens (trap 3.3) and dd_new by TWO (this
            // screen and the template manager, which are the same layout twice), so the
            // section headers are what actually names this screen: PresetOverviewAdapter
            // is the only place item_dd_list_section_header is inflated.
            List<String> headers = presets.sectionHeaders();
            System.out.println(">>> preset overview: header='" + presets.headerText()
                    + "' sections=" + headers + " rows=" + presets.listNames());
            screenshot("ddp-overview");
            assertFalse(headers.isEmpty(),
                    "The overview always renders its 'Smart lists' / 'Custom lists' headers");
        } finally {
            backToLibrary();
        }
    }

    // ==================== G-2 ====================

    @Test
    @Order(2)
    @DisplayName("G-2 CORRECTED: this screen has NO empty state — render() forces dd_empty GONE, "
            + "and the 'All contacts' smart row is always there instead")
    void thePresetOverviewNeverShowsAnEmptyState() {
        DocDistPresetsPage presets = openLists();
        try {
            // Source: render() builds [Header, Smart(All contacts), Header, …presets] and
            // then sets ddEmpty.visibility = GONE unconditionally — the comment on the line
            // says "the smart list is always present, so the full-screen empty state no
            // longer applies". dd_empty_cta is a CHILD of dd_empty, so it is not in the
            // tree either. The case table's "empty state offers a CTA" describes a screen
            // this build does not have.
            boolean empty = presets.hasEmptyState();
            boolean cta = presets.hasEmptyCta();
            List<String> rows = presets.listNames();
            System.out.println(">>> dd_empty=" + empty + " dd_empty_cta=" + cta + " rows=" + rows);
            screenshot("ddp-no-empty-state");

            assertFalse(empty, "dd_empty must never show on the preset overview");
            assertFalse(cta, "dd_empty_cta lives inside dd_empty, so it must never show either");
            assertFalse(rows.isEmpty(),
                    "The smart 'All contacts' row is unconditional — the list is never empty");
        } finally {
            backToLibrary();
        }
    }

    // ==================== G-3 ====================

    @Test
    @Order(3)
    @DisplayName("G-3: New opens the preset editor with name, description, the add-recipient row, "
            + "Import CSV, Save and Cancel")
    void newOpensThePresetEditor() {
        DocDistPresetsPage presets = openLists();
        try {
            openEditorFromNew(presets);
            System.out.println(">>> editor title='" + presets.editorTitle()
                    + "' recipients=" + presets.recipientCount());
            screenshot("ddp-editor");

            assertTrue(presets.hasNameField(), "The list name field should be present");
            assertTrue(presets.hasDescriptionField(), "The description field should be present");
            assertTrue(presets.hasEmailField(), "The add-recipient email field should be present");
            assertTrue(presets.hasRecipientNameField(), "The recipient name field should be present");
            assertTrue(presets.hasRecipientJobField(), "The recipient job field should be present");
            assertTrue(presets.hasAddButton(), "The Add-to-list button should be present");
            assertTrue(presets.hasImportCsvButton(), "The Import-from-CSV button should be present");
            assertTrue(presets.hasSaveButton(), "Save should be present");
            assertTrue(presets.hasCancelButton(), "Cancel should be present");
        } finally {
            presets.forceCloseEditor();
            backToLibrary();
        }
    }

    // ==================== G-4 ====================

    @Test
    @Order(4)
    @DisplayName("G-4: Save is gated on the NAME alone — blank name disables it, "
            + "and a name with zero recipients is enough to enable it")
    void nameIsTheOnlyRequirement() {
        DocDistPresetsPage presets = openLists();
        try {
            openEditorFromNew(presets);

            // The app disables the button rather than showing an error, so this is
            // assertable directly — no guessing at an invisible EditText.error.
            assertFalse(presets.isSaveEnabled(),
                    "With nothing typed, Save must be disabled");
            assertTrue(presets.setName("   "), "Could not type into the name field");
            assertFalse(presets.isSaveEnabled(),
                    "A whitespace-only name must leave Save disabled (isNullOrBlank)");

            assertTrue(presets.setName(PRESET + "_probe"), "Could not type a real name");
            boolean enabled = presets.isSaveEnabled();
            int recipients = presets.recipientCount();
            System.out.println(">>> save enabled with a name and " + recipients
                    + " recipients = " + enabled);
            screenshot("ddp-save-gate");

            assertEquals(0, recipients, "This probe editor should still have no recipients");
            // updateSaveEnabled() is exactly `!ddName.text.isNullOrBlank()` — recipients
            // are explicitly optional ("user can create an empty list and add contacts
            // later"), so a name with an EMPTY list must enable Save.
            assertTrue(enabled, "A non-blank name alone must enable Save");
        } finally {
            // Cancel, never Save — this probe list must not survive the case.
            presets.forceCloseEditor();
            backToLibrary();
        }
    }

    // ==================== G-5 ====================

    @Test
    @Order(5)
    @DisplayName("G-5: recipients add as rows; an INVALID address is rejected outright "
            + "(addRecipient() errors the field and returns before adding)")
    void recipientsCanBeAdded() {
        DocDistPresetsPage presets = openLists();
        try {
            openEditorFromNew(presets);
            assertEquals(0, presets.recipientCount(), "A new editor starts with no recipients");

            assertTrue(presets.addRecipient(BAD_EMAIL, CONTACT, "QA"),
                    "Could not fill and submit the add-recipient row");
            assertEquals(0, presets.recipientCount(),
                    "An address failing EmailValidator must NOT become a recipient row");

            assertTrue(presets.addRecipient(EMAIL, CONTACT, "QA"),
                    "Could not add the valid recipient");
            List<String> emails = presets.recipientEmails();
            System.out.println(">>> recipients after add: " + emails
                    + " emailField='" + presets.emailFieldText() + "'");
            screenshot("ddp-recipient-added");

            assertTrue(emails.stream().anyMatch(e -> e.equalsIgnoreCase(EMAIL)),
                    "The valid recipient should be listed, got " + emails);
            // The app clears all three inputs only on the success path (addRecipient()
            // sets ddInEmail.text = null AFTER EmailValidator passes; the reject path
            // errors the field and returns with the text intact) — a second, independent
            // signal that the add was accepted rather than silently dropped.
            //
            // READ THROUGH emailFieldCleared(), NEVER isBlank(). dd_in_email is a
            // TextInputEditText whose hint sits on the wrapping TextInputLayout
            // (@string/dd_field_email = "Email"), and Material's accessibility delegate
            // publishes that hint AS THE NODE'S TEXT while the editor is empty. So a field
            // the app has just emptied dumps as text="Email" and isBlank() is false for a
            // clear that plainly happened — which is exactly how this case failed on its
            // first run, with the recipient row visibly added behind the assertion.
            assertTrue(presets.emailFieldCleared(),
                    "A successful add clears the email field. It reads '"
                            + presets.emailFieldText() + "'");
        } finally {
            // Discard: G-6 creates the list this suite actually keeps.
            presets.forceCloseEditor();
            backToLibrary();
        }
    }

    // ==================== G-6 ====================

    @Test
    @Order(6)
    @DisplayName("G-6: saving creates the list and it appears in the overview")
    void savingCreatesThePreset() {
        DocDistPresetsPage presets = openLists();
        try {
            openEditorFromNew(presets);
            assertTrue(presets.setName(PRESET), "Could not name the list");
            assertTrue(presets.setDescription("Created by DocDistPresetsTest"),
                    "Could not type a description");
            assertTrue(presets.addRecipient(EMAIL, CONTACT, "QA"), "Could not add a recipient");
            assertEquals(1, presets.recipientCount(), "The list should hold exactly one recipient");

            // saveAndWaitClosed() returns on the dialog CLOSING, which the app does only
            // from result.onSuccess — a failed save snackbars and stays put, so this is
            // the app's own "created" signal rather than a guess at timing.
            assertTrue(presets.saveAndWaitClosed(Duration.ofSeconds(25)),
                    "The editor should close once a valid list is saved");
        } finally {
            presets.forceCloseEditor();
        }

        try {
            boolean listed = presets.waitForList(PRESET, Duration.ofSeconds(45));
            System.out.println(">>> after save, overview = " + presets.listNames());
            screenshot("ddp-created");
            assertTrue(listed, "The new list should be in the overview. Got: "
                    + presets.allListNamesScrolled());
            created = true;
        } finally {
            backToLibrary();
        }
    }

    // ==================== G-7 ====================

    @Test
    @Order(7)
    @DisplayName("G-7: opening the list edits it — the recipient survived the round-trip, "
            + "and a rename persists")
    void presetCanBeEdited() {
        assumeTrue(created, "G-6 must create the list first");
        DocDistPresetsPage presets = openLists();
        try {
            assumeTrue(presets.waitForList(PRESET, Duration.ofSeconds(30)), "The list is missing");
            assumeTrue(presets.openList(PRESET), "Could not open the list's row");
            assumeTrue(presets.editorOpen(Duration.ofSeconds(15)), "The editor did not open");

            String title = presets.editorTitle();
            String name = presets.nameFieldText();
            List<String> emails = presets.recipientEmails();
            System.out.println(">>> edit mode: title='" + title + "' name='" + name
                    + "' recipients=" + emails);
            screenshot("ddp-editing");

            assertEquals(PRESET, name, "Edit mode should prefill the saved name");
            // Preset.recipientCount is `recipients.size` — a derived field, so the list
            // payload has to carry the recipients for the row's count to be right. This
            // is what "the change persists" actually means for G-7.
            assertTrue(emails.stream().anyMatch(e -> e.equalsIgnoreCase(EMAIL)),
                    "The recipient saved in G-6 should still be on the list, got " + emails);

            assertTrue(presets.setName(RENAMED), "Could not type the new name");
            assertTrue(presets.saveAndWaitClosed(Duration.ofSeconds(25)),
                    "The editor should close once the rename is saved");
        } finally {
            presets.forceCloseEditor();
        }

        try {
            boolean renamed = presets.waitForList(RENAMED, Duration.ofSeconds(45));
            System.out.println(">>> after rename, overview = " + presets.listNames());
            screenshot("ddp-renamed");
            assertTrue(renamed, "The renamed list should be listed as '" + RENAMED + "'");
            presetName = RENAMED;
        } finally {
            backToLibrary();
        }
    }

    // ==================== G-9 ====================

    @Test
    @Order(8)
    @DisplayName("G-9 CORRECTED: dd_badge_lists is an UNREAD counter, not a list count — "
            + "it never renders 0, and opening Lists marks the unit read")
    void listsBadgeIsAnUnreadCounter() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        String before = nullToEmpty(lib.listsBadge());

        // renderToolbarBadge(): `if (count > 0) { text = if (count > 99) "99+" else count }
        // else visibility = GONE`. So the rendered value is ALWAYS one of: absent, "99+",
        // or an integer ≥ 1. A literal "0" would be a regression, and an absolute-value
        // assertion (trap 3.2) would be asserting whatever the server happened to push.
        assertTrue(wellFormedBadge(before),
                "The Lists badge must be absent or a count ≥ 1 (never '0'), got '" + before + "'");

        // The tap that opens this screen also calls viewModel.markListsRead(), which
        // clears the notifications locally and asks CommonBadgesHandler to recompute —
        // so merely LOOKING at the lists is what moves this badge, not creating one.
        assumeTrue(lib.openLists(), "The Lists shortcut is not reachable on this build");
        DocDistPresetsPage presets = new DocDistPresetsPage(driver);
        try {
            assumeTrue(presets.isOverview(Duration.ofSeconds(20)), "The preset overview did not open");
        } finally {
            backToLibrary();
        }

        String after = "";
        for (int i = 0; i < 8; i++) {
            after = nullToEmpty(new DocDistLibraryPage(driver).listsBadge());
            if (after.isBlank()) break;
            sleep(1500);
        }
        System.out.println(">>> lists badge before='" + before + "' after opening Lists='" + after
                + "' (mark-read is the only thing that moves it; creating a list does not)");
        screenshot("ddp-badge");

        assertTrue(wellFormedBadge(after),
                "The Lists badge must stay absent or a count ≥ 1, got '" + after + "'");
        // Direction, never an absolute value: mark-read can only take the count DOWN.
        assertTrue(badgeValue(after) <= badgeValue(before),
                "Opening Lists marks the unit read, so its badge cannot grow: '"
                        + before + "' → '" + after + "'");
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    /** Absent, "99+", or an integer ≥ 1 — the only three things renderToolbarBadge emits. */
    private boolean wellFormedBadge(String badge) {
        if (badge == null || badge.isBlank()) return true;
        String t = badge.trim();
        if (t.equals("99+")) return true;
        try { return Integer.parseInt(t) >= 1; }
        catch (NumberFormatException e) { return false; }
    }

    /** Badge text as a number for direction comparisons; absent = 0, "99+" = 99. */
    private int badgeValue(String badge) {
        if (badge == null || badge.isBlank()) return 0;
        String t = badge.trim();
        if (t.equals("99+")) return 99;
        try { return Integer.parseInt(t); }
        catch (NumberFormatException e) { return 0; }
    }

    // ==================== G-10 ====================

    @Test
    @Order(9)
    @DisplayName("G-10: picking the list in the composer's 'Pick a list' chooser adds its recipients "
            + "as To chips (nothing is ever sent)")
    void presetAppliedInTheComposerAddsItsRecipients() {
        assumeTrue(created, "G-6 must create the list first");
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        assumeTrue(lib.hasCompose() && lib.openCompose(),
                "Compose is not reachable on this build (trap 3.17 — hidden on the emulator's "
                        + ".dev APK, unconditionally visible in the current source)");

        DocDistComposerPage composer = new DocDistComposerPage(driver);
        DocDistPresetsPage presets = new DocDistPresetsPage(driver);
        try {
            assumeTrue(composer.isOpen(Duration.ofSeconds(20)), "The composer did not open");
            int before = composer.chipCount("dd_to_chips");

            // dd_btn_lists belongs to BOTH the library and the composer — this only means
            // "the composer's Lists button" because the composer is what is on screen.
            //
            // REOPEN rather than wait: showListPicker() calls setItems() over
            // viewModel.state.value.presets, so the dialog is a snapshot. Opened before the
            // composer's state hydrates it shows only "Pick a list" / "+ Create new list"
            // and never changes — watching it for twelve seconds produced the identical two
            // rows at the end. Only a rebuilt dialog can pick up the saved list.
            //
            // Rows read "<name> (<count>)", so match the PREFIX — never the count, and
            // never a position: the chooser starts with "+ Create new list" and only then
            // shows "All contacts" when the address book is non-empty.
            assertTrue(presets.chooseListReopening(presetName, 4),
                    "'" + presetName + "' should be offered in the composer's list chooser");
            sleep(1200);
            // The chooser is now multi-choice (several lists at once): ticking a list adds
            // its people, and Done closes the dialog so the chips can be read.
            try {
                var done = driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/button1\")"));
                if (!done.isEmpty()) done.get(0).click();
            } catch (Exception ignored) { }
            sleep(1500);

            int after = composer.chipCount("dd_to_chips");
            List<String> chips = composer.chipTexts("dd_to_chips");
            System.out.println(">>> chips " + before + " → " + after + " = " + chips);
            screenshot("ddp-applied-in-composer");

            assertTrue(after > before,
                    "Applying the list should add its recipients as To chips ("
                            + before + " → " + after + ")");
            assertTrue(String.join(" ", chips).contains(TOKEN),
                    "The added chip should be this suite's recipient (chips render the NAME "
                            + "when there is one, so both carry '" + TOKEN + "'), got " + chips);
        } finally {
            // Applying a list dirties the composer, which now asks "Keep this email as a
            // draft?" — Discard, so no QA draft is left behind. Send is never touched.
            composer.leaveDiscarding();
            backToLibrary();
        }
    }

    // ==================== WEB gap cases (2026-11-23) ====================

    /**
     * [WEB L217] Sheet DD-16: leaving a distribution list with unsaved members dropped
     * them silently. Now the close icon asks "Unsaved changes"; Cancel keeps editing,
     * Discard closes without creating the list.
     */
    @Test
    @Order(11)
    @DisplayName("G-11 [WEB DD-16]: leaving a list with unsaved members asks first; Discard creates nothing")
    void leavingWithUnsavedMembersAsks() {
        String name = PRESET + "_U";
        DocDistPresetsPage presets = openLists();
        try {
            openEditorFromNew(presets);
            assertTrue(presets.setName(name), "Could not name the list");
            assertTrue(presets.addRecipient(EMAIL, CONTACT, "QA"), "Could not add a member");
            assumeTrue(presets.recipientCount() == 1, "The member row did not appear");

            assumeTrue(presets.tapToolbarClose() || presets.tapCancel(), "Could not tap the editor's close");
            boolean asked = presets.unsavedPromptShown(Duration.ofSeconds(5));
            System.out.println(">>> G-11 unsaved prompt shown=" + asked);
            screenshot("ddp-g11-prompt");
            assertTrue(asked, "[DD-16] Closing a list with an unsaved member must ask first; the editor "
                    + (presets.editorClosed(Duration.ofSeconds(1)) ? "closed silently" : "stayed with no prompt"));

            assertTrue(presets.answerUnsavedPrompt("Cancel", Duration.ofSeconds(2)), "Could not tap Cancel");
            assertTrue(presets.editorOpen(Duration.ofSeconds(4)), "Cancel should keep the editor open");
            assertEquals(1, presets.recipientCount(), "Cancel should keep the unsaved member");

            assumeTrue(presets.tapToolbarClose() || presets.tapCancel(), "Could not tap close again");
            assertTrue(presets.answerUnsavedPrompt("Discard", Duration.ofSeconds(5)), "Could not tap Discard");
            assertTrue(presets.editorClosed(Duration.ofSeconds(6)), "Discard should close the editor");
        } finally {
            presets.forceCloseEditor();
        }
        try {
            sleep(1500);
            assertFalse(presets.hasList(name), "Discard must not create the list '" + name + "'");
        } finally {
            backToLibrary();
        }
    }

    /**
     * [WEB L223] Sheet DD-19: a second list could be saved with an existing name. Now Save
     * fails with "A list with this name already exists." and the editor stays open.
     */
    @Test
    @Order(12)
    @DisplayName("G-12 [WEB DD-19]: a second list with the same name is refused")
    void duplicateListNameRefused() {
        assumeTrue(created, "G-6 must create the list first");
        DocDistPresetsPage presets = openLists();
        try {
            assumeTrue(presets.waitForList(presetName, Duration.ofSeconds(30)), "The list is missing");
            openEditorFromNew(presets);
            assertTrue(presets.setName(presetName.toLowerCase()), "Could not type the duplicate name");
            assertTrue(presets.tapSave(), "Could not tap Save");
            boolean error = presets.textShown("already exists", Duration.ofSeconds(8));
            boolean stillOpen = !presets.editorClosed(Duration.ofSeconds(2));
            System.out.println(">>> G-12 error=" + error + " editorStillOpen=" + stillOpen);
            screenshot("ddp-g12");
            assertTrue(error, "[DD-19] Saving a list whose name is taken should say 'A list with this name "
                    + "already exists.'");
            assertTrue(stillOpen, "[DD-19] The editor should stay open after the refused save");
        } finally {
            presets.forceCloseEditor();
        }
        try {
            sleep(1000);
            long copies = presets.allListNamesScrolled().stream()
                    .filter(n -> n.equalsIgnoreCase(presetName)).count();
            assertEquals(1, copies, "[DD-19] Exactly one list should carry the name " + presetName);
        } finally {
            backToLibrary();
        }
    }

    /** [WEB L229] Sheet DD-14: a one-member list read "1 people". */
    @Test
    @Order(13)
    @DisplayName("G-13 [WEB DD-14]: a one-member list reads singular ('1 recipient')")
    void oneMemberReadsOnePerson() {
        assumeTrue(created, "G-6 must create the list first");
        DocDistPresetsPage presets = openLists();
        try {
            assumeTrue(presets.waitForList(presetName, Duration.ofSeconds(30)), "The list is missing");
            String count = presets.countTextFor(presetName);
            System.out.println(">>> G-13 count line of " + presetName + " = '" + count + "'");
            // Custom lists read "N recipient(s)" (dd_footer_summary_recipients); the smart
            // row reads "N person/people". Either way one member must be singular.
            assertTrue(count.matches("^1 (person|recipient)\\b.*"),
                    "[DD-14] A list with one member should read '1 recipient' / '1 person', got '" + count + "'");
        } finally {
            backToLibrary();
        }
    }

    /**
     * [WEB L343] CSV import into a list: valid rows added, invalid and duplicate rows
     * flagged. G-3 only proved the button exists. The fixture is written to the device
     * by this case (Appium pushFile) with two good rows, one invalid address and one
     * repeat; only the two good rows may be added. The list is saved and then deleted.
     */
    @Test
    @Order(14)
    @DisplayName("G-15 [WEB L343]: CSV import adds the 2 good rows of 4; the invalid and the repeat are flagged")
    void csvImportAddsOnlyGoodRows() {
        String file = "zl_dd_import.csv";
        String good1 = "qacsv1" + SUFFIX + "@example.com";
        String good2 = "qacsv2" + SUFFIX + "@example.com";
        String csv = "name,email,job\n"
                + "QA Csv One," + good1 + ",QA\n"
                + "QA Csv Two," + good2 + ",QA\n"
                + "QA Csv Bad,not-an-email,QA\n"
                + "QA Csv Repeat," + good1 + ",QA\n";
        try {
            // pushFile(String, byte[]) takes BASE64 data, not raw bytes.
            driver.pushFile("/sdcard/Download/" + file, java.util.Base64.getEncoder()
                    .encode(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            assumeTrue(false, "Could not write the CSV fixture to the device: " + e.getMessage());
        }
        String name = PRESET + "_CSV";
        boolean saved = false;
        DocDistPresetsPage presets = openLists();
        try {
            openEditorFromNew(presets);
            assertTrue(presets.setName(name), "Could not name the list");
            assumeTrue(presets.hasImportCsvButton() && clickAppId("dd_import_csv"), "No Import CSV button");
            assumeTrue(presets.textShown("Choose CSV file", Duration.ofSeconds(8)), "The CSV import sheet did not open");
            assumeTrue(clickAppId("dd_choose_file"), "Could not tap 'Choose CSV file'");
            com.zillit.automation.pages.common.SystemFilePickerPage picker =
                    new com.zillit.automation.pages.common.SystemFilePickerPage(driver);
            assumeTrue(picker.isOpen(Duration.ofSeconds(20)), "The system picker did not open");
            boolean picked = picker.pickFile(file, Duration.ofSeconds(45));
            if (!picked) picker.cancel();
            assumeTrue(picked, "The picker could not reach " + file);
            picker.confirmIfWaiting();

            boolean summary = presets.textShown("Rows:", Duration.ofSeconds(20));
            String text = "";
            try { text = driver.findElement(byAppId("dd_summary")).getText(); } catch (Exception ignored) { }
            System.out.println(">>> G-15 summary='" + text + "'");
            screenshot("ddp-g15-summary");
            assertTrue(summary, "The import sheet should summarise the parsed file");
            assertEquals("Rows: 4 · Ready: 2 · Duplicates: 1 · Invalid: 1", text.trim(),
                    "Two good rows, one repeat and one invalid address");
            assertTrue(presets.textShown("Add 2 contacts", Duration.ofSeconds(3)),
                    "The add button should read 'Add 2 contacts'");
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Add 2 contacts\")")).click();
            sleep(1500);

            List<String> members = presets.recipientEmails();
            System.out.println(">>> G-15 members after import = " + members);
            assertEquals(2, members.size(), "Exactly the two good rows should be added, got " + members);
            assertTrue(members.stream().anyMatch(m -> m.equalsIgnoreCase(good1))
                    && members.stream().anyMatch(m -> m.equalsIgnoreCase(good2)), "Both good addresses expected");
            saved = presets.saveAndWaitClosed(Duration.ofSeconds(25));
            assertTrue(saved, "The list with the imported members should save");
            assertTrue(presets.waitForList(name, Duration.ofSeconds(45)), "The saved list should be listed");
            String count = presets.countTextFor(name);
            assertTrue(count.startsWith("2 "), "The list should hold 2 people, count line '" + count + "'");
        } finally {
            presets.forceCloseEditor();
            if (presets.waitForList(name, Duration.ofSeconds(saved ? 10 : 2)) && presets.tapDelete(name)) {
                ConfirmPopupPage confirm = presets.confirmPopup();
                if (confirm.isShown(Duration.ofSeconds(8)) && confirm.isFrameworkDialog()) {
                    confirm.accept(Duration.ofSeconds(5));
                    presets.waitForListGone(name, Duration.ofSeconds(30));
                } else {
                    confirm.clearIfBlocking();
                }
            }
            backToLibrary();
        }
    }

    /**
     * [WEB L337] A list row's own CSV download ({@code dd_export}, "Export list as CSV").
     * Saves through the system picker, waits for the app's "Saved <file>" confirmation,
     * then reads the file back from Downloads to check the header and the member.
     */
    @Test
    @Order(15)
    @DisplayName("G-14 [WEB L337]: Export list as CSV saves the members")
    void exportListAsCsv() {
        assumeTrue(created, "G-6 must create the list first");
        DocDistPresetsPage presets = openLists();
        com.zillit.automation.pages.docdist.DocDistAddressBookPage saf =
                new com.zillit.automation.pages.docdist.DocDistAddressBookPage(driver);
        String saved = "";
        try {
            assumeTrue(presets.waitForList(presetName, Duration.ofSeconds(30)), "The list is missing");
            assertTrue(presets.hasExportFor(presetName), "The list row should carry its Export CSV button");
            assumeTrue(clickRowExport(presetName), "Could not tap the row's export");
            ConfirmPopupPage gate = presets.confirmPopup();
            if (gate.isShown(Duration.ofSeconds(2)) && !gate.isFrameworkDialog()) {
                String t = gate.text();
                gate.cancel(Duration.ofSeconds(3));
                assumeTrue(false, "Export hit the rights dialog: '" + t + "'");
            }
            assumeTrue(saf.isSystemPickerOpen(Duration.ofSeconds(25)), "The save picker did not open");
            assumeTrue(saf.saveInSystemPicker(), "Could not commit the save");
            long deadline = System.currentTimeMillis() + 20000;
            while (System.currentTimeMillis() < deadline && saved.isEmpty()) {
                try {
                    for (var e : driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                            "new UiSelector().textStartsWith(\"Saved \")"))) {
                        String t = e.getText();
                        if (t != null && t.endsWith(".csv")) { saved = t.substring(6).trim(); break; }
                    }
                } catch (Exception ignored) { }
            }
        } finally {
            saf.cancelSystemPicker();
        }
        System.out.println(">>> G-14 saved file = '" + saved + "'");
        screenshot("ddp-g14");
        try {
            assertFalse(saved.isEmpty(), "The app should confirm 'Saved <file>.csv' after the export");
            String path = "/sdcard/Download/" + saved;
            byte[] bytes = null;
            try { bytes = driver.pullFile(path); } catch (Exception e) {
                System.out.println(">>> G-14 could not read " + path + " back: " + e.getMessage());
            }
            if (bytes != null) {
                String csv = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println(">>> G-14 csv head='" + csv.substring(0, Math.min(120, csv.length())).replace("\n", "\\n") + "'");
                assertTrue(csv.toLowerCase().contains("email"), "The CSV should carry a header row");
                assertTrue(csv.contains(EMAIL), "The CSV should list the member " + EMAIL);
                try { driver.executeScript("mobile: deleteFile", java.util.Map.of("remotePath", path)); }
                catch (Exception ignored) { }
            }
        } finally {
            backToLibrary();
        }
    }

    private boolean clickRowExport(String name) {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage + ":id/dd_row'][.//*[@text='" + name + "']]"
                            + "//*[@resource-id='" + appPackage + ":id/dd_export']")).click();
            sleep(800);
            return true;
        } catch (Exception e) { return false; }
    }

    // ==================== G-8 / CLEANUP ====================

    @Test
    @Order(20)
    @DisplayName("G-8 / CLEANUP: deleting the list asks to confirm, and the row disappears")
    void deletePreset() {
        assumeTrue(created, "Nothing to delete — G-6 must run first");
        DocDistPresetsPage presets = openLists();
        try {
            String target = presets.waitForList(RENAMED, Duration.ofSeconds(20)) ? RENAMED : PRESET;
            assumeTrue(presets.waitForList(target, Duration.ofSeconds(20)),
                    "The list to delete is not in the overview");
            assertTrue(presets.hasDeleteFor(target),
                    "A custom list row carries a delete affordance (the smart row does not)");
            assertTrue(presets.tapDelete(target), "Could not tap the row's delete button");

            ConfirmPopupPage confirm = presets.confirmPopup();
            assertTrue(confirm.isShown(Duration.ofSeconds(10)),
                    "Deleting a list must ask for confirmation");
            String text = confirm.text();
            System.out.println(">>> delete confirm says: '" + text
                    + "' framework=" + confirm.isFrameworkDialog());

            // The delete confirmation is the FRAMEWORK family (MaterialAlertDialogBuilder,
            // "Delete this distribution list? This can't be undone."). The CUSTOM family
            // here would be the posting-rights request, whose right button messages an
            // admin — decline that one and skip rather than confirm something this run
            // has no business confirming.
            if (!confirm.isFrameworkDialog()) {
                confirm.cancel(Duration.ofSeconds(5));
                assumeTrue(false, "No posting rights — Delete showed the request-permission "
                        + "dialog instead of the confirmation: '" + text + "'");
            }
            assertTrue(confirm.accept(Duration.ofSeconds(5)), "Could not confirm the delete");
            sleep(2500);

            // Prove the screen is READABLE before asserting absence — a dialog still up
            // would make "the row is gone" pass against a covered list.
            assertFalse(presets.confirmPopup().isShown(Duration.ofSeconds(2)),
                    "The confirmation should be gone after confirming");
            assertFalse(presets.listNames().isEmpty(),
                    "The overview must be readable (the smart row is always there)");

            boolean gone = presets.waitForListGone(target, Duration.ofSeconds(45));
            System.out.println(">>> after delete, overview = " + presets.listNames());
            screenshot("ddp-deleted");
            assertTrue(gone, "The deleted list should be gone from the overview");
        } finally {
            backToLibrary();
        }
    }
}
