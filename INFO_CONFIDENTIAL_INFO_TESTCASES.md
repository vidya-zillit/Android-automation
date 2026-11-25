# Info and Confidential Info: test cases

Suites: `InfoTest` (Tools → Info) and `ConfidentialInfoTest` (Tools → Confidential Info).
Both open the same Android activity, `ConfidentialInfo.kt`, so every case lives once in
`tests/tools/ToolChatBaseTest.java` and runs in both suites. Page object for the feed-only
helpers: `pages/tools/InfoFeedPage.java` (extends `HomeChatPage`).

Run one suite at a time:

```
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ./run-and-report.sh --only InfoTest
ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ./run-and-report.sh --only ConfidentialInfoTest
```

The web-gap cases (orders 100–232, plus the clean-up at 299) are enabled (JUnit `@EnabledIf`) only for
suites that override `infoFeedCases()` to return true. Info and Confidential Info do;
Catering inherits the base class but does not, so these cases are reported as disabled
there rather than run against a different activity. Offline cases run only on an emulator (`ZILLIT_UDID=emulator-*`) and always restore Wi-Fi/data.

## Original cases (orders 1–15)

| # | Case |
|---|---|
| 1 | Module opens its chat (input bar or no-rights bar) |
| 2–4 | Empty input hides Send; whitespace not sendable; typing reveals Send |
| 5 | A valid message reaches the server (no clock, no Retry) |
| 6 | Attachment picker offers the media types (Info/CI now require all 8) |
| 7–8 | Long-press menu opens; Reply posts a comment |
| 9–11 | Edit, Forward (picker opens), Gallery (library opens) |
| 12 | Read By User is offered on a sent message and opens the read-by screen (fails, not skips) |
| 13 | Share enters the Share bar and opens the Android share sheet (no always-true fallback) |
| 14–15 | Delete (3-step); image message |

## Web gap cases (added 2026-11-23)

Web ids refer to `web-e2e/gaps/info.md` and `gaps/confidential-info.md`
(`unit-feed.suite.js:<line>` is the shared web suite). "Last result" is the last run on
the `Zillit_32539-dev` build; see the run notes below.

### Shared feed cases (`ToolChatBaseTest`, run in InfoTest and ConfidentialInfoTest)

Results: **I** = InfoTest, **C** = ConfidentialInfoTest. These are the last runs on an emulator (Vidya Testing, admin) on 2026-11-24, unless marked otherwise. PASS = passed. BUG = failed on a reproducing app bug (the message names it). SKIP = precondition missing (the reason is logged).

