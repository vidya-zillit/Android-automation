# Home gap analysis: web E2E vs Android Appium suites

Web source: `web-e2e/home.md` (165 tests, 34 sheet bugs).
Android suites: `src/test/java/com/zillit/automation/tests/homechat/` (HomeChatTest, ChatOptionsTest, PostingRightsTest, MediaPreviewTest, MediaSystemPickerTest, ManualPickSendTest, AllUnitsMediaTest, TwoUserChatTest, RegressionStressTest) plus `tests/callsheet/CallSheetTest` and `tests/adminsettings/CreateUpdateHomeUnitTest` where they touch Home.
App source: `Zillit-Android/app/src/main` (paths below are relative to `java/com/zillit/zillitapp/` for Kotlin and to `res/` for XML). Key files: `bottomNav/home/Home.kt`, `bottomNav/home/viewmodel/HomeVm.kt`, `baseUtils/CommonKeyboardFragment.kt`, `layout/fragment_home.xml`, `layout/emoji_keyboard_layout.xml`, `layout/chat_text_layout.xml`.

Web test numbers (#) below follow the order of `home.md` (1 = "opens with the composer and the feed root", 165 = "a non-admin poster gets the 30-minute notice").

## Summary

| Class | Web tests | Notes |
|---|---|---|
| COVERED | 30 | 11 of these are weak (only "screen opens" / partial asserts), listed with fixes |
| MISSING | 102 | grouped into 39 proposed Android cases (M1-M39) |
| WEB-ONLY | 30 | browser/MDI/keyboard features, or features absent from Android Home |
| CONFLICT | 3 | 2 conflict rows (C1, C2) |
| **Total** | **165** | |

Most valuable MISSING cases (P1): M1 reply quote leaks across unit switch, M2 per-unit drafts, M3 post in flight during unit switch, M4 offline send / retry / delete of a failed row, M5 Edit offered on another member's message, M6 double-tap Send opens mic, M7 machine-speed typing OOM, M8 forward a forwarded copy to a member, M9 unread badge + New Messages divider, M10 edit-dialog validation.

## MISSING (prioritised)

Ids are `com.zillit.zillitapp:id/<id>`. "FAIL expected" means the current Android source suggests the case will fail, i.e. it catches a known or likely bug.

### P1

**M1. Reply quote is dropped when switching units** (web #33). FAIL likely.
- Steps: Bulletin; send marker; long-press it, Reply (reply bar `llReply` shows, emoji_keyboard_layout.xml:130); tap the Call Sheet chip in `recTab`, then the Bulletin chip (or another chat unit); check `llReply`; type text and Send.
- Expected: `llReply` gone and `etMessage` empty after the switch; the next Send posts a normal message, not a comment on the earlier message.
- Source: `handleUnitChangeObserver` (Home.kt:390-420) calls `btnBack.performClick()` and `registerListener` but never `keyBoardFragment.resetUi()`. The pending reply target `homeChatInfo` is set in CommonKeyboardFragment.kt:384 and cleared only in `resetUi` (:428), and Send routes to `replyOnMessage` whenever it is non-null (:164-168). The quote can therefore survive a unit switch and the reply can attach to a message from another unit.

**M2. Each unit keeps its own unsent draft** (web #41, #42, #57; sheet BUL-27).
- Steps: Bulletin: type `qa-draft-b-<ts>` and do not send. Tap Call Sheet: `etMessage` must be empty. Type `qa-draft-cs-<ts>`. Back to Bulletin: the Bulletin draft is shown. Back to Call Sheet: its own draft is shown. Switch the bottom nav to Email and back to Home: the draft is still there. Send it, then switch units and back: `etMessage` is empty. Optional: terminate and activate the app, then record whether the draft survives.
- Expected: drafts are kept per unit and never leak between units. Once a draft is sent it is cleared.
- Source: draft is written per unit on every change (`SharedPref.setUserCacheMessage(unitId, text)`, Home.kt:831; SharedPref.kt:1055-1058), restored from the in-memory map on `registerListener` (CommonKeyboardFragment.kt:62-71, 127-130; the SharedPref read is commented out, so a cold restart probably loses it), and cleared in `clearMessage` (:411-414).

**M3. A post still sending when the unit changes ends as one row** (web #148; BUL-28).
- Steps: send `qa-inflight-<ts>` and tap another unit chip at once. Wait 8 s, then return. Repeat with the network off (see M4) so the send fails.
- Expected: exactly one row for the marker. It is either sent (no `ivClock` and no `btnTryAgain`, chat_text_layout.xml:198 and :149) or failed with `btnTryAgain`. It is never duplicated or stuck on the clock.
- Source: send status lifecycle 0 -> 2 / -1 (HomeVm.kt:744-811; TextVH.kt:210-232).

**M4. Offline send, retry, and deleting a failed row** (web #112, #113, #114, #147; BUL-25). The delete part should FAIL.
- Steps: (a) Turn the network off (Appium `setConnection` / `svc wifi disable` + `svc data disable`). Send `qa-offline-<ts>`: the row shows `btnTryAgain`. Turn the network back on and tap `btnTryAgain`: the row becomes sent and appears exactly once. (b) Offline, send `qa-offdel-<ts>`. Long-press it, Delete, tap the action-bar Delete, then Yes. Relaunch online.
- Expected: (a) the failed row is marked and resends once. (b) the unsent row is removed locally with no "Internet connection lost" message and does not come back after a relaunch (web parity).
- Source: offline send sets status -1 (HomeVm.kt:803-806), and the retry handler is at Home.kt:1130-1290 ("Do you want to try again?" / "Resend Message", strings.xml:1971-1972, shown only for multi-item groups). The action-bar Delete blocks offline with `internet_connection_lost_please_check` (Home.kt:1441-1442) before `deleteSelectedUnSendMessage` (HomeVm.kt:1110-1121) can run, which is BUL-25.

**M5. Another member's message offers no Edit** (web #109; BUL-16). FAIL expected.
- Steps: extend `TwoUserChatTest#otherUserMessageInteractions`: on B's message assert `menuOptions()` contains no "Edit". Then tap Edit if it is present and record the snackbar.
- Expected: no Edit item on other members' messages.
- Source: `showEdit = !homeChatInfo.message.isNullOrEmpty()` (Home.kt:1404) has no owner check. The owner check only runs after the tap (`you_cant_edit_other_user_message`, Home.kt:1663-1675), or the 30-minute guard fires first (Home.kt:2159-2170).

**M6. A fast double tap on Send posts once and never opens the mic** (web #60, #146; BUL-22).
- Steps: `pm revoke <pkg> android.permission.RECORD_AUDIO`. Type `qa-dbl-<ts>` and double-tap `ivSendMessage` within about 100 ms (a W3C pointer action with two taps). Wait 4 s.
- Expected: exactly one row for the marker. No permission dialog appears and the recording bar `audioVw` stays hidden.
- Source: after the send clears the text, Send swaps back to `ivRecord` in the same place (CommonKeyboardFragment.kt:271-272, 313-320), and `ivRecord` opens the audio permission request (:209-218, 229-250).

**M7. Machine-speed typing does not run out of memory** (web #142; BUL-13, Android Open).
- Steps: focus `etMessage` and feed 1,500 characters quickly through the IME path (`adb shell input text` in 50-character chunks, without sleeping). Read `dumpsys meminfo <pkg>` before and after. Do not take a uiautomator dump mid-run (see the device-stability note).
- Expected: the app stays alive, `etMessage` holds all the text, and the Java heap grows by less than about 150 MB.
- Source: every keystroke runs `onTextChange` and writes the full draft to SharedPreferences (Home.kt:820-833) plus the text watcher (CommonKeyboardFragment.kt:290-300). This is the likely heap churn.

**M8. Forward a forwarded copy to a member** (web #151, #95; BUL-26 High).
- Steps: long-press own text, Forward, "Are you sure you want to forward?" (Home.kt:1471-1480, strings.xml:841), Yes. In ForwardPage ("Select Tab to Forward", strings.xml:1387) tap Home, then "Select Unit" (ForwardPage.kt:255-300), then Bulletin: two copies are in the feed. On the copy: Forward, Yes, Members, tick device B, Submit. On B, open C&C, then the chat with A.
- Expected: the copy appears in the feed. B receives the forwarded text within 60 s, or A sees an error. It is never silently dropped.

**M9. Unread badge on a unit chip and the New Messages divider** (web #122, #160).
- Steps (two devices): A opens Home and selects Calendar. B posts in Bulletin. A checks the Bulletin chip badge, then opens Bulletin.
- Expected: A's Bulletin chip shows the `badge` count (custom_tabbar_item.xml:34; HomeUnitHolder.kt:26-29). After A opens Bulletin, the "New Messages" header (`unreadMessageHeader`, chat_text_layout.xml:16; strings.xml:1354) sits above B's post and the badge clears (`markRead`, HomeVm.kt:181-197, 314-319).

**M10. Edit dialog validation** (web #77, #78; BUL-12). Part should FAIL.
- Steps: on a fresh own text: Edit, then Cancel (`leftButton`), and check the text is unchanged (`HomeChatPage.cancelEditDialog` exists but no test uses it). Edit again and tap Update without changing anything. Edit again, clear the text and tap Update. Edit again, set 2,050 characters and tap Update.
- Expected: Cancel keeps the text. Unchanged Update adds no red "Edited" prefix to `tvMsgTime` (FAIL expected: the right button always sends, CommonKeyboardFragment.kt:352-372). Empty text shows the field error "Message could not be empty" (:355-358, strings.xml:1201) and the dialog stays open. Over 2,000 characters shows "There is only 2000 Characters limit allowed to send" (:359-367). The dialog is not cancelable by back (:377), so there is no Escape equivalent to test.

### P2

**M11. In-chat search** (web #20, #104, #105, #143, #153; BUL-14, BUL-30).
- Steps: tap `ivSearch` (emoji_keyboard_layout.xml:278). The `laySearch` / `edittext_search` bar shows (fragment_home.xml:108-117). (1) Type 3 spaces and press IME Search. (2) Type an exact fresh marker and press IME Search, then tap `top_button` / `bottom_button` (:134/:142). (3) Change the text to `zzq-no-match-<ts>` and tap an arrow without IME Search. (4) Search your own display name. (5) Two devices: search B's name where B only replied to A's post. (6) Close with `btnBack2`.
- Expected: (1) snackbar "Please type text to search." (Home.kt:2647-2650, strings.xml:776). (2) `tvCount` reads 1 and the row is highlighted and scrolled into view. (3) the count drops to 0. FAIL expected: the arrows reuse the old result while `isEnableSearch` is set (Home.kt:2665-2690). (4) matches, because the sender name is searched (HomeChatAdapter.kt:716-721). (5) should match. FAIL expected: only the comment text is searched, not the comment sender (:708-715). (6) the composer is restored.

**M12. Scroll-to-bottom and jump to latest** (web #4, #22, #108, #145; BUL-21).
- Steps: open Bulletin (at least 8 rows) and check that the newest row is visible. Swipe the feed down several times so it moves away from the newest row and check `arrowDownCard` (fragment_home.xml:263). Tap it. Post a 1,500-character text ending in `END`, expand it with "Read more", scroll away and tap the arrow. Check `userHistoryBtn` is absent on Bulletin and present on Call Sheet.
- Expected: the feed opens on the newest row (Home.kt:799-817). The arrow shows only when the newest row is not fully visible (Home.kt:290-310). Tapping it brings the newest row into view and hides the arrow. For a long newest row it lands on `END` and the arrow goes away (BUL-21: lands at the top of the row). History shows only on Call Sheet (Home.kt:397). Note: `arrowDownCard` has no `visibility="gone"` in the XML, so also assert it is hidden on first open.

**M13. Older messages load on scroll up** (web #23).
- Steps: on a unit with 50 or more rows, count the rows and note the oldest marker. Swipe down to the top repeatedly or pull to refresh (`refresh`, fragment_home.xml:221).
- Expected: older rows appear above (`getChatListFromApi(PREVIOUS)` Home.kt:320-326, `getPreviousMessage` Home.kt:232-236).

**M14. Message text rendering** (web #50, #51, #52, #53, #55).
- Steps: post (a) `qa-clip-<ts>` padded to 1,200 characters ending `TAIL-<ts>`. (b) `<img src=x onerror=alert(1)> <b>bold</b> qa-xss-<ts>`. (c) `Film Café ©®™ *bold* _it_ مرحبا 12345 qa-chars-<ts>` with an emoji. (d) `https://example.com/qa-<ts>`, `see www.example.com qa-www-<ts>`, `javascript:alert(1) qa-js-<ts>`. (e) `qa+e2e@example.com qa-mail-<ts>`.
- Expected: (a) `tvMessage` ends with "... Read more" and TAIL is hidden. Tapping it shows TAIL and " Read less" (Extensions.kt:5568-5630, limit 1,000 in Constants.kt:263, strings.xml:3429-3430; web clips earlier, so use more than 1,000 characters). (b) shown literally. (c) every character round-trips unchanged through encryption and translation (HomeVm.kt:749-760, 860-871). (d) `tvMessage` uses `autoLink="all"` (chat_text_layout.xml:113), so on Android both https and `www.` become links (this differs from web) and `javascript:` does not. Tapping the https link leaves the app for a browser. (e) tapping the email address opens a mail chooser.

**M15. Long-press menu by message type** (web #72, #74, #19; strengthens #71).
- Steps: own document with no caption (MediaSystemPickerTest send), own voice note, own text. Open each menu, then close with back and with a tap outside.
- Expected: the document and audio menus offer Download and no Edit (`showSave` for non-text, Home.kt:1386; `showEdit` only when there is a caption, :1404) and no Image Reply. The text menu has no Download, Image Reply, Copy or Distribute (`showCopy=false`, `isShowDistribute=false`, :1389, :1408). The popup closes on back and on an outside tap.

**M16. Selection mode and delete variants** (web #75, #81, #82, #118, #141; BUL-09).
- Steps: post A and B, then long-press B and tap Delete. Check `layOption` / `tvCount` (fragment_home.xml:61/:86) and that `etMessage` is hidden. Tap A: count 2. Tap A again: count 1. Exit with `btnBack`: both rows remain. Delete again, action-bar Delete, No: the row remains. Delete A and B together with Yes: both are gone. Enter selection again and tap another unit chip, then return.
- Expected: counts as described. Only the long-pressed row has `imgSelection` selected (chat_text_layout.xml:233). No keeps the rows and Yes removes both (Home.kt:1419-1468). The unit switch leaves selection mode with nothing deleted (Home.kt:395).

**M17. Comments (replies)** (web #85, #89, #90, #91).
- Steps: reply to your own post and wait 5 s. Open the comment's menu (comment arrow, `CHAT_ENUM.ARROW_DOW`, Home.kt:1112-1125). Edit it, Delete it with No, then Delete it with Yes. Open Read By on the comment.
- Expected: the reply shows exactly once inside `rvMessageReply` (socket echo, Home.kt:600-620). The menu offers Edit, Delete and Read By (Home.kt:1360-1378). An edited comment shows "Edited" (HomeReplyHolder.kt:75-80). No keeps it and Yes removes it (Home.kt:2300-2330). Read By opens the read-receipt page.

**M18. Composer rules while replying** (web #87, #88).
- Steps: long-press, then Reply. Check `ivAttachment` and `ivRecord`. Type 3 spaces and Send. Cancel the reply with `llDelete`.
- Expected: `ivAttachment` and `ivRecord` are hidden while replying (CommonKeyboardFragment.kt:384-392). Spaces give "Please Type Message To Send" (:175, strings.xml:524) and the reply bar stays. After cancel the paperclip is back (`resetUi` :416-430).

**M19. Forward page content** (web #93, #94, #96; BUL-20). Part should FAIL.
- Steps: Forward, then Yes. On ForwardPage list the rows. Tap Home and read "Select Unit". Back out, tap Members, search a name that does not exist, then clear. Tap Remote Projects and back out.
- Expected: Home, Info, Confidential Info, Members, Group, Email (ForwardPage.kt:880-958) and Remote Projects (:1229). The unit list has no Calendar (:261) and no Call Sheet for a text message. FAIL expected: Call Sheet is added for text (:262-285). Search empties and restores the list. Remote Projects opens and posts nothing.

**M20. Read By content** (web #98, #126, #162; BUL-18).
- Steps: Read By User on a fresh post. Check the read and unread segments and the "N remaining" line (ReadByUserPage.kt:544-546). Search a name that does not exist, then clear. Two devices: with A's Read By open, B opens Bulletin.
- Expected: "No Users Found." for no match (FAIL expected, BUL-18: the lists go blank). Clearing restores the lists. B moves to the read list live.

**M21. Gallery (library) from the message menu** (web #21, #106, #135; BUL-19). Part should FAIL.
- Steps: post zl_test.pdf, long-press any row, tap Gallery (`Options.Gallery`, Home.kt:2268, 2279-2284). Check the tabs, open Docs, read the subtitle line. Close with back.
- Expected: tabs Media (n), Docs (n), Links (n) (HomeChatLibraryActivity.kt:184-186; Android has no Audio tab). The PDF is listed with a size that has a unit. FAIL expected: the raw `file_size` is shown (HomeChatLibraryActivity.kt:155, ChatLibraryAdapter.kt:121-123).

**M22. Unit strip: Calendar switch, labels, default unit** (web #27, #28, #31).
- Steps: read every `menuText` in `recTab`. Tap Calendar. Relaunch the app and note the selected chip.
- Expected: no label is blank or a raw key ending in `_label` (HomeUnitHolder.kt:19). Calendar hides the composer and shows the calendar container (Home.kt:340-357). After a relaunch exactly one chip is selected, and it is the profile's default unit or the first unit (HomeVm.kt:200-232).

**M23. Mic on Bulletin, none on Call Sheet** (web #43).
- Steps: empty composer on Bulletin, then on Call Sheet.
- Expected: `ivRecord` is visible on Bulletin and hidden on Call Sheet (`hideRecordingBtn`, Home.kt:413). The Call Sheet picker is document-only (Home.kt:966-983).

**M24. Document types** (web #12, #62, #63, #64; BUL-24). The CSV part should FAIL.
- Steps: push sample.xlsx, sample.docx and sample.csv to /sdcard/Download. Paperclip, Document, pick each one.
- Expected: each posts a document bubble named after the file. CSV FAIL expected: `text/csv` is missing from the picker MIME list (mediaHandler/contracts/FilePickerContract.kt:29-37), so the file is greyed out.

**M25. A PDF carrying JavaScript is refused** (web #17; BUL-23 Medium). FAIL expected.
- Steps: push evil-js.pdf. Paperclip, Document, pick it.
- Expected: refused with a message, and nothing is posted. Android has no PDF content check in the Home upload path (`startUpload`, Home.kt:943-965).

**M26. Edit a media caption** (web #65).
- Steps: send an image with a caption, long-press it, tap Edit. The "Edit description" dialog is prefilled (Home.kt:1685-1712). Change the text and save.
- Expected: the new caption shows with "Edited" (AttachmentVH.kt:249-256).

**M27. Two images picked together post two rows** (web #67).
- Steps: Gallery, `MediaPreviewPage.selectPhotos(2)`, then the send FAB, then preview send.
- Expected: `imageBubbleCount()` grows by 2.

**M28. Download from the menu** (web #100, #101).
- Steps: long-press an own PDF and tap Download. Repeat for an image.
- Expected: toast "File Downloaded Successfully." (Home.kt:2470-2500, strings.xml:3369). Without download rights the snackbar is `msg_download_right` (Home.kt:2251-2256).

**M29. A posted video plays with its picture** (web #150; BUL-34 High).
- Steps: after `MediaPreviewTest#sendVideoFromGallery`, tap the video bubble (`openFileListShowingPage`, Home.kt:1814-1835). Use `HomeChatPage.isPlayerOpen`.
- Expected: the player surface shows frames and playback advances. Today TwoUserChatTest only "soft-opens", which is logged and not asserted.

**M30. Persistence across a relaunch** (web #58, #83).
- Steps: post a marker and relaunch (terminateApp / activateApp): it is still shown. Delete a second marker and relaunch: it is still gone.

**M31. Translation** (web #119, #124, #138, #139, #140, #163; BUL-08, BUL-10, BUL-11).
- Steps: with the device language equal to the project language, check `translateHint` (fragment_home.xml:214), `translateSendToggle` (emoji_keyboard_layout.xml:35) and the menu on B's post. Switch the device or app language to French and repeat. Tap Translate on B's post. Two devices: A in French sends with the toggle on, and B reads it. Switch back to English and check the designation in the card header.
- Expected: same language gives no banner, no toggle and no Translate item (Home.kt:209, 1399). A different language shows the banner "Project language: ..." (strings.xml:1656) and Translate shows the translation. B sees exactly one "Translated message:" label (strings.xml:1288), in the project language. The designation returns to English. The sent text is really translated (BUL-11 is a backend issue).

**M32. 30-minute edit/delete window for a non-admin** (web #120, #165).
- Steps: a non-admin with posting rights posts, Edit opens, and Cancel. Then use a message more than 30 minutes old (pre-seeded, or move the emulator clock forward 31 minutes) and tap Edit, then Delete.
- Expected: Edit gives "You can't edit message after 30 Minutes" (strings.xml:3330; Home.kt:2159-2170; window `DIFFERENCE_IN_HOURS = 30` minutes, Constants.kt:1620). Delete gives the delete notice (Home.kt:2119-2122). Flag the wording: `you_cannot_delete_message_after_2_hrs` reads "You can't delete message after 2 Hours" (strings.xml:1546) although the window is 30 minutes.

**M33. Posting rights change arrives live** (web #154; BUL-33).
- Steps: admin A sets B to View on / Post off, then back to Post on. B keeps Bulletin open the whole time.
- Expected: B's `layNoMsg` / `tvRequest` switches to `layPostMsg` without a relaunch (`postingRightsListFlow` and `hasPostingRights` collectors, Home.kt:447-470, 666-670; `unitViewRights` socket :521-526).

**M34. An edit arriving live keeps the replies** (web #152; BUL-29).
- Steps: A posts and replies. B sees `rvMessageReply` under the post. A edits the post.
- Expected: B shows the edited text and the reply is still under it (`isMessageEdit` handler, Home.kt:541-553).

**M38. Emoji picker** (web #10, #56).
- Steps: tap `ivEmoji` (emoji_keyboard_layout.xml:228): `emojiPicker` (:377) shows. Type `qa-emoji-<ts>`, put the cursor at the start, pick the first emoji. Tap `etMessage` and Send.
- Expected: the picker opens and closes when the text box is tapped (CommonKeyboardFragment.kt:296-305). The emoji is appended at the end, like on web (`append`, :184-186), and the message posts with it.

### P3

**M35. Unit list load failure, empty list, skeleton** (web #37, #38, #136, #40; BUL-07 analogue).
- Steps: cold-start with the network off (cached units from Realm, UnitDbManager.kt:94-107). Clear app data, log in and open Home offline. Watch `loaderView` (fragment_home.xml:350) on a cold online load.
- Expected: no crash, and the cached units are listed. With no cache, a message or retry should show. Android currently ignores the error (`getUnitListFromServer` onError is empty, HomeVm.kt:341-342), so expect a blank Home like web BUL-07. The shimmer shows, then hides.

**M36. Unit strip overflow arrow** (web #35).
- Steps: on a project with more than 3 units, check `more_units_arrow`. Give a unit past position 3 an unread post.
- Expected: the arrow is visible, and `red_dot` shows for unread posts in hidden units (Home.kt:418, 752-765; fragment_home.xml:36/:46).

**M37. Create Home unit offers departments or members** (web #137, #155; BUL-35). FAIL expected.
- Steps: extend `CreateUpdateHomeUnitTest`: Settings, Admin, Create Unit on Home, Create New Unit.
- Expected per the sheet: a department or user choice. Android `MainUnitFragment` (bottomNav/settings/admin/units/MainUnitFragment.kt) offers the name only. The department picker exists only in `CreateEditUnitPage` for shooting units.

**M39. Low-value or adapted single checks** (10 web tests):
- #9 Typing `@a` opens no mention list (the Home composer has no tagging; a cheap negative check).
- #14 Back out of the image/document preview: `messageCount()` is unchanged (add to `MediaPreviewTest#previewOpens`).
- #130 A BMP picked from the gallery previews or is refused, and Home does not crash (BUL-02, Android listed as Not affected).
- #107 Row header: avatar, name, and `tvMsgTime` in the form "MMM dd, yyyy hh:mm a" (TextVH.kt:191-193; Android has no " at " unlike web).
- #157 A personal project opens `PersonalProjectPage` and has no Home tab (bottomNav/BottomNavigationActivity.kt:433-437).
- #102 Tapping a PDF bubble opens a viewer or external app (Home.kt:1838-1846). Assert it, rather than soft-logging as TwoUserChatTest does.
- #149 Scroll to load older rows and switch units at once: no error toast on the new unit (BUL-32; hard without a proxy).
- #115 Open Bulletin offline: no crash, no endless loader, and the composer is usable.
- #79 Edit or delete refused by the server. Not stubbable on Android. Note: delete is optimistic and server errors are ignored (HomeVm.kt:1063-1100), and a failed edit keeps the new text with status 1 (HomeVm.kt:838-843), so Android behaves differently from web here.
- #92 Reply while offline gives an error and adds no comment.

## CONFLICT

**C1. Message length limit** (web #144 "a 2,100-character message posts (web allows 8,000)") vs `HomeChatTest#overLimitMessageRejected` ("Message longer than 2000 characters is rejected with the limit error").
- Android source: `TEXT_LIMIT = 2000` (utils/Constants.kt:733), enforced at Send (CommonKeyboardFragment.kt:155-163) and in the edit dialog (:359-367). The message is "There is only %d Characters limit allowed to send" (strings.xml:530).
- Verdict: the Android test is correct for the current Android build. The web/Android difference is open as sheet BUL-17 (Android Open, expected 8,000). Keep the test, label it as documenting BUL-17, and flip it to "2,100 posts / 8,001 rejected" if product aligns the limit. Android has no live counter, so web #8 is web-only.

**C2. View-only member and Reply** (web #121 stub, #164 "RGT-02..06 ... a posting-rights question on Reply; no Edit, Delete, Forward or Share") vs `PostingRightsTest#viewOnlyUnitHidesReply` ("On a view-only unit the long-press menu does NOT offer Reply").
- Android source: `showReplyOption = !isGroupItem && status == 2 && viewModel.hasPostingRights()` (Home.kt:1384). Reply is hidden, not offered-then-questioned as on web.
- Verdict: the Android test is right per the Android source; this is a platform difference.
- On a view-only unit Android still shows Forward and Share (Home.kt:1388-1391, gated only on "not Call Sheet") and Edit/Delete (:1400, :1404). Tapping Forward or Share then gives "You do not have Posting Rights on this Home unit." (Home.kt:2226-2246, strings.xml:3335).
- Suggested addition to PostingRightsTest: on a view-only unit, tapping Forward, Share or Edit opens nothing and shows that snackbar. The rest of RGT (REQUEST bar, `layNoMsg`, `tvRequest`) is already covered by `PostingRightsTest#viewOnlyUnitBlocksPosting`.

## COVERED (web test -> Android test)

| # | Web test | Android test | Note |
|---|---|---|---|
| 1 | opens with composer and feed root | HomeChatTest#selectUnitOpensChat | window title part is web-only |
| 7 | empty / whitespace-only not posted | HomeChatTest#emptyInputHidesSend, #whitespaceNotSendable | |
| 11 | paperclip menu lists pickers, closes outside | HomeChatTest#attachmentPickerHasAllMediaTypes, #attachmentCancelSendsNothing | Android list is 8 types (Home.kt:974-983) vs web 6 |
| 13 | Location Upload opens, Cancel posts nothing | HomeChatTest#eachMediaTypeOpens | weak: add a message-count check |
| 25 | home loads with unit tabs and bulletin | HomeChatTest#unitsVisible | weak: assert Bulletin / Calendar / Call Sheet names |
| 29 | switch to Call Sheet and back | PostingRightsTest#everyUnitHasConsistentState, AllUnitsMediaTest | |
| 36 | units with view_access / enabled false have no tab | CreateUpdateHomeUnitTest TC7 "Disabled unit is no longer visible on Home" | enabled part only; view_access filter at UnitDbManager.kt:97-99 is untested |
| 46 | post text: row, sender, cleared composer | HomeChatTest#sendValidMessage | |
| 47 | mic becomes Send, Send posts | HomeChatTest#typingRevealsSend, #emptyInputHidesSend, #sendValidMessage | |
| 59 | pending row shows clock until ack | HomeChatTest#sendValidMessage (`waitMessageSent` requires the clock to clear) | weak |
| 61 | PDF uploads as doc bubble with name / size | MediaSystemPickerTest#sendDocument; CallSheetTest "PDF posts with file size" | weak on Bulletin: name and size not asserted |
| 66 | image uploads with caption | MediaPreviewTest#captionAcceptsText, #sendImagePostsWithLoader | caption in bubble not asserted |
| 68 | audio file uploads as audio bubble | MediaSystemPickerTest#sendAudio | |
| 70 | Image Reply posts a new image row | ChatOptionsTest#imageReplyOnMedia | |
| 71 | own text menu has expected items, nothing forbidden | ChatOptionsTest#textMessageOptions | weak: no absence asserts (see M15) |
| 73 | own image adds Download and Image Reply | ChatOptionsTest#imageMessageOptions | |
| 76 | edit own message: Update, Edited | ChatOptionsTest#editMessagePersists; TwoUserChatTest#editBothWays | weak: prefill and "Edited" tag not asserted |
| 80 | delete own message via the selection flow | ChatOptionsTest#deleteOption; TwoUserChatTest#deleteBothWays | |
| 84 | reply nests as a comment | HomeChatTest#replyToOwnMessage | weak: nesting in `rvMessageReply` not asserted |
| 86 | reply quote names sender, can be dismissed | HomeChatTest#longPressShowsReply, #cancelReplyPreview | weak: sender name in `llReply` not asserted |
| 97 | Share puts selected messages into a share target | ChatOptionsTest#shareOption | weak: Android Share uses selection mode plus the system share (Home.kt:2532-2595); web uses Compose Email |
| 103 | image bubble opens preview, back closes | MediaPreviewTest#openSentMediaViewer | |
| 116 | message from a second context appears live | TwoUserChatTest#textBothWays | |
| 123 | VOI stub (skipped) | RegressionStressTest#recordAndSendAudio | |
| 125 | edits / deletes / comments live stub (skipped) | TwoUserChatTest#editBothWays, #deleteBothWays, #otherUserMessageInteractions | |
| 128 | Location Select posts map row stub (skipped) | ManualPickSendTest "LOCATION ..." | manual; location tool deferred by design |
| 129 | regression: 50 varied messages land | RegressionStressTest#rapidTextMessages | weak: 12 plain markers; content mix in M14 |
| 158 | Location Select posts browser location | ManualPickSendTest "LOCATION ..." | as #128 |
| 159 | VOI: timer, stop + Send posts, bin discards | RegressionStressTest#recordAndSendAudio; TwoUserChatTest#audioBothWays | bin discard (`canelAudiRec`, emoji_keyboard_layout.xml:82) and `audioTimer` are not asserted |
| 161 | another account's post / edit / comment / delete live | TwoUserChatTest#textBothWays, #editBothWays, #deleteBothWays, #otherUserMessageInteractions | |

## WEB-ONLY

| # | Web test | Why not applicable on Android |
|---|---|---|
| 2 | scoped composer resolves to exactly one element | DOM scoping across MDI windows; Android Home hosts a single `keyboardLayout` fragment (fragment_home.xml:293) |
| 3, 39 | feed / rail theme toggle | no theme control in Home: `layTabs` holds only `recTab`, `more_units_arrow`, `red_dot` (fragment_home.xml:10-57) |
| 5 | every row has an options button; counter is aria-live | Android uses long-press menus (TextVH.kt:160-178); no counter exists |
| 6 | first character auto-capitalised (incl. paste) | Android only sets the IME hint `textCapSentences` (emoji_keyboard_layout.xml:261); no programmatic capitalisation; Appium setValue bypasses the IME. Manual only |
| 8 | composer caps at 8,000 with a counter from 7,200 | no `maxLength` or counter on `etMessage` (emoji_keyboard_layout.xml:251-265); the limit (2,000) is checked at Send, see C1 |
| 15, 16, 18, 131 | pickers reject gif / exe / txt / upper-case .EXE after picking | Android pickers filter by MIME before the pick (FilePickerContract.kt:29-43; in-app gallery for photo/video); there is no post-pick validation or toast |
| 132 | every offered video type (.mkv .wmv .3gp .mpeg) accepted | Video Gallery is an in-app MediaStore picker with no extension check; sheet BUL-04 says Android Not affected |
| 24 | minimise / restore window keeps feed and draft | MDI window; the Android analogue (leave the Home tab and return) is in M2 |
| 26, 30 | roving tabindex; arrow / Home / End keys | keyboard focus model of the web tab strip |
| 32 | `history_identifier` marker picks the unit once | browser storage marker |
| 34 | switching tabs keeps the URL | browser URL |
| 44, 45 | MDI project lands on /workspace; close the Home window and reopen it | workspace window layout |
| 117 | Edit modal renders inside the tool window | MDI portal |
| 48, 49 | Shift+Enter newline; Ctrl+Enter sends | `etMessage` is `textMultiLine`, so Enter inserts a newline and only `ivSendMessage` sends |
| 54 | own mention token renders as own name | the Home renderer `handleChatMessage` (bottomNav/home/adapter/holder/AttachmentVH.kt:603-660) has no `@{{id}}` substitution (C&C has it, chatAndGroupChat/viewholders/HoldersViewhandler.kt:329). Worth one manual check that web-posted mentions show raw braces on Android |
| 69 | dropping an image on the feed | drag-and-drop from desktop |
| 99 | Publish to Doc Distribution on a Bulletin document (skipped) | Android offers Distribute to DD only on Call Sheet documents (Home.kt:1409) |
| 110, 111 | Bulletin History (skipped) | Android History exists only on Call Sheet (`userHistoryBtn`, Home.kt:397), covered by CallSheetTest "Call-sheet unit shows the History button" |
| 127, 156 | custom units after one divider (stub and real) | `line` in custom_tabbar_item.xml:42 is always gone and never toggled (HomeUnitHolder.kt). The personal-project half of #127 is in M39 (#157) |
| 133 | Gallery search finds an image by caption | HomeChatLibraryActivity has no search |
| 134 | non-poster cannot forward others' Gallery items | Android library cards have no per-item menu (ChatLibraryAdapter.kt) |

## Sheet bugs: Android relevance

- **Would be caught by a proposed Android case:**
  - BUL-08, BUL-10, BUL-11 (M31)
  - BUL-09 (M16)
  - BUL-12 (M10)
  - BUL-13 (M7)
  - BUL-14, BUL-30 (M11)
  - BUL-16 (M5)
  - BUL-17 (C1, already "caught" by HomeChatTest)
  - BUL-18 (M20)
  - BUL-19 (M21)
  - BUL-20 (M19)
  - BUL-21 (M12)
  - BUL-22 (M6)
  - BUL-23 (M25)
  - BUL-24 (M24)
  - BUL-25 (M4)
  - BUL-26 (M8)
  - BUL-27 (M2)
  - BUL-28 (M3)
  - BUL-29 (M34)
  - BUL-32 (M39)
  - BUL-33 (M33)
  - BUL-34 (M29)
  - BUL-35 (M37)
- **Android-only bug with no web test:** BUL-15 (deny media permissions, then Document does nothing). Add a small case: `pm revoke` READ_MEDIA_*, paperclip, Document, deny both prompts. Expect a message or the picker; today nothing happens.
- **Not relevant to Android Home:** BUL-02, BUL-03, BUL-04, BUL-05 (sheet says Android Not affected or the feature is absent); BUL-06 (no Gallery item menu on Android); BUL-07 (analogue only, M35); BUL-31 (Film Tools, not Home).

## Other observations (Android tests vs Android source)

- `TwoUserChatTest#otherUserMessageInteractions` asserts device A cannot delete B's message. If account A is an admin, Android allows it (`setDeleteAction` accepts `isAdmin`, Home.kt:1422; `handleMultiSelection` :1308-1309), so the assert depends on A being a non-admin. Pin the account role or branch the assertion.
- `ChatOptionsTest#readByOption`, `#forwardOption` and `#shareOption` pass on "input bar not visible OR text shown". They can pass without the target screen opening. M19 and M20 give concrete asserts.
