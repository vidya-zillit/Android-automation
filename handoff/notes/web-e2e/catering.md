# Web E2E — Catering

Source: Zillit E2E Report (generated 2026-11-17T10:27:04.399Z, env dev.zillit.com).

## Web test cases (148)

### Catering feed

- **opens with the composer and the feed root @smoke** — passed (unit-feed.suite.js:156)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Check that the message box (“Type your message here...”) can be typed in → In the window layout, check that the window title reads “Catering”
  - Expected: The feed opens with a message box ready for typing; in the window layout its window is titled “Catering”.

- **the scoped composer resolves to exactly one element @smoke** — passed (unit-feed.suite.js:165)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Count the message boxes in the Catering feed → Check that there is exactly one message box and one composer bar
  - Expected: The feed holds exactly one message box, even when other feeds stay open in the background.

- **theme toggle flips the surface theme and restores @smoke** — passed (unit-feed.suite.js:171)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Find the moon/sun theme button beside the tabs and note its label (“Switch to dark mode” or “Switch to light mode”) → Click the theme button → Check that the label has changed and the feed switched theme → Check that the app saved the new theme choice → Click the theme button again → Check that the label is back to the original one
  - Expected: The first click switches the theme and the app saves the new choice; the second click switches it back.

- **gallery (and history) FABs are present; scroll-to-bottom only when scrolled away @smoke** — passed (unit-feed.suite.js:187)
  - Where: Tools → Catering (it opens on the Breakfast tab), with at least 8 messages · account: iPhone XRR
  - Steps: Open Catering → Check that the round Gallery button shows at the bottom right of the feed → Check that there is no History (clock) button → Check that the down-arrow scroll-to-bottom button is not shown → Scroll the feed up a long way → Check that the down-arrow button appears → Click the down-arrow button → Check that the down-arrow button disappears again
  - Expected: The Gallery button is there and no History button is; the scroll-to-bottom button shows only while the feed is scrolled away from the latest message.

- **every acked row has an options button; the counter is aria-live @smoke** — passed (unit-feed.suite.js:204)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Check that every saved message has its options (chevron) button → Paste 7,200 characters into the message box → Check that the character counter appears and is set to be read out by screen readers → Clear the message box
  - Expected: Every saved message has an options button, and the character counter is announced politely to screen readers.

- **the first character is auto-capitalised @smoke** — passed (unit-feed.suite.js:251)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “hello e2e” in the empty message box → Check that the box shows “Hello e2e” → Clear the box and type “abc” one key at a time → Check that the box shows “Abc” → Clear the box and paste “  lead” (two leading spaces) → Check that the box shows “  Lead” → Clear the box without sending
  - Expected: The first letter typed into an empty message box is upper-cased, also after leading spaces; nothing is sent.

- **empty and whitespace-only messages are not posted @smoke** — passed (unit-feed.suite.js:266)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and count the messages → Press Enter in the empty message box → Type four spaces and press Enter → Click the button at the right of the message box (Send or microphone) → Wait 3 seconds → Check that nothing was sent and the feed has the same number of messages → Clear the message box
  - Expected: An empty or spaces-only message is never sent and no new message appears.

- **the composer caps at 8,000 characters and shows the counter from 7,200 @smoke** — passed (unit-feed.suite.js:311)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Paste 7,199 characters into the message box → Check that no character counter is shown → Paste 7,200 characters instead → Check that the counter reads “7,200 / 8,000” → Paste 8,100 characters instead → Check that the box keeps only 8,000 characters and the counter reads “8,000 / 8,000” → Clear the box and check that the counter disappears
  - Expected: The message box never holds more than 8,000 characters, and the counter shows only from 7,200 characters on.

- **typing @ shows no mention picker @smoke** — passed (unit-feed.suite.js:408)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “@a” in the message box → Wait about 2 seconds → Check that no list of people to mention pops up → Clear the message box
  - Expected: Typing “@” does not open a mention picker on this feed.

- **the emoji picker lists emoji and closes again @smoke** — passed (unit-feed.suite.js:438)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the smiley (emoji) button → Check that the emoji picker opens with more than 10 emoji → Click the smiley button again (or click the message box if the picker is still open) → Check that the emoji picker is closed
  - Expected: The emoji picker opens with a full set of emoji and closes again.

- **the paperclip menu lists the expected pickers and closes on an outside click @smoke** — passed (unit-feed.suite.js:518)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip next to the message box → Check that the menu lists Image Upload, Document Upload and Audio Upload → Check that Upload Media, Video Upload and Location Upload are not offered → Click in the message box → Check that the menu closes and the paperclip is still shown
  - Expected: The paperclip menu offers only Image, Document and Audio Upload and closes on a click outside it.

- **each hidden file input advertises the right accept list @smoke** — passed (unit-feed.suite.js:536)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip to open the upload menu → Look up which file types each file picker accepts → Click in the message box to close the menu → Check that, together, the pickers accept .pdf, .wav and .png files → Check that there are three file pickers (Image, Document and Audio Upload)
  - Expected: Each of the three upload choices limits the file dialog to its own file types.

- **Location Upload opens the Select Location modal; Cancel posts nothing @smoke** — skipped (unit-feed.suite.js:645)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and count the messages → Click the paperclip, then “Location Upload” → Check that the “Select Location” window opens → Click Cancel → Check that the window closes and no new message appears
  - Expected: Location Upload opens “Select Location”, and cancelling it posts nothing.

- **closing the document preview posts nothing @smoke** — passed (unit-feed.suite.js:661)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and count the messages → Click the paperclip, then “Document Upload” → Pick the file sample.pdf → Check that the preview shows “sample.pdf” and its file size → Click Close in the preview → Wait 3 seconds and check that no new message appears
  - Expected: Closing the upload preview posts nothing.

- **the image picker rejects a gif @smoke** — passed (unit-feed.suite.js:672)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Image Upload” → Pick a GIF image (e2e.gif) → Check that the message “No valid image files found…” pops up → Check that no upload preview opens
  - Expected: A GIF is refused by Image Upload with “No valid image files found”, and nothing can be uploaded.

- **the document picker rejects an executable @smoke** — passed (unit-feed.suite.js:679)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload” → Pick an .exe file (e2e.exe) → Check that the message “Executable files are not allowed.” pops up → Check that no upload preview opens
  - Expected: An executable file is refused by Document Upload, and nothing can be uploaded.

- **a PDF carrying JavaScript is blocked @smoke** — passed (unit-feed.suite.js:686)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload” → Pick a PDF that contains JavaScript (e2e-bad.pdf) → Check that the message “Blocked potentially malicious PDF…” pops up → Check that no upload preview opens
  - Expected: A PDF that carries JavaScript is blocked, and nothing can be uploaded.

- **the audio and media pickers reject a text file @smoke** — passed (unit-feed.suite.js:693)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Audio Upload”, and pick a text file (e2e.txt) → Check that the message “No valid audio files found…” pops up and then goes away → Check that no upload preview opens
  - Expected: A text file is refused by Audio Upload, and nothing can be uploaded (Catering has no Upload Media).

- **the options menu closes on Escape and on an outside click @smoke** — passed (unit-feed.suite.js:819)
  - Where: Tools → Catering (it opens on the Breakfast tab), with at least one message · account: iPhone XRR
  - Steps: Open Catering → Open the options menu of the latest saved message → Press Escape → Check that the menu closes → Open the same options menu again → Click in the message box → Check that the menu closes
  - Expected: The options menu closes both on Escape and on a click outside it.

