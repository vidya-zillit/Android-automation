# Web E2E — Confidential Info

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (125)

### Confidential Info feed

- **opens with the composer and the feed root @smoke** — passed (unit-feed.suite.js:156)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Check that the message box (“Type your message here...”) can be typed in → In the window layout, check that the window title reads “Confidential Info”
  - Expected: The feed opens with a message box ready for typing; in the window layout its window is titled “Confidential Info”.

- **the scoped composer resolves to exactly one element @smoke** — passed (unit-feed.suite.js:165)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Count the message boxes in the Confidential Info feed → Check that there is exactly one message box and one composer bar
  - Expected: The feed holds exactly one message box, even when other feeds stay open in the background.

- **theme toggle flips the surface theme and restores @smoke** — passed (unit-feed.suite.js:171)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Find the moon/sun theme button beside the tabs and note its label (“Switch to dark mode” or “Switch to light mode”) → Click the theme button → Check that the label has changed and the feed switched theme → Check that the app saved the new theme choice → Click the theme button again → Check that the label is back to the original one
  - Expected: The first click switches the theme and the app saves the new choice; the second click switches it back.

- **gallery (and history) FABs are present; scroll-to-bottom only when scrolled away @smoke** — passed (unit-feed.suite.js:187)
  - Where: Tools → Confidential Info, with at least 8 messages · account: iPhone XRR
  - Steps: Open Confidential Info → Check that the round Gallery button shows at the bottom right of the feed → Check that the round History (clock) button shows above it → Check that the down-arrow scroll-to-bottom button is not shown → Scroll the feed up a long way → Check that the down-arrow button appears → Click the down-arrow button → Check that the down-arrow button disappears again
  - Expected: The Gallery and History buttons are always there; the scroll-to-bottom button shows only while the feed is scrolled away from the latest message.

- **every acked row has an options button; the counter is aria-live @smoke** — passed (unit-feed.suite.js:204)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Check that every saved message has its options (chevron) button → Paste 7,200 characters into the message box → Check that the character counter appears and is set to be read out by screen readers → Clear the message box
  - Expected: Every saved message has an options button, and the character counter is announced politely to screen readers.

- **the first character is auto-capitalised @smoke** — passed (unit-feed.suite.js:251)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “hello e2e” in the empty message box → Check that the box shows “Hello e2e” → Clear the box and type “abc” one key at a time → Check that the box shows “Abc” → Clear the box and paste “  lead” (two leading spaces) → Check that the box shows “  Lead” → Clear the box without sending
  - Expected: The first letter typed into an empty message box is upper-cased, also after leading spaces; nothing is sent.

- **empty and whitespace-only messages are not posted @smoke** — passed (unit-feed.suite.js:266)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and count the messages → Press Enter in the empty message box → Type four spaces and press Enter → Click the button at the right of the message box (Send or microphone) → Wait 3 seconds → Check that nothing was sent and the feed has the same number of messages → Clear the message box
  - Expected: An empty or spaces-only message is never sent and no new message appears.

- **the composer caps at 8,000 characters and shows the counter from 7,200 @smoke** — passed (unit-feed.suite.js:311)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Paste 7,199 characters into the message box → Check that no character counter is shown → Paste 7,200 characters instead → Check that the counter reads “7,200 / 8,000” → Paste 8,100 characters instead → Check that the box keeps only 8,000 characters and the counter reads “8,000 / 8,000” → Clear the box and check that the counter disappears
  - Expected: The message box never holds more than 8,000 characters, and the counter shows only from 7,200 characters on.

- **typing @ shows no mention picker @smoke** — passed (unit-feed.suite.js:408)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “@a” in the message box → Wait about 2 seconds → Check that no list of people to mention pops up → Clear the message box
  - Expected: Typing “@” does not open a mention picker on this feed.

- **the emoji picker lists emoji and closes again @smoke** — passed (unit-feed.suite.js:438)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the smiley (emoji) button → Check that the emoji picker opens with more than 10 emoji → Click the smiley button again (or click the message box if the picker is still open) → Check that the emoji picker is closed
  - Expected: The emoji picker opens with a full set of emoji and closes again.

- **the paperclip menu lists the expected pickers and closes on an outside click @smoke** — passed (unit-feed.suite.js:518)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip next to the message box → Check that the menu lists Upload Media, Video Upload, Image Upload, Document Upload, Audio Upload and Location Upload → Click in the message box → Check that the menu closes and the paperclip is still shown
  - Expected: The paperclip menu offers all six upload choices and closes on a click outside it.

- **each hidden file input advertises the right accept list @smoke** — passed (unit-feed.suite.js:536)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip to open the upload menu → Look up which file types each file picker accepts → Click in the message box to close the menu → Check that, together, the pickers accept .pdf, .wav, .png and .mp4 files → Check that there are five file pickers (one per menu item except Location Upload)
  - Expected: Each upload choice limits the file dialog to its own file types, and there is one file picker per upload choice except Location Upload.

- **Location Upload opens the Select Location modal; Cancel posts nothing @smoke** — passed (unit-feed.suite.js:645)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and count the messages → Click the paperclip, then “Location Upload” → Check that the “Select Location” window opens → Click Cancel → Check that the window closes and no new message appears
  - Expected: Location Upload opens “Select Location”, and cancelling it posts nothing.

- **closing the document preview posts nothing @smoke** — passed (unit-feed.suite.js:661)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and count the messages → Click the paperclip, then “Document Upload” → Pick the file sample.pdf → Check that the preview shows “sample.pdf” and its file size → Click Close in the preview → Wait 3 seconds and check that no new message appears
  - Expected: Closing the upload preview posts nothing.

- **the image picker rejects a gif @smoke** — passed (unit-feed.suite.js:672)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Image Upload” → Pick a GIF image (e2e.gif) → Check that the message “No valid image files found…” pops up → Check that no upload preview opens
  - Expected: A GIF is refused by Image Upload with “No valid image files found”, and nothing can be uploaded.

- **the document picker rejects an executable @smoke** — passed (unit-feed.suite.js:679)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload” → Pick an .exe file (e2e.exe) → Check that the message “Executable files are not allowed.” pops up → Check that no upload preview opens
  - Expected: An executable file is refused by Document Upload, and nothing can be uploaded.

- **a PDF carrying JavaScript is blocked @smoke** — passed (unit-feed.suite.js:686)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload” → Pick a PDF that contains JavaScript (e2e-bad.pdf) → Check that the message “Blocked potentially malicious PDF…” pops up → Check that no upload preview opens
  - Expected: A PDF that carries JavaScript is blocked, and nothing can be uploaded.

- **the audio and media pickers reject a text file @smoke** — passed (unit-feed.suite.js:693)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Audio Upload”, and pick a text file (e2e.txt) → Check that the message “No valid audio files found…” pops up and then goes away → Click the paperclip, then “Upload Media”, and pick the same text file → Check that the message “No valid media files found…” pops up → Check that no upload preview opens
  - Expected: A text file is refused by both Audio Upload and Upload Media, and nothing can be uploaded.

- **the options menu closes on Escape and on an outside click @smoke** — passed (unit-feed.suite.js:819)
  - Where: Tools → Confidential Info, with at least one message · account: iPhone XRR
  - Steps: Open Confidential Info → Open the options menu of the latest saved message → Press Escape → Check that the menu closes → Open the same options menu again → Click in the message box → Check that the menu closes
  - Expected: The options menu closes both on Escape and on a click outside it.

