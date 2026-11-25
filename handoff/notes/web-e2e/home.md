# Web E2E — Home

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).
Coverage: deep — Unit rail, Bulletin feed (shared chat suite)

## Web test cases (165)

### Home Bulletin feed

- **opens with the composer and the feed root @smoke** — passed (unit-feed.suite.js:156)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Check that the message box (“Type your message here...”) can be typed in → In the window layout, check that the window title reads “Home”
  - Expected: The Bulletin feed opens with a message box ready for typing; in the window layout its window is titled “Home”.

- **the scoped composer resolves to exactly one element @smoke** — passed (unit-feed.suite.js:165)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Count the message boxes in the Home Bulletin feed → Check that there is exactly one message box and one composer bar
  - Expected: The feed holds exactly one message box, even when other feeds stay open in the background.

- **theme toggle flips the surface theme and restores @smoke** — passed (unit-feed.suite.js:171)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Find the moon/sun theme button beside the tabs and note its label (“Switch to dark mode” or “Switch to light mode”) → Click the theme button → Check that the label has changed and the feed switched theme → Check that the app saved the new theme choice → Click the theme button again → Check that the label is back to the original one
  - Expected: The first click switches the theme and the app saves the new choice; the second click switches it back.

- **gallery (and history) FABs are present; scroll-to-bottom only when scrolled away @smoke** — passed (unit-feed.suite.js:187)
  - Where: Home → Bulletin tab, with at least 8 messages · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Check that the round Gallery button shows at the bottom right of the feed → Check that there is no History (clock) button → Check that the down-arrow scroll-to-bottom button is not shown → Scroll the feed up a long way → Check that the down-arrow button appears → Click the down-arrow button → Check that the down-arrow button disappears again
  - Expected: The Gallery button is there and no History button is; the scroll-to-bottom button shows only while the feed is scrolled away from the latest message.

- **every acked row has an options button; the counter is aria-live @smoke** — passed (unit-feed.suite.js:204)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Check that every saved message has its options (chevron) button → Paste 7,200 characters into the message box → Check that the character counter appears and is set to be read out by screen readers → Clear the message box
  - Expected: Every saved message has an options button, and the character counter is announced politely to screen readers.

- **the first character is auto-capitalised @smoke** — passed (unit-feed.suite.js:251)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “hello e2e” in the empty message box → Check that the box shows “Hello e2e” → Clear the box and type “abc” one key at a time → Check that the box shows “Abc” → Clear the box and paste “  lead” (two leading spaces) → Check that the box shows “  Lead” → Clear the box without sending
  - Expected: The first letter typed into an empty message box is upper-cased, also after leading spaces; nothing is sent.

- **empty and whitespace-only messages are not posted @smoke** — passed (unit-feed.suite.js:266)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and count the messages → Press Enter in the empty message box → Type four spaces and press Enter → Click the button at the right of the message box (Send or microphone) → Wait 3 seconds → Check that nothing was sent and the feed has the same number of messages → Clear the message box
  - Expected: An empty or spaces-only message is never sent and no new message appears.

- **the composer caps at 8,000 characters and shows the counter from 7,200 @smoke** — passed (unit-feed.suite.js:311)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Paste 7,199 characters into the message box → Check that no character counter is shown → Paste 7,200 characters instead → Check that the counter reads “7,200 / 8,000” → Paste 8,100 characters instead → Check that the box keeps only 8,000 characters and the counter reads “8,000 / 8,000” → Clear the box and check that the counter disappears
  - Expected: The message box never holds more than 8,000 characters, and the counter shows only from 7,200 characters on.

- **typing @ shows no mention picker @smoke** — passed (unit-feed.suite.js:408)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “@a” in the message box → Wait about 2 seconds → Check that no list of people to mention pops up → Clear the message box
  - Expected: Typing “@” does not open a mention picker on this feed.

- **the emoji picker lists emoji and closes again @smoke** — passed (unit-feed.suite.js:438)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the smiley (emoji) button → Check that the emoji picker opens with more than 10 emoji → Click the smiley button again (or click the message box if the picker is still open) → Check that the emoji picker is closed
  - Expected: The emoji picker opens with a full set of emoji and closes again.

- **the paperclip menu lists the expected pickers and closes on an outside click @smoke** — passed (unit-feed.suite.js:518)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip next to the message box → Check that the menu lists Upload Media, Video Upload, Image Upload, Document Upload, Audio Upload and Location Upload → Click in the message box → Check that the menu closes and the paperclip is still shown
  - Expected: The paperclip menu offers all six upload choices and closes on a click outside it.

- **each hidden file input advertises the right accept list @smoke** — passed (unit-feed.suite.js:536)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip to open the upload menu → Look up which file types each file picker accepts → Click in the message box to close the menu → Check that, together, the pickers accept .pdf, .wav, .png and .mp4 files → Check that there are five file pickers (one per menu item except Location Upload)
  - Expected: Each upload choice limits the file dialog to its own file types, and there is one file picker per upload choice except Location Upload.

- **Location Upload opens the Select Location modal; Cancel posts nothing @smoke** — passed (unit-feed.suite.js:645)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and count the messages → Click the paperclip, then “Location Upload” → Check that the “Select Location” window opens → Click Cancel → Check that the window closes and no new message appears
  - Expected: Location Upload opens “Select Location”, and cancelling it posts nothing.

- **closing the document preview posts nothing @smoke** — passed (unit-feed.suite.js:661)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and count the messages → Click the paperclip, then “Document Upload” → Pick the file sample.pdf → Check that the preview shows “sample.pdf” and its file size → Click Close in the preview → Wait 3 seconds and check that no new message appears
  - Expected: Closing the upload preview posts nothing.

- **the image picker rejects a gif @smoke** — passed (unit-feed.suite.js:672)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Image Upload” → Pick a GIF image (e2e.gif) → Check that the message “No valid image files found…” pops up → Check that no upload preview opens
  - Expected: A GIF is refused by Image Upload with “No valid image files found”, and nothing can be uploaded.

- **the document picker rejects an executable @smoke** — passed (unit-feed.suite.js:679)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload” → Pick an .exe file (e2e.exe) → Check that the message “Executable files are not allowed.” pops up → Check that no upload preview opens
  - Expected: An executable file is refused by Document Upload, and nothing can be uploaded.

- **a PDF carrying JavaScript is blocked @smoke** — passed (unit-feed.suite.js:686)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload” → Pick a PDF that contains JavaScript (e2e-bad.pdf) → Check that the message “Blocked potentially malicious PDF…” pops up → Check that no upload preview opens
  - Expected: A PDF that carries JavaScript is blocked, and nothing can be uploaded.

- **the audio and media pickers reject a text file @smoke** — passed (unit-feed.suite.js:693)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Audio Upload”, and pick a text file (e2e.txt) → Check that the message “No valid audio files found…” pops up and then goes away → Click the paperclip, then “Upload Media”, and pick the same text file → Check that the message “No valid media files found…” pops up → Check that no upload preview opens
  - Expected: A text file is refused by both Audio Upload and Upload Media, and nothing can be uploaded.

- **the options menu closes on Escape and on an outside click @smoke** — passed (unit-feed.suite.js:819)
  - Where: Home → Bulletin tab, with at least one message · account: iPhone XRR
  - Steps: Open Home Bulletin → Open the options menu of the latest saved message → Press Escape → Check that the menu closes → Open the same options menu again → Click in the message box → Check that the menu closes
  - Expected: The options menu closes both on Escape and on a click outside it.