- **search edge cases: no match, one character, spaces, regex characters, Escape @smoke** — passed (unit-feed.suite.js:1493)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and click the magnifying-glass Search button → Check that the drawer reads “0 Matches” → Type a nonsense word and check “No results found” shows with no results → Type the single letter “z” and check it still reads “0 Matches” without “No results found” → Type three spaces and check the message “Please write in search box” pops up → Type “[e2e] (” and then “.*”, and check that nothing breaks → Press Escape and check that the drawer stays open → Click Close in the search drawer
  - Expected: Search handles no match, a single letter, spaces and special characters without errors, and Escape does not close the drawer.

- **the Gallery drawer opens with its four tabs and closes on Escape @smoke** — passed (unit-feed.suite.js:1532)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the round Gallery button → Check that the “Gallery” drawer opens with the tabs Media (n), Links (n), Docs (n) and Audio (n) → Check that Media is the selected tab → Check that opening the Gallery made no extra library request to the server → Press Escape and check that the drawer closes
  - Expected: The Gallery opens on Media with its four counted tabs, loads nothing extra from the server, and closes on Escape.

- **the feed opens with the newest row in view @smoke** — passed (unit-feed.suite.js:1596)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Wait for the messages to finish loading → Check that the newest (last) message is in view
  - Expected: The feed opens scrolled to the newest message.

- **scrolling up loads an older page (skips under 50 rows) @smoke** — passed (unit-feed.suite.js:1603)
  - Where: Tools → Catering (it opens on the Breakfast tab), with at least 50 messages · account: iPhone XRR
  - Steps: Open Catering and count the loaded messages → Scroll up to the top of the feed, again and again for up to a minute → Check that older messages load above the first ones (the count grows) → Check that the app fetched an older batch of messages from the server
  - Expected: Scrolling to the top loads older messages.

- **minimizing and restoring the window keeps the feed and the draft @smoke** — passed (unit-feed.suite.js:1818)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering in the window layout → Type “[e2e] window-draft <timestamp>” in the message box without sending → Click the window’s Minimize button → Check that the window is hidden → Click the window’s tab in the window tab strip to restore it → Check that the message box is shown again and still holds the draft → Clear the message box
  - Expected: Minimizing and restoring the window keeps the feed and the unsent draft.

### Catering shell and units

- **opens on the three meal tabs with a composer @smoke** — passed (catering.spec.js:107)
  - Where: Tools → Catering (opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Check that the Breakfast, Lunch and Dinner tabs are shown → Check that the “Type your message here...” box can be typed in → Check that the Breakfast tab is the selected one
  - Expected: Catering opens on its three meal tabs with Breakfast selected and a message box ready to type in.

- **opens from the Tools tile with the "Catering" window title @smoke** — passed (catering.spec.js:116)
  - Where: Tools → Catering tile · account: iPhone XRR
  - Steps: Open Tools → Click the Catering tile → Check that the Catering message box appears → If Catering opened in a window, check that the window title reads “Catering”
  - Expected: The Catering tile opens Catering, and its window is titled “Catering”.

- **a slow unit list shows the tab skeleton, never the permanent Loader @smoke** — passed (catering.spec.js:127)
  - Where: Tools → Catering · account: iPhone XRR
  - Setup: The test makes the list of meal units answer 2.5 seconds late.
  - Steps: Open Catering → Reload Catering while the unit list is slowed down → Check that grey placeholder tabs (the tab skeleton) show while it loads → Check that the Breakfast tab appears once the list arrives → Check that the placeholder tabs are gone
  - Expected: While the unit list is loading, Catering shows placeholder tabs and then the real tabs, never a spinner that stays.

- **the unit list loads first, then the first unit's chat page (limit=50&page=0) @smoke** — passed (catering.spec.js:148)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open the browser’s network panel → Open Catering → Check that the list of meal units is fetched first → Check that the first chat fetch comes after it and asks for the first page of 50 messages → Check that this chat fetch is for the first unit (Breakfast)
  - Expected: Catering loads its unit list first, then the first unit’s latest 50 messages.

- **the Back arrow shows on the classic layout only @smoke** — passed (catering.spec.js:166)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering → If Catering opened in a window, check that there is no Back arrow next to the tabs → On the classic full-page layout, check that the Back arrow is shown
  - Expected: The Back arrow shows on the classic layout only; a Catering window has none.

- **MDI window controls: minimize, restore, maximize, close @smoke** — passed (catering.spec.js:173)
  - Where: Tools → Catering (opens as a window) · account: iPhone XRR
  - Steps: Open Catering → Click Minimize on the Catering window and check that it hides → Click Catering in the workspace’s window tabs and check that it shows again → If Maximize is offered, click it, check that Restore appears, then click Restore → Click Close on the window → Check that Catering is no longer open
  - Expected: The Catering window can be minimized, brought back, maximized, restored and closed.

- **opening raises no page error and no badge-lookup console warning @smoke** — passed (catering.spec.js:192)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open the browser console → Open Catering and wait 3 seconds → Check that no page error was raised (a notification-permission error is ignored) → Check that there is no console warning about a missing unit name for the badges
  - Expected: Opening Catering raises no page error and no badge warning in the console.

- **the tabs match the unit list: count, translated labels, first selected @smoke** — passed (catering.spec.js:207)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering and note the units the server sends (network panel) → Check that there is one tab per unit → Check that each tab carries its unit’s name, with the meal units shown as Breakfast, Lunch and Dinner → Check that no tab shows a raw key such as “breakfast_label” → Check that the first tab is selected
  - Expected: The tabs match the project’s units one for one, with readable names and the first one selected.

- **switching a tab selects it and fetches that unit's chat @smoke** — passed (catering.spec.js:223)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering on the Breakfast tab → Click the second tab (Lunch) → Check that Lunch becomes the selected tab → Check that Lunch’s messages are fetched (network panel) → Check that the message box is still shown → Click Breakfast and check that it is selected again
  - Expected: Clicking a tab selects it and loads that unit’s chat.

- **an empty unit list leaves the permanent Loader with no tabs (documents the missing empty state) @smoke** — passed (catering.spec.js:245)
  - Where: Tools → Catering · account: iPhone XRR
  - Setup: The test makes the server answer the unit list with no units at all.
  - Steps: Open Catering → Reload Catering while the unit list comes back empty → Wait 5 seconds → Check that no tabs are shown → Check that the page did not crash with “Something went wrong.” → Note (not checked) whether the message box and a Loading text are shown
  - Expected: With no units, Catering shows no tabs and does not crash. Known bug: there is no empty state saying there are no units; the page just keeps loading (on dev the message box still shows from saved data).

- **a failing unit list leaves the permanent Loader with no tabs (documents the missing empty state) @smoke** — passed (catering.spec.js:245)
  - Where: Tools → Catering · account: iPhone XRR
  - Setup: The test makes the unit list fail with a server error.
  - Steps: Open Catering → Reload Catering while the unit list fails → Wait 5 seconds → Check that no tabs are shown → Check that the page did not crash with “Something went wrong.” → Note (not checked) whether the message box and a Loading text are shown
  - Expected: When the unit list fails, Catering shows no tabs and does not crash. Known bug: there is no error or empty message; the page just keeps loading.

- **the theme toggle flips the page theme, the body class and the stored preference @smoke** — passed (catering.spec.js:276)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering → Check that the theme button is labelled “Switch to dark mode” or “Switch to light mode” → Click the theme button → Check that the Catering page switches theme (light to dark or back) → Check that the rest of the page follows the new theme → Check that the browser remembers the new theme (saved preference) → Click the theme button again and check that the first theme is back
  - Expected: The theme button flips Catering between light and dark and remembers the choice.

- **a draft typed in Breakfast survives a visit to Lunch @smoke** — passed (catering.spec.js:334)
  - Where: Tools → Catering, Breakfast tab · account: iPhone XRR
  - Steps: Open Catering on the Breakfast tab → Type “[e2e] draft …” in the message box without sending it → Click the Lunch tab and check that its message box is empty → Click the Breakfast tab again → Check that the draft text is back in the message box
  - Expected: An unsent draft stays with its own tab: Lunch starts empty and Breakfast gets the draft back.

- **with no enabled caterer a send is refused with a toast — and the text is lost @smoke** — passed (catering.spec.js:346)
  - Where: Tools → Catering · account: iPhone XRR
  - Setup: The test makes the unit list say that no unit has any caterer.
  - Steps: Reload Catering with the faked unit list → Type “[e2e] lost …” in the message box and press Enter → Note (not checked) whether a toast says you cannot post until a person joins the Catering Department → Wait 2 seconds → Check that nothing was sent → Check that no message with that text appears → Check that the message box is empty
  - Expected: Without a caterer in the unit, the send is refused and nothing is posted. Known bug: the typed text is cleared anyway, so the message is lost.

- **the Select User button and the caterer instructions follow the caterer role @smoke** — skipped (catering.spec.js:387)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering → Find out whether the account is in the catering department (its profile) → If it is, check that the “Select User” button and the blue instructions box (“Click on Select User…”) are shown → If it is not, check that neither is shown
  - Expected: The Select User button and the caterer instructions show for catering-department members only.

- **the "+" and Settings ▸ Catering Settings follow the catering designation / department @smoke** — skipped (catering.spec.js:430)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering → Find out whether the account has a catering designation (its profile) → Check that the orange “+” (new unit) button next to the tabs is shown only if it has one
  - Expected: The “+” button for adding a unit shows only for an account with a catering designation. (Despite the title, the Catering Settings entry in Settings is not checked here.)

- **posting is forced on: no REQUEST bar even without the posting right @smoke** — passed (catering.spec.js:488)
  - Where: Tools → Catering · account: iPhone XRR
  - Steps: Open Catering → Check that no “REQUEST” bar (asking for the posting right) is shown → Check that the message box is shown → Check that the paper-clip and the gallery button are shown
  - Expected: Posting is always on in Catering: the message box and attachments show and nobody is asked to request the posting right. (The test uses the account’s own rights; it does not take the posting right away.)

- **with Home mounted too, every feed locator stays scoped to the Catering page @smoke** — passed (catering.spec.js:498)
  - Where: Tools → Catering (opens as a window; Home stays open behind it) · account: iPhone XRR
  - Steps: Open Catering in its window → Check that the Catering window holds exactly one message box → Check that the first message shows its “To :” line → Check that there is only one Catering page
  - Expected: With Home also open, the Catering window shows a single message box and its own messages. This mainly guards the automation’s lookups.

### Catering feed

- **post text with Enter: row, own sender, cleared composer, status-1 POST** — passed (unit-feed.suite.js:216)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “[e2e] post <timestamp>” in the message box → Press Enter → Check that the server accepts the message → Check that the message appears in the feed and the message box is empty again → Check that the message gets its options button (it is saved) and shows the sender’s name → Check that the message shows a “To :” receiver line
  - Expected: The message is posted once, appears in the feed under the own name with a “To :” receiver line, and the message box is cleared.

- **the mic slot becomes Send once there is text, and Send posts** — passed (unit-feed.suite.js:238)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Note the microphone button at the right of the message box → Type “[e2e] send-button <timestamp>” in the message box → Check that the microphone button has turned into the Send (paper-plane) button → Click Send → Check that the message appears, gets its options button, and the message box is empty again
  - Expected: Typing turns the microphone button into Send, and clicking Send posts the message.

- **Shift+Enter adds a newline; Enter keeps both lines in one bubble** — passed (unit-feed.suite.js:282)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “[e2e] shift-first <timestamp>” in the message box → Press Shift+Enter → Check that nothing was posted yet → Type “second-<timestamp>” on the new line → Press Enter → Check that one message appears holding both lines, and it gets its options button
  - Expected: Shift+Enter only adds a line break; Enter then sends both lines together as one message.

- **Ctrl+Enter sends** — passed (unit-feed.suite.js:301)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “[e2e] ctrl-enter <timestamp>” in the message box → Press Ctrl+Enter → Check that the message appears in the feed and gets its options button
  - Expected: Ctrl+Enter sends the message just like Enter.

- **a long message clips with Read More and expands back with Read Less** — passed (unit-feed.suite.js:327)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] clip <timestamp>” followed by “lorem ipsum ” 40 times and the word “TAIL-<timestamp>” at the end → Check that the message shows “Read More” and the TAIL word is hidden → Click “Read More” → Check that the TAIL word is now shown → Click “Read Less” → Check that the TAIL word is hidden again
  - Expected: A long message is cut short with “Read More”, opens fully on click, and folds back with “Read Less”.

- **HTML in a message renders as text, with no page error** — passed (unit-feed.suite.js:343)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “<img src=x onerror=alert(1)> <b>bold</b> [e2e] xss <timestamp>” → Check that the message shows “<b>bold</b>” as plain text → Check that no bold text and no broken image appear in the message → Check that no script error or alert popup occurs
  - Expected: HTML typed into a message is shown as plain text and never runs.

- **links: https becomes a safe link; www and javascript: do not** — passed (unit-feed.suite.js:355)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “https://example.com/e2e-<timestamp>” → Check that the address becomes a link that opens in a new tab, starts with https and cannot reach back to the app (no opener) → Post “see www.example.com e2e-www-<timestamp>” → Check that this message has no link → Post “javascript:alert(1) e2e-js-<timestamp>” → Check that this message has no link either
  - Expected: A full https address becomes a safe link that opens in a new tab; a bare “www.” address and a “javascript:” text stay plain text.

- **an e-mail address becomes a mailto link** — passed (unit-feed.suite.js:374)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “qa+e2e@example.com e2e-mail-<timestamp>” → Check that the e-mail address shows as a link that opens a new e-mail
  - Expected: An e-mail address in a message becomes a link to write an e-mail to it.

- **the own mention token renders as the own name** — passed (unit-feed.suite.js:382)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Look up the account’s own user ID → Post “@{{<own user ID>}} hi e2e-mention-<timestamp>” → Note the sender name on the new message → Check that the message text shows “@<sender name>” → Check that no curly braces are left in the message text
  - Expected: A mention code for the own user is shown as “@” plus the own name.

- **special characters round-trip exactly and markdown stays literal** — passed (unit-feed.suite.js:394)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “🎬 مرحبا Café ©®™ *bold* _it_ 12345 e2e-chars-<timestamp>” → Check that the message shows 🎬, مرحبا, Café, ©®™, *bold*, _it_ and 12345 exactly as typed → Check that nothing in the message is shown in bold or italics
  - Expected: Emoji, right-to-left text, accents and symbols come back unchanged, and markdown markers stay as typed.

- **an emoji from the picker is appended at the end and sent** — passed (unit-feed.suite.js:418)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “[e2e] emoji <timestamp>” in the message box → Press Home to move the cursor to the start of the text → Click the smiley (emoji) button and pick the first emoji → Check that the emoji was added at the end of the text, not at the cursor → Press Enter → Check that the message appears and gets its options button
  - Expected: The picked emoji is added at the end of the text wherever the cursor is, and the message sends with it.

- **an unsent draft survives a reopen and is cleared once sent** — passed (unit-feed.suite.js:449)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “[e2e] draft <timestamp>” in the message box without sending → Reload the page and go back to Catering → Check that the message box still holds the draft → Press Enter → Check that the message appears and gets its options button → Reload the page and go back to Catering again → Check that the message box is empty
  - Expected: An unsent draft is kept across a reload, and it is gone once it has been sent.

- **a posted message persists across a reopen** — passed (unit-feed.suite.js:466)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] persistent <timestamp>” and wait for its options button → Reload the page and go back to Catering → Check that the message is still in the feed
  - Expected: A posted message is still there after a reload.