- **search edge cases: no match, one character, spaces, regex characters, Escape @smoke** — passed (unit-feed.suite.js:1493)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and click the magnifying-glass Search button → Check that the drawer reads “0 Matches” → Type a nonsense word and check “No results found” shows with no results → Type the single letter “z” and check it still reads “0 Matches” without “No results found” → Type three spaces and check the message “Please write in search box” pops up → Type “[e2e] (” and then “.*”, and check that nothing breaks → Press Escape and check that the drawer stays open → Click Close in the search drawer
  - Expected: Search handles no match, a single letter, spaces and special characters without errors, and Escape does not close the drawer.

- **the Gallery drawer opens with its four tabs and closes on Escape @smoke** — passed (unit-feed.suite.js:1532)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the round Gallery button → Check that the “Gallery” drawer opens with the tabs Media (n), Links (n), Docs (n) and Audio (n) → Check that Media is the selected tab → Check that opening the Gallery made no extra library request to the server → Press Escape and check that the drawer closes
  - Expected: The Gallery opens on Media with its four counted tabs, loads nothing extra from the server, and closes on Escape.

- **the feed opens with the newest row in view @smoke** — passed (unit-feed.suite.js:1596)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Wait for the messages to finish loading → Check that the newest (last) message is in view
  - Expected: The feed opens scrolled to the newest message.

- **scrolling up loads an older page (skips under 50 rows) @smoke** — passed (unit-feed.suite.js:1603)
  - Where: Tools → Confidential Info, with at least 50 messages · account: iPhone XRR
  - Steps: Open Confidential Info and count the loaded messages → Scroll up to the top of the feed, again and again for up to a minute → Check that older messages load above the first ones (the count grows) → Check that the app fetched an older batch of messages from the server
  - Expected: Scrolling to the top loads older messages.

- **minimizing and restoring the window keeps the feed and the draft @smoke** — passed (unit-feed.suite.js:1818)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info in the window layout → Type “[e2e] window-draft <timestamp>” in the message box without sending → Click the window’s Minimize button → Check that the window is hidden → Click the window’s tab in the window tab strip to restore it → Check that the message box is shown again and still holds the draft → Clear the message box
  - Expected: Minimizing and restoring the window keeps the feed and the unsent draft.

### Confidential Info shell

- **the single unit tab reads "Confidential Info" and is selected @smoke** — passed (confidential-info.spec.js:53)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info → Wait for the message box to show → Check that the header holds exactly one tab → Check that the tab reads “Confidential Info” and is selected
  - Expected: Confidential Info shows a single tab, “Confidential Info”, already selected.

- **opens from the Tools tile @smoke** — passed (confidential-info.spec.js:61)
  - Where: Tools · account: iPhone XRR
  - Steps: Open Tools → Click the Confidential Info tile → Check that Confidential Info opens with its message box
  - Expected: Clicking the Confidential Info tile opens it with its message box ready.

- **the History FAB lands on "Confidential Info History" right after opening @smoke** — passed (confidential-info.spec.js:69)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info → Right away, click the History floating button → Check that “Confidential Info History” shows → Check that the address bar does not contain the word “undefined” (the blank page a known race leads to)
  - Expected: Opening History straight after opening Confidential Info lands on “Confidential Info History”, instead of a blank screen.

- **no confidentiality chrome: nothing blurred, text selectable, Download offered to admins @smoke** — passed (confidential-info.spec.js:84)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info → Check that the message list is not blurred → Check that message text can be selected with the mouse → Check that nothing on the feed is blurred or carries a watermark
  - Expected: Confidential Info adds no confidentiality layer: nothing is blurred, there is no watermark, and text can be selected. (Despite its title, the test does not check the Download item.)

- **the Back button exists only on the classic layout @smoke** — passed (confidential-info.spec.js:95)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info → If it opened in a workspace window, check that it has no Back button → On the classic layout (no windows), check that a Back button shows
  - Expected: Confidential Info has a Back button only on the classic layout; inside a workspace window there is none.

### Confidential Info feed

- **post text with Enter: row, own sender, cleared composer, status-1 POST** — passed (unit-feed.suite.js:216)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “[e2e] post <timestamp>” in the message box → Press Enter → Check that the server accepts the message → Check that the message appears in the feed and the message box is empty again → Check that the message gets its options button (it is saved) and shows the sender’s name
  - Expected: The message is posted once, appears in the feed under the own name, and the message box is cleared.

- **the mic slot becomes Send once there is text, and Send posts** — passed (unit-feed.suite.js:238)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Note the microphone button at the right of the message box → Type “[e2e] send-button <timestamp>” in the message box → Check that the microphone button has turned into the Send (paper-plane) button → Click Send → Check that the message appears, gets its options button, and the message box is empty again
  - Expected: Typing turns the microphone button into Send, and clicking Send posts the message.

- **Shift+Enter adds a newline; Enter keeps both lines in one bubble** — passed (unit-feed.suite.js:282)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “[e2e] shift-first <timestamp>” in the message box → Press Shift+Enter → Check that nothing was posted yet → Type “second-<timestamp>” on the new line → Press Enter → Check that one message appears holding both lines, and it gets its options button
  - Expected: Shift+Enter only adds a line break; Enter then sends both lines together as one message.

- **Ctrl+Enter sends** — passed (unit-feed.suite.js:301)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “[e2e] ctrl-enter <timestamp>” in the message box → Press Ctrl+Enter → Check that the message appears in the feed and gets its options button
  - Expected: Ctrl+Enter sends the message just like Enter.

- **a long message clips with Read More and expands back with Read Less** — passed (unit-feed.suite.js:327)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] clip <timestamp>” followed by “lorem ipsum ” 40 times and the word “TAIL-<timestamp>” at the end → Check that the message shows “Read More” and the TAIL word is hidden → Click “Read More” → Check that the TAIL word is now shown → Click “Read Less” → Check that the TAIL word is hidden again
  - Expected: A long message is cut short with “Read More”, opens fully on click, and folds back with “Read Less”.

- **HTML in a message renders as text, with no page error** — passed (unit-feed.suite.js:343)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “<img src=x onerror=alert(1)> <b>bold</b> [e2e] xss <timestamp>” → Check that the message shows “<b>bold</b>” as plain text → Check that no bold text and no broken image appear in the message → Check that no script error or alert popup occurs
  - Expected: HTML typed into a message is shown as plain text and never runs.

- **links: https becomes a safe link; www and javascript: do not** — passed (unit-feed.suite.js:355)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “https://example.com/e2e-<timestamp>” → Check that the address becomes a link that opens in a new tab, starts with https and cannot reach back to the app (no opener) → Post “see www.example.com e2e-www-<timestamp>” → Check that this message has no link → Post “javascript:alert(1) e2e-js-<timestamp>” → Check that this message has no link either
  - Expected: A full https address becomes a safe link that opens in a new tab; a bare “www.” address and a “javascript:” text stay plain text.

- **an e-mail address becomes a mailto link** — passed (unit-feed.suite.js:374)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “qa+e2e@example.com e2e-mail-<timestamp>” → Check that the e-mail address shows as a link that opens a new e-mail
  - Expected: An e-mail address in a message becomes a link to write an e-mail to it.

- **the own mention token renders as the own name** — passed (unit-feed.suite.js:382)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Look up the account’s own user ID → Post “@{{<own user ID>}} hi e2e-mention-<timestamp>” → Note the sender name on the new message → Check that the message text shows “@<sender name>” → Check that no curly braces are left in the message text
  - Expected: A mention code for the own user is shown as “@” plus the own name.

- **special characters round-trip exactly and markdown stays literal** — passed (unit-feed.suite.js:394)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “🎬 مرحبا Café ©®™ *bold* _it_ 12345 e2e-chars-<timestamp>” → Check that the message shows 🎬, مرحبا, Café, ©®™, *bold*, _it_ and 12345 exactly as typed → Check that nothing in the message is shown in bold or italics
  - Expected: Emoji, right-to-left text, accents and symbols come back unchanged, and markdown markers stay as typed.

- **an emoji from the picker is appended at the end and sent** — passed (unit-feed.suite.js:418)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “[e2e] emoji <timestamp>” in the message box → Press Home to move the cursor to the start of the text → Click the smiley (emoji) button and pick the first emoji → Check that the emoji was added at the end of the text, not at the cursor → Press Enter → Check that the message appears and gets its options button
  - Expected: The picked emoji is added at the end of the text wherever the cursor is, and the message sends with it.

- **an unsent draft survives a reopen and is cleared once sent** — passed (unit-feed.suite.js:449)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “[e2e] draft <timestamp>” in the message box without sending → Reload the page and go back to Confidential Info → Check that the message box still holds the draft → Press Enter → Check that the message appears and gets its options button → Reload the page and go back to Confidential Info again → Check that the message box is empty
  - Expected: An unsent draft is kept across a reload, and it is gone once it has been sent.

- **a posted message persists across a reopen** — passed (unit-feed.suite.js:466)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] persistent <timestamp>” and wait for its options button → Reload the page and go back to Confidential Info → Check that the message is still in the feed
  - Expected: A posted message is still there after a reload.

- **a pending row shows the clock and no options button until the ack** — passed (unit-feed.suite.js:476)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Slow the connection so that sending takes about 4 seconds (the test holds the send request back) → Type “[e2e] pending <timestamp>” and press Enter → Check that the message appears at once with a clock icon and no options button → Wait for the send to finish → Check that the options button then appears
  - Expected: While a message is still being sent it shows a clock and no options button; the options button appears once the server has saved it.

- **a fast double Enter posts the message once** — passed (unit-feed.suite.js:501)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Type “[e2e] double-enter <timestamp>” in the message box → Press Enter twice at the same moment → Wait 5 seconds → Check that exactly one message with that text is in the feed
  - Expected: A double Enter sends the message only once.

- **a PDF uploads as a document bubble with its name, size and thumbnail** — passed (unit-feed.suite.js:556)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload” → Pick the file sample.pdf → In the preview, type “[e2e] pdf <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows “sample.pdf”, a “File Size - …” line and a PDF thumbnail
  - Expected: The PDF is posted as a document message with its name, its size and a thumbnail of its first page as a preview.

- **a xlsx document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload” → Pick the file sample.xlsx → In the preview, type “[e2e] sample.xlsx <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.xlsx” with a document icon
  - Expected: The Excel (xlsx) document is posted as a document message showing its file name and a document icon.

- **a docx document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload” → Pick the file sample.docx → In the preview, type “[e2e] sample.docx <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.docx” with a document icon
  - Expected: The Word (docx) document is posted as a document message showing its file name and a document icon.

