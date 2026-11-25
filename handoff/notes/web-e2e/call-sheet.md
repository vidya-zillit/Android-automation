# Web E2E — Call Sheet

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: moderate — Home Call Sheet unit only

## Web test cases (9)

### Call Sheet unit (Home)

- **Call Sheet tab renders the feed with composer + History @smoke** — passed (callsheet.spec.js:67)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Steps: Enter the project and wait for the Home tabs (Bulletin is shown) → Click the Call Sheet tab → Check that the message box “Type your message here” is shown → Check that the round History button (clock icon) is shown on the feed
  - Expected: The Call Sheet tab opens its feed with the message box and the Call Sheet–only History button.

- **post a message to the Call Sheet feed** — passed (callsheet.spec.js:82)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Steps: Open Home and click the Call Sheet tab → Type a unique message such as “[e2e] post <time>” in the message box → Press Enter → Check that the message appears in the feed → Check that the message box is empty again
  - Expected: The message is posted to the Call Sheet feed and the message box clears.

- **a posted message opens its options menu (incl. Publish to Doc Distribution)** — passed (callsheet.spec.js:87)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Setup: Posts a message “[e2e] options <time>” to the Call Sheet feed.
  - Steps: Open Home and click the Call Sheet tab → Post the message “[e2e] options <time>” → Click the message’s options arrow → Check that the options menu shows Reply → Press Escape to close the menu
  - Expected: A posted message opens its options menu, which offers Reply. (Despite the title, the test does not check for “Publish to Doc Distribution”.)

- **edit a posted message** — passed (callsheet.spec.js:118)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Setup: Posts a message “[e2e] edit <time>” to the Call Sheet feed.
  - Steps: Open Home and click the Call Sheet tab → Post the message “[e2e] edit <time>” → Open the message’s options menu and choose Edit → In the edit dialog, replace the text with the same text plus “ EDITED” → Click Update → Check that the feed shows the edited message
  - Expected: The edited text replaces the message in the feed.

- **reply to a posted message** — passed (callsheet.spec.js:138)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Setup: Posts a message “[e2e] replyto <time>” to the Call Sheet feed.
  - Steps: Open Home and click the Call Sheet tab → Post the message “[e2e] replyto <time>” → Open the message’s options menu and choose Reply → Type “[e2e] reply <time>” in the message box (the quoted message shows above it) → Press Enter → Check that the reply text appears in the feed
  - Expected: The reply is posted and shows in the Call Sheet feed.

- **delete a posted message** — passed (callsheet.spec.js:153)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Setup: Posts a message “[e2e] delete <time>” to the Call Sheet feed.
  - Steps: Open Home and click the Call Sheet tab → Post the message “[e2e] delete <time>” → Open the message’s options menu and choose Delete (the feed switches to selection mode) → Click the Delete button in the selection bar → Confirm in the delete dialog (Yes / Ok / Delete / Confirm) → Check that the message is gone from the feed
  - Expected: The message is deleted and disappears from the Call Sheet feed.

- **send a message with emojis** — passed (callsheet.spec.js:171)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Steps: Open Home and click the Call Sheet tab → Type “[e2e] emoji <time> 🎬🔥✨👍” in the message box (emojis typed directly, not from the picker) → Press Enter → Check that the message with the emojis appears in the feed → Check that the message box is empty again
  - Expected: A message containing emojis is posted and shows in the feed with its emojis.

- **upload a document in Continuation mode** — passed (callsheet.spec.js:221)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Steps: Open Home and click the Call Sheet tab → Click the paper-clip button, choose “Document Upload” and pick sample.pdf → In the preview, type the caption “[e2e] cs-continuation …” and press Upload → Check that a box offers Continuation and New, with its send button reading “Select an option to send” and disabled → Choose Continuation, check Send becomes enabled, and click Send → Check that the upload does not ask to replace the earlier messages → Check that the document appears in the feed under its caption with the name sample.pdf, and is sent
  - Expected: A PDF uploaded in Continuation mode is added next to the existing Call Sheet messages, not in place of them.

- **document upload offers the New (replace-all) option** — passed (callsheet.spec.js:245)
  - Where: Home → Call Sheet tab · account: iPhone XRR
  - Steps: Open Home and click the Call Sheet tab → Click the paper-clip button, choose “Document Upload” and pick sample.pdf → In the preview, type the caption “[e2e] cs-new-cancelled …” and press Upload → In the Continuation / New box, choose New → Check that Send becomes enabled (do not press it: New replaces the whole feed) → Click Cancel and check the box closes → Wait 3 seconds and check that nothing was uploaded and no message with the caption appears
  - Expected: The upload offers the New (replace everything) option and enables Send for it; Cancel backs out without uploading anything.

## Sheet bugs (3)

- **CS-01** The Save As sheet has the wrong title — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open a call sheet draft. 2. Tap Save As.
  - Expected: The name sheet is titled "Save As New Draft".
  - Actual: The sheet showed the title of a different name prompt.

- **CS-02** A call sheet's version stays at v1 after it is saved again — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Create and publish a call sheet. 2. Edit it and save or publish again. 3. Look at the version.
  - Expected: The version goes up (v2).
  - Actual: It stays at v1.

- **CS-03** No comment badge for the other user after a comment on a call sheet — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. On device A, comment on a call sheet that B can see. 2. Look at Tools and the call sheet on device B.
  - Expected: B sees an unread badge for the comment.
  - Actual: No badge on B.