- **a pending row shows the clock and no options button until the ack** — passed (unit-feed.suite.js:476)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Slow the connection so that sending takes about 4 seconds (the test holds the send request back) → Type “[e2e] pending <timestamp>” and press Enter → Check that the message appears at once with a clock icon and no options button → Wait for the send to finish → Check that the options button then appears
  - Expected: While a message is still being sent it shows a clock and no options button; the options button appears once the server has saved it.

- **a fast double Enter posts the message once** — passed (unit-feed.suite.js:501)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Type “[e2e] double-enter <timestamp>” in the message box → Press Enter twice at the same moment → Wait 5 seconds → Check that exactly one message with that text is in the feed
  - Expected: A double Enter sends the message only once.

- **a PDF uploads as a document bubble with its name, size and thumbnail** — passed (unit-feed.suite.js:556)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload” → Pick the file sample.pdf → In the preview, type “[e2e] pdf <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows “sample.pdf”, a “File Size - …” line and a PDF thumbnail
  - Expected: The PDF is posted as a document message with its name, its size and a thumbnail of its first page as a preview.

- **a xlsx document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload” → Pick the file sample.xlsx → In the preview, type “[e2e] sample.xlsx <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.xlsx” with a document icon
  - Expected: The Excel (xlsx) document is posted as a document message showing its file name and a document icon.

- **a docx document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload” → Pick the file sample.docx → In the preview, type “[e2e] sample.docx <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.docx” with a document icon
  - Expected: The Word (docx) document is posted as a document message showing its file name and a document icon.

- **a csv document uploads with its file name** — passed (unit-feed.suite.js:570)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload” → Pick the file sample.csv → In the preview, type “[e2e] sample.csv <timestamp>” in the “Type a message” box → Press Upload → Check that a new message shows the file name “sample.csv” with a document icon
  - Expected: The CSV document is posted as a document message showing its file name and a document icon.

- **a captioned document shows the caption and offers Edit Message** — passed (unit-feed.suite.js:581)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] doc-caption <timestamp>” → Check that the new message shows the caption → Open the message’s options menu → Check that it offers Edit Message and Download → Click Edit Message, change the text to “<caption> edited” and press Update → Check that the message shows the new caption with an “Edited” tag
  - Expected: A document with a caption shows the caption, and the caption can be edited; the message is then marked “Edited”.

- **an image uploads with its caption** — passed (unit-feed.suite.js:604)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Image Upload” → Pick the image red.png → In the preview, type “[e2e] image <timestamp>” in the “Type a message” box and press Upload → Check that a new message shows the image, the caption and a “File Size - …” line
  - Expected: The image is posted with its caption and its file size.