- **search edge cases: no match, one character, spaces, regex characters, Escape @smoke** — passed (unit-feed.suite.js:1493)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and click the magnifying-glass Search button → Check that the drawer reads “0 Matches” → Type a nonsense word and check “No results found” shows with no results → Type the single letter “z” and check it still reads “0 Matches” without “No results found” → Type three spaces and check the message “Please write in search box” pops up → Type “[e2e] (” and then “.*”, and check that nothing breaks → Press Escape and check that the drawer stays open → Click Close in the search drawer
  - Expected: Search handles no match, a single letter, spaces and special characters without errors, and Escape does not close the drawer.

- **the Gallery drawer opens with its four tabs and closes on Escape @smoke** — passed (unit-feed.suite.js:1532)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Click the round Gallery button → Check that the “Gallery” drawer opens with the tabs Media (n), Links (n), Docs (n) and Audio (n) → Check that Media is the selected tab → Press Escape and check that the drawer closes
  - Expected: The Gallery opens on Media with its four counted tabs and closes on Escape.

- **the feed opens with the newest row in view @smoke** — passed (unit-feed.suite.js:1596)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Wait for the messages to finish loading → Check that the newest (last) message is in view
  - Expected: The feed opens scrolled to the newest message.

- **scrolling up loads an older page (skips under 50 rows) @smoke** — passed (unit-feed.suite.js:1603)
  - Where: Home → Bulletin tab, with at least 50 messages · account: iPhone XRR
  - Steps: Open Home Bulletin and count the loaded messages → Scroll up to the top of the feed, again and again for up to a minute → Check that older messages load above the first ones (the count grows) → Check that the app fetched an older batch of messages from the server
  - Expected: Scrolling to the top loads older messages.

- **minimizing and restoring the window keeps the feed and the draft @smoke** — passed (unit-feed.suite.js:1818)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin in the window layout → Type “[e2e] window-draft <timestamp>” in the message box without sending → Click the window’s Minimize button → Check that the window is hidden → Click the window’s tab in the window tab strip to restore it → Check that the message box is shown again and still holds the draft → Clear the message box
  - Expected: Minimizing and restoring the window keeps the feed and the unsent draft.

### Home units

- **home loads with unit tabs and the bulletin feed @smoke** — passed (home.spec.js:121)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home in SG Document Distribution and click the Bulletin tab → Check that the unit strip shows the Bulletin, Calendar and Call Sheet tabs → Check that at least one Bulletin message shows its message options button → Check that the message box (“Type your message here...”) can be typed in
  - Expected: Home opens with the Bulletin, Calendar and Call Sheet tabs, the Bulletin feed shows its messages, and the message box is ready to type in.

- **exactly one tab is selected and holds the roving tabindex @smoke** — passed (home.spec.js:130)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Check that exactly one unit tab is marked as selected → Check that the selected tab is the one the Tab key reaches in the unit strip → Check that every other tab is left out of the Tab order (reached only with the arrow keys)
  - Expected: Exactly one unit tab is selected, and only that tab is in the keyboard Tab order; the other tabs are reached with the arrow keys.

- **every tab label is resolved and mirrors its title @smoke** — passed (home.spec.js:139)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Check that the unit strip has at least one tab → For every tab, check that its name is not blank → Check that no tab name is a raw text key ending in “_label” → Hover each tab and check that its tooltip shows exactly its name → Check that each tab is linked to its unit (an identifier the page keeps for every tab)
  - Expected: Every unit tab shows a real name (no blank or untranslated key), and its tooltip repeats that name.

- **switch to the Calendar unit @smoke** — passed (home.spec.js:153)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Click the Calendar tab → Check that the Calendar tab is selected → Check that the Bulletin message box is no longer shown
  - Expected: Calendar becomes the selected tab and the Bulletin feed with its message box goes away.

- **switch to the Call Sheet unit and back @smoke** — passed (home.spec.js:160)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Click the Call Sheet tab → Check that the Call Sheet tab is selected → Click the Bulletin tab → Check that the Bulletin tab is selected again
  - Expected: Home switches to the Call Sheet unit and back to the Bulletin.

- **arrow keys, Home and End move the selection along the rail @smoke** — passed (home.spec.js:168)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Put the keyboard focus on the Bulletin tab and press the Right arrow key → Check that the next tab is selected and has the focus → Press the Left arrow key and check that Bulletin is selected again → Put the focus back on the selected tab, press End and check that the last tab is selected → With the focus on the selected tab, press Home and check that the first tab is selected → Press the Left arrow key on the first tab and check that the selection wraps to the last tab → Click the Bulletin tab
  - Expected: The arrow keys, Home and End move the selection along the unit strip, and Left on the first tab wraps to the last. The test also notes, without failing, whether the strip kept the focus after the Bulletin message box opened.

- **a fresh load selects the profile's default unit (or the first unit) @smoke** — passed (home.spec.js:200)
  - Where: Home, on a fresh page load · account: iPhone XRR
  - Steps: Load Home fresh without clicking any tab → Find the account’s default Home unit (the test reads it from the profile the app loads) → Note which unit tab is selected → If the default unit has a tab, check that it is the selected one → If the default unit has no tab here, or there is no default, check that the first tab is selected
  - Expected: A fresh load opens Home on the account’s default unit, or on the first unit when there is no default or the account can no longer see it.

- **history_identifier picks the unit once and is removed; a bogus key is ignored @smoke** — flaky (home.spec.js:223)
  - Where: Home, on a fresh page load · account: iPhone XRR
  - Steps: Load Home and note the selected (default) tab → Leave the “open Call Sheet next” marker in the browser, as the Back button of Call Sheet History does → Reload the page → Check that the Call Sheet tab is selected and the marker is gone → Leave a marker that names a unit that does not exist, and reload the page → Check that exactly one tab is selected and the Bulletin message box or the Calendar shows → Check that this marker is gone too
  - Expected: The “return to this unit” marker opens Home on that unit once and is then cleared; a marker for an unknown unit is ignored and cleared, and Home opens normally.
  - Web error: Error: expect(locator).toHaveAttribute(expected) failed

- **switching units drops the reply quote and empties the composer @smoke** — skipped (home.spec.js:244)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Open the newest message’s options menu and click Reply → Check that the reply quote shows above the message box → Click the Call Sheet tab and check that it is selected → Click the Bulletin tab → Check that the reply quote is gone and the message box is empty
  - Expected: Switching units drops a half-started reply: the quote is gone and the message box is empty.

- **switching tabs keeps the URL @smoke** — passed (home.spec.js:279)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Note the page address in the browser’s address bar → Click the Calendar tab and check that it is selected → Check that the page address has not changed → Click the Bulletin tab
  - Expected: Switching unit tabs does not change the page address, on the classic layout and in the workspace alike.

- **a narrow strip shows the scroll arrows @smoke** — passed (home.spec.js:289)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Narrow the browser window to 420 pixels wide → Check that a right scroll arrow appears at the end of the unit strip → Click the right scroll arrow → Check that a left scroll arrow appears → Widen the window back to 1280 pixels
  - Expected: When the unit tabs do not fit, the strip shows scroll arrows, and scrolling right brings up the left arrow.

- **units hidden by view_access:false or enabled:false have no tab (stub) @smoke** — passed (home.spec.js:303)
  - Where: Home · account: iPhone XRR
  - Setup: The test changes the server’s unit list so that Call Sheet is marked as not viewable and Calendar as switched off.
  - Steps: Open Home → Check that the Bulletin tab shows → Check that there is no Call Sheet tab → Check that there is no Calendar tab
  - Expected: Units the member cannot view, and units that are switched off, get no tab.

- **a failing unit list renders a blank Home without crashing (stub) @smoke** — passed (home.spec.js:332)
  - Where: Projects → SG Document Distribution (Home opens on entry) · account: iPhone XRR
  - Setup: The test makes the server fail the Home unit list with a server error.
  - Steps: Open the Projects list and click SG Document Distribution → Wait 8 seconds → Check that no unit tab strip is shown → Check that the “Something went wrong.” crash page is not shown
  - Expected: When the unit list fails to load, Home stays blank but does not crash. Note: Home shows nothing that explains the blank screen (recorded as a known issue; BUL-07 checks for an error panel).

- **an empty unit list renders a blank Home without crashing (stub) @smoke** — passed (home.spec.js:332)
  - Where: Projects → SG Document Distribution (Home opens on entry) · account: iPhone XRR
  - Setup: The test makes the server answer the Home unit list with an empty list.
  - Steps: Open the Projects list and click SG Document Distribution → Wait 8 seconds → Check that no unit tab strip is shown → Check that the “Something went wrong.” crash page is not shown
  - Expected: With no units at all, Home stays blank but does not crash. Note: Home shows no empty-state message (recorded as a known issue).

- **the rail theme toggle flips the surface theme, persists, and is restored @smoke** — passed (home.spec.js:353)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Note whether the unit strip is light or dark → Click the sun / moon theme button at the right end of the unit strip → Check that the strip switches theme and the Bulletin feed takes the same theme → Check that the new theme is saved in the browser → Reload the page and open Home → Check that the strip still shows the new theme → Click the theme button again and check that the original theme is back
  - Expected: The theme button switches the strip and the feed between light and dark, and the choice survives a reload.

- **a cold load shows the Home skeleton, then the tabs @smoke** — passed (home.spec.js:371)
  - Where: Projects → SG Document Distribution (Home opens on entry) · account: iPhone XRR
  - Steps: Open the Projects list and click SG Document Distribution → Check that the “Loading home” placeholder or the unit tabs appear → Check that the unit tabs appear → Check that the loading placeholder is gone
  - Expected: On entering the project Home shows its loading placeholder (when the load is slow enough to see it), then the unit tabs, and the placeholder goes away.

- **an unsent draft survives switching units @smoke** — passed (home.spec.js:382)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Type “[e2e] draft <time>” in the message box without sending it → Click the Calendar tab and check that the message box is gone → Click the Bulletin tab → Check that the message box still holds the draft
  - Expected: An unsent Bulletin draft is still there after switching to Calendar and back.

- **Bulletin and Call Sheet keep separate drafts @smoke** — passed (home.spec.js:393)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Type “[e2e] bulletin draft <time>” in the message box without sending it → Click the Call Sheet tab and check that its message box is empty → Type “[e2e] callsheet draft <time>” there without sending it → Click the Bulletin tab and check that the message box shows the Bulletin draft → Clear it, click the Call Sheet tab and check that it shows the Call Sheet draft → Clear it and click the Bulletin tab
  - Expected: Each unit keeps its own draft: the Bulletin and the Call Sheet never show each other’s text.

- **the Bulletin offers the mic on an empty composer; the Call Sheet does not @smoke** — passed (home.spec.js:414)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab, with the message box empty → Note the button beside the empty message box (the microphone) → Click the Call Sheet tab and wait for its message box → Check that the same button on the empty Call Sheet message box is different (no microphone) → Click the Bulletin tab
  - Expected: The empty Bulletin message box offers the microphone; the empty Call Sheet message box shows a different button instead.

- **an MDI project lands on /workspace with a maximized Home window @smoke** — passed (home.spec.js:445)
  - Where: Home, in the workspace (window) layout · account: iPhone XRR
  - Steps: Open SG Document Distribution so that Home opens → Check that the app opened the workspace page, not the classic Home page → Check that exactly one window holds the unit tabs → Check that this window’s title reads “Home”
  - Expected: In the window layout the project opens on the workspace with a single window titled “Home” that holds the unit tabs. (The test does not check that the window is maximised.)

- **closing the Home window and clicking the sidebar Home reopens it @smoke** — passed (home.spec.js:454)
  - Where: Home, in the workspace (window) layout · account: iPhone XRR
  - Steps: Open SG Document Distribution so that the Home window opens → Click the Close button of the Home window → Check that the unit tabs are gone → Click Home in the sidebar → Check that the unit tabs come back with exactly one tab selected
  - Expected: Closing the Home window removes it, and the sidebar’s Home item opens it again with a unit selected.

### Home Bulletin feed

- **post text with Enter: row, own sender, cleared composer, status-1 POST** — passed (unit-feed.suite.js:216)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “[e2e] post <timestamp>” in the message box → Press Enter → Check that the server accepts the message → Check that the message appears in the feed and the message box is empty again → Check that the message gets its options button (it is saved) and shows the sender’s name
  - Expected: The message is posted once, appears in the feed under the own name, and the message box is cleared.

- **the mic slot becomes Send once there is text, and Send posts** — passed (unit-feed.suite.js:238)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Note the microphone button at the right of the message box → Type “[e2e] send-button <timestamp>” in the message box → Check that the microphone button has turned into the Send (paper-plane) button → Click Send → Check that the message appears, gets its options button, and the message box is empty again
  - Expected: Typing turns the microphone button into Send, and clicking Send posts the message.

- **Shift+Enter adds a newline; Enter keeps both lines in one bubble** — passed (unit-feed.suite.js:282)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “[e2e] shift-first <timestamp>” in the message box → Press Shift+Enter → Check that nothing was posted yet → Type “second-<timestamp>” on the new line → Press Enter → Check that one message appears holding both lines, and it gets its options button
  - Expected: Shift+Enter only adds a line break; Enter then sends both lines together as one message.

- **Ctrl+Enter sends** — passed (unit-feed.suite.js:301)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “[e2e] ctrl-enter <timestamp>” in the message box → Press Ctrl+Enter → Check that the message appears in the feed and gets its options button
  - Expected: Ctrl+Enter sends the message just like Enter.

- **a long message clips with Read More and expands back with Read Less** — passed (unit-feed.suite.js:327)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] clip <timestamp>” followed by “lorem ipsum ” 40 times and the word “TAIL-<timestamp>” at the end → Check that the message shows “Read More” and the TAIL word is hidden → Click “Read More” → Check that the TAIL word is now shown → Click “Read Less” → Check that the TAIL word is hidden again
  - Expected: A long message is cut short with “Read More”, opens fully on click, and folds back with “Read Less”.

- **HTML in a message renders as text, with no page error** — passed (unit-feed.suite.js:343)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “<img src=x onerror=alert(1)> <b>bold</b> [e2e] xss <timestamp>” → Check that the message shows “<b>bold</b>” as plain text → Check that no bold text and no broken image appear in the message → Check that no script error or alert popup occurs
  - Expected: HTML typed into a message is shown as plain text and never runs.

- **links: https becomes a safe link; www and javascript: do not** — passed (unit-feed.suite.js:355)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “https://example.com/e2e-<timestamp>” → Check that the address becomes a link that opens in a new tab, starts with https and cannot reach back to the app (no opener) → Post “see www.example.com e2e-www-<timestamp>” → Check that this message has no link → Post “javascript:alert(1) e2e-js-<timestamp>” → Check that this message has no link either
  - Expected: A full https address becomes a safe link that opens in a new tab; a bare “www.” address and a “javascript:” text stay plain text.

- **an e-mail address becomes a mailto link** — passed (unit-feed.suite.js:374)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “qa+e2e@example.com e2e-mail-<timestamp>” → Check that the e-mail address shows as a link that opens a new e-mail
  - Expected: An e-mail address in a message becomes a link to write an e-mail to it.

- **the own mention token renders as the own name** — passed (unit-feed.suite.js:382)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Look up the account’s own user ID → Post “@{{<own user ID>}} hi e2e-mention-<timestamp>” → Note the sender name on the new message → Check that the message text shows “@<sender name>” → Check that no curly braces are left in the message text
  - Expected: A mention code for the own user is shown as “@” plus the own name.

- **special characters round-trip exactly and markdown stays literal** — passed (unit-feed.suite.js:394)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “🎬 مرحبا Café ©®™ *bold* _it_ 12345 e2e-chars-<timestamp>” → Check that the message shows 🎬, مرحبا, Café, ©®™, *bold*, _it_ and 12345 exactly as typed → Check that nothing in the message is shown in bold or italics
  - Expected: Emoji, right-to-left text, accents and symbols come back unchanged, and markdown markers stay as typed.

- **an emoji from the picker is appended at the end and sent** — passed (unit-feed.suite.js:418)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “[e2e] emoji <timestamp>” in the message box → Press Home to move the cursor to the start of the text → Click the smiley (emoji) button and pick the first emoji → Check that the emoji was added at the end of the text, not at the cursor → Press Enter → Check that the message appears and gets its options button
  - Expected: The picked emoji is added at the end of the text wherever the cursor is, and the message sends with it.

