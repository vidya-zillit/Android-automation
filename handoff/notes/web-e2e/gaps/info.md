# Info: web E2E cases vs Android Appium suite

Date: 2026-11-23. This is a read-only comparison; nothing was run.

Inputs:
- **Web:** `web-e2e/info.md`, with 131 tests and 32 sheet bugs. The suite files are `unit-feed.suite.js`, `info.spec.js` and `sheet-bugs/info.spec.js`.
- **Android tests:** `Android-automation/src/test/java/com/zillit/automation/tests/tools/InfoTest.java`. This only sets `moduleName()`; all 15 cases come from `ToolChatBaseTest.java`.
- **Android app:** `Zillit-Android/app/src/main`. Info opens `bottomNav/tools/confidentialInfo/ConfidentialInfo.kt` (`Tools.kt:589-594`), the same activity as Confidential Info.

## Summary

| Class | Web tests | Groups |
|---|---|---|
| COVERED | 14 | 13 rows (most only partly: see the notes) |
| MISSING | 101 | 25 shared groups + 6 Info groups |
| WEB-ONLY | 16 | 13 rows |
| CONFLICT | 0 | No Android case asserts the opposite of web. Platform differences and weak Android assertions are listed under the shared section |
| **Total** | **131** | |

The Android suite has 15 cases. Web has 131, of which 96 are the shared `unit-feed.suite.js` block, which is identical in Confidential Info.

### Top missing cases (priority order)
1. **G-SEARCH:** in-feed search (find, count, arrows, re-search, close). Covers 3 shared tests and 3 sheet-bug tests. Includes open Android bug INFO-20.
2. **G-RIGHTS:** view-only member sees the REQUEST bar; no view access means no tile and the screen closes; live rights changes. 10 tests. Needs account B.
3. **G-OFFLINE:** failed send shows Retry and resends; offline edit and delete are refused. 6 tests.
4. **G-MENU:** long-press option matrix per message type (text, image, document, audio). 4 tests.
5. **G-HISTORY:** History opens, deleted marker is listed, failed History is not silently blank. 3 tests.
6. **G-UPLOAD:** PDF/xlsx/docx/csv documents, image with caption, two images, audio. 7 tests; CSV is known Android bug INFO-27.
7. **G-COMMENT / G-REPLY:** reply bar, whitespace reply, comment edit/delete/Read By. 7 tests.
8. **G-SELECT / G-DELETE:** selection count, back-out, multi-delete, gone after reopen. 4 tests.
9. **G-LIMIT:** the Android 2,000-character limit versus web's 8,000 (INFO-25). 2 tests.
10. **G-DRAFT:** unsent draft survives a reopen (likely fails: INFO-23). 2 tests (1 shared + 1 sheet-bug).

## Info-specific cases (35 tests: shell, skipped placeholders, sheet-bug tests)

### MISSING (Info-specific)

#### G-RIGHTS: posting and viewing rights (10 tests)
Web tests:
- view-only member sees REQUEST bar (info.spec.js:86 skipped; :241 passed)
- granting/revoking posting flips the composer live (:87 skipped; :264)
- INFO-17 granting posting enables the open composer and Info stays open (sheet-bugs/info.spec.js:260)
- turning off View closes Info without a posting-only message (:287)
- rights changed while Info is open apply at once (:328)
- without view access Info closes back to Tools, not "No Unit found" (:238, stub)
- view-only member sees a post's comments read-only (:245, stub)
- non-admin with download rights can download (:132, stub)

Plus, counted in other groups: forwarding to a switched-off Confidential Info (:534, stub) is in G-FORWARD-INFO.

Why: this is the core permission model and nothing on Android covers it. `ToolChatBaseTest` simply *skips* when rights are missing.

