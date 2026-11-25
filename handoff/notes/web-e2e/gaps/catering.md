# Catering: web E2E vs Android Appium gap analysis

Scope (2026-11-23). Compares the 148 web cases in `web-e2e/catering.md` (Zillit E2E report of 2026-11-17, dev.zillit.com) with the Android suite `CateringTest.java` (20 own cases) plus the 15 shared chat cases inherited from `ToolChatBaseTest.java` and the page objects `CateringPage.java` / `HomeChatPage.java`. Whether a feature exists on Android was checked in the app source `Zillit-Android/app/src/main`. Nothing was run. Web refs are `web:<line>` in `web-e2e/catering.md`. Short names for source files used below:

- CA = `java/com/zillit/zillitapp/bottomNav/tools/catering/CateringActivity.kt`
- CVM = `java/com/zillit/zillitapp/bottomNav/tools/viewmodel/CateringVm.kt`
- KB = `java/com/zillit/zillitapp/baseUtils/CommonKeyboardFragment.kt`
- OH = `java/com/zillit/zillitapp/utils/OptionsHandler.kt`
- TVH = `java/com/zillit/zillitapp/bottomNav/home/adapter/holder/TextVH.kt`
- LIB = `java/com/zillit/zillitapp/chatAndGroupChat/library/HomeChatLibraryActivity.kt`
- ACS = `java/com/zillit/zillitapp/bottomNav/settings/activity/AccAndCaterSettingsActivity.kt`
- MUF = `java/com/zillit/zillitapp/bottomNav/settings/admin/units/MainUnitFragment.kt`
- FWD = `java/com/zillit/zillitapp/forwardPage/ForwardPage.kt`
- `activity_catering.xml`, `emoji_keyboard_layout.xml`, `chat_text_layout.xml` = `res/layout/...`; `strings.xml` = `res/values/strings.xml`
- CT = `CateringTest.java`, TCB = `ToolChatBaseTest.java` (Android-automation, `src/test/java/com/zillit/automation/tests/tools/`)

## Summary

| Class | Web tests | Groups |
|---|---|---|
| COVERED | 24 | 20 rows (several are only partly covered, see notes) |
| MISSING | 85 | 33 groups (M1 to M33; M32 needs network fault injection and is deferred) |
| CONFLICT | 5 | 3 (Read By, attachment picker assertion, unit-delete assumption) |
| WEB-ONLY | 34 | 27 rows |
| Total | 148 | |

Top missing cases (priority order):
1. M1 Failed send and Retry: post offline, check the Retry chip, go back online, tap Retry (web:557, 562, 616).
2. M24 Delete rights: as a non-admin, picking someone else's message gives "You have no permission to delete other user message."; plus the admin path, where Android differs from web (web:628, 671, 724, 742).
3. M23 The receiver leaks across meal tabs: a caterer picks a user in Breakfast and the pick is still there in Lunch. This is a web bug, and the Android source has it too (web:624, 652).
4. M5 and M6 Attachments: send Document (pdf/xlsx/docx/csv), Image with caption, multi-image and Audio, and check each bubble (web:302 to 337).
5. M2 and M9 Composer and edit limits: the 2,000-character snackbar, empty edit refused, unchanged edit (sheet bugs CAT-03 and CAT-04) (web:44, 387).
6. M3 Draft per unit or across reopen (sheet bug CAT-02) (web:194, 282).
7. M10 and M11 The exact long-press menus for own text, others' text, document, image and audio, with Read By, Copy, Print and Forward to Remote Projects absent (web:352 to 367, 542).
8. M4 Search: find by text, find by sender, empty or space-only input (web:104, 517, 522).
9. M15 and M14 Delete variants (No at the dialog, back out of selection, multi-select, still gone after reopen) and the selection bar (web:372, 402 to 412).
10. M16 and M17 Reply composer (quote shown and dismissed, attachment hidden, space-only reply) and comment edit or delete (web:427 to 447).

---

## MISSING (85 web tests, 33 groups)

In every proposed case, the "Expected" line is Android's behaviour as read from the source. Where it differs from web, the difference is flagged.

### M1 Failed send and Retry (3)
- Web: "a message sent while offline errors and can be resent" (web:557); "a POST answered with status 0 marks the row as failed" (web:562); "a POST answered with status 0 ... Resend recovers it" (web:616).
- Why: this is the only defence against losing messages, and nothing on Android covers it. `HomeChatPage.messageFailed()` (line 293) already exists but no test calls it.
- Proposed: "Offline send shows Retry, and Retry posts after reconnect".
  - Steps: switch the network off with `driver.setConnection(AIRPLANE)` or `adb shell svc wifi/data disable` (do not use a uiautomator dump mid-run). Type a marker and tap `ivSendMessage`. Check the row shows `btnTryAgain` ("Retry", chat_text_layout.xml:149,172). Turn the network back on and tap Retry. Wait with `waitMessageSent`.
  - Expected: offline saves the row with status -1 (CVM:578-581), which shows `btnTryAgain` (TVH:211-216). Tapping Retry while still offline shows the snackbar "Internet connection lost. Please check your internet connection and try again." (CA:1193-1196, strings.xml:744). Once online, Retry resends (CA:1309-1344) and `ivClock` and `btnTryAgain` go away.
  - The server-refusal variants (web:562 and 616, status 1, CVM:554-568) need a stubbed backend. Defer them with M32.