- **an unsent draft survives a reopen and is cleared once sent** — passed (unit-feed.suite.js:449)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “[e2e] draft <timestamp>” in the message box without sending → Reload the page and go back to Home Bulletin → Check that the message box still holds the draft → Press Enter → Check that the message appears and gets its options button → Reload the page and go back to Home Bulletin again → Check that the message box is empty
  - Expected: An unsent draft is kept across a reload, and it is gone once it has been sent.

- **a posted message persists across a reopen** — passed (unit-feed.suite.js:466)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] persistent <timestamp>” and wait for its options button → Reload the page and go back to Home Bulletin → Check that the message is still in the feed
  - Expected: A posted message is still there after a reload.

- **a pending row shows the clock and no options button until the ack** — passed (unit-feed.suite.js:476)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Slow the connection so that sending takes about 4 seconds (the test holds the send request back) → Type “[e2e] pending <timestamp>” and press Enter → Check that the message appears at once with a clock icon and no options button → Wait for the send to finish → Check that the options button then appears
  - Expected: While a message is still being sent it shows a clock and no options button; the options button appears once the server has saved it.

- **a fast double Enter posts the message once** — passed (unit-feed.suite.js:501)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Type “[e2e] double-enter <timestamp>” in the message box → Press Enter twice at the same moment → Wait 5 seconds → Check that exactly one message with that text is in the feed
  - Expected: A double Enter sends the message only once.

- **a PDF uploads as a document bubble with its name, size and thumbnail** — passed (unit-feed.suite.js:556)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload” → Pick the file sample.pdf → In the preview, type “[e2e] pdf <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows “sample.pdf”, a “File Size - …” line and a PDF thumbnail
  - Expected: The PDF is posted as a document message with its name, its size and a thumbnail of its first page as a preview.

- **a xlsx document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload” → Pick the file sample.xlsx → In the preview, type “[e2e] sample.xlsx <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.xlsx” with a document icon
  - Expected: The Excel (xlsx) document is posted as a document message showing its file name and a document icon.

- **a docx document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload” → Pick the file sample.docx → In the preview, type “[e2e] sample.docx <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.docx” with a document icon
  - Expected: The Word (docx) document is posted as a document message showing its file name and a document icon.

- **a csv document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload” → Pick the file sample.csv → In the preview, type “[e2e] sample.csv <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.csv” with a document icon
  - Expected: The CSV document is posted as a document message showing its file name and a document icon.

- **a captioned document shows the caption and offers Edit Message** — passed (unit-feed.suite.js:581)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] doc-caption <timestamp>” → Check that the new message shows the caption → Open the message’s options menu → Check that it offers Edit Message and Download → Click Edit Message, change the text to “<caption> edited” and press Update → Check that the message shows the new caption with an “Edited” tag
  - Expected: A document with a caption shows the caption, and the caption can be edited; the message is then marked “Edited”.

- **an image uploads with its caption** — passed (unit-feed.suite.js:604)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Image Upload” → Pick the image red.png → In the preview, type “[e2e] image <timestamp>” in the “Type a message” box and press Upload → Check that a new message shows the image, the caption and a “File Size - …” line
  - Expected: The image is posted with its caption and its file size.

- **two images picked at once land as two rows** — passed (unit-feed.suite.js:615)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Upload Media” → Pick the two images red.png and blue.png together → Press Upload in the preview (it may read “Upload 2”) → Check that two new image messages appear → Check that each gets its options button and shows its image
  - Expected: Two images picked together are posted as two separate image messages.

- **an audio file uploads as an audio bubble** — passed (unit-feed.suite.js:631)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Audio Upload” → Pick the file sample.wav → Check that the preview names “sample.wav” → Press Upload → Check that a new message appears with a Play button and gets its options button
  - Expected: The audio file is posted as an audio message that can be played.

- **dropping an image on the feed opens the preview; Close posts nothing, Upload posts** — passed (unit-feed.suite.js:705)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Drag the image red.png from the computer and drop it on the message list → Check that the upload preview opens → Click Close and check that no new message appears → Drop red.png on the message list again → Press Upload in the preview → Check that a new image message appears, gets its options button and shows the image
  - Expected: Dropping an image opens the upload preview; closing it posts nothing, and Upload posts the image.

- **Image Reply posts a new image row carrying the caption** — passed (unit-feed.suite.js:731)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload red.png through the paperclip → Image Upload with the caption “[e2e] image-reply-src <timestamp>” → Open the image message’s options menu and click Image Reply → In the preview, type “[e2e] image-reply <timestamp>” in the “Type a message” box and press Upload → Check that a new, separate image message appears with that caption and the image → Check that the new message is not nested as a comment
  - Expected: Image Reply posts a new image message of its own carrying the reply caption, not a comment.

- **an own text message offers the expected menu and nothing forbidden** — passed (unit-feed.suite.js:750)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] menu-text <timestamp>” → Open the message’s options menu → Check that it offers Reply, Edit Message, Forward In App, Delete, Read By, Forward to Remote Projects and Share (Translate, if shown, is ignored) → Check that it does not offer Copy, Distribute, Publish to Doc Distribution, Download or Image Reply → Close the menu
  - Expected: The own text message menu holds exactly the expected actions and none of the forbidden ones.

- **an own document without a caption adds Download and drops Edit Message** — passed (unit-feed.suite.js:765)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Document Upload”, pick sample.csv and press Upload without a caption → Wait for the new document message to get its options button → Open its options menu → Check that it offers Download → Check that it offers neither Edit Message nor Image Reply → Check that it does not offer Publish to Doc Distribution → Close the menu
  - Expected: An own document without a caption offers Download, but no Edit Message, Image Reply or Publish to Doc Distribution.

- **an own image adds Download and Image Reply** — passed (unit-feed.suite.js:787)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload red.png through the paperclip → Image Upload with the caption “[e2e] menu-image <timestamp>” → Open the image message’s options menu → Check that it offers Download, Edit Message and Image Reply → Close the menu
  - Expected: An own captioned image offers Download, Edit Message and Image Reply.

- **an own audio adds Download and offers no Edit Message** — passed (unit-feed.suite.js:802)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the paperclip, then “Audio Upload”, pick sample.wav and press Upload → Wait for the new audio message to get its options button → Open its options menu → Check that it offers Download and no Edit Message → Close the menu
  - Expected: An own audio message offers Download and cannot be edited.

- **selection mode hides the options buttons and FABs and counts the selection** — passed (unit-feed.suite.js:833)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] selection <timestamp>” → Open its options menu and click Delete → Check that the bar reads “1 message(s) selected” and shows a Delete button → Check that no options buttons, no Gallery button and no message box are shown → Click the selected message → Check that the bar reads “0 message(s) selected” and the Delete button is gone → Click the message again and check that the bar reads “1 message(s) selected” → Leave selection mode with the bar’s X button → Check that the message box and the Gallery button are back and the message is still there
  - Expected: Selection mode hides the options buttons, the round buttons and the message box, counts the selected messages, and leaving it restores everything without deleting anything.

- **edit an own message: prefilled modal, Update, Edited tag, row moves last** — passed (unit-feed.suite.js:859)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] edit-original <timestamp>”, then “[e2e] edit-sibling <timestamp>” → Open the first message’s options menu and click Edit Message → Check that the “Edit Message” window holds the original text → Replace it with “[e2e] edit-updated <timestamp>” and press Update → Check that the message shows the new text with an “Edited” tag → Note whether the edited message moved to the bottom of the feed
  - Expected: The edit window opens with the current text, and after Update the message shows the new text marked “Edited”. The test only records whether the edited message moved to the bottom; it does not fail on it.

- **edit Cancel and Escape leave the message unchanged** — passed (unit-feed.suite.js:884)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] edit-keep <timestamp>” → Open its options menu, click Edit Message and type “discarded-<timestamp>” → Click Cancel → Check that the window closes and the message still shows the original text → Open Edit Message again and type “discarded-<timestamp>” → Press Escape → Check that the window closes and no message shows the discarded text
  - Expected: Leaving the edit window with Cancel or Escape keeps the message as it was.

- **unchanged text sends no PUT, empty text keeps the modal open, the box caps at 8,000** — passed (unit-feed.suite.js:903)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] edit-noop <timestamp>” → Open its options menu, click Edit Message and press Update without changing anything → Check that the window closes, nothing is sent to the server and no “Edited” tag appears → Open Edit Message again, replace the text with spaces and press Update → Check that the window stays open → Paste 8,100 characters into the edit box → Check that the box keeps only 8,000 characters → Click Cancel and check that no edit was sent at any point
  - Expected: Saving unchanged text sends no edit, an empty edit is refused with the window left open, and the edit box holds at most 8,000 characters.

- **a failing edit keeps the text and a refused delete keeps the row** — passed (unit-feed.suite.js:932)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] edit-fail <timestamp>” → Make the server fail every edit with a server error (the test fakes this) → Open Edit Message, change the text to “<original> nope” and press Update → Wait 3 seconds; if the edit window is still open, click Cancel → Check that the message still shows the original text and the “nope” text appears nowhere → Make the server refuse the delete with an error message (the test fakes this) → Open the options menu, click Delete, click Delete in the bar and answer Yes → Wait 3 seconds and check that the message is still in the feed → Leave selection mode if it is still on
  - Expected: When the edit fails the message keeps its original text, and when the server refuses the delete the message stays in the feed.

- **delete an own message through the selection flow (PUT delete/chats)** — passed (unit-feed.suite.js:977)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] delete-me <timestamp>” → Open its options menu and click Delete → Check that the bar reads “1 message(s) selected” → Click Delete in the bar → Check that the “Confirm Deletion” dialog asks “Are you sure you want to delete these messages…” → Click Yes → Check that the message disappears and exactly one delete was sent to the server
  - Expected: The message is removed after the selection bar and the “Confirm Deletion” dialog, with a single delete request.

- **delete can be backed out at both steps** — passed (unit-feed.suite.js:1000)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] delete-keep <timestamp>” → Open its options menu and click Delete → Leave selection mode with the bar’s X button → Check that the message is still there → Open its options menu, click Delete, then click Delete in the bar → Click No in the confirm dialog → Check that the dialog closes and the message is still there → Leave selection mode if it is still on
  - Expected: Backing out of a delete, either at the selection bar or at the confirm dialog, keeps the message.

- **multi-select delete removes two messages at once** — passed (unit-feed.suite.js:1016)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] multi-a <timestamp>” and “[e2e] multi-b <timestamp>” → Open the first message’s options menu and click Delete → Check that the bar reads “1 message(s) selected” → Click the second message → Check that the bar reads “2 message(s) selected” → Click Delete in the bar and answer Yes → Check that both messages disappear
  - Expected: Two selected messages are deleted together in one go.

- **a deleted message stays gone after a reopen** — passed (unit-feed.suite.js:1033)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] delete-reopen <timestamp>” → Delete it: options menu → Delete → Delete in the bar → Yes → Reload the page and go back to Home Bulletin → Check that the deleted message does not come back
  - Expected: A deleted message is still gone after a reload.

- **a reply nests as a comment inside the parent, which moves to the bottom** — passed (unit-feed.suite.js:1046)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] reply-target <timestamp>”, then “[e2e] reply-sibling <timestamp>” → Open the first message’s options menu and click Reply → Check that a quote of the message shows above the message box → Type “reply-<timestamp>” and press Enter → Check that the reply shows as a comment inside the first message, with a sender name and its own options button → Check that exactly one comment was sent and the reply text appears in only one message → Note whether the replied-to message moved to the bottom of the feed
  - Expected: A reply is shown once, as a comment nested inside the message it answers. The test only records whether that message moved to the bottom; it does not fail on it.