Proposed cases (a new `InfoRightsTest`; uses the admin settings page objects to change account B's rights, or a project where B is pre-configured view-only):
1. **View-only shows REQUEST.** As view-only B, open Info. Expect `layNoMsg` + `tvRequest` "REQUEST" (`isRequestRightsButtonVisible`) and no `etMessage`. Tapping REQUEST sends a request to the admins (`CI.kt:249-260`).
2. **No view access.** The Info tile is absent from Tools (`Tools.kt:346`). If B reaches Info another way, for example from a notification, the screen closes back to Tools (`CI.kt:358-368`, `finish()`), not to Home's "No Unit found".
3. **Live grant/revoke** (documents INFO-07/INFO-17/INFO-30). With Info open on B, the admin grants posting and `etMessage` should appear without a reopen. Revoke posting and expect the REQUEST bar. Revoke view and Info closes.
4. **View-only sees comments read-only.** A post with comments shows its `rvMessageReply` to B, and the comment menu offers no Edit/Delete (INFO-08).
5. **Download right.** A non-admin with download access gets Download working on a PDF. Without it, they get "You do not have Downloading Rights" (INFO-31).

#### G-LIVE-INFO: live updates from another account (2 tests)
Web: edits, deletes and comments from another device arrive live (info.spec.js:89 skipped; :219 passed).
Proposed case (two devices or two Appium sessions): B posts, edits, comments and deletes; A's open Info reflects each change without a reopen. Socket events: `INFO_MESSAGE_ADDED/EDITED/...` (`BaseSocketListener.kt:4004+`).

#### G-BADGE: tile and tab badges (2 tests)
Web: another member's post badges the tile and the tab, cleared on open (info.spec.js:88 skipped; :165 failed).
Proposed case: B posts in Info while A is on Tools. A's Info tile shows a badge count (`Tools.kt:1433-1434`). A opens Info and goes back, and the badge is cleared (`readBadges()`, `CI.kt:272-279`). Related to INFO-18 (the "New message received" banner shows while already viewing Info).

#### G-CUSTOM: Info cannot be switched off (3 tests)
Web: Info cannot be disabled per project (info.spec.js:90 skipped; :104 failed; :104 passed variant).
Android: `ToolsEnableDisablePage.kt` has no Info special-case in the client. `ToolsManagementTest.disable...` deliberately avoids Info and Confidential Info (`ToolsManagementTest.java:192`), so nothing proves either way.
Proposed case: open Customization of tools (`toolsCustomization`). The Info row is either absent, or ticked and not toggleable. Tapping it leaves it ticked after Update. Verify the expected behaviour with product first, because the server may enforce it.

#### G-FORWARD-INFO: forwarding edge cases (3 tests)
Web:
- Forward In App from Info opens "Home Tab" onto its units (sheet-bugs:@smoke; INFO-02, Android not affected)
- forwarding to a switched-off Confidential Info is hidden or explained (stub; INFO-06)
- a forwarded copy forwarded to a member reaches them (account B; INFO-29 **High, Android Open**)

Proposed cases:
1. Covered by shared G-FORWARD #1 (Home → Bulletin).
2. **Forward a forwarded copy to a member.** Forward M from Info to Info (a copy), then forward the copy to member B's C&C. B receives it. This documents INFO-29; per the sheet it is silently dropped today.
3. With Confidential Info switched off for the project, Forward → Confidential Info is hidden or says the tool is off, rather than "no rights". Low priority; it needs a project setting.

#### Info sheet-bug tests mapped to shared groups (10 tests)
| Web sheet-bug test | Group | Note |
|---|---|---|
| a 2,100-character message posts (web allows 8,000) | G-LIMIT | Android refuses it at 2,000 (INFO-25) |
| Gallery Docs shows the file name and a size with a unit | G-GALLERY | INFO-24, Android Open |
| a hyphenated word is found by search, with a count | G-SEARCH | INFO-09. Add `qa-hyphen-word` to search case #1 |
| search runs again when the word changes | G-SEARCH | INFO-20, Android Open (`CI.kt:2262-2266`) |
| closing search leaves the cards looking as before | G-SEARCH | INFO-12 |
| the floating buttons cover no text or button of the newest card | G-FAB | INFO-11, Android **Open**. Assert the `arrowDownCard`/`userHistoryBtn` bounds do not overlap the newest row's `tvMessage` |
| a restored one-line draft opens the composer at one-line height | G-DRAFT | INFO-13, low |
| a comment shows its author and designation in full | G-COMMENT | INFO-14. Assert the author `TextView` is not ellipsized (`layout.getEllipsisCount`, which needs a screenshot or text compare) |
| Info History whose request fails is not blank and silent (stub) | G-HISTORY | INFO-28, Android Open. Open History offline |
| another member's comment offers no Edit | G-OTHER | INFO-16, Android **Open**: Edit is always shown on comments (`CI.kt:1222`) |

#### G-HEADER: title (1 test)
Web: the single unit tab reads "Info" and is selected (info.spec.js:50).
Android has no tabs. Proposed case: on open, the `toolbar` title equals the Info tile label (`Tools.kt:589-594`, header from `toolsInfo.unitName`). Fold it into `moduleOpens`.

### COVERED (Info-specific)
| Web test | Android test |
|---|---|
| opens from the Tools tile (info.spec.js:58) | `ToolChatBaseTest.enterModule/openModuleChat` + `moduleOpens` |

### WEB-ONLY (Info-specific)
| Web test | Why | Evidence |
|---|---|---|
| the Back button exists only on the classic layout (info.spec.js:66) | Workspace window layouts | Android always has the toolbar back |
| confidentiality parity: Info text is selectable and nothing is blurred (:73) | CSS blur and DOM selection | No blur in `activity_confidential_info.xml` / `chat_text_layout.xml` |
| an upper-case .EXE is refused like .exe (sheet-bugs) | Browser file-input extension check | Android filters by MIME (`FilePickerContract.kt:29-38`); sheet INFO-05 says Android is not affected |

## Sheet bugs (32): do they apply to Android?

The Android column is the sheet's own status; the third column is what the source shows.

| Bug | Android (sheet) | Source check / covering case |
|---|---|---|
| INFO-02 forward to Home Bulletin | Not affected | G-FORWARD #1 guards it |
| INFO-05 upper-case .EXE | Not affected | MIME filter, web-only |
| INFO-06 forward to switched-off CI says "no rights" | Not checked | G-FORWARD-INFO #3 |
| INFO-07 granted posting needs a reload | Not checked | Likely applies: there is no Info posting-rights listener in `CI.kt`. G-RIGHTS #3 |
| INFO-08 view-only sees empty Comments | Not checked | G-RIGHTS #4 |
| INFO-09 hyphenated search | Not affected | G-SEARCH |
| INFO-10 View off says "Posting rights revoked" | Not checked | G-RIGHTS #3 |
| INFO-11 floating button covers newest card | **Open** | G-FAB sheet row |
| INFO-12 search styling after cancel | Not affected | G-SEARCH #5 |
| INFO-13 one-line draft height | Not affected | G-DRAFT, low |
| INFO-14 comment author cut off | Not affected | G-COMMENT, low |
| INFO-15 "Nm ago" times frozen | Not affected | Android uses absolute times (`TextVH.kt:194`); n/a |
| INFO-16 Edit offered on another user's comment | **Open** | Confirmed: `CI.kt:1222` `showEdit = true`. G-OTHER |
| INFO-17 posting back on closes Info | Not checked | G-RIGHTS #3 |
| INFO-18 "New message" banner while viewing Info | Not checked | G-BADGE |
| INFO-19 unchanged Update marks "Edited" | **Open** | G-EDIT #4 |
| INFO-20 arrows don't re-search | **Open** | Confirmed: `CI.kt:2262-2266`. G-SEARCH #4 |
| INFO-21 Read By no-match shows nothing | **Open** | Extend `readByUser`: search "zzz" and expect an empty-state text |
| INFO-22 Info History has no search | **Open** | `searchHolder` hidden at `UserChatHistoryPage.kt:239`. G-HISTORY #3 |
| INFO-23 unsent text lost on leave | **Open** | Likely cause: `KB.kt:129` vs `CI.kt:647`. G-DRAFT #1 |
| INFO-24 Gallery Docs bare size / storage path | **Open** | G-GALLERY |
| INFO-25 Android 2,000 vs web 8,000 | **Open** | Confirmed: `Constants.kt:733`. G-LIMIT |
| INFO-26 PDF with JS can be posted | **Open** | No check in the source. G-PDFJS |
| INFO-27 CSV can't be attached | **Open** | Confirmed: no `text/csv` in `FilePickerContract.kt:29-38`. G-UPLOAD #2 |
| INFO-28 History blank offline | **Open** | G-HISTORY / G-OFFLINE #3 |
| INFO-29 forwarded copy dropped when forwarded to a member | **Open (High)** | G-FORWARD-INFO #2 |
| INFO-30 rights change never applied while open | Not checked | Applies at least to CI (listener commented out, `BaseSocketListener.kt:3915`). G-RIGHTS #3 |
| INFO-31 download refused despite rights | Not checked | G-RIGHTS #5 |
| INFO-32 no History view | Not checked | **Not applicable**: Android has `userHistoryBtn` |
| INFO-33 no "Distribute to Document Distribution" | Not checked | **Not applicable**: "Publish to Doc Distribution" exists (`CI.kt:1270-1272`). G-DD |
| INFO-34 socket frames from other productions | Not checked | Needs two projects; not proposed |
| INFO-35 switched-off Info shows "No Unit found" | Not checked | G-RIGHTS #2 (Android `finish()`es) |

---

## Android facts this report relies on

Paths below are short forms:
- `CI.kt` = `Zillit-Android/app/src/main/java/com/zillit/zillitapp/bottomNav/tools/confidentialInfo/ConfidentialInfo.kt`. One activity serves **both** Info and Confidential Info (`Tools.kt:552-594`).
- `KB.kt` = `.../baseUtils/CommonKeyboardFragment.kt`, the composer.
- `TextVH.kt` / `AttachmentVH.kt` = `.../bottomNav/home/adapter/holder/`.
- Layout and string files are under `app/src/main/res/`.

| Topic | What the Android source does |
|---|---|
| Layout ids | `activity_confidential_info.xml`: `toolbar layOption btnBack tvCount laySearch edittext_search top_button bottom_button layAction layButton imgAction tvAction rvChat refresh userHistoryBtn arrowDownCard layPostMsg keyboardLayout layNoMsg tvRequest empView` |
| Composer | `etMessage` uses `inputType=textCapSentences|textMultiLine` and has no maxLength (`emoji_keyboard_layout.xml:251-265`). The bar also holds `ivEmoji`/`emojiPicker`/`backPress`, `ivSearch`, `ivAttachment`, `ivRecord` and `ivSendMessage` (`KB.kt:136-208`). |
| Text limit | `Constants.TEXT_LIMIT = 2000` (`Constants.kt:733`). Sending, and saving an edit, over the limit shows "There is only 2000 Characters limit allowed to send" (`KB.kt:155-162`, `KB.kt:359-368`). Typing past it shows the same snackbar (`CI.kt:636-653`). There is no counter. Web allows 8,000 and shows a counter from 7,200. |
| Empty send | A blank or space-only send shows "Please Type Message To Send" (`KB.kt:173`, `CI.kt:381-384`). |
| Draft | Typing saves the draft to `SharedPref.setUserCacheMessage` (`CI.kt:647`), but the composer restores it from the in-memory `AppHelper.userCacheMessage` (`KB.kt:129`). This mismatch is a likely cause of sheet bug "unsent text lost". |
| Picker | Info and Confidential Info offer 8 options: Camera(Photo), Gallery, Video, Video Gallery, Audio, Document, Location, Contact (`CI.kt:784-797`). Web offers 6. The Document MIME allow-list is doc/docx/ppt/pptx/ppsx/xls/xlsx/txt/pdf and has **no CSV** (`mediaHandler/contracts/FilePickerContract.kt:29-38`). Nothing checks for JavaScript inside a PDF. |
| Long-press menu | `CI.kt:1229-1281`, labels from `utils/OptionsHandler.kt`. **Reply** and **Read By User** need status 2. **Download** (`Options.Save`) shows for anything that is not text or location. **Image Reply** (string `edit_and_post`) shows on images when you have posting rights. **Edit** shows whenever the message has text or a caption, *including other people's messages*; tapping it on someone else's message is refused with "You Don't Have Permission To Edit This Message." (`CI.kt:1770-1774`). **Publish to Doc Distribution** shows for non-text, non-location messages (`CI.kt:1270-1272`, `strings.xml:5032`). **Forward**, **Share**, **Delete** and **Gallery** always show. **Translate** shows only on other people's messages when translation is on. |
| Comment menu | Opened from the comment chevron (`CHAT_ENUM.ARROW_DOW`, `CI.kt:985-996`). It offers Read By User, Edit (always shown, `CI.kt:1222`) and Delete with a No/Yes confirm (`CI.kt:1911-1931`). Editing someone else's comment is refused with "You can't edit other users message" (`CI.kt:1991-1996`). |
| Edit | Text edits open the "Edit" dialog: `editTextInput`, `leftButton` = Cancel, `rightButton` = Update (`KB.kt:333-381`). An empty edit shows the field error "Message could not be empty" and the dialog stays open. Edit is time-gated by `isShowEditOption`: the string says "You can't edit message after 30 Minutes". Media captions use `showEditDescriptionDialog` (`CI.kt:1539-1566`). An edited row shows a red "Edited" before the time (`TextVH.kt:182-194`). |
| Delete | Long-press, then Delete, enters selection mode: `layOption`/`layAction` appear, `tvCount` starts at 1 and the composer is hidden. Tapping the `tvAction` "Delete" opens "Are you sure you want to delete?" with No/Yes (`CI.kt:1288-1338`). `btnBack` leaves selection mode (`CI.kt:1590-1603`). A non-admin deleting someone else's message gets "You have no permission to delete other user message." |
| Share | Share also enters selection mode with a `tvAction` "Share". Tapping it opens the **Android share sheet** (`openShareMultipleIntent`, `CI.kt:2128-2194`). Web opens Compose Email instead. |
| Search | The `ivSearch` button in the composer opens `laySearch`. Pressing the IME search key in `edittext_search` runs the search, and `tvCount` shows the number of matches. `top_button`/`bottom_button` step through the matches. Searching for nothing shows "Please type text to search." Once one search has run, the arrows **do not re-run** for a changed word because `isEnableSearch` stays true (`CI.kt:2225-2296`), which is sheet bug INFO-20/CONF-18. |
| Scroll | `arrowDownCard` shows only while the feed is scrolled away from the latest row, and tapping it scrolls to the bottom (`CI.kt:307-330`). Pull-to-refresh (`refresh`), or reaching the top, loads the older page (`CI.kt:244-248`, `CI.kt:341-347`). |
| History | `userHistoryBtn` opens `UserChatHistoryPage`: its toolbar uses the header, `headerMsg` reads "Records of deleted messages", `searchHolder` starts hidden (`UserChatHistoryPage.kt:239/308`) and rows offer Download/View (`:1033-1045`). |
| Gallery | The menu's **Gallery** item opens `HomeChatLibraryActivity` with **3** tabs, Media / Docs / Links, each with a count (`HomeChatLibraryActivity.kt:87-89,185-186`). Web has 4 tabs. |
| Rendering | `tvMessage` has `autoLink="all"` (`chat_text_layout.xml:113`), so URLs (including `www.`), e-mail addresses and phone numbers become links. "Read more"/"Read less" appear above 1,000 characters (`AttachmentVH.kt:640-653`, `Constants.kt:263`). The timestamp is formatted `MMM dd, yyyy hh:mm a` (`TextVH.kt:194`); web shows "Mon DD, YYYY at hh:mm AM/PM". A pending row shows `ivClock`; a failed row shows `btnTryAgain` "Retry" (`TextVH.kt:210-233`). The Info renderer has no mention-token parsing (`AttachmentVH.kt:603-660`). |
| Retry | `CHAT_ENUM.RETRY` needs internet and posting rights. With several pending items it asks "This file" / "Upload all (n)" (`CI.kt:1002-1100`). |
| Rights | No view access makes the screen `finish()`. No posting right swaps `layPostMsg` for `layNoMsg` with the `tvRequest` "REQUEST" button (`CI.kt:358-368`, `strings.xml:922`). The Tools list drops tiles whose `viewAccess == false` (`Tools.kt:346`). The Confidential Info posting-rights socket listener is **commented out** (`BaseSocketListener.kt:3915`). Message, edit, delete and comment socket events are live (`BaseSocketListener.kt:3927-3975`, `:4004+`). |
| Badges | The tile badge comes from `badgesData.confidentialTotal` / the Info count (`Tools.kt:1431-1434`). Opening the screen calls `readBadges()` (`CI.kt:272-279`). |

## Shared feed cases (the same 96 `unit-feed.suite.js` tests run on Info and Confidential Info)

Refs are `unit-feed.suite.js:<line>` and are the same in both web files.

### Summary of the shared block

| Class | Smoke (24) | Feed (72) | Total (96) |
|---|---|---|---|
| COVERED | 4 | 9 | 13 |
| MISSING | 12 | 58 | 70 |
| WEB-ONLY | 8 | 5 | 13 |
| CONFLICT | 0 | 0 | 0 |


### MISSING (shared), in priority order

Each group gives the web tests, why it matters, and a proposed Android case. Proposed cases belong in `ToolChatBaseTest` so both modules get them, unless marked otherwise. Page-object helpers that already exist in `HomeChatPage.java` are named.

#### G-SEARCH: in-feed search (3 shared tests, plus sheet-bug tests in the module section)
Web: search edge cases (:1493); search finds, highlights and scrolls (:1471); search by own sender name (:1520).
Why: the feature exists on Android (`ivSearch` → `laySearch`) and has no case. It also carries open Android bug INFO-20/CONF-18.
Proposed cases:
1. **Search finds a posted marker.** Post marker M, then tap `ivSearch`. Expect `laySearch` and `edittext_search` visible and the composer hidden. Type M and press IME Search. Expect `tvCount` = "1" and M scrolled into view.
2. **Empty search is refused.** Open search and press IME Search with an empty field. Expect snackbar "Please type text to search."
3. **No-match and special characters.** Search "zzqq-nomatch-<ts>" and expect `tvCount` = "0". Search `.*[(` and expect no crash and the count shown.
4. **Changing the word re-runs search** (documents INFO-20/CONF-18). Search M1, then change the text to M2 and tap `top_button`. Expect `tvCount` to reflect M2. This fails today because of `CI.kt:2262-2266`.
5. **Closing search restores the feed.** Tap `btnBack` and expect `laySearch` gone, the composer back and no highlight.
6. **Search by own name** lists your own rows: expect `tvCount` >= 1.

#### G-HISTORY: deleted-message History (2 shared tests, plus module-specific ones)
Web: History opens with heading, alert and search (:1661); deleted message shows in History, with PDFs offering View/Download/Print (:1681).
Proposed cases:
1. **History opens and comes back.** Tap `userHistoryBtn`. Expect `UserChatHistoryPage`, a toolbar titled with the module name, and `headerMsg` = "Records of deleted messages". Press Back and expect `rvChat` and the composer.
2. **A deleted marker appears in History.** Delete marker D with the existing `deleteMessage` flow, open History and expect D listed. Long-press a deleted PDF row and expect Download/View (`UserChatHistoryPage.kt:1033-1045`).
3. **History search** (documents INFO-22/CONF-20). Expect `searchView`/`searchHolder` to be reachable. It starts hidden (`:239`).

#### G-OFFLINE: failure and Retry (6 shared tests)
Web: offline send errors and can be resent (:1709); POST status 0 marks the row failed (:1726, needs a stub); a failed message can be deleted from the retry menu (:1750); a failing edit keeps the text and a refused delete keeps the row (:932); a failing comment POST shows a toast (:1189); a failing page GET leaves the composer usable (:1783).
Proposed cases (toggle connectivity with `driver.setConnection` or Wi-Fi/data off; no adb during the run):
1. **Offline send shows Retry, and Retry resends.** Go offline and send M. Expect `ivClock`, then `btnTryAgain` "Retry". Go online and tap Retry. Expect `waitMessageSent(M)` and exactly one M. Known Android bug CONF-25 is that the row stays on the clock with no Retry until reopened.
2. **Offline edit and delete are refused.** Go offline, long-press, Edit, change the text and tap Update. Expect snackbar "Internet connection lost. Please check your internet connection and try again." or "No Internet Connection", with the text unchanged. Repeat with Delete and expect the row to stay.
3. **Feed opens offline** without a crash: the composer is enabled and History is not silently blank (INFO-28/CONF-26).
4. Status-0 POST (:1726) and comment-POST failure (:1189) need a server stub. Defer them, or approximate with offline mode.
5. :1750 (delete a failed row with no server call): Android's Delete path needs internet (`CI.kt:1308-1310`). Verify the behaviour first, then assert it.

#### G-MENU: long-press menu by message type (4 shared tests)
Web: own text offers the expected menu and nothing forbidden (:750); own document without caption adds Download and drops Edit (:765); own image adds Download and Image Reply (:787); own audio adds Download and has no Edit (:802).
Our `longPressShowsOptions` only prints `menuOptions()`.
Proposed case **"Menu matrix per type"**, asserting `menuOptions()` for your own delivered messages:
- text: contains Reply, Read By User, Forward, Share, Delete, Edit, Gallery; no Download, Image Reply or Publish to Doc Distribution.
- image with a caption: also Download, Image Reply, Publish to Doc Distribution, Edit.
- document without a caption: Download and Publish to Doc Distribution; **no** Edit or Image Reply.
- audio: Download; no Edit.
Source: `CI.kt:1229-1281`.

#### G-UPLOAD: attachment uploads (7 shared tests)
Web: PDF (:556), xlsx/docx/csv (:570 x3), image with caption (:604), two images at once (:615), audio (:631).
Our only upload is `sendImageMessage` with no caption, so caption and size are not asserted.
Proposed cases (reuse `tapAttachmentOption("Document")`, `pickSystemFileByName`, `waitAttachmentSent`, `docBubbleCount`, `audioBubbleCount`, `setPreviewCaption`):
1. **PDF, xlsx and docx documents.** Push fixtures in setup. Each posts as a document bubble showing its file name, and `docBubbleCount` goes up by 1.
2. **CSV** (documents INFO-27/CONF-23). The CSV is not pickable because the Android MIME list has no `text/csv`. Assert it is selectable, as on web, so the case fails until the app is fixed. Mark it `@Tag("known-bug")`.
3. **Image with caption.** `sendOneImage("cap-<ts>")`: the caption shows on the image row.
4. **Two images in one pick** produce two `imageParent` rows.
5. **Audio file** via Audio: an audio bubble appears (`audioBubbleCount` +1). An Android-only voice note through `ivRecord` is worth one case too (`tapRecord`/`sendRecording`).

#### G-COMMENT and G-REPLY: comments (replies) (7 shared tests)
Web, comments: edit own comment (:1129); delete own comment with No then Yes (:1153); comment offers Read By (:1174); own comment renders once (:1071).
Web, reply composer: quote names the sender and can be dismissed (:1086); paperclip hidden while replying (:1102); whitespace-only reply sends nothing and keeps the quote (:1114).
Our `replyToMessage` only checks that the reply was sent.
Proposed cases:
1. **Reply bar.** Long-press, then Reply. Expect `llReply`, `tvUserName` = your own name, `tvMessage` = the parent text and `ivAttachment` **hidden** (`KB.kt:383-392`). Tap `llDelete` and expect `llReply` gone, the input empty and `ivAttachment` back.
2. **Whitespace reply.** Open Reply, type spaces and tap Send. Expect snackbar "Please Type Message To Send", `llReply` still shown and no new comment.
3. **Comment renders once.** After the reply, the comment text occurs exactly once inside the parent's `rvMessageReply`.
4. **Comment menu.** Tap the comment chevron. Expect Read By User, Edit and Delete. Edit opens the dialog; after Update, expect the new text and "Edited". Delete with No keeps the comment; with Yes it is gone. Read By User opens `readByRv`.

#### G-SELECT and G-DELETE: selection mode and delete variants (4 shared tests)
Web: selection mode hides controls and counts the selection (:833); delete can be backed out at both steps (:1000); multi-select deletes two (:1016); a deleted message stays gone after reopen (:1033).
Proposed cases:
1. **Selection mode.** Long-press, then Delete. Expect `layOption`/`layAction` visible, `tvCount` = "1" and `etMessage` gone. Tap a second own message and expect `tvCount` = "2". Tap `btnBack`: selection mode ends and both messages remain.
2. **Back out at the dialog.** Tap the `tvAction` "Delete" and answer **No**. The message stays and the composer is back.
3. **Multi-delete.** Select two markers, tap Delete, then Yes. Both are gone.
4. **Gone after reopen.** Leave the module and reopen it: the deleted marker is still absent. Put this in the same case as #3.
5. Android-only: a non-admin long-pressing someone else's message and tapping Delete gets "You have no permission to delete other user message."

#### G-EDIT: edit-dialog rules (2 shared tests)
Web: edit Cancel/Escape leave the message unchanged (:884); unchanged text sends no PUT, empty text keeps the modal open, the box caps at 8,000 (:903).
Proposed cases (use `cancelEditDialog`, `setEditDialogText`, `saveEditDialog`):
1. **Cancel keeps the text.** After Cancel, the original text is shown and has no "Edited".
2. **Empty edit is refused.** Clear the field and tap Update. Expect the field error "Message could not be empty" and the dialog still open.
3. **Over-limit edit.** 2,001 characters gives snackbar "There is only 2000 Characters limit allowed to send".
4. **Unchanged Update** (documents INFO-19/CONF-17): expect no "Edited" tag. Fails today.
Also extend the existing `editMessage` to assert the red "Edited" in `tvMsgTime`.

#### G-RENDER: message rendering (6 shared tests)
Web: Read More/Read Less (:327); HTML renders as text (:343); links (:355); e-mail link (:374); special characters round-trip (:394); row shows avatar, "Name · Designation" and timestamp (:1583).
Proposed cases:
1. **Read more.** Send 1,500 characters, under the Android limit. The bubble ends with " Read more"; tapping it shows the full text with " Read less".
2. **Literal HTML and markdown.** Send `<b>x</b><script>1</script> **y**` and expect the exact text shown.
3. **Special characters.** Send `Ünïcødé ✓ 😀 שלום` and expect an exact match.
4. **Links.** Send `https://example.com` and `qa@example.com`: both are clickable (autoLink). Platform difference: Android also links `www.example.com`; web keeps it plain.
5. **Row header.** `tvUserName` shows your own name and designation, and `tvMsgTime` matches `^[A-Z][a-z]{2} \d{2}, \d{4} \d{2}:\d{2} (AM|PM)$`. There is no "at", unlike web.

#### G-FORWARD: forward variants (3 shared tests)
Web: Home Tab submenu lists Home units without Calendar or Call Sheet (:1262, skipped on web); forwarding into this feed creates a copy (:1290); Forward to Remote Projects (:1316).
Proposed cases (ForwardPage, `ForwardPage.kt:894/918/1229`):
1. **Forward to Home.** Home lists units including Bulletin and excludes Calendar and Call Sheet. Forward to Bulletin and expect the marker there. This is sheet INFO-02/CONF-02, marked not affected on Android.
2. **Forward into the same tool** creates a second copy: count of M = 2. Clean up after.
3. **Remote Projects** opens "Select Remote Project", or shows the toast "Remote Project Not Created Yet!". Close it and expect nothing sent.

#### G-DOWNLOAD and G-VIEWER: open and download media (4 shared tests)
Web: Download on PDF (:1399); Download on image (:1417); PDF bubble opens a viewer, spreadsheet opens nothing (:1435); image opens a preview (:1456).
Proposed cases:
1. **Download.** Long-press a PDF or image, then Download. Expect the download progress or the toast "File Downloaded Successfully.". Without download rights, expect "You do not have Downloading Rights".
2. **Image viewer.** `openNewestImage()` → `isImageViewerOpen`; Back returns to the feed.
3. **Document tap.** Android opens docs in an external app (`openFileFromExternalFromUrl`, `CI.kt:1690-1695`); web opens an in-app viewer. Assert that the foreground package or activity changes, then return.

#### G-FAB and G-PAGING: scroll controls (4 shared tests)
Web: Gallery/History FABs present, scroll-to-bottom only when scrolled away (:187); feed opens at the newest row (:1596); scroll-to-bottom returns to the latest (:1623); scrolling up loads an older page (:1603).
Proposed cases:
1. **Opens at bottom.** On open the newest marker is visible, `arrowDownCard` is hidden and `userHistoryBtn` is shown.
2. **Scroll-to-bottom.** Swipe down several times so the list moves up: `arrowDownCard` appears. Tap it: the newest marker is visible and the card hides.
3. **Older page.** Skip if there are fewer than 50 rows. Pull to refresh at the top, or swipe to the top: the row count grows.

#### G-DRAFT, G-PERSIST and G-DOUBLE: draft, persistence, double send (3 shared tests)
Web: an unsent draft survives a reopen and clears once sent (:449); a posted message persists across a reopen (:466); a fast double Enter posts once (:501); plus the deleted-gone-after-reopen case in G-DELETE.
Proposed cases:
1. **Draft.** Type D without sending, press Back to Tools and reopen: `etMessage` = D (documents INFO-23/CONF-16; likely fails per `KB.kt:129` vs `CI.kt:647`). Send it, reopen, and the input is empty.
2. **Persistence.** Send M, leave and reopen: M is present and sent.
3. **Double tap Send.** Double-tap `ivSendMessage` quickly: M occurs exactly once.

#### G-EMOJI: emoji picker (2 shared tests)
Web: picker lists emoji and closes (:438); picked emoji is appended and sent (:418).
Proposed case: tap `ivEmoji` and expect `emojiPicker` visible. Pick the first emoji: it is appended to `etMessage`. `backPress` deletes it as one character (surrogate-safe, `KB.kt:187-201`). Pick again, send, and expect the emoji in the row.

#### G-LIMIT: message length limit (1 shared test, plus a sheet-bug test in the module section)
Web: the composer caps at 8,000 and shows a counter from 7,200 (:311).
Platform difference, not a conflict with any Android test: Android allows 2,000 and has no counter. This is sheet INFO-25/CONF-22 (Android Open).
Proposed case: paste 2,000 characters, send, and expect it posted. Paste 2,001 characters, tap Send, and expect snackbar "There is only 2000 Characters limit allowed to send" with no new row. If product decides 8,000 is right, flip this to expect the 2,100-character post to succeed, matching the web sheet test.

#### Smaller groups (one shared test each)
- **G-CAPTION** (:581, captioned document offers Edit). Long-press a captioned document, then Edit: the "Edit description" dialog is prefilled with the caption. Update, then expect the new caption and "Edited" (`CI.kt:1539-1566`).
- **G-IMGREPLY** (:731, Image Reply). Long-press an image, then "Image Reply". Pick an image and add a caption: a new image row with that caption is posted, not a comment (`CI.kt:2040-2063`).
- **G-DD** (:1382, Publish to Doc Distribution). The option appears on a document and not on text. Cancel the publish sheet and nothing is created. It needs DD posting rights, otherwise expect `dd_no_distribute_permission`. Note: sheet INFO-33/CONF-30 says the option is missing on Android, but the source shows it (`CI.kt:1270-1272`).
- **G-GALLERY** (:1551, Gallery Docs tab). Open Gallery, then the Docs tab: the uploaded document is listed by file name with a size and unit. The tab label shows a count. Documents INFO-24/CONF-21.
- **G-OTHER** (:1638, another member's message has no Edit). On a message from another user, Android **shows** Edit and refuses it when tapped. Assert the snackbar "You Don't Have Permission To Edit This Message." today, or assert Edit is absent if product aligns with web. Uses any existing message by another user in the feed.
- **G-PICKER-CANCEL** (:645 Location Upload cancel; :661 closing the document preview). Open Location and back out: nothing is posted (`isLocationSendUp`). Open the media preview and back out: nothing is posted (`isMediaPreviewUp`).
- **G-PDFJS** (:686, a PDF carrying JavaScript is blocked). Upload a fixture PDF with `/JS`: expect it refused. Android has no check today (INFO-26/CONF-15 Open), so mark it known-bug.
- **G-LIVE** (:1802, a message from a second context appears live). Needs a second device or account B. Defer it until the suite has a two-account harness.
- **G-COMPOSER, low** (:251 auto-capitalise; :408 "@" opens no mention picker). Capitalisation comes from the IME (`textCapSentences`), and Appium `sendKeys` bypasses it, so it is low value. "@" opens no picker on Android either (no mention code in `KB.kt`).
- **G-MENU-DISMISS, low** (:819, the options menu closes on Escape or an outside click). On Android the popup dismisses on an outside tap or on scroll (`CI.kt:301-306`). Assert that a tap outside closes `rvVw`.

### COVERED (shared)

| Web test(s) | Android test (`ToolChatBaseTest`) | Note |
|---|---|---|
| opens with the composer and the feed root (:156) | `moduleOpens` | The window-title part is web-only |
| empty and whitespace-only messages are not posted (:266) | `emptyInputHidesSend`, `whitespaceNotSendable` | |
| the paperclip menu lists the expected pickers (:518) | `attachmentPickerHasMediaTypes` | Asserts only >= 6; Android offers 8, so assert `HomeChatPage.EXPECTED_MEDIA` exactly |
| the Gallery drawer opens with its tabs (:1532) | `galleryOption` | Android has 3 tabs (Media/Docs/Links); web has 4 |
| post text with Enter (:216); the mic slot becomes Send (:238) | `sendValidMessage`, `typingRevealsSend`, `emptyInputHidesSend` | |
| a pending row shows the clock until the ack (:476) | `sendValidMessage` (via `waitMessageSent`: clock and Retry gone) | |
| edit an own message: prefilled, Update, Edited (:859) | `editMessage` | Does not assert the "Edited" tag or the prefilled text |
| delete an own message through the selection flow (:977) | `deleteMessage` | |
| a reply nests as a comment (:1046) | `replyToMessage` | Does not check the nesting in `rvMessageReply` |
| the Forward modal lists the destinations (:1215) | `forwardMessage` | Only checks that ForwardPage opens |
| Share opens Compose Email (:1329) | `shareMessage` | Android goes to the share bar and the OS share sheet; Compose Email is web-only. See weak assertions below |
| Read By lists two segments, searches, empty state (:1357) | `readByUser` | Search and empty state not asserted. Android shows a "Notify Users" button (`activity_read_by_users.xml:164-172`); web has none |

### WEB-ONLY (shared)

| Web test(s) | Why web-only | Evidence |
|---|---|---|
| the scoped composer resolves to exactly one element (:165) | Multi-window workspace DOM scoping | There is one activity per screen on Android |
| theme toggle flips the surface theme (:171) | No per-feed theme button | No theme control in the `activity_confidential_info.xml` ids |
| every acked row has an options button; the counter is aria-live (:204) | Android uses long-press, with no chevron per message and no counter/aria | `CI.kt:915-925` (LONG_PRESS) |
| each hidden file input advertises the right accept list (:536) | HTML `accept` attribute | The Android equivalent is the SAF MIME list, `FilePickerContract.kt:29-38` |
| image picker rejects a gif (:672); document picker rejects an executable (:679); audio and media pickers reject a text file (:693) | Browser file-input validation. On Android the system picker greys out types outside the MIME list, so there is no in-app rejection to assert | `FilePickerContract.kt:29-38`; GIF handling in the in-app gallery is unverified |
| minimizing and restoring the window (:1818) | Workspace window minimize | The Android analog (backgrounding) is covered by G-DRAFT |
| Shift+Enter adds a newline (:282); Ctrl+Enter sends (:301) | Keyboard shortcuts | Android sends with `ivSendMessage` only |
| the own mention token renders as the own name (:382) | The Info renderer has no mention parsing | `AttachmentVH.kt:603-660` |
| dropping an image on the feed (:705) | Drag and drop | — |
| the Edit modal renders inside the tool window body (:1838) | Workspace windows | — |

### Weak assertions in the existing Android cases (not conflicts, but worth fixing)
1. `shareMessage` → `HomeChatPage.shareStarts()` falls back to `!currentActivityContains("home")`. Inside Info and Confidential Info the activity is `ConfidentialInfo`, so this fallback is **always true** and the case cannot fail once the Share item is tapped. Assert the `tvAction` "Share" bar, or the OS chooser package, instead.
2. `readByUser` calls `assumeTrue(opened)` just before `assertTrue(opened)`, so it skips instead of failing.
3. `longPressShowsOptions` asserts only that the menu opened. See G-MENU for the option matrix.
4. `attachmentPickerHasMediaTypes` accepts >= 6 while Android always offers 8 here (`CI.kt:790-797`).
5. `galleryOpensLibrary` accepts `textShown("Gallery")`, which can match the menu row itself.

### Platform differences found while comparing (no Android test asserts either side)
| Topic | Web | Android source | Which looks right |
|---|---|---|---|
| Max length | 8,000, counter from 7,200 | 2,000, snackbar, no counter (`Constants.kt:733`) | Product decision. Already logged as INFO-25/CONF-22 |
| Edit on another member's message | Not offered | Offered, then refused (`CI.kt:1255`, `:1770-1774`) | Web. Android shows a dead option (INFO-16/CONF-24 for comments) |
| `www.` text | Plain text | Linked (`autoLink="all"`) | Minor |
| Timestamp | "Mon DD, YYYY at hh:mm AM/PM" | `MMM dd, yyyy hh:mm a` | Cosmetic |
| Share | Compose Email | OS share sheet | By design per platform |
| Gallery tabs | 4 | 3 (Media/Docs/Links) | By design |
| Read By | No notify button | "Notify Users" button | By design |
| CSV attach | Allowed | Not in the MIME list | Web. Android bug INFO-27/CONF-23 |