### M2 Composer character limit (1)
- Web: "the composer caps at 8,000 characters and shows the counter from 7,200" (web:44).
- Android has no counter. The limit is `Constants.TEXT_LIMIT = 2000` (`utils/Constants.kt:733`), enforced only when Send is tapped (KB:154-163). This is sheet bug CAT-04.
- Proposed: "Over-limit text is refused with the 2,000-character snackbar".
  - Steps: `etMessage.setValue` with 2,001 characters, then tap `ivSendMessage`.
  - Expected: snackbar "There is only 2000 Characters limit allowed to send" (strings.xml:530). Nothing posts and the text stays. Also check that exactly 2,000 characters does post.
  - If product aligns with web at 8,000, change the expected value then.

### M3 Draft per unit and across reopen (2)
- Web: "a draft typed in Breakfast survives a visit to Lunch" (web:194); "an unsent draft survives a reopen and is cleared once sent" (web:282).
- Android: KB restores `AppHelper.userCacheMessage[unitId]` on `registerListener` (KB:125-130, called on every tab change at CA:477). But Catering only writes the cache when `currentUnitInfo == null` (CA:877-898), which never happens once a unit is open. So the draft is lost (sheet bug CAT-02, Android Open).
- Proposed: "Draft stays with its meal tab".
  - Steps: on Breakfast, type a marker without sending. Select Lunch and check `etMessage` is empty. Select Breakfast again.
  - Expected (web parity): `etMessage` holds the marker. On the current Android build this fails, which documents CAT-02. Run a second variant that leaves Catering with back, reopens it from Tools, and checks the same.

### M4 Search (3)
- Web: "search edge cases" (web:104); "search finds a message, highlights it and scrolls to it" (web:517); "search by the own sender name lists own rows" (web:522).
- Android: `ivSearch` (emoji_keyboard_layout.xml:278) opens `laySearch` with `edittext_search`, `tvCount`, `top_button` and `bottom_button` (activity_catering.xml:101-134). Search matches message text, comments and the sender name (`HomeChatAdapter.kt:694-719`).
- Proposed: "Search finds a posted marker and the own name".
  - Steps: post a marker, tap `ivSearch`, type the marker and press IME Search.
  - Expected: `tvCount` is at least 1 and the row is scrolled into view. Then search the own display name: `tvCount` is at least 1. Then a nonsense word: `tvCount` = "0".
  - Edge case: IME Search on empty input gives the snackbar "Please type text to search." (CA:2128-2129, strings.xml:776). Note that a space-only query passes the `isEmpty()` check, is trimmed to "", and matches every row (CA:2128-2133). Web shows "Please write in search box" instead. Assert web parity so this gap gets reported.
  - Close the search with `btnBack` (CA:1649-1658).

### M5 Document uploads (4)
- Web: PDF bubble with name, size and thumbnail (web:302); xlsx, docx and csv uploads (web:307, 312, 317).
- Android: the Catering picker offers Document (CA:949-956, label "Document" at strings.xml:135).
- Proposed: "Upload a PDF / XLSX / DOCX / CSV through Document".
  - Steps: push the fixtures to `/sdcard/Download`, tap `ivAttachment`, then `tapAttachmentOption("Document")`. Use `pickSystemFileByName("sample.pdf")`. If the preview opens, set a caption with `setPreviewCaption`, then `tapSendBtn`.
  - Expected: `docBubbleCount()` goes up by 1, the file name is shown, and `waitAttachmentSent` is true. Parametrise over the 4 files.

### M6 Image with caption, multiple images, audio (3)
- Web: "an image uploads with its caption" (web:327); "two images picked at once land as two rows" (web:332); "an audio file uploads as an audio bubble" (web:337).
- TCB `sendImageMessage` (TCB:370) sends one image with an empty caption only.
- Proposed:
  - (a) Image Gallery with caption "qa-cap-<ts>": the image bubble and the caption text are shown.
  - (b) Image Gallery with 2 images selected: `imageBubbleCount()` goes up by 2.
  - (c) Audio (label strings.xml:137) with sample.wav: `audioBubbleCount()` goes up by 1 and `playNewestAudio()` works.

### M7 Cancelling the upload preview posts nothing (1)
- Web: "closing the document preview posts nothing" (web:74).
- Proposed: "Backing out of the picker or preview posts nothing". Note `chatRowCount()`, open Document, pick sample.pdf, then press back on the preview (`isMediaPreviewUp`). Expected: the row count is unchanged after 3 s.

### M8 Caption edit on media (1)
- Web: "a captioned document shows the caption and offers Edit Message" (web:322).
- Android: Edit is shown for media when the message has a caption (CA:1432-1436). It opens the description dialog (`editMediaDescription`, CA:1604-1630).
- Proposed: upload sample.pdf with caption C, long-press it, tap "Edit" (strings.xml:277), set "C edited", and save. Expected: the new caption is shown with the red "Edited" tag (TVH:182-187, strings.xml:742).