- **an own comment renders once — the socket echo must not duplicate it** — passed (unit-feed.suite.js:1071)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] comment-once <timestamp>” → Open its options menu, click Reply, type “once-<timestamp>” and press Enter → Check that the comment appears inside the message → Wait 5 seconds for the live update to arrive → Check that the comment is shown exactly once
  - Expected: The own comment is shown once and is not doubled when the live update for it arrives.

- **the reply quote names the sender and can be dismissed** — passed (unit-feed.suite.js:1086)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] quote <timestamp>” → Note the sender name shown on the message → Open its options menu and click Reply → Check that the quote above the message box shows the sender name and the message text → Click the quote’s X button → Check that the quote disappears and the message box is empty
  - Expected: The reply quote names the sender and repeats the message, and its X closes it without leaving text behind.

- **the paperclip is hidden while replying** — passed (unit-feed.suite.js:1102)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] reply-clip <timestamp>” → Check that the paperclip is shown → Open the message’s options menu and click Reply → Check that the reply quote shows and the paperclip is gone → Close the quote with its X button → Check that the paperclip is back
  - Expected: Attachments cannot be added while replying: the paperclip hides during a reply and returns when the reply is dismissed.

- **a whitespace-only reply sends nothing and keeps the quote** — passed (unit-feed.suite.js:1114)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] reply-blank <timestamp>” → Open its options menu and click Reply → Type three spaces and press Enter → Wait about 2 seconds → Check that no comment was sent and the reply quote is still shown → Close the quote with its X button
  - Expected: A reply made only of spaces is not sent, and the reply stays open.

- **edit an own comment** — passed (unit-feed.suite.js:1129)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] comment-edit <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the new comment’s options menu → Check that it offers Edit Message, Delete and Read By → Click Edit Message, change the text to “comment-<timestamp> edited” and press Update → Check that the comment shows the new text with an “Edited” tag
  - Expected: The own comment’s menu offers Edit Message, Delete and Read By, and an edited comment shows its new text marked “Edited”.

- **delete an own comment: No keeps it, Yes removes it** — passed (unit-feed.suite.js:1153)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] comment-delete <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the comment’s options menu and click Delete → Check that the “Delete Comment” dialog opens and click No → Check that the comment is still there → Open the comment’s options menu again, click Delete and answer Yes → Check that the comment disappears from the message
  - Expected: Answering No keeps the comment; answering Yes removes it.

- **a comment offers Read By** — passed (unit-feed.suite.js:1174)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] comment-readby <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the comment’s options menu and click Read By → Check that the “READ BY USERS” window opens → Close it with its X button
  - Expected: A comment’s Read By opens the “READ BY USERS” window, which closes again.

- **a failing comment POST shows an error toast** — passed (unit-feed.suite.js:1189)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] comment-fail <timestamp>” → Make the server refuse every new comment with an error message (the test fakes this) → Open the message’s options menu and click Reply → Type “comment-fail-<timestamp>” and press Enter → Check that an error message pops up → Check that no comment is added to the message → Close the reply quote if it is still shown
  - Expected: When the server refuses a comment, an error message pops up and no comment is added.

- **the Forward modal lists the destinations and its pickers send nothing** — passed (unit-feed.suite.js:1215)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin, post “[e2e] forward-look <timestamp>”, open its options menu and click Forward In App → Check that “Select Tab To Forward” lists Home Tab, Info Tab, Confidential Info Tab, Chat User, Group Chat and Email → Click Chat User and check that “Select Users” lists people → Search for a name that does not exist, check the list is empty, then clear the search and check the people come back → Click Select All and check the selected count equals the number of people listed → Click De-Select All and close the people picker with its X → Click Group Chat (reopen Forward In App first if the Forward window closed), search for a group that does not exist and check “No Data found” shows with no Send Group button → Close the “Select Group” window and the Forward window with their X buttons
  - Expected: The Forward window lists every destination, and its people and group pickers search, select and close without sending anything.

- **the Home Tab submenu lists Home units without Calendar or Call Sheet** — passed (unit-feed.suite.js:1262)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin, post “[e2e] forward-home-tab <timestamp>”, open its options menu and click Forward In App → Hover over “Home Tab” in the Forward window (click it if the submenu does not open) → Check that the submenu lists Bulletin → Check that it lists neither Calendar nor Call Sheet → Press Escape and close the Forward window
  - Expected: Forwarding to the Home tab offers the Home units such as Bulletin, but never Calendar or Call Sheet.

- **forwarding into this feed creates a copy (both deleted)** — passed (unit-feed.suite.js:1290)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home, click the Bulletin tab and post “[e2e] forward-self <timestamp>” → Open its options menu and click Forward In App → Hover over “Home Tab” in the Forward window and click Bulletin in its submenu → Check that “Forward Successfully” pops up → Check that the feed now shows two messages with that text, both saved
  - Expected: Forwarding a message to Home Tab → Bulletin adds a second copy of it to the Bulletin.

- **Forward to Remote Projects opens its modal and closes** — passed (unit-feed.suite.js:1316)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] forward-remote <timestamp>” → Open its options menu and click Forward to Remote Projects → Check that the “Forward to Remote Projects” window opens → Close it with its X button → Check that the window closes
  - Expected: The “Forward to Remote Projects” window opens and closes without forwarding anything.

- **Share opens Compose Email with every selected message; Cancel sends nothing** — passed (unit-feed.suite.js:1329)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] share-a <timestamp>” and “[e2e] share-b <timestamp>” → Open the first message’s options menu and click Share → Check that the bar reads “1 message(s) selected” → Click the second message and check that the bar reads “2 message(s) selected” → Click Share in the bar → Check that “Compose Email” opens holding the text of both messages → Click Cancel and check that the window closes → Leave selection mode if it is still on
  - Expected: Share puts every selected message into “Compose Email”, and Cancel closes it without sending.

- **Read By lists the two segments, searches and shows the empty state** — passed (unit-feed.suite.js:1357)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home, click the Bulletin tab, post “[e2e] readby <timestamp>”, open its options menu and click Read By → Check that the “READ BY USERS” window shows the READ USER and UN-READ USER switches → Click UN-READ USER and check that people are listed → Check that the notify button shows beside the switch (do not click it) → Search for a name that does not exist and check “No Users Found.” shows → Clear the search and check that the people come back → Close the window with its X button
  - Expected: Read By shows who has and has not read the message, offers the notify button for unread people, and its search works and shows “No Users Found.” for no match.

- **Publish to Doc Distribution shows on a document, not on text; Cancel creates nothing** — skipped (unit-feed.suite.js:1382)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] publish-dd <timestamp>” → Open the document message’s options menu and click Publish to Doc Distribution → Check that the “Publish to Document Distribution” dialog opens → Click Cancel and check that the dialog closes
  - Expected: A document message offers Publish to Doc Distribution, and cancelling its dialog publishes nothing.

- **Download on a PDF emits a download event** — passed (unit-feed.suite.js:1399)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] download-pdf <timestamp>” → Open the document message’s options menu → Click Download → Check that the browser downloads a file whose name ends in .pdf
  - Expected: Download on a PDF message saves a .pdf file. (The test switches off the browser’s “Save as” picker so the file downloads directly.)

- **Download on an image emits a download event** — passed (unit-feed.suite.js:1417)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload red.png through the paperclip → Image Upload with the caption “[e2e] download-image <timestamp>” → Open the image message’s options menu → Click Download → Check that the browser downloads an image file (.png, .jpg, .jpeg or .webp)
  - Expected: Download on an image message saves an image file. (The test switches off the browser’s “Save as” picker so the file downloads directly.)

- **a PDF bubble opens the viewer without a Download button; a spreadsheet does not** — passed (unit-feed.suite.js:1435)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload sample.pdf through Document Upload with the caption “[e2e] view-pdf <timestamp>” → Click the PDF in the new message → Check that the document viewer opens with a Close button and no Download button → Click Close and check that the viewer closes → Upload sample.xlsx through Document Upload with the caption “[e2e] view-xlsx <timestamp>” → Click the spreadsheet in the new message and wait a moment → Check that no document viewer opens
  - Expected: Clicking a PDF message opens the document viewer without a Download button; clicking a spreadsheet message opens nothing.

- **an image bubble opens the preview, Escape closes it** — passed (unit-feed.suite.js:1456)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload red.png through Image Upload with the caption “[e2e] view-image <timestamp>” → Click the image in the new message → Check that the full-size image preview opens → Press Escape → Check that the preview closes
  - Expected: Clicking an image opens its preview, and Escape closes it.

- **search finds a message, highlights it and scrolls to it on click** — passed (unit-feed.suite.js:1471)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Post “[e2e] searchable <timestamp>”, then two more messages “[e2e] search-pad-0 …” and “[e2e] search-pad-1 …” → Click the magnifying-glass Search button next to the message box → Check that the search box has the cursor → Type the full “[e2e] searchable <timestamp>” text → Check that the drawer reads “1 Match” and the result shows the text with the match highlighted → Click the result → Check that the feed scrolls the message into view and the search drawer stays open → Click Close in the search drawer
  - Expected: Search finds the message, highlights the match, and clicking the result scrolls the feed to it while the drawer stays open.

- **search by the own sender name lists own rows** — passed (unit-feed.suite.js:1520)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] search-sender <timestamp>” → Note the sender name on the new message → Click the magnifying-glass Search button and type that sender name → Check that the first result shows that name as its sender → Click Close in the search drawer
  - Expected: Searching for the own name lists the own messages.

- **the Gallery Docs tab lists an uploaded document, filters, and offers a card menu** — passed (unit-feed.suite.js:1551)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Upload sample.pdf through Document Upload with the caption “[e2e] gallery-doc <timestamp>” → Click the round Gallery button, then the Docs tab → Check that sample.pdf is listed → Type a nonsense word in the Gallery search and check “No matching documents” shows → Clear the search and check that sample.pdf is listed again → Click the small down arrow on the sample.pdf card → Check that its menu offers Forward In App → Press Escape twice and check that the Gallery closes
  - Expected: The Docs tab lists the uploaded document, its search filters the list, and each card has a menu with Forward In App.

- **a posted row shows avatar, sender · designation and a formatted timestamp** — passed (unit-feed.suite.js:1583)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] sender-line <timestamp>” → Check that the new message shows an avatar and the sender’s name → If a designation is shown, check that a “·” separates it from the name → Check that the time reads like “Nov 16, 2026 at 10:05 AM”
  - Expected: A posted message shows the avatar, “Name · Designation” and a timestamp in the form “Mon DD, YYYY at hh:mm AM/PM”.

- **the scroll-to-bottom button returns to the latest message** — passed (unit-feed.suite.js:1623)
  - Where: Home → Bulletin tab, with at least 8 messages · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] bottom-anchor <timestamp>” → Scroll the feed up a long way → Check that the down-arrow scroll-to-bottom button appears → Click the down-arrow button → Check that the posted message is back in view and the button disappears
  - Expected: The scroll-to-bottom button brings the latest message back into view and then hides.

- **another member's message offers no Edit Message** — passed (unit-feed.suite.js:1638)
  - Where: Home → Bulletin tab, with a message from another member · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] whoami <timestamp>” to see the own sender name → Find a saved message from another member, scrolling up if needed → Open that message’s options menu → Check that it does not offer Edit Message → Close the menu
  - Expected: A message sent by someone else cannot be edited.

- **History opens with its heading, alert and search, and comes back** — skipped (unit-feed.suite.js:1661)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Click the round History (clock) button → Check that the “Home Bulletin History” title shows with the note “Records of deleted messages.” → Check that a Search button shows and there is no message box and no History button → Click Back (or the browser’s back button if there is no Back) → Check that the feed with its message box is back
  - Expected: History opens as a read-only list of deleted messages titled “Home Bulletin History”, and Back returns to the feed.