- **two images picked at once land as two rows** — passed (unit-feed.suite.js:615)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Image Upload” → Pick the two images red.png and blue.png together → Press Upload in the preview (it may read “Upload 2”) → Check that two new image messages appear → Check that each gets its options button and shows its image
  - Expected: Two images picked together are posted as two separate image messages.

- **an audio file uploads as an audio bubble** — passed (unit-feed.suite.js:631)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Audio Upload” → Pick the file sample.wav → Check that the preview names “sample.wav” → Press Upload → Check that a new message appears with a Play button and gets its options button
  - Expected: The audio file is posted as an audio message that can be played.

- **dropping an image on the feed opens the preview; Close posts nothing, Upload posts** — passed (unit-feed.suite.js:705)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Drag the image red.png from the computer and drop it on the message list → Check that the upload preview opens → Click Close and check that no new message appears → Drop red.png on the message list again → Press Upload in the preview → Check that a new image message appears, gets its options button and shows the image
  - Expected: Dropping an image opens the upload preview; closing it posts nothing, and Upload posts the image.

- **Image Reply posts a new image row carrying the caption** — skipped (unit-feed.suite.js:731)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload red.png through the paperclip → Image Upload with the caption “[e2e] image-reply-src <timestamp>” → Open the image message’s options menu and click Image Reply → In the preview, type “[e2e] image-reply <timestamp>” in the “Type a message” box and press Upload → Check that a new, separate image message appears with that caption and the image → Check that the new message is not nested as a comment
  - Expected: Image Reply posts a new image message of its own carrying the reply caption, not a comment.

- **an own text message offers the expected menu and nothing forbidden** — passed (unit-feed.suite.js:750)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] menu-text <timestamp>” → Open the message’s options menu → Check that it offers Reply, Edit Message, Forward In App, Delete, Forward to Remote Projects and Share (Translate, if shown, is ignored) → Check that it does not offer Read By, Copy, Distribute, Publish to Doc Distribution, Download or Image Reply → Close the menu
  - Expected: The own text message menu holds the expected actions and no Read By or other forbidden ones.

- **an own document without a caption adds Download and drops Edit Message** — passed (unit-feed.suite.js:765)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Document Upload”, pick sample.csv and press Upload without a caption → Wait for the new document message to get its options button → Open its options menu → Check that it offers Download → Check that it offers neither Edit Message nor Image Reply → Check that it does not offer Publish to Doc Distribution → Close the menu
  - Expected: An own document without a caption offers Download, but no Edit Message, Image Reply or Publish to Doc Distribution.

- **an own image adds Download and Image Reply** — passed (unit-feed.suite.js:787)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload red.png through the paperclip → Image Upload with the caption “[e2e] menu-image <timestamp>” → Open the image message’s options menu → Check that it offers Download and Edit Message but not Image Reply → Close the menu
  - Expected: An own captioned image offers Download and Edit Message; Image Reply is not offered on Catering for one’s own image.

- **an own audio adds Download and offers no Edit Message** — passed (unit-feed.suite.js:802)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the paperclip, then “Audio Upload”, pick sample.wav and press Upload → Wait for the new audio message to get its options button → Open its options menu → Check that it offers Download and no Edit Message → Close the menu
  - Expected: An own audio message offers Download and cannot be edited.

- **selection mode hides the options buttons and FABs and counts the selection** — passed (unit-feed.suite.js:833)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] selection <timestamp>” → Open its options menu and click Delete → Check that the bar reads “1 message(s) selected” and shows a Delete button → Check that no options buttons, no Gallery button and no message box are shown → Click the selected message → Check that the bar reads “0 message(s) selected” and the Delete button is gone → Click the message again and check that the bar reads “1 message(s) selected” → Leave selection mode with the bar’s X button → Check that the message box and the Gallery button are back and the message is still there
  - Expected: Selection mode hides the options buttons, the round buttons and the message box, counts the selected messages, and leaving it restores everything without deleting anything.

- **edit an own message: prefilled modal, Update, Edited tag, row moves last** — passed (unit-feed.suite.js:859)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] edit-original <timestamp>”, then “[e2e] edit-sibling <timestamp>” → Open the first message’s options menu and click Edit Message → Check that the “Edit Message” window holds the original text → Replace it with “[e2e] edit-updated <timestamp>” and press Update → Check that the message shows the new text with an “Edited” tag → Note whether the edited message moved to the bottom of the feed
  - Expected: The edit window opens with the current text, and after Update the message shows the new text marked “Edited”. The test only records whether the edited message moved to the bottom; it does not fail on it.

- **edit Cancel and Escape leave the message unchanged** — passed (unit-feed.suite.js:884)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] edit-keep <timestamp>” → Open its options menu, click Edit Message and type “discarded-<timestamp>” → Click Cancel → Check that the window closes and the message still shows the original text → Open Edit Message again and type “discarded-<timestamp>” → Press Escape → Check that the window closes and no message shows the discarded text
  - Expected: Leaving the edit window with Cancel or Escape keeps the message as it was.

- **unchanged text sends no PUT, empty text keeps the modal open, the box caps at 8,000** — passed (unit-feed.suite.js:903)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] edit-noop <timestamp>” → Open its options menu, click Edit Message and press Update without changing anything → Check that the window closes, nothing is sent to the server and no “Edited” tag appears → Open Edit Message again, replace the text with spaces and press Update → Check that the window stays open → Paste 8,100 characters into the edit box → Check that the box keeps only 8,000 characters → Click Cancel and check that no edit was sent at any point
  - Expected: Saving unchanged text sends no edit, an empty edit is refused with the window left open, and the edit box holds at most 8,000 characters.

- **a failing edit keeps the text and a refused delete keeps the row** — passed (unit-feed.suite.js:932)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] edit-fail <timestamp>” → Make the server fail every edit with a server error (the test fakes this) → Open Edit Message, change the text to “<original> nope” and press Update → Wait 3 seconds; if the edit window is still open, click Cancel → Check that the message still shows the original text and the “nope” text appears nowhere → Make the server refuse the delete with an error message (the test fakes this) → Open the options menu, click Delete, click Delete in the bar and answer Yes → Wait 3 seconds and check that the message is still in the feed → Leave selection mode if it is still on
  - Expected: When the edit fails the message keeps its original text, and when the server refuses the delete the message stays in the feed.

- **delete an own message through the selection flow (PUT delete/chats)** — passed (unit-feed.suite.js:977)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] delete-me <timestamp>” → Open its options menu and click Delete → Check that the bar reads “1 message(s) selected” → Click Delete in the bar → Check that the “Confirm Deletion” dialog asks “Are you sure you want to delete these messages…” → Click Yes → Check that the message disappears and exactly one delete was sent to the server
  - Expected: The message is removed after the selection bar and the “Confirm Deletion” dialog, with a single delete request.

- **delete can be backed out at both steps** — passed (unit-feed.suite.js:1000)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] delete-keep <timestamp>” → Open its options menu and click Delete → Leave selection mode with the bar’s X button → Check that the message is still there → Open its options menu, click Delete, then click Delete in the bar → Click No in the confirm dialog → Check that the dialog closes and the message is still there → Leave selection mode if it is still on
  - Expected: Backing out of a delete, either at the selection bar or at the confirm dialog, keeps the message.

- **multi-select delete removes two messages at once** — passed (unit-feed.suite.js:1016)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] multi-a <timestamp>” and “[e2e] multi-b <timestamp>” → Open the first message’s options menu and click Delete → Check that the bar reads “1 message(s) selected” → Click the second message → Check that the bar reads “2 message(s) selected” → Click Delete in the bar and answer Yes → Check that both messages disappear
  - Expected: Two selected messages are deleted together in one go.

- **a deleted message stays gone after a reopen** — passed (unit-feed.suite.js:1033)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] delete-reopen <timestamp>” → Delete it: options menu → Delete → Delete in the bar → Yes → Reload the page and go back to Catering → Check that the deleted message does not come back
  - Expected: A deleted message is still gone after a reload.

- **a reply nests as a comment inside the parent, which moves to the bottom** — passed (unit-feed.suite.js:1046)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] reply-target <timestamp>”, then “[e2e] reply-sibling <timestamp>” → Open the first message’s options menu and click Reply → Check that a quote of the message shows above the message box → Type “reply-<timestamp>” and press Enter → Check that the reply shows as a comment inside the first message, with a sender name and its own options button → Check that exactly one comment was sent and the reply text appears in only one message → Note whether the replied-to message moved to the bottom of the feed
  - Expected: A reply is shown once, as a comment nested inside the message it answers. The test only records whether that message moved to the bottom; it does not fail on it.