### M9 Edit dialog validation (2)
- Web: "edit Cancel and Escape leave the message unchanged" (web:382); "unchanged text sends no PUT, empty text keeps the modal open, the box caps at 8,000" (web:387).
- Android edit dialog: `edit_comment_dialog.xml` has `message`, `leftButton` (Cancel) and `rightButton` (KB:333-381).
- Proposed:
  - (a) Edit, type extra text, tap `leftButton` (`HomeChatPage.cancelEditDialog`). Expected: the original text stays and no "Edited" tag appears.
  - (b) Edit, clear to spaces, tap `rightButton`. Expected: the field error "Message could not be empty" (KB:355-358, strings.xml:1201) and the dialog stays open.
  - (c) Edit with more than 2,000 characters. Expected: the word_limit_error snackbar (KB:359-367).
  - (d) Edit and tap Update without changing anything. Expected (web parity): no "Edited" tag. On Android today the PUT is sent anyway (no equality check at KB:353-372, then CA:526-541 and CVM:654-660), which is sheet bug CAT-03, so this case will fail until fixed.

### M10 Long-press menu: own text and others' text (2)
- Web: "an own text message offers the expected menu and nothing forbidden" (web:352); "another member's message offers no Edit Message" (web:542).
- Android menu for own delivered text (CA:1415-1443, OH): Edit, Gallery, Reply (if the sender is not a caterer, CA:1417), Forward, Share and Delete (if admin or within the edit window). Read By, Copy, Print and Image Reply are hard-coded false (CA:1422-1431). There is no "Forward to Remote Projects".
- Proposed: "Own text menu is exactly the Catering set". Expected `menuOptions()` contains Edit, Reply, Forward, Share, Delete and Gallery, and does not contain "Read By User", "Copy", "Print", "Download" or "Edit and Post". Second case: long-press a message from another user; the menu has no "Edit" (CA:1434-1436 requires `AppHelper.userId == sender`).
- TCB `longPressShowsOptions` (TCB:219) only prints the options.

### M11 Long-press menu on media (3)
- Web: own document without caption (web:357), own image (web:362), own audio (web:367).
- Android: "Download" for any non-text, non-location message (`showSave`, CA:1419, OH label strings.xml:1472). Edit only when there is a caption (CA:1434). Image Reply is never offered (CA:1431).
- Proposed: three cases after the M5 and M6 uploads.
  - Doc without caption: Download yes, Edit no.
  - Image with caption: Download and Edit yes, "Edit and Post" no.
  - Audio: Download yes, Edit no.

### M12 Download (2)
- Web: Download on a PDF (web:497) and on an image (web:502).
- Proposed: long-press the document or image, tap "Download" (`HomeChatPage.tapDownload`). Expected: the toast or snackbar "File Downloaded Successfully." (CA:1965-1996, strings.xml:3369). If the account lacks the right, see M25.

### M13 Viewers (2)
- Web: "a PDF bubble opens the viewer ..." (web:507); "an image bubble opens the preview, Escape closes it" (web:512).
- Android: an image or video tap opens the in-app file viewer (`openFileListShowingPage`, CA:1723-1743). Any other document goes to `openFileFromExternalFromUrl`, an external app (CA:1744-1751). Audio does nothing (CA:1721).
- Proposed:
  - (a) `openNewestImage()`, then `isImageViewerOpen`, then back. The viewer closes and the chat is shown again.
  - (b) `openNewestDoc()`. Expected: the app hands off to the external viewer (check `safeCurrentPackage()` changes), then `returnToChat`.
- Web expects an in-app viewer with no Download button. This is a platform difference and not a bug.

### M14 Selection mode (1)
- Web: "selection mode hides the options buttons and FABs and counts the selection" (web:372).
- Android: Delete opens `layOption` with `tvCount`, `layAction` and `tvAction`="Delete", and hides the keyboard bar (`showHideView(false)`) (CA:1450-1465). Tapping a selected row toggles it (CA:1140-1158). `btnBack` leaves (CA:1649-1658).
- Proposed: long-press, Delete. Check `tvCount`="1" and that `etMessage` is not visible. Tap the row and check the count changes. Tap `btnBack`. Expected: `etMessage` is back and the message is still listed.

### M15 Delete variants (3)
- Web: "delete can be backed out at both steps" (web:402); "multi-select delete removes two messages at once" (web:407); "a deleted message stays gone after a reopen" (web:412).
- Android: the confirm dialog is titled "Delete" with "Are you sure you want to delete?", "No" and "Yes" (CA:1473-1491, strings.xml:375).
- Proposed:
  - (a) Delete, then `btnBack`: the message stays. Delete, Delete in the bar, then "No": the message stays.
  - (b) Post two markers. Delete the first, tap the second (`tvCount`="2"), Delete, Yes. Both are gone.
  - (c) Delete, leave Catering, reopen it, and check with `messageShownScrolled` that it is still gone.