| Order | Case (method) | Web id / gap row | Last result |
|---|---|---|---|
| 100 | `headerShowsModuleName` | G-HEADER (info.spec.js:50, CI spec:53) | I PASS · C PASS |
| 101 | `composerXorRequestBar` | G-RIGHTS invariant | I PASS · C PASS |
| 102 | `viewOnlyShowsRequestBar` | G-RIGHTS #1 (info.spec.js:86/:241, CI :106/:619) | SKIP: both accounts are admins with posting rights (real users' rights are not changed) |
| 103 | `feedOpensAtNewest` | G-FAB #1 (:187/:1596) | I PASS · C PASS |
| 104 | `scrollToBottomCard` | G-FAB #2 (:1623) | I PASS · C PASS |
| 105 | `olderPageLoads` | G-PAGING (:1603) | I PASS (117 rows) · C PASS (71 rows) |
| 106 | `fabDoesNotCoverNewestRow` | INFO-11 | I PASS · C PASS (not reproduced on this build) |
| 110 | `searchFindsMarker` | G-SEARCH #1 (:1471, INFO-09) | I PASS · C PASS |
| 111 | `emptySearchRefused` | G-SEARCH #2 (:1493) | I PASS · C PASS |
| 112 | `noMatchAndSpecialCharsSearch` | G-SEARCH #3 (:1493) | I PASS · C PASS |
| 113 | `changedWordReSearches` | INFO-20 / CONF-18 | I PASS · C PASS (INFO-20/CONF-18 fixed in this build, BUL-14) |
| 114 | `closeSearchRestoresFeed` | INFO-12 / CONF-12 | I PASS · C PASS |
| 115 | `searchByOwnName` | G-SEARCH #6 (:1520) | I PASS · C PASS |
| 120 | `historyOpensAndReturns` | G-HISTORY #1 (:1661), G-HISTORY-CI #1 (CI :69) | I PASS · C PASS |
| 121 | `deletedMessageInHistory` | G-HISTORY #2 (:1681) | I PASS · C PASS |
| 122 | `historyHasSearch` | INFO-22 / CONF-20 | **BUG** I and C: History has no search (`UserChatHistoryPage` shows its search icon only for Script Notes) |
| 130 | `selectionModeCountsAndBacksOut` | G-SELECT #1 (:833) | I PASS · C PASS |
| 131 | `deleteBackOutAtDialog` | G-DELETE #2 (:1000) | I PASS · C PASS |
| 132 | `multiDeleteGoneAfterReopen` | G-DELETE #3/#4 (:1016/:1033) | I PASS · C PASS |
| 140 | `editShowsEditedTag` | G-EDIT (:859) | I PASS · C PASS |
| 141 | `editCancelKeepsText` | G-EDIT #1 (:884) | I PASS · C PASS |
| 142 | `emptyEditRefused` | G-EDIT #2 (:903) | I PASS · C PASS |
| 143 | `overLimitEditRefused` | G-EDIT #3 (:903) | I PASS · C PASS |
| 144 | `unchangedUpdateNotEdited` | INFO-19 / CONF-17 | I PASS · C PASS (fixed in this build, BUL-12) |
| 150 | `textMenuMatrix` | G-MENU text (:750) | I PASS · C PASS |
| 160 | `replyBarQuotesAndDismisses` | G-REPLY #1 (:1086/:1102) | I PASS · C PASS |
| 161 | `whitespaceReplyRefused` | G-REPLY #2 (:1114) | I PASS · C PASS |
| 162 | `commentRendersOnceAndMenuWorks` | G-COMMENT #3/#4 (:1071/:1129/:1153/:1174), CONF-05/CONF-08 | I PASS · C PASS |
| 170 | `messageLengthLimit` | G-LIMIT (:311), INFO-25 / CONF-22 | I PASS · C PASS. Android limit is 2,000 (web 8,000); the case asserts Android's rule |
| 171 | `readMoreReadLess` | G-RENDER (:327) | I PASS · C PASS |
| 172 | `literalAndSpecialCharacters` | G-RENDER (:343/:394) | I PASS · C PASS |
| 173 | `rowHeaderAndTimestamp` | G-RENDER (:1583) | I PASS · C PASS |
| 174 | `urlIsLinked` | G-RENDER links (:355) | I PASS · C PASS (opens Chrome) |
| 180 | `draftSurvivesReopen` | INFO-23 / CONF-16 | **BUG** I and C: the unsent draft is empty after a reopen |
| 181 | `messagePersistsAfterReopen` | G-PERSIST (:466) | I PASS · C PASS |
| 182 | `doubleTapSendPostsOnce` | G-DOUBLE (:501) | I PASS · C PASS. The second tap lands on the mic and starts a recording, which the case cancels |
| 185 | `emojiPickerAppendsAndSends` | G-EMOJI (:418/:438) | I PASS · C PASS |
| 190 | `readBySearchNoMatch` | INFO-21 / CONF-19 | I PASS · C PASS (fixed in this build, BUL-18) |
| 195 | `otherMembersMessageEditRefused` | G-OTHER (:1638) | SKIP when no other member's post is loaded; covered for real by `InfoTwoUserTest` |
| 196 | `atSignOpensNoMentionPicker` | G-COMPOSER "@" (:408) | I PASS · C PASS |
| 197 | `menuClosesOnOutsideTap` | G-MENU-DISMISS (:819) | I PASS · C PASS |
| 200 | `offlineSendShowsRetryAndResends` | G-OFFLINE #1 (:1709), CONF-25 | I PASS · C PASS. CONF-25 is not reproduced: the row turns to Retry and Retry delivers it. Emulator only |
| 201 | `offlineDeleteRefused` | G-OFFLINE #2 (:932) | I PASS · C PASS (emulator only) |
| 202 | `offlineEditDoesNotLoseMessage` | G-OFFLINE #2 edit (:932) | I PASS · C PASS. The offline edit shows "Internet connection lost" and the edit is queued and applied (emulator only) |
| 203 | `feedOpensOffline` | G-OFFLINE #3 (:1783) | I PASS · C PASS (emulator only) |
| 204 | `historyOfflineNotBlank` | INFO-28 / CONF-26 | I PASS · C PASS. Not reproduced: offline History shows its empty view (emulator only) |
| 210 | `uploadDocumentsWithCaption` | G-UPLOAD #1 (:556/:570), G-MENU doc (:765) | I PASS · C PASS (docx, xlsx and pdf with captions; PDF menu matrix) |
| 211 | `csvDocumentSelectable` | INFO-27 / CONF-23 | **BUG** I and C: the CSV is listed but greyed out in the Document picker |
| 213 | `galleryDocsShowsNameAndSize` | G-GALLERY (:1551), INFO-24 / CONF-21 | **BUG** I and C: the Docs tab lists the file name, but the size has no unit ("426  ·  NOV 24, 2026") |
| 215 | `imageWithCaptionAndMenu` | G-UPLOAD #3 (:604), G-MENU image (:787) | I PASS · C PASS |
| 216 | `imageReplyPostsNewImage` | G-IMGREPLY (:731) | I PASS · C PASS |
| 217 | `documentWithoutCaptionMenu` | G-MENU document without caption (:765) | I PASS · C PASS |
| 218 | `downloadDocument` | G-DOWNLOAD #1 (:1399), INFO-31 / CONF-28 (CI goes through the watermark path) | I PASS · C PASS ("File Downloaded Successfully."; for CI this goes through the watermark path) |
| 219 | `documentTapOpensViewer` | G-VIEWER #3 (:1435) | I PASS · C PASS (opens the Drive PDF viewer) |
| 220 | `imageViewerOpens` | G-VIEWER #2 (:1456) | I PASS · C PASS (CustomImageViewer) |
| 221 | `publishToDocDistCancel` | G-DD (:1382) | I PASS · C PASS. The prompt "… to the Document Distribution library?" is cancelled |
| 222 | `voiceNotePostsAndMenu` | G-UPLOAD #5 (:631), G-MENU audio (:802) | FAIL, unconfirmed: the voice note posts and uploads, but no long-press menu opened on the audio row in I or C (and none after a reopen). The locator was last changed to press the row's plain File Size/duration labels, and that version has not run yet. If it still fails, check by hand. AudioVH attaches the row's long-press only when `deleted == 0L`, and the sender name, play button and seek bar are clickable and swallow the press. |
| 223 | `twoImagesInOnePick` | G-UPLOAD #4 (:615) | I PASS · C PASS (Gallery media count +2) |
| 224 | `pickerCancelPostsNothing` | G-PICKER-CANCEL (:645/:661) | I PASS · C PASS |
| 225 | `pdfWithJavascriptRefused` | G-PDFJS (:686), INFO-26 / CONF-15 | **BUG** I and C: a PDF with an /OpenAction JavaScript is accepted and posted |
| 226 | `deletedDocumentInHistoryOffersDownloadView` | G-HISTORY #2 PDF (:1681) | I PASS · C PASS (the History row offers Download) |
| 227 | `documentCaptionEdit` | G-CAPTION (:581) | I PASS · C PASS (the captioned image; in Info's single-method rerun it skips, because it depends on earlier cases) |
| 230 | `forwardIntoSameToolCopies` | G-FORWARD #2 (:1290) | I PASS · C PASS |
| 231 | `forwardHomeListsUnits` | G-FORWARD #1 (:1262), INFO-02 / CONF-02 | I PASS · C PASS. Platform difference: Android lists Call Sheet for a text forward (ForwardPage hides it only for image, video and audio) |
| 232 | `forwardRemoteProjects` | G-FORWARD #3 (:1316) | I PASS · C PASS |
| 299 | `sweepAutoMessages` | clean-up | I PASS · C PASS. Best effort: deletes this run's ZL_AUTO rows through multi-select (duplicates and rows scrolled far away can survive) |

**Totals, last full runs on an emulator:** ConfidentialInfoTest has 81 cases: 73 pass, 6 fail and 2 skip. The 6 failures are 5 known bugs (CONF-15, CONF-16, CONF-20, CONF-21, CONF-23) and the voice-note menu. InfoTest last ran in full in run 3, before the final fixes. Every case changed after that run was rerun alone and now passes, except the known bugs (INFO-20 passes; INFO-22, INFO-23, INFO-24, INFO-26 and INFO-27 fail) and the voice-note menu.

Weak or never-failing cases that were fixed: `shareMessage` now asserts the Share bar and the OS share sheet, `readByUser` now fails instead of skipping, and `attachmentPickerHasMediaTypes` now requires all 8 options for Info and CI. I PASS · C PASS.

### Two-device cases (`InfoTwoUserTest`, `TestOrder.INFO_TWO_USER` = 210)

A = emulator (Vidya Testing) watches; B = Pixel (Vidya Pixel) acts. Run with
`ZILLIT_UDID=<emulator> ZILLIT_RECEIVER_UDID=34281FDH2005XC ./run-and-report.sh --only InfoTwoUserTest`.

| # | Case | Web id / gap row | Last result |
|---|---|---|---|
| 1 | `liveUpdatesInfo`: B's post, edit, comment and delete appear live on A | G-LIVE-INFO (info.spec.js:89/:219), G-LIVE (:1802) | NOT RUN: the Pixel was busy (compiles; see the report) |
| 2 | `liveUpdatesConfidentialInfo` | G-LIVE-CI, CONF-10 | NOT RUN: the Pixel was busy (compiles; see the report) |
| 3 | `otherMembersMessageEditRefused` | G-OTHER (:1638) | NOT RUN: the Pixel was busy (compiles; see the report) |
| 4 | `otherMembersCommentNoEditInfo` | INFO-16 (known bug) | NOT RUN: the Pixel was busy (compiles; see the report) |
| 5 | `otherMembersCommentNoEditCi` | CONF-24 (known bug) | NOT RUN: the Pixel was busy (compiles; see the report) |
| 6 | `infoTileBadge`: badge after B posts, cleared on open | G-BADGE (info.spec.js:88/:165) | NOT RUN: the Pixel was busy (compiles; see the report) |
| 7 | `confidentialTextNotLeaked` | CONF-07 | NOT RUN: the Pixel was busy (compiles; see the report) |


## Not automatable here (and why)

| Gap row | Web refs | Reason |
|---|---|---|
| G-RIGHTS #2–#5: no view access closes the tool, live grant or revoke while open, View off closes, view-only sees comments read-only, download rights for a non-admin (INFO-07/08/10/17/30/31/35, CONF-06/13/27/28/32/33) | info.spec.js:87/:132/:238/:245/:264/:287/:328, sheet-bugs :260; CI spec :103/:104/:107/:625/:632/:646, sheet-bugs :81/:113/:120/:171/:217 | These need a member whose Info or CI rights an admin changes. Both project members are admins, and changing real users' rights has no safe restore here (brief rule). `viewOnlyShowsRequestBar` runs for real on any view-only account. |
| G-FORWARD-INFO #2: forwarding a forwarded copy to a member (INFO-29) | sheet-bugs | Sends a message into the other test user's C&C chat, which the C&C agent owns. Left for the C&C two-user suite. |
| G-FORWARD-INFO #3: forward to a switched-off Confidential Info (INFO-06) and G-CUSTOM: Info cannot be switched off | info.spec.js:90/:104/:534 | Changes the project's tool customization; the product rule is unconfirmed. |
| G-OFFLINE status-0 POST and failing comment POST | unit-feed :1726/:1189 | Need a server stub; Android has no fault-injection hook. Offline send, edit, delete, open and History are covered. |
| G-OFFLINE: delete a failed row with no server call | unit-feed :1750 | Android's Delete always needs a connection (`CI.kt` setDeleteAction); covered by `offlineDeleteRefused`. |
| G-HISTORY-CI #3: deleted confidential posts readable in History (CONF-11) | CI sheet-bugs | Expected rule not agreed with product ("To verify"). `deletedMessageInHistory` documents that they are listed. |
| G-DOWNLOAD (CI): the downloaded file is watermarked | CI-only | Download success is asserted; checking the watermark means pulling and decoding the saved file from shared storage. |
| G-LIVE: the "New message received" banner while viewing Info (INFO-18) | info.spec.js:165 | The banner string is not in the app resources and could not be identified. CONF-07 (no confidential text outside the tool) is covered in `InfoTwoUserTest`. |
| G-COMPOSER auto-capitalisation | unit-feed :251 | Appium types with setText, which bypasses the IME's textCapSentences. |
| INFO-13 one-line draft height; INFO-14 comment author ellipsis | sheet-bugs | INFO-13 is blocked by INFO-23 (the draft is not restored at all). INFO-14 is visual: UiAutomator returns the full text of an ellipsized TextView. |
| INFO-34 / CONF-31: socket frames from other productions | sheet-bugs | Needs two projects. |
| Web-only rows | see the gap files | Workspace windows, theme toggle, keyboard shortcuts, drag and drop, HTML accept lists, and the classic-layout Back button. |
