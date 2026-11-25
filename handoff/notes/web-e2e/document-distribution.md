# Web E2E — Document Distribution

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: deep — Library, folders, composer, history, two-user realtime

## Web test cases (101)

### Document Distribution

- **DD shell renders: title, toolbar, library @smoke** — passed (dd.spec.js:329)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Open Tools → Document Distribution and wait for the library to finish loading → Check that the screen title reads “Document Distribution” → Check that the toolbar shows “Compose email”, “Templates”, “Distribution lists”, “Contacts” and “Create Folder” → Check that the toolbar shows the “History” button → Check that the search box “Search files & folders...” is shown
  - Expected: The Document Distribution screen opens with its title, the main toolbar buttons, History and the library search box.

- **History drawer opens with the server total and closes clean @smoke** — passed (dd.spec.js:350)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar → Check that the “Distribution History” drawer opens and shows either distribution rows or “No distributions yet” → Read the total shown in the tag next to the drawer title → Count the distribution rows loaded in the drawer → Check that there are at most 50 rows and no more rows than the total → Close the drawer with its ×
  - Expected: The drawer shows the total number of distributions, loads at most one batch of 50 rows (never more than the total), and closes completely.

### Document Distribution — bug-sheet regressions

- **a double Enter in Create Folder makes one folder** — passed (sheet-bugs/dd.spec.js:407)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Create Folder” in the toolbar → Type “[e2e] DD-02 folder <number>” in Name → Press Enter twice quickly → Check that the dialog closes → Check that the folder is listed exactly once → Check that only one folder was created and only one “Folder created” message appeared
  - Expected: Two quick Enters make one folder: one row and one “Folder created” message.

- **a search that hides every file keeps Distribute this folder and Publish** — passed (sheet-bugs/dd.spec.js:442)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-05 folder <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Reopen Document Distribution and open the folder → Check that the bar above the list shows “Distribute this folder” → Type a term that matches nothing (“zzz-e2e-nomatch-<number>”) in “Search files & folders...” → Check that “No matching files or folders.” is shown → Check that “Distribute this folder” and “Publish” are still shown in the bar → Check that the bar does not say “Upload files to enable distribution”
  - Expected: While a search hides every file, the folder’s bar keeps Distribute this folder and Publish and never asks you to upload files.

- **a trailing space in the library search still finds the file** — passed (sheet-bugs/dd.spec.js:469)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-06 folder <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Type “pdf ” (with a space at the end) in “Search files & folders...” → Check that the search is run for “pdf”, without the space → Check that sample.pdf is listed → Check that “No matching files or folders.” is not shown
  - Expected: The trailing space is ignored: the search runs for “pdf” and sample.pdf is listed.

- **Edit folder with a blank name cannot close as if saved** — passed (sheet-bugs/dd.spec.js:497)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-07 folder <number>” with Create Folder
  - Steps: Hover the folder, click ⋯ (More actions) and choose “Edit” → In “Edit folder”, clear the name → Check that “Save” is disabled, or that pressing it keeps the dialog open with a “name is required” message → Type only spaces as the name → Check the same again: Save disabled, or an error with the dialog still open → Click “Cancel” → Check that the folder is still listed under its original name
  - Expected: A blank or spaces-only name cannot be saved: Save is disabled (or shows an error and stays open), and the folder keeps its name.