### M16 Reply composer (3)
- Web: "the reply quote names the sender and can be dismissed" (web:427); "the paperclip is hidden while replying" (web:432); "a whitespace-only reply sends nothing and keeps the quote" (web:437).
- Android: `llReply` shows `tvUserName` and `tvMessage`. `ivAttachment` and `ivRecord` are hidden because Catering registers with `isAccount=false`, and `ivSearch` is hidden too (KB:383-392). `llDelete` dismisses (KB:396-398).
- Proposed:
  - (a) Reply on an own marker. `llReply` is visible and `tvMessage` = the marker. `ivAttachment` is not visible. Tap `llDelete`: `llReply` is gone and `ivAttachment` is back.
  - (b) Reply, type 3 spaces, tap `ivSendMessage`. Expected: snackbar "Please Type Message To Send" (KB:175, strings.xml:524). No comment is added and `llReply` is still shown.
- Caterer caveat: Reply resets the recipient to "Select User" (CA:1633-1639).

### M17 Comments: edit, delete, render once (3)
- Web: "an own comment renders once" (web:422); "edit an own comment" (web:442); "delete an own comment: No keeps it, Yes removes it" (web:447).
- Android comment menu (comment arrow, CA:1170-1181, 1386-1412): Edit (own comment, within the window) and Delete. Read By, Copy, Forward and Share are off (CA:1390-1397). Delete asks "Delete" / "Are you sure you want to delete?" with No and Yes (CA:1885-1903).
- Proposed: post a marker and reply with "c-<ts>". Check `rvMessageReply` holds "c-<ts>" exactly once after 5 s. Open the comment menu and check it has Edit and Delete and no "Read By User". Edit to "c-<ts> edited". Delete, then No (it stays), then Yes (it is gone).

### M18 Emoji picker (2)
- Web: "the emoji picker lists emoji and closes again" (web:54); "an emoji from the picker is appended at the end and sent" (web:277).
- Android: `ivEmoji` toggles `emojiPicker` (KB:179-186), and the picked emoji is appended at the end of the text (`etMessage.text.append`, KB:185), the same as web.
- Proposed: type "qa-emo-<ts>", move the cursor to the start, tap `ivEmoji`, then tap the first emoji. `etMessage` ends with the emoji. Send, and the row posts. Tap `etMessage`: `emojiPicker` hides (KB:296-306).

### M19 Read More (1)
- Web: "a long message clips with Read More and expands back with Read Less" (web:247).
- Android: `setReadMore` with a limit of `TEXT_VIEW_CHAR_LIMIT_SHOW = 1000` (Constants.kt:263, `utils/Extensions.kt:5568-5626`). The labels are " Read more" and " Read less" (strings.xml:3429-3430).
- Proposed: post 1,200 characters ending in "TAIL-<ts>". Expected: "Read more" is visible and the TAIL is not shown. Tap the "Read more" span (by coordinates at the end of `tvMessage`) and the TAIL is shown. Tap "Read less" and it is hidden again.

### M20 Links and e-mail (2)
- Web: "links: https becomes a safe link; www and javascript: do not" (web:257); "an e-mail address becomes a mailto link" (web:262).
- Android: `tvMessage` uses `android:autoLink="all"` (chat_text_layout.xml:109-113), so web URLs, e-mail addresses and phone numbers are linked. Note that Linkify also links a bare "www.example.com", which web does not.
- Proposed: post "https://example.com/qa-<ts>" and tap it: the browser intent opens (package changes), then back. Post "qa+e2e@example.com": tapping opens a mail chooser. Record the `www.` result as a known difference.

### M21 Content rendering (2)
- Web: "HTML in a message renders as text" (web:252); "special characters round-trip exactly and markdown stays literal" (web:272).
- Android: plain `setText` through `setReadMore` (`AttachmentVH.kt:603-660`).
- Proposed: post `<b>bold</b> qa-<ts>` and check that `tvMessage` text contains the literal `<b>bold</b>`. Post "🎬 مرحبا Café ©®™ *bold* _it_ 12345 qa-<ts>" and check the text is the same as typed.

### M22 Scrolling: arrow, newest row, pagination (4)
- Web: "gallery (and history) FABs are present; scroll-to-bottom only when scrolled away" (web:24); "the feed opens with the newest row in view" (web:114); "scrolling up loads an older page" (web:119); "the scroll-to-bottom button returns to the latest message" (web:537).
- Android: `arrowDownCard` shows when the last row is not fully visible and scrolls to the end on tap (CA:245-247, 402-418). Scrolling to the top loads older messages (CA:437-442), as does pull-to-refresh `refresh` (CA:240-243). There is no Gallery FAB (Gallery is in the long-press menu) and no History.
- Proposed:
  - (a) Open a meal tab. The newest marker is visible and `arrowDownCard` is gone.
  - (b) Swipe down 5 times. `arrowDownCard` is visible. Tap it: the marker is visible and the arrow hides.
  - (c) With 50 or more rows, `chatRowCount` grows after repeated swipes to the top.

### M23 Receiver leaks across meal tabs (2)
- Web: "the receiver leaks across tabs and across the Accounts window" (web:624); "a receiver picked in Breakfast is not carried into Lunch" (web:652, known web bug).
- Android: on a tab change `handleUnitChangeObserver` never resets `userName.tag` (CA:468-507), so a user picked in Breakfast is still the receiver in Lunch (`getReceiverUser`, CA:287-294). The bug is likely on Android too.
- Proposed (caterer account): Breakfast, `addMember`, "Select User", pick user X, and check `userName` = X. Select Lunch. Expected (web intent): `userName` = "Select User" and `layNoMsg` is shown. On Android today this will likely fail, which documents the bug.