- **an own comment renders once — the socket echo must not duplicate it** — skipped (unit-feed.suite.js:1071)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] comment-once <timestamp>” → Open its options menu, click Reply, type “once-<timestamp>” and press Enter → Check that the comment appears inside the message → Wait 5 seconds for the live update to arrive → Check that the comment is shown exactly once
  - Expected: The own comment is shown once and is not doubled when the live update for it arrives.

- **the reply quote names the sender and can be dismissed** — passed (unit-feed.suite.js:1086)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] quote <timestamp>” → Note the sender name shown on the message → Open its options menu and click Reply → Check that the quote above the message box shows the sender name and the message text → Click the quote’s X button → Check that the quote disappears and the message box is empty
  - Expected: The reply quote names the sender and repeats the message, and its X closes it without leaving text behind.

- **the paperclip is hidden while replying** — passed (unit-feed.suite.js:1102)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] reply-clip <timestamp>” → Check that the paperclip is shown → Open the message’s options menu and click Reply → Check that the reply quote shows and the paperclip is gone → Close the quote with its X button → Check that the paperclip is back
  - Expected: Attachments cannot be added while replying: the paperclip hides during a reply and returns when the reply is dismissed.

- **a whitespace-only reply sends nothing and keeps the quote** — passed (unit-feed.suite.js:1114)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] reply-blank <timestamp>” → Open its options menu and click Reply → Type three spaces and press Enter → Wait about 2 seconds → Check that no comment was sent and the reply quote is still shown → Close the quote with its X button
  - Expected: A reply made only of spaces is not sent, and the reply stays open.

- **edit an own comment** — passed (unit-feed.suite.js:1129)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] comment-edit <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the new comment’s options menu → Check that it offers Edit Message and Delete but not Read By → Click Edit Message, change the text to “comment-<timestamp> edited” and press Update → Check that the comment shows the new text with an “Edited” tag
  - Expected: The own comment’s menu offers Edit Message and Delete (no Read By on Catering), and an edited comment shows its new text marked “Edited”.

- **delete an own comment: No keeps it, Yes removes it** — passed (unit-feed.suite.js:1153)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] comment-delete <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the comment’s options menu and click Delete → Check that the “Delete Comment” dialog opens and click No → Check that the comment is still there → Open the comment’s options menu again, click Delete and answer Yes → Check that the comment disappears from the message
  - Expected: Answering No keeps the comment; answering Yes removes it.

- **a comment offers Read By** — skipped (unit-feed.suite.js:1174)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] comment-readby <timestamp>” → Open its options menu, click Reply, type “comment-<timestamp>” and press Enter → Open the comment’s options menu and click Read By → Check that the “READ BY USERS” window opens → Close it with its X button
  - Expected: A comment’s Read By opens the “READ BY USERS” window, which closes again.

- **a failing comment POST shows an error toast** — passed (unit-feed.suite.js:1189)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] comment-fail <timestamp>” → Make the server refuse every new comment with an error message (the test fakes this) → Open the message’s options menu and click Reply → Type “comment-fail-<timestamp>” and press Enter → Check that an error message pops up → Check that no comment is added to the message → Close the reply quote if it is still shown
  - Expected: When the server refuses a comment, an error message pops up and no comment is added.

- **the Forward modal lists the destinations and its pickers send nothing** — passed (unit-feed.suite.js:1215)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering, post “[e2e] forward-look <timestamp>”, open its options menu and click Forward In App → Check that “Select Tab To Forward” lists Home Tab, Info Tab, Confidential Info Tab, Chat User, Group Chat and Email → Click Chat User and check that “Select Users” lists people → Search for a name that does not exist, check the list is empty, then clear the search and check the people come back → Click Select All and check the selected count equals the number of people listed → Click De-Select All and close the people picker with its X → Click Group Chat (reopen Forward In App first if the Forward window closed), search for a group that does not exist and check “No Data found” shows with no Send Group button → Close the “Select Group” window and the Forward window with their X buttons
  - Expected: The Forward window lists every destination, and its people and group pickers search, select and close without sending anything.

- **the Home Tab submenu lists Home units without Calendar or Call Sheet** — skipped (unit-feed.suite.js:1262)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering, post “[e2e] forward-home-tab <timestamp>”, open its options menu and click Forward In App → Hover over “Home Tab” in the Forward window (click it if the submenu does not open) → Check that the submenu lists Bulletin → Check that it lists neither Calendar nor Call Sheet → Press Escape and close the Forward window
  - Expected: Forwarding to the Home tab offers the Home units such as Bulletin, but never Calendar or Call Sheet.

- **forwarding into this feed creates a copy (both deleted)** — skipped (unit-feed.suite.js:1290)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] forward-self <timestamp>” → Open its options menu and click Forward In App → Click “Catering Tab” in the Forward window → Check that “Forward Successfully” pops up → Check that the feed now shows two messages with that text, both saved
  - Expected: Forwarding a message into its own feed adds a second copy of it there.

- **Forward to Remote Projects opens its modal and closes** — passed (unit-feed.suite.js:1316)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] forward-remote <timestamp>” → Open its options menu and click Forward to Remote Projects → Check that the “Forward to Remote Projects” window opens → Close it with its X button → Check that the window closes
  - Expected: The “Forward to Remote Projects” window opens and closes without forwarding anything.

- **Share opens Compose Email with every selected message; Cancel sends nothing** — passed (unit-feed.suite.js:1329)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] share-a <timestamp>” and “[e2e] share-b <timestamp>” → Open the first message’s options menu and click Share → Check that the bar reads “1 message(s) selected” → Click the second message and check that the bar reads “2 message(s) selected” → Click Share in the bar → Check that “Compose Email” opens holding the text of both messages → Click Cancel and check that the window closes → Leave selection mode if it is still on
  - Expected: Share puts every selected message into “Compose Email”, and Cancel closes it without sending.

- **Read By lists the two segments, searches and shows the empty state** — skipped (unit-feed.suite.js:1357)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering, post “[e2e] readby <timestamp>”, open its options menu and click Read By → Check that the “READ BY USERS” window shows the READ USER and UN-READ USER switches → Click UN-READ USER and check that people are listed → Check that there is no notify button beside the switch → Search for a name that does not exist and check “No Users Found.” shows → Clear the search and check that the people come back → Close the window with its X button
  - Expected: Read By shows who has and has not read the message, its search works and shows “No Users Found.” for no match, and there is no notify button.

- **Publish to Doc Distribution shows on a document, not on text; Cancel creates nothing** — skipped (unit-feed.suite.js:1382)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] publish-dd <timestamp>” → Open the document message’s options menu and click Publish to Doc Distribution → Check that the “Publish to Document Distribution” dialog opens → Click Cancel and check that the dialog closes
  - Expected: A document message offers Publish to Doc Distribution, and cancelling its dialog publishes nothing.

- **Download on a PDF emits a download event** — passed (unit-feed.suite.js:1399)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload sample.pdf through the paperclip → Document Upload with the caption “[e2e] download-pdf <timestamp>” → Open the document message’s options menu → Click Download → Check that the browser downloads a file whose name ends in .pdf
  - Expected: Download on a PDF message saves a .pdf file. (The test switches off the browser’s “Save as” picker so the file downloads directly.)

- **Download on an image emits a download event** — passed (unit-feed.suite.js:1417)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload red.png through the paperclip → Image Upload with the caption “[e2e] download-image <timestamp>” → Open the image message’s options menu → Click Download → Check that the browser downloads an image file (.png, .jpg, .jpeg or .webp)
  - Expected: Download on an image message saves an image file. (The test switches off the browser’s “Save as” picker so the file downloads directly.)

- **a PDF bubble opens the viewer without a Download button; a spreadsheet does not** — passed (unit-feed.suite.js:1435)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload sample.pdf through Document Upload with the caption “[e2e] view-pdf <timestamp>” → Click the PDF in the new message → Check that the document viewer opens with a Close button and no Download button → Click Close and check that the viewer closes → Upload sample.xlsx through Document Upload with the caption “[e2e] view-xlsx <timestamp>” → Click the spreadsheet in the new message and wait a moment → Check that no document viewer opens
  - Expected: Clicking a PDF message opens the document viewer without a Download button; clicking a spreadsheet message opens nothing.

- **an image bubble opens the preview, Escape closes it** — passed (unit-feed.suite.js:1456)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload red.png through Image Upload with the caption “[e2e] view-image <timestamp>” → Click the image in the new message → Check that the full-size image preview opens → Press Escape → Check that the preview closes
  - Expected: Clicking an image opens its preview, and Escape closes it.