- **a csv document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload” → Pick the file sample.csv → In the preview, type “[e2e] sample.csv <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.csv” with a document icon
  - Expected: The CSV document is posted as a document message showing its file name and a document icon.

- **a captioned document shows the caption and offers Edit Message** — passed (unit-feed.suite.js:581)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] doc-caption <timestamp>” → Check that the new message shows the caption → Open the message’s options menu → Check that it offers Edit Message and Download → Click Edit Message, change the text to “<caption> edited” and press Update → Check that the message shows the new caption with an “Edited” tag
  - Expected: A document with a caption shows the caption, and the caption can be edited; the message is then marked “Edited”.

- **an image uploads with its caption** — passed (unit-feed.suite.js:604)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Image Upload” → Pick the image red.png → In the preview, type “[e2e] image <timestamp>” in the “Type a message” box and press Upload → Check that a new message shows the image, the caption and a “File Size - …” line
  - Expected: The image is posted with its caption and its file size.

- **two images picked at once land as two rows** — passed (unit-feed.suite.js:615)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Upload Media” → Pick the two images red.png and blue.png together → Press Upload in the preview (it may read “Upload 2”) → Check that two new image messages appear → Check that each gets its options button and shows its image
  - Expected: Two images picked together are posted as two separate image messages.

- **an audio file uploads as an audio bubble** — passed (unit-feed.suite.js:631)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Audio Upload” → Pick the file sample.wav → Check that the preview names “sample.wav” → Press Upload → Check that a new message appears with a Play button and gets its options button
  - Expected: The audio file is posted as an audio message that can be played.

- **dropping an image on the feed opens the preview; Close posts nothing, Upload posts** — passed (unit-feed.suite.js:705)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Drag the image red.png from the computer and drop it on the message list → Check that the upload preview opens → Click Close and check that no new message appears → Drop red.png on the message list again → Press Upload in the preview → Check that a new image message appears, gets its options button and shows the image
  - Expected: Dropping an image opens the upload preview; closing it posts nothing, and Upload posts the image.

- **Image Reply posts a new image row carrying the caption** — passed (unit-feed.suite.js:731)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload red.png through the paperclip → Image Upload with the caption “[e2e] image-reply-src <timestamp>” → Open the image message’s options menu and click Image Reply → In the preview, type “[e2e] image-reply <timestamp>” in the “Type a message” box and press Upload → Check that a new, separate image message appears with that caption and the image → Check that the new message is not nested as a comment
  - Expected: Image Reply posts a new image message of its own carrying the reply caption, not a comment.

- **an own text message offers the expected menu and nothing forbidden** — passed (unit-feed.suite.js:750)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] menu-text <timestamp>” → Open the message’s options menu → Check that it offers Reply, Edit Message, Forward In App, Delete, Read By, Forward to Remote Projects and Share (Translate, if shown, is ignored) → Check that it does not offer Copy, Distribute, Publish to Doc Distribution, Download or Image Reply → Close the menu
  - Expected: The own text message menu holds exactly the expected actions and none of the forbidden ones.

- **an own document without a caption adds Download and drops Edit Message** — passed (unit-feed.suite.js:765)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Document Upload”, pick sample.csv and press Upload without a caption → Wait for the new document message to get its options button → Open its options menu → Check that it offers Download → Check that it offers neither Edit Message nor Image Reply → Check that it offers Publish to Doc Distribution → Close the menu
  - Expected: An own document without a caption offers Download and Publish to Doc Distribution, but no Edit Message or Image Reply.

- **an own image adds Download and Image Reply** — passed (unit-feed.suite.js:787)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload red.png through the paperclip → Image Upload with the caption “[e2e] menu-image <timestamp>” → Open the image message’s options menu → Check that it offers Download, Edit Message and Image Reply → Close the menu
  - Expected: An own captioned image offers Download, Edit Message and Image Reply.

- **an own audio adds Download and offers no Edit Message** — passed (unit-feed.suite.js:802)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the paperclip, then “Audio Upload”, pick sample.wav and press Upload → Wait for the new audio message to get its options button → Open its options menu → Check that it offers Download and no Edit Message → Close the menu
  - Expected: An own audio message offers Download and cannot be edited.

- **selection mode hides the options buttons and FABs and counts the selection** — passed (unit-feed.suite.js:833)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] selection <timestamp>” → Open its options menu and click Delete → Check that the bar reads “1 message(s) selected” and shows a Delete button → Check that no options buttons, no Gallery button and no message box are shown → Click the selected message → Check that the bar reads “0 message(s) selected” and the Delete button is gone → Click the message again and check that the bar reads “1 message(s) selected” → Leave selection mode with the bar’s X button → Check that the message box and the Gallery button are back and the message is still there
  - Expected: Selection mode hides the options buttons, the round buttons and the message box, counts the selected messages, and leaving it restores everything without deleting anything.

- **edit an own message: prefilled modal, Update, Edited tag, row moves last** — passed (unit-feed.suite.js:859)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] edit-original <timestamp>”, then “[e2e] edit-sibling <timestamp>” → Open the first message’s options menu and click Edit Message → Check that the “Edit Message” window holds the original text → Replace it with “[e2e] edit-updated <timestamp>” and press Update → Check that the message shows the new text with an “Edited” tag → Note whether the edited message moved to the bottom of the feed
  - Expected: The edit window opens with the current text, and after Update the message shows the new text marked “Edited”. The test only records whether the edited message moved to the bottom; it does not fail on it.

- **edit Cancel and Escape leave the message unchanged** — passed (unit-feed.suite.js:884)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] edit-keep <timestamp>” → Open its options menu, click Edit Message and type “discarded-<timestamp>” → Click Cancel → Check that the window closes and the message still shows the original text → Open Edit Message again and type “discarded-<timestamp>” → Press Escape → Check that the window closes and no message shows the discarded text
  - Expected: Leaving the edit window with Cancel or Escape keeps the message as it was.

- **unchanged text sends no PUT, empty text keeps the modal open, the box caps at 8,000** — passed (unit-feed.suite.js:903)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] edit-noop <timestamp>” → Open its options menu, click Edit Message and press Update without changing anything → Check that the window closes, nothing is sent to the server and no “Edited” tag appears → Open Edit Message again, replace the text with spaces and press Update → Check that the window stays open → Paste 8,100 characters into the edit box → Check that the box keeps only 8,000 characters → Click Cancel and check that no edit was sent at any point
  - Expected: Saving unchanged text sends no edit, an empty edit is refused with the window left open, and the edit box holds at most 8,000 characters.

- **a failing edit keeps the text and a refused delete keeps the row** — passed (unit-feed.suite.js:932)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] edit-fail <timestamp>” → Make the server fail every edit with a server error (the test fakes this) → Open Edit Message, change the text to “<original> nope” and press Update → Wait 3 seconds; if the edit window is still open, click Cancel → Check that the message still shows the original text and the “nope” text appears nowhere → Make the server refuse the delete with an error message (the test fakes this) → Open the options menu, click Delete, click Delete in the bar and answer Yes → Wait 3 seconds and check that the message is still in the feed → Leave selection mode if it is still on
  - Expected: When the edit fails the message keeps its original text, and when the server refuses the delete the message stays in the feed.

- **delete an own message through the selection flow (PUT delete/chats)** — passed (unit-feed.suite.js:977)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] delete-me <timestamp>” → Open its options menu and click Delete → Check that the bar reads “1 message(s) selected” → Click Delete in the bar → Check that the “Confirm Deletion” dialog asks “Are you sure you want to delete these messages…” → Click Yes → Check that the message disappears and exactly one delete was sent to the server
  - Expected: The message is removed after the selection bar and the “Confirm Deletion” dialog, with a single delete request.

- **delete can be backed out at both steps** — passed (unit-feed.suite.js:1000)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] delete-keep <timestamp>” → Open its options menu and click Delete → Leave selection mode with the bar’s X button → Check that the message is still there → Open its options menu, click Delete, then click Delete in the bar → Click No in the confirm dialog → Check that the dialog closes and the message is still there → Leave selection mode if it is still on
  - Expected: Backing out of a delete, either at the selection bar or at the confirm dialog, keeps the message.

- **multi-select delete removes two messages at once** — passed (unit-feed.suite.js:1016)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] multi-a <timestamp>” and “[e2e] multi-b <timestamp>” → Open the first message’s options menu and click Delete → Check that the bar reads “1 message(s) selected” → Click the second message → Check that the bar reads “2 message(s) selected” → Click Delete in the bar and answer Yes → Check that both messages disappear
  - Expected: Two selected messages are deleted together in one go.

- **a deleted message stays gone after a reopen** — passed (unit-feed.suite.js:1033)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] delete-reopen <timestamp>” → Delete it: options menu → Delete → Delete in the bar → Yes → Reload the page and go back to Confidential Info → Check that the deleted message does not come back
  - Expected: A deleted message is still gone after a reload.

