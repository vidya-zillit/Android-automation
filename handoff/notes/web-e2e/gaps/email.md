# Email: web cases vs Android suite

Web input: `email.md` (10 web tests, 3 sheet bugs).
Android suites: `tests/email/` (EmailTest, EmailDrawerTest, EmailExtrasTest, EmailActionsTest, EmailCalendarTest).
App source: `Zillit-Android/app/src/main/java/com/zillit/zillitapp/bottomNav/new_email/`, called `new_email/` below.

## Summary

| # | Web test / sheet bug | Status | Android case / note |
|---|---|---|---|
| W1 | email module loads with nav strip, sidebar and folders | COVERED | `EmailTest.landingLoads` + `EmailTest.folderDrawer`. On Android the Email/Calendar/Contacts strip is the drawer (`nav_mail`, `nav_calendar`, `nav_contacts`, `nav_settings`) |
| W2 | system folders (INBOX/Sent/Drafts/Trash) listed | COVERED (weak) | `EmailTest.folderDrawer` asserts Inbox plus Sent **or** Drafts. Trash is never asserted. Tighten it (see below) |
| W3 | nav strip switches to Calendar and back | COVERED | `EmailDrawerTest.drawerCalendar` |
| W4 | contacts nav opens contacts | COVERED | `EmailDrawerTest.drawerContacts` |
| W5 | selecting a folder updates the URL and loads its list | COVERED | `EmailTest.sentEmailInSentFolder` (`openFolder("Sent")`). The URL part is web-only |
| W6 | search filters the email list (no-match → empty) | **MISSING** | `EmailTest.searchEmails` accepts results, no results or the initial state, so it cannot fail |
| W7 | opening an email renders the reading pane | COVERED | `EmailTest.openEmailDetail` |
| W8 | compose and send to self, lands in Sent | COVERED (weak) | `EmailTest.composeAndSend` + `sentEmailInSentFolder`, `EmailExtrasTest.sendToProjectUserAndVerifyInSent`. These send to another address, not to yourself, and the Sent check also passes if any email is listed (`\|\| e.emailCount() > 0`) |
| W9 | empty recipient blocks send with a warning | **CONFLICT** (Android is right) | `EmailTest.sendRequiresRecipient` asserts that Send is **disabled**. The web shows a warning instead |
| W10 | save a draft, reopen it, then discard it | **MISSING** | Nothing in the suite opens Drafts |
| EMAIL-01 | rule summary cut to two lines | **MISSING** | The suite never opens Email Rules |
| EMAIL-02 | mail sent to your own mailbox arrives already read | **MISSING** | — |
| EMAIL-03 | typing in the body scrolls the composer header away | **MISSING** (low) | — |

Counts: 7 COVERED (2 weak), 5 MISSING, 1 CONFLICT, 0 WEB-ONLY.

---

## MISSING, in full

### W6: search with no match shows the no-results state

The Android search is a separate screen (`SearchActivity`, `fragment_search.xml`). It is not the web's inline filter.

- **Proposed case:** `EmailTest.searchNoMatchShowsEmpty` (or tighten `searchEmails`).
- **Steps:**
  1. On the Email list, tap `action_search`. `et_search` opens.
  2. Type `zz-no-such-email-zz-<epoch>`.
  3. Wait at least 2.5 s for the debounce and the server.
- **Expected:**
  - `empty_no_results` is visible.
  - `rv_results` is gone or has 0 children.
  - `tv_result_count` is absent or reads 0.
- **Then:** tap `btn_clear`, and check that `empty_initial` comes back.
- **Why:** `searchEmails` currently passes on `hasResults || isNoResults || isInitialState`, so a broken search still goes green.

### W10: a draft is saved, reopens in Drafts, and is deleted

On Android a draft is saved **on close**, not by a "Save as Draft" button:
- `ComposeActivity.handleClose()` (ComposeActivity.kt:139-148) calls `viewModel.saveDraft()` when To, Subject or Body is non-empty.
- Tapping a row in Drafts reopens Compose with toolbar title **"Draft"** (ComposeActivity.kt:129, EmailListFragment.kt:169-172).
- Drafts are deleted from selection mode with the action **"Delete drafts"** (EmailListFragment.kt:466 and :516).

**Proposed case:** `EmailExtrasTest.draftSaveReopenDelete`

**Steps:**
1. Tap `fab_compose`.
2. In `et_to`, add your own address. Read it from the drawer's `tv_user_email`.
3. Set `et_subject` to `[e2e] draft <epoch>`. Type a body in `web_view_body`.
4. Tap the toolbar navigation (close). Do **not** tap `btn_send`.
5. Open the drawer and tap the folder row whose `tv_folder_name` is "Drafts".
6. Check that a row with `tv_subject` = the subject is listed.
7. Tap the row. Expect Compose with toolbar title "Draft" and `et_subject` prefilled.
8. Close Compose without changes.
9. Long-press the draft row so that `toolbar_selection` appears. Tap the "Delete drafts" action and confirm.
10. Reopen Drafts.

**Expected:**
- The draft appears in Drafts after close.
- It reopens with its subject.
- After "Delete drafts" it is gone from Drafts.

**Note:** EmailListFragment.kt:91-97 says Drafts are not observed from Realm and are reloaded with `reloadDrafts()` on return. If the row is missing on the first look, pull to refresh before failing.

### EMAIL-01: Email Rules readback shows the whole sentence