### M24 Delete permissions (4)
- Web: "non-admin delete toast on others' rows; admin deletes account B's row" (web:628); "an admin deletes the caterer's message and it disappears for the caterer too" (web:671); the two "without admin rights ... 'You have no permission to delete other user message'" cases (web:724, 742).
- Android:
  - A non-admin tapping another user's row in delete mode gets "You have no permission to delete other user message." (CA:1155-1158, strings.xml:773).
  - A non-admin after 2 hours gets "You can't delete message after 2 Hours" (CA:1144-1149, strings.xml:1546).
- Difference from web: Android lets an admin delete someone else's message only if the admin is also in the catering department (`isAdmin && departmentIdentifier == CATERING`, CA:1360, 1451). A non-caterer admin, like the web iPhone XRR, gets the same "no permission" toast. Web says the admin can delete. Raise this with product.
- Proposed (two accounts, or a seeded foreign message): as a non-admin, post an own marker, Delete, then tap the foreign row. Expected: the toast and `tvCount` still "1". Second case as a non-caterer admin: record the actual result against web.

### M25 View and Download rights (4)
- Web: "no view access bounces; no download right opens the permission request" (web:634); "without the Catering Download right, Download offers the admin request" (web:726, 748); "without the Catering View right the tile is gone ..." (web:728).
- Android: Download without the right shows the alert "You do not have Downloading Rights on this Tool. Do you want to request one of the Admins to give you that permission?" with Cancel and Yes, which leads to the admin request (OH, `showSave` block; strings.xml:3038). With View off, the activity closes on the rights flow (CA:559-564) and when the tool is disabled (CA:830-833).
- Proposed: with an admin account, set the second account's Catering Download to off.
  - Long-press an image, then Download. Expected: the alert text above. Tap Yes and the admin-request list opens. Close it without sending.
  - With View off: the Tools list has no "Catering" (`ToolsPage.isModuleListed` is false), and an open Catering screen closes.

### M26 Catering Settings screen (1)
- Web: "CAT-O02..O04 Catering Settings lists the units, locks the system ones and searches" (web:606).
- Android: Settings shows "Catering Settings" (strings.xml:119) for catering-department users (`SettingPage.kt:529-562`). ACS lists units with edit and delete and a search box (ACS:90-143, 341-352). System units refuse delete with "Unit Cannot be Deleted." (ACS:105-106, strings.xml:844).
- Proposed (caterer): Settings, Catering Settings. The list count matches the number of Catering tabs. Delete on Breakfast gives "Unit Cannot be Deleted.". A nonsense search gives an empty list.

### M27 Duplicate unit name (3)
- Web: "duplicate unit name toast ..." (web:630); "creating a unit with a name already in use is refused ..." (web:712, failed on web); "a second unit with a name already in use is refused ... ('Unit already exists')" (web:736).
- Android: the server error is shown as a snackbar through `getDataFromLabelKey` (MUF:215-216).
- Proposed (caterer): "+", type "QA Snack", Submit. CT already reuses this unit. Expected: a snackbar saying the unit exists, the form stays open, and the tab strip has "QA Snack" only once.

### M28 Realtime between two accounts or devices (5)
- Web: "a message posted from a second context appears live" (web:577); "CAT-P01..P07 realtime ..." (web:632); "a caterer's new, edited and deleted message reach a member's open Catering live" (web:677); "a caterer's reply to a member's message reaches the member live" (web:683); "a unit the caterer creates, renames and deletes ... follows on a member's open tabs live" (web:706).
- Android: socket listeners for message add, edit, delete, multi-delete and comment add, edit and delete (CA:655-777), and for unit create and delete (CA:636-647).
- Proposed: needs two devices or two Appium sessions. Run it as a separate suite (memory: one suite per session) with a caterer account and a member account. Steps: A posts, edits and deletes; B sees each change within 10 s without reopening.
- `catererCanReplyToUserMessage` (CT:379) only checks that Reply is offered.

### M29 Unread badges and "New Messages" (2)
- Web: "a caterer's post in Lunch badges the member's Lunch tab; opening it shows 'New Messages' and clears the badge" (web:689); "a caterer's post raises the Catering tile's unread count" (web:695).
- Android: the tab badge is `zBadge` (`HomeUnitHolder.kt:26-30`, fed by CVM:217-240). The row header `unreadMessageHeader` says "New Messages" (chat_text_layout.xml:16,46; strings.xml:1354; CVM:1540-1557). Catering counts toward the Tools badges (`CommonBadgesHandler.kt:229, 264-270`).
- Proposed (two devices): B is on Breakfast while A posts in Lunch. B's Lunch tab shows a badge of 1 or more. B opens Lunch: "New Messages" is shown above the post and the badge clears.