- **a reply nests as a comment inside the parent, which moves to the bottom** — passed (unit-feed.suite.js:1046)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] reply-target <timestamp>”, then “[e2e] reply-sibling <timestamp>” → Open the first message’s options menu and click Reply → Check that a quote of the message shows above the message box → Type “reply-<timestamp>” and press Enter → Check that the reply shows as a comment inside the first message, with a sender name and its own options button → Check that exactly one comment was sent and the reply text appears in only one message → Note whether the replied-to message moved to the bottom of the feed
  - Expected: A reply is shown once, as a comment nested inside the message it answers. The test only records whether that message moved to the bottom; it does not fail on it.

- **an own comment renders once — the socket echo must not duplicate it** — passed (unit-feed.suite.js:1071)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] comment-once <timestamp>” → Open its options menu, click Reply, type “once-<timestamp>” and press Enter → Check that the comment appears inside the message → Wait 5 seconds for the live update to arrive → Check that the comment is shown exactly once
  - Expected: The own comment is shown once and is not doubled when the live update for it arrives.

- **the reply quote names the sender and can be dismissed** — passed (unit-feed.suite.js:1086)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] quote <timestamp>” → Note the sender name shown on the message → Open its options menu and click Reply → Check that the quote above the message box shows the sender name and the message text → Click the quote’s X button → Check that the quote disappears and the message box is empty
  - Expected: The reply quote names the sender and repeats the message, and its X closes it without leaving text behind.

- **the paperclip is hidden while replying** — passed (unit-feed.suite.js:1102)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] reply-clip <timestamp>” → Check that the paperclip is shown → Open the message’s options menu and click Reply → Check that the reply quote shows and the paperclip is gone → Close the quote with its X button → Check that the paperclip is back
  - Expected: Attachments cannot be added while replying: the paperclip hides during a reply and returns when the reply is dismissed.

- **a whitespace-only reply sends nothing and keeps the quote** — passed (unit-feed.suite.js:1114)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] reply-blank <timestamp>” → Open its options menu and click Reply → Type three spaces and press Enter → Wait about 2 seconds → Check that no comment was sent and the reply quote is still shown → Close the quote with its X button
  - Expected: A reply made only of spaces is not sent, and the reply stays open.

- **edit an own comment** — passed (unit-feed.suite.js:1129)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] comment-edit <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the new comment’s options menu → Check that it offers Edit Message, Delete and Read By → Click Edit Message, change the text to “comment-<timestamp> edited” and press Update → Check that the comment shows the new text with an “Edited” tag
  - Expected: The own comment’s menu offers Edit Message, Delete and Read By, and an edited comment shows its new text marked “Edited”.

- **delete an own comment: No keeps it, Yes removes it** — passed (unit-feed.suite.js:1153)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] comment-delete <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the comment’s options menu and click Delete → Check that the “Delete Comment” dialog opens and click No → Check that the comment is still there → Open the comment’s options menu again, click Delete and answer Yes → Check that the comment disappears from the message
  - Expected: Answering No keeps the comment; answering Yes removes it.

- **a comment offers Read By** — passed (unit-feed.suite.js:1174)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] comment-readby <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the comment’s options menu and click Read By → Check that the “READ BY USERS” window opens → Close it with its X button
  - Expected: A comment’s Read By opens the “READ BY USERS” window, which closes again.

- **a failing comment POST shows an error toast** — passed (unit-feed.suite.js:1189)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] comment-fail <timestamp>” → Make the server refuse every new comment with an error message (the test fakes this) → Open the message’s options menu and click Reply → Type “comment-fail-<timestamp>” and press Enter → Check that an error message pops up → Check that no comment is added to the message → Close the reply quote if it is still shown
  - Expected: When the server refuses a comment, an error message pops up and no comment is added.

- **the Forward modal lists the destinations and its pickers send nothing** — passed (unit-feed.suite.js:1215)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info, post “[e2e] forward-look <timestamp>”, open its options menu and click Forward In App → Check that “Select Tab To Forward” lists Home Tab, Info Tab, Confidential Info Tab, Chat User, Group Chat and Email → Click Chat User and check that “Select Users” lists people → Search for a name that does not exist, check the list is empty, then clear the search and check the people come back → Click Select All and check the selected count equals the number of people listed → Click De-Select All and close the people picker with its X → Click Group Chat (reopen Forward In App first if the Forward window closed), search for a group that does not exist and check “No Data found” shows with no Send Group button → Close the “Select Group” window and the Forward window with their X buttons
  - Expected: The Forward window lists every destination, and its people and group pickers search, select and close without sending anything.

- **the Home Tab submenu lists Home units without Calendar or Call Sheet** — skipped (unit-feed.suite.js:1262)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info, post “[e2e] forward-home-tab <timestamp>”, open its options menu and click Forward In App → Hover over “Home Tab” in the Forward window (click it if the submenu does not open) → Check that the submenu lists Bulletin → Check that it lists neither Calendar nor Call Sheet → Press Escape and close the Forward window
  - Expected: Forwarding to the Home tab offers the Home units such as Bulletin, but never Calendar or Call Sheet.

- **forwarding into this feed creates a copy (both deleted)** — passed (unit-feed.suite.js:1290)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] forward-self <timestamp>” → Open its options menu and click Forward In App → Click “Confidential Info Tab” in the Forward window → Check that “Forward Successfully” pops up → Check that the feed now shows two messages with that text, both saved
  - Expected: Forwarding a message into its own feed adds a second copy of it there.

- **Forward to Remote Projects opens its modal and closes** — passed (unit-feed.suite.js:1316)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] forward-remote <timestamp>” → Open its options menu and click Forward to Remote Projects → Check that the “Forward to Remote Projects” window opens → Close it with its X button → Check that the window closes
  - Expected: The “Forward to Remote Projects” window opens and closes without forwarding anything.

- **Share opens Compose Email with every selected message; Cancel sends nothing** — passed (unit-feed.suite.js:1329)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] share-a <timestamp>” and “[e2e] share-b <timestamp>” → Open the first message’s options menu and click Share → Check that the bar reads “1 message(s) selected” → Click the second message and check that the bar reads “2 message(s) selected” → Click Share in the bar → Check that “Compose Email” opens holding the text of both messages → Click Cancel and check that the window closes → Leave selection mode if it is still on
  - Expected: Share puts every selected message into “Compose Email”, and Cancel closes it without sending.

- **Read By lists the two segments, searches and shows the empty state** — passed (unit-feed.suite.js:1357)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info, post “[e2e] readby <timestamp>”, open its options menu and click Read By → Check that the “READ BY USERS” window shows the READ USER and UN-READ USER switches → Click UN-READ USER and check that people are listed → Check that there is no notify button beside the switch → Search for a name that does not exist and check “No Users Found.” shows → Clear the search and check that the people come back → Close the window with its X button
  - Expected: Read By shows who has and has not read the message, its search works and shows “No Users Found.” for no match, and there is no notify button.

- **Publish to Doc Distribution shows on a document, not on text; Cancel creates nothing** — passed (unit-feed.suite.js:1382)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] publish-dd <timestamp>” → Open the document message’s options menu and click Publish to Doc Distribution → Check that the “Publish to Document Distribution” dialog opens → Click Cancel and check that the dialog closes
  - Expected: A document message offers Publish to Doc Distribution, and cancelling its dialog publishes nothing.

- **Download on a PDF emits a download event** — passed (unit-feed.suite.js:1399)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] download-pdf <timestamp>” → Open the document message’s options menu → Click Download → Check that the browser downloads a file whose name ends in .pdf
  - Expected: Download on a PDF message saves a .pdf file. (The test switches off the browser’s “Save as” picker so the file downloads directly.)

- **Download on an image emits a download event** — passed (unit-feed.suite.js:1417)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload red.png through the paperclip → Image Upload with the caption “[e2e] download-image <timestamp>” → Open the image message’s options menu → Click Download → Check that the browser downloads an image file (.png, .jpg, .jpeg or .webp)
  - Expected: Download on an image message saves an image file. (The test switches off the browser’s “Save as” picker so the file downloads directly.)

- **a PDF bubble opens the viewer without a Download button; a spreadsheet does not** — passed (unit-feed.suite.js:1435)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload sample.pdf through Document Upload with the caption “[e2e] view-pdf <timestamp>” → Click the PDF in the new message → Check that the document viewer opens with a Close button and no Download button → Click Close and check that the viewer closes → Upload sample.xlsx through Document Upload with the caption “[e2e] view-xlsx <timestamp>” → Click the spreadsheet in the new message and wait a moment → Check that no document viewer opens
  - Expected: Clicking a PDF message opens the document viewer without a Download button; clicking a spreadsheet message opens nothing.

- **an image bubble opens the preview, Escape closes it** — passed (unit-feed.suite.js:1456)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload red.png through Image Upload with the caption “[e2e] view-image <timestamp>” → Click the image in the new message → Check that the full-size image preview opens → Press Escape → Check that the preview closes
  - Expected: Clicking an image opens its preview, and Escape closes it.