- **a search unticks the files it hides, so Delete removes only what is listed** — bug · known bug web dev: the bar stays at "2 selected" and Delete removes the hidden file too (fix in PR #6685, not merged),web dev: the bar stays at "2 selected" and Delete removes the hidden file too (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:535)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-11 folder <number>”, opens it and uploads a PDF (sample.pdf) and a Word file (sample.docx)
  - Steps: Tick sample.pdf and sample.docx → Check that the selection bar reads “2 selected” → Type “docx” in “Search files & folders...” → Check that only sample.docx is listed → Check that the selection bar now reads “1 selected” → Click “Actions” → “Delete”, check that the question reads “Delete 1 item?”, and confirm → Check that sample.docx is gone → Clear the search and check that sample.pdf is still there
  - Expected: A search unticks the files it hides, so Delete removes only the listed file and the hidden PDF survives. Known bug: the bar stays at “2 selected” and Delete removes the hidden file too.
  - Web error: Error: expect(locator).toHaveText(expected) failed

- **a one-file date group reads "1 item", and Publish counts one file** — bug · known bug web dev: the date header reads "1 OF 1 ITEMS" (fix in PR #6685, not merged),web dev: the date header reads "1 OF 1 ITEMS" (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:573)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-14 folder <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Hover sample.pdf and click its globe button (“Publish”) → Check that the Publish window says “1 of 1 file selected” and never “1 documents”, “1 files” or “1 items” → Close the Publish window → Click the sort button in the toolbar and choose “Date (newest first)” → Check that the date group header reads “1 item” (or “1 of 1 item”), not “items”
  - Expected: Counts of one are singular: Publish says “1 of 1 file selected” and the date header says “1 item”. Known bug: the date header reads “1 OF 1 ITEMS”.
  - Web error: Error: expect(locator).toContainText(expected) failed

- **the library search × clears the term @smoke** — passed (sheet-bugs/dd.spec.js:608)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Type a term that matches nothing in “Search files & folders...” → Check that “No matching files or folders.” is shown → Click the × at the end of the search box → Check that the search box is empty → Check that the no-match message is gone
  - Expected: The search box’s × empties it and the library is listed again.

- **a search on the main screen finds a file and a folder inside folders** — bug · known bug web dev: the root search sends folder_id=null, so it finds only top-level folders (fix in PR #6690, not merged),web dev: the root search sends folder_id=null, so it finds only top-level folders (fix in PR #6690, not merged) (sheet-bugs/dd.spec.js:623)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-18 parent <number>”, a subfolder “[e2e] DD-18 child <number>” inside it, and uploads a PDF named “e2e-dd18-<number>.pdf” into the child
  - Steps: Go back to the main screen with the home button (“Back to root”) → Type “e2e-dd18-<number>” in “Search files & folders...” → Check that the PDF from the child folder is listed → Replace the search with the child folder’s name → Check that the child folder is listed
  - Expected: A search on the main screen looks inside every folder and finds both the nested file and the nested folder. Known bug: the main-screen search only looks at top-level folders, so neither is found.
  - Web error: Error: expect(locator).toBeVisible() failed

- **in a 700 px window the Name column keeps most of the list width** — bug · known bug web dev: Name is about 108 px at a 700 px window, Date/Size/actions keep fixed widths (fix in PR #6690, not merged),web dev: Name is about 108 px at a 700 px window, Date/Size/actions keep fixed widths (fix in PR #6690, not merged) (sheet-bugs/dd.spec.js:656)
  - Where: Tools → Document Distribution, in a browser window 700 px wide · account: iPhone XRR
  - Steps: Make the browser window 700 px wide (and 900 px tall) → Open Tools → Document Distribution → Measure the width of the Name column header and of the whole list → Check that Name takes at least 35% of the list’s width
  - Expected: In a 700 px window the Name column keeps at least about a third of the list’s width. Known bug: Name is only about 108 px wide (under a fifth of the list), while Date, Size and the actions keep fixed widths.
  - Web error: Error: Name 92px of 580px

- **Move here stays on screen when the folder list is long** — passed (sheet-bugs/dd.spec.js:672)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-23 folder <number>” with Create Folder
  - Steps: Tick the folder → Click “Actions” → “Move…” → Check that “Move here” is visible in the “Move items” dialog → Scroll the folder list down to the last destination → Check that “Move here” is still visible → Click “Cancel”
  - Expected: However long the folder list, “Move here” stays on screen, so a move can always be finished. Nothing is moved.

- **the breadcrumb home button leaves a nested folder for the main screen** — passed (sheet-bugs/dd.spec.js:698)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-25 parent <number>” with a subfolder “[e2e] DD-25 child <number>” inside it, and opens the child
  - Steps: Check that the breadcrumb shows the parent and then the child → Click the home button (“Back to root”) at the start of the breadcrumb → Check that no folder is open any more → Check that the parent folder is listed on the main screen
  - Expected: From two folders deep, the breadcrumb names both folders and the home button goes straight back to the main screen.

- **a dismissed "How to use this module" stays dismissed in other folders @smoke** — passed (sheet-bugs/dd.spec.js:724)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click × (Dismiss) on the “How to use this module” banner → Check that the banner is hidden → Open the first folder on the main screen → Check that neither the banner nor its short one-line form is shown → Click the home button (“Back to root”) → Check that the banner is still hidden
  - Expected: Once dismissed, “How to use this module” stays hidden inside other folders and back on the main screen.

- **the view toggles and the breadcrumb home button are named for what they do @smoke** — passed (sheet-bugs/dd.spec.js:743)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Inspect the library’s view toggles with a screen reader or the browser’s accessibility view → Check that the list toggle is named “List view” → Check that the grid toggle is named “Grid view” → Open the first folder on the main screen → Check that the breadcrumb’s home button is named with “home” or “root”
  - Expected: Screen readers hear “List view”, “Grid view”, and a home or root name for the breadcrumb’s home button.

- **a row checkbox is named for its row, not left blank or "Select all"** — bug · known bug NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: the row and header checkboxes have no accessible name at all,NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: the row and header checkboxes have no accessible name at all (sheet-bugs/dd.spec.js:757)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Inspect the first library row’s checkbox with a screen reader or the browser’s accessibility view → Check that the checkbox has a name → Check that the name is not “Select all” → Also check that the header checkbox is named “Select all” (noted, but it does not stop the test)
  - Expected: Each row checkbox has its own name, and the header checkbox is named “Select all”. Known bug: the row and header checkboxes have no accessible name at all.
  - Web error: Error: expect(locator).toHaveAccessibleName(expected) failed

- **Create Folder offline shows a plain error and keeps the dialog** — passed (sheet-bugs/dd.spec.js:774)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the folder-create request fail as if the internet were down
  - Steps: Click “Create Folder” in the toolbar → Type “[e2e] DD-35 folder <number>” in Name → Click “Create” → Check that an error message appears → Check that the message shows no web address, server name, technical error code or code trace → Check that the dialog stays open with the name still typed → Click “Cancel”
  - Expected: Offline, Create Folder shows a plain error and keeps the dialog with the name, so nothing typed is lost.

- **a move the server rejects shows its error and no "Moved"** — bug · known bug web dev: a 404 {message} body resolves as success, so both "Moved 1 item" and "Moved" show (fix in PR #6685, not merged),web dev: a 404 {message} body resolves as success, so both "Moved 1 item" and "Moved" show (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:812)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates two folders, “[e2e] DD-17 to <number>” and “[e2e] DD-17 from <number>”, uploads a PDF (sample.pdf) into “from” and opens it. The test makes the server refuse the move with a not-found error that carries a message
  - Steps: Tick sample.pdf → Click “Actions” → “Move…” → In “Move items”, click the “to” folder and click “Move here” → Watch the messages that follow for a few seconds → Check that no “Moved” success message appears → Check that an error message appears → Check that sample.pdf is still listed in the folder
  - Expected: A refused move shows the server’s error, no “Moved”, and the file stays where it was. Known bug: the refusal is treated as success, so both “Moved 1 item” and “Moved” show.
  - Web error: Error: success toasts

- **a move shows one success message, not two** — bug · known bug web dev: every move shows "Moved 1 item" and "Moved" (fix in PR #6685, not merged),web dev: every move shows "Moved 1 item" and "Moved" (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:854)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e] DD-17 mover <number>” and “[e2e] DD-17 home <number>”, with Create Folder
  - Steps: Tick the mover folder → Click “Actions” → “Move…” → In “Move items”, click the home folder and click “Move here” → Watch the success messages for a few seconds → Check that exactly one “Moved” message appears → Check that the mover folder is no longer listed on the main screen
  - Expected: A move shows one success message and the folder leaves the main screen. Known bug: every move shows both “Moved 1 item” and “Moved”.
  - Web error: Error: "Moved" toasts

- **Move and Attach tell same-named items apart by their folder** — passed (sheet-bugs/dd.spec.js:886)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e] DD-30 A <number>” and “[e2e] DD-30 B <number>”, each holding a subfolder “[e2e] DD-30 dup <number>” with the same PDF “e2e-dd30-<number>.pdf” in it
  - Steps: On the main screen, click “Compose email”, then “Attach from library” → Type the PDF’s name in “Search by filename” → Check that two rows are listed, one showing a folder path through A and one through B → Click “Cancel” in the picker and close the composer → Tick folder A and click “Actions” → “Move…” → Check that “Move items” lists the dup folder twice → Check that each dup folder sits directly under its own parent, indented one step further → Click “Cancel”
  - Expected: Same-named items can be told apart: Attach from library shows each file’s folder path, and Move shows each dup folder indented under its own parent. Nothing is attached or moved.

- **Watermark PDF and share opens the composer watermarked, with no toggle advice** — passed (sheet-bugs/dd.spec.js:943)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-03 folder <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Tick sample.pdf → Click “Actions” → “Watermark PDF and share” → Check that the email composer opens with sample.pdf attached → Check that the attachment’s Watermark switch is on → Check that no message tells you to toggle Watermark → Close the composer
  - Expected: The composer opens with the PDF already set to be watermarked, and no advice to toggle Watermark (which would turn it off). Nothing is sent.

- **a failed watermark stamp shows an error and saves nothing** — passed (sheet-bugs/dd.spec.js:971)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-04 folder <number>”, opens it and uploads a PDF (sample.pdf). The test makes the watermark request fail: first with a not-found error, then with a normal-looking answer that reports a failure
  - Steps: Hover sample.pdf and click its watermark button (“Watermark + download”) → In “Download with watermark”, click “Download” → Check that an error message appears, with no raw browser error text → Click “Download” again (this time the failure comes inside a normal answer) → Check that another error message appears, again with no raw browser error text → Check that no file was downloaded and no “Downloaded watermarked copy” message appeared → Click “Cancel”
  - Expected: A failed stamp shows a plain error each time and saves no file.

- **Publish and Download with watermark read "one of the modules" and "this download only"** — bug · known bug web dev: "PUBLISH IN ANY ONE OF THE MODULE BELOW" and "applies to this send only" (fix in PR #6685, not merged),web dev: "PUBLISH IN ANY ONE OF THE MODULE BELOW" and "applies to this send only" (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:1019)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-22 folder <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Hover sample.pdf and click its globe button (“Publish”) → Check that the window reads “Publish in one of the modules below”, not “any one of the module below” → Close the Publish window → Hover sample.pdf and click its watermark button (“Watermark + download”) → Check that the note in “Download with watermark” says “this download only”, not “this send only” → Click “Cancel”
  - Expected: Publish reads “Publish in one of the modules below”, and the watermark download note says the change applies to “this download only”. Known bug: Publish reads “PUBLISH IN ANY ONE OF THE MODULE BELOW” and the note says “applies to this send only”.
  - Web error: Error: expect(locator).toContainText(expected) failed

- **Watermark PDF and download leaves a non-PDF off the list or says it is skipped** — passed (sheet-bugs/dd.spec.js:1051)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-33 folder <number>”, opens it and uploads a PDF (sample.pdf) and a Word file (sample.docx)
  - Steps: Tick sample.pdf and sample.docx → Click “Actions” → “Watermark PDF and download” → Check that the Files step lists sample.pdf → If sample.docx is listed too, check that the screen says it will be skipped or is not supported → Click “Cancel”
  - Expected: The Word file is either left off the list or clearly marked as skipped. Nothing is downloaded.

- **unticking an attached file in Attach from library removes it** — passed (sheet-bugs/dd.spec.js:1080)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-01 folder <number>”, opens it and uploads a PDF (sample.pdf) and a Word file (sample.docx)
  - Steps: Click “Distribute this folder” → Check that the composer shows two attachments → Click “Attach from library” and click the e2e folder in the picker → Check that both files are listed and sample.docx is ticked → Click sample.docx to untick it → Check that the button reads “Attach 1 selected” and click it → Check that only sample.pdf is left attached → Close the composer
  - Expected: Unticking an attached file in the picker removes it from the email: only sample.pdf stays attached. Nothing is sent.

- **Attach from library with nothing ticked has a disabled Attach button** — passed (sheet-bugs/dd.spec.js:1114)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Compose email” in the toolbar → Click “Attach from library” → Check that no file is ticked in “Attach documents” → Check that the Attach button is disabled and shows no “0” → Click “Cancel” and close the composer
  - Expected: With nothing ticked, the picker’s Attach button is disabled and does not read “Attach 0 selected”.

- **closing the composer with a typed subject asks first or keeps the draft** — bug · known bug NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: Cancel closes at once and Compose reopens blank (same as the Android report),NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: Cancel closes at once and Compose reopens blank (same as the Android report) (sheet-bugs/dd.spec.js:1127)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Compose email” in the toolbar → Type “[e2e] DD-24 subject <number>” in Subject → Click “Cancel” in the composer’s footer → If a question about discarding or leaving appears, choose to discard (the test ends there, passing) → Otherwise click “Compose email” again → Check that Subject still holds the typed subject → Close the composer
  - Expected: Closing a composer with a typed subject either asks before throwing it away or keeps it for next time. Known bug: Cancel closes at once and Compose email reopens blank.
  - Web error: Error: expect(locator).toHaveValue(expected) failed

- **an invalid address is flagged before Preview and left out of its To line** — passed (sheet-bugs/dd.spec.js:1158)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Compose email” in the toolbar → Type a valid address “e2e+<number>@example.com” in To and press Enter → Type “notanemail” in To and press Enter → Check that two chips are shown → If Preview is disabled, stop: the invalid address is blocked, which is correct → Otherwise check that “Invalid email address: notanemail” is shown → Click “Preview” and check that its To line lists the valid address but not “notanemail” → Click “Edit” and close the composer
  - Expected: An invalid address is flagged before Preview (or Preview is blocked), and Preview’s To line lists only the valid address. Nothing is sent.

- **Compose and Share documents inside a folder are titled Compose email** — passed (sheet-bugs/dd.spec.js:1186)
  - Where: Tools → Document Distribution, inside the test’s own e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e] DD-28 folder <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Inside the folder, click “Compose email” in the toolbar → Check that the composer is titled “Compose email”, not “Distribute folder” → Close the composer → Tick sample.pdf and click “Actions” → “Share documents” → Check that the composer is again titled “Compose email”, not “Distribute folder” → Close the composer
  - Expected: Only sending a whole folder is titled “Distribute folder”; Compose email and Share documents are titled “Compose email”. Nothing is sent.

- **Email Preview never runs a script stored in a template** — bug · known bug web dev: Preview renders the template HTML unsanitised, so an onerror handler runs (open, no fix yet),web dev: Preview renders the template HTML unsanitised, so an onerror handler runs (open, no fix yet) (sheet-bugs/dd.spec.js:1214)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server list one template, “[e2e] DD-10 stub template”, whose message holds a broken image with a harmless script that only counts inside the browser
  - Steps: Click “Compose email” and add a valid address to To → Click “Templates” and pick “[e2e] DD-10 stub template” under “Load template” → If asked whether to replace your message, choose “Replace” → Check that Subject reads “[e2e] DD-10 stub subject” → Click “Preview” and check that the template’s text is shown → Wait two seconds → Check that the template’s script did not run and no script is left in the preview → Click “Edit” and close the composer
  - Expected: Preview shows the template’s text but never runs a script stored in it. Nothing is saved or sent. Known bug: Preview renders the template’s HTML unfiltered, so the script runs.
  - Web error: Error: onerror handler runs

- **loading a template over a written message asks first** — bug · known bug web dev: the template replaces subject and message with no question (fix in PR #6691, not merged),web dev: the template replaces subject and message with no question (fix in PR #6691, not merged) (sheet-bugs/dd.spec.js:1247)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server list one template, “[e2e] DD-20 stub template”
  - Steps: Click “Compose email” in the toolbar → Click into Message and type a note, “E2E-DD20-NOTE-<number>” → Click “Templates” and pick “[e2e] DD-20 stub template” under “Load template” → Check that a question asks whether to replace your message → Choose “Keep my message” → Check that the note is still in Message and Subject is not the template’s subject → Close the composer
  - Expected: Loading a template over a written message asks first, and Keep my message keeps the note. Known bug: the template replaces the subject and message without asking.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a loaded template keeps the signature at the end** — bug · known bug web dev: loading a template drops the auto-inserted signature (fix in PR #6691, not merged),web dev: loading a template drops the auto-inserted signature (fix in PR #6691, not merged) (sheet-bugs/dd.spec.js:1272)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server list one email signature, “[e2e] DD-20 signature”, set for new emails, and one template, “[e2e] DD-20 stub template”
  - Steps: Click “Compose email” in the toolbar → Check that Message already holds the signature → Click “Templates” and pick “[e2e] DD-20 stub template” under “Load template”; if asked, choose “Replace” → Check that Message now holds the template’s text → Check that the signature is still there, after the template’s text → Close the composer
  - Expected: The template’s text goes in and the signature stays at the end. Known bug: loading a template drops the signature that was added automatically.
  - Web error: Error: expect(locator).toContainText(expected) failed

- **a signature with a name but no text says so and stays open** — passed (sheet-bugs/dd.spec.js:1308)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server report no saved signatures, and blocks any signature save so the account’s real signatures are never touched
  - Steps: Click “Compose email” in the toolbar → Click “Signature”, then “Manage signatures” → Click “Create Signature” → Type “[e2e] DD-32 signature <number>” as the signature name and leave its text empty → Click “Save” → Check that “Please enter signature content” appears → Check that the name is still typed and nothing was saved → Close the signatures window and the composer
  - Expected: A signature with a name but no text is refused with “Please enter signature content”; the window stays open and nothing is saved.

- **Share some documents mails the members on screen, saved or not** — bug · known bug web dev: the composer opens with the list as last saved, so To is empty (fix in PR #6685, not merged),web dev: the composer opens with the list as last saved, so To is empty (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:1343)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Distribution lists” in the toolbar, click “New list”, type “[e2e] DD-12 list <number>” in “Enter a list name” and click “Create” → Add two members, each with Email, Name and “Add to the list”, without saving → Click “Share some documents” → If asked to save your changes, choose “Save” → Check that the composer opens → Check that To holds both members (shown by address or by name) → Close the composer without sending
  - Expected: Share some documents addresses the members shown on screen, saved or not. Known bug: the composer opens with the list as last saved, so To is empty.
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **leaving a list with unsaved members asks first** — bug · known bug web dev: Back and Close this list leave at once and the members are lost (fix in PR #6685, not merged),web dev: Back and Close this list leave at once and the members are lost (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:1377)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Distribution lists” in the toolbar, click “New list”, type “[e2e] DD-16 list <number>” in “Enter a list name” and click “Create” → Add one member (Email, Name, “Add to the list”) without saving → Click “Back” → Check that a question about unsaved changes appears, or that the list saved itself and reads “1 person” → If asked, choose “Keep editing” and check that the member is still listed → Click “Close this list” and check that the question appears again → Choose “Discard” and check that you are back on the overview of lists
  - Expected: Leaving a list with unsaved members asks first (or saves it), so members are never lost without warning. Known bug: Back and Close this list leave at once and the members are lost.
  - Web error: Error: expect(locator).toBeVisible() failed

- **a second list with an existing name is refused** — bug · known bug web dev: "List created" twice, two lists with one name; the backend accepts it too (open, no fix yet),web dev: "List created" twice, two lists with one name; the backend accepts it too (open, no fix yet) (sheet-bugs/dd.spec.js:1410)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Distribution lists” in the toolbar, click “New list”, type “[e2e] DD-19 list <number>” in “Enter a list name” and click “Create” → Click “Back” to return to the overview of lists → Click “New list” again, type exactly the same name and click “Create” → Wait for an error message or for a second list to open (if it opens, click “Back”) → Type the name in “Search for a list” → Check that only one list has that name and no second “List created” message appeared
  - Expected: A second list with an existing name is refused. Known bug: “List created” shows twice and two lists share the name (the server accepts it too).
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **a list with one member reads "1 person"** — bug · known bug web dev: custom lists read "1 people" (fix in PR #6685, not merged),web dev: custom lists read "1 people" (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:1447)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Distribution lists” in the toolbar, click “New list”, type “[e2e] DD-22 list <number>” in “Enter a list name” and click “Create” → Add one member (Email, Name, “Add to the list”) → Click “Save changes” and wait for the save → Click “Back” (choose “Discard” if asked) → Check that the list’s row reads “1 person”, not “1 people”
  - Expected: A list with one member reads “1 person”. Known bug: it reads “1 people”.
  - Web error: Error: expect(locator).toContainText(expected) failed

- **a second template with an existing name is refused** — bug · known bug NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: no name check, so the template is created twice (backend accepts it too; open),NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: no name check, so the template is created twice (backend accepts it too; open) (sheet-bugs/dd.spec.js:1481)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Templates” in the toolbar, then “New template” → Type “[e2e] DD-31 template <number>” as Name and “[e2e] DD-31 template <number> subject” as Subject, then click “Create template” → Check that the template is listed once → Click “New template” again, type exactly the same name and subject, and click “Create template” → Wait for an error message or for the dialog to close; if it is still open, click “Cancel” → Check that only one template has that name
  - Expected: A second template with an existing name is refused. Known bug: there is no name check, so the template is created twice (the server accepts it too).
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **a History record that fails to load says so, with Try again and Back** — bug · known bug web dev: the detail spins forever with no Back button (fix in PR #6685, not merged),web dev: the detail spins forever with no Back button (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:1512)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server list one History record, “[e2e] DD-08 mock <number>”, and fail to load its details with a server error
  - Steps: Click “History” in the toolbar → Click the “[e2e] DD-08 mock <number>” row → Check that “Back to history” and a “Try again” button are shown → Click “Back to history” → Check that the record is listed again → Close the drawer
  - Expected: A record that fails to load says so and offers Try again and Back to history. Known bug: the detail spins forever with no Back button.
  - Web error: Error: expect(locator).toBeVisible() failed

- **Contacts Export CSV starts with a UTF-8 BOM, so Excel shows accents** — bug · known bug web dev: Address Book.csv has no byte-order mark (fix in PR #6685, not merged),web dev: Address Book.csv has no byte-order mark (fix in PR #6685, not merged) (sheet-bugs/dd.spec.js:1545)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server list one contact, “Zoë Müller”
  - Steps: Click “Contacts” in the toolbar → Check that “Zoë Müller” is listed in the Address book → Click “Export CSV” → Check that the downloaded file contains “Zoë Müller” → Check that the file starts with the UTF-8 byte-order mark (bytes EF BB BF), which tells Excel to show the accents → Close the Address book
  - Expected: The exported CSV starts with a UTF-8 byte-order mark, so Excel shows “Zoë Müller” correctly. Known bug: Address Book.csv has no byte-order mark, so Excel garbles accented names.
  - Web error: Error: first three bytes

- **Contacts that fail to load show an error with Retry, not "No contacts yet"** — bug · known bug NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: an error toast, then "No contacts yet." with no way to retry,NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: an error toast, then "No contacts yet." with no way to retry (sheet-bugs/dd.spec.js:1566)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the contact list fail to load, as if the internet were down
  - Steps: Click “Contacts” in the toolbar and wait for loading to finish → Check that “No contacts yet” is not shown → Check that a “Retry” (or “Try again”) button is shown → Let the contact list load again (the test now answers with one contact, “[e2e] DD-36 contact <number>”) → Click “Retry” → Check that the contact is listed → Close the Address book
  - Expected: A failed load shows an error with Retry, and Retry loads the contacts. Known bug: an error message, then “No contacts yet.” with no way to retry.
  - Web error: Error: expect(locator).toHaveCount(expected) failed

### Document Distribution

- **composer: recipient chip + subject reach the preview stage (never Send)** — passed (dd.spec.js:265)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Click “Compose email” in the toolbar → Type “e2e-dd-noreply@example.com” in To and press Enter → Check that one recipient chip is shown → Type “[e2e-dd] subject <number>” in Subject → Check that “Preview” is enabled and click it → Check that the preview shows the subject and a “Send” button → Close the composer with its ×; if it asks to keep the email as a draft, click Discard (never click Send)
  - Expected: With one valid recipient and a subject, Preview is enabled and the preview shows the subject with Send ready. Nothing is sent.

- **DD-F10: folder ✈ opens the Distribute folder composer** — passed (dd.spec.js:448)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] dist-folder <number>” with Create Folder
  - Steps: Hover the folder row and click its paper-plane button (“Distribute this folder”) → Check that the email composer opens → Check that the composer is titled “Distribute folder” → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: The folder’s paper-plane button opens the email composer titled “Distribute folder”. Nothing is sent.

- **DD-H07: Duplicate opens a prefilled composer (never sent)** — failed (dd.spec.js:676)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar and click the first distribution → Click “Duplicate” (the copy icon) in the detail header → Check that the email composer opens → Check that it is filled from the record: attachments counted, a recipient chip, or a subject (any one of them) → Close the composer with its ×; if it asks to keep the email as a draft, click Discard (never click Send)
  - Expected: Duplicate opens the composer filled from the record (its attachments, recipients or subject). Nothing is sent.
  - Web error: Error: Duplicate did not prefill attachments, recipients or subject

- **DD-C02: an invalid address alone does not enable Preview** — passed (dd.spec.js:710)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Click “Compose email” in the toolbar → Type “not-an-email” in To and press Enter → Check that one recipient chip is shown → Type “[e2e-dd] invalid <number>” in Subject → Check that “Preview” is disabled → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: With only an invalid address in To, Preview stays disabled even when a subject is filled in.

- **DD-C03: Cc / Bcc toggle reveals both rows** — passed (dd.spec.js:729)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Compose email” in the toolbar → Check that the composer shows four field labels and one address box (To, “alice@studio.com”) → Click “Cc / Bcc” → Check that six labels are now shown: To, Cc, Bcc, Subject, Message and Attachments → Check that the three address boxes read “alice@studio.com”, “cc@studio.com” and “bcc@studio.com” → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: Cc / Bcc adds a Cc and a Bcc address field next to To. The test does not add addresses to them.

- **DD-C08: Edit from preview returns with fields intact** — passed (dd.spec.js:760)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Click “Compose email” in the toolbar → Type “e2e-dd-noreply@example.com” in To and press Enter → Type “[e2e-dd] edit-roundtrip <number>” in Subject → Click “Preview” and check that the preview is shown → Click “Edit” → Check that Subject still holds the typed subject → Check that To still shows one recipient chip → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: Going back from Preview with Edit keeps the subject and the recipient. Nothing is sent.

- **DD-C04: attach a library file and remove it** — passed (dd.spec.js:787)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] attach-src <number>” and uploads a PDF (sample.pdf) into it. The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Go back to the main screen and click “Compose email” in the toolbar → Click “Attach from library” → In “Attach documents”, type “sample.pdf” in “Search by filename” → Click the first sample.pdf result → Click the Attach button (“Attach 1 selected”) → Check that sample.pdf is listed under Attachments in the composer → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: The file picked from the library appears in the composer’s attachment list. Despite the title, the test does not remove the attachment; it just closes the composer.

- **DD-T02/04: template create, use in composer, delete** — flaky (dd.spec.js:898)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Click “Templates” in the toolbar, then “New template” → In “Save email template”, type “e2e template <number>” as Name and Description, “[e2e-dd] tpl subject <number>” as Subject, and “Template body from e2e.” as the message → Click “Create template”, check that it is listed in Email Templates, and close the drawer → Click “Compose email”, click “Templates” and pick the new template under “Load template” → Check that Subject now reads “[e2e-dd] tpl subject <number>”, then close the composer (click Discard if it asks to keep a draft) → Open “Templates” again, click the template’s Delete button and confirm with “Delete” → Check that the template is gone, then close the drawer
  - Expected: A new template appears in the Email Templates list, loading it in the composer fills the subject, and Delete removes it.
  - Web error: Test timeout of 150000ms exceeded.

- **DD-C07: a distribution list fills composer recipients** — failed (dd.spec.js:1215)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Click “Compose email” in the toolbar → Click the composer’s own “Distribution lists” button next to To → Check that the “Use a distribution list” box opens → Open “Select a list” and pick the first list → If the box still has a button to use the list (“Use”, “Add” or “Apply”), click it; current builds add the people as soon as a list is picked → Check that at least one recipient chip appears in To → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: Picking a distribution list fills To with its members. Nothing is sent.
  - Web error: Test timeout of 150000ms exceeded.

- **DD-D04: the upload card names each file while it uploads, then goes away** — passed (dd.spec.js:1345)
  - Where: Tools → Document Distribution, inside a new e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] progress <number>” with Create Folder and opens it. The test holds each file’s save for 4 seconds before passing it on, so the card stays up long enough to read
  - Steps: Click “Upload” → “Upload files” and pick sample.pdf and sample.docx together → Check that the upload card reads “Uploading 1 of 2” and names one of the two files → Check that it then reads “Uploading 2 of 2” and names the other file → Check that the card goes away once both are done → Check that both files are listed in the folder
  - Expected: While files upload, a card counts them (“Uploading 1 of 2”, then “2 of 2”) and names the file in progress; it goes away when the upload ends and both files are listed.

- **DD-D15: the library lists 50 files first and loads the rest when you scroll to the end** — passed (dd.spec.js:1386)
  - Where: Tools → Document Distribution (main screen) · account: iPhone XRR
  - Setup: The test answers the main screen’s file list with 60 made-up files, “[e2e-dd] page-doc 01.pdf” to “60.pdf”, handed out 50 at a time; the folders are the real ones
  - Steps: Open Document Distribution → Check that 50 of the made-up files are listed and the next batch has not been asked for → Check that a “Loading more…” row sits at the end of the list → Scroll down to that row → Check that the list grows to all 60 files, ending with “page-doc 60” → Check that the “Loading more…” row is gone
  - Expected: The library shows the first 50 files, asks for the next batch only when you scroll to the end, appends it, and stops offering more once everything is loaded.

- **DD-C06: the saved signature is added on open, and Signature appends the one you pick** — passed (dd.spec.js:1435)
  - Where: Tools → Document Distribution → Compose email · account: iPhone XRR
  - Setup: The test answers the account’s email signature list with two made-up signatures: “[e2e-dd] Sig auto” (marked for new emails) and “[e2e-dd] Sig picked”. The test answers the composer’s automatic draft saves itself, so no draft is stored
  - Steps: Click “Compose email” in the toolbar → Check that the message already ends with the “Sig auto” text and has no “Sig picked” text → Click “Signature” above the message → In “Insert signature”, open “Select a signature” and pick “[e2e-dd] Sig picked” → Check that the “Sig picked” text is added to the message after the “Sig auto” text → Check that a message says Inserted “[e2e-dd] Sig picked” → Close the composer with its ×; if it asks to keep the email as a draft, click Discard
  - Expected: A new email starts with the signature marked for new emails, and picking another one under Signature appends it below. The account’s real signatures are never read or changed.

- **DD-L05: Download as CSV on a distribution list saves its members in a file named after the list** — passed (dd.spec.js:1490)
  - Where: Tools → Document Distribution → Distribution lists · account: iPhone XRR
  - Setup: Creates a list “[e2e-dd] export-list <number>” with one member, “E2E Export Member” (e2e-dd-export-<number>@example.com), and saves it
  - Steps: Click “Distribution lists” in the toolbar, then “New list” → Type the list name and click “Create” → Type the member’s address in Email and “E2E Export Member” in Name, click “Add to the list”, then “Save changes” → Click “Back” to return to the lists → Click the download icon (“Download as CSV”) on the list’s row → Check that a file named “<list name>.csv” is downloaded → Check that its first line names the columns (email among them) and that it holds the member’s address and name
  - Expected: Download as CSV saves the list’s members as a CSV file named after the list.

- **DD-A05: a CSV import adds its valid rows to a distribution list and flags the bad and repeated ones** — passed (dd.spec.js:1522)
  - Where: Tools → Document Distribution → Distribution lists · account: iPhone XRR
  - Setup: Creates a list “[e2e-dd] csv-list <number>”. The CSV file used has the columns name, email, job, department and four rows: two people with good addresses, one with the address “not-an-email”, and one repeating the first address
  - Steps: Click “Distribution lists”, then “New list”; type the name and click “Create” → Under “Import a file (.csv)”, click “Browse…” → In “Import recipients”, choose the CSV file → Check that four rows are shown: two good ones, one tagged “invalid email” and one tagged “duplicate” → Click “Add 2 contacts” → Check that both good addresses are in the Recipients table and “not-an-email” is not → Click “Save changes”, close the dialog and open “Distribution lists” again → Open the list and check that both people are still there, the second with role “Producer” and department “Production”
  - Expected: Only the valid, non-repeated rows are added; the bad and the repeated row are flagged; the saved list keeps both people with their role and department.

- **DD-P02: Publish a library PDF to Info sends one request for that file and confirms it** — passed (dd.spec.js:1565)
  - Where: Tools → Document Distribution, inside a new e2e folder · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] publish-info <number>” and uploads sample.pdf into it. The test answers the publish request itself with a success, so nothing is posted to Info
  - Steps: Hover the sample.pdf row and click its globe button (“Publish to the project”) → In “Publish files”, click the Info card (“General notices and updates”) → Check that the footer reads “1 of 1 file selected” → Click “Publish” → Check that a message says “Distributed 1 file via Info” and the dialog closes → Check that exactly one publish request went out, for Info and for that one file
  - Expected: Publishing a library PDF to Info sends one request naming Info and that file, says “Distributed 1 file via Info” and closes the dialog.

- **DD-P03: Publish to Doc Distribution on an Info document files it in the library’s Info folder** — flaky (dd.spec.js:1611)
  - Where: Tools → Info, then Tools → Document Distribution · account: iPhone XRR
  - Setup: Notes whether the library already has a folder named exactly “Info”, then posts a PDF “e2e-dd-from-tool-<number>.pdf” in Info with “Document Upload” and the caption “[e2e] dd-from-tool <number>”
  - Steps: Open the posted message’s options and click “Publish to Doc Distribution” → Check that the “Publish to Document Distribution” dialog names the file, and click “Publish” → Check that “Published.” appears naming the file, and click OK → Open Document Distribution and open the “Info” folder → Search for the number in the file name and check that the file is listed → Click the file’s Delete button and confirm with “Delete” → Check that the file is gone
  - Expected: Publish to Doc Distribution on an Info document asks first, confirms with “Published.”, and files the document in the library’s “Info” folder.
  - Web error: Test timeout of 450000ms exceeded.

- **DD-H11: a teammate’s send announced over the live connection refreshes the open History drawer** — passed (dd.spec.js:1678)
  - Where: Tools → Document Distribution → History · account: iPhone XRR
  - Setup: The test passes the page’s live connection through to the server and, once the drawer is open, answers its list reloads with the real list plus one made-up distribution “[e2e-dd] live send <number>”
  - Steps: Click “History” in the toolbar and note the total in the drawer header → Play the server’s “a distribution was sent” announcement into the page, as if another device had sent one → Check that “[e2e-dd] live send <number>” appears in the open drawer without reopening it → Check that the drawer reloaded its list and the header total went up by one → Close the drawer
  - Expected: When someone else sends a distribution, the open History drawer refreshes by itself, lists it and updates the total. Nothing is sent.

### Document Distribution — slot b with lowered rights (User Management)

- **DD-R01: without the posting right, slot b sees the View-only banner and every write control disabled** — failed (dd.spec.js:1766)
  - Where: Tools → Document Distribution (as Lava Device); Settings → Admin Settings → User Management (as iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR creates a folder “[e2e-dd] rights <number>” with sample.pdf in it; then, in User Management (in a separate browser window), turns off Lava Device’s Admin switch and, in its posting rights under Tools → Document Distribution, turns Post off with View and Download on
  - Steps: As Lava Device, open Document Distribution → Check that the banner reads “View-only access” and offers one button (to request posting access) → Check that Upload, Create Folder and Compose email are disabled, while Templates, Distribution lists and Contacts stay enabled → Hover the e2e folder’s row and check that its Distribute and Publish buttons are disabled → Open the folder, hover sample.pdf and check that Delete and Distribute are disabled but Download is enabled
  - Expected: Without the posting right, the member sees a “View-only access” banner and every write control is disabled, while browsing and downloading still work.
  - Web error: Test timeout of 400000ms exceeded.

- **DD-R02: without the view right, slot b cannot stay in Document Distribution** — passed (dd.spec.js:1822)
  - Where: Tools → Document Distribution (as Lava Device); Settings → Admin Settings → User Management (as iPhone XRR) · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR turns off Lava Device’s Admin switch and its Document Distribution View, Post and Download rights in User Management
  - Steps: As Lava Device, open Tools and note whether the Document Distribution tile is shown → Open Document Distribution by its direct link → Check that no Document Distribution window stays open and the module does not stay on screen → In the classic layout, check that the Tools page is shown instead
  - Expected: Without the view right, the member cannot stay in Document Distribution: the classic layout sends them to the Tools page and windowed view closes the window.

### 

- **leftovers of the dd-ph1 run** — flaky (.e2e-scratch/dd-cleanup.spec.js:24)
  - Web error: Test timeout of 600000ms exceeded.

### Document Distribution

- **DD-H07: Duplicate opens a prefilled composer (never sent)** — passed (e2e/dd.spec.js:679)

- **DD-T02/04: template create, use in composer, delete** — passed (e2e/dd.spec.js:905)

- **DD-C07: a distribution list fills composer recipients** — passed (e2e/dd.spec.js:1218)

- **DD-C06: the saved signature is added on open, and Signature appends the one you pick** — passed (e2e/dd.spec.js:1435)

- **DD-P03: Publish to Doc Distribution on an Info document files it in the library’s Info folder** — passed (e2e/dd.spec.js:1608)

### Document Distribution — slot b with lowered rights (User Management)

- **DD-R01: without the posting right, slot b sees the View-only banner and every write control disabled** — passed (e2e/dd.spec.js:1769)

- **DD-R02: without the view right, slot b cannot stay in Document Distribution** — passed (e2e/dd.spec.js:1828)

- **DD-R99: slot b is an admin again after the rights tests** — passed (e2e/dd.spec.js:1863)

### Document Distribution

- **History search filters server-side and clears back** — notrun (dd.spec.js:143)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar → Type a term that matches nothing, such as “zzz-no-match-<number>”, in “Search by subject or recipient name / email” → Check that “No distributions match your search.” appears → Clear the search box → Check that distribution rows are listed again → Close the drawer
  - Expected: A search with no match says “No distributions match your search.” (not the empty-history message), and clearing the search brings the rows back.

- **History paginates: 50 rows first, scroll loads the next page** — notrun (dd.spec.js:167)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar → Check that the total next to the drawer title is more than 50 → Check that exactly 50 rows are loaded at first → Scroll to the bottom of the list inside the drawer → Check that more than 50 rows are now listed → Close the drawer
  - Expected: History loads 50 rows first and adds the next batch when you scroll to the end of the drawer.

- **History row opens the detail view and Back returns to the list** — notrun (dd.spec.js:195)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar → Click the first distribution in the list → Check that the detail view shows the subject and at least one recipient → Click “Back to history” → Check that the list of distributions is shown again → Close the drawer
  - Expected: A distribution opens its detail view with the subject and recipients, and “Back to history” returns to the list.

- **create a folder, open it, delete it** — notrun (dd.spec.js:220)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Create Folder” in the toolbar → Type “[e2e-dd] folder <number>” in Name, set Date to today if it is empty, and click “Create” → Check that the new folder is listed → Click the folder’s name to open it → Check that the breadcrumb shows the folder and the “Upload” button is shown (it only exists inside a folder) → Click the home button (“Back to root”) in the breadcrumb → Hover the folder, click ⋯ (More actions) and choose “Delete” → Confirm with “Delete” and check that the folder is gone
  - Expected: The new folder is listed, opens (showing Upload), and disappears once its Delete is confirmed.

- **upload a PDF into a folder, then delete doc and folder** — notrun (dd.spec.js:234)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] upload <number>” with Create Folder and opens it
  - Steps: Click “Upload” and choose “Upload files” → Pick a small PDF (sample.pdf) → Check that sample.pdf is listed in the folder → Hover the sample.pdf row and click its Delete (trash) button → Confirm with “Delete” and check that the file is gone → Click the home button (“Back to root”) in the breadcrumb → Delete the folder through ⋯ (More actions) → “Delete” and confirm
  - Expected: The uploaded PDF is listed in the folder and disappears once its Delete is confirmed; the folder is then deleted too.

- **DD-S02: info banner dismisses (non-persistent)** — notrun (dd.spec.js:299)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Find the “How to use this module” banner above the library → Click its × (Dismiss) button → Check that the banner is hidden
  - Expected: The “How to use this module” banner closes when dismissed. The test does not reload to check that the banner comes back.

- **DD-S03: theme toggle flips data-theme** — notrun (dd.spec.js:307)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Note whether Document Distribution is in light or dark mode → Click the sun/moon button at the right end of the toolbar (“Switch to dark mode” or “Switch to light mode”) → Check that the screen switches to the other theme → Click the same button again → Check that the screen is back in the original theme
  - Expected: Each click switches Document Distribution between light and dark; the second click restores the original theme.

- **DD-F02: folder description shows in the Description column** — notrun (dd.spec.js:322)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Create Folder” in the toolbar → Type “[e2e-dd] desc-folder <number>” in Name → Type “about <number>” in Description → Leave Date at its default and click “Create” → Check that the new folder’s row shows “about <number>” in the Description column → Delete the folder through ⋯ (More actions) → “Delete” and confirm
  - Expected: The new folder’s row shows the description typed in the Create folder dialog.

- **DD-F04: folder create validation — empty name does not create** — notrun (dd.spec.js:344)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Create Folder” in the toolbar → Leave Name empty → Check that “Create” is disabled → Check that the dialog stays open → Click “Cancel”
  - Expected: Without a name, Create is disabled, so no folder can be made and the dialog stays open.

- **DD-F05: rename folder via ⋯ → Edit** — notrun (dd.spec.js:359)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] rename-before <number>” with Create Folder
  - Steps: Hover the folder, click ⋯ (More actions) and choose “Edit” → In the “Edit folder” dialog, replace the name with “[e2e-dd] rename-after <number>” → Click “Save” → Check that the folder is listed under the new name → Check that the old name is no longer listed
  - Expected: The folder is renamed: the new name is listed and the old one is gone.

- **DD-F03+F08: subfolder create, cascade delete from root** — notrun (dd.spec.js:382)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] parent <number>” with Create Folder and opens it
  - Steps: Inside the parent folder, click “Create Folder” → Check that the dialog title reads “New subfolder inside” followed by the parent’s name → Type “[e2e-dd] child <number>” in Name and click “Create” → Check that the child folder is listed inside the parent → Click the home button (“Back to root”) in the breadcrumb → Delete the parent through ⋯ (More actions) → “Delete” and confirm → Check that the parent folder is gone
  - Expected: Inside a folder, Create Folder makes a subfolder (the dialog names the parent), and deleting the parent from the main screen removes it. The test does not look for the child afterwards.

- **DD-F06: move folder into another folder via selection Actions** — notrun (dd.spec.js:409)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e-dd] mover <number>” and “[e2e-dd] new-home <number>”, with Create Folder
  - Steps: Tick the checkbox of the mover folder → In the selection bar, click “Actions” and choose “Move…” → In the “Move items” dialog, click the new-home folder → Click “Move here” → Check that the mover folder is no longer listed on the main screen → Open the new-home folder → Check that the mover folder is listed inside it
  - Expected: The moved folder leaves the main screen and appears inside the destination folder.

- **DD-F09: bulk delete two folders via selection Actions** — notrun (dd.spec.js:432)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e-dd] bulk-a <number>” and “[e2e-dd] bulk-b <number>”, with Create Folder
  - Steps: Tick the checkbox of the bulk-a folder → Tick the checkbox of the bulk-b folder → In the selection bar, click “Actions” and choose “Delete” → Confirm with “Delete” → Check that neither folder is listed any more
  - Expected: Both ticked folders are deleted in one go.

- **DD-D02/05/06/07/09/10: doc lifecycle — multi-upload, preview, download, watermark gating, single + bulk delete** — notrun (dd.spec.js:470)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] doc-life <number>” with Create Folder and opens it
  - Steps: Click “Upload” → “Upload files” and pick a PDF (sample.pdf) and a Word file (sample.docx) together → Check that both files are listed → Click the sample.pdf name, check that the preview opens showing “sample.pdf”, and close it → Hover sample.pdf, click its Download button, and check that a file named like “sample” downloads → Hover sample.pdf and check that its watermark button (“Watermark + download”) is enabled → Hover sample.docx and check that its watermark button is disabled → Click the Delete (trash) button on sample.docx, confirm with “Delete”, and check that it is gone → Tick sample.pdf, click “Actions” → “Delete”, confirm, and check that it is gone
  - Expected: Two files upload in one pick, the PDF previews and downloads, watermarking is offered for the PDF but not for the Word file, and both the row Delete and the bulk Delete remove files.

- **DD-D08: move a document to another folder** — notrun (dd.spec.js:537)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e-dd] move-from <number>” and “[e2e-dd] move-to <number>”, and uploads a PDF (sample.pdf) into move-from
  - Steps: In the move-from folder, tick sample.pdf → Click “Actions” and choose “Move…” → In the “Move items” dialog, click the move-to folder and click “Move here” → Check that sample.pdf is no longer listed in move-from → Go back to the main screen with the home button (“Back to root”) and open move-to → Check that sample.pdf is listed there
  - Expected: The file leaves its folder and appears in the destination folder.

- **DD-D11: library search filters server-side and clears** — notrun (dd.spec.js:567)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e-dd] search-hit <number>” and “[e2e-dd] search-miss <number>”, with the same number
  - Steps: Type “search-hit <number>” in “Search files & folders...” → Check that the search-hit folder is listed → Check that the search-miss folder is not listed → Clear the search box → Check that the search-miss folder is listed again
  - Expected: The search lists only the matching folder, and clearing it brings the other one back.

- **DD-D13: sort toggles between A–Z and Z–A** — notrun (dd.spec.js:589)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates two folders, “[e2e-dd] aaa <number>” and “[e2e-dd] zzz <number>”, with the same number
  - Steps: Type the shared number in “Search files & folders...” so only the two folders are listed → Check that the aaa folder is listed first (the default sort, Name A–Z) → Click the sort button in the toolbar and choose “Name (Z–A)” → Check that the zzz folder is now listed first → Click the sort button again and choose “Name (A–Z)” → Clear the search box
  - Expected: The default sort is A–Z, and choosing “Name (Z–A)” puts the zzz folder before the aaa folder.

- **DD-D14: list ⇄ grid toggle** — notrun (dd.spec.js:622)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click the “Grid view” button in the toolbar → Check that the library shows its items as cards → Click the “List view” button → Check that the library is shown as a table again
  - Expected: Grid view shows the library as cards, and List view brings the table back.

- **DD-H05/H09: detail analytics render and Refresh keeps them alive** — notrun (dd.spec.js:636)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar and click the first distribution → Check that the status donut chart and its legend are shown → Count the recipient rows (there must be at least one) → Click the “Refresh” button in the detail header → Check that the donut chart is still shown → Check that the same number of recipient rows is listed → Close the drawer
  - Expected: A distribution’s detail shows the status donut, its legend and the recipients, and Refresh reloads them without leaving the detail or changing the recipient count. The test does not compare the legend numbers with the recipients’ statuses.

- **DD-H06: export recipients as CSV downloads a file** — notrun (dd.spec.js:660)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar and click the first distribution → Check that its recipients are listed → Click “Export recipients as CSV” (the download icon) in the detail header → Check that a file ending in .csv downloads → Close the drawer
  - Expected: Export recipients as CSV downloads a .csv file. The test does not open the file to count its rows.

- **DD-L02/03/04: distribution list lifecycle in the manager** — notrun (dd.spec.js:830)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Distribution lists” in the toolbar → Click “New list”, type “[e2e-dd] list <number>” as the list name and click “Create” → In the new list, type “e2e-dd-list@example.com” in Email and “E2E List Member” in Name → Click “Add to the list” → Click “Save changes” → Click “Remove this list” and confirm with “Remove” → Check that the list’s name is no longer shown → Close the Distribution lists window
  - Expected: A new list can be created, given a member and saved, and Remove this list deletes it. Despite the title, the test does not reopen or rename the list to check the saved changes.

- **DD-A02/03/04/05: contact lifecycle in the address book** — notrun (dd.spec.js:865)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Contacts” in the toolbar and wait for the “Address book” → Click “Add contact” → Type “e2e-dd-contact-<number>@example.com” in Email and “E2E Contact <number>” in Name → Press Enter in the Role (designation) field to save the contact → Type the contact’s name in the search box “Name, email, department, list” → Check that the contact’s email is listed → Click the contact’s Delete button and confirm with “Delete” → Check that the email is no longer listed, then close the Address book
  - Expected: A new contact is saved, found by search, and removed by Delete. Despite the title, the test does not edit a contact, check the composer’s suggestions, or import a CSV.

- **DD-P01: Publish modal opens with destination cards** — notrun (dd.spec.js:969)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] publish-src <number>”, opens it and uploads a PDF (sample.pdf)
  - Steps: Hover sample.pdf and click its globe button (“Publish”) → Check that the “Publish files” window opens → Check that at least one destination card (a module to publish into) is shown → Close the window without publishing
  - Expected: Publish opens with at least one destination module card. Nothing is published.