- **search finds a message, highlights it and scrolls to it on click** — passed (unit-feed.suite.js:1471)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Post “[e2e] searchable <timestamp>”, then two more messages “[e2e] search-pad-0 …” and “[e2e] search-pad-1 …” → Click the magnifying-glass Search button next to the message box → Check that the search box has the cursor → Type the full “[e2e] searchable <timestamp>” text → Check that the drawer reads “1 Match” and the result shows the text with the match highlighted → Click the result → Check that the feed scrolls the message into view and the search drawer stays open → Click Close in the search drawer
  - Expected: Search finds the message, highlights the match, and clicking the result scrolls the feed to it while the drawer stays open.

- **search by the own sender name lists own rows** — passed (unit-feed.suite.js:1520)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] search-sender <timestamp>” → Note the sender name on the new message → Click the magnifying-glass Search button and type that sender name → Check that the first result shows that name as its sender → Click Close in the search drawer
  - Expected: Searching for the own name lists the own messages.

- **the Gallery Docs tab lists an uploaded document, filters, and offers a card menu** — passed (unit-feed.suite.js:1551)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Upload sample.pdf through Document Upload with the caption “[e2e] gallery-doc <timestamp>” → Click the round Gallery button, then the Docs tab → Check that sample.pdf is listed → Type a nonsense word in the Gallery search and check “No matching documents” shows → Clear the search and check that sample.pdf is listed again → Click the small down arrow on the sample.pdf card → Check that its menu offers Forward In App → Press Escape twice and check that the Gallery closes
  - Expected: The Docs tab lists the uploaded document, its search filters the list, and each card has a menu with Forward In App.

- **a posted row shows avatar, sender · designation and a formatted timestamp** — passed (unit-feed.suite.js:1583)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] sender-line <timestamp>” → Check that the new message shows an avatar and the sender’s name → If a designation is shown, check that a “·” separates it from the name → Check that the time reads like “Nov 16, 2026 at 10:05 AM”
  - Expected: A posted message shows the avatar, “Name · Designation” and a timestamp in the form “Mon DD, YYYY at hh:mm AM/PM”.

- **the scroll-to-bottom button returns to the latest message** — passed (unit-feed.suite.js:1623)
  - Where: Tools → Catering (it opens on the Breakfast tab), with at least 8 messages · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] bottom-anchor <timestamp>” → Scroll the feed up a long way → Check that the down-arrow scroll-to-bottom button appears → Click the down-arrow button → Check that the posted message is back in view and the button disappears
  - Expected: The scroll-to-bottom button brings the latest message back into view and then hides.

- **another member's message offers no Edit Message** — passed (unit-feed.suite.js:1638)
  - Where: Tools → Catering (it opens on the Breakfast tab), with a message from another member · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] whoami <timestamp>” to see the own sender name → Find a saved message from another member, scrolling up if needed → Open that message’s options menu → Check that it does not offer Edit Message → Close the menu
  - Expected: A message sent by someone else cannot be edited.

- **History opens with its heading, alert and search, and comes back** — skipped (unit-feed.suite.js:1661)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Click the round History (clock) button → Check that the “Catering History” title shows with the note “Records of deleted messages.” → Check that a Search button shows and there is no message box and no History button → Click Back (or the browser’s back button if there is no Back) → Check that the feed with its message box is back
  - Expected: History opens as a read-only list of deleted messages titled “Catering History”, and Back returns to the feed.

- **a deleted message shows in History; text rows have no menu, PDFs offer View / Download / Print** — skipped (unit-feed.suite.js:1681)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering and post “[e2e] history-text <timestamp>” → Upload sample.pdf through Document Upload with the caption “[e2e] history-pdf <timestamp>” → Delete both messages through options menu → Delete → Delete in the bar → Yes → Click the round History (clock) button and wait for “Catering History” → Check that the deleted text message is listed without an options button → Check that the deleted PDF message is listed → Open the PDF message’s options menu → Check that it offers View, Download and Print → Close the menu
  - Expected: Deleted messages appear in History; a text message has no menu there, and a PDF offers View, Download and Print.

- **a message sent while offline errors and can be resent** — passed (unit-feed.suite.js:1709)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Switch the browser offline → Type “[e2e] offline-resend <timestamp>” and press Enter → Check that the message appears with a red retry icon → Switch the browser back online → Click the red retry icon, then “Resend Message” → Check that the red icon goes away and the message gets its options button
  - Expected: A message sent offline is marked as failed, and Resend Message posts it once the connection is back.

- **a POST answered with status 0 marks the row as failed** — passed (unit-feed.suite.js:1726)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Make the server refuse new messages with an error message (the test fakes this) → Type “[e2e] status-zero <timestamp>” and press Enter → Check that the message appears with a red retry icon → Let the server accept messages again → Click the red retry icon, then “Resend Message” → Check that the message gets its options button
  - Expected: A message the server refuses is marked as failed, and Resend Message posts it once the server accepts again.

- **a failed message can be deleted from the retry menu without a server call** — passed (unit-feed.suite.js:1750)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Make every send fail with a network error (the test fakes this) → Type “[e2e] error-delete <timestamp>” and press Enter → Check that the message shows a red retry icon → Click the red retry icon, then Delete → Check that the selection bar shows, click Delete in it and answer Yes → Check that the failed message disappears → Check that no delete was sent to the server
  - Expected: A failed message can be removed from its retry menu, and the removal stays on this device without a server call.

- **a failing chat page GET leaves the composer usable and nothing crashes** — passed (unit-feed.suite.js:1783)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Make the server fail every request for the feed’s messages with a server error (the test fakes this) → Open Catering → Check that the “Something went wrong.” crash screen does not show → Check that the message box can still be typed in → Check that no loading indicator stays on screen
  - Expected: When the messages cannot be loaded the feed does not crash, stops loading, and the message box stays usable.

- **a message posted from a second context appears live** — passed (unit-feed.suite.js:1802)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering → Open Catering in a second, separate browser session signed in to the same account → In the second session, post “[e2e] realtime <timestamp>” and wait for its options button → Check that the message appears in the first session without a reload → Close the second session
  - Expected: A message posted from another session shows up live in the open feed.

- **the Edit modal renders inside the tool window body** — passed (unit-feed.suite.js:1838)
  - Where: Tools → Catering (it opens on the Breakfast tab) · account: iPhone XRR
  - Steps: Open Catering in the window layout → Post “[e2e] portal <timestamp>” → Open its options menu and click Edit Message → Check that the “Edit Message” window opens inside the Catering window, not over the whole screen → Click Cancel
  - Expected: The Edit Message window opens inside the feed’s own window.

### Catering shell and units

- **an own post carries the "To :" line with the member / caterer audience** — passed (catering.spec.js:294)
  - Where: Tools → Catering, Breakfast tab · account: iPhone XRR
  - Steps: Open Catering on the Breakfast tab → Type “[e2e] receiver …” in the message box and press Enter → Wait until the message is sent (its options arrow appears) → Check that the message has a line starting “To :” → Check that it reads “All caterer” for a crew member, or “All users” for a caterer → Check that the sender’s name is shown
  - Expected: Your own message shows your name and who it went to: “To : All caterer” from a crew member, “To : All users” from a caterer.

- **neither a message nor a comment offers Read By** — failed (catering.spec.js:315)
  - Where: Tools → Catering, Breakfast tab · account: iPhone XRR
  - Setup: Posts “[e2e] no-readby …” in Breakfast.
  - Steps: Open the message’s options arrow and click Reply → Type “comment-…” and press Enter → Check that the comment appears under the message → Open the comment’s options menu → Check that it has no “Read By” item → Close the menu
  - Expected: A comment’s menu does not offer Read By. (Despite the title, only the comment menu is checked here.)
  - Web error: Test timeout of 270000ms exceeded.

- **CAT-G02..G07 the Select Users modal: options view, Select All, the user list, search, reopen** — skipped (catering.spec.js:397)
  - Where: Tools → Catering (as a catering-department member) · account: iPhone XRR
  - Steps: Click “Select User” and check the “Select Users” dialog offers “Select All” and “Select User”; close it with × → Click “Select User” again, then “Select All” → Check that the dialog closes and the button reads “All Users…” → Open the dialog again and click “Select User” → Check that the user list loads and does not include your own name → Type a made-up name in “Search User…” and check that it says “No Users Found” → Close the dialog, open it again and check it starts on the “Select All” / “Select User” choice; close it
  - Expected: The Select Users dialog offers Select All or a user list; Select All sets “All Users”, the list leaves you out and searches, and reopening starts on the choice again.