- **search finds a message, highlights it and scrolls to it on click** — passed (unit-feed.suite.js:1471)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Post “[e2e] searchable <timestamp>”, then two more messages “[e2e] search-pad-0 …” and “[e2e] search-pad-1 …” → Click the magnifying-glass Search button next to the message box → Check that the search box has the cursor → Type the full “[e2e] searchable <timestamp>” text → Check that the drawer reads “1 Match” and the result shows the text with the match highlighted → Click the result → Check that the feed scrolls the message into view and the search drawer stays open → Click Close in the search drawer
  - Expected: Search finds the message, highlights the match, and clicking the result scrolls the feed to it while the drawer stays open.

- **search by the own sender name lists own rows** — passed (unit-feed.suite.js:1520)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] search-sender <timestamp>” → Note the sender name on the new message → Click the magnifying-glass Search button and type that sender name → Check that the first result shows that name as its sender → Click Close in the search drawer
  - Expected: Searching for the own name lists the own messages.

- **the Gallery Docs tab lists an uploaded document, filters, and offers a card menu** — passed (unit-feed.suite.js:1551)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Upload sample.pdf through Document Upload with the caption “[e2e] gallery-doc <timestamp>” → Click the round Gallery button, then the Docs tab → Check that sample.pdf is listed → Type a nonsense word in the Gallery search and check “No matching documents” shows → Clear the search and check that sample.pdf is listed again → Click the small down arrow on the sample.pdf card → Check that its menu offers Forward In App → Press Escape twice and check that the Gallery closes
  - Expected: The Docs tab lists the uploaded document, its search filters the list, and each card has a menu with Forward In App.

- **a posted row shows avatar, sender · designation and a formatted timestamp** — passed (unit-feed.suite.js:1583)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] sender-line <timestamp>” → Check that the new message shows an avatar and the sender’s name → If a designation is shown, check that a “·” separates it from the name → Check that the time reads like “Nov 16, 2026 at 10:05 AM”
  - Expected: A posted message shows the avatar, “Name · Designation” and a timestamp in the form “Mon DD, YYYY at hh:mm AM/PM”.

- **the scroll-to-bottom button returns to the latest message** — passed (unit-feed.suite.js:1623)
  - Where: Tools → Confidential Info, with at least 8 messages · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] bottom-anchor <timestamp>” → Scroll the feed up a long way → Check that the down-arrow scroll-to-bottom button appears → Click the down-arrow button → Check that the posted message is back in view and the button disappears
  - Expected: The scroll-to-bottom button brings the latest message back into view and then hides.

- **another member's message offers no Edit Message** — passed (unit-feed.suite.js:1638)
  - Where: Tools → Confidential Info, with a message from another member · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] whoami <timestamp>” to see the own sender name → Find a saved message from another member, scrolling up if needed → Open that message’s options menu → Check that it does not offer Edit Message → Close the menu
  - Expected: A message sent by someone else cannot be edited.

- **History opens with its heading, alert and search, and comes back** — passed (unit-feed.suite.js:1661)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Click the round History (clock) button → Check that the “Confidential Info History” title shows with the note “Records of deleted messages.” → Check that a Search button shows and there is no message box and no History button → Click Back (or the browser’s back button if there is no Back) → Check that the feed with its message box is back
  - Expected: History opens as a read-only list of deleted messages titled “Confidential Info History”, and Back returns to the feed.

- **a deleted message shows in History; text rows have no menu, PDFs offer View / Download / Print** — failed (unit-feed.suite.js:1681)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info and post “[e2e] history-text <timestamp>” → Upload sample.pdf through Document Upload with the caption “[e2e] history-pdf <timestamp>” → Delete both messages through options menu → Delete → Delete in the bar → Yes → Click the round History (clock) button and wait for “Confidential Info History” → Check that the deleted text message is listed without an options button → Check that the deleted PDF message is listed → Open the PDF message’s options menu → Check that it offers View, Download and Print → Close the menu
  - Expected: Deleted messages appear in History; a text message has no menu there, and a PDF offers View, Download and Print.
  - Web error: Test timeout of 270000ms exceeded.

- **a message sent while offline errors and can be resent** — passed (unit-feed.suite.js:1709)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Switch the browser offline → Type “[e2e] offline-resend <timestamp>” and press Enter → Check that the message appears with a red retry icon → Switch the browser back online → Click the red retry icon, then “Resend Message” → Check that the red icon goes away and the message gets its options button
  - Expected: A message sent offline is marked as failed, and Resend Message posts it once the connection is back.

- **a POST answered with status 0 marks the row as failed** — passed (unit-feed.suite.js:1726)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Make the server refuse new messages with an error message (the test fakes this) → Type “[e2e] status-zero <timestamp>” and press Enter → Check that the message appears with a red retry icon → Let the server accept messages again → Click the red retry icon, then “Resend Message” → Check that the message gets its options button
  - Expected: A message the server refuses is marked as failed, and Resend Message posts it once the server accepts again.

- **a failed message can be deleted from the retry menu without a server call** — passed (unit-feed.suite.js:1750)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Make every send fail with a network error (the test fakes this) → Type “[e2e] error-delete <timestamp>” and press Enter → Check that the message shows a red retry icon → Click the red retry icon, then Delete → Check that the selection bar shows, click Delete in it and answer Yes → Check that the failed message disappears → Check that no delete was sent to the server
  - Expected: A failed message can be removed from its retry menu, and the removal stays on this device without a server call.

- **a failing chat page GET leaves the composer usable and nothing crashes** — passed (unit-feed.suite.js:1783)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Make the server fail every request for the feed’s messages with a server error (the test fakes this) → Open Confidential Info → Check that the “Something went wrong.” crash screen does not show → Check that the message box can still be typed in → Check that no loading indicator stays on screen
  - Expected: When the messages cannot be loaded the feed does not crash, stops loading, and the message box stays usable.

- **a message posted from a second context appears live** — passed (unit-feed.suite.js:1802)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info → Open Confidential Info in a second, separate browser session signed in to the same account → In the second session, post “[e2e] realtime <timestamp>” and wait for its options button → Check that the message appears in the first session without a reload → Close the second session
  - Expected: A message posted from another session shows up live in the open feed.

- **the Edit modal renders inside the tool window body** — passed (unit-feed.suite.js:1838)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Confidential Info in the window layout → Post “[e2e] portal <timestamp>” → Open its options menu and click Edit Message → Check that the “Edit Message” window opens inside the Confidential Info window, not over the whole screen → Click Cancel
  - Expected: The Edit Message window opens inside the feed’s own window.

### Confidential Info shell

- **revoking posting ejects the viewer even with view access** — skipped (confidential-info.spec.js:103)

- **demoting the admin ejects the viewer (and possibly everyone — no user_id check)** — skipped (confidential-info.spec.js:104)

- **the History deep link without view access must return zero messages** — skipped (confidential-info.spec.js:105)

- **a view-only member sees the REQUEST bar instead of the composer** — skipped (confidential-info.spec.js:106)

- **an account without view access gets no tile and bounces on the deep link** — skipped (confidential-info.spec.js:107)

### Sheet bugs — Confidential Info

- **Forward In App from Confidential Info opens "Home Tab" onto its units (Bulletin) @smoke** — bug · known bug ForwardMsgModal builds Home Tab's children from homeUnitDetailsForForward, filled only by Home; seen on dev 14 Nov: "Home Tab" is a plain item with no units,ForwardMsgModal builds Home Tab's children from homeUnitDetailsForForward, filled only by Home; seen on dev 14 Nov: "Home Tab" is a plain item with no units (sheet-bugs/confidential-info.spec.js:50)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info and wait for the feed → Open the newest message’s options menu and click Forward In App → Check that “Home Tab” is listed → Hover or click Home Tab and check that it opens a list of Home units → Check that the list includes Bulletin → Close the Forward dialog without forwarding
  - Expected: From Confidential Info, Forward In App lets you open Home Tab and pick Bulletin. Known bug: “Home Tab” is a plain item with no units under it.
  - Web error: Error: "Home Tab" has no unit submenu

- **an upper-case .EXE is refused like .exe @smoke** — bug · known bug handleDocumentFiles compares the extension to "exe" without lower-casing it (same code as BUL-03); seen on dev 14 Nov: TEST.EXE and Test.Exe open the upload preview,handleDocumentFiles compares the extension to "exe" without lower-casing it (same code as BUL-03); seen on dev 14 Nov: TEST.EXE and Test.Exe open the upload preview (sheet-bugs/confidential-info.spec.js:60)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info → Click the paperclip and choose Document Upload → Pick a file named “TEST.EXE” → Check that “Executable files are not allowed” appears and no upload preview opens (if a preview opens, close it without uploading) → Repeat with a file named “Test.Exe”
  - Expected: Upper- and mixed-case .EXE files are refused just like .exe. Known bug: TEST.EXE and Test.Exe open the upload preview instead of being refused.
  - Web error: Error: opened the upload preview instead of being refused: TEST.EXE, Test.Exe