- **DD-H10: send a real distribution and see it land in History** — notrun (dd.spec.js:994)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Contacts”, note the first email address in the Address book, and close it → Click “Compose email”, type that address in To and press Enter → Type “[e2e-dd] live-send <number>” in Subject → Click “Preview”, then click “Send” → Check that the composer closes and a sending notification appears → Click “History” and search for the subject → Check that the new distribution is listed → Close the drawer
  - Expected: The email is sent to the contact and the distribution appears in History under its subject.

- **DD-F11: realtime — folder created by a second user appears live** — notrun (dd.spec.js:1049)
  - Where: Tools → Document Distribution, open on both accounts at the same time · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: open Document Distribution and stay on the main screen → Lava Device: open Document Distribution in another browser → Lava Device: create a folder “[e2e-dd] realtime <number>” with Create Folder → iPhone XRR: without reloading, check that the new folder appears → Lava Device: delete the folder through ⋯ (More actions) → “Delete” and confirm → iPhone XRR: without reloading, check that the folder disappears
  - Expected: A folder created by the other user appears live, and disappears live when they delete it.

- **DD-D03: drag-and-drop uploads into the open folder** — notrun (dd.spec.js:1080)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] dnd <number>” with Create Folder and opens it
  - Steps: Take a PDF named “dropped-<number>.pdf” on your computer → Drag it over the library area of the open folder → Drop it there → Check that “dropped-<number>.pdf” is listed in the folder
  - Expected: A file dropped onto the open folder uploads and is listed there.