- **a deleted message shows in History; text rows have no menu, PDFs offer View / Download / Print** — skipped (unit-feed.suite.js:1681)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin and post “[e2e] history-text <timestamp>” → Upload sample.pdf through Document Upload with the caption “[e2e] history-pdf <timestamp>” → Delete both messages through options menu → Delete → Delete in the bar → Yes → Click the round History (clock) button and wait for “Home Bulletin History” → Check that the deleted text message is listed without an options button → Check that the deleted PDF message is listed → Open the PDF message’s options menu → Check that it offers View, Download and Print → Close the menu
  - Expected: Deleted messages appear in History; a text message has no menu there, and a PDF offers View, Download and Print.

- **a message sent while offline errors and can be resent** — passed (unit-feed.suite.js:1709)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Switch the browser offline → Type “[e2e] offline-resend <timestamp>” and press Enter → Check that the message appears with a red retry icon → Switch the browser back online → Click the red retry icon, then “Resend Message” → Check that the red icon goes away and the message gets its options button
  - Expected: A message sent offline is marked as failed, and Resend Message posts it once the connection is back.

- **a POST answered with status 0 marks the row as failed** — passed (unit-feed.suite.js:1726)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Make the server refuse new messages with an error message (the test fakes this) → Type “[e2e] status-zero <timestamp>” and press Enter → Check that the message appears with a red retry icon → Let the server accept messages again → Click the red retry icon, then “Resend Message” → Check that the message gets its options button
  - Expected: A message the server refuses is marked as failed, and Resend Message posts it once the server accepts again.

- **a failed message can be deleted from the retry menu without a server call** — passed (unit-feed.suite.js:1750)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home Bulletin → Make every send fail with a network error (the test fakes this) → Type “[e2e] error-delete <timestamp>” and press Enter → Check that the message shows a red retry icon → Click the red retry icon, then Delete → Check that the selection bar shows, click Delete in it and answer Yes → Check that the failed message disappears → Check that no delete was sent to the server
  - Expected: A failed message can be removed from its retry menu, and the removal stays on this device without a server call.

- **a failing chat page GET leaves the composer usable and nothing crashes** — passed (unit-feed.suite.js:1783)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Make the server fail every request for the feed’s messages with a server error (the test fakes this) → Open Home Bulletin → Check that the “Something went wrong.” crash screen does not show → Check that the message box can still be typed in → Check that no loading indicator stays on screen
  - Expected: When the messages cannot be loaded the feed does not crash, stops loading, and the message box stays usable.

- **a message posted from a second context appears live** — passed (unit-feed.suite.js:1802)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Open Home → Bulletin tab in a second, separate browser session signed in to the same account → In the second session, post “[e2e] realtime <timestamp>” and wait for its options button → Check that the message appears in the first session without a reload → Close the second session
  - Expected: A message posted from another session shows up live in the open feed.

- **the Edit modal renders inside the tool window body** — passed (unit-feed.suite.js:1838)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home in the window layout and click the Bulletin tab → Post “[e2e] portal <timestamp>” → Open its options menu and click Edit Message → Check that the “Edit Message” window opens inside the Home window, not over the whole screen → Click Cancel
  - Expected: The Edit Message window opens inside the feed’s own window.

### Home units

- **switching units leaves selection mode and keeps the message** — passed (home.spec.js:258)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Posts “[e2e] select-switch <time>” in the Bulletin.
  - Steps: Open Home and click the Bulletin tab → Type “[e2e] select-switch <time>” and press Enter → Open that message’s options menu and click Delete → Check that the “1 message(s) selected” bar appears → Click the Call Sheet tab, then the Bulletin tab → Check that the selection bar is gone and the message box is back → Check that the message is still in the feed
  - Expected: Switching units leaves selection mode without deleting anything: the bar is gone and the message stays.

- **a same-language project shows no Translate item, banner or toggle** — passed (home.spec.js:427)
  - Where: Home → Bulletin tab, with the browser in the project’s language · account: iPhone XRR
  - Setup: Posts “[e2e] translate-probe <time>” in the Bulletin.
  - Steps: Open Home and click the Bulletin tab → Check that no translation hint banner shows → Check that no “translate before sending” switch shows above the message box → Type “[e2e] translate-probe <time>” and press Enter → Open that message’s options menu → Check that the menu has no Translate item → Press Escape
  - Expected: When the project and the browser use the same language, the Bulletin shows no translation banner, no translate-before-sending switch and no Translate menu item.

- **non-admin cannot edit or delete a message older than 30 minutes** — skipped (home.spec.js:471)

- **RGT-02..06 a member without posting rights sees the Request composer and the permission confirms** — skipped (home.spec.js:472)

- **unread badge and New Messages divider appear for another user's posts** — skipped (home.spec.js:473)

- **VOI-01..03 voice recording posts an audio message (fake-media flags)** — skipped (home.spec.js:474)

- **messages translate when project and device languages differ** — skipped (home.spec.js:475)

- **edits, deletes and comments from another account arrive live** — skipped (home.spec.js:476)

- **Read By moves a member to READ USER live** — skipped (home.spec.js:477)

- **custom-unit divider and personal projects without Home** — skipped (home.spec.js:478)

- **Location Upload → Select posts a map row (Google Maps + network)** — skipped (home.spec.js:479)