- **a 2,100-character message posts (web allows 8,000)** — passed (sheet-bugs/confidential-info.spec.js:68)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Setup: Posts a 2,100-character message that starts with “[e2e] 2100-chars <time>”.
  - Steps: Open Tools → Confidential Info and wait for the feed → Type a 2,100-character message: “[e2e] 2100-chars <time>” followed by x’s → Press Enter → Check that the message is sent (its message options button appears) → Check that no length-limit message shows and the message has no red Retry icon
  - Expected: A 2,100-character message posts normally; web allows up to 8,000 characters.

- **Gallery Docs shows the file name and a size with a unit** — passed (sheet-bugs/confidential-info.spec.js:74)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Setup: Uploads “sample.pdf” with the caption “[e2e] gallery-size <time>”.
  - Steps: Open Tools → Confidential Info and wait for the feed → Click the paperclip → Document Upload, pick “sample.pdf”, type the caption “[e2e] gallery-size <time>” and click Upload → Click the Gallery floating button and open the Docs tab → Find the card with that caption → Check that the card shows a size with a unit (B, KB, MB or GB) → Check that the card is titled “sample.pdf” → Press Escape
  - Expected: The Gallery’s Docs card shows the real file name and a size with a unit.

- **a non-admin with download rights can download a file (stub)** — passed (sheet-bugs/confidential-info.spec.js:81)
  - Where: Tools → Confidential Info, plus a second window made to look like a non-admin member · account: iPhone XRR
  - Setup: Uploads “sample.pdf” with the caption “[e2e] download-right <time>”.
  - Steps: Open Tools → Confidential Info and upload “sample.pdf” through the paperclip → Document Upload, with the caption “[e2e] download-right <time>” → Open a second browser window of the same account in which faked server replies make it a non-admin with Confidential Info view and download rights but no posting → In that window, open Tools → Confidential Info and check that the REQUEST bar shows instead of the message box → Open the PDF message’s options menu → Check that Download is offered and click it → Check that a .pdf file downloads and no “do not have download…” message appears
  - Expected: A non-admin with download rights can download a file from the feed.

- **search runs again when the word changes, including a word with no match @smoke** — passed (sheet-bugs/confidential-info.spec.js:89)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info and wait for the feed → Note the sender name on the newest message → Click the search button next to the message box and type that name → Check that the count shows one or more Matches → Change the word to one that matches nothing (“zzq-no-match-<time>”) → Check that “No results found” and “0 Matches” show and the result list is empty → Change the word back to the name and check that there are matches again → Close search
  - Expected: Search runs again every time the word changes, including for a word with no match.

- **closing search, or coming back from the Gallery, leaves the cards looking as before @smoke** — passed (sheet-bugs/confidential-info.spec.js:94)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Steps: Open Tools → Confidential Info and wait for the feed → Note how the newest message looks (sender name weight and size, text size) → Click the search button, type that message’s sender name and click the first result → Close search and wait 2 seconds → Check that the newest message looks exactly as before → Check that no highlighted words and no highlight colour remain in the feed → Click the Gallery floating button, then press Escape → Check that the newest message still looks exactly as before
  - Expected: Closing search, and coming back from the Gallery, leave the messages looking exactly as before, with no search highlight left.

- **Confidential Info History whose request fails is not blank and silent (stub) @smoke** — bug · known bug NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: with its request failing (net::ERR_INTERNET_DISCONNECTED) Confidential Info History shows its heading and "Records of deleted messages." over an empty list, no error and no retry,NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: with its request failing (net::ERR_INTERNET_DISCONNECTED) Confidential Info History shows its heading and "Records of deleted messages." over an empty list, no error and no retry (sheet-bugs/confidential-info.spec.js:102)
  - Where: Tools → Confidential Info · account: iPhone XRR
  - Setup: The test makes the Confidential Info History request fail as if the internet connection had dropped.
  - Steps: Open Tools → Confidential Info and wait for the feed → Click the History floating button → Check that “Confidential Info History” shows → Wait 8 seconds → Check that History shows either messages or an explanation (an error message, or a Try Again / Retry button)
  - Expected: When Confidential Info History cannot load, it says so or offers a retry instead of staying blank. Known bug: it shows its heading and “Records of deleted messages.” over an empty list, with no error and no retry.
  - Web error: Error: History is blank with no message after its request failed

- **without view access Confidential Info closes back to Tools, not a "No Unit found" page (stub) @smoke** — passed (sheet-bugs/confidential-info.spec.js:113)
  - Where: Confidential Info, opened by its direct link · account: iPhone XRR
  - Setup: The test makes the project’s tool list say the account has neither view nor posting access to Confidential Info.
  - Steps: Open Confidential Info by its direct link (entering SG Document Distribution first if needed) → Wait 10 seconds → Check that Confidential Info’s message box and REQUEST bar are not shown → Check that no “No Unit found” page and no “…access to any unit yet” message appear
  - Expected: Without view access Confidential Info does not open, and the app does not fall back to Home’s “No Unit found” page (it returns to Tools).

- **a view-only member sees a post's comments, read-only (stub)** — passed (sheet-bugs/confidential-info.spec.js:120)
  - Where: Tools → Confidential Info, plus a second window made to look like a view-only member · account: iPhone XRR
  - Setup: Posts “[e2e] view-only-comments <time>” and replies to it with “e2e-comment-<time>”.
  - Steps: Open Tools → Confidential Info, post “[e2e] view-only-comments <time>” and reply to it with “e2e-comment-<time>” → Open a second browser window of the same account in which faked server replies make it a non-admin with Confidential Info view rights only → In that window, open Tools → Confidential Info and check that the REQUEST bar shows instead of the message box → Check that the post is visible → Check that the reply shows under the post
  - Expected: A view-only member sees the post together with its comments, read-only.

- **another member's comment offers no Edit (account B comments)** — passed (sheet-bugs/confidential-info.spec.js:127)
  - Where: Tools → Confidential Info, in two browsers · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts “[e2e] others-comment <time>”; Lava Device replies to it.
  - Steps: iPhone XRR: open Tools → Confidential Info and post “[e2e] others-comment <time>” → Lava Device: open Tools → Confidential Info and reply to that post with “e2e-b-comment-<time>” → iPhone XRR: wait for the reply to show (reopen Confidential Info if it has not arrived within 30 seconds) → iPhone XRR: open the reply’s options menu → Check that it offers Read By and Delete, but no Edit → Close the menu
  - Expected: On another member’s comment an admin is offered Read By and Delete, never Edit.

- **a member's first post after being given posting appears live on the admin's open screen (rights, account B)** — failed (sheet-bugs/confidential-info.spec.js:135)
  - Where: Tools → Confidential Info (both accounts), with Settings → Admin Settings → User Management in a second tab for iPhone XRR · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: in Settings → Admin Settings → User Management, switch off Lava Device’s Admin, then open Posting Rights → Tools → Confidential Info and set View on and Post off (in a second tab) → iPhone XRR: open Tools → Confidential Info and leave it open → Lava Device: open Tools → Confidential Info and check that the REQUEST bar shows → iPhone XRR: turn Lava Device’s Confidential Info Post on → Lava Device: wait for the message box (reopen Confidential Info if it has not come within 30 seconds) → Lava Device: post “[e2e] first-post-after-grant <time>” → iPhone XRR: without reloading, check that the post appears within 30 seconds
  - Expected: A member’s first post after being given posting rights appears live on the admin’s open Confidential Info screen.
  - Web error: Test timeout of 480000ms exceeded.

- **after a posting-right change, Reply quotes the post that was picked (rights, account B)** — failed (sheet-bugs/confidential-info.spec.js:171)
  - Where: Tools → Confidential Info (both accounts), with Settings → Admin Settings → User Management in a second tab for iPhone XRR · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts “[e2e] quote-target <time>”, then “[e2e] quote-other <time>”, in Confidential Info.
  - Steps: iPhone XRR: open Tools → Confidential Info and post “[e2e] quote-target <time>”, then “[e2e] quote-other <time>” → iPhone XRR: in Settings → Admin Settings → User Management, switch off Lava Device’s Admin, then open Posting Rights → Tools → Confidential Info and set View and Post on (in a second tab) → Lava Device: open Tools → Confidential Info and check that “quote-target” shows → iPhone XRR: turn Lava Device’s Confidential Info Post off, then on again → Lava Device: open Confidential Info again (the revoke closed it) and wait for the message box → Lava Device: on “quote-target”, open the options menu and click Reply → Check that the reply quote shows “quote-target” and not “quote-other” → Close the reply quote, so nothing is sent
  - Expected: After a posting-right change, Reply quotes the message that was picked, not another one.
  - Web error: Test timeout of 480000ms exceeded.

- **revoking View while Confidential Info is open takes the confidential posts off the screen (rights, account B)** — failed (sheet-bugs/confidential-info.spec.js:217)
  - Where: Settings → Admin Settings → User Management (iPhone XRR) and Tools → Confidential Info (Lava Device) · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: in Settings → Admin Settings → User Management, switch off Lava Device’s Admin, then open Posting Rights → Tools → Confidential Info and set View and Post on → Lava Device: open Tools → Confidential Info and wait for the feed → iPhone XRR: turn Lava Device’s Confidential Info View off → Lava Device: check that Confidential Info closes within 45 seconds (no message box, no REQUEST bar) → Lava Device: check that no confidential messages remain on screen
  - Expected: Revoking View while Confidential Info is open closes it and takes the confidential posts off the member’s screen.
  - Web error: Error: no User Management row named "Lava  device"