### M30 Push notification opens the unit (1)
- Web: "the 'New message received' toast for a catering message, and its click to the unit" (web:701, a placeholder on web).
- Android: a notification click passes `EXTRA_NOTIFICATION_OBJ` and switches to that unit (CA:296-337). This can be tested on a real device with `driver.openNotifications()`. Low priority and two-device.

### M31 Gallery Docs tab (1)
- Web: "the Gallery Docs tab lists an uploaded document, filters, and offers a card menu" (web:527).
- Android: Gallery is in the long-press menu (CA:1442, 1854-1871). It opens LIB with Media, Docs and Links tabs (LIB:184-186). There is no Audio tab, no search and no card menu. The size is the raw `file_size` (LIB:155, `ChatLibraryAdapter.kt:121-123`).
- Proposed: after uploading sample.pdf, long-press, Gallery, Docs. sample.pdf is listed. Assert the size text matches `\d+(\.\d+)? ?(KB|MB|B)`; this fails today (sheet bug CAT-05).

### M32 Network-fault cases, deferred (5)
- Web: empty unit list (web:177), failing unit list (web:183), "a failing edit keeps the text and a refused delete keeps the row" (web:392), "a failing comment POST shows an error toast" (web:457), "a failing chat page GET leaves the composer usable" (web:572).
- These need a stub or proxy (for example a mitmproxy rule), which the Appium setup does not have.
- Android note to keep: delete is optimistic. Rows are removed before the PUT and `onError` is commented out (CVM:829-870), so a refused delete still removes the row locally. Web keeps the row. This difference is worth raising even before automation.

### M33 Low-value or cheap checks (7)
- "typing @ shows no mention picker" (web:49): type "@a" and check that no picker or list appears. Android has no mention support in KB or CA (no match for "mention" in either).
- "the options menu closes on Escape and on an outside click" (web:99): open the menu and tap outside or press back. The popup dismisses (OH `binding.root.setOnCLick` dismiss; it also closes on scroll at CA:392,397).
- "posting is forced on: no REQUEST bar" (web:215): `tvRequest` is not visible (`visibility=gone`, activity_catering.xml:331-339) and `ivAttachment` is visible. `handlePostingRights` is a no-op (CA:836-844).
- "a pending row shows the clock ..." (web:292): `ivClock` shows while status is 0 (TVH:217). This needs a slow network, so it is flaky.
- "a fast double Enter posts the message once" (web:297): double-tap `ivSendMessage` and check the marker appears once. The second tap finds the box empty and shows "Please Type Message To Send".
- "the Home Tab submenu lists Home units without Calendar or Call Sheet" (web:467): Forward, then Home. Calendar is excluded (FWD:261). Call Sheet is listed only for document forwards (FWD:262-270).
- "a posted row shows avatar, sender · designation and a formatted timestamp" (web:532): `tvUserName` is the own name and `tvMsgTime` is not empty (chat_text_layout.xml:73,181).

---

## CONFLICT (5 web tests, 3 groups)

### C1 Read By (3 web tests): web:452 "a comment offers Read By" (skipped), web:487 "Read By lists the two segments ..." (skipped), web:594 "neither a message nor a comment offers Read By"
- Web says: Catering offers no Read By on messages or comments. web:594 is the definitive case, and the two skipped ones are placeholders.
- Android test says: TCB `readByUser` (TCB:317-331, DisplayName "READ BY USER opens the read-by list") expects Read By to open. It uses `assumeTrue(opened)`, so on Catering it always skips and never checks anything.
- Android source says: `showReadBy = false` for messages (CA:1422) and for comments (CA:1393).
- Verdict: web and Android source agree; the inherited test is wrong for Catering. Override `readByUser` in CT to assert that "Read By User" is absent from `menuOptions()`, for both a message and a comment (see M17).

### C2 Attachment picker set (1 web test): web:59 "the paperclip menu lists the expected pickers ..."
- Web says: exactly Image Upload, Document Upload and Audio Upload. Upload Media, Video and Location are not offered.
- Android test says: TCB `attachmentPickerHasMediaTypes` has the DisplayName "Attachment picker offers the media types (Photo/Gallery/Video/Document/Audio/Location/Contact)" (TCB:191). It asserts only `size >= minAttachmentOptions()`, and CT sets that to 4 (CT:40). That would still pass if Video or Location were added to Catering by mistake.
- Android source says: exactly Photo, Image Gallery, Audio and Document (`PICKER_CAMERA`, `PICKER_IMAGE_GALLERY`, `PICKER_ITEM_PICK_AUDIO`, `PICKER_ITEM_DOCUMENT`; CA:949-956 and 976-984; labels at `mediaHandler/ItemAdapter.kt:94-104`).
- Verdict: the Android source is right for Android. It adds a Camera "Photo" entry that web does not have, which is a platform difference. Fix the test: override it in CT to assert the exact set {Photo, Image Gallery, Audio, Document} and the absence of Video, Video Gallery, Location and Contact. Also fix the misleading DisplayName for Catering.