Android has the feature:
- Settings → Email Rules: `cardEmailRules` in `email_fragment_settings.xml:253` opens `EmailRulesActivity` (`fabAdd`, `recyclerRules`) and then `EditEmailRuleActivity`.
- In Edit Rule, the live summary `tvReadback` (`activity_edit_email_rule.xml:178`) has **no maxLines**, so the bug as written (Edit Rule) should not happen on Android.
- The **list card** summary `tvRuleSummary` is `maxLines=2` with an ellipsis (`item_email_rule.xml:95-100`). That one does cut long rules.
- Email Rules is hidden while the join request is pending (GeneralSettingsActivity.kt:121-125).

**Proposed case:** `EmailRulesTest.readbackShowsFullSentence`. Rules are not automated at all today.

**Steps:**
1. Open the drawer → `nav_settings` → `cardEmailRules` → `fabAdd`.
2. Fill the conditions:
   - `etName` = `ZL AUTO rule <epoch>`
   - `etFrom` = `a.very.long.sender.address@example.com`
   - `etSubject` = `quarterly production budget review`
   - `etHasWords` = `invoice, purchase order, approval`
3. Tick `checkHasAttachment`.

**Expected:**
- The text of `tvReadback` contains all four parts: From, Subject, Has words, and attachment. These are built by `RuleSummary.readback` (RuleSummary.kt:129-150).
- No condition value is cut off.

**Optional:** save with an action from `btnAddAction`, then go back to the list. The `tvRuleSummary` there will be ellipsized at 2 lines. Treat that as a product decision, not a failure, unless product says the list must show the full sentence. Delete the rule afterwards from `btnOverflow`.

### EMAIL-02: mail to your own mailbox arrives unread

The app marks mail as read only when it is opened (EmailDetailViewModel.kt:49 and :114, `markAsRead(..., true)`).

**Proposed case:** `EmailExtrasTest.selfMailArrivesUnread`. It also covers the "send to self" part of W8.

**Steps:**
1. Read your own address from the drawer's `tv_user_email`.
2. Compose to that address with subject `[e2e] self-unread <epoch>` and send it.
3. Open Inbox. Pull to refresh (`swipe_refresh`) until a row with that `tv_subject` appears (up to about 60 s).
4. **Do not open it.**
5. Read that row's `unread_dot`, then the Inbox `tv_unread_count` in the drawer.
6. Tap `action_filter` → `chip_unread`.

**Expected:**
- `unread_dot` is visible on the row.
- The drawer count is 1 or more.
- Under the Unread filter, the row is still listed and `empty_view` is not shown.

**Cleanup:**
1. Open the email, which marks it read.
2. Delete it from the detail screen.
3. Reset the filter to `chip_all`.
4. Also delete the Sent copy.

### EMAIL-03: the composer header stays reachable while typing in the body

Android layout: the `MaterialToolbar` holding `btn_send` sits **outside** the `ScrollView` (activity_compose.xml:10-71 and :75), and ComposeActivity uses `windowSoftInputMode="adjustResize"` (AndroidManifest.xml:617-619). So the header should hold. A cheap guard:

- **Proposed case:** `EmailTest.composeHeaderStaysWhileTyping`
- **Steps:**
  1. Open Compose and tap `web_view_body`.
  2. Type about 10 lines. Use repeated `\n` key events, not `sendKeys` on the WebView.
- **Expected:**
  - `toolbar` and `btn_send` are still displayed while the keyboard is up (`isDisplayed()`).
  - `et_to` can be reached with one scroll up.

---

## CONFLICT

### W9: empty recipient. Android disables Send; the web warns

- Web: clicking Send with no recipient shows a warning toast, and the composer stays open.
- Android: `binding.btnSend.isEnabled = !state.isSending && state.to.isNotEmpty() && !state.hasUploadingAttachments` (ComposeActivity.kt:1093). Send cannot be tapped, so there is no warning to show.
- **Verdict:** `EmailTest.sendRequiresRecipient` is correct for Android. Keep it. No Android change is needed. Note the difference in any cross-platform test plan.
- **Side effect, as on the web:** closing that composer with a subject typed auto-saves a draft (`handleClose`). Our test only types a To address and then closes, so it can leave a draft behind with only that To address. If Drafts start to fill up, add cleanup.

---

## COVERED and weak cases (compact)

- **W1:** `landingLoads` checks `fab_compose` and search. `folderDrawer` checks the folder list. The nav items are not asserted by name. Optionally assert `nav_calendar`, `nav_contacts` and `nav_settings` are displayed.
- **W2 (weak):** in `folderDrawer`, require all four of Inbox, Sent, Drafts and Trash (case-insensitive contains). Today, Sent **or** Drafts is enough.
- **W3:** `drawerCalendar` opens the calendar. The way back is `recoverToEmailList`. Optionally assert that `nav_mail` brings back `fab_compose`.
- **W4:** `drawerContacts` asserts that `EmailContactsPage` is loaded.
- **W5:** `openFolder("Sent")` is used throughout. Android has no URL.
- **W7:** `openEmailDetail` asserts that the detail screen and Reply are shown. The body WebView is best-effort.
- **W8 (weak):** `sentEmailInSentFolder` and `sendToProjectUserAndVerifyInSent` both end with `isEmailShown(subject) || emailCount() > 0`. Drop the `|| emailCount() > 0` so the test checks for the actual subject.