- **CAT-O02..O04 Catering Settings lists the units, locks the system ones and searches** — skipped (catering.spec.js:438)
  - Where: Settings → Catering Settings (as a catering-department member) · account: iPhone XRR
  - Steps: Open Settings → Click “Catering Settings” → Check that the dialog is titled “Catering Settings” and its count (“N Units”) matches the number of Catering tabs → Check that the edit and delete buttons of Breakfast, Lunch and Dinner are disabled → Type a made-up name in the search box → Check that it says “No results found” and “Try a different search term.”
  - Expected: Catering Settings lists every unit with the right count, locks the three meal units against edit and delete, and its search reports no match.

- **create a custom unit from Catering Settings, then delete it** — skipped (catering.spec.js:460)
  - Where: Settings → Catering Settings (as a catering-department member) · account: iPhone XRR
  - Steps: Click “Create New Unit” → Type “e2e unit …” in Unit Name and press Save → Check that the save succeeds and the new unit is listed → Click the new unit’s edit (pencil) button, clear the name and press Save → Check that a toast says the name cannot be empty → Press Cancel → Click the unit’s delete (bin) button and answer Yes → Check that the unit is gone from the list
  - Expected: A custom unit can be created and deleted, and renaming it to an empty name is refused.

- **a POST answered with status 0 marks the row as failed and Resend recovers it** — passed (catering.spec.js:509)
  - Where: Tools → Catering, Breakfast tab · account: iPhone XRR
  - Setup: The test makes the server refuse the send (it answers with a failure).
  - Steps: Type “[e2e] status-zero …” in the message box and press Enter → Check that the message shows the red retry icon → Stop faking the server → Click the red icon and choose “Resend Message” → Check that the message is sent this time (its options arrow appears) → Delete the message (Delete, Delete, Yes) and check it is gone
  - Expected: A refused send marks the message as failed, and Resend Message sends it.

- **a caterer sends to one user (receiver in the POST) and to All Users** — skipped (catering.spec.js:541)

- **the receiver leaks across tabs and across the Accounts window (global state)** — skipped (catering.spec.js:542)

- **no Reply on caterer-authored messages** — skipped (catering.spec.js:543)

- **non-admin delete toast on others' rows; admin deletes account B's row** — skipped (catering.spec.js:544)

- **duplicate unit name toast; rename through the "+" path collapses the modal** — skipped (catering.spec.js:545)

- **CAT-P01..P07 realtime posts, edits, comments, badges and unit changes from a second account** — skipped (catering.spec.js:546)

- **no view access bounces; no download right opens the permission request** — skipped (catering.spec.js:547)

- **tab overflow with more than four units in a small window** — skipped (catering.spec.js:548)

- **with more units than fit, the tabs that do not fit are listed in the tab strip's "more" menu @smoke** — passed (catering.spec.js:929)
  - Where: Tools → Catering, in a 900 px wide browser window · account: iPhone XRR
  - Setup: The test adds six made-up units (“E2e Overflow Unit 1” … “6”) to the unit list the server sends; nothing is created.
  - Steps: Open Catering in a narrow browser window → Check that there is one tab per unit, the six made-up ones included → Check that the tab strip shows its “more” (…) button → Point at (or click) the “more” button → Check that its menu lists the tabs that do not fit, all of them real tab names, “E2e Overflow Unit 6” among them → Press Escape
  - Expected: When the units do not fit, the tabs that are cut off can still be reached from the tab strip’s “more” menu.

### Catering — two users (slot b as a caterer)

- **a caterer sends to one member (who sees "To : <name>" live), an image to that member, then a message to All Users** — passed (catering.spec.js:1056)
  - Where: Tools → Catering, Breakfast tab, open for both accounts · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, click “Select User”, then “Select User”, search “iPhone” and click iPhone XRR → Check that the button now reads “iPhone XRR...” → Send “[e2e] to-member …” and check the message is addressed to iPhone XRR only and reads “To : iPhone XRR (…)” → As iPhone XRR, without reloading, check the message appears with the same “To : iPhone XRR” line → As Lava Device, upload red.png through Image Upload with the caption “[e2e] image-to-member …” and check it is also addressed to iPhone XRR → Click “Select User”, then “Select All”, and check the button reads “All Users...” → Send “[e2e] to-all …” and check it goes to no single person and reads “To : All users”
  - Expected: A caterer can address one member (text and attachments) or everyone; the chosen member sees the message live with their name in the “To :” line.

- **a receiver picked in Breakfast is not carried into Lunch** — bug · known bug Catering keeps ONE receiver for all its tabs (unitChat.receiverUserData): the person picked in Breakfast stays picked in Lunch and gets the next Lunch message,Catering keeps ONE receiver for all its tabs (unitChat.receiverUserData): the person picked in Breakfast stays picked in Lunch and gets the next Lunch message (catering.spec.js:1108)
  - Where: Tools → Catering, Breakfast tab · account: Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, click “Select User”, then “Select User”, and pick iPhone XRR → Check that the button reads “iPhone XRR...” → Click the Lunch tab → Check that the button reads “Select User” again
  - Expected: Each meal tab starts without a chosen person. Known bug: the person picked in Breakfast stays picked in Lunch (one receiver is shared by all tabs), so the next Lunch message would go to them.
  - Web error: Error: expect(locator).toHaveText(expected) failed

- **closing an Accounts window leaves the receiver picked in the Catering window alone** — skipped (catering.spec.js:1122)
  - Where: Tools → Catering in the Windowed view, then Tools → Accounts in a second window · account: Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, in the Catering window click “Select User”, then “Select User”, and pick iPhone XRR → Click Tools in the sidebar and open Accounts from its tile → Wait for the Accounts message box, then close the Accounts window → Click the Catering tab in the window bar → Check that the button still reads “iPhone XRR...”
  - Expected: Closing the Accounts window does not touch the person chosen in Catering. Known bug: Accounts and Catering share one chosen-person setting, and closing Accounts clears the one picked in Catering.

- **a caterer's message offers no Reply — not to the caterer, not to a member** — passed (catering.spec.js:1143)
  - Where: Tools → Catering, Breakfast tab, open for both accounts · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, send “[e2e] caterer-menu …” → Open the message’s options menu → Check that it has no Reply but has Edit Message, Delete, Forward In App, Forward to Remote Projects and Share → As iPhone XRR, find the same message (it arrives without a reload) and open its options menu → Check that it has neither Reply nor Edit Message, and has Delete, Forward In App and Share
  - Expected: Nobody can reply to a message written by a caterer — not the caterer, not a member; an admin member can still delete or forward it.

- **an admin deletes the caterer's message and it disappears for the caterer too** — passed (catering.spec.js:1177)
  - Where: Tools → Catering, Breakfast tab, open for both accounts · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, send “[e2e] admin-deletes …” → As iPhone XRR (a project admin), wait for the message to appear → Open its options menu, choose Delete, press Delete in the selection bar and answer Yes → Check that the message is gone for iPhone XRR → Check that it disappears for Lava Device too, without a reload
  - Expected: An admin can delete another person’s catering message, and the deletion reaches its author live.

- **a caterer's new, edited and deleted message reach a member's open Catering live** — passed (catering.spec.js:1195)
  - Where: Tools → Catering, Breakfast tab, open for both accounts · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, send “[e2e] live …” → As iPhone XRR, check the message appears without a reload → As Lava Device, edit it (Edit Message) to “[e2e] live … edited” and press Update → As iPhone XRR, check the new text appears with the “Edited” tag → As Lava Device, delete the message (Delete, Delete, Yes) → As iPhone XRR, check the message disappears
  - Expected: A caterer’s post, edit and delete all reach a member’s open Catering screen without a reload.

- **a caterer's reply to a member's message reaches the member live** — passed (catering.spec.js:1217)
  - Where: Tools → Catering, Breakfast tab, open for both accounts · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As iPhone XRR, send “[e2e] to-be-answered …” → As Lava Device, open Catering and find that message → Choose Reply from its options menu, type “[e2e] answer …” and press Enter → As iPhone XRR, check the reply appears under the message without a reload
  - Expected: A caterer’s reply to a member’s message reaches the member live.

- **a caterer's post in Lunch badges the member's Lunch tab; opening it shows "New Messages" and clears the badge** — passed (catering.spec.js:1244)
  - Where: Tools → Catering, iPhone XRR on the Breakfast tab, Lava Device on the Lunch tab · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group. If iPhone XRR’s Lunch tab already has an unread count, the test opens Lunch first to read it.
  - Steps: As iPhone XRR, stay on Breakfast → As Lava Device, click the Lunch tab, send “[e2e] badge …” and check the send is accepted → As iPhone XRR, check that the Lunch tab shows a red unread count → Click the Lunch tab → Check that the message is there with a “New Messages” divider → Check that the Lunch tab’s unread count goes away
  - Expected: A new message in another meal tab shows as an unread count on that tab; opening the tab marks where the new messages start and clears the count.