- **DD-D12: date filter applies and Clear filters restores** — notrun (dd.spec.js:1113)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: Creates a folder “[e2e-dd] dated <number>” with Create Folder (its date defaults to today)
  - Steps: Click “Filter by date” and pick today in the calendar → Check that the date box now shows a date → Check that the new folder is still listed → Hover the date box and click its clear (×) icon → Check that the date box is empty → Check that the folder is still listed
  - Expected: Filtering by today keeps today’s folder listed, and clearing the date empties the filter with the folder still listed. The test clears with the date box’s ×, not the “Clear filters” button in the title (that button only shows when nothing matches).

- **DD-H08: save recipients as a distribution list, then remove it** — notrun (dd.spec.js:1142)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “History” in the toolbar and click the first distribution → Check that its recipients are listed → Click “Save recipients as a distribution list” in the detail header → Type “[e2e-dd] from-recipients <number>” in List name and click “Create list” → Check that the dialog closes, then close the History drawer → Click “Distribution lists” and open the new list → Click “Remove this list” and confirm with “Remove” → Check that the list is gone, then close the window
  - Expected: The record’s recipients are saved as a new distribution list, which then shows in Distribution lists and can be removed.

- **DD-H12: legacy flat-array backend degrades to client-side history** — notrun (dd.spec.js:1179)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Setup: The test makes the server answer the History list in the old format: a plain list holding one record, “Legacy mock distribution”, with no total and no batches
  - Steps: Click “History” in the toolbar → Check that “Legacy mock distribution” is listed → Type “legacy mock” in “Search by subject or recipient name / email” → Check that “Legacy mock distribution” is still listed → Close the drawer
  - Expected: History still lists records when the server sends the old format, and the search filters them without asking the server.