### C3 Unit deletion assumption (1 web test): web:611 "create a custom unit from Catering Settings, then delete it"
- Web says: a custom unit can be created, renamed (an empty rename is refused) and deleted from Settings, Catering Settings.
- Android test says: `createNewMealUnit` (CT:444-446) says "A catering unit CANNOT be deleted from the app", so it reuses "QA Snack" and never cleans up.
- Android source says: Settings, Catering Settings (`SettingPage.kt:529-562`) lists units with edit and delete. Delete asks "Are you sure you want to delete this unit?" and calls `USER_UNIT_CATERING_DELETE` (ACS:104-143, strings.xml:846). Edit opens "Edit Catering Unit" (`CreateNewUnitPage.kt:71`). An empty name is refused with "Please Enter Unit Name" (MUF:170-172, strings.xml:692).
- Verdict: the Android source contradicts the test's assumption. Add a create, rename, then delete case through Catering Settings (it can clean up after itself) and correct the comment. The "+" path (CA:228-238) can still only create.

---

## COVERED (24 web tests, 20 rows)

| Web test(s) | web: | Android case | Notes |
|---|---|---|---|
| opens with the composer and the feed root | 9 | TCB `moduleOpens` (inherited) | The window-title part is web-only |
| empty and whitespace-only messages are not posted | 39 | TCB `emptyInputHidesSend`, `whitespaceNotSendable` (inherited) | |
| the Gallery drawer opens with its four tabs | 109 | TCB `galleryOption` (inherited) | Android Gallery is a long-press option with 3 tabs (Media, Docs, Links; LIB:184-186) and no Audio tab |
| opens on the three meal tabs; tabs match the unit list (2) | 131, 167 | CT `mealTabsVisible` | Partial: "first tab selected" and "no raw _label key" are not asserted (labels go through `getDataFromLabelKey`, `HomeUnitHolder.kt:19`) |
| opens from the Tools tile | 136 | TCB `moduleOpens` through `openModuleChat` (inherited) | |
| switching a tab selects it and fetches that unit's chat | 172 | CT `eachMealTabOpens` | |
| with no enabled caterer a send is refused with a toast | 199 | CT `noCatererBlocksPosting` | The test comment expects a local echo, but per CA:526-548 nothing is sent and the box is cleared. Web's known bug ("text lost") applies to Android too; asserting that the box is empty is not done |
| the Select User button and the caterer instructions follow the role | 205 | CT `catererRecipientGating` | `isRecipientHintShown` is printed, not asserted |
| the "+" follows the catering designation | 210 | CT `catererRecipientGating` | The non-caterer case (no "+") is not asserted |
| post text with Enter ...; the mic slot becomes Send (2) | 227, 232 | TCB `sendValidMessage`, `typingRevealsSend` (inherited) | |
| a posted message persists across a reopen | 287 | CT `messagePersistsAcrossTabSwitch` | Partial: a tab switch, not a reopen |
| edit an own message ... Edited tag | 377 | TCB `editMessage` (inherited) | The "Edited" tag is not asserted |
| delete an own message through the selection flow | 397 | TCB `deleteMessage` (inherited) | |
| a reply nests as a comment inside the parent | 417 | TCB `replyToMessage` (inherited); CT `replyAllowedOnUserMessage` | |
| the Forward modal lists the destinations | 462 | TCB `forwardMessage` (inherited) | Partial: the ForwardPage destinations (Home, Info, Confidential Info, Members, Group, Email; FWD:869-958) are not asserted. Android shows "Are you sure you want to forward?" first (CA:1501-1524) |
| Share opens Compose Email ... | 482 | TCB `shareMessage` (inherited) | Android opens the system share sheet through the multi-select bar (CA:2027-2090), not Compose Email |
| an own post carries the "To :" line | 589 | CT `userMessageIsAddressedToCaterer`, `catererBroadcastGoesToAllUsers` | Android label "To : All Caterer" (strings.xml:861, 2451; TVH:49-71) versus web "All caterer"; each platform is right for itself |
| CAT-G02..G07 the Select Users modal | 601 | CT `catererRecipientDialogOptions`, `catererDirectedMessage` | Partial: own name excluded, "No Users Found" search and reopen are not covered |
| a caterer sends to one user ... and to All Users (2) | 622, 646 | CT `catererDirectedMessage`, `catererBroadcastGoesToAllUsers` | Single device. The live receipt and the image-to-member parts are in M28. `catererDirectedMessage` would also pass on "To : All Users"; tighten it to "To : <picked>" |
| no Reply on caterer-authored messages (2) | 626, 665 | CT `userCannotReplyToCatererMessage`, `replyHiddenOnCatererAuthoredMessage` | Matches CA:1417. Web also checks the member sees no Edit (M10) |

Android-only cases with no web counterpart: CT `sendInEveryMeal`, `mealConversationsAreIsolated`, `postingRequiresACaterer`, `catererCanReplyToUserMessage`, `createUnitScreenOpens`, `createUnitEmptyNameRejected`, `createNewMealUnit`, `sendMessageInCreatedUnit`; TCB `longPressShowsOptions`, `sendImageMessage`.

---

## WEB-ONLY (34 web tests, 27 rows)