- **a caterer's post raises the Catering tile's unread count on a member's Tools page** — passed (catering.spec.js:1268)
  - Where: Tools (the tool tiles), for iPhone XRR; Tools → Catering, Breakfast tab, for Lava Device · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As iPhone XRR, open Tools and note the unread number on the Catering tile (none counts as 0), once it stops changing → As Lava Device, send “[e2e] tile-badge …” in Catering → As iPhone XRR, check that the Catering tile’s unread number goes up, without a reload
  - Expected: A new catering message raises the Catering tile’s unread count on the member’s Tools screen.

- **the "New message received" toast for a catering message, and its click to the unit — it arrives only as a Firebase push, which a headless browser with service workers blocked never receives** — skipped (catering.spec.js:1298)
  - Where: Any screen outside Catering · account: iPhone XRR and Lava Device
  - Steps: Not automated: this test is a placeholder and never runs; the steps are its intent → As Lava Device (a caterer), send a catering message → As iPhone XRR, outside Catering, check that a “New message received” notice pops up → Click it and check that Catering opens on that unit
  - Expected: Not automated: the notice comes only as a browser push notification, which the test browser cannot receive. Intended: the notice appears and leads to the unit.

- **a unit the caterer creates, renames and deletes in Catering Settings follows on a member's open tabs live** — passed (catering.spec.js:1303)
  - Where: Tools → Catering for iPhone XRR; Settings → Catering Settings for Lava Device · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As iPhone XRR, open Catering and leave it open → As Lava Device, open Settings, click “Catering Settings”, click “Create New Unit”, type “e2e live unit …” and press Save → As iPhone XRR, check that a tab with that name appears without a reload → As Lava Device, click the unit’s edit (pencil) button, type “e2e live renamed …” and press Save → As iPhone XRR, check that the tab now shows the new name and the old name is gone → As Lava Device, click the unit’s delete (bin) button and answer Yes → As iPhone XRR, check that the tab disappears
  - Expected: Creating, renaming and deleting a catering unit show up on a member’s open Catering tabs live.

- **creating a unit with a name already in use is refused with "Catering unit name is already in use."** — failed (catering.spec.js:1353)
  - Web error: Error: the refusal is explained to the user

- **Edit on a unit in the "+" dialog turns the row into an editor and keeps the dialog open** — bug · known bug Catering "+" dialog: Edit on a unit closes the whole dialog (startRename clears newUnit, the only flag keeping that dialog open) instead of turning the row into an editor,Catering "+" dialog: Edit on a unit closes the whole dialog (startRename clears newUnit, the only flag keeping that dialog open) instead of turning the row into an editor (catering.spec.js:1378)
  - Where: Tools → Catering, the orange “+” next to the meal tabs · account: Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, click the orange “+” next to the tabs → Type “e2e plus unit …” in Unit Name and press Save; the dialog closes → Click the “+” again and find the new unit in the list → Click its edit (pencil) button → Check that the row turns into a name field and the dialog stays open
  - Expected: Edit in the “+” dialog lets you rename the unit in place. Known bug: clicking Edit closes the whole dialog instead.
  - Web error: Error: expect(locator).toBeVisible() failed

### Catering — slot b without admin rights (User Management)

- **a member without admin rights who picks another person's message in delete mode is told "You have no permission to delete other user message"** — skipped (catering.spec.js:1380)

- **a member without the Catering Download right who picks Download is offered the admin request instead of a file** — skipped (catering.spec.js:1417)

- **without the Catering View right the tile is gone and the Catering link does not open it** — passed (catering.spec.js:1500)
  - Where: Tools (the tool tiles), then the Catering link · account: Lava Device
  - Setup: iPhone XRR opens Settings → Admin Settings → User Management, switches off Lava Device’s Admin switch and, in Lava Device’s Posting Rights (Tools section), sets Catering View off.
  - Steps: As Lava Device (not an admin, no view right), open Tools → Check that there is no Catering tile → Open the Catering link directly → Wait about 40 seconds for the app to check the rights → Check that Catering does not stay open (the window closes, or the Tools page shows instead)
  - Expected: Without the view right, Catering is neither offered on the Tools page nor reachable through its link.

### Catering — two users (slot b as a caterer)

- **a second unit with a name already in use is refused, and the notice says so ("Unit already exists")** — passed (catering.spec.js:1353)
  - Where: Settings → Catering Settings · account: Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group.
  - Steps: As Lava Device, open Settings and click “Catering Settings” → Click “Create New Unit”, type “e2e dup unit …” and press Save; check it is listed once → Click “Create New Unit” again, type the same name and press Save → Check that the save is refused, and that the notice that pops up says the unit already exists (“Unit already exists” on dev) → Check that the list still shows that name only once
  - Expected: A second unit with a name already in use is refused, and the user is told it already exists.

- **without admin rights, picking another person's message in delete mode is refused with "You have no permission to delete other user message"** — passed (catering.spec.js:1386)
  - Where: Tools → Catering, Breakfast tab, open for both accounts · account: iPhone XRR and Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group. Then iPhone XRR opens Settings → Admin Settings → User Management, switches off Lava Device’s Admin switch and, in Lava Device’s Posting Rights (Tools section), sets Catering View on.
  - Steps: As iPhone XRR, send “[e2e] foreign …” → As Lava Device (a caterer, now not an admin), open Catering and find that message → Send “[e2e] own …”, open its options menu and choose Delete → Check the selection bar reads “1 message(s) selected” → Click iPhone XRR’s message → Check that the toast “You have no permission to delete other user message” appears and the bar still reads “1 message(s) selected” → Leave the selection, then delete Lava Device’s own message
  - Expected: Someone who is not an admin cannot pick another person’s message for deletion and is told why.

- **without the Catering Download right, Download offers the admin request instead of a file** — passed (catering.spec.js:1424)
  - Where: Tools → Catering, Breakfast tab · account: Lava Device
  - Setup: Before the first case, iPhone XRR opens Settings → Admin Settings → User Management, edits Lava Device (pencil) and moves it to the Catering department with a catering designation, so Lava Device is a caterer for the whole group. Then iPhone XRR opens Settings → Admin Settings → User Management, switches off Lava Device’s Admin switch and, in Lava Device’s Posting Rights (Tools section), sets Catering View on and Download off.
  - Steps: As Lava Device (a caterer, not an admin, no download right), open Catering → Upload red.png through Image Upload with the caption “[e2e] no-download …” → Open the image’s options menu and choose Download → Check that a box says “You do not have Downloading Rights on this Unit…” and answer Yes → Check that the “Send Admin Request” list opens, then close it with × (never press Send Request) → Check that no file was downloaded → Delete the image message
  - Expected: Without the download right, Download offers to ask an admin instead of saving the file.

## Sheet bugs (5)

- **CAT-01** Chat search for one or two letters shows no result text — Low · Web: Not checked, Android: Not affected
  - Steps: 1. Open the Catering chat. 2. Search for "Q".
  - Expected: "No Matches Found" (or the match count) is shown.
  - Actual: The result label stayed hidden for short queries.

- **CAT-02** Unsent text is lost when you switch between Catering units — Medium · Web: Not checked, Android: Open
  - Steps: 1. On Dinner, type a message without sending it. 2. Tap Lunch, then Breakfast. 3. Tap Dinner again.
  - Expected: The draft is still in Dinner's composer, as web keeps it per unit.
  - Actual: The composer is empty and the text is gone, with no warning.

- **CAT-03** Update in Edit Message with no change still marks the message "Edited" — Low · Web: Not checked, Android: Open
  - Steps: 1. Long-press your own message → Edit. 2. Change nothing and tap Update.
  - Expected: The dialog closes and the message stays as it was.
  - Actual: The message now shows "Edited" next to its time, although nothing changed.

- **CAT-04** Android stops at 2,000 characters where web allows 8,000 — Low · Web: Not checked, Android: Open
  - Steps: 1. Paste 2,100 characters into the composer. 2. Tap Send.
  - Expected: The same limit as web (8,000).
  - Actual: "There is only 2000 Characters limit allowed to send", and nothing is sent; the text stays in the composer.

- **CAT-05** Gallery Docs shows file size as a bare number with no unit — Low · Web: Not checked, Android: Open
  - Steps: 1. Long-press a message → Gallery. 2. Open the Docs tab.
  - Expected: A readable size, such as 771 KB.
  - Actual: Sizes read "302" and "789993" with no unit.