- **DD-A03: edit a contact and see the change persist** — notrun (dd.spec.js:1250)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Contacts” in the toolbar, then “Add contact” → Type “e2e-dd-edit-<number>@example.com” in Email and “E2E Edit Before <number>” in Name, then press Enter in the Role (designation) field to save → Search the Address book for the email and check that “E2E Edit Before <number>” is shown → Click the contact’s Edit button → Replace the name with “E2E Edit After <number>” and press Enter in the Role field to save → Check that “E2E Edit After <number>” is shown → Click the contact’s Delete button and confirm with “Delete” → Check that the contact is gone, then close the Address book
  - Expected: The edited name replaces the old one in the Address book right after saving. The test does not close and reopen the Address book to check it.

- **DD-T03: edit a template and the list reflects it** — notrun (dd.spec.js:1293)
  - Where: Tools → Document Distribution · account: iPhone XRR
  - Steps: Click “Templates” in the toolbar, then “New template” → Type “[e2e-dd] tpl-edit name <number>” as Name, “[e2e-dd] tpl-edit before <number>” as Subject and “body” as the message, then click “Create template” → Check that the template is listed with its subject → Click the template’s Edit button and check that the dialog reads “Edit template” → Replace Subject with “[e2e-dd] tpl-edit after <number>” and click “Save changes” → Check that the list shows the new subject → Click the template’s Delete button and confirm with “Delete” → Check that the template is gone, then close the drawer
  - Expected: After an edit, the Email Templates list shows the template’s new subject.