| Web test(s) | web: | Reason | Evidence |
|---|---|---|---|
| the scoped composer resolves to exactly one element | 14 | DOM scoping | n/a on a native Activity |
| theme toggle (2) | 19, 189 | No in-screen theme toggle; Android follows the system theme | No toggle id in activity_catering.xml (ids at 12-409) |
| every acked row has an options button; the counter is aria-live | 29 | No per-row chevron (long-press instead) and no counter or aria | TVH:155-174 long-press; no counter in KB |
| the first character is auto-capitalised | 34 | Done by the IME (`textCapSentences`), not the app; Appium `setValue` bypasses the IME | emoji_keyboard_layout.xml:261 |
| each hidden file input advertises the right accept list | 64 | No DOM file inputs; the system picker (SAF) filters | `mediaHandler/PickerDialog.kt:314-320` |
| Location Upload opens Select Location (skipped on web) | 69 | Location is not in the Catering picker (also absent on web) | CA:949-956 |
| image picker rejects gif; document picker rejects exe; PDF with JavaScript is blocked; audio picker rejects text (4) | 79, 84, 89, 94 | No in-app file validation; the OS picker filters by MIME | No match for "malicious", "Executable" or "No valid" in app source |
| minimizing and restoring the window keeps the draft | 124 | Window (MDI) layout | n/a |
| a slow unit list shows the tab skeleton | 141 | No skeleton view; units load from the Realm cache first | CVM:134-150; no skeleton in activity_catering.xml |
| unit list loads first, then limit=50&page=0 | 147 | Network-order assertion | n/a with Appium |
| the Back arrow shows on the classic layout only | 152 | Layout modes | n/a |
| MDI window controls | 157 | Window layout | n/a |
| opening raises no page error / console warning | 162 | Browser console | n/a |
| with Home mounted too, locators stay scoped | 220 | Web DOM guard (TCB `inModuleScreen` covers the Android equivalent) | TCB:82-88 |
| Shift+Enter newline; Ctrl+Enter sends (2) | 237, 242 | Hardware-keyboard shortcuts | n/a |
| the own mention token renders as the own name | 267 | No mention-token rendering in the Catering chat | `AttachmentVH.kt:603-660` has no `{{...}}` handling |
| dropping an image on the feed | 342 | Drag and drop | n/a |
| Image Reply posts a new image row (skipped) | 347 | Image Reply is off in Catering | CA:1431 `imageReply = false` |
| forwarding into this feed creates a copy (skipped) | 472 | ForwardPage has no Catering destination | FWD:869-958 |
| Forward to Remote Projects | 477 | Not in the Catering menu | No "remote" in CA; OH option list |
| Publish to Doc Distribution (skipped) | 492 | `isShowDistributeToDD` not passed for Catering | CA:1415-1443 |
| History opens ...; a deleted message shows in History (2, skipped) | 547, 552 | No History in Catering (also absent on web) | No "history" in CA or activity_catering.xml |
| a failed message can be deleted from the retry menu | 567 | Android retry is a single "Retry" chip with no Resend/Delete menu | CA:1193-1351; chat_text_layout.xml:149 |
| the Edit modal renders inside the tool window body | 582 | Window layout | n/a |
| tab overflow; tabs that do not fit are in the "more" menu (2) | 636, 638 | `recTab` is a horizontally scrolling RecyclerView with no overflow menu (`more_units_arrow` is only referenced in a constraint and has no view) | activity_catering.xml:32, 62-63 |
| closing an Accounts window leaves the Catering receiver alone | 659 | Separate Activities with their own bindings; no shared window state | CA:217-224 |
| Edit on a unit in the "+" dialog keeps the dialog open | 715 | Android "+" opens the create-only "Add Catering Unit" screen with no unit list | CA:228-238; `CreateNewUnitPage.kt:71` |

---

## Sheet bugs: do they apply to Android?

| Bug | Applies to Android? | Evidence |
|---|---|---|
| CAT-01 Search for 1-2 letters shows no result text | No (the sheet already says "Not affected"). `tvCount` shows the match count for any length, including "0". There is no "No Matches Found" label, though | CA:2132-2139 |
| CAT-02 Draft lost when switching units | Yes, Open. The draft is cached only when `currentUnitInfo == null` | CA:877-898; KB:125-130. Case M3 |
| CAT-03 Update with no change marks "Edited" | Yes, Open. The dialog never compares old and new text and the PUT always goes out | KB:353-372; CVM:588-660. Case M9d |
| CAT-04 2,000-character limit versus web 8,000 | Yes, Open. `TEXT_LIMIT = 2000` | Constants.kt:733; KB:155-163. Case M2 |
| CAT-05 Gallery Docs size is a bare number | Yes, Open. The raw `file_size` is passed through | LIB:155; ChatLibraryAdapter.kt:121-123. Case M31 |

Other Android-side differences found while checking (not on the sheet):
- A non-caterer admin cannot delete others' messages (CA:1360, 1451); web allows it.
- A refused delete still removes the row locally (CVM:829-870).
- A space-only search matches every row (CA:2128-2133).
- autoLink links bare `www.` (chat_text_layout.xml:113).
- The caterer receiver persists across tabs (CA:468-507).
- activity_catering.xml declares the `layPostRights`, `layPostMsg`, `keyboardLayout`, `layNoMsg` and `tvRequest` blocks twice (lines 290-341 and 344-398), so id-based locators can match two nodes. Use `findElements` and check visibility.