- **regression: post 50 varied-type bulletin messages, all land @regression** — passed (home.spec.js:486)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home and click the Bulletin tab → Post 50 messages one after another, each starting with a numbered marker “[e2e <run>#01]” to “#50” → Vary the text in a cycle: plain text, emoji, digits, symbols, accented letters, Arabic and Hebrew, a link, #tags and @team, a long lorem-ipsum paragraph, a mix → After each one, check that the message box empties and the message appears within 20 seconds → Check at the end that the 50th message is in the feed
  - Expected: All 50 messages of mixed content are posted and appear in the Bulletin.

### Sheet bugs — Bulletin

- **a BMP is previewed (or refused with a message) and never crashes Home @smoke** — passed (sheet-bugs/bulletin.spec.js:105)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home → Bulletin tab → Click the paperclip → Image Upload and pick a small BMP image → Check that the upload preview opens showing the picture, or that “No valid image files found” appears → Check that the “Something went wrong.” crash page never shows → Close the preview without uploading → Repeat with the paperclip → Upload Media (its refusal reads “No valid media files found”)
  - Expected: A BMP image is either previewed with its picture or refused with a clear message, and Home never crashes.

- **an upper-case .EXE is refused like .exe @smoke** — bug · known bug handleDocumentFiles compares the extension to "exe" without lower-casing it; seen on dev 14 Nov: TEST.EXE and Test.Exe open the upload preview,handleDocumentFiles compares the extension to "exe" without lower-casing it; seen on dev 14 Nov: TEST.EXE and Test.Exe open the upload preview (sheet-bugs/bulletin.spec.js:130)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home → Bulletin tab → Click the paperclip and choose Document Upload → Pick a file named “TEST.EXE” → Check that “Executable files are not allowed” appears and no upload preview opens (if a preview opens, close it without uploading) → Repeat with a file named “Test.Exe”
  - Expected: Upper- and mixed-case .EXE files are refused just like .exe. Known bug: TEST.EXE and Test.Exe open the upload preview instead of being refused.
  - Web error: Error: opened the upload preview instead of being refused: TEST.EXE, Test.Exe

- **every video type the picker offers (.mkv .wmv .3gp .mpeg) is accepted @smoke** — bug · known bug isVideoFile lists video/mkv and video/wmv (not real MIME types) and lacks video/3gpp, video/mpeg; seen on dev 14 Nov: .wmv .3gp .mpeg (and once .mkv) get "No valid video files found",isVideoFile lists video/mkv and video/wmv (not real MIME types) and lacks video/3gpp, video/mpeg; seen on dev 14 Nov: .wmv .3gp .mpeg (and once .mkv) get "No valid video files found" (sheet-bugs/bulletin.spec.js:138)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home → Bulletin tab → Click the paperclip and check that Video Upload is listed → Note which file types the Video Upload picker offers → For each of .mkv, .wmv, .3gp and .mpeg that it offers, pick a small file of that type through Video Upload → Check that no “No valid video files found” message appears → Close the upload preview without uploading
  - Expected: Every video type the picker offers is accepted after picking. Known bug: .wmv, .3gp and .mpeg (and sometimes .mkv) are refused with “No valid video files found”.
  - Web error: Error: offered by the picker, then refused after picking

- **Gallery search finds an image by a word of its caption** — passed (sheet-bugs/bulletin.spec.js:170)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Uploads an image with the caption “[e2e] gallery caption zebra<time>”.
  - Steps: Open Home → Bulletin tab and wait for the feed → Click the paperclip → Image Upload, pick an image, type the caption “[e2e] gallery caption zebra<time>” and click Upload → Click the Gallery floating button → Type the caption word “zebra<time>” in the Gallery search → Check that the image is found (allow up to 45 seconds) and “No matching media” does not show → Type the first 8 letters of the word in capitals and check that the image is still found → Type a word that matches nothing and check that “No matching media” shows → Press Escape
  - Expected: Gallery search finds an image by a word of its caption, in any letter case and by part of the word.

- **a member without posting rights cannot forward other members' Gallery items (stub) @smoke** — passed (sheet-bugs/bulletin.spec.js:200)
  - Where: Home → Bulletin tab → Gallery, in a second window made to look like a member without posting rights · account: iPhone XRR
  - Steps: Open Home → Bulletin tab → Open a second browser window of the same account in which faked server replies make it a non-admin without Bulletin posting rights → In that window, open Home → Bulletin tab and check that the REQUEST bar shows instead of the message box → Click the Gallery floating button → On the Media tab, open the ▾ menu of each item posted by another member (up to 12) → Check that none of these menus offers Forward → Do the same on the Docs tab
  - Expected: A member without posting rights is not offered Forward on other members’ Gallery items.

- **Gallery Docs shows the file name and a size with a unit** — passed (sheet-bugs/bulletin.spec.js:253)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Uploads “sample.pdf” with the caption “[e2e] gallery-size <time>”.
  - Steps: Open Home → Bulletin tab and wait for the feed → Click the paperclip → Document Upload, pick “sample.pdf”, type the caption “[e2e] gallery-size <time>” and click Upload → Click the Gallery floating button and open the Docs tab → Find the card with that caption → Check that the card shows a size with a unit (B, KB, MB or GB) → Check that the card is titled “sample.pdf” → Press Escape
  - Expected: The Gallery’s Docs card shows the real file name and a size with a unit.

- **a failed units request shows an error with Try Again, which loads the tabs (stub) @smoke** — passed (sheet-bugs/bulletin.spec.js:261)
  - Where: Projects → SG Document Distribution → Home · account: iPhone XRR
  - Setup: The test makes the server fail the Home unit list with a server error.
  - Steps: Open SG Document Distribution and go to Home → Check that an error panel saying “Unable to load the units” appears, with a Try Again button → Check that the “Something went wrong.” crash page does not show → Let the unit list load normally again (the test stops the fault) → Click Try Again → Check that the Bulletin tab appears
  - Expected: When the units fail to load, Home explains it and offers Try Again, which then loads the unit tabs.

- **Create New Unit offers a way to pick the unit's department or members @smoke** — suspected · known bug web creates a Home unit inline with its name only; the members picker (CreateHomeUnit) is unreachable,web creates a Home unit inline with its name only; the members picker (CreateHomeUnit) is unreachable,web creates a Home unit inline with its name only; the members picker (CreateHomeUnit) is unreachable (sheet-bugs/bulletin.spec.js:273)
  - Web error: Test timeout of 90000ms exceeded.

- **a translated post carries one "Translated Message" label in the project language (two users)** — bug · known bug generateTranslatedMessage takes the label from the sender's UI language; seen on dev 14 Nov: B (English) reads "Message traduit" under a French sender's post (no double colon),generateTranslatedMessage takes the label from the sender's UI language; seen on dev 14 Nov: B (English) reads "Message traduit" under a French sender's post (no double colon) (sheet-bugs/bulletin.spec.js:327)
  - Where: Home → Bulletin tab in two browsers: iPhone XRR with the browser language set to French, Lava Device in English · account: iPhone XRR and Lava Device
  - Setup: The test answers the translation request itself with the fixed text “E2E TRANSLATED TEXT”.
  - Steps: iPhone XRR: open a browser window set to French and open Home → Bulletin tab → iPhone XRR: turn on the “translate before sending” switch above the message box (if it is off) → iPhone XRR: type “[e2e] label-language <time> Bonjour à toute l’équipe” and press Enter → iPhone XRR: turn the switch back off if it was off → Lava Device: open Home → Bulletin tab and find the post → Lava Device: check that it shows the translated text → Lava Device: check that the label reads “Translated Message” exactly once, with no “Message traduit” and no double colon
  - Expected: The other member reads a single “Translated Message” label in the project’s language. Known bug: Lava Device reads the French label “Message traduit”, because the label follows the sender’s language.
  - Web error: Error: expect(received).not.toMatch(expected)

- **the designation follows the browser language back to English @smoke** — passed (sheet-bugs/bulletin.spec.js:388)
  - Where: Home → Bulletin tab, in English, then French, then English again · account: iPhone XRR
  - Steps: Open Home → Bulletin tab in English and wait for the feed → Note the designation shown after the sender name on the newest message → Open a new window of the same account with the browser language set to French, open Home → Bulletin tab and find the same message → Note its designation there → Switch the same signed-in browser back to English and open Home → Bulletin tab → Check that the message’s designation is the original English text again
  - Expected: After the browser is switched back to English, the designation is shown in English again, not kept in French.

- **"Translate before sending" really translates (the endpoint must not echo its input)** — passed · known bug backend: POST /v2/chat-gpt/translate on dev answers status 1 with the input text unchanged,backend: POST /v2/chat-gpt/translate on dev answers status 1 with the input text unchanged (sheet-bugs/bulletin.spec.js:432)
  - Where: Home → Bulletin tab, with the browser language set to French · account: iPhone XRR
  - Setup: Posts “[e2e] translate-echo <time> …” with translation before sending switched on.
  - Steps: Open a browser window set to French and open Home → Bulletin tab → Turn on the “translate before sending” switch above the message box (if it is off) → Type “[e2e] translate-echo <time> Le tournage commence demain matin, merci d’arriver tôt avec vos costumes.” and press Enter → Read the translation the server returns for it → Check that the translation succeeded and is not empty → Check that the translation differs from the French text → Turn the switch back off if it was off
  - Expected: Translate before sending returns a real translation. Known bug: on dev the translation service answers “success” with the text unchanged, so the message goes out in French under the translation label.

- **Delete from an own row's menu selects that row and only that row** — passed (sheet-bugs/bulletin.spec.js:477)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Posts “[e2e] select-sibling <time>”, then “[e2e] select-own <time>”.
  - Steps: Open Home → Bulletin tab and wait for the feed → Post “[e2e] select-sibling <time>”, then “[e2e] select-own <time>” → Open the options menu of “select-own” (the newest) and click Delete → Check that the bar reads “1 message(s) selected” → Check that exactly one tick box is ticked in the feed, and that it is on “select-own” → Leave selection mode with the bar’s ×
  - Expected: Delete from a message’s menu selects that message and only that one.

- **text typed at machine speed keeps up without a memory blow-up @smoke** — passed (sheet-bugs/bulletin.spec.js:499)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home → Bulletin tab → Note how much memory the page uses → Type a 1,500-character text (“machine speed typing check …” repeated) into the message box as fast as the browser takes keys → Check that the message box holds the whole text, with its first letter capitalised → Check that the page’s memory grew by less than 150 MB → Check that no page error occurred
  - Expected: Very fast input keeps up: the message box holds all 1,500 characters, nothing errors and memory stays in check.

- **search runs again when the word changes, including a word with no match @smoke** — passed (sheet-bugs/bulletin.spec.js:522)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home → Bulletin tab and wait for the feed → Note the sender name on the newest message → Click the search button next to the message box and type that name → Check that the count shows one or more Matches → Change the word to one that matches nothing (“zzq-no-match-<time>”) → Check that “No results found” and “0 Matches” show and the result list is empty → Change the word back to the name and check that there are matches again → Close search
  - Expected: Search runs again every time the word changes, including for a word with no match.

- **a 2,100-character message posts (web allows 8,000)** — passed (sheet-bugs/bulletin.spec.js:527)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Posts a 2,100-character message that starts with “[e2e] 2100-chars <time>”.
  - Steps: Open Home → Bulletin tab and wait for the feed → Type a 2,100-character message: “[e2e] 2100-chars <time>” followed by x’s → Press Enter → Check that the message is sent (its message options button appears) → Check that no length-limit message shows and the message has no red Retry icon
  - Expected: A 2,100-character message posts normally; web allows up to 8,000 characters.

- **the jump-to-latest button reaches the very end of a long newest row and goes away** — passed (sheet-bugs/bulletin.spec.js:533)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Posts a very long message (about 5,400 characters) that starts with “[e2e] long-newest <time>” and ends with “END”.
  - Steps: Open Home → Bulletin tab and wait for the feed → Post a very long message: “[e2e] long-newest <time>”, “lorem ipsum dolor sit amet” repeated 200 times, then “END” → Click Read More on it if shown, so it is at full height → Scroll the feed far up → Check that the jump-to-latest button appears, and click it → Check that the feed scrolls to the very end, the bottom of the long message → Check that the jump-to-latest button goes away
  - Expected: The jump-to-latest button takes the feed all the way to the end of a long newest message and then disappears.

- **a fast double click on Send posts once and does not reach for the microphone** — bug · known bug NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: one post is sent, but the second click lands on the mic and opens "Microphone access is required to record. Please enable microphone permissions in your browser settings.",NEW for web — the sheet only has it for Android (Web: Not checked); seen on dev 14 Nov: one post is sent, but the second click lands on the mic and opens "Microphone access is required to record. Please enable microphone permissions in your browser settings." (sheet-bugs/bulletin.spec.js:564)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: The test makes every microphone request fail and counts them.
  - Steps: Open Home → Bulletin tab and wait for the feed → Type “[e2e] double-click-send <time>” in the message box → Double-click the Send button quickly → Check that the message appears, then wait 4 seconds → Check that there is exactly one copy of it → Check that the browser was never asked for the microphone and no microphone dialog or message shows
  - Expected: A fast double click on Send posts the message once and does nothing else. Known bug: one post is sent, but the second click lands on the microphone button and opens “Microphone access is required to record. Please enable microphone permissions in your browser settings.”
  - Web error: Error: expect(locator).toHaveCount(expected) failed

- **a failed message can be deleted while still offline, and stays gone** — passed (sheet-bugs/bulletin.spec.js:605)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Steps: Open Home → Bulletin tab and wait for the feed → Turn the network off (the browser goes offline) → Type “[e2e] offline-delete <time>” and press Enter → Check that the message shows the red Retry icon → Click the Retry icon → Delete, then Delete in the selection bar, then Yes → Check that the message is gone and no “Internet connection lost” message shows → Turn the network back on and check that no delete request reached the server → Reopen Home → Bulletin tab, wait 5 seconds and check that the message has not come back
  - Expected: A message that never sent can be deleted while offline, with no server call, and it stays gone once the browser is back online.

- **a post still sending when the tab changes ends as one row: sent, or failed with Retry** — passed (sheet-bugs/bulletin.spec.js:637)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: The test slows the first post down by 5 seconds, and makes the second post fail after 4 seconds.
  - Steps: Open Home → Bulletin tab and wait for the feed → Type “[e2e] switch-mid-send <time>”, press Enter and check that it appears (still sending) → Click the Call Sheet tab, wait 8 seconds, then click the Bulletin tab → Check that the message is sent (its options button shows) and appears only once → Type “[e2e] switch-mid-fail <time>” and press Enter (this send will fail) → Switch to Call Sheet and back the same way → Check that this message shows once, with the red Retry icon
  - Expected: A post that is still sending when the tab changes ends as a single row: sent, or failed with Retry; never duplicated or stuck on sending.

- **a failed older-page load does not surface on the tab the user moved to (stub) @smoke** — passed (sheet-bugs/bulletin.spec.js:699)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: The test makes loading older Bulletin messages fail with a server error after 4 seconds.
  - Steps: Open Home → Bulletin tab and wait for the feed → Scroll the feed up until it starts loading older messages → Click the Call Sheet tab straight away and check that it is selected → Wait 8 seconds → Check that no error message appears → Check that Call Sheet is still the selected tab
  - Expected: A failed load of older Bulletin messages shows no error on the Call Sheet tab the user moved to.

- **a posted video plays with its picture** — skipped (sheet-bugs/bulletin.spec.js:739)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Uploads a short MP4 video.
  - Steps: Open Home → Bulletin tab and wait for the feed → Click the paperclip → Video Upload and pick a short MP4 video → Click Upload in the preview and wait for the video message → Click the message’s “Play video” button → Check that the video shows a picture, not only sound → Check that it plays forward over 3 seconds (the test mutes it) → Press Escape
  - Expected: A posted video plays with its picture.

- **a forwarded copy forwarded to a member reaches them (account B)** — passed (sheet-bugs/bulletin.spec.js:776)
  - Where: Home → Bulletin tab (iPhone XRR) and C & C (Lava Device) · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts “[e2e] forward-copy-to-member <time>” in Bulletin.
  - Steps: iPhone XRR: open Home → Bulletin tab and post “[e2e] forward-copy-to-member <time>” → Open its options menu → Forward In App → Home Tab → Bulletin; check “Forward Successfully” and that the feed now shows two copies → On the forwarded copy, open the options menu → Forward In App → Chat User → Search for Lava Device, select them and click Send → Check “Forward Successfully” → Lava Device: open C & C and the chat with iPhone XRR → Lava Device: check that the forwarded message arrives within a minute
  - Expected: A forwarded copy can be forwarded again to a member, and it reaches their C & C chat.

- **an edit arriving live keeps the post's replies (account B watches)** — passed (sheet-bugs/bulletin.spec.js:784)
  - Where: Home → Bulletin tab, in two browsers · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts “[e2e] edit-keeps-replies <time>” and replies to it with “e2e-reply-<time>”.
  - Steps: iPhone XRR: open Home → Bulletin tab, post “[e2e] edit-keeps-replies <time>” and reply to it with “e2e-reply-<time>” → Lava Device: open Home → Bulletin tab and check that the post shows with the reply under it → iPhone XRR: open the post’s options menu → Edit Message, add “ edited” to the text and click Update → Lava Device: without reloading, check that the edited text arrives (within 45 seconds) → Lava Device: wait 3 seconds and check that the reply is still under the post
  - Expected: An edit that arrives live keeps the post’s replies on the other member’s screen.

- **search by a member's name also finds posts that member replied to (account B)** — passed (sheet-bugs/bulletin.spec.js:816)
  - Where: Home → Bulletin tab, in two browsers · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts “[e2e] replied-by-b <time>”; Lava Device replies to it.
  - Steps: iPhone XRR: open Home → Bulletin tab and post “[e2e] replied-by-b <time>” → Lava Device: open Home → Bulletin tab and reply to that post with “e2e-b-reply-<time>” → iPhone XRR: wait for the reply to show (reload Home if it has not arrived within 30 seconds) → iPhone XRR: click the search button next to the message box and type Lava Device’s full name → Check that the results include the post Lava Device replied to → Close search
  - Expected: Searching a member’s name also finds posts that member only replied to.

- **promoting a member to admin opens their Bulletin composer live (rights, account B)** — passed (sheet-bugs/bulletin.spec.js:853)
  - Where: Settings → Admin Settings → User Management (iPhone XRR) and Home → Bulletin tab (Lava Device) · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: in Settings → Admin Settings → User Management, switch off Lava Device’s Admin, then open Posting Rights → Home → Bulletin and set View on and Post off → Lava Device: open Home → Bulletin tab → Lava Device: check that the REQUEST bar shows instead of the message box → iPhone XRR: switch Lava Device’s Admin back on and confirm with Yes → Lava Device: without reloading, check that the message box appears (within 45 seconds) and the REQUEST bar is gone
  - Expected: Making a member an admin opens their Bulletin message box live, without a reload.

- **creating a Home unit offers a choice of departments or members, not just a name @smoke** — failed (sheet-bugs/bulletin.spec.js:288)
  - Where: Settings → Admin Settings → Create Unit on Home · account: iPhone XRR
  - Steps: Open SG Document Distribution and wait for Home’s Bulletin tab → Click Settings in the sidebar and open the Admin Settings tab → Click “Create Unit on Home” → In the unit list dialog, click “Create New Unit” → Check that a field for the unit name shows → Check that the form also offers a choice of departments or members (for example ALL DEPARTMENT, SELECTED DEPARTMENT, SELECTED USER) → Click Cancel and press Escape, so nothing is created
  - Expected: Creating a Home unit asks for a name and lets the admin choose departments or members, not just a name.
  - Web error: Error: no field to name the new unit

### Home units

- **production-made units sit after one divider, behind the system units (a stub adds one when the project has none) @smoke** — passed (home.spec.js:490)
  - Where: Home (the unit tabs at the top) · account: iPhone XRR
  - Setup: When the project has no unit of its own, the test adds one (“E2E Custom Unit”) to the unit list the server sends back, for this browser only.
  - Steps: Open Home → Check that at least one production-made (custom) unit tab shows → Check that the system tabs (Bulletin, Calendar, Call Sheet) all come first and the custom tabs after them → Check that the tab strip has exactly one divider → Check that the divider sits right before the first custom tab → When the project has a real custom unit: click it, check that it opens on a message feed (message box or REQUEST bar), then click Bulletin
  - Expected: Units the production made are grouped after the system units, with one divider between the two groups; a real custom unit opens on its feed.

- **a personal project has no Home in the sidebar (a stub marks the sandbox personal in this browser) @smoke** — passed (home.spec.js:545)
  - Where: C & C, with the sandbox project treated as a personal project · account: iPhone XRR
  - Setup: The test makes this browser treat the sandbox project as a personal project: the project type in the saved project and in the server’s project reply reads “personal”. Nothing changes on the server.
  - Steps: Open C & C (where a personal project lands) → Wait for the sidebar to show Settings → Check that the sidebar shows C & C → Check that the sidebar has no Home item → Check that no Home unit tabs are on screen
  - Expected: A personal project has no Home: no Home item in the sidebar and no unit tabs.

- **Location Upload → Select posts the browser location as a map row** — passed (home.spec.js:606)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: The browser shares a fixed location (New Delhi) with the app.
  - Steps: Open Home → Bulletin tab → Click the paperclip and choose Location Upload → In “Select Location”, wait for the Google map to draw around the shared location → Click Select → Check that a new row appears in the feed and is sent (its options button shows) → Check that the row shows a map picture whose link points at the shared location → Check that the Select Location dialog has closed
  - Expected: Selecting the location posts a location message with a map picture of the place, and the dialog closes.

- **VOI-01..03 with a fake microphone: the mic records with a running timer, stop + Send posts an audio message, the bin discards** — passed (home.spec.js:648)
  - Where: Home → Bulletin tab · account: iPhone XRR
  - Setup: Runs in a separate browser started with a fake microphone (it records a test tone) and with microphone access allowed.
  - Steps: With the message box empty, click the microphone button → Check that the recording bar shows a timer that reaches at least 00:02 → Click the stop (pause) button and check that a playable preview of the recording shows → If no Send button shows yet, play the preview once → Click Send and check that the recording bar closes (the recording is posted at once, with no upload preview) → Check that a new audio message with a Play button appears and is sent, then delete it → Click the microphone again and, while it records, click the bin icon → Check that the recording bar closes, the message box returns and nothing new is posted
  - Expected: The microphone records with a running timer, stop and Send posts an audio message, and the bin throws a recording away without posting it.

- **another member's post badges the Bulletin tab while Calendar is open; opening Bulletin shows New Messages and clears the badge (account B)** — failed (home.spec.js:724)
  - Where: Home, in two browsers · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: open Home → Bulletin tab, then click the Calendar tab → iPhone XRR: note the Bulletin tab’s unread count (usually none) → Lava Device: open Home → Bulletin tab and post a message marked [e2e] → iPhone XRR: check that the Bulletin tab’s unread badge goes up and reads “N unread” → iPhone XRR: click the Bulletin tab → Check that Lava Device’s message shows, with a “New Messages” divider in the feed → Check that the Bulletin tab’s badge goes away
  - Expected: Another member’s post badges the Bulletin tab while another unit is open; opening the Bulletin shows a New Messages divider and clears the badge.
  - Web error: Error: expect(locator).toBeVisible() failed

- **another account's post, its edit, its comment and its delete all reach the open Bulletin live (account B)** — passed (home.spec.js:759)
  - Where: Home → Bulletin tab, in two browsers · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: open Home → Bulletin tab and leave it open → Lava Device: open Home → Bulletin tab and post a message marked [e2e] → iPhone XRR: check that the message appears without a reload → Lava Device: edit the message (Edit Message → Update) → iPhone XRR: check that the new text and the “Edited” label appear → Lava Device: reply to the message → iPhone XRR: check that the reply appears under the message → Lava Device: delete the message → iPhone XRR: check that the message disappears
  - Expected: Every change another account makes to a Bulletin post (new post, edit, reply, delete) shows on an open Bulletin at once, without a reload.

- **when another member opens the Bulletin, the open Read By list moves them to READ USER (account B)** — passed (home.spec.js:777)
  - Where: Home → Bulletin tab, in two browsers · account: iPhone XRR and Lava Device
  - Steps: iPhone XRR: open Home → Bulletin tab and post a message marked [e2e] → iPhone XRR: open the message’s Read By and search for Lava Device → Under UN-READ USER, check that Lava Device is listed → Switch to READ USER and check that Lava Device is not there → Lava Device: open Home → Bulletin tab and check that the message shows → iPhone XRR: with Read By still open on READ USER, check that Lava Device appears there without reopening it
  - Expected: As soon as Lava Device opens the Bulletin, the open Read By list moves them from UN-READ USER to READ USER.

- **on a French browser in an English project, another member's post offers Translate and shows the translation (fixed translation reply)** — passed (home.spec.js:833)
  - Where: Home → Bulletin tab, in two browsers; iPhone XRR’s second browser is set to French · account: iPhone XRR and Lava Device
  - Setup: Lava Device posts an English message marked [e2e]. The test answers the translation service itself with the fixed text “E2E TEXTE TRADUIT”. The project’s own language is not changed.
  - Steps: Lava Device: open Home → Bulletin tab and post “[e2e] … The shoot starts at seven tomorrow” → iPhone XRR: open Home → Bulletin tab in a browser set to French → Check that the translation hint banner shows → Open the options menu of Lava Device’s message → Check that it offers Translate (labelled in French) → Click Translate → Check that the message now shows the translation → Check that exactly one translation was asked for, into French, with the message’s text
  - Expected: When the browser’s language differs from the project’s, another member’s post can be translated into the reader’s language and the translation shows on the post.

- **RGT-02..06 a view-only member gets the REQUEST bar, the admin list behind it, and a posting-rights question on Reply; no Edit, Delete, Forward or Share on others' posts (rights, account B)** — passed (home.spec.js:886)
  - Where: Home → Bulletin tab, in two browsers · account: iPhone XRR and Lava Device
  - Setup: iPhone XRR posts a message marked [e2e]. Then, in Settings → Admin Settings → User Management, iPhone XRR makes Lava Device a non-admin with View but no Post on Bulletin.
  - Steps: Lava Device: open Home → Bulletin tab → Check that a REQUEST bar with a “You do not have Posting Rights” note replaces the message box and the paperclip → Click REQUEST and check that “Send Admin Request” lists admins with a Send Request button; close it without sending → Open the options menu of iPhone XRR’s message → Check that it offers Reply, but not Edit Message, Delete, Forward In App, Share or Forward to Remote Projects → Click Reply → Check that a question about asking for posting rights appears, and answer No → Check that no reply quote is left above the message box
  - Expected: A view-only member can only read: REQUEST replaces the message box, no write action is offered on others’ posts, and Reply asks for posting rights instead of replying.

- **a non-admin poster gets the 30-minute notice instead of Edit or Delete on an older own post (rights, account B, B's clock moved on)** — passed (home.spec.js:946)
  - Where: Home → Bulletin tab, in two browsers · account: iPhone XRR and Lava Device
  - Setup: In Settings → Admin Settings → User Management, iPhone XRR makes Lava Device a non-admin with View and Post on Bulletin.
  - Steps: Lava Device: open Home → Bulletin tab and post a message marked [e2e] → Open its options menu, click Edit Message, check that the Edit Message dialog opens, and Cancel → Move Lava Device’s browser clock 31 minutes forward → Open the options menu again and click Edit Message → Check that a notice says posts can be edited or deleted only within 30 minutes, and click Ok → Open the options menu again and click Delete → Check that the same notice shows, and click Ok → Check that no Edit Message dialog and no selection bar are open
  - Expected: A non-admin may edit or delete an own post only within 30 minutes of posting it; after that both actions show the 30-minute notice instead.

## Sheet bugs (34)

- **BUL-02** Attaching a BMP image crashes the Home screen — High · Web: In progress, Android: Not affected
  - Steps: 1. Open Home → Bulletin. 2. Click the paperclip → Image Upload. 3. Pick any .bmp image. (Also try Upload Media with the same file.)
  - Expected: The BMP is previewed and can be sent (BMP is listed as an allowed type), or it is rejected with a clear message.
  - Actual: Image Upload: the Home window crashes to "Something went wrong." (error code tool-window:/home-…) and needs a reload. Upload Media: the preview opens with no image in it.

- **BUL-03** Executable files with an upper-case extension (.EXE) can be attached — Medium · Web: Open, Android: Not affected
  - Steps: 1. Open Home → Bulletin. 2. Click the paperclip → Document Upload. 3. Pick a file named TEST.EXE (or Test.Exe).
  - Expected: Rejected with "No valid document files found. Executable files are not allowed.", the same as test.exe.
  - Actual: test.exe is rejected, but TEST.EXE and Test.Exe open the upload preview and can be sent.

- **BUL-04** Video formats offered by the picker are rejected after picking (.mkv, .wmv, .3gp, .mpeg) — Low · Web: Open, Android: Not affected
  - Steps: 1. Open Home → Bulletin. 2. Click the paperclip → Video Upload. 3. Pick a .mkv, .wmv, .3gp or .mpeg file (the picker lists these types).
  - Expected: The video is accepted, or the picker doesn't offer formats that can't be sent.
  - Actual: Toast "No valid video files found" for all four formats. An .mp4 is accepted.

- **BUL-05** Gallery search can't find an image by its caption — Low · Web: In progress, Android: Not affected
  - Steps: 1. Post an image with a caption, for example "zebra". 2. Open the Gallery (button at the bottom right of the feed). 3. Search for zebra.
  - Expected: The image is found (its caption is shown on the card).
  - Actual: "No matching media". Searching the file name (red.png) does find it.

- **BUL-06** A member without posting rights can forward other people's items from the Gallery — Medium · Web: In progress, Android: Not checked
  - Steps: 1. Log in as a member without Bulletin posting rights (not an admin). 2. Open the menu of a message from another member: it shows only Reply and Read By. 3. Open the Gallery and the ▾ menu of an item from another member.
  - Expected: Forward is hidden for other members' items, as in the message menu. (Share is already hidden.)
  - Actual: The Gallery menu offers Forward In App and Forward to Remote Projects.

- **BUL-07** Home shows a blank window when the units fail to load — Low · Web: In progress, Android: Not checked
  - Steps: 1. Open Home while the units request (GET /v2/home/unit/user) fails with a server error or status 0.
  - Expected: An error message with a way to retry.
  - Actual: The Home window is completely blank: no tabs, no composer, no message and no retry.

- **BUL-08** Translated message label is in the sender's language with a double colon — Medium · Web: Open, Android: Not checked
  - Steps: 1. On device A set the device language to French (project language English). 2. Home → Bulletin, type a French message, keep the translate checkbox ticked and send. 3. Look at the card on device B (English).
  - Expected: B sees the message with one "Translated Message:" label in the project language.
  - Actual: B saw the label in the sender's language with a double colon ("Message traduit ::"), written into the message body.

- **BUL-09** Delete from your own card's menu once selected another member's card — Medium · Web: Not affected, Android: Not affected
  - Steps: 1. Have your own card and another member's card on the Bulletin. 2. On your own card tap ⋯ → Delete.
  - Expected: Selection mode opens with your own card ticked.
  - Actual: Once in two tries the tick was on the other member's card. The selection was cancelled before deleting; the retry ticked the right card.

- **BUL-10** Card header keeps the French designation after switching the device back to English — Low · Web: Not affected, Android: Not checked
  - Steps: 1. Switch the device language to French and open Home → Bulletin. 2. Switch back to English and relaunch the app. 3. Look at the header of your own card.
  - Expected: The designation shows in English.
  - Actual: The designation still showed in French.

- **BUL-11** "Translate before sending" doesn't translate on dev: the message goes out in the original language under the translation label — Medium · Web: Open, Android: Not checked
  - Steps: 1. Open a project whose language differs from the browser's, e.g. ZL-French Fires (French) in an English browser. 2. Home → Bulletin: the composer shows "Translate my messages to French before sending: ON". 3. Send a message, e.g. "The shoot starts tomorrow morning, please arrive early with your costumes."
  - Expected: The message carries a French translation for French readers.
  - Actual: POST /v2/chat-gpt/translate (lang "fr") answers status 1 "integrations_chat_gpt_success" but returns the text unchanged, so the stored message is the original, the translation label, and the same untranslated text again. The other direction fails the same way (French text, lang "en", 3 sentences). Older web posts in ZL-French Fires do carry real French translations, so this broke recently.

- **BUL-12** Update in Edit Message with no change still marks the message "Edited" — Low · Web: Not checked, Android: In progress
  - Steps: 1. Long-press your own message → Edit. 2. Change nothing and tap Update.
  - Expected: The dialog closes and the message stays as it was.
  - Actual: The message now shows "Edited" next to its time, although nothing changed.

- **BUL-13** App crashes out of memory when text reaches the composer at machine speed (not at normal typing speed) — Low · Web: Not checked, Android: Open
  - Steps: 1. Feed text into the composer at machine speed, about 20 characters a second or faster (adb here). 2. Keep going past about 1,000 characters.
  - Expected: The composer keeps up, or simply slows down.
  - Actual: Java heap rose from 42 MB to 346 MB in about 1,000 characters, and the app crashed before 1,900 with OutOfMemoryError. Reproduced twice, the second time on a freshly started app. At human speed (about 4 characters a second, 300 characters) the heap stayed flat at 48-52 MB, and pasting up to 2,100 characters was fine, so normal use should not hit this.

- **BUL-14** After one search, the arrows don't run a new search for a changed word — Low · Web: Not checked, Android: In progress
  - Steps: 1. Tap the search icon, type a word and tap an arrow: it searches and shows a count. 2. Change the word to one with no matches. 3. Tap an arrow.
  - Expected: The new word is searched, or the old result is cleared.
  - Actual: The count stays at the old number (1 for "zzqq", which matches nothing). The keyboard's search key does run the new search and shows 0.

- **BUL-15** Declining media access makes the Document option do nothing, with no message — Low · Web: Not checked, Android: Open
  - Steps: 1. With media access not yet granted, tap the paperclip → Document. 2. Tap DON'T ALLOW on both prompts (music and audio, then photos and videos).
  - Expected: A message saying why the picker can't open, with a way to allow it. Picking a document shouldn't need photo or audio access, as Document Distribution's picker on this phone doesn't.
  - Actual: The screen returns to the paperclip menu with no message and the picker never opens.

- **BUL-16** Another member's message offers Edit, then says "You can't edit message after 30 Minutes" — Low · Web: Not checked, Android: In progress
  - Steps: 1. As an admin, long-press another member's message that is over 30 minutes old. 2. Tap Edit.
  - Expected: No Edit on other members' messages, as on web.
  - Actual: Edit is in the menu, and tapping it gives the 30-minute message, which suggests the only obstacle is the time.

- **BUL-17** Android stops at 2,000 characters where web allows 8,000 — Low · Web: Not checked, Android: Open
  - Steps: 1. Paste 2,100 characters into the composer. 2. Tap Send.
  - Expected: The same limit as web (8,000).
  - Actual: "There is only 2000 Characters limit allowed to send", and nothing is sent; the text stays in the composer. Exactly 2,000 sends. There is no live counter.

- **BUL-18** Read By search with no match shows nothing — Low · Web: Not checked, Android: In progress
  - Steps: 1. Open Read By User on a message. 2. Search for a name nobody has.
  - Expected: "No Users Found.", as on web.
  - Actual: The lists go blank with no message, and "13 remaining" stays as it was.

- **BUL-19** Gallery Docs tab shows file size as a bare number with no unit — Low · Web: Not checked, Android: Open
  - Steps: 1. Long-press a message → Gallery. 2. Open the Docs tab and read the line under a file name.
  - Expected: A readable size, such as 43 KB, like "File Size : 600.53 KB" in the feed.
  - Actual: Rows read "44099 · OCT 22, 2026" and "945 · OCT 21, 2026".

- **BUL-20** Forward → Home offers the Call Sheet unit, which web leaves out — Low · Web: Not checked, Android: Open
  - Steps: 1. Long-press a message → Forward → Yes. 2. Tap Home and read the unit list.
  - Expected: Bulletin and custom units only, as on web.
  - Actual: The list shows Bulletin, Call Sheet and Testing.

- **BUL-21** Jump-to-latest arrow stays on screen at the end of the feed — Low · Web: Not checked, Android: Open
  - Steps: 1. Post a long message so it is the newest row. 2. Scroll up, then tap the down arrow. 3. Scroll to the very end of the feed.
  - Expected: The arrow takes you to the end of the feed and then goes away.
  - Actual: It lands on the top of the long row, not the end, and the arrow stays visible at the very end of the feed.

- **BUL-22** A fast double tap on Send opens the microphone prompt — Low · Web: Not checked, Android: Open
  - Steps: 1. Type a message, with microphone access not yet granted. 2. Double-tap Send quickly.
  - Expected: One message is sent and nothing else happens.
  - Actual: One message is sent (correct), but Send has already turned back into the mic, so the second tap opens the microphone permission sheet.

- **BUL-23** A PDF containing JavaScript can be posted — Medium · Web: Not checked, Android: Open
  - Steps: 1. Tap the paperclip → Document. 2. Pick a PDF with an embedded /JavaScript action (test file: evil-js.pdf). 3. Tap Send.
  - Expected: Blocked, as on web: "Blocked potentially malicious PDF: …"
  - Actual: The caption screen opens and the PDF posts (114 B), available to every member.

- **BUL-24** CSV files can't be attached — Low · Web: Not checked, Android: Open
  - Steps: 1. Tap the paperclip → Document. 2. Open a folder with a .csv file.
  - Expected: The CSV can be picked, as on web.
  - Actual: sample.csv is greyed out in the system picker. PDF, DOCX, XLSX and TXT can be picked.

- **BUL-25** A message that failed to send can't be deleted while offline — Low · Web: Not checked, Android: Open
  - Steps: 1. Turn Wi-Fi off and send a message: it shows Retry. 2. Long-press it → Delete → Delete.
  - Expected: The unsent message is removed, as on web, where a failed row is deleted with no server call.
  - Actual: "Internet connection lost", and the row stays. Once online the same steps remove it.

- **BUL-26** A forwarded text message can't be forwarded to a member: it's dropped with no error — High · Web: Not checked, Android: Open
  - Steps: 1. On the Bulletin, forward a text message into Home → Bulletin, so a forwarded copy appears. 2. Long-press that copy → Forward → Yes → Members. 3. Tick a member → Submit. 4. Open that member's chat in C&C.
  - Expected: The message arrives in the member's chat, or the app says it could not be sent.
  - Actual: The app goes back to the Bulletin with no error, and the message never reaches the chat. The server refuses it with cnc_message_type_missing, which is only visible in the phone's log.

- **BUL-27** Half-typed text follows the user to the next unit tab and is posted there — High · Web: Not checked, Android: Not checked
  - Steps: 1. On Production Notices, type a message but do not send it. 2. Click another unit tab (e.g. Night Shoot). 3. Press Enter.
  - Expected: The other unit's composer is empty; each unit keeps its own draft, restored when you come back.
  - Actual: The text stays in the composer on the new tab; pressing Enter posts it to the wrong unit. Coming back, the original unit's draft is not its own.

- **BUL-28** A post still sending when the user switches tab comes back duplicated / stuck on 'Sending' — High · Web: Not checked, Android: Not checked
  - Steps: 1. Type a message (or attach a file) and press Post. 2. Before it finishes, click another unit tab. 3. Return to the first tab.
  - Expected: One card for the post: sent if it succeeded, 'Not sent' with Retry if it failed.
  - Actual: Success: two cards (server copy + a copy stuck on 'Sending' forever). Failure: the card is stuck on 'Sending' with no Retry, and the failure toast showed on the other tab.

- **BUL-29** An edit arriving over the socket replaces the whole post: replies vanish and a pinned post is unpinned — Medium · Web: Not checked, Android: Not checked
  - Steps: 1. Open a unit with a post that has replies (or is pinned). 2. On another device, edit that post.
  - Expected: Post text updates; replies and pin stay.
  - Actual: Replies disappear from the card and the pin banner drops the post until the board is read again (any time the edited frame omits `comments`/`pinned`, as the edit route's own answer does).

- **BUL-30** Find on board cannot find posts or replies by the sender's name — Medium · Web: Not affected, Android: Not checked
  - Steps: 1. Open the find bar on a board with posts written from the web. 2. Type a crew member's name (e.g. 'Grace'), or the name of someone who replied.
  - Expected: Posts by that person, and posts with a reply by them, are matched.
  - Actual: No match: search only reads the row's `name` field (absent on web-written rows) and never looks at reply senders.

- **BUL-31** Offline Film Tools grid loses its sections (everything under 'Ungrouped') when groups answered after the tools call — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Open Film Tools online (groups call slower than tools call). 2. Go offline and open Film Tools again.
  - Expected: The saved grid shows the same sections as online.
  - Actual: All tools are listed under 'Ungrouped'.

- **BUL-32** A failed 'Show older' shows its error on a different tab if the user switched tabs — Low · Web: Not checked, Android: Not checked
  - Steps: 1. Scroll to the top of a board so older posts load (slow network, request fails). 2. Switch tab before it answers.
  - Expected: No error on the new tab (the request belonged to the old one).
  - Actual: 'The board is busy…' appears on the new tab.

- **BUL-33** Realtime posting right not updated in ui — Medium · Web: , Android: 
  - Steps: User A giving admin rights to user B But user B side not updated in real time
  - Expected: 
  - Actual: 

- **BUL-34** Video not playing properly — High · Web: , Android: 
  - Steps: When user tap video for play voice coming but video not showing
  - Expected: 
  - Actual: 

- **BUL-35** In create home unit department,user selection option missing — High · Web: , Android: 
  - Steps: Only unit name option available
  - Expected: 
  - Actual: 