### Document Distribution — slot b with lowered rights (User Management)

- **DD-R99: slot b’s Admin switch is back where it was before the rights tests** — notrun (dd.spec.js:1863)
  - Where: Settings → Admin Settings → User Management (as iPhone XRR) · account: iPhone XRR
  - Steps: If an earlier rights test in this run stopped before putting Lava Device back, finish that now (its Document Distribution rights, then its Admin switch) → Open Settings → Admin Settings → User Management → Find Lava Device’s row → Check that its Admin switch is as it was before this run’s first change (on, in the sandbox’s usual setup)
  - Expected: After the rights tests, Lava Device’s Admin switch is back where it was before them (normally on: both accounts administer the sandbox).

## Sheet bugs (36)

- **DD-01** Unticking an attached file in "Attach from library" does not remove it from the email — Medium · Web: Ready to retest, Android: Not affected
  - Steps: 1. Open a folder with files → Distribute this folder. 2. Click Attach from library: the attached files start ticked. 3. Untick one (the button reads "Attach 19 selected") and confirm.
  - Expected: The unticked file is removed from the email.
  - Actual: All 20 files are still attached, including the one unticked, and it would be sent.

- **DD-02** Pressing Enter twice in "Create folder" creates two identical folders — Medium · Web: Ready to retest, Android: Not affected
  - Steps: 1. Click Create Folder. 2. Type a name. 3. Press Enter twice in quick succession (normal typing speed).
  - Expected: One folder.
  - Actual: Two create requests, "Folder created" twice, and two folders with the same name.

