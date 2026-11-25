# Web E2E — Email

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: written — Needs a local build with the email test ids

## Web test cases (10)

### Email

- **email module loads with nav strip, sidebar and folders @smoke** — failed (email.spec.js:129)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Check that the left strip shows the Email, Calendar and Contacts buttons → Check that the Inbox folder is listed → Check that the “New Email” and Settings buttons are shown
  - Expected: Email opens with its Email / Calendar / Contacts strip, the folder list (Inbox), New Email and Settings.
  - Web error: Error: email-shell testid not found. This spec needs the email data-testids, which are NOT on deployed dev yet — run against a local build:

- **system folders (INBOX/Sent/Drafts/Trash) are listed @smoke** — notrun (email.spec.js:144)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Check that the Inbox, Sent, Drafts and Trash folders are all listed
  - Expected: The four system folders Inbox, Sent, Drafts and Trash are listed.

- **nav strip switches to Calendar and back @smoke** — notrun (email.spec.js:155)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click the Calendar button in the left strip → Check that the calendar view shows and the email view is hidden → Click the Email button in the left strip → Check that the email view and the Inbox folder are shown again
  - Expected: The left strip switches from the email view to the calendar view and back.

- **contacts nav opens the contacts modal @smoke** — notrun (email.spec.js:170)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click the Contacts button in the left strip → Check that the contacts dialog opens → Press Escape to close it
  - Expected: The Contacts button opens the contacts dialog.

- **selecting a folder updates the URL and loads its list @smoke** — notrun (email.spec.js:179)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click the Sent folder → Check that the address bar now names the Sent folder → Check that Sent is marked as the selected folder → Wait until the list shows emails or “No emails found”
  - Expected: Choosing Sent selects that folder (also in the address bar) and loads its list.

- **search filters the email list @smoke** — notrun (email.spec.js:213)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click Inbox and wait for its list → Type a text that matches nothing, such as “zz-no-such-email-zz-<time>”, in the search box → Check that the email list becomes empty → Clear the search box
  - Expected: A search that matches no email empties the list.

- **opening an email renders the reading pane and marks it read** — notrun (email.spec.js:192)
  - Where: Email (sidebar) · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click Inbox and wait for its list → Click the first email in the list → Check that the reading pane shows the email (its subject or body)
  - Expected: Opening an email shows it in the reading pane. (Despite the title, the test does not check that the email is marked read.)

- **compose and send an email to self, lands in Sent** — notrun (email.spec.js:281)
  - Where: Email (sidebar) → New Email · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click Inbox and wait for its list (or the “No emails found” message) → Click “New Email” and wait for the compose form → Read your own address from the From line → Type your own address in To and pick it from the suggestions → Type the subject “[e2e] send <time>” and the body “e2e regression send body” → Click Send and check that the compose form closes → Open the Sent folder and search for the subject → Check that the sent email is listed
  - Expected: An email sent to your own address closes the composer and shows up in the Sent folder.

- **empty recipient blocks send with a warning** — notrun (email.spec.js:308)
  - Where: Email (sidebar) → New Email · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click Inbox and wait for its list (or the “No emails found” message) → Click “New Email” and wait for the compose form → Leave To empty → Type the subject “[e2e] no-recipient <time>” and the body “body” → Click Send → Check that a warning message appears and the compose form stays open
  - Expected: Sending without a recipient is blocked with a warning and the composer stays open. Nothing is sent, though the app may autosave the typed email as a draft, which the test does not remove.

- **save a draft, reopen it, then discard it (self-cleans)** — notrun (email.spec.js:323)
  - Where: Email (sidebar) → New Email, then Drafts · account: iPhone XRR
  - Steps: Click Email in the sidebar and wait for the Inbox folder to appear (close the mailbox tour if it shows) → Click Inbox and wait for its list (or the “No emails found” message) → Click “New Email” and wait for the compose form → Type your own address (from the From line) in To and pick it from the suggestions → Type the subject “[e2e] draft <time>” and the body “e2e draft body” → Click “Save as Draft” → Open the Drafts folder and check that the draft is listed → Click the draft and check that the compose form opens with its subject filled in → Click the Discard button (a reopened draft may label it Close) → Open Drafts again and check that the draft is gone
  - Expected: A saved draft is listed in Drafts, reopens with its subject, and is removed from Drafts after Discard.

## Sheet bugs (3)

- **EMAIL-01** Email rule summary is cut to two lines and can't be read — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create a rule with several conditions. 2. Open it in Edit Rule and read the summary sentence.
  - Expected: The whole rule sentence is readable.
  - Actual: The summary box cut the sentence to two lines with an ellipsis.

- **EMAIL-02** Mail sent to your own mailbox arrives already marked as read — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Compose a mail to your own mailbox and send it. 2. Wait for it to arrive in Inbox, then tap Refresh. 3. Open Filters → Read Status → Unread.
  - Expected: The received copy is unread: dot, bold sender, tinted row, and listed under Unread (it passed this way on 21 Oct 2026).
  - Actual: The received copy has no unread styling, even after Refresh, the drawer shows no unread count, and the Unread filter shows "Your inbox is empty". The mail was never opened.

- **EMAIL-03** Compose: typing in the body scrolls the whole composer up and hides its header — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Compose. 2. Tap into the message body and type a few lines.
  - Expected: The header (close, title, Send) and the To/Subject fields stay reachable while typing.
  - Actual: The whole composer scrolled up and hid its header until the keyboard's Done was tapped.