### Confidential Info shell

- **a view-only member sees the REQUEST bar instead of the composer (rights, account B)** — passed (confidential-info.spec.js:144)
  - Where: Tools → Confidential Info, in two browsers · account: iPhone XRR and Lava Device
  - Setup: In Settings → Admin Settings → User Management, iPhone XRR makes Lava Device a non-admin with View but no Post on Confidential Info.
  - Steps: Lava Device: open Tools → Confidential Info → Check that a REQUEST bar shows instead of the message box → Check that there is no message box and no paperclip → Check that the note says the member has no Posting Rights
  - Expected: A view-only member reads Confidential Info but cannot post: the REQUEST bar replaces the message box.

- **revoking posting closes Confidential Info for a member who keeps view access (rights, account B)** — suspected · known bug ConfidentialInfoMain navigates away on a confidential_info posting-rights update with enabled false; on dev the revoke reaches B (REQUEST bar) but Confidential Info stays open,ConfidentialInfoMain navigates away on a confidential_info posting-rights update with enabled false; on dev the revoke reaches B (REQUEST bar) but Confidential Info stays open (confidential-info.spec.js:161)
  - Where: Tools → Confidential Info, in two browsers · account: iPhone XRR and Lava Device
  - Setup: In Settings → Admin Settings → User Management, iPhone XRR makes Lava Device a non-admin with View and Post on Confidential Info.
  - Steps: Lava Device: open Tools → Confidential Info and check that the message box shows → iPhone XRR: in User Management, turn Post off for Lava Device on Confidential Info (View stays on) → Lava Device: check that Confidential Info closes by itself within 45 seconds → If it stays open, note whether it now shows the REQUEST bar or still the message box
  - Expected: Losing the posting right closes Confidential Info for the member at once, even though they can still view it. Known bug: the change reaches Lava Device (the REQUEST bar replaces the message box), but Confidential Info stays open.
  - Web error: Error: Confidential Info stayed open after posting was revoked

- **demoting an admin closes Confidential Info for them, and not for the admin who demoted them (rights, account B)** — suspected · known bug ConfidentialInfoMain leaves on any project_user_admin_access event with admin_access false and never checks user_id: demoting one member closed Confidential Info for the admin too,ConfidentialInfoMain leaves on any project_user_admin_access event with admin_access false and never checks user_id: demoting one member closed Confidential Info for the admin too (confidential-info.spec.js:193)
  - Where: Tools → Confidential Info, in three browser windows · account: iPhone XRR and Lava Device
  - Setup: In Settings → Admin Settings → User Management, iPhone XRR gives Lava Device View and Post on Confidential Info as a member, then makes Lava Device an admin again.
  - Steps: Lava Device (admin): open Tools → Confidential Info and check that the message box shows → iPhone XRR: open Confidential Info in a second browser window and leave it open → iPhone XRR: in User Management, switch Lava Device’s Admin off → Lava Device: check that Confidential Info closes by itself → iPhone XRR: after 5 more seconds, check that Confidential Info is still open in the second window
  - Expected: Losing admin closes Confidential Info for the demoted member only; the admin who made the change keeps it open. Known bug: iPhone XRR’s own second window loses Confidential Info too — the admin change closes it for everyone who has it open, not only for the demoted member.
  - Web error: Error: demoting B also closed Confidential Info for A

- **the History deep link shows no messages to a member without view access (rights, account B)** — suspected · known bug ToolsHistory has its view-access check commented out and the server answers the history request: a member without view access reads Confidential Info History by its address (70 deleted posts on screen, this test's own probe among them),ToolsHistory has its view-access check commented out and the server answers the history request: a member without view access reads Confidential Info History by its address (70 deleted posts on screen, this test's own probe among them) (confidential-info.spec.js:232)
  - Where: Confidential Info History, opened by its direct address, in two browsers · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts and deletes a message marked [e2e] in Confidential Info, so History holds a record. Then, in User Management, iPhone XRR makes Lava Device a non-admin without View on Confidential Info.
  - Steps: iPhone XRR: open the Confidential Info History address directly and check that the deleted message is listed → Lava Device: open the same Confidential Info History address directly → Give the page 10 seconds to load after the History heading (if it shows) → Count the messages on screen, and look for iPhone XRR’s deleted [e2e] message among them → Check that no messages are on screen
  - Expected: A member without view access sees no Confidential Info History messages, even through its direct address. Known bug: the History page opens and lists the deleted posts (about 70 on dev on 17 Nov), including the message iPhone XRR deleted in this test.
  - Web error: Error: a member without view access reads Confidential Info History

- **without view access the Confidential Info tile is gone and its deep link does not open it (rights, account B)** — passed (confidential-info.spec.js:301)
  - Where: Tools page and the Confidential Info address, in two browsers · account: iPhone XRR and Lava Device
  - Setup: In Settings → Admin Settings → User Management, iPhone XRR makes Lava Device a non-admin without View on Confidential Info.
  - Steps: Lava Device: open the Tools page and check that the Info tile shows (the grid has loaded) → Check that there is no Confidential Info tile → Open the Confidential Info address directly → Wait 10 seconds → Check that no Confidential Info feed is on screen and no “No Unit found” page shows
  - Expected: Without view access the Confidential Info tile is hidden and its direct address does not open it.

## Sheet bugs (31)

- **CONF-02** Can't forward a message from Confidential Info to the Home Bulletin — Medium · Web: Open, Android: Not affected
  - Steps: 1. Open Home and Confidential Info. 2. In Confidential Info, open a message's options → Forward In App. 3. Look at "Home Tab".
  - Expected: "Home Tab" expands to its units (Bulletin, …).
  - Actual: "Home Tab" has no units under it, so the message can't be forwarded to the Bulletin.

- **CONF-04** Upper-case .EXE files are accepted as documents — Medium · Web: To verify, Android: Not affected
  - Steps: 1. Attach a file named TEST.EXE.
  - Expected: Blocked file types are rejected whatever the letter case.
  - Actual: Suspected: accepted.

- **CONF-05** Comment menu on a post card does nothing (Read By, Delete Comment, Edit Comment) — Medium · Web: Not affected, Android: Not affected
  - Steps: 1. Open Confidential Info on a post that already has comments. 2. On the card, tap a comment's ⋯ → Read By, or ⋯ → Delete Comment.
  - Expected: Read By opens READ BY USER. Delete Comment asks "Are you sure you want to delete?" No / Yes.
  - Actual: The menu closes and nothing happens. The actions work only after that card's comments change (a comment added or removed). The Comments screen's own menu works.

- **CONF-06** A member with View but no posting right sees an empty Comments screen — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. As an admin, post in Confidential Info and add comments. 2. Give a member View on Confidential Info with Post off. 3. As that member open the post's "View all N comments".
  - Expected: The Comments screen lists the post's comments (read-only).
  - Actual: The Comments screen is completely blank.

- **CONF-07** Confidential post text shows in the in-app "New message received" banner — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Give member B View on Confidential Info. 2. With B's app in the foreground (any screen, including Confidential Info), post or comment in Confidential Info as admin A.
  - Expected: Decided by product: the banner hides the confidential text, or showing it is accepted.
  - Actual: B's banner shows "Tools : Confidential Info : <author>" and the full post or comment text, even while B is on the Confidential Info screen.

- **CONF-08** After deleting a comment on the Comments screen, the screen still lists it — Medium · Web: Not affected, Android: Not affected
  - Steps: 1. Open a post's Comments screen. 2. On a comment tap ⋯ → Delete Comment → Yes.
  - Expected: The comment disappears from the Comments screen and from the card.
  - Actual: The comment was deleted on the server (the card dropped to "3 comments"), but the Comments screen still listed it.

- **CONF-09** Back from the Comments screen goes to Tools instead of Confidential Info — Medium · Web: Not affected, Android: Not affected
  - Steps: 1. Open Confidential Info → a post's "View all N comments". 2. Delete a comment there. 3. Tap the back chevron.
  - Expected: Back returns to Confidential Info.
  - Actual: Back returned to the Tools list.

- **CONF-10** A member's first post didn't appear live on the admin's open screen — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Admin A has Confidential Info open. 2. Member B (just given Post) posts in Confidential Info.
  - Expected: B's post appears on A's screen within a few seconds.
  - Actual: A got the banner but the post appeared only after reopening the tool. B's second post arrived live.

- **CONF-11** Deleted confidential posts stay readable in History for anyone with View — Low · Web: Not checked, Android: To verify
  - Steps: 1. Delete a post in Confidential Info. 2. As any user with View, open ⋯ → History.
  - Expected: Decided by product.
  - Actual: "Confidential Info History" lists the deleted post with its text.