- **DD-03** "Watermark PDF and share" tells you to toggle Watermark, which turns it off — Medium · Web: Ready to retest, Android: Not affected
  - Steps: 1. Tick a PDF. 2. Actions → Watermark PDF and share. 3. Read the hint and look at the file's Watermark switch.
  - Expected: No instruction, or "Watermark is on for every PDF", since the composer already watermarks them.
  - Actual: The hint says: Toggle "Watermark" on each PDF before sending. Meanwhile the PDF already shows "Watermarked" with its Watermark switch on. Following the hint turns the watermark off.

- **DD-04** "Download with watermark" mishandles a failed stamp: raw browser error, or the error saved as the PDF — Medium · Web: Ready to retest, Android: In progress
  - Steps: 1. Open a folder with a PDF. 2. Hover the PDF row and click the shield icon (Download with watermark). 3. Click Download while the watermark service fails.
  - Expected: An error message, and no file is saved.
  - Actual: If the server answers with an error status, the toast shows a browser error ("Failed to execute 'createObjectURL' on 'URL': Overload resolution failed.") and the dialog stays open. If it answers HTTP 200 with a failure body, the app saves sample_watermarked.pdf containing the error text {"status":0,"message":"watermark_failed"} and says "Downloaded watermarked copy".

- **DD-05** "Distribute this folder" and Publish disappear while a search hides the folder's files — Low · Web: Ready to retest, Android: In progress
  - Steps: 1. Open a folder that has files: the bar shows Publish and Distribute this folder. 2. Type a search that matches none of its files.
  - Expected: The folder can still be distributed (its files are still in it), or the bar explains that a search is active.
  - Actual: Both buttons disappear and the bar says "Upload files to enable distribution", although the folder has files.

- **DD-06** A trailing space in the library search hides matching files — Low · Web: Ready to retest, Android: Not affected
  - Steps: 1. Open a folder that has a PDF. 2. Search "pdf": the PDF is listed. 3. Add a space: "pdf ".
  - Expected: The same results as "pdf".
  - Actual: "No matching files or folders. Nothing matches "pdf"." The message itself shows the trimmed word, while the request sent the untrimmed one.

- **DD-07** Saving "Edit folder" with a blank name closes the dialog with no message — Low · Web: Ready to retest, Android: In progress
  - Steps: 1. Open a folder's ⋮ menu → Edit. 2. Clear the name (or type only spaces). 3. Click Save.
  - Expected: A "name is required" message; the dialog stays open.
  - Actual: The dialog closes, nothing is saved, the folder keeps its old name, and no message appears.

- **DD-08** A History record that fails to load spins forever, with no message and no way back — Low · Web: In progress, Android: In progress
  - Steps: 1. Open History. 2. Click a distribution while its detail request fails (server error or network drop).
  - Expected: An error message with Retry, and the Back to history button.
  - Actual: The detail area shows a spinner indefinitely; no error and no Back button. Only closing the drawer gets out.

- **DD-09** Contacts "Export CSV" garbles accented names in Excel — Low · Web: In progress, Android: In progress
  - Steps: 1. Add a contact named "Zoë Müller". 2. Contacts → Export CSV. 3. Open Address Book.csv in Excel.
  - Expected: Excel shows "Zoë Müller", as it does for the History recipients export.
  - Actual: The file has no UTF-8 byte-order mark, so Excel shows "ZoÃ« MÃ¼ller".

- **DD-10** Email Preview runs scripts stored in a template's HTML — High · Web: Open, Android: In progress
  - Steps: 1. Store an email template whose body contains HTML with an event handler, e.g. <img src="x" onerror="…"> (possible through the templates API, which any member with posting rights can call). 2. Compose email → Templates → load that template. 3. Add a recipient and click Preview.
  - Expected: The preview shows the template's text; markup from the server is sanitised and never runs.
  - Actual: The template's script runs as soon as Preview opens (the onerror handler fired once in the test). It did not run while the template was only loaded into the editor, so the Preview step is what executes it.

- **DD-11** Ticked files hidden by a search are still deleted by the selection's Delete — Medium · Web: In progress, Android: In progress
  - Steps: 1. In a folder, tick two files. 2. Type a search that hides both (e.g. a word in neither name). 3. The bar still says "2 selected". Actions → Share documents opens with no attachments. 4. Actions → Delete.
  - Expected: The selection follows what is listed (cleared or narrowed when a search hides items), or the bar says that hidden items are selected.
  - Actual: "Delete 2 items?" and both hidden files are deleted. Share documents, from the same selection, attached nothing.