- **CONF-12** Cards keep the search styling after Cancel (also after returning from Media Gallery) — Low · Web: Not checked, Android: Not affected
  - Steps: 1. Open Confidential Info and search for a word that matches. 2. Tap Cancel (or open Media Gallery and come back).
  - Expected: Cards go back to their normal look.
  - Actual: Author names lose bold, comment text gets larger and the last comment preview is clipped, until the screen is reopened.

- **CONF-13** After a posting-right change, a post's reply arrow quotes a different post — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Member B is in Confidential Info when an admin changes B's posting right (the screen pops and B reopens it). 2. On a post tap the reply arrow (or menu → Reply).
  - Expected: The reply strip quotes the post that was tapped.
  - Actual: The reply strip quoted another post. Reopening the screen fixed it.

- **CONF-14** The keyboard "Done" bar from the photo caption screen stays on screen over the composer — Low · Web: Not checked, Android: Not affected
  - Steps: 1. Attach a photo from the library and post it. 2. Look at the bottom of the Confidential Info screen.
  - Expected: No keyboard toolbar once the caption screen is gone.
  - Actual: A "Done" keyboard bar stays docked at the bottom and half-covers the composer and the selection bar (screenshots CONF-018-4 and CONF-018-8).

- **CONF-15** A PDF containing JavaScript can be posted — Medium · Web: Not checked, Android: Open
  - Steps: 1. Tap the paperclip → Document. 2. Pick a PDF with an embedded /JavaScript action (test file: evil-js.pdf). 3. Tap Send.
  - Expected: Blocked, as on web: "Blocked potentially malicious PDF: …"
  - Actual: The caption screen opens and the PDF posts (114 B), available to every member with access.

- **CONF-16** Unsent text is lost when you leave Confidential Info — Medium · Web: Not checked, Android: Open
  - Steps: 1. Type a message without sending it. 2. Go back to Tools. 3. Open Confidential Info again.
  - Expected: The draft is still in the composer, as on the Android Bulletin and on web.
  - Actual: The composer is empty and the text is gone, with no warning.

- **CONF-17** Update in Edit Message with no change still marks the message "Edited" — Low · Web: Not checked, Android: Open
  - Steps: 1. Long-press your own message → Edit. 2. Change nothing and tap Update.
  - Expected: The dialog closes and the message stays as it was.
  - Actual: The message now shows "Edited" next to its time, although nothing changed.

- **CONF-18** After one search, the arrows don't run a new search for a changed word — Low · Web: Not checked, Android: Open
  - Steps: 1. Search for a word that matches (Vivek: 3). 2. Change it to a word with no matches. 3. Tap an arrow.
  - Expected: The new word is searched, or the old result is cleared.
  - Actual: The count stays at 3.

- **CONF-19** Read By search with no match shows nothing — Low · Web: Not checked, Android: Open
  - Steps: 1. Open Read By User on a message. 2. Search for a name nobody has.
  - Expected: "No Users Found.", as on web.
  - Actual: The lists go blank with no message, and "10 remaining" stays as it was.

- **CONF-20** Confidential Info History has no search — Low · Web: Not checked, Android: Open
  - Steps: 1. Open Confidential Info → History. 2. Look for a way to search the deleted messages.
  - Expected: A search, as web's History has.
  - Actual: The screen shows the heading, "Records of deleted messages" and the list, with no search.

- **CONF-21** Gallery Docs shows file size as a bare number with no unit — Low · Web: Not checked, Android: Open
  - Steps: 1. Long-press a message → Gallery. 2. Open the Docs tab.
  - Expected: A readable size, such as 505 KB.
  - Actual: Sizes read "517319" and "574474" with no unit.

- **CONF-22** Android stops at 2,000 characters where web allows 8,000 — Low · Web: Not checked, Android: Open
  - Steps: 1. Paste 2,100 characters into the composer. 2. Tap Send.
  - Expected: The same limit as web (8,000).
  - Actual: "There is only 2000 Characters limit allowed to send", and nothing is sent; the text stays in the composer.

- **CONF-23** CSV files can't be attached — Low · Web: Not checked, Android: Open
  - Steps: 1. Tap the paperclip → Document. 2. Open a folder with a .csv file.
  - Expected: The CSV can be picked, as on web.
  - Actual: sample.csv is greyed out in the system picker.

- **CONF-24** Another member's comment offers Edit — Low · Web: Not checked, Android: Open
  - Steps: 1. As an admin, open the ▾ menu on another member's comment.
  - Expected: Only actions you may use are offered: Read By and, for an admin, Delete.
  - Actual: The menu lists Edit, Read By User and Delete.

- **CONF-25** A message sent after the connection drops stays on the clock with no Retry until the screen is reopened — Medium · Web: Not checked, Android: Open
  - Steps: 1. Open Confidential Info. 2. With the screen open, turn Wi-Fi off. 3. Send a message and wait. 4. Leave the screen and open it again.
  - Expected: The message shows Retry within a few seconds, as it does on the Bulletin.
  - Actual: The row keeps the clock icon with no Retry. It was still that way more than a minute later, and a second message did the same, so it looks as if it is still sending. Retry only appears after leaving and reopening the screen.

- **CONF-26** Confidential Info History is blank offline, with no message — Low · Web: Not checked, Android: Open
  - Steps: 1. Turn Wi-Fi off. 2. Open Confidential Info → History.
  - Expected: A message that History can't be loaded, with a way to retry.
  - Actual: The heading shows over an empty list. Only the app-wide "No Internet Connection" bar explains it.

- **CONF-27** Info / Confidential Info never apply a rights change while open (posting granted/revoked, view revoked) — High · Web: Not checked, Android: Not checked
  - Steps: 1. Crew member has view-only on Info (or view on Confidential Info) and has the tool open. 2. Admin grants Info posting rights (or revokes Confidential Info view) in the rights grid. 3. Look at the open window without reopening it.
  - Expected: Composer opens when posting is granted; when view is revoked the confidential posts leave the screen (web navigates to /film-tools).
  - Actual: Composer stays read-only / confidential posts stay visible until the window is closed and reopened. The board's units come from the tools grid's permissions; `info:posting-rights:update` reaches the board (UnitsChanged → loadUnits) at the same moment as ToolsRefresh starts the grid's network reload, so the board re-reads the OLD permissions; when the grid's answer lands, nothing re-tells the boards (SwapInRealRights does not list them), and access-grid:*-rights:update never reaches the board at all.

- **CONF-28** Download refused to crew who have download rights on Info / Confidential Info — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Crew member (not admin) has view + download on confidential_info_tool (or info_tool). 2. Open a post's menu on a file and choose Download.
  - Expected: The file is saved.
  - Actual: Refused with 'You do not have downloading rights on Confidential Info.' The app builds the board's unit from the tool's rights without copying download_access, so HomeUnit.canDownload is always false for non-admins.

- **CONF-29** No History view on Info or Confidential Info — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open Info or Confidential Info. 2. Look for History (deleted/replaced posts).
  - Expected: A History button opens the record of deleted/replaced posts, as on web.
  - Actual: No History control; the toggle is drawn only for Call Sheet and Production Report units (isDocumentUpload). The repository and view model already support it (loadHistory with ?deleted=).

- **CONF-30** 'Distribute to Document Distribution' missing on Info / Confidential Info attachments — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. As a crew member with Document Distribution posting rights, open a PDF post in Info or Confidential Info. 2. Open its menu.
  - Expected: 'Distribute to Doc Distribution' offered, filed under 'Info' / 'Confidential Info'.
  - Actual: Not offered: boardFeed passes no DistributionHook, and the menu item is drawn only for Call Sheet units.

- **CONF-31** Info / Confidential Info accept socket frames from other productions — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Be a member of productions P1 and P2; have P1 open with Confidential Info. 2. Someone posts in P2's Confidential Info.
  - Expected: P1's board ignores the frame (Home's source drops frames naming another production).
  - Actual: The frame is delivered to P1's board; it is not shown (unit id differs) but is written into P1's offline Confidential Info store under P2's unit id, and P2's rights/archive frames make P1's board re-read its strip. A frame without unit_id would be shown on P1's board.

- **CONF-32** No-access / switched-off Info shows Home's 'No Unit found' page with no way back — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Confidential Info (e.g. from a notification) as a crew member without view rights, or after the tool was switched off.
  - Expected: A message that names the tool and the missing right (with Request access), or the tool closes back to Film Tools as on web.
  - Actual: 'No Unit found — You don't have access to any unit yet. A project admin can give you access.' — Home's wording about units, which Info does not have, and no request action.

- **CONF-33** Tapping a Confidential Info notification opens the tool for a member with no viewing right — High · Web: Not checked, Android: Not checked
  - Steps: 1. Admin A turns member B's Confidential Info View, Post and Download OFF. 2. A Confidential Info notification arrives on B. 3. B taps the banner.
  - Expected: B is refused (no viewing right) and does not see the tool's content.
  - Actual: CONFIDENTIAL INFORMATION opens on B and shows every post and comment in the tool.