- **DD-12** A distribution list's "Share some documents" ignores members added but not saved — Medium · Web: In progress, Android: Not affected
  - Steps: 1. Distribution lists → New list → Create. 2. Add two members with "Add to the list" (don't press Save changes). 3. Click Share some documents.
  - Expected: The composer's To has the two members shown in the list (or the list asks to save first).
  - Actual: Share is enabled, but the composer opens with To empty: it uses the list as last saved. Edits made before Share are also not saved.

- **DD-13** "Attach 0 selected" in "Attach from library" looks active when nothing is selected — Low · Web: Not affected, Android: Not affected
  - Steps: 1. Open Document Distribution and tap Send by email on a file (or Compose). 2. Under ATTACHMENTS tap Library. 3. Don't tick any file.
  - Expected: "Attach 0 selected" is greyed out, like a disabled button.
  - Actual: The button is drawn in full orange like an active button, but tapping it does nothing.

- **DD-14** Counts are not singular: "1 ITEMS" in the library and "1 documents" in Publish — Low · Web: In progress, Android: In progress
  - Steps: 1. Open Document Distribution with one item in the list. 2. Read the section header. 3. On a single file tap Publish and read the sheet header.
  - Expected: "1 item" and "1 document".
  - Actual: The header reads "1 ITEMS" and the Publish sheet reads "Publish documents · 1 documents".

- **DD-15** Library search field has no clear (×) button — Low · Web: Not affected, Android: In progress
  - Steps: 1. Open Document Distribution. 2. Type a word into "Search files & folders…".
  - Expected: Decided by product: a clear (×) button like other search fields in the app, or none.
  - Actual: There is no clear button; the text has to be deleted by hand.

- **DD-16** Leaving a distribution list with Back or "Close this list" drops unsaved members without warning, even after "Added 3 contacts" — Medium · Web: In progress, Android: In progress
  - Steps: 1. Distribution lists → New list → Create. 2. Import a file (.csv) → Choose CSV file → Add 3 contacts (toast "Added 3 contacts"), and add one more person with "Add to the list". 3. Click Back (or Close this list) without pressing Save changes.
  - Expected: A warning that there are unsaved changes (or the list saves itself), and no "Added" message until something is saved.
  - Actual: The list closes with no warning. It still shows "0 people" and all 4 members are gone, although the toast had said "Added 3 contacts".

- **DD-17** Move shows two success messages, and still says "Moved" when the move fails — Medium · Web: In progress, Android: Not checked
  - Steps: 1. In a folder, tick a file. 2. Actions → Move… → pick another folder → Move here.
  - Expected: One confirmation. If the move fails, an error message and no success message.
  - Actual: Every move shows two success toasts, "Moved 1 item" and "Moved". When the move request fails (404 route_not_found), both success toasts still appear and the file stays where it was.

- **DD-18** Library search only looks inside the open folder, so from the main screen it can never find a file — Medium · Web: In progress, Android: In progress
  - Steps: 1. Have a file (e.g. "still.png") in a subfolder of a folder. 2. Go back to the library's main screen (root). 3. Search "still", then search the subfolder's name.
  - Expected: Matching files and folders from anywhere in the library, each with its folder; or, if search is meant to cover only the open folder, the empty state says so.
  - Actual: "No matching files or folders. Nothing matches "still". Try a different term or check your spelling." The subfolder is not found either; only top-level folder names match. Files always live inside folders, so a search at the root never returns a file.

- **DD-19** A distribution list can be created with the same name as an existing one — Low · Web: Open, Android: In progress
  - Steps: 1. Distribution lists → New list → type a name → Create. 2. New list again → type the same name → Create.
  - Expected: "A list with this name already exists" (as Contacts does for a duplicate email).
  - Actual: "List created": two lists with the same name, which can't be told apart in the composer's Distribution lists picker, the merge dialog's "Choose a list" or the watermark ZIP's list chooser. The project already has duplicates ("Yh — recipients" ×3, "asdfasdf" ×2).

- **DD-20** Loading a template replaces the whole message, including the signature, without asking — Low · Web: In progress, Android: In progress
  - Steps: 1. Compose email: your saved signature is added to the message automatically (type a line too if you like). 2. Templates → Load template → pick a template.
  - Expected: Ask before replacing what is already written, and keep the signature at the end.
  - Actual: The subject and the whole message are replaced by the template with no warning. The signature is gone, so the email goes out without it unless it is inserted again from Signature.

- **DD-21** In a narrow window the Name column shrinks to a few characters — Low · Web: In progress, Android: Not affected
  - Steps: 1. Open Document Distribution in a browser window about 700 px wide (the tool window is then about 600 px), or narrow the tool window. 2. Look at the list.
  - Expected: Names stay readable: Date and Size give way, and the row actions (shown only on hover) don't reserve space.
  - Actual: The Name column is 83 px wide, so names read "[e2e…", "345…", "ASS…". Date (140 px), Size (100 px) and an empty 196 px actions column keep their widths. At 1024 px and wider the Name column is fine (407 px).

- **DD-22** Wording slips: "1 people", "any one of the module below", "applies to this send only" on a download — Low · Web: In progress, Android: In progress
  - Steps: 1. Distribution lists: look at a list with one member. 2. Select a file → Actions → Publish: read the heading. 3. Open a PDF → Watermark: read the note under the settings.
  - Expected: "1 person"; "Publish in one of the modules below"; "applies to this download only".
  - Actual: Custom lists read "1 people" (the All contacts row does say "person"); Publish reads "PUBLISH IN ANY ONE OF THE MODULE BELOW"; the download dialog says "Any change you make here applies to this send only."

- **DD-23** Move can't be finished: "Move here" is pushed off the screen when the project has many folders — High · Web: Not checked, Android: In progress
  - Steps: 1. In a project with more folders than fit on one screen (SG Document Distribution has dozens), tick a file. 2. Actions → Move. 3. Pick a folder, then scroll to the very end of the list.
  - Expected: "Move here" stays on screen and the list scrolls above it.
  - Actual: The folder list fills the whole sheet and "Move here" never appears, even at the last row. Dragging the sheet only closes it, so nothing can be moved by touch.

- **DD-24** Cancel or Back in the email composer drops what was typed, with no warning — Medium · Web: Not checked, Android: In progress
  - Steps: 1. Compose, or Distribute this folder. 2. Type a subject, or change the attachments. 3. Tap Cancel or the phone's Back. 4. Open Compose again.
  - Expected: A "Discard this email?" prompt, or a saved draft.
  - Actual: The composer closes at once and everything is gone: Compose reopens blank.

- **DD-25** The home icon in the breadcrumb does nothing inside a folder — Medium · Web: Not checked, Android: In progress
  - Steps: 1. Open a folder, or a folder inside a folder. 2. Tap the home icon at the left of the breadcrumb.
  - Expected: Goes back to the main Document Distribution screen.
  - Actual: Nothing happens and the same folder stays open, at any depth. Only the phone's Back leaves the folder. In a nested folder the breadcrumb also shows only home and the current folder, not the parents.

- **DD-26** Email Preview's "To:" shows an invalid address that won't be emailed — Low · Web: Not checked, Android: In progress
  - Steps: 1. In To, add a valid address and "notanemail" (a comma commits each one). 2. Tap Preview. 3. Read the To line.
  - Expected: The invalid address is blocked or flagged before Preview.
  - Actual: Preview's To line lists "notanemail" with no warning, although the footer counts only the valid address.

- **DD-27** History search with no match says "No emails sent yet." — Low · Web: Not checked, Android: In progress
  - Steps: 1. Open History (157 emails in SG Document Distribution). 2. Search for a word no email contains.
  - Expected: "No matching emails", as Contacts says "No matching contacts."
  - Actual: "No emails sent yet.", which reads as if the history were empty.

- **DD-28** Composer heading says "DISTRIBUTE FOLDER" when no folder is being sent — Low · Web: Not checked, Android: In progress
  - Steps: 1. Inside a folder, tap Compose in the toolbar, or tick one file and choose Actions → Share documents. 2. Read the heading.
  - Expected: "Compose email", as History → Duplicate shows.
  - Actual: The heading reads "DISTRIBUTE FOLDER" with 0 attachments from Compose, and with only the ticked file from Share documents.

- **DD-29** "How to use this module" opens again on every folder and pushes the list down — Low · Web: Not checked, Android: In progress
  - Steps: 1. Collapse "How to use this module". 2. Open a folder.
  - Expected: It stays collapsed.
  - Actual: It is open again in the new folder, and takes about 40% of the screen, so only one or two rows show until it is collapsed again.

- **DD-30** Move and Attach lists show names only, so same-named items can't be told apart — Low · Web: Not checked, Android: In progress
  - Steps: 1. Open Actions → Move, or Attach in the composer. 2. Look for items with the same name.
  - Expected: Each row shows its folder path.
  - Actual: Move lists "frontend" three times and "SyncOnSet" twice; Attach showed five identical rows. Nothing says which folder each one is in, so the wrong one can be picked.

- **DD-31** An email template can be created with the same name as an existing one — Low · Web: Not checked, Android: In progress
  - Steps: 1. Templates → New template. 2. Name it the same as an existing template ("Test 8"). 3. Create template.
  - Expected: "A template with this name already exists".
  - Actual: The template is created with no warning, and "Test 8" then appears twice in the Templates list. A name alone is enough: subject and body can be empty.

- **DD-32** Saving a signature with a name but no text closes as if saved, and nothing is created — Low · Web: Not checked, Android: In progress
  - Steps: 1. Composer → Signature → +. 2. Type a name, leave the signature text empty. 3. Tap Save.
  - Expected: "Signature text is required", with the form left open.
  - Actual: The form closes with no message and the list still says "No signatures yet", including after reopening. With text in it the same steps save normally.

- **DD-33** "Watermark PDF and download" leaves a ticked non-PDF out of the zip without saying so — Low · Web: Not checked, Android: In progress
  - Steps: 1. Tick two PDFs and an .xlsx. 2. Actions → Watermark PDF and download. 3. Add a recipient → Watermark and download zip. 4. Open the zip.
  - Expected: The .xlsx is left off the screen, or a message says it will be skipped.
  - Actual: All three files show on the screen, but the zip holds only the two PDFs and nothing mentions the .xlsx.

- **DD-34** Screen readers get wrong labels in the library — Low · Web: Not checked, Android: In progress
  - Steps: 1. Read the library's accessibility labels (TalkBack, or a UI dump).
  - Expected: Each control is named for what it does.
  - Actual: The list-view toggle is labelled "History", every row's checkbox "Select all", and the breadcrumb's home icon "Back".

- **DD-35** Failed actions show a raw network error with the server's host name — Low · Web: Not checked, Android: In progress
  - Steps: 1. Turn the phone's Wi-Fi off. 2. Try a row's Download, Create Folder, or Watermark → Download.
  - Expected: A plain message, such as "No internet connection. Try again."
  - Actual: All three show: Unable to resolve host "documentdistributionapi-dev.zillit.com": No address… Nothing is lost: no file is saved, and Create Folder stays open with the name kept. Opening a PDF offline is fine ("Couldn't load this document").

- **DD-36** Contacts says "No contacts yet." when the list failed to load, and keeps saying it after reconnecting — Low · Web: Not checked, Android: In progress
  - Steps: 1. Turn the phone's Wi-Fi off. 2. Open Contacts. 3. Turn Wi-Fi back on and wait, staying on the screen.
  - Expected: An error with Retry, and the list loads once the phone is back online.
  - Actual: It says "No contacts yet." with Export CSV greyed out, although the project has 153 contacts. It stayed that way after reconnecting, until Contacts was closed and reopened.
